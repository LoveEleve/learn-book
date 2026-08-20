# 闭环笔记 q5: 多路复用抽象 — 六接口与 epoll 细节

## 假设
后端抽象 = 六静态接口 + 编译期 include; epoll 的事件映射与 ADD/MOD/DEL 语义。

## 验证过程
- 编译期选择 (ae.c:29-43): HAVE_EVPORT → evport / HAVE_EPOLL → epoll / HAVE_KQUEUE → kqueue / 否则 select — 性能降序注释 (L29-30)
- 六接口 (ae_epoll.c 全 118 行): aeApiCreate (L18) / Resize (L38) / Free (L45) / AddEvent (L53) / DelEvent (L70) / Poll (L88) / Name (L116)
- epoll 细节:
  - **epoll_create(1024)** (L27): 参数只是内核提示 (现代内核忽略)
  - ADD vs MOD (L58-59): events[fd].mask == NONE → EPOLL_CTL_ADD, 否则 MOD — 合并旧事件 (L62)
  - **事件映射** (L102-105): EPOLLIN → READABLE / EPOLLOUT → WRITABLE / **EPOLLERR 和 EPOLLHUP → WRITABLE|READABLE 双触发** (保证错误也会唤醒读回调)
  - DEL (L79-84): mask 非空 → MOD; 空 → EPOLL_CTL_DEL (注释: 老内核 2.6.9 前 DEL 需要非空指针)
- kqueue 后端 190 行 / select 后端 89 行 (同接口不同实现)
- aeApiPoll 超时转换 (L92-93): timeval → ms (向上取整 +999/1000)
- 返回 fired 数组填充 (L98-108) — aeProcessEvents 直接消费

## 代码类型
Mechanism (后端抽象)

## 跨域关联
- R-28 (networking 只面对 ae 接口) / R-20 (aeCreateEventLoop)

## 结论
多路复用 = 编译期特化 (无虚函数/无函数指针表, 静态链接): 性能最优后端优先。epoll 的 ERR/HUP 双触发映射是经典细节 — 连接断开同时唤醒读写回调, 由应用层 (networking) 判断。
源码位置: ae.c:29-43; ae_epoll.c:18-118
