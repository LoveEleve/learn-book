# 01 站点国际化云原生架构（公开课）

> 用"国际化"切题，揭示国际化本质是一种**架构模式**——根据请求上下文动态路由到不同资源，和多活架构中"区域路由"是完全相同的思想。

## 讲什么

### 基础设施

| 维度 | 内容 |
|------|------|
| Java | JDK ResourceBundle → Spring MessageSource → 数据库动态加载 → 热部署文案 |
| 核心 | Microsphere I18n 特性：国际化优化、Open API 暴露、运维 Endpoints |

### 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Web 视图国际化、验证消息国际化（英文日志便于 ES 搜索，中文给运营看） |
| 整合 | Microsphere I18n 生态整合：日志国际化、站点国际化 |
| 容器化 | Spring Boot 应用 Docker 镜像构建 |

### 应用管理端

| 维度 | 内容 |
|------|------|
| Java | Microsphere DevOps 国际化整合，在线改文案实时生效 |
| UI | Node.js + TypeScript + React + Ant Design Pro 搭建 DevOps UI，前端国际化 |

## 涉及的代码仓库

- [microsphere-i18n](https://github.com/microsphere-projects/microsphere-i18n)
- [microsphere-devops](https://github.com/microsphere-projects/microsphere-devops)
- [microsphere-devops-ui](https://github.com/microsphere-projects/microsphere-devops-ui)
