# 代码写完了, 怎么安全上线? — CI/CD流水线+GitOps+12因子, 云原生的工程交付闭环

> Cluster D: 13 KPs | 依赖: 10-container-orchestration, 08-resilience-patterns | 读者基线: 用过Git, 写过部署脚本

---

### 1. CI/CD流水线 — 从git push到用户看到新功能, 中间经过了什么?
  你push代码到Git→等30分钟构建+测试→手动部署→发现漏了配置→回滚→修→再跑30分钟→3小时后才上线一个改LOG的commit
  - B4 Ch9 §5: CI/CD全流程 — 代码提交→CI(编译+单元测试+代码扫描)→构建镜像(Docker build)→推Registry→CD(部署到测试/预发/生产)→集成测试→回归测试→部署 → 目标是"提交即上线(在5分钟内)"
  - 持续交付vs持续部署: CDelivery(人工确认后才部署), CDeploy(自动化部署到生产, 需要极端完善的测试+监控) (B4 Ch9 §5.1-5.2)
  - 流水线工具: Jenkins(老牌, 灵活, 但Pipeline复杂)→GitLab CI(内置, .gitlab-ci.yml)→GitHub Actions(新派, 简洁)→Tekton(K8s原生, CRD操作) [案例: GitHub Actions免费2000分钟/month开源项目, Dependabot自动升级依赖+CI验证+合并PR]
  - 关键设计: CI≠跑build, CD≠rsync — CI=每次commit验证一切, 包括lint/test/scan/coverage, 快速反馈(<5min); CD=自动化一切, 包括部署/健康检查/流量切换/回滚

### 2. GitOps — Git是唯一的真实源, 运维操作=Git Pull
  传统: SSH到服务器→kubectl apply→手动输YAML→忘了保存→下次重复 — GitOps: 一切配置在Git, 自动化同步到K8s
  - B4 Ch9 §5.5: GitOps原则 — Git是真理源(所有期望状态在Git), 自动协调器(Flux/ArgoCD对比Git与K8s实际状态→差异→自动修复), 手动变更=修改Git+PR+Merge→自动部署 (B4 Ch9 §5.5)
  - ArgoCD核心: Application CRD(YAML定义Git Repo+Target Cluster+Sync Policy), 自动/手动同步, Health监测, 回滚=git revert+重新sync
  - 声明式基础设施: 所有配置在Git→不可变+可审计+可版本—对比手动运维"ssh到服务器改然后祈祷"
  - 关键设计: GitOps的核心=运维操作的"不可变+可审计"— 谁改了? 改了什么? 因何改?→ PR代码审查+Git历史+Rollback回退

### 3. 12因子应用 — 微服务设计的最佳实践清单
  你的应用跑在K8s上但总报错"端口冲突/数据丢失/日志stdout没有" — 因为你没遵循12因子
  - B4 Ch9 §1: 12因子核心 — 代码库(1代码=1部署, 多代码库→离散), 依赖(manifest声明+隔离), 配置(Env→镜像, Dev/Staging/Prod不同环境同一镜像), 后端服务(DB/Redis=附着的资源, URL连接即可切换) (B4 Ch9 §1)
  - 具体实践: 日志→stdout(让平台收集), 进程(无状态+share-nothing, 有状态数据放Redis/DB), 端口绑定(HTTP端口export, 平台提供路由), 开发/生产环境差→统一环境 (B4 Ch9 §1)
  - [案例: 12factor.net 由Heroku工程师总结的SaaS应用最佳实践, 违反'dev/prod parity'导致'在我机器上没问题'是部署地狱的根源]
  - 关键设计: 12因子不是"必须全遵守" — 是"理解原则后灵活应用", 但"配置与代码分离"和"无状态"是关键底线

### 4. 弹性伸缩与成本优化 — 你怎么知道该加多少个Pod?
  你看到CPU 80%→加2个Pod→CPU降到40%→浪费 — 弹性伸缩的4个维度
  - K8s HPA(Horizontal Pod Autoscaler): CPU/MEM指标→Pod数自动调整, 算法=desiredReplicas=ceil[currentReplicas*(currentMetric/desiredMetric)] (B4 Ch4 §5)
  - K8s VPA(Vertical Pod Autoscaler): 自动调整Pod的CPU/MEM request/limit, 适用于资源需求波动大的服务
  - Cluster Autoscaler: 当Pod无法调度(无节点资源)→自动加入新Node; 当Node闲置→自动缩容删除Node
  - 成本优化: Spot实例(折扣80%但可被回收, 适合无状态+可中断), 混合实例(On-Demand保证安全+Spot扩容), Rightsizing(定期审计→资源使用率<30%→减少request/limit)
  - 关键设计: Auto-Scaling不是"自动魔法" — 需要正确设置request/limit(HPA依赖它计算利用率)+正确容量建模

### 5. 未来的方向 — Serverless(FaaS/BaaS) + AIOps
  你的运维团队从10人减到3人 — 需要新的运维范式
  - B4 Ch8 §1: Serverless — FaaS(函数计算, AWS Lambda, 用一次付钱一次, 不支付空转) + BaaS(后端服务, 云提供AuthDB/信息, 你只有写UI), 适用场景: 事件驱动/定时任务/API Gateway后端
  - AIOps: 用ML自动(异常检测/根因分析/容量预测) — 从"报警→人处理→恢复"到"检测→自动修复→通知"
  - [案例: Netflix用Serverless做视频转码(文件上传→Lambda→并行多分片编码→合并)高峰用上千函数实例 谷峰=0, 比常驻EC2集群省钱70%]
  - 关键设计: Serverless不适合全场景 — 冷启动(初始化函数+加载类库, 100ms~2s)、执行时间限制(最长15分钟)、状态管理(无状态, 只能靠外部), 不适合长连接/长事务

### 6. 收束 — 从代码到上线到运维的闭环
  - CI/CD→GitOps→12因子→弹性→Serverless — 是工程交付的5个阶段: 验证→部署→治理→扩容→超量
  - 云原生不是"上K8s就够了", 是"基础设施即代码+一切自动化+数据驱动持续改进"
  - 12因子+GitOps+弹性伸缩三者结合, 真正实现"提交→自动上线→自动检测→自动回滚→人工复盘"

---

### 核心悬念
**"你学了9章, 从网络通信到云原生交付 — 但你的系统还只是一个正常的电商系统。淘宝是怎么从单体→垂直拆分→服务化→单元化, 在用户的10亿次请求下不崩的?"**

→ 引出 架构演进全景: 单体→垂直拆分→SOA→微服务→Service Mesh→单元化+多活 — 真实世界大型网站的架构进化之路 (12-architecture-evolution)
