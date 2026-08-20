# 闭环笔记 q8: 命令层衔接 — codec 显式传参 + Decoder 响应侧选择

## 假设
codec 不存于 RedisCommand, 而是作为参数沿命令执行链显式传递; 响应侧 CommandDecoder 用 CommandData 携带的 codec 选 decoder。

## 验证过程
- 命令发起: `CommandAsyncService.evalWriteAsync(String key, Codec codec, RedisCommand<T> evalCommandType, String script, ...)` (L471) — **codec 显式参数**
- 创建 CommandData: `createCommandData` 把 codec 打包进 CommandData (type, key, codec, command, params)
- 响应侧 `CommandDecoder.selectDecoder` (L565-575): 
  - data==null → `StringCodec.INSTANCE.getValueDecoder()` (L570, 兜底)
  - 否则 `multiDecoder.getDecoder(data.getCodec(), paramIndex, state, size, parts)` (L573) — **从 CommandData 取 codec, 交给 ReplayDecoder 选 decoder**
- RedisCommand 自身**不带 codec** (纯命令元数据: 类型/名称/replay 逻辑) — 分离关注点: 命令格式静态, 编解码随调用动态
- 结构层调用: RedissonLock.evalWriteAsync(...codec...) 传对象 codec / 命令参数用原型

## 代码类型
Glue (协议接线) — 关注点分离

## 跨域关联
- RD-4 (命令流水线) → 核心衔接点
- RD-2 (锁 Lua) → eval 时 codec 显式传
- Q6 (原型) → 命令参数原型 vs 值业务 codec

## 结论
codec 沿调用链显式参数传递 (不进 RedisCommand), 响应侧从 CommandData.codec 取 decoder (selectDecoder L570-573). 设计: 命令格式静态/编解码动态分离。data==null 时 StringCodec 兜底保证解码不 NPE。
源码位置: CommandAsyncService.java:471, CommandDecoder.java:565-575