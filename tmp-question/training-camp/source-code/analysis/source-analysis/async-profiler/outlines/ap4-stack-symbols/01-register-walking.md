# 01. 只有 PC 和寄存器,怎么走出调用链? — 寄存器行走

> 🔴 Deep | 13 KP 中的 2 个(寄存器行走/序言识别)
> 读者处境: 信号处理器拿到 ucontext——里面有被打断线程的所有寄存器。怎么从 RSP/RBP/PC 恢复出完整调用链?

### 1. "寄存器的读法" — StackFrame 访问器

场景: ucontext 是平台相关的裸结构,StackFrame 统一读法。

- `StackFrame::pc/sp/fp/link/arg0-3/jarg0`(src/stackFrame_x64.cpp:21-58)——从 ucontext 提取各寄存器:`fp()` = `REG(RBP, rbp)`(:30);`link()`(:37)= frame link(前一帧返回地址)
- 每个 CPU 架构一个 stackFrame_*.cpp(x64/aarch64 保留,其余淘汰)——**寄存器布局的架构抽象**
- [x86-64: 调用约定——RBP=当前帧基址,RSP=栈顶,PC=指令指针;frame link 是栈上存的"调用者返回地址"]

关键设计: **ucontext 是唯一信息源**: 信号处理器没有栈元数据,只有被打断线程的寄存器快照——StackFrame 把这堆寄存器组织成"行走接口"(pc/sp/fp),行走逻辑与平台解耦(AP-3 篇 3 的 walkFP 用它)。

### 2. "帧与帧怎么串起来" — frame pointer 链与序言识别

场景: 每帧的 fp 指向哪?怎么知道"这帧到底建没建 frame pointer"?

- 经典链: `push rbp; mov rbp, rsp` 序言 → fp 链: `fp[0]`=上一帧 fp,`fp[1]`=返回地址
- **序言识别**(stackFrame_x64.cpp:123-127): 检查指令 `*ip == 0x55`(`push rbp` 机器码)——判定函数入口是否建帧
- **叶函数补偿**(:142-155): 被优化的叶函数**不建 frame pointer**——识别 `pop rbp` 等尾部指令,用 sp 链补走
- [x86: 0x55 是 `push %rbp` 的操作码;GCC/Clang 默认 -O2 会省略叶函数的帧指针(frame pointer omission)——这是行走器的第一道坎]

关键设计: **"编译器知识"内嵌**: 行走器不认识高级语言,只认机器码模式——`push rbp`/`pop rbp` 模式 + frameSize 判断,就是"识别优化边界"的全部。这也是为什么 frame pointer 模式(FramePointers)是默认、DWARF 是回退(AP-3 篇 3)。

### 3. "行走的边界" — max_depth 与安全

场景: 栈损坏/野指针——行走器怎么不崩?

- `walkFP(ucontext, callchain, max_depth)`(stackWalker.cpp:73)——深度上限 + 地址合法性校验
- 守护: `MAX_WALK_SIZE`(stackWalker.cpp:47)——栈范围判定(tcb 附近),防止走到内存任意处
- [安全: 信号处理器崩溃 = 采样线程崩溃;栈指针被信号栈污染时,范围检查是最后防线]

关键设计: **"宁可少走,不可走错"**: max_depth 防无限链,栈范围防野指针——行走器在**不可信输入**(用户栈)上做防御性遍历,这是采样器稳定性的底线。

---

跨域桥: 行走模式选择 = AP-3 篇 3(walkFP/Dwarf/VM);行走结果消费 = 本篇 02(符号解析);架构抽象 = stackFrame_aarch64 同构。

**OpenJDK 关联**: [域 24 Frame & Stack — outlines/24-frame-stack/] — frame pointer 链与 JVM 物理帧布局。
