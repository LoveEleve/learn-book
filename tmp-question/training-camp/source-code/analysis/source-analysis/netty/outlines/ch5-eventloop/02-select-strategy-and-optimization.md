# Ch5 SelectStrategy 与 Selector 数组优化

> Cluster C: 8 KPs | 依赖 §5.1 run() | §5.1 → §5.2

### 1. SelectStrategy 三态 — 什么时候等 IO

场景: EventLoop 的 `runIo()` 到了 select 环节——现在 submit 队列有 5 个任务在等待。是 select() 阻塞等 IO(让任务等), 还是跳过 select 直接 runAllTasks(让 IO 等)?

源码路径: `SelectStrategy.java:31-39` — 三态常量: `SELECT=-1`(阻塞 select 等待 IO)、`CONTINUE=-2`(跳过 select, 立即回到 IO 循环, 用于任务密集时)、`BUSY_WAIT=-3`(非阻塞轮询, 不等任何事件直接返回)。`SelectStrategy.java:51` — `calculateStrategy(IntSupplier selectSupplier, boolean hasTasks)`: 有任务→调 `selectSupplier.getAsInt()`(相当于 selectNow() 非阻塞轮询) → 返回结果; 无任务→返回 SELECT。`DefaultSelectStrategy.java:29-31` — INSTANCE 单例: `hasTasks ? selectSupplier.getAsInt() : SELECT`。

关键设计: 当 hasTasks=true 时, DefaultSelectStrategy 仍然调用 `selectSupplier.getAsInt()` 做一次 selectNow()——有可能这个 selectNow 立即返回了就绪的 Channel(IO 任务也有)。如果 selectNow 返回 0(无就绪)且 hasTasks, 返回值为 0→EventLoop 把 0 视为 CONTINUE 语义——跳过 select, 直接 runAllTasks。这个设计让 task 密集型场景下 IO 不会被完全忽略——每次 runAllTasks 前都用 selectNow 快速 peek 一下有没有 IO 就绪。

数据流: `calculateStrategy(selectNow, hasTasks=true)` → `selectNow.getAsInt()` 返回 0(无 IO) → 0→CONTINUE → 跳过阻塞 select → `runAllTasks()`。`calculateStrategy(selectNow, hasTasks=false)` → 直接返回 SELECT(-1) → 阻塞 `selector.select()` → 等待 IO。

### 2. SelectedSelectionKeySet — 数组替代 HashSet

场景: Ch3 的 JDK Selector.selectedKeys() 返回 HashSet——每次 select 后遍历 selectedKeys 要创建 Iterator 对象, add/contains 有哈希开销。10K 连接场景下, Iterator 对象分配频繁触发 GC。

源码路径: `SelectedSelectionKeySet.java:25-31` — `extends AbstractSet<SelectionKey>`, `keys = new SelectionKey[1024]` 预分配。`add(key)` — O(1) 尾部追加(`keys[size++] = key`), 无去重(Selector 保证不重复), 满时 `increaseCapacity()` 翻倍(SelectedSelectionKeySet.java:35-46,104-108)。`remove()` — 永远返回 false, selectedKeys 不需要随机删除(SelectedSelectionKeySet.java:48-51)。`reset(int start)` — `Arrays.fill(keys, start, size, null)` + `size=0`——部分重置支持 `needsToSelectAgain`(SelectedSelectionKeySet.java:95-101)。

关键设计: 数组为什么比 HashSet 快? 1) add() 无哈希计算和冲突处理, 纯 `keys[i]=key; i++`。2) 遍历 selectedKeys 不创建 Iterator 对象——直接 `for(int i=0; i<size; i++) { process(keys[i]); keys[i]=null; }`——GC 零分配。3) remove 不需要——Ch3 的 selectedKeys 遍历后需要 `iterator.remove()`, 数组用 `size=0` 一次清空。但数组为什么可以替代 HashSet? 因为 JDK Selector.selectedKeys 只需要两种操作: add(sel_count) 和 clear(counters)——无随机删除和随机查找。数组刚好覆盖。

数据流: `selector.select()` → JDK SelectorImpl 内部用 selectedKeys(已被替换为数组) → `selectedKeys.add(key)` → `keys[size++] = key` → select 返回 → NioIoHandler 遍历: `for(int i=0; i<selectedKeys.size; i++) { processSelectedKey(keys[i]); keys[i]=null; }` → `selectedKeys.reset(0)` → `size=0`。

### 3. Selector 注入 — Unsafe/反射替换 JDK 内部字段

场景: 怎么把 `SelectorImpl.selectedKeys` 字段从 JDK 的 HashSet 替换为自定义的 SelectedSelectionKeySet? 不能改 JDK 源码——只能用反射或 Unsafe 注入。

源码路径: `NioIoHandler.java:143-234` — `openSelector()`: `provider.openSelector()` 创建 Selector → 反射获取 `sun.nio.ch.SelectorImpl` 类 → Java9+ Unsafe 路径: `objectFieldOffset(selectedKeys)` + `putObject(selector, offset, selectedKeySet)` 直接替换内部字段(NioIoHandler.java:186-204)——绕 Java9+ 反射限制。反射降级: Unsafe 失败→`ReflectionUtil.trySetAccessible()` + `Field.set()`(NioIoHandler.java:206-216)。注入结果: 成功→`SelectedSelectionKeySetSelector` 包装; 失败→`selectedKeys=null`→回退到 `processSelectedKeysPlain()`(Iterator 遍历)(NioIoHandler.java:224-233)。`DISABLE_KEY_SET_OPTIMIZATION` 系统属性(`io.netty.noKeySetOptimization`)可禁用(NioIoHandler.java:63-64)。

关键设计: Unsafe 注入是 Netty 最 hack 的操作之一——用 `sun.misc.Unsafe.putObject()` 直接修改 `SelectorImpl` 的 private 字段。Java9+ 以后对反射的限制更多(module system), 所以优先 Unsafe 路径(Unsafe 不受 module 限制)。但 Unsafe 不是万能的——某些 JVM 实现没有 `objectFieldOffset` 对应的字段→反射降级。两个路径都失败→selectedKeys=null→走 Plain 路径——用 Iterator 遍历原始 HashSet——功能正确但性能降级。

数据流: `openSelector()` → 反射/Unsafe 替换 selectedKeys → `new SelectedSelectionKeySetSelector(unwrapped, selectedKeySet)` → selector 返回 → select 后用 `processSelectedKeysOptimized()` 数组直接遍历。`selectedKeys==null` → `processSelectedKeysPlain()` 用 Iterator 遍历。

### 核心悬念

**"Selector 优化解决了性能问题——但 JDK 的 epoll 空轮询问题是 NIO 最著名的生产级 bug。Linux epoll_wait() 有时会被假唤醒——select 返回 0 但没有任何 Channel 真的就绪——导致 EventLoop 空转, CPU 100%。Netty 的 §5.3 用了三层防护: wakeup CAS race fix(第一层)、selectCnt 累积检测(第二层, SELECTOR_AUTO_REBUILD_THRESHOLD=512)、rebuildSelector0() 重建+迁移全量注册(第三层)。这三层防护是 EventLoop 能在生产环境 7×24 运行的基础。"**

→ 引出 §5.3 epoll bug 与 Selector 重建 — epoll 空轮询的三个层次: CAS 防护 wakeup 竞态→selectCnt 累积触发→重建 Selector。重建期间——旧 Selector 的 key 迁移到新 Selector 的时候, 发生新连接和新 I/O 事件——会被丢弃还是被新 Selector 捕获?
