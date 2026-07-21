## Page 1

JVM G1
源码分析和调优
JVM G1 Implementation and Performance Tuning
彭成寒编著

<!-- 图源: ./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p001_scan.jpg -->

---

## Page 2

<span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">目录</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">1. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">目录</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">2. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">封面</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">3. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">版权信息</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">4. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">前言</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">5. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">6. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.1 Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发展概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">7. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书常见术语</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">8. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">回收算法概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">9. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.3.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分代管理算法</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">10. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.3.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">复制算法</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">11. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.3.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标记清除</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">12. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.3.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标记压缩</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">13. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.3.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法小结</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">14. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.4 JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收器概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">15. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.4.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">串行回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">16. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.4.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">17. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.4.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">18. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">1.4.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾优先回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">19. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的基本概念</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">20. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">21. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.2 G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停顿预测模型</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">22. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">卡表和位图</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">23. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象头</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">24. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">内存分配和管理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">25. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.6 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">26. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.6.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">栈帧</span>

---

## Page 3

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">27. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.6.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">句柄</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">28. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.6.3 JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本地方法栈中的对象</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">29. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.6.4 Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本地方法栈中的对象</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">30. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.7 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">31. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">2.8 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">32. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的对象分配</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">33. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象分配概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">34. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">快速分配</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">35. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">慢速分配</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">36. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.3.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大对象分配</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">37. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.3.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">最后的分配尝试</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">38. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.4 G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收的时机</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">39. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.4.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分配时发生回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">40. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.4.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">外部调用的回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">41. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">3.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">42. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">43. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">记忆集</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">44. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.2 Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程的功能及原理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">45. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.2.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">抽样线程</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">46. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.2.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">管理</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">RSet</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">47. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.2.3 Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">处理</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">DCQ</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">48. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.2.4 Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程的工作原理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">49. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.3 Refinement Zone</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">50. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.4 RSet</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">涉及的写屏障</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">51. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">52. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">4.6 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">53. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新生代回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">54. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.1 YGC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">55. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.2 YGC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代码分析</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">56. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.2.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行任务</span>

---

## Page 4

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">57. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.2.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">其他处理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">58. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3 YGC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法演示</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">59. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">选择</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">CSet</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">60. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">根处理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">61. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3.3 RSet</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">处理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">62. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">复制</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">63. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3.5 Redirty</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">64. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.3.6 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">释放空间</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">65. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">66. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.4.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大对象日志分析</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">67. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.4.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象年龄日志分析</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">68. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">5.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">69. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">混合回收</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">70. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记算法详解</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">71. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记算法的难点</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">72. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.2.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">三色标记法</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">73. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.2.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">难点示意图</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">74. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.2.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">再谈写屏障</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">75. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.3 G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中混合回收的步骤</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">76. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">混合回收中并发标记处理的线程</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">77. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记线程启动的时机</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">78. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">根扫描子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">79. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">80. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">再标记子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">81. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">清理子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">82. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.4.6 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">启动混合收集</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">83. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记算法演示</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">84. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.5.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">初始标记子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">85. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.5.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">根扫描子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">86. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.5.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记子阶段</span>

---

## Page 5

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">87. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.5.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">再标记子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">88. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.5.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">清理子阶段</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">89. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.6 GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">活动图</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">90. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.7 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">91. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">6.8 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数优化</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">92. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> Full GC</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">93. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.1 Evac</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">失败</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">94. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">串行</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">FGC</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">95. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.2.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标记活跃对象</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">96. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.2.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">计算对象的新地址</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">97. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.2.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">更新引用对象的地址</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">98. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.2.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">移动对象完成压缩</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">99. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.2.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">后处理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">100. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">FGC</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">101. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.3.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行标记活跃对象</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">102. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.3.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">计算对象的新地址</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">103. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.3.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">更新引用对象的地址</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">104. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.3.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">移动对象完成压缩</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">105. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.3.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">后处理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">106. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">107. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">7.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">108. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的引用处理</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">109. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">引用概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">110. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可回收对象发现</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">111. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时的处理发现列表</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">112. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">重新激活可达的引用</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">113. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">114. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">8.6 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">115. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的新特性：字符串去重</span>

---

## Page 6

<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">116. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">9.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">字符串去重概述</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">117. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">9.2 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志解读</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">118. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">9.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">119. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">9.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">字符串去重和</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">String.intern</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的区别</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">120. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">9.5 String.intern</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的实现</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">121. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程中的安全点</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">122. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">安全点的基本概念</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">123. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.2 G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发线程进入安全点</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">124. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.3 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">解释线程进入安全点</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">125. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.4 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">编译线程进入安全点</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">126. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.5 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">正在执行本地代码的线程进入安全点</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">127. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.6 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">安全点小结</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">128. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.7 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日志分析</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">129. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">10.8 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数介绍和调优</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">130. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收器的选择</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">131. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">11.1 </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何衡量垃圾回收器</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">132. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">11.2 G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">调优的方向</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">133. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新一代垃圾回收器</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">134. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">12.1 Shenandoah</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">135. </span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">12.2 ZGC</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">136. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">附录</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">A </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">编译调试</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">JVM</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">137. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">附录</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">B </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本地内存跟踪</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">138. </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">附录</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">C </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">阅读</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">需要了解的</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#0000ee">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">知识</span>

---

## Page 7

---

## Page 8

<span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">版权信息</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">书名：</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">JVM G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">源码分析和调优</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作者：彭成寒</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出版社：机械工业出版社</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出版时间：</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">2019-03-01</span>
<span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">ISBN</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">：</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000">9787111621973</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">品牌方：机械工业出版社有限公司</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书由机械工业出版社有限公司授权微信读书进行制作与发行</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">版权所有</span><span style="font-family:'LiberationSerif';font-size:15.00pt;color:#000000"> · </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">侵权必究</span>

---

## Page 9

<span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">前言</span>

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是目前最成熟的垃圾回收器，已经广泛应用在众多公司的生产</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">环境中。我们知道，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为使用最为广泛的垃圾回收器，也有令人</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">头疼的问题，即如何对其众多的参数进行正确的设置。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的目标就是</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">替代</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，所以在设计之初就希望降低程序员的负担，减少人工的介</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">入。但这并不意味着我们完全不需要了解</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的原理和参数调优。笔者</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在实际工作中遇到过一些因参数设置不正确而导致</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停顿时间过长的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">问题。但要正确设置参数并不容易，这里涉及两个方面：第一，需要</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的原理熟悉，只有熟悉</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的原理才知道调优的方向；第二，能</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分析和解读</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">运行的日志信息，根据日志信息找到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">运行过程中的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">异常信息，并推断哪些参数可以解决这些异常。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书尝试从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的原理出发，系统地介绍新生代回收、混合回收、</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、并发标记、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程等内容；同时依托于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">jdk8u</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的源代码</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Hotspot</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何实现</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，通过对源代码的分析来了解</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">提供了哪些</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">参数、这些参数的具体意义；最后本书还设计了一些示例代码，给出</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在运行这些示例代码时的日志，通过日志分析来尝试调整参数并</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">达到性能优化，还分析了参数调整可能带来的负面影响。</span>

---

## Page 10

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">乍听起来，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">非常复杂，应该会有很多的参数。实际上在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK8</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">实现中，一共新增了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">93</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个参数，其中开发参数（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">develop</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）有</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">41</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个，产品参数（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">product</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）有</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">31</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个，诊断参数（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">diagnostic</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）有</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个，实</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">验参数（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">experimental</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）有</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个。开发参数需要在调试版本中才能进</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">行验证（本书只涉及个别参数），其余的三类参数都可以在发布版本</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中打开、验证和使用。本书除了几个用于验证的诊断参数外，覆盖了</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发布版本中涉及的所有参数，为读者理解</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">以及调优</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">提供了帮</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">助。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书共分为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章，主要内容如下：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍垃圾回收的发展及使用的算法，同时还介绍一些重要</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并常见的术语。该章的知识不仅仅限于本书介绍的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，对于研读</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">文章或者</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">源码都有帮助。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的基本概念，包括分区、卡表、根集合、线程栈</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等和垃圾回收相关的基本知识点。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是如何分配对象的，包括</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">TLAB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和慢速分配，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象分配和其他垃圾回收器的对象分配非常类似，只不过在分配的时</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">候以分区为基础，除此之外没有额外的变化，所以该章知识不仅仅适</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">也适用于其他垃圾回收器，最后介绍了参数调优，同样也适用</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">于其他的垃圾回收器。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1 Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程，包括</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何管理和处理代际引用，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">从而加快垃圾回收速度，介绍了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Refinement</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">调优涉及的参数；虽然</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">也有卡表处理代际引用，但是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的处理和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并不相同，</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新引入的部分。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍新生代回收，包括</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何进行新生代回收，包括对象</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标记、复制、分区释放等细节，还介绍了新生代调优涉及的参数。</span>

---

## Page 11

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍混合回收。主要介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的并发标记算法及其难点，以</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">及</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中如何解决这个难点，同时介绍了并发标记的步骤：并发标记、</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Remark</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（再标记）和清理阶段；最后还介绍了并发标记的调优参数。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对整个堆进行垃圾回收，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">该章介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的串行</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">之后的并行</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍垃圾回收过程中如何处理引用，该功能不是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">独有</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的，也适用于其他垃圾回收器。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的新特性：字符串去重。根据</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的官方文</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">档，该特性可平均节约内存</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">13%</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">左右，所以这是一个非常有用的特</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">性，值得大家尝试和使用。另外，该特性和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">String</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">类的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">intern</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">方</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">法有一些类似的地方，所以该章还比较了它们之间的不同。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍线程中的安全点。安全点在实际调优中涉及的并不</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">多，所以很多人并不是特别熟悉。实际上，垃圾回收发生时，在进入</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">安全点中做了不少的工作，而这些工作基本上是串行进行的，这些事</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">情很有可能导致垃圾回收的时间过长。该章除了介绍如何进入安全点</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">之外，还介绍了在安全点中做的一些回收工作，以及当发现它们导致</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">过长时该如何调优。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍如何选择垃圾回收器，以及选择</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">遇到问题需要调</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">优时我们该如何下手。该章属于理论性的指导，在实际工作中需要根</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">据本书提到的参数正面影响和负面影响综合考虑，并不断调整。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍了下一代垃圾回收器</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Shenandoah</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">ZGC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发挥重要作用的垃圾回收器仍有不足之处，因此未来的垃圾回收器仍</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">会继续发展，该章介绍了下一代垃圾回收器</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Shenandoah</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">ZGC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的改进之处及其工作原理。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书的附录包含如下内容：</span>

---

## Page 12

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">附录</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">A</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">介绍如何开始阅读和调试</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代码。这里简单介绍了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代码架构和组织形式。另外简单介绍了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Linux</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的调试工具</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GDB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，这个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">工具对于想要了解</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">细节的同学必不可少。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">附录</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">B</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">介绍如何使用</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NMT</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">内存进行跟踪和调试。这个知识</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对于想要深入理解</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">内存的管理非常有帮助，另外在实际工作中，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">特别是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">升级中我们必须比较同一应用在不同</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">运行情况下的内</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">存使用。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">附录</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">介绍了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">程序员阅读</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时需要知道的一些</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">知识。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这里并未罗列</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的语法以及语法特性，仅仅介绍一些</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言特有</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的、而</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言没有的语法，或者</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言中的使用或理解不同于</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言的部分语法。这个知识是为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">程序员准备的，特别是为在</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">阅读</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代码时准备的。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中出现，经历</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的发展，到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">已经相当成熟，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">之后</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">就作为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的默认垃圾回收器。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">司长期支持的版本，本书主要基于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">进行分析，所用的版本是</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">jdk8u60</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。在第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章中为了扩展读者的视野，追踪最新的技术，还介绍</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的并行</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。读者可以自行到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的官网下载，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">也可以使用笔者在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GitHub</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的备份（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#0000ee">https://github.com/chenghanpeng/jdk8u60</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#0000ee">https://github.com/chenghanpeng/jdk10u</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书在分析源码的时候会给出源代码所属的文件，例如在介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区类型时，指出源代码位于</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">hotspot/src/share/vm/gc_implementation/g1/heapRegionType.hpp</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这里的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">hotspot</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">就是你下载的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">jdk8u60</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代码里面的一级目录。如果你不</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">希望在本地保留源代码可以直接浏览网址</span>

---

## Page 13

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#0000ee">https://github.com/chenghanpeng/jdk8u60</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，在此你可以找到这个一</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">级目录</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">hotspot</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，然后通过逐个查看子目录</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">src</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">share</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">vm</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">gc_implementation</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">g1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">就可以找到源文件</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">heapRegionType.hpp</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">需要注意的是，在分析源码的时候为了节约篇幅，通常会对原始</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的代码进行一些调整，例如删除一些大括号、统计信息、打印信息，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">或者删除一些不影响理解原理和算法的代码，大家在和源码比较时需</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">要注意这些变化。另外对于定义在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">header</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">文件和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">cpp</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">文件中的一些函</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">数，为了使代码紧凑，通常会忽略头文件中的定义，直接按照</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语法，即类名</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">::</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">成员函数的方式给出源码，这样的代码可能和原文件</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">不完全一致，但是完全符合</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">C++</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言的组织，阅读源码时要注意将定</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">义和实现分开。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">由于笔者水平有限，时间仓促，书中难免出现一些错误或者不准</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">确的地方，恳请读者批评指正。可以通过</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#0000ee">https://github.com/chenghanpeng/jdk8u60/issues</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">进行讨论，期待能</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">够得到读者朋友们的真情反馈，在技术道路上互勉共进。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在本书的写作过程中，得到了很多朋友以及同事的帮助和支持，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在此表示衷心的感谢！</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">感谢吴怡编辑的支持和鼓励，在写作过程中给出了非常多的意见</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和建议，不厌其烦地认真和笔者沟通，力争做到清晰、准确、无误。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">感谢你的耐心，为你的专业精神致敬！</span>

---

## Page 14

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">感谢我的家人，特别是谢谢我的儿子，体谅爸爸牺牲了陪伴你的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时间。有了你们的支持和帮助，我才有时间和精力去完成写作。</span>

---

## Page 15

<span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">第</span><span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#a47000">1</span><span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">章</span><span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">垃圾回收概述</span>

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的发展已经超过了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">20</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年，已是最流行的编程语言。为了更好</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">地了解和使用</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，越来越多的开发人员开始关注</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">虚拟机</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）的实现技术，其中垃圾回收（也称垃圾收集）是最热门的技</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">术点之一。目前</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中最新、最成熟的垃圾回收器受到很多的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">人关注，本书从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的原理出发，介绍新生代收集、混合收集、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、并发标记、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Evacuation</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等内容。本章先回顾</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发展历程，然后介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中一些常用的概念以便与读者统一术语，随</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">后介绍垃圾回收的主要算法以及</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中实现了哪些垃圾回收的算法。</span>

---

## Page 16

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">1.1</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">Java</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">发展概述</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">平台和语言最开始是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">SUN</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1990</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月进行的一个内</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">部研究项目，我们通常所说的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一般泛指</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java Developer</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Kit</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），它既包含了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言和开发工具，也包含了执行</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的虚拟机</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java Virtual Machine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1996</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">23</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日开始，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 1.0</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">版本正式发布，到如今</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">已经经历了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">23</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个春秋。以下是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发展历</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">程中值得纪念的几个时间点：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·1998</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">迎来了一个里程碑版本</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1.2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。其技术体系被分</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为三个方向，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">J2SE</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">J2EE</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">J2ME</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。代表技术包括</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">EJB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java Plug-</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Swing</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">；虚拟机第一次内置了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JIT</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">编译器；语言上引入了</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Collections</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">集合类等。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·2000</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK1.3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发布。在该版本中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Hotspot</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">正式成为默认</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的虚拟机，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Hotspot</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1997</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">SUN</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司收购</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">LongView Technologies</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">司而获得的。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·2002</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">13</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK1.4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发布。该版本是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">走向成熟的一个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">版本。从此之后，每一个新的版本都会增加新的特性，比如</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">改进</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了内存模型、支持泛型等；</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">增强了锁同步等；</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">正式支持</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收、升级类加载的架构等；</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">支持函数式编程等。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·2006</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">13</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JavaOne</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大会上，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">SUN</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司宣布最终会把</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开源，由</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">组织对这些源码独立管理，从此之后</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">程</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">序员多了一个研究</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的官方渠道。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·2009</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">20</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司宣布正式以</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">74</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">亿美元的价格收购</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">SUN</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">商标从此正式归</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所有，自此</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的管</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">理和发布进入了一个新的时期。</span>

---

## Page 17

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">随着时间的推移，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">也已经正式发布，但是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 9</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并不是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">长期支持的版本（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Long Term Support</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），这</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">意味着</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">只是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的一个过渡版本，它们只用于整合</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新的特性，当下一个版本发布之后，这些过渡版本将不再更新维护。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2018</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">25</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">日</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">正式发布，随着新版本的发布，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司未</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">来对</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的支持也会变化。按照现在的声明，从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2019</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月起对于商</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">业用户，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司对</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">不再提供公共的更新，从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2020</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">12</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月起</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对个人用户也不再提供公共的更新。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的替代者，一直吸引着众多</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开发者的目光，自</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">正式推出以来，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">不断地增强，并从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开始越来越成</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">熟，在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中都成为默认的垃圾回收器。实际上</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">也有越来越多的公司开始在生产环境中使用</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为垃圾回收器，有一</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">篇文章描述了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的基准测试（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">benchmark</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），表明</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">已经优</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">于其他的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。可以预见随着</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK 11</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的推出，会有越来越多的公司</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和个人使用</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为生产环境中的垃圾回收器。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的目标是在满足短时间停顿的同时达到一个高的吞吐量，适用</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">于多核处理器、大内存容量的系统。其实现特点为：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">短停顿时间且可控：</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对内存进行分区，可以应用在大内存系</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">统中；设计了基于部分内存回收的新生代收集和混合收集。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">高吞吐量：优化</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">工作，使其尽可能与</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发工作。设计</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了新的并发标记线程，用于并发标记内存；设计了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程并发处</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">理分区之间的引用关系，加快垃圾回收的速度。</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p017_img01.png)

---

## Page 18

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新生代收集指针对全部新生代分区进行垃圾回收；混合收集指不</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">仅仅回收新生代分区，同时回收一部分老生代分区，这通常发生在并</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">发标记之后；</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">指内存不足时需要对全部内存进行垃圾回收。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新引入的部分，指的是在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">运行的同时标记</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">哪些对象是垃圾，看到这里大家一定非常好奇</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">到底是怎么实现的，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">举一个简单的例子。比如你的妈妈正在打扫房间，扫房房间需要识别</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">哪些物品有用哪些无用，无用的物品就是垃圾。同时你正在房间活</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">动，活动的同时你可能往房间增加了新的物品，也可能把房间的物品</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">重新组合，也可能产生新的无用物品。最简单的垃圾回收器如串行回</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">收器的做法就是在打扫房间标识物品的时候，你要暂停一切活动，这</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个时候你的妈妈就能完美地识别哪些物品有用哪些无用。但最大的问</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">题就是需要你暂停一切活动直到房间里面的物品识别完毕，在实际系</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">统中意味着这段时间应用程序不能提供服务。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的并发标记就是在打</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">扫房间识别物品有用或者无用的同时，你还可以继续活动，怎么正确</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">做标记呢？一个简单的办法就是在打扫房间识别垃圾物品开始的时候</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">记录你增加了哪些物品，动过哪些物品。然后在物品标记结束的时候</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对这些变更过的物品重新标记一次，当然在这一次标记时需要你暂停</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一切活动，否则永远也没有尽头，这通常称为再标记（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Remark</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。这</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个就是所谓的增量并发标记，在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中具体的算法是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Snapshot-At-The-</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Beginning</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">SATB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），关于这个算法我们会在第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章详细介绍。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Refine</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程也是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新引入的，它的目的是为了在进行部分收集的时候加速识</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">别活跃对象，具体介绍参见第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章。</span>

---

## Page 19

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书依托于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">jdk8u</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的源代码来介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何实现</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，通过源代码</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的分析理解算法以及了解</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">提供的参数的具体意义；最后还会给出一</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">些例子，通过日志，分析该如何调整参数以达到性能优化。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这里提到的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">jdk8u</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是指</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的代码，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">SUN</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（现</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）推出的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开源代码，因为标准的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（这里指</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">版的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）会有一些内部功能的代码，那些代码在开源的时候并未公</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2017</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">月</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">公司宣布</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">将能自由切</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">换，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Oracle JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">也会依赖</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的代码进行构建，所以通常都是使</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的代码进行分析和研究。读者可以自行到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OpenJDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的官</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">网上下载源代码，值得一提的是，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的代码会随着</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">bug</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">修复不断改</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">变，所以为了保持阅读的一致性，我把本书使用的代码推送到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GitHub</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">上</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，也使用该版本进行编译调试。</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p019_img01.png)

---

## Page 20

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">1.2</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">本书常见术语</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">系统非常复杂，市面上有很多中英文书籍从不同的角度来介</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，其中都用到了很多术语，但是大家对某些术语的解释并不完</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">全相同。为了便于读者的理解，在这里统一定义和解释本书使用的一</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">些术语。这些术语有些是我们约定俗成的叫法，有些是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">里面的特</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">别约定，还有一些是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法引入的。为了保持准确性，这里仅仅解释</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这些术语的含义，后续会进一步解释相关内容，本书将尽量使用这里</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">定义的术语。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">parallelism</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），指两个或者多个事件在同一时刻发生，在</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">现代计算机中通常指多台处理器上同时处理多个任务。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">concurrency</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），指两个或多个事件在同一时间间隔内发</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">生，在现代计算机中一台处理器</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">“</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">同时</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">”</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">处理多个任务，那么这些任务</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">只能交替运行，从处理器的角度上看任务只能串行执行，从用户的角</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">度看这些任务</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">“</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">”</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">执行，实际上是处理器根据一定的策略不断地切</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">换执行这些</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">“</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">”</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的任务。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中，我们也常看到并行和并发。比如，典型的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">ParNew</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">般称为并行收集器，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一般称为并发标记清除（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Concurrent Mark</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Sweep</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。这看起来很奇怪，因为并行和并发是从处理器角度出发，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">但是这里明显不是，实际上并行和并发在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">被重新定义了。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#a47000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的并行，指多个垃圾回收相关线程在操作系统之上并发运</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">行，这里的并行强调的是只有垃圾回收线程工作，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应用程序都暂</span>

---

## Page 21

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停执行，因此</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">ParNew</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">工作的时候一定发生了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。本书提到的</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">***ParTask</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（例如</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1ParTask</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）指的就是在这些任务运行的时候应用</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">程序都必须暂停。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#a47000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的并发，指垃圾回收相关的线程并发运行（如果启动多个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">线程），同时这些线程会和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应用程序并发运行。本书提到的</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">***Concurrent***Thread</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（例如</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">ConcurrentG1RefineThread</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）就是指</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这些线程和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应用程序同时运行。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·Stop-the-world</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），直译就是停止一切，在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中指停止</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一切</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应用线程。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">安全点（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Safepoint</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），指</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在执行一些操作的时需要</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">但并不是任何线程在任何地方都能进入</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，例如我们正在执行一段</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代码时，线程如何能够停止？设计安全点的目的是，当线程进入到安</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">全点时，线程就会主动停止。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，在很多英文文献和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">源码中，经常看到这个单词，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">它指的是我们的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Java</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应用线程。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的含义是可变的，在这里的含</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">义是因为线程运行，导致了内存的变化。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中通常需要</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">才能使</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">暂停。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">记忆集（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Remember Set</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），简称为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">RSet</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。主要记录不同代际对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">象的引用关系。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·Refine</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，尚未有统一的翻译，有时翻译为细化，但是不太准确，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书中不做翻译。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">ConcurrentG1RefineThread</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">主要指处理</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">RSet</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的线程。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·Evacuation</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，转移、撤退或者回收，简称为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Evac</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，本书中不做翻</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">译。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中指的是发现活跃对象，并将对象复制到新地址的过程。</span>

---

## Page 22

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">回收（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Reclaim</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），通常指的是分区对象已经死亡或者已经完成</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Evac</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，分区可以被</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">再次使用。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·Closure</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，闭包，本书中不做翻译。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中是一种辅助类，类</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">似于我们已知的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">iterator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，它通常提供了对内存的访问。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·GC Root</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，垃圾回收的根。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的垃圾回收过程中，需要从</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC Root</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出发标记活跃对象，确保正在使用的对象在垃圾回收后都是</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">存活的。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">根集合（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Root Set</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的垃圾回收过程中，需要从不同的</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC Root</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出发，这些</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC Root</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">有线程栈、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">monitor</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">列表、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JNI</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象等，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">而这些</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC Root</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">就构成了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Root Set</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，简称为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">FGC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，整个堆的垃圾回收动作。通常</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">串行的，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Full GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">不仅有串行实现，在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中还有并行实现。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">再标记（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Remark</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。在本书中指的是并发标记算法中，处理完</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记后，需要更新并发标记中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">变更的引用，这一步需要</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>

---

## Page 23

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">1.3</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">回收算法概述</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Garbage Collection</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）指的是程序不用关心对象</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在内存中的生存周期，创建后只需要使用对象，不用关心何时释放以</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">及如何释放对象，由</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">自动管理内存并释放这些对象所占用的空</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">间。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的历史非常悠久，从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1960</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Lisp</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言开始就支持</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。垃圾回</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">收针对的是堆空间，目前垃圾回收算法主要有两类：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">引用计数法：在堆内存中分配对象时，会为对象分配一段额外的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">空间，这个空间用于维护一个计数器，如果对象增加了一个新的引</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用，则将增加计数器。如果一个引用关系失效则减少计数器。当一个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象的计数器变为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，则说明该对象已经被废弃，处于不活跃状态，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可以被回收。引用计数法需要解决循环依赖的问题，在我们众所周知</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Python</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">语言里，垃圾回收就使用了引用计数法。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可达性分析法（根引用分析法），基本思路就是将根集合作为起</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">始点，从这些节点开始向下搜索，搜索所走过的路径称为引用链，当</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一个对象没有被任何引用链访问到时，则证明此对象是不活跃的，可</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">以被回收。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这两种算法各有优缺点，具体可以参考其他文献。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的垃圾回</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">收采用了可达性分析法。垃圾回收算法也一直不断地演化，主要有以</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">下分类：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收算法实现主要分为复制（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Copy</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）、标记清除（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mark-</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Sweep</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）和标记压缩（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mark-Compact</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在回收方法上又可以分为串行回收、并行回收、并发回收。</span>

---

## Page 24

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在内存管理上可以分为代管理和非代管理。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">我们首先看一下基本的收集算法。</span>

---

## Page 25

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.3.1</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">分代管理算法</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分代管理就是把内存划分成不同的区域进行管理，其思想来源</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是：有些对象存活的时间短，有些对象存活的时间长，把存活时间短</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的对象放在一个区域管理，把存活时间长的对象放在另一个区域管</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">理。那么可以为两个不同的区域选择不同的算法，加快垃圾回收的效</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">率。我们假定内存被划分成</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个代：新生代和老生代。把容易死亡的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象放在新生代，通常采用复制算法回收；把预期存活时间较长的对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">象放在老生代，通常采用标记清除算法。</span>

---

## Page 26

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.3.2</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">复制算法</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">复制算法的实现也有很多种，可以使用两个分区，也可以使用多</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个分区。使用两个分区时内存的利用率只有</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">50%</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">；使用多个分区（如</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个分区），则可以提高内存的使用率。我们这里演示把堆空间分为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个新生代（分为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个分区：</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Eden</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Survivor0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Survivor1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个老生</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代的收集过程。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">普通对象创建的时候都是放在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Eden</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">区，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分别是两个存活</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">区。第一次垃圾收集前</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">都为空，在垃圾收集后，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Eden</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">里</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">面的活跃对象（即可以通过根集合到达的对象）都放入了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">区，如图</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">复制算法第一次回收</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">回收后</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">继续运行并产生垃圾，在第二次运行前</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Eden</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">都有活跃对象，在垃圾收集后，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Eden</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">里面的活跃对象（即可以</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">通过根节点到达的对象）都被放入到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">S0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">区，一直这样循环收集，如图</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p026_img01.jpg)

---

## Page 27

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-2</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">复制算法第二次回收</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p027_img01.jpg)

Eden  保留空间S0  S1  老生代  保留空间  第二次回收前

Eden  保留空间S0  S1  老生代  晋升保留空间第二次回收

---

## Page 28

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.3.3</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">标记清除</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">从根集合出发，遍历对象，把活跃对象入栈，并依次处理。处理</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">方式可以是广度优先搜索也可以是深度优先搜索（通常使用深度优先</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">搜索，节约内存）。标记出活跃对象之后，就可以把不活跃对象清</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">除。下面演示一个简单的例子，从根集合出发查找堆空间的活跃对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">象，如图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标记清除算法</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这里仅仅演示了如何找到对象，没有进一步介绍找到对象后如何</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">处理。对于标记清除算法其实还需要额外的数据结构（比如一个链</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">表）来记录可用空间，在对象分配的时候从这个链表中寻找能够容纳</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对象的空间。当然这里还有很多细节都未涉及，比如在分配时如何找</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p028_img01.jpg)

---

## Page 29

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">到最合适的内存空间，有</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">First Fit</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Best Fit</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Worst Fit</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等方法，这里</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">不再赘述。标记清除算法最大的缺点就是使内存碎片化。</span>

---

## Page 30

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.3.4</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">标记压缩</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">标记压缩算法是为了解决标记清除算法中使内存碎片化的问题，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">除了上述的标记动作之外，还会把活跃对象重新整理从头开始排列，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">减少内存碎片。</span>

---

## Page 31

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.3.5</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">算法小结</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收的基础算法自提出以来并没有大的变化。表</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对几种算</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">法的优缺点进行了比较，更加详细的介绍请参考其他书籍。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">表</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收基础算法的优缺点</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p031_img01.jpg)

---

## Page 32

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">1.4</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">JVM</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">垃圾回收器概述</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为了达到最大性能，基于分代管理和回收算法，结合回收的时</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">机，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">实现垃圾回收器了：串行回收、并行回收、并发标记回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）和垃圾优先回收。</span>

---

## Page 33

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.4.1</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">串行回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">串行回收使用单线程进行垃圾回收，在回收的时候</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">需要</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">STW</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。新生代通常采用复制算法，老生代通常采用标记压缩算法。串</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">行回收典型的线程交互图如图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">串行回收</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p033_img01.jpg)

---

## Page 34

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.4.2</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">并行回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行回收使用多线程进行垃圾回收，在回收的时候</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">需要暂</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停，新生代通常采用复制算法，老生代通常采用标记压缩算法。线程</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">交互如图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并行回收</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p034_img01.jpg)

---

## Page 35

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.4.3</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">并发标记回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记回收（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）的整个回收期间划分成多个阶段：初始标</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">记、并发标记、重新标记、并发清除等。在初始标记和重新标记阶段</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">需要暂停</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，在并发标记和并发清除期间可以和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mutator</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发运</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">行，如图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。这个算法通常适用于老生代，新生代可以采用并行</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">回收。</span>

---

## Page 36

<span style="font-family:'LiberationSans-Bold';font-size:16.88pt;font-weight:bold;color:#a47000">1.4.4</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">垃圾优先回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾优先回收器（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Garbage-First</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，也称为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JDK7 Update 4</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">开始正式提供。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">致力于在多</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CPU</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和大内存服务器上对垃圾回收提供</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">软实时目标和高吞吐量。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收器的设计和前面提到的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">种回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">器都不一样，它在并行、串行以及</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">针对堆空间的管理方式上</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">都是连续的，如图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-6</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并发标记回收</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p036_img01.jpg)

---

## Page 37

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">连续空间管理</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">连续的内存将导致垃圾回收时收集时间过长，停顿时间不可控。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">因此</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">将堆拆成一系列的分区（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Heap Region</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），这样在一个时间段</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">内，大部分的垃圾收集操作只针对一部分分区，而不是整个堆或整个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（老生）代，如图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">所示。</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p037_img01.jpg)

---

## Page 38

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区空间管理</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">里，新生代就是一系列的内存分区，这意味着不用再要求新</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">生代是一个连续的内存块。类似地，老生代也是由一系列的分区组</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">成。这样也就不需要在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">运行时考虑哪些分区是老生代，哪些是新</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">生代。事实上，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">通常的运行状态是：映射</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区的虚拟内存随着</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时间的推移在不同的代之间切换。例如一个</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区最初被指定为新生</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代，经过一次新生代的回收之后，会将整个新生代分区都划入未使用</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的分区中，那它可以作为新生代分区使用，也可以作为老生代分区使</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用。很可能在完成一个新生代收集之后，一个新生代的分区在未来的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">某个时刻可用于老生代分区。同样，在一个老生代分区完成收集之</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">后，它就成为了可用分区，在未来某个时候可作为一个新生代分区来</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">使用。</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p038_img01.jpg)

---

## Page 39

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新生代的收集方式是并行收集，采用复制算法。与其他</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">圾回收器一样，一旦发生一次新生代回收，整个新生代都会被回收，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这也就是我们常说的新生代回收（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Young GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。但是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和其他垃圾</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">回收器不同的地方在于：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">会根据预测时间动态改变新生代的大小。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">化主要是根据内存的使用情况进行的。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中则是以预测时间为导向，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">根据内存的使用情况调整新生代分区的数目。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">老生代的垃圾回收方式与其他</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收器对老生代处理</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">有着极大的不同。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">老生代的收集不会为了释放老生代的空间对整个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">老生代做回收。相反，在任意时刻只有一部分老生代分区会被回收，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">并且，这部分老生代分区将在下一次增量回收时与所有的新生代分区</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一起被收集。这就是我们所说的混合回收（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Mixed GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。在选择老生</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代分区的时候，优先考虑垃圾多的分区，这也正是垃圾优先这个名字</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的由来。后续我们将逐一介绍这些内容。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中还有一个概念就是大对象，指的是待分配的对象大小超过</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一定的阈值之后，为了减少这种对象在垃圾回收过程的复制时间，直</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">接把对象分配到老生代分区中而不是新生代分区中。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">从实现角度来看，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法是复合算法，吸收了以下算法的优势：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">列车算法，对内存进行分区，参见图</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-8</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，对分区进行并发标记。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">最老优先，最老的数据（通常也是垃圾）优先收集。</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">其他垃圾回收新生代的大小也可以动态变化，但这个变</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p039_img01.png)

---

## Page 40

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">关于列车算法、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和最老优先可以参考其他的书籍，这里不再</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">赘述。</span>

---

## Page 41

<span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">第</span><span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#a47000">2</span><span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">章</span><span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">　</span><span style="font-family:'LiberationSans-Bold';font-size:30.00pt;font-weight:bold;color:#a47000">G1</span><span style="font-family:'Unnamed-T3';font-size:30.00pt;color:#000000">的基本概念</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">通常我们所说的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是指垃圾回收，但是在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的实现中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">更为</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">准确的意思是指内存管理器，它有两个职能，第一是内存的分配管</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">理，第二是垃圾回收。这两者是一个事物的两个方面，每一种垃圾回</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">收策略都和内存的分配策略息息相关，脱离内存的分配去谈垃圾回收</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是没有任何意义的。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">3</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章会介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如何分配对象，第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章到第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章都是介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是如何进行垃圾回收的。为了更好地理解后续章节，本章主要介绍</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的一些基本概念，主要有：</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">实现中所用的一些基础数据堆分区、</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的停顿预测模型、垃圾回收中使用到的对象头、并发标记中涉及的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">卡表和位图，以及垃圾回收过程中涉及的线程、栈帧和句柄等。</span>

---

## Page 42

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">2.1</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">分区</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Heap Region</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）或称堆分区，是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">堆和操作系统交</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">互的最小管理单位。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的分区类型（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HeapRegionType</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）大致可以分</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为四类：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">自由分区（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Free Heap Region</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">FHR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新生代分区（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Young Heap Region</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">YHR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大对象分区（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Humongous Heap Region</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HHR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">老生代分区（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Old Heap Region</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">OHR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">其中新生代分区又可以分为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Eden</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Survivor</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">；大对象分区又可以</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分为：大对象头分区和大对象连续分区。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">每一个分区都对应一个分区类型，在代码中常见的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">is_young</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">is_old</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">is_houmongous</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等判断分区类型的函数都是基于上述的分区</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">类型实现，关于分区类型代码如下所示：</span>

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">hotspot/src/share/vm/gc_implementation/g1/heapRegionType.hpp </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0000 0 [ 0] Free </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">//  </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0001 0      Young Mask </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0001 0 [ 2] Eden </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0001 1 [ 3] Survivor </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">//  </span>

---

## Page 43

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0010 0      Humongous Mask </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0010 0 [ 4] Humongous Starts </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 0010 1 [ 5] Humongous Continues </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">//  </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// 01000 [ 8] Old </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中每个分区的大小都是相同的。该如何设置</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的大小？设</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">置</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的大小有哪些考虑？</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的大小直接影响分配和垃圾回收效率。如果过大，一个</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">以存放多个对象，分配效率高，但是回收的时候花费时间过长；如果</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">太小则导致分配效率低下。为了达到分配效率和清理效率的平衡，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">有一个上限值和下限值，目前上限是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">32MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，下限是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（为了适应</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">更小的内存分配，下限可能会被修改，在目前的版本中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的大小只能</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">8MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">、</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">16MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">32MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），默认情况下，整个堆</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">空间分为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2048</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（该值可以自动根据最小的堆分区大小计算得</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出）。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大小可由以下方式确定：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可以通过参数</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1HeapRegionSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">来指定大小，这个参数的默认</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">值为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">启发式推断，即在不指定</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大小的时候，由</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">启发式地推断</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大小。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">启发式推断根据堆空间的最大值和最小值以及</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个数进行推</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">断，设置</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Initial HeapSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（默认为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">0</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）等价于设置</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Xms</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，设置</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">MaxHeapSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（默认为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">96MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）等价于设置</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Xmx</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。堆分区默认大小的</span>

---

## Page 44

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">计算方式在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">HeapRegion.cpp</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">setup_heap_region_size()</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，代码如</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">下所示：</span>

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">hotspot/src/share/vm/gc_implementation/g1/heapRegion.cpp </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">void HeapRegion::setup_heap_region_size(...) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  /*</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">判断是否是设置过堆分区大小，如果有则使用；没有，则根据初始内存和最大</span>
<span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">分配内存，获得平均值，并根据</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">HR</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">的个数得到分区的大小，和分区的下限比较，取</span>
<span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">两者的最大值。</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">*/ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  uintx region_size = G1HeapRegionSize; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (FLAG_IS_DEFAULT(G1HeapRegionSize)) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    size_t average_heap_size = (initial_heap_size + </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">max_heap_size) / 2; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    region_size = MAX2(average_heap_size / </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">HeapRegionBounds::target_number(), </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">                      (uintx) HeapRegionBounds::min_size()); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">对</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">region_size</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">按</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">2</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">的幂次对齐，并且保证其落在上下限范围内</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  int region_size_log = log2_long((jlong) region_size); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  region_size = ((uintx)1 &lt;&lt; region_size_log); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">确保</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">region_size</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">落在</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">[1MB</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">，</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">32MB]</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">之间</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (region_size &lt; HeapRegionBounds::min_size()) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    region_size = HeapRegionBounds::min_size(); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } else if (region_size &gt; HeapRegionBounds::max_size()) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    region_size = HeapRegionBounds::max_size(); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">根据</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">region_size</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">计算一些变量，如卡表大小</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  region_size_log = log2_long((jlong) region_size); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  LogOfHRGrainBytes = region_size_log; </span>

---

## Page 45

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  LogOfHRGrainWords = LogOfHRGrainBytes - LogHeapWordSize; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  GrainBytes = (size_t)region_size; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  GrainWords = GrainBytes &gt;&gt; LogHeapWordSize; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  CardsPerRegion = GrainBytes &gt;&gt; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">CardTableModRefBS::card_shift; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">按照默认值计算，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可以管理的最大内存为</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2048×32MB=64GB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。假设设置</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">xms=32G</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">xmx=128G</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，则每个堆分</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">区的大小为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">32M</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，分区个数动态变化范围从</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1024</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">4096</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中大对象不使用新生代空间，直接进入老生代，那么多大的对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">象能称为大对象？简单来说是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">region_size</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的一半。</span>
<span style="font-family:'Unnamed-T3';font-size:8.74pt;color:#000000">新生代大小</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新生代大小指的是新生代内存空间的大小，前面提到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中新生代</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">大小按分区组织，即首先计算整个新生代的大小，然后根据上一节中</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的计算方法计算得到分区大小，两者相除得到需要多少个分区。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">与新生代大小相关的参数设置和其他</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法类似，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中还增加了两</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个参数</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1MaxNewSizePercent</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1NewSizePercent</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">用于控制新生代</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的大小，整体逻辑如下：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如果设置新生代最大值（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">MaxNewSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）和最小值</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">），可以根据这些值计算新生代包含的最大的分区和最小</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的分区；注意</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Xmn</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等价于设置了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">MaxNewSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，且</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewSize=MaxNewSize</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>

---

## Page 46

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如果既设置了最大值或者最小值，又设置了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewRatio</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，则忽略</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewRatio</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如果没有设置新生代最大值和最小值，但是设置了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewRatio</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">则新生代的最大值和最小值是相同的，都是整个堆空间</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">/</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">NewRatio+1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如果没有设置新生代最大值和最小值，或者只设置了最大值和最</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">小值中的一个，那么</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">将根据参数</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1MaxNewSizePercent</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（默认值</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">60</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1NewSizePercent</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（默认值为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）占整个堆空间的比例来计</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算最大值和最小值。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">值得注意的是，如果</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">推断出最大值和最小值相等，则说明新生</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">代不会动态变化。不会动态变化意味着</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在后续对新生代垃圾回收的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时候可能不能满足期望停顿的时间，具体内容将在后文继续介绍。新</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">生代大小相关的代码如下所示：</span>

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">hotspot/src/share/vm/gc_implementation/g1/g1CollectorPolicy.c</span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">pp </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">初始化新生代大小参数，根据不同的</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">参数判断计算新生代大小，供后续使用</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">G1YoungGenSizer::G1YoungGenSizer() : </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">_sizer_kind(SizerDefaults), _adaptive_ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  size(true),  _min_desired_young_length(0), </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">_max_desired_young_length(0) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">如果设置</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">NewRatio</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">且同时设置</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">NewSize</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">或</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">MaxNewSize</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">的情况下，则</span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">NewRatio</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">被忽略</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (FLAG_IS_CMDLINE(NewRatio)) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    if (FLAG_IS_CMDLINE(NewSize) || </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">FLAG_IS_CMDLINE(MaxNewSize)) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      warning(&quot;-XX:NewSize and -XX:MaxNewSize override -</span>

---

## Page 47

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">XX:NewRatio&quot;); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    } else { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      _sizer_kind = SizerNewRatio; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      _adaptive_size = false; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      return; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">参数传递有问题，最小值大于最大值</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (NewSize &gt; MaxNewSize) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    if (FLAG_IS_CMDLINE(MaxNewSize)) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      warning(&quot;…”); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    MaxNewSize = NewSize; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">根据参数计算分区的个数</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (FLAG_IS_CMDLINE(NewSize)) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _min_desired_young_length = MAX2((uint) (NewSize / </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">HeapRegion::  </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      GrainBytes), 1U); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    if (FLAG_IS_CMDLINE(MaxNewSize)) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      _max_desired_young_length = MAX2((uint) (MaxNewSize / </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">HeapRegion::  </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">        GrainBytes), 1U); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      _sizer_kind = SizerMaxAndNewSize; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      _adaptive_size = _min_desired_young_length == </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">_max_desired_young_length; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    } else { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      _sizer_kind = SizerNewSizeOnly; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } else if (FLAG_IS_CMDLINE(MaxNewSize)) { </span>

---

## Page 48

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _max_desired_young_length =  MAX2((uint) (MaxNewSize / </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">HeapRegion::  </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      GrainBytes), 1U); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _sizer_kind = SizerMaxNewSizeOnly; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">使用</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">G1NewSizePercent</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">来计算新生代的最小值</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">uint G1YoungGenSizer::calculate_default_min_length(uint </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">new_number_of_heap_ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  regions) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  uint default_value = (new_number_of_heap_regions * </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">G1NewSizePercent) / 100; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  return MAX2(1U, default_value); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">// </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">使用</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">G1MaxNewSizePercent</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">来计算新生代的最大值</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">uint G1YoungGenSizer::calculate_default_max_length(uint </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">new_number_of_heap_ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  regions) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  uint default_value = (new_number_of_heap_regions * </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">G1MaxNewSizePercent) / 100; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  return MAX2(1U, default_value); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">/*</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">这里根据不同的参数输入来计算大小。</span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">recalculate_min_max_young_length</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">在初始化时被调用，在堆空间改变时也</span>
<span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">会被调用。</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">*/ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">void G1YoungGenSizer::recalculate_min_max_young_length(uint </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">number_of_heap_ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  regions, uint* min_young_length, uint* max_young_length) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  assert(number_of_heap_regions &gt; 0, &quot;Heap must be </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">initialized&quot;); </span>

---

## Page 49

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  switch (_sizer_kind) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    case SizerDefaults: </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *min_young_length = </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">calculate_default_min_length(number_of_heap_regions); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *max_young_length = </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">calculate_default_max_length(number_of_heap_regions); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      break; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    case SizerNewSizeOnly: </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *max_young_length = </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">calculate_default_max_length(number_of_heap_regions); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *max_young_length = MAX2(*min_young_length, </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">*max_young_length); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      break; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    case SizerMaxNewSizeOnly: </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *min_young_length = </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">calculate_default_min_length(number_of_heap_regions); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *min_young_length = MIN2(*min_young_length, </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">*max_young_length); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      break; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    case SizerMaxAndNewSize: </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      // Do nothing. Values set on the command line, don&#x27;t </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">update them at runtime. </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      break; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    case SizerNewRatio: </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *min_young_length = number_of_heap_regions / (NewRatio </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">+ 1); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      *max_young_length = *min_young_length; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      break; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    default: </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">      ShouldNotReachHere(); </span>

---

## Page 50

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如果</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是启发式推断新生代的大小，那么当新生代变化时该如何</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">实现？简单地说，使用一个分区列表，扩张时如果有空闲的分区列表</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">则可以直接把空闲分区加入到新生代分区列表中，如果没有的话则分</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">配新的分区然后把它加入到新生代分区列表中。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">有一个线程专门抽</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">样处理预测新生代列表的长度应该多大，并动态调整。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">另外还有一个问题，就是分配新的分区时，何时扩展？一次扩展</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">多少内存？</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是自适应扩展内存空间的。参数</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">-XX:GCTimeRatio</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">表示</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">与</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应用的耗费时间比，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中默认为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">9</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，计算方式为</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">_gc_overhead_perc=100.0×(1.0/(1.0+GCTimeRatio))</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，即</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1 GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">间与应用时间占比不超过</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10%</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时不需要动态扩展，当</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时间超过这个</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">阈值的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10%</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，可以动态扩展。扩展时有一个参数</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1ExpandByPercentOfAvailable</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（默认值是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">20</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）来控制一次扩展的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">比例，即每次都至少从未提交的内存中申请</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">20%</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，有下限要求（一次</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">申请的内存不能少于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1M</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，最多是当前已分配的一倍），代码如下所</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">示：</span>

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">size_t G1CollectorPolicy::expansion_amount() { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">先根据历史信息获取平均</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">时间</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000"> </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  double recent_gc_overhead = recent_avg_pause_time_ratio() * </span>

---

## Page 51

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">100.0; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  double threshold = _gc_overhead_perc; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  /* G1 GC</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">时间与应用时间占比超过阈值才需要动态扩展，这个阈值的值为</span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">_gc_overhead_perc = 100.0 × (1.0 / (1.0 + GCTimeRatio))</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">，上文</span>
<span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">提到</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">GCTimeRatio=9</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">，即超过</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">10%</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">才会扩张内存</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">*/ </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (recent_gc_overhead &gt; threshold) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    const size_t min_expand_bytes = 1*M; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    size_t reserved_bytes = _g1-&gt;max_capacity(); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    size_t committed_bytes = _g1-&gt;capacity(); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    size_t uncommitted_bytes = reserved_bytes - </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">committed_bytes; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    size_t expand_bytes; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    size_t expand_bytes_via_pct = uncommitted_bytes * </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">G1ExpandByPercentOfAvailable / 100; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    expand_bytes = MIN2(expand_bytes_via_pct, </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">committed_bytes); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    expand_bytes = MAX2(expand_bytes, min_expand_bytes); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    expand_bytes = MIN2(expand_bytes, uncommitted_bytes); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    …… </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    return expand_bytes; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } else { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    return 0; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中内存的扩展时机在第</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">章介绍。</span>

---

## Page 52

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">2.2</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">G1</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">停顿预测模型</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是一个响应时间优先的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算法，用户可以设定整个</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">过程的</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">期望停顿时间，由参数</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">MaxGCPauseMillis</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">控制，默认值</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">200ms</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。不过</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">它不是硬性条件，只是期望值，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">会努力在这个目标停顿时间内完成</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">垃圾回收的工作，但是它不能保证，即也可能完不成（比如我们设置</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了太小的停顿时间，新生代太大等）。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">那么</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">怎么满足用户的期望呢？就需要停顿预测模型了。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">根</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">据这个模型统计计算出来的历史数据来预测本次收集需要选择的堆分</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">区数量（即选择收集哪些内存空间），从而尽量满足用户设定的目标</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停顿时间。如使用过去</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">次垃圾回收的时间和回收空间的关系，根据</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">目前垃圾回收的目标停顿时间来预测可以收集多少的内存空间。比如</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">最简单的办法是使用算术平均值建立一个线性关系来预测。如过去</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">次一共收集了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">10GB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的内存，花费了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1s</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，那么在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">200ms</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的停顿时间要</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">求下，最多可以收集</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">2GB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的内存空间。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的预测逻辑是基于衰减平均</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">值和衰减标准差。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">衰减平均（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">Decaying Average</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）是一种简单的数学方法，用来计</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">算一个数列的平均值，核心是给近期的数据更高的权重，即强调近期</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">数据对结果的影响。衰减平均计算公式如下所示：</span>

---

## Page 53

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">式中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">α</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为历史数据权值，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1-α</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为最近一次数据权值。即</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">α</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">越小，最</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">新的数据对结果影响越大，最近一次的数据对结果影响最大。不难看</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出，其实传统的平均就是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">α</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">取值为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">(n-1)/n</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的情况。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">同理，衰减方差的定义如下：</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停顿预测模型是以衰减标准差为理论基础实现的，代码如下所</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">示：</span>

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">hotspot/src/share/vm/gc_implementation/g1/g1CollectorPolicy.h</span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">pp </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">double get_new_prediction(TruncatedSeq* seq) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  return MAX2(seq-&gt;davg() + sigma() * seq-&gt;dsd(), </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">              seq-&gt;davg() * conf idence_factor(seq-&gt;num())); </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在这个预测计算公式中：</span>

![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p053_img01.jpg)

davgn=V  if n = 1
（davgn =(1 -α）x V, + α × davgn-1,ifn>1



![](./JVM G1源码分析和调优 (彭成寒) (Z-Library)_assets/images/p053_img02.jpg)

davr,=0  if n =1
(davr, =(1 -α)×(V - davgn)²+α× davrn-1,ifn>1

---

## Page 54

<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·davg</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">表示衰减均值。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·sigma()</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">返回一个系数，来自</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1ConfidencePercent</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（默认值为</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">50</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">sigma</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">0.5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）的配置，表示信赖度。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·dsd</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">表示衰减标准偏差。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·confidence_factor</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">表示可信度相关系数，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">confidence_factor</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">当样</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本数据不足时（小于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个）取一个大于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的值，并且样本数据越少该值</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">越大。当样本数据大于</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">5</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">时</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">confidence_factor</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">取值为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。这是为了弥补</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">样本数据不足，起到补偿作用。</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">方法的参数</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">TruncateSeq</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，顾名思义，是一个截断的序列，它只</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">跟踪序列中最新的</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">n</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个元素。在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1 GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">过程中，每个可测量的步骤花</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">费的时间都会记录到</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">TruncateSeq</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">（继承了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">AbsSeq</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）中，用来计算衰</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">减均值、衰减变量、衰减标准偏差等，代码如下所示：</span>

<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">hotspot/src/share/vm/utilities/numberSeq.cpp </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">void AbsSeq::add(double val) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  if (_num == 0) { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    // </span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">初始时，还没有历史数据，</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">davg</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">就是当前参数，</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">dvar</span><span style="font-family:'Unnamed-T3';font-size:12.00pt;color:#000000">设置为</span><span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">0 </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _davg = val; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _dvariance = 0.0; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } else { </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _davg = (1.0 - _alpha) * val + _alpha * _davg; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    double diff = val - _davg; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">    _dvariance = (1.0 - _alpha) * diff * diff + _alpha * </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">_dvariance; </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">  } </span>
<span style="font-family:'LiberationMono';font-size:12.00pt;color:#000000">} </span>

---

## Page 55

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这个</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">add</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">方法就是上面两个衰减公式的实现代码。其中</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">_davg</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为衰</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">减均值，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">_dvariance</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">为衰减方差，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">_alpha</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">默认值为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">0.7</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的软实时</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">停顿就是通过这样的预测模型来实现的。</span>

---

## Page 56

<span style="font-family:'LiberationSans-Bold';font-size:21.25pt;font-weight:bold;color:#a47000">2.3</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">　</span><span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">卡表和位图</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">卡表（</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CardTable</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">）在</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">CMS</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中是最常见的概念之一，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">G1</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中不仅保</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">留了这个概念，还引入了</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">RSet</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。卡表到底是一个什么东西？</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">GC</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">最早引入卡表的目的是为了对内存的引用关系做标记，从而根</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">据引用关系快速遍历活跃对象。举个简单的例子，有两个分区，假设</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分区大小都为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">1MB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，分别为</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">A</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">和</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">B</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">。如果</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">A</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中有一个对象</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">objA</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">B</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中有</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一个对象</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">objB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，且</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">objA.field=objB</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，那么这两个分区就有引用关系</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了，但是如果我们想找到分区</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">A</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，要如何引用分区</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">B</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">？做法有两种：</span>
<span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">·</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">遍历整个分区</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">A</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">，一个字一个字的移动（为什么以字为单位？原</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">因是</span><span style="font-family:'LiberationSans';font-size:15.00pt;color:#000000">JVM</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">中对象会对齐，所以不需要按字节移动），然后查看内存里</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">面的值到底是不是指向</sp