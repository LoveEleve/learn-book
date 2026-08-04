# 《The Java Virtual Machine Specification》JVMS 17 —— 完整目录

> 7章 + 附录 | Java虚拟机规范权威标准 | JVM结构/Class文件格式/加载链接初始化/完整指令集

---

### 第1章 Introduction 简介
1.1 A Bit of History
1.2 The Java Virtual Machine
1.3 Organization of the Specification / Notation

### 第2章 The Structure of the Java Virtual Machine JVM 结构
2.1 The class File Format
2.2 Data Types and Values（整数/浮点/returnAddress/boolean）
2.3 Reference Types and Values
2.4 Run-Time Data Areas（pc Register/JVM Stacks/Heap/Method Area/Run-Time Constant Pool/Native Method Stacks）
2.5 Frames（Local Variables/Operand Stacks/Dynamic Linking/正常/异常方法调用完成）
2.6 Representation of Objects
2.7 Floating-Point Arithmetic（IEEE 754/浮点模式/Value Set Conversion）
2.8 Special Methods（实例初始化/类初始化/Signature Polymorphic Methods）
2.9 Exceptions
2.11 Instruction Set Summary（加载存储/算术/类型转换/对象创建操作/操作栈管理/控制转移/方法调用返回/抛异常/同步）
2.12 Class Libraries / Public Design Private Implementation

### 第3章 Compiling for the Java Virtual Machine 面向JVM编译
3.1-3.16（常量与控制结构/算术/运行时常量池/接收参数/调用方法/类实例/数组/编译switch/操作栈/异常/finally/同步/注解/模块）

### 第4章 The class File Format Class文件格式
4.1 The ClassFile Structure
4.2 Names（二进制类名/非限定名/模块与包名）
4.3 Descriptors（语法记号/字段描述符/方法描述符）
4.4 The Constant Pool（11种常量结构：Class/String/Integer/Float/Long/Double/NameAndType/Utf8/MethodHandle/MethodType/Dynamic/InvokeDynamic/Module/Package）
4.5 Fields / 4.6 Methods
4.7 Attributes（29种属性：ConstantValue/Code/StackMapTable/Exceptions/InnerClasses/EnclosingMethod/Synthetic/Signature/SourceFile/LineNumberTable/LocalVariableTable/Deprecated/注解/BootstrapMethods/MethodParameters/Module/NestHost/NestMembers等）
4.8 Format Checking
4.9 Constraints on JVM Code（静态约束/结构约束）
4.10 Verification of class Files（Type Checking/Type Inference/字节码验证器/long和double/实例初始化/finally）
4.11 Limitations of JVM

### 第5章 Loading, Linking, and Initializing 加载、链接与初始化
5.1 Run-Time Constant Pool / JVM Startup
5.2 Creation and Loading（Bootstrap类加载器/用户自定义类加载器/创建数组类/Loading Constraints/派生类）
5.3 Modules and Layers
5.4 Linking（Verification/Preparation/Resolution解析：类和接口/字段/方法/接口方法/方法类型和句柄/动态常数和调用点/Access Control/Method Overriding/Method Selection）
5.5 Initialization
5.6 Binding Native Method Implementations / JVM Exit

### 第6章 The Java Virtual Machine Instruction Set JVM 指令集
6.1-6.4（全部字节码指令逐条定义：加载存储/算术/类型转换/对象创建/操作栈/控制转移/方法调用/异常/同步等200+条指令）

### 第7章 Opcode Mnemonics by Opcode 按Opcode排序的指令助记符

### 附录 A Limited License Grant
