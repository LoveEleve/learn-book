# Pass 2 闭环笔记 Q3: ExceptionCircuitBreaker 的异常比例与异常数

## 验证过程

- 断路器构造时从规则读取：
  - strategy = exception ratio 或 exception count
  - minRequestAmount
  - threshold
  - 一个单 bucket 的 `SimpleErrorCounterLeapArray` (`ExceptionCircuitBreaker.java:35-51`)
- 每次完成请求时读取当前 entry 的 `error`：有 error 则 `errorCount + 1`，无论是否异常都 `totalCount + 1` (`ExceptionCircuitBreaker.java:57-71`)。
- CLOSED 状态下遍历滑窗内所有 counter，先判断 `totalCount >= minRequestAmount`，再计算：
  - exception count 模式：`errCount`
  - exception ratio 模式：`errCount / totalCount`
- 当前值严格 `>` threshold 时才 CLOSED → OPEN (`ExceptionCircuitBreaker.java:74-104`)。
- HALF_OPEN 时不重新比较全窗比例，而是只看探测请求：无 error → CLOSED，有 error → OPEN。

## 结论
异常熔断有两种阈值语义：异常数直接比较，异常比例除以总请求数比较；两者都受最小请求数保护。HALF_OPEN 是单请求探测，不复用 CLOSED 的全窗阈值判断。