# q46 — Provider/Model + 外围域(深度版:transform 降级 + 工具面)

> 域:②底层 + 支撑 | 文件:opencode/src/provider/(provider 2011/transform 1858)+ core/src/(provider 25/model 41/integration 520/npm 269/background-job 365)+ opencode/src/share/(share-next 371/session 58)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Provider = 供应商配置(AISDK/native 双 API 描述);transform = 供应商降级转换(参数/消息/JSON schema 兼容);外围域:Integration(集成认证)/NPM(依赖安装)/BackgroundJob(后台任务)/Share(分享)。

## 验证

### 1. Provider 模型(设计 1:AISDK vs Native)

```ts
// core/provider.ts:6-25:ID(opencode/anthropic/openai/google/bedrock/azure/openrouter... 品牌常量)
// schema/provider.ts:AISDK{type, package, url, settings} | Native{...}(双 API 描述)
// transform.ts(1858):供应商转换——message/temperature/topP/topK/variants/options/providerOptions/maxOutputTokens/schema
//   OUTPUT_TOKEN_MAX 32k(transform.ts:18)
//   sanitizeSurrogates:代理对清理(截断安全)
//   maxOutputTokens = min(model.limit.output, 32k)
```

### 2. Schema 降级(设计 2:OpenAI 兼容)

```ts
// transform.ts:1425-1470 sanitizeOpenAISchema:
// "Mirrors Codex's Rust JSON schema compatibility lowering for OpenAI tool schemas"
boolean schema → { type: "string" }(OpenAI 不支持布尔形式)
$ref/description/enum(const→enum)/properties/required/items/additionalProperties/composition(anyOf 等)递归清理
// 用途:工具参数 schema → OpenAI 兼容(provider 差异集中处理)
```

### 3. Integration(设计 3:认证方法族)

```ts
// integration.ts(520 行):ID/MethodID/AttemptID/When(时机)
Method = OAuthMethod | KeyMethod | EnvMethod | TextPrompt | SelectPrompt
// 集成认证:多种方法(oauth/key/env/提示)统一模型
// credential.ts 的 integrationID 绑定(q41)
```

### 4. NPM(设计 4:包安装 + 安全)

```ts
// npm.ts:45-70
sanitize(pkg):非法字符 → "_"(包名净化)
InstallFailedError;resolveEntryPoint(Bun import.meta.resolve)
ArboristNode/Tree:依赖树解析(插件安装依赖)
// 插件安装:install.ts 用它装插件包(q24)
```

### 5. BackgroundJob(设计 5:后台任务生命周期)

```ts
// background-job.ts:20-99
Active{info, done, scope, token, pending, next, output{sequence,text}, tail, promoted, onPromote}
State{jobs: SynchronizedRef<Map<string, Active>>}
Start/Extend(输出追加)/Finish/Promote(提升为前台?)
// 用途:bash 后台任务/长任务(结构:输出分片 sequence + tail 等待)
```

### 6. Share(设计 6:会话分享)

```ts
// share-next.ts(371):分享会话(公开只读视图)
// share/session.ts(58):分享会话操作
// SessionTable.share_url(q10)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Provider 双 API(AISDK/Native)+ 品牌 ID | core/provider.ts + schema/provider.ts | ②供应商抽象 |
| 2 | Schema 降级(OpenAI 兼容,Codex 镜像) | transform.ts:1425-1470 | ②兼容层 |
| 3 | Integration 认证方法族 | integration.ts | ②第三方认证 |
| 4 | NPM 安全安装(sanitize) | npm.ts:45-70 | ②依赖管理 |
| 5 | BackgroundJob(输出分片+提升) | background-job.ts | ②后台任务 |
| 6 | Share 会话分享 | share-next.ts | ④分享 |

## 面试弹药

- "供应商差异集中降级":sanitizeOpenAISchema 一处处理 OpenAI 兼容(boolean→string 等)——新增 provider 的适配点
- "包名净化防注入":sanitize 非法字符→_——依赖安装安全
- "后台任务输出分片":output{sequence, text} + tail——流式输出可追溯
- "集成认证方法族":oauth/key/env/prompt 统一模型——认证方式可插拔

## 待深挖

- [ ] transform 的 variants/providerOptions 细节
- [ ] integration 的 OAuth 流实现
- [ ] background-job 的 promote 语义
