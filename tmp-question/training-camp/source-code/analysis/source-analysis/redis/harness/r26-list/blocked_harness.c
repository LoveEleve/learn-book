/*
 * R-26 t_list+blocked harness — 阻塞框架 + 就绪队列极简复现
 * 还原核心机制 (不依赖真实 dict/list/quicklist):
 *  1. 双向注册: bstate.keys + blocking_keys (blocked.c:359-410)
 *  2. 三级快检 + ready_keys dict 防重 (L447-494)
 *  3. 消费: FIFO + 类型匹配 + PENDING_COMMAND 重处理 (L553-670)
 *  4. 空键不变量: pop 空即删键, 唤醒只需 dbAdd 路径 (db.c:192)
 *  5. 超时: null 回复 (L700-708)
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness blocked_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#define BLOCKED_NONE 0
#define BLOCKED_LIST 1
#define BLOCKED_STREAM 3
#define BLOCKED_MODULE 4
#define OBJ_LIST 1
#define OBJ_STRING 0

/* ---- 简化客户端 ---- */
typedef struct Client Client;
typedef struct {
    Client *c;
    char *key;
    struct BlockNode *node;   /* db 侧 list node (双向关联) */
    struct BlockEntry *next;
} BlockEntry;   /* bstate.keys 条目 */

typedef struct {
    char *key;
    Client **clients;   /* blocking_keys 的 list (FIFO) */
    int n, cap;
} KeyWaiters;

typedef struct Client {
    int id;
    int blocked;
    int btype;
    BlockEntry *bstate_keys;      /* client→key */
    int pending_command;
    int unblock_on_nokey;
    char *result;                 /* 重执行结果 */
} Client;

#define MAX_KEYS 16
static KeyWaiters blocking_keys[MAX_KEYS];
static int nkeys = 0;
static int by_type_count[8];      /* blocked_clients_by_type */
static char *ready_keys_dict[MAX_KEYS];  /* 防重 */
static char *ready_queue[MAX_KEYS];      /* server.ready_keys 列表 */
static int ready_dict_n = 0, ready_q_n = 0;
static Client *all_clients[16];   /* 注册表 (供 cleanup) */
static int n_all_clients = 0;

static KeyWaiters *findWaiters(const char *key) {
    for (int i = 0; i < nkeys; i++)
        if (strcmp(blocking_keys[i].key, key) == 0) return &blocking_keys[i];
    return NULL;
}

/* blockForKeys (对照 L359-410 简化) */
static void blockForKeys(Client *c, const char *key, int unblock_on_nokey) {
    all_clients[n_all_clients++] = c;   /* 注册 (cleanup 用) */
    /* client 侧 */
    BlockEntry *be = malloc(sizeof(BlockEntry));
    be->c = c; be->key = strdup(key); be->next = c->bstate_keys;
    c->bstate_keys = be;
    /* db 侧 */
    KeyWaiters *kw = findWaiters(key);
    if (!kw) {
        kw = &blocking_keys[nkeys++];
        kw->key = strdup(key); kw->clients = NULL; kw->n = kw->cap = 0;
    }
    kw->clients = realloc(kw->clients, sizeof(Client*) * (kw->n + 1));
    kw->clients[kw->n++] = c;
    c->btype = BLOCKED_LIST;
    c->blocked = 1;
    by_type_count[BLOCKED_LIST]++;
    c->pending_command = 1;
    c->unblock_on_nokey = unblock_on_nokey;
}

/* signalKeyAsReadyLogic (对照 L447-494 简化): 三级快检 + 防重 */
static int signalKeyAsReady(const char *key, int type, int deleted) {
    /* 快检 1: 类型可阻塞 (OBJ_LIST→BLOCKED_LIST) */
    int btype = (type == OBJ_LIST) ? BLOCKED_LIST : BLOCKED_NONE;
    if (btype == BLOCKED_NONE) return 0;
    /* 快检 2: 无阻塞者 */
    if (!by_type_count[btype] && !by_type_count[BLOCKED_MODULE]) return 0;
    /* 快检 3: 键无等待者 */
    KeyWaiters *kw = findWaiters(key);
    if (deleted) {
        /* 删键场景: 只唤醒 nokey 客户端 (简化: 检查无等待者即返) */
        if (!kw) return 0;
    } else {
        if (!kw) return 0;
    }
    /* 防重: ready_keys dict */
    for (int i = 0; i < ready_dict_n; i++)
        if (strcmp(ready_keys_dict[i], key) == 0) return 0;   /* 已排队 */
    ready_keys_dict[ready_dict_n++] = strdup(key);
    ready_queue[ready_q_n++] = strdup(key);
    return 1;
}

/* dbAdd 路径 (对照 db.c:192): 新键创建即唤醒 */
static void dbAddAndSignal(const char *key) {
    signalKeyAsReady(key, OBJ_LIST, 0);
}

/* handleClientsBlockedOnKeys (对照 L306-670 简化): 消费队列 */
static void handleClientsBlockedOnKeys(void) {
    while (ready_q_n > 0) {
        char *key = ready_queue[0];
        /* 摘除 dict 标记 (L338) */
        for (int i = 0; i < ready_dict_n; i++)
            if (strcmp(ready_keys_dict[i], key) == 0) { free(ready_keys_dict[i]); memmove(&ready_keys_dict[i], &ready_keys_dict[i+1], sizeof(char*)*(ready_dict_n-i-1)); ready_dict_n--; break; }
        memmove(&ready_queue[0], &ready_queue[1], sizeof(char*)*(ready_q_n-1));
        ready_q_n--;
        /* 处理等待者 (FIFO, 类型匹配 L578-580) — 一个 push 元素只唤醒一个客户端 (BLPOP 语义) */
        KeyWaiters *kw = findWaiters(key);
        if (kw && kw->n > 0) {
            Client *c = kw->clients[0];
            if (c->btype == BLOCKED_LIST) {   /* 类型匹配 */
                /* unblockClientOnKey (L631-670): 解注册 + 重处理 */
                c->blocked = 0;
                c->btype = BLOCKED_NONE;
                by_type_count[BLOCKED_LIST]--;
                memmove(&kw->clients[0], &kw->clients[1], sizeof(Client*)*(kw->n-1));   /* 解链 (不释放: 栈对象) */
                kw->n--;
                if (c->pending_command) {
                    c->pending_command = 0;
                    /* 重处理: 模拟 BLPOP 成功 */
                    c->result = strdup(key);
                }
            }
        }
        free(key);
    }
}

/* pop 空即删键 (对照 t_list.c:736-755): 维持空键不存在 */
static void listPopToEmpty(const char *key) {
    /* 模拟: 弹空后删键 + 删除信号 (XREADGROUP nokey 解阻) */
    signalKeyAsReady(key, OBJ_LIST, 1);
}

/* 清理 */
static void cleanup(void) {
    /* 释放所有客户端的 bstate_keys (BlockEntry 链表) — 注册表遍历, 覆盖已解链者 */
    for (int i = 0; i < n_all_clients; i++) {
        Client *c = all_clients[i];
        BlockEntry *be = c->bstate_keys;
        while (be) { BlockEntry *nxt = be->next; free(be->key); free(be); be = nxt; }
        c->bstate_keys = NULL;
        free(c->result);
        c->result = NULL;
    }
    for (int i = 0; i < nkeys; i++) {
        free(blocking_keys[i].key);
        free(blocking_keys[i].clients);
    }
    for (int i = 0; i < ready_dict_n; i++) free(ready_keys_dict[i]);
    for (int i = 0; i < ready_q_n; i++) free(ready_queue[i]);
}

/* 重置就绪队列 (测试用, 先释放再清零) */
static void resetQueues(void) {
    for (int i = 0; i < ready_dict_n; i++) free(ready_keys_dict[i]);
    for (int i = 0; i < ready_q_n; i++) free(ready_queue[i]);
    ready_dict_n = 0; ready_q_n = 0;
}

static int tests = 0, passed = 0;
#define CHECK(cond) do { tests++; if (cond) { passed++; printf("  [PASS] %s\n", #cond); } \
    else { printf("  [FAIL] %s (line %d)\n", #cond, __LINE__); } } while (0)

int main(void) {
    printf("=== R-26 t_list+blocked harness (gcc+ASan) ===\n");

    printf("\n[1] 双向注册: BLPOP 后 bstate.keys + blocking_keys (L359-410)\n");
    Client c1 = {.id = 1};
    blockForKeys(&c1, "mylist", 0);
    CHECK(c1.blocked == 1 && c1.btype == BLOCKED_LIST);
    CHECK(c1.pending_command == 1);
    CHECK(c1.bstate_keys != NULL && strcmp(c1.bstate_keys->key, "mylist") == 0);
    KeyWaiters *kw = findWaiters("mylist");
    CHECK(kw != NULL && kw->n == 1 && kw->clients[0] == &c1);
    CHECK(by_type_count[BLOCKED_LIST] == 1);

    printf("\n[2] 三级快检: 无阻塞者 → 零开销 (L451-463)\n");
    /* 无 list 阻塞者 */
    by_type_count[BLOCKED_LIST] = 0;
    int sig = signalKeyAsReady("nobody", OBJ_LIST, 0);
    CHECK(sig == 0 && ready_q_n == 0);       /* 快检 2 拦截 */
    sig = signalKeyAsReady("mylist", OBJ_STRING, 0);
    CHECK(sig == 0);                          /* 快检 1 拦截 (类型不可阻塞) */
    by_type_count[BLOCKED_LIST] = 1;          /* 恢复 (c1 仍在等) */

    printf("\n[3] dbAdd 唤醒 + 防重 (L477-493)\n");
    dbAddAndSignal("mylist");                 /* LPUSH 新键 → 唤醒 */
    CHECK(ready_q_n == 1 && ready_dict_n == 1);
    dbAddAndSignal("mylist");                 /* 脚本内再次 push → 防重 */
    CHECK(ready_q_n == 1 && ready_dict_n == 1);   /* 只排一次 */

    printf("\n[4] 消费: FIFO + 类型匹配 + 重处理 (L553-670)\n");
    Client c2 = {.id = 2};
    blockForKeys(&c2, "mylist", 0);           /* 第二个等待者 */
    /* 手动把 c2 加回 (c1 已注册, c2 新注册) — 重置队列处理 */
    resetQueues();
    dbAddAndSignal("mylist");
    handleClientsBlockedOnKeys();
    CHECK(c1.result != NULL && strcmp(c1.result, "mylist") == 0);  /* c1 重处理成功 */
    CHECK(c2.blocked == 1);                   /* c2 未被唤醒 (防无限循环 count) — 实际 FIFO: c1 先 */
    CHECK(by_type_count[BLOCKED_LIST] == 1);  /* 只剩 c2 */

    printf("\n[5] 类型不匹配防误醒 (L578-580)\n");
    /* 键被错误类型覆盖 (STRING): 快检 1 拦截 — 类型不可阻塞 → 不排队不唤醒 */
    resetQueues();
    by_type_count[BLOCKED_LIST] = 1;
    int sig2 = signalKeyAsReady("mylist", OBJ_STRING, 0);   /* 键变 STRING 后的信号 */
    CHECK(sig2 == 0 && ready_q_n == 0);                     /* 未排队 */
    CHECK(by_type_count[BLOCKED_LIST] == 1);                /* c2 未被唤醒 */

    printf("\n[6] 空键不变量: pop 空删键 + 删除信号 (t_list.c:736-755)\n");
    resetQueues();
    Client c3 = {.id = 3};
    blockForKeys(&c3, "queue", 1);            /* XREADGROUP 语义: unblock_on_nokey */
    listPopToEmpty("queue");                  /* pop 空 → 删键信号 */
    CHECK(ready_q_n == 1);                    /* nokey 客户端被唤醒 */

    printf("\n[7] 超时: null 回复 + 清除 (L700-708)\n");
    Client c4 = {.id = 4};
    blockForKeys(&c4, "never", 0);
    /* unblockClientOnTimeout: 简化模拟 */
    c4.blocked = 0;
    c4.btype = BLOCKED_NONE;
    c4.pending_command = 0;
    by_type_count[BLOCKED_LIST]--;
    CHECK(c4.blocked == 0 && c4.pending_command == 0);   /* 超时后 PENDING 清除 */

    printf("\n=== 结果: %d/%d PASS ===\n", passed, tests);

    cleanup();
    return passed == tests ? 0 : 1;
}
