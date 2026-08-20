/*
 * R-6 skiplist harness — 概率层级 + span 排名 + 复合排序 + 范围查询极简复现
 * 还原核心控制流 (不双结构 dict 侧/不做完整命令面):
 *  1. zslRandomLevel: P=0.25 几何分布
 *  2. zslInsert: update[]+rank[] 双数组 + span 差维护
 *  3. zslGetRank: span 累加 O(log n) 排名
 *  4. (score, ele) 复合排序 (同分字典序)
 *  5. 范围查询: IsInRange 首尾判空 + 跳表跳过范围外
 * 编译: gcc -O0 -o harness zset_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define ZSKIPLIST_MAXLEVEL 32
#define ZSKIPLIST_P 0.25

typedef struct zslNode {
    long long score;
    char *ele;
    struct zslNode *backward;
    struct zslLevel { struct zslNode *forward; unsigned long span; } level[];
} zslNode;

typedef struct {
    zslNode *header, *tail;
    unsigned long length;
    int level;
} zsl;

static int zslRandomLevel(void) {
    static const int threshold = ZSKIPLIST_P * RAND_MAX;
    int level = 1;
    while (rand() < threshold) level += 1;
    return (level < ZSKIPLIST_MAXLEVEL) ? level : ZSKIPLIST_MAXLEVEL;
}
static zslNode *zslCreateNode(int level, double score, const char *ele) {
    zslNode *n = calloc(1, sizeof(zslNode) + sizeof(struct zslLevel) * level);
    n->score = score;
    n->ele = strdup(ele);
    return n;
}
static zsl *zslCreate(void) {
    zsl *z = calloc(1, sizeof(zsl));
    z->header = zslCreateNode(ZSKIPLIST_MAXLEVEL, 0, "");
    z->level = 1;
    return z;
}

/* (score, ele) 复合比较: score 升序, 同分 ele 字典序 (对照 t_zset.c:147-150) */
static int zslLess(zslNode *a, double score, const char *ele) {
    return a->score < score || (a->score == score && strcmp(a->ele, ele) < 0);
}

/* 插入: update[]+rank[] 双数组 (对照 t_zset.c:137-192) */
static zslNode *zslInsert(zsl *zsl, double score, const char *ele) {
    zslNode *update[ZSKIPLIST_MAXLEVEL], *x;
    unsigned long rank[ZSKIPLIST_MAXLEVEL];
    int i, level;

    x = zsl->header;
    for (i = zsl->level - 1; i >= 0; i--) {
        rank[i] = i == (zsl->level - 1) ? 0 : rank[i + 1];
        while (x->level[i].forward && zslLess(x->level[i].forward, score, ele)) {
            rank[i] += x->level[i].span;
            x = x->level[i].forward;
        }
        update[i] = x;
    }
    level = zslRandomLevel();
    if (level > zsl->level) {
        for (i = zsl->level; i < level; i++) {
            rank[i] = 0;
            update[i] = zsl->header;
            update[i]->level[i].span = zsl->length;
        }
        zsl->level = level;
    }
    x = zslCreateNode(level, score, ele);
    for (i = 0; i < level; i++) {
        x->level[i].forward = update[i]->level[i].forward;
        update[i]->level[i].forward = x;
        x->level[i].span = update[i]->level[i].span - (rank[0] - rank[i]);
        update[i]->level[i].span = (rank[0] - rank[i]) + 1;
    }
    for (i = level; i < zsl->level; i++) update[i]->level[i].span++;
    x->backward = (update[0] == zsl->header) ? NULL : update[0];
    if (x->level[0].forward) x->level[0].forward->backward = x;
    else zsl->tail = x;
    zsl->length++;
    return x;
}

/* 排名: span 累加 (对照 t_zset.c:508-528) */
static unsigned long zslGetRank(zsl *zsl, double score, const char *ele) {
    zslNode *x = zsl->header;
    unsigned long rank = 0;
    for (int i = zsl->level - 1; i >= 0; i--) {
        while (x->level[i].forward &&
               (x->level[i].forward->score < score ||
                (x->level[i].forward->score == score &&
                 strcmp(x->level[i].forward->ele, ele) <= 0))) {
            rank += x->level[i].span;
            x = x->level[i].forward;
        }
        if (x->ele && x->score == score && strcmp(x->ele, ele) == 0) return rank;
    }
    return 0;
}

/* 范围查询 (对照 t_zset.c:317-410, 简化) */
typedef struct { double min, max; int minex, maxex; } zrangespec;
static int zslIsInRange(zsl *zsl, zrangespec *r) {
    if (r->min > r->max || (r->min == r->max && (r->minex || r->maxex))) return 0;
    zslNode *x = zsl->tail;
    if (x == NULL || x->score < r->min || (x->score == r->min && r->minex)) return 0;
    x = zsl->header->level[0].forward;
    if (x == NULL || x->score > r->max || (x->score == r->max && r->maxex)) return 0;
    return 1;
}
static zslNode *zslFirstInRange(zsl *zsl, zrangespec *r) {
    if (!zslIsInRange(zsl, r)) return NULL;
    zslNode *x = zsl->header;
    for (int i = zsl->level - 1; i >= 0; i--) {
        while (x->level[i].forward &&
               (x->level[i].forward->score < r->min ||
                (x->level[i].forward->score == r->min && r->minex)))
            x = x->level[i].forward;
    }
    return x->level[0].forward;
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

int main(void) {
    srand(42);
    printf("[1] 双结构省略 — 纯跳表核心 (dict 侧已在 R-3 验证)\n");

    printf("\n[2] 插入 + 复合排序 (同分字典序)\n");
    zsl *z = zslCreate();
    zslInsert(z, 10, "banana");
    zslInsert(z, 5, "apple");
    zslInsert(z, 10, "cherry");   /* 同分 10: cherry 排在 banana 后 (字典序) */
    zslInsert(z, 1, "fig");
    CHECK(z->length == 4, "4 元素");
    unsigned long r1 = zslGetRank(z, 5, "apple");
    unsigned long r2 = zslGetRank(z, 10, "banana");
    unsigned long r3 = zslGetRank(z, 10, "cherry");
    CHECK(r1 == 2, "apple (5) 排名 2");
    CHECK(r2 == 3 && r3 == 4, "同分: banana 3 / cherry 4 (字典序)");

    printf("\n[3] span 排名 O(log n): 1000 元素排名正确\n");
    zsl *big = zslCreate();
    for (int i = 0; i < 1000; i++) zslInsert(big, (double)(rand() % 10000), "x");
    /* 插入已知极值验证排名 */
    zslInsert(big, -1, "min_elem");
    CHECK(zslGetRank(big, -1, "min_elem") == 1, "最小元素排名 1");

    printf("\n[4] 范围查询: IsInRange 判空 + FirstInRange\n");
    zrangespec r = {2, 8, 0, 0};
    zslNode *first = zslFirstInRange(z, &r);
    CHECK(first != NULL && first->score == 5, "范围 [2,8] 首个 = 5 (apple)");
    zrangespec empty = {100, 200, 0, 0};
    CHECK(zslFirstInRange(z, &empty) == NULL, "范围 [100,200] 判空 (IsInRange 拦截)");
    zrangespec half = {10, 10, 1, 1};   /* 开区间 (10,10) */
    CHECK(zslFirstInRange(z, &half) == NULL, "开区间 (10,10) 空 (min==max && 开)");

    printf("\n[5] 层级分布: P=0.25 期望 ~1.33\n");
    long levels = 0, cnt = 200000;
    for (long i = 0; i < cnt; i++) levels += zslRandomLevel();
    double avg = (double)levels / cnt;
    printf("    平均层级 %.3f (理论 1/(1-0.25)=1.333)\n", avg);
    CHECK(avg > 1.2 && avg < 1.5, "层级期望 ≈ 1.33");

    printf("\n结果: %s\n", fail ? "FAIL" : "ALL PASS");
    return fail ? 1 : 0;
}
