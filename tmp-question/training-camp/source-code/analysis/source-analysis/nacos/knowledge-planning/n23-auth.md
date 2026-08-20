# N-23 认证/权限深化 — 知识规划 (KP)

> 🟡 B | 模块: auth (28 文件) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 身份构建 | Grpc/HttpIdentityContextBuilder | 协议提取 |
| 2 | 资源解析 | ResourceParser 六实现 | 协议×领域 |
| 3 | 声明权限 | @Secured | 资源动作 |
| 4 | 服务器身份 | ServerIdentityChecker | 防伪 |
| 5 | 配置 | NacosAuthConfigHolder | authEnabled |
| 6 | 错误码 | AuthErrorCode | 分级 |

## 02 高频坑
1. 资源解析按协议×领域 (含 Ai)
2. Secured 声明式权限
3. 服务器身份防伪造节点
4. 身份从 metadata/header 提取

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 身份 | 双构建 / 提取 |
| 资源 | 解析矩阵 / 默认 |
| 权限 | Secured / 校验 / 服务器身份 |
| 配置 | Holder / 错误码 |

## 04 跨域桥接
- ← NC-7: ProtocolAuthService 深化
- ↔ N-08: 客户端认证对称
- → 面试: "Nacos 认证三件套" — 身份 + 资源 + 权限
