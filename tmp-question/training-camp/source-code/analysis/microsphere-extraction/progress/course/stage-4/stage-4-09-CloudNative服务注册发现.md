# stage-4 · 第 09 节：第六节：Cloud-Native 服务注册与发现多活架构通用设计与实现 — 知识点提取

> 课程：stage-4 多活架构 第 09 节（通用化组 07-09 收官）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/09. 第六节：Cloud-Native 服务注册与发现多活架构通用设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（Cloud-Native 注册中心形态 + Hybrid 注册发现——07 篇 AZ Locator 的 Cloud-Native 适配面）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**microsphere 框架文档（非 Eureka 文档）——Eureka 仅生态列表一行（docs:14）**

> **文档形态**：**Cloud-Native 主题（399 行）**——主要内容：①AZ Locator 适配 **K8s API Server/etcd/istio XDS** 注册中心（docs:3）②Microsphere 整合实现 **Hybrid 注册发现**（docs:4）；**大量重复**——K8s 简介/组件/插件（stage-3-22 已提取）、xDS（stage-3-24 已提取）→ **交叉引用不重复提取（06 纪律）**；**本篇增量** = Cloud-Native 注册中心适配 + Hybrid 形态 + K8s 对象模型 + 初体验工程。

---

## 一、本节概览

- **技术域**：Cloud-Native 注册中心（K8s API Server/etcd/istio XDS）、Hybrid 注册发现、K8s 对象模型、生态全景
- **维度**：`[分布式问题]`（Cloud-Native 注册形态）+ `[工程问题]`（K8s 对象/初体验）+ `[规范]`（K8s 声明式模型）
- **核心命题**：**Cloud-Native 时代的注册与发现形态**——docs 两条主线：①**平台化注册中心**（K8s API Server/etcd/istio XDS——基础设施承担注册发现，业务零自建）②**Hybrid 多注册发现**（Microsphere 整合不同注册中心混合——05/06 多注册机制的 Cloud-Native 形态）；**知识本体 = "注册中心的平台化演进"**（stage-3-24 KP-03 演进主线的本篇展开）
- **知识点数**：6 个
- **前置**：07 篇（AZ Locator）、05/06 篇（多注册）、stage-3-22（K8s 基础）、stage-3-24（xDS/K8s 注册中心）

## 前置条件清单
读者需先掌握：
1. **AZ Locator 抽象**（07 篇——区域感知通用层）
2. **多注册中心机制**（05/06 篇——组合/适配）
3. **K8s 基础**（stage-3-22——组件/能力边界——本篇交叉不重提）
4. **xDS/K8s 注册中心**（stage-3-24 KP-01/03）
未达前置者，先补：stage-3-22 / stage-3-24

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **交叉不重提**：K8s 简介/组件（stage-3-22）、xDS（stage-3-24）→ 交叉引用
- **增量聚焦**：Cloud-Native 适配 + Hybrid 形态 + K8s 对象模型（22 篇未覆盖）
- **参考实现回退**：my-xhs K8s 未引入（stage-3-22 已证）——Cloud-Native 适配为演进项

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Cloud-Native 注册中心适配（K8s API Server/etcd/istio XDS——平台化注册）【docs 主要内容①】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：07 篇（AZ Locator）、stage-3-24 KP-03（K8s 注册中心）
- **来源**：docs 主要内容①（docs:3）+ 发散 + stage-3-24 交叉
- **需求**：**Cloud-Native 的注册中心形态**——docs 明确：基于 AZ Locator 抽象适配 **Kubernetes API Server、etcd、istio XDS（Envoy）** 注册中心（docs:3——业务自建注册中心 → 平台内建）
- **自主实现**：若我设计——平台化注册三形态：**K8s API Server**（Service/Endpoints 天然服务发现——注册表平台化）/ **etcd**（强一致 KV 底座——K8s 自身数据面）/ **istio XDS**（Mesh 控制面——动态配置下发）
- **参考实现**（docs 声明 + 交叉 + 发散）：**docs 适配声明（照录）**——AZ Locator 适配三种 Cloud-Native 注册中心（docs:3）；**K8s 注册中心（stage-3-24 KP-03 交叉）**——Service/Endpoints 天然发现（kube-apiserver 为注册表）——**零部署/平台内建**（演进主线：业务自建 → K8s → xDS）；**etcd（交叉 + 发散）**——K8s 集群数据底座（stage-3-22 组件交叉）——**etcd 作注册中心 = 强一致 CP 注册表**（**stage-3-26 配置中心 etcd 交叉——etcd 机制已提取；修正：原稿误写 stage-2 26（ShardingSphere）——篇号主题核对教训**）；**istio XDS（stage-3-24 KP-01 交叉）**——Mesh 控制面动态配置（RDS/LDS/CDS/EDS——xDS 族已提取）；**AZ Locator 适配（07 篇衔接）**——区域感知抽象适配平台化注册中心（E=ServiceInstance 统一——07 篇 KP-05）
- **对比取舍**：**平台化注册（K8s/etcd/XDS——零自建）vs 业务自建（Nacos/Eureka——能力可控）**——基础设施化 vs 业务可控——**演进方向 = 平台承担（stage-3-24 演进主线）**
- **机制/说明**：Cloud-Native 注册的本质 = **"注册中心平台化"**——注册表从"业务组件"变为"基础设施能力"（K8s Service 声明式、etcd 强一致、XDS 动态下发）——**应用的注册发现需求不变，载体上移到平台**；AZ Locator 是适配的统一层（区域感知不变）
- **测试佐证**：docs:3（声明照录）+ stage-3-24 KP-01/03（xDS/K8s 注册交叉）+ stage-3-22（组件交叉）

### KP-02 Hybrid 服务注册与发现（Microsphere 整合——多注册中心的 Cloud-Native 形态）【docs 主要内容②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：05/06 篇（多注册机制）
- **来源**：docs 主要内容②（docs:4）+ 05/06 篇衔接 + 发散
- **需求**：**Hybrid（混合）注册发现**——docs 明确：整合 Microsphere 项目，实现**不同注册中心 Hybrid 服务注册与发现**（docs:4——业务注册中心 + 平台注册中心混合并存）
- **自主实现**：若我设计——Hybrid = 多注册中心的"跨代混合"：业务侧（Nacos/Eureka）+ 平台侧（K8s/xDS）并存——统一抽象合并（05 篇客户端合并机制）
- **参考实现**（docs 声明 + 05/06 衔接 + 发散）：**docs 声明（照录）**——Microsphere 项目整合（docs:4——`microsphere-spring-cloud` 链接）；**机制衔接（05/06 篇——不重提）**——多注册中心客户端合并（05 篇 KP-01——Nacos 多 server/组合）+ 类型组合（06 篇 KP-01——同构/异构）+ **异构组合的 Cloud-Native 场景**（业务注册中心 + 平台注册中心 = 06 篇"异构"形态的典型）；**迁移场景（stage-3-24 KP-03 交叉）**——"K8s 注册中心逐步替代 Eureka——双注册"（迁移期 Hybrid 并存——docs 24 篇场景）；**AZ Locator 统一（07 篇衔接）**——区域抽象覆盖所有注册中心（docs:3 适配 + docs:4 Hybrid = **一套区域抽象 + 多注册中心混合**）
- **对比取舍**：**Hybrid（多注册并存——迁移/混合形态）vs 单一平台（全量迁移）**——平滑过渡 vs 一步到位——**Hybrid 是迁移期的标准形态**（双注册——05/06 篇）
- **机制/说明**：Hybrid 的本质 = **"迁移期共存 + 跨代混合"**——业务自建（Nacos）+ 平台内建（K8s/xDS）并存——**客户端合并（05 篇）+ 抽象统一（07 篇）是 Hybrid 的支撑**——注册发现能力"边走边迁"
- **测试佐证**：docs:4（声明照录）+ 05/06 篇（多注册交叉）+ stage-3-24 KP-03（双注册场景交叉）

### KP-03 注册中心生态全景（开源 5 + 商业化 4）【docs §常见的 Spring Cloud 服务注册与发现】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：Medium
- **前置**：stage-2 22（注册中心生态）
- **来源**：docs §常见的 Spring Cloud 服务注册与发现（docs:8-37——**全部空节标题**）+ 发散
- **需求**：**注册中心生态全景**——docs 列出开源 5 + 商业化 4（docs:12-37——空节标题 `[跳过：标题无内容——生态定位发散补全]`）
- **自主实现**：若我设计——生态按"维护方/形态"组织：开源（Eureka/Nacos/ZK/Consul/K8s）+ 商业化（Azure/Alibaba/AWS/GCP——云厂商托管）
- **参考实现**（docs 列表照录 + 发散）：**开源 5（docs:14-24 照录）**——Netflix Eureka / Alibaba Nacos / Apache Zookeeper（**Curator Discovery 模块**——docs:20）/ Consul / **Kubernetes**；**商业化 4（docs:29-37 照录）**——Spring Cloud Azure（Microsoft）/ Spring Cloud Alibaba（Alibaba——**MSE** 托管，docs:33）/ Spring Cloud AWS（Amazon）/ Spring Cloud GCP（Google）；**生态定位（发散——已提取篇交叉）**——Eureka（过时方向——02-06 篇场景）/Nacos（国内主流——stage-3 25）/ZK（CP 协调——stage-2 09-12）/Consul（国外主流——stage-2 24 区域差异标注）/K8s（平台化——本篇 KP-01）——**商业化 = 云厂商托管版**（开源的云上服务化）
- **对比取舍**：**自建开源 vs 云厂商托管**——可控 vs 免运维——**商业化是云时代的注册中心消费方式**（Azure/Alibaba MSE/AWS/GCP）
- **机制/说明**：生态全景的演进 = **"从组件到平台到云服务"**——开源组件（Eureka/Nacos）→ 平台内建（K8s——KP-01）→ 云托管（商业化 4）——**注册中心的三代消费形态**
- **测试佐证**：docs:12-37（列表照录）+ stage-2 22/24（生态交叉）

### KP-04 Kubernetes 对象模型（spec/status/必须字段/管理 3 方式）【docs §理解 Kubernetes 对象——22 篇未覆盖增量】
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-3-22（K8s 基础——交叉）
- **来源**：docs §理解 Kubernetes 对象（docs:131-177）
- **需求**：**K8s 声明式对象模型**——docs 明确：对象是持久化实体（目标性记录——创建后系统持续确保存在）；**spec（期望状态）+ status（当前状态）**（docs:135-138）；**声明式控制**（Deployment 3 副本案例——docs:140）
- **自主实现**：若我设计——声明式模型 = spec（目标）+ status（现状）+ 控制回路（持续收敛）
- **参考实现**（docs 照录 + stage-3-22 交叉）：**对象（docs:132 照录）**——"目标性记录"——创建即告知期望状态；**spec/status（docs:135-140 照录）**——spec 用户设置（期望）/status 系统更新（当前）——控制平面持续管理实际状态匹配期望；**必须字段（docs:150-152 照录）**——apiVersion（API 版本）/kind（对象类别）/metadata（name/UID/namespace）；**对象管理 3 方式（docs:159-161 照录）**——指令式命令/指令式对象配置/声明式对象配置（kubectl 自动检测创建更新删除——**目录化操作**）；**名称与 ID（docs:165-173 照录）**——名称（同类唯一）/UID（集群唯一）/标签与注解（非唯一属性）+ 命名约束（DNS 子域名/RFC 1123/路径分段）；**命名空间（docs:177 照录）**——多虚拟集群（同一物理集群）；**声明式控制回路（stage-3-22 KP-01 交叉）**——"不是编排是控制回路"（docs:60 已在 22 篇提取——交叉不重提）
- **对比取舍**：**声明式（期望状态收敛）vs 指令式（显式操作）**——系统自治 vs 用户控制——**K8s 的核心范式**（docs:161 三种方式并存——声明式是目标）
- **机制/说明**：对象模型 = **"期望状态 + 控制回路"的信息载体**——spec 是目标（用户声明）、status 是现实（系统报告）、控制器持续收敛——**这与注册发现的关联（发散）**：K8s Service/Endpoints 就是对象（spec 声明服务 → 控制回路维护 Endpoints——**注册表由控制回路维护而非应用注册**——Cloud-Native 注册的本质，KP-01）
- **测试佐证**：docs:131-177（照录）+ stage-3-22（控制回路交叉）

### KP-05 K8s 初体验（MongoDB Secret/ConfigMap/Deployment/Service——声明式应用）【docs §Kubernetes 初体验】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-04（对象模型）
- **来源**：docs §Kubernetes 初体验（docs:242-373——4 个 yaml 块全文）
- **需求**：**K8s 应用声明的工程模式**——docs 完整示例：MongoDB 应用（Secret 凭证 + ConfigMap 配置 + Deployment/Service 声明）
- **自主实现**：若我设计——应用声明四步：Secret（敏感数据）→ ConfigMap（非敏感配置）→ Deployment（容器声明——env 从 Secret/ConfigMap 注入）→ Service（暴露）
- **参考实现**（docs yaml 4 块照录）：**Secret（docs:248-257 照录）**——`mongodb-secret`（Opaque——base64 凭证 `dXNlcm5hbWU=`）；**ConfigMap（docs:260-268 照录）**——`mongodb-configmap`（`database_url: mongodb-service`）；**Deployment & Service（docs:272-317 照录）**——`mongodb-deployment`（replicas:1/selector/labels/`env.valueFrom.secretKeyRef`——**凭证从 Secret 注入**）+ `mongodb-service`（selector 匹配 app: mongodb——**Service 发现**）；**Mongo Express（docs:321-372 照录）**——`secretKeyRef`（凭证）+ **`configMapKeyRef`（database_url——服务地址从 ConfigMap 注入——docs:356-357）** + Service `type: LoadBalancer`（nodePort 30000——docs:367-372）；**工程模式（发散）**——**配置/凭证外置**（Secret/ConfigMap——不重建镜像）+ **声明式编排**（Deployment 描述期望状态）——**与 08 篇"依赖面决定装配"呼应**；**Docker 初体验（docs:191-239 交叉）**——`docker network create`/`docker run`/Compose 版（docs:215-239——**与 stage-3 03 篇 compose 模式交叉——不重提**）
- **对比取舍**：**Secret（敏感）vs ConfigMap（非敏感）分离**——安全边界 vs 简单——**K8s 的配置分层标准**；**Service 发现（平台内建）vs 注册中心发现**——声明式 vs 注册式（KP-01 平台化衔接）
- **机制/说明**：初体验示例 = **声明式应用的完整样板**——**Secret/ConfigMap 是"配置外置"的 K8s 形态**（对比 Nacos 配置中心——stage-3 25 交叉：同类诉求不同载体）；Service = 平台内建的"注册表+负载均衡"（stage-3-24 KP-03 交叉）
- **测试佐证**：docs:242-373（4 yaml 块照录）+ stage-3-22（K8s 组件交叉）+ stage-3 03（compose 交叉）

### KP-06 现状核对（my-xhs：K8s 未引入——Cloud-Native 适配为演进项）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/02
- **来源**：my-xhs 实证（stage-3-22 交叉）+ 架构师整合
- **需求**：以 Cloud-Native 为尺——my-xhs 注册发现的平台化现状
- **自主实现**：若我设计——核对：K8s（无）/etcd（无）/xDS（无）/Hybrid（无）——Nacos 业务自建
- **参考实现**（my-xhs 实证 + 发散规划）：**K8s 未引入**（stage-3-22 KP-06 实证——k8s 模板 grep 有但无 Istio；**`[现状：docker-compose 部署——非 K8s]`**）；**注册 = Nacos 业务自建**（docker-compose.yml:493——05 篇交叉）；**Cloud-Native 适配（发散规划——独立于现状）**——触发条件：**部署平台化**（K8s 引入时）——Service 承担发现（stage-3-24 KP-03 演进路径：Nacos 退配置中心）→ Hybrid 迁移期（KP-02）→ 区域抽象不变（07 篇 AZ Locator 已备）`[决策待定：K8s 引入触发]`
- **对比取舍**：**Nacos 自建（当前）vs K8s 平台化（演进）**——业务可控 vs 零部署——**触发条件驱动（K8s 引入才演进——stage-3-24 演进主线）**
- **测试佐证**：stage-3-22 KP-06（K8s 现状交叉）+ docker-compose.yml:493（Nacos 实证）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Cloud-Native 注册中心适配 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | Medium |
| Hybrid 注册发现 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | Medium |
| 注册中心生态全景 | 工程问题 | 支撑 | P2 | 🟢 | 有效 | Medium |
| Kubernetes 对象模型 | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| K8s 初体验（Secret/ConfigMap/Deployment） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 现状核对（K8s 未引入——演进项） | 分布式问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：无新源码面（本篇为 Cloud-Native 形态 + K8s 对象模型——概念/工程篇）；交叉引用为主（stage-3-22/24 已提取机制）
- **关键实证**（交叉引用）：stage-3-22（K8s 能力/组件/插件——docs 09 的 K8s 基础全部已提取）；stage-3-24（xDS/K8s 注册中心/双注册——docs 09 的 xDS 已提取）；my-xhs（docker-compose.yml:493——Nacos 实证）
- **诚实标注**：docs 为 **Cloud-Native 主题（399 行）**——**大量重复**：K8s 简介/组件/插件（docs:46-128 → stage-3-22 交叉）、xDS（docs:377-393 → stage-3-24 交叉）、Docker 初体验（docs:191-239 → stage-3 03 compose 交叉）、minikube/Testcontainers 链接（docs:185/398 `[无本地源码：外部工具]`）——**交叉引用不重复提取（06 纪律）**；**生态列表空节标题（docs:12-37）`[跳过：标题无内容——发散补全]`**；**docs:375 空标题（"# <br />"）`[跳过：空节]`**；**图 3 张 `[跳过：图示佐证——docs:63 集群架构图/:144 对象描述图/:245 MongoDB 应用图]`**；Eureka 仅生态列表一行（docs:14——场景）
- **关联标注**：07 篇（AZ Locator——适配层）；05/06 篇（多注册——Hybrid 机制）；stage-3-22（K8s 基础）；stage-3-24（xDS/K8s 注册）；stage-2 22/24（生态）；stage-3 25（Nacos 配置）

---

## 五、本节小结（三层次视角）

**需求**：Cloud-Native 时代的注册发现形态——平台化注册中心（K8s/etcd/xDS）+ Hybrid 混合 + 生态全景。

**自主实现核心**：①**注册中心三代消费形态**（开源组件 → 平台内建 → 云托管）②**平台化注册的本质 = 注册表由控制回路维护**（K8s Service/Endpoints——声明式而非注册式）③**Hybrid = 迁移期共存**（业务自建 + 平台内建——客户端合并 + 抽象统一支撑）。

**参考实现**：docs 声明照录（适配 3 平台/Hybrid）+ 交叉引用（stage-3-22/24 已提取机制不重提）+ K8s 对象模型照录（增量）+ 初体验 yaml 4 块照录。

**对比取舍**：知识本体是"**注册中心的平台化演进**"——平台内建（零部署）vs 业务自建（可控）、Hybrid（迁移共存）vs 单一平台、Secret/ConfigMap（配置外置 K8s 形态）vs Nacos 配置；my-xhs **Nacos 自建 + K8s 未引入**（演进触发条件：K8s 引入——stage-3-24 演进主线）。

**待验证汇总**：
- my-xhs K8s 引入触发（`[决策待定]`——部署平台化诉求）
- microsphere-spring-cloud 的 Hybrid 实现（docs:4 链接 `[无本地源码：外部项目]`——source/ 提取核对）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| K8s 注册中心（平台化） | ❌ K8s 未引入（stage-3-22 实证——docker-compose 部署） | 现状说明：触发条件驱动（部署平台化） |
| etcd 注册中心 | ❌ 无（etcd 机制 stage-2 26 已提） | 现状说明：Nacos 覆盖 |
| istio XDS | ❌ 无（stage-3-21 已证） | 现状说明：Mesh 演进项 |
| Hybrid 注册发现 | ❌ 无（单 Nacos） | 现状说明：迁移期形态未触发 |
| K8s 对象模型/初体验 | ⚠️ k8s 模板已有（stage-3-22 KP-06） | 现状说明：模板备而不用（部署未 K8s 化） |

### 差距清单

1. **P3**：K8s 部署平台化（触发条件：部署规模/编排诉求——引入后 Service 承担发现、Nacos 退配置中心——stage-3-24 KP-03 演进路径）
2. **P3**：Hybrid 注册（迁移期形态——K8s 引入时双注册共存）

**结论**：09 篇——my-xhs **Nacos 自建注册 + K8s 未引入**（Cloud-Native 适配全部为演进项——触发条件驱动）；docs 大量内容交叉引用已提取篇（stage-3-22/24）；无 P1/P2 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Cloud-Native 主题（399 行）——声明照录 + 交叉引用为主；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：Cloud-Native 注册发现的完整认知该讲什么

docs 是 Cloud-Native 主题文档。完整还该包含：

1. **"注册中心的平台化是必然演进"**（docs + 发散）：业务自建（Nacos/Eureka——部署/运维/高可用都自己管）→ 平台内建（K8s Service——声明式/控制回路/零部署）→ 云托管（MSE/Azure 等——免运维）——**三代消费形态对应"职责上移"**；**为什么**——注册发现是"基础设施共性"（所有应用都需要——平台化成本最低）——stage-3-24 KP-03 演进主线的本篇展开
2. **"声明式注册 vs 注册式注册"**（docs 对象模型 + 发散）：**K8s 的注册表（Endpoints）由控制回路维护**（spec 声明 Service → 控制器收敛 Endpoints）——**不是应用"注册"而是系统"推导"**——对比 Nacos/Eureka 的"应用主动注册 + 心跳保活"——**两种注册哲学的对照**：平台推导（声明式）vs 应用自报（注册式）——**这是 Cloud-Native 注册的本质差异**
3. **"Hybrid 是迁移期的工程常态"**（docs:4 + 发散）：双注册（业务+平台并存）→ 验证 → 移除一方——**05/06 篇多注册机制 + 07 篇 AZ Locator 抽象 = Hybrid 的三件套支撑**（合并/组合/区域统一）——**迁移不是"切换"是"并存过渡"**
4. **"K8s 对象模型是声明式范式的完整表达"**（docs:131-177 + 发散）：spec/status/控制回路——**"期望状态 + 持续收敛"是 K8s 的一切**（Deployment/Service/Secret 都是对象）——**掌握对象模型 = 掌握 K8s 的使用语法**（与 22 篇组件基础互补）
5. **"配置外置的三代形态"**（docs 初体验 + 发散）：配置文件（本地）→ 配置中心（Nacos——stage-3 25）→ **K8s Secret/ConfigMap（平台外置）**——**同类诉求的载体演进**——K8s 引入后配置载体随平台走（Secret/ConfigMap——不重建镜像）
6. **"my-xhs 单机/自建的合理性"**（发散）：15 服务/单机——K8s 平台化是**部署规模/编排诉求**驱动（触发条件判据）——**学演进、看触发、不照搬**（08 SOP）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 平台化注册（K8s/etcd/XDS） vs 业务自建（Nacos） | 零部署/声明式 vs 可控/生态（演进方向平台化） |
| 声明式（控制回路推导） vs 注册式（应用自报） | 平台自治 vs 应用参与 |
| Hybrid（并存） vs 单一平台（全量迁移） | 平滑过渡 vs 一步到位（迁移期常态） |
| Secret vs ConfigMap | 敏感边界 vs 非敏感（配置分层） |

### 常见坑/反模式

1. **把 K8s 当"升级版 Docker"**：K8s 是声明式控制回路（对象模型），不是容器编排器（docs:60 明确——22 篇已提）
2. **平台化不评估迁移成本**：K8s 引入是**部署架构变革**（不是加个工具）——Service 承担发现后 Nacos 的角色要重新定位（配置中心）
3. **Hybrid 无退出机制**：双注册拖成长期并存（成本翻倍——05/06 篇教训延续）
4. **Secret 明文存储**：K8s Secret 是 base64（不是加密）——敏感数据要配合外部密钥管理（docs 示例的 base64 仅教学）
5. **忽略声明式与注册式的差异**：应用仍"主动注册"到 K8s（注册式思维）——K8s 是控制回路推导（声明式）——**两套思维不可混**
6. **docs 重复内容重复提取**：K8s 基础/xDS 已提取——交叉引用不重提（06 纪律——本篇实践）

### 生态位置

- **stage-4 教学主线**：**通用化组（07-09 收官）**——07 AZ Locator 抽象 → 08 生命周期/装配 → **09 Cloud-Native（本篇：平台化注册 + Hybrid——通用化组收官）** → 10-12 负载均衡 → 14-15 网关 → 16-19 数据面多活
- **前后篇衔接**：07 篇（AZ Locator——适配层）；05/06 篇（多注册——Hybrid 机制）；stage-3-22（K8s 基础）；stage-3-24（xDS/K8s 注册/演进主线）；stage-2 22/24（生态）；stage-3 25（Nacos 配置）；stage-2 26（etcd）
- **与源码提取的关系**：docs:4 microsphere-spring-cloud 链接 `[无本地源码：外部项目]`——Hybrid 实现待 source/ 提取核对

**架构师视角结论**：本篇为 **Cloud-Native 主题（399 行）**——两条主线：**平台化注册中心**（K8s API Server/etcd/istio XDS——注册表由控制回路维护的声明式形态）与 **Hybrid 注册发现**（迁移期多注册并存——05/06/07 篇三件套支撑）；**知识本体 = "注册中心的平台化演进"**（三代消费形态：开源组件 → 平台内建 → 云托管）；大量内容交叉引用 stage-3-22/24（不重提）；my-xhs **Nacos 自建 + K8s 未引入**（演进触发条件驱动）；**通用化组（07-09）收官**，10 篇进入负载均衡。
