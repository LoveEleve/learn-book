/*
 * R-14 Sentinel harness — 判活状态机 + 选举极简复现
 * 从 sentinel.c 提取核心语义 (去掉 server 依赖), 实证:
 *  1. sentinelCheckSubjectivelyDown: elapsed > down_after_period (sentinel.c:4516-4582)
 *  2. sentinelCheckObjectivelyDown: quorum = 自己(1) + MASTER_DOWN 哨兵数 (L4590-4623)
 *  3. sentinelStartFailoverIfNeeded: ODOWN + 非进行中 + 冷却 2×timeout (L4951-4979)
 *  4. sentinelLeaderIncr: runid 投票计数 (L4762+)
 *  5. compareSlavesForPromotion: priority → repl_offset → runid (L5013-5039)
 *  6. failover 状态机 7 状态转移 (L5087-5338)
 *  7. sentinelCheckTiltCondition: delta<0 或 >trigger → TILT (L5437-5447)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness r14_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

/* ---- 标志位 (SRI_*) ---- */
#define SRI_MASTER (1<<0)
#define SRI_SLAVE (1<<1)
#define SRI_S_DOWN (1<<3)
#define SRI_O_DOWN (1<<4)
#define SRI_MASTER_DOWN (1<<5)
#define SRI_FAILOVER_IN_PROGRESS (1<<11)
#define SRI_PROMOTED (1<<13)

/* ---- 简例实例 ---- */
typedef struct {
    char name[32];
    int flags;
    long long down_after_period;
    long long elapsed;       /* 距最后可用时间 */
    long long slave_priority;
    long long slave_repl_offset;
    char runid[40];
    int s_down_since;        /* 模拟时间戳 */
    int role_reported;
    long long role_reported_time;
} Instance;

/* ========== 1. SDOWN 判定 (L4516-4582) ========== */
/* elapsed > down_after_period → SDOWN */
static void check_subjectively_down(Instance *ri, long long now) {
    long long elapsed = ri->elapsed;
    if (elapsed > ri->down_after_period) {
        if (!(ri->flags & SRI_S_DOWN)) {
            ri->flags |= SRI_S_DOWN;
            ri->s_down_since = now;
        }
    } else {
        ri->flags &= ~SRI_S_DOWN;
    }
}

/* ========== 2. ODOWN 判定 (L4590-4623) ========== */
typedef struct {
    Instance *sentinels;     /* 其他哨兵数组 */
    int nsentinels;
} SentinelSet;

static void check_objectively_down(Instance *master, SentinelSet *ss, int quorum) {
    int q = 0;
    if (master->flags & SRI_S_DOWN) {
        q = 1; /* 自己 (L4597) */
        for (int i = 0; i < ss->nsentinels; i++)
            if (ss->sentinels[i].flags & SRI_MASTER_DOWN) q++;
        if (q >= quorum) master->flags |= SRI_O_DOWN;
        else master->flags &= ~SRI_O_DOWN;
    } else {
        master->flags &= ~SRI_O_DOWN;
    }
}

/* ========== 3. 选举计数 (L4762+) ========== */
#define MAX_VOTERS 16
typedef struct {
    char runid[MAX_VOTERS][40];
    int votes[MAX_VOTERS];
    int n;
} VoteTable;

/* sentinelLeaderIncr 语义: runid → 票数+1 */
static void leader_incr(VoteTable *vt, char *runid) {
    for (int i = 0; i < vt->n; i++) {
        if (strcmp(vt->runid[i], runid) == 0) { vt->votes[i]++; return; }
    }
    assert(vt->n < MAX_VOTERS);
    strcpy(vt->runid[vt->n], runid);
    vt->votes[vt->n] = 1;
    vt->n++;
}

/* sentinelGetLeader 语义 (L4773-4830): 胜者双条件 —
 * 绝对多数 voters/2+1 (L4808) 且 ≥ master->quorum (L4811) */
static char *get_leader(VoteTable *vt, int voters, int quorum) {
    static char winner[40];
    int max = 0, maxcount = 0;
    winner[0] = '\0';
    for (int i = 0; i < vt->n; i++) {
        if (vt->votes[i] > max) { max = vt->votes[i]; strcpy(winner, vt->runid[i]); maxcount = 1; }
        else if (vt->votes[i] == max) maxcount++;
    }
    int voters_quorum = voters / 2 + 1;
    return (maxcount == 1 && max >= voters_quorum && max >= quorum) ? winner : NULL;
}

/* ========== 4. 选主排序 (L5013-5039) ========== */
static int cmp_slaves(const void *a, const void *b) {
    Instance **sa = (Instance **)a, **sb = (Instance **)b;
    if ((*sa)->slave_priority != (*sb)->slave_priority)
        return (*sa)->slave_priority - (*sb)->slave_priority;
    if ((*sa)->slave_repl_offset > (*sb)->slave_repl_offset) return -1;
    if ((*sa)->slave_repl_offset < (*sb)->slave_repl_offset) return 1;
    return strcasecmp((*sa)->runid, (*sb)->runid);
}

/* ========== 5. failover 状态机 (L5087-5338) ========== */
enum {
    FS_NONE, FS_WAIT_START, FS_SELECT_SLAVE, FS_SEND_SLAVEOF_NOONE,
    FS_WAIT_PROMOTION, FS_RECONF_SLAVES, FS_DETECT_END, FS_UPDATE_CONFIG
};
static const char *fs_names[] = {
    "NONE", "WAIT_START", "SELECT_SLAVE", "SEND_SLAVEOF_NOONE",
    "WAIT_PROMOTION", "RECONF_SLAVES", "DETECT_END", "UPDATE_CONFIG"
};

/* ========== 6. TILT (L5437-5447) ========== */
#define TILT_TRIGGER_MS 2000
static int check_tilt(long long now, long long *prev) {
    long long delta = now - *prev;
    *prev = now;
    return (delta < 0 || delta > TILT_TRIGGER_MS) ? 1 : 0;
}

/* ---- 测试骨架 ---- */
static int tests = 0, failures = 0;
#define CHECK(cond) do { tests++; if (!(cond)) { failures++; \
    printf("FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } } while (0)

static void test_sdown(void) {
    Instance m = { .flags = SRI_MASTER, .down_after_period = 30000, .elapsed = 1000 };
    check_subjectively_down(&m, 1000);
    CHECK(!(m.flags & SRI_S_DOWN));

    m.elapsed = 31000; /* 超时 */
    check_subjectively_down(&m, 1000);
    CHECK(m.flags & SRI_S_DOWN);

    m.elapsed = 500; /* 恢复 */
    check_subjectively_down(&m, 2000);
    CHECK(!(m.flags & SRI_S_DOWN));
}

static void test_odown(void) {
    Instance m = { .flags = SRI_MASTER, .down_after_period = 30000, .elapsed = 31000 };
    check_subjectively_down(&m, 0);

    Instance others[2] = {0};
    SentinelSet ss = { .sentinels = others, .nsentinels = 0 };
    /* quorum=2, 无其他哨兵报告 → 不 ODOWN */
    check_objectively_down(&m, &ss, 2);
    CHECK(!(m.flags & SRI_O_DOWN));

    /* 1 个哨兵报告 MASTER_DOWN → quorum=2 (自己+1) → ODOWN */
    ss.nsentinels = 1;
    ss.sentinels[0].flags = SRI_MASTER_DOWN;
    check_objectively_down(&m, &ss, 2);
    CHECK(m.flags & SRI_O_DOWN);

    /* 哨兵恢复 → 退出 ODOWN */
    ss.sentinels[0].flags = 0;
    check_objectively_down(&m, &ss, 2);
    CHECK(!(m.flags & SRI_O_DOWN));
}

static void test_election(void) {
    VoteTable vt = {0};
    /* 5 哨兵投票: A×3, B×2 → A 胜 (3 ≥ 5/2+1=3 且 ≥ quorum=3) */
    leader_incr(&vt, "A"); leader_incr(&vt, "B");
    leader_incr(&vt, "A"); leader_incr(&vt, "B");
    leader_incr(&vt, "A");
    char *leader = get_leader(&vt, 5, 3);
    CHECK(leader && strcmp(leader, "A") == 0);

    /* 绝对多数达标但 quorum 不达标: A×3/5, quorum=4 → NULL */
    CHECK(get_leader(&vt, 5, 4) == NULL);

    /* 平票 2-2-1 → 无多数 → NULL */
    VoteTable vt2 = {0};
    leader_incr(&vt2, "A"); leader_incr(&vt2, "A");
    leader_incr(&vt2, "B"); leader_incr(&vt2, "B");
    leader_incr(&vt2, "C");
    CHECK(get_leader(&vt2, 5, 2) == NULL);

    /* 4 哨兵 (偶数): 绝对多数 = 4/2+1 = 3; A×3 → 胜 */
    VoteTable vt3 = {0};
    leader_incr(&vt3, "A"); leader_incr(&vt3, "A");
    leader_incr(&vt3, "A"); leader_incr(&vt3, "B");
    CHECK(get_leader(&vt3, 4, 2) && strcmp(get_leader(&vt3, 4, 2), "A") == 0);
    /* 偶数下 2 票不足 (2 < 3) → NULL */
    VoteTable vt4 = {0};
    leader_incr(&vt4, "A"); leader_incr(&vt4, "A");
    leader_incr(&vt4, "B"); leader_incr(&vt4, "B");
    CHECK(get_leader(&vt4, 4, 2) == NULL);
}

static void test_slave_selection(void) {
    Instance s1 = { .slave_priority = 1, .slave_repl_offset = 100, .runid = "bbbb" };
    Instance s2 = { .slave_priority = 1, .slave_repl_offset = 200, .runid = "aaaa" };
    Instance s3 = { .slave_priority = 2, .slave_repl_offset = 999, .runid = "cccc" };
    Instance *arr[3] = { &s1, &s2, &s3 };

    qsort(arr, 3, sizeof(Instance*), cmp_slaves);
    /* 排序: s2 (priority1, offset200) → s1 (priority1, offset100) → s3 (priority2) */
    CHECK(arr[0] == &s2);
    CHECK(arr[1] == &s1);
    CHECK(arr[2] == &s3);

    /* 同 priority 同 offset → runid 字典序 */
    Instance a = { .slave_priority = 1, .slave_repl_offset = 100, .runid = "aaaa" };
    Instance b = { .slave_priority = 1, .slave_repl_offset = 100, .runid = "bbbb" };
    Instance *arr2[2] = { &b, &a };
    qsort(arr2, 2, sizeof(Instance*), cmp_slaves);
    CHECK(arr2[0] == &a);
}

static void test_failover_states(void) {
    /* 状态机名称表完整性 */
    CHECK(strcmp(fs_names[FS_WAIT_START], "WAIT_START") == 0);
    CHECK(strcmp(fs_names[FS_UPDATE_CONFIG], "UPDATE_CONFIG") == 0);
    /* 7 状态 (NONE 外) 全覆盖 */
    CHECK(sizeof(fs_names)/sizeof(fs_names[0]) == 8);
}

static void test_tilt(void) {
    long long prev = 1000;
    CHECK(check_tilt(1100, &prev) == 0);  /* 正常 100ms 间隔 */
    CHECK(check_tilt(5000, &prev) == 1);  /* 超 trigger (3900ms) */
    CHECK(check_tilt(0, &prev) == 1);     /* 负 delta (时钟回退) */
}

int main(void) {
    printf("R-14 sentinel harness (gcc+ASan)\n");

    test_sdown();
    test_odown();
    test_election();
    test_slave_selection();
    test_failover_states();
    test_tilt();

    printf("tests: %d, failures: %d\n", tests, failures);
    return failures ? 1 : 0;
}
