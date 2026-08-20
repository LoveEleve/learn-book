# S-6 Starter 机制 — 依赖聚合与自动装配入口 (spring-boot-starter-*)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | 基础 starter(spring-boot-starter)+starter-web+starter-tomcat+spring-boot-dependencies(BOM) — 构建文件主导, 源码辅助
> 基线: BOOT-PLAN-v2 S-6 — starter 是"依赖打包"; 前置: S-2 自动装配加载(imports 是自动装配入口) + S-4 — 展开依赖传递→自动装配的映射

---

## §0.8

- 🟡 Working，1篇 — 本质(starter = 空 jar + api 依赖传递聚合, 本身无代码) → 基础 starter(spring-boot+autoconfigure+logging+core+snakeyaml) → 依赖链(starter-web→starter-json/tomcat→各依赖 jar 的 AutoConfiguration.imports→触发 S-2 自动装配) → 版本管理(spring-boot-dependencies BOM 统一版本) → 自定义 starter(命名约定/imports 入口)
- 设计模式: [模式: 聚合器]—依赖传递; [模式: 声明式装配]—依赖即配置; [模式: BOM]—版本集中管理

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| spring-boot-starter/build.gradle(23) | 基础 starter | **聚合**: api(spring-boot)+api(spring-boot-autoconfigure)+api(starter-logging)+jakarta.annotation+spring-core+snakeyaml — 无源码, 纯依赖 | High |
| spring-boot-starter-web/build.gradle(23) | 功能 starter | **starter-web**: api(starter)+api(starter-json)+api(starter-tomcat)+spring-web+spring-webmvc — 按功能聚合 | High |
| spring-boot-starter-tomcat/build.gradle(23) | 容器 starter | **starter-tomcat**: api(tomcat-embed-core/el/websocket) — 引入嵌入式 Tomcat(触发 t7/S-8) | High |
| spring-boot-dependencies/build.gradle | BOM | **版本管理**: 全部依赖版本集中声明 — 项目继承 BOM 免写版本 | High |
| (自动装配入口) | 触发 | **依赖→自动装配**: 依赖 jar 各带 META-INF/spring/*.imports(S-2) — starter 不写自动装配, 由传递依赖的 jar 提供 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: starter 是构建产物(依赖聚合), 知识单线: "starter 聚合依赖 → 传递依赖带 imports → 自动装配触发". 1篇 (~44行) 按"本质→链→版本→自定义"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Starter 本质 (空 jar + api 依赖传递聚合) | 🔴 | **为什么🔴**: starter 不是代码 — 是"依赖即配置"的声明式设计 |
| P1-2 | 依赖链→自动装配映射 (starter-web→imports→S-2 触发) | 🔴 | **为什么🔴**: starter 引入什么依赖 → 触发什么自动装配 — 全链路理解 |
| P1-3 | 基础 starter 组成 (spring-boot+autoconfigure+logging) | 🔴 | **为什么🔴**: 一切 starter 的共同底座 |
| P2-1 | BOM 版本管理 (spring-boot-dependencies) | 🟡 | **为什么🟡**: 版本统一的机制 — 免写版本号 |
| P2-2 | 自定义 starter (命名约定 + 自己的 imports) | 🟡 | **为什么🟡**: 团队复用自动装配的方式 |
| P3-1 | starter 与自动装配的边界 (starter 不写代码) | 🟢 | **为什么🟢**: 职责分离 — 自动装配在 autoconfigure 模块 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **聚合本质** (基础/功能 starter) | 🔴 | starter 是什么 |
| B | **触发链路** (依赖→imports→自动装配) | 🔴 | 怎么生效 |
| C | **版本与自定义** (BOM + 自定义 starter) | 🟡 | 管理与扩展 |

> **Cluster A (§1)**: 基础 starter(spring-boot+autoconfigure+logging+core+snakeyaml) + 功能 starter(starter-web 聚合) — "空 jar"本质
> **Cluster B (§2)**: starter-web 依赖链 → starter-json(Jackson→S-9)/starter-tomcat(嵌入式 Tomcat→t7/S-8) → 各 jar 的 imports(S-2) — 依赖到自动装配的映射
> **Cluster C (§3)**: spring-boot-dependencies BOM + 自定义 starter(命名 spring-boot-starter-*/imports)

→ 引出 S-7: MVC 自动装配 — starter-web 引入了 spring-webmvc, WebMvcAutoConfiguration 怎么自动配好 DispatcherServlet 家族(与 W-1/W-5 衔接)

(End of file - total 61 lines)
