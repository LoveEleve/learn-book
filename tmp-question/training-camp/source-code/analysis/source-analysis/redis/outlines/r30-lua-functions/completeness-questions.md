# R-30 Lua+Functions — completeness-questions (全视角提问验证)

## 开发者视角

1. EVALSHA 为什么可能失败? (缓存未命中 → NOSCRIPT; SHA 长度≠40 快速失败)
2. SCRIPT LOAD 和 EVAL 的缓存差异? (LOAD 不参与 LRU 淘汰)
3. shebang 里能声明什么? (7 标志: no-writes/allow-oom/allow-stale/no-cluster/allow-cross-slot 等)
4. redis.call 里哪些命令会失败? (六重验证: arity/NOSCRIPT/stale/ACL/写允许/OOM + 跨槽)
5. 脚本内 SELECT 会影响外部吗? (不会 — script_client 独立 DB)
6. KILL 已写脚本会发生什么? (拒绝 — 只能 SHUTDOWN NOSAVE)
7. FUNCTION LOAD 失败会半生效吗? (不会 — 双 ctx 原子+回滚)
8. 为什么 math.random 是确定的? (传播一致性 — 从库重放必须同结果)

## 架构师视角

9. 双缓存 (registry+dict) 为什么? (执行句柄 vs 传播素材分离)
10. 白名单 + 递归只读的双层沙箱取舍? (装载面最安全 vs 运行期锁灵活)
11. shebang 标志 vs 2.6 保守检查的设计演进? (声明式合同减少拒绝面)
12. effects 传播 (命令级) 相比"脚本整体广播"的收益? (从库精确重放/只读不传播)
13. busy 模式降级设计? (原子性 → 可用性, 受限命令面)
14. Functions 双 ctx 原子加载 vs EVAL 单命令? (库级原子 vs 脚本级)
15. 引擎抽象为什么只有 Lua? (抽象留了扩展点, 实现成本/收益权衡)
16. LRU 500 为什么只淘汰 EVAL? (显式管理脚本不驱逐)

## 学生视角

17. "原子性"到底指什么? (单线程内不交错; 超时后降级为受限重入)
18. 为什么 io 库进不来而 os 能进? (白名单精细到函数)
19. EVAL 和 FUNCTION 区别一句话? (临时脚本 vs 命名持久化库)
20. 脚本为什么能"写"从库? (effects 传播 — 从库逐命令重放)
