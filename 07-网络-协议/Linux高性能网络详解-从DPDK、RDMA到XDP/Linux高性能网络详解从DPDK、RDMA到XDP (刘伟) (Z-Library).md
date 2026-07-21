## Page 1

Linux

<!-- 图源: ./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p001_scan.jpg -->


<span style="font-family:'FZLTZHUNHK--GBK1-0';font-size:14.00pt;color:#231f20">刘  伟◎著</span>
<span style="font-family:'FZLTZCHK--GBK1-0';font-size:53.00pt;color:#26235b">高性能网络详解</span>
<span style="font-family:'FZLTZCHK--GBK1-0';font-size:35.00pt;color:#26235b">从</span> <span style="font-family:'FZSUHS-R--GB1-0';font-size:35.00pt;color:#eb415e">DPDK</span><span style="font-family:'FZLTZCHK--GBK1-0';font-size:35.00pt;color:#26235b">、</span><span style="font-family:'FZSUHS-R--GB1-0';font-size:35.00pt;color:#eb415e">RDMA</span><span style="font-family:'FZLTZCHK--GBK1-0';font-size:35.00pt;color:#26235b"> 到</span> <span style="font-family:'FZSUHS-R--GB1-0';font-size:35.00pt;color:#eb415e">XDP</span>

---

## Page 2

Linux

<!-- 图源: ./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p002_scan.jpg -->


<span style="font-family:'FZLTZHUNHK--GBK1-0';font-size:14.00pt;color:#231f20">刘  伟◎著</span>
<span style="font-family:'FZLTZCHK--GBK1-0';font-size:53.00pt;color:#231f20">高性能网络详解</span>
<span style="font-family:'FZLTZCHK--GBK1-0';font-size:35.00pt;color:#231f20">从</span> <span style="font-family:'FZSUHS-R--GB1-0';font-size:35.00pt;color:#545456">DPDK</span><span style="font-family:'FZLTZCHK--GBK1-0';font-size:35.00pt;color:#231f20">、</span><span style="font-family:'FZSUHS-R--GB1-0';font-size:35.00pt;color:#545456">RDMA</span><span style="font-family:'FZLTZCHK--GBK1-0';font-size:35.00pt;color:#231f20"> 到</span> <span style="font-family:'FZSUHS-R--GB1-0';font-size:35.00pt;color:#545456">XDP</span>

---

## Page 3

<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TimesNewRomanPSMT';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'SimHei';font-size:10.50pt;color:#000000">内 容 提 要 </span>

<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">本书主要介绍了</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">DPDK</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">RDMA</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">XDP</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 三种高性能网络技术的原理、使用方法和实现方案。</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">本书总计</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">26</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 章，分为四大部分。第</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 部分介绍了计算机网络、计算机硬件和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">Linux</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 操作系统的</span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">基础知识，以及软件和硬件之间传递信息的方式、以内核协议栈为基础的网络方案和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">Corundum</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">。第</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 部分介绍了</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">DPDK</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 的入门知识、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">DPDK</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 的内存管理、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">UIO/DPDK</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 的基本使用方法、测试和分析高</span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">性能网卡，以及如何为</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">Corundum</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 编写</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">DPDK</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 驱动程序。第</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 部分包括</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">RDMA</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 技术简介、软件架构、</span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">基本元素、基本操作类型及其配套机制、传输服务类型、应用程序执行流程、主要元素的实现、数</span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">据传输、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">RoCEv2</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 网卡的配置、性能测试工具等内容。第</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 部分包括</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">XDP</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 简介、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">XDP</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 教程代码分析、</span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">简单的</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">XDP</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 性能测试、如何让网卡驱动程序支持</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">XDP</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 功能等内容。</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">本书适合对高性能网络技术感兴趣的软件和硬件开发工程师、系统工程师、网络性能分析人员</span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">阅读。</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'BernardMT-Condensed';font-size:10.50pt;color:#000000"> </span>

<span style="font-family:'SymbolMT';font-size:10.50pt;color:#000000"></span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">著</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">        </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">刘</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">伟</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">   </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">责任编辑</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">傅道坤</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000">   </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">责任印制</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">王</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">郁</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">马振武</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SymbolMT';font-size:10.50pt;color:#000000"></span><span style="font-family:'BernardMT-Condensed';font-size:10.50pt;color:#000000"> </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">人民邮电出版社出版发行</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000">    </span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000">北京市丰台区成寿寺路</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">11</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 号</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span>
<span style="font-family:'SimSun';font-size:7.50pt;color:#000000">邮编</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">  100164    </span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000">电子邮件</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">  315@ptpress.com.cn</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000">  </span>
<span style="font-family:'SimSun';font-size:7.50pt;color:#000000">网址</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">  https://www.ptpress.com.cn</span><span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000">   </span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">固安县铭成印刷有限公司</span><span style="font-family:'SimSun';font-size:9.00pt;color:#333333">印刷</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SymbolMT';font-size:10.50pt;color:#000000"></span><span style="font-family:'BernardMT-Condensed';font-size:10.50pt;color:#000000"> </span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000">开本：</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">787</span><span style="font-family:'SymbolMT';font-size:7.50pt;color:#000000"></span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">1092  1/16</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:7.50pt;color:#000000">印张：</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">22.75   </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">2023</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 年</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 月第</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 版</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000"> </span>
<span style="font-family:'Garamond';font-size:9.00pt;color:#000000"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:7.50pt;color:#000000">字数：</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">569</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 千字</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">      </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">2023</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 年</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 月河北第</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:7.50pt;color:#000000"> 次印刷</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.50pt;color:#000000"> </span>

<span style="font-family:'SimSun';font-size:9.00pt;color:#000000">定价：</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000">118.80</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000"> 元</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'SimHei';font-size:9.00pt;color:#000000">读者服务热线：</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">(</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">010</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">)</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">81055410  </span><span style="font-family:'SimHei';font-size:9.00pt;color:#000000">印装质量热线：</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">(</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">010</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">)</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">81055316</span><span style="font-family:'BernardMT-Condensed';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'SimHei';font-size:9.00pt;color:#000000">反盗版热线：</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">(</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">010</span><span style="font-family:'SimSun';font-size:9.00pt;color:#000000">)</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">81055315 </span>
<span style="font-family:'SimHei';font-size:9.00pt;color:#000000">广告经营许可证：京东市监广登字</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:9.00pt;font-weight:bold;color:#000000">20170147</span><span style="font-family:'SimHei';font-size:9.00pt;color:#000000"> 号</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#000000"> </span>

![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p003_img01.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p003_img02.png)

---

## Page 4

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TT9245412FtCID';font-size:22.02pt;color:#000000">推荐序 </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">时下新一代光纤和无线通信技术的演进日新月异，云服务、人工智能、虚拟现实等技术</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">的应用发展如火如荼，这些都需要海量的数据交互与处理能力作为支撑，与之相应地，也对</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">网络设备带宽和服务实时性提出了更高的要求。传统的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10M/100M/1000M</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网络设备在应对这</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">种需求时显得捉襟见肘，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10G/25G/40G/100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等设备标准已经推出，纳秒级网络设备也开始走</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">向市场。在硬件标准不断演进的同时，找到适合的软件方案，充分利用和挖掘设备的硬件性</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">能，以达到成本和性能的最优解，已成为一个全新的挑战。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">作为网络设备中主流的嵌入式操作系统，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的开源设计为产品开发提供了诸多的便</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">利条件。但传统的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 内核网络协议栈处理过程存在固有的性能瓶颈，诸如数据多重复制、</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">中断</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">内核态</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">用户态切换、多核迁移、缓存失效等机制会带来大量的性能消耗。随着更高速率</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">网络设备的出现，这种性能制约变得更加突出。因此，各种零复制和内核旁路等软件技术以</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">及</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 这种新（软硬件）架构应运而生，这也是本书所阐述的主题。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书的作者是一位资深的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动开发专家，我与作者在通信领域合作多年，其开</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">发的产品的用户数也已达到千万量级。至今我还对几年前与作者的一次深谈印象深刻，深</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">谈主题可称为“优秀内核驱动工程师的炼成”，那次谈话令我感慨一个驱动开发工程师成</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">长的艰辛。不同于通常意义上的软件工程师，驱动开发工程师有其独特的成长曲线。丰富</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">的硬件知识是一个技术门槛，由于软件工程师和硬件工程师在知识体系上存在巨大差异，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">两者的认知具有天然的技术“鸿沟”，而驱动开发工程师就负责消除这一鸿沟，把软硬件</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">有机地组织起来。驱动开发工程师需要把握每个功能芯片的行为，细微处小到一根引脚或</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">一个时序波形。除了深度，驱动开发工程师更需要广度，只有在累积了足够丰富的芯片经</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">验之后，把零散的知识点组装成树之后，才能结合</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CPU</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、功能芯片、操作系统和应用软件，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">进行完善的全系统架构设计。我们常有戏言：“驱动开发工程师要宛如一粒电子，经历每</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">一条指令，了解每一比特数据，从源头直到目的地”。经受过如此经年累月的艰苦，才能</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">有所收获并得到提升。本书的作者正是一位在</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动领域拥有丰富经验的技术专家，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">曾经凭借其出色的技术能力解决了不少项目中遇到的难题，并又快又好地带领团队开发了</span>

---

## Page 5

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">推荐序 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">几个公司级的重点产品。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书以作者亲身的工作研究和总结为基础，从软硬件结合的视角详细论述了几种基于</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 系统的新兴高性能网络技术方案，主要涉及</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等。本书图文并茂、</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">深入浅出，既有高屋建瓴的原理阐述与分析，也有抽丝剥茧的核心代码讲解，还有细致入微</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">的实战操作技巧和数据结果，体现了作者对驱动技术娴熟的驾驭能力，是一本十分精彩的、</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">贴合</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动开发工程师学习路线的参考指导书。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">“十年磨一剑，一朝惊寰宇”。作者在高性能网络技术这一领域进行总结精炼形成的这本</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">著作，体现了作者积累多年的内核研究与设计经验，阅读之后令人受益匪浅。本书对于加速</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">读者在上述高性能网络技术领域的学习进程、巩固和提高设计能力、启发新的技术创新，都</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">有很大的助益，同时也期待作者下一部精彩的著作问世。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">2 </span>

<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">卢海军</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">上海诺基亚贝尔固网事业部负责人、诺基亚</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">OLT</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 全球研发负责人</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2022</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 年</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 月</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">18</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 日</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

---

## Page 6

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TT6C2446EAtCID';font-size:22.02pt;color:#000000">作者简介 </span>

<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">刘伟</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，拥有</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">14</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 年网络设备开发领域的从业经验，当前就职于浪潮电子信息产业股份有限</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">公司体系结构研究部，负责高性能网卡的架构设计和驱动程序开发工作。在此之前，曾以驱</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">动团队和网络接入设备产品开发负责人的身份在上海诺基亚贝尔固网事业部工作了</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 年；还</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">曾经就职于中兴通讯和上海爱吉信息技术有限公司，负责多款通信产品的研发工作。平时喜</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">欢钻研技术和读书，并经常在自己的个人公众号“布鲁斯的读书圈”中发表原创的技术文章。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

---

## Page 7

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TT6C2446EAtCID';font-size:22.02pt;color:#000000">献辞 </span>

<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">谨将本书献给我的妻子王秀丽、我的孩子刘义嵩和刘美宁，感谢你们一直以来的支持和</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">不断的鼓励。我还要把本书献给一直支持我的父母和所有家人，感谢你们对我的包容和理解。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.98pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.98pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.98pt;color:#000000"> </span>

<span style="font-family:'TT6C2446EAtCID';font-size:22.02pt;color:#000000">致谢 </span>

<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">感谢浪潮公司的领导和同事，包括阚宏伟、宿栋栋、沈艳梅、王彦伟、杨乐、张静东、</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">王江为、刘钧锴、张翔宇、韩海跃等，在我的工作和研究过程中给予的支持和帮助。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">感谢上海诺基亚贝尔固网事业部负责人卢海军（也是我的老领导）为本书写的推荐序。</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">另外，我在该公司的原同事陈峰在看过本书的初稿后，也给出了一些宝贵的建议，在此表示</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">感谢。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">感谢人民邮电出版社的傅道坤编辑在本书写作过程中提供的各种反馈意见。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

---

## Page 8

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TT9245412FtCID';font-size:22.02pt;color:#000000">前   言 </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">今年是我毕业后进入通信行业的第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">14</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 个年头。这些年来通信行业迅猛发展，新技术层出</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">不穷。从无线网络的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">3G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">4G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">5G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，到固定宽带的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DSL</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GPON</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">EPON</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">NG-PON2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，各种凝</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">聚了通信领域研发人员智慧和汗水的新技术不仅改善了人们的生活，也让我们国家在高科技</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">领域不断缩小和发达国家的差距，甚至在某些领域实现反超。身处时代发展的浪潮之中，每</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">每想起我自己的工作也为社会进步贡献了绵薄之力，世界各地都在运行着自己参与研发的设</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">备，就会心潮澎湃。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">从毕业后进入一家小公司</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">——</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">上海爱吉开始，到后来工作过的中兴通讯和上海诺基亚贝</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">尔，我一直都在参与各种通信设备的研发工作，并且始终在嵌入式软件领域深耕细作。嵌入</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">式软件工程师，是一个对工作者拥有的计算机知识的广度和深度要求都比较高的岗位，所涉</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">及的领域从应用程序的需求，到操作系统的架构；从各种总线的配置，到对硬件行为的理解。</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">嵌入式软件从业人员需要与各种软件和硬件模块的负责人沟通协作。工作繁忙的同时，也得</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">到了大量锻炼个人技能的机会。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">由于工作需要，我阅读了大量</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统以及网络相关的书籍和资料，在从大师们</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">的作品中汲取知识的同时，也惊叹于他们对系统细致入微的理解和深入浅出的讲解。因此一</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">直以来都非常希望能像他们一样，写出自己的作品。不过在写作本书之前，虽然平时颇有些</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">技术积累，也时常进行知识分享，但始终无法成体系，所以一直未能如愿。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2020</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 年</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 月以来，由于个人兴趣，加上朋友的建议，我开通了公众号“布鲁斯的读书圈”，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">并时常在上面发表一些读书笔记和技术见解，慢慢地积累了一些彼此联系比较紧密的系列文</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">章。在进入浪潮公司后，由于部门领导的信任，我有幸负责了浪潮基于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">的技术调研、方案设计以及驱动程序和测试工具的开发工作，由此进入高性能网卡研究领域。</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">其间也调查了其他一些高性能网络方案的原理和实现，比如</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等，并为一个开源</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">的基于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡方案</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 编写了</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">各种各样的计算机设备的共同协作向人们提供了丰富多彩的网络服务。在大部分设备上，</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统是其不可或缺的组成部分。在和层出不穷的各种网络技术共同发展的几十年</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">中，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 对网络功能的支持也在逐步完善。但作为运行所有应用程序的软件平台，操作系统</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本身对网络功能的支持始终偏向于通用性和全面性，相比之下，性能就处于相对次要的位置。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">最近十几年以来，随着物理网络的吞吐量逐渐增大，以及通用多核处理器被引入网络数</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">据处理领域，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 略显臃肿的网络协议栈已经无法满足用户对性能的需求。于是人们开始另</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">辟蹊径，开发了多种新型的网络技术方案，比如</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等。和传统的以</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">网络协议栈为基础的方案不同，这几种方案的性能更高，解决了传统网络方案在一些实际应</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">用场景中所遇到的性能瓶颈问题，所以近年来在高性能存储和分布式计算等领域得到了快速</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">发展。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">在上述三种高性能网络方案中，除</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 问世时间较晚外，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 都已经有十</span>

---

## Page 9

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">前言 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">几年的发展历史了，所以在互联网上可以找到很多描述它们的资料，但我没有发现一本令人</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">满意的对这些技术的实现方案进行系统性详细解析的书。我心目中理想的描述某种技术的书</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">是这样的：能够对相关技术产生的原因和发展状况给出基本介绍；既有对总体架构的描述，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">也有对各个模块作用的介绍，最好还能分析为什么要这样设计；对软件和硬件的交互机制与</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">流程有详细的阐述；能够帮助开发人员从根上理解、应用甚至改良这种技术。既然还没有这</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">样的书，那就写一本吧，这就是本书之所以问世的根本原因了。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书收纳了公众号“布鲁斯的读书圈”中的一些原创性文章，并结合我自己长期的工作</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">经验和对相关方案实现代码的深度剖析，系统地讲述了依托于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统的几个高性能</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">网络方案的原理和实现，尤其对软件和硬件交互的细节进行了详细描述。书中包含大量图示，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">并穿插介绍各种小知识，希望对各位读者有所帮助。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">本书的组织结构 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书分</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 个部分，共</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">26</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章。以下是本书各章内容的简要介绍。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">第1 部分，背景知识 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“计算机网络概述”：介绍了计算机网络的基本概念、构成网络的成员、网络</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">分层模型和常见术语等网络相关的基础知识。不过计算机网络是一个博大精深的领</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">域，本书并不是这方面的入门读物，虽然作者已经尽可能地尝试在有限的篇幅内提供</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">足够多的信息，但难免会有读者感到理解困难或意犹未尽。对于这部分读者，可以阅</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">读计算机网络的相关书籍。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“计算机硬件”：简单介绍和计算机网络相关的各种硬件，包括硬件内部的组</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">件，比如中央处理器、存储器、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">PCIe</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 总线和网卡设备等。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统”：对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统的一些基本概念和组件进行介绍，包</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">括用户态和内核态、页表、内核的组成部分等。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“软件和硬件之间传递信息的方式”：描述了读写寄存器、数据缓存、队列和</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">描述符、中断、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等用于在软件和硬件之间传递信息的手段。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“内核协议栈方案及其存在的问题”：描述了</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统对网络功能的全</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">面且层次分明的支持。其中网络协议栈负责数据包的封装和解析，网络设备驱动程序</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">负责管理和控制网卡，最终完成传输数据包的操作。本章最后讲述了前述方案的一些</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">缺点，这些缺点正是本书重点描述的各种高性能网络技术之所以出现的原因。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">——一个开源的基于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡方案”：先对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">方案的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 实现框架和队列机制进行简要描述，然后详细解析它的设备驱动程序。</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">这种涉及方案细节的内容在其他书中一般属于比较核心和靠后的章节，但在本书中仍</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">属于基础知识。原因是此方案属于传统的（第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章中描述的）以</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网络协议栈为</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">基础实现的网络方案，对它进行比较细致的了解有助于深入理解后面章节介绍的各种</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">高性能网络方案的细节以及产生的原因。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">第2 部分，DPDK </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“认识</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">”：解释为什么需要</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，以及介绍</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的技术特点、体系</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">结构和核心组件，并描述一个典型的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序的执行流程。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">2 </span>

---

## Page 10

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">8</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的内存管理”：</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 追求极致的网络数据包处理性能，其内部使用</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">了多种技术加速数据包的收发和处理，这些技术包括多核多线程、单指令多数据、无</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">锁环形缓冲等。其中最能体现</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 对性能极致追求的是在内存管理领域。本章专门</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">描述</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 在内存管理领域所使用的各种性能优化技巧。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">9</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">UIO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">——</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的基石”：</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 使用了</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 提供的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">UIO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 机制对硬件进行</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">直接访问。本章对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">UIO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的构成、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">API</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等进行介绍。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的基本使用方法”：学习使用</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，包括其编译和安装方法、常</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">用的工具软件及其命令选项等，并做了几个简单的测试。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“测试和分析高性能网卡”：首先分析和测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DDR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的访问速率；然后使用</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡进行单核和多核测试，验证前文对于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DDR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 访问速率的分析结果；</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">最后介绍一种定量分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 性能的工具——</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Intel VTune Profiler</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">12</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">“为</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 编写</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序”：目前大部分主流网卡都已支持</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">也就是说在</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 开源代码中已经有了这些网卡的驱动程序。但这些网卡不包含基</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡方案</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。于是作者为</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 编写了</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">序，并获得了比内核协议栈方案更高的数据包收发性能。本章详细阐述了此</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">驱动程序。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">第3 部分，RDMA </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">13</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 技术简介”：简单介绍</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 技术，包括其控制通路和数据通路、</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">协议、网络构成以及</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">LID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等概念。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">14</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 软件架构”：对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 软件的两大主要模块（</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">rdma-core</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和内核</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">子系统）进行综合性阐述。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">15</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 基本元素”：描述</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">QP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CQ</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">WR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">WC</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 基本元素的概念</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">和功能。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">16</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 基本操作类型及其配套机制”：介绍</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Send</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Receive</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA Write</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA Read</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 基本操作类型，以及</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">PD</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等保障</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作顺利进行的</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">配套机制。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">17</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 传输服务”：从两个维度，即“可靠</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">不可靠”和“连接</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">数据报”，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">介绍</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的传输服务类型。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">18</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“一个简单的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序”：介绍一个</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序的代码执行流</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">程，主要作用是呈现程序中调用了哪些</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Verbs API</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 及其调用顺序，其工作流程中的每</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">个步骤在第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章和第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">20</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章中都有单独的小节进行详细描述。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 主要元素的实现”：按照应用程序中的调用顺序，依次对分配</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">PD</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">注册</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、创建</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CQ</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、创建</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">QP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、修改</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">QP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 等步骤的具体实现方法进行详细阐述。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">20</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“进行一次数据传输”：分析一次数据传输过程，主要包含发起数据传输的</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA Write</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和确认传输结束的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Poll CQ</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作，并汇总在各种元素的创建和数据传输</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">过程中软件和硬件的行为。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MAC</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">”：专门针对常用的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 协议，介绍</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">如何在本机网卡中配置通信所需的本地和对端设备的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MAC</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">22</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 性能测试工具——</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">perftest</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">”，引入一个对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 设备进行性能测</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">试的软件工具，并介绍它的安装和使用方法。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">3 </span>

<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">前言 </span>

---

## Page 11

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">前言 </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">第4 部分，XDP </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">23</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 简介”：对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 及其依赖的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">BPF</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 技术进行基本介绍。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 教程代码分析”：分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 教程代码，学习如何编写</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 程序。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">25</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“简单的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 性能测试”：比较</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的性能。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">26</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章，“让网卡驱动程序支持</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 功能”：在分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Mellanox</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡驱动程序的基础</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">上，分析如何让一个网卡驱动程序支持</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 功能。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">本书特色 </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">1．图示代码解析 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">正如</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 内核创始人在一篇新闻稿上所说的，要理解一个软件系统的真正运行机制，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">一定要阅读其源代码。系统本身是一个整体，具有很多看似不重要的细节，但是这些细节对</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">于真正了解一个实际系统的实现方法和手段至关重要。通过解析代码，本书给出了相关网络</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">技术的详细解读，并以流程图的形式展示了一些关键的技术实现。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">2．图示软硬件交互流程 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">市面上介绍</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序的图书大都过于侧重软件，对软件和硬件的交互流程介绍得</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">不够详细，使得底层软件工程师在和硬件工程师在沟通时困难重重。本书在介绍相关技术的</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">实现方案时，描述了很多和软件操作对应的硬件行为，并以图示的方式描绘了软件和硬件之</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">间的交互方法与流程，开发各种网络设备的软件工程师和硬件工程师都能从中得到启发。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">3．提供作者编写的开源代码 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">12</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章描述了作者为</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 编写的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序，相关代码已上传至</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GitHub</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">站，读者可通过搜索关键词“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK-with-Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">”获取。当前已有多所大学的研究人员正</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">在使用此代码进行</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡、数据包卸载（</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Offload</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">）等领域的研究和开发工作，并向作者</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">发来了求助和感谢邮件。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">4．翔实的实现方法 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章、第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">22</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章、第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">25</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 章分别针对</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 技术给出了翔实的测</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">试和使用方法，即使对技术细节不感兴趣的读者，也可以根据这些章节的介绍直接上手操作，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">感知相关技术带来的实际效果。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.02pt;color:#000000">5．小知识 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">书中在多处插入了“小知识”栏目，带领大家对某些重要的技术细节寻根问底。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">本书读者对象 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书定位的读者是具有一定的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统和网络知识基础，在工作或学习中对高性</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">能网络有一定涉猎，但对其具体的工作原理缺乏了解，并希望进一步深入理解相关方案的底</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">层实现机制的爱好者和软件、硬件开发工程师，对网络设备研发工程师尤为适配。编写应用</span>
<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">4 </span>

---

## Page 12

<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">程序的工程师也可以从本书引入的一些代码示例和深度解析中获得助益。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">此外，对于计算机和网络技术的初学者，本书的背景知识部分介绍了计算机网络、计算</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">机硬件、操作系统、软硬件之间的信息交互机制等计算机科学相关的基础知识，可以帮助这</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">部分读者尽快建立相关知识体系。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">阅读本书需具备的基础知识 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">在阅读本书时，读者必须具备一些基本的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">C</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 语言知识。除此之外，最好也能对以下知识</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">有基本了解。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">计算机架构相关的知识，比如</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CPU</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">FPGA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">PCIe</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 总线等术语的意义和它们在计算机</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">中的角色与定位。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 操作系统的一些概念，比如虚拟地址、物理地址、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">swap</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 分区等。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书的第</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 部分对上述基础知识进行了介绍，但受篇幅所限，相关介绍均为点到即止。</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">所幸的是这些知识在互联网上可以很容易地找到。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">特别说明 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'SimSun';font-size:10.02pt;color:#000000">书</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">中在描述函数调用流程时，有时为简单起见，会采用类似“函数</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">A-&gt;</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">函数</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">B-&gt;</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">函数</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">C</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">”这种表达方式，意思是函数</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">A</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 调用函数</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">B</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，函数</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">B</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 再调用函数</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">C</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。其中的“</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">-&gt;</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">”</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">并非</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">C</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 语言中的指针。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">代码或表格中的数据类型</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">__le32</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">，表示小端的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">32</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 位整数。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">由于本书是单色印刷，在以图片的形式解释某些技术细节时，区分度不是很好。为此，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书提供了所有图片的电子文件供读者下载学习。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书有部分内容参考了一些产品手册或论文。为了保证与产品手册或论文的一致性，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">以及与业内名称的一致性，本书中保留了原有的术语，尽管这些术语的表示从出版行</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">业来看是不恰当的。比如</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">（</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">E</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">）网卡，其更为准确的表示应该是</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100Gbit/s</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡；</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">再如千兆以太网，按照出版行业的修改规范应该是“吉比特以太网”。请各位读者在</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">阅读时多加注意。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">本书使用的软件版本 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书中使用的各主要软件及其版本如下。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 内核：</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">5.8.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">：</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">20.11</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">rdma-core</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">：从</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GitHub</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 下载的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2020/8/18</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的版本，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">commit ID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 为</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">526d559740c7599e1f2 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">d533658797290d739554a</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">dpdk-pktgen</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">：</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.03.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">• </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">xdp-tutorial</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">：从</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GitHub</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 下载的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2021/12/14</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的版本，</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">commit ID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 为</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">42ecabf9f6156fefb4 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">09a693d07971355546869d</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">5 </span>

<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">前言 </span>

---

## Page 13

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">前言 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">6 </span>

---

## Page 14

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书由异步社区出品，社区（</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">https://www.epubit.com/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">）为您提供相关资源和后续服务。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">配套资源 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书提供如下资源：</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'Wingdings';font-size:10.02pt;color:#000000"></span><span style="font-family:'Arial';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书图文件。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">要获得以上配套资源，请在异步社区本书页面中单击“配置资源”，跳转到下载界面，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">按提示进行操作即可。注意：为保证购书读者的权益，该操作会给出相关提示，要求输入提</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">取码进行验证。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">提交勘误 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">作者和编辑尽最大努力来确保书中内容的准确性，但难免会存在疏漏。欢迎您将发现的</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">问题反馈给我们，帮助我们提升图书的质量。</span><span style="font-family:'Calibri';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">当您发现错误时，请登录异步社区，按书名搜索，进入本书页面，单击“提交勘误”，输入</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">勘误信息，单击“提交”按钮即可。本书的作者和编辑会对您提交的勘误进行审核，确认并接受</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">后，您将获赠异步社区的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 积分。积分可用于在异步社区兑换优惠券、样书或奖品。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TT9245412FtCID';font-size:21.81pt;color:#000000">资源与支持 </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p014_img01.png)

---

## Page 15

<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">资源与支持 </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">扫码关注本书 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">扫描下方二维码，您将会在异步社区微信服务号中看到本书信息及相关的服务提示。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">与我们联系 </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">我们的联系邮箱是</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">contact@ptpress.com.cn</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">如果您对本书有任何疑问或建议，请您发邮件给我们，并请在邮件标题中注明本书书名，</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">以便我们更高效地做出反馈。</span><span style="font-family:'Calibri';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">如果您有兴趣出版图书、录制教学视频，或者参与图书翻译、技术审校等工作，可以发邮件给</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本书的责任编辑（</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">fudaokun@ptpress.com.cn</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">）。</span><span style="font-family:'Calibri';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">如果您所在的学校、培训机构或企业，想批量购买本书或异步社区出版的其他图书，也</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">可以发邮件给我们。</span><span style="font-family:'Calibri';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">如果您在网上发现有针对异步社区出品图书的各种形式的盗版行为，包括对图书全部或</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">部分内容的非授权传播，请您将怀疑有侵权行为的链接发邮件给我们。您的这一举动是对作</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">者权益的保护，也是我们持续为您提供有价值的内容的动力之源。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:13.98pt;color:#000000">关于异步社区和异步图书 </span>
<span style="font-family:'SimHei';font-size:10.02pt;color:#000000">“异步社区”</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">是人民邮电出版社旗下</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IT</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 专业图书社区，致力于出版精品</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IT</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 技术图书和相</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">关学习产品，为作译者提供优质出版服务。异步社区创办于</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2015</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 年</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">8</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 月，提供大量精品</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IT</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">技术图书和电子书，以及高品质技术文章和视频课程。更多详情请访问异步社区官网</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">https://www.epubit.com</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。</span><span style="font-family:'Calibri';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'SimHei';font-size:10.02pt;color:#000000">“异步图书”</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">是由异步社区编辑团队策划出版的精品</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IT</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 专业图书的品牌，依托于人民邮</span>
<span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">电出版社近</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">30</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 年的计算机图书出版积累和专业编辑团队，相关图书在封面上印有异步图书的</span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">LOGO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">。异步图书的出版领域包括软件开发、大数据、</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">AI</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">、测试、前端、网络技术等。</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">                   </span>

<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">                    </span><span style="font-family:'SimHei';font-size:10.50pt;color:#000000">异步社区</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">                    </span><span style="font-family:'SimHei';font-size:10.50pt;color:#000000">微信服务号</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">2 </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> </span>

![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img01.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img02.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img03.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img04.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img05.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img06.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img07.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img08.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img09.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img10.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img11.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img12.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img13.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img14.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img15.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img16.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img17.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img18.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img19.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img20.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img21.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img22.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img23.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img24.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img25.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img26.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img27.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img28.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img29.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img30.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img31.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img32.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img33.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img34.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img35.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img36.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img37.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img38.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img39.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img40.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img41.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img42.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img43.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img44.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img45.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img46.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img47.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img48.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img49.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img50.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img51.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img52.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img53.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img54.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img55.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img56.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img57.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img58.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img59.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img60.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img61.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img62.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img63.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img64.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img65.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img66.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img67.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img68.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img69.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img70.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img71.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img72.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img73.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img74.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img75.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img76.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img77.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img78.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img79.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img80.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img81.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img82.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img83.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img84.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img85.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img86.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img87.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img88.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img89.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img90.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img91.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img92.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img93.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img94.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img95.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img96.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img97.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img98.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img99.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img100.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img101.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img102.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img103.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img104.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img105.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img106.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img107.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img108.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img109.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img110.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img111.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img112.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img113.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img114.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img115.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img116.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img117.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img118.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img119.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img120.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img121.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img122.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img123.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img124.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img125.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img126.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img127.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img128.png)



![](./Linux高性能网络详解从DPDK、RDMA到XDP (刘伟) (Z-Library)_assets/images/p015_img129.png)

---

## Page 16

<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>

<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第1 章 计算机网络概述</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> .............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">3</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">1.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 计算机网络的定义和构成</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">1.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 计算机网络的体系结构</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">4 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">1.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 常见术语</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">6</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第2 章 计算机硬件</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> .................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 中央处理器</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">10 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2.1.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 处理器体系结构</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2.1.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Cache</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ..................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">12 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">2.1.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">NUMA</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">17 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 存储器</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">2.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 总线</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">2.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网卡</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">22 </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第3 章 Linux 操作系统</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> .............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">25</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 操作系统的诞生和发展</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">25 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 用户态和内核态</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">27 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 虚拟地址、物理地址和页表</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">28 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 用户空间和内核空间</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">30 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 内核的组成</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">31 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">3.5.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 内核源代码的目录结构</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">............................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">31 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">3.5.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 内核的主要组成部分</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">32</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">3.6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 设备驱动程序</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">35 </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第4 章 软件和硬件之间传递信息的方式</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">37</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">4.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 寄存器</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">37 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">4.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 数据缓存</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">38 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">4.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 队列和描述符</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">39 </span>

<span style="font-family:'TT9245412FtCID';font-size:22.02pt;color:#000000">目   录           </span>

<span style="font-family:'TTB78D40B0tCID';font-size:13.98pt;color:#000000">第1 部分 背景知识 </span>

---

## Page 17

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">目录 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">4.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 中断</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">43 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">4.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DMA</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">46</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第5 章 内核协议栈方案及其存在的问题</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">47</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">5.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 内核协议栈方案的工作过程</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">47 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">5.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 内核协议栈方案的数据流</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">49 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">5.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 内核协议栈方案的缺点</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">50</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第6 章 Corundum</span><span style="font-family:'TT21D4BC5CtCID';font-size:10.50pt;color:#000000">——</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">一个开源的基于FPGA 的100G 网卡方案</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ............................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">51</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">6.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 方案简介</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">51 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">6.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 的队列</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">54 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">6.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Corundum</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 的</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网络设备驱动程序解析</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">56</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序源码概览</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">57 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序的编译和使用</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">57 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序的加载和注册</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">58 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序和设备的匹配</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">59 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 初始化阶段</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">60 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 打开网络接口</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">72 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 数据发送</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">75 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.8</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 中断处理</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">81 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.9</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 发送完成处理</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">85 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">6.3.10</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 数据接收</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">89</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>

<span style="font-family:'TTB78D40B0tCID';font-size:13.98pt;color:#000000">第2 部分  DPDK </span>

<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第7 章 认识DPDK</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">97</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">7.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 为什么需要</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">97 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">7.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 体系结构</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">98</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">7.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 核心组件</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">98</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">7.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 轮询模式驱动</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ..................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">100</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">7.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 一个典型的</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 应用程序</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">102</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第8 章 DPDK 的内存管理</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ........................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">104</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">8.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 影响数据包处理速度的内存问题</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">104</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">8.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 大页</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">105</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">8.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 在</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 系统中预留和配置大页</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ...................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">105 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">8.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的大页管理</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">106</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">8.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">mempool</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">111 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">8.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 通道和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">rank</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">113 </span>
<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">2 </span>

---

## Page 18

<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">目录 </span>

<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">8.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 使用的内存管理技巧总结</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">114</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第9 章 UIO</span><span style="font-family:'TT21D4BC5CtCID';font-size:10.50pt;color:#000000">——</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">DPDK 的基石</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">115</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">9.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">UIO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 驱动程序的构成</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">115 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">9.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 应用程序和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">UIO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 的交互方式</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">118 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">9.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">UIO</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 驱动程序的</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">API</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">119 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">9.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 如何使用</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">UIO</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">120</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第10 章 DPDK 的基本使用方法</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ............................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">123</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">10.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 编译</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">123</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">10.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 使用</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">dpdk-testpmd</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 进行数据包转发测试</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">123</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 运行环境和连接方式</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">124 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 使用</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Linux</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 以太网驱动程序运行</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">dpdk-testpmd</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">125 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.2.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 使用轮询模式驱动程序运行</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">dpdk-testpmd</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">129</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">10.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 使用</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">pktgen</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 测试</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Mellanox ConnectX-4 LX 10G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网卡</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">132</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件环境</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">132</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 软件版本</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">132</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 安装</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Mellanox</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡驱动程序</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .......................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">132</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 编译和安装</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">133</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> “回环</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">+</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">转发”测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">133</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 编译</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">pktgen</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">138</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> “外部发包</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">+</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000">本地转发”测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ......................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">138</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">10.3.8</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 测试过程中可能遇到的问题及解决方法</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">141</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第11 章 测试和分析高性能网卡</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ............................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">142</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">11.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 关于</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DDR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 访问速率的思考和测试</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">142</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.1.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件配置和软件版本</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">143</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.1.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DDR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 理论速率</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">144</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.1.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 内存性能测试工具</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">mbw</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">145</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.1.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 单核测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">146</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.1.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 多核测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">148</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">11.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 基于</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">100G</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网卡的单核和多核测试</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">150</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件配置</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">150</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 软件版本和配置</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">150</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.2.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 单核测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">150</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.2.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 双核测试</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">152</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.2.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 测试结果总结</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">153</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">11.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 使用</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Intel VTune Profiler</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 定量分析</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">DPDK</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">154</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件环境和软件版本</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">154</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Intel VTune Profiler</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的下载和安装</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">155</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">3 </span>

---

## Page 19

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">目录 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 测试模型</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">155</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 重新编译安装</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">155</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 使用</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Intel VTune Profiler</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 启动和监控</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">dpdk-testpmd</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ...................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">156</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 开始产生和发送数据包</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">159</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">11.3.7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 统计和分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">159</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第１2 章 为Corundum 编写DPDK 驱动程序</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ........................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">163</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Corundum DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 驱动程序的组成</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ..................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">164 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 注册和打开调试日志</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">164</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">12.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的日志级别</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">164 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">12.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">Corundum DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 驱动程序的日志</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">165</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Corundum DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 驱动程序的注册</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ..................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">167</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Corundum DPDK</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 驱动程序的初始化</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">168 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 启动队列</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">173 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 数据发送</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">176 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">12.7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 编写驱动程序时的注意事项</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">............................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">179</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>

<span style="font-family:'TTB78D40B0tCID';font-size:13.98pt;color:#000000">第3 部分 RDMA </span>

<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第13 章 RDMA 技术简介</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">185</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">13.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 的控制通路和数据通路</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">185 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">13.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 的优势</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ..................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">188 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">13.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 协议</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">189 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">13.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">InfiniBand</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">190</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">13.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCE</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">190</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">13.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">iWARP</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">192</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">13.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网络构成</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">192 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">13.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">LID</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">194</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">13.5.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">LID</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">194</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">13.5.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">195</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第14 章 RDMA 软件架构</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">198</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">14.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">rdma-core</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">198 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">14.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 内核</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 子系统</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">199 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">14.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 软件架构总览</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">201</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第15 章 RDMA 基本元素</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">202</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">15.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">WQ</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">WQE</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">202 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">15.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">QP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">QPN</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">203 </span>
<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">4 </span>

---

## Page 20

<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">目录 </span>

<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">15.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">CQ</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">CQN</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">205 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">15.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">WR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">WC</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">206 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">15.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 基本元素总结</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">207</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第16 章 RDMA 基本操作类型及其配套机制</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ............................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">208</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Send</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Receive</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">.................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">208 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA Write</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">209 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA Read</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">210 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 其他</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 操作类型</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">212 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 操作类型总结</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">213 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.6</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Memory Region</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">214</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">16.6.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的基本概念</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">215</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">16.6.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的作用之一</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">215</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">16.6.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的作用之二</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">216</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">16.6.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的作用之三</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">217</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.7</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">PD</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">218 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.8</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">Doorbell</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 机制</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">219 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">16.9</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RDMA</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 各种元素的实体形式</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">.............................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">220</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第17 章 RDMA 传输服务</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">222</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">17.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 传输服务维度一</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">——</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">可靠</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">不可靠</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ..................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">222 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">17.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 传输服务维度二</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">——</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">连接</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">/</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">数据报</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ..................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">223 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">17.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 传输服务类型</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">225</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第18 章 一个简单的RDMA 应用程序</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ..................................................................... </span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">229 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">18.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 程序的执行和输出</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">229 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">18.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 代码执行流程</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ...................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">230</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第19 章 RDMA 主要元素的实现</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> .............................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">234</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 分配</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">PD</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">234</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 注册</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">MR</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">240</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 代码执行流程分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">240</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 注册</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的具体工作</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">242</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.2.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件查表获取</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 物理地址的过程</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">246</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.2.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MR</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 相关的软硬件行为汇总</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">248</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 创建</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">CQ</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">249</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 代码执行流程分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">249</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CQ buffer</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的组织形式</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ...................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">253</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CQ Context</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的组织形式</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">255</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.3.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件获取</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CQE</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 地址的过程</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">257</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">5 </span>

---

## Page 21

<span style="font-family:'TT8BBAFAAFtCID';font-size:7.50pt;color:#ffffff">第1 章</span><span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#ffffff">  </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">目录 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.3.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">CQ</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 相关的软硬件行为汇总</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">............................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">258</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 创建</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">QP</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">258</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.4.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 代码执行流程分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">258</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.4.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">QP buffer</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的组织形式</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ...................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">263</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.4.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">QP Context</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 的组织形式</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">264</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">19.5</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 修改</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">QP</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">265</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.5.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序修改</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">QP</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">265</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.5.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 代码执行流程分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">267</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">19.5.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 硬件获取</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">WQE</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 地址的过程</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">270</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第20 章 进行一次数据传输</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ...................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">272</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">20.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 发起数据传输</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">——RDMA Write</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ......................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">272 </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">20.1.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序发起数据传输</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">272</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">20.1.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 代码执行流程分析</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">273</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">20.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 确认数据传输完毕</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">——</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">轮询</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">CQ</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">276</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">20.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 软件和硬件行为汇总</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">277</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第21 章 RoCEv2 网卡的MAC、IP 和GID</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> .............................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">279</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">21.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网卡的</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">279</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">21.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 向</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网卡配置自己的</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">MAC</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">IP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">281</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 获取</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡自己的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MAC</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ..................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">281</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 获取</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡自己的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 地址</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">281</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.2.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 配置</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡自己的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">0</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 号</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">281</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.2.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 配置</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡自己的非</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">0</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 号</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">282</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">21.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 向</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 网卡配置对端设备的</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">MAC</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000">、</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">IP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................ </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">282</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序获取本地和对端设备的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">0</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 号</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">283</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 应用程序获取对端设备的非</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">0</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 号（</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 号）</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">GID</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">284</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 向</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡配置对端设备的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">MAC</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 地址</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">284</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">21.3.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 向</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">RoCEv2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 网卡配置对端设备的</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">IP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 地址</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ...................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">285</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第22 章 RDMA 性能测试工具</span><span style="font-family:'TT21D4BC5CtCID';font-size:14.89pt;color:#000000">—</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">perftest</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ............................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">286</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">22.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 源码获取和安装</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">286 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">22.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 测试方法和注意事项</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .......................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">286 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">22.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 测试选项</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">287 </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">22.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 简单的测试过程和结果呈现</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">............................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">289</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>

<span style="font-family:'TTB78D40B0tCID';font-size:13.98pt;color:#000000">第4 部分 XDP </span>

<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第23 章 XDP 简介</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">293</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'Arial';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'Calibri';font-size:9.00pt;color:#000000">6 </span>

---

## Page 22

<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000"> </span>
<span style="font-family:'TTCBCAC711tCID';font-size:9.00pt;color:#000000">目录 </span>

<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">23.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 什么是</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">BPF</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 和</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">eBPF</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">293</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">23.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 系统架构</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ..................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">294</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">23.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 程序的执行流程</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ...................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">295</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">23.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">BPF map</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">297</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第24 章 XDP 教程代码分析</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ..................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">298</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">24.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">xdp-tutorial</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 代码获取和编译</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> ............................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">298</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">24.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 基础课程</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .............................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">299</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.2.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">XDP</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 程序的加载和卸载</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> .................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">299</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.2.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 按名称加载</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">SEC</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">301</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.2.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 使用</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">BPF map</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">302</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.2.4</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 多程序交流和共享</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ........................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">304</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">24.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#000000"> 数据包处理课程</span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000"> .................................................................................................. </span><span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">308</span><span style="font-family:'DengXian';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.3.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 解析数据包</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">308</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.3.2</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 改写数据包</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ....................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">311</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">24.3.3</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.02pt;color:#000000"> 重定向</span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000"> ............................................................................................................... </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">313</span><span style="font-family:'DengXian';font-size:10.02pt;color:#000000"> </span>
<span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000">第25 章 简单的XDP 性能测试</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> ................................................................................ </span><span style="font-family:'TimesNewRoman';font-size:10.02pt;color:#000000">319</span><span style="font-family:'TTF59E640FtCID';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'TimesNewRoman';font-size:10.50pt;color:#000000">25.1</span><span style="font-family:'TT3FBCD30AtCID';font-size:10.50pt;color:#0