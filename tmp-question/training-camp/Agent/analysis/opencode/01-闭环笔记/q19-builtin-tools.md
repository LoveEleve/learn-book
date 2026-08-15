# q19 — 内置工具 leaves(深度版:统一权限序列 + 原子性 + 输出契约)

> 域:②执行(工具实现) | 文件:core/src/tool/(bash 207/apply-patch 219/read 117/write 101/read-filesystem 156?/websearch/webfetch/grep/glob/edit/skill/todowrite/question)+ core/test/(tool-bash/tool-apply-patch/tool-read/tool-write).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

内置工具共享统一模式:source 构造 → LocationMutation.resolve → external_directory 审批(如有)→ 主权限 assert → 执行 → 结构化输出。叶子之间差异在权限序列顺序与原子性。

## 验证

### 1. 统一权限序列(设计 1:四步模板)

```ts
// 每个 leaf 的 execute 开头(bash.ts:124-149, read.ts:54-79, write.ts:64-86, apply-patch.ts:85-123):
const source = { type: "tool", messageID: context.assistantMessageID, callID: context.toolCallID }
const target = yield* mutation.resolve({ path, kind: "file"|"directory" })
if (target.externalDirectory) → permission.assert(externalDirectoryPermission)  ← 外部目录先批
permission.assert({ action, resources: [target.resource], save: [...], agent, source })  ← 主权限
```

**顺序规律**:
- external_directory 审批在 read 内容/执行之前("All targets are resolved and approved before target contents are read" apply-patch.ts:72)
- save 语义:read=save["*"] / bash=save[command] / apply-patch=save["*"] / edit 系共享 "edit" action(withPermission)
- 参数扫描(外部命令目录)是 **advisory 警告,不是审批边界**(bash.ts:138-141)

### 2. bash(设计 2:无沙箱 + 超时语义)

```ts
// bash.ts:19-21,122-196
DEFAULT_TIMEOUT 2min / MAX_TIMEOUT 10min / MAX_CAPTURE_BYTES 1MB
execute:workdir resolve(kind directory)→ external 审批 → 命令参数外部目录扫描(warnings)→ bash 审批
ChildProcess.make(cwd, shell, stdin ignore, detached, forceKillAfter 3s)
appProcess.run(combineOutput, timeout, maxOutputBytes)
// 超时 → 返回 { timeout: true }(非失败)——模型可见可重试
// 截断 → "[output capture truncated at the in-memory safety limit]"
// 结构化输出:truncated/exit/timeout;toModelOutput 补 "Command exited with code X."
// description 明示:host user's filesystem/process/network authority(无沙箱声明)
```

### 3. apply_patch(设计 3:三阶段 + CAS)

```ts
// apply-patch.ts:85-189
阶段 1 解析:patchText → Patch.parse;空 patch 拒绝;move 显式拒绝
阶段 2 预检:所有 hunk → mutation.resolve → external 去重逐个审批 → edit 一次性审批(全资源 + save["*"])
阶段 3 应用:顺序应用,每步:
  add → files.create(补 \n)
  delete → files.remove
  update → files.writeIfUnchanged({ expected: source, content })  ← CAS(并发修改检测!)
// 失败语义:后续失败 → 前面保留 + "Patch partially applied before failing at X. Applied: ..."
// BOM:decode ignoreBOM + FEFF 去除 + Patch.joinBom 恢复(写回原 BOM)
// 输出:applied 列表 + FileDiff.Info(patch/additions/deletions/status)
```

**设计要点**:apply_patch 是"预检后应用"的完整样本——所有目标解析+批准都发生在读内容之前,保证"批准时看到的就是执行时的"。CAS 写入防覆盖并发修改。

### 4. read(设计 4:类型分派 + 图像)

```ts
// read.ts:53-105
mutation.resolve(kind directory)→ external 审批 → reader.inspect(类型)→ read 审批
- directory → list(offset/limit 分页)
- text → read(offset/limit 行分页)
- base64 + 支持图像 mime(jpeg/png/gif/webp)→ image.normalize(ResizerUnavailable 降级)→ toModelOutput 发 file
- base64 非图像 → BinaryFileError(失败)
// 错误映射:BinaryFileError/MediaIngestLimitError/Image 错误透传消息,其他 → "Unable to read path"
```

### 5. write(设计 5:最小 + BOM 保留)

```ts
// write.ts:63-88
mutation.resolve(kind file)→ external 审批 → edit 审批(资源 + save["*"])→ files.writeTextPreservingBom
// 输出:{ operation: "write", target, resource, existed }(模型看到 Wrote/Created)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 统一四步权限序列(source→resolve→external→assert) | bash/read/write/apply-patch | ②权限模板(产品可抄) |
| 2 | bash 无沙箱 + 超时=结构化结果(非失败) | bash.ts:19-21,122-196 | ②执行语义 |
| 3 | apply_patch 三阶段 + CAS + 部分失败报告 | apply-patch.ts:85-189 | ②原子性/失败报告(产品验收器可抄) |
| 4 | read 类型分派 + 图像规范化 + 错误映射 | read.ts:53-105 | ②读取契约 |
| 5 | write 最小 + BOM 保留 | write.ts:63-88 | ②写入契约 |

## 面试弹药

- "预检后应用":apply_patch 所有目标 resolve+批准先于读内容——批准与执行的资源视图一致,无 TOCTOU 窗口(除了文件本身)
- "CAS 写入":writeIfUnchanged(expected bytes)——目标被并发修改 → 失败,不静默覆盖
- "部分失败显式报告":后续失败 → 前面保留 + "Applied: a, b"——模型知道已发生什么,可自纠
- "超时是结构化结果不是错误":timeout: true 进模型上下文——可重试,而非终止
- "advisory ≠ 审批":bash 参数外部目录扫描只给警告,不拦截——诚实标注"这不是沙箱"
- "无沙箱明示":description 里写清楚 host-user authority——模型知道边界在哪

## 待深挖

- [ ] read-filesystem.ts(分页细节/媒体限制)
- [ ] file-mutation.ts(CAS 实现/writeTextPreservingBom)
- [ ] grep/glob/websearch 的权限序列对比
