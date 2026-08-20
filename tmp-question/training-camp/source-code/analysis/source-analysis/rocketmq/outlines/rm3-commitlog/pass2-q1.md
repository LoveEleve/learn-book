# 闭环笔记 q1: 消息编码 — 18 段字段序 + calMsgLength

## 假设
固定字段序 + 长度计算; 版本差异 (V1/V2) + V6 地址标志。

## 验证过程
- **字段序 18 段** (MessageExtEncoder.encode L200-278): TOTALSIZE(4) → MAGICCODE(4) → BODYCRC(4) → QUEUEID(4) → FLAG(4) → QUEUEOFFSET(8) → PHYSICALOFFSET(8, 写后回填) → SYSFLAG(4) → BORNTIMESTAMP(8) → BORNHOST(8/20) → STORETIMESTAMP(8) → STOREHOST(8/20) → RECONSUMETIMES(4) → PreparedTransactionOffset(8) → BODY(int 长+数据) → TOPIC(short/byte 长, **MESSAGE_VERSION_V2 → short**) → PROPERTIES(short 长) → **CRC32 保留位**
- **calMsgLength** (L60-83): 固定头求和 + 变长 (body/topic/properties) + **V6 标志** (BORNHOST_V6_FLAG/STOREHOSTADDRESS_V6_FLAG → 8B→20B IPv6 地址)
- **双上限**: maxMessageBodySize / maxMessageSize (MESSAGE_ILLEGAL); **PROPERTIES_SIZE_EXCEEDED** (propertiesLength > Short.MAX_VALUE, L198-205)
- **CRC32 保留**: crc32ReservedLength 尾部占位 (属性追加分隔符 + 保留区)
- **Batch** (encode L282): 批量消息打包 (msgLen 前缀逐条)
- **encodeWithoutProperties** (5.x): 多分派场景免属性编码 (Compaction 面)

## 代码类型
Implementation (二进制编码)

## 跨域关联
- RM-2 (MappedFile): 编码入缓冲
- RM-8 (消费): MessageDecoder 对称解码

## 结论
消息格式 = 18 段定长+变长混合; calMsgLength 精确预算; V2 版本 (topic 长 short) + V6 地址 (8/20B) 双兼容; 双上限 + 属性超限拒绝。
源码位置: MessageExtEncoder.java:60-83,175-278,282-320
