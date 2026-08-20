# Druid 源码学习范围规划 — 深度 review 记录

> 对照《源码范围规划复盘方法论》和《源码分析深审缺陷档案》对 `Druid源码学习范围规划.md` 的深审记录。
> 日期：2026-08-19

## 一、源码核对结论

本次对 Druid `v1.2.27` 实际源码做了关键声明核对，目录与文件数全部确认无误：

| 子模块 | 规划文件数 | 实测文件数 | 结论 |
|---|---|---|---|
| core/pool | 72 | 72 | ✅ |
| core/filter | 29 | 29 | ✅ |
| core/sql | 1238 | 1238 | ✅ |
| core/wall | 46 | 46 | ✅ |
| core/proxy | 36 | 36 | ✅ |
| core/stat | 24 | 24 | ✅ |
| core/support | 105 | 105 | ✅ |
| core/util | 35 | 35 | ✅ |
| **core 总计** | **1614** | **1614** | ✅ |

关键源码路径：
- `/data/workspace/source-code/code/spring/druid/core/src/main/java/com/alibaba/druid/`
- `pool/DruidDataSource.java`
- `pool/DruidAbstractDataSource.java`
- `pool/DruidPooledConnection.java`
- `filter/FilterChainImpl.java`
- `filter/stat/StatFilter.java`
- `wall/WallFilter.java`

## 二、事实层核对

| 类 | 规划声明 | 实测 | 结论 |
|---|---|---|---|
| `DruidDataSource.java` | 3979 行 | 3979 行 | ✅ |
| `DruidAbstractDataSource.java` | 2388 行 | 2388 行 | ✅ |
| `DruidConnectionHolder.java` | 477 行 | 476 行 | ⚠️ 实测 476 行，已建议定位到方法级时再重验 |
| `FilterChainImpl.java` | ~3000 行 | **5287 行** | ❌ **已修正为 ~5300** |
| `shrink(boolean, boolean)` | ~120 行 | **201 行** | ❌ **已修正为 ~200** |
| `DruidPooledConnection.java` | 1298 行 | 1298 行 | ✅ |
| `StatFilter.java` | — | 1135 行 | ✅ 存在 |
| `WallFilter.java` | — | 1638 行 | ✅ 存在 |

类声明核对：
- `DruidDataSource.java:659` → `public void init()`
- `DruidConnectionHolder.java:46` → `public final class DruidConnectionHolder`
- `DruidPooledConnection.java:43` → `public class DruidPooledConnection`
- `DruidDataSource.java:772` → `connections = new DruidConnectionHolder[maxActive]`（确认固定数组按 maxActive 预分配）
- `DruidDataSource.java:1543` → `getConnectionInternal(long maxWait)`
- `DruidDataSource.java:1894` → `protected void recycle(...)`
- `DruidDataSource.java:3061/3065/3069` → `shrink()` 三重重载存在

## 三、卷级重构结论

### 现有 9 域不是可写的卷结构
- `D-1`/`D-5`/`D-7` 属于连接池骨架层
- `D-2`/`D-8` 属于 Filter / 扩展层
- `D-3`/`D-4` 属于监控与安全层
- `D-6` 属于解析器地基层
- `D-9` 属于集成层

### 建议卷级结构
1. 骨架层：`D-1 DruidDataSource`、`D-5 维护体系`、`D-7 连接验证`
2. Filter/扩展层：`D-2 Filter 拦截链`、`D-8 PreparedStatementPool`
3. 监控与安全层：`D-3 StatFilter`、`D-4 WallFilter`
4. 解析器地基层：`D-6 SQL Parser`（作为共同地基）
5. 集成层：`D-9 Spring Boot 3 Starter`

### 注意
- Druid 主线围绕“一条 JDBC 操作经过连接池 → Filter 链 → SQL 解析 → 监控 → 防火墙”这条链，不能照抄 HikariCP 的“连接生命史”
- SQL 解析器体积巨大（1238 文件），正文只做架构概览，不深入 Lexer/AST/Dialect 实现

## 四、写作时需重新 grep 的硬锚点

> 以下锚点来自本次扫码，写作时须重新确认行号是否漂移，不能直接照抄本文件。

- `DruidDataSource.java:659` `init()`
- `DruidDataSource.java:772` `connections = new DruidConnectionHolder[maxActive]`
- `DruidDataSource.java:1372` 调用 `getConnectionInternal`
- `DruidDataSource.java:1543` `getConnectionInternal(long)`
- `DruidDataSource.java:1894` `recycle(DruidPooledConnection)`
- `DruidDataSource.java:3061/3065/3069` `shrink()` 三重载
- `DruidConnectionHolder.java:46` `class DruidConnectionHolder`
- `DruidPooledConnection.java:43` `class DruidPooledConnection`

## 五、下一次 AI 建议

1. 已修正事实错误：`FilterChainImpl` ~3000 → ~5300；`shrink` ~120 → ~200
2. 后续写 `vol-druid/D-1` 时：
   - 确认 `init()` 全链路：SPI 加载 / Driver 解析 / Filter 初始化 / asyncInit / 三线程启动 / initedLatch
   - 确认 `getConnectionInternal` 路径：closed / disableException / createDirect / pollLast / discard 重试
   - 确认 `shrink` 四阶段：fatalError / idle timeout / keepAlive / compact
   - 确认 `FilterChainImpl` 的 `pos`/`filterSize` 递归链和对象池复用
   - 确认 `StatFilter` 慢 SQL 与参数化
   - 确认 `WallFilter` 四阶段