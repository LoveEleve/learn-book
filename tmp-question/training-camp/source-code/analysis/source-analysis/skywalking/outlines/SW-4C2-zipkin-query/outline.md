# SW-4C2 Zipkin query compatibility — Outline

> 模块: `oap-server/server-query-plugin/zipkin-query-plugin`
> 日期: 2026-08-18

## 1. 域定位
`SW-4C2` 是 Zipkin Query API 兼容层，负责：
- HTTP endpoint
- Zipkin 参数解析
- OAP storage DAO 调用
- Zipkin JSON v2 响应
- v1/v2 DAO 兼容
- attached event 到 span annotations/tags 的转换

它不是 Zipkin receiver，也不是通用 OAP query service 本体。

## 2. 两层结构
### 2.1 Handler
`ZipkinQueryHandler` 负责：
- endpoint 路由
- 参数校验
- HTTP status
- config JSON
- response encoding
- names 排序与 cache header

### 2.2 Service
`ZipkinQueryService` 负责：
- lazy DAO 获取
- v1/v2 DAO 分支
- traces 查询
- attached event 查询与追加

## 3. 真实修复
初版 `getTraceById(...)` 使用 `StringUtil.isEmpty(traceId)`，无法拒绝纯空白 traceId。

后果：
- blank traceId 会进入 `ZipkinQueryService`
- 可能在 DAO 获取/查询阶段产生 NPE/错误
- 协议层没有得到预期 400

修复：
- 使用 `StringUtil.isBlank(traceId)`
- 保留原错误响应正文 `traceId is empty or null`

## 4. 测试覆盖
新增：
- `ZipkinQueryHandlerTest`

覆盖：
- `/config.json` 响应体
- blank `traceId` -> 400
- empty `traceIds` -> 400
- duplicate `traceIds` -> 400

## 5. 其他已确认语义
- traceMany ID 会 trim + normalize
- duplicate 用 400 拒绝
- names endpoint 结果排序后返回
- names endpoint 按数量条件加 cache header
- trace v2 DAO 通过 `IZipkinQueryV2DAO` 类型判断
- attached events 会按 spanId 与方向规则匹配并追加为 annotation/tag

## 6. 外域边界
- `SW-4C2`：Zipkin query compatibility
- `SW-4C1`：GraphQL query API
- `SW-4C3`：PromQL / TraceQL / LogQL query-side DSL
- `SW-5`：storage DAO 实现与 TTL 不在本域展开
- `SW-7`：Zipkin receiver / ingress 不在本域展开

## 7. 验证命令
```bash
./mvnw -pl oap-server/server-query-plugin/zipkin-query-plugin -am -Dtest=ZipkinQueryHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/server-query-plugin/zipkin-query-plugin -am test
```

结果：新增测试 **4/4 PASS**，完整 Zipkin query reactor 回归通过。

## 8. 当前结论
`SW-4C2` 当前达到可交接收敛状态：
- handler/service 边界清晰
- blank traceId 真实缺陷已修复
- 协议输入边界已有专属回归
- 未发现新的已知源码问题
