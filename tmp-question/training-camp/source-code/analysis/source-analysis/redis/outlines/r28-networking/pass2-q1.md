# 闭环笔记 q1: createClient — 连接客户端结构

## 假设
createClient 初始化查询态 + 回复态全字段, 并注册读事件。

## 验证过程
- createClient (networking.c:112-211):
  - 连接面 (L119-125): TCP_NODELAY (L120) / keepalive (L121-122) / **connSetReadHandler(readQueryFromClient)** (L123) — 读事件注册在这里
  - 查询态 (L146-159): qb_pos/querybuf(sdsempty)/reqtype=0/argc/argv=NULL/argv_len/multibulklen=0/bulklen=-1
  - 回复态 (L126-143): **buf = zmalloc_usable(PROTO_REPLY_CHUNK_BYTES 16KB)** (L126) + bufpos=0 + buf_peak
  - 回复链表 (L179-184): reply = listCreate + free/dup 方法 (clientReplyBlock)
  - 身份 (L128-136): next_client_id 原子递增 + resp=2 (LOG_REQ_RES 时 client_default_resp)
  - 阻塞/pubsub/watch (L185-190): blocking 态 + watched_keys + pubsub 三 dict
  - linkClient (L208): 挂入 server.clients 链表
- connSetReadHandler 底层 → aeCreateFileEvent (R-2 连接)
- 伪客户端: conn=NULL (L115-118, Lua 脚本/AOF 加载用)

## 代码类型
Glue (客户端初始化)

## 跨域关联
- R-2 (读事件注册) / R-20 (acceptCommonHandler) / R-1 (robj) / R-4 (sds)

## 结论
client = 查询态 + 回复态双结构: 解析状态 (qb_pos/multibulklen/bulklen) 与发送状态 (bufpos/sentlen/reply) 分离。16KB 静态 buf 起步, 链表兜底大回复。读事件在创建时即注册 (R-2 aeCreateFileEvent)。
源码位置: networking.c:112-211
