# MCP工具使用原则

> 角色：AI 自约束操作规范。域分析时选择工具必读。
> 先修：`01-三层循环框架.md` + `04-方案选择决策树.md`
> 输入：域的语言（Java/C/C++）、分析阶段（Pass 0-3）
> 阅读顺序：01 → 02 → 03 → 04 → 05（本文）→ 06。完整执行顺序见 01。

## 核心原则

**始终优先使用语义查询，而非文本模式匹配。** 理解代码语义的工具（类型层级、调用图、继承）比 grep 更准确，目标语言有可用语义工具时就该用它。

---

## 工具分类

### 语义层（理解代码结构）

| 工具 | 语言 | 核心能力 |
|------|------|---------|
| JavaLens | Java | type hierarchy、DI registrations、HTTP endpoints、JPA model、data flow、change impact（75 工具） |
| LSP MCP + clangd | C/C++ | go-to-definition、find references、call hierarchy、type hierarchy |
| ~~LSP MCP + jls~~ | Java | **不可用** — jls(Java Language Server)未安装；Java 降级链跳过 LSP MCP |

### 索引层（文本索引 + 图查询）

| 工具 | 语言 | 核心能力 |
|------|------|---------|
| codegraph | 21 种语言（含 C/C++） | search、callers、callees、impact、explore |
| codebase-memory-mcp | 66 种语言 | search_graph、query_graph、trace_path、search_code |
| ~~sverklo~~ | 多语言 | search、refs、overview — 可用但未纳入 Pass 分配表；结构查询优先用 codegraph |

**结构查询优先用 codegraph**（callers/callees/type hierarchy），codegraph 的图结构更准确更快。以下场景用 codebase-memory-mcp：（1）跨域大范围查询，（2）`trace_path` 追踪执行链，（3）`search_code` 做文本级 grep 式搜索。

**索引刷新**：两个工具都缓存数据。`git pull` 或切换分支后需重建索引。大仓库（>10 万行）在项目开始时索引一次，日常信任增量更新。

**重建索引命令**：
- codebase-memory-mcp：`mcp__codebase-memory-mcp__index_repository(project="<项目名>", path="<仓库路径>")`
- codegraph：`mcp__codegraph__codegraph_index(project="<项目名>")`

项目开始时索引一次即可，仅在 git pull 或分支切换后重建。

### 平台层（设计上下文 + 版本历史）

| 工具 | 范围 | 核心能力 |
|------|------|---------|
| GitHub MCP | GitHub 仓库 | search_issues、issue_read、pull_request_read、search_code |
| Git MCP | 本地仓库 | git_log、git_diff、git_blame、git_show |

### 专项工具

| 工具 | 范围 |
|------|------|
| Code Analysis Java/Spring | Spring 模式检测、架构分析 |

---

## 按 Pass 分配工具

### Pass 0：读代码前

| 步骤 | 工具 | 操作 |
|------|------|------|
| 设计文档 | GitHub MCP | `search_issues` 用域关键字搜索设计讨论 |
| PR | GitHub MCP | `pull_request_read` 读重大重构的 reviewer 评论 |
| Release notes | GitHub MCP | `get_latest_release` + 读 release 正文 |
| 历史版本 | Git MCP | `git_checkout` 回退到设计初期的 tag |

**规则**：优先用 GitHub MCP。只在仓库没有 GitHub 时用 `git log` 降级。

**无 GitHub 时的降级**：
| GitHub MCP 操作 | Git 降级替代 |
|-----------------|-------------|
| search_issues | `git log --grep="fix\|bug\|refactor\|issue"` — 搜索提交信息中的设计相关关键词 |
| pull_request_read | `git log --merges` — 读合并提交信息 |
| get_latest_release | `git tag --sort=version:refname \| tail -5` — 读标签信息 |

### Pass 1：扫轮廓

| 步骤 | Java | C/C++ |
|------|------|-------|
| 继承树 | JavaLens `get_type_hierarchy` | LSP MCP `type_hierarchy` |
| 调用图 | codegraph `callers` + `callees` | codegraph `callers` + `callees` |
| DI/端点（Spring） | A/B 专用：JavaLens `get_di_registrations` + `get_http_endpoints` | 不适用 |
| 代码搜索 | codebase-memory-mcp `search_code` | codebase-memory-mcp `search_code` |

**D 方案（简略 Pass 1）**：只用继承树和代码搜索工具。跳过 DI/端点扫描和标记问题生成。

**C 方案**：完整 Pass 1（全工具），然后简略 Pass 2 只答 1-2 个问题。

**降级（Java）**：JavaLens → codegraph → grep。**C/C++**：LSP MCP → codegraph → grep。

### Pass 2：盯关键点

| 步骤 | Java | C/C++ |
|------|------|-------|
| 追踪执行路径 | JavaLens `analyze_data_flow` | LSP MCP `call_hierarchy` |
| 追踪调用链 | codebase-memory-mcp `trace_path` | codebase-memory-mcp `trace_path` |
| 时空溯源 | **[A 专用]** Git MCP `git_log` + `git_diff`（早期 tag vs 当前 tag） | **[A 专用]** 同左 |
| 变更影响面 | JavaLens `analyze_change_impact` | codebase-memory-mcp `query_graph` |
| 代码归属 | Git MCP `git_blame` | 同左 |
| 跳转定义 | codegraph `search` → Read 工具 | LSP MCP `go_to_definition` |

**规则**：数据流和影响分析用语义工具（JavaLens/LSP）。版本历史用 Git MCP。**禁止用 grep 追踪执行路径**——grep 会漏掉间接调用和多态分派。Java 跳转定义（jls 未安装）：用 codegraph `search` 定位目标类，再用 Read 直接阅读。

### Pass 3：收尾

| 步骤 | Java | C/C++ |
|------|------|-------|
| 跨域引用 | codebase-memory-mcp `query_graph` | 同左 |
| 复杂度度量 | **[A 专用]** JavaLens `get_complexity_metrics` | 不适用 |
| 循环依赖检测 | **[A 专用]** JavaLens `find_circular_dependencies` | **[A 专用]** codebase-memory-mcp `query_graph` |
| 架构总览 | **[A/B 专用]** Code Analysis `ArchitectureAnalyzer`（Spring） | **[A/B 专用]** codebase-memory-mcp `get_architecture` |

---

## 降级链

### Java
```
JavaLens → codegraph → codebase-memory-mcp → grep
```
注：Java 跳过 LSP MCP — jls (Java Language Server) 未安装。

### C/C++
```
LSP MCP (clangd) → codegraph → codebase-memory-mcp → grep
```

**禁止跳级。** 高层工具失败用下一层，不得在所有语义和索引工具可用时直接跳到 grep。

如果某一层缺少特定能力（如 type hierarchy），用下一层的结构近似替代（如用 grep 类声明推断继承），或在输出中标注 `[不完整：type hierarchy 不可用]`。

---

## 反模式

### 1. 用 grep 做结构化查询

```
错误：grep -rn "implements ChannelHandler"  → 漏掉匿名类、间接实现
正确：JavaLens find_implementations("ChannelHandler") → 编译器精确
```

### 2. 用 grep 查调用链

```
错误：grep "methodName" → 匹配到文本出现，不是实际调用
正确：codegraph callers → 基于图解析的调用关系
```

### 3. 不做 Pass 0 就直接读源码

```
错误：直接开始读源码，不知道设计背景
正确：GitHub MCP search_issues → 理解"为什么要这样写" → 再读源码
```

### 4. 用 JavaLens 分析 C/C++，或对 Java 用 LSP MCP

```
错误：对 Redis 源码用 JavaLens → JavaLens 只支持 Java
错误：对 Java 用 LSP MCP → jls (Java Language Server) 未安装
正确：对 Redis（C 代码）用 LSP MCP + clangd；Java 降级用 JavaLens → codegraph
```

---

## 域分析前工具检查

开始分析前，输出此块：

```
## 工具可用性：{域名}

语言：{Java/C/C++}
语义工具：{JavaLens / LSP MCP+clangd}
索引工具：{codegraph（优先）/ codebase-memory-mcp}
Pass 0 就绪：GitHub MCP {是/否 — 需 GITHUB_PERSONAL_ACCESS_TOKEN}
时空溯源：Git MCP {是/否}
降级链：{JavaLens→codegraph→grep / LSP+clangd→codegraph→grep}
索引状态：{新 / 过期 — 上次索引之后仓库有更新则重建}
```
