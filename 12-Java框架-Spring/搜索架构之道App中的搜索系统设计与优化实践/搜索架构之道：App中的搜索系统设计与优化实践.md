## Page 1

移动开发
THEWAYOF
SEARCH
ARCHITECTURE
Design and Optimization Practices for

搜索架构之道
App中的搜索系统设计与优化实践
刘俊启著  Q

百度10余年搜索研发工程师、我国App研发先行者撰写，
多位百度前高管，技术专家鼎力推荐
站在搜索业务角度分享亿级用户App搜索系统构建和优化之道，覆盖App从竭生
到成为超圾App技术架构层面面临的所有核心挑战及其解决方案

机基工业出版社

<!-- 图源: ./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p001_scan.jpg -->

---

## Page 2

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">版权信息</span>
<span style="font-family:'Unnamed-T3';font-size:8.75pt;color:#000000">COPYRIGHT</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">书名：搜索架构之道：App中的搜索系统设计与优化实践</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作者：刘俊启</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出版社：机械工业出版社</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">出版时间：2024年11月</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">ISBN：9787111764595</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">字数：232千字</span>

---

## Page 3

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">前言</span>

<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">为什么要写这本书</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在当今数字化时代，App已经成为人们日常生活中不可或缺的一部</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">分。而一个优秀的App，往往需要一个高效、稳定、安全的技术架构</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作为支撑，同时搜索功能也成为大多数App的标配。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">2023年年底，我从百度公司离职。算起来，我在百度工作了整整13</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">年。在这
13年里，我从一名研发工程师晋升为资深研发工程师；技术</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">水平从只了解一门编程语言到熟悉整个业务和生态；工作方式从独自</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">写代码转变为多人跨地域、跨部门协同；思维方式也从接受需求、编</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">写功能点，逐渐转变为主动思考问题、解决问题并推动事情落地。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">非常幸运的是，在百度的这
13年里，我参与并负责了多个与搜索有关</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的App从无到有的构建过程，也见证了百度App用户规模从万级到亿</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">级、客户研发团队从几人到数百人的发展和变化。这个过程既充满挑</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">战，也蕴含机遇，让我能够将对技术架构的理解付诸实践，从而使我</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">更加深入地了解App中的技术架构是如何支持App搜索业务的，以及</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">应该如何对App的技术架构进行优化。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">故我撰写本书有两个目标：第一个目标是梳理自己在参与及负责的多</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个App架构优化工作中遇到的挑战、思考过程以及实际成效，作为对</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">自己在百度工作
13年的总结；第二个目标是希望把这些经验分享出</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">来，与所有相关人员进行思想上的交流，因为在App从无到有以及到</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">拥有上亿级用户规模的过程中，不同的App所采用的技术路径是相似</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的。</span>
<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">读者对象</span>

---

## Page 4

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这是一本专注于搜索业务全流程技术实现的书籍，重点介绍了App中</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">搜索系统在不同阶段的架构设计及实现。无论你是初入行业的新手还</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">是经验丰富的技术专家，或是对技术有浓厚兴趣的读者，本书都将为</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">你提供有价值的知识和实用的指导。本书特别适合以下人员阅读。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">从事移动搜索业务的人员：包括测试人员、产品经理、前端工程师</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">及后端工程师等。本书重点介绍了与搜索App技术架构相关的知识，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">可以帮助这些人员更好地理解客户端中的技术架构和设计应用架构。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">移动应用开发者：对于移动应用开发者，本书提供深入的技术架构</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">设计和实践经验，可以帮助开发者构建更加有效、可用的App技术架</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">构。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">技术经理和团队领导：本书包含在解决问题时对技术选择的思考，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这部分内容可以帮助负责管理技术团队和项目的人员做出明智的决</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">策。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">计算机相关专业的学生和学者：本书可作为计算机科学、软件工程</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等相关专业的学生和研究人员学习搜索App开发、App优化的参考书</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">籍，可以帮助他们深入了解App技术架构的实际应用和研究方向。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">对搜索App或App中搜索功能的实现感兴趣的人员：本书能够帮助</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">这部分人员从更全面的视角了解搜索系统架构的原理、重要性和演化</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">趋势。</span>
<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">本书特色</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书是对我从无到有构建多个App技术架构和重构优化超级App技</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">术架构的实践经验的高度总结，在介绍不同架构如何实现的同时，还</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">介绍了当时的背景及实践的流程，目的是让广大读者用更少的时间，</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">在真实的业务场景中学习并吸收我
13年工作经验的精华。</span>

---

## Page 5

<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书中不仅有具体的技术实现细节，还有我对具体问题的深度思</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">考，这些思考不仅可以帮助读者知道怎么做，还能知道为什么这么</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">做，进而让读者实现举一反三，轻松应对所有同类问题。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书兼顾广度和深度，既覆盖了搜索App设计的全部关键环节，又</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">直指问题本质。</span>
<span style="font-family:'SegoeUISymbol';font-size:15.00pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">虽然本书是以搜索App为主进行讲解的，但是其中的方法和思想可</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">以沿用到其他App产品的设计中，而且可以覆盖技术架构师、开发人</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">员、管理人员和项目经理等多个人群。</span>
<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">如何阅读本书</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">本书共三篇。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">基础篇（第1～3章），简要介绍了搜索客户端的发展与价值、基础技</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">术及基础服务，目的是帮助读者了解必备基础知识，为学习本书后续</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">内容做铺垫。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">高级篇（第4～12章），根据搜索全流程业务的需要，围绕输入并行</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">化、可扩展网页能力、场景容器化、安全策略制定、核心指标持续优</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">化、网络统一管理、移动端AI预测、App可变体发布及支持质效提升</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">等
9个维度，着重讲解了App技术架构的实现与思考。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">个人成长篇（第13章），通过融入团队、有效交付及持续优化
3个维</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">度，介绍如何与团队、业务及技术融合，构建个人的架构优化之路。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">如果你对搜索的业务流程、基础技术及服务端依赖比较了解，可以直</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">接从高级篇开始阅读。但如果你是初学者，请一定从第
1章开始学</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">习。</span>
<span style="font-family:'Unnamed-T3';font-size:16.88pt;color:#000000">致谢</span>

---

## Page 6

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">首先感谢百度这个平台。百度有良好的技术及实践氛围，这让我可以</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">长期专注于研发工作并积累与App架构设计相关的知识，这也是本书</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">的内容能做到丰富和深入的基础。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">感谢刘艳红、Proteas、姬路涛、刘爽、谢柏渊、马光潇对本书进行</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">审阅，他们提出了许多宝贵的建议。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">感谢《Java加密与解密的艺术》的作者梁栋的引荐，在他的努力下才</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">促成了这本书的合作与出版。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">最后，感谢妻子和女儿在我写书期间给予的理解与支持。写书占用了</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">一些亲子时光。记得那时，女儿经常跑过来找我一起玩，看到我坐在</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">计算机前，她总是懂事地说一句：“爸爸在写书呢！”然后就跑开</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">了。</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">谨以本书献给我最亲爱的家人、好友和我在百度工作的
13年中一起合</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">作过的伙伴们！</span>

---

## Page 7

<span style="font-family:'Unnamed-T3';font-size:21.25pt;color:#000000">基础篇</span>

<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">■</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第1章</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">搜索客户端的发展与价值</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">■</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第2章</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">搜索客户端基础技术</span>
<span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">■</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">第3章</span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:15.00pt;color:#000000">搜索客户端基础服务</span>

---

## Page 8

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第1章</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">搜索客户端的发展与价值</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本章首先介绍我在百度参与或负责的
7次与搜索客户端架构优化有关的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">工作、所解决的问题以及过程中的思考，并从搜索客户端的架构变化</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">来看搜索客户端的价值；然后，从iOS和Android这两个移动操作系统</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提供的搜索能力来看搜索客户端的价值；最后，通过
6款生活中常用的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App实现的搜索功能来看搜索客户端的价值。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">从我在百度的工作经历看搜索客户端架构演进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从零构建搜索客户端App</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时间回到2011年，我们团队启动了一款新产品的研发——在iOS平台上</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现一个搜索App，名为“百度搜索”。很荣幸，我作为研发负责人</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">参与了这个App从无到有的构建过程。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这是我第一次站在实现者的角度来了解搜索业务。产品经理给“百度</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索”的定位为“有乐趣的、轻量级的搜索客户端”，那时我对搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">业务的理解还不够透彻，仅限于技术实现的水平。听到产品经理给出</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的搜索客户端的定位时，还在想搜索服务用浏览器就可以满足，为什</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">么还要再做一个搜索客户端？十几年过去了，这个问题的答案换过好</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多次，每隔一段时间我都有新的理解。在这个项目中，我学到的基本</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">知识到现在还很受用，一些知识点还在影响着我的工作方法及标准。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">百度搜索初版为用户提供了文本和语音两种输入方式，用户点击搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">框或语音按钮后，便可以使用文本或语音输入想要搜索的内容（关于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入的过程，在第
4章会有详细介绍）。</span>

---

## Page 9

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户输入完成后向搜索服务器请求搜索结果，当时的搜索服务器以网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页格式返回搜索结果（结果页），点击结果页中的结果所进入的页面</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（落地页）也是网页格式。在技术实现上，我们使用的是iOS系统提供</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的浏览内核</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">在本书中，浏览内核代指网页浏览内核，比如系统提供的API</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">UIWebView、WKWebView、WebView或自定义的网页浏览能力。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，支持网页的展现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">及交互。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">尽管通过浏览内核就可以满足用户的搜索浏览需求，但是为了提供页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">面加载切换控制功能，我们在百度搜索视图的底部增加了工具条，以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现最基础的页面浏览控制，如前进、后退、刷新等，这样一个基本</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的搜索App就实现了。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">整个App使用原生（Native）方式实现了主体框架，并通过内置的浏</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">览内核加载网页，实现了搜索客户端的基本功能。初版的搜索客户端</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在表面上看和浏览器有些相似，如图1-1所示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">注意</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Native</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App，简写为NA，即原生应用程序，是某一个移动平台（比如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">iOS或Android）所特有的，使用相应平台支持的开发工具和语言（比</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如iOS平台支持Xcode、Objective-C、Swift）开发的App。同一个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App，需要开发者针对不同的系统平台开发不同的版本。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p009_img01.png)

---

## Page 10

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">初版的搜索客户端与浏览器样式对比</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">此时的百度搜索主要使用系统原生应用程序接口（API）开发，可定制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">性不强。在这个阶段，研发团队和产品本身都处于成长期，架构设计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的重点是构建产品的基础功能。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Ding：优化移动端搜索的高频搜索需求</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在2011年，移动设备中各种垂类App还没有像现在这么普及，搜索天</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">气、股票等信息于部分用户而言属于高频需求。为了满足这些高频需</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">求并将对应信息更好地呈现给用户，产品侧提出的方案为：在用户发</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">起搜索时，为用户展现两类结果，一类为原搜索结果页的数据，使用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览内核加载；一类为自定义的数据，在App内自行解析渲染。我们</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">把实现这种自定义数据的“卡片”称为Ding。Ding可定制数据内容和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索关键字。Ding在结果页中的展现如图1-2所示。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p010_img01.jpg)

---

## Page 11

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Ding在结果页中的展现</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户可以把Ding添加到首页，以便打开App时在首页就可以直接看到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Ding的卡片。Ding支持自动刷新及手动刷新，点击它还可以看到对应</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的详情页面。对于时效性较高且高频的搜索需求，通过Ding来获取，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可明显降低成本。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在这个阶段不再局限于满足用户的搜索需求，端与云的协同实现了以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索或Ding的方式获取、展现信息以及与用户进行交互。这个阶段架</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构设计的重点在于搜索闭环能力的建设，而Ding仅是其中一种实现方</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索业务的本质是帮助用户找到所需信息，Ding满足了用户的一些长</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">尾或高频需求，有了Ding，用户不再需要频繁搜索，就可以直接看到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">高频需求对应的最新信息。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索+浏览双框架：优化移动端搜索过程的体验</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p011_img01.jpg)

---

## Page 12

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索业务有一个典型的特征，就是用户搜索一个信息后，会频繁地在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页和落地页之间切换，直到找到想要的答案。若是没有找到，那</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">么用户就会一直持续这个过程，或者重新描述需求并再次发起搜索，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">然后重复上述过程。这就意味着结果页需要多次加载，因为浏览内核</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的缓存能力有限，所以存在重新加载结果页的情况。而页面重新加载</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时用户需要等待，特别是在移动网络环境下，页面加载慢且出错的概</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">率偏高。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为了解决这个问题，我们将结果页和落地页拆分开，结果页使用原框</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">架——搜索框架进行加载，落地页使用新框架进行加载，这个新框架称</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为浏览框架。浏览框架没有搜索能力，但是可以快速关闭并回到搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">框架中，以便用户继续浏览结果页。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于这样的设计，用户在落地页浏览内容时，如果想再查看其他结</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">果，就可以快速切换到结果页。这个过程只需要进行两个框架的切</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">换，因此实现了近零等待，提升了满足搜索需求的效率。搜索和浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">双框架共存的产品形态如图1-3所示。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p012_img01.jpg)

---

## Page 13

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索和浏览双框架共存</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因为搜索和浏览两个框架是相互独立的，所以技术上可以实现对落地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页的预渲染，如果预渲染的准确率比较高，就可以实现点击后立即展</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">现落地页。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索结果NA化：优化移动端搜索结果浏览体验</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页的内容格式是基于网页的，页面的浏览及交互依赖于浏览内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">核。因为Web生态的开放性，所以在用户搜索和浏览过程中，会有很</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多站点参与其中，这就要求结果页的内容具有较高的适用性和扩展</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">性，但结果页的实现受限于浏览内核，导致自有搜索客户端的优势不</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">明显。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于实现成本、可控性及影响面的考虑，对于股票和外卖这类有明确</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方向的需求（对应特色搜索关键字），我们在百度搜索中进行创新尝</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">试，使用NA自定义内容的扩展方式来增强部分搜索结果的展现，我们</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">称这种扩展方式为搜索结果NA化。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">股票NA化案例：使用双层视图的方式扩展，底层是通过搜索框架加载</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的结果页，上层是通过股票NA的容器加载的股票内容页，两个视图之</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">间可以切换。首次对结果页进行加载时，可以自动调用端能力（客户</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端扩展的能力，在网页中可调用）展现股票NA的视图，如图1-4所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span>

---

## Page 14

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">股票NA化</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">外卖NA化案例：使用同层视图的方式定制。视图的上半部分为外卖</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">NA视图，下半部分为浏览内核。NA视图内部可上下滑动，当滑动到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">底部后，框架根据用户的操作把当前活动的视图切换为浏览内核；反</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">之，当前视图为结果页并向上滑动时，可以再切回外卖NA视图，如图</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1-5所示。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p014_img01.jpg)

---

## Page 15

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">外卖NA化</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">NA化的结果页较传统的网页格式结果页，在性能和用户体验等方面均</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">有明显优势。对于用户来说，视觉效果和流畅度的提升对整体的浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">体验产生了正向影响，这带来的直接变化就是用户的使用时长提升，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">让用户更有意愿浏览更多的内容，找到所需。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这一阶段的整体的技术方案是在原有结果页框架的基础上进行扩展，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在客户端实现网页内容+自定义内容的混合渲染，并通过端与云的协同</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现结果页的差异化定制。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索异步化：优化搜索核心指标</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">NA化的搜索结果页在通用性及扩展性方面存在不足。搜索业务的历史</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">积累较多，从面向浏览器服务进化为面向NA化服务，不是做几个NA</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化的场景这么简单，还需要考虑浏览器的生态。NA化的工作告一个段</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p015_img01.jpg)

---

## Page 16

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落后，要做的是优化用户在结果页上的体验，目标是优化页面的加载</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时长。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">仔细研究搜索结果页会发现，用户在搜索不同的关键字时，页面中总</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会有一些资源是相同的，这些相同的资源与结果页的架构及布局相</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">关，属于基础资源，不受具体的关键字影响。也就是说，如果提前加</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">载这些基础资源，那么在用户发起搜索时，仅需加载与关键字相关的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">资源，这样就可以减少页面加载时间，实现优化结果页加载时长的目</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">标。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">团队的前端人员开发了一套异步搜索框架，提前对结果页的基础资源</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进行加载、解析、渲染。当有搜索请求时，由异步搜索框架实现对关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">键字相关资源的加载。但是，应用这套框架的前提是客户端需要提前</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">创建一个浏览内核，预先加载这个异步搜索框架，当有搜索需求时，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">调用异步框架的接口，发起异步搜索。异步搜索框架有着不同的状</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">态，也会出现异常的情况，当异步框架不可用的时候，客户端则使用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">原加载方案进行搜索。异步搜索流程如图1-6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">异步搜索流程</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这时候的客户端的搜索框架依然采用单浏览内核的方式承载内容（单</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览内核管理框架），在App启动后先加载异步搜索框架。但单浏览</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p016_img01.jpg)

---

## Page 17

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内核架构有一个局限，如图1-7所示，异步搜索框架或其他页面只能加</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">载一个。当进入落地页时，浏览内核中加载的是落地页，而不是异步</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索框架，这时用户再次发起搜索就只能采用同步的方式。由此可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">见，异步搜索的覆盖率没有达到预期。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">异步搜索框架被落地页覆盖</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这个阶段的端与云的协同优化，不仅关注功能的构建，还开始兼顾核</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">心指标的优化。关于指标优化的更多内容，将在第
8章和第
9章详细介</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多容器管理：突破单浏览内核的限制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">解决异步搜索覆盖率问题的理想方案，是在页面加载落地页时，创建</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一个新的浏览内核来加载异步框架。但是这种方案实现成本较高。团</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">队从2018年开始推进搜索NA化，将自有落地页的内容（如视频、地图</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">等）在百度App中改用NA方式实现。然而，现有的单浏览器内核架构</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">无法实现多种不同类型内容的统一接入和管理，这就导致技术实现成</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本过高且不能保证搜索和浏览体验的统一，需要新的技术架构来支</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">持。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">针对上述问题和技术目标，我们实现了多容器管理框架：将线上单浏</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">览内核架构升级为可扩展的多容器共存的架构。多容器管理框架由框</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p017_img01.jpg)

---

## Page 18

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">架层确定容器化的标准，统一管理容器的生命周期和事件，并根据业</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务、性能等目标进行合理调度，支持不同容器接入。我们还构建了容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">器内存/磁盘缓存管理机制，以及预创建、预加载、预渲染等优化策</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">略，在效果体验及资源消耗方面实现了较好的平衡。多容器管理框架</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">核心能力如图1-8所示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">将原网页浏览相关功能升级为网页容器，在多容器管理框架中，可创</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">建多个网页容器。当一个网页容器被使用后，可再创建一个新网页容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">器来加载异步搜索框架以支持搜索。对比原单浏览内核管理框架，多</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容器管理框架在异步搜索方面的转换率有明显的提升。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-8</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多容器管理框架核心能力</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在这个阶段，多容器管理框架像一个简版的操作系统，支持不同类型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的容器接入，并统一管理容器的生命周期、状态切换、事件分发、通</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">信等，多容器管理机制作为基础能力支持搜索业务的全流程优化。详</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">细的内容会在第
6章介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">变体发布：多App复用搜索能力</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p018_img01.jpg)

---

## Page 19

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多容器管理框架上线之后，不同类型的内容可通过较低成本接入，团</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">队的并行研发效率得到了改善。每个容器在各自的模块内独立迭代，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相互影响较小。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时间到了2020年，各大厂均在构建自家的App矩阵，以超级App为中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">心孵化出一批矩阵App，如京东极速版、头条极速版、快手极速版</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">等。这些矩阵App不仅具备主线App的核心能力，甚至还会有一些定制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化功能。百度App也需要孵化矩阵App，但矩阵App复用百度App功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模块的成本较高，主要问题在于，矩阵App既需要基于百度App的能力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进行裁剪，或者进行差异化定制，又需要周期性地从主线App同步最</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新的能力。和传统的模块复用方式不同，这种同步主线App的操作是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App级的复用，这样的能力需要技术架构层提供支持。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我们当时提出了变体发布的思路，即一个App可以通过工程化的配置</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现功能快速裁剪和低影响面的差异化定制，主要实现方式包括技术</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分层、容器化、插件化、动态化、接口与实现分离等，最终实现业务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模块低成本剥离和定制。通过这个思路，矩阵App单次同步主线App功</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能的耗时下降了70%以上。支持变体发布的App架构如图1-9所示。变</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">体发布相关的内容将在第
11章详细介绍。</span>

---

## Page 20

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-9</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可变体发布的App架构</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.1.8</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">小结</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在2013年，我有幸以架构师的身份参与了百度手机浏览器iOS版的技术</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">研发工作，因为我之前有过手百（手机百度或百度App的简称）的研</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">发经验，所以有同事问我：“俊启，咱们已经有了手百，做浏览器还</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">有意义吗？”</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我当时给的答复是“手百作为搜索产品的客户端应该更关注搜索全流</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程的体验，而浏览器以网页浏览的能力作为根本，应该更关注浏览过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程的体验”。貌似聊到搜索，浏览器是永远绕不开的话题。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p020_img01.jpg)

---

## Page 21

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在
20多年前的个人计算机时代，搜索引擎通常基于Web生态构建，用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户不需要专门的搜索类客户端产品，使用浏览器即可免费享受搜索服</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务——打开浏览器，进入搜索引擎的主页，输入关键字，搜索引擎对关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">键字进行检索，并以网页的形式返回结果信息。Web生态是一个开放</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的生态，基于万维网联盟（W3C）标准构建，每个站点再按照其标准</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">建立并生产内容，搜索引擎则基于这些标准进行信息的抓取、分析、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分类和建库，并为用户提供信息检索服务。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览器是基于W3C标准实现的网页加载、浏览及交互的能力，操作系</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">统一般都会内置浏览器，这相当于用户可以零成本使用搜索服务。在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当时的网络环境下，搜索引擎基于Web生态构建，并通过浏览器为用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户提供免费的搜索服务，搜索类客户端产品的优势并不明显。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以W3C标准为基础，内容的生产由站点提供，内容的筛选由搜索引擎</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提供，内容的浏览由浏览器提供，一切看起来这么自然，这完全得益</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">于整个生态是公开、开放、共享的。浏览器可以基于标准实现互联网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中网页的加载及浏览能力，在这个基础之上，任何一个公开站点提供</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的服务都是以网页的形式在浏览器产品中展示的，搜索服务也不例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">外。在浏览器中进行搜索抓取成为一个重要的需求，2006年左右，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">IE</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">7.0和火狐浏览器开始内置搜索引擎服务，用户在浏览器的地址栏中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入关键字就可以直接使用搜索服务。浏览器通常会内置多个搜索引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">擎，并向用户提供多个搜索引擎选项，让用户自行选择。图1-10为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Safari浏览器提供的选择默认搜索引擎的界面。而在搜索客户端中，默</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">认仅有自家的搜索引擎服务。</span>

---

## Page 22

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-10</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Safari浏览器中的选择默认搜索引擎界面</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索引擎的核心能力是信息检索，当用户使用浏览器进行搜索时，搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索引擎会通过算法和策略检索出更符合用户需求的结果。用户点击搜</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p022_img01.jpg)

---

## Page 23

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索结果中的某个条目从而打开一个第三方的落地页，这个过程的浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">体验与浏览器的产品设计有关，不受搜索引擎控制。落地页中的大部</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分结果是第三方页面，当用户发现所浏览的内容质量不好时，由于这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">些结果是由搜索引擎检索得到的，故基于页面打开的先后关系，部分</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户会认为是搜索引擎的问题。总的来说就是在浏览器中使用搜索引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">擎服务，在搜索及浏览体验方面是不可控的，搜索引擎对第三方的内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容质量也无法干预，只有搜索服务和自有内容是可控的，具体示例如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-1
1中灰色区域所示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而当搜索引擎的服务有了自建客户端的支持，搜索及浏览的体验就成</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">了可控的，此时第三方的内容质量问题也可以被识别并干预，这时相</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当于搜索全流程是可控的，如图1-1
2中的灰色区域所示。</span>

---

## Page 24

客户端  云端
影响面
自有内容落地页
可
控
搜索服务  结果页

可控性
不
可  浏览器  第三方内容落地页
控

搜索及浏览体验  内容质量

<!-- 图源: ./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p024_scan.jpg -->


<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图1-11</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览器中搜索体验的可控性示例</span>

---

## Page 25

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图1-12</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索客户端中搜索体验的可控性示例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">同时，因为有搜索客户端产品的支持，从输入关键字到展现结果页，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">再到打开落地页，每一步都可以定向优化，也就是说从用户打开客户</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端到满足搜索需求的整个过程均可进行定制和优化，这拉开了与其他</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索产品的差距。图1-1
3列出了搜索客户端对搜索业务研发的影响</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（部分内容在本节前面已有介绍），这些正向影响都得益于搜索客户</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端。不断地创新及优化端与云的协同，可以为用户提供更好的搜索体</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">验。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p025_img01.jpg)

---

## Page 26

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-13</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索客户端对搜索业务研发的影响</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">总的来说，当使用第三方浏览器进行搜索时，搜索引擎只能通过优化</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索的准确度和检索速度来提升搜索体验，并通过自建落地页来丰富</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索生态中的内容，无法控制搜索全流程中其他节点的体验。而在搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索客户端中，可以实现从用户输入关键字到搜索、浏览第三方或自建</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容页的全流程支持，从而实现端云一体化的搜索全流程优化。这将</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提升用户使用搜索服务的体验，增强用户使用搜索客户端的意愿。相</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">比于浏览器产品中的搜索服务，搜索客户端的优势在于能够提供更好</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的搜索全流程体验，从而提升用户的满意度和忠诚度。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">移动操作系统级的搜索能力支持</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在移动设备中，主流的操作系统为苹果生态的iOS系统和谷歌生态的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Android系统，这两个系统都提供了全局的搜索能力，主要针对应用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内的内容搜索，默认支持系统级应用中对内容的搜索，也支持对设备</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中安装的App中的内容进行搜索。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.2.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">iOS系统搜索能力</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p026_img01.jpg)

---

## Page 27

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">iOS系统从2009年（iOS3.0）开始内置全局搜索框（通常称为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Spotlight），支持对联系人、应用、短信、地图、文件、邮件等进行</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索。苹果公司在2015年的全球开发者大会（WWDC）上推出了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">iOS</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Search</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API，它重点实现对应用内的内容进行搜索的能力，分为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以下
3类子API。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">NSUserActivity</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">NSUserActivity</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API用来支持App向全局搜索框（Spotlight）中注册</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户之前看过的内容，以便用户在App中对这些内容进行搜索。如图1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">14所示，用户在京东App浏览过iPhone</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">15</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Pro相关的商品，在全局搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索框中搜索iPhone</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">15</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Pro时，浏览过的内容就会被展现，点击结果条</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">目则可进入京东App，同时App跳转至对应的页面供用户继续浏览。</span>

---

## Page 28

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-14</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">京东App使用NSUserActivity为用户提供全局搜索浏览历史的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">CoreSpotlight</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">CoreSpotlight</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API用来支持App向Spotlight中注册任意内容，使得这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">些在App中注册的内容在全局搜索框中就可以被搜索到，且这些内容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">是在本机建立的索引。CoreSpotlight</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API比较适合搜索App中的内容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">及用户特有的数据，比如记事本中的内容、社交App中的聊天记录</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">等。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p028_img01.jpg)

---

## Page 29

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Web</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Markup</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">调用NSUserActivity</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API和CoreSpotlight</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API生成的索引都有一个限</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">制——须先安装App后才能搜索到App中的内容。如果App中的内容大</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">部分都是网页形式，并且用户希望在没有安装App时也能搜索到这些</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容，怎么办？苹果为此准备了第三类子API——Web</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Markup</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">API。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">开发者调用该API，就表示允许苹果的爬虫抓取自己的网页内容，并且</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">声明了网页内容和应用的关联性，这样就有机会在未安装App的情况</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">下让网页内容出现在全局搜索框的搜索结果中。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.2.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Android系统搜索能力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Android系统也是从2009年（Android</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.6）开始支持全局搜索的，当</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时Android系统中的全局搜索不仅可以搜索网页上的内容，还可以搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索手机中的联系人、文件、短信和邮件等内容，为用户查找手机里的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">信息提供了快捷入口。具体通过以下两个功能实现。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Firebase</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Indexing</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Google在2013年开始提出将应用像网站一样编入索引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（Indexing</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">just</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">like</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">websites）的思路，相当于在网页中索引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App中的内容。这种思路可以增强Google的搜索能力。原来Google只</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可以检索网页内容，将来还可检索App中的内容。整体的解决方案称</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为Firebase</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Indexing。Firebase</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Indexing可以将某个App</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与Google搜索建立关系。当用户搜索到关联的内容时，如果用户在本</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">机安装了这个App，他们就可以启动这个App，并直接在这个App中跳</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">转到用户正在搜索的内容页。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Firebase</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Indexing不仅可以帮助这个App的用户在其设备上查找</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">公开内容，甚至还可以提供查询自动补全功能帮助用户更快速地找到</span>

---

## Page 30

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所需内容。如果用户还没有安装这个App，相关查询会在搜索结果中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">显示这个App的安装提示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从2022年开始，Google建议使用Android</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Links，它支持用户从</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索结果、网站和其他App中直接链接到某App中的特定内容。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">In</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Apps</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Google在2016年
8月推出了搜索本地App中的内容的功能——</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">In</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Apps，目的也是让用户可以通过系统提供的全局搜索框搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Android手机App内的内容。In</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Apps还支持查询联系人、短信等信</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">息，同时支持对Gmail、Spotify、LinkedIn、Facebook等App内容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进行检索。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.2.3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">小结</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在系统中，全局搜索功能为用户提供了快速查找信息的能力，并且该</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">功能有便捷的触发方式。系统实现的搜索能力与传统网页的搜索能力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">存在一些差异，系统提供的全局搜索功能主要用于解决App内的信息</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">孤岛问题，通过一系列API支持的可搜索内容，实现对预置App和第三</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方App内的信息整合。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而App中提供的搜索服务则主要基于搜索引擎，现阶段的搜索能力主</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要是检索网页和自有内容。由于网页格式是公开的，第三方网页内容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可以被抓取。若要检索第三方App中的内容，则需要在内容可抓取、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可展现和可交互这三个方面进行定制，缺一不可。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从可抓取的角度来看，小程序或自建内容生态对于传统网页搜索具有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重要意义，它们以另一种方式解决了App信息孤岛的问题。事实上，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">系统中的搜索也需要内容的支持，这也是iOS和Android系统开放搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相关API，鼓励不同App参与其中的主要原因。虽然两者都在进行信息</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">整合，但实现层级不同，提供的能力也有差异。</span>

---

## Page 31

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从可展现和可交互的角度来看，现阶段搜索业务中的内容格式主要是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页，其展现和交互依赖于浏览内核。若要实现差异化的内容定制，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">则需要客户端的支持。例如，小程序需要在运行时有框架的支持，而</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多容器管理框架实际上解决了不同类型内容的展现和交互扩展问题。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些非网页格式的内容并不像网页格式那样通过统一资源定位符</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（URL）直接在浏览内核中打开，而是采用自定义的指令格式。因</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">此，指令的解析需要单独定制，也需要客户端的支持。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">1.3</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">App中的搜索功能建设</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在移动设备中，由于很多内容提供方自建了App，且当用户有明确的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">场景需求时更倾向于使用对应的App搜索及浏览内容，这导致一些搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索流量被分走。针对这个问题，我认为作为搜索侧的高阶研发工程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">师，需要清楚搜索客户端在搜索生态中的定位，从而把握好技术方</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">向，通过端云协同的方式更好地支持搜索业务，实现搜索业务的差异</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">我们应该以更广阔的视角来看待搜索业务相关的技术，而不能局限于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索App本身。实际上，在许多App中搜索也是关键能力。为了深入了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">解不同App中的搜索能力，我挑选了
6个生活中常用的App进行使用，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">覆盖了与搜索全流程紧密相关的需求输入、结果页和落地页
3个场景。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">最终我得出的结论是：搜索是一个通用能力，不应仅限于传统的搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">引擎客户端。任何一个App都可能需要搜索功能，而不同的App实现的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索能力也有所不同。下面是对几种不同类型App的搜索功能进行介</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">京东App中的搜索功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">京东App一直都有搜索功能，只不过初期的版本只支持文本输入，主</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要用于搜索商品。后来的版本除支持文本输入外，还支持语音及图像</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入，不仅可以搜索商品、订单，还可以搜索垂类的内容，比如搜索</span>

---

## Page 32

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">京东超市、京东电器等。京东App中与搜索功能相关的
3个场景如图1-</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">15所示。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：支持文本、语音、图像等输入方式。在采用文本输入</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时，支持搜索建议（根据用户输入的文字推荐可能搜索的内容）功</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能，该功能不仅有相近字的联想，还有精细的分类推荐，比如输入</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“鼠标”时，有品牌、无线、游戏电竞专用等推荐。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：支持单列或双列的浏览方式，用户可以根据需要进行选</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">择，搜索的内容支持二次筛选及排序，不同的关键字对应的筛选条件</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">也有所不同。如果搜索的关键字为品牌，结果页中还有品牌京东自营</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">旗舰店的入口。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：落地页中有商品的详细介绍，包括商品的图片、视频、价</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">格、评价、好评率、关联推荐、购买方式（如加入购物车、直接购</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">买）等。落地页中没有搜索框，基本上使用同一类模板展现内容。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-15</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">京东App中不同场景的示例</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p032_img01.jpg)

---

## Page 33

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">微信App中的搜索功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">微信App从1.1版本开始支持搜索功能，包括对通讯录和会话列表的搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索，在后来的版本中，还支持对小程序、公众号、视频号等内容进行</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索。微信App中与搜索功能相关的
3个场景如图1-1
6所示。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-16</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">微信App中不同场景的示例</span>

<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：输入方式主要为文本、语音（语音转文本），在输入过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程中可直接显示输入结果。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：以列表的形式展现，在搜索页面中点击“取消”可以回到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">首页。微信支持对联系人、群聊、聊天记录、收藏、视频号、朋友</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">圈、公众号、小程序等进行搜索。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：微信的落地页展现形式与内容有关，不同内容的展现与非</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索时（正常浏览时）是一样的，只是在搜索状态下点击查看的落地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页在返回时会回到搜索结果页。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p033_img01.jpg)

---

## Page 34

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">快手App中的搜索功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">快手App的早期版本是没有搜索功能的，后来的版本支持通过文本、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">语音和拍照搜索商品。快手App中与搜索功能相关的
3个场景如图1-17</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所示。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：输入方式主要为文本、语音，图片搜索仅提供了搜索商</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">品的能力。在文本输入过程中会提供搜索建议，快手App中的搜索建</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">议不区分类型，点击搜索建议后就可以发起搜索。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：结果页包含综合、商品、直播、用户、视频、图片等多种</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容，还支持针对发布时间、作品时长、发布城市进行筛选。结果页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">展现形式为双列图片流，包括视频、商品、搜索推荐等多种结果在其</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中展示。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：落地页对于不同类型的内容，展现形式和交互形态均有不</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">同，视频落地页没有强化浏览历史，用户浏览的层级较浅，上下滑动</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">就可以切换内容。商品落地页重点展现商品详情及相关商品推荐。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p034_img01.jpg)

---

## Page 35

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-17</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">快手App中不同场景的示例</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">有道词典App中的搜索功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">有道词典App早期的版本只支持文本搜索，现在已经支持在首页、直</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">播、频道等分类中进行搜索，并且支持语音输入和拍图搜索。有道词</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">典App中与搜索功能相关的
3个场景如图1-1
8所示。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：支持文本、语音、拍图等输入方式。文本输入过程中有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索建议，拍图翻译支持涂抹功能。有道词典还支持词典笔输入，前</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提是绑定有道词典笔。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：在有道词典App中，大部分内容都在结果页中展现，包括</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">音标、发音，选择单词后还有不同词态翻译、网络释义、双语例句、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">专业解释、同义词、同根词、短语、百科等。对于拍照翻译，有道词</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">典App会通过光学字符识别（OCR）技术提取照片中文字，并展示翻</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">译内容。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：点击广告、百科、网络释义等都可以直接进入落地页，落</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">地页内容较简洁。</span>

---

## Page 36

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-18</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">有道词典App中不同场景的示例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">招商银行App中的搜索功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">招商银行App的搜索，更关注对具体业务、功能和服务介绍进行检</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索，目的是方便用户更好地使用招商银行App。招商银行App中与搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">功能相关的
3个场景如图1-1
9所示。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：以文本输入方式为主，输入过程中没有搜索建议，直接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">显示搜索结果。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：以列表的形式展现，点击取消可以返回首页。结果页中包</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">含App功能、产品、社区、生活等。在结果页底部提供“提问小招”</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">按钮，点击可进入客服页面。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：不同落地页提供的功能不同，页面组织形式也有所不同。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">招商银行App的部分落地页需要先登录，在打开此类落地页时要先判</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p036_img01.jpg)

---

## Page 37

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">断用户是否登录，若没有则执行登录步骤，登录成功后再打开该落地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-19</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">招商银行App中不同场景的示例</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">夸克浏览器App中的搜索功能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览器是最贴近搜索客户端的产品，夸克是为数不多的有自建搜索引</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">擎的浏览器产品。夸克浏览器App一直都有搜索功能，可使用自家的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索引擎或合作的搜索引擎。夸克浏览器App中与搜索功能相关的3个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">场景如图1-2
0所示。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p037_img01.jpg)

---

## Page 38

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图1-20</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">夸克浏览器App中不同场景的示例</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：支持文本、语音、图像等输入方式，在使用文本输入时</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会提供辅助网址输入功能，同时搜索建议与搜索框的布局方式与其他</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App不同。对于图像的输入，主要为拍照及照片选择两种形式。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：和传统的浏览器一样，结果页主要为网页格式，区别在于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">夸克浏览器App的搜索框在结果页的底部，这样的设计可以使用户使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用大屏设备时更容易操作。作为浏览器产品，夸克浏览器App支持更</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">换搜索引擎，包括夸克、百度、谷歌、必应等搜索引擎。结果页的内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容与用户选择的搜索引擎有关。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：夸克浏览器App的落地页主要为网页形式，用浏览内核承</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">载，同时实现了网页智能保护、智能预加载、智能无图等功能。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.3.7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">小结</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p038_img01.jpg)

---

## Page 39

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过对比多款App中的搜索功能我们发现，每个App的搜索流程基本都</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">是一样的，但在不同场景内的实现细节均有不同。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入：主要为文本、语音、图像及自定义的形式，有关输入过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程中客户端的并行化响应及技术实现的内容将在第
4章进行介绍。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页：结果页的内容主要是网页或自定义的格式，网页格式的内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容主要依赖于浏览内核，网页功能扩展的实现在第
5章进行介绍。搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果依赖于检索的能力，检索的内容与产品的定位有关，关于检索服</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务端相关的技术实现在第
3章进行介绍。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页：落地页的内容主要是网页或自定义的格式，根据内容不同</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可以定制不同的能力扩展，实现搜索浏览过程的需求闭环。与页面加</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">载、展现及交互过程优化相关的内容，在第6～9章均有介绍。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其他：例如登录、支付、理财、预加载、无图等功能是为了满足</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App的需要而构建的，这些功能要在客户端逐一实现，并支持App业务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">流程中的每个环节，甚至是可被搜索到及直接使用的。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为什么这些App都要构建搜索能力呢？答案只有一个——搜索是帮助用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户找到所需信息的最高效的方法。当App中的信息量达到一定的量级</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时，如果没有提供搜索功能，那么用户找到所需信息的成本将会非常</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">高，甚至可能无法实现。所以，随着App中信息量的不断增加，会有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">越来越多的App构建搜索能力，以实现帮助用户快速找到所需的目</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">标。在用户有明确需求的场景中，相比于为用户推荐内容的功能，搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索功能显然是更合适的。从技术的角度来看，构建搜索能力的关键是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">理解用户的需求，产品中要有内容可供搜索，搜索到的内容要能正常</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">展现及交互，这些过程都离不开客户端的支持。有了搜索客户端，还</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能够根据业务的需要来实现搜索能力的差异化定制。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">注意</span>

---

## Page 40

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本书的内容围绕搜索系统的架构设计展开，在后续的内容中“搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App”可以理解为包含搜索功能的App或以搜索为主要功能的App。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“搜索App”有客户端和服务端两个部分，“搜索客户端”更偏重搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索App中的客户端部分。</span>

---

## Page 41

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第2章</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">搜索客户端基础技术</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第
1章介绍了搜索客户端的技术架构的演进、操作系统和几个典型App</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中搜索能力的演进，同时也说明了搜索客户端的价值。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本章以文本输入搜索为例，介绍搜索客户端的完整工作流程，同时对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这个流程中的不同节点所使用的技术进行说明，掌握了这些技术，可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以更好地理解后续内容。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">搜索全流程的
3个核心场景</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上一章讲述了6个App与搜索功能相关的需求输入、结果页、落地页3</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">个场景。本节综合搜索客户端、系统搜索及各垂类App中的搜索相关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能力，对搜索全流程相关的
3个核心场景的能力进行梳理，如图2-1所</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">示。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需求输入场景：重点是让用户正确表达搜索需求。客户端通常包含</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">文本、语音、图像这
3种输入方式，我们还可以通过硬件设备对需求输</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">入进行扩展，硬件设备主要分为有线和无线两种类型。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页场景：重点是让用户准确找到想要的内容。结果页通常是网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页格式，在自有客户端的支持下，也可以使用非网页的格式或网页+非</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页的复合格式，甚至在客户端中可以对结果页进行业务扩展。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页场景：重点是让用户看到更多与结果相关的内容。落地页主</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要是网页格式，在自有客户端的支持下，也可使用非网页的格式或网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页+非网页的复合格式，同样，在客户端中也可以对落地页进行业务扩</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">展。</span>

---

## Page 42

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索业务核心场景的流程及关系</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">按照上述划分，搜索客户端应该支持对这
3个场景的搜索相关功能进行</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建及优化。在设计及实现客户端功能时，需要考虑上述
3个场景之间</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的关系，以实现场景间的切换。常见的搜索过程的场景切换如：需求</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入→结果页→落地页→结果页→落地页……直到用户找到所需或者</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输入新的搜索关键字。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从上面的描述来看，结果页和落地页场景所实现的能力很相近，为什</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">么还要把这两个页面单独拆分出来？主要有以下
5个原因。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从定义的角度来讲：结果页是搜索结果页面的一种统称，落地页是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">点击搜索结果进入的页面的统称。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从分发的角度来讲：由结果页分发落地页，通常在搜索结果页中，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户点击不同的结果查看会进入不同的落地页。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p042_img01.jpg)

---

## Page 43

<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从业务的角度来讲：搜索是一个业务，用户发起搜索后为用户展现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页。点击结果进入的某个落地页，也可能是一个业务，比如地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图、小说、视频和邮箱等，一些客户端是先有了具体业务，再有的搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索能力。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从扩展的角度来讲：结果页和落地页支持的功能不同，实现的逻辑</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">也有所不同。结果页中的功能扩展主要关注为用户提供更好的搜索体</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">验，而落地页中的功能扩展则关注为用户提供更好的浏览体验，以及</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为用户在浏览过程中产生的新的搜索需求提供更好的支持。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从服务的角度来讲：结果页对接的服务与落地页不同，结果页对接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的是自有服务，而落地页对接的是自有服务或非自有服务。服务不</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">同，可协同优化的方案也不同。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基于上述
5个原因，将结果页和落地页拆分之后，流程变化可清晰描</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">述，场景边界可明确管理，功能扩展可精准实施。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">需求输入场景及技术实现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在需求输入场景中，客户端为用户提供的输入方式为文本、语音及图</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">像等，本节以文本输入为例介绍搜索功能在需求输入场景中的主要流</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程和技术实现，语音和图像输入的相关内容在本书第
4章介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">典型的文本输入搜索过程如图2-2所示，该过程主要分为输入搜索关键</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">字、展现搜索建议、点击搜索按钮三个步骤，其中输入搜索关键字和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">点击搜索按钮的动作由用户触发，展现搜索建议为搜索客户端提供的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">辅助输入功能，下面对典型的文本输入过程进行介绍。</span>

---

## Page 44

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">典型的文本输入搜索过程</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在搜索App中都会有搜索框，支持用户输入搜索关键字。搜索框可使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用系统的文本输入控件实现，这类控件在不同的平台中都有，比如iOS</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">平台中的UITextField。用户在点击搜索框后，软键盘弹出，这时搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">框变为输入态，用户可以输入想要搜索的文本内容。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在用户输入文本的过程中，客户端“监听”输入内容的变化，向服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端（服务器）实时提交输入的关键字信息，并由服务端生成用户可能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会搜索的关键字列表（搜索建议），客户端在收到服务端的搜索建议</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">后会展现给用户。如图2-3所示，用户输入“刘德”，客户端向服务端</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提交获取“刘德”搜索建议的请求，服务端返回一组搜索建议信息如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">“刘德华”“刘德凯”“刘德一”……当用户选中“刘德华”时，就</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进入了结果页场景，此时客户端向服务端提交搜索“刘德华”的请</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">求，加载结果页。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在这个阶段，除了业务流程的逻辑实现，还需要使用一些基础的技术</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">来完成功能的构建，如多线程技术、网络数据通信技术、数据处理技</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">术、展现技术和交互技术等。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p044_img01.jpg)

---

## Page 45

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.2.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多线程技术</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多线程技术是一种在计算机程序中同时运行多个任务的技术，旨在提</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">高程序的并发性和效率。如在需求输入阶段，用户输入的过程和搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">建议的获取过程分别由两个线程并行执行，目的是两个任务在执行的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">过程中互不影响。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户输入的过程与UI交互相关，一般来讲，UI相关的操作都是在主线</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程中完成的，而要想网络请求相关的过程与用户输入的过程并行处</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">理、不相互等待，这时就需要使用多线程技术。</span>

---

## Page 46

08:12  50 715
刘德  搜索
Q刘德华
Q刘德凯  个
Q刘德一
Q刘德华个人资料
Q刘德华经典歌曲
Q刘德伟
Q刘德华电影

语音搜索  无痕测览

23  空格  搜索

<!-- 图源: ./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p046_scan.jpg -->

---

## Page 47

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索建议示例</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在单核CPU上实现同个时间段运行多个线程，操作系统会将小的时间</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">片分配给每一个线程，这样就能够让用户感觉到有多个任务在同时进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">行，避免任务之间相互等待。如果CPU是多核的，那么多线程就可以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">真正以并发方式执行，从而减少完成某项操作所需要的总时间。关于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">并行化的相关内容将在第
4章介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.2.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网络请求</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端与服务端的通信通常使用超文本传输协议（HTTP）实现，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">HTTP是一个用于传输超媒体文档（例如HTML）的应用层协议，是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为网页浏览器与网页服务器之间的通信而设计的，也可以用于其他类</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">型的请求。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">iOS及Android系统都提供了用HTTP封装的API，其中，iOS系统中提</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">供了NSURL</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Request、NSURLSession，Android系统提供了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">HttpURLConnection。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">HTTP请求工作流程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">接下来介绍HTTP请求的工作流程。在实际使用过程中，系统提供API</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的设计不同，使用方式也不同，网络请求的几个关键步骤如下。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）参数设定：根据业务的需要进行HTTP请求的参数设定，如设定请</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">求的URL、端口号、请求方式（GET、POST）、Header、Cookie、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Body等信息。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）请求发送：将客户端设定的参数以二进制数据流的方式传给服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端。</span>

---

## Page 48

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3）客户端接收服务端的响应：服务端即服务器接收到客户端的请求</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">后，会向客户端返回响应，包括响应状态码、HTTP</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Header等信息。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">常见的状态码如下。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1××：信息响应，表示请求已收到，正在处理。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2××：成功响应，表示请求已成功完成。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3××：重定向，表示请求需要重定向到其他资源。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4××：客户端错误，表示客户端请求存在错误。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">5××：服务端即服务器错误，表示服务端即服务器无法完成请求。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4）客户端接收服务端即服务器返回数据：如果响应数据量比较大，服</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务端即服务器可能会分多次发送数据，客户端需要多次接收通知。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">5）客户端接收本次通信完成（或失败）通知的时候，相当于本次网络</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">请求结束。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上述流程仅是一般情况下的HTTP请求工作流程，实际应用中可能会因</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网络环境、服务器配置等因素影响而有所不同。在设计和使用网络API</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时，需要根据具体需求和场景进行调整和优化。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">HTTP请求参数介绍</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在HTTP网络请求发出之前，需要对相关的参数进行设定，如请求的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">URL、端口号、请求方式、Header、Cookie、Body等。本节简单介</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">绍URL、请求方式和Header。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）URL：统一资源标识符，又称统一资源定位器、定位地址、URL地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">址，俗称网页地址或直接简称网址，是因特网上标准的资源地址，如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">同网络上的门牌。</span>

---

## Page 49

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）请求方式：常用的请求方式为GET和POST，本节不作详细介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3）Header：对于客户端发出的请求，经常使用的Header是Referer</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">和User-Agent。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Referer：Referer请求头包含了当前请求页面的来源页面的地址，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">即表示当前页面是通过此来源页面里的链接进入的。服务端一般使用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Referer请求头识别访问来源，有时以此进行统计分析、日志记录以及</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">缓存优化等。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">User-Agent（UA）：请求头中包含的一组字符串，用于让网络协</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">议的对端来识别发起请求的用户代理软件的应用类型、操作系统、软</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">件开发商以及版本号，比如用来识别是Android平台还是iOS平台。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.2.3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索建议的数据处理</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端通过网络通信把用户当前输入的关键字上报到服务端，并在接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">到来自服务端的搜索建议后对建议进行解析及展现。在这个过程中，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端和服务端需要对网络通信数据进行结构化的封装、压缩、加解</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">密及数据校验等，以实现安全有效的数据传输。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据封装</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">常用的数据封装及解析格式有可扩展标记语言（XML）、JavaScript</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对象表示法（JSON）及协议缓冲区（PB），在日常工作中使用JSON</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的情况相对多一些。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）XML（eXtensible</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Markup</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Language）：标准通用标记语言的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">子集，可以用来标记数据、定义数据类型，是一种允许用户对自己的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">标记语言进行定义的源语言，具有可扩展性良好、内容与形式分离、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">遵循严格的语法要求、保值性良好等优点。</span>

---

## Page 50

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）JSON（JavaScript</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Object</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Notation）：一种轻量级的数据交换格</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式，具有简洁和清晰的层次结构，这使得它成为理想的数据交换语</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">言，不仅易于相关人员阅读和编写，还易于机器解析和生成，并且能</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">够有效地提升网络传输效率。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3）PB（Protocol</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Buffers）：Google公司开发的一种数据交换的格</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式，它独立于具体的语言和平台。并支持多种语言，如Java、C#、</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">C++、Go和Python等，每一种语言都有相应的编译器以及库文件。相</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">较于XML和JSON，PB序列化之后的数据是二进制的，不可读，更适</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">合大数据量的传输。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这
3种数据封装格式，iOS及Android均有支持。在实际的应用过程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中，可参考数据传输场景、服务端支持情况等信息来确定使用哪一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">种。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据压缩</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据压缩的目的是减少传输过程中的数据量，从而间接地减少网络传</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">输时间。数据压缩常用gzip，gzip是常见的数据压缩及解压缩算法，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在HTTP中也有使用，同时它还是UNIX系统默认的文件压缩格式。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据加解密</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">加解密算法分为对称及非对称两类，常用的为Base64及RSA算法。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）Base64：一种对称的加解密算法，所谓对称，就是指该算法加密和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">解密所用的密钥是相同的，Base64基于
64个可打印字符来表示二进制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据，由于64=2</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">，所以可打印字符每6位（比特）为一个描述单元；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Base64通常用于文本数据处理的场合，用来表示或存储数据，常见于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页面中的二进制数据描述；为了保证所输出的编码为可读字符，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Base64制定了一个编码表。编码表有
64个字符，这也是Base64名称的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">由来；Base64编码把原来每3个
8位的字节（3×8=24）转化为4个6位</span>

---

## Page 51

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的字节（4×6=24），字节前两位均为0，常用的编码表如表2-1所示；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对于私有内容的加解密，也可以自建编码表来实现。</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">表2-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Base64编码表</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）RSA：一种非对称加解密算法，所谓非对称，就是指该算法需要一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对密钥，使用其中一个加密，则需要用另一个才能解密；基于这个特</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">性，RSA常用于客户端与服务端的数据传输，在客户端及服务端分别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">存放加密及解密的密钥，如图2-4所示，客户端发送数据时使用公钥对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据加密，然后通过网络传输加密后的数据，服务端收到数据后使用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">私钥对数据解密，这样即便加密的公钥被公开了，数据传输的保密性</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">也是有保障的。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p051_img01.jpg)

---

## Page 52

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图2-4</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">RSA发送数据加解密过程</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据校验</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据校验常用的算法为MD5。MD5是一种广泛使用的密码散列函数，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">可以产生一个128位（16字节）的散列值，用于确保被传输信息的完整</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">性和一致性。它常用于数据完整性的校验，例如在客户端与服务器的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据传输过程中，先生成数据的MD5值，然后将数据及其MD5值一同</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">传输。服务端在收到数据后，对其进行MD5计算，并将结果与收到的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">MD5值进行比较，以确定数据是否丢失或被篡改。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">5.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">展现及交互</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端收到服务端返回的搜索建议数据后，使用UI控件对搜索建议内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容进行展现，常见的UI控件如TableView、ConllectionView等都支</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">持列表的展现形式。系统一般会提供很多的UI控件供开发者使用，如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">按钮、文本框、进度条、文本绘制、图片绘制等，这些控件通常在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App中展现内容及或提供App与用户交互的能力，是离用户最近的一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">层。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.3</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">结果页场景及技术实现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以文本搜索为例，当用户输入关键字确认搜索后，客户端将加载与关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">键字对应的结果页并将其展现给用户。也就是说，客户端需要具备展</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p052_img01.jpg)

---

## Page 53

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">现结果页的能力，还需要支持用户浏览及选择某个结果条目进入落地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">典型的结果页加载及浏览过程主要分为
4个步骤，包括记录历史，拼装</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索关键字，加载结果页和浏览结果页。其中前
3个步骤在App中实</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">现，最后一个步骤由用户触发。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如图2-5所示，用户发起搜索时，客户端会保存搜索记录即记录历史，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">并将搜索关键字拼装到URL中传给搜索服务器。之后客户端切换到结</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">果页场景，创建浏览内核及结果页相关功能模块，调用浏览内核向服</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务器请求加载携带搜索关键字的URL。服务器接收到请求后，提取</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">URL中的关键字进行搜索，并生成结果页及相关资源。然后，服务器</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会将结果页主文档返回给客户端加载。浏览内核收到服务器的响应</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">后，解析结果页内容。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如图2-6所示，通常情况下，结果页需要加载的资源可能有多个，因此</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览内核会依次向服务器提交请求，下载相关资源，并在下载完成后</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">渲染结果页，直到整个页面加载完成。页面加载完成后，用户可通过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上下滑动来浏览页面。当用户点击页面中的结果条目时，会加载落地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页。</span>

---

## Page 54

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图2-5</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页加载及浏览过程示例</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">▲图2-6</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页相关资源加载</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在页面加载过程中，有时会出现各种异常，这些异常可能源于网络问</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">题或服务器、客户端传递的参数格式错误等。因此，需要在客户端对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些异常进行处理。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">部分浏览器会在页面加载过程中显示进度条，提示用户当前页面加载</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的进度，让用户对页面当前加载状态有所感知。这些状态通常来自浏</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p054_img01.jpg)



![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p054_img02.jpg)

---

## Page 55

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">览内核API的通知，也可主动调用API获取。一般来说，系统提供的原</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生API具备获取基本页面状态和事件通知的能力。关于浏览内核的使用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">及扩展，将在第
5章介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.3.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据持久化存取</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在搜索过程中，记录用户输入的关键字并保存，以便下次用户搜索时</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进行选择。实现这一功能的主要技术是数据的持久存储和持久化存</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">取。持久存储用于存储App中需要长期依赖的数据，持久化存取则涉</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">及在需要时从存储设备中恢复数据。由于不同设备的存储特性不同，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对于文件的存取操作可以抽象地分为三部分：存储路径、存储数据的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方式和存储数据的格式。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">存储路径是指每个数据存储在设备中的文件路径，在移动设备中，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">iOS和Android平台采用的都是沙盒机制，App内部对文件的读取也有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">权限管理，不同文件夹的权限不同，包括是否可以修改、创建、读取</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">等。在具体的业务存储实现中，建议采用一个业务一个目录的方式进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">行管理，一个文件只存储一类内容。同时，文件及目录的命名也要有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">统一的规范，这样可以有效避免冲突并实现存储自治。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">存储数据的方式是指将数据以何种方式存储在文件中，基本操作为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">文件的读取和写入。平台API或第三方开源库都提供了对数据存储方式</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的封装，例如Plist、NSUserDefaults、SQLite3、CoreData等。选择</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用这些工具时，需要考虑要存储的数据量、数据的更新频率、更新</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方式（全量、增量、替换等）以及数据的格式等。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">存储数据的格式是指持久化存取的文件数据格式。根据业务需要，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对所依赖的数据项进行存储格式的封装，其中包括对数据的操作和存</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">储更新相关能力的封装。在研发过程中，需要关注数据升级带来的变</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化以及数据异常对App的影响。前面提到的数据加解密和数据校验这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">些技术在持久化存取过程中也有重要应用。</span>

---

## Page 56

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.3.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">URL携带搜索关键字</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在不同搜索客户端，URL携带搜索关键字的实现有所不同。这一节以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览器携带搜索关键字为例，说明用户提交的搜索关键字是如何传送</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">给搜索引擎的。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">表2-2是不同的搜索引擎在Safari浏览器中搜索“刘俊启”这个搜索关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">键字时，浏览器发出的网络请求。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">表2-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不同搜索引擎在Safari浏览器中搜索“刘俊启”时发出的网络请</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">求</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为什么使用浏览器验证呢？因为搜索服务在浏览器中是使用业界公开</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的标准传递关键字信息的，其次从URL的定义标准来讲，同一个URL</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在不同的浏览器中打开，用户看到的内容应该是一致的（服务端差异</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化定制部分或与用户私人信息相关的部分除外）。若是相同的URL在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不同的浏览器中所展现的内容不一样，说明网站对于不同的浏览器进</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">行了差异化定制。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在使用Safari浏览器做不同的搜索引擎对比时，可以发现从地址栏中复</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">制出来的URL中存在中文，直接把这种带中文的URL复制到浏览器</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中，页面也可以正常打开。这有些不符合人们对URL的常规认知。因</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">此，我最后在浏览器的网络层对真正发出的网络请求进行了确认，发</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">现所有网络请求都是经过URL编码处理的。至少从表面上来看，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Safari浏览器对特殊字符进行了自动兼容。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p056_img01.jpg)

---

## Page 57

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而在Chrome浏览器中，通过使用上面的搜索引擎进行搜索，再从地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">址栏复制URL到浏览器中，可以发现它们都是经过URL编码的，其处</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">理逻辑和Safari浏览器不太一样，但网络请求的URL是一样的。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在搜索客户端中，可能会遇到相同的问题。无论是用户发起的关键字</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索还是直接输入URL，当输入的内容不符合网络通信的标准时，客</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户端需要主动进行编码，以符合标准。用户输入关键字后，客户端向</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">服务端发送搜索请求，这一动作会将关键字作为URL参数的一部分上</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">传到服务端。这时，需要对URL中的特殊字符进行编码，否则即使在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">地址栏看起来正常，在实际的网络请求中也会出现异常。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通过上述分析可以发现，每个搜索引擎中URL携带关键字的方式都有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所不同。以表2-2所示的内容为例，sogou.com中的关键字的key是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">query，而so.com中的关键字的key是q。如果你对此感兴趣，可以使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用表中带有中文的URL进行一次URL编码，或者在不同的浏览器中尝</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">试不同的搜索引擎，观察它们如何在URL中描述关键字。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.3.3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页的分类及加载</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">众所周知，用户可以直接在浏览器中使用搜索业务，这是因为搜索业</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务都是基于网页格式承载的。无论是搜索结果还是点击结果后进入的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页，都以网页的形式呈现。这样一来，整个搜索过程中用户的浏</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">览、交互等需求都可以在浏览器中得到有效满足。而在搜索App中，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页既可以是网页格式，也可以是非网页或网页+非网页的复合格</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页格式结果页：搜索引擎展示内容和与用户进行交互的主要数据</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">格式。对于搜索客户端而言，需要在客户端构建网页浏览能力，以支</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">持网页的加载，其中包括结果页和落地页。只要客户端支持网页浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能力，搜索的核心流程就可以运行。此时搜索框加上工具条（支持前</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">进、后退、刷新等页面操作能力）就构成了搜索客户端的最小集合。</span>

---

## Page 58

<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">非网页格式结果页：使用自定义的数据格式来传输结果页的数据内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容，在客户端进行解析、渲染和交互。与网页格式相比，这种格式可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以提供更大的定制空间，从而在能力、效率和效果等方面带来变化，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为用户提供更好的搜索体验。然而，这种格式需要更高的开发和维护</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">成本。服务端需要同时支持原浏览器的网页格式数据和自定义格式数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">据，否则以浏览器作为客户端的用户将无法使用搜索服务。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">复合格式结果页：以网页格式和非网页格式承载结果页的内容。通</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">过技术手段将两类格式的内容组合在同一个页面，以实现复合格式的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页，复合格式同样需要服务端的支持，同时还需要在客户端增加</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">额外的逻辑来处理网页格式和自定义格式两类内容的传输、解析和渲</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">染等，以支持它们在结果页中进行交互和切换。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为什么要关注数据格式呢？因为传统的搜索结果页是基于网页格式承</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">载的，网页提供的能力受到浏览内核的限制，很难实现差异化定制。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而非网页格式数据的传输、解析、渲染及交互是可以通过系统提供的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">原生API实现的，API的能力则受到操作系统的限制，定制成本低。从</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用的能力层级来看，原生API提供的能力超过浏览内核提供的能力。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因此，非网页的数据格式可以使用更多的能力，从而为用户提供更好</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的搜索体验，这也是搜索客户端的价值之一。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据格式决定了数据的传输、解析、渲染、交互等能力的构建方法。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构建时既需要考虑实现新能力的成本，也需要考虑搜索业务已有能力</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的迁移成本及所有能力的维护成本。只有搜索结果页和详情页都是非</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页数据格式时，才可以不依赖浏览内核展现内容，否则需要提供网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页浏览能力。故浏览内核的使用及扩展优化是一件极其重要的技术性</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">工作，可以考虑利用系统原生API扩展网页浏览能力，或自建网页浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能力。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">注意</span>

---

## Page 59

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在iOS平台中，自建网页浏览能力是不被允许的，App在提交到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Store时审核会不通过（iOS平台的Firefox和Chromium，使用</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的均是系统原生的API）。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.4</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">落地页场景及技术实现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当用户在结果页中看到一条与想要的结果相关的内容并点击对应条目</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时，系统就开始加载落地页了。落地页加载过程中相关事件的处理模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式与结果页几乎一样，这里不做过多介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.4.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页功能扩展</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在搜索客户端中，落地页同样可以是网页、非网页及复合格式。只要</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提供内容的服务是可协同的，那么理论上这个内容格式就是可接受</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对比结果页，网页格式的落地页的功能扩展一般是缺少端云协同的，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">只能借助Web生态的标准进行搜索浏览体验的优化，常见的有扩展功</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">能划词搜索、长按识图、字体大小调整、广告过滤、语音播报网页内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容及性能优化等。这时依赖的技术主要与网页内容的提取及读写有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">关，比如网络通信，网页内容解析，浏览及交互事件处理，JS与网页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通信等。一些功能还会依赖特定的技术，如语音播报需要TTS技术，长</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">按识图需要图像识别技术。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一些自定义格式的内容在客户端以NA方式实现时，所依赖的技术不局</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">限于浏览内核。例如，音视频内容可以自实现，或引入音频和视频的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">编码解码技术来承载；AR/VR内容则依赖3D建模、移动AI和大数据计</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">算等技术；地图内容则依赖地理信息相关技术。实际上，不同类型的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容都需要特定的技术来支持，这些技术点与内容场景密切相关，此</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">处不再赘述。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.4.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">落地页与结果页的切换管理</span>

---

## Page 60

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在用户进行搜索的过程中，结果页和落地页的切换是一个高频操作。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如果结果页和落地页都是网页格式，那么它们可以在同一个浏览内核</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中打开，页面切换主要由浏览内核管理。而当结果页和落地页的格式</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不一致时，需要不同的容器承载，容器切换需要统一的能力支持。在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端中，根据浏览内核是否管理结果页和落地页的切换分以下两种</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">情况。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）由浏览内核管理：结果页和落地页都是网页格式，不需要其他数据</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">解析及渲染能力，只需要在浏览内核中实现页面切换。客户端可以使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用单浏览内核或多浏览内核技术方案，并通过浏览内核的事件通知来</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">获取页面加载状态，以实现状态提示。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）单独构建能力管理：结果页或落地页的内容中包含非网页格式（包</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">括非网页格式和非网页+网页复合格式两种）的数据，需要在客户端中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现数据传输、解析、渲染及交互相关的能力。这里页面的切换、展</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">现以及页面关系的衔接需要借助技术框架来管理，同时也需要知晓新</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页面的加载时机及相关的状态，以便进行统一的调度。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页或落地页的内容中包含非网页格式的数据包含以下
3种情况。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页是网页格式，落地页中包含非网页格式的数据。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页中包含非网页格式的数据，落地页中也包含非网页格式的数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">据。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">结果页中包含非网页格式的数据，落地页是网页格式。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">总的来说，当结果页和落地页包含非网页格式的数据时，超出了浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内核可管理的范围。因为每类页面按需实现，且实现技术均有不同，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所以页面相互独立，需要在技术框架层面对结果页和落地页提供支持</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">及管理，第
6章会对此进行详细介绍。</span>

---

## Page 61

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.5</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">移动客户端研发注意事项</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">前面介绍了搜索流程相关的技术，基于这些技术可以实现移动搜索客</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户端的核心功能。本节重点介绍移动客户端与PC应用研发的区别，以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">及移动客户端与云端服务研发的区别。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.5.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">移动客户端与PC应用研发的区别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在2005年，我从PC端研发转到Symbian系统（塞班公司为手机设计的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">操作系统，主要为诺基亚品牌的智能手机使用）端研发，当时做移动</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端研发的人员极少，大部分都是从PC端转到Symbian的（近几年</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">团队中很多新同事依然没有移动研发经验，即在学校或之前的工作中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">没有接触过移动端研发）。当时的入职培训曾提到移动客户端研发和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">PC应用研发的区别，但由于部分细则我现在已经记不清楚了，所以本</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">节将基于我的理解，来讲述二者的区别。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用场景的区别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户使用PC的场景比较固定，通常是在家、办公室、咖啡厅等比较固</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">定的地点，一般有比较稳定的电源输入和网络环境（网速比较快），</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用的时间也比较固定；而手机一般是随身携带的，主要以电池作为</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">电源，使用的时间、场景（地点）通常是不固定的，以至于网络也是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不稳定的（用户所在位置不同，存在不同网络切换的情况，比如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">WiFi、5G、4G等，也存在无网络、弱网络的情况。在移动网络下还需</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要考虑用户的流量资费问题）。因此进行移动客户端研发，需要关注</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不确定因素带来的异常情况，需要尽量降低对手机的资源消耗以保证</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">手机有足够长的待机时间，还需要及时响应用户在碎片化场景中的需</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">求，同时要注意对用户隐私数据的保护，特别是现在，手机中有非常</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多的传感器。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">硬件配置的区别</span>

---

## Page 62

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与PC相比，手机在CPU计算能力、内存空间等方面存在一定差距，并</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">且手机能够随身携带，因此长时间待机、重启或关机的情况较少。这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">意味着研发手机上的应用时，需要更加合理地利用和释放资源，减少</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不必要的计算，以降低电量消耗，或者避免因资源不足而产生使用异</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">常、对手机的待机时间产生严重影响。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">PC应用的主要输入设备是键盘和鼠标；而在移动客户端主要是触摸</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">屏、软键盘和响应手势。在研发过程中，需要考虑交互方式的变化，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">例如用户点击、滑动的不同响应，以及展现软键盘时对布局的影响。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如果是跨平台项目，在设计架构时还需要考虑输入层的隔离，以保证</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">业务逻辑和基础能力具有较高的可迁移性。同时，移动设备还配备了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">麦克风、前/后置摄像头、地理位置信息等传感器，这些输入设备为移</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">动设备的客户端产品提供了更多创新机会，可以在自有应用中使用。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">PC应用的主要输出设备是显示器，显示器通常在20</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">in</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span>
<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">cm。</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以上，而手机屏幕通常在7</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">in以下。在显示器上可以同时显示多个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">运行的应用程序，而在手机上大多只能有一个App在前台运行。因</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">此，在移动客户端研发过程中，需要区分App当前所处的状态，以及</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">评估一些需要后台执行的任务是否可以得到支持。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">研发生态的区别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">PC生态以微软的Windows为主，而移动生态以苹果的iOS、谷歌的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Android和华为的鸿蒙为主，还有一些厂商基于Android系统开发的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生态。一个App选择了在哪个生态中发布，就意味着选择了这个生态</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中的研发、测试、发布方法，以及对应的市场和用户群体。这个生态</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中涉及的开发工具、开源代码、生态标准等均需要研发人员了解。在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术选型时，对于生态中的标准，系统提供的能力及研发语言，甚至</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生态中的上下游厂商的支持程度也需要研发人员考虑。</span>

<span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">英寸，1</span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#dc143c"> </span><span style="font-family:'Unnamed-T3';font-size:11.13pt;color:#000000">in=2.54</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p062_img01.png)

---

## Page 63

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在开发阶段，iOS平台使用的集成开发环境（IDE）主要是Xcode，而</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Android平台主要是Android</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Studio。在生态的主语言方面，iOS平</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">台从原来的OC逐渐向Swift过渡，Android平台也从原来的Java向</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Kotlin过渡。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不论使用什么语言进行，良好的编码习惯都可以帮助研发人员规避很</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">多不必要的麻烦。对于团队来讲要想形成良好的编程习惯，需要有一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">些流程规范。如果流程规范不够完善，应该及时提出优化方案。流程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">规范是团队研发过程的底线保障，所以必须严格遵守。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在App产出阶段，研发人员可以根据业务需要实现不同的功能，还可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以针对平台特性及业务需要积累一些实用的小工具。这些工具有些是</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">来自开源的，有些是团队内部自研的，一般来说自研的工具，对于提</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">升团队研发效率帮助较大，应该被熟练掌握。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在App发布阶段，iOS平台有App</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">Store支持，Android平台不同的手</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">机厂商也有自家的应用商店。iOS平台实际上也是面向厂商的，只不过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">他们的厂商只有苹果一家。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.5.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">移动客户端与云端服务研发的区别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本节我们分
3个维度来对比移动客户端与云端服务研发的区别。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据版本兼容的区别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一般来说，客户端会在与服务端联调成功之后再发布新版本。数据协</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">议的兼容主要由服务端支持，即在服务端兼容不同版本客户端的数据</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">请求。但是客户端需要关注本机产生的历史数据文件的兼容性，比如</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据文件格式的升级、更名、更改路径等，还需要关注服务端的协议</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">异常，比如服务端下发空数据、数据格式不符合约定等。这些兼容性</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">和容错性问题在客户端均需要考虑，否则就会影响客户端的稳定性。</span>

---

## Page 64

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">质量保证方式的区别</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通常来说，云端服务的接口稳定测试可以通过穷举法来完成，压力测</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">试可以通过并行化来实现（同时也需要考虑不同网络、机房和运营商</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的区别），有很多对应的工具链可供使用。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">相比之下，客户端业务场景会随着版本的迭代而升级，同时会受机型</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">配置、设备授权状态、覆盖安装、功能开关、网络状态和测试环境等</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">诸多因素的影响，这些因素都是易变的，留给开发自动化测试的有效</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">周期较短，因此版本迭代测试机制的实现主要依赖人工。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新版本发布覆盖度</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">云端服务的发布过程主要由内部完成，上线之后可以在较短的时间内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">覆盖全量用户群体。而移动客户端研发完成，在上线时需要先提交到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">应用商店进行审核，存在被拒的风险。审核通过后发布给用户，需要</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户先更新再使用，这个过程需要一段时间，并不是实时的。上线后</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一旦有严重Bug，修复Bug的更新包较大，那么成本会更高，时间也会</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">更长。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">App的技术架构需要尽可能保证代码变化是可控的、影响较小的、风</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">险可评估的，这样一旦线上版本产生问题，可以把问题的影响降到最</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">小。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">2.6</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">设计一份可落地的技术方案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本节重点介绍不同场景中，在客户端上实现搜索业务所依赖的技术方</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">案。技术方案可以保证搜索客户端的主体业务实现得更为顺畅。方案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中涉及的技术在业界中一般都有通用的解决方法，或者是在原生系统</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">中由对应的API提供。如何使用这些技术构建流程中的业务能力，是一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">个关键问题。要构建的业务能力，通常都是功能需求所描述的目标，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">而实现这些目标的技术方案描述了某个功能在使用哪些技术，要经过</span>

---

## Page 65

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">什么样的流程、逻辑、调用等内容。同一个功能，可能会因为设计技</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">术方案时的偏重点不同而有所不同，多轮评估及调整之后的技术方案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">会由多元化转为一体化。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">通常来讲，技术方案的确定，至少需要通过团队中的高阶工程师评</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">审，还需要团队成员达成共识，否则这个方案落地的可能性较低。写</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案是研发人员必备的基础能力，大部分研发人员在写技术方案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时，通常关注“如何去做”，也就是技术方案如何落地。实际上，技</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">术方案的落地离不开团队的资源投入，在技术方案中适当增加一些与</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">资源投入相关的内容，可以使方案更容易通过评审。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.6.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案的辅助决策点评估</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">大多数研发人员在评估技术方案时，都会优先考虑方案的有效性，实</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">际上，也存在方案有效性不完整的情况。除了有效性，对实施成本和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">价值的评估也是技术方案评估的重要环节。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本节重点介绍技术方案的
3个评估点——方案的有效性、实施成本及价</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值。当然，在设计技术方案时，也需要结合功能实现的实际情况进行</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">全面的评估。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案的有效性评估</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对于一个技术方案，首先要评估其有效性，也就是说这个技术方案对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">于解决某个问题（实现某个目标）是否有效，是在全部场景下有效还</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">是在部分场景下有效。如果在全部场景下有效，那就说明基于该技术</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方案研发的产品上线之后，不会出现场景不同造成的逻辑不统一的情</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">况。如果在部分场景下有效，那么就说明要实现最终目标，需要针对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不同的场景提供不同的技术方案，需要多个技术方案并行开发和维</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">护。这时不仅会增加维护成本，也会因为方案的不统一导致支撑业务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的流程不统一、效果不统一，甚至在一些场景下存在无效的情况。</span>

---

## Page 66

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">有效性是评估技术方案是否可行，如果技术方案对于目标实现没有效</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">果，那么根本就不需要推进了。如果技术方案在将来某个时间会有效</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">并趋于成熟，那就先评估是否有必要提前投入资源。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">注意</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在技术方案设计的过程中，所依赖的技术点应该是确定的，如果这个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">都不能确定，那么实施风险就是不可控的。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案的实施成本评估</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">确定技术方案有效之后，就要评估技术方案的实施成本了。技术方案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的实施成本与业务的运营现状、团队的研发现状、线上用户规模有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">关。技术方案实施的最大成本来自现状的变化，以及实现该变化的复</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">杂度和风险控制。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">成本有很多种，比如使用成本、兼容成本、维护成本、协同成本等。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对于部分技术来说，使用成本并不高，但维护成本非常高。特别是一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">些只在少量产品中应用且没有固定团队维护的技术，在评估时，既要</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">关注可用性，又要关注可维护性，否则使用该技术方案获得的良好收</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">益可能只是一时的，长期的维护过程会非常痛苦。使用某个技术的前</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提是清楚技术的边界和细节。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在评估技术方案实施成本时，既要关注短期投入又要关注长期投入，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">既要看投入的资源又要看交付的时间，人力成本和时间成本都是成</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案的价值评估</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">从技术角度来看，技术方案不仅要满足功能需求，还要提供额外的价</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值。这些价值可以通过短期和长期两种方式进行评估。短期价值是指</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在当前功能需求上线时直接产生的价值，包括业务流程、研发流程、</span>

---

## Page 67

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户体验、技术指标和收入等方面的变化。长期价值则是指与之前的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案相比，在实现相同功能需求时所产生的变化，例如业务接入</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方式和研发/维护成本等方面的变化。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案的价值大小取决于方案的提出者对团队需求的理解程度。确</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">定价值的过程，实际上就是确定技术方案目标的过程，确定成本的过</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程，实际上也是确认方案的实施依赖的过程。在有限的成本下，有效</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现技术方案而达到价值的最大化，才是技术方案落地过程的终极目</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">标。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.6.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案优先处理原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案确定后，在实施之前，通常需要经过团队相关人员的讨论，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">目的是对技术方案的实现思路达成共识。这个阶段常用的方式是技术</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">方案评审。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在评审过程中，通常有评审发起人和评审人两种角色参与。除了方案</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的有效性外，技术方案的相关节点也需要在评审中被关注，这些节点</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">包括用户、业务、团队、规范、生态及全局性等方面，会对团队及业</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务的长期价值产生影响。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户优先原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">若是技术方案中存在损害用户使用体验的点，如让用户使用体验变</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">差、资源消耗升高、用户数据隐私被窃取、稳定性变差等，那这个技</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">术方案就不是好的方案。产品的使用体验是影响用户使用产品的一个</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">关键因素，所以技术方案必须规避这类问题。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">业务优先原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所处的角度不同，一些隐藏的影响面在设计技术方案时没有被考虑，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">就可能出现技术方案与现有的业务流程相互冲突的情况，比如行为不</span>

---

## Page 68

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一致、业务适配成本高、指标变化大等。一些技术看起来很厉害，但</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对业务没有帮助，那么这些技术的价值就很难体现出来，技术方案最</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">终要更好地为业务增长赋能，要支持业务高质、高效迭代。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">团队优先原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">若是技术方案中存在与团队的目标和需要不一致的点，就需要评估该</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术方案是否有必要采用了。比如某些技术方案中涉及重复“造轮</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">子”的工作，对于团队来说这不仅是重复投入研发资源，还会增加维</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">护和设备投入等成本。如果技术方案对于团队来说不是最优解，在横</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">向推进时必然会有阻力。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">规范优先原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">若是技术方案中存在与现在运行规范冲突的点，一般情况下要么优化</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">规范，要么调整技术方案。但规范是团队积累下来的经验，是团队内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所有人员都要严防死守的底线，遵守规范是团队协同工作的基本原</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">则，若是不能做到这些，规范将逐渐变为摆设，最终影响整个团队的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">工作和发展。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">5.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">生态优先原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">若是技术方案中存在与平台、生态规则冲突的点，就需要考虑这个技</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">术方案是否需要放弃了。在技术方案中应该通过技术手段首先规避这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">类冲突，只有大家共建，生态才会变得更好，破坏生态规则的要么自</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">己出局，要么最终导致大家都陷入死局。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">6.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">全局优先原则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">若是技术方案在解决当下问题之时，牺牲了其他节点的收益，那么即</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使它实现了当前的目标，也需要谨慎对待。好的技术方案应该站在全</span>

---

## Page 69

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">局的收益基础之上考虑，不仅要局部变好，还要全局变好，若是实现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">局部变好的条件是全局变差，那么是不推荐这个技术方案的。</span>

---

## Page 70

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第3章</span>

<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">搜索客户端基础服务</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第
2章介绍了搜索客户端研发所需的基础技术，基于这些技术可设计并</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实现搜索客户端的不同功能。客户端作为搜索服务的延伸，依赖服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端实现信息检索功能，这同时也需要其他服务端的支持。客户端与服</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务端的紧密合作，确保了搜索业务的完整性。本章将探讨服务端与客</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户端协同的基础服务，重点介绍搜索服务的核心能力，以及客户端在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提升搜索服务价值方面的重要性。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.1</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">搜索客户端协同的服务分类</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索客户端依赖的服务包括两部分：一部分是客户端运行时与服务端</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对接的服务，另一部分是搜索业务与服务端对接的服务。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.1.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端运行时对接的服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">客户端运行时对接的服务端常见的有云控服务端和数据服务端，服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端是客户端实现基础能力的保障，为客户端中的功能提供数据同步支</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">持。本节的内容与搜索服务无依赖关系，既适用于搜索客户端，也适</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用于其他产品客户端。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">云控服务端的对接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">云控服务端通常是客户端类产品依赖的核心服务，用来下发客户端依</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">赖的数据，包含客户端中不同业务的配置。在客户端中，与云控服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端对接的相关能力通常被封装为一个独立的云控模块。云控模块主要</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分为调度控制、状态收集、数据传输及数据分发这四个核心子模块，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如图3-1所示。</span>

---

## Page 71

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">云控模块与云控服务端及业务模块的通信</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">云控功能的触发要通过规则或特定的事件，所以需要在客户端明确更</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新的时机，即通过客户端云控模块向云控服务端发送更新最新配置状</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">态的网络请求，这部分工作主要由调度控制子模块负责。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当调度控制子模块确定需要与云控服务端同步数据时，先会收集当前</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">设备中的配置项、开关等数据，再将这些数据提交到云控服务端。由</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">于业务功能不同，传输的数据不同，业务中的数据格式也有所不同，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">由状态收集子模块负责获取每个业务当前的数据状态，状态收集子模</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">块与业务模块是一对多的关系。数据收集完成后，由数据传输子模块</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对数据进行打包，调用网络模块传输数据至云控服务端，等待接收云</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">控服务端返回的数据。之后通过数据分发子模块将不同的数据分发给</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">不同的业务，至此基本的云控数据更新流程完成。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">云控模块与云控服务端的通信是基于C/S（Client/Server，客户端/服</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">务器）架构实现的。通信请求通常在客户端的启动阶段、某个时间周</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">期或由某个事件触发，以实现客户端配置项数据及其状态的上报和更</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新任务。云控更新过程中更新的是多个业务的数据，随着团队规模变</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">大，会涉及多个模块，也会涉及多个子团队，从架构设计的角度看，</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p071_img01.jpg)

---

## Page 72

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">业务数据的拼装及解析工作应该交给业务层来管理，这样既可以避免</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">业务升级带来的信息不同步或其他数据解析问题，同时还可以让云控</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">模块处于长期稳定的状态。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据服务端的对接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据服务端为客户端提供数据存储支持，记录与业务或用户行为相关</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的数据。在客户端将与数据服务端对接的相关能力封装为独立的数据</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">收集模块，以支持不同业务记录相关指标的数据项。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据收集模块与数据服务端基于C/S架构通信，以上行数据为主。关于</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据收集能力的建设参见8.3.2节。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.1.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索业务对接的服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索业务与服务端对接的服务主要为搜索业务构建。按照是否可定制</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">将搜索业务对接的服务端分为自有服务端和第三方服务端两种，其中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">自有服务端又分为搜索服务前端和自建内容服务端，第三方页面或站</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">点相关的服务都属于第三方服务端。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索服务前端的对接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索引擎是一个比较庞大的系统，在这个系统中，通常以网页的形式</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">为用户提供内容检索服务。在PC时代，搜索产品没有客户端，用户可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以使用浏览器直接加载搜索引擎主页来使用搜索服务。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索服务端提供内容检索的能力，在搜索客户端中用户输入搜索关键</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">字，客户端通过浏览内核加载携带搜索关键字的URL，为用户展示搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索结果。搜索客户端对接的是搜索服务前端，前端通常指的是网站或</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">应用程序的用户界面部分，在搜索服务中前端主要是运行于PC端、移</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">动端等浏览器（或搜索客户端）上展示给用户的网页。</span>

---

## Page 73

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在早期，搜索客户端与搜索前端的通信是基于B/S</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（Browser/Server，浏览器/服务器）架构实现的，客户端可以通过浏</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">览内核加载搜索引擎主页并使用搜索服务。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">随着客户端与服务端协同的不断发展，现在搜索客户端与搜索前端已</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">经可以按需基于C/S和B/S的混合结构进行通信了。搜索服务的更多内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容在本章后面会有更详细的介绍，这里不再展开。关于客户端的设</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">计，应该重点关注网页与客户端的通信，以及端云协同实现的差异化</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">（参见第5章），并且要尽量保证结果页加载及控制逻辑的独立性。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">自建内容服务端的对接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索客户端浏览的自建内容（比如视频、地图、图片等），通常存放</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在自有服务器中为搜索提供服务。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在浏览器中使用搜索服务，搜索结果及落地页只能是网页格式，否则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">就会出现内容可被搜索到但无法打开的情况。而在搜索客户端的支持</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">下，可以对自建内容的格式进行定制化支持，一些非网页格式的自建</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容，可以被搜索到，也可以在搜索客户端中展现及交互。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">因为是自有服务，搜索客户端与自建内容服务端的通信既可基于C/S架</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">构实现，也可基于B/S架构实现。从内容的可用性来看，自建内容服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端应可根据客户端的信号参数，以不同的架构模式提供服务。在客户</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">端的架构设计中，应重点关注内容的可扩展性，这部分内容在本书的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第
6章介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">第三方站点服务端的对接</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索客户端与第三方站点服务端的对接同样是前端，主要用来加载落</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">地页，通常是用户浏览搜索结果时，点击结果页内链接跳转至落地</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页。第三方站点与搜索客户端定向协同的机会不多，二者通信主要基</span>

---

## Page 74

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">于B/S架构。搜索结果中的第三方内容主要为网页，依赖搜索客户端的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">浏览内核加载、展现及交互，以支持用户查看页面内容。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索客户端除了需要具备基础的页面浏览能力外，还需要具备一些辅</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">助功能来为用户浏览提供更好的体验。值得一提的是，落地页的浏览</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">体验是整个搜索体验中的一个重要环节。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">需要提醒的是，第三方的服务和内容存在不可控的情况，其服务质</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">量、稳定性、安全性都需要重点关注，并需要构建相应的机制及管控</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">策略，以此来保证用户在使用搜索客户端时不会产生异常。这部分的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容在本书的第
7章介绍。</span>
<span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">3.2</span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:16.69pt;color:#000000">从客户端的角度看搜索服务端架构</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本节主要介绍搜索服务的基本架构。从客户端的角度来看，搜索服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">是个黑盒。客户端在发起搜索请求时，会通过URL携带搜索关键字给</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">服务端。服务端在收到客户端的请求后，解析对应的搜索关键字信</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">息，从搜索引擎的数据库中，检索出相关的结果并以网页的格式呈现</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">给用户。在这个过程中，客户端研发人员不需要关注服务端如何运</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">转，就可以实现基本的客户端搜索能力，但有关端云协同的事项，客</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">户端研发人员就较难提出关键的建议。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索业务的本质，就是信息的整合，即从海量的数据中找到用户想要</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的内容。基于这个思路，搜索引擎至少需要解决
3个问题。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容如何产生。有内容，搜索引擎才可以实现对内容的检索，没有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容，搜索就会缺少受体，自然检索不出结果。即便有了内容，也需</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要在收集的过程中考虑其质量和时效性，比如过多的重复内容，会使</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户的搜索体验变差，重复内容对服务器的存储及计算来说也是一种</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">资源浪费。</span>

---

## Page 75

<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">海量的内容存储和检索问题。搜索引擎存储的内容越多，可检索到</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的结果就会越多，相应地，检索过程需要处理的信息量就会增大，检</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索的效率就会降低。数据入库的方案一般会与检索的方案相关。如何</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">入库及存储内容以实现高效检索也是搜索引擎需要考虑的关键问题，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">否则在愈发海量的数据面前，检索的效率会变得越来越低，用户的需</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">求长期得不到满足，搜索体验就会变得极差。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">如何理解用户搜索需求及结果内容匹配。用户输入搜索关键字，搜</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">索引擎如何理解用户的需求，并从海量的数据中找到匹配的结果，这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">些问题需要有对应的机制和策略来实现低成本的优化。这与搜索引擎</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的匹配策略有关，也与产品策略有关，同样也受搜索引擎数据库中的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">数据影响。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上述内容仅是笔者个人的理解，与实际中遇到的问题可能有一定偏</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">差，但“万变不离其宗”，搜索引擎需要解决的问题都与这
3个问题有</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">密切的关系。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.2.1</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容的产生</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户使用搜索引擎时，希望检索到的就是整个互联网中的内容，也就</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">是说，希望从整个互联网范围检索想要的内容。基于这个需求，互联</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网中产生的内容应该尽可能地被搜索引擎收录。在搜索引擎中，网页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的收录工作主要由网页爬虫来完成。网页爬虫技术是一个公开已久的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">技术。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索引擎有
3个最主要的指标——全、快、准。在用户检索阶段，快和</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">准是核心指标；在内容收集阶段，全和快是核心指标。这就要求网页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">爬虫既能覆盖较广的内容，又能快速抓取时效性较高的内容，因此，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">爬虫的设计及应用是非常关键的。</span>

---

## Page 76

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页爬虫抓取网页的思路可以概括为：首先加载主页面，保存页面数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">据，然后通过主页面中的链接抓取子页面，递归地进行下去，以获取</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">更多的页面内容。图3-2所示是基本爬虫框架及工作流程，其详细的流</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">程说明如下。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）首先从互联网页面中选择一部分网页，以这些网页的地址作为种子</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">URL并将它们存储在种子URL队列中，网页爬虫再将种子URL队列中</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的这些种子URL放入待抓取URL队列中。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）网页爬虫从待抓取URL队列中依次读取URL。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3）网页爬虫将URL交给网页下载器。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4）网页下载器完成网页内容的下载。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">5）网页爬虫提取网页中的内容，包括网页的源码信息。这个过程中会</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">做两件事：</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">a）将网页中的内容存储到已抓取网页库中，等待建立索引等后续处</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">理；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">b）将网页的URL放入已抓取URL队列中，以避免网页的重复抓取。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">6）从刚下载的网页中提取出所包含的链接信息——sURLs。</span>

---

## Page 77

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">图3-2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">基本的爬虫框架及工作流程</span>

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">7）获取已抓取URL队列中的数据信息——hURLs。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">8）sURLs和hURLs进行匹配，将没有被抓取过的URL，放入待抓取</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">URL队列末尾，在之后的抓取调度中会下载这个URL对应的网页。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复上面的流程，直到待抓取URL队列为空，这意味着爬虫系统已经</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">将能抓取的网页全部抓完，即完成了一轮抓取。抓取策略可以分为3</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">种：全量抓取、增量抓取和实时定向抓取。将这
3种策略组合应用于不</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">同的场景，可以尽可能多地覆盖不同类型的网页，并抓取到时效性较</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">高的内容。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">全量抓取：搜索引擎爬虫的基本能力，目标是覆盖大部分站点及页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">面，配置的种子URL较多；特点为抓取的网页较全，任务较重，一次</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">完整的抓取工作完成迭代周期比较长，抓取的内容时效性偏弱。</span>

![](./搜索架构之道：App中的搜索系统设计与优化实践_assets/images/p077_img01.jpg)

---

## Page 78

<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">增量抓取：针对全量抓取内容时效性偏弱问题的解决方案，它可以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">使用更小的任务量和迭代周期对时效性较高的站点或页面进行内容抓</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">取，种子URL只需要配置几个重点关注的站点或页面，就可以实现较</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">小的时间间隔抓取到新的内容；常见的增量抓取包括对专题页面或对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">新闻门户等站点的抓取。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">实时定向抓取：全量抓取和增量抓取都是网页爬虫主动按照规则周</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">期性抓取网页内容，当页面内容是自有内容或由合作伙伴产生时，可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以建立实时定向抓取机制，即当有新内容产生或内容有变化时，由内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容提供方通知网页爬虫抓取新内容，或按照搜索引擎的标准将新增的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容入库，实现内容的实时更新。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在将网页收录到已抓取网页库的过程中，由于抓取时间与网页产生的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">时间存在一定的时间差。当一次抓取完成后，也会存在一些页面的内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容没有被收录的情况。同时，页面被抓取之后其内容还有可能产生变</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">化。这些情况下要保证检索的内容有较高的时效性，需要定向优化爬</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">虫的更新机制，以符合搜索服务的业务目标。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.2.2</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容的去重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">据统计，相似的网页数量占总网页数量的比例高达29%，而完全相同</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的网页占总网页数量的比例大约为22%，二者相加，即互联网中有一</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">半以上的网页是完全相同或者相近的（数据来自《这就是搜索引擎》</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一书）。识别重复网页一直是搜索引擎的关键能力之一，这项能力的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">强弱决定了后续存储、计算的成本及检索的质量，也间接影响着解决</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">搜索引擎“准”的问题。针对内容的去重问题，本节重点介绍重复网</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页的不同类型、识别价值、思路及算法。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复网页的不同类型</span>

---

## Page 79

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些重复的网页中，有的是没有一点儿改动的副本，有的是仅在内容</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上稍做修改，有的则是页面布局不同。按照内容和布局的区别，重复</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页可以归结为以下
4种类型。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">完全重复页面：如果两个页面的内容和布局格式毫无差别，则称这</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">种页面为完全重复页面。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容重复页面：如果两个页面的内容相同，但是布局格式不同，则</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">称这种页面为内容重复页面。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">布局重复页面：如果两个页面有部分重要的内容相同，并且布局格</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式相同，则称这种页面为布局重复页面。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">部分重复页面：如果两个页面有部分重要的内容相同，但是布局格</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">式不同，则称这种页面为部分重复页面。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">所谓“近似重复网页发现”，就是通过技术手段快速全面发现这些重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">复信息。快速准确地发现这些相似的网页已经成为提高搜索引擎服务</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">质量的关键技术之一。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复网页识别对搜索引擎的价值</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">识别完全相同或者近似重复网页对搜索引擎有很多价值。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">节省存储空间。如果系统能够找出这些重复网页并将它们从自有数</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">据库中去掉，就能够节省部分存储空间，进而可以利用这部分空间存</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">放更多的有效网页内容。同时因为重复项的减少，也提升了搜索引擎</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的搜索质量和用户的搜索体验。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">提升抓取效率。如果系统能够通过对以往信息的分析，提前发现重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">复网页，在以后的网页抓取过程中就可以避开这些重复网页，从而提</span>

---

## Page 80

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">高网页的抓取效率。如某个站点的内容原创度不足，则可以忽略对其</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">站点的抓取。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">辅助计算权重。如果某个网页被镜像的次数较多，说明该网页的内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容相对重要且受欢迎。如果一个站点中的大部分网页内容都比较重</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">要，那么在抓取网页时，应给予这个站点更高的权重和优先级。搜索</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">引擎在响应用户的检索请求并对输出结果排序时，也会参考该站点的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">权重。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">保证内容可用。从客户端的角度来看，如果两个页面是相同的，在</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其中一个页面访问异常时，可以引导用户查看另一个内容相同页面，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这样可以有效地提升用户的检索体验。借助端与云的协同，使用近似</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复网页发现，可以改善搜索引擎系统的服务质量。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一般来讲，网页去重的工作还在页面抓取，网页记录到已抓取网页库</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">的阶段完成。当网页爬虫抓取到网页时，如果判断是重复网页，则对</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">其进行去重处理，如果判断为新的内容，则将其入库。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复网页识别思路</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复网页的识别思路是比较容易理解的，主要分为以下
4个步骤。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对已有的网页文档进行特征提取，生成n个特征。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对新入库的网页文档进行特征提取，生成m个特征。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对两个网页文档的特征进行对比，确定相同的特征个数s。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">m=n=s则是相同网页，否则取s/max（m，n）来确定相似率。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">上述
4个步骤中最关键的是特征提取，关于特征提取的细节不在本书讨</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">论范围内，感兴趣的读者可自行查阅相关的资料。在实际的工程实施</span>

---

## Page 81

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">过程中，不仅需要考虑算法，还需要看效率、效果及对搜索引擎的策</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">略匹配度。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">重复网页识别算法</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这里主要介绍几个与重复网页识别相关的算法，仅作为指引，帮助读</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">者知道有这些算法和这些算法可以解决的问题，不作深入探讨。在实</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">际应用中，算法实现会比介绍的复杂。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1）MD5算法：分别对两个页面内容（主要是页面中的文字内容（非脚</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">本））生成一个哈希值，再对两个页面的哈希值进行比较，只要哈希</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值相同就说明文档完全相同，MD5算法主要识别完全相同的两个页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">面，相当于m=1、n=1，当s=1时相同，s=0时不相同。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">2）I-Match算法：I-Match算法有一个基本的假设，即不经常出现的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">词（低频词）和经常出现的词（高频词）不会影响文档的语义，所以</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">这些词是可以去掉的，这就相当于比赛评分时要去掉一个最高分和最</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">低分后，再算总分一样。I-Match算法先抓取页面内容进行分词，去掉</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一些高频词和低频词后，再对剩余的词应用哈希函数进行计算，当两</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">个页面剩余的词的哈希值相同时，说明这两个网页相同。I-Match算法</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">与MD5算法有些相似，只是针对一些不敏感的词进行了优化，更聚焦</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容的本身，也相当于m=1、n=1，当s=1时相同，s=0时不相同。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3）K-Shingling算法：分别生成两个页面的特征，再对两个页面的特</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">征进行相似度的计算。特征提取主要分为三步。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">a）抓取网页文档中的关键内容并进行分词；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">b）将关键内容拆分为由K个连续的词组成的特征组集合；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">c）将相同的特征组集合去重。</span>

---

## Page 82

<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">之后再取两个页面的相同特征数除以两个页面的特征并集（取两个集</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">合所有的元素），即s/（m</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">U</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">n），得出两个页面的相似度。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">4）SimHash算法：首先对页面内容进行分词，即将文本拆分成一系</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">列的词（或短语）。然后，对每个词应用哈希函数，得到一个固定长</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">度的二进制哈希值。之后将这些哈希值转化为特征向量，第一位中的</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">值为
1则映射为+1，为
0则映射为-1。在得到每个词的特征向量后，算</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">法可根据词在文档中的重要性（通常基于词频、TF-IDF等方法）为其</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">分配权重。然后，这些加权后的特征向量会被累加，形成一个表示全</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">文的主向量。接下来，主向量中的每个分量会被符号化处理，将分量</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">大于或等于
0时映射为1；小于
0时则将映射为0。这样，我们就得到了</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">一个由0和
1组成的SimHash值。最后，计算两个页面SimHash值的海</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">明距离。如果海明距离大于系统设定的阀值，则认为它们不相似；反</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">之，则认为它们相似。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">3.2.3</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">内容的存储</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">当海量的网页数据被网页爬虫技术抓取到已抓取网页库中时，下一步</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">就是把这些数据格式化、索引化，以实现更高的检索效率，这个过程</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">决定了哪些内容可以被用户优先检索到。</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">1.</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页的存储</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页内容在爬虫抓取后存储在搜索引擎的已抓取网页库中，存储的信</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">息包含网页的URL、页面内容、大小、最后更新时间、源码等。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页的URL：主要用于当某个网页被检索到并在结果页中展现时，</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">用户点击该条目，客户端打开这个网页的URL进入落地页以便用户查</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">看详细的内容；在安全干预机制对一些特定的站点进行降级时，也可</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">以通过URL信息进行站点匹配。</span>

---

## Page 83

<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">页面的内容：主要是页面的标题及正文中的文本、图片、视频等内</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">容，基于文本的内容，可以建立文本搜索的索引，以支持文本搜索；</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">对于图片、视频等内容，系统可以对它们进行分类、提取特征、关联</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">文本内容等，使其同样支持被检索。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">网页大小：主要包含这个页面的主文档（HTML文件）大小，以及</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">打开这个文档加载资源的总大小，这些内容可以帮助后续的排序决</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">策，比如相近的检索匹配度的情况下，在用户为移动网络时超大网页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">在结果中排序偏后，或者在结果页中提示页面大小，使用户在打开页</span>
<span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000">面时，对移动网络资费的消耗及页面加载的等待时间情况有预期。</span>
<span style="font-family:'SegoeUISymbol';font-size:14.83pt;color:#000000">❑</span><span style="font-family:'Unnamed-T3';font-size:14.83pt;color:#000000"> </span><sp