# D-8b HTTP 传输栈 http12 — Pass 2 闭环 Q4: REST 元数据面 (Mapping/OpenAPI)

> 核心: rest/ 包 (8 文件) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: REST 面怎么描述服务? Mapping 怎么映射路径? OpenAPI 怎么生成?**

## 机制链 (已实证)

```
rest/ 包 (8 文件) — REST 元数据模型:
├── **Mapping 注解** (Mapping.java:41-60): @Mapping(path/value/...) — 路径→处理器映射声明
│   (D-9 triple 的 REST_ENABLED → mappingRegistry 用此注解!)
├── Operation — 操作元数据 (HTTP 方法/路径/参数)
├── Param + ParamType — 参数模型 (query/path/header/body)
├── Schema — 参数 Schema 描述
├── OpenAPI + OpenAPIRequest + OpenAPIService — OpenAPI 文档生成面
└── D-9 关联: TripleProtocol.export 里 mappingRegistry.register/unregister (REST_ENABLED 时)
```

## 关键设计 (why)

1. **注解驱动映射**: @Mapping 声明式 — 路径到处理器的静态映射 (对比 Spring @RequestMapping 思路)
2. **OpenAPI 生成**: 从元数据模型生成 OpenAPI 文档 — 服务自描述 (与 D-11 元数据呼应)
3. **统一参数模型**: Param/ParamType/Schema — 跨 h1/h2/REST 参数解析基础
4. **D-9 基座**: triple 的 REST_ENABLED 直接消费 (mappingRegistry) — 双协议面 (RPC + REST 同端口)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Mapping 注解 | rest/Mapping.java:41-60 |
| Operation/Param/ParamType/Schema | rest/ |
| OpenAPI 族 | rest/OpenAPI*.java |
| D-9 消费点 (mappingRegistry) | dubbo-rpc-triple TripleProtocol.java:119,138 (REST_ENABLED) |

## 负面空间 (Q4 面)

- 不做动态路由注册 (注解静态声明)
- 不做 OpenAPI 版本协商
- 不做 REST 鉴权 (认证在 D-4 Filter 面)
