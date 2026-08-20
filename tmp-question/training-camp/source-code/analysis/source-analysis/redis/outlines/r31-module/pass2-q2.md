# 闭环笔记 q2: 类型系统 — 64 位类型 ID + 全序列化契约

## 假设
模块类型 = 64 位 ID (名字+版本) + 方法表 (RDB/AOF/内存/复制全契约)。

## 验证过程
- **类型 ID 编码** (moduleTypeEncodeId L6654-6672): **9 字符名** (ModuleTypeNameCharSet 64 符号表, 每字符 6bit) + **10bit encver** → 64 位; 名字非 9 字符或 encver 越界 (0-1023) → 0
- **注册** (RM_CreateDataType L6931): onload 限制 (仅加载期) + ID 冲突检查 (moduleTypeLookupModuleByName)
- **方法表 v1-v5** (L6941-6990): rdb_load/rdb_save/aof_rewrite/mem_usage/digest/free + v2 aux_load/aux_save + v3 free_effort/unlink/copy/defrag + v4 二代 (mem_usage2 等) + v5 aux_save2 — 按 version 字段条件拷贝
- **RDB 序列化** (RDB_MODULE_2, R-8 交叉): moduleTypeSaveData/rdbSaveObjectType — 类型 ID + 数据由模块 rdb_save 回调写入
- **AOF rewrite**: aof_rewrite 回调 — 模块键的 AOF 重建
- **内存记账**: mem_usage 回调 → INFO memory (模块键内存可见)
- **复制/淘汰集成**: digest (DEBUG DIGEST) / free_effort (lazyfree 判定) / unlink (R-21) / copy (COPY 命令) / defrag (R-18 moduleDefragValue)
- **aux 面**: aux_load/aux_save — 模块级附加数据 (RDB 全局段)

## 代码类型
Implementation (扩展类型系统)

## 跨域关联
- R-8 (persistence): RDB MODULE_2 / AOF
- R-18 (defrag): moduleDefragValue/moduleLateDefrag
- R-21 (db): unlink/copy/free_effort

## 结论
模块类型 = 64 位 ID (9×6bit 名 + 10bit 版本) + 版本化方法表 — Redis 数据结构的完整扩展面: 持久化 (RDB/AOF)、内存 (mem_usage)、复制 (digest)、淘汰 (free_effort)、碎片 (defrag) 全部由模块回调契约化。
源码位置: module.c:6654-6672,6931-6990
