/*
 * R-9 复制 harness — backlog 共享缓冲 + PSYNC 握手状态机极简复现
 * 从 replication.c 提取核心语义 (去掉 server 依赖), 实证:
 *  1. repl_buffer_blocks 块链: 尾部追加/新块分配 (replication.c:315-413)
 *  2. refcount 引用计数: backlog + 每从库各持一引用 (L374-397)
 *  3. incrementalTrimReplicationBacklog: refcount==1 才裁/至少留 1 块/每次限块数 (L242-295)
 *  4. backlog offset 语义: 首字节 = master_repl_offset - histlen + 1 (L292-294)
 *  5. PSYNC 主库裁决: replid 双 ID + offset 范围 [offset, offset+histlen] (L718-767)
 *  6. 从库握手状态机: CONNECTING→PING→AUTH→PORT→IP→CAPA→PSYNC (syncWithMaster L2608+)
 *  7. slaveTryPartialResynchronization: cached_master 才有部分机会, 否则 "?" + "-1" (L2451-2458)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness r9_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

/* ---- replBufBlock / replBacklog 极简复现 ---- */
#define PROTO_REPLY_CHUNK_BYTES 16384

typedef struct ReplBufBlock {
    struct ReplBufBlock *next;
    char *buf;
    size_t size, used;
    int refcount;
    long long repl_offset;
} ReplBufBlock;

typedef struct {
    ReplBufBlock *head, *tail;   /* 块链 */
    int nblocks;
    long long histlen;           /* 有效数据总长 */
    long long master_repl_offset;/* 全局复制偏移 */
    long long backlog_size;      /* 配置上限 */
} ReplBuf;

static void rb_init(ReplBuf *rb, long long backlog_size) {
    memset(rb, 0, sizeof(*rb));
    rb->backlog_size = backlog_size;
    rb->master_repl_offset = 1000; /* 假设起始偏移 */
}

/* feedReplicationBuffer 语义 (L315-413): 尾部追加, 满则新块, 更新引用 */
static void rb_feed(ReplBuf *rb, const char *s, size_t len) {
    int had_head = (rb->head != NULL);
    while (len > 0) {
        ReplBufBlock *tail = rb->tail;
        if (tail && tail->size > tail->used) {
            size_t avail = tail->size - tail->used;
            size_t copy = (avail >= len) ? len : avail;
            memcpy(tail->buf + tail->used, s, copy);
            tail->used += copy;
            s += copy;
            len -= copy;
            rb->master_repl_offset += copy;
            rb->histlen += copy;
        }
        if (len) {
            size_t size = (len > PROTO_REPLY_CHUNK_BYTES) ? len : PROTO_REPLY_CHUNK_BYTES;
            ReplBufBlock *nb = malloc(sizeof(*nb));
            nb->buf = malloc(size);
            size_t copy = (size >= len) ? len : size;
            memcpy(nb->buf, s, copy);
            nb->size = size;
            nb->used = copy;
            nb->refcount = 0;
            nb->repl_offset = rb->master_repl_offset + 1;
            nb->next = NULL;
            if (rb->tail) rb->tail->next = nb;
            else rb->head = nb;
            rb->tail = nb;
            rb->nblocks++;
            s += copy;
            len -= copy;
            rb->master_repl_offset += copy;
            rb->histlen += copy;
        }
    }
    /* backlog 引用首块 (源码 L393-397: backlog 首次引用 refcount++) */
    if (!had_head && rb->head) rb->head->refcount++;
}

/* backlog 引用首块 (feedReplicationBuffer L393-397 语义) */
static void rb_backlog_ref(ReplBuf *rb) {
    if (rb->head) rb->head->refcount++;
}

/* 从库引用 (L382-387 语义) */
static void rb_slave_ref(ReplBuf *rb) {
    if (rb->head) rb->head->refcount++;
}

/* incrementalTrimReplicationBacklog 语义 (L242-295): refcount==1 才裁, 至少留 1 块,
 * 裁剪后新 head 引用转移 (L274-278) */
static int rb_trim(ReplBuf *rb, size_t max_blocks) {
    size_t trimmed = 0;
    while (rb->histlen > rb->backlog_size && trimmed < max_blocks) {
        if (rb->nblocks <= 1) break;
        ReplBufBlock *first = rb->head;
        if (first->refcount != 1) break;  /* 有从库引用则不裁 */
        /* 裁剪后仍超限才裁 (避免抖动) */
        if (rb->histlen - (long long)first->used <= rb->backlog_size) break;
        rb->head = first->next;
        if (!rb->head) rb->tail = NULL;
        rb->histlen -= first->used;
        rb->nblocks--;
        /* 新 head 引用转移 (源码 L274-278: backlog 引用转移到新首块) */
        rb->head->refcount++;
        free(first->buf);
        free(first);
        trimmed++;
    }
    return (int)trimmed;
}

/* backlog 首字节 offset (L292-294) */
static long long rb_start_offset(ReplBuf *rb) {
    return rb->master_repl_offset - rb->histlen + 1;
}

/* masterTryPartialResynchronization 范围校验 (L756-758):
 * 拒绝 psync_offset < offset || psync_offset > offset+histlen
 * offset+histlen = master_repl_offset+1 (半开区间 [offset, offset+histlen])
 * 从库可请求 master_repl_offset+1 (已完全追上, 无数据可续) */
static int psync_in_backlog_range(ReplBuf *rb, long long psync_offset) {
    long long start = rb_start_offset(rb);
    long long last_byte_plus_1 = rb->master_repl_offset + 1;
    return psync_offset >= start && psync_offset <= last_byte_plus_1;
}

/* ---- PSYNC 握手状态机 (syncWithMaster 状态转移) ---- */
typedef enum {
    ST_CONNECTING, ST_RECEIVE_PING, ST_SEND_HANDSHAKE,
    ST_RECEIVE_AUTH, ST_RECEIVE_PORT, ST_RECEIVE_IP, ST_RECEIVE_CAPA,
    ST_SEND_PSYNC, ST_RECEIVE_PSYNC
} HandshakeState;

static const char *state_names[] = {
    "CONNECTING", "RECEIVE_PING", "SEND_HANDSHAKE", "RECEIVE_AUTH",
    "RECEIVE_PORT", "RECEIVE_IP", "RECEIVE_CAPA", "SEND_PSYNC", "RECEIVE_PSYNC"
};

/* 简化状态机: 每步返回下一状态 (模拟事件驱动推进) */
static HandshakeState hs_next(HandshakeState st, int has_masterauth, int has_announce_ip) {
    switch (st) {
    case ST_CONNECTING: return ST_RECEIVE_PING;
    case ST_RECEIVE_PING: return ST_SEND_HANDSHAKE;
    case ST_SEND_HANDSHAKE: return has_masterauth ? ST_RECEIVE_AUTH : ST_RECEIVE_PORT;
    case ST_RECEIVE_AUTH: return ST_RECEIVE_PORT;
    case ST_RECEIVE_PORT: return has_announce_ip ? ST_RECEIVE_IP : ST_RECEIVE_CAPA;
    case ST_RECEIVE_IP: return ST_RECEIVE_CAPA;
    case ST_RECEIVE_CAPA: return ST_SEND_PSYNC;
    case ST_SEND_PSYNC: return ST_RECEIVE_PSYNC;
    case ST_RECEIVE_PSYNC: return ST_RECEIVE_PSYNC; /* 终态 */
    }
    return ST_CONNECTING;
}

/* slaveTryPartialResynchronization 写半语义 (L2443-2459): cached_master 才有部分机会 */
typedef struct { int has_cached_master; long long cached_reploff; } SlaveCtx;

static void slave_psync_request(SlaveCtx *sc, char *replid_out, char *offset_out) {
    if (sc->has_cached_master) {
        /* 有缓存: PSYNC replid offset+1 (L2451-2453) */
        memcpy(replid_out, "REPLID1234567890ABCDEF", 22);
        snprintf(offset_out, 32, "%lld", sc->cached_reploff + 1);
    } else {
        /* 无缓存: "?" + "-1" 强制全量 (L2456-2458) */
        memcpy(replid_out, "?", 2);
        memcpy(offset_out, "-1", 3);
    }
}

/* 清理块链 (freeReplicationBacklog 语义简化版) */
static void rb_free(ReplBuf *rb) {
    ReplBufBlock *b = rb->head;
    while (b) {
        ReplBufBlock *next = b->next;
        free(b->buf);
        free(b);
        b = next;
    }
    rb->head = rb->tail = NULL;
    rb->nblocks = 0;
    rb->histlen = 0;
}

/* ---- 测试骨架 ---- */
static int tests = 0, failures = 0;
#define CHECK(cond) do { tests++; if (!(cond)) { failures++; \
    printf("FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } } while (0)

static void test_backlog_blockchain(void) {
    ReplBuf rb;
    rb_init(&rb, 16 * 1024);

    /* 1. 3×5KB 连续追加 → 尾部合并, 恒 1 块 (feedReplicationBuffer L327-341) */
    char data[5000];
    memset(data, 'a', sizeof(data));
    rb_feed(&rb, data, sizeof(data));
    rb_feed(&rb, data, sizeof(data));
    rb_feed(&rb, data, sizeof(data));
    CHECK(rb.nblocks == 1);
    CHECK(rb.histlen == 15000);
    CHECK(rb.master_repl_offset == 1000 + 15000);
    CHECK(rb_start_offset(&rb) == 1000 + 1);

    /* 2. 10KB 追加 → 尾块 avail=1384 填满, 剩 8616 新建块 (L342-372) → 2 块 */
    char big[10000];
    memset(big, 'b', sizeof(big));
    rb_feed(&rb, big, sizeof(big));
    CHECK(rb.nblocks == 2);
    CHECK(rb.tail->used == 8616);
    CHECK(rb.histlen == 25000);

    /* 3. 裁剪 (backlog_size=16KB): 裁首块后 8616 ≤ 16K → **不裁** (源码 L265-266
     *    "if histlen - size <= backlog_size break" — 裁剪后不超限则停) */
    int t = rb_trim(&rb, 100);
    CHECK(t == 0);
    CHECK(rb.nblocks == 2);
    CHECK(rb.histlen == 25000);

    /* 3b. 再喂 17KB (尾块 7768 + 新块 9232) → histlen=42000 → 裁首块后 25616>16K
     *     继续, 再裁后 9232≤16K 停 → t=1 */
    char more[17000];
    memset(more, 'c', sizeof(more));
    rb_feed(&rb, more, sizeof(more));
    CHECK(rb.nblocks == 3);
    t = rb_trim(&rb, 100);
    CHECK(t == 1);
    CHECK(rb.nblocks == 2);
    CHECK(rb.histlen == 25616);
    /* 裁剪后首字节 = master_repl_offset - histlen + 1 (L292-294) */
    CHECK(rb_start_offset(&rb) == rb.master_repl_offset - rb.histlen + 1);

    /* 4. 从库引用 → 不能裁过引用块 (refcount != 1 则不裁, L261) */
    rb_slave_ref(&rb);
    long long before_trim_hist = rb.histlen;
    t = rb_trim(&rb, 100);
    CHECK(t == 0);
    CHECK(rb.histlen == before_trim_hist);

    /* 5. 引用计数语义: 仅 slave 引用 → head refcount == 1 */
    CHECK(rb.head->refcount == 2); /* backlog + slave 双引用 */
    rb_free(&rb);
}

static void test_psync_range(void) {
    ReplBuf rb;
    rb_init(&rb, 64 * 1024);
    char data[10000];
    memset(data, 'c', sizeof(data));
    for (int i = 0; i < 8; i++) rb_feed(&rb, data, sizeof(data)); /* 80KB > 64KB */

    long long start = rb_start_offset(&rb);
    long long last_plus_1 = rb.master_repl_offset + 1;

    /* 范围内: 部分重同步 OK (masterTryPartialResynchronization L756-758) */
    CHECK(psync_in_backlog_range(&rb, start));
    CHECK(psync_in_backlog_range(&rb, rb.master_repl_offset));
    CHECK(psync_in_backlog_range(&rb, last_plus_1)); /* 半开区间: 已完全追上也可续 (0 字节) */
    CHECK(psync_in_backlog_range(&rb, (start + last_plus_1) / 2));

    /* 范围外: start-1 (太旧, 已被裁剪) → 全量 */
    CHECK(!psync_in_backlog_range(&rb, start - 1));
    /* 范围外: last_plus_1+1 (未来偏移) → 全量 */
    CHECK(!psync_in_backlog_range(&rb, last_plus_1 + 1));

    /* 裁剪后 start 前移语义: 从库请求过旧 offset → 拒绝 (L758 语义) */
    rb_trim(&rb, 100);
    long long new_start = rb_start_offset(&rb);
    CHECK(new_start >= start);
    CHECK(!psync_in_backlog_range(&rb, new_start - 1));
    rb_free(&rb);
}

static void test_handshake_states(void) {
    /* 有 masterauth: CONNECTING→PING→HANDSHAKE→AUTH→PORT→CAPA→PSYNC→RECEIVE = 7 跳 */
    HandshakeState st = ST_CONNECTING;
    int steps_auth = 0;
    int saw_auth = 0;
    while (st != ST_RECEIVE_PSYNC) {
        st = hs_next(st, 1, 0);
        steps_auth++;
        if (st == ST_RECEIVE_AUTH) saw_auth = 1;
    }
    CHECK(saw_auth == 1);
    CHECK(steps_auth == 7);

    /* 无 masterauth: AUTH 跳过 (L2730-2731 "if !masterauth skip") → 6 跳 */
    st = ST_CONNECTING;
    int steps_noauth = 0, saw_auth2 = 0;
    while (st != ST_RECEIVE_PSYNC) {
        st = hs_next(st, 0, 0);
        steps_noauth++;
        if (st == ST_RECEIVE_AUTH) saw_auth2 = 1;
    }
    CHECK(saw_auth2 == 0);
    CHECK(steps_noauth == 6);
    CHECK(steps_auth == steps_noauth + 1);

    /* 有 announce_ip: 多 IP 步 → 8 跳 */
    st = ST_CONNECTING;
    int steps_ip = 0, saw_ip = 0;
    while (st != ST_RECEIVE_PSYNC) {
        st = hs_next(st, 1, 1);
        steps_ip++;
        if (st == ST_RECEIVE_IP) saw_ip = 1;
    }
    CHECK(saw_ip == 1);
    CHECK(steps_ip == 8);
}

static void test_psync_request(void) {
    SlaveCtx cached = { .has_cached_master = 1, .cached_reploff = 5000 };
    char replid[40] = {0}, offset[32] = {0};
    slave_psync_request(&cached, replid, offset);
    CHECK(strcmp(replid, "REPLID1234567890ABCDEF") == 0);
    CHECK(strcmp(offset, "5001") == 0); /* offset+1 (L2453) */

    SlaveCtx nocache = { .has_cached_master = 0, .cached_reploff = 0 };
    slave_psync_request(&nocache, replid, offset);
    CHECK(strcmp(replid, "?") == 0);   /* 强制全量 (L2457) */
    CHECK(strcmp(offset, "-1") == 0);  /* (L2458) */
}

int main(void) {
    printf("R-9 replication harness (gcc+ASan)\n");

    test_backlog_blockchain();
    test_psync_range();
    test_handshake_states();
    test_psync_request();

    printf("tests: %d, failures: %d\n", tests, failures);
    return failures ? 1 : 0;
}
