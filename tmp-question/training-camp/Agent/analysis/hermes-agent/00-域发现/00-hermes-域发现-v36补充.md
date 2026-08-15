# Hermes 域发现 v36 补充(续扫第二十五轮:顶层/时区/引导)— 2026-08-14

> 承接:v35。本轮:顶层剩余(hermes_bootstrap/hermes_time/mcp_serve 权限面)。
> 结论:Windows UTF-8 引导与时区时钟确认,无新域。

---

## 一、v36 深化确认

| 文件 | 设计要点 |
|------|---------|
| **hermes_bootstrap(239)** | **Windows UTF-8 双修复**:PEP 540(PYTHONUTF8=1 子进程继承)+ 当前进程 sys.stdout/stderr reconfigure;**每个入口点最顶部导入**(任何文件 I/O/print 前);POSIX 不触碰——**跨平台编码正确性** |
| **hermes_time(135)** | 时区感知时钟:env(HERMES_TIMEZONE)→ config → 本地降级;**坏时区值警告+安全回退**(永不因坏字符串崩溃) |
| **mcp_serve(1,037)** | OpenClaw 9 工具桥(conversations/messages/events/permissions 面,v7 已记)——权限面 permissions_list_open/respond 确认 |

---

## 二、关键设计(通用价值)

1. **"编码引导先于一切"**:Windows UTF-8 在入口最顶部设置(子进程 env + 当前进程)——**跨平台编码的引导顺序**(与 Reasonix psUTF8Prologue 同思想)
2. **"时区安全降级"**:坏时区值不崩溃——**配置容错**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v35 | — | 81 | 81 |
| v36 | 顶层/时区/引导 | +0(深化 2 设计) | **81**(深化) |

> 继续:next 轮 400 行以下中小文件抽查(agent/tools/gateway 剩余)——按需收尾。
