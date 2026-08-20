# 闭环笔记 q7: getDecodedObject + LRU/LFU 双用途

## 假设
读路径的解码是"按需临时": INT → ll2string 缓冲 (不缓存解码结果); lru 字段 24bit 双用途: LRU 时钟 (分钟级) 或 LFU (8bit 频率+16bit 时间)。

## 验证过程
- getDecodedObject (object.c:685-697): sds 编码 → incrRefCount 直接返回 (无解码); INT → `ll2string(buf,32)` 创建新字符串对象 — **每次读都重建** (INT 编码的"解码成本"是临时缓冲, 免长期占用)
- 比较优化 (L706-715): compareStringObjectsWithFlags — INT 对象直接比值 (免解码) — 大部分比较路径不触发 getDecodedObject
- LRU_BITS=24 (server.h:896): LRU 模式 = 分钟级时钟 (lruclock 全局, 22bit 演进到 24); LFU 模式 = `(LFUGetTimeInMinutes() << 8) | LFU_INIT_VAL` (object.c:38) — **高 16bit 访问时间 + 低 8bit 频率计数器**
- initObjectLRUOrLFU (L32-43): 按 maxmemory_policy 初始化; **共享对象跳过** (refcount==OBJ_SHARED_REFCOUNT → return) — 共享对象无 LRU (q3 的 NO_SHARED_INTEGERS 关联: maxmemory 下共享整数无法跟踪 LRU)
- objectCommand 的 IDLETIME/FREQ (L1442+): 读 lru 字段计算

## 代码类型
Implementation (按需解码) + Interface (双用途字段)

## 跨域关联
- R-23 (LRU/LFU 淘汰) → lru 消费者
- q3 (INT 编码) → 解码面
- R-20 (TYPE/OBJECT 命令) → 展示面

## 结论
解码按需临时 (INT 免长期字符串占用); lru 24bit 双模式 (LRU 时钟/LFU 频率+时间), 共享对象无 lru (关联 maxmemory 禁共享整数)。比较路径用值直接比, 多数场景免解码。
源码位置: object.c:32-43,685-715; server.h:896
