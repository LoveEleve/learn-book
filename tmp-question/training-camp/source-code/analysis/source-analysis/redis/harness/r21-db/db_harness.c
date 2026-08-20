/*
 * R-21 db 键空间 harness — 键空间操作 + kvstore 分片 + ebuckets 极简复现
 * 还原核心机制 (不引入完整 dict/rax 依赖):
 *  1. keyHashSlot 语义: crc16 & 0x3FFF (14bit) + {tag} 只哈希中间
 *  2. kvstore 游标编码: 高 48 位 dictScan + 低 num_dicts_bits 位 didx
 *  3. expires 键共享: setExpire 复用主 dict key 指针 (零拷贝)
 *  4. 惰性过期三态: 主库 KEY_DELETED / 从库 KEY_EXPIRED / master 客户端 KEY_VALID
 *  5. FAIR 随机: Fenwick 树选桶概率 ∝ 元素数
 *  6. ebuckets: list(≤16) → rax 转换阈值 + 批量过期 + ACT_UPDATE 重插
 *  7. 三表一致性: 删除键时 keys/expires 同步
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness db_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

#define CLUSTER_SLOT_MASK_BITS 14
#define CLUSTER_SLOTS (1<<CLUSTER_SLOT_MASK_BITS)
#define SLOT_MASK ((unsigned long long)(CLUSTER_SLOTS-1))

/* ---- 1. keyHashSlot 语义 (对照 cluster.h:43-60, crc16 用简单 FNV 模拟) ---- */
static uint64_t crc16(const char *key, int keylen) {
    uint64_t h = 0x811c9dc5;
    for (int i = 0; i < keylen; i++) { h ^= key[i]; h *= 0x01000193; }
    return h;
}
static unsigned int keyHashSlot(char *key, int keylen) {
    int s, e;
    for (s = 0; s < keylen; s++) if (key[s] == '{') break;
    if (s == keylen) return crc16(key, keylen) & SLOT_MASK;
    for (e = s+1; e < keylen; e++) if (key[e] == '}') break;
    if (e == keylen || e == s+1) return crc16(key, keylen) & SLOT_MASK;
    return crc16(key+s+1, e-s-1) & SLOT_MASK;
}

/* ---- 2. kvstore 极简: dict 数组 + 每 dict 链式桶 (键共享指针) ---- */
typedef struct KItem {
    char *key;               /* 由主 dict 持有 (expires 共享指针) */
    long long expire;        /* -1 = 无 TTL */
    int in_expires;          /* 是否已注册 expires 表 */
    struct KItem *next;
} KItem;

typedef struct {
    KItem **buckets;
    unsigned long size;      /* 元素数 */
    unsigned long mask;      /* 桶掩码 */
} KDict;

typedef struct {
    int num_dicts_bits;
    int num_dicts;
    KDict *dicts;
    unsigned long long key_count;
    unsigned long long *dict_size_index;   /* Fenwick 树 (BIT) */
} KVStore;

static unsigned long dictHash(const char *key) {
    unsigned long h = 5381; const unsigned char *p = (const unsigned char*)key;
    while (*p) h = (h<<5) + h + *p++;
    return h & 0xFFFF;
}
static void dictCreate(KDict *d) { d->mask = 15; d->size = 0; d->buckets = calloc(16, sizeof(KItem*)); }
static KItem *dictFind(KDict *d, const char *key) {
    KItem *it = d->buckets[dictHash(key) & d->mask];
    while (it && strcmp(it->key, key)) it = it->next;
    return it;
}
static KItem *dictAdd(KDict *d, const char *key) {
    KItem *it = calloc(1, sizeof(KItem));
    it->key = strdup(key); it->expire = -1;
    unsigned long idx = dictHash(key) & d->mask;
    it->next = d->buckets[idx]; d->buckets[idx] = it;
    d->size++;
    if (d->size > d->mask * 2) { /* 简单扩容 */
        unsigned long newmask = d->mask*2+1; KItem **nb = calloc(newmask+1, sizeof(KItem*));
        for (int i = 0; i <= (int)d->mask; i++) {
            KItem *it2 = d->buckets[i];
            while (it2) { KItem *nxt = it2->next; unsigned long ni = dictHash(it2->key) & newmask;
                it2->next = nb[ni]; nb[ni] = it2; it2 = nxt; }
        }
        free(d->buckets); d->buckets = nb; d->mask = newmask;
    }
    return it;
}
static int dictDelete(KDict *d, const char *key, int free_key) {
    unsigned long idx = dictHash(key) & d->mask;
    KItem **pp = &d->buckets[idx];
    while (*pp && strcmp((*pp)->key, key)) pp = &(*pp)->next;
    if (!*pp) return 0;
    KItem *del = *pp; *pp = del->next;
    if (free_key) free(del->key);   /* 主 dict: 键归它释放 */
    free(del); d->size--;           /* expires 表: key destructor = NULL 不释放 (server.c:505) */
    return 1;
}

/* BIT 更新 (对照 kvstore.c:122-145) */
static void cumulativeKeyCountAdd(KVStore *kvs, int didx, long delta) {
    kvs->key_count += delta;
    if (kvs->num_dicts == 1) return;
    int idx = didx + 1;
    while (idx <= kvs->num_dicts) { kvs->dict_size_index[idx] += delta; idx += (idx & -idx); }
}
static unsigned long long cumulativeKeyCountRead(KVStore *kvs, int didx) {
    if (kvs->num_dicts == 1) return kvs->key_count;
    int idx = didx + 1; unsigned long long sum = 0;
    while (idx > 0) { sum += kvs->dict_size_index[idx]; idx -= (idx & -idx); }
    return sum;
}
/* 对照 kvstore.c:500-523 — BIT 二分定位第 target 个键所在 dict */
static int findDictIndexByKeyIndex(KVStore *kvs, unsigned long target) {
    if (kvs->num_dicts == 1 || kvs->key_count == 0) return 0;
    int result = 0, bit_mask = 1 << kvs->num_dicts_bits;
    for (int i = bit_mask; i != 0; i >>= 1) {
        int current = result + i;
        if (target > kvs->dict_size_index[current]) { target -= kvs->dict_size_index[current]; result = current; }
    }
    return result;
}
static KVStore *kvstoreCreate(int bits) {
    KVStore *kvs = calloc(1, sizeof(KVStore));
    kvs->num_dicts_bits = bits;
    kvs->num_dicts = 1 << bits;
    kvs->dicts = calloc(kvs->num_dicts, sizeof(KDict));
    for (int i = 0; i < kvs->num_dicts; i++) dictCreate(&kvs->dicts[i]);
    if (kvs->num_dicts > 1) kvs->dict_size_index = calloc(kvs->num_dicts+1, sizeof(unsigned long long));
    return kvs;
}
/* 释放 (对照 kvstoreRelease kvstore.c:294-311) — free_keys=1 仅主 dict 释放共享键 */
static void kvstoreRelease(KVStore *kvs, int free_keys) {
    for (int i = 0; i < kvs->num_dicts; i++) {
        for (int b = 0; b <= (int)kvs->dicts[i].mask; b++) {
            KItem *it = kvs->dicts[i].buckets[b];
            while (it) { KItem *nxt = it->next; if (free_keys) free(it->key); free(it); it = nxt; }
        }
        free(kvs->dicts[i].buckets);
    }
    free(kvs->dicts);
    free(kvs->dict_size_index);
    free(kvs);
}
static int getKeySlot(const char *key, int cluster_enabled) {
    return cluster_enabled ? keyHashSlot((char*)key, strlen(key)) : 0;
}

/* 对照 db.c:180-195 — 键入主表, 同步记账 */
static KItem *dbAdd(KVStore *keys, int slot, const char *key, KItem **existing) {
    KItem *ex = dictFind(&keys->dicts[slot], key);
    if (existing && ex) { *existing = ex; return ex; }
    if (ex) { *existing = ex; return ex; }
    KItem *de = dictAdd(&keys->dicts[slot], key);
    cumulativeKeyCountAdd(keys, slot, 1);
    if (existing) *existing = NULL;
    return de;
}

static void dictAddRawNoDup(KDict *d, KItem *it) {
    unsigned long idx = dictHash(it->key) & d->mask;
    it->next = d->buckets[idx]; d->buckets[idx] = it;
    d->size++;
}

/* 对照 db.c:1846-1863 — 键复用主 dict 指针 (零拷贝) */
static void setExpire(KVStore *keys, KVStore *expires, int slot, KItem *kde, long long when) {
    KItem *de = dictFind(&expires->dicts[slot], kde->key);
    if (!de) {
        KItem *k = calloc(1, sizeof(KItem));
        k->key = kde->key;   /* 共享主 dict 的 key 指针! */
        k->expire = when; k->in_expires = 1;
        dictAddRawNoDup(&expires->dicts[slot], k);
        cumulativeKeyCountAdd(expires, slot, 1);
    } else {
        de->expire = when;
    }
}

/* 对照 db.c:1928-1942 — 逻辑过期判定 */
static int keyIsExpired(KVStore *expires, const char *key, long long now) {
    int slot = 0;
    KItem *de = dictFind(&expires->dicts[slot], key);
    if (!de || de->expire < 0) return 0;
    return now > de->expire;
}
/* 主库/从库语义 (对照 db.c:1974-2017) */
typedef enum { KEY_VALID, KEY_EXPIRED, KEY_DELETED } keyStatus;
static keyStatus expireIfNeeded(KVStore *keys, KVStore *expires, const char *key,
                                long long now, int masterhost, int force, int from_master_client) {
    if (!keyIsExpired(expires, key, now)) return KEY_VALID;
    if (masterhost) {
        if (from_master_client) return KEY_VALID;         /* master 同步的命令永不过期 */
        if (!force) return KEY_EXPIRED;                   /* 从库只报过期不删 */
    }
    /* 删除: 先 expires 后 keys — expires 不释放键 (key destructor=NULL), 安全 */
    int slot = 0;
    dictDelete(&expires->dicts[slot], key, 0);
    cumulativeKeyCountAdd(expires, slot, -1);
    dictDelete(&keys->dicts[slot], key, 1);
    cumulativeKeyCountAdd(keys, slot, -1);
    return KEY_DELETED;
}

/* 对照 kvstore.c:102-117 — 游标编码 */
static void addDictIndexToCursor(KVStore *kvs, int didx, unsigned long long *cursor) {
    if (kvs->num_dicts == 1) return;
    if (didx < 0) return;
    *cursor = (*cursor << kvs->num_dicts_bits) | didx;
}
static int getAndClearDictIndexFromCursor(KVStore *kvs, unsigned long long *cursor) {
    if (kvs->num_dicts == 1) return 0;
    int didx = (int)(*cursor & (kvs->num_dicts-1));
    *cursor = *cursor >> kvs->num_dicts_bits;
    return didx;
}

/* ---- 3. ebuckets 极简: 有序链表 (每 16 项一个 segment 计数) ---- */
typedef struct EbItem { long long ttl; int update_me; struct EbItem *next; } EbItem;
typedef struct { EbItem *head; int total; int segments; int is_rax; } Ebuckets;
#define EB_SEG_MAX_ITEMS 16

static void ebAddSorted(Ebuckets *eb, EbItem *item) {
    EbItem **pp = &eb->head;
    while (*pp && (*pp)->ttl <= item->ttl) pp = &(*pp)->next;
    item->next = *pp; *pp = item;
    eb->total++;
    if (eb->total > EB_SEG_MAX_ITEMS) eb->is_rax = 1;   /* 阈值: list → rax (对照 ebuckets.c:62-63,528) */
    eb->segments = (eb->total + EB_SEG_MAX_ITEMS - 1) / EB_SEG_MAX_ITEMS;
}
static EbItem *ebAdd(Ebuckets *eb, long long ttl) {
    EbItem *it = calloc(1, sizeof(EbItem));
    it->ttl = ttl;
    ebAddSorted(eb, it);
    return it;
}
static int ebExpire(Ebuckets *eb, long long now, long long *nextExpireTime) {
    int expired = 0;
    *nextExpireTime = -1;
    while (eb->head && eb->head->ttl <= now) {
        EbItem *h = eb->head;
        if (h->update_me) {           /* ACT_UPDATE_EXP_ITEM: 重插 (对照 ebuckets.c:704-707,1530-1544) */
            eb->head = h->next; eb->total--; eb->segments = (eb->total+EB_SEG_MAX_ITEMS-1)/EB_SEG_MAX_ITEMS;
            long long newTtl = h->ttl + 100;
            if (newTtl > now) { h->ttl = newTtl; h->update_me = 0; ebAddSorted(eb, h);
                                 if (*nextExpireTime < 0 || newTtl < *nextExpireTime) *nextExpireTime = newTtl; }
            else { free(h); expired++; }
        } else {
            eb->head = h->next; free(h); expired++; eb->total--;
            eb->segments = (eb->total+EB_SEG_MAX_ITEMS-1)/EB_SEG_MAX_ITEMS;
        }
    }
    if (eb->head) { if (*nextExpireTime < 0 || eb->head->ttl < *nextExpireTime) *nextExpireTime = eb->head->ttl; }
    if (eb->total == 0) eb->is_rax = 0;
    return expired;
}

static void ebFree(Ebuckets *eb) {
    while (eb->head) { EbItem *h = eb->head; eb->head = h->next; free(h); }
    eb->total = 0; eb->segments = 0; eb->is_rax = 0;
}

static int tests = 0, passed = 0;
#define CHECK(cond) do { tests++; if (cond) { passed++; printf("  [PASS] %s\n", #cond); } \
    else { printf("  [FAIL] %s (line %d)\n", #cond, __LINE__); } } while (0)

int main(void) {
    printf("=== R-21 db 键空间 harness (gcc+ASan) ===\n");

    printf("\n[1] keyHashSlot 语义 (cluster.h:43-60)\n");
    unsigned int s1 = keyHashSlot((char*)"user:1000", 9);
    unsigned int s2 = keyHashSlot((char*)"{user:1000}", 11);
    unsigned int s3 = keyHashSlot((char*)"a{1000}.b", 8);
    CHECK(s1 == (crc16("user:1000", 9) & SLOT_MASK));
    CHECK(s2 == (crc16("user:1000", 9) & SLOT_MASK));      /* 无 {} 与 {whole} 同槽 */
    CHECK(s3 == (crc16("1000", 4) & SLOT_MASK));           /* {tag} 只哈希中间 */
    CHECK(s3 != (crc16("a{1000}.b", 8) & SLOT_MASK));      /* tag 改变槽 */
    CHECK(keyHashSlot((char*)"{bar", 4) == (crc16("{bar", 4) & SLOT_MASK));  /* 无 } 退全键 */

    printf("\n[2] kvstore 游标编码 48+bits (kvstore.c:102-117)\n");
    KVStore *kvs = kvstoreCreate(2);   /* 4 dicts */
    unsigned long long cur = 0;
    addDictIndexToCursor(kvs, 3, &cur);
    CHECK(cur == 3);
    addDictIndexToCursor(kvs, 3, &cur);
    CHECK((cur >> 2) == 3);            /* didx 3 移入高段 */
    int didx = getAndClearDictIndexFromCursor(kvs, &cur);
    CHECK(didx == 3 && cur == 3);      /* 往返一致 */
    KVStore *kvs1 = kvstoreCreate(0);  /* 单 dict: 不编码 */
    addDictIndexToCursor(kvs1, 0, &cur);
    CHECK(cur == 3);                   /* 单 dict 游标不碰 */

    printf("\n[3] 分片寻址: cluster 14bit 键落槽 (getKeySlot db.c:210)\n");
    CHECK(getKeySlot("foo", 0) == 0);                      /* 非 cluster 恒 0 */
    CHECK(getKeySlot("foo", 1) == keyHashSlot((char*)"foo", 3));
    CHECK(getKeySlot("foo", 1) < CLUSTER_SLOTS);           /* 14bit 掩码内 */

    printf("\n[4] expires 键共享零拷贝 (db.c:1846-1863)\n");
    KVStore *keys = kvstoreCreate(0), *expires = kvstoreCreate(0);
    KItem *kde = dbAdd(keys, 0, "k1", NULL);
    setExpire(keys, expires, 0, kde, 1000);
    KItem *ed = dictFind(&expires->dicts[0], "k1");
    CHECK(ed != NULL);
    CHECK(ed->key == kde->key);        /* 指针共享, 无 sds 副本 */
    CHECK(ed->expire == 1000);
    CHECK(kvs1->key_count == 0 && keys->key_count == 1 && expires->key_count == 1);

    printf("\n[5] 惰性过期三态 (db.c:1974-2017)\n");
    /* 主库: 过期键被删除 */
    keyStatus st = expireIfNeeded(keys, expires, "k1", 2000, 0, 0, 0);
    CHECK(st == KEY_DELETED);
    CHECK(dictFind(&keys->dicts[0], "k1") == NULL);        /* 主表已删 */
    CHECK(dictFind(&expires->dicts[0], "k1") == NULL);     /* 先删 expires */
    CHECK(keys->key_count == 0 && expires->key_count == 0);
    /* 从库: 只报过期不删 */
    kde = dbAdd(keys, 0, "k2", NULL);
    setExpire(keys, expires, 0, kde, 1000);
    st = expireIfNeeded(keys, expires, "k2", 2000, 1, 0, 0);
    CHECK(st == KEY_EXPIRED);
    CHECK(dictFind(&keys->dicts[0], "k2") != NULL);        /* 从库不删 (等 master DEL) */
    /* master 客户端: 永不过期 */
    st = expireIfNeeded(keys, expires, "k2", 2000, 1, 0, 1);
    CHECK(st == KEY_VALID);
    /* 写操作强制删除 (LOOKUP_WRITE + 可写从库) */
    st = expireIfNeeded(keys, expires, "k2", 2000, 1, 1, 0);
    CHECK(st == KEY_DELETED);
    /* 未过期 */
    kde = dbAdd(keys, 0, "k3", NULL);
    setExpire(keys, expires, 0, kde, 3000);
    CHECK(expireIfNeeded(keys, expires, "k3", 2000, 0, 0, 0) == KEY_VALID);
    CHECK(dictFind(&keys->dicts[0], "k3") != NULL);

    printf("\n[6] FAIR 随机: BIT 树选桶概率 ∝ 元素数 (kvstore.c:431-434,500-523)\n");
    KVStore *fk = kvstoreCreate(2);
    for (int i = 0; i < 1; i++)  dbAdd(fk, 0, "a0", NULL);   /* dict0: 1 键 */
    for (int i = 0; i < 9; i++) { char b[8]; snprintf(b, 8, "b%d", i); dbAdd(fk, 1, b, NULL); } /* dict1: 9 键 */
    CHECK(cumulativeKeyCountRead(fk, 0) == 1);
    CHECK(cumulativeKeyCountRead(fk, 1) == 10);
    int target = findDictIndexByKeyIndex(fk, 10);            /* 第 10 个键在 dict1 */
    CHECK(target == 1);
    target = findDictIndexByKeyIndex(fk, 1);                 /* 第 1 个键在 dict0 */
    CHECK(target == 0);
    int cnt0 = 0, cnt1 = 0;
    for (int i = 0; i < 10000; i++) {
        unsigned long t = (rand() % fk->key_count) + 1;
        if (findDictIndexByKeyIndex(fk, t) == 0) cnt0++; else cnt1++;
    }
    CHECK(cnt1 >= 8700 && cnt1 <= 9300);                     /* dict1 (9/10) ≈ 90% */
    printf("    dict0=%d dict1=%d (期望 10%%/90%%)\n", cnt0, cnt1);

    printf("\n[7] ebuckets: list→rax 阈值 16 (ebuckets.c:62-63,528)\n");
    Ebuckets eb = {0};
    for (int i = 0; i < 16; i++) ebAdd(&eb, i * 10);
    CHECK(eb.is_rax == 0 && eb.total == 16 && eb.segments == 1);
    ebAdd(&eb, 200);                                          /* 第 17 项 → 转 rax */
    CHECK(eb.is_rax == 1 && eb.total == 17);
    CHECK(eb.segments == 2);                                  /* 2 segments */

    printf("\n[8] ebuckets 批量过期 + ACT_UPDATE 重插 (ebuckets.c:1464-1549)\n");
    Ebuckets eb2 = {0};
    EbItem *up = ebAdd(&eb2, 30);  up->update_me = 1;         /* 到期且要续期 */
    for (int i = 0; i < 5; i++) ebAdd(&eb2, 10 + i * 5);      /* 10,15,20,25,30 均 ≤ now=30 */
    for (int i = 0; i < 3; i++) ebAdd(&eb2, 50 + i * 10);     /* 50,60,70 未到期 */
    long long nxt;
    int n = ebExpire(&eb2, 30, &nxt);
    CHECK(n == 5);                                            /* 5 个普通过期 + 1 个续期 (重插后 130>30) */
    CHECK(eb2.total == 4);                                    /* 3 未到期 + 1 续期 */
    CHECK(nxt == 50);                                         /* 下个最小到期 */
    CHECK(eb2.head->ttl == 50);

    printf("\n[9] 三表一致性: 删除键时 keys/expires 同步 (db.c:372-425)\n");
    KItem *k5 = dbAdd(keys, 0, "k5", NULL);
    setExpire(keys, expires, 0, k5, 5000);
    /* 删除走通用路径 (对照 dbGenericDelete: 先 expires 后 keys, expires 不释放键) */
    dictDelete(&expires->dicts[0], "k5", 0); cumulativeKeyCountAdd(expires, 0, -1);
    dictDelete(&keys->dicts[0], "k5", 1); cumulativeKeyCountAdd(keys, 0, -1);
    CHECK(keys->key_count == 1 && expires->key_count == 1);   /* k3 + k3 的 TTL 仍在 */
    CHECK(dictFind(&keys->dicts[0], "k5") == NULL);
    CHECK(dictFind(&expires->dicts[0], "k5") == NULL);

    printf("\n[10] kvstoreScan 游标跨 dict (kvstore.c:361-403)\n");
    KVStore *sk = kvstoreCreate(2);
    const char *ks[] = {"x0","x1","x2","x3","x4","x5"};
    int nks = 6, collected = 0, roundtrip = 0;
    for (int i = 0; i < nks; i++) {
        int slot = keyHashSlot((char*)ks[i], strlen(ks[i])) & 3;  /* 4 dicts 模拟 */
        KItem *de = dbAdd(sk, slot, ks[i], NULL);
        (void)de;
    }
    CHECK(sk->key_count == 6);
    /* 模拟 kvstoreScan: 每 dict 扫空 → 跳到下一非空 dict */
    unsigned long long cur2 = 0;
    unsigned long long seen = 0;
    for (int iter = 0; iter < 100 && seen != sk->key_count; iter++) {
        int d = getAndClearDictIndexFromCursor(sk, &cur2);
        if (d >= sk->num_dicts) break;
        /* dict 内全扫 (含最后桶: mask 索引范围 0..mask) */
        for (int i = 0; i <= (int)sk->dicts[d].mask; i++) {
            KItem *it = sk->dicts[d].buckets[i];
            while (it) { seen++; collected++; it = it->next; }
        }
        /* 找下一非空 dict (BIT 跳转) */
        unsigned long long next_key = cumulativeKeyCountRead(sk, d) + 1;
        if (next_key <= sk->key_count) {
            int nd = findDictIndexByKeyIndex(sk, next_key);
            cur2 = 0;
            addDictIndexToCursor(sk, nd, &cur2);
        } else {
            roundtrip = 1; break;
        }
    }
    CHECK(collected == 6);        /* 全键扫到 (迭代前存在的键必返回 — dictScan 弱语义) */
    CHECK(roundtrip == 1);        /* 游标正确走完 */

    printf("\n=== 结果: %d/%d PASS ===\n", passed, tests);

    /* 清理: 先 expires (不释放共享键) 后 keys (释放键) — 对照 dbGenericDelete 顺序 */
    kvstoreRelease(expires, 0);
    kvstoreRelease(keys, 1);
    kvstoreRelease(kvs, 1);
    kvstoreRelease(kvs1, 1);
    kvstoreRelease(fk, 1);
    kvstoreRelease(sk, 1);
    ebFree(&eb);
    ebFree(&eb2);

    return passed == tests ? 0 : 1;
}
