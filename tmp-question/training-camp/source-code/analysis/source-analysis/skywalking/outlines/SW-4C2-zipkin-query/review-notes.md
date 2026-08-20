# SW-4C2 Zipkin query compatibility — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 结构复核
确认：
- provider 负责独立 HTTP server 装配
- handler 负责协议边界和响应编码
- service 负责 DAO 分支和 event 追加
- v1/v2 DAO 是核心内部兼容点

## Review 2 — 输入边界质疑
新增 `ZipkinQueryHandlerTest` 后发现真实问题：
- `getTraceById("   ")` 未被 `StringUtil.isEmpty` 拦截
- 继续进入 DAO 链路，无法稳定返回 400

同时确认：
- traceMany 空输入和重复输入已有明确 400 语义
- config.json 的关键字段可稳定输出

## Review 3 — 修复与回归
修复：
- `ZipkinQueryHandler.getTraceById(...)` 改用 `StringUtil.isBlank(...)`

验证：
```bash
./mvnw -pl oap-server/server-query-plugin/zipkin-query-plugin -am -Dtest=ZipkinQueryHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/server-query-plugin/zipkin-query-plugin -am test
```

结果：新增测试 **4/4 PASS**，完整模块回归通过。

## 最终判断
- blank traceId 缺陷已修复
- 当前无新的已知生产代码问题
- 文档、源码、测试已一致

## 剩余风险
- v1/v2 DAO 及 attached event 复杂路径仍主要依赖跨模块集成测试
- names cache header 的最终客户端行为留待更高层 integration 交叉验证
