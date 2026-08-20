# 闭环笔记 q3: 事件分派顺序 — 读先写后 + BARRIER 逆序

## 假设
同一 fd 读事件先于写事件; BARRIER 标志强制逆序 (先写后读); 同 proc 只调一次。

## 验证过程
- 分派循环 (ae.c:391-443):
  - invert = fe->mask & AE_BARRIER (L408)
  - 非逆序: 读先 (L416-420) → 写后 (L423-428)
  - 逆序: 写先 (L423-428) → 读后 (L432-440)
- **去重** (L424, L435): `!fired || fe->wfileProc != fe->rfileProc` — 读写同 proc 且已调 → 跳过 (一次回调处理双事件)
- **resize 刷新** (L419, L433): 回调可能改事件表 (realloc) → 重新取 fe 指针
- 有效掩码 (L416): `fe->mask & mask & AE_READABLE` — 回调可能已删事件 (注释 L410-412)
- BARRIER 用途 (L402-407 注释): beforeSleep 里 fsync 后统一回复 — AOF 持久化优先于应答
- fired 结构 (ae.h:72-75): fd + mask (epoll 层填充)

## 代码类型
Mechanism (分派顺序契约)

## 跨域关联
- R-20 (beforeSleep 的 AOF flush) / R-28 (networking 读写回调)

## 结论
分派 = "读先写后" (可立即应答已读请求) + BARRIER 逆序 (AOF 先落盘再应答) + 同 proc 去重 (一次回调处理 R+W)。回调内可改事件表 (删/重注册), 分派代码通过 mask 复查 + 指针刷新保证安全。
源码位置: ae.c:391-443
