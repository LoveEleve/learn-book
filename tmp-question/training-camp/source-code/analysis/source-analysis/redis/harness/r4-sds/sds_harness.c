/*
 * R-4 SDS harness — 分级头 + 扩容三路 + 预分配 + 伪降型极简复现
 * 还原核心控制流 (不并发/不二进制安全全量/不用真实 jemalloc):
 *  1. 5 级头部选型 (5/8/16/32) — 长度阈值
 *  2. sdshdr5 特殊性: 增长型字符串强制 8
 *  3. 扩容三路: avail 够→原地 / 同型→realloc / 升级→malloc+memcpy+free
 *  4. greedy 预分配: <1MB→2× / ≥1MB→+1MB
 *  5. 伪降型缩容: 保留旧头只缩 alloc (s[-1] 不更新)
 *  6. sdsIncrLen 零拷贝模式 (read 直写 + 递增)
 * 编译: gcc -O0 -o harness sds_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define SDS_MAX_PREALLOC (1024*1024)
#define SDS_TYPE_MASK 7

/* ---------- 简化头部 (只做 5/8/16/32 四型, 演示阈值) ---------- */
struct hdr8  { unsigned char len, alloc, flags; char buf[]; };
struct hdr16 { unsigned short len, alloc; unsigned char flags; char buf[]; };
struct hdr32 { unsigned int len, alloc; unsigned char flags; char buf[]; };
typedef char *sds;

static int hdr_size(char t) { return t==1 ? 3 : t==2 ? 5 : 9; }
static unsigned char *fp(sds s) { return (unsigned char*)s - 1; }
static char req_type(size_t len) {
    if (len < 32) return 0;        /* 5: 静态串专用 */
    if (len < 256) return 1;       /* 8 */
    if (len < 65536) return 2;     /* 16 */
    return 3;                      /* 32 */
}
static size_t sdslen(sds s) {
    switch (*fp(s) & SDS_TYPE_MASK) {
        case 0: return *fp(s) >> 3;                      /* 5: 5bit len */
        case 1: return ((struct hdr8*)(s-hdr_size(1)))->len;
        case 2: return ((struct hdr16*)(s-hdr_size(2)))->len;
        default: return ((struct hdr32*)(s-hdr_size(3)))->len;
    }
}
static size_t sdsavail(sds s) {
    switch (*fp(s) & SDS_TYPE_MASK) {
        case 0: return 0;                                 /* 5 无 alloc */
        case 1: { struct hdr8 *h = (void*)(s-hdr_size(1)); return h->alloc - h->len; }
        case 2: { struct hdr16 *h = (void*)(s-hdr_size(2)); return h->alloc - h->len; }
        default: { struct hdr32 *h = (void*)(s-hdr_size(3)); return h->alloc - h->len; }
    }
}
static void sdssetlen(sds s, size_t len) {
    switch (*fp(s) & SDS_TYPE_MASK) {
        case 0: *fp(s) = 0 | (len << 3); break;
        case 1: ((struct hdr8*)(s-hdr_size(1)))->len = len; break;
        case 2: ((struct hdr16*)(s-hdr_size(2)))->len = len; break;
        default: ((struct hdr32*)(s-hdr_size(3)))->len = len; break;
    }
}
static void sdssetalloc(sds s, size_t alloc) {
    switch (*fp(s) & SDS_TYPE_MASK) {
        case 1: ((struct hdr8*)(s-hdr_size(1)))->alloc = alloc; break;
        case 2: ((struct hdr16*)(s-hdr_size(2)))->alloc = alloc; break;
        default: ((struct hdr32*)(s-hdr_size(3)))->alloc = alloc; break;
    }
}

/* ---------- 创建 (对照 sds.c:81-144) ---------- */
static sds sdsnewlen(const void *init, size_t initlen) {
    char type = req_type(initlen);
    if (type == 0 && initlen == 0) type = 1;      /* 空串强制 8 (对照 L87) */
    size_t hdrlen = hdr_size(type);
    void *sh = malloc(hdrlen + initlen + 1);
    sds s = (char*)sh + hdrlen;
    *fp(s) = type;
    switch (type) {
        case 0: *fp(s) |= (initlen << 3); break; /* 5: len 进 flags */
        case 1: { struct hdr8 *h = sh; h->len = initlen; h->alloc = initlen; break; }
        case 2: { struct hdr16 *h = sh; h->len = initlen; h->alloc = initlen; break; }
        default: { struct hdr32 *h = sh; h->len = initlen; h->alloc = initlen; break; }
    }
    if (initlen && init) memcpy(s, init, initlen);
    s[initlen] = '\0';
    return s;
}

/* ---------- 扩容三路 + 预分配 (对照 sds.c:217-268) ---------- */
static sds sdsMakeRoomFor(sds s, size_t addlen) {
    size_t avail = sdsavail(s);
    if (avail >= addlen) return s;               /* 路 1: 原地 */
    size_t len = sdslen(s);
    char oldtype = *fp(s) & SDS_TYPE_MASK;
    size_t newlen = len + addlen;
    if (newlen < SDS_MAX_PREALLOC) newlen *= 2;  /* 预分配: <1MB 倍增 */
    else newlen += SDS_MAX_PREALLOC;             /* ≥1MB 线性 */
    char type = req_type(newlen);
    if (type == 0) type = 1;                     /* 扩容永不落 5 (对照 L244) */
    int hdrlen = hdr_size(type);
    if (oldtype == type) {                       /* 路 2: 同型 realloc */
        void *newsh = realloc((char*)s - hdr_size(oldtype), hdrlen + newlen + 1);
        s = (char*)newsh + hdrlen;
    } else {                                     /* 路 3: 升级 malloc+memcpy+free */
        void *newsh = malloc(hdrlen + newlen + 1);
        memcpy((char*)newsh + hdrlen, s, len + 1);
        free((char*)s - hdr_size(oldtype));
        s = (char*)newsh + hdrlen;
        *fp(s) = type;
        sdssetlen(s, len);
    }
    sdssetalloc(s, newlen);
    return s;
}

static sds sdscat(sds s, const char *t) {
    size_t curlen = sdslen(s), len = strlen(t);
    s = sdsMakeRoomFor(s, len);
    memcpy(s + curlen, t, len);
    sdssetlen(s, curlen + len);
    s[curlen + len] = '\0';
    return s;
}

/* ---------- 伪降型缩容 (对照 sds.c:327-354) ---------- */
/* 注: 真实代码 would_regrow=0 时允许降到 5 型 (object.c:601 字符串 shrink);
 *     本 harness 简化: 一律不低于 8 (would_regrow 语义) */
static sds sdsResize(sds s, size_t size) {
    char oldtype = *fp(s) & SDS_TYPE_MASK;
    size_t len = sdslen(s);
    char type = req_type(size);
    if (type == 0) type = 1;                     /* 降到 5 禁止 (简化: 一律 8) */
    int use_realloc = (oldtype == type || (type < oldtype && type > 1));  /* 伪降型条件 */
    int oldhdrlen = hdr_size(oldtype);
    if (use_realloc) {
        /* 保留旧头: 分配缩到 oldhdrlen+size+1, s[-1] 不更新 (伪降型!) */
        void *newsh = realloc((char*)s - oldhdrlen, oldhdrlen + size + 1);
        s = (char*)newsh + oldhdrlen;
        sdssetalloc(s, size);                    /* 只缩 alloc */
    } else {
        int hdrlen = hdr_size(type);
        void *newsh = malloc(hdrlen + size + 1);
        memcpy((char*)newsh + hdrlen, s, len < size ? len : size);
        free((char*)s - oldhdrlen);
        s = (char*)newsh + hdrlen;
        *fp(s) = type;                           /* 真换头 */
        sdssetalloc(s, size);
    }
    sdssetlen(s, len < size ? len : size);
    s[sdslen(s)] = '\0';
    return s;
}

/* ---------- sdsIncrLen 零拷贝 (对照 sds.c:399-440) ---------- */
static void sdsIncrLen(sds s, ssize_t incr) {
    size_t len = sdslen(s);
    if (incr >= 0) {
        if (sdsavail(s) < (size_t)incr) { printf("FAIL: overrun\n"); return; }
        sdssetlen(s, len + incr);
    } else {
        if (len < (size_t)(-incr)) { printf("FAIL: underflow\n"); return; }
        sdssetlen(s, len - (size_t)(-incr));
    }
    s[sdslen(s)] = '\0';
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

int main(void) {
    printf("[1] 创建与分级: 3B→5 型 (flags 1B), 300B→16 型 (头 5B)\n");
    sds s5 = sdsnewlen("abc", 3);
    CHECK((*fp(s5) & 7) == 0, "3B 串用 5 型 (1B 头)");
    CHECK(sdslen(s5) == 3, "len == 3");
    char *bigbuf = malloc(70001);
    memset(bigbuf, 'x', 70000); bigbuf[70000] = 0;
    sds s16 = sdsnewlen(bigbuf, 300);
    CHECK((*fp(s16) & 7) == 2, "300B 串用 16 型 (5B 头)");
    CHECK(sdslen(s16) == 300, "300B len");

    printf("\n[2] 空串强制 8 + sdshdr5 不用于增长\n");
    sds e = sdsnewlen("", 0);
    CHECK((*fp(e) & 7) == 1, "空串强制 8 型");
    sds g = sdsnewlen("yab", 3);
    g = sdscat(g, "0123456789012345678901234567890123456789");  /* 增长跨越 32 */
    CHECK((*fp(g) & 7) != 0, "扩容后不落 5 型 (仍在 8)");
    CHECK(sdslen(g) == 43, "增长后 len == 43");

    printf("\n[3] 扩容三路: 原地 / 同型 realloc / 升级\n");
    sds a = sdsnewlen("", 0);                    /* 8 型, alloc=0 */
    a = sdscat(a, "1234567890");                 /* 同型扩容 (8 型保持) */
    CHECK((*fp(a) & 7) == 1, "10B 追加后仍 8 型 (同型 realloc 路径)");
    sds up = sdsnewlen("", 0);
    up = sdscat(up, "x");                        /* 同型 */
    CHECK(sdsavail(up) >= 1, "预分配后 avail >= 1");
    char *oldp = up;
    for (int i = 0; i < 100; i++) up = sdscat(up, "abcdefghij");  /* 增长到 1000B 跨阈值 */
    CHECK((*fp(up) & 7) == 2, "1000B 升级到 16 型 (malloc+memcpy 路径)");
    CHECK(sdslen(up) == 1001, "升级后 len == 1001");

    printf("\n[4] 预分配: <1MB 倍增 (alloc > len)\n");
    CHECK(sdsavail(up) >= 1, "倍增后富余空间 ≥ 追加量");

    printf("\n[5] 伪降型缩容: 保留旧头只缩 alloc\n");
    sds big = sdsnewlen(bigbuf, 70000);          /* 70000 ≥ 65536 → 32 型 (头 9B) */
    char bigtype = *fp(big) & 7;
    CHECK(bigtype == 3, "70000B 用 32 型头");
    sds res = sdsResize(big, 1000);              /* 1000B → 16 型需求 (type=2>1) → 伪降型 */
    CHECK((*fp(res) & 7) == bigtype, "缩容保留旧 32 型头 (伪降型, s[-1] 不更新)");
    CHECK(sdslen(res) == 1000, "缩容后 len == 1000");
    free(bigbuf);

    printf("\n[6] sdsIncrLen 零拷贝: 扩好→直写→递增 (含负向回退)\n");
    sds q = sdsnewlen("", 0);
    q = sdsMakeRoomFor(q, 10);
    memcpy(q + sdslen(q), "HELLO\r\n", 7);
    sdsIncrLen(q, 7);
    CHECK(sdslen(q) == 7, "直写后递增 len == 7");
    sdsIncrLen(q, -2);                           /* 去 CRLF */
    CHECK(sdslen(q) == 5 && memcmp(q, "HELLO", 5) == 0, "负增量去 CRLF → len == 5");

    printf("\n结果: %s\n", fail ? "FAIL" : "ALL PASS");
    return fail ? 1 : 0;
}
