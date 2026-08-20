/*
 * R-33 zmalloc harness — 双路径记账 + OOM 分层 + usable 语义极简复现
 * 还原核心控制流 (不并发安全/不平台抽象/不统计面):
 *  1. PREFIX_SIZE 双路径 (usable_size vs 前缀存大小)
 *  2. used_memory 记账 (请求大小 vs 分配器实际大小)
 *  3. zmalloc (OOM 崩溃) vs ztrymalloc (OOM NULL) 错误分层
 *  4. usable 家族 + SDS 式免费膨胀 (桶分配模拟)
 * 编译: gcc -O0 -o harness zmalloc_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ---------- 模拟 jemalloc size classes: 2 的幂桶 ---------- */
static size_t bucket_of(size_t n) {
    size_t b = 8;                    /* 最小桶 8B */
    while (b < n) b <<= 1;
    return b;
}
/* HAVE_MALLOC_SIZE 路径: 分配器可报实际大小 */
static size_t mock_malloc_usable_size(void *p) {
    size_t *h = (size_t*)p;
    return *h;                       /* 桶大小存在头部 */
}
static void *mock_malloc(size_t n) {
    size_t bucket = bucket_of(n);
    size_t *p = malloc(bucket + sizeof(size_t));
    if (!p) return NULL;
    *p = bucket;                     /* 记录实际可用 */
    return (char*)p + sizeof(size_t);
}
static void mock_free(void *p) {
    if (!p) return;
    free((char*)p - sizeof(size_t));
}

/* ---------- 记账 ---------- */
static size_t used_memory = 0;       /* harness 单线程, 省略原子 */
#define MALLOC_MIN_SIZE(x) ((x) > 0 ? (x) : sizeof(long))

static void default_oom(size_t size) {
    fprintf(stderr, "OOM allocating %zu bytes -> PANIC\n", size);
    exit(1);
}
static void (*oom_handler)(size_t) = default_oom;

/* ---------- 双路径分配核心 (对照 zmalloc.c:93-111) ---------- */
#define HAVE_MALLOC_SIZE 1           /* 开关: 0 = 前缀路径 */
#if HAVE_MALLOC_SIZE
#define PREFIX_SIZE 0
#else
#define PREFIX_SIZE (sizeof(size_t))
#endif

static void *alloc_internal(size_t size, int use_try, size_t *usable_out) {
    if (size >= (size_t)-1 / 2) return NULL;
    size_t req = MALLOC_MIN_SIZE(size) + PREFIX_SIZE;
#if HAVE_MALLOC_SIZE
    void *ptr = mock_malloc(req);    /* 分配器返回桶大小 */
    if (!ptr) return NULL;
    size_t actual = mock_malloc_usable_size((char*)ptr - sizeof(size_t));
    used_memory += actual;           /* 记账 = 分配器实际大小 */
    if (usable_out) *usable_out = actual;
    if (!use_try) printf("  zmalloc(%zu): usable=%zu used=%zu\n", size, actual, used_memory);
    return ptr;
#else
    void *ptr = malloc(req);
    if (!ptr) return NULL;
    *((size_t*)ptr) = MALLOC_MIN_SIZE(size);
    used_memory += MALLOC_MIN_SIZE(size) + PREFIX_SIZE;  /* 记账 = 请求+前缀 */
    if (usable_out) *usable_out = MALLOC_MIN_SIZE(size);
    if (!use_try) printf("  zmalloc(%zu): prefix-used=%zu used=%zu\n", size, MALLOC_MIN_SIZE(size), used_memory);
    return (char*)ptr + PREFIX_SIZE;
#endif
}

/* OOM 分层: zmalloc 崩溃 vs ztrymalloc 降级 */
static void *zmalloc(size_t size) {
    void *p = alloc_internal(size, 0, NULL);
    if (!p) oom_handler(size);
    return p;
}
static void *ztrymalloc(size_t size) {
    return alloc_internal(size, 1, NULL);
}

/* free 对称记账 (对照 zmalloc.c:393-411) */
static void zfree_harness(void *ptr) {
    if (!ptr) return;
#if HAVE_MALLOC_SIZE
    used_memory -= mock_malloc_usable_size((char*)ptr - sizeof(size_t));
    mock_free(ptr);
#else
    size_t *realptr = (size_t*)((char*)ptr - PREFIX_SIZE);
    used_memory -= *realptr + PREFIX_SIZE;
    free(realptr);
#endif
}

/* ---------- SDS 式 usable 消费 (对照 sds.c:90-105) ---------- */
typedef struct { size_t len, alloc; char buf[]; } sds_simple;
static sds_simple *sds_new(const char *s) {
    size_t hdr = sizeof(sds_simple), initlen = strlen(s), usable = 0;
    void *p = alloc_internal(hdr + initlen + 1, 1, &usable);
    if (!p) return NULL;
    usable = usable - hdr - 1;                       /* 内容可用容量 */
    sds_simple *x = p;
    x->len = initlen;
    x->alloc = usable;                               /* alloc = 分配器实际 (免费膨胀) */
    memcpy(x->buf, s, initlen);
    x->buf[initlen] = '\0';
    return x;
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

int main(void) {
    printf("== PREFIX_SIZE = %d (HAVE_MALLOC_SIZE=%d)\n\n", PREFIX_SIZE, HAVE_MALLOC_SIZE);

    printf("[1] 记账: 初始 0\n");
    CHECK(used_memory == 0, "initial used_memory == 0");

    printf("\n[2] 请求 10B (桶=16): 记账按路径语义\n");
    size_t usable;
    void *p1 = alloc_internal(10, 0, &usable);
#if HAVE_MALLOC_SIZE
    CHECK(used_memory == 16, "10B request -> used_memory == 16 (bucket/usable)");
    CHECK(usable == 16, "usable == 16 (free expansion)");
#else
    CHECK(used_memory == 18, "10B request -> used_memory == 18 (request+prefix)");
    CHECK(usable == 10, "usable == 10 (no expansion, prefix path)");
#endif

    printf("\n[3] SDS 式免费膨胀: 请求 3B 内容, alloc 字段 = 桶-头部\n");
    sds_simple *x = sds_new("abc");
    CHECK(x->alloc >= 3, "sds alloc >= 3 (usable free expansion)");
    printf("    sds len=%zu alloc=%zu (请求 3, 实际可得 %zu)\n", x->len, x->alloc, x->alloc);

    printf("\n[4] OOM 分层: 模拟失败分配\n");
    /* 用一个超大尺寸触发 alloc_internal 的 SIZE_MAX/2 守卫 (不走真实 malloc) */
    void *big = ztrymalloc((size_t)-1 / 2);
    CHECK(big == NULL, "ztrymalloc(SIZE_MAX/2) returns NULL (try 降级)");

    printf("\n[5] used_memory 对称: free 后归零\n");
    zfree_harness(p1);
    zfree_harness(x);
    CHECK(used_memory == 0, "used_memory back to 0 after frees");
    return fail ? 1 : 0;
}
