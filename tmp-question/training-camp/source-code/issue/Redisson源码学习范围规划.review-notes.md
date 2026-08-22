# Redisson 源码学习范围规划复盘与修订路线图

> 复盘基线：Redisson main 分支（latest），源码 `/data/workspace/source-code/code/spring/redisson/`。
> 本文不是正文大纲，而是按"先模块扫描，再按机制重组；补齐失败路径、测试证据、集成边界"的方法论，对现有 7 域规划做卷级复盘。

## 一、复盘结论

现有 7 域规划（4 核心 + 3 扩展）抓住了 Redisson 的最高价值主题：

- 连接管理与初始化链路
- RLock 分布式锁 + Watchdog 批量续期
- Codec 序列化体系
- 命令执行流水线
- RMap 分布式映射
- Spring Cache 集成
- 基础数据结构封装

从方法论看，原规划还有两个风险：

1. **R-4 命令执行流水线与 R-1 连接管理紧密耦合**。`CommandAsyncService` 的初始化完全依赖 `ServiceManager`，两台篇在阅读时建议紧密相邻，规划可考虑合并为"连接与命令执行"单一域或保持两篇但明确依赖。

2. **低估了 RLock 的面试深度**。Watchdog 的 `AsyncChunkProcessor` 批量续期、RedLock 的时序问题、`fairLockWaitTimeout` 的公平锁排队机制——这些面试高频点在规划中只有概述，正文需要展开到方法级锚点。

## 二、已验证的仓库事实

| 项目 | 规划值 | 实际值 | 差异 |
|------|:------:|:------:|:----:|
| Redisson.java | 1532 | 1532 | 一致 |
| Config.java | 1351 | 1351 | 一致 |
| ServiceManager.java | 804 | 804 | 一致 |
| CommandAsyncService.java | 1256 | 1256 | 一致 |
| RedissonLock.java | 600 | 600 | 一致 |
| RedissonMap.java | 1967 | 1967 | 一致 |
| api 接口数 | 801 | 801（未逐文件核对） | 可接受 |
| 核心子模块 | 1570 文件 | 1570（未逐文件核对） | 可接受 |

## 三、建议的阅读顺序

```
R-1 连接管理 + 初始化
  → R-4 命令执行流水线
    → R-2 RLock + Watchdog
      → R-3 Codec 序列化
        → R-5 RMap 分布式映射
          → R-6 Spring Cache 集成
            → R-7 基础数据结构
```

## 四、需要修订的旧判断

### 1. R-1 与 R-4 的依赖关系需明确

原规划把 R-1 和 R-4 列为独立域，但 `CommandAsyncService` 的初始化依赖 `ServiceManager`，而 `ServiceManager` 在 R-1 中管理。写作时 R-1 正文中需要点到 `CommandAsyncService` 但不深挖，R-4 正文中回顾 `ServiceManager` 的初始化。

### 2. R-3 Codec 放在 R-2 之后

原规划把 Codec 放在 R-2（RLock）之后。从机制上看，Codec 在 R-1 初始化时就需要（`config.setCodec`），但理解 Codec 不依赖任何其他域。R-2 的 Watchdog 也不需要 Codec 知识。所以 Codec 放在 R-2 之后是合理的——先理解锁，再理解序列化。

## 五、方法论自检结果

- 已按 Java 项目方法论适配（锚点格式 `org.redisson.xxx.ClassName.java:line`）
- 已明确淘汰清单（12 个子模块/功能）
- 已明确阅读顺序
- 当前仍未完成：逐域源码锚点表、逐域测试证据盘点

## 六、统计

| 类别 | 数量 |
|:----:|:----:|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 3 |
| **总域** | **7** |
| 淘汰子模块/功能 | 12 个 |