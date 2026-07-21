## Page 1

基互联网大厂济深工程特国写，多年领城实践经验总结
攻克业务难题  数据库技术
作者以问题引导式调解，全润别析存储引掌特性、
LSM紧系引擎的宏观原理、微观设计要点与主流源码实项

深入浅出
存储引擎
文小飞著

EXPLORING
STORAGE
ENGINES
From the Basics to
the Advanced

机械工业出版社

<!-- 图源: ./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p001_scan.jpg -->

---

## Page 2

内容简介
本书由某互联网大厂资深工程师横写，带你研
读存储引擎的底层支撑技术、主流派系及其设计与
实现精髓。作者特意采用经典计算机图书的福序渐
进方式讲解，不新抛出一个个引导你患考的问题，
让你渐入佳填，从纷繁复杂的产品和业务中抽取出
本质，从容应对多种存储与系统难题。
全书共9章，分为三部分。第一部分（第1-3
章）讲解存储引掌的全貌，涉及存储引擎中高操使
用的数据结构、存储介质等。为深入学习后面的内
客做铺垫。第二部分（第4~6章）介细基于B+树的
存储引擎。重点介绍为什么选择B+别作为存储引擎
素引结构、B+树存储引掌解决哪些问题以及如何解
决，并以BoIDB存储引摩项目为例来讲解核心原理
与实现脂节。第三部分（第7-9章）介绍基于LSM
源系的存储引擎，重点介绍LSM Tree中各组件的
动能及作用,最后部析了LeveIDB项目的核心原理
与实现细节。

<!-- 图源: ./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p002_scan.jpg -->

---

## Page 3

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">说明</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">每个页内部的关键字编号</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">（</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">如</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">key1</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">key2</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 等</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">）</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">均是为了方便区分设</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">定的</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">不同页中同样的编号没有关联</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">关系</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">这个设定对本章中所有描述</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">B+</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">树存储引擎的图都适用</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">比如</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">根节</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">点中</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">branch2</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 的</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">key2</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 实际上和分支节</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">点中</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">branch1</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 的</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">key1</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 是相等的</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 val2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index2</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4-10</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储引擎的完整结构</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index1</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 pgid2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch2</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 pgid2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch1</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">根节点</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">分支节点</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.01pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.01pt;color:#231f20">key1 val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.01pt;color:#231f20">leaf1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.01pt;color:#231f20">key2 val2</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">块索引中块编号指</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">向的块</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">块索引中最小值关</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">键字指向的节点</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">块间链接的指针</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">记录索引项</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">块索引项</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">记录数据</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">叶子节点</span>

---

## Page 4

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#ee7e80">page 4</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#ee7e80">叶子节点</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#ee7e80">page 7</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#ee7e80">叶子节点</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 val2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 4</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 9</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">7</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">4</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 3</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 8</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4-14</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 磁盘和内存中的</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 pgid2</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 7</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 pgid2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 val1</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#ee7e80">page 0</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#ee7e80">根节点</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">branch1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20"> val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 6</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#ee7e80">page 2</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.00pt;color:#ee7e80">非叶子节点</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">key2 val2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 0</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">page 5</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">内存</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">index1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.00pt;color:#231f20">leaf1</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">磁盘</span>

---

## Page 5

<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Put(key, value)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Delete(key)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Batch(Tx)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Update(Tx)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">View(Tx)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Begin(Tx)</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">5(branch)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">10(leaf)</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">4(leaf)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">9(leaf)</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">bucketElm1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">bucketElm2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">bucketElm</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">b_key1 b_val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">b_val2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">b_key2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">b_key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20"> b_val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20"> pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val2</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6-1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 整体实现架构</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">3(freelist)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">8(branch)</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">pgid</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key2 pgid2</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key2 pgid2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">n</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">…</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">7(branch)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">2(meta)</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key1 pgid1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">branch1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:6.00pt;font-style:italic;color:#231f20">i</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Get(key)</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">6(freepage)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">1(meta)</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">用户层</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Open()</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">key1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">val1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">leaf1</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">内存层</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">磁盘层</span>

---

## Page 6

<span style="font-family:'ArialMT';font-size:8.00pt;color:#ffffff">1. </span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">合并方式：分级合并、分层合并</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#ffffff">2. </span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">合并时机：定时合并、达到阈值合并等</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">多个小文件数据读到内存中遍历合并，</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">效率低下不可取</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">2.2</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 合并策略怎么定呢</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">？</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">1.</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 如何提升合并效率</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">？</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">选择支持排序的数据结构组织数据，例如红</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">黑树、跳表、</span><span style="font-family:'ArialMT';font-size:8.00pt;color:#ffffff">B+</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff"> 树等</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">保证每个文件写入的数据有序，利用多</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">路归并思路合并</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">2.1</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 如何保证每个文件中的数据有序呢</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">？</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">a.</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 怎么保证内存中的</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">数据有序呢</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">？</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">为保证数据的持久性，所有写操作写</span><span style="font-family:'ArialMT';font-size:8.00pt;color:#ffffff">WAL</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">日志，异常重启时根据</span><span style="font-family:'ArialMT';font-size:8.00pt;color:#ffffff">WAL</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff"> 日志恢复数据</span><span style="font-family:'ArialMT';font-size:8.00pt;color:#ffffff"> </span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">用户写操作的数据，先在内存中缓存一</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">段时间，然后排好序时，再写磁盘</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">b.</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 进程异常数据</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">丢了怎么办</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">？</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7-3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据写过程推导总结</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Delete(k)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Put(k,v)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Set(k,v)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Get(k)</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Open()</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.95pt;color:#231f20">用户接口层</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:7.95pt;color:#231f20">…</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Write</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">2. </span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20">写</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">MemTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">MemTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">1. </span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20">从</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">MemTable</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20"> 读取</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Read</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">2. </span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20">从</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">ImmuMemTable</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20"> 读取</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">3. </span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20">从</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">SSTable</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20"> 读取</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:7.95pt;color:#231f20">内存层</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">ImmuMemTable</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">1. </span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20">写</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.96pt;color:#231f20">WAL</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.96pt;color:#231f20"> 日志</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Flush &amp; Minor Compact</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">level0</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">level1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">10MB</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Major Compact</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">Major Compact</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:7.95pt;color:#231f20">磁盘层</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">100MB</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:7.95pt;color:#231f20">…</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">WAL</span><span style="font-family:'FZS3K--GBK1-0';font-size:7.95pt;color:#231f20"> 日志</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">level</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:7.95pt;font-style:italic;color:#231f20">N</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">SSTable</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">10</span><span style="font-family:'TimesNewRomanPS-ItalicMT';font-size:4.77pt;font-style:italic;color:#231f20">N</span><span style="font-family:'TimesNewRomanPSMT';font-size:4.77pt;color:#231f20">+1</span><span style="font-family:'TimesNewRomanPSMT';font-size:7.95pt;color:#231f20">MB</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9-1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LevelDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的整体架构</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p006_img01.jpg)



![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p006_img02.jpg)

---

## Page 7

<span style="font-family:'FZDHTJW--GB1-0';font-size:15.00pt;color:#ffffff">数据库</span><span style="font-family:'FZHTJW--GB1-0';font-size:10.00pt;color:#ffffff">技术丛书</span>

<span style="font-family:'HelveticaNeueLTStd-Roman';font-size:15.84pt;color:#939598">EXPLORING STORAGE ENGINES</span>

<span style="font-family:'HelveticaNeueLTStd-Lt';font-size:10.00pt;color:#939598">From the Basics to the Advanced</span>

<span style="font-family:'FZLTDHK--GBK1-0';font-size:61.18pt;color:#231f20">深入浅出</span>
<span style="font-family:'FZLTDHK--GBK1-0';font-size:61.18pt;color:#231f20">存储引擎</span>

<span style="font-family:'FZLTXHK';font-size:10.00pt;color:#231f20">文小飞   著</span>

---

## Page 8

<span style="font-family:'SimHei';font-size:10.48pt;color:#231f20">图书在版编目</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.48pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.48pt;color:#231f20">CIP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.48pt;color:#231f20">）</span><span style="font-family:'SimHei';font-size:10.48pt;color:#231f20">数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">深入浅出存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 文小飞著.—北京：机械工业出版社，</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2024.4</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">（数据库技术丛书）</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">ISBN  978-7-111-75300-1</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">Ⅰ.①深… Ⅱ.①文… Ⅲ.①存储技术 Ⅳ.①</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">TP333</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">中国国家版本馆</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">CIP</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据核字（</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2024</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">）第</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">052872</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 号</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">机械工业出版社</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">（北京市百万庄大街</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">22</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 号 邮政编码</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">100037</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">）</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">策划编辑：杨福川      责任编辑：杨福川  侯 颖</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">责任校对：张勤思 李 杉  责任印制：郜 敏</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">三河市宏达印刷有限公司印刷</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">2024</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 年</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">5</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 月第</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">1</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 版第</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">1</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 次印刷</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">186mm</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">×</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">240mm</span><span style="font-family:'FZSSJW--GB1-0';font-size:8.00pt;color:#231f20">·</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">23.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 印张</span><span style="font-family:'FZSSJW--GB1-0';font-size:8.00pt;color:#231f20">·</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">2</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 插页</span><span style="font-family:'FZSSJW--GB1-0';font-size:8.00pt;color:#231f20">·</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">506</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 千字</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">标准书号：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">ISBN  978-7-111-75300-1</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">定价：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">99.00</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 元</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">电话服务</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">网络服务</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">客服电话：</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">010-88361066 </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">机 工 官 网：</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">www.cmpbook.com</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">010-88379833 </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">机 工 官 博：</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">weibo.com/cmp1952</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">010-68326294 </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">金  书  网：</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">www.golden-book.com</span>

<span style="font-family:'SimHei';font-size:9.00pt;color:#231f20">封底无防伪标均为盗版</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">机工教育服务网：</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">www.cmpedu.com</span>

---

## Page 9

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>

<span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20">为什么要写这本书</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在互联网行业中，存储是一个非常重要的领域。所有的互联网应用都离不开数据的存</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">储和检索。然而，存储系统是计算机中非常复杂的一类系统。想掌握它，不仅需要掌握数</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">据结构、算法、操作系统等知识，还要掌握分布式系统相关知识。因此，存储领域的入门</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">门槛较高，对于初学者或者存储爱好者而言并不友好。不幸的是，我也属于存储爱好者这</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">个行列。此外，在日常工作和求职时，存储相关知识的运用和考察占比非常大。如果对存</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">储系统的原理有深入的理解，在工作时能够更好地编写高效的软件，在求职时也将是明显</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的优势和加分项。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">目前业界的存储系统主要包括关系数据库（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MySQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Oracle</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等）、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MongoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">InfluxDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OrientDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等）、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">TiDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OceanBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">CockroachDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等），以及消息队列中间件（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Kafka</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">RocketMQ</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Pulsar</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等）。这些存储系</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">统绝大部分都是分布式系统，它们将多个单机节点有序地组织在一起来提供服务。如果按</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">照模块化的思路进行拆解，这类系统可以分为单机存储组件和分布式组件。单机存储组件</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">主要针对单个实例，关注数据如何高效地存储和检索，主要考虑数据如何组织、索引如何</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">维护、数据如何在磁盘上布局、单机事务如何支持等问题。而分布式组件则更多关注多</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">个实例之间的数据同步、故障发生后的故障自动迁移、数据一致性的保证、数据分片等</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">问题。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">虽然不同类型的存储系统有很多差异，但在本质上它们之间存在一些通用的技术点。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">市场上有几本相关的书籍介绍这些内容，比如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Martin Kleppmann</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 所著的《数据密集型应用</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">系统设计》和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Alex Petrov</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 所著的《数据库系统内幕》等。此外，还有一些主要介绍关系数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">库的书籍，比如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Hector Garcia-Molina</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等人所著的《数据库系统实现》、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Abraham Silberschatz</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">等人所著的《数据库系统概念》和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Baron Schwartz</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等人所著的《高性能</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MySQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">》等。对于</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">第一类书籍，我阅读完后发现其广度非常大，每个技术点都介绍到了，但在具体的技术专题</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">上缺乏深度，需要搜罗其他资料进行深入学习；而第二类书籍更多的是从理论上介绍，读者</span>

<span style="font-family:'RoughSpring-Regular';font-size:14.00pt;color:#808285">  Preface  </span><span style="font-family:'MicrosoftYaHei';font-size:14.00pt;color:#231f20">前  言</span>

---

## Page 10

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">IV</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">阅读完以后很难直接上手去剖析任何一款数据库的源码。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2020</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 年年底，由于工作需要，我有幸负责了单机嵌入式的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（键值对）存储引擎的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">调研工作，随后接触并研究了</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LevelDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">RocksDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">PebblesDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Bitcask</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等项目。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在调研过程中我花费了很多的时间和精力，也走了很多弯路。当时在网上搜索了很多资料，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">但没有解开我内心的困惑。当时想，如果市面上有一本系统地介绍存储引擎的书就好了（比</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">如存储引擎的分类、适用场景、设计上的共通之处及工程实现等），这样的书可以帮助我少</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">走很多探索的弯路。后来偶然的机会接触到了《数据密集型应用系统设计》这本书，一读</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">就被这本书的内容深深地吸引了。我反复读了第</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章，每次读完书中对存储引擎简明扼要</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的介绍时，都有一种豁然开朗的感觉。在后来不断研究的过程中，我对单机的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">擎有了深入的认识和个人理解。其间，我将其作为一个专题在团队内部和外部社区进行了</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">分享，收到了不错的反馈，也有幸帮助到了一些技术小伙伴。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">后来我想，像我当初一样，在入门存储时存在各种疑惑的初学者可能有很多。于是我</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">决定尝试动手写一本解开上述疑惑的书，记录研究过程中的一些经验和感悟。于是，有了</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">这本书。本书将给读者一个全新的视角，秉承大道至简的主导思想，聚焦于单机存储引擎</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本身，重点分析存储引擎如何处理存储和检索，编写上采用理论结合实践的方式，并给出</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">项目源码分析。本书不仅仅是某种技能的分享，更致力于建立方法论，分享个人的一些想</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">法和见解，希望能够抛砖引玉，为读者拓展出更深入、更全面的思路，帮助存储初学者和</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">爱好者知其然并知其所以然。最后，希望本书能够填补存储领域的一些空白。</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20">本书特色</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本书主要有三个目标。首先，分析存储引擎和各种存储系统之间的关系，使读者明确</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">存储引擎在存储系统中的位置和角色。其次，给出存储引擎的整体框架和分类，使读者对</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">存储引擎有全面的了解。最后，对于每种存储引擎，主要关注数据的存储和检索过程，解</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">释每类存储引擎背后的设计思想和方案选择，使读者既知其然又知其所以然。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本书在写作上采用了理论结合实践的方式。每一类存储引擎的介绍，分为理论部分</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">和实践部分。理论部分重点介绍设计方案和思想，不仅告诉读者每一类存储引擎能解决</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">什么问题、适用于什么场景，还告诉读者为什么它们能解决这些问题。在介绍设计原理</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的基础上，配套一个开源项目进行核心源码的分析，帮助读者更深入地理解存储引擎的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">原理。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本书的目的不是介绍某个项目或技术，而是阐述存储引擎背后通用的设计思想和方法</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">论。我在前人的基础上抽象和整理出来的方法论可以帮助读者更好、更快、更轻松地理解</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">存储引擎，解决存储领域门槛较高的问题。此外，这些设计思想不局限于存储系统，读者</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在深刻理解后可以在计算机的其他系统中复用。因此，处于不同工作阶段的不同人群可能</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">有不同的阅读感受，读者可以根据需要在不同阶段多次阅读本书。</span>

---

## Page 11

<span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20">读者对象</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在阅读本书之前，希望读者对计算机基本知识有一个大致的了解，同时具备一定的编</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">程基础，至少熟悉一种编程语言（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">C++</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 或</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Go</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）等。如果有一些关系数据库或者其他数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">库的经验会更好，否则阅读起来可能有些许困难。本书的读者对象主要包括：</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'ZapfDingbats';font-size:10.00pt;color:#231f20">❑</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据库架构师。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'ZapfDingbats';font-size:10.00pt;color:#231f20">❑</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">开发应用架构师。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'ZapfDingbats';font-size:10.00pt;color:#231f20">❑</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据库开发人员。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'ZapfDingbats';font-size:10.00pt;color:#231f20">❑</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">后端开发人员。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'ZapfDingbats';font-size:10.00pt;color:#231f20">❑</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">存储、数据库爱好者。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'ZapfDingbats';font-size:10.00pt;color:#231f20">❑</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">其他计算机从业人员。</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20">如何阅读本书</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本书共</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章内容，从逻辑上分为三部分。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">第一部分为</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">存储引擎基础</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">，一方面对存储引擎进行概述，另一方面介绍存储引擎中高</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">频使用的数据结构和存储介质。这部分包括以下</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章内容。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">1</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">首先对互联网上的各种存储系统进行不同维度的分类，并在此基础上分析其内</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">部数据存储的核心</span><span style="font-family:'FZSSK--GBK1-0';font-size:14.14pt;color:#231f20">—</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">存储引擎。接着对存储引擎进行分类。本章是提纲挈领的一章。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">2</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">按照读</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 写的时间复杂度从低到高的顺序介绍存储引擎中索引高频使用的数据结</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">构。涉及的数据结构包括数组、链表、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（哈希）表、位图、布隆过滤器、二叉搜索树、</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">红黑树、跳表、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树等。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">3</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">对存储引擎中的存储介质进行介绍，主要包括内存、持久化内存、磁盘等介质。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本章内容涉及了大量的操作系统知识，例如虚拟内存、文件系统等。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">第二部分为</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">基于</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 树的存储引擎</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">，重点讨论处理读多写少场景的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎的相</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">关内容。这部分包括以下</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章内容。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">4</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">从宏观角度分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎的原理。这一章采用了逐步推导的思路来展开介</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">绍，告诉读者</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎背后的方案选型和取舍。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">5</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">从微观角度介绍</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎中的细节。一方面介绍了</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎的正常</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">处理流程、边界条件的处理过程、异常情况的应对方案；另一方面介绍了存储引擎中事务</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的实现方案和多版本并发控制等内容。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">6</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">以</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎为例，分析其核心源码实现逻辑。本章是实践内容，通过对</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 核心源码的分析，使读者更好地理解</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎的内部工作原理。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">第三部分为</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">基于</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">LSM</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 派系的存储引擎</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">，主要介绍处理写多读少场景的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 派系存储</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">引擎的相关内容。这部分包括以下</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章内容。</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">V</span>

---

## Page 12

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">VI</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">7</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">也采用了逐步推导的方式，首先介绍</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树）存储引擎的内部原理</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">和演变过程，其次对</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的几个核心问题（如数据合并、数据分区、放大问题、写放</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">大优化等）进行详细的介绍。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">8</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">对</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 派系的各类存储引擎（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Array</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、消息队</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">列</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Kafka</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等）进行阐述。其中，在介绍</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 时重点对</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 分离存储技术</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">WiscKey</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 进行</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">了详细的讲解。</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">9</span><span style="font-family:'FZHTK--GBK1-0';font-size:10.00pt;color:#231f20"> 章</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">以</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LevelDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 为例，对其核心源码进行剖析。通过前面两章的理论介绍和本章的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">源码分析，读者可以深入理解</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎的原理。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">其中，第</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章、第</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章、第</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章、第</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">7</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章、第</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">8</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 章为本书的重点。如果你没有充足的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">时间阅读全书，可以选择性地阅读重点章节。</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20">勘误和支持</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">由于我的水平有限且编写时间紧张，书中难免会出现一些错误或者不准确的地方，恳</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">请读者批评指正。读者可以通过邮箱</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2282186474@qq.com</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 反馈宝贵的意见和建议，期待与</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">大家在技术交流中互勉共进。</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20">致谢</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">感谢那些为开源项目做过分享的技术大咖和发表过学术论文的学者，以及各社区和平</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">台上的技术爱好者，尤其是《数据密集型应用系统设计》的作译者。他们的开源和分享对</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本书的编写起到了至关重要的作用。在编写本书的过程中，我一方面参考了大量相关论文、</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">项目源码和资料，另一方面也参考了一些非常优秀的博客文章。这些资料对我的研究和探</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">索也起到了非常重要的作用。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">感谢我的妻子王淑明女士，为写作这本书，我牺牲了很多陪伴她的时间。也感谢我的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">其他家人，他们的关怀给了我坚持写作的动力。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">特别感谢我职业生涯中的导师杨天琳先生，在本书写作之前的方案调研和准备过程中，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">他给了我很多建议。此外，在日常工作中我们进行过很多次技术讨论和交流，他给了我很</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">多帮助。没有他在技术上的指导，我估计不会有写作本书的计划。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">谨以此书献给我最亲爱的家人、朋友，以及为计算机行业做出巨大贡献的大师们。</span>

<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">文小飞</span>

---

## Page 13

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>

<span style="font-family:'FZXBSK--GBK1-0';font-size:10.00pt;color:#231f20">前言</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">1</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 存储引擎概述</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··························1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据存储体系</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ································1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 与</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">HTAP ·············1</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 关系数据库、</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据库与</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">  </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据库</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····················2</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.1.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 内存型存储组件与磁盘型存储</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">  </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 组件</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····································8</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.1.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 读多写少组件、写多读少组件</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">  </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 和读多写多组件</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················9</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.1.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据存储与检索</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················· 10</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据存储的核心：存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·········· 10</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储引擎整体架构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················· 10</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储引擎的共性问题</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·············· 13</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎的分类</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··························· 13</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 读多写少：基于</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树的存储</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">  </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·································· 14</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 写多读少：基于</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 派系的</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">  </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························· 15</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············································ 17</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">2</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 索引数据结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················18</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 基础数据结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······························ 18</span>

<span style="font-family:'RoughSpring-Regular';font-size:14.00pt;color:#808285">Contents</span><span style="font-family:'AdobeSongStd-Light';font-size:14.00pt;color:#808285">  </span><span style="font-family:'RoughSpring-Regular';font-size:14.00pt;color:#808285">  </span><span style="font-family:'MicrosoftYaHei';font-size:14.00pt;color:#231f20">目  录</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数组</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··································· 18</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 链表</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··································· 20</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 类数据结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························· 22</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 表</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······························· 22</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 位图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··································· 27</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.2.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 布隆过滤器</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························· 28</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 二叉树类数据结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ························ 32</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 二叉搜索树</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························· 33</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 红黑树</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ································ 36</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.3.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 跳表</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··································· 45</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 多叉树类数据结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ························ 48</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.4.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··································· 49</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.4.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·································· 57</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">2.4.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 其他多叉树</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························· 61</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············································ 61</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">3</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 数据存储介质</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················64</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 内存</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············································ 65</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">3.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 内存的基本内容</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················· 65</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">3.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 内存管理机制</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················· 69</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">3.1.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 虚拟内存管理机制</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················· 80</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 持久化内存</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·································· 92</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 磁盘</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············································ 96</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">3.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 磁盘的基本内容</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················· 97</span>

---

## Page 14

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">VIII</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">3.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 磁盘管理机制</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················102</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">3.3.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 加速磁盘访问的方案</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············· 111</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································112</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">4</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 </span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20"> </span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">从宏观角度理解</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">B+</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 树存储</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20"> </span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 引擎的原理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ························ 113</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">4.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎产生的起点</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········114</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 诞生的背景</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························114</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 设计的目标</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························116</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">4.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎方案选型</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··············117</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据结构方案对比</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················117</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 目光转向磁盘</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················118</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.2.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 索引维护和存储</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················121</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.2.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 选择</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树还是</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·············125</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">4.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 树存储引擎方案选型结果</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ········128</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方案选型结果</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················128</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">4.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 反向论证</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························130</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">4.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································130</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">5</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 </span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20"> </span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">从微观角度理解</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">B+</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 树存储</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20"> </span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 引擎的工程细节</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················· 132</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 边界条件处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····························132</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树在磁盘和内存中的映射</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···132</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 读操作的处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················133</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.1.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 写操作的处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················137</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 异常情况处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····························154</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 异常情况总体分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················154</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据部分写入的异常处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······156</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 事务</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································158</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 事务的基本概念</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················158</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">5.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 并发控制</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························160</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 范围查询与全量遍历</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················170</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································171</span>

<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">6</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 </span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">BoltDB</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 核心源码分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ········ 172</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 整体结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························172</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 项目结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················172</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">BoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 整体实现架构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············173</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">page</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 解析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···································175</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">page</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 基本结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················176</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 元数据页</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························177</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.2.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 空闲列表页</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························179</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.2.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 分支节点页</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························183</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.2.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 叶子节点页</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························186</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">node</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 解析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···································187</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">B+</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 树结构概述</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····················187</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">node</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················187</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.3.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">node</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的增删改查</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················189</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.3.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">node</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 分裂</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························190</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.3.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">node</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 合并</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························195</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Bucket</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 解析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ································199</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.4.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Bucket</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················199</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.4.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> Bucket</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 遍历的</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Cursor</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 核心</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························201</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.4.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Bucket</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的增删改查</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················206</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.4.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据的增删改查</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··············210</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.4.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Bucket</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的分裂和合并</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·············211</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Tx</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 解析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······································213</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.5.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Tx</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ························213</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.5.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Commit()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方法分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················214</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.5.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Rollback()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方法分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···············217</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.6</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">DB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 解析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····································219</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.6.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">DB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················219</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.6.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Open()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方法分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················221</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.6.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Begin()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方法分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················224</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.6.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Update()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">View()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方法分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····226</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">6.6.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Batch()</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 方法分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················227</span>

---

## Page 15

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6.7</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································229</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">7</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 深入理解</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">LSM Tree</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 原理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··· 232</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">7.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的发展背景</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················232</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">7.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 从零推导</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····················234</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储介质的选择</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···················234</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 写请求的处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················234</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.2.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 读请求的处理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················239</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">7.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的架构演进</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················240</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据更新分类</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················240</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 双组件</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········241</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.3.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 多组件</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········242</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.3.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 实际的</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········243</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">7.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的核心问题</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················245</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.4.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据压缩</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 合并</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····················245</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.4.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据分区</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····························246</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.4.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 读放大、写放大和空间放大</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····249</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">7.4.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 写放大优化</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·························251</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">7.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································252</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">8</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 </span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">LSM</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 派系存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ············ 253</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">8.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····················253</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 工程应用</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···············253</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> LSM Tree</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 分离存储</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> </span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 技术</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">WiscKey ······················256</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">8.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">·····················264</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的起源</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·················264</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Bitcask</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的核心原理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···············265</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">8.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM Array</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····················269</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM Array</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的设计思想</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········269</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Moss</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的核心原理</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················270</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">8.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 其他</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LSM</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····················274</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.4.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LSM</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储引擎扩展</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···············274</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">8.4.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 消息队列</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Kafka</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储引擎</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······275</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">8.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································277</span>

<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">IX</span>

<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">第</span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">9</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 章 </span><span style="font-family:'TimesNewRomanPS-BoldMT';font-size:11.00pt;font-weight:bold;color:#231f20">LevelDB</span><span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20"> 核心源码分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······ 278</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">LevelDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 概述</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">······························278</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.1.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LevelDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 整体架构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················279</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.1.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">LevelDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 项目结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················280</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">DB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 核心接口分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ························282</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.2.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">DB</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····························282</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.2.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> Open(options,dbname,dbptr) </span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的实现</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······························284</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.2.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Put(k,v)</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Delete(k)</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的实现</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····285</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.2.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Get(k)</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的实现</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················292</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MemTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的实现分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·················294</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.3.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">MemTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····················294</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.3.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Add(k,v)</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Get(k)</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的实现</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······295</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.3.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">SkipList</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················297</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">WAL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 日志的实现分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················302</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.4.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">WAL</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 日志的格式</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··················302</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.4.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Writer</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的实现</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················303</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.4.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Reader</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的实现</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······················307</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.5</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SSTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的实现分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·····················311</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.5.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">SSTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的数据格式</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··············312</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.5.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Block</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的写入和读取</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ··············316</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.5.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">SSTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的写入和读取</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········325</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.5.4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">SSTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 的读取全过程</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········334</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.6</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Compact</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的实现分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····················338</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.6.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Compact</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 过程</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················339</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.6.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Minor Compact ·····················340</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.6.3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Major Compact  ····················343</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.7</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 多版本的实现分析</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ·······················352</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.7.1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">Version</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">VersionEdit</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ······352</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">9.7.2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">VersionSet</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 结构</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ····················356</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">9.8</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 小结</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ···········································361</span>
<span style="font-family:'FZXBSK--GBK1-0';font-size:11.00pt;color:#231f20">后记</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20"> ················································· 362</span>

---

## Page 16

<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">本章对数据存储体系、存储引擎的整体框架、存储引擎的分类进行概述，以便让读者</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">对存储引擎有一个基本的认识。</span>
<span style="font-family:'TimesNewRomanPS-BoldMT';font-size:14.00pt;font-weight:bold;color:#231f20">1.1</span><span style="font-family:'FZXBSK--GBK1-0';font-size:14.00pt;color:#231f20"> 数据存储体系</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">不同的存储场景需要选择不同的数据库，不同的设计目标也使得每个数据库背后的方</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">案选型、面临的问题不尽相同。例如有些组件设计之初就定位为关系数据库，而有些组件</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">则定位为</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库，随着技术的发展又诞生了</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库。本节将对互联网中</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">涉及存储数据的常用存储组件进行分类，并介绍它们的适用场景和设计目标。</span>
<span style="font-family:'-';font-size:11.50pt;color:#231f20">1.1.1 OLTP、OLAP 与HTAP</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（在线事务处理）数据库的主要功能是处理用户的在线实时请求，直接为用户提</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">供服务。因此，这类数据库通常对处理请求的时延要求比较高，正常情况下绝大部分请求</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">会在毫秒级完成。</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库很多，除了大家最熟悉的关系数据库（如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MySQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Oracle</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">外，还有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MongoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等非关系数据库。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（在线分析处理）数据库的主要功能是对数据或者任务进行离线处理，不直接为</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">用户提供服务。</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 系统对请求的处理通常比</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 慢得多，一般为秒级、分钟级甚至</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">小时级，通常在数据统计、报表分析、数据聚合分析等场景使用。这类数据库的典型代表</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Teradata</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Hive</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Presto</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Druid</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">ClickHouse</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。互联网企业往往都需要使用</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">，因此为了满足这两类需求，通常结合多个系统一起开发使用。这样的做法</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:12.00pt;color:#231f20">第</span><span style="font-family:'ArialMT';font-size:12.00pt;color:#231f20">1</span><span style="font-family:'FZHTK--GBK1-0';font-size:12.00pt;color:#231f20"> 章</span>

<span style="font-family:'RoughSpring-Regular';font-size:14.00pt;color:#808285">Chapter 1</span>

<span style="font-family:'MicrosoftYaHei';font-size:18.00pt;color:#231f20">存储引擎概述</span>

---

## Page 17

<span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">2</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">深入浅出存储引擎</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">当然是可行的，而且多数企业采用的也是这种方式。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">随着互联网技术的发展，需要存储的数据量呈爆炸式增长，这种模式带来的存储成本</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">问题成为新的矛盾点，人们开始探索是否有一种数据库能将</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 这两类应用合</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">二为一。于是，一种新的解决方案出现了，那就是下面要介绍的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（混合事务分析处理）</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据库。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 在设计时就充分考虑了对</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 两种场景的需求，通过在系统内部</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">实现上进行更好的兼容，为上层应用程序使用提供了统一的服务。在处理上述两种场景</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">时，底层可以使用同一套数据库来完成。这类数据库既可以处理在线事务，又可以进</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">行在线分析。可以认为</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP=OLTP+OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">。</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的主要代表有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">TiDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OceanBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">CockroachDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库有它的优点，但是也间接带来了很大的挑战。只使用</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库就可</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">以完成在线事务处理和在线分析处理这两类需求，这对用户而言无疑是一种好的选择，因</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">为底层采用同一套系统存储数据，在存储资源和成本上有很大的优势。但随之而来的是系</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">统复杂性的增加，这类数据库的复杂度相比纯</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库和纯</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库高很多，软</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">件开发难度也大很多。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 与</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 之间不同维度的对比见表</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1-1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">。</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">表</span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">1-1</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">OLTP</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">、</span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">OLAP</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> 与</span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">HTAP</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> 之间不同维度的对比</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">维度</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">OLTP</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">OLAP</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">HTAP</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">系统功能</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">日常交易处理（</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">在线事务</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">处理</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">）</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">统计、分析、报表（</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">在线</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">分析处理</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">）</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">同时支持在线事务处理和在线分</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">析处理</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">设计目标</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">面向实时交易类应用</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">面向统计分析类应用</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">服务于</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 两种场景</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">数据处理</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">当前的、最新的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">历史的、聚集的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">既支持最新数据处理，也支持历</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">史数据聚合分析</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">实时性</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">实时性读</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 写要求高</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">实时性读</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 写要求低</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">实时性要求高</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">事务</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">强事务</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">弱事务</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">强事务</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">分析要求</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">低、简单</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">高、复杂</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">高</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">典型代表</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">MySQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Oracle</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">MongoDB</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">HBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Teradata</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Hive</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Presto</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Druid</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">ClickHouse</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">CockroachDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">OceanBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">TiDB</span>
<span style="font-family:'-';font-size:11.50pt;color:#231f20">1.1.2 关系数据库、NoSQL 数据库与NewSQL 数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">关系数据库、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库这三者是现如今讨论很多的一组概念，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">其实要搞明白它们之间的区别，就不得不回到数据库的发展历程上来审视它们产生的背景</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">和动机。数据库的发展历程如图</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1-1</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 所示。</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p017_img01.jpg)

---

## Page 18

<span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">第1 章</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">存储引擎概述</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">3</span>

<span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">2010</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 年左右</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">得益于硬件的发展</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">内存的</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">容量和网络的带宽与延迟得</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">到了很大的提升</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">数据库架</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">构迎来变革</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">内存数据库和</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">分布式数据库大规模投入</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">生产</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">代表产品有</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Google </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Spanner</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">SAP HANA</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">SQL </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Server Hekaton</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Amazon </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Aurora</span>

<span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">20</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 世纪</span><span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">80</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 年代至</span><span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">90</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 年代</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">市面上涌现出大量关系数</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">据库产品</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">垂直领域逐渐</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">开始分化</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">代表产品有</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Oracle</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">DB2</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">SQL Server</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">PostgreSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">MySQL</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20"> 等</span>

<span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">20</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 世纪</span><span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">70</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 年代</span>
<span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">2000</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 年左右</span>
<span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">21</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 世纪</span><span style="font-family:'ArialMT';font-size:7.00pt;color:#231f20">10</span><span style="font-family:'FZHTK--GBK1-0';font-size:7.00pt;color:#231f20"> 年代以后</span>

<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">由</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">IBM</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20"> 研发的世界</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">上第一个关系数据库</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">System R</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20"> 面世</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">传统关系数据库的功能越</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">来越丰富</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">一些重要的</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">组件出现</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">比如</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">MySQL </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">InnoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Oracle RAC</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">与</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">此同时</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">满足海量数据存</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">储需求的</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20"> 数据库也</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">出现了</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">比如</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">MongoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Cassandra</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">HBase</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">延续了</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">21</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20"> 世纪</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">10</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20"> 年代初</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">期的辉煌</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">各种</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">NewSQL</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">数据库出现</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">可以承载更</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">加复杂的负载</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">。</span><span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">代表产品</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:6.00pt;color:#231f20">有</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">CockroachDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">TiDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">VoltDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:6.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:6.00pt;color:#231f20">Azure Cosmos DB</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1-1</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据库的发展历程</span>

<span style="font-family:'-';font-size:10.00pt;color:#231f20">1. 关系数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">关系数据库也称为</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库（为描述方便，后面简称为</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库）。最早的数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">库可以追溯至</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">20</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 世纪</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">70</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 年代</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">IBM</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 研发的第一个</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">System R</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">，这也是最早的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据库。再后来，</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">20</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 世纪</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">80</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 年代至</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">90</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 年代涌现出大量的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库产品，例如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Oracle</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">DB2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL Server</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">PostgreSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MySQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库按照以“行”为单位的二维表格存储数据，这种方式最符合现实世界中的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">实体，同时通过事务的支持为数据的一致性提供了非常好的保证。这既是</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库的优</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">势，也是它的缺陷。在面对海量数据存储、高并发访问的场景下，</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库的扩展性和性</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">能会受到限制。随着互联网的飞速发展，到</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2000</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 年左右，存储海量数据、高并发处理读</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 写</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的需求变得非常强烈，这对</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库提出了巨大的挑战。为了解决这个问题，出现了支</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">持数据可扩展性、最终一致性的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库。因此，</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库可以看作基于</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据库的缺陷而诞生的一种新产品。</span>
<span style="font-family:'-';font-size:10.00pt;color:#231f20">2. NoSQL 数据库</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 组件普遍选择牺牲复杂</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 的支持及</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">ACID</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 事务功能，以换取弹性扩展能力</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">和更高的读</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 写性能。这类系统主要存储半结构化或非结构化数据。根据存储的数据种类，</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库主要分为文档数据库（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Document-based Database</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）、键值数据库（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Key Value </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Database</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）、图数据库（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Graph-based Database</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）、时序数据库（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Time Series Database</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）、列式存</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">储（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Column-based Store</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）及多模数据库（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Multi-model Database</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）。</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p018_img01.jpg)



![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p018_img02.jpg)

---

## Page 19

<span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">4</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">深入浅出存储引擎</span>
<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1</span><span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">）文档数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">文档数据库以文档作为基本的单元进行操作。这里的文档并不是传统意义上的“文档”，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">它所指的是一条数据记录，类似关系数据库中的一行数据。这个记录是可以进行自我描述</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的，主要的形式有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">JSON</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">XML</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTML</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等，文档数据库中存储的每个文档可以具有完全</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">不同的结构。从广义上来看，文档数据库也是一种</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库，只不过它和键值数据库的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">区别在于它的值是文档。此外，文档数据库还可以通过文档内容构建复杂索引。这类数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">库除了</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MongoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 外，还有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">CouchDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OrientDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。这类数据库通常很容易和关系数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">进行数据转换。文档数据库非常适用于爬虫、物流、游戏、物联网等场景，存储一些数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">模型无法确定的数据。</span>
<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2</span><span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">）键值数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">键值数据库也就是一般意义上的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">KV</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库，它提供的功能和数据结构中的哈希</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Hash</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）表类似。通常添加或更新数据时调用</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Put(k,v)</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 接口，而在检索和删除时都只需要传</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">入</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">k</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 即可。一般用</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Get(k)</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 接口来获取数据，用</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Delete(k)</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 接口来删除数据。这类数据库最为</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">常用的是</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">，此外还有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Riak</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Amazon DynamoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。这类数据库的主要特点是读</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 写</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">性能超高，系统内部可以支持弹性扩展，主要适用于对性能要求比较高的单点读</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 写场景，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">例如推荐系统，用于存储用户或物品特征、用户对内容的互动信息（点赞数、收藏数）等。</span>
<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">3</span><span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">）图数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">图数据库是一种使用图数据结构进行语义查询的数据库。图是一组点和边的集合，“点”</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">表示实体，“边”表示实体间的关系，图数据库通过点和边来存储数据。鉴于它采用独特的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">图数据结构组织数据，类似于现实世界中的人际关系、交通网络，因此这类数据库比较适</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">用于社交网络、数据挖掘等场景。这类数据库的主要代表有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Neo4j</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Dgraph</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">TigerGraph</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。</span>
<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">4</span><span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">）时序数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">时序数据库又称为时间序列数据库，它是用来存储和管理时间序列数据的专业数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">库，具备写多读少、海量数据持续高并发写入、数据冷热分离等特点。此外，这类数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">可以基于时间区间进行数据聚合分析和灵活检索。它被广泛应用在物联网、金融、工业制</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">造、软硬件兼容系统等高频度、高密度、动态实时采集场景下。时序数据库的热门产品有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20"> </span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">InfluxDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">KDB+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Amazon Timestream</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">TimescaleDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。</span>
<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">5</span><span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">）列式存储</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">列式存储一般也指宽列式数据库，这类数据库一般采用列族数据模型存储数据。和关</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">系数据库类似，列式存储也由多行数据构成，每行数据包含多个列族，每个列族又会对应</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">多列。不同的行可以具有不同数量的列族。同一列族的数据存储在一起。简单来看这类数</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">据库和关系数据库差不多，但实际上在数据存储上存在很大的差别。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">传统的关系数据库中数据是按照行来组织的，多个列构成的一行数据在存储时会按照</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">特定的行格式进行扁平化组织，然后写入文件中。当检索一行中的某列数据时需要读取整</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">行数据，再返回该列的数据。这对每次总是使用整行中的多列信息的场景来说非常高效。</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p019_img01.jpg)

---

## Page 20

<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">而在一些统计分析场景中，往往需要对海量数据中的某列或者某几列数据进行频繁的读取</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">和聚合。在这样的模式下，关系数据库的处理方式就变得不太高效了，而列式存储的优势</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">可以得到充分发挥。列式存储中的数据按照列组织后，同一类型的稀疏数据有时候可以采</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">用一些手段（例如位图编码）进行压缩以节约空间。列式存储主要适用于</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 场景，典型</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的产品有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">ClickHouse</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Cassandra</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">BigTable</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。</span>
<span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">6</span><span style="font-family:'FZKTK--GBK1-0';font-size:10.00pt;color:#231f20">）多模数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">多模数据库是下一代新型数据库，它与传统的支持单一数据模型的数据库不同。这类</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据库是在一套系统内支持多种不同数据模型的数据库。这些数据模型可包括传统的关系</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">模型、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据模型（文档模型、键值模型、图模型等）。多模数据库的主要特性之一是</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">通常支持一种或者多种查询语言，可以以灵活的方式访问多种不同的数据模型，甚至跨越</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">模型进行</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">join</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等操作。这类数据库使得对数据的存储、组织、查询变得比以往的数据库更</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">加灵活和便捷。目前有些关系数据库和</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库正在通过扩展对其他数据模型的支持</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">来转变为多模数据库。多模数据库的典型代表有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">MongoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（支持文档、图等模型）、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Oracle</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（支持关系表、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">XML</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、文档等模型）、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OrientDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（支持文档、图、键值等模型）、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">ArangoDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（支</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">持文档、图、键值等模型）等。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">目前，不同的多模数据库通过不同的底层架构来实现多模型数据的管理。多模数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的总体实现有两种方式：一种方式是在原生存储引擎存储主数据模型，然后扩展实现其他</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">数据模型，例如某些产品用文档来实现主存储，然后使用文档之间的关系实现图模型；另</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">一种方式是在所有数据模型上层增加一个中间层来集成所有操作，这样每种数据模型都需</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">要有对应的处理模块。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">结合上述对各种</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库的介绍，将它们对应存储的数据结构进行汇总，如图</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1-2</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">所示。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">虽然</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库克服了关系数据库存储的缺陷，但它无法完全代替关系数据库。在</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库出现后的一段时间内，互联网软件的构建基本上都是结合二者来提供服务</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的，在不同的场景下选择不同的数据库进行数据存储。虽然这样的合作方式很好，但是在</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">这样的模式下，一个用户可能会因为场景的不同而存储多份相同的数据到不同的数据库中，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在用户量级和存储数据量很小的情况下没什么问题，一旦量级发生变化就会引发新的问题。</span>
<span style="font-family:'-';font-size:10.00pt;color:#231f20">3. NewSQL 数据库</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">随着存储数据量的不断增加，资源的浪费和成本的上升不容忽视，工业界和学术界都</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在寻找更好的解决方案。直到</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">2010</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 年左右，诞生了</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库（也称为分布式数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">库）。它的出发点是结合关系数据库的事务一致性，又具备</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库的扩展性及访问</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">性能。这无疑给系统的设计及实现带来了更大的挑战，</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库不仅要考虑单机环</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">境下高效存储的问题，还需要考虑多机情况下数据复制、一致性、容灾、分布式事务等问</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">题。目前，</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库的典型代表有</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">TiDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OceanBase</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">CockroachDB</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 等。</span>

<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">5</span>

<span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">第1 章</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">存储引擎概述</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p020_img01.jpg)

---

## Page 21

<span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">6</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">深入浅出存储引擎</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">文档</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">文档数据采用继承类树结构的</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">JSON</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 或者</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">XML</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 对象存储</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">典型例子</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">MongoDB</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">使用场景</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">内容管理</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">爬虫</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">物流等</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">优点</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">可扩展性强</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">易用性好</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">键值数据通常按照键值对的方式进行存储</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">典型例子</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Redis</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">使用场景</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">电商购物车</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">信息流</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">社交关</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">系等</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">优点</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">可扩展性强</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">性能高</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">图数据中点存储数据实体</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">边存储实体之</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">间的关系</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">典型例子</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">Neo4j</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">使用场景</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">社交</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">推荐</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">数据挖掘等</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">优点</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">可以形象地表示实体之间的关系</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">，</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">可扩展性强</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">通常按照时间维度收集</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">统计一些数据和</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">事件</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">典型例子</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">InfluxDB</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">使用场景</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">物联网</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">服务监控</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">实时采样等</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">优点</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">非常适用于实时采集数据大量写的</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">场景</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">写性能较好</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">数据按照列族划分存储</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">，</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">支持非常灵活的</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">结构定义</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">典型例子</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">HBase</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">使用场景</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">大数据分析</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">OLAP</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20"> 等</span>
<span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">优点</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">：</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">可扩展性强</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">、</span><span style="font-family:'FZS3K--GBK1-0';font-size:8.00pt;color:#231f20">性能较好</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">文档数据库</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">键值</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">键值数据库</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">图</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">图数据库</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">时序数据</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">Time</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">Value</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">Time Series ID</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">时序数据库</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">列族</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">列式存储</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1-2</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据存储类型</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">从数据库的发展历程可以看到，每种类型数据库的产生都是为了解决特定场景下的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">问题，这也是计算机软件发展的一个永恒不变的规律。</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库、</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数据库不同维度的对比见表</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1-2</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">。</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p021_img01.jpg)



![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p021_img02.jpg)

---

## Page 22

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">维度</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">SQL</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20"> 数据库</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20"> 数据库</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20"> 数据库</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">关系属性</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">强，关系数据库主要遵循</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">关系模型</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">ACID</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20"> 事务属性</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持，</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">ACID</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 事务属性是其</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">应用的基础</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">绝大部分不支持，这类系</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">统提供</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">CAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 支持</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持，</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">ACID</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 事务属性原</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">生支持</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">SQL</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">SQL</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">绝大部分不支持</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">SQL</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">对旧</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 有适当的支持，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">甚至增强了功能</span>
<span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">OLTP</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">，关系数据库效</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">率一般</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">部分支持，但不是最适合</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">的</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 数据库的功</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">能，效率很高</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">扩展性</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持垂直扩展</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持水平扩展</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">支持水平扩展</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">查询处理</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">可以轻松地处理简单的查</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">询，当查询的性质变得复杂</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">时就会失败</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">如果以组件的类型是关系数据库还是非关系数据库，并结合服务的场景是</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 还是</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 来对业界的各种存储组件进行划分的话，可以得到图</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1-3</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 所示的结果。关系数据库中</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">既有为</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLTP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 设计的，也有为</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">OLAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 设计的，同时还有新兴发展起来兼容二者的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HTAP</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 数</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">据库。这些系统都有各自适用的业务场景，在实际方案选型时需要结合具体场景灵活选择</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">合适的数据库。</span>

<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">7</span>

<span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">第1 章</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">存储引擎概述</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>

<span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20">表</span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">1-2</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">SQL</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据库、</span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">NoSQL</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据库、</span><span style="font-family:'ArialMT';font-size:9.00pt;color:#231f20">NewSQL</span><span style="font-family:'FZHTK--GBK1-0';font-size:9.00pt;color:#231f20"> 数据库对比</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">弱，不遵循关系模型。设</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">计理念和</span><span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 数据库完全</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">不同</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">强，上层支持关系模型</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">在处理复杂的查询时比</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:8.00pt;color:#231f20">SQL</span><span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20"> 更好</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">在处理复杂查询和小型</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:8.00pt;color:#231f20">查询时效率很高</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1-3</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 存储组件的分类</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p022_img01.jpg)



![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p022_img02.jpg)

---

## Page 23

<span style="font-family:'ArialMT';font-size:10.00pt;color:#231f20">8</span><span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span>
<span style="font-family:'KozMinPro-Regular';font-size:9.00pt;color:#231f20"> </span><span style="font-family:'FZLTXHJW--GB1-0';font-size:9.00pt;color:#231f20">深入浅出存储引擎</span>
<span style="font-family:'-';font-size:11.50pt;color:#231f20">1.1.3 内存型存储组件与磁盘型存储组件</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">在计算机发展的几十年里，计算机的整体结构依然没有太大的变化，计算机中充当存</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">储介质的主要有内存、磁盘两类。内存的访问速度要比磁盘快几个量级，但是内存的容量</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">比磁盘要小很多。一般对磁盘进行访问时，首先会从磁盘文件加载数据到内存中，然后响</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">应用户。内存和磁盘的各维度对比如图</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">1-4</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 所示。</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">维度</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">内存</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#ffffff">磁盘（硬盘）</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">外形</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">成本</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">高</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">低</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">大</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">慢（顺序</span><span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">I/O&gt;</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20"> 随机</span><span style="font-family:'ArialMT';font-size:8.00pt;color:#231f20">I/O</span><span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">）</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">非易失性存储，断电不丢数据</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">容量（单机）</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">小</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">访问速度</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">快</span>

<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">存储特性</span>
<span style="font-family:'FZHTK--GBK1-0';font-size:8.00pt;color:#231f20">易失性存储，断电丢数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20">图</span><span style="font-family:'TimesNewRomanPSMT';font-size:9.00pt;color:#231f20">1-4</span><span style="font-family:'FZSSK--GBK1-0';font-size:9.00pt;color:#231f20"> 内存和磁盘对比</span>

<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">结合前面的介绍，在大部分系统设计时，通常会选择一种主要存储介质来存储数据，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">另一种存储介质则作为辅助使用。在目前的各种存储组件中，根据每个存储组件存储数据</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">的主要介质，可将其分为两类：内存型组件和磁盘型组件。</span>
<span style="font-family:'-';font-size:10.00pt;color:#231f20">1. 内存型组件</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">内存型组件的典型特点是读</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">/</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 写性能高、访问速度快。其缺点在于，保存的数据量受限</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">于当前机器的内存容量，并且当机器宕机或发生突发情况断电后，保存在内存中的数据会</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">全部丢失。例如，</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 是大家较熟悉也较常用的一个内存型存储组件。</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 主要采用内</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">存存储数据，而磁盘则用于做辅助的持久化存储。</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">RabbitMQ</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 也主要采用内存存储消息，同</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">时支持将消息持久化到磁盘。</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">对采用内存存储数据的方案而言，难点之一在于如何在不降低访问效率的情况下，充</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">分利用有限的内存空间来存储尽可能多的数据。这个过程少不了对数据结构的选型、优化，</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">以及对数据过期、数据淘汰等方案的选择。同时，绝大多数的内存型组件在保证单机功能</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">完备的情况下，都会优先考虑对存储的数据进行分片，并构建集群系统对外提供服务，以</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">解决单机内存容量这一限制。另一个难点在于，内存型组件如何保证在机器发生故障的情</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">况下，数据尽可能少丢失。针对这类问题，业界经典的解决方案是快照</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">+</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20"> 广泛意义的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">WAL</span>

![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p023_img01.jpg)



![](./深入浅出存储引擎 (文小飞) (Z-Library)_assets/images/p023_img02.jpg)

---

## Page 24

<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Write Ahead Log</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">）日志，其典型代表有很多，比如</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">Redis</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">。</span>
<span style="font-family:'-';font-size:10.00pt;color:#231f20">2. 磁盘型组件</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">和内存型组件不同，磁盘型组件的特点是单机磁盘存储的数据量非常大，要远大于</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">内存型组件。同时，在机器宕机或者发生突发情况断电的情况下不会出现数据丢失（排除</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">极端情况）。然而，磁盘型组件，尤其是典型的</span><span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt;color:#231f20">HDD</span><span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">（机械磁盘），由于先天性的磁盘结</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">构，其访问数据的速度比内存慢得多。同时，在相同的磁盘结构下，对磁盘的访问方式决</span>
<span style="font-family:'FZSSK--GBK1-0';font-size:10.00pt;color:#231f20">定了磁盘访问的耗时。磁盘顺序访问要远远快于磁盘随机访问。磁盘型组件有关系数据库、</span>
<span style="font-family:'TimesNewRomanPSMT';font-size:10.00pt