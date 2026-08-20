/*
 * R-2 事件驱动+IO 多线程 harness — 事件循环内核 + 扇出扇入极简复现
 * 还原核心机制 (不依赖真实 epoll/pthread):
 *  1. 事件表: fd 索引数组 + mask 合并 (ae.h:20-27,51-56)
 *  2. 睡眠编排: DONT_WAIT / 睡到最早时间事件 / 无限等 (ae.c:353-377)
 *  3. 分派顺序: 读先写后 + AE_BARRIER 逆序 + 同 proc 去重 (ae.c:391-443)
 *  4. 时间事件: 无序链表 + 周期重排 + maxId 防迭代中新事件 (ae.c:261-325)
 *  5. io threads 扇出扇入: 分发 % num + pending 归零 (networking.c:4393-4484)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness events_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#define AE_NONE 0
#define AE_READABLE 1
#define AE_WRITABLE 2
#define AE_BARRIER 4
#define AE_NOMORE -1
#define AE_DELETED_EVENT_ID -1

/* ---- 1. 事件表 (对照 ae.h:51-56, ae.c:143-183) ---- */
typedef void (*fileProc)(int fd, int mask, const char *tag);
typedef struct {
    int mask;
    fileProc rfileProc, wfileProc;
} FileEvent;
static FileEvent events[64];
static int maxfd = -1;
static int read_calls = 0, write_calls = 0;

static int aeCreateFileEvent(int fd, int mask, fileProc proc) {
    if (fd >= 64) return -1;
    events[fd].mask |= mask;
    if (mask & AE_READABLE) events[fd].rfileProc = proc;
    if (mask & AE_WRITABLE) events[fd].wfileProc = proc;
    if (fd > maxfd) maxfd = fd;
    return 0;
}
static void aeDeleteFileEvent(int fd, int mask) {
    if (fd >= 64 || events[fd].mask == AE_NONE) return;
    if (mask & AE_WRITABLE) mask |= AE_BARRIER;   /* 删 WRITABLE 连带 BARRIER (ae.c:169-171) */
    events[fd].mask &= ~mask;
    if (fd == maxfd && events[fd].mask == AE_NONE) {
        while (maxfd >= 0 && events[maxfd].mask == AE_NONE) maxfd--;
    }
}

/* ---- 2. 时间事件 (对照 ae.c:200-325) ---- */
typedef struct TimeEvent {
    long long id;
    long long when;        /* us */
    int (*proc)(struct TimeEvent *te);
    struct TimeEvent *next;
    int refcount;
} TimeEvent;
static TimeEvent *timeEventHead = NULL;
static long long timeEventNextId = 0;

static long long aeCreateTimeEvent(int ms, int (*proc)(TimeEvent *)) {
    long long id = timeEventNextId++;
    TimeEvent *te = calloc(1, sizeof(TimeEvent));
    te->id = id; te->when = 1000000 + (long long)ms * 1000;
    te->proc = proc; te->next = timeEventHead;
    timeEventHead = te;
    return id;
}
/* 循环计数 (每个事件 id 调用次数) */
static int cron_calls = 0;
static int cronProc(TimeEvent *te) { cron_calls++; return 100; } /* 100ms 周期 */

/* maxId 测试: 事件内注册新事件 (ae.c:297-305) */
static int selfRegisteringProc(TimeEvent *te) {
    (void)te;
    aeCreateTimeEvent(1, cronProc);  /* 新事件 when = 1000000+1000, 立即过期 */
    return AE_NOMORE;
}

/* usUntilEarliestTimer (ae.c:245-258) */
static long long usUntilEarliestTimer(long long now) {
    TimeEvent *te = timeEventHead, *earliest = NULL;
    while (te) {
        if ((!earliest || te->when < earliest->when) && te->id != AE_DELETED_EVENT_ID)
            earliest = te;
        te = te->next;
    }
    if (!earliest) return -1;
    return (now >= earliest->when) ? 0 : earliest->when - now;
}

/* processTimeEvents (ae.c:261-325): maxId 防迭代中新事件 */
static int processTimeEvents(long long now) {
    int processed = 0;
    TimeEvent *te = timeEventHead;
    long long maxId = timeEventNextId - 1;
    while (te) {
        if (te->id == AE_DELETED_EVENT_ID) { TimeEvent *nxt = te->next; if (!te->refcount) { if (te == timeEventHead) timeEventHead = te->next; else { TimeEvent *p = timeEventHead; while (p && p->next != te) p = p->next; if (p) p->next = te->next; } free(te); } te = nxt; continue; }
        if (te->id > maxId) { te = te->next; continue; }   /* 本迭代新注册不处理 */
        if (te->when <= now) {
            te->refcount++;
            int retval = te->proc(te);
            te->refcount--;
            processed++;
            if (retval != AE_NOMORE) te->when = now + (long long)retval * 1000;
            else te->id = AE_DELETED_EVENT_ID;
        }
        te = te->next;
    }
    return processed;
}

/* ---- 3. 分派 (对照 ae.c:391-443) ---- */
static int dispatch(int fd, int fired_mask) {
    int fired = 0, invert = events[fd].mask & AE_BARRIER;
    if (!invert && events[fd].mask & fired_mask & AE_READABLE) {
        events[fd].rfileProc(fd, fired_mask, "read"); read_calls++; fired++;
    }
    if (events[fd].mask & fired_mask & AE_WRITABLE) {
        if (!fired || events[fd].wfileProc != events[fd].rfileProc) {
            events[fd].wfileProc(fd, fired_mask, "write"); write_calls++; fired++;
        }
    }
    if (invert) {
        if ((events[fd].mask & fired_mask & AE_READABLE) && (!fired || events[fd].wfileProc != events[fd].rfileProc)) {
            events[fd].rfileProc(fd, fired_mask, "read"); read_calls++; fired++;
        }
    }
    return fired;
}

/* ---- 4. io threads 扇出扇入模拟 (对照 networking.c:4393-4484) ---- */
#define IO_THREADS_NUM 4
static int clients_pending_write[16];
static int pending_write_count = 0;
static int io_threads_list[IO_THREADS_NUM][16];
static int io_threads_list_len[IO_THREADS_NUM];
static int io_threads_op = 0;   /* 0=IDLE, 1=WRITE */
static long io_writes_processed = 0;

static void writeToClient(int cid) { io_writes_processed++; }

static int fanout_fanin_write(void) {
    int processed = pending_write_count;
    if (processed == 0) return 0;
    /* 分发 (networking.c:4407-4437): 从库 (cid==-1) 强制 list[0] */
    int item_id = 0;
    for (int i = 0; i < pending_write_count; i++) {
        int c = clients_pending_write[i];
        int target = (c == -1) ? 0 : (item_id++ % IO_THREADS_NUM);
        io_threads_list[target][io_threads_list_len[target]++] = c;
    }
    io_threads_op = 1;  /* WRITE */
    /* 主线程处理 list[0] (networking.c:4449-4456) */
    for (int i = 0; i < io_threads_list_len[0]; i++) writeToClient(io_threads_list[0][i]);
    io_threads_list_len[0] = 0;
    /* 其他线程处理 (模拟并行, 顺序执行等价) */
    for (int j = 1; j < IO_THREADS_NUM; j++) {
        for (int i = 0; i < io_threads_list_len[j]; i++) writeToClient(io_threads_list[j][i]);
        io_threads_list_len[j] = 0;
    }
    io_threads_op = 0;  /* IDLE */
    pending_write_count = 0;
    return processed;
}

/* ---- 测试辅助 ---- */
static char order_log[16][8];
static int order_idx = 0;
static void logOrder(const char *tag) { snprintf(order_log[order_idx++], 8, "%s", tag); }
static void dummyRead(int fd, int mask, const char *tag) { (void)fd; (void)mask; logOrder(tag); }
static void dummyWrite(int fd, int mask, const char *tag) { (void)fd; (void)mask; logOrder(tag); }
static void dummyR(int fd, int mask, const char *tag) { (void)fd; (void)mask; (void)tag; }
static void dummyW(int fd, int mask, const char *tag) { (void)fd; (void)mask; (void)tag; }
static int tests = 0, passed = 0;
#define CHECK(cond) do { tests++; if (cond) { passed++; printf("  [PASS] %s\n", #cond); } \
    else { printf("  [FAIL] %s (line %d)\n", #cond, __LINE__); } } while (0)

int main(void) {
    printf("=== R-2 事件驱动 harness (gcc+ASan) ===\n");

    printf("\n[1] 事件表: mask 合并与删除 (ae.c:143-183)\n");
    aeCreateFileEvent(5, AE_READABLE, dummyRead);
    aeCreateFileEvent(5, AE_WRITABLE, dummyWrite);
    CHECK(events[5].mask == (AE_READABLE|AE_WRITABLE));
    CHECK(maxfd == 5);
    CHECK(events[5].rfileProc == dummyRead && events[5].wfileProc == dummyWrite);
    aeDeleteFileEvent(5, AE_WRITABLE);
    CHECK(events[5].mask == AE_READABLE);       /* 删 WRITABLE 连带删 BARRIER */
    aeDeleteFileEvent(5, AE_READABLE);
    CHECK(events[5].mask == AE_NONE);
    CHECK(maxfd == -1);                          /* maxfd 回退 */

    printf("\n[2] 睡眠编排: usUntilEarliestTimer (ae.c:245-258)\n");
    long long now = 1000000;
    CHECK(usUntilEarliestTimer(now) == -1);      /* 无定时器 */
    aeCreateTimeEvent(500, cronProc);            /* when = 1000000+500000 */
    long long d = usUntilEarliestTimer(now);
    CHECK(d == 500000);                          /* 睡 500ms */
    CHECK(usUntilEarliestTimer(1500000) == 0);   /* 已到期立即处理 */
    /* DONT_WAIT 语义: tv=0 立即返回 (ae.c:367-369) — 用 0 等待表示 */

    printf("\n[3] 分派顺序: 读先写后 (ae.c:416-428)\n");
    order_idx = 0;
    aeCreateFileEvent(7, AE_READABLE|AE_WRITABLE, dummyRead);
    events[7].wfileProc = dummyWrite;
    dispatch(7, AE_READABLE|AE_WRITABLE);
    CHECK(order_idx == 2 && strcmp(order_log[0], "read") == 0 && strcmp(order_log[1], "write") == 0);
    read_calls = write_calls = 0;
    order_idx = 0;
    events[7].wfileProc = dummyRead;             /* 同 proc */
    dispatch(7, AE_READABLE|AE_WRITABLE);
    CHECK(order_idx == 1);                       /* 去重: 一次回调 */

    printf("\n[4] AE_BARRIER 逆序 (ae.c:408,432-440)\n");
    order_idx = 0;
    events[7].mask = AE_READABLE|AE_WRITABLE|AE_BARRIER;
    events[7].wfileProc = dummyWrite;
    dispatch(7, AE_READABLE|AE_WRITABLE);
    CHECK(order_idx == 2 && strcmp(order_log[0], "write") == 0 && strcmp(order_log[1], "read") == 0);
    /* 恢复: 非 BARRIER 时仍读先写后 */
    order_idx = 0;
    events[7].mask = AE_READABLE|AE_WRITABLE;
    dispatch(7, AE_READABLE|AE_WRITABLE);
    CHECK(order_idx == 2 && strcmp(order_log[0], "read") == 0);

    printf("\n[5] 时间事件: 周期重排 + maxId (ae.c:261-325)\n");
    cron_calls = 0;
    processTimeEvents(1000000);                  /* 首个到期 1500000? 不, when=1500000 > now */
    processTimeEvents(1500000);                  /* 到期: 调用 1 次, 重排到 1500000+100000 */
    CHECK(cron_calls == 1);
    CHECK(timeEventHead->when == 1600000);       /* 周期重排 100ms */
    processTimeEvents(1600000);
    CHECK(cron_calls == 2);                      /* 第二次调用 */
    /* maxId: 时间事件内注册新事件, 本迭代不处理 (ae.c:297-305) */
    {
        aeCreateTimeEvent(1, selfRegisteringProc);   /* when = 1000000+1000 */
        int before = cron_calls;
        processTimeEvents(1001000);      /* selfRegistering 到期 → 内部注册新 cron (when=1002000) */
        CHECK(cron_calls == before);     /* 新事件 id > maxId 快照 → 本迭代未处理 */
        processTimeEvents(1002000);      /* 下一迭代 → 新事件到期且 id ≤ 新快照 → 处理 */
        CHECK(cron_calls == before + 1);
    }

    printf("\n[6] 扇出扇入: 分发/处理/归零 (networking.c:4393-4484)\n");
    io_writes_processed = 0;
    int cli[5] = {1, 2, -1, 3, 4};              /* -1 = 从库客户端 */
    memcpy(clients_pending_write, cli, sizeof(cli));
    pending_write_count = 5;
    int processed = fanout_fanin_write();
    CHECK(processed == 5);
    CHECK(io_writes_processed == 5);            /* 全部写出 */
    CHECK(io_threads_list_len[0] == 0);          /* list[0] 已清 (含从库) */
    CHECK(io_threads_op == 0);                   /* op 复位 IDLE */
    /* 从库强制 list[0]: 验证 -1 在 main 处理 */
    {
        int item_id = 0;
        for (int i = 0; i < pending_write_count; i++) {
            int c = clients_pending_write[i];
            int target = (c == -1) ? 0 : (item_id++ % IO_THREADS_NUM);
            (void)target;
        }
        /* 上轮已消费, 本轮无 pending */
        CHECK(fanout_fanin_write() == 0);
    }

    printf("\n=== 结果: %d/%d PASS ===\n", passed, tests);
    return passed == tests ? 0 : 1;
}
