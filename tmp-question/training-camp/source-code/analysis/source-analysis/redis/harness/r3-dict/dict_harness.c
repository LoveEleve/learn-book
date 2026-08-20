/*
 * R-3 Dict harness — 双表渐进 rehash + 阈值 + 掩码 + 反向游标极简复现
 * 还原核心控制流 (不指针编码/不 SipHash/不 dictType 全量):
 *  1. 双表 ht[2] + rehashidx 逐桶迁移 + 完成提升
 *  2. 阈值: 1:1 扩 / <1:8 缩 (ENABLE)
 *  3. 2 幂掩码: 缩容 idx&新掩码 (免重算哈希)
 *  4. 查找联动: 目标桶未迁 → 先迁再查
 *  5. dictScan 反向游标: 扩表期间遍历不丢不重
 * 编译: gcc -O0 -o harness dict_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#define HASHTABLE_MIN_FILL 8
typedef struct entry { unsigned long key, val; struct entry *next; } entry;
typedef struct {
    entry **ht[2];
    unsigned long used[2];
    long rehashidx;          /* -1 = 不在 rehash */
    int size_exp[2];         /* 表大小 = 1<<exp */
} dict;

#define DICTHT_SIZE(exp) (1UL<<(exp))
#define DICTHT_MASK(exp) (DICTHT_SIZE(exp)-1)
#define dictIsRehashing(d) ((d)->rehashidx != -1)

static entry *entry_new(unsigned long k, unsigned long v) {
    entry *e = malloc(sizeof(entry));
    e->key = k; e->val = v; e->next = NULL;
    return e;
}
static dict *dict_new(void) {
    dict *d = calloc(1, sizeof(dict));
    d->size_exp[0] = 2;                       /* 初始 4 桶 */
    d->ht[0] = calloc(DICTHT_SIZE(2), sizeof(entry*));
    d->rehashidx = -1;
    return d;
}
static unsigned long hash(unsigned long k) {
    /* 简化哈希 (真实 SipHash) */
    unsigned long h = k * 2654435761UL;
    h ^= h >> 13; h *= 5; h ^= h >> 16;
    return h;
}

/* ---------- 渐进 rehash (对照 dict.c:312-414) ---------- */
static void rehash_bucket(dict *d, unsigned long idx) {
    entry *de = d->ht[0][idx], *next;
    while (de) {
        next = de->next;
        unsigned long h;
        if (d->size_exp[1] > d->size_exp[0])
            h = hash(de->key) & DICTHT_MASK(d->size_exp[1]);  /* 扩: 重算 */
        else
            h = idx & DICTHT_MASK(d->size_exp[1]);            /* 缩: 掩码复用! */
        de->next = d->ht[1][h];
        d->ht[1][h] = de;
        d->used[0]--; d->used[1]++;
        de = next;
    }
    d->ht[0][idx] = NULL;
}
static int check_completed(dict *d) {
    if (d->used[0] != 0) return 0;
    free(d->ht[0]);
    d->ht[0] = d->ht[1];
    d->used[0] = d->used[1];
    d->size_exp[0] = d->size_exp[1];
    d->ht[1] = NULL; d->used[1] = 0;
    d->rehashidx = -1;
    return 1;
}
static int dict_rehash(dict *d, int n) {
    if (!dictIsRehashing(d)) return 0;    /* 真实: dict.c:389 */
    int empty_visits = n * 10;
    while (n-- && d->used[0] != 0) {
        while (d->ht[0][d->rehashidx] == NULL) {
            d->rehashidx++;
            if (--empty_visits == 0) return 1;
        }
        rehash_bucket(d, d->rehashidx);
        d->rehashidx++;
    }
    return !check_completed(d);
}

/* ---------- 触发阈值 (对照 dict.c:1492-1550) ---------- */
static void expand_if_needed(dict *d) {
    if (dictIsRehashing(d)) return;
    if (d->used[0] >= DICTHT_SIZE(d->size_exp[0])) {      /* 1:1 */
        d->ht[1] = calloc(DICTHT_SIZE(d->size_exp[0]+1), sizeof(entry*));
        d->size_exp[1] = d->size_exp[0] + 1;
        d->rehashidx = 0;
    }
}
static void shrink_if_needed(dict *d) {
    if (dictIsRehashing(d) || DICTHT_SIZE(d->size_exp[0]) <= 4) return;
    if (d->used[0] * HASHTABLE_MIN_FILL <= DICTHT_SIZE(d->size_exp[0])) { /* <1:8 */
        int newexp = d->size_exp[0] - 1;
        d->ht[1] = calloc(DICTHT_SIZE(newexp), sizeof(entry*));
        d->size_exp[1] = newexp;
        d->rehashidx = 0;
    }
}

/* ---------- 查找联动 (对照 dict.c:736-770) ---------- */
static entry *dict_find(dict *d, unsigned long key) {
    unsigned long h = hash(key);
    unsigned long idx = h & DICTHT_MASK(d->size_exp[0]);
    if (dictIsRehashing(d)) {
        if (idx >= (unsigned long)d->rehashidx && d->ht[0][idx])
            rehash_bucket(d, idx);           /* 缓存友好: 先迁目标桶 */
        else
            dict_rehash(d, 1);               /* 游标步进 */
    }
    /* 双表查找 (真实 L758-770) */
    for (int table = 0; table <= 1; table++) {
        if (table == 0 && dictIsRehashing(d) && (long)idx < d->rehashidx)
            continue;                        /* 已迁区跳过 ht[0] */
        unsigned long i = h & DICTHT_MASK(d->size_exp[table]);
        entry *e = d->ht[table][i];
        while (e) { if (e->key == key) return e; e = e->next; }
        if (!dictIsRehashing(d)) break;
    }
    return NULL;
}
static int dict_add(dict *d, unsigned long k, unsigned long v) {
    expand_if_needed(d);
    if (dictIsRehashing(d)) dict_rehash(d, 1);
    if (dict_find(d, k)) return -1;
    /* rehash 中新增插新表 ht[1] (真实 dictAddRaw 语义), 否则污染迁移计数 */
    int t = dictIsRehashing(d) ? 1 : 0;
    unsigned long idx = hash(k) & DICTHT_MASK(d->size_exp[t]);
    entry *e = entry_new(k, v);
    e->next = d->ht[t][idx];
    d->ht[t][idx] = e;
    d->used[t]++;
    return 0;
}

/* ---------- dictScan 反向游标 (对照 dict.c:1369-1470) ---------- */
static unsigned long rev(unsigned long v) {
    unsigned long s = CHAR_BIT * sizeof(v);
    unsigned long mask = ~0UL;
    while ((s >>= 1) > 0) {
        mask ^= (mask << s);
        v = ((v >> s) & mask) | ((v << s) & ~mask);
    }
    return v;
}
static unsigned long scan_next(unsigned long v, unsigned long mask) {
    v |= ~mask;                                  /* 置位未掩码位 */
    v = rev(v);
    v++;
    v = rev(v);
    return v;
}
static unsigned long dict_scan(dict *d, unsigned long v, unsigned long *visited) {
    if (!dictIsRehashing(d)) {
        unsigned long m0 = DICTHT_MASK(d->size_exp[0]);
        entry *e = d->ht[0][v & m0];
        while (e) { visited[e->key]++; e = e->next; }
        v = scan_next(v, m0);
        return v;
    }
    int t0 = 0, t1 = 1;
    if (DICTHT_SIZE(d->size_exp[t0]) > DICTHT_SIZE(d->size_exp[t1])) { t0 = 1; t1 = 0; }
    unsigned long m0 = DICTHT_MASK(d->size_exp[t0]);
    unsigned long m1 = DICTHT_MASK(d->size_exp[t1]);
    entry *e = d->ht[t0][v & m0];
    while (e) { visited[e->key]++; e = e->next; }
    do {
        e = d->ht[t1][v & m1];                   /* 大表展开区 */
        while (e) { visited[e->key]++; e = e->next; }
        v |= ~m1;
        v = rev(v); v++; v = rev(v);
    } while (v & (m0 ^ m1));
    return v;
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

int main(void) {
    printf("[1] 渐进 rehash: 扩容后逐桶迁移 + 完成提升\n");
    dict *d = dict_new();
    for (unsigned long i = 0; i < 4; i++) dict_add(d, i, i);
    CHECK(d->rehashidx == -1, "4 键/4 桶: 1:1 未触发");
    dict_add(d, 4, 4);                           /* 5 键 → 1:1 溢出 → 触发扩 */
    CHECK(dictIsRehashing(d), "第 5 键触发扩容 (双表并存)");
    CHECK(d->size_exp[1] == 3, "新表 8 桶 (2 倍)");
    while (dict_rehash(d, 2));                   /* 迁完 */
    CHECK(!dictIsRehashing(d), "迁移完成: 新表提升, rehashidx=-1");
    CHECK(d->size_exp[0] == 3 && d->used[0] == 5, "完成: ht[0]=8 桶 5 键");

    printf("\n[2] 缩容掩码复用: 免重算哈希\n");
    dict *s = dict_new();
    for (unsigned long i = 0; i < 32; i++) dict_add(s, i, i);
    while (dict_rehash(s, 100));
    CHECK(s->size_exp[0] == 5, "32 键扩到 32 桶");
    for (unsigned long i = 0; i < 28; i++) {     /* 删到 4 键 → <1:8 */
        unsigned long idx = hash(i) & DICTHT_MASK(s->size_exp[0]);
        entry *prev = NULL, *e = s->ht[0][idx];
        while (e) { if (e->key == i) { if (prev) prev->next = e->next; else s->ht[0][idx] = e->next; free(e); s->used[0]--; break; } prev = e; e = e->next; }
    }
    shrink_if_needed(s);
    CHECK(dictIsRehashing(s), "<1:8 触发缩容");
    while (dict_rehash(s, 100));
    CHECK(s->size_exp[0] == 4, "缩到 16 桶");
    for (unsigned long i = 28; i < 32; i++)
        CHECK(dict_find(s, i) != NULL, "缩容后键仍可查 (掩码复用无损)");

    printf("\n[3] 查找联动: 目标桶先迁\n");
    dict *m = dict_new();
    for (unsigned long i = 0; i < 5; i++) dict_add(m, i, i);
    CHECK(dictIsRehashing(m), "rehash 进行中");
    entry *found = dict_find(m, 3);
    CHECK(found != NULL && found->val == 3, "rehash 中查找命中 (联动迁移后)");

    printf("\n[4] dictScan 反向游标: 扩表期间不丢不重\n");
    dict *sc = dict_new();
    for (unsigned long i = 0; i < 16; i++) dict_add(sc, i, i);
    while (dict_rehash(sc, 100));                /* 扩到 16 桶完成 */
    unsigned long visited[128] = {0}, cursor = 0, missing = 0;
    int rounds = 0;
    do {
        cursor = dict_scan(sc, cursor, visited);
        /* 每轮之间触发扩容+推进迁移 (真实: SCAN 间隔有操作+serverCron) */
        if (rounds == 1) { dict_add(sc, 100, 100), dict_add(sc, 101, 101), dict_add(sc, 102, 102); }
        dict_rehash(sc, 2);
        rounds++;
    } while (cursor != 0 && rounds < 100);
    printf("    rounds=%d (重复允许; 中途插入的键可漏 — SCAN 文档语义)\n", rounds);
    /* SCAN 保证: 迭代开始前已存在的键不丢 (重复允许);
     * 中途插入的键 (100/101/102, 游标 0 之后) 可能不被返回 — 合法语义 */
    for (unsigned long i = 0; i < 16; i++) if (visited[i] == 0) missing++;
    CHECK(missing == 0, "迭代开始时存在的 16 键全部至少访问一次 (SCAN 不丢保证)");
    printf("    中途插入键 visited: 100=%lu 101=%lu 102=%lu (允许缺失)\n",
           visited[100], visited[101], visited[102]);

    printf("\n结果: %s\n", fail ? "FAIL" : "ALL PASS");
    return fail ? 1 : 0;
}
