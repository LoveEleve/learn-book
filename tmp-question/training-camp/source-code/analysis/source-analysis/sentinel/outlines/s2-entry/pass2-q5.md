# Pass 2 闭环笔记 Q5: SphO — boolean 版入口的转译语义

## 初始假设
- SphO 与 SphU 只是 API 形状不同, 内部行为一致。

## 验证过程
- 读 `SphO.java:79-205` (9 个 entry 重载): 全部转发 `Env.sph.entry(...)`, 与 SphU 同路径。
- 读 `SphO.java:180-194` (最终实现):
  ```
  try { Env.sph.entry(name, trafficType, batchCount, args); }
  catch (BlockException e) { return false; }
  catch (Throwable e) { RecordLog.warn("SphO fatal error", e); return true; }  // 关键!
  return true;
  ```
- **差异点(行为层面)**: catch Throwable → **return true(放行)** — "Sentinel 内部意外错误不拦截业务流量"(与 CtSph 的 catch Throwable 不抛同哲学)。
- 读 `SphO.java:219-225` (exit): `ContextUtil.getContext().getCurEntry().exit(...)` — 直接取当前线程 curEntry, 要求 entry/exit 同线程配对(类注释明确: 错配抛 ErrorEntryFreeException)。

## 代码类型
- Glue(API 形状适配, 零规则逻辑)

## 跨域关联
- S-2 → S-3: 返回 false 的语义由规则层 BlockException 驱动, 本域只做转译

## 结论
SphO = SphU 的 boolean 转译壳: BlockException→false, **Throwable→true 放行**(SphO.java:180-194), exit 直接走当前线程 curEntry(SphO.java:219-225)。除 "内部错误放行" 语义外与 SphU 无行为差异。