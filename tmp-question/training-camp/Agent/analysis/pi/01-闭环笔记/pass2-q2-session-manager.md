# 闭环笔记 Q2:SessionManager — 会话存储与分支管理

> 域:SessionManager(session-manager.ts,1714 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

SessionManager 是 Pi 的会话持久层,用 JSONL 文件 + 树形条目组织会话历史。它的条目类型系统、压缩语义、分支机制是产品④(书级知识库)的核心蓝图。

## 验证过程

### 1. 会话文件模型:JSONL + 树形条目

`session-manager.ts:1044-1066` 验证:
```
_appendEntry: fileEntries.push + byId.set + leafId 推进 + _persist(JSONL 追加)
appendMessage: {type:"message", id, parentId: leafId, timestamp, message}
```

**关键设计**:
- **会话 = JSONL 文件**(每行一个条目,`:953` `${fileTimestamp}_${sessionId}.jsonl`)
- **树形结构**:每个条目有 parentId,指向当前 leaf;leaf 随 append 推进
- **目录按 cwd 编码**(`:476-480` `--${cwd}--`,每个工作目录一个会话目录)
- **版本迁移**(`:30` CURRENT_SESSION_VERSION = 3;`:282-295` migrateV1ToV2/V2ToV3 自动迁移 + _rewriteFile 重写)

**产品映射**:书级知识库 = 同样的 JSONL 追加模型,但目录按"书籍/章节"组织而非 cwd。版本迁移机制直接抄——知识库 schema 演进不丢历史。

### 2. 条目类型系统(产品④的核心数据结构)

`session-manager.ts:46-120` 验证,8 种条目:

| 类型 | 用途 | 参与 LLM 上下文? |
|------|------|:--:|
| message | 普通消息 | ✅ |
| thinking_level_change | 思考级别变更 | ✅(通过 context 推导) |
| model_change | 模型切换 | ✅ |
| compaction | 压缩摘要 | ✅(作为摘要) |
| branch_summary | 分支摘要 | ✅ |
| **custom** | 扩展自定义数据 | ❌ **不参与!**(:101-102) |
| label | 用户书签 | ❌(树节点但非消息) |
| session_info | 会话元数据 | ❌ |

**核心洞察(CustomEntry)**:
> "Purpose: Persist extension state across session reloads. On reload, extensions can scan entries for their customType and reconstruct internal state. Does NOT participate in LLM context."

**这是产品④的直接蓝图**:书级知识库的"结论/证据/章节结构"用 CustomEntry 存——**持久化但不污染 LLM 上下文**。上下文只装 compaction/branch_summary 摘要。

### 3. 压缩语义(buildContextEntries,420-454)— 核心算法

```
路径构建 → 找最近 compaction 条目
→ contextEntries = [compaction 摘要]
  + firstKeptEntryId 之后的旧条目(保留尾巴)
  + compaction 之后的新条目
```

**"摘要 + 保留尾巴 + 新增"三段式**——不是全删旧条目,是**保留 firstKeptEntryId 以后的"重要尾巴"**(通常是最近一次工具结果/用户指示)。

### 4. 会话上下文构建(buildSessionContext,461-470)

```
buildSessionPath(entries, leafId) → 从 leaf 走到 root 的路径
→ getSessionContextSettings 推导 thinkingLevel/model(遍历 path 的变更条目)
→ buildContextEntries().flatMap(sessionEntryToContextMessages)
```

**关键**:上下文从**树的路径**构建,不是全部条目——branch 场景下只有当前分支的路径进上下文。

### 5. 分支机制(1360-1505)— 会话时间线分叉

| 方法 | 行为 |
|------|------|
| branch(branchFromId) | 仅移动 leaf 指针到旧条目 → 后续 append 产生新分支(:1360-1365) |
| branchWithSummary | 分支 + 追加 branch_summary 条目(记录被放弃路径的摘要)(:1381-1405) |
| createBranchedSession | 提取 root→leaf 单一路径为新会话文件,label 重链(:1412-1505) |

**设计价值**:
- **分支 = "尝试不同方向"**:分析源码时走错路 → branch 回去 → 带摘要重来
- **createBranchedSession = "导出单线成果"**:从分叉的会话提取一条完整路径成为独立会话——**产品"每章独立成册"的机制**!

### 6. label 系统(110-115, 1223-1235)— 用户书签

```
LabelEntry: {type:"label", targetId, label}
appendLabelChange(targetId, label) → labelsById 映射
```

**设计价值**:给任意条目打标签("重要"/"结论"/"待验证")——**产品④"结论标记"的现成机制**,配合 createBranchedSession 的 label 重链,标签跨分支保留。

### 7. 静态工厂(1519-1593)— 会话恢复四模式

| 工厂 | 用途 |
|------|------|
| create | 新会话 |
| open(path) | 打开指定会话文件(支持 header 快速读取 + 大文件降级全读,1536-1544) |
| continueRecent | **自动找最近会话**(findMostRecentSession) |
| forkFrom | **跨项目 fork**(源会话全历史 → 新 cwd 新会话) |

**产品映射**:continueRecent = "上次分析到哪了,继续";forkFrom = "把这本书的分析迁移到新项目"。

### 8. 延迟落盘机制(_persist,1015-)— review 新增

```
_persist 行为:
- 没有 assistant 消息时:只攒内存,不写文件(flushed=false)
- 第一条 assistant 到达:全量写文件(flushed=true)
- 之后:增量 appendFileSync
```

**设计价值**:**"无效会话不落盘"**——用户问了一句话 agent 没回应(或会话失败),不产生垃圾文件。只有 agent 真正产出(assistant 消息)才持久化。
**产品映射**:书级知识库的"垃圾章节不落盘"——分析未产出的探索过程不写库,产出结论才写。

### 9. buildSessionPath 算法(验证)

```
从 leaf 出发沿 parentId 向上走到 root,reverse → 时间正序路径
leafId === null → 空路径(新会话)
无 leafId → 默认取最后一条
```

**验证结论**:路径 = 当前分支的时间线。原笔记"路径构建上下文(仅分支路径)"✅ 正确。

### 10. sessionEntryToContextMessages 映射(验证)

| 条目类型 | 转成消息 |
|---------|---------|
| message | 原样(但 **null content 容错** → content:[] ) |
| custom_message | createCustomMessage |
| branch_summary | createBranchSummaryMessage |
| compaction | createCompactionSummaryMessage |
| 其他(custom/label/session_info/thinking/model_change) | **空数组(不产生消息)** |

**review 发现补充**:
- **null content 容错**(session 文件手工编辑/旧版本/fork 可能损坏):`message.content == null → content: []`,不崩溃
- **thinking_level_change/model_change 不直接产生消息**——它们影响的是"状态"(context 推导),不是消息流

### 11. getTree 树构建与孤儿容错(1310-1348)— review 第二轮新增

```
getTree: entries → nodeMap(带解析后的 label)→ 按 parentId 建树
容错规则:
- parentId === null 或 parentId === entry.id(自引用)→ root
- parent 缺失(断链)→ 当 root 处理(孤儿不丢)
- 子节点按时间戳排序(迭代式,防深树栈溢出)
```

**设计价值**:
- **断链容错**:文件被手工编辑/部分损坏时,孤儿条目不丢,作为 root 保留——**知识库损坏时"内容不丢,只是结构变形"**
- **迭代排序防栈溢出**:深树(长会话)不崩溃——产品书库超长章节的安全保障

### 12. forkFrom 完整语义(1579-1630)— review 第二轮新增

```
forkFrom(source, targetCwd):
1. 读源会话全部条目
2. 写新 header:cwd 更新为 targetCwd,parentSession 指向源文件
3. 复制全部非 header 条目到新文件
```

**关键**:fork = **复制内容 + 换 header(cwd/父引用)**,不是移动。原会话不变,新会话独立演进。
**产品映射**:"把一本书的分析搬到另一个项目" = fork,源分析保留。

### 13. 会话列表(1638-1657)— review 第二轮新增

- `list(cwd)`:列出目录下会话,可按 cwd 过滤,按修改时间倒序
- `listAll()`:跨项目列全部
- **列表支持并发加载 + 进度回调**(onProgress)

**产品映射**:书库的"章节列表"——按修改时间排序 = "最近分析的章节",进度回调 = 大库加载的 UI 反馈。

### 14. 流式分块加载 + 头部快速读取(514-584)— review 第三轮新增

**loadEntriesFromFile**(514-556):
- 1MB buffer 分块读 + StringDecoder(处理多字节 UTF-8 跨块边界)
- 逐行 parseSessionEntryLine,**坏行跳过不崩溃**(503-511:try/catch JSON.parse 失败返回 null)
- 头部校验:首条必须是 session + id 字符串,否则返回空

**readSessionHeader**(571-584):
- 4KB 小 buffer + 1MB 扫描上限(MAX_SESSION_HEADER_SCAN_BYTES)
- **防恶意/超大头部**:超限抛 SessionHeaderScanLimitError,open() 降级全量加载

**设计价值**:
- **大文件读取的内存效率**(分块而非全读)
- **头部发现优化**:findMostRecentSession 只读头部不加载全文——**会话列表/最近会话 O(头部) 而非 O(全文)**
- 产品④"章节列表/最近章节"性能优化直接抄

### 15. findMostRecentSession 优化(635-659)— review 第三轮新增

```
readdir .jsonl → 每个文件只读 header(快速发现)
→ 按 cwd 过滤(header.cwd 匹配)
→ 按 mtime 排序取最新
```

**关键**:不加载任何完整文件,只读头部 + stat 元数据。会话很多时(上千个)依然快。
**产品映射**:书库"继续上次分析" = 同样的"只读 header 找最近"。

### 16. listSessionsFromDir 并发加载(811-842)— review 第三轮新增

- 并发加载所有 session 文件(buildSessionInfosWithConcurrency)
- 进度回调(loaded/total)
- 出错返回空列表(容错)

**产品映射**:章节列表的并发加载 + 进度 UI。

### 17. 类文档注释即架构总结(845-854)— review 第三轮新增

> "Manages conversation sessions as append-only trees stored in JSONL files. Each session entry has an id and parentId forming a tree structure. The 'leaf' pointer tracks the current position. Appending creates a child of the current leaf. Branching moves the leaf to an earlier entry, allowing new branches without modifying history. Use buildSessionContext() to get the resolved message list for the LLM, which handles compaction summaries and follows the path from root to current leaf."

**一句话设计哲学**:追加写树 + leaf 指针 + 分支不改历史 + buildSessionContext 处理压缩。

## 代码类型

Implementation(文件存储实现)

## 跨域关联

- ← 被依赖:AgentSession(所有 append 都走这里)、compaction/(压缩后写这里)
- → 依赖:config(会话目录)、messages(消息类型)

## 结论

SessionManager = **JSONL 文件 + 树形条目 + 分支 + label 的会话持久层**,核心可抄设计 17 个:
1. **JSONL 追加写 + 版本迁移** → 知识库存储基础
2. **8 种条目类型 + CustomEntry 不参与上下文** → 结论/证据/章节结构的存储
3. **压缩三段式语义(摘要+尾巴+新增)** → 上下文重建算法
4. **路径构建上下文(仅分支路径)** → 分支场景上下文
5. **分支三操作(branch/withSummary/createBranchedSession)** → "走错路回退 + 单线导出"
6. **label 书签系统** → 结论标记,跨分支保留
7. **四工厂模式(create/open/continueRecent/forkFrom)** → 会话恢复与跨项目迁移
8. **延迟落盘(无效会话不写文件)** → 垃圾章节不落库
9. **null content 容错** → 知识库文件损坏恢复
10. **getTree 孤儿容错(断链当 root)** → 内容不丢,结构变形
11. **迭代式排序防栈溢出** → 超长章节安全
12. **forkFrom 复制+换 header** → 分析迁移不破坏源
13. **会话列表(按修改时间/跨项目)** → 章节列表
14. **流式分块加载 + 坏行跳过** → 大文件内存效率 + 容错
15. **头部快速读取(4KB/1MB 上限)** → 列表/最近会话 O(头部)性能
16. **findMostRecentSession 只读 header** → "继续上次分析"快
17. **并发加载 + 进度回调** → 大库加载 UI

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| JSONL + 版本迁移 | ✅ 抄 | 知识库存储基础 |
| CustomEntry 不参与上下文 | ✅ 抄 | 结论/证据存储(持久化不污染上下文) |
| 压缩三段式(摘要+尾巴+新增) | ✅ 抄 | 章节上下文重建 |
| branch 三操作 | ✅ 抄 | "分析走错路回退 + 导出单章" |
| label 书签 | ✅ 抄 | 结论标记("结论/证据/待验证") |
| continueRecent | ✅ 抄 | "上次分析到哪,继续" |
| forkFrom | ✅ 抄 | 书籍分析跨项目迁移 |
| 延迟落盘 | ✅ 抄 | 无效探索不写库,产出才写 |
| null content 容错 | ✅ 抄 | 损坏恢复 |
| 孤儿容错 + 迭代排序 | ✅ 抄 | 库文件容错 + 超长安全 |
| 流式加载 + 头部快速读取 | ✅ 抄 | 大库性能(章节列表快) |
| 并发加载 + 进度 | ✅ 抄 | 大库 UI |
| 目录按 cwd 编码 | ⚠️ 改 | 改成按"书籍/章节"组织 |

## 面试问答弹药

- **Q**:会话怎么持久化?→ A:JSONL 追加写,每行一个条目,parentId 组成树
- **Q**:上下文重建时压缩怎么处理?→ A:三段式——压缩摘要 + firstKeptEntryId 后的尾巴 + 新增条目
- **Q**:agent 分析走错路了怎么办?→ A:branch 回退,分支摘要记录被放弃路径
- **Q**:扩展数据会不会污染上下文?→ A:CustomEntry 设计为不参与 LLM 上下文,只做持久化
- **Q**:会话怎么恢复?→ A:continueRecent 自动找最近会话;open 指定文件;forkFrom 跨项目迁移
- **Q**:无效会话会落盘吗?→ A:不会——第一条 assistant 消息前只攒内存,避免垃圾文件
- **Q**:会话文件损坏怎么办?→ A:null content 容错 + getTree 孤儿当 root——内容不丢,结构变形
- **Q**:会话多时列表性能?→ A:findMostRecentSession 只读 4KB header 不加载全文,按 mtime 排序
