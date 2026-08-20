# N-08 客户端基础 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B, 2 篇大纲域级)

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 09 审计称 SecurityProxy 在 client/security | 实测确认 + **ClientAuthService SPI 在 plugin/auth** (非 client-basic/auth 实现) — 契约/实现分离精确化 |
| 2 | 规划未提 NacosClientProperties PROTOTYPE 体系 | N-08-02 新增: 原型继承 + 三源搜索 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "客户端认证只有用户名密码" | 实测: **RAM 认证族 (CredentialWatcher/Sts/ResourceInjector 四类/签名工具) 2239 行** — 认证面扩充实证 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "client-basic 只是 remote" | 实测: env (NacosClientProperties) + auth (RAM 族) + address + utils — 09 审计的 client-basic 只有 HttpClientManager 说法需限定 remote 子包 |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (认证/属性/校验/工具 4 面全覆盖, 2 篇)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过
