## Page 1

华中科技大学出版社Pearson Education（培生教育出版集团）

深度探索
C++对象模型
InsideTheC++ObjectModel

StanleyB.Lippman著

侯捷译

Object Lessons
· The Semantics of Constructors
•The Semantics of Data
• The Semantics of Function
• Semantics of Construction,Destruction,and Copy
Runtime Semantics
• On the Cusp of the Object Model

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p001_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 2

深度探索
C++对象模型
Inside The C + + Object Model

Stanley B. Lippmar
侯捷译

华中科技大学出版社
PearsonEducation（培生教育出版集团）

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p002_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 3

深度探C++对象模型
Inside The C++ 0bject lloxel
Staeley B. Lipptas

Sinplified Chinese Coprright 20e1 by lluss
Elucat ion Sorth Asie Linited.
311 rights Reserred,
Rub1ished by sermange

版权所有，翻印必究。
本书封面贴有华中科技大学出版社（原华中理工大学出版社）激光防伪标签，无标签
者不得销售，
图书在版编目(CIP) 数据
&求 C+对象膜壁 / (炎) Stenlc  著、候捷通
式汉;毕中科技人学出航社,2001.5
158N 7560924182/TP • 427
1. B-
11. 0) S- @
111. 前向时象适完, C
IV. TP312
声任编别：μ英
出版发行:毕中科技大学出版社武岛唯家山
hsttp://gress. hust, edu. ce
经销：新华书店湖北发行所
录排：华中科技大学惠友科数文印中心
的别：泥北省新华印刷
开本： 787× 1092 1/16  印账： 22.5字截：350 000
躯次: 2001 年 5 月第 1 般即次：2001 年6月第2 次印刷
印数: 5 00111 000
定价：54.00元

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p003_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 4

本立道生(候捷洋序

本立道生
（侯捷译序）

对于传统的结构化（seqamial）语言，我们向来没有大多的疑惑，虽然在面
数调用的背后，也有着维找建立，参数弹列，返目地址、堆找通除等等事后机划。
但函数调用是那么地自然西明显，好像只是炎带着一个包案，从程序的某一-个地
点观到另一个地点去执行
但是对于面向对象（Otet Okiead）语言，我们的疑感就多了，完其国，这
种语言的编评器为我们（程序员）做了文多的服务：构造通数，解构函数，虚拟
函数、继承、多态…-有时候它为我们合成出一些额外的函数（或运算符）。
时候它又扩张我们所写的函数内容，放进更多的操查，有时能它还会为我目的
cdtjects添拍加醋，放速一些奇妙的东西，使你面对sinvef 的结果大惊失色
存在我心里头一真有个疑感：让算机程所最基础的形式，总是脱离不了一行
行的据序执行膜式，为付么00（面向对象）语言细能够“自动完成”这么多事货
呢1另一个疑感是，成力强大的 polymobim（多态），其底是机制究竞如何？

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p004_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 5

深度N素C++对单根型

如果不了解编译器对我们断写的C++代码像了什么手期，这些图感水远
本开
这本书解决了过去令我百思不解的诸多疑感，我要向所有已具备C++多号
程序设计经验的间好们大力推荐这本书
这本书网时也是跃向组件软件（compem-are）基本精神的跳板，不管你担
] COM (Composent Object Model) IR CORBA (Common Objet Request Brokie
Architecture) 。 或是 SOM (System Ohjest Medel) ,T部 C++ Otject Model. #
使作更清楚软作组件（oompoemts）设计上的难点与应用之通，不但我自己在学
习COM的道路上有此强烈的感受，Esmiel COM（COM本质论，候提弹，
睡 1998) 的作者 Don Bex 也在他的书中推素 Lippman 的这本卓避的各糖
是的。这当然不会是一本轻松的书题，某在章节（例如3、4两章)可能给作
立即的享受——享受于面对座层机制有所体会与享控的快乐：某些章节（例如5、
6、7三章）可能带给你短暂的痛者—苦于观难保霍，难以香哺的内容，这些
快系与痛苦，其实就是我翻详此书时的心情写思，无论如何，我希望透过我的译笔
把这本难得的好书都到更多人面前，引领大家见识C++庭层构造的技术之类
快捷2001.03.20于新
13ho9l1jhou con

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p005_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 6

本立道生（保捷洋序）

请注意：本书特点，作者Lippman在其前言中有很详细的握述，我不再多言。
翻语用词与心得，记录在第0章（语者的适）之中，对您或有导读之功。
请注意：原文本有大大小小约80-90个笔误，有的无伤大雅，有的对周读顺
畅影响基区（如前后文所用符号不一致、内文与图形所用符号不一致——甚至因
面导致图片的文字解释不正确），我已在第0章（译者的话）列出我找到的所有
错误，此外，某些场合我还会在错误出现之处再加注，表示原文内容为何，这么
做不是画蛇添足，也不为彰显什么，我知道有些读者会章看原文书和中评书对照
看看，我肥原书错误加性出来，可免读者怀疑是否我打错字或是评错了，另一方
面生是为了文责自负.....万Lipman是对的西LJ.Hee 错了呢我
层有相当把星，还是希望明白用开来让读者检松

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p006_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 7

前言
(Stanley B. Lippman)

差不多有 10 年之久，我在贝尔实验室（BellLabortoris）理百于 C++的实
现任务，最初的工作是在 cfoet 上面（Bjumc Streustup 的第个 C++ 编译
器），从 1986 年的 1.1 原则 1991 年9 月的 3.0版，然后移转到 Simnpifer （这是
我们内部的命名），也就是Foundtian 项日中的 C++对象模型部分。在
Simplifer 设计能词，我开始断醒这本书
Feundsien 项目是什么？在 Bjarnme 的领导下。且尔实验重中的一个小组探
索看以C=+完成大规模程序设计时的种种问题的解决之道，Foundaioe 项目是
我们为了构造大系扰面努力定又的一个新的开发模型：我们只使用C+，并不提
供多重语育的解决方案，这是个令人兴奋的工作，一方面是因为工作本身，一方
亚是因为工作伙争: Bjarse. Andy Koenig, Reb Mamy, Martis Camoll Judy Wrd.
Sheve Berf Peer hal, 以及我自己, Baha Mse 管理我们这群人 (Bjrse 和
Andy 除外），Barhars Moo 需说管理一个软件团队，就像放教一群骄重的期

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p007_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 8

深度指董 C++ 3象提型 (uele The C++ Ohyrcr Model)
我们肥 Foundrion 思象成一个核心，在那上面，其它人可以为使用者辅设一
显真正的开发环境,把它整据为他们所期望的 UNIX 或 Srmallak 模型。 私底下
我们把它称为Grsl（传说中耶账最后的晚餐所用的圣杯），人人都想要，但是采
来没人我到过！
Gnil 使用一个由 Rob Mamy 发展出案并会名为 ALF 的面向对象层次结
构，提供一个永久的、以语意为基础的表现法。在Gnil中。传统编译器被分解
为数个各自分离的可执行文件。parer 负责建立程序的 ALF 表现续，其它每一
个组件(比如 type checking sinplifkation、 code gosentioe) L及工具(比如
browser）都在程序的一个ALF 表现体上操作（并可能加以扩展）。 Simpier 是
编译器的部分，处于tpechecking 和 codegmerntion 之间， Sinpifer这个名
称是病 Bjare 所省议的, 它原本是 cfhont 的个阶段 (ghase) ,
在 tpe checking 程 code geion 之间，Sinplifer 数么事呢！它用来转
换内那的程序表观，有三种转换风殊是任何对象模型都需要的

这是与特定编语器有关的转换，在ALF 之下，这意味着我们所谓的
“aaive” sodes. 例如，当 parser 看到这个表达式：
fet [D) r
它井不知道是否 ()这是一个函数调用推疗，或者(b)这是cverloaded cal
operntar 在 chas otject,fer 上的一种应用.默认博况下，这个式子所代表的是一个
函数调用，但是当(b） 的情况出现时，Simplifer 就要重写并调换cal nbree
2. 语言语意转损 (Languagt semastics transformations)
这包括cestrsctorlaestctor的合或和扩展，memerwise 初始化，对于
erwistecopy 的文持、在程序代码中安植 ceevernion spenors..临时性对象
以及对 oostructeidestructor 的调用。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p008_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 9

3. 程序代码和对象模型的转换 ( Code and ebject r
这包斯对 vinual fenctiots, virual base class 起 isheriance 的般文持。 sr
和 dele 运算荐, cdas otjocts 所组或的数组。 local satic clasinstanceoes，董有非
营量表达式 （soncomitant oxpresxin）之 global otject 的静态初始化操作。我对
Sinpiler 所规划的一个目标是：提供一个对象膜型体系，在其中，对象的实现是
一个虚损提口，支持各种对象模型
最后两种类型的转换构成了本书的基础，这意味着本书是为编详器设计者面
写的吗？不是，绝对不是！这本书是由一位编译器设计者针对中高眼C+程序
员所写的，隐藏在这本书背后的假设是，程序员如果了解C++对象模型，就可
以写出比校设有错误频病而且比校有效率的代码。
什么是C++对象模型
有两个吸念可以解释C++对象模型
1.语言中直接支持面向对象程序设计的部分
2.对于各种支持的庭层实现机制
语言层图的支持，量于我的C++Pier一书以及其它诗多C++书糖当
中，至于第二个念，则儿乎不能够于H前任何读物中发现，只有[ELLIS90]和
[STROUP96] 勉强有一些练丝马连。本书主要专注于C++对象模型的第二个概
念，本书语言遵确 C+*委员会于 1995冬季会议中通过的 Standand C++草案
（险了某些细节，这份章案应该能够反映出该语言的最终原本）。
C++对象膜型的第个氨念是一种“不变量”.例如，C+class的完整vinusl
funtistes 在编语时期就固定下来了，程序员授有办续在执行期动态增加成取代其
中某一个，这使得虚拟调用操作得以L有慢速的图透（diganch）结果，付出的成本
则是执行期的弹性。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p009_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 10

探度探常 C++ 对象提型 (ide 7e C++ Obyecr Modn/)
对象模型的座层实现机制，在语言层面上是看不出来的一显然对象模型
逐意本身可以使得某些实现品（编泽器）比其它实成是更接近自然，例如，vitul
hction cals,一般面言是通过个表格 (内含 vintual fuenctioms 地址) 的素引R
决议得知。一定要使用如此的 vital tabke 吗1不，编语器可以自由引进其它任
例交通激础，如果使用vinaltobe，那么其有局、存取方法、产生时机以及数百
个细节也都必须决定下来，图所有决定生都由每一个实现品（编译器）自行取舍
不过，既然说列这里，我出必须明确告诉你。目前所有编译器对于 vital fancicn
的实成法都是使用各个clas 专属的vinal ate.大小固定，并且在程序执行前
就构造好了。
如果C++对象模型的底屋机制并末标准化，那么你可能会间：呵必探讨它
呢？主要的理由是，我的经验告诉我，如果一个程序员了解底层实成模型，地就
能都写出致率较高的代码，自信心也比校高，一个人不应该用猪的方式，或是等
某大师的宜判，才确定“何时提供一个copycomrweter 面何时不需要”。这类
向题的解答应该亲自于我们白身对于对象模型的了解
写这本书的第二个理由是为了消除我们对于C+语言（及其对面向对象的
支持）的各种错误认识，下面一段适节录自我收到的一封值，亲信老希望将C++
引造其程序环境中：
我和一群人一起工作，他们过去不曾写过（或完全不熟悉）C++和O0.其
中一位工程师从 1985 年就开始写 C了，性强烈地认为 C+只对那查wm-p
程序才好用，对servr程序部不理想，他说如果要写一个快速而有效率的数据率
引掌，应该使用C药非C++.单认为C++建大叉辽
C++当然并不是天生地康大叉迟缓，但我发现这似平成为C程序员的一个
共识，然图，先是这么设并不是以使人值服，何况我又被认为是C+*的“代言
人”。这本书就是金图极尽可能地将各式各样的Otjet Iicilis （如 izher

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p010_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 11

言
除了我个人目答这封售，我生把此信转资始 HP 公司的 Steve
我曾与他时论过C++的效事间题，以下节录自他的响应：
过去数年我听过大多与你的间事类似的看接，许多情况下，这些看出是国于
对C++事实真象的肤乏了解，就在上周，我才和一位期发闲聊，他在一家X制
造厂服务，他说他们不使用C++，因为“它在作的背后值事情”，我继续递问。
于是他说果据他的了解，C++ 调用 malloc) 和 e) 而不让程序是知道。
然不是真的，这是一种所得的迷思与特误，引导出类似于你的同事的看法
在独象性和实际性之网找出平需点，需要知识，经验，以及许多思考，C++
使用需要付出件多心力，但是我的经验告诉我，这项投资的限酬率相为高。
我喜欢把这本书想象成我对率一封读者来信的同答，是的，这本书是一个知
识莱列库，帮量大家去除围绕在C++四周的速思与传说
如果C++对象模型的庭层机制会因为实现品（编洋器）和时间的变动图不
同，我如何能够对于任何静定主题植供一般化的讨论呢！静态机始化（Suric
icn）可为此我供一个有运的树子。
已知个 cas.X 有载 cerstrutor, 像这样
eiass x

private:*ptr:

而个 claas X 的 global otjeu 的声明，像这排

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p011_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 12

聚度R C++ 对量根型 (ile 7e C++ Oyer.,
X buf;
nais13
/ but必强在这个时教构欧起来

C++对象模型保证，X censtructor 将在mk) 之前便把 h 折始化，然而
它并授有说明这是如何办到的。答案是所谱的静态析始化（snie imializarion）
实际做法则有糖开发环境对此的支持属于哪一层级。
原始的cboet实现品不单只是很想没有环境支持，它生银想设有明确的目标
平台，唯一能够数想的平台就是LNIX 及其衍化的一热变体，我们的解决之通也
固此只专性在 UNIX 身上：我们使用 nmm 命令。 CC 食(一个 UINIX shel
scripg) 严生出一个可执行文件，然后我们把 sm随行于其上，产生出一个新的c
文特。然后编语这个新的文件，再重新链损出一个可执行文件（这就是所课
的munch solkrion）。这种循法是以编译器时间来文换移植性
接下亲是提供一个“平台特定”解决之适：直模验证并穿越COFF-based程
序的可执行文件（即所谓的puch sohutioe)，不再需要nm. compile reirk.COFF
是 Common Otjet File Fomt 的指写, 是 Syitem V pre-Rclasc 4 UNIX 系境所
发展出来的格式，这两种解决方案都属于程序层图，也就是说，针对每一个需要
静态初始化的文件，choet 会产生出个a通数。执行必要的初始化操作。
不论是 petch solution 或是 munch solrioe,都会去寻我以L mi 开头的函数,并L
安排它们以一种未被定文的次序扶行起来（经曲安猫在maie)之后第一行的一
个 library finctiee_maing 执行之)(请性：本书第6 章对此有讲细说明)
Synicm V COFF-spocific C++ 编评器与 cfom 的各个服本平行发展。由于相
准了一个特定平台和特定操作系统，该编译器因面能都影响链接器特地为它修
改：产生出一个新的inisoctie，用以收集需要费态析始化的otbjocts，链接器的

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p012_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 13

前吉
这种扩充方式，提供了所请的envi.都当数更在
cltion 星次之上
至此。 任何以 cfoet pogmam-based sohution 为基础的般化 （近型) 操作特
令人速感。为什会？因为C++已经成为主值婚言，它已经模收了更多更多的
emcnt-bascd soluties-这本书如例维护其间的平衡呢1我的巢略如下：如莱
在不同的C++编译器上有重大的实现技术差异，我就至少讨论两种嫩出，但如
果chot之后的编译器实现模型只是解决chmnl原本就已理解的问题，例如对
虚损继承的支持，那么我就用述历史的澳化，当我说到“传境模型”时，我的意
思是Soroustrp 的原始构想（反映在con 身上）。它提供一种实现模式，在今
天所有的商业化实成品上仍然可见，
本书组织
第1章，关于对象（Object Lesmems）。提供以对象为基础的现念背景，以及
由 C++提供的面向对象程序设计典范（paradigm.请注：关于 parndigm 这个字
请参阅本书22页的评注），本章包括对于对象模型的一个大略则笼，说明目前
普及的工业产品，但没有对多重是承和盘拟继承有大靠近的现察（那是第3章和
第4章的重头戏)。
第2章，构造函数语意学（The Semantics of Cestructes）。详细讨论
construcor 如例工作，本章设到 constrctkors 何时被编译器合成。以及给你的程
序效享带集付么样的意文
第3章至第5章是本书的重要题材。在这里，我详细地时论了C++对象模
型的细节, 第 3章, Data 语盘学(The Semastics ef Data) , i论 data membes 的
处理，第 4章，Fusetios 语意学（The Sematics af Functise）。专注于各式各年
的 member fnctiomi.。 并特别评细地计论如何支持 vital famctioms. 第 5 章，将
造，解物。携贝语意学 (Sematios ef Ceestrectiss, Destrtiss,snd Copy)。 讨
论如何支持 can 模型，也讨论到otjoct 的生命期，每一章都有菌试程序以及测

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p013_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 14

聚皮指R C++ 3象R型 (uiale Tie C++ Oleer MovA
试数据。我们对效率的预测将拿末和实际组果做比较
第6章，执行别语意学（Rustime Semastics），检视执行期的某些对象模型
行为，包括病时对象的生命及其托亡，以及对 new 运算持和 dekte 运其养的支
持。
第1章，在对象模型的实增（Os the Cusp of the Objet Medl)，专注于
nception handig. template suppert. nstime type denifcatiee.
预定的读者
这本书可以扮演家庭教师的免色，不过官定位在中级以上的C+程序员。
到非C++新手，我尝试提供足够的内容，使它期够被任何有点C++基稻（例如
读过我的 C*+Primr 并有一些实际经验）的人接受，理想的读者是，曾经有过
数年的C++程序经验。希望进一步了解“底层做些什么事”的人。书中某些部
分甚至对于C+高手包具有吸引力，比如格时性对象的严生，以及named netsm
valie（NRV）优化的细节等等，在与本书素材相同的各个公开演谱场合中，我已
经证实了这些材料的吸引力。
程序范例及其执行
本书的醒序瓶例主要有两个目的：
1.为了提供书中所说的C++对象模型各种摄念的具体说明。
2.提供测试，以测量各种语言性质的相对成本
无论感一种意图，都只是为了展现对象模型，尊例而言，虽然我在书中有大
量的举例，但我并非建议一个真实的 3Dgraphik library必须以l虚和想承的方式来
表现个 3D 点（不过你可以在[POKOR94]中发现作者 Pakomy 的确在这么

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p014_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 15

董言

书中斯有的期试程序都查部 SGI Indigs2sL 上编谨执行, 使用 5GI 5.2
UINIX 操作系镜中的 CC 和 NCC 编评器 CC 是 cfoet 3.0.1 版(它全产生岛 C
码，再由一个 C 编译器重新编译为可执行文件) NCC 是 Edson Design Groug
的 C++ fom-nd 2.19 最,内含—个由 SGI 供应的程序代码产生器。 至于9时间别
量，是采用UNIX的 timox 命令针对一子万次造代测试质得的平均值
显然在dl机器上使用这两个编洋器，对读者面言可能觉得有杰神秘，我阳
觉得对本书的目的面言，很好。不论是chont 或现在的 E&isos Design Groups
C++ from-nd（Bjeme 称其为“cfrom 的儿子”)，都与平台无关，它们是种
一般化的编译器，被授权给 34家以上的计算机制造商（其中包括 Grny、SGI
Intel) 和较补开发环境厂商 (包括 Centerline 和 Novell, 后者是源先的 UNEX 软
件实验室），效率的期量并非为了对目前面上的各家编译系统进行评比，而只
是为了提供C++对象模型之各种特性的一个相对成本测量，至于商业评比的效
事数到，靠可以在几乎任何一本计算机杂志的计算机产品检验损告中获得
数谢

参考书目

1993)iting Class Templttes", C++ Beport (Septermbc

ing C++ Cor

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p015_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 16

深度保家C*+对象原型(I

gs, Portlasd, OR(1992)
M ef Class Tet

ing, Part 1°, The
[CL  ation (Junc 1994)ew & Delete", C++ Reporn (Ma)
ings & Endings", C+* Repert (Septcmber
[ELL

2++ OI  Usenix C++ Confe

H  n, Cay S., °C== Car
[KCC++F  ring Virteal Tables ir
KC
[KO  Coesig, Andrew, Cenbining C and C+*”, C++ Repart (laly/Aagus
[ISOH  tienal Standard, Draft(April 28, 1995)

L
and, OR(1992
nce Procters to Class
L, VoL.1, No.4 (1991)  . C+

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p016_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 17

, Stanley. C*+ Primer
mnley. *Defaslt Caasts
*Applying The Copy Cot
April 1994)
anley. °Objecand Datare”, C=+ R

: 3. and Roe Zahavi, The Es

19z] Pilay

‘, C++  pril 1994)
Schyar

MA(1994

tes, Todd, “Using C++ Template Metap
if CORBA°, C++ Repot
re, °Mapping CORBA IDI. into C*+*, C++ Report (Septem

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p017_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 18

第0章导续 (证老的运

第0章

导读
（译者的话）

合适的读者
崔不容易三言两语就说明成书的适当读者，作者Lipman参与设计了全世界
第一套C++编译器dhont.这本书就是一位伟大的C+*编语器设计者在向你用
述他如何处理各种eoplict （明白出现于 C++程序代码）如 inplict （隐藏于程
序代药背后）的C+语意.
对于C++程序老手，这必然是一本让你大呼过瘤的地妙好节。
C++老手分两类，一种人把活言用得巡熟，O0观念也有，另一种人不世如此。
还时于台面下的机制，如编译器合成的 defaslt cotuctoer 啦、otyet 的内存有局
等等有莫大的兴题，本客对于第二类老手的吸引力自不待言，至于第一类老于，
或许你没那么大的制服究底的兴题，不过我还是极力推靠你周读此书，了解C-+
对象模型，绝对有助于作在语言本身以及面向对象观念两方面的层次提升

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p018_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 19

聚度R C++ 对量根型 (le 7e C+ + Oywer AMor/)
你需要细细推慧每一个每子，每一个州子，图归看枣是完全没有用的，持者
是C++大师缓人物，并且参与开发了第一要C++编译器，他的解说以及让释确
实是糖群人里，你务必在看过每一小段之后，账会贯通，把地的思想或念化为心
有，两接续另一小节，但周读次序外不要要被照布中的章节排列
阅读实序
我个人认为，第1.3.4章验能带给读者迅速到且最大的帮助，这些都是经常
引起程序员图感的主题。作者在这些章节中有不少示意图（我自己生加了不少)
你或许可以从这三章执着看起。
其它章节比较闻湿一些（我的感党)，不防“覆可而择之”
当然，这都是十分主现的认定，客观的意见只有一个：你可以随你的兴是与
需求，从任一章开始看起，各章之同没有必然关联性。
翻译风格
大多那友告诉我，地们属读中文计算机书籍，不论是著作或评作，最大的阅
读图难在于一大唯授有标准译名的技术名词或写惯用语（至于那些误逐不知所云
的奇经作品当然本就不在考意之列），其实，就算国家相关机构有统一译名（或
曾有过，遗知速！），流通于工业界与学术界之间的还是原文名间与术语。
对于工程师，我希望我所写的书和我所谦的书能够让各位读来通体顺畅：对
于学生，我还希望多发那一点引导的力量，引导各位多使用、多认识源文术语和
专有名词，不要说应像一无模式对话盒“这种奇怪的话。
由于本书读者定位之放，我决定保暂大量的原文投术名词与术语，我请楚地
年道，在我们的技术领域里，张究人员成工程师如何被用这整语汇。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p019_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 20

第0章导读（洋者的活)

当然。有些中文译名较普及，也较贴谐，我并不排除便用，其间的携选与决
定，不可到免地带了点个人色彩，
下面是本书出现的源文名润（教字母排序）及其意文：

访问级.就是 C++的 pubic、 privite、 protsted 三种等级

aligr  边界调整，调整至某症bytes的信数，其继果视不同的机器而定、例如32位机额通常调整至4的售数
bisd  携定，将程序中的某个养号真正附着（决议）至一跌实体上
dhain  辛链
class
class hicr  chas 体系，clas 显次结构
组合。通意与继承（inhriance）一同讨论
具体继承（相对于抽象增承）
构造函数
data
声明
define定义（通书您带一在内存中挖一块空同”的行为）
湿生
邮构函数
封装
明确的（通常指C+程序代码中明确出现的）
体系，星次结构
实现（动调）

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p020_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 21

尿度探素 C+* 对象模型 (Jnride The C++ Oher Mosdr)

mplicit隐含的、璀喻的（通常指未岛现在C+程序代码中的）
组承
nlis  内联（C++的一个天键润）
实体（有查书磨评力“案例”或实例，板不妥当）
布局，本书营常出现达个字，意指ctjen在内存中的数据分
布情况
名称切期重组（C→+对于函数名称的一种处理方式）
成员通数，亦或被称为fscion member
成员, 泛指 dsea mcmsbers 和 nemberfnctioes
bbjec  对象（根据cas的声明而完成的一份占有内存的实体）
编移位置
换价数
运算符
额外负担（因某种设计，面导致的额外成本）
重收函数
改写 （对 vintal fnction 的重新设计)
典范（请参考422页）
指针
多态（“面向对象”录重要的一个性质）
程序设计、程序化
参考、参用（动词）
C++的&运算养所代表的东西，当作名词解
决议，函数调用时随接器所进行的一种操作，将符号与函数
实体产生关联，如果你调用tunc0面随接时找不到func)
实体，就会出院“umeslved catmb”链接误
表格中的一格（一个元案）：条孔：条目：条格

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p021_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 22

第0章导洪 (评者的话)

子类型

ifut
虚拟团数
rirtual inher虚拟继承
vitual table虚拟表格（为实现虚拟机制而设计的一种表格，内放vinu
snctions 的地址)
有时候考意到上下文的圆素，面对间一个名闻，在译与不详之间，我可能会
有不同的选择，例如，面对“pointaer”，我会语为“指针”，但由于我并末将rcfrnc
承为鲁考”（实在不对味），所以如果原文是“tc manipultion of a peiter
cloremceisC+*.，为了中英对等或平衡的，我不会把它译为C++中对
于指和rferce 的操作行为……，我会译为"C+*中对于poister 和

译注
书中有一些详性，大部分评注，如果够短的话，会被费直独放在暂号之中，
模续本文，较长的谦注，则被我安排在被注文字的股客下面（累临，并加标示）。
原书错误
这本书强说质地根佳，制作的严谨度年不及格：有提Lipman的大师地位。
属于一作者笔误”之类的错误，比校无伤大雅，例如少了一个：符号，波是
多了一个：养号，或是少了一个}符号，或是多了一个）特号等等，比较严重
的错误，是程序代码变量名称成函数名称或clas名称与文字额述不一致，甚成
是图片中对于cben布周的疆按，与程序代码中的声明不一致，这两种错误都会
严重耗费读者的心神

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p022_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 23

探度强素 C++ 对象浆型 (Juise Tle C++ Objner Mod
只要是被我发现的错误，都已被我修正。以下是错误更正列表。
示例：L5表示第5行，L-9表示数第9行，页碱所示为原书页码。
p.35最后一行Bahhl)  Bethl0:
P.F7表格第二行1.32.36  1:32.36
p6I L1  mxmcpy_程序代码最后少了一个)
p61 L10
p64 L-9  程序代码最后多了一个：
p.78最后四行码  组乎应为=
p.84图 3.1b 说明stret Point3d  claes Pointd
p.87L2  vintual.程序代码最后少了一个;
p.87全页多处  px2,2 （不符合命名意文)  pc1_2 （辨合命名意义)
p.90图 3.2s 说明Vptr placement and end of classVptr placnet  end et claeo
p.9181.23)  _p_m,yrt  _pe_has_vits
p.92码 L-7  clas Vente2d  clas Vete3d
p.92码 L-6  pubie Pin2d  Nic hicd
p.93图3.4说明Vertex2d 的对象布局  Vermx34 的对象布局
93-  养号名称湿乳，前后误瀑不符已全部更改过
p.97码L2  配合图3.5，应调整次序为
pebli Vetex, publi Poistd
p.99图3.5(x)  养号与书中程序代码多处不养已全部更改过
p.100图3.5(b)  特号与书中程序代码多处不养已全邦更改过
p.100 L2  pv34+最后多了个)
p.106 L16  ptld:y  pQ#_y
p.1097 L10  & 34,_point:zx,
p.198 L6  & 34_point:z;  aNis04:z

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p023_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 24

第0章导读（证者的话)

p.108 1.6  Isn d:*dnp, d *gd  int Derived:*dmg, Derived *p
p.109 L1  d *pd  Deied*
p.09 14  int b2:*bmp = 8b2=val2;
p.110 12  不特合精早出现的程序代码  把 pdd 改% Pint0d
p.11s L1  mgsihd04)
p.126 LI2  pe * nev Pvintdt,
p.136 图42 右下Derived:-clase()  Derivedtlose()
p.138 L12  das Pein3d. 量后少个 (
p.140 程序代码  设有与文字中的 class 命名一致所有的 pG4 改为 Point34
p.142 L-7  程序代码最右造少
.140 程年代药没有与文字中的 dlas 非名一致所有的 pt3d 改为 Poiat
p.145 L6  peinter0  Rrint:30
p.47 L1  peinte:*pf  Poist:*pef
p.147 L5  peint30  Nit:s)
p.47 L6  peinta)  Point:30
p.47中段药 L-1程序代码最后缺少一个）
p.148 中段码 L1(ppmf) 函数量后少个 ;
p.148 中段码 L-1(pep[.函数最后少一个 )
p.150 程序代码没有与文宇中的 caus 命名一题所有的 p04 改为 Peint3d
p.15e L-7  p_vp_pd 量后少个 ;
p.152 14  point seu_R  Point sew_pt:
p.156 L7  1  1
p.160 L11,L12  itrnset_Base  Abstrset_bese
p.162 L-3  Abstet,baee 函数最后少一个 ;
p.166 中,  L3Point1 loall =  Poit locall =
p.166 中, 6 L4Point2 local2;  Pvit loal;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p024_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 25

深度据素 C++ 对象根型 (Juile TIle C++ Oiecr Moder)

p.174 中, 码 L-1 Liae:Lie0 函数最后多了个 ;
p.174 中下, 码 1-1 Lne:Line() 虽数量后多了个 :
p.17s 中上, 药 L-1 Lise=~Line() 通数最后多一个 ;
p.182 中下, 5 L6 Peis3e:Pein0d)  PVetec:PVerte()
p.183 上, 5 L9Peistd:Point3d()  PVetexPVene()
p.185 上,  13y=0.0 之指缺少 fet
p.86 中下, 码 L6 接少个 mtsm
p.187 中,  L3const Poin3d &p  cest Point3d &p3d
p.204 下,  L3缺少-个 msam 1;
p.208 中下, 码 L2 sew Pvertex;  ngv PVenec
p.219 上,  L1_nw(5*siaof(is()  _sew(5*sizsoffint);
p.22 上,  L8
p224 中,  L1Point2w ptw =  PeisCv *gtx *
p224 下,  L5openor aew) 函数定义事一个 ;
p225 上,  L2Psistw ptw * -  Poist2w *ptw *
P.226下,  L1Peinst2w p2v =  Poist2w *p2v =
p.229中.药L1c.operi(a+)  6.4p4(a + b )
p.232中下,  L2X.33;  Xx
p.232 中下,  L3xY  X
p.232下,  L2sret x_Ix;  stret X_1xx
p.232 F, 码 L3trut x_1y;  rnet X _1y,
p.233 县 12  eret x_0_Q1;  trnt X _0_Q1;
p.233 码 13  trst x _0._Q2;  stnet X _0_Q2;
p.233 中,码  条件句的最后多了一个：
p.253  L-1  foo()函数量后多了一个：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p025_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 26

第0章导读 (详者的适)

推荐
我个人翻译过不少书路，每一本都糖揽细选后才动手（品质不够的原文书
逐它做地1，在这么些谦本当中，我队来不做直模而露骨的推界，好的书语白
然面然会得到识老的欣實，过去我详的那些明显具有实用价值的书糖，总有相当
数量的读者有强烈的需求，所以我从不担心投有足够的人来为好书教播口碑担
Lipmn的这本书不一样，它可能不会为你零来明是周立即的实用性，它可能因
此在书肆中蒙上一层寒（其原文书我就没听混多少人读过），红费我风众多原文
书中挑出这本好书。我担心听期这样的话
对象模型？呵，我会写C++粒序，写得一级棒，这费属于编详答层面的东
西，于我例有收！
对象模型是探层蜡构的加识，关系到一与语言无美，与平台无关，前网络可执
行”软特组件（efwar componem）的基磁源理。出因此。了解 C对象原型,
是学习目前较件组件三大现格（COM.CORBA.SOM）的技术基础。
如果你对软件组件（sowaecmpo）没有兴题，C++对象模型也能够使
你对虚拟通数、虚拟继承，查报膜口有股胎换骨的断认识，成是对于各种C+写
继新誉案的教率利益有通盘的认识
我因此要大声地说：有经验的C++programmer都应该看看这本书
如果他对COM有兴睡，我也要风时准荐作看另一本书：Emtlal COM
Des Bex 著，Addiaee Wesley 1998 出服（COM本质论，侯捷语，基峰1998)
这也是一本论述非靠清整的书路，把COM的由案（为什么需要COM.如何使
用COM）以道序渐进的方式用述得非营探刻，是我所看过最理想的一本COM基
磁籍
看Esdal COM之前，你最好有这本 Iite The C++ Oyer Modd 的基础

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p026_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 27

<span style="font-size:37.50pt;color:#0000ff"> { — ˚ ç  Ô  s &lt; œ </span>

<span style="font-size:28.12pt;color:#000000">ì {2!&gt;  hÚ  tX+ h tÿìUìY&#x27;qŁ\ÿÔ; h t ô! lF&#x27;</span>
<span style="font-size:28.12pt;color:#000000"> r  Ł¸&amp;ì ·ÿp¨Mä hru,ÿì pQ&#x27;Ip»ÿqÓ{ h2%81˛ X</span>
<span style="font-size:28.12pt;color:#000000">+</span>
<span style="font-size:28.12pt;color:#000000"> § »ö lÓ« ·˛ììÿ&amp;ö÷ì O u÷Ñr: hØ Ò[v hßO- ˛[r: h{òVŒ</span>
<span style="font-size:28.12pt;color:#000000">I2ìÞ ßO-ÿ ˛[  ! . 3+/ Ø Ò[vK [v=l ‹{	{2 2Cÿ{IÓ!¢‘ÿÕÞ</span>
<span style="font-size:28.12pt;color:#000000">ìk ˛ À t!I »ÿqÓ{ hì  F O L  @ K J A P  I B ?  R &gt;ÿ&quot;\î ÀI   ‡Ò  ˚ÖNâì</span>
<span style="font-size:28.12pt;color:#000000">Þ»Wì F O L @ K J A P À  ïfﬂÿ_  w ı ý  t»</span>

<span style="font-size:28.12pt;color:#000000"> r˛ ÿ ÆH{2 § \t  r  !    tb&lt; §  h1ü + $ !  tb §  {Ò h</span>

<span style="font-size:28.12pt;color:#000000"> ÿwì&#x27;ì = O L  J A P*ì  ¨y Ü!  J A P  F  A A  L D L §   hÚh4 ¶N_ðI   h r</span>
<span style="font-size:28.12pt;color:#000000">ÿe  v yY u ·ìÿ O M H O A N R A N  ? O O  = F = T v ß[ &gt; hH tßìW Ö ø| l</span>
<span style="font-size:28.12pt;color:#000000">H tßW”ª I ¸y  ˇˇ»+ rW r §  ˇˇ&#x27; æ?  ì’ æ  q ?Ú æ</span>
<span style="font-size:28.12pt;color:#000000">I ?ì! &#x27;) h § Jh I &#x27;)Þs  ¥ÿxŁ ð Ó Ób   r kk ‡à!e</span>
<span style="font-size:28.12pt;color:#000000"> ÆÚh«  J A PìÞ˘  hÆ F  A Aÿs7 hÆ = L E{2   hØ Ò[v%‡[ y4ì </span>
<span style="font-size:28.12pt;color:#000000">ÿ¢ ı  rŁ¸&amp;ì ·ÿMäI »ÿqÓ{! ˛ À</span>

<span style="font-size:28.12pt;color:#000000">´¨  J A P F = R =ÿN_? tØð!|{ Ø Ò[vI   ˚ tÕÞI »!f˛»ÿ h   Óh</span>

<span style="font-size:28.12pt;color:#000000"> ý¨ t ý!¨ h&#x27; I B ?  J A Pp‹Ù ¶Ö… § k æÞ  hfÿ Ú h ˚ »kÜ t  h! &#x27;</span>
<span style="font-size:28.12pt;color:#000000"> ràkà t!I»Þ  tbïÿVŒ tbÿV &#x27;ì  J A Pÿ Æ9 ÀÆØe Ù ¶</span>
<span style="font-size:28.12pt;color:#000000">÷ t÷ð  ¥ür ð  tb  §  l tâð   Ó&#x27;˚N_  Ó tbïÿVŒ  ÓkÜ t</span>
<span style="font-size:28.12pt;color:#000000">  h ˚ ýßE÷ðÿ h t¨ ¥N_ï&lt;	 h ˚Ó&#x27; ÜïÿVŒyWð Àfÿ Ú</span>

<span style="font-size:28.12pt;color:#000000">Ó{ ¾’ÿ –wJÓßÿ» hß&gt;÷Ñr:Oìÿ tÞ hx 2Cÿ{J˘ ¾8 xJ</span>

<span style="font-size:28.12pt;color:#000000"> ‹SD W`uUÿ÷Ñr:ß&gt; t ÜØ Ò[v hxJ ÜI N_’ þÿVŒ ï ·k» Ø Ò[v</span>
<span style="font-size:28.12pt;color:#000000">ÿ t Ü|{ h{òVŒ hx b2CE˘Ó¸Ó¸Ø h t{˚òÿ –</span>

<span style="font-size:28.12pt;color:#000000">ß&gt;!ìÞ»ÖNâWì  J A P h F = R =I #by ÀÚh#s+ hï ·[ÿ&#x27;,—¸  h w t ˚</span>

<span style="font-size:28.12pt;color:#000000">&#x27;þ h  q h I R ?  K N II A: h Óh Àð  t ý  § ð hÚlpÀ hÚìÞ Ó«</span>
<span style="font-size:28.12pt;color:#000000">‹{	 h4 ¶’2 hŒß^RI »ìÞ WI k ‡Ö tbE pb</span>

<span style="font-size:28.12pt;color:#000000">˛ tÓ˛þxæþ&#x27;,P  , # 2 6 + *II&#x27;,⁄¨ß ¥ hkß&gt;,ÿ» tß h ì&#x27;</span>

<span style="font-size:28.12pt;color:#000000"> pƒp˘ÖJ hh!&amp; h t!Kÿ“ÿI þ¸ÿ&#x27;,Û Ł¸FÿVŒ hÓ f</span>
<span style="font-size:28.12pt;color:#000000">ÿì &#x27;,H s C hßßÿ«»ïìÞ h! I » hÓÕÞe þ&#x27;, f </span>

<span style="font-size:28.12pt;color:#000000">Nâ{2E÷ òì !  !   [v = L E{2 h  ï ·ØFF hì  ﬂ ·Æ¥ZVŒìÞ  ?  !  </span>

<span style="font-size:28.12pt;color:#000000">{2» hWì  I B ?   J A PI  Às757 h wf.ß äŒì  4  {2§Ó    hì »2</span>
<span style="font-size:28.12pt;color:#000000"> 4 !ÿ2CŒß§Ó &gt;e ß&gt; Ó òì 4   h4ì y h t À  hH &gt; h^C4 hß&gt; òì </span>
<span style="font-size:28.12pt;color:#000000">y h4ì 4   h Ł Óh§Ó  ’ìh“9¸k</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 28

<span style="font-size:28.12pt;color:#000000">{2Þsk- hß&gt;ì   J A P  I B ?  R &gt;I þ ˛ÿøÞ’ þ˚- ‰R 	 h»ÿì - hq</span>

<span style="font-size:28.12pt;color:#000000">‰R- tß tk ˚ÿf¸ ¥ Àà ﬁ  ´ ¯Æb § k!ôô h¯b</span>
<span style="font-size:28.12pt;color:#000000">ôÿ¸‹4¯h/ßk-l hô¾1 o yNâk Õ= ÷Õ âÿI¢Uÿ</span>
<span style="font-size:28.12pt;color:#000000">- h}À¸˙œ¯Y. rk  H\ ´Óÿ  p˚.-Ñ] H&#x27; y hn k˘k‹ÿi</span>
<span style="font-size:28.12pt;color:#000000">- hægkÕ¯Æb ´Ós ôÿ»ÿ h qÓÿÖt {2 Þs˚[ hL»Þ !</span>
<span style="font-size:28.12pt;color:#000000">Öˇ ÀHÔÿ h wh#˚ hˇÒ #8qI Fï+ t!ð  t˚¢ÿ tì h</span>
<span style="font-size:28.12pt;color:#000000"> ·m‡ÕÞÿ»</span>

<span style="font-size:28.12pt;color:#000000">I   ¥ÿ;Õ hüÿ tf¥Z h˛k ‡I &amp;ì ·ÿp¨ h Ýóy˛Öł À! ˛ À</span>

<span style="font-size:28.12pt;color:#000000">ÿ  ÿ Ú˛ h«I &amp;ì ·ÿMäÿøBłŒ  L @ B ˛&#x27; s‹ s6 2%˛!81 L @ B</span>
<span style="font-size:28.12pt;color:#000000">“›  D P P L    &gt; &gt; O  P D A E P D K I A  ? K I  N A = @   D P I   P E @       D P I H</span>

<span style="font-size:28.12pt;color:#000000">ü I ¸“Ø˛ÓÞßÿ» h tÓh·R{ÿ&#x27;, hÞ»Wìe &#x27;,</span>

<span style="font-size:28.12pt;color:#000000">X. ”ª h/ÿ_ ÀÞÿ</span>

<span style="font-size:28.12pt;color:#000000">»I qÓxJìÞïö lÚhìÞï ·E÷ÉI »2[ÿÿ‹ ¸Ö p ÿ §  ìO</span>

<span style="font-size:28.12pt;color:#000000"> Ø {2ç  –˛ ö  þ ˛ “o&#x27;+Ø‡q+ ÿ—–p 	I I p ÿ § t ¤81 ·Þ» h w</span>
<span style="font-size:28.12pt;color:#000000"> ·ÿ2C ¥ï   ï‡˛ÿ_ h Ýà 6óI  §y h ·ÿ2C + À=†ı¨I</span>
<span style="font-size:28.12pt;color:#000000">  §? tÞ ! î\ h¥ÿ˛ìðI  \ô0ÿ  t¸ I  Y å{ŒØ ,!4 ô À</span>
<span style="font-size:28.12pt;color:#000000">! ^ !MÖ Ø‡ h˛ œwÆØì I  § h Àìÿ.ßÔ⁄!% IJ</span>
<span style="font-size:28.12pt;color:#000000">  ‡yÿì  sßÿ»</span>
<span style="font-size:28.12pt;color:#000000">+Ø‡×§ j</span>
<span style="font-size:28.12pt;color:#ff0000"> &gt; &gt; O  P D A E P D K I A  ? K I</span>
<span style="font-size:28.12pt;color:#000000">   ßO-ì» t—ßÿ&quot;\¨ ÿ{ hÓq-</span>
<span style="font-size:28.12pt;color:#000000">   ì Ú†p ÿ § h¨ E  § hxJk-</span>
<span style="font-size:28.12pt;color:#000000">   ì Ý ×Ö  ÿ“ÿ hìÞ h t Àe ^</span>
<span style="font-size:28.12pt;color:#000000">  q+ B ÆØ h v h Àìÿ.ßÔ⁄</span>
<span style="font-size:28.12pt;color:#000000">  ÆØÿkp ÿ § hqÖ+k Àâ ·aî q+  ÆØf! ÿ</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 29

深度探素C++对象模型
Inside The C++ Otject Model

目录

本立道生 (侯捷谦序)  /009
目录  /0e5
前 言 (Stanley B. Lippman)  /e3
第0章导读（译老的话)  /425
加上封装后的布局成本 (Layut Costs for Adding En  /005
1.1C++ 对象:模式 (The C++ Otjet Model)  /006
简单对象模型 (A Simple Objnct Modei)  /0079
表县驱边象模型（A Table-driven Otje Model]  /008
C++ 对象:模型 (The C++ Otjet Model)  /009
对象模型如何影响程序 （1ho de Ohut Medel Cfhcs  /013
1.2关键司质带泉的差异（A Keywon Distincion）  /015
关健词的图优  /016

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p029_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 30

原度根董 C++ 对象模型 (Aeie 7he C+ + Oljrer Molel)
聚略性正R的 struct (The Politically Coet Stnct)  019
1.3 对象的差异 (An Otject Ditinction)  /022
指针的类型 (The Type cf a Pointer)  /028
加上多&之后 (Addng Pslymorptism)  /029

2.1 Defult Contrctor 的建构操作  /039
“带有Defaul Constn  /941
“带有 Defaul Conitsctor* 的 Base Claes  /044
*鲁个 Vinusl fnetiee” I的 Clas  / 044
“有个 Vinual Base Clas° 的 Cles  /046
总结  /047
2.2 Cepy Centntor 的建构操作  /048
Defult Meerwise lsitaliatios  /049
Bitsise Copy Sematis (位通次携贝)  / 051
不要 Biwise Copy Serantios1  /053
重斯设定 Vintal Table 的指针  /054
处 Vital Bae Clas Subotijet  /057
2.3 程序转换语意学(Progran Transformasion Sc  / 060
明确的物始化操作 (Explici sialiatios)  /061
步数的初始化 (Argumest Iniaiasion)  /062
返居值的初始化 (Retuems Valae nitiaiaioe)  /063
在使用者层图优化（Optimizzuticen at the User Level  /065
在编译器层医优化（Optimzatice a De Compile Level)/066
Copy Coe：要还是不要！  /072

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p030_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 31

资要  1474
2.4成员们的初始化队伍 (Momber lsitialiarioe Iist)  /074

3.1 Daa Member 的界定 ( The Bindingofs Dua Monber  /08
3.2 Da Menbr 的布员 (Dsa Member Layout)  /02
3.3 Data Member 的存取
Static Data Members  / 095
Neestic Dea Menbets  /097
3.4“糖表* 与 Deta Member  /099
只要继承不要多态（Inheriae  / 190
加上多& (AMing Pelymrhis)  / 107
多重继承 (Meltiple Inheritance)  /112
虚系继录 (Vintusl tshertanc)  /116
3.5对象成,员的胶率 （Otjee Memher Emkimey)  /124
3.6 指肉 Dea Membes 的指址 (Priter o Dua Monhes  / 129
“提向 Membens 的指针”的教率问题  /134
4.1Memher 的各种调用方式  / 140
Ninstkic Menher Functioms (非静态成,员函数)  /141
Vintal Merber Fusciets(盘报成,员通数)  /147
Sturic Member Functfioes (静态8员函数)  /148
4.2 Vintsal Menber Fusctions (盘拟皮员通数)  /152
多道继录下的 Vintual Functioes  159

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p031_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 32

深度型素 C+* 对象根型 (buide TIv C+- Oier M
虚拟继录下的 Virtual Fusct  /168
4.3函数的效能  /170
4.4 指R Menber Functios 的I指l (Peinter-o-Member )  / 174
支持“8R Vital Menbe Funcions"之指  / 176
在多重建系之下,向 Mcmbe Fanctioms 的指针  /178
“指向 Memther Funstios 之指计* 的胶率  /180
4.5 Inline Fanctions  /182
形式鲁数 (Feml Ngumc)  /185
局部交量（Local Variables)  /186

线盘拟函数的存在 (Pesme efas Paee Vital Fusctios)  /193
虚拟规格的存在 (Presene ofa Vistual Specifcaion)  /194
虚拟规格中 conn 的存在  / 195
重新考虑clas 的声明  / 195
5.1无继承懂况下的对象构造  /196
按象数图类型（Abdtrac Data  / 198
为继承做准备  /262
5.2增承体系下的对象构造  /206
适拟冠承 (Vital Iheriance)  /210
vptr 初始化语意学 (The Semaof' the vptr l  /213
5.3对象复制语意学 （Otjct Copy Sena  /219
5.4对象的功能（Otject Ecimey)  /225
5.5 解物近意学 (Semantics of Desta  /231

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p032_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 33

6.1 对象的构通和解构(Otjcc Coatr  24(
全期对象 (Giobael Ohjpots)  242
异那鲁态对象 (Locel Suatic Otjcts)  / 247
象数细 (Amy of Objcts)  /250
Default Coestructoes RB  /252
6.2 sew 相 dclete 运算排  /254
针对数组的 new 适意  /257
Pacement Opernmer new 的适意  /26)
6.3始性对象 (Temponry Otjects)  / 267
临时性对象的速思（神话、说)  /275

7.1 Template  /280
Templte 的 *其现* 行为 (Templae Instantiation)  /281
Template 的误很鲁 (1rer Reporting within a Templatc)  /285
Templat 中的名用决议方式 (Name Reschtioe wihin a Templane) /289
Menber Tunction ISAI&I17 (Member Iunerie Itsaranion)/292
7.2  异常姓理（Eoxepioe landig )  /297
Exceptioe Handing 快速检阀  /298
对 Emepice Handing 的支持  /303
7.3执行期类型识别 (Rantie Type ldetifcaien, RTT1)  /308
Type-Sadr Dencat (保证变全的次 下转型换作)  /310
Type-Safe Dynamic Cat (保证安全的动态转型)  /311

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p033_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 34

聚度别素 C++ 对象模型 (ferid The C- + Olyeer Msder)
Referemces #不昆 Poistee  /313
Typeid 运算行  /314
7.4效率有了。弹性呢  /318
动态共享函数库 (Dyrnumi Shared Libraris)  /318
共享内存（Shared Menory)  /318

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p034_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 35

第 1 # 天下对象 (Ohkjea L

第1章

关于对象
(Object Lessons)

在C语言中，“数据”和“处理数据的操作（函数）”是分开来声明的
也就是说，语言本身并没有支持“数据和函数”之间的关现性，我们把这种程序
方偿称为程序性的（proceanml），由一组“分布在各个以劝能为导向的函数中”
的算法所整动，它们处理的是共同的外部数图，辜个例子，如果我们声明一个
stnat Pait3d,像这样,
typedef strvet poist34
flot xi
floetyr
)Peint3dr
款打印一个Potld.可能就得定文一个像这样的函数

ptistE(*(kg, g, kg 1°, pd->s, pd->y, pd->1

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p035_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 36

深度指象 C++ 对象模型 (Auide Zhe C++ Oyecr .Me

或者，如果要更有效半一费，就定文一个宏
ptistt(*(kg. g. g 1*, p4->x, pd>y. pl->)
也可直接在程序中完成其操作：
voidmY_f09-()
Psist34 *p4 - grt_a_point (1/
/直接打印出poi

网样道理，某个点的特定坐标值可以直接存取

也可以经由一个前置处理本案完成：
detiae X( p, xva1 1 (gp.x) = (xmal)/
K( p4, 0.0 11
在C++中，Poinr3d 有可能用独立的“抽象数据类型（absta daa type
ADT)*泉实现
class Foist34
publie:Peist36( fieet × + 0.0, fieet y * 0.0, f1sat s = 0.0 )
float x() ( returs _x))
float s() ( returs _az )

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p036_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 37

第 1 章 天T对象 (0hjet Lesoes)
void x( float xvai 1 ( _x = xval/ )
// ... ete .
fioat _4i
inline oetreaniperatorcc( ostrean sos, const Poist3d 4gt )

1r
或是以一个双层减三层的clas 体系完成：
Point  palietclass Point (
Peintc float ×= 0.0 ) 1 _xt ×) ( 1
Point2d  leat xt) g retuen _xr 3
Point3d
class Polst2d : public Point {paiit1
Point2d( float x = 0.0, float y = 0.0 )
Nant( x 1, _rf y ) 1 1

float _
Fr
class Peint3d : psblic Foist28 (

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p037_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 38

尿度探案 C++ 3 8模型 (Auide 7her C++ Otbyecr Ma

fleat _a/

更进一步来退，不管得一种那式，它们都可以被参数化，可以是坐标类型的
参数化
tenpiate < ciass type >

0, tyPe 1 = 0.0 )
rpexilIretoid xt type xval 1 1 _x * xvaLr [
// .- ete -=

也可以是坐标类型和坐标数网者都参数化
class Pointate < class type, int dia )
psblic:Polat()

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p038_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 39

第 1 # 关于对量(0hjet L.sos
//.. tc ..*
type _etordst dim 1:
: c1ass typt, int din
orccIeatrean

#《< g()
很明量，不只在程序风格上有截然的不间，在程序的思考上也有明是的差。
有许多令人结量的讨论告诉我们，从款持工程的服光来看，为什么”一个ADT或
clas hierircty 的数图封案”比“在C程序中程序性地使用全局数据”好，但是
这些时论往往被那费“被要求快速让一个应用程序上马应孩，并且执行起来又快
又有效率”的程序员所忽略，毕竞C的吸引力就在于它的整慶和菌易（相对于
C++西W) .
在C++中实成3D全标点，比在C中复杂，尤其是在使用 wmpate 的清
况下，但这并不意味C+就不更有成力，或是（增，从软件工程的眼元来看）
更好，当然啦，更有成力或是更好，也不意球着使用上就更容易。
加上封装后的布局成本（Layout Cests for Adding Encapsulation）
程序员看到Peintf 转换到 C++之后，第一个可能会网的问题就是：加上了
鲜装之后，布局成本增加了多少？答案是caesPomt3f并授有增加成本，三个da
mcmben 直换内含在都一个 caes ctjocu 之中，就像 C met 的情况一样.
mmber finctioes 虽然含在 cdhen 的声明之内，年不出现在 ctjec 之中。 每一个
tias只会调生一个通数实体，至于每一个“揭有零个或一

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p039_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 40

探度探素 C++ 对象模型 (ile 7e C++ Oyer AModr)
定文”的 ifint fanction 斯会在其每个使用者（模块）身上产生一个函数实体
Point3支持封蒙性质，这一点并未导给它任何空间或执行期的不良质应，你即
将看到。 C++ 在布局以及存取时间上主要的额外负担是由 vitual 引起，包据：
virtual fumetien 机制用以支持一个有效率的“执行期务定”
■virtual base class用以实现“多次出现在理承体系中的 basc
clas，有一个单一面被共享的实体”
此外，还有一些多重继承下的膜外负担，发生在“一个 derived clas 和其第
二减后继之 hase cas的转换”之间，然面。一般言之，并发有什么天生的覆由
说C+程序一定比其C兄弟度大或忍缓。
1.1C++对象模式（The C++Objeet Model)
在 C+* 中, 有胃种 cheg data mombers: saic 和 sostatic,以及三种 claes
mesber feectiees: static, sorsisik 和 vitaal, 已年下显这个 dlass Poowr 声明
siass oiat (pub1:c:
1rteal -PeLnt())

prist ( estrean sos 1 con

这个clas Paiw在机器中将会被怎么样表成呢？生就是况，我们如何模智
[modrling) 出各种 daca mersbers 期 funetios members 呢7

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p040_scan.jpg -->


<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">本页已使用福昕阅读器进行编辑。</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">福昕软件(C)2005-2007，版权所有，</span>
<span style="font-family:'MSSong';font-size:11.00pt;color:#ff0000">仅供试用。</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 41

简单对象模型（A SimpleObject Model）
我们的第一个模型十分简单，它可能是为了尽量减低C++编译器的设计复
杂度面开发出来的，赌上的则是空间和执行期的效率，在这个简单模型中，一个
object 是系列的 slots, 每个 slot 指向个 membes. Membens 按其声明次
序, 各被指定个 slot. 每个 data member 或 functioe menber 都有白已的-
个sdot.图1.1可以说明这种模型

图1.1黄单对象模型（Simple Object Model
在这个员羊模型中.mem本身井不放查ctjot之中.只有“指向member
的指针”才放在otject 内，这么他可以避免“n有不间的类型，因而需要

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p041_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 42

深度指R C** 对B 8型 (Ieide 7he C++ Obeer Moder)
不同的存储空间”所相致的同题，Objoct 中的 mcmbens 是以 dlot 的索引值末寻
址，本例之中 x 的素引是 6. _poisr_oaer 的素引量 7,个 cas ebjet 的大
小很容易计其出来：“指针大小，鼎以clas 中所声明的 menhets数目使是
虽燃这个模型并疫有被应用于实际产品上，不过关于素引成 sle 数目的现
念，例是被应用到C+*的“指向成员的指针”（pointero-nember）或念之中
表格驱动对象模型（ATable-drivenObject Model）
为了对所有 clases 的所有otjects 都有一致的表达方式，另一种对象模型是
把所有与mmhers 相关的值息抽出来，放在一个daa
member fasctiee tatble 之中, class object 本身期内含指向这两个表格的指针
ien table 是 系别的 skots, 等个 slot 指出
table 则直接含有data本身，如围1.2所示

情含实际数图）

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p042_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 43

第 1 章 关于对象(Otject Lesoms)
显然这个模型也没有实际应用于真正的C++编译器身上，组me

C++对象模型（The C++ Objeet Model)
Sroutrep 当析设计（当前亦仍占有优势）的C++对象模型是从简单对象膜
型派生面来的，并对内存空间和存取时网效了优化。在此模型中，Nostaic dn
members 被配置于每一个 clas otjot 之内,ntic daa mmhens 则被存放在所有
的 chas object 之外。 Stutic 和 senstatic fenctios menibers 也被技在所有的 clast
dtjet 之外， Virtal inctiees 则以两个步建支持之：
1.每一个 class 产生出一堆向virtaal funetions 的指针，放在表之
中.这个表格被称为intacl table（vtbl)
2.每一个 classotjaet 被添加了一个指针，指向相关的 virtal tabic.通常
这个提针塞移为 vptr，pr 的设定（srtting）和重置（resing）都由
每个 ciss  ceesrecy、 4estructor 相 copy assigamsmt 运算符自动
完成（我后在期s 章t论）.<个 cas 预关联的op_iny%object（用
以支持atims (;pe iserisatioe, RTT1) 速经由 vinsal table 被指出
亲，通常是玻在表略的第一个slat处。

1至少有一个COR3ACRB实影产品使用了适种“双表格模型”.SOM对象模型也值校
这种“单象整模型”[HAM95]

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p043_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 44

发NR C++ 8要E (uiale Zhe C++ OlrerMo

图 1.3C++对象模型（C++Object Model)
图1.3说明C++对象模型如间应用于前面所说的Poircas 身上.这个模
型的主要优点在于它的空闻和存取时间的效率：主要敏点则是，如果应用程序代
码本身未管改变，但所用到的 claes ctjects 的 nontaic dana mmbers 有所修改
（可能是增加、移除成更改），那么那益应用程序代码网样得重新编译，关于这
点，前述的取表略模型就提供了校大的弹性，因为它多提供了一层同接性，不过
它电因此付出空同和执行效率两方面的代价就是了。
加上继承 (Adding Inheritance)
C+支持单一承：
class Lbrary_aaterials ( ... 1/arials 4 ---1

C++也支持多重继承：
//原本的（更早于标准原的）Lostresn实现方式

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p044_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 45

第 18 关于对时象 (0hjct Leoes)
cLass 1ost:rean:

著至，继承关系生可以指定为虚损（vintal，也就是共享的意思）：

在虚担型承的情况下，baseclas不管在继承申链中被派生（dorivd）多少次。
永远只会存在一个实体（称为 subcbjet），例如 ionaw 之中就只有 vital ioy
base class 的个实殊。
一个deried cas 如何在本质上模整其 bast clan 的实体视！在“境单对象
模型* 中, 每一个 buse clas 可以度 derived clas otject 内的—个 slet 指出.该
slot 内含 hae clas naetject 的地址，这个体制的主要缺点是，因为间接性可导
致空间和存取时间上的服外负担。优点则是 clas otjoct 的大小不会因其 bas
lases的改变则受到影响
当然啦。你生可以想象另一种所清的 hau table 模型，这里所说的 bas clas
teble 被产生出来时，表格中的每一个dlot 内含一个相关的 bae cas 地址，这
提像 vitsal tbie 为含每—个 vinal fnction 的地址一样、每—个 clas ebjoc 内
含一个 bpr，它会被初始化，指向其 base clas uble.这种繁略的主要装点是由
于间接性而导致的空间和存取时间上的眼外负图，优点则是在每一个cas ckjectl
中对于继承都有一致的表现方式，每--个clascbject 都应请在某个园定位置上安
放一个 basn table 指针，与 base clases 的大小或数目无关，第二个优点是，不
需要波变cls etjes 本身，就可以放大，壤小、成更改 ban chasabe

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p045_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 46

深度探素 C++ 对象模型 (/mlude The C++ Olyecr Mndt

不管上过哪一种体制，“风接性”的级数都榜因为建承的探度而增加，例归
一个Rnal_ok需要两次间接存取才能够握取到继承自 Library_mderiai 的
hcmbers,而 Beok 只黄要一次。如果在 derived dlass 内复制一个指针，指向级
承申链中的每一个bas clasn，例是可以获得一个水选不变的存取时间，当然这公
颁付出代价，因为需要额外的空间来放置额外的指针。
C++ 最初采用的继承模型并不通用任例间接性: base cass subebject 的 daa
memben 被直接放置于 derived clestjet 中,这提供了对 base clas members 最
紧澳死显量有数率的存取，敏点呢？当然就是：base clats menbers的任何改变，
包括地加，称除成改变类型等等，都使得所有用到“此ba clas或其 drivod chasn
之objects”者必领重影编讲。
自 C++ 2.0 起才斯导人的 vintual basc cdas. 贯要一热间接的 basc clas 激现
方法。Virtual hase dlaes的原始模型是在 class ctject 中为每一个有关联的 vital
bac clan 加上一个指针。其它演化出来的模型则若不是导人一个 vintaalbasc

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p046_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 47

第 1章关于对象 (Oljet Lessas)
class tabe,批是旷光原巴存在的 virtual table, 以使维护每个 virtal base claes
位置：3.4节有这方要的详细讨论。
对象模型如何影响程序（How the Object Model Effects Program
这对程序员带亲什么意文呢？骤，不同的对象模型，会导致“现有的程序代码
必须廉改”以及“必须加人新的程序代码”两个继果，例如下图这个通数，其中class
X 定义了 个 coey ceetructor, 个 vinual destnato, R个 vital fnctioe fo
foohar()

/ foer) 8 ↑ virtsal fuset:iot
px>oo1
retars a;
这个函数有可能在内即被转化为：
voi4 foobar( X s_result )
rslt.X1x(17
if : px 1= 0 )
PK-2E1:E(1 :
fool 4_resuit 1/
/ 使用 virtual 机航量 px->foo(I

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p047_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 48

振度探素 C++ 对象模型 (Durile Tle C++ Oyircr Moder)
*px-3rtbl[ 2 1 1( px
// 扩& delete px)
delete( pe 1)

retezsy

握，真是差屏额大，不是吗！当然，此到你并不需要了解所有的转化过程及
结果，我会在后继章节解释其中每一步操作的用意，以及为什么那么效，我思家
会国头看，一边所寿你的手指头，一造说“餐款，是的，鱼然”，同时奇怪你忽
么会管经述情过。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p048_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 49

第 1 章 美于时象(0bjpct Lesoms)
1.2关键词所带来的差异（AKeywordDistinction）
如果不是为了务力维护与C之网的兼容性，C++远可以比现在更美单。
举个例子，如果设有八种整数需要支持的话，overloaded fnction的解决方式将会
燕单得多，同样的道理，妇果C++丢掉C的声明语法，就不需要花脑系去判激
下面这一行其实是gf的一个函数调用操作（inmocatiom）面不是其声明：
on 逐是 isvocation
nt ( *pf 1t 1024 1/
面在下面这个声明中，像上度那样的“向期頭第（lockahcad)“甚至起不了
作用
// meta=lac
Ist ( *pq 1( 12
当语言无法区分那是一个声明还是一个表达式（espesion）时，我们需要一
个尾继通言花围的现别，而该规则会将上述式子判账为一个“声明”
网样地，如果C++并不需要支持C 原有的 strwet，那么cias 的现念可
以借由关键词cla来文持，但地对令你禁语的是，从C迁携到C++，除了
效率，另一个最常被程序员询问的问题就是：世么时候一个人应镇在C++积序
中以 tret 取代 class
如果是1986年，我的答案毫不癌现带水：“绝不”：在我的C++Primer 第
版和第二层中（泽注：第三版已于1998%05出版），关键间dnct并未出或在
书正文，只出现在附录C，面附录C用来封论C语言，在那时候，这是一个
人非万不得已不会指出的一费小小哲学问题中的一个，然面如果能够指出这个间
题，你可以获得一些小小（一般公认非常小）的满足。通意问题会被这样指出：
“理。你知道局，那个类键词，其实疫什么用”，但就像员尔实验室
（译注：Be Lab，C++发需地）的一位网事院转对我选的，即使是量小的暂平

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p049_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 50

问题电有人需要解答，如果一个C程序员渴望学习C++，当他发成我的书中社
有提到mtrnct。一定会相当否情，很显然把达个主题含人，可以提供语言移转的
的教生素，让程序员攀上高峰时少点折断，呵。多么暂学明！
关键调的困扰
那么，让我重新间一次。“什么时候一个人应该使用muct取代class？”答
案之一是：当它让一个人感党比较好的时候。
显然这个答案并没有达到亮拉术水平，组它的确指出了一个重要的特性：
健词stret本身并不一定要象征其后随之声明的任何东西，我们可以使用
struet 代特 elass，但仍然声明 pubis， proteted，prite 等等容取区段，[以
及一个完全 public 的强口,以及 vinsal fenctioms,以及单一是承、多重糖承，虚
拟赠承等等，以前，似乎每个人都得在一小时的C++美企中花费整整10分钟
来看清胞以下网者的相同：

7/ mom:1e
和其C等品

1r
当人们和均科书说到struet时，他们的意思是一个数据集合体，没有 priviac
daa. 没有 4ata 的相应操作 （译注：指 member finctien) 、 亦即规然的 C 川
陆，这种用建应该和 C++的“使用者自定文类型”（usr-defined tpe）用法区
分开案，在C这一边，这个关键词的设计理由固其用法面春在：面在C++那一
边，选择 struet 或 class“作为关键试，并用以L导人ADT”的理由，是考里
从此比较键全，这遇比过论“函数需不需要一个大括号”，或是“要不要在变量

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p050_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 51

第 1 # 关T对象 (0Ojet Lesms)
名称和类型名称中侵用下划线（例如 Litbgkr 或a_rigk)*更具幅神显次
在 C 新支持的 strect 相 C+- 所支持的 chas 之间,有个观念上的重要型
异，我的重点很简单：关键闻本身并不提供这种差异，也就是说，如果一个人拥
有下要的C++使用者自定又类型，单可以误“置、那是一个clas”
/ struct 名B (或 class 名称) 暂时省略
pebllet

事实上客可以说上面那东西是个muct.包可以说它是个as，这两种产明
的现念上的意文取决于对“声明”本身的检验。
个例子，在 chet （译注：第一个 C++实作品，由 Lipman 完成）之中，
上述两个关键词在语意分析器（parser）中是以共享的“AGGR”普换的，面在
Foundaion 项目中, Boo Mumay 的 ALF 显次雄构保管了程序员真正使用的关键
调，然面这份信息并未在更内层的编译器中被使用，例是可以被一个“urpancr
工具用案还原程序的 ASCI1 面较，明，是的，如果程序经过“unjwser”工具处
理过后，无法还原原本使用的天键词，程序员一定会很组责—即使程序在其它
方面是相等的。
我第一次被我所道的“关键词受难记”终例，是在大约198年，当时我们
测试小组中的一位成员对cDont发出一个“大难临头，即将光蛋”的臭虫握告
在ehaomt 内都的类型层次结构的原始声明中，根节点（noet node）和每一个抓生
下来的子类型（subtype）是以 straet 关键词来声明的，面在陆续疹改的头文件
(header fles) 中, 某费抓生子类型 （deived sbypes) 的期量声明 （foewand
natioe)如是使用关键词 elas强：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p051_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 52

深度探素C++对象模型（Juide 7hr

/ 不金熟与1不。只不过是不一重基了ciass node/
struct node ( .--. 1=
我们的测试员说这是一个租野的错误，是一个cfom无法情提的问题，国
为....当.… 用.国它自
真正的问暴并不在于所有“使用者自定义类型”的声明是否必须使用相网的
关键润，问题在于使用class或truet 关键润是否可以始予“类型的内那声
明”以某种承诺，也就是说，如果truet 关键词的使用实现了C的数据本取
观念。面 elass 关键词实现的是 C++的 ADT （Abstnct Data Typc）观念，用
公当然“不一致性“是一种错误的语言用法，就好像下面这种错误，一个otect 被
开盾地声明为 smatic 和 cdom
atatie iat fooi
exteth ist foe/
这组声明对于joo的存健空间造成矛盾，然面，如你所见，strwet和elass
这同个关键间并不会造成这样的矛质。clamm 的真正特性是由声明的本身
（declmsion bob)来决定的，“一致性的用续”只不过是一种风格上的间题而C
我第二次射这个间题是在C++ 3.0 所引1人的parnmter lits of temlat
上头、StegBueorf，我的另一位贝尔实验室同事，有一天走进我的办会室并指出
以下程序代码被语意分析器（pse）程为不合法：
stract musble ( ..- 1/
然面下面的代码邮是合法的

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p052_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 53

第 1 章 于对象（0kjeot Lc
/间题：它明自使用了e1ass 关健词
struet murele ( -: 1r
“为付么·单问道。
“为什么不？”我增是地予以图告，然后详细说明emplanes 并不打算与C
兼容，我说让我们附开tnmct不读，然后再看看它做什么事，我想我大一致
而过我的Sun3/60机器并以最佳麦态挥舞鼠标——老实说我不记得了，不过我记
得最终我更改了语意分析器（pwser），使它阿时独受两个关键间——在没有事先
通知 Bjame 和少不更事的ANSI C++委员会的情形下。这是这个语言用词的通
你可能会争持说，如录运个治告六支持一个关键间，可以省障济多提增与建感。
但你要知道，如果C++要式所现存的C程序代码，它就不能不文持struct.好
的，那么它需引人所的关键词las吗？真的需要码？不：但引人它的确分常
令人满意，过为这个浮言所引人的不只是大硬词，还有它所支持的封装和继承的暂
学、非不是药下团染力,经型波论别个糖象的 base et (衡始Zeo.4nimo
tucnt 星次结构) 时，其中内含-个或更多svintaa base sret 的情形。
在前面的计论中，我区分了“wtrwet关键词的使用”和“一个snuct 声明
的辑意义”，你也可以主强说这个关键词的便用伴随者一个pubic接口的声
明，就好像在公开演讲中使用暗语成呢称一样，你基至可以主准说它的用建只是
为了方便C程序员迁提至C++部落
策略性正确的 struet(The Politically Correet Struet)
C程序是的巧计有时候即成为C++程序员的陷供，例如把单一元素的数组
放在一个ntrc 的尾确，于是每个 stcatjets 可以拥有可变大小的数组：
struct muable (

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p053_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 54

尿度探索 C++ 对象模型（bui

//然后为strect本身和该字样申配置足够的内在//从档案或标准输人装置中非得一个字群单
malioc( siteof( struct
stropyt smsble.pc, string 1/
如果我们改用 casr 来声明，西该 claes 是：
■指定多个accesectiens，内含数据：
■从务一个class 医生而案：
■定又有个或多个virtsl func
那么或许可以联利转化，但也或许不行！
C++中凡处于同一个aos sccion的数据，必定保证以其声明次序出现在
内存布助当中，然而被放置在多个acen mctioas 中的各笔数，带列次序就不
一定了，下面的声明中，需述的C仗俩或许可以有效运行，或诗不能，需
pronced da menhes 被放在 private dala memben 的能面或后面而定 (谦注：建
在前面才可以）：
clasastuable (

pcivate:/* pr1rstutf */
har pcI 1 17
同样的道理,base clases 和 derive dass 的 das menbers的布局也没有谊
先谁后的摄制规定，因面也就不保证前述的C住例一定有效，Vinul fnctions

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p054_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 55

第 1#  关于对量(0jet L.sms)
存在也会使前述使情的有效性成为一个间号，所以最好的患告就是：不要那么做
（第3章会更详组地过论相关的内存布局主题）
如果一个程序员迪切需要一个相当复杂的 C++clae的某部分数据，使地拥
有C声明的那种样子，那么那一部分最好抽取出来成为一个独立的stnc1声
明，将 C 与 C++ 组合在一起的作法就是，从 Csrut 中振生 C++ 的部分

于是C和C++两种用独都可获得支持：
exters *c* void drax_zect ( C_poist, C_poist 1/
ciet( 0,c1100
这种习惯用法现已不再被推荐，因为黑卷编译器（如 MicrnC++）在支持
vital fnsctiee 的机剧中对于 clas 的继承布局做了一些改变（请看 3.4 节的讨
论).组合（cmpoioe)，而非继承。才是把C 和 C++结合在一起的唯可
行方法（cvenion 运算行摄供了一个十分便利的萃取方出）：
struct C_peint ( ..- 1/
piass Point (

//.._c_posint
Cdtruct 在 C++中的个台理用速，是当你要传通“一个复杂的 clas ctjoct
的全那或部分”到某个C函数中去时，mst声明可以病数据时装超束，并保证
拥有与C兼容的空间市局，然面这项保证只在组合（compce）的情况下才有

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p055_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 56

在，如果是“继承”图不是“继合”，编译器会决定是否应该有额外的daa
被安禁到 basc struct subobject 之中 (再次谱你多考 3.4 节的讨论以及图 3.2a
和图3.2b)
1.3对象的差异（AnObjeetDitinction）
C++程序设计模型直接支持三种pg  gms（程序设计典范）

1.程序模型（precedural modet），就像C一样，C++当然也支持它。
字特串的处理就是一个例子，我们可以使用字符数组以及r*函数能
（定文在标准的C函数库中）：
char *p_son;boy[1 = *bansy°;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p056_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 57

第 1 童关于对象 (Ctjet Lsoms)

2.接象数图类型模型（abstract data type medel，ADT）.该模型所
请的“抽业”是和一组表达式（peblie 接(1)一起提供，西其运算定文
的然染或未明。例加下医的 Siring class:
gtzing gir1 = *Asna*String daughterr

// stri
take_te_disneyland( gir1 1
3.面向对象模型（objeet-eriented model）.在克模型中有一些技比
相关的类型，通过一个抽象的base class（用以摄供共通接）被到装起
象。 Library_materiels class 就是个例子, 真正的 sbtypes 例如 Boot
Fidee, Compeev_Dise, Pwpper、 Laptsp 等等部可以从那里进生面来
rosdheek_in( Librery_saterials *pnat )
pmat-slate() 1
pat->checi_ia(1:
paat->sotify1 plend 1Jeserved[)

纯榨2一种pmadipm写程序，有助于整体行为的良好稳图，然面如果显合
了不民的pandigms，就可期会带来让人惊吓的后果，特别是在没有通慎处理的情
况下，最靠见的流忽发生在当佛以一个 busc clams的具体实体如：
Library_saterials thinql:
亲完成某种多态（pelymophim）局面时;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p057_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 58

深度指董 C++ 3 象 R型 (Auide Zhe C++ Obyeer Modnl
// class Book : peblic Litcaty_naterials
Library_materialBock book.I
Book
thing1 - books
// 霍度: 调用的量 Library_matetiaiaticheck_in()hingl-check_in():
而不是通过 base clas 的 pointor 或 neference 来充成多态局图：
/ O.: 度 thing2 鲁考例 bookLibrary_nateriais sthing2 = bock,

显然你可以直接或间接处理继承体系中的一个hasn casobjet.组只有通过
poister 或reference 的润膜处理，才支持 OO 程序设计所需的多态性质。上个例
子中的 ahing2 的定文和运用，是 00 pandign 申一个良好的例证，shing/ 的定
文和运用则逸出了O0 的习惯：它反映的是一个 ADT panligm 的良好行为
hing/的行为是好是坏，视程序员的意图而定，在此花的中，它的行为亦靠有可
能不是你要的！
在OO paradigm之中，程序员需要处理一个未强实体，它的类型蛋然有所界
定，却有无旁可能，这组类型受限于其继承体系，然面该体系理论上投有探度和
广度的限制，原则上，被指定的otject 的真实类整在每一个特定执行点之前，是
无波解析的。在C+*中，只有通过poiens 和 refenes 的操作才董够完成
相反地。在ADTpandigm中程序员处理的是一个提有图定而单一类型的实体
它在编译时期就心经完全定文好了。举个例子，下面这组声明

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p058_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 59

第1章共于对象 （Otjot L4

Librar_saterials krx = *ps.:Ye_some_material ():
//提述已知物：不可能有令人惊研的结果产生
ibrae_materiaia dx = *pxr
你绝对没有办量确定地说出p或r到底指向阿种类型的atjrcbs，你只能
等说它要不就是Lbrary_mutrials chjet.要不就是后者的一个子类型（(shype）
不过，我们例是可以确定，只能是Library_mamialr class 的个 otjet. $
节稍后，我会付论为什公这样的行为虽然或许未如作所预期，却是良好的行为
显然“对于otjoct 的多态操作“要求此 otect 必须可以经由一个 poiner 或
rmce 亲存取，然源 C+ 中的 peinter 或 mferoc 的处理如不是多态的必要
结果，思想下图的情况
// 提有多态 （深性: 因为操行对象不是 ciass oaject)at *pir

： class x 携为- base class （注：可以有多态的教果）
在 C++，多态只存在于一个个的publiccss 体系中.个例子，pa可能指
向自我类型的一个otect.或脂向以l pcblic抓生页来的一个类型（请不要把不良
的转型操作考患在内），Nonbic 的置生行为以及类型为 woid*的脂针可以说
是多态，但它们并投有被语言明白地支持，也就是误它们必须由程序员通过明白
的转型操作来管理（你或许可以说它们并不是多态对象的一线选手），
C+以下列方盐支持多态
1.经由一组隐含的特化操作，例如把一个 derived class 指射转化为一个指
向其 pabic hase typec 的指针:
shape *gs = ntv circle(1;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p059_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 60

探度探素 C++ 对象提型 (Drile The C++ Oircr Mod

2. 经由 virtual fsnctioe 机制:
ps->retate())
3. 比由 aymamic_cer 相 opeif i运算得:

多态的主要用建是经由一个共间的接口来影响类型的封装，这个接口通常树
定义在个抽象的 haee class 中。 例如 Lbrery_mserialr class 算为 Boei,
Iides.Pager 等 stbype 定文了一个接口，这个共享楼口是以 vital finctiee 机
删引发的，它可以在执行期根据ctject的真正类型解析出到底是哪一个函数实性
被调用，经由这样的骤作：
Liarary_material->checa_out (1)
我们的代药可以避免由于“借册某一特定 libry 的mris”百导致变动无常。
这不只使得“当类型有所增加，修改，或断减时，我们的程序代码不需改变”。
医且也使一个新约 Lrary_merialr mbype 的供应者不需要重新写出“对继承体
系中的所有员验都共通”的行为和操作。
考息一下这标的码
velid drstalt (
cen c x *po.:
//在执行期之载，元结决定月症调用哪一个rotate（）实用
referesce.rotate():*pointer)-rotate(1 7

Mais(3 (

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p060_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 61

第 1 章 天于对象 (Otjfou Less
± 2 / 2 是 x 的个子要型
rotate z, st, ± 11
经由pointer 和referemce完成的阿个“函数调用推作”会被动态完成：武例
中它们都调用Z:roa).经由dnm完成的“函数调用操作”则可能（或可能
不）经由 vintl 机制，不过。它反正总是调用 X:rostater) 就是了。（这就是所
请的“编译家界”问题：不管经由 dutan 所调用的 vitul ftuncin采不采用
vitual机制，从语意末说，结果都是相同的，42梦对此有更详细的讨论）
需要多少内存才能够表现一个casctjecr？一般面言要有：
其natic data memers 的总和大 小:
■加上任何由于 alignmcmt （谭注）的需求面填补（pading）上去的空间
（可能存在于members 之间，也可能存在于集合体边界）
请注：ligumem:就是将数值调整到某数的倍数，在32位计算机上，通
常 aligument 为 4 byes (32 位) , 以使 bes 的“运输量” 达则量高效事.
■加上为了支持virtual页由内部产生的任何额外负担（overead)
一个指针2，不管它指向癌一种数据类型，指针本身所需的内存大小是圆定
的，个务子。下害有一个Zosnima声明
c1ess BeoAsi8a1 (
Anina1 () ;

如果特换为间强于出、让男要一个瘤针。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p061_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 62

深度探素 C++ 对象根型 (Luile Thr C++ Obincr M

virtaal wois rotat+11i
Lat 1ocz

其中的 caschjan 和指针 pae 的可能布局如图1.4所示。我在第3
再团到“dta membes 的布期”这个主题上
指针的类型（The Type of a Pointer)
包是。一个指向 Zoe(ninal的指针是如何地与一个指向整数的指针或一个指
向 template Amy (如下, 与j个 Snng 一#产生) 的指针有所不间呢！
$toAtina1 *x1
atrayc String > *pta/Et *p47
以内存需求的观点来设，授有付么不同！它们三个都需要有是够的内存来皮
置一个机器地址（通常是个wed，译注）。“指向不同类型之各指针”网的差是，
既不在其脂针表示法不同，也不在其内容（代表一个地址）不网，面是在其所寻
址出来的otjoct类型不网，也就是说，“指针类型”会教导编译器如何解释某个
特定地址中的内存内容及其大小：
增注：Lipman 孩“不同机器上的 woed 为可变大小，ir 则固定是 16-bi
组势有一种说肽是，“不同机器上的iw为可变大小，werd图定为16-bis
不可不察！

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p062_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 63

第 18 关于对象 (0hjpet Lss)
1.一个指向地址1000的整数指针，在 32位机器上，将量盖地址空间
1000~1003 （译注：因为 32 位机器上的整数是 4-byes) ,
2. 如果 Sring 是待统的 8-bytes （包括个 4-byes的字养指针和个月
来表示字辛长度的整数），那么一个Zoo.iximo指针将模前地址空间
1000~1015 (谭注: 4+8+4. 如图4.4)

Stringrint String:len
char* Stringst

1016:1900
Z:oAtinal *pza = &za;
图1.4伊立（9溪生）e92的object 布局和pointer 布局
可，影么，一个证沟地址000而类墅为wi*的指针，将基重怎样的地址
空间呢！是的，我们不年道！这就是为什么一个类型为woid*的指付只能够含有
一个地址，而不能够通过它操作所指之otbnct的缘敏。
所以，钟型（ct）其实是一种编译器指令。大部分情况下它并不改变一个指
针所含的真正地址，它只影响“被指出之内存的大小和其内容”的解释方式。
加上多态之后（Adding Polymorphism)
现在，让我们定文一个 Bar，非为一种Zoommaf.当然，经由“pubtic 是
承”可以完成这特任务
peblic:Bear (1/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p063_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 64

聚度探素 C++ 对象积型 (Juide Thle C++ Olyirct.
bear();

//..-
Bear
int cel1_block
Bear b( *Yogi* 11leae *pb = sb
Bear stb • *pb
b、pb、rb会有忽样的内存需求呢？不管是pointer 或mene都只需要
个 word 的空间（在32 位机基上是 4-byes）.Brar otjet 黄要 24bytes，也就
是Zooininal 的 15 bytes 加上 Beer 所带亲的 8hytes.围1.5 晨示可能的内存
布局

图 1.5 Derived class 的 object 和

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p064_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 65

第 1 章关于对象（Otjet Lessom)

好，根设我们的 ear object 放在地址 1000 处，一个 Beor 指针和
Zooinimul脂针有什么不同？

它们每个都指向 Bear object 的第一个 bytc.其间的差别是。pb 所通重的地
垃包含整个 Bear cbject,面 p 所量量的地址只包含 Bnarotjet 中的 Zaednimn
ssbobject.
除了 Zoonimal subotjec 中出现的mmbern，你不能够使用 pr 来直接处理
Bear 的任例 members. 唯例外基通过 vintsl 机制
4-30e11_6loki
/ok：经过一个明日的
(( Bear* 1pa) ->cel1_block/
//下图达辨更好，世它是一个 run-Lime operation（弹注：成本校高）f c Bear* pb2 = dnic_cast< Bear* >( pa 1)
pb2->ee11_block/

当我们写：
pt->rotate(11
时，pr的类型将在编译时期决定以下同点：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p065_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 66

深度指浆 C++ 对象模型 (Aeile 7he C++ Olyeer Modr/)
■图定的可用接口 , 也就是说,pr 只能够调用 Zoo/nimal 的 public 提 口
■该接 口的 acces level ( 例如 rotme) 是 Zoninaf 的 个 pab)
mber).
在每一个执行点，pr 所指的objoct 类型可以决定 rotaeG 所调用的实体
类型信息的封装并不是维护干 pr 之中，面是维护于 Ink 之中，此lik 存在于
“etjet 的 ve”程”vper 所指之 vitual uMe*之间. (4.2 节对于 vintual functions)
有一个完整的讨论）
现在，请看这种携况：
Sear b

为什么re0 断调用的是 Zoomal 实体面不是 Brar 实体1 此外，如果
初始化通数（译过：应用于上述 amignmen 操作发生时）将一个ctjec 内容光
整携关到另个 object 中去, 为什么 as 的 vper 不指病 Brar 的 virtul table
第二个问题的答案是，编评器在（1)智始化及（(2) 指定（asigmemt）操作
（将一个 clas ohject 指定给另个 chas cbjet） 之间做出了神放， 编泽器必须
确保如果某个 otjee 含有一个或一个以上的vptrs.那些vptrs 的内容不会被
base cdas ctjet 初始化或改变
至于第一个问题的答案是：no并不是（而且生绝不会是）一个Bear，它是
（并且只能是）一个Zee4mmal.多态所造成的“一个以上的类型”的潜在力量。
并不能够实际发挥在“直换存取cbjcs”这件事情上，有一个似是而非的观念：
00程序设计并不支持对otjet的直独处理，举个例子，下面这一组定文：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p066_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 67

第1 章天于对象（Otjnt L.u

Dear
Panda  2a = h
其可能的内存布局如图1.6所示。

图1.6依次定文得到的内存布局
将n或b的地址，成pp所含的内容（出是个地址）指定胎po，显然不
是问题，一个peiner 或一个nmce之所以支持多态，是因为它们并不引发内
存中任例“与类型有关的内存委托操作（bpe-agedn comnt)：会受则
改变的只是它们所指向的内存的“大小客决容魅幕方式”面己。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p067_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 68

指皮指浆 C++ 对象模型 (Aeie 7he C++ Oyee Msil/)
然到，任何人如果企图改变cbjccan 的大小，便会遗反其定义中受契的保护
的“资国雾求量”，如果把整个Ber ohoct指定给m.则会盈出它所配置得列
的内存，执行结果当然也款不对了。
当个 base claes object 被直膜初始化为 (或是被指定为) 个 dcrived cat
otjet时, derhed otject 就会被切别 (sliced) ， 以塞人较小的 baue tpc 内存中,
drived tpe 将投有留下任何赚丝马迹，多态于是不再呈现，而一个严格的编译器可
以在编泽时期解析一个“通过该cbjec面触发的vitul fnctin 调用操你”.因而
图建 vital 机制。 加是 virtal fendien 被定义为 iar, 则更有数率上的大收款.
总面言之，多态是一种威力强大的设计机制，允许你继一个独象的pubic使
口之后，封装相炎的类型。我所季的 Library_maeriuai 休系就是一例，需要付出
的代价就是额外的间接性——不论是在“内存的获得”或是在“类型的决断”上。
C++通过 clas 的 pointes 相 nelerenoes 果支持多态，这种程序设计风格就称为
“面向对象
C++也支持具体的 ADT 程序风格，如今被称为atjec-based（OB），到如
Sring das、一种非多态的数据类型，SPring cans 可以展示封装的非多态形式;
它提供一个pudblic独口和一个privm:实作品，包基数据和算法，但是不支持类
型的扩充，一个CB设计可能比一个对等的0O设计速度更快面且空间更案
溪，速度快是因为所有的函数引发操作都在编评时期解析完成，对象建构起末时
不需要设置vintul 机制：空间紧澳则是因为每一个dlestjeet不需要负担传统
上为了支持vital机制而需要的额外负荷，不过，OB设计比较没有弹性
00和 08设计量略都有它们的据护者和批评者，你可以在[B0OCH893]
[CARROLL93]和[LEA93]找到一些有建的正反两面论点的讨论，这查论文所计
论的县 C++ Booch Conponcrts Ibrary Bll Laboraterier Sundard C++ Components
ibhary。以及 GNU g*+ ibrry 的设计决策，在弹性（OO）和效率（OB）之间常
常存在看取与舍，一个人在能够有效选择其一之前，必须先请能了解阿者的行为
和应用银城的需求

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p068_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 69

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 70

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 71

第2章传道函数运意学（T

第2章

构造函数语意学
(The Semantics of Constructors)

请注：本章大量出现以下美文术通
：暗中的、隐含的（通章意指并非在程序票代码中出现的）
：明确的（通常意指程序源代明中新出孩的）
我有用的
：有用的
: 3时每个 msember 施D[
对每个 bit L
：语意

关于C++、最意听到的一个图想就是，编译答背着程序员做了太多事情
Coeversion 运算特就是最常被引用的个例子，Jery Schwarz. kostm 通数库的
建筑师，就借经误过一个放事，他说他最早的意图是文持一个iostrcirm class objec
的纯量测试（scalar test)，像这样;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p071_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 72

探度探素 C+ 对象提型 (ule Tle C*+ Oirer Mode
if ( cis 1 --*
为了让cin 能够求得一个真假值，lery 首先为它定文一个 commsien 运算
符：openabor inrg。在良好行为期上者，这的确可以正确运行，组是在下面这种级
误的程序设计中。它的行为就势必令人大吃一惊了：
/ 着肽： 应访是 cout. 周不是 cin
in (< intVal
这个粗心的程序员要的应该是coer 而不是cin。Clam 层次结构的“ppe
tat”天性应该能够抛提这类输出运算符的错误运用，然面，带有儿分唯物主义色
彩的编译器，比较喜放找到一个正确的诠释（如果有的话），面不只是把程序标
示为错误就算了！在此例中，内建的左移位运算符（le ait opemor，<）只有
在“cin可改变为和一个整数留同文”时才适用，编译器会检查可以使用的各个
esioe 运算得，此后它我到了openator int).那正是它要的东西，左移位运
算养现在可以操作了：如果股有成功，至少也是合法的：
//霍欧：不完全是程序员想要的
Ist tenp * cin.operatoer ist (1;teap <c intTraL/
Jerry 如何解决这个意患不到的行为？地以 openator void*0 取代 aperuip
ng.这种错误有时做被戏称为“SchwarrEmor”、虽然这种错误只能算是一种“不
足”，但较乏一个iplie cas cemsn 机制都实在是强烈的败笔，Srig clas
的量初实例（引述于 [STROUP94] p.83）可以说是一种推动力量：如果授有 impici
venion的支持，Sring 函数库必须将每个拥有字持参数的C nnime
lirnry 面数都复制一份1.
在不少程序员之间，存在者一种点正不安的情绪，认为一个被编泽器墙中重
1 有是的是, 感度的 C Shrury ting dam 体不提员一个 inglit cansiee 运子：仓
提供的是一个具名实体(namedinrtanoz)，使用者必须明确调用才行。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p072_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 73

第 2 章构造函数各意学 (The Semantics of Coes
行的“user-dcfised coevertioe 运算符”可能不会导题 Schwarz Enor.事实上关键
词expllclt之所以被导人这个语言，就是为了提供给程序员一种方法，使他们
能够制止“单一参数的cosmcter”被当做一个coverien运算符，显然他们很
容易从 SchwzEmr的放事中获得安慰，但是conversion 运算符实际上很难在一
种可预期的良好行为模式下使用，Ceevenien运算得的引人应该是明智的，面其
则试应该是严想的，开显在程序一出现不寻常活动的第一个症状时就发出疑间。
问题在于编济器大过于雀是字要患文亲解释作的意图，我性有在背后为你多
做点什公事情—显然，要让程序免相倍他们被 Schwaz Emor 町上是额为固难
的。“背后括动”比较可能发生在“menerwie inliaiee”或是所请的“naed
retarm valae oeptimizarion”（NRV）身上，在这一章中，我要托围编译器对于“对
象构造过程”的干涉，以及对于“程序那式”和“程序效率”上的冲由。
2.1DefaultConstructor的建构操作
C++ Anacnetef Rt;femmoe ifasmar (/μM) [ELLIS90] 中的 Section 12.1 告 5
我们：“dcfaiconrs在屏要的时做被编译器产生出来”，关键字服是“在
需要的时候”，被谁需要？做什么事情？看看下面这没程序代码：
ciass feo I pabLiei int val: Foo *pnext: 1:
roid foo_bar ()
if (/ bar.val 1I bar- pnext 1

谦注：以下文字的原文多次使用 inplkemmmion这个字服，许多时候它是指
“C++实现器”，也就是指C+·编评器，这种情况下我会将它直接泽为编译器

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p073_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 74

深度探素 C++ 对象横型 ( unide Tv C*+ Obincr Modr
在这个例子中，正确的程序语意是要求Foo 有一个 dcfaslt contsctar，可以
将它的两个 members 初始化为0.上面这税码可替符合 ARM 所说的“在需要
的时候”？答案是so.其网的差别在于一个是程序的需要，一个是编译器的需受
程序如果有需要，那是程序员的责任：本例要承担责任的是设计cas Foo的人2
是的，上述程序片段并不会合成岛一个 default constructor
那么，什么时候才会合成出一个defleeesnxter 呢？当编译器需要它的时
：此外，被合成出集的comtr只执行编泽器所需的行动，包氧是说，即债
有需要为 cles Fee 合成一个deflanl conmtecr，那个 conutuctor 也不金将两个
dses memben nal 和 pmear 析始化为 0. 为了让上一授码正确执行, clas Foo 的
设计者必领提供一个明显的daultcorwctor，将调个 membens适当地机始化
C++ Stundard 已经参改了 ARM 中的说法，显然其行为事实上骨然是相间
的。 C++ Standaed [IS0-C+*95] 的 Sestiee 12.1 这公说:
对于 clas X, 如果股有任何 user-declared coratructor, 那公会有个 deflni
tor 被增中（implicity）声明出案个被暗中声明出来的defial
uckor 特是一个 wivial （线腾图无能。设哈用的）coms
C++ Standand 然后开始一一氨述在付么样的情况下这个 implicit defist
seestrwter 会被视为 sivial-个 soetrivial defiault construcoer 在 ARM 的术语中
就是编译器所需要的那种，必要的话会由编评器合成出来，下面的四小节分别计
论 nomivil efiel ceestnuctor 的四种博况

2 Gibd ctjet 的内存保证会在程序量括的时禁被情为 0. Lood jxts配量于程乎的增
线中，hjnc配置于自由空间中，都不一定会被康为0.它们的内容特是内存上次能
使用后的遗速。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p074_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 75

第2章构港函数通盘学（TheSe

“带有Default Constructor” 的Member Class Object
如果个 claesn 没有任何 coscter，组它内含一个 mcmber cbjsct.面后者
有 defaslt onstrudter， 那公这个 cdaes 的 inplit dal coestseter 就是
“nomivial.编详器需要为此 clas 合成出个 defait cstwtor，不过这个
合成操作只有在consudtor真正需要被调用时才会发生
于是出现了一个有趣的间题：在C++各个不同的编泽模肤中（错注：原文
为 compilaioe mod, 是否为 compilation modals 之误1 不间的编评模块意指不间
的档案），编评器如何建免合成出多个dfaalt csmctr(臂如录一个是为 A.C 档
合成，另一个是为B.C 档合成）呢？解次方法是把合成的 dehukcomrnctor.cop
函数有静态链膜（micliaagr），不会被档案以外者看到，如果函数太复杂。不
适合做成 inlise，就会合成出一个 oplit soe-inline tatic 实体 （inlise 函数疼在
4.5节有比较详细的议明）：
举个例子。在下翼的程序对段中，编译器为clas&ar 合成一个 deu
class Foe ( publiet Foo(), Foo( int ) -*fooz char *stxr 1r // 性: 不是思承
是内含
veid foo_bar ()
拥有 defaultcoastroctor，排合本小节主趣t.西其c
if ( str 1( 1
被合成的 Br detuk conitrnctor 内含必要的代码，能够调用 can Foo 的
defial csictaer 亲处理 memer otjet Bar:jfoo，但它并不产生任有码来初始化
Bar:sr、是的。将Br:fbe 初始化是编译器的贵任，将&or: 初始化则是程

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p075_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 76

聚度素 C++ 对象根型 (Juidle The C++ Oliner Molel

员的费任：管合成的efaslco看起来可能律这样3
能金这样合
har:Bar()
/ c+-码
0011F09L1 7

再一次请你注意，被合成的dfault constrctor 只调足编译器的需要，而不是
程序的胃要，为了让这个程序升股能够正确执行，字符指针Ir选黄要被初始化
让我们最设程序员经由下面的 defe construdtor 提供了 ar 的初始化操作：
BaritBar(1 (I str = 0j 1
现在程序的肾求嵌得演足了，但是编译器还需要初始化memberebjecto
由于deflikcmntrnctar 已经被明确地定义出来，编泽器授办张合成第二个，“境
售整题”，你可能会这样说，编译器会采取什会行动呢！
编译器的行动是：*如果clas 4 内含个度一个以上的mcmber clas
objects, 那公 class 4 时每个 cosstructor 必须调用每个 member classes 的
dcfiult conitsdtor。编评器会扩张已存在的ostrncters，在其中安一热码。
使得user code 在被执行之前。先调用必要的 deal ceesmnucters.沿续前一个例
子，扩张后的 corstructors 可能像这样：
/ 扩报脂的 defaslt censtruetor
// C++D
Bar::Bar()
str = 0:  // explicit/前加上前

3为了是化我们的计论，这些得子都否略辣息合的 出b指付

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p076_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 77

第 2章转港i函B通盘学(The Scmantics of Coertnc

如暴有多个 class mcmber objccts 都要求 constructer 初始化操作，将如
呢？C++语言要求以“member cbjecs 在claes 中的声明次序*来调用各个
4ers，这一点含编译器完成，它为每一个comstrnctor安插程序代码，以
“mcmber 产明次序”调用每一个 member 所关联的 detsaltconsrnctos，这燕码
将被安插在 csplickaser code 之前，单个例子，最设我们有以下三个 chacs
ciass Depey( peLie: Dopey()s -.. 1r
class Bashfel ( pblici Bashful(r --. )/iess Smeezy
以及个 clam Ssew_#%r
elass Seeu_white (pubiie:
neety 相 bsshful 是三↑ nenbt

private:/.

如果Sno_Whie 投有定文defiulk contructor，默会有个nonti
muctor 被合度出来, 彼序调用 Depey Ssty.Buiyw 的 defait contn
然页如果Swow_%ie 定义了下要这样的 defult comstr
/ 程序员胶写的 6efaslt constnscter
now_Rsite::Snow_Msite() : sneetyt 1024 )
sunbLe = 2048/
他会被扩生为
// 糖课据R后的 detaait cosstructor/ C++ %
Ssos_shiter:tee_hite() : amnseny 1024 1
/ M人 nesber class object

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p077_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 78

尿度报浆 C++ 对象模型 (Anuide Zhe C++ Obyecr Mod

bashfel.tashfel:bashtsal 17

2.4 节将时论“调用implict defaulconutructors°和“调用明确条列于 mee
initialiatien lis 中的 constrnctars 之间的互动关系。
“带有Default Constructor”的Base Class
类似的道理，如果一个没有任例comstrdters 的 clas激生自一个“带有
dfaelt cotor* 的 bae dls,那公这个 erived clas 的 dfalt cwctor 会
被视为 nommial.，并园此需要被合虞出案，它将调用上一层 base chases 的
defaslt corstcdtor（根据它们的声明次序），对一个后继源生的 clas而言，这个
合成的conor 和一个“被明确提供的 del cstor”改有什么差异。
如果设计者提供多个costrnctes但其中都波有ocfuk cennuctr 呢！编
译着会扩强现有的每一个coesrctos，将“用以调用所有必要之dcrfaul
amuctors”的程序代码加通去。它不会合成一个新的 deult ceswaor，这是
因为其它“由uer 所提供的conitructors”存在的嫌故，如果阅时亦存在者“
有 defut ceetcton’ 的 mnber clas chjects, 那些 deful ctuctr 也会孩

“带有—个Virtual Function”的Class
另有两种增况，也需要合成出deflault contss
1. class 声明 （或继承)个 virtual fanotioe
2.class 生自一个继承申整，其中有一个或更多的virts

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p078_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 79

第2章转建国长适量学（TheS
不管哪种殖况，由于缺乏由wr 声明的 cmlars，编证器会评能记
合成一个 dedault constructar 的必要值息。以下脂这个程序片段为例

taal void flip() = 0)

// 很设 se11 和 whLstle 都除生自 widget
BeL1 by
histle Mr
flipt b 1
faipt v 1r
下面网个旷张推待会在编译期网发生：
1.一个 vintal functioe table （在 cfrost 中被称为 vibt）会被编译器产生
出来，内放 clas 的 vintal functiens 地址。
2.在每一个 class etject 中，个级外的 pointer mxmber （也就是vptr）
会被编泽器合成出案，内含相关的class vibl 的地址。
此外，witgnr,Ap)的虚损引发操作（vinlinrowion）会被重斯改写，以使
用 witger 的 vptr 和 vihl 申的 Ap(0 条目:
/ vi4pet,[lip() 的建拟引度操疗 (virtsal inmcat:ion)的精变
( *vidget.wptr( 1 1 1( widget )
其中
■1表示Ap 在 vitual able 中的医定索引：
widgr代表要交给“被调用的某个Ap)函数实体”的thi指针

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p079_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 80

深度授意 C++ 对童要型 (Jke 7he C++ Oher Mon)
为了让这个机制发挥功致，编译器必须为每一个Wigr（或其派生类之）
ctjoct 的 vptr 设定初值。 放置适当的 vintsal ablte 地址， 对于 clas 所定义的辑
一个comstrukar.编译器会安强一些码案做这样的事情（请看5.2节），对于称
些未声明任何csnuxtors 的 clases,编语器会为它们合度个 defaal
cter，以便正确地析始化每个 cams ebject 的 vpe
“带有—个Virtual Base Class”的Class
Virual base class 的实现读在不间的编证器之间有极大的差异，然而，每一
实现法的其通点在于必须使 vinal base clas 在其每一个 derived daes otject 中
的位量，能够于执行期准备妥当，例如下面这段程序代码中：
class 8 : pub1ie virtual x
class C : pub1se A, publie a ( palie: int k; 1

main(:
fso( 1= a 1 /)

编泽强无独型定住f0o0 之中“经由 pe面存取的X:”的实际编移位置
因为p的真三类型可以改变.编泽器必质改变“执行存取推作“的那些码，使x：
可以延辽至执行期才决定下象。原先 chont 的做法是靠“在derived cass cbjeo
的每一个 vital base clases中安趟一个指针”完成，所有“经由 rekrc
pointr 来存取一个 vinul bas clas的操作都可以通过相关指针完成，在我的例
子中，o可以被改写如下，以符合这样的实现重略
/可能的编诉器特变操布
roi4 feec eoast A* pa ) ( pa*>_vbcx->i = 1024: 1

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p080_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 81

第2章构造函数适盘学（The 5

其中 _ndek 表示编评器新产生的指针，指向 virtual bise clas X
正如作所量测的即样，_rbcX（或编译器所做出的某个什公东西）是在clas
object 建构期间被完成的。对于 clas 所定文的每一个conitructor，编译器会安
那查“尤许每一个 vitaal hee chs 的执行期存取操作”的码，如果clas 没
有声明任何comtrnctans，编译器必须为它合成个 deul conitrector
总结
有四种情况，会导致“编评器必须为未声明cettor 之 clases合成一个
dfslt cstctr* . C++ Smdnd 把那他合或物称为 inplicit norivil det
contes，被合成屈来的contucbor只能满编译器（百非程序）的离要，它
之所以能够完成任务，是量套“调用 member otjet 或 haoe clas 的 delault
constructor* 或是“为每个 objet 初始化其 vital finctiee 机制成 viral base
clas 机制即完成。至于度有存在那四种增况而又授有声明任何comtrcter 的
clases，我们退它们据有的是 implici tivial deult cosructens，它们实际上并不
会额合成出案。
在合成的 dealt coette 中, 只有 bee das stetjets 和 member chs
otjocts 会被初始化，所有其它的nsaicdaa mmber，如整数，整数指针、整
数数组等等都不会被初始化，这查初始化操作对程序面言或咨有需要，但对编诉
暴则并非必要，如果程序需要一个“把某指针设为0”的dedaul cos
么提供它的人应该是程序员。
C++新手一般有网个常芜的误解：
1.任何 clhass 如果授有定又 dcthault coestrcter，就会被合成出一个来。
2.编语器合成出来的deslt cerwcter 会明确设定*class 内每一个 da
如你所见，投有一个是真的！

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p081_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 82

探度报素 C++ 对象模型 (/mide 7e C++ Olyer Mode)
2.2CopyConstructor的建构操作
有三种情况，会以一个otject 的内容作为另一个 clas otject 的初值，最
显的一种情况当然就是对一个cbjct值明确的初始化操作，像这样

// 男确地以个 object 的内容作为另一个 ciass sbyeet 折初值
X xx * 3/
另网种情况是当objct 被当作参数交给某个函数时，列如：
estets *eLd f9o( x × 11
vesd bar()

/ 以 xx 参为 foo（) 第一个参数的抑值（不明显的初始化推行
00(xx 1F
7/.
以及当通数传图个 clas otject 时,例如
foo_bar ()

returs xxr

假设clas 设计者明确定文了一个 cepy coeatn
有一个参数的类型是其clastype），像下面这释：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p082_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 83

第 1 章构造通数运意学 （The Sem
//可以是多参教形式，其第二参数及追继参数以一个数认很决应之r的实贵
Yr:Yi eonst rs y. iat • 9 1:
那公在大部分情况下，当一个 clas atjent 以另一个同类实体作为初值时， 上
述的comsuctaer 会被测用。这可能会导致一个暂时性 clas object 的产生或程序
代码的航变（或两者都有）
Default Memberwise Initialization
如果 clas 投有提供个 explicit copy ceestnwcter 又当如间? 当 dhes otjct
以“相同das 的另一个 lycat”作为衍值时。其内部是以所请的drfaul
ervise inisalinfioe?生光成的。生就是把每一个内建的或系生的a
member(例妇一个整针度-效三组）的值,从某个 objsct 携见一份到另一个 ctjet
身上。不理它并不合博显式牛的 menber claes ctjet.西基以通归的方式施行
wie niixie,， 9如，为临下需过个 caes 产明;
c.esa Steia; (
// ... 我有 esplicit copy constructor
privatechar *str

个 Sring oltject 的 default memt  urion发生在这种情况之下
itzing verb = r
其完成方式就好像个别设定每一个
//语意相等

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p083_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 84

保度握家C*+对象膜型（/

class Word (

部公↑ Ward object  defiulkt mes  会秀美其内建

这样的推价实际上如何充成？ARM音诉我们
反展念上图育，对于个 casX，这个操作是被个copy

其中主要的字服是“概念上”，这个注释又紧跟着一些解释
一个良好的编译器可以为大部分 clas cbjets 产生 biwise capies.因为它们

也就是说。*如果个clas 未定文岛copy comor，编评器就自动为它
产生出一个”这句话不对，面是应该像ARM所说：
Defaul conuctars El copy cons在必要的时候才由编评器产生出来。
这个句子中的“必要”意指鱼 clas 不展现 bitvisc copy scmanantics B . C++
Sandard 仍然保留了ARM 的意文，但是将相关请论更形式化加下（括号内是我
的批注)：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p084_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 85

第2章构建函数通盘学（TheS

一个cdias cbjet 可以L反两种方式复制得则，一种是被初始化（生就是我们这
显所关心的），另一种是被脂定（asigmm，第s 章讨论之），反赛念上面言。
这两个操作分期是以copy com  emt operator 光成的。
Bt像象 defaslt consttructor 一样, C+* Sandard 上说, 如果 clas 设有声明个
ntcter,就会有隐含的声明（inplily docland）或助含的定文（inpicity
defeed) 出成, 和以能样,。 C+* Standed 把 cepy constrwor 区分为 sivil 和
drisial 两种，只有 momivial 的实体才会被合度于程序之中，决定一个copy
ncter是否为sivial 的标准在于 cdlas 是否展现出所调的“biwis copy
aeies 。 下节我将使明 “das 最藏比 bitwis capy memawics” 这句运是什
么意思。
Bitwise Copy Semanties（位逐次拷贝）
在下国的程序片级中：
itclsde *ierd.h*
Wee soue( *boek* 1J
void foo()
Merd vetb = nostf/.-
很明显wrb是根据ma 来初始化。包是在尚术看过 cdhas Ford的产明之能
我们不可能预测这个初始化操作的程序行为，如果clasFord的设计者定义了一
个oopy contuctor，werb 的初始化操作会调用它。但如果谈 cas 投有定又
pickcopy coestcor，那么是否会有一个编译器合成的实体被调用呢？这款得
核该 clas 是否展现“tisvise opy semanter”面定，举个务子，已知下面的 chaun
Ford 声明:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p085_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 86

深度据董 C++ 对象模型 (Auide 7he C++ Otyecrt,&
// 以下产明摄度7 bitvise copy sesastics
:lass Word I

erd[ ( delete [] strz )

nt

这种殖况下并不需要合成出一个achk copycomuclaor，因为上述声明展步
了“dfaut copy sematios，面 werb的初始化操作也算不需要以一个函数调用
收物4.然面，如果cdas Wonrd 是这样声明：
pablie:Moed (
//-1

其中 Sering 声明了一个 oplicit

String(1:

4当然，程序的执行将因为 clas Wiond 如此宜告面支情协重，如今 locl ct
global otject nosr 都指内都网的字界事。 在退岛 d 2前, loal otjntwrb会执
stractor.干是字并串管数排，gobelo从此泰向一堆无意又之物以改写f
tice”或是想“不允诗完全情员”都决之，不过这和“是否有

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p086_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 87

在这个情况下，编译器必渠合成出一个000y0以度调用m

/C++
inine Weed::Weed( const Mords wd )
str .5tring::8triag( wd-str 1/

有一点保值得注意：在这被合成出来的cagy cmnutaor 中，如整数、指针
数组等等的moncasmemhes 也都会机复制，正如我们所期持的一样。
不要 Bitwise Copy Semanties!
什么时候一个 cdas 不展现出“bivise copys°呢！有器种情况
1. 当class 内含一个 mmber ohject 百后著的clas 声明有个 copy
costrutor 时（不论是被 clan 设计者明确地声明，就像指图的 Soring
那样：或是被编译器合成，蒙clais Word那样）。
2.当class 继承自一个base class 离后者存在有一个 coy con
（再次强调，不论是被相确声明或是被合成而得）
3. 当 clas 严明了个或多个 virtual functioes 时,
4.当class 派生自个是承申链，其中有个或多个virtual base class
推可冲维况中，编洋器必级将 metber 或 he clas 的eopy contruciaors 调
用操作”安循到被合成的 cepy cststor 中，前一节 clas Wend 的“合成而得
的 copy conmnsctor”王是以说明模况1.情况3和 4有点复杂，是我接下来要
罚艺的题目。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p087_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 88

尿度探素 C++ 对象模型 (Ierife Thr C++ Obyecr Mode

重新设定Virtual Table 的指针
回忆编详期同的两个程序扩强操作（只要有一个chas声明了一个或多
irtual functioes 就公如此) :
增加个virtual faectioe table ( vtbl) ,内含每个有用的 virtsal
fusctioe 的地址
■将个指向 virtal funetiee table 的指针 （vptr)，安销在每个 clas
object 内.
很显然。 如果编泽器对干每一个新产生的 claes ohject 的 ver 不能成功面正
确地设好其初值，将导致可怕的后果，因此，当编译器导人一个vpe 到clas之
中时，该clas 就不再展规 binwie smaties了，现在，编译器需要合成出一个
copy comsoudtor， 以求将 vptr 适当地标始化。下面是个例子,
苗先，我定文两个 classi. Zoo4xiomar/ 和 Beer
clas toeiai (
virtsal
virtual void aniaate (1)

rittual vold datcel):/评理：虽末明写virtca1.它其实是virtca

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p088_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 89

private:
// Bess (6 aniaate() B dtee() 8 dancet
//所需要的教星
ZooAxinal claes otjfet EL另个 Zoodnina claes otjet f节为初区。 或 Bear
clas ohjet 以另一个 Bear chas stjet 作为初值, 都可以直接容 “biwise copy
artios”完成（除了可能会有的 peinter mebar 之外，为了黄化，这种情况被
我期）。举个例子：

被设定指向 Ber dlas 的 vintl tublc（靠编译器安描的码完成），四此，把g
的 vpr 值携灵给 winmir 的 vpt 是安全的.

英vpr复制操疗也必领保证安全，完始：
bumalfranny * yogi;// 讲注:这会发生智期(slioed) 打2

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p089_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 90

探度探索 C++ 对象模型 (buie TIe C++ Objincr Moder)
pony 的 vpor 不可以被设定指向 Sear chass 的 vintual able (组如果 yogi 的
vpe被直接“biwise oy”的话，就会导致此结果）。否则当下面程序片段中的
dho被调用面Prany 被传进去时，就会“得度”（blowup）5,
roid dtaw( const Eooknimals soey 1 I soey-deaw()) )204dtoo111
/ trasny 的 vptr 童R teoAnina1 的 virteal table

dravt fransy 1) // 黄8 to

Ismul 实体街非 Bear 实体（孤
上  作为错值)。因为月
（成如果它是一个指针，而其值为ygr的维址），那么经由
才会是Br的通数实得.这已在13 节计论过

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p090_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 91

第2章转造通数意学（Tc5
也就是况，合成出亲的 Zodnima copy cestnter 会明确设定otject
pe 指向 Zooininmaf clas 的 vintal uble,图不是直楼从右手边的 chas otject 中
将其vpe现殖携关过来
处理Virtual Base Class Subobject
Virtal bae clas 的存在需要特到处理。一个 clas otjou 如果以另一个

每一个编译器对于虚拟继承的支持承诺，都表示必领让“derihed clas objict
中的vintul basecdlas sheet 位置”在执行期就准各妥当，维护“位量的完整
性”是编译器的责任，“Binwis cop wmantics”可能会破环这个位置，所以编译
暴必渠在它自己合成出来的cpycmtnctr中做出仲藏，举个例子，在下面的声
期中, Zootsinmal 度%为 Racmon )个 vinual hene clas
class Racooen 1 peblie virteal IooAninal (//la: re
pubile:
//.
//张有必要的管据
编译器质产生的代码（用以调用 Zooinmal的dealt comtrwctar、将
Raccon 的 v 初始化，并定位出 Racon 中的 ZsAnima nubotjkct）被安插
在网个Racccors之内，成为其先头部队
那公”mmervise 初始化“现喂,个 vital hase clas 的存在会使 hinwi
Copy sicrmics无效，其衣，间题并不发生于*一个camotjnot以另一个间类的
ctject 作为初值* 之时，而是发生干*一个 cas ohbjeu 以其 4r

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p091_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 92

深度图浆 C++ 对象根型 (Jile Mhe C++ Oyercr Molef)
为初值, 百 Au/sxde 声明如 下:
anda ()( /* R定 peivte dsta 机量 •/
7.
//张有必要的数区
puqac
我再强调次，如果以一个Racoo oject 作为另一个 RacobjectPi卡
鱼，那会“bivise cepy”就增辨有余了:
/ 贯单的 bitvi.se cepy 意E# 7
hacesoe 1itti_ritter * rocky:
然而如果企脂以个 RenPanis ctjen 作为 inle_oriser 的初度， 编译器C
须判斯“后续当程序员企围存取其Zooinimal subotjet 时是否能够正确地执行”
（这是一个理性的程序员新期登的）

// virtual base class poin
oe littla_critter = 1ittie_red;da 1ittie_re6i
在这种情况下。为了完成正确的Anmlk_rir 初值设定，编译器必须合成一
个 copy coter, 变错些码以设定 vintal hecla peerfe 的初值 (成
只是觉单地确定它投有被抹摘），对每一个 memes 执行必要的memberwise 初
始化操作，以及执行其它的内存想关工作（3.4 节对子 vinalhaee dasos 有更讲
爆的讨论），

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p092_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 93

第 2 靠相造运数语意学 （The Sc

在下面的情况中，编详器无屈知道是否“biwise copy smarsios”还保持者，
图为它无摇知道（没有流程分析）&acoe指针是否指向一个真正的Raccor
/ 黄单的 bitvise copy 可能都用。也可能不够用
1ittie_crister • *p5x7*ptr2
这里有一个有建的问题：当一个初始化操作存在并保持者“bitwis cop
smatis”的状态时，如果编语器能够保证cdbjen 有正确西相等的初始化操作
是否它应该压抑copycomatrnctr的调用，以使其所产生的程序代码优化？至少在
合成的copycomsructer之下，程序剔作用的可能性是零，所以优化似乎是合理
的，如果copy comsntor 是由cas设计者提供的呢？这是一个额有争议的题
目，我将在下一节结京需国案计论之。
让我做个总结：我们已经看过匹种情况，在那些增况下caes不再保持
bivis copy manics”，面且_defiut cepy comtrnetr 如果末被声明的证，会
被视为是mil.在这四种增况下，如果献乏一个已声明的copycmr：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p093_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 94

探度探素 C+* 对象模型 ( /eide 7e C++ Ohyecr Modr
编译器为了正确处理“以L一个 clas cbjen 作为另一个 clas ctjct 的初值”，
略，以及这查策略如何影响我们的程序。
2.3程序转化语意学（Progm Trstormtionseantics）
已知下离程序片段：
#inciude *K.h*
x foo-()
X xI/...
return xs;

一个人可能会数出以下银设：
1.每次fo0被调用，就传目xx 的值
2.如量clasX 定文了一个 copy coastrctor，那么当foe0 被调用时，保
证该cepy constrcter也会被调用，
第一个最设的真实性，必须视classx如何定文面定，第二个根设的真实性
强然也有部分必领机casX 如何定义而定，但最主要还是视你的 C++编译器员
提供的进取性优化程度（deef gsive optimiatioe）面定，你基至可以最设
在一个高是质的 C+编添器中，上述两点对于 claesx 的 moetival defaiien
都不正确，以下小节将讨论其原因，

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p094_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 95

第2 章构进函数通意学（The5oics ef Car

明确的初始化操作（Explicit Initialization)
已知有这样的定文
x x0
下国有三个定叉，每一个都明显地以x9来初始化其
void foe_bar1)(
x1( x0 1:K x2 = x0;  //弹速，竞文了 x1
K x3 = x( x0 1/
// ..

必要的程序转化有两个阶段
1.重写每一个定文，其中的初始化操作会被新除。（弹注，这里所调的“定
文”是指上述的xJ,x2.xJ 三行：在严谨的 C+用词中，“定又”是
指“占用内存”的行为）
2. class 的 copy costrsctor 调用接作会被安插进去。
举个务子，在明确的取阶段转化之后，/oo_Aar0可能看起来像这样：
7/可载的程床特换
//c++物

工X3://驿注：定又积重写，相始化票作被刺除
// 编济著安猫 x copy contruction 的氨形鲁作
x2.X::X( x0 1)1X±:x( x0 12
x3.Kt1K( x0 12
/...

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p095_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 96

探度指素 C++ 对象模型 (Aeile 7he C++ Oyeer Mx
其中的：
x1.3: :X( x0 1
就表现出对以下的cepy oarxter的调用：
XI:X( censt Xs xx 1/
参数的初始化（Argument Initialization)
C++ Sadand(Setio 8.5）说, 把个 cles stjoct m做参数传始个函数（或
是作为一个需数的退签俱），相当于以下形式的初始化操作：
X sx * argr
其中代表形式参数（或返图值）面arg代表真正的参数值，因此，者己
知这个函数：
ve56 toe( X x0 1)
下面这样的调用方式

foo: xx 1
将会要求周都实体（local intancc） x) 以 menbewie 的方式明 当做初值
在编译器实现技术上，有一种策略是导人所谓的暂时性etben，并调用copy
ceedtnctor 将它特始化，然后将该暂时性otbecu交地函数，例如将前一段程序代
码转换如下：
//编讲器产生出来的智时对象/ c++码
x_temg0
temp0-K::K( xx )1strecter 的调用

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p096_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 97

第2章购造函数证退学（Thc
/ 重新改写通数调用操作。以便使用上述的管时对象_temp9 1r
然面这样的转换只做了一半功夫而已，你看出残留问题了吗？同题出在
的声明。暂时性otjet 先以 clas X 的 opy conutncaer 正确地设定了初值，然
后再以 biwite方式携贝列 so这个网影实体中，确，真付氏，Ao的声明固页
生必渠被转化，那式参数必须从源先的一个 cham X otet 改变为一个claes
ce.像这样：
Peid foo[ 36 x0 13
其中clasX 声明了一个 destnter，它会查foo0)函数元成之后被调用，对付税
个智时性的object
另一种实观方法是以“携关建构”（copycomtct）的方式把实际参数直接
建构在其应请的位置上，请位置视函数括动范图的不网记录于程序堆栈中，在函
数返图之前，局部对象（local otject）的desrnwtaer （如果有定文的话）会被执行。
Borland C++编泽器就是使用此注，但它电提供一个编译选项，用以指定前一种
缴法，以便和其早期版本兼容。
返回值的初始化（ReturnValue Initialization）
已知下面这个函数定文：
x bar )

returnsx:
你可能会问bar)的返因值如何从局部对象x中携员过来？S
中的解决方继是一个双阶段转化：
1. 首先加上一个额外参数，类型是clas object 的一个 refere

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p097_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 98

数将用亲放置被“携贝建构（coy consirwcted）”而得的返润值
2.在renre 指令之安据一个copy constractor 调用操作，以便将微传现
乙o动ect的内谷当量上述能理参数时制值
高正的返团值是什么？最后一个转化操作会重新改写函数，使它不传回任何
留.根据这样的其法。bar) 转换如下;
//函数持费
/ c*+物病/ 以反胰出 eepy coestructor 的度用
roid
bar( X% __result 3 // 导性: 加上个量外鲁董

/ 编译鲁产生的 default constructor 误用摄作
// -.. 此理 x
/ 编济著表产生的 copy o
return/

现在编译器必须转换每一个har)调用操作，以反映风斯定义，例如
X xx = bar (17
请被转换为下列网个指令号

bar() neefetc (): // i生- 执行 ber(1 斯特量

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p098_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 99

第2章构是函数酒意学（The Sesmics ef Cn
可能被转化为：
// 编泽器所产生的暂时对象
( bett _tenpo 1. _tempO 1 .nefenc(1;
同样道理，如果程序声明了一个函数哲针，像这样

它生必渠敏转化为
void ( *pt 1( Xs 1/

在使用者层面做优化（Optimization at the User Level）
我相信始作通者是 Jonahae Shopio：他对于像 bor) 这样的通数，最先提出
“程序员优化”的观念：定文一个“计算用”的contwm，换句话说程序不
再写：
X bar( censt T iye const T sz )
X 107// --  y 相 ± 兼处度 xx
cetuEn sx:
那会要求xr 被“mxmerwise”地携贝列编译器所产生的_nir 之中，Jonal
定文另一个conntrctor，可以直接计算xr的值：
x bar( const T sy, const T 42 1
reters X( y. ± 1/
于是当bor)的定又被转换之后，效事会比较高：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p099_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 100

深度探素 C++ 对象模型 (/eile 7he C++ Olyer Madrv)
/ c++码
// 课注; 上hi@者度为 bar ( X s_resat, const T sy。 cbar( x s_teselt 1
reternysslt.E1X( y, 1 1J

_eni 被直接计算出来，面不是经由copycomsrwctor 携员而得：不过这种
解决方法受到了某种世评，的那些特殊计算用途的contrckor可能会大量扩散
在这个层图上，clasn的说计是以效率考虑居多，面不是以“支持抽象化”为优先
在编译器面做优化（Opimiztis t the Compiler Level）
在一个如 hor) 这样的函数中，所有的rtlrw指令传民相同的具名数值（译
注：mamed valse，我思作者意指p.64的xx），因武编译器有可能自己做优化，方
法是以 ren 步数取代 namef ntum vale。 例如下图的 br0 定义：
x bar()
- 论母 x
rvtecs xu
编译着汇其中的 x  _nmab 取代:

_result.X:1X11 :

eturni

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p100_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 101

第1章我造通数适盘学（The Ses
这样的编语器优化操作。有时候植称为 Named Betsm Valae （NRV）优化，
在 ARM Sectis 12.1.1.c(300-303 页）中有所描述。NRV 优化如今被税为是析
准C++编译器的一个文不容持的优化操作品然其需求其实疑路了正式标准
之外，为了对效率的改善有所感觉，请你想想下面的码：
class test (frieed test foe( double 1:
pabiie:
test ()
privatei  ole317I
desble array[ 100 1;
网时请考虑以下通数，它产生、修改、开情民一个stca

test 1oca1/
10ocal.array1 0 1 - va1)
retuen 1ocali

Mais()
( test t = foo( douole( ost 1 3/ 1
retars 0;

这个程序的第一个服本不能实通NRV优化，因为narcas敏少一个cop
ucter. 第二个版本加上一个 inine copy ceestructer 如下

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p101_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 102

尿度浆素 C** 对象模型 (de 7%r C++ Oyer
teat:iteat ( oonst test st )LnL1se
encpy( this. st, siseot( test 1 1)
/ 课进：照每了在 ciass test 的声要中加个 s
//  ialise test ( oost teet st 1/
这个 c0pgy c08nctar 的出成激括了 C+ 编泽器中的 NRV 优化.NRV 优
化的执行井不通过另丹独立的优化工具完成。下面是测试时间表（除了效事的改
卷，另一个值得往意的或许是不同编评器间的效率差异）：
Named Return Value (NRV) f优

虽然NRV优化图供了重要的效率改养，它还是他受微评，其中一个原医是。
优化由编译器默款完成，而它是否真的被完成，并不十分清整（园为很少有编译
会说明其实现程度，或是否实现）（请注见下页），第二个原沉是，一丑函数
变得比校复杂，优化也就变得比较难以地行，在cbomt中，只有当所有的nod
rctsm 指令何发生于通数的sp kvl 时，优化才随行，如果导人“a ncsted loca
block wihartum strmt，chom 就会静静地将优化关用，许多程序员主涨对
于这种情况应该L“特握化的comtructor”策略取代之。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p102_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 103

第 2 章 梅进通B道89 (The 5es
请注：我许MiV就是Ln所说的即种编器，
把前述的序随例以VC5.0编译后执行，在Pn133.96MBRAM的机
上得结果加下，加上yr之后，授应实RV优化，结
效率反图差，于不知L付种硬设备上测试CC和NCC.所以我
的达些数不应来和上一页的两试结果比校，只宜就NRV之实施与未实
种博民比权之
Named Return Value ( NRV)(t
未实NRY实NRV实NV+-0（详注，-O表示优化）
VC221  2:26.42:20.8
译注，饮再知程序所费间，可mm）的forbp先后调用
时间函数）：
icletip.ftotprantt
7/tortisel)andlocaltine(
(vodd)
atrscts*tineptr
tine.tsecsnow
tineptt-iocaitine（se
0--4,/人2，这h70
(tiaectz->tn_mon）.1,
Rlseptr-stn_mtayl.
itisngtr-)tn_yearll)
prntLc t2d:a:02n,
cimptz-)tauain,tineptr-9ts_hoer
tinnptr-teeec17

9

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p103_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 104

探度探素 C++ 对象模型 (Jade TIv C+ Oher Modl)

上述两个批评主要关心的是编译器可能在施行优化时失数。第三个批评到是
及相反的方向出发，某些程序员真的不喜放应用程序被优化，你知道他们泡想付
么吗想象作已经据好了你的oy ceswter的阵势，使你的程序“以lcoin
方式产生出一个ctjee 时”，对称地调用 deitnctor，例如：
veid to9-()
bar ();
//这盘满用 destrscter
在此情况下，对称性被优化给打破了：程序虽然比校费，却是错误的，难
道编译器因为压抑了copycontructaor的调用面在这里出毛病吗？电就是说，准
道copy cosrustor 在“objot 是经由 copy 面完成其初始化”的增况下，一定
要被调用吗
这样的需求在许多程序中可能会被征以严格的“效率税”，例如，虽然下面
三个初始化排作在语意上相等：
X xox9( 1024 11
X sx2 = ( X ) 1024;
但是在第二行和第三行中，语递明显地提供了两个步键的树始化操行。
1. 将一个暂时性的 otject 设以初值 1024:
2.将智时性的otbjet 以携美建构的方式作为 explici object 的初值。
换句语说，x0是被单一的constrncter 操作设定初值
// C++物病
x80.: :X ( 1024 1 =
面 ax7或 xx2 却调用两个 comstructor。产生一个管时性 object。并针对该管时

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p104_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 105

第 2 章构是函数通意学（Thke S

1.KX15eng0.K1:3.1 1024 1:
_temp0.I: :K(1
C+标准委员会已经讨论过“剔除copy coutrncor 调用操你”的合法性
在费下笔时刻，要员会还授有最后的决议，然图，装照hoseeLajee（委员会副
主案，网时也是 Cone Langag goup 主客）的看肽，NRV 优化非管重要，不应
请敬图，很明是这项时论已经连系导放多少特点神秘气息的两种情况：是否c0
cetructer 的剔腺在“携员 stutic otjct 和 local etje” 时生应该度立？ 列如,
已年下面程序片段：
Thsng oater/
/ 可以不加考建 insez 明
Thisg itoet( otet 11
inep 应该从sobr 中携贝构造起来。或是inmer可以摘单地被忽略？这
的问题可以很类似地客问在有关maiceemobjcts 携贝构造的操作身上，彼照
losee 的看法，要消除mtic otyect 的opycctor，儿手可以背定是不被允
许的。至于 automatic otjects 如 ioner，则尚末决汉.
一般面言，面对 “以一个 claes otect 作为另个 clas objcct 的初组” 的增
形，语言允许编译器有大量的自由发挥空间，其利益当然是导致机器码产生时有
明显的效率提升，缺点则是作不能够安全地规划作的copgy conutrvctor的副作用，
必须视其执行面定。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p105_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 106

探度探素 C++ 对象模型 (fmide 7he C++ Ohyecr Mod
CopyConstructor：要还是不要？
已知下真的 3D 坐标点类
c1ass Point3d (pcbLieI
peist3et fioet s, float y, flset z 1:
flsat _8- _7- _μ1
这个 cles 的设计者应请提供一个 oplici opy contrnctor 周
上述 clas 的 dealt coy comtrnctr 被税为 tivil. 它既没有任何 mcmh
(或 base) clas ehtjents 带有 opy coenctor, 生股任何的 vintual bac clas 或
vital fnctiee. 数以L, 数认情况 下, 个 Ponrid cas otject 的 *membervist
初始化操作会导致“bitwise co”，这样的效率提英，但安全吗
答案是 yei.三个坐标成员是以数便末储存.bitwise copy既不会导致memr
lak，不会产生 ades alasing，图此它既快建又安全。
那么，这个 chen 的设i者应读提俱一个 eapidat opy coeatructer 码？ 你将如
何目答这个同题？答案当然程明显是o，投有任何理由要你提供一个co9
stor函数实体，因为编还器自动为作实地了最好的行为，比较难以网答的是。
如果作被问及是否预见 clan 需要大量的 mcmberwise 初始化操作，例如以传值（b)
vae)的方式传因 etyet？如果答案是 yei，那么提供一个 copy coesnctaor 的
rpict inline 函数实体就导常合理—在“作的编评器提供 NRV 优化”的期提下
究如,Poit3f 支持下翼一组的数:
Peist3d opereter*( censt Point3ds, const Peint3ds 1)const Peint3ds 17
Peist?e eperatoe* ( consat Psint3ds, Lat 1)

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p106_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 107

第 2 章构造函数通意9（TheSo
所有那造函数都星够疫好地得合NRVtcmpla

returh resuit:

实现copy constnucter 的最黄单方法律这样
Poist3d: :Poiat34( const Poiat34 szha )
_x • ths-_xry = ths-_yi
_t = rbs._1/

这没网题，包使用 C++ Sibrary宝更有收率
Peint3eiiPeint?d( cssst Peint3d szhs )
Mncpy( this, 4rhs, siseef( Posst3d ) 1/

然面不管使用mmgy) 或memee)，都只有在“clases不含任何由编泽器
产生的内部 members时才能有效运行，如果Point3f clas 声明一个成一个以上
的 vintal fintioms， 或内含一个 vitul hae chas, 那么使用上述函数将会导致系
些“被编译器产生的内那mmben”的析值被改写，例如，已知下面声明。
cless 5hape (
//吸欧，这会改变内部的vptr
// ..--Shape ()1

编洋器为此constructor扩账的内容看起来像是：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p107_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 108

探度探素 C++ 对象胰型 (/mde 7he C++ Oher Md

Shape::Stape ()
//tr必须在使用者的代码执行之前光设定要
_vptr_Shape * _vtb1_Shape/
/ 霍B: nemaet 会将 vptr 为 0
sset( this, 0, sizeof( Shape ) 12
如作所关，著要正确使用mzmet)和mmy)，需得掌握某些C++Ohjoct
Model 的语意学知识！
摘要
copy omtcter的应用，造使编译器多多少少对你的程序代码做部分转化.
尤其是当一个通数以传值（by valbue）的方式传圆一个 chas cjet，面该 clas 有
一个copyomncter（不论是明确定又出来的。成是合成的）时，这将导致深类
的程序转化—-不论在函数的定义或使用上，此外编译器也将copy comstructor 的
调用操作优化，以一个额外的第一参数（数值被直接存放于其中）取代NRV.程
序员如果了解那费转换，以及copy coesrnmnxctor优化后的可能状态，就比较能等担
制他们的程序的执行效率。
2.4成员们的初始化队伍（Member Initializatioe List）
当你写下一个 cosarwter时，你有机会说定 cdas mxmbars 的初值，要不是
经由 menber inialiation lit,就是在 contructar 函数本身之内。除了四种情况。
你的任何选择其实都差不多，
本节之中，我首先要澄遗何时使用 iiaiiee in才有意文，然后解辑 Iis
内部的真正提作是什么，然后我们再未看看一些重妙的陷阱。
下列情况中，为了让你的程序能够被原利编译，你必须便用membe
laitialization list:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p108_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 109

第2章构建函数活意学（The Sn
1. 当初始化个 reference member 时:
2.当折始化个const mcmber 时：
3.当调用一个 base class 的 costructor，面它拥有一组参数时；
4. 当调用个 member class 的constructor,而它拥有组参数时
在这西种懂况中，程序可以被正确编译并执行，但是效享不影，例如：
class Word (
int
/我有错误，只不过大天

在这里。Wor eetntor 会先产生一个餐时性的 Srg object.热后将它树
始化。再以一个 asigmat 运算符将暂时性objc 指定给_ome，然后再推置
那个暂时性ctjel.这是放意的吗！不大可能，编译器会产生一个警告吗？我不
知道！以下是contuctor 可能的内那扩张结是：
/
ase-String: :String(1:
//产生智时性对象
stting tenp = Strisg( 0 1/
/ *sesbervise* 地济员 _sas
//握蛋智时性对象
,cst = 0;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p109_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 110

尿度指R C++ 3&要型 (Auidr Zhle C++ Oly.Moe

对程序代码反复审查并修正之，得到一个明显更有效率的实成方出：
/较理的方式
_cst = 9
它会被扩张成这个样子：
/ c++物6foad:rssed: /* (his pointer goes here */ )
/ W册 string( int )Eng118tring( 0 12
_cnt = 0,

//可能是（也可能不是）个好主//视yp的正类型图定

这会号净某经程序员十分积极进取地坚持所有的member 初始化操作必须在
nber iriainrcn lar中完度，著至即使是一个行为良好的 mmber 如_(Ml
//持此种写有风热
= _ent( 0 1, _nase( 0 )
在这里我们不禁要提出一个合理的间题：mmberiniaization lin 中列底会发
生价么事情？待多 C+-新手对于 lit的语丝感到述感，他们说以为它是一组函
数调用，当然它不是！

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p110_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 111

第 2 章 构建函数通意学 (The Ssmatics of Con
编译器会——操作 inifaliation is, 以适当次序在 comsrncter 之内安插初始
化操价，并且在任何opicit user codc 之期。黄如，元前的 Ward contr
扩充为
/ c-erd::Meed( /* thss pointee goes here */ )
pane.itring1:8triag( 0 17
ent = 0i
.，它看起来很像是在cuctdor中定cnu的值，事实上，有一
定，不是白
被产明于_cm之前，所以它的树始化电比授早。
“初始化次序”和“initasiarion lit中的项目排列次序”之间的外现错及
会导放下面意想不到的危险：
class X (int i7
//呢欧，你看出问题了吗？（弹注）
1 1t val ), kt 3 1
( 1

课注：上述程序代码看起来像是要肥设初值为nur，再把：设初值为j
问题在于，由于声明次序的嫌妆，inialixionlian 中的6）其实比(ra） 更早执
行，但固为）一开始末有初值，所以i）的执行蜡果导致；无法预知其值。
后我会判出一个完整的范例

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p111_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 112

深度例素 C++ 3象模型 (Juidle The C++ Oljirer.Modlel

这个“类立”的置维度在于它很不容易被观察出来，编译器应该发出一个警
告简息，但是到目案为止我只细道有一个编评器（g++，GNUC++编译器）做
到这一点，我建议你总是把一个member的初始化操作和另一个肢在一起（如果
你真党得必要的话），放在contrsctaor之内，像这样：
//比较受到基爱的方式
X::X( iat val )
: 3( va1 )
1 = 1/

请注：现在我把有始供的写法和更改后的写法放到一个程序中去，检验其组果：
9o01inelude <stdio.h)0002
0003class X
10004public:

: 3（ va1 1, 1( 3 3 // 建脂膜的写胰

2as8 T 1

[ 1 = }; 1//修改后的军

6不率的量，写出这个需告消息的人告诉我，在看过上述一小教文字（首次出现在我所主
专世）之街，他的器队中段有人了解多个警告消息的真正意文

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p112_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 113

第2 章构建函数逐意学（The 5o

0025
9021 x(313
0029
903003priMEi"y-1 = h y·) *
retsrn 0r
执行结果如下：
y.t + s y-3 * 5×.] * 3
果然x的值不是我行期的3

这整还有-个要的题，hdianioe lin 中的项目被安描到 cont
中，会继增保字声明之序码？就是试，已知：

: 3( val )
1 = 5r
/的初始化操查会安腊在 explicit user asignme 操作（/ - J） 之能成之后 如
果声明次序续被保存，则这份码大大不妙（谦注，因为势必要先用：初始化再
将初始化）。然面这份码其实是正确的，因为 initiaianioe lin 的项目被放在
cxplicit user code 2前

设定一个 member 的初值

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p113_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 114

深度探素 C+* 对象模型 (nude 7or C++ Obiecr Mo
// X：xfoo1) 被调用.这样好码
X::X( Lnt va1 ): s( axfee ad 1 1, 3T val
{1
其中 xoo) 是x 的个ember funtias.答案是ye但是時之
加上但是，是因为我要给你一个患告：请使用“存在于comsmcor 体内的一个
menbe*， 而不要使用“存在干 mamber itiaiaio list 中的 mnber” ， 末为
另一个mmber 设定初值，你并不年道 yjoo) 对X otjet 的依赖性有多高，如
果肥yfoo) 放在conituctar 停内，那么对于“到庭是哪-个 member 在x69)
执行时被设立初值”这件事，就可以确保不会发生模被河可的情况。
Menber fanctie的使用是合脂的（当然我们必须不考虑它系用到的membm
是否已初始化），这是因为相此etjes 相关的 ths 指针已经被建构蛋当，而
actor 大的被扩充为
// c++ h例 est扩充后的结
Li i3( /* this peinter, */ iat val )
1 • this-7afoet al 11
1 = vaLI
最后，如是一个 derived casmamber funcion 被调用，其返目值被当做 bui
scesmuadar 的一个参数，将会如间：
/ 调用 rooar:Eval() 可以码1 pbie ×1
Lat _fva1  // i注-base class 是 :

X(val () 1// #, (val () 鲁为 base cLsss
constructor meh1 1

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p114_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 115

你认为如间！这是一个好主意吗？下面是它可能的旷张继果：
/ C++物药
//呢数：实在不是一个好主8
Eva2 = va1.

izatien lint中的初始化程序会有比校详罪的说明）
简略地说，编译器会对 iiialization lit一一处准并可能重新排序，以反映出
sbers 的声明改序，它会安插一感代码到 consiructor 体内，并置于任例epici
user code 之前

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p115_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 116

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 117

B ) 章 Duea 语意学 (The Semanti

第3章

Data语意学
(The Semantics of Data)

非些时餐我收到一封案自法国的电子部件，发信人如平有些进情也有查额
乱，他志惠（要不就是被选图）为他的项目团队提供一个“永恒的”Iibrary，在他
准备工作的时候，他写出以下的码并打印出它们的megf 继果：
lass X112
class a : public T, pebiic 2 ( 1r

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p117_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 118

探度探案 C++ 对象膜型 (Auudre Zhe C++ Oyeer.Ms

上述x1, Z, 4中授有任例一个clas 内含明显的数据，其间只表示了是果
关系。所以发倡者认为每一个 cas 的大小都应该是0.当然不对！即使是cdla
X的大小包不为0
sL3eoE×的结量为 1
sLeoE的果为6
siseoE A 的继果为 12
让我们依次看看每一个clas的声明，并看看它们为付么庚得上述结果。
一个空的 class 如
elass  E 1r/ siveof X == 1
事实上并不是空的，它有一个急确的1by，那是被编译器安插进去的一个
hr、这使得这个cas的调个cbjocts得以在内存中配置睡一无二的地址：
If (sa ** sb) cerr c< "yipes1* cc end1s区 a,b1
令案信设者感到值误和图费的，我怀疑是Y 和Z的ruvf 结果：
// siseot Y ea siseot z ** 8
class I : psolie virtual x ( 1:
在来信者的机器上。Y和Z的大小都是8，这个大小和机器有关，也和编证
都有类，事实上Y和Z的大小受到三个因家的影响
1.语盲本身所造成的额外负报（everhead）当语言支持virtal base
classes 时，就会导取一费额外负图，在derived clas中，这个额外负担
反映在某种形式的指针身上，它或者指向virtual base class subobject，读
者指向一个相天表格：表格中存放的著不是virtualbaseclass subobjec
的地址、就是其移量（eet），在来值者的机器上，指针是4by

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p118_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 119

第 ) 童 Owx i8.8牛 (Thc Semantis ofData)
(我将套 3.4节讨论 virtual base class)
2.编译器对于特殊情况所提供的优化处理Virtual base clas X subob
的1 bytes 大小也岛现在 clas  和 Z 身上。传扰上它被放在 derive
class 的固定（不变动）部分的尾增，某些编泽器会对 empty vitual base
class 提俱静殊支持（以下第3点之后的一段文字对此有比较详细的讨
论）。来信读者所使用的编详器，显然并未提供这项特殊处理
3. Alignmemt 的限制clas  和 Z 的大小载至目病为 5 hbytes.在大部
分机器上，群象的继构体大小会受则 alignmmt的限制，使它们能够更
有效来地在内存中被存取，在来位读者的机器上，alignment是4byts.
所以class Y 和 Z 必领填补3 bytes.最终得到的结果就是 8 bytes
津注：aligmm就是特数值调整列某数的整数倍，在32位计算机上，通常
m为4bytes（32位），以便 bus的“运输量”达到最高效率

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p119_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 120

深度报素 C++ 对象要型 (Andke 7he C++ Olyer Modr/)
Empty vital base clas 已经度为 C++ 00 设计的个特有术语了 它提供
个virtsal imterace。没有定文任何数据。某些新近的编译器（译注）对此提供了
特殊处理 （读看 [SUN94al]) - 在这个策略之下,一个 empty vinul hase clas 被
提为 drived clas atject 最开头的一部分，也就是说它并授有花费任何的额外空
同，这就节省了上述第 2 点的 1 byges （请注：因为既然有了 memntbs.就不需要
原本为了 empty daes 而安额的一个 chr)，也就不再需要第 3点所说的 3 bytes 的
填补，只新下第1点所说的眼外负担.在此模型下,Y和Z的大小都是4而不是$

编译器之间的增在差异正说明了C++对象模型的演化，这个模型为一般情
况提供了解决之道，当特殊情况连系被挖图出来时，种种启发（会试错误）法于
是被引人，提供优化的处理，如果成功，启发法于是就握升为普意的策略，并路
建各种编译器而合并，它被视为标准（显然它并不被规范为标准），久而久之也
算成了语言的一部分，Vintualfnction tubie 就是一个好例子，另一个例子是第2
章计论过的 “named retar vale (NRV) 优化”,

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p120_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 121

第 3 8Data i董% (The Soma

那么。你期望 caos 4 的大小是什么呢？很明显，某种程度上必须规你所使
用的编译器而定，首先，请你考虑那种并未特别处理 enpgy vital hase clas 的编
泽器。如果我们高记和Z 都是“虚征激生”自clas.x，我们可能会因答 16,
毕党丫和Z 的大小都是 . 然而当我们对 ctas.4 滩以 rivef 还算特时，得到
的答案竞然是12.到底是怎么用事
记往, 一个 vitual bise cdas ubotject 只金在 derived claes 中存在一价实
体，不管它在 chas 糖承体系中出现了多少次！ clas.4 的大小由下列几点决定：
■被大家共享的境一个classx实体，大小为 1bygg：
■Base chass Y 的大小, 减去“国 vintsal base clas.X 而配置* 的大小. 均
果是 4 byes. Base class Z 的其法齐间。 加起来是 8 bytes.
■ class 4 自己的大小： 0 byte-
■class.4的 alignmest 数量（如果有的适）.前述三项总合，表示调整前
的大小是9 bytes.clas 4必须调整至4 bytes 边界，所以需要填补3
bytes.  果是 12 bytes.
现在妇果我们考虑那种“粉别对 ompty vintas bhare clas 做了处理”的编译都
呢？一如前述，clas. 实体的那1byte 将被拿障，于是联外的 3btes 填补蒙也
不必了。因此 clas.4 的大小特是 8 byaes.注意，如果我们在 virtual base clas
中放置一个（EL上）的 dsa memers.两种编译器（“有特殊处理”者和“投有
特殊处理”者）就会产生出完全相同的对象布局。
C* Sandard 并不该航成定如 “base class suetjets 的雅列次序”或*不网
存取展级的4aa mmbers 的排列次序”这种类碎细节，它也不规定vimul
finctioms 或 vintal base classes 的实或细节, C++ Standaed 只况： 那些细节由各家
厂商自定，我在本章以及全书中，都会区分“C++ Standnd”和“当能的 C++实
现标准网种讨论

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p121_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 122

深度拟素 C++ 对象感型 (/ude The C++ O%*r Mn)
在这章中, cas 的 da mcmbens 以及 clas hienrhy 是中心议题。 ↑
claes 的 daa mambers. 一般面言，可以l表现这个 cdhas 在程序执行时的某种机
态。 Nonstaric dma memhes 放置的是“个别的 claesotjet” 感兴睡的数据，ssic
mhoms 则放置的是“整个clas”感兴睡的数图。
C++对象覆型尽量以空间优化和存取速度优化的考虑集表现nomsatic dita
membrs.并立保持和C适言suct 数据配置的兼容性，它把数据直接存放在等
个 class otject 之中-对于继脉而来的 nonstatic data membems(不管是 virtaal 或
winal bae clas）也是如此。不过界设有强制定文其网的排判期序。至于 stai
data members, 则被放置在程序的一个 global data segmemt 中。 不会影响个别的
clas etbjet 的大小. 在程序之中，不管该 claes 被产生出多少个 ctjects （经由直
提产生或间强激生)， staik data mombers 水透只存在一份实体（译注：孤至即使
该 class 投有任何 objet 实体。 其 static data members 也已存在} 、 但是 ↑
mbers 的行为精有不间。7.1 节有详细的对论
等一个 clas objct 因此必须有足够的大小以L容纳它所有的 monstatic dati
membars.有时候其雀可能令作吃惊（正如那位肽国来信者），因为它可能比你思
象的还大。原因是：
1.由编译器白动加上的额外 datamembers.用以支持某些语言特性（主要
是各种 virtaal 特性),
2.因为 aigament （界调整）的需要。
3.1 Data Member 的绑定(The Binding ofa Data Member)
考港下图这股程序代码：
// 案个 foo.b头文件。从某处含人
1fhoatxI

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p122_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 123

第 1 章 Dua 证盘学 ( The So
/ 糖序员的 Point.3d.h 文件
pubLie1

fioat 3, y- 1/
妇果我问你Poin3：x)传据得一个x是clams内部的那个x，还是外部
（cdm）的那个x？今天每个人都会目答我是内都胞一个，这个答案是正确的。
组并不是质过去以束一直都业称
在C最琴的验济券上，如果在:x0的两个函数实例中对x进行
参（取用）提行。这通作将合指向ghoblxotjoct1这样的事定皓果儿手音通地
不在大家的形日之，并选龙导乌尽期C++的阿种防御性程序设计风格。
1.把所有的 dstsmmher:放clas声明起头处，以确保正确的绑定
lass Peint3d
// 在ciass声明起头处先放量册有时 data nenbe
public:loat 8, 了; 1/
float X() eonst ( return xz )

2.把所有的 ialine fanctiaes,不管大小都放在 class 声明之外
class Poiat3d
相中设计风格移都到ciass之月
peiat34(11

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p123_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 124

聚度强素 C++ 对象提型 (Juidle TIle C++ Olyrcr Moi

/... etc.onst:
Joiatsriot
Xi)enst
returns

这些程序设计风格事实上到今天还存在，虽然它们的必要性已经自队C+
2.0 之后（件随者C+ Refenmcr Mawel 的修订）就消失了，这个古老的语言规
则被称为“mnbernringnle”，大意是“一个inline 函数实体，在整个 cas
产明末被完全看见之期，是不会被评估求值（evaluated）的”。C++ Standard 以
er scape resolution ruiles”来糖娠这个“nrweiting nle”，其胶果是，如果-
个 indine 通数在clan 声明之后立则被定文的话，那么就还是对其评估求值
（evalane)，生就是说，当一个人写下这样的码：
extern ist x/
class Point3d

ate:

// 事实上。分析在达里进行
时，对 member finties 本身的分析，金直到整个chass 的廖明都出现了才开始。
因此, 在个 inline mnber fiactie 影体之内的一个 data mooxr定操作。会

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p124_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 125

第 1 章 Das B8学 (Tte 5emetis of Dea)
在整个clas 声明完成之后才发生
然面,这对于 menber finctios 的 argumem ia 并不为真。 Argummt lit 中
的名称还是会在它们第一次道通时被适当地决试（rnsolved）完成，因此在cm
和ncted tpe names之间的非直党挥定操作还是会发生，例如在下面的程序片段
中, lngh 的类型在两个 member finction sigmnes 中都决议 (resohe) 为 global
oypederf,也就是m. 当后续再有 lngh 的 neMed bypedef 声明出现时， C++
Stindind 就把稍早的绑定标示为非统：
typedet int length:
class Point3d
pub11c1
void mumbie( length val 3 1 _sal = val.: 1ngth musbie() ( returs _val; )
/..
peiv
一个参考操疗2指被看见
1ength _va11typedef float length;
/...
上述这种语官状况，仍然需要黑种防御性程序风格：请始终肥”nedope产
明”放在chaes 的始处，在上述例子中，妇果把 lngth 的 need npeder 定义
于“在clas中被参考”之前，就可以确保非直觉佛定的正确性。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p125_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 126

深度探R C+* 对8模型 (Jenide 7 C++ Oywer Modr)

3.2 Data Member 的布(Data Member Layout)
己如下董组 data menlt
eless Neist3d (
loat
1oat yi
fioat t/itatic const int chunksise = 250u

Nonstaic data menbers 在 clas otject 中的排列展序特和其被声明的联序
样, 任何中间分 人的 statie data menbers 如 FrelLir 和 chunkSior 都不金被放进
对象布局之中，在上述例子里。每一个Poowr3d 对象是由三个 Boat 组成，次子
是 x 为 2. aic da members 存技在程序的 dala ngmet 中, 和个别的 cdass
objeca无天，
C++ Stndard 要求,在同个 acems sectin (也就是 privae-pubic-prond
等区段)中，members 的排列只需符合“较晚出成的 menbes 在 cas objet 中
有较高的地址”这一条持即可（请看C+Sndad 9.2节），也就是说，各个
mbern 并不一定得连续排列，付么东西可能会分于被声明的members之间
呢7 members 的边界调整（alignment)可能就带要填补一费 bytes-对于 C 和 C-
面言，这的确是真的，对当前的C+编泽器实现博况而言，这也是真的
编译器还可期会合成一些内部使用的dsamcmhes，以支持整个对象模型
ver就基这样的东西，当前所有的编译各都把它安错在每一个“内含vinal
Sncien 之 chat的otject 内，vper 会被放在什公位量呢？传统上它被放在所
有明确声明的 menbes 的最后，不过如今也有一些编译器把vptr 放在个 cas

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p126_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 127

3 # Dats I8B9 (The 5entises ef Dala)
object 的最前境。C++ Suandard重持先我所说的“对于布局所特的放任态度”
允许编译器把那些内部产生出来的members自由放在任何位置上，著至放在部
被程序员声明出来的 membxns 之间.
C++ Standed 允许编评器得多个 acoee sections 之中的 daa members 自
由募列，不必在手它们应现在clas声明中的次序，也就是说。下面这样的声明
pias mt.3a 1
Peivate:float x/
static List<Peintid*> *freeLiati
private1
atie c
ptisgteat si
\1
其 chas ctjct 的大小和组成都和我们先前声明的那个相网，但是mcmborns 的排
列次序则械编译器面定，编语器可以随意把y或：减什么东西放为第一个，不
过就费所知，当前投有任何编泽器会达么做。
当前各家编译器都是肥一个以L上的 accessctioms连镇在一起，依照声明的
次序，成为一个连续区映，Acoesc5es 的多塞井不会指来额外类图，例如在
得到的otject 大小是一样的。
下面达个 templae fanction.接受两个 dats mcnbers，然后判断谁先出现在
clas otjet 之中,如是同个 mcnbers 都是不间的 aces secdioes 中的第个被声
者，E.函数就可以用末判新惠一个 sctin无出现（如果你对clas menber 的
指针弄不热悉，请参考3.6节) ;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p127_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 128

聚度指素 C++ 对象模型 (Juide Thle C++ Ohect)
tesplate clas cas,tyet,
class data_type2 )
har*ess_ocder (
data_type2 class_type::*
return:(8es1 1= men2 1/
meml < mn2*menber 1 occurs Eirst*
:*mesber 2 occurs first"

上述函数可以这样被调用

于是 ciam_spe 会被期定为 Pow.面 do_ope/ 和 duno_ope2 会被绑定
为 fo
3.3DataMember的存取
已知下面这股程序代码：
Poist3d otiginu
你可能会间x的存取成本是付么？答案税：和Poir3d 如何声明面定，x可
能是个 static mcmber，这可能是个 nonitaic membr，Psint3 可能是个睡立（非述
生）的clag，可能是从另一个单一的bae clas 医生而束：虽然可能性不高，但
它甚至可能是成多重继承或虚和增承而来，下面数节将依次检验每一种可能性，
在开始之脂，让我先围出一个间题，如果我们有两个定文。orgin和pl
Psist)6 etigin, *gt - serigin/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p128_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 129

第 ) 章 Dua 通盘9 (The Semartios ef Data)
我用它们来存取 daea members，像这样：

通过orgin存取和通过p存取，有什么重大差异吗1如果你的国答是
yes.请你从 claesPsint 和 dsmmerx 的角度案说明差异的发生国家，我会
在这一节结束前重返这个间题并提出我的答案。
Statie Data Members
Stutic ditamcmens,按其字面意义，被编评器提出于 clas 之外，一如我在 1.1
节所说，并被视为一个globl交量（但只在 ches 生命范围之内可见）。每一个
member 的存取许可 （译注： privale 或 pronecded 或 pubic) , 以及与 claen 的关
联，并不会导数任何空润上或执行时间上的额外负担——不论是在个别的ctass
cbjects 或是症 static dala mmber 本身,
每一个 sttic dm meber 只有一个实体，存放在程序的 dsasmm 之中。
每次程序参周（取用）sati mmber，就会被内部转化为对请唯一的otem 实体
的直接参考操作。例如：
Point3d:ichunksize =250://评往：我票作者的意思可能是要说
// Petnt3d::chunkSise = 256.

从指令执行的观点来看，这是C++语言中“通过一个指针和通过一个对象
来存取member，继论完全相同”的唯一一种增况，这是因为“经由membo
seletie opertans （绿注：也就是运算荐）对一个 sie da mber 进行存取
操作”只是语然上的一种便宜行事面已，memher 其实井不在can otject 之中。
因此存取 matic menbers 并不需要通过 chas etjod.

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p129_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 130

探度探素 C++ 对象模型 (/lde 7he C++ Ohecrt Mdn)
但如果chanSior 是一个从复杂腹承关系中继承面来的mcmbr，又当如
何？ 或许它是一个“vintal base chas 的 vital hase clas”（或其它间等复杂的继
承组构）的member 性说不定，晚，那无关紧要，程序之中对于 stasicmembens 还
是只有唯一个实体。面其存取路经仍然是那么直接。
如果sti dimmmeer 的存取是经由函数调用（或其它某查语法）面被存职
呢！举个务子，如果我们写
roobar(1.chenksise *-250:// 评性：我患作者的意思可量是要识//  foebar () -chunksise = 250:
调用 foehr 会发生价么事情？ 者 C+ 的雅标准（pre-Standed） 规略中
投有人知道会发生付么事，园为ARM4并末指定 focbar是否必须被求银
（enad）.ceet 的做法是简单地把它丢排，但 C++Stded 明确要求
ochar)必须被求值（evalnd），虽然其继果并无用处，下面是一种可能的转化：
// Ivibar () -chunksite == 250;//详性：我思作者的意思是费说// fosbar [l -chenksire = 250
//eslsgte espressios, discarting resalt

若取一个atedaxher 的地址，会得到一个指向其数据类型的指针，面
不是一个拉习其 cdas member 的指针，因为 maik member 并不内含在一个 clsn
objet 之中。WU
sFoL:t3d: :cbuskdiae/
会获得类型如下的内存地址：
eeest ist*

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p130_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 131

第 3 章 Dm 89 (The Senastio ef Duts)
如果有两个 clses， 每一个都声明了个 sasic memer peelie，那么当它们
都被放在程序的 dta sgmcmt时，或会导政名称冲突，编译器的解决方肽是墙中
对每一个 mtatic da membor 编码(这种手续有个硬类的名称: same-manging)
以获得一个睡一无二的程序识别代码，有多少个编详器，就有多少种samm
manging 谢法！ 通常不外手是表略啦、语出措眸啦等等。任何 mame-mangling 做
法都有两个要点：
1.一种算法，推导出脏一无二的名称。
2.万一编语系统（或环境工具）必须和使用者交谈，那些殖一无二的名称
可以轻易被推导回到原来的名称。
Nonstatie Data Members
Nersmic dat mmbers 直接存放在每一个 cas otject 之中.除非经出明确的
(cxplicik） 或暗喻的 （impick) cas otject.改有办出直接存取它们]。只要程序员
在一个 mmber fcsien 中直接处理一个 neesatic da member，质谓“inpice
las cbjet 就会发生，例如下图这段码：
x += pt-xrf += pt-yi
+= pt-1I
表面上所看到的对于x,y：的直接存取，事实上是经由一个“impick clas
cbjcr*（由 shir 指针表达）完成，事实上这个函数的参数是
/ mgmper fusetioa 的内排辑化

this->z += pt.1/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p131_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 132

尿度指董 C++ 对象鼠型 (Auiudre Zhe C++ Oyee .Moll
Member finctiems在本书第4意有比校详细的时论
欧对一个 nonskaic da moner 进行存取操作,编译器需要把 clas odtjeon 的
起始地址加上 dmta member 的编移量（ofet)，个例子。如果：
origin-_y = 0.0J
部么地址&origin_y 将等于：
serigin • (ioint3e11_y - 1)/
谨注意其中的 -1 操作，指向 data mcmber 的指针， 其 ofhet 但总是被加上
1.这样可以使编译系优区分出”一个指向 daca member 的指针,用以指出 clas 的
第一个 membe”和“一个指向 ala member 的指针，没有指出任例member”两
种情况，“指向 dammembers的指针”将在3.6节有比较详细的讨论。
每一个nontadic datamenber 的编移量（ofe）在编译时能即可获知，基至
如果 momber 属于一个 base claes subobject （振生自单一或多重继承申链）也是
一样，因此，存取一个nonic damner，其教率和存取一个 Csct mebe
一个noedelerived claes 的 member 是样的.
现在让我们看看遗拟继承，虚拟继承将为“经台 base cass saboetjea 存取
bxrn”导人一层新的网接性。警如

其执行效事在z是一个 stet member、一个 claes member、单一继承.多重继
承的增况下都完全相同，但如果_x 是一个 vital base clas的member，存取速
虞会比较慢一点，下一节我会验证“继承对于mxmber布局的影响”，在我们尚
未进行到那重之前，请用忆本节一开始的一个间题，以两种方法存取坐标，像

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p132_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 133

第 ) 章 Dws 适.盘学 (Tw Senavios of Dma)

“从arigie 存取”和“从px 存取”有什么重大的差算？答案是“当Point3
是一个 derived class,而在其继承结转中有一个 vitual base class, 并且被存取的
menber (如本例的 x) 是个及读 vitsal base clas 继承而来的 member 时. 获
会有重大的差录”，这时候我们不能够说pr必然指向图一种claosbpe（因此我
们也就不短道编译时期这个mcnber 真正的cfbhet 位置），期以这个存取操作必
深尾迟至执行期。经病一个额外的网接导引，才肥够解决，但如果使用orgx
就不会有这些问题，其类型无疑是Peint clms，而即使它理承自vintaal ban
clas，membes 的ofhe 位置也在编评时期就固定了。一个积板进取的编译器据
至可以静态地经向orge放解决*对x的存取。
3.4“继承”与DataMember
在 C++指承2中，一个 dcrivdcas otjet 所表现出来的东西，是其自己
9 members I上其 hase cla/fes) mui)ens 的总和. 至干 dcrived class menben
和 haee dliases) mern 的判次序并来在 C++ Standand 中强制指定：建论上
编译器可以自由安持之，在大部分编评器上头，base chasmmbers 总是先出现。
包属于 virtal hase clas 的脉外 (般而言。 任间条现则一且硬上 vintaa base
klaes 就没属儿。这里亦不例外)。
了解了这种继承模型之后，你可能会问，如果我为2D（二维）或3D（三维
生标点提供两个抽象数显类型如下：

onstructer (s)
// acoess functions
private:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p133_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 134

聚度指浆 C++ 对象提型 (erile Tr C++ Oywer Modei
Point2dclass Poist3d (

fioat kc y。 3/
这和“提供两层或三层继承继构，每一层（代表一个维度）是一个claes，派生白
较能维层次”有什么不同？下面各小节的讨论将器盖“单一继承且不含vinui
Sions”、“单一继承并含vital funtions”、“多重继承”、“盘拟睡承”
等医种情况。图3.1a 就是Pain2d 和Pointl 的对象布局围。在投有vintua
m 的博况下（如本树)，它们和 C stuet 完全一样。
foat x,

pinf3d
pt3d;
图 3.1a个别struots 的数据布是
只要继承不要多态（Inheritance withoutPolymorphism）
想象一下，程序员或许希望。不论是2D或3D垒标点，都能够其享同一个
实体，但又能够继续使用“与类型性质相关（所谱 typ-poifc)“的实体，我们
有一个设计巢略，就是从 Pain2d 抓生出一个 Point3u.于是Poonr3ld 将继承 x
和y坐标的一切（包相数显实体和操作方法），骨案的影响则是可以共享“数据
本身”以及“数据的处理方出”，并佛其局部化，一般而吉，其体继承（concre
inhertanoe，译注：相对于虚损继承vitual inharianoc）并不会增加空间成存取时
间上的额外负担。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p134_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 135

ciass Peintzd (
Peiat2d( f1oat x = 0.0,
1 _xt × 1 _yt y 1 1 31
float x() ( retuen _x; )
ioat yl) 1 return _yr )

x +e rha.x(1 7088t Point264 rhs 1
_y +* zba.y(1)
// ... mMce sesbers
float

t2d  // irheritaLass Poist2d : publie Peint2d (
Peblset

est Point3dk rhs 1r**[ ths 3]
_t *= zhs.t()2

fioet _1

这样设计的好处就是可以把管理x和y坐标的程序代码局部化，此外这个
设计可以明显表现出两个抽象类之网的紧密关系，当这两个clases独立的时

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p135_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 136

候，Point2f dtjen 和 Peinr3etjet 的声明和使用都不会有所改变，所以这两个
独象类的使用者不需要细道cbjects是否为独空的classcs 类型，或是被此之间有
继承的关系，图3,1b显示Pais2 和 Por 继承关系的实物布局，其间并没
有声明vintul 接口.
foat_x

Moat_J
) pt3d;
图3.1b单一继承而且没有virtual function 时的数握布局
把两个原本瞳立不相干的 clsics凑成一对“ope/shpe”，并审有增承关
系，会有什公易犯的错误呢！经验不足的人可能会重复设计一些相阿操作的函
数。以我们例子中的 contrctor 和operor*=为例，它们并设有被缴成 inise
函数（生可肥是编评器为了某杰理由没有支持ininememer Snciom），Point3d
cbject 的析始化操作或加肽推作，将需要都分的 Peint2d otject 和择分的 Poietd
cbjiect 作为成本。一般而言，选择某些函数缴成 intine 函数，是设计 cas时的
一个重要课题
第二个易肥的错误是，把一个chas分解为两屋成更多层，有可能会为了“表
现 chas 体系之换象化“而警醒所需空间.C+语言保证“出现在dcrived clas中
的base clasbebjocei 有其完整原样性”，正是重点所在，这似手有点难以理解！
最好的解释方放就是相底了解一个例程，让我们从一个具体的clais开始：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p136_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 137

:1895

c2:

分加下

2. cJc - c2 和 c3 各占 月 1 bytes

或在板设，经过某些分析之后，我们决定了一个更逻调的表达方式，把
分裂为三层结构
PabLieoncretel (
11.-
private:Iat valr
char bit1/

C1888

chsr b4t2:

public:ciess Cc
7/ ..*
ptiate bit3:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p137_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 138

深国R C++ 3象根型 (Juile Tle C+ + Oyerer Molel)
队设计的观点来看，这个结构可能比校合理，但员效率的观点来看，我们可
能会受图于个事实：现在Concnrte3 object 的大小是16bytes,比原先的设计
了一单
怎么民事，还记得“base daes suhoetjea 在 4erived das 中的原神性” 吗让
我们仔组观察这一理承结构的内存布局，看看到尾发生了什么事
Comerete/ 内含网个 members: val 和 Ntl, 加起来是 5 bytes. 面 ↑
Comcree/ otjet 实际用 sbtes.包握填补用的 3bytes,以使otject 能够养合
一部机器的word 边界，不论是C或C+都是这棒，一般面言，边界调警
（alignmms）是由处理器（pesor）来决定的。
到目前为止没付么需要图总的。红这种典型的布局会导致轻率的程序员犯下
错误，Comcrmr2 加了唯一—个 nontaric dn mcmber bu2， 数据类型为 chr. 轻
率的程序员以为它会和ComcNar/握房在一起，占用原本用来填补空间的1
bytes;于是 Coecram2 otjet 的大小为 8 byes,，其中 2 bytes 用于填补空间
然面 Comecrese2 的 bar2 实际上部是被放在填补空间所用的 3 bytes 之后，于
是其大小变成12 hyte.不是8bytes.其中有6hys 激费在填补空间上。相同
的道理使得 Concwtjec的大小是16byes，其中9hes 用于填补空间
“真是养意”，我们即位纯真小避猫这么说，许多让者以电子部件、电话或
是用嘴巴他对我这么说，你可了解为什么这个语言有这样的行为？

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p138_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 139

第) 盘 Dea i89 (Th ShnasWoeser Dala

让我们声明以下一级指针：
Coserete2*pec2
其中 pcr_/和pc/_2两者都可以指向前述三种chasescbjicts.下面这个指定图
*pe1_2 = *pc1_1/
应该执行一个款认的“menberwite”复制操作（复制一个个的menbos），
对象是被指的ctect 的 Comcne/那一那分，如果pcl_/实际指向一个
Comcre2otect 或 Concwe3 ctject，则上述操作应该将复制内容指定给其
mte/ subebject
然页，如果 C++ 语言把 derived cas members （也就是 Coec

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p139_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 140

探度探索 C++ 3象吸型 (Juidle Tle C+* Ohirct Moder)
Cone3:bi3） 和 Conce/ sbotjex 募佛在一起，去除填补空间，上述那些证
意就无法保留了，那么下面的指定操作：
pc1_1 • pC2 // 净注: 9 pc1,1 度 Conerete2 对象
*pe1_2 = *ge1_11
就会将“被蛋佛在一起、继承面得的”membes内容覆盖排，程序员必须花费板
大的心力才能找出这个“奥虫”！

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p140_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 141

)章 Das B8学 (The Semaetis of Desa)
加上多态(Adding Polymorphism)
例，那么我需要在健承关系中提供一个 vintal ftnction 接口。让我们看看如果运
么量，情况会有什会改变
// 语性：以下的 roim26 声明请与101 页的声明作比较lass Point2d {
pubLie1Point2d( float × = 0.0, float y ● 0.0 )
1 _xt x ). ,yi y ) ( 1r
香取的数与前
是合理的rirtoal float s() ( retuzn 0.0; ) // i#性. 2d A6 ± 为 0.
viateal void at float 1 ( 1
//投定以了的运置的为vLrts)
y +* tha.y13
// ... ncee Mab4t3
protected:
flo8t _x, _31
只有当我们全图以多态的方式（pohmorpbically）处理 2d 或 3d 坐标点时
在设计之中导人一个vinual提口才显得合理，也就是设，写下这样的码
veid foe( Poiat2e 4pl, Peint2d sp2 3 (

其中p/和p2可能是24也可能是3d坐标点。这并不是先前征何设计所能支

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p141_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 142

深度指京 C++ 对象提型 (buile Thr C++ Oyeer Morv/
持的。这样的弹性，当然正是面向对象程序设计的中心，支持这样的弹性，势
给我们的 Pein2f elas 审是空间和存取时间的额外负担
■导人一个程Point2d 有关的virtual table,用来存放它账声明的每
virtaal funetions 的地址。这个 table 的元素数目般面言是被声明的
virtal fanctioes 的数目，再加上一个或两个 sles（用以支持 rusti
type ideetification)
■在每一个class ohect 中导人个vptr，提供执行期的链接，使每
object 董够找到相症的 virtual table,
■加债 cosstructor，使它能够为vpr 设定初值，让它指向 class 所对应
的 virtal table. 这可能意味者在 derived class 和每个 basc class 的)
trsctar 中，重新设定por 的值，其博况模编谦器的优化的积报生
图定，第5章对此有比较详细的讨论。
■加强 dentructor，使它能够抹消“指肉 clas 之相关vital tablc°的
vptr. 要知道， vptr 报可能已经在 derived clas detnscter 中被设定 为
derived cls 的 virtual tahte 地址，记住， destrwctor 的调用次序是反内
的：从 derived clas 到 base clas.一个积板的优化编译器可以压抑者
些大量的指定操作。
这些额外负担带案的冲击程度栈“被处理的Point2f ctjocts 的数目和生命
期”面定，也机“对这些ctjocts做多态程序设计所得的利益”面定，如果一个
应用程序细建它所能使用的 poietjos只限于二维坐标点或三维坐标点，那公
这种设计资事来的额外负担可能变得令人无法独受1，
以下是新的 Point3d 产明

publie1
1我不知道是否有那个产品系统真正使用了一个多态的Powr类别体系，

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p142_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 143

第 ) 章 Dama I88 (Te Semunties ef De

Poist2e( x, y 1。 _s( × 1 ( 1/
tioat sc)( return _4) )

_s += rhs.1()rerator+=( zhs 12
// -., note sesbets

译注：上述新的（与 p.101 比较) Psimt24 和 Poim3f 声明，最大一个好处
是，你可以把 opeater+=用在个 Poinr3d 对象和个 Paint2n 对象身上：
Foint24 g2d1.1, 2.t)1
Peist34 p3%(3.1, 3.2, 2.3)2
p3d ** dr
得到的 3 生病是 (5.2, 5.4, 3.3)
显然clas 的声染独没有改变，但每件事情都不一特了，两个a）memh
Sms [及 peme*~0 运翼都度了盘报通数： 每一个 Pon3 clas cbjcct
内含一个额外的 vprmmber (增承自 Pon2) : 多了个 PoasMvintsal tolie
此外，每一个 vintl membe finctioe 的调用也比以前复余了（第4 章对此有详组
规明）：
目前在C++编评器那个领域是有一个主要的讨论题目，把vpr放置在
clasctjet 的感里会最好？在chont 编泽器中，它被放在 clas ctjec 的尾端，
用以支持下图的继承类型，加图3.2a所示：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p143_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 144

探度探素 C++ 对象模型 (uridle The C** Obecr Mod

class has_virts : public no_virts fpubLic:
//rteal void foo(12
at43

int d1
intd2
structno_virts nv

图 3.2aVptr 被放在class 的尾编
把 vptr 放在 claes cbject 的尾编，可以保管 hae chas C sarut 的对象布局。
因面允许在C程序代码中也能使用，这种数压在C++量初网重时。被许多人采

到了C++2.0.开始支持虚扣增承以及抽象基类，并且由于面向对象典范（00
paadigm）的兴起，某些编泽器开始把 vpo 放列 clas ctject 的患头处（例如
Martia OKioedan,他领导 Micresol 的第个 C++ 编译器产品。就十分主张这
种徽法)，请看图3.2》的困解设明。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p144_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 145

第 3 章 Data 退B9 (The Sm
intd
strudt no virts nv;

图 3.2bVptr 被放在elass 的素腺
把vptr 放在 cdams otjex 的前端，对于“在多重继承之下，通过指向 cas
mcmbers 的查针调用 vital fanction” . 会带束一查帮动（谱参考 4.4 节） 、 否
则，不仅“从 dlas ctbject 起始点开始量起” 的efse 必须在执行期备妥，甚至
与csvpe 之间的cthst 也必须备蛋，当然，vpr 放在前碳，代价就是表失了 C
言兼容性。这种良失有多少意文？有多少程序会从一个Cart湿生出一个具
多态性我的cams呢！当前我手上并投有什么扰计数据可以告诉我这一点
图3.3 显示 Po2v 和 Psint3d 加上了 virtsal finctiee 之后的量承布局-注

Moat_y

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p145_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 146

深度探素 C++ 对象横型 (huude The C++ Oyect Moude)

多重继承（MultipleInheritance）
单一继承提供了一种“自然多态（natnl polymopbism)“形式，是关于 clase
体系中的 bisc ype 和 derived typc 之间的转换。 请看星 3,1b. 围 3.2a 或图
3.3. 你会看到 base clas 和 derived cdaes 的 otjects 都是从相同的地址开始， 其
间差异只在于 4erived ebject 比较大。 用以 多容纳它自 ℃ 的 noastatik da)
m.下重这样的指定操作
Point3d p3d/esnt2d *p = 4p3d;
把一个 derived clas otject 指定始 bae class（不管继承深度有多保) 的指针波
refeme，该操作并不需要编评器去调师或参改地址，它很自然地可以发生，而
且提供了最佳执行效率。
图 3.2b 把 vpr 放在 claes etjer 的起始姓， 始果 bae claos 没有 vitul
finetion 面 derived cdaes 有 （译注： 正如图 3.2b) 。 那么单一继承的自然多态
(nateral poljmorphim） 其会被红破，在这种情况下, 肥一个 erived otjet 转损
为其basn类型，就需要编泽器的介人，用以调整地址（因vpr人之故）.在
既是多重继承又是虚拟是承的情况下，编译器的介人更有必要。
多重理承既不像单一糖承，住不容易模塑出其模型，多重继承的复杂度在于
derived elass 和其上一个 be class 乃至于上上个 baee clas之间的 *鲁自
然”关系，例期，考惠下面这个多重是承所获得的 chaes Fontcx3M
译注：原书的p92-p94 有很多前后不一致的地方，以及很多“本身服没有错
误却可能误导读者思想”的叙述，程序代码和图片说明也不相符，简直一团现！
我已将其全部更正，加果您拿着原文书对照此中语本看，清不要乍兄之下对我产
生调会
plass Poletiad (
112

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p146_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 147

B) 章 Dams i盘学 (The Sesaeties of Dwa)
//《弹注：拥有virtoal 接口，原以 9oint2d对象之中会有 vptr)
f1sat _3- _y

perotflo#t _u
cies vertex (
Vertex *sext;
F
class Veztex3d : // 净性: 原书误批 vert

多重建承的问储主要发生于之间的转换：  cbjects 和其第二或后继的 bas

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p147_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 148

聚度探素 C+* 对象提型 (Jxride The C++ Obecr Modr)
sxtern void munbie( const Vertexs ))

或是经由其所支持的vitualfamcion 机删做转换，因支持“vintal funcion 之调
用操作”更引发的问是将在4.2节过论
对一个多重派生对象，将其地址指定始“最左端（也就是第一个）bar cass的
指针”。情况将和单一继承时相网，因为二者都指向相同的起始地址，需付出的
成本只有地址的指定操作图巴（图3.4显示出多重继承的布局），至于第二个或
后继的 basc claen 的地址指定操作，则需要将地址修改过：加上（或减去，如果
an 的话)分于中间的 hase clasbetjed(t) 大小，衡如，
Tertex3s v3
Fertea p2dr
那么下面这个指定操作：
pv = sv3&/
需要这样的内部转化：
pv = (vert// #S c++es*) ( (tehar*) 6v3d) + sseef( Peswt3d 21i
面下面的指定操作：
p2d · v38)
都只需要简单地携贝其地址就行了，如果有两个指针如下
Vertex34 *gv3d;//评世：原书命名为*p]d.不晋合命名规则.改为*pv)0
Vertex*pr;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p148_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 149

第3 章 Dm 8牛 (The

那么下需的指定操作：
pr = pv3d;
不整够只是觉羊地被特换为：
/ 盘报 c++ 得
因为如果 pr3d 为 0. p 将获得 nongf( Peint3s) 的值，这是错误的： 所以L,
对于指针，内那转换播查需要有一个条件测试，
// 盘权 c++ 荷
9
至于ndo，则不需要针对可能的0值做防卫，因为reereece不可能参
考到“无物”（ne otject）

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p149_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 150

探度指浆 C++ 对 象模型 (ferile 7he C++ Obyeer Mx

译注原书的图 3.4 只避出 Iere.x2d.双有黑岛 Feriex34.虽然其中的 Frtier
时象布局图“可能”是正确的（我们井没有在书中看到其声明码），但我相禁达
其实是Lippman的笔误，因为它与书中的许多讨论没有关系，所以我把真正与书
中时论有关的Fer 的对象年周据于泽本的星3.4,如上
C++ Standard 井苏登R Fertex3e 中B base classes Peietd 8 Fertex 有8)
定的得列次序，原始的ctert 输译器是根据产明次序来排列它们.因此choet 编
评器制作出来的 Fertaz,Jd 对象，将可被视为是一个 PeinrM suboetjet （其中又有
个 Peins2/ sabotject) 加上个 Fersex sbotjes, 最后再加上: Feriex3d 自已的
部分。目前各编译器仍然是以此方式究成多重bae class 的右局（但如果加上
虚拟继承，就不一样了）。
某些编译器（例如 MetaWarc）设计有一种优化技术，只要第二个（或后整）
basc clas 声%了个 vintual fenction, 或第个 base class 没有, 就把多个 basc
clases 的次序进行调换。 这样写以在 derived das tjet 中少严生一个 vpr， 这
定优化技术并未得到全球各厂商的认可，因此并不鲁及。
如果要存E;第二个（成运继） base dlas 9一个 dam member，得会是怎样
的情况！满要洲出后外的成本吗！不，mcmbers 的位置在编承时就面定了，因式
存取mmben 只是一个关单的fset运算，就像单一是承一样菌单—不管是
经由个指、一个 refeenee 或是一个 otjea 来存取
虚拟继承（Virtual Inheritance）
多重继承的一个语意上的副作用就是，它必须支持某种形式的“shared
sbotjcet 录” 。 一个典型的所子是最早的 iseran ibry

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p150_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 151

第 3 章 Dats 8学 (The 5emtics ef Dala.

class:class

不论是ioa 度awan 都内青个 ir wbolt. 热须在ioram的对
象布局中，我们只需要单一一份iorubobject 就好，语言层面的解决办法是导人
所请的虚拟继承：
lass 1cs ( .-. 1:Lstrean
class sootreamlass  18
一如其语意所呈规的复杂度，要在编评器中支持虚拟磨承，实在是国唯度额
高，在上述ismam例子中，实现技术的携战在于，要找到一个是够有效的方法，
将 hseam 期 cata 各自维护的一个 ior ubotjct. 折叠成为个由 iaren
维护的单 lor sotjet, 并儿还可以保存 basc clas 和 derived cas 的指针 (以
tcm）之间的多态指定摄作（pemoghism asigmess)
般的实现方法如下所述。Clas 如果内含一个或多个 vinul base cas
t5，像unam那样，将被分期为两部分：一个不变局部和一个其享局部。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p151_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 152

聚度素 C+* 对B模型 (Junidle Tr C+· Obywer Modir/)
不变局部中的数据，不管后继如何街化，总是拥有固定的ehe（从ctbjcct的开
头算起），所以这一那分数据可以被直换存取，至于共享局部，所表现的就是vinal
bas clas uabobjict.这一部分的数据，其位置会因为每次的医生操作图有变化
所以它们只可以被间接存取，各家编译器实现技术之间的差弹就在于间接存取的
方法不同，以下说明三种主流策略，下度是Venxf 虚拟继承的层次结构2
pubiic:class Point2d (
flset _3 _31
P 2

peblsc:

fertex *next

public:
ted:
fioat _1a
cless Vertex3d
之
pie1i41
Ffloat muable/

118

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p152_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 153

第) 8 Dex 8牛 (Thte Semaa ef Data

校的者局策略是先安排好dclas的不变都分。然后再建立其共享部

然图，这中间存在着一个网题，如何能够存取clas的共享部分呢？choet 编
译器会在每个 deried caes oject 中安错一些指针，每个指针指向一个 vintaa
base cls，要存取继承得来的 vitual hase clas menens，可以使用相天指针间度
完成。举个例子，如果我们有以下的Pain3d运其符：

*= rhs-_x)Y +=ths-_7/
_t *= ths-_t/
在com重略之下，这个运算符会被内部转换为：
//盘C+异
eiat2d->_y1
_ *= ts-_u
页一个 derived clas 程一个 basecdas 的实例之间的转换，像这样：
Point2d *p2d = pv3d // 课性： 显书为Tertex *pr = pv36; 易为电要

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p153_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 154

深度指素 C++ 对象模型 (/uide The C++ Otyect Model)
在cfom 实现模型之下，会变成：
/ 盘指 c++ 6
/评注：原书为24 *p24 = pr3e 7 pv3d->__vbcPoint2d : 0tex *pv * pv3d 7 pv3st2d : C
为笔误（感谢黄使达先生与划东岳先生来信德导）
这样的实成模型有两个主要的肤点：
1.每一个对象必须针对其每一个virnusl baae class 育负一个额外的指针
然面理想上我们却希望clais ebject 有固定的负担，不因为其virtual
base clases 的数目西有所变化，想想看这该如何解决？
2.由于虚损继承申髓的加长，导致间接存取层次的增加，这里的意思是
如果我有三层虚招哲化，我就需要三次间楼存取（经由三个vitulbasc
slass 指针），然面理想上我们即卷望有图定的存取时间，不因为虚拟得
化的握度面改变
MeaWare和其它编译器到今天仍然使用coet的原始实现模型末解决第二
个间题，它们经合携贝操作取得所有的 ncsted vinul base claes 指针，放到 derirod
chas otjedt 之中。这就解次了“医定存取时同”的问题。虽然付出了一些空间上
的代价。MetsWise提供一个编译时期的选项，允许程序员选择是否要产生双重
指针，围3.5a说明达种“以指针指向 bast chan°的实现模型。
至于第一个同题，一般面言有阿个解决方法。Mikcoa编译器引人所谨的
vinual basc clas tsble. 每个 claes otjet 如果有个或多个 vintsal basc clhses,
就会由编译器安循个指，指风 vintasl baue clas table，至于真正的 virtal basg
cas指针，鱼然是被放在该表格中，虽然此法已行之有年，但我井不知道是否有
其它任何编评器使用此法，说不定Micmmse8对此然提出专利，以至别人不能使
用尼
第二个解决方酷，网时也是Bjumme比校要效的方法（至少当我还和他共事于
Feundatiee 项目时),是在 vital fnctien nabie 中放置 vitual baue clas 的 oftst
（面不是地址） 。 田 3.5b 显示这种 base cdas chet 实现模型，我在Foundaiot

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p154_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 155

第 ) 8 Dota 通8盘9 (The Sesantis of Dama)

图 3.5a
符，所以我全部改为Vertex）x写为Vertex2d.与书中程序代码不
项目中实现出这种方盐， 特 vinual hase cdas ehe 和 vintul ftncion etres 显杂
在一起，在新近的 Sun 编译器中， vinal finstien ble 可经由正值或负促来案
引.如果是正值，很星然就是索引则 visal fanstiees：如果是负值，则是索引到
vital basc das oes. 在这样的繁略之下， Pwint3d 的 openar* 运其符必须
被转换为以下形式（为了可读性，我设有微类型转换，网时我股有先执行对效
率有智助的地址预先计算操作）：
// 盘报c+硝

_# += ths._a/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p155_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 156

尿度报浆 C++ 对象模型 (Anide 7he C++ Obyecr Mode)
最然在此策略之下，对于继承而来的mcmbars做存乘操作，成本会比较昂
费，不过该成本已经被分除至“对 member 的使用”上，属于局那性成本。Deried
clas 实体和 bas cdhen 实体之民的特换操作，例如：
reint2d *p26 = pv3d: / 9It. 累6为 vertex *pv * pv3d; 鲁为笔
在上述实现模型下将变成：
/盘组 c+ 药1 ? pv38 + p3d->_vptr_Foiet3e[-1] : (s

上述每一种方摇都是一种实现模型，现不是一种标准，每一种模型都是用末
解决“存取med shet 内的数据（其位置会因每次强生操作面有变化）“
所引发的问题，由于对vital ban clas的文持带来原外的负担以及高度的复杂
性，每一种实现模型多少有点不间，面且我想还会随着时间面进化。
经由个非多态的 clas etjet 亲存取-个继承可来的 vintsal hae class 的
Foist3d origin/
origin._x:
可以被优化为一个直接存取操作，就好像一个经由对象调用的 vinl fnction两
用操作，可以在编译时期被决议（moed）完成一样，在这皮存取以及下一次夺
取之间，对象的类型不可以应变，所以“vital bhase das bobjets 的位置会交
化”的问题在这种情况下就不再存在了。
一般而言，vinasl basce claus 最有效的一种运用形式就是：一个抽象的 vitua
tase clas,没有任例 deta mcmbers

2

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p156_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 157

)章Dm e学 (T Snmis ef D)

图 3.5b
书中程序代码不符，照以我全部改为Vertex）Vertex2d.

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p157_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 158

3.5对象成员的效率（ObjeetMember Efficieney）
下面数个测试，旨在洲试聚合（agregatian）、封装（encapoulaien）、 L及
继承（inthortuance）所引发的额外负有的程度，所有测试都是以个别局部变量的加
续、减续，赋组（asin）等操作的存取成本为依据，下国就是个别的局部变量
f1oat pA_x = 1.725, PA_y = 0.875, pA_z = 0.478:
每个表达式需执行一千万次，如下所示（当然啦，一旦坐标点的表现方式有
变化，运算语续也就得随之变化）：
for ( int iters = 0/ iters < 10090c0o; sters** )

pa_s * pA_z + pB_r1
我们首光针对三个font元素所组成的周形数组进行测试：
ena fssy I x. y。 a 1/
Eor ( int iters = O: Lters < 10c000: Lters** )
pb[ × 1 • pA[ = 1 + peI y 1:
第二个期试是把网样的数组元素转换为一个Cscn数据抽象类型，其中的
成员管为Bm，成员名称是x了.2
for ( ist iters = 0: iters < 10000000; iters*+
pe.x ~ pAax + pa.t1
pB.t = pA.1 * pB-y1

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p158_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 159

第3 章 Dala 济意学 (The SemareBiscs of Data

更深一层的独象化，是做出数据封装，并使用inlise 函数，坐标点现在以一
个独立的Poirt3clas来表示，我尝试网种不同形式的存取函数，第一，我定义
一个 inline 函数，国一个 neference、允许它应现在 asigsmen 运算符的两确：
class Potst36 (
1 _x( xx 1。 _yt yT 1. _a( a 1 ( 1
floets x()( retses _8z )( returs _Fr )
floats s() ( returs _t> )
FPfiset _- _3- _U
那么真正对每一个坐标元素的存取握作应请是像这样
foe ( iat iters = 0; iters < 10000000: 1ters*e

p8.±() + p8.z(3 + pB.y()

我所定义的第二种存取函数形式是，提供一对gcthet通数：
即set

于是对每一个坐标值的存取操作应该像这样：
pB-xt pA-x(1 - p8-s() 1/
表格3.1列出两种编译器上述各种前试的结果，只有当两个编译都的效末有
明显差异时。我才会肥两者分别列出

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p159_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 160

表格3.1不断加强抽象化程度之后，数握的存取效率

局邮数组  2 .55
0.801.42
class 之中有 s1ise oet 鲁B
3.36

这里所显示的重点在于，如果把优化开关打开，“封装”就不会带来执行期
的效事成本，使用ininc存取函数亦然。
我很奇怪为什么在CC之下存取数组，儿手比 NCC慢两情，尤其是数组存
取所牵扯的只是C数组，并设有用到任何复杂的C++特性，一位程序代码产生
（codcgmiee）专家将这种反营现象解释为“一种奇行怪-与特定的编译
器有关”，或许是真的，但它发生在我正用案开发联件的编泽器身上耶：我决定
挖图其中秘者，呵我“爱携毛病的养治”吧，如果你喜放的话！如是你对这个题
目不感英题，请直接跳往下一个主题
在下面的 smby 语言输岛片段中，Ls 表示加敢（load）一个单精度浮点
数。5s.表示储存（bre）一个单精度得点数，nbs表示将两个单融度浮点数相
减，下面是两种编译器的ammbby语言输出结果，它们都加载同个值，将某一
个减去另一个，然后储存其结果，在效率校差的CC编译器中，每一个局部变量
的地址都被计算并放进一个缓存器之中（adbu表示无正负号的加法）：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p160_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 161

第3 章 De i适鲁单 (ThcSor
// cc asreseler tvtput
13  pB[ × 1 = pA[ x 1 - pB[ a 1
1-5adda
1.s
ssb.s sfe, sr4, st6

面在 NCC 编译器的bly 输出中，加载（load）步建直模计算地址
x 1 p
s.3 $re, 8(5ap)554, $f6
如是周部变量被存取多次，CC董略成许比较有效事，然图对于单一存取指
作，把变量地址放到一个缓存器中很明是地增加了表达式的成本，不论哪一种编
译器，只要把优化开关打开，两股码都会变得相网，在其中，环内的所有运算
都会以缓存器内的数值来执行，
让我下一个结论：如果没有把忧化开关行开，就很难则一个程序的效率表
现，四为程序代码播在性地受到专家所请的“一种奇行怪…-与特定编译器有
关”的度完影响，在你开始“程序代码层面的优化握作”以加速程序的运行之前。
你应该先确实地测试做率，面不是容着推论与需识判断。
在下一个测试中，我首先要介绍Pvir换象化的一个三层单一继承表达出，
然后再介绍Peinr 抽象化的一个虚拟继承表达偿，夜要洲试直接存取和 itinc存
取（多重继承并不适用于这个模型，所以我决定放弃它），三是单一继承表达出
F
// 维护 :
class Peint3d : pubiic Poist2d

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p161_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 162

深度探素 C++ 对象模型 (Juide The C++ Olirer Model)
“单层建拟糖承”是成Por/d 中量拟据生出Poir24;
则又从 Psint2 中虚相逐生出Pooxt3d.囊格 3.2 列出了两种编译器的测试组果
同样地，只有当两种编译器的致率有明显不间时，我才会把两者分别列出。
表格3.2在继承模型之下的数据存取
单一继示
使用 isLie 通数蓝接存取  0.80  1.42
0.80  3.58
使用inise i函数  1.60  1.94
1.60  3.3
虚织继录(取层)
直接存取CC
3.3  3.-4
使用 inline iB
NCC  3.23
单一继承应该不会影响测试的效率，因为mxmlnbees 被连续储存于derived
clesetjet 中，并且其ofhet 在编评时期就已知了，期试结果一如预期，和衰格
3.1中的抽象数剔类型结是相网，这一结果在多重增承的情况下应请也是相间
的，包我不能确定
其次，但得往意的是，如果把优化关用，以需识来判断，我们说效率应该相同
（对于“直楼存取”和“inlie 存取”离种做法），然而实际上邮是inlin存取比
按慢我们再次得到教训：程序员如果关心其程序效率，应请实际测试，不要光先
推论或意识判账或假设，另一个需要注意的是，优化操作并不一定总是能够有敬运
121

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p162_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 163

第 3 章 Des 还@单 (The SemO ef Data
行，我不只一次以优化方式来编译一个已通过编译的正常程序，部以失败收场
虚损理承的效事令人失望！两种编译着都没能够识别出对“增承而来的
mbrxld:x”的存取是通过一个非多态对象（因而不需要执行期的间接存
取）.两个编译器都会对xId:（及取是虚和增承中的x2t:y）产生间接存
重操作，虽然其在Poiat3d 对象中的位置早在编语时期就固定了。“间接性”压
抑了“把所有运算都移在暖存器执行”的优化他力，但是问操性并不会严重影响
非优化程序的执行效率
3.6指向Data Members 的指针（Pointer to Dat Members)
指向asmembers 的针，是一个有点神秘但颜有用处的语言特性，特别是
如果你需要讲脂调查cbss mmbers 的底层布局的话，这样的调查可用以决定
p是放在as的记始或必尾离，另一个用逾展我于3.2节，可用来决定
clas中的:级:oms占次厅，一如我增说过，那是一个神秘但有时般有用的
链言特性。
考患下面的 Peintf 声明。其中有个 vinl fincticn.一个 static dt
r，以及三个生标组：
class Point3d (oublie:
virtusl -Pointle(1///.
Goat x, 7. 1)

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p163_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 164

聚度M京 C++ 3 B 提型 (Irile Thr C+ Obecr Modr
间图不间的是ptr 的住置，C++ Sundard 允济vpr 被放在对象中的任何位置
在起始处，在尾确，波是在各个mmbs之间，然面实际上，所有编译器不是肥
pr放在对象的头部，就是放在对象的尾部。
那么。取某个望标成员的地址，代表件么意思？例如，以下推查所得到的
代表什么
s Feint3411#7 // 度: .书时 + 3d_poist::z: 成为笔减
上述操作将得到 ： 坐标在 clasetjet 中的编移量（ofset) 。 最低限度其便
将是x和 y 的大小总和，因为 C++ 语言要求阿个 acoeslevel 中的 mem
的排列次序应请和其声明次序相同。
然面vpr 的位置就授有服制，不过容我再说一次，实际上vpr不是放在时
象的头部，就是放在对象的尾部，在一那32位机器上，每一个oul是4 bhycs
所以我f应读期整附才获得的值要不是s.就是12在 32 但机器上一个vpt 是
by9es7 -
然面，这样的期里却还少1 bte.对于C和C++程序员面言，这多少前
是个有点年代的错误了
如果vper 放在对象的尾晚，则三个量标值在时象布周中的effict 分别是0
4.8.如果ve致在对象的起头，则三个全标值在对象布局中的axet 分别是 4
8. 12. 然面依若去取 dsmmembens 的地址,传用的值总是多 1, 也就是 1. 5. 9 或
5.9,13 等等。你账道为付么 Bjam 决定要这公爱码
谨注：如何取4Paimet:的值并行印出来？以下是示范作法

PrintE(*&Peim31:e = pn*, sPoist3d::2)//R vC5: C. BCB3:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p164_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 165

第3 章 Dua 语意学 (Te 5emfis ef Daa)

注意，不可以这么做
cout cc *afciat.3d:s + * cc sPesnt3d:ix c< eadL,
coutcc *6Poist3d::2* cc iPeintad:iz <c endli
否则会得到错误消息
emer C2679: binary c< : so 0
foat Peint3d:*' (or there

我使用的编译器是 Mikrse Visal C++ 5.0.为什么执行结果并不如书中界
说增加1呢？原因可能是VislC++数了特殊处理，其道理与本意一开始对于
ty vitual base clas 的时论相近！
问题在于，如何区分一个“设有指向任何daammber°的指针和一个指向“第
个datameeber”的脂针！考悉这样的例子：
fleat Peint36r:*p1 = 0;PaiM36::*p2 =
//承性,Foint3d::*时盘思是：*想向 point3d dstaer”的指针类型
//霍款：如向区分
if ( p1 =* p2 1 (
cout <c * they must
为了区分 p/ 和 p2， 每一个真正的 mmber oset 值都被加上1. 因此,不论编
评器成使用者都必领记住，在真正使用该值以指出一个membr之前，请先减择1.
认识“指向 dsamemben 的指针”之后，我们发成，要解释：
s beint34:1/ / 在: 显男的 s 34,peist1a 良为笔成

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p165_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 166

深度探素 C++ 对象模型 (buad 7he C++ Oter Modnl)
s oeigin-1
之间的差异，就非京明确了，鉴于“取一个mmai dmeber 的地址，将公
得到它在 claes 中的 oafhet” 。 取一个 “绑定于真正 chaes otbjout 身上的 dat
member”的地址，将会得到误 mcmbcr 在内存中的真正地址。把
s origin.1
所得继果基（译注：原文为加，错误）：的编移值（相财于origin 起始地址)
并加1（谭注：原文为盏，错误），就会得到argiw起始地址，上一行的返民望
类型应该是：
fioat*
面不是
float Point3d::*
由子上述操作所参考的是一个特定实例，所以取一个asic dsmamcnber 的地
址、意文出相网
在多重继承之下，若要将第二个（或后继）bas cdas 的指针和一个“与 derived
clas etjen 携定”之 membe 结合起来，那么将会国为“黄要加人efset 值”否
变得相当复杂，例如，板设我们有：

roid fusc1( iat Derived:*oep, Derived *pd
oase class Z n
pd->*dmp
void fsnc2( Derived *pd )
/ bap 将或% 1
st Base2::*bep * saase2::va12

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p166_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 167

里)  Ds i适B9 (The SemNs ef [Data)

fuse1( bap. pd 11
当 bp 被作为 μnci0 的第一个参数时,它的值就必蛋因介人的 Base/ cla
的大小面调整，否则/wcl0 中这样的操作：
pd->*dnp:
将存取到 Basr/:val7，而非程序员所以为的 Baz2nul2,要解决这个间题，必所
//经由编译器内部转换
fusc1( bap + sizeet: Base1 1.。 pd 1)
然页，一般而言，我们不著够保证hmp不是0.因武必须特别留意之：
/ ap/内部转换
funcl( bep 7 bep + sieot( sase1 ) : 0, pd 12

译注：我实际写了一个小程序。打印上述各个 mcnber 的 offet 值
printf(*sBasel::ral1 = tp (s*, sBasel::va11):// (1)
ptint(*4eried:a11 - p 1s, soerived:al1)/ (2)// (2)
print(*sberived:va12 = tp is*, sDerived: a12):// (4)
经过Vial C++ 5.0 编译后，执行继果竞然都是0.（1XC2)3)部是0是可以
理解的（失计公不是17可能是因为Vinal C++有特殊处理：稳早p.131 中我
的另一个泽注曾有说明）。但为什么（4）也是0，而不是47是否编译器已经内
帮处理过了呢？很可能（我只能如此随测），

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p167_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 168

聚度握素 C++ 3象根型 (Jule Tle C++ Oyrcr AMc

如果我肥Deried 的声明改为：
strset Derivee : ease1, sase2 c ist aid 1
那么：
pristf(*soezived::rald = tp (n*, sDerived::va1d) ;
将得到 8,表示 vul 的能面的确有 wa/7 和 ral2.
“指向Members的指针”的效率问题
下面的测试企图获得一些测试数据，让我们了解，在 3D全标点的各种（cass
的实限方式之下，使用“指向memhes 的指针”所带来的影响。一开始的阿个例
子持改有继承关系，第一个例子是要取得一个“已绑定的mcmber”的地址：
float *ax = spA.x
然后施以献值（asignme)，加法，减出操作如下：

*bs = *s1 + *by
第二个例子则是针对三个 menbes.取得“指向 4ata
北
flsat Peist34s:*ax + sboist3d::xJ
而赋值（asigsmmt)、加肽和减法等换作，都是使用“指向 data n
指针”语肽，把数值期定到对象p4和p中：
p6.,*bs + pA.*ax - p8, *bt:pB.*by * pA.*ay + pB,*bx
58.*h4 + ga.*a1 + g.*by1
回亿3.5节中的直接夺取操作，平均时间是0.8秒（当优化开启时）或1.42
秒（当优化天闭时），现在再执行这网个期试，继果列于表略3.3中

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p168_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 169

第) 章Dats i B学 (The 5eni ef Daea

表然 3.3存取 Nonstatic Data Member
直接存取 (进参考 3.5 节)0.80  I. 42
指针指向已康定的 Menber0.80  3.04
指针指向 Data MenberCC
9CC  0.00  5.34
来优化的结果正如预期，也就是议，为每一个“member 存取操作”加上
层间独性（经血已绑定的指针），会慢执行时间多出一售不止，以“指向mombcr
的指针”来存取数蛋，再一次儿平用障了取他时间，爱把“指间mcnbr 的指付”
解定到 clas objet 身上，需要膜外地肥oe 减1.更重要的是，当然，优化
可以使所有三种存取策略的效事交得一致，唯NC编译器除外，你不妹注意
下，在这里，NCC编评器所产生的码在优化情况下有看令人震惊的低效率，这反
映岛它所产生出案的msmby 韩有看可怜的优化播作，这和 C++程序代码如州
表现并无直接关系—要知道，我管检验过 CC 和 NCC 产生些来的末优化
ascnby 码，两者完全一样！
下一组则试要看看“组承”对于“指向datammbr的指针”所带来的效率
净击，在第一个例子中，独立的 Psiar cas 被重新设计为一个三层单一继承作
系,每个 cdams 有个 member
// tlaat 1
第二个例子分然是三层单一继承体系，但导人一层虚损继承：Pwin2/虚相注
生自 Powe., 结果,每次对于 Pciet:x 的存取, 将是对个 vinal bhasc chas dass
mmher 的存取，最后一个例子，实用性很低，儿手纯粹是好奇心的驱使：我加上
第二层虚拟继承，使Pot3虚扣流生自Pon2d.表格3.4显示测试结素，注意

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p169_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 170

聚度指案 C+* 对象模 (Juide The C++ Obyeer Model)
由于 NCC优化的效率在各项测试中都是一致的，我已经把它从表格中剔录了。
兼格3.4“指向Data Member 的指针”存取方式
投有继承  0.80  5.34
单一继承 (三星)  0.80  5.34
盘家继系 （单星)  1.60  5.44
虚指继承（双)  2.14  5.51
由于被继承的 dsm mmers 是直膜存放在 chas ctjact 之中，预以继承的引
人一点他不会影响这热码的效事，虚拟继承所带案的主要冲击是，它防碍了优化
的有效性。为什么见！在两个编译器中，每一是虚拟继承都导人一个额外层次的
周损性，在两个编译器中，每次存取P3.像这样：
pa.*bx
会被转换为：
spl->_sbcFeist + ( bx - 1 1
至不是特表最直续的：
sp9 • ( bt - 1 )
额外的间接性会障低“把所有的处理都移到接存器中执行”的优化能力

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p170_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 171

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 172

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 173

第4章

Function语意学
(The Semantics of Function)

如果我个Pt的指针象

当我这样锁：

时，会发生什么事呢！其中的 Poir3d  0 定X如下
pointad:

seema1 ._y - _y/nag

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p173_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 174

深度强素 C++ 对象根型 (Juile The C++ Oiner Mc
horma1-_1 * _t/sag
return soraa1;
面其中的Por3:mapindk0又定文如下：
float
return sgrt (_z * _a + _y * _y * _e * _a)
答案是：我不短道！C++支持三种类型的 member finctions: satic.nonstitic
和vintal.每一种类型被调用的方式都不相同，其间差异正是下一节的主题，不
过，我们显然不能够确定 mormslin) 和 maginaikr( 阿函数是否为 vintal 或
seevintal.包可以确定它一定不是matic.原因有二：（1)它直接存取 nomntatic 数
据：(2) 它被产明为 connt。 是的。static mmber fanctioas 不可能做到达两点
4.1Member的各种调用方式
图展历史, 原始的 “C wah Clases”只支持 mmsti membe fintioms (语
看[STROUP82]，其中有C 语言的第一个公开说明），Vintual 函数是在20 包纪
80年代中期被加进案的，并儿程显然受则许多质疑（许多质疑至今在C族群中仍
然存在)，衣 [STROUIP94] 文献中，Bjame 写速：
有一种常见的现点，认为 vintuailtuncdions只不过是一种皱脚的函数指针，没
什么用.其意思主要就是，witual tuncions是一种没有效能的形式，
Sttic member functioes 是最后被引人的一种函数类型， 它们在 1987 年的
Useix C++ 研讨会的商研习营（Implemenmtors Werkahep）中被正式振议加人
C中。并由ceet2.0实现出来

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p174_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 175

4 8 Fesoice 2.89 (The 5omsasSei er Feslkn
Nonstatie Member Functions（非静态成员函数）
C++ 的键计准期之算是; nomMatic mmher hscion 至少必须和取:的)
eemamber functicn 有相阿的效率。选就是说。如果我们要在以下网个通数之间
作选择
Eloat Point3d: :mag
那么选择member famcsien 不应该带来付么额外负担。这是因为编译基内部C
mcmber适数实体”H换为对等的“nommbcr函数实体”
幕个例子，下图是 moginde) 的个 somcner 定又：
this->_y - _this->_y *
_this->_a * _this->_t 11
乍见之下组手monmerber fanctien 比胶没有效事，它间接地经由参数取用生
标成员，面 member finctien 妇是直接取用坐标成员.然面实际上 member finctit
被内化为06sembar 的形式。下面就是转化步骤：
1.改写函数的 signuee（得注：意指函数原型）以安猫一个额外的步数到
menber fanctioe 中。用以提供个存腺管道，使 clas chject 得以调用
该通数。该额外参数被称为chis指针：
Poist3i:asgnitode( Poist34 *const this )

/ietst sorstetic mnber 2扩张过程

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p175_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 176

2. 将每一个*对snmaic dnta memer 的存取操作”改为经由 chis 指针 *
存車：

this->_z * thss*>_= 1:
mnhber fenctios 重新写或一个外部函数。对通数名称进
“mangling”处理，使它在程序中成为独一无二的语汇：
register Pointld *const this 1J_7Point3drr1
现在这个函数已经被转换好了，百其每一个调用操作也都必须转损，于是
ob) .aagnitsde () /
变成了：
agnitude_TPoist3dilvi sob] 1:

htede1lr
变成了
nagnitude_7Peist3dFv( ptr 1;

本章一开始所提及的mvmrie函数会被转化为下面的形式，其中最设C经
声明有个 Peint3d copy costructor, 面 samed ntumed vale (NRV) 的优化生
已播行

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p176_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 177

第 4 童 Fanctice i通8单 (The Sems
// 以下推述“named retuen valse 通数” 的内那特免7/ 使用 C++优科
alLss_7iroint1dFv1

// defaaitc
_reslt -_* = this->_x/nop

returh
一个比较有效率的做法是直独建构“nomal”值，像这样。
Point3ds16senalise (1 censtPeint3d

它会被转化为以下的码（我高次最设Pour34的cogycore已腔
明好了。面NRV的优化也已实施）：

seoalia_7Pist3rv9(
cegister float sug * this->nagnitude()
/ reault 用以单代逐R量（retsn valoe)

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p177_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 178

区度董 C+* 3 8提型 (/eile 7he C++ Otyer Aodr/)

这可以节省 dcfaslt ceestucdor 衍始化账引超的级外负担
名称的特殊处理（Name Mangling)
般面言,mombcr 的名称前调会被加上 cass 名称。形成理一无二的命
例如下要的声明，
class Bat I psb11e: ist 1vaiz -.- 1:
其中的ail 有可能变成这样
/ menber 般过 sase-msngling 2指的可能些果2-
iva1__3Bar
为什么编译器要这么缴？请考患这样的逐生操作（de)
elass ree 1 publie Bar ( pseliet ist ivall -.- 1r
记维。Fee 对象内部结合了 bascc class 相 derived class 周者

不管你要处理哪个 hal，通过“aame manging”，都可以绝对清整地指出
来。由于menbe functioas 可以被重载化（cverloaded)。 所以需要更广泛的
anging手法，以提供绝对独一无二的名称。如果把：
class Foistpublie:
fl+et x1)rolid x( float semit 12

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p178_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 179

第 4 章 Fancties i8学 (The 3e
转换%
ciess Peint t
float x_5Peint (1 :

会导致两个被重裁化（oveloaded）的函数实体提有相同的名称，为了让它们独一
无二，唯有再加上它们的参数链表（可以从函数原型中参考得到），妇果把参数
类型也编码进去。算一定可以制造出独一无二的结果，使我们的周个x)函数有
良好的转换（包如果你声明 clem“c”，就会压抑 meemember finctiees 的
rangling”放果)
publle:1ass Point (
rold x_5PointrFt : float sevft 12

以上所示的只是cfom采用的编码方法，我必须承认，目前的编译器并投有
统一的编码方法——虽然不断有一些活动企图导引出这方面的一个工业标准，当
前C++编译器对naemanging的做法还没有统一，但我们知道它忍早会统一。
把参数和函数名称编码在一起，编译器于是在不同的被编译模换之间达成了
一种有限那式的类型检验。举个例子。如是一个prir函数被这样定文
void prist ( eonst Poist3ds 1 ( -.- )
包意外地被这样声明和调用：
// W度 : L%是 const Poist.3ds
两个实体如果拥有挂一无二的nemngling，那么任何不正确的调用操价在

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p179_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 180

深度素 C++ 对象要型 (Joid Tle C+ Oecr Modkl)
链接时期就因无法决议（resohnf）而失败，有时候我们可以乐现地称此为“确保
类型安全的链接行为”（tope-afe lnkage），我说“系观地”是因为它只可以独
提函数的标记（绿注：signaturc.亦即函数名称+参数数目·参数类型）错误：
如果“返目类型”声明错误，就投办法检查出来！
当指的编添系统中，有一种兵通的demmngling工具，用来担截名称并将其转
换团去，使用著可以仍然处于“不知道内部名称”的服大本福之中，然西生命并
不是长久以案一直如此轻始，在chom1.1涨，由于该系统未经世放，放总是收流
两种名称（课注：末经 mangied 和经过 margled 的两种名称）：编评错误消息用
的是程序代码函数名称，然而链膜器却不，它用的是经过mangled的内部名称。
我还记得那些极度告闷、半零在怒，红头发、有省现的工程师，在一个午后，
握据是是地止进我的办公重，历声诸问我列底chet对他的程序代码做了些什么
手脚，这种与使用者之间的互动关系对我面言根斯鲜，所以我的第一个想法是团
答他：“没方，当然什么都没有！听，真的投有，无论加闻，我不知道…你为
付公不去间 Bjumc呢？”我的第二个组接则是平静地询问他问题出在那里（这
优反获得了一些声量，司可）
“这里”，他凡光电哪油向我雅来一金运记结果，并以一种嫌悉的口物告诉
我，链接器试池有一个无续决议（unsolhed）的函数：
_,c3pl_mar44rcnst44
或是一般公认很具亲和性的东西，比期一个4n4矩阵类的加法运算特的
mangling 结果
mat44::opezator+ ( coest mat44s 12
原案，这位老见声明并调用请运算特，但是却忘了定文它！“款”，他况，
“嗯”。他又加了一声，然后他强烈建议我们以后不要把内部名称显示给使用老
看，大体来说，我们采纳了他的建议。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p180_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 181

第 4 8 Function IBB9 (The Semantics ofFunction
Virtual Member Functions（虚拟成员函数）
如果 normaloe) 是个 vintal menber fanctios,。 那么,以 下的调用
ptr->normaLise ();
将会被内部转化为
( * ptr->vptr[ 1 11t ptr 1/
中：
■vptr 表示由编译器产生的指针.指向virnal able.它被安禁在每一个声
明有（或增承自）一个或多个 virtal fusctions°的 class etbjct 中.事
实上其名称也会被“mamngled”，因为在一个复杂的class 派生体系中
可能存在有多个vptrs
■1是virtal table slet 的索引值，天联到 normulioe0 通数。
■第二个 p表示khb 指针：
美氨的道理，如果 mguihdeo 也是一个 vital funcion,它在 xorwalie() 之
中的调用操作将被转换如下：
register float mag * ( *tasa->eptx[ 2 11I this 1/// register float nag * nagnitade()/
此时,由于 Poartz:mgihde) 是在 Pondit:asralior0 中被调用, 至后
者已经由虚拟机制面决议（rselhed）妥当，所以明确地润用“Paimr3实体”会
比校有效率，并因此压制由于虚拟机制而产生的不必要的重复调用操作。
/ 明确的调用操价(exp11citly Lsoocat:ion）全压制虚拟肌制Pgister float meg = Point3d::msgnitude ():
如果magide声明为inlise 函数会更有致率，使用clas scope penmo
明确调用一个 virtal finctiee,，其决议（xnsob）方式会和 non

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p181_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 182

深度接董 C++ 对象很型 (Juiale Tle C+· Olircr Mod
一样
register tloat ma
对于以下调用

如果编译器把它转换为
[ * ob] -vptr[ 1 1 1( 4obj 11
显然语意正确，即授有必要，请忆那些并不支持多态（polymorphiam）的对象
（1.3节）。所L上述经由ety 调用的函数实体只可以是Psint3:norc
“经由一个 clas otjet 调用个 vinal fnctios° 。 这种接作应该总是被编译源
靠对得—般的 nomstatic member fusction 一样地加决议 (resolved) (
norma1it_7Point3drr( icb) 1;
这项优化工程的另一利益是，vintal functien 的一个 hnine 通数实体可以被
扩展（epanded）开束，因而提俱板大的胶丰利益。
Vitual nctiees.特别是它们在继承机制下的行为，将在 4.2节有比校评细
的材论。
StatieMemberFunctions（静态成员函数）
ction，以下网个调用操作

将被转换为一般的moemmber 运数润用，像这样：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p182_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 183

M + 章 Fencios i适.89 (The 3cna

在 C++ 引l人 siatic mer fenctiaes 2前, 我想作很少看到下要这种恒异3
[( Peint3d* ) 0 ↓=>ebject_cosnt (1:
其中 的obyect_coaetg 只是莫单依目_obyet_csser 这个static Catt
membe，这种习惯是如呵演化来的呢？
在引人 static member f:nction: 之前, C++ 适言要或别有的 member functioes
都必巢经血该 cdas 的 otb:=t 未调用. 而实际上, 只有当一个或多个 seestic
da memhers 在 memler ioncen 中被直接存取时。 才用要 clas otjot. Class
otjct 提供了br 提行价这那式的函数调用使用，这个 shb 指针把“在
menber fntin 中/RI) noatic chass members* 定干 *etject 内3座的
nbems之上 如果长有任病一个cembs 被直提存取,事实上就不需要 shi 指
针,因此生缺2有必要通过个 Cas etjet 来调用个 member funtioe. 不过
C++语言到当首为上排不载够识别证种情况
这么一来就在存取 stait 6ms mcmhens 时产生了些不成则性。如果 cls
的设计者把 saic de menbe 声明为 aoepbic (这直被视为是种好的习
票），那么他就必须提供个或多个mcnber fancions 黑存取试menbor.因此
虽然你可以不靠 class object 来存取一个 static mcmber.但其存取函数却得胰定
Jmadan 5hgm、关尔实验重的或列，是我所细第位使用这种方结的人，维世是为 C+
的生要售导者。 Siaier flanitioes 第次正式提出， 是在
198 年 Uwx C++新计会的厂有要习会议上由我所作的个度调中，当时提的主意基
s. 我。 并不令人评染地。 没有密够让 Tom Cmgil 承认多重理要
不大国难。我间时也多少提到了点koran 有关于 ntic的想法
需天康地，他进上了害合，对我们大谈其理念。不过在 [5STROCP94] 文最中，Bgre
一次有到的si

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p183_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 184

深度探素 C++ 对象提型 (Juide TIle C+ Oer AModer)

独立于 cas ohject 之外的存取瘦作，在呈个时候静别重要：当 caus 设计者
希整支持“没有 clas etjen 存在”的情况（默嫌能速的atenct_cont) 那样）
程序方进上的解决之道是很奇特地肥0 强制转型为一个 ches 指针,因药提供出
个r指针实体：
object_coust( ( Poimt3a* 3 9 1/
至于语言层面上的解决之道。 是由cfrort 2.0 所引 人的 sgatic menber
functiees。 Stutic mesber functions 的主要特性就展它设有 sho 指针。 以下的次费
特性统统根源于其主要特性：
■它不能够直换存取其clas 中的 sonati mcmberni
■它不规够被产明为 coest.velatile 或virtusl
■它不需要经自classebjeet才被调用届然大部分时它是这样被
用的！
“momber electiee”语续的使用是一种特号上的便利，它会键转化为一个直
接调用操作：
If ( Peint3diieejeet_eeust () > 1 ) ---
如果clas ctjet 是国为黑个表达式而获得的，会如何呢！例如：
if c fos() -object_cosnt () > 1 ) ---
赛，这个表达式易然需要被评估求值（evaluacd）
//特化，以集存期查用ad)foe(17
sf ( Poist3d::object_count () > 1 )

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p184_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 185

第 4 章 Fncin 8学 (7x Se

cticn,当然会被提出于 clas 声明之外,并始子个
sosigaed Int
objeet_eeent ()
eturh _ebject_ee

金被 cdhoet 转化为
/ 在 cfront之下的内部转化结果

reters _object_count_sFoist3d
其中 SFv 表示它是个 staic  一个S口 (vd) 的事
地表(argunent Iist)
如果取一个satic mcmberfunctiaon 的地址，获得的将是其在内存中的位置，
也就是其地址，曲于 snai menber hunctian 没有 Ahir 指针，所以其地址的类型并
不是一个“指向 casmenber fantien 的指付”，而是一个“sonmmbar 函数指
针”、也就是议：
sfeint3di1eeject_eount ()
会得到一个数值，类型是：
snsigned int (*) (1/
面不是：
usigned iat ( Peint?d::* 3 ()2

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p185_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 186

聚度NR C++ 对B根型 (ee 7e C+ + Oyer Mor/)
famction.它提供了一个意想不到的好处：度为一个 calhack 函数，使我们得C将
C++ 和 C-based X Windew 系度组合 (清看 [YOUNG95] 中的讨论) 它们也可
以成功地应用在线程 (也reads) 函数9上 (进卷 [SCHMBDT94a]) ,
4.2VirtualMemberFunctions（虚成员函数）
我们C经看过了 vitul famcion 的一般实现模型：每一个 chas 有个
vital abie, 内含该 cas 之中有作用的 vintal fintioe 的地址, 然后每个 ohbjoct
有一个vpr，指向 vinl abie 的所在，在这一节中，我要走访一组可能的设i计，
然后根据单一继承，多重继承和虚扣继承等各种情况，从细部上探究这个模型
为了支持vinal facion 机剂，必须百先能够对于多志对象有种形式的“执
行期类型判账续（nusimetpe nsohation）“，也就是说，以下的调用接作将需要
p在执行期的某查相关信息
ptr->s())
如此一亲才能够我到并调用=0的适当实体。
成许最直接了当组是成本最高的解决方法就是把必要的信息加在p异
上、在这样的策略之下。一个指针（或是一个 werence）含有两项信息：
1.它所参考到的对象的地址（也教是当前它所含有的东西）：
2.对象类型的某种编码，或是某个结构（内含某查但息，用以正确决议出)
通数实例）的地址。
这个方出带亲两个间题，第一，它明显增加了空间负担，即使程序并不使用
多本（polmormhism）：第二、它打断了为 C 程序间的链接兼容性。
如果这份额外信息不能都和指针放在一起，下一个可以考虑的地方就是把它
放在对象本身，但是哪一个对象真正需要这些值息呢？我们应该把这些信息放近

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p186_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 187

第 4 章 Fenctioe 889 (The Seruntio ef Fntion)
可能被继承的每一个案合体身上吗！或许吧1但诱考虑一下这样的Cstruct 声明
struct date ( 1st n, 4, rz 1J
严格地说，这将合上述成范，然而事实上它并不需要那些但息，加上那热值
息将使Csm那账并且打破链接兼容性，母我有事案任何明显的补偿利益
“好吧，”你说，“只有面对那些明确使用了claos天键间的声明，才应误
加上额外的执行期信息。“这么敬就可以保暂语言的兼容性了，不过仍然不是一
个够聪明的故策，举个例子，下面的clas特合新规花：
class date ( psbliei ist as, d 7/ 1/
但实际上它并不需要那份保息，下面约cas声明虽然不静合新底花，印需
要那份信息：
struct geos ( psbLici vietual -geoe(1) .- 11
类，是的，我们需要一个更好的现范，一个“以das的使用为基础，百不
在手关键词量chas或mtret (1.2 节）*的规范，如果 chas 真正需要那份信息。
它就会存在：如是不需要，它就不存在，那么，到底何时才需要这份售息？很明
显是在必须支持某种那式之“执行期多态（nntime polymerisn）“的时候，
在C*+中，多态（plymism）表示*以一个 peli bse caes 的指t （或
mferee)。寻址出一个 derived clas ctjer”的意思，或如下面的声明：
Posint *ptz:
我们可以脂定pe以导址岛一个Poa2 对象：

或是一个 Point3d 对象
ptr * bee Foist.3t,

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p187_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 188

深度张素 C++ 对象模型 ( Aide 7r C++ Ohyecr.Mode/I
pr 的多态机能主要扮演一个输选机制（ompoen mesharism）的角色，经由
它。我们可以在程序的任何地方采用一组 pebic derived 类型，这种多态彤式被
称为是消板的（pasive)，可以在编语时期完成vintasl hse claes 的情况除外。
当被指品的对象真正被使用时。多态也就变成积根的（acive）了，下面对于
ital Suection 的调用。 就是务
// *积根多基 (active polymorphLan) * 的常见得子ptr->4()
在 nntime type demtiarion (RTT1） 性质于 1993 年被引人 C++ 酒言之肌,
C++ 3 “积服多态 (active pelymorphism)“ 的唯一支持, 就是对于 virtual fenction
cnll的决议（resoion）操作，有了RT.就能够在执行期查询一个多态的 poinr
成多态的rcfonmce 了 (RTT1 将在本各 7.3 节讨论) ,
/ *根貌多态 (active po1lymorphL.sm) * 的第二个附子
zetscs p34->_s1
所以，问题已经被区分出来，那就是：掌基定哪些cas展现多态特性。
我们需要原外的执行期信息，一如我所说，关键词chas和dnuct并不能够帮酬
我们。由于没有导人如 polymorphic 之类的斯关键词，因此识别一个 clas 是否
支持多态，唯一适当的方核就是看看它是否有任何 virlfascion.只要clan 得
有一个virtual functien，它就展要这份额外的执行期值息
下一个明是的问题是，什么样的额外住息是我们需要存储起来的？也就是
说，归果我弃这样的调用：
Mr->1())
其中=0是一个 vinal finctiee，那么作么信息才能让我们在执行期调用正确的

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p188_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 189

g 4 8 Ivedioe 887 (Te 5cmnKF ot Feneti
n)实体1我需要知道：
■ptr 所指对象的真实类型。这可便我们选择正确的n0实体
■z)实体位置，以便我能够调用它
在实现上，首先费可以在每一个多态的 clas otoct 身上增加两个md
1.一个字特串或数字，表示class的类型：
一个指针。指向某表特，表格中学有程序的 virtual85的执行期
邮址
表格中的 virtaal functiors 地址如何能建构起象? 在 C++ 中, vital fanctians
（可经由其chas ctjoct 被调用）可以在编译时期获知，此外，这一组地址是图定不
变的。执行期不可能新增或特损之，血于程所执行时，表婚的大小和内容都不会改
变，所以其建构和存取管可以由编译器完全事提。不需要执行期的任何介人
然图，执行期备妥那热函数地址，只是解答的一平面已，另一半解答是找到
那费地址，以下阿个步霍可以完成这项任务：
1.为了我到表略，每一个 clas chjen 被安糖上一个由编语器内部产生的
指针，指向波表格
2.为了找列函数地址，每一个vintal fumctios 被指派一个表系索引值
这费工作都由编译器完成。执行期要做的，只是在特定的vital tableslx(（记
浆营 virtual function 地址) 中董据 virtal finction:
一个 clmm 只会有个 vinal table. 每一个 nle 内含其对应的 clas stjxt
中系有 acive virteal fanctions 通数实体的地址, 这盘 active virtaal functioes 包指
这个 class 所定文的函数实体，它会改写（overriding）一个可能存在的

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p189_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 190

深度指素 C++ 对象模型 (Anidr 7he C++ Otyecr.Ms
■继承自 base clas 的函数实得 这是在 derived class 决定不改写 viti
淘non时才企出规的售况
个 pur_wrnesl_cafled() 函数实体, 它服可[ 以扮演 pare virtsal fuec
的空网保卫者角色。也可以当做执行期异意处理函数（有时候会用到
每一个 vinaal finction 都被指派一个因定的素引值，这个素引在整个继承何
系中保持与特定的 vinual function 的关联。例如在我们的 Pan/ claes 体系中
cLass FoLst (
virtual -Noist (11

vi.zteai fioat i0) cosst ( retarn 0; )
protected:Point ( fl1oat x = 0.0 1J
f1oa* _xu
intssl dnear 被Z e 1. E mah) 提赋值 slet 2. 此例并&有 nsi
的函数定(量道:否为它是个 pere vinal fncioe),新以 pr_sital_calleri)
的函数地址公板尔在det2中，如果该函数意外地被调用，通营的操作是结束冲
这个程序- yC N献运 siox3 图r) 被鼠望 slet4. x) 的 diot 是多少？ 答案是注
有。因为 s0 并非 vitual function. 图 4,1 麦示 Psow 的内存市局和其 vita

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p190_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 191

董 章 Fancion @89 (Te Semantis offno

图4.1Virtual Table 的布局：单一组承情况
当一个 claen 累生自 Psier时， 会发生价么事？ 例期 dlas Poin2d
elass Peint2d : pub11c Pesnt (ublic:
Pelst24t tloat x = 0.l, float y = 0.0 )

/…其它量作

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p191_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 192

深度NR C+* 对象根型 (le 7r C++ Obrcr M

典有三种可能性：
1. 它可以继承 base class 所声明的 virtsal fantions 的函数实体。王确地
说，是该通数实体的地址会被携关别 derived claas 的virtual table 相
的 slot 之中。
2.它可以使用自已的函数实体，这表示它自已的函数实体地址必须放在对
度的 sliot 之中,
3. 它可以加人一个新的 virtal functios.这时候 virtsal table 的尺才会增
大一个slet,图新的函数实体地址会被放进试slet 之中。
Point2f 的 vitual uole 在 slet 1 中指出 desweor, 西在 slat 2 中指出
mabg (取代 pumevintul fusciee) . 它自己的 yG 通数实体地址放在 sion 3. 增
承白 Paier 的 r) 函数实体地址则放在 slet 4
类组的增况, Poin3d 提生自 Paint2d 。 如下;
elass Poiet3d : publlc Peint2d (Publiet
Posnt3d( flost x = 0.0, float y = 0.0, f1oat s = 0.0 11 Peint2di x, y F. _zi ± s ( )
Point3d():

// 其E操作float a(1 ot ( retern _sr 1
5e01
loat _a

virtualtable 中 Bslat1R置Paint3

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p192_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 193

第 4 8 Fenctioe BB9 (Th: Semuetios efFenction)
Psint3f:msb) 函数地址. slx 3 放置继承自 Pont2d 的 y0 函数地址, slot4 抗
置自C的 20 函数地址，围4,4 显示 Psint4 的对象布局和其vinal sabic
成在，如果我有这样的式子：
ptr->a()
那么。我如何有足够的知识在编译时期设定 vitul fnctien的调用呢！
■一般面言，我并不知道p所指对象的真正类型，然面我知道，经由p
可以存取到该对象的 vitsal table。
■虽然我不知道需一个20函数实体会被调用，但我知道每一个20函数
地址都被放在 slot 4.
这些信息使得编评与可仁行该调用转化为：
( *gtr->kt 4 11( ptr 11
在这个特化中，ge 说示编运养然变心的脂针，指向vinal abl：4 表示r)
被赋值的 ax 编号 （关联到 Poirr 体系的 vitual tble) 个在执行期才
能知道的东西是：dox4所指的到底是感一个aG函数实体！
在一个单一继承殊系中, vinaa fenction 机制的行为十分良好,不但有效率须
显很容易型造出模型案，但是在多重继承和激担继承之中，对 vinul fnctioms 的
支持就没有那么美好了。
多重继承下的Virtual Functions
在多重继承中文持vinul functioms，其复杂度围捷在第二个及后继的 basc
cases 身上。 以及 *必算在执行期调整 rbr 指针” 这点, 以下调的 clas

cLass Base1 (
pubiic:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p193_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 194

NR C++对&编型(/e 7h C++ Oye
Baselo,-Base1(11
virtual Base1 *clone 1) const:virtual roid s
protected:floet data_Baseli

base2():
virtual Bese2 *clene()virtual void e

Derivee(1/
virtaal Derived *clone () coeat/vrirtoal -Derived(1 r
protected:fioat ata_0erived;

谭注：上述例子无法通过VC3 编译，会出现如下错误酒息：
emor C25ss: Derived:xdlonf' : overiding virtuasl fenctios difers fe
Aase1 :scloe only ty etum tpe er alling comveatiou
emor C25s5: Derived:xlene
donr' oely by ntem tpe er calling co
担这是园为VC3未符合C++标准之妆，原本C++规定，改写函数
（overing fanstien）的类型。包括函数名称，参数列、返用值类型，都必须和
被改写函数（evemided fmcin） 相间。本例由于 Dernef 抓生白 Basr/ 和
e2, 质以面时“Bae/:cioe0 传因 Bas/*” 面 “Base2:clom( 传图 Bax2*”
这两种增况，Deriel:clme0无所适从，热我时至今日，C+标原已针对此项
了修改，为的是容许所调的虚担构造函数（vinutor) 。鲁见 p.166

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p194_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 195

第+ 8Fuentioe 889 (The Semntios ofuntin)
“Derhed 支持 vitusl flnctions” 的国难度， 统统落在 Bese? ubetjetl
上。有三个同题需要解决，以此例否言分新是(l) vintual deitractar. (2)批继承下
首先，我把一个从hap 中配置而得的Derinf 对象的地址。指定给--个
Ban2指村;
Base2 *pbese2 • sev Derived:
新的 Derief 对象的地址必须调整。Ll指向其 Bas nsbotject. 编通时期会
产生以下的码：
as42 *yo*se2 + tenp 7 teep + siseot ( Basei 1 / 0)erived *tenp
如果没有这样的调整，指针的任何“8多态运用”（像下面那样）都将失
// 即使 pbas2 被指定一个 Derived 对象，这也应动设有问题[=)data_aase2.
当程序员要删胞phase2所指的对象时：
//3的集行可能需要调整，以基合完整比象的起始点
指针必须被再一次调整，以求再一次指向Derled对象的起始处（推测它还指向
Derhnd对象)。然医上述的efse 加法如不能够在编评时期直接设定。因为
pase2所指的真正对象只有在执行期才能确定。
般规则是，经由指向“第二成后继之 hase clas”的指针（或 nefernce）来
调用 derived cdas vitasl fanction- 通建留就藏冰的运
Bse2 *pbase2 = nev Derived;
delete gbase2: // iavoke derired ciass's des

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p195_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 196

深NR C+* 3 B.模型 (/emide 7hr C++ OheorModn)
该调用操作所连带的“必要的指针调整”操作，必须在执行期究成，
就是说, ofset 的大小, 以及肥 offet 加到 ke 指针上头的那一小段程序代的
必须由编泽器在某个地方拍人，间睡是，在那个地方？
Bjame 原先实施于 cbeet 编评器中的方法是务 virtal table 加大, 使它容浆
此处所需的aos 指针，调整相关事物，每一个 vital attesat，不再只是一个作
针。或是一个聚合体, 内含可能的 ger 以及地址。 干是 vitul functie 的调甲
播价由：
(*pbase2->vptr[1]1 ( pbase2 1/
改变发
( pbase2 + pbase2->vptr[1] .offset ) ;
其中 jde 内含 vintal fanctio 地址。 eer 内含 hiv 指针调整值
这个做出的缺点是，它相当于连带处罚了所有的 vintal functioe 调用操作。
不管它们是否需要dfer 的调整，我所课的处罚，也括afher的额外存取及其
张，以及每个 vintal tatble slet 的大小改变。
比较有效率的解扶方法是利用所谱的busk，当我第一次学到这个字服的时
候，我的教授开玩芙地告诉我，hurk是 knuh的例辨字，所以他把这项技术归
功于 keuh 排± O.
谦注： Dorald E. Knuth, 写出经典名著 The .An ef Comwer Pogrmmlng 的
那个人，此套书籍被为“he bile offindame lporibms”，说拥有的人得
多，看过的人很少○.目前（1998）出版有
Volane 1 : Fund
rical Algorithma 3/e

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p196_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 197

Thurk 投术初次引进到编译器技术中，我相信是为了支持ALGOL提一无二
的 pas-by-rane 近意。 所调 hunk 是一小股 asembly 码, 用案 (l) 以适当的
offet 但调整 shir 指村, (2) 就别 virteal fenetios 去。 例妇. 经由个 Basr.? 指
针调用 Derined destmuctar， 其相关的 tunk 可能看起来是这个样子：
//盘拟c+码

Bjarme 男不是不翔道 thusk 技术, 间题是 thunk 只有以 assembly 码完成才
有效率可言，由于cho 使用C作为其程序代码产生语言，所以无法提供一个
有效半的 由unk 编证器
Thunk 技术允许 vital ualesdae 继续内含一个横单的指针，因此多重继承不
需要任何空间上的原外负担。Slkes 中的地址可以直接指向vintul function，也可
以指向一个相关的 daunk（如果需要调整dba指针的话）.于是，对于那些不需
要调整dhr 指针的 vintl fmcion （相结大部分是如此，虽然我手上投有数据）
面言，也就不需承载教率上的额外负担。
调整aia指针的第二个额外负担就是，由于两种不同的可能：(1）经由
deived clas (或第个 hase dlas） 调用. () 最由第二个(或其后理)baoc claes 调
用、同一通数在 vintaal tabic 中可能需要多笔对应的 slats,例如:
3rsae2  -pbase? - as Derivoedr

虽然两个 dine 操作导致相同的 Dernef dcstnlor，但它们需象用个不同民
rirtal table slot:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p197_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 198

深度指素 C++ 对 象模型 Awe 7 C++ Olyer.Adn/
1. pbare/ 不需要调整 skur 指针 (因为 Base/ 是最左竭 base class 之致
它巴经指向 Derhed 对象的起始处)。其 vintual tsole slee 需放置真正
的 destructor 池址
2. pbae2 胃要调整 shur 指针。 其 virtual table slet 需要相关的 tbusk
在多重继承之下,一个 deried caem 内含 a-I 个原外的 vintal ubles. = 丧
示其上一层 hae class 的数目（因此，单继承将不会有照外的 vintal ables)
对于本例之 Deriwd 面言，含有两个vital tablo 被编译器产生出来
1.一个主要实体，与 Bae/ （最左明 baie class）共享
2.一个次要实体，与Bae2（第二个 base class）有关。
针对每个 vinal ubles, Derief 对象中有对应的 vptr. 图4.2 说明了这
B.vprs 婷在 comsNm(s) 中被设立初值（经由编译器所产生出来的码）：
用以支得*一个cas 舞有多个vial abes的统方除是，将每一个 uole
以务部对象的形式产生出来，并给子睡一无二的名称。例期。Dered 所天联的
两个aes 可能有这样的名称
tb1__Derivedr//王要表香
于是当你将一个 Deriwed 对象地址指定给一个 Bar/指针减 Dered 哲付
时，被处理的 vital abe 是主要表格 nd_Dered. 而当你事一个 Deried 对
象地址指定地个Baz2 指针时。被处理的vintul tube 是次要表势
aM_Bae2_Derived.

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p198_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 199

图4.2Virtual Table 的布局：多重继承情况。
（译注：右下角的三个星号，就是下页的三种情况）
由于执行期链独器（nntine inkms）的降格（可以支持动态共享函数库），
符号名称的储接可能变得非营缓慢，量慢可到-在一部SparcStioe10
工作站上，每一个毫移（ms）只处理一个符号名称。为了调节执行翻链接器的效
率，Sun编译器将多个virmlabes连债为一个：指向次要表格的指针，可自主
要表格名称加上一个efftet 获得，在这样的嫌略下，每一个 cas只有一个具名

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p199_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 200

尿度NR C++ 对8使型 (Aeile 7her C++ Oyer Mode)

的virtaal tuble.“对于许多Sun 项目程序代码面言，速度的振升十分明显°2
精早我曾写道，有三种情况，第二或后继的 hasc clas 会影响对 vitial
oms 的支持，第一种情况是，通过一个“指向第二个hase cas”的指针，起
邮 derived class virtual functien. B[t
Base2 *ptr = rev Derired;

delete ptr:
从图 4.2 之中，你可以看到这个表用握作的重/p 指向 Derhed 对象中
的 Base2 subobjet; 为了能够正确执行, pe 必须调整指间 Derind 对象的起始处
第二种博死是第一种情况的变化，通过一个“指向 darived cans”的指针，清
期第二个 baee claes 中个继承而来的 vintal fntion, 在武情况下, derived clas
脂针必须再长,民整，以指向第二个 bese sebobjet.例如
Derived *poer = sev Derired:

der-ze（)
第三卡况发生于一个语言充性质之下：允许一个 vimul fonction 的返
但类型有所无化。可能是 baetype ，可能是 pebicly derived type，这一点可以
通过 Deriesi:cloe0 通数实体来设明.clone 函数的 Derhnd 版本特图个
Derinf clas 指针，默款地改写了它的两个 bae cas 通数实体。当我们通过“指
向第二个 bese dlass 的指针来调用 clome(时, sktar 指针的 oet 同题于是建生

2 著出自 Mie Bal (Sue C++ 编济著的建抑者) 与我的通生

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p200_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 201

B + # Fentioe I8.89 (The Sema
Bass2 *pb1 = stv Eerived;
Base2 *pb2 = pb1->c1ose[))
当进行 pbj->clomr() 时, pb/ 会敏调整指向 Derned 对象的起始地址,于是
chor) 的 Doeriwed 驱会被调用; 它会传闭个指针, 指向个新的 Derienf 3
象：该对象的地址在被指定始pA2之前，必须先经过调整，以指向 Bse2
abetjeel
当面数被认为“足够小”的时候，5m 编评器会提供一个所调的“splt
fntes”技术：以相网算法产生出两个通数，其中第二个在返回之前，为指针
加上必要的 efet，于是不论通过 Beme/ 指针减 Dernwr 指针调用函数，都不其
要调整返居值：至通过Bae2指针所氧用的，是另一个函数
如果函数并不小，“splitfnnction”聚略会始予此函数中的多个进人点（emny
points) 中的一个， 每一个进人点需要三个指令， 包 Mae Ball 照办张去除了这项
成本，对于0O 授有经验的程序员，可能会怀疑这种“spltftaction”的应用性
然面0O程序员都会尽量使用小规模的 vintualfianction 将操作“周那化”，通常
winual fanction 的平均大小是 8 行3,
函数如果支持多重进人点，就可以不必有许多“bunkas”，如IBM 就是把hunk
楼愚在真正被调用的 vinal fancien 中.函数一开始先（b） 调整 khur 指针，然
后才（2）执行程序目所写的函数码：至于不需调整的函数调用操作，就直接进人
()的部分
Micnson 以所调的“sdres poins”来取代 dhunk 策略，即将用来改写别人
的那个函数(也就是overiding fntien）期持获得的是“引l人该 vinal fenction 之
3 一个 vitad hnctioe 的平均大小是行，这是我在第时某地所来的说量（不过我忘了是
何时得地)。这一说法养合费个人的经验。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p201_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 202

深建围素 C++ 3象级型 (Juidle Tive C++ Oyrcr Moder)

class” (通鲁 derived class) 的地址。 这就是该通数的 “address
财此有完整的讨论。
虚拟继承下的Virtual Functions
考虑下黄的 vitual base clas 激生体 感。 从 Poiw2d 区生出 Pour
class Point2a i
0.0, f14at = 0.0 11

irtaal float ±():
fiset _x- _Y/

Peist3dt float = 0.o, fioat • 0.0, fltet = 0.0 1.Foint.3d11 :
protfloat s()
float _==

最然 Pont3e 有唯—个 (同时生是最左造的) bas cas, 也就是 Poon2/,
包 Por3d 和 Pon2d 的起始部分并不律“非虚担的单一糖承”增况那样一致。
这种情况是示于图4.3.由于Pvcint2f 和Point3d 的对象不再相符，两者之间的
转换也就黄要调整aha指针，至于在虚拟继承的情况下要消除huks.一般而方
已经被证明是一项高难度技术。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p202_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 203

第 +章 Fenctioe i88学 (The Senartios efinct

图4.3
Point2d::
当一个 vital base claes 从另一个 vinal base clas 振生而米，并且两者都支
挣 vital fnctios 和 nnstatic datamem 时,编译腊对于 vital he cas 的
支持黄直就像进了进官一样，虽然我手上有一整柜带有答案的例程，并且有一个
以上的算法可以决定适当的aets以及各种调整，组这些家材实在太过诺调述
离，不适合在此处过论！我的建议是，不要在一个 vinulbase cdhas 中声明
datmmbm.如果这公做，你会距离复杂的深国意来意近，将不可拨

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p203_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 204

聚度浆素 C++ 对象根型 (Juridle Thr C++ Oincr AMod

4.3函数的效能
在下面这组测试中，我在不同的编泽器上计算两个3D点。其中用到一个
nember friend fanction. ↑ member function, L及个 viftual merbys
fncion，并且 Vimual memher fanctiee 分别在单一、拟。多重想承三种情况下
执行。下面就是 ronmember fanctien:
eidEross_prodect ( const Poist3d spA, const Poist3d spB)
Foist3d gC.
sC.1 - pA.x * ph.y - jA.y * p8.xi
aint) 函数看起来律这样 （调用的是 nommsber fani
0.317,

retsrs 0:

如果调用不间形式的函数，当然mcm0就需要改变，表格4.1列出别试结果

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p204_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 205

第 4 童 Fantios i889 (The S  O ef Fasc

表格4.1函数效率
0 .06
Bossesber Triesd  4.43  6.13
Static Menbes  4.43  6.12
4.43  6.12
Firtusl Menb+
4.73  6.0
nec  4.99  7.96
per （虚拟组承）
5.20  7.03
如 4.2 节所材论的。nom
数都被转化为光全相间的形式。所以我们毫不惊诱地看到三者的效事完全相同。
但是我们很馆讶地发成，未优化的imlise函数提高了25%左右的效率，而
其优化版本的表现简直是奇造：这是忽么国事
这项你人的继果日功于编评器特“被机为不变的表达式（espresiors）“提
到提环之外，因此只计算一次，武列显示，intint函数不只能够节省一般函数调
用所带来的额外央担，也提供程序优化的最外积会。
我对 vintalfinction 的调用是通过一个 refemmoe，而不是通过一个对象，由
此我就可以确定调用操作确实经过了虚机制，效率的障低程度队4%到11%
不等，其中的一部分反膜出Paint3d contctor 对于ve 一千万次的键定操作，
其它则因为 CC 和 NCC两者（或至少在 NCC 的“ctront 董容模式”中）使用

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p205_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 206

探度强素 C++ 对象根型 (Jmile 7e C++ Obyer Modrl
了 dclta-ofhet (偏移差值) 模型来支持 virtual functi
在该模型中，著要一个cfset 以调整hs 指针，指向放置于vintl tuble
的适当地址，所有像这样的调用形式
ptr->virt_fne(1:
部会被转换为
/ 宽川 vLrtusl tunct.ion(*gtr->,_yptr[ 1sdex 1.addr)
ptr->_sptrI index I .delta 1 / 鼻 this 提树的调里
著至即使在大部分调用原作中，调整值都是0（只有在第二或后继的basx
clams 或 vinalbae can 的情况下，其调整值才不是0)，在这种实现技术之下
不论是单一继承或多重继承，只要是虚拟调用操作，算会消耗相网的波本，当然
在unk 要型中。o 指针的调整成本可以被局限于有必要那么缴的通数中。
多重继承中的vitul function 的调用。似手用掉了较多的成本，这今人感到
困感，当有人期量编译器实现出hunk 模型，以调用第二个或后继的 baocca
的vitalfncion.面用来测试的这两个编译器邮不支持duk 技术，就会得列
这种继果，由于bo指针的调整已经施行于单一继承和多重继承中，欧其额外负
组不能用来解释这项成本
当我在单一继承懂况下执行这项测试时，我也困感地发现，每多一层继承
vital function 的执行时间就有明显的增加。一开始我无法思象其同发生了什么
事，然面当我成为这些程序代码的“最佳男主角”够久了之后，我路于渐情了。
不管单一继承的保度如何，主握环中用以调用函数的码事实上是完全相同的：同
排道理，对于星标值的处理是完全相网的，其间的不网，周时也是样早前我授
有考虑到的，就是cro_proact) 中出现的局那性Point3Mf clas etjet pC，于是
defaak Poeint3f cosucter 被调用了一千万次。增加糖承尿度，就多增加执行成
本，这一事实上反映出pC身上的oeter的复余度，这也能都解释为什么多
172

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p206_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 207

第 + 章 Fesctice 1889 (The Semantis efFensti
重健承的调用另有一些眼外负担。
导人 vinusl finctie 之后, clas contrcter 将获得参数以设定 vintaal uable
指针。 CC 和 NCC 都不能够把这个设定操作在*无任例 vitual fncion 之 bao
clas建构时优化，所以每多一层继承，就会多增加一个额外的vpe设定，此外，
不论在 CC或NCC，下面这个期试操作会被安膜到comticler 中，以国费兼谷
C++ 2.0
中国调用
/// saer code goes bete
在导人nrw 和dlere 运算符之前，承担clas 内存管理的唯一方法就是在
ctor 中指定 sha 指针。洲才的  判斯也支持该做密，对于 cfom,“shi
的指定操作”的语意居期兼容性一直到4.0版才获得保证（由于各种超越本文范
国的排秘理由之故）、有讽新意糖的是，NCC是SGI产易的“与chom兼容”
模式，图以NCC供了这种用图兼容性，除了图兼容，不再有任何理由带
要在contrcthr 中含入附才的携试.成代的编洋器肥nn 运算符的调用换作
分离开来，就律把一个运算队coestructor 的调用中分高出案样（6.2 节）。“
指定操作”的连意不再由语言来支持。
在这杰编译器中，每一个额外的 base clas 或级外的单一继承层次，其
uctaor 内会被加人另一个对Air 指针的期试（就本例图言是不必要的）.
执行这些coatrtar一千万次，效率就会因此下降至可以则试的程度，这种效率
表现明是月应出一个编评都的反章，而不是对象模型的不正章
应任何情况下，我想看看是否cotnctiom调用操作的额外损失会被核为额
外花觉的效率就间，我以两种不润的风格重写这个通数，都不使用局部对象：
1.在函数参数中加上一个对象，用以存放加法的结果：
oid
cross_perodect (Foiat3d 4pC, coest Posst3d ipA, const Feint3d sp

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p207_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 208

探度探素 C++ 对象要型 (buids Thle C++ Oljier 3ouk
/xBa,y * pa.1 - pA.1 + pa-yi
2.直接在thu对象中计算结果：

两种情况中，其未优化的执行平均时间为6.90秒。
有趣的是这个语言并不提供一个机剂，指示是否一个del stor并非
必要面应该省略，也就是说，局都性的 pCcdian atject 即使求被我们使用，它还
是算要一个cotstractor世我们可以经由消腺对局部对象的使用。面消除其
censtrudor 的调用操作。
4.4指向MemberFunction的指针
(Pointer-to-Member Functions)
在第三章中我们已经看到了，取一个sonaie duna mxmber 的地址，得到的
结果是慎member 在 chan 布局中的 bytes 位置（再加 1），你可以想象它是一
个不完整的值，它需要被脚定于某个clasotbjet 的地址上，才能够被存取，
取一个 mmie member fnctiae 的地址，如果该函数是novital.则得到
的结果是它在内存中真正的地址，然面这个值也是不完全的，它也需要被雾定于
某个cdas stjet 的地址上，才能够通过它调用该函数，所有的nomsmaricemmher
Tnctions 都其要对象的地址（以参数 shiur 指出）。

174

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p208_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 209

第 +# Fentice i逐89 (The Se
因顾下,一个指向 mer动iee的指针,其男语显险下
eubie  / retirn type

然后我们可以这样定文并初始化请指针：
double (lNesatii*coord) () = sPelst iix.
也可以这样指定其值：
cseed • sPoist :1yr
继调用它，可以这么做
( origin.*coerd 1()/
威
{ ptt->*coed 1()1
这些操作会被编译器特化为：

指向 memberfnctin 的指针的声明语续，以及指向“member slectios 运期
符的指针，其作用是作为bo指针的空网保留者，这也就是为什会staicmembe
fncies （没有 Aka 指针）的类型是“函数指针”，而不是“指向 mcnberfsci
之脂针”的源园

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p209_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 210

聚度影素 C+* 对象模型 (fmile 7e C++ Ohyer Modr)
使用一个“member fmction 指针” 如果井不用于 vintsal funstiee， 多重增
求. virtsal base class 等情况的语, 并不会比使用个 “nonmenber functien 指什
的成本更高，上述三种增况对于“mcmbor functioe 指针”的类型以及调用都大过
复杂,事实上,对于那些授有 vinal fnctiaes 或 vinsal base cdas,或 multiple base
claises 的 clases 而言，编还器可以为它们是供相网的效事。下一节我要讨论为
什会 vintsal finctioes 的出现。会使得“menber funsction 指针”更复余化
支持“指向Virtual MemberFunctions”之指针
注意下面的框序片段：
flat (Psist*ef) ()sPeint:1u)
paf. 个指肉 member feection 的指针, 被设值为 Poinr:a0 (个 vinua
finction）的考址。 pp 则被指定以一个 Psint3y 对象。如果我们直模经由 po 到
用 =0：
P->g(1;
别被现用的业Poiur:a)。但如果我们员ray 别接调用a) 呢？
( ptr->*pef 1 ()
仍然是Psia)被调用吗？也氧是说，虚损机制易然能够在使用“指向
ber asc:r之指针”的情况下运行吗？答案是 yes,问题是如向实规呢？
在期一小节中，我们看到了，对一个sosmaic mmber funcion 取其地址，济
获得该函数在内存中的地址，然西面对一个vinulfinciee，其地址在编译时期
是未知的，所能知道的仅是 vinal fucicn 在其相关之vital alble 中的索引值。
也就是况，对一个 vinual momber fanction 取其地址，新能获得的只是一个素引值

176

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p210_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 211

第4 8 Iuncim 语B学 (Te5s

例如， 假设我们有以下的 Poinr 声明：
ciass Point
float x()
virtaal floet s(1:float yt)3

然后取dctructor 的地址
sFoisti1-Peint;
得到的结果是1.重r)或y0的地址：

将到的则是函数在内存中的地址，四为它们不是vitul.票r0的地址
sPeint::±(01 r
得到的蜡果是2.通过pm/来调用r)，会被内部转化为一个编泽时期的式子。
一般形式如下：
( * ptr->eptr[ (int)pnf 1)t pte 1;
对一个“指肉 mcmberfinction 的指针” 评估求值（eraluated），金因为该值
有阿种意文而复杂化：其调用操作也将有别于章规调用操作.pf的内部定义，
性就是
flost (lelat11*gat) (1))
必须允评镇通数能够寻址出noevital x) 和viteal a) 胃个menb
fanctions，而那网个通数有看相间的原型：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p211_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 212

探度探索 C++ 对象模型 (Juide The C++ Ober Modi/)
//二老都可以被脂定始 pet

只不过其中一个代表内存地址，另一个代表 viataltabe 中的索引值。因此
编译器必领定义pa使它期够（1)含有网种数值.（2）更重要的是其数值可以神
区别代表内存地址还是 Vinteal tublc 中的素引值。你有好点子吗？
在com 2.0 非正式服中。这两个值被内含在一个普通的指针内，cfee如
何识期该值是内存地址还是 vintual tabl 素引呢？ 它使用了如下枝巧
（4t ±st ) pef ) s =127 1// son-virtual Levecatios

加 Strcustup 在 [LIPP88] 中所设
当然啦，这种实现技巧必须假设组承体系中最多只有128个vitualtuncions
这并不是我们所始望的，但都证精是可行的。然而，多重继承的引入，导致需要更
一般化的实现方式。并链机排去对 vital tunctions的数目限制，
在多重继承之下，指向MemberFunetions的指针
为了让指向 membar fiunctioas 的指针包能够支持多重继承和虚拟继承。
utnep 设计了下图一个结构体 （([LIPP88] 中有其原始内容）：
sesber funct.iors IfH
union (nt indesj
ptrtofuseiat
_otfset.

178

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p212_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 213

第 4 # Fentioe i8学 (The Ser

它们要表现什么呢: nder 和 adb 分别 (不网时) 母有 vitual table 素引8
nosvintual memberfanction 池址 (为了方便, 当 index 不指肉 vinal able 时 盐
被设为-1）。在该模型之下，像这样的调用操作：
( ptr->*pmf 1(1)
会变成：

( * ptr->vgtr1 pgf.index)(ptz) 1)
这种方独所受列的独订是，一个调用操作都得付出上述成本，检查其是否
为 vinal 或eral。 ivon 把这项检查拿排，导人一个它所请的vcall
hask. 在此策略之下， fac 键指定的要不就是真正的 member fintiee 地址（如
果通数是ncimitua：治话)，要不次显wll unk 的地址-于是 vitual 减 soevitsa
函数的调归量作透明化, v 心mk 会活出并调用相关 vinul table 中的适当 slot
这个结构体的另一个到疹用就是，当特通一个不变值的指针给membe
Aanctioe时，它需要产生一个额时性对象，辜个例子。如果你这么效

// ..
其中&Poin3d:ormal 的值类组这样：
( 0, -1, 10727417 1
将需要产生一个额时性对象，有明确的初值：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p213_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 214

深度指素 C++ 3象吸型 (Juidr Tle C+ + Oirer Mod
/ 盘报 c++ 异
_APtr te8p * ( 0, -1, 10727417 )
fool p, teap 1/
再图到本节一开始的那个结构体，ahe字胶表示ko 指针的ofet 值，到
.er 字段放的是一个 vintsal (成多重继承中的第二或后增的） baoc clas 的 vt
位置，如果vper 被编详器放在clas 对象的起头处，这个字股就没有必要了。代
价则是C对象兼容性降低（请闭额3.4节）、这些字段只在多重继承或虚拟继
承的情况下才有其必要性，有许多编评器在白身内那根据不同的cluics特性提供
多种指肉 member fenctioms 的指针形式，例如 Mikrosed 算供应了三种风啡
1.一个单一继承实例（其中带有vcall thunk 地址或是函数地址）：
2. 一个多重继承实例（其中带有ab 和 adte 两个 mmbers）：
3.一个虚担继承实例（其中号有医个mcmbers）。
“指向MemberFunetions之指针”的效率
在下图一组测试中，crs_prodhct) 函数经由以下方式调用：
1. 个指向 sonmenber functioe 的指针;
2. 一个指向 clas member fsnction 的指针;
3. 个指向 virtual mmher fusctios 的指针:
4. 多董继录下的 nonvirtual 及 virtual menber functios call
5. 虚拟继承下的 noevirtual 及 virtual mcmber functios call.
第个期试（指肉nomtioe的指针）以下列方式进行

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p214_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 215

第 4 章 Funtion 8学 (The 5er

retuzn 0
第二个别试。“指向mcmber funcion 之指村”的产明和调用操作加下：

ters < 100300: sters++ 1
不论在 CC或 NCC中，都会把上述操价转化为以下的内部形式。于是以下的
pA.*pet r pe 1: // 9性: I号为 ( *pA.pet 1 ( pe 1 鲁为词
金被转化为达样的判康：

(4ph - pA._ptr_lostdt pef.Lsder 1.deita, pB)/*pM-_ptr_Peist3dl pnf-index 1 .tadd
还记得吗，一个“指向memher funtion 之指针”是一个结构，内含三个字
股: index、 fasir 和 dia. inder 若不是内带一个相关 vitual able 的素引望,
就是以 1 表示函数是 neevintaal, Ade 带有 meevirtal momber fnction 的地
址，ddle带有一个可能的 hu 指针调整值，囊格4.2显示测试的结果。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p215_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 216

家腺家 C++ 对象类型 (/dr 7r C++ O

表格4.2函数指针的效率

向  -VLrtua:
4.39  7.65
BCC  $.35  4.30
$.7  4.09
5.641,70  3.39
导霖  8.72

.84  8.80

4.5Inline Functions
下面是Painrcan 的一个加能运算符的可能实成内容：
elass Poist (friend Point

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p216_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 217

B 4 8 Fuetion 889 (Tlie Sesanties ofFunci
Peint sev_ptr
new_pgt._y - ihs._x + rha._yr
return ses_pt/
理论上。一个比粒“手净”的继法是使用 ilin 函数束完成nct和gcl 函数

nee_pt.x( Lbax() + rhs.x() 1J
血于我们受限只量在上述两个函数中对直模存取，因此也就将稍后可能
发生的data mmbers 的改变（例如在理承体系中上移或下移）所带来的冲击最小
化了，如果把这些存取函数声明为inlise,我们就可以继续保持直接存取mcnbeg
的那种高效率一员然我们亦兼顾了函数的封装性，起外，加法运算养不再要要
甚产明为 Paine 的个 Biend
然面，实际上我们并不能够强道将任何函数部交成 imline-星然cfom 客
户一度错经发出一封权限板高的够改需求，要求我们加上一个mar_ior关键
调, 关健词 inline (或 class declaration 中的 menber functien 或 triend fenction
的定义）只是一项遣求，如果这项请求被接受，编评器就必领认为它可以用一个
表达式（esresion）合理地将这个函数旷服开来。
当我说“编译器相值它可以合理地扩展一个inline函数”时，我的意思是在
某个限次上，其执行成本比一般的函数调用及返回机制所带来的负荷低，cm有
一套复杂的测试法，通意是用末计算 asigmets，fction cals.vinal functiot
c% 等作的次数，每个表达式（esesion）种类有一个权留，面ininc 函数
的复杂度就以这费操作的总和来决定。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p217_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 218

聚度N浆 C++ 对象提型 (/nide 7he C++ Obyeer Moder)

般面言，处理一个inine 函数。有两个阶段：
1. 分析函数定文，以决定函数的“inrinsieilise abiry”（本质的 inlinc
力)。“intriasic”（本质的、固有的）闻在这里意指“与编译器相关
如果函数其复杂度，或因其建构问题，被判账不可成为iline，它会被转
为一个c函数，并在“被编泽模换”内产生对应的函数定又、在一个支持“膜
块个别编译”的环境中，编详器儿手投有什会权宜之计，理思辆况下，随接器会
将被产生出来的重复东西清理掉，然面一般来说，目前市面上的链接器并不会种
“随波调用而被产生出泉的重复调试结息”清理障，UNIX 环境中的srp 命
可以选到这个目的
2.真正的inline函数扩展原作是在调用的那一点上，这会带来参数的求值
操作（eralhatios)以及癌时性对象的管理。
同样是在扩展点上，编译器将决定这个调用是否“不可为intinc”，在chos
中，inline函数如果只有一个表达式（cspresion），则其第二或后继的调用操作：
nes_pt-x( 1hsx() + rhax(1 1
就不会被扩展开案，这是因为在cho中它被变成：

这就完全投有带来效率上的改善！对此，我们唯一能够缴的就是重写其内容
seu_pt.x( 1hs-_x + zhs-_x 1
其中的批注。是要让未亲读这份码的人知道，我们管考虑使用pubic inin
inderfioe，包都必须定图头路

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p218_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 219

第 4 # Fundtioe IB学 (The Semanties of Fenetion)

其它编译器在处理 iline 的扩展时，有像cfhomn 这样的束障写？不！然到
很不非地, 大部分编译器厂商 (UNIX 和 PC 都有) 但乎认为不值得在 inlinc 支
持技术上做讲细的讨论。通常你必须进人列汇编器（assenber）中才能看到是？
真的实限了inine
形式参数（FormalArguments）
在inline 扩展期间，到庭真正发生了什么事情？暖，是的。每一个形式参数
都会被对应的实际参数取代，如果说有付么别作用，那就是不可以只是简单地一
一封塞程序中出现的每一个形式参数，因为这要导放对于实际参数的多次求值据
作（evalatiomns），一般图言，面对“会带来副作用的实际参数”，通常都雾要引
人临时性对象，换仰语说，如果实际参数是一个拿量表达式（cosantepresion）
我们可以在替换之前先完成其求值操作（evalutiams）：后继的inine 替换，就可
以把拿量直膜“撑”上去，如果既不是个常量表达式，出不是个带有副作用的表
达式，那么款直接代换之。
个得子。银设我们有以下的前单ininc 通数：

zetuzn 1 < 1 7 1 1 14
下要是三个调用推存
knline int
int aliema1/

/* (3) */ minral = nin( feo(), ber()+1 ):

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p219_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 220

深发探索 C++ 对象模型 (Auuadr Zhe C++ Olyuct Aod

returh niwali
标示为（1)的那一行合被扩展为：

标示为（）的那一行直接拥然常量
//（2)代换之后。直糖使用常量LEva2*1024:
标示为（9)的那一行则积发参数的副价用，它需要导人一个额时对象，以
免重复求低（msltiple evaluatioss）
// (3) 有到查用，医以导人糖时对象

t1 < t2 7 t1 : t2:
局部变量（Loes!Variables)
如果线们费患改变定文，在aline定文中加人一个局都变量，会怎样
I*Lise intai( snt L, Lst ) 1

这个局部变量需要什么级外的支持成处理码！如果我们有以下的测用操作
int local_var:int minraL;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p220_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 221

第 4 童Feactice 证89 (The Sot

iine 被扩展开亲后，为了维护其周部变量，可能会成为这个样子（理论上
这个例子中的局部变量可以被优化，其值可以直膜在mal中计算）：
nt miEvai2Mt local_vaF
/ 将 islise 最数的局多变量始试“nang元ing” 提价
-ALr_< a12 7 a11 : a12 1,
_8Ls_1_sinma2r
一般而言，inine函数中的每一个局即交量都必须被放在函数调用的一个封
用区段中，拥有一个挂一无二的名称，如果inline 函数以单一表达式（epresicn）
扩展多次，那么部次扩质都需要自已的一组局部交量，如果iatine函数以分离的
多个式子（discretesatmems）被扩展多次，那公只需一组局部变量，就可以重复
使用（谭注：因为它们被放在一个封闭区段中，有自己的sope）。
ilins函数中的局部变量，再加上有副作用的参数，可能会导致大量临时性
对象的产生。特别是如果它以单表达式（exspresiee）被扩展多次的话。例如
下图的调用操作：
ninval = min( va11, va22 ) + sis( foo(), foe()+1 1
可期被扩展为
int _nin_1v_sinva1_01i

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p221_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 222

深度探索 C++ 3象模型 (Juiw TIle C++ Oljiner Model
/为放置到作用值面产生糖时变量

_ais_1s_8inma1_00)va12 ),
(1 _mts_1y_msiamal_01 = ( t1 ● foo() 1,

hine 函数对于封装提供了一种必要的支持，可以有效存取装子clas 中
的nopuhlic 数据。它间时也是 C程序中大量使用的 define（需置处理史）的
一个安全代替品—特别是如是宏中的参数有副作用的话，然而一个inrinc函数
如果被调用太多次的话，会产生大量的扩服码，使程序的大小暴意。
一如我所描述过的，参数骨有断作用，或是以一个单一表达式做多重调用。
或是在iline通数中有多个局部交量，都会产生物时性对象，编译器也许（或包
许不）能够肥它们移除，此外，ininc中再有 inline，可期会使一个表面上看起来
平凡的iine却因其连锁复杂度而改办接扩服开案，这种增况可能发生于复杂
Elas 体系下的 comstrnetoes,或是 otjet 体系中一些表图上并不正确的 inine 调
用所组成的串链——它们每一个都会执行一小组运算，然后对另一个对象发出请
求，对于原要安全又要效率的程序，illne通数提供了一个强面有力的工具，然
面，与m-iline 函数比起来，它们要更加小心地处理

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p222_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 223

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 224

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 225

第5 章相进、邮病，携员语意学（

第5章

构造、解构、拷贝
语意学
Semantics of Construction,
Destruction,andCopy

考虑下面这个 abstact base cles 声明
tlass Abstract_base (public:

chat *,mublei
你看出价么间题了没有？显然这个 can 被设计为一个抽象的 bas clas（其
中有purevial fancion，使得 Absacr_Aane 不可能拥有实体），组它易然需要
一个明确的构通函数以初始化其daamember_mamble，如果没有这个韧始化提

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p225_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 226

作，其 derived clas 的局那性对象_msmble 将无密决定初值，例期
cotcrete_derived(?:
90i4 f001I
2e 未教初始业
//...
你可能会争持说，也许 4bwer_har 的设t著试图让其每一个 drined class
提供_mamde 的初值，然面如果是这样，derived clas 的唯一要求就是
Albetrar_,Aanr 必须提供个骨有唯多数的 protcted coest
[1munbie( munble_valee )
一般图营，claes 的 dat member 应该被初始化，并且只在 conrctar 中或
是在 clas 的其它monber funciors 中指定物值，其它任何操作都将破坏封装性
质，使clan 的维护和修改更加图难。
当然你速可能争持说设计者的错误并不在于未提供一个expliciconutrucor，而
是他不应该在抽象的 hecas中声明dana menbes，这是比权强面有力的论点
（把itertace 和inplemeaiee 分离），组它井不是行遍天下售有理，因为将“被
共享的数据”抽取出来放在he ds中，毕竞是一种正当的设计。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p226_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 227

第5 章构绝、解构、携贝逐意学（5c

纯虚拟函数的存在（Presemce of a Pure Virteal Funetion)
C++题手靠常很综语地发现。一个人竞然可以定文和调用（inoke）一个
vital fasction;；不过它只能被静态地调用（ivokedsmatialy)）。不能经血虚拟机
制调用。例如，非可以合法地写下这段码
eked statica1ly)
Asstsaect,base:sterEace() 0
//通注：通注意.先前管产明Pure virteal eonst
// -..

/ gk : 鲁志调用 (static ismocatios)
//讲世，请往意，我们免结能都调用一个purevLrtus
fusct.lon
要不要这样做，全由cas设计者决定、唯一的例外就是pure vitul
rctor：cas 设计者一定得定文它。为什会因为每一个 derived clas
nctor会被编济器加以扩展，以费态调用的方式调用其“每一个vintal buc
las*以及“上一层 bae class°的 desructer，因此，只要缺乏任何一个 bane class
tomr的定文，就会导致链接失败。
操注：以 VCS 面言，如果我们授有定文 Abrer_bane cas 的 pure vi
女r，虽然可以通过编译，但整接时会出现以下错谈：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p227_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 228

探度指R C++ 对象根型 (Auide Zhe C++ Olect .Model
你可能会争持说，难适对一个 punevinal destrctor 的调用接查，不应该心
“编译器扩展 derived ches 的 destmctor” 时压持下亲吗？不1 claes 设计者可肥
已经真的定文了个 pure vintul drstuctoe（就像上一个例子中定文了
Ahrenacr_Aase:imerer) 那样) 这样的设计是以 C++ 适言的一个保证为润提
是承体系中每一个 cles etjee 的 destnucters 都会被调用，所以编译器不能够压
排达个调用推
你速可能争精设，难道编详器不应该有足够的数据“合成”（如果clas设
计者忘记定文或不知道要定文)一个 pure vital destrtor 的函数定又吗？不：编
译器的确投有足够知识，因为编泽器对一个可执行文件采取“分离编译模型”之
放，是的，开发环境可以提供一个设备，在链接时我出 pure virtual dcsiructor 不
存在的事实体，然后重新激活编译器，赋子一个特殊脂令（diroctive），以合成一
个必要的函数实体：但是我不知道目能是否有任何编译器这么缴
个比被好的替代方案就是，不要把virtal destnuctor 产明为 pure
虚拟规格的存在（Presenee of aVirtual Specification）
如果你决定把 Aberart_hne:Wumhe( 设计为一个 vital functiee, 那将是
一个槽糖的选择，因为其函数定文内容开不与类型有天，因面几乎不会被后题的
derivd clas 改写。此外，由于它的 ncn-vital 通数实体是一个 ialine 通数， 如
果需靠被调用的话，效率上的报应实在不轻。
然面，编译器难道不能够经由分析，知道该函数只有一个实体存在于clas层
次体系之中？果然如此的话，难道它不能够把调用操作转换为一个静志调用操作
(sttic invocaion）, 以允许该调用操作的 inlinc esparsion？ 如果 chas 是次体系
随续被加入新的 class.带有这个函数的斯实体，又当如何？是的，新的 claos 会
破坏优化！该函数现在必须被重新编译（减是严生第二个——就是多志—实
体，编译器将通过选程分析决定哪一个实体要被调用），不过，函数可以以二进
制形式存在于一个library 中，敬挖据出这样的相依性。有可能展要某种那式的
194

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p228_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 229

第5章构造、繁构、携是语意学（Sc

一般图言，把所有的成员函数都声明为vintul fusctin，然后再享编译器的优
化推价把非必要的vitsal invocatios去除，并不是好的设计观念，
虚拟规格中const的存在
决定一个 vital fantioe 是否需要co，似平是件续周的事情，但当你真
正面对一个 absonactbasc clam 时，却不容易做决定。做这件事情，意味着得我设
nabchas实体可能被无穷次数地使用，不把函数声明为comt，意球着该函数不能
够嵌得一个 referce 或consl pointer比较令人头大的是，声明一个函数
为cont，然后才发现实际上其 derived isamee 必便改某一个 danamcmber.我
不知道有没有一致的解决办张，我的想续很类单，不再用cmm就是了
重新考虑class 的声明
由前面的时论可知，重新定文4hawor_hw 如下，才是比校适当的一种设
sfr
ebise:virtual-Abstract_base())  // 谦性: 不再是 pute
istsatvLetua
高是virtcaltected:
absteact_bese( chat *pc * 0 1;onatructor  // 新理个等有唯参数的
har *_eenb1es

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p229_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 230

深度R C+* 3象硬型 (Jeile Ter C*+ Obyer Me

5.1“无继承”情况下的对象构造
考虑下面这个程序片段：
Peint global.:
Polint fosbar ()
Poist 1oeai
*beap - 1oca1:
[19)zeturs 1ocal.
(11)
L1,L.5,16表现出三种不同的对象产生方式：global 内存配置、locl内存配
置和 heap 内存配置， L7 把一个 claes otject 指定给另一个， L.1o 设定返用值。
L9 期明确地以 defee 运算护割股 hra otjedt:
一个rbjsen 的生命，是读 etject 的一个执行期属性，local otjet 的生命从
L5 的定义开然 调 L109 为止- glohrl otjet 济生命和整个程序的生命相网, hrcp
otject 的生命它款mn：运革符配置出来开始，到它被dnlete 运算行握级为止、
下面是/or 约第一次声可，可以写成 C 程序，C++ Sandard 说这是种
所润的 Pla OF Data 声明形式
tyf strsct
Point/float x, y, 17
如果我们以C++亲编评这段码，会发生什么事！现念上，编泽器会为Poir
声明个 ativial defialt cotsernster、个 trivial destrsctoe、 个 trisial cepy
mctor. 以及个 sivial copy asignmmt eperntor. 组实际上, 编译器会分析

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p230_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 231

第5 章相造、解病.携炎语意学（Se
这个声明。并为它能上 Plain O' Data 参标
当编译器遇到这样的定文：
(1)Poist globa1i
时。或念上 Psier 的 sivialcoestetoe 和 deitrscter 都会被产生并被调用.
csmuctoer 在程序起始 （tamup）处被通用面 dcitrncer 在程序的 ena) 处被调
用(eray 系由系统产生,其在 main( 结京之前),热面,事实上那些 wiviali menben
要不是没被定文，就是投被调用，程序的行为一如它在C中的表现一样。
，只有一个小小的例外，在C之中，giabef被视为一个“临时性的定文”
因为它没有明确的初始化操作。一个“糖时性的定文”可以在程序中发生多次
那些实州会被链接器价叠起来，只留下单独一个实体，被放在程序dss gTct
中一个“特别保智始未初始化之glohal ateces使用”的空间，由于历史的缘址
这换空间被排为 BSS。 这量 Bleck Started by Syrmbol 的细写, 是 IBM 704
mbler 的↑ pseade-op:
C++并不支持“临时性的定文”、这是因为clas构通行为的瞻含应用之故。
虽然大家会认这个语言可以判断个 cas ctjects 或是一个 Plain Or Data, 但射
平投有必要描得那公复杂，因此，ghbw在C+中被税为完全定义（它会图止
第二个成更多个定文).C 相C+*的一个差异算在于，BS5 daa sgmem在C++
中相对地不重要。C++的所有全局对象那被当作“初始化过的数据”末对持。
fobar 最数中的 L.5.有一个 Poxrohject local ，网样也是既没有被构造电
设有被解构，当然啦，Peinr ohtject iocwl如果授有先经过初始化，可能会成为一个
在的程序臭虫———万一第一次使用它就需要其智值的话（如L7），至于h中p
object 在 L6 的物始化操作：
(6)Peint *seap = nte PointJ
会被转换为对n 运算符（由Iibrary 提供）的调用。
Poist *beap * _nsee: siaeof( reist 1 1/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p231_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 232

探度探素 C++ 对象根型 (Juids TIv C++ ORer Model)

再一次容我强调，并改有dcfault coestructor 推行于 are运算符所传居的
Poir otject 身上, L7 对此 etject 有个赋留 (鼠值, asiga) 操作, 如果 lxca
普被适当地析始化过，一切就设有问题：
(7)*hsap = 1ocs1:s
事实上这一行会产生编洋警告如下：
vazning, 3ine 7 : 1ocal is sed befoe being instialiaed.
观念上，这样的指定操作全敏发 sivin oopy asigmemt operntor 进行携贝表
运操作。 然图实际上此 otject 是一个 Plaie OF Dams,所L赋便操作 (asigsmet)
将只是像C 那样的纯位移操作，L9 执行一个aeiee操作：
(9)delete beap:
会被转换为对 ddlre 运算荐（由 Ihary 提供）的调用；
_delete( beap 1
现念上，这样的操作会敏发Poow 的 wivil dsrwtor，但一如我们所见，
desstoe 要不是受有被产生就是没有被调用。最后，函数以持值（by vabac）的
方式将 local 当敏返居值传国，这在成念上会敏发aivial copy contnsctor，不过
实际上 nhm推作只是一个菌单的位携贝操作，因为对象是一个Plain Or' Dsc
抽象数据类型（Abstraet Data Type）
以下是 Poie 的第二次声明，在pic 独口之下多了 privwac 数据，提供完
整的封装性。包股有提供任何 vitsal fanctiens
class Poist (
_x( × 1, _3( y 1- _a( = 1 { )

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p232_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 233

第5 章构造、繁狗、携灵语意学（Scm
/ no copy conatsctot, copy opereto

这个经过时装的Poair clais，其大小并没有改变，还是三个连续的fol是
的，不论 privamte 。pulblic 存取层，或是 menher finctiee 的声明，都不会占用
期外的对象空间。
我们开设有为 Poiw 定文一个 cepy costrcter 或 copy openor. 因为款认
的位语意（deault bitwise su*:s.译注：看第2章，#51 页） C经足够。我
们他不需要提供一个dcstr:.tor，为程序数认的内存管理方法也C足够。
对于个 gl%al 买体:
Poist glrt-al:; // 3& zedst: Peist ( e,0, 0.0, 0.0 1:
现在有了 ceAsit cen成uctor 作用干其上。由于 globa被定文在全局范時中。其
初始化提作将延退到程序缴盾（startup）时才开始（6.1节对此有详细讨论）
如果要对clais 中的所有成员都设定常量初值，那么给子个oxlici
isiialization lit 金比较高效 (比起意文相网的 coestructer 的 inline epansioe 茂
言），甚至在 leel sape 中性是如此（不过在 lecal scpe 中这可能稍微有签不
够直观。你可以在5.4节看到一些讨论），举个例子。考虑下面这个程序片段：
vold munble()
Peint 1eeai1 = ( 1.9, 1.0, 1.0 )
Peint 1oca12:
loca12._x - 1.0
1oca12._y * 1.0

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p233_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 234

探度探素 C++ 对象模型 (eride Tle C++ Obrer Mnder)
1o6a12._1 = 1.0
facall 的析始化操作会比 local2 的高效这是因为当函通数的 activs
被放进程序准栈时，上述 inilintien is 中的常量就可以被放进iocaf！ 内存中
T
Explit itiaiains is 来三项肤,点:
1.只有当 class mombers 都是public时，武法才奏效
2.只能指定需量，因为它们在编泽时期就可以被评他求值（evluated）
3.由于编评器并没有自动施行之，所以初始化行为的失败可能性会比较高
好了，esplicit inializasion Tit 所带来的效事优点期够补偿其软件工程上的缺
点吗？一般面言，答塞是mo，然而在某费特推情况下又不一样，例如，或许你比
手工打造了一卷巨大的数据结构如调色盘（color paletr）。或是作正要把一堆常
量数据（比加以 Aia 或 Sotinag 软件产生出集的复杂儿何模型中的腔制点和
节点）横例始程序,那么explit initiaizatiee lit 的效率金比 inline ceswor 好
得多，特斯是对全局对象（gohl etjet）页言，
在编评器层面，会有一个优化机制用案识别 inine coesanucton，后者简单地
提供一个mmberbty-mnber 的常量指定操作，然后编译器会抽取出那查值，共
且对特它们就好像是 cxpliat ieitidlinmion lit 所供应的样。图不会把
累或为系列的 assignmem 指令。
于是, loxal Poimr otjet 的定 又

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p234_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 235

塞 5 章传建.繁构. 携贝语意学（Sem

现在键附加.上defaal Poitr consucsoeE)iniise cspan
/ LsLise expansson ot defauit constructot

L6 置出个 heap Poir otject:
6)Pesnt *besp * nev Pedst:
现在则被附加一个“对 dcfaak Pointf coestructor 的有象件调用操作”
/%
heap>oist :1Peint () /
然后才又被编译器进行 inline cspe操作。 至于把 Aeap 指针指向 Iocaf
objeet:
(7)*heep = 1ocal.
则保持者简单的位携贝操作，以传值方式特图Iacaletbject，情况也是一样
(10)retuzn 1ocal;
L.9 删除 hap 所指之对象:
(9)delete beap:
该操作并不会导致dismector被调用，因为我们并投有明确地提供一个
ctor函数实体。
我念上，我们的 Peir clams 有一个相关的 dcfhalt copy conancor. co
pemor 和 desinctor，然面它们都是无关费痒的 （riviol）。而且编评器实际上析
本设有产生它们。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p235_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 236

聚度素 C+* 对象根型 (urile Tlv C++ Oiecr Moder)
为继承微准备
我们的第三个Peior声明，将为“是承性质”以及某查操作的动态决汉
[dymamic reselutios）省准备。当前我们限制对：成员进行存取操作
clans Peint (
Posst< floet x = 0.0, [loeat y = 9.0 )
: _x[ × 1. _71 y 3 4 1

Virtal flost s(3)

再一次容我强调，我并设有定文一个cop00m
mnxtar，我们的所有members都以l数优来据存，因此在程序层面的默认语意之
下.行为良好。某费人可能会争是说，vitual functions 的导人应该总是附骨着
个virtal desmuctor的声明，但那么重在这个得子中对我们并无好处
vital fancioms 的 引| 人促使每个 Poie ctjpect 拥有个 vitual tek
pcidr.这个指针握供给我们 vinmual楼口的弹性，代价是：每一个otbjec 需要
额外的一个werd空网，有付么重大影响与？税应用情况而定！在3D模型应用
领城中，如果你要表现一个复杂的儿何那状，有看60个NURB表面，每个表
面有 512 个控制点,那么每个 otject 多负担 4 个 bytes 将导股大约 200,000 个
bytes的额外负据。这可能有意文，也可能设有意文。必须视它对多态
(polymarphism）设计所带来的实际效益的比例类定。只有在光成之后，你才能
评估要不要建免之
最了每个 chas objet 多负担个 vpe 之外， vintl fanction 的引人也引
发编译器3于我们的 Poircas 产生影胀作用:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p236_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 237

第5章构绝、解构、携贝酒意学（5oties of CoSan, Den, asd Copy)
■我们所定文的 costrwcter 被附加了一些码，以便将vper 初始化，这些
码必蛋被附加在任何 base clas constructors 的调用之后，组必须在任何
血使用者（程序员）供应的码之前，州如，下面就是可期的附加结果（以
Pow 为W) :
/ c++ 物码 AB影量
Posat11Peint (flsat x, float y )
(x),70y)

合度个cepy censtrectee 和个 copy amsigamenttor，西且其操
不再是 trivial (他 implit &estructer 易然是 trivial), 如是个 Poine
sbjet 被初始化或以一个 derived clas stject 赋值，那么以位为基础
（bitvise）的据作可能始tr带泉非盐设定。
// c++ 药
//oopyconstructor 的内部合成Inline Peint*
PoiatiPeiat( Poist *this, const Peiat srha 1
*P_"vtbi_Foints

eturs this/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p237_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 238

探度探素 C++ 对象模型 (emnidle 7he C++ Obyer Mo
编译器在优化状态下可能会把cbjen 的连续内容携买到另一个oltject
上，面不会实成一个糖确地“以成员为基础（mcmborwise.译注：请参考第2 章
49 页) * 的赋重操作。 C++ Stunderd 要求编译器尽量斑迟 nonmrivial members 的
实际合成票作，直到真正通到其使用场合为止
为了方领起见，我把fohar)再次列于此：
(2)Peist globa1;
Polst foobar ()
66E89686Point 1ocal;
*heap = 1oca1:
30
(11)
L1 的 globaf 机始化操作, L6 的 hrap 初始化描作以及 L9 的 Arap 最腺
操作。都还是和前早的Poinr 版本相间，热n L7 的 mcmberwise 赋留推作
*seap = 1oca1/
很有可能触发copy asignme eperer 的合度，及其调用操作的一个 inrinc
cspasion (内联扩展) ， 以 aho 取代 Anap 西以 rie 取代 local.
最戏剧性的净击发生在以传值方式特居Iocal的那一行（L10）。由于copy
stor 的出现，/6ohar)很有可能被转化为下面这样（2.3节普有过讲细的计
)1

Poiat foobar( Point s_result )
lscal .eist1iPeistI 0.9, 0.9 )3Peint 1oca1:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p238_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 239

宽 5 章构准- 新构.持贝退盘学 (5
/ hap 的部分与指相网
// copy cosatruetor B8用

// 10oal.Peint11-Posst(1)
etuEn

如果支持 samed retum valbc （NRV) 优化。这个函数会进一步被转化为

Poist focbar( Point s_resuit 1
_resslt.?eint:Feiat( 0.0, 0.0 1J
/ heap 的都分与提相间
returhu
一般面言，如果你的设计之中有许多函数都需要以特值方式（by vala）传网
个 local claes otjedt,如像如下形式的个其术运算
T operator*( eonat Ts, cosst 7s )

那么振供一个 cepy contnctar 就比腔合理——著至即使 dctash mmberwvise 语
意已经足够，它的出限会触发NRV优化。然图，就像我在前一个例子中所服成
的那样，NRV优化后将不再需要调用copycearnuctr，因为运算蜡果已经被直
接量于“将被传国的ctject”傅内了

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p239_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 240

聚度探素 C++ 对象根型 (.lide The C*+ Olircr
5.2继承体系下的对象构造
当我们定文一个 cbjct 如下;
T cb)ecti
时，实际上会发生什么事情呢！如果厂有一个comitractor（不论是由 user 提体
或是由编译器合成），它会被调用，这很明显，比较不明显的是，comsmnuctor 的
调用真正伊随了什么
Caestnctor可能内带大量的隐藏码，因为编译器会扩充每一个crstrcto
扩充程度视clas7的是承体系而定。一般面言编译器所做的扩充操作大约短
1. 记录在 memer inicaliatios list 中的 data mxmers 初始化操作会被放
进costrctor 的函数本身，并以members 的声明原序为原序
2. 如果有一个 member 并位有出现在 member iaitiliation lit 之中, 但它
有一个 defalt coestructer， 那公该 default constructor 必须被调用,
3. 在那之前七是 clas sbject 有 virtus: uble poiner(s), 它 (们) 多须被
设定初使。 指户道为的 virtsual tuble(r)
4. 在那之部, 层等上层 ase class cosstuctors 必9被调用, CL base class
的声账那序为序（与 mber initializatio lit 中的原序授关联） :
》如果 batc class 被列于 mcmber isitializatios lit 中，那么任何明确
哲定的步数都应该传递过去。
>如是base class 没有被列于member initialiSos lit 中，面它 有
就调用之。ructor (3成 defaslt m
>如果 base class 是多重继承下的第二或后继的 base class,，那么 chu
指针必须有所调整
5. 在那之案，所弃 virtal basr class constrnctors 必须被调用，兵左列右。
从量探到量线：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p240_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 241

第 5 章 构通、 解构、 携见还意学 (5md  setion, and Cop??
>如果 class 被列于 member initialization list 中,那么如果有任何明
确指定的参数，都应该传通过去，若设有列于 lit 之中，面 class 有
个defasult censtrsctor。忠反调用之。
>武外。 class 中的每个 virtual base clas sbabject 的编移量
（offset）必须在执行期可被存取。
>如景 clas ebject 是最底层 (most-derived） 的 class. 其 cornstructor
可能被调用：某些用以支持这个行为的机制必须被放进来
在这一节中。我要从“C++语言对 clas所保证的语意”这个角度来探计
scars 扩充的必要性，我再次议Poir 为例，并为它增加一个copy
Chor. 个 copy ogeneoe. 个 vinal desestor (如下 :
ciass Pesnt (
copy const.ructorcopy assign
operator
rirtua1 -Foist (1)2  // i8: 1virtualdestruetor

在我开始介组外一步步走过以Poir为根器的继承体系之前，我要者你很快
地看看 Linrcas 的声明和扩充结果。它曲_Agpie 和_end 两个点构成：
psbeist _begin, _end;PSBLSC1
draw():

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p241_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 242

深度报索 C++ 对象膜型 (Juidle Tie C++ Obyirct Model)
每一个eplicit constructor都会被扩充以调用其网个：
bons.如果我们定文 comtreter 如下:
LiseiiLiner cosst Point 4begin, const Point send_end( end 1- _begin( begin )
它会被编译器扩充并转换为：
Lisei:Liser Line *this

tuen thisr
曲于 Posne 声明了 个 cepy coestrncer.个 cepy epernter, 以及个
operntor 和 destekor 都特有实际功能 (nostrivial)
当程序员写下：
Line ar
时，inplkit Linr detucter 会被合成出案（如果Lne 握生自Peir，那公合成出
来的 dstructer 将会是 vital.然图由于 Linr 只是内带 Poinr ctbjects 商非理承
自 Poinr，所以被合成出来的 detncter 只是 soetrivil 面已） 。在其中，它的
mmbercas ctjets 的 destrncoes 会被调用（以其构造的相反展序）：
/ Cc++ 物码; 合武出来的 sire destnseter
5i3l1-56se: Lise *0%is 1
this=>_begin.Potint::-Poist (1/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p242_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 243

第5童构造.鲜构.携贝酒意学（5em  and Cupy 1
当然，如果Poor destnctor 是 irline 函数，那么每个通用摄作会在调用池
点被扩展开来，请注意，虽然Poier detructer 是vintul，但其调用操作（在
ining clas detrctor 之中)会被静态地决议出束 （reselved saticaly)
类制的道理，当一个程序员写下：
Line b = aJ
时, implicit Lise copy costructer 金被合成出来, 或为个 inline public
量后，当程序员写下：
a * b
B, implicit copy assigp
最近当我改写chrcr 对，我注意到在产生opy oper 的时候，并设有用
如下的条件句第名：
If  this ** stis 1zeters *this:
于是com会能多余的拆关操作。靠这样
*pi - *p2:
我发现并不是只有cdhom才如此，Borland 也缺少这项费选，而我怀疑大部
分编评器也都如此，在一个由编语器合成而来的copyopeor中，上选的重复操
作员然安全邮累费，因为并投有伴随任何的资源释放行动，在一个由程序员供应
的cepy openor 中忘记检查自我指眠（赋组）操作是否失数。是新手极易陷人的
一项错误，贺妇：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p243_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 244

度探索C++

ot opezatoi
String  g srhs 1(
str = sev chsr[ strles: shs.str 1 + 1 1/delete [l str/

有许多次我想为 che 加上一个警告消显，说“在一个copy opernor 之牛
面对自我携灵缺乏一个降选操作：但即有一个dinr opcraor 对应某个mme
操作”，这项意围并没有实现，但我仍然认为像这样的一个警告消息对程序员是
有帮款的。
虚拟继承（Virtual Inheritance）
考虑下面这个虚拟糖承（继承白我们的Peiur)：
las Pom36 : pabc wal oat
Poist3d( const Foiat3es rha )Point ( x
: Peiat.t rhs 1, _st zha._s 1 ( )
NvistMs opetatet=: corat Posnt3os 1 /Point3d(12
//tsal float ±() ( returs _a) )

F
mkor扩充成象”并设有用，这是因为vin
享性”之：
//nstreeter 扩充A容
210

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p244_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 245

this->ieint1Peist ( x, y 11
thia->_vptr_Poist3d_Pointhis->__vgtz_Point3d_vtb1_Foist3d,
_vthi_Poim3_Poistr

扩充内容有付么错误码
试着想据以下三种类派生情况：
class vertex3d :  : ( ..- 1=
class FVertex : public Vertexld ( ... 1J
Feriex B9 corsttctr 必领也调用 Psir 的 comstrncer 然面,当 Peintus 和
Femtes 同为 FertezxM 的 sebotjets 时, 它们对 Aow cotsuctor 的调用操作
定不可以发生，取面代之的是，作为一个最底是的 cas.Fertex3d 有责任特Pobr
初始化，面更往后（往下）的继承，则血Wwrten（不再是Verwxu）来负责完
成“被共享之Poie subotjoct”的构造.
传统的策略如果要支持“好，我在将vital hase chas 初始化啊，我在
不要，会导致ostctor中有更多的充内容，用以指示vini basccas
Bsctoens应不应该被调用，comtrctar的函数本身因而必原条件式地测试传进
来的参数，然后决定调用或不调用相关的vinulhase clas cosrder下面就是
Paint3d 邯 constructor扩充内容
// c++ 物病;
/ 套 virtual base class 况下的 cos

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p245_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 246

N度医家 C++对象楼型

tbi_Point3d_Point,
this->_t = rhs-_tr
teteas this:

在更探层的继承增况下，例如 Woriex3f，当调用 Poixrbd 和 Vertex 的
tuctor 时。总是会把_mor_deried 参数设为Mise，于是就压制了两个
toes 中对 Peinr coestnctor 的调用推作,

vertex3d:ertex.3(
_aost_derived 1= faise )his->Point::Pesnt( x, y 1r
/设定

/设定vptra
retern thisr
这样的策略得以保持语意的正确无误，辜个例子，当我们定文：
Peint3e origin/
时。Peint conitructor 可以正确地调用风 Peir vinaa b
我们定文

212

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p246_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 247

第5 章相查、邮码，携关语意学(
Vertex3d cri
时。 FertexM coestruter 正确地词用 Poier cesstructier- Pointa B Fertex 的S
cetrctos会做每一件该数的事情——对Paie 的调用操作糖外、如果这个11为
是正确的，那么什会是错误的呢？
我想许多人已经注惠别了呈种批态，在这种状态中。“vinal hasccass
constrscters 的被调用”有着明确的定又，只有当一个完整的 clas ctjoct 被定义
出来（例如origia）时，它才会被调用：如果cbjeat 只是某个完整otjei 的
sabotject，它就不会被调用。
以这一点为粗杆，我们可以产生更有效事的 comstrctors，黑卷新近的编计器
把每一个comctar分熨为二，一个针对完整的 cjet，另一个针对sbobct
“完整objer”版无条仟地调用 vital baur contrctons，设定所有的 vpos 等等。
“subebjer” 最期不调用 vintal has coemoes.也可能不设定 ves 等等，我
将在下一节计论对ve的设定，cosmcter的分提可带亲程序速度的提升，不拿
的是我并没有真正用过这一类编评器，所以没有测试数据可以提供给各位，然而
在 Fondation 原目中, Rob Mumay 完成了 cfomt 的 C ouaput 优化, 可省略掉
非必要的条件删试，并设定ve，开且他记录到一份额为可观的速度现升结果
vptr 初始化语意学（The Semanties of the vptr
Initialization)
当我们定又个 PVerter otjet 时, consructers的调用原序是
intiax, xy ),
Pvertex( s, y。  12
最键这个继承体系中的每一个 chaes都定文了一个 vintual fincti
函数负资情居cdas的大小，如果我们写：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p247_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 248

Pesst *pt = spr
那公这个调用操作：
pt->size(1
精传图 PYerter 的大小，百：

将传目 Poin3d 的大小
更进一步，我们假设这个他承体系中的部一个com内资个调用
作。靠这样。
Peint3d:iPeint3d( float x, Eloat y, float 1_x( x 1. _F I y 1- _=t = I

当我们定又 PYwter otject 时,前述的五个 contnsctars 金如何！ 每一次sor(
通用会被决试为PYerz:r)吗（华竞那是我们正在构造的东西）？或者每次
调用会被决议为”当前正在执行之costructor所对应之clas的norG通数实体
C++语言现则告诉我们，在 Pointifcontwmor 中调用的 sin0函数，必须
被决议为 Pain3d:rir 面不是 Pertezie(.更一般地,在个 das (本例为
PaintM) 的 coratrnctor (排 destructor)中。经典构通中的对象 (本例为 Perte
对象）来调用一个 vinal lanction, 其函数实体应该是在此cdlas （本例为Povinr3/)
中有作用的那个。由于各个comitructorn 的调用眼序之放，上述博况是必要的

214

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p248_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 249

第 5 章 腺造. 解构.携贝适盘导(Sc
Constrncters 的调用眼序是： 由根源死末增 （betom sp) 由内面外 （inside er)
当 base claes censtcter 执行时， derived 实体还没有被构造出来， 在 Pliertex
csoer 执行完毕之编,Pleriex 并不是一个完整的对象：Paint3d comstctor 执
行之后。有 Poar3ubotject 构通充毕
这意味着，鱼每一个PVnter bae clas constes 被调用时，编泽系统必须
保证有适当的sbe0函数实体被调用，怎样才能办到这一点呢？
如果调用换作级制必须在cotnchr（或destrncter）中直接流用，那么答案
十分明星：将每一个调用操作以静态方式决议之，千万不要用到虚损机制，如黑
然到如果 ribe) 之中又调用一个 vitsal funtion。会发生世么事情呢？ 这种
增况下，这个调用也必领决议为PolnM 的函数实体，面在其它情况下，这个调
用是线正的vitml，必质经自虚拟机制来决定其积向，住就是说，虚相机制本身
必须知道是否这个调用源自于一个comstuctor之中。
另一个我们可以采取的方量是，在constructer （成 destrutor)内设立一个郁
志，说“确，不，这次请以静态方式来决议”。然后我们就可以以标志值作为判
断供据，产生条件式的调用操你
这的确可行，虽然感觉起来有点不够优推和有致率，就算是一种“hack”行
为吧：我们基至可以写一个批注来为自己开股：
// yuck111 tix the Lasguage senastics1
这个解决方法感觉起来比校像是我们的第一个设计紧略失致后的一个对策。
面不是差底抽薪的办础，根本的解决之道是，在执行一个comrwdtor 时，必领限
组 vintsal fenctioss 较选名单,
想一里，什么显决定一个 clas 的 virtal functioes名单的天键！等塞是
vital table，Vintal tabit 如何被处理1 答案是通过 vpr， 以,，为了控制个
218

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p249_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 250

尿浆N董 C++ 对象膜型 (Auidr Ther C++ Olyeor AM
cas 中有所作用的函数。编译系统只要觉单地控制住 vptr 的初始化和设定握代
即可，当然，设定vpv是编详器的资任，任年程序员都不必操心此事
r初始化推待应该如何处理！本质而言，就其这得程vptr 在coms
之中“应填在何时被初始化”面定，我们有三种选择：
1.在任何提作之罪
khers 调用操作之后，组是在程序员供应的码或是
on list 中景判的members初始化操作”之期。
3.在每一价事情发生之后。
答塞是2.另具种选得我价公价班，如果你不相信，读在1波3策略下试
试看，策略 2 繁决了“在 claes 中聚制一组 vintal finctioas名单”的问题。 如
果每一个 coestwtor 都一直等持到其 hase clas costrcton 执行完基之后才设
定其对象的y，那么每次它都能够调用正确的vil tncin实体
令每一个 hae chan cotstratr 设定其对象的 vpe，便它指向相天的vital
uable之后，换逊中的对象就可L严格面正确轮充成“构造过程中所幻化出束的每
一个chas”对象包致是说，一个 Pmex 对象会先感成一个Pan 对象，
一个Pmte 对老、一个Wmmev 对象，一个 Yeriax3d 对象，热后才成为一个
PFemter 对象. 注与 个 base hes emtrwor 中, 对象可以与 comtrotars clas
的完整对象作记段，对于对象质言，“个体发生学“姐括了“系统发生学”.cosu1b
的执行差出源贯运下：
1. 在 derived class cosstructer 中, *册有 virtsal base classes” 及 “上-
层 basc clas 的 ceestreters 会被调用,
2.上述完成之后，对象的ptr(s)被初始化，指向相关的viral table(s)
3. 如果有member initialiatioe lit 的话，将在cestrueter 体内扩展开
泉，这必须查 vpr 被设定之后才进行，以免有一个vintal

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p250_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 251

第5 章购造、邮构.胃贝语意学（Scm
4.桑后。执行程序员所提供的弱。
例如，已知下面这个由程序员定文的 PYertex cm
Pertexiiertex( [lsat x, fleet y, floet t 1_nent ( 0 1 , Vertex3d( 3, y, ± 1,
Point( x, 7 1

cc *sise: * cc sise() cc endlr //
它楼可能被扩展为

Flertex::Plertex(Flertex*  rrertex* this,
_detsved
//条件式地现用vLrt  tructoe
fI_mostthis->ihesnt::Peist ( K, y 1)
/元条件地面用上
/格相关的vptr 智始化
_rtb1_Point_Pvertes/
//程序员所写的码
Point3d: :Point3d()Mertex:PPertex ) * // 师注; 原多为
1/R为笔误
<e ohi8->_ptr_rvertex[ 3 1-faddr) (this)
//传回被构造的对象eturn this;

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p251_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 252

深度根素 C++ 3对鲁很型 (Auiale Tie C*+ Ohircr MoeI)

这就完美地解决了我们所援的有关限制虚损机制的问题，但是，这真是
完美的解答吗！最设我们的 Pcaircomstructor 定又为：

我们的 Poin3f cestrudtoe 定义为:
t3d:Point3d(float
更进一步，最设我们的 Fenter 和 Fertex.Jd onstrcers 有类数的定义。你是
否能够看出我们的解决方法并不完美一一即使我们完美地解决了我们的问题！
下面是vpr必须被设定的周种情况：
1.当一个完整的对象被构造起案时，如果我们声明一个Paiu 对象，Poin
2. 当个 sbebjet costructor 调用了个 virtual function(不论是直接调
用或间接调用)时。
如果我们声明一个PVerter 对象，然后角于我们对其 haee claes coaituctr)
的最新定又，其vpp 将不再需要在每一个h da cctor 中被设定，解决
之道是把 cestuctor 分裂为一个完墨的 cbject 实体和一个 subotject 实体.生
subotect 实体中，vpe 的设定可以省略（如果可能的语）。
知速了这些之后，你应该能够密答下面的问题了吧。在clas的ceno
的 menber iialiationliat 中调用 chan 的一个虚获函数，安全吗？就实际
言，将该函数运行于其clans dms mxnber 的初地化行动中，总是安全的，这是
因为，正如我们所见，vper 保证能够在 memberisalintionlist 被扩展之能，由
编译器正确设定好，但是在语意上这可能是不安全的，因为函数本身可能还得你
糖末被设立初值的mmhers.所以我并不推荐这种做法。然列，风vpr 的整休

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p252_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 253

8 5 章 构速、 解构. 持贝道8学(5cm
角度来看，这是安全的。
何时需要供应参数给个 basc chas constuctoe？这种情况下在“clas 的
comtrncter 的 member ntiliasien lis 中” 调用该 claes 的虚拟通数。 仍然安全
吗1不！此时vpr 者不是尚末被设定好，就是被设定指向错误的clais.更进
步地，该函数所存取的任何 cans diamcnens一定还投有被相始化
5.3对象复制语意学（ObjectCopy Semanties）
当我们设个 cas, 并以-个 clas etjet 指定给另个 dhes otjet 时
我们有三种选择：
1.什么都不偿，因此得以实通数认行为。
2. 提供 个 exti espy acsigumcnt operatoer.
3. 明确地拒姚粒个 cs sxject 指定给另个 class object
如果要志净第 3 点, 不用- cthuan otjet 指定始另一个 chas ctject. 那
么只要将ct opμr 声明为prme，并且不提供其定文即可，把它
设为 private, 我们就不再尤许于任何地点（除了在 memberfunctioes以及此 claes
的 frimnds 之中）避行鼠值（asign）操作，不提供其函数定义，则一且某个 membet
funcioe 或 tind全图影响一份携贝，程序在链接时就会失败，一般认为这和题
接器的性质有关（也就是说并不属于语言本身的性质），所以不是很令人满意
在这一节，我要验证 capyasigmen opemer的语意，以及它们如何被模型
出案，再一次容我强调，我规用Painr claes 来帮票讨论：
// .. (&te virtual tuPesint (
fioat _3, _y

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p253_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 254

探度根董 C++ 3 象 级型 (Auide The C++ Otirct Model)
投有什么理由需要禁止持员一个Polvcbjoc.因此问题就变成了：默认行为
是否足够？如果我们要支持的只是一个提单的携关操作，那么默认行为不祖足够
西且有效事，我们投有理由再自己提供一个copynig
只有在款认行为所导致的语意不安全或不正确时，我们才第要设计一个0
met oapemoe (关于 memberwise copy 及其港在陷阶, [LIP91c| 有完整的
时论) 。 好，默认的 memherwise copy 行为对于我们的 Poie otject 不安全吗？
不至确吗！不，由于坐标都内骨数值，所以不会发生“别名化（aliaing）”或“内
存理露（memory leak）* ，如果我们自C提俱个 eopy aosigpSst eferalor. ti
序反据会执行得比控慢
如果我们不时Pcoer 试应个copy anigpew opMer，而光是依赖款以的
mxmberwise copy，编评器会产生出一个实体码？这个答案和 copy contrctor 的
增况一样：实际上不会！ 由于龙 chas 已经有了 hiowise copy 语意，所以 inplict
mapmor被视为毫无用处，也眼本不会被合成出来。
一个 cdaes 对于数认的 oopy axigmmt epentor，在以下情况不会表或出
bitsvise cpy B.8:
1. 当 class 内静一个 memher objes，面其 clas 有一个 cepy assgnmcr
perator pf
2. 当个 class 的 base clas 有个 copy asignmet eperator 时,
3.当一个 clas 声明了任何virtsal faneties（我们一定不能够携贝右源
elas cbjeet 的 per 地址， 因为它可能是一个 derived elas ebjec1)
4. 当 class 组承自一个 virtal basc class(不论武 base class 有没有 copy
peralar? Ff -
C++ Standaed 上误opy asigm opentoes 井不表示biwise copy
atis 是 nomtil. 实际上，只有 mostrivid intanoes 才会被合成出来。

220

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p254_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 255

于是， 对于我们的 Posinr cas. 这样的赋值 （asign) 操作;
Foist a, bv
& = bu
由 bitwise cepy 完成. 把 Peinr b 携员到 Peinef e, 其间并设有 copy asignmes
mtox被调用，成语意上或成效率上考虑，这都是我们所需要的，注意，我们
还是可规提供个 copy omructar， 为的是把 name mesam vabue (NRV) 优化打
cor的出现不应课让我们认为速一定要提供一个oopy us

现在我要导人一个cogpy asiopersor，用L说明该epernitor 在继承之
下的行为：

_x * p-_xr

protected://.-
lowt_#

确泽器就必渠仓
一个（因为前述的第二项和第器项理自），合成面得的东西可能看起来像这样

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p255_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 256

8NE C*+ 3fB 9S2 (Aneiaer Zhe C++ Otyeor Answ

/ 调用 base class 的的数实8
hs8->Nsnt1ieperatog*1 p 11

opy msigrment operer 有个8正交性情况 (aonorthogoral aspest., 重指不
够理想、不够严谨的增况），那就是它缺乏一个mxmber msigrmest lit ( 也.就t是
/ C+* 物码，以下性质务不支持。
: Psint ( p34 1, 1r p3d._= )
我们必须写度以下两种形式，才能调用base cas 的copy aisgmc
Poist::opecator*( p3d 1/
( *(Foint*)thLs 1 * p36)
缺少 coysigme lit.看来或许只是一件小事，血如果没有它，编译器
般面言就授有办盐压抑上一层 basc class 的 cogy opemons 被调用.例如，下面
是个 Yertex copy openor，其中Vortex 包是盘拟继承自 Pain
/ class vertex : virtsal publie Point
Vertes:1operatoe*: cortat Vertes tv )
return *this;
22

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p256_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 257

第5 章构造、解狗.携关语意学（Se

现在让我们从 Point3 8 1erwex 中报生出 Fertnid, 下国是 Ferex3
Lallse Veztex3.dsr*( const Vertex3d 6v
this>Posint.3d:operator* (  1
this->tertex::operator=(  3;

编译基如例能部在 PoiwtM R Fenex 的 cepy aas
Poer 的 copy assignmest operalars 呢1 编i评器不能够重传统的 ceestractoer 航
决方案（附加上额外的参数）。这是四为，相 coetucte 以及dcstrwcor不网的
是，“取copy asigamnt opcrnar 地址”的操作是合接的，因此，下面这个子
是毫无理的合法程序代码（虽然它也毫无最变地推题了我们看望把c
epennor 数得更灵巧的企图）
typedet Foiat3ds (Peset?dr:*pmfPeint3d) (
petfoiat3e pef = sPois13e1:eperater*g
然面我们无法支持它，我们伤然降要根据其孩特的继承体系，安抽任何可能
数目的参数给 oopyamnigrmcst opmator.这一点在我们支持由 clas otjects（内事
vinal base clases）所组成的数组的配量操作时，也被证明是很有问题的（请看 6.2
节的计论）
另一个方法是，编评器可量为 copy aigrmet openaor 产生分化通数 （spli
tioms), 以支持这个 cdass 成为 moe-derived daes 波成为中间的 baec clam. 如
rment openoor 被编津器产生的话，那么“split finction 繁决方案”可
温业定文明确，但如果它是被claen设计者所完成的，那就不能算是定文明确，例
如,一个人如何分化像下图这样的函数呢（特别是当 imei_ham) 为 vital 时)：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p257_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 258

NR家 C++ 对家S Lereke Thr C++ Oyeer Adoder
LnLine Wertex3ds
init_be9+s ( v 1/

事实上, copy asi在虚拟继承情况下行为不载，需要小心地设
比和说明，许多编译器甚至并不尝试取得正确的通意，它们在每一个中间（调师
用)的 copy asigr  ke,子是绝成 vintaa
llass copy iosgremel epenmar 的多个实体被调用，我知道 Cfeel， Edboat
Design Group 的前增处理器、 Borland C++ 4.5 以及 Symantc 最斯娠 C++ 编泽
器都这么做，我携想你的编语器生是如此，C++Shndard是怎么说这件事的呢
我们并授有规定那查代表vitusl busecas的subobjct 是否该被“隐喻
dare, Section 12.8)
如果使用一个以语言为基磁的警决办去，那公应该为copy asigpmm ope
提供一个附加的“mcmber copy lit、简单地况，任何解决方案如果是以程序接行
为基础，将导致较高的复余度和校大的错误能向，一般公认，这是语言的一个额点。
也是一个人应请小-心检验其程序代码的地方（当他度用vintual basc casaes 时）。
有种方肽可以保证 mont-derived claes 会引发(完成) vintaal base clas
stjet 的 ep 行为, 那就是在 drived das 的 cop asigpnt epnter 函数
实体的最后，明确调用那个opcrabar，像这样：
Inline Wertex3dsrertex3d1reperator*( eonst Vertex3d &v )
this->Point3ed:soperator-(  )

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p258_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 259

第 5 章 构造、解病.持是语盘学(Ser
stor=( v 11e class 1nvocat.1ons

这外不鼠够省略 subobjccts 的多重烤员，但部可以保证语意正确。另一个般
决方案要求把 vitual ssbobect 男贝别到一个分真的函数中。并根据 call peth 条
件化地调用它。
我建议尽可能不要允许一个vintal bhase chas的用贝操作，我基至提供一个
比校奇怪的建议：不要在任例 vitul bane clars 中声明数据。
5.4对象的功能（Objeet Eficieney）
在以下的效率测试中，对象构造和携关所胃的成本是以 Poictfcas 声明为
基准，从黄单形式逐渐列复杂形式，包括Plais OI' Da，抽象数据类型（Abtact
Daa Type.ADT)，单一继承、多重继承、虚拟继承，以下函数是测试的主角：
Poist3d 1ots_ef_copies (Peiat34 a, Point3d b )
Poist.36 sc + a/
DC -  7/ (2)
retsrs (C)
它内带因个mxmberwise初始化播作，包括两个参数，一个返图崔以及一个
局那时象pC.它电内带网个 mcmbcrvise 携贝操作,分期是标示为(1) 和(2) B
两行的 pC 和 b. mok) 通数如下:
mMls ()
Foist3ad yci

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p259_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 260

探度探素 C++ 对象模型 (/nnide 7her C++ Obyecr A

pC = lets_of_copiesi pA, pB 1r
retera 0/
在最初两个程序中，数据类型是一个it 和一个拥有public 数据的ca

对 p4 和 pB 的物始化操作是通过 esplictintilizsionlit 亲进行的

这两个操作表规出biwis copy语意，所以你应该会预期它们的执行有最好
的效率，结果如下

优化未优化
CC  5.05  6.39
5.84  7.22
CC的效率比较好，是因为NCC握环中多产生了六个额外的汇编语言指
令.这个额外负握并不会反映出任何特定的C++语意，这两个编译器的“中网C
摊出”大致是相等的
下一个期试，唯一的改变是数据的封装以及inlinc函数的使用，以及一个
mructr，周以相始化每一个 cbjen. clamn仍然展现出 hise copy 语
意，所以常识告诉我们，执行期的效率应该相网，事实上则是有一些距离：

22E

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p260_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 261

优化  来忧化
CC  5.18  6.52
6.00  7.33
我曾经想过，效率上的差算并不是因为lsu_dfcoker，到是四为msie0函
数中的 can etjecn 初始化操，所以费修改 strct 的机始化操作如下，并复制
clas cosstructor 的扩展邯分：
male( (Point3d pA/
pax + 1.725: pA-y * 0.475s ga.1 * 0.478;

/ 其相间
它们现在成为一种封装后的clas表成方式，我发现两次执行时间都增加了：

优化  未优化
CC  5.18  6.52
NOX  5.99  7.33
经由 coentsctor 的 insise esparsion,坐标成员的初始化操作带束以下两个指
令的汇确语言码：一个指令把常量值加载列缓存器中。另一个指令进行真正的储
存操作：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p261_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 262

聚度N素 C++ 3象税型 (Duide Tlv C*- Oecr Moder
20 pt3d A3 1.725, 0,975, 0.478 1:
ete5f4, 176c5ap9
坐标度员著经由 cosplict initiaisrsticn Iist 来做析始化操作，会得到单指
令，因为常量慎被预先加载好了：
7:float 1,7250000238418579e+00
etc+
在封蒙和末封装过的网种PowJ声明之间，另一个差异是关于下-行的证
Poist3d pC/
如果使用 ADT 表示基。 pC 会以其 dfaslt cocor 的 inlin epnsie 自
动进行初始化——甚至显然就此例面言，没有折始化电很安全，从星一个角度来
说，虽然这些差异实在很小，但它们扮演警告的角色，警告设“封装加上inint支
持，完全相当于C程序中的直接数据存取”，风另一个角度来说。这些差异共
不具有什公意文，因此也就股有理由放弃“封装”特性在联件工程上的利益，它
们是一些作得记在心中以备特殊情况下能够医上用肠的东西
下一个费试，我把Paint3的表现法切割为三个层次的单一继承

我没有使用任何 vital funtios- 由于 Paintf 仍然展现出 hinwise copy 的
语意，所以额外的单一理承不应该影响“nmmbrise 对象初始化或携关操作”的
成本。这可以由测试期果显示出来：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p262_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 263

第5 章 构港. 邮的, 携贝通意学 (Semantios ef Constrnitios, D

优化  来优化
CC  5.18  6.52
NCX  6.26  7.33
下面的多重继承。一般公认是比较高明的设计，会于其mcm
完成了任务（至少提供了我们一个期试列程）：

1 gubise Poiet14, peb1ie Pesiat2d t 1: / _1
由于 Peints cdas 仍然显观出 birwise copy 语意，所以服外的多重继承关联
不应该在memberwie 的对象初始化操作或携贝操作上增加成本，除了 CC 优化
情况之外（它的效率格微高一些），执行的结果的确验证了我的思比

优化  未优化
5.06  6.52
NCC  6.26  7.33
截至目靠的所有删试，所有服本的道异都是以“树始化三个lolbjcts”为
中心，而不是以“memberwise italiation 程cnpy的消耗”为中心、这疮操作的
完成都是一成不变的，因为到目前为止的所有那查陈述都支持“bitwi

225

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p263_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 264

深度探素 C++ 对象:模型 (/mide 7he C++ Ohyer Msd/)
意、然到一且导人虚拟继承暗意，情况就改变了。下面这样的单层虚拟继承
class Poist3d : publlc Poist2d ( -.. 1Peintid 1--.1r
不再允许 clas 拥有 bivise cpy 语意（第一层虚拟继承不允许之，第二层继承
则更加复奈) 。合成型的 inine cogy 0omsStrnuctor 8l copy ae
被产生出案，并被抓上用场。这导致效率成本上的一个重大增加：

优化  未优化
15.59  26.45
NCC  17.29  23.93
为了更实在地了解这些数据，让我因到先前的陈述：有着封装味道并加上
个 vintaal fanction 的 class 声明，国忆下，这种携况并不北许 biwise copy 语
意存在， 合成型的 inline oopy coscte 和 cepy asigment openmsor 是被产生
出束，并且被派上用场，效率成本的增加星不如预期，比起binwinecopy即也有大
40%-50%，如果这个通数是程序员供应的一个 m0n-ininc函数。或本仍然较

优化  未优化
8.34  9.94
7.67  13.05
23

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p264_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 265

第5章梅造，解构.得贝酒意学（5em

下面的测试是采用其它有者biwie copy 语意的表现方式，取代 ininec 合成
型 mbwise coy cestor 和 oopy aigm perar. 这一次反胰出来的是
对象构造和携贷的成本增加，原因是继承体系的复杂度增加了。

优化  水优化
单一继承
12.69  17.47
NCC  10.35  17.74
多重理承
CC  14.91  21.51
NCC  12.39  20.39
虚拟继承
cC  19.9)  29.73
NCC  19.31  26.80
5.5解构语意学（Semantics of Destruetion）
如果 clas 没有定文 detrncor.那么只有在 clas内带的 member cbjecr （或
是 claes 自C的 base chaes） 拥有 deststor 的情况下，编评器才会自动合成出一
个束，否则，demtrwtr会被程为不需要，包就不需被合成（当然更不需要被调用）。
例如，我们的Pow，默认情况下并投有被编洋器合成应一个de
至最然它排有一个 vinul fenction
Peist( feat x + 0.0, f1oat y * 0.9 1/teiatt eetst Peiatk 1:

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p265_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 266

M指家 C++ 3象 要型 (Auide Zhe C++ Oercr Mole

rirtsal flost ().

类似的道理，如果我们把两个Poi
elass Lise t
public:LibeI censt Poin

rotected:
oist _segin, _es6
Liner也不会提有一个被合成出来的 deitrs
当我们质 Psinr 国生出 Poinr3df （基至即便是一种虚拟振生关联）时，如果
我们没有声明一个 desttor，编评器就股有必要合成一个&estnter
不论 Peine 或 Pont3d，都不需要 detrecter， 为它们提供一个 denmuctor 反
到不将合效率，你应该距绝那种被我称为“对称兼略”的奇怪想拨：“你已经定
文了一个coauctor，所以以为提供一个dector生是天经地文的事”，事
实上，你应请因为“需要”面非“感觉”来提供detctr，更不要因为你不确定
是否需要一个 denmdor，于是就供它。
为了决定 clhes 是否需要一个程序是面的desrter （减是com
你想思一个clascbjct的生命在哪重结束（或开始）？需要什会操作才能保证对
象的完整？这是你写程序时比校需要了解的（或是你的cas使用者比较需要了解
的），这也是conrntor 和 desmter什么时候起作用的关键，单个例子，已知：

232

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p266_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 267

第5章抑绝. 册构. 携R染意学 (Semaetics of Coe

Peint pt/Po1Et3d
delete p
我们看到，p 和p在作为foo)函数的参数之前，都必须先初始化为某热全
标值，这时候需要一个comtructor，否则使用者必须明确地美供生标值，一般到
言，cas 的使用者设办盐检验一个 local变量式 hap 交量以知道它们是否被机
始化，把comsmructor想象为程序的一个额外负担是错误的，固为它们的工作有其
必要性，如果没有它们.独象化（ahanaction）的赞用就会有错误的能向。
当我们明晚地4w摊p时会如何呢！有任何程序上必须处理的吗？是否
要在delee之前这公缴
p=>x( 0 11 p=>y( 0 1J
不，当然不需要，没有任何理由说明在date一个对象之期先得将其内客请
除干净，你也不需要归还任何资源，在结束pt和p的生命之前，改有任有”chass
使用者展因”的程序操作是绝对必要的，因此。也就不一定需要一个 detruckr
然图请考虑我们的Verterclas，它维护了一个由紧邻的“顶点”所形成的链
表，并且当一个顶点的生命结束时，在精表上亲创移动以完成删除操作，如果这
（或其它语意）正是程序员所菁要的，那公这就是renrter deswtor 的工作。
当我1及 Poont3y 和 Vernex 报生出 Ferecx3s 时, 如果我们不供应一个
pl:2 F:tex3M destneoe, 那么我们还是希里 Fwses destwor 被调用, 以结束
一个 Fertex3d stjes.提此，编译器必须合成一个Fersex3f destctox，其唯一任
状是间用Yerex destrter，如果我们提供个 Vertex3f desrctor，编评器会扩
果它，使它调用Femter dsttoe（在我们所俱应的程序代码之后），一个由程序
员定文的 denaructor 被扩展的方式类组comtnuctens 被扩展的方式，但原序相反：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p267_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 268

RN素 C*+ 38类型 (AesAe Zhe C+ + Otyen M
1. 如果 ebjet 内带个 vptr, 邯会首先重设 (reset) 相关的 virtual tah
2. destructor 的通数本身现在被扶行。也就是说 vptr 会在程序是的码执行
前款董设 (reset)
3. 如果 clais 拥有 member class ohjects. 面后者提有 dest
们会以其产明展序的相反展序被调用
4, 如果有任何直接的 (上一层) nenvirtual base classes 拥有 destructor, E
们会以其声明原序的相反展序被调用。
5. 如果有位何 vietal base classes 得有 dastructer。 图当前讨论的这
class 是量尾确（mom-derived）的clas，那么它们会以其原来的构造署
修的相反顺序被调用。
绿注：这个顾序似手有点问题，请参考下页说明
就集 cestnwter 样, 目指对于 dotxctor的一种最佳实规策母就是维护
风l据实体：
plete ohjeet 实体,总最设定好 por(s), 并调用 virtaal base class
2. 个base class subotject 实体：除非在deitrector 函数中要用个
virtaal fuection, 否则它绝不会调用 virtual base class destructees 并设定
一个otjen 的生会结于其 deitrctor开始执行之时，曲于每一个 hai
class destnctor 都轮香被调用，所以 derived otjet 实际上变成了个完整的
stject.例如一个 Prertex 对象日还其内春空间之罪，会像次变成一个Versex3
对象、一个Femex 对象、个 PomMf 对象，最后成为一个Pow 对象，当我
们在 desttor 中调用 mmber fancions时，对象的舰安会国为 ve 的重新设定
（在每一个dmmwtor中，在程序员所供应的码执行之前）而受到影响，在程序
中集行 destdton 的真正语意得在据6章讲述。

234

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p268_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 269

谦注：上页的 desrctor 扩展形式组乎应为2,3, 1,4,5,才特合 cos
相反顺序，重新整理于下：
xtor的函数本身百光被执行。
2. 如是 clas 得有 memher class objects,  reters,那么它
们会以其声明顺序的相反顺序被调用
3.如果object 内带一个vptr，我在被重斯设定，指向适当之hase clas
的virtual table.

5.…网相早新述

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p269_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 270

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 271

第 6章执行期的意学

第6章

执行期语意学
(Runtime Semantics)

想象一下我们有下面这个简单的式子：
If （ yy =* (ox-getvaloe() ) -
其中xx 和yy 定文为

das Y 定文为：
claiser (

//

casX 定又为：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p271_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 272

深度探素 C++ 对象模型 (Juile The C++ Obyiner Moder)
class x
pet'value () )at:// t- canwersion 运者

真的换单，不是吗好，让我们看看本章一开始的那个表达式该如何处理
首先，让我们决定cquliy（等号）运算特所参考到的真正实体，在这个例
子中，它将被决议（nehes）为“被overloaded 的r成员实体”，下医是请式
子的第一次轿换：
/ reaolution of iatended operator
T 的eqaiy （等号）运算并需要一个类型为 Y 的参数，然面 gnale( 特
图的都是一个类型为x的otjet.者非有什么方法可以把一个Xotjet 转换为
一个Y cbjct，那么这个式子就算错：本领中X提供一个converion 运算符
把一个 X objet 转换为个 Y objet.它必领施行于 grPalr) 的返国值身上
下面是该式子的第二次转换
Lt1 yy-operator*=|
到目指为止所发生的一切都是编译器根据clas 的路含语意，对我们的程序代
码所做的“增胖”摄作，如果我们需要，我们也可以明确地写出那样的式子，不。
我并投有建议你那么缴，不过如果你那么缴，会使编语速皮稍敬快一些，
然程序的语意是正确的，但其教育性印尚不能说是正确的，接下来我们必
须产生一个怕时对象，用亲放置采数调用所传团的值：
产生一个临时的 classXotject,放量 gutFahe 的返用值：
X templ = xx,getTalae ())

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p272_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 273

第 6 章执竹B港8学 (Rantime Semmtis)

■产生一个临时的 class Fobjoct， 其置opernatoer Y0 的返界值：
Y teap2 = tenpl.opezator Y(12
■产生一个临时的inr chbjeel.政置eqality（等号）运算符的返网值：
int teap3 * yy-operator**( teep2 1/
最近，适当的 dlewctor 将被施行于每一个感时性的 claes cteo 身上，这毕
致我们的式子被转损为以下形式：

int tes>3 = sy-opezator= ( teap2 1;
sf ( tesp3 1-*

，似乎不少呢！这是C++的一件困难事情：不大容易从程序代码看出表
达式的复杂度，本章之中，我要骨领各位看看执行期所发生的一些转换，我会在
6.3节因到“临时性生成物”这个主避上。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p273_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 274

深度探素 C++ 3象模型 (Juude Tie C++ Ohjier ModeI)

6.1对象的构造和解构（Objeet Construetioa and Destruct
般图音。cructor 和 destructor 的安糖都如作所预期：
// c++ hB
point.
// poist Pist:-Posnt,() 数指害会键变错在过里
如果一个区段（谦注：以（1括起来的区城）或函数中有一个以上的离开点,
情况会精微混乱一些，Detnxcter 必须被放在每一个离开点（当时atjea 还存括）
之前，例如

vitch( iM( peint-xt) ) ) 1ca8e -1 :
/ munbieztor 在这里行会
or在这里行会

tor 在这里行卖

destrecter 在这里行物

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p274_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 275

第 6  执行能活童学 (Rartime Ern
在这个例子中,poor 的 deitructor 必须在 ich 指令医个出口的 nthrs
作前被生成出案。另外电很有可能在这个区段的结童养号（右大括号）之前被生
成出来—归使程序分析的些果发现地不会速行到那里
网样的道理，g借令也可能需要许多个 destor读用操你，例如下生的
程序片段：

goto found

// xx 的 destractor 在这显行动cache iter
retuan1J
Detnuctr 调用操作必领被放在最后两个 ntarm 指令之前，但是如不必杭质
在量初的rhrw之前，那当然是因为那时objoct 尚未被定文出来！
一般面言我们会把ctjecn尽可能放置在使用它的那个程序区股附近，这样效
可以节省不必要的对象产生操作和推发操作，以本例而言，加量我们在检查coch
之能就定文了Poieeex，那就不够理思.这个道理敏子非靠明显，但许多 Pascl
或C程序员使用C++的时候，分然习票把所有的etjeta放在函数或某个区段
的器始处。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p275_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 276

聚度探素 C+* 对象模型 ( Juile Te C++ Ohyiecr Modr

全局对象（Global Objects）
超果我们有以下程序片投：
Matrix identity)
mais(

returs 0:
C++保证，一定会在 mainc 函数中第一次用到 Miemiy 之前，把 Miemniy 构造
出果。而在 main) 通数继束之非把 Mdenniy 推级掉。像 idiemtiy 这样的质调
gloeal cbjet 如果有cmsrudter 和 destucter 的话。我们说它需要费态的初始化
推和内存释放操作。
C++ 程序中所有的 global objects 都被放置在程序的 data scgment 中，如果
明确些定给它一个值，ctjet 将以该值为初值，否则otyject所配置到的内存内和
为0.园此在下图达段码中：

v/ 和v2 那被配量于程序的 dmamgmem, v/ 优为 1024,v2 值为 0 （这和
C 略有不网，C外不自动设定初值）。在 C语言中一个gohl ctjen 只能够板
一个靠量表达式（可在编译时期求其值的那种）设定初值，当然，comtnuctor并
不是常量表达式，虽然clas ctject 在编语时期可以被放置于 data mgmnt 中并
且内容为0.但ceter一直要到程序票活（mamp）时才会实施，必须对一
个“放置于 prorm daa gmen 中的otjet 的初始表这式”微评估
（elae），这正是为付么一个 cbjecet 黄要静态相始化的原因。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p276_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 277

第 6 8执行R泌8学 (Rantin

当chom 还是唯一的 C++编译器。而且跨子台移植性比驶率的考滤更重费
的时候，有一个可样楼但成本颜高的参志暂始化（以及内存释放）方法，我把它
称为 munch.chom 的束端是，它的解决方需必领在每一个 UNIX 平台上—从
Cnty 到 VAX 到 Sun 则 UNIX PC (ATAT 曾短暂地推过这项产品)——都有
效.因此不论是相关的 linker 或otyet-file forml.都不能预先敏任何板设，由
于有这样的象制，下面这费munch策略就浮现岛来了：
1.为每一个需要静态析始化的档案产生一个_m函数，内审必要的
constructer 调用操作或 inrine expansies.例如前要所说的 idrnty 对象
金在 matris.c 中严生出下面的 _mi) 函数 （得注：我想 sti 就是 static
nitialization 的继写)：
LaitLalirationideetsty.Matzix:matrix()) / 伊注: 这就量 static
其中mapa_是文件名编码._eniy 表示文件中所定文的第一个tati
ctject (绿注：原书写的是 nostatic otjet。 不过我想应该是 msic ebjet)
在_m之后需加上这两个名称，可以为可执行文件提俱一个独一无二的云别
养号（Anby Konig 和 Bjunc 两人努力设计出这种编码体制，以解庆 Jim
Coplies质提出的名称冲突的图扰）
2.类似情况，在每一个需要费态的内存释放操作（statie deallocation）的文
特中，产生一个_mdt) 函数（译注：我想 sd 就是 static deallicatios
的笔写}，内带必要的 desctor 调用操作，或是其inlincspar
在我们的例子中会有一个_u)函数被产生出来，针对iniy 对象测
是Jeeir destrector
3.关供一组 natime ibrary“musch”函数：个_main( 通数（用以资
用可执行文件中的所有_mi0函数），以及一个caag函数（以类组方
式调用所有的_md函数）.

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p277_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 278

聚皮探浆 C++ 对象 模型 (.Anide 7he C+ + Obyeer Mxc

chont 在你的程序中安一个_in函数调用操作，作为muk通数的第
一个指争，这里的 ent) 和 C ibrary 的 enat0 不网,为了链接前者，在 choen 的
CC 命令中必须先指定C+ sandard lirry，一般置言这样算可以了。包对于不间
平台上的 cfel仍然可能夺在某些变化。例如 HP工作站上的编译系统最析算
拒地披握出mmchenat函数，所持理由如今我已经忘记了（谢天囊地），不过
我记得当初是十分令人地望的。这样的绝楚来自于使用者发线他或姓的sttic
e开设有被调用起来。
最后一个需要解换的网题是，如何收集一个程序中各个ctjectfles 的_mi
函数和_mdt)函数，还记得吗，它必须是可移植的—显然移桂性限制在UNIX
平台，花一点时间想想，你如何解决这个间题，这不算是个投术上的孩成，组在
当时，choet（也就代表C++）若要或功地流行于各平台，必须依客它
我们的解决方法是使用 nm 命令.nm 会期印出otjoct flic的香号表略项目
（gmbol ablemries），一个可执行文件系自文件产生出束，mm将施行于可
执行文件身上。其输出被导人（“piped imo°）munch程序中（我印象中是 Rob
Mumay 写出munch 程序，其它有质献的人士我就记不得了）.mamch 程序会“用

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p278_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 279

第6董我行期通意学 (Rantins
力咀响”号表略中的名称，推寻以_mi或_mul开头的名称（是的，但得安
删的是那查目标是以 _r 或_mf 开头，例如_mi,bu folef yow) ， 然后把
面数名称加到一个 mi) 通数和m) 函数的跳离表格（jump abie）中，接下来它
把这个表略写到一个小的pogam tot文件中，然后，顿案或许魂异，C命令
被重新票括，将这个内含表替的文件加以编语，然后整个可执行文件被重新
接。_mai) 和 emitg 于是在各个表势上走访一通，轮其调用每个项目 （代表
个通数地址）
这个做接可以解决间题，但似乎离正统的计算机科学遇了一些，在SymmV
1.0的时代，其募补版（patch）中有一个比较快速的变种办法（我记得是 Jer）
Schwazz 完成的）.鲁补版（pch）假设可执行文件是 SyemV COFF （Commo
Otjest FileFomn）格式，于是它检验可执行文特并我出那然“有着_ik sodes”
并“内带一个指针，指向_mG通数和_m)函数”的文件，将它们统统申题
在一起，接下案它把链表的眼激设为一个全局性的_Araf otjecn（定文于新的
pnch runtime Sibary 中) - 这个 palch ibrary 内形另种不间的 _msint0 函数和
ena) 函数,将以 _bad 为起始的链表走访一遍-最后,针对 Ssn.BSD 以及 ELF
的其它patch librwies 终于也由各个使用者团体损赠出案，用以和各式各样的
omn版本搭配。
当特定平台上的C++编译器开始出现时，更有效率的方法也就有可能随之
出现，因为各平台上有可能扩充链接器和目标文件格式（ctbject fle foma），以
求直接支持静态初始化和内存释放操作。何如，5ym V的Excuble and
Liaking Formt (ELF) 被步充以增加文持 ina 和 nai 两个 soctioms (译注)
这两个9ctaas内带对象所需要的信息，分别对应于静态初始化和释放操疗，编
译器特定（Inpieratioe-specife) 的 smartsp 通数（通常名为 cnoo)会完成平
台特定（pathm-peic）的支持（分期针对静态初始化和释放操作的支持）

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p279_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 280

深度授素 C++ 对象:要型 (uue Thle C++ Oer Modd)

谦注：所调 sexties 就是 16 位时代所说的 xgmest, 例如 code sxgmest
hta xgmet 等等。 System V 的 COFF 格式对不同目的的 sectioms (放置不间的
物息) 给予不同的命名, 例如 .Iext scction. idata scctios- edata sectiee, .arc
等，每一个mcion名称都以字符“”开头，不过有时候我们分然延用过去的习
贯用语。如 data sgmemt (本章稍早鲁出现过) 或 code sigm
cfhomt 2.0 版之前并不文持sonclas objcct 的静态折始化操作：继款是况，C
语言的限制仍然我留看，所以，像下面这样的例子。每一个初始化操作都被标示
为不合融
extern int 1:
/全都都要求静态相始化(static initiallzation
snt 2 - 1
doubie sal * (_sal( get_smpioyee( 1 1 1;
文持“eur:ias objecte 的静志初始化”，在某种程度上是文持virtual bua
clses 的一个别产录。 vitial base clases 却么会性进这个主题究: 哦, 以个
trived cle: 的 eistr; 或 rs:mmee 来存取 vintal base clas sbotjet,是一
ion，必须在位行期才能加议评估求值，例如，尽管下列程序片
股在编译香家可知：
/ ccstast espressios
/注:此处使用的 ca健承体系系第用第5 8 21 的接述
Poie 的 subotject 在每一个 derived clas 中的位置部可能会
变动，因此不提够在编弹时期设定下案，下医的初始化操作：
// pi化4 的个 virtual base class

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p280_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 281

第 6 章 执行期语意学 (Rantime Somatioa)
/ 基种形式的执行期研估 (rustime evalsatios)
Point *pt = p3d:
需要编译器提供内部扩克，以支持 clas ohjoct 的静态析始化（至少通道dia
stjets 的指针和 referesoes) . 例(如:

使用被静态初始化的ohbjects 有一些缺点，例如，如果cxceptiee handing
支持，那些ctject将不能够被放置于 by区股之内，这对于被静态调用的
contractars 可能是特别无法接受的，因为任何的 dhrew 操作将必然触发
eception handing IBery 默认的 aerminaw( 通数。另个缺点是为了控制“需费
将建模块做静态智始化”objocts的相依展序页性出来的复杂度（请参考
[5CHWARZ89],其中有对该问题的第次时论，以及如今被称为 Schwarz cees
的东西，如果你需要更广泛的讨论文献，请参考[CARROLL95])，我建议你根
本就不要用那些需要势态初始化的glbhiobjecta（虽然这项建议儿平音通地不为
C程序员所接受）
同部静态对象(Local Static Objects）
假设我们有以下程序片级：
coset Katrixs
dentityl)(
irn mat_identity
Local static clas otjet 保证了什公样的语意

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p281_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 282

聚度指索 C++ 对象提型 (Juiade The C*+ Oircr Moder)
mat_idextiy 的 cosstructer 必组只能施行一次。虽然上述通数可鼠会
用多次。
mar_idemniy 的&estrectee 必须只能施行一款,虽然上适函数可能会
调用多
编译器的董略之一就是，无条件池在程序起始（startup）时构造出对象末。
然面这会导致所有的 local saik clas etjets 都在程序起始时被初始化，即使它们]
所在的那个函数从不曾被调用过。因此，只在dexmiy)被调用时才把
mor_demnly 构通起束。是比较好的做法（现在的 C++ Standard 已经强制要求这
一点），我们应该忽公做呢
以下就是我在cfhom之中的微法。百先，我导人一个临时性对象以保护
msar_deniy的衍始化操作。第一次处理 idenniy) 时，这个格时对象被评估为
alse、于是contrwctor会被调用，然后猫时对象被改为e，这样就解决了构速
的间题，百在相反的那一期，desinuctor 也黄要有条件地施行于msr_idemily 身
上，但只有在mar_idntity C经被构造起素时才算数，要判断 mar_denniy 是
否被构造起来，很简单，如果那个编时对象为uc，就表示构造好了，固唯的是。
由于 cheet 产生 C 码, mer_dexniy 对通数图言分热是 lecal, 因此我投办张在
静态的内存静放通数（saicdealloaiee fanction）中存取它，项，伤脑多：解决
的方法有点说异，结构化语言建之唯烫不及：我取出 loxaletjeet 的地址。（当然
电，由于ohjet 是 staie，其地址在 dwnsra componmt 中将会被转换到程序
内用来波置 glohal otjet 的 dama mgmen 中) 下图是 chomt 的输出 （经过轻数
的修润)：
/款产生出来的卷时对象.作为成护之用
statie struct Matrix *o_ro = 0:

248

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p282_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 283

第6章执行路酒意学（Ra

// Lnt fc) ( ias a1;//int
//业后一行会变成：
// .., retsrs _1va1 + val.:
stat.ic strsct Matrix _lnat_idestity
//（b)设定强护对象.使它指向目标对象/(a) Rlf constructor:
ixFv ( *lnat_sdestity ),

是后, dcowtr 必须在 *与 Icl pogan Sle (生算是本病中的 sr_0.c) 有
关联的静态内有驿放的数（taic dallocioe function）“中被有多件地调用
Mar _sx×_atst_0_c_2 11
: 0 1

记住，提针的使用是choun所特有的：然面条件式解构则是所有编评器都
要的，在费下笔之时，C++标准委员会似平正蕴酸要改变destrnctar 对于 locl
clas ctjects 的语意，新的规则要求编译单位中的 saic loalcdhes etjiects必果被
推签—-以构造的相反次序爱，由于这些ctjcts是在需要时才被构造（例如
每一个含有msic local cles ctjets的通数第一次被进人时），所以编译时无出预
期其集合以及顺序，为了支持新的规则，可能需要对被产生出来的naicclan
objncts保持一个执行期链表

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p283_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 284

浆度探素 C++ 对量橙型 (uiule Tle C*+ Oincr Modef

对象数组（ArrayofObjeets）
假设我们有下列的数组定义：
Point knota[ 10 1
其要完成什么东西呢！如果Peir既没有定文个conutuctor 也授有定义一
个&trator，那么我们的工作不会比建立一个“内建（bub-n）美型所组成的数组”
更多，也就是识，我们只需配置足够的内存以储存10个连续的Poir元素。
然面Por 的确定文了个 delult detrucer,所以这个 dcstrutor 必领轮流
施行于每一个元素之上，一般图言这是经由一个成多个 nuntinme ibhrary 函数达
成，在 cfroet 中，我们使用一个被命名为 mec_nrw) 的通数，产生出以 dass
stjects 构造而成的数组，比较晚近的编译器。 包括 Barland. Microsen 和 Sun
则是是供调个通数,一个用来处理*没有 virtsal baee cas” 的 cas、另一个用
来处理“内甲 vintal hasc claes”的 clas. 后一个通数通意被称为 wr_mre)、函
数类型通需如下（当然在各平台上可能会有些许差异存在）：
void*
/数组起始地址

其中的 coetrwctor 和 desirwctor 参数是这个 clas 的 defast conu
deast desttor 的函数指针。参数orzy 有的者不是具名数组（本例为
boo）的地址，就是0.如果是0.那么数组特经由应用程序的nrv运算符
抗动态配置于 hap 中，Sun 把“血 clas chjecths 所组成的具名数组”和“动态
配置否来的数组。的处理操作分为两个 library 通数：_mecter_new2细
_ector_cos. 它们各自拥有一个 vintsal base clas 函数实体。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p284_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 285

第6 章执行胞语意学（Rat
参数rem_abv 表示数组中的元素数目（我将在 6.2节讨论nrw 和 drie
时涛国到这个主题案) 。 在 wr_erw() 中, constructar 施行于 elem_coer 个元素
之上， 对于支持 essetiomnhanding 的编泽器面言，destnwter 的提供是必要的， 下
亚是编评器可能针对我们的 10 个 Poxer 元家所数的 wc_n( 更用握作
sizeof( Posnt 1, 19, sPoimt::Peist, 0 1/
如是 Peie 也定文了一个 deitructr，当 bor 的生命结束时,该 destrcon
包必领施行于那 10个Poor 元累身上。我想你不会馆谢，这是经由一个类似的
w_arle0 (或是个 wer_weire()-如果 clases 拥有 vintal base clases 9)
话）的 rmustime library 函数完成（Su 对于“具名数星” 和“动态配置到来的数
组”，处理方式不同）的。其函数类型如下：

//数组中的元素数目ject 的大小
void (*destractor) ( void*, chaz )

有些编译器会另外增加一些参数，用以传通其它数望，以便能够有条件地导
引1 wer_deriem( 的逻黄。 在 wr_delrw( 中。 destucter 被施行于 eirm_cowsr 个
元累身上。
如果程序类提供一个成多个明显初但给一个由 clas objrcts 组成的数组，像
下面这样，会赔列：
Peist ksots[ 10 1 = [
Peiatt 1.0, 1.0, 0.5 1.-1.0
对于那些明显获得初值的元素，wr_ww0不再有必要，对于那些尚未被初始

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p285_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 286

深度指案 C++ 对象R型 (buile The C+ Obicr Moder)
化的元素，ec_ne0) 的随行方式就像面对“由 cas clemns 组成的数组，而计
数继授有exglict initilinatien Iit一样，因此上一个定文很可能被转换为
Peiat kseta[ 10 1/
// c++ h
7/明确给树始化前3个元素

// Bve智胎化后7个元款

Default Construetors和数组
如果作想要在程序中取岛一个comtrnctar的地址，这是不可以的，当然较。
这是编译器在支持 mc_Ae)时该做的事情，然面，经由一个指针来票活
comstrcter,将无放(不被尤许 )存取 defalt arumest vales. This has aways resuhed
in les that fnt clas hding ofhe initialitioe f an amy ofclass otjects (注: 8
拖款，我末能充分明了这句话的意思）
举个例子，在 choet 2.0 之票，声明一个由 class etjects 所组成的数组，意
味着这个 clhem 必领股有声明 coststoes 或一个 des血 centnctor (没有步数
那种）。一个conmctor不可以取一个或一个以上的累认参数值，这是违反直觉
的，会导致以下的大错，下面是在coet 1.0中对于复数通数库（complex library）
的声明，你能都看出其中的错误吗！
Le*0.0, doub1e=0.0)1

在当时的语言规则下，此复数函数库的使用者我办盐声明一个由compies
chas chjecs 组成的数组，显然我们在语育的一个陷胰上被终倒了。在1.1涨
25

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p286_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 287

第 6 章执行聚通意牛（Ratinme 3m
我们募改的是 claslihrary：然而在2.0 版，我们像改了语言本身。
再一次地，让我们花点时间想想。如何支持以下句子：
coaplex11eoeaplex(6ouble=00, double=0.0);
程序员写出：
complex c_arrayl t0 1/
时，而编泽器量终需要调用：

默认的参数如何能够对wr_(而言有用？
很明是，有多种可能的实现方续，chom所采用的方拨是产生一个内部的tuzb
tsctor，没有参数，在其函数内调用由程序员提供的contucor，并将dctaut
参数值明确地指定过去（由于comntructor的地址已被取得，所以它不能够成为一
个 infine)
atructor
cosp1ex( 0.9, 0.0 11
编译器自己又一次造反了一个明显的语言规则：clas如今支持了两个设有带
参数的consnctas，当然，只有当 clas ctjecta 数枢真正被产生出来时，sb实
体才会被产化以及被使用。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p287_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 288

尿度素 C++ 对象模型 (Inibe 7r C+*

6.2new和delete运算符
运算符nw的使用，看起来组手是个单一运算，像这样
sst *pi * Mev Lst( 5 1)
但事实上它是由以下两个步骤完成：
1.通过适当的nw 运算并函数实体，配置所需的内存：
//黄用函数库中的new运算符
ist *pi = _nte ( siseof( int ) 11
2.给配置得来的对象设立初值：
*pi = 5r
更进一步地，初始化握作应该在内存配置成确（经出nr运算持）后才执行
//nnw运第养的两个分离少要
// gires: ist *p1 * sev ist ( 5 )2

dler 运算特的情况类制，当程序员写下：
dtiete p51
时，如果p的值是0.C++语言会要求deee运算持不要有操作，因此编译
必须为此调用构造一层保护膜：
t ( p11= 0 )deletel pi 11
请注意并不会因此被自动清除为0.因此像这样的后继行为

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p288_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 289

第 6 章 执行南透意学 (Rusinw Se
//呢致：授有庭好的定文。但是合法
虽然没有良好的定文，但是可能（也可能不）被评估为真，这是因为对于
指向之内存的变更或再使用，可能（他可能不）会发生。
所指对象之生会会因dnleter 而结章，所以后糖任何对 xv 的参考操作就不
再保证有夜好的行为，并因此被视为是一种不野的程序风格，然页，把pi燃续
当做一个指针来用，仍然是可以的（报然其使用受到聚制），例如：
Lf I gi ** sentiael 1 ---*
在这里，使用指针和使用p所指之对象，其差别在于哪一个的生命已经
站束了。虽然请地址上的对象不再合法，但地址本身部仍然代表一个合法的和序
空间，因此p能够继续被使用，但只能在受限制的情况下，楼像一个wiv*指
针的情况，
以L constrector 果配置一个 class etjed, 增况类似- 例加
Pointld *erigin = aev Point36r
被特换为：

如果实现出excaptin handing，那么转换结果可能会更复杂费：
Lf t otig6s • _tev1 siseot( Peiat3d 1 1 1 (/ c++物
srigin = Peist3d::Poist3d( oeigin ):
zateh( ..- 1 (

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p289_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 290

探度探素 C++ 对象 模型 (Arike Zhe C++ OtyeerMoel

_deiete ( otigin 1/
/ 鲁8来的 excagtion 上throwu

在这里，如果以rv运算养配置objet，面其costutr丢出一个
eoetion.配量得来的内存就会被释放排。然后ocptio 再被丢出去（上传）
Deitmuctar 的应用根为类似。下面的式子：
delete origin;
会变成：

如果在asepn bing 的情况下,dtnwctor 应认被放在一个sy 区段
中，cogin hniler 会凭用 dlete 运第符，热后再一次张出速
一般比cey对于n运算养的实成操作都程直藏了当，包有两个晴巧之
处值得期药（请注宽，以下版本界未考虑expieehandling）：

if 1 sive == 0 )sise = 1;
roid *Last_a11oe)while ( 1(1ast_ailoe * nallee( siee ) )
It ( _new_handier 1

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p290_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 291

第 6 章 执行南逐意学（Rantin
( *_seu_hasdler 1 (12
zeturn 1ast_aLloc:
属然这样写是合法的：
new T[ 0 1
但语言要求每一次对mrw的调用都必须传图一个独一无二的指针，解决该问
题的传统方法是转间一个指针，指向一个数认为 1-bye的内存区换（这就是为什
么程序代码中的rar被设为1的源因），这个实现技术的另一个有趣之处是
它允许使用者摄供一个属于自己的_AovAanler0面数，这正是为什公每一次得
环都调用_n_Aander) 之放
mw运算特实际上总是以标准的Cmalec完成，虽然并改有成定一定再过
么重不可，相同的增况，dler 运算行电总是以标准的 CPe0完成：
operator dtlete( oi4 *gtr 1
if I ptr )ree( (chas*)ptr ):

针对数组的new语意
当我们述公写：
int *g_atray * nev ist[ 5 1:
时，wc_an)不会真正被调用，因为它的主要功能是把 default consclorM
chas ctjcts 所组成的数组的每一个元素身上，例是nw 运算行函数会被调用：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p291_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 292

深度报素 C++ 对象膜型 (luile The C++ Obyner Modei
相同的情况，如果我们可

mc_ww0 也不会被调用，为什么呢？因为nimple_agr 并没有定文一个
ostncter 或 destructer, 别以配置数组以及增除,Ag 数组的得作，只是单纯地
获得内存和释放内存面已。这些推作由nrw和dlete运算符求完成就增障有余了
然面如果 claes 定文有一个 defiault cotstrnudter，某些版本的 wer_n() 就会
被调用，配置并构造clas ctjecs 所组成的数组，例如这个算式：
Foist3d *p_atray = new Foist3d[ 10 1
通常会被编译为：
Foint3d *p_area)
sPoiat3ds : -Polst.34 1/Noiat3d:iFoist3d,
还记得吗，在个别的数组元素构通过程中，如果发生eepiee，erudtor
会被持通给wer_rwG.只有已经构造要当的元素才需要dncor的施行，因为
它们的内存已经被配置出来了wr_nr)有责任在cpion发生的时候把算
内存释放障。
在 C+2.0版之前，将数组的真正大小提供给程序的dinte运算符，是程序
提的费任。因此，如果我们原先写下：

那么我们款必禁对应地写下：
delete [ array_sise 1 p_arrayr

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p292_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 293

第 6 章执行期退意学 (Rani
在2.1服中，这个通言有了一些修改，程序员不再需要在de时指定教
元累的数目，因此我们现在可以这样写：
delete I 1 p_arragi
热到为了向下兼容，两种形式都可以接受，支持这种新形式的第一个编洋器
当然就是 cfro，内lonhan Shopioo完成任务，这项性术文持售先展要短道的
是指针所指的内存空间，然后是其中的元素数目，
导我数组维度给山ier运算的效率带来服大的影响，新认才导放这样的蛋
的：只有在中括号出现对，编译器才寻找数组的维度，否到它便最设只有单独一
个objocts 要被康除，如果程序员我有提供必须的中括号，像这样
delete p_sarray; / mB
那么就只有第一个元家会被系内，其它的元家仍然存在——虽然其相关的内存已
经被要求归还了
各家综讯器之天存在一个有动积是户，那就是元家数目如果被明显指定，是
否会被章用，1oln的原始版本中，单优先采用便用者（程序员）明确
指定的值，下面是他所写的程序代码的虚拟版本（psedo-versia），需带注释：
/ 首先检查是否量后一个抓配量的项目(_cacha_bey)/ 是国密费银 oeiete 即提Ⅲ

int elee_coast > _cach_oey =* peinter
：//取出元数

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p293_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 294

探度强R C++ 对量模型 (ide 7r C++ Obyer.AModn

然面儿乎晚近所有的C++编译器都不考虑程序员的明确指定（如果有的

foe 0l ( dclete [ 12 I pl.i )Ignored(anachrenise
为什么 Jonahan 优先采用程序员所指定的值，西新近的编评器部不这么做
呢！因为这个性质刚被华人的时候，没有任何程序代码会不“明确指定数组大
小”，时代演化到chom4.0的今天，我们已经继这个习很贴上“签伍”的标记，
并且产生一个类组的需告消息。
应该如何记录元素数目！一个明显的方法就是为uc_n)所传因的每一个
内存区块配置一个额外的 wond，然后把元素数日包藏在那个 woed 之中，通常这
种被也展的数殖称为所调的 coekie （小图饼)。然目,Jonatban 和 Sue 编译器
狭定维护个“联合数组(associarive amay)”,改置指针及大小,Sen 虑肥 deitructon
的地址维护于此数组之中，请看 [CLAM93],
c0okie策略有一个普通引起优患的话题，那就是如果一个坏指针应该被交给
drle_wc)，取出来的cookie 自然是不合继的，一个不合独的元素数目和一个环
的起始地址，会导放 destnuctor以非预期的次数被患行于一段非预期的区城，然汽
在“取合数组”的我策之下，坏指针的可能绪果就只是取出错误的元累数目面己
在原始编译器中，有两个主要函数用来储存和取出所请的cockic

/ eles_oount is the ceunt: it mty be 0

/ 头表格中京出（并去障） arrtay_,bty

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p294_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 295

第 6 章执/i期通意学 (Rastime Semantics)
/ 若不是特用 e1em_coant.。 批是特例 -1
esters ist _teaore_old_attay1 PFr array_key 1)
下面是 dboet 中的 ver_nreg 原始内容经过糖润蹈的一份显现。并是加我注
PT _vee_nevc Pr ptr_array, int elenint size, PV eonstruct )

/ T arayt enst 1
Jf sev ( ptI_array 1 7( 10 1/
Lnt a1ioc = 0; // 我要& vec_sev 中&量码
nt erray_sz = eien_count * siaei

ng Z 下,
f I ptr_arEay == 0 )
/把数组元家数目放到cache中
ing 之下冉垂出 exoeptior
Lf ( a11oc )
return 0;delete ptr_arrayr

char* 116* (char*)ptz_atray
//W是个代贵一个函针
元素上（由e3em指包）你用

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p295_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 296

深度探素 C+* 对象模型 (/ude he C++ O%yer Model)
(*fp) (vesd*)e1en) )
//前进到下一个元素len += si2e.

ec_dim)的操作差不多，例其行为并不总是C++程序员所预期或黄
的，例如，已如下面两个ces声明
elass Point (
Pesnt 11=
virtoa1 -NeiMt (3)

-Foint3M(1r

如果我们配置个数组，内量 10 个 PoonrJd objects，我们会预期 Paier 和
Peint3f 的 coestructor 被调用各 10 次。每次作用于数组中的一个元家：
//完全不是个好主意

面当我们 adlee “由 pbr 所指向的 10 个Point3d 元素”时，会发生什公事
情呢！很明是，我们需要虚拟机制的帮助，以获得预期的Poinr dent
Point3f destnadtor 各10 次的呼唤（每一次作用于数组中的个元素）;

/ Rt Peint::-Point 银调用
delete [1 ptr/

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p296_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 297

第 6 章执行南语意学(Rarsine Semantics)
施行于数组上的 4eitructr，如我们所见，是根据交始 wec_4rlenr) 函数之“被
删除的指针类型的 detrscter”—本例中正是Pvoir detrwtor，这很明显并非我
们所看望，此外，每一个元素的大小也一并被传递过去，这就是ner_drlesr如
何选代走过每一个数组元素的方式，本例中被待通过去的是Poor claescbjsc 的
大小面不是Point3f das stet 的大小，整个运行过程非常不率地失败了。不只
是因为执行路错误的 destreter，面儿自风第一个元素之后，该desnwitor 即被施
行于不正确的内存区块中（谭注：因为元素的大小不对）
程序员应该怎样做才好！最好就是通免以一个 hae clas 指针指向--个
derived cdaes otjcts 所组成的数组—如是 deried das stjot 比其 base 大的
适（译注：通常如此》，如果你真的一定得这样子写程序，解决之通在于程序员
层善，商非语言层面：

delete pr
基本上，程序是必源选代武过整个数组，把der运算特实摊于每一个元素
身上，以此方式，调用操作将是 virtal.因此，Posint 和 Povie 的 dentnsctar 报
会施行于数组中的每一个 atjects 身上
Placement Operator new的语意
有一个预先定文好的重载的（ovorioaded）rv 运算行，称为plao
n.它需要第二个参数，类型为void，调用方式妇下：
Doint2v *ptv = nev( area 1 Point2v:
其中arm 指向内春中的一个区族，用以放置新产生出果的 Poinr2w otjet. 这
个顶先定文好的placeme opemorrw的实观方挫简直是出子意料的平风，它只
要将“获得的指针（情注：上例的as）”账指的地址传国即可

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p297_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 298

深度拟素 C+* 对象根型 (Juid TIv C+ Oiecr Ader)
vese*itox nev( siie_t, void* p)
tuen pi
如果它的作用只是传目其第二个参数，那么它有什么价值呢？也就是说，为
什么不简单地这么写算了（这不就是实际所发生的操作吗）：
Poist2v *ptv • ( Peiat2v* I aresa/
班，事实上这只是所发生的覆作的一半而己，另外一半无出曲程序员产生出
来，患想这些问题：
1.付么是使placemem nrr operntor 层够有效运行的另一半部扩充（而且是
oreas 的明确指定操作（explicit asignmet)“所没有提供的）？
2. 价公是arem 指针的真正类型1 该类型增示了什么？
Placemmt.new operntor 所扩充的另半边是将 Painsi2 conitructar 自动实排
于 anms 所指的地址上:
( Peint2v* 1 azenar
这正是侵 placement openboxr nrw 或力如武强大的原因，这一垂码决定

然面都有一个轻微的不良行为，你看得出来吗？下面是个有问题的程序片段
Peint2v *g2v - se: arena 1 Point2vs// ... 6o it ..*
p2v = nev ( arena ) Point2v;//.-.rnov sasipelate a sev sbject ..*

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p298_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 299

第 6章执行期语盘学 (Ruetinw Semantios)

如果 placeme eperaer 在原已存在的—个 cbject 上构通新的 object，质试
现有的objct 有一个destrtor，这个 deststor 并不会被调用，调用法
destrctar 的方结之一是将那个指针diene 排。不过在此例中如果你像下面这样
做。绝对是个错误：
/以下并不是实离 6estrwctee 的正确方技lete pzw.
p2v = sev ( aresa 1 Peint2v:
是的，dite运算将会发生作用，这的确是我们所期持的，但是它也会释放
由p2所指的内存，这都不是我们所看塑的，因为下一个指令就要用到p>
了.因此，我们应请明确地调用deitcdor 并保督储存空网，以便再便用
p2v * stv ( erena 1 Point2v;
剩下的唯一间题是一个设计上的问题：在我们的前子中对 placomcn opcra
的第一次调用，会将斯ctjecn 构造于原已存在的ctjet之上？还是会构造于全
新地址上7也就是说。如果我们这样写：
Feist2v *gov + tev I aresa 1 feist2v;
我们如何知道arns所指的这块区城是否需要先解构？这个间题在语言限
医上弄没有解答，一个合理的习货是令执行uw的这一确也要负责执行
cler 的费任。
另一个间题关系到ave 所表成的真正指针类型，C++Standaed说它必须指
向相间类型的clas，要不就是一映“新解”内存，足够容纳该类型的otbjnct，注
anded C+ 以个 plont openbx arlw 娇正了过个是误， 它金对 chjoet 实是
tor.但不果度内存，图以就不必再直禁离川4mco了

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p299_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 300

聚度指董 C++ 3象&型 (luside Tle C++ Ohircr Moder)
意， deived clas 握明显并不在被支持之列，对于一个 deried clas,， 或是其它没
有关联的类型，其行为虽然养非不合法，却也未经定义。
“新鲜”的储存空间可以这样配置页来：
char *arena = sev char[ siseof( Point2v ) 1;
相同类型的otject 则可以这样获得：
Peisnt2v *arena = sev Peint2x:
不论哪一种情况，新的Point2的储存空间的确是覆盖了rm的位置，而
此行为已在良好控制之下。然图，一般面言，placcmemnt arw opernaer 弄不支持多
态（pomorpbibm）.被交给nrw 的脂针，应镇适当地指向一疾预先配置好的内
存。 如果 derived claes I比其 base class 大, 例如
PoLn:2v *p2v = nev ( arena ) Feist3v/
Pointw 的 constructer 将会导效重的破坏,
Placmaem nw penor 被引人C++ 2.0 时，最酶涩隐增的问题就是下图这个
由 loraan Shpiro 指出的问题：
striet sase : ks  rirtual void f()a 1
0 ar) (
b,-3ase ().b-(117  // Base::f() 世氨用
b.f () :sev ( 4b 1 Derive6i/那个t(1被翼用
由于上述两个 clases 有相网的大小、放把 derined otject 放在为 bae clas
面配置的内存中是安全的，然面，要支持这一点，或许必质胶弃对于“经由objct
志调用所有vintal functies（比题b/)）“通意都会有的优化处理，结果.

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p300_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 301

第6 章执行南语意学（Bs
memt new operaler 的这种使用方式在 Standard C++ 中末能获得支持 （调看
C++ Standand 3.8 节)。于是上述程序的行为没有明确定文。我们不能够新钉截铁
地说哪一个9函数实体会被调用，尽管大那分使用者可能以为调用的是
Derinnd:;9。包大部分编详器调用的排是Base:0)
6.3临时性对象（Temporary Objects）
如果我们有一个函数，形式如下：
T egeretse* ( coest T%, cosst T& 1J
以及网个 Totjects, a 和 , 那么:
+ bi
可能会导致一个物时性对象，以放置的回的对象，是否会导致一个临时性对象
规编译器的进取性（agesivees）以及上述操作发生时的程序上下关系(prngmu
0mtest)面定。例如下离这个片段：

编译器会产生一个临时性对象，放置a+的结果，然后再使用7的co
contructor，把该缩时性对象当量c的智始留，然商比较更可能的特要是直接以
携贝构造的方式，将a+的值放到c中（2.3节对于加法运算的转换管有讨
论），于是就不需要赖时性对象，以及对其comtrctor 和 dnmtrcor 的调用了
此外，提opernor0 的定义而定，mamed rtum vale (NRV）优化（请看 2.3
节）也可能实施起案，这将导致直接在上述：对象中求表达式结案，避免执行
copy cestnuster 如其名对:象 (samed tjet) 的 destrnxcter,
三种方式所获得的c对象，结果都一样，其同的差异在于初始化的成本，
个编译器可能给我们任何保证吗！严格地议没有，C++Sandaed允许编译器对于

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p301_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 302

临时性对象的产生有完全的自由度：
在某些环境下，由p9cmso产生楼时性对象是有必要的，亦或是比校方
的。 这样的糖时性对象由编译器象定义,(C+* Sandard, 12.2 节)
理论上。C++Stadad尤许编泽器厂商有完全的自曲度，担实际上，由于市
场的宽争，几平保证任何表达式（cnspremion）如果有这种形式：
T c * a + br
西其中的加出运算特被定文为：
T eperater*( eonst T%. cosat 7% 1J
T T11eperate+ ( eonst Ts 1:
那么实度时根本不产生一个临时性对象。
然到请你注意，意文相当的aosig
c = a * bv
不能够忽略临时性对象，相反，它会导致下面的结果
// *+
T teap///  teap * a + b;

标示为（1）的那一行，未构通的略时对象被赋值给opemor0.这意思是要
不是“表达式的结果被copycomcted至临时对象中”，就是“以临时对象取
代 NRV”。在后者中，原本要通行于 NRV 的comrctar，现在将施行于该身
时对象。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p302_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 303

第6章执行期适意学(Ru
不管塞一种增况，直接传递c（上列赋值操作的目标对象）列运算养函数中
是有问避的、由于运其符函数并不为其外加非数测用一个destucter（它期菱一块
“新解的”内存），所以必须在此调用之前先调用desrtar，然面，“转换”语
意将被用来将下面的 asigmm 操作：
c = a + br // e.operator*( a + b ):
取代为其copy asigprmert 运算的隐含调用操作，以及一系列的 destructer 和
/物
e.f:7( a + b 1:
popy constructar. Sertreiio: 以及 copy assigement operater 都可以出使用者9
应，所以不能够保区上达斯个操作会导致相同的语意，因此，以一连审的
trnatioe cop“omntreto 来取代 asigamm，一般面言是不安全的，页且
会产生着所或象，所以这年的初脂化单师：
c * a + bj
总是比下面的操作更有效率地被编译器特损：
c = a + br
第三种运算形式是，没有出现目标对象：
a + Br // so tarpet
这时候有必要产生一个临时对象。以放置运算后的结果，虽然看起来有点怪
异，但这种增况实际上在子表这式（uboprsioes)中十分普通，例如，如果我
们这样写：
String a( * helio* ), t( *wseld* ), s( ** )
那么不论

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p303_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 304

深度握素 C++ 对象模型 (Lbu
steisg ot + u/
ptintE( *hs`n*, s + t 1:
部会导数产生一个猫时对象，与s+r相关联
最后一个表达式带来一些秘教式的论题，那就是“编时对象的生命期”，这
个论题颜值得尿人探过，在StandndC++之前，特时对象的生命（电就是说它的
dcstctar 何时实施）并授有明确指定，西是由编弹厂商自行决定，换句话说，上
述的prio0并不保证安全。提为它的正确性与+：何时被握级有关
（本例的一个可能性是，Sring clas 定文了一个 comvemsion 运算特如下
Btting1ieperator eonst char* () ( retern _stzr )
其中 _e 是个 privae member adesing sergr, 在 Sring otjet 狗造时
置。在其 dstructer 中被释放)
因此，如果轴时对象在调用 pri之前就被解构了，经由covatio运算行
交给它的地址就是不合盐的，真正的结果视底那的dle运算得在释放內存时的造
最性面定，某些编译器可能会肥这块内存标示为tc，不以任何方式改变其内容。
在这换内存被其它地方直称主权之前，只要它逐授有被deted排，它就可以被款
用，虽然对于放件工程面言这不是以作为模花，但律这样在内存被释放之后又再被
使用，并非早见，事实上malloc的许多编评都会提俱一个持提的润用操作
nal1oc(0))
它正是用来保证上述行为的
例如，下面是对于试算式的一个可能的 pre-Sundard 转换，虽然在 p
Sadard语育定文中是合续的，但排可能造成重大文唯：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p304_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 305

第 6章执行期通8学 (Rant
的合去特费
Sttisg teap1 = operatoe+( s, t ))
//喝款：合理包最有文考意.大过轻事
/达时能萍末

另一种（比较被喜欢的）转换方式是在调用 pring)之后实施Sring
rncor.在C++ Standard 之下，这正是该表达式的必须转换方式，标准成格上
这公误：
非时性对象的被推签，应该是对完整表达式（hl-spmsin）求值过程中的
最后一个步键，请完整表达式通成临时对象的产生（Sectiee12.2）
世么是一个完整表达式（fnll-cxpresion）}非正式地说，它是被操括的表达
式中最务握的那个。下面这个式子：
ests 5 s0e-expres84ont
7 eb5A + obg8 1 foo( oeyA, oej8 1
一其有五个子算式（nesesos），内带在一个“9：完整表达式”中，任何一
个子表达式所产生的任何一个临时对象，都应请在完整表达式被求值完或后，才
可以象表。
当临时性对象是根据程序的执行期语意有条件地被产生出案时，临时性对象
的生命规则就显得有杰复杂了，举个例子，像这耕的达式：
if ( s + t 11 s + v )
其中的w*v子算式只有在s+1被评估为mibe时，才会开始被评估，与第二个
2T1

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p305_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 306

聚度探隶 C++ 对象 模型] (burile The C++ Obyer Modr/)
子算式有关的临时性对象必须被握质，但是，很明显地，不可以被无条件地服资。
也就是说，我们希望只有在物时性对象被产生出案的增况下才去握质它。
在讨论临时对象的生命航则之前，标准编译器将糖时对象的构造和解构附者
于第二个子算式的评估程序中，例如，对于以下的clas 声明：

X()3

Peist al/
以及对于 das.X 的两个 cbjcts 的条件测试

XYYT
if ( oxfee(1 11 yy-fee() )
return 0J
cfhomt 对于 moin)产生出以下的持换结是（C经过轻微的够润和注释)
Lst sais (oid)

Lat _0_ressltJ
/ ame anged a
ct_1xrv( <_1y 11

272

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p306_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 307

第6 章执行期语意学(ReK
//被产生出果的编时性对象
struet x_02
int_0__03;
.每一究度一个删还负的激达式。
teape1-Ki:-X()Jsrator int():

0_03
t_1xv( _0_01, 2 1), _0_03 )_01 >31)
1I((
_dt_1xIv( ≤_0_02, 2 11, _0_03 1)
6,t * 3
t,ixv( _1xx, 2 1)
retarm _0_resuit/

把额时性对象的dismuctor放在每一个子算式的求值过程中，可以免最“努
力追廊第二个子算式是否真的著要被评估”，然期在C++Sundard 的输时对象生
命规则中，这样的量略不再被允许，临时性对象在完整表达式尚求评估完全之前
不得护疲，也就是说，某些形式的条件测试现在必须被安独进来，以决定是否
带握费行第二算式有关的临时对象
输时性对象的生命脱则有两个例外，第一个例外发生在表达式被用来初始保
个 objeet 时。 例如

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p307_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 308

深指董 C++ 3象很型 (Juide Tle C++ Oirer Moder
boel verboses

其中 prtgName 和 progersioe 都是 Sering otjects. 这时娱会生出个要时3]
象，放置加法运算特的运算组果

临时对象必须根据对werhosr的测试结果有条件地解构，在临时对象的生命
规则之下，它应该在完整的“？：表达式”结束评估之后尽快被推签，然而，如果
pragkiamrFersion 的初始化需要调用个 oogy conitrscort
proglaneVeraion-5tring: :5trsng( teap 1:// C+物
那么赖时性对象的解构（在“？：完整表达式”之后）当然算不是我们所期望的
C++ Standand 要求说:
几含有表达式执行继果的临时性对象，应该存留则altjocn的初始化操作
完成为止。

基至即使每一个人都整守C+Stndard中的临时对象生命规则，程序员还是有
可能让一个临时对象在他们的控制中被接级。其间的主要差异在于这时候的行为有
明确的定文，例如，在新的临时对象生命成则中，下套这个初始化操作保证失败：
//喝欧：不是个好主意
cosst char *progsaprogliane + proglersion,Tersion =
其中 progNer 和 regermion 都是 Sring otjocts.产生出来的程序代码看来
像这样

274

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p308_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 309

第 6 京执行期治意学 (Rantine s
/ C++ paesdo Code
teap.ttriag11-txisg(1)
此别 pogNameFension 指向米定文的 ha 内存
临时性对象的生命现则的第二个例外是“当一个额时性对象被一个refer
绑定”时，例妇：

产生出这样的程序代药：
teap,3tzing:8trisg: * * 1/
const String sspace = tesp;
很明显，如果临时性对象成在被推级，那个refernremoe 也款差不多没付用
了。所以规期上议：
如果个核时性对象被病定于一个refersce，对象将线留，直到被初始化之
refemce 的生命结束，或直到精时对象的生命范略（scope）结束——视感一种情
况光则达而定。
临时性对象的迷思（神话、传说）
有一种说法是，由于当需的C++编泽器会严生临时性对象，导致程序的执
行比较没有效率，固此在工程界或科学界，C++只能成为FORTRAN以外可作
的第二选择，更有人认为，这种效事上的不影足以施盖C++在“抽象化”上的
质献(例如 [BUDGE92] 相反的论点则请参考 [NACK94]) , 发表于 7hr Aoarmal
efC Lenguag 上的 [BUDGE94] 对此有过份有是的研究

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p309_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 310

深度N董 C++ 31 象89 (Juiale The C+* Olircr Model)
在 FORTRAN-77 和 C++ 的一场比较之中, Kemt Budgc 和其助手分别以用
种语言写了一个复数测试程序（在FORTRAN中复数是内建类型，在C+中它
是一个具体类，有同个menbes.一个代表实数，一个代表虚数.StandaedC++已
经把复数类技在标准链接库中）。C+程序实泥inline 运其符如下：

谱注意。 被传通的 clas etjets (内电有 coestnxters 和 destnc
alae 西非 hy eferemee 的方式传通，像这样：
friend cosplex operator+( const conplexs, esnst ceepless
一般页言共争是种好的 C+程序风易，最了“opy by vaue posibly ngt
clas etjecs这个主题之外，每个至式参数的周部实体都被 copy contructed相
estnuted，并且可能导致临时对象的遇生。在这个删试务程中。作者声称把正式
参数转换为comt nelereece 并不会明显改变效率，这只是因为部一个函数是

试程序看起来这样

tcr  ist 1 - 9y ↓ < Ri i++ 1{4] = b[↓]+c[1; - b[1]*e[1]
其中对于左数的加法、减法、乘法和 asignment 运算符，都是 inlinc 函数
C++药产生出五个额时对象：
1. 一个额时对象，用来放置 b(]+c[间-
一个糖时对象，用来放置b可]*c[i1.
3.一个临时对象，用来放置上述两个临时对象的相减结果
4.两个临时对象。分别用来放置上述第一个临时对象和第二个临时对象

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p310_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 311

第 6 章执行期通盘9(Rantinc
为的是完成第三个临时对象
测试结果发度，FORTRAN-77的码果然快达两任。他们的第一个假设是把资
任归特于感时对象，为了输证，借们以手工方式肥 chom中介输出码中的所有
时时象一一消除，一加膜期，效率增加了两情，儿手和FORTRAN-77 相当
测试并设有就此静下案，Bedp和其助手以另一个方法案实验，这一次他们
采用反聚合（diagpreged）手拨，生就是说，他们把那输时对象拆开为一对
对的略时性 dosMe交量，结果发现，这种微法对于效率的提升，和先前“法染
所有临时对象”一样，于是他们写下这样的心得：
我们所测试的编译系统很明显批够调能内建（build-in）类型的局部变量，组
对于cdhaes类型的局部变量就行不通.这是 C++ back-eeds （图不是 C++ fom
emd) 的腺剂,这组平是很音通的情况，国为在 Sun CC, GNU g*+以L及 HIP CC 等
编济器中都会发生。 [BUDGE94]
这期文章分析了被产生出来的ascmby 码，并表示教率障低的原因是曲于程
序中春在大量的堆栈存取操作（以读写个到的dasmembes），经过反聚合
（diagrngaed）。并将个别的memhm 放到缓存器中，就能够达到儿手两倍的
效事，这使他们写下这样的结论：
加上适度的努力，反聚合（disgrgmioe）大有可为，但是一般的C++编i
器并设有把它视为一个重要的优化关键
这份研宽对于良好的优化提供了一个其有说服力的证明。目前存在有一些优
化工具，的确把获时对象的一费成分放进了缓存器中，当编译器厂商把地们的然
点从语音特性的支持（与Standed C++比较）转移到实现技术的优劣上时，如反
聚合（disagmgaiee）这般的优化操作就会更普通了。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p311_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 312

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 313

第 T章R在年家&2FX编 (On the Cep ef ie Otypct R

第7章

站在对象模型的尖端
On the Cusp of the Object Model

译注：本意大量式用两个原文词汇： instntite(动间) 和 instaniaion (名词),
你不容易在一账的字兵上董式这两个试。牛津计算机字典上对于 inutaniaiee 的
解释是：

在本意中应采用第一个解释，有费时候我会把intantiaios译为“实成”或
“具成”“其体实现出一个实体（instance）“的意思，
这一章我要讨论三个著名的C++语言扩充性质，它们都会影响C++对象.它
价分别是 tomplae, eoeptioe handing (EH) I nmime tpe demtiflation (RTT1)
BH（以及 RTT可想象成是EHI的一个翻作用） 对于这本韦所谈到的其它语言
性质而言，其是一个特例，国为我度有机会真王实现它，我的村论是以[CHASE94]
[ILAJOIE94a]。 [LAJ0E94b]。 [UENKOV92] BL及 (SUN94a]为基确的

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p313_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 314

深度指案 C++ 对象 根型 (Aeile The C++ Olyeert Modlel
7.1 Template
C++ 程序设it的风格及习果, 自从 1991 年的 cfront 3.0 引人, templates 2
后就尿探地改变了 。 原本 tomplae 被视为是对 coeainer clases 如 Lies 和
Arys 的一项支持，但度在它已经成为通用程序设计（也就是 Stundand Templut
Lirary,SIL）的基础，它也被用于属性湿合（加内存配置策略，[BOOCH93])
或互斥 (mstal exclasion) 机制 （使用于线程网步化控制) 的参数化技术之中
将在编评时期图非执行期被评估（rwd），因或带来重大的效事提分（通参
考[VELD95])
然到，如果我说wnglaoo是最令程序员有控胶感的一个主题，恐怡生是真的。
错误消息可能远在离真王间避相胞十万八千里处就产生了，编译时间提高了不
少、面程序员开始极增害怡费改一个内有多重相蒙类系的且文件，特别是如果
他正在和“奥失”奇成的时候，程序大小像气球一样地那聚出是意有的事，抗有
进者，tmplae 的所有行为都递越了一般程序人员的理解能力。那些人但求能够
完成手上的工作就好，如果上述问题持续存在，他们可能会认为templde 是一个
障碍而不是一个帮助，你根容易看到一个被命名为mplae专家的人，被一群项
目成员图团围住，要求解决问题并将产生点来的 ungiac 优化
这一节的集点放在 uenhae 的语意上源，我们确付论 tomplaos 在编译系扰
中“何时”、“为什么”以及“如何”发挥其功能，下面是有天tempae 的三个
主要计论方向：
1. tcmplatc 的声明。 基本上患说就是当伟严明一个 template class- template
class menber fsnctioe 等等时，全发生什么事情。
2. 如何*具成 (instamtiates) * 出 class objeet 以及 isline sonmenber, 以
er temglate fenctiees，这费是“都一个编评单位都会据有一份实
体的东西

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p314_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 315

第 7 章始在群象模型的实明 (On  Cwg ef tc Otjct Modcl
3. 如舟 *具现 (irstastiates) *出 sosmember 以及 member tomp
以及static tcmplane class mcmbers.这径都是*每一个可执行文件中只需
一份实体”的东西，这出就是一般图言 tcmplate 所带来的间题
我使用“具成”（insaniaion）这个字服来表示“行程（prces）将真正的
类型和表达式期定到 templae 相关形式参数（foml purnandters）上头”的操作
举个例子, 下周是一个 tcmplat fusctioe
teaplate (class Type)yPe
alst esat Tyge st1, cosat yge st2 1 4 *** 1
用庆如下：
nint 1.0, 2.0 17
于是行程就把 T)p 鼻定为 double 并产生 mn) 的一个程序文字实体（并
道当施以“mangling”手术。始它一个独一无二的名称），其中u 和2的类
型都是 double-
Template 的“具现”行为（Template Instantiation)
考思下面的 templane Poinr cles
tenplate cclass figa>

x - 0.0, 1yPe Y = 0.0, WSB8 ± = 0.0)

reidoperator deiete(

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p315_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 316

深度指案 C++ 3象根型 (Auidr The C++ Oly)

>+freeList:
T8 _x, _7, _21
首先，当编洋器看到 wmpac chas 声明时，它会做出付么反应？ 在实际程序
中，什么反应也投有！也氧是说，上述的 daic daa membens 并不可用.nsted cmum
或其mumc
显然 etem Shaher 的真正类型在所有的 Paiv istastiaioes 中都一样。 其
mmcntern也是，做它们啊一个都只能够通过 tmplate/sinrclas的某个实体采
存取或操作，图此费们可以这样写

组不能这样写：

即使两种类型独象地来说是一样的（而且，最理想情况下。我们希竖这个
cmm只有一个实体被产生出案，如果不是这样，我们可能会想要把这个cmum 抽
出到个 noememlame haoe clais 中,以避免多份携员)。
间胖的道理，FeeLar 和 chaskSov 对程序而言也还不可用，我们不能够写

我们必须明确地脂定类型，才能使用户eeLinr

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p316_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 317

第 7 章 在对象模型的尖确 (On De Catp of de Ohject Model)
像上要这样使用 sttic mcmber，会使其一份实体列Por
tion在程序中产生关联，如果我们写：

就会出现第二个 PeeLur 实体。与g Poier class 的 double insteiaias 产生关容
如果我们定义一个指针，指向特定的实体，律这样：
Point< float > *ptr • 0/
这一次，程序中什么患股有发生，为伊么呢？因为一个指向 clas ckjeci 的指计，
本身并不是一个 claus otject，编评着不落要知道与该 cas 有关的任例 menbes
的数提成otject 布局数据，所以亮“Paiw的一个fen实体”其观就设有必
要，在C++Suandard完成之前，“声明一个指针指向某个mplate clas这件事
增并未被强制定义，编评器可以自行决定要或不要将tmpiae”具现”出来，m
就是这么缴的（这使得某查程序员大感固套）！如今C++Standard已经繁止编讯
卷达样敏。
如果不是 poiner 而是 nefenence,又如何？ 根设：
cosst Point< flost > stef = 0

会租扩展为：

为什么呢？因为ndkrnce 并不是无物（no otect）的代名词，0被视为整数
必须被转损为以下类型的一个对象：
Peint< float >

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p317_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 318

指R C*+ 3 8 8型 (Juidle Tile C+* Obyircr Moder)
如果没有转换的可能，这个定义就是错误的，会在编课时被执出来
所以，一个chas ctject的定又，不论是由编评器蜡中地质（像精早程序代罚
中出现过的inmponary），或是出程序美像下图这样明确地缴：
cosst Peint< float > origis
部会导败 tcmplate cles 的“其现”、也就是说，flot instartition 的其正对象布
局金被产生岛束。风展先前的 templee 声明，我们看到 Paie 有三个 noestaric
mmbers. 每个的类型都是 Type. 7ype 显在被病定为 flom， 所以 origpin 的配
置空间必须足够容纳三个 flen 成员，
然面，mmberfunctions（至少对于那些术被使用过的）不应该被“实体”化
只有在menberfenctioas被使用的时候，C++ Standaed 才要求它们被“其成”出
来，当前的编译器并不糖确遵循这项要求，之所以由使用者来主导“具现”
m）或则，有周个主要原因：
1.空间和时间效率的考虑，如果 class 中有 100 个 member fancties,组
你的程序只错对黑个类型硬用其中两个，针对另一个类型使用其中五
个，那么将其它193个函数都“具度”将会花费大量的时间和空间。
2.尚未实现的机能，并不是一个tmplate 具现出来的所有类型算一定能够
完整支持一组member fusctieas 所需要的所有运算符，如果只“具现”
造成编添时期错误的类型（types）
举个例子，origin 的定又需要调用Por 的 deauk costructer 和dstc
那么只有这调个通数需要被“具现”，类组的道理，当程序员写：
Peinte float > *p * sev Peistc float >j
时, 只有 (I) Peir emplte 的 fot 实例. (2)arw 运算荐, (3) defalt corstnctior
需要被“其现”化，有趣的是，虽然nrw 运算特是这个clas 的一个 inplicitly
staic member,以至于它不能够直提处理其任何一个 nostatic mcmbers,但它还是

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p318_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 319

第 1 童在3)8模型的实明 (Oe hr Cetp of the Otjet Model
值赖真正的 template 参数类型, 国为它的第一参数 ror_r 代表 clas 的大小
这些函数在付么时候“具现”出末呢？当额流行两种策略：
■在编译时候，那么函数辨“具成”于oripn和p春在的那个文件中
■在链接时候，那么编泽器会被一查辅助工具重新激地。emplate 函数实
体可期被放在这个文件中，别的文件中，或一个分离的储存位置上。
植后的小节将对通数的“具现”化作更评雨的讨论。
[CARGIL95} 曾经提列有硬的一点, 在“imt 和 Ilong 一致” (或“dosbt 和
longdeabe一致”）的结构之中，两个类型具现原作：

应该产生一个还是两个实体呢？目薪我知道的所有编译器都产生两个实体（可能
有两组完整的momber fanctioms)，C+Suandard 并末对此有什么强制规定。
Template的错误报告(Error Reporting within a Template)
考建下图的 templaic 声明
(3
pub1ic5:
_t ( t )
1f ( tt 1= t)
12)
13)
这个 Mamble template class 的声明内含一些成露骨文港抗的

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p319_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 320

深探素 C++ 对象根型 (Luile The C++ Obyier Modef)
1.L4:使用 s字脊是不对的，这项错误有两方面，第一、S 并不是一个
可以合法用于标识特的半格：第二，cas 声明中只允许有pabic
etected. private 三个鲁标（ labels）， $ 的出现使 publics 不成为
public。 第一点是语汇（ lexical)）上的错误，第二点则是道甸/解析
(systactic/parsing) 上的 错误-
2.L5：r被初始化为整数常量1024，或许可以，也或待不可以。视7 的
真实类型面定，一般则言，只有template 的各个实体才诊断得出米
3.L6：_井不是哪一个member 的名称，才是，这种错误一般会在“类
型检验”这个阶胶被我出来，是的，每一个名称必须那定于一个定文身
上、要不算会产生储误
4.L8：运算特可能已定文好，但也可能还设有，核7的真正类型面定
和第2点一样。只有templte的各个实体才诊断得出来
5.L9：我们意外地键入ea两次。这个错误会在编译时期的解析（garsing）
阶段被发观，C++语言中一个合出的句子不允许一个标识符紧跟在另
个标识符之后。
6.1.13：我们忘记以一个分号作为clas 声明的组束，这项错误也会在编评
时期的语句分析（parsing）阶段键发现
在一个rms产明中，这六个点露骨又潜抗的错误会被编洋器执
出亲，但 tmcaker claer与不间，举个例子，所有与类型有关的检验，如果奉多到
ple参整，业镇照忍到真Z的其我操作（intaatios)发生，才得为之。
也就是设，L.5组 L8的潜在懂误（上述2,4 两项）会在每个具现操作
taion）发生财被检查出来并记录之。其结果将因不间的实际类型而不间
于是，如果：
feebiec iat > Bl/
则 L.5 和 1.8 是正确的, 面如量:
Mesble< int* > pnLI

制

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p320_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 321

第 1 章 始在对象根型的炎期 (Os e Cetp ofte Otject Modl
那公L8正确图L5错误，四为体不能够将一个整数需量（除了0）指定给一
指针，医对这样的声明
class Sna11Ist

由于其运算养并末定文，所以下面的句子，
Mublec Saal1tet > sai:
会造成L8 错误，页 L.5正确，当然，下面的例子：
Muble< tal.tst* > p4ai:
又造成1.8王确图1.5错误。
那么，什么样的错误会在编译器处理temnpaie声明时被标示出来？这里有一
部分和menhae 的处理策略有关，chont 对 templae 的始理是完全解析（perse）
但不做类型检验：只有在每一个具现操作（intanion）发生时才做类型检验。
所以在一个 parsing 量略之下,所有语汇（Ining）错误和解析（paning）错误都
会在处理mplace严明的过程中管标示出来
酒汇分析器（lxial amamer）会在 L4 摊提到一个不合核的字养，解析器
（paer）会这祥标示它：
pulLiet: // eaoght
表示这是一个不合增的卷标（iabei)）。解析器（pser）不会吧“对一个末命名的
mmber 偿出参考缴作”视为错误：
_t( t 1 // not caught

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p321_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 322

探度探浆 C+* 对象提型 (/emide 7he C+ Otwor Med/)
旭它会振出 L9“er出现两次”以及 L13“缺少个分号”这两种错误
在一个十分鲁通的替代策略中 （例期 [BALL92a) 中所记录) , Iemplatc 的产
明被收集成为一系列的“lesicl tokem°，面 parning 操查超迟，直列真正有周
现操作（instantitios) 发生时才开始, 每当看到一个 instantiation 发生, 这组 okem
其被想往punor，然后调用类型检验等等，面对先前出现的那个emplaec 声明，
“lesial ukeming”会提出什么错误吗！ 事实上很少。只有 L4预使用的不合法
字开会被指出，其余的 templde声明都被解析为合拨的tokms并被收集起来
目前的编泽器，面对一个tempate 声明，在它被一组实际参数具现之前，只
能施行以有限的错误检查，lomplinc 中那些与语肽无关的错误，程序员可能认为
十分明是，编泽器部让它通过了，只有在特定实体被定文之后，才会发出胞想。
这是目前实现技术上的一个大间题
menber templte fnctioms 在具现行为 (instantiatioe) 发生2
前他一样没有缴到完全的类型检验，这导放某查十分露骨的Iienplar 错误声明觉
然得以通过编译。例如下面的 icmplaoc 声明
teaplate <class type>l8ss Foo
pabisei

ty9* _ai
不论是 cfheet 或 Sen 编译器或 Borland 编泽器，都不会对以下程序代码产
生想言

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p322_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 323

第 7 章 是在对象根型的炎明 (Os e Cet of te Ohjet Medel
// 目前各家编济器都不会显示出以下定又的通句余接面语意量误ber 不量 class f个 menber fusc
enplate cciass type>

再说一次，这是编洋器设计者自己的决定。 Tempatc facilty 并授有说不允许
对templme 声明的类型部分有更严始的检验，当然，像这样的错误是可以被发现
并标示出亲的。只不过是没有人去缴黑了。
Template 中的名称决议方式（Name Ressltiss wiais Tenlate）
售业须能够区分以下H种量、种是C+Stnded 所满的“wepe ef e
mplate deaiten，也汇是“叉出 emplane”的程序，另一种是 C++ Stundnrd
所谱的“sepe oft imlhoe lastamtiatie”，也就是“具现出 tmplan”的程序
第一种情况单长红下
estrs doc.hle foci douia 11
class SeopeReles

第二种情况学例加下
// scope of the tenplate iaatantiatio

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p323_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 324

深度据素 C+ 对象模型 (bride 7%r C+ Obyecr Moc
en int foo[ int 1)
hcagelulest iast > st:
在 SeepeRaler template 中有胃个 foo) 调用握f作。在“scope ef temg
efaie中，只有一个 oo)通数声明位于 scope 之内，然面在“soope o6
mplate instantiatioe”中,两个 oeg 通数声明都位子 sope 之内，如果我们有
个函数调用操作：
sr0.smar1ant(1tenplate Instantiation
那么，在imarin）中调用的究竞是部一个foo0)函数实体呢
/ 调用的是感一个 t0o() 通数实体
在调用操作的那一点上，程序中的两个函数实体是
estecspdot the template decarat.ion
inatantiation
而_wwl的类型是i，那么你认为选中的是哪一个呢？（提示：除了陪殖之外
唯一能够正确回答的办张，就是知速正确容案）。结果，被选中的是直觉以外
的那一个
Garation
Tempae 之中，对于一个moememerame 的决议结果是根据这个 name 的
使用是否与“用以L具覆出该Iempate 的参数类型”有关而决定的，如果其使用互
不相关，那么就以“sope of the tmplaee eclaios”来决定 nanc，如果其使用
互有关联，那么就以“spe ofe temlae instniaion”来决定 rae，在第一个

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p324_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 325

第 1 量 8在对象模型的炎期 (Oe he Cep of te Ohject Mec

%子中，foa0 与用以其现 ScapiRhair的参数类型无类
/ the reaoliutios of foo () is set
sber = foot _9al 11
这是国为 _ver 的类型是 int: _ver 是个 “类型不会变动” 的 tomplate clasr
mher，也就是说，被用来其我出这个emplate的真正类型，对于_nl的类型
并没有患响，此外，函数的决议结果只和函数的原型（signinure）有关，和通数的
驱润值没有关联，因此：_mmber的类型并不会影响事一个foo实体被遇中
foe0的调用与 template 参数毫无关联：所以调用操作必蛋根据“scopc of thc
emphe delarnion”来换议，在式 scope 中，只有一个 foo0） 候选者（注意，这
种行为不能够以一个菌单的宏扩展比如境用个meme—直我2）
让我们另外看看“与类型相关”（type-dgendnt）的用法：
st0.type_depesdent ():
这个函数的内容如下
retues foo1 _sesber 1s
它究竞会调用感一个eo呢
这个例子很清能地与emplant 参数有关，因为该参数将决定_mcmber 的真
正类型, 所以这一次 foag 必须在 “sope ef the template insoe°中决议。本
务中这个sepe 有两个foe)函数声明，由于_mmber的类型在本例中为 it
新以应该是 int 版的 foo0 出战。如果 Scopefhaler 是以 double 灵型具现出来
那么就应该是 double 版的 foo) 出线， 如果 ScgpeRaler 是以l umsigned int 或
long类型具现出束，那么joo)调用操作就暖球不明，最后，如果ScoprAhier是
以某一个 cas 类型具或出来，面读 chas 授有针对i 度 doubhle 实现出
ertien运算符，那么foo）调用操作会被标示为错误，不管如何改变，都是

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p325_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 326

聚.BN浆 C++ 对I象根 S (Ieriale Ther C++ Ohbyeer Modef

Bl*'soope ef the template instastiioe”来块定，面不是由“ope efthc tcmg
declaratiena决定,
这意味看个编译器必须保特胃个 scope contcxt
1. *scape af the template declaratien* . 用以 专注于般的 tomplate cias
2.“sepe ef the template instamtiatios”，用以专注于特定的实体。
编译器的决议（reselatie）算出必须决定部一个才是适当的 scope，然后在
其中搜寻适当的 same
Member Funetion 的具现行为 (Member Functios Instantiation)
对 于 tmplate 的 支时 。 最图 难 莫过 于 tcmplaoe funtiee 的 具或
Rartiaion），目能的编译器提供了两个爱略：一个是偏译时期策略，程序代
码必须在 pregram tee e 中条要可用：另一个是蜡缓时期蒙略，有一些 mnctb
oa 工具可以导引编证器的具现行为 (insta
下面是编译器设计者必须送答的三个主要问题：
1.编证器如何我出函数的定文？
答案之一是包含mplam pregrar test Sle，联好像它是个 hcader 文件一样
Barland 编译器就是通着这个策略，另一种方法是要求一个文件命名规则，例如，
我们可以要求，在 Point.h 文件中发现的通数声明，其 tmplae progam tet 一定
要放置于文件 PoinLC 或 PoinL.cpp 中，依此类雅，cdhoe 就是准循这个策略
Edison Desige Goup 编洋器对此河种管略都文持。
2. 编泽器如何能够只其现出程序中用到的menber feincties
解决办础之一就是，根本忽略这项要求，把一个已经具现出来的cdas的所
nber fanctioms 都产生出意。 Borlaed 就是这么做的——苗然它生提供

292

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p326_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 327

第 7 童 在对象提型的实指 (Oe he Cetp ofte Objext Model
pragmas让作压制（或具现出）特定实体，另一种策略就是价真髓接换作，拉测
看看那一个函数高正需要，然后只为它（们）产生实体， cfhomt 就是这么做的
3.编泽器如何阻止member definities 在多个文件中都被具现呢
解决办强之一就是产生多个实体，然后从链独器中现供支持，只暂下其中一
个实体，其余都起略，另一个办法就是由使用者来导引“仿真链横险段”的且现
策略，决定哪些实体（inmamos）才是所需求的。
目前，不论编译时期成链膜时期的其成（inantaion）最略，其弱点就是，当
tumphatc实体被产生出案时，有时候会大量增加编评时网，提显然，这将是tcomplit
fancies第一次具现时的必要条件，然面当那些函数被非必要地再次具现，或是当
“决定那些通数是否需要再具院”所花的代价太大时，编证器的表现令人失望！
C+* 支持 wmplaxc 的原始意图可以想见是一个由使用者导引的自动具现机
制 (use-dirncted wtemtic insartiatioe mechanion) 。 既不需要使用者的介入。也
不需要相同文件有多次的具现行为。世是这已被证明是非营难以达成的任务，比
任何人此到所能想象的还要电（清参考[STROUP94])， pdink.随者 chromt3.0 项
所附的原始具现工具，提供了一个由使用者执行的自动具现机制（us-divm
o mnchaisn），但它实在大复杂了，即使是久经世敏的人生
投一下子了解。
Edicn Design Grap 开发出套第二代的 dirested-instanritiae 机制, 非常接
近于（我乒年的） twmghoe failly 原始强文。 它的主要过程如下;
1. 一个程序的程序代码被编译时。最析并不会产生任何“template 其现
”，然西，相关售息C经被产生于object les 之中，
2.当objeet files 被膜在一块儿时，会有一个prelinker 程序被执行起来。
它会检查otjeest iles，寻找 tmplate 实体的相互参考以及对应的定义

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p327_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 328

尿度聚素 C++ 对象模型 (/nide 7hr C++ Obyeer ,Modn/)
3.对于每一个“参考到templaee 实体”百“该实体却没有定文”的情况
pelinker 将该文件提为与另一个文件（在其中，实体已经具现）同类
以这种方地，就可以将必要的程序具现操作指定给特定的文件，这些都
会注册在 poelinker 所产生的i 文特中（放在磁盘目录 iLfle)
4.pelinker重新执行编译器，重新编泽每一个"i文件鲁被改变过”的文
件。这个过程不断重复，直到所有必要的具我操作都已完成
5.所有的cbject files 被键接度一个可执行文件。
这种 dincted-intantitioe 体制的主要成本在于，程序第一次被编译时的三
文件设定时间，次要成本则是必须针对每一个“compilc afarwatd"”执行
peinker，以确保所有被参考到的wempale 都存在有定文，在量初的设定以及成
功地第一次储膜之后，重新编译操作包含以下程序：
1.对于每一个确被重新编译的 prognam text le，编译器检查其对应的 i
文件。
2.如果对应的文件列出一组要被具庞（iestainted）的 tomlates.那
些templatcs（百且也只有那些nemplates）会在此次编译时被具现，
3. pelinker 必须执行起案，确保所有被参考到的 templates C经被定文妥
以我的观点，出现基种形式的 antomed tempime 机制，是“对程序员友善
的C++输译系统”的一个必要组件，星然大家电公认，目前股有任何一个系统
是完美的，作为一个程序开发者，我不会使用（也不会推荐）一个设有这种机制
的编通系统。
不率的是，授有任何-个机制是没有 hsp 的，Edisoe Desige Group 的编译
签使用了一个由 cfo2.0 引人的算[KOENIO90],针对程序中的每个 cas
自动产生 vitualtabie 的单一实体（在大部分增况下）。例如下面的 clas 声明：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p328_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 329

第 7 章 延在对象模型的实编 (On he Cup of de Ohjict

pitl1e1virteal -hrixitiveobjeet (1)
virtosl void drav(),

如果它被含人于15个或45个程序要码中，编评器如何能够确保只有一个
vintaal table 实体被产生出来呢！ 产生 15 份域 45 分实体例还容易费
Andy Koenig 以下面的方提解决这个间题; 每个 virtual functiee 的地址都
被放量于 active classes 的 vintsal table 中, 如显取得函数地址，别表示 vital
fnction的定文必定出观在程序的某个地点：否则程序就无国链独成功，此外，
该函数只能有一个实体，否则也是链接不底功，那么，就把virtal taole放在定
文了该 dlas 之第个 se-inline、 nospere virtsal fsnctios 的文件中吧。 以我们的
例子图富。编评器会格 virtual table 产生在储存着 virtual destnuctor 的文特之中
不季的是，在temmpliee 之中，这种单一定文养不一定为真，在 tomplate 所
支持的“将模换中的每一样末西都编语”的模型下，不只是多个定文可能被产生。
而且随接器也放任让多个定文同时出观，它只要选择其中一个面将其余都息略出
就是了。
好吧, 其是有超, 但 Edison Desigs Groep  sutomatic instana5ien 机制能价
么事呢！考虑下面这个Ibrary 孟数：
void fso( osat Peintc float > *ptr )
ptr->virtual_fusc(1;
vitsal finctios call 被转换为类似这样的东西：

C* SusddC经披检了对这一点衍要求

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p329_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 330

/ C++物码
*ptr->_vtb1_Poist< float >[ 2 1 1I gtr 1r
于是导股具现 ( instartiated ) 岛 Poier class 的个foat 实体及 其
vonor finc). 由于每个 vitsal fanctioe 的地址被胶置于 ate 之中。 如果
vital tablie 被产生出束, 每个 vital finstiee 也都必质被具死 (inta
这就是为什么 C++ Standard 有下要的文字说明的腺放
如果一个 vinal hinctiee 被其现（instantiaed）出束,其具观点室跟在其 clas
的具现点之后。
然图，如果编译器遵据 cfhom 的 vitsal tblc 实现体制， 那么在“Psie 的
Bem 实体有一个 vital desructer 定文被具成出案”之前，这个 uble 不会被产
生。除非在这一点上，并股有明确使用vinual dentructor 以担保其其现行为
E6isc: Desiga Cmp 的 snomaic tmplte 机制并不明确它自己的编译器对
于第一个 m-iline，mme vinal functi 的鲁毒使用，所以并设有把它标示
于1文件中，结灵，始接器反面回头拖想下面这个符号没有出现：
_vth1_eintc [1sar >
并柜绝产生一个可执行文件、映，真是麻频！Aumaic insatisioe 在此失收！
程序员必须明确始强造将dctructor 具成出来，目前的编译系扰是以prngma
指令案支持此胃求，然而 Cr+Standaed 电已经扩充了对mmplhee 的支持，允许
程序员明确地要求在一个文件中将整个cleis templinte 其成出来：
tenplate cLass Peint3ds fleat >j
或是针对个 template class 的个别 member functions
tenplate float Point36cfloat>::X() eeast.

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p330_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 331

第 1 量期在对象根型的失指 (On he Ce
或是针对某个个别 templtc finction

在实成层面.上， template inittion 影甲指绝全面自动化。其至显然每件
工作都做对了，产生出来的otjet fles 的重新编详成本分然可能大高———如果程
序十分巨大的诺！以手动方式先在个别的cjelmae 中完成预先其现覆作
tios)，虽然风闷，部是唯一有敏率的方读。
7.2异常处理（Exception Handling）
款支持exepie handliag.编译器的主要工作就是找出 cath子句，以处理
被丢出来的exoepeie，这多少需要递踪程序准模中的每一个通数的当前作用区域
（包括追踪通数中的 local clas otjects 当时的增况) .同时，喻译器必须现供基
种查询csoeptiee ojcts的方法，以知道其实际类型（这直接导致某种形式的执
行期类型识别，出就是RTT1)，最后，还需要某种机制用以管理被丢出的cbjct
包括它的产生、储存，可能的解构（如果有相关的 destrnctar）、清理（cleansp）
以及一般存取，也可能有个以上的ctjects 网时起作用，一般两言，cexoptioe
handing 机删黄要与编译答所产生的数据结构以及执行期的一个cxcaption
tibrary紧密合疹，在程序大小和执行速度之间，编译器必须有所快择
■为了维持执行速度，编译器可以在编评时期建立起用于文持的数据洁
购，这会使程序影账的大小，但编译器可以几乎忽略这径结构，直到有
被丢出来
■为了维护程序大小，编译器可以在执行期建立起用于支持的数据结构
这会影响程序的执行速度，但意味看编译器只有在必要的时额才建立那
些数据结构（养是可以抛弃之）：
根墨[CHASE94 所言，Modsla-3Rapom 竞然将一个原本为了维护执行连度
面编好的敏法交成了一个制度，它警成“在ccpionalca 上花费1000 个指

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p331_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 332

NNR C++ 3fIR福2) (Aenide The C+ + Otyeet Madn

令.以节省正靠情况中的一个指令”-但这样的交易井非水远账行无碍，最近在Te
Asiv 的-场研付会中，我与 Shay Bushinky 变读，急是“bamier”项且的开发者
“Junior”是一个国际象模程序，在1994年冬季的全理计集机国际象棋程序大赛
中与IBM的医蓝（De Biu）得分相网，井列第三，令人惊语的是，这个程序
在一部 Pentiam 个人计算机上运行 （Dep Blae 则使用了 256 额 CPU) 。 他告
诉我当他们在并有 eoeptios haslling 的 Berland 编评器上重新编译“hunior” 之
后，程序虽然投有任何改变，内存却不够运用了，继果，他们只好留头找一套泪
版的Bortand 编译器，对于“hmior”面官，它并不需要一个体积度大但是没有
增加多少波击能力的新版本
过去还有一种错误的看法，认为由于excepioehanding 的出现才导致 chom
的灭地,因为不可能提供一个腹可接受用又强固的 esoeptiam hndling 机制,都没
有支持程序代码产生器（和链接器） 。 UNIX Sedoare Labontory （USL） 当初属
置了由 HP 易交出束的 esception handing C-gmering implemenation 大约有-
年之久（请看 [LENKOV92]）。 USL 最后降于一致赞成取滴掉 cdom 4.0（及任
有更高版本）的开发计划。
Exception Handling 快速检阅
C++的 esoptionhanding 由三个主要的语汇组件构成
1.一个thre子句。它在程序某姓发出一个exoeptiea.被丢出去的
exeeption可以是内建类型，也可以是使用者自定类型。
2.一个多个emtch子句，每一个catch子妇都是一个cescption
handlr，它用来表示设，这个子句准备处理某种类型的esoxptien，并且
在封阅的大括号区段中提供实际的处理程序
3.一个 try 区段，它被图捷以一系列的数述句（stmmmts）.这些叙述句
可能会引发cakch子句起布用。
当一个cxcopion被丢出去时，控制权会从通数调用中被释放出案，并寻找

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p332_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 333

第 1 章追在对 象模型的实期 (On thc Cuip-ef the Objet Model)
一个吻合的cath子句，归果都没有吻合者，那么默认的处理例程un
被调用，当控制权被放弃后，准找中的考一个通数测用也就被推高（popedup）
这个程序称为uswinding the stack.在每一个函数被推离唯栈之前，函数的 Iioci

Exoeptien handling 中比较不那么直党的就是它对于那签似手级什么事做的
孟数所号来的影响。例如下医这个通数：
002
+0003
pinst agol, *p2pt1 = fso()
retsre 0:
Pmiat p)
*0011001:
+001.3+20:14:etets pt1/
+901200161
如果有一个 cxczptios在第一次调用 foo)(1.5)时被丢出，那公这个 msmhle
函数会被推出程序唯栈.由于调用fooG的操作并不在一个py区段之内，位就
不需要尝试和一个 cate 子切吻合，这里也没有任何 local class etjects 需要解
构。然面如果有个 csception 在第二次调用 foo0 （L11) 时被丢出。exceptio
handing 机制就必须在“及程序堆线中“umwinding”这个函数”之前，先调用

在 excxptios handing 之下, L4-L8 和 L9-L16 截现为阿块适意不间的[
城，因为当cscoption被丢出来时，这两跌区城有不间的执行期语意。而且，
支持ccapioe handing，美要额外的一些“登记”操作与数据，编译器的做法有

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p333_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 334

聚度探索 C++ 对象模型(Juide Tle C+ Oyner Moner)
两肿。一种是把再块区域以个别的“将被握股之 local otbjects”链表（已在编评时
期设要）联合起来，另一种徽法是让两换区城其享间一个链表，该替表会在执行
期扩大或小
在程序关层面，cccpie handling也改变了函数在资源管理上的语意，例如，
下面的函数中含有对一换共享内存的 locking 和 umiocking 操作，虽然看起来和
ptions 没有付公关联。但在 ekception handing 2下并不保证能够正确运行：
voLesueble( roid *aresa )
Poist *p a* neu Poist.( arena 1r // function cal1
/./ 如果有一个exception 在此发生。间题就象了
satr5eck( arese 12 / fusction ca11eiete pv

本例之中，cxoeptieehanding 机制肥整个函数机为单一区域，不需要操心“将
函数从程序堆栈中“unwinding”“的事懂，然而头语意上来设，在运数被推出堆
栈之票，我们需要uock 其享内存，井aklne p.让通数成为“esapionpe
的最明确（组不是最有效率）的方接就是安插一个dctalcach子号，像这样
vesdble ( void *areta I
tock ( aresa 11
/..
fateh( -.- 1 (
telete pi

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p334_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 335

第 7 靠贴在对象度型的失编 (Oe e Cesp efhe Ok)

这个函数现在有了两个区城：
1. by block 以外的区城，在那里。cxoeptios handllling机a制B了“pep
序堆栈之外，设有其它事情要做
2. aby bleck 以内的区城 (以及它所联合的 &efault catch 子切)
请注意，nnw运算符的调用并字在Dy区段内，这是我的错读吗？如果nn
运算符成是Porcomtncter 在配置内存之后发生一个cation.那么内存医不
会被 unloking,p 也不会被 drine （这两动作都在comch 区股内）。这是正确的
站意吗
是的，它是，如果mrw 运算特丢出一个cxaption，那么就不需要配置 hcap
中的内存，Poir costuctor 也不需要被调用，所以也就没有理由调用drlt 运
算荐。然面如果基在Poaie conrncter 中发生eoceptios，此时内存已配置完成
那么Poir 之中任何构造好的合鼠物或子对象（notjsect，也就是一个mmher
clas stjee 或 hate clas objoct) 都机自动被解构排，然后 hea 内存电会被释放
掉.不论哪种情况，都不需要测用iee运算，
类似的道理，如果一个csaqpion是在ww运算特执行过程中被丢出，orm
所指向的内存就绝不会被locked，固此，这度有必要unlock之，
处理这些资源管理问题，我的一个建议办法就是，将资面需求封装于一个
clas otjet 体内，并白 detnucter 来静放资源（然西如果资源必须被案求，被释
放、再被索求、再被释放许多次的时候，这种风络会变得有点累资）：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p335_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 336

深度报家 C++ 对象 股型 (Auidle The C++ Olyrer Mouel

mzble( void *arena
ste_ptrcPoint> ph ( tev Pint 1/
/如果这里丢出一个 except.Lon，我在就没有风题了
//不期要男施un
/ ss.SXEock11-SREock ()1// ph.asto_ptr<Point>::-asto_ptz<Poist>()

从esceptiee handing 的角度看，这个函数现在有三个区段
1. 第一区段是 ntt,pr 被定义之处。
2.第二区股是 SMLack 被定义之处。
3.上述两个定文之后的整个函数。
如果eseption 是在aMppr ceestnwter 中被丢出的，那么就授有 active
local cbjects 黄要被 EH 机别握强.然面妇是 SMLock coestructor 中丢岛一个
cxcaption，则auto_pe otet 必须在“uinding”之前先被爱，至于在第三个
区设中。两个 locl etjects 当然都必提被推级。
支持 EH, 会使那供据有 member claes subobjects 或 base class subebjects (排
且它们也都有 coestrucons）的 clases 的 conutrathor 更复杂。一个 claes 如果护
部分构造，则其 detrwctor 必须只施行于那费已被构造的 suboebjets 和（或）
mcnber otbjocts 身上。 例如, 假设 clas.X 有 mxmber otjocts.4. B 和 C, 都各有
3 censtuetor 和 destnxter, 如量 4 的 conitractor 丢岛个 eceptioe, 不论
4 或8或 C 都不需要调用其 destrudor。如果 8的ceestt
csception.则 4 的 dcstructor 必须被调用。但 C 不用。处理所有这些意外事放
是编洋器的贯任

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p336_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 337

第 1 章是在对象根型的失明 (O% e Cesp ef tbe Ohjet Model
同样的通理，如果程序员写下：

会发生两件事：
1.从 heap 中配置是以始 512个PeinrM otjects 所用的内存。
2. 如果成功。先是Poiw2 coastructor，热后是PowMcosstructor，会速行
于每一个元累身上，
如果27元素的Pow comstrcter 丢出一个 espin，会怎样呢？时于
427元素，只有 Poe2d octructor 需要调用执行。对于缩 26个元素，Poinr3d
destrctor和PoowMdesrucler都费要起可执行，然后内存必须被释放因去
对 Exception Handling 的支持
当一个cxoption 发生时，编译系统必须完成以下事情：
1.检验发生bhro 操作的函数：
2.决定diro 操作是否发生在ory 区段中：
3.若是，编泽系使必须把cxceptieetype 拿来和每一个 canch子句比较
4.如果比校哪合。填程控制应该交到cath子句手中：
5.如果dr 的发生井不在 by 区股中，减授有一个catch子句物合，服
么系统必强 (a) 报级积有 active local cbjects, (b) 反堆栈中将当前的通
数“wine”排，（x)进行到程序难我中的下-个函数中去，热后重复上
证# 2-5,
决定throw是否发生在一个 try区段中
还记得吗，一个函数可以被想象成是好儿个区城
■ary 区段以外的区城, 周L设有 active local object

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p337_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 338

原度指董 C++ 对象模型 (Auidr 7he C++ Obyecr .Moal/)
■ary 区股以外的区城, 组有个 (以上) 的 aetive lecal ebjects 需要解购
■bty区投以内的区城
输评器必须标示出以上各区域，并使它们对执行期的escepion handing 系统
有所作用，一个很棒的策略就是构造出pregam coumr-ang表略，
图忆下, program coenter (绿注： 在 Intel CPU 中为 EIP 缓存器) 内含下
一个即将执行的程序脂令，好，为了在一个内含Dy区段的通数中标示出某个区
域.可以把 pegram counter 的起始值和结重便（或是起始值和花围）储存在一个
表略中
当drw 操作发生时，当靠的pgnmouler 望被章来与对应的“范服表格”
进行比较，以决定当前作用中的区城是否在一个by区段中，如果是，就需要找出
相关的 cakch 子句（糖后我再来看这一部分）。如果这个 cscepice 无法被处理
（或者它被再次丢出），当前的这个函数会反程序唯栈中被推出（peped）。而
tr会被设定为调用瑜地址，担后这样的是坏再重新开始。
路exeeption的类型和每一个catch子句的类型做比较
对子每一个被丢出来的eweptioe，编译器必须产生一个类型提述器。对
cscpicn 的类型进行编码，如果那是一个derihedtype，则编码内容必须包括其
所有 bae cas 的类型信息, 只编还 peblic base claes 的类型是不够的, 因为这个
eptiee 可能被一个 menber functioe 提,而在一个 mmber fancien 的范至
(scope) 之中, 在 derived class 8 sonpublic base dlaas 之间可以转换
类型腊述器（typc dscripoe）是必要的，因为真正的exoaption 是在执行期
被处理，其otect必须有自己的类型信息，RTTI正是因为支持 EHI 而获得的额
产品，我将在 73节讨论RTT1.
编译基还必须为每一个cmch子句产生一个类型措述器，执行期的exoepiet
hander 会对“被丢出之otjocst 的类型撞述器”和“每一个 cwor 子句的类型绩

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p338_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 339

第 1 量 贴在对量提型的失编 (Os de Cap ofde Ohjet Model
述器”进行比较，直至找到吻合的一个，或是直到堆栈己经被“a
inate已被调月
每一个函数会产生出一个cxcaption 表格，它猫述与函数相关的各区域，任
得必要的著后特 (ciem code, 被 local dles objet destrusters 调用) , 以及 crch
子句的位置（如果某个区城是在try区段之中）
当一个实际对象在程序执行时被丢出，会发生什么事
当一个 cxcxpion 被丢出时。cscoaption otjiecr 会被产生出亲并通常胶置在相
同形式的 ceion 数据堆栈中，以arew 增带桑地 catch 子句的是cscptioe
cbject 的地址、类型接述器（或是一个函数指针，该函数会传团与该 esoaption tpe
有类的类型述器对象），以及可能会有的ceplieejeu 植述器（如果有人定
文它的话）
考虑一个 cach 子旬如下:
cetch( exheint p )
/ do sosethisng
以及一个cption ctjec，美型为cFeer，握生自wr，这两种类型都喘合
于是cath子句会作用起案，那么p金发生什么事？
■p 将以cxceptios objoct 作为初值，就像是一个函数步数一样，这意味
若如果定文有（或由编语器合成出）个cpy constrcter 和一个
destrscter 的诺。它们都会实蓬于 local copy 身上。
由于p是一个abjeet 面不是一个reteresce，当其内容被携员的时
tien objeet 的 nee-esPoow 部分会错样 (sliced off) , 此外
如果为了 excepioe 的继承而提供有 virtusl functions,， 那么p 的 vir
会被设为exPoor 的 virtal table:exceptien object 的 vper 不会被携扎

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p339_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 340

报tNR C++ X& 提2 (AesAle 7he C+ + (tyee Modw)

当这个ocaptioe 被再丢出一次时，会发生什公事情呢！p现在是繁殖出来
的etject ? 还是员 shrov 增产生的原始 esceptiee objct? p 是个 loca
object。在catch 子号的末酯精被服验。丢出 p 需得产生另一个输时对象、并意
味者费失原来的 cxception 的 Fertex 部分。 原来的 cxcoptionobject银再
丢出：任何对p的修改都会被抛开。
像下面这样的一个 caeh 子号：
atch( exleint srp 1
// 6o somet3is
throm;

则是参考到真正的eceptio otject。任何建拟调用都会被决议（nsolved)为
nces active for esFertes, 也就是 eception object 的真正类型, 任何3此 object
的改变都会核影到下个cath子句中
最后，这里提出一个有趣的进题，如果我们有下面的 shro操作
axveziex err/st
..

titrow errVerzable () * 1/
//...

究宽是真正的ocaptioe erie 被繁殖，抑或是errler 的一个复制品被构造于
ccaptias mack 之中并被量殖？答案是一个复制品被构造出来，全局性的errler
并授有被累理，这意味看在一个cath子句中对于cxopion ohject 的任何改交

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p340_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 341

第 7 章站在时象模型的只编 (On he Cup ofhe Ohject Model)

会再丢出印  B ebjet才会银指强
在某次对 PC C++编泽器的评论当中（请参考[HORST95])，Ca
n测量了 EH 所带来的效率和大小的额外负担，Cry 编泽务执行一个期以
程序。产生并握级大量的 local etjrct-它的有自己的
两个程序都不发生实际的它们之间的差异只截手其中
内有个eac.)子句，下要是他测量了
的结是，首先，程序大小有异

87,310  1.%

其次，执行速度也有异：
兼格7.2执行速度（两种情况
7移

与其它语言特性作比校，C++编泽器支持EH机制所付出的代价最大，黑种
程度上是由于其执行期的天性以及对底层硬件的依赖，以反UNIX和PC两种
平台对于执行速度和程序大小有曹不同的乘合优无状态之款

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p341_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 342

聚度型浆 C++ 对象模型 ((eide 7e C++ Obyeer Mode/)

7.3执行期类型识别（Runtime Type Identification，RT
在chom中，用以表现出一个程序的所调“内那类型体系”，看起来像这样
/ 程序星次结构的提类（root class)ciess node ( -.- )1
//deciyedt, typepeisters,arra9,/ tot ef the 'tyee' potres, basic tye,
class type I public sode (  1)
/ tes represestatiora for fanetioss
class ges : psaLie type ( -.- 17Lass fct : public type (
其中 gee 是 goneric 的菌写，用来表现一个 overloaded fa
于是只要你有一个交量，或是类型为opr*的成员（并知道它代表一个函
数）.你就必须决定其特定的 dcrived tpe 是否为ér 或是 gem.在2.0 之前，除
tructor 之外唯一不能够被overloaded 的函数就是coevenion 运算符。例妇
class String (Pubiiet
rat4 ctax*(1)

在2.0 导人coat member funtioes 2前，comias 运算符不能够被
oveloaded,因为它们不使用参数。直到引进了 consmember fintios 后，增况才
有所变化，现在，像下面这样的声明就有可能了：
clas String (
operatee ctat*() etesti

308

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p342_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 343

性就是说, 在 2.0 版之前, 以一个 explici cae 亲存取 deived etjet 总是安
全（面正比较快速）的，业下面这样：
typedet tyte*pftype
siaplify_oosy_sp( ptype pt 1
et pf = pfet( pt 11Lon operatots can oniy be feti

在commmber fncicas 引人之前，这份码是正确的，请注意其中甚至有一
个批注。说明这样的转型的长全哲。包是在conat momber functioes 引退之后，不
论程序截注或程序代码与不3了，程序代碍之所以失数，非意不率是固为Sring
claos 声明的改变-得为her* c:mversice 运算特我在被内那视为一个 g 而不是
一个e。
下面这的转型形式：
pfet pt = pfet( pt 11
被称为 doncat（向下转型），因为它有效地把一个 basecdas 转换至继承结构
的末晚，变成其 deivel elaes 中的某一个，Dowmcat 有增在性的意险，因为它
温制了类型系统的作用，不正确的使用可能会带亲错误的解释（如果它是一个
mad 操作）或腐独掉程序内容（如果它是一个wric 操作），在我们的例子中
一个指向grm ohjet 的指针被不正确地转型为一个指向crotject 的指针nf、所
有后续对g的使用都是不正确的（除非只是检查它是否为0.或只是把它靠来
和属它指付作比较）

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p343_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 344

深SN素 C++ 3象 根显 (Juidle The C++ Obyer Modr/

Type-SafeDowncast（保证安全的向下转型操作）
C++被吹毛求就的一点就是，它缺乏一个保证安全的 dowmcan（向下转型操
作）。只有在“类型真的可以被适当转型”的情况下，你才能够执行dowca（情
看 [BUDD91]) ，一个 tpe-afe downcat 必须在执行期对指针有所查询.看看它
是否指向它所展现（表达）之cbjet 的高正类型，医此，敬文持 tbpe-aft dor
在 otea空间和执行时间上都需要一些额外负担：
■需要额外的空间以储存类型信息（type infermaiee)，通常是个指针
指向某个类型信息节点
■需要额外的时间以决定执行期的类型（ranimg type），因为，正如其名
所示，这需要在执行期才能决定
这样的积制题对下面这样平意的C继构，会如间影响其大小，致事，以及
随接兼容性呢！
char *virnie_tb1[ 1 = ( *ruely in sy tuny°, *oh, bother*
祖明显，它所导致的空同和数率上的不良握应客为可观。
冲突发生在两继使用者之间
1.程序员大量使用多态（polymorphism），并园面黄要正统面合出的大量
douscat 推作
2.程序员使用内建数据类型以及非多态设备，因面不受各种额外负担所步
来的拉应。
理想的解决方案是，为两抓使用者提供正统而合法的需要一显然或许得暂
牲一些设计上的纯度与优履性，你知道要加么教吗
C++的 KTT1 机制提供一个安全的 dowmcan 设备，但只对那些展现“多态
（也就是使用继承和动志辨定）”的类型有效，我们如有分房这格！编译器能否

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p344_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 345

第 7 京随&对象模型的次编 (Oe e Curg ef Otject)
光看 clas 的定文就决定这个 cas 用以表现一个独立的 ADT 或是一个支持
态的可腺承子类型（ubype）？当然，董略之一就是导人一个新的关键词，优点
是可以进楚地识别出支持断特性的类型。缺点别是必领翻新旧程序
另个蒙略是经血声明一个或多个 vinul fhscioms 来区别 clas 声明, 其优
点是通明化地将旧有程序转换过来，只要重新编译就好，缺点则是可能会将一个
其实井非必要的 vintai functiee 强迫导入继承体系的 bae chas 身上。毫无冠问
你还可以想出更多蒙略，不过，目前所说的这一个正是RTT1机制所支持的策
略 。 在 C++ 中,一个具备多态性质的 class (所调的 pohmorphic class)， 正是
内含着糖求用束 (或直接户明) 的 vitual fanctions-
反编译器的角度来看，这个策略还有其它优点，就是大量降低额外负担，所
有 polymorphic clases  ohjeets 都维护了 个指村 (vper) , 指R virtsal functior
tbic. 只要我们肥与该 clas 相关的 RTTletbjet 地址放进 virtsal abie 中 (通常
放在第一个 slet），那么服外负担就降低为，每一个claesotect 只多花费一个指
针，这个指针只需被设定一次，它是被编评器静志设定，面不是在执行期由cas
wtaor 设定 （vptr 才是这么设定）：
Type-Safe Dynamie Cast（保证安全的动态转型）
aamir_cant 运算符可以在执行期决定真正的类型，如果 downcat 是安全
的（也就是况， 如是 bae type pointer 指向一个 drived das ltjet），这个运算
符会传图被适当转型过的指针，如果downcat不是安全的，这个运算符会传因
0.下面就是我们如何重写费们原本的 choet dom（当热啦，我在的p实际
类型可能是r，可能是grm，比较受效避的程序方接是使用 vital funcion
在此接中，参数的真正类型被封装超案，程序比校清哪也此校容易扩克，得以处
理更多类型）：

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p345_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 346

聚度型素 C++ 对象提型 (Drile The C++ Ol/eer Monl
typedeteaptioe
siplity_eety_opl gtype pt 1
eic_cast< pfet >I pt 11 (
+1s+ ( ..- 1
世公是 dymamir_oar 的真正成本呢！pfér 的一个类型增述器会被编译器/
生出案，由yr 哲向之clas ctjet 类型描述幕必须在执行期通过vr 取得，下
需就是可能的转换：
/取界pt的类型猫述器ya_iate*) (gt-7agptr( 9 111>_ty94_descxkigthe1
op_ijfe 是 C++ Standaed 所定文的类型接述器的 chan 名称，该 clas 中波
置君持素求的类型信息， vintsal uble 的第一个 slet 内含 op_iyfe otjet 的地
址: 此 ope_jfo etjee 与 pr 新指之 clastype 有关 (济看 1.1 节的图 1.3)
这胃个类型推述器被交舱一个 nuntine Iihnry 函数，比校之后告诉我们是否响
合，很显然这比stkecar 昂贵得多，担却安全得多（如是我们把一个Ar类型
“dowmcast” 为一个 go 类型的话) 。
量初对nmimtctn的支持提议中，养未引进任何关健润或额外的语接，下
面达样的转型换作：
ast 的提试通
究竞是 nmtic 还是dymamic.必须视 pr 是否指向一个多态 clas objoct 而
定，贝尔实验室中的我们这一伙认为这样子提棒，但是C+标准委员会想的是
另一套，他们的评论，就我所别，认为这么一案一个代价易贵的 nnime 操作与
一个菌单的masic car大相律了。也就是说，当我们看到这个can操你，无法知
312

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p346_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 347

第 1 章 链在对象最型的火明 (On de Cnp ef he Otjet Modl)
道 p 是否指向多态对象（polymorphic ctject）。也就无法知道这个 casr 操作是
执行于编译时期或是执行期，这当然是事实，然图，vintal fanctios all不也网样
吗！或许C++标准委员也应误引进一种新语继和关键词，以便区分
pt->fosbar(1;
是一个静态决议的 functios all，或是一个通过虚担机制的调用推作！
References井不是Pointers
程序执行中对一个chas指针类型施以4mum_r 运算猎，会嵌得tt
成 faise
■如果传真正的地址，表示这个cbject的动态类型被确认了，一些与类
整有关的操香现在可以施行子其上
■如果传图0，表示设有指向任何abjen.意味应该以另一种定钢施行于
这个动态类型末确定的object身上
ami_r运算特也适用于 neferesce 身上。然而对于一个 no-type-afe
cat.其结果不会与施行于指针的情况相间，为价么？一个meftnce 不可以像指
针那样“把自已设为0便代表了“no objecr”“：若特一个mfree 设为0.会
引起一个临时性对象（我有被参考到的类型）被产生出来，该临时对象的初低为
0.这个refrno 然后被设定成为该临时对象的一个期名（alias），因此当
nic_ar 运算符施行于一个nefeoe 时，不能够现供对等于指针情况下的那
一组ouehls.取面代之的是，会发生下列事情。
如果refereace 真正参考到道当的 derived clas包括下一层或下下一层
成下下下一层或）、4rm会被执行面程序可以继续进行
加refereace 并不真正是某一种derived class.那么，由于不能够传因
0, 道盖出个 baf_cer exceptios.
下医是重新实成后的sp_com_op 通数，参数改为
313

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p347_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 348

聚度R求 C++ 对R模型(Denile The C++ Obyer

sisplify_oeev_og-( censt type srt )
try 1srf = dyssc_cest< fets >1 rt 1
/

其中执行的操作十分理思地表现出种cs  ，面不只是单（
如反前）的控制流程。
Typeid运算符
使用bpeid运算护，就有可能以一个 efrmce 达到相网的执行期替代路线
“allemative pathway”* ) :
siaplify_oey_spl esst type srt )
sf ( typesdt rt 1 ** typeid( fet 1 1
/..Net 4zt = static_cast< fcts >( rt )r
1s4 I -.- 1)
虽然我必须况，在这里，一个明显校佳的实现策略是在grm 和 crclass
部引进个 vintual fenction.
opeid 运算符情居一个 comn referemee，类型为 opr_iyfo.在先前测试中出现
的qtalty（等号）运算符，其实是一个被overloaded 的函数
b001type_info::
eperator=*( censt type_infos 1 cotst

314

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p348_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 349

如果两个opy_iyotjects 相等，这个 eqlity 运算得就传因tne
op_iafeotjet 由ff 含组成: C++ Standad (Sectie 18.5.1) 中对 opr_i6 的
定文如下（错过：你可以在Visul C++的oyeindbh 中找到类敏的定文）

J/ date sesberi
编泽器必须摄供的最小量信息是clas 的真实名称，以及在opr_noatjects
之间的某热排序算续（这就是bor函数的目的）、以及某些形式的殖述器
照来表现 eaplier dles bype 和这个 clas 的任何subopes.在接述 escaptios
handlig 的原始文章（[KOENIC9e]）中，普建议实成出一种接述器，编码后的
字养串（译注）。其它策略谦义[SUN94]和[LENKOV9G]
绿进：Mirsen 的Visul C++就是采用编码后的字特事作为值述器，所以
Visul C+* 的 typeinfb.h 中对于 op_ife 的定文, 翼比上述的 C+ Stuedard 别

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p349_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 350

下面是以 VisualC++完成的个小范例，用以检验编码后的 cas 名称

Foid main()

执行结果为

显然 kTT1 提供的oP_iyo 对于eding的支持来说是必要
的，但对于ecoptionhanding的完整文持面言，还不够，如果再加上额外的-
bpr_afo derived clases, B可[Ele[ exceon发生时提供有关于指针、通数及类
等等的更讲细信息。例如 MeaWare就定义了以下的服外类：
class Proister_type_insfos pablisc type_iadfo/ .. 1

并允济使用者取用它们、不季的是，那些 derived classes 的大小以及命名习

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p350_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 351

第 7 章 结在时象模型的尖编 (Oe de Casp afde Ohjet)
惯都没有一个标准，各家编译器大有不同
虽然我早说过 RTT1 只适用于多态类（gpelmomhi clases)，事买上 opr_yf
cbjicts生适用于内建类型，以及非多态的使用者白定类量，这对于esm
nding的支持有必要，例如：
int ex_errnos
thtow es_errhoi
其中 int 类型速有它自C的 opr_iyfootjct. 下面就是使用法
int *ptr)

在程序中使用opeicxg。健这样：
Lst 1val.a
type54( iva1 1 -.1
或是使用opeitypc)。像这样。
ty9e14( 4ohLe 1 ---1
会传因一个 comt ope_d，这与先前使用多态类型（pomophie opes）
的差异在于，这时候的bpgochjsct是静态取得，而非执行期取得，一般的实
现策略是在需要时才产生ope_ipoctjet，面非程序一开头就严生之。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p351_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 352

深度授素 C++ 对象根型 (Juside Tle C+ Objicr Mod

7.4效率有了.弹性呢
持扰的C++对象模型提供有效率的执行期支持，这份效率，再加上与C之
间的兼容性，造成了C++的广泛被接受度，然面，在黑费领城方面，像是动态
其享函数库（dymaniallyshared ibraris） 、 共享内存 （shared momory) 、 以及分
布式对象（diribued otject）方面，这个对象模型的弹性还是不够。
动态共享函数库（Dynamic Shared Libraries）
理想中。一个动态随接的shed Sihary应该像“突然造访”一样，就是说
当应用程序下一次再执行时，会通明化地取用新的 ibhry 原本，新的 Iibrary 同
重不应该对旧的应用程序产生便略性，应用程序不应该需要为此重新建造
(bulding) 一次、热面，在目前的 C++对象模型中,如果斯质 library 中的 clas
cbject 布局有所交更，上述的“ibrary无侵略性”说然便有持商整了，这是图为
chesn 的大小及其每一个直膜（或继承面泉）的 membors 的编移量（offhet） 都在
编译时期就已经固定（虚拟继承的membes除外），这虽然带来效率，埠在二进
制层面（binary kevel）影响了弹性，如果ctbjea 布同改变，应用程序就必须重距
编译。[GOLD94]和[PALAY92]两需文章描述了前人的许多有遵的努力，希望
把C++对象模型雕至更具“突然通访”的能方，当然，这会丧失部份的执行期
建度优势和大小优势。
共享内存（SharedMemory）
当一个 hared Ihrary 被加载，它在内存中的位量由 nnine Inker 奖定，
般面言与执行中的行程（procs）无关，然到，在C*对象模型中，当一个动
态的 sharned lbrary 支持个 clas cbjet， 其中含有 vinal fanctioas (被波在
shared memory 中),上述说法便不正确-问题并不在于“将该 cbjeet 致置于 shacd
mary 中”的那个行程，百在于“想爱经由这个 shared objsct 附看并调用一个
vital functie” 的第二个或更后继的行程, 除鲁 onanicshared iomary 被放置于
318

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p352_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 353

第 1 童 指在对鲁报型的义量 (Os Ie Cwi of be Otject Modll)
完全相同的内存位置上，就像当韧加载这个hured ctjcn 的行程一样，否资
vinusl functioe 金死得提难看, 可能的错误包括 mgmem fsht 或 bus emor, 病灶
出在每一个 vintua fanction 在 vintal table 中的位量已经被写死了。 目前的解决
方法是属于程序是图。程序员必须保证让跨越行程的 shared ibraries 有相网的准
落地址（在 SGI 中，便用著可以根据所调的 so-locaxion 档。指定每一个 shared
ibrary 的精确位置),亚于编译系统层图上的解决方法,势必得牺粒系本的 vinus
table实现模型所津来的高效率。
Cemmee ORjest Requee Bnsker Ardtinoctere (CORBA) - Cempenent Otject
Model (COM) 。 以及 Sysem Objet Model (SOM) 都全围定文岛分布式。 二进
新层面的对象模型，并儿任何肥序语言无类（调关[MOWBRAY95]对CORBA
的详细讨论，及对 SOMCOM的次要材论，至于C+导向的时论，语见
[IIAM95]  SOM- [aCX9S}  COM, EL及 [VINOS93] 和 [VINOS94] 的
CORBA）。这费考为矿可宠在未案有C++对象模型推往更高的弹性（经由更多
的间接性》。所我们定知主那证要哪上更多的执行期速度与效率。
当我电算N的离求进化（试继型 We premming, lra appicts)
时，持携的C++对象模型，带着它那“高效率”和“C兼容”的特性，可能
会不表累加克域，然到，到了那个时候，对象模型（Object Medel) 将证明 C++的
全面适用性，不论在各式各样的操作系统，各式各样的硬件重动程序，基因工程。
以及我自己目前专注的 3D计算机经图和动通上

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p353_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 354

深度探素 C++ 对象要型 (usuie Thle C++ Oljer ModerI)

课注：如果患对于 Componm Otject Model （COM）感兴通。我推非河本原
想的书糖,本是 Esemtiel COM (Don Box / Addisos Weslcy / 1998) 。 另本是
de COM (Dslc Rogerson /Micrsof Pres/ 196) . Esestiaf CO0M B的 1 2 R
靠把软件组件（componcnts）的本质、问题所在、以及 COM的解决之道解释
非常好，骨着读者以一股的、纯椭的C++语言（不借助于任何工具）完成一个
COM程序结构：第3章以后的内容略嫌限置，可辅以baideCOM.uldeCOM
全书清爽菌易，但最好必须先读过EsmtalCOM的前两章，您才会有托实深那
的基吧去损受它。

<!-- 图源: ./深度探索C++对象模型 ([美]Stanley B.Lippman著 侯捷译) (Z-Library)_assets/images/p354_scan.jpg -->


<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 355

<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">[General Information]</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">书名=深度探索C++对象模型</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">作者=[美]Stanley  B.Lippman著  侯捷译</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">页数=320</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">SS号=10459808</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">出版日期=2001年
05月第1版</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">出版社=华中科技大学出版社</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 356

<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">封面</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">书名</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">版权</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">前言</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">目录</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">本立道生（侯捷 译序）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">第0章 导读（译者的话）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">第1章 关于对象（Object Lessons）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">加上封装后的布局成本（Layout Costs for Adding Encapsulation）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">1.1 C++模式模式（The C++ Object Model）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">简单对象模型（A Simple Object Model）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">表格驱动对象模型（A Table-driven Object Model）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">C++对象模型（The C++ Object Model）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">对象模型如何影响程序（How the Object Model Effects Programs）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">1.2 关键词所带来的差异（A Keyword Distinction）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">关键词的困扰</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">策略性正确的struct（The Politically Correct Struct）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">1.3 对象的差异（An Object Distinction）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">指针的类型（The Type of a Pointer）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">加上多态之后（Adding Polymorphism）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">第2章 构造函数语意学（The Semantics of constructors）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">2.1 Default Constructor的建构操作</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">&quot;带有Default Constructor&quot;的Member Class Object</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">&quot;带有Default Constructor&quot;的Base Class</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">&quot;带有一个Virual Function&quot;的Class</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">&quot;带有一个virual Base class&quot;的Class</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">总结</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">2.2 Copy Constructor的建构操作</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Default Memberwise Initialization</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Bitwise Copy Semantics（位逐次拷贝）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">不要Bitwise Copy Semantics！</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">重新设定的指针Virtual Table</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">处理Virtual Base Class Subobject</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">2.3程序转换语意学（Program Transformation Semantics）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">明确的初始化操作（Explicit Initialization）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">参数的初始化（Argument Initialization）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">返回值的初始化（Return Value Initialization）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">在使用者层面做优化（Optimization at the user Level）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">在编译器层面做优化（Optimization at the Compiler Level）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Copy Constructor：要还是不要？</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">摘要</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">2.4 成员们的初始化队伍（Member Initialization List）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">第3章 Data语意学（The Semantics of Data）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">3.1 Data Member的绑定（The Binding of a Data Member）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">3.2 Data Member的布局（Data Member Layout）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">3.3 Data Member的存取</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Static Data Members</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Nonstatic Data Member</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">3.4 &quot;继承&quot;与Data Member</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">只要继承不要多态（Inheritance without Polymorphism）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">加上多态（Adding Polymorphism）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">多重继承（Multiple Inheritance）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">虚拟继承（Virtual Inheritance）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">3.5 对象成员的效率（Object Member Efficiency）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">3.6 指向Data Members的指针（Pointer to Data Members）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">&quot;指向Members的指针&quot;的效率问题</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">第4章 Function语意学（The Semantics of Function）</span>

<span style="font-family:'Times-Roman';font-size:15.00pt;color:#010101">bbs.theithome.com</span>

---

## Page 357

<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">4.1 Member的各种调用方式</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Nonstatic Member Functions（非静态成员函数）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Virtual Member Functions（虚拟成员函数）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">Static Member Functions（静态成员函数）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">4.2 Virtual Member Functions（虚拟成员函数）</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">多重继承下的Virtual Functions</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">虚拟继承下的Virtual Functions</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">4.3 函数的效能</span>
<span style="font-family:'STSong-Light';font-size:28.00pt;color:#000000">4.4 指向Mem