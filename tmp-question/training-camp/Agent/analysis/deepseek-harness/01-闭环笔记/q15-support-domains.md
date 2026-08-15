# q15 — 支撑域收尾(深度版:Context/Storage/Spill/Session-Query + 指令契约)

> 域:支撑 | 文件:packages/(context/agent-instructions + session-reference)+(storage/storage)+(spill/spill)+ session-query/(session-query + tool-session-query)+ attachment + code-runtime + feedback + plan + preset
> review 轮次:3 轮(源码结构 + 核心接口 + 指令测试契约 4648 行)

---

## 假设

支撑域补齐:Context 注入(agent-instructions = 文件指令投影;session-reference = 会话引用)、Storage(后端注册表 + KV 面)、Spill(溢出)、Session-Query(会话检索 + 工具)、其他小域。**指令测试契约(4648 行)揭示发现序/预算/定界符安全**。

## 验证

### 1. Agent-Instructions(设计 1:文件指令投影)

```ts
// context/agent-instructions/src/index.ts:80-130:
apply(ctx, config):resolveConfig + 版本缓存 + 基线准备
compose(agent, signal, claimed, pending, touchedPaths):从文件系统指令组装 user 消息
  maxBytes <= 0 → undefined(禁用)
  findProjectRoot(cwd, markers, fs, signal)——项目根发现
// 提交边界:"Execution ancestry and the enclosing durable step are the two commit
//   boundaries before an asynchronous projection may mutate the agent inbox"
```

### 2. 指令测试契约(设计 2:发现序/预算/定界符)★ review 轮 3

```ts
// tests/agent-instructions.spec.ts(4648 行,关键):
1. 发现序(294-361):user-global 先,然后 root-to-cwd 优先级序;同目录本地 overlay 默认加载;
   候选空 → 无 overlay;ENOTDIR 探测 = 确认缺席
2. 项目根标记(382):.git 文件 = 根标记,不向上搜索
3. 内容重读(402):同版本同大小重写后重读(内容变化检测)
4. 符号链接(453-474):跟随到目标内容(经 ctx.fs)
5. 字节预算(496-830):零禁用;预算内保留更具体文件并命名省略/截断路径;
   父文件丢弃保子文件;最长最具体后缀;单超大文件截断到最大切片
6. system-reminder 安全(696-748):熟悉文本渲染无自定义标记;**字面关闭定界符中和**
   (内容/路径/预算标记路径全部中和——防指令注入逃逸)
7. 配置候选(514-566):排除 CLAUDE.md/配置序/仅同目录文件名
8. home 解析(577-664):cwd 为根(无标记)/DSH_HOME/默认 ~/.dsh/展开/去重
```

**设计要点**:定界符中和是安全关键(指令内容含 `</system-reminder>` 字面 → 中和,防注入逃逸);预算策略"最长最具体后缀"。

### 3. 其他(设计 3:Storage/Spill/Session-Query)

```ts
// storage/storage:BackendRegistry + StorageError + StorageBackend/KvFacet/KvUnit + UNIT_NAME_RE
// spill/spill:溢出处理(code-dispatch-log 的 spill 政策)
// session-query:corpus/cursor/documents/extraction/filters/sources/tracing
// tool-session-query:operations(5 操作)+ presentation + service-boundary + workspace-access
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 指令投影(两提交边界 + 串行化) | agent-instructions:80-130 | ①指令注入 |
| 2 | 发现序(user-global → root-to-cwd + overlay) | agent-instructions.spec:294-361 | ①指令优先级 |
| 3 | 字节预算策略(最长最具体后缀) | agent-instructions.spec:762-830 | ①上下文预算 |
| 4 | 定界符中和(防注入逃逸) | agent-instructions.spec:696-748 | ①安全 |
| 5 | Storage 后端注册表 + KV | storage/storage | ④存储抽象 |
| 6 | Spill 溢出 + Session-Query 检索 | spill + session-query | ②④ |

## 面试弹药

- "定界符中和防逃逸":指令内容含 </system-reminder> 字面 → 中和——注入防护
- "预算策略保最具体":父文件丢弃保子文件,最长最具体后缀——上下文最优保留
- "同版本同大小也重读":内容变化检测不止于元数据——重写检测
- "ENOTDIR = 确认缺席":探测失败分类(缺席 vs 错误)

## 待深挖

- [ ] session-query 的语料/提取实现
- [ ] spill 的溢出策略细节
- [ ] code-runtime 的 worker-thread 语义
