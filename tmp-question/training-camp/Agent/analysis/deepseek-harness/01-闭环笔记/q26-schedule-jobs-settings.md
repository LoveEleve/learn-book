# q26 — Schedule/Jobs/Settings-Scope/Client-传输(深度版:支撑细节)

> 域:②执行(调度/后台)+ ①配置(作用域)+ ②客户端 | 文件:packages/(schedule/schedule:domain 519+/runtime/transaction/persistence/tools)+(jobs/jobs-local:index)+(settings/settings:index 863+)+(client/connection:websocket-downlink 144+/rpc-host/http-bridge/loopback-hostname/api-request-trust)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Schedule = 事务化调度(时间解析严格 + 时区规范化);Jobs = 进程本地任务注册表(每 owner 上限);Settings = 作用域注册(owner scope);Client = 连接传输(WebSocket 下行 + RPC 宿主)。

## 验证

### 1. Schedule(设计 1:严格时间域)

```ts
// schedule/domain.ts:21-56:
SCHEDULE_CHANGE_VERSION = 1;MIN_EVERY_INTERVAL_SECONDS = 300(固定率下限 5 分钟)
时间解析正则(UTC/OFFSET/LOCAL_DATE/LOCAL_TIME/IANA_ZONE/OFFSET_NAME)——严格校验
ScheduleLogError(code: 'corrupt_schedule_log')/ ScheduleInputError:
  ——"malformed or transition-invalid durable Schedule data"vs"model-supplied rule that cannot become a record"
canonicalizeTimeZone(251):时区规范化
resolveEveryOccurrence(519):固定率出现解析
FoldedSchedules/EveryOccurrence(90-110):折叠调度
// transaction.ts:调度变更事务化(持久化一致)
```

### 2. Jobs(设计 2:本地任务注册表)

```ts
// jobs-local/index.ts:25-50:
TASK_WAIT_TIMEOUT;DEFAULT_MAX_CONCURRENT_TASKS_PER_OWNER = 10
TrackedTask:{ id, kind, label, outputLimitBytes, owner(精确生命周期 owner;会话 ID 授权从它派生), cancel, readOutput, status, detail }
  "The registry's mutable per-job record (never handed out — see LocalJobRegistry.snapshot)"
// ——job 记录永不外发(快照只读);owner 授权派生
```

### 3. Settings 作用域(设计 3:owner scope)

```ts
// settings/settings/src/index.ts:426-435,831-871:
register(ns, schema, options):注册命名空间 schema 并接收 owner scope
  ——"the owner scope for reads, observation, and updates"
resolveSettingsSource(831):活跃配置源(已解析作用域)
installSettingsSection(863):base 层 + 源 thunk 指向已解析作用域
  "no settings service ever mounted means none of this runs"(scoped fiber)
```

### 4. Client 传输(设计 4:连接层)

```ts
// client/connection/websocket-downlink.ts:51-144:
WebSocketDownlinks(下行集合)+ rejectWebSocketUpgrade(socket)(拒绝升级——安全)
// rpc-host.ts:HostConnectionService implements HostConnectionHandle
// http-bridge/loopback-hostname/api-request-trust:HTTP 桥/回环安全/请求信任
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Schedule 严格时间域 + 双错误分类 | schedule/domain.ts | ②调度 |
| 2 | Jobs 注册表(owner 授权 + 快照只读) | jobs-local | ②后台 |
| 3 | Settings owner scope | settings/settings | ①配置作用域 |
| 4 | Client 传输(WS 下行 + 升级拒绝) | client/connection | ②连接 |

## 面试弹药

- "时间域严格校验":UTC/偏移/本地/IANA 全正则——畸形调度数据显式错误(corrupt 区分 input)
- "job 记录永不外发":快照只读——内部可变状态不泄漏
- "owner 授权派生":会话 ID 授权从 job owner 派生——权限跟生命周期
- "设置无服务则不跑":scoped fiber——零安装零开销

## 待深挖

- [ ] schedule transaction 的提交语义
- [ ] websocket-downlink 的完整连接管理
- [ ] settings 的 layer 合并
