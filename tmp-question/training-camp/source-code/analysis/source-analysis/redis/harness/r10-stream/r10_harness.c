/*
 * R-10 Stream+rax harness — rax 压缩节点 + stream ID 极简复现
 * 从 rax.c / t_stream.c 提取核心语义 (去掉 server 依赖), 实证:
 *  1. rax 插入: raxLowWalk 沿压缩/非压缩节点下行 + parentlink (rax.c:436-477)
 *  2. 压缩节点分裂: 插入 "ANNIBALE"→"ANNIENTARE" 式分裂 (ALGO 1)
 *  3. rax 查找: 精确匹配
 *  4. streamID: 比较/递增/递减 (含溢出回绕, t_stream.c:78-117)
 *  5. streamNextID: ms 前进用新 ms+seq0, 否则 last+1 (L119-129)
 *  6. 128bit BE 字典序键 (rax_key[2], streamAppendItem 语义)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness r10_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

/* ---- 极简 rax 节点 (压缩/非压缩) ---- */
typedef struct RNode {
    struct RNode **children;   /* 非压缩: 按字符索引 (256); 压缩: 1 个 */
    unsigned char *edge;
    size_t edgelen;
    int iscompr;
    int iskey;
    void *data;
} RNode;

typedef struct {
    RNode *head;
    size_t numele;
} RRax;

static RNode *rnode_new(int iscompr, size_t edgelen) {
    RNode *n = calloc(1, sizeof(*n));
    n->iscompr = iscompr;
    n->edgelen = edgelen;
    n->edge = malloc(edgelen ? edgelen : 1);
    n->children = calloc(iscompr ? 1 : 256, sizeof(RNode*));
    return n;
}

/* raxLowWalk 语义 (L436-477): 返回匹配前缀长度 + 停止节点 + parentlink */
typedef struct {
    RNode *node;       /* 停止节点 */
    RNode **plink;     /* 父中指向 node 的槽 (用于替换) */
    int splitpos;      /* 压缩节点内不匹配位置 */
    size_t matched;
} WalkResult;

static WalkResult rwalk(RRax *r, const unsigned char *s, size_t len) {
    RNode *h = r->head;
    RNode **parentlink = &r->head;
    size_t i = 0;
    int splitpos = 0;
    while (h && h->edgelen && i < len) {
        if (!h) break;
        if (h->iscompr) {
            size_t j;
            for (j = 0; j < h->edgelen && i < len; j++, i++) {
                if (h->edge[j] != s[i]) break;
            }
            if (j != h->edgelen) { splitpos = (int)j; break; }
            parentlink = &h->children[0];
            h = h->children[0];
        } else {
            /* 非压缩: 边字符按位置匹配, children[j] 对应 edge[j] */
            size_t j;
            for (j = 0; j < h->edgelen; j++)
                if (h->edge[j] == s[i]) break;
            if (j == h->edgelen) break;
            if (!h->children[j]) break;
            parentlink = &h->children[j];
            i++;
            h = h->children[j];
            splitpos = 0;
        }
    }
    WalkResult wr = { h, parentlink, splitpos, i };
    return wr;
}

static void *rfind(RRax *r, const unsigned char *s, size_t len) {
    WalkResult wr = rwalk(r, s, len);
    /* raxFind 语义 (L895-904): 完全匹配 (含压缩节点 splitpos==0) 且 iskey */
    if (wr.matched != len) return NULL;
    if (wr.node->iscompr && wr.splitpos != 0) return NULL;
    if (!wr.node->iskey) return NULL;
    return wr.node->data;
}

/* ALGO 1 (L596-780): 压缩节点中间不匹配 (i < len, splitpos = j) */
/* 返回替换后的父节点 (已挂入树) */
static void algo1_split(RNode *h, RNode **plink, int j,
                        const unsigned char *s, size_t i, size_t len, void *data,
                        RRax *r) {
    RNode *old = h; /* 原节点 (3a/3b 后被替换) */
    size_t size = h->edgelen;
    RNode *next = h->children[0]; /* 原子子节点 */
    RNode *postfix;
    size_t postfixlen = size - j - 1;
    if (postfixlen) {
        postfix = rnode_new(1, postfixlen);
        memcpy(postfix->edge, h->edge + j + 1, postfixlen);
        postfix->children[0] = next; /* 原原子子链 */
    } else {
        postfix = next; /* 4b: 直接使用原子子 */
    }

    if (j == 0) {
        /* 3a: splitnode 替换原节点, iskey 继承 (L709-719) */
        RNode *splitnode = rnode_new(0, 1);
        splitnode->edge[0] = h->edge[0];
        if (h->iskey) { splitnode->iskey = 1; splitnode->data = h->data; }
        splitnode->children[0] = postfix;
        *plink = splitnode;
        h = splitnode;
    } else {
        /* 3b: trimmed [0..j) + splitnode (L719-740) */
        RNode *trimmed = rnode_new(1, j);
        memcpy(trimmed->edge, h->edge, j);
        trimmed->iskey = h->iskey;
        trimmed->data = h->data;
        RNode *splitnode = rnode_new(0, 1);
        splitnode->edge[0] = h->edge[j];
        splitnode->children[0] = postfix;
        trimmed->children[0] = splitnode;
        *plink = trimmed;
        h = splitnode;
    }

    /* 5/6: 继续插入 (新键余下走单字符/压缩) */
    /* 剩余字符 = s[i+1..len), 挂到 h 的 children[1] */
    size_t rest = len - i - 1;
    RNode *newtail;
    if (rest == 0) {
        newtail = rnode_new(0, 0);
        newtail->iskey = 1;
        newtail->data = data;
    } else {
        newtail = rnode_new(1, rest);
        memcpy(newtail->edge, s + i + 1, rest);
        newtail->children[0] = rnode_new(0, 0);
        newtail->children[0]->iskey = 1;
        newtail->children[0]->data = data;
    }
    h->edge = realloc(h->edge, 2);
    h->edge[1] = s[i];
    h->edgelen = 2;
    h->children[1] = newtail;

    /* 原节点已被 splitnode/trimmed 替换, 释放 (真实 rax rax_free(h)) */
    free(old->children);
    free(old->edge);
    free(old);
    (void)r;
}

/* ALGO 2 (L759-806): 压缩节点全匹配到键尾 (i == len, j = splitpos > 0)
 * trimmed = [0..j) iskey 继承; postfix = [j..size) iskey=新键数据; postfix->child = 原子子 */
static void algo2_split(RNode *h, RNode **plink, int j, void *data) {
    size_t size = h->edgelen;
    size_t postfixlen = size - j;
    RNode *next = h->children[0];

    RNode *postfix = rnode_new(1, postfixlen);
    memcpy(postfix->edge, h->edge + j, postfixlen);
    postfix->iskey = 1;
    postfix->data = data;
    postfix->children[0] = next;

    RNode *trimmed = rnode_new(1, j);
    memcpy(trimmed->edge, h->edge, j);
    trimmed->iskey = h->iskey; /* 原键转移 */
    trimmed->data = h->data;
    trimmed->children[0] = postfix;

    *plink = trimmed;
    free(h->children);
    free(h->edge);
    free(h);
}

static int rinsert(RRax *r, const unsigned char *s, size_t len, void *data) {
    WalkResult wr = rwalk(r, s, len);
    RNode *h = wr.node;
    RNode **plink = wr.plink;
    size_t i = wr.matched;

    /* 1. 键尾到达且不在压缩节点中间 → 设为键 (L502-527) */
    if (i == len && (!h->iscompr || wr.splitpos == 0)) {
        if (h->iskey) return 0;
        h->iskey = 1;
        h->data = data;
        r->numele++;
        return 1;
    }

    /* 2. 压缩节点处理: ALGO 1 (i<len 中间/开头不匹配) / ALGO 2 (i==len 前缀键) */
    if (h->iscompr && i < len) {
        /* ALGO 1 (L596-780): j = splitpos (0=开头 3a, >0 中间 3b) */
        algo1_split(h, plink, wr.splitpos, s, i, len, data, r);
        r->numele++;
        return 1;
    }
    if (h->iscompr && i == len && wr.splitpos != 0) {
        /* ALGO 2 (L759-806): 前缀键插入 */
        algo2_split(h, plink, wr.splitpos, data);
        r->numele++;
        return 1;
    }

    /* 3. 剩余字符插入循环 (L812-846) */
    while (i < len) {
        RNode *child;
        if (h->edgelen == 0 && len - i > 1) {
            /* 压缩剩余字符 (L819-830: raxCompressNode — realloc 复用原节点并保留 iskey,
             * L384-394; harness 用新节点替代 → 释放原空节点) */
            RNode *compr = rnode_new(1, len - i);
            memcpy(compr->edge, s + i, len - i);
            if (h->iskey) { compr->iskey = 1; compr->data = h->data; } /* 键保留 */
            child = rnode_new(0, 0); /* 空叶子 */
            compr->children[0] = child;
            *plink = compr;
            plink = &compr->children[0];
            free(h->edge); free(h->children); free(h); /* 释放被替换的空节点 */
            h = child;
            i = len;
        } else {
            /* 单字符 raxAddChild (L832-841): 边字符追加, children[old] 位置 */
            size_t old = h->edgelen;
            h->edge = realloc(h->edge, old + 1);
            h->edge[old] = s[i];
            h->edgelen = old + 1;
            child = rnode_new(0, 0);
            h->children[old] = child;
            plink = &h->children[old];
            h = child;
            i++;
        }
    }
    /* 数据设在最终叶子 (L844-846: raxReallocForData + raxSetData) */
    h->iskey = 1;
    h->data = data;
    r->numele++;
    return 1;
}


static void rfree_tree(RNode *n) {
    if (!n) return;
    for (size_t i = 0; i < (n->iscompr ? 1 : 256); i++)
        rfree_tree(n->children[i]);
    free(n->children);
    free(n->edge);
    free(n);
}

/* ---- stream ID 语义 (t_stream.c:78-129) ---- */
typedef struct { uint64_t ms, seq; } SID;

static int sid_cmp(SID *a, SID *b) {
    if (a->ms != b->ms) return a->ms < b->ms ? -1 : 1;
    if (a->seq != b->seq) return a->seq < b->seq ? -1 : 1;
    return 0;
}

static int sid_incr(SID *id) {
    int ret = 0;
    if (id->seq == UINT64_MAX) {
        if (id->ms == UINT64_MAX) { id->ms = id->seq = 0; ret = -1; }
        else { id->ms++; id->seq = 0; }
    } else { id->seq++; }
    return ret;
}

static int sid_decr(SID *id) {
    int ret = 0;
    if (id->seq == 0) {
        if (id->ms == 0) { id->ms = id->seq = UINT64_MAX; ret = -1; }
        else { id->ms--; id->seq = UINT64_MAX; }
    } else { id->seq--; }
    return ret;
}

/* streamNextID 语义 (L119-129) */
static void sid_next(SID *last, uint64_t now_ms, SID *out) {
    if (now_ms > last->ms) { out->ms = now_ms; out->seq = 0; }
    else { *out = *last; sid_incr(out); }
}

/* 128bit BE 键 (streamAppendItem 语义) */
static void sid_to_raxkey(SID *id, uint64_t key[2]) {
    key[0] = __builtin_bswap64(id->ms);
    key[1] = __builtin_bswap64(id->seq);
}

/* ---- 测试骨架 ---- */
static int tests = 0, failures = 0;
#define CHECK(cond) do { tests++; if (!(cond)) { failures++; \
    printf("FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } } while (0)

static void test_rax_basic(void) {
    RRax r = { .head = rnode_new(0, 0), .numele = 0 }; /* 根 = 非压缩空节点 */
    int v1 = 1, v2 = 2, v3 = 3;

    CHECK(rinsert(&r, (unsigned char*)"foo", 3, &v1) == 1);
    CHECK(rfind(&r, (unsigned char*)"foo", 3) == &v1);
    CHECK(rfind(&r, (unsigned char*)"fo", 2) == NULL);
    CHECK(rfind(&r, (unsigned char*)"fooo", 4) == NULL);
    CHECK(rinsert(&r, (unsigned char*)"foo", 3, &v2) == 0); /* 重复 */

    /* "foobar": 根边 'f' + 压缩 "oo" → 分裂 "oo"+"bar" */
    CHECK(rinsert(&r, (unsigned char*)"foobar", 6, &v2) == 1);
    CHECK(rfind(&r, (unsigned char*)"foo", 3) == &v1);
    CHECK(rfind(&r, (unsigned char*)"foobar", 6) == &v2);

    /* "foozap": 非压缩节点 'b'/'z' 两分 */
    CHECK(rinsert(&r, (unsigned char*)"foozap", 6, &v3) == 1);
    CHECK(rfind(&r, (unsigned char*)"foozap", 6) == &v3);
    CHECK(rfind(&r, (unsigned char*)"foobar", 6) == &v2);

    rfree_tree(r.head);
}

static void test_rax_compression_split(void) {
    RRax r = { .head = rnode_new(0, 0), .numele = 0 };
    int v1 = 1, v2 = 2;

    /* 压缩 "ANNIBALE" (根边 'A' + 压缩 "NNIBALE") */
    CHECK(rinsert(&r, (unsigned char*)"ANNIBALE", 8, &v1) == 1);
    /* "ANNIENTARE": 在 "NNIBALE" 的 splitpos=3 ('B' vs 'E') 分裂 */
    CHECK(rinsert(&r, (unsigned char*)"ANNIENTARE", 10, &v2) == 1);
    CHECK(rfind(&r, (unsigned char*)"ANNIBALE", 8) == &v1);
    CHECK(rfind(&r, (unsigned char*)"ANNIENTARE", 10) == &v2);
    /* 前缀仍共享: "ANNI" 前缀匹配两者 */
    CHECK(rfind(&r, (unsigned char*)"ANNI", 4) == NULL); /* 中间节点非键 */

    rfree_tree(r.head);
}

static void test_stream_id(void) {
    SID a = {1, 2}, b = {1, 3}, c = {2, 0}, d = {1, 2};
    CHECK(sid_cmp(&a, &b) < 0);
    CHECK(sid_cmp(&b, &a) > 0);
    CHECK(sid_cmp(&a, &c) < 0);
    CHECK(sid_cmp(&a, &d) == 0);

    SID x = {1, UINT64_MAX};
    CHECK(sid_incr(&x) == 0);
    CHECK(x.ms == 2 && x.seq == 0);
    SID y = {UINT64_MAX, UINT64_MAX};
    CHECK(sid_incr(&y) == -1);
    CHECK(y.ms == 0 && y.seq == 0);

    SID z = {0, 0};
    CHECK(sid_decr(&z) == -1);
    CHECK(z.ms == UINT64_MAX && z.seq == UINT64_MAX);

    SID last = {1000, 5}, out;
    sid_next(&last, 2000, &out);
    CHECK(out.ms == 2000 && out.seq == 0);
    sid_next(&last, 1000, &out);
    CHECK(out.ms == 1000 && out.seq == 6);
    sid_next(&last, 999, &out); /* 时钟回退 */
    CHECK(out.ms == 1000 && out.seq == 6);
}

static void test_raxkey_be(void) {
    SID id1 = {1, 100}, id2 = {1, 101}, id3 = {2, 0};
    uint64_t k1[2], k2[2], k3[2];
    sid_to_raxkey(&id1, k1);
    sid_to_raxkey(&id2, k2);
    sid_to_raxkey(&id3, k3);
    CHECK(memcmp(k1, k2, 16) < 0);
    CHECK(memcmp(k2, k3, 16) < 0);
    CHECK(k1[0] < k3[0]);
}

int main(void) {
    printf("R-10 stream+rax harness (gcc+ASan)\n");

    test_rax_basic();
    test_rax_compression_split();
    test_stream_id();
    test_raxkey_be();

    printf("tests: %d, failures: %d\n", tests, failures);
    return failures ? 1 : 0;
}
