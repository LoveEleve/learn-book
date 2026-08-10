# TCP流控(滑动窗口) + 拥塞控制(CUBIC/BBR) + 分段与分片

> Cluster B: 13 KPs | 依赖: 02-TCP状态机 | 读者基线: TCP包格式与握手

---

### 1. 滑动窗口 — TCP流控的物理极限
  - 窗口语义: 接收方通告"我还能收N字节", 发送方最多发N字节未确认数据 (RFC 793 §3.4)
  - 内核实现: tcp_sock.rcv_wnd(通告窗口), tcp_sock.snd_wnd(发送窗口=min(拥塞窗口cwnd, 通告窗口rwnd)) (include/linux/tcp.h → tcp_sock)
  - 零窗口探测: 对端通告rwnd=0时, 发送方定时发ZeroWindowProbe探测窗口是否恢复 (net/ipv4/tcp_output.c → tcp_send_probe0)
  - Window Scale: TCP Options字段扩展窗口倍数(最大14倍), 解决16位窗口最大值65KB限制 (net/ipv4/tcp_output.c → tcp_syn_options → TCPOPT_WINDOW)
  - 滑动本质: 窗口=等待确认的区间, ACK到达→窗口右移, 新Seq可发 (net/ipv4/tcp_input.c → tcp_clean_rtx_queue)

### 2. TCP分段 vs IP分片 — send 1KB到底发几个包
  - TCP分段: 传输层行为 — TCP按MSS(通常1460=1500MTU-20IP头-20TCP头)切分用户数据 (net/ipv4/tcp_output.c → tcp_mtu_to_mss)
  - IP分片: 网络层行为 — 链路MTU更小时(如隧道), IP层按MTU拆包, 接收端重组 (net/ipv4/ip_output.c → ip_fragment)
  - 为什么避免IP分片: 分片丢失=整个IP包重传(无SACK), 且防火墙常丢弃分片 (DF位: net/ipv4/ip_output.c → ip_dont_fragment)
  - PMTUD(路径MTU发现): 设DF位→沿途MTU小的链路上ICMP Frag Needed→调整MSS (net/ipv4/tcp_ipv4.c → tcp_v4_err)
  - MSS vs MTU: MSS=MTU-40(IP头+TCP头标准), 实际值握手时SYN的Options字段协商 (net/ipv4/tcp_output.c → tcp_syn_options)

### 3. 拥塞控制算法 — 从Reno到CUBIC
  - 核心变量: cwnd(拥塞窗口, 限制inflight包数), ssthresh(慢启动阈值) (include/net/tcp.h → tcp_congestion_ops)
  - 慢启动: cwnd从1 MSS指数增长, 每RTT翻倍, 直到达到ssthresh或丢包 (net/ipv4/tcp_input.c → tcp_slow_start)
  - 拥塞避免: cwnd线性增长(每RTT+1 MSS), 丢包时ssthresh=cwnd/2, cwnd降为1或恢复 (net/ipv4/tcp_cong.c → tcp_reno_cong_avoid)
  - 快速重传: 收到3个重复ACK→立即重传缺失段, 不等超时 (net/ipv4/tcp_input.c → tcp_fastretrans_alert)
  - 快速恢复: 快速重传后不进入慢启动, 而是cwnd=ssthresh+3×MSS继续线性增长 (net/ipv4/tcp_input.c → tcp_fastretrans_alert)

### 4. CUBIC — 现代Linux默认拥塞算法
  - 核心思想: cwnd按三次函数增长 W(t)=C×(t-K)³+W_max (net/ipv4/tcp_cubic.c → bictcp_update)
  - 优势: 对RTT不敏感(不同于Reno增1/RTT), 公平性更好, 吞吐更高 (net/ipv4/tcp_cubic.c → bictcp_cong_avoid)
  - Hystart: 慢启动快速退出 — 利用RTT增长检测瓶颈, 提前进入拥塞避免 (net/ipv4/tcp_cubic.c → hystart_update)
  - 查看当前算法: `sysctl net.ipv4.tcp_congestion_control` (/proc/sys/net/ipv4/tcp_congestion_control)

### 5. BBR — Google的拥塞控制革命
  - 核心理念: 基于带宽(BW)和RTT建模, 非基于丢包 — 解决bufferbloat问题 (net/ipv4/tcp_bbr.c → bbr_main)
  - 状态机: Startup→Drain→ProbeBW→ProbeRTT 四个阶段循环 (net/ipv4/tcp_bbr.c → bbr_update_model)
  - BW探测: 每8个RTT中1个做增益1.25×发送, 1个做减益0.75×, 其余6个正常 (net/ipv4/tcp_bbr.c → bbr_update_cycle_phase)
  - 与传统对比: CUBIC填满buffer才丢包→高延迟, BBR在buffer填满前感知瓶颈 (适合长肥管道)
  - 加载BBR: `modprobe tcp_bbr && sysctl -w net.ipv4.tcp_congestion_control=bbr`

### 6. 收束
  - 滑动窗口=接收方驱动的流控, cwnd=发送方驱动的拥塞控制, 实际发送窗口=min(rwnd, cwnd)
  - MSS/MTU是理解"一个包能装多少数据"的基础: send 1500B→1包, send 1501B→2包(1个1460+1个41)
  - CUBIC→BBR是从丢包信号到带宽探测的范式转换, BBR v3是当前最佳实践

---

### 核心悬念
**"如果TCP有完美的流控和拥塞控制, 为什么线上还全是TIME_WAIT过多、粘包、Nagle延迟的问题?"**

→ 引出 TCP生产排错全景(04-tcp-troubleshooting)
