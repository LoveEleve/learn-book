/*
 * R-1 redisObject harness — 外壳布局 + 编码降级 + 引用计数极简复现
 * 还原核心控制流:
 *  1. 16B 外壳 (type:4+encoding:4+lru:24+refcount+ptr)
 *  2. EMBSTR 64B arena 数学 (16+3+44+1=64)
 *  3. INT 编码零分配 + 共享整数池
 *  4. tryObjectEncoding 优化链 (INT → 共享 → EMBSTR)
 *  5. 引用计数三态 (释放分派/递减/共享免计数)
 * 编译: gcc -O0 -o harness object_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#define OBJ_STRING 0
#define OBJ_SHARED_INTEGERS 10000
#define OBJ_ENCODING_RAW 0
#define OBJ_ENCODING_INT 1
#define OBJ_ENCODING_EMBSTR 8
#define OBJ_SHARED_REFCOUNT INT_MAX
#define OBJ_ENCODING_EMBSTR_SIZE_LIMIT 44

typedef struct robj {
    unsigned type : 4;
    unsigned encoding : 4;
    unsigned lru : 24;
    int refcount;
    void *ptr;
} robj;

/* ---------- 创建 ---------- */
static robj *createObject(int type, void *ptr) {
    robj *o = calloc(1, sizeof(robj));
    o->type = type;
    o->encoding = OBJ_ENCODING_RAW;
    o->ptr = ptr;
    o->refcount = 1;
    return o;
}
static robj *makeObjectShared(robj *o) {
    o->refcount = OBJ_SHARED_REFCOUNT;
    return o;
}
/* EMBSTR: robj + sdshdr8(3B) + buf 同 chunk (对照 object.c:71-93) */
static robj *createEmbeddedStringObject(const char *ptr, size_t len) {
    robj *o = malloc(sizeof(robj) + 3 + len + 1);
    unsigned char *sh = (unsigned char*)(o + 1);
    o->type = OBJ_STRING;
    o->encoding = OBJ_ENCODING_EMBSTR;
    o->ptr = sh + 3;
    o->refcount = 1;
    sh[0] = len; sh[1] = len; sh[2] = 1;   /* sdshdr8: len/alloc/flags */
    memcpy(sh + 3, ptr, len);
    sh[3 + len] = 0;
    return o;
}
/* INT: ptr 存值零分配 */
static robj *createIntObject(long value) {
    robj *o = createObject(OBJ_STRING, NULL);
    o->encoding = OBJ_ENCODING_INT;
    o->ptr = (void*)value;
    return o;
}

/* ---------- 共享池 ---------- */
static robj *shared_integers[OBJ_SHARED_INTEGERS];
static void createSharedIntegers(void) {
    for (int j = 0; j < OBJ_SHARED_INTEGERS; j++) {
        shared_integers[j] = makeObjectShared(createIntObject(j));
    }
}

/* ---------- 优化链 (对照 object.c:607-683, 简化) ---------- */
static int string2l(const char *s, size_t len, long *value) {
    char buf[32];
    if (len == 0 || len >= 20) return 0;
    memcpy(buf, s, len); buf[len] = 0;
    char *end;
    long v = strtol(buf, &end, 10);
    if (*end != 0) return 0;
    *value = v;
    return 1;
}
static robj *tryObjectEncoding(robj *o, int maxmemory) {
    if (o->refcount > 1) return o;                       /* 共享不可变 */
    long value;
    size_t len = o->encoding == OBJ_ENCODING_RAW ? strlen(o->ptr) : 0;
    char *s = o->ptr;
    if (o->encoding != OBJ_ENCODING_INT && len <= 20 && string2l(s, len, &value)) {
        if (!maxmemory && value >= 0 && value < OBJ_SHARED_INTEGERS) {
            free(o->ptr);
            free(o);
            return shared_integers[value];               /* 共享池 */
        }
        free(o->ptr);
        o->encoding = OBJ_ENCODING_INT;
        o->ptr = (void*)value;
        return o;
    }
    if (len <= OBJ_ENCODING_EMBSTR_SIZE_LIMIT) {          /* RAW→EMBSTR */
        robj *emb = createEmbeddedStringObject(s, len);
        free(o->ptr);
        free(o);
        return emb;
    }
    return o;
}

/* ---------- 引用计数 (对照 object.c:349-377, 简化) ---------- */
static long ptr_freed = 0;      /* RAW 才单独 free ptr */
static long obj_freed = 0;      /* 对象外壳释放数 */
static void freeStringObject(robj *o) {
    /* 真实语义: RAW 单独 free sds; EMBSTR 同 chunk 整体 free; INT 零分配 */
    if (o->encoding == OBJ_ENCODING_RAW) ptr_freed++;
    obj_freed++;
}
static void incrRefCount(robj *o) {
    if (o->refcount < OBJ_SHARED_REFCOUNT) o->refcount++;
}
static void decrRefCount(robj *o) {
    if (o->refcount == 1) {
        if (o->type == OBJ_STRING) freeStringObject(o);
        free(o);
    } else {
        if (o->refcount != OBJ_SHARED_REFCOUNT) o->refcount--;
    }
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

int main(void) {
    printf("[1] 16B 外壳位域\n");
    CHECK(sizeof(robj) == 16, "robj == 16 字节 (64 位)");

    fprintf(stderr, ">>> [2] BEGIN\n");
    printf("\n[2] EMBSTR 64B arena 数学: 16+3+44+1=64\n");
    fprintf(stderr, "    sizeof(robj)=%zu\n", sizeof(robj));
    robj *emb = createEmbeddedStringObject("hello", 5);
    fprintf(stderr, "    emb=%p ptr=%p off=%ld\n", emb, emb->ptr, (char*)emb->ptr-(char*)emb);
    CHECK(emb->encoding == OBJ_ENCODING_EMBSTR, "5B 串 → EMBSTR");
    CHECK(sizeof(robj) + 3 + 44 + 1 == 64, "44B 上限: 16+3+44+1 = 64B (jemalloc 桶)");
    CHECK((char*)emb->ptr == (char*)emb + sizeof(robj) + 3, "对象与内容同 chunk (连续)");

    fprintf(stderr, ">>> [3] BEGIN\n");
    printf("\n[3] INT 零分配 + 共享池\n");
    fprintf(stderr, "    creating shared pool\n");
    createSharedIntegers();
    fprintf(stderr, "    shared pool done, [5]=%p\n", shared_integers[5]);
    robj *i5 = createIntObject(5);
    CHECK(i5->encoding == OBJ_ENCODING_INT, "INT 编码");
    CHECK(i5->refcount == 1, "非共享 INT refcount=1");
    robj *shared5 = shared_integers[5];
    CHECK(shared5->refcount == OBJ_SHARED_REFCOUNT, "共享整数 refcount=特殊值");

    fprintf(stderr, ">>> [4] BEGIN\n");
    printf("\n[4] 优化链: \"12345\" → 共享/INT; \"hello\" → EMBSTR\n");
    fprintf(stderr, "    [4a] r1 优化\n");
    robj *r1 = createObject(OBJ_STRING, strdup("5"));   /* 5 < 10000 → 共享 */
    robj *opt1 = tryObjectEncoding(r1, 0);
    fprintf(stderr, "    [4a] done opt1=%p\n", opt1);
    fprintf(stderr, "    check1: opt1=%p shared[5]=%p\n", opt1, shared_integers[5]);
    CHECK(opt1 == shared_integers[5], "\"5\" 无 maxmemory → 共享池对象");
    fprintf(stderr, "    [4b] r2 优化\n");
    robj *r2 = createObject(OBJ_STRING, strdup("99999999"));
    robj *opt2 = tryObjectEncoding(r2, 0);
    fprintf(stderr, "    [4b] done opt2=%p enc=%u\n", opt2, opt2->encoding);
    CHECK(opt2->encoding == OBJ_ENCODING_INT, "99999999 (≥10000) → INT 编码 (非共享)");
    fprintf(stderr, "    [4c] r3 优化\n");
    robj *r3 = createObject(OBJ_STRING, strdup("hello"));
    robj *opt3 = tryObjectEncoding(r3, 0);
    fprintf(stderr, "    [4c] done opt3=%p enc=%u\n", opt3, opt3->encoding);
    CHECK(opt3->encoding == OBJ_ENCODING_EMBSTR, "\"hello\" → EMBSTR (优化链最后一步)");
    robj *r4 = createObject(OBJ_STRING, strdup("12345"));
    incrRefCount(r4);                                    /* refcount=2 */
    robj *opt4 = tryObjectEncoding(r4, 0);
    CHECK(opt4 == r4 && r4->encoding == OBJ_ENCODING_RAW, "refcount>1 → 不编码 (共享守卫)");

    fprintf(stderr, ">>> [5] BEGIN\n");
    printf("\n[5] 引用计数三态\n");
    decrRefCount(opt2);                                  /* INT 释放 */
    decrRefCount(opt3);                                  /* EMBSTR 释放 */
    CHECK(obj_freed == 2, "两个对象外壳释放 (走分派)");
    CHECK(ptr_freed == 0, "INT/EMBSTR 均不单独 free ptr (零分配/同 chunk)");
    decrRefCount(shared5);                               /* 共享不减 */
    CHECK(shared5->refcount == OBJ_SHARED_REFCOUNT, "共享对象 decr 不碰 (免计数)");

    printf("\n结果: %s\n", fail ? "FAIL" : "ALL PASS");
    return fail ? 1 : 0;
}
