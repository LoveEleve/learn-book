# Reasonix 域发现 v36 补充(续扫第二十二轮:remote/sftpfs)— 2026-08-14

> 承接:v35。本轮:internal/remote/sftpfs + appidentity。
> 结论:sftpfs 的截断读确认,深化 ②远程执行;无新域。

---

## 一、v36 深化确认

| 设计 | 位置 | 要点 |
|------|------|------|
| **sftpfs 远程文件系统** | sftpfs/sftpfs.go | FS(ssh.Client 包装)/List/Stat/**ReadFile(maxSize 截断读——返回 truncated 标志 + 类型检测)**;run() 操作超时上下文 |
| **类型检测** | detect.go | DetectKind(样本 → 类型);**截断 rune 处理**(TestDetectKindTruncatedRune——截断的 UTF-8 不误判) |
| **appidentity** | appidentity/ | 平台身份(Unix/Windows) |

---

## 二、关键设计(通用价值)

1. **"远程截断读"**:maxSize 截断 + truncated 标志——**远程读的上下文保护**(与 tool_result_storage 同哲学)
2. **"截断 rune 不误判"**:部分 UTF-8 字节的类型检测——**边界正确性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v35 | — | 102 | 102 |
| v36 | remote/sftpfs | +0(深化 1 设计) | **102**(深化) |

> **Reasonix 续扫 22 轮完成**:internal/ 96 包全部覆盖(含 8 个大文件 agent.go/task.go/usecapability/agentpreset/event/permission/tool/compact 的源码级验证)。剩余:desktop 前端(排除)/i18n(数据)/providers 适配(同构)——已到边际。
