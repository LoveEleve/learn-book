# q18 — Policy + LocationMutation(深度版:声明式策略 + 路径安全边界)

> 域:安全 | 文件:core/src/policy.ts(49)+ location-mutation.ts(162)+ core/test/(policy/location-mutation/location-filesystem).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

Policy = 声明式工具可见性策略(action/resource/effect,allow|deny,无 ask——询问在 PermissionV2)。LocationMutation = 路径解析的安全边界:相对路径必须留在 Location,绝对路径在外需 external_directory 审批。

## 验证

### 1. Policy(设计 1:与 PermissionV2 同族但更简单)

```ts
// policy.ts:10-47
Policy.Info = { action, effect: "allow"|"deny", resource }
evaluate(action, resource, fallback):statements.findLast(双匹配)?.effect ?? fallback
load(statements) / hasStatements()
// 与 PermissionV2.evaluate 相同模式,但:无 ask(效果只有 allow/deny),fallback 调用方提供
// 用途:工具定义可见性过滤(与 registry 的 whole-tool 过滤配合)
```

### 2. LocationMutation(设计 2:三层校验)

```ts
// location-mutation.ts:120-150 resolve:
1. 词法校验:相对路径必须 FSUtil.contains(location.directory, absolute)
   —— 相对逃逸(relative_escape)拒绝
2. 真实路径校验:词法内部但 realPath 后逃出 locationRoot → location_escape 拒绝(符号链接穿越!)
3. 资源派生:
   内部 → resource = 相对 locationRoot 的路径
   外部 → resource = canonical 绝对路径 + externalDirectory = { action: "external_directory", resource: dir/*, save }
// resolvePath(location-mutation.ts:90-118):realPath 存在 → canonical + type;
//   不存在 → 向上找最近 canonical 目录祖先(non_directory_ancestor 错误),canonical = resolve(anchor, relative)
```

**设计要点**:
- 符号链接逃逸在 realPath 层被拦截(词法在内 ≠ 实际在内)
- external_directory 是独立权限 action(需要单独审批),save = dir/*(一次批准该目录所有)
- resolve 只解析不审批("This does not approve the mutation",location-mutation.ts:56-57)

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Policy(allow/deny + fallback,无 ask) | policy.ts:10-47 | ②工具可见性 |
| 2 | LocationMutation 三层校验(词法/真实/资源) | location-mutation.ts:120-150 | ②路径安全 |
| 3 | external_directory 独立审批边界 | location-mutation.ts:135-148 | ②边界权限 |
| 4 | resolve 只解析不审批 | location-mutation.ts:56-57 | ②职责分离 |

## 面试弹药

- "词法在内 ≠ 实际在内":realPath 后逃出 locationRoot 照样拒绝(符号链接穿越防护)
- "external_directory 一次批准一个目录":resource = dir/*,save = dir/*——边界清晰可审计
- "resolve 与审批分离":路径解析只派生资源,审批在 PermissionV2——单一职责

## 待深挖

- [ ] policy.test.ts 的边界契约
- [ ] 工具 leaves 如何使用 LocationMutation(bash/apply-patch 的 workdir 校验)
