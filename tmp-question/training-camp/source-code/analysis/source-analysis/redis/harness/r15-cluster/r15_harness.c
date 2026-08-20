/*
 * R-15 Cluster harness — 槽散列 + 重定向判定极简复现
 * 从 cluster.h / cluster.c 提取核心语义 (去掉 server 依赖), 实证:
 *  1. keyHashSlot: crc16 & 0x3FFF + {tag} 处理 (cluster.h:43-62)
 *  2. crc16 算法 (crc16.c:82-88, 标准 XMODEM 多项式 0x1021)
 *  3. CLUSTER_REDIR 七种判定 (cluster.h:16-23)
 *  4. 槽迁移状态: migrating_slots_to / importing_slots_from (cluster_legacy.c:617-619)
 *  5. MOVED vs ASK 语义: 槽归属节点 vs 迁移中导入节点
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness r15_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <assert.h>

/* ---- crc16 表 (crc16.c crc16tab 前若干项验证 + 运行时查表) ---- */
/* 标准 CRC-16/XMODEM 多项式 0x1021 */
static uint16_t crc16tab[256];
static void crc16_init(void) {
    for (int i = 0; i < 256; i++) {
        uint16_t crc = (uint16_t)(i << 8);
        for (int j = 0; j < 8; j++)
            crc = (crc << 1) ^ ((crc & 0x8000) ? 0x1021 : 0);
        crc16tab[i] = crc;
    }
}

/* crc16 算法 (crc16.c:82-88) */
static uint16_t crc16(const char *buf, int len) {
    uint16_t crc = 0;
    for (int counter = 0; counter < len; counter++)
        crc = (uint16_t)((crc << 8) ^ crc16tab[((crc >> 8) ^ (uint8_t)*buf++) & 0x00FF]);
    return crc;
}

/* ---- keyHashSlot (cluster.h:43-62) ---- */
static unsigned int keyHashSlot(char *key, int keylen) {
    int s, e;
    for (s = 0; s < keylen; s++)
        if (key[s] == '{') break;
    if (s == keylen) return crc16(key, keylen) & 0x3FFF; /* 无 { → 全键 */
    for (e = s + 1; e < keylen; e++)
        if (key[e] == '}') break;
    if (e == keylen || e == s + 1) return crc16(key, keylen) & 0x3FFF; /* 无 } 或空 tag */
    return crc16(key + s + 1, e - s - 1) & 0x3FFF; /* 只哈希 {} 之间 */
}

/* ---- 重定向判定 (cluster.c:1179-1205 语义) ---- */
enum { REDIR_NONE, REDIR_CROSS_SLOT, REDIR_UNSTABLE, REDIR_ASK,
       REDIR_MOVED, REDIR_DOWN_STATE, REDIR_DOWN_UNBOUND };

/* 判定: 本节点是否服务该槽 (含迁移导入) */
static int getRedirect(unsigned int slot,
                       unsigned int *slot_owner, unsigned int my_id,
                       unsigned int *migrating_to, unsigned int *importing_from)
{
    if (slot_owner[slot] == 0) return REDIR_DOWN_UNBOUND;    /* 无归属 */
    if (slot_owner[slot] == my_id) return REDIR_NONE;        /* 本节点 */
    if (importing_from[slot] != 0) return REDIR_ASK;         /* 导入中 → ASK */
    return REDIR_MOVED;                                       /* 他节点 → MOVED */
}

/* ---- 测试骨架 ---- */
static int tests = 0, failures = 0;
#define CHECK(cond) do { tests++; if (!(cond)) { failures++; \
    printf("FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } } while (0)

static void test_crc16(void) {
    crc16_init();
    /* 已知值: "123456789" 的 CRC-16/XMODEM = 0x31C3 */
    CHECK(crc16("123456789", 9) == 0x31C3);
    /* 空串 = 0 */
    CHECK(crc16("", 0) == 0);
    /* 与 Redis 已知槽位对照: "foo" 的槽 */
    /* keyHashSlot("foo") — 与 Redis 文档已知值 (槽 12182) 对照 */
    CHECK(keyHashSlot("foo", 3) == 12182);
    CHECK(keyHashSlot("bar", 3) == 5061);
}

static void test_keyhashslot_tag(void) {
    /* {tag}: 只哈希 tag 部分 */
    unsigned int a = keyHashSlot("foo{bar}baz", 10);
    unsigned int b = keyHashSlot("{bar}", 5);
    CHECK(a == b); /* tag 相同 → 同槽 */
    CHECK(a == keyHashSlot("bar", 3));

    /* 无 { → 全键 */
    unsigned int c = keyHashSlot("hello", 5);
    CHECK(c == (crc16("hello", 5) & 0x3FFF));

    /* { 但无 } → 全键 */
    CHECK(keyHashSlot("foo{bar", 7) == (crc16("foo{bar", 7) & 0x3FFF));

    /* {} 空 tag → 全键 */
    CHECK(keyHashSlot("foo{}bar", 8) == (crc16("foo{}bar", 8) & 0x3FFF));

    /* 多 { 取第一个: {a}b{b} → 哈希 {a}b? 不 — 取第一个 { 到第一个 } */
    /* keyHashSlot 语义: s=第一个{, e=第一个} → tag = [s+1, e) */
    unsigned int d = keyHashSlot("x{a}y{b}z", 8);
    CHECK(d == (crc16("a", 1) & 0x3FFF));

    /* 槽范围: 0..16383 */
    CHECK(keyHashSlot("", 0) == 0); /* crc16("")=0 */
    CHECK((crc16("k", 1) & 0x3FFF) < 16384);
}

static void test_redirect(void) {
    /* 三节点: 1=本节点, 2, 3 */
    unsigned int owner[16384] = {0};
    unsigned int migrating[16384] = {0};
    unsigned int importing[16384] = {0};
    unsigned int my_id = 1;

    owner[100] = 1; /* 本节点槽 */
    owner[200] = 2; /* 他节点槽 */
    owner[300] = 3; /* 迁移目标 */

    /* 本节点槽 → NONE */
    CHECK(getRedirect(100, owner, my_id, migrating, importing) == REDIR_NONE);

    /* 他节点槽 → MOVED */
    CHECK(getRedirect(200, owner, my_id, migrating, importing) == REDIR_MOVED);

    /* 无归属 → DOWN_UNBOUND */
    CHECK(getRedirect(999, owner, my_id, migrating, importing) == REDIR_DOWN_UNBOUND);

    /* 迁移导入: 槽 200 正在导入 (从节点 2) → ASK */
    importing[200] = 2;
    CHECK(getRedirect(200, owner, my_id, migrating, importing) == REDIR_ASK);
    importing[200] = 0;

    /* 迁移导出: 本节点槽 100 迁往 2 → 本节点仍服务 → NONE */
    migrating[100] = 2;
    CHECK(getRedirect(100, owner, my_id, migrating, importing) == REDIR_NONE);
}

static void test_slot_math(void) {
    /* 16384 = 2^14; 掩码 0x3FFF */
    CHECK(16384 == (1 << 14));
    CHECK((16384 - 1) == 0x3FFF);
    /* crc16 全范围 65536, 槽 = 低 14 位 */
    CHECK((crc16("z", 1) & 0x3FFF) == (crc16("z", 1) % 16384));
}

int main(void) {
    printf("R-15 cluster harness (gcc+ASan)\n");
    crc16_init();

    test_crc16();
    test_keyhashslot_tag();
    test_redirect();
    test_slot_math();

    printf("tests: %d, failures: %d\n", tests, failures);
    return failures ? 1 : 0;
}
