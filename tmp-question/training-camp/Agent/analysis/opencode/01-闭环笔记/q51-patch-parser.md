# q51 — Patch 解析器(深度版:标记格式 + 宽容匹配 + 推导)

> 域:②执行(文件编辑前端) | 文件:core/src/patch.ts(197)+ file.ts(6)+ schema/src/file-diff.ts(13)
> review 轮次:2 轮(源码全文)

---

## 假设

apply_patch 的输入格式 = 标记式(*** Begin/End Patch + Add/Delete/Update File),不是标准 unified diff。解析器宽容匹配(四档比较器)是容错核心;derive = 从 chunks 推导完整新文件内容(BOM 保留)。

## 验证

### 1. 格式(设计 1:标记式)

```ts
// patch.ts:3-11
Hunk = Add{path, contents} | Delete{path} | Update{path, movePath?, chunks}
UpdateFileChunk = { oldLines, newLines, changeContext?, endOfFile? }
// 结构:
*** Begin Patch
*** Add File: path
+内容
*** Update File: path
@@ context
 上下文行(空格前缀)
-删除行
+新增行
*** End Patch
// stripHeredoc:支持 cat <<'EOF' 包装(模型输出常见)
```

### 2. 解析(设计 2:三命令 + 严格校验)

```ts
// patch.ts:25-69
parse:找 Begin/End 标记(缺失 → "Invalid patch format")
- Add:行必须 + 前缀("Invalid add file line")
- Delete:仅路径
- Update:可带 "*** Move to:"(movePath)+ 至少一个 @@ chunk
- 未知行 → "Invalid patch line"(严格)
```

### 3. 宽容匹配(设计 3:四档比较器)

```ts
// patch.ts:160-193
seek(lines, pattern, start, eof):依次尝试四种比较:
1. exact:逐字节相等
2. rstrip:trimEnd 后相等(忽略尾随空白)
3. trim:两侧 trim 后相等(忽略缩进!)
4. normalized:Unicode 规范化后相等(弯引号/破折号/省略号/不间断空格 → ASCII)
// eof:从文件末尾对齐匹配(追加场景)
// 失败 → "Failed to find expected lines"(带上下文错误)
```

**设计要点**:模型生成的补丁常见"缩进/引号"漂移——四级降级匹配吸收;最后仍失败给精确错误。

### 4. derive(设计 4:chunks → 完整内容)

```ts
// patch.ts:71-81
source = splitBom(original)
lines = text.split("\n")(去尾空)
computeReplacements:逐 chunk 找匹配(changeContext 先定位)→ [start, remove, insert]
updated = 应用所有替换(倒序应用,防偏移)
尾部补 \n;BOM:source.bom || next.bom(保留原 BOM)
// computeReplacements(patch.ts:132-158):
//   changeContext → seek 定位后继续
//   oldLines 空 → 追加到文件末尾
//   尾空行 fallback:oldLines 去尾空重试(常见模型输出差异)
//   多个替换排序后应用
```

### 5. File.Diff(设计 5:diff 模型)

```ts
// file.ts + schema/src/file-diff.ts:
Info = { file?, patch?, additions, deletions, status: added|deleted|modified }
// apply_patch 输出/快照 diff 的公共形状(q16/q19 用)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 标记式格式(Add/Delete/Update + Begin/End) | patch.ts:3-11 | ②补丁格式 |
| 2 | 严格解析(三命令 + 错误定位) | patch.ts:25-69 | ②输入校验 |
| 3 | 四级宽容匹配(exact→normalized) | patch.ts:160-193 | ②容错(模型输出漂移) |
| 4 | derive(BOM 保留 + 倒序应用 + 尾空 fallback) | patch.ts:71-81 | ②正确性 |
| 5 | File.Diff 公共形状 | file-diff.ts | ③diff 契约 |

## 面试弹药

- "四级宽容匹配":exact → rstrip → trim → Unicode 规范化——模型补丁的缩进/引号漂移全部吸收
- "倒序应用防偏移":多个替换从后往前应用——行号不失效
- "heredoc 支持":模型爱用 cat <<EOF 包装——解析器剥壳
- "失败给精确错误":找不到预期行 → 报出期望内容——模型可自纠

## 待深挖

- [ ] V1 的 apply_patch 解析对比(与 core 版本差异)
- [ ] 补丁的安全边界(路径校验在 LocationMutation,q19)
