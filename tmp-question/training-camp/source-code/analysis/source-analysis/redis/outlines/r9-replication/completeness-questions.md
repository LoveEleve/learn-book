# R-9 复制 — completeness-questions (两篇)

## R-9a 全量同步 — 开发者视角

1. 握手状态机有哪些状态? 顺序?
2. PING 的目的是什么?
3. REPLCONF capa eof psync2 是什么?
4. PSYNC 的 offset 为什么 +1?
5. FULLRESYNC 为什么延迟回复?
6. 磁盘/无盘 RDB 传输差异?
7. 从库临时文件怎么命名?
8. 首 ACK 与命令流启动的关系?

## R-9a 全量同步 — 架构师视角

9. 状态机设计的兼容策略 (老主库忽略选项)?
10. 双 ID 裁决的意义 (PSYNC2)?
11. FULLRESYNC 携带 RDB 时刻 offset 的设计?
12. 无盘复制多从库管道扇出?
13. 空库交换 vs 磁盘加载的取舍?
14. 首 ACK 门控的必要性?
15. 全量期间从库服务行为 (stale-data)?
16. 级联复制的 RDB 流向?

## R-9a 全量同步 — 学生视角

17. 全量同步期间新写入的命令去哪了?
18. SYNC 和 PSYNC 的区别?
19. 从库加载 RDB 时客户端会怎样?
20. diskless 复制为什么延迟更低?

## R-9b 增量同步 — 开发者视角

1. repl_backlog 块链结构?
2. refcount 引用计数的角色?
3. 裁剪的条件是什么?
4. ACK 多久发一次? 内容?
5. GETACK 什么时候用?
6. replid2 什么时候生效?
7. cached_master 缓存什么?
8. 部分重同步的范围校验?

## R-9b 增量同步 — 架构师视角

9. 共享块链 vs 每从库独立缓冲的取舍?
10. 引用计数如何天然保护 backlog 不裁穿?
11. rax 索引 (64 块/索引) 的查询语义?
12. 增量裁剪 max_blocks=64 的意义?
13. ACK 偏移级确认 vs 命令级确认?
14. PSYNC2 单代历史的边界?
15. WAIT 命令的强一致语义?
16. 慢从库 (输出缓冲限制) 怎么处理?

## R-9b 增量同步 — 学生视角

17. 断线 5 秒重连为什么不用全量?
18. backlog 太小会怎样?
19. 从库重启后还能部分重同步吗?
20. 主库重启后从库会怎样?

# R-9 复制 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 | SYNC 全量复制 (主从雏形, replication.c 版权 2009-Present) |
| 2.8 | **PSYNC 部分重同步** — repl_backlog 引入 (L718-814 裁决逻辑) + REPLCONF 命令 |
| 3.0 | **无盘复制** diskless-sync (rdbPipeReadHandler L1487) |
| 4.0 | **PSYNC2** — replid/replid2 双 ID + shiftReplicationId (L1698); 混合持久化配合 |
| 5.0 | 级联复制完善 (replid 沿树传播) |
| 6.2 | **WAIT** 命令 (L3521) — 副本确认 |
| 7.0 | **共享 repl_buffer_blocks** (多从库零复制, L315-413 重构) + REPLCONF RDB-ONLY/RDB-FILTER-ONLY (L1220-1255) |
| 7.4 | **WAITAOF** + REPLCONF ACK fack (L1194-1198, L3555) |

## 痕迹证据

- L102-113: backlog offset 虚设语义注释 ("virtually the first byte... is the next byte")
- L346-353: 块大小自适应注释 (可裁与可填平衡)
- L245-261: 引用计数裁剪注释 ("implicitly makes backlog bigger than our setting, but makes the master accept partial resync as much as possible")
- L2718-2723: capa eof psync2 能力声明注释
- L2533-2562: +CONTINUE 新 replid 处理注释 (级联拓扑)
- L808-813: FULLRESYNC 延迟回复注释 (offset 语义权威依据)

## 推断标注

- "2.8 PSYNC" — 版本推断 (PSYNC 2.8 发布; 仓库浅克隆无法 git 验证)
- "3.0 无盘" — 版本推断
- "7.0 共享块链重构" — 版本推断 (repl_buffer_blocks 与 6.x 的 repl_backlog 环形单缓冲差异)
- "backlog 默认 1MB" — 需 grep 确认 repl-backlog-size 默认值 (config.c)
