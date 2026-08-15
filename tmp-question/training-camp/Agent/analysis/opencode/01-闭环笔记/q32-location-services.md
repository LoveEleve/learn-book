# q32 — Location 服务路由(深度版:LayerMap 按目录实例化)

> 域:共享基础设施 | 文件:core/src/location-services.ts(115)+ location-service-map.ts(18)+ effect/(app-node.ts/layer-node.ts)+ core/test/(location-layer/location).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

"Location 作用域"是 OpenCode 的核心架构决策:34 个服务按目录实例化(每个打开的项目一套)。LocationServiceMap = 按 Location.Ref 惰性创建/回收服务集的 LayerMap——这就是"多目录互不干扰"的实现。

## 验证

### 1. 34 服务 group(设计 1:locationServices)

```ts
// location-services.ts:42-79
locationServices = LayerNode.group([Location/Policy/Config/AgentV2/Command/Reference/Integration/Catalog/
  AISDK/Plugin/ProjectCopy/FileSystem/Watcher/Pty/Skill/SystemContextRegistry/BuiltIns/LocationMutation/
  FileMutation/PermissionV2/ToolOutputStore/ToolRegistry/Image/SkillGuidance/ReferenceGuidance/SessionTodo/
  QuestionV2/ReadToolFileSystem/BuiltInTools/SessionRunnerModel/Snapshot/SessionRunnerLLM])
// 排除:EventV2/Database/SessionExecution/RunCoordinator(process-global)
// 结论:permission/tool/config/system-context 等都是 Location 级;事件/数据库/执行协调是全局级
```

### 2. LayerMap(设计 2:按 ref 惰性实例化 + 回收)

```ts
// location-services.ts:84-112
LayerMap.make((ref) => {
  const location = LayerNode.hoist(locationServices, global, [[Location.node, Location.boundNode(ref)]])
  return LayerNode.compile(location.node) + fresh + tap(启动日志) + provide(hoisted)
}, { idleTimeToLive: "60 minutes" })
// 每次 Location 打开:全量编译该 Location 的服务层(hoist 裁剪全局依赖)
// 60 分钟空闲回收——"每个打开的项目一套服务,自动清理"(InstanceState 同哲学)
```

### 3. 路由(设计 3:get/静态 get)

```ts
// location-service-map.ts:7-14
Service = LayerMap<Location.Ref, LocationServices>
static get(ref) → Layer.unwrap(Service.map(locations => locations.get(ref)))
// runner 用:drain 时 Effect.provide(locations.get(session.location))(q11)
```

### 4. 节点构建(设计 4:app-node/layer-node)

```ts
// effect/app-node.ts:Node(global/location 双 tag)+ makeLocationNode/makeGlobalNode/makeLocationNode 工厂
// effect/layer-node.ts:LayerNode(依赖声明 + 构建 + hoist)——服务组装的声明式 DSL
// replacement 语义:hoist 时应用(Location.boundNode 引入新依赖 Project,hoist walk 才能裁剪)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 34 服务 group(Location 级 vs 全局级分离) | location-services.ts:42-79 | ②作用域架构 |
| 2 | LayerMap 惰性实例化 + 60 分钟回收 | location-services.ts:84-112 | ②多目录隔离 |
| 3 | get(ref) 路由 + runner 注入 | location-service-map.ts:7-14 | ②服务路由 |
| 4 | Node/LayerNode 声明式组装 + hoist 裁剪 | effect/* | ②依赖管理 |

## 面试弹药

- "Location 级 vs 全局级":权限/工具/配置按目录实例化,事件/数据库/执行全局——作用域划分是架构决策
- "LayerMap 惰性 + 回收":打开项目才编译服务,60 分钟空闲回收——多目录不互相泄漏
- "hoist 时应用 replacement":Location.boundNode 引入新依赖(Project),只有 hoist walk 能裁剪——顺序敏感
- "每个 runner 注入自己的 Location 服务":drain 时 provide(locations.get(session.location))——服务跟着会话走

## 待深挖

- [ ] effect/app-node.ts 的 Node tag 机制
- [ ] LayerNode.hoist 的裁剪算法
- [ ] location.ts(39 行)的 Location.Ref 结构
