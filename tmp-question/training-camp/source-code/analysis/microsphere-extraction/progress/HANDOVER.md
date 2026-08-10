# 交接文档 — Microsphere 训练营知识点提取

> **本文件是唯一权威的进度与约定文档。** 接手前请**完整阅读**本文（含 Review 标准、工作方法、当前状态），再动任何文件。
> 时间：2026-08-09

---

## 一、任务背景

对**小马哥（mercyblitz）训练营 + microsphere 生态**做系统性的知识点提取，最终产出：
1. **训练营课程知识点**（stage-1~4 的 119 篇 docs + 示例代码 + 论文）
2. **microsphere 生态源码知识点**（核心代码仓库）
3. **合并的维度化总教学大纲**（按 5 大维度组织）+ 课程↔源码关联分析

**核心视角（08 SOP）**：小马哥课程/源码**只是参考**，不是知识本体。每个知识点走三层次：需求 → 自主实现 → 参考实现 → 对比取舍。

---

## 二、方法论框架（先读这些）

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/microsphere-extraction/
├── index/zh/README.md              ← 入口 + G0 盘问闸 + 执行流程
├── methodology/zh/  (9 SOP)
│   ├── 00-项目清单与依赖.md         ← 两主体清单（本文件已更新）
│   ├── 01-训练营课程提取.md
│   ├── 02-源码逐文件提取.md
│   ├── 03-聚合与深度分类.md        ← 5 大维度
│   ├── 04-过时内容处理.md          ← 三级过时【JDK11/17】
│   ├── 05-聚类与教学顺序.md
│   ├── 06-关联分析.md
│   ├── 07-产出格式.md              ← 三层次记录 + 维度化总大纲
│   └── 08-需求转换与自主实现视角.md ★贯穿视角
├── prompt/zh/self-constraint-prompt.md
├── skills/zh/ (01-快速参考 + grill-me)
└── progress/                       ← 本目录（交接 + 每单元进度）
```

---

## 三、源码与环境位置

### 训练营课程
```
/data/workspace/java-training-camp/stage-{1..4}/docs/   ← 课程 docs（119 篇）
/data/workspace/java-training-camp/stage-1/src/biz-project/       ← stage-1 示例
/data/workspace/java-training-camp/stage-2/src/middleware-projects/ ← stage-2 示例
/data/workspace/java-training-camp/stage-2/papers/                ← 10 篇论文
```

### microsphere 生态源码
```
/data/workspace/source-code/code/microsphere/           ← 主要提取位置
/data/workspace/java-training-camp/cloud-native-code/   ← 更全版本（share/stage-4/projects/stage-3）
```
两处为同一 git remote 副本，以 java 文件多的为准。

### JDK 源码（过时验证用，JDK11/JDK17 基准）
```
/data/workspace/source-code/openjdk11u/               ← JDK11
/data/workspace/source-code/code/spring/jdk17/        ← JDK17（已拉取）
/data/workspace/source-code/openjdk-book/             ← 格物致知 OpenJDK 分析
```

---

## 四、当前状态（2026-08-09）

### 方法论：✅ 已完成并通过多次深度 REVIEW

9 份 SOP + prompt + skills(grill-me) 全部完成。六套核心机制：
1. 两主体清单（课程 + microsphere 官方 36 仓库）
2. 5 大维度（规范/分布式问题/分布式理论/工程问题/性能优化）
3. 三级过时（JDK11/JDK17 基准）
4. 禁止批量操作（执行期质量闸）
5. grill-me 盘问（规划期对齐闸）
6. 需求 + 自主实现视角（贯穿）

### 仓库清单：✅ 已更新（00）

- 官方 36 仓库已核对
- microsphere-test 已拉取（纯 Maven 配置，0 Java）
- 3 个站点/UI 仓库（projects.github.io / .github / devops-ui）跳过
- 本地独有：confucius / shopizer / segmentfault

### 提取：⬜ 尚未开始

**下一步：G0 盘问 → 确认本次提取范围和顺序 → 开始第一个单元**

---

## 五、工作方法（铁律）

1. **G0 盘问（grill-me）**：每个任务/新单元开始前，先盘问"对象/范围/深度/顺序"，达成共识才动手
2. **禁止批量操作**：默认一次只做 1 个单元（一篇 docs / 一个仓库），完成后停下交用户 review
3. **事实自查**：能用 bash/MCP 查到的事实绝不问用户；决策必须等用户
4. **穷尽性**：逐文件/逐篇扫描所有单元，不遗漏
5. **三层次视角**：每个知识点带"需求+自主实现+参考实现+对比取舍"
6. **JDK11/17 验证**：JDK 机制断言用 JDK11/JDK17 源码验证，验证不了标 `[待验证]`
7. **每单元 review 门**：完成后停下交用户确认才继续

---

## 六、产出位置

```
progress/
├── HANDOVER.md            ← 本文（权威进度）
├── course/                ← 训练营课程提取文档（stage-N 各一份）
├── source/                ← microsphere 源码提取文档（每仓库一份）
└── outline/               ← 最终维度化总教学大纲（07）
```

---

## 七、交接约定（切换 session 时）

上下文满了切换 session 时：
1. **写一份新的 `progress/HANDOVER-{session}.md`**，记录本 session 完成的单元、产出、未决问题
2. **更新 `HANDOVER.md`** 的"当前状态"和"已完成单元清单"
3. 下个 session 接手：先读 `HANDOVER.md` → 再读方法论 index → 继续未完成单元
