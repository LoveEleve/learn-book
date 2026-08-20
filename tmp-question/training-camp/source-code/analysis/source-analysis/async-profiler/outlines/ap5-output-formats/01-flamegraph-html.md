# 01. 采样数据怎么变成一张 HTML? — 火焰图生成

> 🔴 Deep | 12 KP 中的 3 个(Trie 树/minwidth/INCBIN)
> 读者处境: 你看到的交互式火焰图,是 async-profiler 自己生成的 HTML——从采样栈到 `<div>` 树,中间是前缀树(Trie)+ 阈值过滤 + 模板嵌入。

### 1. "模板长在二进制里" — INCBIN 嵌入

场景: build 出来的 .so 只有一个文件,HTML 模板放哪?

- `INCBIN(FLAMEGRAPH_TEMPLATE, "src/res/flame.html")`(flameGraph.cpp:17)——**编译期把 flame.html 模板转成二进制数组嵌入**
- [C++: INCBIN 宏用汇编 `.incbin` 指令把文件塞进 .data 段——运行期零文件依赖、零 IO]

关键设计: **单文件部署**: agent 库自带模板——目标机不需要任何资源文件(与 AP-1 的"静态链接自包含"哲学一脉相承: 能塞进二进制就不带文件)。

### 2. "合并相同调用点" — Trie 树构建

场景: 100 万条采样栈,火焰图只有一个节点每个调用点。

- `FlameGraph::addChild`(flameGraph.cpp:82): 按栈 ID 逐帧走 Trie——**相同 (父节点, 帧名) 复用节点**,`f->_total += value`(:92)累加计数
- 数据源: AP-4 篇 4 的 CallTraceStorage(存储已去重,这里再合并成树)
- `dump`(:109): 深度计算 + 输出
- [数据结构: Trie(前缀树)——调用栈天然是前缀结构,父帧相同则共享路径;这就是火焰图"宽块=高占比"的数学基础]

关键设计: **两层合并**: 存储层按"完整栈"去重(AP-4),输出层按"栈前缀"合并(Trie)——第二层才是火焰图形状的来源。`_minwidth` 阈值(:111,`_root._total * _minwidth / 100`)过滤占比低于 0.1% 的细帧——**"宽而不热"的噪声帧在此被滤掉**(AP-0 篇 2 的 `--minwidth` 参数落点)。

### 3. "帧的类型标记" — printFrame 与 [inlined]

场景: 火焰图里黄色(Java)/绿色(JIT)/`[inlined]` 怎么来的?

- `printFrame`(flameGraph.cpp:149): 输出 HTML 帧;`_inlined < _total && _interpreted < _total`(:152)判定是否纯内联/解释帧
- 颜色/标记来源: AP-4 篇 3 的 typeSuffix(帧类型)在树节点上累积
- [HTML: 输出是 div 嵌套(块宽=占比)——交互(点击缩放/搜索)靠模板里的 JS]

关键设计: **"名字+类型+计数"三要素**: 每个树节点 = 帧名(Trie key)+ 类型(颜色)+ 计数(宽度)——火焰图的可视化语义完全由这三要素决定。`[inlined]` 标记让"JIT 内联但占了时间"的帧可见(AP-3 篇 3 的 walkVM 展开在此呈现)。

---

跨域桥: 数据源 = AP-4 篇 4(CallTraceStorage);帧类型 = AP-4 篇 3(typeSuffix);读法 = AP-0 篇 3(宽块/颜色/内联);存储去重 = AP-4 篇 2。

**OpenJDK 关联**: 火焰图 HTML 为自研格式,无 JVM 对应;概念可对照 [域 32 JFR — outlines/32-jfr/](事件聚合)。
