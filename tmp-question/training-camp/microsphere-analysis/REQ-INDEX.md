# microsphere 开源生态——完整需求规格（REQ）文档索引

> **449b1ae** | 2026-08-03 | 13 个模块 | 251 项需求 | 从 0 重实现的全量需求基准

---

## 一、背景

你在 microsphere 生态中有一系列开源 Java 项目（mercyblitz/小马哥 出品，GitHub: microsphere-projects）。这些项目从 JDK 底层工具到 Spring Cloud 扩展，覆盖了微服务基础设施的各个层面。

**目标**：从这些项目的源码中**提取需求规格**——不是分析代码，而是从"开发者生产需要什么"出发，写出每模块的完整需求文档。**最终目的**：以这些 REQ 文档为唯一需求基准，从 0 重新实现所有能力——不依赖 microsphere 原代码，可以换用更主流的技术栈（如用 MyBatis-Plus 替代 microsphere-mybatis、用 Redisson 替代 microsphere-redis、用 druid-spring-boot-starter 替代 microsphere-druid）。

## 二、方法论

### 2.1 每模块工作流程

1. **深度探索源码** → 读所有 Java 文件 + 测试 + spring.factories/AutoConfiguration
2. **对照主流框架** → 逐项 MCP 验证"官方/主流是否已有等价实现"
3. **写 REQ 文档** → 三层结构：已实现 + 待修复 Bug + 发散（生产还需要但没做的）
4. **深度查漏补缺** → 包覆盖率 100% + MCP 索引验证 + 读已有分析文档

### 2.2 三层 REQ 结构

| 层级 | 编号 | 含义 | 例子 |
|------|:---:|------|------|
| 已实现 | REQ-001~NNN | 源码中已有实现，标注已知 bug + Spring/官方重叠 | REQ-001: ExecutorFilter Chain（13-mybatis） |
| 待修复 | REQ-D01~DNN | Bug 修复 + JDK 版本适配 | REQ-D01: BlockException 透传（07-sentinel） |
| 发散 | REQ-N01~DNN | 生产环境还需要但当前未覆盖 | REQ-N01: RLock 竞争监控（08-redis） |

### 2.3 "主流视角"规则

对于 microsphere 薄封装主流框架的模块，**REQ 从主流框架视角重写**：

| 模块 | 原视角 | 重写视角 | 原因 |
|------|--------|---------|------|
| 08-redis | SDR 命令拦截 | **Redisson** | Redisson 是主流 Redis 客户端 |
| 13-mybatis | ExecutorFilter 链 | **MyBatis-Plus** | MP 是 16K+ Star 主流框架 |
| 14-druid | 微球集成层 | **Druid 官方** | druid-spring-boot-starter 已有 |

### 2.4 强制验证规则

- **每项发散必须 MCP 或源码验证**——确认"官方/主流真的没有"才能写到 REQ
- **每模块必须对照 Spring 生态**——grep Spring Framework/Boot/Cloud 源码确认不重复
- **数字自洽**——头部声明计数 = 正文 grep 计数

---

## 三、REQ 文档清单

### 3.1 有效模块（13 个，251 项）

| # | 模块 | 定位 | 文件数 | REQ 项 | Spring/主流重叠 | 视角 |
|---|------|------|:---:|:---:|:---:|------|
| 01 | confucius-commons | JDK 后门工具 | 38 | **36** | ~10% | 微球 |
| 02 | microsphere-java | 生态标准库 | 776 | **48** | ~70% | 微球（⚠️ 仅标注，不展开原理） |
| 03 | microsphere-spring | Spring 生产增强 | 323 | **35** | ~20% | 微球 |
| 04 | microsphere-spring-boot | Boot 适配 | 77 | **26** | ~55% | 微球（⚠️ 待 Boot REQ 对照） |
| 05 | microsphere-spring-cloud | Cloud 增强 | 78 | **19** | ~30% | 微球 |
| 07 | microsphere-sentinel | Sentinel 集成 | 60 | **15** | 5 类独有适配器 | 微球 |
| 08 | microsphere-redis | Redis 增强 | 86 | **22** | SDR→Redisson | **Redisson 视角** |
| 13 | microsphere-mybatis | MyBatis 拦截 | 25 | **13** | Filter→InnerInterceptor | **MyBatis-Plus 视角** |
| 14 | microsphere-druid | Druid 增强 | 12 | **7** | 微球→官方 | **Druid 官方视角** |
| 16 | microsphere-gateway | 网关动态路由 | 28 | **8** | SCG 无 `we://` | SCG 对比 |
| 17 | microsphere-multiactive | Zone-Aware 多活 | 31 | **6** | SCL 对比 | SCL 对比 |
| 18 | microsphere-dynamic | 动态多数据源 | 81 | **8** | baomidou 对比 | baomidou 对比 |

### 3.2 跳过/废弃（5 个）

| # | 模块 | 原因 |
|---|------|------|
| 06 | microsphere-nacos | thin HTTP wrapper——官方 nacos-client SDK 已全覆盖 |
| 10~12 | dubbo/resilience4j/tomcat | 空壳/已移除/仅官方源码分析 |
| 15 | microsphere-configuration | 11 文件注解改名 wrapper |
| 09 | observability | ✅ 已由其他 AI 完成（唯一完整三件套） |

---

## 四、文档位置

```
microsphere-analysis/
  ├── 01-confucius-commons-analysis/01-REQ-requirements-spec.md
  ├── 02-microsphere-java-analysis/02-REQ-requirements-spec.md
  ├── 03-microsphere-spring-analysis/03-REQ-requirements-spec.md
  ├── 04-microsphere-spring-boot-analysis/04-REQ-requirements-spec.md
  ├── 05-microsphere-spring-cloud-analysis/05-REQ-requirements-spec.md
  ├── 07-microsphere-sentinel-analysis/07-REQ-requirements-spec.md
  ├── 08-microsphere-redis-analysis/08-REQ-requirements-spec.md
  ├── 13-microsphere-mybatis-analysis/13-REQ-requirements-spec.md
  ├── 14-microsphere-druid-analysis/14-REQ-requirements-spec.md
  ├── 16-microsphere-gateway-analysis/16-REQ-requirements-spec.md
  ├── 17-microsphere-multiactive-analysis/17-REQ-requirements-spec.md
  ├── 18-microsphere-dynamic-analysis/18-REQ-requirements-spec.md
  └── HANDOVER.md
```

---

## 五、从 0 重实现指南

### 5.1 核心原则

1. **REQ 是唯一需求基准**——不依赖原 microsphere 源码，只看 REQ 文档
2. **优先主流技术栈**——能用 Spring Boot 内置的不自己写、能用 MyBatis-Plus 的不自己写
3. **每项 REQ 必须有"问题"和"产出"**——知道"为什么做"和"做到什么程度"
4. **发散需求（N 系列）优先级最低**——先做已实现和 Bug 修复，再做发散

### 5.2 模块实现顺序建议

按自底向上依赖：

```
第 1 批（无依赖）：
  01-confucius-commons（JDK 底层——先做少量独特能力：ClassLoader、Unsafe、JVM Attach）
  02-microsphere-java（标注 Spring 重叠，不做，用 Spring 替代）

第 2 批（依赖 02）：
  03-microsphere-spring → 04-microsphere-spring-boot → 05-microsphere-spring-cloud

第 3 批（独立中间件）：
  07-microsphere-sentinel → 08-redis(Redisson视角) → 13-mybatis(MP视角)
  14-druid(Druid视角) → 16-gateway → 17-multiactive → 18-dynamic
```

### 5.3 每个 REQ 的阅读顺序

1. 读"问题"——理解生产场景
2. 读"产出"——确认交付物
3. 读"已知问题"——避免重复踩坑
4. 读"配置规格"——知道暴露哪些属性

### 5.4 原源码参考

微球源码路径（仅供参考，不依赖）：
```
/data/workspace/java-training-camp/cloud-native-code/share/  (01-05, 13-14)
/data/workspace/java-training-camp/cloud-native-code/stage-4/ (05, 08, 15-18)
/data/workspace/java-training-camp/cloud-native-code/projects/ (06, 07)
```

主流框架源码（MCP 已索引，可直接 search_graph）：
```
/data/workspace/source-code/code/spring/
  ├── spring-framework/     (v6.2.17)
  ├── spring-boot/          (v3.5.16)
  ├── spring-cloud-gateway/ (v4.1.6)
  ├── spring-cloud-commons/ (v4.3.2)
  ├── mybatis-plus/         (9,780 节点)
  ├── druid/                (64,301 节点)
  ├── redisson/             (72,189 节点)
  └── sentinel/             (19,748 节点)
```

---

## 六、统计总览

| 统计项 | 数值 |
|--------|:---:|
| 有效模块 | 13 |
| 跳过/废弃 | 5 |
| 总 REQ 需求项 | **251** |
| 其中已实现 | 134 |
| 其中待修复 Bug | 88 |
| 其中发散需求 | 68 |
| 总文档行数 | ~5,200 |
| 覆盖源码文件 | ~1,600+ |

---

## 七、Git 信息

- 仓库：`github.com:LoveEleve/learn-book.git`
- 分支：`main`
- 路径：`tmp-question/training-camp/microsphere-analysis/`
- 最新 commit：`449b1ae`
- 推送时间：2026-08-03
