# Reasonix 域发现 v23 补充(续扫第九轮:pluginpkg manifest/serve auth)— 2026-08-14

> 承接:v22。本轮:internal/pluginpkg(manifest 校验)+ internal/serve/auth(673)。
> 结论:strictDecode 的未知字段拒绝与认证模式确认,深化 ③/安全;无新域。

---

## 一、v23 深化确认

### pluginpkg(插件包)

| 设计 | 位置 | 要点 |
|------|------|------|
| **strictDecode 严格解码** | manifest_v1.go:69-79 | **DisallowUnknownFields**——未知字段拒绝(manifest 漂移静默失败 → 显式失败) |
| **Manifest 全模型** | pluginpkg.go:135-231 | 兼容性检查(Compatibility/Issue)/SkillRef/AgentRef/CommandRef/PromptRef/ThemeRef/HookRef/MCPServerRef;Hook 自定义 UnmarshalJSON |
| **v1 解析** | :47-272 | sniffManifestAPIVersion(嗅探版本)/parseV1PathList/HookMap/MCPServerMap/合并(legacy+contrib) |
| **运行时规范** | :29 | RuntimeSpec |

### serve/auth(服务认证)

| 设计 | 要点 |
|------|------|
| **认证模式** | NormalizeAuthMode(模式规范化) |
| **限流** | rateLimit(IP 窗口 allow + cleanupLoop) |
| **密码认证** | HashPassword/sessionKeyForPasswordHash(密码哈希 → session key) |
| **token bootstrap** | tokenBootstrapPublicPath(公开路径引导) |

---

## 二、关键设计(通用价值)

1. **"未知字段拒绝"**:manifest 解码 DisallowUnknownFields——**配置严格性**(与 Pi 类型白名单解码、协议拒绝有损转换同哲学——"静默忽略 = 静默漂移")
2. **"manifest 版本嗅探"**:sniffManifestAPIVersion——**多版本兼容的显式嗅探**
3. **"密码哈希 → session key"**:认证的确定性推导

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v22 | — | 102 | 102 |
| v23 | pluginpkg/serve auth | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 extension 剩余(protocol validate/publish/runtimeplan)、appidentity、i18n——按需。
