# 闭环笔记 q8: 协议安全面 — 未认证限制与上限

## 假设
安全面 = 未认证客户端的资源限制 (1MB querybuf/16384 bulk/10 参数) + 全局上限 (client_max_querybuf_len/proto_max_bulk_len)。

## 验证过程
- **未认证限制** (authRequired, L103-111):
  - querybuf: mstate + querybuf > 1MB → 断开 (networking.c:2745)
  - multibulk 计数: ll > 10 → 协议错误 (L2323-2326)
  - bulk 长度: ll > 16384 → 协议错误 (L2375-2378)
  - 依据: 未认证客户端无法执行命令, 防资源洪水 (连接吃 buffer 不干活)
- **全局上限**:
  - proto_max_bulk_len (L2371): 默认 512MB (server.h 默认值, config 可调) — master 豁免 (L2371: !CLIENT_MASTER)
  - client_max_querybuf_len (L2744): 默认 1GB (config)
  - PROTO_INLINE_MAX_SIZE = 64KB (L2304,2348): 计数行超长报错
  - INT_MAX: multibulk 计数上限 (L2319)
- 协议错误处理 (setProtocolError L2252-2291): 记日志 + CLIENT_PROTOCOL_ERROR 标记 + 连接关闭 (防死循环解析)
- 认证后限制解除 (L2745: authRequired 为假)

## 代码类型
Mechanism (安全防护)

## 跨域关联
- R-32 (ACL 认证) / R-4 (querybuf 上限) / R-20 (proto_max_bulk_len 配置)

## 结论
安全 = **未认证阶段收紧, 认证后放宽**: 未认证只能发 ≤10 参数/≤16KB bulk/≤1MB 总量; 认证后靠全局上限 (512MB bulk/1GB querybuf)。协议错误一律断开 — 不重试不降级。
源码位置: networking.c:103-111,2252-2291,2323-2378,2739-2755
