# q6 — Profile/Bundle 组合(深度版:patch 覆盖 + 层序)

> 域:①对齐(组合配置)+②执行(装配) | 文件:packages/boot/app-boot/src/(index 829/profile 420)+ bundle/(base/headless/web-app)+ docs/architecture.md §Profiles
> review 轮次:2 轮(源码全文核心)

---

## 假设

运行中的 dsh = 引导时从有序层组合的插件树。Profile = 命名组合(存 Harness home,列 bundle + 用户 cordis.patch.yml);Bundle = 分发格式(patch 文件 + 代码)。patch 按行 id 覆盖(最后写赢),**替换整行 config 不合并**。

## 验证

### 1. 层序(设计 1:六层)

```ts
// architecture.md:27:
空 entry 列表 → 每 profile 列出的 bundle(按序)→ profile 的 cordis.patch.yml
→ home 级 patch → 任何 --patch overlay
// patch 目标行:按 id 替换整个 config,或插入新行
// dsh-base = 每 profile 第一层(模型适配器/工具/持久化/沙箱+审批策略/设置/凭证/遥测)
// dsh-web-app 加浏览器应用;dsh-headless = 无服务器的一次性 runner
// PROFILE_TEMPLATES(profile.ts:114):web/headless 模板
// DEFAULT_PROFILE_BUNDLES = ['@deepseek-ai/dsh-base']
```

### 2. Patch 语义(设计 2:文件格式 + 失败策略)

```ts
// app-boot/src/index.ts:280-340:
patch 文件 = top-level YAML 数组(PatchOptions:按 id 覆盖 + insert 列表;!!js 表达式允许)
loadOptionalPatches:文件缺失 → undefined(可选)
loadOverlayPatches:文件缺失 → throw("caller named this file — its absence is a misconfiguration")
parsePatchList:非数组/非 mapping → throw;无效字段/值 → throw(不能应用的 patch = 配置错误)
单 patch 目标行缺失 → per-entry Loader warning
  ——"one overlay shared across surfaces does not have to match every tree"
```

**设计要点**:patch 文件级缺失 = 硬错(用户点名了);行级缺失 = 警告(共享 overlay 宽容)。失败策略按"谁犯了错"区分。

### 3. 行级语义(设计 3:最后写赢 + 整行替换)

```ts
// bundle/base/cordis.patch.yml 注释:
"applied as ONE insert over the empty profile root.Later bundle patches and the
 user's profile cordis.patch.yml address these rows by id,with the last write winning per row"
"A patch replaces the targeted row's whole config rather than merging into it"
// 模式特定行不在此(每模式 bundle 各自完整重述配置,单一 bundle 层 + 用户)
// 行序无加载语义(激活是服务可用性驱动;分组仅为读者)
```

**产品启示**:①对齐模块的"配置覆盖"模型——用户改规格书 = 顶层 patch 覆盖(最后写赢),与 OpenCode 的配置三来源合并互补(这里更强:整行替换防漂移)。

### 4. 调试(设计 4:dump-config)

```ts
// architecture.md:31-36:
dsh --profile web --dump-config → 打印实际引导的树
任何打印的行可被自己的 patch 替换——"see the tree your machine actually boots"
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 六层层序(bundle→profile→home→overlay) | architecture.md:27 | ①配置覆盖模型 |
| 2 | Patch 失败策略(文件硬错/行级警告) | app-boot/index.ts:280-340 | ②失败语义 |
| 3 | 最后写赢 + 整行替换 | bundle/base/cordis.patch.yml | ①防漂移 |
| 4 | dump-config 可调试 | architecture.md:31-36 | ②可观测 |

## 面试弹药

- "整行替换不合并":patch 替换行 config——防漂移(与 OpenCode 配置合并互补)
- "文件缺失硬错,行缺失警告":失败策略按"谁犯错"区分(点名 vs 共享)——配置错误可诊断
- "一行一个 bundle 层":模式特定配置不共享(每模式完整重述)——单层责任
- "行序无语义":激活是服务可用性驱动——加载顺序自由

## 待深挖

- [ ] profile.ts 的模板/解析细节
- [ ] loader 的挂载决策(服务可用性驱动)
- [ ] headless 与 web-app bundle 的差异
