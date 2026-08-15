# hq31 网关授权混入(Authz Mixin)— 产品②"入口授权"蓝本

> 项目:Hermes(gateway/authz_mixin.py 900 行 + gateway/pairing.py 配对存储 + gateway/run.py:6200 接入)
> 假设:谁被允许与 agent 对话(用户/聊天级别)— Hermes 的平台级授权适配器是"入口授权"的样本(域发现 v7:DM/group 策略、发送者 allowlist、配对存储、upstream 授权 vs 本地策略)。
> 结论:✅ 成立——适配器解析/授权检查/allowlist/上游授权区分/DM 策略/未授权行为全具备,产品②"入口安全"直接蓝本。

---

## 一、架构全景:入站授权集群

```
┌────────────────────────────────────────────────────────────┐
│ GatewayAuthorizationMixin(90):入站消息授权集群            │
│   _is_user_authorized(386):用户/聊天是否允许与 agent 对话  │
├────────────────────────────────────────────────────────────┤
│ 适配器解析:                                               │
│   _authorization_adapter(93)/_adapter_for_source(130)/     │
│   _registered_transport_adapter(157)/_adapter_profile_for_ │
│   source(180,多 profile)                                  │
├────────────────────────────────────────────────────────────┤
│ 策略查询:                                                 │
│   _adapter_dm_policy(246)/_adapter_group_policy(283)/      │
│   _adapter_group_has_sender_allowlist(318)/                │
│   _adapter_authorization_is_upstream(194)/                 │
│   _adapter_enforces_own_access_policy(218)                 │
├────────────────────────────────────────────────────────────┤
│ 未授权行为:_get_unauthorized_dm_behavior(797)             │
│ 配对:_pairing_store_for(371,gateway/pairing.py 存储)      │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:授权检查(核心)

**位置**:`authz_mixin.py:386`(_is_user_authorized)+ `90`(mixin)

```
_is_user_authorized:入站消息授权判定
  ——用户/聊天是否允许与 agent 对话(核心入口检查)

配置:env 门(_auth_env/_platform_gate_env)+ allowlist(_coerce_allow_set)
```

**正确性价值**:单入口授权判定——谁可与 agent 对话集中管理。

**产品④映射**:入口授权单判定——用户/聊天级准入。

## 设计 2:适配器解析(平台分层)

**位置**:`authz_mixin.py:93-180`

```
- _authorization_adapter:授权用适配器(注册传输 vs 回退)
- _adapter_for_source:事件源 → 适配器
- _registered_transport_adapter:注册传输适配器
- _adapter_profile_for_source:源 → profile(multiplex 多 profile)
```

**正确性价值**:源 → 适配器 → profile 解析链——多 profile 下授权按 profile 分层。

**产品④映射**:入口授权按 profile 分层(multiplex 兼容)。

## 设计 3:策略查询(平台差异)

**位置**:`authz_mixin.py:194-371`

```
- _adapter_authorization_is_upstream:上游授权(平台自有)
- _adapter_enforces_own_access_policy:平台自有访问策略
- _adapter_dm_policy:DM 策略(per-adapter)
- _adapter_group_policy:group 策略
- _adapter_group_has_sender_allowlist:发送者 allowlist
```

**正确性价值**:策略差异按平台查询——upstream vs 本地策略分层(与 hq30 认证标志衔接)。

**产品④映射**:入口授权策略分层——平台自有 vs 本地,DM/group 分策略。

## 设计 4:配对存储 + 未授权行为

**位置**:`authz_mixin.py:371`(_pairing_store_for)+ `797`(_get_unauthorized_dm_behavior)+ `gateway/pairing.py`

```
- _pairing_store_for:配对存储解析(授权配对记录)
- _get_unauthorized_dm_behavior:未授权 DM 行为(响应策略)
- pairing.py:配对存储(allowlist 同步/用户 id 归一化/_user_ids_match 别名匹配)
```

**正确性价值**:配对存储(持久授权)+ 未授权行为显式(不默认静默)。

**产品④映射**:入口授权持久化——配对存储 + 未授权行为显式策略。

---

## 三、与四项目对比(入口授权)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes authz_mixin |
|------|----|----------|----------|-----|---------------------|
| 入口授权 | — | — | permission | — | **用户/聊天级授权** |
| 策略 | — | — | deny 永远赢 | — | **DM/group/allowlist 分层** |
| 上游 | — | — | — | — | **upstream vs 本地区分** |
| 配对 | — | — | — | — | **配对存储持久化** |
| 未授权 | — | — | — | — | **显式行为策略** |

**结论**:产品"入口安全"参考 = Hermes authz_mixin(授权集群 + 策略分层 + 配对)+ OpenCode permission(deny 永远赢)。**Hermes 是平台多入口授权,OpenCode 是工具权限——两层互补**。

---

## 四、面试弹药

1. **"单入口授权判定"**:_is_user_authorized——用户/聊天级准入集中
2. **"策略按平台查询"**:DM/group/allowlist 分层——平台差异内化
3. **"upstream vs 本地"**:_adapter_authorization_is_upstream——平台自有授权区分
4. **"配对存储持久化"**:授权配对记录持久(重启保留)
5. **"未授权行为显式"**:不默认静默——响应策略明确

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 授权单判定 | 入口准入集中 |
| 适配器解析 | 源→适配器→profile 分层 |
| 策略查询 | DM/group/allowlist 平台差异 |
| 配对存储 | 授权持久化 |
| 未授权行为 | 显式策略 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_multiplex_profile_authz + test_relay_upstream_authz(9 用例)+ test_authorization_gate(关联)
> 位置:GatewayAuthorizationMixin :90 / _is_user_authorized :386 / _adapter_dm_policy :246 / _pairing_store_for :371 / 接入 run.py:6200
> 关联:gateway/pairing.py(配对存储,user id 归一化/别名匹配)
