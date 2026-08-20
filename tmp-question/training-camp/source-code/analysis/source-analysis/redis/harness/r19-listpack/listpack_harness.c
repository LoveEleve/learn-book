/*
 * R-19 listpack harness — 紧凑布局 + backlen 无级联 + 双向遍历 + 写路径极简复现
 * 还原核心控制流 (编码族简化为 7BIT_INT/6BIT_STR/EOF; 不批量/不完整性校验):
 *  1. 布局: 6B 头 (total_bytes + num_elements) + entry* + 0xFF EOF
 *  2. 编码: 7BIT_INT (0x00xxxxxx, 2B) / 6BIT_STR (0x10xxxxxx, 1B 头) + backlen (自身长度)
 *  3. backlen 无级联: 插入变长只改头部, 后驱不受影响 (对照 ziplist 级联)
 *  4. lpPrev O(1) 向后遍历
 *  5. lpInsert 三合一 (插入/删除/替换) — 单次 memmove
 * 编译: gcc -O0 -o harness listpack_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#define LP_HDR_SIZE 6
#define LP_EOF 0xFF
#define ENC_INT 0x00        /* 7bit 整数: 0x00xxxxxx */
#define ENC_STR 0x80        /* 6bit 字符串: 0x10xxxxxx (简化为 0x80|len) */
#define ENC_STR_MASK 0xC0

/* ---------- 头部访问 ---------- */
static uint32_t lp_total(unsigned char *lp) {
    return lp[0] | (lp[1]<<8) | (lp[2]<<16) | ((uint32_t)lp[3]<<24);
}
static void lp_set_total(unsigned char *lp, uint32_t v) {
    lp[0]=v&0xff; lp[1]=(v>>8)&0xff; lp[2]=(v>>16)&0xff; lp[3]=(v>>24)&0xff;
}
static uint16_t lp_numele(unsigned char *lp) { return lp[4] | (lp[5]<<8); }
static void lp_set_numele(unsigned char *lp, uint16_t v) { lp[4]=v&0xff; lp[5]=(v>>8)&0xff; }

/* ---------- backlen 编码 (简化为 1B: 长度 ≤127; 真实 1-5B) ---------- */
static unsigned char *encode_backlen(unsigned char *buf, uint64_t l) {
    buf[0] = (unsigned char)l;     /* 简化: 单字节 */
    return buf + 1;
}
static uint64_t decode_backlen(unsigned char *p) { return *p; }

/* ---------- 元素访问 ---------- */
static unsigned char *lp_first(unsigned char *lp) {
    return (lp[LP_HDR_SIZE] == LP_EOF) ? NULL : lp + LP_HDR_SIZE;
}
static unsigned char *lp_eof(unsigned char *lp) { return lp + lp_total(lp) - 1; }

/* 返回元素总长 (编码+数据+backlen) */
static unsigned long lp_entry_size(unsigned char *p) {
    unsigned long enc, len;
    if ((p[0] & ENC_STR_MASK) == ENC_STR) {
        enc = 1; len = p[0] & 0x3F;                 /* 6bit 长度 */
    } else {
        enc = 1; len = 1;                            /* 7bit 整数: 值 1B */
    }
    unsigned long entrylen = enc + len;
    return entrylen + 1;                             /* +1B backlen (简化) */
}
static unsigned char *lp_next(unsigned char *lp, unsigned char *p) {
    p += lp_entry_size(p);
    return (p[0] == LP_EOF) ? NULL : p;
}
static unsigned char *lp_prev(unsigned char *lp, unsigned char *p) {
    if (p == lp + LP_HDR_SIZE) return NULL;
    p--;                                             /* 前驱 backlen */
    uint64_t prevlen = decode_backlen(p);
    p -= prevlen + 1 - 1;                            /* 前驱总大小 = prevlen+1 */
    return p;
}
static void lp_get(unsigned char *p, long long *lval, unsigned char **sval, unsigned long *slen) {
    if ((p[0] & ENC_STR_MASK) == ENC_STR) {
        *sval = p + 1; *slen = p[0] & 0x3F;
    } else {
        *lval = (p[0] & 0x7F); *sval = NULL;
    }
}

/* ---------- lpInsert 三合一 (对照 listpack.c:821-968, 简化) ---------- */
static unsigned char *lp_insert(unsigned char *lp, unsigned char *p, int where,
                                const unsigned char *str, unsigned long slen,
                                long long ival, int is_int) {
    int delete = (str == NULL && !is_int);
    if (delete) where = 2;                          /* LP_REPLACE */
    if (where == 1) { p = p + lp_entry_size(p); where = 0; }   /* AFTER → BEFORE */
    unsigned long poff = p - lp;

    /* 编码新元素 */
    unsigned char encbuf[16];
    unsigned long enclen;
    if (delete) {
        enclen = 0;
    } else if (is_int) {
        encbuf[0] = ENC_INT | (ival & 0x7F);        /* 7bit 整数 */
        enclen = 2;                                 /* 编码+值 */
    } else {
        encbuf[0] = ENC_STR | (slen & 0x3F);        /* 6bit 字符串 */
        memcpy(encbuf + 1, str, slen);
        enclen = 1 + slen;
    }
    unsigned long backlen_size = delete ? 0 : 1;    /* 简化 1B */
    unsigned long replaced_len = 0;
    if (where == 2) {
        replaced_len = lp_entry_size(p);
        if (delete) { /* 删除: 移除整个元素 */ }
    }
    if (where == 2 && delete) {
        /* 删除元素: memmove 覆盖 */
        unsigned long move = lp_total(lp) - poff - replaced_len;
        memmove(lp + poff, lp + poff + replaced_len, move);
        lp_set_total(lp, lp_total(lp) - replaced_len);
        lp_set_numele(lp, lp_numele(lp) - 1);
        return lp;
    }
    /* 插入 (BEFORE/REPLACE 且非删除) */
    unsigned long old_total = lp_total(lp);
    unsigned long new_total = old_total + enclen + backlen_size - (where == 2 ? replaced_len : 0);
    /* 扩先 realloc 后 memmove / 缩先 memmove 后 realloc (真实 L893-914) */
    unsigned char *dst;
    if (new_total > old_total) {
        lp = realloc(lp, new_total + 1);
        dst = lp + poff;
        if (where == 0) {
            memmove(dst + enclen + backlen_size, dst, old_total - poff);
        } else {
            memmove(dst + enclen + backlen_size, dst + replaced_len,
                    old_total - poff - replaced_len);
        }
    } else {
        dst = lp + poff;
        if (where == 0) {
            memmove(dst + enclen + backlen_size, dst, old_total - poff);
        } else {
            memmove(dst + enclen + backlen_size, dst + replaced_len,
                    old_total - poff - replaced_len);
        }
        lp = realloc(lp, new_total + 1);
        dst = lp + poff;                     /* 真实 L913: realloc 后更新 dst */
    }
    memcpy(dst, encbuf, enclen);
    encode_backlen(dst + enclen, enclen);           /* backlen = 自身长度 */
    lp_set_total(lp, new_total);
    if (where != 2) lp_set_numele(lp, lp_numele(lp) + 1);
    return lp;
}

static unsigned char *lp_append_str(unsigned char *lp, const char *s) {
    return lp_insert(lp, lp_eof(lp), 0, (unsigned char*)s, strlen(s), 0, 0);
}
static unsigned char *lp_append_int(unsigned char *lp, long long v) {
    return lp_insert(lp, lp_eof(lp), 0, NULL, 0, v, 1);
}

/* ---------- 验证 ---------- */
static int fail = 0;
#define CHECK(cond, msg) do { if (!(cond)) { printf("FAIL: %s\n", msg); fail++; } else printf("PASS: %s\n", msg); } while (0)

int main(void) {
    printf("[1] 布局与编码: 6B 头 + entry + EOF\n");
    unsigned char *lp = malloc(LP_HDR_SIZE + 1);
    lp_set_total(lp, LP_HDR_SIZE + 1);
    lp_set_numele(lp, 0);
    lp[LP_HDR_SIZE] = LP_EOF;
    CHECK(lp_total(lp) == 7, "初始 total=7 (6B 头+EOF)");
    lp = lp_append_int(lp, 5);
    CHECK(lp_numele(lp) == 1, "append int: numele=1");
    lp = lp_append_str(lp, "hello");
    CHECK(lp_numele(lp) == 2, "append str: numele=2");
    CHECK(lp_total(lp) == 7 + 3 + 7, "total=7+int(2+1B backlen)+str(1+5+1) = 17");

    printf("\n[2] 双向遍历: lpNext/lpPrev O(1)\n");
    unsigned char *e = lp_first(lp);
    long long lv; unsigned char *sv; unsigned long sl;
    lp_get(e, &lv, &sv, &sl);
    CHECK(sv == NULL && lv == 5, "第一个元素: 整数 5");
    e = lp_next(lp, e);
    lp_get(e, &lv, &sv, &sl);
    CHECK(sv != NULL && sl == 5 && memcmp(sv, "hello", 5) == 0, "第二个元素: 字符串 hello");
    e = lp_prev(lp, e);
    lp_get(e, &lv, &sv, &sl);
    CHECK(sv == NULL && lv == 5, "lpPrev 跳回: 整数 5 (O(1) 无需扫描)");
    CHECK(lp_prev(lp, e) == NULL, "再往前 = NULL (首元素)");

    printf("\n[3] 无级联: 头部插入变长元素, 后驱遍历不受影响\n");
    /* 头部插入一个长字符串 (改变布局) — ziplist 会级联传播, listpack 不会 */
    lp = lp_insert(lp, lp_first(lp), 0, (unsigned char*)"a-long-string", 13, 0, 0);
    CHECK(lp_numele(lp) == 3, "头部插入: numele=3");
    e = lp_first(lp);
    lp_get(e, &lv, &sv, &sl);
    CHECK(sv != NULL && sl == 13, "头部元素: 长字符串 (变长插入)");
    e = lp_next(lp, e); lp_get(e, &lv, &sv, &sl);
    CHECK(sv == NULL && lv == 5, "后驱 1: 整数 5 仍正确 (无级联)");
    e = lp_next(lp, e); lp_get(e, &lv, &sv, &sl);
    CHECK(sv != NULL && sl == 5 && memcmp(sv, "hello", 5) == 0, "后驱 2: hello 仍正确");
    /* 尾部反推验证全部 backlen 一致 */
    e = lp_eof(lp);
    unsigned char *tail = lp_prev(lp, e);
    lp_get(tail, &lv, &sv, &sl);
    CHECK(sv != NULL && sl == 5 && memcmp(sv, "hello", 5) == 0, "lpPrev(EOF) → 末元素 hello");

    printf("\n[4] 替换与删除 (三合一)\n");
    lp = lp_insert(lp, lp_first(lp), 2, (unsigned char*)"XY", 2, 0, 0);  /* REPLACE 头部 */
    CHECK(lp_numele(lp) == 3, "替换不改变 numele");
    e = lp_first(lp); lp_get(e, &lv, &sv, &sl);
    CHECK(sv != NULL && sl == 2 && memcmp(sv, "XY", 2) == 0, "头部被替换为 XY");
    lp = lp_insert(lp, lp_first(lp), 2, NULL, 0, 0, 0);                  /* 删除头部 */
    CHECK(lp_numele(lp) == 2, "删除头部: numele=2");
    e = lp_first(lp); lp_get(e, &lv, &sv, &sl);
    CHECK(sv == NULL && lv == 5, "删除后头部 = 整数 5");

    printf("\n结果: %s\n", fail ? "FAIL" : "ALL PASS");
    return fail ? 1 : 0;
}
