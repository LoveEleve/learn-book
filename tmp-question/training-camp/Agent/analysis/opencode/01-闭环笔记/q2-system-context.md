# q2 — SystemContext(深度版:代数 + 状态机 + 指令域)

> 域:①对齐(上下文代数)+ ②执行(基线管理) | 文件:core/src/system-context/(index.ts 320/registry.ts 49/builtins.ts 50)+ instruction-context.ts(101)+ skill/guidance.ts(76)+ reference/guidance.ts(69)+ test/system-context/(index 307,18 契约/builtins 129,4 契约)+ test/instruction-context.test.ts(323,8 契约)
> review 轮次:3 轮(源码全文 + 30 测试契约 + 内建源/技能/引用源)

---

## 假设

SystemContext 是"可刷新上下文源的代数 + 状态机":initialize(全量观察→基线)/ reconcile(比较→四态)/ replace(替换)。指令源(AGENTS.md)是它的第一个真实消费者,测试契约揭示了很多代码注释没写的语义(顺序、空渲染、竞态保护)。

## 验证

### 1. 代数核心(设计 1:Source 六字段 + make 类型隐藏 + 惰性渲染)

```ts
// index.ts:32-39
Source<A> = { key(命名空间:^[a-z0-9][a-z0-9._-]*\/...), codec, load, baseline, update, removed? }
// make(index.ts:135-173):闭包捕获 A,内部 build load:
//   isUnavailable → 原样返回
//   否则 { baseline(), compare(previous) }:compare → None:Incompatible/等价:Unchanged/不等:Updated(render 惰性)
```

**惰性渲染**(index.ts:109-117):比较时不渲染,渲染推迟到确定要发消息时。测试 13(index.test.ts:224-247):替换路径 updates 计数 = 0。

### 2. 三操作(设计 2)

**initialize**(index.ts:198-206):observe 全部源(并发 unbounded)→ 任一 Unavailable → **InitializationBlocked(keys)**(测试 6);全可用 → 一次性渲染所有 baseline(loads=1,测试 2)。

**reconcile**(index.ts:218-280):observe → reconcileObservation:
- 源无法解码 → Replace(index.ts:239,测试 11)
- 源消失且无 removed 渲染 → Replace(index.ts:242-245,测试 8)
- 逐源比较 → Unchanged/Updated
- 消失源有 removed → 渲染移除文本(测试 7)
- 多移除按 key 排序(index.ts:272-277,测试 9)
- 新源 → 渲染 baseline(测试 4)

**replace**(index.ts:283-291):任一已有源 Unavailable → ReplacementBlocked(测试 5/14);否则一次观察重建完整 generation(loads=1,测试 12)。

### 3. Unavailable 语义(设计 3:stale-while-revalidate 全路径)

| 操作 | 已有 snapshot | 无 snapshot | 测试 |
|------|--------------|-------------|------|
| reconcile | Unchanged(保留已承认值) | 跳过该源 | index.test.ts:121-130 |
| replace | ReplacementBlocked | ReplacementReady | index.test.ts:121-130 |
| initialize | — | InitializationBlocked(keys) | index.test.ts:132-144 |

### 4. 渲染约束(设计 4:非空 + 稳定序)

- **空渲染拒绝**(index.ts:309-312 requireText):baseline/update/removed 渲染空串 → throw(测试 10)。
- **稳定序**:removed 按 key 排序(index.ts:272-277);registry 加载按 key 排序(registry.ts:40)+ 并发 load 后 combine。
- **snapshot 校验**(测试 18):key 命名空间 + removed 非空。

### 5. 指令域(设计 5:观察协议 + 三竞态保护)

```ts
// instruction-context.ts:40-74 observe:
1. fs.up({ targets: ["AGENTS.md"], start: directory, stop: project.directory })   ← 项目根向上
2. OPENCODE_DISABLE_PROJECT_CONFIG || !insideProject → 不扫(测试 6/7)
3. 去重 + 全局 config/AGENTS.md 置顶
4. 并发读所有文件(readFileStringSafe)
5. 竞态保护(测试 3/4):
   - 发现的文件读前消失且属于 discovered → 返回 Unavailable(整体不可观察)
   - up() 失败 → catch 到 Unavailable(instruction-context.ts:86-87)
6. 空文件保持可用(测试 2)
```

**渲染**(instruction-context.ts:99-101):`Instructions from: ${path}\n${content}`,多文件 "\n\n" 连接。

**更新/移除语义**(instruction-context.ts:35-37 + 测试 1):
- 聚合变化 → "These instructions replace all previously loaded ambient instructions." + 完整新聚合
- 全部移除 → "Previously loaded instructions no longer apply."
- 顺序:global 在前,然后项目向上文件(测试 1:[globalFile, packageFile, projectFile])

### 6. Registry(设计 6)

```ts
// registry.ts:25-44
register:Ref 追加 + acquireRelease;重复 key → die(registry.ts:28-33)
load:toSorted(key) + 并发 load + SystemContext.combine —— 稳定 contribution-key 顺序
```

### 7. 内建源(设计 7:builtins + 技能/引用指导)

```ts
// builtins.ts:24-40
core/environment: 工作目录/workspace/git repo/平台 → "<env>...</env>"
core/date: 日期 → "Today's date: ..."(同本地日历日不重复更新,builtins.test.ts:96-105)
// 组合顺序:builtins → instructions(builtins.test.ts:107-128)
```

```ts
// skill/guidance.ts:46-68:按 agent 权限过滤,只列 name+description(正文走 skill 工具);
// 空列表 + 权限 deny → SystemContext.empty;移除 → "Do not use any previously listed skill"
// reference/guidance.ts:40-61:同模式(core/reference-guidance)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Source 六字段 + make 类型隐藏 + 惰性渲染 | index.ts:32-39,135-173 | ①6 维盘问来源建模 |
| 2 | 三操作代数(initialize/reconcile/replace) | index.ts:198-291 | ①上下文状态机 |
| 3 | Unavailable = stale-while-revalidate 全路径 | index.ts:198-206,283-291 | ①失败语义 |
| 4 | 渲染约束(非空 + 稳定序) | index.ts:309-312 + registry.ts:40 | ①确定性输出 |
| 5 | 指令域观察协议 + 三竞态保护 | instruction-context.ts:40-74 | ①A 档指令读取 |
| 6 | Registry 注册 + 稳定序组合 | registry.ts:25-44 | ①插件源扩展 |
| 7 | 内建源 + 技能/引用指导(权限过滤) | builtins.ts + skill/guidance.ts + reference/guidance.ts | ①对齐组合 |

## 面试弹药

- "reconcile 返回 exactly one action":四态代数,调用方零分支
- "惰性渲染":替换路径丢弃的更新渲染 0 次(测试证明)
- "空渲染是缺陷":render 空串 throw——防空白上下文污染模型
- "指令 = 一次性聚合源":全部 AGENTS.md 作为一个值比较,任何变化发完整替换消息——简单性优先
- "竞态保护":发现文件读前消失 → 整体 Unavailable,不渲染部分指令
- "Unavailable ≠ removed":临时失败保留旧值,真消失才发移除消息

## 待深挖

- [ ] skill/discovery.ts 的发现安全(isSafeSegment/isSafeRelativePath)——q14
- [ ] 技能工具(skill.ts 109 行)的权限检查路径
