# SW-4C Query interface / query plugin boundary — Pass 0 发现

> 模块: `oap-server/server-query-plugin`
> 日期: 2026-08-18
> 状态: **仅完成 SW-4C 的域内再拆分 Pass 0，尚未进入具体子域的 Pass 1/2/3**

## 1. 为什么 SW-4C 仍然不能直接当单域
`server-query-plugin/` 当前已经确认至少包含：
- `query-graphql-plugin`
- `zipkin-query-plugin`
- `promql-plugin`
- `traceql-plugin`
- `logql-plugin`
- `status-query-plugin`

这些子模块的机制完全不同：
- GraphQL：统一 query API / resolver 体系
- Zipkin query：Zipkin 兼容接口
- PromQL / TraceQL / LogQL：各自 DSL parser/visitor/runtime
- Status query：调试/状态/cluster/alarm/ttl 等运维接口

若直接把 SW-4C 当单域，会再次出现“大域吞并多个机制”的问题。

## 2. 当前建议的 SW-4C 域内再拆分
### SW-4C1 GraphQL query boundary
主战场：
- `server-query-plugin/query-graphql-plugin/`

### SW-4C2 Zipkin query compatibility
主战场：
- `server-query-plugin/zipkin-query-plugin/`

### SW-4C3 Query-side DSL plugins
主战场：
- `server-query-plugin/promql-plugin/`
- `server-query-plugin/traceql-plugin/`
- `server-query-plugin/logql-plugin/`

### SW-4C4 Status / debug / ops query
主战场：
- `server-query-plugin/status-query-plugin/`

当前只完成了这一步拆域判断。

## 3. 已见到的结构证据
### 3.1 `server-query-plugin` 父模块本身很薄
首轮 `mvn -pl oap-server/server-query-plugin -am test` 显示父模块主要是聚合器；复杂逻辑并不在顶层 pom，而在子插件目录。

### 3.2 子模块类型明显分化
- `zipkin-query-plugin`：module/provider/config/handler/service 结构清晰，是协议兼容接口型插件
- `promql-plugin`：自带 `PromQLParser.g4` / visitor / handler / response model，是“query-side DSL + API”混合体
- `traceql-plugin`：同样拥有 grammar / parser / converter / proto 依赖
- `status-query-plugin`：大量 debugging/ttl/alarm/cluster/status handler

### 3.3 Query plugin 与前两子域交叉明显
- `SW-4A OAL`、`SW-4B MQE` 更像底层规则/表达式运行时
- `SW-4C` 各 query plugin 是面向接口、兼容层、调试层的消费方

因此从依赖主线看，先完成 A/B，再拆 C，是正确路径。

## 4. 当前方法论结论
从这里继续，正确动作不是写一个宽泛的 SW-4C 总纲，而是：
1. 接受 `server-query-plugin` 需要继续拆域
2. 先从 **SW-4C1 GraphQL query boundary** 开始
3. 再看 **SW-4C2 Zipkin query compatibility**
4. 再处理 **SW-4C3 Query-side DSL plugins**
5. 最后 **SW-4C4 Status/debug/ops query**

## 5. 下一步唯一推荐主线
唯一推荐继续路径：
1. `SW-4C1 GraphQL query boundary`
2. `SW-4C2 Zipkin query compatibility`
3. `SW-4C3 Query-side DSL plugins`
4. `SW-4C4 Status/debug/ops query`

原因：
- GraphQL 更接近统一 query 主入口
- Zipkin 是清晰的兼容边界
- PromQL/TraceQL/LogQL 已各自形成 DSL 子系统，适合单列
- Status/debug 更像运维查询侧支线，最后统一更清晰
