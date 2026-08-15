# q26 — 剩余内置工具(深度版:权限差异面)

> 域:②执行(工具) | 文件:core/src/tool/(grep/glob/edit/webfetch/websearch/skill/todowrite/question)+ core/test/(tool-grep/tool-glob/tool-edit/tool-webfetch/tool-websearch/tool-skill/tool-todowrite/tool-question).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

12 个内置工具共享统一权限模板,但"资源模式"按工具语义分化:文件系走 LocationMutation(路径安全),搜索系直接断言 pattern(字符串即资源),会话系断言固定资源("*" 或会话内 ID)。这反映了权限模型的表达力边界。

## 验证

### 1. 权限资源模式矩阵(设计 1:三类资源)

```ts
// 文件系(路径安全):read/write/edit/apply_patch
//   → mutation.resolve → resource = Location 相对或 canonical 绝对
// 搜索系(字符串即资源):grep/glob/webfetch/websearch
//   → 直接断言 action, resources=[pattern/query/url], save=["*"]
//   示例 grep(grep.ts:81-86):assert({ action: "grep", resources: [input.pattern], save: ["*"], metadata: {root, path, include, limit} })
// 会话系(固定/会话内):skill/todowrite/question
//   skill:resources=[skill.name] save=[skill.name](技能级批准)
//   todowrite:resources=["*"] save=["*"](无条件)
//   question:action="question"
```

### 2. edit(设计 2:字符串替换 + CAS)

```ts
// edit.ts:127-189
- oldString === newString → 拒绝("No changes to apply")
- oldString === "" → 拒绝("Use write to create or overwrite")
- 行结束符检测/转换(CRLF 兼容:detectLineEnding/convertToLineEnding)
- 多重匹配 → 拒绝("Provide more surrounding context or set replaceAll")
- StaleContentError → "File changed after permission approval. Read it again before editing."(CAS 语义)
// withPermission(edit):与 write/apply_patch 共享 "edit" action
```

**设计要点**:edit 是"精确字符串替换 + CAS 校验"——比 apply_patch 更保守(一次一处,除非 replaceAll)。

### 3. skill(设计 3:正文按需加载 + 文件限制)

```ts
// skill.ts:65-109
1. skills.list() 查找(name 精确)
2. permission.assert(action "skill", resources=[skill.name], save=[skill.name])
3. SKILL.md 目录 → fs.glob("**/*") 排除 SKILL.md,排序,slice(FILE_LIMIT)
// 输出:name/directory/output(正文文件列表)
// 设计要点:技能正文不注入系统上下文,只在调用工具时按需加载(与 guidance 只列名+描述呼应)
```

### 4. websearch/webfetch(设计 4:URL/查询即资源)

```ts
// webfetch.ts:126-139 / websearch.ts:201-210
assert({ action: name, resources: [url/query], save: ["*"] })
// webfetch:URL 抓取(内容提取/限制);websearch:搜索 API
// TODO 注:媒体/规范化处理
```

### 5. todowrite/question(设计 5:会话内工具)

```ts
// todowrite.ts:33-46:todos.update({ sessionID, todos })(无条件权限 + 会话作用域)
// question.ts:action "question"(向用户提问,答案进上下文)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 三类权限资源模式(文件系/搜索系/会话系) | 各 leaf | ②权限模型表达力 |
| 2 | edit 精确替换 + CRLF 兼容 + CAS 错误消息 | edit.ts:127-189 | ②编辑安全 |
| 3 | skill 正文按需加载 + FILE_LIMIT | skill.ts:65-109 | ②技能正文暴露面 |
| 4 | URL/查询即资源 | webfetch/websearch | ②网络权限 |
| 5 | 会话内工具(无条件) | todowrite/question | ②轻量工具 |

## 面试弹药

- "资源模式 = 权限表达力":文件=路径、搜索=字符串、会话=固定——权限模型适配工具语义,不强行统一
- "edit 拒绝模糊":多重匹配拒绝(不猜)、空 oldString 拒绝(引导 write)、CRLF 兼容——替换精确性优先
- "skill 正文不注入":guidance 只列名,正文 glob 按需加载且限制数量——最小暴露面
- "save 按工具语义":grep save["*"]、skill save[skill.name]、todowrite save["*"]——批准粒度不同

## 待深挖

- [ ] webfetch 的内容提取/大小限制细节
- [ ] question 工具的答案如何进上下文(QuestionV2 集成)
- [ ] websearch 的提供商抽象
