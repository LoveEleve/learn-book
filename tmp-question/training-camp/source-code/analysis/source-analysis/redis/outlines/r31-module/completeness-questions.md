# R-31 module — completeness-questions (全视角提问验证)

## 开发者视角

1. 怎么注册一个模块命令? (RM_CreateCommand + proxy + 双 dict)
2. 模块命令的键位置怎么声明? (静态 firstkey/lastkey/keystep 或 getkeys-api)
3. 模块类型怎么进 RDB? (RM_CreateDataType + rdb_save 回调)
4. 后台线程怎么安全操作? (ThreadSafeContext + Lock)
5. 模块怎么阻塞客户端? (RM_BlockClient / OnKeys)
6. 怎么拦截命令? (RegisterCommandFilter)
7. 模块键怎么被 defrag? (RegisterDefragFunc — R-18)
8. 模块加载失败会怎样? (exit(1) — 信任扩展)

## 架构师视角

9. 361 个 API 的版本兼容策略? (typemethods 版本字段 + APIVER)
10. 类型 ID 编码为什么 9×6bit+10bit? (64 位无碰撞面 + 版本)
11. proxy 模式为什么让模块命令"同构"? (命令表/ACL/键提取全复用)
12. 临时客户端的设计收益? (RM_Call 复用完整命令链)
13. 全局锁线程模型 vs 多线程核心? (单线程核心的受控并发)
14. 钩子面 (过滤/事件) 的取舍? (行为扩展 vs 侵入核心)
15. 为什么不做沙箱? (C 信任扩展 — 对照 Lua)
16. 模块依赖图 (usedby/using) 解决什么? (加载顺序/卸载安全)

## 学生视角

17. 模块和 Lua 脚本什么区别? (编译语言全权限 vs 沙箱脚本)
18. 模块键是什么? (Redis 不认识, 但 RDB/复制都"认识")
19. "信任扩展"什么意思? (崩溃即进程崩溃)
20. 什么时候用模块? (新数据结构/定制行为 — 面试低频但扩展面完整)
