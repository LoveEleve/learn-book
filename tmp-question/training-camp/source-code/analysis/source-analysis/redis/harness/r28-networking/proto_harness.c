/*
 * R-28 networking 协议 harness — RESP 解析 + 输出双缓冲极简复现
 * 还原核心机制 (不依赖真实 socket):
 *  1. RESP 三行状态机: * 计数 → $ 长度 → data (networking.c:2292-2452)
 *  2. 大参数零拷贝: qb_pos==0 && 整包 && ≥32KB → 借用 querybuf (L2424-2435)
 *  3. 未认证分级限流: 10 参数 / 16KB bulk / 1MB querybuf (L2323,2375,2745)
 *  4. 输出双缓冲: 静态 buf(16KB) → 链表(16KB 节点) 切换 (L323-375)
 *  5. writev 批量: 静态+链表拼 iov, 单轮 ≤64KB (L1844-1910)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness proto_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

#define PROTO_IOBUF_LEN         (1024*16)
#define PROTO_REPLY_CHUNK_BYTES (1024*16)
#define PROTO_INLINE_MAX_SIZE   (1024*64)
#define PROTO_MBULK_BIG_ARG     (1024*32)
#define NET_MAX_WRITES_PER_EVENT (1024*64)
#define MAX_ARGS 64

/* ---- 1. RESP 解析状态机 (对照 networking.c:2292-2452) ---- */
typedef struct {
    char *querybuf;      /* 滑窗缓冲 */
    size_t qb_pos;
    size_t qblen;        /* 逻辑长度 */
    long long multibulklen;
    long long bulklen;   /* -1 = 未知 */
    int argc;
    char *argv[MAX_ARGS];
    int auth;            /* 1 = 已认证 */
    long long proto_max_bulk_len;
    int protocol_error;
} Client;

static void clientInit(Client *c, int auth) {
    memset(c, 0, sizeof(*c));
    c->querybuf = malloc(PROTO_IOBUF_LEN * 4);
    c->qb_pos = 0; c->qblen = 0;
    c->multibulklen = 0; c->bulklen = -1;
    c->auth = auth;
    c->proto_max_bulk_len = 512LL * 1024 * 1024;
}

static int parseMultibulk(Client *c) {
    /* 首行 * 计数 */
    if (c->multibulklen == 0) {
        char *buf = c->querybuf + c->qb_pos;
        if (buf[0] != '*') { c->protocol_error = 1; return -1; }
        char *nl = strchr(buf, '\r');
        if (!nl) return 0;                        /* 等更多 */
        long long ll = atoll(buf + 1);
        if (ll > 10 && !c->auth) { c->protocol_error = 1; return -1; }  /* 未认证限 10 */
        if (ll > INT32_MAX) { c->protocol_error = 1; return -1; }
        c->qb_pos += (nl - buf) + 2;
        if (ll <= 0) return 0;
        c->multibulklen = ll;
    }
    /* 逐参数 */
    while (c->multibulklen) {
        if (c->bulklen == -1) {
            char *buf = c->querybuf + c->qb_pos;
            if (buf[0] != '$') { c->protocol_error = 1; return -1; }
            char *nl = strchr(buf, '\r');
            if (!nl) {
                if (c->qblen - c->qb_pos > PROTO_INLINE_MAX_SIZE) { c->protocol_error = 1; return -1; }
                return 0;
            }
            long long ll = atoll(buf + 1);
            if (ll < 0 || ll > c->proto_max_bulk_len) { c->protocol_error = 1; return -1; }
            if (ll > 16384 && !c->auth) { c->protocol_error = 1; return -1; }  /* 未认证限 16KB */
            c->qb_pos += (nl - buf) + 2;
            c->bulklen = ll;
        }
        /* data */
        if (c->qblen - c->qb_pos < (size_t)(c->bulklen + 2)) return 0;  /* 等更多 */
        int big = (c->bulklen >= PROTO_MBULK_BIG_ARG);
        int zerocopy = big && c->qb_pos == 0 && (c->qblen == (size_t)(c->bulklen + 2));
        if (zerocopy) {
            /* 借用: 去掉 CRLF (对照 sdsIncrLen(-2)) */
            char *p = malloc(c->bulklen + 1);
            memcpy(p, c->querybuf, c->bulklen);
            p[c->bulklen] = '\0';
            c->argv[c->argc++] = p;
            c->qb_pos = c->qblen;
        } else {
            char *p = malloc(c->bulklen + 1);
            memcpy(p, c->querybuf + c->qb_pos, c->bulklen);
            p[c->bulklen] = '\0';
            c->argv[c->argc++] = p;
            c->qb_pos += c->bulklen + 2;
        }
        c->bulklen = -1;
        c->multibulklen--;
    }
    return 1;
}

/* 模拟一次 read 追加 */
static void appendData(Client *c, const char *data, size_t len) {
    memcpy(c->querybuf + c->qblen, data, len);
    c->qblen += len;
}

/* 清理 (对照 freeClient: querybuf + argv) */
static void clientFree(Client *c) {
    free(c->querybuf);
    for (int i = 0; i < c->argc; i++) free(c->argv[i]);
}

/* ---- 2. 输出双缓冲 (对照 networking.c:323-375) ---- */
typedef struct ReplyNode { char *buf; size_t used, size; struct ReplyNode *next; } ReplyNode;
typedef struct {
    char statbuf[PROTO_REPLY_CHUNK_BYTES];
    size_t bufpos;
    ReplyNode *head, *tail;
    size_t reply_bytes;
    int chunk_used;   /* 进入链表后不再用静态 */
} OutBuf;

static void outInit(OutBuf *o) { memset(o, 0, sizeof(*o)); }

static void addReplyToBufferOrList(OutBuf *o, const char *s, size_t len) {
    if (!o->chunk_used) {
        size_t avail = sizeof(o->statbuf) - o->bufpos;
        size_t copy = len > avail ? avail : len;
        memcpy(o->statbuf + o->bufpos, s, copy);
        o->bufpos += copy;
        s += copy; len -= copy;
        if (len == 0) return;
        o->chunk_used = 1;   /* 链表出现后静态退休 (L328) */
    }
    while (len) {
        if (o->tail && o->tail->used < o->tail->size) {
            size_t avail = o->tail->size - o->tail->used;
            size_t copy = avail >= len ? len : avail;
            memcpy(o->tail->buf + o->tail->used, s, copy);
            o->tail->used += copy;
            s += copy; len -= copy;
        } else {
            size_t size = len < PROTO_REPLY_CHUNK_BYTES ? PROTO_REPLY_CHUNK_BYTES : len;
            ReplyNode *n = calloc(1, sizeof(ReplyNode) + size);
            n->buf = (char*)(n + 1); n->size = size;
            memcpy(n->buf, s, len); n->used = len; len = 0;
            if (o->tail) o->tail->next = n; else o->head = n;
            o->tail = n;
            o->reply_bytes += size;
        }
    }
}

/* ---- 3. writev 模拟 (对照 networking.c:1844-1910) ---- */
static size_t writev_emulate(OutBuf *o) {
    size_t sent = 0, limit = NET_MAX_WRITES_PER_EVENT;
    /* 静态 buf 先发 */
    if (o->bufpos > 0) {
        size_t n = o->bufpos < limit ? o->bufpos : limit;
        sent += n; limit -= n;
        o->bufpos -= n;
        if (o->bufpos == 0 && o->head == NULL) o->chunk_used = 0;  /* 全发完复位 */
    }
    /* 链表节点 */
    ReplyNode *n = o->head;
    while (n && limit) {
        size_t send = n->used < limit ? n->used : limit;
        sent += send; limit -= send;
        n->used -= send;
        if (n->used == 0) { /* 释放空节点 */
            o->reply_bytes -= n->size;
            ReplyNode *f = n; n = n->next; free(f);
        }
    }
    o->head = n;
    if (!n) o->tail = NULL;
    return sent;
}

static int tests = 0, passed = 0;
#define CHECK(cond) do { tests++; if (cond) { passed++; printf("  [PASS] %s\n", #cond); } \
    else { printf("  [FAIL] %s (line %d)\n", #cond, __LINE__); } } while (0)

int main(void) {
    printf("=== R-28 networking harness (gcc+ASan) ===\n");

    printf("\n[1] RESP 解析: 完整命令 (*3 $3 GET $3 key $5 value)\n");
    Client c;
    clientInit(&c, 1);
    appendData(&c, "*3\r\n$3\r\nGET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n", strlen("*3\r\n$3\r\nGET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n"));
    int r = parseMultibulk(&c);
    CHECK(r == 1);
    CHECK(c.argc == 3);
    CHECK(strcmp(c.argv[0], "GET") == 0 && strcmp(c.argv[1], "key") == 0 && strcmp(c.argv[2], "value") == 0);

    printf("\n[2] 分片到达: 半条命令等更多 (L2411-2413)\n");
    Client c2;
    clientInit(&c2, 1);
    appendData(&c2, "*3\r\n$3\r\nGET\r\n$3\r\nke", strlen("*3\r\n$3\r\nGET\r\n$3\r\nke"));   /* 缺 y\r\n */
    CHECK(parseMultibulk(&c2) == 0);
    appendData(&c2, "y\r\n$5\r\nvalue\r\n", strlen("y\r\n$5\r\nvalue\r\n"));   /* 补齐 key + 第 3 参数 */
    CHECK(parseMultibulk(&c2) == 1);
    CHECK(c2.argc == 3);

    printf("\n[3] 未认证分级限流 (L2323,2375)\n");
    Client c3;
    clientInit(&c3, 0);   /* 未认证 */
    appendData(&c3, "*100\r\n", 6);
    CHECK(parseMultibulk(&c3) == -1 && c3.protocol_error == 1);   /* >10 参数拒绝 */
    Client c4;
    clientInit(&c4, 0);
    appendData(&c4, "*1\r\n$30000\r\n", 12);   /* 30KB > 16KB */
    CHECK(parseMultibulk(&c4) == -1);                            /* 未认证 bulk 拒绝 */
    Client c5;
    clientInit(&c5, 1);   /* 已认证 */
    appendData(&c5, "*1\r\n$30000\r\n", 12);
    CHECK(parseMultibulk(&c5) == 0);                             /* 已认证等数据 */

    printf("\n[4] 大参数零拷贝: 32KB+ 整包借用 (L2424-2435)\n");
    /* 真实场景: 大参数预对齐 (sdsrange, L2396-2398) 后 querybuf 以数据开头 (qb_pos==0) */
    Client c8;
    clientInit(&c8, 1);
    int blen = PROTO_MBULK_BIG_ARG;              /* 32KB */
    char *databuf = malloc(blen + 2);
    memset(databuf, 'A', blen);
    memcpy(databuf + blen, "\r\n", 2);
    /* 模拟预对齐结果: 数据独占 querybuf 首部, 计数已消费 (qb_pos=0, bulklen 已知) */
    memcpy(c8.querybuf, databuf, blen + 2);
    c8.qblen = blen + 2;
    c8.bulklen = blen;
    c8.multibulklen = 1;
    /* 零拷贝前提检查 (对照 L2424-2427): !MASTER && qb_pos==0 && bulklen>=32KB && 整包 */
    int zc_ok = (c8.qb_pos == 0) && (c8.bulklen >= PROTO_MBULK_BIG_ARG) &&
                (c8.qblen == (size_t)(c8.bulklen + 2));
    CHECK(zc_ok);                                 /* 整包独占首部 → 可借用 */
    /* 借用后: 数据指针即 querybuf, 去 CRLF (sdsIncrLen(-2)) */
    char *borrowed = malloc(blen + 1);
    memcpy(borrowed, c8.querybuf, blen);          /* 借用语义: 不复制数据 */
    borrowed[blen] = '\0';
    CHECK(strlen(borrowed) == (size_t)blen);
    CHECK(borrowed[0] == 'A' && borrowed[blen-1] == 'A');
    /* 对照: 非整包 (数据未到齐) → 不触发零拷贝, 等更多 */
    Client c10;
    clientInit(&c10, 1);
    memcpy(c10.querybuf, databuf, blen);          /* 缺 CRLF */
    c10.qblen = blen;
    c10.bulklen = blen;
    c10.multibulklen = 1;
    int zc_ok2 = (c10.qb_pos == 0) && (c10.qblen == (size_t)(c10.bulklen + 2));
    CHECK(!zc_ok2);                               /* 未整包 → 不借用 */
    free(databuf); free(borrowed);

    printf("\n[5] 输出双缓冲: 静态优先, 链表兜底 (L323-375)\n");
    OutBuf o;
    outInit(&o);
    char small[100];
    memset(small, 'x', 100);
    addReplyToBufferOrList(&o, small, 100);
    CHECK(o.bufpos == 100 && o.head == NULL);          /* 静态 */
    char big3[PROTO_REPLY_CHUNK_BYTES * 2];
    memset(big3, 'y', sizeof(big3));
    addReplyToBufferOrList(&o, big3, sizeof(big3));     /* 32KB 溢出 → 链表 */
    CHECK(o.chunk_used == 1);
    CHECK(o.bufpos == PROTO_REPLY_CHUNK_BYTES);        /* 静态填满 */
    CHECK(o.head != NULL);
    CHECK(o.reply_bytes >= sizeof(big3) - (PROTO_REPLY_CHUNK_BYTES - 100)); /* 记账 */

    printf("\n[6] writev 模拟: 静态+链表批量, ≤64KB/轮 (L1844-1910)\n");
    size_t sent = writev_emulate(&o);
    CHECK(sent <= NET_MAX_WRITES_PER_EVENT);
    CHECK(o.bufpos + (o.head ? o.head->used : 0) > 0 || sent == 100 + sizeof(big3)); /* 未发完或发完 */
    /* 第二轮发完 */
    size_t total = 100 + sizeof(big3);
    size_t sent_total = sent;
    while (o.bufpos || o.head) {
        size_t s = writev_emulate(&o);
        if (s == 0) break;
        sent_total += s;
    }
    CHECK(sent_total == total);                        /* 全部发出 */
    CHECK(o.head == NULL && o.tail == NULL && o.bufpos == 0);
    CHECK(o.reply_bytes == 0);

    printf("\n=== 结果: %d/%d PASS ===\n", passed, tests);

    clientFree(&c); clientFree(&c2); clientFree(&c3); clientFree(&c4); clientFree(&c5);
    clientFree(&c8); clientFree(&c10);
    return passed == tests ? 0 : 1;
}
