# OF-9 动态刷新 — 三种 URL 动态面与刷新链

> 前置: [[OF-1-注册机制]] [[OF-2-代理创建与装配]] [[OF-7-配置隔离]] | 引出: 无 (全书收尾) | 对照: SCC (C-2 @RefreshScope) + Spring @Value
> 🟡 B | 8 KP | [模式: 动态代理目标 + FactoryBean + 懒加载]
> Pass 2 闭环: q1(Target 刷新) q2(FactoryBean) q3(懒加载) q4(联动)

**读者处境**: 配置中心改了 url, Feign 客户端怎么动态拿到新地址? 这篇拆 RefreshableHardCodedTarget + RefreshableUrlFactoryBean + PropertyBasedTarget。

### 1. Target 刷新面 — RefreshableHardCodedTarget

场景: 刷新后 URL 怎么动态?
源码路径:
- **extends Target.HardCodedTarget** (L28) + 持有 **RefreshableUrl** (L30) + cleanPath (L31)
- **url() 覆写** (L49-51): refreshableUrl.getUrl() + cleanPath — **每次调用取最新!**
- ⚠ **刷新机制精确化**: RefreshableUrl **不可变** (final) — 刷新 = **FeignClientsRegistrar.registerRefreshableBeanDefinition** (L485-503): **setScope("refresh") + ScopedProxyUtils.createScopedProxy (作用域代理)** + "beanType-contextId" 命名 — 刷新时代理重建 → 新实例 (非 @RefreshScope 注解!)
- ⚠ **isClientRefreshEnabled** (L499-500): **refresh-enabled 属性 (默认 false)** — OF-1 refreshableClient 来源闭环; ⚠ **refreshableClient 3 处影响** (L279/447/529): **options 条件** (L279) + **getOptionsByName 按名获取** (L447-450: "Request.Options-"+contextId — **超时也动态刷新!**) + resolveTarget (L529)
- ⚠ **resolveTarget 完整回退链** (L524-560): 有 url → HardCodedTarget / refreshableClient → RefreshableUrl (非空且有值) → RefreshableHardCodedTarget / **isUrlAvailableInConfig (L545-548: config[contextId].url 判定)** → PropertyBasedTarget — 三态回退完整
关键设计 (q1): **动态覆写 (每次取) vs 静态 (final)**。[模式: 刷新面]

### 2. FactoryBean 面 — RefreshableUrlFactoryBean

场景: RefreshableUrl 怎么构建?
源码路径:
- **FactoryBean<RefreshableUrl>** (L34) + getObject 缓存 (L53-56)
- 配置来源: **FeignClientProperties.config[contextId].url** (L57-59) + **null 容错** (L61-66)
- **FeignClientsRegistrar.getUrl 构建** (L67) — OF-1 复用
- **@RefreshScope**: 配置刷新 → FactoryBean 重建 → 新 URL (SCC C-2)
关键设计 (q2): **FactoryBean 模式 + RefreshScope 联动 + null 容错**。[模式: 工厂面]

### 3. 懒加载面 — PropertyBasedTarget

场景: 配置属性 URL 怎么懒加载?
源码路径:
- **extends HardCodedTarget** (L31) + 注释 (L22-25: config[clientId].url)
- 持有 config (L35) + **url() 懒计算** (L50-62: url==null → config.getUrl()+path)
- **AOT 场景**: 编译期无运行时配置 → 懒加载可用
关键设计 (q3): **懒计算 + 缓存 + AOT 兼容**。[模式: 懒加载面]

### 4. 联动面 — getUrl + 消费链

场景: URL 解析怎么统一? 三态怎么消费?
源码路径:
- **FeignClientsRegistrar.getUrl** (L115-127): **SpEL 排除 → 前缀补全 → URI 校验**
- **OF-2 resolveTarget 三分支** (L528-535): "RefreshableUrl-" + contextId 按名获取
- 三态: 直连 (HardCoded) / 动态 (Refreshable) / 懒加载 (PropertyBased)
关键设计 (q4): **URL 解析统一 + 三态消费 + RefreshScope 联动**。[模式: 联动面]

## 代码类型
Architecture (动态目标) + Spring 容器机制

## 负面空间 (OF-9, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不 URL 缓存 | 每次取 (q1) |
| 不刷新传播 in-flight | 下次调用生效 (q2) |
| 不做 URL 模板 | 静态值 (q2) |
| 不 URL 刷新 | 懒加载非动态 (q3) |
| 不做多 RefreshableUrl | 按 contextId 唯一 (q4) |
| 不做 SpEL 求值 | 留运行时 (q4) |

## 结尾桥 OUTBOUND — 全书收尾

- → **OpenFeign 阶段收尾**: 9 域闭环 — 注册 (OF-1) → 代理 (OF-2) → 契约 (OF-3) → 编解码 (OF-4) → 负载均衡 (OF-5) → 熔断 (OF-6) → 配置隔离 (OF-7) → 客户端/压缩 (OF-8) → 动态刷新 (OF-9) — **从"怎么被发现"到"怎么动态"的完整认知**
- → 对照: SCC C-2 @RefreshScope / Feign 本体 (F-1~F-6 底座)
