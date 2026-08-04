# Spring 生态源码仓库清单

> **拉取日期**: 2026-08-01
> **本地路径**: `/data/workspace/source-code/code/spring/`
> **版本基准**: Spring Boot 3.5.x + Spring Cloud 2025.0.x + SCA 2025.0.0.0
> **MCP 索引**: 32 个仓库全部已建立 MCP codebase-memory 索引

## Spring 核心（4个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| spring-framework | v6.2.17 | spring-projects/spring-framework | 176,633 / 845,797 |
| spring-boot | 3.5.16 | spring-projects/spring-boot | 126,185 / 536,907 |
| tomcat | 10.1.34 | apache/tomcat | 77,127 / 318,085 |
| netty | 4.2.15.Final | netty/netty | 75,975 / 363,582 |

## Spring Cloud（4个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| spring-cloud-commons | v4.3.2 | spring-cloud/spring-cloud-commons | 7,805 / 26,514 |
| spring-cloud-gateway | v4.3.2 | spring-cloud/spring-cloud-gateway | 12,572 / 49,388 |
| spring-cloud-openfeign | v4.3.2 | spring-cloud/spring-cloud-openfeign | 4,394 / 16,661 |
| spring-cloud-alibaba | 2025.0.0.0 | alibaba/spring-cloud-alibaba | 9,784 / 22,662 |

## 注册/配置/协调（3个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| nacos | 3.0.3 | alibaba/nacos | 56,363 / 266,491 |
| zookeeper | 3.9.5 | apache/zookeeper | 28,664 / 117,210 |
| curator | 5.8.0 | apache/curator | 10,830 / 51,462 |

## 流控/事务/调度（3个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| sentinel | 1.8.9 | alibaba/Sentinel | 19,748 / 66,846 |
| seata | v2.5.0 | apache/incubator-seata | 56,955 / 228,857 |
| xxl-job | 2.4.2 | xuxueli/xxl-job | 4,251 / 12,950 |

## 消息队列（2个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| rocketmq | 5.3.1 | apache/rocketmq | 49,381 / 220,730 |
| kafka | 4.1.2 | apache/kafka | 177,531 / 1,003,832 |

## 数据/存储（8个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| hikaricp | 7.0.2 | brettwooldridge/HikariCP | 2,680 / 11,148 |
| druid | 1.2.27 | alibaba/druid | 64,301 / 334,836 |
| mybatis | 3.5.16 | mybatis/mybatis-3 | 23,948 / 74,337 |
| mybatis-plus | 3.5.7 | baomidou/mybatis-plus | 9,780 / 34,954 |
| shardingsphere | 5.5.1 | apache/shardingsphere | 112,876 / 368,278 |
| redis | 7.4.2 | redis/redis | 31,593 / 78,829 |
| redisson | main | redisson/redisson | 72,189 / 390,316 |
| elasticsearch | v8.12.2 | elastic/elasticsearch | 423,447 / 2,368,197 |

## RPC/共识（4个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| dubbo | main | apache/dubbo | 69,088 / 294,099 |
| feign | main | OpenFeign/feign | 12,846 / 51,415 |
| grpc-java | v1.83.1 | grpc/grpc-java | 56,081 / 184,932 |
| sofa-jraft | v1.4.1 | sofastack/sofa-jraft | 21,056 / 84,862 |

## 可观测性/诊断（4个）

| 仓库 | 版本 | GitHub | MCP 节点/边 |
|---|---|---|---|
| micrometer | main | micrometer-metrics/micrometer | 22,276 / 98,226 |
| micrometer-tracing | main | micrometer-metrics/tracing | 4,719 / 18,327 |
| skywalking | v10.4.0 | apache/skywalking | 55,992 / 171,801 |
| arthas | 4.3.2 | alibaba/arthas | 24,775 / 80,198 |

## 统计数据

- **仓库总数**: 32
- **MCP 索引总节点**: ~1,888,235
- **MCP 索引总边**: ~8,718,928
- **源码总大小**: ~1.9GB（含 .git 目录）

## 补充说明

### 不在 spring/ 目录下的源码

| 目录 | 内容 | 说明 |
|---|---|---|
| `/data/workspace/source-code/code/microsphere/` | microsphere 项目 34 个仓库 | 已有 MCP 索引（17 个），属于 microsphere 分析项目 |
| `/data/workspace/source-code/code/source-md/` | 源码分析 markdown 文档积累 | sentinel/rocketmq 等项目的源码阅读笔记 |

### 拉取命令模板

```bash
# 标准拉取方式（浅克隆，指定分支，目标路径 /data/workspace/source-code/code/spring/）
git clone --depth 1 --branch <VERSION_TAG> https://github.com/<ORG>/<REPO>.git
```

### Git 版本管理

- 清单文件通过 MinerU 的 git 仓库（`github.com:LoveEleve/learn-book.git`）管理
- spring/ 下的 32 个源码仓库各自独立 git 管理（上游仓库的 `--depth 1` 浅克隆）
- 源码不推送到 GitHub（均为公开仓库，通过本清单可随时重建）
