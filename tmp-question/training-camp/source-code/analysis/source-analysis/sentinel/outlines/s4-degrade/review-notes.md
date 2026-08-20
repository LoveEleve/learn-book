# S-4 熔断降级域 — 审查记录

## Pass 1 / Pass 2

- 识别两个槽的边界：`DegradeSlot`(旧式规则)与 `DefaultCircuitBreakerSlot`(默认规则)。
- 确认状态机：CLOSED / OPEN / HALF_OPEN，靠 `AtomicReference.compareAndSet` 惰性推进，不靠定时线程。
- 确认探测请求唯一性：`OPEN → HALF_OPEN` 用 CAS，只有一个请求返回 true。
- 确认 `whenTerminate` 是 HALF_OPEN 兜底，为 issue #1638 场景提供恢复路径。
- 确认异常熔断支持异常比例/异常数两种 grade，慢调用熔断支持 RT grade。
- 确认规则更新按 rule 身份复用断路器，保留状态连续性。

## 深审修正

1. 默认 `"*"` 规则的断路器复用语义不能简单等同于旧式 resource-specific 规则，已在 `pass2-q9` 修正。
2. HALF_OPEN 兜底回调未检查 CAS 返回值却直接通知观察者，存在边界不一致，已在 `pass2-q10` 修正。
3. 上篇正文锚点修正：
   - `DegradeSlot` / `DefaultCircuitBreakerSlot` 入口与 exit 行号
   - `DegradeRuleManager.newCircuitBreakerFrom` 实际在 165-175
   - `isValidRule` 实际在 177-200
4. 中篇正文锚点修正：
   - `tryPass` 实际在 68-77
   - `fromCloseToOpen` 在 93-101
   - `fromOpenToHalfOpen` 在 104-112
   - `fromHalfOpenToOpen` / `fromHalfOpenToClose` 在 132-147
5. 下篇正文锚点修正：
   - `ExceptionCircuitBreaker` 构造/统计/阈值判定行号
   - `ResponseTimeCircuitBreaker` 构造/统计/阈值判定行号

## 三篇正文

- `01-degrade-check.md`：规则加载、两槽边界、规则校验、断路器复用
- `02-circuit-breaker.md`：三态状态机、恢复窗口、探测请求、观察者
- `03-strategies.md`：异常比例/异常数、慢调用比例、HALF_OPEN 单请求判定

## 遗留

1. 默认规则 manager 的 `getExistingSameCbOrNew` 使用了 `getCircuitBreakers(rule.getResource())`，其中 rule 的 resource 是 `"*"`，与 `getDefaultCircuitBreakers` 的缓存 key 语义不同，存在未完全一致的复用边界；已在闭环记录标注，未深入追查。
2. HALF_OPEN 兜底回调的 CAS 返回值未检查属于历史遗留，正文如实记录，未做修改建议。
3. `DegradeException` 与 `FlowException` 的继承关系未在正文展开（属于 block 异常族，可后续统一）。
