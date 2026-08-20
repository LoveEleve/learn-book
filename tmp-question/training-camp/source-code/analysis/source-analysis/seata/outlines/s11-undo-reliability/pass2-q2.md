# 闭环笔记 q2: 子表面 — 80% 阈值拆分 + 主/子行

## 假设
超 max_allowed_packet 的 undo_log 拆分子行存储; 读取时拼接。

## 验证过程
- **拆分阈值** (MySQLUndoLogManager:136-143): **limit = maxAllowedPacket × 0.8** — maxAllowedPacket from context (默认 **1MB** — mysql 5.6, L137-143)
- **拆分写入** (L150-167): 超限 → **首片在主行 (branchId)** + 后续子片 UUID subId 子行 (SUB_SPLIT_KEY "," 拼接) → context 记录 **SUB_ID_KEY** (子 id 列表)
- **子行标记**: subRollbackCtx = BRANCH_ID_KEY + branchId — 子行关联主分支
- **读取拼接** (L83-120): getSubRollbackInfo — **SELECT WHERE branch_id IN (...) AND xid = ?** → 逐子行取 rollback_info 字节 → **主+子字节拼接** → 解压 (q1)
- **批删联动** (S-2): DELETE_SUB_UNDO_LOG_SQL (context = branchId:xxx) — 主/子行同步清理

## 代码类型
Implementation (子表拆分)

## 跨域关联
- S-2: SUB_ID_KEY 消费 (getRollbackInfo)
- S-4: flush 触发 (拆分发生在插入面)

## 结论
子表 = 80% 阈值拆分 (首片主行 + UUID 子行) + SUB_ID_KEY 索引 + 读取拼接 + 同步清理。
源码位置: MySQLUndoLogManager.java:83-120,133-172
