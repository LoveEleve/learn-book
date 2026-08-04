# Stage3 Ch02 规划：多重继承与虚继承

> **定位**：Stage3 第二章。前提——读者已完成 Ch01（单继承/虚函数/虚表），理解 vptr 和虚表的基本机制。
> **主线版本**：C++11
> **参考书**：深度探索C++对象模型 §3-4，C++程序设计语言 §21

---

## 1. 本章要解决的核心问题

Ch01 的单继承很清晰——Dog 继承 Animal，一个基类一个派生类。但 C++ 支持**多重继承**——一个类同时继承多个基类。这带来了三个复杂问题：

1. **对象内存布局不再简单**——多个 vptr，对象不是"基类数据 + 派生类数据"的简单叠加
2. **菱形继承**——类 D 继承 B 和 C，B 和 C 都继承 A——A 的数据在 D 里出现两份？
3. **指针转换**——`this` 指针在基类间传递时需要调整地址（不是简单的 `static_cast`）

本章不是"劝你多用 MI"——是"如果你维护用了 MI 的代码（如 C++ GUI 框架、COM 编程），你至少要知道它会带来什么"。

---

## 2. 节结构（5 节）

### §1 多重继承的基本语法——同时拥有两个"父亲"

```cpp
class Flyable {
public:
    virtual void fly() { std::cout << "flying\n"; }
};

class Swimmable {
public:
    virtual void swim() { std::cout << "swimming\n"; }
};

// Duck 同时拥有飞行和游泳的能力
class Duck : public Flyable, public Swimmable {
public:
    void fly() override  { std::cout << "duck flying\n"; }
    void swim() override { std::cout << "duck swimming\n"; }
};
```

- **语法**：`class D : public B1, public B2`——用逗号分隔基类
- **Duck 对象能做什么**：`duck.fly()`、`duck.swim()`——来自两个基类的接口都可用
- **这是 MI 最合理的用法**——两个基类提供**完全独立**的接口（Flyable 只管飞行，Swimmable 只管游泳），没有重叠、没有冲突

### §2 多重继承的对象内存布局——为什么指针会"飘移"

```cpp
Duck d;
Flyable*  f = &d;       // 指向 d 的 Flyable 子对象部分
Swimmable* s = &d;      // 指向 d 的 Swimmable 子对象部分
// f 和 s 的地址**不一样**！
// Duck 对象内存布局：
// ┌────────────────┐  ← &d (也是 f 的位置，第一个基类)
// │ Flyable::vptr  │
// │ Flyable 数据    │
// ├────────────────┤
// │ Swimmable::vptr│  ← s = &d + sizeof(Flyable)
// │ Swimmable 数据  │
// ├────────────────┤
// │ Duck 自己的数据  │
// └────────────────┘
```

- **关键理解**：`Duck` 对象里有两个 vptr——一个指向 Flyable 虚表，一个指向 Swimmable 虚表。编译器给 `d` 赋给 `Swimmable*` 时，自动加上 `sizeof(Flyable)` 的偏移——这叫 **this 指针调整**。
- **可以验证**：`printf("%p %p\n", (void*)f, (void*)s);`——两个指针不同。
- **对 `dynamic_cast` 的影响**：`dynamic_cast<Duck*>(s)` 时需要**减去**偏移回到 Duck 的起始地址——虚表里记录了从每个基类到派生类的偏移量。

### §3 菱形继承——同一个祖父出现两次

当一个类多次继承自同一个祖先类（通过不同的路径），祖先的数据**出现两次**：

```cpp
class Animal {
public:
    int age;    // Animal 的数据
};

class Mammal  : public Animal {};   // 路径 1
class Winged  : public Animal {};   // 路径 2
class Bat : public Mammal, public Winged {};

Bat b;
// b.age = 5;           // ✗ 编译错误——哪个 age？Mammal 的还是 Winged 的？
b.Mammal::age = 5;      // ✓ 明确指定路径
b.Winged::age = 3;      // 另一个 age——不一样的！
```

- **问题**：`Animal` 的 `age` 在 Bat 对象里有两个副本——一个来自 Mammal→Animal，一个来自 Winged→Animal。浪费内存 + 不一致的风险。
- **为什么编译器不自动合并**：C++ 的设计原则是"你让我多重继承——我给你两份。如果你只想要一份——明确告诉我说这是虚继承"。

### §4 虚继承——"我只要一份祖父"

```cpp
class Animal { public: int age; };

class Mammal : public virtual Animal {};   // 🆕 virtual
class Winged : public virtual Animal {};   // 🆕 virtual
class Bat : public Mammal, public Winged {};

Bat b;
b.age = 5;   // ✓ 只有一个 age——不管是走 Mammal 还是 Winged 路径
```

- **虚继承的代价**：对象内存里不再直接"嵌"Animal 的数据——多了一个**虚基类指针（vbptr）**，指向一张表记录"Animal 数据在哪"。每次访问 `age` 需要一次额外间接寻址。
- **内存布局**：虚基类的数据被放到对象末尾（不是 Mammal 和 Winged 的内部）——所有派生类通过 vbptr 找到它。

| 继承方式 | 对象大小 | age 访问 | 适合 |
|---|---|---|---|
| 普通继承 | 不额外增大 | 直接偏移 | 大多数场景 |
| 虚继承 | 多一个 vbptr | 间接寻址 | 菱形继承 |

- **核心结论**：虚继承解决菱形继承问题——但代价是对象变大 + 访问变慢。**不要为了"以后可能菱形"而用虚继承**——只在真的菱形继承时才用。
- **构造规则**：虚基类由**最派生类**（most derived class）负责初始化——Mammal 和 Winged 的构造函数中对 Animal 的初始化被编译器忽略。防止 Animal 被多次构造。
- **和 Java 的对比**：Java 禁止多继承（只能单继承类 + 多实现接口），根本避免了菱形继承的数据重复问题。

### §5 `sizeof`、指针转换与 MI 总结

**`sizeof` 对 MI 的影响**：
```cpp
sizeof(Mammal)    // = sizeof(Animal) + Mammal 自己的数据（普通继承）
sizeof(Bat)       // = sizeof(Mammal) + sizeof(Winged) - 一个 Animal
                  // + Bat 自己的数据 ——如果虚继承，只有一份 Animal
```

**指针转换的三种情况**：
```cpp
Bat b;
Animal*    a = &b;   // 普通继承——直接偏移到 Animal 子对象
Bat*       bt = dynamic_cast<Bat*>(a);  // MI 下必须 dynamic_cast——编译器不确定 a 来自哪条路径
void*      v = &b;   // 不保证指向对象起始地址——不要用 void* 绕过 MI 的类型系统
```

**多重继承的"三不"原则**（实用的工程指南）：
1. **不加数据成员的接口类**可以多重继承（类似 Java interface 的 C++ 实现）
2. **有数据成员的类**避免多重继承（菱形问题会变得更复杂）
3. **虚继承只在真的菱形继承时使用**（不要"预防性"虚继承）

---

## 3. 编写方针

1. **§2 内存布局图是本章灵魂**——读者必须"看到"两个 vptr + 地址飘移，不能只看文字
2. **从单继承到 MI 到虚继承——梯次递进**，每步讲清楚动机：MI 让指针漂移 → 菱形让数据重复 → 虚继承解决重复但变慢
3. **"三不"原则作为工程收尾**——本章不是教怎么用 MI，是教"什么时候坚决不用"
4. 本章不讲的内容：MI 下的虚表完整结构（到虚函数条目的偏移）、构造函数中虚基类的初始化顺序（编译器特定）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：MI 对象的内存布局

**面试官**：`class D : public B1, public B2 {}; D d; B1* p1 = &d; B2* p2 = &d;`——`p1` 和 `p2` 的地址一样吗？为什么？

**回答**：不一样。`p1` 指向 d 对象中 B1 子对象的位置（通常是对象起始地址），`p2` 指向 d 对象中 B2 子对象的位置（偏移了 `sizeof(B1)` 字节）。编译器自动做了 this 指针调整——把 `D*` 转成 `B2*` 时加上偏移量。这就是为什么 MI 下的指针转换不是"零开销"——编译器需要插入额外的加减指令。

**追问（面试官）**：`dynamic_cast` 在 MI 下怎么知道要减多少偏移？

**追问回答**：虚表里记录了偏移量。每个有虚函数的基类在虚表中有一个条目（vtable slot）存储"从这个基类子对象到完整派生类对象的偏移"。`dynamic_cast<Duck*>(swim_ptr)` →读 Swimmable 的虚表→查 offset_to_top 条目→ `swim_ptr - offset` 得到 Duck 的地址→再验证类型匹配。这是 `dynamic_cast` 比 `static_cast` 慢的原因——它需要查虚表。

### 面试题 2：菱形继承与虚继承

**面试官**：什么是菱形继承？C++ 怎么解决？虚继承有什么代价？

**回答**：类 D 通过两条路径继承 A（B→A 和 C→A）→A 的数据在 D 里有两份副本→`d.a` 歧义。虚继承解决——`class B : virtual public A`——编译器只保留一份 A 的副本。代价——(1) 对象多一个 vbptr（虚基类指针）；(2) 每次访问 A 的成员需要多一次间接寻址（通过 vbptr 找 A 的位置）。

**追问（面试官）**：Java 为什么禁止多继承（只能单继承类）？C++ 怎么用接口模式避免菱形问题？

**追问回答**：Java 禁止多继承类就是为了避免菱形继承的数据重复和歧义——只有接口能多继承（接口没有数据成员→没有重复问题）。C++ 可以用类似模式——只让**纯抽象类**（只有纯虚函数、没有数据成员）参与多重继承，数据类保持单继承链。这就是 §5 的"三不"原则——接口类多用 MI，数据类走单继承。

### 面试题 3：虚继承的内存布局

**面试官**：虚继承的对象比普通继承多什么？为什么访问虚基类的成员更慢？

**回答**：多一个 vbptr（虚基类指针，通常 8 字节），指向一张虚基类表（记录每个虚基类在对象中的偏移量）。访问`d.age` → 读 d 的 vbptr→查表中 Animal 的偏移→加上偏移→读 age。比普通继承（`d.age` = `*(&d + sizeof(Mammal))`——编译时常量偏移）多一次间接寻址。

**追问（面试官）**：那构造函数里虚基类怎么初始化？Mammal 和 Winged 的构造函数都初始化 Animal 吗？

**追问回答**：不会——**最派生类**（most derived class，这里是 Bat）负责初始化虚基类。Mammal 和 Winged 构造函数里的 Animal 初始化被编译器忽略（如果它们不是单独作为对象构造的）。这是 C++ 标准的一个微妙规则——防止虚基类被初始化多次。如果不加这个规则，Bat 构造时 Mammal 的构造函数调了 Animal 的构造函数、Winged 的构造函数又调一次→Animal 被构造两次→UB。

---

## 5. 可运行错误实验

### 实验 1：指针飘移——亲眼看到 `B1*` 和 `B2*` 指向不同地址

```cpp
#include <iostream>

struct B1 { int x; virtual ~B1() {} };
struct B2 { int y; virtual ~B2() {} };
struct D : B1, B2 { int z; };

int main() {
    D d;
    B1* p1 = &d;
    B2* p2 = &d;
    D*  pd = &d;

    std::cout << "&d:  " << &d  << "\n";
    std::cout << "B1*: " << p1 << " (offset=0)\n";
    std::cout << "B2*: " << p2 << " (offset=" << ((char*)p2 - (char*)p1) << ")\n";
    std::cout << "D*:  " << pd << " (same as B1*)\n";
    // B2* 比 B1* 多 sizeof(B1) = 16 字节（vptr 8B + int x 4B + padding 4B）
    return 0;
}
```

### 实验 2：菱形继承的数据重复

```cpp
class A { public: int data = 0; };
class B : public A {};
class C : public A {};
class D : public B, public C {};

D d;
d.B::data = 1;     // 设置 B 路径的 data
d.C::data = 2;     // 设置 C 路径的 data——两个不同的 data！
std::cout << d.B::data << " " << d.C::data;  // "1 2"
// 内存里有两个 int——浪费 + 潜在不一致
```

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | MI 对象内存布局（双 vptr + this 指针地址飘移的可运行验证）；`dynamic_cast` 通过虚表 offset_to_top 实现跨基类转换；虚继承 vbptr 的间接寻址开销；最派生类初始化虚基类的标准规则 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（MI 内存布局+this 调整 / 菱形继承+虚继承代价 / vbptr+最派生类初始化），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | this 指针飘移可运行实验 + 菱形数据重复实验；`void*` 绕过 MI 类型系统的危险；虚继承不要"预防性"使用的工程原则 | 8/10 |
| 交叉引用 | §1-§2 从 Ch01 单继承/虚表出发→扩展到 MI；§3 共性问题→Java 禁止多继承（对比 C++ 设计哲学）；"三不"原则→Stage4 设计模式（好习惯预防问题） | 8/10 |
| **总分** | | **33/40** |
