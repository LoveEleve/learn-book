# Selector 工程优化 — 生产环境的正确性与性能

## 概念依赖链

```
Q4 (KeySet 优化) → Q5 (wakeup race) → Q9 (rebuild 双路径)

性能优化 → 并发正确性 → 故障自愈 — 按麻烦程度递增
```

## 叙事顺序

1. **问题引入** — 从篇一 select 循环过渡
   - 篇一结尾的 `processSelectedKeys()` — 这里有一个巨大的性能问题待解开
   - `selector.selectedKeys()` 返回 HashSet — 100 个注册 Channel，3 个有事件 → 还是要遍历 100 个 key 查 readyOps？

2. **Q4：SelectedSelectionKeySet — 替换 HashSet**
   - 反射注入 `selectorImpl.selectedKeys` + `publicSelectedKeys` → `SelectedSelectionKeySet`（`NioIoHandler.java:143-200`）
   - `openSelector()`（`NioIoHandler.java:143`）— AccessController + Unsafe.putObject 替换两个 Set 字段
   - 数组 + size 计数器：`processSelectedKeysOptimized()`（`NioIoHandler.java:563`）只遍历 `selectedKeys.size` 个选中的 key
   - 性能对比：100 注册 vs 3 选中 → 30x 遍历量差

3. **过渡：select 中间的并发 bug — 有人在你阻塞时调了 wakeup**

4. **Q5：wakenUp CAS — select 与 wakeup 竞态**
   - 标准流程：`wakenUp.set(false)` → `selector.select(timeout)` → check
   - Race BAD：`wakenUp.set(false)` 和 `selector.select()` 之间 → 线程 B 调 `wakeup()` → CAS 成功 → select() 阻塞 — 预期唤醒不生效
   - Netty 解决：select() 后 `if (wakenUp.get()) { selector.wakeup(); }`（`NioIoHandler.java:464-466`）— 双重检查
   - 代价：BAD case 改正了，但 OK case 多了一次无害的 wakeup — wakeup 幂等

5. **过渡：再怎么优化竞态，Selector 本身可能坏掉**

6. **Q9：rebuildSelector 双路径**
   - 内部自动：select 循环 → selectCnt ≥ 512（`NioIoHandler.java:697-702`）
   - 外部主动：`NioEventLoop.rebuildSelector()`（`NioEventLoop.java:161-171`）— inEventLoop 检查保证线程安全
   - 两条路径汇聚 `NioIoHandler.rebuildSelector0()`（`NioIoHandler.java:255`）— 新 Selector → 迁移所有 key → 关闭旧

7. **收束：Selector 的三层防护**
   ```
   性能层:    SelectedSelectionKeySet        → 只遍历选中的 key
   并发层:    wakenUp CAS + post-select       → 防止唤醒丢失
   自愈层:    selectCnt ≥ 512 → 重建 Selector → 修复 JDK epoll bug
   ```
   三层的每一层都只在单个 EventLoop 内部的 Selector 上工作。但实际生产中有几百个 EventLoop——Channel 怎么被分配到特定的 EventLoop？整个线程池怎么管理？引出篇三。

## 核心悬念

**"Netty 对 JDK Selector 的改装是全方位的——不满足于包装 API，而是用反射注入数据结构、用 CAS 消灭唤醒竞态、用自动重建修复 OS 层 bug。"**
