# q47 — core effect 节点机制(深度版:LayerNode 依赖树 + 双 tag + service-use)

> 域:共享基础设施(架构核心) | 文件:core/src/effect/(layer-node.ts 333/app-node.ts 14/app-node-builder.ts 23/app-node-platform.ts 18/service-use.ts 43/memo-map.ts 3/runtime.ts 21)
> review 轮次:2 轮(源码全文)

---

## 假设

LayerNode = "声明式服务依赖树":每个服务是一个 Node(实现 + 依赖 + tag),编译成 Effect Layer。类型级检查(缺依赖/错 tag 编译期报错)+ 运行时环检测 + replacement/hoist 裁剪。这是 34 个 Location 服务 group(q32)和全局服务的共同骨架。

## 验证

### 1. Node 模型(设计 1:三 kind + 类型级检查)

```ts
// layer-node.ts:22-31
Node = { kind: "layer"|"unbound"|"group", name, service?, implementation?, dependencies, tag? }
// make(layer-node.ts:81-96):layer 节点(服务实现 + deps)
// unbound(98-106):未绑定(服务由外部提供,如 LocationServiceMap)
// group(108-112):依赖集合(组合)
// 类型级检查:CheckDependencies(Missing<Layer.Services, Dependencies> → 编译期错误 "Missing dependencies")
//   CheckTags("Invalid tag dependencies")——依赖必须是正确 tag 的节点
```

### 2. 双 tag(设计 2:global/location)

```ts
// app-node.ts:3-12
tags = LayerNode.tags({ location: ["global"], global: [] })
makeGlobalNode = tags.make("global") / makeLocationNode = tags.make("location")
// 语义:location 节点可以依赖 global(声明 "location: [global]");global 节点不能依赖 location
// —— 依赖方向强制:Location 服务可依赖全局,反之不行(架构纪律在类型层)
```

### 3. hoist(设计 3:tag 裁剪)

```ts
// layer-node.ts:211-248
hoist(root, tag, replacements?):
- walk 依赖树:遇 tag 匹配的节点 → 移到 hoisted 集合(替换为空 group)
- 同名多实现 → "Tag has conflicting implementations"
- 返回 { node(裁剪后), hoisted(被移出的节点集合) }
// 用途(q32):Location 服务的 global 依赖被 hoist 到共享层,每个 Location 只编译自己的部分
//   ——"hoist walk is the only pass that can still slice those back out"
```

### 4. compile(设计 4:递归提供)

```ts
// layer-node.ts:250-272
compile(root, replacements?):
walk:unbound → throw("Unbound layer node")
dependencies → implementation.provide(deps)
flatten 后 reduce(provideMerge)——缓存(cache Map,环检测)
// replacement:名称+tag 必须匹配("Cannot replace X across tags")
```

### 5. service-use(设计 5:惰性 Proxy 访问器)

```ts
// service-use.ts:17-42
serviceUse(Service) → Proxy(惰性缓存每个方法的访问器)
访问器:tag.use(service => service[key](...args))——调用时解析服务
// 类型:ServiceUse 只暴露 Effect 返回的方法(编译期)
// 用途:V1 服务的便捷访问(Agent.use/LSP.use 等)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Node 三 kind + 类型级依赖检查 | layer-node.ts:22-112 | ②声明式组装 |
| 2 | 双 tag(global/location 依赖方向强制) | app-node.ts:3-12 | ②架构纪律类型化 |
| 3 | hoist 裁剪(共享层提取) | layer-node.ts:211-248 | ②性能(Location 编译) |
| 4 | compile + replacement(名称+tag 校验) | layer-node.ts:250-272 | ②替换安全 |
| 5 | service-use 惰性 Proxy | service-use.ts:17-42 | ②服务访问 |

## 面试弹药

- "架构纪律在类型层":location 节点可依赖 global,反之编译期报错——依赖方向不靠自觉
- "hoist 是性能关键":Location 服务的 global 依赖一次编译共享,每 Location 只编译自己部分
- "replacement 跨 tag 拒绝":替换必须同 name 同 tag——防错配
- "环检测在 walk":运行时 Cycle detected(编译期类型检查覆盖不到的部分)

## 待深挖

- [ ] app-node-builder.ts(测试构建器)
- [ ] app-node-platform.ts(llmClient/httpClient 平台节点)
- [ ] memo-map/runtime(编译缓存)
