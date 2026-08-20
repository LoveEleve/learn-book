# N-14 命名集群与监控 — 知识规划 (KP)

> 🟡 B | 模块: naming/cluster + naming/monitor + naming/remote (20 文件) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 状态机 | ServerStatusManager | UP/DOWN/READY_ONLY |
| 2 | 就绪检查 | NamingReadinessCheckService | 就绪语义 |
| 3 | TPS 监控 | NamingTpsMonitor | 实时统计 |
| 4 | TopN | ServiceTopNCounter | 热点服务 |
| 5 | 动态指标 | NamingDynamicMeterRefreshService | 刷新 |
| 6 | 性能日志 | PerformanceLoggerThread | 定期输出 |
| 7 | gRPC handler | remote/rpc/handler 族 | 请求分派 |
| 8 | UDP 遗留 | remote/udp | 兼容 |

## 02 高频坑
1. ServerStatus 三态 (非 UP/DOWN 两态)
2. READY_ONLY = 订阅恢复中
3. handler 与 api 请求族一一对应
4. TPS 与 TopN 双维度

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 状态 | ServerStatusManager / Readiness / 三态 |
| 监控 | TPS / TopN / 动态指标 / 性能日志 |
| 远端 | handler 族 / UDP 遗留 |

## 04 跨域桥接
- → NC-1: getServerStatus 两端闭合
- → N-11: 健康状态消费
- → 面试: "服务端状态与监控" — 三态机 + TPS + handler 族
