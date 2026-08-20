# R-24 字符串命令 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | t_string.c 初版 — get/set/incr/append 家族 (版权 "2009-Present") |
| 2.6 | SETNX/SETEX/GETSET; SETRANGE/GETRANGE 引入 |
| 2.8/3.0 | INCRBYFLOAT (重写 SET KEEPTTL 传播); MSET/MSETNX |
| 4.0 | SET NX/XX/EX/PX 选项 (setGenericCommand 统一); tryObjectEncoding 链 |
| 6.0 | LCS 引入 (DP 表 + 防护); GETEX/GETDEL (读改写合一) |
| 6.2+ | SET GET/KEEPTTL 选项; parseExtendedStringArgumentsOrReply 统一解析 (SET/GETEX 共用) |
| 演进 | 标志从 4 个 (NX/XX/EX/PX) → 9 个 (加 KEEPTTL/GET/EXAT/PXAT/PERSIST) — 位域持续扩展 |

## 痕迹证据

- L33-47: setGenericCommand 头注释 — 支持的命令清单 (SET/SETEX/PSETEX/SETNX/GETSET)
- L87: "When expire is not NULL, we avoid deleting the TTL so it can be updated later instead of being deleted and then created again" — KEEPTTL 优化动机
- L367-368: GETEX "never propagated as is" — 传播重写设计
- L668-670: INCRBYFLOAT 传播注释 ("differences in float precision or formatting will not create differences in replicas")
- L780-783: LCS 表索引注释 (线性数组 LCS[j+(blen+1)*i])
- L24-25: checkStringLength 注释 ("uint64_t cast is there just to prevent undefined behavior on overflow")

## 推断标注

- "INCR 高频路径零分配" — 四条件原地更新是代码事实, "高频"占比是推断 (无统计)
- "GETRANGE 总是复制" — 代码事实 (addReplyBulkCBuffer 复制), "为什么不做切片共享"是推断
- "SET key 100 比 abc 省内存" — INT 编码是事实, 节省幅度 (16B robj 内嵌 vs sds 分配) 是推断量级
