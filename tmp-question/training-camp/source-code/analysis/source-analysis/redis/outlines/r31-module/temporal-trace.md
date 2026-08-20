# R-31 module — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| **4.0 (2018)** | 模块系统引入 — RedisModule API 初版 (类型/命令/调用/阻塞; 版权 2020-Present 为 RSAL 重组); tests/modules 基础面 (helloworld/hellotype) |
| 5.0-6.0 | 扩展面: 线程安全上下文完善; 事件系统扩展 (REDISMODULE_EVENT_*); 命令过滤 (RegisterCommandFilter); 模块 ACL 集成 (RM_CreateModuleUser); 模块 defrag (RegisterDefragFunc) |
| **7.0 (2022)** | 模块配置体系 (RM_RegisterStringConfig 家族 + RM_LoadConfigs); 子命令 (RM_CreateSubcommand); 动态键位置 getkeys-api; typemethods v3/v4 (free_effort/unlink/copy/defrag + 二代); 模块依赖图 (usedby/using); busy 模块命令面 (busy_module_yield) |
| 7.x | typemethods v5 (aux_save2); 模块 GIL 细化 (module_gil_acquring); PostExecUnitJobs (execution unit 后作业) |

## 痕迹证据

- module.c:1253-1276: RM_CreateCommand 双 dict 注册 + ACL ID
- module.c:1286-1301: proxy 注释 ("generic command that works as binding between modules and Redis")
- module.c:6654-6672: 类型 ID 编码 (64 符号 6bit + 10bit encver)
- module.c:6941-6990: typemethods v1-v5 版本字段
- module.c:11999-12060: moduleInitModulesSystem 全局结构 (含线程管道注释)
- module.c:12109: moduleLoadFromQueue 失败退出注释
- redismodule.h: 361 个 API 声明 + REDISMODULE_EVENT_* 45 处
- tests/modules/: 40 个测试模块 (datatype/blockonkeys/defragtest/commandfilter/propagate/hooks)

## 推断标注

- "4.0 引入" — 模块系统公知版本, 仓库无 release note 实证 (标注)
- "5.0-6.0 扩展" — 各 API 年代推断 (标注)
- "typemethods v3/v4 7.0" — 与多域 7.0 重构同代推断 (标注)
- 仓库浅克隆无法 git 考古 — 版本线依赖代码结构/测试面, 已逐条标注实证级别
