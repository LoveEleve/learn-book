## Loop Note: Q3 — CompositeByteBuf 零拷贝组合

**Hypothesis**: CompositeByteBuf 内部用 `Component[]` 数组持有对原始 ByteBuf 的引用，零拷贝聚合多个 ByteBuf 为一个逻辑连续缓冲区。

**Verification** (CompositeByteBuf.java:49-2391):
- `Component[] components` (line 59) — 动态数组，按需扩容
- `int componentCount` (line 58) — 当前组件数量
- `int maxNumComponents` (line 56) — 默认 1024，超限自动 consolidate
- `addComponent0()` (line 280) — 分配 Component，retain srcBuf，计算 offset/endOffset，shiftComps 后移
- `removeComponent()` (line 613) — `comp.free()` (release srcBuf) → removeCompRange
- `findComponent(offset)` (line 1617) — weak cache `lastAccessed` + 二分查找 `findIt(offset)` (二分)
- `Component` 内部类 (line 1913): `srcBuf`(原始引用), `buf`(slice 视图), `offset`, `endOffset`, `srcIdx`, `length`
- `consolidate0()` (line 1774) — 超出 maxNumComponents 时合并组件到新的连续 buffer
- `discardSomeReadBytes()` (line 1818) — 移除已读组件，free 它们的引用

**Code type**: Algorithmic

**设计权衡**:
| 维度 | CompositeByteBuf | 传统拷贝方式 |
|---|---|---|
| 内存 | 零拷贝（引用+offset） | sizeof(total) 新分配 |
| 读取 | O(log n) 二分查找组件 + O(1) 组件内 | O(1) 直接索引 |
| 写入 | 复杂（需 splitting/merging 组件） | O(1) 直接索引 |
| 组件管理 | retain/release 每个组件的生命周期 | N/A |
| 碎片 | add/remove 会产生空隙（offset gap） | 无 |

**关键机制**: 
- 读 ByteBuf 时先 `findComponent(offset)` → 得到 Component → `component.buf.getByte(localIndex)`
- 写字节同理 → 但大范围写入可能触发 consolidate → 拷贝到新 buffer
- 弱缓存 `lastAccessed` 优化连续访问（common in sequential read）

**结论**: CompositeByteBuf = 零拷贝的关键。HTTP 响应头+体 = 两个 ByteBuf 拼成 CompositeByteBuf 发给 socket，全程无数组拷贝。代价是读性能降至 O(log n) vs O(1)。source: CompositeByteBuf.java:59,280,613,1617,1774,1818
