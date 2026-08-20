# 闭环笔记 q1: aeEventLoop 结构与文件事件注册

## 假设
事件循环 = fd 索引数组 (events/fired) + 时间事件链表; 文件事件注册维护 mask/maxfd。

## 验证过程
- 结构 (ae.h:78-90): maxfd / setsize / timeEventNextId / events[fired] 数组 / timeEventHead / stop / apidata (后端私有) / beforesleep / aftersleep / flags
- aeCreateEventLoop (ae.c:46-78): setsize 参数 (server.c:2657: maxclients+CONFIG_FDSET_INCR) → events/fired 双数组 + aeApiCreate (L64) + mask 初始化 AE_NONE (L67-68)
- **aeCreateFileEvent** (L143-161): fd ≥ setsize → ERANGE; aeApiAddEvent (L152) → mask 合并 (L154) → r/w proc 分别挂 (L155-156) → maxfd 提升 (L158-159)
- **aeDeleteFileEvent** (L163-183): mask==NONE 早退 (L167); 删 WRITABLE 时连带删 BARRIER (L169-171); maxfd 回退扫描 (L175-182)
- 事件表语义 (ae.h:20-27): AE_NONE=0 / READABLE=1 / WRITABLE=2 / BARRIER=4 (与 WRITABLE 组合: 同轮读后不写)
- aeResizeSetSize (L104-120): 新槽初始化 NONE; maxfd ≥ 新 size → AE_ERR

## 代码类型
Mechanism (事件表管理)

## 跨域关联
- R-20 (aeCreateEventLoop server.c:2657) / R-28 (networking 注册 readQueryFromClient)

## 结论
事件循环 = **fd 直接索引数组** (无哈希, fd 即下标) — 注册/删除 O(1)。mask 合并支持同 fd 多事件, maxfd 维护轮询范围。BARRIER 是"读后不写"的显式屏障标志。
源码位置: ae.h:20-27,78-90; ae.c:46-78,104-183
