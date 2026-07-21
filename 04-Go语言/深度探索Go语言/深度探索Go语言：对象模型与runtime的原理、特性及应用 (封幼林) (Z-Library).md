## Page 1

深度探索Go语言

<!-- 图源: ./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p001_scan.jpg -->

---

## Page 2

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">目</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">    </span><span style="font-family:'SimSun';font-size:20.99pt;color:#000000">录</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">版权信息</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">内容简介</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">作者简介</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">序一</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">序二</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">序三</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">序四</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">前言</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">致谢</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">汇编基础</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">1.1 x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">通用寄存器</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">1.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">常用汇编指令</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">1.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">内存分页机制</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">1.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">汇编代码风格</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">1.5 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">指针</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">2.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">指针构成</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">2.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">相关操作</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">2.3 unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">包</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">2.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">函数</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">栈帧</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">逃逸分析</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3.3 Function Value</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3.4 defer</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3.5 panic</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">3.6 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">方法</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">4.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">接收者类型</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">4.2 Method Value</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">4.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">组合式继承</span>

---

## Page 3

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">4.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">接口</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">5.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">空接口</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">5.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">非空接口</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">5.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">类型断言</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">5.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">反射</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">5.5 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> goroutine</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">进程、线程与协程</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.2 IO</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">多路复用</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">巧妙结合</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.4 GMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">模型</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.5 GMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">主要数据结构</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.6 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">调度器初始化</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.7 G</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">的创建与退出</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.8 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">调度循环</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.9 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">抢占式调度</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.10 timer</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.11 netpoller</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.12 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">监控线程</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">6.13 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">同步</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">7.1 Happens Before</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">7.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">内存乱序</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">7.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">常见的锁</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">7.4 Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">语言的同步</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">7.5 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">堆</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">8.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">内存分配</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">8.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">垃圾回收</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">8.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">栈</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">9.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">栈分配</span>

---

## Page 4

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">9.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">栈增长</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">9.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">栈收缩</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">9.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">栈释放</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#4183c4">9.5 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#4183c4">资源链接</span>

---

## Page 5

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">版权信息</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">书名：深度探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言：对象模型与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的原理、特性及应用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编著：封幼林</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">出版社：清华大学出版社</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">出版时间：</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2022-08-01 </span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ISBN</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">：</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9787302600855 </span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">·</span>

---

## Page 6

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">内容简介</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书主要讲解</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的一些关键特性的实现原理。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Nicklaus Wirth</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大师曾经说过：算法</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数据结构＝</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">程序，语言特性的实现不外乎是数据结构</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">代码逻辑。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">全书内容共分为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">部分：第一部分是基础特性（第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章），第二部分是对象模型（第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章），</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第三部分是调度系统（第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">6</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章），第四部分是内存管理（第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章）。书中主要内容包括指针、</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">函数栈帧、调用约定、变量逃逸、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Function Value</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、闭包、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">defer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">panic</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、方法、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Method Value</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、组</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">合式继承、接口、类型断言、反射、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、抢占式调度、同步、堆和栈的管理，以及</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">书中包含大量的探索示例和源码分析，读者在学会应用的同时还能了解实现原理。书中绝大部分代</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">码是用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言实现的，还有少部分代码是用汇编语言实现的，这些代码都可以使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言官方</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SDK</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">直接编译。探索过程循序渐进、条理清晰，用到的工具也都是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SDK</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">自带的，方便读者亲自上手</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">实践。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书适合</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的初学者，在学习语言特性的同时了解其实现原理。更适合有一定的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言应用</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">基础，想要深入研究底层原理的技术人员，以及有一些其他编程语言基础，想要转学</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的开</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">发者阅读。</span>

---

## Page 7

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">作者简介</span>

---

## Page 8

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">封幼林</span><span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000"> </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">资深软件工程师，十多年</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">IT</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">从业经验，曾涉足</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Win32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">桌面程序开发、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Android</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">移动端开发，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">以及互联网服务器端开发等多个领域。喜欢研究底层技术，用自己的方法探究背后的实现原理。热</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">爰技术交流与分享，创建了微信公众号</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">“</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">幼麟实验室</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">”</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，致力做一些形象、通透的计算机教程，让开</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">发者</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">“</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">知其然亦知其所以然</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">”</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>

---

## Page 9

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">序一</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">FOREWORD</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">常有同学问我，学习技术的原理、机制到底有什么用？现在已经有很多不同的操作系统和编程语</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">言，我们个人不太可能再去实现操作系统或编程语言！的确如此，如果以学习为目的，我们可以实</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">现操作系统或编程语言的简陋原型，但在工作中并没有这样的需求和机会，而学习原理、机制的目</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的是增进自己对技术问题的判断，同时获得对典型问题和最佳方案的积累，让自己具备分析复杂技</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">术问题和求解正确的技术方案的能力。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对于编程语言，优秀的程序员既能熟练地使用语言的各种特性，快速满足业务领域开发，又可以掌</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">握语言的设计原理和底层机制。既是别人眼中的快刀手，也是面对难题，一击必中的高手。工作中</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在面对不同技术方案时，可以快速做出最合理的选择。既可以解决当前的问题，又可以让系统长治</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">久安地演进，将来不会推倒重来，而这些分析判断都取决于你对技术原理和机制的理解。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">最近几年，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言进展迅速，吸引广大的程序员学习和使用。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言有很多优秀特性，如</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可以让大家轻易写出高并发的服务。语言掌握起来也简单，往往学习两三周，就可以实际</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">投入工作开发，但真正遇到复杂的场景、资源竞争或</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">敏感时，缺少对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言机制和进程结构的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">理解，你会很难完成上述挑战。很可能当你使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言多年后，仍然不能写出健壮的核心业务服</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">务。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在《深度探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">——</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对象模型与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的原理、特性及应用》中，封幼林把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言主要的核</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">心特性从原理到应用，从底层的汇编代码到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言代码，以庖丁解牛般的剖析让读者对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言豁</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">然开朗，使语言的原理与机制变得清晰和简单。相信读者在认真学习后，将使自己对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言理解</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">与掌握有一个质的飞跃。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">左文建</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">奇安信集团副总裁</span>

---

## Page 10

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">序二</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">FOREWORD</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言诞生距今已有十余年，我最开始使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言还是在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2012</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">年，当时</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1.0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">版本刚刚发</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">布，虽然继承了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Plan 9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的衣钵，却有很多让人诟病的地方。我们当时用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言实现了一些</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">HTTP</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Client</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和网络爬虫业务，虽然编写过程十分顺畅，但是会遇到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的性能和其他稳定性的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">问题，于是就变成了一次浅尝辄止的尝试。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">随着时间的推移，我再次在业务中使用的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言已经到了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1.4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">版本，它的稳定性问题已得到了解</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">决。很快，随着</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go 1.5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">版本的发布，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">性能问题也不复存在，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言终于成长为一门优秀的开发</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言。而随着最近几次版本的新特性</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">——</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">泛型的加入，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言在表达能力上获得更进一步的提</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">升，未来十分可期。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">我大部分时间在用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言写服务器端程序，但也用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言写过客户端程序，写过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PoC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，写过</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">DSL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，写过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">JIT</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，甚至写过嵌入式程序的通信界面，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言现在对我来讲已经成为相当称手的工</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">具。选择</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言进行开发意味着快速、便捷、高性能，甚至它已经成为云原生的代名词。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在我最初接触</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的时候，当时唯一一本</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的书籍就是许式伟老师编写的《</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言编</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">程》，可以说是大家用中文学习</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的唯一途径，而现在则不断有很棒的中文书籍问世。《深</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">度探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">——</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对象模型与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的原理、特性及应用》直接从底层开始，为大家介绍需要的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编基础知识，紧接着从指针、函数、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">逐步深入，不断剖析</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言原理，让大家获得最</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">贴近实现原理的知识。拨开运行时的迷雾，不必猜测编写的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言代码运行时的行为，真正地让</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大家掌握</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言全部的精髓。可以毫不夸张地说，这是一本</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">High-End</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图书。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">书中作者先用示例代码描述原理和概念，然后辅以图例说明，最后使用对应生成的汇编代码予以佐</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">证，可以说是学习</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言底层知识的最佳途径。我阅读</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言源代码特别喜欢直接在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言源码</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中进行</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Hack</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，得益于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的编译速度，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Hack</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">完毕后进行编译，然后测试修改结果也十分迅速，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">这无疑提升了学习速度。建议大家不要怕源代码，只有在源代码中才能洞悉设计者的真正意图，才</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">能理解设计所面临的工程问题和解决方案的精妙之处。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">相信大家看完本书后，一定会受益匪浅，水平得到质的提升！</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">张旭红</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">金山办公</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Exline</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">技术副总监，掘金技术社区前技术总监</span>

---

## Page 11

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">序三</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">FOREWORD</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">不知从什么时候起，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言圈子里突然多了一个看上去很可爱的蛋壳形象，把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的底层实现</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">与枯燥的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">知识变成了妙趣横生的动画，呈现在编程世界里，赢得了大家的喜爱，让人有一</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">种</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">“</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">旧时王谢堂前燕，飞入寻常</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Gopher</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">家</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">”</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的感觉。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">一件事情，一门技术，如果能让大多数人觉得有意思，那再去学习它就不是什么难事了。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作者输出的文章和动画让我对作者本身也产生了一些兴趣，因为我也是一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Gopher</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和技术写作者，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">深知把庞杂的底层知识给别人讲明白是一件多么有挑战的事情。这些透彻的文章，以及看了令人舒</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">心的技术动画，到底是怎么制作出来的呢？</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在微信上与作者做过简单的交流之后，得知了作者自身多年的开发经验，以及底层与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的研发背</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">景，这些谜团便揭开了。在我的研发生涯中碰到的大多数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C/C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">工程师，对于底层和高并发知识都</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">能如数家珍，但能够将这些积累与他人说清道明并不是每个人都能做得到的。既能学会又能讲清之</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">人少之又少，作者就是其中之一，会使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">VideoScribe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的工程师也不是那么多。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">现代的软件工程师，无论是应用工程师，还是基础设施工程师，都会对底层、高并发知识有浓厚的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">兴趣，这不是没有理由的，因为这些知识能够帮助我们定位出大部分日常开发中碰到的性能问题，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">理解并解决所有线上遇到的高并发环境下才会触发的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Bug</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。这些知识也是每个有追求的互联网公司</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的工程师所必备的专业素质，多读书，多写代码，多积累，最终才能让我们一步一步成为一个合格</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的技术专家，这些与公司内的头衔无关，是真正的技术硬实力。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书的内容主要是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的底层知识，相比其他写底层的书而言可能没有覆盖到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">“</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">”</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">底层细</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节，但其覆盖到的内容都是细之又细，相信大家在阅读本书或阅览作者制作的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言系列动画时</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">一定能够有所收获。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">曹春晖</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">《</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言高级编程》作者</span>

---

## Page 12

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">序四</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">FOREWORD</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">得知《深度探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">——</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对象模型与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的原理、特性及应用》即将出版上市，我感到非常</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">高兴，更开心的是作者邀请我为此书写推荐序。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">我和封幼林的相识，是通过幼麟实验室。幼麟实验室从</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2020</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">年</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">月开始，持续以图解的形式讲解计</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">算机和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的相关知识，至今已经发布了一系列与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言相关的视频。内容涉及</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">map</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、内存对齐、函数栈帧、闭包、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">defer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">panic</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等基础特性，还有反射、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、调度系</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">统、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Mutex</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">channel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，以及</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等复杂问题，都以简单易懂的形式呈现出来。对广大</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Gopher</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">来讲，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是非常不错的参考学习资料。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书是作者在图解视频和知乎系列文章的基础上，更加系统地重新创作而成。我们从本书的副标</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">题</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">“</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对象模型与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的原理、特性及应用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">”</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，就能看出本书的侧重点，对于想要深入了解这部分内</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">容的读者很有参考价值。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书从反汇编开始，结合图示讲解和源码分析，非常系统地探索了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的基础特性、对象模</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">型、调度系统和内存管理模块。在讲解</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言底层知识的同时，作者的探索方法也很值得学习借</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">鉴，让我们知其然也知其所以然。特别是对想要亲自动手探索语言底层实现的读者来讲，简直就是</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">福音。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">杨文</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">夜读发起人</span>

---

## Page 13

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">前言</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">PREFACE</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">近几年来，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言作为一门服务器端开发语言越来越受欢迎，简洁易学的语法加上天生的高并发</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">支持，还有日益完善的社区，让很多互联网公司开始转向</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言。随着</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言生态日趋成熟，各</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">种组件框架如雨后春笋般涌现，市面上相关的书籍也多了起来，但是其中大部分是以应用为主，对</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">于语言特性本身探索一般不太深入。笔者希望能够有一本讲解语言特性及实现原理的书，这也是写</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作本书的动机。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">笔者当年刚参加工作的时候，使用的第一门开发语言是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。虽然之前在学校用过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言和汇编语</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">言，但在接触到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的一些面向对象特性时还是困惑了很久。直到有一天发现了《深度探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">象模型》，作者</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Stanley Lippman</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">当年在贝尔实验室工作，是世界上第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编译器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">——cfront</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的实</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">现者，他从一个语言实现者的高度，对一些关键特性的实现原理及其背后的思考进行了详细阐述，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">使笔者受益匪浅。后来因为工作的原因，笔者开始使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言，因为有了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C/C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">相关的基础，所以</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">学习起来更加高效。尤其是当年学习</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对象模型，让笔者认识到语言特性也是通过数据结构和代</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">码实现的，所以就按照自己的方式一边学习一边探索。第一次萌生要写点东西的念头是在给从</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PHP</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">转</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的妻子讲完接口动态派发的实现原理后，用她的话来讲就是有种豁然开朗的感觉，并鼓</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">励笔者把这些东西整理一下。后来我们就在微信公众号上以幼麟实验室的名义发布了一系列视频和</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">文章，主要分析语言特性的底层实现。在一年多的时间里，幼麟实验室受到了广大网友的好评与支</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">持，清华大学出版社的赵佳霓编辑也是在此期间联系了笔者，希望笔者能够把自己的探索研究整理</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">成书。因为写作本书的关系，让笔者能够更系统地思考，收获颇多。希望本书能够帮助各位读者，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">解决大家学习</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中遇到的一些困惑。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书主要内容</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的一些基础知识，包括通用寄存器、几条常用的指令，以及内存分页的实现原理</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍指针的实现原理，包括指针构成、相关操作，以及</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章围绕函数进行一系列探索，包括栈帧布局、调用约定、变量逃逸、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Function Value</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、闭包、</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">defer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">panic</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍方法的实现原理，包括接收者类型、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Method Value</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和组合式继承等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章围绕接口对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的动态特性展开探索，包括装箱、方法集、动态派发、类型断言、类型系</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">统和反射等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">6</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的实现，包括</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">模型、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的创建与退出、调度循环、抢占式调度、</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">timer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">netpoller</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和监控线程等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍同步的原理及其相关的组件，包括内存乱序、原子指令、自旋锁、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中的互</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">斥锁和信号量，以及</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">sync.Mutex</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">channel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍堆内存管理，包括</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">heapArena</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">mspan</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等几种主要的数据结构，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">mallocgc</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">函数的主要逻</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">辑，以及</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的三色抽象、写屏障等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍栈内存管理，包括</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">goroutine</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">栈的分配、增长、收缩和释放等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">阅读建议</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本书写作过程主要使用了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go 1.16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">及之前的几个版本，为了避免后续版本可能发生的不兼容问题，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">相关示例建议使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go 1.13</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go 1.16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编译运行。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">阅读本书不需要精通汇编语言、操作系统，但是需要对进程、线程这类基本概念有所了解。毕竟</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言可直接构建生成系统原生的可执行文件，如果想要深入理解一些语言特性的实现原理，还</span>

---

## Page 14

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是建议学习并实践一下多线程编程、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">IO</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">多路复用这类关键技术。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第一部分主要包括指针和函数，笔者希望大家能够通过这部分内容，对运行时栈及函数栈帧的相对</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寻址方式有深入的理解，为后续探索打下坚实的基础。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第二部分想要表达对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Lippman</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大师的崇高敬意，至今难忘初次阅读《深度探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对象模型》时那</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">种</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">“</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">初闻大道，喜不自胜</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">”</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的心情。按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Lippman</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大师的解释，对象模型应该是编译器对自定义数据</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型的建模，指导了对象内存布局及其他一些数据结构和代码的生成。只有理解了语言特性的实现</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">原理，才真正是磨刀不误砍柴工。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第三部分从服务器端程序开发的角度，梳理了如何从最初的多进程、多线程，逐渐发展到现在的协</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">程。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的调度逻辑还是比较复杂的，但是最核心的思想就是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">IO</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">多路复用与协程的结合，让每</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个任务有自己独立的栈，而同步的核心就是确立</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Happens Before</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">条件。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">第四部分从堆和栈两方面，梳理了内存管理的实现。内存分配方面应重点关注主要的数据结构。至</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">方面，应先理解宏观层面的整体思想和流程，然后去研究一些细节会更加容易。整个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">实际上是个不可分割的整体，在这里会看到内存管理对类型系统的依赖。</span>

---

## Page 15

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">致谢</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">感谢那些喜爱</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的网友对笔者的支持；感谢清华大学出版社的赵佳霓编辑；感谢我的家人，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">尤其是和我一起讨论技术问题并帮忙整理书稿的妻子，给予我莫大的支持。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">由于时间仓促，并且受限于笔者水平，书中难免有不妥之处，请读者见谅，并提宝贵意见。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">封幼林</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000"> </span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2022</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">年</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">月</span>

---

## Page 16

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">第</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">1</span><span style="font-family:'SimSun';font-size:20.99pt;color:#000000">章</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">汇编基础</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">20</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">世纪</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">90</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">年代，随着</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Microsoft Windows</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">系统在世界范围流行，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">公司的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CPU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">占据了个人</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">计算机的主要市场。近十年来，开源的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Linux</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">系统日渐成熟完善，伴随着云计算的热潮，各大互联</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">网巨头纷纷推出了基于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86_64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的弹性计算服务，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构又占据了服务器的主要市场。本章只</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">简单讲解</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编语言的必要基础知识，其目的是为后续研究</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的底层特性做好准备。熟悉</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的读者，可以跳过本章直接阅读后续章节。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">文中所使用的寄存器名称，以及示例汇编代码都符合</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编风格，与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言自带的反汇编工具</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有一些差异，在本章的最后会进行简单的对比说明。</span>

---

## Page 17

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">1.1 x86</span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">通用寄存器</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本节简单介绍一下</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的通用寄存器，包括</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86_64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构，后者是由</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AMD</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">公司首先推出的，也称为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构。因为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构是基于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位扩展而来的，保持了向前兼</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">容，所以本节先介绍</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构，再介绍</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位进行了哪些扩展。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.1.1 32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CPU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的通用寄存器，在汇编语言中可以通过名称直接引用这</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个寄存器。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令编码中的编号和名称如表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-1 Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令编码中</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个通用寄存器的编号和名称</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">其中编号为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个寄存器还可以进一步拆分。如图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的低</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位可以单独使用，引用</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">名称为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">又可以进一步拆分成高字节的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AH</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和低字节的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">两个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位寄存器。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-1 EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器的结构</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ECX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EDX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EBX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器都是按照表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示的方式设计的。这种设计让开发者能够非常方</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">便地对不同大小的数据进行操作。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编号为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的寄存器的结构设计</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p017_img01.jpg)

编号  名称  编号  名称
0  EAX  4  ESP
1  ECX  5  EBP
2  EDX  6  ESI
3  EBX  7  EDI



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p017_img02.jpg)

---

## Page 18

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编号为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个寄存器，低</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位也有独立的名称，但是没有对应的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位寄存器，如表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示。可</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">以认为这</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位寄存器是为了向前兼容</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8086</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的程序中很少使用。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编号为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的寄存器的结构设计</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有些通用寄存器是有特殊用途的：</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器会被乘法和除法指令自动使用，通常称为扩展累加寄存器。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ECX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">被</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">LOOP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">系列指令用作循环计数器，但是多数上层语言不会使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">LOOP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令，一般通过条</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">件跳转系列指令实现。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">用来寻址栈上的数据，很少用于普通算数或数据传输，通常称为扩展栈指针寄存器。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESI</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EDI</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">被高速内存传输指令分别用来指向源地址和目的地址，被称为扩展源索引寄存器和</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">扩展目标索引寄存器。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EBP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在高级语言中被用来引用栈上的函数参数和局部变量，一般不用于普通算数或数据传</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">输，称为扩展帧指针寄存器。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">除了这些通用寄存器之外，还有一个标志寄存器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EFLAGS</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">比较重要。汇编语言中用于比较的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">TEST</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">会修改标志寄存器里的相关标志，再结合条件跳转系列指令，就能实现上层语言中的大部分</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">流程控制语句，此处不进一步展开。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">最后还有一个很重要而且很特殊的寄存器，即指令指针寄存器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。指令指针寄存器中存储的是下</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">一条将要被执行的指令的地址，而且汇编语言中不能通过名称直接引用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，只能通过跳转、</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CALL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RET</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等指令间接地修改</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的值。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.1.2 64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构把通用寄存器的个数扩展到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个，之前的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个通用寄存器也被扩展成了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，每个寄存器</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的低</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位都可以单独使用。寄存器结构设计如表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-4 64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构下</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个通用寄存器的结构设计</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p018_img01.jpg)

32位  16位  高8位  低8位
EAX  AX  AH  AL
ECX  CX  CH  CL
EDX  DX  DH  DL.
EBX  BX  BH  BL.



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p018_img02.jpg)

32位  16位  32位  16位
ESP  SP  ESI  S1
EBP  BP  EDI  DI

---

## Page 19

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令指针</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">被扩展为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，但依然不能在代码中直接引用。标志寄存器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EFLAGS</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">被扩展为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RFLAGS</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，里面的标志位保持向前兼容。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">内存地址也扩展到了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，实际上目前的硬件只使用了低</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">48</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1.3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节介绍内存分页机制时会进</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">行相关说明。</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p019_img01.jpg)

64位  32位  16位  8位
RAX  EAX  AX  AL.
RCX  ECX  CX  CL
RDX  EDX  DX  DL.
RBX  EBX  BX  BL
RSP  ESP  SP  SPL
RBP  EBP  BP  BPL
RSI  ESI  SI  SIL.
RDI  EDI  DI  DIL
R8~R15  R8D~R15D  R8W~R15W  R8B~R15B

---

## Page 20

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">1.2 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">常用汇编指令</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的汇编指令一般由一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">opcode</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（操作码）和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">到多个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">operand</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（操作数）组成，大多数指令包含</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">两个操作数，一个目的操作数和一个源操作数。为了便于理解上层语言中一些特性的实现，下面简</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">单介绍几条常用的指令。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.2.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">整数加减指令</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ADD</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令进行整数的加法运算，该指令有两个操作数，第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数也叫作目的操作</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数，第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数也叫作源操作数。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ADD</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令把两个操作数的值相加，然后把结果存放到目的操作</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数中。源操作数可以是寄存器、内存或立即数，而目的操作数需要满足可写的条件，所以只能是寄</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">存器或内存，而且两个操作数不能同时为内存。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">如下指令将</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器的值加上</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，并把结果存回</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中，指令如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">整数减法运算通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SUB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令来完成，对操作数的要求和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ADD</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令一致，不过是从目的操作数中减</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">去源操作数，并把结果存回目的操作数中。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">如下指令将</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器的值减去</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，并把结果存回</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中，就像高级语言中分配函数栈帧时所做的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">那样，指令如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包括</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ADD</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SUB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在内的很多汇编指令能够接受不同大小的参数，例如通过两个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位寄存器进行</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int8</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">加法，指令如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">通过两个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位寄存器进行</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">加法，指令如下：</span>

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是一个复杂指令集架构，很多指令像这样支持多种操作数组合，虽然代码中使用同一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">opcode</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">名称，但是实际编译后对应的是不同的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">opcode</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。上层语言中的数据类型会指导编译器，在编译阶段</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">选择合适的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">opcode</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和对应的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">operand</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.2.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数据传输指令</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有多种数据传输指令，这里只简单介绍最常用的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOV</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOV</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令主要用来在寄存器之间</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">及寄存器和内存之间传输数据，也可以用来把一个立即数写到寄存器或内存中。第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数称为</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">目的操作数，第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数是源操作数，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOV</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令用于把源操作数的值复制到目的操作数中。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ECX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器的值复制到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器中，指令如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">把数值</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1234</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">复制到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EDX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器中，指令如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p020_img01.jpg)

ADD EAX,16



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p020_img02.jpg)

SUBESP,32



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p020_img03.jpg)

ADDAL,CL



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p020_img04.jpg)

ADDAX,CX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p020_img05.jpg)

MOV EAX,ECX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p020_img06.jpg)

MOVEDX,1234

---

## Page 21

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">因为涉及从内存中读写数据，所以接下来有必要了解一下</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">常用的几种内存寻址方式，实际上很</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">多指令会涉及内存寻址，不过跟数据传输放在一起讲解更容易理解。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令中可以直接给出内存地址的偏移量，又称为位移，也可以通过一项或多项数据计算得到一个地</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">址。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Displacement</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">：位移，是一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位或</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的值。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Base</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">：基址，存放在某个通用寄存器中。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Index</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">：索引，存放在某个通用寄存器中，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">不可用作索引。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Scale</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">：比例因子，用来与索引相乘，可以取值</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">经过计算得到的地址称为有效地址，计算公式如式（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）所示。</span>

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Base</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Index</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Displacement</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可以随意组合，任何一个都可以不存在，如果不使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Index</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">也就没有</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Scale</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Index</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Scale</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">主要用来寻址数组和多维数组，这里不继续展开。下面简单介绍基于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Base</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Displacement</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的寻址。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）位移（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Displacement</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）：一个单独的位移表示距离操作数的直接偏移量。因为位移被编码在指</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">令中，所以一般用于编译阶段静态分配的全局变量之类。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）基址（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Base</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）：将内存地址存储在某个通用寄存器中，寄存器的值可以变化，所以一般用于</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">运行时动态分配的变量、数据结构等。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）基址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位移（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Base+Displacement</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）：基址加位移，尤其适合寻址运行时动态分配的数据结构</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的字段，以及函数栈帧上的变量。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">如下</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">条汇编指令分别使用位移、基址和基址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位移这</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">种寻址方式，指令如下：</span>

<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.2.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">入栈和出栈指令</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1.1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节在介绍通用寄存器的时候，提到过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器有特殊用途，被</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CPU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">用作栈指针。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的一些指</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">令虽然不直接以</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">为操作数，但是会隐式地修改</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的值，例如入栈和出栈指令。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">入栈指令</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PUSH</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">只有一个操作数，即要入栈的源操作数。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PUSH</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令会先将</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">向下移动一个位置，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">然后把源操作数复制到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指向的内存处，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等价于：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">最后这个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOV</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">用作基址进行寻址。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">出栈指令</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">POP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">也只有一个操作数，是用来接收数据的目的操作数。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">POP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令会先把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指向的内存</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">处的值复制到目的操作数中，然后把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">向上移动一个位置，代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p021_img01.jpg)

EffectiveAddress=Base+（IndexXScale）+Displacement(1-1)



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p021_img02.jpg)

MOVEAX,[16]
MOV EAX,[ESP]
MOV EAX,[ESP+16]



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p021_img03.jpg)

PUSHEAX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p021_img04.jpg)

SUBESP,4
MOV[ESP],EAX

---

## Page 22

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等价于：</span>

<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.2.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">分支跳转指令</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的指令指针寄存器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">始终指向下一条将要被执行的指令，但是汇编代码中并不能通过名称直接</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">引用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，所以无法通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOV</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">之类的指令修改</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的值。有一系列用于进行分支跳转的指令会隐式</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">地修改</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EIP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的值，例如无条件跳转指令</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">JMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">JMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令只有一个操作数，可以是一个立即数、通用寄存器或内存位置，通过这个操作数给出了将</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">要跳转到的目的地址，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">跳转操作与过程调用不同，不记录返回地址。除了无条件跳转指令，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">还提供了一组条件跳转指</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">令，根据标志寄存器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EFLAGS</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中的不同标志位来决定是否跳转，此处不一一介绍。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.2.5 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">过程调用指令</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">绝大多数的上层语言提供了函数这一语言特性，在汇编语言中被称为过程。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的过程调用通过</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CALL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令实现，该指令和跳转指令一样只有一个操作数，也就是过程的起始地址。可以认为</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CALL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">JMP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的基础上多了一步记录返回地址的操作，返回地址就是紧随</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CALL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">之后的下一条指令</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CALL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令先把返回地址入栈，然后跳转到目的地址执行。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">目的地址也可以经由一个立即数、通用寄存器或内存位置来给出。假如下一条指令的地址为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，代</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等价于：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">子过程执行完成后通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RET</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令返回，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RET</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令会从栈上弹出返回地址，并跳转到该地址处继续执</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">行。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RET</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令有两种格式，一种没有操作数，只用来完成返回地址弹出和跳转，另一种有一个立即数参</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数，在上层语言实现某些调用约定时用来调整栈指针，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等价于：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p022_img01.jpg)

POPEAX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p022_img02.jpg)

MOVEAX,[ESP]
ADDESP,4



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p022_img03.jpg)

//跳转到地址32处
JMP32
//跳转目的地址经由EAX给出
JMPEAX
//跳转目的地址经由内存位置给出
JMP[EAX+32]



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p022_img04.jpg)

CALLEAX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p022_img05.jpg)

PUSH32
JMPEAX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p022_img06.jpg)

RET8

---

## Page 23

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">远调用（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Call far</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）和远返回在上层语言中基本不会用到，这里不予介绍。</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p023_img01.jpg)

RET
ADDESP,8

---

## Page 24

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">1.3 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">内存分页机制</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.3.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">线性地址</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">DOS</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">时代，应用程序直接访问物理内存，代码中的地址都是实际的物理内存地址。任何程序都有</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">权读写所有的物理内存，稍有不慎就会覆盖掉其他程序的代码或数据，连操作系统内核也无法自</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">保。随着</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">80386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">芯片的到来，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进入了保护模式，并且开启了内存分页模式，通过特权级和进程地</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">址空间隔离机制，解决了上述问题。如今，主流的操作系统采用分页的方式管理内存。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在分页模式下，应用程序中使用的地址被称为线性地址，需要由</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MMU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Memory Management</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Unit</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）基于页表映射转换为物理地址，整个转换过程对于应用程序是完全透明的。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.3.2 80386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">两级页表</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">80386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的线性地址的宽度为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，所以可以寻址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大小的空间，与进程的地址空间大小相对</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">应。地址总线为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，硬件可以寻址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的物理内存。分页机制将每个物理内存页面的大小设定为</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4096</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节，并按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4096</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对齐。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">因为每个页面的大小为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4096</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节，并且地址总线的宽度为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，所以每个页面中正好可存储</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1024</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">物理页面的地址。完整的页表结构的第一层是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个页目录页面，其物理地址存储在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CR3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器中，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">通过页目录页面进一步找到第二层的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1024</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个页表页面。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的线性地址被</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MMU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">10</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+10</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+12</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位划分，整个地址转换过程如图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示。前两个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">10</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">取值范围都是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1023</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，分别用作页目录和页表的索引。最后的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">12</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，取值范围为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4095</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，用作最</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">终的页面内偏移。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-2 80386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">线性地址到物理地址的转换</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.3.3 PAE</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">三级页表</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">80386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的线性地址的宽度为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，每个进程拥有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的线性地址空间。主流操作系统一般按照</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">∶</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">或</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">∶</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的方式进一步将进程的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">地址空间划分为用户空间和内核空间。因为内核只有一份，</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p024_img01.jpg)

---

## Page 25

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所以内核占用的这组物理页面由所有进程共享，而每个进程独享自己</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">或</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的用户空间，即所</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">谓的进程地址空间隔离就是通过进程独立的页表实现的，然而硬件</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的地址总线只能寻址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">物理内存，在多进程的操作系统上，每个进程实际能够映射到的物理页面远远不足</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。在这种情</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">况下，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">推出了物理地址扩展技术（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Physical Address Extension</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PAE</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PAE</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">将地址总线拓展到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">36</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，从而使硬件能够寻址多达</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的物理内存。线性地址的宽度仍然是</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MMU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的页表映射机制需要进行相应调整，以支持从</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位线性地址到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">36</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位物理地址的映射。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">为了支持</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">36</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的物理地址，页目录和页表中的地址项被调整为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，一个页面只能存储</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">512</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个地</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">址。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MMU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">将</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的线性地址按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+12</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位划分，整个地址转换过程如图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示，在页</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">目录之前又加了一层页目录指针，总共三级页表映射。高两位用来选择一个页目录，接下来的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">用来选择一个页表，再用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位来选择一个物理页面，加上最后</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">12</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的偏移值，最终确定一个物理地</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">址。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-3 PAE</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">线性地址到物理地址的转换</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.3.4 x64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">四级页表</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PAE</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">技术，虽然硬件支持的物理内存变大了，但进程的地址空间大小并没有变化。对于某些类</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">型的程序，例如数据库程序，进程</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">～</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3GB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的用户地址空间成为明显的瓶颈，而且</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的数据宽度</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">也无法满足时下的计算需求，所以</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位架构应运而生了。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">推出的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">IA64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构因为与原来的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构不兼容，所以没能普及，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AMD</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">公司通过扩展</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">推出</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构，因为良好的向下兼容性而被广泛采用。常见的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86_64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">都是指</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构，如今的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个人计算机基本是基于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">上，寄存器的宽度变成了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，而线性地址实际只用到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">48</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，也就是最大可寻址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">256TB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">内存。很少有单台计算机会安装如此大量的内存，所以没有必要实现</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">48</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位的地址总线，常见的个人</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">计算机的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CPU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址总线实际还不到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">40</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，例如笔者的计算机的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Core i7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">实际只有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">36</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位。服务器的</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CPU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址总线的宽度会更大，例如</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Xeon E5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">系列能达到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">46</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">PAE</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的基础上进一步把页表扩展为四级，每个页面的大小仍然是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4096</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MMU</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">将</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">48</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的线性地址按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+12</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位划分，整个地址转换过程如图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示。高</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位选择一个页</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p025_img01.jpg)

---

## Page 26

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">目录指针表，再用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位选择一个页目录，接下来的两个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位分别用于选择页表和物理页面，最后的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">12</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位依然用作页内偏移值。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-4 amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">线性地址到物理地址的转换</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">1.3.5 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">虚拟内存</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">乍看起来，完整的页表结构会占用大量的内存，例如在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">80386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">上就会占用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1+1024</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">＝</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1025</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个物理页</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">面。因为页目录本身也被用作页表，所以实际上是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1024</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个页面，总共占用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4096×1024</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">＝</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4MB</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的空</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">间。因为系统空间是所有进程共享的，所以对应的页表也是共享的，而大多数进程并不会申请大量</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的用户空间内存，用不到的页表也不会被分配，所以进程的页表是稀疏的，并不会占用太多的内</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">存。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进程是以页面为单位向操作系统申请内存的，操作系统一般只是对进程已申请的区间进行记账，并</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">不会立刻映射所有页面。等到进程真正去访问某个未映射的页面时，才会触发</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Page Fault</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">异常，操</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作系统注册的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Page Fault Handler</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">会检查内存记账：如果目标地址已申请，就是合法访问，系统会分</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">配一个物理页面并完成映射，然后恢复被中断的程序，程序对这一切都是无感的；如果目标地址未</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">申请，就是非法访问，系统一般会通过信号、异常等机制结束目标进程。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">当物理内存不够用的时候，操作系统可以把一些不常使用的物理页面写到磁盘交换分区或交换文</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">件，从而能够将空出的页面给有需要的进程使用。当被交换到磁盘的页面再次被访问时，也会触发</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Page Fault</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，由</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Page Fault Handler</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">负责从交换分区把数据加载回内存。程序对这一切都是无感的，并</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">不知道某个内存页面到底是在磁盘上，还是在物理内存中，所以称为进程的虚拟内存。</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p026_img01.jpg)

---

## Page 27

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">1.4 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">汇编代码风格</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言使用的汇编代码风格跟最常见的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">风格和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AT&amp;T</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">风格都不太相同，根据官方文档的说法，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是基于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Plan 9</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编器的风格做了一些调整。本节简单对比</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的风格差异。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">1</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．操作数的宽度</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编中通过指令的后缀来判断操作数的宽度，后缀</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">W</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">代表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，后缀</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">L</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">代表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，后缀</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Q</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">代表</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位，不像</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编中有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RAX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">不同的寄存器名称。例如对于整数自增指令，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">风格的代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对应的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编风格的代码如下：</span>

<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">2</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．操作数的顺序</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对于常见的有两个操作数的指令，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编中操作数的顺序与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编中操作数的顺序是相反的，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">源操作数在前而目的操作数在后。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">例如</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">转换成</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的代码如下：</span>

<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">3</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．地址的表示</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有效地址的计算公式如式（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1-1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）所示，如果要用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ESP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作为基址寄存器，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">EBX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作为索引寄存器，比</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">例系数取</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，位移为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，则可以分别给出两种风格的代码。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的代码如下：</span>

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的代码如下：</span>

<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">4</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．立即数格式</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编中的立即数类似于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AT&amp;T</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">风格的立即数，需要加上</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">$</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">前缀。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p027_img01.jpg)

INCEAX
INCRCX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p027_img02.jpg)

INCLAX
INCQCX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p027_img03.jpg)

MOV EAX,ECX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p027_img04.jpg)

MOVLCX,AX



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p027_img05.jpg)

[ESP+EBX2+16]



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p027_img06.jpg)

16(SP)(BX2)

---

## Page 28

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编的代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p028_img01.jpg)

MOV EAX,1234



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p028_img02.jpg)

MOVL S1234,AX

---

## Page 29

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">1.5 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本章简单介绍了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构的通用寄存器、内存寻址方式和比较关键的几组指令。了解了操作系统以</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">页面为单位的内存管理机制，以及分页模式下线性地址到物理地址的映射过程。还简要地对比了</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编风格与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Intel</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">汇编风格的几点不同。鉴于后续章节中将会经常用到反汇编技术来探索</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的特性，所以本章内容旨在让读者掌握必要的汇编基础知识。</span>

---

## Page 30

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">第</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">2</span><span style="font-family:'SimSun';font-size:20.99pt;color:#000000">章</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">指针</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针凭借其灵活强大的内存操作能力，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C++</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中扮演着非常重要的角色，但也因一些常见的安</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">全问题给人们带来很多困扰。指针在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中被保留了下来，但是影响力似乎大大降低了，出于</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">安全方面的考虑，指针运算等一些重要特性被移除，使指针显得不再那么重要。在学习过程中，有</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">很多人对值类型、指针或引用类型，以及值和地址这些概念感到困惑。本章从指针的构成出发，首</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">先理解指针的本质，然后逐一分析指针的常见操作的实现原理，以及常见的问题和解决方法，最后</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">介绍关于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包的一些思考和实践。</span>

---

## Page 31

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">2.1 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">指针构成</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中，声明一个指针变量的示例代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">变量名为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，其中的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">*int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">为变量的类型。对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">*int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进一步拆解，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">*</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表明了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是一个指针变量，用来存储一</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个地址，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是指针的元素类型，也就是当</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中存了一个有效地址的时候，该地址处的内存会被解</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">释为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">无论指针的元素类型是什么，指针变量本身的格式都是一致的，即一个无符号整型，变量大小能够</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">容纳当前平台的地址。例如在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">386</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构上是一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位无符号整型，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构上是一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位无符</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">号整型。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有着不同元素类型的指针被视为不同类型，这是语言设计层面强加的一层安全限制，因为不同的元</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">素类型会使编译器对同一个内存地址进行不同的解释。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.1.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">地址</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中，一个有效的地址就是一个无符号整型数值，运行阶段用来在进程的内存地址空间中</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">确定一个位置。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章中简单地介绍了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的几种常用寻址方式，指针一般会用到基址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位移的寻址方式。例如当</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针元素的类型为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">时，通过指针访问</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">元素的代码被编译成汇编指令，就是将某个通用寄存器</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">用作基址进行寻址。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构下通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">go build</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">命令编译一个示例，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">自带的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">objdump</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">工具反编译</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">main.read</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数，得到的汇编代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p031_img01.jpg)

varp*int



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p031_img02.jpg)

//第2章/code2_1.go
package main
func main(）(
n:=10
println(read(8n))

//go:noinline
funcread（p*int)(v int）(
V=p
return

---

## Page 32

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在第一条</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令中，第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0x8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）表示参数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在栈上的地址，关于函数栈帧布局，将</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">会在第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章中详细介绍，目前只要理解这条指令的作用是把参数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中存储的地址值复制到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中即可。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在第二条</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令中，第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作为基址加上位移</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，也就是用基址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">+</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">位移的方式寻</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">址指针</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指向的数据，所以这条指令的作用就是把目标地址处的值复制到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在第三条</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令中，第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个操作数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0x10</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）表示栈上返回值的地址，所以这条指令的作用就</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中存储的值复制到返回值</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">v</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">经过上面三条指令，便可成功地把指针</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指向的数据复制到函数返回值空间。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.1.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">元素类型</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针本身就是个无符号整型，这一点不会因不同的元素类型而有所不同，而元素类型会影响编译器</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">如何对指针中存储的地址进行解释，这一点也可以通过汇编代码进行验证。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">把第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">/code_2_1.go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">read</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数修改为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">read32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数，其主要目的是改变参数和返回值的类</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">型，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">修改后的代码重新进行编译和反编译，得到的汇编代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可以看到第一条用于复制指针存储的地址的指令没有发生变化。第二条指令中的内存寻址单元</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">）也没有变，而原本后两条</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令现在变成了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，表明复制的数据长度发生了变</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">化，从</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节变成了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节。造成这一变化的原因正是指针元素类型从</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">变成了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p032_img01.jpg)

$go tool objdump-S-smain.read’gom.exe
TEXT main.read（SB)C:/gopath/src/fengyoulin.com/gom/code_2_1.go
V=P
0x488ee0  488b442408  MOVQOxB(SP),AX
0x488ee5  488b00  MOVQO(AX),AX
return
0x488ee8  4889442410  MOVQAX,0x10(SP)
0x488eed  C3  RET



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p032_img02.jpg)

//第2章/code2_2.go
//go:noinline
funcread32（p*int32）（vint32）（
V=p
return



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p032_img03.jpg)

$go toolobjdump-S-s‘main.read32'gom.exe
TEXTmain.read32（SB)C:/gopath/src/fengyoulin.com/gom/code 2_2.go
VP
0x488f30  488b442408  MOVQ0x8（SP),AX
0x488f35  8b00  MOVLO(AX),AX
return
0x488f37  89442410  MOVLAX,0x10(SP)
0x488f3b  3  RET

---

## Page 33

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">2.2 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">相关操作</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本节分析指针常见操作及其底层实现原理，也会介绍指针所引发的那些广受诟病的问题，以及在</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中如何解决这些问题。此外，一些指针特性受限于安全问题，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中不能直接使用，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在本节也会探讨一些替代方案及背后的思考。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.2.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">取地址</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针中存储的是地址，而地址一般通过取地址运算符获得，或者在动态分配内存时由</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">new</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">之类的函</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数返回。在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中取地址运算符与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言相比似乎没什么变化，编译器会确保应用取地址运算符</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的变量类型与指针的元素类型是一致的。下面仍然通过反编译一个简单的函数，来看一下取地址运</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">算符到底做了什么。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构下通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">go build</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">命令编译一个示例，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">反编译</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">main.addr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数得到的代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">其中</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">LEAQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令的作用就是取得</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">main.n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址并装入</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寄存器中。后面的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令则把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">AX</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的值</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">复制到返回值</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">这里获取的是一个包级别变量</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址，等价于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的全局变量，变量</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址是在编译阶段静态</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">分配的，所以</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">LEAQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令通过位移寻址的方式得到了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">main.n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的地址。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">LEAQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">同样也支持基于基址和</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">索引获取地址，具体可参考第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章所介绍的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">寻址方式。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中，不应该将函数内某个局部变量的地址作为返回值返回，虽然编译器允许这样的代码通</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">过编译，但在代码逻辑上却属于明显的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Bug</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。因为函数一旦返回，栈帧随即销毁，这部分内存会被</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">后续的函数栈帧覆盖，所以通过返回的指针读写栈上的数据就可能会造成程序异常崩溃，虽然也有</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可能不会崩溃，但是基于错误的数据继续运行下去，会变得更加难以调试和排查。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中，通过逃逸分析机制避免了此类问题。来看一个示例，代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p033_img01.jpg)

//第2章/code_2_3.go
package main
var n int
funcmain（）{
println（addr())

//go:noinline
func addr（）（p*int）
return&n



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p033_img02.jpg)

$go toolobjdump-S-s‘main.addr'gom.exe
TEXTmain.addr(SB)C:/gopath/src/fengyoulin.com/gom/code_2_3.go
return&n
0x488f90  488d05691f0f00  LEAQmain.n(SB),AX
0x48897  4889442408  MOVQAX,0x8(SP)
0x488f9c  C3  RET

---

## Page 34

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">其中变量</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">实际上是在堆上分配的，因为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">逃逸到堆上，所以即使</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">newInt</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数返回，函数栈帧销</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">毁，也不会影响后续正常使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的指针。待到第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章介绍函数时再进一步介绍逃逸。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.2.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">解引用</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">通过指针中的地址去访问原来的变量，就是所谓的指针解引用。在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2.1.1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节已经通过反编译验证了指</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">针的解引用过程，就是把地址存入某个通用寄存器，然后用作基址进行寻址。接下来就介绍一下</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中与指针解引用相关的几个常见问题，以及这些问题在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中是如何解决的。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">1</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．空指针异常</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所谓空指针，就是地址值为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的指针。按照操作系统的内存管理设计，进程地址空间中地址为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的内</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">存页面不会被分配和映射，保留地址</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">0</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在程序代码中用作无效指针判断，所以对空指针进行解引用</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">操作就会造成程序异常崩溃，程序代码在对指针进行解引用前，始终要确保指针非空，因而需要添</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">加必要的判断逻辑。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所以遭遇空指针异常并非语言设计方面的缺陷，而是程序逻辑上的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Bug</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中对空指针进行解</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">引用会造成程序</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">panic</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（宕机）。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">2</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．野指针问题</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">野指针问题一般是由于指针变量未初始化造成的。众所周知，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中声明的变量需要显式地初始</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">化，否则就是内存中上次遗留的随机值。对于未初始化的指针变量而言，如果内存中的随机值非</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">零，就会使指针指向一个随机的内存地址，而且会绕过代码中的空指针判断逻辑，从而造成内存访</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">问错误。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">为了解决</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言变量默认不初始化带来的各种问题，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中声明的变量默认都会初始化为对应类</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">型的零值，指针类型变量都会初始化为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">nil</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，而代码中的空指针判断逻辑能够避免空指针异常，从</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">而使问题得到解决。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:8.25pt;font-weight:bold;color:#000000">3</span><span style="font-family:'SimSun';font-size:8.25pt;color:#000000">．悬挂指针问题</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中，程序员需要手动分配和释放内存，而所谓悬挂指针问题，就是指程序过早地释放了内</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">存，而后续代码又对已经释放的内存进行访问，从而造成程序出现错误或异常。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言实现了自动内存管理，由</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">负责释放堆内存对象。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">基于标记清除算法进行对象的存活分</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">析，只有明确不可达的对象才会被释放，因此悬挂指针问题不复存在。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.2.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">强制类型转换</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">基于指针的强制类型转换非常高效，因为不会生成任何多余的指令，也不会额外分配内存，只是让</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">编译器换了一种方式来解释内存中的数据。出于安全方面的考虑，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言不建议频繁地进行指针</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">强制类型转换。两种不同类型指针间的转换需要用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">作为中间类型，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可以</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和任意一种指针类型互相转换。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构下反编译一个函数，代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p034_img01.jpg)

//第2章/code2_4.go
//go:noinline
func newint（）（p*int）（
varnint
return &n

---

## Page 35

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">得到汇编代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">把指针的类型强转换为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">后，原本的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令变成了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，没有产生任何额外指令，所以</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">转换效率是非常高的。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.2.4 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针运算</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中，指针和不指定长度的数组，在元素类型相同的情况下是可以等价使用的，指针加上一</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个整数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">等价于取数组中下标为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">n</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的元素的地址。指针可以进行加减运算，给操作多维数组带来了很</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大方便，但也经常会造成内存访问越界问题。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中的数组必须指定长度，并且是值类型，与指针不再等价，指针运算也不再支持，这些都</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是出于安全考虑的。数组的长度在编译时期能够确定，编译器可以生成代码检测下标越界问题，而</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针则不然，编译器无法确定指针运算的安全边界，所以无法保证其安全性。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">集成了数组和指针的优点，既能像指针那样关联一个可以动态增长的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Buffer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，又能像</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数组那样让编译器生成下标越界检测代码，在某些场合可以考虑用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">代替指针运算。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">如果还想像</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中那样直接进行指针运算，就需要借助</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进行转换。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2.2.3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节中已经提</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可以与任何一种指针类型互相转换，除此之外</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">还可以与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">互相转</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">换，而后者可以进行整数运算。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">假如有一个元素类型为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的指针</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，要把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">移动到下一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的位置，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中可以通过指针的自增</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">运算实现，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中等价的代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中实现此功能就显得有些烦琐了，先把</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">p</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">转换为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型，再进一步转换为</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型，然后加上一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的大小，再转换回</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型，最终转换为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">*int</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型。</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p035_img01.jpg)

//第2章/code2_5.go
//go:noinline
func convert（p*int）{
q：=（int32）（unsafe.Pointer（p））
*q=0
人



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p035_img02.jpg)

$go toolobjdump-S-s'main.convert'gom.exe
TEXTmain.convert（SB）C:/gopath/src/fengyoulin.com/gom/code_25.go
*4=0
0x488fa0  488b442408  MOVQ0x8（SP),AX
0x488fa5  C70000000000  MOVLSOxO,0(AX)
0x488fab  C3  RET



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p035_img03.jpg)

+tpi



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p035_img04.jpg)

p=（*int)（unsafe.Pointer（uintptr（unsafe.Pointer(p))+unsafe.Sizeof（*p))）

---

## Page 36

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">2.3 unsafe</span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">包</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本节简单地介绍</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2.2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节中已经用到了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进行指针的强制类型转换和</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指针运算，实际上就是人为地干预编译器对内存地址的解释方式，这些能力对于研究语言的底层实</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">现来讲是不可或缺的。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">代码中用好</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，能够优化程序的性能，想必很多人都见过经典的类型转换，代码如下：</span>

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Slice Header</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">结构只是比</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">String Header</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">结构多了一个容量字段，相当于内嵌了一个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">String Header</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，如</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-1 String Header</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Slice Header</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的结构</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">用这种强制类型转换的方式可以避免额外的内存分配，从而减少程序的开销，但是也会带来一些风</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">险。因为按照</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的设计思想，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">string</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的内容是不可修改的，但是</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">元素是可以修改的，基于</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">上述方法得到的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">string</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">与原来的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">共享底层</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Buffer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，如果不经意修改了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">就可能会造成程序逻辑</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">错误。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">根据官方文档的说法，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包包含的操作绕过了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的类型安全机制，使用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包会造成程</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">序不可移植，并且不受</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go 1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">兼容性准则的保护。那么</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">到底该不该用呢？本节就围绕这个问题</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进行一些分析研究。</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.3.1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">标准库与</span><span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">keyword</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本节主要分析</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包的本质，到底是标准库还是一组</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">keyword</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。这个思考源于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2.2.4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">节进行指针运</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">算时用到的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中是个关键字。先从源码入手，梳理</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包都提供了</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">些什么，代码如下：</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p036_img01.jpg)

func convert(s[]byte)string(
return *（=string)(unsafe.Pointer（&s）)



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p036_img02.jpg)

---

## Page 37

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">根据源码中的注释，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">ArbitraryType</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在这里只是用于文档目的，实际上并不属于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包，它可以表</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">示任意的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表达式类型。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数用来返回任意类型的大小，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Offsetof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数用来返回任</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">意结构体类型的某个字段在结构体内的偏移，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Alignof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数用来返回任意类型的对齐边界，</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">最重要的是这</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个函数的返回值都是常量。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">基于上述信息，已经可以断定</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">并不是一个真实的包，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">提供的这些能力不是标准库层面</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">能够实现的。指针强制类型转换本来就是在编译阶段实现的，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Offsetof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Alignof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数返回的是常量值，也就要求返回值必须在编译阶段确定，所以必须由编译器直</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">接支持。可以通过实验进行验证，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">平台，反编译</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">size</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数得到汇编代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">这条</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">MOVQ</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令直接向返回值</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">o</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中写入了立即数</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，也就说明</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数在编译阶段就被转换成</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">了立即数，与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">并无区别。上述测试方法同样适用于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Offsetof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Alignof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">函数。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">既然这些都是由编译器直接支持的，本质上跟</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">keyword</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">一样，为什么</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言要放到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包中呢？</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">根本原因还是出于安全考虑。直接的任意操作内存的能力可以让程序员写出更高效的代码，但是也</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">因为过于灵活而让编译器无法落实安全检查，从而使程序变得不安全。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">这个名字就旨在提醒</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">程序员，内存操作有风险，要谨慎！</span>
<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.3.2 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">关于</span><span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">uintptr</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">很多人都认为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是个指针，其实不然。不要对这个名字感到疑惑，它只不过是个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uint</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，大小与</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">当前平台的指针宽度一致。因为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">可以跟</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">互相转换，所以</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中可以把指针转</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">换为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">进行数值运算，然后转换回原类型，以此来模拟</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中的指针运算。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">需要注意的是，不要用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">来存储堆上对象的地址。具体原因和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有关，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在标记对象的时候</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">会跟踪指针类型，而</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">不属于指针，所以会被</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">GC</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">忽略，造成堆上的对象被认为不可达，进而</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">被释放。用</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">就不会存在这个问题了，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Pointer</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类似于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">C</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">void*</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，虽然未指</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">定元素类型，但是本身类型就是个指针。</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p037_img01.jpg)

//一个”任意类型”定义
type ArbitraryTypeint
//指针类型定义
type Pointer ArbitraryType
//3个工具函数原型（只有原型，没有实现）
func Sizeof(xArbitraryType)uintptr
func Offsetof(x ArbitraryType）uintptr
func Alignof(x ArbitraryType)uintptr



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p037_img02.jpg)

//第2章/code2_6.go
//go:noinline
func size(）（ouintptr）{
0=unsafe.Sizeof（o)
return



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p037_img03.jpg)

$gotoolobjdump一S-s'main.size'gom.exe
TEXTmain.size(SB）C:/gopath/src/fengyoulin.com/gom/code2_6.go
return
0x488fb0  48c744240808000000  MOVQS0x8,0x8（SP)
0x488fb9  c3  RET

---

## Page 38

<span style="font-family:'LiberationSerif-Bold';font-size:10.50pt;font-weight:bold;color:#000000">2.3.3 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">内存对齐</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">硬件的实现一般会将内存的读写对齐到数据总线的宽度，这样既可以降低硬件实现的复杂度，又可</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">以提升传输的效率。有些硬件平台允许访问未对齐的地址，但是会带来额外的开销，而有的硬件平</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">台不支持访问未对齐的地址，当遇到未对齐的地址时会直接抛出异常。鉴于这些原因，编译器在定</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">义数据类型时，还有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">runtime</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在分配内存时，都要进行对齐操作。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言的内存对齐规则参考了两方面因素：一是数据类型自身的大小，复合类型会参考最大成员</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">大小；二是硬件平台机器字长。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">机器字长是指计算机进行一次整数运算所能处理的二进制数据的位数，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">平台可以理解成数据</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">总线的宽度。当数据类型自身大小小于机器字长时，会被对齐到自身大小的整数倍；当自身大小大</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">于机器字长时，会被对齐到机器字长的整数倍。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Sizeof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe.Alignof</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">（）函数可以得到目标数据类型的大小和对齐边界，表</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">给出了常见内置类型的大小和对齐边界。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">表</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-1 </span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">常见内置类型的大小和对齐边界</span>

<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">complex</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">类型由实部和虚部两个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">float</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">组成，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">complex64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">相当于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">[2]float32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">complex128</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">相当于</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">[2]float64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，所以对齐边界分别与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">float32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">、</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">float64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">一致。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">map</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">多数情况下会被分配在堆上，本地只有一个指针指向堆上的数据结构，而指针的对齐边界自然</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">相同。</span>
<span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">string</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">slice</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的结构定义可参考</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">reflect.StringHeader</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">与</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">reflect.SliceHeader</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">它们的对齐边界与其最大的成员，即类型</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">uintptr</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的对齐边界相同。值得强调的是对于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">struct</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">而言，每</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个成员都会以结构体的起始地址为基地址，按自身类型的对齐边界对齐。除此之外，整个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">struct</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">还</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p038_img01.jpg)

32位平台  64位平台
类  型
大小  对齐边界  大小  对齐边界
bool  1  1  1  1
int8.uint8  1  1
intl6、uint16  2  2  2  2
int32、uint32、float32  4  4  4  4
int64、uint64、float64  8  4  8  8
int、uint、uintptr  8  8
complex64  8  7  8  4
complex128  16  4  16  8
string  8  4  16  8
slice  12  24  8
map  4  4  8  8



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p038_img02.jpg)

type StringHeader struct{
Data uintptr
Lenint
type SliceHeader struct
Datauintptr
Len-int
Capint

---

## Page 39

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">要按照成员中最大的对齐边界进行对齐，所以编译器会按需要在结构体相邻成员之间及最后一个成</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">员之后添加</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">padding</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，因此需要合理地排列数据成员的顺序，从而使整个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">struct</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的空间占用最小化。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">来看一个示例，代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数据类型</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">s1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">amd64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构上占用了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">32</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节空间，如图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示，在</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">a</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">b</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">之间有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">7</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">padding</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，目的</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">是让成员</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">b</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对齐到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">c</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">d</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">之间有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">padding</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，为的是让成员</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">d</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对齐到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">4</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。又因为整个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">struct</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的成员</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">中最大的对齐边界为</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">int64</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">对应的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，所以</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">e</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">之后还有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">6</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">padding</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，使整个结构体对齐到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">8</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，但是</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">这样总共浪费了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节空间，空间利用率只有</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">50%</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-2 s1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">内存布局</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">接下来通过调整结构体成员的位置，尽量避免编译器添加</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">padding</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，调整后的代码如下：</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">如图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-3</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">所示，数据类型</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">s2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">和之前的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">s1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">有着相同类型的</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">5</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">个数据成员，但是经过人为优化成员的顺序</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">后，编译器没有添加任何</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">padding</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，整个</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">struct</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">占用了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">16</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">字节空间，利用率达到</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">100%</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">。</span>

<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">图</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">2-3 s2</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">内存布局</span>

![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p039_img01.jpg)

//第2章/code2_7.go
type sl struct
aint8
b int64
cint8
dint32
eintl6



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p039_img02.jpg)

01  8  1617  20  24  26  32
padding  3
b  6字节
7字节  字节



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p039_img03.jpg)

//第2章/code2_8.go
type s2struct
aint8
c int8
eint16
dint32
bint64



![](./深度探索Go语言：对象模型与runtime的原理、特性及应用 (封幼林) (Z-Library)_assets/images/p039_img04.jpg)

0  2  4  8  16

a  C  e  d  b

---

## Page 40

<span style="font-family:'LiberationSerif-Bold';font-size:14.99pt;font-weight:bold;color:#000000">2.4 </span><span style="font-family:'SimSun';font-size:14.99pt;color:#000000">本章小结</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">本章首先从指针的构成开始讲解，通过反汇编的方式，展示了编译器如何使用指针存储的地址进行</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">内存寻址，以及元素类型对指令生成的影响。后续又介绍了与指针相关的操作、常见问题和解决方</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">法。最后结合指针强制类型转换的实例介绍了</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">包，并通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">的实际应用，了解了内存对</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">齐的原理。在接下来对</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言特性的探索中，</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">unsafe</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">也会起到非常重要的作用。</span>

---

## Page 41

<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">第</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000">3</span><span style="font-family:'SimSun';font-size:20.99pt;color:#000000">章</span><span style="font-family:'LiberationSerif-Bold';font-size:20.99pt;font-weight:bold;color:#000000"> </span>
<span style="font-family:'SimSun';font-size:20.99pt;color:#000000">函数</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">函数在主流的编程语言中是一个基础且重要的特性。通过函数对逻辑单元进行封装，使代码结构更</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">加清晰，便于实现代码复用，基于函数的编译链接技术让构建大型应用程序更为方便。也正因为函</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">数太过于基础，所以很多人对于其底层细节并不甚关心，在实际应用中便会遇到一些问题。本章从</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">函数的底层实现开始研究，逐步梳理</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">语言中与函数相关的特性，旨在理解其背后的设计思想。</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">从代码结构来看，层层函数调用就是一个后进先出的过程，与数据结构中的入栈出栈操作完全一</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">致，所以非常适合用栈来管理函数的局部变量等数据。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">架构提供了对栈的支持，本书第</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">1</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">章汇编</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">基础部分介绍了栈指针寄存器</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">SP</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">，以及入栈出栈对应的指令。</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">x86</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">还通过</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">CALL</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令和</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">RET</span><span style="font-family:'SimSun';font-size:10.50pt;color:#000000">指令实</span>
<span style="font-family:'SimSun';font-size:10.50pt;color:#000000">现了对过程的支持（汇编语言中的过程等价于</span><span style="font-family:'LiberationSerif';font-size:10.50pt;color:#000000">Go</span><span style="font-family:'SimSun';font-size