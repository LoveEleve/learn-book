# D-10 序列化 — Pass 2 闭环 Q4: fastjson2 + 安全面 + 多序列化

> 核心: fastjson2/ + Fastjson2SecurityManager | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: fastjson2 实现? 反序列化安全? 多序列化怎么组合?**

## 机制链 (已实证)

```
FastJson2Serialization (fastjson2/ 6 文件):
├── FastJson2Serialization — Serialization 实现
├── FastJson2ObjectInput/Output — JSON 对象读写 (fastjson2 编解码)
├── **Fastjson2SecurityManager implements AllowClassNotifyListener** (L38) — 安全面!
│   ├── checkSerializable 开关 (L48, volatile)
│   ├── notifyCheckSerializable (L89-90) — 动态开关通知
│   └── 允许类检查 (反序列化类白名单/黑名单 — 防反序列化攻击)
├── Fastjson2CreatorManager — 创建器管理 (fastjson2 高效反射)
└── Fastjson2ScopeModelInitializer

多序列化面:
├── DefaultMultipleSerialization (q1: URL 自适应)
└── protobuf 面 (triple 内, D-9 已埋): PbUnpack/SingleProtobufUtils — 异构互通首选
```

## 关键设计 (why)

1. **反序列化安全 = 一等公民**: SecurityManager 类检查 — 防 RCE (反序列化攻击是 Java 安全重灾区)
2. **动态开关**: notifyCheckSerializable — 运行时开关安全检查 (性能/安全权衡)
3. **JSON vs 二进制**: fastjson2 (可读/异构) vs hessian2 (紧凑/对象图) vs protobuf (跨语言) — 三选择面
4. **安全通知监听**: AllowClassNotifyListener — 配置中心联动 (D-5 配置面呼应)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Fastjson2SecurityManager | fastjson2/Fastjson2SecurityManager.java:38-90 |
| FastJson2ObjectInput/Output | fastjson2/ |
| Fastjson2CreatorManager | fastjson2/Fastjson2CreatorManager.java |
| DefaultMultipleSerialization | common/serialize/DefaultMultipleSerialization.java:25-43 |

## 负面空间 (Q4 面)

- 不做自动白名单学习 (静态配置)
- 不做内容嗅探 (显式 content-type/ID)
- 不做加密序列化 (加密在传输层 SSL, D-8a pipeline)
