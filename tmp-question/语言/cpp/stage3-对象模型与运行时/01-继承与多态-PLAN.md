# Stage3 Ch01 规划：继承与多态

> **定位**：Stage3 第一章。前提——读者已完成 Stage1（类/RAII/智能指针）和 Stage2（模板/STL），理解 C++ 的"编译时多态"（模板）。
> **主线版本**：C++11
> **参考书**：深度探索C++对象模型 §1-4，C++程序设计语言 §20-21

---

## 1. 本章要解决的核心问题

Stage2 的模板是"编译时多态"——`max<int>` 和 `max<double>` 是编译时生成的不同函数。但有些场景编译时不知道具体类型——"用户选择了圆形还是矩形"要在运行时才知道。

C 里实现运行时多态要手写 vtable（函数指针表）+ `void*`——Ch01 §5 演示过。C++ 给了更安全、更高效的方案：**虚函数（virtual）**。

本章教"运行时多态"的基础层——继承、虚函数、虚表、`override`/`final`、抽象类。多重继承留给 Ch02。

---

## 2. 节结构（6 节）

### §1 继承的基本语法——从 C 的"组合"到 C++ 的"继承"

| 你在 C 里这样写 | C++ 里可以这样 |
|---|---|
| 子结构体嵌父结构体作为第一个成员，手动转发调用 | `class Dog : public Animal { ... };`——自动继承基类的接口和数据 |

```cpp
// 基类
class Animal {
public:
    void eat() { std::cout << "eating\n"; }
};

// 派生类——继承 Animal 的所有 public 成员
class Dog : public Animal {
public:
    void bark() { std::cout << "barking\n"; }
};

Dog d;
d.eat();   // Dog 继承了 Animal 的 eat()
d.bark();  // Dog 自己的方法
```

- **`public`/`protected`/`private` 继承**：`public` 继承——基类的 public/protected 成员保持原有访问级别（最常见）。`private` 继承——基类所有成员变成 private（"用基类实现，但不暴露基类接口"——少见）。`protected` 继承介于两者之间。
- **C 的模拟方式**：子 struct 第一个成员是父 struct→指针转换 `(Parent*)child`→手动写函数转发——不安全且繁琐。

### §2 虚函数——运行时"猜"类型

继承解决了"代码复用"——Dog 不用重写 eat()。但更重要的需求是"统一接口"：

```cpp
class Animal {
public:
    virtual void speak() { std::cout << "...\n"; }  // 🆕 virtual
};

class Dog : public Animal {
public:
    void speak() override { std::cout << "Woof!\n"; }  // 重写
};

class Cat : public Animal {
public:
    void speak() override { std::cout << "Meow!\n"; }
};

// 关键：通过基类指针调用——编译器不知道指向 Dog 还是 Cat
Animal* a1 = new Dog();
Animal* a2 = new Cat();
a1->speak();  // "Woof!"——运行时调用 Dog::speak
a2->speak();  // "Meow!"——运行时调用 Cat::speak
```

- **非虚函数 vs 虚函数**：非虚函数——调用哪个版本在编译时决定（看指针类型）。虚函数——调用哪个版本在**运行时**决定（看指针指向的真实对象类型）。
- **`override`（C++11）**：告诉编译器"我在重写基类的��函数"——防止拼写错误（参数写错了本该重写却变成了新函数）。
- **`final`（C++11）**：`void speak() final`——禁止子类进一步重写此函数；`class Dog final`——禁止继承 Dog。**性能含义**——`final` 保证没有子类重写→编译器在知道确切类型时可以把虚函数调用优化为直接调用（"去虚化"devirtualization）——省去 vptr→虚表→函数的两次间接寻址。

### §3 虚表（vtable）——虚函数是怎么实现的

虚函数不是魔术——编译器用一张函数指针表实现：

```
Animal 对象的内存布局：              Dog 对象的内存布局：
┌──────────────┐                    ┌──────────────┐
│  vptr (8B)   │ → vtable          │  vptr (8B)   │ → vtable
├──────────────┤   ┌──────────┐    ├──────────────┤   ┌──────────────┐
│  (数据成员)   │   │ speak  → Animal::speak │    │  (数据成员)   │   │ speak → Dog::speak │
└──────────────┘   │ ~Animal  ││    └──────────────┘   │ ~Animal  ││
                   └──────────┘│                       │ ~Dog  ││
                               │                       └──────────────┘
                               │
```

- **`vptr`（虚指针）**：每个有虚函数的对象第一个 8 字节（64 位）是指向虚表的指针——这是对象比"纯数据 struct"多出来的唯一开销。
- **虚表**：每个有虚函数的类有一张——类的所有对象共享同一张虚表。表里存的是"这个类的虚函数地址"——Dog 的表指向 `Dog::speak`，Cat 的表指向 `Cat::speak`。
- **虚函数调用的开销**：`a1->speak()` → 读 `a1` 的第一个 8 字节→找到虚表→读表中 speak 的条目→跳转。多一次间接寻址——和直接调用比多 ~5-10 cycles。
- **vptr 的生命周期——构造和析构时它指向谁？** 构造一个派生类对象时，vptr 不是一开始就指向派生类的虚表——构造先从基类开始，此时 vptr 指向**基类的虚表**；基类构造完后进入派生类构造，vptr **切换**到派生类的虚表。构造函数里调虚函数→调用的是当前构造层的版本（不是最终派生类的版本）。析构时反过来——vptr 从派生类虚表逐步**回退**到基类虚表。这就是"构造函数/析构函数里虚函数不多态"的根源——不是编译器 bug，是 vptr 此时确实指向当前这一层的虚表。
- **和 C 手写 vtable 的区别**：C 里你要手写函数指针表、手动初始化 vptr、手动转换——C++ 编译器替你做这三个步骤。

### §4 虚析构函数——为什么基类的析构函数必须是虚的

```cpp
class Animal {
public:
    ~Animal() { std::cout << "~Animal\n"; }  // 🔴 非虚析构！
};
class Dog : public Animal {
    char* buffer;  // Dog 自己分配的资源
public:
    Dog() : buffer(new char[1024]) {}
    ~Dog() { delete[] buffer; std::cout << "~Dog\n"; }
};

Animal* a = new Dog();
delete a;   // 🔴 只调了 ~Animal()，没调 ~Dog()——buffer 泄漏！
```

- **为什么**：`delete a` 时编译器看 `a` 的类型是 `Animal*`→调 `~Animal()`。如果析构函数不是虚的，编译器不知道 `a` 实际指向 Dog→Dog 的析构函数不跑→Dog 的资源泄漏。
- **修复**：`virtual ~Animal() {}`——编译器通过虚表找到真实的析构函数（先调 `~Dog()` 再调 `~Animal()`）。
- **核心规则**：有虚函数的基类——析构函数必须加 `virtual`。面试必考。

### §5 抽象类与纯虚函数——"接口"的 C++ 实现

```cpp
class Shape {
public:
    virtual double area() const = 0;  // 纯虚函数——"= 0"表示没有实现
    virtual ~Shape() {}               // 虚析构
};

class Circle : public Shape {
    double r;
public:
    Circle(double radius) : r(radius) {}
    double area() const override { return 3.14159 * r * r; }
};

// Shape s;     // ✗ 编译错误——抽象类不能实例化
Shape* s = new Circle(5.0);  // ✓ 通过基类指针
std::cout << s->area();      // 78.54
```

- **纯虚函数 `= 0`**：表示"我这个函数不提供实现，派生类必须实现"。
- **抽象类**：至少有一个纯虚函数的类——不能创建对象，只能作为基类。
- **和 Java `interface` 的区别**：C++ 的抽象类可以包含数据成员和已实现的函数（不只有纯虚函数）——比 Java interface 更灵活但语法更重。

### §6 RTTI ——运行时查类型

```cpp
Animal* a = new Dog();

// dynamic_cast——安全的向下转型
Dog* d = dynamic_cast<Dog*>(a);
if (d) { d->bark(); }  // 转型成功

// typeid——查真实类型
if (typeid(*a) == typeid(Dog)) { /* 是 Dog */ }
```

- **`dynamic_cast`**：运行时检查指针是否真的指向目标类型——失败返回 `nullptr`（指针）或抛异常（引用）。
- **为什么不用 C 风格转换**：`(Dog*)a` 不检查——如果 a 实际指向 Cat→UB。`dynamic_cast` 有运行时检查——失败不崩溃。
- **代价**：`dynamic_cast` 慢（一次字符串比较或虚表遍历）——不放在热路径中。需要 RTTI 开启（`-frtti`，默认开）。

---

## 3. 编写方针

1. **从 C 的手写 vtable 出发**——读者在 Stage1 Ch01 §5 见过 C 的 vtable 模式（`shape_vtable_t`），本章用这个旧知识引出"C++ 编译器帮你做了这一切"
2. **§3 虚表布局是本章灵魂**——配图 + 内存布局对比，读者必须亲眼看到 Dog 对象比 Animal 对象多出来的 vptr
3. **§4 虚析构函数必须配可运行泄漏实验**——让读者亲手写一个 `delete` 基类指针导致派生类资源泄漏的代码
4. 本章不讲的内容：多重继承/虚继承（Ch02）、`type_info` 细节（参考手册即可）

---

## 4. 面试题（3 组，每组含答案+追问）

### 面试题 1：虚函数的实现机制

**面试官**：虚函数是怎么实现的？`virtual` 给对象增加了什么开销？

**回答**：虚表（vtable）+ 虚指针（vptr）。每个有虚函数的类有一张虚表（类的所有对象共享），表里存的是该类实际调用的虚函数地址。每个对象有一个 vptr（通常第一个 8 字节），指向它所属类的虚表。调用 `a->speak()` → 通过 vptr 找到虚表→查 speak 条目→跳转到实际函数。开销：(1) 对象多 8 字节（vptr）；(2) 每次虚函数调用多一次间接寻址（~5-10 cycles）。

**追问（面试官）**：构造函数里调虚函数会多态吗？

**追问回答**：不会。在构造函数执行期间，vptr 指向的是**当前正在构造的类**的虚表，不是派生类的虚表。因为派生类还没构造完——如果这时候调派生类的重写版本，派生类的成员还没初始化→UB。同理，析构函数里调虚函数也不会多态——vptr 被逐步"回退"到基类的虚表。

### 面试题 2：虚析构函数

**面试官**：什么时候基类的析构函数必须加 `virtual`？不加会怎样？

**回答**：只要类有可能被继承（作为基类），析构函数就应该加 `virtual`。不加→`Animal* a = new Dog(); delete a;` 只调 `~Animal()` 不调 `~Dog()`→派生类的资源泄漏。面试里的标准回答：有虚函数或打算被继承的类→析构函数必须 virtual。`final` 类可以不加（不能被继承）。

**追问（面试官）**：为什么析构函数需要虚的而构造函数不需要？

**追问回答**：构造时对象从基类到派生类逐层构造——编译器知道当前在构造哪一层，不需要虚表来"找正确的构造函数"。析构时通过基类指针 `delete`，编译器只知道指针类型不知道真实对象类型——必须靠虚表找到正确的析构函数。构造函数是"自顶向下"（已知路径），析构函数是"自底向上"（需要查表）。

### 面试题 3：override 和 final

**面试官**：`override` 和 `final` 各解决什么问题？加了有什么好处？

**回答**：`override`——告诉编译器"我在重写基类的虚函数"，防止拼写错误（参数写错→本该重写却变成定义新函数→编译通过→运行时 bug）。`final`——阻止进一步重写（`void f() final`）或阻止继承（`class Foo final`）。加了之后——(1) 编译器帮你检查是否真的在重写；(2) `final` 给编译器优化机会——`final` 类的虚函数调用可以被"去虚化"（编译时确定目标，省去间接跳转）。

**追问（面试官）**：`final` 类的虚函数调用为什么能被优化？

**追问回答**：`final` 保证没有子类——编译器知道"这个类只有自己的虚函数实现，不可能有子类重写版本"。于是编译器可以在某些场景（如通过值对象调用、通过 final 类指针调用）把虚函数调用变成直接调用——省去 vptr→vtable→function 的间接跳转。这是一个典型的 C++"零开销抽象"体现——你用虚函数写代码，编译器在它知道确切类型时不走虚表。

---

## 5. 可运行错误实验

### 实验 1：虚析构函数缺失导致资源泄漏

```cpp
#include <iostream>

class Animal {
public:
    Animal() { std::cout << "Animal()\n"; }
    ~Animal() { std::cout << "~Animal()\n"; }  // 🔴 不是虚的！
};

class Dog : public Animal {
    char* buf;
public:
    Dog() : buf(new char[1024]) { std::cout << "Dog() allocated 1KB\n"; }
    ~Dog() { delete[] buf; std::cout << "~Dog() freed 1KB\n"; }
};

int main() {
    Animal* p = new Dog();
    delete p;   // 🔴 只输出 ~Animal()——~Dog() 没被调用——1KB 泄漏！
    return 0;
}
```

**修复**：`virtual ~Animal() {}`——编译运行，现在看到 `~Dog()` + `~Animal()` 都输出。

### 实验 2：构造函数里的虚函数不生效

```cpp
class Base {
public:
    Base() { print(); }          // 构造里调虚函数
    virtual void print() { std::cout << "Base\n"; }
};
class Derived : public Base {
public:
    virtual void print() { std::cout << "Derived\n"; }
};
int main() {
    Derived d;  // 输出 "Base"——不是 "Derived"！
    return 0;
}
```

---

## 6. 4 维度自检（目标 ≥28/40）

| 维度 | 本章怎么做 | 预估 |
|---|---|---|
| 技术专家深度 | 虚表内存布局（图解 vptr + vtable 结构）；虚函数调用的间接跳转开销（~5-10 cycles 量化）；构造函数/析构函数中虚函数的行为（vptr 逐步切换）；`final` 的"去虚化"编译优化原理 | 8/10 |
| 面试覆盖 | 3 组完整面试 Q&A（虚表实现+构造里虚函数 / 虚析构+构造不需要虚 / override+final+去虚化），每组含答案 + 二层追问 | 9/10 |
| 生产陷阱 | 虚析构缺失→资源泄漏可运行实验；构造函数中调虚函数不生效实验；`dynamic_cast` 引用版本失败抛 `bad_cast` 异常 | 8/10 |
| 交叉引用 | §1 从 C 手写 vtable 出发→Stage1 Ch01 §5；虚函数开销→Stage2 Ch01 模板（编译时 vs 运行时多态）；`dynamic_cast`→Ch02 多重继承下更复杂；`final` 优化→Ch01 零开销抽象哲学 | 8/10 |
| **总分** | | **33/40** |
