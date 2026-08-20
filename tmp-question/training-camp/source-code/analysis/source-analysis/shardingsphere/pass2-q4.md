# Pass 2 闭环笔记 SS-4: 分布式主键算法的真实范围

## 初始假设
- 执行计划里的 `Snowflake/UUID/LEAF` 三种算法都在当前主战场源码中实现。

## 验证过程
- 主战场目录 `infra/algorithm/type/key-generator` 下实测可见：
  - `SnowflakeKeyGenerateAlgorithm`
  - `UUIDKeyGenerateAlgorithm`
  - SPI 接口 `KeyGenerateAlgorithm`
- 未找到执行计划意义上的 LEAF 算法实现类；仅在 `mode/api` 的测试 fixture 命名中出现 `leaf`，不构成现行主战场能力。
- 同时，大量 test fixture 也围绕 `KeyGenerateAlgorithm` SPI 进行扩展验证，说明这一域的核心是“SPI + 两个内置实现”，不是某个第三种算法族。

## 结论

SS-4 规划应先收敛为：`KeyGenerateAlgorithm` SPI + `Snowflake` + `UUID` 两个内置实现。`LEAF` 不能在当前阶段写成现行核心算法，只能作为执行计划遗留名词被审计纠正。