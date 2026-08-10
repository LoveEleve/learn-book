# "你服务跑在101.132.1.34, 换个机器就跑不起来了" — 容器与Kubernetes编排

> Cluster D: 12 KPs | 依赖: 09-service-mesh, 07-microservices-design | 读者基线: 用过Docker, 了解微服务基本架构

---

### 1. 容器vs虚拟机 — 为什么Docker取代了VM成为微服务部署标准?
  VM给你完整的OS隔离但200ms启动+4GB内存 — Docker给你线程级隔离但5秒启动+50MB内存
  - B1 Ch8 §1.2: 容器本质 — Cgroups(限制: CPU/内存/磁盘配额) + Namespace(隔离: PID/NET/MNT/UTS/IPC/USER) + Union FS(写时复制层, 镜像分层共享基础层) (B4 Ch3 §3.1)
  - B4 Ch3 §3.3: Docker vs VM — VM(KVM: Guest OS→Hypervisor→Host OS, 完整虚拟→安全但重), Docker(进程隔离+共享内核, 轻量→但不适合不同OS)
  - Docker Registry: Docker Hub(公共) vs 私有Registry(Harbor, 镜像仓库+漏洞扫描+RBAC) (B4 Ch3 §3.3.3)
  - 关键设计: Docker的核心不是"容器化"而是"不可变基础设施" — 镜像=一次性, 部署=构建→推→拉→启, 环境一致性保证

### 2. Kubernetes核心抽象 — Pod/Service/Deployment/ConfigMap/Secret/Ingress, 每个解决什么?
  Docker能启动容器但无法编排 — K8s引入5个抽象
  - B1 Ch8 §3.1-3.2: K8s核心 — Pod(最小调度单位, 共享IP+IPC+Volume, 1主容器+Sidecar容器), Service(固定IP+负载均衡→Pod, ClusterIP/NodePort/LoadBalancer), Deployment(副本管理+滚动更新+回滚, ReplicaSet→Pod)
  - ConfigMap/Secret(配置注入, 环境变量/挂载文件/热更新) — 将配置与镜像分离, 同一镜像在不同环境不同配置 (B1 Ch8 §3.1)
  - Ingress: L7 HTTP路由(域名+Path→Service), 类比API网关 — 将外部流量导向内部Service网络 (B1 Ch8 §3.1)
  - 关键设计: Service是K8s最创新抽象 — Pod IP是临时的(重建就变), Service提供稳定DNS名+VIP, 让上下线对调用方透明

### 3. 部署策略 — RollingUpdate(滚动更新) vs BlueGreen(蓝绿) vs Canary(金丝雀)
  你上线v2版本, 如果用rolling update→v1 Pod逐次替换→用户看到v1和v2混合→如果v2有问题, 已部署的Pod无法快速撤销
  - B1 Ch8 §3.1: Deployment RollingUpdate — maxSurge(允许超出期望副本数N)+maxUnavailable(允许不可用N)→逐个替换Pod→检测新Pod Ready才继续→如果有问题需pod restart(慢) (B1 Ch8 §3.2)
  - B4 Ch4 §2: 蓝绿部署 — v1(blue)+v2(green)同时存在→切换Service selector从v1→v2→确认v2正常→删v1; 零停机+快速回滚→但需要2x资源
  - 金丝雀(Canary): v1 95% + v2 5% → 逐渐增加v2比重→配合Service Mesh(Istio VirtualService控制权重) → 精确到请求级别
  - 关键设计: 蓝绿=infrastructure级(交换机切换) / Canary=请求级(按流量权重) — 蓝绿简单但贵, Canary复杂但省

### 4. 镜像构建最佳实践 — 你的Docker镜像1GB, 同事的镜像只有50MB
  你COPY了/src/target/app.jar → 你的镜像包含了整个JDK8 200MB → Docker registry占满
  - B4 Ch3 §3.4: Dockerfile优化 — 多阶段构建(Maven构建层 + JRE运行层→最终镜像只含运行时, 丢弃Maven), .dockerignore减小上下文, ENTRYPOINT exec格式
  - 分层缓存: RUN apt-get install → COPY pom.xml → RUN mvn dependency → COPY src → RUN mvn package → 依赖变化少→缓存命中率最高
  - [案例: 生产环境Java服务Docker镜像典型大小=50-80MB(Alpine JRE+JAR), 而非1GB Big Linux+JDK+JAR]
  - 关键设计: 镜像层共享 — 10个Java服务共享同一个基础层(Alpine/JRE), 只上传差异层(JAR), 仓库空间从10GB→500MB

### 5. K8s vs Docker Swarm — 为什么Swarm"输"了编排战争?
  Swarm和K8s都做编排, 但Swarm更简单
  - B1 Ch8 §2.4: Docker Swarm — Docker生态, docker-compose语法升级, 服务(Deploy)+网络(Overlay)+密钥(Secret), 简单但功能受限
  - K8s vs Swarm对比: K8s(社区大+生态丰富+支持复杂调度/网络策略/CRD扩展, 运维复杂) vs Swarm(Docker CLI+简单部署, 功能有限+社区小)
  - 关键设计: Swarm输在"不是在解决问题的方向上复杂" — K8s可处理万级节点复杂调度, 但中小企业运行5-10节点K8s也是万金油配置

### 6. 收束 — 回到"换机器跑不起来"
  - Docker说是容器, 本质是不可变基础设施 — 镜像=一次性, 环境=由ConfigMap注入, 在任何K8s节点启动→自动获取Service IP→注册发现→就绪
  - K8s不是容器管理工具, 是"声明式基础设施" — 你声明"我要3个健康Pod, 随时可用"而不关心怎么创建Pod
  - Swarm简单但小规模, K8s复杂但全场景 — 你的服务规模决定选择

---

### 核心悬念
**"K8s解决了部署, 但你怎么知道新版本不会搞炸? — 需要CI/CD流水线+GitOps+弹性伸缩+成本优化——建设有效的工程交付管线"**

→ 引出 云原生实践: CI/CD流水线+GitOps+12因子+弹性伸缩+Serverless方向 (11-cloud-native-patterns)
