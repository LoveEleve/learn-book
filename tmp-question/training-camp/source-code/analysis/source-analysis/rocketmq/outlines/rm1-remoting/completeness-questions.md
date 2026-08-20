# RM-1 remoting 协议层 — completeness-questions (全视角提问验证)

## 开发者视角

1. 帧格式各字段位宽? (4B 总长 + 4B 头长[高 8 位类型+低 24 位头长] + header + body)
2. 双序列化怎么选? (SerializeType: JSON 默认 / ROCKETMQ 二进制, 服务端全局配置)
3. 怎么区分请求和响应? (flag bit0 RPC_TYPE)
4. 一个请求怎么找到处理线程? (code → processorTable → 独立线程池)
5. 响应怎么找到等待者? (opaque → responseTable)
6. 三调用模式差异? (Sync 等待/Async 回调/Oneway 免注册)
7. 超时怎么算? (从取通道开始扣, 分级关闭)
8. suspended 是啥? (长轮询挂起标记, 独立字段非 flag)

## 架构师视角

9. 类型信息放头长高字节的动机? (零额外开销协议协商)
10. 每处理器独立线程池 vs 共享? (命令隔离 — 慢命令不拖垮心跳)
11. 为什么 5.x 加二进制序列化? (JSON 反射慢/体积大 — 性能面)
12. fastEncodeHeader 零拷贝解决什么? (ByteBuf 直写免中间数组)
13. 超时分级的取舍? (误杀健康连接 vs 残留僵尸连接)
14. 协议错误即断连? (对照 Redis 同策略 — 快速失败)
15. RPCHook 与协议解耦的收益? (认证可插拔)
16. 为什么不做连接级协议协商? (简单性 — 全局配置)

## 学生视角

17. opaque 是什么? (请求 ID, 类比 HTTP 请求 ID)
18. 粘包/拆包为什么不用管? (Netty LengthFieldBasedFrameDecoder)
19. 同步调用底层也是异步? (Future 等待 — Netty 全异步)
20. 16MB 帧上限防什么? (巨型帧内存攻击)
