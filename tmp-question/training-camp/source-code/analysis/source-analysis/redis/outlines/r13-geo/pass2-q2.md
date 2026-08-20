# 闭环笔记 q2: GEOADD — ZADD 包装 (零新类型)

## 假设
GEOADD 内部构造 ZADD 参数向量, score = 52bit 编码。

## 验证过程
- geoaddCommand (geo.c:445-503):
  - 选项解析 NX/XX/CH (L451-458, CH 注释 "Handle in zaddCommand" L455) — 直接透传
  - 参数校验: (argc-longidx)%3 或 NX+XX 同现 → syntaxerr (L460-464)
  - **构造 zadd 向量**: argv[0]="zadd" (L470), 前段选项共享 (L471-474), 每元素 encode+score (L479-498)
  - `geohashEncodeWGS84(xy, GEO_STEP_MAX)` + `geohashAlign52Bits` (L491-492) → score 字符串
  - **replaceClientCommandVector(c,argc,argv) + zaddCommand(c)** (L501-502) — 命令重写, 复用 R-6 全部语义 (NX/XX/CH/编码选择/传播)
- GEOADD 无独立写入路径 — 建键/编码/传播全走 ZADD (对照 R-12 PFADD 自带存储逻辑)
- 命令标志 CMD_WRITE|CMD_DENYOOM (commands.def:11019) — 与 ZADD 一致

## 代码类型
Command (包装/委托)

## 跨域关联
- R-6 (已交付): zaddCommand/zset 双结构 — GEO 完全复用
- R-12: 对照 — PFADD 自己管理 sds 存储, GEOADD 委托 ZADD; 两个"特殊字符串/zset"的不同构造路径
- R-1: 字符串承载 (score 对象)

## 结论
GEOADD = 纯命令包装: 编码→score→ZADD, 零新数据结构。GEO 的正确性是 zset + 编码函数联合保证的。
源码位置: geo.c:445-503
