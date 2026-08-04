# HANDOFF——从 REQ 到实现

> **上一个会话**：完成全部 13 模块 REQ 需求规格文档（逐文件+逐方法+官方源码对照），约 250 项需求。
> **下一个会话**：以 REQ 为蓝图，从 01 开始逐模块实现代码。

---

## 一、当前状态

### 13 个有效模块（REQ 均已完成并源码验证）

| # | 模块 | REQ | 备注 |
|---|------|:---:|------|
| 01 | confucius-commons | 21 | JDK6 独立项目，Unsafe/JVM Attach 真实存在 |
| 02 | microsphere-java | 31 | Spring 重叠 0%~90% 随包变化 |
| 03 | microsphere-spring | 35 | ~70% 真增量 |
| 04 | spring-boot | 26 | v1 有 3 处 falsified，v2 已修正 |
| 05 | spring-cloud | 28 | 唯一 0 falsified 模块 |
| 07 | sentinel | 18 | 5 类官方缺失适配器 |
| 08 | redis | 19 | Redisson 视角 |
| 13 | mybatis | 18 | MyBatis-Plus 视角 |
| 14 | druid | 7 | Druid 视角 |
| 16 | gateway | 8+12bug | we://协议 + SCG 对照 |
| 17 | multiactive | 6 | SCL 对照 |
| 18 | dynamic | 8 | baomidou 对照 |

### 5 个跳过/废弃的模块

| # | 原因 |
|---|------|
| 06 | thin HTTP wrapper——官方 nacos-client SDK 已有 |
| 09 | 已由其他 AI 完成 |
| 10~12 | 空壳/已移除 |
| 15 | 注解改名 wrapper |

---

## 二、文档位置

所有 REQ 文档在 `tmp-question/training-camp/microsphere-analysis/` 下：

```
01-confucius-commons-analysis/01-REQ-requirements-spec.md
02-microsphere-java-analysis/02-REQ-requirements-spec.md
...
18-microsphere-dynamic-analysis/18-REQ-requirements-spec.md
REQ-INDEX.md  ← 全模块索引 + 从0重实现指南
```

源码在 `tmp-question/training-camp/` 的其他子目录下，如 `cloud-native-code/share/`、`cloud-native-code/stage-4/`。

---

## 三、实现步骤

### 3.1 第一步：读 REQ-INDEX.md

打开 `tmp-question/training-camp/microsphere-analysis/REQ-INDEX.md`——包含了 13 个模块清单、三层 REQ 结构（已实现/待修复/发散）、从 0 重实现指南。

### 3.2 实现顺序

**批次 1（无依赖）**：01-confucius-commons（JDK 底层——先做少量独特能力：ClassLoader、Unsafe、JVM Attach）

**批次 2（依赖 02）**：03-microsphere-spring → 04-spring-boot → 05-spring-cloud

**批次 3（独立中间件）**：07-sentinel → 08-redis(Redisson) → 13-mybatis(MP) → 14-druid → 16-gateway → 17-multiactive → 18-dynamic

### 3.3 每个 REQ 的交付物

- **已实现（REQ-XXX）**：按"产出"描述写代码
- **待修复（REQ-DXX）**：修复 bug
- **发散（REQ-NXX）**：存在现有基础上实现新能力

---

## 四、关键经验

1. **永远不要声称"官方没有 X"而不 grep 验证**——7 次官方能力 falsified 修正
2. **Agent 的 bug 报告必须源码复核**——D09 ValueHolder 缓存被 Agent 错误报告
3. **`git add -A` 危险**——此仓库曾因添加大 PDF 导致 5 次推不上
4. **REQ 视角选择**——薄包装模块应从主流框架视角重写（Redisson/MP/Druid）

---

## 五、Git 信息

- 仓库：`git@github.com:LoveEleve/learn-book.git`
- 当前分支：`main`（孤儿提交 `834a2ff`，零 PDF，427 文件）
- 本地路径：`/data/workspace/source-code/book/成长之路/`
