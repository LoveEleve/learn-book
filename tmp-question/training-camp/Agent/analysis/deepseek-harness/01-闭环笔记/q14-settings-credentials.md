# q14 — Settings/Credentials/Identity/Session-Title(深度版:会话支持域)

> 域:①对齐(设置/凭证)+ ④知识库(标题) | 文件:packages/(settings/settings)+(credentials/credentials + credentials-env + credentials-file)+(identity/anonymous-user-id)+ session/(session-title + session-title-llm + session-title-first-prompt-llm + session-title-all-prompts-llm)
> review 轮次:2 轮(源码全文核心)

---

## 假设

四支持域:S组成设置(命名空间 + 作用域注册 + 事件)、凭证(4 操作 + 空值即缺席规则)、身份(匿名)、标题(sole provider 契约 + LLM 变体)。

## 验证

### 1. Settings(设计 1:命名空间 + 事件)

```ts
// settings/settings/src/index.ts:
settingsNamespace(value):品牌化命名空间(settingsNamespace('permission') 等)
SettingsRegisterOptions/Descriptor/Scope:注册/描述/作用域
deepEqualJson(a, b):JSON 深度相等(变更检测)
事件:
'settings/updated'(ns, next, prev, source):设置更新(源可追溯)
'settings/document-updated'(ns, revision):文档更新
installSettingsSection:安装设置段(UI 呈现)
// ——permission-presets 用 settingsNamespace('permission')(q9)
```

### 2. Credentials(设计 2:4 操作 + 空值即缺席)

```ts
// credentials/credentials/src/index.ts:28-62:
ResolvedCredential = { value(非空), source(local provider: env/file/project-env/user-env) }
CredentialInfo = { configured, source?, writable }——UI 安全事实,永不含值
CredentialProvider 抽象:4 操作(resolve/describe/set/...)
"one seam-wide rule binds them all:an empty stored value is absent everywhere
 ——resolve skips it,describe reports it unconfigured"
  ——"a blank never masquerades as a configured secret"
'credentials/updated'(ref):更新事件(监听失败日志不崩溃)
```

**产品启示**:④知识库凭证安全——"空值即缺席"防空白冒充配置的秘密(与 OpenCode 的 0600/环境注入互补)。

### 3. Identity(设计 3:匿名)

```ts
// identity/anonymous-user-id:匿名用户 ID(遥测/统计归属,不追踪真实身份)
```

### 4. Session-Title(设计 4:sole provider + LLM 变体)

```ts
// session/session-title:
SessionTitleProviderRequest(126)/ SessionTitleUserMessage(115)/ SessionTitleSnapshot(71)/
  SessionTitleModelProvenance(40)/ Config
// session-title-llm + first-prompt-llm + all-prompts-llm:LLM 标题生成变体
//   (首 prompt vs 全部 prompt)
// architecture.md:124:"Generate session titles:register the sole ctx.sessionTitle provider"
//   ——sole provider 契约(唯一注册者)
// session-title/client.ts + normalize.ts:客户端 + 归一化
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Settings 命名空间 + 事件(源可追溯) | settings/settings | ①配置 |
| 2 | Credentials 4 操作 + 空值即缺席 | credentials | ④凭证安全 |
| 3 | 匿名身份 | identity | ④遥测归属 |
| 4 | Session-Title sole provider + LLM 变体 | session/session-title* | ④标题生成 |

## 面试弹药

- "空值即缺席":空白永不当配置的秘密——安全规则绑定所有 provider(缝级)
- "settings 事件源可追溯":settings/updated 带 prev/next/source——谁改了什么可审计
- "sole provider 契约":标题唯一注册者——架构级"只有一个实现"的约定
- "CredentialInfo 不含值":UI 只看到 configured/source/writable——值永不出服务

## 待深挖

- [ ] credentials 的 local provider 层(env/file/project-env/user-env)
- [ ] settings 的作用域注册语义(agent 级?)
- [ ] session-title 的 LLM 变体差异
