# OF-9 动态刷新 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | RefreshableHardCodedTarget + RefreshableUrl 骨架 (@RefreshScope 联动) |
| 2.1+ | RefreshableUrlFactoryBean (FactoryBean<RefreshableUrl> + 配置构建) |
| 3.x | PropertyBasedTarget (配置属性懒加载 — AOT 场景); FeignClientsRegistrar.getUrl 统一规范化 (SpEL/前缀/URI 校验) |
| 4.x | null 容错 (无配置 → RefreshableUrl(null)); "RefreshableUrl-" + contextId 按名获取 |

## 痕迹证据

- RefreshableHardCodedTarget.java:28,63-66: extends HardCodedTarget + url() 覆写 (2.x 锚)
- RefreshableUrlFactoryBean.java:34,53-68: FactoryBean + getObject 配置构建 (2.1+ 锚)
- PropertyBasedTarget.java:22-25,50-62: 配置属性注释 + url() 懒计算 (3.x 锚)
- FeignClientsRegistrar.java:115-127: getUrl 规范化 (3.x 锚)
- FeignClientFactoryBean.java:528-535: 三分支消费 (3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ FactoryBean" — 类实证 (实证)
- "3.x 懒加载/AOT" — 类注释实证 (实证)
- "4.x 容错/命名" — 常量实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- SCC C-2 @RefreshScope: scc1-bootstrap (RefreshScope 域) — 刷新机制对照
- Spring @Value: 属性注入 vs 动态 URL — 配置对照
- Feign 本体 HardCodedTarget: 静态目标 vs Refreshable 动态 — 底座对照
