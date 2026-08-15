# q28 — Config 系统(深度版:优先级合并 + V1 迁移)

> 域:配置 | 文件:core/src/config.ts(227)+ core/src/config/*(agent/attachments/command/compaction/experimental/formatter/lsp/markdown/mcp/plugin/provider/reference/tool-output/watcher)+ opencode/src/config/(config.ts/parse.ts/managed.ts/variable.ts/tui.ts)+ core/test/config/
> review 轮次:2 轮(源码全文)

---

## 假设

配置 = "低优先级 → 高优先级"的文档序列;同名键后者覆盖(latest findLast)。V1 配置自动迁移;policy 规则顺序相反(用户全局覆盖仓库)。Location 打开时读取一次(缓存)。

## 验证

### 1. 发现与优先级(设计 1:三来源合并)

```ts
// config.ts:142-203
文件名:opencode.json / opencode.jsonc(带 .opencode 目录补充)
发现:fs.up(targets: [".opencode", "opencode.jsonc", "opencode.json"], start: location, stop: project)
directories = [global, ...向上发现的 .opencode(反转——"closer wins")]
directPaths = 发现的直接配置文件(反转)
合并顺序(config.ts:201-203):"global config, project files, then .opencode files"
  = [...supplementary[0], ...direct, ...supplementary.slice(1).flat()]
// latest(config.ts:122-126):findLast(entry.info[key] !== undefined)——低→高,后者胜
```

### 2. V1 迁移(设计 2:自动)

```ts
// config.ts:155-159
ConfigMigrateV1.isV1(input) → decodeV1Info → migrate → decodeInfo(双层解码)
// 旧格式自动转换,新代码只看 Info
```

### 3. Policy 反向顺序(设计 3:全局规则覆盖仓库)

```ts
// config.ts:206-211
policy.load(configs.toReversed()...policies)
// 注释:"Rules use the opposite order so a user-global rule can override a repository rule"
// ——设置后者胜,规则前者胜(全局优先)
```

### 4. 缓存语义(设计 4:Location 打开时读一次)

```ts
// config.ts:175-176:"Read configuration once when this location opens. Later calls reuse these values until the location is reopened."
// entries() 返回缓存数组(不可变)
```

### 5. 解析容错(设计 5:宽松解码)

```ts
// config.ts:143-152
decodeOptions:{ errors: "all", onExcessProperty: "ignore", propertyOrder: "original" }
parse:allowTrailingComma
// 解析失败/解码失败 → 静默跳过该文件(不崩溃)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 三来源合并(global < project < .opencode) | config.ts:142-203 | ①配置优先级 |
| 2 | V1 自动迁移(双层解码) | config.ts:155-159 | ④演进兼容 |
| 3 | Policy 反向顺序(全局规则优先) | config.ts:206-211 | ②安全规则优先 |
| 4 | Location 打开缓存 + 宽松解析 | config.ts:143-176 | ②性能 + 容错 |

## 面试弹药

- "设置后者胜,规则前者胜":配置覆盖方向与安全规则方向相反(用户全局 deny 永远压过仓库 allow)
- "V1 自动迁移":isV1 → migrate → decode——旧文件对用户无感升级
- "宽松解码":onExcessProperty ignore + 解析失败跳过——配置不阻塞启动
- "closer wins":fs.up 从目录开始,反转后应用——项目级覆盖全局,嵌套覆盖外层

## 待深挖

- [ ] opencode/src/config/managed.ts(托管配置)
- [ ] parse.ts(markdown/config 解析器)
- [ ] 各 config/* 模块 schema(agent/compaction/mcp 等)
