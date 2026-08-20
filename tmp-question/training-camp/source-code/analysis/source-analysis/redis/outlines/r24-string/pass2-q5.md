# 闭环笔记 q5: GETEX/GETDEL — 读后改 TTL 三路径

## 假设
GETEX = 读 + 三路径 (过期删/设置/移除); GETDEL = 读 + 删; 传播重写为单一命令。

## 验证过程
- getexCommand (t_string.c:340-397):
  - 解析 (L345, COMMAND_GET: 支持 PERSIST/EX/PX/EXAT/PXAT, 不支持 NX/XX/GET/KEEPTTL)
  - lookup + checkType (L351-356) + **先校验 expire 再回复** (L358-362)
  - **回复先行** (L365): addReplyBulk — 值先返回
  - **三路径**:
    1. **PXAT/EXAT 已过期** (L369-378): dbGenericDelete (lazyfree_lazy_expire) + **重写 DEL/UNLINK** (L374-375) — 绝对戳已过 → 直接删
    2. 有 expire (L379-388): setExpire + **重写 PEXPIREAT <绝对毫秒>** (L384, R-22 归一)
    3. PERSIST (L389-396): removeExpire + 重写 PERSIST (L392)
  - 注释 (L367-368): "never propagated as is" — 统一改写
- getdelCommand (L399-408): getGenericCommand (L400) + dbSyncDelete (L401) + **重写 DEL** (L403)
- 共同点: 读命令但**传播为写命令** (命令表标记 CMD_READONLY 不适用 — 实际是 RW)

## 代码类型
Mechanism (读改写合一)

## 跨域关联
- R-22 (PEXPIREAT 归一/checkAlreadyExpired) / R-21 (dbGenericDelete)

## 结论
GETEX/GETDEL = "读 + 副作用" 合体: 值先返回, 然后 TTL 三路径 (删/设/移) 各自重写为单一传播命令。PXAT 已过期删键与 EXPIRE 的 checkAlreadyExpired (R-22) 同语义。
源码位置: t_string.c:340-408
