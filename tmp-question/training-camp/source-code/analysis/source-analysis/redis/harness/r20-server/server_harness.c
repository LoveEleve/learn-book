/*
 * R-20 server 骨架 harness — 周期引擎 + hz 自适应 + 时间分级极简复现
 * 还原核心控制流 (不信号/不命令表/不配置全量):
 *  1. serverCron 时间分级: 每 tick / 100ms / 1s 任务调度 (run_with_period 语义)
 *  2. hz 自适应: clients/hz > 200 → hz×2 (上限 500)
 *  3. 事件循环模拟: beforeSleep (每轮) vs cron (周期) 双面
 * 编译: gcc -O0 -o harness server_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CONFIG_DEFAULT_HZ 10
#define CONFIG_MAX_HZ 500
#define MAX_CLIENTS_PER_CLOCK_TICK 200

typedef struct {
    int hz;
    int dynamic_hz;
    long clients;
    long ticks;          /* cron 总执行数 */
    long cronloops;
    long t100ms, t1s;    /* 分级任务计数 */
    long total_time_ms;  /* 模拟运行毫秒 */
} server_t;

static server_t s;

/* run_with_period 语义 (对照 server.c 宏): 返回 1 当 (period == 0 || (cronloops % (period/hz_interval)) == 0) */
static int run_with_period(int period_ms) {
    int ticks_per_period = (period_ms * s.hz) / 1000;
    if (ticks_per_period == 0) ticks_per_period = 1;
    return (s.cronloops % ticks_per_period) == 0;
}

/* 每 tick 执行的 serverCron (对照 server.c:1273-1540, 简化) */
static void serverCron(void) {
    s.ticks++;
    /* 每 tick 先重置为 config_hz (对照 L1283: server.hz = server.config_hz) */
    int reset_hz = s.hz;
    s.hz = CONFIG_DEFAULT_HZ;
    /* hz 自适应 (对照 L1283-1295) */
    if (s.dynamic_hz) {
        while (s.clients / s.hz > MAX_CLIENTS_PER_CLOCK_TICK) {
            s.hz *= 2;
            if (s.hz > CONFIG_MAX_HZ) { s.hz = CONFIG_MAX_HZ; break; }
        }
    }
    /* 每 tick 必做: clientsCron/databasesCron 等 (模拟) */
    /* run_with_period 分级任务 */
    if (run_with_period(100)) s.t100ms++;
    if (run_with_period(1000)) s.t1s++;
    s.cronloops++;
    s.total_time_ms += 1000 / s.hz;   /* 返回 1000/hz */
}

/* beforeSleep 语义: 每轮事件循环必做 (对照 server.c:1637+) */
static void beforeSleep(void) {
    /* AOF flush / 客户端待写 / 阻塞键 (模拟: 计数) */
}

int main(void) {
    printf("[1] 默认 hz=10: cron 每 100ms 一次\n");
    s.hz = CONFIG_DEFAULT_HZ;
    s.dynamic_hz = 0;
    s.clients = 0;
    for (int i = 0; i < 10; i++) serverCron();
    printf("    10 ticks 耗时 %ldms (理论 1000ms)\n", s.total_time_ms);
    printf("    t100ms=%ld t1s=%ld (10 ticks = 1s: 100ms 任务 10 次, 1s 任务 1 次)\n", s.t100ms, s.t1s);

    printf("\n[2] 时间分级: 100ms 任务每 tick, 1s 任务每 10 tick\n");
    /* 上面已经验证: t100ms=10 (每 tick), t1s=1 (每 10 tick) */

    printf("\n[3] hz 自适应: 10000 客户端 → hz 翻倍\n");
    s.hz = CONFIG_DEFAULT_HZ;
    s.dynamic_hz = 1;
    s.clients = 10000;
    serverCron();
    printf("    10000 clients, hz=%d (10000/10=1000 > 200 → 翻倍)\n", s.hz);
    /* 预期: 10 → 20 → 40 → 80 → 160 → 320 → 500(上限) */

    printf("\n[4] 客户端配额: hz 收敛使 clients/hz ≤ 200\n");
    s.hz = CONFIG_DEFAULT_HZ;
    s.clients = 10000;
    for (int i = 0; i < 20 && s.hz < CONFIG_MAX_HZ; i++) serverCron();
    printf("    hz=%d, clients/hz=%.1f (配额 200)\n", s.hz, (double)s.clients / s.hz);
    printf("    clients/hz ≤ 200? %s\n", (double)s.clients / s.hz <= 200 ? "是" : "否");

    printf("\n[5] 客户端少 → 回到默认 hz\n");
    s.clients = 100;
    serverCron();
    printf("    100 clients: hz=%d (回落 config_hz=10)\n", s.hz);

    return 0;
}
