# hq13 工具结果持久化(Tool Result Storage)— 产品②"上下文防溢出"蓝本

> 项目:Hermes(tools/tool_result_storage.py 254 行 + tools/budget_config.py + agent/tool_executor.py 三层接入 + tools/registry.py 阈值)
> 假设:上下文防溢出是三层防线(工具内截断/单结果持久化/回合聚合预算),Hermes 把大工具结果写盘+预览替换,是"上下文保护"的完整样本。
> 结论:✅ 成立——三层防线/沙箱写/stdin 传输/文件名安全/回合预算全具备,产品②"上下文防溢出"直接蓝本。

---

## 一、架构全景:三层防线

```
问题:大工具结果(搜索 100K 行/单行 600KB/日志洪泛)撑爆上下文——
     截断丢信息,全量进上下文炸预算。三层防线:

┌────────────────────────────────────────────────────────────┐
│ L1 工具内截断:search_files 等工具自己预截断(工具作者控制)    │
├────────────────────────────────────────────────────────────┤
│ L2 单结果持久化:maybe_persist_tool_result                  │
│   结果返回后 > 注册阈值(registry.get_max_result_size)      │
│   → 全量写沙箱临时目录(env.execute,任何后端可 read_file)   │
│   → 上下文中替换为 <persisted-output> 预览 + 路径引用       │
├────────────────────────────────────────────────────────────┤
│ L3 回合聚合预算:enforce_turn_budget                        │
│   一回合所有工具结果收集后 > MAX_TURN_BUDGET_CHARS(200K)    │
│   → 最大的未持久化结果先溢出到盘,直到聚合低于预算           │
│   ——抓"多个中型结果合计溢出"(每个都低于 L2 阈值)           │
└────────────────────────────────────────────────────────────┘
```

**三层关系**:L1 工具作者控制;L2 单结果;L3 聚合(补 L2 盲区:6×42K=252K 每个都 <100K 阈值但合计超 200K)。

---

## 二、设计 1:L2 单结果持久化(阈值 + 预览替换)

**位置**:`tool_result_storage.py:144-200`(maybe_persist_tool_result)+ `169`(阈值解析)

```
流程:
1. effective_threshold = 显式 threshold 或 config.resolve_threshold(tool_name)
2. threshold == inf → 原样返回(该工具不限制)
3. len(content) <= threshold → 原样返回
4. 超限 → 写沙箱(_write_to_sandbox)+ 返回 <persisted-output> 替换块

替换块(_build_persisted_message):
  <persisted-output>
  This tool result was too large (N characters, X KB/MB).
  Full output saved to: <path>
  Use the read_file tool with offset and limit to access specific sections...
  Preview (first N chars):...
  </persisted-output>

回退:写失败/无 env → 内联截断("Full output could not be saved to sandbox")

接入(tool_executor.py:1471):
  function_result = maybe_persist_tool_result(...) if not _is_multimodal_tool_result
  ——多模态结果(图像块)不替换(保持块结构)

阈值解析链(budget_config.py:37-59,优先级 pinned > overrides > registry > default):
  - PINNED_THRESHOLDS:read_file = inf(永不持久化)——★ 有意设计:
    "prevents infinite persist->read->persist loops"(77c5bc9da 引入):
    read_file 结果若再落盘,模型为读它又调 read_file → 结果又落盘 = 死循环
  - tool_overrides(env 级)次之
  - registry 每工具值 cap 到 default_result_size(上下文缩放预算 #23767:
    小模型窗口下 registry 大固定值(web/terminal 100K)不能重新撑大 cap)
  - registry inf 直接返回(工具作者显式声明不限制)

★ 预算随模型窗口缩放(_budget_for_agent,tool_executor.py:78-89,#23767 同源):
  _tool_budget = budget_for_context_window(context_length) 或 DEFAULT_BUDGET
  ——小模型(65K token 本地模型)窗口按比例缩预算,单大工具结果不能推过
   模型上限;L2/L3 阈值都随此缩放(缩放降 default_result_size,cap 防
   registry 大值重新撑大——同一 #23767 的两面)

★ 双守卫关系(易误读为矛盾,实为设计):
  - PINNED read_file=inf:防"读→落盘→再读"死循环(对 read_file 特例放行)
  - 测试 test_read_file_registry_cap_is_100k 禁 registry 层 inf:
    "float('inf') is not allowed — it disables the Layer 2 result-size guard"
    ——防的是"任何工具 registry 被改 inf 导致 L2 全失效"
  两层保护不同面:PINNED 是工具特例,registry 测试是通用守卫。
  (resolve_threshold("read_file")=inf 但 registry("read_file")=100K,
   实测验证一致——笔记 v1 声称"read_file/search_files=100K"不精确,
   search_files=100K 生效,read_file 因 PINNED 实际不持久化)
```

**正确性价值**:
1. 全量写盘 + 预览进上下文——模型用 read_file 按需读(保头保尾 + 分页)
2. 写失败降级内联截断(不丢信息但可见"截断了")
3. 多模态结果豁免(块结构不可破坏)

**产品④映射**:知识库大块内容(源码/日志)同理——全量落盘 + 指针 + 按需分页读(与 OpenCode 工具输出托管同族,但用沙箱文件而非托管存储)。

## 设计 2:stdin 传输(128KB argv 天花板教训)

**位置**:`tool_result_storage.py:100-116`(_write_to_sandbox)+ 注释

```
问题(历史):heredoc-in-command-string 方案——Linux MAX_ARG_STRLEN 限单个
  argv 元素 128KB(32*PAGE_SIZE),任何 >~128KB 工具结果静默失败
  "OSError: [Errno 7] Argument list too long"——恰是持久化要处理的场景。

修复:内容走 stdin(stdin_data=content)而非命令串:
  cmd = "mkdir -p <dir> && cat > <path>"
  result = env.execute(cmd, timeout=30, stdin_data=content)
  → local + ssh(_stdin_mode=="pipe")无上限;heredoc 后端保持 API-body 限
    (比 exec-arg 天花板大几个数量级)

测试(test_write_to_sandbox):
- test_large_content_via_stdin:200KB 内容,cmd < 1000 字符,stdin_data == 内容
- test_success:内容不在命令串中(stdin 验证)
- 路径空格/元字符($();等)shlex.quote 中和(4 个注入测试)
```

**正确性价值**:**大内容永远不嵌入命令串**——argv 天花板 + 注入双风险同一修复。

**产品④映射**:知识库写大内容的传输纪律——数据走 stdin/流,不走命令参数(上限 + 注入双防)。

## 设计 3:文件名安全(路径穿越 + 注入)

**位置**:`tool_result_storage.py:64-79`(_safe_result_filename)+ `44-45`(正则)

```
_safe_result_filename(tool_use_id):
  1. 非 [A-Za-z0-9_.-] 字符 → "_"
  2. 空结果 → "tool_result"
  3. 变更或超 120 字符 → sha256(raw_id)[:12] 后缀 + 截断
  → "<stem>.txt"(永不含 / $ ; 等)

测试(test_safe_result_filename + test_tool_use_id_cannot_escape_storage_dir):
- "../outside/$(whoami);x" → outside_whoami_x_<hash>.txt
- 路径穿越 "../" 消除;命令注入 $()/; 消除
- 断言:Full output saved to 路径无 /../,$() 不出现在目标
```

**正确性价值**:tool_use_id 是模型/工具可影响输入——直接拼路径 = 穿越 + 注入;净化 + 哈希后缀双保险。

**产品④映射**:知识库文件名的外来输入净化(内容寻址后缀防碰撞)。

## 设计 4:L3 回合聚合预算(补 L2 盲区)

**位置**:`tool_result_storage.py:203-254`(enforce_turn_budget)

```
流程:
1. 收集:遍历 turn 内 tool 消息,算 total_size;未含 <persisted-output> 的进候选
2. total <= config.turn_budget → 原样返回
3. 超限 → 候选按大小降序 → 逐个 maybe_persist_tool_result(threshold=0 强制)
   → total 减旧加新 → 直到 <= 预算或候选耗尽
4. 原地变异消息列表并返回

接入(tool_executor.py:1573):
  finalize 且 num_tools>0 → enforce_turn_budget(最后 N 条 tool 消息)
  ——/steer 注入在预算强制之后(steer 标记永不截断,注释明确)

测试(test_enforce_turn_budget):
- test_medium_result_regression:6×42K=252K(每个 <100K 阈值但合计超 200K)
  → ≥2 个被持久化(需消 ~52K)——L3 抓 L2 盲区的回归测试
- test_under_budget_no_changes / test_empty_messages
```

**正确性价值**:L3 专门抓"多中型合计溢出"(L2 每个都放行);强制 threshold=0 复用 L2 持久化路径。

**产品④映射**:知识库回合级聚合预算——单条有阈值,回合有总量(双层预算,Reasonix 多层阈值同族)。

## 设计 5:预览生成(行对齐截断)

**位置**:`tool_result_storage.py:82-90`(generate_preview)

```
- len <= max_chars → 原样(has_more=False)
- 截到 max_chars,回退到 > max_chars//2 的最后一个换行(行对齐,不切半行)
- 返回 (preview, has_more)

测试:短内容原样 / 精确边界(max_chars 整)不触发截断
```

**产品④映射**:预览生成的行对齐纪律——半行截断会破坏结构化输出。

## 设计 6:heredoc 标记冲突防护

**位置**:`tool_result_storage.py:93-97`(_heredoc_marker)

```
- 默认 HERMES_PERSIST_EOF;内容已含该标记 → uuid 后缀生成新标记
- 测试:无冲突默认 / 冲突生成 UUID 标记且不在内容中
```

> ★ review 补深(2026-08-15 深度 review 发现):`_heredoc_marker` **全仓无调用点**
> (grep 仅函数定义 + 测试引用)——stdin 传输改造后(_write_to_sandbox 统一走
> stdin_data)heredoc 标记生成是**死代码**,测试 TestHeredocMarker 测的是死函数。
> 笔记 v1 声称"残留于 heredoc 模式后端"不准确——当前代码路径完全不走 heredoc;
> 仅远端后端内部(_stdin_mode=="heredoc")保留 API-body 限,但那在后端层,
> 不经此函数。产品实现勿复制此死代码。

**产品④映射**:嵌入定界符的冲突防护(内容含标记时换标记)——如产品需要
heredoc 路径须实现此防护,但默认 stdin 路径不需要。

---

## 三、与四项目对比(上下文防溢出)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes tool_result_storage |
|------|----|----------|----------|-----|---------------------------|
| L1 工具内截断 | 工具预设分级 | — | 输出托管(bounded preview) | 工具结果修剪(tool-result-pruner) | **工具自控(搜索类预截断)** |
| L2 单结果持久化 | — | — | **bounded preview + 托管文件** | 修剪(上下文) | **全量写沙箱 + 预览 + read_file 指针** |
| L3 回合聚合 | — | 多层阈值(0.85/50%/硬上限) | — | — | **200K 回合预算(最大先溢出)** |
| 传输 | — | — | — | — | **stdin(128KB argv 教训)** |
| 文件名安全 | — | — | — | — | **净化 + 哈希后缀(穿越/注入双防)** |

**结论**:产品"上下文防溢出"参考 = Hermes 三层(工具截断/单结果持久化/回合预算)+ OpenCode 输出托管(托管文件指针)+ dsh 工具结果修剪(修剪语义)。**Hermes 与 OpenCode 同"指针替代"哲学;Hermes 独有 stdin 传输 + 回合聚合预算**。

---

## 四、面试弹药

1. **"128KB argv 天花板"**(#22906):heredoc 在命令串中,Linux MAX_ARG_STRLEN 限 128KB——恰是持久化要处理的场景静默失败;内容走 stdin 移除上限(local+ssh)
2. **"L3 抓 L2 盲区"**:6×42K 每个 <100K 阈值但合计 252K 超 200K 预算——单条阈值放行不等于回合安全,聚合预算补上
3. **"tool_use_id 是注入面"**:模型可影响输入拼路径 = 穿越 + 命令注入;净化 + sha256 哈希后缀双保险
4. **"多模态结果豁免"**:图像块结构不可替换——只对文本结果做持久化
5. **"写失败降级截断不丢信息"**:无 env/写失败 → 内联截断 + 可见声明("could not be saved")——诚实降级
6. **"steer 在预算后注入"**:/steer 标记永不截断——预算强制先跑,纠偏文本后加

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| L1 工具内截断 | 工具作者控制的第一道防线 |
| L2 单结果持久化 | 大结果全量落盘 + 预览 + 指针 |
| stdin 传输 | 大内容不嵌命令串(argv + 注入双防) |
| 文件名安全 | 外来输入净化 + 哈希后缀 |
| L3 回合聚合预算 | 双层预算(单条阈值 + 回合总量) |
| 预览行对齐 | 半行截断防结构化破坏 |
| heredoc 冲突防护 | 嵌入定界符冲突兜底 |
| 多模态豁免 | 块结构不可替换 |

> 覆盖设计数:6(设计 1-6)+ 3 子细节
> 测试契约:test_tool_result_storage.py(26 用例:预览/heredoc/写沙箱 6 注入测试/存储目录/文件名/消息结构/阈值/持久化/回合预算/注册表阈值集成)
> 接入点:tool_executor.py:1471(L2 单结果)/1573(L3 回合预算,steer 前)/2273(L2 另一路径,均含多模态豁免)
> 常量:DEFAULT_TURN_BUDGET_CHARS=200K / DEFAULT_RESULT_SIZE_CHARS=100K(read_file/search_files 注册阈值,测试锁定 inf 禁止)/ DEFAULT_PREVIEW_SIZE_CHARS=1500
