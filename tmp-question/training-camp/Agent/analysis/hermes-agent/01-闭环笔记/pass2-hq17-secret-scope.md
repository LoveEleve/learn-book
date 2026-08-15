# hq17 秘密作用域(Secret Scope)— 产品②"凭证隔离"蓝本

> 项目:Hermes(agent/secret_scope.py 293 行 + agent/secret_sources/ 7 文件 + hermes_cli/env_loader.py + gateway/run.py multiplex 接入)
> 假设:多 profile 网关进程内服务多 profile,各 profile 有自己 .env——不能并入进程全局 os.environ(泄漏 profile A 的 key 给 profile B 回合与子进程)。Hermes 用 contextvar 作用域 + fail-closed 是"凭证隔离"的完整样本。
> 结论:✅ 成立——contextvar 作用域/双模式(fail-closed/透明兼容)/全局豁免名单/解析器 round-trip/外部源合并全具备,产品②"凭证隔离"直接蓝本。

---

## 一、架构全景:为什么需要秘密作用域

```
问题(multiplexing gateway,Workstream A):一个进程服务多 profile。
  每个 profile 有自己的 .env(provider key/平台 token)——不能 union 进
  os.environ(泄漏 profile A 的 key 给 profile B 的回合,以及所有
  env=dict(os.environ) 派生的子进程)。

┌────────────────────────────────────────────────────────────┐
│ set_secret_scope(mapping):安装当前 profile 的秘密到 contextvar│
│   (contextvar → copy_context() 传播进 agent worker 线程,     │
│    与 HERMES_HOME override 同机制)                          │
├────────────────────────────────────────────────────────────┤
│ get_secret(name):从作用域读                                  │
│   multiplex 开 + 无作用域 → RAISE(UnscopedSecretError)      │
│     ——未迁移/新增调用点在那一行响亮失败,而非泄漏另一 profile│
│   multiplex 关(默认)→ 透明读 os.environ(单 profile 不变)    │
├────────────────────────────────────────────────────────────┤
│ 全局豁免名单(_GLOBAL_ENV_EXACT/PREFIXES):进程/部署级设置     │
│   (PATH/HERMES_HOME/API_SERVER_*/KANBAN 路径)——非 profile 秘密│
├────────────────────────────────────────────────────────────┤
│ build_profile_secret_scope:profile .env + 外部秘密源         │
│   (bitwarden/onepassword/command 等 secret_sources)          │
└────────────────────────────────────────────────────────────┘
```

**核心权衡**:multiplex 开 = fail-closed(未作用域读 = 响亮崩溃,防跨 profile 泄漏);multiplex 关 = 完全向后兼容(单 profile 行为零变化)。设计文档 docs/design/multiplexing-gateway.md。

---

## 二、设计 1:contextvar 作用域(隔离机制)

**位置**:`secret_scope.py:55-87`(ContextVar + set/reset/current)

```
_SECRET_SCOPE: ContextVar[Optional[Mapping[str, str]]] = ContextVar(..., default=None)
set_secret_scope(mapping) → Token(供 reset_secret_scope 恢复)
reset_secret_scope(token) → 恢复前一作用域
current_secret_scope() → 当前映射或 None

传播:contextvar 经 copy_context() 进 worker 线程——与 HERMES_HOME override
  同机制(task 级隔离,线程继承上下文副本)

嵌套:test_nested_scopes_restore 验证(t1=K:a → t2=K:b → reset t2 → K:a)

_MULTIPLEX_ACTIVE = 进程级全局(非 contextvar)——描述部署模式,
  不是 per-task 值;gateway 启动时 set_multiplex_active 一次
```

**正确性价值**:contextvar 天然 task 级隔离(不泄漏到别的回合/线程);reset token 防作用域泄漏。

**产品④映射**:知识库"谁在读凭证"的隔离——contextvar 作用域 + token 恢复;进程级模式标志与 per-task 值分离。

## 设计 2:get_secret 双模式(核心决策)

**位置**:`secret_scope.py:132-186`(get_secret)

```
解析顺序(模块注释逐条):
1. 全局豁免(_is_global_env)→ 恒读 os.environ(部署设置非 profile 秘密)
2. 有作用域:
   - 命中 → 返回
   - 未命中 + multiplex 开 → 返回 default(不落 os.environ——可能另一 profile 值)
   - 未命中 + multiplex 关 → 落 os.environ ★(关键兼容决策):
     "单 profile 部署合法地经进程环境提供凭证(systemd Environment=/
     pass-cli run/op run/plain shell export)而非 <home>/.env;
     且 cron 无条件装 .env 作用域——不落会 401 占位 key"
     (scope 是 .env 覆盖层,不是眼罩)
3. 无作用域:
   - multiplex 关(默认)→ 读 os.environ(与旧 os.getenv 行为完全一致)
   - multiplex 开 → RAISE UnscopedSecretError(响亮失败,防泄漏)

测试(test_secret_scope.py):
- TestMultiplexInactiveBackwardCompat:透明读/缺省/default/无作用域不抛
- TestMultiplexActiveFailClosed:未作用域读抛 / 作用域缺省不落 environ
- TestScopedSingleProfile:作用域命中赢 / 缺省落 environ(multiplex 关)
  /multiplex 开恢复权威
```

**正确性价值**:
1. fail-closed 只开在"真的有跨 profile 风险"时(multiplex)——单 profile 零行为变化
2. scope 未命中落 environ 的兼容性:cron 无条件装作用域,不落 = 401 占位 key 事故
   (注释明确实证)
3. UnscopedSecretError 的修复引导:"wrap in set_secret_scope,not widen allowlist"

**产品④映射**:凭证读的 fail-closed 开关——只在隔离风险真实存在时开启;兼容性 fallthrough 有真实事故驱动(cron 401)。

## 设计 3:全局豁免名单(部署设置 vs profile 秘密)

**位置**:`secret_scope.py:98-129`(_GLOBAL_ENV_EXACT/_PREFIXES/_is_global_env)

```
_EXACT(28 项):
- Hermes 运行时/部署:HERMES_HOME/HERMES_PROFILE/HERMES_GATEWAY_LOCK_DIR/
  HERMES_MAX_ITERATIONS/HERMES_MAX_TOKENS/HERMES_API_TIMEOUT/
  HERMES_REDACT_SECRETS/HERMES_NOUS_TIMEOUT_SECONDS/_HERMES_GATEWAY
- OS/解释器:PATH/HOME/USER/LANG/LC_ALL/TZ/PWD/SHELL/TMPDIR/
  VIRTUAL_ENV/PYTHONPATH/SSL_CERT_FILE
- Kanban 路径(per-board 非 per-profile-secret):HERMES_KANBAN_DB/
  HERMES_KANBAN_WORKSPACES_ROOT/HERMES_KANBAN_BOARD
- API-server 监听器(部署配置,Docker compose environment 块):
  API_SERVER_ENABLED/HOST/PORT/CORS_ORIGINS
  ★ API_SERVER_KEY 刻意不在——它是凭证,保持 profile-scoped(#69379)

_PREFIXES:HERMES_KANBAN_/HERMES_TELEGRAM_(调参旋钮非 token)/TERMINAL_(后端设置)

纪律:"保持名单紧——拿不准就是 profile 秘密,不是全局"(注释明确)

测试:test_api_server_key_stays_profile_scoped(#69379 回归——监听器
  var 读 environ 但 KEY 绝不借跨 profile 值)
```

**正确性价值**:豁免名单是"哪些真的是进程级"的显式契约——拿不准默认是秘密(安全方向);API_SERVER_KEY 与监听器配置刻意分离(凭证 vs 部署配置)。

**产品④映射**:凭证隔离的豁免白名单——部署配置豁免、凭证永不豁免;名单保持紧。

## 设计 4:解析器 round-trip(带引号凭证不损坏)

**位置**:`secret_scope.py:189-223`(_strip_inline_comment)+ `226-269`(load_env_file)

```
load_env_file(不碰 os.environ——隔离是全部意义):
- utf-8-sig 编码(Windows Notepad/PowerShell BOM 不污染首 key——测试锁定)
- export 前缀剥除;全行注释跳过;KEY=VALUE 子集
- _parse_env_value(hermes_cli.config 规范解析器)处理引号

_strip_inline_comment(镜像 python-dotenv 1.2.2 语义,实证验证):
- 引号值:匹配闭合引号(★ 只有双引号反斜杠转义感知——`if quote == '"'`
  secret_scope.py:213,单引号内反斜杠按字面,与 python-dotenv 一致)→
  闭引号后 # 尾注丢弃;"has # inside" # trailing → has # inside
- 非引号值:仅"空白前置的 #"截断——KEY=foo#bar 保留 foo#bar
- 未闭合引号:原样留(:222)

★ 事故驱动(测试注释):旧实现只剥外层引号,凭证含 " 或 \ 交互可用但
  scoped(cron/multiplex)解析损坏——round-trip 字节精确回归锁定

测试(6 用例):
- test_load_env_file_unescapes_quoted_values(round-trip 字节精确)
- test_inline_comment_stripped / test_hash_without_preceding_whitespace
- test_inline_comment_with_escaped_quote_inside_value(\" 不终止值)
- test_round_trip_writer_value_with_trailing_comment
- test_strips_utf8_bom_from_first_key
```

**正确性价值**:解析器与写入器必须 round-trip 字节精确——引号/转义/内联注释三态;破坏只发生在 scoped 路径(交互可用的假象)。

**产品④映射**:知识库凭证解析的 round-trip 纪律——写读同解析器,引号/转义/BOM 三关。

## 设计 5:外部秘密源合并(bitwarden/onepassword/command)

**位置**:`secret_scope.py:272-293`(build_profile_secret_scope)+ `agent/secret_sources/`

```
build_profile_secret_scope(home):
  secrets = load_env_file(home/.env)
  external = get_secret_source_values(home)(env_loader 缓存,
    _SECRET_SOURCE_VALUES_BY_HOME per-home 键控)
  → 非全局 key 合并(外部源覆盖 .env——测试:placeholder 被 bitwarden 值替换)
  → 全局 key 跳过(不复制进 scope,get_secret 直接读 environ)

secret_sources 族(agent/secret_sources/):
  base.py:SecretSource ABC(API_VERSION=1/source_environment contextvar/
    FetchResult/ErrorKind 枚举/is_valid_env_name/scrub_ansi/run_secret_cli
    超时 DEFAULT_FETCH_TIMEOUT=120s/DEFAULT_CLI_TIMEOUT=30s)
  bitwarden.py/onepassword.py/command.py(外部密码管理器源)
  registry.py/_cache.py(发现 + 缓存)

隔离:per-home 键控(_SECRET_SOURCE_VALUES_BY_HOME)——测试:
  test_build_profile_secret_scope_ignores_other_home_external_secrets
  (其他 home 的外部值绝不进入本 profile scope)

> ★ review 补深(2026-08-15 深度 review 发现,两层 contextvar):
> - **source_environment 是第二层 contextvar**(secret_sources/base.py:54-64):
>   registry.py:308 应用外部源时装 env 隔离(外部源 CLI 子进程的环境),
>   与 secret_scope 的 _SECRET_SCOPE 独立——一层管"读什么凭证",
>   一层管"外部源子进程的环境快照"
> - **来源标签安全语义**(env_loader.get_secret_source):返回来源标签
>   ("bitwarden" 等)只作元数据(credential-pool 持久化解释借用来源),
>   "必须绝不视为持久化原始值的授权"——标签是解释,不是许可

**产品④映射**:凭证多源合并——.env 基底 + 外部管理器覆盖 + per-home 隔离;
两层 contextvar(读作用域 + 子进程环境)分离;来源标签永不授权持久化。

---

## 三、与四项目对比(凭证隔离)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes secret_scope |
|------|----|----------|----------|-----|---------------------|
| 凭证存储 | — | — | auth 0600 凭证 | Credential 空值即缺席 | **profile .env + 外部源** |
| 隔离机制 | — | — | — | — | **contextvar 作用域(per-task)** |
| 失败模式 | — | fail-closed | — | — | **multiplex 开 = fail-closed;关 = 透明兼容** |
| 豁免名单 | — | — | — | — | **部署设置豁免(API_SERVER_KEY 永不)** |
| 解析器 | — | — | — | — | **round-trip 字节精确 + BOM/引号/转义** |
| 外部源 | — | — | — | 空值即缺席 | **bitwarden/onepassword/command + per-home 隔离** |

**结论**:产品"凭证隔离"参考 = Hermes secret_scope 全案 + dsh"空值即缺席"(凭证语义)+ OpenCode 0600 权限(存储)。**Hermes 独特贡献:双模式开关(fail-closed 只开在有风险时)+ 豁免名单 + round-trip 解析器**。

---

## 四、面试弹药

1. **"union 进 os.environ = 泄漏"**:multiplex 进程内 profile A 的 key 会出现在 profile B 的回合与 env=dict(os.environ) 子进程——contextvar 作用域是 task 级隔离
2. **"fail-closed 只开在有风险时"**:multiplex 开 = 未作用域读响亮崩溃(UnscopedSecretError);关 = 完全向后兼容(单 profile 零变化)——安全代价只在隔离真实需要时付
3. **"作用域缺省落 environ 是兼容不是漏洞"**:cron 无条件装 .env 作用域,单 profile 凭证经 systemd Environment=/pass-cli run 提供——不落 = 401 占位 key 事故(注释实证)
4. **"API_SERVER_KEY 与监听器配置分离"**:API_SERVER_ENABLED 等是部署配置(豁免),KEY 是凭证(永不豁免)——#69379 容器部署事故驱动
5. **"round-trip 字节精确"**:引号/转义/内联注释三态;旧实现只剥外层引号——凭证含 " 或 \ 交互可用但 scoped 解析损坏
6. **"拿不准就是 profile 秘密"**:豁免名单保持紧——默认安全方向

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| contextvar 作用域 | 凭证 per-task 隔离(线程传播 + token 恢复) |
| 双模式 | fail-closed 只开在有隔离风险时 + 透明兼容 |
| 豁免名单 | 部署配置豁免、凭证永不豁免 |
| round-trip 解析器 | 引号/转义/BOM 三关(事故驱动) |
| 外部源合并 | .env 基底 + 外部管理器覆盖 + per-home 隔离 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_secret_scope.py(22 用例:兼容/fail-closed/覆盖层/嵌套/解析 8/外部源 3/API_SERVER 2)+ test_secret_scope_tier1_migration.py + test_64674_multiplex_primary_token_scope.py + test_credential_pool.py(关联)+ secret_sources 族测试(tests/secret_sources/:conformance/registry/profile_secrets/error_remediation + test_bitwarden_secrets/test_onepassword_secrets/test_command_secret_source)
> 接入点:gateway/run.py:6316(multiplex 主接入,set_multiplex_active)+ cron/scheduler.py:4873(每 job 装作用域,镜像 gateway per-turn 模式)+ 16 个平台/agent 模块 get_secret 调用点(authz_mixin/pairing/api_server 等)
> 设计文档:docs/design/multiplexing-gateway.md(Workstream A)
