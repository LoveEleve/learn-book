# SW-4C2 Zipkin query compatibility — Pass 0 发现

> 模块: `oap-server/server-query-plugin/zipkin-query-plugin`
> 日期: 2026-08-18

## 1. 域职责
`SW-4C2` 为 OAP 提供 Zipkin Query API 兼容层：
- 以独立 HTTP server 暴露 Zipkin v2 query endpoints
- 将 Zipkin 查询参数转换为 OAP storage/query DAO 调用
- 将 OAP 返回的 span/traces 编码成 Zipkin JSON v2
- 兼容 trace v1/v2 storage DAO
- 追加 span attached events 到 Zipkin span annotations/tags

## 2. 主链
`ZipkinQueryProvider.start()`
→ 初始化 `HTTPServer`
→ 注册 `ZipkinQueryHandler`（GET）
→ handler 解析 Zipkin HTTP 参数
→ `ZipkinQueryService`
→ `IZipkinQueryDAO` / `IZipkinQueryV2DAO`
→ `SpanBytesEncoder.JSON_V2`

## 3. 模块装配
`ZipkinQueryProvider`：
- provider name=`default`
- 依赖 `TelemetryModule` 与 `CoreModule`
- 独立读取 rest host/port/context/timeout/queue 配置
- `RunningMode.isInitMode()` 时不启动 HTTP server

## 4. Handler endpoint 族
当前主要接口：
- `/config.json`
- `/api/v2/services`
- `/api/v2/remoteServices`
- `/api/v2/spans`
- `/api/v2/trace/{traceId}`
- `/api/v2/traces`
- `/api/v2/traceMany`
- `/api/v2/autocompleteKeys`
- `/api/v2/autocompleteValues`

## 5. Service 边界
`ZipkinQueryService`：
- lazy 获取 storage DAO
- 若 DAO 实现 `IZipkinQueryV2DAO`，走 trace v2 wrapper 分支
- 否则走旧 DAO 分支
- 统一处理 trace attached event 的查询与追加

## 6. 首轮质疑点
- Q1: 空/blank traceId 是否在进入 DAO 前被拒绝
- Q2: traceMany 是否去重、规范化并对重复 ID 返回 400
- Q3: v1/v2 DAO 分支是否共享相同响应编码语义
- Q4: config.json 是否准确反映 query 配置
- Q5: trace attached event 追加是否会改变原 span 顺序/匹配语义
- Q6: cached names response 是否稳定排序与 cache header

## 7. 当前测试现实
模块原本没有专属测试。

本轮新增 `ZipkinQueryHandlerTest`，优先覆盖 handler 协议边界而不是直接复制 storage DAO 测试。
