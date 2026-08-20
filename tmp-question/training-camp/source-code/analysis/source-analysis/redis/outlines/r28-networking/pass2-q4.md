# 闭环笔记 q4: processMultibulkBuffer — RESP 多行解析

## 假设
RESP 多行 = * 计数行 + N 个 $ 长度行 + 数据; argv 动态增长; 大参数零拷贝借用 querybuf。

## 验证过程
- processMultibulkBuffer (networking.c:2292-2452):
  - **首行 * 计数** (L2297-2340): 找 \r\n (L2302); 超 PROTO_INLINE_MAX_SIZE (64KB) 报错 (L2304); string2ll 解析 (L2318); **ll > INT_MAX 报错** (L2319); **未认证且 >10 报错** (L2323)
  - argv 初始化 (L2335-2339): **argv_len = min(multibulklen, 1024)** 起步; argv_len_sum=0
  - **循环解析每参数** (L2343-2445):
    - $ 长度行 (L2345-2407): 找 \r\n; **非 $ 首字节报错** (L2361-2367); string2ll; **ll < 0 或 > proto_max_bulk_len 报错** (L2370-2374, master 豁免); **未认证且 >16384 报错** (L2375)
    - **大参数预对齐** (L2382-2406): bulklen ≥ 32KB 且剩余 ≤ ll+2 → `sdsrange(querybuf, qb_pos, -1)` (L2397) + NonGreedy 预分配 (L2401) — 让 bulk 独占 querybuf 起始 (零拷贝准备)
    - **数据读取** (L2411-2444): 不足 bulklen+2 → break 等更多数据
      - **零拷贝路径** (L2424-2435): 非 master && qb_pos==0 && bulklen ≥ 32KB && querybuf 恰好 = bulklen+2 → **createObject(OBJ_STRING, querybuf) 直接借用** (L2429) + sdsIncrLen(-2) 去 CRLF (L2431) + 新 querybuf (L2434)
      - 普通路径 (L2437-2440): createStringObject 复制 + qb_pos 前进
    - **argv 增长** (L2416-2419): argc ≥ argv_len → `argv_len = min(argv_len*2, INT_MAX)` (上限 INT_MAX)
  - 完成条件 (L2447-2451): multibulklen==0 → C_OK (整命令就绪); 否则 C_ERR 等更多
- 协议错误 (setProtocolError L2252): 客户端标记 CLIENT_PROTOCOL_ERROR → 断开

## 代码类型
Mechanism (RESP 解析)

## 跨域关联
- R-4 (sdsrange/sdsIncrLen/sdsMakeRoomForNonGreedy) / R-1 (createObject) / R-2 (io 线程) / R-32 (认证)

## 结论
RESP 解析 = 三行式状态机 (* → $ → data), 全部上限防护 (64KB 计数行/INT_MAX/proto_max_bulk_len/未认证 16384)。大参数零拷贝是核心优化: 精确读让 bulk 独占 querybuf 首部, argv 直接借用 sds 省一次复制 (32KB+ 才值得)。
源码位置: networking.c:2252,2292-2452
