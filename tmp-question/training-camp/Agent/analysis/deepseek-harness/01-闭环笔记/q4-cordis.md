# q4 — Cordis 插件框架(深度版:五想法 + 四分派模式 + 可逆效果)

> 域:②执行引擎(框架基础) | 文件:vendor/cordis/src/(context 146/events 352/fiber 754/reflect 418/registry 337/service 115/logger 270)+ docs/cordis-primer.md(权威)+ docs/cordis-api/context.md
> review 轮次:2 轮(源码结构 + primer 全文)

---

## 假设

Cordis = vendored 插件框架("一切皆插件的底料")。五想法:插件=Service/上下文=服务仓库/inject 声明依赖/类型化事件/注册=可逆效果。分派四模式(emit/waterfall/parallel/serial)。

## 验证

### 1. 五想法(设计 1:primer 权威)

```ts
// cordis-primer.md:
1. 插件 = 实现 Service 的对象(函数插件:可选 inject + apply(ctx);Service 子类:生命周期挂载)
2. 上下文 = 服务仓库:服务声明 ctx.<key>(ctx.tools/ctx.llm/ctx.sessions);其他插件按键找服务(不 import 具体实现)
3. inject 声明依赖:命名所需服务 → 等它们存在 → 加载顺序由服务需求表达(非手动 boot 排序)
4. 类型化事件:TS 声明合并声明事件名;按 emit/waterfall/parallel/serial 分派
5. 注册 = 可逆效果:prompt sections/tool schemas/adapters/providers/listeners 经 ctx.effect()/ctx.on() 安装
   ——reload/teardown 可预测回滚
```

### 2. 四分派模式(设计 2:事件契约)

| 模式 | 等待? | 顺序 | 返回值 |
|------|:--:|------|:--:|
| emit | 否 | 注册序观察 | 无 |
| waterfall | 否 | 注册序观察 | 有 |
| parallel | 是 | 并行观察 | 无 |
| serial | 是 | 注册序 | 有 |

```ts
// 分派模式是事件公共契约的一部分;新事件用 @mode 标注;生成目录对照声明与分派点
// waterfall = around-middleware:监听器收 (...args, next);调 next() 委托(可能包装结果);
//   不调 = 短路;next() 返回值传播
// 单决策事件:短路即设计(策略监听器拥有决策时不调 next;仅注解/观察必须委托)
```

**产品启示**:①对齐模块的三档提问 = 单决策瀑布(策略拥有决策);②执行管线的每个扩展点都标注 @mode——契约即文档。

### 3. 源码结构(设计 3:vendored 核心)

```ts
// vendor/cordis/src:
context.ts(146):Context 服务仓库
events.ts(352):事件分派系统(四模式)
fiber.ts(754):插件生命周期(UNLOADING/DISPOSED/FAILED 状态)
reflect.ts(418):服务反射(依赖注入)
registry.ts(337):注册表
service.ts(115):Service 基类
logger.ts(270):日志
// vendor/ 其余:cosmokit(工具)/group/hmr(热重载)/include(配置解析 !!js)/loader/logger-console/schemastery(schema)
```

### 4. Loader 配置(设计 4:!!js + overlay)

```ts
// cordis-primer Loader Configuration:
@deepseek-ai/cordis-plugin-include 解析 !!js 为表达式节点(!!js 允许,!js 禁止)
Loader 插值 entry.config(声明 injections 激活后,针对插件上下文)和 entry.disabled(每次挂载决策,针对 loader 上下文)
其他元数据保持字面量;环境选插件用 overlays
```

### 5. 实用规则(设计 5:归属)

```ts
// "工具管线事件属 ctx.tools,模型流属 ctx.llm,live agent 协调属 ctx.agents"
// 拦截/策略用事件;直接能力调用用服务方法
// 每个注册有 disposer(ctx.effect() 返回或 Cordis helper)
// teardown 顺序重要 → 相关工作在同一个 effect(disposal 按预期序回滚)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 五想法(插件/上下文/inject/事件/可逆效果) | cordis-primer.md | ②插件化架构 |
| 2 | 四分派模式(emit/waterfall/parallel/serial) | cordis-primer.md + events.ts | ②扩展点契约 |
| 3 | fiber 生命周期(UNLOADING/DISPOSED/FAILED) | fiber.ts(754) | ②插件生命周期 |
| 4 | Loader 配置(!!js + overlays) | cordis-primer.md | ①配置组合 |
| 5 | 归属规则(事件拦截/服务调用) | cordis-primer.md | ②架构纪律 |

## 面试弹药

- "无特权核心":插件树由配置组成,模型适配器/工具注册表/会话日志/agent 循环全是插件——可替换性即架构
- "注册 = 可逆效果":ctx.effect() 返回 disposer,reload/teardown 预测回滚——HMR 安全
- "inject 声明依赖":加载顺序由服务需求表达,不是手动 boot 排序——拓扑自动
- "waterfall 短路即设计":单决策事件,策略监听器不调 next 即拥有决策——与 OpenCode 的中间件不同(这里短路是特性)

## 待深挖

- [ ] fiber.ts 的生命周期细节(754 行)
- [ ] events.ts 的分派实现
- [ ] vendor/loader 的挂载决策
