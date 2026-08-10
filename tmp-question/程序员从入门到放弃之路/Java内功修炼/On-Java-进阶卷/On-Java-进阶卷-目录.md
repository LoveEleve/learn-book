# 《On Java 中文版・进阶卷》—— 完整目录

> 作者：Bruce Eckel | 8章 + 6附录 | 枚举→集合→注解→并发→底层并发→I/O→设计模式

---

### 01 枚举类型
1.1 枚举基本特性 / 静态导入 / 自定义方法 / switch中使用
1.2 values()方法 / 实现而非继承 / 随机选择
1.3 使用接口组织枚举 / EnumSet / EnumMap
1.4 常量特定方法（枚举分发/职责链/EnumMap分发/二维数组）
1.5 模式匹配新特性（switch箭头语法/case null/switch表达式/智能转型/递进式模式/守卫/支配性/支配范围）

### 02 对象传递和返回
2.1 传递引用（别名）
2.2 创建本地副本（值传递/克隆/可克隆能力/二维数组/组合对象/ArrayList深拷贝/序列化深拷贝/继承层次可克隆性/复制构造器）
2.3 不可变类（创建/缺点/String特殊性）

### 03 集合主题
3.1 List/Set行为 / Map函数式操作
3.2 填充集合（Suppliers填充Collection/Map）
3.3 Collection功能 / 可选操作
3.4 Set与存储顺序（SortedSet）
3.5 Queue（优先队列/Deque）
3.6 Map（TreeMap/SortedMap/LinkedHashMap）
3.7 工具函数（排序查找/不可修改/同步/WeakHashMap）
3.8 Java 1.0/1.1集合（Vector/Enumeration/Hashtable/Stack/BitSet）

### 04 注解
4.1 基本语法（定义注解/元注解）
4.2 编写注解处理器（默认值限制/赋值处理器/生成外部文件/不支持继承）
4.3 用javac处理注解 / 基于注解的单元测试

### 05 并发编程
5.1 并发四定律 / 残酷的事实
5.2 并行流（parallel()非灵丹妙药/limit()/只是看起来简单）
5.3 创建和运行任务（Task和Executor/多线程/生成结果/lambda方法引用）
5.4 终止长时间运行任务
5.5 CompletableFuture（基本用法/其他操作/合并多个/模拟场景/异常）
5.6 死锁 / 构造器不是线程安全的 / 并发设计失败之处

### 06 底层并发
6.1 线程（最佳线程数/最多多少线程）/ 捕获异常
6.2 共享资源（资源竞争/解决/生产者消费者同步）
6.3 volatile关键字（竞争与共享/可见性/指令重排序和先行发生/何时使用）
6.4 原子性（Josh序列号/原子类）
6.5 临界区 / Lock对象
6.6 库组件（DelayQueue/PriorityBlockingQueue/无锁集合）

### 07 Java I/O 系统
7.1 I/O流（InputStream类型/OutputStream类型/Reader和Writer/RandomAccessFile/典型用法）
7.2 标准I/O（标准输入读取/System.out转PrintWriter/重定向/进程控制）
7.3 新I/O系统NIO（ByteBuffer/转换数据/基本类型/视图缓冲区/操纵数据/内存映射文件/文件加锁）

### 08 设计模式
8.1 设计模式概念 / 单例模式 / 分类
8.2 模板方法 / 封装实现（代理/状态/职责链）
8.3 工厂模式（动态工厂/多工厂/抽象工厂）
8.4 函数对象模式（命令/策略/职责链）
8.5 改变接口 / 适配器 / 解释器
8.6 回调（观察者模式/花朵观察者/可视化观察者）
8.7 多路分发（Trash子类/信使对象/通用工厂/文件解析/DynFactory回收/访问者模式）

### 附录（6个）
- A：编程指南
- B：Javadoc
- C：理解equals()和hashCode()
- D：数据压缩
- E：对象序列化
- F：静态类型检查的利与弊
