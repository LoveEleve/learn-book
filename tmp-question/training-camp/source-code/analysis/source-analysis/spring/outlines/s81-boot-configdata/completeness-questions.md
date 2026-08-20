# S-17 外部化配置深化 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么改默认搜索路径/加配置位置? | §3 (spring.config.location, DEFAULT_SEARCH_LOCATIONS) |
| 2 | optional: 前缀作用? | §3 (找不到不报错) |
| 3 | 为什么 application-dev.yml 覆盖 application.yml? | §3 (AFTER 阶段优先) |
| 4 | 我加的 EnvironmentPostProcessor 什么时候跑? | §1 (environmentPrepared 事件, 按 ORDER) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么分阶段加载? | §2 (profile 可能定义在配置里) |
| 6 | 为什么配置文件用 addLast 垫在命令行之下? | §3 (C-3 索引 0 优先级最高) |
| 7 | 为什么 file:/ 比 classpath 优先? | §3 (先 addLast → 索引更小) |
| 8 | 贡献者为什么是树? | §2 (spring.config.import 递归) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | application.yml 到底怎么被找到的? | §3 (DEFAULT_SEARCH_LOCATIONS) |
| 10 | profile 在配置里定义时怎么处理? | §2 (两轮加载) |
| 11 | 优先级顺序一句话? | §3 (命令行>系统>环境>dev>yml>default) |

## 覆盖: 11 问 / 3 身份 / 100%
