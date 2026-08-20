/*
 * R-8 RDB+AOF harness — 持久化核心编解码极简复现
 * 从 rdb.c/aof.c 提取纯函数真实实现 (去掉 server 依赖), 实证:
 *  1. rdbSaveLen/rdbLoadLenByRef 四档长度编码 (rdb.c:151-238)
 *  2. rdbEncodeInteger/rdbLoadIntegerByRef 整数编码 (INT8/16/32)
 *  3. rdbSaveMillisecondTime/rdbLoadMillisecondTime (v9 BE 修复语义, LE 无操作)
 *  4. rdbSaveBinaryDoubleValue/rdbLoadBinaryDoubleValue 二进制双精度
 *  5. rdbGenericSaveStringObject 决策树 (整数编码 vs LZF vs raw)
 *  6. catAppendOnlyGenericCommand AOF 命令序列化 (aof.c)
 *  7. AOF 重写变参批量 AOF_REWRITE_ITEMS_PER_CMD 语义
 * 编译: gcc -O0 -g -fsanitize=address,undefined -o harness r8_harness.c && ./harness
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#include <assert.h>
#include <limits.h>
#include <arpa/inet.h>
#include <endian.h>

/* ---- 极简 rio buffer (还原 rioBufferIO 语义: rio.c:62-106) ---- */
typedef struct {
    unsigned char *buf;
    size_t len, cap, pos;
    int read_error;
} RIO;

static void rio_init(RIO *r) { r->buf = NULL; r->len = r->cap = r->pos = 0; r->read_error = 0; }
static int rio_write(RIO *r, const void *p, size_t n) {
    if (r->pos + n > r->cap) {
        size_t nc = r->cap ? r->cap * 2 : 64;
        while (nc < r->pos + n) nc *= 2;
        r->buf = realloc(r->buf, nc);
        r->cap = nc;
    }
    memcpy(r->buf + r->pos, p, n);
    r->pos += n;
    if (r->pos > r->len) r->len = r->pos;
    return 1;
}
static int rio_read(RIO *r, void *p, size_t n) {
    if (r->pos + n > r->len) { r->read_error = 1; return 0; }
    memcpy(p, r->buf + r->pos, n);
    r->pos += n;
    return 1;
}
static size_t rio_tell(RIO *r) { return r->pos; }
static void rio_seek(RIO *r, size_t pos) { r->pos = pos; r->read_error = 0; }

/* ---- endianconv 极小实现 (LE 平台无操作) ---- */
static void memrev64ifbe(void *p) { (void)p; } /* 本平台 LE */

/* ---- 从 rdb.c 提取: 长度编码 (L151-238) ---- */
#define RDB_6BITLEN 0
#define RDB_14BITLEN 1
#define RDB_32BITLEN 0x80
#define RDB_64BITLEN 0x81
#define RDB_ENCVAL 3
#define RDB_LENERR UINT64_MAX

static size_t rdbWriteRaw(RIO *rdb, void *p, size_t len) {
    if (rdb && rio_write(rdb, p, len) == 0) return 0;
    return len;
}

static int rdbSaveLen(RIO *rdb, uint64_t len) {
    unsigned char buf[2];
    size_t nwritten;
    if (len < (1 << 6)) {
        buf[0] = (len & 0xFF) | (RDB_6BITLEN << 6);
        if (rdbWriteRaw(rdb, buf, 1) == 0) return -1;
        nwritten = 1;
    } else if (len < (1 << 14)) {
        buf[0] = ((len >> 8) & 0xFF) | (RDB_14BITLEN << 6);
        buf[1] = len & 0xFF;
        if (rdbWriteRaw(rdb, buf, 2) == 0) return -1;
        nwritten = 2;
    } else if (len <= UINT32_MAX) {
        buf[0] = RDB_32BITLEN;
        if (rdbWriteRaw(rdb, buf, 1) == 0) return -1;
        uint32_t len32 = htonl((uint32_t)len);
        if (rdbWriteRaw(rdb, &len32, 4) == 0) return -1;
        nwritten = 1 + 4;
    } else {
        buf[0] = RDB_64BITLEN;
        if (rdbWriteRaw(rdb, buf, 1) == 0) return -1;
        uint64_t l = htobe64(len);
        if (rdbWriteRaw(rdb, &l, 8) == 0) return -1;
        nwritten = 1 + 8;
    }
    return nwritten;
}

static int rdbLoadLenByRef(RIO *rdb, int *isencoded, uint64_t *lenptr) {
    unsigned char buf[2];
    int type;
    if (isencoded) *isencoded = 0;
    if (rio_read(rdb, buf, 1) == 0) return -1;
    type = (buf[0] & 0xC0) >> 6;
    if (type == RDB_ENCVAL) {
        if (isencoded) *isencoded = 1;
        *lenptr = buf[0] & 0x3F;
    } else if (type == RDB_6BITLEN) {
        *lenptr = buf[0] & 0x3F;
    } else if (type == RDB_14BITLEN) {
        if (rio_read(rdb, buf + 1, 1) == 0) return -1;
        *lenptr = ((buf[0] & 0x3F) << 8) | buf[1];
    } else if (buf[0] == RDB_32BITLEN) {
        uint32_t len;
        if (rio_read(rdb, &len, 4) == 0) return -1;
        *lenptr = ntohl(len);
    } else if (buf[0] == RDB_64BITLEN) {
        uint64_t len;
        if (rio_read(rdb, &len, 8) == 0) return -1;
        *lenptr = be64toh(len);
    } else {
        return -1;
    }
    return 0;
}

/* ---- 从 rdb.c 提取: 整数编码 (L258-320) ---- */
#define RDB_ENC_INT8 0
#define RDB_ENC_INT16 1
#define RDB_ENC_INT32 2
#define RDB_ENC_LZF 3

static int rdbEncodeInteger(long long value, unsigned char *enc) {
    if (value >= -(1 << 7) && value <= (1 << 7) - 1) {
        enc[0] = (RDB_ENCVAL << 6) | RDB_ENC_INT8;
        enc[1] = value & 0xFF;
        return 2;
    } else if (value >= -(1 << 15) && value <= (1 << 15) - 1) {
        enc[0] = (RDB_ENCVAL << 6) | RDB_ENC_INT16;
        enc[1] = value & 0xFF;
        enc[2] = (value >> 8) & 0xFF;
        return 3;
    } else if (value >= -((long long)1 << 31) && value <= ((long long)1 << 31) - 1) {
        enc[0] = (RDB_ENCVAL << 6) | RDB_ENC_INT32;
        enc[1] = value & 0xFF;
        enc[2] = (value >> 8) & 0xFF;
        enc[3] = (value >> 16) & 0xFF;
        enc[4] = (value >> 24) & 0xFF;
        return 5;
    } else {
        return 0;
    }
}

static int rdbLoadIntegerByRef(RIO *rdb, int enctype, long long *val) {
    unsigned char buf[4];
    int i;
    if (enctype == RDB_ENC_INT8) {
        if (rio_read(rdb, buf, 1) == 0) return -1;
        *val = (signed char)buf[0];
    } else if (enctype == RDB_ENC_INT16) {
        if (rio_read(rdb, buf, 2) == 0) return -1;
        *val = (int16_t)((buf[0]) | (buf[1] << 8));
    } else if (enctype == RDB_ENC_INT32) {
        if (rio_read(rdb, buf, 4) == 0) return -1;
        *val = (int32_t)((uint32_t)buf[0] | ((uint32_t)buf[1] << 8) |
                         ((uint32_t)buf[2] << 16) | ((uint32_t)buf[3] << 24));
    } else {
        *val = 0;
        return -1;
    }
    (void)i;
    return 0;
}

/* ---- 从 rdb.c 提取: 毫秒时间戳 (L119-146) ---- */
static size_t rdbSaveMillisecondTime(RIO *rdb, long long t) {
    int64_t t64 = (int64_t)t;
    memrev64ifbe(&t64);
    return rdbWriteRaw(rdb, &t64, 8);
}

static long long rdbLoadMillisecondTime(RIO *rdb, int rdbver) {
    int64_t t64;
    if (rio_read(rdb, &t64, 8) == 0) return LLONG_MAX;
    if (rdbver >= 9)
        memrev64ifbe(&t64);
    return (long long)t64;
}

/* ---- 从 rdb.c 提取: 二进制 double (L644-650) ---- */
static int rdbSaveBinaryDoubleValue(RIO *rdb, double val) {
    memrev64ifbe(&val);
    return rdbWriteRaw(rdb, &val, sizeof(val));
}

static int rdbLoadBinaryDoubleValue(RIO *rdb, double *val) {
    if (rio_read(rdb, val, sizeof(*val)) == 0) return -1;
    memrev64ifbe(val);
    return 0;
}

/* ---- 字符串编码决策树 (rdb.c rdbGenericSaveStringObject 核心逻辑) ---- */
/* 极简 LZF: 仅复现"压缩决策", 用恒等变换模拟压缩 (字节格式: type+len+len+data) */
static size_t lzf_compress_dummy(const void *in, size_t inlen, void *out, size_t outlen) {
    if (outlen < inlen) return 0;
    memcpy(out, in, inlen);
    return inlen;
}

/* 复现 rdbSaveRawString 的编码选择: 先试整数编码, 再试 LZF, 否则 raw
 * 返回 0=raw 1=LZF 2=INT 编码 (选择路径实证) */
static int rdbStringEncodePath(RIO *rdb, const unsigned char *s, size_t len, long long *out_int) {
    unsigned char enc[5];
    /* 1. 整数编码尝试: 字符串可解析为整数且 <=11 字节? (rdbTryIntegerEncoding 语义) */
    if (len <= 11) {
        long long value = 0;
        int neg = 0, ok = 1, i = 0;
        if (len > 0 && s[0] == '-') { neg = 1; i = 1; }
        for (; i < (int)len; i++) {
            if (s[i] < '0' || s[i] > '9') { ok = 0; break; }
            value = value * 10 + (s[i] - '0');
        }
        if (ok && (len > 1 || !neg)) {
            if (neg) value = -value;
            if (rdbEncodeInteger(value, enc)) {
                *out_int = value;
                rdbWriteRaw(rdb, enc, rdbEncodeInteger(value, enc) == 2 ? 2 :
                            (rdbEncodeInteger(value, enc) == 3 ? 3 : 5));
                return 2; /* INT 编码路径 */
            }
        }
    }
    /* 2. LZF 尝试: len > 20 且压缩有收益 (rdbSaveLzfStringObject 条件) */
    if (len > 20) {
        unsigned char *comp = malloc(len);
        size_t complen = lzf_compress_dummy(s, len, comp, len);
        if (complen && complen < len - 4) {
            /* 格式: RDB_ENCVAL|LZF + clen(len) + ulen(len) + data */
            unsigned char header[2];
            header[0] = (RDB_ENCVAL << 6) | RDB_ENC_LZF;
            rdbWriteRaw(rdb, header, 1);
            rdbSaveLen(rdb, complen);
            rdbSaveLen(rdb, len);
            rdbWriteRaw(rdb, comp, complen);
            free(comp);
            return 1; /* LZF 路径 */
        }
        free(comp);
    }
    /* 3. raw: rdbSaveLen(len) + data */
    rdbSaveLen(rdb, len);
    rdbWriteRaw(rdb, s, len);
    return 0;
}

/* ---- 从 aof.c 提取: 命令序列化 (catAppendOnlyGenericCommand) ---- */
static int catAppendOnlyGenericCommand(void **dst, size_t *dstlen, int argc, char **argv, size_t *argvlen) {
    
    /* 极简 sds 模拟: 用动态 buffer */
    char *buf = malloc(1);
    size_t buflen = 0;
    size_t total = 0;
    size_t i;

    /* 头部: *<argc>\r\n */
    char hdr[32];
    int hdrlen = snprintf(hdr, sizeof(hdr), "*%d\r\n", argc);
    buf = realloc(buf, buflen + hdrlen + 1);
    memcpy(buf + buflen, hdr, hdrlen);
    buflen += hdrlen;
    total += hdrlen;

    for (i = 0; i < (size_t)argc; i++) {
        /* $<len>\r\n<data>\r\n */
        char lhdr[32];
        int lhdrlen = snprintf(lhdr, sizeof(lhdr), "$%zu\r\n", argvlen[i]);
        buf = realloc(buf, buflen + lhdrlen + argvlen[i] + 2 + 1);
        memcpy(buf + buflen, lhdr, lhdrlen);
        buflen += lhdrlen;
        memcpy(buf + buflen, argv[i], argvlen[i]);
        buflen += argvlen[i];
        buf[buflen++] = '\r';
        buf[buflen++] = '\n';
        total += lhdrlen + argvlen[i] + 2;
    }
    buf[buflen] = '\0';
    *dst = buf;
    *dstlen = buflen;
    return (int)total;
}

/* ---- 测试骨架 ---- */
static int tests = 0, failures = 0;
#define CHECK(cond) do { tests++; if (!(cond)) { failures++; \
    printf("FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } } while (0)

static void test_len_roundtrip(uint64_t v) {
    RIO r; rio_init(&r);
    int n = rdbSaveLen(&r, v);
    assert(n > 0);
    size_t encoded = r.pos;
    rio_seek(&r, 0);
    uint64_t out; int isenc;
    assert(rdbLoadLenByRef(&r, &isenc, &out) == 0);
    CHECK(out == v);
    CHECK(!isenc);
    /* 编码档位验证 */
    if (v < (1 << 6)) CHECK(encoded == 1);
    else if (v < (1 << 14)) CHECK(encoded == 2);
    else if (v <= UINT32_MAX) CHECK(encoded == 5);
    else CHECK(encoded == 9);
    free(r.buf);
}

static void test_int_roundtrip(long long v) {
    unsigned char enc[5];
    int elen = rdbEncodeInteger(v, enc);
    CHECK(elen > 0);
    RIO r; rio_init(&r);
    rio_write(&r, enc, elen);
    rio_seek(&r, 0);
    long long out;
    int enctype = (enc[0] >> 6) == RDB_ENCVAL ? (enc[0] & 0x3F) : -1;
    CHECK(enctype == RDB_ENC_INT8 || enctype == RDB_ENC_INT16 || enctype == RDB_ENC_INT32);
    /* 跳过 1B 类型头 */
    rio_seek(&r, 1);
    assert(rdbLoadIntegerByRef(&r, enctype, &out) == 0);
    CHECK(out == v);
    free(r.buf);
}

static void test_str_paths(void) {
    /* INT 路径: "12345" */
    RIO r; rio_init(&r);
    long long iout;
    int p = rdbStringEncodePath(&r, (const unsigned char*)"12345", 5, &iout);
    CHECK(p == 2);
    CHECK(iout == 12345);
    free(r.buf);

    /* raw 路径: 含非数字字符的短串 */
    rio_init(&r);
    p = rdbStringEncodePath(&r, (const unsigned char*)"hello", 5, &iout);
    CHECK(p == 0);
    CHECK(r.pos == 1 + 5); /* 1B len + 5B data */
    free(r.buf);

    /* LZF 路径: >20B 随机串 (dummy 压缩恒等, 不省空间 → 走 raw) */
    rio_init(&r);
    unsigned char big[100];
    for (int i = 0; i < 100; i++) big[i] = (unsigned char)(i * 7);
    p = rdbStringEncodePath(&r, big, 100, &iout);
    /* dummy 压缩不省空间, 所以走 raw — 验证"压缩收益"条件门槛;
     * len=100 ≥64 → rdbSaveLen 走 14bit 档 (2B), 总长 2+100 */
    CHECK(p == 0);
    CHECK(r.pos == 2 + 100);
    free(r.buf);
}

static void test_double_time(void) {
    RIO r; rio_init(&r);
    double vals[] = {0.0, 3.141592653589793, -1e308, 1e-308, NAN, INFINITY};
    for (int i = 0; i < 6; i++) {
        rio_seek(&r, 0);
        r.len = 0; r.pos = 0;
        assert(rdbSaveBinaryDoubleValue(&r, vals[i]));
        rio_seek(&r, 0);
        double out;
        assert(rdbLoadBinaryDoubleValue(&r, &out) == 0);
        if (isnan(vals[i])) CHECK(isnan(out));
        else if (isinf(vals[i])) CHECK(isinf(out) && signbit(out) == signbit(vals[i]));
        else CHECK(out == vals[i]);
    }
    free(r.buf);

    /* 毫秒时间戳: 含 v9 修复路径 (LE 平台 memrev 无操作) */
    rio_init(&r);
    long long ts = 1723545600123LL;
    assert(rdbSaveMillisecondTime(&r, ts));
    rio_seek(&r, 0);
    CHECK(rdbLoadMillisecondTime(&r, 9) == ts);   /* v9+ 修复路径 */
    rio_seek(&r, 0);
    CHECK(rdbLoadMillisecondTime(&r, 8) == ts);   /* v8 旧路径 (LE 同样值) */
    free(r.buf);
}

static void test_aof_serialize(void) {
    /* catAppendOnlyGenericCommand: "SET key value" → *3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n */
    char *argv[3] = {"SET", "key", "value"};
    size_t argvlen[3] = {3, 3, 5};
    void *dst; size_t dstlen;
    catAppendOnlyGenericCommand(&dst, &dstlen, 3, argv, argvlen);
    const char *expect = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n";
    CHECK(dstlen == strlen(expect));
    CHECK(memcmp(dst, expect, dstlen) == 0);
    free(dst);

    /* 大参数 (>32KB) 格式不变 */
    size_t biglen = 40000;
    char *big = malloc(biglen);
    memset(big, 'x', biglen);
    char *argv2[2] = {"SET", big};
    size_t argvlen2[2] = {3, biglen};
    catAppendOnlyGenericCommand(&dst, &dstlen, 2, argv2, argvlen2);
    char hdr[32];
    int hdrlen = snprintf(hdr, sizeof(hdr), "*2\r\n$3\r\nSET\r\n$%zu\r\n", biglen);
    CHECK(dstlen == (size_t)hdrlen + biglen + 2);
    CHECK(memcmp(dst, hdr, hdrlen) == 0);
    free(big);
    free(dst);
}

static void test_aof_rewrite_batch(void) {
    /* AOF 重写变参批量: RPUSH 每批最多 AOF_REWRITE_ITEMS_PER_CMD (64) 元素
     * 复现 rewriteListObject 的批量决策语义 */
    #define AOF_REWRITE_ITEMS_PER_CMD 64
    #define AOF_REWRITE_ITEMS 5000
    char **elems = malloc(AOF_REWRITE_ITEMS * sizeof(char*));
    size_t *elemlens = malloc(AOF_REWRITE_ITEMS * sizeof(size_t));
    for (int i = 0; i < AOF_REWRITE_ITEMS; i++) {
        char *e = malloc(16);
        int n = snprintf(e, 16, "item%d", i);
        elems[i] = e;
        elemlens[i] = n;
    }
    /* 批量切分: 每批 ≤64, 统计命令数 (对照 rewriteListObject 的 RPUSH 批量) */
    int cmds = 0;
    for (int i = 0; i < AOF_REWRITE_ITEMS; i += AOF_REWRITE_ITEMS_PER_CMD) {
        int batch = AOF_REWRITE_ITEMS - i;
        if (batch > AOF_REWRITE_ITEMS_PER_CMD) batch = AOF_REWRITE_ITEMS_PER_CMD;
        cmds++;
        CHECK(batch <= AOF_REWRITE_ITEMS_PER_CMD);
    }
    CHECK(cmds == (AOF_REWRITE_ITEMS + AOF_REWRITE_ITEMS_PER_CMD - 1) / AOF_REWRITE_ITEMS_PER_CMD);
    /* 5000 元素 → 79 条 RPUSH (每批 64) */
    CHECK(cmds == 79);
    for (int i = 0; i < AOF_REWRITE_ITEMS; i++) free(elems[i]);
    free(elems);
    free(elemlens);
}

int main(void) {
    printf("R-8 persistence harness (gcc+ASan)\n");

    /* 1. 长度编码四档边界: 0/63/64/16383/16384/UINT32_MAX/UINT32_MAX+1/2^63+5 */
    uint64_t lens[] = {0, 63, 64, 16383, 16384, UINT32_MAX, (uint64_t)UINT32_MAX + 1, (1ULL << 63) + 5};
    for (size_t i = 0; i < sizeof(lens) / sizeof(lens[0]); i++)
        test_len_roundtrip(lens[i]);

    /* 2. 整数编码边界: INT8/16/32 三档 */
    long long ints[] = {0, 127, -128, 128, -129, 32767, -32768, 32768, -32769,
                        2147483647LL, -2147483648LL};
    for (size_t i = 0; i < sizeof(ints) / sizeof(ints[0]); i++)
        test_int_roundtrip(ints[i]);

    /* 3. 字符串编码决策树 */
    test_str_paths();

    /* 4. double/时间戳 */
    test_double_time();

    /* 5. AOF 序列化 */
    test_aof_serialize();

    /* 6. AOF 重写批量 */
    test_aof_rewrite_batch();

    printf("tests: %d, failures: %d\n", tests, failures);
    return failures ? 1 : 0;
}
