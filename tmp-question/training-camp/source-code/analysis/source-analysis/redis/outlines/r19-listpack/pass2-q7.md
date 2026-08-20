# 闭环笔记 q7: 头部惰性计数 + 完整性校验

## 假设
num_elements 头部字段是"尽力而为"缓存: 超 65535 (LP_HDR_NUMELE_UNKNOWN) 时全扫描计数, 计数在范围内回填; lpValidateIntegrity 校验头部/EOF/逐元素。

## 验证过程
- LP_HDR_NUMELE_UNKNOWN = UINT16_MAX (listpack.c:27) — 16bit 计数字段上限
- lpLength (L505-521): 读缓存非 UNKNOWN → 直接返回; UNKNOWN → 全扫描 + **count < 65535 时回填** (L520) — 惰性缓存
- 写路径 (L937-944): 非 UNKNOWN 时 ±1 更新; UNKNOWN 时不维护 (扫描兜底)
- lpValidateIntegrity (L1541+): 头部大小可读 (≥7B) / **total_bytes == 实际 size** (L1548-1550, 防伪造头) / 末字节 == LP_EOF (L1553) / deep 模式逐元素校验 (编码合法性/lpValidateNext)
- 防御面: RDB 加载损坏数据 / 网络攻击构造畸形 listpack (object.c 加载时校验)

## 代码类型
Implementation (惰性缓存) + Algorithmic (完整性校验)

## 跨域关联
- R-8 (RDB 加载) → lpValidateIntegrity 主场景
- R-25/R-10 (消费方长度查询) → lpLength 高频
- 安全面: 畸形 listpack 防御

## 结论
头部计数是惰性缓存 (超 65535 降级全扫描+回填), 写路径只维护缓存值; 完整性校验双保险: 头部字段一致性 + deep 逐元素 — 防 RDB/网络喂入的畸形数据引发越界。
源码位置: listpack.c:27,505-521,1541-1553
