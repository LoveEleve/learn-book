# Hermes 域发现 v18 补充(续扫第七轮:plugins.py 完整/slash_commands)— 2026-08-14

> 承接:v17。本轮:hermes_cli/plugins.py(6,318)细看 + gateway/slash_commands.py(5,772)。
> 结论:插件依赖拓扑排序确认,无新域。

---

## 一、v18 深化确认

### plugins.py(6,318)

| 设计 | 位置 | 要点 |
|------|------|------|
| **依赖拓扑排序** | :854-920 | graphlib.TopologicalSorter:A 需 B → B 先注册;循环检测 → 回退字母序;**缺失依赖警告不硬失败**(loads 永不因 advisory 依赖失败,ctx.has_plugin 运行时探测);自依赖忽略 |
| **v2 manifest 解析** | :683 | _parse_manifest_v2_fields |
| **入口点发现** | :407-475 | discover_entrypoint_manifests/_classify_entrypoint_value_kind |
| **模块来源检测** | :922-1021 | _detect_kind_from_source/resolve_module_origin/_resolve_module_source(限 8192) |
| **system prompt 段落** | :506-543 | is_valid_system_prompt_section_id/format_system_prompt_sections——**插件可贡献 system prompt 段落** |
| **启用/禁用** | :565-621 | _env_enabled/_get_disabled_plugins/_get_enabled_plugins |

### slash_commands(5,772)

| 设计 | 要点 |
|------|------|
| GatewaySlashCommandsMixin | 网关斜杠命令(mixin,模型切换偏斜守卫/线程解析) |

---

## 二、关键设计(通用价值)

1. **"依赖缺失不硬失败"**:advisory 依赖缺失 → 警告 + 运行时探测——**依赖的宽容加载**(防单依赖缺失禁用整个插件)
2. **"循环回退字母序"**:依赖环 → 确定性回退——**依赖图的退化路径**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v17 | — | 80 | 80 |
| v18 | plugins.py/slash_commands | +0(深化 2 设计) | **80**(深化) |

> 继续:next 轮 update_cmd(5,893)/models.py(5,752)/tools_config(5,553)——按需收尾。
