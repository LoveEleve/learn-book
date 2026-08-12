# HANDOFF — 主交接文档(唯一入口,非常详细版)

> **状态**: 2026-08-11 | 卷 2 写作进行中(第 1 批,6/13 篇完成) | 上下文已满,本文件为**非常详细交接版**——新 AI 只读本文件即可继续,不必读其他文档
> **接收者: 新 AI —— 只读本文件,按"第一步"执行**

---

## 〇、三十秒总览(先读这个)

**项目**: 写一本 OpenJDK 源码分析书("格物致知"),源码树 = jdk11u(`/data/workspace/jdk11u/src/hotspot/`)。

**当前正在做**: 卷 2(按 48 域规划写源码文章),每篇严格按方法论: 读大纲 → **所有行号重新 grep 验证** → 写 → 代码块与源码逐字核对 → 深审 → 提交。

**下一步(唯一,无选择)**: 45-math-library 域 → 48-utils 域 → 第 2 批 02-assembler(依拓扑)。

**铁律**: ① 一篇一篇写,写完自查合格再下一篇;② 大纲/KP 的行号是"线索不是事实",写作时必须重 grep;③ 代码块贴真实源码(截取可,编造不可);④ 每篇写完整理后做深审。

---

## 一、项目全貌

| 卷 | 位置 | 状态 |
|---|---|---|
| 卷 0 地基 | `docs/openjdk/vol-00/`(4 章) | ✅ 旧会话完成,不动 |
| 卷 T 工具观测 | `docs/openjdk/vol-tools/ch01-07.md` | ✅ 本会话完成(7 篇,深审 32 处修正),写作时引用其素材做实证 |
| 卷 1-bak 启动 | `docs/openjdk/vol-01-bak/`(14 章) | ✅ 归档(旧会话的启动链叙事,新写作不沿用此结构) |
| **卷 2 运行时深处** | `docs/openjdk/vol-02/` | 🚧 **当前任务**,按 48 域依赖拓扑写 |
| 域规划 | `docs/openjdk/planning/` | 48 域权威清单(00-domain-discovery-v3.md)+ 每域 KP(knowledge-planning/0X-*.md)+ 每域大纲(planning/outlines/0X-*/) |
| 工具素材库 | `docs/openjdk/planning/outlines/00-jvm-tools/materials/` | ✅ 130+ 命令输出/21 截图/10 JFR 录制(gitignore,不入库) |

**git 仓库**: `/data/workspace/source-code/openjdk-book/`(remote: git@github.com:LoveEleve/openjdk-book.git,main 分支,每篇一提交一推送)

---

## 二、卷 2 写作进度(精确到篇)

**写作顺序依据**: `docs/openjdk/planning/knowledge-planning/00-domain-writing-order.md`(48 域依赖拓扑 7 层,脚本验证自洽)

```
第 1 批(地基):     01 → 05 → 45 → 48      ← 当前在这: 01✅ 05✅ 45 进行中
第 2 批(原语):     02 → 03 → 04 → 06 → 16 → 38 → 41 → 42
第 3 批(对象/类):  07 → 09 → 17
第 4 批(执行/帧):  10 → 19 → 23 → 24 → 08 → 31 → 44
第 5 批(VM 核心):  11 → 12 → 13 → 18 → 20 → 27 → 30 → 32 → 34 → 36 → 37 → 39 → 46
第 6 批(JIT/GC):   14 → 15 → 21 → 25 → 28 → 29 → 33 → 43
第 7 批(上层):     22 → 26 → 35 → 40 → 47
```

**已完成 6 篇**(全部在 `docs/openjdk/vol-02/`):

| 域 | 篇 | 文件 | 行数 |
|---|---|---|---|
| 01-os | 1 | `01-os/01-platform-detection.md`(平台探测) | 294 |
| 01-os | 2 | `01-os/02-virtual-memory.md`(虚拟内存四态) | 203 |
| 01-os | 3 | `01-os/03-threads-and-sync.md`(7 种线程/优先级/Event) | 219 |
| 01-os | 4 | `01-os/04-signals-and-safepoint.md`(SIGSEGV 五阶段) | 165 |
| 05-cpu | 1 | `05-cpu-primitives/01-atomic-and-memory-order.md` | 123 |
| 05-cpu | 2 | `05-cpu-primitives/02-safefetch-and-platform.md` | 122 |

**每篇 commit 号**: 01=e8e9e92, 02=c3627f1, 03=46a85e2, 04=d909938, 05=884c66e, 06=74352ee(+各自深审修正 commit)

---

## 三、每篇写作流程(严格执行,不可省略)

```
1. 读大纲: planning/outlines/<NN>-<域>/<NN>-<篇>.md
2. 读 KP: planning/knowledge-planning/<NN>-<域>.md(若大纲信息不足)
3. 【铁律】验证大纲里所有 file:line —— 逐个 grep/sed 核对,发现漂移用真实行号
4. 写正文到 vol-02/<NN>-<域>/<NN>-<篇>.md
5. 自查:
   - 代码块与源码逐字核对(sed 对比)
   - 五维检查表(见下)
   - 工具实证引用必须真实存在(materials/ 里 grep 到才引用)
6. git add + commit + push(信息: 域/篇/深审修正清单)
7. 更新 vol-02/README.md 勾选进度
```

**五维检查表**(每篇必过):
| 维度 | 检查项 |
|---|---|
| 场景 | 每节开头有场景句/问题句 |
| 源码 | 每个机制有 file:line + 函数名,全部 grep 验证 |
| 关键设计 | 每节有"关键设计(斜体)"解释 why |
| 跨层 | [C++:][x86:][内核:][man N xxx] 标注 |
| 悬念 | 结尾"核心悬念" + OUTBOUND 桥到下一篇/下一域 |

---

## 四、方法论体系(写作规范全集)

### 4.1 WRITING-GUIDELINES.md(`docs/openjdk/WRITING-GUIDELINES.md`,10 条)
1. 去 AI 味(写的是书,不是文档;无模板标题/无✅❌符号)
2. **依赖驱动排序(A 依赖 B,先写 B)** ← 48 域拓扑的依据
3. 从问题开始(先"为什么"再"是什么")
4. 禁止前向引用("后面会讲"=结构错了)
5. 构造/运行时严格分离
6. 一个概念改三轮讲不通 → 删除(多半是前置缺失,后置再写)
7. 说人话(先人话再源码术语)
8. **每句陈述有源码依据** + 书稿代码块纪律(见 4.3)
9. 画流程图(时间线/数据流)

### 4.2 v5 文章格式(HANDOFF-NEW-AI.md §一)
- 每机制段落四要素: 场景 → 技术描述(file:line+函数名+调用链) → 关键设计(why) → 跨层标注
- 巨型域(>30K 行或 >100 文件)拆 6-8 篇,分段写作: 3-4 篇 → pause 自查 → 补齐
- 已知巨型域: C2(177K)/G1(46K)/GC Framework(37K)/OOPs(38K)/ClassFile(46K)/JFR(217 文件)

### 4.3 书稿代码块纪律(教训 #14/#15,血泪总结)
1. **代码块 = 真实源码**: 截取可(省略模板/错误处理),核心语句逐字,禁止凭记忆写伪代码
2. **行号写作时重新 grep**: 大纲/KP 是规划期产物(2026-08-08),行号大量漂移——**每篇实测都有 2-6 处漂移**,必须重验
3. **默认值/开关查 globals.hpp**: 如 UseContainerCpuShares=false、Tier2CompileThreshold=0、MaxPriority→nice=-5
4. **自查命令**: 文章每个 file:line 逐个 sed -n;代码块与源码 diff
5. 实测教训案例(已发生,避免重犯):
   - os.cpp:1744 伪代码(实际 3 行直接赋值)
   - "uncommit 用 madvise(MADV_DONTNEED)"(jdk11u 根本不存在,实际 mmap PROT_NONE 重映射)
   - "WatcherThread Critical=11"(实际 MaxPriority=10,Critical 仅限并发 GC 线程)
   - "PER_CPU_SHARES 归一化"(实际 1024→-1 未设置)
   - "RegisterMap bit 标记 OOP"(实际是位置表 _location[] + 有效性位图,与 oop map 配合)
   - "prefetch 用 prefetchnta"(实际只有 prefetcht0,prefetchw 被注释)

### 4.4 深审缺陷档案(`issue/源码分析深审缺陷档案.md`,已扩到 15 类)
- A 内容层: #1 事实错误 / #2 API 编造 / #3 文件名推断 / #4 跨项目转移 / #5 覆盖率 / #6 跨层不一致
- B 结构层: #7 文字锚 / #8 篇节标注 / #9 表格格式 / #10 数字自洽
- C 过程层: #11 范围规划不可信 / #12 待确认清零 / #13 一次一域 / **#14 书稿代码块编造(写作期高发)** / **#15 大纲描述与源码漂移**

---

## 五、素材库(写作时引用实证)

| 素材 | 位置 | 用途 |
|---|---|---|
| 素材索引 | `planning/outlines/00-jvm-tools/materials/INDEX.md` | 按域查素材的入口 |
| JFR 录制 | `materials/jfr-recordings/rec-demo.jfr` 等 10 个 | 事件计数实证(如 SafepointBegin 2710) |
| 命令输出 | `materials/commands/` 130+ 文件 | jcmd/jstat/jmap 等真实输出 |
| 截图 | `materials/screenshots/` 21 张 | 已复制进文章 assets 的用 assets |
| 卷 T 文章 | `vol-tools/ch01-07.md` | 引用格式: "[卷 T ch05](openjdk/vol-tools/ch05.md) 的 Environment 页" |

**引用纪律**: 工具实证必须真实存在——引用前 grep materials/ 验证(曾犯错误: 说 VM.info 显示"JVM_handle_linux_signal 地址",实际是 "javaSignalHandler in libjvm.so")。

---

## 六、用户偏好与纪律(重要,违背会被批评)

1. **严格按规划,不做多余选择**: 拓扑定了顺序就逐项推进;写域内第几篇也是大纲定的——不要问"还是写 X?"(曾因制造选择被批评)
2. **每篇都做深度 REVIEW**: 用户会要求"按照方法论深度的 REVIEW",写完后主动自查,不要等
3. **一篇一篇写**: 不并行、不跳步
4. **数字/事实必须验证**: 任何带数字的陈述回源码/素材验证,禁止"凭记忆"
5. **命名混淆注意**: "域 01"与"05 域的第 01 篇"都带 01,表述时写清"域 XX 第 Y 篇"
6. 中文交流,提交信息用中文

---

## 七、待办清单(按优先级)

- [ ] **45-math-library 域**(第 1 批第 3 个): 大纲在 `planning/outlines/45-math-library/`(需先 ls 看有几篇)
- [ ] **48-utils 域**(第 1 批第 4 个,收尾第 1 批)
- [ ] 第 2 批: 02-assembler → 03-flags → 04-logging → 06-oops → 16 → 38 → 41 → 42
- [ ] **用户 Ubuntu GUI 截图**(8 项 14 张,手册在 `planning/outlines/00-jvm-tools/GUI-manual.md`): 用户完成后补进对应文章
- [ ] Obsidian 知识图谱(想法记录在 `planning/IDEAS-OBSIDIAN.md`,副本操作方案,远期)
- [ ] 每域完成后在 `vol-02/README.md` 勾选进度

---

## 八、关键路径速查

| 东西 | 路径 |
|---|---|
| 写作顺序权威依据 | `docs/openjdk/planning/knowledge-planning/00-domain-writing-order.md` |
| 48 域权威清单 | `docs/openjdk/planning/00-domain-discovery-v3.md` |
| 每域 KP | `docs/openjdk/planning/knowledge-planning/<NN>-<域>.md` |
| 每域大纲 | `docs/openjdk/planning/outlines/<NN>-<域>/` |
| 写作指南 | `docs/openjdk/WRITING-GUIDELINES.md` |
| 规划总览 | `docs/openjdk/planning/HANDOFF-NEW-AI.md` |
| 深审缺陷档案 | `issue/源码分析深审缺陷档案.md`(15 类) |
| 卷 2 进度 | `docs/openjdk/vol-02/README.md` |
| 源码树(jdk11u) | `/data/workspace/jdk11u/src/hotspot/` |
| 工具素材 | `docs/openjdk/planning/outlines/00-jvm-tools/materials/` |
| JDK 工具 | `/opt/codev/TencentKona/bin/`(17.0.8.1) |
| GUI 手册 | `docs/openjdk/planning/outlines/00-jvm-tools/GUI-manual.md` |

---

## 九、下一步(读完立即做)

```
1. 读 planning/outlines/45-math-library/ 下的大纲(先 ls 看篇数)
2. 读 planning/knowledge-planning/45-math-library.md(KP)
3. 按第三节流程写第一篇: 验证锚点 → 写 → 自查 → 提交 → 更新进度
4. 写完 45 域全部篇 → 48-utils 域 → 第 2 批
```

**环境**: Linux 容器,无显示器(GUI 截图等用户在 Ubuntu 补);jdk11u 源码在本地;git 推送即部署(docsify 站点)。
