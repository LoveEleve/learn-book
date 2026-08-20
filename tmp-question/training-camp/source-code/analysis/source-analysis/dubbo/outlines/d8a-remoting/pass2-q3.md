# D-8a 传输抽象 + exchange — Pass 2 闭环 Q3: 心跳与响应回填

> 核心: HeartbeatHandler + Timer 任务族 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 长连接怎么保活? 心跳谁发谁回? 空闲连接怎么清?**

## 机制链 (已实证)

```
HeartbeatHandler (support/header/HeartbeatHandler.java:33-100) — 装饰器链 (AbstractChannelHandlerDelegate):
├── HEARTBEAT_KEY 参数 (⚠ 默认 DEFAULT_HEARTBEAT = 60s, Constants:157)
├── 收到心跳请求 (isHeartbeatRequest) → HeartBeatRequest → 回 HeartBeatResponse
├── 收到心跳响应 → 日志 (连接仍活)
└── 无数据传输超时 → 触发心跳/断连处理

Timer 任务族 (support/header/, AbstractTimerTask 基类):
├── HeartbeatTimerTask — 定时发心跳 (无数据超时 → 发送)
├── CloseTimerTask — 空闲连接关闭
├── ReadWriteTimeoutTimerTask — 读写超时
└── AbstractTimerTask (基类) + 定时调度

响应回填路径 (q2 关联):
HeaderExchangeHandler.received:
├── Response → DefaultFuture.received (q2: FUTURES.remove → complete)
└── Request → handleRequest → ExchangeHandler.reply (服务端面, D-2/D-9 关联)
```

## 关键设计 (why)

1. **心跳 = 长连接保活**: TCP 长连接空闲时靠心跳探测对端存活 — 防止半开连接 (防火墙/对端崩溃)
2. **双向心跳**: 心跳请求/响应成对 — 单发单收都验证连通性
3. **Timer 任务族分工**: 心跳 (保活) / 关闭 (空闲清理) / 读写超时 — 连接生命周期面
4. **装饰器链**: HeartbeatHandler 是 ChannelHandler 装饰器 (D-1 Wrapper 式) — 可组合

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| HeartbeatHandler 核心 | HeartbeatHandler.java:33-100 |
| DEFAULT_HEARTBEAT = 60s | remoting/Constants.java:156-157 |
| AbstractTimerTask 基类 | support/header/AbstractTimerTask.java |
| CloseTimerTask | support/header/CloseTimerTask.java |
| HeaderExchangeHandler.received 分流 | HeaderExchangeHandler.java:196-205 |

## 负面空间 (Q3 面)

- 不做自适应心跳间隔 (固定 heartbeat 参数)
- 不做对端空闲推测 (只发心跳, 不猜状态)
- 不做多路复用心跳 (每连接独立)
