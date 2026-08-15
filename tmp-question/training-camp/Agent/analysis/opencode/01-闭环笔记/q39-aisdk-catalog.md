# q39 — AISDK 兼容层 + Catalog(深度版:钩子链 + 缓存 + 请求体修复)

> 域:②底层(兼容层) | 文件:core/src/aisdk.ts(235)+ catalog.ts(301)+ core/test/(catalog/models).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

AISDK = "AI SDK 供应商的插件化加载器":钩子链(sdk/language 两阶段)让插件注入供应商实现,带缓存(按 provider/model/variant)。fetch 包装负责超时/中断聚合/请求体修复。Catalog 是模型目录(政策过滤)。

## 验证

### 1. 钩子链(设计 1:sdk → language 两阶段)

```ts
// aisdk.ts:130-179
hook.sdk(callback):注册 SDK 提供者(插件调用)——Scope 关闭自动移除(active flag + filter)
hook.language(callback):注册语言模型包装器
runSDK(event):顺序跑 sdk 钩子(每个可改 event.sdk)
runLanguage(event):顺序跑 language 钩子(可改 event.language)
// language(model):缓存查找 → prepareOptions → runSDK → runLanguage → sdk.languageModel
// 缓存:languages Map(key = providerID/modelID/variant);sdks Map(key = JSON.stringify(api+options))
```

**设计要点**:供应商实现 = 插件注册的钩子(不是内置);Scope 作用域注册(卸载自动清理)——与 State.transform 同模式。

### 2. fetch 包装(设计 2:超时 + 中断聚合 + 请求体修复)

```ts
// aisdk.ts:74-119
prepareOptions:name/providerID + api.settings + request.body + baseURL + customFetch
fetch 包装:
- 信号聚合:caller signal + chunkTimeout AbortController + options.timeout(AbortSignal.timeout)→ AbortSignal.any
- 请求体修复:openai/azure/bedrock 且 body.store !== true → 删除 input item 的 id 字段
  (防缓存 key 污染——请求体带 id 会破坏 provider 缓存)
- wrapSSE:chunkTimeout → SSE 每次 read 限时(超时 abort + cancel)
```

**设计要点**:id 删除是"provider 缓存正确性"的细节(带 id 的 input 破坏 prompt cache);chunkTimeout 是流式读的超时粒度(不是总超时)。

### 3. Catalog(设计 3:模型目录 + 政策)

```ts
// catalog.ts:20-62
PolicyActions = ["provider.use"](政策动作)
Service:模型目录查询/解析(带政策过滤)
// deps:EventV2 + Policy + Integration
// 与 SessionRunnerModel 关系:runner 用 Catalog 解析会话模型(q23)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 钩子链两阶段(sdk/language)+ Scope 注册 | aisdk.ts:130-179 | ②供应商插件化 |
| 2 | 双缓存(languages/sdks 按模型+选项) | aisdk.ts:151-167 | ②初始化去重 |
| 3 | fetch 包装(信号聚合/chunk 超时/请求体修复) | aisdk.ts:74-119 | ②网络健壮性 |
| 4 | Catalog 政策过滤(provider.use) | catalog.ts:20-62 | ②模型访问控制 |

## 面试弹药

- "供应商 = 钩子不是内置":插件注册 sdk/language 提供者,Scope 关闭自动卸载——可扩展不侵入
- "删除 input.id 保缓存":请求体带 id 破坏 provider prompt cache——细节决定成本
- "chunkTimeout 是流式粒度":SSE 每次 read 限时(不是总超时)——流卡住的检测
- "双缓存":SDK 按 api+options 缓存,language 按 provider/model/variant——初始化一次

## 待深挖

- [ ] catalog.ts 的模型解析细节(Generation Controls 分区)
- [ ] models-dev.ts 的目录同步
- [ ] 钩子注册的插件调用点(plugin 域)
