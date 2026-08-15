# Hermes 域发现 v19 补充(续扫第八轮:update_cmd/models/tools_config)— 2026-08-14

> 承接:v18。本轮:hermes_cli/(update_cmd 5,893/models 5,752/tools_config 5,553)。
> 结论:更新后验证与模型目录合并确认,无新域。

---

## 一、v19 深化确认

### update_cmd(5,893)

| 设计 | 位置 | 要点 |
|------|------|------|
| **更新后验证** | :171-301 | _validate_critical_files_syntax/_validate_critical_modules_import——**更新后关键文件语法/导入验证**(防更新破坏) |
| **模块热重载** | :64-115 | _reload_updated_runtime_modules/_reload_config_modules——更新后重载 |
| **更新后提示族** | :384-537 | curator 首跑/FTS 优化/最近运行提示 |

### models.py(5,752)

| 设计 | 要点 |
|------|------|
| **策展模型目录** | _codex_curated_models/_xai_curated_models/_xai_promote_top/_xai_merge_curated_extras——**策展提升/合并** |
| **免费层判定** | _is_model_free/is_nous_free_tier/partition_nous_models_by_tier——**免费/付费分区** |
| **推荐并集** | union_with_portal_free/paid_recommendations |

### tools_config(5,553)

| 设计 | 要点 |
|------|------|
| **平台工具集允许** | _toolset_allowed_for_platform/_toolset_configuration_platform——**每平台工具集配置** |
| **凭证存在检测** | _xai_credentials_present/_homeassistant_credentials_present——工具集可用性门 |
| **CUA 驱动解析** | _cua_driver_cmd/_resolved_cua_driver_cmd——计算机使用驱动 |

---

## 二、关键设计(通用价值)

1. **"更新后验证"**:语法+导入验证防更新破坏——**升级安全**(与 Reasonix repair 自修复同族)
2. **"凭证门工具集"**:凭证存在才显示工具集——**可用性驱动的配置**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v18 | — | 80 | 80 |
| v19 | update_cmd/models/tools_config | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 cron/scheduler(5,432)细看/browser_tool(5,383)/approval(4,919)细看——按需。
