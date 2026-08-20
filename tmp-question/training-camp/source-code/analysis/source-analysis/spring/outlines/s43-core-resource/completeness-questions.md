# C-1 Resource 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Value("classpath:xxx") 注入 Resource 时, 字符串怎么变成对象？ | §3 (ResourceEditor→getResource 五路分派) |
| 2 | classpath: 和 file: 和 http:// 三种位置分别对应什么实现？ | §2 (三实现对照) |
| 3 | 为什么读 jar 内的配置文件用 classpath: 而不是相对路径？ | §2 (无真实 File 概念, classLoader 解析) |
| 4 | 资源不存在时会发生什么？ | §1 (exists 判定) + §2 (getInputStream 抛 FileNotFoundException) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 Spring 要抽象 Resource 而不是直接用 java.io.File/URL？ | §1 关键设计 (句柄与读取解耦) |
| 6 | 五路分派顺序的优先级逻辑是什么？ | §3 关键设计 (特殊→通用) |
| 7 | 我想支持自定义协议(如 git://)怎么扩展？ | §3 (ProtocolResolver) |
| 8 | exists() 为什么不用 File.exists 一个实现搞定？ | §1 模板 + §2 (无 file 语义的实现) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | exists() 判定"存在"的兜底思路是什么？ | §1 (开流探测) |
| 10 | AbstractResource 为什么不支持的方法抛 FileNotFoundException？ | §1 (默认异常, 统一 IO 语义) |

## 覆盖: 10 问 / 3 身份 / 100%
