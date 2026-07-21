Understanding Linux Kernel version 4.4.0

TCP Protocol Part

Linux 4.4.0 内核源码分析TCP 实现

2020 年 3 月 10 日

# 1 准备部分 1

1.1 用户层 TCP . 1  
1.2 探寻 tcp_prot，地图 get~ . 1  
1.3 RFC 2

1.3.1 RFC793 : Transmission Control Protocol . . 2   
1.3.2 RFC1323 : TCP Extensions for High Performance . . . . 5   
1.3.3 RFC1337 : TIME-WAIT Assassination Hazards in TCP . . . . . 6   
1.3.4 RFC2018 : TCP Selective Acknowledgement Options . . . . . . 8   
1.3.5 RFC2525 : Known TCP Implementation Problems . . . . . . . 9   
1.3.6 RFC3168 : The Addition of Explicit Congestion Notification (ECN) to IP . . 9   
1.3.7 RFC6937 : Proportional Rate Reduction for TCP 10   
1.3.8 RFC7413 : TCP Fast Open(Draft) . . . 10

# 2 网络子系统相关核心数据结构 12

2.1 网络子系统数据结构架构 12  
2.2 sock 底层数据结构 12

2.2.1 sock_common 12   
2.2.2 sock 14   
2.2.3 request_sock 18   
2.2.4 sk_buff 19   
2.2.5 msghdr 23

2.3 inet 层相关数据结构 23

2.3.1 ip_options . . . 23   
2.3.2 inet_request_sock . . . 24   
2.3.3 inet_connection_sock_af_ops . . 24   
2.3.4 inet_connect_sock . . 25

2.3.5 inet_timewait_sock . . 26   
2.3.6 sockaddr & sockaddr_in . . . 27   
2.3.7 ip_options 28

# 2.4 路由相关数据结构 29

2.4.1 dst_entry 29   
2.4.2 rtable 30   
2.4.3 flowi . 31

# 2.5 TCP 层相关数据结构 31

2.5.1 tcphdr . . 31   
2.5.2 tcp_options_received 32   
2.5.3 tcp_sacktag_state . . 33   
2.5.4 tcp_sock . . 33   
2.5.5 tcp_fastopen_cookie . . . 38   
2.5.6 tcp_fastopen_request . . . 38   
2.5.7 tcp_request_sock . . . 38   
2.5.8 tcp_skb_cb . . . 39

# 3 TCP 输出 41

# 3.1 数据发送接口 41

3.1.1 tcp_sendmsg . . . 41   
3.1.2 tcp_sendmsg_fastopen . . 46   
3.1.3 TCP Push 操作 . . 46

# 3.2 输出到 IP 层 48

3.2.1 tcp_write_xmit . . . 48   
3.2.2 tcp_transmit_skb 51   
3.2.3 tcp_select_window(struct sk_buff *skb) 54

# 4 TCP 输入 58

# 4.1 Linux 内核网络数据接收流程概览 . 58

# 4.2 自底向上调用与自顶向下调用 59

4.2.1 自底向上处理 . . . 59  
4.2.2 自顶向下处理 . . . 65

# 5 TCP 建立连接 77

# 5.1 TCP 主动打开-客户 77

5.1.1 基本流程 77  
5.1.2 第一次握手：构造并发送 SYN 包 . . . 77  
5.1.3 第二次握手：接收 SYN+ACK 包 . . . . . 82  
5.1.4 第三次握手: 发送 ACK 包 . . . . . . 93

# 5.2 TCP 被动打开-服务器 94

5.2.1 基本流程 94

5.2.2 第一次握手：接受 SYN 段 . . . 95  
5.2.3 第二次握手：发送 $\mathrm { S Y N + A C K }$ 段 . . . . . 103  
5.2.4 第三次握手：接收 ACK 段 . . . . 107

# 6 TCP 拥塞控制 113

# 6.1 拥塞控制实现 113

6.1.1 拥塞控制状态机 113  
6.1.2 显式拥塞通知 (ECN) . . . 118   
6.1.3 拥塞控制状态的处理及转换 . . . 120

# 6.2 拥塞控制引擎 123

6.2.1 接口 . . . 124  
6.2.2 CUBIC 拥塞控制算法 . . . 127

# 7 TCP 释放连接 138

# 7.1 tcp_shutdown 138

# 7.2 主动关闭 139

7.2.1 第一次握手: 发送 FIN . . . . 139  
7.2.2 第二次握手: 接收 ACK . . . . 145  
7.2.3 第三次握手: 接受 FIN . . . . 149  
7.2.4 第四次握手: 发送 ACK . . . . 152  
7.2.5 同时关闭 . 154  
7.2.6 TIME_WAIT . . 155

# 7.3 被动关闭 157

7.3.1 基本流程 157  
7.3.2 第一次握手: 接收 FIN . . . . 157  
7.3.3 第二次握手: 发送 FIN 的 ACK . . . . . 159  
7.3.4 第三次握手: 发送 FIN . . 159  
7.3.5 第四次握手: 接收 FIN 的 ACK . . . . . 160

# 8 非核心代码分析 161

# 8.1 BSD Socket 层 161

8.1.1 msg_flag . . . 161   
8.1.2 数据报类型 162  
8.1.3 Sock CheckSum . . 163   
8.1.4 SK Stream 165   
8.1.5 sk_stream_wait_connect . . 165   
8.1.6 pskb_may_pull . . . 166

# 8.2 Inet 167

8.2.1 __inet_stream_connect . . 167   
8.2.2 inet_hash_connect && __inet_hash_connect . . . . 169   
8.2.3 inet_csk_reqsk_queue_add . . . 171

8.2.4 inet_twsk_put . . . 172

8.3 TCP 相关参数 172

8.3.1 TCP 标志宏 . . . 177  
8.3.2 函数宏 . . . 178

8.4 TCP CheckSum . . 178

8.4.1 tcp_checksum_complete 178   
8.4.2 tcp_v4_checksum_init . 179

8.5 TCP Initialize . 179

8.5.1 TCP Initialize Handle . . 179   
TCP Options . . 181   
8.6.1 TCP Options Handle . . . 182

8.7 TCP PAWS 188

8.7.1 TCP PAWS Flags 188   
8.7.2 tcp_paws_check . . . 189   
8.7.3 tcp_paws_reject 189

8.8 TCP TimeStamp . . 190

8.8.1 tcp_store_ts_recent . . 190   
8.8.2 tcp_replace_ts_recent 190

8.9 TCP ACK . 191

8.9.1 ACK Check . . . . 191   
8.9.2 ACK Update . . 195

8.10 TCP Window 196

8.10.1 Window Compute 196   
8.10.2 Window Update . . 197

8.11 TCP Urgent Data 199

8.11.1 相应标识 . . 199  
8.11.2 TCP Urgent Check . . . 199   
8.11.3 TCP Urgent Deal 201

8.12 Congestion Control . 202

8.12.1 COngestion Control Flag . . . 202   
8.12.2 Congestion Control Window . . . 202   
8.12.3 Congestion Control Window Undo . . . 204   
8.12.4 About Restransmit . 212

8.13 TCP Finish 212

8.13.1 Time Compute 212

8.14 TCP Close 213

8.14.1 TCP Close Flags . . . 213   
8.14.2 TCP State Machine . . . 213   
8.14.3 tcp_done 214

8.14.4 tcp_shutdown 214

# 9 附录: 基础知识 216

9.1 计算机底层知识 . 216

9.1.1 机器数 . . . 216  
9.2 GNU/LINUX 217   
9.2.1 错误处理 217  
9.2.2 调试函数 221

9.3 C 语言 . 221

9.3.1 结构体初始化 . . . 221  
9.3.2 位字段 . . 222

9.4 GCC 222

9.4.1 __attribute__ 222   
9.4.2 分支预测优化 . . 223

9.5 Sparse . . 224

9.5.1 __bitwise 224

9.6 操作系统 225

9.6.1 RCU . 225

9.7 CPU 225

存储系统 225

9.8.1 字节序 . . . 225   
9.8.2 缓存 Cache . . . . . 226

# CHAPTER 1

准备部分

# Contents

1.1 用户层 TCP . . . 1  
1.2 探寻 tcp_prot，地图 get~ . . . 1  
1.3 RFC 2

1.3.1 RFC793 : Transmission Control Protocol . . 2

1.3.1.1 TCP 状态图 . . . 2  
1.3.1.2 TCP 头部格式 . . . 4

1.3.2 RFC1323 : TCP Extensions for High Performance . . . . . 5

1.3.2.1 简介 . . 5  
1.3.2.2 窗口缩放 (Window Scale) . . . . 5   
1.3.2.3 PAWS(Protect Against Wrapped Sequence Numbers) 6

1.3.3 RFC1337 : TIME-WAIT Assassination Hazards in TCP . . . . 6

1.3.3.1 TIME-WAIT Assassination(TWA) 现象 . . . . . . . . 7  
1.3.3.2 TWA 的危险性及现有的解决方法 . . . . . . 8

1.3.4 RFC2018 : TCP Selective Acknowledgement Options . . . . . 8

1.3.4.1 基本介绍 . . 8  
1.3.4.2 选项字段 . . 8

1.3.5 RFC2525 : Known TCP Implementation Problems . . . . . . . 9

1.3.6 RFC3168 : The Addition of Explicit Congestion Notification (ECN) to IP 9

1.3.6.1 ECN 9   
1.3.6.2 TCP 中的 ECN . . 10

1.3.7 RFC6937 : Proportional Rate Reduction for TCP 10   
1.3.8 RFC7413 : TCP Fast Open(Draft) . . . 10

1.3.8.1 概要 . . 10   
1.3.8.2 Fast Open 选项格式 . . . . . 11

# 1.1 用户层 TCP

用户层的 TCP 编程模型大致如下，对于服务端，调用 listen 监听端口，之后接受客户端的请求，然后就可以收发数据了。结束时，关闭 socket。

```txt
1 // Server  
2 socket(...,SOCK_STREAM,0);  
3 bind(...,&server_address,...);  
4 listen(...);  
5 accept(...,&client_address,...);  
6 recv(...,&clientaddr,...);  
7 close(...); 
```

对于客户端，则调用 connect 连接服务端，之后便可以收发数据。最后关闭 socket。

```matlab
1 socket(.,SOCK_STREAM,0);   
2 connect();   
3 send(.,&server_address,...); 
```

那么根据我们的需求，我们着重照顾连接的建立、关闭和封包的收发过程。

# 1.2 探寻 tcp_prot，地图 get~

一般游戏的主角手中，都会有一张万能的地图。为了搞定 TCP，我们自然也是需要一张地图的，要不连该去找那个函数看都不知道。很有幸，在tcp_ipv4.c中，tcp_prot定义了tcp的各个接口。

tcp_prot的类型为struct proto，是这个结构体是为了抽象各种不同的协议的差异性而存在的。类似面向对象中所说的接口 (Interface) 的概念。这里，我们仅保留我们关系的部分。

1 struct proto tcp_prot = {   
2 .name $=$ "TCP",   
3 .owner $=$ THISMODULE,   
4 .close $=$ tcp_close,   
5 .connect $=$ tcp_v4_connect,   
6 .disconnect $=$ tcp_disconnect,   
7 .accept $=$ inlet_csk_accept,   
8 .destroy $=$ tcp_v4_destroy_sock,   
9 .shutdown $=$ tcp_shutdown,   
10 .setsockopt $=$ tcp_setsockopt,   
11 .getsockopt $=$ tcp_getsockopt,   
12 .recvmsg $=$ tcp_recvmsg,   
13 .sendmsg $=$ tcp_sendmsg,   
14 .sendpage $=$ tcp_sendpage,   
15 .backlog_rcv $=$ tcp_v4_do_rcv,   
16 .get_port $=$ inlet_csk_get_port,   
17 .twsk_prot $=$ &tcp时限wait_sockOps,   
18 .rsk_prot $=$ &tcp_request_sock OPS,   
19 };

通过名字，我大致筛选出来了这些函数，初步判断这些函数与实验所关心的功能相关。对着这张 ‘‘地图’’，就可以顺藤摸瓜，找出些路径了。

# 1.3 RFC

在分析 TCP 的过程中会遇到很多 RFC，在这里，我们将可能会碰到的 RFC 罗列出来，并进行一定的讨论，便于后面的分析。

# 1.3.1 RFC793 : Transmission Control Protocol

该 RFC 正是定义了 TCP 协议的那份 RFC。在该 RFC 中，可以查到 TCP 的很多细节，帮助后续的代码分析。

# 1.3.1.1 TCP 状态图

在 RFC793 中，给出了 TCP 协议的状态图，图中的 TCB 代表 TCP 控制块。原图如下所示：

![](images/62cad61cb1d1fab0001c38d7c82cccbd409ff4edb732d669358b188c7941687f.jpg)

![](images/b2078f67fc0b406b80273ae968c4c4ca4df1995e1291ccb9fef4680aa4ba00c7.jpg)

这张图对于后面的分析有很强的指导意义。

连接部分分为了主动连接和被动连接。主动连接是指客户端从 CLOSED 状态主动发出连接请求，进入 SYN-SENT 状态，之后收到服务端的 $\mathrm { S Y N + A C K }$ 包，进入 ESTAB状态（即连接建立状态），然后回复 ACK 包，完成三次握手。这一部分的代码我们将在5.1 中进行详细分析。被动连接是从 listen 状态开始，监听端口。随后收到 SYN 包，进入 SYN-RCVD 状态，同时发送 $\mathrm { S Y N + A C K }$ 包，最后，收到 ACK 后，进入 ESTAB状态，完成被动连接的三次握手过程。这一部分的详细讨论在5.2中完成。

连接终止的部分也被分为了两部分进行实现，主动终止和被动终止。主动终止是上图中从 ESTAB 状态主动终止连接，发送 FIN 包的过程。可以看到，主动终止又分为两种情况，一种是 FIN 发出后，收到了发来的 FIN（即通信双方同时主动关闭连接），此时转入 CLOSING 状态并发送 ACK 包。收到 ACK 后，进入 TIME WAIT 状态。另一种是收到了 ACK 包，转入 FINWAIT-2 状态，最后收到 FIN 后发送 ACK，完成四次握手，进入 TIME WAIT 状态。最后等数据发送完或者超时后，删除 TCB，进入 CLOSED状态。被动终止则是接收到 FIN 包后，发送了 ACK 包，进入 CLOSE WAIT 状态。之后，当这一端的数据也发送完成后，发送 FIN 包，进入 LAST-ACK 状态，接收到 ACK后，进入 CLOSED 状态。

# 1.3.1.2 TCP 头部格式

RFC793 中，对于 TCP 头部格式的描述摘录如下：

![](images/75cd13c288a8b7da0847886cce5fc6e81d91fcf9b92263bda2f1e1d7a7088c19.jpg)

23

Note that one tick mark represents one bit position.

这张图可以很方便地读出各个位占多长，它上面的标识是十进制的，很容易读。这里我们挑出我们比较关心的 Options 字段来解读。因为很多 TCP 的扩展都是通过新增选项来实现的。

选项总是在 TCP 头部的最后, 且长度是 8 位的整数倍。全部选项都被包括在校验和中。选项可从任何字节边界开始。选项的格式有 2 种情况:

1. 单独的一个字节，代表选项的类型例如：

```txt
1 End of Option List   
2 +-   
3 |0000000|   
4 +-   
5 Kind=0   
6   
7 No-Operation   
8 +-   
9 |0000001|   
10 +-   
11 Kind=1   
1 
```

2. 第一个字节代表选项的类型，紧跟着的一个字节代表选项的长度，后面跟着选项的数据。例如：

```txt
1 Maximum Segment Size   
2   
3 |00000010|00000100| max seg size 4   
4 Kind=2 Length=4 
```

# 1.3.2 RFC1323 : TCP Extensions for High Performance

# 1.3.2.1 简介

这个 RFC 主要是考虑高带宽高延迟网络下如何提升 TCP 的性能。该 RFC 定义了新的 TCP 选项，以实现窗口缩放 (window scaled) 和时间戳 (timestamp)。这里的时间戳可以用于实现两个机制：RTTM(Round Trip Time Measurement) 和 PAWS(ProtectAgainst Wrapped Sequences)。

在 RFC1323 中提出，在这类高带宽高延迟网络下，有三个主要的影响 TCP 性能的因素：

窗口尺寸限制 在 TCP 头部中，只有 16 位的一个域用于说明窗口大小。也就是说，窗口大小最大只能达到 $2 ^ { 1 6 } = 6 4 K$ 字节。解决这一问题的方案是增加一个窗口缩放选项，我们会在1.3.2.2中进一步讨论。

丢包后的恢复 丢包会导致 TCP 重新进入慢启动状态，导致数据的流水线断流。在引入了快重传和快恢复后，可以解决丢包率为一个窗口中丢一个包的情况下的问题。但是在引入了窗口缩放以后，由于窗口的扩大，丢包的概率也随之增加。很容易使 TCP 进入到慢启动状态，影响网络性能。为了解决这一问题，需要引入 SACK机制，但在这个 RFC 中，不讨论 SACK 相关的问题。

往返时间度量 RTO(Retransmission timeout) 是 TCP 性能的一个很基础的参数。在 RFC1323 中介绍了一种名为 RTTM 的机制，利用一个新的名为 Timestamps的选项来对时间进行进一步的统计。

# 1.3.2.2 窗口缩放 (Window Scale)

这一扩展将原有的 TCP 窗口扩展到 32 位。而根据 RFC793 中的定义，TCP 头部描述窗口大小的域仅有 16 位。为了将其扩展为 32 位，该扩展定义了一个新的选项用于表示缩放因子。这一选项仅会出现在 SYN 段，此后，所有通过该连接的通信，其窗口大小都会受到这一选项的影响。

该选项的格式为：

```txt
1 + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
```

kind域为 3，length域为 3，后面跟着 3 个字节的 shift.cnt，代表缩放因子。TCP的 Options 域的选项的格式在 ??中已有说明。这里采用的是第二种格式。

在启用了窗口缩放以后，TCP 头部中的接收窗口大小，就变为了真实的接收窗口大小右移 shift.cnt的值。RFC 中对于该选项的实现的建议是在传输控制块中，按照 32位整型来存储所有的窗口值，包括发送窗口、接受窗口和拥塞窗口。

作为接收方，每当收到一个段时（除 SYN 段外），通过将发来的窗口值左移来得到正确的值。

1 SND.WND $=$ SEG.WND<<Snd.Wind.Scale

作为发送方则每次在发包前，将发送窗口的值右移，然后再封装在封包中。

```txt
1 SEG.WND = RCV.WND >> Rcv.Wind.Scale 
```

# 1.3.2.3 PAWS(Protect Against Wrapped Sequence Numbers)

PAWS是一个用于防止旧的重复封包带来的问题的机制。它采用了TCP中的Times-tamps 选项。该选项的格式为：

```txt
+  
|Kind=8 | 10 | TS Value (TSval) | TS Echo Reply (TSecr) |  
1 | 1 | 4 | 4 
```

TSval(Timestamp Value) 包含了发送该 TCP 段时时钟的值。如果 ACK 位被设置了的话，TSecr(Timestamp Echo Reply) 会记录最近一次收到的报文中 TSval 的值。

PAWS 的算法流程为：

1. 如果当前到达的段 SEG 含有 Timestamps 选项，且 SEG.TSval $<$ TS.Recent 且该 TS.Recent 是有效的，那么则认为当前到达的分段是不可被接受的，需要丢弃掉。  
2. 如果段超过了窗口的范围，则丢弃它（与正常的 TCP 处理相同）  
3. 如果满足 SEG.SEQ⩽Last.ACK.sent（最后回复的 ACK 包中的时间戳），那么，将SEG 的时间戳记录为 TS.Recent。  
4. 如果 SEG 是正常按顺序到达的，那么正常地接收它。  
5. 其他情况下，将该段视作正常的在窗口中，但顺序不正确的 TCP 段对待。

# 1.3.3 RFC1337 : TIME-WAIT Assassination Hazards in TCP

在 TCP 连接中，存在TIME_WAIT这样一个阶段。该阶段会等待 2MSL 的时间，以使得属于当前连接的所有的包都消失掉。这样可以保证再次用相同端口建立连接时，不会有属于上一个连接的滞留在网络中的包对连接产生干扰。

# 1.3.3.1 TIME-WAIT Assassination(TWA) 现象

TCP 的连接是由四元组（源 IP 地址，源端口，目的 IP 地址，目的端口）唯一决定的。但是，存在这样一种情况，当一个 TCP 连接关闭，随后，客户端又使用相同的IP 和端口号向服务端发起连接，即产生了和之前的连接一模一样的四元组。此时，如果网络中还存在上一个连接遗留下来的包，就会出现各类的问题。对于这一问题，RFC793中定义了相关的机制进行应对。

1. 三次握手时会拒绝旧的 SYN 段，以避免重复建立连接。  
2. 通过判断序列号可以有效地拒绝旧的或者重复的段被错误地接受。  
3. 通过选择合适的 ISN(Initial Sequence Number) 可以避免旧的连接和新的连接的段的序列号空间发生重叠。  
4. TIME-WAIT 状态会等待足够长的时间，让旧的滞留在网络上的段因超过其生命周期而消失。  
5. 在系统崩溃后，在系统启动时的静默时间可以使旧的段在连接开始前消失。

然而，其中的TIME_WAIT状态的相关机制却是不可靠的。网络中滞留的段有可能会使得TIME_WAIT状态被意外结束。这一现象即是 TIME-WAIT Assassination 现象。RFC1337中给出了一个实例：

```txt
TCP A TCP B  
1. ESTABLISHED ESTABLISHED  
(Close)  
2. FIN-WAIT-1 --<SEQ=100><ACK=300><CTL=FIN,ACK> -->CLOSE-WAIT 
```

```txt
7   
8   
9   
10   
11   
12   
13   
14   
15   
16   
17   
18   
19   
20   
21   
22 
```

可以看到，TCP A 收到了一个遗留的 ACK 包，之后响应了这个 ACK。TCP B 收到这个莫名其妙的响应后，会发出 RST 报，因为它认为发生了错误。收到 RST 包后，TCPA 的 TIME-WAIT 状态被终止了。然而，此时还没有到 2MSL 的时间。

# 1.3.3.2 TWA 的危险性及现有的解决方法

RFC1337 中列举了三种 TWA 现象带来的危险。

1. 滞留在网络上的旧的数据段可能被错误地接受。  
2. 新的连接可能陷入到不同步的状态中。如接收到一个旧的 ACK 包等情况。  
3. 新的连接可能被滞留在网络中的旧的 FIN 包关闭。或者是 SYN-SENT 状态下出现了意料之外的 ACK 包等。都可能导致新的连接被终止。

而解决 TWA 问题的方法较为简单，直接在 TIME-WAIT 阶段忽略掉所有的 RST段即可。在7.2.6中的代码中可以看到，Linux 正是采用了这种方法来解决该问题。

# 1.3.4 RFC2018 : TCP Selective Acknowledgement Options

# 1.3.4.1 基本介绍

该 RFC 的引入只要是为了提供一种解决大量丢包的问题的方法。通过选择性确认，接收方可以告知发送方哪些段已经收到了。因此，发送方就只需要重传那些真正看起来丢失的段了。

# 1.3.4.2 选项字段

选择性确认使用了两个 TCP 的选项字段，其中一个是是否支持 SACK，即 SACK-Permitted, 这个只能在连接建立的过程中通过 SYN 段通告对方自己是否可以支持，另外一个是在连接已经建立的期间通过相应的真正的确认字段来传达被确认的字段。

Sack-Permitted Option 如下所示:

```txt
+---+---+   
| Kind=4 | Length=2|   
+---+---+
```

Sack Option Format 如下所示:

```txt
1 Kind:5   
2 Length:Variable   
3 + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -   
4 |Kind=5 |Length|   
5 + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + 
```

在这个选项中，接收方通告发送方那些没有连续收到的数据块，当接收方收到了那些在空隙中的块之后，就会将左边沿前移，同时将相应的确认序列号填入到 TCP 首部。

其中 Left Edge of Block 和 Right Edge of Block 均为为 32 位无符号正数，字节序为网络类型，前者为相应块的起始序列号，后者为这个块的最后一个字节序号加 1.

由于 TCP 的首部的选项部分最多有 40 字节，而 SACK 占据的为 $8 ^ { * } \mathrm { n } \mathrm { + } 2$ , 故而，一次通告，最多确认 4 大块。而且 SACK 经常和用于 RTTM 的时间戳一起使用，而时间戳占据了 10 各字节，故而，依次通过也就最多确认 3 大块了。

值得一说的是，这个 SACK 选项是劝告型的，也就是说，他只是只是提醒数据的发送方，我已经接收到了相应的段，但是数据的接收方是允许将 SACK 选项确认的段删除的。这个也就是常说的接收方违约，这个问题也确实引起了很多讨论。

关于 SACK 的更多的叙述，后面再细细补充。remain to do。

# 1.3.5 RFC2525 : Known TCP Implementation Problems

# 1.3.6 RFC3168 : The Addition of Explicit Congestion Notification (ECN) to IP

该 RFC 为 TCP/IP 网络引入了一种新的用于处理网络拥塞问题的方法。在以往的TCP 中，人们假定网络是一个不透明的黑盒。而发送方通过丢包的情况来推测路由器处发生了拥塞。这种拥塞控制方法对于交互式的应用效果并不理想，且可能对吞吐量造成负面影响。因为此时网络可能已经计入到了拥塞状态（路由器的缓存满了，因此发生了丢包）。为了解决这一问题，人们已经引入了主动队列管理算法（AQM）。允许路由器在拥塞发生的早期，即队列快要满了的时候，就通过主动丢包的方式，告知发送端要减慢发送速率。然而，主动丢包会引起包的重传，这会降低网络的性能。因此，RFC3168 引入了 ECN（显式拥塞通知）机制。

# 1.3.6.1 ECN

在引入 ECN 机制后，AQM 通知发送端的方式不再局限于主动丢包，而是可以通过 IP 头部的 Congestion Experienced(CE) 来通知支持 ECN 机制的发送端发生了拥塞。这样，接收端可以不必通过丢包来实现 AQM，减少了发送端因丢包而产生的性能下降。

ECN 在 IP 包中的域如下所示：

```txt
1   
2   
3   
4 ECT CE [Obsolete] RFC 2481 names for the ECN bits.   
5 0 0 Not-ECT   
6 0 1 ECT(1)   
7 1 0 ECT(0)   
8 1 1 CE 
```

Not-ECT 代表该包没有采用 ECN 机制。ECT(1) 和 ECT(0) 均代表该设备支持ECN 机制。CE 用于路由器向发送端表明网络是否正在经历拥塞状态。

# 1.3.6.2 TCP 中的 ECN

在 TCP 协议中，利用 TCP 头部的保留位为 ECN 提供了支持。这里规定了两个新的位：ECE 和 CWR。ECE 用于在三次握手中协商手否启用 ECN 功能。CWR 用于让接收端决定合适可以停止设置 ECE 标志。增加 ECN 支持后的 TCP 头部示意图如下：

```solidity
0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  
+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+--+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---
```

对于一个 TCP 连接，一个典型的基于 ECN 的序列如下：

1. 发送方在发送的包中设置 ECT 位，表明发送端支持 ECN。  
2. 在该包经历一个支持 ECN 的路由器时，该路由器发现了拥塞，准备主动丢包。此时，它发现正要丢弃的包支持 ECN。它会放弃丢包，而是设置 IP 头部的 CE 位，表明经历了拥塞。  
3. 接收方收到设置了 CE 位的包后，在回复的 ACK 中，设置 ECE 标记。  
4. 发送端收到了一个设定了 ECE 标记的 ACK 包，进入和发生丢包一样的拥塞控制状态。  
5. 发送端在下一个要发送的包中设定 CWR 位，以通知接收方它已经对 ECE 位进行了相应处理。

通过这种方式，就可以在经历网络拥塞时减少丢包的情况，并通知客户端网络的拥塞状态。

# 1.3.7 RFC6937 : Proportional Rate Reduction for TCP

# 1.3.8 RFC7413 : TCP Fast Open(Draft)

RFC7413 目前还处于草案状态，它引入了一种试验性的特性：允许在三次握手阶段的 SYN 和 SYN-ACK 包中携带数据。相较于标准的三次握手，引入 TFO(TCP FastOpen) 机制可以节省一个 RTT 的时间。该 RFC 由 Google 提交，并在 Linux 和 Chrome中实现了对该功能的支持。此后，越来越多的软件也支持了该功能。

# 1.3.8.1 概要

TFO 中，最核心的一个部分是 Fast Open Cookie。这个 Cookie 是由服务器生成的，在初次进行常规的 TCP 连接时，由客户端向服务器请求，之后，则可利用这个 Cookie在后续的 TCP 连接中在三次握手阶段交换数据。

请求 Fast Open Cookie 的过程为：

1. 客户端发送一个带有 Fast Open 选项且 cookie 域为空的 SYN 包  
2. 服务器生成一个 cookie，通过 SYN-ACK 包回复给客户端  
3. 客户端将该 cookie 缓存，并用于后续的 TCP Fast Open 连接。

建立 Fast Open 连接的过程如下：

1. 客户端发送一个带有数据的 SYN 包，同时在 Fast Open 选项里带上 cookie。  
2. 服务端验证 cookie 的有效性，如果有效，则回复 SYN-ACK，并将数据发给应用程序。  
3. 客户端发送 ACK 请求，确认服务器发来的 SYN-ACK 包及其中的数据。  
4. 至此，三次握手已完成，后续过程与普通 TCP 连接一致。

# 1.3.8.2 Fast Open 选项格式

该选项的格式如下：

![](images/2a959c54062c93887448188d0f6f9faecd36d331cb2402fd767fb7256da2bafc.jpg)

这里注意，虽然图示中 Cookie 按照 32 位对齐了，但是这并不是必须的。

# CHAPTER 2

# 网络子系统相关核心数据结构

# Contents

2.1 网络子系统数据结构架构 . . . 12  
2.2 sock 底层数据结构 . . . 12

2.2.1 sock_common 12   
2.2.2 sock 14   
2.2.3 request_sock 18   
2.2.4 sk_buff . 19   
2.2.5 msghdr 23

2.3 inet 层相关数据结构 . . . 23

2.3.1 ip_options . . 23   
2.3.2 inet_request_sock . . 24   
2.3.3 inet_connection_sock_af_ops 24   
2.3.4 inet_connect_sock . . 25   
2.3.5 inet_timewait_sock 26   
2.3.6 sockaddr & sockaddr_in . . 27   
2.3.7 ip_options 28

2.4 路由相关数据结构 29

2.4.1 dst_entry 29   
2.4.2 rtable 30   
2.4.3 flowi 31

2.5 TCP 层相关数据结构 . . . 31

2.5.1 tcphdr . . 31   
2.5.2 tcp_options_received . . 32   
2.5.3 tcp_sacktag_state 33   
2.5.4 tcp_sock . . 33

2.5.5 tcp_fastopen_cookie . . . . 38   
2.5.6 tcp_fastopen_request . . . 38   
2.5.7 tcp_request_sock . . . 38   
2.5.8 tcp_skb_cb . . . 39

2.5.8.1 TCP_SKB_CB 39   
2.5.8.2 tcp_skb_cb 结构体 . . 39  
2.5.8.3 tcp_out_options . . 40

# 2.1 网络子系统数据结构架构

# 2.2 sock 底层数据结构

2.2.1 sock_common   
/* Location: include/net/sock.h Description: minimal network layer representation of sockets This is the minimal network layer representation of sockets, the header for struct sock and struct inet_timeout SOCK. Member: \* @skc_daddr: Foreign IPv4 addr \* @skc_rcu_saddr: Bound local IPv4 addr \* @skc_hash: hash value used with various protocol lookup tables \* @skc_u16hashes: two u16 hash values used by UDP lookup tables \* @skc_dport: placeholder forINET_dport/tuw_dport \* @skc_num: placeholder forinet_num/tuw_num \* @skc_family: network address family \* @skc_state: Connection state \* @skc_reuse: %SO_REUSEADDR setting \* @skc_reuseport: %SO_REUSEPORT setting \* @skc_bound_dev_if: bound device index if $! = 0$ \* @skc_bind_node: bind hash linkage for various protocol lookup tables \* @skc_portaddr_node: second hash linkage for UDP/UDP-Lite protocol \* @skcprot: protocol handlers inside a network family \* @skc_net: reference to the network namespace of this socket \* @skc_node: main hash linkage for various protocol lookup tables \* @skc_nulls_node: main hash linkage for TCP/UDP/UDP-Lite protocol \* @skc_tx_queue Mapping: tx queue number for this connection \* @skc_flags: place holder for sk_flags \* %SO_LINGER (l_onoff), %SO_BROADCAST, %SO_KEEPALIVE, \* %SO_OOBINLINE settings, %SO_TIMESTAMPING settings \* @skc_incoming_cpu: record/match cpu processing incoming packets \* @skc_refcnt: reference count   
struct sock_common { /\* skc_daddr and skc_rcu_saddr must be grouped on a 8 bytes aligned

\*address on 64bit arches : cf INET_MATCH()   
\*/   
union { __addrpair skc_addrpair; struct { __be32 skc_daddr; __be32 skc_rcv_saddr; }; } ; union { unsigned int skc_hash; __u16 skc_u16hashes[2]; }; /* skc_dport && skc_num must be grouped as well */ union { __portpair skc_portpair; struct { __be16 skc_dport; __u16 skc_num; }; unsigned short skc_family; volatile unsigned char skc_state; unsigned char skc_reuse:4; unsigned char skc_reuseport:1; unsigned char skc_ipv6only:1; unsigned char skc_net_refcnt:1; int skc_bound_dev_if; union { struct hlist_node skc_bind_node; struct hlist_nulls_node skc_portaddr_node; }; struct proto \*skcprot; possible_net_t skc_net;   
#if IS_ENABLED (CONFIG_IPV6)   
struct in6_addr skc_v6_daddr;   
struct in6_addr skc_v6_rcv_saddr;   
#endif   
atomic64_t skc_cookie;   
/\* following fields are padding to force \* offset(struct sock, sk_refcnt) $= =$ 128 on 64bit arches \* assuming IPV6 is enabled. We use this padding differently \* for different kind of 'sockets' \*/   
union { unsigned long skc_flags; struct sock \*skc Listener; /* request SOCK */ struct inlet_timeout_death_row \*skc_tw_DR; /* inlet_timeout SOCK */ }; /\* \* fields between dontcopy_begin/dontcopy_end \* are not copied in sock_copy() \*/ /\* private: \*/

2.2.2 sock   
```txt
/*   
Location: include/net/sock.h   
Description: sock结构是比较通用的网络层描述块，构成传输控制块的基础，与具体的协议族无关。 它描述了各协议族的公共信息，因此不能直接作为传输层控制块来使用。不同协议族的 传输层在使用该结构的时候都会对其进行拓展，来适合各自的传输特性。例如，inet SOCK 结构由 sock结构及其它特性组成，构成了IPV4协议族传输控制块的基础。   
Member: \*struct sock - network layer representation of sockets \* @_sk_common: shared layout with inlet_timewait_sock \* @sk_shutdown: SEND_SHUTDOWN 或者 RCV_SHUTDOWN 的掩码 \* @sk_userlocks: %SO_SNDBUF and %SO_RCVBUF settings \* @sk_lock: synchronizer \* @sk_rcvbuf：接受缓冲区的大小（单位为字节） \* @sk_wq: sock wait queue and async head \* @sk_rx.dst: receive input route used by early demux \* @sk.dst_cache: destination cache \* @sk_policy: flow policy \* @sk_receive_queue: incoming packets \* @sk_umem_alloc: transmit queue bytes committed \* @sk_write_queue: Packet sending queue \* @sk_omem_alloc:"o" is "option" or "other" \* @sk_umem_queue:d persistent queue size \* @sk_forward_alloc: space allocated forward \* @sk_napi_id: id of the last napi context to receive data for sk \* @sk ll_usec: uses to busypoll when there is no data \* @sk_allocation: allocation mode
```

* @sk_pacing_rate: Pacing rate (if supported by transport/packet scheduler)

* @sk_max_pacing_rate: Maximum pacing rate (%SO_MAX_PACING_RATE)

* @sk_sndbuf: size of send buffer in bytes

* @sk_no_check_tx: %SO_NO_CHECK setting, set checksum in TX packets

* @sk_no_check_rx: allow zero checksum in RX packets

* @sk_route_caps: route capabilities (e.g. %NETIF_F_TSO)

* @sk_route_nocaps: forbidden route capabilities (e.g NETIF_F_GSO_MASK)

* @sk_gso_type: GSO type (e.g. %SKB_GSO_TCPV4)

* @sk_gso_max_size: Maximum GSO segment size to build

* @sk_gso_max_segs: Maximum number of GSO segments

* @sk_lingertime: %SO_LINGER l_linger setting

* @sk_backlog: always used with the per-socket spinlock held

* @sk_callback_lock: used with the callbacks in the end of this struct

* @sk_error_queue: rarely used

* @sk_prot_creator: sk_prot of original sock creator (see ipv6_setsockopt,

* IPV6_ADDRFORM for instance)

* @sk_err: last error

* @sk_err_soft: errors that don't cause failure but are the cause of a

persistent failure not just 'timed out'

* @sk_drops: raw/udp drops counter

* @sk_ack_backlog: current listen backlog

* @sk_max_ack_backlog: listen backlog set in listen()

* @sk_priority: %SO_PRIORITY setting

* @sk_cgrp_prioidx: socket group's priority map index

* @sk_type: socket type (%SOCK_STREAM, etc)

* @sk_protocol: which protocol this socket belongs in this network family

* @sk_peer_pid: &struct pid for this socket's peer

* @sk_peer_cred: %SO_PEERCRED setting

* @sk_rcvlowat: %SO_RCVLOWAT setting

* @sk_rcvtimeo: %SO_RCVTIMEO setting

$^ *$ @sk_sndtimeo: %SO_SNDTIMEO setting

$^ *$ @sk_txhash: computed flow hash for use on transmit

* @sk_filter: socket filtering instructions

$^ *$ @sk_timer: sock cleanup timer

$^ *$ @sk_stamp: time stamp of last packet received

$^ *$ @sk_tsflags: SO_TIMESTAMPING socket options

$^ *$ @sk_tskey: counter to disambiguate concurrent tstamp requests

$^ *$ @sk_socket: Identd and reporting IO signals

$^ *$ @sk_user_data: RPC layer private data

$^ *$ @sk_frag: cached page frag

$^ *$ @sk_peek_off: current peek_offset value

$^ *$ @sk_send_head: 发送队列的头指针

$^ *$ @sk_security: used by security modules

$^ *$ @sk_mark: generic packet mark

$^ *$ @sk_classid: this socket's cgroup classid

$^ *$ @sk_cgrp: this socket's cgroup-specific proto data

$^ *$ @sk_write_pending: a write to stream socket waits to start

$^ *$ @sk_state_change: callback to indicate change in the state of the sock

$^ *$ @sk_data_ready: callback to indicate there is data to be processed

$^ *$ @sk_write_space: callback to indicate there is bf sending space available

$^ *$ @sk_error_report: callback to indicate errors (e.g. %MSG_ERRQUEUE)

$^ *$ @sk_backlog_rcv: callback to process the backlog

$^ *$ @sk_destruct: called at sock freeing time, i.e. when all refcnt == 0

struct sock { /*

$^ *$ Now struct inet_timewait_sock also uses sock_common, so please just $^ *$ don't add nothing before this first member (__sk_common) --acme */

```c
struct sock_common __sk_common;
#define sk_node __sk_common.skc_node
#define sk_nulls_node __sk_common.skc_nulls_node
#define sk_refcnt __sk_common.skc_refcnt
#define sk_tx_queue_mapping __sk_common.skc_tx_queue Mapping
#define sk_dontcopy_begin __sk_common.skc_dontcopy_begin
#define sk_dontcopy_end __sk_common.skc_dontcopy_end
#define sk_hash __sk_common.skc_hash
#define sk_portpair __sk_common.skc_portpair
#define sk_num __sk_common.skc_num
#define sk_dport __sk_common.skc_dport
#define sk_addpair __sk_common.skc_addpair
#define sk_daddr __sk_common.skc_daddr
#define sk_rcu_saddr __sk_common.skc_rcu_saddr
#define sk_family __sk_common.skc_family
#define sk_state __sk_common.skc_state
#define sk_reuse __sk_common.skc_reuse
#define sk_reuseport __sk_common.skc_reuseport
#define sk_ipu6only __sk_common.skc_ipu6only
#define sk_net_refcnt __sk_common.skc_net_refcnt
#define sk_bound_dev_if __sk_common.skc_bound_dev_if
#define sk_bind_node __sk_common.skc_bind_node
#define sk_prot __sk_common.skc_prot
#define sk_net __sk_common.skc_net
#define sk_v6_daddr __sk_common.skc_v6_daddr
#define sk_v6_rcu_saddr __sk_common.skc_v6_rcu_saddr
#define sk_cookie __sk_common.skc_cookie
#define sk_incoming_cpu __sk_common.skc_incoming_cpu
#define sk_flags __sk_common.skc_flags
#define sk_rxhash __sk_common.skc_rxhash
socket_lock_t sk_lock;
struct sk BUFF_head sk_receive_queue;
/* The backlog queue is special, it is always used with the per-socket spinlock held and requires low latency */
* access. Therefore we special case it's implementation.
* Note: rmem_alloc is in this structure to fill a hole on 64bit arches, not because its logically part of backlog.
*/
struct {
    atomic_t rmem_alloc;
    int len;
    struct sk_buffer *head;
    struct sk_buffer *tail;
} skbackslog;
#define sk_rmem_alloc skbackslog.rmem_alloc
int sk_forward_alloc;
__u32 sk_txhash;
#ifdef CONFIG_NET_RXBUSY POLL
unsigned int sk_napi_id;
unsigned int sk_xxUseC;
#endif
atomic_t skdrops;
int sk_rcvbuf; 
```

```txt
struct sk_filter __rcu *sk_filter;
union {
    struct socket_wq __rcu *sk_wq;
    struct socket_wq *sk_wq_raw;
};
#ifdef CONFIG_XFRM
struct xfrm_policy __rcu *sk_policy[2];
#define
    struct dst_entry *sk_rx.dst;
    struct dst_entry __rcu *sk.dst_cache;
/* Note: 32bit hole on 64bit arches */
atomic_t sk_wmem_alloc;
atomic_t sk_omem_alloc;
int sk_sndbuf;
struct sk BUFF_head sk_write_queue;
kmemcheck_bitfield_begin(flags);
unsigned int sk_shutdown : 2,
        sk_no_check_tx : 1,
        sk_no_check_rx : 1,
        sk_userlocks : 4,
        sk_protocol : 8,
        sk_type : 16;
#	define SK_PROTOCOL_MAX U8_MAX
kmemcheck_bitfield_end(flags);
int sk_wmem_QUEued;
gfp_t sk_allocation;
u32 sk_pacing_rate; /* bytes per second */
u32 sk_max_pacing_rate;
netdev_features_t sk-routeCaps;
netdev_features_t sk-routenocaps;
int sk_gso_type;
unsigned int sk_gso_max_size;
u16 sk_gso_max_segs;
int sk_rcvlowat;
unsigned long sk_lingertime;
struct sk BUFF_head sk_error_queue;
struct proto *sk_prot Creator;
rwlock_t sk_callback_lock;
int sk_err,
        sk_errsoft;
u32 sk_ACK.Backlog;
u32 sk_max_ACK_backlog;
__u32 sk_priority;
#ifdef IS_ENABLED (CONFIG_CGROUP_NET_PRIO)
__u32 sk_cgrp_prioidx;
#define
    struct pid *sk_peer.pid;
const struct cred *sk_peercred;
long sk_rcvtimeo;
long sk SNDtimeo;
struct timer_list sk_timer;
ktime_t sk_stamp;
u16 sk tsflags;
u32 sk tskey;
struct socket *sk_sock;
void *sk_user_data;
struct pagefrag sk_frag;
struct sk_buffer *sk_send_head; 
```

```c
__s32 skpeek_off; int sk_write_pending; #ifdef CONFIGSECURITY void \*sk_security; #endif __u32 sk_mark; #ifdef CONFIG_CGROUP_NET_CLASSID u32 sk_classid; #endif struct cg.proto \*sk_cgrp; void (*sk_state_change)(struct sock \*sk); void (*sk_data_ready)(struct sock \*sk); void (*sk_write_space)(struct sock \*sk); void (*sk_error_report)(struct sock \*sk); int (*skbackslog_rcv)(struct sock \*sk, struct sk BUFF \*skb); void (*sk-destruct)(struct sock \*sk); }; 
```

2.2.3 request_sock   
```c
/* Location: /include/net/request SOCK.h   
Description: struct request SOCK - mini sock to represent a connection request 该结构用于表示一个简单的TCP连接请求。   
\*/ struct request SOCK { struct sock_common __req_common; #define rsk_refcnt __req_common.skc_refcnt #define rsk_hash __req_common.skc_hash #define rsk Listener __req_common.skc Listener #define rsk_window_clamp __req_common.skc_window_clamp #define rsk_rcu_wnd __req_common.skc_rcu_wnd struct request SOCK \*dl_next; u16 mss; u8 num_retrans; /\*number of retransmits \*/ u8 cookie ts:1; /\*syncookie: encode tcpopts in timestamp \*/ u8 num_timeout:7; /\*number of timeouts \*/ u32 tsrecent; struct timer_list rsk_timer; const struct request SOCKOps \*rsk ops; struct sock \*sk; u32 \*saved_syn; u32 secid; u32 peer(secid; }； 
```

# 2.2.4 sk_buff

struct sk_buff这一结构体在各层协议中都会被用到。该结构体存储了网络数据报的所有信息。包括各层的头部以及 payload，以及必要的各层实现相关的信息。

该结构体的定义较长，需要一点一点分析。结构体的开头为

```c
union { struct { /\* These two members must be first. \*/ struct sk_buffer \*next; struct sk_buffer \*prev; union { ktime_t tstamp; struct skb_mstamp skb_mstamp; }; }； struct rb_node rbnode; /* used in netem and tcp stack */ }; 
```

可以看到，sk_buff可以被组织成两种数据结构：双向链表和红黑树。且一个sk_buff不是在双向链表中，就是在红黑树中，因此，采用了 union 来节约空间。next 和 prev 两个域是用于双向链表的结构体，而 rbnode 是红黑树相关的结构。

包的到达/发送时间存放在union {ktime_t tstamp;struct skb_mstamp skb_mstamp;};中，之所以这里有两种不同的时间戳类型，是因为有时候调用ktime_get()的成本太高。因此，内核开发者希望能够在 TCP 协议栈中实现一个轻量级的微秒级的时间戳。struct skb_mstamp正是结合了local_clock()和 jiffies二者，而实现的一个轻量级的工具。当然，根据内核邮件列表中的说法，并不是任何时候都可以用该工具替换调ktime_get()的。因此，在struct sk_buff结构体中，采用union的方式同时保留了这二者。

在定义完数据结构相关的一些部分后，又定义了如下的结构体

```c
/* 拥有该 sk_buffer 的套接字的指针 */
struct sock *sk;
/* 与该包关联的网络设备 */
struct net_device *dev;
/* 控制用的缓冲区，用于存放各层的私有数据 */
char cb[48] __aligned(8);
/* 存放了目的地项的引用计数 */
unsigned long _skb_refdst;
/* 析构函数 */
void (*destructor)(struct sk_buffer *skb);
#ifndef CONFIG_XFRM
/* xfrm 加密通道 */
struct sec_path *sp;
#endif
#ifndef IS_ENABLED(CONFIG bridges_NETFILTER)
/* 保存和 bridge 相关的信息 */
struct nf_bridge_info *nf_bridge;
#endif
```

其中的char cb[48]比较有意思，各层都使用这个 buffer 来存放自己私有的变量。这里值得注意的是，如果想要跨层传递数据，则需要使用 skb_clone()。XFRM 则是Linux 在 2.6 版本中引入的一个安全方面的扩展。

之后，又定义了一些长度相关的字段。len代表 buffer 中的数据报总长度（含各协议的头部），以及分片长度。而data_len代表分片中的数据的长度。mac_len是 MAC 层头部的长度。hdr_len是一个克隆出来的可写的头部的长度。

```txt
unsigned int len, data_len;  
__u16 mac_len, hdr_len; 
```

kmemcheck 是内核中的一套内存检测工具。kmemcheck_bitfield_begin 和 kmem-check_bitfield_end 可以用于说明一段内容的起始和终止位置。其代码定义如下：

```c
1 #define kmemcheck_bitfield_begin(name) \
2 int name##_begin[0];
3
4 #define kmemcheck_bitfield_end(name) \
5 int name##_end[0]; 
```

通过定义，我们不难看出，这两个宏是用于在代码中产生两个对应于位域的起始地址和终止地址的符号的。当然，这两个宏是为 kmemcheck 的功能服务的。如果没有开启该功能的话，这两个宏的定义为空，也即不会产生任何作用。

```c
/* Following fields are _not_ copied in _copy_skb_header()  
* Note that queueMapping is here mostly to fill a hole.  
*/  
kmemcheck_bitfield_begin(flags1);  
__u16 queue Mapping; /* 对于多队列设备的队列关系映射 */  
__u8 cloned:1, /* 是否被克隆 */  
nohdr:1, /* 只引用了负载 */  
fclone:2, /* skbuff 克隆的情况 */  
peeked:1, /* peeked 表明该包已经被统计过了，无需再次统计 */  
headfrag:1,  
xmit_more:1; /* 在队列中有更多的 SKB 在等待 */  
/* one bit hole */  
kmemcheck_bitfield_end(flags1);
```

在这段定义中，内核将一系列的标志位命名为了 flags1，利用那两个函数可以在生成的代码中插入 flags1_begin和flags1_end两个符号。这样，当有需要的时候，可以通过这两个符号找到这一段的起始地址和结束地址。

紧接着是一个包的头部，这一部分再次使用了类似上面的方法，用了两个零长度的数组 headers_start和headers_end来标明头部的起始和终止地址。

```c
/* 在 __copy_skb_header() 中，只需使用一个 memcpy() 即可将 headers_start/end
* 之间的部分克隆一份。
*/
/* private: */
__u32         headers_start[0];
/* public: */
/* if you move pkt_type around you also must adapt those constants */
#ifndef __BIG-ENDIAN_BITFIELD
#define PKT_TYPE_MAX (7 << 5)
#else
#define PKT_TYPE_MAX 7
#endif
#define PKT_TYPE_OFFSET()
{
    __u8        __pkt_type_offset[0];
}
```

```lisp
__u8 pkt_type:3;
__u8 pfmemalloc:1;
/* 是否允许本地分片 (local fragmentation) */
__u8 ignore_df:1;
/* 表明该 skb 和连接的关系 */
__u8 nfctinfo:3;
/* netfilter 包追踪标记 */
__u8 nf_trace:1;
/* 驱动（硬件）给出来的 checksum */
__u8 ip_summed:2;
/* 允许该 socket 到队列的对应关系发生变更 */
__u8 ooookay:1;
/* 表明哈希值字段 hash 是一个典型的 4 元组的通过传输端口的哈希 */
__u8 l4_hash:1;
/* 表明哈希值字段 hash 是通过软件栈计算出来的 */
__u8 sw_hash:1;
/* 表明 wifi_aced 是否被设置了 */
__u8 wifi_aced_valid:1;
/* 表明帧是否在 wifi 上被确认了 */
__u8 wifi_aced:1;
/* 请求 NIC 将最后的 4 个字节作为以太网 FCS 来对待 */
__u8 no_fcs:1;
/* Indicates the inner headers are valid in the skbuff. */
__u8 encapsulation:1;
__u8 encaps hdr_csum:1;
__u8 csum_valid:1;
__u8 csum_COMPLETE_sw:1;
__u8 csum_level:2;
__u8 csumBAD:1;
#ifdef CONFIG_IPV6_NDISC_NODETYPE
__u8 ndisc_nodetype:2; /* 路由类型（来自链路层） */
#endif
/* 标明该 skbuff 是否被 ipvs 拥有 */
__u8 ipvs_property:1;
__u8 inner_protocol_type:1;
__u8 remcsum_offload:1;
/* 3 or 5 bit hole */
#ifdef CONFIG_NET_SCHED
__u16 tc_index; /* traffic control index */
#ifdef CONFIG_NET_CLS_ACT
__u16 tc verd; /* traffic control verdict */
#endif
union {
    __wsum csum; /* 校验码 */
struct {
        /* 从 skb->head 开始到应当计算校验码的起始位置的偏移 */
__u16 csum_start;
        /* 从 csum_start 开始到存储校验码的位置的偏移 */
__u16 csum_offset;
};
```

```c
__be16 vlan.proto; /* vlan 包装协议 */
__u16 vlan_tci; /* vlan tag 控制信息 */
#if defined (CONFIG_NET_RXBUSY Poll) // defined (CONFIG_XPS)
union {
unsigned int napi_id; /* 表明该 skb 来源的 NAPI 结构体的 id */
unsigned int sender_cpu;
};
endif
union {
#define CONFIG_NETWORK_SECMARK
__u32 secmark; /* 安全标记 */
#define CONFIG_NET_SWITCHDEV
__u32 offload_fwd_mark; /* fuding offload mark */
};
union {
__u32 mark; /* 通用的包的标记位 */
__u32 reservedTAILROOM;
};
union {
__be16 inner_protocol; /* 协议（封装好的） */
__u8 inner_ipproto;
};
/* 已封装的内部传输层头部 */
__u16 inner transporting_header;
/* 已封装的内部网络层头部 */
__u16 inner_network_header;
/* 已封装的内部链路层头部 */
__u16 inner_mac_header;
/* 驱动（硬件）给出的包的协议类型 */
__be16 protocol;
/* 传输层头部 */
__u16 transport_header;
/* 网络层头部 */
__u16 network_header;
/* 数据链路层头部 */
__u16 mac_header;
/* private: */
__u32 headers_end[0];
```

最后是一组是管理相关的字段。其中，head和end 代表被分配的内存的起始位置和终止位置。而data和tail 则是实际数据的起始和终止位置。

```txt
/\* These elements must be at the end, see alloc_skb() for details. \*/ sk_buffer_t tail; sk_buffer_t end; unsigned char *head, \*data; unsigned int truesize; atomic_t users; 
```

users是引用计数，所以是个原子的。truesize是数据报的真实大小。

2.2.5 msghdr  
```c
/* Location: include/linux/socket.h */
struct msghdr {
    void *msg_name; /*指向 socket地址结构体的指针*/
    int msg_namelen; /*socket地址结构体的大小*/
    structiov_iter msg_iter; /*数据*/
    void *msg_control; /*辅助数据*/
    __kernel_size_t msg_controllen; /*辅助数据缓冲区大小*/
    unsigned int msg_flags; /*收到的消息所带的标记*/
    struct kiocb *msg_iocb; /*指向 iocb的指针（用于异步请求）*/
};
```

# 2.3 inet 层相关数据结构

2.3.1 ip_options   
```c
/*   
Location:   
Description: @faddr - Saved first hop address @nexthop - Saved nexthop address in LSRR and SSRR @is_strictroute - Strict source route @srr_is_hit - Packet destination addr was our one @is Changed - IP checksum more not valid @rrneedaddr - Need to record addr of outgoing dev @tsNeedtime - Need to record timestamp @ts needaddr - Need to record addr of outgoing dev   
\*/   
struct ip_options { __be32 faddr; /\*下一跳 \*/ _be32 nexthop; /\*标识IP首部中选项所占的字节数\*/ unsigned char optlen; /\* 记录宽松路由或严格路由选项在IP首部中的偏移量， 即选项的第一个字节的地址减去IP首部的第一个字节的地址   
\*/ unsigned char srr; unsigned char rr; unsigned char ts; unsigned char is_strictroute:1, srr_is_hit:1, is_changed:1, rr_needaddr:1, ts_needtime:1, ts_needaddr:1; unsigned char routeralert; unsigned char cipso; unsigned char __pad2; unsigned char __data[0];   
}; 
```

# 2.3.2 inet_request_sock

该结构位于/include/net/inet_sock.h中。

这个结构的功能呢？

```txt
struct inlet_request_sock {
    struct inlet_request_sock req;
    #define ir_location_addr req._req_common.skc_rcu_saddr
    #define ir_rmt_addr req._req_common.skc_daddr
    #define ir_num req._req_common.skc_num
    #define ir_rmt_port req._req_common.skc_dport
    #define ir_v6_rmt_addr req._req_common.skc_v6_daddr
    #define ir_v6_location_addr req._req_common.skc_v6_rcu_saddr
    #define ir_iif req._req_common.skc_bound_dev_if
    #define ir_cookie req._req_common.skc_cookie
    #define ireq_net req._req_common.skc_net
    #define ireq_state req._req_common.skc_state
    #define ireq_family req._req_common.skc_family
    kmemcheck_bitfield_begin(flags);
    u16 snd_wscale : 4,
    rcv_wscale : 4,
    tstamp.ok : 1,
    sack.ok : 1,
    wscale.ok : 1,
    ecn.ok : 1,
    acked : 1,
    no_srccheck : 1;
    kmemcheck_bitfield_end(flags);
    u32 ir_mark;
    union {
        struct ip_options_rcu *opt;
        struct sk_buffer *pktops;
    };
}; 
```

# 2.3.3 inet_connection_sock_af_ops

该结构位于/include/net/inet_connect_sock.h中, 其后面的 af 表示 address offunction 即函数地址, ops 表示 operations，即操作。

该结构封装了一组与传输层有关的操作集，包括向网络层发送的接口、传输层的setsockopt接口等。

```c
struct inlet_connection_sock_ifOps {
    int (*queue_xmit)(struct sock *sk, struct sk_buff *skb, struct flowi *fl);
    void (*send_check)(struct sock *sk, struct sk_buff *skb);
    int (*rebuild_header)(struct sock *sk);
    void (*sk_rx.dst_set)(struct sock *sk, const struct sk_buff *skb);
    int (*conn_request)(struct sock *sk, struct sk_buff *skb);
    struct sock *(*syn_recv_sock)(const struct sock *sk, struct sk_buff *skb, struct request_sock *req, struct dst_entry *dst, struct request_sock *req_unhash, bool *own_req);
    u16 net_header_len;
    u16 net_frag_header_len; 
```

```c
u16 sockaddr_len;  
int (*setsockopt)(struct sock *sk, int level, int optname, char __user *optval, unsigned int optlen);  
int (*getsockopt)(struct sock *sk, int level, int optname, char __user *optval, int __user *optlen);  
#ifdef CONFIG_COMPAT  
int (*compat_setsockopt)(struct sock *sk, int level, int optname, char __user *optval, unsigned int optlen);  
int (*compat_getsockopt)(struct sock *sk, int level, int optname, char __user *optval, int __user *optlen);  
#endif  
void (*addr2sockaddr)(struct sock *sk, struct sockaddr *);  
int (*bind_conflict)(const struct sock *sk, const struct inet_bind_BUCKET *tb, bool relax);  
void (*mtu_reduced)(struct sock *sk);  
}; 
```

# 2.3.4 inet_connect_sock

该结构位于/include/net/inet_connect_sock.h中，它是所有面向传输控制块的表示。其在inet_sock的基础上，增加了有关连接，确认，重传等成员。

```c
/* inlet_connection_sock - INET connection oriented sock */
* @icsk_accept_queue: FIFO of established children
* @icsk_bind_hash: Bind node
* @icsk_timeout: Timeout
* @icsk_retransmit_timer: Resend (no ack)
* @icsk_rto: Retransmit timeout
* @icsk_pmtu_cookie: Last pmtu seen by socket
* @icsk_ca ops: Pluggable congestion control hook
* @icsk.af ops: Operations which are AF_INET{4,6} specific
* @icsk_ca_state: 拥塞控制状态
* @icsk_retransmits: Number of unrecovered [RTO] timeouts
* @icsk_pending: Scheduled timer event
* @icsk_backoff: Backoff
* @icsk_syn_retries: Number of allowed SYN (or equivalent) retries
* @icsk_probes_out: unanswered O window probes
* @icsk_ext hdr_len: Network protocol overhead (IP/IPv6 options)
* @icsk_ACK: Delayed ACK control data
* @icsk_mtop; MTU probing control data
*/ struct inlet_connection_sock {
/* inlet_sock has to be the first member! */
struct inlet_sock icsk_inet;
struct request_sock_queue icskAccept_queue;
struct inlet_bind:bucket *icsk_bind_hash;
unsigned long icsk_timeout;
struct timer_list icsk_retransmit_timer;
struct timer_list icsk_delack_timer;
__u32 icsk_rto;
__u32 icsk_pmtu_cookie;
const struct tcp_congestionOps *icsk_caops;
const struct inlet_connection_sock.afOps *icsk.afops;
unsigned int (*icskSync_mss)(struct sock *sk, u32 pmtu);
__u8 icsk_ca_state:6, 
```

```c
icsk_ca_setsockopt:1, icsk_ca.dstLocked:1;   
_u8 icsk_retransmits;   
_u8 icsk_pending;   
_u8 icsk_backoff;   
_u8 icsk_syn_retries;   
_u8 icsk_probes_out;   
_u16 icsk_ext hdr_len;   
struct {   
_u8 pending; /* ACK is pending */   
_u8 quick; /* Scheduled number of quick acks */   
_u8 pingpong; /* The session is interactive */   
_u8 blocked; /* Delayed ACK was blocked by socket lock */   
_u32 ato; /* Predicted tick of soft clock */   
unsigned long timeout; /* Currently scheduled timeout */   
_u32 lrcvtime; /* timestamp of last received data packet */   
_u16 last_seg_size; /* Size of last incoming segment */   
_u16 rcv_mss; /* MSS used for delayed ACK decisions */   
} icsk_ACK;   
struct {   
int enabled;   
/* Range of MTUs to search */   
int search_high;   
int search_low;   
/* Information on the current probe. */   
int probe_size;   
u32 probe_timestamp;   
} icsk_rtup;   
u32 icsk_user_timeout;   
u64 icsk_capriv[64 / sizeof(u64)]; /* 拥塞控制算法私有空间 */ #define ICSK_CA_PRIV_SIZE (8 * sizeof(u64))}; 
```

# 2.3.5 inet_timewait_sock

在进入到等待关闭的状态时，已经不需要完整的传输控制块了。Linux 为了减轻在重负载情况下的内存消耗，定义了这个简化的结构体。

```c
/* include/net/inet_timeout_wait_sock.h */
* 
* 这个结构体的存在主要是为了解决重负载情况下的内存负担问题。
*/
struct inlet_timeout_wait_sock {
/* 此处也使用了 sock_common 结构体。
* Now struct sock also uses sock_common, so please just
* don't add nothing before this first member (__tu_common) --acme
*/ 
struct sock_common __tw_common;
/* 通过宏定义为 sock_common 中的结构体起一个 tw 开头的别名。 */
#define tw_family __tw_common.skc_family
#define tw_state __tw_common.skc_state
#define tw_reuse __tw_common.skc_reuse
#define tw_ipv6only __tw_common.skc_ipv6only
```

```c
define tw_bound_dev_if __tw_common.skc_bound_dev_if
#define tw_node __tw_common.skc_nulls_node
#define tw_bind_node __tw_common.skc_bind_node
#define tw_refcnt __tw_common.skc_refcnt
#define tw_hash __tw_common.skc_hash
#define tw_prot __tw_common.skc_prot
#define tw_net __tw_common.skc_net
#define tw_daddr __tw_common.skc_daddr
#define tw_v6_daddr __tw_common.skc_v6_daddr
#define tw_rcu_saddr __tw_common.skc_rcu_saddr
#define tw_v6_rcu_saddr __tw_common.skc_v6_rcu_saddr
#define tw_dport __tw_common.skc_dport
#define tw_num __tw_common.skc_num
#define tw_cookie __tw_common.skc_cookie
#define tw_DR __tw_common.skc_tw_DR
/* 超时时间 */
int tw_timeout;
/* 子状态，用于区分 FIN_WAIT2 和 TIME_WAIT */
volatile unsigned char tw_substate;
/* 窗口缩放 */
unsigned char tw_rcv_wscale;
/* 下面的部分都和 inlet_sock 中的成员相对应的。*/
__be16 tw_sport;
kmemcheck_bitfield_begin(flags);
/* And these are ours. */
unsigned int tw_kill : 1,
tw-transparent : 1,
tw_flowlabel : 20,
tw_pad : 2, /* 2 bits hole */
tw_tos : 8;
kmemcheck_bitfield_end(flags);
/* 超时计时器 */
struct timer_list tw_timer;
struct inlet_bind_BUCKET *tw_TB;
};
```

# 2.3.6 sockaddr & sockaddr_in

sockaddr用于描述一个地址。

```c
/* include/linux/socket.h */
struct sockaddr {
    sa_family_t sa_family; /* 地址所属的协议族，AF_xxx */
    char sa_data[14]; /* 在协议下的地址 */
};
```

可以看出sockaddr是一个较为通用的描述方法。可以支持任意的网络层协议。那么具体到我们的情况，就是 IP 网络。下面是 IP 网络下，该结构体的定义。

```c
/* include/uapi/linux/in.h  
* 该结构体用于描述一个Internet（IP）套接字的地址  
*/  
struct sockaddr_in{  
__kernel_ta_family_t sin_family; /*这里和sockaddr是对应的，填写IP网络*/  
__be16 sin_port; /*端口号 */  
struct in_addr sin_addr; /*Internet地址 */
```

```c
8 /*填充位，为了将sockaddr_in填充到和sockaddr一样长*/ unsigned char __pad[__SOCK_SIZE__ - sizeof(short int) - sizeof(unsigned short int) - sizeof(struct in_addr)];   
10   
11   
12 ； 
```

sockaddr的使用方法是在需要的地方直接强制转型成相应网络的结构体。因此，需要让二者一样大。这就是为何sockaddr_in要加填充位的原因。

2.3.7 ip_options   
```c
/* struct ip_options - IP Options */
* @faddr - 保存的第一跳地址
* @nexthop - 保存在 LSRR 和 SSRR 的下一跳地址
* @is_strictroute - 严格的源路由
* @srr_is_hit - 包目标地址命中
* @is_changed - IP 校验和不合法
* @rrneedaddr - 需要记录出口设备的地址
* @ts Needtime - 需要记录时间戳
* @ts Needaddr - 需要记录出口设备的地址
*/
struct ip_options {
    __be32 faddr;
    __be32 nexthop;
    unsigned char optlen;
    unsigned char srr;
    unsigned char rr;
    unsigned char ts;
    unsigned char is_strictroute:1, srr_is_hit:1, is Changed:1, rrNeedaddr:1, ts Needtime:1, ts Needaddr:1;
    unsigned char routeralert;
    unsigned char cipso;
    unsigned char __pad2;
    unsigned char __data[0];
};
```

# 2.4 路由相关数据结构

# 2.4.1 dst_entry

该结构位于/include/net/dst.h中。

最终生成的 IP 数据报的路由称为目的入口 (dst_entry)，目的入口反映了相邻的外部主机在本地主机内部的一种“映象”。它是与协议无关的目的路由缓存相关的数据结构，保护了路由缓存链接在一起的数据结构成员变量、垃圾回收相关的成员变量、邻

居项相关的成员、二层缓存头相关的成员、输入/输出函数指针以用于命中路由缓存的数据包进行后续的数据处理等。

```c
/* Each dst_entry has reference count and sits in some parent list(s).  
* When it is removed from parent list, it is "freed" (dst_free).  
* After this it enters dead state (dst->obsolete > 0) and if its refcnt  
* is zero, it can be destroyed immediately, otherwise it is added  
* to gc list and garbage collector periodically checks the refcnt.  
*/  
struct dst_entry {  
struct rcu_head rcu_head;  
struct dst_entry *child;  
struct net_device *dev;  
struct dstOps *ops;  
unsigned long _metrics;  
unsigned long expires;  
struct dst_entry *path;  
struct dst_entry *from;  
#ifdef CONFIG_XFRM  
struct xfrm_state *xfrm;  
#else  
void __pad1;  
#endif  
int (*input)(struct sk BUFF *);  
int (*output)(struct net *net, struct sock *sk, struct sk BUFF *skb);  
unsigned short flags;  
#define DST_HOST 0x0001  
#define DST_NOXFRM 0x0002  
#define DST_NOPOLICY 0x0004  
#define DST_NOHASH 0x0008  
#define DST_NOCACHE 0x0010  
#define DST_NOCOUNT 0x0020  
#define DST_FAKE交易平台 0x0040  
#define DST_XFRM_TUNNEL 0x0080  
#define DST_XFRM_queue 0x0100  
#define DST_METADATA 0x0200  
unsigned short pending confirm;  
short error;  
/* A non-zero value of dst->obsolete forces by-hand validation  
* of the route entry. Positive values are set by the generic  
* dst layer to indicate that the entry has been forcefully  
* destroyed.  
* Negative values are used by the implementation layer code to  
* force invocation of the dstOps->check() method.  
*/  
short obsolete;  
#define DST_OBSOLETE_NONE 0  
#define DST_OBSOLETE_DEAD 2  
#define DST_OBSOLETE FORCE_CHK -1  
#define DST_OBSOLETE_KILL -2  
unsigned short header_len; /* more space at head required */  
unsigned short trailer_len; /* space to reserve at tail */  
#ifdef CONFIG_IP-route_CLASSID 
```

```c
__u32 tklassid;   
#else   
__u32 __pad2;   
#endif   
#ifndef CONFIG_64BIT struct lwtunnel_state \*lwtstate; /\* \* Align __refcnt to a 64 bytes alignment \* (L1_CACHE_SIZE would be too much) /\* long __pad_to_align_refcnt[1]; #endif /\* \* __refcnt wants to be on a different cache line from \* input/output/ops or performance tanks badly \*/ atomic_t __refcnt; /\* client references \*/ int __use; unsigned long lastuse;   
#ifndef CONFIG_64BIT struct lwtunnel_state \*lwtstate;   
#include { struct dst_entry \*next; struct rtable __rcu \*rt_next; struct rt6_info \*rt6_next; struct dn-route __rcu \*dn_next; };   
}; 
```

# 2.4.2 rtable

该结构位于/include/net/route.h中。

这是ipv4路由缓存相关结构体，保护了该路由缓存查找的匹配条件，即struct flowi类型的变量、目的 ip、源 ip、下一跳网关地址、路由类型等。当然了，还有最重要的，保护了一个协议无关的dst_entry变量，能够很好地实现dst_entry与 rtable 的转换，而dst_entry中又包含邻居项相关的信息，实现了路由缓存与邻居子系统的关联。

```c
struct rtable {
    /* 存储缓存路由项中独立于协议的信息 */
    struct dst_entry dst;
    int rt_genid;
    /* 表示路由表项的一些特性和标志 */
    unsigned int rt_flags;
    __u16 rt_type;
    __u8 rt_is_input;
    __u8 rt Uses Gateway;
    int rt_iif;
    /* Info on neighbour */
    __be32 rt Gateway;
    /* Miscellaneous cached information */
}
```

```c
18 u32 rt_pmtu;   
19   
20 u32 rt_table_id;   
21   
22 struct list_head rt_uncached;   
23 struct uncached_list \*rt_uncached_list;   
24}; 
```

# 2.4.3 flowi

该数据结构位于/include/net/flow.h中，它是与路由查找相关的数据结构。

```txt
1 struct flowi{ union{ struct flowi_common __fl_common; struct flowi4 ip4; struct flowi6 ip6; struct flowidn dn; }u;   
8 #define flowi_oif u._fl_common.flowic_oif #define flowi_iif u._fl_common.flowic_iif #define flowi_mark u._fl_common.flowic_mark #define flowi_tos u._fl_common.flowic_tos   
12 #define flowi_scope u._fl_common.flowic_scope   
13 #define flowi Proto u._fl_common.flowic Proto   
14 #define flowi_flags u._fl_common.flowic_flags   
15 #define flowi_secid u._fl_common.flowic_secid   
16 #define flowi_tun_key u._fl_common.flowic_tun_key   
17 }__attribute_((__aligned__(BITS_PER_Long/8)))； 
```

# 2.5 TCP 层相关数据结构

# 2.5.1 tcphdr

该数据结构位于/include/uapi/linux/tcp.h 中。这一结构正是 TCP 首部在网络中的样貌。

```txt
1 struct tcphdr {   
2 __be16 source;   
3 _be16 dest;   
4 _be32 seq;   
5 _be32 ack_seq;   
6 #if defined(_LITTLE.ENDIAN_BITFIELD)   
7 __u16 res1:4,   
8 doff:4,   
9 fin:1,   
10 syn:1,   
11 rst:1,   
12 psh:1,   
13 ack:1,   
14 urg:1,   
15 ece:1,   
16 cwr:1;   
17 #elif defined(_BIG_ENDIAN_BITFIELD)   
18 __u16 doff:4,   
19 res1:4, 
```

```c
cwr:1,  
ece:1,  
urg:1,  
ack:1,  
psh:1,  
rst:1,  
syn:1,  
fin:1;  
#else  
#error "Adjust your <asm/byteorder.h> defines"  
#endif  
__be16 window;  
__sum16 check;  
__be16 urg_ptr;  
}; 
```

# 2.5.2 tcp_options_received

该结构位于/include/linux/tcp.h中，其主要表述 TCP 头部的选项字段。

```txt
struct tcp_options_received {
/* PAWS/RTTM data */
long tsrecent_stamp; /* Time we stored tsrecent (for aging)
记录从接收到的段中取出时间戳设置到tsrecent的时间
用于检测tsrecent的有效性：如果自从该事件之后已经
经过了超过24天的时间，则认为tsrecent已无效。
*/
u32 tsrecent; /* Time stamp to echo next
下一个待发送的TCP段中的时间戳回显值。当一个含有最后
发送ACK中确认序号的段到达时，该段中的时间戳被保存在
tsrecent中。而下一个待发送的TCP段的时间戳值是由
SKB中TCP控制块的成员when填入的，when字段值是由协议
栈取系统时间变量jiffies的低32位。
*/
u32 rcv_tval; /* Time stamp value
保存最近一次接收到对端的TCP段的时间戳选项中的时间戳值。
*/
u32 rcv_tsecr; /* Time stamp echo reply
保存最近一次接收到对端的TCP段的时间戳选项中的时间戳
回显应答。
*/
u16 saw_tstamp:1, /* Saw TIMESTAMP on last packet
标识最近一次接收到的TCP段是否存在TCP时间戳选项，1为有，
0为无。
*/
tstamp.ok:1, /* TIMESTAMP seen on SYN packet
标识TCP连接是否启动时间戳选项。
*/
dsack:1, /* D-SACK is scheduled */
wscale.ok:1, /* Wscale seen on SYN packet
标志接收方是否支持窗口扩大因子，只出现在SYN段中。
*/
sack.ok:4, /* SACK seen on SYN packet
标记是否对方提供SACK服务
*/
snd_wscale:4, /* Window scaling received from sender */
rcv_wscale:4; /* Window scaling to send to receiver */
u8 num_sacks; /* Number of SACK blocks */
```

```javascript
39 u16 user_mss; /\* mss requested by user in ioctl \*/   
40 u16 mss_clamp; /\* Maximal mss, negotiated at connection setup \*/   
41}; 
```

2.5.3 tcp_sacktag_state   
```c
1 /\* Location   
2   
3 net/ipv4/tcp_input.c   
4   
5 \*/   
6 struct tcp_sacktag_state { int reord; int fack_count; /\* Timestamps for earliest and latest never-retransmitted segment \* that was SACKed. RTO needs the earliest RTT to stay conservative, \* but congestion control should still get an accurate delay signal. \*/ struct skb_mstamp first_sackt; struct skb_mstamp last_sackt; int flag;   
16   
}; 
```

# 2.5.4 tcp_sock

该数据结构位于/include/linux/tcp.h中。

该数据结构时 TCP 协议的控制块，它在inet_connection_sock结构的基础上扩展了滑动窗口协议、拥塞控制算法等一些 TCP 的专有属性。

```c
struct tcp SOCK {
    /* inlet_connection_sock has to be the first member of tcp_sock */
    struct inlet_connection_sock inletconn;
    u16 tcp_header_len; /* 需要发送的 TCP 头部的字节数 */
    u16 gso_segs; /* Max number of secs per GSO packet */
}
/* Header prediction flags
* 0x5?10 << 16 + snd_umd in net byte order
首部预测标识。
*/
__be32 pred_flags;
*/
RFC793 variables by their proper names. This means you can
* read the code and the spec side by side (and laugh ...
* See RFC793 and RFC1122. The RFC writes these in capitals.
*/
u64 bytes_received; /* RFC4898 tcpStatsAppHCThruOctetsReceived
* sum(delta(rcv_nxt)), or how many bytes
* were acked.
*/
u32 segs_in; /* RFC4898 tcpStatsPerfSegsIn
* total number of segments in.
*/
u32 rcv_nxt; /* 下一个待接收的字节号 */
u32 copied_seq; /* Head of yet unread data 
```

```c
尚未读取数据的头部  
u32 rcv_wup; /* rcv_nxt on last window update sent意思就是在上一个窗口更新的时候，所接收到的确认号也就是上一个窗口更新之后，将要发送的第一个字节的序列号。*/  
u32snd_nxt; /*下一个待发送的序列 */  
u32 segs_out; /* RFC4898 tcpEStatsPerfSegsOut* The total number of segments sent.  
u64 bytes_acked; /* RFC4898 tcpEStatsAppHCTruOctetsAcked* sum (delta (snd_una)), or how many bytes* were acked.  
*/struct u64.statsSync syncp; /* protects 64bit vars (cf tcp_get_info()) */  
u32snd_una; /*待ACK的第一个字节的序号 */  
u32snd_sml; /*Last byte of the most recently transmitted small packet */  
u32rcv_tstamp; /*timestamp of last received ACK (for keepalives) */  
u32lsndtime; /*timestamp of last sent data packet (for restart window) */  
u32last_ow_ACK_time; /* timestamp of last out-of-window ACK */  
u32tsoffset; /* timestamp offset */  
struct list_head tsq_node; /* anchor in tsq_tasklet.head list */  
unsigned long tsq_flags;  
/* Data for direct copy to user */  
struct {struct sk BUFF_head prequeue;struct task_struct *task;struct msghdr *msg;int memory;int len;}ucopy;  
u32snd wl1; /* Sequence for window update */  
u32snd_wnd; /*发送窗口 */  
u32max_window; /*Maximal window ever seen from peer */  
u32mss_cache; /*Cached effective mss, not including SACKS */  
u32window_clamp; /* Maximal window to advertise滑动窗口最大值*/  
u32rcv_ssthresh; /* Current window clamp */  
/* Information of the most recently (s)acked skb */  
struct tcp_rack {struct skb_mstamp mstamp; /* (Re)sent time of the skb */u8 advanced; /* mstamp advanced since last lost marking */u8 reord; /* reordering detected */} rack;  
u16advmss; /* Advertised MSS本端能接收的MSS上限，在建立时用来通告对方。*/  
u8unused; 
```

```c
u8 nonagle : 4,/* Disable Nagle algorithm? */
thin_lto : 1,/* Use linear timeouts for thin streams */
thin_dupack : 1,/* Fast retransmit on first dupack */
repair : 1,
frto : 1;/* F-RTO (RFC5682) activated in CA_Loss */
u8 repair_queue;
u8 do_early_retrans : 1,/* Enable RFC5827 early-retransmit */
syn_data:1, /* SYN includes data */
syn_fastopen:1, /* SYN includes Fast Open option */
syn_fastopen_exp:1,/* SYN includes Fast Open exp. option */
syn_data_acked:1,/* data in SYN is acked by SYN-ACK */
save_syn:1, /* Save headers of SYN packet */
is_cwnd Limited:1;/* forward progress limited by snd_cwnd? */
u32 tlp_high_seq; /* snd_nxt at the time of TLP retransmit. */
/* RTT measurement */
u32 srtt_us; /* smoothed round trip time << 3 in usecs */
u32 mdev_us; /* medium deviation */
u32 mdev_max_us; /* maximal mdev for the last rtt period */
u32 rttrvar_us; /* smoothed mdev_max */
u32 rtt_seq; /* sequence number to update rttrvar */
struct rtt(meas {
u32 rtt, ts; /* RTT in usec and sampling time in jiffies. */
} rtt_min[3];
u32 packets_out; /* Packets which are "in flight"
发送方发送出去但是还未得到确认的 TCP 段的数目
packets_out=SND.NXT-SND.UNA
*/
u32 retrans_out; /* Retransmitted packets out
重传并且还未得到确认的 TCP 段的数目
*/
u32 max_packets_out; /* max packets_out in last window */
u32 max_packets_seq; /* right edge of max_packets_out flight */
u16 urg_data; /* Saved octet of OOB data and control flags */
u8 ecn_flags; /* ECN status bits.
*/
u8 keepalive_probes; /* num of allowed keep alive probes */
u32 reordering; /* Packet reordering metric.
*/
u32 snd_up; /* Urgent pointer */
*/
*/
* Options received (usually on last packet, some only on SYN packets).
*/
struct tcp_options_received rx_opt;
*/
* Slow start and congestion control (see also Nagle, and Karn & Partridge)
*/
u32 snd_ssthresh; /* Slow start size threshold */
u32 snd_cwnd; /* Sending congestion window
发送的拥塞窗口
*/
u32 snd_cwnd_cnt; /* Linear increase counter
自从上次调整拥塞窗口到目前为止接收到的总 ACK 段数。
如果该字段为零，则说明已经调整了拥塞窗口，且到目前
为止还没有接收到 ACK 段。调整拥塞窗口之后，每接收到
一个 ACK, ACK 段就会使 snd_cwnd 加 1。
*/ 
```

u32snd_cwnd_clamp;/\*Do not allowsnd_cumd to grow above this \*/   
u32snd_cwnd_used;   
u32snd_cwnd_stamp; /\*记录最近一次检验拥塞窗口的时间 在拥塞期间，接收到ACK后会进行 拥塞窗口的检验。而在非拥塞期间，为了防止由于应用 程序限制而造成拥塞窗口失效，因此在成功发送段后， 如果有必要也会检验拥塞窗口。 \*/   
u32prior_cwnd; /\*Congestion window at start of Recovery. 在进入Recovery状态时的拥塞窗口 \*/   
u32prrdelivered; /\*Numberof newly delivered packets to \*receiver inRecovery. 在恢复阶段给接收者新发送包的数量 \*/   
u32prr_out; /\*Total number of pkts sent during Recovery. 在恢复阶段一共发送的包的数量 \*/   
u32rcv_wnd; /\*Current receiver window \*/   
u32write_seq; /\*Tail(+1)of data held in tcp send buffer 已加入发送队列中的最后一个字节序号。 \*/   
u32notsent_lowat; /\*TCP_NOTSENT_LOWAT \*/   
u32pushed_seq; /\*Last pushed seq,required to talk to windows \*/   
u32lost_out; /\*Lost packets 丢失的数据报 \*/   
u32sacked_out; /\*SACK'd packets 启用SACK时，通过SACK的TCP选项标识已接收到的段的 不启用SACK时，标识接收到的重复确认的次数，该值在接 只从STCP,retrans queue hinting \*/   
structsk BUFF\* lost_skb_hint;   
structskbuff\*retransmit_skb_hint;   
/\*000 segments go in this list.Note that socket lock must be held, \*as we do not use sk BUFF_head lock. \*/   
structskbuffhead out_of_order_queue;   
/\*SACKs data,these 2 need to be together (see tcp_options_write) \*/ structtcp_sack_block duplicate_sack[1]；/\*D-SACK block \*/ structtcp_sack_block selective_acks[4]；/\*The SACKS themselves\*/ structtcp_sack_block recv_sack_cache[4];   
structskbuff\*highest_sack; /\*skb just after the highest \*skb with SACKed bit set \*(validity guaranteed only if \*sacked_out $>0$

```c
int lost_cnt_hint;   
u32 retransmit_high; /\* L-bits may be on up to this seqno \*/   
u32 prior_ssthresh; /\* ssthresh saved at recovery start \*/   
u32 high_seq; /\*snd_nxt at onset of congestion 开始拥塞的时候下一个要发送的序号字节 \*/   
u32 retrans_stamp; /\* Timestamp of the last retransmit, \* also used in SYN-SENT to remember stamp of \* the first SYN. \*/   
u32 undomarker; /\*snd_una upon a new recovery episode. 在使用F-RTO算法进行发送超时处理，或进入Recovery进行重传， 或进入Loss开始慢启动时，记录当时SND.UNA，标记重传起始点。 它是检测是否可以进行拥塞控制撤销的条件之一，一般在完成 拥塞撤销操作或进入拥塞控制Loss状态后会清零。 \*/   
int undo_retrans; /\*number of undoable retransmissions. 在恢复拥塞控制之前可进行撤销的重传段数。在进入FTRO算法或 拥塞状态Loss时，清零，在重传时计数，是检测是否可以进行拥塞 撤销的条件之一。 \*/   
u32 total_retrans; /\* Total retransmits for entire connection \*/   
u32 arg_seq; /\*Seq of received urgent pointer \*/   
unsigned int keepalive_time; /\*time before keep alive takes place \*/   
unsigned int keepalive_intvl; /\* time interval between keep alive probes \*/   
int linger2;   
/\* Receiver side RT estimation \*/   
struct { u32 rtt; u32 seq; u32 time; }rcv_rtt_est;   
\/* Receiver queue space \*/   
struct { int space; u32 seq; u32 time; }rcvq_space;   
\/* TCP-specific MTU probe information. \*/   
struct { u32 probe_seq_start; u32 probe_seq_end; }mtuprobe;   
u32 mtu_info; /\*We received an ICMP_FRAG_NEEDED/ICMPV6_PKT_TOOBIG \* while socket was owned by user. \*/   
#ifdef CONFIG_TCP_MD5SIG   
\/*TCP AF-Specific parts;only used by MD5 Signature support so far \*/ 
```

```c
const struct tcp_sock.afOps *afspecific;   
/\*TCP MD5 Signature Option information \*/ struct tcp_md5sig_info __rcu \*md5sig_info; #endif   
/\*与TCPFastOpen相关的信息\*/ struct tcp_fastopen_request \*fastopen_req; /\*fastopen_rsk points to request_sock that resulted in this big \* socket. Used to retransmit SYNACKs etc. \*/ struct request_sock \*fastopen_rsk; u32 \*saved_syn;   
}; 
```

# 2.5.5 tcp_fastopen_cookie

这里，len 和 val 分别对应于1.3.8.2中描述的选项格式中的 len 和 cookie。如果 len为 0，则代表需要请求一个 Cookie。

```c
/* Location: include/linux/tcp.h */
*/
* TCP Fast Open Cookie as stored in memory
*/
struct tcp_fastopen_cookie {
    s8 len;
    u8 val[TCP_FASTOPEN_COOKIE_MAX];
    bool exp; /* In RFC6994 experimental option format */
}; 
```

# 2.5.6 tcp_fastopen_request

该结构体用于记录在 fast open 过程中发送数据的请求。

```c
struct tcp_fastopen_request {
    /* Fast Open cookie. */
    struct tcp_fastopen_cookie
        struct msghdr
            size_t
int
}; 
```

# 2.5.7 tcp_request_sock

```c
struct tcp_request_sock {
    struct inet_request_sock req;
    const struct tcp_request_sockOps *afspecific;
    struct skb_mstamp cnt_synack; /* first SYNACK sent time */
    bool tfo Listener;
    u32 txhash;
    u32 rcv_isn;
    u32 cnt_isn;
    u32 last_oow_ACK_time; /* last SYNACK */
    u32 rcv_nxt; /* the ack # by SYNACK. For */
* FastOpen it's the seq#
* after data-in-SYN. 
```

```txt
13 14}; 
```

# 2.5.8 tcp_skb_cb

在2.2.4中，我们分析过cb。该结构是 TCP 层在 SKB 区的私有信息控制块。对这个私有信息控制块的赋值一般在本层接收到段或发送段之前进行。在这一节中，我们将看到 TCP 层具体是如何使用这个控制缓冲区 (Control Buffer) 的。

# 2.5.8.1 TCP_SKB_CB

在 TCP 层，用该宏访问给定的sk_buff的控制缓冲区的变量。在后续的章节中，可以在很多函数中看到它的身影。该宏的定义如下：

```c
1 #define TCP_SKB_CB(_skb) ((struct tcp_skb_cb *)&((_,skb->cb[0])) 
```

可以看到，该宏实际上是将cb的指针强制转型成tcp_skb_cb 结构体的指针。也就是说，TCP 对于控制缓冲区的使用，可以从tcp_skb_cb 的定义分析出来。

# 2.5.8.2 tcp_skb_cb 结构体

tcp_skb_cb结构体用于将每个 TCP 包中的控制信息传递给发送封包的代码。该结构体的定义如下：

```c
struct tcp_skb_cb {
    __u32 seq; /* 起始序号 */
    __u32 end_seq; /* SEQ + FIN + SYN + dataLen */
    union {
        /* Note: tcp_tw_isn is used in input path only
            */
            (* (isn chosen by tcp_timeout_state_process))
            *
            */
            * tcp_gso_segs/size are used in write queue only,
            */
            * cf tcp_skb_pcount()/tcp_skb_mss()
        }
    __u32 tcp_tw_isn;
    struct {
        u16 tcp_gso_segs;
        u16 tcp_gso_size;
    };
}; 
```

紧接着，定义了一些宏作为标志

```c
1 #define TCPCB_SACKED_ACKED 0x01 /* SKB 被确认了 */
2 #define TCPCB_SACKED_RETRANS 0x02 /* SKB 被重传了 */
3 #define TCPCB_LOST 0x04 /* SKB 已丢失 */
4 #define TCPCB_TAGBITS 0x07 /* 标志位掩码 */
5 #define TCPCB_REPAIRED 0x10 /* SKB 被修复了 (no skb_mstamp) */
```

```c
6 define TCPCB_EVER_RETRANS 0x80 /* SKB 曾经被重传过 \*/   
7 #define TCPCB_RETRANS (TCPCB_SACKED_RETRANS/TCPCB_EVER_RETRANS/ \   
8 TCPCB_REPAIRED)
```

接下来又继续定义 TCP 相关的位。

```c
__u8 ip_dsfield; /* IPv4 tos or IPv6 dsfield */
/* 1 byte hole */
__u32 ack_seq; /* ACK 的序号 */
union {
    struct inet_skb parm h4;
};
if IS_ENABLED(CONFIG_IPV6)
{
    structINET6_skb parm h6;
};
endif
}header; /* For incoming frames */
};
```

# 2.5.8.3 tcp_out_options

tcp_out_options用于存放发送 TCP 包时，TCP 包所含的选项。

```c
/* Location: net/ipv4/tcp_output.c */
#define OPTION_SACK_ADVISE (1 << 0)
#define OPTION_TS (1 << 1)
#define OPTION_MD5 (1 << 2)
#define OPTION_WSCALE (1 << 3)
#define OPTION_FAST_OPEN_cookie (1 << 8)
struct tcp_out_options {
    u16 options; /* OPTION_* 的位域 */
    u16 mss; /* 如果为 0，则表示关闭该选项 */
    u8 ws; /* 窗口放大，0 表示关闭该选项 */
    u8 num_sack_blocks; /* number of SACK blocks to include */
    u8 hash_size; /* bytes in hash_location */
    __u8 *hash_location; /* temporary pointer, overloaded */
    __u32 tsval, tsecr; /* need to include OPTION_TS */
    struct tcp_fastopen_cookie *fastopen_cookie; /* Fast open cookie */
}; 
```

# CHAPTER 3

TCP 输出

# Contents

3.1 数据发送接口 . . . 41

3.1.1 tcp_sendmsg . . 41   
3.1.2 tcp_sendmsg_fastopen . . 46   
3.1.3 TCP Push 操作 . . 46

3.1.3.1 forced_push . . . 46   
3.1.3.2 tcp_mark_push 47   
3.1.3.3 tcp_push . . . 47   
3.1.3.4 tcp_push_one . . 48   
3.1.3.5 __tcp_push_pending_frames . . 48

3.2 输出到 IP 层 . . . 48

3.2.1 tcp_write_xmit . . . 48   
3.2.2 tcp_transmit_skb . 51   
3.2.3 tcp_select_window(struct sk_buff *skb) 54

# 3.1 数据发送接口

# 3.1.1 tcp_sendmsg

使用 TCP 发送数据的大部分工作都是在tcp_sendmsg函数中实现的。

```txt
/* Location: net/ipv4/tcp.c
* 
* Parameter:
*     sk 传输所使用的套接字
*     msg 要传输的用户层的数据包
*     size 用户要传输的数据的大小
*/
```

{ struct tcp_sock \*tp $=$ tcp_sk(sk); struct sk_buffer \*skb; int flags, err, copied $= 0$ int mss_now $= 0$ size_goal, copied_syn $= 0$ bool sg; long timeo; /\*对套接字加锁\*/ lock_sock(sk); flags $=$ msg->msg_flags; if (flags & MSG_FASTOPEN) { err $=$ tcp_sendmsg_fastopen(sk, msg, &copied_syn, size); if (err $= =$ -EINPROGRESS && copied_syn $>0$ goto out; else if (err) goto out_err; }

根据调用者传入的标志位，判断是否启用了快速开启 (Fast Open)。关于 Fast Open 的讨论，详见 1.3.8。如果启用了 Fast Open，则会调用 tcp_sendmsg_fasopen函数进行相关处理。

timeo $=$ sock_sndtimeo(sk, flags & MSG_DONTWAIT);   
/*等待连接完成。由于TCP是面向连接的，如果还没有建立好连接的话，是无法 *发送任何东西的。不过，这里有一种例外情况，如果是处于FastOpen的被动端的话 \*是可以在三次连接的过程中带上数据的。   
\*/ if((1<<sk->sk_state)&~(TCPF_ESTABLISHED|TCPF_CLOSE_WAIT))&&！tcp Passive fastopen(sk)){ err $=$ sk_STREAM_wait_connect(sk,&timeo); if(err!=0) goto do_error;   
}   
/\*TCP repair是Linux3.5引入的新补丁，它能够实现容器在不同的物理主机间迁移。 \*它能够在迁移之后，将TCP连接重新设置到之前的状态。   
\*/ if (unlikely(tp->repair)){ if(tp->repair_queue $= =$ TCP_RECV_queue){ copied $=$ tcp_send_rcvq(sk, msg,size); goto out_nopush; } err $= -EINVAL$ if(tp->repair_queue $= =$ TCP_NO_queue) goto out_err; /\* 'common' sending to sendq \*/   
}   
/\*This should be in poll \*/ sk_clear_bit(SOCKWQASYNC_NOSPACE,sk);   
/\*获取MSS大小。size_goal是数据报到达网络设备时所允许的最大长度。 \*对于不支持分片的网卡，size_goal是MSS的大小。否则，是MSS的整倍数。

\*/ mss_now $=$ tcp_send_mss(sk, &size_goal, flags); /\* Ok commence sending. \* copied 是已经从用户数据块复制出来的字节数。 \*/ copied $= 0$ err $=$ -EPIPE; if (sk->sk_err || (sk->sk_shutdown & SEND_SHUTDOWN)) goto out_err; sg $= 11$ (sk->sk-route_caps & NETIF_F_SG); /* 不断循环，将用户想要发送的东西全部发送出去。\*/ while (msg_data_left(msg)){ /\* copy 代表本次需要从用户数据块中复制的数据量。\*/ int copy $= 0$ int max $=$ size_goal; /\*获得队尾的SKB，并判断SKB剩余的能携带的数据量。\*/ skb $=$ tcp_write_queueTAIL(sk); if (tcp_send_head(sk)) { if (skb->ip_summed $= =$ CHECKSUM_NONE) max $=$ mss_now; copy $=$ max-skb->len; } /\*如果没法携带足够的数据了，那么就重新分配一个SKB。\*/ if (copy <= 0){ new_segment: /\* Allocate new segment.If the interface is SG, \*allocate skb fitting to single page. \*/ if (!sk_STREAM_memory_free(sk)) goto wait_for_sndbuf; skb $=$ sk_STREAM_alloc_skb(sk, select_size(sk, sg), sk->sk_allocation, skb_queue_empty(&sk->sk_write_queue)); if (!skb) goto wait_for_memory; /\* Check whether we can use HW checksum. \*/ if (sk->sk-route_caps & NETIF_F_ALL_CSUM) skb->ip_summed $=$ CHECKSUM_PARTIAL; /\*将新的SKB放到队尾，并设定copy和max的值。\*/ skb_entail(sk, skb); copy $=$ size_goal; max $=$ size_goal; /\* All packets are restored as if they have \*already been sent. skb_mstamp isn't set to \*avoid wrong rtt estimation.

```c
if (tp->repair) TCP_SKB_CB(skb)->sacked |= TCPCB_REPAIRED;   
}   
/* Try to append data to the end of skb. */ if (copy > msg_data_left(msg)) copy = msg_data_left(msg);   
/* 下面的部分在寻找哪里还有空间可以放数据 */   
if (skb_availroom(skb) > 0) { /* 在 SKB 头部还有一些空间，重新计算 copy 的值，以使用该空间。*/ copy = min_t(int, copy, skb_availroom(skb)); err = skb_add_data_nocache(sk, skb, &msg->msg_iter, copy); if (err) goto do_fault; } else { bool merge = true; int i = skb_shinfo(skb) ->nr_frags; struct pagefrag *pfrag = sk_page_frag(sk); if (!sk_pagefrag_refill(sk, pfrag)) goto wait_for_memory; /* 判断能否在最后一个分片加数据。*/ if (!skb_can_coalesce(skb, i, pfrag->page, pfrag->offset)) { if (i == systcl_max_skb_frags || !sg) { /* 无法设置分配，那么就重新分配一个 SKB。*/ tcp_mark-push(tp, skb); goto new_segment; } merge = false; } copy = min_t(int, copy, pfrag->size - pfrag->offset); if (!sk_wmem_schedule(sk, copy)) goto wait_for_memory; /* 将用户数据块复制到 SKB 中。*/ err = skb_copy_to_page_nocache(sk, &msg->msg_iter, skb, pfrag->page, pfrag->offset, copy); if (err) goto do_error; /* 更新 SKB。*/ if (merge) { skb FAG_size_add(&skb_shinfo(skb) ->frags[i - 1], copy); } else { skb_fill_page_desc(skb, i, pfrag->page, pfrag->offset, copy); get_page(pfrag->page); } pfrag->offset += copy; } 
```

if(!copied) TCP_SKB_CB(sk)->tcp_flags $\& =$ ~TCPHDR_PSH;   
\*/更新TCP的序号\*/ tp->write_seq $+ =$ copy; TCP_SKB_CB(sk)->end_seq $+ =$ copy; tcp_skpcount_set(sk,0); copied $+ =$ copy; if(!msg_data_left(msg)){ tcp_tx_timestamp(sk,skb); goto out; } if(sk->len<max||(flags&MSG_OOB)||unlikely(tp->repair)) continue;   
\*/检查该数据是否必须立即发送。\*/ if Forced.push(tp)){ /*如果需要立即发送，则调用相关函数将队列里的数据都发送出去\*/ tcp_mark.push(tp,skb); --tcp.push_pending Frames(sk,mss_now,TCP_NAGLE_PUSH); }elseif(sk==tcp_send_head(sk)) tcp.push_one(sk,mss_now); continue;   
wait_for_sndbuf: set_bit(SOCK_NOPACE,&sk->sk_SOCKET->flags); /\*设置当前的状态为无空间状态，并等待内存空间。\*/   
wait_for_memory: /\*如果已经复制了一定的数据了，那么将数据先发出去。\*/ if(copied) tcp.push(sk,flags&~MSG_MORE,mss_now, TCP_NAGLE_PUSH,size_goal); err $=$ sk_STREAM_wait_memory(sk,&timeo); if(err $! = 0$ goto do_error; mss_now $=$ tcp_send_mss(sk,&size_goal,flags);

之后，是一系列的错误处理部分。（事实上，正常退出也要经过这里的out和 out_nopush阶段）

out: /\*如果发生了超时或者要正常退出，且已经拷贝了数据，那么尝试将该数据发出\*/ if (copied) tcp.push(sk,flags,mss_now,tp->nonagle,size_goal);   
out_nopush: /\*将sk释放，并返回已经发出的数据量。\*/ release SOCK(sk); return copied $^+$ copied_syn;   
do_fault: /\*当拷贝数据发送异常是，会进入这个分支。如果当前的SKB是新分配的， \*那么，将该SKB从发送队列中去除，并释放该SKB。 \*/ if(!skb->len){

```c
tcp_unlink_write_queue(skb, sk);
/* It is the one place in all of TCP, except connection */
* reset, where we can be unlinking the send_head.
*/
tcp_check_send_head(sk, skb);
sk_wmem_free_skb(sk, skb);
}
do_error:
/* 如果已经拷贝了数据，那么，就将其发出。
*/
if (copied + copied_syn)
goto out;
out_error:
/* 获取并返回错误码，释放锁。 */
err = sk_STREAM_error(sk, flags, err);
/* make sure we wake any epoll edge trigger waiter */
if (unlikely(skb_queue_len(&sk->sk_write_queue) == 0 && err == -EAGAIN))
sk->sk_write_space(sk);
release_sock(sk);
return err;
} 
```

# 3.1.2 tcp_sendmsg_fastopen

如果启用了 Fast Open，则会在这里分配一个tcp_fastopen_request结构体，并将用户消息和大小填写到对应的字段上。

static int tcp_sendmsg_fastopen(struct sock \*sk, struct msghdr \*msg, int \*copied,size_t size)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); int err，flags; /\*如果没有开启该功能，返回错误值\*/ if(!(sysct1 tcp_fastopen&TFO_CLIENT_ENABLE)) return -EOPNOTSUPP; /\*如果已经有要发送的数据了，返回错误值\*/ if(tp->fastopen_req) return -EALREADY; /*Another Fast Open is in progress */ /\*分配空间并将用户数据块赋值给相应字段\*/ tp->fastopen_req=kzalloc(sizeof(struct tcp_fastopen_request), sk->sk_allocation); if (unlikely(!tp->fastopen_req)) return -ENOBUFS; tp->fastopen_req->data $=$ msg; tp->fastopen_req->size $=$ size; flags $=$ (msg->msg_flags&MSG_DONTWAIT)?O_NONBLOCK:0; /\*由于fast open时，连接还未建立，因此，这里直接调用了下面的 \*函数建立连接。这样数据就可以在连接建立的过程中被发送出去了。 \*/ err $=$ __inet_STREAM_connect(sk->sk_SOCKET，msg->msg_name， msg->msg_namelen，flags); \*copied $=$ tp->fastopen_req>copied; tcp_free_fastopen_req(tp); return err;

在连接结束后，释放 Fast Open 请求信息结构体，完成了 Fast Open 过程。

# 3.1.3 TCP Push 操作

TCP 协议提供了 PUSH 功能，只要添加了该标志位，TCP 层会尽快地将数据发送出去。以往多用于传输程序的控制命令。在目前的多数 TCP 实现中，用户往往不会自行指定 PUSH。TCP 的实现会根据情况自行指定 PUSH 位。

# 3.1.3.1 forced_push

既然目前的 TCP 实现多数都会自行指定 PUSH 标志位，那么究竟在什么情况下，数据包会被设置 PUSH 标志位呢？forced_push函数给出了必须设置 PUSH 标志位的一种情况。

```c
static inline bool forced.push(const struct tcp_sock *tp) {
    /* 当上一次被 PUSH 出去的包的序号和当前的序号 */
    *相差超过窗口的一半时，会强行被 PUSH。
    */
    return after(tp->write_seq, tp->pushed_seq + (tp->max_window >> 1));
}
```

从这里可以看出，在 Linux 中，如果缓存的数据量超过了窗口大小的一半以上，就会被尽快发送出去。

# 3.1.3.2 tcp_mark_push

tcp_mark_push函数用于标记一个数据包的 PUSH 位。

```c
static inline void tcp_mark.push(struct tcp_sock *tp, struct sk_buffer *skb) {
    TCP_SKB_CB(skb) ->tcp_flags |= TCPHDR_PSH;
    tp->pushed_seq = tp->write_seq;
} 
```

# 3.1.3.3 tcp_push

tcp_push函数用于发送处于队列中的包。

```c
static void tcp.push(struct sock *sk, int flags, int mss_now, int nonagle, int size_goal)  
{ struct tcp SOCK *tp = tcp_sk(sk); struct sk_buffer *skb; /*如果已经没有可以发送的包了，直接返回。*/ if (!tcp_send_head(sk)) return; /*取出队尾的数据包，如果没有更多的数据片段或者 *满足了forced.push的条件，那么，就将该包标记上PUSH。 */ skb = tcp_write_queueTAIL(sk);
```

if(!flags&MSG_MORE)||forced.push(tp)) tcp_mark.push(tp,skb);   
tcp_mark Urg(tp，flags);   
if（tcp_shoulautocork(sk，skb,size_goal）{ /\*avoidatomicopifTSQ_THROTTLEDbitisalreadyset\*/ if(!test_bit(TSQ_THROTTLED， $\text{已} \mathfrak { p } - >$ tsq_flags)){ NET_INC_STATSSocket_net(sk)，LINUX_MIB_TCPAUTOCORKING); set_bit(TSQ_THROTTLED， $\text{已} \mathfrak { p } - >$ tsq_flags); 1 /\*ItispossibleTXcompletionalreadyhappened \*beforewesetTSQ_THROTTLED. \*/ if(atomic_read(&sk->sk_wmem_alloc)>skb->truesize) return;   
}   
if flags&MSG_MORE) nonagle $=$ TCP_NAGLE_CORK;   
/\*将数据包发送出去\*/ --tcppushpending Frames(sk，mss_now，nonagle);

# 3.1.3.4 tcp_push_one

该函数用于将发送队列首部的单个包发送出去。

void tcp.push_one(struct sock \*sk，unsigned int mss_now)   
{ struct sk_buffer \*skb $=$ tcp_send_head(sk); BUG_ON(!skb || skb->len < mss_now); tcp_write_xmit(sk，mss_now，TCP_NAGLE_PUSH,1，sk->sk_allocation);   
}

# 3.1.3.5 __tcp_push_pending_frames

/\*将等待在队列中的包全部发出。\*/   
void__tcp.push_pending Frames(struct sock \*sk，unsigned int cur_mss, int nonagle)   
{ /\*如果此时连接已经关闭了，那么直接返回。\*/ if (unlikely(sk->sk_state $= =$ TCP_CLOSE)) return; /\*将剩余的部分发送出去。\*/ if (tcp_write_xmit(sk，cur_mss，nonagle，0, sk_gfp_atomic(sk,GFPAtomic))) tcp_checkprobe_timer(sk);   
}

# 3.2 输出到 IP 层

# 3.2.1 tcp_write_xmit

在上面的很多代码中，都最终调用了tcp_write_xmit来完成发送功能。

```c
/* Location: net/ipv4/tcp_output.c */
*/
*/
Parameter:
* sk: 套接字
* mss_now: 当前有效的 MSS 大小
* nonagle: 是否启用 Nagle 算法。
* push_one: 当 push_one 大于 0 时, 最多发送一个包
*/
static bool tcp_write_xmit(struct sock *sk, unsigned int mss_now, int nonagle, int push_one, gfp_t gfp)
{
struct tcp SOCK *tp = tcp_sk(sk);
struct sk BUFF *skb;
unsigned int tso_segs, sent_pkts;
int cwnd_quota;
int result;
bool is_cwnd_limit = false;
u32 max_segs;
/* 该变量用于统计发送的包的数量, 初始化为 0 */
sentPkts = 0;
if (!push_one) {
/* 进行 MTU 探测 */
result = tcp_mtuprobe(sk);
if (!result) {
return false;
} else if (result > 0) {
sentPkts = 1;
}
}
/* 获取最大的段数 */
max_segs = tcp_tso_autosize(sk, mss_now);
/* 不断循环发送队列, 进行发送 */
while ((skb = tcp_send_head(sk))) {
unsigned int limit;
/* 获取 TSO 的信息 */
tso_segs = tcp_init_tso_segs(skb, mss_now);
BUG_ON(!tso_segs);
if (unlikely(tp->repair) && tp->repair_queue == TCP_SENDQueue) {
/* "skb_mstamp" is used as a start point for the retransmit timer */
skb_mstamp_get(&skb->skb_mstamp);
goto repair; /* Skip network transmission */
}
/* 获取 CWND 的剩余大小 */
cwnd_quota = tcp_cwnd_test(tp, skb);
if (!cwnd_quota) {
if (push_one == 2) 
```

/\*强制发送一个包进行丢包探测。\*/ cwnd_quota $= 1$ else $\text{一} ^ { \text{一} }$ 如果窗口大小为0，则无法发送任何东西。\*/ break;   
}   
/\*如果当前段不完全在发送窗口内，则无法发送\*/ if (unlikely(!tcp_snd_wnd_test(tp, skb, mss_now))) break;   
if(tso_segs $= = 1$ ）{ /\*如果无需TSO分段，则检测是否启用Nagle算法。\*/ if(unlikely(!tcp_nagle_test(tp, skb, mss_now, (tcp_skb_is_last(sk, skb)? nonagle：TCP_NAGLE_PUSH)))) break;   
}else{ /\*如果需要TSO分段，则检查是否需要延迟发送。\*/ if(!push_one&& tcp_tso.should_defer(sk, skb, &is_cwnd限制的, max_segs)) break;   
}   
/\*根据分段对于包进行分段处理。\*/ limit=mss_now;   
if(tso_segs>1&&!tcp Urg_mode(tp)) limit $=$ tcp_mss_split_point(sk, skb, mss_now, min_t(unsigned int, cwnd_quota, max_segs), nonagle);   
/\*如果长度超过了分段限制，那么调用tso Fragment进行分段。\*/ if(sk->len>limit&& unlikely(tso Fragment(sk, skb, limit, mss_now, gfp))) break;

后面是 Linux3.6 引入的新机制。在之前的 Linux 实现中，如果 TCP 的窗口很大，那么，可能导致驱动队列中待发送的包的的数目超过队列缓存的大小，因而导致丢包。为了解决该问题，Linux 引入了 TCP 小队列机制。

/\*TCPSmallQueues：\*控制进入到qdisc/devices中的包的数目。\*该机制带来了以下的好处：\* -更好的RTT估算和ACK调度\* -更快地恢复\* -高数据率\*Alas,somedrivers/subsystemsrequirea fair amount\*of queued bytes to ensure line rate.\*One example is wifi aggregation (802.11 AMPDU)\*/limit $=$ max(2 $\ast$ skb->truesize，sk->sk_pacing_rate>>10);limit $=$ min_t(u32，limit，sysctl tcp_limit_output_bytes);if (atomic_read(&sk->sk_wmem_alloc)>limit){set_bit(TSQ_THROTTLED，&tp->tsq_flags);

/\*It is possible TX completion already happened \* before we set TSQ_THROTTLED, so we must \* test again the condition. \*/ smp_mb_after_atomic(); if (atomic_read(&sk->sk_wmem_alloc) > limit) break; } /\*调用tcp_transmit_skb将包真正发送出去\*/ if (unlikely tcp_transmit_skb(sk, skb, 1, gfp))) break;   
repair: /\* Advance the send_head. This one is sent out. \*This call will increment packets_out. \*/ tcp_event_new_data_sent(sk, skb); /\*如果发送的段小于MSS，则更新最后一个小包的序号\*/ tcp_minshall_update(tp, mss_now, skb); sent_pkts $+ =$ tcp_skb_pcount(skb); if (push_one) break;   
} /\*如果发送了数据，那么就更新相关的统计。\*/ if (likely(sent_PKts)) { if (tcp_in_cwnd_reduction(sk)) tp->prr_out $+ =$ sent_PKts; /\* Send one loss probe per tail loss episode.\*/ if (push_one != 2) tcp_schedule_lossProbe(sk); is_cwnd限制 |= (tcp_packets_in_airight(tp) $> =$ tp->snd_cwnd); tcp_cwnd_validator(sk, is_cwndlimited); return false; } return!tp->packets_out && tcp_send_head(sk);

# 3.2.2 tcp_transmit_skb

真正的发送操作是在tcp_transmit_skb中完成的。该函数最主要的工作是构建TCP的首部，并将包交付给 IP 层。由于数据报需要等到 ACK 后才能释放，所以需要在发送队列中长期保留一份 SKB 的备份。

```c
/* Location: net/ipv4/tcp_output.c  
*  
* Parameter:  
* sk: 套接字  
* skb: 要发送的包  
* clone_it: 克隆或复制  
* gpf_mask: 内存分配方式  
*/  
static int tcp_transmit_SKb(struct sock *sk, struct sk BUFF *skb, int clone_it,
```

gfp_t gfp_mask)   
{ const struct inlet_connection_sock \*icsk $=$ inlet_csk(sk); struct inlet SOCK \*inet; struct tcp SOCK \*tp; struct tcp_SKb_cb \*tcb; struct tcp_out_options opts; unsigned int tcp_options_size, tcp_header_size; struct tcp_md5sig_key \*md5; struct tcphdr \*th; int err; BUG_ON(!skb || !tcp_skb_pcount(sk)); if (clone_it){ skb_mstamp_get(&skb->skb_mstamp); /\*这里收到的SKB可能是原始SKB的克隆，也可能是 \*来自重传引擎的一份拷贝。 \*/ if (unlikely(skb_cloned(sk))) skb $=$ pskb_copy(skb,gfp_mask); else skb $=$ skb_clone(skb,gfp_mask); if (unlikely(!skb)) return -ENOBUFS; }

之后正是开始构建 TCP 的头部。首先判断该 TCP 包是否是一个 SYN 包。如果是，则调用tcp_syn_options构建相应的选项。否则，调用 tcp_established_options来构建相应的选项。注意，这里仅仅是计算出来具体的选项及其大小，并没有形成最终 TCP 包中选项的格式。

```matlab
inet = inet_sk(sk);
tp = tcp_sk(sk);
 tcb = TCP_SKB_CB(skb);
memset(&args, 0, sizeof(args));
if (unlikely(tcb->tcp_flags & TCPHDR_SYN))
    tcp_options_size = tcp_syn_options(sk, skb, &args, &md5);
else
    tcp_options_size = tcp_established_options(sk, skb, &args, &md5); 
```

根据选项的大小，可以进一步推算出 TCP 头部的大小。之后，调用相关函数在 skb 头部为 TCP 头部流出空间。

```c
tcp_header_size = tcp_options_size + sizeof(struct tcphdr);  
/* if no packet is in qdisc/device queue, then allow XPS to select  
* another queue. We can be called from tcp tsq_handler()  
* which holds one reference to sk_wmem_alloc.  
*  
* TBD: Ideally, in-flight pure ACK packets should not matter here.  
* One way to get this would be to set skb->truesize = 2 on them.  
*/  
skb->ooookay = sk_wmem_alloc_get(sk) < SKB_TRUESIZE(1); 
```

```c
skb.push(sk, tcp_header_size);   
skb_reset_transport_header(sk);   
skb_orphan(sk);   
skb->sk = sk;   
skb->destructor = skb_is.tcpPURE_ACK(sk)? sock_wfree: tcp_wfree;   
skb_set_hash_from_sk(sk, sk);   
atomic_add(sk->truesize, &sk->sk_wmem_alloc); 
```

然后，就进入到了真正构建 TCP 头部并计算校验和的时候了。

```c
/* Build TCP header and checksum it. */
th = tcp hdr(sk);
th->source = inet->inet_sport;
th->dest =INET->inet_dport;
th->seq = htonl(tp->seq);
th->ack_seq = htonl(tp->rcv_nxt);
*(((_be16 *)th) + 6) =htons((tcp_header_size >>2) << 12) | tcb->tcp_flags);
if (unlikely(tp->tcp_flags & TCPHDR_SYN)) {
/* RFC1323: The window in SYN & SYN/ACK segments
* is never scaled.
*/
th->window =htons(min(tp->rcv_wnd, 65535U));
} else {
th->window =htons(tp_select_window(sk));
}
th->check =0;
th->urg_ptr =0;
/* The erg_mode check is necessary during a below snd_una win probe */
if (unlikely(tp_ergic_mode(tp)&&before(tp->seq,tp->snd_up))){
if (before(tp->snd_up,tp->seq+0x10000)){ th->urg_ptr=htons(tp->snd_up-tcb->seq);
th->urg=1;
} else if(after(tp->seq+0xFFFF,tp->snd_nxt)){ th->urg_ptr=htons(0xFFFF);
th->urg=1;
}
}
/*在写完首部后，调用函数将TCP选项写入*/
tcp_options_write((_,be32*)(th+1),tp,&opts);
skb_shinfo(sk)->gso_type=sk->sk_gso_type;
if (likely((tp->tcp_flags & TCPHDR_SYN) ==0))
tcp_ecn_send(sk,skb,tp-header_size);
#ifdef CONFIG.tcp_MD5SIG
/* Calculate the MD5 hash, as we have all we need now */
if (md5){ sk_nocaps_add(sk,NETIF_F_GSO_MASK);
tp->afSpecific->calc_md5_hash(optionshash_location,
mk5,sk,skb);
} 
```

最后，总算是进入到了要将包发给 IP 层的地方了。

icsk->icsk_avOps->send_check(sk, skb);   
/*触发相关的TCP事件，这些会被用于拥塞控制算法。*/ if (likely(tpb->tcp_flags&TCPHDR_ACK)) tcp_event ACK_sent(sk, tcp_skb_pcount(sk)); if (skb->len != tcp_header_size) tcp_event_data_sent(tp, sk); if(after(tpb->end_seq,tp->snd_nxt)||tcb->seq==tcb->end_seq) TCP_ADD_STAT(Sock_net(sk),TCP_MIB_OUTSEGS, tcp_skb_pcount(sk)); tp->segs_out += tcp_skb_pcount(skb); /*OK,its time to fill skb_shinfo(sk)->gso{'segs/size}*/ skb_shinfo(sk)->gso_segs $\equiv$ tcp_skb_pcount(skb); skb_shinfo(sk)->gso_size $\equiv$ tcp_skb_mss(skb); /*Our usage of tstamp should remain private */ skb->tstamp.tv64 $= 0$ .   
/*Cleanup our debris for IP stacks*/ memset(skb->cb,0,max(sizeof(struct inlet_skb parm), sizeof(struct inlet6_skb parm));   
/*在这里，我们将包加入到IP层的发送队列*/ err $\equiv$ icsk->icsk_avOps->queue_xmit(sk, skb, &inet->cork.fl); if (likely(err <= 0)) return err;   
/*如果发送了丢包（如被主动队列管理丢弃)，那么进入到拥塞控制状态。*/ tcp-enter_cwr(sk); return net_xmit_eval(err);

# 3.2.3 tcp_select_window(struct sk_buff *skb)

这个函数的作用是选择一个新的窗口大小以用于更新tcp_sock。返回的结果根据RFC1323（详见1.3.2）进行了缩放。

static u16 tcp_select_window(struct sock \*sk)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); u32 old_win $=$ tp->rcv_wnd; u32 cur_win $=$ tcp_receive_window(tp); u32 new_win $=$ _tcp_select_window(sk); /\*old_win是接收方窗口的大小。 \*cur_win当前的接收窗口大小。 \*new_win是新选择出来的窗口大小。 \*/   
/\*当新窗口的大小小于当前窗口的大小时，不能缩减窗口大小。 \*这是IEEE强烈不建议的一种行为。 \*/ if (new_win < cur_win）{

/*Danger Will Robinson! *Don't update rcu_wup/rcu_umd here or else *we will not be able to advertise a zero *window in time. --DaveM * *Relax Will Robinson. */ if(new_win $= = 0$ ) NET_INC_STATSSock_net(sk), LINUX_MIB_TCPWANTZEROWINDOWADV); /*当计算出来的新窗口小于当前窗口时，将新窗口设置为大于cur_win *的1<<tp->rx_opt.rcv wscale的整数倍。 */ new_win $\equiv$ ALIGN(cur_win,1<<tp->rx_opt.rcv wscale);   
} /*将当前的接收窗口设置为新的窗口大小。*/ tp->rcv_wnd $\equiv$ new_win; tp->rcv_wup $\equiv$ tp->rcv_nxt;   
/*判断当前窗口未越界。*/ if(!tp->rx_opt.rcv wscale &&sysctl tcp_workaround_signed_windows) new_win $\equiv$ min(new_win, MAX_TAC窗户); else new_win $\equiv$ min(new_win,(65535U <<tp->rx_opt.rcv wscale));   
/*RFC1323缩放窗口大小。这里之所以是右移，是因为此时的new_win是 *窗口的真正大小。所以返回时需要返回正常的可以放在16位整型中的窗口大小。 *所以需要右移。 */ new_win $> = >$ tp->rx_opt.rcv wscale;   
/*If we advertise zero window, disable fast path.*/ if(new_win $= = 0$ { tp->pred_flags $\equiv$ 0; if(old_win) NET_INC_STATSSock_net(sk), LINUX_MIB_TCPTOZEROWINDOWADV); }else if(old_win $= = 0$ { NET_INC_STATSSock_net(sk), LINUX_MIB_TCPFROMZEROWINDOWADV);   
} return new_win;

在这个过程中，还调用了__tcp_select_window(sk)来计算新的窗口大小。该函数会尝试增加窗口的大小，但是有两个限制条件：

1. 窗口不能收缩 (RFC793)  
2. 每个 socket 所能使用的内存是有限制的。

RFC 1122 中说：

```python
"the suggested [SWS] avoidance algorithm for the receiver is to keep RECV.NEXT + RCV.WIN fixed until: RCV.BUFF - RCVUSER - RCV.WINDOW >= min(1/2 RCV.BUFF, MSS)" 
```

推荐的用于接收方的糊涂窗口综合症的避免算法是保持 recv.next+rcv.win

不变，直到：RCV.BUFF - RCV.USER - RCV.WINDOW $> =$ min(1/2 RCV.BUFF,MSS)

换句话说，就是除非缓存的大小多出来至少一个 MSS 那么多字节，否则不要增长窗口右边界的大小。

然而，根据 Linux 注释中的说法，被推荐的这个算法会破坏头预测 (header predic-tion)，因为头预测会假定th->window不变。严格地说，保持th->window固定不变会违背接收方的用于防止糊涂窗口综合症的准则。在这种规则下，一个单字节的包的流会引发窗口的右边界总是提前一个字节。当然，如果发送方实现了预防糊涂窗口综合症的方法，那么就不会出现问题。

Linux 的 TCP 部分的作者们参考了 BSD 的实现方法。BSD 在这方面的做法是是，如果空闲空间小于最大可用空间的 $\textstyle { \frac { 1 } { 4 } }$ ，且空闲空间小于 mss 的 $\begin{array} { l } { { \frac { 1 } { 2 } } } \end{array}$ ，那么就把窗口设置为0。否则，只是单纯地阻止窗口缩小，或者阻止窗口大于最大可表示的范围 (the largestrepresentable value)。BSD 的方法似乎“意外地”使得窗口基本上都是 MSS 的整倍数。且很多情况下窗口大小都是固定不变的。因此，Linux 采用强制窗口为 MSS 的整倍数，以获得相似的行为。

u32__tcp_select_window(struct sock \*sk)   
{ struct inlet_connection_sock \*icsk $=$ inlet_csk(sk); struct tcp SOCK \*tp $=$ tcp_sk(sk); int mss $=$ icsk->icsk_ACK_rcv_mss; int free_space $=$ tcp_space(sk); int allowed_space $=$ tcp_full_space(sk); int full_space $=$ min_t(int,tp->window_clamp,allowed_space); int window;   
/\*如果mss超过了总共的空间大小，那么把mss限制在允许的空间范围内。\*/ if(mss>full_space) mss $=$ full_space;   
if(free_space<（full_space>>1））{ /\*当空闲空间小于允许空间的一半时。\*/ icsk->icsk_ackquick $= 0$ ： if (tcp_under_memory_pressure(sk)) tp->rcv_ssthresh $=$ min(tp->rcv_ssthresh, 4U $\ast$ tp->advmss); /\*free_space有可能成为新的窗口的大小，因此，需要考虑 \*窗口扩展的影响。 \*/ free_space $=$ round_down(free_space,1<<tp->rx_opt.rcv_wscale); /\*如果空闲空间小于mss的大小，或者低于最大允许空间的的1/16，那么， \*返回O窗口。否则，tcp_clamp_window()会增长接收缓存到tcp_rmem[2]。 \*新进入的数据会由于内酷限制而被丢弃。对于较大的窗口，单纯地探测mss的 \*大小以宣告O窗口有些太晚了（可能会超过限制）。 \*/ if(free_space<（allowed_space>>4）||free_space<mss) return 0;

} if(free_space $\rightharpoondown$ tp->rcv_ssthresh) free_space $=$ tp->rcv_ssthresh; /\*这里处理一个例外情况，就是如果开启了窗口缩放，那么就没法对齐mss了。 \*所以就保持窗口是对齐2的幂的。 \*/ window $=$ tp->rcv_wnd; if(tp->rx_opt.rcv_wscale){ window $=$ free_space; /\*Advertise enough space so that it won't get scaled away. \*Import case:prevent zero window announcement if \*1<<rcv_wscale $\rightharpoonup$ mss. \*/ if((window>>tp->rx_opt.rcv_wscale）<<tp->rx_opt.rcv_wscale）！=window) window $=$ (((window>>tp->rx_opt.rcv_wscale）+1) $\ll$ tp->rx_opt.rcv_wscale); }else{ /\*如果内存条件允许，那么就把窗口设置为mss的整倍数。\*或者如果free_space>当前窗口大小加上全部允许的空间的一半， \*那么，就将窗口大小设置为free_space \*/ ifwindow $\Leftarrow$ free_space-mss||window $\rightharpoondown$ free_space) window $=$ (free_space/mss)*mss; elseif(mss $= =$ full_space&& free_space $>$ window+(full_space>>1)) window $=$ free_space; } return window;

# CHAPTER 4

TCP 输入

# Contents

4.1 Linux 内核网络数据接收流程概览 . . . . 58  
4.2 自底向上调用与自顶向下调用 59

4.2.1 自底向上处理 . . 59

4.2.1.1 tcp_v4_rcv 59   
4.2.1.2 tcp_prequeue . . 64

4.2.2 自顶向下处理 . 65

4.2.2.1 tcp_recvmsg . . . 65

# 4.1 Linux 内核网络数据接收流程概览

正如上图所示，Linux 内核在接收网络数据时分为以上 5 个层次。每个层次都有自己的功能，相互依赖，同时又独立成块。下面，我们来依次介绍这些层次。

应用层 对于应用层来说，提供多种接口来接收数据，包括 read,recv,recvfrom.

BSD Socket 层 这一层主要是在应用层进行进一步的处理。

INET Socket 层 当被 IP 层的函数调用的时候，如果发现用户进程因正在操作传输控制块而将其锁定就会将未处理的 TCP 段添加到后备队列中，而一旦用户进程解锁传输控制块，就会立即处理后备队列，将 TCP 段处理之后的直接添加到接收队列中。当被BSD Socket 层的函数调用的时候，此时说明用户此时需要数据，那么我们就利用tcp_recvmsg 来进行消息接收，当然，起初得判断队列是否为空，不为空才可以直接处理，为空就得进一步进行相关的处理。

IP 层 IP 层则主要是在系统调度的时候，根据数据接收队列中是否由数据进行进一步的检验，判断该包是不是发往该主机。然后进行数据包的重组 (这是因为 TCP 数据有可能因为太大而被分片)，然后调用 INET 层的函数进行进一步的处理。

硬件层 这一部分主要是网卡接收到数据之后，进行发出请求中断，然后调用相关函数进行处理，将接收到的数据放入到数据接收队列中。当然，这时，那些没有经过 FCS检验过的数据包这时候已经被抛弃了。

而在本文的分析中，我们主要关注的是 TCP 层的实现，故而我们主要分析 INETSocket 层。

TCP 传输控制块主要有三个关于接收的队列，接收队列，prequeue 队列和后备队列。

当启用 tcp_low_latency(参见非核心部分讲解) 时，TCP 传输控制块在软中断中接收并处理 TCP 段，然后将其插入到接收队列中，等待用户进程从接收队列中获取 TCP段后复制到用户空间中，最终删除并释放。

而不启用 tcp_low_latency 这个选项的时候，则可以提高 tcp 协议栈的吞吐量及反应速度。TCP 传输控制块在软中断中将 TCP 段添加到 prequeue 队列中，然后立即处理 prequeue 队列中的段。如果用户进程正在读取数据，则可以直接复制数据到用户空间的缓存区中，否则添加到接收队列中，然后从软中断返回。在多数情况下，有机会处理 prequeue 队列中的段，但只有当用户进程在进行 recv 类系统调用之前，才在软中断中复制数据到用户空间的缓存区。

当然，在用户进程因操作传输控制块而将其锁定时，无论是否启用 tcp_low_latency，都会将未处理的 TCP 段添加到后备队列中，一旦用户进程解锁传输控制块，就会立即处理后备队列，将 TCP 段处理之后添加到接收队列中。

# 4.2 自底向上调用与自顶向下调用

# 4.2.1 自底向上处理

# 4.2.1.1 tcp_v4_rcv

正如上面的流程图所展示的样子，当 IP 层接收到报文，或将多个分片组装成一个完整的 IP 数据报之后，会调用传输层的接收函数，传递给传输层处理。

```c
1 /* Location:   
2   
3 net/ipv4/tcp_ipv4.c   
4   
5   
6 Function:   
7   
8 TCP接收数据的入口。   
9   
10 Parameter:   
11 skb：从IP层传递过来的数据报。   
12   
13 \*/   
14 int tcp_v4_rcv(struct sk BUFF\*skb)   
15 { const struct iphdr \*iph;
```

```c
const struct tcphdr *th;  
struct sock *sk;  
int ret;  
struct net *net = dev_net(sk->dev);  
/*如果不是发往本机的就直接丢弃*/  
if (skb->pkt_type != PACKET_HOST)  
goto discard_it;  
/* Count it even if it's bad */  
TCP_INC_STATISTICS_BH(net, TCP_MIB_INSEGS);  
/* 如果TCP段在传输过程中被分片了，则到达本地后会在IP层重新组装。组装完成后，报文分片都存储在链表中。在此，需把存储在分片中的报文复制到SKB的线性存储区域中。如果发生异常，则丢弃该报文。*/  
if (!pskb可能會Pull(skb, sizeof(struct tcphdr)))  
goto discard_it;
```

关于 pskb_may_pull 更多的内容，请参见8.1.6.

```c
/* 得到相关报文的头部 */
th = tcp hdr(skb);
/* 如果 TCP 的首部长度小于不带数据的 TCP 的首部长度, 则说明 TCP 数据异常。统计相关信息后, 丢弃。
*/
if (th->doff < sizeof(struct tcphdr) / 4)
    goto bad_packet;
/* 检测整个 TCP 首部长度是否正常, 如有异常, 则丢弃。
*/
if (!pskb可能會Pull(skb, th->doff * 4))
    goto discard_it;
```

关于 pskb_may_pull 更多的内容，请参见8.1.6.

/\*An explanation is required here, I think. \* Packet length and doff are validated by header prediction, \* provided case of th->doff $= = 0$ is eliminated. \* So, we defer the checks. \*/ /\* 验证TCP首部中的校验和，如校验和有误，则说明报文已损坏，统计相关信息后丢弃。 $\text{水}$ if (skb_checksum_init(skb，IPPROTO.tcp，inet_compute_pseudo)) goto csum_error;

接下来，根据 TCP 首部中的信息来设置 TCP 控制块中的值，因为 TCP 首部中的值都是网络字节序的，为了便于后续处理直接访问 TCP 首部字段，在此将这些值转换为本机字节序后存储在 TCP 的私有控制块中。

$\begin{array}{rl} & {\mathrm{th} = \mathrm{tcp\_hdr}(\mathrm{skb});}\\ & {\mathrm{iph} = \mathrm{ip\_hdr}(\mathrm{skb});}\\ & {/*\mathrm{This~is~tricky:~We~move~IPCB~at~its~correct~location~into~TCP\_SKB\_CB()}}\\ & {\mathrm{*~barrier()~makes~sure~compiler~wont~play~fool~Waliasing~games.}}\\ & {\mathrm{*/}}\\ & {\mathrm{memmove(&TCP\_SKB\_CB(skb) ->header.h4,~IPCB(skb),}}\\ & {\mathrm{sizeof(struct~inet\_skb\_param));}}\\ & {\mathrm{/*}\mathrm{禁止编译器或底层做优化} * /} \end{array}$

```c
barrier();
TCP_SKB_CB(skb) -> seq = ntohl(th->seq); //段开始序号
TCP_SKB_CB(skb) -> end_seq = (TCP_SKB_CB(skb) -> seq + th->syn + th->fin + skb->len - th->doff * 4); // 
TCP_SKB_CB(skb) -> ack_seq = ntohl(th->ack_seq); //下一个等待发送的字节的序号
TCP_SKB_CB(skb) -> tcp_flags = tcp_flag_byte(th);
TCP_SKB_CB(skb) -> tcp_tw_isn = 0;
TCP_SKB_CB(skb) -> ip_dsfield = ipv4_get_dsfield(iph);
TCP_SKB_CB(skb) -> sacked = 0; 
```

接下来调用__inet_lookup在 ehash 或 bhash 散列表中根据地址和端口来查找传输控制块。如果在 ehash 中找到，则表示已经经历了三次握手并且已建立了连接，可以进行正常的通信。如果在 bhash 中找到，则表示已经绑定已经绑定了端口，处于侦听状态。如果在两个散列表中都查找不到，说明此时对应的传输控制块还没有创建，跳转到no_tcp_socket 处处理。

1 lookup: sk $=$ _inet_lookup_skb(&tcp_hashinfo,skb，th->source，th->dest); if(!sk) goto no tcp socket;

接下来继续处理。

```c
process: //TIME_WAIT 状态，主要处理释放连接 if (sk->sk_state == TCP_TIME_WAIT) goto do_time_wait; //NEW_SYN_RECV 状态，??? if (sk->sk_state == TCP_New_SYN_RECV) { struct request_sock *req = inlet_reqsk(sk); struct sock *nsk; sk = req->rsk Listener; if (unlikely(tp_v4_inbound_md5_hash(sk, skb))) { reqsk_put(req); goto discard_it; } if (unlikely(sk->sk_state != TCP_LISTEN)) { inlet_csk_reqsk_queue_drop_and_put(sk, req); goto lookup; } sock_hold(sk); nsk = tcp_check_req(sk, skb, req, false); if (!nsk) { reqsk_putreq); goto discard_and_relse; } if (nsk == sk) { reqskPutreq); } else if (tcp_child_process(sk, nsk, skb)) { tcp_v4_send_reset(nsk, skb); goto discard_and_relse; } else { sock_put(sk); return 0; } 
```

继续处理。  
```c
}  
/*ttl小于给定的最小的ttl*/  
if (unlikelyIPH->ttl < inet_sk(sk) ->min ttl)) {  
    NET_INC_STATISTICS_BH(net, LINUX_MIB_TCPMINTTLDROP);  
    goto discard_and_rese;  
} 
```

```c
//查找IPsec数据库，如果查找失败，进行相应处理.  
if(!xfrm4_policy_check(sk, XFRM_POLICY_IN, skb))  
goto discard_and_relse;  
//md5相关  
if(tpv4_inbound_md5_hash(sk, skb))  
goto discard_and_relse;  
nf_reset(skb);  
//过滤器  
if(sk_filter(sk, skb))  
goto discard_and_relse;  
/*设置SKB的dev为NULL，这是因为现在已经到了传输层，此时dev已经不再具有意义。*/  
sk->dev = NULL;  
/*LISTEN状态*/  
if(sk->sk_state == TCP_LISTEN) {  
ret = tcp_v4_do_rcv(sk, skb);  
goto put_and_return;  
}  
skincoming_cpu_update(sk);  
/*在接收TCP段之前，需要对传输控制块加锁，以同步对传输控制块接收队列的访问。*/  
bh_lock_sock_nested(sk);  
tcp_sk(sk)->segs_in += max_t(u16, 1, skb_shinfo(skb) ->gso_segs);  
ret = 0;  
/*进程此时没有访问传输控制块*/  
if(!sock-owned_by_user(sk)) {  
/*prequeue可读取*/  
if(!tcp_prequeue(sk, skb))  
ret = tcp_v4_do_rcv(sk, skb);  
} else if (unlikely(sk_add_backward(sk, skb, sk->sk_rcvbuf + sk->sk_sndbuf))) {  
/*添加到后备队列成功*/  
bh_unlock_sock(sk);  
NET_INC_STATISTICS_BH(net, LINUX_MIB.tcpBACKLOGDROP);  
goto discard_and_relse;  
}  
//解锁。  
bh_unlock_sock(sk);
```

```javascript
put_and_return: /\*递减引用计数，当计数为0的时候，使用sk\free释放传输控制块\*/ sock_put(sk);
```

```c
return ret;   
\*/ 处理没有创建传输控制块收到报文，校验错误，坏包的情况，给对端发送RST报文。   
\*/   
no tcp socket: /\*查找IPsec数据库，如果查找失败，进行相应处理.\*/ if(!xfrm4_policy_check(NULL,XFRM_POLICY_IN,skb)) goto discard_it; if（tcp_checksumcomplete(skb））{ csum_error: TCP_INC_STATSHBH(net,TCP_MIB_CSUMERRORS); badPacket: TCP_INC_STATSHBH(net,TCP_MIB_INERRS); }else{ tcp_v4_send_reset(NULL,skb); 1   
\*/ 丢弃数据包。   
\*/ discard_it: /\*Discard frame.\*/ kfree_skb(skb); return 0;   
discard_and_relse: sock_put(sk); goto discard_it;   
\*/ 处理TIME_WAIT状态\*/   
do_time_wait: if(!xfrm4_policy_check(NULL,XFRMPOLICY_IN,skb)){ /\*inet_timeout_sock减少引用计数\*/ inlet_twskPut(inet_twsk(sk)); goto discard_it; } if（tcp_checksumcomplete(skb)）{ inlet_twskPut(inet_twsk(sk)); goto csum_error;
```

关于 inet_twsk_put 更多的内容，请参见8.2.4。

//根据返回值进行相应处理   
switch (tcp_timeout_state_process(inet_twsk(sk),skb，th)) { case TCP TW_SYN: { struct sock \*sk2 $=$ inet.lookup Listener(dev_net(sk->dev), &tcp_hashinfo, iph->saddr，th->source, iph->daddr，th->dest, inlet_iif(sk)); if (sk2){ inlet_twsk_deschedule_put(inet_twsk(sk)); sk $=$ sk2; goto process; } /* Fall through to ACK */   
}   
case TCP TW_ACK: tcp_v4_timer_waitAck(sk，skb);

```txt
break;   
case TCP_TW_RST: goto no.tcp_SOCKET;   
case TCP_TW_SUCCESS: 1   
}   
goto discard_it;   
} 
```

4.2.1.2 tcp_prequeue   
/\*   
Location:   
net/ipv4/tcp_ipv4.c   
Function:   
Packet is added to VJ-style prequeue for processing in process   
context(进程上下文)，if a reader task is waiting. Apparently, this exciting   
idea (VJ's mail "Re: query about TCP header on tcp-ip" of 07 Sep 93)   
failed somewhere. Latency? Burstiness? Well, at least now we will   
see, why it failed. 8)8) --ANK   
Parameter:   
sk:传输控制块   
skb：缓存区   
\*/   
bool tcp_prequeue(struct sock \*sk, struct sk BUFF \*skb)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); //如果开启了 tcp_low-latency 或者用户没有在读数据，直接返回 if (sysctl tcp_low-latency ||！tp->ucopy.task) return false; /\* 数据包长度不大于 tcp 头部的长度，并且 prequeue 队列为空。 \*/ if (skb->len <= tcp_hdrlen(sk) && skb_queue_len(&tp->ucopy.prequeue) == 0) return false; /\* Before escaping RCU protected region, we need to take care of skb \* dst.Prequeue is only enabled for established sockets. \* For such sockets, we might need the skb dst only to set sk->sk_rx.dst \* Instead of doing full sk_rx.dst validity here, let's perform \* an optimistic check. \*/ if (likely(sk->sk_rx.dst)) skb.dst_drop(sk); else skb.dst_force-safe(skb); /\* 将接收到的段添加到 prequeue 队列中，并更新 prequeue 队列消耗的内存。 \*/ _skb_queueTAIL(&tp->ucopy.prequeue, skb); tp->ucopy-memory += skb->truesize;

```c
/* 如果prequeue消耗的内存超过接收缓存上限，则立刻处理prequeue队列上的段。  
*/ if(tp->ucopy.memory > sk->sk_rcvbuf) { struct sk_buffer *skb1; BUG_ON(sock-owned_by_user(sk)); while ((skb1 = __skbdeque(&tp->ucopy.prequeue)) != NULL) { sk_backlog_rcv(sk, skb1); NET_INC_STATSHB(sock_net(sk), LINUX_MIB_TCPPPREQUEUEEDROPPED); } tp->ucopy-memory = 0; } else if (skb_queue_len(&tp->ucopy.prequeue) == 1) { //队列长度为1 wake_up_interruptible_sync polled(sk_sleep(sk), POLLIN | POLLRDNORM | POLLRBAND); //不需要发送ack，则复位重新启动延迟确认定时器。 if (!inet_csk ACKscheduled(sk)) inlet_csk_reset_xmit_timer(sk, ICSK_TIME_DACK, (3 * tcp_rto_min(sk)) / 4, TCP_RTO_MAX); } return true;
```

# 4.2.2 自顶向下处理

# 4.2.2.1 tcp_recvmsg

我们一步一步对该函数进行分析。

```c
/* Location: net/ipo4/tcp.c
Function:
* This routine copies from a sock struct into the user buffer.
* Technical note: in 2.3 we work on _locked_SOCKET, so that * tricks with *seq access order and skb->users are not required.
* Probably, code can be easily improved even more.
Parameter:
sk: 传输控制块。
msg:
len:
nonblock: 是否阻塞
flags: 读取数据的标志。
addrlen:
*/
int tcp_recvmsg(struct sock *sk, struct msghdr *msg, size_t len, int nonblock, 
```

```c
int flags, int *addr_len)  
{  
struct tcp_sock *tp = tcp_sk(sk);  
int copied = 0;  
u32 peek_seq;  
u32 *seq;  
unsigned long used;  
int err;  
int target; /* Read at least this many bytes */  
long timeo;  
struct task_struct *user_recv = NULL;  
struct sk BUFF *skb, *last;  
u32 urg_hole = 0;  
//如果只是为了接收来自套接字错误队列的错误，那就直接执行如下函数。  
if (unlikely(flags & MSG_ERRQUEUE))  
return inet_recv_error(sk, msg, len, addr_len);  
//不是特别理解，？？？？  
if (sk_can_busy_loop(sk) && skb_queue_empty(&sk->sk_receive_queue) && (sk->sk_state == TCPEstablishED))  
sk_busy_loop(sk, nonblock); 
```

上述主要处理一些比较异常的情况，接下来就开始进行正常的处理了。

```c
1 /* 在用户进程进行读取数据之前，必须对传输层进行加锁，这主要时为了在读的过程中，软中断操作传输层，从而造成数据的不同步甚至更为严重的不可预料的结果。  
2 lock_sck(sk);  
3 //初始化错误码，Transport endpoint is not connected  
4 err = -ENOTCONN;  
5 /* 如果此时只是处于 LISTEN 状态，表明尚未建立连接，此时不允许用户读取数据，只能返回。  
6 /* if (sk->sk_state == TCP_LISTEN) goto out;  
7 /* 获取阻塞读取的超时时间，如果进行非阻塞读取，则超时时间为 0。  
8 /* timeo = sock_rcvtimeo(sk, nonblock);  
9 /* Urgent data needs to be handled specially. 如果是要读取带外数据，则需要跳转到 recv Urg 进行处理。  
10 /* if (flags & MSG_OOB) goto recv Urg;
```

继续处理。

```c
1 //被修复了  
2 if (unlikely(tp->repair)) {  
3 //Operation not permitted  
4 err = -EPERM;  
5 //如果只是查看数据的话，就直接跳转到 out 处理  
6 if (!flags & MSG_PEEK))  
7 goto out;  
8 //????  
9 if(tp->repair_queue == TCP_SENDQueue)  
10 goto recv_sndq; 
```

```c
//Invalid argument
err = -EINVALID
if (tp->repair_queue == TCP_NO_queue)
goto out;
/* 'common' recv queue MSG_PEEK-ing */
} 
```

接下来进行数据复制。在把数据从接收缓存复制到用户空间的过程中，会更新当前已复制位置，以及段序号。如果接收数据，则会更新 copied_seq, 但是如果只是查看数据而并不是从系统缓冲区移走数据，那么不能更新 copied_seq。因此，数据复制到用户空间的过程中，区别接收数据还是查看数据是根据是否更新 copied_seq，所以这里时根据接收数据还是查看来获取要更新标记的地址，而后面的复制操作就完全不关心时接收还是查看。

最后一行调用相关函数根据是否设置了 MSG_WAITALL 来确定本次调用需要接收数据的长度。如果设置了 MSG_WAITALL 标志，则读取数据长度为用户调用时输入的参数 len。

```c
//被修复了???啥意思》？》  
if (unlikely(tp->repair)) { //Operation not permitted err = -EPERM; //如果只是查看数据的话，就直接跳转到out处理 if(!flags&MSG_PEEK)) goto out; //????? if(tp->repair_queue == TCP_SENDQueue) goto recv_sndq; //Invalid argument err = -EINVALID if(tp->repair_queue == TCP_NOQueue) goto out; /*'common'recv_queue MSG_PEEK-ing*/ } 
```

接下来进行数据复制。在把数据从接收缓存复制到用户空间的过程中，会更新当前已复制为止，以及段序号。如果接收数据，则会更新 copied_seq, 但是如果只是查看数据而并不是从系统缓冲区移走数据，那么不能更新 copied_seq。因此，数据复制到用户空间的过程中，区别接收数据还是查看数据是根据是否更新 copied_seq，所以这里时根据接收数据还是查看来获取要更新标记的地址，而后面的复制操作就完全不关心时接收还是查看。

最后一行调用相关函数根据是否设置了 MSG_WAITALL 来确定本次调用需要接收数据的长度。如果设置了 MSG_WAITALL 标志，则读取数据长度为用户调用时输入的参数 len。

1 seq $=$ &tp->copied_seq;   
2 if flags&MSG_PEEK){   
3 peek_seq $\equiv$ tp->copied_seq;   
4 seq $=$ &peek_seq;   
5 }

```txt
target = sock_rcvowat(sk, flags & MSG_WAITALL, len); 
```

接下来通过 urg_data 和 urg_seq 来检测当前是否读取到带外数据。如果在读带外数据，则终止本次正常数据的读取。否则，如果用户进程有信号待处理，则也终止本次的读取。

do { u32 offset; /\*Are we at urgent data? Stop if we have read anything or have SIGURG pending. \*/ if(tp->urg_data&&tp->urg_seq $= =$ \*seq）{ if (copied) break; if (signal_pending(current)){ copied $=$ timeo？sock_intr_errno(timeo）：-EAGAIN; break; }   
}

接下来首先获取一个缓存区。

```c
/* Next get a buffer. */
//获取下一个要读取的的段。
last = skbpeekTAIL(&sk->sk_receive_queue);
skb_queue_walk(&sk->sk_receive_queue, skb) {
    last = skb;
    /* Now that we have two receive queues this
        * shouldn't happen.
            如果接收队列中的段序号比较大，则说明也获取不到下一个待获取的段，
            这样也只能接着处理 prequeue 或后备队列，实际上这种情况不应该发生。
        */
    if (WARN(before(*seq, TCP_SKB_CB(skb)->seq),
            "recvmsg bug: copied %X seq %X rcvnext %X fl %X\n",
            *seq, TCP_SKB_CB(skb)->seq, tp->rcv_nxt,
            flags))
            break;
    */
    /* 到此，我们已经获取了下一个要读取的数据段，计算该段开始读取数据的偏移位置，
            当然，该偏移值必须在该段的数据长度范围内才有效。*/
    offset = *seq - TCP_SKB_CB(skb)->seq;
    */
    /* 由于 SYN 标志占用了一个序号，因此如果存在 SYN 标志，则需要调整
            偏移。由于偏移 offset 为无符号整型，因此，不会出现负数的情况。
    */
    if (TCP_SKB_CB(skb)->tcp_flags & TCPHDR_SYN)
        offset--;
    */
    /* 只有当偏移在该段的数据长度范围内，才说明待读的段才是有效的，因此，接下来
            跳转到 found.ok_skb 标签处读取数据。
    */
    if (offset < skb->len)
        goto found.ok_skb;
    */
    /* 如果接收到的段中有 FIN 标识，则跳转到 found_fin.ok 处处理。
    */
    if (TCP_SKB_CB(skb)->tcp_flags & TCPHDR_FIN)
```

37 goto found_fin.ok;   
38WARN(!flags&MSG_PEEK),   
39 "recvmsg bug 2: copied $\% X$ seq $\% X$ rcvnext $\% X$ fl $\% X\backslash n"$ 40 \*seq，TCP_SKB_CB(skb)->seq，tp->rcv_nxt，flags);   
41 }

只有在读取完数据后，才能在后备队列不为空的情况下区处理接收到后备队列中的TCP 段。否则终止本次读取。

由于是因为用户进程对传输控制块进行的锁定，将 TCP 段缓存到后备队列，故而，一旦用户进程释放传输控制块就应该立即处理后备队列。处理后备队列直接在 re-lease_sock 中实现，以确保在任何时候解锁传输控制块时能立即处理后备队列。

```c
1 /* Well, if we have backlog, try to process it now yet. */
2
3 if (copied >= target && !sk->sk_backlog.tail)
4 break; 
```

当接收队列中可以读取的段已经读完，在处理 prequeue 或后备队列之前，我们需要先检查是否会存在一些异常的情况。如果存在这类情况，就需要结束这次读取，返回前当然还顺便检测后备队列是否存在数据，如果有则还需要处理。

1 if (copied) {  
2 /*  
3 检测条件：  
4 1.有错误发生  
5 2.TCP处于CLOSE状态  
6 3.shutdown状态  
7 4.收到信号  
8 5.只是查看数据  
9 */  
10 if $(\mathrm{sk->sk\_err}||$ sk->sk_state $= =$ TCP_CLOSE ||(sk->sk_shutdown&RCV_SHUTDOWN)||！timeo||signal_pending(current))break;  
11 }else{  
17 /*检测TCP会话是否即将终结*/if(sock_flag(sk，SOCK_DONE))break;  
19 /*如果有错误，返回错误码*/if $(\mathrm{sk->sk\_err})$ {copied $=$ sock_error(sk);break;  
20 }/*如果是shutdown，返回*/if $(\mathrm{sk->sk\_shutdown}\& \mathrm{RCV\_SHUTDOWN})$ break;  
21   
22   
23 }  
24   
25 /*如果是shutdown，返回*/if $(\mathrm{sk->sk\_shutdown}\& \mathrm{RCV\_SHUTDOWN})$ break;  
26   
27   
28   
29 如果TCP状态为CLOSE，而套接口不再终结状态，则进程可能在读取一个从未建立连接的套接口，因此，返回相应的错误码。  
30   
31 */if $(\mathrm{sk->sk\_state} == \mathrm{TCP\_CLOSE})$ {if(!sock_flag(sk，SOCK_DONE)){/\*Thisoccurswhenusertries toread\*from never connected socket.

进一步处理。  
\*/ copied $=$ -ENOTCONN; break; } break; } /\* 未读到数据，且是非阻塞读取，则返回错误码Try again。 \*/ if(!timeo){ copied $=$ -EAGAIN; break; } /\* 检测是否收到数据，并返回相应的错误码\*/ if(signal_pending(current)){ copied $=$ sock_intr_errno(timeo); break; }

```javascript
//检测是否有确认需要立即发送  
tcp_cleanup_rbuf(sk, copied);  
/*在未启用慢速路径的情况下，都会到此检测是否需要处理prepare队列。  
*/if(!sysctl tcp_low latency &&tp->ucopy.task == user_recv){/\*Install new reader如果此次是第一次检测处理prepare队列的话，则需要设置正在读取的进程标识符、缓存地址信息，这样当读取进程进入睡眠后，ESTABLISHED状态的接收处理就可能把数据复制到用户空间。\*/if(!user_recv&&!(flags&(MSG_TRUNC|MSG_PEEK))){user_recv = current;tp->ucopy.task = user_recv;tp->ucopy.msg = msg;}//更新当前可以使用的用户缓存的大小。tp->ucopy.len = len;//进行报警信息处理。WARN_ON(tp->copied_seq != tp->rcv_nxt &&！(flags& (MSG_PEEK|MSG_TRUNC));/\*Ugly...Ifprequeueisnotempty，wehave to\*processitbeforereleasingsocket,otherwise\*orderwill be broken at second iteration.\*More elegant solution is required!!!\*\*Look:we have the following(pseudo) queues:\*1.packets in flight\*2.Backlog\*3.prequeue\*4.receive_queue\*Each queue can be processed only if the next ones\*are empty.At this point we have empty receive_queue. 
```

```txt
38 *But prequeue _can_ be not empty after 2nd iteration,   
39 *when we jumped to start of loop because backlog   
40 * processing added something to receive_queue.   
41 *We cannot release_sock(),because backlog contains   
42 * packets arrived _after_ prequeued ones.   
43 *   
44 *Shortly,algorithm is clear --- to process all   
45 * the queues in order.We could make it more directly,   
46 * requeueing packets from backlog to prequeue,if   
47 *is not empty.It is more elegant,but eats cycles,   
48 * unfortunately.   
49 \*/   
50 /\*   
51 如果prequeue队列不为空，则跳转到do\orequeue标签处处理prequeue队列。   
52 \*/   
53 if(!skb_queue_empty(&tp->ucopy.prequeue)) goto do_prequeue;   
55   
56 /\*Set realtime policy in scheduler \*/   
57 
```

继续处理, 如果读取完数据，则调用 release_sock 来解锁传输控制块，主要用来处理后备队列，完成后再调用 lock_sock 锁定传输控制块。在调用 release_sock 的时候，进程有可能会出现休眠。

如果数据尚未读取，且是阻塞读取，则进入睡眠等待接收数据。这种情况下，tcp_v4_do_rcv处理 TCP 段时可能会把数据直接复制到用户空间。

```txt
1 if (copied >= target) { /* Do not sleep, just process backlog. */ release_sock(sk); lock_sock(sk); } else { sk_wait_data(sk, &timeo, last); } 
```

if (user_recv) { int chunk; /\*__Restore normal policy in scheduler \*/ /\* 更新剩余用户空间长度以及已经复制到用户空间的数据长度 \*/ chunk $=$ len -tp->ucopy.len; if（chunk！ $= 0$ ）{ NET_ADD_STATSD_USER(sock_net(sk),LINUX_MIB_TCPDIRECTCOPYFROMBACKLOG，chunk); len $\equiv$ chunk; copied $+ =$ chunk; } /\* 如果接收到接收队列中的数据已经全部复制到用户进程空间， 但prequeue队列不为空，则需要继续处理prequeue队列，并 更新剩余的用户空间的长度和已经复制到用户空间的数据长度。 \*/ if(tp->rcv_nxt $= =$ tp->copied_seq&& !skb_queue_empty(&tp->ucopy.prequeue)) { do_prequeue:

tcp_prequeue_process(sk); chunk $=$ len -tp->ucopy.len; if（chunk $! = 0$ ）{ NET_ADD_STATSD_USER(sock_net(sk)，LINUX_MIB_TCPDIRECTCOPYFROMPREQUEUE，chunk); len $= =$ chunk; copied $+ =$ chunk; }   
}

处理完 prequeue 队列后，如果有更新 copied_seq，且只是查看数据，则需要更新 peek_seq。然后继续获取下一个段进行处理。

```c
if ((flags & MSG_PEEK) && (peek_seq - copied - arg_hole != tp->copied_seq)) { net_dbg_ratelimited("TCP(%s:%d): Application bug, race in MSG_PEEK\n", current->comm, task.pid_NR(current)); peek_seq = tp->copied_seq; } continue; 
```

found.ok_skb: /\* Ok so how much can we use? \*/ /\* 获取该可读取段的数据长度，在前面的处理中 已由TCP序号得到本次读取数据在该段中的偏移。 \*/ used $=$ skb->len - offset; if(len $<  <$ used) used $=$ len; /\*Do we have urgent data here? 如果该段中包含带外数据，则获取带外数据在该段中的偏移。 如果偏移在该段可读的范围内，则表示带外数据有效。 进而 如果带外数据偏移为0，则说明目前需要的数据正是带外数据， 且带外数据不允许放入到正常的数据流中，即在普通的数据数据 流中接受带外数据，则需要调整读取正常数据流的一些参数，如已 读取数据的序号、正常数据的偏移等。最后，如果可读数据经过调 整之后为0，则说明没有数据可读，跳过本次读数据过程到skip_copy处 处理。 如果带外数据偏移不为0，则需要调整本次读取的正常长度直到读到带外 数据为止。 \*/ if(tp->urg_data){ u32urg_offset $=$ tp->urg_seq-\*seq; if(urg_offset $<  <$ used){ if(!urg_offset){ if(!sock_flag(sk,SOCK_URGINLINE)){ ++\*seq; urg_hole++; offset++; used--; if(!used) goto skip_copy; }

} else used $=$ urg_offset; }   
}

接下来处理读取数据的情况。

if(!flags&MSG_TRUNC）{/\*调用skb_copy_dataragram msg来将数据复制到用户空间并且根据返回的值来判断是否出现了错误。\*/err $=$ skb_copy_dataragram msg(sk，offset，msg，used);if(err）{//Exception.Bailout！\*/if(!copied)copied $\equiv$ -EFAULT;break;1}  
}/\*\*调整读正常数据流的一些参数，如已读取数据的序号、已读取数据的长度，剩余的可以使用的用户空间缓存大小。如果是截短，则通过调整这些参数，多余的数据就默默被丢弃了。\*/\*seq+=used;copied+=used;len-=used;/\*tcp_rcv_space_adjust 调整合理的TCP接收缓存的大小\*/tcp_rcv_space_adjust(sk);

```txt
skip_copy:  
/* 如果已经完成了对带外数据的处理，则将带外数据标志清零，设置首部预测标志，下一个接收到的段，就又可以通过首部预测执行快速路径还是慢速路径了。  
*/ if (tp->urg_data && after(tp->copied_seq, tp->urg_seq)) {tp->urg_data = 0;tcp_fast_path_check(sk);}  
/* 如果该段还有数据没有读取（如带外数据），则只能继续处理该段，而不能将该段从接收队列中删除。  
*/ if (used + offset < skb->len) continue;  
/* 如果发现段中存在 FIN 标志，则跳转到 found\_fin\_ok 标签处处理 */if (TCP_SKB_CB(skb)->tcp_flags & TCPHDR_FIN) goto found_fin.ok;  
/* 如果已经读完该段的全部数据，且不是查看数据，则可以将该段从接收队列中删除，然后继续处理后续的段。  
*/ if (!flags & MSG_PEEK)) sk_eat_skb(sk, skb); continue;
```

```c
由于FIN标志占一个序号，因此当前读取的序号需要递增。如果已经读完该段的全部数据并且不是查看数据，则可以将该段从接收队列中删除。然后就可以退出了，无需处理后续的段了。  
\*/  
found_fin.ok: /\*Process the FIN. \*/  
\*\*\*seq;  
if(!flags&MSG_PEEK))  
    sk_eat_skb(sk,skb);  
break;  
}while(len>0);
```

```c
if (user_recv) { //prequeue 队列非空 if (!skb_queue_empty(&tp->ucopy.prequeue)) { int chunk; tp->ucopy.len = copied > 0 ? len : 0; //处理 prequeue 队列。 tcp_prequeue_process(sk); /* 如果在处理 prequeue 队列的过程中又有一部分数据复制到用户空间，则需要调整剩余的可用用户空间缓存大小和已读取数据的序号。 */ if (copied > 0 && (chunk = len - tp->ucopy.len) != 0) { NET_ADD_STATISTICS_USER(sock_net(sk), LINUX_MIB_TCPDIRECTCOPYFROMPREQUEUE, chunk); len -= chunk; copied += chunk; } } /* 最后清零 ucopy.task 和 ucopy.len，表示用户当前没有读取数据。这样当处理 prequeue 队列时，不会将数据复制到用户空间，因为只有在未启用 tcp_low-latency 的情况下，用户进程主动读取时，才有机会将数据直接复制到用户空间。 */ tp->ucopy.task = NULL; tp->ucopy.len = 0; } end
```

在完成读取数据后，需再次检测是否有必要立即发送ACK段，并根据情况确定是否发送ACK段。在返回之前需解锁传输返回已经读取的字节数。

```txt
begin{minted}[linenos]{c} /\*According to UNIX98, msg_name.msg_namelen are ignored \* on connected socket. I was just happy when found this 8) --ANK \*/ /\* Clean up data we have read: This will do ACK frames. \*/ tcp_cleanup_rbuf(sk, copied); release SOCK(sk); return copied; 
```

接下来，进行一些其他情况的处理。

1 /* 如果在读取的过程中遇到 $l$ 错误，就会跳转到此，解锁传输层然后返回错误码。   
2   
3   
4 out: release_sock(sk); return err;   
5 /* 如果是接收带外数据，则调用 tcp_recv Urg 接收。   
6   
7 recv Urg: err $\equiv$ tcp_recv Urg(sk, msg, len, flags); goto out;   
10   
11 \*/ 这一步是干什么的呢？   
12   
13   
14   
15   
16 recv_sndq: err $\equiv$ tcppeek_sndq(sk, msg, len); goto out;   
17   
18   
19

![](images/a3d02110c89aaaf191d4bfb570bafcd9d750236faeb012172e6d0628857cd5fd.jpg)

# CHAPTER 5

# TCP 建立连接

# Contents

# 5.1 TCP 主动打开-客户 . . . 77

5.1.1 基本流程 77  
5.1.2 第一次握手：构造并发送 SYN 包 . . . . . . . 77

5.1.2.1 基本调用关系 . . . . . . 77  
5.1.2.2 tcp_v4_connect . . . 78   
5.1.2.3 tcp_connect . . 81

5.1.3 第二次握手：接收 $\mathrm { S Y N + A C K }$ 包 . . . . . . 82

5.1.3.1 基本调用关系 . . . . . 82  
5.1.3.2 tcp_rcv_state_process . . 82   
5.1.3.3 tcp_rcv_synsent_state_process . . . . 84   
5.1.3.4 tcp_ack . . . . 89

5.1.4 第三次握手: 发送 ACK 包 . . . . . 93

5.1.4.1 基本调用关系 . . . 93  
5.1.4.2 tcp_send_ack . . . 93

# 5.2 TCP 被动打开-服务器 . . . 94

5.2.1 基本流程 94  
5.2.2 第一次握手：接受 SYN 段 . 95

5.2.2.1 基本调用关系 . . . . . 95  
5.2.2.2 tcp_v4_do_rcv 95   
5.2.2.3 tcp_v4_cookie_check . . 96   
5.2.2.4 tcp_rcv_state_process . . . 97   
5.2.2.5 tcp_v4_conn_request && tcp_conn_request . . . . 99

5.2.3 第二次握手：发送 SYN+ACK 段 . . . . . 103

5.2.3.1 基本调用关系 . . . 103

5.2.3.2 tcp_v4_send_synack . . . . . 103   
5.2.3.3 tcp_make_synack . . . . 104

# 5.2.4 第三次握手：接收 ACK 段 . . . 107

5.2.4.1 基本调用关系 . . . . 107  
5.2.4.2 tcp_v4_do_rcv 108   
5.2.4.3 tcp_v4_cookie_check . . . . 109   
5.2.4.4 tcp_child_process . . 109   
5.2.4.5 tcp_rcv_state_process . . . . 110

# 5.1 TCP 主动打开-客户

# 5.1.1 基本流程

客户端的主动打开是通过 connect 系统调用等一系列操作来完成的，这一系统调用最终会调用传输层的tcp_v4_connect函数。基本就是客户端发送SYN段，接收SYN+ACK段，最后再次发送 ACK 段给服务器。

# 5.1.2 第一次握手：构造并发送 SYN 包

# 5.1.2.1 基本调用关系

![](images/de5f941ceb937f6113691ca629fe86ce0334c1ceb954123b89fb3cd2f7c3dff2.jpg)  
图 5.1: Client:Send SYN

# 5.1.2.2 tcp_v4_connect

tcp_v4_connect的主要作用是进行一系列的判断，初始化传输控制块并调用相关函数发送 SYN 包。

/\*   
Location:   
net/iju4/tcp_ipu4.c   
Function:   
这个函数会初始化一个的连接。   
Parameters:   
sk：传输控制块   
uaddr：通用地址结构，包含所属协议字段和相应的地址字段。 addr_len：目的地址长度   
\*/   
int tcp_v4_connect(struct sock \*sk, struct sockaddr \*uaddr, int addr_len)   
{ /\* sockaddr_in 结构体用于描述一个Internet（IP）套接字的地址 \*/ struct sockaddr_in \*usin $=$ （struct sockaddr_in \*)uaddr; structINET SOCK \*inet $=$ inlet_sk(sk); struct tcp_sock \*tp $=$ tcp_sk(sk); \*/ 网络协议主要使用大端存储， be16means16bitsstoredwithbig-endian be32,the same. \*/ _be16orig_sport,orig_dport; _be32daddr,nexthop; struct flowi4 \*fl4; struct rtable \*rt; int err; struct ip_options_rcu \*inet_opt;   
\/*检验目的地址长度是否有效\*/ if(addr_len $<  <$ sizeof(struct sockaddr_in)) return -EINVAL; //Invalidargument错误码为22   
\/*检验协议族是否正确\*/ if(usu->sin_family！ $= =$ AF_INET）//IPV4地址域 return-EAFNOSUPPORT; //Addressfamilynotsupportedbyprotocol   
\/*将下一跳地址和目的地址暂时设置为用户传入的IP地址\*/ nexthop $=$ daddr $=$ usin->sin_addr.s_addr; inlet_opt $=$ rcu_derefence_protected(inet->inet_opt, sock-owned_by_user(sk)); \/*如果选择源地址路由，则将下一跳地址设置为IP选项中的faddr-first hop address\*/ if(inet_opt&&inet_opt->opt.srr){ if(!daddr) return -EINVAL; nexthop $=$ inlet_opt->opt.faddr;

上面 rcu_dereference_protected 函数使用了 RCU 锁，RCU 锁的基本介绍参见9.6.1.

源地址路由是一种特殊的路由策略。一般路由都是通过目的地址来进行的。而有时也需要通过源地址来进行路由，例如在有多个网卡等情况下，可以根据源地址来决定走哪个网卡等等。

```c
orig_sport = inlet->inet_sport;  
orig_dport = usin->sin_port;  
fl4 = &inet->cork.f1.u.ip4; //对应于 ipu4 的流  
/* 获取目标的路由缓存项，如果路由查找命中，则生成一个相应的路由缓存项，这个缓存项不但可以用于当前待发送的 SYN 段，而且对后续的所有数据包都可以起到一个加速路由查找的作用。  
*/  
rt = ip-route_connect(f14, nexthop, inlet->inet_saddr, RT_CONN_FLAGS(sk), sk->sk_bound_dev_if, IPPROTO_TCP, orig_sport, orig_dport, sk);  
/* 判断指针是否有效 */  
if (IS_ERR(rt)) {  
    err = PTR_ERR(rt);  
    if (err == -ENETUNREACH) //Network is unreachable  
        IP_INC_STATISTICS(sock_net(sk), IPPSTATS_MIB_OUTNOROUTES);  
    return err;  
}
```

对于函数 IS_ERR、PTR_ERR 的具体介绍，请参见9.2.1.2.

/\*TCP不能使用类型为组播或多播的路由缓存项\*/ if（rt->rt_flags&（RTCF Multicast|RTCF_BROADCAST)）{ ip_rt_put(rt); return -ENETUNREACH; //Network is unreachable   
}   
/\*如果IP选项为空或者没有开启源路由功能，则采用查找到的缓存项\*/ if(!inet_opt||!inet_opt->opt.srr) daddr $=$ f14->daddr;   
/\*如果没有设置源地址，则设置为缓存项中的源地址\*/ if(!inet->inet_saddr) inlet->inet_saddr $=$ f14->saddr; sk_rcv_saddr_set(sk，inet->inet_saddr);   
/\* 如果该传输控制块的时间戳已被使用过，则重置各状态 rx_opt：tcp_options_received   
\*/ if(tp->rx_opt.tsrecent_stamp &&inet->inet_daddr $! =$ daddr){ /\*Reset inherited state\*/ /\*下一个待发送的TCP段中的时间戳回显值\*/ tp->rx_opt.tsrecent $\equiv 0$ /\*从接收到的段中取出时间戳\*/ tp->rx_opt.tsrecent_stamp $= 0$ /\*What does it means repair\*/ if(likely(!tp->repair)) tp->write_seq $\equiv 0$ }   
/\* 在启用了tw_recycle:time wait recycle的情况下，重设时间戳 它用来快速回收TIME_WAIT连接.   
\*/ if（tcp_death_row.sysct1_tw_recycle&&！tp->rx_opt.tsrecent_stamp &&f14->daddr==daddr) tcp_fetch_timewait_stamp(sk,&rt->dst);

/\*设置传输控制块\*/  
inet->inet_dport $\equiv$ usin->sin_port;  
sk_daddr_set(sk，daddr);  
inet_csk(sk)->icsk_ext_hdr_len $= 0$ ·  
if (inet_opt)  
    inlet_csk(sk)->icsk_ext_hdr_len $\equiv$ inlet_opt->opt optlen;  
/\*设置MSS大小\*/  
tp->rx_opt.mss_clamp $\equiv$ TCP_MSS_DEFAULT;  
/\*Socket identity is still unknown (sport may be zero).  
\*However we set state to SYN-SENT and not releasing socket  
\*lock select source port, enter ourselves into the hash tables and  
\*complete initialization after this.  
\*/  
\*/将TCP的状态设置为SYN_SENT\*/  
tcp_set_state(sk，TCP_SYN_SENT);  
err $\equiv$ inet_hash_connect(&tcp_death_row，sk);  
if(err)  
    goto failure;  
sk_set_txhash(sk);  
\*/如果源端口或者目的端口发生改变，则需要重新查找路由，并用新的路由缓存项更新sk中保存的路由缓存项。  
\*/  
rt $\equiv$ ip-route�新ports(fl4，rt，orig_sport，orig_dport，  
    inlet->inet_sport，inet->inet_dport，sk);  
if(IS_ERR(rt)){err $\equiv$ PTR_ERR(rt);rt $\equiv$ NULL;goto failure;}  
\*/将目的地址提交到套接字\*/  
sk->sk_gso_type $\equiv$ SKB_GSO_TCPV4;  
sk_setup_caps(sk，&rt->dst);  
\*/如果没有设置序号，则计算初始序号  
序号与双方的地址与端口号有关系\*/  
if(!tp->write_seq && likely(!tp->repair))tp->write_seq $\equiv$ secure tcp_sequence_number(inet->inet_saddr,inet->inet_daddr,inet->inet_sport,usin->sin_port);  
\*/计算IP首部的id域的值全局变量jiffies用来记录自系统启动以来产生的节拍的总数\*/  
inet->inet_id $\equiv$ tp->write_seq^jiffies;  
\*/调用tcp_connect构造并发送SYN包\*/  
err $\equiv$ tcp_connect(sk);  
rt $\equiv$ NULL;

```txt
98 if (err) 99 goto failure;   
100   
101 return 0; 
```

总结起来，tcp_v4_connect是在根据用户提供的目的地址，设置好了传输控制块，为传输做好准备。如果在这一过程中出现错误，则会跳到错误处理代码。

```c
1 failure:
2 /* 将状态设定为 TCP_CLOSE, 释放端口, 并返回错误值。
3 */
4 /* */
5 tcp_set_state(sk, TCP_CLOSE);
6 ip_rt_put(rt);
7 sk->sk-routeCaps = 0;
8 inlet->inet_dport = 0;
9 return err; 
```

# 5.1.2.3 tcp_connect

上面的tcp_v4_connect会进行一系列的判断，之后真正构造 SYN 包的部分被放在了tcp_connect中。接下来，我们来分析这个函数。

```c
/* Location: net/ipu4/tcp_output.c */
Function:
  该函数用于构造并发送 SYN 包。
Parameter:
  sk: 传输控制块。
*/
int tcp_connect(struct sock *sk)
{
    struct tcp SOCK *tp = tcp_sk(sk);
    struct sk BUFF *buff;
    int err;
    /* 初始化 tcp 连接 */
    tcp_connect_init(sk); 
```

关于 tcp_connect_init 更多的内容，请参见8.5.1.1。

```c
1 if (unlikely(tp->repair)) { //what does it mean repair?  
2 /* 如果 repair 位被置 1，那么结束 TCP 连接 */  
3 tcp_finish_connect(sk, NULL);  
4 return 0;  
5 }  
6  
7 /* 分配一个 sk_buffer */  
8 buff = sk_STREAM_alloc_skb(sk, 0, sk->sk_allocation, true);  
9 if (unlikely(!buffer))  
10 return -ENOUBFS; //No buffer space available 
```

```c
/* 初始化 skb，并自增 write_seq 的值 */
tcp_init_nodata_skb(buffer, tp->write_seq++, TCPHDR_SYN);
```

关于 tcp_init_nondata_skb 更多的内容，请参见8.5.1.2。

```c
/*设置时间戳*/
tp->retrans_stamp = tcp_time_stamp;
/*将当前的sk BUFF添加到发送队列中*/
tcp_connect_queue_skb(sk,buff);
/*ECN(Explicit Congestion Notification,)
支持显式拥塞控制
*/
tcp_ecn_send_syn(sk,buff); //what does this function do?
*/
发送SYN包，这里同时还考虑了Fast Open的情况，意思就是
可以在连接建立的时候同时发数据。
*/
err = tp->fastopen_req?tcp_send_syn_data(sk,buffer):
    tcp_transmit_skb(sk,buffer,1,sk->sk_allocation);
if(err == -ECONNREFUSED) /*Connection refused*/
    return err;
/*We change tp->snd_nat after the tcp_transmit_skb() call
*in order to make this packet get counted in tcpOutSegs.
*/
tp->snd_nxt = tp->write_seq; //send next下一个待发送字节的序列号
/*pushed_seq means Last pushed seq, required to talk to windows?*/
tp->pushed_seq = tp->write_seq;
TCP_INC_STATISTICS(sock_net(sk),TCP_MIB_ACTIVEOPENS);
/*设定超时重传定时器*/
inet_csk_reset_xmit_timer(sk,ICSK_TIME_RETRANS,
    inlet_csk(sk)->icsk_rto,TCP_RTO_MAX);
return 0; 
```

# 5.1.3 第二次握手：接收 $\mathbf { S } \mathbf { Y } \mathbf { N } { + } \mathbf { A } \mathbf { C } \mathbf { K }$ 包

# 5.1.3.1 基本调用关系

# 5.1.3.2 tcp_rcv_state_process

tcp_rcv_state_process实现了 TCP 状态机相对核心的一个部分。该函数可以处理除 ESTABLISHED 和 TIME_WAIT 状态以外的情况下的接收过程。这里，我们仅关心主动连接情况下的处理。在5.1.2.2中，我们分析源码时得出，客户端发送 SYN 包后，会将状态机设置为TCP_SYN_SENT状态。因此，我们仅在这里分析该状态下的代码。.

```txt
/\*   
Location:   
net/ipu4/tcp_input.c   
Function: 
```

![](images/8d62fce37ac2aaa7528dc48e82d6898e7dc1efea0d29643f833c4d0cf6f0ff29.jpg)  
图 5.2: Client:Receive SYN+ACK

This function implements the receiving procedure of RFC 793 for all states except ESTABLISHED and TIME_WAIT. It's called from both tcp_v4_rcv and tcp_v6_rcv and should be address independent.   
Paramater:   
sk:传输控制块。   
skb：缓存块。   
\*/   
int tcp_rcv_state_process(struct sock \*sk，struct sk_buffer \*skb)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); struct inlet_connection_sock \*icsk $=$ inlet_csk(sk); const struct tcphdr \*th $=$ tcp hdr(skb); struct request_sock \*req; int queued $= 0$ bool acceptable; /\*标识接收到的TCP段不存在TCP时间戳选项？?why\*/ tp->rx_opt.saw_tstamp $= 0$ .   
switch（sk->sk_state）{ case TCP_CLOSE:/\*CLOSE状态的处理代码\*/   
case TCP_LISTEN:/\*LISTEN状态的处理代码\*/   
case TCP_SYN_sent:/\*处理接收到的数据段\*/ queued $=$ tcp_rcv_synsent_state_process(sk,skb,th); if (queued $\geqslant 0$ 1 return queued;   
/\*处理紧急数据并检测是否有数据需要发送\*/

```c
tcp urg(sk, skb, th);
/* 释放缓存 */
__kfree_skb(skb);
tcp_data_snd_check(sk);
return 0;
}
```

关于 tcp_urg 的详细内容，请参见8.11.3.1.

# 5.1.3.3 tcp_rcv_synsent_state_process

具体的处理代码在tcp_rcv_synsent_state_process中，通过命名就可以看出，该函数是专门用于处理 SYN_SENT 状态下收到数据的情况。

/*   
Location:   
net/ipo4/tcp_input.c   
Function:   
处理 syn 已经发送的状态。   
Parameter:   
sk：传输控制块   
skb：缓存   
th:tcp 报文的头部   
*/   
static int tcp_rcv_synsent_state_process(struct sock \*sk，struct sk BUFF \*skb, const struct tcphdr \*th)   
{ struct inlet_connection_sock \*icsk $=$ inlet_csk(sk); struct tcp SOCK \*tp $=$ tcp_sk(sk); struct tcp_fastopen_cookie foc $=$ { .len $= -1$ }; int saved_clamp $=$ tp->rx_opt.mss_clamp; /\*解析TCP选项，并保存在传输控制块中\*/ tcp_scan_options(skb,&tp->rx_opt,0,&foc); /\* rcv_tsecr 保存最近一次接收到对端的TCP段的时间 截选项中的时间戳回显应答。？？？ tsoffset means timestamp offset   
if(tp->rx_opt.saw_tstamp&&tp->rx_opt.rcv_tsecr) tp->rx_opt.rcv_tsecr $= =$ tp->toffset;

关于 tcp_parse_options 的更多的内容，请参见8.6.1.2。

接下来的部分，就是按照TCP协议的标准来实现相应的行为。注释中出现的RFC793即是描述 TCP 协议的 RFC 原文中的文本。

1 if(th->ack){   
2 /\*rfc793: \* "If the state is SYN-SENT then \* first check the ACK bit \* If the ACK bit is set \* If SEG.ACK $= <$ ISS,or SEG.ACK $>$ SND.NXT,send \* a reset (unless the RST bit is set,if so drop \* the segment and return)"   
9 \* ISS代表初始发送序号(Initial Send Sequence number) snd_una在输出的段中，最早一个未被确认段的序号   
11 \*/   
12 if(!after(TCP_SKB_CB(sk)>ack_seq,tp->snd_una)|| after(TCP_SKB_CB(sk)>ack_seq,tp->snd_nxt)) goto reset_and Undo;   
15 /\*必须在对应的时间内\*/   
16 if(tp->rx_opt.saw_tstamp &&tp->rx_opt.rcv_tsecr && !between(tp->rx_opt.rcv_tsecr,tp->retrans_stamp, tcp_time_stamp)){ NET_INC_STATSHBH(sock_net(sk),LINUX_MIB_PAWSACTIVEREJECTED); goto reset_and Undo;   
21 }

上面的一段根据 RFC 在判断 ACK 的值是否在初始发送序号和下一个序号之间，如果不在，则发送一个重置。

```c
/* 此时，ACK已经被接受了  
* 
* "If the RST bit is set
* If the ACK was acceptable then signal the user "error:
* connection reset", drop the segment, enter CLOSED state,
* delete TCB, and return."
*/  
if (th->rst) { 
    tcp_reset(sk); 
    goto discard; 
```

接下来，判断了收到的包的 RST 位，如果设置了 RST，则丢弃该分组，并进入 CLOSED状态。

```c
/* rfc793: */
* "fifth, if neither of the SYN or RST bits is set then
* drop the segment and return."
* 
* See note below!
* --ANK(990513)
*/
if (!th->syn)
    goto discard_and Undo; 
```

之后，根据 RFC 的说法，如果既没有设置 SYN 位，也没有设置 RST 位，那么就将分组丢弃掉。前面已经判断了 RST 位了，因此，这里判断一下 SYN 位。

接下来就准备进入到 ESTABLISHED 状态了。

```c
/* rfc793: */
* "If the SYN bit is on ...
*     are acceptable then ...
*     (our SYN has been ACKed), change the connection
*     state to ESTABLISHED...
*/
tcp_ecn_rcv_synack(tp, th);
/* 初始化与窗口有关的参数 */
tcp_init wl(tp, TCP_SKB_CB(skb) -> seq);
/* 处理 ACK*/
tcp_ACK(sk, skb, FLAG_SLOWPATH);
/* Ok.. it's good. Set up sequence numbers and
* move to established.
*/
tp->rcv_nxt = TCP_SKB_CB(skb) -> seq + 1;
tp->rcv_wup = TCP_SKB_CB(skb) -> seq + 1; 
```

对于 SYN 和 SYN/ACK 段是不进行窗口放大的。关于 RFC1323 窗口放大相关的内容，我们在 1.3.2中进行了详细讨论。接下来手动设定了窗口缩放相关的参数，使得缩放不生效。

```c
/* RFC1323: The window in SYN & SYN/ACK segments is */
* never scaled.
*/
tp->snd_wnd = ntohs(th->window); //network to host short int
/* wscale.ok 标志接收方是否支持窗口扩大因子
window_clamp 滑动窗口最大值
*/
if (!tp->rx_opt.wscale.ok) {
    tp->rx_opt.snd_wscale = tp->rx_opt.rcv_wscale = 0;
    tp->window_clamp = min(tp->window_clamp, 65535U);
} 
```

根据时间戳选项，设定相关字段及 TCP 头部长度。

```c
if (tp->rx_opt.saw_tstamp) {
    tp->rx_opt.tstamp.ok = 1;
    tp->tcp_header_len =
        sizeof(struct tcphdr) + TCPOLEN_TSTAMP_ALIGNED;
    /* 减去选项长度 */
    tp->advmss -= TCPOLEN_TSTAMP_ALIGNED;
    tcp_store-tsrecent(tp);
} else {
    tp->tcp_header_len = sizeof(struct tcphdr);
} 
```

之后会根据设定开启 FACK 机制。FACK 是在 SACK 机制上发展来的。SACK 用于准确地获知哪些包丢失了需要重传。开启 SACK 后，可以让发送端只重传丢失的包。而当重传的包比较多时，会进一步导致网络繁忙，FASK 用来做重传过程中的拥塞控制。

```c
if (tcp_is_sack(tp) && sysctl tcp_fack) //sysctl: system control tcp_enable_fack(tp); 
```

最后初始化 MTU、MSS 等参数并完成 TCP 连接过程。

tcp_mtup_init(sk);   
tcp-sync_mss(sk,icsk->icsk_pmtu_cookie);   
tcp_initize_rcv_mss(sk);   
/\*Remember，tcp_poll() does not lock socket! \*Change state from SYN-SENT only after copied_seq \*is initialized.\*/   
tp->copied_seq $=$ tp->rcv_nxt;   
smp.mb();   
tcp_finish_connect(sk，skb);

此后，开始处理一些特殊情况。Fast Open 启用的情况下，SYN 包也会带有数据。这里调用 tcp_rcv_fastopen_synack函数处理 SYN 包附带的数据。

if((tp->syn_fastopen || tp->syn_data)&&tcp_rcv_fastopen_synack(sk, skb, &foc))return -1;  
/\*根据情况进行延迟确认模式???Confused\*/if(sk->sk_write_pending||icsk->icsk.accept_queue. rskq_defer Accept ||icsk->icsk_ACK.pingpong）{/\* Save one ACK.Data will be ready after\*several ticks，if write pending is set.\* \*It may be deleted,but with this feature tcpdumps\*look so_wonderfully_clever，that I was not able\*to stand against the temptation 8) --ANK\*/inet_csk_schedule_ACK(sk);icsk->icsk_ACK.lrcvtime $=$ tcp_time_stamp;tcp-enter QUICKACK_mode(sk);inet_csk_reset_xmit_timer(sk, ICSK_TIME_DACK,TCP_DELACK_MAX，TCP_RTO_MAX);  
discard:_kfree_skb(skb);return 0;  
} else {/\*回复ACK包\*/tcp_send_ACK(sk);  
}  
return -1;

最后是一些异常情况的处理。

```c
/* 进入该分支意味着包中不包含 ACK */
if (th->rst) {
/* rfc793:
* "If the RST bit is set 
```

\* \* Otherwise (no ACK) drop the segment and return." \*如果收到了RST包，则直接丢弃并返回。 \*/ goto discard_and Undo;   
}   
/\* PAWS检查\*/ if(tp->rx_opt.tsrecent_stamp &&tp->rx_opt.saw_tstamp && tcp_paws_reject(&tp->rx_opt,0)) goto discard_and Undo;   
/\*仅有SYN而无ACK的处理\*/ if(th->syn){ /\*We see SYN without ACK. It is attempt of \* simultaneous connect with crossed SYNs. \*Particularly, it can be connect to self. \*/ tcp_set_state(sk，TCP_SYN_RECV); /\*下面的处理和前面几乎一样\*/ if(tp->rx_opt.saw_tstamp){ tp->rx_opt.tstamp.ok = 1; tcpstore tsrecent(tp); tp->tcp_header_len $=$ sizeof(struct tcphdr）+TCPOLEN_TSTAMPAligned; }else{ tp->tcp_header_len $=$ sizeof(struct tcphdr);   
}tp->rcv_nxt $=$ TCP_SKB_CB(sk)->seq +1; tp->copied_seq $=$ tp->rcv_nxt; tp->rcv_wup $=$ TCP_SKB_CB(sk)->seq +1; /\*RFC1323:The window in SYN&SYN/ACK segments is \* never scaled. \*/ tp->snd_wnd $=$ ntohs(th->window); tp->snd wl1 $=$ TCP_SKB_CB(sk)->seq; tp->max_window $=$ tp->snd_wnd; tcp_ecn_rcv_syn(tp，th); tcp_mtup_init(sk); tcp-sync_mss(sk,icsk->icsk_pmtu_cookie); tcp_initize_rcv_mss(sk); tcp_send_synack(sk);   
#if 0 /\*Note，we could accept data and URG from this segment. \*There are no obstacles to make this (except that we must \*either change tcp_recumsg() to prevent it from returning data \*before 3WHS completes per RFC793，or employ TCP Fast Open). \* \*However，if we ignore data in ACKless segments sometimes, \*we have no reasons to accept it sometimes. \*Also，seems the code doing it in step6 of tcp_rcu_state_process

\* is not flawless. So, discard packet for sanity. \* Uncomment this return to process the data. \*/ return -1;   
#else goto discard;   
endif } /\* "fifth, if neither of the SYN or RST bits is set then \* drop the segment and return." \*/   
discard_and Undo: tcp_clear_options(&tp->rx_opt); tp->rx_opt.mss_clamp $=$ saved_clamp; goto discard;   
reset_and Undo: tcp_clear_options(&tp->rx_opt); tp->rx_opt.mss_clamp $=$ saved_clamp; return 1;   
}

# 5.1.3.4 tcp_ack

/\*   
Location: net/ipv4/tcp_input.c   
Function: This routine deals with incoming acks, but not outgoing ones.   
Parameter: sk:传输控制块。 skb：缓存区。 flag:   
\*/ static int tcp ACK(struct sock \*sk，const struct sk BUFF \*skb，int flag) { structinet_connection_sock \*icsk $=$ inet_csk(sk); struct tcp_sock \*tp $=$ tcp_sk(sk); struct tcp_sacktag_state sack_state; u32 prior_snd_una $=$ tp->snd_una; u32 ack_seq $=$ TCP_SKB_CB(sk)->seq;//起始序号 u32 ack $=$ TCP_SKB_CB(sk)->ack_seq;//确认号 bool is_dupack $=$ false; u32 prior_fackets; int prior_packets $=$ tp->packets_out; const int prior_unsacked $=$ tp->packets_out - tp->sacked_out; int acked $= 0$ /\*Number of packets newly acked \*/ /?*/ sack_state.first_sackt.v64 $= 0$ .   
/\*We very likely will need to access write queue head. \*/ prefetchw(sk->sk_write_queue.next);

/\*If the ack is older than previous acks \* then we can probably ignore it. \*/ if(before(ack,prior_snd_una)){ /* RFC 5961 5.2 [Blind Data Injection Attack].[Mitigation] */ if(before(ack,prior_snd_una - tp->max_window)){ tcp_send_challeng ACK(sk, skb); return -1; } goto old_ack;   
\*/ /\*If the ack includes data we haven't sent yet, discard \* this segment (RFC793 Section 3.9). \*/ if(after(ack,tp->snd_nxt)) goto invalid_ack;   
if (icsk->icsk_pending $= =$ ICSK_TIME_EARLY_RETRANS || icsk->icsk_pending $= =$ ICSK_TIME_LOSS-ProBE) tcp_rearm_rto(sk); /*重置超时重传定时器 \*/ /\*说明已经改变了snd_una位置\*/ if(after(ack,prior_snd_una){ flag |= FLAG_SND_UNA_ADVANCED; icsk->icsk_retransmits $= 0$ .   
}   
prior_fackets $=$ tp->fackets_out;   
/\* tsrecent update must be made after we are sure that the packet \* is in window. \*/ ifflag&FLAG_UPDATE_TS_RECENT) tcp_replace tsrecent(tp,TCP_SKB_CB(skb)->seq);/*替换时间戳\*/

关于 tcp_replace_ts_recent 更多的内容，请参见8.8.2。

/\*如果执行的是快速路径并且确认号大于先前的snd_una\*/  
if(!flag&FLAG_SLOWPATH) &&after(ack,prior_snd_una)){/\*Windowisconstant，pureforwardadvance.\*No more checksarequired.\*Note,weuse the fact that SND.UNA $\vDash$ SND.WL2.\*//\*更新窗口的左边界，tp->snd wl1 $=$ seq;\*/tcp_update wl(tp，ack_seq);/\*更新snduna\*/tcp_snd_una_update(tp，ack);/\*标记进行了更新\*/flag $\equiv$ FLAG_WIN_UPDATE;/\*通知拥塞控制算法本次是快速路径\*/tcp_in_ACK_event(sk，CA_ACK_WIN_UPDATE);NET_INC_STATSBH(sock_net(sk)，LINUX_MIB_TBPHPACKS);

关于 tcp_snd_una_update 更多的内容，请参见8.9.2.1.

```c
} else {  
/* 慢速路径 */  
u32 ack_ev_flags = CA_ACK_SLOWPATH;  
/* 判断 ACK 是否是负载数据携带的 */  
if (ack_seq != TCP_SKB_CB(sk) -> end_seq)  
flag |= FLAG_DATA;  
else  
NET_INC_STATISTICS_BH(sock_net(sk), LINUX_MIB_TCPPUREACKS);  
/* 更新发送窗口 */  
flag |= tcp acknowledgment_window(sk, skb, ack, ack_seq);
```

关于 tcp_ack_update_window 更多的内容，请参考8.10.2.2.

/*存在SACK选项，标记重传选项*/ if(TCP_SKB_CB(sk)->sacked) flag |= tcp_sacktag_write_queue(sk,skb,prior_snd_una, &sack_state); /*检测是否存在ECE标志*/ if (tcp_ecn_rcv_ecn_ echo(tp,tp hdr(sk))) { flag |= FLAG_ECE; ack_ev_flags |= CA_ACK_ECE; } if (flag & FLAG_WIN_UPDATE) ack_ev_flags |= CA_ACK_WIN_UPDATE; tcp_in ACK_event(sk,ack_ev_flags);   
1   
/*We passed data and got it acked,remove any soft error \*log. Something worked...   
\*/ sk->sk_errsoft = 0; icsk->icsk_probes_out = 0; /*设置时间戳*/ tp->rcv_tstamp $=$ tcp_time_stamp; /*检测是否有未发送的段，没有则跳转 \*/ if(!prior_packets goto no_queue;   
/\*See if we can take anything off of the retransmit queue. \*/ acked $=$ tp->packets_out; flag |= tcp_clean_rtx_queue(sk,prior_fackets,prior_snd_una, &sack_state);   
/\*得到确认的包的数量 \*/ acked $\equiv$ tp->packets_out;   
/\* tcp ACK_is_dubious用于判断ACK是不是可疑的 1.ACK是不是重复的 2.是不是带有SACK选项并且有ECE 3.是不是Open态 只要有一项为真就会返回1.   
\*/ if (tcp ACK_is_dubious(sk,flag)){ isdupack $=$ !(flag&(FLAG_SND_UNA_ADVANCED|FLAG_NOT_DUP)); tcp_fastretrans alerted(sk,acked,prior_unsacked, isdupack,flag);

关于 tcp_ack_is_dubious 的更多的内容，请参见8.9.1.2. 关于 tcp_fastretrans_alert

的更多内容，请参见8.10.1.2.  
接下来，源代码中已经有一定的注释，就不再分析了。  
if(tp->tlp_high_seq) tcp_process_tlp ACK(sk,ack，flag);   
/\*Advance cumd if state allows \*/   
if (tcp_may raisc_cwnd(sk，flag)) tcp_cong_ avoid(sk，ack，acked);   
\*/ 如果ACK确认的段时包括新的数据、SYN段、以及接收到新的SACK选项或者接收到的ACK是重复的，则认为该传输控制块的输出路由缓存项是有效的。   
\*/   
if((flag&FLAG FORWARD PROGRESS) || !(flag&FLAG NOT DUP)){ structdst_entry \*dst $=$ __sk.dst_get(sk); if(dst) dst confirm(dst);   
}   
if(icsk->icsk_pending $= =$ ICSK_TIME_RETRANS) tcp_schedule_lossprobe(sk);   
tcp_update_pacing_rate(sk);   
return 1;

```c
no_queue: /\* If data was DSACKed, see if we can undo a cumd reduction. \*/ if (flag & FLAG_DSACKING_ACK) tcp_fastretrans ALERT(sk, acked, prior_unsacked, is_dupack, flag); /\* If this ack opens up a zero window, clear backoff. It was \* being used to time the probes, and is probably far higher than \* it needs to be for normal retransmission. \*/ if (tcp_send_head(sk)) tcp ACK probe(sk); if (tp->tlp_high_seq) tcp_process_tlp ACK(sk, ack, flag); return 1;   
invalid_ACK: SOCK_DEBUG(sk, "Ack %u after %u:%u\n", ack, tp->snd_una, tp->snd_nxt); return -1;   
old_ACK: /\* If data was SACKed, tag it and see if we should send more data. \* If data was DSACKed, see if we can undo a cumd reduction. \*/ if (TCP_SKB_CB(skb)->sacked) { flag |= tcp_sacktag_write_queue(sk, skb, prior_snd_una, &sack_state); tcp_fastretrans ALERT(sk, acked, prior_unsacked, is_dupack, flag); } SOCK_DEBUG(sk, "Ack %u before %u:%u\n", ack, tp->snd_una, tp->snd_nxt); return 0; 
```

# 5.1.4 第三次握手: 发送 ACK 包

# 5.1.4.1 基本调用关系

![](images/741e56959ace787e495b04c16cd8b85d3d03156258aa39163ca816b47588b212.jpg)  
图 5.3: Client:Send ACK

# 5.1.4.2 tcp_send_ack

在5.1.3分析的代码的最后，我们看到它调用了tcp_send_ack()来发送 ACK 包，从而实现第三次握手。

```c
/* 
Location 
net/ipv4/tcp_output.c 
Function: 
  This routine sends an ack and also updates the window. 
  该函数用于发送 ACK, 并更新窗口的大小 
Parameter: 
  sk: 传输控制块 
*/ 
void tcp_send_ACK(struct sock *sk) 
{ 
    struct sk BUFF *buff; 
```

/\*如果当前的套接字已经被关闭了，那么直接返回。\*/ if $(\mathrm{sk - > sk\_state} = = \mathrm{TCP\_CLOSE})$ return; /\*拥塞避免\*/ tcp_ca_event(sk,CA_EVENT_NON_DELAYED_ACK); /\*We are not putting this on the write queue,so \*tcp_transmit_skb() will set the ownership to this \*sock. \*为数据包分配空间 \*/ buff $=$ alloc_SKB(MAX.tcpHEADER,sk_gfp_atomic(sk,GFPAtomic)); if(!buff){ inlet_csk_schedule_ACK(sk); inlet_csk(sk)->icsk ACK.ato $=$ TCPATO_MIN; inlet_csk_reset_xmit_timer(sk,ICSK_TIME_DACK, TCP_DELACK_MAX,TCP_RTO_MAX); return; } /\*初始化ACK包\*/ skb_reserved(buffer,MAX.tcpHEADER); tcp_init_nodata_SKB(buffer,tpAcceptable_seq(sk),TCPHDR_ACK); /\*We do not want pure acks influencing TCP Small Queues or fq/pacing \*too much. \*SKB_TRUESIZE(max(1..66, MAX.tcpHEADER))is unfortunately~784 \*We also avoid tcp_wfree() overhead (cache line miss accessing \*tp->tsq_flags) by using regular sock_wfree() \*/ skb_set.tcpPure_ACK(buffer); /\*添加时间戳并发送ACK包\*/ skb_mstamp_get(&buff->skb_mstamp); tcp_transmit_SKb(sk,buffer,O,sk_gfp_atomic(sk,GFPAtomic));

关于 tcp_init_nondata_skb 更多的内容请参见8.5.1.2。

# 5.2 TCP 被动打开-服务器

# 5.2.1 基本流程

tcp 想要被动打开，就必须得先进行 listen 调用。而对于一台主机，它如果想要作为服务器，它会在什么时候进行 listen 调用呢？不难想到，它在启动某个需要 TCP 连接的高级应用程序的时候，就会执行 listen 调用。经过 listen 调用之后，系统内部其实创建了一个监听套接字，专门负责监听是否有数据发来，而不会负责传输数据。

接着客户端就开始等待接收 SYN 段，然后回复 SYN+ACK 段，最后接收 ACK 段，三次握手建立连接。

![](images/0b0e8d853404e675e22aad4636f9b2a718a897f06e8f6af9b0e5624c4b38566e.jpg)  
图 5.4: Server:Receive SYN

# 5.2.2 第一次握手：接受 SYN 段

# 5.2.2.1 基本调用关系

# 5.2.2.2 tcp_v4_do_rcv

在进行第一次握手的时候，TCP 必然处于 LISTEN 状态。传输控制块接收处理的段都由tcp_v4_do_rcv来处理。

```c
/*   
Location:   
net/ipv4/tcp_ipv4.c   
Function:   
The socket must have it's spinlock held when we get here, unless it is a TCP_LISTEN socket.   
We have a potential double-lock case here, so even when doing backlog processing we use the BH locking scheme. This is because we cannot sleep with the original spinlock held.   
Parameter:   
sk:传输控制块   
skb：传输控制块缓冲区   
\*/   
int tcp_v4_do_rcv(struct sock \*sk，struct sk BUFF \*skb)   
{ struct sock \*rsk;   
/\*省略无关代码 \*/   
/\*基于伪首部累加和进行全包的校验和，判断包是否传输正确 \*/ if (tcp_checksum_COMPLETE(sk)) goto csum_err; 
```

关于 tcp_checksum_complete 更多内容，请参见8.4.1.

```c
if (sk->sk_state == TCP_LISTEN) {  
/* 进行相应的 cookie 检查 */  
struct sock *nsk = tcp_v4_cookie_check(sk, skb);  
if (!nsk)  
goto discard;  
if (nsk != sk) {  
sock_rps_save_rxhash(sk, skb);  
sk_mark_napi_id(sk, skb);  
if (tcp_child_process(sk, nsk, skb)) {  
rsk = nsk;  
goto reset;  
}  
return 0;  
}  
} else  
sock_rps_save_rxhash(sk, skb); 
```

接下来处理收到的数据包，在 Linux 的 TCP 实现中，tcp_rcv_state_process 负责根据接收到的包维护 TCP 的状态机。不止是 SYN 段，其他收到的段大多也会交给该函数处理。

/\*处理接收到的SYN段，返回1，表示处理错误\*/ if（tcp_rcv_state_process(sk，skb））{ $\mathrm{rsk} = \mathrm{sk}$ ： goto reset; } return0;   
reset: tcp_v4_send_reset(rsk，skb);   
 discard: kfree_skb(skb); /\*Be careful here.If this function gets more complicated and \*gcc suffers from register pressure on the x86, sk (in %ebx) \*might be destroyed here. This current version compiles correctly, \*but you have been warned. \*/ return 0;   
csum_err: TCP_INC_STATSHBH(sock_net(sk)，TCP_MIB_CSUMERRORS); TCP_INC_STATSHBH(sock_net(sk)，TCP_MIB_INERRS); goto discard;   
}

# 5.2.2.3 tcp_v4_cookie_check

TCP 的 SYN Cookie 机制是为了防范 SYN Flood 攻击而产生的。其思想是在收到TCP SYN 包后，根据 SYN 包的信息计算一个 cookie 值作为 $\mathrm { S Y N + A C K }$ 报的初始序列号。当客户端返回 ACK 包时，根据返回的确认序号来判断该连接是否为一个正常的连接。tcp_v4_cookie_check函数正是用于检查该 cookie 值的。

1 /\* Location   
2   
3 net/ipo4/tcp_ipu4.c   
4   
5   
6 Function   
7   
8 cookie检查   
9   
10 Parameter   
11 sk：传输控制块   
13 skb：传输控制块缓冲区。   
14 \*/   
15 static struct sock \*tcp_v4_cookie_check(struct sock \*sk，struct sk BUFF \*skb)   
16 {   
17 #ifdef CONFIG_SYN_COOKIEES   
18 const struct tcphdr \*th $=$ tcp hdr(sk);   
19 if(!th->syn)   
21 sk $=$ cookie_v4_check(sk,skb);   
22 #endif   
23 return sk;   
24 }

显然对于第一次握手的时候，接收到的是 syn 包。此时还没有 cookie 值，故而直接返回了 sk。

# 5.2.2.4 tcp_rcv_state_process

与第一次握手相关的代码如下：

1 /\* Location:   
2   
3 /net/iju4/tcp_input.c   
4   
5   
6 Function:   
7 \* This function implements the receiving procedure of RFC 793 for   
8 all states except ESTABLISHED and TIME_WAIT.   
9 \* It's called from both tcp_v4_rcv and tcp_v6_rcv and should be   
10 address independent.   
11 Parameter   
12   
13   
14 sk:传输控制块   
15   
16 skb:传输控制块缓冲区   
17 \*/   
18   
19 int tcp_rcv_state_process(struct sock \*sk,struct sk BUFF \*skb)   
20 { struct tcp SOCK \*tp $=$ tcp_sk(sk); struct inlet_connection SOCK \*icsk $=$ inlet_csk(sk); const struct tcphdr \*th $=$ tcp hdr(skb); struct request SOCK \*req; int queued $= 0$

bool acceptable; /\*saw_tstamp 表示在最新的包上是否看到的时间戳选项 \*/ tp->rx_opt.saw_tstamp $= 0$ .   
switch (sk->sk_state) { /\*省略无关代码 \*/ case TCP_LISTEN: if (th->ack) return 1; if (th->rst) goto discard; if (th->syn) { if (th->fin) goto discard; if (icsk->icsk.af ops->conn_request(sk, skb) $<  0$ ） return 1; /\* Now we have several options: In theory there is \* nothing else in the frame. KA9Q has an option to \* send data with the syn, BSD accepts data with the \* syn up to the [to be] advertised window and \* Solaris 2.1 gives you a protocol error. For now \* we just ignore it, that fits the spec precisely \* and avoids incompatibilities. It would be nice in \* future to drop through and process the data. \* \* Now that TTCP is starting to be used we ought to \* queue this data. \* But, this leaves one open to an easy denial of \* service attack, and SYN cookies can't defend \* against this problem. So, we drop the data \* in the interest of security over speed unless \* it's still in use. \*/ kfree_SKb(skb); return O; } goto discard; /\* 省略无关代码 \*/ discard: __kfree_SKb(skb); } return O;

显然，所接收到的包的 ack、rst、fin 字段都不为 1，故而这时开始进行连接检查，判断是否可以允许连接。经过不断查找，我们发现icsk->icsk_af_ops->conn_request最终会掉用tcp_v4_conn_request进行处理。如果 syn 段合法，内核就会为该连接请求创建连接请求块，并且保存相应的信息。否则，就会返回 1, 原函数会发送 reset 给客户端表明连接请求失败。

当然，如果收到的包的 ack 字段为 1, 那么由于此时链接还未建立，故该包无效，返回 1, 并且调用该函数的函数会发送 reset 包给对方。如果收到的是 rst 字段或者既有 fin

又有 syn 的字段，那就直接销毁，并且释放内存。

# 5.2.2.5 tcp_v4_conn_request && tcp_conn_request

该函数如下：

```c
/* why make two functions here?   
Location   
/net/ipv4/tcp_ipv4.c   
Function:   
服务端用来处理客户端连接请求的函数。   
Parameter:   
sk：传输控制块 skb：传输控制块缓冲区   
\*/ int tcp_v4 conn_request(struct sock \*sk，struct sk BUFF \*skb) { /* Never answer to SYNs send to broadcast or multicast */ if (skb_rtable(sk)->rt_flags & (RTCF_BROADCAST | RTCF Multicast)) goto drop; return tcp conn_request(&tcp_request_sockOps, &tcp_request_sock_ipv4Ops, sk, skb);   
drop: NET_INC_STAT5_BH(sock_net(sk), LINUX_MIB_LISTENDROPS); return 0;   
} 
```

如果一个 SYN 段是要被发送到广播地址和组播地址，则直接 drop 掉，然后返回 0。否则的话，就继续调用tcp_conn_request进行连接处理。如下：

```c
/\*   
Location   
/net/iju4/tcp_ipu4.c   
Function:   
服务端用来处理客户端连接请求的函数。   
Parameter:   
rsk ops：请求控制块的函数接口 af ops:TCP 请求块的函数接口 sk：传输控制块 skb：传输控制块缓冲区   
\*/   
int tcp conn_request(struct request_sockOps \*rsk ops, const struct tcp_request_sock OPS \*af ops, struct sock \*sk，struct sk BUFF \*skb)   
{ /\*初始化len字段\*/
```

```c
struct tcp_fastopen_cookie foc = {.len = -1};
/*tw: time wait isn: initial sequence num 初始化序列号 */
__u32 isn = TCP_SKB_CB(sk) -> tcp_tw_isn;
struct tcp_options_received tmp_opt;
struct tcp_sock *tp = tcp_sk(sk);
struct sock *fastopen_sk = NULL;
struct dst_entry *dst = NULL;
struct request_sock *req;
/*标志是否开启了SYN_COOKIE选项*/
bool want_cookie = false;
/*路由查找相关的数据结构*/
struct flowi fl;
/*TW(time wait) buckets are converted to open requests without
*limitations, they conserve resources and peer is
* evidently real one.
*/
if ((sysctl tcp_syncookies == 2 ||
    inet_csk_reqsk_queue_is_full(sk)) && !isn) {
    want_cookie = tcp_syn_flood_action(sk, skb, rsk_op->slab_name);
    if (!want_cookie)
        goto drop;
} 
```

如果打开了 SYNCOOKIE 选项，并且 SYN 请求队列已满并且 isn 为 0, 然后通过函数tcp_syn_flood_action 判断是否需要发送 syncookie。如果没有启用 syncookie 的话，就会返回 false，此时不能接收新的 SYN 请求，会将所收到的包丢掉。

```javascript
/\*Acceptbacklogisfull.Ifwehavealready queuedenough \*ofwarmentriesinsynqueue，droprequest.Itisbetterthan \*clogging syn queue with openreqswith exponentially increasing \*timeout.\*/ if(sk_acceptq_is_full(sk)&&inet_csk_reqsk_queue_young(sk)>1){ NET_INC_STATSBH(ock_net(sk)，LINUX_MIB_LISTENOVERFLOWS); goto drop;   
} 
```

如果存放已经完成连接的套接字的队列长度已经达到上限且 SYN 请求队列中至少有一个握手过程中没有重传过段，则丢弃当前请求。

```txt
req = inetrequires_alloc( rsk ops, sk, !want_cookie); if (!req) goto drop; 
```

这时调用inet_reqsk_alloc分配一个连接请求块，用于保存连接请求信息，同时初始化在连接过程中用来发送 ACK/RST 段的操作集合，以便在建立连接过程中能方便地调用这些接口。如果申请不了资源的话，就会放弃此次连接请求。

tcp_rskreq $\rightharpoonup$ af_specific $=$ af ops;   
tcp_clear_options(&tmp_opt);   
tmp_opt.mss_clamp $=$ af ops->mss_clamp;   
tmp_opt.user_mss $=$ tp->rx_opt.user_mss;   
tcp.parse_options(skb, &tmp_opt, 0, want_cookie ? NULL : &foc);

之后，清除 TCP 选项，初始化mss_vlamp和user_mss. 然后调用tcp_parse_options解析 SYN 段中的 TCP 选项，查看是否有相关的选项。

关于 tcp_clear_options 的更多内容，请参见8.6.1.3。

```txt
if (want_cookie && !tmp_opt.saw_tstamp) tcp_clear-options(&tmp_opt); 
```

如果启动了 syncookies，并且 TCP 选项中没有存在时间戳，则清除已经解析的 TCP选项。这是因为计算syn_cookie必须用到时间戳。

```javascript
/\* tstamp.ok 表示在收到的 SYN 包上看到的 TESTAMP\*/ tmp_opt.tstamp.ok = tmp_opt.saw_tstamp; tcp_openreq_init(req, &tmp_opt, skb, sk);
```

这时，根据收到的 SYN 段中的选项和序号来初始化连接请求块信息。

/\*Note: tcp_v6_init_req() might override ir_iif for link locals \*/   
inet_rskreq $\rightharpoonup$ ir_iif $=$ sk->sk_bound_dev_if;   
af ops->init_reqreq,sk,skb);   
if (security_inetconn_request(sk,skb,req)) goto drop_and_free;

这一部分于 IPV6 以及安全检测有关，这里不进行详细讲解。安全检测失败的话，就会丢弃 SYN 段。

if(!want_cookie &&!isn){ /\*VJ's idea.We save last timestamp seen \*from the destination in peer table,when entering \*state TIME-WAIT,and check against it before \*accepting new connection request. \* \*If"ism"is not zero,thisrequesthit alive \* timewait bucket,so that all the necessary checks \*are made in the function processing timewait state. \*/ if (tcp_death_row.sysctl_tw_recycle) { bool strict; dst $=$ af ops->route_req(sk,&fl,req,&strict); if (dst && strict &&！tcppeer_is_proven(req,dst,True, tmp_opt.saw_tstamp)){ NET_INC_STATISTICS_BH(sock_net(sk),LINUX_MIB_PAWSPASSIVEREJECTED); goto drop_and_release; } } /\*Kill the following clause,if you dislike this way.\*/ else if(!sysct1 tcp_syncookies && (sysct1_max_syn_backlog -inet_csk_reqsk_queue_len(sk)< (sysct1_max_syn_backlog>>2))&&！tcppeer_is_proven req,dst,false, tmp_opt.saw_tstamp)){ /\*Without syncookies last quarter of \*backlog is filled with destinations, \* proven to be alive. \*It means that we continue to communicate \*to destinations,already remembered

\* to the moment of synflood. \*/ pr_drop_req(req, ntohs.tcp hdr(skb)->source), rsk ops->family); goto drop_and_release; } isn $=$ af ops->init_seq(skb);

如果没有开启 syncookie 并且 isn 为 0 的话，其中的第一个 if 从对段信息块中获取时间戳，在新的连接请求之前检测 PAWS。后边的表明在没有启动 syncookies 的情况下受到 synflood 攻击，丢弃收到的段。之后由源地址，源端口，目的地址以及目的端口计算出服务端初始序列号。

if(!dst){ dst $=$ af ops->route_req(sk, &fl, req, NULL); if(!dst) goto drop_and_free;   
}   
/\*显示拥塞控制\*/ tcp_ecn_create_request(req, skb, sk, dst);   
/\*如果开启了 cookie的话，就需要进行相应的初始化\*/ if(want_cookie){ isn $=$ cookie_init_sequence.af ops,sk, skb,&req->mss); req->cookie ts $=$ tmp_opt.tstamp.ok; if(!tmp_opt.tstamp.ok) inlet_rskreq->ecn.ok $= 0$ .   
}   
tcp_rskreq->snt_isn $=$ isn;   
tcp_rskreq->txhash $=$ net_tx_rndhash();   
tcp_openreq_init_rwinreq,sk,dst);   
if(!want_cookie){ tcp_reqsk_record_syn(sk, req, skb); fastopen_sk $\equiv$ tcp_try_fastopen(sk, skb, req, &foc, dst);   
}   
/\*如果开启了fastopen的话，顺便发送数据 否则就是简单地发送。   
\*/   
if(fastopen_sk){ af ops->send_synack(fastopen_sk, dst, &fl, req, &foc,false); /\*Add the child socket directly into the accept queue \*/ inlet_csk_reqsk_queue_add(sk, req, fastopen_sk); sk->sk_data_ready(sk); bh_unlock_sock(fastopen_sk); sock_put(fastopen_sk);   
} else{ tcp_rskreq->tfo Listener $=$ false; if(!want_cookie) inlet_csk_reqsk_queue_hash_add(sk, req,TCP_TIMEOUT_INIT); af ops->send_synack(sk,dst,&fl, req, &foc, !want_cookie); if(want_cookie)

```c
goto drop_and_free;   
} reqsk_putreq); return 0;   
drop_and_release: dst_releasedst);   
drop_and_free: reqsk_freereq);   
drop: NET_INC_STATSBH(sock_net(sk),LINUX_MIB_LISTENDROPS); return 0; 
```

关于inet_csk_reqsk_queue_add的更多内容请参见8.2.3。关于inet_csk_reqsk_queue_hash_add的更多的内容请参见8.2.3.1。

# 5.2.3 第二次握手：发送 SYN+ACK 段

在第一次握手的最后调用了af_ops->send_synack函数，而该函数最终会调用tcp_v4_send_synack函数进行发送, 故而这里我们这里就从这个函数进行分析。

# 5.2.3.1 基本调用关系

![](images/90bb909f6ee99fe870f5c2df089c4f587eda5bfe50384068fbf9f0cc1d68fcf4.jpg)  
图 5.5: Server:Send SYN+ACK

# 5.2.3.2 tcp_v4_send_synack

```txt
1 /* Location:   
2   
3 net/ipo4/tcp_ipu4.c   
4   
5   
6 Function:   
7 Send a SYN-ACK after having received a SYN.   
8   
9 This still operates on a request_sock only, not on a big socket.   
10 
```

```c
Paramaters: sk: 传输控制块 dst: 存储缓存路由项中独立于协议的信息。 fl: req: 请求控制块 foc: 快打开缓存 attach_req: static int tcp_v4_send_synack(const struct sock *sk, struct dst_entry *dst, struct flowi *fl, struct request_sock *req, struct tcp_fastopen_cookie *foc, bool attach_req) { const struct inet_request_sock *ireq = inet_rsk的要求); struct flowi4 f14; int err = -1; struct sk_buffer *skb; /* First, grab a route. */ if (!dst && (dst =INET_csk-route_req(sk, &f14, req)) == NULL) return -1; 
```

首先，如果传进来的 dst 为空或者根据连接请求块中的信息查询路由表，如果没有查到，那么就直接退出。

```c
/* 根据当前的传输控制块，路由信息，请求等信息构建 syn+ack 段 */
skb = tcp.make_synack(sk, dst, req, foc, attach_req);
/* 如果构建成功的话，就生成 TCP 校验码，然后调用
ip_build_and_send_pkt 生成 IP 数据报并且发送出去。
*/
if (skb) {
    __tcp_v4_send_check(skb, ireq->ir_location_addr, ireq->ir_rmt_addr);
    err = ip_build_and_send_pkt(skb, sk, ireq->ir_location_addr,
            ireq->ir_rmt_addr,
            ireq->opt);
    err = net_xmit_eval(err);
}
```

net_xmit_eval用于过滤错误码中的拥塞提示错误，换句话说，就是如果错误是由于拥塞造成的，那么就忽略掉，否则就返回错误码。

# 5.2.3.3 tcp_make_synack

```txt
/\*   
Location:   
net/ipv4/ycp_output.c   
Function: 
```

tcp.make_synack - Prepare a SYN-ACK. Allocate one skb and build a SYNACK packet. @dst is consumed : Caller should not use it again.该函数用来构造一个SYN+ACK段，并初始化TCP首部及SKB中的各字段项，填入相应的选项，如MSS，SACK，窗口扩大因子，时间戳等。   
Parameter: sk : listener socket dst : dst entry attached to the SYNACK req : request_sock pointer foc : fast open cookie attach_req : \*/ struct sk BUFF \*tcp.make_synack(const struct sock \*sk,struct dst_entry \*dst, struct request_sock \*req, struct tcp_fastopen_cookie \*foc, bool attach_req) { struct inet_request_sock \*ireq $=$ inet_rsk的要求); const struct tcp_sock \*tp $=$ tcp_sk(sk); struct tcp_md5sig_key \*md5 $=$ NULL; struct tcp_out_options opts; struct sk_buffer \*skb; int tcp_header_size; struct tcphdr \*th; u16 user_mss; int mss; skb $=$ alloc_skb(MAX.tcpHEADER,GFPAtomic); if (unlikely(!skb)){ dst_release.dst); return NULL; }

首先为将要发送的数据申请发送缓存，如果没有申请到，那就会返回 NULL。关于 unlikely 优化的更多内容，请参见9.4.2.

```txt
/* Reserve space for headers. */ skb_reserved(skb, MAX_TCPHEADER); 
```

为 MAC 层，IP 层，TCP 层首部预留必要的空间。

```c
if (attach_req) { skb_set-owner_w(skb, req_to_sk(req)); } else { /* sk is a const pointer, because we want to express multiple * cpu might call us concurrently. * sk->sk_wmem_alloc in an atomic, we can promote to rw. */ skb_set-owner_w(skb, (struct sock *)sk); } skb.dst_set(skb, dst); 
```

根据attach_req来判断该执行如何执行相关操作。然后设置发送缓存的目的路由。

```txt
mss = dst_metric_advmss.dst);  
user_mss = READ_ONCE(tp->rx_opt.user_mss);  
if (user_mss && user_mss < mss)  
mss = user_mss; 
```

根据每一个路由器上的 mss 以及自身的 mss 来得到最大的 mss。

```c
memset(&args, 0, sizeof(args));
#ifdef CONFIG_SYN(cookIES
    if (unlikely(req->cookie(ts))
        skb->skb_mstamp.stamp_jiffies = cookie_init_timestampreq);
    else
        #endif
    skb_mstamp_get(&skb->skb_mstamp); 
```

清除选项，并且设置相关时间戳。

ifdef CONFIG_TCP_MD5SIG rcu_read_lock(); md5 $=$ tcp_rskreq->af_specic->req_md5LOOKUP(sk, req_to_skreq)); #endif

查看是否有 MD5 选项，有的话构造出相应的 md5.

skb_set_hash(skb, tcp_rskreq) $\rightharpoonup$ txhash,PKT_HASH_TYPE_L4);   
tcp_header_size $=$ tcp_synack_options req,mss, skb,&opts, md5, foc) $^+$ sizeof(*th);   
skb.push(skb, tcp_header_size);   
skb_reset transporting_header(skb);

得到 tcp 的头部大小，然后进行大小设置，并且重置传输层的头部。

```c
th = tcp hdr(skb);
memset(th, 0, sizeof(struct tcphdr));
th->syn = 1;
th->ack = 1;
tcp_ecn.make_synack(req, th);
th->source = htons(ireq->ir_num);
th->dest = ireq->ir_rmt_port; 
```

清空 tcp 头部，并设置 tcp 头部的各个字段。

/\* Setting of flags are superfluous here for callers (and ECE is \* not even correctly set)   
tcp_init_nodata_SKb(skb, tcp_rskreq->snt_isn, TCPHDR_SYN | TCPHDR_ACK);   
th->seq $=$ htons(TCP_SKB_CB(skb->seq);   
/\* XXX data is queued and acked as is. No buffer/window check \*/ th->ack_seq $\equiv$ htons.tcp_rskreq->rcv_nxt);   
\*/ RFC1323: The window in SYN & SYN/ACK segments is never scaled. \*/ th->window $\equiv$ htons(min(req->rsk_rcv_wnd, 65535U));   
tcp_options_write((_,be32 \*) (th + 1), NULL, &options);

14 th->doff $=$ (tcp_header_size>>2);   
15 TCP_INC_STATISTICS_BH(sock_net(sk)，TCP_MIB_OUTSEGS);

首先初始化不含数据的 tcp 报文，然后设置相关的序列号，确认序列号，窗口大小，选项字段，以及 TCP 数据偏移，之所以除以 4，是 doff 的单位是 32 位字，即以四个字节长的字为计算单位。

关于 tcp_init_nondata_skb 更多的内容，请参见8.5.1.2。

```c
#ifdef CONFIG.tcp_MD5SIG
/* Okay, we have all we need - do the md5 hash if needed */
if (md5)
    tcp_rsk(req) -> af_specic->calc_md5_hash(options.hash_location,
                md5, req_to_skreq), skb);
rcu_read_unlock();
#endif
/* Do not fool tcpdump (if any), clean our debris */
skb->tstamp.tv64 = 0;
return skb;
} 
```

最后判断是否需要 md5 哈希值，如果需要的话，就进行添加。最后返回生成包含SYN+ACK 段的 skb。

# 5.2.4 第三次握手：接收 ACK 段

在服务器第二次握手的最后启动了建立连接定时器，等待客户端最后一次握手的ACK 段。

# 5.2.4.1 基本调用关系

![](images/c0cd0f1fd1d948f2d35aaf49193b3aa5591c228122d8ecea3895900223e405d2.jpg)  
图 5.6: Server:Receive ACK

5.2.4.2 tcp_v4_do_rcv   
/\*   
Location:   
/net/ipv4/tcp_ipv4.c   
Function:   
The socket must have it's spinlock held when we get here, unless it is a TCP_LISTEN socket.   
We have a potential double-lock case here, so even when doing backlog processing we use the BH locking scheme. This is because we cannot sleep with the original spinlock held.   
Parameter:   
sk:传输控制块。 skb：传输控制块缓冲区。   
\*/   
int tcp_v4_do_rcv(struct sock \*sk，struct sk BUFF \*skb) { struct sock \*rsk; /\*\*省略无关代码\*/ if (tcp_checksum_COMPLETE(skb)) goto csum_err; if (sk->sk_state $= =$ TCP_LISTEN）{ /\* NULL，错误 nsk $= =$ sk，接收到SYN nsk！ $= \mathrm{sk}$ ，接收到ACK \*/ struct sock \*nsk $=$ tcp_v4_cookie_check(sk，skb); if(!nsk) goto discard; if(nsk != sk){ sock_rps_save_rxhash(nsk，skb); sk_mark_napi_id(nsk，skb); if (tcp_child_process(sk，nsk，skb)){ rsk $=$ nsk; goto reset; } return 0; } } else sock_rps_save_rxhash(sk，skb); /\*\*省略无关代码\*/   
reset: tcp_v4_send_reset(rsk，skb); discard: kfree_skb(skb); /\*Be careful here.If this function gets more complicated and \*gcc suffers from register pressure on the x86,sk(in %ebx)

```c
\* might be destroyed here. This current version compiles correctly, \* but you have been warned.   
\*/ return 0;   
csum_err: TCP_INC_STATSHBH(sock_net(sk),TCP_MIB_CSUMERRRS); TCP_INC_STATSHBH(sock_net(sk),TCP_MIB_INERRS); goto discard;   
} 
```

在服务器最后一次握手的时候，其实传输控制块仍然处于 LISTEN 状态，但是这时候cookie检查得到的传输控制块已经不是侦听传输控制块了，故而会执行tcp_child_process来初始化子传输控制块。如果初始化失败的话（返回值非零），就会给客户端发送 RST 段进行复位。

5.2.4.3 tcp_v4_cookie_check   
static struct sock \*tcp_v4_cookie_check(struct sock \*sk, struct sk BUFF \*skb)   
{ #ifdef CONFIG_SYNCookies const struct tcphdr \*th $=$ tcp hdr(sk); if(!th->syn) sk $=$ cookie_v4_check(sk,skb); #endif return sk;   
}

如果 Linux 内核中定义CONFIG_SYN_COOKIES宏，此时在第三次握手阶段，并不是syn 包, 内核就会执行cookie_v4_check。在这个函数中，服务器会将客户端的 ACK 序列号减去 1, 得到 cookie 比较值，然后将客户端的 IP 地址，客户端端口，服务器 IP 地址和服务器端口，接收到的 TCP 序列好以及其它一些安全数值等要素进行 hash 运算后，与该 cookie 比较值比较，如果相等，则直接完成三次握手，此时不必查看该连接是否属于请求连接队列。

# 5.2.4.4 tcp_child_process

该函数位于/net/ipv4/minisocks.c中，子传输控制块开始处理 TCP 段。

```txt
/*   
Location:   
net/ipv4/tcp_minisock.c   
Functions:   
Queue segment on the new socket if the new socket is active, otherwise we just shortcircuit this and continue with the new socket.   
For the vast majority of cases child->sk_state will be TCP_SYN_RECV when entering. But other states are possible due to a race condition 
```

where after __inetLOOKUP_established() fails but before the listener locked is obtained, other packets cause the same connection to be created. Parameters: parent:父传出控制块 child：子传输控制块 skb：传输控制块缓存 \*/ int tcp_child_process(struct sock \*parent，struct sock \*child, struct sk BUFF \*skb) { int ret $= 0$ int state $=$ child->sk_state; tcp_sk(child)->segs_in $+ =$ max_t(u16,1,skb_shinfo(sk)->gso_segs); if(!sock_own_by_user(child)){ ret $=$ tcp_rcv_state_process(child,skb); /\* Wakeup parent, send SIGIO \*/ if(state $= =$ TCP_SYN_RECV && child->sk_state != state) parent->sk_data_ready(parent); }else{ /\* Alas,it is possible again,because we do lookup \* in main socket hash table and lock on listening \* socket does not protect us more. \*/ ____sk_add.Backlog(child,skb); } bh_unlock_sock(child); sock_put(child); return ret;

首先，如果此时刚刚创建的新的子传输控制块没有被用户进程占用，则根据第三次握手的 ACK 段，调用tcp_rcv_state_process继续对子传输控制块做初始化。否则的话，只能将其加入后备队列中，等空闲时再进行处理。虽然这种情况出现的概率小，但是也是有可能发生的。

# 5.2.4.5 tcp_rcv_state_process

该函数位于/net/ipv4/tcp_input.c中。

该函数用来处理 ESTABLISHED 和TIME_WAIT状态以外的 TCP 段，这里处理SYN_RECV状态。

acceptable $=$ tcp ACK(sk, skb, FLAG_SLOWPATH | FLAG_UPDATE_TS_RECENT) $>0$

首先对收到的 ACK 段进行处理判断是否正确接收，如果正确接收就会发送返回非零值。

```c
switch (sk->sk_state) {  
    case TCP_SYN_RECV:  
        if (!acceptable)  
            return 1;  
    if (!tp->srtt_us)  
        tcp_synack_rtt(meas(sk, req);  
        /* Once we leave TCP_SYN_RECV, we no longer need req  
            * so release it.  
            */  
    if (req) {  
        tp->total_retrans = req->num_retrans;  
        reqsk_fastopen_remove(sk, req, false);  
    } else {  
        /* Make sure socket is routed, for correct metrics. */  
        icsk->icsk.af ops->rebuild_header(sk);  
        tcp_init_congestion_control(sk);  
        tcp_mtup_init(sk);  
        tp->copied_seq = tp->rcv_nxt;  
        tcp_init_buffer_space(sk);  
    }  
    smb_nb();  
    tcp_set_state(sk, TCP Established);  
    sk->sk_state_change(sk); 
```

进行一系列的初始化，开启相应拥塞控制等，并且将 TCP 的状态置为 ESTAB-LISHED。

/\* Note, that this wakeup is only for marginal crossed SYN case. \* Passively open sockets are not waked up, because \* sk->sk_sleep $= =$ NULL and sk->sk_socket $= =$ NULL. \*/ if (sk->sk_socket) sk_wake_async(sk,SOCK_WAKE_IO,POLL_OUT);

发信号给那些将通过该套接口发送数据的进程，通知它们套接口目前已经可以发送数据了。

```c
tp->snd_una = TCP_SKB_CB(skb) ->ack_seq;  
tp->snd_wnd = ntohs(th->window) << tp->rx_opt.snd_wscale;  
tcp_init wl(tp, TCP_SKB_CB(skb) ->seq);  
if (tp->rx_opt.tstamp.ok)  
tp->advmss -= TCPOLEN_TSTAMP_ALIGNED; 
```

初始化传输控制块的各个字段，对时间戳进行处理。

```c
if (req) {  
/* Re-arm the timer because data may have been sent out.  
* This is similar to the regular data transmission case  
* when new data has just been ack'd.  
*  
* (TFO) - we could try to be more aggressive and  
* retransmitting any data sooner based on when they  
* are sent out.  
*/ 
```

```txt
10 tcp_rearm_rto(sk);   
11 } else   
12 tcp_init_metricstsk);
```

为该套接口初始化路由。

```c
tcp_update_pacing_rate(sk);
/* Prevent spurious tcp_cund_restart() on first data packet */
tp->lsndtime = tcp_time_stamp;
tcp_init_rcv_mss(sk);
tcp_fast_path_on(tp);
break; 
```

更新最近一次的发送数据报的时间，初始化与路径 MTU 有关的成员，并计算有关TCP 首部预测的标志。

```javascript
1 /* step 6: check the URG bit */   
2 tcp Urg(sk, skb, th); 
```

检测带外数据标志位。

关于 tcp_urg 的详细内容，请参见8.11.3.1.

```c
/* step 7: process the segment text */
switch (sk->sk_state) {
    /* 省略无关代码 */
    case TCP Established:
        tcp_data_queue(sk, skb);
        queued = 1;
        break;
} 
```

对已接收到的 TCP 段排队，在建立连接阶段一般不会收到 TCP 段。

```c
/* tcp_data could move socket to TIME-WAIT */
if (sk->sk_state != TCP_CLOSE) {
    tcp_data_snd_check(sk);
    tcp ACK_snd_check(sk);
} 
```

显然此时状态不为 CLOSE，故而就回去检测是否数据和 ACK 要发送。其次，根据 queue 标志来确定是否释放接收到的 TCP 段，如果接收到的 TCP 段已添加到接收队列中，则不释放。

# CHAPTER 6

TCP 拥塞控制

# Contents

# 6.1 拥塞控制实现 . . . . 113

6.1.1 拥塞控制状态机 113

6.1.1.1 基本转换 . . . . 113  
6.1.1.2 Open 状态 . . 114   
6.1.1.3 Disorder 状态 114   
6.1.1.4 CWR 状态 . 114   
6.1.1.5 Recovery 状态 . . 115   
6.1.1.6 Loss 状态 . . 115

6.1.2 显式拥塞通知 (ECN) . . . 118

6.1.2.1 IP 对 ECN 的支持 . . . . 118  
6.1.2.2 TCP 对 ECN 的支持 119

6.1.3 拥塞控制状态的处理及转换 . . . . . . 120

6.1.3.1 拥塞控制状态处理:tcp_fastretrans_alert . . . . . 120

# 6.2 拥塞控制引擎 . . . 123

6.2.1 接口 . . 124

6.2.1.1 tcp_congestion_ops . . . . . 124   
6.2.1.2 拥塞控制事件 . . . 124  
6.2.1.3 tcp_register_congestion_control() . . . . . . . . 125   
6.2.1.4 tcp_unregister_congestion_control() . . . . . . . 126

6.2.2 CUBIC 拥塞控制算法 . 127

6.2.2.1 CUBIC 算法介绍 127   
6.2.2.2 模块定义 . . 127  
6.2.2.3 基本参数及初始化 . . . . 129  
6.2.2.4 ssthresh 的计算 130

6.2.2.5 慢启动和拥塞避免 . . 130   
6.2.2.6 拥塞状态机状态变更 . . . 134   
6.2.2.7 撤销 CWND 减小 . . . . 134   
6.2.2.8 处理 CWND 事件 . . 134  
6.2.2.9 收到 ACK 135  
6.2.2.10 hystart . . 135

# 6.1 拥塞控制实现

# 6.1.1 拥塞控制状态机

# 6.1.1.1 基本转换

转换如下:

![](images/58f7939044230162a5f02fd359350d813cf9a1694a551124514ca7880f33b481.jpg)  
图 6.1: Congestion Control State Machine

定义如下:

```c
enum tcp_ca_state {
    TCP_CA_Open = 0,
    #define TCPF_CA_Open (1 << TCP_CA_Open)
    TCP_CA_Disorder = 1,
    #define TCPF_CA_Disorder (1 << TCP_CA_Disorder)
    TCP_CA CWER = 2,
    #define TCPF_CA CWER (1 << TCP_CA CWER) 
```

```c
TCP_CA_Recovery = 3,  
define TCPF_CA_Recovery (1<<TCP_CA_Recovery)  
TCP_CA_Loss = 4  
#define TCPF_CA_Loss (1<<TCP_CA_Loss)  
}; 
```

# 6.1.1.2 Open 状态

Open 状态是常态，在这种状态下 TCP 发送方通过优化后的快速路径来处理接收ACK。当一个确认到达时，发送方根据拥塞窗口是小于还是大于慢启动阙值，按慢启动或者拥塞避免来增大拥塞窗口。

# 6.1.1.3 Disorder 状态

当发送方检测到 DACK(重复确认) 或者 SACK(选择性确认) 时，将转变为 Disor-der(无序) 状态。在该状态下，拥塞窗口不做调整，而是每个新到的段触发一个新的数据段的发送。因此，TCP 发送方遵循包守恒原则，该原则规定一个新包只有在一个老的包离开网络后才发送。在实践中，该规定的表现类似于 IETF 的传输提议，允许当拥塞窗口较小或是上个传输窗口中有大量数据段丢失时，使用快速重传以更有效地恢复。

# 6.1.1.4 CWR 状态

TCP 发送方可能从显式拥塞通知、ICMP 源端抑制 (ICMP source quench) 或是本地设备接收到拥塞通知。当收到一个拥塞通知时，发送方并不立刻减小拥塞窗口，而是每隔一个新到的 ACK 减小一个段直到窗口的大小减半为止。发送方在减小拥塞窗口大小的过程中不会有明显的重传，这就处于 CWR(Congestion Window Reduced, 拥塞窗口减小) 状态。CWR 状态可以被 Revcovery 状态或者 Loss 状态中断。进入拥塞窗口减小的函数如下：

```c
/* 
Location: 
net/iju4/tcp_input.c 
Function: 
Enter CWR state. Disable cumd undo since congestion is proven with ECN 
Parameter: 
sk: 传输控制块 
*/ 
void tcp-enter_cwr(struct sock *sk) 
{
    struct tcp_sock *tp = tcp_sk(sk);
    /* 进入 CWR 后就不需要窗口撤消了， 
因此需要清除拥塞控制的慢启动阈值 
*/
    tp->prior_ssthresh = 0;
    /* 可以看出只有 Open 状态和 Disorder 状态可以转移到该状态 */
if (inet_csk(sk) ->icsk_ca_state < TCP_CA_CWR) { 
```

23 /\*进入CwR状态后不允许在进行拥塞窗口撤消了\*/   
24 tp->undomarker $= 0$ ·   
25 $\text{一} *$ 进行相关的初始化\*/   
26 tcp_init_cwnd_reduction(sk);   
27 $\text{一} *$ 设置状态\*/   
28 tcp_set_ca_state(sk，TCP_CA_CWR);   
29 }   
30 }

关于 tcp_init_cwnd_reduction 更多的内容，请参见8.12.2.1。

# 6.1.1.5 Recovery 状态

当足够多的连续重复 ACK 到达后，发送方重传第一个没有被确认的段，进入 Re-covery(恢复) 状态。默认情况下，进入 Recovery 状态的条件是三个连续的重复 ACK，TCP 拥塞控制规范也是这么推荐的。在 Recovery 状态期间，拥塞窗口的大小每隔一个新到的确认而减少一个段，和 CWR 状态类似。这个窗口减小过程终止与拥塞窗口大小等于 ssthresh，即进入 Recovery 状态时，窗口大小的一半。拥塞窗口在恢复期间不增大，发送方重传那些被标记为丢失的段，或者根据包守恒原则在新数据上标记前向传输。发送方保持 Recovery 状态直到所有进入 Recovery 状态时正在发送的数据段都成功地被确认，之后该发送方恢复 OPEN 状态，重传超时有可能中断 Recovery 状态。

# 6.1.1.6 Loss 状态

当一个 RTO 到期后，发送方进入 Loss 状态。所有正在发送的数据段标记为丢失，拥塞窗口设置为一个段，发送方因此以慢启动算法增大拥塞窗口。Loss 和 Recovery 状态的区别是:Loss 状态下，拥塞窗口在发送方设置为一个段后增大，而 Recovery 状态下，拥塞窗口只能被减小。Loss 状态不能被其他的状态中断，因此，发送方只有在所有 Loss开始时正在传输的数据都得到成功确认后，才能退到 Open 状态。例如，快速重传不能在 Loss 状态期间被触发，这和 NewReno 规范是一致的。

当接收到的 ACK 的确认已经被之前的 SACK 确认过，这意味着我们记录的 SACK信息不能反映接收方的实际状态，此时，也会进入 Loss 状态。

调用tcp_enter_loss进入 Loss 状态，如下：

```txt
/* 
Location: 
net/ipv4/tcp_input.c 
Function: 
Enter Loss state. If we detect SACK reneging(违约), forget all SACK information and reset tags completely, otherwise preserve SACKs. If receiver dropped its ofo?? queue, we will know this due to reneging detection. 
Parameter: 
sk: 传输控制块 
*/ 
```

void tcp-enter_loss(struct sock \*sk)   
{ const struct inlet_connection SOCK \*icsk $=$ inlet_csk(sk); struct tcp_sock \*tp $=$ tcp_sk(sk); struct sk BUFF \*skb; bool new_recovery $=$ icsk->icsk_ca_state $<$ TCP_CA_Recovery; bool is_reneg; /\* is receiver reneging on SACKs? \*/ /\* Reduce ssthresh if it has not yet been made inside this window. \*/ if (icsk->icsk_ca_state $\Leftarrow$ TCP_CA_Disorder ||!after(tp->high_seq,tp->snd_una)|| (icsk->icsk_ca_state $= =$ TCP_CA_Loss &&！icsk->icsk_retransmits)) { /\*保留当前的阈值\*/ tp->prior_ssthresh $=$ tcp_current_ssthresh(sk); /\*计算新的阈值\*/ tp->snd_ssthresh $=$ icsk->icsk_ca_opss>sssthresh(sk); /\*发送CA_EVENT_LOSS拥塞事件给具体拥塞算法模块\*/ tcp_ca_event(sk，CA_EVENT_LOSS); /\* 在下面的函数中会做以下两个事情： tp->undomarker $= \mathrm{tp - > }$ SND_una; /\*Retransmission still in flight may cause DSACKs later. undo_retrans在恢复拥塞控制之前可进行撤销的重传段数 $\text{？}\text{？}$ retrans_out重传并且还未得到确认的TCP段的数目 \*/ tp->undo_retrans $=$ tp->retrans_out ? : -1; \*/ tcp_init Undo(tp);

关于 tcp_init_undo 更多内容, 请参见8.12.3.1.

/\*拥塞窗口大小设置为1\*/tp->snd_cwnd $\equiv$ 1;/\*snd_cwnd_cnt表示自从上次调整拥塞窗口到目前为止接收到的总ACK段数，自然设置为 $0 / \ast /$ tp->snd_cwnd_cnt $\equiv$ 0;/\*记录最近一次检验拥塞窗口的时间\*/tp->snd_cwnd_stamp $\equiv$ tcp_time_stamp; $/^{*}$ 设置重传的但还未得到确认的TCP段的数目为零\*/tp->retrans_out $\equiv$ 0;/\*丢失的包\*/tp->lost_out $\equiv$ 0;/\*查看当前的tp里是否由SACK选项字段，有的话，返回1，没有的话返回O根据这一点来判断是否需要重置tp中选择确认的包的个数为0\*/if（tcp_is_reno(tp))tcp_reset_reno_sack(tp);

关于 tcp_is_reno 更多的内容，请参见8.9.1.1; 关于 tcp_reset_reno_sack 更多的内容，请参见8.9.2.2.

1 skb $=$ tcp_write_queue_head(sk);   
2 /\*判断接受者是否认为SACK违约\*/   
3 is_reneg $\equiv$ skb &&(TCP_SKB_CB(sk)->sacked&TCPCB_SACKED_ACKED);   
4 if(is_reneg){

NET_INC_STATSBH(sock_net(sk),LINUX_MIB_TCPSSACKRENEGING);tp->sacked_out $= 0$ tp->fackets_out $= 0$ ·

关于违约的讲解的更多内容，请参见8.3.1.

/\*清除有关重传的记忆变量\*/   
tcp_clear_all_retrans_hints(tp);   
tcp_for_write_queue(sk，sk）{ if $(\mathrm{skb} ==$ tcp_send_head(sk)) break; TCP_SKB_CB(sk)->sacked& $=$ （\~TCPCB_TAGITS)|TCPCB_SACKED_ACKED; if(!(TCP_SKB_CB(sk)->sacked&TCPCB_SACKED_ACKED)||is_reneg）{ /\*清除ACK标志\*/ TCP_SKB_CB(sk)->sacked& $=$ \~TCPCB_SACKED_ACKED; /\*添加LOST标志\*/ TCP_SKB_CB(sk)->sacked|=TCPCB_LOST; /\*统计丢失段的数量\*/ tp->lost_out+=tcp_skb_pcount(sk); /\* 重传的最大序号????\*/ tp->retransmit_high $=$ TCP_SKB_CB(sk)->end_seq; }   
1   
/\*确认没有被确认的TCP段的数量left_out\*/   
tcp_checkleft_out(tp);

关于 tcp_verify_left_out 更多的内容，请参见8.10.1.2。

/\*Timeout in disordered state after receiving substantial DUPACKs \* suggests that the degree of reordering is over-estimated. \*/ if (icsk->icsk_ca_state <= TCP_CA_Disorder && tp->sacked_out >= sysct1 tcp_reordering) /\*重新设置reordering\*/ tp->reordering = min_t(unsigned int,tp->reordering, sysct1 tcp_reordering); /\*设置拥塞状态\*/ tcp_set_ca_state(sk,TCP_CA_Loss); /\*记录发生拥塞时的snd.nxt\*/ tp->high_seq $=$ tp->snd_nxt; /\*设置ecn_flags，表示发送方进入拥塞状态\*/ tcp_ecn_queue_cwr(tp); /\*F-RTO RFC5682 sec 3.1 step 1: retransmit SND.UNA if no previous \* loss recovery is underway except recurring timeout(s) on \* the same SND.UNA (sec 3.2). Disable F-RTO on path MTU probing \*/ tp->frto $=$ sysct1 tcp_frto && (new_recovery || icsk->icsk_retransmits)&& !inet_csk(sk)->icsk_mtup.probe_size;   
}

# 6.1.2 显式拥塞通知 (ECN)

在处理网络中的拥塞的时候，有一种方法叫做显式拥塞控制。从名字上看就是，我们会直接收到关于拥塞的通知。至于是如何实现呢？我这里先简单说一下原理，然后再

细细说明。当 TCP 传递的时候，路由器使用 IP 首部的一对比特位来记录是否出现了拥塞。这样，当 TCP 段到达后，接收方知道报文段是否在某个位置经历过拥塞。但是，需要注意的是，发送方才是真正需要了解是否发生了拥塞状况。因此，接收方使用下一个ACK 通知发送方有拥塞发生。然后，发送方作出响应，缩小自己的拥塞窗口。

我们知道路由器是网络层的设备，所以说，如果想要路由器帮忙记录拥塞控制就必然需要 IP 的支持。当然，除此之外，也需要 TCP 层的支持。

下面是具体的叙述。

# 6.1.2.1 IP 对 ECN 的支持

IP 首部中的八位的服务类型域 (TOS) 原先在 RFC791 中定义为表明包的发送优先级、时延、吞吐量、可靠性和消耗等特征。在 RFC2474 中被重新定义为包含一个 6 位的区分服务码点 (DSCP) 和两个未用的位。DSCP 值表明一个在路由器上配置的和队列相关联的发送优先级。IP 对 ECN 的支持用到了 TOS 域的剩下的两位。

基本的定义如下：

```txt
enum {
    INET_ECN_NOT_ECT = 0, //TOS 后两位为 00: 表示不支持 ECN
    INET_ECN_ECT_1 = 1, //TOS 后两位为 01: 表示支持 ECN
    INET_ECN_ECT_0 = 2, //TOS 后两位为 10: 表示支持 ECN?? 区别
    INET_ECN_CE = 3, //TOS 后两位为 11: 表示在某路由器处出现拥塞
    INET_ECN_MASK = 3, //ECN 域的掩码
};
```

当路由器检测到拥塞的时候，设置 ECN 域为 11。当然在设置之前会检测之前是否出现了拥塞。没有的时候再进行设置。

1 /\* Location: include/net/inet_ecn.h   
4   
5   
6   
7   
8   
9   
10 iph:ip 头部。   
12 \*/   
13 static inline int IP_ECN_set_ce(struct iphdr \*iph) { u32 check $=$ (_force u32)iph->check; //force????,为啥32位 u32ecn $=$ (iph->tos + 1)& INET_ECN_MASK;   
17   
18 \*/ \*After the last operation we have (in binary): \*INET_ECN_NOT_ECT $\Rightarrow$ 01 \*INET_ECN_ECT_1 $\Rightarrow$ 10 \*INET_ECN_ECT_0 $\Rightarrow$ 11 \*INET_ECN_CE $\Rightarrow$ 00   
21   
22   
23   
24   
25 /\*不支持ECN的返回0，已经设置拥塞的不重复设置，返回。\*/ if(!ecn&2))

27 return!ecn;   
28 $\text{水}$ 30 \*The following gives us: \* INET_ECN_ECT_1 $\Rightarrow$ check $+ =$ htons(OxFFFD) \* INET_ECN_ECT_0 $\Rightarrow$ check $+ =$ htons(OxFFFE)   
33 \*/ check $+ =$ (_force u16)htons(OFFFFB) $^+$ (_force u16)htons(ecn);   
35 iph->check $=$ (_force _sum16)(check $^+$ (check $\coloneqq$ 0xFFFF)); //重新计算校验码   
37 iph->tos |= INET_ECN_CE; /\* 把 ECN 域设置为 11，表示发生了拥塞 \*/   
38 return 1;   
39 }

这里计算校验码需要我们仔细分析一下，remain to do.????

# 6.1.2.2 TCP 对 ECN 的支持

路由器使用 IP 包的相关域来设置相关标志位来表示是否发生了拥塞. 而主机使用TCP 的首部来告知发送方, 网络正在经历拥塞。

TCP 使用 6 位保留位（Reserved）的后两位来支持 ECN。两个新的标志 CWR、ECE 含义如下

CWR CWR 为发送端缩小拥塞窗口标志，用来通知接收端它已经收到了设置 ECE 标志的 ACK。

ECE ECE 有两个作用，在 TCP 三次握手时表明 TCP 端是否支持 ECN；在传输数据时表明接收到的 TCP 段的 IP 首部的 ECN 被设置为 11，即接收端发现了拥塞.

当两个支持 ECN 的 TCP 端进行 TCP 连接时，它们交换 SYN、SYN+ACK、ACK段。对于支持 ECN 的 TCP 端来说，SYN 段的 ECE 和 CWR 标志都被设置了，SYN的 ACK 只设置 ECE 标志。

相关宏定义如下:

```c
1 /* 本端支持 ECN */
2 #define TCP_ECN_OK 1
3 /* 本端被通知了拥塞，此时作为发送方 */
4 #define TCP_ECN_queueUE_CWR 2
5 /* 通知对端发生了拥塞，此时作为接收方 */
6 #define TCP_ECN_DEMAND_CWR 4
7 #define TCP_ECN_SEEN 8
8 #define TCPHDR_FIN 0x01
9 #define TCPHDR_SYN 0x02
10 #define TCPHDR_RST 0x04
11 #define TCPHDR_PSH 0x08
12 #define TCPHDR_ACK 0x10
13 #define TCPHDR_URG 0x20
14 #define TCPHDR_ECE 0x40
15 #define TCPHDR_CWR 0x80
16
17 #define TCPHDR_SYN_ECN (TCPHDR_SYN / TCPHDR_ECE / TCPHDR_CWR)
```

# 6.1.3 拥塞控制状态的处理及转换

在章，客户端在第二次连接的时候会调用 tcp_ack 我们对其分析后，发现其中会调用 tcp_fastretrans_alert 拥塞控制状态处理函数，这里我们就主要讲解这一函数。

# 6.1.3.1 拥塞控制状态处理:tcp_fastretrans_alert

/*   
Location:   
net/ipu4/tcp_input.c   
Function:   
Process an event, which can update packets-in-flight not trivially. Main goal of this function is to calculate new estimate for left_out, taking into account both packets sitting in receiver's buffer and packets lost by network. Besides that it does CWND reduction, when packet loss is detected and changes state of machine. It does _not_ decide what to send, it is made in function tcp_xmit_retransmit_queue().   
Parameter:   
sk:传输控制块. acked:得到确认的包的数量 prior_unsacked:在拥塞控制状态转换之前，从发送队列发出而未得到确认的TCP段的数目. is_dupack:是否有重复ACK??需要好好分析分析。 flag：标志位   
\*/ static void tcp_fastretransalert(struct sock \*sk, const int acked, const int prior_unsacked, bool is_dupack,int flag)   
{ struct inlet_connection SOCK \*icsk $=$ inlet_csk(sk); struct tcp_sock \*tp $=$ tcp_sk(sk); /\*\*/ bool do_LOst $=$ is_dupack || ((flag & FLAG_DATA_SACKED) && (tcp_fackets_out(tp) > tp->reordering)); /\*means what????*/ int fast_rexit $= 0$ /\*WARN 的返回值是什么呢？ 如果没有待确认的包以及并且选择确认的包的数量不为0, 就置其为零。 $\ast /$ if (WARN_ON(!tp->packets_out && tp->sacked_out)) tp->sacked_out = 0; /\* 如果选择确认的包的数量为零，并且未确认与已选择确认之间存在段 \*/ if (WARN_ON(!tp->sacked_out && tp->fackets_out)) tp->fackets_out = 0;

接下来，拥塞状态处理机启动。

/\*Now state machine starts. A.ECE,hence prohibit(阻止）cund undoing，the reduction is required. 由于接收到显式拥塞通知，因此禁止窗口撤销，并且开始减小拥塞窗口。   
\*/ if (flag&FLAG_ECE) tp->prior_ssthresh $= 0$

如果接收到的 ACK 指向已记录的 SACK，这说明我们记录的 SACK 并没有反应接收方的真实状态。也就是说接收方现在已经处于严重的拥塞状态或者在处理上有 bug，那么我们接下来就要按照重传超时的方式去处理。因为按照正常的逻辑流程，接受的 ACK不应该指向已记录的 SACK，而应指向 SACK 并未包含的，这说明接收方由于拥塞已经把 SACK 部分接收的段已经丢弃或者处理上有 BUG，我们必须需要重传了。

```txt
/\*B.In all the states check for reneging SACKs. \*/ if (tcp_check_sack_reneging(sk，flag)) return; 
```

关于 tcp_check_sack_reneging 更多的内容，请参见8.9.1.3。

```txt
/\*C. Check consistency of the current state. \*/ tcp_checkleft_out(tp); 
```

查看是否从发送队列发出的包的数量是否不小于发出主机的包的数量。

关于 tcp_verify_left_out 的更多的内容，请参见8.10.1.2.

接下来检测从拥塞状态返回的条件，当 high_seq 被确认时，结束拥塞状态，进入Open 状态。

```c
/* D. Check state exit conditions. State can be terminated
* when high_seq is ACKed. */
if (icsk->icsk_ca_state == TCP_CA_Open) {
//根据条件判断是否输出警告信息
WARN_ON(tp->retrans_out != 0);
//消除上一次的重传时间戳
tp->retrans_stamp = 0;
} else if (!before(tp->snd_una, tp->high_seq)) {
/* 进入的条件是tp->snd_una>tp->high_seq，即
拥塞时，记录的SND NXT被确认了。拥塞情况已经好转很多了。
此时如果拥塞状态不处于Open状态，可根据当前的状态结束回
到Open状态。
*/
switch (icsk->icsk_ca_state) {
case TCP_CA_CWR:
/* CWR is to be held something *above* high_seq
* is ACKed for CWR bit to reach receiver. */
if(tp->snd_una != tp->high_seq) {
//结束窗口减小
tcp_end_cwnd_reduction(sk);
//回到Open状态
tcp_set_ca_state(sk, TCP_CA_Open);
}
break; 
```

关于 tcp_end_cwnd_reduction 的更多内容，请参见8.12.2.2.

```c
case TCP_CA_Recovery: //判断对方是否提供了SACK服务，提供，返回0，否则返回1 if（tcp_is_reno(tp)) //设置sacked_out为0 tcp_reset_reno_sack(tp); /*尝试从Recovery状态撤销 成功，就直接返回。 \*/ if（tcp_try_undo_recovery(sk)) return; //结束拥塞窗口缩小 tcp_end_cwnd_reduction(sk); break; }
```

关于 tcp_try_undo_recovery 的更多内容，请参见8.12.3.5 ; 关于 tcp_end_cwnd_reduction的更多内容，请参见8.12.2.2.

/* Use RACK (Restransmit Ack????) to detect loss 有时间好好分析。   
\*/   
if (sysctl tcp_recovery & TCP_RACK_LOST_RETRANS && tcp rack_mark Losingsk)) flag $\vDash$ FLAG_LOST_RETRANS; //说明重传的丢失   
\*/ E.Process state. \*/   
switch(icsk->icsk_ca_state){ caseTCP_CA_Recovery: //判断是否没有段被确认 if(!flag&FLAG_SND_UNA_ADVANCED)){ //判断是否启用了SACK，未启用返回1，并且收到的是重复的ACK, if (tcp_is_reno(tp)&&is_dupack) tcp_add_reno_sack(sk); //记录接收到的重复的ACK数量 }else{ /\*确认了部分重传的段\*/ if (tcp_try_undo_partial(sk,acked,prior_unsacked,flag)) return; /\*Partial ACK arrived. Force fast retransmit. \*/ do_loss $=$ tcp_is_reno(tp)|| tcp_fackets_out(tp)>tp->reordering; } if (tcp_try_undo_dsack(sk)){ tcp_try_keep_open(sk); return; } break;

关于 tcp_add_reno_sack 的更多内容，请参见8.9.2.3; 关于 tcp_try_undo_partial的更多的内容，请参见8.12.3.4;

```cpp
case TCP_CA_Loss:  
    tcp_process_loss(sk, flag, is_dupack);  
    if (icsk->icsk_ca_state != TCP_CA_Open && ! (flag & FLAG_LOST_RETRANS))  
        return;  
/* Change state if cumd is undone or retransmits are lost */  
default: 
```

```c
//判断是否开启了 SACK，设启用返回1
if (tcp_is_reno(tp)) {
    if (flag & FLAG_SND_UNA_ADVANCED)
        tcp_reset_reno_sack(tp); //重置 sacked
    if (is_dupack)
        tcp_add_reno_sack(sk); //记录接收到的 sack
}
```

# 6.2 拥塞控制引擎

在Linux中,实现了多种不同的拥塞控制算法。为了简化拥塞控制算法的编写，Linux的开发者们实现了一套拥塞控制引擎或者说是框架，专门用于实现拥塞控制算法。只要实现必要的接口，即可完成一套拥塞控制算法，极大地简化了拥塞控制算法的开发工作。

# 6.2.1 接口

# 6.2.1.1 tcp_congestion_ops

struct tcp_congestion_ops结构体描述了一套拥塞控制算法所需要支持的操作。其原型如下：

```c
/* Location include/net/tcp.h */
/* struct tcp_congestionOps {
/* 链接注册到系统中不同的各种拥塞算法 */
struct list_head list;
u32 key; /* 算法名称的哈希值 */
u32 flags;
/* 初始化私有数据（可选）*/
void (*init)(struct sock *sk);
/* 释放私有数据（可选）
调用场合：
1. 关闭套接口
2. 传输控制块选择了另一种拥塞控制算法 */
void (*release)(struct sock *sk);
/* 返回 ssthresh（必须实现）*/
u32 (*ssthresh)(struct sock *sk);
/* 计算新的拥塞窗口（必须实现）*/
void (*cong_Avoid)(struct sock *sk, u32 ack, u32 acked);
/* 在改变 ca_state 前会被调用（可选）*/
void (*set_state)(struct sock *sk, u8 new_state);
/* 处理拥塞窗口相关的事件（可选）*/
void (*cwnd_event)(struct sock *sk, enum tcp_ca_event ev);
/* 处理 ACK 包到达事件（可选）*/
void (*in_ACK_event)(struct sock *sk, u32 flags);
/* 用于撤销“缩小拥塞窗口”（可选）*/
u32 (*undo_cwnd)(struct sock *sk);
/* 有段被确认时会调用此函数（可选）
num_acked 为此次 ACK 确认的段数 */
void (*pkts_acked)(struct sock *sk, u32 num_acked, s32 rtt_us);
/* 为 inlet(diag 准备的获取信息的接口（可选）*/
size_t (*get_info)(struct sock *sk, u32 ext, int *attr,
						union tcp_CC_info *info);
/* 拥塞控制算法的名称 */
char name[TCP_CA_NAME_MAX];
struct module *owner;
};
```

# 6.2.1.2 拥塞控制事件

为了将拥塞控制算法相关的部分抽取处理，Linux 的开发者们采用了事件机制，即在发生和拥塞控制相关的事件后，调用拥塞控制算法中的事件处理函数，以通知拥塞控制模块，具体发生了什么。而作为实现拥塞控制算法的开发者，则无需关心事件是如何发生的，以及相关的实现，只要专注于事件所对应的需要执行的算法即可。

当发生相关事件是会调用cwnd_event函数。该函数会传入一个枚举值作为参数，代表具体发生的事件。该枚举值的定义如下：

```c
1 /\*   
2 Location   
3 include/net/tcp.h   
4 include/net/tcp.h   
5 \*/   
6 enum tcp_ca_event{ /\*首次传输，且无已发出但还未确认的包\*/ CA_EVENT_TX_START, /\*拥塞窗口重启\*/   
10 CA_EVENT_CWND_RESTART, /\*拥塞恢复结束\*/   
11 CA_EVENT_COMPLETE_CWR,   
13 \*/超时，进入loss状态\*/   
14 CA_EVENT_LOSS, /\*ECN被设置了，但CE没有被置位\*/   
16 CA_EVENT_ECN_NO_CE, /\*收到了设置了CE位的IP报文\*/   
18 CA_EVENT_ECN_IS_CE, /\*延迟确认已被发送\*/   
20 CA_EVENT-delayED_ACK,   
21 CA_EVENT_NON_DELAYED_ACK,   
22 }；
```

当收到了 ACK 包时，会调用in_ack_event()。此时也会传递一些信息给拥塞控制算法。相关的定义如下：

```c
enum tcp_ca ACK_event_flags {
    CA_ACK_SLOWPATH = (1 << 0), /* 在慢速路径中处理 */
    CA_ACK_WIN_UPDATE = (1 << 1), /* 该 ACK 更新了窗口大小 */
    CA_ACK_ECE = (1 << 2), /* ECE 位被设置了 */
};
```

# 6.2.1.3 tcp_register_congestion_control()

该函数用于注册一个新的拥塞控制算法。

```c
int tcp_register_congestion_control(struct tcp_congestionOps *ca)
{
    int ret = 0;
    /* 所有的算法都必须实现 ssthresh 和 cong_avoid ops */
    if (!ca->ssthresh || !ca->cong_avoid) {
        pr_err("%s does not implement required ops\n", ca->name);
        /* 如果没实现，则返回错误 */
        return -EINVALID;
    }
    /* 计算算法名称的哈希值，加快比对速度。 */
    ca->key = jhash(ca->name, sizeof(ca->name), strlen(ca->name));
    spin_lock(&tcp_cong_list_lock);
    if (ca->key == TCP_CA_UNSPEC || tcp_ca_find_key(ca->key)) {
        /* 如果已经注册被注册过了，或者恰巧 hash 值重了（极低概率）,
        * 那么返回错误值。
        */
        pr notice("%s already registered or non-unique key\n",
        ca->name);
}
```

ret $=$ -EEXIST;   
} else { /\*将算法添加到链表中\*/ list_addTAIL_rcu(&ca->list，&tcp_cong_list); prDebug("%s registered\n"，ca->name); 1 spin_unlock(&tcp_cong_list_lock); return ret;

其中，tcp_ca_find_key函数通过哈希值来查找名称。jash 是一种久经考验的性能极佳的哈希算法。据称，其计算速度和产生的分布都很漂亮。这里计算哈希值正是使用了这种哈希算法。早些版本的内核查找拥塞控制算法，是通过名字直接查找的，如下:

```c
/* Simple linear search, don't expect many entries! */   
static struct tcp_congestionOps \*tcp_ca_find(const char \*name)   
{ struct tcp_congestionOps \*e; list_for_each_entry_rcu(e,&tcp_cong_list，list）{ if（strcmp(e->name，name）==0) return e; } return NULL; 
```

可以看到，每次查找都要对比字符串，效率较低。这里为了加快查找速度，对名字进行了哈希，并通过哈希值的比对来进行查找。

/\*Simple linear search,not much in here.\*/   
struct tcp_congestionOps \*tcp_ca_find_key(u32 key)   
{ struct tcp_congestion ops \*e; list_for_each_entry_rcu(e,&tcp_cong_list，list）{ if $(\mathrm{e - > }$ key $= =$ key) return e; } return NULL;

一般情况下，拥塞控制算法都作为单独的模块实现。以便于更加方便地使用。在模块初始化时，调用 tcp_register_congestion_control函数来进行注册，之后，即可使用新的拥塞控制算法。

# 6.2.1.4 tcp_unregister_congestion_control()

与注册相对应的，tcp_unregister_congestion_control用于撤销一个拥塞控制算法。其实现如下：

```c
void tcp unregister_congestion_control(struct tcp_congestionOps *ca) {
    spin_lock(&tcp_cong_list_lock);
    /* 删除该拥塞控制算法 */
    list_del_rcu(&ca->list);
    spin_unlock(&tcp_cong_list_lock);
    /* Wait for outstanding readers to complete before the
        * module gets removed entirely.
        */
        * A try_module_get() should fail by now as our module is
        * in "going" state since no refs are held anymore and
        * module_exit() handler being called.
        */
        synchronize_rcu();
} 
```

# 6.2.2 CUBIC 拥塞控制算法

Cubic 算法是 Linux 中默认的拥塞控制算法。

# 6.2.2.1 CUBIC 算法介绍

CUBIC 算法的思路是这样的：当发生了一次丢包后，它将此时的窗口大小定义为$W _ { m a x }$ ，之后，进行乘法减小。这里与标准 TCP 不同，乘法减小时不是直接减小一半，而是乘一个常系数 $\beta$ 。快重传和快恢复的部分和标准 TCP 一致。当它进入到拥塞避免阶段以后，它根据 cubic 函数凹的部分进行窗口的增长，直至到达 $W _ { m a x }$ 为止。之后，它会根据 cubic 函数凸的部分继续增长。

![](images/85cac597a15031247a56480960938e8b2b7281571eca0a58e29d01f7771eee73.jpg)  
图 6.2: Cubic 函数

# 6.2.2.2 模块定义

在 Linux 中，拥塞控制算法是作为模块单独实现的。Cubic 也不例外。首先，定义了模块的基本信息

```c
module_init(cubictcp register);   
module_exit(cubictcp_unregister);   
MODULE_AUTHOR("Sangtae Ha, Stephen Hemninger");   
MODULE牌照("GPL");   
MODULE_DESCRIPTION("CUBIC TCP");   
MODULE_VERSION("2.3"); 
```

这部分定义包括了模块作者、所使用的许可证、版本等等。其中，最重要的是定义了模块的初始化函数，和模块被卸载时所要调用的函数。初始化函数如下：

```c
static int __init cubictcp register(void)  
{  
    BUILD_bug_ON(sizeof(struct bibtcp) > ICSK_CA_PRIV_SIZE);  
    /* 预先计算缩放因子（此时假定 SRTT 为 100ms） */  
    beta_scale = 8*(BICTCP_BETA_SCALE + beta) / 3 / (BICTCP_BETA_SCALE - beta);  
    cube_rtt_scale = (bic_scale * 10); /* 1024*c/rtt */  
    /* 计算公式 (xmax-cumd) = c/rtt * K^3 中的 K  
        * 可得 K = cubic_root((xmax-cumd)*rtt/c)  
        * 注意，这里 K 的单位是 bibtcp_HZ=2^10，而不是 HZ  
        */  
    /* 1/c * 2^2* bibtcp_HZ * srtt */  
    cube_factor = 1ull << (10+3*BICTCP_HZ); /* 2^40 */  
    /* divide by bic_scale and by constant Srtt (100ms) */  
    do_div(cube_factor, bic_scale * 10);  
    return tcp_register_congestion_control(&cubictcp);  
}
```

在初始化时，根据预先设定好的参数，按照 Cubic 算法的公式计算好cube_factor。之后，调用tcp_register_congestion_control函数注册 Cubic 算法。Cubic 算法所实现的操作如下：

```txt
static struct tcp_congestionOps cubictcp__readmostly = { .init = bibtcp_init, .ssthresh = bibtcp_recalc_ssthresh, .cong_avoid = bibtcp_cong_avoid, .set_state = bibtcp_state, .undo_cwnd = bibtcp_undo_cwnd, .cwnd_event = bibtcp_cwnd_event, .pkts_aced = bibtcp_aced, .owner = THISModule, .name = "cubic", }; 
```

根据这里定义好的操作，我们就可以去理解对应的实现。在模块被卸载时，将调用cubictcp_unregister函数。该函数只有一个职责——将 Cubic 算法从拥塞控制引擎中注销掉。

```txt
static void __exit cubictcp_unregister(void)   
{ tcp_unregister_congestion_control(&cubictcp);   
}
```

# 6.2.2.3 基本参数及初始化

CUBIC 的所需的基本参数定义如下：

```c
/\*BICTCPParameters\*/   
structbictcp{ u32 cnt; /\*每次cumd增长1/cnt的比例\*/ u32 last_max_cwnd; /\*snd_cwnd之前的最大值\*/ u32 loss_cwnd; /\*最近一次发生丢失的时候的拥塞窗口\*/ u32last_cwnd; /\*最近的snd_cwnd\*/ u32last_time; /\*更新last_cwnd的时间\*/ u32bic_origin_point;\*/bic函数的初始点\*/ u32 bic_K; /\*从当前一轮开始到初始点的时间\*/ u32 delay_min; /\*最小延迟（msec<<3）\*/ u32 epoch_start; /\*一轮的开始\*/ u32ack_cnt; /\*ack的数量\*/ u32tcp_cwnd; /\*estimated tcp cwnd\*/ u16unused; u8 sample_cnt; /\*用于决定curr_rtt的样本数\*/ u8 found; /\*是否找到了退出点？\*/ u32 round_start; /\*beginning of each round\*/ u32end_seq; /\*end_seqofthe round\*/ u32last_ACK; /\*last time when the ACK spacing is close\*/ u32curr_rtt; /\*the minimum rtt of current round \*/   
}； 
```

初始化时，首先重置了 CUBIC 所需的参数，之后，将loss_cwnd设置为 0。因为此时尚未发送任何丢失，所以初始化为 0。最后根据是否启用 hystart 机制来决定是否进行相应的初始化。最后，如果设置了initial_ssthresh，那么就用该值作为初始的snd_ssthresh。

static void bictcp_init(struct sock \*sk)   
{ struct bictcp \*ca $=$ inet_csk_ca(sk); bictcp_reset(ca); ca->loss_cwnd $= 0$ if (hystart) bictcp_hystart_reset(sk); if(!hystart &&initial_ssthresh) tcp_sk(sk)\~>snd_ssthresh $=$ initial_ssthresh;   
}

其中，bictcp_reset函数将必要的参数都初始化为 0 了。

```c
static inline void bictcp_reset(struct bictcp *ca) { ca->cnt = 0; ca->last_max_cwnd = 0; ca->last_cwnd = 0; 
```

```c
6 ca->last_time = 0;   
7 ca->bic_origin_point = 0;   
8 ca->bic_K = 0;   
9 ca->delay_min = 0;   
10 ca->epoch_start = 0;   
11 ca->ack_cnt = 0;   
12 ca->tcp_cwnd = 0;   
13 ca->found = 0;   
14 } 
```

# 6.2.2.4 ssthresh 的计算

门限值的计算过程在bictcp_recalc_ssthresh函数中实现。

static u32 bibtcp_recalc_ssthresh(struct sock \*sk)   
{ const struct tcp_sock \*tp $=$ tcp_sk(sk); struct bibtcp \*ca $=$ inlet_csk_ca(sk); ca->epoch_start $= 0$ /\*end of epoch\*/ /\* Wmax and fast convergence \*/ if(tp->snd_cwnd < ca->last_max_cwnd && fast_convergence) ca->last_max_cwnd $=$ (tp->snd_cwnd \* (BICTCP_BETA_SCALE + beta)) / (2 \* BICTCP_BETA_SCALE); else ca->last_max_cwnd $=$ tp->snd_cwnd; ca->loss_cwnd $=$ tp->snd_cwnd; return max((tp->snd_cwnd \*beta)/BICTCP_BETA_SCALE,2U);   
1

这里涉及到了 Fast Convergence 机制。该机制的存在是为了加快 CUBIC 算法的收敛速度。在网络中，一个新的流的加入，会使得旧的流让出一定的带宽，以便给新的流让出一定的增长空间。为了增加旧的流释放的带宽量，CUBIC 的作者引入了 Fast Convergence机制。每次发生丢包后，会对比此次丢包时拥塞窗口的大小和之前的拥塞窗口大小。如果小于了之前拥塞窗口的最大值，那么就说明可能是有新的流加入了。此时，就多留出一些带宽给新的流使用，以使得网络尽快收敛到稳定状态。

# 6.2.2.5 慢启动和拥塞避免

处于拥塞避免状态时，计算拥塞窗口的函数为bictcp_cong_avoid。

static void bictcp_cong_avoid(struct sock \*sk，u32 ack，u32 acked)   
{ struct tcp_sock \*tp $=$ tcp_sk(sk); struct bictcp \*ca $=$ inlet_csk_ca(sk); if(!tcp_is_cwndlimited(sk)) return; /\*当tp->snd_cumd $<$ tp->snd_ssthresh时， \*让拥塞窗口大小正好等于ssthresh的大小。并据此计算acked的大小。

```c
if (tcp_in_slow_start(tp)) { if (hystart && after(ack, ca->end_seq)) bictcp_hystart_reset(sk); acked = tcp Slow start(tp, acked); if (!acked) return; } bictcp_update(ca, tp->snd_cwnd, acked); tcp_congavoid.ai(tp, ca->cnt, acked);   
} 
```

这里不妨举个例子。如果 ssthresh 的值为 6，初始 cwnd 为 1。那么按照 TCP 的标准，拥塞窗口大小的变化应当为 1,2,4,6 而不是 1,2,4,8。当处于慢启动的状态时，acked 的数目完全由慢启动决定。慢启动部分的代码如下：

```c
u32 tcp Slow_start(struct tcp_sock *tp, u32 acked)  
{ /* 新的拥塞窗口的大小等于 ssthresh 和 cumd 中较小的那一个 */ u32 cwnd = min(tp->snd_cwnd + acked, tp->snd_ssthresh); /* 如果新的拥塞窗口小于 ssthresh, 则 acked=0。 * 否则 acked 为超过 ssthresh 部分的数目。 */ acked -- cwnd - tp->snd_cwnd; tp->snd_cwnd = min(cwnd, tp->snd_cwnd_clamp); return acked; }
```

也就是说，如果满足 $c w n d < s s t h r e s h$ ，那么，bictcp_cong_avoid就表现为慢启动。否则，就表现为拥塞避免。拥塞避免状态下，调用bictcp_update来更新拥塞窗口的值。

static inline void bictcp_update(struct bictcp *ca, u32 cwnd, u32 acked)  
{u32 delta, bic_target, max_cnt;u64 offs, t;ca->ack_cnt += acked; /*统计ACKed packets的数目*/if(ca->last_cwnd == cwnd &&(s32)(tcp_time_stamp - ca->last_time) <= HZ / 32)return;/\*CUBIC函数每个时间单位内最多更新一次ca->cnt的值。\*每一次发生cund减小事件，ca->epoch_start会被设置为0.\*这会强制重新计算ca->cnt。\*/if(ca->epoch_start&&tcp_time_stamp $= =$ ca->last_time)goto tcp FRIENDliness;ca->last_cwnd = cwnd;ca->last_time = tcp_time_stamp;if(ca->epoch_start $= = 0$ {ca->epoch_start $\equiv$ tcp_time_stamp; /*记录起始时间\*/ca->ack_cnt = acked; /*开始计数\*/ca->tcp_cwnd $\equiv$ cwnd; /*同步cubic的cumd值\*/

```c
if (ca->last_max_cwnd <= cwnd) { ca->bic_K = 0; ca->bic_origin_point = cwnd; } else { /* 根据公式计算新的 K 值 * (xmax-cwnd) * (srtt>>3 / HZ) / c * 2^(3*bictcp_HZ) */ ca->bic_K = cubic_root(cube_factor * (ca->last_max_cwnd - cwnd)); ca->bic_origin_point = ca->last_max_cwnd; } } /* cubic function - calc*/ /* calculate c * time^3 / rtt, * while considering overflow in calculation of time^3 * (so time^3 is done by using 64 bit) * and without the support of division of 64bit numbers * (so all divisions are done by using 32 bit) * also NOTE the unit of those variables * time = (t - K) / 2^bictcp_HZ * c = bic_scale >> 10 * rtt = (srtt >> 3) / HZ * !!! The following code does not have overflow problems, * if the cwnd < 1 million packets !!! */ t = (s32)(tcp_time_stamp - ca->epoch_start); t += msecs_to_jiffies(ca->delay_min >> 3); /* change the unit from HZ to bictcp_HZ */ t <<= BICTCP_HZ; do_div(t, HZ); if (t < ca->bic_K) /* t - K */ offs = ca->bic_K - t; else offs = t - ca->bic_K; /* c/rtt * (t-K)^3 */ delta = (cube_rtt_scale * offs * offs * offs) >> (10+3*BICTCP_HZ); if (t < ca->bic_K) /* below origin*/ bic_target = ca->bic_origin_point - delta; else /* above origin*/ bic_target = ca->bic_origin_point + delta; /* 根据 cubic 函数计算出来的目标拥塞窗口值和当前拥塞窗口值，计算 cnt 的大小。*/ if (bic_target > cwnd) { ca->cnt = cwnd / (bic_target - cwnd); } else { ca->cnt = 100 * cwnd; /* 只增长一小点 */ } } /* The initial growth of cubic function may be too conservative * when the available bandwidth is still unknown. */ if (ca->last_max_cwnd == 0 && ca->cnt > 20) ca->cnt = 20; /* increase cwnd 5% per RTT */ 
```

```c
tcp Friendlyliness:
/* TCP 友好性 */
if (tcp Friendlyliness) {
u32 scale = beta_scale;
/* 推算在传统的 AIMD 算法下，TCP 拥塞窗口的大小 */
delta = (cwnd * scale) >> 3;
while (ca->ack_cnt > delta) {
    ca->ack_cnt -= delta;
    ca->tcp_cwnd++;
}
/* 如果 TCP 的算法快于 CUBIC，那么就增长到 TCP 算法的水平 */
if (ca->tcp_cwnd > cwnd) {
delta = ca->tcp_cwnd - cwnd;
max_cnt = cwnd / delta;
if (ca->cnt > max_cnt)
    ca->cnt = max_cnt;
}
/* 控制增长速率不高于每个 rtt 增长为原来的 1.5 倍 */
ca->cnt = max(ca->cnt, 2U);
}
```

在更新完窗口大小以后，CUBIC 模块没有直接改变窗口值，而是通过调用 tcp_cong_avoid_ai来改变窗口大小的。这个函数原本只是单纯地每次将窗口大小增加一定的值。但是在经历了一系列的修正后，变得较为难懂了。

```c
void tcp_cong_avoid.ai(struct tcp_sock *tp, u32 w, u32 acked)
{
    /* 这里做了一个奇怪的小补丁，用于解决这样一种情况：
    */
    /* 如果 w 很大，那么，snd_cwnd_cnt 可能会积累为一个很大的值。
    */
    /* 此后，w 由于种种原因突然被缩小了很多。那么下面计算处理的 delta 就会很大。
    */
    /* 这可能导致流量的爆发。为了避免这种情况，这里提前增加了一个特判。
    */
    if (tp->snd_cwnd_cnt >= w) {
        tp->snd_cwnd_cnt = 0;
        tp->snd_cwnd++;
    }
}
/* 累计被确认的包的数目 */
tp->snd_cwnd_cnt += acked;
if (tp->snd_cwnd_cnt >= w) {
    /* 窗口增大的大小应当为被确认的包的数目除以当前窗口大小。
    */
    /* 以往都是直接加一，但直接加一并不是正确的加法增加（AI）的实现。
    */
    /* 例如，w 为 10, acked 为 20 时，应当增加 20/10=2，而不是 1。
    */
    u32 delta = tp->snd_cwnd_cnt / w;
    tp->snd_cwnd_cnt -= delta * w;
    tp->snd_cwnd += delta;
}
tp->snd_cwnd = min(tp->snd_cwnd, tp->snd_cwnd_clamp);
}
```

# 6.2.2.6 拥塞状态机状态变更

当拥塞状态机的状态发生改变时，会调用set_state函数，对应到 CUBIC 模块中，就是bictcp_state函数。

static void bibtcp_state(struct sock \*sk，u8 new_state) { if (new_state $= =$ TCP_CA_Loss）{ bibtcp_reset(inet_csk_ca(sk)); bibtcp_hystart_reset(sk); }   
7

CUBIC 只特殊处理了一种状态：TCP_CA_Loss。可以看到，当进入了 LOSS 以后，就会调用bictcp_reset函数，重置拥塞控制参数。这样，拥塞控制算法就会重新从慢启动开始执行。

# 6.2.2.7 撤销 CWND 减小

CUBIC 通过返回当前拥塞窗口和上一次 LOSS 状态时的拥塞控制窗口中的最大值，来得到窗口缩小前的大小。

```c
static u32 bictcp Undo_cwnd(struct sock *sk) {
    struct bictcp *ca = inet_csk_ca(sk);
    return max.tcp(sk->snd_cwnd, ca->loss_cwnd);
} 
```

# 6.2.2.8 处理 CWND 事件

如果目前没有任何数据包在传输了，那么需要重新设定epoch_start。这个是为了解决当应用程序在一段时间内不发送任何数据时，now-epoch_start 会变得很大，由此，根据Cubic函数计算出来的目标拥塞窗口值也会变得很大。但显然，这是一个错误。因此，需要在应用程序重新开始发送数据时，重置epoch_start 的值。在这里CA_EVENT_TX_START事件表明目前所有的包都已经被确认了（即没有任何正在传输的包），而应用程序又开始发送新的数据包了。所有的包都被确认说明应用程序有一段时间没有发包。因而，在程序又重新开始发包时，需要重新设定 epoch_start的值，以便在计算拥塞窗口的大小时，仍能合理地遵循 cubic 函数的曲线。

static void bictcp_cwnd_event(struct sock \*sk, enum tcp_ca_event event)   
{ if (event $= =$ CA_EVENT_TX_START）{ struct bictcp \*ca $=$ inlet_csk_ca(sk); u32 now $=$ tcp_time_stamp; s32 delta; delta $=$ now-tcp_sk(sk)->lsndtime; /\*We were application limited (idle) for a while. \*Shift epoch_start to keep cwnd growth to cubic curve.

if(ca->epoch_start&&delta>0){ ca->epoch_start $+ =$ delta; if(after(ca->epoch_start，now)) ca->epoch_start $=$ now; } return; 1

# 6.2.2.9 收到 ACK

收到 ACK 后，Cubic 模块会重新计算链路的延迟情况。

/* Location: net/iju4/tcp_cubic.c */
	* Track delayed acknowledgment ratio using sliding window
	* ratio = (15*ratio + sample) / 16
	*/
static void bictcp_acked(struct sock *sk, u32 cnt, s32 rtt_us)
\{
 const struct tcp_sock *tp = tcp_sk(sk);
 struct bictcp *ca = inet_csk_ca(sk);
 u32 delay;
 /* Some calls are for duplicates without timetamps */
	if (rtt_us < 0)
		return;
 /* Discard delay samples right after fast recovery */
	if (ca->epoch_start && (s32)(tcp_time_stamp - ca->epoch_start) < HZ)
		return;
 delay $=$ (rtt_us $\ll  3$ ) / USEC_PER_MSEC;
 if (delay == 0)
		delay = 1;
 /* 当第一次调用或者链路延迟增大时,重设 delay_min 的值。 */
	if (ca->delay_min == 0 || ca->delay_min > delay)
	(ca->delay_min = delay;
 /* 当 cund 的大小大于阈值后,会触发 hystart 更新机制 */
	if (hystart && tcp_in Slow start(tp) &&
	tp->snd_cwnd >= hystart_low_window)
		hystart_update(sk, delay);
}

# 6.2.2.10 hystart

在长期的实践中，人们发现慢启动存在这样一个问题：如果慢启动时窗口变得很大（在大带宽网络中），那么如果慢启动的过程中发生了丢包，有可能一次丢掉大量的包（因为每次窗口都会加倍）。HyStart(Hybrid Slow Start) 是一种优化过的慢启动算法，可以避免传统慢启动过程中的突发性丢包，从而提升系统的吞吐量并降低系统负载。

在 HyStart 算法中，慢启动过程仍然是每次将拥塞窗口加倍。但是，它会使用 ACK空间和往返延迟作为启发信息，来寻找一个安全的退出点 (Safe Exit Points)。退出点即

结束慢启动并进入拥塞避免的点。如果在慢启动的过程中发生丢包，那么 HyStart 算法的表现和传统的慢启动协议是一致的。

初始化 HyStart 算法时，会记录当前的时间，上一次的 rtt 值，并重新统计当前的rtt 值。

```c
/* Location: net/ipv4/tcp_cubic.c */
static inline void bibtcp_hystart_reset(struct sock *sk) {
    struct tcp_sock *tp = tcp_sk(sk);
    struct bibtcp *ca = inet_csk_ca(sk);
    ca->round_start = ca->last_ACK = bibtcp_clock();
    ca->end_seq = tp->snd_nxt;
    ca->curr_rtt = 0;
    ca->sample_cnt = 0;
} 
```

每次接到 ACK 以后，如果 tcp 仍处于慢启动状态，且拥塞窗口大小已经大于了一定的值，那么就会通过调用hystart_update()进入到 hystart 算法。

```c
/* Location: net/ipv4/tcp_cubic.c */
static void hyststart_update(struct sock *sk, u32 delay)
{
    struct tcp_sock *tp = tcp_sk(sk);
    struct bibtcp *ca = inet_csk_ca(sk);
    /* 如果已经找到了退出点，那么直接返回 */
    if (ca->found & hyststartdetect)
        return;
    if (hyststartdetect & HYSTART_ACK_TrAIN) {
        u32 now = bibtcp_clock();
        /* first detection parameter - ack-train detection */
        if ((s32)(now - ca->last_ACK) <= hyststart_ACK delta) {
            ca->last_ACK = now;
            if ((s32)(now - ca->round_start) > ca->delay_min >> 4) {
                ca->found |= HYSTART_ACK_TrAIN;
                NET_INC_STATISTICS_BH(sock_net(sk), LINUX_MIB.tcpHYSTARTTRAINDETECT);
                NET_ADD_STATISTICS_BH(sock_net(sk), LINUX_MIB.tcpHYSTARTTRAINCWnd, tp->snd_cwnd);
                tp->snd_ssthresh = tp->snd_cwnd;
            }
        }
} 
```

上面是第一个判别条件，如果收到两次相隔不远的 ACK 之间的时间大于了 rtt 时间的一半，那么，就说明窗口的大小差不多到了网络容量的上限了。这里之所以会右移4，是因为delay_min是最小延迟左移 3 后的结果。rtt 的一半相当于单程的时延。连续收到的两次ACK 可以用于估计带宽大小。这里需要特别解释一下：由于在慢启动阶段，会一次性发送大量的包，所以，可以假设在网络中，数据是连续发送的。而收到的两个连续的包之间的时间间隔就是窗口大小除以网络带宽的商。作为发送端，我们只能获得两次 ACK 之间

的时间差，因此用这个时间来大致估计带宽。拥塞窗口的合理大小为 $C = B \times D _ { m i n } + S$ ，这里 $C$ 是窗口大小， $B$ 是带宽， $D _ { m i n }$ 是最小的单程时延， $S$ 是缓存大小。由于带宽是基本恒定的，因此，只要两次 ACK 之间的时间接近与 $D _ { m i n }$ 就可以认为该窗口的大小是基本合理的，已经充分的利用了网络。

第二个启发条件是如果当前往返时延的增长已经超过了一定的限度，那么，说明网络的带宽已经要被占满了。因此，也需要退出慢启动状态。

```c
1 if (hystartdetect & HYSTART_DELAY) { /\*obtain the minimum delay of more than sampling packets \*/ 2 if (ca->sample_cnt < HYSTART_MINSAMPLES) { 3 if (ca->curr_rtt == 0 || ca->curr_rtt > delay) 4 ca->curr_rtt = delay; 5 6 7 ca->sample_cnt++; 8 } else { 9 if (ca->curr_rtt > ca->delay_min + 10 HYSTART_DELAY_THRESH(ca->delay_min >> 3)) { 11 ca->found |= HYSTART_DELAY; 12 NET_INC_STATSHBH(sock_net(sk), 13 LINUX_MIB_TCPHYSTARTDELAYDETECT); 14 NET_ADD_STATSHBH(sock_net(sk), 15 16 NET_MIB_TCPHYSTARTDELAYCWND, 17 tp->snd_cwnd); 18 } 19 } 20 } 21 } 
```

退出慢启动状态的方法很简单，都是直接将当前的snd_ssthresh的大小设定为和当前拥塞窗口一样的大小。这样，TCP 自然就会转入拥塞避免状态。

# CHAPTER 7

# TCP 释放连接

# Contents

7.1 tcp_shutdown . . 138   
7.2 主动关闭 139

7.2.1 第一次握手: 发送 FIN . . . . . 139

7.2.1.1 基本调用关系 . . . 139  
7.2.1.2 tcp_close 139   
7.2.1.3 tcp_close_state . . . . 143   
7.2.1.4 tcp_send_fin . . 143

7.2.2 第二次握手: 接收 ACK . 145

7.2.2.1 tcp_rcv_state_process . . . . . 145   
7.2.2.2 tcp_time_wait . 148

7.2.3 第三次握手: 接受 FIN . . 149

7.2.3.1 tcp_timewait_state_process . . . . . 149

7.2.4 第四次握手: 发送 ACK 152  
7.2.5 同时关闭 . . 154  
7.2.6 TIME_WAIT . . 155

7.3 被动关闭 157

7.3.1 基本流程 157  
7.3.2 第一次握手: 接收 FIN . . . . . . 157

7.3.2.1 函数调用关系 . . . 157  
7.3.2.2 tcp_fin . . . . . 157

7.3.3 第二次握手: 发送 FIN 的 ACK . . . 159  
7.3.4 第三次握手: 发送 FIN . . . . . 159  
7.3.5 第四次握手: 接收 FIN 的 ACK . . . . 160

# 7.1 tcp_shutdown

通过 shutdown 系统调用，主动关闭 TCP 连接。该系统调用最终由tcp_shutdown实现。

```c
/*   
Location:   
net/ipv4/tcp.c   
Function:   
Shutdown the sending side of a connection. Much like close except that we don't receive shut down or sock_set_flag(sk, SOCKdead).   
Parameter:   
sk:传输控制块   
how:????   
\*/   
void tcp_shutdown(struct sock \*sk，int how)   
{ /* We need to grab some memory,and put together a FIN, * and then put it into the queue to be sent. * Tim MacKenzie(tym@dibbler.cs.monash.edu.au) 4 Dec '92. */ if(!(how&SEND_SHUTDOWN)) return;   
/\* 发送方向的关闭，并且TCP状态为ESTABLISHED、SYN_SENT、 SYN_RECV、CLOSE_WAIT状态中的一个。 /\* if((1<<sk->sk_state)& (TCPF_ESTABLISHED|TCPF_SYN_SENT| TCPF_SYN_RECV|TCPF_CLOSE_WAIT)){ /\* Clear out any half completed packets.FIN if needed.\*/ /\*如果此时已经发送一个FIN了，就跳过。\*/ if (tcp_close_state(sk)) tcp_send_fin(sk); } 
```

该函数会在需要发送 FIN 时，调用tcp_close_state()来设置 TCP 的状态。

# 7.2 主动关闭

# 7.2.1 第一次握手: 发送 FIN

# 7.2.1.1 基本调用关系

# 7.2.1.2 tcp_close

当一段完成数据发送任务之后，应用层即可调用 close 发送一个 FIN 来终止该方向上的连接，当另一端收到这个 FIN 后，必须通知应用层，另一端已经终止了数据传送。

而 close 系统调用在传输层接收的实现就是 tcp_close, 这里我们依次分析。

/\*   
Location:   
net/ipo4/tcp.c   
Function: 进行主动关闭的第一步，发送FIN   
Parameter: sk：传输控制块。 timeout：在真正关闭控制块之前，可以发送剩余数据的时间。   
\*/   
void tcp_close(struct sock \*sk, long timeout)   
{ struct sk_buffer \*skb; int data_was_unread $= 0$ int state; lock SOCK(sk); sk->sk_shutdown $=$ SHUTDOWN_MASK; if (sk->sk_state $= =$ TCP_LISTEN){ tcp_set_state(sk,TCP_CLOSE); /\*Special case.\*/ inlet_csk.listen_stop(sk); goto adjudge_to_death;

首先，对传输控制块加锁。然后设置关闭标志为 SHUTDOWN_MASK, 表示进行双向的关闭。

如果套接口处于侦听状态，这种情况处理相对比较简单，因为没有建立起连接，因此无需发送 FIN 等操作。设置 TCP 的状态为 CLOSE，然后终止侦听。最后跳转到adjudge_to_death 处进行相关处理。

```c
/* We need to flush the recu. buffs. We do this only on the
* descriptor close(什么意思), not protocol-sourced(什么意思) closes, because the
* reader process may not have drained(消耗) the data yet!
*/
while ((skb = __skb_dequeue(&sk->sk_receive_queue)) != NULL) {
    u32 len = TCP_SKB_CB(sk)->end_seq - TCP_SKB_CB(sk)->seq;
    if (TCP_SKB_CB(sk)->tcp_flags & TCPHDR_FIN)
        len--;
    data_was_unread += len;
    __kfree_skb(sk);
} 
```

因为要关闭连接，故需要释放已接收队列上的段，同时统计释放了多少数据，然后回收缓存。

```c
/\* If socket has been already reset (e.g. in tcp_reset()) - kill it. \*/ if (sk->sk_state == TCP_CLOSE) goto adjudge_to_death; 
```

如果 socket 本身就是 close 状态的话，直接跳到 adjudge_to_death 就好。

/\*As outlined in RFC 2525, section 2.17, we send a RST here because \* data was lost. To witness the awful effects of the old behavior of \* always doing a FIN, run an older 2.1.x kernel or 2.0.x, start a bulk \* GET in an FTP client, suspend the process, wait for the client to \*advertise a zero window, then kill -9 the FTP client, whee..\* Note: timeout is always zero in such a case. /\* /\*开启 repair 选项 \*/ if (unlikely tcp_sk(sk) $\rightharpoondown$ repair)) { sk->skprot->disconnect(sk, 0); } else if (data_was_unread) { /\* Unread data was tossed, zap the connection. 在存在数据未读的情况下断开连接。这时应该直接置状态 CLOSE, 并主动向对方发送 RST, 因为这是不正常的情况, 而发送 FIN 表示一切正常。\*/ NET_INC_STATSD_USER(sock_net(sk), LINUX_MIB.tcpABORTONCLOSE); tcp_set_state(sk, TCP_CLOSE); tcp_send.active_reset(sk, sk->sk_allocation); } else if (sock_flag(sk, SOCK_LINGER) && !sk->sk_lingertime) { /\* Check zero linger_after_checking for unread data. /\*/ \*/ 如果设置了 SO_LINGER 并且，延时时间为 0 , 则直接调用 disconnect 断开 、删除并释放已建立连接但未被 accept 的传输控制块。同时删除并释放 已收到在接收队列、失序队列上的段以及发送队列上的段。\*/ sk->sk_prot->disconnect(sk, 0); NET_INC_STATSD_USER(sock_net(sk), LINUX_MIB.tcpABORTONDATA); } else if (tcp_close_state(sk)) { //判断是否可以发送 FIN /\* We FIN if the application ate all the data before \* zapping the connection. /\*/ /\* RED-PEN. Formally speaking, we have broken TCP state \* machine. State transitions: \* \* TCP Established -> TCP_FIN_WAIT1 \* TCP_SYN_RECV -> TCP_FIN_WAIT1 (forget it, it's impossible) \* TCP_CLOSE_WAIT -> TCP LAST_ACK \* \* are legal only when FIN has been sent (i.e. in window), \* rather than queued out of window. Purists blame. \* F.e. "RFC state" is ESTABLISHED, \* if Linux state is FIN-WAIT-1, but FIN is still not sent. \* The visible declinations are that sometimes \* we enter time-wait state, when it is not required really \* (harmless), do not send active resets, when they are

```c
* required by specs (TCP_estABLISHED, TCP_CLOSE_WAIT, when
* they look as CLOSING or LAST_ACK for Linux)
* Probably, I missed some more holelets.
* --ANK
* XXX (TFO) - To start off we don't support SYN+ACK+FIN
* in a single packet! (May consider it later but will
* probably need API support or TCP_CORK SYN-ACK until
* data is written and socket is closed.)
*/
tcp_send_fin(sk);
}
sk_STREAM_wait_close(sk, timeout); 
```

在给对端发送 RST 或 FIN 段后，等待套接口的关闭，直到 TCP 的状态为 FIN_WAIT_1、CLOSING、LAST_ACK 或等待超时。

adjudge_to_death: /\* 置套接口为DEAD状态，成为孤儿套接口，同时更新系统中的孤儿套接口数。 \*/ state $=$ sk->sk_state; sock_hold(sk); sock_orphan(sk); /\*Itisthe last release_sockin its life.It will remove backlog.\*/ release_sock(sk); /\*Now socket is owned by kernel and we acquire BH lock to finish close.No need to check for user refs. \*/ local_bh_disable(); .(sk); WARNING_ON(sock-owned_by_user(sk)); percpucounter_inc(sk->sk_prot->orphan_count); /\*Havewealreadybeen destroyedbyasoftirqorbacklog? \*/ if(state！ $= \mathrm{TCP}$ CLOSE&&sk->sk_state $\equiv =$ TCP_CLOSE) goto out;

接下来处理 FIN_WAIT2 到 CLOSE 状态的转换。

/\* This is a (useful) BSD violating of the RFC. There is a \* problem with TCP as specified in that the other end could \* keep a socket open forever with no application left this end. \* We use a 1 minute timeout (about the same as BSD) then kill \* our end. If they send after that then tough - BUT: long enough \* that we won't make the old $4^{*}rto =$ almost no time - whoops reset mistake. \* Nope, it was not mistake. It is really desired behaviour \* f.e. on http servers, when such sockets are useless, but \* consume significant resources. Let's do it with special \* linger2 option. --ANK \*/   
if (sk->sk_state $= =$ TCP_FIN_WAIT2){ struct tcp SOCK \*tp $=$ tcp_sk(sk);

如果 linger2 小于 0，则表示无需从 TCP_FIN_WAIT2hangtag 等待转移到CLOSE 状态，而是立即设置 CLOSE 状态，然后给对端发送 RST 段。

\*/ if(tp->linger2<0){ tcp_set_state(sk，TCP_CLOSE); tcp_send.active_reset(sk,GFP_ANOMIC); NET_INC_STATSHBH(sock_net(sk), LINUX_MIB_TCPABORTONLINGER); }else{ /\*计算需要保持TCP_FIN_WAIT2状态的时长\*/ const int tmo $=$ tcp_fin_time(sk); $\text{一} *$ 小于1min，调用TCP_FIN_WAIT2定时器\*/ if(tmo>TCP_TIMEWAIT_LEN）{ inlet_csk_reset_keepalive_timer(sk, tmo-TCP_TIMEWAIT_LEN); }else{ /\* 否则调用timewait控制块取代tcp_sock传出控制块。 $\text{一} /$ tcp_time_wait(sk，TCP_FIN_WAIT2，tmo); goto out; 1

继续处理。

/\*不是CLOSE状态\*/ if(sk->sk_state $! =$ TCP_CLOSE){ sk_mem_reclaim(sk); if (tcp_check_oom(sk,0)) { tcp_set_state(sk，TCP_CLOSE); tcp_send.active_reset(sk,GFP_ANOMIC); NET_INC_STATSBH(sock_net(sk), LINUX_MIB.tcpABORTONMEMORY)； } } /\*是CLOSE状态了\*/ if(sk->sk_state $= =$ TCP_CLOSE）{ struct request_sock $\ast$ req $=$ tcp_sk(sk)->fastopen_rsk; /\*We could get here with a non-NULL req if the socket is \* aborted(e.g.,closed with unread data) before 3WHS \*finishes. \*/ if (req) reqsk_fastopen_remove(sk,req,false); inlet_cskdestroy_sock(sk); } /*Otherwise，socketisretrieveduntilprotocolclose.\*/ out: bh_unlock_sock(sk); local_bh_enable(); sock_put(sk);

该函数会根据当前的状态，按照1.3.1.1中给出的状态图。

# 7.2.1.3 tcp_close_state

/\*   
Location: net/iju4/tcp.c   
Function: 进行状态转移，并且判断是否可以发送FIN。   
Parameter: sk：传输控制块   
\*/   
static int tcp_close_state(struct sock \*sk) { int next $=$ (int)new_state[sk->sk_state]； /\*去除可能的FIN_ACTION\*/ int ns $=$ next&TCP_STATE_MASK; /\*根据状态图进行状态转移\*/ tcp_set_state(sk,ns); /\*如果需要执行发送FIN的动作，则返回真\*/ return next&TCP_ACTION_FIN;   
}

7.2.1.4 tcp_send_fin   
/\*   
Location:   
net/iju4/tcp_output.c   
Function: Send a FIN. The caller locks the socket for us. We should try to send a FIN packet really hard, but eventually give up.   
Parameter:   
sk:传输控制块   
\*/   
void tcp_send_fin(struct sock \*sk)   
{ /\*取得sock发送队列的最后一个元素，如果为空，返回null\*/ struct sk BUFF \*skb，\*tskb $=$ tcp_write_queueTAIL(sk); struct tcp_sock \*tp $=$ tcp_sk(sk); /\*这里做了一些优化，如果发送队列的末尾还有段没有发出去，则利用该段发送FIN。\*/ if（tskb&&（tcp_send_head(sk)||tcp_under_memory_pressure(sk))){ /\*如果当前正在发送的队列不为空，或者当前TCP处于内存压力下(值得进一步分析)，则进行该优化\*/ coalesce: TCP_SKB_CB(tskb)->tcp_flags |= TCPHDR_FIN; //递增序列号 TCP_SKB_CB(tskb)->end_seq++; tp->write_seq++; /\*队头为空\*/

if(!tcp_send_head(sk)) { /\*This means tskb was already sent. \* Pretend we included the FIN on previous transmit. \*We need to set tp->snd_nxt to the value it would have \* if FIN had been sent. This is because retransmit path \* does not change tp->snd_nxt. \*/ tp->snd_nxt++; return; } } else { /\*为封包分配空间\*/ skb $=$ alloc_skb_fclone(MAX意義, $s_k->s_k\_ allocation);$ if (unlikely(!skb)){ /\*如果分配不到空间，且队尾还有未发送的包，利用该包发出FIN。\*/ if(tskb) goto coalesce; return; } skb_reserved(skb, MAX意義); sk Forced_mem_schedule(sk, skb->truesize); /\*FIN eats a sequence byte, write_seq advanced by tcp_queue_skb().\*/ /\*构造一个FIN包，并加入发送队列。\*/ tcp_init_nodata_skb(skb,tp->write_seq, TCPHDR_ACK|TCPHDR_FIN); tcp_queue_skb(sk, skb); } _tcp.push_pending Frames(sk, tcp_current_mss(sk), TCP_NAGLE_OFF); }

在函数的最后，将所有的剩余数据一口气发出去，完成发送 FIN 包的过程。至此，主动关闭过程的第一次握手完成。

# 7.2.2 第二次握手: 接收 ACK

在发出 FIN 后，接收端会回复 ACK 确认收到了请求。从这里开始有两种情况，一种是教科书式的四次握手的情况，另一种是双方同时发出 FIN 的情况，这里我们考虑前一种情况。后者会在7.2.5中描述。

我们知道内核中接收数据都是从 tcp_v4_do_rcv 函数开始，简单分析就知道会继续调用 tcp_rcv_state_process 函数，我们就直接从这里开始了。

# 7.2.2.1 tcp_rcv_state_process

```c
int tcp_rcv_state_process(struct sock *sk, struct sk BUFF *skb) {
    struct tcp SOCK *tp = tcp_sk(sk);
    struct inlet_connection_sock *icsk = inlet_csk(sk);
    const struct tcphdr *th = tcp_hdr(skb);
    struct request_sock *req;
    int queued = 0;
    bool acceptable;
    tp->rx_opt.saw_tstamp = 0;
    switch (sk->sk_state) { 
```

case TCP_CLOSE: goto discard;   
case TCP_LISTEN:/\*LISTEN状态处理代码，略去\*/   
case TCP_SYN_SENT:/\*SYN-SENT状态处理代码，略去\*/   
}   
req $\equiv$ tp->fastopen_rsk;   
if (req){WARN_ON_ONCE(sk->sk_state！ $=$ TCP_SYN_RECV && sk->sk_state！ $=$ TCP_FIN_WAIT1); if(!tcp_check_req(sk,skb,req,True)) goto discard;   
}   
if(!th->ack&&!th->rst&&!th->syn) goto discard;   
if(!tcpValidateIncoming(sk,skb,th,O)) return 0;   
/\*step5:check the ACK field，判断是否可以接收\*/ acceptable $\equiv$ tcp_ACK(sk,skb,FLAG_SLOWPATH | FLAG_UPDATE_TS_RECENT）>0;   
switch(sk->sk_state){ caseTCP_SYN_RECV:/\*SYN-RECV状态处理代码，略去\*/ caseTCP_FIN_WAIT1:{ struct dst_entry \*dst; int tmo; /\*如果当前的套接字为开启了FastOpen的套接字，且该ACK为 \*接收到的第一个ACK，那么这个ACK应该是在确认SYNACK包， \*因此，停止SYNACK计时器。 \*/ if (req){ /\*Return RST if ack_seqisinvalid. \*Note that RFC793 only says to generate a \*DUPACK for it but for TCP Fast Open it seems \*better to treat this case like TCP_SYN_RECV \* above. \*/ if(!acceptable) return 1; /\*移除fastopen请求\*/ reqsk_fastopen_remove(sk,req,false); tcp_rearm_rto(sk); } /\*未确认的序列号是不是等于发送队列队尾序列号\*\*/ if(tp->snd_una！ $=$ tp->write_seq) break;   
/\*收到ACK后，转移到TCP_FIN_WAIT2状态，将发送端关闭。\*/ tcp_set_state(sk,TCP_FIN_WAIT2); sk->sk_shutdown|=SEND_SHUTDOWN;

/\*如果路由缓存非空，确认路由缓存有效\*/  
dst $=$ __sk.dst_get(sk);  
if (dst)dst.confirm(dst); $\ast$ 唤醒等待该套接字的进程\*/  
if(!sock_flag(sk,SOCK_DEAD)){/\*Wake up lingeringing close() \*/sk->sk_state_change(sk);break;  
} $\ast$ 如果linger2小于0，则说明无需在FIN_WAIT2状态等待，直接关闭传输控制块。\*/  
if(tp->linger2<0||(TCP_SKB_CB(sk)->end_seq != TCP_SKB_CB(sk)->seq&&after(TCP_SKB_CB(sk)->end_seq-th->fin,tp->rcv_nxt))){tcp_done(sk);NET_INC_STATSBH(sock_net(sk),LINUX_MIB.tcpABORTONDATA);return 1;  
}

转换到TCP_FIN_WAIT2以后，计算接受 fin 包的超时时间。如果还能留出 TIME-WAIT 阶段的时间（TIMEWAIT 阶段有最长时间限制），那么在此之前，就激活保活计时器保持连接。如果时间已经不足了，就主动调用tcp_time_wait 进入 TIMEWAIT 状态。

tmo $=$ tcp_fin_time(sk); if (tmo $\rightharpoondown$ TCP_TIMEWAIT_LEN）{ inlet_csk_reset_keepalive_timer(sk，tmo-TCP_TIMEWAIT_LEN); } else if(th->fin||sock-owned_by_user(sk)){//Bad case.We could lose such FIN otherwise. \*It is not a big problem,but it looks confusing \*and not so rare event.We still can lose it now, \*if it spins in bh_lock_sock(),but it is really \*marginal case. \*/ inlet_csk_reset_keepalive_timer(sk，tmo); }else{ /\*进入TCP_FIN_WAIT2状态等待。\*/ tcp_time_wait(sk，TCP_FIN_WAIT2，tmo); goto discard; 1 break; /\*其余状态处理代码，略去\*/   
}   
/\*step6:check the URG bit\*/ tcp_urg(sk，skb，th);   
/\*step7:process the segment text\*/ switch（sk->sk_state）{//其他状态处理代码，略去\*/ case TCP_FIN_WAIT1: case TCP_FIN_WAIT2:

```c
/\* RFC 793 says to queue data in these states, \* RFC 1122 says we MUST send a reset. \* BSD 4.4 also does reset. \*/ /\* 如果接收方向已经关闭，而又收到新的数据，则需要给对方发送 RST 段，告诉对方已经把数据丢弃。 \*/ if (sk->sk_shutdown & RCV_SHUTDOWN) { if (TCP_SKB_CB(sk)->end_seq != TCP_SKB_CB(sk)->seq && after(TCP_SKB_CB(sk)->end_seq - th->fin, tp->rcv_nxt)) { NET_INC_STATSH_BH(sock_net(sk), LINUX_MIB.tcpABORTONDATA); /*如果接收端已经关闭了，那么发送RESET。*/ tcp_reset(sk); return 1; } } /\* Fall through \*/ /\*其他状态处理代码，略去 \*/ } /\* tcp_data could move socket to TIME-WAIT 如果TCP的状态不处于CLOSE状态，则发送队列中的段， 同时调度ACK，确定是立即发送，还是延时发送。 \*/ if (sk->sk_state != TCP_CLOSE){ tcp_data_snd_check(sk); tcp_ACK_snd_check(sk); } if (!queued) { discard: __kfree_skb(sk); } return 0;
```

执行完该段代码后，则进入了FIN_WAIT2状态。

由于进入到FIN_WAIT2状态后，不会再处理 TCP 段的数据。因此，出于资源和方面的考虑，Linux 内核采用了一个较小的结构体tcp_timewait_sock来取代正常的 TCP传输控制块。TIME_WAIT也是可作同样处理。该替换过程通过函数tcp_time_wait完成。

# 7.2.2.2 tcp_time_wait

```txt
/\*   
Location:   
net/ipv4/tcp_minicock.c   
Function: Move a socket to time-wait or dead fin-wait-2 state.   
Parameter: sk: sock 
```

```c
state: 状态
timeo: 超时时间
\/
void tcp_time_wait(struct sock *sk, int state, int timeo)
\{
 const struct inet_connection_sock *icsk = inet_csk(sk);
 const struct tcp_sock *tp = tcp_sk(sk);
 structINETtimewait_sock *tw;
 boolrecycle.ok = false;
 /* 如果启用tu_recycle，且tsrecent_stamp有效，则记录相关时间戳信息到对端信息管理块中。
*/
if (tcp_death_row.sysct1_tw_recycle &&tp->rx_opt.tsrecent_stamp)
recycle.ok = tcp Remember stamp(sk);
 */
/*分配空间*/
tw =inet_twsk_alloc(sk, &tcp_death_row, state);
/*分配成功*/
if(tw) \{
struct tcp.timewait_sock *tcptw = tcp_twsk((struct sock*)tw);
/*计算重传超时时间*/
const int rto = (icsk->icsk_rto << 2) - (icsk->icsk_rto >> 1);
structINET_sock *inet =INETSk(sk);
/*将值复制给对应的域*/
tw->tw Transparent =inet->transparent;
tw->tw_rcvWscale =tp->rx_opt.rcvWscale;
tcptw->tw_rcv_nxt =tp->rcv_nxt;
tcptw->tw_snd_nxt =tp->snd_nxt;
tcptw->tw_rcv_wnd =tcp_receive_window(tp);
tcptw->tw tsrecent =tp->rx_opt.tsrecent;
tcptw->tw tsrecent_stamp =tp->rx_opt.tsrecent_stamp;
tcptw->tw ts_offset =tp->toffset;
tcptw->tw_last_oow_ACK_time =0;
/*部分对于ipv6和md5的处理，略过*/
/*Get the TIME_WAIT timeout firing.*/
if(timeo < rto)
timeo =rto;
/*如果成功将相关时间戳信息添加到对端信息管理块中，则TIME_WAIT的超时时间设置为3.5倍的往返时间。
否则，设置为60s。
*/
if (recycle.ok) \{
tw->tw_timeout =rto;
} else \{
tw->tw_timeout =TCP_TIMEWAIT_LEN;
if(state == TCP_TIME_WAIT)
timeo =TCP_TIMEWAIT_LEN;
\}
/*启动定时器*/
inet_twsk_schedule(tw, timeo);
/*将timewait控制块插入到哈希表中，替代原有的传输控制块*/ 
```

```c
__inet_twsk_hashdance(tw, sk, &tcp_hashinfo);
__inet_twsk_put(tw);
} else {
/* 当内存不够时，直接关闭连接 */
NET_INC_STATISTICS_BH(sock_net(sk), LINUX_MIB_TCPTIMEWAITOVERFLOW);
}
/* 更新一些测量值并关闭原来的传输控制块 */
tcp_update_metrica(sk);
tcp_done(sk);
}
```

# 7.2.3 第三次握手: 接受 FIN

此时，由于已经使用了 timewait 控制块取代了 TCP 控制块。因此，对应的处理代码不再位于 tcp_rcv_state_process中，而是换到了 tcp_timewait_state_process函数中。

# 7.2.3.1 tcp_timewait_state_process

该函数的代码如下，可以看到参数中已经变成了inet_timewait_sock。

```c
/*   
Location   
net/ipo4/tcp_minisock.h   
Description   
\* \* Main purpose of TIME-WAIT state is to close connection gracefully,   
\* when one of ends sits in LAST-ACK or CLOSING retransmitting FIN   
\* (and, probably, tail of data) and one or more our ACKs are lost.   
\* \* What is TIME-WAIT timeout? It is associated with maximal packet   
\* lifetime in the internet, which results in wrong conclusion, that   
\* it is set to catch "old duplicate segments" wandering out of their path.   
\* It is not quite correct. This timeout is calculated so that it exceeds   
\* maximal retransmission timeout enough to allow to lose one (or more)   
\* segments sent by peer and our ACKs. This time may be calculated from RTO.   
\* \* When TIME-WAIT socket receives RST, it means that another end   
\* finally closed and we are allowed to kill TIME-WAIT too.   
\* \* Second purpose of TIME-WAIT is catching old duplicate segments.   
\* Well, certainly it is pure paranoia, but if we load TIME-WAIT   
\* with this semantics, we MUST NOT kill TIME-WAIT state with RSTs.   
\* \* If we invented some more clever way to catch duplicates   
\* (f.e. based on PAWS), we could truncate TIME-WAIT to several RTOs.   
\*   
\* The algorithm below is based on FORMAL INTERPRETATION of RFCs.   
\* When you compare it to RFCs, please, read section SEGMENT ARRIVES   
\* from the very beginning.   
\*   
\* NOTE. With recycling (and later with fin-wait-2) TW bucket   
\* is _not_ stateless. It means, that strictly speaking we must   
\* spinlock it. I do not want! Well, probability of misbehaviour   
\* is ridiculously low and, seems, we could use some mb() tricks   
\* to avoid misread sequence numbers, states etc. --ANK 
```

\*We don't need to initialize tmp_out.sack ok as we don't use the results   
Parameter tw:接收处理段的timewait控制块。 skb:FIN_WAIT2和TIME_WAIT状态下接收到的段 th：TCP的首部   
\*/   
enum tcp_tw_status   
tcpTimewait_state_process(structinet timewait_sock \*tw，structskbuff \*skb, const struct tcphdr \*th)   
{ struct tcp_options_received tmp_opt; struct tcpTimewait_sock \*tcptw $=$ tcp_twsk((struct sock \*)tw); boolpaws_reject $=$ false; /\*首先标记没有看到时间戳\*/ tmp_opt.saw_tstamp $= 0$ . if(th->doff $>$ (sizeof(\*th）>>2)&\*tcptw->tw-tsrecent_stamp){ tcp.parse_options(skb,&tmp_opt,0,NULL); if（tmp_opt.saw_tstamp）{ tmp_opt.rcv_tsecr --=tcptw->tw-ts_offset; tmp_opt.tsrecent $=$ tcptw->tw-tsrecent; tmp_opt.tsrecent_stamp $=$ tcptw->tw-tsrecent_stamp; paws_reject $=$ tcp_paws_reject(&tmp_opt，th->rst); }

检测收到的包是否含有时间戳选项，如果有，则进行 PAWS 相关的检测。之后，开始进行 TCP_FIN_WAIT2相关的处理。

关于 tcp_paws_reject 更多的内容，请参见8.7.3.

```c
if (tw->tw_substate == TCP_FIN_WAIT2) { /* 重复 tcp_rcv_state_process() 所进行的所有检测 */ /* 序号不在窗口内或序号无效，发送 ACK */ if (paws_reject ||！tcp_in_window(TCP_SKB_CB(skb)->seq, TCP_SKB_CB(skb)->end_seq, tcptw->tw_rcv_nxt, tcptw->tw_rcv_nxt + tcptw->tw_rcv_wnd)) return tcp.timewait_check_oow_rate_limit( tw, skb, LINUX_MIB.tcpACKSKIPPEDFINWAIT2); /* 如果收到 RST 包，则销毁 timewait 控制块并返回 TCP_TW_SUCCESS */ if (th->rst) goto kill; /* 如果收到过期的 SYN 包，则销毁并发送 RST */ if (th->syn && !before(TCP_SKB_CB(skb)->seq, tcptw->tw_rcv_nxt)) goto kill_with_rst; /* 如果收到 DACK，则释放该控制块，返回 TCP_TW_SUCCESS*/ if (!th->ack ||！after(TCP_SKB_CB(skb)->end_seq, tcptw->tw_rcv_nxt) || TCP_SKB_CB(skb)->end_seq == TCP_SKB_CB(skb)->seq) { inlet_twsk_put(tw); return TCP_TW_SUCCESS; }
```

/\*之后只有两种情况，有新数据或收到FIN包\*/ if(!th->fin|| TCP_SKB_CB(skb)->end_seq != tcptw->tw_rcv_nxt + 1）{ /\*如果收到了新的数据或者序号有问题， \*则销毁控制块并返回TCP_TW_RST。 \*/ kill_with_rst: inlet_twsk_deschedule_put(tw); return TCP_TW_RST; } /\*收到了FIN包，进入TIME_WAIT状态\*/ tw->tw_substate $=$ TCP_TIME_WAIT; tcptw->tw_rcv_nxt = TCP_SKB_CB(skb)->end_seq; /\*如果启用了时间戳选项，则设置相关属性\*/ if (tmp_opt.saw_tstamp){ tcptw->tw-tsrecent_stamp $=$ getSeconds(); tcptw->tw-tsrecent $=$ tmp_opt.rcv tsval; } /\*启动TIME_WAIT定时器\*/ if (tcp_death_row.sysct1_tw_recycle && tcptw->tw-tsrecent_stamp && tcp_tw Remember stamp(tw)) inlet_twsk_reschedule(tw，tw->tw_timeout); else inlet_twsk_reschedule(tw，TCP_TIMEWAIT_LEN); return TCP_TW_ACK; } /\*TIME_WAIT阶段处理代码\*/

# 7.2.4 第四次握手: 发送 ACK

在tcp_v4_rcv中，如果发现目前的连接处于FIN_WAIT2 或TIME_WAIT状态，则调用tcp_timewait_state进行处理，根据其返回值，执行相关操作。

```c
switch (tcp_timeout_state_process(inet_twsk(sk), skb, th)) {  
    case TCP_TW_SYN: {  
        struct sock *sk2 = inet.lookup Listener(dev_net(sk->dev), &tcp_hashinfo, iph->saddr, th->source, iph->daddr, th->dest,inet_iif(sk));  
        if (sk2) {  
            inet_twsk_deschedule_put(inet_twsk(sk));  
            sk = sk2;  
            goto process;  
        }  
    /* Fall through to ACK */  
}  
case TCP_TW_ACK:  
/* 回复 ACK 包 */  
tcp_v4_timeout ACK(sk, skb); 
```

```txt
break;   
case TCP_TW_RST: goto no.tcp_SOCKET;   
case TCP_TW_SUCCESS:;   
} 
```

根据上面的分析，在正常情况下，tcp_timewait_state_process会返回 TCP_TW_ACK，因此，会调用tcp_v4_timewait_ack。该函数如下：

static void tcp_v4TIMewait_ACK(struct sock \*sk，struct sk BUFF \*skb)   
{ structinetTimewait_sock \*tw $=$ inlet_twsk(sk); structtcpTimewait_sock \*tcptw $=$ tcp_twsk(sk); /\*发送ACK包\*/ tcp_v4_send_ACK(sock_net(sk)，skb, tcptw->tw_snd_nxt，tcptw->tw_rcv_nxt, tcptw->tw_rcv_wnd>>tw->tw_rcvWscale, tcp_time_stamp $^+$ tcptw->tw-ts_offset, tcptw->tw tsrecent, tw->tw_bound_dev_if, tcp_twsk_md5_key(tcptw), tw->tw Transparent？IP Reply.Arg_NOSRCCHECK：0, tw->tw_tol; /\*释放timewait控制块\*/ inlet_twsk_put(tw);

紧接着又将发送 ACK 包的任务交给tcp_v4_send_ack来执行。

```c
/* 下面的代码负责在 SYN_RECV 和 TIME_WAIT 状态下发送 ACK 包。  
*  
* The code following below sending ACKs in SYN-RECV and TIME-WAIT states  
* outside socket context is ugly, certainly. What can I do?  
*/  
static void tcp_v4_send_ACK(struct net *net, struct sk_buffer *skb, u32 seq, u32 ack, u32 win, u32 tsval, u32 tsecr, int oif, struct tcp_md5sig_key *key, int reply_flags, u8 tos)  
{ const struct tcphdr *th = tcp hdr(skb); struct { struct tcphdr th; __be32 opt[(TCPOLEN_TSTAMP_ALIGNED >> 2)  
#ifdef CONFIG.tcp_MD5SIG + (TCPOLEN_MD5SIG_ALIGNED >> 2)  
#endif ]; } rep; struct ip_reply_arg arg;memset(&rep.th, 0, sizeof(struct tcphdr));memset(&arg, 0, sizeof(arg)); /*构造参数和 TCP 头部 */ 
```

arg.iov[0].iov_base $=$ (unsigned char \*)&rep;   
arg.iov[0].iov_len $=$ sizeof(rep.th);   
if (tsecr){ rep.opt[0] $=$ htonl((TCPOPT_NOP<<24）|（TCPOPT_NOP<<16）| （TCPOPT_TIMESTAMP<<8）| TCPOLEN_TIMESTAMP); rep.opt[1] $=$ htonl(tsval); rep.opt[2] $=$ htonl(tsecr); arg.iov[O].iov_len $+ =$ TCPOLEN_TSTAMP_ALIGNED;   
}   
/*交换发送端和接收端*/   
rep.th_DEST $=$ th->source;   
rep.th.source $=$ th->dest;   
rep.th.doff $=$ arg.iov[0].iov_len/4;   
rep.th seq $=$ htonl(seq);   
rep.th.acc_seq $=$ htonl(ack);   
rep.th.ack $= 1$ .   
rep.thwindow $=$ htons(win);   
/*略去和MD5相关的部分代码*/   
/*设定标志位和校验码*/   
arg.flags $=$ reply_flags;   
arg.csum $=$ csum tcpupd_nofold(ip hdr(skb)->daddr, ip hdr(skb)->saddr，/\*XXX\*/ arg.iov[O].iov_len，IPPROTO_TCP，0);   
arg.csumoffset $=$ offsetof(struct tcphdr，check）/2;   
if (oif) argbound_dev_if $=$ oif;   
arg.tos $=$ tos;   
/*调用IP层接口将包发出*/ ip_send_unicast_reply(*this_cpu_ptr(net->ipv4.tcp_sk), skb,&TCP_SKB_CB(skb)->header.h4 opt, ip hdr(skb)->saddr，ip hdr(skb)->daddr, &arg，arg.iov[O].iov_len);   
TCP_INC_STATISTICS_BH(net，TCP_MIB_OUTSEGS);

至此，四次握手就完成了。

# 7.2.5 同时关闭

还有一种情况是双方同时发出了 FIN 报文，准备关闭连接。表现在 TCP 的状态图上，就是在发出 FIN 包以后又收到了 FIN 包，因此进入了 CLOSING 状态。该段代码在7.3.2.2 中进行了解析。之后，CLOSING 状态等待接受 ACK，就会进入到下一个状态在tcp_rcv_state_process中，处理TCP_CLOSING的代码如下：

```c
case TCP_CLOSING: if (tp->snd_una == tp->write_seq) { tcp_time_wait(sk, TCP_TIME_WAIT, 0); goto discard; } break; 
```

如果收到的 ACK 没问题则转入TIME_WAIT状态，利用 timewait 控制块完成后续的工

作。

1 switch (sk->sk_state) { case TCP_CLOSE_WAIT: case TCP Closing: case TCP LAST_ACK: if(!before(TCP_SKB_CB(sk)->seq,tp->rcv_nxt)) break;   
7 case TCP_FIN_WAIT1:   
8 case TCP_FIN_WAIT2: /\* RFC 793 says to queue data in these states, \* RFC 1122 says we MUST send a reset. \* BSD 4.4 also does reset. \*/ if (sk->sk_shutdown & RCV_SHUTDOWN) { if(TCP_SKB_CB(sk)->end_seq !=TCP_SKB_CB(sk)->seq && after(TCP_SKB_CB(sk)->end_seq - th->fin,tp->rcv_nxt)) { NET_INC_STATISTICS_BH(sock_net(sk),LINUX_MIB_TCPABORTONDATA); tcp_reset(sk); return 1;   
19 }   
20 }   
21 /\* Fall through \*/   
22 case TCP_ESTABLISHED: tcp_data_queue(sk, skb); queued $= 1$ break;   
26}

如果段中的数据正常，且接口没有关闭，那么就收下数据。否则，就直接忽略掉数据段的数据。

# 7.2.6 TIME_WAIT

该状态的处理也在tcp_timewait_state_process函数中。紧接在 7.2.3中处理FIN_WAIT2状态的代码之后。此时，说明当前的状态为TIME_WAIT。

```txt
1 /*   
2 \* Now real TIME-WAIT state.   
3 \*   
4 \* RFC 1122:   
5 \* "When a connection is [...] on TIME-WAIT state [...]   
6 \* [a TCP] MAY accept a new SYN from the remote TCP to   
7 \* reopen the connection directly, if it:   
8 \*   
9 \* (1) assigns its initial sequence number for the new   
10 \* connection to be larger than the largest sequence   
11 \* number it used on the previous connection incarnation,   
12 \* and   
13 \*   
14 \* (2) returns to TIME-WAIT state if the SYN turns out   
15 \* to be an old duplicate".   
16 \*/   
17 \*/   
18 序号没有回卷，且正是预期接收的段，段中没有负载   
19 或段中存在 RST 标志。   
20 \*/ 
```

if(!paws_reject&& (TCP_SKB_CB(skb)->seq == tcptw->tw_rcv_nxt && (TCP_SKB_CB(skb)->seq == TCP_SKB_CB(skb)->end_seq || th->rst))) { /*如果存在RST*/ if(th->rst) { /*ThisisTIME_WAITassassination,in twoflavors. \*Ohwell...nobodyhasasufficientsolutiontothis \*protocolbugyet. \*/ if(sysctl tcpRFC1337 $= = 0$ ）{   
kill: inlet_twsk_deschedule_put(tw); return TCP_TW_SUCCESS; } /\*重新激活定时器\*/ inlet_twsk_reschedule(tw，TCP_TIMEWAIT_LEN); if(tmp_opt.saw_tstamp){ tcptw->tw-tsrecent $=$ tmp_opt.rcv tsval; tcptw->tw-tsrecent_stamp $=$ getSeconds(); } inlet_twsk_put(tw); return TCP_TW_SUCCESS;

如果处于TIME_WAIT状态时，受到了 Reset 包，那么，按照 TCP 协议的要求，应当重置连接。但这里就产生了一个问题。本来TIME_WAIT之所以要等待 2MSL 的时间，就是为了避免在网络上滞留的包对新的连接造成影响。但是，此处却可以通过发送 rst 报文强行重置连接。重置意味着该连接会被强行关闭，跳过了 2MSL 阶段。这样就和设立 2MSL的初衷不符了。具体的讨论见1.3.3。如果启用了 RFC1337，那么就会忽略掉这个 RST报文。

/* 之后是超出窗口范围的情况。

All the segments are ACKed immediately.

The only exception is new SYN. We accept it, if it is not old duplicate and we are not in danger to be killed by delayed old duplicates. RFC check is that it has newer sequence number works at rates $< 40\mathrm{Mbit / sec}$ However, if paws works, it is reliable AND even more,   
newer sequence number works at rates $< 40\mathrm{Mbit / sec}$ However, if paws works, it is reliable AND even more,   
we even may relax silly seq space cutoff.   
RED-PEN: we violate main RFC requirement, if this SYN will appear old duplicate (i.e. we receive RST in reply to SYN-ACK), we must return socket to time-wait state. It is not good, but not fatal yet. \*/ /\* 接收到 SYN 段，并且没有 RST 和 ACK 标志，并且没有拒绝检测，并且序号有效。 \*/ if (th->syn && !th->rst && !th->ack && !paws_reject &&

23 (after(TCP_SKB_CB(skb)->seq, tcptw->tw_rcv_nxt) ||   
24 (tmp_opt.saw_tstamp &&   
25 (s32)(tcptw->tw(tsrecent - tmp_opt.rcv tsval) < 0))) {   
26 /\*如果可以接受该SYN请求，那么重新计算isn号，并返回TCP_TW_SYN。\*/   
27 u32 isn $=$ tcptw->tw_snd_nxt $+65535 + 2$ · if(isn $\equiv = 0$ ）   
28 isn++;   
29   
30 TCP_SKB_CB(skb)->tcp_tw_isn $=$ isn;   
31 return TCP_TW_SYN;   
32 }   
33   
34 if(paws_reject)   
35 NET_INC_STATSHBH(twsk_net(tw),LINUX_MIB_PAWSESTABREJECTED);

此后，如果收到了序号绕回的包，那么就重置TIME_WAIT定时器，并返回 TCP_TW_ACK。

```c
1 if(!th->rst){ /\*In this case we must reset the TIMEWAIT timer. \* \*If it is ACKless SYN it may be both old duplicate \* and new good SYN with random sequence number <rcu_nxt. \*Do not reschedule in the last case. \*/ if(paws_reject||th->ack) inlet_twsk_reschedule(tw，TCP_TIMEWAIT_LEN); return tcp_timewait_check_oow_rate_limit( tw，skb，LINUX_MIB_TCPACKSKIPPEDITIMEWAIT); } inlet_twsk_put(tw); return TCP_TW_SUCCESS;   
16 
```

# 7.3 被动关闭

# 7.3.1 基本流程

在正常的被动关闭开始时，TCP 控制块目前处于 ESTABLISHED 状态，此时接收到的 TCP 段都由tcp_rcv_established函数来处理，因此 FIN 段必定要做首部预测，当然预测一定不会通过，所以 FIN 段是走慢速路径处理的。

在慢速路径中，首先进行 TCP 选项的处理 (假设存在)，然后根据段的序号检测该FIN 段是不是期望接收的段。如果是，则调用tcp_fin函数进行处理。如果不是，则说明在 TCP 段传输过程中出现了失序，因此将该 FIN 段缓存到乱序队列中，等待它之前的所有 TCP 段都到, 才能做处理。

# 7.3.2 第一次握手: 接收 FIN

在这个过程中，服务器端主要接收到了客户端的FIN段，并且跳转到了CLOSE_WAIT状态。

# 7.3.2.1 函数调用关系

# 7.3.2.2 tcp_fin

此时，首先调用的传输层的函数为tcp_rcv_established, 然后走慢速路径由tcp_data_queue函数处理。在处理的过程中，如果 FIN 段是预期接收的段，则调用tcp_fin函数处理，否则，将该段暂存到乱序队列中，等待它之前的 TCP 段到齐之后再做处理，这里我们不说函数tcp_rcv_established与tcp_data_queue了，这些主要是在数据传送阶段介绍的函数。我们直接介绍tcp_fin函数。

/*   
Location:   
net/ipo4/tcp_input.c   
Function:   
Process the FIN bit. This now behaves as it is supposed to work and the FIN takes effect when it is validly part of sequence space. Not before when we get holes. If we are ESTABLISHED, a received fin moves us to CLOSE-WAIT (and thence onto LAST-ACK and finally, CLOSE, we never enter TIME-WAIT) If we are in FINWAIT-1, a received FIN indicates simultaneous close and we go into CLOSING (and later onto TIME-WAIT) If we are in FINWAIT-2, a received FIN moves us to TIME-WAIT.   
Parameters:   
sk:传输控制块   
*/   
static void tcp_fin(struct sock \*sk)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); inlet_csk_schedule_ACK(sk);   
sk->sk_shutdown |= RCV_SHUTDOWN; sock_set_flag(sk, SOCK_DONE);

首先，接收到 FIN 段后需要调度、发送 ACK。

其次，设置了传输控制块的套接口状态为RCV_SHUTDOWN，表示服务器端不允许再接收数据。

最后，设置传输控制块的SOCK_DONE标志，表示 TCP 会话结束。

当然，接收到 FIN 字段的时候，TCP 有可能并不是处于TCP_ESTABLISHED的，这里我们一并介绍了。

```c
1 switch (sk->sk_state) {
2     case TCP_SYN_RECV:
3         case TCP Established:
4         /* Move to CLOSE_WAIT */ 
```

tcp_set_state(sk, TCP_CLOSE_WAIT);

inet_csk(sk)->icsk_ack.pingpong $\ l = \ 1$ ;

break;

//what does it means pingpong? to do.

如果 TCP 块处于TCP_SYN_RECV或者TCP_ESTABLISHED状态，接收到 FIN 之后，将状态设置为CLOSE_WAIT, 并确定延时发送 ack。

```txt
case TCP_CLOSE_WAIT:   
case TCP_CLOSING:/\*Received a retransmission of the FIN,do \*nothing. break;   
case TCP LAST_ACK:/\*RFC793: Remain in the LAST-ACK state. \*/ break; 
```

在TCP_CLOSE_WAIT或者TCP_CLOSING状态下收到的 FIN 为重复收到的 FIN，忽略。

在TCP_LAST_ACK状态下正在等待最后的 ACK 字段，故而忽略。

```txt
case TCP_FIN_WAIT1: /\*This case occurs when a simultaneous close \* happens, we must ack the received FIN and \* enter the CLOSING state. \*/ tcp_send_ACK(sk); tcp_set_state(sk，TCP_CLOSING); break; 
```

显然，只有客户端和服务器端同时关闭的情况下，才会出现这种状态，而且此时双方必须都发送 ACK 字段，并且将状态置为TCP_CLOSING。

```c
case TCP_FIN_WAIT2: /\*Received a FIN -- send ACK and enter TIME_WAIT. \*/ tcp_send_ACK(sk); tcp_time_wait(sk，TCP_TIME_WAIT，0); break; default: /\*Only TCP_LISTEN and TCP_CLOSE are left, in these \* cases we should never reach this piece of code. \*/ pr_err("%s: Impossible, sk->sk_state=%d\n", __func__，sk->sk_state); break;   
} 
```

在TCP_FIN_WAIT2状态下接收到 FIN 段，根据 TCP 状态转移图应该发送 ACK 响应服务器端。

在其他状态，显然是不能收到 FIN 段的。

```c
/\*It_isisable,thatwehave something out-of-order_after FIN. \* Probably,we should reset in this case.For now drop them. \*/ _skb_queue_purge(&tp->out_of_order_queue); if (tcp_is_sack(tp)) tcp_sack_reset(&tp->rx_opt); sk_mem_reclaim(sk); 
```

8 if(!sock_flag(sk,SOCKDead)){ sk->sk_state_change(sk); /\*Do not send POLL_HUP for half duplex close. \*/ if(sk->sk_shutdown $= =$ SHUTDOWN_MASK|！ sk->sk_state $= =$ TCP_CLOSE) sk_wake_async(sk，SOCK_WAKE_WAITD，POLL_HUP）； else sk_wake_async(sk，SOCK_WAKE_WAITD，POLL_IN)； }   
19 1

清空接到的乱序队列上的段，再清除有关 SACK 的信息和标志，最后释放已接收队列中的段。

如果此时套接口未处于 DEAD 状态，则唤醒等待该套接口的进程。如果在发送接收方向上都进行了关闭，或者此时传输控制块处于 CLOSE 状态，则唤醒异步等待该套接口的进程，通知它们该连接已经停止，否则通知它们连接可以进行写操作。

# 7.3.3 第二次握手: 发送 FIN 的 ACK

这个是在哪里执行呢？？remain to do。

# 7.3.4 第三次握手: 发送 FIN

这里与主动关闭的第一次握手类似，就不再讲解了。

# 7.3.5 第四次握手: 接收 FIN 的 ACK

我们知道内核中接收数据都是从 tcp_v4_do_rcv 函数开始，简单分析就知道会继续调用 tcp_rcv_state_process 函数，然后会根据相应的状态到这里。

1 case TCP LAST_ACK: /\*未确认等于下一个要发送，更新 metrics，\*/   
2 if（tp->snd_una $\equiv$ tp->write_seq）{ tcp_update.metrics(sk); /\*置状态为CLOSE\*/ tcp_done(sk); goto discard;   
8 }   
9 break;   
10 1

# CHAPTER 8

非核心代码分析

# Contents

8.1 BSD Socket 层 . . . 161

8.1.1 msg_flag . . 161   
8.1.2 数据报类型 162  
8.1.3 Sock CheckSum . . . 163

8.1.3.1 CheckSum 相关标志位 163  
8.1.3.2 skb_csum_unnecessary . . . . 164   
8.1.3.3 __skb_checksum_complete . . . . 165

8.1.4 SK Stream 165   
8.1.5 sk_stream_wait_connect . . 165   
8.1.6 pskb_may_pull . . 166

8.2 Inet . . 167

8.2.1 __inet_stream_connect 167   
8.2.2 inet_hash_connect && __inet_hash_connect . . . . . . . 169

8.2.2.1 inet_hash_connect . . . 169   
8.2.2.2 __inet_hash_connect 169

8.2.3 inet_csk_reqsk_queue_add . . . 171   
8.2.3.1 inet_csk_reqsk_queue_hash_add . . . . . . . . . . . 171

8.2.4 inet_twsk_put . . 172

8.3 TCP 相关参数 . . . 172

8.3.1 TCP 标志宏 177   
8.3.2 函数宏 . . 178

8.3.2.1 before & after . . . . 178   
8.3.2.2 between . . . 178

8.4 TCP CheckSum 178

8.4.1 tcp_checksum_complete 178   
8.4.2 tcp_v4_checksum_init . . . . 179

# 8.5 TCP Initialize 179

8.5.1 TCP Initialize Handle . . . 179

8.5.1.1 tcp_connnect_init . . . . 179   
8.5.1.2 tcp_init_nondata_skb . . 180

# 8.6 TCP Options . . . 181

8.6.0.1 TCP Options Flags . . . 181

8.6.1 TCP Options Handle . . . 182

8.6.1.1 tcp_parse_fastopen_option . . . . . 182   
8.6.1.2 tcp_parse_options . . . . 182   
8.6.1.3 tcp_clear_options . . . . . 185   
8.6.1.4 tcp_syn_option . . . . 186   
8.6.1.5 tcp_established_options . . . . . . 187

# 8.7 TCP PAWS . . 188

8.7.1 TCP PAWS Flags . . . 188   
8.7.2 tcp_paws_check . . . . 189   
8.7.3 tcp_paws_reject . . . 189

# 8.8 TCP TimeStamp . . . . 190

8.8.1 tcp_store_ts_recent . . . . 190   
8.8.2 tcp_replace_ts_recent . . . 190

# 8.9 TCP ACK . . . 191

8.9.1 ACK Check . . . . 191

8.9.1.1 tcp_is_sack & tcp_is_reno & tcp_is_fack . . . . 191   
8.9.1.2 tcp_ack_is_dubious . 192   
8.9.1.3 tcp_check_sack_reneging . . . . . . . 193   
8.9.1.4 tcp_limit_reno_sacked . . . . 194   
8.9.1.5 tcp_check_reno_reordering . . . 194

8.9.2 ACK Update . . . . 195

8.9.2.1 tcp_snd_una_update . . . . 195   
8.9.2.2 tcp_reset_reno_sack . . . . 195   
8.9.2.3 tcp_add_reno_sack . . . . . 196

# 8.10 TCP Window . . . 196

8.10.1 Window Compute . . 196

8.10.1.1 tcp_left_out . . . . 196   
8.10.1.2 tcp_verify_left_out . . . 197

8.10.2 Window Update . . . 197

8.10.2.1 tcp_may_update_window . . . . 197

8.10.2.2 tcp_ack_update_window . . . 198

# 8.11 TCP Urgent Data . . . 199

8.11.1 相应标识 199  
8.11.2 TCP Urgent Check . . . 199   
8.11.2.1 tcp_check_urg 199   
8.11.3 TCP Urgent Deal 201   
8.11.3.1 tcp_urg . . . . 201

# 8.12 Congestion Control . . . 202

8.12.1 COngestion Control Flag . . . 202

8.12.1.1 sysctl_tcp_recovery . 202

8.12.2 Congestion Control Window . . . 202

8.12.2.1 tcp_init_cwnd_reduction . . . . 202   
8.12.2.2 tcp_end_cwnd_reduction . 203

8.12.3 Congestion Control Window Undo . . . . 204

8.12.3.1 tcp_init_undo . . 204   
8.12.3.2 tcp_may_undo . . . 204   
8.12.3.3 tcp_undo_cwnd_reduction . . . . . . 205   
8.12.3.4 tcp_try_undo_partial . . . . 206   
8.12.3.5 tcp_try_undo_recovery . . . . . 206   
8.12.3.6 tcp_rack_mark_lost . . . 207   
8.12.3.7 tcp_try_undo_dsack . . . . 209   
8.12.3.8 tcp_process_loss 210   
8.12.3.9 tcp_try_undo_partial . . . . . 211

8.12.4 About Restransmit . . 212

8.12.4.1 Related Macro . . 212

# 8.13 TCP Finish . . 212

8.13.1 Time Compute 212   
8.13.1.1 tcp_fin_time . . . 212

# 8.14 TCP Close 213

8.14.1 TCP Close Flags . . . 213   
8.14.1.1 TCP State . . . 213   
8.14.2 TCP State Machine 213   
8.14.3 tcp_done . . 214   
8.14.4 tcp_shutdown 214

# 8.1 BSD Socket 层

# 8.1.1 msg_flag

MSG_OOB 接收或发送带外数据。

MSG_PEEK 查看数据，并不从系统缓存区移走数据。

MSG_DONTROUTE 无需路由查找，目的地位于本地子网。

MSG_CTRUNC 指明由于缓存区空间不足，一些控制数据已被丢弃。

MSG_PROBE 使用这个标识，实际上并不会进行真正的的数据传递，而是进行路径 MTU 的探测。

MSG_TRUNC 只返回包的真实长度，并截短 (丢弃) 返回长度的数据。

MSG_DONTWAIT 无阻塞接收或发送。如果接收缓存中由数据，则接收数据并立刻返回。没有数据也立刻返回，而不进行任何等待。

MSG_WAITALL 必须一直等待，直到接收到的数据填满用户空间的缓存区。

MSG_CONFIRM 标识网关有效。只用于 SOCK_DGRAM 和 SOCK_RAM 类型的套接口。

MSG_ERRQUEUE 指示除了来自套接字错误队列的错误外，不接受其他数据。

MSG_NOSIGNAL 当另一端终止连接时，请求在基于流的错误套接字上不要发送 SIGPIPE 信号。

MSG_MORE 后续还有数据待发送。

SG_CMSG_COMPAT 64 位兼容 32 位处理方式。

# 8.1.2 数据报类型

```c
1 /\* Location: include/uapi/linux/if_packet.h   
4 Description:   
8 Packet types   
9 \*/   
10 #define PACKET_HOST 0 /* To us \*/   
11 #define PACKET_BROADCAST 1 /* To all \*/   
12 #define PACKET MultICAST 2 /* To group \*/   
13 #define PACKET_OTHERHOST 3 /* To someone else \*/   
14 #define PACKET_OUTGOING 4 /* Outgoing of any type,本机发出的包 \*/   
15 #define PACKET_LOOPBACK 5 /* MC/BRD frame looped back \*/   
16 #define PACKET_USER 6 /* To user space \*/   
17 #define PACKET_KERNEL 7 /* To kernel space \*/   
18 /* Unused，PACKET_FASTROUTE and PACKET_FASTROUTE are invisible to user space \*/   
19 #define PACKET_FASTROUTE 6 /* Fastrouted frame \*/   
20 
```

```c
/* Packet socket options */   
#define PACKET_ADD_MEMBERSHIP 1   
#define PACKET Drops MEMBERSHIP 2   
#define PACKET_RECV_OUTPUT 3   
/* Value 4 is still used by obsolete turbo-packet. */   
#define PACKET_RX_RING 5   
#define PACKET_STATISTICS 6   
#define PACKET_copy_THRESH 7   
#define PACKET AUXDATA 8   
#define PACKET_ORIGDEV 9   
#define PACKET_VERSION 10   
#define PACKET_HDRLEN 11   
#define PACKET_RESERVE 12   
#define PACKET_TX_RING 13   
#define PACKET_LOSS 14   
#define PACKET_VNET_HDR 15   
#define PACKET_TX_TIMESTAMP 16   
#define PACKET_TIMESTAMP 17   
#define PACKET_FANOUT 18   
#define PACKET_TXHAS_OFF 19   
#define PACKET_QDISC_BYPASS 20   
#define PACKET_ROLLOVER_STATIS 21   
#define PACKET_FANOUT_DATA 22   
#define PACKET_FANOUT_HASH 0   
#define PACKET_FANOUT_LB 1   
#define PACKET_FANOUT_CPU 2   
#define PACKET_FANOUT_ROLLOVER 3   
#define PACKET_FANOUT_RND 4   
#define PACKET_FANOUT_QM 5   
#define PACKET_FANOUT_CBPF 6   
#define PACKET_FANOUT_EBPF 7   
#define PACKET_FANOUT_FLAG_ROLLOVER 0x1000   
#define PACKET_FANOUT_FLAG_DEFRAG 0x8000 
```

# 8.1.3 Sock CheckSum

# 8.1.3.1 CheckSum 相关标志位

```c
\* A. Checksumming of received packets by device.   
\*   
\* CHECKSUM_NON:   
\*   
\* Device failed to checksum this packet e.g. due to lack of capabilities.   
\* The packet contains full (though not verified) checksum in packet but not in skb->csum. Thus, skb->csum is undefined in this case.   
\*   
\* CHECKSUM_UNNECESSARY:   
\*   
\* The hardware you're dealing with doesn't calculate the full checksum (as in CHECKSUM_COMPLETE), but it does parse headers and verify checksums for specific protocols. For such packets it will set CHECKSUM_UNNECESSARY if their checksums are okay. skb->csum is still undefined in this case though. It is a bad option, but, unfortunately, nowadays most vendors do this. Apparently with the secret goal to sell you new devices, when you will add new protocol to your host, f.e. IPv6 8) 
```

```txt
* CHECKSUM_UNNECESSARY is applicable to following protocols:
* TCP: IPv6 and IPv4.
* UDP: IPv4 and IPv6. A device may apply CHECKSUM_UNNECESSARY to a zero UDP checksum for either IPv4 or IPv6, the networking stack may perform further validation in this case.
* GRE: only if the checksum is present in the header.
* SCTP: indicates the CRC in SCTP header has been validated.
* skb->csum_level indicates the number of consecutive checksums found in the packet minus one that have been verified as CHECKSUM_UNNECESSARY.
* For instance if a device receives an IPv6->UDP->GRE->IPv4->TCP packet and a device is able to verify the checksums for UDP (possibly zero),
* GRE (checksum flag is set), and TCP-- skb->csum_level would be set to two. If the device were only able to verify the UDP checksum and not GRE, either because it doesn't support GRE checksum of because GRE checksum is bad, skb->csum_level would be set to zero (TCP checksum is not considered in this case).
* CHECKSUM_COMPLETE:
* This is the most generic way. The device supplied checksum of the whole packet as seen by netif_rx() and fills out in skb->csum. Meaning, the hardware doesn't need to parse L3/L4 headers to implement this.
* Note: Even if device supports only some protocols, but is able to produce skb->csum, it MUST use CHECKSUM_COMPLETE, not CHECKSUM_UNNECESSARY.
* CHECKSUM_PARTIAL:
* A checksum is set up to be offloaded to a device as described in the output description for CHECKSUM_PARTIAL. This may occur on a packet received directly from another Linux OS, e.g., a virtualized Linux kernel on the same host, or it may be set in the input path in GRO or remote checksum offload. For the purposes of checksum verification, the checksum referred to by skb->csum_start + skb->csum_offset and any preceding checksums in the packet are considered verified. Any checksums in the packet that are after the checksum being offloaded are not considered to be verified.
* B. Checksumming on output.
* CHECKSUM_NONE:
* The skb was already checksummed by the protocol, or a checksum is not required.
* CHECKSUM_PARTIAL:
* The device is required to checksum the packet as seen by hard_start_xmit()
* from skb->csum_start up to the end, and to record/write the checksum at offset skb->csum_start + skb->csum_offset.
* The device must show its capabilities in dev->features, set up at device setup time, e.g. netdev_features.h:
* NETIF_F_HW_CSUM - It's a clever device, it's able to checksum everything.
* NETIF_F_IP_CSUM - Device is dumb, it's able to checksum only TCP/UDP over IPv4. Sigh. Vendors like this way for an unknown reason. 
```

```c
77 * Though, see comment above about CHECKSUM_UNNECESSARY. 8)  
78 * NETIF_F_IPV6_CSUM - About as dumb as the last one but does IPv6 instead.  
79 * NETIF_F... - Well, you get the picture.  
80 *  
81 * CHECKSUM_UNNECESSARY:  
82 *  
83 * Normally, the device will do per protocol specific checksumming. Protocol  
84 * implementations that do not want the NIC(Network Interface Controller,网卡) to perform the checksum calculation should use this flag in their outgoing skbs.  
85 *  
86 * NETIF_F_FCOE_CRC - This indicates that the device can do FCoE FC CRC  
87 * offload. Correspondingly, the FCoE protocol driver  
88 * stack should use CHECKSUM_UNNECESSARY.  
89 *  
90 *  
91 * Any questions? No questions, good. --ANK  
92 *  
93  
94 /* Don't change this without changing skb_csum_unnecessary! */  
95 #define CHECKSUM_NONE 0  
96 #define CHECKSUM_UNNECESSARY 1  
97 #define CHECKSUM_COMPLETE 2  
98 #define CHECKSUM_PARTIAL 3 
```

8.1.3.2 skb_csum_unnecessary   
```c
/* Location: include/linux/skbuff.h */
Parameter:
    skb: 传输控制块缓存
*/
static inline int skb_csum_unnecessary(const struct sk_buffer *skb)
{
    return ((skb->ip_summed == CHECKSUM_UNNECESSARY) || skb->csum_valid || (skb->ip_summed == CHECKSUM_PARTIAL && skb_checksum_start_offset(skb) >= 0));
} 
```

如果不想要网卡帮忙校验或者 Checksum 合法，或者硬件给出的时部分校验并且校验的偏移必须大于零。则返回 1, 否则，返回 0。

8.1.3.3 __skb_checksum_complete   
```c
__sum16 __skb_checksum_COMPLETE(struct sk_buffer *skb)
{
    __wsum csum;
    __sum16 sum;
    csum = skb_checksum(skb, 0, skb->len, 0);
    /* skb->csum holds pseudo checksum */
    sum = csum_fold(csum_add(skb->csum, csum));
    if (likely(!sum)) { 
```

```c
if (unlikely(skb->ip_summed == CHECKSUM_COMPLETE) && !skb->csum_COMPLETE_sw) netdev_rx_csum_fault(skb->dev); } if (!skb_shared(skb)) { /* Save full packet checksum */ skb->csum = csum; skb->ip_summed = CHECKSUM_COMPLETE; skb->csum_COMPLETE_sw = 1; skb->csum_valid = !sum; } return sum; } 
```

# 8.1.4 SK Stream

Linux 内核中，提供了一套通用的处理网络数据流的函数。这一套函数是为了共享各种协议中，和处理数据流相关的代码，以实现代码重用。

# 8.1.5 sk_stream_wait_connect

该函数用于等待套接字完成连接。

/\*Location:net/core/stream.c   
\*   
\* Parameter:   
\* sk：套接字   
\* timeo_p：等待多长时间   
\*   
\* 该函数必须在sk被上锁的情况下被调用。   
\*/   
int sk_STREAM_wait_connect(struct sock \*sk，long \*timeo_p)   
{ struct task_struct \*tsk $=$ current; DEFINE_WAIT(wait); int done; do { int err $=$ sock_error(sk); if(err) return err; if((1<<sk->sk_state)&-(TCPF_SYN_SENT|TCPF_SYN_RECV)) return -EPIPE; if(!\*timeo_p) return -EAGAIN; if (signal_pending(tsk)) return sock_intr_errno(\*timeo_p); prepare_to_wait(sk_sleep(sk)，&wait，TASK_INTERRUPTIBLE); sk->sk_write_pending++; /\*等待TCP进入连接建立状态\*/ done $=$ sk_wait_event(sk，timeo_p, !sk->sk_err && !((1<<sk->sk_state)& \~(TCPFEstablishEDI | TCPF_CLOSE_WAIT)))；

```txt
33 finish_wait(sk_sleep(sk), &wait);   
34 sk->sk_write_pending--;   
35 } while (!done);   
36 return 0;   
37 } 
```

# 8.1.6 pskb_may_pull

我们首先需要直到的是收到的包的数据在 sk_buff 中是如何组织的。数据先会出现在 sk_buff- $>$ data 中，也就是线性数据缓冲区，多余的数据就放在 skb_shinfo(skb)->frag[] 当中，这些数据存在于所谓的非线性缓冲区当中，这些数据都存在于 unmappedpage 当中，这是用于支持驱动的分散/聚集 I/O 的。另外，还有一部分存在于 skb_shinfo(skb)->frag_list 当中，这是一个 sk_buff 结构的链表。所以，对 shk_shinfo(skb)- $\cdot >$ frag_list当中的 sk_buff 中的数据可以进行上述的递归表达。

造成上述的原因是数据可能会分片。

如果数据本身的头部长度就已经大于给定的头部长度了，说明合法。如果给定长度大于第一个分片的全部长度，显然不合法，如果都不是，那么就需要调用__pskb_pull_tail处理剩下的分片了，这里我们就不详细叙述了。

```c
1 /\* Location: include/linux/tcp BUFF.h   
3   
4   
5   
6   
7   
8   
9   
10   
11   
12   
13   
14   
15 static inline int pskb可能會Pull(struct skbuff \*skb, unsigned int len) { if (likely(len <= skb_headlen(sk))) return 1; if (unlikely(len > skb->len)) return 0; return \_\_pskb_PULL谥(sk, len - skb_headlen(sk)) != NULL; } 
```

调用位置:

1 4.2.1.1中的 tcp_v4_rcv 调用。

# 8.2 Inet

#

这个函数是用于连接到一个远程的主机的。

```c
/* Location: net/ipv4/af_inet.c  
*  
* Function: Connect to a remote host. There is regrettably still a little  
* TCP 'magic' in here.  
*  
* Parameter:  
* sock: 发起连接的套接字  
* uaddr: 目标地址  
* addr_len: sockaddr 结构体的长度  
* flags: 标志位  
*/  
int __inet_STREAM_connect(struct socket *sock, struct sockaddr *uaddr, int addr_len, int flags)  
{  
    struct sock *sk = sock->sk;  
    int err;  
    long timeo;  
/* 判断长度和协议 */  
if (addr_len < sizeof(uaddr->sa_family))  
    return -EINVALID;  
if (uaddr->sa_family == AF_UNSPEC) {  
    err = sk->skprot->disconnect(sk, flags);  
    sock->state = err ? SS_DISCONNECTING : SS_UNCONNECTED;  
    goto out;  
}  
/* 根据当前的状态执行相应的功能  
* 处于已连接或者其他状态返回错误值。  
* 如果处于未连接状态则调用传输层的连接函数，实现连接远程主机的功能。  
*/  
switch (sock->state) {  
    default:  
        err = -EINVALID;  
        goto out;  
case SS_CONNECTED:  
            err = -EISCONN;  
            goto out;  
case SS_CONNECTED:  
            err = -EALREADY;  
/* Fall out of switch with err, set for this state */  
break;  
case SS_UNCONNECTED:  
    err = -EISCONN;  
if (sk->sk_state != TCP_CLOSE)  
    goto out;  
err = sk->sk_prot->connect(sk, uaddr, addr_len);  
if (err < 0)  
    goto out;  
sock->state = SS_CONNECTED;  
/* Just entered SS_CONNECTED state; the only difference is that return value in non-blocking case is EINPROGRESS, rather than EALREADY. 
```

err = -EINPROGRESS; break;   
}   
timeo $=$ sock_sndtimeo(sk, flags & 0_NONBLOCK);   
if ((1 << sk->sk_state) & (TCPF_SYN_SENT | TCPF_SYN_RECV)) { int writebias $=$ (sk->sk_protocol $= =$ IPPROTO_TCP)&& tcp_sk(sk)->fastopen_req && tcp_sk(sk)->fastopen_req->data?1:0; /\* Error code is set above \*/ if(!timeo||!inet_wait_for_connect(sk,timeeo,writebias)) goto out; err $=$ sock_intr_errno(timeo); if (signal_pending(current)) goto out;   
}   
/\* Connection was closed by RST, timeout, ICMP error \* or another process disconnected us. \*/ if (sk->sk_state $= =$ TCP_CLOSE) goto sock_error; /\* sk->sk_err may be not zero now, if RECVERR was ordered by user \* and error was received after socket entered established state. \* Hence, it is handled normally after connect() return successfully. \*/ sock->state $=$ SS_CONNECTED; err $= 0$ .   
out: return err;   
sock_error: err $=$ sock_error(sk)? : -ECONNABORTED; sock->state $=$ SS_UNCONNECTED; if (sk->skprot->disconnect(sk, flags)) sock->state $=$ SS_DISCONNECTING; goto out;

# 8.2.2 inet_hash_connect && __inet_hash_connect

# 8.2.2.1 inet_hash_connect

/\* \*Bind a port for a connect operation and hash it. \*/ int inset_hash_connect(struct inset.timewait_death_row \*death_row, struct sock \*sk) { u32 port_offset $= 0$ if(!inet_sk(sk)->inet_num) port_offset $=$ inlet_sk_port_offset(sk);

```c
return __inet_hash_connect(death_row, sk, port_offset, __inet_check_established); } 
```

8.2.2.2 __inet_hash_connect   
int __inet_hash_connect(struct insettimewait_death_row *death_row, struct sock *sk, u32 port_offset, int (*check_established)(struct inset timewait_death_row *, struct sock *, __u16, struct inset timewait SOCK *)) { struct inset_hashinfo *hinfo = death_row->hashinfo; const unsigned short snum = inset_sk(sk) ->inet_num; struct inset_bind_hashbucket *head; struct inset_bind_BUCKET *tb; int ret; struct net *net = sock_net(sk); if (!snum) { int i, remaining, low, high, port; static u32 hint; u32 offset = hint + port_offset; struct inset.timewait SOCK *tw = NULL; inset_get_local_port_range(net, &low, &high); remaining = (high - low) + 1; /* By starting with offset being an even number, * we tend to leave about $50\%$ of ports for other uses, * like bind(0). */ offset &= ~1; local_bh_disable(); for $(i = 0; i <$ remaining; $i++)$ { port = low + (i + offset) % remaining; if (inet_is_local_reserved_port(net, port)) continue; head = &hinfo->bhash[inet_bhashfn(net, port, hinfo->bhash_size)]; spin_lock(&head->lock); /* Does not bother with rcv_saddr checks, * because the established check is already * unique enough. */ inlet_bind_buckets_for_each(tb, &head->chain) { if (net_eq(ab_net(tb), net) && tb->port == port) { if (tb->fastreuse >= 0 || tb->fastreuseport >= 0) goto next_port; WARNING_ON(hlist_empty(&tb->owners)); if (!check_established(death_row, sk, port, &tw)) goto ok; goto next_port; }

} tb $=$ inlet_bind:bucket_create(hinfo->bind:bucket_cachep, net, head, port); if(!tb){ spin_unlock(&head->lock); break; } tb->fastreuse $= -1$ . tb->fastreuseport $= -1$ goto ok; next_port: spin_unlock(&head->lock); 1 local_bh_enable(); return-EADDRNOTAVAIL; ok: hint $+ =$ (i +2)&~1; /\*Head lock still held and bh's disabled \*/ inlet_bind_hash(sk, tb, port); if(sk_unhashed(sk)){ inlet_sk(sk)->inet_sport $\equiv$ htons.port); inlet_ehash_nolisten(sk, (struct sock \*)tw); } if(tw) inlet_twsk_bind_unhash(tw,hinfo); spin_unlock(&head->lock); if(tw) inlet_twsk_deschedule_put(tw); ret $= 0$ goto out; } head $=$ &hinfo->bhash[inet_bhashfn(net,snum,hinfo->bhash_size)]; tb $=$ inlet_csk(sk)->icsk_bind_hash; spin_lock_bh(&head->lock); if(sk_head(&tb->owners) $= =$ sk &&!sk->sk_bind_node.next){ inlet_ehash_nolisten(sk, NULL); spin_unlock_bh(&head->lock); return 0; } else{ spin_unlock(&head->lock); /\*No definite answer... Walk to established hash table \*/ ret $=$ check_established(death_row,sk,snum,NULL); out: local_bh_enable(); return ret; }

# 8.2.3 inet_csk_reqsk_queue_add

/\*   
Location:   
\*/   
struct sock \*inet_csk_reqsk_queue_add(struct sock \*sk, struct request_sock \*req, struct sock \*child)   
{ struct request_sock_queue \*queue $=$ &inet_csk(sk)->icsk_accept_queue; spin_lock(&queue->rskq_lock); if (unlikely(sk->sk_state != TCP_LISTEN)) { inlet_child Forget(sk, req, child); child $=$ NULL; } else { req->sk $=$ child; req->dl_next $=$ NULL; if (queue->rskq.accept_head $= =$ NULL) queue->rskq.accept_head $=$ req; else queue->rskq.accept.tail->dl_next $=$ req; queue->rskq.accept.tail $=$ req; sk_acceptq-added(sk); } spin_unlock(&queue->rskq_lock); return child;

这一个函数所进行的操作就是直接将请求挂在接收队列中。

调用位置:

1 8.6.1.5中的 tcp_conn_request 调用。

# 8.2.3.1 inet_csk_reqsk_queue_hash_add

```c
void inlet_csk_reqsk_queue_hash_add(struct sock \*sk, struct request_sock \*req, unsigned long timeout)   
{ reqsk_queue_hash_req(req, timeout); inlet_csk_reqsk_queue-added(sk); 
```

首先将连接请求块保存到父传输请求块的散列表中，并设置定时器超时时间。之后更新已存在的连接请求块数，并启动连接建立定时器。

调用位置:

1 8.6.1.5中的 tcp_conn_request 调用。

# 8.2.4 inet_twsk_put

该函数用于释放inet_timewait_sock结构体。

```c
void inlet_twsk_put(struct inlet.timewait_sock *tw) {
    /* 减小引用计数，如果计数为 0，则释放它 */
    if (atomic_dec_and_test(&tw->tw_refcnt))
        inlet_twsk_free(tw);
}
```

1 4.2.1.1中的 tcp_v4_rcv 调用。

# 8.3 TCP 相关参数

tcp_syn_retries INTEGER, 默认值是 5 对于一个新建连接，内核要发送多少个 SYN 连接请求才决定放弃。不应该大于 255，默认值是 5，对应于 180 秒左右时间。(对于大负载而物理通信良好的网络而言, 这个值偏高, 可修改为 2. 这个值仅仅是针对对外的连接, 对进来的连接, 是由 tcp_retries1 决定的)

tcp_synack_retries INTEGER, 默认值是 5 对于远端的连接请求 SYN，内核会发送 SYN ＋ ACK 数据报，以确认收到上一个 SYN 连接请求包。这是所谓的三次握手 ( threeway hand-shake) 机制的第二个步骤。这里决定内核在放弃连接之前所送出的 SYN+ACK数目。不应该大于 255，默认值是 5，对应于 180 秒左右时间。(可以根据上面的tcp_syn_retries 来决定这个值)

tcp_keepalive_time INTEGER, 默认值是 7200(2 小时) 当 keepalive 打开的情况下，TCP 发送 keepalive消息的频率。(由于目前网络攻击等因素, 造成了利用这个进行的攻击很频繁, 曾经也有 cu 的朋友提到过, 说如果 2 边建立了连接, 然后不发送任何数据或者 rst/fin消息, 那么持续的时间是不是就是 2 小时, 空连接攻击?tcp_keepalive_time 就是预防此情形的. 我个人在做 nat 服务的时候的修改值为 1800 秒)

tcp_keepalive_probes INTEGER, 默认值是 9 TCP 发送 keepalive 探测以确定该连接已经断开的次数。(注意: 保持连接仅在 SO_KEEPALIVE 套接字选项被打开是才发送. 次数默认不需要修改, 当然根据情形也可以适当地缩短此值. 设置为 5 比较合适)

tcp_keepalive_intvl INTEGER, 默认值为 75 探测消息发送的频率，乘以 tcp_keepalive_probes 就得到对于从开始探测以来没有响应的连接杀除的时间。默认值为 75 秒，也就是没有活动的连接将在大约 11 分钟以后将被丢弃。(对于普通应用来说, 这个值有一些偏大, 可以根据需要改小. 特别是 web 类服务器需要改小该值,15 是个比较合适的值)

tcp_retries1 INTEGER, 默认值是 3 放弃回应一个 TCP 连接请求前﹐需要进行多少次重试。RFC 规定最低的数值是 3﹐这也是默认值﹐根据 RTO 的值大约在 3 秒 - 8 分钟之间。(注意: 这个值同时还决定进入的 syn 连接)

tcp_retries2 INTEGER, 默认值为 15 在丢弃激活 (已建立通讯状况) 的 TCP 连接之前﹐需要进行多少次重试。默认值为 15，根据 RTO 的值来决定，相当于 13-30 分钟 (RFC1122规定，必须大于 100 秒).(这个值根据目前的网络设置, 可以适当地改小, 我的网络内修改为了 5)

tcp_orphan_retries INTEGER, 默认值是 7 在近端丢弃 TCP 连接之前﹐要进行多少次重试。默认值是 7 个﹐相当于 50 秒 - 16 分钟﹐视 RTO 而定。如果您的系统是负载很大的 web服务器﹐那么也许需要降低该值﹐这类 sockets 可能会耗费大量的资源。另外参的考 tcp_max_orphans 。(事实上做 NAT 的时候, 降低该值也是好处显著的, 我本人的网络环境中降低该值为 3)

tcp_fin_timeout INTEGER, 默认值是 60 对于本端断开的 socket 连接，TCP 保持在 FIN-WAIT-2状态的时间。对方可能会断开连接或一直不结束连接或不可预料的进程死亡。默认值为 60 秒。过去在 2.2 版本的内核中是 180 秒。您可以设置该值﹐但需要注意﹐如果您的机器为负载很重的 web 服务器﹐您可能要冒内存被大量无效数据报填满的风险﹐FIN-WAIT-2 sockets 的危险性低于 FIN-WAIT-1 ﹐因为它们最多只吃 1.5K 的内存﹐但是它们存在时间更长。另外参考 tcp_max_orphans。(事实上做 NAT 的时候, 降低该值也是好处显著的, 我本人的网络环境中降低该值为 30)

tcp_max_tw_buckets INTEGER, 默认值是 180000 系统在同时所处理的最大 timewait sockets 数目。如果超过此数的话﹐time-wait socket 会被立即砍除并且显示警告信息。之所以要设定这个限制﹐纯粹为了抵御那些简单的 DoS 攻击﹐千万不要人为的降低这个限制﹐不过﹐如果网络条件需要比默认值更多﹐则可以提高它 (或许还要增加内存)。(事实上做 NAT 的时候最好可以适当地增加该值)

tcp_tw_recycle BOOLEAN, 默认值是 0, 打开快速 TIME-WAIT sockets 回收。除非特别熟悉，否则，请不要随意修改这个值。建议做 NAT 的时候打开。

tcp_tw_reuse BOOLEAN, 默认值是 0 该文件表示是否允许重新应用处于 TIME-WAIT 状态的socket 用于新的 TCP 连接 (这个对快速重启动某些服务, 而启动后提示端口已经被使用的情形非常有帮助)

tcp_max_orphans INTEGER, 缺省值是 8192 系统所能处理不属于任何进程的 TCP sockets 最大数量。假如超过这个数量﹐那么不属于任何进程的连接会被立即 reset，并同时显示警告信息。之所以要设定这个限制﹐纯粹为了抵御那些简单的 DoS 攻击﹐千万不要依赖这个或是人为的降低这个限制 (这个值 Redhat AS 版本中设置为 32768, 但是很多防火墙修改的时候, 建议该值修改为 2000)

cp_abort_on_overflow BOOLEAN, 缺省值是 0 当守护进程太忙而不能接受新的连接，就象对方发送 reset消息，默认值是 false。这意味着当溢出的原因是因为一个偶然的猝发，那么连接将恢复状态。只有在你确信守护进程真的不能完成连接请求时才打开该选项，该选项会影响客户的使用。(对待已经满载的 sendmail,apache 这类服务的时候, 这个可以很快让客户端终止连接, 可以给予服务程序处理已有连接的缓冲机会, 所以很多防火墙上推荐打开它)

tcp_syncookies BOOLEAN, 默认值是 0 只有在内核编译时选择了 CONFIG_SYNCOOKIES 时才会发生作用。当出现 syn 等候队列出现溢出时象对方发送 syncookies。目的是为了防止 syn flood 攻击。注意：该选项千万不能用于那些没有收到攻击的高负载服

务器，如果在日志中出现 synflood 消息，但是调查发现没有收到 synflood 攻击，而是合法用户的连接负载过高的原因，你应该调整其它参数来提高服务器性能。参考:tcp_max_syn_backlog、tcp_synack_retries、tcp_abort_on_overflow。syncookie严重的违背 TCP 协议，不允许使用 TCP 扩展，可能对某些服务导致严重的性能影响 (如 SMTP 转发)。(注意, 该实现与 BSD 上面使用的 tcp proxy 一样, 是违反了 RFC 中关于 tcp 连接的三次握手实现的, 但是对于防御 syn-flood 的确很有用.)

tcp_stdurg BOOLEAN, 默认值为 0 使用 TCP urg pointer 字段中的主机请求解释功能。大部份的主机都使用老旧的 BSD 解释，因此如果您在 Linux 打开它﹐或会导致不能和它们正确沟通。

tcp_max_syn_backlog

INTEGER, 对于那些依然还未获得客户端确认的连接请求﹐需要保存在队列中最大数目。对于超过 128Mb 内存的系统﹐默认值是 1024 ﹐低于 128Mb 的则为128。如果服务器经常出现过载﹐可以尝试增加这个数字。警告﹗假如您将此值设为大于 1024﹐最好修改 include/net/tcp.h 里面的 TCP_SYNQ_HSIZE ﹐以保持TCP_SYNQ_HSIZE $^ { * } 1 6 < =$ tcp_max_syn_backlog ﹐并且编进核心之内。(SYNFlood 攻击利用 TCP 协议散布握手的缺陷，伪造虚假源 IP 地址发送大量 TCP-SYN 半打开连接到目标系统，最终导致目标系统 Socket 队列资源耗尽而无法接受新的连接。为了应付这种攻击，现代 Unix 系统中普遍采用多连接队列处理的方式来缓冲 (而不是解决) 这种攻击，是用一个基本队列处理正常的完全连接应用(Connect() 和 Accept() )，是用另一个队列单独存放半打开连接。这种双队列处理方式和其他一些系统内核措施 (例如 Syn-Cookies/Caches) 联合应用时，能够比较有效的缓解小规模的 SYN Flood 攻击 (事实证明 ${ < } 1 0 0 0 \mathrm { p / s } )$ 加大 SYN 队列长度可以容纳更多等待连接的网络连接数，所以对 Server 来说可以考虑增大该值.)

tcp_window_scaling

BOOL, 缺省值为 1, 该标志表示 tcp/ip 会话的滑动窗口大小是否可变。参数值为布尔值，为 1 时表示可变，为 0 时表示不可变。tcp/ip 通常使用的窗口最大可达到 65535 字节，对于高速网络，该值可能太小，这时候如果启用了该功能，可以使tcp/ip 滑动窗口大小增大数个数量级，从而提高数据传输的能力 (RFC 1323)。当然，对普通的百 M 网络而言，关闭会降低开销，所以如果不是高速网络，可以设置为 0。

tcp_timestamps

BOOLEAN, 缺省值为 1，该标志表示是否启用以一种比超时重发更精确的方法（RFC 1323）来启用对 RTT 的计算，为了实现更好的性能应该启用这个选项。同时，Timestamps 可以防范那些伪造的 sequence 序列号，一条 1G 的宽带线路或许会重遇到带 out-of-line 数值的旧 sequence 号码 (假如它是由于上次产生的)。Times-tamp 会让它知道这是个’ 旧封包’。

tcp_sack BOOLEAN, 缺省值为 1，该标志表示是否启用有选择的应答（Selective Acknowl-edgment），这可以通过有选择地应答乱序接收到的报文来提高性能（这样可以让发送者只发送丢失的报文段）。它可以用来查找特定的遗失的数据报，因此有助于快速恢复状态。对于广域网通信来说这个选项应该启用，但是这会增加对 CPU 的占用。

tcp_fack BOOLEAN, 缺省值为 1 打开 FACK 拥塞避免和快速重传功能。(注意，当 tcp_sack设置为 0 的时候，这个值即使设置为 1 也无效)  
tcp_dsack BOOLEAN, 缺省值为 1 允许 TCP 发送" 两个完全相同" 的 SACK。  
tcp_ecn BOOLEAN, 缺省值为 0 打开 TCP 的直接拥塞通告功能。  
tcp_reordering INTEGER, 默认值是 3 TCP 流中重排序的数据报最大数量。(一般有看到推荐把这个数值略微调整大一些, 比如 5)  
tcp_retrans_collapse BOOLEAN，缺省值为 1 对于某些有 bug 的打印机提供针对其 bug 的兼容性。(一般不需要这个支持, 可以关闭它)  
(3 个 INTEGER 变量) min, default, max。min：为 TCP socket 预留用于发送缓冲的内存最小值。每个tcp socket 都可以在建议以后都可以使用它。默认值为 4096(4K)。  
default：为 TCP socket 预留用于发送缓冲的内存数量，默认情况下该值会影响其它协议使用的 net.core.wmem_default 值，一般要低于 net.core.wmem_default 的值。默认值为 16384(16K)。  
max: 用于 TCP socket 发送缓冲的内存最大值。该值不会影响 net.core.wmem_max，" 静态" 选择参数 SO_SNDBUF 则不受该值影响。默认值为 131072(128K)。（对于服务器而言，增加这个参数的值对于发送数据很有帮助, 在我的网络环境中, 修改为了 51200 131072 204800）  
tcp_rmem (3 个 INTEGER 变量)min, default, max。min：为 TCP socket 预留用于接收缓冲的内存数量，即使在内存出现紧张情况下 tcp socket 都至少会有这么多数量的内存用于接收缓冲，默认值为 8K。  
default：为TCP socket预留用于接收缓冲的内存数量，默认情况下该值影响其它协议使用的 net.core.wmem_default 值。该值决定了在 tcp_adv_win_scale、tcp_app_win和 tcp_app_win $\mathord { = } 0$ 默认值情况下，TCP 窗口大小为 65535。默认值为 87380  
max：用于 TCP socket 接收缓冲的内存最大值。该值不会影响 net.core.wmem_max，" 静态" 选择参数 SO_SNDBUF 则不受该值影响。默认值为 128K。默认值为$8 7 3 8 0 ^ { * } 2$ bytes。（可以看出，.max 的设置最好是 default 的两倍, 对于 NAT 来说主要该增加它, 我的网络里为 51200 131072 204800）  
tcp_mem (3 个 INTEGER 变量)low, pressure, high。low：当 TCP 使用了低于该值的内存页面数时，TCP 不会考虑释放内存。(理想情况下，这个值应与指定给 tcp_wmem的第 2 个值相匹配 - 这第 2 个值表明，最大页面大小乘以最大并发请求数除以页大小 $\left( 1 3 1 0 7 2 \mathrm { ~ ^ * ~ } 3 0 0 \mathrm { ~ / ~ } 4 0 9 6 \right) \circ$ )  
pressure：当 TCP 使用了超过该值的内存页面数量时，TCP 试图稳定其内存使用，进入 pressure 模式，当内存消耗低于 low 值时则退出 pressure 状态。(理想情况下这个值应该是 TCP 可以使用的总缓冲区大小的最大值 $\left( 2 0 4 8 0 0 \mathrm { ~ ^ { * } ~ 3 0 0 ~ / ~ 4 0 9 6 } \right) \mathrm { { c } }$ 。)

high：允许所有 tcp sockets 用于排队缓冲数据报的页面量。(如果超过这个值，TCP连接将被拒绝，这就是为什么不要令其过于保守 $\left( 5 1 2 0 0 0 \cdots 3 0 0 \Big / 4 0 9 6 \right)$ 的原因了。在这种情况下，提供的价值很大，它能处理很多连接，是所预期的 2.5 倍；或者使现有连接能够传输 2.5 倍的数据。我的网络里为 192000 300000 732000)

一般情况下这些值是在系统启动时根据系统内存数量计算得到的。

tcp_app_win INTEGER, 默认值是 31 保留 $\operatorname* { m a x } ( w i n d o w / 2 ^ { t c p \_ a p p \_ w i n }$ , mss) 数量的窗口由于应用缓冲。当为 0 时表示不需要缓冲。

tcp_adv_win_scale INTEGER, 默认值为 2 计算缓冲开销 bytes/2tcp_adv_win_scale(如果 tcp_adv_win_scale$> 0$ ) 或者 bytes − bytes/2−tcp_adv_win_scale(如果 tcp_adv_win_scale $< = 0$ ）。

tcp_rfc1337 BOOLEAN, 缺省值为 0 这个开关可以启动对于在 RFC1337 中描述的"tcp 的 time-wait 暗杀危机" 问题的修复。启用后，内核将丢弃那些发往 time-wait 状态 TCP套接字的 RST 包.

tcp_low_latency BOOLEAN, 缺省值为 0。该段的英文阐述如下：If set, the TCP stack makes decisions that prefer lower latency as opposed to higher throughput. By default, this option is not set meaning that higher throughput is preferred. An example of an application where this default should be changed would be a Beowulf compute cluster. 可以看出该选项在设置的时候主要是用于高吞吐量的环境，而设置之后， 则是主要用于低延时的环境。这个选项一般情形是的禁用，但在构建 Beowulf 集 群 (超算) 的时候, 打开它很有帮助。

tcp_westwood BOOLEAN, 缺省值为 0, 启用发送者端的拥塞控制算法，它可以维护对吞吐量的评估，并试图对带宽的整体利用情况进行优化；对于 WAN 通信来说应该启用这个选项。

tcp_bic BOOLEAN, 缺省值为 0, 为快速长距离网络启用 Binary Increase Congestion；这样可以更好地利用以 GB 速度进行操作的链接；对于 WAN 通信应该启用这个选项。

# 8.3.1 TCP 标志宏

```c
1 #define FLAG_DATA 0x01 /* Incoming frame contained data. */
2 #define FLAG WIN_UPDATE 0x02 /* Incoming ACK was a window update. */
3 #define FLAG_DATA_ACKED 0x04 /* This ACK acknowledged new data. */
4 #define FLAG_RETRANS_DATA_ACKED 0x08 /* "" "" some of which was retransmitted. */
5 #define FLAG_SYN_ACKED 0x10 /* This ACK acknowledged SYN. */
6 #define FLAG_DATA_SACKED 0x20 /* New SACK. */
7 #define FLAG_ECE 0x40 /* ECE in this ACK */
8 #define FLAG_LOST_RETRANS 0x80 /* This ACK marks some retransmission lost */
9 #define FLAG_SLOWPATH 0x100 /* Do not skip RFC checks for window update. */
10 #define FLAG_ORIG_SACK_ACKED 0x200 /* Never retransmitted data are (s)acked */
11 #define FLAG SND_UNA_ADVANCED 0x400 /* SND_una was changed (!= FLAG_DATA_ACKED) */
12 #define FLAG_DSACKING_ACK 0x800 /* SACK blocks contained D-SACK info */
13 #define FLAG_SACK_RENEGING 0x2000 /* SND_una advanced to a sacked seq */ 
```

14 #define FLAG_UPDATE_TS_RECENT $\mathsf{0x4000} / *\mathsf{tcp\_replace\_ts\_recent()}*$ 15   
16 #define FLAG_ACKED (FLAG_DATA_ACKED/FLAG_SYN_ACKED)   
17 #define FLAG_NOT_DUP (FLAG_DATA/FLAG WIN_UPDATE/FLAG_ACKED)   
18 #define FLAG_CA_ALERT (FLAG_DATA_SACKED/FLAG_ECE)   
19 #define FLAG FORWARD PROGRESS (FLAG_ACKED/FLAG_DATA_SACKED)   
20   
21 #define TCP_REMNANT (TCP_FLAG_FIN/TCP_FLAG_URG/TCP_FLAG_SYN/TCP_FLAG_PSH)   
22 #define TCP_HP BITS (~(TCP_RESERVED BITS/TCP_FLAG_PSH))

这里我们需要介绍几个相关的概念，如下：

SACK Selective Acknowledgment(SACK)，这种方式需要在 TCP 头里加一个 SACK 的东西，ACK 还是 Fast Retransmit 的 ACK，SACK 则是汇报收到的数据碎版。这样，在发送端就可以根据回传的 SACK 来知道哪些数据到了，哪些没有到。于是就优化了 Fast Retransmit 的算法。当然，这个协议需要两边都支持。

Reneging 所谓 Reneging 的意思就是违约，接收方有权把已经报给发送端 SACK 里的数据给丢了。当然，我们肯定是不鼓励这样做的，因为这个事会把问题复杂化。但是，接收方可能会由于一些极端情况这么做，比如要把内存给别的更重要的东西。所以，发送方也不能完全依赖 SACK，主要还是要依赖 ACK，并维护 Time-Out。如果后续的 ACK 没有增长，那么还是要把 SACK 的东西重传，另外，接收端这边永远不能把 SACK 的包标记为 Ack。 注意：SACK 会消费发送方的资源，试想，如果一个攻击者给数据发送方发一堆 SACK 的选项，这会导致发送方开始要重传甚至遍历已经发出的数据，这会消耗很多发送端的资源。

D-SACK Duplicate SACK 又称 D-SACK，其主要使用了 SACK 来告诉发送方有哪些数据被重复接收了。D-SACK 使用了 SACK 的第一个段来做标志，如果 SACK 的第一个段的范围被 ACK 所覆盖，那么就是 D-SACK。如果 SACK 的第一个段的范围被 SACK 的第二个段覆盖，那么就是 D-SACK。引入了 D-SACK，有这么几个好处：　 1）可以让发送方知道，是发出去的包丢了，还是回来的 ACK 包丢了。

2）是不是自己的 timeout 太小了，导致重传。  
3）网络上出现了先发的包后到的情况（又称 reordering）  
4）网络上是不是把我的数据包给复制了。

# 调用位置:

1 6.1.1.6的 tcp_enter_loss 中调用。

# 8.3.2 函数宏

# 8.3.2.1 before & after

在一些需要判断序号前后的地方出现了before()和after() 这两个函数。这两个函数的定义如下

```c
/* include/net/tcp.h */
* 比较两个无符号 32 位整数
*/
static inline bool before(_u32 seq1, _u32 seq2)
{
    return (_s32)(seq1-seq2) < 0;
}
#define after(seq2, seq1) before(seq1, seq2)
```

可以看到，这两个函数实际上就是将两个数直接相减。之所以要单独弄个函数应该是为了避免强制转型造成影响。序号都是 32 位无符号整型。

# 8.3.2.2 between

```css
/* is s2<=s1<=s3 ? */
static inline bool between(_u32 seq1, _u32 seq2, _u32 seq3)
{
    return seq3 - seq2 >= seq1 - seq2;
} 
```

# 8.4 TCP CheckSum

# 8.4.1 tcp_checksum_complete

该函数时基于伪首部累加和，完成全包校验和的检测，值得注意的是，该函数用于校验没有负载的 TCP 段。如果需要校验，并且校验成功，则返回 1。

```c
1 /\*   
2 Location: include/net/tcp.h   
3 \*/   
4 static inline bool tcp_checksum_COMPLETE(struct sk BUFF \*skb)   
5 { return !skb_csum_unnecessary(skb) &&   
6 --tcp_checksum_COMPLETE(skb);   
7 } 
```

对于__tcp_checksum_complete函数，如下：

```c
1 /\*   
2 Location: include/net/tcp.h   
3 \*/   
4 static inline __sum16 __tcp_checksum_COMPLETE(struct sk BUFF \*skb) { return __skb_checksum_COMPLETE(skb);   
7 
```

对于函数__skb_checksum_complete, 参见 BSD Socket 处的叙述。

调用位置:

1 5.2.2.2中的 tcp_v4_do_rcv 调用。

# 8.4.2 tcp_v4_checksum_init

1

/* 没找到。。*/

# 8.5 TCP Initialize

# 8.5.1 TCP Initialize Handle

# 8.5.1.1 tcp_connnect_init

/* Do all connect socket setups that can be done AF independent. */   
static void tcp_connect_init(struct sock \*sk)   
{ const struct dst_entry \*dst $=$ __sk.dst_get(sk); struct tcp SOCK \*tp $=$ tcp_sk(sk); __u8 rcv_wscale; /* We'll fix this up when we get a response from the other end. \* See tcp_input.c:tcp_rcv_state_process case TCP_SYNSENT. \*/ tp->tcp_header_len $=$ sizeof(struct tcphdr) + (sysctl.tcp_timestamps ? TCPOLEN_TSTAMP_ALIGNED : 0);   
#ifndef CONFIG.tcp_MD5SIG if (tp->afspecific->md5.lookup(sk, sk)) tp->tcp_header_len += TCPOLEN_MD5SIG_ALIGNED;   
#endif /* If user gave his TCP_MAXSEG, record it to clamp */ if (tp->rx_opt.user_mss) tp->rx_opt.mss_clamp = tp->rx_opt.user_mss; tp->max_window = 0; tcp_mtop_init(sk); tcp-sync_mss(sk, dst_mtu(dst)); tcp_ca_dst_init(sk, dst); if (!tp->window_clamp) tp->window_clamp = dst_metric(dst, RTAX WINDOW); tp->advmss = dst_metric_advmss(dst); if (tp->rx_opt.user_mss && tp->rx_opt.user_mss < tp->advmss) tp->advmss = tp->rx_opt.user_mss; tcp_initializer_rcv_mss(sk); /* limit the window selection if the user enforce a smaller rx buffer */ if (sk->sk_userlocks & SOCK_RCVBUF_LOCK && (tp->window_clamp > tcp_full_space(sk) || tp->window_clamp == 0)) tp->window_clamp = tcp_full_space(sk); tcp_select_init_window(tcp_full_space(sk), tp->advmss - (tp->rx_opt.tsrecent_stamp ? tp->tcp_header_len - sizeof(struct tcphdr &tp->rcv_wnd, &tp->window_clamp, sysctl.tcp_window_scaling, &rcv_wscale, dst_metric(dst, RTAX_INITRWND));

```txt
tp->rx_opt.rcv_wscale = rcv_wscale;  
tp->rcv_ssthresh = tp->rcv_wnd;  
sk->sk_err = 0;  
sock_reset_flag(sk, SOCK_DONE);  
tp->snd_wnd = 0;  
tcp_init wl(tp, 0);  
tp->snd_una = tp->write_seq;  
tp->snd_sml = tp->write_seq;  
tp->snd_up = tp->write_seq;  
tp->snd_nxt = tp->write_seq;  
if (likely(!tp->repair))  
tp->rcv_nxt = 0;  
else  
tp->rcv_tstamp = tcp_time_stamp;  
tp->rcv_wup = tp->rcv_nxt;  
tp->copied_seq = tp->rcv_nxt;  
inet_csk(sk) ->icsk_rto = TCP_TIMEOUT_INIT;  
inet_csk(sk) ->icsk_retransmits = 0;  
tcp_clear_retrans(tp); 
```

# 调用位置:

1 5.1.2.3的 tcp_connect 中调用。

# 8.5.1.2 tcp_init_nondata_skb

该函数提供了初始化不含数据的 skb 的功能。

/\*   
Location:   
net/ipv4/tcp_output.c   
Function: 初始化不含数据的skb.   
Parameter: skb：待初始化的sk BUFF。 seq：序号 flags：标志位   
\*/ static void tcp_init_nodata_skb(struct sk BUFF \*skb，u32 seq，u8 flags) { /\*设置校验码\*/ skb->ip_summed $=$ CHECKSUM_PARTIAL; skb->csum $= 0$ · /\*设置标志位\*/ TCP_SKB_CB(sk)->tcp_flags $=$ flags; TCP_SKB_CB(sk)->sacked $= 0$

```c
tcp_skb_pcount_set(skb, 1);  
/* 设置起始序号 */  
TCP_SKB_CB(skb) -> seq = seq;  
if (flags & (TCPHDR_SYN | TCPHDR_FIN))  
seq++;  
TCP_SKB_CB(skb) -> end_seq = seq; 
```

# 调用位置:

1 5.1.2.3中的 tcp_connect 调用。  
2 5.1.4.2中的 tcp_send_ack 调用。

# 8.6 TCP Options

8.6.0.1 TCP Options Flags   
```c
1 /\*   
2 Location   
3 include/net/tcp.h   
4 Description:   
5   
6 TCP option   
7   
8   
9 \*/   
10 #define TCPOPT_NOP 1 /\* Padding \*/   
11 #define TCPOPT_EOL 0 /\* End of options \*/   
12 #define TCPOPT_MSS 2 /\* Segment size negotiating \*/   
13 #define TCPOPTWINDOW 3 /\* Window scaling \*/   
14 #define TCPOPT_SACK_PERM 4 /\* SACK Permitted \*/   
15 #define TCPOPT_SACK 5 /\* SACK Block \*/   
16 #define TCPOPT_TIMESTAMP 8 /\* Better RT estimations/PAWS \*/   
17 #define TCPOPT_MD5SIG 19 /\* MD5 Signature (RFC2385) \*/   
18 #define TCPOPT_FASTOPEN 34 /\* Fast open (RFC7413) \*/   
19 #define TCPOPT_EXP 254 /\* Experimental \*/   
20 /\* Magic number to be after the option value for sharing TCP   
21 \* experimental options. See draft-ietf-tcpm-experimental-options-00.txt   
22 \*/   
23 #define TCPOPT_FASTOPEN_MAGIC OxF989   
24   
25 \*/   
26 \* TCP option lengths   
27 \*/   
28   
29 #define TCPOLEN_MSS 4   
30 #define TCPOLEN WINDOW 3   
31 #define TCPOLEN_SACK_PERM 2   
32 #define TCPOLEN_TIMESTAMP 10   
33 #define TCPOLEN_MD5SIG 18   
34 #define TCPOLEN_FASTOPEN_BASE 2   
35 #define TCPOLEN_EXP_FASTOPEN_BASE 4 
```

# 8.6.1 TCP Options Handle

8.6.1.1 tcp_parse_fastopen_option   
/*   
Location:   
net/ipv4/tcp_input.c   
Function:   
处理Fastopen   
Parameter:   
len：长度   
cookie：用于fastopen   
syn：是否有syn   
foc：用于fastopen cookie   
exp_opt：是否是实验阶段。   
*/   
static void tcp_scan_fastopen_option(int len，const unsigned char \*cookie, bool syn，struct tcp_fastopen_cookie \*foc, bool exp_opt)   
{ /\*Valid only in SYN or SYN-ACK with an even length. \*/ if(!foc||!syn||len $<  0$ ||（len&1)) return; if(len $\coloneqq$ TCP_FASTOPEN_COOKIE_MIN&& len $<   =$ TCP_FASTOPEN_COOKIE_MAX) memcpy(foc->val，cookie，len); else if (len！ $= 0$ ） len $= -1$ . foc->len $=$ len; foc->exp $=$ exp_opt;   
}

调用位置:

# 8.6.1.2 tcp_parse_options

关于 TCP Options 的格式，如果不清楚的话，可以参考1.3.1.2中的介绍。

```txt
/* Location: net/ipv4/tcp_input.c
Function:
    Look for tcp options. Normally only called on SYN and SYNACK packets.
    But, this can also be called on packets in the established flow when the fast version below fails.
Parameter:
    skb: 缓存块 
```

```c
opt_rx: TCP 的选项部分
eatab: 标志是否建立。
foc: 表明是否是快速打开。
*/
void tcp_parser_options(const struct sk BUFF *skb, struct tcp_options_received *opt_rx, int estab, struct tcp_fastopen_cookie *foc)
{
const unsigned char *ptr;
const struct tcphdr *th = tcp hdr(skb);
/* 选项部分长度 */
int length = (th->doff * 4) - sizeof(struct tcphdr);
/* 得到选项的首部 */
ptr = (const unsigned char *) (th + 1);
/* 标记没有看到时间戳 */
opt_rx->saw_tstamp = 0;
while (length > 0) {
/* 选项的类型 */
int opcode = *ptr++;
/* 选项长度的大小 */
int opacity;
switch (opcode) {
//End of options
case TCPOPT_EOL:
return;
//Padding
case TCPOPT_NOP: /* Ref: RFC 793 section 3.1 */
length--;
continue;
default:
/* 得到选项的长度，选项长度不可能小于 2，
具体参见上面的宏定义说明。
*/
opacity = *ptr++;
if (opsize < 2) /* "silly options" */
return;
/*也不可能大于 length*/
if (opsize > length)
return; /* don't parse partial options */
switch (opcode) {
/* 最大报文段长度，只用于最初的协商阶段 */
case TCPOPT_MSS:
if (opsize == TCPOLEN_MSS && th->syn && !estab) {
/* 取最大段长度，如果为 0，那怎么办呢？这里似乎并没有处理 */
u16 in_mss = get_unaligned_be16(pk);
if (in_mss) {
/* 将最大段长度设置为选项中和用户自身的较小值。
*/
if (opt_rx->user_mss &&
opt_rx->user_mss < in_mss)
in_mss = opt_rx->user_mss;
opt_rx->mss_clamp = in_mss;
}
}
```

/* 窗口扩大因子选项，也只能出现在 SYN 且未建立的情况下，并且支持该选项 */case TCPOPT_WINDOW:

if (opsize == TCPOLENWINDOW && th->syn &&!estab && sysct1 tcp_window_scaling){/\*取位移数\*/\_u8snd.wscale $=$ \*(_u8\*)ptr;/\*将表示包含窗口扩大因子的选项置位\*/opt_rx->wscale.ok = 1;/*大于14则警告，并设重新设置为14.\*/if (snd.wscale >14){net_info_ratelimited("%s:Illegal window scaling value%d>14 received\n",__func_,snd.wscale);snd.wscale $= 14$ ·}opt_rx->snd.wscale $=$ snd.wscale;break; $\text{一}$ 时间戳选项，满足条件为：在建立的时候或者未建立但支持时间戳选项\*/  
case TCPOPT_TIMESTAMP:if((opsize $=$ TCPOLEN_TIMESTAMP)&&((estab&&opt_rx->tstamp.ok)||(!estab&&sysct1.tcp_timestamps))){//看见时间戳\*/opt_rx->saw_tstamp $= 1$ /\*time stampvalue\*/opt_rx->rcv_tval $=$ get_unaligned_be32(pk);/\*time stamp echo reply\*/opt_rx->rcv_tsecr $=$ get_unaligned_be32(pk + 4);1break; $\text{一}$ SACK Permit，只能在 syn并且未建立的时候使用\*/  
case TCPOPT_SACK_PER:if(opsize $=$ TCPOLEN_SACK_PERM&&th->syn&&！estab&&sysct1.tcp_sack){//标记看见opt_rx->sack.ok $=$ TCP_SACK_SEEN;\*/将与SACK有关的字段清零dsack,eff_sacks,num_sacks\*/tcp_sack_reset(opt_rx);1break; $\text{一}$ 表明有SACK\*/  
case TCPOPT_SACK:\*\*TCPOLEN_SACK_BASE为2,TCPOLEN_SACK_PERBLOCK为8因此，if语句的意思是，该选项包含kind、length以及一个左右边界值对，且扣除kind和length字段之后，nnegbei左右边界值对的大小整除，sack.ok为1表示允许SACK.\*/if((opsize $=$ (TCPOLEN_SACK_BASE+TCPOLEN_SACK_PERBLOCK))&&!((opsize-TCPOLEN_SACK_BASE)%TCPOLEN_SACK_PERBLOCK)&&opt_rx->sack.ok）{//将sacked的值指向这些左右边界值对的起始处\*/TCP_SKB_CB(skb)->sacked $=$ (ptr -2)- (unsigned char \*)th;1break;

```c
ifdef CONFIG_TCP_MD5SIG
case TCPOPT_MD5SIG:
/* The MD5 Hash has already been
* checked (see tcp_v{4,6}_do_rcv());
*/
break;
#endif
/* 快打开 */
case TCPOPT_FASTOPEN:
tcp.parse_fastopen_option(
    opacity - TCPOLEN_FASTOPEN_BASE,
    ptr, th->syn, foc, false);
break;
/* 实验阶段的 Fast Open, 现在基本上已经成形了。*/
case TCPOPT_EXP:
/* Fast Open option shares code 254 using a
* 16 bits magic number.
*/
if (opacity >= TCPOLEN_EXP_FASTOPEN_BASE && get_unaligned_be16(ptr) == TCPOPT_FASTOPEN_MAGIC)
tcpparse_fastopen_option(opsize -
    TCPOLEN_EXP_FASTOPEN_BASE,
    ptr + 2, th->syn, foc, true);
break;
}
/* 进入下一个循环, 处理下一个选项 */
ptr += opacity-2;
length -= opacity; 
```

# 调用位置:

1 5.1.3.3的 tcp_rcv_synsent_state_process 中调用。

# 8.6.1.3 tcp_clear_options

```c
/* Location: include/linux/tcp.h Function: 主要是使时间戳标记，SACK，窗口扩大选项设置为0。 */
static inline void tcp_clear_options(struct tcp_options_received *rx_opt) {
    rx_opt->tstamp.ok = rx_opt->sack.ok = 0;
    rx_opt->wscale.ok = rx_opt->snd_wscale = 0;
} 
```

1 的 tcp_conn_request 中调用。

# 8.6.1.4 tcp_syn_option

该函数用户构建带 SYN 选项的 TCP 包。当然，这里只是计算出 TCP 的选项字段需要多大空间，并不是真正地将选项构建成了 TCP 标准中所规定的格式。

```c
/* Location: net/ipv4/tcp_output.c */
* Parameter:
* sk: 套接字
* skb: 数据报
* opts: 用于存放 TCP 选项的指针
* md5: MD5 码（如果开启了相关选项）
*/
static unsigned int tcp_syn_options(struct sock *sk, struct sk BUFF *skb, struct tcp_out_options *opts, struct tcp_md5sig_key **md5)
{
struct tcp SOCK *tp = tcp_sk(sk);
unsigned int remaining = MAXneau_OPTION_SPACE;
struct tcp_fastopen_request *fastopen = tp->fastopen_req;
/* remaining 代表剩余了多少空间。根据 RFC793, TCP 选项的空间是有限的。
* 在后面的代码中，每增加一个选项就会相应地减少 remaining。
*/
#ifdef CONFIG.tcp_MD5SIG
*md5 = tp->afspecific->md5.lookup(sk, sk);
if (*md5) {
    options->options |= OPTION_MD5;
    remaining = TCPOLEN_MD5SIG_ALIGNED;
}
#else
*md5 = NULL;
#endif
/* We always get an MSS option. The option bytes which will be seen in * normal data packets should timestamps be used, must be in the MSS */
* advertised. But we subtract them from tp->mss_cache so that
* calculations in tcp_sendmsg are simpler etc. So account for this
* fact here if necessary. If we don't do this correctly, as a
* receiver we won't recognize data packets as being full sized when we
* should, and thus we won't abide by the delayed ACK rules correctly.
* SACKs don't matter, we never delay an ACK when we have any of those
* going out. */
options->mss = tcp_advertise_mss(sk);
remaining = TCPOLEN_MSS_ALIGNED;
if (likely(sysct1 tcp_timestamps && !*md5)) {
    options->options |= OPTION_TS;
    options->tsval = tcp_skb_timestamp(skb) + tp->toffset;
    options->tsecr = tp->rx_opt.tsrecent;
    remaining = TCPOLEN_TSTAMP_ALIGNED;
}
if (likely(sysct1 tcp_window_scaling)) {
    options->ws = tp->rx_opt.rcv_wscale;
    options->options |= OPTION_WSCALE;
    remaining = TCPOLEN_WSCALE_ALIGNED;
} 
```

54 options->options |= OPTION_SACK_ADVERTISE; if (unlikely(!(OPTION_TS & opts->options))) remaining -- = TCPOLEN_SACKPERM_ALIGNED;   
57 }   
58   
59 if (fastopen && fastopen->cookie.len >= 0) { u32 need = fastopen->cookie.len; need += fastopen->cookie.exp ? TCPOLEN_EXP_FASTOPEN_BASE : TCPOLEN_FASTOPEN_BASE; need $=$ (need + 3) & \~3U; /* Align to 32 bits */ /* 到这里会产生一个有趣的现象，remaining 有可能不够用。 \* 可以看到，如果选项中的剩余空间不够用了，那么就放弃启用 Fast Open。   
63 \*/ if (remaining >= need) { options->options |= OPTION_FAST_OPEN_cookie; options->fastopen_cookie = &fastopen->cookie; remaining -- = need; tp->syn_fastopen = 1; tp->syn_fastopen_exp = fastopen->cookie.exp ? 1 : 0;   
74 }   
75 }   
76   
77 return MAX.tcp_OPTION_SPACE - remaining;   
78 }

# 8.6.1.5 tcp_established_options

这个函数用于计算连接已经建立的状态下，TCP 包的头部选项所占用的空间。通过和 tcp_syn_options函数的对比我们可以直观地看到，大部分 TCP 选项都是在发起连接的时候放在 TCP 包里的。真正开始传输后，TCP 包所需携带的选项信息就少了很多。

static unsigned int tcp_established_options(struct sock \*sk, struct sk BUFF \*skb, struct tcp_out_options \*opts, struct tcp_md5sig_key \*\*md5)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); unsigned int size $= 0$ unsigned int eff_sacks; opts->options $= 0$ #ifdef CONFIG_TCP_MD5SIG \*md5 $=$ tp->afspecific->md5.lookup(sk, sk); if (unlikely(\*md5)){ options->options |= OPTION_MD5; size $+ =$ TCPOLEN_MD5SIG_ALIGNED; } #else \*md5 $=$ NULL; #endif if (likely(tp->rx_opt.tstamp.ok)){ options->options |= OPTION_TS; options->tsval $=$ skb?tcp_skb_timestamp(sk)+tp->toffset:0; optse->tsecr $=$ tp->rx_opt.tsrecent;

size $+ =$ TCPOLEN_TSTAMPAligned;   
}   
eff_sacks $\equiv$ tp->rx_opt.num_sacks $^+$ tp->rx_opt.dsack; if (unlikely(eff_sacks)){ const unsigned int remaining $\equiv$ MAX.tcp_OPTION_SPACE - size; opts->num_sack_blocks $=$ min_t(unsigned int, eff_sacks, (remaining-TCPOLEN_SACK_BASEAligned)/ TCPOLEN_SACK_PERBLOCK); size $+ =$ TCPOLEN_SACK_BASEAligned + opt-s>num_sack_blocks \* TCPOLEN_SACK_PERBLOCK;   
}   
return size;   
}

调用位置:

1 的 tcp_conn_request 中调用。

# 8.7 TCP PAWS

# 8.7.1 TCP PAWS Flags

```c
1 /\* Location   
2   
3 include/net/tcp.h   
4 \*/   
5   
6 #define TCP_PAWS_24DAYS (60 \* 60 \* 24 \* 24)   
7 #define TCP_PAWS_MSL 60 /\* Per-host timestamps are invalidated * after this time. It should be equal \* (or greater than) TCP_TIMEWAIT_LEN \* to provide reliability equal to one \* provided by timewait state.   
9   
10   
11   
12   
13 #define TCP_PAWS WINDOW 1 /\* Replay window for per-host \* timestamps. It must be less than \* minimal timewait lifetime.   
14   
15   
16 
```

# 8.7.2 tcp_paws_check

```txt
1 /*  
2 Location:  
3 include/net/tcp.h  
4 5  
6 Function:  
7 8 序号绕回检测。  
9  
10 Parameter:  
11 rx_opt: 接收到的 tcp 选项。  
12
```

```c
paws_win:  
\*/  
static inline bool tcp_paws_check(const struct tcp_options_received *rx_opt, int paws_win)  
{  
    if ((s32)(rx_opt->tsrecent - rx_opt->rcv_tsval) <= paws_win) return true;  
    /* 判断从存储 tsrecent 到现在是否有 24 天的时间，有的话，就可能失效 */ if (unlikely(getSeconds() >= rx_opt->tsrecent_stamp + TCP_PAWS_24DAYS)) return true;  
    /* Some OSes send SYN and SYNACK messages with tsval=0 tsecr=0,  
    * then following tcp messages have valid values. Ignore O value,  
    * or else 'negative' tsval might forbid us to accept their packets.  
    /*  
    if (!rx_opt->tsrecent) return true;  
    return false;  
} 
```

# 8.7.3 tcp_paws_reject

```c
/*   
Location: include/net tcp.h   
Function: 判断是否要拒绝   
Parameter: rx_opt：接收到的tcp选项。 rst: static inline bool tcp_paws_reject(const struct tcp_options_received \*rx_opt, int rst) { /*检测是否通过PAWS检测*/ if (tcp_paws_check(rx_opt，0)) return false; /* RST segments are not recommended to carry timestamp, and, if they do, it is recommended to ignore PAWS because "their cleanup function should take precedence over timestamps." Certainly, it is mistake. It is necessary to understand the reasons of this constraint to relax it: if peer reboots, clock may go out-of-sync and half-open connections will not be reset. Actually, the problem would be not existing if all the implementations followed draft about maintaining clock via reboots. Linux-2.2 DOES NOT! However, we can relax time bounds for RST segments to MSL. / /\* 如果收到了rst，并且还没有超时，就不拒绝 
```

```cpp
if (rst && getSeconds() >= rx_opt->tsrecent_stamp + TCP_PAWS_MSL) return false; return true; 
```

# 调用位置:

1 的 tcp_timewait_state_process 中调用。

# 8.8 TCP TimeStamp

8.8.1 tcp_store_ts_recent   
```c
/* 
Location 
net/ipv4/tcp_input.c 
Function: 
    替换时间戳 tsrecent 
Parameter: 
    tp:TCP sock 
*/ 
static void tcpstore(tsrecent(struct tcp sock *tp) 
{
    /* 替换时间戳为刚刚接收到的时间戳值 */
    tp->rx_opt.tsrecent = tp->rx_opt.rcv_tsval;
    /* 记录下替换时间戳的时间 */
    tp->rx_opt.tsrecent_stamp = getSeconds();
} 
```

8.8.2 tcp_replace_ts_recent   
```c
/* 
Location 
net/iju4/tcp_input.c 
Function: 
    考虑是否要替换时间戳 tsrecent 
Parameter: 
    tp:TCP sock 
seq: 
*/ 
static void tcp_replace(tsrecent(struct tcp sock *tp, u32 seq) 
{
    if (tp->rx_opt.saw_tstamp && !after(seq, tp->rcv_wup)) { /* PAWS bug workaround wrt. ACK frames, the PAWS discard 
        * extra check below makes sure this can only happen 
```

```c
\*for pure ACK frames.-DaveM \* \*Not only,also it occurs for expired timestamps. \*/ /\*PAWS检测成功\*/ if（tcp_paws_check(&tp->rx_opt，0)) tcpstore tsrecent(tp); }   
1 
```

# 调用位置:

1 的 tcp_ack 中调用。

# 8.9 TCP ACK

ACK 机制与数据传输紧密关联可以体现在以下几个方面。

1 通过 ACK 可以使得发送方可以很容易地计算出数据往返时间。  
2 因为 ACK 段中携带接收方的通告窗口，因此，此时接收方能接收的数据上限为通告的接收窗口大小，接收到 ACK 后，在正常情况下会引发下一个段的发送。  
3 通过拥塞窗口的调节，TCP 可以进行限制性地传输，以免网络拥塞。从接收 ACK可以判断和评估当前网络的状况，从而进一步调整拥塞窗口。

# 8.9.1 ACK Check

8.9.1.1 tcp_is_sack & tcp_is_reno & tcp_is_fack   
```c
/*   
Location:   
include/net/tcp.h   
Description: These functions determine how the current flow behaves in respect of SACK handling. SACK is negotiated with the peer, and therefore it can vary between different flows. tcp_is_sack - SACK enabled tcp_is_reno - No SACK tcp_is_fack - FACK enabled, implies SACK enabled */ static inline int tcp_is_sack(const struct tcp_sock \*tp) { return tp->rx_opt.sack.ok; } static inline bool tcp_is_reno(const struct tcp_sock \*tp) { return !tcp_is_sack(tp); } static inline bool tcp_is_fack(const struct tcp_sock \*tp) 
```

```c
26 { returntp->rx_opt.sack.ok&TCP_FACK_ENABLED;   
27   
28 } 
```

# 调用位置:

1 6.1.1.6的 tcp_enter_loss 中调用。

8.9.1.2 tcp_ack_is_dubious   
1 /\* Location:   
2   
3 net/iju4/tcp_input.c   
4   
5   
6 Function:   
7   
8 判断一个ACK是否可疑。   
9   
10 Parameter: sk：传输控制块 flag:FLAG标志位 \*/ static inline bool tcp ACK_is_dubious(const struct sock \*sk，const int flag) { return！(flag&FLAG_NOT_DUP)||（flag&FLAG_CA_ALERT）|！ inlet_csk(sk)->icsk_ca_state $! =$ TCP_CA_Open;   
19 }

什么样的 ACK 算是可疑的呢? 如下:

非 FLAG_NOT_DUP 通过查看相关的宏定义我们知道 FLAG_NOT_DUP 为 (FLAG_DATA|FLAG_WIN_UPDATE|FL而 FLAG_ACKED 又为 (FLAG_DATA_ACKED|FLAG_SYN_ACKED), 故而即其所包含的种类有接收的 ACK 段是 (1) 负荷数据携带的;(2) 更新窗口的;(3) 确认新数据的;(4) 确认 SYN 段的。如果上述四种都不属于那是可疑的了。

FLAG_CA_ALERT 通过查看相关的宏定义我们知道 FLAG_CA_ALERT 为 (FLAG_DATA_SACKED|FLAG_ECE)，如果被发现时确认新数据的 ACK 或者在 ACK 中存在 ECE 标志，即收到显式拥塞通知，也认为是可疑的。似乎和上面的有矛盾，

非 Open 即当前拥塞状态不为 Open。

# 调用位置:

1 的 tcp_ack 中调用。

# 8.9.1.3 tcp_check_sack_reneging

如果接收到的确认 ACK 指向之前记录的 SACK，这说明之前记录的 SACK 并没有反映接收方的真实状态。接收路径上很有可能已经有拥塞发生或者接收主机正在经历严重的拥塞甚至处理出现了 BUG，因为按照正常的逻辑流程，接收的 ACK 不应该指向已

记录的 SACK，而应该指向 SACK 后面未接收的地方。通常情况下，此时接收方已经删除了保存到失序队列中的段。

为了避免短暂奇怪的看起来像是违约的 SACK 导致更大量的重传，我们给接收者一些时间, 即 $m a x ( R T T / 2 , 1 0 m s )$ 以便于让他可以给我们更多的 ACK，从而可以使得SACK 的记分板变得正常一点。如果这个表面上的违约一直持续到重传时间结束，我们就把 SACK 的记分板清除掉。

/*   
Location:   
net/ipv4/tcp_input.c   
Function:   
If ACK arrived pointing to a remembered SACK, it means that our   
remembered SACKs do not reflect real state of receiver i.e.   
receiver_host_ is heavily congested (or buggy).   
To avoid big spurious (假的) retransmission bursts(爆发） due to transient SACK   
scoreboard oddities that look like reneging, we give the receiver a   
little time (max(RTT/2, 10ms)) to send us some more ACKs that will   
restore sanity to the SACK scoreboard. If the apparent reneging   
persists until this RTO then we'll clear the SACK scoreboard.   
Paramater:   
sk:传输控制块。   
flag：相关标志位   
\*/   
static bool tcp_check_sack_reneging(struct sock \*sk，int flag)   
{ //如果确认接收方违约了。 if (flag & FLAG_SACK_RENEGING){ struct tcp_sock \*tp $=$ tcp_sk(sk); //计算超时重传时间 unsigned long delay $=$ max(usecs_to_jiffies(tp->srtt_us >> 4), msecs_to_jiffies(10)); //更新超时重传定时器 inlet_csk_reset_xmit_timer(sk, ICSK_TIME_RETRANS, delay,TCP_RTO_MAX); return true; } return false;

# 调用位置:

1 中的 tcp_fastretrans_alert 调用。

# 8.9.1.4 tcp_limit_reno_sacked

```txt
1 /*   
2 Location:   
3   
4 net/iju4/tcp_input.c   
5 
```

Function: Limits sacked_out so that sum with lost_out isn't ever larger than packets_out. Returns false if sacked_out adjustment wasn't necessary.   
Parameter: tp:????   
\*/ static bool tcp_limit_reno_sacked(struct tcp SOCK \*tp) { u32 holes; //记录丢失的包，？？？很奇怪，为什么没能直接利用 lost_out 呢？ //既然由重复的 ACK，那么必然是之前传输的段有丢失的了，所以至少为 1。 holes $=$ max(tp->lost_out,1U); //但是又不能大于所有的发出去的。 holes $=$ min(holes,tp->packets_out); if((tp->sacked_out + holes) $>$ tp->packets_out){ tp->sacked_out $=$ tp->packets_out - holes; return true; } return false;   
}

该函数用于检查 sacked_out 是否过多，过多则限制，且返回 true，说明出现 re-ordering 了。

那么我们怎么判断是否有 reordering 呢？我们知道 dupack 可能由 lost 引起，也有可能由 reorder 引起，那么如果 sacked_out $+$ lost_out $>$ packets_out，则说明 sacked_out偏大了，因为它错误的把由 reorder 引起的 dupack 当客户端的 sack 了。

8.9.1.5 tcp_check_reno_reordering   
1 /\* Location:   
2 net/ipo4/tcp_input.c   
5 Function:   
7 If we receive more dupacks than we expected counting segments   
9 in assumption of absent reordering, interpret this as reordering.   
10 The only another reason could be bug in receiver TCP.   
11 Parameter:   
13 sk:传输控制块 addend:?   
15 static void tcp_check_reno_reordering(struct sock \*sk，const int addend) { struct tcp_sock \*tp $=$ tcp_sk(sk); //判断是否有reordering if (tcp_limit_reno_sacked(tp)) tcp_update_reordering(sk,tp->packets_out +addend,0);//????这个函数到底时用于干什么呢？   
20

# 8.9.2 ACK Update

8.9.2.1 tcp_snd_una_update   
1 /\* Location:   
2   
3 net/ipv4/tcp_input.c   
4 Description:   
5   
6 If we update tp->snd_una, also update tp->bytes_acked \*/   
7   
8 Parameter: tp:tcp sock ack:确认号   
13 static void tcp_snd_una_update(struct tcp SOCK \*tp，u32 ack)   
15 { /\*得到已经确认的数量\*/ u32 delta $=$ ack-tp->snd_una; /\*记录已经得到确认的字节数量\*/ u64.stats_update_begin(&tp->syncp); tp->bytes_acked += delta; u64 stats_update_end(&tp->syncp); /\*更新una\*/ tp->snd_una $=$ ack;   
25 }

调用位置:

1 的 tcp_ack 中调用。

8.9.2.2 tcp_reset_reno_sack  
```c
/* Location: net/ipv4/tcp_input.c
Function:
    将 sacked_out 字段置为 0
Parameter:
    tp: ??? */
static inline void tcp_reset_reno_sack(struct tcp_sock *tp)
{
    tp->sacked_out = 0;
} 
```

调用位置:

1 6.1.1.6的 tcp_enter_loss 中调用。

8.9.2.3 tcp_add_reno_sack   
```c
/* Location: net/iju4/tcp_input.c
Location: Emulate(仿真) SACKs for SACKless connection: account for a new dupack.
Parameter:
sk: 传输控制块 */
static void tcp_add_reno_sack(struct sock *sk)
{
    struct tcp SOCK *tp = tcp_sk(sk);
    tp->sacked_out++;
    //增加 sacked 的包数.
    tcp_check_reno_reordering(sk, 0); // 检查是否有 reordering
    tcp_check_reno_reordering(sk, 0); // 检查是否有 reordering
    tcp_check_reno_reordering(sk, 0); // 检查是否有 reordering
} 
```

调用位置:

1 8.10.1.2中的 tcp_fastretrans_alert 调用。

# 8.10 TCP Window

# 8.10.1 Window Compute

# 8.10.1.1 tcp_left_out

该函数用于计算已经发出去的 TCP 段 (离开主机) 中一共有多少个 tcp 段还未得到确认。

```c
static inline unsigned int tcp_left_out(const struct tcp_sock *tp) {
    return tp->sacked_out + tp->lost_out;
} 
```

# 8.10.1.2 tcp_verify_left_out

该函数的功能主要是判断 left_out 是否大于 packets_out, 当然，这是不可能的，因为前者是已经发送离开主机的未被确认的段数，而后者是已经离开发送队列 (不一定离开主机)但未确认的段数。故而，这里有一个WARN_ON，以便输出相应的警告信息。

```txt
1 /*   
2 Location:   
3 include/net/tcp.h   
4 function:   
5   
6   
7 
```

```c
Use define here intentionally(有意地）to getWARN_ON location showm at the caller   
Parameter:   
\*/   
#define tcp_checkleft_out(tp)WARN_ON(tp_left_out(tp)>tp->packets_out) 
```

# 调用位置:

1 6.1.1.6中的 tcp_enter_loss 调用。  
2 中的 tcp_fastretrans_alert 调用。

# 8.10.2 Window Update

# 8.10.2.1 tcp_may_update_window

```c
/* 
Location: 
net/iju4/tcp_input.c 
Function: 
Check that window update is acceptable. 
The function assumes that snd_una<=ack<=snd_next. 
Parameter: 
tp:tcp sock 
ack:起始序列号 
ack_seq:确认序列号 
nwin:窗口大小 
*/ 
static inline bool tcp可能會_update_window(const struct tcp_sock *tp, const u32 ack, const u32 ack_seq, const u32 nwin) 
{ /* 
三种情况成立一种 
1. 起始序列号大于una 
2. 确认号大于窗口的左端 
3. 确认号等于窗口的左端，同时，窗口大于本身的发送窗口 
*/ 
return after(ack, tp->snd_una) || 
after(ack_seq, tp->snd wl1) || 
(ack_seq == tp->snd wl1 && nwin > tp->snd_wnd); } 
```

# 8.10.2.2 tcp_ack_update_window

```txt
1 /*   
2 Location:   
3   
4 net/ipv4/tcp_input.c   
5   
6 Description: 
```

Update our send window. Window update algorithm, described in RFC793/RFC1122 (used in linux-2.2 and in FreeBSD.NetBSD's one is even worse.) is wrong.   
Parameter: sk: 传输控制块 skb: 缓存块 ack: ack_seq: static int tcp ACK_UPDATE_window(struct sock \*sk, const struct sk BUFF \*skb, u32 ack, u32 ack_seq) struct tcp_sock \*tp = tcp_sk(sk); int flag $= 0$ /\*得到接收窗口大小\*/ u32 nwin $=$ ntohs.tcp hdr(skb)->window); /\*判断是否有 syn，没有的话，窗口扩大\*/ if (likely(!tcp hdr(skb)->syn)) nwin $<   <   =$ tp->rx_opt.snd_wscale; if (tcp may update window(tp,ack,ack_seq,nwin)){ /\*表明要更新\*/ flag |= FLAG WIN UPDATE; /\*tp->snd wl1 $=$ seq\*/ tcp_update wl(tp,ack_seq); /\*更新本端发送窗口\*/ if(tp->snd_wnd != nwin){ tp->snd_wnd = nwin; /\*Note, it is the only place, where \*fast path is recovered for sending TCP. /\*取消头部预测\*/ tp->pred_flags $= 0$ /\*快速路径检测\*/ tcp_fast_path_check(sk); /\*\*\*/ if (tcp_send_head(sk)) tcp slows_start_after_idle_check(sk); /\*如果接收窗口大于之前的最大的接收窗口，更新，重新计算MSS\*/ if(nwin $>$ tp->max_window){ tp->max_window = nwin; tcp sync mss(sk,inet_csk(sk)>icsk_pmtu_cookie); } } } \*/ \* 更新una\*/ tcp_snd_una_update(tp,ack); return flag;

# 调用位置:

1 的 tcp_ack 中调用。

# 8.11 TCP Urgent Data

# 8.11.1 相应标识

```c
1 /*   
2 Location:   
3 include/net/tcp.h   
4   
5 \*/   
6 #define TCP_URG_VALID 0x0100   
7 #define TCP_URG_NOTYET 0x0200   
8 #define TCP_URG_READ 0x0400 
```

# 8.11.2 TCP Urgent Check

# 8.11.2.1 tcp_check_urg

/*   
Location:   
net/iju4/tcp_input.c   
Function:   
This routine is only called when we have urgent data signaled. Its the 'slow' part of tcp urg. It could be moved inline now as tcp urg is only called from one place. We handle URGent data wrong. We have to - as BSD still doesn't use the correction from RFC961. For 1003.1g we should support a new option TCP_STDURG to permit either form (or just set the sysctl tcp_stdurg).   
Parameter:   
sk:传输控制块 th:tcp 头部   
*/   
static void tcp_check_urg(struct sock \*sk, const struct tcphdr \*th) { struct tcp SOCK \*tp = tcp_sk(sk); /\* 字节序转换 \*/ u32 ptr $=$ ntohs(th->ulgptr); /\* 计算带外数据的位置   
注意： 目前仍有许多关于紧急指针是指向带外数据的最后一个字节还是指向 带外数据最后一个字节的下一个字节。Host Requirements RFC 确定 指向最后一个字节是正确的。然而，大多数的实现，包括BSD 的实现， 继续使用错误的方法来实现。因此如果启用兼容错误选项，就需要将 指针前移。 \*/ if (ptr && !sysctl tcp_stdurg) ptr--; ptr += ntohl(th->seq);   
/\* Ignore urgent data that we've already seen and read.

如果我们已经读取过或者接收过相应的数据，就忽略。

```c
\*/   
if (after(tp->copied_seq, ptr)) return;   
/\* Do not replay urg ptr. \* NOTE: interesting situation not covered by specs. \* Misbehaving sender may send urg ptr, pointing to segment, \* which we already have in ofo queue. We are not able to fetch \* such data and will stay in TCP_URG_NOTYET until will be eaten \* by recumsg(). Seems, we are not obliged to handle such wicked \* situations. But it is worth to think about possibility of some \* DoSes using some hypothetical application level deadlock.   
\*/   
if (before(ptr, tp->rcv_nxt)) return;   
/\* Do we already have a newer (or duplicate) urgent pointer? 如果还有带外数据用户进程尚未读取，并且当前带外数据早于该 尚未读取的带外数据，则丢弃该带外数据。   
\*/   
if (tp->urg_data &&!after(ptr,tp->urg_seq)) return;   
/\* Tell the world about our new urgent pointer. 唤醒异步等待该套接口的进程，并告诉它们有新的带外数据到达。   
\*/   
sk_send_sigurg(sk);   
/\* We may be adding urgent data when the last byte read was \* urgent. To do this requires some care. We cannot just ignore \*tp->copied_seq since we would read the last urgent byte again \* as data, nor can we alter copied_seq until this data arrives \* or we break the semantics of SIOCATMARK (and thus sockatmark(); \* NOTE. Double Dutch. Rendering to plain English: author of comm \* above did something sort of send("A", MSG_OOB); send("B", MSG_ \* and expect that both A and B disappear from stream. This is _w\* Though this happens in BSD with high probability, this is occa\* Any application relying on this is buggy. Note also, that fix \* only in this artificial test. Insert some normal data between \* decline of BSD again. Verdict: it is better to remove to trap \* buggy users.   
\*/   
\*/   
在当前接收到的带外数据的段的序号正是接下来需要复制到进程空间 的段的序号，且下一个接收段的序号与需要复制到进程空间的序号相等 的情况下，需要检测接收队列中的第一个段是否有效。因为当前处理的 是带外数据，因此需要递增 copied_seq。接着检测接收队列中第一个段 是否已经国企，如果是，则释放。   
\*/   
if (tp->urg_seq == tp->copied_seq && tp->urgent_data && !sock_flag(sk, SOCK_URGINLINE) && tp->copied_seq != tp->rcv_nstruct sk BUFF *skb = skb Peek(&sk->sk_receive_queue); tp->copied_seq++; if (skb && !before(tp->copied_seq, TCP_SKB_CB(sk)->end_seq)) __skb_unlink(sk, &sk->sk_receive_queue); __kfree_skb(sk); 
```

}   
} /\* 设置带外数据序号以及标记，以便后续根据标志作相应处理。 \*/ tp->urg_data $=$ TCP_URG_NOTYET; tp->urg_seq $=$ ptr; /\* Disable header prediction. 由于接收到了带外数据，禁止下次首部预测。 \*/ tp->pred_flags $= 0$ ·   
}

# 8.11.3 TCP Urgent Deal

# 8.11.3.1 tcp_urg

/*   
Location:   
net/ipv4/tcp_input.c   
Function:   
This is the fast' part of urgent handling.   
Parameter:   
sk:传输控制块   
skb：缓存块   
th:TCP首部   
*/   
static void tcp urg(struct sock \*sk,struct sk BUFF \*skb，const struct tcphdr \*th)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); /\* Check if we get a new urgent pointer - normally not. 检查带外数据偏移量是否正常。 \*/ if (th->urg) tcp_check_urg(sk，th); /\* Do we wait for any urgent data? - normally not... \*/ if(tp->urg_data $= =$ TCP_URG_NOTYET）{ u32 ptr $=$ tp->urg_seq-ntohl(th->seq)+（th->doff\*4)- th->syn; /\*Is the urgent pointer pointing into this packet? \*/ if(ptr<skb->len){ u8 tmp; if(sk_copy_bits(skb，ptr,&tmp，1)) BUG(); /\* 设置TCP_URG_VALID标志表示用户进程可以读取，然后通知 用户进程读取。 \*/ tp->urg_data $=$ TCP_URG_VALID|tmp;

```c
if(!sock_flag(sk,SOCK_DEAD))   
42 sk->sk_data_ready(sk);   
43 }   
44 1   
45 } 
```

# 调用位置:

1 5.1.3.2的 tcp_rcv_state_process 中调用。  
2 7.2.2.1的 tcp_rcv_state_process 中调用。

# 8.12 Congestion Control

# 8.12.1 COngestion Control Flag

# 8.12.1.1 sysctl_tcp_recovery

1 int sysctl tcp_recovery __readoretly $=$ TCP_RACK_LOST_RETRANS;

# 8.12.2 Congestion Control Window

# 8.12.2.1 tcp_init_cwnd_reduction

/*   
Location   
net/ipv4/tcp_input.c   
Function: The cund reduction in CwR and Recovery uses the PRR algorithm in RFC 6937. It computes the number of packets to send (sndcnt) based on packets newly delivered: 1) If the packets in flight is larger than ssthresh, PRR spreads the cund reductions across a full RTT. 2) Otherwise PRR uses packet conservation to send as much as delivered. But when the retransmits are acked without further losses, PRR slow starts cumd up to ssthresh to speed up the recovery.   
Parameter:   
sk:sock   
\*/   
static void tcp_init_cwnd_reduction(struct sock \*sk)   
{ struct tcp SOCK \*tp = tcp_sk(sk); /\*开始拥塞的时候下一个要发送的字节序号 \*/ tp->high_seq $=$ tp->snd_nxt; tp->tlp_high_seq $= 0$ . /\*自从上次调整拥塞窗口到目前为止接收到的总ACK段数\*/ tp->snd_cwnd_cnt $= 0$ /\*在进入Recovery状态时的拥塞窗口\*/ tp->prior_cwnd $=$ tp->snd_cwnd; /\*在恢复阶段给接收者新发送包的数量\*/ tp->prr_delivered $= 0$ /\*在恢复阶段一共发送的包的数量\*/

```c
tp->prr_out = 0;  
/* 根据拥塞算法，计算新阈值 */  
tp->snd_ssthresh = inet_csk(sk) ->icsk_ca ops->sssthresh(sk);  
/* 设置 TCP_ECN_queue_CWR 标志，标识由于收到显式拥塞通知而进入拥塞状态 */  
tcp_ecn_queue_cwr(tp);
```

# 调用位置:

1 6.1.1.4的 tcp_enter_cwr 中调用。

8.12.2.2 tcp_end_cwnd_reduction   
1 /\* Location:   
2   
3 net/ipv4/tcp_input.c   
4   
5   
6 Function:   
7   
8 结束拥塞窗口减小。   
9   
10 Parameter: sk：传输控制块。   
11 static inline void tcp_end_cwnd_reduction(struct sock \*sk) { struct tcp SOCK \*tp $=$ tcp_sk(sk); /\*Reset cwnd to ssthresh in CwR or Recovery (unless it's undone) \*/ if (inet_csk(sk)->icsk_ca_state $= =$ TCP_CA_CWR || (tp->undomarker && tp->snd_ssthresh < TCP_INFINITE_SSTHRESH)) { /\* 拥塞窗口恢复慢启动门限值 \*/ tp->snd_cwnd $=$ tp->snd_ssthresh; /\* 记录检验拥塞窗口的时间 \*/ tp->snd_cwnd_stamp $=$ tcp_time_stamp; } tcp_ca_event(sk, CA_EVENT_COMPLETE_CWR);   
27 1

# 调用位置:

1 8.10.1.2的 tcp_fastretrans_alert 中调用。

# 8.12.3 Congestion Control Window Undo

8.12.3.1 tcp_init_undo   
```txt
1 /*   
2 Location   
3   
4 net/ipv4/tcp_input.c   
5   
6 Function   
7   
8 撤销初始化 
```

```c
Parameter
tp: tcp sock
*/
static inline void tcp_init Undo(struct tcp_sock *tp)
{
/* 标记重传起始点 */
tp->undomarker = tp->snd_una;
/* Retransmission still in flight may cause DSACKs later. */
tp->undo_retrans = tp->retrans_out ? : -1;
} 
```

# 调用位置:

1 6.1.1.6的 tcp_enter_loss 中调用。

8.12.3.2 tcp_may_undo   
```txt
/* 
Location: 
net/iju4/tcp_input.c 
Function: 
检测能否撤销。 
Parameter: 
sk: 传输控制块。remain to do in the future ??? 
*/ 
static inline bool tcp可能會 Undo(const struct tcp_sock *tp) 
{ 
    /* 记录了重传起点 
    以及没有重传的段数 
    或者没有重传或者重传了之后还没有接收到对方发送的确认。 
    */
    return tp->undomarker && (!tp->undo_retrans || tcpPacket_delayed(tp));
```

8.12.3.3 tcp_undo_cwnd_reduction   
```txt
/* Location: net/ipv4/tcp_input.c
Function: 撤销窗口减小
Parameter: sk: 传输控制块。
unmark_loss: 标记段是否丢失 
```

static void tcp Undo_cwnd_reduction(struct sock \*sk, bool unmark_loss)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); /\*如果丢失了\*/ if (unmark_loss){ struct sk_buffer \*skb; tcp_for_write_queue(sk, sk) { if (skb $= =$ tcp_send_head(sk)) break; TCP_SKB_CB(sk)->sacked $\& =$ ~TCPCB_LOST; } tp->lost_out $= 0$ \*/ /\*清除所有重传标记\*/ tcp_clear_all_retrans_hints(tp); } /\*如果之前的慢启动门限值不为零\*/ if(tp->prior_ssthresh){ const struct inetzzaunction SOCK \*icsk $=$ INET_csk(sk); if(icsk->icsk_ca ops->undo_cwnd) tp->snd_cwnd $=$ icsk->icsk_ca ops->undo_cwnd(sk); else tp->snd_cwnd $=$ max(tp->snd_cwnd,tp->snd_ssthresh<<1); if(tp->prior_ssthresh $>$ tp->snd_ssthresh){ tp->snd_ssthresh $=$ tp->prior_ssthresh; tcp_ecn_withdraw_cwr(tp); } } else { tp->snd_cwnd $=$ max(tp->snd_cwnd,tp->snd_ssthresh); } tp->snd_cwnd_stamp $=$ tcp_time_stamp; tp->undomarker $= 0$ .

# 8.12.3.4 tcp_try_undo_partial

```c
/\*   
Location: net/ipu4/tcp_input.c   
Function: Undo during fast recovery after partial ACK.   
Parameter: sk:传输控制块。 acked：此次确认的段的数目 prior_unsacked: flag:   
\*/ static bool tcp_try_undo_partial(struct sock \*sk, const int acked, const int prior_unsacked,int flag)   
{ 
```

```c
struct tcp_sock *tp = tcp_sk(sk);
/* 有重传起点，并且包延迟 */
if (tp->undomarker && tcp_packet_delayed(tp)) {
    /* Plain luck! Hole if filled with delayed * packet, rather than with a retransmit. */
tcp_update_reordering(sk, tcp_fackets_out(tp) + acked, 1);
/* We are getting evidence that the reordering degree is higher * than we realized. If there are no retransmits out then we * can undo. Otherwise we clock out new packets but do not * mark more packets lost or retransmit more.
/* if (tp->retrans_out) {
    tcp_cwnd_reduction(sk, prior_unsacked, 0, flag);
    return true;
}
if (!tcp_any_retrans_done(sk)) {
    tp->retrans_stamp = 0;
DBGUNDO(sk, "partial recovery");
/* 撤销窗口减小 */
tcp Undo_cwnd Reduction(sk, true);
NET_INC_STATISTICS_BH(sock_net(sk), LINUX_MIB.tcpPARTIALUNDO);
tcp_try_keep_open(sk);
return true;
} return false;
} 
```

# 调用位置:

1 8.10.1.2的 tcp_fastretrans_alert 中调用。

# 8.12.3.5 tcp_try_undo_recovery

1 /\* Location:   
2   
3 net/ipv4/tcp_input.c   
4   
5 Function:   
6尝试从恢复状态撤销。   
7   
8 Parameter:   
9 static bool tcp_try_undo_recovery(struct sock \*sk) { struct tcp SOCK \*tp $=$ tcp_sk(sk); /\*检测能否撤销\*/ if (tcp_may_undo(tp)) { int mibidx;   
20   
21 $\ast$ Happy end! We did not retransmit anything

\*or our original transmission succeeded. \*/ DBGUNDO(sk,inet_csk(sk) $\rightharpoondown$ icsk_ca_state $= =$ TCP_CA_Loss ? "loss" : "retrans"); tcp Undo_cwnd_reduction(sk, false); if (inet_csk(sk) $\rightharpoonup$ icsk_ca_state $= =$ TCP_CA_Loss) mibidx $=$ LINUX_MIB_TCPLOSSUNDO; else mibidx $=$ LINUX_MIB_TCPFULLUNDO; NET_INC_STATSBH(sock_net(sk),mibidx); } /\*如果不支持SACK，则需要防止虚假的快重传\*/ if(tp->snd_una $= =$ tp->high_seq &&tcp_is_reno(tp)){ /*Hold old state until something \*above\* high_seq \*is ACKed.For Reno it is MUST to prevent false \*fast retransmits (RFC2582). SACK TCP is safe.\*/ tcp_moderate_cwnd(tp); if(!tcpany_retrans_done(sk)) tp->retrans_stamp $= 0$ return true; } /\*支持，回到OPen状态\*/ tcp_set_ca_state(sk,TCP_CA_Open); return false;

# 调用位置:

1 8.10.1.2的 tcp_fastretrans_alert 中调用。

8.12.3.6 tcp_rack_mark_lost   
```txt
/*   
Location:   
net/iju4/tcp_recovery.c   
Function:   
Marks a packet lost, if some packet sent later has been (s)acked. The underlying idea is similar to the traditional dupthresh and FACK but they look at different metrics:   
depthresh: 3 000 packets delivered (packet count)   
FACK: sequence delta to highest sacked sequence (sequence space)   
RACK: sent time delta to the latest delivered packet (time domain)   
The advantage of RACK is it applies to both original and retransmitted packet and therefore is robust against tail losses. Another advantage is being more resilient to reordering by simply allowing some "settling delay", instead of tweaking the dupthresh.   
The current version is only used after recovery starts but can be easily extended to detect the first loss.   
Parameter: 
```

int tcp_rack_mark Losing(struct sock \*sk)   
{ struct tcp_sock \*tp $=$ tcp_sk(sk); struct sk_buffer \*skb; u32 reo_wnd, prior_retrans $=$ tp->retrans_out; if (inet_csk(sk)->icsk_ca_state < TCP_CA_Recovery ||!tp->rack.advanced) return 0; /\*Reset the advanced flag to avoid unnecessary queue scanning \*/ tp->rack.advanced $= 0$ . /\*To be more reordering resilient, allow min_rtt/4 settling delay \* (lower-bounded to 1000uS). We use min_rtt instead of the smoothed \* RTT because reordering is often a path property and less related \* to queuing or delayed ACKs. \* \* TODO: measure and adapt to the observed reordering delay, and \* use a timer to retransmit like the delayed early retransmit. /reo_wnd $= 1000$ . if(tp-rack.reord &&tcp_min_rtt(tp)！ $= -0U$ ) reo_wnd $=$ max(tp_min_rtt(tp) $\ggg$ 2,reo_wnd); tcp_for_write_queue(skb,sk){ struct tcp_skb_cb \*scb $=$ TCP_SKB_CB(skb); if (skb $= =$ tcp_send_head(sk)) break; /\*Skip ones already (s)acked \*/ if(!after(scb->end_seq,tp->snd_una) || scb->sacked & TCPCB_SACKED_ACKED) continue; if (skb_mstamp_after(&tp->rack.mstamp,&skb->skb_mstamp)){ if (skb_mstamp_us_diff(&tp->rack.mstamp, &skb->skb_mstamp) <= reo_wnd) continue; /\*skb is lost if packet sent later is sacked \*/ tcp_skb_mark Losing_uncond_checkify(tp,skb); if (scb->sacked & TCPCB_SACKED_RETRANS){ scb->sacked $\& =$ ~TCPCB_SACKED_RETRANS; tp->retrans_out $- =$ tcp_skb_pcount(skb); NET_INC_STATSHBH(sock_net(sk), LINUX_MIB_TCPLOSTRETRANMIT); } } else if (!(scb->sacked & TCPCB_RETRANS)) { /\* Original data are sent sequentially so stop early \* b/c the rest are all sent after rack_sent \*/ break; } }

return prior_retrans - tp->retrans_out;   
}   
/* Record the most recently (re)sent time among the (s)acked packets */   
void tcp_rack Advance(struct tcp_sock \*tp, const struct skb_mstamp \*xmit_time,u8 sacked)   
{ if(tp->rack.mstamp.v64 && !skb_mstamp_after(xmit_time，&tp->rack.mstamp)) return;   
if(sacked & TCPCB_RETRANS){ struct skb_mstamp now; /\*If the sacked packet was retransmitted, it's ambiguous \* whether the retransmission or the original (or the prior \* retransmission) was sacked. \* \* If the original is lost, there is no ambiguity. Otherwise \* we assume the original can be delayed up to aRTT + min_rtt. \* the aRTT term is bounded by the fast recovery or timeout, \* so it's at least one RTT (i.e., retransmission is at least \* an RTT later). \*/ skb_mstamp_get(&now); if(skb_mstamp_us delta(&now,xmit_time)< tcp_min_rtt(tp)) return; }   
tp->rack.mstamp = \*xmit_time;   
tp->rack.advanced $= 1$ .

8.12.3.7 tcp_try_undo_dsack   
/\*   
Location:   
net/iju4/tcp_input.c   
Function: Try to undo cumd reduction, because D-SACKs acked all retransmitted data   
Parameter: sk:传输控制块   
\*/   
static bool tcp_try_undo_dsack(struct sock \*sk)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); if(tp->undomarker &&！tp->undo_retrans){ DBGUNDO(sk，"D-SACK"); tcp_undo_cwnd_reduction(sk,false); NET_INC_STATSBH(sock_net(sk)，LINUX_MIB_TCPDSACKUNDO); return true;   
}

```txt
return false; 
```

8.12.3.8 tcp_process_loss   
/*   
Location:   
net/ipv4/tcp_input.c   
Function: Process an ACK in CA_Loss state. Move to CA_Open if lost data are recovered or spurious. Otherwise retransmits more on partial ACKs.   
Parameter: sk:传输控制块 flag: is_dupack: static void tcp_process_loss(struct sock \*sk, int flag, bool is_dupack) { struct tcp_sock \*tp $=$ tcp_sk(sk); bool recovered $=$ !before(tp->snd_una,tp->high_seq); if ((flag & FLAG_SND_UNA_ADVANCED) && tcp_try Undo_loss(sk,false)) return; if (tp->frto){/\*F-RTO RFC5682 sec 3.1 (sack enhanced version). \*/ /\*Step 3.b.A timeout is spurious if not all data are \* lost,i.e.,never-retransmitted data are (s)acked. \*/ if ((flag & FLAG_ORIG_SACK_ACKED)&& tcp_try Undo_loss(sk, true)) return; if(after(tp->snd_nxt,tp->high_seq)){ if (flag & FLAG_DATA_SACKED || is_dupack) tp->frto $= 0$ ;/\*Step 3.a. loss was real \*/ } else if (flag & FLAG_SND_UNA_ADVANCED &&！recovered){ tp->high_seq $=$ tp->snd_nxt; --tcp-push_pending Frames(sk，tcp_current_mss(sk)， TCP_NAGLE_OFF); if(after(tp->snd_nxt,tp->high_seq)) return; /* Step 2.b */ tp->frto $= 0$ .   
}   
}   
if (recovered){ /\*F-RTO RFC5682 sec 3.1 step 2.a and 1st part of step 3.a \*/ tcp_try_undo_recovery(sk); return;   
}   
if (tcp_is_reno(tp)){ /\*A Reno DUPACK means new data in F-RTO step 2.b above are

8.12.3.9 tcp_try_undo_partial   
/*   
Location:   
net/ipo4/tcp_input.c   
Function:   
Undo during fast recovery after partial ACK.   
Parameter:   
sk:传输控制块。   
acked:   
prior_unacked:   
flag:   
*/   
static bool tcp_try_undo_partial(struct sock \*sk, const int acked, const int prior_unsacked, int flag)   
{ struct tcp SOCK \*tp $=$ tcp_sk(sk); if (tp->undomarker && tcpPacket_delayed(tp)) { /\*Plain luck! Hole if filled with delayed \* packet, rather than with a retransmit. \*/ tcp_update_reordering(sk, tcp_fackets_out(tp) + acked, 1); /\*We are getting evidence that the reordering degree is higher \* than we realized. If there are no retransmits out then we \* can undo. Otherwise we clock out new packets but do not \* mark more packets lost or retransmit more. \*/ if (tp->retrans_out) { tcp_cwnd_reduction(sk, prior_unsacked, 0, flag); return true; } if (!tcp_any_retrans_done(sk)) tp->retrans_stamp $= 0$ .   
DBGUNDO(sk,"partial recovery"); tcp Undo_cwnd Reduction(sk, true); NET_INC_STAT5_BH(sock_net(sk),LINUX_MIB.tcpARTIALUNDO); tcp_try_keep_open(sk); return true;

```txt
47 return false;   
48 } 
```

# 8.12.4 About Restransmit

8.12.4.1 Related Macro   
```c
1 /\* include/net/tcp.h   
2 \*/   
3 \*/ Use TCP RACK to detect (some) tail and retransmit losses \*/   
4 5 #define TCP_RACK_LOST_RETRANS Ox1 
```

# 8.13 TCP Finish

# 8.13.1 Time Compute

# 8.13.1.1 tcp_fin_time

计算等待接收 FIN 的超时时间。超时时间至少为 $\frac { 7 } { 2 }$ 倍的 rto。

1 /\* Location include/net/tcp.h   
3   
4   
5   
6   
7   
8   
9   
10   
11   
12   
13 static inline int tcp_fin_time(const struct sock \*sk) { int fin_timeout $=$ tcp_sk(sk)->linger2?：sysctl tcp_fin_timeout; const int rto $=$ inlet_csk(sk)->icsk_rto; if (fin_timeout $<  <   (\mathrm{rto} <   <   2) -$ (rto>>1)) fin_timeout $=$ (rto<<2)-(rto>>1); return fin_timeout;   
20

调用位置:

1 7.2.2.1的 tcp_rcv_state_process 中调用。

# 8.14 TCP Close

# 8.14.1 TCP Close Flags

这是关于是接收到关闭连接还是说发送了关闭连接的标志。

```c
/*   
Location include/net/sock.h   
\*/   
#define SHUTDOWN_MASK 3   
#define RCV_SHUTDOWN 1   
#define SEND_SHUTDOWN 2 
```

8.14.1.1 TCP State   
/*include/net/tcp_state.c*/   
enum { TCP_ESTABLISHED $= 1$ TCP_SYN_SENT, TCP_SYN_RECV, TCP_FIN_WAIT1, TCP_FIN_WAIT2, TCP_TIME_WAIT, TCP_CLOSE, TCP_CLOSE_WAIT, TCP LAST_ACK, TCP_LISTEN, TCP_CLOSING, /\* Now a valid state \*/ TCP_new_SYN_RECV, TCP_MAXStates /\*Leave at the end! \*/ }; #define TCP_STATE_MASK OxF

8.14.2 TCP State Machine   
8.14.3 tcp_done   
/*include/net/tcp_state.c*/   
#define TCP_ACTION_STATE $(1 <   <   7)$ /*Location:net/ipu4/tcp.c*/   
static const unsigned char new_state[16] $= \{$ $\text{一} ^ { \text{一} }$ 当前状态： 新的状态： 动作： \*/   
[0/\* (Invalid) \*/] =TCP_CLOSE,   
[TCP_ESTABLISHED] =TCP_CLOSE_WAIT1 | TCP_ACTION_STATE,   
[TCP_SYNSENT] =TCP_CLOSE,   
[TCP_SYN_RECV] =TCP_CLOSE_WAIT1 | TCP_ACTION_STATE,   
[TCP_SYN_WAIT1] =TCP_CLOSE_WAIT1,   
[TCP_SYN_WAIT2] =TCP_CLOSE_WAIT2,   
[TCP_TIME_WAIT] =TCP_CLOSE,   
[TCP_CLOSE] =TCP_CLOSE,   
[TCP_CLOSE_WAIT] =TCP LAST_ACK | TCP_ACTION_STATE,   
[TCP LAST_ACK] =TCP LAST_ACK,   
[TCP_LISTEN] =TCP_CLOSE,   
[TCP_CLOSING] =TCP_CLOSING,   
[TCP Newly SYN_RECV] =TCP_CLOSE, /*should not happen！*/   
};

```c
/* 该函数用于完成关闭 TCP 连接，回收并清理相关资源。*/  
void tcp_done(struct sock *sk)  
{  
    struct request_sock *req = tcp_sk(sk) -> fastopen_rsk;  
    /* 当套接字状态为 SYN_SENT 或 SYN_REV 时，更新统计数据。*/  
    if (sk->sk_state == TCP_SYNSENT || sk->sk_state == TCP_SYN_REV)  
        TCP_INC_STATISTICS_BH(sock_net(sk), TCP_MIB_ATTEMPTFAILS);  
    /* 将连接状态设置为关闭，并清除定时器。*/  
    tcp_set_state(sk, TCP_CLOSE);  
    tcp_clear_xmit_timers(sk);  
    /* 当启用了 Fast_Open 时，移除 fastopen 请求 */  
    if (req)  
        reqsk_fastopen_remove(sk, req, false);  
    sk->sk_shutdown = SHUTDOWN_MASK;  
    /* 如果状态不为 SOCK_DEAD，则唤醒等待着的进程。*/  
    if (!sock_flag(sk, SOCKDead))  
        sk->sk_state_change(sk);  
    else  
        inset_cskdestroy_sock(sk);  
}
```

# 8.14.4 tcp_shutdown

tcp_shutdown是 TCP 的 shutdown 系统调用的传输层接口实现，由套接口层的实现inet_shutdown调用。

```c
/\*   
Location:   
net/iju4/tcp.c   
Function: Shutdown the sending side of a connection. Much like close except that we don't receive shut down or sock_set_flag(sk, SOCK_DEAD).   
Parameter: sk:传输控制块。 how:   
\*/   
void tcp_shutdown(struct sock \*sk，int how)   
{ /\* We need to grab some memory,and put together a FIN, \* and then put it into the queue to be sent. \* Tim MacKenzie(tym@dibbler.cs.monash.edu.au) 4 Dec '92. \*/ if(!(how&SEND_SHUTDOWN)) return;   
/\* If we've already sent a FIN,or it's a closed state,skip this.\*/ if((1<<sk->sk_state)& (TCPF_ESTABLISHED|TCPF_SYN_SENT 
```

```txt
TCPF_SYN_RECV | TCPF_CLOSE_WAIT)) { /* Clear out any half completed packets. FIN if needed. */ if (tcp_close_state(sk)) tcp_send_fin(sk); } } 
```

如果是发送方向的关闭，并且 TCP 状态为 ESTABLISHED、SYN_SENT、SYN_RECV或 CLOSE_WAIT 时，根据 TC 状态迁移图和当前的状态设置新的状态，并在需要发送 FIN 时，调用 FIN 时，调用tcp_send_fin时向对方发送 FIN。

而对于接收方向的关闭，则无需向对方发送 FIN，因为可能还需要向对方发送数据。至于接收方向的关闭的实现，在 recvmsg 系统调用中发现设置了 RCV_SHUTDOWN标志会立即返回。

# CHAPTER 9

附录: 基础知识

# Contents

# 9.1 计算机底层知识 . . 216

9.1.1 机器数 . . 216  
9.1.1.1 正负数 . . 216  
9.1.1.2 原码 . . 216  
9.1.1.3 反码 . . 216  
9.1.1.4 补码 . . 216

# 9.2 GNU/LINUX . . . 217

9.2.1 错误处理 217

9.2.1.1 错误码 217  
9.2.1.2 IS_ERR,PTR_ERR,ERR_PTR . 220

9.2.2 调试函数 221

9.2.2.1 BUG_ON 221   
9.2.2.2 WARN_ON . . . 221

# 9.3 C 语言 . . 221

9.3.1 结构体初始化 . . 221  
9.3.2 位字段 . . 222

# 9.4 GCC 222

9.4.1 __attribute_ 222

9.4.1.1 设置变量属性 . . . 223  
9.4.1.2 设置类型属性 . . . 223  
9.4.1.3 设置函数属性 . . . 223

9.4.2 分支预测优化 . . 223

# 9.5 Sparse . . . 224

9.5.1 __bitwise 224

9.6 操作系统 . 225  
9.6.1 RCU . . 225   
9.7 CPU 225   
9.8 存储系统 . 225

9.8.1 字节序 . . . 225   
9.8.2 缓存 Cache . . 226

9.8.2.1 __read_mostly 226

# 9.1 计算机底层知识

# 9.1.1 机器数

在计算机内的数（称之为“机器数”）值有 3 种表示法：原码、反码和补码。

# 9.1.1.1 正负数

在计算机内，通常把二进制数的最高位定义为符号位，用 0 表示正数，用 1 表示负数。其余表示数值。

在下面的陈述中，为了方便，我们均假设机器的字长为 8 位。

# 9.1.1.2 原码

原码 (true form) 是一种计算机中对数字的二进制定点表示方法。原码表示法在数值前面增加了一位符号位（即最高位为符号位）：正数该位为 0，负数该位为 1（0 有两种表示： $+ 0$ 和-0），其余位表示数值的大小。

如 $- 1 = 1 0 0 0 0 0 0 1$   ， $1 = 0 0 0 0 0 0 0 1$   。

# 9.1.1.3 反码

正数的反码与其原码相同；负数的反码是对其原码逐位取反，但符号位除外。一般情况下，我们需要先把给定数字转化为原码，然后转化为反码。

如 −1 = 10000001   = 11111110  ， $1 = 0 0 0 0 0 0 0 1 \ = 0 0 0 0 0 0 1 .$

# 9.1.1.4 补码

正数的补码就是其本身，负数的补码是在其原码的基础上, 符号位不变, 其余各位取反, 最后 $+ 1$ 。

如 1 = 10000001   = 11111110   = 11111111  ， $1 = 0 0 0 0 0 0 1 = 0 0 0 0 0 0 1 =$ 00000001 . 如果在加 1 的时候出现了溢出就直接不管那个溢出的 1. 由于补码利于运算，故而计算机中数都是利用补码表示的。

# 9.2 GNU/LINUX

# 9.2.1 错误处理

# 9.2.1.1 错误码

```c
/* Location: 1 - 34 include/uapi/asm-generic/errno-base.h 35- 133 include/uapi/asm-generic/errno.h   
Function:   
定义了Linux中的错误码。   
\*/   
#define EPERM 1 /\*Operation not permitted \*/   
#define ENOENT 2 /\*No such file or directory \*/   
#define ESRCH 3 /\*No such process \*/   
#define EINTR 4 /\*Interrupted system call \*/   
#define EIO 5 /\*I/O error \*/   
#define ENXIO 6 /\*No such device or address \*/   
#define E2BIG 7 /\*Argument list too long \*/   
#define ENOEXEC 8 /\*Exec format error \*/   
#define EBADF 9 /\*Bad file number \*/   
#define ECHILD 10 /\*No child processes \*/   
#define EAGAIN 11 /\*Try again \*/   
#define ENOMEM 12 /\*Out of memory \*/   
#define EACCES 13 /\*Permission denied \*/   
#define EFAULT 14 /\*Bad address \*/   
#define ENOTBLK 15 /\*Block device required \*/   
#define EBUSY 16 /\*Device or resource busy \*/   
#define EEXIST 17 /\*File exists \*/   
#define EXDEV 18 /\*Cross-device link \*/   
#define ENODEV 19 /\*No such device \*/   
#define ENOTDIR 20 /\*Not a directory \*/   
#define EISDIR 21 /\*Is a directory \*/   
#define EINVALID 22 /\*Invalid argument \*/   
#define ENFILE 23 /\*File table overflow \*/   
#define EMFILE 24 /\*Too many open files \*/   
#define ENOTTY 25 /\*Not a typewriter \*/   
#define ETXTBSY 26 /\*Text file busy \*/   
#define EFBIG 27 /\*File too large \*/   
#define ENOSPC 28 /\*No space left on device \*/   
#define ESPIEE 29 /\*Illegal seek \*/   
#define EROFS 30 /\*Read-only file system \*/   
#define EMLINK 31 /\*Too many links \*/   
#define EPIPE 32 /\*Broken pipe \*/   
#define EDM 33 /\*Math argument out of domain of func \*/   
#define ERANGE 34 /\*Math result not representable \*/   
#define EDEADLK 35 /\*Resource deadlock would occur \*/   
#define ENAMETOOLONG 36 /\*File name too long \*/   
#define ENOLCK 37 /\*No record locks available \*/   
/* This error code is special: arch syscall entry code will return   
\*-ENOSYS if users try to call a syscall that doesn't exist. To keep 
```

* failures of syscalls that really do exist distinguishable from   
* failures due to attempts to use a nonexistent syscall, syscall   
* implementations should refrain from returning -ENOSYS. *

#define ENOSYS 38 /* Invalid system call number */   
#define ENOTEMPTY 39 /* Directory not empty */   
#define ELOOP 40 /* Too many symbolic links encountered */ #define EWOULDBLOCK EAGAIN /* Operation would block */   
#define ENOMSG 42 /* No message of desired type */ #define EIDRM 43 /* Identifier removed */   
#define ECHRNG 44 /* Channel number out of range */   
#define EL2NSYNC 45 /* Level 2 not synchronized */   
#define EL3HLT 46 /* Level 3 halted */   
#define EL3RST 47 /* Level 3 reset */   
#define ELNRNG 48 /* Link number out of range */   
#define EUNATCH 49 /* Protocol driver not attached */   
#define ENOCSI 50 /* No CSI structure available */ #define EL2HLT 51 /* Level 2 halted */   
#define EBADE 52 /* Invalid exchange */   
#define EBADR 53 /* Invalid request descriptor */   
#define EXFULL 54 /* Exchange full */   
#define ENOANO 55 /* No anode */   
#define EBADRQC 56 /* Invalid request code */ #define EBADSLT 57 /* Invalid slot */   
#define EDEADLOCK EDEADLK   
#define EBFONT 59 /* Bad font file format */   
#define ENOSTR 60 /* Device not a stream */   
#define ENODATA 61 /* No data available */   
#define ETIME 62 /* Timer expired */   
#define ENOSR 63 /* Out of streams resources *   
#define ENONET 64 /* Machine is not on the network */   
#define ENOPKG 65 /* Package not installed */   
#define EREMOTE 66 /* Object is remote */   
#define ENOLINK 67 /* Link has been severed */   
#define EADV 68 /* Advertise error */   
#define ESRMNT 69 /* Srmount error */   
#define ECOMM 70 /* Communication error on send */   
#define EPROTO 71 /* Protocol error */   
#define EMULTIHOP 72 /* Multihop attempted   
#define EDOTDOT 73 /* RFS specific error */   
#define EBADMSG 74 /* Not a data message */   
#define EOVERFLOW 75 /* Value too large for defined data type */   
#define ENOTUNIQ 76 /* Name not unique on network */   
#define EBADFD 77 /* File descriptor in bad state */   
#define EREMCHG 78 /* Remote address changed */   
#define ELIBACC 79 /* Can not access a needed shared library */   
#define ELIBBAD 80 /* Accessing a corrupted shared library */   
#define ELIBSCN 81 /* .lib section in a.out corrupted */   
#define ELIBMAX 82 /* Attempting to link in too many shared libraries */   
#define ELIBEXEC 83 /* Cannot exec a shared library directly */   
#define EILSEQ 84 /* Illegal byte sequence */   
#define ERESTART 85 /* Interrupted system call should be restarted */   
#define ESTRPIPE 86 /* Streams pipe error */   
#define EUSERS 87 /* Too many users */   
#define ENOTSOCK 88 /* Socket operation on non-socket */

```c
111 #define EDESTADDRREQ 89 /* Destination address required */   
112 #define EMSGSIZE 90 /* Message too long */   
113 #define EPROTETYPE 91 /* Protocol wrong type for socket */   
114 #define ENOPROTOOPT 92 /* Protocol not available */   
115 #define EPROTONOSUPPORT 93 /* Protocol not supported */   
116 #define ESOCKTNOSUPPORT 94 /* Socket type not supported */   
117 #define EOPNOTSUPP 95 /* Operation not supported on transport endpoint */   
118 #define EPFNOSUPPORT 96 /* Protocol family not supported */   
119 #define EAFNOSUPPORT 97 /* Address family not supported by protocol */   
120 #define EADDRINUSE 98 /* Address already in use */   
121 #define EADDRNOTAVAIL 99 /* Cannot assign requested address */   
122 #define ENETDOWN 100 /* Network is down */   
123 #define ENETUNREACH 101 /* Network is unreachable */   
124 #define ENETRESET 102 /* Network dropped connection because of reset */   
125 #define ECONNABORTED 103 /* Software caused connection abort */   
126 #define ECONNRESET 104 /* Connection reset by peer */   
127 #define ENOBUFS 105 /* No buffer space available */   
128 #define EISCONN 106 /* Transport endpoint is already connected */   
129 #define ENOTCONN 107 /* Transport endpoint is not connected */   
130 #define ESHUTDOWN 108 /* Cannot send after transport endpoint shutdown */   
131 #define ETOOMANYREFS 109 /* Too many references: cannot splice */   
132 #define ETIMEDOUT 110 /* Connection timed out */   
133 #define ECONNREFUSED 111 /* Connection refused */   
134 #define EHOSTDOWN 112 /* Host is down */   
135 #define EHOSTUNREACH 113 /* No route to host */   
136 #define EALREADY 114 /* Operation already in progress */   
137 #define EINPROGRESS 115 /* Operation now in progress */   
138 #define ESTALE 116 /* Stale file handle */   
139 #define EUCLEAN 117 /* Structure needs cleaning */   
140 #define ENOTNAM 118 /* Not a XENIX named type file */   
141 #define ENAVAL 119 /* No XENIX semaphores available */   
142 #define EISNAM 120 /* Is a named type file */   
143 #define EREMOTEIO 121 /* Remote I/O error */   
144 #define EDQUOT 122 /* Quota exceeded */   
145   
146 #define ENOMEDIUM 123 /* No medium found */   
147 #define EMEDIUMTYPE 124 /* Wrong medium type */   
148 #define ECANCELED 125 /* Operation Canceled */   
149 #define ENOKEY 126 /* Required key not available */   
150 #define EKEYEXPIRED 127 /* Key has expired */   
151 #define EKEYREVOKED 128 /* Key has been revoked */   
152 #define EKEYREJECTED 129 /* Key was rejected by service */   
153   
/* for robust mutexes */   
#define EOWNERDEAD 130 /* Owner died */   
#define ENOTRECOVERABLE 131 /* State not recoverable */   
157   
#	define ERFKILL 132 /* Operation not possible due to RF-kill */   
159   
#	define EHWPOISON 133 /* Memory page has hardware error */ 
```

# 9.2.1.2 IS_ERR,PTR_ERR,ERR_PTR

相关定义如下：

```txt
1 /* Location: 2 3 
```

```c
include/linux/err.h   
Description: Kernel pointers have redundant information, so we can use a scheme where we can return either an error code or a normal pointer with the same return value. This should be a per-architecture thing, to allow different error and pointer decisions.   
\*/ #define MAX_ERRNO 4095 #idfdef _ASSEMBLY_ #define IS_ERR_VALUE(x) unlikely((x) >= (unsigned long)-MAX_ERRNO) static inline void \* must_check ERR_PTR(long error) { return(void \*)error; } static inline long __must_check PTR_ERR(_force const void \*ptr) { return(long)ptr; } static inline bool __must_check IS_ERR(_force const void \*ptr) { return IS_ERR_VALUE((unsigned long)ptr); } static inline bool __must_check IS_ERR_OR_NULL(_force const void \*ptr) { return !ptr || IS_ERR_VALUE((unsigned long)ptr); } 
```

要想明白上述的代码，首先要理解内核空间。所有的驱动程序都是运行在内核空间，内核空间虽然很大，但总是有限的，而在这有限的空间中，其最后一个 page 是专门保留的，也就是说一般人不可能用到内核空间最后一个 page 的指针。换句话说，你在写设备驱动程序的过程中，涉及到的任何一个指针，必然有三种情况：有效指针；NULL，空指针；错误指针，或者说无效指针。

而所谓的错误指针就是指其已经到达了最后一个 page，即内核用最后一页捕捉错误。比如对于 32bit 的系统来说，内核空间最高地址 0xffffffff，那么最后一个 page 就是指的 0xfffff000 0xffffffff(假设 4k 一个 page)，这段地址是被保留的。内核空间为什么留出最后一个 page？我们知道一个 page 可能是 4k，也可能是更多，比如 8k，但至少它也是 4k，留出一个 page 出来就可以让我们把内核空间的指针来记录错误了。内核返回的指针一般是指向页面的边界 (4k 边界)，即ptr & 0xfff $\scriptstyle = = 0$ 。如果你发现你的一个指针指向这个范围中的某个地址，那么你的代码肯定出错了。IS_ERR() 就是判断指针是否有错，如果指针并不是指向最后一个 page，那么没有问题；如果指针指向了最后一个page，那么说明实际上这不是一个有效的指针，这个指针里保存的实际上是一种错误代码。而通常很常用的方法就是先用 IS_ERR() 来判断是否是错误，然后如果是，那么就

调用 PTR_ERR() 来返回这个错误代码。因此，判断一个指针是不是有效的，我们可以调用宏 IS_ERR_VALUE，即判断指针是不是在（0xfffff000，0xffffffff) 之间，因此，可以用 IS_ERR() 来判断内核函数的返回值是不是一个有效的指针。注意这里用 unlikely()的用意！

至于 PTR_ERR(), ERR_PTR()，只是强制转换以下而已。而 PTR_ERR() 只是返回错误代码，也就是提供一个信息给调用者，如果你只需要知道是否出错，而不在乎因为什么而出错，那你当然不用调用 PTR_ERR() 了。

如果指针指向了最后一个 page，那么说明实际上这不是一个有效的指针。这个指针里保存的实际上是一种错误代码。而通常很常用的方法就是先用 IS_ERR() 来判断是否是错误，然后如果是，那么就调用 PTR_ERR() 来返回这个错误代码。

调用位置:

1 5.1.2.2中 tcp_v4_connect 调用 IS_ERR.

# 9.2.2 调试函数

# 9.2.2.1 BUG_ON

```c
1 #define BUG_ON(condition) do { /  
2 if (unlikely((condition)! = 0)) /  
3 BUG(); /  
4 } while(0) 
```

如果在程序的执行过程中，觉得该 condition 下是一个 BUG，可以添加此调试信息，查看对应堆栈内容。

# 9.2.2.2 WARN_ON

而 WARN_ON 则是调用 dump_stack，打印堆栈信息，不会 OOPS

```c
define WARN_ON(condition) do {
    if (unlikely(condition)! = 0) {
        printf("Badness in %s at %s:%d/n", __FUNCTION__, __FILE__, __LINE%);
        dump_stack(); // }
    } / 
} while (0) 
```

# 9.3 C 语言

# 9.3.1 结构体初始化

typedef struct {
    int a;
    char ch;
} flag;  
/* 目的是将 $a$ 初始化为 $1, ch$ 初始化为 'u'.  
*/  
/* 法一：分别初始化 */  
flag tmp;

10 tmp.a=1;   
11 tmp.ch='u';   
12   
13 /\*法二：点式初始化\*/   
14 flag tmp $=$ {.a=1,.ch $\equiv$ 'u'}； //注意两个变量之间使用，而不是；   
15 /\*法三：\*/   
16 flag tmp={   
17 a:1,   
18 ch:'u'   
19 }；

当然，我们也可以使用上述任何一种方法只对结构体中的某几个变量进行初始化。

# 9.3.2 位字段

在存储空间极为宝贵的情况下，有可能需要将多个对象保存在一个机器字中。而在linux 开发的早期，那时确实空间极其宝贵。于是乎，那一帮黑客们就发明了各种各样的办法。一种常用的办法是使用类似于编译器符号表的单个二进制位标志集合，即定义一系列的 2 的指数次方的数，此方法确实有效。但是，仍然十分浪费空间。而且有可能很多位都利用不到。于是乎，他们提出了另一种新的思路即位字段。我们可以利用如下方式定义一个包含 3 位的变量。

```txt
1 struct { unsigned int a:1; unsigned int b:1; unsigned int c:1; }flags; 
```

字段可以不命名，无名字段，即只有一个冒号和宽度，起到填充作用。特殊宽度 0可以用来强制在下一个字边界上对齐，一般位于结构体的尾部。

冒号后面表示相应字段的宽度 (二进制宽度)，即不一定非得是 1 位。字段被声明为unsigned int类型，以确保它们是无符号量。

当然我们需要注意，机器是分大端和小端存储的。因此，我们在选择外部定义数据的情况下爱，必须仔细考虑那一端优先的问题。同时，字段不是数组，并且没有地址，因此不能对它们使用&运算符。

# 9.4 GCC

# 9.4.1 __attribute__

GNU C 的一大特色就是__attribute__机制。__attribute__可以设置函数属性（Function Attribute）、变量属性（Variable Attribute）和类型属性（Type Attribute）。

__attribute__的书写特征是：__attribute__前后都有两个下划线，并切后面会紧跟一对原括弧，括弧里面是相应的__attribute__参数。

__attribute__的语法格式为：__attribute__((attribute-list))

__attribute__的位置约束为：放于声明的尾部“；”之前。参考博客：http://www.cnblogs.com/astwish/p关键字__attribute__也可以对结构体（struct）或共用体（union）进行属性设置。大致有

六个参数值可以被设定，即：aligned, packed, transparent_union, unused, deprecated和 may_alias 。

在使用__attribute__参数时，你也可以在参数的前后都加上两个下划线，例如，使用__aligned__而不是 aligned ，这样，你就可以在相应的头文件里使用它而不用关心头文件里是否有重名的宏定义。aligned (alignment) 该属性设定一个指定大小的对齐格式（以字节为单位）。

# 9.4.1.1 设置变量属性

下面的声明将强制编译器确保（尽它所能）变量类型为int32_t的变量在分配空间时采用 8 字节对齐方式。

```javascript
typedef int int32_t __attribute__(aligned(8)); 
```

# 9.4.1.2 设置类型属性

下面的声明将强制编译器确保（尽它所能）变量类型为 struct S 的变量在分配空间时采用 8 字节对齐方式。

```txt
1 struct S { short b[3];   
2 } __attribute__ ((aligned (8)); 
```

如上所述，你可以手动指定对齐的格式，同样，你也可以使用默认的对齐方式。如果 aligned 后面不紧跟一个指定的数字值，那么编译器将依据你的目标机器情况使用最大最有益的对齐方式。例如：

```txt
1 struct S { short b[3];   
2 }__attribute_((aligned)); 
```

这里，如果 sizeof（short）的大小为 2（byte），那么，S 的大小就为 6 。取一个 2的次方值，使得该值大于等于 6 ，则该值为 8，所以编译器将设置 S 类型的对齐方式为8 字节。aligned 属性使被设置的对象占用更多的空间，相反的，使用 packed 可以减小对象占用的空间。需要注意的是，attribute 属性的效力与你的连接器也有关，如果你的连接器最大只支持 16 字节对齐，那么你此时定义 32 字节对齐也是无济于事的。

# 9.4.1.3 设置函数属性

# 9.4.2 分支预测优化

现代处理器均为流水线结构。而分支语句可能导致流水线断流。因此，很多处理器均有分支预测的功能。然而，分支预测失败所导致的惩罚也是相对高昂的。为了提升性能，Linux 的很多分支判断中都使用了 likely()和unlikely()这组宏定义来人工指示编译器，哪些分支出现的概率极高，以便编译器进行优化。

这里有两种定义，一种是开启了分支语句分析相关的选项时，内核会采用下面的一种定义

```c
/* include/linux/compiler.h */
* 采用 __builtin_constant_p(x) 来忽略常量表达式。
*/
{
    # ifndef likely
        # define likely(x) (__builtin_constant_p(x) ? !!(x) : __branch_check__(x, 1))
    #endif
    # ifndef unlikely
        # define unlikely(x) (__builtin_constant_p(x) ? !!(x) : __branch_check__(x, 0))
    #endif 
```

在该定义下，__branch_check__用于跟踪分支结果并更新统计数据。

如果不开启该选项，则定义得较为简单：

1 #define likely(x) __builtinbaof $!!(x)$ 1   
2 #defineunlikely(x) __builtinbaof $!!(x)$ 0

其中__builtin_expect是 GCC 的内置函数，用于指示编译器，该条件语句最可能的结果是什么。

调用位置:

1 5.2.3.3中的 tcp_make_synack 调用。

# 9.5 Sparse

# 9.5.1 __bitwise

```txt
1 #define __bitwise __attribute__(bitwise)) 
```

该宏主要是为了确保变量是相同的位方式 (比如 bit-endian, little-endiandeng)，对于使用了__bitwise宏的变量，Sparse 会检查这个变量是否一直在同一种位方式 (big-endian, little-endian 或其他) 下被使用。如果此变量再多个位方式下被使用了，Sparse会给出警告。例子：

```c
#include <check>
#define __attribute__((bitwise))
else
#define __attribute__ 
endif
#define __CHECK_ENDIAN_
#define __attribute__ __bitwise__
else
#define __bitwise
endif
typedef __u16 __bitwise __le16;
typedef __u16 __bitwise __be16;
typedef __u32 __bitwise __le32;
typedef __u32 __bitwise __be32;
typedef __u64 __bitwise __le64;
typedef __u64 __bitwise __be64; 
```

# 9.6 操作系统

# 9.6.1 RCU

RCU(Read-Copy Update), 是数据同步的一种方式，在当前的 Linux 内核中发挥着重要的作用。RCU 主要针对的数据对象是链表，目的是提高遍历读取数据的效率。为了达到目的，使用 RCU 机制读取数据的时候不对链表进行耗时的加锁操作。这样在同一时间可以有多个线程同时读取该链表，并且允许一个线程对链表进行修改（修改的时候，需要加锁）。RCU 适用于需要频繁的读取数据，而相应修改数据并不多的情景，例如在文件系统中，经常需要查找定位目录，而对目录的修改相对来说并不多，这就是 RCU 发挥作用的最佳场景。

Linux 内核源码当中, 关于 RCU 的文档比较齐全，我们可以在 /Documentation/RCU/目录下找到这些文件。

通过 RCU 的实现，主要解决以下问题：

1、在读取过程中，另外一个线程删除了一个节点。删除线程可以把这个节点从链表中移除，但它不能直接销毁这个节点，必须等到所有的读取线程读取完成以后，才进行销毁操作。RCU 中把这个过程称为宽限期（Grace period）。  
2、在读取过程中，另外一个线程插入了一个新节点，而读线程读到了这个节点，那么需要保证读到的这个节点是完整的。这里涉及到了发布-订阅机制（Publish-SubscribeMechanism）。  
3、保证读取链表的完整性。新增或者删除一个节点，不至于导致遍历一个链表从中间断开。但是 RCU 并不保证一定能读到新增的节点或者不读到要被删除的节点。

调用位置:

1 5.1.2.2中 tcp_v4_connect 调用 rcu_dereference_protected.

# 9.7 CPU

# 9.8 存储系统

# 9.8.1 字节序

在计算机中内存存储一般分为大端和小端两种。而在网络传输的过程中，大小端的不一致会带来问题。因此，网络协议中对于字节序都有明确规定。一般采用大端序。

Linux 中，对于这一部分的支持放在了include/linux/byteorder/generic.h 中。而实现，则交由体系结构相关的代码来完成。

/* 下面的函数用于进行对 16 位整型或者 32 位整型在网络传输格式和本地格式之间的转换。

```lisp
\*/  
ntohl(_u32 x)  
ntohs(_u16 x)  
htonl(_u32 x)  
htons(_u16 x)
```

上面函数的命名规则是末尾的 l 代表 32 位，s 代表 16 位。n 代表 network，h 代表host。根据命名规则，不难知道函数的用途。比如 htons 就是从本地的格式转换的网络传输用的格式，转换的是 16 位整数。

# 9.8.2 缓存 Cache

# 9.8.2.1 __read_mostly

我们经常需要被读取的数据定义为__read_mostly类型，这样 Linux 内核被加载时，该数据将自动被存放到 Cache 中，以提高整个系统的执行效率。另一方面，如果所在的平台没有 Cache，或者虽然有 Cache，但并不提供存放数据的接口 (也就是并不允许人工放置数据在 Cache 中)，这样定义为__read_mostly类型的数据将不能存放在 Linux 内核中，甚至也不能够被加载到系统内存去执行，将造成 Linux 内核启动失败。??? 需要再仔细去思考。