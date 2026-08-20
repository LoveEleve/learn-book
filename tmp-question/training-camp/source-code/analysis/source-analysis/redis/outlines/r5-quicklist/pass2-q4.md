# 闭环笔记 q4: recompress — 解压后延迟重压

## 假设
压缩节点的读写要解压; 操作完成后不立即重压, 而是置 recompress 标志 — 同一次操作序列中多次访问免重复压解。

## 验证过程
- __quicklistDecompressNode (quicklist.c:260-270): 解压 + recompress=0
- quicklistDecompressNode 宏 (L283-290): 解压后 `(_node)->recompress = 1` — **标记"用完要重压"**
- 重压触发 (L380-390 注释): "If the 'recompress' flag of the node is true, we compress it directly" — 批量操作结束 (如 quicklist 变更后) 统一重压带标志的节点
- 头尾守卫 (L311-312): `assert(head->recompress == 0 && tail->recompress == 0)` — 头尾永不置重压标志
- 收益: 一次命令中的多次节点访问 (如 LINDEX 遍历) 只解压一次; 命令结束统一重压
- 风险: recompress 节点若未及时重压 → 内存峰值 (解压态); 但这是设计取舍 (延迟重压 vs 频繁压解)

## 代码类型
Implementation (延迟重压) — 访问优化

## 跨域关联
- q3 (压缩) → 配套机制
- R-26 (LINDEX/LRANGE 遍历) → 消费场景
- 内存面: 解压态内存峰值

## 结论
recompress = "读时解压, 用完标记, 批量后重压": 一次操作内多次访问免重复压解; 头尾永不标记。延迟重压的代价是操作间隙的内存峰值 — 换取压解频率的大幅降低。
源码位置: quicklist.c:260-290,311-312,380-390
