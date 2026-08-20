# 闭环笔记 q2: readQueryFromClient — 读取路径与分配策略

## 假设
读路径 = postpone 检查 → readlen 决策 (16KB 基准/大参数精确读) → 分配 (NonGreedy/Greedy) → 读 → 上限检查 → 解析。

## 验证过程
- readQueryFromClient (networking.c:2655-2764):
  - postponeClientRead (L2662, R-2 io threads) → 延期返回
  - **readlen 决策** (L2667-2688):
    - 默认 readlen = PROTO_IOBUF_LEN (16KB, server.h:164)
    - 大参数场景 (L2674-2675): MULTIBULK && multibulklen && bulklen ≥ PROTO_MBULK_BIG_ARG (32KB) → readlen = 剩余字节 (L2677-2682) — 一次读全, 免零拷贝复制 (R-4 连接)
    - MASTER 客户端 (L2686-2687): readlen < 16KB 时回升 16KB (#9100)
  - **分配策略** (L2690-2707):
    - 非 MASTER + (大参数 || alloc < 16KB) → **sdsMakeRoomForNonGreedy** (L2698, 按需防恶意膨胀, R-4)
    - 否则 **sdsMakeRoomFor (Greedy 2×)** (L2703) + readlen = sdsavail (L2706, 一次多读省 read 调用)
  - connRead (L2708) → -1 连接错误 / 0 关闭 → freeClientAsync (L2714,2723)
  - sdsIncrLen (L2727) → lastinteraction (L2731) → 统计 (L2732-2737)
  - **querybuf 上限** (L2739-2755): mstate.argv_len_sums + querybuf > client_max_querybuf_len → 关连接 (L2752); **未认证时 >1MB 同样关** (L2745)
  - processInputBuffer (L2759)

## 代码类型
Mechanism (读取路径)

## 跨域关联
- R-2 (postponeClientRead) / R-4 (sdsMakeRoomFor 族) / R-20 (maxclients)

## 结论
读路径的核心权衡 = **"多读省系统调用" vs "精确读省复制"**: 普通场景 Greedy 读满 avail (省 read), 大参数场景精确读剩余 (让 querybuf 恰好装下 bulk → 零拷贝)。未认证 1MB 上限是安全面 (防未认证洪水)。
源码位置: networking.c:2655-2764; server.h:164-167
