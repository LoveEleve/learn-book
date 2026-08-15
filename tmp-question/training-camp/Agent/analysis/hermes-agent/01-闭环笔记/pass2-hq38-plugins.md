# hq38 插件系统(Plugin System)— 产品②"扩展"蓝本

> 项目:Hermes(hermes_cli/plugins.py 6,318 行 + tests/test_plugin*.py 534 用例)
> 假设:新能力走插件不进核心——Hermes 的四源插件发现 + 生命周期钩子 + 工具注册是"扩展"的完整样本(域发现 v9:plugin.yaml + 事件订阅 + PluginState 配额)。
> 结论:✅ 成立——四源发现/生命周期钩子/工具注册/系统提示段落/作用域加载/兼容契约全具备,产品②"扩展面"直接蓝本。

---

## 一、架构全景:四源插件发现

```
┌────────────────────────────────────────────────────────────┐
│ 四源发现(模块头):                                         │
│   1. Bundled:<repo>/plugins/<name>/                        │
│   2. User:~/.hermes/plugins/<name>/                        │
│   3. Project:./.hermes/plugins/(opt-in 环境开关)           │
│   4. Pip:hermes_agent.plugins entry point                  │
│   ——同名的用户插件替换 bundled                            │
├────────────────────────────────────────────────────────────┤
│ PluginManager(3388):发现/加载/管理                        │
│   _discover_and_load_inner(3865)/_load_plugin(4590)/       │
│   _load_plugin_scoped(4595,作用域加载)                     │
│   discover_plugins(5505,幂等)                              │
├────────────────────────────────────────────────────────────┤
│ 能力:                                                    │
│   生命周期钩子(pre/post tool/llm/session——model_tools/     │
│     run_agent 调用)                                       │
│   工具注册(ctx.register_tool)                              │
│   系统提示段落(format_system_prompt_section,520)           │
│   CLI 子命令(ctx.register_cli_command)                     │
├────────────────────────────────────────────────────────────┤
│ 契约:PluginManifest/PluginState 配额/作用域/兼容契约       │
│   (website/docs/developer-guide/plugins/index.md)          │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:四源发现(同名替换)

**位置**:`plugins.py:1-25`(模块头)+ `407`(discover_entrypoint_manifests)

```
四源:bundled/user/project(pip 可选)/pip entry point
——同名的用户插件替换 bundled(最后写赢,用户覆盖)

discover_entrypoint_manifests(407):pip 源清单发现
_classify_entrypoint_value_kind(475):入口点值分类
```

**正确性价值**:多源 + 用户覆盖(bundled 可被替换)——扩展面灵活。

**产品④映射**:插件发现多源——用户覆盖 bundled。

## 设计 2:生命周期钩子(能力面)

**位置**:`plugins.py`(钩子族)+ 调用方(model_tools/run_agent)

```
生命周期钩子:
  pre_tool_call/post_tool_call/pre_llm_call/post_llm_call/
  on_session_start/on_session_end
  ——model_tools.py(pre/post tool)+ run_agent.py(生命周期)调用

工具注册:ctx.register_tool(新工具)
CLI 子命令:ctx.register_cli_command(argparse 树接入 hermes)
系统提示段落:format_system_prompt_section(520)/sections(545)
```

**正确性价值**:能力面全(钩子/工具/CLI/prompt 段落)——插件不碰核心文件。

**产品④映射**:扩展面完整——钩子/工具/CLI/prompt 段落,核心零改动。

## 设计 3:作用域加载 + 兼容契约

**位置**:`plugins.py:4590`(_load_plugin)+ `4595`(_load_plugin_scoped)+ 兼容契约

```
_load_plugin_scoped:作用域加载(隔离/配额)
PluginState 配额(域发现 v9)

兼容契约(website/docs/developer-guide/plugins/index.md):
  "Compatibility is enforced as a behavior contract,not through a
  monolithic PLUGIN_API_VERSION"——行为契约而非版本字面量
  (新增钩子 payload 关键词字段;签名内省旧窄签名只收声明字段;
   忽略未知 manifest 字段;新 provider 方法默认实现)
```

**正确性价值**:行为契约兼容(非版本字面量)——旧插件不因新核心损坏。

**产品④映射**:插件兼容契约——行为契约 + 签名内省(非版本字面量)。

## 设计 4:发现时机坑 + 幂等

**位置**:`plugins.py:5505`(discover_plugins)

```
★ 发现时机坑(AGENTS.md):discover_plugins() 只在 import model_tools.py
  的副作用运行——读插件状态不 import model_tools 的代码路径必须显式调
  (幂等)
```

**正确性价值**:发现时机显式(副作用 vs 显式调用)——防"读了没发现"。

**产品④映射**:插件发现幂等 + 时机显式。

---

## 三、与四项目对比(扩展)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes plugins |
|------|----|----------|----------|-----|----------------|
| 扩展面 | 扩展系统 | 插件宿主 | 插件窄能力 | **能力缝三角色** | **四源 + 钩子/工具/CLI/prompt** |
| 发现 | — | — | — | 声明合并 | **四源同名替换** |
| 兼容 | — | — | Zod 边界 | 版本机制 | **行为契约(非版本字面量)** |
| 隔离 | — | — | — | 可逆效果 | **作用域加载 + 配额** |

**结论**:产品"扩展"参考 = Hermes plugins(四源 + 行为契约 + 作用域)+ dsh 能力缝(组件插件化)。**Hermes 与 dsh 同"扩展不进核心"哲学;Hermes 是钩子面,dsh 是服务面**。

---

## 四、面试弹药

1. **"扩展不进核心"**:钩子/工具/CLI/prompt 段落——插件不碰核心文件(核心窄腰)
2. **"四源同名替换"**:用户插件替换 bundled——最后写赢
3. **"行为契约非版本字面量"**:签名内省旧窄签名只收声明字段——旧插件不因新核心损坏
4. **"发现时机坑"**:discover_plugins 是 import 副作用——读插件状态必须显式调(幂等)
5. **"作用域加载 + 配额"**:隔离 + PluginState 配额——插件资源有界

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 四源发现 | 扩展多源 + 用户覆盖 |
| 生命周期钩子 | 钩子/工具/CLI/prompt 能力面 |
| 行为契约兼容 | 非版本字面量(旧插件不坏) |
| 作用域 + 配额 | 插件资源有界 |
| 发现幂等 | 时机显式 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:tests/test_plugin*.py 534 用例(plugin_context_references/plugin_llm/plugin_llm_task_routing/plugin_prompt_sections 等)
> 位置:PluginManager :3388 / discover_plugins :5505 / _load_plugin_scoped :4595 / discover_entrypoint_manifests :407 / format_system_prompt_section :520
> 契约:website/docs/developer-guide/plugins/index.md(行为契约 + 兼容策略)
