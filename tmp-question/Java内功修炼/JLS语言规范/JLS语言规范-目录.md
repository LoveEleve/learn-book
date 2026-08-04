# 《The Java Language Specification》JLS Java SE 11 — 完整目录

> 19章 + 附录 | Java语言规范权威标准 | 词法→类型→名字→类/接口→数组→异常→执行→兼容性→语句→表达式→赋值→线程与锁→类型推断

---

### 第1章 Introduction 简介
1.1 Organization / Example Programs / Notation / References

### 第2章 Grammars 文法
2.1 Context-Free Grammars / Lexical Grammar / Syntactic Grammar

### 第3章 Lexical Structure 词法结构
3.1 Unicode / Unicode Escapes / Line Terminators / Tokens
3.2 White Space / Comments / Identifiers / Keywords / Separators / Operators
3.3 Literals（整数/浮点/布尔/字符/字符串/转义序列/null）

### 第4章 Types, Values, and Variables 类型、值与变量
4.1 Kinds of Types and Values
4.2 Primitive Types（整数/浮点/boolean）
4.3 Reference Types（Object/String/同一性）
4.4 Type Variables / Parameterized Types（类型实参/构造器）
4.5 Type Erasure / Reifiable Types / Raw Types / Intersection Types
4.6 Subtyping（原始类型/类和接口/数组/Least Upper Bound/Type Projections）
4.7 Variables（原始类型/引用类型/变量种类/final/初始值）

### 第5章 Conversions and Contexts 转换与上下文
5.1 Kinds of Conversion（Identity/Widening/Narrowing/Boxing/Unboxing/Unchecked/Capture/String/Forbidden）
5.2 Assignment Contexts / Invocation Contexts / String Contexts / Casting Contexts
5.3 Numeric Promotions（Unary/Binary）

### 第6章 Names 名字
6.1 Declarations / Names and Identifiers / Scope / Shadowing and Obscuring
6.2 Determining the Meaning of a Name（模块名/包名/类型名/表达式名/方法名）
6.3 Access Control（Determining Accessibility/protected细节）
6.4 Fully Qualified Names and Canonical Names

### 第7章 Packages and Modules 包与模块
7.1 Package Members / Compilation Units
7.2 Package Declarations（命名包/未命名包/可观察性与可见性）
7.3 Import Declarations（单类型/按需类型/单静态/按需静态）
7.4 Module Declarations（依赖/导出与开放包/服务消费与提供/未命名模块/可观察性）

### 第8章 Classes 类
8.1 Class Declarations（修饰符abstract/final/strictfp/泛型/内部类/超类/超接口）
8.2 Field Declarations（static/final/transient/volatile/初始化）
8.3 Method Declarations（修饰符/泛型方法/返回类型/throws/重载/重写与隐藏）
8.4 Constructor Declarations（修饰符/签名/throws/构造器体/显式构造器调用/默认构造器）
8.5 Enums / Instance Initializers / Static Initializers

### 第9章 Interfaces 接口
9.1 Interface Declarations（修饰符/泛型/超接口）
9.2 Interface Method Declarations / 接口字段初始化
9.3 Inheritance and Overriding（重写要求/方法体）
9.4 Annotation Types（声明/默认值/预定义注解@Target/@Retention/@Documented/@Inherited/@Repeatable）
9.5 Functional Types

### 第10章 Arrays 数组
10.1 Array Types / Creation / Access / Store Exception / Initializers

### 第11章 Exceptions 异常
11.1 Kinds and Causes of Exceptions
11.2 Compile-Time Checking / Run-Time Handling

### 第12章 Execution 执行
12.1 JVM Startup（Load/Link/Initialize/Invoke main）
12.2 Loading / Linking / Initialization of Classes and Interfaces
12.3 Creation of New Class Instances / Finalization / Unloading / Program Exit

### 第13章 Binary Compatibility 二进制兼容性
13.1 Evolution of Packages/Modules/Classes/Interfaces/Annotation Types

### 第14章 Blocks and Statements 代码块与语句
14.1 Blocks / Local Variable Declaration / if/assert/switch/while/do/for/break/continue/return/throw/synchronized/try(try-with-resources)

### 第15章 Expressions 表达式
15.1 Evaluation / Primary Expressions（字面量/this/括号）
15.2 Class Instance Creation / Array Creation and Access / Field Access
15.3 Method Invocation Expressions（4步编译时决策：确定搜索类→确定方法签名→是否合适→调用类型）
15.4 Method Reference Expressions
15.5 Unary/Postfix/Cast/Multiplicative/Additive/Shift/Relational/Equality/Bitwise/Conditional/Assignment Operators
15.6 Lambda Expressions / Constant Expressions

### 第16章 Definite Assignment 确定赋值
16.1 Basics / Statements（if/switch/while/do/for/break等）

### 第17章 Threads and Locks 线程与锁
17.1 Synchronization / Wait Sets and Notification
17.2 Memory Model（Shared Variables/Actions/Program Order/Happens-before/Synchronization Order/Execution/Observability/Optimizations/Causality）
17.3 Final Field Semantics（安全发布/部分构造对象/final语义）
17.4 Non-Atomic Treatment of double and long

### 第18章 Type Inference 类型推断
18.1 Concepts / Inference Variables / Resolution / Invocation Type Inference

### 第19章 Syntax 语法附录
