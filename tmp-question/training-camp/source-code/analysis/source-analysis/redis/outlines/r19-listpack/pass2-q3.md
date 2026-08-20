# 闭环笔记 q3: 整数嗅探 — 字符串当整数存

## 假设
lpEncodeGetType 把"能严格解析为 int64 的字符串"编码为整数 (如 "123" → 7BIT_INT) — 省空间; 判定用严格的 string2ll 语义。

## 验证过程
- lpInsert L850-860: `enctype = lpEncodeGetType(elestr,size,intenc,&enclen); if (enctype == LP_ENCODING_INT) eleint = intenc;` — 字符串先嗅探, 能编码整数就用整数编码
- lpEncodeGetType (L660+): 调 lpStringToInt64 — 成功 → 按值域选整数编码并编码进 intenc; 失败 → LP_ENCODING_STR
- lpStringToInt64 (L154+, 移植自 utils.c string2ll, 2011 Pieter Noordhuis): 严格语义 — 无空格/无前导零 (除 "0")/int64 范围/LONG_STR_SIZE 上限
- 收益: "123" 存为 7BIT_INT (2B) vs 6BIT_STR (1B 头+3B 数据+backlen) — 小整数大省
- 对称性: lpGet 读整数时若调用方要字符串 → 数字转字符串 (intbuf, LP_INTBUF_SIZE=21)
- 注意: hash 的 field "1" 与整数 1 是同构的 (存取对称)

## 代码类型
Algorithmic (类型嗅探) + Glue (string2ll 移植)

## 跨域关联
- R-25 (t_hash 字段/值) → HSET "count" "5" 场景
- R-10 (t_stream) → 消息 ID 等数字字段
- utils.c (string2ll 源) → 复用

## 结论
字符串写入时先做严格 int64 嗅探: 能解析 → 整数编码 (省 50%+); 不能 → 字符串编码。string2ll 的严格性保证"整数↔字符串"无损往返 (无前导零/空格歧义)。
源码位置: listpack.c:154-179,317,850-860
