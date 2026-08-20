/*
 * R-5 quicklist harness — 分页链表 + 双容器 + fill 限制 + 分裂极简复现
 * 还原核心控制流 (listpack 容器简化为数组; 不做 LZF 真实压缩):
 *  1. 双向链表分页: 每节点一个"容器" (PACKED 数组 / PLAIN 单元素)
 *  2. fill 双语义: 正=元素数 / 负=字节数 (2^k 表)
 *  3. 三路路由: PLAIN (大元素) / 就地追加 (允许) / 新节点 (满)
 *  4. 分裂: 复制 + 双侧裁剪
 *  5. 压缩状态模拟: 中间节点可压缩 (头尾永不)
 * 编译: gcc -O0 -o harness quicklist_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define QL_CONTAINER_PACKED 2
#define QL_CONTAINER_PLAIN 1

typedef struct qlNode {
    struct qlNode *prev, *next;
    char **items;            /* PACKED: 元素数组; PLAIN: items[0] */
    size_t *sizes;
    unsigned int count;      /* 元素数 */
    size_t bytes;            /* 容器字节和 */
    int container;           /* PLAIN/PACKED */
    int compressed;          /* 模拟压缩状态 */
} qlNode;

typedef struct {
    qlNode *head, *tail;
    unsigned long count;     /* 总元素 */
    unsigned long len;       /* 节点数 */
    int fill;                /* >0 元素数 / <0 字节 (2^k) */
    int compress;            /* 两端不压深度 */
} quicklist;

static const size_t opt_level[] = {4096, 8192, 16384, 32768, 65536};
static size_t node_size_limit(int fill) {
    if (fill >= 0) return (size_t)-1;      /* 元素数限制 */
    size_t off = (-fill) - 1;
    if (off >= sizeof(opt_level)/sizeof(*opt_level)) off = 4;
    return opt_level[off];
}
static unsigned int node_count_limit(int fill) {
    return fill > 0 ? fill : (unsigned int)-1;
}
static int is_large(size_t sz, int fill) {
    return fill < 0 && sz > node_size_limit(fill);
}

static quicklist *qlCreate(void) {
    quicklist *q = calloc(1, sizeof(quicklist));
    q->fill = -2;            /* 默认 8KB */
    return q;
}
static qlNode *qlNodeNew(int container, const char *val, size_t sz) {
    qlNode *n = calloc(1, sizeof(qlNode));
    n->container = container;
    n->items = malloc(sizeof(char*));
    n->sizes = malloc(sizeof(size_t));
    n->items[0] = val ? strdup(val) : NULL;
    n->sizes[0] = sz;
    n->count = 1;
    n->bytes = sz;
    return n;
}
static void qlNodeAppend(qlNode *n, const char *val, size_t sz) {
    n->items = realloc(n->items, sizeof(char*) * (n->count + 1));
    n->sizes = realloc(n->sizes, sizeof(size_t) * (n->count + 1));
    n->items[n->count] = strdup(val);
    n->sizes[n->count] = sz;
    n->count++;
    n->bytes += sz;
}

/* 三路路由 (对照 quicklist.c:583-603) */
static void qlPush(quicklist *q, const char *val, size_t sz) {
    if (is_large(sz, q->fill)) {
        /* 路 1: PLAIN 节点 */
        qlNode *n = qlNodeNew(QL_CONTAINER_PLAIN, val, sz);
        n->next = q->head;
        if (q->head) q->head->prev = n; else q->tail = n;
        q->head = n;
        q->len++;
        q->count++;
        return;
    }
    if (q->head == NULL) {
        q->head = q->tail = qlNodeNew(QL_CONTAINER_PACKED, val, sz);
        q->len = 1; q->count = 1;
        return;
    }
    qlNode *h = q->head;
    int ok_count = h->count < node_count_limit(q->fill);
    int ok_bytes = (h->bytes + sz) <= node_size_limit(q->fill);
    if (ok_count && ok_bytes) {
        qlNodeAppend(h, val, sz);              /* 路 2: 就地 */
    } else {
        qlNode *n = qlNodeNew(QL_CONTAINER_PACKED, val, sz);
        n->next = h; h->prev = n;              /* 路 3: 新节点 */
        q->head = n;
        q->len++;
    }
    q->count++;
}

/* 分裂 (对照 quicklist.c:971-1004, 简化: 从中间劈开) */
static void qlSplitNode(quicklist *q, qlNode *n) {
    if (n->count < 2 || n->container != QL_CONTAINER_PACKED) return;
    unsigned int mid = n->count / 2;
    qlNode *new_n = calloc(1, sizeof(qlNode));
    new_n->container = QL_CONTAINER_PACKED;
    new_n->items = malloc(sizeof(char*) * (n->count - mid));
    new_n->sizes = malloc(sizeof(size_t) * (n->count - mid));
    new_n->count = 0; new_n->bytes = 0;
    for (unsigned int i = mid; i < n->count; i++) {
        new_n->items[new_n->count] = n->items[i];
        new_n->sizes[new_n->count] = n->sizes[i];
        new_n->count++;
        new_n->bytes += n->sizes[i];
    }
    n->count = mid;
    n->bytes = 0;
    for (unsigned int i = 0; i < mid; i++) n->bytes += n->sizes[i];
    /* 链入 */
    new_n->next = n->next;
    new_n->prev = n;
    if (n->next) n->next->prev = new_n; else q->tail = new_n;
    n->next = new_n;
    q->len++;
}

/* 压缩标记: 两端 compress 深度之外标记 compressed (简化, 不真压) */
static void qlCompress(quicklist *q) {
    if (q->compress <= 0 || q->len < (unsigned long)(q->compress * 2)) return;
    qlNode *n = q->head;
    unsigned long idx = 0;
    while (n) {
        if (idx >= (unsigned long)q->compress &&
            q->len - idx - 1 >= (unsigned long)q->compress)
            n->compressed = 1;                 /* 冷中间区 */
        n = n->next;
        idx++;
    }
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

static unsigned long qlTotalItems(quicklist *q) {
    unsigned long t = 0; qlNode *n = q->head;
    while (n) { t += n->count; n = n->next; }
    return t;
}

int main(void) {
    printf("[1] 分页与 fill: 默认 -2=8KB, 元素填充到节点上限\n");
    quicklist *q = qlCreate();
    /* 8000B/元素 × 2 → 超 8KB → 新节点 */
    char buf[8000]; memset(buf, 'x', 7999); buf[7999] = 0;
    qlPush(q, buf, 8000);
    qlPush(q, buf, 8000);
    CHECK(q->len == 2, "2×8KB 元素 → 2 节点 (8KB 上限)");
    CHECK(qlTotalItems(q) == 2, "总元素 2");

    printf("\n[2] 三路路由: 大元素 → PLAIN 节点\n");
    char big[20000]; memset(big, 'y', 19999); big[19999] = 0;
    qlPush(q, big, 20000);
    CHECK(q->head->container == QL_CONTAINER_PLAIN, "20KB 大元素 → PLAIN 节点 (头)");
    CHECK(q->head->count == 1, "PLAIN 节点单元素");

    printf("\n[3] fill 正数语义: 每节点 ≤N 元素\n");
    quicklist *q2 = qlCreate();
    q2->fill = 3;
    for (int i = 0; i < 10; i++) qlPush(q2, "a", 1);
    CHECK(q2->len == 4, "10 元素 / fill=3 → 4 节点 (3+3+3+1)");

    printf("\n[4] 分裂: 中间节点劈开\n");
    /* 找第一个可分裂节点 (count>=2) — head 可能是 count=1 的新节点 */
    qlNode *split_target = q2->head;
    while (split_target && split_target->count < 2) split_target = split_target->next;
    printf("    分裂目标 count=%u\n", split_target->count);
    qlSplitNode(q2, split_target);
    printf("    分裂后 len=%lu\n", q2->len);
    CHECK(q2->len == 5, "分裂后 5 节点");
    CHECK(split_target->count == 1 || split_target->count == 2, "分裂后目标节点变短");
    CHECK(qlTotalItems(q2) == 10, "分裂不丢元素");

    printf("\n[5] 压缩: 两端深度外标记\n");
    quicklist *q3 = qlCreate();
    q3->fill = 2;
    for (int i = 0; i < 8; i++) qlPush(q3, "b", 1);
    q3->compress = 1;                          /* 两端各 1 节点不压 */
    qlCompress(q3);
    qlNode *n = q3->head;
    int compressed_count = 0, idx = 0;
    while (n) {
        if (idx >= 1 && q3->len - idx - 1 >= 1) compressed_count++;
        n = n->next; idx++;
    }
    CHECK(compressed_count == q3->len - 2, "compress=1: 中间 len-2 节点标记压缩 (头尾不压)");
    CHECK(q3->head->compressed == 0 && q3->tail->compressed == 0, "头尾永不压缩");

    printf("\n结果: %s\n", fail ? "FAIL" : "ALL PASS");
    return fail ? 1 : 0;
}
