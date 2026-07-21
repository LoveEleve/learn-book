## Page 1

<span style="font-family:'LiberationSerif';font-size:9.21pt;color:#6e675f">A G E N T WAY  /  H A R N E S S  B O O K S</span>

<span style="font-family:'UMingCN';font-size:14.47pt;color:#111111">控制面</span>
<span style="font-family:'UMingCN';font-size:14.47pt;color:#111111">循环</span>
<span style="font-family:'UMingCN';font-size:14.47pt;color:#111111">恢复</span>
<span style="font-family:'UMingCN';font-size:9.87pt;color:#efe7d9">先有规矩</span>
<span style="font-family:'UMingCN';font-size:9.87pt;color:#efe7d9">再谈聪明</span>

<span style="font-family:'UMingCN';font-size:13.15pt;color:#111111">权限</span>
<span style="font-family:'UMingCN';font-size:13.15pt;color:#111111">中断</span>
<span style="font-family:'UMingCN';font-size:13.15pt;color:#111111">验证</span>
<span style="font-family:'LiberationSerif';font-size:7.23pt;color:#c7beb0">SYSTEM FIRST</span>
<span style="font-family:'LiberationSerif';font-size:7.23pt;color:#c7beb0">MODEL SECOND</span>

<span style="font-family:'LiberationSerif-Bold';font-size:42.09pt;font-weight:bold;color:#161616">Harness</span>
<span style="font-family:'LiberationSerif-Bold';font-size:42.09pt;font-weight:bold;color:#161616">Engineering</span>

<span style="font-family:'LiberationSerif';font-size:24.99pt;color:#7b1e1e">Claude Code </span><span style="font-family:'UMingCN';font-size:24.99pt;color:#7b1e1e">设计指南</span>

<span style="font-family:'UMingCN';font-size:20.39pt;color:#f4ede0">一个系统是否可靠，</span>
<span style="font-family:'UMingCN';font-size:20.39pt;color:#f4ede0">不在它会不会说，</span>
<span style="font-family:'UMingCN';font-size:20.39pt;color:#f4ede0">而在它出了岔子以后，</span>
<span style="font-family:'UMingCN';font-size:20.39pt;color:#f4ede0">谁来收拾残局。</span>

<span style="font-family:'LiberationSerif';font-size:9.87pt;color:#d7cdbd">CONTROL PLANE / QUERY LOOP / RECOVERY / VERIFICATION</span>
<span style="font-family:'UMingCN';font-size:9.87pt;color:#d7cdbd">从源码抽象出可控 </span><span style="font-family:'LiberationSerif';font-size:9.87pt;color:#d7cdbd">AI </span><span style="font-family:'UMingCN';font-size:9.87pt;color:#d7cdbd">编程系统的工程原则</span>
<span style="font-family:'LiberationSerif';font-size:9.87pt;color:#d34a43">agentway.dev</span>

---

## Page 2

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:17.93pt;color:#000000">从会用Agent，到做出Agent PoC</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:15.94pt;color:#7b1e1e">https://agentway.dev</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:10.46pt;color:#666666">2026‑04‑01 ‧ rev 37dfc2</span>

---

## Page 3

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">目录</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">导读</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">1</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">序言Harness</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">2</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第1 章为什么需要Harness Engineering</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">5</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.1 问题在于让模型别乱来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">5</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.2 Claude Code 的第一层Harness：受约束的会话系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">5</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.3 第二层Harness：代理依赖持续循环</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">6</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.4 第三层Harness：工具调用必须服从调度</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">7</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.5 第四层Harness：最危险的工具，必须配最细的规矩</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">7</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.6 第五层Harness：错误属于主路径的一部分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">8</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">1.7 从源码里可以提炼出的第一个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">8</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第2 章Prompt 不是人格</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">10</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.1 把prompt 当成人设，是一种常见误会</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">10</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.2 从源码看，Claude Code 的prompt 从一开始就是分层的</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">10</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.3 Prompt 的真正价值，不在文字本身，而在优先级</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">11</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.4 Prompt 不是静态文案，它还连接着记忆系统</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">12</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.5 真正的控制平面，还要考虑缓存与计算成本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">13</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.6 用户可以覆盖prompt，但不能跳过这套结构</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">13</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.7 为什么说prompt 在这里更像宪法，而不是台词</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">14</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">2.8 从源码里可以提炼出的第二个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">14</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第3 章Query Loop</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">16</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.1 一个代理系统是否成熟，先看它有没有循环</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">16</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.2 状态属于主业务</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">16</span>

---

## Page 4

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.3 Query loop 的第一职责是治理输入</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">18</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.4 调用模型只是循环的一段，不是循环本身</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">19</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.5 心跳必须处理中断，否则它就只是惯性</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">19</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.6 心跳还必须处理恢复，否则它只是脆弱的重复劳动</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">20</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.7 停止条件不能只有一个，否则系统会把失败和完成混为一谈</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">21</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.8 QueryEngine 说明它属于会话生命周期</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">21</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">3.9 从源码里可以提炼出的第三个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">24</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">25</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.1 一旦模型开始调用工具，问题的性质就变了</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">25</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.2 工具调度属于行为宪法的一部分</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">25</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.3 运行一个工具，真正执行前已经发生了很多事</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">26</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.4 权限先于能力：Claude Code 没把模型当有天然授权的人</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">26</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.5 权限结果本身也是一种运行时语义</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">27</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.6 StreamingToolExecutor 说明中断是一等语义</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">27</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.7 Bash 为什么永远比别的工具更可疑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">29</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.8 工具系统真正保护的，不只是用户，还包括系统自己</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">31</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">4.9 从源码里可以提炼出的第四个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">31</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第5 章上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">33</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.1 上下文一多，系统就容易产生一种低级幻觉</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">33</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.2</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#800000"> CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000"> 体系说明，长期指令不能和临场对话混在一起</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">33</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.3</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#800000"> MEMORY.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000"> 是索引，不是日记本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">34</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.4 Session memory 说明，短期连续性也不能靠聊天记录硬扛</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">35</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.5 自动compact 说明，上下文治理首先是预算治理</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">36</span>

---

## Page 5

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.6</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#800000"> compactConversation()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000"> 说明，摘要要重建可继续工作的上下文</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">36</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.7 上下文治理的关键是保留工作语义</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">37</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">5.8 从源码里可以提炼出的第五个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">38</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第6 章错误与恢复</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">39</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.1 工程世界最不值得相信的话，就是“正常情况下”</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">39</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.2</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#800000"> prompt too long</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000"> 是一种必然周期</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">39</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.3 响应式compact 说明，恢复的关键在于别把自己逼进死循环</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">40</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.4</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#800000"> max_output_tokens</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000"> 的处理说明，恢复要以续写为主</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">41</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.5 auto compact 的失败熔断，说明恢复系统自己也要受治理</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">41</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.6 compact 自己也会爆，所以连“修复动作”都需要修复策略</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">42</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.7 abort 语义说明，中断也属于错误恢复的一部分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">42</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.8 错误处理真正保护的，是执行叙事的一致性</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">45</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">6.9 从源码里可以提炼出的第六个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">45</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第7 章多代理与验证</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">47</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.1 单代理走到一定程度，问题就不再是“会不会做”，而是“怎么分工”</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">47</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.2 forked agent 的第一原则是cache‑safe</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">47</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.3 状态隔离说明，子代理首先要减少污染</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">48</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.4 协调者模式说明，synthesis 才是稀缺能力</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">49</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.5 验证必须独立成阶段，否则“实现完成”很快就会冒充“问题解决”</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">49</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.6 hooks 和任务生命周期说明，子代理不是扔出去就算了</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">50</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.7 验证不仅针对代码，也针对记忆和建议</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">52</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.8 多代理真正解决的是不确定性的分区</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">52</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">7.9 从源码里可以提炼出的第七个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">53</span>

---

## Page 6

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第8 章团队落地</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">54</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.1 个人顺手，不代表团队就能稳定复用</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">54</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.2 团队第一步，是先把最低边界做清楚</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">54</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.3</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#800000"> CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000"> 的价值，在于稳定、分层、少争议</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">55</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.4 复用的重点，先是验证定义，再是skill 数量</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">56</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.5 skill 更适合作为工作流模块来理解</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">57</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.6 approval 的重点，是按风险分层</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">57</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.7 hook 是高级能力，通常不必作为第一步</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">58</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.8 可复盘轨迹很重要，但要分清基线层和高阶层</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">59</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">8.9 从源码里可以提炼出的第八个原则</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">60</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">第9 章Harness Engineering 十条原则</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">61</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.1 把模型当不稳定部件，不要当同事</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">61</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.2 Prompt 是控制面的一部分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">61</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.3 Query loop 才是代理系统的心跳</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">61</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.4 工具是受管执行接口</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">62</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.5 上下文是工作内存</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">62</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.6 错误路径就是主路径</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">62</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.7 恢复的目标是继续工作</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">62</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.8 多代理的意义，是把不确定性分区</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">62</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.9 验证必须独立，不能让系统自己给自己打分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">62</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.10 团队制度比个人技巧重要</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">63</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">9.11 最后一句话</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">63</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">附录A 检查清单</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">64</span>

---

## Page 7

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.1 Agent Runtime 设计清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">64</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.2 Prompt 设计清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">64</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.3 Tool 与Permission 设计清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">65</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.4 Context 治理清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">65</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.5 Error Recovery 设计清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">66</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.6 Multi‑Agent 设计清单</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">66</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.7 Team 落地清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">66</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.8 Review 问题单</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">67</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">A.9 最后一个清单</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">67</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">附录B 图示</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">68</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">B.1 图一：Claude Code 总体控制面</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">68</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">B.2 图二：Query Loop 主循环与恢复分支</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">69</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">B.3 图三：Tool Batch Ordering 与StreamingToolExecutor</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">70</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">B.4 图四：Context Sources 与Compact Rebuild</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">70</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">B.5 图五：Coordinator‑Worker Flow 与Verification Separation</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">70</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">B.6 图六：团队治理图</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">70</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#800000">附录C 源码地图</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:11.96pt;font-weight:bold;color:#000000">76</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.1 第1 章为什么需要Harness Engineering</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">76</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.2 第2 章Prompt 是控制面，不是人格装修</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">77</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.3 第3 章Query Loop：代理系统的心跳</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">77</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.4 第4 章工具、权限与中断</span> <span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">77</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.5 第5 章上下文治理：Memory、CLAUDE.md 与Compact</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">78</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.6 第6 章错误与恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">79</span>

---

## Page 8

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.7 第7 章多代理与验证</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">79</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.8 第8 章团队落地</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">80</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">C.9 第9 章十条原则</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">. . . . . . . . . . . . . . . . . . . . . . . . . .</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">80</span>

---

## Page 9

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">导读</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这本书关心的不是“模型会不会写代码”，而是“一个会写代码的模型被放</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">进终端、仓库和团队流程以后，怎样才不会把系统带偏”。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这不是源码注释汇编，也不是产品功能介绍。它关注的是Claude Code 如何把不稳定</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">模型收束进可持续运行的工程秩序，让控制面、主循环、工具权限、上下文治理、恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">路径、多代理验证与团队制度组织成一套完整骨架。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">本书有三个阅读前提：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 重点不在模型能力，而在harness 如何组织约束与执行</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 重点不在函数逐条解释，而在运行时结构为什么必须呈现为这种形态</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 重点不在个人技巧，而在这些结构怎样变成团队可以复用的制度</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">建议阅读顺序：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">1. 序言Harness、终端与工程约束</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">2. 第1 章为什么需要Harness Engineering</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">3. 第2 章Prompt 不是人格，Prompt 是控制平面</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">4. 第3 章Query Loop：代理系统的心跳</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">5. 第4 章工具、权限与中断：为什么代理不能直接碰世界</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">6. 第5 章上下文治理：Memory、CLAUDE.md 与Compact 是预算制度</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">7. 第6 章错误与恢复：出错后仍能继续工作的代理系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">8. 第7 章多代理与验证：用分工和验证管理不稳定性</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">9. 第8 章团队落地：把一个聪明工具变成可复用制度</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">10. 第9 章Harness Engineering 十条原则</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">11. 附录A 检查清单：把原则落成能执行的约束</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">12. 附录B 图示：把运行时骨架画出来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">13. 附录C 源码地图：本书各章主要依据哪些文件</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果只想先看总判断，可以直接跳到第9 章。</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">1</span>

---

## Page 10

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">序言Harness、终端与工程约束</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这些年，人们喜欢把会写代码的模型叫作智能体。这个词带着明显的乐观色彩，仿佛只</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">要模型能读仓库、调工具、写出像样的patch，它就可以在工程环境里独立行动。可工</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">程环境有明确后果。终端、文件系统和Git 历史都不是抽象空间，任何改动都会留下痕</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">迹。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一个只会输出文本的模型，出错时主要带来理解成本。一个能运行命令、写文件、访问</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">网络、修改仓库的模型，出错后留下的是执行结果。目录会变化，进程会中断，配置会</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">损坏，历史会变得难以追踪。到了这一步，核心问题不再是模型是否足够聪明，而是系</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">统是否提供了足够约束。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这本书讨论的，正是这种约束。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">我把它叫作Harness Engineering。这里的harness 可以理解为一组持续生效的控制</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">结构，用来约束模型在工程环境中的行为边界。对AI coding agent 来说，没有约束的</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">能力只会扩大事故半径。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这本书也不是一份Claude Code 源码讲解。源码当然重要，但如果只沿着目录逐个解</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">释，很容易写成注释汇编。那样的内容能说明函数做了什么，却未必能回答系统为什么</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">必须呈现为现在这种结构。要理解Claude Code 这类系统，光知道有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">queryLoop()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compactConversation()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">runTools()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 还不够。更重要的问题是：为什么一个”会</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">写代码”的系统，最终需要prompt 分层、权限判定、状态机、compact、恢复分支、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">subagent 生命周期、verification 阶段和团队制度这些结构？</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">答案并不复杂，因为模型本身并不稳定。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这个判断未必讨喜，但工程系统不能依赖乐观叙事维持。一个部件如果本质上不稳定，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">系统就必须围绕这个事实设计；否则，问题只会在事故复盘里集中出现。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 值得研究，因为它在实现上保持了明确的工程克制：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 没有假定模型会持续正确，因此用query loop 管理状态；</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 没有假定工具调用天然安全，因此用权限和调度约束工具；</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 没有假定上下文越多越好，因此引入memory、CLAUDE.md、compact 和session</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">memory；</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">2</span>

---

## Page 11

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">序言HARNESS</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">3</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 没有把错误视为偶发事件，因此为prompt too long、max output tokens、中</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">断和hook 回环设计恢复路径；</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 没有把多代理直接等同于更强能力，因此把synthesis 和verification 单独拆开，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">避免系统自我背书。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一整套东西，合起来才是agent。模型只是agent 里最会说话、也最不稳定的那个部</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">件。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以这本书有一个始终不变的基本立场：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Prompt 决定它怎么说话，Harness 决定它怎么做事。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这里说的harness，不是一层附属工具，也不是对模型能力的情绪化防御。它是模型进</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">入工程环境的前提。缺少这层约束，风险最终会转移给用户、团队和未来的维护者。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这里也先说明一个边界：本书不会附带Claude Code 源代码，也不会长篇逐段转录源</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">码。原因很简单，就是版权边界。我们能做的，是在合理引用和工程分析的范围内，基</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">于源码结构提炼设计原则、运行机制与方法论判断，而不是把受版权保护的实现文本重</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">新发布一遍。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">本书试图做两件事。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第一件，是基于Claude Code 源码，把真正决定系统可靠性的结构讲清楚。重点在于解</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">释为什么上下文治理必须成为主路径，为什么多代理解决的是职责分区，为什么团队制</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">度必须纳入生命周期节点，而不是简单罗列“这里有compact”“这里有subagent”“这</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">里有hook”。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第二件，是把这些实现背后的判断提炼成更一般的工程原则。具体代码版本会变化，函</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">数名会变化，产品形态也会变化。但只要大家还在尝试把不稳定模型接入真实工作流，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">某些原则就仍然有效。比如：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 错误路径要按主路径设计</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 验证必须进入完成定义</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 权限是系统器官，而不是附属功能</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 上下文是资源，不是垃圾桶</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 多代理要靠角色分离，不靠人海战术</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 团队制度比个人技巧重要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果这些判断成立，那么Claude Code 更适合作为一份样本。它的价值不在于教人复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">制一套一模一样的CLI，而在于展示一个面向真实工程环境的AI 代理，最终会如何走</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">向更严格的约束结构。</span>

---

## Page 12

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">序言HARNESS</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">4</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更直接地说，这本书不讨论如何用模型包装出一个”像工程师”的幻觉，而讨论如何在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">模型并不具备工程师稳定性的前提下，仍然构造出一个可运行的工程系统。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这类工作通常不显眼。回滚、审批、权限、验证、compact、清理孤儿进程都不显眼，但</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">系统能否长期稳定运行，往往取决于这些部分。过度追求”像人一样自然”的代理体验，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">其常见结果是系统具备了类似人的失误模式，却没有承担后果的能力。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">既然如此，就从约束谈起。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">接下来九章，讨论的都是这套Harness 结构如何形成，为什么必须采用这种安排，以</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">及一个团队如何把个人经验沉淀成可以复用的工程制度。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">@wquguru</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">2026.04.01</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 源码泄漏的愚人节</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">btw. 您可以在</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#800000">harness‑books.agentway.dev/book1‑claude‑code</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 访问在线版，获取</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更好的阅读体验</span>

---

## Page 13

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">第1 章为什么需要Harness</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">Engineering</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.1 问题在于让模型别乱来</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这些年，人们很喜欢谈智能体。这个词常常带着轻快的预期，仿佛只要模型会写几段代</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">码、会调几个工具，就可以像见习工程师一样在终端里独立工作。可终端和文件系统都</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">带有明确后果。一个会说话的概率分布，一旦能接触shell、Git、网络和本地文件，问</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">题就从”回答得不够好”变成”执行造成实际破坏”。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以问题的重点，一直是怎样把它约束成一个可管理的系统。所谓Harness Engineer‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">ing，讨论的就是这件事。Harness 是一整套制度化的控制平面，用来处理一个很现实</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的问题：模型并不天然值得信任。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这个判断未必轻松，但通常有用。一个代理系统要进入真实工程环境，首先要承认自己</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的核心部件是不稳定的。忽视这一点，问题最后通常会在日志和事故记录里出现。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.2 Claude Code 的第一层Harness：受约束的会话系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果只看表面，Claude Code 像一个能和用户对话、还能顺手改代码的CLI。可从实现</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">上看，它一开始就没有把自己当成“裸模型接口”来设计，而是当成一个带有上下文边</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">界、运行时状态和行为规约的会话系统。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一点从system prompt 的组织方式就能看出来。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:175</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始，系统先定义身份和总任务。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:186</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始，补上关于工具、权限、系统提醒和</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">上下文压缩的系统级说明。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:199</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始，再补上做任务时的工程约束，比如</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">不要越权改动、不要把验证说成已经完成、不要为了省事发明抽象。</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">5</span>

---

## Page 14

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第1 章为什么需要HARNESS ENGINEERING</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">6</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这里值得停一下。很多人谈prompt，还停留在“你是一个什么样的助手”这种修辞层</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">面。Claude Code 在实现上把prompt 放进了运行时控制结构里，这些文字用来规定</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">执行边界、失败行为和报告责任。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更重要的是，这个prompt 采用分段拼装方式。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:444</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">getSystemPrompt()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，静态部分和动态部分被明确拆开，memory、lan‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">guage、output style、MCP instructions、scratchpad 等内容按段注入。到了</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/utils/systemPrompt.ts:28</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统又把默认prompt、自定义prompt、agent</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">prompt 和append prompt 组织成一套优先级规则。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这说明了一个朴素的工程事实：一个真正可用的代理系统，不能依赖一段“万能提示词”</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">解决所有问题。它必须把控制拆成层，把层次拆成职责。否则，新增提醒和禁令很快就</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">会互相冲突，系统行为也会变得难以预测。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.3 第二层Harness：代理依赖持续循环</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果说prompt 规定了它应该成为什么样的东西，那么query loop 规定了它实际上如</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">何运行。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的核心不在某个单独的API 调用，而在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:219</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始的</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，以及</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:241</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">queryLoop()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。这段实现里最重要的一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">点，是它明确承认代理系统依赖带状态的多轮执行。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:268</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统把</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">messages</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">toolUseContext</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">autoCompactTrack-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ing</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">maxOutputTokensRecoveryCount</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">hasAttemptedReactiveCompact</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">pend-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ingToolUseSummary</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">turnCount</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">transition</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 等内容放进同一个跨迭代状态里。一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">个会话系统一旦这样设计，就等于正式承认：上一轮留下的问题会进入下一轮，系统必</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">须有能力继续处理。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这是Harness 思维的核心。真正的问题在于系统能不能在连续多轮里保持行为一致：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 有没有预算概念</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 有没有恢复概念</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 有没有上下文膨胀后的自救机制</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 有没有在工具调用失败后继续推进任务的能力</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">缺少这些结构，所谓智能体就只是一个不稳定的执行者。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:365</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，这个循环还会在每轮调用前处理消息裁剪、tool result</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">budget、history snip、microcompact、context collapse、autocompact 等内容。</span>

---

## Page 15

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第1 章为什么需要HARNESS ENGINEERING</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">7</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">实现细节虽然很多，但共同指向一点：Claude Code 在调用发生前就尽量把控制权收回</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到运行时一侧。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这也是为什么Harness Engineering 不能被看作prompt engineering 的附属品。前</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">者关心状态机，后者关心措辞。措辞当然重要，但状态机决定系统行为最终由谁负责。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.4 第三层Harness：工具调用必须服从调度</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一个模型如果只能输出文本，顶多让人觉得它有时说得太满。可一旦它能调用工具，系</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">统风险就立刻从修辞风险变成执行风险。这时候最重要的问题是：谁决定工具怎么跑。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 给出的答案很直接。运行时会根据工具属性决定并发还是串行。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/toolOrchestration.ts:19</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">runTools()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，工具调用</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">先经过</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">partitionToolCalls()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 分组。到了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/toolOrchestration.ts:91</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">系统会读取工具schema，并调用</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">isConcurrencySafe()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 判断一个工具是否适合并发</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">执行。能并发的归成一批，不能并发的按顺序一个个来。并发路径里，context modifier 会</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">先缓存，再按原始block 顺序回放，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/toolOrchestration.ts:31</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:63</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事很有代表性。它说明Claude Code 没有把工具当成模型能力的自然延伸，而是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">当成需要调度纪律的受管执行单元。缺少调度纪律的工具系统，只会把模型的不稳定性</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">放大到外部世界。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">并发如果不受约束，就会扩大事故半径。Claude Code 在这里采取了偏保守的策略。在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">会碰到文件、终端和权限的场景里，这种保守通常更可靠。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.5 第四层Harness：最危险的工具，必须配最细的规矩</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在所有工具里，Bash 最值得警惕。因为它几乎不受领域边界约束，可以直接接触文件、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">进程、网络和Git 仓库，还会带上重定向、管道等复杂shell 语义。一个系统如果对Bash</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">过度信任，后果通常会很具体。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 对Bash 的态度，可以在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/tools/BashTool/prompt.ts:42</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">看得很清楚。这里写了一整段操作规约，尤其是围绕git 和PR 的那部分：不要乱改</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">git config，不要跳过hooks，不要随手</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">git add .</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，不要在pre‑commit 失败后用</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">--amend</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把上一条提交也搭进去，不要在没有明确要求时提交，更不要默认push。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">写到这个地步，有人会觉得它过于细碎。但高风险接口通常就需要高密度约束。Bash 一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">旦进入真实工作流，很多规则都必须明确写出来。</span>

---

## Page 16

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第1 章为什么需要HARNESS ENGINEERING</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">8</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Harness Engineering 的一个重要原则，就是把高风险能力包装成高约束能力。能力</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">越强，控制越细。原因很简单：外部世界不会因为模型语气坚定，就自动原谅一次错误</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">执行。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.6 第五层Harness：错误属于主路径的一部分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多软件把失败路径看成例外，把成功路径看成正文。代理系统不能这样做。因为代理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">系统的失败不是偶发性的，它是一种稳定存在。模型会超token，会触发</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">prompt too</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">long</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，会撞上</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">max_output_tokens</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，还会遇到工具拒绝、用户打断、hook 阻塞、API</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">重试等各种中断。要是这些情况都只在最后用几个</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">catch</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 打发一下，那系统表面上在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">运行，实际上只是不断把麻烦往后滚。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在query loop 里没有这样处理。光看</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:453</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后关于</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">autocompact 的处理，以及</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:592</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后对上下文上限和阻断逻辑的注</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">释，就能看出它把失败当作会持续发生的结构性条件来处理。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这也是Harness 和普通助手的重要差别之一。普通助手常见的设计逻辑是先回答，错</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">了再道歉。Harness 更强调先约束，再执行；即使出错，也要按恢复路径处理，而不是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">靠临场发挥补救。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一个会道歉的系统，不一定成熟。一个知道何时不该开始、何时该重试、何时该中止、何</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">时该准确汇报失败的系统，才更接近成熟。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">1.7 从源码里可以提炼出的第一个原则</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到这里，第一章其实只想说一件事：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">代理系统的关键能力是约束执行。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的源码在几个关键位置都指向同一个结论：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> constants/prompts.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 说明prompt 是控制平面的一部分，而不是人格装饰</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> utils/systemPrompt.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 说明系统行为必须有清楚的分层优先级</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 说明代理运行依赖持续的循环状态，而不是单次问答</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> services/tools/toolOrchestration.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 说明工具调用必须服从调度纪律</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> tools/BashTool/prompt.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 说明高风险工具必须伴随高密度约束</span>

---

## Page 17

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第1 章为什么需要HARNESS ENGINEERING</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">9</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">把这些放在一起看，就会发现Harness Engineering 并不神秘。它只是坚持几条常被</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">忽视的工程常识：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 模型会犯错</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 工具会扩大错误后果</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 上下文会膨胀</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 状态会污染下一轮</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 用户会打断你</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 失败会反复出现</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">既然如此，系统就不能靠“聪明”维持秩序，只能靠结构维持秩序。结构不像聪明那样</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">显眼，但通常更可靠。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下一章要谈的，是这套结构里最容易被误解的一层：system prompt。很多人把它看成</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">人设文本，本书会说明它更接近操作系统里的规章制度。人设可以改善观感，规章才能</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">约束机器。</span>

---

## Page 18

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">第2 章Prompt 不是人格，Prompt 是</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">控制平面</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.1 把prompt 当成人设，是一种常见误会</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多人一说起system prompt，首先想到的是一段熟悉的话术：你是谁，你擅长什么，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">你应该温柔、专业、简洁，最好再有一点稳定的人格。对于只负责聊天的系统，这种理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">解倒也够用；但对一个要读文件、调工具、动shell、处理权限、跨轮执行的代理系统来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">说，这种理解明显不够。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">原因很简单。人设描述解决的是“它像什么”，控制平面解决的是“它能做什么、什么时</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">候做、做错了怎么办、谁来兜底”。两者不在同一层。一个系统可以有讨人喜欢的人设，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">同时在执行层面缺少规矩。那种系统出事时往往会显得很真诚，因为它很会道歉。但道</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">歉并不能替代运行时设计。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的实现恰好说明了这一点。它的system prompt 是一组分层拼装的行为</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">区块。换句话说，这里的prompt 更接近一套运行时协议，而不是一篇人物小传。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.2 从源码看，Claude Code 的prompt 从一开始就是分</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">层的</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:444</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">getSystemPrompt()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，Claude Code 返</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">回的是一个由多个section 组成的数组，而不是一段完整字符串。这个细节很重要。因</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">为一旦prompt 变成多个块，系统就正式承认它内部包含一组职责不同的约束。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这些section 至少包含几类东西。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">首先是身份和总任务说明。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:175</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统说明自己是一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">个交互式代理，要用可用工具帮助用户完成软件工程任务。这里同时嵌入了一些安全约</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">束，比如不要乱猜URL。</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">10</span>

---

## Page 19

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第2 章PROMPT 不是人格</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">11</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">然后是系统级规则。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:186</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始，系统明确规定：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 用户能看见的是哪些文本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 工具调用可能触发权限审批</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 用户拒绝后不能机械重试</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ tool result 和user message 里可能混入system‑reminder</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 上下文会被自动压缩</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这些内容有一个显著特征：它们并不关心模型“像不像一个聪明助手”，而是关心它是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">否是一个守规矩的执行体。这就是控制平面的语气，它的核心任务是定义边界。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">再往下，在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:199</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始，是做任务时的工程性指令：不要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">随意增加需求，不要越权优化，不要为了让结果显得完整而隐瞒验证失败，不要在没有</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">必要时制造抽象。这些内容看起来像写作风格要求，其实它们和工程约束绑得很紧。一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">个会自动“顺手优化一切”的模型，从产品角度看也许很热情，从工程角度看则相当危</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">险。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以，从源码结构上就能看出来：Claude Code 的prompt 要解决的是如何让模型在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">复杂运行时里遵守边界。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.3 Prompt 的真正价值，不在文字本身，而在优先级</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果prompt 只是写在那里，还不够说明问题。真正决定它是否属于控制平面的，是系</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">统是否给它定义了严格优先级。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一点可以看</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/utils/systemPrompt.ts:28</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">buildEffectiveSystem-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Prompt()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。这段代码把prompt 的来源明确排成一条链：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">1. override system prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">2. coordinator system prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">3. agent system prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">4. custom system prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">5. default system prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">最后还会统一拼接</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">appendSystemPrompt</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这个设计很说明问题。它表明Claude Code 并不相信“默认prompt 一劳永逸”。相反，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">它承认系统里存在多种语境：</span>

---

## Page 20

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第2 章PROMPT 不是人格</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">12</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 协调者模式需要自己的系统行为</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ agent 模式需要自己的职责说明</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 用户可以通过CLI 覆盖或追加prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 默认prompt 只是没有更高优先级时的基线</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更朴素地说，成熟系统不会迷信唯一版本的prompt。它会把prompt 看成一个有层级</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的配置系统，让不同职责在不同上下文里生效。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这里还有一个很值得注意的细节。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/utils/systemPrompt.ts:99</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">对proactive mode 做了特殊处理：如果agent prompt 和proactive mode 同时存在，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">agent prompt 不再替换默认prompt，而是附加在默认prompt 之后。这个决定本身</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">就很说明问题。它意味着系统知道，有时候默认约束不能丢，新增agent 只能在默认约</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">束之上叠加领域行为，而不能把整套纪律换掉。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">可以把它理解为一套通用制度外加岗位说明书。岗位说明书可以补充职责，但不能直接</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">冲掉底层制度，否则系统很快就会各自为政。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.4 Prompt 不是静态文案，它还连接着记忆系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果说前面这些内容已经像一套运行时说明书，那么看到Claude Code 如何处理mem‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">ory 和</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 后，就会更清楚地意识到：这里的prompt 已经是整个上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">入口，而不只是“写给模型看的一段话”。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/utils/claudemd.ts:1153</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">getClaudeMds()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，系统会把project in‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">structions、local instructions、team memory、auto memory 等不同来源的内容</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">整理成统一格式，再拼接进prompt 相关上下文中。这里连每种内容的来源说明都写得</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很细，比如这是项目级指令、用户私有项目指令、共享team memory，还是跨会话持</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">久化的auto memory。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">而在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/memdir/memdir.ts:187</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">buildMemoryLines()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，系统连“如何</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">保存记忆”这件事都变成了prompt 的一部分。它会明确告诉模型：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ memory 是文件化持久系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> MEMORY.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 是索引，不是正文</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 要如何写frontmatter</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 哪些信息不该保存</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ plan 和task 不该被误用成memory</span>

---

## Page 21

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第2 章PROMPT 不是人格</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">13</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事非常关键。它把prompt 的职责从“约束当前行为”扩展到了“约束未来知识的</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">沉淀方式”。这已经超出了通常意义上的提示词，更接近一份写给运行时参与者的知识</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">治理协议。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">换句话说，Claude Code 不只是用prompt 规定“这一轮怎么说话”，还用prompt 规</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">定“长期记忆如何形成”。一个系统只要走到这一步，它的prompt 就不可能再只是语</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">气问题，而必然进入制度问题。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.5 真正的控制平面，还要考虑缓存与计算成本</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">多数人理解prompt 时，很少会想到性能。常见想法是prompt 只是喂给模型的文本，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">写好即可。Claude Code 的实现更务实：prompt 同时也是计算成本。它越复杂、变化</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">越频繁，缓存命中就越差，系统运行就越贵、越慢。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/systemPromptSections.ts:16</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统把prompt section</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">区分成两类：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 可缓存的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">systemPromptSection</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 会打破缓存的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">DANGEROUS_uncachedSystemPromptSection</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">而</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">resolveSystemPromptSections()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 会优先从缓存里拿已经计算过的内容，只在必</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">要时重算。到了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">clearSystemPromptSections()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统又会在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">/clear</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 或</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">/compact</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">之后清空这些状态。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事看起来像优化，实际上同样属于控制平面。一个真正可运行的prompt 系</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">统，不可能只考虑表达能力，而不考虑它对吞吐、延迟和缓存的影响。Claude Code</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">getSystemPrompt()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里甚至把静态部分和动态部分用boundary 显式分开，见</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/constants/prompts.ts:560</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后。这说明它在设计时已经承认：有些内容在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">会话中相对稳定，有些内容会逐轮变化，二者不能混在一起消耗缓存。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一个工程系统只要开始关心“哪部分prompt 会导致缓存失效”，它就已经不再把</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">prompt 当作文案创作。文案追求完整表达，控制平面追求可治理、可复用、可预测</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的行为成本。两者关注的问题不同。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.6 用户可以覆盖prompt，但不能跳过这套结构</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 并没有把用户锁死在默认prompt 上。相反，CLI 明确支持覆盖和追加。</span>

---

## Page 22

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第2 章PROMPT 不是人格</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">14</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/main.tsx:1342</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统处理</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">--system-prompt</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">--system-prompt-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">file</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">--append-system-prompt</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">--append-system-prompt-file</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 这些选项。也</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">就是说，用户当然可以带着自己的规约来。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">但这里有个关键点。系统虽然允许覆盖和追加，却仍然坚持用统一的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">buildEffec-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tiveSystemPrompt()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 做最终装配。这说明它允许自定义，但不放弃秩序。用户可以</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">改内容，系统仍然保留结构。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">没有结构的可定制，最后往往会退化成另一种随意。今天加一段，明天减一段，后天某</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">个agent 又替换掉基线约束，系统行为就会越来越像临时口头通知。Claude Code 的</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">选择是让用户修改，但修改必须发生在既定优先级和分层机制里。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.7 为什么说prompt 在这里更像宪法，而不是台词</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果把前面各节放在一起看，可以得到一个相当明确的结论：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的prompt 更像宪法。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所谓台词，是给角色在场上说的；所谓宪法，是规定权力边界、责任关系和例外情况如</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">何处理。Claude Code 的prompt 更接近后者，因为它满足了几个结构条件：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它分层，而不是一块写到底</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它有优先级，而不是谁后写谁说了算</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它与memory、CLAUDE.md、agent instructions、MCP instructions 一起组</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">成完整控制平面</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它有缓存和动态section 机制，不是随手拼一段文本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它和runtime 紧密耦合，而不是游离于系统之外的装饰物</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这也是为什么“写一个好prompt”单独拿出来时价值有限。更重要的问题是：prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在系统里处于什么位置，它和哪些模块配合，它是否参与权限、状态、上下文和长期记</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">忆的治理。如果不回答这些问题，所谓好prompt 往往只是在某个顺利场景里暂时成立。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">2.8 从源码里可以提炼出的第二个原则</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一章最后可以归纳成一句话：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Prompt 的价值，在于它是否被纳入一套清楚的控制结构。</span>

---

## Page 23

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第2 章PROMPT 不是人格</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">15</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的源码在几个地方共同证明了这一点：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> constants/prompts.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把prompt 写成分段控制结构，而不是一段统一宣言</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> utils/systemPrompt.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 明确规定了prompt 来源的优先级</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> utils/claudemd.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把项目级和长期记忆内容纳入上下文装配</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> memdir/memdir.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 用prompt 规定了长期记忆的保存规则</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> constants/systemPromptSections.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 则把prompt 进一步变成可缓存、可</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">失效、可按段重算的运行时对象</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以，一个成熟代理系统里的prompt，不该被理解成“让模型入戏的开场白”。它更像</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一套运行中的制度文本。制度文本当然也可以写得清楚，但最重要的部分始终是约束力。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下一章要讨论的，是另一根更硬的骨头：query loop。因为再好的控制平面，最后都要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">落到执行循环里。prompt 规定边界，循环决定命运。一个系统最终会成为什么样子，往</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">往体现在它每一轮如何继续、如何中断、如何恢复的那套状态机里。</span>

---

## Page 24

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">第3 章Query Loop：代理系统的心跳</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.1 一个代理系统是否成熟，先看它有没有循环</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果把一个会写代码的模型看成代理系统，最容易犯的错误，就是把它想象成一个加强</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">版问答接口。用户发来一句话，模型输出一个结果，事情就算办完。这种想法并非毫无</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">来由，因为很多大模型产品确实这样工作。但只要系统开始调用工具、跨轮执行、处理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">中断、保存状态、重试失败、压缩上下文，这种“一问一答”的理解就会迅速失效。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的实现没有犯这个错误。它从结构上明确承认：代理依赖一段持续的、有</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">状态的执行过程。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一点在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:219</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 和</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:241</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">queryLoop()</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">里表现得很明显。前者只是壳，真正重要的是后者。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">queryLoop()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 不是把模型调用包</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在一个</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">try/catch</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里就结束。它维护了一套跨迭代状态，先处理一系列前置治理动作，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">然后进入模型流式阶段；等模型返回后，再决定是进入工具执行、恢复、压缩、继续下</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一轮，还是直接终止。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这意味着Claude Code 的核心是维持一个会话内的执行秩序。这里的关键名词是life‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">cycle。一个系统是否能被称为agent，往往不取决于它会不会说，而取决于它能不能在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">几轮之后仍然知道自己在做什么。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.2 状态属于主业务</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多系统在设计之初，都倾向于把状态看成包袱，仿佛无状态才更优雅。对代理系统来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">说，这种偏好作用有限。只要它进入真实工作流，状态就会自然出现。忽视状态，并不</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">能消除状态，只会让它以更难管理的方式返回。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在这里的态度很直接。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:203</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:217</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统把query</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">loop 的可变状态定义得很清楚：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> messages</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">16</span>

---

## Page 25

Chapter 3·Query Loop Core

mble loop stati

Call model and stream

nitted?

Plan toolworl

Execute tools
Merge tool_result and

Apply coet / retry branch  Apper  er/systemeffect

<!-- 图源: ./book1-claude-code_assets/images/p025_scan.jpg -->


<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">17</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">图1: Claude Code Query Loop Core</span>

---

## Page 26

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">18</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> toolUseContext</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> autoCompactTracking</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> maxOutputTokensRecoveryCount</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> hasAttemptedReactiveCompact</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> pendingToolUseSummary</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> stopHookActive</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> turnCount</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> transition</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:268</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，这些状态在每次query loop 启动时被整体装配成一个</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">State</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 对象，并在后续各个continue 分支里整体更新。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一点很重要。Claude Code 没有把恢复、压缩、预算、hook、turn 计数散落在局部</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">变量和布尔开关里，而是承认它们共同构成了“本轮结束后下一轮如何继续”的基础。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">它把状态当作心跳的一部分。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这就是成熟代理系统和一次性脚本的区别。脚本只关心这一步有没有跑完，代理系统还</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">要关心：这一步失败之后，下一步能不能继续承接前面留下的状态。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.3 Query loop 的第一职责是治理输入</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从外部看代理系统，很多人会以为它的核心动作是“调用模型”。但在工程上，真正重要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的常常是模型调用之前那一长串整理工作。Claude Code 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">queryLoop()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里把这件事</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">写得很清楚。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在正式进入模型流之前，系统会先做这些事：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 启动相关memory 的预取，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:297</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 预取skill discovery，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:323</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 截取compact boundary 之后的有效消息，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:365</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 应用tool result budget，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:369</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 进行history snip，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:396</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 进行microcompact，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:412</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 进行context collapse，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:428</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 最后才尝试autocompact，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:453</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这串顺序本身就是一种架构声明。它告诉读者，Claude Code 把“上下文治理”放在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">“模型推理”之前。也就是说，它不把从混乱中整理秩序的责任交给模型，而是先由运行</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">时完成治理，再把更干净的输入交给模型。</span>

---

## Page 27

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">19</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事很重要，因为很多系统恰恰相反：先把大量上下文塞进去，再寄希望于模型自己</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">判断什么重要，什么不重要。那种做法看似省事，实际上是在把运行时应承担的责任转</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">嫁给概率分布。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的做法更接近传统工程流程：先整理现场，再开始执行。它不追求潇洒，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">但通常更稳妥。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.4 调用模型只是循环的一段，不是循环本身</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">等前面的治理工作都做完，Claude Code 才进入模型调用阶段。这个阶段出现在</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:652</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后。这里有个值得专门指出的细节：系统会进入</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">for await</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 流</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">式消费模型输出，而不是同步拿一个完整结果回来。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这意味着模型输出在Claude Code 里是一串事件流，而不只是“最终答案”。事件里可</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">能包含：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ assistant 文本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> block</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ usage 更新</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ stop reason</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ API 错误</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一点在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:826</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后尤其明显。系统会把assistant message 存起来，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">提取其中的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> block，决定是否需要follow‑up，还可能边流边把工具送给</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">StreamingToolExecutor</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从工程角度看，这是一种根本性的变化。一旦把模型输出当成事件流，系统架构就不再</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">只是“请求‑响应”，而更像“驱动‑调度‑反馈”的过程。流式输出的意义，也不只是更早</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">看到几个字，而是允许运行时在模型尚未完全结束之前，就开始安排下一步执行。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这也是为什么前面说query loop 才是代理系统的心跳，而不是模型调用本身。模型调</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">用只是心跳中的一次收缩，真正维持系统运行的是整套循环：输入如何收进来，流如何</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">消费，工具如何调度，失败如何恢复，何时继续下一轮。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.5 心跳必须处理中断，否则它就只是惯性</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一个真正的心跳，不只是能持续跳动，还必须能在必要时停下来。停不下来，系统就只</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">剩惯性。</span>

---

## Page 28

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">20</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 对中断的处理写得很实在。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:1011</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统会优先</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">处理streaming abort。如果启用了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">streamingToolExecutor</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，就必须先消费剩余结</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">果，生成synthetic tool_result，避免已经发出的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 没有配套结果；否则，就</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">用</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">yieldMissingToolResultBlocks()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 主动补全中断说明。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这背后有一个很基础的工程原则：只要系统向外承诺了一段执行，就要在中断时把账补</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">平。不能因为用户打断了，就假装前面的几个</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 从未发生。外部系统、UI 和</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">transcript 都需要一致的因果链，哪怕结果是“中断了”，也必须中断得完整。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事之所以重要，是因为代理系统一旦进入多工具、多轮次状态，外部世界对它的要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">求就不只是“有没有最终答案”，而是“它留下的轨迹能不能被解释”。不能解释的执行</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">轨迹，迟早会变成运维问题、审计问题，或者变成团队里谁也说不清楚的长期隐患。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以，处理中断是runtime 的基本责任。已经开始的动作需要有交代，哪怕交代的是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">“没做完”。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.6 心跳还必须处理恢复，否则它只是脆弱的重复劳动</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果说中断是外部世界打进来的意外，那么恢复就是系统内部预留的余量。没有恢复能</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">力的循环，不管表面多整洁，最后都会暴露出同一个问题：它把幸运当成了设计。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 对恢复的处理是层层递进的，而不是简单重试。最典型的是prompt‑too‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">long 和max‑output‑tokens。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:1065</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统会先判断最后一条assistant message 是否是被</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">withheld 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">prompt too long</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。如果是，先试图让context collapse 把积压的collapse</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">提交出去（见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1086</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1116</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">）；如果还不够，再进入reactive compact（见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1119</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1166</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">）。换句话说，系统会按成本和破坏性从低到高，逐层尝试恢复。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">对</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">max_output_tokens</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的处理也一样。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:1185</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统先尝试</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">提升token cap；如果还不行，再生成一条meta message，让模型从被截断处继续往</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下做，而不是先道歉、先总结、先写一段漂亮的空话。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这很能说明Claude Code 的设计态度。它把恢复看成运行时主路径的一部分，而不是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">模型失败后的礼貌动作。恢复的意义，在于给系统一个继续工作的机会。在真实工程里，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">继续工作通常比维持表面上的礼貌更重要。</span>

---

## Page 29

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">21</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.7 停止条件不能只有一个，否则系统会把失败和完成混为</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">一谈</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">普通问答系统的停止条件比较简单：有回答就结束。代理系统不能这么偷懒。因为一个</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">会话里，出现“当前轮结束”并不等于“任务完成”，更不等于“系统成功”。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的query loop 至少区分了这些情况：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ streaming 正常完成但有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，需要follow‑up</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 没有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，进入stop hooks 和可能的后续判定</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 被用户中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 触发prompt‑too‑long 恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 触发max‑output‑tokens 恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ stop hook 阻塞导致重进循环</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ API 错误直接返回</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这可以从</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:1062</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后一直看到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1305</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。尤其是stop hooks 那段，在</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1267</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:1305</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统不仅处理hook，还专门防止“compact 后仍然太长，再被hook</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">阻塞，再继续compact”的死循环。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这个地方很值得注意。许多系统只有一种朴素想法：失败了就重试。Claude Code 则承</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">认，重试本身也是一种需要被管理的行为。系统必须知道为什么重试、已经试过什么、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">哪些保护状态不能被重置、哪些情况会导致无限循环。正是这些判断，把一个“会继续</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">试”的系统和一个“知道什么时候不该再试”的系统区分开了。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.8 QueryEngine 说明它属于会话生命周期</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">queryLoop()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 还不足以说明问题，那么</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">QueryEngine</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的存在就更直接了。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/QueryEngine.ts:176</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开始，源码明确写着：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">QueryEngine owns the query lifecycle and session state for a conver‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">sation.</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这句话已经把整章的重点说得很明确。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">QueryEngine</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 管理的是一个conversation 的</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">query lifecycle，而不是某一次调用。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/QueryEngine.ts:180</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 还专门说明：一个</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">QueryEngine</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 对应一个conversation，每次</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">submitMessage()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 都是在同一个con‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">versation 里开启新一轮turn，状态会持续保存。</span>

---

## Page 30

Chapter3·QueryEngineTurnFlow

submitMessage(...)

Normalizeinput and attachments

Build system/ user/ tool context

Invoke query(.)

Consume streamed query events

Persist transcript and replay markers
→
Update SDK-facing session state
QueryEngine wraps one turn
around thelower-level
query loop.

<!-- 图源: ./book1-claude-code_assets/images/p030_scan.jpg -->


<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">22</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">图2: Claude Code QueryEngine Turn Flow</span>

---

## Page 31

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">23</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">图3: Claude Code QueryEngine State Carry‑Over</span>

![](./book1-claude-code_assets/images/p031_img01.png)

---

## Page 32

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第3 章QUERY LOOP</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">24</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/QueryEngine.ts:675</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">QueryEngine</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把准备好的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">messages</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">sys-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">temPrompt</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">userContext</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">systemContext</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">toolUseContext</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 一起交给</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">再把assistant、user、compact boundary 等消息写回transcript。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这说明query loop 是会话系统真正的执行中心。外层的UI、SDK、session persistence</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">都围着它转。要理解Claude Code 的设计，不能只看它有哪些工具，也不能只看它</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">prompt 写了什么，最终还是得看这个循环如何把前面的约束落实成连续行为。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">3.9 从源码里可以提炼出的第三个原则</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一章最后可以收敛成一句话：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">代理系统的核心能力，是维持可恢复的执行循环。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的源码在几个关键点共同支持这个判断：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 用显式</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">State</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 管理跨轮执行状态，而不是把一切寄托在局部变量上</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 模型调用前有大段输入治理逻辑，说明运行时先于推理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 流式消费把模型输出当事件流，而不是当最终文案</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 中断路径会补齐synthetic tool_result，说明系统关心因果闭环</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ prompt‑too‑long、max‑output‑tokens、stop hooks 都走明确恢复分支，说明</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">失败是主路径的一部分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> QueryEngine.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 明确把query lifecycle 当作conversation 的所有权对象</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这意味着一个成熟agent 的“心跳”至少要满足几个条件：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它有明确的跨轮状态</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它能治理输入，而不只是被动消费输入</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它能流式地承接模型输出</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它能补齐中断后的执行账本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 它能区分完成、失败、恢复和继续</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">缺少这些结构的系统，也许仍然能做出漂亮demo，但它们更接近一次性表演，而不是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">运行时。表演当然有价值，只是不能替代秩序。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下一章要讨论的，是这套心跳最直接碰到外部世界的地方：工具、权限与中断。前面这</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一章解释了循环为什么存在，下一章要继续说明，循环一旦拥有工具，为什么必须学会</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">克制。</span>

---

## Page 33

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">第4 章工具、权限与中断：为什么代理不</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">能直接碰世界</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.1 一旦模型开始调用工具，问题的性质就变了</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">只会输出文本的模型，出错时主要增加沟通成本。它说错了，可以不信；它总结得糟，可</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">以重问。可一旦模型开始调用工具，问题的性质就变了。因为工具不是意见，工具是动</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">作。动作会留下结果，结果会接触真实世界。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事在shell 上最容易看清。一个模型如果把一段解释写错了，影响通常还停留在理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">解层面；可要是它运行了一条不该运行的命令，文件会被删掉，进程会被中止，Git 历</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">史也会变得难以收拾。能力增强往往伴随后果增强。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以，工具系统最重要的问题是：谁来约束这些工具。Claude Code 对这个问题的回答，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">是把工具变成受管执行接口，避免让模型直接伸手去碰世界。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.2 工具调度属于行为宪法的一部分</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在</span> <span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/toolOrchestration.ts:19</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的</span> <span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">run-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Tools()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，先做了一件很有代表性的事情：不直接执行一串</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，而是先按并</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">发安全性分批。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/toolOrchestration.ts:91</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">partitionTool-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Calls()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，系统会先读取工具的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">inputSchema</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，再调用</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">isConcurrencySafe()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 判</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">断这类调用是否适合并发。如果适合，就把它们归入并发批次；如果不适合，就拆成串</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">行单元。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这看上去像性能优化，实际更接近一致性设计。一个工具系统一旦允许并发，就必须回</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">答一个老问题：上下文变化由谁决定、按什么顺序生效。Claude Code 在并发路径里没</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">有让最先完成的工具抢先改上下文，而是在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">toolOrchestration.ts:31</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:63</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 先缓</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">25</span>

---

## Page 34

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">26</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">存</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">contextModifier</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，再按原始block 顺序回放。也就是说，即便执行是并发的，语</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">义上的上下文演化仍然保持确定顺序。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这是一种典型的工程保守。它的前提是：并发可以提高吞吐，但不能破坏因果秩序。工</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">具如果只会跑得更快，却不能保证上下文一致性，就会替系统制造另一种随机性。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">成熟代理系统不会迷信并发。它会把并发当成需要证明自身无害的例外，而不是默认自</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">由。Claude Code 在这里显然把问题扩散速度考虑得很充分。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.3 运行一个工具，真正执行前已经发生了很多事</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多人以为</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 一旦出现，下一步自然就是执行。Claude Code 的实现说明，真</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">正靠谱的系统不会这么草率。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/toolExecution.ts:30</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 之后，</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">runToolUse()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 所依赖的执</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">行逻辑，已经把permission、hooks、telemetry、synthetic error materialization 等</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">能力接进来了。即使不追每个细节，只看整体结构也能发现：工具执行在Claude Code</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">里是一段完整的流程，包含：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 前置校验</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 执行中事件</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 执行后修正</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 失败补偿</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这说明工具在这里的地位，和普通库函数并不一样。库函数默认属于程序内部，调用者</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">自己承担后果；工具则属于模型与外部世界之间的接口，所以系统不能假设调用者具备</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">稳定判断。换句话说，工具执行周围之所以需要这么多包裹层，是因为调用者本身就是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">最不稳定的变量。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从设计哲学上说，这一点很重要：工具不应该被建模为“模型能力的延长线”，而应该被</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">建模为“需要运行时代为管理风险的外部能力”。一旦接受这一点，permission、hooks、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">interrupt、synthetic result 这些结构就更像常识，而不是负担。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.4 权限先于能力：Claude Code 没把模型当有天然授权的</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">人</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的权限入口，在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/hooks/useCanUseTool.tsx:27</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">CanUse-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ToolFn</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的存在本身已经说明一件事：工具是否允许执行，并不由模型自己说了算，而</span>

---

## Page 35

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">27</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">要交给权限判定链。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">useCanUseTool()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，系统不会因为模型提出了一个工具请求，就默认执行。相</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">反，它会先调用</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">hasPermissionsToUseTool(...)</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 做权限判定，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">useCanUse-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Tool.tsx:37</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。返回结果会分成</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">allow</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">deny</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 或</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ask</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。这一点看上去平常，其实很</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">关键。因为真正成熟的权限系统，除了“能”和“不能”，还要承认第三种状态：系统自</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">己也不该替用户做决定。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">useCanUseTool.tsx:64</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，这条链继续分出不同路径：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> deny</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">：直接拒绝</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> ask</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">：进入协调器、swarm worker、classifier 或交互式审批路径</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> allow</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">：才真正放行</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这意味着Claude Code 从结构上否认了一种常见且危险的想法：模型懂了用户意图，就</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">等于它有权代替用户执行。事实并非如此。理解意图不等于拥有授权，更不等于拥有持</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">续授权。系统必须把“会做”和“可以做”分开。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从这个角度看，权限系统是在澄清代理角色。Claude Code 允许模型提出动作建议，但</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">是否放行，由运行时、规则和用户决定。系统刻意把能力判断和授权判断分开。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.5 权限结果本身也是一种运行时语义</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/utils/permissions/PermissionResult.ts:23</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统甚至给权限行</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">为准备了专门的描述函数：</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">allow</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">deny</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ask</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。这个细节很重要。它说明权限在Claude</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Code 里不只是内部布尔值，而是有独立语义的运行时对象。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事之所以重要，是因为权限系统要让系统能够明确地表达“为什么这一步没有继</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">续”。当一个代理说“我需要确认”时，系统是在声明责任边界。责任边界一旦说清楚，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">后续的拒绝、放行、缓存规则、临时授权、永久授权，才有地方安放。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更直接地说，一个代理系统如果连“这一步是我能做、不能做，还是需要问”的区别都</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">说不清，就不该碰终端。因为终端不会替系统补完语义，终端只会执行。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.6 StreamingToolExecutor 说明中断是一等语义</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">工具一旦开始并发和流式执行，中断问题就会立刻变得复杂。此时系统面对的是一个包</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">含queued、executing、completed、yielded 等多状态的队列，而不再只是单一动作。</span>

---

## Page 36

Chapter4·PermissionDecisionLayers
Tool request

Claude Code keeps an
Ruleslayer  explicit ask path.
allow / deny / askIntent is not the same as
authorization.

Mode layer
bypass / plan /
auto/default

Automated checkshard deny
classifier / hooks /
policy gates
bypass allow
Interactive approval  clear auto decision
onlywhenruntime
should not decide

Final result
ALLOWorDENY

<!-- 图源: ./book1-claude-code_assets/images/p036_scan.jpg -->


<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">28</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">图4: Claude Code Permission Decision Layers</span>

---

## Page 37

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">29</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/tools/StreamingToolExecutor.ts:34</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，明</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">确把这套东西做成了一个独立的流式工具执行器。这里面最值得注意的是它如何处理中</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">断和丢弃。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">StreamingToolExecutor.ts:64</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:70</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，系统允许在streaming fallback 时整</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">体discard 当前工具集合；在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:153</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 到</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:205</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，它会根据不同原因生成synthetic error</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">message，包括：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ sibling error</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ user interrupted</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ streaming fallback</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:210</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，系统还会专门判断中断原因，区分：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 因为别的并行工具出错而取消</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 因为用户interrupt 而取消</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 因为fallback 而放弃当前批次</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更细一点，在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">:233</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后，工具还有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">interruptBehavior</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，决定它在用户插话时究竟</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">该</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">cancel</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 还是</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">block</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这套设计的意义很大。它说明Claude Code 并不把中断理解成“执行失败的一种特殊</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">情况”，而是把中断当成和执行本身同样重要的语义。系统不仅要知道工具能不能开始，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">还要知道它被打断时如何收场、如何补齐结果、是否允许新消息插入。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这正是Harness Engineering 的一个基本特点：不仅设计开始，也设计停下。没有停</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下语义的执行系统，最终只能依赖用户外部打断来补完设计。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.7 Bash 为什么永远比别的工具更可疑</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在Claude Code 的工具世界里，Bash 不是普通工具，它更像风险放大器。原因很简单：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">它过于通用。越通用的接口，越难靠领域知识限制它。一个file read tool 至少不会顺</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">手杀进程，一个grep tool 至少不会偷偷push 代码，而Bash 几乎什么都能做。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 对Bash 的不信任，写得相当实在。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一层是在prompt 上，见</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/tools/BashTool/prompt.ts:42</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后。这里对git、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">PR、危险命令、hook、force push、interactive flags 这些事情写了大量明确规则。那</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">段prompt 看上去啰嗦，实际上很有分寸：凡是后果大的地方，系统就不怕啰嗦。</span>

---

## Page 38

Chapter4·ToolExecutionLifecycle

Receive assistant tool_use blocks
Partition calls by concurrency safety

Queue tools in StreamingToolExecutor  serial
Run tools in parallel  Run tools one by one
Check permission + interrupt rules
Buffer context modifiersApply each modifier immediately
Run tools as they become eligible
Apply modifiers in original order
Emit progress or synthetic errors
Yield finished results in safe order

Execution may be parallel.
Merge tool_result into transcriptContext evolution is still replayed
in assistant block order.
Harness controls live around the tool call
permission gates,
Returm deterministic context back to query loopinterrupt behavior,
sibling cancellation,
and synthetic results.

<!-- 图源: ./book1-claude-code_assets/images/p038_scan.jpg -->


<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">30</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">图5: Claude Code Tool Execution Lifecycle</span>

---

## Page 39

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">31</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">另一层是在权限和安全判定上。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/tools/BashTool/bashPermissions.ts:1</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">后整整一大段，都在处理shell 语义、命令前缀、重定向、wrapper、安全环境变量、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">classifier 与规则匹配。你从</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">bashPermissions.ts:95</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后甚至能看到系统为了防止</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">复合命令导致检查失控，还专门给subcommand 数量设了上限。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这说明，Bash 在Claude Code 里一直被视为需要特殊审查的危险通道，不是普通的命</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">令入口。工程师在这里承认了一件简单的事实：Bash 很强，所以必须被当成特例。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这是一个值得借鉴的判断。高风险能力不应该享受通用能力的待遇。能力越通用，越要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">特殊看管。把Bash 当成普通工具，往往只是设计上的偷懒。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.8 工具系统真正保护的，不只是用户，还包括系统自己</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">权限、调度和中断看起来像是在保护用户，其实它们同时也在保护系统自身。因为一个</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">代理系统如果允许自己留下这些问题——不完整的tool_result、失序的上下文修改、无</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">边界的并发副作用、说不清楚的中断语义——最终最先崩掉的往往是系统的一致性。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一点在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里和工具执行层是互相咬合的。前一章提到，query loop 在中断时</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">会补齐synthetic tool_result；这一章看到，StreamingToolExecutor 也在内部预留</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">了discarded、hasErrored、siblingAbortController、interruptBehavior 等机制。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">两边一起作用，目的是让系统在“执行过什么、没执行完什么、为什么停了”这些问题</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">上还能保持一条可追溯的因果链。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这也是Harness 的核心含义之一：替系统保住秩序。很多约束表面上是在防止误操作，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更深一层是在防止系统自己变成一堆无法解释的状态残片。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">4.9 从源码里可以提炼出的第四个原则</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一章最后可以压成一句话：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">工具是受管执行接口；权限是代理系统的基本器官。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的源码在几个地方共同支持这个判断：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> toolOrchestration.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把工具先分批，再执行，说明调度先于冲动</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> toolExecution.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把hooks、permission、telemetry 和synthetic error 包</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在工具执行周围，说明执行不是裸调用</span>

---

## Page 40

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第4 章工具、权限与中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">32</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> useCanUseTool.tsx</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把权限结果分成</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">allow / deny / ask</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，说明系统把授权</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">当成独立语义</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> StreamingToolExecutor.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 为中断、fallback、并发出错预留专门语义，说明</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">停止和开始同样重要</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> BashTool/prompt.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 与</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">bashPermissions.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 对Bash 采取特殊高压治理，说</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">明高风险能力必须接受更密约束</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果要把这些提炼成可迁移的工程原则，大概可以写成这样几条：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 让模型提出动作，不等于让模型拥有授权</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 工具调度必须保持因果秩序，哪怕执行并发</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 中断要有一等语义，不能靠异常兜底</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 高风险工具必须区别对待，不能图省事走通道化设计</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 一个工具系统真正保护的，既是用户，也是运行时本身</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下一章要讨论的是这套系统里另一种常见错觉：上下文越多越好。Claude Code 的实</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">现恰好说明，真正有经验的系统不会把上下文当仓库，而会把它当资源。接下来要讲的，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">是memory、CLAUDE.md 与compact 如何共同组成上下文治理。</span>

---

## Page 41

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">第5 章上下文治理：Memory、</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">CLAUDE.md 与Compact 是预算制度</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.1 上下文一多，系统就容易产生一种低级幻觉</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">人一旦可以往上下文里不停塞东西，就很容易相信一个朴素的神话：信息越多，系统越</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">聪明。这个神话听起来甚至有点合情合理。毕竟知道得多，总比知道得少强。可惜代理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">系统不是图书馆，模型也不是藏书管理员。上下文不是一个“存进去就算拥有”的仓库，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">它首先是一笔昂贵、易膨胀、还会自我污染的预算。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的源码在这件事上很不浪漫。它并没有把上下文设计成一个可以无限堆</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">叠的记忆池，反而在很多地方反复提醒自己：该加载什么、该截断什么、什么东西要长</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">期保留、什么东西只能短期摘要，都是运行时必须严肃治理的事。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以这一章要讨论的是：Claude Code 怎样防止自己被记住的东西拖死。这件事和“记</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">住更多”看起来相近，工程上却是两种制度。前者偏向收藏癖，后者才接近治理术。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.2</span><span style="font-family:'NotoSansMono-Bold';font-size:16.21pt;font-weight:bold;color:#000000"> CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000"> 体系说明，长期指令不能和临场对话混在一</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">起</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/utils/claudemd.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 开头就把记忆层次说得很清楚。它把in‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">struction source 分成几层：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ managed memory，例如</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">/etc/claude-code/CLAUDE.md</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ user memory，例如</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">~/.claude/CLAUDE.md</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ project memory，例如项目根目录里的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">.claude/CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">.claude/rules/*.md</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ local memory，例如</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">CLAUDE.local.md</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">33</span>

---

## Page 42

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第5 章上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">34</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">而且这些文件会按优先级和目录距离加载。离当前工作目录越近的project 规则，优先</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">级越高；越偏向私有、越偏向本地的规则，越晚加载，因而越靠近模型的注意力前沿。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事特别要紧。因为它说明Claude Code 从一开始就拒绝把“长期协作规则”和“本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">轮临时对话”混成一锅粥。团队规范、个人偏好、仓库约束，这些东西的寿命远长于某</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一轮用户消息；如果把它们全都塞进聊天记录里，系统就会在两个极端之间摇摆：要么</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">每轮都重复注入，浪费上下文；要么靠模型自己回忆，迟早失手。</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">claudemd.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 给出的答案，是把这些稳定规则做成可发现、可分层、可组合的持久指</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">令系统。还有个细节很有意思：它支持</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">@include</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，并且只允许一大批明确列出的文本</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">扩展名。这说明工程师除了追求include 的便利，也在提防另一种常见事故：有人把二</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">进制、巨型文档、甚至不该进prompt 的东西糊里糊涂带进来了。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这是正经工程师才有的克制。系统会先问：”什么东西值得进入系统记忆，什么东西一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">旦进入就是污染。”</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.3</span><span style="font-family:'NotoSansMono-Bold';font-size:16.21pt;font-weight:bold;color:#000000"> MEMORY.md</span><span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000"> 是索引，不是日记本</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">CLAUDE.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 管的是规则层，那么</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">memdir</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 处理的就是另一类更细的长期记忆。</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/memdir/memdir.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里有一段设计很值得反复看：</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ENTRYPOINT_NAME</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 被定义成</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MEMORY.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，但这个文件并不被鼓励用来直接堆内容，它被定义为index。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">源码里写得很实在。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">buildMemoryLines()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 明确告诉模型，保存memory 是两步：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">1. 把具体memory 写进独立文件</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">2. 再在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MEMORY.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里加一个一行指针</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">为什么这么麻烦？因为系统知道入口文件天然会被频繁加载，而频繁加载的东西一旦变</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">胖，整套上下文就会被它慢慢拖成一个不好收拾的胖子。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这也是为什么</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">memdir.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里专门有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MAX_ENTRYPOINT_LINES = 200</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 和</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MAX_ENTRYPOINT_BYTES</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">= 25_000</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。超过了，系统会直接</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">truncateEntrypointContent()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，并在结尾追加</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">明确警告：只加载了一部分，请把细节移到topic files。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这套做法特别像一个见过太多失控索引的人。它不相信大家会天然克制，所以把“入口</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">必须短”做成硬约束。因为入口文件一旦既当目录又当正文，最后就既不是目录，也不</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">是正文，只是一个谁都不愿再读第二遍的烂尾摘要。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从Harness Engineering 的角度看，这里抽出来的原则非常清楚：长期记忆必须分成</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">“入口”和“正文”。入口负责低成本寻址，正文负责高密度承载。把两者混为一谈，最</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">终一定是入口失效，随后整套记忆系统退化成摆设。</span>

---

## Page 43

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第5 章上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">35</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.4 Session memory 说明，短期连续性也不能靠聊天记录</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">硬扛</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">只有长期memory 还不够。代理系统真正难受的地方，常常在于“这轮之前我们到底</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">做到哪一步了”。这是一次会话内部的连续性问题。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/SessionMemory/prompts.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里专门给这件事建了</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一套模板。默认模板里有这些栏目：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Current State</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Task specification</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Files and Functions</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Workflow</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Errors &amp; Corrections</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Codebase and System Documentation</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Learnings</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Key results</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Worklog</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">你一看就知道，这不是给人抒情的。它关心的是：现在做到哪了，踩过什么坑，改过哪</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">些文件，后面该接什么。更有意思的是更新prompt 的语气。源码里明确要求：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 只能用Edit tool 更新notes file</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 不要提note‑taking 这件事本身</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 不要改模板结构</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> Current State</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 必须始终反映最近工作</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 每节都要信息密集，但要控制预算</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这说明session memory 在Claude Code 里并非“另存一份聊天记录”，它会把当前</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">会话萃取成一种可继续工作的操作说明书。它不求完整复刻对话，而求压缩出未来继续</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">干活所必需的骨架。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这里有个极其工程化的细节。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">prompts.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里定义了</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MAX_SECTION_LENGTH = 2000</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">和</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MAX_TOTAL_SESSION_MEMORY_TOKENS = 12000</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。超过预算，系统不会夸你记得</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">细，而是要求你aggressively condense，尤其优先保留</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Current State</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 和</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Errors</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">&amp; Corrections</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这很能说明问题。真正成熟的系统，会把“为继续工作保留最有用的部分”当成美德。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">因为上下文预算是工作内存。工作内存的第一职责是可操作。</span>

---

## Page 44

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第5 章上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">36</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.5 自动compact 说明，上下文治理首先是预算治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">到这里，长期规则、持久memory、session memory 都有了，但上下文还是会膨胀。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">于是Claude Code 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/compact/autoCompact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里进一步承认一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">个现实：不管你多会整理，只要对话够长，总会逼近窗口边缘。</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">getEffectiveContextWindowSize()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 先把模型context window 减去一笔保留给</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">summary 输出的预算。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MAX_OUTPUT_TOKENS_FOR_SUMMARY</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 直接预留了20,000 to‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">kens。也就是说，系统先假定compact 本身要花钱，绝不把窗口吃到只剩一口气时才</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">想起求生。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">接着</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">getAutoCompactThreshold()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 又在有效窗口上再扣掉</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">AUTOCOMPACT_BUFFER_TOKENS</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">= 13_000</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。警告阈值、错误阈值、手动compact 预留空间，也都各自分出buffer。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这套数字背后有个很朴素的道理：上下文治理需要提前为失败和恢复留出余地。不留余</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">地的系统，平时看着像节俭，出事时才暴露真相——不过是把风险账单留给了下一轮。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">更有意思的是</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">AutoCompactTrackingState</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。它不仅记</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compacted</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，还记</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">turn-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">Counter</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">turnId</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 和</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">consecutiveFailures</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。这说明autocompact 是一段会被追</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">踪、会失败、会被限流的运行时行为。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">源码甚至写了一个很直白的注释：全球每天曾经浪费大量API calls 在连续失败的au‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">tocompact 上，所以</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MAX_CONSECUTIVE_AUTOCOMPACT_FAILURES = 3</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，再失败就</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">触发circuit breaker。这里的气质非常好，像一个终于受不了浪费的人：你可以失败，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">但不能无限次、无记忆地失败。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.6</span><span style="font-family:'NotoSansMono-Bold';font-size:16.21pt;font-weight:bold;color:#000000"> compactConversation()</span><span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000"> 说明，摘要要重建可继续工</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">作的上下文</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多人一听compact，会以为就是“把前面聊天摘要一下”。Claude Code 的实现要复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">杂得多。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/compact/compact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compactConversation()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 真</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">正做的，是把原有上下文拆开、摘要、再注入必要附件，重新搭出一个还能工作的后</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">compact 世界。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">先看压缩前的清洗。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">stripImagesFromMessages()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 会把图片、文档替成</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">[image]</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">、</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">[document]</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 之类的标记；</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">stripReinjectedAttachments()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 会把反正之后还要重</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">新注入的attachment 先剥掉，免得浪费token。仅这两个动作就说明，compact 会有</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">选择地丢掉那些”对摘要没用、但token 开销极大”的部分。</span>

---

## Page 45

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第5 章上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">37</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">再看摘要失败时的处理。源码里有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">truncateHeadForPTLRetry()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，专门应对“compact</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">请求自己都prompt too long”的尴尬场面。也就是说，系统不仅承认主流程会爆，还</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">承认“救火工具本身也会爆”。这很像真实世界，而不是demo。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">而在compact 成功之后，Claude Code 做的不是简单保留一条summary。它还会：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 清空旧的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">readFileState</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 重新生成post‑compact file attachments</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 把plan attachment 补回来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 把plan mode attachment 补回来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 把invoked skills attachment 补回来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 把deferred tools、agent listing、MCP instructions 的delta attachment 重</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">新补回来</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 执行session start hooks 和post‑compact hooks</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 写compact boundary message，记录pre‑compact token 数与边界信息</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这些动作合在一起，意思很明确：compact 的目标是把“继续干活所需的运行时环境”</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">重新铺平。摘要只是中间产物，不是最终目的。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以compact 在Claude Code 里更像一次受控重启，而不是一次聊天总结。旧上下</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">文会被转译成新的工作底座。这种设计很值得记住，因为很多系统只做前半截，结果</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">compact 之后虽然“还记得大概”，却已经失去了工具状态、计划状态、附件状态，接</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下来还得再花几轮找回自己。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.7 上下文治理的关键是保留工作语义</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果只看</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 的后半段，会发现一个贯穿始终的倾向：Claude Code 真正在</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">意的是把工作语义保住。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">例如它会恢复最近访问文件的attachment，因为这些文件往往构成当前工作面的局</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">部现实；它会恢复plan mode，因为否则模型压缩完以后可能忘了自己还处在plan</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">discipline 里；它会保留invoked skills 的内容，但又给每个skill 设置token cap，避</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">免skill 本身在post‑compact 阶段反客为主。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">源码里这句话很有味道：per‑skill truncation beats dropping。意思是，即使要裁，也</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">优先保住开头那一段最关键指令，而不是整个扔掉。这就是治理，不是纯粹节流。纯节</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">流是砍，治理是知道该砍哪里、该保什么。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从这里可以抽出一个相当稳妥的经验：上下文系统应该优先保留能维持行动语义的东</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">西，而不是优先保留看起来信息量最大的东西。文件细节、当前计划、错误修正、技能</span>

---

## Page 46

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第5 章上下文治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">38</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">约束，这些都直接决定下一步能不能做对。反过来，冗长的历史对话、重复出现的附件、</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">运行时随时可以重新拿到的东西，就没必要再占着座位。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">5.8 从源码里可以提炼出的第五个原则</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这一章最后可以压成一句话：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">上下文是工作内存。治理它的目标是支持系统继续工作。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的源码在几个层面共同支持这个判断：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> claudemd.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把长期指令分层加载，说明稳定规则要和临时对话分开治理</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> memdir.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 把</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">MEMORY.md</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 定义成索引并强行截断，说明入口文件必须短而可寻</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">址</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> SessionMemory/prompts.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 用固定模板提炼会话连续性，并对section 和总</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">量设预算，说明短期记忆也必须结构化</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> autoCompact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 为compact 预留输出预算、缓冲区和失败熔断，说明上下文</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">窗口要按风险来经营</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000"> compact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 在摘要后恢复计划、文件、技能、工具附件和hook 状态，说明compact</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">的目标是重建工作语义，而不是写一段好看的总结</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果把这些抽象成可迁移的工程原则，大概有这样几条：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 长期规则、长期记忆、会话连续性，应该分层，不该混写</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 入口型记忆必须短小，否则整个系统会被入口拖垮</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ session summary 应该服务于“继续工作”，而不是服务于“回忆完整”</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ compact 是上下文治理主路径</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 压缩后的上下文必须保住运行语义，而不是只保住语言表面</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">下一章要讲的是这套治理系统碰到极限时怎么办。因为一个真系统终究会出错：prompt</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">too long，max output tokens，hook 死循环，恢复分支相互打架。到那时你才看得</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">出来，一个代理系统到底是在“赌不出事”，还是在认真设计出事之后如何继续运行。</span>

---

## Page 47

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">第6 章错误与恢复：出错后仍能继续工作</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:24.79pt;font-weight:bold;color:#000000">的代理系统</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.1 工程世界最不值得相信的话，就是“正常情况下”</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多系统设计文档里，最常见的偷懒方式，就是先讲一遍“正常情况下”的流程，仿佛</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">只要主路径足够漂亮，错误就会自动显得次要。可代理系统一旦进入真实运行环境，这</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">种写法通常很快就会露馅。因为现实中什么都会出问题：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 模型会被截断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 请求会超长</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ hook 会制造回环</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 工具会中断</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ fallback 会发生</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ 恢复逻辑本身也会失手</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以，判断一个代理系统成熟不成熟，不能只看它回答顺畅的时候有多像个人，而要看</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">它出故障的时候像不像系统。前者容易靠一点prompt 工程粉饰，后者只能靠运行时纪</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">律。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在这一点上的可取之处，是它没有假装自己不会出错。相反，源码里反复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">体现出一种冷静判断：错误属于主路径，恢复则是必须提前设计好的运行机制。</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.2</span><span style="font-family:'NotoSansMono-Bold';font-size:16.21pt;font-weight:bold;color:#000000"> prompt too long</span><span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000"> 是一种必然周期</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">对长会话代理来说，</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">prompt too long</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 是一种迟早会来的季节变化。你如果把它当偶</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">发异常，系统迟早会被它教育。</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">39</span>

---

## Page 48

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第6 章错误与恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">40</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 就没有把它当偶发异常处理。在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里，系统</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">甚至会“暂时扣下”这类错误，不立刻把它原样抛给用户。流式阶段里，</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">withheld</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 逻</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">辑会识别recoverable errors，包括：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ prompt too long</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ media size error</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">‧ max output tokens</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事的意思很明确：有些错误要先交给恢复系统试着处理，再决定是否展示给用户。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这个顺序很关键，因为用户真正关心的通常是系统还能不能继续干活。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">在prompt too long 的分支上，Claude Code 先尝试更便宜、更保守的恢复路径。若启</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">用了context collapse，就先</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">recoverFromOverflow()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，把已经staged 的collapse</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">提交掉；如果还不行，再进入</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">reactiveCompact.tryReactiveCompact()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。也就是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">说，恢复是分层的：先排空已知积压，再做更重的全文压缩，不会一上来就重建世界。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这种次序特别有工程味。因为真正好的恢复系统，不会把所有错误都交给“最重的一把</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">锤子”。它会先试图保住最细粒度的上下文，再在必要时接受更粗糙的摘要替代。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.3 响应式compact 说明，恢复的关键在于别把自己逼进</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">死循环</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多系统做恢复时容易犯一个愚蠢但常见的错误：一旦发现错误可恢复，就不停重试，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">直到把错误从偶发事件升级成资源灾难。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 对这件事非常警惕。</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里有两个地方都能看出这种警惕。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第一处，是</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">hasAttemptedReactiveCompact</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。一旦reactive compact 已经试过，再</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">次遇到同类问题时，系统不会装傻重来。因为工程师很明白：如果compact 之后还是</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">不行，那么继续compact 大概率只是在把同一种失败换个姿势再演一次。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第二处，是stop hooks 的防死循环处理。源码里有非常直白的注释：如果prompt too</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">long 之后还让stop hooks 介入，就可能出现death spiral，路径大致是：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">错误‑&gt; hook blocking ‑&gt; retry ‑&gt; 错误‑&gt; hook blocking</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这话写得一点也不文学，却比许多文学都诚实。因为它承认系统里最危险的错误，是失</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">败分支和恢复分支彼此咬住，开始无限自我复制。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以Claude Code 在prompt too long 无法恢复时，会直接surface 错误并跳过stop</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">hooks。原因很简单：这时候继续走形式流程，只会让坏事更有仪式感。</span>

---

## Page 49

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第6 章错误与恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">41</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.4</span><span style="font-family:'NotoSansMono-Bold';font-size:16.21pt;font-weight:bold;color:#000000"> max_output_tokens</span><span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000"> 的处理说明，恢复要以续写为主</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">大模型产品有个很坏的习惯，一旦输出截断，就先来一段很客气的废话：抱歉，刚才被</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">截断了，我来总结一下。听上去态度不错，实际上对工作几乎没帮助。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/query.ts:1185</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 往后的处理，明显更接近工程系统。它先试一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">种成本较低的恢复：如果当前使用的是较保守cap，就把</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">maxOutputTokensOverride</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">提升到更高值，直接重跑同一请求。注意，这一步没有插入meta message，也没有让</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">模型先寒暄，系统先给它一次把原任务做完的机会。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果更高cap 也不够，再进入第二层恢复：给模型追加一条meta user message，内</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">容非常实在，大意是：</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">直接继续，不要道歉，不要recap，若中断发生在半句，就从半句接着写；剩余工作拆</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">小一点。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这是一条很有启发意义的系统指令。它说明Claude Code 对恢复的理解，是尽量保持</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">任务连续性，不要把额外token 花在礼貌性收尾上。在长任务里，这种区别非常大。因</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">为每一次截断后的recap，都会进一步消耗预算，并且增加语义漂移。最后系统做的就</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">不再是任务本身，而是一轮轮地回顾自己做任务。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">所以对</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">max_output_tokens</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 来说，较好的恢复通常是续写。Claude Code 在这里优</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">先保证任务连续性，而不是补充礼貌性说明。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.5 auto compact 的失败熔断，说明恢复系统自己也要受</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">治理</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">如果说前面讲的是“单次错误怎么救”，那</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">src/services/compact/autoCompact.ts</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">处理的就是另一个层面的问题：当恢复机制本身不断失败时，怎么办。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">源码的回答很简单，也很正确：别一直试。</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">AutoCompactTrackingState</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里专门有</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">consecutiveFailures</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。一旦失败次数超过</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">阈值，</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">shouldAutoCompact</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 即便判断“按理说该compact 了”，系统也会直接跳过。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">源码注释甚至给出过往数据：曾有大量session 在连续autocompact failure 上白白</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">烧掉海量API calls，所以必须加circuit breaker。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">失败熔断的本质，是承认当前恢复手段在这个局面里已经失效。一个真正成熟的系统，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">不能只会在成功时记录指标，也得在失败时懂得收手。不会收手的恢复系统，和不会刹</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">车的汽车差不多，理论上都叫系统，实际上都不该上路。</span>

---

## Page 50

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第6 章错误与恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">42</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">从Harness Engineering 角度看，这里可以抽出一条很硬的原则：任何自动恢复机制</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">都必须可计数、可限次、可熔断。否则恢复会从保险丝变成新的起火点。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.6 compact 自己也会爆，所以连“修复动作”都需要修复</span>

<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">策略</span>

<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compactConversation()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 这段代码还有一个很动人的现实主义时刻：它承认com‑</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">pact 请求自己也可能</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">prompt too long</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这件事看上去带一点黑色幽默。系统为了缩短上下文去发摘要请求，结果摘要请求也因</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">为上下文太长而失败。很多人不喜欢这种情形，因为它暴露得过于直接。但工程系统首</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">先要解决的是继续运行，而不是保持表面完整。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 的处理方式，是在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 里引入</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">truncateHeadForPTLRetry()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">当compact 自己太长时，系统会先把更早的API round 成组地从头部剥掉，再重试</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">compact，避免让用户卡在“连压缩都压不动”的状态里。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这里的取舍很清楚：这种修复有损，也会丢历史，但它优先保证用户不被完全锁死。源</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">码注释写得很实在，这是一种last‑resort escape hatch。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这种处理方式的价值，在于它没有回避现实约束。系统快要窒息时，优先级是先恢复呼</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">吸，再讨论信息保真度。这个判断不追求漂亮，但很实用。</span>
<span style="font-family:'NotoSerifCJKjp-Bold';font-size:17.22pt;font-weight:bold;color:#000000">6.7 abort 语义说明，中断也属于错误恢复的一部分</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">很多人把abort 单独归到交互体验，不愿意把它放进错误恢复讨论里。从运行时角度看，</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">中断就是一种必须被正确回收的失败态。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">Claude Code 在两个层面都认真处理了这件事。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">一层在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">query.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。如果streaming 时用户打断，系统会先消费</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">StreamingToolEx-</span>
<span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">ecutor.getRemainingResults()</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，为已经发出但尚未完成的工具生成synthetic</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">tool_result，确保前面承诺过的</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">tool_use</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000"> 不会变成悬空债务。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">另一层在</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">compact.ts</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">。源码里专门把compact 的abort controller 传给forked</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">agent，并处理</span><span style="font-family:'NotoSansMono-Regular';font-size:11.46pt;color:#000000">APIUserAbortError</span><span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">，防止“被用户按了Esc 的compact”误算成一</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">次成功摘要。</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">这两处连在一起看，意思很明确：中断不只是“用户不想看了”，而是一次需要正确收</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">尾的状态转移。错误恢复如果只管异常、不管中断，最终会留下大量语义半残的执行轨</span>

---

## Page 51

Chapter6·RecoveryDecisionPaths

Recoverable failure observed
prompt toolong?  max_output_tokens?other
yes  yes
Drain staged collapse  Raise output cap if cheap
Classify API retry / abort /
unrecoverable error
Attempt reactive compact onceOtherwise continue directly
without recap

continue
Surface error  Prepare retry or compact result
Re-enter query loop

Recovery is layered:
cheaper local fixes first,
heavier summarization later.

<!-- 图源: ./book1-claude-code_assets/images/p051_scan.jpg -->


<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">第6 章错误与恢复</span>
<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">43</span>

<span style="font-family:'NotoSerifCJKjp-Regular';font-size:11.96pt;color:#000000">图6: Claude Code Recovery Decision Paths</span>

---

## Page 52

Chapter6.CompactFallbacks

Start compact request
ession-memory path enough?
Apply compact resultBuild conversation summary request

compact request hits PTL?
Trim oldest history groups

Retry compact request

repeated failures trip cireuit breaker?
Stop auto-compact attempts  Restore critical files / skills / deltas
Y
Return control to caller  Resume next turn with compact boundary

Even compact can fail.
Claude Code keepsafinalescapehath
instead 