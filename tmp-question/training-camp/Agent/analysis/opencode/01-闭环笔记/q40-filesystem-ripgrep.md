# q40 — Filesystem + Ripgrep(深度版:保护路径 + 搜索适配器 + 监控)

> 域:共享基础设施 | 文件:core/src/filesystem/(fff.bun 140/fff.node 138/ignore 67/protected 53/search 239/watcher 140)+ fs-util.ts(274)+ ripgrep.ts(281)+ core/test/(filesystem/ripgrep).test.ts
> review 轮次:2 轮(源码全文核心)

---

## 假设

文件系统层四件套:FFF(双运行时文件系统)、Protected(保护路径清单)、Ripgrep(搜索适配器,进程原语不掺权限)、Watcher(parcel watcher 封装)。fs-util 是全局文件工具(向上发现/mime/路径安全)。

## 验证

### 1. Protected(设计 1:平台保护清单)

```ts
// protected.ts:6-51
DARWIN_HOME:Music/Pictures/Movies/Downloads/Desktop/Documents/Public/Applications/Library
DARWIN_LIBRARY:AddressBook/Calendars/Mail/Messages/Safari/Cookies/TCC(隐私敏感!)
DARWIN_ROOT:DocumentRevisions/Spotlight/Trashes/fseventsd(系统)
WIN32_HOME:AppData/Downloads/Desktop/Documents/Pictures/Music/Videos/OneDrive
names():home 目录扫描跳过;paths():绝对路径永不 watch/stat/scan
// 用途:watcher/扫描/读取全部绕过——隐私与性能
```

**设计要点**:macOS 的隐私目录(TCC/AddressBook/Mail)显式保护——agent 不会扫描用户隐私数据。

### 2. Ripgrep(设计 2:进程原语适配器)

```ts
// ripgrep.ts:1-28 注释:
// "Small core-owned ripgrep execution adapter. It deliberately exposes raw
//  process-oriented rows, not model text or permission behavior."
//   ——工具负责展示与权限,ripgrep 只出原始行
RawMatch schema:path/lines/line_number/absolute_offset/submatches(start/end)
限制:ERROR_BYTES 8k / MAX_RECORD_BYTES 64k / MAX_SUBMATCHES 100
FindInput:cwd/pattern/limit/hidden/follow/signal/onEntry(流式回调)
```

### 3. Watcher(设计 3:parcel watcher 封装)

```ts
// watcher.ts:22-40
lazy 加载 @parcel/watcher 平台绑定(glibc 选择,失败返回 undefined——降级无监控)
getBackend:win32 → windows;darwin → fs-events;linux → inotify
SUBSCRIBE_TIMEOUT 10s
// ignore.ts:gitignore 语义;protected:保护路径过滤
// watcher 事件 → EventV2(filesystem-watcher schema)
```

### 4. FFF 双实现(设计 4:与 database 同模式)

```ts
// fff.bun.ts(140)/fff.node.ts(138):同接口(FileSystem Service)两运行时实现
// (fff = "fast file finder"?)
// search.ts(239):文件搜索(glob/内容)
```

### 5. fs-util(设计 5:全局工具)

```ts
// fs-util.ts:14-49:FileSystemError + DirEntry + Interface extends FileSystem.FileSystem(全接口 + 扩展)
// 224-270:纯函数——mimeType/normalizePath/normalizePathPattern/resolve/windowsPath/overlaps/contains
//   contains(parent, child):路径包含检查(权限/安全核心,大量使用)
// up():向上发现(targets, start, stop)——指令/配置发现共用
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 平台保护清单(隐私目录) | protected.ts:6-51 | ②隐私边界(产品可抄) |
| 2 | Ripgrep 进程原语(不掺权限) | ripgrep.ts:1-28 | ②职责分离 |
| 3 | Watcher 平台绑定 + 降级 | watcher.ts:22-40 | ②文件监控 |
| 4 | FFF 双实现 + search | fff.*.ts + search.ts | ②可移植 |
| 5 | fs-util 纯函数(mime/contains/up) | fs-util.ts:224-270 | ②共享工具 |

## 面试弹药

- "隐私目录显式保护":macOS TCC/AddressBook/Mail 永不扫描——agent 的隐私边界是代码强制
- "Ripgrep 不掺权限":进程原语只出原始行,展示/权限在工具层——单一职责
- "watcher 平台绑定降级":绑定加载失败 → 无监控(不崩溃)——渐进增强
- "contains 是安全核心":路径包含检查(词法)被权限/工具/项目全用——共享纯函数

## 待深挖

- [ ] ignore.ts 的 gitignore 解析
- [ ] search.ts 的搜索算法
- [ ] fff 的具体接口
