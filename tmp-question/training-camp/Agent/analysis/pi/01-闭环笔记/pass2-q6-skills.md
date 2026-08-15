# 闭环笔记 Q6:Skills 系统(两层)

> 域:harness/skills.ts(375 行)+ core/skills.ts(487 行)+ system-prompt.ts(34 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

Pi 有"技能加载 + 提示词注入 + 调用包装"三层机制。技能 = SKILL.md 文件(frontmatter + 正文),加载后注入 system prompt 让模型按需调用。

## 验证过程

### 1. 技能文件格式(frontmatter + 正文)

```yaml
---
name: my-skill          # 可选,默认用父目录名
description: 必填,≤1024 字符
disable-model-invocation: true  # 可选,禁用模型自动调用
---
正文内容...
```

解析:parseFrontmatter(harness:380-394)——YAML frontmatter + 正文 body,失败返回诊断。

**校验规则**(validateName/validateDescription,harness:344-378):
```
name: 小写字母+数字+连字符 / 不能首尾连字符 / 不能连续 --
description: 必填 / ≤1024
```

**设计价值**:技能命名/描述有**硬校验**(符合 Agent Skills 规范 agentskills.io)——保证技能清单质量。产品"章节技能"同样校验。

### 2. 技能发现(loadSkills,harness:60-)

```
遍历目录:
- 递归找 SKILL.md(每个目录最多一个,找到即止:loadSkillsFromDirInternal:96-101)
- 目录根的 .md 文件也算技能(includeRootFiles)
- 尊重 ignore 文件(.gitignore/.ignore/.fdignore)
- 跳过 . 开头和 node_modules
```

**关键**:SKILL.md 规范(每目录一个)+ gitignore 尊重——**技能目录结构标准化**。

### 3. 多源合并与去重(core:loadSkills,387-487)

```
来源三档:
- user: ~/.pi/skills/(全局)
- project: .pi/skills/(项目)
- path: 显式指定路径

去重/冲突处理:
- symlink 去重(realPathSet + canonicalizePath)——同一文件经符号链接加载两次不重复
- 名称碰撞 → collision 诊断(含 winner/loser 路径)
```

**设计价值**:多源技能(用户全局/项目/显式)合并,冲突可诊断。**产品"书籍技能库 + 用户技能库"合并直接抄**。

### 4. 技能注入 system prompt(两层同构)

harness system-prompt.ts:1-33 / core formatSkillsForPrompt:335-366:

```
visibleSkills = 过滤 disableModelInvocation
输出 <available_skills> 清单:
  <skill><name><description><location>
外加指导:
  "Use the read tool to load a skill's file when the task matches"
  "Resolve relative paths against the skill directory"
```

**关键设计**:
- **清单注入 + 按需读取**:system prompt 只放名称/描述/位置(小),**内容不注入**——模型匹配描述后用 read 工具读完整文件
- **disableModelInvocation 过滤**:某些技能只给用户手动调用
- XML 转义(escapeXml)——技能描述含特殊字符不破坏格式

**产品映射**:产品"领域骨架清单"注入 system prompt(名称+描述),**内容按需读取**——不撑爆上下文。这直接回答"骨架库怎么进上下文"的问题。

### 5. 技能调用包装(formatSkillInvocation,harness:38-45)

```
<skill name="X" location="/path/SKILL.md">
References are relative to <skill 目录>.
<正文内容>
</skill>
```
- **location 必带**:模型知道从哪读
- **相对路径说明**:技能内引用相对于技能目录

### 6. 诊断体系(diagnostics)

```
类型: warning 级别,带稳定 code:
- file_info_failed / list_failed / read_failed / parse_failed / invalid_metadata
- collision(名称冲突,core 版)
坏文件不崩溃,进诊断列表。
```

**设计价值**:**软失败**——坏技能不影响其他技能加载,诊断可呈现给用户。

### 7. 技能调用入口(AgentSession.parseSkillBlock,agent-session.ts:129-138)

```ts
const match = text.match(/^<skill name="([^"]+)" location="([^"]+)">\n([\s\S]*?)\n<\/skill>(?:\n\n([\s\S]+))?$/);
```
用户消息里的 `<skill>` 块被解析(name/location/content/userMessage)。

### 8. 技能调用的两条路径(agent-session.ts:1309-1333)— review 新增

```
路径 1(用户主动):/skill:name args 命令
  → _expandSkillCommand(1309):
    资源加载器找技能 → readFileSync 读内容 → stripFrontmatter 剥离 YAML
    → 包成 <skill> 块 + 相对路径说明 + args → 进用户消息
  → 技能找不到 → 原文透传(不报错)
  → 读文件失败 → _extensionRunner.emitError(skill_expansion 事件,错误事件化)

路径 2(模型自动):system prompt 的 <available_skills> 清单
  → 模型用 read 工具读 SKILL.md → 内容进上下文
```

**关键设计**:
- **两条路径殊途同归**:都产出 `<skill name location>` 块格式 + frontmatter 剥离
- **失败软处理**:技能不存在透传原文;读失败事件化(不中断)
- **技能内容 = 运行时读取,不是常驻上下文**:用的时候才读文件

### 9. 技能块的 UI 渲染(interactive-mode.ts:3539-3556)— review 新增

```
parseSkillBlock 结果 → SkillInvocationMessageComponent(可折叠渲染)
  → userMessage 存在时:技能块 + 用户消息分开展示
```

**设计价值**:技能块和用户指令分离展示——用户能看到"哪部分是技能、哪部分是自己的话"。
**产品映射**:产品"分析指令 + 骨架引用"分离展示,用户可审计。

### 10. 技能块格式即调用协议(验证)

`<skill name location>` 块在整个系统里的多重用途:
- 用户消息里的主动调用(parseSkillBlock)
- /skill: 命令展开产物(_expandSkillCommand)
- export-html 导出渲染(template.js:648)
- interactive-mode 折叠展示

**一个格式,多方消费**——技能的"调用协议"统一。

### 11. 技能清单按工具集条件注入(system-prompt.ts:63-67)— review 第三轮新增

```ts
// Append skills section (only if read tool is available)
const customPromptHasRead = !selectedTools || selectedTools.includes("read");
if (customPromptHasRead && skills.length > 0) {
  prompt += formatSkillsForPrompt(skills);
}
```

**关键设计**:**没有 read 工具就不注入技能清单**——模型没 read 工具就没办法读 SKILL.md,注入清单没意义。**提示词内容与能力匹配**。
**产品映射**:产品"学习模式(有 read)注入骨架清单;无 read 不注入"——提示词与工具能力对齐。

### 12. system prompt 按工具集动态组装(system-prompt.ts:79-138)— review 第三轮新增

```
Available tools: 只列选中工具 + 各自 snippet(81-84)
Guidelines: 按工具组合生成(97-113)
  例如:只有 bash 无 grep/find/ls → 加"Use bash for file operations"
文档位置注入(131-138):pi 自己的 docs 路径
项目上下文(<project_context>):54-61
```

**核心设计**:
- **提示词 = f(工具集)**:换工具集 → 工具列表/guidelines 全变
- **文档"按需读"**:prompt 只放文档路径,模型需要时读
- **project_context 注入**:AGENTS.md 类项目说明,`<project_instructions path>` 格式

### 13. 技能来源追踪 + skillsOverride(core:136-148, resource-loader:176)— review 第三轮新增

```
createSkillSourceInfo:user/project/path 三级来源 → SourceInfo(scope 追踪)
resource-loader 的 skillsOverride:可完全替换技能加载逻辑(测试/定制)
```

**产品映射**:产品技能库来源溯源(用户库 vs 书籍库 vs 显式)+ 可覆盖加载。

## 代码类型

Implementation(文件加载 + 提示词生成)

## 跨域关联

- → 被依赖:AgentSession(解析 skill 块)、system-prompt 构建
- ← 依赖:yaml(解析)、ignore(gitignore)

## 结论

核心可抄设计 13 个:
1. **SKILL.md 文件格式(frontmatter+正文)+ 硬校验** → 产品技能/知识文件规范
2. **技能发现(递归 + ignore 尊重)** → 技能目录扫描
3. **多源合并 + symlink 去重 + 碰撞诊断** → 技能库合并
4. **清单注入 + 按需读取(内容不进 prompt)** → 骨架库/技能进上下文的方式
5. **disableModelInvocation 过滤** → 技能可见性控制
6. **调用包装(含 location + 相对路径说明)** → 技能调用协议
7. **软失败诊断体系** → 坏文件不阻塞
8. **双调用路径(/skill: 命令 + 模型自动 read)** → 用户/模型都能触发
9. **技能块格式统一(多方消费)** → 一个格式全链路复用
10. **技能内容运行时读取(不常驻上下文)** → 上下文节省
11. **技能清单按工具能力条件注入(无 read 不注入)** → 提示词与能力对齐
12. **system prompt 动态组装(工具集 → 工具列表/guidelines)** → 提示词=f(工具集)
13. **技能来源追踪(user/project/path)+ skillsOverride** → 溯源与可覆盖

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| SKILL.md 格式+校验 | ✅ 抄 | 产品"分析技能/章节规范文件" |
| 清单注入+按需读取 | ✅ 抄 | **领域骨架进上下文不撑爆** |
| 多源合并去重 | ✅ 抄 | 书籍技能库+用户库合并 |
| 调用包装 | ✅ 抄 | 技能调用协议 |
| 软失败诊断 | ✅ 抄 | 坏技能不影响分析 |
| 双调用路径 | ✅ 抄 | 用户 /skill: 显式 + 模型按需自动 |
| 块格式统一 | ✅ 抄 | 调用/渲染/导出同一协议 |
| 条件注入 | ✅ 抄 | 无 read 工具不注入技能清单 |
| 动态组装 | ✅ 抄 | 提示词随工具集变化 |

## 面试问答弹药

- **Q**:技能怎么组织?→ A:SKILL.md 文件(frontmatter 描述 + 正文),每目录一个,递归发现
- **Q**:技能怎么进上下文?→ A:清单注入(名称/描述/位置)+ 按需 read——内容不进 prompt
- **Q**:技能冲突怎么处理?→ A:多源合并 + symlink 去重 + 名称碰撞诊断(winner/loser)
- **Q**:所有技能模型都能调?→ A:不——disable-model-invocation 过滤,只给用户手动调
- **Q**:技能命名规范?→ A:小写字母+数字+连字符,description 必填 ≤1024,硬校验
- **Q**:技能怎么被调用?→ A:双路径——用户 /skill:name 展开;模型按 <available_skills> 清单用 read 工具自取
- **Q**:技能内容常驻上下文吗?→ A:不——只放清单,内容运行时读,省上下文
- **Q**:没有 read 工具时技能清单注入吗?→ A:不注入——模型没 read 就读不了技能,注入没意义
- **Q**:system prompt 怎么按工具集变化?→ A:Available tools 只列选中工具 + guidelines 按工具组合生成
