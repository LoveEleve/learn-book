# 闭环笔记 q4: resp3 命令适配 — RESP2/3 双协议映射表

## 假设
RESP3 模式下部分命令需要换用 V2 版本 (返回结构支持 RESP3 的 map/array 语义)。resp3(command) 用静态映射表做版本转换。

## 验证过程
- ServiceManager:686 `cfg.getProtocol() == Protocol.RESP3` — 协议判断
- RESP3MAPPING (ServiceManager.java:689-705): XREADGROUP/XREADGROUP_BLOCKING/XREADGROUP_BLOCKING_SINGLE/XREADGROUP_SINGLE/XREAD_BLOCKING_SINGLE/XREAD_SINGLE/XREAD_BLOCKING/XREAD/HRANDFIELD/VSIM_WITHSCORESATTRIBS/ZRANGE_SINGLE_ENTRY/ZRANGE_ENTRY/ZREVRANGE_ENTRY → 各 _V2 版
- 受影响命令类型: **Stream 读族 (XREAD/XREADGROUP) + ZSet 范围读 + HRANDFIELD + 搜索** — 响应含 map/嵌套结构, RESP3 用 map 类型表达
- resp3() 方法: 查表 → 有 _V2 版则在 RESP3 模式替换, 无则原命令 (未完读, 基于 L689 静态表 + 命令面语义推断)
- 呼应 r28-networking: 服务端 RESP2/3 握手 (HELLO), 客户端按协议模式选命令版本

## 代码类型
Interface (协议适配) — 静态映射 + 模式判断

## 跨域关联
- r28 (RESP 协议) → HELLO 协商后的双协议
- Q5 (SORT_RO) → 同类"协议能力"适配
- RD-0 (导论 RESP2/3) → 展开点

## 结论
RESP3 适配 = 静态映射表: Stream/ZSet 读族等复杂响应命令在 RESP3 下用 _V2 版 (map 语义)。协议模式由 Config.protocol 决定, 命令面版本随协议切换 — 这是"协议演进兼容"的优雅做法。
源码位置: ServiceManager.java:686,689-705