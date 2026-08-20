# Pass 2 闭环笔记 Q2: TokenResult 的字段与状态消费

## 验证过程

- `TokenResult` 字段：
  - `status`：结果状态
  - `remaining`：剩余额度
  - `waitInMs`：需要等待的毫秒数
  - `tokenId`：并发 token 的唯一 ID
  - `attachments`：附加信息 (`TokenResult.java:17-31`)
- `TokenResultStatus` 定义完整状态集：
  - 错误：`BAD_REQUEST=-4` / `TOO_MANY_REQUEST=-2` / `FAIL=-1`
  - 成功：`OK=0`
  - 拒绝：`BLOCKED=1` / `SHOULD_WAIT=2`（应等待下一窗口）
  - 无规则：`NO_RULE_EXISTS=3` / `NO_REF_RULE_EXISTS=4`
  - 不可用：`NOT_AVAILABLE=5`
  - 释放：`RELEASE_OK=6` / `ALREADY_RELEASE=7` (`TokenResultStatus.java:13-43`)
- checker 侧的消费（`FlowRuleChecker.applyTokenResult`）：
  - OK → 放行
  - SHOULD_WAIT → sleep `waitInMs` 后放行
  - NO_RULE_EXISTS / BAD_REQUEST / FAIL / TOO_MANY_REQUEST → 按 `fallbackToLocalOrPass` 回退本地或放行
  - BLOCKED 及默认 → 拒绝 (`FlowRuleChecker.java:186-209`)

## 结论

`TokenResult` 是集群 token 请求的统一结果载体，`TokenResultStatus` 用整数编码所有结果。checker 只关心核心三态（OK / SHOULD_WAIT / BLOCKED），其余状态统一走 fallback 策略，避免集群失败直接影响流量。