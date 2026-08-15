# q31 — Observability(深度版:OTLP 双通道 + 结构化日志 + 录制安全)

> 域:可观测(JD 7/10) | 文件:core/src/observability/(otlp 79/logging 71/shared)+ packages/stats/ + packages/http-recorder/(cassette 179/redaction 117/redactor 135/socket 326/websocket 173)+ core/test/effect/observability.test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

可观测分三层:结构化日志(文件+stderr)、OTLP 导出(logs + traces 双通道)、HTTP 录制(测试基建,带密钥检测防泄漏)。runID(8 位随机)贯穿所有资源属性——单次运行的关联键。

## 验证

### 1. runID(设计 1:运行关联键)

```ts
// observability/shared.ts:runID = crypto.randomUUID().slice(0, 8)
// resource() 里:opencode.run + service.instance.id 都 = runID(otlp.ts:44-45)
// 日志 formatter:每条带 run 字段(logging.ts:12)
// 用途:一次启动的所有日志/追踪可关联
```

### 2. OTLP(设计 2:logs + traces 双通道)

```ts
// otlp.ts:50-77
loggers():endpoint 配置才启用 → OtlpLogger({ url: /v1/logs, resource, headers })
tracingLayer():动态 import(懒加载)@effect/opentelemetry + OTLP trace exporter(/v1/traces)
  + BatchSpanProcessor
// 关键:AsyncLocalStorageContextManager 全局注册——"AI SDK uses it to parent spans"
//   (Effect Node SDK 不注册全局 context manager,但 AI SDK 需要)
// resource 属性:serviceName "opencode" + version + deployment.environment.name(安装渠道)
//   + opencode.client + opencode.run + service.instance.id
```

**设计要点**:tracing 懒加载(Endpoint 未配置零成本);AI SDK 与 Effect 的 span 父子关系通过全局 context manager 打通。

### 3. 结构化日志(设计 3:key=value 扁平化)

```ts
// logging.ts:6-52
formatter:timestamp/level/run + message + cause + spans + annotations
flatten:嵌套对象 → key.path=value(循环引用 → "[Circular]")
format:值含空白/引号 → JSON.stringify(否则裸输出)
// fileLogger:追加写(flag "a");batchWindow 不能 0(高 CPU 警告)
// minimumLogLevel:OPENCODE_LOG_LEVEL 环境变量(DEBUG/INFO/WARN/ERROR,默认 INFO)
// OPENCODE_PRINT_LOGS=1 → 文件 + stderr 双输出
```

### 4. HTTP 录制(设计 4:cassette + 密钥检测)

```ts
// http-recorder/cassette.ts:9-26,41-70
cassette = { version, metadata, interactions[] }(JSON 2 空格缩进)
cassettePath:名字安全校验(绝对路径/.. 穿越拒绝)
failIfUnsafe:写入前扫描 secretFindings → 有密钥 → UnsafeCassetteError 拒绝写入!
// redaction.ts:secretFindings(密钥模式检测);redactor.ts:脱敏
// socket.ts(326)+ websocket.ts(173):WebSocket 录制
// matching.ts:回放匹配(method/URL/头/body)
```

**设计要点**:录制文件先过密钥检测再落盘——防止 API key 泄漏进测试夹具(LLM 包的录制测试也用它)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | runID 运行关联键(日志+资源属性) | shared.ts + logging/otlp | ③全链路关联 |
| 2 | OTLP logs+traces 双通道 + 懒加载 | otlp.ts:50-77 | ③遥测导出 |
| 3 | 结构化日志 key=value 扁平化 | logging.ts:6-52 | ③机器可读日志 |
| 4 | 录制密钥检测(防泄漏) | cassette.ts:69-70 + redaction | ③测试安全 |

## 面试弹药

- "AI SDK 与 Effect span 打通":Effect 不注册全局 context manager,AI SDK 用 AsyncLocalStorage 需要——手动全局注册
- "录制先过密钥检测":cassette 写盘前 scan secrets,发现即拒绝——测试夹具不泄漏 key
- "runID = 一次运行的关联键":日志/资源属性/instance id 共享——跨通道关联
- "tracing 懒加载":无 endpoint 零成本(动态 import)

## 待深挖

- [ ] stats 包(Athena/R2 统计管道)
- [ ] http-recorder 的 websocket 录制
- [ ] observability.test.ts 契约
