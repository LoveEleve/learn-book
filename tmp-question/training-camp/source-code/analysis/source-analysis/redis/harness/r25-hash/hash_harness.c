/*
 * R-25 t_hash harness — 编码三态 + 转换阈值 + HFE 惰性链极简复现
 * 还原核心机制 (不依赖真实 listpack/dict/ebuckets):
 *  1. 编码三态: LISTPACK(两元素组) / LISTPACK_EX(三元素组) / HT (t_hash.c:855-977)
 *  2. 转换触发: 512 字段/64 值/lpSafeToAdd 总量 (L594-623)
 *  3. GETF 惰性链: OK/EXPIRED/EXPIRED_HASH 三态 + 级联删键 (L711-779)
 *  4. 条件 TTL: GT/LT/NX/XX 矩阵 + 无 TTL 视为无限 (L979-1060)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness hash_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

#define OBJ_ENCODING_LISTPACK 1
#define OBJ_ENCODING_LISTPACK_EX 2
#define OBJ_ENCODING_HT 3
#define HASH_MAX_ENTRIES 512
#define HASH_MAX_VALUE 64
#define HASH_LP_NO_TTL 0
#define EB_EXPIRE_TIME_INVALID 0xFFFFFFFFFFFFFFFFULL

/* ---- 简化 hash 对象: 字段数组 (f/v/ttl 三元) ---- */
typedef struct {
    char *field, *value;
    uint64_t ttl;          /* HASH_LP_NO_TTL = 无 */
    int encoding;          /* 模拟编码 */
} Field;
typedef struct {
    Field *fields;
    int count, cap;
    int encoding;
} HashObj;

static HashObj *hashCreate(void) {
    HashObj *h = calloc(1, sizeof(HashObj));
    h->encoding = OBJ_ENCODING_LISTPACK;
    return h;
}
static Field *hashFind(HashObj *h, const char *f) {
    for (int i = 0; i < h->count; i++)
        if (strcmp(h->fields[i].field, f) == 0) return &h->fields[i];
    return NULL;
}

/* 转换触发 (对照 t_hash.c:594-623): 字段数/单值/总量 */
static void tryConversion(HashObj *h) {
    if (h->encoding == OBJ_ENCODING_HT) return;
    if (h->count > HASH_MAX_ENTRIES) { h->encoding = OBJ_ENCODING_HT; return; }
    for (int i = 0; i < h->count; i++)
        if (strlen(h->fields[i].value) > HASH_MAX_VALUE) { h->encoding = OBJ_ENCODING_HT; return; }
}

/* hashTypeSet (对照 L855-977 简化): 写入 + 编码内行为 */
static int hashSet(HashObj *h, const char *f, const char *v, int keep_ttl) {
    tryConversion(h);
    Field *de = hashFind(h, f);
    int update = de != NULL;
    if (!de) {
        h->fields = realloc(h->fields, sizeof(Field) * (h->count + 1));
        de = &h->fields[h->count++];
        de->field = strdup(f); de->ttl = HASH_LP_NO_TTL; de->value = NULL;
    } else if (!keep_ttl) {
        de->ttl = HASH_LP_NO_TTL;   /* 覆盖清 TTL (对照 L920-922) */
    }
    free(de->value);
    de->value = strdup(v);
    if (de->ttl != HASH_LP_NO_TTL && h->encoding == OBJ_ENCODING_LISTPACK)
        h->encoding = OBJ_ENCODING_LISTPACK_EX;  /* 带 TTL 字段 → EX 编码 (模拟) */
    tryConversion(h);
    return update;
}

/* GETF 惰性链 (对照 L711-779 简化) */
typedef enum { GETF_OK, GETF_NOT_FOUND, GETF_EXPIRED, GETF_EXPIRED_HASH } GetFieldRes;
typedef struct {
    int masterhost;     /* 从库? */
    int from_master;    /* CLIENT_MASTER? */
    long long now;
    int hash_deleted;   /* 输出: 空 hash 删键 */
} Ctx;

static GetFieldRes hashGetValue(HashObj *h, const char *f, Ctx *ctx, char **vout) {
    Field *de = hashFind(h, f);
    if (!de) return GETF_NOT_FOUND;
    if (de->ttl != HASH_LP_NO_TTL && (uint64_t)ctx->now >= de->ttl) {
        /* 过期判定 (L737) */
        if (ctx->masterhost) {
            if (ctx->from_master) return GETF_OK;      /* CLIENT_MASTER 视为有效 (L742) */
            return GETF_EXPIRED;                        /* 从库只报不删 (L746) */
        }
        /* 主库删除链 (L760-778) */
        free(de->field); free(de->value);
        memmove(&h->fields[de - h->fields], &h->fields[de - h->fields + 1],
                sizeof(Field) * (h->count - (de - h->fields) - 1));
        h->count--;
        if (h->count == 0) { ctx->hash_deleted = 1; return GETF_EXPIRED_HASH; }  /* 级联删键 (L770-775) */
        return GETF_EXPIRED;
    }
    *vout = de->value;
    return GETF_OK;
}

/* 条件 TTL (对照 L979-1060 简化): GT/LT/NX/XX */
#define HFE_NX 1
#define HFE_XX 2
#define HFE_GT 4
#define HFE_LT 8
static int setFieldExpiry(HashObj *h, const char *f, uint64_t expireAt, int cond, long long now) {
    Field *de = hashFind(h, f);
    if (!de) return 0;
    if (de->ttl == HASH_LP_NO_TTL) {
        /* 无 TTL 视为无限: XX|GT 拒 (L997-1000), LT/NX 通过 */
        if (cond & (HFE_XX | HFE_GT)) return 0;
    } else {
        if ((cond == HFE_GT && de->ttl >= expireAt) ||
            (cond == HFE_LT && de->ttl <= expireAt) ||
            (cond == HFE_NX)) return 0;
    }
    if (expireAt <= (uint64_t)now) {
        /* 已过期删字段 (对照 L1044-1051: propagateHashFieldDeletion + hashTypeDelete) */
        free(de->field); free(de->value);
        memmove(&h->fields[de - h->fields], &h->fields[de - h->fields + 1],
                sizeof(Field) * (h->count - (de - h->fields) - 1));
        h->count--;
        return -1;
    }
    de->ttl = expireAt;
    if (h->encoding == OBJ_ENCODING_LISTPACK)
        h->encoding = OBJ_ENCODING_LISTPACK_EX;
    return 1;
}

/* 清理 */
static void hashFree(HashObj *h) {
    for (int i = 0; i < h->count; i++) { free(h->fields[i].field); free(h->fields[i].value); }
    free(h->fields);
    free(h);
}

static int tests = 0, passed = 0;
#define CHECK(cond) do { tests++; if (cond) { passed++; printf("  [PASS] %s\n", #cond); } \
    else { printf("  [FAIL] %s (line %d)\n", #cond, __LINE__); } } while (0)

int main(void) {
    printf("=== R-25 t_hash harness (gcc+ASan) ===\n");

    printf("\n[1] 编码三态: 写入 + 升级 (t_hash.c:855-977)\n");
    HashObj *h = hashCreate();
    CHECK(h->encoding == OBJ_ENCODING_LISTPACK);
    CHECK(hashSet(h, "f1", "v1", 0) == 0);      /* 新建 */
    CHECK(hashSet(h, "f1", "v2", 0) == 1);      /* 更新 */
    CHECK(strcmp(hashFind(h, "f1")->value, "v2") == 0);
    CHECK(h->count == 1);

    printf("\n[2] 转换触发: 512 字段 / 64 值 (config.c:3215,3221)\n");
    HashObj *h2 = hashCreate();
    char buf[80];
    for (int i = 0; i < HASH_MAX_ENTRIES; i++) {
        snprintf(buf, sizeof(buf), "field_%d", i);
        hashSet(h2, buf, "x", 0);
    }
    CHECK(h2->encoding == OBJ_ENCODING_LISTPACK);    /* 恰 512 未超 */
    hashSet(h2, "field_512", "y", 0);
    CHECK(h2->encoding == OBJ_ENCODING_HT);          /* 513 → HT */
    HashObj *h3 = hashCreate();
    char big[100];
    memset(big, 'a', sizeof(big));
    big[sizeof(big)-1] = '\0';
    hashSet(h3, "big", big, 0);                      /* 99 > 64 */
    CHECK(h3->encoding == OBJ_ENCODING_HT);

    printf("\n[3] GETF 惰性链: 主库删字段 + 级联删键 (t_hash.c:711-779)\n");
    HashObj *h4 = hashCreate();
    Ctx ctx = {0};
    ctx.now = 1000;
    hashSet(h4, "exp", "v", 0);
    hashSet(h4, "keep", "v", 0);                 /* 第二字段: 删除 exp 后 hash 仍在 */
    h4->fields[0].ttl = 500;                     /* 直接构造已过期字段 (绕过 SetEx 的删字段) */
    char *vout;
    CHECK(hashGetValue(h4, "exp", &ctx, &vout) == GETF_EXPIRED);
    CHECK(hashFind(h4, "exp") == NULL);              /* 主库已删字段 */
    CHECK(h4->count == 1 && hashFind(h4, "keep") != NULL);  /* 剩余字段保留 */
    /* 级联: 最后一个字段过期 → hash 删除 */
    HashObj *h5 = hashCreate();
    hashSet(h5, "only", "v", 0);
    h5->fields[0].ttl = 500;                     /* 直接构造已过期 */
    ctx.hash_deleted = 0;
    CHECK(hashGetValue(h5, "only", &ctx, &vout) == GETF_EXPIRED_HASH);
    CHECK(ctx.hash_deleted == 1);
    CHECK(h5->count == 0);

    printf("\n[4] 从库语义: 只报不删 (L740-747)\n");
    HashObj *h6 = hashCreate();
    Ctx slave = {0};
    slave.masterhost = 1; slave.now = 1000;
    hashSet(h6, "f", "v", 0);
    h6->fields[0].ttl = 500;                     /* 直接构造已过期 */
    CHECK(hashGetValue(h6, "f", &slave, &vout) == GETF_EXPIRED);
    CHECK(hashFind(h6, "f") != NULL);                /* 从库不删 */
    slave.from_master = 1;                            /* CLIENT_MASTER */
    CHECK(hashGetValue(h6, "f", &slave, &vout) == GETF_OK);  /* master 命令视为有效 */

    printf("\n[5] 条件 TTL 矩阵: GT/LT/NX/XX + 无 TTL 视为无限 (L979-1060)\n");
    HashObj *h7 = hashCreate();
    Ctx c7 = {0}; c7.now = 1000;
    hashSet(h7, "f", "v", 0);                         /* 无 TTL */
    CHECK(setFieldExpiry(h7, "f", 2000, HFE_XX, c7.now) == 0);  /* XX: 无 TTL 拒 */
    CHECK(setFieldExpiry(h7, "f", 2000, HFE_GT, c7.now) == 0);  /* GT: 无 TTL 视为无限拒 */
    CHECK(setFieldExpiry(h7, "f", 2000, HFE_NX, c7.now) == 1);  /* NX 通过 */
    CHECK(h7->fields[0].ttl == 2000);
    /* 有 TTL 后 */
    CHECK(setFieldExpiry(h7, "f", 1500, HFE_GT, c7.now) == 0);  /* GT: 1500 < 2000 拒 */
    CHECK(setFieldExpiry(h7, "f", 3000, HFE_GT, c7.now) == 1);  /* GT: 3000 > 2000 通过 */
    CHECK(setFieldExpiry(h7, "f", 2500, HFE_LT, c7.now) == 1);  /* LT: 2500 < 3000 通过 */
    CHECK(setFieldExpiry(h7, "f", 2600, HFE_NX, c7.now) == 0);  /* NX: 已有 TTL 拒 */
    /* 已过期时间戳 → 删字段 */
    CHECK(setFieldExpiry(h7, "f", 500, HFE_LT, c7.now) == -1);  /* 过期删 (L1044-1051) */
    CHECK(hashFind(h7, "f") == NULL);

    printf("\n[6] KEEP_TTL: 覆盖保留 TTL (t_hash.c:918-919)\n");
    HashObj *h8 = hashCreate();
    Ctx c8 = {0}; c8.now = 1000;
    hashSet(h8, "f", "v1", 0);
    setFieldExpiry(h8, "f", 2000, 0, c8.now);
    hashSet(h8, "f", "v2", 1);                        /* KEEP_TTL */
    CHECK(hashFind(h8, "f")->ttl == 2000);            /* TTL 保留 */
    CHECK(strcmp(hashFind(h8, "f")->value, "v2") == 0);
    hashSet(h8, "f", "v3", 0);                        /* 不带 KEEP_TTL */
    CHECK(hashFind(h8, "f")->ttl == HASH_LP_NO_TTL);  /* TTL 清除 (L920-922) */

    printf("\n=== 结果: %d/%d PASS ===\n", passed, tests);

    hashFree(h); hashFree(h2); hashFree(h3); hashFree(h4); hashFree(h5);
    hashFree(h6); hashFree(h7); hashFree(h8);
    return passed == tests ? 0 : 1;
}
