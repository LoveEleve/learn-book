# Reasonix 域发现 v52 补充(续扫第三十八轮:sessioninbox disk/cmd 入口)— 2026-08-14

> 承接:v51。本轮:internal/sessioninbox/disk.go(431)+ cmd/reasonix/main。
> 结论:blob 校验和验证与私有目录权限确认,无新域。

---

## 一、v52 深化确认

### sessioninbox/disk(431)

| 设计 | 位置 | 要点 |
|------|------|------|
| **blob 校验和** | disk.go:67-89 | writeBlobLocked(带校验和)/**readBlobLocked(wantChecksum 验证)**——信封完整性 |
| **私有目录权限** | :90-107 | ensurePrivateDir + **validatePrivateDir(权限校验)** |
| **隔离/抢救** | :179-274 | quarantineFileLocked/gcOrphansLocked/**salvageOrphanBlobsLocked(孤儿 blob 抢救恢复为条目)** |
| **有限读** | :108 | readRegularFile(maxBytes)——有界读 |

### cmd 主入口

| 设计 | 要点 |
|------|------|
| **崩溃捕获** | runWithCrashCapture(exitCode 返回)——入口层崩溃处理 |

---

## 二、关键设计(通用价值)

1. **"孤儿 blob 抢救"**:孤儿 blob 抢救恢复为条目(而非删除)——**数据的可恢复性**(与 memory 归档、curator 归档同哲学)
2. **"校验和验证读"**:信封读取验证校验和——**存储完整性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v51 | — | 102 | 102 |
| v52 | sessioninbox disk/cmd 入口 | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 chat_tui.go(5,569)主体/workers/cmd 工具——按需。
