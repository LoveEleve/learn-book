# Reasonix 域发现 v12 补充(第七轮深扫:extension 插件运行时层)— 2026-08-14

> 承接:v11。v12 深扫 internal/extension(sidecar 782/rpcwire 879/uihub 715/dispatch 454/builder 451/publish 417/protocol validate 419)——插件进程运行时。
> 结论:extension 是"子进程插件协议"的完整实现(生命周期预算/代际隔离/凭证脱敏),新增 4 个中高价值域。

---

## 一、v12 新增域

### 🔴 高价值新增(2 个)

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 91 | **侧车生命周期预算** | internal/extension/sidecar/client.go(782) | 782 | **子进程插件的完整生命周期预算**:握手 30s/关机请求 5s/通知队列 256(**满则断连而非丢弃**)/写停滞上限 10s/拦截超时(60s 上限,快路径 5s);进程级隔离 | ③扩展 |
| 92 | **UI 中枢 uihub** | internal/extension/uihub/hub.go(715) | 715 | 结构化 UI 面:**代际隔离**(迟到的发布/请求不匹配当前代 → 丢弃,重载后的迟到结果绝不覆盖新代状态);凭证脱敏(所有 sidecar 文本先 RedactCredentials);未知/崩溃客户端拒绝 | ③扩展/④安全 |

### 🟡 中价值新增(2 个)

| # | 域 | 文件 | 体量 | 设计要点 |
|---|----|------|:--:|---------|
| 93 | **RPC 线协议** | internal/extension/rpcwire/conn.go(879) | 879 | JSON-RPC NDJSON;HandlerResponse **AfterWrite 传输清理回调**(成功写后才执行,如 detach 前确认) |
| 94 | **扩展分发/构建/发布** | extension/(dispatch 454/builder 451/publish 417/protocol validate 419) | 1,741 | 分发调度器/构建器/发布管线/协议校验 |

---

## 二、跨项目印证(插件运行时归并)

| 维度 | Hermes | Reasonix |
|------|--------|----------|
| 插件进程 | plugins/ + hermes_cli/plugins.py | **extension sidecar(独立进程)** |
| 协议 | — | **Extension Protocol v2(JSON-RPC NDJSON)** |
| 超时预算 | — | **握手 30s/通知队列 256 满则断连** |
| 代际隔离 | — | **uihub:迟到结果绝不覆盖新代** |
| 凭证脱敏 | message_sanitization/redact | **RedactCredentials 先于表面化** |

**新增通用模式**:
1. **"通知队列满则断连而非丢弃"** — 流式通知的背压策略(丢数据比断连更糟)
2. **"代际隔离"** — 重载后的迟到结果绝不覆盖新代状态(与 SessionStore routing_generation 同思想)
3. **"插件文本凭证脱敏先于表面化"** — 任何外部来源文本进 UI 前脱敏

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v5 | 原始 | 41 | 41 |
| v6 | 体量排序复测 | +12 | 53 |
| v7 | 策略/契约/存储层 | +14 | 67 |
| v8 | 验收报告层 | +6 | 73 |
| v9 | 执行正确性/并行层 | +7 | 80 |
| v10 | boot 运行时组装层 | +5 | 85 |
| v11 | bot 消息网关层 | +5 | 90 |
| v12 | extension 插件运行时层 | +4 | **94** |

> 剩余:provider 适配细节、i18n/notify/secrets 等支撑、desktop 前端——产品价值已低。94 域远超需求,可收敛。
