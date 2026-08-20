# 闭环笔记 q7: 配置系统 — createXxxConfig 宏族 + 统一注册表

## 假设
config.c 用宏族 (Int/UInt/Long/ULong/Bool/String/Enum/SizeT/Memory/Special) 统一注册配置: 每项含类型/范围/默认值/验证/apply 回调 — 解析与 CONFIG SET 共用一套。

## 验证过程
- 宏族 (config.c:2244+): createIntConfig → embedCommonNumericalConfig + `.numeric_type = NUMERIC_TYPE_INT` + `.config.i = &(config_addr)` — **宏展开为 standardConfig 联合体项**
- 配置族统计: 53 Bool + 41 Int + 36 String + 20 Enum + 13 SizeT + 9 Special (config.c)
- 参数面: flags (MODIFIABLE/IMMUTABLE/DEBUG/HIDDEN) + lower/upper 范围 + is_valid 验证 + apply 回调 (如 port → updatePort, L3148)
- 生命周期: 启动 parse (配置文件/命令行) + **CONFIG SET 动态** — 同一 standardConfig 表
- CONFIG SET 失败回滚 (config.c:760-780): **restoreBackupConfig** — 备份旧值 → 设置 → apply → 失败恢复全部
- 模块配置: moduleConfigApplyConfig (L778) — 模块项同框架
- 样例 (多域引用): list-max-listpack-size (R-5), hash-max (R-19), set-max-intset (R-7), zset-max (R-6), databases=16 (L3147), io-threads (L3149)

## 代码类型
Interface (配置框架) — 宏 DSL

## 跨域关联
- 全部消费域 (R-5/6/7/19 阈值) → 配置来源
- R-20 (CONFIG 命令) → 动态面
- R-31 (模块配置) → 扩展面

## 结论
配置系统 = 宏 DSL 统一注册: 类型/范围/默认/验证/apply 五合一 — 启动解析与 CONFIG SET 共用; CONFIG SET 失败整体回滚 (restoreBackupConfig) 保一致性。这是"所有阈值域"的统一入口。
源码位置: config.c:2244+,3147-3149
