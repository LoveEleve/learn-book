# OOP in C

> **对应 Layer**：Layer 8（设计模式与工程化），参考 C-C++技术专家学习路线.md § Layer 8  
> **参考书章节**：极致C 第6-8章（OOP）；Fluent C 全书  
> **前置依赖**：S1Ch03 §5.5（vtable 雏形—函数指针表）；S3Ch03（内存布局）  
> **主线标准**：C11/C17

---

## 1. 引言：C 没有类——但这反而是一种自由

Java 用 `class` 统一封装、继承、多态——语法糖提供，机制透明（vtable 由 JVM 自动生成）。C 没有 `class` 关键字——你必须用 struct + 函数指针 + opaque pointer 从零搭建这些机制。但这带来了一个 Java 没有的自由——你对内存布局有绝对控制、对性能有精确的预见、对接口有完全的自定义权。

Linux 内核的 `file_operations`、`device_driver`、`inode_operations` 就是 OOP in C 的最佳实践——内核用 C 写，却有清晰的封装边界、多态接口和继承层次。

---

## 2. 封装——opaque pointer

### 2.1 标准模式

```c
// stack.h — 公共头文件
#ifndef STACK_H
#define STACK_H

typedef struct stack stack_t;  // 前向声明——调用者不知道 struct 的内部结构

stack_t *stack_create(void);           // 构造函数
void     stack_destroy(stack_t *s);    // 析构函数
void     stack_push(stack_t *s, int v);
int      stack_pop(stack_t *s);
int      stack_empty(const stack_t *s);

#endif
```

```c
// stack.c — 实现文件（调用者看不到）
#include "stack.h"
#include <stdlib.h>

struct stack {
    int    *data;
    size_t  size;
    size_t  capacity;
};

stack_t *stack_create(void) {
    stack_t *s = calloc(1, sizeof(stack_t));
    s->capacity = 4;
    s->data = malloc(s->capacity * sizeof(int));
    return s;
}

void stack_push(stack_t *s, int v) {
    if (s->size == s->capacity) {
        s->capacity *= 2;
        s->data = realloc(s->data, s->capacity * sizeof(int));
    }
    s->data[s->size++] = v;
}

int stack_pop(stack_t *s) {
    return s->data[--s->size];
}

stack_t *stack_create(void) {
    stack_t *s = calloc(1, sizeof(stack_t));
    if (!s) return NULL;  // 分配失败
    s->capacity = 4;
    s->data = malloc(s->capacity * sizeof(int));
    if (!s->data) { free(s); return NULL; }  // 部分分配失败——清理
    return s;
}

void stack_destroy(stack_t *s) {
    if (!s) return;
    free(s->data);
    memset(s, 0, sizeof(*s));  // 清零防 UAF
    free(s);
}
```

**为什么要用 opaque pointer？**
1. **信息隐藏**：调用者无法直接访问 `struct stack` 的成员——不能破坏不变量（如 `size > capacity`）
2. **ABI 稳定**：加字段不影响外部调用——只要 `stack.h` 不变，`stack.o` 可以独立升级
3. **编译隔离**：修改 `struct stack` 只会重新编译 `stack.c`，使用 `stack.h` 的所有 .o 都无需重新编译

### 2.2 对比：C++ 的 private 访问控制

| 维度 | C opaque pointer | C++ `private` |
|---|---|---|
| 信息隐藏 | 编译期——调用者看不见定义 | 语法层——调用者能看见定义，编译器必须拒绝访问 |
| ABI 稳定 | ✅ 加字段不影响外部 | ❌ 加 private 字段影响对象大小 → ABI 破坏 |
| 性能 | 间接分配——多一次间接 | 内联可优化 |

**opaque pointer 的真正优势不是"编译不报错"——是"ABI 不破坏"。** 这是 C 大型项目升级共享库的基础。

---

## 3. 继承——struct 嵌套

### 3.1 单继承模式

```c
// 基类——person
typedef struct {
    char name[32];
    int  age;
} person_t;

// 派生类——employee（第一个成员是基类）
typedef struct {
    person_t base;    // 必须是第一个成员
    char     job[32];
    int      salary;
} employee_t;

// 安全上转——标准 C 保证第一个成员的地址与 struct 的地址相同
#define upcast(ptr, to) ((to *)(ptr))

employee_t e = { .base = { .name = "Alice", .age = 30 }, .job = "Engineer" };
person_t *p = upcast(&e, person_t);  // 安全——&e == &e.base
printf("%s is %d years old\n", p->name, p->age);
```

**为什么 `person_t` 必须是第一个成员？** C 标准保证 struct 的第一个成员与 struct 本身拥有相同的地址（`&e == &e.base`）。如果 `person_t` 不在首位，上转需要偏移。

### 3.2 多重继承的模拟

```c
// 接口 A
typedef struct { int a; } interface_a_t;
// 接口 B
typedef struct { int b; } interface_b_t;

// 多继承类——两个基类并排
typedef struct {
    interface_a_t a;
    interface_b_t b;
    int my_data;
} multi_t;

// 上转到 a ——直接（第一个成员）
// 上转到 b ——需要偏移
#define container_of(ptr, type, member) \
    ((type *)((char *)(ptr) - offsetof(type, member)))

multi_t m;
interface_b_t *pb = &m.b;
multi_t *back = container_of(pb, multi_t, b);  // 从子成员找回父 struct
```

**`container_of` 是 Linux 内核的核心宏**——用于从链表节点找回父 struct、从驱动成员找回 `device_driver`。但必须在确认指针确实是该 struct 的一部分时使用——否则 UB。

---

## 4. 多态——vtable

### 4.1 完整实现

```c
// shape.h — 公共接口
#ifndef SHAPE_H
#define SHAPE_H

typedef struct shape_vtable {
    double (*area)(void *self);
    void   (*draw)(void *self);
} shape_vtable_t;

typedef struct {
    const shape_vtable_t *vtable;  // 必须是第一个成员——为了上转
    int id;
} shape_t;

double shape_area(shape_t *s);
void   shape_draw(shape_t *s);

#endif
```

```c
// shape.c — 基类实现
#include "shape.h"

double shape_area(shape_t *s) { return s->vtable->area(s); }
void   shape_draw(shape_t *s) { s->vtable->draw(s); }
```

```c
// circle.h — 派生类
#include "shape.h"

typedef struct {
    shape_t base;     // 第一个成员——标准单继承
    double radius;
} circle_t;

circle_t *circle_create(double r);
```

```c
// circle.c — 派生类实现
#include "circle.h"
#include <math.h>
#include <stdlib.h>

static double circle_area(void *self) {
    circle_t *c = (circle_t *)self;
    return M_PI * c->radius * c->radius;
}

static void circle_draw(void *self) {
    circle_t *c = (circle_t *)self;
    printf("Circle(r=%.2f, area=%.2f)\n", c->radius, c->base.vtable->area(self));
}

static const shape_vtable_t circle_vtable = {
    .area = circle_area,
    .draw = circle_draw
};

circle_t *circle_create(double r) {
    circle_t *c = calloc(1, sizeof(circle_t));
    if (!c) return NULL;
    c->base.vtable = &circle_vtable;
    c->radius = r;
    return c;
}

void circle_destroy(circle_t *c) {
    if (!c) return;
    // 析构顺序：子类先清理 -> 父类后清理（与构造相反）
    // c->radius 无资源需释放，直接 free 父类 struct
    free(c);
}

static const shape_vtable_t circle_vtable = {
    .area = circle_area,
    .draw = circle_draw
    // 未实现的 draw() — 如果忘记，调用时 vtable->draw == NULL -> SIGSEGV
    // 防御方案 1: 基类提供默认空函数
    // 防御方案 2: 调用前检查 if (s->vtable->draw) s->vtable->draw(s);
};
```

### 4.2 调用路径

```
shape_area(&circle->base)
    → s->vtable->area(s)
        → 查虚表——发现 circle_vtable → 跳转到 circle_area()
            → 上转到 circle_t * → 读 c->radius → 计算 π*r²
```

**每一步都有明确的指令和跳转**——没有魔术，没有自动生成，没有 GC。Java 的 `invokevirtual` 在 JVM 内部做同样的事，但在 C 中你对每层的开销有绝对的可视性。

### 4.3 与 Java 的对比

| 维度 | Java | C（本实现） |
|---|---|---|
| 虚表创建 | JVM 自动为每个类生成 | 手动定义 + 赋值 `const shape_vtable_t circle_vtable` |
| 虚表查找 | `invokevirtual` — JVM 优化（内联缓存） | `s->vtable->area` — 一次间接跳转（~2ns） |
| 上转 | `(Person) employee` — 编译器检查 | `(shape_t *)&circle->base` — 无检查 |
| 构造 | `new Circle(r)` — 自动初始化 | `calloc + 赋值` — 手动设置虚表 |
| 析构 | GC — 无谓析构 | `free` — 注意不能先 free 虚表再 free 对象 |

**Java 的 implicit cost**：`new Circle(r)` 触发了至少 3 次分配（对象 + monitor + 可能的 vtable 备份）。C 中 `calloc + 赋值`——一次分配。这就是 C 的优势——你对内存有绝对控制。

---

## 5. 生产案例——Linux 内核的 file_operations

### 5.1 内核的"多态"实现

```c
// 内核中 file_operations 的简化结构
struct file_operations {
    int  (*open)(struct inode *, struct file *);
    int  (*release)(struct inode *, struct file *);
    ssize_t (*read)(struct file *, char __user *, size_t, loff_t *);
    ssize_t (*write)(struct file *, const char __user *, size_t, loff_t *);
};

// ext4 文件系统的实现
const struct file_operations ext4_file_operations = {
    .open    = ext4_file_open,
    .release = ext4_release_file,
    .read    = new_sync_read,
    .write   = new_sync_write,
};

// 设备驱动——完全不同的实现
const struct file_operations urandom_fops = {
    .read = urandom_read,
    // 无 write——urandom 不接受写操作
};

// 通用调用——通过 vtable 分发
ssize_t vfs_read(struct file *file, char __user *buf, size_t count, loff_t *pos) {
    return file->f_op->read(file, buf, count, pos);
    // ext4: → ext4_file_open → 从磁盘读取数据块
    // urandom: → urandom_read → 从熵池生成随机字节
    // 调用者不需要知道实现
}
```

**同一个 `vfs_read` 调用，通过 `f_op->read` 分发到完全不同的实现——这就是 OOP 的核心价值**。

### 5.2 内核的命名空间管理

```c
// 不同子系统的 file_operations 被打前缀标识命名空间
const struct file_operations ext4_file_operations;    // ext4 文件系统
const struct file_operations proc_operations;          // /proc 伪文件系统
const struct file_operations socket_file_ops;          // socket
```

在 C++ 中这可能是 `Ext4FileOperations` vs `ProcOperations` vs `SocketFileOperations`——在 C 中用前缀名模拟命名空间。

---

## 6. 类型检查与向下转型——RTTI in C

### 6.1 虚表中的类型标记

```c
typedef enum { SHAPE_CIRCLE, SHAPE_RECTANGLE } shape_type_t;

struct shape_vtable {
    shape_type_t type;     // 运行时类型标识
    double (*area)(void *self);
    void   (*draw)(void *self);
};

#define SAFE_DOWNCAST(ptr, tgt_type, vtable_type, id) \
    (ptr && ptr->vtable && ptr->vtable->type == id \
        ? container_of(ptr, tgt_type, base) : NULL)

circle_t *c = SAFE_DOWNCAST(shape_ptr, circle_t, shape_vtable_t, SHAPE_CIRCLE);
if (c) printf("radius=%.2f\n", c->radius);
```

### 6.2 `offsetof` 的面试陷阱

```c
#define offsetof(type, member) ((size_t)&(((type *)0)->member))
```

**为什么对 NULL 解引用不崩溃？** `&` 取地址操作不会产生实际内存访问——编译器在编译时计算 `member` 相对于 `type` 的偏移量，直接填入常量池。运行时没有任何指令实际读取地址 0。面试官追问："如果 `member` 是位域（bit-field）呢？"——`offsetof` 对位域无效（位域没有地址）。

### 6.3 `container_of` 的安全使用条件

1. **ptr 必须确实是父 struct 的一部分**——错误使用不是编译错误，是运行时错误
2. **ptr 不是 NULL**——NULL 的 `container_of` 产生的地址在低地址区，访问即 SIGSEGV
3. **父 struct 的第一个成员必须是被嵌入的类型**——否则偏移不对

---

## 7. 总结

| 概念 | C 实现 | Java 等价 |
|---|---|---|
| 封装 | opaque pointer——调用者看不见 struct 定义 | `private` 关键字 |
| 继承 | struct 嵌套——基类为第一个成员 | `extends` |
| 上转 | `(base_t *)&derived->base`——标准 C 保证 | JVM 自动 |
| 多态 | vtable = 函数指针表 + const 初始化 | `invokevirtual` |
| 虚表调用 | `s->vtable->area(s)`——一次间接跳转 | 内联缓存优化 |
| 构造 | `calloc + 赋值虚表`——手动 | `new` + 构造链 |
| 析构 | `free`——先释放资源再 free 自己 | GC |

**核心观点**：OOP 不是 C++/Java 的特性——它是**程序组织的方法**。C 没有语法糖，但有完整的基础工具（struct + 函数指针 + opaque pointer）。技术专家不是"知道 C++ 比 C 更好写面向对象"，而是"知道在 C 中写面向对象时，你无法依赖编译器保证类型安全——你需要自己保证每一个上转都正确、每一个虚表指针都在调用前被初始化、每一个释放都释放了正确的子对象"。

---

## 验收要点

1. opaque pointer 相比公开 struct 有什么优势？为什么 ABI 稳定是核心价值？
2. 单继承中为什么基类必须是派生类的第一个成员？C 标准的什么保证提供了这一前提？
3. `container_of` 宏解决了什么场景？有什么风险？
4. 虚表调用的完整步骤是什么？从 `shape_area()` 到实际执行 `circle_area()` 经历了哪些跳转？
5. 构造和析构在 C 中如何处理？它们的正确顺序是什么？
6. Linux 内核的 `file_operations` 如何体现 OOP 多态？
7. C 的 OOP 和 C++ 的 OOP 在性能和内存控制上有什么差异？
---

## 面试题

**Q1：C 能实现多态吗？标准答案。**

A：能——用 struct 嵌入函数指针表（vtable）。与 C++ 的关键差异：C++ 的 `virtual` 允许编译器做 **devirtualization**——当编译器能证明具体类型时，`virtual` 调用被优化为直接跳转（省去查 vtable）。C 的 vtable 永远走间接跳转（一次指针解引用+一次 `call [rax]`，CPU 的 branch predictor 无法预测间接跳转的目标）。

**追问**："为什么不预测？"——间接跳转的目标在一个链接时不可知的值上，CPU 的 BTB（Branch Target Buffer）需要运行时学习——首次调用 100% miss，后续调用需要多次训练。

**Q2：`offsetof` 为什么对 NULL 解引用但不崩溃？**

`#define offsetof(type, member) ((size_t)&(((type *)0)->member))`。`&` 操作符不产生实际内存访问——编译器在编译时计算 member 相对 type 的偏移，直接填入常量池。运行时零指令。追问："对 bit-field 有效吗？"——无效，位域没有地址。
