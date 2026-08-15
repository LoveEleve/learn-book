# q41 — Auth + Credential(深度版:凭证存储安全)

> 域:安全 | 文件:opencode/src/auth/index.ts(97)+ core/src/credential.ts(138)+ credential/sql.ts + account/ + core/test/credential.test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

两套凭证:Auth(provider 认证,JSON 文件 0600 + 环境注入)与 Credential(集成凭证,SQLite 表)。安全要点:文件权限 0600、环境变量可注入(CI)、OAuth 刷新令牌持久化。

## 验证

### 1. Auth(设计 1:JSON 文件 + 0600 + 环境注入)

```ts
// auth/index.ts:8-12,58-67
file = Global.Path.data/auth.json
OAUTH_DUMMY_KEY = "opencode-oauth-dummy-key"(占位)
Info = Oauth{refresh/access/expires/accountId/enterpriseUrl} | Api{key} | WellKnown{key/token}(discriminator: type)
all():OPENCODE_AUTH_CONTENT 环境变量优先(CI/测试注入);否则读 JSON + Record.filterMap(解码失败静默跳过)
set:键归一化(去尾斜杠)+ writeJson(file, data, 0o600)——0600 = 仅 owner 读写
// get(providerID):按 provider 取认证
```

**设计要点**:OAuth 刷新令牌明文存 0600 文件(本地信任);环境注入支持无文件场景(CI)。

### 2. Credential(设计 2:SQLite 表 + 集成隔离)

```ts
// credential.ts:23-47,94-121
Info = { id, integrationID, label, value }
create:事务内先删同 integrationID 的旧凭证再插新(每集成一个凭证,替换语义!)
list(integrationID):按集成查
// credential 表(60-71 schema):id/integration_id/label/value/active/time_created/time_updated
// 集成模型:integrationID 绑定凭证(MCP/第三方服务)
```

### 3. 安全对比(设计 3:两套的边界)

| 维度 | Auth | Credential |
|------|------|-----------|
| 存储 | auth.json(0600) | SQLite(credential 表) |
| 注入 | OPENCODE_AUTH_CONTENT | — |
| 归属 | providerID | integrationID |
| 替换 | set 覆盖 | create 事务替换 |
| 刷新 | OAuth refresh 持久化 | Value(编码) |

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Auth JSON 0600 + 环境注入 | auth/index.ts:58-80 | ④凭证安全 |
| 2 | OAuth 刷新令牌持久化 | auth/index.ts:14-21 | ④长会话 |
| 3 | Credential 每集成一凭证(事务替换) | credential.ts:94-121 | ④集成凭证 |
| 4 | 解码失败静默跳过 | auth/index.ts:66 | ④容错 |

## 面试弹药

- "0600 文件权限":明文刷新令牌仅 owner 可读——本地信任模型
- "环境注入优先":OPENCODE_AUTH_CONTENT 覆盖文件——CI/测试无文件可用
- "每集成一个凭证":create 事务删旧插新——无陈旧凭证
- "discriminator 联合":Oauth/Api/WellKnown 三类型按 type 判别——扩展凭证类型容易

## 待深挖

- [ ] account/(账号:登录/设备码)
- [ ] oauth/page.ts(授权页)
- [ ] MCP 凭证与 Credential 的关系
