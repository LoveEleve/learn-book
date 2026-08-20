# 闭环笔记 q6: 共享对象池 — 10000 整数 + 响应串

## 假设
启动时创建 shared 池: 10000 个 INT 编码整数 (0-9999, makeObjectShared) + 命令响应常量串 (+OK/错误/批量前缀) — 高频对象全程共享零分配。

## 验证过程
- createSharedObjects (server.c:1847+): 响应串族 — shared.ok/emptybulk/czero/cone/emptyarray/pong/queued/emptyscan + 错误族 (wrongtypeerr/err/nokeyerr/syntaxerr/oomerr/execaborterr...) + 特殊串 (space/plus/special_equals/redacted)
- 整数池 (server.c:1992-1995): `shared.integers[j] = makeObjectShared(createObject(OBJ_STRING,(void*)j)); encoding=INT` — **0-9999 全共享**
- OBJ_SHARED_INTEGERS=10000 (server.h:108) — 为什么 10000: 高频小整数范围 (计数/索引/长度); 10000 个 × 16B = 160KB 启动常驻
- makeObjectShared (object.c:56-60): refcount=OBJ_SHARED_REFCOUNT — 不可变, incr/decr 免维护, 多线程安全
- 消费: 命令回复写 +OK (shared.ok); SET count 5 → shared.integers[5]; INCR 结果小 → 池
- 响应串收益: 每条命令回复的协议前缀 (批量/错误) 零分配

## 代码类型
Interface (共享池) — 启动常驻优化

## 跨域关联
- R-20 (命令回复) → 主消费
- q3 (INT 编码) → 整数池载体
- R-28 (网络回复) → 协议串

## 结论
共享池 = 高频对象的永久驻留: 10000 整数 (160KB) + 协议响应串 — 回复路径零分配; OBJ_SHARED_REFCOUNT 让共享对象"免计数" (不可变语义, 线程安全)。
源码位置: server.c:1847+,1992-1995; server.h:108
