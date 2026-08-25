# Microsphere 源码分析

> 30 个模块，3216 个 Java 文件，全覆盖源码清单。

---

## 模块清单与 GitHub 仓库

| 编号 | 模块 | Java 文件数 | GitHub |
|------|------|------------|--------|
| 01 | confucius-commons | 60 | [mercyblitz/confucius-commons](https://github.com/mercyblitz/confucius-commons) |
| 02 | microsphere-java | 776 | [microsphere-projects/microsphere-java](https://github.com/microsphere-projects/microsphere-java) |
| 03 | microsphere-spring | 612 | [microsphere-projects/microsphere-spring](https://github.com/microsphere-projects/microsphere-spring) |
| 04 | microsphere-spring-boot | 144 | [microsphere-projects/microsphere-spring-boot](https://github.com/microsphere-projects/microsphere-spring-boot) |
| 05 | microsphere-spring-cloud | 175 | [microsphere-projects/microsphere-spring-cloud](https://github.com/microsphere-projects/microsphere-spring-cloud) |
| 06 | microsphere-nacos | 150 | [microsphere-projects/microsphere-nacos](https://github.com/microsphere-projects/microsphere-nacos) |
| 07 | microsphere-sentinel | 60 | [microsphere-projects/microsphere-alibaba-sentinel](https://github.com/microsphere-projects/microsphere-alibaba-sentinel) |
| 08 | microsphere-redis | 181 | [microsphere-projects/microsphere-redis](https://github.com/microsphere-projects/microsphere-redis) |
| 09 | microsphere-observability | 47 | [microsphere-projects/microsphere-observability](https://github.com/microsphere-projects/microsphere-observability) |
| 10 | microsphere-dubbo | - | [microsphere-projects/microsphere-dubbo](https://github.com/microsphere-projects/microsphere-dubbo) |
| 11 | microsphere-resilience4j | 99 | [microsphere-projects/microsphere-resilience4j](https://github.com/microsphere-projects/microsphere-resilience4j) |
| 12 | microsphere-tomcat | - | [microsphere-projects/microsphere-tomcat](https://github.com/microsphere-projects/microsphere-tomcat) |
| 13 | microsphere-mybatis | 85 | [microsphere-projects/microsphere-mybatis](https://github.com/microsphere-projects/microsphere-mybatis) |
| 14 | microsphere-druid | 38 | [microsphere-projects/microsphere-alibaba-druid](https://github.com/microsphere-projects/microsphere-alibaba-druid) |
| 15 | microsphere-configuration | 15 | [microsphere-projects/microsphere-configuration](https://github.com/microsphere-projects/microsphere-configuration) |
| 16 | microsphere-gateway | 53 | [microsphere-projects/microsphere-gateway](https://github.com/microsphere-projects/microsphere-gateway) |
| 17 | microsphere-multiactive | 45 | [microsphere-projects/microsphere-multiactive](https://github.com/microsphere-projects/microsphere-multiactive) |
| 18 | microsphere-dynamic | 127 | [microsphere-projects/microsphere-dynamic](https://github.com/microsphere-projects/microsphere-dynamic) |
| 19 | microsphere-java-enterprise | 229 | [microsphere-projects/microsphere-java-enterprise](https://github.com/microsphere-projects/microsphere-java-enterprise) |
| 20 | microsphere-microprofile | 101 | [microsphere-projects/microsphere-microprofile](https://github.com/microsphere-projects/microsphere-microprofile) |
| 21 | microsphere-i18n | 78 | [microsphere-projects/microsphere-i18n](https://github.com/microsphere-projects/microsphere-i18n) |
| 22 | microsphere-logging | 60 | [microsphere-projects/microsphere-logging](https://github.com/microsphere-projects/microsphere-logging) |
| 23 | microsphere-hibernate | 26 | [microsphere-projects/microsphere-hibernate](https://github.com/microsphere-projects/microsphere-hibernate) |
| 24 | microsphere-etcd | 11 | [microsphere-projects/microsphere-etcd](https://github.com/microsphere-projects/microsphere-etcd) |
| 25 | microsphere-netflix | 11 | [microsphere-projects/microsphere-netflix](https://github.com/microsphere-projects/microsphere-netflix) |
| 26 | microsphere-security | 12 | [microsphere-projects/microsphere-security](https://github.com/microsphere-projects/microsphere-security) |
| 27 | microsphere-tools | 12 | [microsphere-projects/microsphere-tools](https://github.com/microsphere-projects/microsphere-tools) |
| 28 | microsphere-jakarta | 5 | [microsphere-projects/microsphere-jakarta](https://github.com/microsphere-projects/microsphere-jakarta) |
| 29 | microsphere-bom | 2 | [microsphere-projects/microsphere-bom](https://github.com/microsphere-projects/microsphere-bom) |
| 30 | microsphere-build | 2 | [microsphere-projects/microsphere-build](https://github.com/microsphere-projects/microsphere-build) |

> 10-microsphere-dubbo 和 12-microsphere-tomcat 仅有 parent/dependencies 模块，无 Java 代码。

---

## 一键 Clone

```bash
# 创建工作目录
mkdir -p microsphere-source && cd microsphere-source

# 01 - confucius-commons
git clone https://github.com/mercyblitz/confucius-commons.git

# 02-30 - microsphere 系列
git clone https://github.com/microsphere-projects/microsphere-java.git
git clone https://github.com/microsphere-projects/microsphere-spring.git
git clone https://github.com/microsphere-projects/microsphere-spring-boot.git
git clone https://github.com/microsphere-projects/microsphere-spring-cloud.git
git clone https://github.com/microsphere-projects/microsphere-nacos.git
git clone https://github.com/microsphere-projects/microsphere-alibaba-sentinel.git
git clone https://github.com/microsphere-projects/microsphere-redis.git
git clone https://github.com/microsphere-projects/microsphere-observability.git
git clone https://github.com/microsphere-projects/microsphere-dubbo.git
git clone https://github.com/microsphere-projects/microsphere-resilience4j.git
git clone https://github.com/microsphere-projects/microsphere-tomcat.git
git clone https://github.com/microsphere-projects/microsphere-mybatis.git
git clone https://github.com/microsphere-projects/microsphere-alibaba-druid.git
git clone https://github.com/microsphere-projects/microsphere-configuration.git
git clone https://github.com/microsphere-projects/microsphere-gateway.git
git clone https://github.com/microsphere-projects/microsphere-multiactive.git
git clone https://github.com/microsphere-projects/microsphere-dynamic.git
git clone https://github.com/microsphere-projects/microsphere-java-enterprise.git
git clone https://github.com/microsphere-projects/microsphere-microprofile.git
git clone https://github.com/microsphere-projects/microsphere-i18n.git
git clone https://github.com/microsphere-projects/microsphere-logging.git
git clone https://github.com/microsphere-projects/microsphere-hibernate.git
git clone https://github.com/microsphere-projects/microsphere-etcd.git
git clone https://github.com/microsphere-projects/microsphere-netflix.git
git clone https://github.com/microsphere-projects/microsphere-security.git
git clone https://github.com/microsphere-projects/microsphere-tools.git
git clone https://github.com/microsphere-projects/microsphere-jakarta.git
git clone https://github.com/microsphere-projects/microsphere-bom.git
git clone https://github.com/microsphere-projects/microsphere-build.git
```

---

## 目录结构

每个 `XX-*-analysis/` 目录下包含：

- `XX-01-source-code-analysis.md` — 源码文件清单（按子模块分组）
- 其他分析文档（架构对比、生产部署、需求规格等）

```
microsphere-analysis/
├── README.md
├── 01-confucius-commons-analysis/
│   ├── 01-01-source-code-analysis.md    ← 源码清单
│   ├── 01-classloader-introspection.md
│   └── ...
├── 02-microsphere-java-analysis/
│   ├── 02-01-source-code-analysis.md    ← 源码清单
│   └── ...
├── ...
└── 30-microsphere-build-analysis/
    └── 30-01-source-code-analysis.md    ← 源码清单
```
