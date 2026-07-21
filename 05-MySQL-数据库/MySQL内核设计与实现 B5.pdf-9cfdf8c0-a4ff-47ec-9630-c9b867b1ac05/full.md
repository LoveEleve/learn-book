# MySQL 内核设计与实现

Design and Implementation of MySQL Kernel

赵景波著

# 图书在版编目（CIP）数据

MySQL内核设计与实现/赵景波著.--北京：机械工业出版社，2025.7.--（数据库技术丛书）.--ISBN978-7-111-78565-1

I. TP311.132.3

中国国家版本馆CIP数据核字第2025NK7093号

机械工业出版社（北京市百万庄大街22号 邮政编码100037）

策划编辑：刘锋

责任编辑：刘锋 章承林

责任校对：李霞 杨霞 景飞

责任印制：刘媛

三河市宏达印刷有限公司印刷

2025年7月第1版第1次印刷

$186\mathrm{mm}\times 240\mathrm{mm}\cdot 22$ 印张·474千字

标准书号：ISBN978-7-111-78565-1

定价：109.00元

电话服务 网络服务

客服电话：010-88361066

010-88379833

010-68326294

机 工 官 网：www.cmpbook.com

机 工 官 博：weibo.com/cmp1952

金书网：wwwgolden-book.com

封底无防伪标均为盗版

## 2018年机械教育服务网

本书是深入探索MySQL内核的权威指南。景波凭借自身丰富的实践经验，从源码层面详细解读了MySQL的各个核心模块，包括其发展历程、架构设计、数据字典、存储引擎以及索引实现等核心内容。无论是对于开发者深入理解MySQL的内部工作机制，还是数据库管理员（DBA）优化数据库的性能，都具有极高的参考价值。本书将带你走进MySQL的内核世界，帮助你在数据库领域的专业技能得到进阶。

——祝海强 腾讯云数据库副总经理

在信息技术的海洋中，数据库管理系统是航海者们不可或缺的罗盘，而MySQL作为其中的明星产品，以其卓越的性能、灵活性和开源特性，赢得了全球开发者的广泛青睐。本书是一部深入探索MySQL内部机制的精品之作，它为我们揭开了MySQL这一开源数据库管理系统的神秘面纱，让我们能够更深入地理解其设计理念和实现原理。

对于DBA、开发者、系统架构师，以及任何对数据库技术充满热情的读者来说，本书都是一本不可多得的好书。它不仅能够帮助你提高对MySQL的理解，还能够指导你在实际工作中做出更加明智的技术决策。在这个数据驱动的时代，掌握数据库的核心技术意味着拥有了开启未来之门的钥匙。

我强烈推荐本书给所有对MySQL内核感兴趣的读者，希望本书能够成为你探索MySQL内核世界的指南，为你在数据库领域的学习和实践之路提供有力支持。愿你在阅读中享受收获知识的乐趣，不断拓宽自己的技术视野。

——王伟 甲骨文（中国）MySQL首席架构师

本书为读者全面解析MySQL的内核设计与实现细节，涵盖了InnoDB存储引擎、并发控制、事务管理等所有核心模块，帮助读者深入理解MySQL的运行机制。无论是数据库新手还是资深开发者，都能从本书中获得宝贵的知识和深刻的洞见，提升对MySQL内部工作机制的

理解，用好数据库、管好数据库。强烈推荐给所有希望深入掌握MySQL的读者。

——张友东 StarRocks TSC Member，镜舟科技 CTO

在数据时代，深入理解数据库管理系统的内部工作原理可以让技术人员在使用和维护数据库的过程中变得游刃有余。本书恰如其分地满足了这一需求，它犹如一把打开MySQL黑盒的钥匙，通过描述MySQL的启动流程和一条SQL语句的执行流程，为读者展现了数据库管理系统的精妙设计与高效实现。从底层存储结构到高级查询优化，从锁机制到事务处理，作者以其渊博的知识和清晰的表达，将复杂的概念转化为易于理解的内容。通过阅读本书，你将能够洞察MySQL的核心细节，理解每一个设计决策背后的原因，从而在实际工作中做出更明智的选择。无论是优化数据库查询语句，还是维护MySQL，本书所提供的见解都将成为你的强大后盾。

——张晋涛 Microsoft MVP, CNCF Ambassador

本书是作者根据自身多年的MySQL实践和开发经验所著，包含大量MySQL原理解析和实践经验，对希望实现MySQL进阶的读者非常有帮助。

——付磊 《Redis 开发与运维》作者

# Forward 推荐序一

MySQL 改变了世界。

1995年，MySQL诞生，恰逢互联网迅猛发展的黄金时代。MySQL以其开源的基因迅速崛起，成为构筑互联网数据世界的坚实基石。

在同一时期，Oracle于1992年发布了其旗舰数据库版本Oracle7，该版本在随后几年几乎统治了商业数据库市场。然而互联网的风起云涌让所有人措手不及，直至1998年，Oracle才在其数据库命名中加入了 $i$ （代表internet)，正式发布了面向互联网的Oracle8i版本。

正是在商业数据库的空档期，MySQL借助互联网的浪潮实现普及，到1998年MySQL3.22版本发布时，LAMP（Linux+Apache+MySQL+PHP）体系已经成为互联网架构的经典组合。

从未有任何一款产品能够在关系数据库的成熟期挑战成功、“登上王座”，而MySQL做到了。

我对MySQL之父Monty（Michael“Monty”Widenius）制定的15分钟原则印象深刻。

为了让用户更顺利地使用MySQL数据库，Monty希望能够让用户在下载MySQL后的15分钟内运行起数据库。正是注重用户体验、快速反馈与解决用户痛点的产品理念，让MySQL好评如潮。据Monty本人回忆，在MySQL发布的最初5年内，他回复了超过3万封邮件来解答用户的问题。

成功往往来自近乎痴迷的超强投入。

景波写作本书，同样源自痴迷与热爱。十年的学习积累，三年的笔耕不辍，这样的旅程乍一想就容易让人放弃，只有深陷其中的痴迷、不顾一切的热爱才能支撑这漫长的旅程，最终写成本书。

写作一本好书，最难的是构思，只有独特的构思才能让读者认识一位作者，并跟随作者的独特视角学习、收获、成长。

本书的核心部分通过一条SQL语句在MySQL中的执行流程，串联起SQL引擎、存储引擎和并发控制，我认为这正是作者的匠心所在。

数据库领域的图灵奖得主迈克尔·斯通布雷克先生曾说，SQL已经成为一门星际语言，它是打开数据库大门的钥匙，这把钥匙牵一发而动全身。深入理解SQL的工作原理，以及由其驱动的数据库内核齿轮运作，就能够真正地洞察MySQL数据库的精髓。我在学习数据库的过程中，也常常通过SQL驱动来推演和思考数据库的工作原理。

据说尼古拉·特斯拉先生具备一项神奇的能力——他在构思或者设计一个机器时，可以在大脑中完成可视化的建模与运行推演，这个过程最终与现实别无二致。

SQL 就是数据库世界中的那根“金手指”。SQL 注入数据库的世界，这个沉寂的空间瞬时就喧闹起来，SQL 背后是优化器解析、内存访问、存储读写、并发控制……如果我们能够在脑海里驱动所有齿轮，让整个系统严丝合缝、圆转如意，那么大概我们也就精通了一个数据库产品。

而景波更进一步，基于MySQL的开源代码，他可以从内核角度将这个精密的数据库仪器分解开来、解读清晰，让MySQL的技术精髓在读者面前一览无遗。

十年积累，三年笔耕，你我读者何其幸哉！

盖国强 云和恩墨创始人

# Foreword 推荐序二

在人类智慧的长河中，有一句名言如璀璨星辰般闪耀：读书的厚度，决定了读者人生的高度。这简洁而深刻的话语，道破了书籍与人生之间千丝万缕的联系。然而，我们不应忽视的是，书的厚度又何尝不是作者的渊博知识与坚韧性格的生动映照。

当我接过景波这本沉甸甸的书稿时，仿佛打开了一扇通往他内心世界的大门。在那一页页书稿之间，我清晰地看到了过去几年间他那辛勤钻研的身影，宛如一位孤独的行者在知识的广袤沙漠中艰难跋涉，却又始终坚定不移。每一个字符、每一段论述，都是他笔耕不辍的见证。那是在疫情肆虐的艰难阶段，当世界仿佛被按下暂停键，当无数人在不安与迷茫中徘徊时，景波却将自己沉浸在知识的海洋里，以坚韧不拔的毅力，用无数个日夜的努力，精心雕琢出了这份珍贵的成果。这份成果，就像一颗在黑暗中熠熠生辉的宝石，凝聚着他的心血与汗水，怎能不让人肃然起敬！

回溯近年，我们不难发现，MySQL在信息技术的舞台上如同一颗耀眼的新星飞速发展。它的产品功能经历了翻天覆地的变化，如同化蛹成蝶一般，展现出全新的面貌。在这样的大背景下，整个行业都在翘首以盼一本能够对MySQL进行深度解析的书，就像在茫茫大海中渴望一座指引方向的灯塔。而景波的这本书，恰似应运而生的及时雨，它的出版可谓恰逢其时！仔细研读本书内容，我们会惊喜地发现，无论是深入MySQL的源码层面，还是从较为浅显易懂的操作角度入手，它都能为广大使用MySQL的同行提供宝贵的指导和帮助。它就像是一把万能钥匙，能够打开MySQL世界中一扇扇知识的大门，让更多的人在探索的道路上少走弯路，从中汲取无尽的智慧和力量，进而在自己的职业生涯中受益无穷。我们由衷地期待本书能够在行业内掀起一股学习和探索的热潮，为MySQL的发展和应用注入新的活力。

# 推荐序三Foreword

非常荣幸推荐本书，它的作者是一位曾在我们公司工作并展示出卓越潜力的年轻才俊。作为一名大学实习生加入我们公司后，他迅速适应工作并深入学习了数据库技术，尤其是在MySQL领域展现出了非凡的专注力和求知欲。他在职业道路上的不断攀升，正是他持续提升自身能力、深入钻研技术的结果。从初入职场时对数据库领域的探索，到后来成为MySQL内核专家，他始终保持着对技术的热情与求知欲。

本书反映了作者在MySQL内核尤其是InnoDB存储引擎方面的深刻理解和实战经验。它系统地讲解了MySQL的内核架构，不仅涵盖了数据库系统的基本操作，更深入探讨了MySQL的高可用性架构和数据一致性保障等高级主题。本书的最大亮点在于，它不仅对MySQL的源码进行了分析，还将复杂的数据库概念通过源码分析和原理图的形式逐一拆解，使读者能够快速掌握MySQL各模块的协作关系和内部工作机制。特别是对InnoDB引擎的文件组织方式、事务日志、并发控制等核心部分的详细讲解，展现了作者深厚的理论基础和丰富的实战经验。

对于那些希望深入理解MySQL内核设计的数据库工程师，以及从事相关研发工作的开发者，本书不仅是一本不可多得的学习指南，也是一部实用的技术参考手册。它可以帮助读者在面对复杂技术难题时，通过源码获得更为全面、深刻的洞察。我相信本书将为数据库技术领域的从业者带来重要的价值和启示。

陈栋 沃趣科技创始人 &CEO

# Forward 推荐序四

在当今的数据驱动 +AI 时代，数据库作为数据存储和管理的核心系统，其性能和可靠性至关重要。随着国产数据库的兴起，MySQL 数据库或兼容 MySQL 协议的数据库在互联网、金融、政务等核心应用中被广泛使用。对于广大的开发人员和数据库管理员来说，深入理解 MySQL 的内核设计是提升数据库应用能力的关键。本书的推出，无疑为我们提供了一本深入探索 MySQL 核心原理的技术指南。

景波是一名数据库老兵，本书凝聚了他多年丰富的实践经验和深入的研究成果。本书全面而系统地介绍了MySQL的内核架构、存储引擎、并发控制、高可用实现等关键方面，在实现原理和源码分析上，内容丰富翔实，逻辑清晰严谨。对于数据库的开发人员来说，阅读本书能够“知其然亦知其所以然”。

在云计算公司，通常会遇到各种客户和业务类型，包括I/O延时敏感、大容量存储、超大事务、大批量库表等应用场景。景波当时作为研发负责人，能够对MySQL从应用层到数据库层，从优化器到存储引擎，从单体数据库到分布式数据库等维度，全面理解问题并分析其根源所在，快速解决了客户业务的痛点问题。这些有血有肉的体验和技术精华都记录在了本书中。

总的来说，这是一本具有很高实践指导意义的书，不仅适合数据库开发人员和管理员阅读，也适合对数据库原理感兴趣的读者学习。通过阅读本书，我们可以深入了解MySQL的内核设计，掌握数据库的核心技术，为开发高效、可靠的数据库应用程序提供有力支持。我相信，本书将会成为广大数据库爱好者和专业人士的必备图书。

余邵在 金山云前数据库负责人

# 推荐序五 Foreword

在当今的数据库领域，我们不得不关注这样一个现象：绝大多数国产数据库是在MySQL体系或者PostgreSQL体系之上研发的产品。这一现状深刻地影响着数据库从业者的学习和发展方向。在这样的大环境下，深入学习并透彻掌握MySQL无疑是上上之策。MySQL作为一款开源数据库，具有广泛的应用和深远的影响力。对于我们来说，掌握好MySQL能够提升自身数据库技能。MySQL就像是一座蕴藏着无尽宝藏的知识矿山，通过学习它，我们可以深入了解数据库的各种功能和特性，从基础的数据存储、查询到复杂的事务处理、性能优化等。这些知识和技能的积累，就如同为我们的职业生涯搭建了坚实的阶梯，让我们在面对日益复杂的数据库工作时能够更加从容自信。而且，这种技能的提升能够直接应用到实际工作当中。无论在小型企业的数据管理场景还是大型公司的海量数据处理场景中，MySQL的知识都能让我们游刃有余地应对各种数据库相关的任务，如设计高效的数据库架构，优化查询语句以提高系统性能，确保数据的安全性和完整性等。

赵景波老师凭借着十年DBA工作经历，以及从数据库内核入手解决问题的独特经验，为我们开启了一扇深入了解InnoDB存储引擎的大门。他深入浅出地剖析了InnoDB存储引擎的方方面面，例如InnoDB存储引擎的技术架构、算法原理、模块调用关系、元数据存储管理机制、索引实现机制、缓冲区管理机制等，每一个环节都如同精密仪器的零部件，相互协作又各自发挥着关键作用。在讲解模块调用关系时，他清晰地阐述了各个模块是如何协同工作的，就像在描绘一幅复杂而有序的流程图，让我们能够直观地看到数据在存储引擎内部的流转路径。这些知识对于从事数据库产品研发的人员来说，是创新和优化产品的有力武器。他们可以根据这些深入的理解，设计出更高效、更稳定的数据库产品，满足市场对于数据处理能力不断增长的需求。对于数据库运维人员而言，这是解决日常问题的宝典。当遇到数据库性能下降、数据丢失等问题时，他们可以透过现象看本质，从存储引擎的底层原理出发，迅速定

位并解决问题，确保数据库系统的稳定运行。对于数据库架构师来说，这些知识有助于他们更好地进行数据建模和设计数据存储架构。他们可以根据InnoDB的特性，构建出更符合业务需求、性能更卓越的数据架构，为企业的数据管理提供坚实的支撑。

金官丁 热璞数据库HotDB创始人&CEO

# 前言

自2014年起，我开始涉足数据库领域的工作，其间遭遇了众多的数据库问题。至2018年，我深刻认识到，唯有深入阅读并调试MySQL的源码，方能使我从容地应对这些挑战。起初，我感到不解的地方有很多，但经过一个月的不懈努力，我开始逐渐掌握了一些技巧。要深入理解MySQL的任何一个模块，都需要以月为单位进行长时间的学习和研究。我曾广泛搜集市面上的相关资料，发现尽管有些资料讲解得相当出色，但其覆盖范围往往有限。因此，我萌生了一个想法：深入阅读和调试MySQL的大部分模块，并将相关经验总结成书，供他人参考。那些阅读过MySQL源码的同行可能清楚，这是一项艰巨的任务，它需要投入巨大的精力。

2020年3月，我与出版社取得联系，并随即开启了写作本书的征程。经过近3年的努力，包括工作日晚上和节假日的不懈创作，书稿终于初具规模。然而，工作的繁重一度让我萌生了放弃出版的念头。在此，我要特别感谢我的妻子，是她坚定的鼓励与督促才使得本书顺利完成。

本书主要聚焦于MySQL的InnoDB存储引擎。InnoDB存储引擎是一个结构复杂的系统，包含数十个模块，本书对每个模块都提供了代码级别的详细解释和易于理解的原理图。此外，本书还涵盖了MySQL的并发控制、高可用主从架构以及强一致性等高级主题。

本书共9章，主要内容如下。第1章详细阐述了MySQL内核的发展历程，并指导读者如何下载MySQL源码包以及搭建调试环境。第2章系统介绍了MySQL内核的整体架构，从Server到InnoDB存储引擎层，几乎涉及了所有内部组件，旨在为读者提供对MySQL架构的初步理解。此外，第2章在结尾部分描述了MySQL的启动流程，将架构中的各个模块相互连接，使读者能够清晰地理解MySQL的工作机制。

接下来，本书通过一条SQL语句在MySQL中的执行流程，详细阐述了各个模块的工作原理。第3章探讨了客户端和服务端之间的交互协议，第4章分析了数据字典的结构，介绍

了在MySQL中表的元数据是如何存储的，以及在执行SQL语句时数据字典信息是如何被访问的。紧接着，本书进入几个至关重要的章。第5章详细介绍了InnoDB存储引擎的架构，包括在执行SQL语句时，InnoDB存储引擎中的缓冲池、双写缓冲区、自适应哈希索引以及后台线程是如何协同工作的。第6章讨论了InnoDB的文件组织方式，解释了SQL查询的数据是如何在文件中组织的。第7章揭示了InnoDB索引的实现机制，阐述了SQL语句是如何在索引上进行数据扫描和插入操作的。

在读者对 SQL 执行过程有所了解之后，第 8 章详尽阐述 MySQL 的并发控制机制，深入探讨了包括事务处理、多版本并发控制（Multi-Version Concurrency Control, MVCC）、锁定机制在内的复杂主题。因此，第 8 章内容极为丰富，体现了 MySQL 内核设计的精华。

第9章介绍了MySQL的高可用性，包括MySQL不同阶段的高可用性发展及其原理，并对MySQL MGR进行了非常详细的介绍。

本书内容主要基于MySQL5.7版本，也包含部分MySQL8.0的相关内容。

# 读者对象

可以肯定的是，多数开发人员与MySQL或多或少有些交集。然而，本书并非为初学者所著，它不能即刻助你解决工作中的具体问题。尽管如此，书中所阐述的理念仍能助你提升架构设计能力，并让你领略到一个设计严谨的系统的精髓。深入理解内部机制有助于你更高效地运用MySQL，并能助你解决一些复杂的技术难题。对于那些致力于成为DBA或从事数据库研发的同人，本书可作为你工作中的参考手册，遇到相关问题时可随时查阅。

# 勘误和支持

必须强调，MySQL 是一个极其复杂的系统。鉴于我的认知水平有限，本书难免存在疏漏或不完善之处。我诚挚地希望读者能够共同参与，进一步完善本书内容。若你在阅读过程中遇到任何问题，欢迎与我联系（zbcxy10@gmail.com）。我将在工作之余与各位探讨交流，深表感谢。

# 致谢

衷心感谢我的技术导师余绍在先生，长期以来，他为我提供了宝贵的机会，使我得以在数据库领域不断深入探索，从最初的运维开发逐步过渡到内核研发。同时，我也感激新浪、

金山云、字节跳动等平台，让我经历了在大规模数据库场景下的一次次磨炼。对在此期间给予我帮助和支持的同事表示诚挚的谢意。

在本书的撰写过程中，众多友人给予了宝贵的协助，对此我深表感激。特别感谢张雨女士，她协助我完成了第3章的内容；潘友飞先生对全书进行了详尽的审阅工作；桑栋先生对InnoDB相关章节提出了宝贵的建议和修改意见。此外，我必须感谢我的妻子王慧玲女士，她一直给予我鼓励、支持和监督，对此我深怀感激之情。

特别感谢为本书撰写推荐的专家：盖国强先生、周彦伟先生、陈栋先生、余邵在先生、金官丁先生、祝海强先生、王伟先生、张友东先生、张晋涛先生、付磊先生。诸位前辈和朋友的认可给了我巨大的信心和勇气，让我感觉这几年的坚持非常有价值。

最后衷心感谢机械工业出版社的编辑团队对全书审校工作的倾力投入！基于工作原因，常需要在深夜及周末沟通，编辑团队总是不辞辛劳、高效严谨地回应，为成书提供了不可或缺的支持。

# Contents 目录

# 本书赞誉

推荐序一

推荐序二

推荐序三

推荐序四

推荐序五

前言

# 第1章 MySQL内核简介

1.1 MySQL 内核历史  
1.2 MySQL内核衍生 2  
1.3 MySQL内核版本  
1.4 MySQL 内核社区  
1.5 开始编译 MySQL 4

1.5.1 下载MySQL源码包  
1.5.2 编译MySQL 9  
1.5.3 使用IDE进行调试 12  
1.5.4 调试技巧 19

1.6 总结· 26

# 第2章 MySQL内核整体架构… 27

2.1 Server层· 28

2.1.1 连接层· 28   
2.1.2 查询优化 30  
2.1.3 参数、状态、 performance_schema 31  
2.1.4 缓存· 31   
2.1.5 日志· 32  
2.1.6 锁 33   
2.1.7 存储过程相关 33   
2.1.8 用户自定义函数 34  
2.1.9 复制层 34   
2.1.10 API层· 34

# 2.2 存储引擎层 35

2.2.1 缓冲池· 36   
2.2.2 重做日志缓冲区· 37  
2.2.3 双写机制 37  
2.2.4 后台线程 37

# 2.3 文件层 39

# 2.4 MySQL启动流程 40

2.4.1 第一阶段· 41  
2.4.2 第二阶段 43  
2.4.3 第三阶段· 50

# 2.5 总结 52

# 第3章 客户端和服务端交互协议 53

3.1 MySQL的连接方式 53

3.1.1 TCP/IP套接字 53  
3.1.2 UNIX域套接字… 54   
3.1.3 命名管道和共享内存… 55

3.2 交互过程· 55

3.2.1 MySQL通信协议… 56   
3.2.2 连接阶段 58  
3.2.3 命令执行阶段 61

3.3 处理连接与创建线程 65

3.3.1 MySQL监听客户端请求… 66   
3.3.2 创建连接线程 67  
3.3.3 THD类… 69

3.4 总结· 71

# 第4章 数据字典 72

4.1 数据字典简介 72

4.1.1 文件层· 74  
4.1.2 InnoDB存储引擎层 79  
4.1.3 MySQL Server 层 83

4.2 .frm文件· 89   
4.3 数据字典的使用 93

4.3.1 创建表· 93  
4.3.2 查询表· 95  
4.3.3 rowid 96

4.4 MySQL 8.0 数据字典… 97

4.4.1 文件存储层 99  
4.4.2 数据字典缓存… 103  
4.4.3 数据字典的使用 110  
4.4.4 SDI 115

4.4.5 原子DDL 121

4.5 总结 124

# 第5章 InnoDB存储引擎 125

5.1 整体架构 125  
5.2 缓冲池 126

5.2.1 总体架构 126  
5.2.2 缓冲池初始化 128  
5.2.3 缓存及淘汰 130   
5.2.4 相关参数 … 131

5.3 插入缓冲区 132

5.3.1 插入缓冲的流程 132  
5.3.2 相关参数 136

5.4 自适应哈希 136

5.4.1 使用自适应哈希查询…… 140  
5.4.2 自适应哈希索引的维护… 140

5.5 重做日志缓冲区 ………………………………………… 140

5.5.1 整体架构· 141   
5.5.2 管理结构 142  
5.5.3 更新语句的流程 143  
5.5.4 重做日志刷盘· 145

5.6 双写机制 146

5.6.1 双写缓冲区管理 147  
5.6.2 数据的可靠性保证 149

5.7 后台线程 149

5.7.1 master 线程 150  
5.7.2 I/O 线程 ..... 151  
5.7.3 刷脏线程 153  
5.7.4 清理线程 154

5.8 总结 156

# 第6章 InnoDB文件组织 157

6.1 数据文件 157

6.1.1 逻辑组织结构概览· 157  
6.1.2 逻辑组织结构管理 … 160  
6.1.3 物理组织结构· 167  
6.1.4 数据文件的更新操作 177

6.2 重做日志文件 ………………………………………… 181

6.2.1 总体架构· 181  
6.2.2 更新操作的重做日志 186

6.3 回滚日志文件 189

6.3.1 总体架构· 189  
6.3.2 回滚日志的管理 192  
6.3.3 更新操作的回滚日志……195

6.4 总结 196

# 第7章 InnoDB索引的实现 197

7.1 索引简介 … 197

7.1.1 B树和 $\mathbf{B}+$ 树 198  
7.1.2 全文索引 199

7.2 索引的结构 … 200

7.2.1 聚簇索引的结构 200  
7.2.2 二级索引的结构… 202  
7.2.3 复合索引的结构… 203

7.3 索引的管理 204

7.3.1 索引在内存中的管理… 204  
7.3.2 索引的加载… 205  
7.3.3 索引的创建… 206

7.4 数据检索· 209

7.4.1 聚簇索引数据检索 209  
7.4.2 二级索引数据检索… 209  
7.4.3 插入数据 209

7.4.4 删除数据· 210  
7.4.5 更新数据 210

7.5 索引分裂和合并 … 211

7.5.1 索引页分裂… 211  
7.5.2 索引页合并 213  
7.5.3 索引页重组· 215

7.6 总结 215

# 第8章 MySQL并发控制 216

8.1 MySQL事务的实现 216

8.1.1 事务的管理… 216  
8.1.2 事务的执行流程… 218  
8.1.3 事务的ACID实现… 220  
8.1.4 MVCC 229   
8.1.5 崩溃恢复流程 231  
8.1.6 组提交 237   
8.1.7 分布式事务 238

8.2 MySQL锁实现 242

8.2.1 简介· 242   
8.2.2 元数据锁 247  
8.2.3 表锁 254  
8.2.4 InnoDB行锁 261   
8.2.5 InnoDB表锁 269  
8.2.6 互斥锁· 272   
8.2.7 读写锁· 275   
8.2.8 锁升级和锁降级 279  
8.2.9 死锁· 281

8.3 总结 283

# 第9章 MySQL高可用实现 284

9.1 MySQL 主从复制 … 284

# XVIII

9.1.1 数据同步流程 285  
9.1.2 binlog 日志详解 … 290  
9.1.3 半同步复制 294  
9.1.4 并行复制 296

# 9.2 组复制 303

9.2.1 总体架构… 303

9.2.2 数据流 307  
9.2.3 MGR Paxos 协议优化 … 325  
9.2.4 MGR冲突检测 328   
9.2.5 MGR流控 330

# 9.3 总结 331

# MySQL 内核简介

本章首先带领读者了解MySQL内核的发展历程。随后，带领读者进行MySQL的下载与编译操作，并进行相应的调试。最后分享笔者在过往多年间所积累的有关调试的宝贵经验。

# 1.1 MySQL内核历史

MySQL 的起源可追溯至 1979 年，当时其作者 Monty 在 TcX 公司任职。他最初开发了一个面向报表的存储引擎，并持续在该公司工作至 1990 年。1990 年，一个客户提出需求，希望该报表存储引擎支持 SQL。面对这一挑战，Monty 决定自主开发 SQL 层，尽管初期成效不佳，但他通过不懈努力，持续优化，最终在 1996 年发布了 MySQL 1.0 版本，初期主要面向内部客户。同年 10 月，MySQL 3.11 版本问世，年底则推出了首个基于 Linux 的 MySQL 版本。

此后，MySQL迅速崛起，被移植至多个平台，并为公司带来了一定的收益。1998年，MySQL发布了多线程版本，并全面支持多种语言的API（Application Program Interface，应用程序接口），标志着其在商用领域的进一步成熟。1995年，MySQLAB公司成立，并在2001年与Sleepycat合作开发了BerkeleyDB引擎（后来著名的InnoDB存储引擎的前身），该引擎支持事务处理，使MySQL在1999年开始支持事务。

进入21世纪，MySQL持续进化。2000年，MySQL整合了ISAM（Indexed Sequential Access Method，索引顺序存取法）引擎，即现在的MyISAM引擎，使得Server（服务器）层与MyISAM的耦合度显著提升。2008年，MySQLAB公司被Sun公司收购；随后在

2009年，Sun公司又被Oracle收购。值得注意的是，Oracle对MySQL的收购早有预兆，早在2005年便收购了InnoDB团队。

自2010年起，MySQL发布了多个重要版本。MySQL5.5将InnoDB设为默认存储引擎；MySQL5.6新增了在线的数据定义语言（DataDefinitionLanguage，DDL）、索引下推（IndexConditionPushdown，ICP）、多范围读取（Multi-RangeRead，MRR）等功能以提升性能；MySQL5.7GA则支持了MySQL组复制（MySQLGroupReplication，MGR）和多元复制等特性。2016年发布的MySQL8.0更是进行了重大重构，包括改进数据字典、支持原子DDL以及引入HashJoin等功能。

2024年MySQL发布了9.0版本，支持使用JavaScript编写存储过程，引入VECTOR向量类型以及移除mysql_native_password插件等。

注意 自MySQL 5.7版本以来，新增功能相对较少，而MySQL的主要工作重心已转向性能优化。

# 1.2 MySQL内核衍生

在不断演进中，MySQL吸引了众多用户群体。然而，鉴于用户需求的多样性、业务场景的差异性以及侧重点的不同，MySQL无法全面覆盖所有用户的具体需求。因此，从MySQL中衍生出了多个分支版本，其中较为流行的如下：

MariaDB：MySQL的创始人Monty在离开Sun公司后维护的一个开源分支，其命名源自Monty的女儿Maria。MariaDB聚焦于性能优化，在性能层面相较于社区版MySQL有显著提升，如引入了Hash Join及Semi Join的优化措施。为防范MariaDB重蹈MySQL的覆辙，陷入过度商业化的困境，Monty创立了一个基金组织来负责其管理。该组织严格遵循非商业原则，资金主要来源于各大公司会员的慷慨赞助，从而确保了MariaDB的持续开源与免费属性。

□ Percona：由Percona公司推出的另一个MySQL开源分支。Percona公司起初专注于咨询业务及数据库周边工具如XtraBackup和Percona Toolkit的开发。因此，Percona分支的主要目标在于提升MySQL的维护与诊断能力，通过提供性能诊断工具、增加参数与命令控制选项以及针对极端场景的性能优化等手段，为用户带来更好的使用体验。此外，Percona还独立维护了一个名为XtraDB的存储引擎，该引擎基于InnoDB进行开发，进一步增强了数据库的性能。

这两个MySQL分支均保持了与MySQL的高度兼容性，确保了业务在这些分支之间的平滑迁移。然而，值得注意的是，高版本的MariaDB可能存在一定程度的兼容性问题。因此，在选择分支版本时，应基于具体的产品需求进行考量。若无明显偏好，建议默认使用官方版本，因其生态系统相对完善，且商业机制的有效运作保障了其可持续发展的能力。

此外，在国内市场，一些大型企业及云服务商也根据自身业务需求，维护了各自的MySQL分支版本。随着近年来数据库国产化需求的日益增长，还涌现出了一批基于MySQL定制开发的创业公司，致力于解决特定行业场景下的数据库问题。

# 1.3 MySQL 内核版本

一款优秀的软件往往依赖于一个健全且可持续的版本管理体系，如同Linux、MySQL等经典软件所展现的那样。它们的版本管理流程虽各有特色，但核心思想一致：通过不同版本的发布来区分功能迭代，同时维护稳定版与开发版。

具体到MySQL内核的版本管理，其规则如下：

□首个数字代表主版本号，这一数字的变动通常伴随着重大新特性的引入，如从5.x跃升至8.x、9.x。主版本号的更迭往往间隔数年，是软件发展历程中的重要里程碑。  
□次位数字为发行级别，当软件新增功能或发生不兼容变更时，此数字随之增加。例如，MySQL 5.6 到 5.7 的过渡即反映了发行级别的提升。每个发行级别内部，通常会经历数十个小版本的迭代。  
□末位数字则代表发行系列内的具体版本号，用于标记小特性的添加与漏洞的修复。例如，MySQL 5.7.19至5.7.20的更新即属于此类范畴。一般而言，此类小版本的发布周期为数月。一旦某个发行序列超出其维护期限，其版本号将不再递增。

至于如何区分MySQL的稳定版与开发版，关键在于理解其发行序列的不同阶段：

- Milestone 阶段，标志着新发行序列的初步亮相，如 MySQL 5.7.1 至 5.7.6。此阶段的版本尚不成熟，建议仅用于实验与测试，避免直接部署于生产环境。  
- Release Candidate 阶段，预示着软件即将达到正式可用（General Availability, GA）状态。例如，MySQL 5.7.7 与 5.7.8 即处于此阶段。此时，软件内部已历经充分测试与修复，稳定性显著提升，但仍可能存在少量待解决的问题。  
□GA阶段，如MySQL5.7.9及后续版本，即表示该发行序列已正式成为稳定可靠的版本，可供用户广泛采用。

注意 自MySQL8.0起，MySQL引入了新的版本模型，并推出了两种类型的版本：

□MySQL创新版，旨在持续更新功能特性，确保用户能够及时体验到软件的最新进展。例如，MySQL8.1.0即被规划为首个创新版本。  
□MySQL长期支持（Long-Term Support，LTS）版，专注于提供必要的修复与稳定性保障，为那些追求稳定运行环境的项目与应用提供有力支持。在MySQL8.0的生命周期内（预计于2026年4月结束），8.0.34及之后的版本将专注于错误修复工作，直至最终成为LTS版本。此举旨在为用户从8.0.x迁移至8.x LTS版本提供充足的时间与便利。

# 1.4 MySQL 内核社区

如果你想加入到MySQL开发中，那么以下是一些有用的资源：

□ MySQL社区论坛（https://mysqlcommunity.slack.com/），在这里你可以讨论各种问题，并且可以找到很多有经验的开发者。  
□ MySQL 官方开发者邮件列表（mysql-dev-subscribe@lists.mysql.com），可以在这里讨论各种问题。  
□MySQL官方支持（https://support.Oracle.com/portal/），可以在这里提交各种问题。  
□ MySQL 漏洞提交（https://bugs.mysql.com/），可以在这里提交各种问题，以及尝试向MySQL 提交漏洞修复代码。

# 1.5 开始编译MySQL

本节旨在引导读者逐步掌握MySQL内核的调试方法。鉴于MySQL内核源码规模庞大，包含上百万行代码，若无恰当的方法，初学者难以顺利入门。因此，本节将系统地介绍从源码下载、编译、初始化到调试MySQL内核的全过程，确保读者能够逐步深入，最终掌握相关技能。

此外，本节还将分享笔者多年在MySQL内核调试方面的宝贵经验，旨在帮助读者规避常见的误区和陷阱，避免在调试过程中走弯路，从而更加高效地掌握MySQL内核的调试技巧。

# 1.5.1 下载MySQL源码包

在调试MySQL之前，先下载MySQL源码包。可以从很多渠道下载MySQL源码，但是这里建议从官方网站进行下载。

# 1. 下载方式

具体操作流程如下：

进入MySQL官方网站下载页面（https://dev.mysql.com/downloads/），如图1-1所示。

# $\odot$ MySQL Community Downloads

. MySQL Yum Repository   
MySQL APT Repository   
. MySQL SUSE Repository

. MySQL Community Server

MySQL NDB Cluster   
MySQL Router   
MySQL Shell   
,MySQLOperator   
. MySQL NDB Operator   
- MySQL Workbench   
- MySQL Installer for Windows

, C API (libmysqlclient)   
Connector/C++   
Connector/J   
Connector/NET  
Connector/Node.js   
- Connector/ODBC   
- Connector/Python   
- MySQL Native Driver for PHP

- MySQL Benchmark Tool   
. Time zone description tables   
Download Archives

![](images/a9f5790c59fa81b519444e15094552e982f00a184957dc9230104ac6927a1ac6.jpg)  
图1-1 MySQL官方网站下载页面

单击MySQL Community Server项，进入MySQL Server下载页面，如图1-2所示。

![](images/38be61d7a009822d7aa4ced6ebaefe9230fe58ca033c0b037ed9eb6d44821ea0.jpg)  
图1-2 MySQLServer下载页面

单击Archives按钮，然后选择版本。本书的写作是以MySQL 5.7.19版本的源码为主的，因此这里建议大家也先选择此版本。Operating System（操作系统）选择Source Code，最后的OS Version（操作系统版本）选择All Operating Systems（Generic）（Architecture Independent），Compressed TAR Archive Includes Boost Headers进行下载，MySQL 5.7.19版本的下载界面如图1-3所示。

![](images/87bad0d2b040c7999cd38e2f4803d4d199397a77f97aefd5468bfb9186657872.jpg)  
图1-3 MySQL5.7.19版本的下载界面

单击 Download 按钮开始下载。

# 2. 目录说明

下载完源码包之后，用解压命令进行解压：

tar -zxf mySql-boost-5.7.19.tar.gz

进入MySQL源码目录中：

cd mysql-5.7.19/

可以看到MySQL目录中有很多目录和文件：

<table><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>33704</td><td>6</td><td>22</td><td>2017</td><td>configure.cmake</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>13832</td><td>6</td><td>22</td><td>2017</td><td>config.h.cmake</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>88</td><td>6</td><td>22</td><td>2017</td><td>VERSION</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>2478</td><td>6</td><td>22</td><td>2017</td><td>README</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>333</td><td>6</td><td>22</td><td>2017</td><td>INSTALL</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>66241</td><td>6</td><td>22</td><td>2017</td><td>Doxyfile-perfschema</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>17987</td><td>6</td><td>22</td><td>2017</td><td>COPYING</td></tr><tr><td>-rw-r--r--@</td><td>1</td><td>zbdba</td><td>staff</td><td>26727</td><td>6</td><td>22</td><td>2017</td><td>CMakeLists.txt</td></tr><tr><td>drwxr-xr-x@</td><td>47</td><td>zbdba</td><td>staff</td><td>1504</td><td>6</td><td>22</td><td>2017</td><td>libevent</td></tr><tr><td>drwxr-xr-x@</td><td>4</td><td>zbdba</td><td>staff</td><td>128</td><td>6</td><td>22</td><td>2017</td><td>libbinlogstandalone</td></tr><tr><td>drwxr-xr-x@</td><td>9</td><td>zbdba</td><td>staff</td><td>288</td><td>6</td><td>22</td><td>2017</td><td>libbinlogevents</td></tr><tr><td>drwxr-xr-x@</td><td>106</td><td>zbdba</td><td>staff</td><td>3392</td><td>6</td><td>22</td><td>2017</td><td>include</td></tr><tr><td>drwxr-xr-x@</td><td>17</td><td>zbdba</td><td>staff</td><td>544</td><td>6</td><td>22</td><td>2017</td><td>extra</td></tr><tr><td>drwxr-xr-x@</td><td>17</td><td>zbdba</td><td>staff</td><td>544</td><td>6</td><td>22</td><td>2017</td><td>dbg</td></tr><tr><td>drwxr-xr-x@</td><td>3</td><td>zbdba</td><td>staff</td><td>96</td><td>6</td><td>22</td><td>2017</td><td>cmd-line-utils</td></tr><tr><td>drwxr-xr-x@</td><td>46</td><td>zbdba</td><td>staff</td><td>1472</td><td>6</td><td>22</td><td>2017</td><td>cmake</td></tr><tr><td>drwxr-xr-x@</td><td>34</td><td>zbdba</td><td>staff</td><td>1088</td><td>6</td><td>22</td><td>2017</td><td>client</td></tr><tr><td>drwxr-xr-x@</td><td>16</td><td>zbdba</td><td>staff</td><td>512</td><td>6</td><td>22</td><td>2017</td><td>BUILD</td></tr><tr><td>drwxr-xr-x@</td><td>18</td><td>zbdba</td><td>staff</td><td>576</td><td>6</td><td>22</td><td>2017</td><td>plugin</td></tr><tr><td>drwxr-xr-x@</td><td>10</td><td>zbdba</td><td>staff</td><td>320</td><td>6</td><td>22</td><td>2017</td><td>packaging</td></tr><tr><td>drwxr-xr-x@</td><td>17</td><td>zbdba</td><td>staff</td><td>544</td><td>6</td><td>22</td><td>2017</td><td>mysys_SSL</td></tr><tr><td>drwxr-xr-x@</td><td>109</td><td>zbdba</td><td>staff</td><td>3488</td><td>6</td><td>22</td><td>2017</td><td>mysys</td></tr><tr><td>drwxr-xr-x@</td><td>19</td><td>zbdba</td><td>staff</td><td>608</td><td>6</td><td>22</td><td>2017</td><td>mysql-test</td></tr><tr><td>drwxr-xr-x@</td><td>21</td><td>zbdba</td><td>staff</td><td>672</td><td>6</td><td>22</td><td>2017</td><td>libservices</td></tr><tr><td>drwxr-xr-x@</td><td>12</td><td>zbdba</td><td>staff</td><td>384</td><td>6</td><td>22</td><td>2017</td><td>libmysqld</td></tr><tr><td>drwxr-xr-x@</td><td>15</td><td>zbdba</td><td>staff</td><td>480</td><td>6</td><td>22</td><td>2017</td><td>libmysql</td></tr><tr><td>drwxr-xr-x@</td><td>32</td><td>zbdba</td><td>staff</td><td>1024</td><td>6</td><td>22</td><td>2017</td><td>zlib</td></tr><tr><td>drwxr-xr-x@</td><td>3</td><td>zbdba</td><td>staff</td><td>96</td><td>6</td><td>22</td><td>2017</td><td>win</td></tr><tr><td>drwxr-xr-x@</td><td>17</td><td>zbdba</td><td>staff</td><td>544</td><td>6</td><td>22</td><td>2017</td><td>vio</td></tr><tr><td>drwxr-xr-x@</td><td>6</td><td>zbdba</td><td>staff</td><td>192</td><td>6</td><td>22</td><td>2017</td><td>unittest</td></tr><tr><td>drwxr-xr-x@</td><td>6</td><td>zbdba</td><td>staff</td><td>192</td><td>6</td><td>22</td><td>2017</td><td>testclients</td></tr><tr><td>drwxr-xr-x@</td><td>13</td><td>zbdba</td><td>staff</td><td>416</td><td>6</td><td>22</td><td>2017</td><td>support-files</td></tr><tr><td>drwxr-xr-x@</td><td>58</td><td>zbdba</td><td>staff</td><td>1856</td><td>6</td><td>22</td><td>2017</td><td>strings</td></tr><tr><td>drwxr-xr-x@</td><td>9</td><td>zbdba</td><td>staff</td><td>288</td><td>6</td><td>22</td><td>2017</td><td>sql-common</td></tr><tr><td>drwxr-xr-x@</td><td>577</td><td>zbdba</td><td>staff</td><td>18464</td><td>6</td><td>22</td><td>2017</td><td>sql</td></tr><tr><td>drwxr-xr-x@</td><td>20</td><td>zbdba</td><td>staff</td><td>640</td><td>6</td><td>22</td><td>2017</td><td>scripts</td></tr><tr><td>drwxr-xr-x@</td><td>30</td><td>zbdba</td><td>staff</td><td>960</td><td>6</td><td>22</td><td>2017</td><td>regex</td></tr><tr><td>drwxr-xr-x@</td><td>4</td><td>zbdba</td><td>staff</td><td>128</td><td>6</td><td>22</td><td>2017</td><td>rapid</td></tr><tr><td>drwxr-xr-x@</td><td>77</td><td>zbdba</td><td>staff</td><td>2464</td><td>6</td><td>22</td><td>2017</td><td>man</td></tr><tr><td>drwxr-xr-x@</td><td>6</td><td>zbdba</td><td>staff</td><td>192</td><td>6</td><td>22</td><td>2017</td><td>Docs</td></tr></table>

```txt
drwxr-xr-x@ 13 zbdba staff 416 6 22 2017 storage  
drwxr-xr-x@ 3 zbdba staff 96 6 22 2017 boost  
drwxr-xr-x@ 7 zbdba staff 224 6 25 13:11 cmake-build-debug 
```

MySQL的重要目录如表1-1所示。

表 1-1 MySQL 的重要目录  

<table><tr><td>目录</td><td>作用</td></tr><tr><td>BUILD</td><td>主要包含一些各平台编译的脚本</td></tr><tr><td>boost</td><td>boost库是一个开源的C++库,提供了许多实用的功能和工具,例如字符串和文本处理、数据结构和算法、图像处理、网络编程等,MySQL强依赖该库</td></tr><tr><td>client</td><td>客户端相关逻辑,例如我们使用的MySQL客户端,在客户端执行一条SQL语句最终发送到服务器进行处理</td></tr><tr><td>cmake</td><td>CMake使用CMakeLists.txt文件来描述项目的构建配置,包括源文件、依赖项、编译选项等。通过CMake方便地在不同平台上进行项目的构建和部署,并且可以灵活地配置项目的属性</td></tr><tr><td>cmd-line-utils</td><td>客户端依赖的一些工具,例如readline、libedit工具</td></tr><tr><td>Docs</td><td>一些文档,主要是ChangeLog</td></tr><tr><td>extra</td><td>包含innodbchecksum、lz4_decompress等工具</td></tr><tr><td>include</td><td>包含所有的头文件</td></tr><tr><td>libbinlogevents</td><td>包含binlog所有event的定义</td></tr><tr><td>libevent</td><td>一个用于事件驱动网络编程的库。它提供了一种高效的方式来处理网络连接、I/O事件、定时器等</td></tr><tr><td>libmysql</td><td>库文件,主要供客户端使用</td></tr><tr><td>libmysqld</td><td>嵌入式MySQL服务端库,可以运行在客户端应用侧,它在5.7.19版本中已经被废弃,在8.0版本中已经被移除</td></tr><tr><td>libservices</td><td>定义了一些函数供动态插件加载使用</td></tr><tr><td>man</td><td>包含MySQL所有命令的使用说明,在安装完MySQL之后,可以使用man mysql来查看</td></tr><tr><td>mysql-test</td><td>包含MySQL的所有测试集,如果修改了MySQL内核的代码,需要运行一下该测试集以确保所有功能正常</td></tr><tr><td>mysys</td><td>MySQL封装的常用系统工具方法,并且支持跨平台,例如实现的string、字符集、内存分配等</td></tr><tr><td>mysys_SSL</td><td>OpenSSL、YaSSL相关封装函数的实现</td></tr><tr><td>package</td><td>用于打包及rpm构建</td></tr><tr><td>plugin</td><td>包含一些插件的实现,例如半同步插件、全文本插件等</td></tr><tr><td>rapid</td><td>包含MySQL Group Replication核心逻辑,就是我们常说的MySQL MGR 集群版</td></tr><tr><td>regex</td><td>包含一些正则表达式的实现</td></tr><tr><td>scripts</td><td>包含一些脚本,例如mysqld-safe.sh用来运行mysqld-safe以启动MySQL或者初始化MySQL</td></tr><tr><td>sql</td><td>这里包含MySQL Server层的大部分实现,连接认证、交互协议、查询优化、主从复制等,MySQL启动入口文件mysqld.cc也在这个目录下</td></tr><tr><td>sql-common</td><td>存放部分客户端和服务端都会用到的代码</td></tr><tr><td>storage</td><td>包含MySQL所有存储引擎的实现,例如MyISAM、InnoDB、CSV、blackhole等</td></tr><tr><td>string</td><td>包含一些字符串处理的行数</td></tr><tr><td>unittest</td><td>包含一些单侧文件</td></tr><tr><td>vio</td><td>MySQL 实现的虚拟 I/O 系统，里面封装了网络 I/O 相关的操作函数</td></tr><tr><td>zlib</td><td>MySQL 实现的压缩算法，用于表压缩、网络传输压缩</td></tr></table>

前面介绍了大概的目录，这里主要根据模块来介绍一些核心文件，方便大家在研究不同的模块的时候快速上手。MySQL的重要模块及其解释如表1-2所示。

表 1-2 MySQL 的重要模块及其解释  

<table><tr><td>模块</td><td>文件路径及作用</td></tr><tr><td>MySQL启动入口</td><td>sql/mysql.cc, MySQL启动入口, 在该文件的 mysql_main 方法中</td></tr><tr><td rowspan="3">MySQL客户端实现</td><td>client/mysql.cc, MySQL客户端启动入口, 在该文件的 main 方法中</td></tr><tr><td>client/mysqldump.c, mysqldump 工具主要逻辑</td></tr><tr><td>client/mysqlbinlog.cc, mysqlbinlog 工具主要逻辑</td></tr><tr><td rowspan="4">MySQL服务端连接认证、协议、网络模块</td><td>sql/conn_handler/connectionhandlermanager.cc, 处理MySQL 连接相关, 为客户端创建一个用户线程</td></tr><tr><td>sql/protocol_classic.cc, MySQL 协议相关逻辑</td></tr><tr><td>sql/net serv.cc, MySQL 网络相关逻辑</td></tr><tr><td>sql/auth/sq.authentication.cc, MySQL 登录认证相关逻辑</td></tr><tr><td rowspan="3">查询优化模块</td><td>sql/sql_OPTimizer.cc, MySQL优化器核心逻辑</td></tr><tr><td>&lt;br/&gt;sql数据分析.cc, MySQL语法解析核心逻辑</td></tr><tr><td>sql/sql_planner.cc, MySQL执行计划核心逻辑</td></tr><tr><td>Server层锁模块</td><td>sql/lock.cc, Server层锁的实现, 例如元数据锁 (Metadata Lock, MDL)</td></tr><tr><td>日志模块</td><td>sql/log.cc, error log、general log 核心逻辑</td></tr><tr><td rowspan="3">存储过程、函数、触发器、事件</td><td>sql/events.cc, MySQL事件相关逻辑</td></tr><tr><td>sql/sp.cc, MySQL存储过程核心逻辑</td></tr><tr><td>sql/table_trigger特派员.cc, 触发器相关逻辑</td></tr><tr><td>MGR模块</td><td>rapid)];plugin/group_replication/src和社会模, MGR 核心逻辑</td></tr><tr><td>主从复制模块</td><td>sql/rpl_slave.cc, 包含主从复制核心逻辑</td></tr><tr><td>半同步复制模块</td><td>plugin/semisync/semisync/master.cc, 半同步核心逻辑</td></tr><tr><td>binlog 日志模块</td><td>libbinlogevents/src/binlog_event.cpp, 包含 binlog 协议核心逻辑</td></tr><tr><td>InnoDB缓冲池</td><td>storage/innobase/include/buf0buf.ic, 包含缓冲池核心逻辑</td></tr><tr><td>InnoDB插入缓冲</td><td>storage/innobase/ibuf/ibuf0ibuf.cc, 包含插入缓存核心逻辑</td></tr><tr><td>InnoDB自适应哈希索引</td><td>storage/innobase/btr/btr0sea.cc, 包含自适应哈希索引核心逻辑</td></tr><tr><td>InnoDB双写机制</td><td>storage/innobase/buf/buf0dblwr.cc, 包含双写机制核心逻辑</td></tr><tr><td rowspan="2">InnoDB重做日志</td><td>storage/innobase/log/log0log.cc, 包含重做日志读写核心逻辑</td></tr><tr><td>storage/innobase/mtr/mtr0log.cc, 重做日志mgr 核心逻辑</td></tr><tr><td>InnoDB回滚日志</td><td>storage/innobase/trx/trx0undo.cc, 包含回滚日志核心逻辑</td></tr><tr><td>InnoDB数据文件</td><td>storage/innobase/fsp/fsp0file.cc, 包含数据文件核心逻辑</td></tr><tr><td>InnoDB锁模块</td><td>storage/innobase/lock/lock0lock.cc, 包含 InnoDB 锁核心逻辑</td></tr><tr><td>InnoDB事务模块</td><td>storage/innobase/include/trx0sys.ic,包含事务模块核心逻辑</td></tr><tr><td>InnoDB索引模块</td><td>storage/innobase/btr/btr0btr.cc,包含索引核心逻辑</td></tr><tr><td>InnoDB数据字典模块</td><td>storage/innobase/dict/dict0crea.cc,包含数据字典核心逻辑</td></tr><tr><td>InnoDB多版本控制</td><td>storage/innobase/read/read0read.cc,包含MVCC核心逻辑</td></tr><tr><td rowspan="3">InnoDB后台线程</td><td>storage/innobase/srv/srv0srv.cc,包含master线程核心逻辑</td></tr><tr><td>storage/innobase/srv/srv0start.cc,包含I/O线程核心逻辑</td></tr><tr><td>storage/innobase/buf/buf0flu.cc,包含刷脏页线程逻辑</td></tr></table>

注意，以上文件是MySQL5.7.19版本的，其他版本的也基本一致，部分文件可能存在出入。

# 1.5.2 编译MySQL

MySQL的编译过程对于初学者而言确实具有一定的复杂性，因其涉及诸多参数与依赖项。本小节旨在详尽指导用户完成MySQL的编译过程，并深入解析其中一些至关重要的编译参数，以帮助用户更好地理解与实施。

编译环境：

```txt
[root@i0j176srbmwqzii8km11rZ mysql-5.7.19]# uname -a Linux i0j176srbmwqzii8km11rZ 3.10.0-1160.83.1.el7.x86_64 #1 SMP Wed Jan 25 16:41:43 UTC 2023 x86_64 x86_64 x86_64 GNU/Linux [root@i0j176srbmwqzii8km11rZ mysql-5.7.19]# cat /etc/redhat-release CentOS Linux release 7.9.2009 (Core) 
```

安装依赖包：

```txt
yum install cmake  
yum -y install ncurses-develop  
yum install bison  
yum -y install gcc-c++ 
```

如果你下载的MySQL源码不包含boost包，则需要下载，其下载地址为https://sourceforge.net/projects/boost/files/boost/1.59.0/boost_1_59_0.zip/download。

安装完依赖包之后开始编译，创建一个 build 目录来存放编译后的文件：

```txt
[root@i0j176srbmwqzii8km11rZ mysql-5.7.19]# mkdir build  
[root@i0j176srbmwqzii8km11rZ mysql-5.7.19]# cd build/  
[root@i0j176srbmwqzii8km11rZ build]# 
```

先执行 cmake:

cmake .. -DCMAKE.install_prefix=/usr/local/mysql-5.7.19 \
-DMYSQL建档= /tmp/mysql.sock \
-DDEFAULT.Charset $\equiv$ utf8 \
-DDEFAULTCOLLATION $\equiv$ utf8_bin \
-DWITHExtra.Charsets:STRING $\equiv$ all

-DWITH_MYISAM_STOREAGEENGINE=1\  
-DWITH_INNOBASE_STOREAGEENGINE=1\  
-DWITH MEMORY STORAGE ENGINE=1\  
-DWITH_READLINE=1\  
-DENABLED_LOCALINFIL $\equiv$ 1\  
-DMYSQL_TCP_PORT $= 3313$ -DMYSQL_DATADIR=/data1/mysql3313\  
-DMYSQL_USER=mysql\  
-DDOWNLOAD_BOOST $\equiv$ 1\  
-DWITH_DEBUG $\equiv$ 1\  
-DWITH_BOOST=/data/tmpm/mysql-5.7.19/boost

执行成功之后会有如下输出：

-- CMAKE-built_TYPE: Debug   
-- COMPILE Definitions: GNU_SOURCE;FILE_OFFSET BITS $= 64$ ;HAVE_CONFIG_H;HAVELIBEVENT1   
-- CMAKE_C Flags: -fPIC-Wall-Wextra-Wformat-security-Wvla-Wwrite-stringsWdeclaration-after-statement-Werror   
-- CMAKE_CXX Flags: -fPIC-Wall-Wextra-Wformat-security-Wvla-Woverloadedvirtual-Wno-unused-parameter-Werror   
-- CMAKE_C LINK FLAGS:   
-- CMAKE_CXX LINK FLAGS:   
-- CMAKE_CFLAGS_DEBUG: -g-fabi-version $= 2$ -fno-omit-frame-pointer-fno-strict-aliasing-DENABLED_DEBUG_SYNC -DSAFE_MUTEX   
-- CMAKE_CXX FLAGS_DEBUG: -g-fabi-version $= 2$ -fno-omit-frame-pointer-fno-strict-aliasing-DENABLED_DEBUG_SYNC -DSAFE_MUTEX   
-- Configuring done   
-- Generating done   
CMake Warning:Manually-specified variables were not used by the project:MYSQL_USERWITH MEMORY STORAGE ENGINEWITH_READLINE   
-- Build files have been written to:/data/tmp/mysq1-5.7.19/build

然后执行编译：

```txt
make 
```

如果CPU数量充足，可以指定并行编译：

```txt
make -j4 
```

编译会执行10多分钟，性能较差的计算机可能需要几十分钟，编译成功之后会有如下输出：

```txt
Building CXX object sql/CMakeFiles/sql.dir/sql_client.cc.o  
[98%] [98%] Building CXX object sql/CMakeFiles/sql.dir/srv_session.cc.o  
Building CXX object sql/CMakeFiles/sql.dir/srv_session_info_Service.cc.o  
[98%] Building CXX object sql/CMakeFiles/sql.dir/srv_sessionservice.cc.o 
```

```txt
[98%] Building CXX object sql/CMakeFiles/sql.dir/mysqld Daemon.cc.o  
Linking CXX static library libsql.a  
[98%] Built target mysqltest_embedding  
[98%] Built target sql  
Scanning dependencies of target pfs_connect_attr-t  
Scanning dependencies of target mysqld  
[100%] Building CXX object sql/CMakeFiles/mysqld.dir/main.cc.o  
Linking CXX executable mysqld  
[100%] [100%] Building CXX object storage/perfschema/unittest/CMakeFiles/pfs_connect_attr-t.dir/pfs_connect_attr-t.cc.o  
Building CXX object storage/perfschema/unittest/CMakeFiles/pfs_connect_attr-t.dir/___/___/sql/sql_builtin.cc.o  
[100%] Building C object storage/perfschema/unittest/CMakeFiles/pfs_connect_attr-t.dir/___/___/mysysstring.c.o  
Linking CXX executable pfs_connect_attr-t  
[100%] Built target mysqld  
Scanning dependencies of target udf_example  
[100%] Building CXX object sql/CMakeFiles/udf_example.dir/udf_example.cc.o  
Linking CXX shared module udf_example.so  
[100%] Built target udf_example  
[100%] Built target pfs_connect_attr-t 
```

然后进行安装，这个过程会把编译好的二进制文件安装到指定的目录中：

```txt
[root@iZ0jl76srbbmwqziii8km11rZ build]# make install 
```

安装成功之后有如下信息输出：

```lua
-- Installing: /usr/local/mysql-5.7.19/mysql-test/../include/restore Strict_mode. inc  
-- Installing: /usr/local/mysql-5.7.19/mysql-test/../include/turn_off_strict mode.inc  
-- Installing: /usr/local/mysql-5.7.19/mysql-test/mtr  
-- Installing: /usr/local/mysql-5.7.19/mysql-test/mysql-test-run  
-- Installing: /usr/local/mysql-5.7.19/mysql-test/lib/My/SafeProcess/my_safe_ process  
-- Up-to-date: /usr/local/mysql-5.7.19/mysql-test/lib/My/SafeProcess/mySAFE_ process  
-- Installing: /usr/local/mysql-5.7.19/mysql-test/lib/My/SafeProcess/Base.pm  
-- Installing: /usr/local/mysql-5.7.19/support-files/mysqlmulti.server  
-- Installing: /usr/local/mysql-5.7.19/support-files/mysql-log-rotate  
-- Installing: /usr/local/mysql-5.7.19/support-files/magic  
-- Installing: /usr/local/mysql-5.7.19/share/aclocal/mysql.m4  
-- Installing: /usr/local/mysql-5.7.19/support-files/mysql.server 
```

然后检查一下最终安装好的二进制文件，进入/usr/local/mysql-5.7.19/bin/目录中：

```txt
[root@i0j176srbmwqzii8km11rZ bin]# cd /usr/local/mysql-5.7.19/bin/ 
```

可以看到MySQL相关的二进制文件及命令行工具都在这个目录中，执行MySQL客户端命令工具：

```txt
[root@iZ0j176srbmwqzii8km11rZ bin]# ./mysql --version  
./mysql Ver 14.14 Distrib 5.7.19, for Linux (x86_64) using EditLine wrapper 
```

到此就编译结束了。下面来重点介绍MySQL的重要编译参数及其说明，如表1-3所示。

表 1-3 MySQL 的重要编译参数及其说明  

<table><tr><td>编译参数</td><td>说明</td></tr><tr><td>CMAKEInstall Prefix</td><td>指定MySQL二进制文件安装位置</td></tr><tr><td>MYSQL牌照</td><td>指定MySQL默认套接字文件位置</td></tr><tr><td>DEFAULT_CHARSET</td><td>指定MySQL默认字符集</td></tr><tr><td>WITH_MYISAM_STORAGEENGINE</td><td>指定是否支持MyISAM存储引擎</td></tr><tr><td>WITH_INNOBASE_STORAGEENGINE</td><td>指定是否支持InnoDB存储引擎</td></tr><tr><td>MYSQLTONPORT</td><td>指定MySQL默认端口号</td></tr><tr><td>MYSQL_DATADIR</td><td>指定MySQL默认数据文件目录</td></tr><tr><td>MYSQL_USER</td><td>指定MySQL用户</td></tr><tr><td>WITH_DEBUG</td><td>指定debug模式,注意,这里由于我们是为了调试,所以需要指定,用于生产环境的不需要指定</td></tr></table>

# 1.5.3 使用IDE进行调试

现在已经完成了MySQL的编译，接下来可以进行调试了。调试可以借助很多种工具，例如GDB（GNU Debugger，GNU调试器）和一些IDE（Integrated Development Environment，集成开发环境）工具等。

这里推荐使用IDE工具，因为调试的时候更加直观。我们可以根据自己的习惯和喜好选择IDE工具，这里使用的是CLion。下面会使用CLion来简单演示如何调试MySQL。

# 1.导入编译好的MySQL

单击 File $\rightarrow$ New CMake Project from Sources 选项，准备导入 MySQL 项目如图 1-4 所示。

然后弹出 Select Directory to Import（选择导入目录）对话框，这里就选择我们刚刚编译完成的 build 目录，如图 1-5 所示。

选择完后单击 OK 按钮，然后弹出 Import CMake Project 对话框，如图 1-6 所示。

单击 Open Existing Project 按钮，最终导入完成。之后 CLion 会加载一段时间，加载完成后就可以准备配置 MySQL 启动参数了。单击 Edit Configurations 选项，如图 1-7 所示。

选择mysqld选项，在右边可以看到一些配置，从这里配置启动参数，如图1-8所示。

在图1-8中的Program arguments文本框中填入对应的启动参数，参数如下：

```lua
--defaults-file=/data/mysql3313/my.cnf  
--basedir=/data/mysql3313  
--datadir=/data/mysql3313/data  
--plugin-dir=/usr/local/mysql-5.7.19/lib/plugins  
--user=mysql  
--log-error=zbdba.err  
--pid-file=zbdba.pid 
```

```txt
--socket=/var/lib/mysql/mysql_3313.sock  
--port=3313
```

注意，配置好了还不能直接启动，因为还没有初始化MySQL。

![](images/5c19a19d8f98ec61ef58dd29160c0aa9eb35bd6e753061bf7411bc98a08d941a.jpg)  
图1-4 准备导入MySQL项目

![](images/eb1b23b546550ec3d19d676eb7d9850166faa09715620ab327b2cf3657977d6e.jpg)  
图1-5 选择build目录

![](images/ad34dbff5c8c6551a1924c4fbe0d85615739e1c97b803c18e46c151c78d06eb7.jpg)  
图1-6 弹出Import CMake Project对话框

![](images/5301a8d9d5c18f25ab7349bbb5fe5862b501e53199f48585c17a5617a2605be6.jpg)  
图1-7 单击EditConfigurations选项

![](images/989f4b76e2f25cabb78ceb3d2c4e25c60084b6c70545eec5b0624f709140152d.jpg)  
图1-8 配置MySQL启动参数截图

# 2. 初始化 MySQL

首先创建MySQL目录：

```txt
[root@i0j176srbmwqzii8km11rZ data]# mkdir mysql13313  
[root@i0j176srbmwqzii8km11rZ data]#  
[root@i0j176srbmwqzii8km11rZ data]# cd mysql13313/ 
```

然后创建一个简单的MySQL配置文件：

```ini
[root@iZ0j176srbmwqzii8km11rZ mysql13313]#  
[root@iZ0j176srbmwqzii8km11rZ mysql13313]# cat my.cnf  
[mysqld]  
# 数据库存储路径  
datadir = /data/mysql13313/data  
# 端口号  
port = 3313  
# 最大连接数  
max.getConnections = 100  
# 连接超时时间  
connect_timeout = 10  
# 等待数据库连接释放的时间  
wait_timeout = 600  
# 日志文件  
log_error = /data/mysql13313-error.log  
# 慢查询日志  
slow_query_log = 1  
slow_query_log_file = /data/mysql13313/slow_query.log 
```

```txt
查询缓存大小  
query_cache_size = 64M  
# 表打开缓存大小  
table_open_cache = 256  
# 线程缓存大小  
thread_cache_size = 8  
# tmpdir = /data/mysql3313/tmp 
```

再创建MySQL的data和tmp目录、错误日志文件：

```txt
[root@i0j176srbmwqzii8km11rZ mysql3313]# mkdir data  
[root@i0j176srbmwqzii8km11rZ mysql3313]# mkdir tmp  
[root@i0j176srbmwqzii8km11rZ mysql3313]# touch /data/mysql3313误差.log  
[root@i0j176srbmwqzii8km11rZ mysql3313]# ls  
data error.log my.cnf slow_query.log tmp 
```

创建MySQL用户：

```txt
[root@i0j176srbmwqzii8km11rZ mysql13313]#useadd mysql [root@i0j176srbmwqzii8km11rZ mysql13313]#groupadd mysql -g mysql 
```

授予MySQL目录的mysql用户权限：

```txt
[root@i0j176srbmwqzii8km11rZ data]# chown -R mysql:mysql mysql3313/  
[root@i0j176srbmwqzii8km11rZ data]# cd mysql3313/  
[root@i0j176srbmwqzii8km11rZ mysql3313]# ls -lrt  
-rw-r--r-- 1 mysql mysql 536 7月 5 15:02 my.cnf  
drwxr-xr-x 2 mysql mysql 4096 7月 5 15:04 data  
drwxr-xr-x 2 mysql mysql 4096 7月 5 15:04 tmp 
```

然后进行初始化：

```shell
[root@iz0jl76srbmwqzii8km11rZ mysql3313]# /usr/local/mysql-5.7.19/bin/mysqld-safe --defaults-file=/data/mysql3313/my.cnf --user=mysql --initialize  
2024-07-05T07:11:36.586942Z mysqld_safe Logging to '/data/mysql3313-error.log'.  
2024-07-05T07:11:36.612018Z mysqld_safe Starting mysqld daemon with databases from /data/mysql3313/data  
2024-07-05T07:11:40.617190Z mysqld_safe mysqld from pid file /data/mysql3313/data/iz0jl76srbmwqzii8km11rZ.pid ended 
```

在初始化完成之后查看日志文件，可以看到初始化的MySQL密码：

```txt
[root@i0j176srbmwqzii8kmllrZ mysql3313]# cat error.log   
2024-07-05T07:11:36.619494Z 0 [Warning] TImeSTAMP with implicit DEFAULT value is deprecated. Please use --explicit_defaultst-for_timestamp server option (see documentation for more details).   
2024-07-05T07:11:37.775855Z 0 [Warning] InnoDB: New log files created, LSN=45790   
2024-07-05T07:11:38.026907Z 0 [Warning] InnoDB: Creating foreign key constraint system tables.   
2024-07-05T07:11:38.097652Z 0 [Warning] No existing UUID has been found, so we assume that this is the first time that this server has been started. Generating a new UUID: d2la1651-3a9d-11ef-8d68-00163e03689c. 
```

2024-07-05T07:11:38.099752Z 0 [Warning] Gtid table is not ready to be used. Table 'mysql.gtid_executed' cannot be opened.

2024-07-05T07:11:38.100462Z 1 [Note] A temporary password is generated for root@localhost: 8Uyge+Wekp30

这表明MySQL已经初始化完成，接下来可以通过IDE来启动。在启动之前，我们在IDE上设置一个断点，断点就设置在MySQL的入口函数中，如图1-9所示。

![](images/fb5ec46bd8437670b163231de14a7ea78780614bea66062453e9a78fa4f1816a.jpg)  
图1-9 设置断点

断点的设置非常简单，单击代码最左侧的空白位置即可。然后单击IDE的调试按钮进行调试，调试按钮如图1-10所示。

断点调试MySQL如图1-11所示。

可以看到MySQL停在了断点上，这个时候就可以一步步地进行调试了。从这里调试可以看到MySQL的整个启动流程。如果想让MySQL直接启动，则单击CLion的继续按钮，如图1-12所示。

在IDE中启动了MySQL后，在终端通过MySQL客户端进行连接：

[root@iZ0j176srbmwqzii8km11rZ build]# /usr/local/mysql-5.7.19/bin/mysql -uroot -p'8Uyqe+Wekp30' -P3313

mysql: [Warning] Using a password on the command line interface can be insecure. Welcome to the MySQL monitor. Commands end with; or \g.

Your MySQL connection id is 3  
Server version: 5.7.19-debug-

Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its affiliates. Other names may be trademarks of their respective owners. Type 'help;' or '\h' for help. Type '\c' to clear the current input statement. mysql>

![](images/0086d06b95aea94c11f52280541df2d53399e47e38b72a3589e08f0a8e232202.jpg)  
图1-10 调试按钮

![](images/d59674cef27120f542175c7ad55825f851fba9b2347521750c3b9b9eb5d659d7.jpg)  
图1-11 断点调试MySQL

![](images/6fbaa3c4527ad163d56d4a7a7849bf8d3abe1797174450a748efcc3ffb86cbe0.jpg)  
图1-12 直接启动MySQL

这里的密码就是刚刚初始化时生成的临时密码。若需调试 SQL 语句的执行流程，推荐在客户端触发 SQL 操作，随后在 IDE 中设置断点以追踪。此操作的前提是，开发者需对 SQL 执行过程中可能调用的函数有所了解。以 SELECT 语句为例，它通常会经过 row_search_mvcc 方法进行处理。因此，用户可尝试在此方法内设置断点，并执行 SQL 语句以观察执行流程。

关于断点的设置位置，则需要开发者逐步熟悉并深入了解源码结构。通过持续阅读和分析源码，开发者将能自然掌握在何处设置断点以获取所需的调试信息。

此外，对于调试的启动方式，除了直接在IDE中启动调试外，CLion等IDE还支持attach模式，即允许开发者在终端启动MySQL服务后，通过IDE连接到特定的MySQL进程上进行调试。此模式同样支持断点的设置与调试，为开发者提供了更多的灵活性和选择。对此感兴趣的读者可进一步深入研究并实践。

# 1.5.4 调试技巧

MySQL是一款拥有上百万行代码的复杂系统，我们掌握一定的调试技巧能够显著提升开发效率。这些技巧是笔者通过不断实践和学习逐步积累而来的，写在这里供广大开发者借鉴参考，减少在探索过程中走不必要的弯路。

起初，笔者主要聚焦于Python脚本及小型项目的编写，当时主要依赖Vim编辑器进行代码编辑，并借助pdb工具进行调试。随着技术的深入，笔者开始涉足Redis的阅读与开发，同样以Vim编辑器为核心工具，但在此基础上引入了更多插件以优化阅读体验，同时

转向使用GDB进行调试。

随后，笔者将学习范围扩展至MongoDB，在此过程中，笔者尝试使用VS Code进行代码阅读与调试，以探索新的开发工具与调试方法。最终，在深入MySQL源码的阅读与开发阶段，笔者进一步尝试了Visual Studio、NetBeans以及CLion等多种IDE，以寻找最适合MySQL源码阅读和调试的工具组合。

# 1. 多线程调试

多线程调试主要分为以下两种情况：

□多线程运行不同的代码。针对多线程运行不同的代码，调试的时候主要跟踪自己的线程，如遇到其他线程的断点触发切换到其他线程，再将线程切换回来即可。GDB下多线程调试命令如下：

```txt
查看所有的线程info thread#切换线程thread3
```

在CLion等IDE下可以直接通过鼠标进行线程的查看和切换。

□多线程运行相同的代码。针对多线程运行相同的代码，调试起来就比较麻烦，当你设置一个断点的时候，可能有多个线程触发，这个时候就会干扰你的调试。可以通过在GDB中设置set schedulero-lockingon命令来强制限定只调试当前线程。

# 2. 调试分布式项目

分布式项目一般会部署多个节点。在处理分布式项目的调试任务时，通常会选取某一节点作为断点调试的焦点。然而，由于分布式系统特有的协议与交互机制，该选定节点有可能面临被剔除或发生异常的风险，从而阻碍我们实施准确调试的进程。针对这一挑战，调试分布式项目主要可遵循以下两大策略：

□Mock或者单元调试。有些项目是自己做好了Mock，我们只需要调试一个节点就几乎能调试其所有的功能。如果没有Mock，我们也可以尝试自己做Mock，不过这需要你对该项目非常熟悉。除了Mock，我们还可以采用单元测试来对每个函数进行调试。不过这会有逻辑不连贯、单侧覆盖不全的问题。  
□打印日志。分布式项目最常用的方式还是打印日志。在一些重要的地方打印日志，就能知道代码的大致执行路径。在厘清整体执行路径后，再在每个方法中打印详细日志即可完成调试。

在调试MySQL的MGR时，就会遇到上述问题。

# 3. 编译优化机制

在调试的时候，打印对应的变量有时会报optimized out的错误，这是因为在编译时指定了优化选项。优化选项是通过 $\mathrm{gcc / g + + }$ 指定的。

- -00（字母O后跟一个零），关闭所有优化选项，也是CFLAGS或CXXFLAGS中没有设置-O等级时的默认等级。这样就不会优化代码，通常不是我们想要的。  
-01，这是最基本的优化等级。编译器会在不花费太多编译时间的情况下试图生成更快、更短的代码。这些优化是非常基础的，但一般这些任务肯定能顺利完成。  
- -O2，-O1的进阶。这是推荐的优化等级，除非你有特殊的需求。-O2会比-O1多启用一些标记。设置了-O2后，编译器会试图在不增大代码体积和占用大量编译时间的前提下提高代码性能。  
- -O3，这是最高、最危险的优化等级。用这个选项会延长编译代码的时间，并且在使用gcc4.x的系统里不应全局启用。自3.x版本以来，gcc的行为已经有了极大的改变。在3.x版本中，-O3生成的代码也只是比-O2快一点点而已，而且在gcc4.x中还未必更快。用-O3来编译所有的软件包将产生体积更大、更耗内存的二进制文件，大大增加编译失败的可能性，或出现不可预知的程序行为（包括错误）。这样做得不偿失，记住，过犹不及。在gcc4.x中使用-O3是不推荐的。  
- Os，这个等级用来优化代码尺寸。它启用了 -O2 中不会增加磁盘空间占用的代码生成选项。这对于磁盘空间极其紧张或者 CPU 缓存较小的计算机非常有用，但也可能产生些许问题，因此软件树中的大部分 ebuild 会过滤掉这个等级的优化。使用 -Os 是不推荐的。

在MySQL中，将所有Makefiles中的O1、O2、O3替换成O0，这样就关闭了优化。其他项目也是这样，这里我们直接在编译时指定debug即可。

# 4. core dump

core dump 的主要思想是在程序崩溃后，通过调试其生成的 core 文件，得知当时程序运行的状态，从而找到触发崩溃的条件。

MySQL core dump 配置如下：

1）打开Linux的core文件配置。

```txt
ulimit -c unlimited 
```

2）添加MySQL的core_file配置（配置在[mysqld]下面)，并重启测试实例。

```ini
[mysqld]   
core_file   
[mysqld_safe]   
core-file-size=unlimited 
```

3）配置suid_dumpable（MySQL通常会以suid方式启动）。

```shell
echo 2 >/proc/sys/fs/suid_dumpable 
```

4）设置core文件存放的目录并设置完全控制权限。

```txt
mkdir /data/core && chmod 777 /data/core && echo "/data/core/core" > /proc/sys/ kernel/core_pattern 
```

5）模拟MySQL的crash场景，执行如下命令。

kill -SEGV `pidof mysqld`

kill 操作执行完成后，终于看到了久违的 core 文件。

找到core文件后执行：

gdb core.1234 /usr/local/mysql-5.7.19/bin/mysqld

# 5. pstack

pstack 是一个功能强大的工具，它具备查看运行中程序所有线程堆栈信息的能力。然而，使用该工具时需要谨慎，因为它在捕获进程堆栈的过程中，会导致进程出现短暂的阻塞现象。特别是在处理拥有大量线程的程序时，这一行为可能会对服务的正常运行产生不良影响。pstack 命令如下：

[root@zbdba mysql-5.7.19]# ps -ef|grep mysql   
root 10620 5674 0 21:23 pts/2 00:00:00 vim sql/mysql.dcc   
root 27575 3841 0 21:32 pts/1 00:00:00 /bin/sh /usr/local/mysql-5.7.19- online/bin/mysqlsafe --defaults-file=/data/mysql3322/my.cnf   
mysql 27960 27575 0 21:32 pts/1 00:00:10 /usr/local/mysql-5.7.19- online/bin/mysqld --defaults-file=/data/mysql3322/my.cnf --basedir=/data/ mysql3322 --datadir=/data/mysql3322/data --plugin-dir=/usr/local/mysql-5.7.19- online/lib/plugins --user=mysql --log-error=zbdba.err --pid-file=zbdba.pid --socket=/var/lib/mysql/mysql_3322.sock --port=3322   
root 72534 3841 0 21:59 pts/1 00:00:00 grep --color $\equiv$ auto mysql [root@zbdba mysql-5.7.19]#   
[root@zbdba mysql-5.7.19]#   
[root@zbdba mysql-5.7.19]#   
[root@zbdba mysql-5.7.19]# pstack 27960   
Thread 31 (Thread 0x7fdb64948700 (LWP 27967)):   
#0 0x00007fdb6d11c53a in sigwaitinfo () from /lib64/libc.so.6   
#1 0x000000000eb865b in timer_notify_thread_func (arg $\equiv$ arg@entry $=$ 0x7ffce7e6be50) at /home/zhaojingbo/online/mysql-5.7.19/mysys/posix_timers.c:77   
#2 0x000000001213131 in pfs_spawn_thread (arg $=$ 0x3cbbb4b0) at /home/zhaojingbo/ online/mysql-5.7.19/storage/perfschema/pfs.cc:2188   
#3 0x00007fdb6e51ee65 in start_thread () from /lib64/libpthread.so.0   
#4 0x00007fdb6d1e388d in clone () from /lib64/libc.so.6   
Thread 30 (Thread 0x7fdb1382c700 (LWP 27972)):   
#o 0x00007fdb6e315644 in _io_getevents_0_4 () from /lib64/libaio.so.1   
#1 0x00000000f6fee7 in LinuxAIOHandler::collect (this $\equiv$ this@entry $=$ 0x7fdb1382bddfO) at /home/zhaojingbo/online/mysql-5.7.19/storage/innobase/os/os0file.cc:25oo   
#2 0x00000000f7085a in LinuxAIOHandler::poll (this $\equiv$ this@entry $=$ 0x7fdb1382bddfO, m1=m1@entry $=$ 0x7fdb1382be9O, m2=m2@entry $=$ 0x7fdb1382beaO, request $\equiv$ request@ entry $=$ 0x7fdb1382bebO) at /home/zhaojingbo/online/mysql-5.7.19/storage/ innobase/os/os0file.cc:2646   
#3 0x00000000f726d8 in os_aio_linux Handler (request $=$ 0x7fdb1382bebO, m2=Ox7fdb1382beaO, m1=Ox7fdb1382be9O, global_segment $\equiv$ O) at /home/zhaojingbo/ online/mysql-5.7.19/storage/innobase/os/osOfile.cc:27O2   
#4 os_aiohandler (segment $\equiv$ segment@entry $=$ O,m1=m1@entry $=$ Ox7fdb1382be9O,

```txt
m2=m2@entry=0x7fdb1382bea0, request=request@entry=0x7fdb1382beb0) at /home/ zhaojingbo/online/mysql-5.7.19/storage/innobase/os/os0file.cc:6259 #5 0x000000000112a32d in fil_aio_wait (segment=segment@entry=0) at /home/ zhaojingbo/online/mysql-5.7.19/storage/innobase/fil/fil0fil.cc:5835 #6 0x0000000010le758 in io_handleer_thread (arg=<optimized out>) at /home/zhaojingbo/ online/mysql-5.7.19/storage/innobase/srv/srv0start.cc:311 #7 0x00007fdb6e5lee65 in start_thread () from /lib64/libpthread.so.0 #8 0x00007fdb6d1e388d in clone () from /lib64/libc.so.6 
```

在MySQL夯住时，或者看MySQL的后台线程时，就会用到上述命令。实际上，通过上述命令就能找到对应的方法，如果想要调试，就可以到对应的方法中加上断点。

# 6. 调试及阅读代码的思路

总体策略是分层次地阅读，遵循由整体至细节的原则。首先明确调用的基本路径，随后深入核心方法的理解，从而在心中构建出一个清晰的框架体系。对于规模庞大的项目，尤为重要的是采取模块化视角进行审视，细致掌握各模块的功能定位及其实现机制，并在此基础上有针对性地提出疑问。随后，应带着这些问题深入源代码中探寻答案，以确保对项目有全面且深入的理解。

# （1）熟悉目录及文件

当我们着手探索一个庞大的开源项目时，首要任务是明确其目录结构及各关键文件的基本功能，以此为基础构建初步的认知框架。这些准备工作将有助于我们在后续的代码阅读与调试过程中保持清晰的思路与方向。MySQL 的目录结构及重要文件已在前文中详尽阐述，此处不再重复说明。

# （2）熟悉具体功能原理

我们应当全面且系统地收集有价值的资料，以深入理解其实现原理。在审视代码之前，务必确保已充分掌握其背后的逻辑与机制，否则在浏览代码的过程中可能会感到困惑不解。

# （3）先看注释再看代码

在一些优秀的项目中，基本上每个方法都会有注释，并且会对每个人参和出参进行说明，例如MySQL的方法：

```txt
从master读取一个事件  
SYNOPSIS  
read_event()  
mysql MySQL 连接  
mi master 连接信息  
suppressWarnings 设置为True的时候，当正常的网络读取超时导致我们尝试重新连接时，我们不想向错误日志中打印任何内容，因为这在空闲服务器中是正常事件。  
RETURN VALUES  
'packet_error' 错误信息  
number 包的长度
```

```txt
static ulong read_event(MYSQL* mysql, Master_info *mi, bool* suppressWarnings) 
```

在看完注释后，我们能大致知道该方法是干什么用的，可以判断要不要继续看下去，如果继续看下去就更容易理解。如果注释没有放在方法前，则可能在头文件定义中。

# （4）跟着主线思路走

在代码调试过程中，我们时常会深陷于代码的执行流程，而偏离了初始的调研目标。这会浪费大量时间，尤其是在面对拥有数百万行代码的庞大项目时。然而，值得注意的是，这些项目的核心逻辑代码量往往远小于整体规模。因此，为了高效推进工作，我们需要聚焦于核心要点，并采用以下策略：

□持续保持对调研初衷的清晰认知，时刻提醒自己此次代码审查的核心目的。  
□精准定位核心方法，对于诸如检查、校验、条件判断等辅助性方法，仅需理解其基本功用即可，无须深入探究其实现细节。  
□一旦识别出核心实现部分，务必记录相应的堆栈信息。此举旨在为后续可能的断点调试工作提供便利，确保能够迅速定位并专注于关键代码段。

# （5）利用参数和返回值快速阅读代码

有时候一个方法可能有上千行，如果跟着代码的逻辑走，可能会花费大量的时间。这时就可以通过入参、出参和返回值来快速定位核心代码。

大部分方法的主要作用其实就是处理人参，然后里面可能调用其他方法再将该参数或者处理过的参数传入该方法中，最终都要么有返回值，要么写到出参中。我们只需要跟踪这些参数和返回值，即可快速找到核心逻辑。

例如MySQL中的page_delete_rec_list_end：

```c
从给定记录开始删除页面上的记录，包括该给定记录本身。但最小记录和最大记录不会被删除。\*/  
void  
page_delete_rec_list_end(  
/*========*/  
rec_t* rec, /*指向页面上记录的指针。\*/  
buf_block_t* block, /*页的缓冲块。\*/  
dict_index_t* index, /*记录描述符。\*/  
uint n_recs, /*!\< in: 要删除的记录数量，如果未知则为无符号长整型未定义值（ULONG Undefined）。*/  
uint size, /*!\< in: 要删除的链末尾记录大小的总和，如果未知则为无符号长整型未定义值（ULONG Undefined）。*/  
mtr_t* mtr) /\!\< in: mtr\*/
```

上述方法是想要删除一条MySQL的记录，可以看到参数里面有一个rec，也就是要删除这条记录，所以我们只需要跟踪rec这条记录即可快速理清楚核心逻辑。

# 7. 利用单元测试进行调试理解

如果遇到无法直接通过常规手段触发特定代码调试的情况，一个可行的策略是转而利

用单元测试来辅助调试过程。

注意 MySQL 的测试框架因其独特性，并不支持此种调试方式。而部分项目则能够采用单元测试作为调试手段。

# 8. 查看代码提交记录

在某些情况下，即便经过多次调试，可能仍难以理解特定的逻辑段。这可能是由于该逻辑段较为复杂或特殊，且缺乏必要的注释说明。为了应对这一难题，可以采用git blame命令来追溯该逻辑段的修改历史，并查找对应的提交记录。通常，提交记录会详细说明此次修改的目的和内容，为理解该逻辑段提供有价值的线索。此外，如果运气较好，我们甚至可能找到对应的MySQL的工作日志，它可能包含对该逻辑段的概要说明或详细设计，从而进一步加深我们的理解。

# 9. 理解底层技术

在探讨基础软件时，我们常会发现它们与操作系统或网络交互紧密，且主要聚焦于内存管理与磁盘操作。因此，深入掌握操作系统的内存管理机制、文件系统架构及CPU工作原理等基础知识，将极有利于我们快速领悟相关开源项目的精髓。

对于Linux等底层操作系统的熟悉，是理解诸如MySQL、Nginx等上层应用不可或缺的基石。例如，对epoll I/O多路复用技术有了深入了解后，再研读Redis、Nginx的代码，会发现其高并发设计的核心正是基于这一技术。同样，对Linux磁盘的同步与异步读写机制有了清晰认识后，MySQL中的同步I/O与异步I/O实现也将不再晦涩难懂。

此外，精通TCP及套接字编程能够使我们更加顺畅地理解数据库客户端与服务器之间的交互过程，进而洞察到各类数据库或基于TCP的应用在底层实现上的共通之处。

除了上述操作系统与网络知识外，我们还需熟练掌握一些常用的数据结构，如MySQL索引中广泛应用的B+树、崩溃恢复机制中的红黑树，以及Redis中采用的跳跃表和基数树等。同时，了解基本的算法也至关重要，如MySQL在索引页中查找数据时所使用的二分查找法等。对这些知识与技能的综合运用，将为我们深入理解并优化各类软件系统提供坚实的支撑。

# 10. 重复阅读

对于无法理解的代码逻辑，一般调试 $3 \sim 5$ 遍基本上就能理解，但是遇到一些晦涩难懂的代码，并且没有注释时，可能需要调试10遍以上才能理解。对于已经理解的代码逻辑，隔一段时间再去看，也会有一些不一样的认识。

# 11. 总结笔记

记录笔记是非常重要的，推荐在笔记中记录你阅读、分析、调试的过程，记录你的问

题以及你收获到了什么，例如：

□核心流程。一定要记录其实现的核心流程，这样方便后续看的时候快速理解。  
□调用关系。记录调用关系，方便后续调试定位。

另外，对一些实现细节可以添加代码注释，方便后续理解。

# 12. 分享和交流

自己的理解有时候不全面，或者有一些误差，跟别人交流后总能查漏补缺或者纠正错误。

# 1.6 总结

自1979年起至今，MySQL内核拥有长达四十余年的发展历程，深远且持久。在此期间，MySQL内核不仅孕育了众多分支版本，还经历了持续不断的迭代与优化。MySQL内核的版本管理体系极为严谨，并辅以一套完善的版本管理流程，确保了产品的稳定性和可靠性。同时，MySQL内核社区氛围极为活跃，非常欢迎各界人士加入到MySQL的开发行列中来，共同推动MySQL的进步与发展。

在阅读本书时，建议准备一套用于查看MySQL源码的环境。这将使你超越绝大多数MySQL用户，并为你从源码层面深入理解MySQL的实现原理提供强有力的支持。

本章详细阐述了从MySQL源码的下载、编译到使用IDE进行调试的全过程。读者只需遵循此流程，即可顺利启动MySQL的调试工作。然而，读者需要具备一定的C/C++编程基础，以便更顺畅地理解和分析源码。

值得注意的是，MySQL的许多逻辑和代码结构相当复杂，即便掌握了调试技巧，也可能难以迅速领悟其精髓。面对这种情况，我们倡导采用反复调试的策略，带着问题去深入探索，通过数十次的迭代调试，即便是最复杂的模块，其大部分内容也将逐渐变得清晰可解。

调试MySQL无疑是一项既复杂又耗时的任务，缺乏明确的目标或浓厚的兴趣往往难以持久。笔者之所以能持之以恒地调试MySQL的所有核心逻辑代码长达五年，正是源于对技术的浓厚兴趣，以及工作和写作目标的双重驱动。

# MySQL 内核整体架构

数据库系统展现出高度的复杂性，其中关系数据库的实现尤为显著。本章致力于全面且细致地解析MySQL的整体架构，旨在为后续的深入探讨打下坚实的理论基础。本章中提及的特定模块将在后续章节中进行独立且详尽的论述。MySQL内核整体架构如图2-1所示。

图2-1 MySQL内核整体架构  
![](images/8f58dc4ea556123576878007938690bc102c19138ea6229bbffb19dbe3d9b052.jpg)  
注：DML(Data Manipulation Language)，意为数据操纵语言。

图2-1是直接引用的MySQL官方文档中的图片，从图中可以看到MySQL主要分为三层：

Server层。在MySQL的架构中，Server层扮演着核心角色，涵盖SQL接口管理、SQL语句的解析与优化、缓存管理，除此之外还有连接管理、主从复制等关键功能的实现。这些功能共同确保数据库系统的高效运行。  
□存储引擎层。作为数据操作与管理的基石，存储引擎层专注于执行对数据库表的增、删、改、查等操作。该层将内存中的数据按照预设的数据结构组织，并高效地写入数据文件中，确保数据的持久化存储。MySQL在Server层上，通过统一的接口抽象了存储引擎的功能，使得不同的存储引擎得以灵活实现，并通过插件化的方式动态加载，极大地增强了系统的可扩展性与灵活性。  
□文件层。作为数据存储的最终归宿，文件层负责承载MySQL的系统数据与用户数据。值得注意的是，由于不同存储引擎的设计思想与实现方式存在差异，因此它们在文件存储的内容与格式上也会有所不同。这种设计既体现了MySQL对不同数据存储需求的适应性，也要求用户在使用时需根据实际需求选择合适的存储引擎。

这三层不可或缺，各自承担着不同的职责。在接下来的章节中，我们将对这三层架构进行更为详尽的介绍。

# 2.1 Server层

我们已对Server层的功能有了初步的了解，但需要明确的是，Server层还包含诸多组件，它们协同工作以确保整个系统顺畅运行。Server层根植于MySQL的早期设计之中，其初衷在于与MyISAM表引擎保持兼容性。尽管Server层不直接涉及数据的组织和存储，但它承担着与客户端进行交互、优化查询处理、管理主从复制以及与存储引擎进行交互等关键任务。这些模块的实现均较为复杂。MySQLServer层的整体架构如图2-2所示。

下面简单介绍 Server 层中每个组件的作用。

# 2.1.1 连接层

连接层级的核心职责在于与客户端交互。此层级集成了多个关键模块，包括专注于网络监听的实现以及数据包的发送与接收的vio模块，用于确保数据交互的安全性与合规性的登录认证与权限控制模块，以及相应的线程缓存与表缓存机制。

当客户端发起连接请求时，服务器端先通过vio模块与客户端建立稳定的连接通道，随后利用认证模块执行登录认证及权限审核流程，以确保客户端身份的合法性与操作权限的适宜性。

一旦验证通过，系统将为客户端创建一个用户线程（THD），并将其存储在线程缓存中，以便后续快速访问与管理。同时，用户线程所打开的表也将被记录并存储在表缓存中，以提高数据访问的效率与响应速度。

![](images/6883f2272d78d6e9869764d36e4dc25ad56b889f9709ca01ada14c58429a0d57.jpg)  
图2-2 MySQLServer层的整体架构

# 1. vio 模块

vio模块封装着套接字操作的相关方法，例如Linux网络编程中的send方法向网络文件描述符（fd）发送数据包，recv方法监听网络文件描述符接收数据包等，下面介绍vio提供的一些方法：

vio_delete   
vio_read   
vio_write   
vio_keepalive   
vio_io_wait   
vio_shutdown

vio也结合了epollio多路复用技术来支撑高并发场景，这些内容在第3章会详细介绍。

# 2. 登录认证与权限控制

MySQL的登录认证过程是在客户端与服务端之间完成三次握手及一系列校验之后进行的。在此过程中，客户端会向服务端发送一个认证数据包，该数据包包含通过特定认证协议加密的用户名、密码、请求访问的数据库名称等关键信息。目前，MySQL支持的常用认证协议包括mysql_native_password、sha256_password、caching_sha2_password以及ed25519等。服务器在接收到这个认证数据包后，会根据所使用的协议进行解密操作，并将解密后的信息与系统中维护的用户信息进行比对，以确认用户身份是否合法，从而决定认证是否成功。这一过程的详细技术细节将在第3章中深入阐述。

此外，MySQL还提供了对数据库、表以及列级别的精细权限控制机制。在用户尝试访问任何数据库资源之前，系统会首先验证该用户是否具备相应的权限。具体而言，系统会根据用户的请求以及所请求资源的权限设置，进行严格的权限校验。如果用户未获得相应的访问权限，系统将直接返回错误信息，阻止未经授权的访问操作。

# 3. 线程缓存

在MySQL系统中，存在一个精心设计的线程缓存机制，其核心策略是高效复用连接资源。具体而言，该机制通过将创建的用户线程管理对象存储于内存缓存之中，实现对象的快速复用。当系统需要新的用户线程对象时，会直接从缓存中取出已存在的对象再利用，并根据需要执行必要的数据初始化操作。而当用户线程对象不再需要时，系统会将其相关的维护数据清空，并重新放回线程缓存中，以备后续使用。

注意 MySQL 的线程缓存机制在某些特定场景下可能面临内存泄漏的风险。特别是当系统同时开启大量线程，并在这些线程中频繁申请小量内存时，若未能妥善管理这些内存资源的释放，就可能导致在连接关闭后，部分内存仍然被占用而未得到释放。这一现象可能表现为 MySQL 进程的内存占用持续且缓慢地增长，进而可能对系统的稳定性和性能产生不利影响。

# 4. 表缓存

MySQL会在Server层维护一个表缓存，主要缓存表结构信息。表缓存又分为全局级别和会话级别，表缓存属于数据字典的一部分，数据字典在MySQL8.0之后进行了重构优化，详细介绍请参考第4章。

# 2.1.2 查询优化

一条SQL语句在查询优化模块需要经历如下四个流程：

1）通过解析器完成词法及语法解析，解析完成后生成一个执行树。  
(2) 查询缓存中查看是否有满足条件的执行计划, 前提是打开了查询缓存。

3）通过优化器生成逻辑执行计划和物理执行计划。逻辑执行计划主要是关系代数基础上的优化，通过对关系代数表达式进行逻辑上的等价变换，包含一些SQL改写、列裁剪、谓词下推、聚合消除等；物理执行计划是对具体的执行路径进行优化，在统计信息的基础上，通过选择最佳的索引、合适的关联方式（Join）等来估算最低的执行代价。  
4）执行器会负责完成优化器生成的整个执行计划的执行，最终将结果返回给客户端。

# 2.1.3 参数、状态、performance_schema

MySQL 的参数模块，用于平常的参数设置。参数又分为静态参数和动态参数，动态参数可以在 MySQL 运行期间通过 set variables 命令修改。MySQL 内部针对每个参数实现了具体的修改方法，修改之后会在内存中更新全局的参数。不过有时候虽然实现了动态修改，在内部也需要重新读取参数才能生效，比如修改 binlog_format 参数时，需要断开客户端连接并重连，参数才会生效。

MySQL的状态模块，用于收集各项指标状态。MySQL几乎在每个模块都有埋点记录相关的指标信息，可以通过show global status来查看全部状态指标信息。这些指标信息几乎都是递增的，并且只存储在内存中，一旦重启将会重新计数。实际上几乎所有的MySQL监控系统都是从这里采集的数据。

performance_schema 是 MySQL 的一个数据库，里面采集了很多相关的指标数据。上述的 status 在 performance_schema 中就有对应的表，除此之外还有内存使用情况、线程使用情况、io、锁、事件等待等信息。可以通过 performance_schema 中的表来分析线上相关问题，比如内存的使用情况。

# 2.1.4 缓存

Server层设计并维护了多种类型的缓冲区，这些缓冲区的核心目标在于优化SQL语句的执行效率。以下是对每种缓冲区作用的简要概述：

□ join buffer。在涉及join操作的场景中，若join的表未建立相应的索引，系统将采取优化措施，即将这些表的数据暂存于join buffer之中，以此提升join操作的执行效率。join buffer的具体容量由系统参数join_buffer_size进行调控，其作用域限定于每一次独立的join查询过程。因此，在配置该参数时，务必充分考虑业务实际需求及特点，以做出合理的设定。  
□ net buffer。MySQL 客户端和服务端会维护相应的缓冲区，用以缓存发送和接收的数据。具体地讲，每个客户端线程均会被分配一个连接缓冲区以及一个结果集缓冲区，以便高效地处理数据传输和存储。注意这里说的连接 buffer 虽然区分客户端和服务端，不过都是维护在服务端的。上述连接缓冲区与结果集缓冲区的容量主要受 net_buffer_length 和 max AllowedPacket 两个系统参数的控制。其中，初始容

量设置 net_buffer_length 的默认值，即 16KB。在任何情况下，这两个缓冲区的最大容量均不得超过 max AllowedPacket 参数所设定的上限值。

□ myisamsortbuffer。MyISAM维护的排序缓冲区主要用于在repairtable、create index、alter table等场景中对MyISAM索引进行排序操作，其大小由myisamsortbuffer size参数进行精确控制。  
- read buffer。主要用于缓存 MyISAM 存储引擎顺序扫描表的结果，它是线程级别的。同时针对所有存储引擎，在 Order By 排序、批量插入分区、缓存嵌套循环数据等场景下使用。由 read buffer size 参数控制。  
□ key buffer。其主要功能为缓存MyISAM引擎的索引块，旨在通过此种方式加速查询操作。该缓存具有全局粒度，意味着其资源为所有线程所共享，并受到key_buffer size参数的直接控制与管理。  
- read rnd buffer。该特性主要应用于MyISAM引擎中的Order by语句场景。在此场景下，系统直接从read rnd buffer中读取数据，以此来提升Order by语句的执行性能。此功能是以线程为单位进行管理的，其大小由参数read_rnd_buffer_size进行控制。  
□ sort buffer。该机制主要被设计用于在所有存储引擎中处理数据查询排序的场景，其以线程为单位进行运作，并由sort buffer size参数进行精确控制。  
□ sql buffer。由sql_buffer_result参数控制，它会强制MySQL将查询结果集放入一个临时表中。这样做的主要目的是将客户端与实际的表查询操作分离开来，使得客户端可以更快地获取到部分结果，而MySQL可以在后台继续处理查询结果的其他部分，例如进行排序、分组等操作。

# 2.1.5 日志

Server层主要维护如下四种日志：

□全量日志，主要用于问题定位、审计等场景，其启用与否由参数general_log控制。在默认情况下，此功能处于关闭状态，以确保系统性能和资源利用率的优化。  
□ 慢查询日志，用于记录执行时间超过预设阈值的 SQL 语句。其启用与否由 slow_query_log 参数控制。具体而言，当 SQL 语句的执行时间超过 long_query_time 参数所设定的阈值时，这些 SQL 语句将被记录在慢查询日志中，以供后续分析和优化。  
□错误日志，用于在MySQL启动过程中或遇到错误时记录相关信息的工具。观察和分析这些错误日志，可以诊断问题、定位错误原因，并据此采取相应的解决措施。  
□ binlog，MySQL主从同步的媒介是binlog文件，该文件在事务被提交时，负责将SQL语句记录其中。

# 2.1.6 锁

Server层主要维护两种类型的锁：元数据锁和表锁。

元数据锁的主要功能是确保表在并发访问过程中的安全性，能够锁定表结构信息以及表数据，从而避免在并发环境下发生数据冲突或不一致。具体而言，当表被打开时，系统会自动加上元数据读锁，以保护表结构的稳定；而当需要更改表结构时，则会加上元数据写锁，以确保修改操作的原子性和一致性。

注意 元数据锁的作用范围不仅限于表对象，它还能锁定 schema、存储过程以及全局级别的对象，为数据库的整体并发控制和数据一致性提供强有力的支持。从实现层面来看，元数据锁主要是在 MySQL Server 层进行管理和控制的。

表锁指的是针对表对象的锁定机制，起初仅存在表锁，随后引入了元数据锁，目前采用表锁与元数据锁相结合的方式来实现。需要特别注意的是，表锁主要在Server层实现，而部分存储引擎也提供了自身的表锁实现方式。

# 2.1.7 存储过程相关

存储过程相关主要包括：存储过程、函数、触发器和事件。

# 1. 存储过程

这是一组可编程的函数集合，旨在完成特定的功能，它们以 SQL 语句集的形式存在。这些函数集经过编译后创建并存储在数据库中。用户可以通过指定存储过程的名称并提供必要的参数来调用并执行这些存储过程。存储过程的定义及其相关信息被保存在 information_schema.ROTINES 表中，该表采用 InnoDB 作为存储引擎。因此，存储过程的定义实质上是存储在 InnoDB 存储引擎之下的。然而，存储过程的实际实现则位于 Server 层。

# 2. 函数

MySQL具备自定义函数的能力，这些函数能够执行特定的SQL语句，并允许用户传入参数。当这些函数被执行时，其结果将直接反馈给客户端。此外，所有自定义函数的定义均存储在information_schema.Routines表中，以便后续管理与查询。

# 3. 触发器

可以通过创建触发器来监测数据库表的数据操作语言（Data Manipulation Language, DML）操作，例如，利用触发器实现表数据的同步处理。触发器的定义及其相关属性均被系统地存储在 information_schema.TRIGGERS 表中，以便用户查询和管理。

# 4. 事件

MySQL中的定时器功能，类似于Linux系统中的crontab工具，允许用户在事

件中编写 SQL 语句，并设置定时触发机制。这些事件的定义及其相关信息均被存储在 information_schema EVENTS 表中，以便进行管理和查询。

# 2.1.8 用户自定义函数

MySQL 支持多种内置函数，如 sum 和 count 等，这些函数在数据处理和分析中扮演着重要角色。然而，在实际业务场景中，有时需要扩展这些内置函数的功能以满足特定需求。MySQL 在 Server 层定义了一套 UDF（User Define Function，用户定义函数）的实现接口，该接口允许开发者根据需求自定义函数的逻辑。开发者只需遵循这套接口规范，并在接口内部实现自定义函数的具体逻辑。当 MySQL 服务启动时，就会自动加载这些自定义函数，使得它们可以在数据库操作中被调用。

在使用自定义函数之前，还需要通过CREATE FUNCTION语句在MySQL中正式创建这些函数。通过这种方式，开发者可以在MySQL中定制特定的特性和功能，并通过自定义函数来管理这些功能，从而提高数据库操作的灵活性和效率。

# 2.1.9 复制层

MySQL的复制主要分为主从复制和组复制两个大的阶段。

主从复制是在 Server 层实现的，主要有以下模块：

□ binlog dump 线程。在主库侧启动，用于给所有的从库发送 binlog 日志。  
□ slave io 线程。在从库测启动，用于接受主库侧发送过来的 binlog 日志。  
□ slave sql 线程。在从库侧启动，用于解析 binlog 日志并应用到从库。  
□ binlog cache。用于缓冲 binlog 日志，在写入 binlog 文件前会先写入 binlog cache 中，然后再统一触发刷盘到 binlog 文件中。  
□ semi-sync。半同步插件，主要通过在主库事务提交时写 binlog 的流程中控制等待从库接收 binlog 完成从而实现半同步，semi-sync 通过 hook 技术，在主库侧会开启一个 ack receiver 的线程。

组复制作为一种分布式数据强一致同步方案，其核心基于类Paxos协议构建。它的实现机制与semi-sync相似，均通过插件形式实现。具体而言，在事务提交的流程中，利用hook机制来确保binlog已被成功发送至多数派的跟随者节点之后，领导者节点上的事务方能得以提交。这一设计确保了数据在分布式系统中的高度一致性与可靠性。

# 2.1.10 API层

API层是指MySQLServer所定义的与存储引擎相关的接口集合。为了成功实现一个存储引擎，必须遵循Server层所抽象出的这些接口规范，包括但不限于以下示例接口：

□ prepare。由于MySQL架构中明确区分了Server层与存储引擎层，因此事务的提交过程被设计为包含两个阶段：首先是准备（prepare）阶段，其次是存储引擎层的提交（commit）阶段。在准备阶段，主要进行的是Server层的操作，而存储引擎层在这一阶段也可以执行相关的操作，这些操作的具体逻辑可以在prepare阶段得到实现。这样的设计确保了事务的完整性和一致性。  
□commit。事务的最终提交过程实际上发生在存储引擎层面。在提交逻辑中，包括事务状态的修改，例如修改成事务提交状态，然后可以释放事务中一些锁定的资源等。实际上在InnoDB存储引擎中，提交操作就包含前面描述的这些步骤。  
□ rollback。在进行事务回滚操作时，其核心环节在于存储引擎层的有效执行。具体而言，此过程旨在将已写入数据文件的数据恢复到先前的状态版本，相应的逻辑实现需被严谨地封装于指定方法之内。值得注意的是，Server层生成的binlog一旦完成磁盘写入操作，便无法再行回滚，原因在于Server层本身并未设计包含对binlog进行回滚的逻辑机制。因此，存储引擎层须具备兼容此逻辑的能力，以应对可能出现的异常情况。以InnoDB存储引擎为例，若在系统完成binlog写入并发生崩溃后重启，系统需自动执行扫描binlog的操作，以确保将相关事务的更改准确地补写回InnoDB存储引擎中，从而保证数据的一致性和完整性。  
□ create。在数据库系统中，实现创建表逻辑是一个严谨且关键的过程。以 InnoDB 存储引擎为例，这一过程涉及对数据字典信息的详尽记录。具体而言，表结构信息被精确无误地存储在数据字典中，以确保数据的完整性和可访问性。随后，为了高效地存储和检索数据，会创建索引结构，这些索引结构专门用于存储具体的数据记录，从而优化数据库的性能和响应速度。整个过程体现了数据库管理系统在数据处理上的严谨性、稳定性和高效性。

上述仅为几个常用的接口示例，对于更广泛的接口需求，建议感兴趣的读者查阅sql/handler.h文件中关于handlerto结构体的详细定义。此外，MySQL的内部文档也是获取此类信息的重要资源，可供参考。

# 2.2 存储引擎层

本节主要介绍InnoDB存储引擎，其架构如图2-3所示。

从图2-3中可以清晰地看到InnoDB存储引擎的两大核心组成部分：上半部分聚焦于内存中的组件；下半部分则涵盖底层的文件结构。接下来，我们将基于图2-3对各个组件进行简要的介绍。

![](images/ace2da745930a0b4dde7c843a91fe36db871acceeb6d59045bc58ced8b042cdb.jpg)  
图2-3 InnoDB存储引擎架构

# 2.2.1 缓冲池

缓冲池是InnoDB存储引擎中的一个至关重要的组件，其核心功能在于缓存数据页、回滚页，插入缓冲以及自适应哈希等关键数据。在日常操作中，无论是读取还是写入，所处理的数据均会先经过缓冲池。该组件负责将磁盘上数据文件中相应的数据页加载至其维护的内存区域中，并借助链表等数据结构对这些数据页进行高效管理。

值得注意的是，除了回滚页缓存和索引页缓存外，InnoDB的插入缓冲和自适应哈希机制也依赖于缓冲池中的内存资源，下面简单地介绍一下。

# 1. 插入缓冲区

插入缓冲区的设计初衷在于优化MySQL数据库中二级索引的随机I/O操作。该机制通过合并原本分散的随机I/O请求，有效降低了随机I/O的频率，从而提升了数据库性能。最初，该缓冲区仅支持insert语句，但随后扩展了对update和delete语句的支持，因

此其名称也由insert buffer变更为change buffer。为便于理解，本章将统一采用“插入缓冲区”这一称谓。

# 2. 自适应哈希

我们知道，MySQL的核心索引机制采用B+树结构，其中聚簇索引的叶子节点负责存储表的具体数据。然而，在应对大规模数据集时，B+树的层级可能显著增加，且各层宽度亦会扩大。具体而言，小规模数据可能仅需两级B+树即可满足需求，而大规模数据则可能需要扩展至四级，这势必影响数据检索的效率，尤其是在数据未驻留于内存时，检索速度将更为迟缓。

为解决上述问题，MySQL引入了哈希表这一数据结构，旨在通过其独特的键值映射特性，实现数据的快速定位。MySQL的策略是将高频访问的数据预先插入哈希表中，从而在后续访问时能够直接从哈希表获取所需数据，此举显著提升了数据读取的性能。

注意 自适应哈希技术虽能显著提升特定场景下的操作效率，但其应用亦受诸多条件限制，并非万能之策。因此，在实际应用中，我们需根据具体场景和需求，审慎评估并选用最为合适的数据访问策略。

# 2.2.2 重做日志缓冲区

重做日志缓冲区是InnoDB用于缓存重做日志记录的区域。在事务提交或后台线程触发时，该缓冲区内的内容将被同步至磁盘上的重做日志文件中，以确保数据的持久性和一致性。

# 2.2.3 双写机制

鉴于磁盘的基本存储单元为512B，而MySQL数据库在进行数据读写操作时，其最小单位通常是一个数据页，该数据页的大小普遍设定为16KB。因此，在数据写入过程中，若遭遇突然断电等异常情况，可能导致数据页仅部分写入，进而引发数据页损坏及数据不一致的问题。

为解决此问题，MySQL引入了双写机制。该机制的核心策略是，在将数据页修改内容直接写入其原始数据文件之前，首先将这些修改内容临时写入一个独立的双写缓冲区文件中。一旦双写操作成功完成，MySQL随后会将这部分已确认无误的数据从双写缓冲区文件同步至其对应的数据文件中。此过程确保了即使发生断电等意外情况，也能通过双写缓冲区文件恢复数据页的一致性，从而有效避免数据损坏问题。

# 2.2.4 后台线程

由于InnoDB存储引擎负责数据的读写、各种特性的正常运转。所以后台开启了很多线程，每个线程负责不同的功能，MySQL后台线程如表2-1所示。

表 2-1 MySQL 后台线程  

<table><tr><td>线程名</td><td>默认数量</td><td>作用</td></tr><tr><td>master线程(srv/master_thread)</td><td>1</td><td>主要做检查点、将重做日志刷盘、插入缓冲区合并、表缓存清理等工作</td></tr><tr><td>读I/O线程(io Handler_thread)</td><td>4</td><td>处理异步读操作,当MySQL发起异步读操作后,完成后读I/O 线程会进行收尾工作,主要检查页是否损坏,是否在双写缓冲区存在。如果是压缩页是否能解压等</td></tr><tr><td>写I/O 线程(io Handler_thread)</td><td>4</td><td>处理异步写操作,当MySQL 发起异步写操作,完成后写I/O 线程会进行收尾工作,主要从 flush 链表中移除对应的被刷完盘的脏页,并且更新双写缓冲区</td></tr><tr><td>插入缓冲I/O 线程(io Handler_thread)</td><td>1</td><td>插入缓冲I/O 线程主要是在插入缓冲合并的时候异步I/O 读取完数据页进行一些收尾工作,它的功能跟读I/O 线程基本一致,给它单独区分出来就是为了不影响原有的读I/O 线程</td></tr><tr><td>日志I/O 线程(io Handler_thread)</td><td>1</td><td>处理日志异步写操作,当MySQL 执行 checkpoint 会异步写入重做日志文件中,完成后日志I/O 线程会进行收尾工作,主要是主动将重做日志文件刷盘,并且更新当前系统中的 checkpoint 号</td></tr><tr><td>page cleaner 调度线程(buf Flush_page_cleaning_coordinates)</td><td>1</td><td>主要用于将缓冲区的脏页写入磁盘中,调度线程负责唤醒工作线程,然后调度线程和工作线程都进行刷脏页操作</td></tr><tr><td>page cleaner 工作线程(buf Flush_page_cleaning-worker)</td><td>3</td><td>主要用于将缓冲区脏页写入磁盘数据文件中</td></tr><tr><td>purge 线程(srv_purge Coordinator_thread)</td><td>1</td><td>主要用于删除标记的数据,调度线程负责找到需要清理的被标记删除的数据,分发给工作线程。工作线程再进行具体的数据删除</td></tr><tr><td>清理工作线程(srv worker_thread)</td><td>3</td><td>主要用于删除标记的数据</td></tr><tr><td>buffer pool dump 线程(buf dump_thread)</td><td>1</td><td>在MySQL 关闭时,将缓冲区中使用的数据页持久化到磁盘上,这里只是记录的页编号而不是具体的数据。在MySQL 启动的时候,再将记录的这些页主动加载到缓冲区中</td></tr><tr><td>buffer pool resize 线程(bufResize_thread)</td><td>1</td><td>主要支持 MySQL 动态修改缓冲区的大小</td></tr><tr><td>全文索引表优化线程(fts优化 thread)</td><td>1</td><td>主要优化带有全文索引的表,优化其被删除的分词,将其从索引中删除</td></tr><tr><td>锁超时检查线程( lock_wait_timeout_thread)</td><td>1</td><td>主要检查锁等待超时,超时时间由innodb_lock_wait_timeout 参数控制,该线程会扫描所有被挂起的用户线程的锁等待信息,发现超过innodb_lock_wait_timeout设置的值,会主动释放锁等待,并给客户端返回报错</td></tr><tr><td>错误监控线程(srv_error_monitor_thread)</td><td>1</td><td>主要用于监控 MySQL 内部互斥锁等待超时,超时后 MySQL 会自动崩溃</td></tr><tr><td>monitor 线程(srv_monitor_thread)</td><td>1</td><td>跟错误监控线程配合使用,主要在MySQL 异常的情况打印InnoDB 存储引擎的监控信息,跟主动执行 show engine innodb status 命令的信息一样</td></tr><tr><td>统计信息收集线程(dist stats_thread)</td><td>1</td><td>主要用于表统计信息的收集</td></tr></table>

上述只是MySQL5.7的线程，随着版本的叠加，MySQL会引入新的线程，并且线程的数量也可能有所变化。我们可以通过pstackMySQL的进程或者调试其启动的逻辑，找到所有的后台线程。

# 2.3 文件层

同样，本节只介绍InnoDB存储引擎相关的文件。

# 1. 重做日志文件

在MySQL数据库中，重做日志扮演着至关重要的角色，其核心功能在于确保数据库操作的原子性和持久性得以实现。具体而言，每当数据发生更新时，相应的操作会被详尽地记录至重做日志文件中。这一流程严格遵循WAL（Write-Ahead Logging，日志先行）机制，即确保在数据文件的实际写入之前，相应的日志记录已先行完成并确认无误。

注意 MySQL 中的重做日志文件虽以物理操作为基础进行记录，但其内容大多呈现为逻辑形式，并不直接映射至具体的物理数据层面，这一特性与 PostgreSQL 等数据库系统存在显著差异。此差异部分归因于 InnoDB 存储引擎的后续引入及其特定设计考量。关于重做日志的详细构成与工作原理，将在后续章节中深入阐述。

# 2. 数据文件

数据文件是用于存储具体数据的媒介，其类型主要分为两大类。第一类是系统数据文件，该类文件专门用于存储MySQL数据库管理系统所需的管理数据，包括但不限于数据字典、双写区以及回滚段等关键信息。第二类是用户数据文件，其主要功能在于存储用户根据自身需求所创建的表的数据内容。关于数据文件的内部组成细节，将在后续的数据文件章节中予以详尽阐述。

# 3.回滚日志文件

回滚日志文件的核心功能在于支持数据的回滚操作以及实现MVCC机制，其核心作用是将操作执行前的数据状态准确地记录至相应的回滚日志文件中。后续章节将深入剖析回滚日志文件的内部结构及组成要素。

# 4. ib_buffer_pool 文件

ib_buffer_pool 主要是在 MySQL 关机的时候用于存储当前缓冲池缓存的数据页信息，在 MySQL 有一个 dump 线程来做这个事情，dump 的内容主要是将最近使用的数据页的表空间 ID 和对应数据页 number 记录到 ib_buffer_pool 文件中，在下次启动的时候会将这些对应的数据页提前加载。

# 2.4 MySQL 启动流程

在深入探讨MySQL的启动流程前，我们已经对Server层与InnoDB层的基本架构有所了解。接下来，我们将聚焦于MySQL在初始化及启动过程中，如何分别针对Server层与InnoDB层的相关组件进行配置与激活。MySQL的启动架构如图2-4所示，本书详细阐述了MySQL如何系统地准备并启动其核心组件。

![](images/f22930fa866d29dde826eb78b58e8a692757883315380d18ba8dbaf6a3219b6c.jpg)  
图2-4 MySQL的启动架构

从图2-4可知，MySQL的启动主要经历三个大的阶段：

□第一阶段：初始化变量、锁、MySQL命令、参数等。  
□第二阶段：初始化各组件、插件，包括各种存储引擎。  
□第三阶段：准备提供服务，初始化ssl、权限、信号量，监听套接字提供服务。

在MySQL的启动流程中，InnoDB存储引擎的启动与初始化被安排在第二阶段进行。作为MySQL的一个重要组成部分，InnoDB存储引擎被视作一个插件。因此，在MySQL系统加载并激活各类插件的过程中，InnoDB存储引擎也随之被加载并启动，进而执行其初始化流程。

MySQL 的启动入口在 mysqld.cc 文件中的 mysqld_main 方法中。

```c
extern int mysqld_main(int argc, char **argv);  
int main(int argc, char **argv)  
{  
    return mysqld_main(args, argv);  
}
```

# 2.4.1 第一阶段

下面重点介绍下第一阶段几个较为重要的步骤。

1）初始化performance_schema的内存结构。

performance_schema本身是一个内存内的数据库系统，专门用于存储MySQL的各类性能指标。自MySQL启动之初，performance_schema即被初始化，旨在持续追踪并记录一系列关键的性能指标数据，代码如下。

```c
#ifndef WIN32
#define WITH_PERFSchema_STOREngine
pre_initize_performance_schema(); 
```

2）初始化MySQLsys线程、锁相关变量。

MySQL 内部封装了一系列系统库，统称为 my_sys，这些库涵盖了文件操作、日志记录、输入输出处理、字符集管理等核心功能。在此阶段，my_sys 的主要任务是初始化后续过程中将要使用的线程及锁相关变量，以确保 MySQL 数据库系统的稳定运行和高效性能。

```c
if (my_init())
{
    sql_print_error("my_init() failed.")
    flush_error_log/messages();
    return 1;
} 
```

3）获取配置文件。

此步骤涉及获取用于启动MySQL服务的特定my.cnf配置文件。根据MySQL的默认行为，它会在一系列预设的目录下搜索此配置文件，例如/etc目录。若用户已明确指定了

配置文件的路径，则MySQL将直接访问并开启该路径下的配置文件，随后将其内容载入内存，以便在后续操作中使用。

```c
if (load_defaults(MYSQL_CONFIG_NAME, load_default_groups, &args, &args)) { flush_error_log/messages(); return 1; } 
```

4）初始化early参数变量。

early参数变量是指在程序或系统启动流程的早期阶段就已被定义并可能随即在后续流程中使用的变量。这些变量通常具有关键性作用，对于确保流程的顺畅进行以及实现预期功能至关重要。例如，在软件初始化过程中设置的skip-grant-tables、help、initialize等参数变量均属于此类，它们为后续的程序执行或数据处理提供了必要的上下文或条件。

```javascript
ho_error= handle_early_options(); 
```

5）初始化MySQL命令。

我们平常会用到多种MySQL命令，例如alter_table、alter_tablespace、alter_user、begin等，该阶段主要初始化这些命令的内存结构。

```txt
init_sql_statement_names(); 
```

6）调整open_files相关参数。

open file limit、max connection、table_cache_size、table_def_size 参数跟 open_files 参数之间存在比例关系，MySQL 会根据 open_files 的大小来调整这些参数的值。

```javascript
adjust_related_options(&requested_open_files); 
```

7）初始化相关模块互斥锁。

在MySQL的内部实现中，广泛采用了互斥锁（Mutex）这一同步机制，以确保多线程环境下数据的一致性和完整性。具体而言，互斥锁被应用于多个关键组件中，包括但不限于日志模块、审计系统以及查询日志等，以保障这些模块在并发访问时能够安全、有序地执行各自的任务。

```txt
init_error_log();  
mysql_audit_initize();  
querylogger.init(); 
```

8）初始化所有参数变量的值。

在上述阶段中，MySQL已经将配置文件读取到内存中，这里会根据参数文件的值和默认值对MySQL的所有参数进行赋值。

```txt
mysql_init_variables mysqld.cc:7019  
init_common_variables mysqld.cc:2657  
mysqld_main mysqld.cc:4556  
main main.cc:25  
__libc_start_main 0x00007f9ae0e1e555  
_start 0x000000000e96959 
```

9）初始化信号。

初始化MySQL进程用到的信号，MySQL会为一些信号设置相关的处理函数。

```c
my_init Signals();  
(void) sigaction(SIGSEGV, &sa, NULL);  
(void) sigaction(SIGABRT, &sa, NULL);  
(void) sigaction(SIGBUS, &sa, NULL);  
(void) sigaction(SIGILL, &sa, NULL);  
(void) sigaction(SIGFPE, &sa, NULL); 
```

10）设置MySQL工作目录。

设置MySQL的工作目录，也是对应的datadir参数。

```c
if (my_setwd(mysql_real_data_home, MYF(MY_WME)) && !opt_help) { sql_print_error("failed to set datadir to %s", mysql_real_data_home); unireg_abort(MYSQLD.AbORT_EXIT); } 
```

11）设置启动用户。

设置MySQL进程的启动用户，一般用户为mysql。

```txt
if ((user_info = check_user(mysqld_user)))  
    set_user(mysqld_user, user_info); 
```

# 2.4.2 第二阶段

本阶段的核心任务是加载MySQL的各项组件，特别是插件部分，此部分尤为关键。在插件的架构中，各类存储引擎得以实现，而InnoDB存储引擎则占据了主导地位。因此，本小节将详尽阐述InnoDB存储引擎的初始化与启动流程。在深入探讨InnoDB存储引擎之前，我们有必要先对其他几个关键组件的初始化流程进行简要的概览。

初始化元数据锁的内存结构。  
□初始化表缓存的哈希表结构。  
初始化 table def cache 哈希表结构。  
初始化查询缓存内存结构。  
□初始化错误日志文件。  
□初始化事务缓存哈希表结构。

初始化键缓存内存结构。  
初始化gtid相关内存结构。

在完成了上述组件的初始化步骤之后，随即进入插件的初始化阶段。值得注意的是，各个插件的初始化过程各具特色，无统一模式可循。以 binlog 插件为例，其初始化的核心在于配置与 binlog 紧密相关的功能方法，为后续操作奠定基础。鉴于本书中涉及的插件种类繁多，无法逐一详尽阐述。因此，此处将聚焦于 InnoDB 存储引擎的初始化过程，如图 2-5 所示。

![](images/1a77898ef83f251f6282ba26edf1935160758dcd26647a828c7714f3ecb61447.jpg)  
图2-5 初始化InnoDB存储引擎

InnoDB存储引擎的初始化过程包含多个阶段，且每个阶段均较为复杂，为便于理解，参考图2-5我们将其概括为以下四个主要阶段：

□准备阶段。在正式执行之前，需要进行一系列的准备工作。首先，设定一个明确的数据目录，方便数据的存储与管理。随后，进行I/O配置，确保系统能够顺畅地进行数据的读写操作。紧接着，创建I/O线程，以并行处理的方式提升数据处理的效

率。最后，初始化缓冲池，用以暂存数据，优化内存使用，加速数据访问。这一系列步骤的严谨执行是系统稳定运行与高效运作的基础。

□初始化阶段。在数据处理流程中，首要步骤是读取数据，这一过程涵盖了打开数据文件以及各类日志文件。随后，系统进入初始化阶段，包括事务系统的启动、数据字典的构建，以及回滚段的创建等关键步骤。至此，InnoDB已具备基础的服务提供能力，能够支持后续的数据操作与事务处理。  
□崩溃恢复阶段。作为InnoDB数据库系统中极为关键的特性，崩溃恢复被独立划分为一个独立且重要的处理环节，因此我们特别将其视为一个独立的阶段进行详细探讨。  
□启动后台线程阶段。在完成崩溃恢复流程后，InnoDB引擎已恢复并正式进入服务状态。然而，还需启动一系列服务于InnoDB周边环境的后台线程，以确保系统的全面运作。

# 1. 准备阶段

首先，初始化目录。为了后续文件读取的顺利进行，MySQL将预先设立data、undo、redo三个目录结构。

```txt
fil_path_to_mysql_datadir = default_path;  
folder_mysql_datadir = fil_path_to_mysql_datadir; 
```

/* 根据从 MySQL 的 .cnf 文件中读取的值来设置 InnoDB 初始化参数。*/

/* 数据文件的默认目录是 MySQL 的数据目录。*/

```txt
srv_data_home = innobase_data_home_dir ? innobase_data_home_dir : default_path; 
```

接下来设置参数。在配置InnoDB存储引擎时，应当依据具体的需求和场景，精确设定各项参数，以确保数据库系统的稳定、高效运行。这些参数包括但不限于：

□sry io capacity，用于调整InnoDB在后台线程中执行I/O操作的能力。  
- Srv_log_buffer_size，用于设定InnoDB日志缓冲区的大小，该缓冲区用于存储即将写入磁盘的日志信息。  
- srv_buf_pool_size，直接关联到InnoDB缓冲池的大小，它是InnoDB用来缓存数据、索引等内容的内存区域。  
□srv_n_read_io Threads和srv_n_write_io Threads，分别用于配置InnoDB执行读I/O操作和写I/O操作的线程数量，以优化并发处理性能。  
□srv_use-doublewrite_buf，一个关键的配置项，用于启用或禁用InnoDB的双写缓冲区功能，该功能旨在提高数据页的写入可靠性。因此，在调整这些参数时，

需要综合考虑系统资源、工作负载以及数据安全性等多方面因素。

```c
srv_io_capacity = svr_max_io_capacity;  
srv_log_buffer_size = (uint) innobase_log_buffer_size;  
srv_buf_pool_size = (uint) innobase_buffer_pool_size;  
srv_n_read_io Threads = (uint) innobase_read_io Threads;  
srv_n_write_io Threads = (uint) innobase_write_io Threads;  
srv_use-doublewrite_buf = (ibool) innobase_use-doublewrite; 
```

在数据库管理中，为了支持后续操作的高效执行，需要构建专用的临时表空间。此空间被广泛应用于临时表的创建，确保常用临时表能够依托此表空间实现数据的临时存储与处理。

```c
srv_dict_tmpfile = os_file_create_tmpfile(NULL);  
if (!srv_dict_tmpfile) {  
return(srv_init_abort(DB_ERROR));  
}  
latex_create(LATCH_ID_SRV_MISCTmpFILE, &srv_misc_tmpfile_mutex);  
srv_misc_tmpfile = os_file_create_tmpfile(NULL); 
```

MySQL数据库系统支持异步I/O操作，这一特性将在后续的章节中进行详尽的阐述。目前，此处的重点仅在于构建与异步I/O相关的内存结构。

```c
if(!os_aio_init(srv_n_read_io Threads,srv_n_write_io Threads,SRV_MAX_N_PENDING_SYNC_IOs)) { 
```

初始化表空间缓存。启动并初始化 fil_system 表空间管理结构，该结构旨在全面管理和维护系统中的所有表空间。

```c
fil_init(srv_file_per_table ? 50000 : 5000, svr_max_n_open_files); 
```

创建I/O线程，提供读写数据文件、日志文件能力。

```c
for (ulint t = 0; t < srv_n_file_io Threads; ++t) { n[t] = t; os_thread_create(io_handler_thread, n + t, thread_ids + t); } 
```

创建 page cleaner 线程，旨在专门负责将脏页有效地刷新到数据文件中，以确保数据的完整性和持久性。

```c
/* 即使在只读模式下，内部表操作也可能会产生刷新任务。*/  
buf Flush_page Cleaner_init();  
os_thread_create(buf Flush_page Cleaner_coordinates, NULL, NULL);  
for (i = 1; i <srv_n_page cleaners; ++i) { os_thread_create(buf Flush_page Cleaner_workers, 
```

```csv
NULL, NULL); } 
```

初始化缓冲池内存结构，主要用于在内存中缓存数据页。

ib::info() $<  <   \text{串}$ "Initializing buffer pool, total size $=$ " << size $<  <   \text{串}$ unit $<  <   \text{串}$ ", instances $=$ " $<  <   \text{串}$ sv_buf_pool_instances << ", chunk size $=$ " $<  <   \text{串}$ chunk_size << chunk_unit; err $=$ buf_pool_init(srv_buf_pool_size, sv_buf_pool_instances);

可以看到这些准备工作就是服务于接下来的打开数据文件等操作，比如创建I/O线程、初始化缓冲池等。

# 2. 初始化阶段

启动数据文件处理流程，将相关必要信息从数据文件中读取并加载至系统内存中，以确保后续能够顺畅地读取与扫描数据文件内容。

```txt
err = svr_sys_space.open_or_create(
    false, create_new_db, &sum_of_new_sizes, &flushed_lsn); 
```

启动并打开 redo 文件，以便在该文件中继续保存和记录后续的 redo 操作。

```javascript
err = open_log_file(&files[i], logfilename, &size); 
```

打开系统表空间数据文件，该文件存储了MySQL数据库所有至关重要的内部核心信息。在此阶段，MySQL首先执行打开操作，以便在后续的步骤中能够顺利读取数据文件中与系统相关的数据页内容。

```txt
fil_open_log_and_system_tablespace_files(); 
```

初始化 Undo 表空间，这里实际上是打开回滚表空间，最终打开对应的回滚日志文件。

```txt
err = svr Undo_tablespaces_init(
create_new_db,
svr Undo_tablespaces,
&svr Undo_tablespaces_open); 
```

初始化 trx_sys 结构，该结构是用于管理事务系统的。

```txt
trx_sys_create(); 
```

初始化数据字典内存结构，后续读取表结构信息将存储在该结构中。

```txt
dict.boot dict0boot.cc:287  
innobase_start_or_create_for_mysql svv0start.cc:2232  
innobase_init ha_innodb.cc:4048  
ha_init_handleerton handler.cc:838  
plugin_initize sqlPlugin.cc:1197  
plugin_init sqlPlugin.cc:1539  
init_server_components mysqld.cc:4036 
```

```txt
mysqld_main mysqld.cc:4673  
main main.cc:25  
__libc_start_main 0x00007f0c96122555  
__start 0x000000000e96959 
```

创建回滚段，用于后续管理回滚记录。

```javascript
/* InnoDB 中存在的回滚段（rsegs）数量由状态变量srv-available_undo_logs给出。要使用的回滚段数量可以通过动态全局变量srvROLLback_segments来设置。*/srv-available_undo_logs = trx_sys_create_rsegs(srv_undob_tablespaces,srvROLLback_segments,srv_tmp_undo_logs);
```

此时，InnoDB相关文件已成功打开，且相关系统已完成初始化，标志着系统已基本具备向用户提供服务的能力。

# 3. 崩溃恢复阶段

崩溃恢复作为InnoDB数据库管理系统中的一个至关重要的特性，其影响力广泛，几乎覆盖了InnoDB的每一个核心组件。鉴于其复杂性和重要性，我们将在后续章节中对其进行详尽而深入的剖析，以确保内容的完整性和准确性。在此，我们仅就崩溃恢复的大致流程进行概括性描述。

该流程的核心在于对redo日志文件和binlog日志文件进行扫描与分析，以识别并判断在系统崩溃时，是否存在尚未完成的事务需要被回滚或提交。这一过程对于确保数据库的一致性和完整性至关重要，是InnoDB实现高效、可靠数据管理不可或缺的一环。

```c
/* 我们总是尝试进行恢复操作，即便数据库是正常关闭的。这是正常的启动流程。*/  
err = recv_recovery_from_checkpoint_start(flushed_lsn);
```

# 4. 启动后台线程阶段

在本阶段，我们主要聚焦于创建一系列相关线程，其各自的功能已在本章先前内容中进行了详尽阐述，故在此不再重复说明。以下仅为相关代码的简要列举，供有兴趣的读者自行探究与学习。

创建 lock_wait_timeout_thread、error_monitor_thread、monitor_thread 线程。

```c
if(!srv_read_only_mode) {  
/* 创建用于监控锁等待超时情况的线程。*/  
os_thread_create(  
lock_wait_timeout_thread,  
NULL, thread_ids + 2 + SRV_MAX_N_IO_THREAD);  
/* 创建用于对长时间等待信号量发出警告的线程。*/  
os_thread_create(  
srv_error_monitor_thread,  
NULL, thread_ids + 3 + SRV_MAX_N_IO_THREAD);  
/* 创建用于打印 InnoDB 监控信息的线程。*/  
os_thread_create(
```

```c
srv_monitor_thread, NULL, thread_ids + 4 + SRV_MAX_N_IO_THREAD); svstart_state_set(SRV_START_STATE_MONITOR); } 
```

创建外键约束系统表。

```c
/* 创建 SYS FOREIGN 和 SYS FOREIGN_COLS 系统表。*/  
err = dict_create_or_check_foreign Constraint_tables();  
if (err != DB_SUCCESS) {  
    return(srv_init_abort(err));  
}
```

创建 SYS_TABLESPACES 系统表。

```txt
/* Create the SYS_TABLESPACES system table */
err = dict_create_or_check_sys_tablespace(); 
```

创建 SYS_VIRTUAL 表。

```txt
/* Create the SYS_VIRTUAL system table */
err = dict_create_or_check_sys_virtual(); 
```

创建 master 线程。

```c
if (!srv_read_only_mode) { os_thread_create( svr/master_thread, NULL, thread_ids + (1 + SRV_MAX_N_IO_THREAD)); svr_start_state_set(SRV_START_STATE MASTER); } 
```

创建 purge 调度和 worker 线程。

if (!srv_read_only_mode &&srv_force_recovery $<$ SRV FORCE_NOBACKGROUND){ os_thread_create( svv_purge_coordinates_thread, NULL, thread_ids + 5 + SRV_MAX_N_IO_THREAD); ut_a(UT_ERR_SIZE(thread_ids) $>5+$ svv_n_purge Threads $^+$ SRV_MAX_N_IO_THREAD); /\*We've already created the purge coordinate thread above.\*/ for $(\mathrm{i} = 1$ ;i $<$ svv_n_purge Threads; $+ + \mathrm{i})$ { os_thread_create( svv worker_thread, NULL, thread_ids $+5+$ i $^+$ SRV_MAX_N_IO_THREAD); } svv_start_wait_for_purge_to_start(); svv_start_state_set(SRV_START_STATE_PURGE); } else{ purge_sys->state $=$ PURGE_STATE_DISABLED;

创建buf dump\dict.stats\optimize FTS线程。

```c
/\*创建bufferpooldump/load线程\*/  
os_thread_create(buf_dump_thread，NULL，NULL);  
/\*创建dict stats gathering线程\*/  
os_thread_create(dist.stats_thread，NULL，NULL);  
/\*创建将会优化全文检索（FTS）子系统的线程。\*/  
fts_optimize_init(); 
```

创建buf resize_thread线程。

```c
/* 创建 buffer_pool resize 线程 */
os_thread_create(buf Resize_thread, NULL, NULL);
```

至此 InnODB 存储引擎全部初始化完成。

# 2.4.3 第三阶段

第三阶段主要是准备提供服务，初始化ssl、权限、信号量，监听套接字提供服务。

首先创建 auto.cnf。在 InnoDB 中，每个实例都需要有一个 UUID（Universally Unique Identifier，通用唯一识别码），这个 UUID 会保存到 auto.cnf 文件中，如果没有该文件，则 InnoDB 会创建一个。

```javascript
/\*每个MySQL实例都应该有一个UUID。如果不存在的话，InnoDB将会自动创建它。\*/if(init_serverauto_options()）{sql_print_error("Initializationof the server'sUUIDfailedbecauseitcould" not be read from the auto.cnf file.If this is a new""server，the initialization failed because it was not""possible to generate a newUUID.）;unireg_abort(MYSQLD.AbORT_EXIT); 
```

接着初始化ssl相关内存结构，加载RSA密钥对，并存储在全局。

```lisp
if (init_SSL()) 
```

然后初始化网络相关对象，例如设置端口，使用ip、port、backlog等配置创建TCP套接字和UNIX套接字对象。

```python
if (network_init()) 
```

创建PID（ProcessIdentifier，进程标识符）文件。

```javascript
/\*将此进程的PID保存到一个文件中。\*/if(!opt.bootstraps)create.pid_file();
```

清理上次创建的临时表。

```python
mysql_rmtmp_tables() 
```

初始化user/db和table/column-level级别的权限。

```txt
acl_init(opt_noacl) grant_init(opt_noacl)
```

初始化自定义函数，从 mysqlfunc 表中读取所有的用户自定义 func。

```c
if(!opt_noacl) { #ifdef HAVE_DOPEN udf_init(); #endif } 
```

初始化 global status。让 show status 可以读取到 all_status_vars 变量，这样用户执行时就可以看到所有的变量了。

```txt
init.status_vars(); 
```

初始化 slave。如果是从库，则会开启 I/O 和 SQL 线程。

```c
if (server_id != 0)  
init(slave(); /* 
```

执行 ddl recovery。在线的 ddl 会将 ddl 日志写到对应的 recovery log 中，重启后通过该方法恢复。

```javascript
execute_ddl_log_recovery(); 
```

初始化 event scheduler。

```txt
if (Events::init(opt_noacl || opt_BOOT strap))  
unireg_abort(MYSQLD_ABORT_EXIT); 
```

创建 signal handler 线程，用于监听 signal。

```txt
start_signal_handle1(); 
```

加载审计插件。

```txt
if (mysql_audit_notify(AUDIT_EVENT(MYSQL=AUDIT_SERVER_STARTUP_STARTUP), (const char **) argv, argc)) unireg_abort(MYSQLD.AbORT_EXIT);
```

创建gtid table压缩线程。

```txt
create_compress_gtid_table_thread 
```

在正式提供服务之前设置 super_read_only 参数，该参数启用之后数据库为只读。

```txt
set superf_read_only_post_init(); 
```

开启套接字监听，如果监听到请求则创建对应的连接，MySQL的启动最终会阻塞在这里。

```javascript
mysqld_SOCKET.acceptor->connection_event_loop(); 
```

至此，MySQL已经正式对外提供服务。

# 2.5 总结

在本章中，我们全面审视了MySQL Server层与InnoDB存储引擎层所涵盖的基本模块。本章仅对各模块的功能进行了简要阐述。然而，在实际应用场景中，这些模块是如何协同工作的？后续的章节将通过多种流程详细阐述，以助你深入理解整体架构。针对核心模块，还将有专门的章节进行详尽解析。

# 客户端和服务端交互协议

连接MySQL数据库的操作，实质上是一种进程间通信过程，其中进程与MySQL数据库实例进行交互。在进程通信的多种机制中，常见的包括管道、命名管道、命名字、TCP/IP套接字以及UNIX域套接字。MySQL数据库所提供的连接手段，从根本上讲均在上述列举的进程通信方式范畴之内。

# 3.1 MySQL的连接方式

接下来将介绍三种常用的连接方式，分别是TCP/IP套接字、UNIX域套接字、命名管道和共享内存。

# 3.1.1 TCP/IP 套接字

MySQL数据库提供了一种普遍适用的连接方式，即套接字方式，此方式在各类系统环境下均被支持，且在日常开发中被广泛应用。它不仅适用于本地数据库的连接需求，同样也能够满足远程连接的场景。本章后续所探讨的通信协议正是建立在此连接方式的基础之上。

这里我们使用Linux的tcpdump命令，初步了解TCP/IP套接字连接的基本原理。首先我们在MySQL服务端开启监听，然后使用远程客户端来连接MySQL数据库，之后断开连接。这时我们来观察服务端监听端口捕获的所有数据包，如图3-1所示。

tcpdump的基本输出遵循一定的格式规范，具体为：先是系统时间，紧随其后的是来源主机的IP地址及端口号，指向符号“>”，再后则是目标主机的IP地址及其端口号，最

后列出数据包的相关参数。在此参数集中，Flag标志位作为关键信息之一，用于明确标识每个数据包的特定类型。

![](images/c9210e476c809d9981c6c7f45f0ec974e43ade5203023cb5215455470a5a3c28.jpg)  
图3-1 tcpdump抓取的MySQL数据包

为便于理解上述数据包类型，这里对常见Flag标志位的含义进行了简明扼要的归纳，如表3-1所示。

特别需要指出的是，Flags [.]实际上用于表示ACK（确认）状态。基于上述信息，我们可以对MySQL服务端监听到的数据包进行深入分析。前三个数据包构成了TCP连接的

表 3-1 Flag 标志位的含义  

<table><tr><td>Flag</td><td>简写</td><td>含义</td></tr><tr><td>FIN</td><td>F</td><td>表示关闭连接</td></tr><tr><td>SYN</td><td>S</td><td>表示建立连接</td></tr><tr><td>RST</td><td>R</td><td>表示连接重置</td></tr><tr><td>PSH</td><td>P</td><td>表示有数据传输</td></tr><tr><td>ACK</td><td>A</td><td>表示响应</td></tr></table>

三次握手过程，这是建立连接的标准流程。而最后三个数据包则代表了TCP连接的断开过程，即四次挥手，但在此处仅捕获到三个包，原因在于在实际的四次挥手过程中，第二次和第三次挥手被合并为一个数据包发送，因此未能分开捕获。至于中间的数据包，则主要涉及客户端与服务端之间的连接握手认证以及命令执行等交互过程，这些具体的协议细节将在后续的MySQL协议章节中做详细阐述。

# 3.1.2 UNIX域套接字

在Linux和UNIX系统环境中，该连接方式可被采用，但其适用条件严格限定客户端与实例必须处于同一台服务器上，以确保其作为最高效的连接途径。实施此方式前，首要步骤是在相应的配置文件中精确指定套接字文件的存放路径，具体格式为：

```txt
--socket=/var/lib/mysql/mysql.sock 
```

随后，当客户端通过此方式建立与服务器的连接时，必须明确使用 -S 参数，并附带正确的套接字文件路径，示例命令格式如下：

mysql -uroot -p123456 -S /var/lib/mysql/mysql.sock

# 3.1.3 命名管道和共享内存

这两种方法均仅限于在 Windows 环境下运行，且要求客户端与 MySQL 实例部署于同一台服务器之上。在采用此方案之前，用户需在相应的配置文件中明确启用此功能，具体步骤为添加对应的配置指令。

□开启命名管道：-shared-memory=on/off。  
□开启共享内存：-enable-named-pipe=on/off。

# 3.2 交互过程

本节进一步阐述MySQL客户端发送请求至服务端的具体流程，以及服务端如何接收并处理这些请求，最后如何将处理结果有效地反馈回客户端。MySQL客户端请求服务端流程如图3-2所示。

![](images/ddf5891131d536347a370adbc5de056c92a77452a3879dbe0dc0000c9bc45ff9.jpg)  
图3-2 MySQL客户端请求服务端流程

客户端发送的请求首先遵循特定的协议格式被封装成一系列数据包，随后通过预设的连接方式被传输至服务端。服务端在接收到这些数据包后，首要步骤是进行协议解码，提取出需要执行的具体指令。完成指令执行后，服务端将执行结果再次按照既定的协议格式封装成数据包，并回传给客户端。

客户端与服务端之间的所有交互均通过MySQL通信协议实现，该协议位于应用层之下、TCP/IP网络层之上。值得注意的是，在不同的交互阶段，以及针对不同的请求命令时，MySQL通信协议会采用相应的不同格式。MySQL客户端登录认证流程如图3-3所示。

以TCP/IP套接字连接方式为例，MySQL客户端与服务端之间的通信流程需遵循一系列

列严谨且标准化的步骤。首先，双方需建立TCP连接，此过程标志着通信准备阶段的开

始。紧接着，进入连接或认证阶段，在这个阶段中，服务器会主动向客户端发送握手初始化消息，作为通信建立的初步确认。客户端在接收到此消息后，会相应地发送一个验证包，以响应服务器的认证请求。随后，服务器将对客户端进行权限验证，验证结果将通过特定消息形式发送回客户端。

若权限认证成功，通信双方即进入命令交互阶段。在此阶段，服务端将处于持续监听状态，接收来自客户端的命令请求。针对每个请求，服务端将执行相应的操作，并将执行结果，包括操作状态、查询结果集等关键信息，准确无误地反馈给客户端。直至客户端主动发起断开连接的请求，整个通信过程方告结束。

![](images/ead26c771fad722ec7ed6d7b35770d5cd9f378f7b1da1d7b25f3784e264aa5e5.jpg)  
图3-3 MySQL客户端登录认证流程

# 3.2.1 MySQL 通信协议

作为数据库操作中的核心机制，MySQL Client Server 通信协议广泛应用于客户端连接、主从复制配置，以及 MySQL 代理服务等场景。此外，对于开发数据库中间件或实现高效数据传输等高级应用来说，深入理解 MySQL 底层的通信协议尤为重要。本小节将聚焦于 MySQL 通信协议如何有效促进客户端与服务器之间的信息交互。首先，我们需要对 MySQL 的基本概念与原理进行必要的回顾与理解。

# 1. 基础类型

如同编程语言中的基本类型概念，MySQL通信协议亦设定了其基本数据类型，其结构相对简洁，仅涵盖两种核心类型：整数型与字符型。对于整数型数据，MySQL协议进一步细化为两种编码方式，即固定长度整数类型（Fixed-Length Integer Type）与长度编码整数类型（Length-Encoded Integer Type），以满足不同的数据传输需求。

□固定长度整数类型：表示定长的无符号整数，具体固定字节数可以是1、2、3、4、8，使用小字节序传输。  
□长度编码整数类型：顾名思义，就是长度编码的整型，用来存储变长的整数，其中

数据所占的字节数不定，由第一个字节约定。长度编码整数类型数据所占的字节数约定如表3-2所示。

表 3-2 长度编码整数类型数据所占的字节数约定  

<table><tr><td>第一个字节值</td><td>后续字节数</td><td>数据范围</td><td>数据说明</td></tr><tr><td>0x00 ~ 0xFB</td><td>0</td><td>[0,251)</td><td>第一个字节值即为真实数据</td></tr><tr><td>0xFC</td><td>2</td><td>[251,216)</td><td>第一个字节后额外2个字节标识数据</td></tr><tr><td>0xCD</td><td>3</td><td>[216,224)</td><td>第一个字节后额外3个字节标识数据</td></tr><tr><td>0xFE</td><td>8</td><td>[224,264)</td><td>第一个字节后额外8个字节标识数据</td></tr></table>

例如，对于整型而言，100的编码被明确地表示为 $0 \times 64$ ，这一表述直观易懂。而针对65536的编码，其呈现为 $0 \times \mathrm{FD} 0 \times 000001$ 。在此，需明确 $0 \times$ 为十六进制数的标识。首个字节FD作为标识，指出该整数数值位于 $2^{16} \sim 2^{24}$ 的范围内。紧随其后的三个字节则具体表示了数值的真实内容。将65536转换为十六进制，得到的是 $0 \times 10000$ 。然而，鉴于采用的是小字节序的存储方式，故而在编码中呈现为 $0 \times 000001$ 。一旦掌握了这种编码规则，后续对于长度编码的字符串类型所采用的类似处理方式，便能够轻松理解。

对于字符型而言，在MySQL协议中，字符型数据支持五种编码方式，具体为：FixedLengthString、NullTerminatedString、VariableLengthString、LengthEncodedString以及RestOfPacketString。这些编码方式确保了字符型数据在MySQL协议中的有效传输和处理。FixedLengthString指固定长度的字符串类型，其中一个具体实例为ERR_Packet，后续将详细进行阐述。此类型字符串长度始终保持为5B。NullTerminatedString类型是我们在数据处理中常遇到一种字符串形式，它以特定的字节值Null（即字节值为00）作为终止符。这种字符串结构确保了数据的完整性和边界的明确性，便于在多种编程环境和数据处理系统中进行高效且准确的读写操作。VariableLengthString是变长字符串类型，字符串的长度由另一个字段决定或在运行时计算，比如一个字符串由Int + Value组成，我们通过计算Int值来获取Value的具体长度。LengthEncodedString即采用长度编码的字符串类型，其前缀为一个整数，该整数为字符串长度的长度编码形式，符合VariableLengthString所规定的Int+Value方式，其中长度的编码方式采用前文所述的长度编码整数类型。RestOfPacketString是包末端的字符串，可根据包的总长度和当前位置得到字符串的长度，实际中并不常用。

# 2. 报文结构

MySQL 客户端或服务器想要发送数据，首先需要遵循以下原则：

每个数据包的大小必须严格限制在 $2^{24}\mathrm{B}$ （即16MB）以内。每个数据包需遵循特定的报文结构进行构建，该报文内部结构如图3-4所示。

报文由消息头和消息体两个核心部分构成。消息头占据固定的4B空间，其设计旨在为后续的数据处理提供必要的信息框架。消息体的长度并非随意设定，而是严格依据消息头内的一个特定长度字段来确定，以确保数据的完整性和准确性。

![](images/91de1b1e87ee39d93ba9e456b219c4d701f313463fd7241a4793ea0a200d2317.jpg)  
图3-4 报文内部结构

在消息头的组成上，前3B承担着标记当前请求实际数据长度值的重要职责，这一设计确保了数据接收方能够准确无误地识别并处理每一条请求中的核心数据内容。而第4B则作为当前请求的序列ID存在，其值从0起始，并随着请求的生成依次递增，这一机制为消息的有序处理提供了坚实的保障，确保了数据在传输和接收过程中的顺序正确性。

对于上述报文结构的详细应用实例及进一步的解析，我们将在后续章节中逐一呈现，以便读者能够更加深入地理解和掌握。

# 3.2.2 连接阶段

连接阶段主要分为握手初始化和登录认证，握手初始化主要是在TCP/IP连接建立之后，主要是校验一些基础信息，例如数据库版本、字符集编码等。登录认证主要是验证客户端发送过来的用户名和密码是否正确，下面将详细介绍这两个阶段。

# 1.握手初始化报文

在TCP/IP连接成功建立之后，服务端将主动向客户端发送一个请求，即握手初始化过程，其目的在于通知客户端：“请注意，我方即将开始验证你的身份。”这一过程中所使用的报文，即握手初始化报文（Handshake Packet），如图3-5所示。

![](images/fc00cca8e9bcbcf6723727dbeabb5e66255df6e4da00f6cfc916774b7406427a.jpg)  
图3-5 握手初始化报文

下面对该报文的关键属性进行简单说明：

□协议版本。即协议版本号，通常设定为10，此数值依据PROTOCOL_VERSION宏定义来确定。  
□数据库版本。数据库版本信息，由MYSQL_SERVER_VERSION宏定义确定。  
□ 线程ID。服务端为此次连接启动的线程ID。  
□随机挑战数。该过程被划分为两个主要部分，旨在实现数据库认证。在MySQL数据库认证场景中，我们采用了一种称为挑战、应答的认证机制。具体而言，此机制首先由服务端发起，生成一个挑战数并将其发送至客户端。随后，客户端需对该挑战数进行相应处理，并将处理结果返回给服务端。服务端在接收到客户端的应答后，会将其与预期的结果进行比对，以验证其正确性。若比对结果一致，则表明用户认证成功，从而完成整个认证流程。  
□ 服务器权能标志。用于与客户端协商通信方式。  
□字符编码。标识当前数据库所采用的字符集。  
□ 服务器状态。用于表示服务器状态，比如是否处于自动提交模式或者事务模式。

下面我们通过tcpdump -X命令来观察握手初始化数据包：

```txt
0x0000: 4500 0082 c558 4000 4006 b4c5 c0a8 1fff9 E....X@.E.....
0x0010: c0a8 1f0e 0cea b07e b719 0a2f eca1 2ce3
0x0020: 8018 01fe c0cc 0000 0101 080a 33c4 5dd6
0x0030: fbee 62cc 4a00 0000 0a35 2e37 2e31 3900 ..b.J....5.7.19.
0x0040: 0700 0000 321f 052d 3a52 454c 00ff f721
0x0050: 0200 ff81 1500 0000 0000 0000 0000 004d
0x0060: 730e 405a 4d6d 215c 591c 2b00 6d79 7371 s.@ZMm! \Y.+.mysq
0x0070: 6c5f 6e61 7469 7665 5f70 6173 7377 6f72 l-native_password
0x0080: 6400 
```

在深入理解TCP/IP协议体系的基础上，我们可以明确，IP数据报文的结构由IP首部和IP数据部分两大核心组件构成。IP首部遵循着固定的20B长度规范，确保了数据传输的一致性与高效性。而IP数据部分则进一步细分为TCP的首部及数据部分，其中，TCP的数据部分承载着至关重要的MySQL协议信息，这是我们需要深入剖析与研究的对象。

综上所述，通过解析上述数据包内容，可以明确前 $20\mathrm{B}$ 构成了IP头部，紧接着的是TCP头部。在TCP头部的结构中，其开头4B用于标识源端口和目标端口信息；随后的两个4B字段则分别承载着序列号和确认号的关键数据，这些细节在此不做进一步阐述。我们的关注焦点在于TCP头部中第4B的前4bit，这一字段实际上用于表示TCP头部的长度，其计量单位为4B。基于这一信息，我们可以计算出TCP头部总共占据了 $8\times 4 = 32$ B的空间。在明确TCP头部的结构之后，我们将能够深入细致地分析登录认证过程中所传输的数据报文。由前面分析的报文结构可以知道：

□前3B0x4a00，00表示报文长度，转换为十进制即 $74\mathrm{B}$ □  
□后面 $1 \mathrm{~B} 0 \times 00$ 表示消息序列号，后面就是包体内容。  
□后1B0x0a表示协议版本号，转换为十进制是10，所以协议号版本是10。

□ 再后表示数据库版本信息，它是NullTerminatedString类型，即遇到00结束，352e372e3139对应的5.7.19，正是当前使用的数据库版本。  
□再后面4B0x07000000表示线程ID。  
□其后8B0x321f052d3a52454c为随机挑战数。  
□ 00为填充值，fff7表示与客户端协商通信方式，此处为-CLIENT_PLGIN_AUTH，表示支持身份验证插件。  
□21表示数据库的编码，0200表示服务器状态。  
□之后26B就是挑战随机数和填充值，最后22B表示认证插件。

# 2. 登录认证报文

在服务器发起握手初始化过程之后，客户端会向服务器提交一个登录认证包，该过程旨在验证数据库用户的登录凭证。由于MySQL 4.0版本前后存在差异，这里聚焦于MySQL 4.1及后续版本中登录认证报文（Authentication Packet）的格式描述，登录认证报文结构如图3-6所示。

![](images/65949e055b08a69713b8ccaba8ab3717cc0f4dcc5d71a94a226d338c59c2f65f.jpg)  
图3-6 登录认证报文结构

下面对该报文的关键属性进行简单说明：

□ 客户端权能标志。用于与服务端协商通信方式。客户端收到服务端发送的握手初始化报文后，会对服务端发送的权能标志进行修改，保留自身所支持的功能，然后将权能标志返回给服务端，从而保证服务端与客户端通信的兼容性。  
□最大消息长度。客户端在通信过程中，发送或接收的消息均遵循一定的长度限制，该限制由客户端所支持的最大消息长度值确定。关于字符编码方面，客户端所采用的字符编码应与在握手初始化报文中由服务端所发送的字符编码保持一致。  
□用户名。用户名是客户端登录时所需的标识符。在挑战认证流程中，客户端会将用户密码与服务器发送的挑战随机数结合进行加密处理，生成挑战认证数据，随后将

此数据返回至服务器，以便进行用户身份的验证与确认。

□数据库名。在客户端的权限与功能配置中，若CLIENT_connect_WITH_DB标志位被明确激活或置位，则此特定字段成为必填项，旨在明确指示所连接的目标数据库。反之，在标志位未激活的情况下，该选项则被视为非强制性，用户可根据实际情况选择是否提供。为了进一步解析与审查登录认证过程中的数据包详情，我们可以借助tcpdump工具并附带-X选项，此操作将允许我们以十六进制及ASCII码形式捕获并展示网络数据包，从而深入观察并分析登录认证流程中的具体信息。

<table><tr><td>0x0000:</td><td>4500</td><td>00ef</td><td>bcae</td><td>4000</td><td>4006</td><td>bd02</td><td>c0a8</td><td>1f0e</td><td>E......@......</td></tr><tr><td>0x0010:</td><td>c0a8</td><td>1ff9</td><td>b07e</td><td>0cea</td><td>eca1</td><td>2ce3</td><td>b719</td><td>0a7d</td><td>......~......,......</td></tr><tr><td>0x0020:</td><td>8018</td><td>01f6</td><td>b463</td><td>0000</td><td>0101</td><td>080a</td><td>fbee</td><td>62cf</td><td>......c......b.</td></tr><tr><td>0x0030:</td><td>33c4</td><td>5dd6</td><td>b700</td><td>0001</td><td>85a6</td><td>ff01</td><td>0000</td><td>0001</td><td>3.][......</td></tr><tr><td>0x0040:</td><td>2100</td><td>0000</td><td>0000</td><td>0000</td><td>0000</td><td>0000</td><td>0000</td><td>0000</td><td>!......</td></tr><tr><td>0x0050:</td><td>0000</td><td>0000</td><td>0000</td><td>0000</td><td>726f</td><td>6f74</td><td>0014</td><td>ab41</td><td>......root...A</td></tr><tr><td>0x0060:</td><td>a9cc</td><td>c77b</td><td>68ac</td><td>a47e</td><td>d697</td><td>44e8</td><td>9933</td><td>fd79</td><td>......{h..~D..3.y}</td></tr><tr><td>0x0070:</td><td>858d</td><td>6d79</td><td>7371</td><td>6c5f</td><td>6e61</td><td>7469</td><td>7665</td><td>5f70</td><td>..mysq1_native_p</td></tr><tr><td>0x0080:</td><td>6173</td><td>7377</td><td>6f72</td><td>6400</td><td>6603</td><td>5f6f</td><td>7305</td><td>4c69</td><td>asset.f._os.Li</td></tr><tr><td>0x0090:</td><td>6e75</td><td>780c</td><td>5f63</td><td>6c69</td><td>656e</td><td>745f</td><td>6e61</td><td>6d65</td><td>nux._client_name</td></tr><tr><td>0x00a0:</td><td>086c</td><td>6962</td><td>6d79</td><td>7371</td><td>6c04</td><td>5f70</td><td>6964</td><td>0533</td><td>.libmysq1_pid.3</td></tr><tr><td>0x00b0:</td><td>3037</td><td>3438</td><td>0f5f</td><td>636c</td><td>6965</td><td>6e74</td><td>5f76</td><td>6572</td><td>0748._client_ver</td></tr><tr><td>0x00c0:</td><td>7369</td><td>6f6e</td><td>0635</td><td>2e37</td><td>2e31</td><td>3909</td><td>5f70</td><td>6c61</td><td>sion.5.7.19..pla</td></tr><tr><td>0x00d0:</td><td>7466</td><td>6f72</td><td>6d06</td><td>7838</td><td>365f</td><td>3634</td><td>0c70</td><td>726f</td><td>tform.x86_64.pro</td></tr><tr><td>0x00e0:</td><td>6772</td><td>616d</td><td>5f6e</td><td>616d</td><td>6505</td><td>6d79</td><td>7371</td><td>6c</td><td>gram_name.mysql</td></tr></table>

同样，起始部分是IP首部，占据20B，紧接着是TCP首部，占据32B（ $8\times 4$ ），随后从偏移量 $0\mathrm{x}$ b700开始即为登录认证数据包的具体内容。通过综合分析报文结构与登录认证报文格式，我们可以进一步进行解析与探讨。

□前 $3\mathrm{B}0\mathrm{x}$ b700，00表示报文长度，转换为十进制即 $183\mathrm{B}$ □

□后面 $1\mathrm{B}0 \times 01$ 表示消息序列号，后面就是包体内容。

□0x85a6 ff01 为客户端权能标志。

□0x0000 0001为客户端支持最大消息长度。

□0x21表示数据库的编码，后面23对00为填充值。

□0x726f 6f74表示用户名，以00为结束标志，解码即为root。

□0x14对应的十进制是20，表示后续20B就是加密后的密码。

本例可选的数据库名称不存在，最后部分表示认证插件。

# 3.2.3 命令执行阶段

命令执行阶段涉及的内容非常多，因为MySQL支持非常多的命令，并且不同的命令最终返回的内容及编码格式也不一样。特别地，如果查询的是MySQL的表数据，其中数据也区分各种类型的编码。下面将大致介绍一下命令执行的流程，并用查询语句来举例说明。

# 1. 命令请求报文

在客户端成功建立与数据库的连接之后，其便拥有了向服务端发起执行数据库操作的请求权限，这些操作包括但不限于数据的增加、删除、修改及查询，以及数据库的创建和表的建立等。此类命令请求报文如图3-7所示。

<table><tr><td>1 B
命令类型</td><td>n B
参数</td></tr></table>

图3-7 命令请求报文

下面对该报文的属性进行简单说明：

□命令类型。本段旨在阐述当前请求命令的类型。命令类型的具体说明如表3-3所示（命令列表已在源代码目录下的include/mysql_com.h头文件中明确定义）。

表 3-3 命令类型的具体说明  

<table><tr><td>命令</td><td>取值</td><td>说明</td></tr><tr><td>0x00</td><td>COM_SLEEP</td><td>(内部线程状态)</td></tr><tr><td>0x01</td><td>COM_QUIT</td><td>关闭连接</td></tr><tr><td>0x02</td><td>COM_INIT_DB</td><td>切换数据库</td></tr><tr><td>0x03</td><td>COM_QUERY</td><td>SQL 查询请求</td></tr><tr><td>0x04</td><td>COM_FIELD_LIST</td><td>获取数据表字段信息</td></tr><tr><td>0x05</td><td>COM_CREATE_DB</td><td>创建数据库</td></tr><tr><td>0x06</td><td>COM Drops_DB</td><td>删除数据库</td></tr><tr><td>0x07</td><td>COM_REFRESH</td><td>清除缓存</td></tr><tr><td>0x08</td><td>COM_SHUTDOWN</td><td>停止服务器</td></tr><tr><td>0x09</td><td>COM_STATISTICS</td><td>获取服务器统计信息</td></tr><tr><td>0x0A</td><td>COM_PROCESS_INFO</td><td>获取当前连接的列表</td></tr><tr><td>0x0B</td><td>COM_connect</td><td>(内部线程状态)</td></tr><tr><td>0x0C</td><td>COM_PROCESS_KILL</td><td>中断某个连接</td></tr><tr><td>0x0D</td><td>COM_DEBUG</td><td>保存服务器调试信息</td></tr><tr><td>0x0E</td><td>COM_PING</td><td>测试连通性</td></tr><tr><td>0x0F</td><td>COM_TIME</td><td>(内部线程状态)</td></tr><tr><td>0x10</td><td>COM-delayED_insert</td><td>(内部线程状态)</td></tr><tr><td>0x11</td><td>COM_CHANGE_USER</td><td>重新登录(不断连接)</td></tr><tr><td>0x12</td><td>COM_BINLOG=DUMP</td><td>获取 binlog 日志信息</td></tr><tr><td>0x13</td><td>COM_TABLE=DUMP</td><td>获取数据表结构信息</td></tr><tr><td>0x14</td><td>COMConnect_OUT</td><td>(内部线程状态)</td></tr><tr><td>0x15</td><td>COMRegister_SLAVE</td><td>从服务器向主服务器进行注册</td></tr><tr><td>0x16</td><td>COM_STMT_PREPARE</td><td>预处理 SQL 语句</td></tr><tr><td>0x17</td><td>COM_STMT_EXECUTE</td><td>执行预处理语句</td></tr><tr><td>0x18</td><td>COM_STMT_SEND_Long_DATA</td><td>发送 BLOB 类型的数据</td></tr><tr><td>0x19</td><td>COM_STMT_CLOSE</td><td>销毁预处理语句</td></tr><tr><td>0x1A</td><td>COM_STMT_RESET</td><td>清除预处理语句参数缓存</td></tr><tr><td>0x1B</td><td>COM_SET_OPTION</td><td>设置语句选项</td></tr><tr><td>0x1C</td><td>COM_STMT_fetch</td><td>获取预处理语句的执行结果</td></tr></table>

□参数。用户输入的MySQL客户端命令（不包含每行命令末尾的分号“;”），其字段的字符串最终结束符并非依赖于NULL字符，而是通过消息头部中的长度值来界定其边界的。

下面我们通过tcpdump-X的方式来观察请求执行一条select命令的数据包：

```asm
0x0000: 4500 004d bcd0 4000 4006 bd82 c0a8 1f0e E..M..@....   
0x0010: c0a8 1ff9 b07e 0cea eca1 2f25 b719 0eb3 ....\~..../...   
0x0020: 8018 01f5 902b 0000 0101 080a fbf2 7715 ....+.....w.   
0x0030: 33c8 2a97 1500 0000 0373 656c 6563 7420 3.\*.....select.   
0x0040: 2a20 6672 6f6d 2074 6573 7474 62 *.from.testtb 
```

命令请求数据包比较简单，除去IP首部和TCP首部，分析如下：

□前 $3\mathrm{B}0\mathrm{x}1500$ ，00表示报文长度，转换为十进制即21B。  
□后面 $1\mathrm{B}0\times 01$ 表示消息序列号，再后面就是包体内容。  
□ $0 \times 03$ 表示命令类型，即SQL查询请求。  
其后20B即为实际请求命令。

# 2. 服务器响应报文

不管是客户端发起登录认证还是请求执行命令，服务器都要返回相应的执行结果给客户端，此处的执行结果可以是查询指令的结果集，也可以是数据库操作的执行状态。服务器响应报文的第一个字节表示报文类型，客户端收到响应报文后，根据报文类型解析具体报文内容。响应报文详解如表3-4所示。

表 3-4 响应报文详解  

<table><tr><td>响应报文类型</td><td>含义</td><td>具体格式</td></tr><tr><td>OK_Packet</td><td>执行成功标志,比如连接数据库、非查询操作、注册从库、数据刷新等</td><td>int&lt;int&gt;：恒为0x00int：受影响行数int：该值为AUTO_INCREMENT索引字段生成,如果没有索引字段,则为0x00。int&lt;int&gt;：服务器状态int&lt;int&gt;：告警计数string:服务器消息(可选)</td></tr><tr><td>ERR_Packet</td><td>执行失败标志,比如登录认证不通过、非法查询、非空字段未指定值</td><td>int&lt;int&gt;：恒为0xFFint&lt;int&gt;：错误码,在源代码/include/mysqld_error.h头文件中定义string[1]:SQL执行状态标识位,用#进行标识string[5]:SQL的具体执行状态string:错误消息</td></tr><tr><td>EOF_Packet</td><td>用于标识Field和Row Data的结束,在预处理语句中,EOF也被用来标识参数的结束</td><td>int&lt;int&gt;:恒为0xFEint&lt;int&gt;:告警计数int&lt;int&gt;:状态标志位</td></tr><tr><td>Result Set</td><td>返回结果集,比如SELECT、SHOW</td><td>Result Set 消息分为五部分:Result Set Header:返回数据的列数量Field:返回数据的列信息(多个)EOF:列结束RowData:行数据(多个)EOF:数据结束</td></tr><tr><td>Field</td><td>数据表的列信息</td><td>LengthEncodedString:目录名称LengthEncodedString:数据库名称LengthEncodedString:数据表名称(AS后名称)LengthEncodedString:数据表原始名称(AS前名称)LengthEncodedString:列(字段)名称LengthEncodedString:列(字段)原始名称int&lt;int&gt;:填充值int&lt;int&gt;:字符编码int&lt;int&gt;:列(字段)长度int&lt;int&gt;:列(字段)类型(参考源代码/include/mysql_com.h头文件中的enum_field_type)int&lt;int&gt;:列(字段)标志(参考源代码/include/mysql_com.h头文件中的宏定义)int&lt;int&gt;:针对DECIMAL和NUMERIC类型的精度int&lt;int&gt;:填充值(0x00)LengthEncodedString:默认值</td></tr><tr><td>Row Data</td><td>在Result Set消息中,会包含多个Row Data结构,每个Row Data结构又包含多个字段值,这些字段值组成一行数据</td><td>LengthEncodedString:字段值......:多个字段值</td></tr></table>

除了上述常见的报文结构外，还存在PREPARE_OK包、Execute包、Parameter包等特定的报文结构，这些结构被应用于某些特定的响应场景中。例如，PREPARE_OK包出现在客户端向服务器发送预处理SQL语句后，服务器正确进行响应的场景中。这种机制在编程实践中尤为常见，比如在编写Java程序时，经常使用的PreparedStatement便是一种进行SQL预处理的有效工具。

接下来，我们将通过tcpdump -x 选项来详细观察客户端请求执行select命令后，服务器所返回的响应结果。此过程旨在深入了解网络通信中报文的具体传输情况。

```txt
mysql>select \*from testtb; +- -+ + id name 
```

+---+---+ 1 | zhangyu

服务器响应数据包如下：

<table><tr><td>0x0000:</td><td>4500</td><td>00b6</td><td>c576</td><td>4000</td><td>4006</td><td>b473</td><td>c0a8</td><td>1ff9</td><td>E....v@.e..s...</td></tr><tr><td>0x0010:</td><td>c0a8</td><td>1f0e</td><td>0ceab</td><td>b07e</td><td>b719</td><td>0eb3</td><td>eca1</td><td>2f3e</td><td>......~....../&gt;</td></tr><tr><td>0x0020:</td><td>8018</td><td>01fd</td><td>c100</td><td>0000</td><td>0101</td><td>080a</td><td>33c8</td><td>7221</td><td>......3.r!</td></tr><tr><td>0x0030:</td><td>fbf2</td><td>7715</td><td>0100</td><td>0001</td><td>022c</td><td>0000</td><td>0203</td><td>6465</td><td>..w.....,......de</td></tr><tr><td>0x0040:</td><td>6606</td><td>7465</td><td>7374</td><td>6462</td><td>0674</td><td>6573</td><td>7474</td><td>6206</td><td>f.testdb.testtb.</td></tr><tr><td>0x0050:</td><td>7465</td><td>7374</td><td>7462</td><td>0269</td><td>6402</td><td>6964</td><td>0c3f</td><td>000b</td><td>testtb.id.id.?...</td></tr><tr><td>0x0060:</td><td>0000</td><td>0003</td><td>0350</td><td>0000</td><td>0030</td><td>0000</td><td>0303</td><td>6465</td><td>......P....0....de</td></tr><tr><td>0x0070:</td><td>6606</td><td>7465</td><td>7374</td><td>6462</td><td>0674</td><td>6573</td><td>7474</td><td>6206</td><td>f.testdb.testtb.</td></tr><tr><td>0x0080:</td><td>7465</td><td>7374</td><td>7462</td><td>046e</td><td>616d</td><td>6504</td><td>6e61</td><td>6d65</td><td>testtb.name.name</td></tr><tr><td>0x0090:</td><td>0c21</td><td>006c</td><td>0000</td><td>00fd</td><td>0000</td><td>0000</td><td>000a</td><td>0000</td><td>!.l......</td></tr><tr><td>0x00a0:</td><td>0401</td><td>3107</td><td>7a68</td><td>616e</td><td>6779</td><td>7507</td><td>0000</td><td>05fe</td><td>..l.zhangyu......</td></tr><tr><td>0x00b0:</td><td>0000</td><td>2200</td><td>0000</td><td></td><td></td><td></td><td>..&quot;......</td><td></td><td></td></tr></table>

由于响应查询请求，按照 Result Set 格式来分析数据包：

0x02 对应的是 Result Set Header，表示 2 个字段。  
0x2c 0000 02 表示第一列信息占 $44\mathrm{B}$   
□0x03 6465 66中03表示后面3B是目录名称，此处为默认值def。  
□0x06746573746462中06表示后面6B是数据库名称，解码后即为testdb。  
□0x0674 6573 7474 62中06表示后面6B是数据表名称，解码后为testtb。  
□0x0674 6573 7474 62中06表示后面6B是数据表原始名称，解码后为testtb。  
□ $0\mathrm{x}02$ 6964中02表示后面2B为列名称，即id。  
□ $0 \times 02$ 6964中02表示后面2B为列原始名称，即id。  
□0x0c3f 00表示填充值和字符编码。  
□0x0b0000 00 表示列长度，此处id定义为int(10)，转换为十六进制即为 $0\mathrm{b}$ 。  
□0x03 0350 00分别表示列类型（FIELD_TYPE LONG）、标识、整型值精度。  
□ $0 \times 0000$ 为填充值。  
□0x30 0000 03 表示第二列信息所占48B，具体列信息不再赘述。  
□从0a 0000 04开始，后面即为行数据，所占10B，具体解析为： $①$ 0x0131中01表示后面一个字节为第一个字段id值，解码后为1； $②$ 0x077a68616e677975中07表示后面7个字节为第二个字段name值，解码后为zhangyu。

# 3.3 处理连接与创建线程

我们知道MySQL内部是一个多线程的处理模型，那么在多个客户端同时发请求时，MySQL内部是如何处理客户端连接并且为每个客户端创建一个线程的呢？下面将详细介绍。

# 3.3.1 MySQL监听客户端请求

MySQL 是一款采用 C 和 $\mathrm{C}++$ 编程语言开发的软件。为了深入且迅速地理解 MySQL 的源代码，通常需要从其主函数入手。MySQL 服务端的起始点明确位于 sql/main.cc 文件中。一旦进入该入口函数，可以观察到其主要作用是调用 mysql_main 函数，该函数实为 MySQL 启动流程的核心实现。

在第2章我们介绍过，mysqld_main函数承载了诸多关键任务，包括处理配置文件与启动参数、初始化系统层面的全局变量、设置日志机制及同步信号、初始化网络通信模块并启动循环监听等。本节重点聚焦于与连接创建直接相关的操作流程，MySQL启动流程概要如图3-8所示。

在跳过mysqld_main函数中针对配置文件及参数的处理步骤后，首先需要进入network_init阶段，以执行网络系统的初始化。此过程涉及两个至关重要的类，它们在网络系统的构建与配置中扮演着核心角色。

![](images/0601e56396b93ecab038d7fad32746f77cad5939fcbcee11543bf7a361889444.jpg)  
图3-8 MySQL启动流程概要

□Connection acceptor：这是一个模板类，它通过Mysqld_SOCKET Listener类完成实例化，封装了对于监听套接字的操作，支持不同的监听实现。

□Mysqld_SOCKET Listener：实现以套接字的方式监听客户端的连接，支持TCP套接字和UNIX套接字两种方式。

网络初始化具体完成以下两项工作：先获取mysqld_SOCKET Listener，创建Connection acceptor；然后执行mysqld_SOCKET Listener的setup Listener操作，对网络和域套接字分别初始化，注意此时还没有启动监听。

网络初始化完成后，进入mysqld_SOCKET.acceptor->connection_event_loop()，启动循环监听客户端连接，与该操作相关的类为：Connectionhandlermanager和Connectionhandler。前者是一个全局的单例模式，用于管理连接处理器；后者是一个虚基类，用于具体实现如何处理连接。各种连接方式都是继承这个类来实现，常见连接方式主要有三种：

□ Per_thread_connectionhandler：一个连接一个线程，默认实现方式，可以通过 thread_handle 参数设置。  
One_thread_connectionhandler：所有连接用一个线程。  
□ Plugin_connectionhandler：由插件具体实现，例如线程池。

该过程主要有两项工作：一是执行mysqld_SOCKET Listener的listen_for_Connection_event操作，监听客户端请求，获取请求后，构造一个Channel_info用于保存所有与连接相关的信息；二是执行Connection handler manager的process_

new_connection 操作，处理新连接，它的方法流程图如图 3-9 所示。

![](images/63659c1c89c5dd221d5052525d2c001cb4a361c2d56a0c59359bb79f08d4280a.jpg)  
图3-9 process_new_connection方法流程图

首先判断服务是否停止，如果是，则关闭连接并返回，否则进行下一步。在执行连接计数递增操作时，通过调用check_and_Incr_connect_count函数来实现。该函数负责检查当前连接数是否已达上限。若当前连接数已超出预设的最大连接数，则返回失败。

注意 存在一个例外情况：在当前连接数恰好等于最大连接数时，系统仍会允许管理员用户额外建立一个连接，因此实际可承受的最大连接数应为预设的最大连接数加一。

在确认连接数尚未触及上限的前提下，将执行Connectionhandler模块中的add connection操作。在Per_thread_connectionhandler模式下，每个独立的连接均被分配至一个专属线程进行处理，同时，MySQL系统为了优化连接处理效率，会预先缓存一定数量的线程以供复用。这一缓存线程的上限由maxBlocked_pthreads变量进行调控。在执行add_connection操作时，系统会首先评估当前是否存在空闲线程。若有空闲线程存在，则立即唤醒该线程，并将channel_info信息加入至其处理队列中；反之，若当前无空闲线程可用，系统将触发新线程的创建流程，以满足新增连接的处理需求。

# 3.3.2 创建连接线程

在MySQL系统中，若遇到无空闲线程可供分配的情境，系统将触发mysql_thread_create函数的执行，以创建新的线程来应对请求。具体而言，这一过程中会执行hangle_Connection函数以处理新的连接。接下来，我们将深入探讨在Per_thread_connection

handler模式下，连接线程的创建流程，如图3-10所示。

hangle_connection函数首先执行线程所需内存的初始化流程，随后调用init_new_thd函数来创建THD对象。作为MySQL系统中至关重要的一部分，THD结构体负责承载并维护一个线程的上下文信息，其详尽描述将在后续章节中展开。创建完成的THD对象随后会被加入到thdmanager中，后者是一个全局性的Global_THDmanager线程管理类，它通过链表结构来集中管理和维护所有线程的THD对象。

在Per_thread_connectionhandler模式下，每个线程均被映射至一个独立的THD结构。随后，通过执行thdprepare_connection函数进行连接验证，此过程涉及多个步骤，包括用户登录验证、THD结构中用户信息的更新、用户权限核实、SS（安全套接字层）检查以及针对单一用户设置的最大连接数限制等。在此，我们将重点阐述用户登录验证的流程。

MySQL的用户管理相关数据被妥善存储在系统表mysql.user中，该表不仅详细记录了用户的基本身份信息，还涵盖了用户的权限设置。实际上，连接验证的核心环节便是从该表中检索用户信息，并据此进行系统验证。具体步骤概述如下：

![](images/07ff8013fb3c6d92b30df10408066a26dd88251d0a81595e8d79e17efe1bf913.jpg)  
图3-10 连接线程的创建流程

1）在获取客户端的IP地址与主机名后，我们利用acl_check_host(sql/sql_ acl.cc)函数进行连接权限的验证。此验证流程核心在于检索两个关键数据结构：首先，是动态数组acl_wild_hosts，该数组用于存储包含通配符的主机名，以便于进行模式匹配；其次，是哈希表acl_check_hosts，该表则用于存储确切的主机名，以实现快速查找。值得注意的是，这两个数据结构均在MySQL服务器启动阶段，从系统表mysql_users中读取并初始化。

2）客户端IP验证通过后，服务端发送握手初始化给客户端，等待客户端请求。

3）客户端对密码进行加密后，发送登录认证请求。

4）服务端获取请求，解析获得用户名密码后，调用check_user(sql/sql_connect.c)函数验证用户名密码。

在此需要指出，对于已在MySQL中创建用户的读者而言，完成用户创建及授权步骤后，必须执行flush PRIVILEGES操作，以确保新创建的用户能够立即生效。这一步骤的必要性源于MySQL为提高权限判断效率并减少磁盘I/O操作，在mysqld服务启动时，会将权限系统表的内容加载至内存中。然而，对于后续的用户权限变更操作，这些变更仅直

接反映于磁盘层面，而内存中的权限系统表内容并未同步得到更新。因此，若未执行flushPRIVILEGES操作，新建用户将无法成功登录系统。

为使新建用户生效，存在两种解决方案：一是重启 mysql 服务，通过服务重启的方式强制重新加载权限系统表；二是执行 flush PRIVILEGES 命令，该命令的作用即触发权限系统表的重新加载过程。

一旦连接验证通过，系统将进入处理请求的准备阶段。此阶段的主要任务包括内存申请、动态系统变量（如字符集、事务设置等）的设置，以及执行必要的初始化命令。准备工作完成后，系统将进入do_command阶段，即命令执行阶段。此阶段通过for循环机制，持续从网络接收客户端发送的命令。在新建立连接或当前连接处于空闲状态时，系统将在此阶段阻塞以等待新命令的到来。一旦接收到请求，系统将调用dispatch_command函数进行处理。以下是对该函数功能的一个简化描述：

```c
bool dispatch_command(THD \*thd, const COM_DATA \*com_data,   
enum enum_server_command command)   
{ thd->set_command(command); switch (command) { case COM_INIT_DB: .. .. case COM_REGISTER_SLAVE: ... .. .. case COM_QUERY: { if (alloc_query(thd, com_data->com_query.query,   
com_data->com_query.length)) break; . mysql_scan(thd, &parser_state); 
```

显然，该函数构成了一个复杂的 switch 结构，旨在依据命令类型的差异执行相应的处理流程。

注意 尽管标记为COM_QUERY类型，但其涵盖的功能远不止简单的查询操作。实际上，该函数充当了处理数据库所有类型访问请求的枢纽，包括DDL和DML等操作。针对这些操作的具体处理，是由mysql_scan函数来完成的。

# 3.3.3 THD类

THD类，作为线程描述类，其定义体现了线程与THD之间一一对应的紧密关系。THD内部存储了与线程相关联的多种关键信息，这一设计旨在高效地管理与访问线程相关数据。

该类位于sql/sql_class.cc文件中，体现了其在系统架构中的重要地位。此外，THD类通过继承机制，融合了三个基类的特性，THD继承的类如表3-5所示。这一继承关系进一步增强了其功能的多样性和灵活性。

表 3-5 THD 继承的类  

<table><tr><td>名称</td><td>说明</td></tr><tr><td>MDL_context Owner</td><td>将 MDL 与 THD 分隔的接口，包括元数据边界控制（进入/退出），元数据信息通知等</td></tr><tr><td>Query arena</td><td>维护语法树信息的类</td></tr><tr><td>Open_tables_state</td><td>维护该线程打开和封锁的表的状态，维护了表信息和锁信息</td></tr></table>

下面介绍一下THD类重要成员变量，如表3-6所示。

表 3-6 THD 类重要成员变量  

<table><tr><td>名称</td><td>说明</td></tr><tr><td>net</td><td>客户连接描述符</td></tr><tr><td>protocol</td><td>网络通信协议描述符</td></tr><tr><td>Packet</td><td>动态缓冲,用作网络I/O</td></tr><tr><td>mem_root</td><td>继承自Queryarena,MySQL的内存管理模块,用于统一申请和释放内存</td></tr><tr><td>Lex</td><td>语法树描述符</td></tr><tr><td>stmt_map</td><td>该连接中存储prepare语句的map</td></tr><tr><td>Real_id</td><td>该线程在操作系统上的ID</td></tr><tr><td>Thread_id</td><td>MySQL服务器分配非该线程的ID,可通过show processlist查看</td></tr><tr><td>Query_id</td><td>当前查询ID,该变量是全局的,由受互斥锁保护计数器自动生成</td></tr><tr><td>Proc_info</td><td>描述线程当前运行状态,即执行show processlist结果中info列的内容</td></tr><tr><td>Slave_thread</td><td>标记该线程是否为从服务器线程</td></tr><tr><td>Server_id</td><td>唯一的标识某个数据库实例,应用在主从复制中,表示操作来源,从而避免操作无限循环</td></tr><tr><td>Query</td><td>字符串形式记录当前接收的SQL</td></tr><tr><td>Query_plan</td><td>语句执行计划</td></tr><tr><td>command</td><td>标识当前查询的类型:COMstmt_PREPARE,COM_QUERY等</td></tr><tr><td>variables</td><td>会话级别系统变量,比如autocommit、sql_mode等,可通过set session variables=×××方式设置,通过show session variables\G查询</td></tr><tr><td>user_connect</td><td>描述当前连接的当前连接用户的信息,包括用户名、host、该用户更新数、连接数等,注意并不是所有连接都会配置user_connect,这取决于mysql.user里面的最后四个字段以及配置参数max_user Connections</td></tr><tr><td>db</td><td>当前所在数据库</td></tr><tr><td>Db_charset</td><td>描述当前数据库字符集</td></tr><tr><td>Db_access</td><td>描述用户对当前所在数据库的操作权限</td></tr><tr><td>Col_access</td><td>描述用户具备执行show table权限的表</td></tr></table>

在本小节中，我们将深入探讨每个线程的内存使用情况，并特别聚焦于某些关键变量的作用。MySQL在启动阶段会申请一定量的内存，这部分内存被设计为公有内存，供所有

线程共享使用。此外，为了支持各个用户连接的独立操作，MySQL 还需为每个连接单独分配内存，这部分内存不具备共享性，因此被称为私有内存。

MySQL 通过分阶段处理用户连接请求的机制，使得我们能够分阶段地详细分析它为每个连接所分配的内存情况。在连接处理的各个阶段中，MySQL 会针对用户名、数据库名等必要信息进行内存分配。然而，鉴于这些内存占用相对较小，且对整体性能分析的影响有限，故在此暂不进行深入探讨。分配内存详细如下：

1）创建THD并初始化。每个连接独占一个线程，每个线程独占一个堆栈，大小为thread_stack（x64机器默认为256KB）；thd->net(buf为用户线程接收网络包的缓冲区，大小可动态调整。初始大小为net_buffer_length（默认为16KB）。  
2）登录阶段。thd->packet为MySQL发送数据的缓冲区，MySQL在登录阶段为thd->packet分配内存，大小为net_buffer_length（默认为16KB）。  
3）登录完成。thd $\rightarrow$ mem_root是每个线程的内存池，登录成功后为其分配内存，初始大小为query_prealloc_size（默认8KB）；如果内存池内存不足以使用，MySQL会额外增加内存的分配，每次最少query_alloc_block_size。在每个Query处理完毕后，内存被统一释放。  
4）接收SQL阶段。如果thd->net(buf的大小不足以容纳用户发送的SQL，MySQL会重新为 $\mathrm{thd}\rightarrow$ net(buf分配内存，大小足以容纳整个SQL，但最大不超过max Allowed packet；从mem_root中为thd->query分配内存，如果mem_root不足以容纳整个Query，则调用系统malloc为thd->query分配内存，大小为整个SQL长度。  
5）词法语法分析阶段。可以认为 thd->queryquery 在 thd->lex 中有两个副本，一个是输入，另一个是解析之后的结果。  
6）查询阶段。对于join查询、Orderby查询、临时表等不同操作进行不同的内存分配，比如join查询，根据join表的个数来分配内存，两个表之间join会使用一个join buffer。  
7）返回结果。MySQL 使用 thd->packet 作为 MySQL 的发送缓冲区，初始大小为 net_buffer_length。如果 MySQL 表有 blob 或者 text 字段，导致发送缓冲区不足以容纳一行数据，MySQL 会重新分配内存，但是最大不超过 maxAllowedPacket。返回结果以后，MySQL 会把发送缓冲区恢复为默认。

# 3.4 总结

本章详细阐述了MySQL中常见的连接途径、MySQL通信协议及交互流程，并给出了处理连接与创建线程的具体步骤。深入掌握这些知识，有助于我们灵活选用既可用又高效的连接方式。深入理解MySQL通信协议则能够帮助我们便捷地实现自定义网络监控功能。而清晰地理解处理连接与创建线程的过程，对于分析特定内存问题以及性能瓶颈，特别是针对短连接优化的处理，特别有帮助。

# 数据字典

数据库表由表结构和数据内容组成，这两者通常独立存储。数据库系统负责统一管理所有表的结构信息，即元数据，这些信息统称为数据字典。数据字典本质上是系统中所有数据元素定义的汇总。本章专注于探讨数据字典中的一个关键组成部分——表结构的管理。在MySQL 8.0版本之前，数据字典的管理较为杂乱无章，主要原因是MySQL Server层和InnoDB存储引擎层均各自维护了数据字典信息。然而，自MySQL 8.0起，数据字典信息的维护已统一至InnoDB存储引擎层。

# 4.1 数据字典简介

MySQL 5.7 版本的数据字典架构如图 4-1 所示。

MySQL 5.7 的数据字典主要包含如下三层：

□MySQL文件层。ibdata1文件主要承载了InnoDB存储引擎的数据字典信息，而MyISAM存储引擎的数据字典信息则保存在.frm文件中。鉴于MySQLServer层需要依赖.frm文件来检索数据字典的相关信息，因此每个InnoDB存储引擎的表同样保留了对应的.frm文件。所以对于每一个InnoDB存储引擎表而言，实际上存在两套数据字典信息。

□InnoDB存储引擎层。在MySQL的运行过程中，InnoDB存储引擎会从ibdata1文件中加载相关表的数据字典信息，并在内存中构建相应的表对象。这些表对象被保存在哈希表和最近最少使用（Least Recently Used，LRU）链表中，LRU链表主要用于实现快速查找和数据淘汰功能。

![](images/30b7073c1b3a4258830206b141ec458dfb77c23297355ac51233ae8e62dc8ed7.jpg)  
图4-1 MySQL5.7版本的数据字典架构

□MySQL Server层。在MySQL的运行过程中，Server层会从.frm文件中读取表的数据字典信息，并据此构建相应的表对象。同时，Server层还会使用哈希表来管理这些对象。当客户端需要操作这些表时，它会首先在哈希表中查找相应的对象。如果在哈希表中未找到所需对象，则Server层会从.frm文件中加载所需信息，并将其存储在哈希表中以供后续使用。

或许有人会对InnoDB存储引擎的表感兴趣，InnoDB存储引擎层和Server层均在内存中构建了相应的表对象，那它们各自承担着何种职能呢？

在InnoDB存储引擎中，从索引的叶子节点检索到的记录以二进制格式呈现。一条记录由多个字段组成，每个字段具有不同的数据类型，且这些数据类型各自拥有独特的编码机制。InnoDB存储引擎维护的表对象的核心功能之一是提供表结构信息，其中详细记录了每个字段的数据类型。了解了每个字段的数据类型后，便能够依据其编码规则对字段数据进

行解析。此外，InnoDB存储引擎中的表对象还负责记录索引的相关信息，在执行数据检索操作时，会从表结构中提取相应的索引信息。

相比之下，MySQL Server层负责维护表结构信息，同时也管理着各个字段的数据类型。需要注意的是，这些数据类型及其编码方式与InnoDB存储引擎所使用的存在差异。当InnoDB存储引擎完成数据解析后，会将其返回给Server层，在此过程中会进行数据转换。有关此转换过程的详细信息，可以参考row_sel_STORE_mysql_rec方法。数据转换完成后，Server层会根据其定义的数据类型再次进行解析，并最终将结果返回给客户端。

在简单了解了每一层的作用后，下面针对每一层进行详细的介绍。

# 4.1.1 文件层

最下面一层为文件存储层，直接访问MySQL的数据目录可以查看该层级的内容，图4-1中的内容就是基于数据目录中的信息整理而成的。文件层主要涵盖以下几部分：

# 1. ibdata1

该系统表空间承担着存储InnoDB存储引擎核心数据字典信息的职责。通过图4-1可以清晰地看到，它维护着四个关键的数据字典表：系统表（SYS_TABLES）、系统索引表（SYS_INDEXES）、系统列表（SYS_COLUMNSS）、系统索引列表（SYS_FIELDS）。这些表详细记录了数据库内所有表的数据字典信息，包括字段信息、索引信息等。此外，该表空间还包含系统外键列表（SYS FOREIGN）、系统数据文件表（SYS_DATAFILES）等其他表，用于记录相关的数据字典信息。

在此，读者可能会产生疑问：既然 SYS_TABLES、SYS_INDEXES、SYS_COLUMNMS、SYS_FIELDS 用于记录其他表的字段、索引等信息，那么这些表自身的字段和索引信息又是如何记录的呢？实际上，这些信息是通过硬编码的方式嵌入在 MySQL 代码中的，具体代码如下：

# 系统表：

```c
/* 将基础系统表的描述插入字典缓存 */
/*--------*/
table = dict_mem_table_create("SYS_TABLES", DICT_HDR_SPACE, 8, 0, 0, 0);
dict_mem_table_add_col(table, heap, "NAME", DATA_BINARY, 0, MAX_FULL_NAME_LEN);
dict_mem_table_add_col(table, heap, "ID", DATA_BINARY, 0, 8);
/* ROW_MATCH = (N_COLS >> 31) ? COMPACT : REDUNDANT */
dict_mem_table_add_col(table, heap, "N_COLS", DATA_INT, 0, 4);
/* The low order bit of TYPE is always set to 1. If the format is UNIV_MATCH_B or higher, this field matches table->flags. */
dict_mem_table_add_col(table, heap, "TYPE", DATA_INT, 0, 4);
dict_mem_table_add_col(table, heap, "MIX_ID", DATA_BINARY, 0, 0);
/* MIX_LEN may contain additional table flags when 
```

ROW_FORMAT! $\equiv$ REDUNDANT. Currently, these flags include   
DICT_TF2_TEMPORARY.\*/   
dict_mem_table_add_col(table, heap, "MIX_LEN", DATA_INT, 0, 4);   
dict_mem_table_add_col(table, heap, "CLUSTER_NAME", DATA_BINARY, 0, 0);   
dict_mem_table_add_col(table, heap, "SPACE", DATA_INT, 0, 4);   
table->id $=$ DICT_TABLES_ID;   
dict_table_add_to_cache(table, FALSE, heap);   
dict_sys->sys_tables = table;   
mem_heap_empty(heap);   
index $=$ dict_mem_index_create("SYS_TABLES","CLUST_IND", DICT_HDR_SPACE, DICT_UNIQUE | DICT_CLUSTERED, 1);   
dict_mem_index_add_field(index, "NAME", 0);   
index->id $=$ DICT_TABLES_ID;   
error $=$ dict_index_add_to_cache(table, index, mtr_read-ulint dictated hdr +DICT_HDR_TABLES, MLOG_4BYTES,&mtr), FALSE);   
ut_a(error $= =$ DB_SUCCESS);   
/\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\*/\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* /\* / $ID\_IND"$ , DICT_HDR_SPACE, DICT_UNIQUE, 1);   
dict_mem_index_add_field(index, "ID", 0);   
index->id $=$ DICT_TABLE_IDS_ID;   
error $=$ dict_index_add_to_cache(table, index, mtr_read-ulint dictated hdr +DICT_HDR_TABLE_IDS, MLOG_4BYTES,&mtr), FALSE);   
ut_a(error $= =$ DB_SUCCESS);

# 系统索引表：

table $=$ dict_mem_table_create("SYS_INDEXES",DICT_HDR_SPACE, DICT_NUM_COLS_SYS_INDEXES,0,0,0); dict_mem_table_add_col(table,heap,"TABLE_ID",DATA_BINARY,0,8); dict_mem_table_add_col(table,heap,"ID",DATA_BINARY,0,8); dict_mem_table_add_col(table,heap,"NAME",DATA_BINARY,0,0); dict_mem_table_add_col(table,heap,"N_FIELDs",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"TYPE",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"SPACE",DATA_INT,0,4);

```c
dict_mem_table_add_col(table, heap, "PAGE_NO", DATA_INT, 0, 4);  
dict_mem_table_add_col(table, heap, "MERGE_threshold", DATA_INT, 0, 4);  
table->id = DICT_INDEXES_ID;  
dict_table_add_to_cache(table, FALSE, heap);  
dict_sys->sys_indexes = table;  
mem_heap_empty(heap);  
index = dict_mem_index_create("SYS_INDEXES", "CLUST_IND", DICT_HDR_SPACE, DICT UNIQUE | DICT_CLUSTERED, 2);  
dict_mem_index_add_field(index, "TABLE_ID", 0);  
dict_mem_index_add_field(index, "ID", 0);  
index->id = DICT_INDEXES_ID;  
error = dict_index_add_to_cache(table, index, mtr_read-ulint(dist hdr + DICT_HDR_INDEXES, MLOG_4BYTES, &mtr), FALSE);  
ut_a(error == DB_SUCCESS); 
```

# 系统列表：

table $=$ dict_mem_table_create("SYS_COLUMNNS",DICT_HDR_SPACE, 7,0,0,0); dict_mem_table_add_col(table,heap,"TABLE_ID",DATA_BINARY,0,8); dict_mem_table_add_col(table,heap,"POS",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"NAME",DATA_BINARY,0,0); dict_mem_table_add_col(table,heap,"MTYPE",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"PRTYPE",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"LEN",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"PREC",DATA_INT,0,4); table->id $=$ DICT_COLUMNNS_ID; dict_table_add_to_cache(table,FALSE,heap); dict_sys->sys-columns $\equiv$ table; mem_heap_empty(heap); index $=$ dict_mem_index_create("SYS_COLUMNNS","CLUST_IND", DICT_HDR_SPACE, DICT UNIQUE | DICT_CLUSTERED,2); dict_mem_index_add_field(index,"TABLE_ID",0); dict_mem_index_add_field(index,"POS",0); index->id $=$ DICT_COLUMNNS_ID;

error $=$ dict_index_add_to_cache(table,index, mtr_read-ulint(dist_hdr +DICT_HDR_COLUMNSMLOG_4BYTES,&mtr), FALSE);   
ut_a(error $\equiv$ DB_SUCCESS);

系统索引列表：

table $=$ dict_mem_table_create("SYS_FIELDS",DICT_HDR_SPACE,3,0,0,0); dict_mem_table_add_col(table,heap,"INDEX_ID",DATA_BINARY,0,8); dict_mem_table_add_col(table,heap,"POS",DATA_INT,0,4); dict_mem_table_add_col(table,heap,"COL_NAME",DATA_BINARY,0,0); table->id $\equiv$ DICT_FIELDS_ID; dict_table_add_to_cache(table,FALSE,heap); dict_sys->sys_fields $\equiv$ table; mem_heap_free(heap); index $=$ dict_mem_index_create("SYS_FIELDS","CLUST_IND", DICT_HDR_SPACE, DICT UNIQUE | DICT_CLUSTERED,2); dict_mem_index_add_field(index,"INDEX_ID",0); dict_mem_index_add_field(index,"POS",0); index->id $\equiv$ DICT_FIELDS_ID; error $=$ dict_index_add_to_cache(table,index, mtr_read-ulint(table,hdr +DICT_HDR_FIELDS, MLOG_4BYTES,&mtr), FALSE); ut_a(error == DB_SUCCESS);

每次系统启动时，都会通过硬编码的方式构建各个表的表对象。而一旦构建完成，若需遍历表内数据，如何确定索引的确切位置便成为问题。通过查阅系统索引表，我们发现它记录了每个索引的表空间ID以及根页码号。然而，系统索引表、系统表等数据字典表的索引位置信息又是如何存储的呢？这些信息同样是通过硬编码的方式嵌入在代码中的，具体定义如下：

```c
/\* 数据字典头偏移量 \*/  
\*/ 最近分配的row id \*/  
#define DICT_HDR_ROW_ID 0  
\*/ 最近分配的表ID \*/  
#define DICT_HDR_TABLE_ID 8  
\*/ 最近分配的索引ID \*/  
#define DICT_HDR_INDEX_ID 16  
\*/ 最近分配的表空间ID \*/  
#define DICT_HDR_MAX_SPACE_ID 24 #define DICT_HDR_MIX_ID_LOW 28 /\* Obsolete,always DICT_HDR_FIRST_ID\*/  
\*/ 数据字典表SYS_TABLES 聚簇索引的根节点页的页号\*/
```

```c
define DICT_HDR_TABLES 32  
/* 数据字典表 SYS_TABLES 二级索引的根节点页的页号 */  
#define DICT_HDR_TABLE_IDS 36  
/* 数据字典表 SYS_COLUMN 聚簇索引的根节点页的页号 */  
#define DICT_HDR_COLUMN 40  
/* 数据字典表 SYS_INDEXES 聚簇索引的根节点页的页号 */  
#define DICT_HDR_INDEXES 44  
/* 数据字典表 SYS_FIELDS 聚簇索引的根节点页的页号 */  
#define DICT_HDR_FIELDS 48  
/* 为创建字典头的表空间段的段头 */  
#define DICT_HDR_FSEGHEADER 56
```

上述字段构成了数据字典头的定义，其中 DICT_HDR_TABLES、DICT_HDR_COLUMNSS、DICT_HDR_INDEXES、DICT_HDR_FIELDS 分别记载了 SYS_TABLES、SYS_COLUMNSS、SYS_INDEXES、SYS_FIELDS 四个系统表的聚簇索引的根页码，这些索引默认存储于系统表空间内，因此无须记录表空间标识符。在对相应表进行扫描时，凭借这些信息即可精确定位索引的位置。此外，数据字典头还记录了 row id、表 ID 以及索引 ID，在数据字典头初始化过程中，DICT_HDR_ROW_ID、DICT_HDR_TABLE_ID、DICT_HDR_INDEX_ID、DICT_HDR_MIX_ID_LOW 将被初始化为 DICT_HDR_FIRST_ID 的值，而 DICT_HDR_FIRST_ID 则定义为数值 10。后续章节将重点阐述 row id 的分配机制。

# 2. MySQL Schema

这里主要保存了db、user、columns PRIV、tables PRIV等系统表，这些表负责维护数据库用户的详细信息以及相关的权限设置。它们部分采用MyISAM存储引擎，部分则采用InnoDB存储引擎。对于MyISAM存储引擎而言，表结构信息被保存在.frm文件中，索引信息则存储在.MYI文件中，而数据本身则位于.MYD文件。至于InnoDB存储引擎，表结构信息同样在.frm文件中保存一份，并且在ibdata1文件的数据字典表中也保存一份副本，其数据实际存储在.ibd文件中。

# 3. information_schema

这里主要保存数据库配置、运行状态等信息，包括但不限于CHARACTER_SETS、GLOBAL_STATUS、GLOBAL_variables、INNODB_LOCKS、INNODB_TRX等。这些信息存储于临时表中，其中一部分采用内存引擎，另一部分则使用InnoDB临时表。这些表的结构信息已预先编码于代码内，具体细节可参考ST_SCHEMA_TABLE schema_tables中的定义，例如CHARACTER_SETS表的详细信息：

```c
ST_FIELD_INFO charsets_fields_info[] = {  
    {"CHARACTER_SET_NAME", MY_CS_NAME_SIZE, MySQL_TYPE_STRING, 0, 0, "Charset", SKIP_OPEN_TABLE},  
    {"DEFAULTCOLLATE_NAME", MY_CS_NAME_SIZE, MySQL_TYPE_STRING, 0, 0, "Default collation", SKIP_OPEN_TABLE}, 
```

```javascript
{"DESCRIPTION",60,MYSQL_TYPE_STRING，0,0,"Description",SKIP_OPEN_TABLE},{"MAXLEN",3,MYSQL_TYPE_longLONG,0,0,"Maxlen",SKIP_OPEN_TABLE},{0,0,MYSQL_TYPE_STRING，0,0,0,SKIP_OPEN_TABLE}; 
```

# 4. performance_schema

这里主要保存的是MySQL运行时的监控数据。例如，与事件相关的表会在SQL语句执行的不同阶段进行数据采集点的设置，使得MySQL能够追踪SQL语句执行的详细过程。同样，performance_schema通过设置数据采集点，记录每个环节所分配的内存信息，以便于定位内存相关的问题。performance_schema中的所有表均采用performance_schema引擎，其数据字典信息存储于.frm文件中，而实际数据并不存储，而是在MySQL运行时动态收集。

# 5. mytest schema

该数据库由笔者构建，可以视为我们日常业务应用中所创建的数据库，其内部主要涵盖以MyISAM和InnoDB为存储引擎的各类表。

根据上述说明，我们能够理解MySQL 5.7版本中数据字典信息的管理存在一定的混乱，其主要根源在于多种存储引擎的并存。在MySQL的早期阶段，其Server层主要是为MyISAM存储引擎量身定制的。因此，随着其他存储引擎的引入，它们也必须与Server层的逻辑保持兼容，并使用Server层的数据字典信息。因此，我们可以观察到大多数表都保留了.frm文件。

# 4.1.2 InnoDB 存储引擎层

在前文提及的 ibdata1 文件中，存储了 InnoDB 存储引擎的数据字典信息。那么，在 MySQL 运行期间，这些数据字典信息是如何被利用的呢？实际上，在 InnoDB 存储引擎运作时，它为每个表维护了一个表对象。这个表对象里面包含诸如表的字段数量、字段类型、索引信息等关键数据，这些数据是从 ibdata1 文件内的 SYS_TABLES、SYS_INDEXES、SYS_COLUMNMS、SYS_FIELDS 等表中读取的。接下来，我们将探讨这个表对象的具体定义。

```c
/\*数据库表的数据结构。在dict_mem_table_create()中，大多数字段将被初始化为0、NULL或FALSE\*/  
struct dict_table_t{  
/\*\*表ID. \*/table_id_t id;  
/\*\*表名称.\*/table_name_t name;
```

```c
/\*NULL或者此表被分配到的表空间名称，由TABLESPACE选项指定\*/ id_name_t tablespace; /\*\*放置表的聚簇索引的表空间.\*/ uint32_t space; /\*总列数（包括虚拟列和非虚拟列）\*/ unsigned n_t cols:10; /\*列描述的数组\*/ dict_col_t\* cols; /\*以字符串形式打包的列名 "name1\0name2\0...nameN\0"。在字符串包含n cols之前，它将从临时堆中分配。最终的字符串将从 table->heap中分配。\*/ const char\* col_names; /\*\*表的索引列表.\*/ UT_LIST_BASE_NODE_T(dist_index_t) indexes; /\*表中的外键约束列表。这些指的是其他表中的列\*/ UT_LIST_BASE_NODE_T(dist_foreign_t) foreign_list;
```

鉴于dict_table_t结构体的定义较为冗长，这里仅展示其核心字段。该结构体存储了表的ID、名称、字段数量、字段类型、字段名称、索引信息以及外键信息等关键元数据。InnoDB存储引擎通过这些元数据信息对表执行具体操作，例如，在执行数据检索时，仅需定位到相应的索引信息；而在进行数据解析时，则根据字段信息和编码类型进行解析。

我们已经了解到，在InnoDB存储引擎层，表的数据字典信息由dict_table_t结构体维护。dict_table_t结构体从ibdata1文件中获取数据字典信息。因此，每次对表进行操作时，是否都需要从ibdata1中获取信息并构建dict_table_t结构体对象？这需要考虑操作是否由同一用户线程执行。基于这些考量，InnoDB引擎层在内存中维护了两个哈希表和一个LRU链表，以管理所有的dict_table_t结构体对象。

一个dict_table_t表对象需要存储在三个地方：

□ table_hash。以表名的哈希值作为键（Key），以指向 dict_table_t 表对象的指针作为值（Value）。  
□ table_id_hash。以表标识符的哈希值作为键，以指向dict_table_t表对象的指针作为值。  
□ table_LRU。该链表的节点是指向dict_table_t表对象的指针。这种设计主要是为了通过表名或表标识符迅速访问对应的dict_table_t表对象。鉴于表对象的数量可能极为庞大，因此有必要设定一个阈值。一旦表对象数量超过这一阈值，就需要执行淘汰机制，因此维护了一个LRU链表。

上述哈希表和LRU统一维护在dict_sys_t对象中，相应字段如下：

```c
struct dict_sys_t{  
/* 保护数据字典的互斥锁；也保护基于磁盘的字典系统表；此互斥锁对 CREATE TABLE 和 DROP TABLE 进行序列化，同时还用于从系统表中读取表的字典数据 */  
DictSysMutex mutex;  
/* 要分配的下一个行 ID；请注意，在检查点时，此值必须写入字典系统头并刷新到文件；在恢复过程中，此值必须从日志记录中恢复 */  
row_id_t row_id;  
/* 表名为 key, dict_table_t 对象为 Value 的哈希表 */  
hash_table_t* table_hash;  
/* 表 ID 为 key, dict_table_t 对象为 Value 的哈希表 */  
hash_table_t* table_id_hash;  
/* 数据字典表和索引对象所占用的可变字节空间 */  
lint size;  
/* 系统表 */  
dict_table_t* sys_tables;  
/* 系统列表 */  
dict_table_t* sys-columns;  
/* 系统索引表 */  
dict_table_t* sysIndexes;  
/* 系统索引列表 */  
dict_table_t* sys_fields;  
/* 系统虚拟表 */  
dict_table_t* sys_virtual;  
{/*}}}  
/* 存储表对象的 LRU 链表 */  
UT_LIST_BASE_NODE_T(dist_table_t)  
table_LRU;  
/* 存储表对象的链表，不能被淘汰 */  
UT_LIST_BASE_NODE_T(dist_table_t)  
table_non_LRU;  
/* 用于存储表 ID 和自增值的映射，当表被逐出时 */  
autoinc_map_t* autoinc_map;  
}
```

dict_sys_t结构体的整体架构如图4-2所示，可以直观地看出dict_sys_t结构体对象其实维护了4个核心数据字典表对象，普通的表则维护在table_hash、table_id_hash、table_LRU中。

前面提到，当表对象数量超过特定阈值时会启动淘汰机制，那么这个阈值由什么参数来确定呢？实际上，这一机制复用了MySQL Server层的table_defined_cache参数。在Server层后台线程中，会定期对table_LRU链表的长度进行检查，以确保其不超过table_defined_cache参数所设定的大小。一旦超出，系统将执行表对象的淘汰流程，将不再使用的表从table_LRU、table_hash、table_id_hash等数据结构中移除。

![](images/fd7cf664c9a03888c49a5f58766b1c96aba477107197a3730663ebcf414cf92b.jpg)  
图4-2 dict_sys_t结构体的整体架构

这里大家可能有些疑问，例如：在MySQL需要扫描某表数据时，如何从数据字典中检索其索引信息及存储位置？下面将以聚簇索引的扫描为例进行说明。

在扫描之前会调用如下方法从表对象中获取对应的聚簇索引：

```c
获取表上的第一个索引（聚簇索引）。  
@返回索引，如果不存在则返回NULL*/  
UNIV_INLINE  
dict_index_t*  
dict_table_get_first_index(  
/*--------*/  
const dict_table_t* table) /*!< in: table */  
{  
ut_ad(table);  
ut_ad(table->magic_n == DICT_TABLE_magic_N);  
return(UT_LIST_GET_FIRST((dict_table_t*) table)->indexes));  
}
```

dict_index_t结构体的定义如下：

```javascript
/\*\*索引的数据结构。在dict_mem_index_create()中，大多数字段将被初始化为0、NULL或FALSE\*/struct dict_index_t{/\*索引ID\*/index_id_tid;
```

```c
/\*堆内存空间\*/  
mem_heap_t\* heap;  
/\*索引名称\*/  
id_name_t name;  
/\*表名称\*/  
const char\* table_name;  
/\*指向表的反向指针\*/  
dict_table_t\* table;  
/\*放置索引树的表空间\*/  
unsigned space:32;  
/\*索引树根页编号\*/  
unsigned page:32;  
/\*从开头起足以唯一确定索引条目的字段数量\*/  
unsigned n_uniq:10;  
/\*到目前为止定义的字段数量\*/  
unsigned n_def:10;  
/\*索引中的字段数量\*/  
unsigned n_fields:10;  
/\*可为空字段的数量\*/  
unsigned n_nullable:10;  
/\*字段描述的数组\*/  
dict_field_t\* fields; 
```

由于篇幅原因，这里只列举了部分字段信息。dict_index_t结构体主要依据SYS_INDEXES数据字典表中的记录来构建。通过这些记录，我们可以获取表空间ID，进而定位相应的数据文件。同时，记录中还包含了根页号，这使得我们能够精确地定位到特定的数据页，并从索引根节点开始进行数据扫描。此外，dict_index_t还存储了包括索引ID、索引名称、字段数量等在内的多种信息。

# 4.1.3 MySQL Server层

在InnoDB存储引擎层与MySQLServer层中，均对表对象进行了维护，尽管它们的功能与实现机制各异。MySQLServer层负责维护TABLE_SHARED与TABLE这两种表对象。接下来介绍TABLE_SHARED表对象，它的结构体如下：

```c
struct TABLE_SHARED
{
    /* 指向索引名称的指针 */
    TYPELIB keynames;
    /* 指向列名的指针 */
    TYPELIB fieldnames;
    /* 指向区间信息的指针 */
    TYPELIB *intervals;
    Field **field;
    /* 表索引定义的数据 */
    KEY *key_info;
    /* 字段数组中 BLOB 的索引 */
}
```

```c
uint *blob_field;  
/* 表的注释 */  
LEX_STRING comment;  
/* 压缩算法 */  
LEX_STRING compress;  
/* 加密算法 */  
LEX_STRING encrypt_type;  
/* 字符串类型的默认字符集 */  
const CHARSET_INFO *table_charset;  
/* 指向 db 的指针 */  
LEX_STRING db;  
/* 表名称 */  
LEX_STRING table_name;  
/* frm 文件的路径 */  
LEX_STRING path;  
/* 列的数量 */  
uint fields;  
/* 当前表定义的索引的数量 */  
uint keys;  
/* 唯一索引的数量 */  
uint uniques;  
/* 空字段数量 */  
uint null_fields;  
/* blob 字段类型数量 */  
uint blob_fields;
```

由于篇幅原因，这里只列举了部分字段信息。从上述字段可见，TABLE_SHARED包含了几乎全部的表元数据信息，涵盖了字段和索引等细节。这些信息与.frm文件中的内容大体一致，后续章节将详细阐述.frm文件的结构组成。

注意 与InnoDB表对象的区别在于，TABLE_SHARED是通过解析.frm文件中的信息来构建表对象的，而InnoDB则利用其内部维护的数据字典信息来创建相应的表对象。对于一个使用InnoDB存储引擎的表来说，其打开过程需要在Server层和InnoDB层分别构建相应的表对象。

MySQL在Server层维护了一个哈希表来存储TABLE_SHARED表对象，大小由table_defined_cache参数控制，如下所示：

bool table_def_init(void)   
{ #ifdef HAVE PSI_INTERFACER init_tdc_psi_keys(); #endif mysql_mutex_init(key_LOCK_open, &LOCK_open, MY_MUTEX_INIT_FAST); mysql_cond_init(key_COND_open, &COND_open); oldest_unused_share $\equiv$ &end_of_unused_share;

```c
end_of_unused_share prevailed = &oldest_unused_share;  
if (table_cache管理水平�始())  
{  
    mysql_cond_destroy(&COND_open);  
    mysql_mutex_destroy(&LOCK_open);  
    return true;  
}  
/* 即使其初始化失败，销毁未初始化的哈希表也是安全的。*/  
table_def_inited = true;  
return my_hash_init(&table_def_cache, &mycharset_bin, table_def_size, 0, 0, table_def_key, (my_hash_free_key) table_def_free_entry, 0, key_memory_table_share) != 0; 
```

每次从哈希表中获取表对象时，会主动检查哈希表中的数量是否超过 table_definition_cache 设置的大小，超过后会删除最近未使用的 TABLE_SHARED 表对象，如下所示：

/*如果空闲的缓存太大*/  
while(table_def_cacherecords $>$ table_def_size&&oldest_unused_share->next)my_hash_delete(&table_def_cache，(uchar\*)oldest_unused_share);

了解了 TABLE_SHARED 对象后，下面来介绍 TABLE 表对象，其定义如下：

```c
struct TABLE {
    TABLE_SHARED *s;
    handler *file;
    /* 当前是哪个线程在使用 */
    THD *in_use;
    /* 指向表列信息的指针 */
    Field **field;
    /* 指向记录的指针 */
    uchar *record[2]; /* Pointer to records */
    */
    possible QUICK_keys 是 quick_keys 的超集，用于无连接（join）命令（单表 update 和 delete）的 explain。
```

当解释常规的连接（join）时，我们使用JOIN_TAB::keys来输出possible_keys列的值。然而，对于单表的update和delete命令，它不可用，因为它们在顶层不使用连接优化器。另外，它们直接使用范围优化器，在此处收集所有可用于范围访问的索引。

```c
\*/   
key_map possible QUICK_keys;   
/\* 
```

一组可在引用此表的查询中使用的索引。

在实例化时，将从该集合中减去表的 TABLE_SHARED 上禁用的所有索引（请参阅 TABLE::s）。因此，对于任何表 t，都满足 t.keys_in_use_for_query 是 t.s.keys_in_use 的子集。通常，我们绝不能在此处引入任何新索引（请参阅 setup_tables）。

该集合以位图的形式实现。

```c
\*/  
key_map keys_in_use_for_query;  
/*可用于在不进行排序的情况下计算GROUP BY的索引的映射*/  
key_map keys_in_use_for_group_by;  
/*可用于在不进行排序的情况下计算ORDER BY的索引的映射*/  
key_map keys_in_use_for_order_by;  
/*表定义的索引信息*/  
KEY *key_info;  
/*表的别名*/  
const char *alias;
```

一个或多个查询条件所引用的字段的位图。仅在optimizer_condition_fanout_filter被打开时使用。

目前，仅考虑内连接的 where 子句和 on 子句，但不考虑外连接的 on 条件。

此外，having条件适用于组，因此作为表条件过滤器没有用。

```sql
\*/   
MYBITMAP cond_set; /\* 活跃的读写集合 \*/   
MYBITMAP \*read_set, \*write_set; MDL-ticket \*mdl Ticket; my bool force_index; 
```

基于篇幅原因，这里同样只列举了部分字段信息。根据上述字段分析，可以明确TABLE对象主要负责存储表字段、索引信息以及数据集合，并且包含优化器相关信息。这些信息表明，在Server层执行语句时，TABLE对象与优化器协作，从底层存储中检索数据，并将处理后的结果存储于TABLE对象内，最终返回给客户端。

观察 TABLE 对象的结构，可以发现其中维护了一个指向 TABLE_SHARED 的指针。实际上，TABLE 对象是基于 TABLE_SHARED 构建的，具体实现细节可参见 open_table_from_share 方法。在 MySQL 的 Server 层，表对象通过哈希表进行存储，但为了降低并

发读写操作时锁冲突的影响，这里采用了多个哈希表来分别保存表对象。所有哈希表的总体大小由 table_cache_size 参数进行控制，如下所示：

static bool fix_table_cache_size(sys_var \*self，THD \*thd，enum_var_type type)   
{ /\*   
table_open_cache参数是所有表缓存实例中对象总数的软限制。一旦此值更新，我们需要更新每个实例表 缓存大小的软限制值。 \*/ table_cache_size_per_instance $=$ table_cache_size / table_cache_instances; return false;

table_cache_instances默认为16，所以每个哈希表的大小为table_cache_size/16。

/\*\*初始化表缓存的实例。  
@返回值false-成功。  
@返回值true-失败。\*/  
boolTable_cache::init()  
{mysql_mutex_init(m_lock_key，&m_lock，MY_Mutex_INIT_FAST);m_unused_tables $\equiv$ NULL;m_table_count $= 0$ ·if（my_hash_init(&m_cache，&mycharset_bin,table_cache_size_per_instance，0,0,table_cache_key，（my_hash_free_key）table_cache_free_entry，0,PSI_INSTRUMENT_ME)）{mysql_mutexdestroy(&m_lock);return true;1return false;

同样，当缓存的 TABLE 表对象数量超过 table_cache_key/16 后会进行淘汰，在每次往哈希表中添加表对象的时候触发，淘汰逻辑如下所示：

```txt
/** 
如果表缓存中 TABLE 对象的总数超过了 table_cache_size_per_instance 限制，则释放未使用的 TABLE 实例。 
@ 注意 如果动态更改了 table_cache_size，在此调用期间我们可能需要释放多个实例。 
*/ 
void Table_cache::free_unused_tables_if_required(THD *thd)
```

```txt
{ /\* 
```

我们周围有太多的 TABLE 实例，让我们尝试释放它们。

注意，在服务器运行时，如果动态更改了table_cache_size，我们可能需要释放多个TABLE对象，因此需要下面的循环。

```c
\*/ if (m_table_count > table_cache_size_per_instance && m_unused_tables) { mysql_mutex_lock(&LOCK_open); while (m_table_count > table_cache_size_per_instance && m_unused_tables) { TABLE *table_to_free = m_unused_tables; remove_table(table_to_free); intern_close_table(table_to_free); thd->status_var.table_open_cache_overflows++; } mysql_mutex_unlock(&LOCK_open); } 
```

那么具体的一个表对象应该放在哪个缓存中呢，由如下路由规则控制：

```txt
/\*\*获取特定连接要使用的表缓存实例。\*/   
Table_cache\* get_cache(THD \*thd)   
{ return &m_table_cache[thd->>thread_id() % table_cache_instances];   
}
```

在大致了解了MySQL 5.7的数据字典管理之后，我们简单地总结一下。这里以具体执行一条SQL语句为例，执行如下SQL语句：

```sql
select id, name from mytest; 
```

请注意，这里的mytest是InnoDB存储引擎表。具体流程如下：首先，在MySQLServer层中，会在table_def_cache哈希表中获取mytest对应的TABLE_SHARED表对象。这里可以找到，原因是在启动的时候打开了所有的表并构建了TABLE_SHARED表对象。如果没有找到，则需要进行构建，构建TABLE_SHARED表对象时主要从.frm文件中读取信息。

然后调用 open_table_from_share，通过 TABLE_SHARED 构建 TABLE 表对象，后续对表的相关操作都需要依赖 TABLE 表对象。

到InnoDB层时，会检查dict_sys维护的哈希表中是否有对应的dict_table_t表对象。如果没有，则需要从底层数据字典信息中获取对应的信息来构建。这里的核心就是去SYS_TABLES、SYS_INDEXES、SYS_COLUMNNS、SYS_FIELDS中获取对应的信息。

SYS_TABLES、SYS_INDEXES、SYS_COLUMNS、SYS_FIELDS 在 MySQL 启动的时候会从 ibdata1 系统文件中加载出来并维护在 dict_sys 对象中。

至此，Server层和InnoDB层相应的表对象都构建完成了，后续InnoDB层对表的操作就依赖InnoDB的表对象，Server层对表的操作就依赖Server层的表对象。

# 4.2 .frm 文件

前面我们提到了.frm文件，对于熟悉MySQL的读者而言，这个文件应该不陌生。它位于MySQL的数据目录内，无论是采用MyISAM引擎还是InnoDB引擎的表，都会配备一个相应的.frm文件。该文件由MySQLServer层负责存储表结构、索引等信息，其功能已在前文简要提及。当MySQL执行创建表操作时，表结构信息及索引等数据将记录于.frm文件中。接下来，我们将对.frm文件的格式进行简要介绍，其内部架构如图4-3所示。

![](images/8df331a29fd569ec2f60d8f8847f1a46475d395c236948bc5114cafd2691df23.jpg)  
图4-3 .frm文件内部架构

.frm文件主要包含如下几部分内容：

- Header 区域，长度为 64 B，主要存储.frm 文件版本、存储引擎类型、索引长度等信息。  
□索引信息区域，长度为key_info_length，具体取决于索引的数量和每个索引的长度，主要存储表中所有的索引信息。  
□列信息区域，主要包含各列的元数据信息，例如元数据信息，长度为288B，主要存储screen、enum和set类型、表注释等信息；屏显信息，长度在forminfo中的info_length字段记录，主要存储创建表的语句在屏幕显示的情况；字段信息，每个字段长度为17B，主要存储表中所有字段的元数据信息，例如字段类型、长度等。

其中重点说明Header区域、索引信息区域、列区域元信息和字段信息，各个部分具体存储的内容分别如表4-1～表4-4所示。

表 4-1 Header 区域的详细解释  

<table><tr><td>偏移量</td><td>长度/B</td><td>值</td><td>解释</td></tr><tr><td>0</td><td>1</td><td>fe</td><td>默认为254</td></tr><tr><td>1</td><td>1</td><td>1</td><td>默认为1</td></tr><tr><td>2</td><td>1</td><td>9</td><td>FRM_VER (which is in include/mysql_version.h) +3+test (create_info-&gt;varchar)</td></tr><tr><td>3</td><td>1</td><td>9</td><td>查看sql/handler.h文件中的enum legacy_db_type。例如,09表示DB_TYPE_MYISAM(即MyISAM存储引擎类型),但如果是带有分区功能的MyISAM,则为14。InnoDB则为DB_TYPE_INNODB,值为12</td></tr><tr><td>4</td><td>1</td><td>3</td><td>默认为1</td></tr><tr><td>5</td><td>1</td><td>0</td><td>默认为0</td></tr><tr><td>6</td><td>2</td><td>10</td><td>IO_SIZE,默认大小为4096,表示下一个块从这里开始</td></tr><tr><td>8</td><td>2</td><td>100</td><td>form的数量,总是为1</td></tr><tr><td>000a</td><td>4</td><td>300000</td><td>基于key_length + rec_length + create_info-&gt;extra_size存储所有索引记录的长度</td></tr><tr><td>000e</td><td>2</td><td>1000</td><td>“‘”tmp_key_length”,based on key_length”用于临时存储所有索引记录的长度,如果key_length小于0xffff,存储key_length,否则存储0xffff值</td></tr><tr><td>10</td><td>2</td><td>600</td><td>用于存储rec_length</td></tr><tr><td>12</td><td>4</td><td>0</td><td>create_info-&gt;max_rows</td></tr><tr><td>16</td><td>4</td><td>0</td><td>create_info-&gt;min_rows</td></tr><tr><td>001b</td><td>1</td><td>2</td><td>默认为2</td></tr><tr><td>001c</td><td>2</td><td>800</td><td>key_info_length,存储索引信息长度</td></tr><tr><td>001e</td><td>2</td><td>800</td><td>create_info-&gt;table_options也称为db_create_options吗?其中一个可能的选项是HA LONG BLOB_PTR存储表的选项</td></tr><tr><td>20</td><td>1</td><td>0</td><td>默认为0</td></tr><tr><td>21</td><td>1</td><td>5</td><td>默认为5</td></tr><tr><td>22</td><td>4</td><td>0</td><td>create_info-&gt;avg_row_length,存储平均行长度</td></tr><tr><td>26</td><td>1</td><td>8</td><td>create_info-&gt;default_table_charset,存储字符集</td></tr><tr><td>27</td><td>1</td><td>0</td><td>默认值为0</td></tr><tr><td>28</td><td>1</td><td>0</td><td>create_info-&gt;row_type,存储row_type的值,为枚举类型有ROW_TYPE_DEFAULT,ROW_TYPE_DYNAMIC,ROW_TYPE_COMPRESSED等类型配置。默认为ROW_TYPE_DEFAULT</td></tr><tr><td>29</td><td>6</td><td>00..00</td><td>通常用于支持RAID</td></tr><tr><td>002f</td><td>4</td><td>10000000</td><td>存储key_length,即所有索引的长度</td></tr><tr><td>33</td><td>4</td><td>c0c30000</td><td>来自include/mysql_version.h中的MYSQL_VERSION_ID,存储MySQL版本信息</td></tr><tr><td>37</td><td>4</td><td>10000000</td><td>create_info-&gt;extra_size,存储额外的数据</td></tr><tr><td>003b</td><td>2</td><td>0</td><td>留作extra_rec_buf_length使用</td></tr><tr><td>003d</td><td>1</td><td>0</td><td>保留为default_part_db_type,但如果MyISAM带有分区,则为09</td></tr><tr><td>003e</td><td>2</td><td>0</td><td>create_info-&gt;key_block_size,存储key_block_size,默认为0</td></tr></table>

表 4-2 索引信息区域的详细解释  

<table><tr><td>偏移量</td><td>长度/B</td><td>值</td><td>解释</td><td>说明</td></tr><tr><td>0</td><td>1</td><td>key_count</td><td>存储索引的数量</td><td rowspan="5">存储索引相关属性信息,多个索引重复存储</td></tr><tr><td>1</td><td>1</td><td>key_parts</td><td>作用于索引的字段数量</td></tr><tr><td>2</td><td>1</td><td>0</td><td>0</td></tr><tr><td>3</td><td>1</td><td>0</td><td>0</td></tr><tr><td>4</td><td>2</td><td>length</td><td>存储索引名称的长度</td></tr><tr><td>6</td><td>2</td><td>??</td><td>存储索引的flag</td><td rowspan="5">存储索引的字段信息,多个字段重复存储</td></tr><tr><td>8</td><td>2</td><td>key_length</td><td>存储索引的长度</td></tr><tr><td>10</td><td>1</td><td>user_defined_key_parts</td><td>存储索引的字段数量</td></tr><tr><td>11</td><td>1</td><td>algorithm</td><td>存储索引的算法</td></tr><tr><td>12</td><td>2</td><td>block_size</td><td>存储索引的block_size,默认为0</td></tr><tr><td>14</td><td>2</td><td>key_part-&gt;fieldnr+1+FIELD_NAMEusoED</td><td>存储字段在表中的编号</td><td rowspan="6">存储索引的name,多个索引重复存储</td></tr><tr><td>16</td><td>2</td><td>offset</td><td>存储字段的偏移量</td></tr><tr><td>18</td><td>1</td><td>-</td><td>存储常量值为0</td></tr><tr><td>19</td><td>2</td><td>key_part-&gt;key_type</td><td>存储字段的类型</td></tr><tr><td>21</td><td>2</td><td>key_part-&gt;length</td><td>存储字段的长度</td></tr><tr><td>23</td><td>1</td><td>NAMES_SEP_CHAR</td><td>NAMES_SEP_CHAR</td></tr><tr><td>24</td><td>xxx</td><td>key-&gt;name</td><td>key-&gt;name</td><td rowspan="2">存储索引的注释,多个索引重复存储</td></tr><tr><td></td><td>1</td><td>NAMES_SEP_CHAR</td><td>NAMES_SEP_CHAR</td></tr><tr><td></td><td>1</td><td>0</td><td>0</td><td rowspan="3"></td></tr><tr><td></td><td>2</td><td>comment.length</td><td>存储索引注释的长度</td></tr><tr><td></td><td>xxx</td><td>comment.str</td><td>存储索引的注释</td></tr></table>

表 4-3 列区域元信息的详细解释  

<table><tr><td>偏移量</td><td>长度/B</td><td>值</td><td>解释</td></tr><tr><td>0</td><td>2</td><td>length</td><td>存储 forminfo 总长度</td></tr><tr><td>2</td><td>2</td><td>maxlength</td><td>存储 forminfo 最大长度</td></tr><tr><td>xx</td><td>xx</td><td>xx</td><td>xx</td></tr><tr><td>46</td><td>1</td><td>create_info-&gt;comment.length</td><td>create_info-&gt;comment.length</td></tr><tr><td>47</td><td>xxx</td><td>comment.str</td><td>存储表的注释</td></tr><tr><td>xx</td><td>xx</td><td>xx</td><td>xx</td></tr><tr><td>256</td><td>2</td><td>screens</td><td>存储 screens 的数量，如果一屏能显示完全，则存储为 1</td></tr><tr><td>258</td><td>2</td><td>create_fieldselements</td><td>存储创建表的字段数量</td></tr><tr><td>260</td><td>2</td><td>info_length</td><td>screen section 的长度</td></tr><tr><td>262</td><td>2</td><td>totlength</td><td>所有字段总的长度</td></tr><tr><td>264</td><td>2</td><td>no_empty</td><td>field-&gt;unireg_check = Field::NO EMPTY或 field-&gt;unireg_check &amp; MTYP_NOEMPTY_BIT 字段数</td></tr><tr><td>266</td><td>2</td><td>reclength</td><td>所有字段记录长度之和</td></tr><tr><td>268</td><td>2</td><td>n_length</td><td>所有列名称的长度+字段数量</td></tr><tr><td>270</td><td>2</td><td>int_count</td><td>enum、set类型字段的数量</td></tr><tr><td>272</td><td>2</td><td>int_parts</td><td>enum、set类型字段中选项的数量</td></tr><tr><td>274</td><td>2</td><td>int_length</td><td>enum、set类型所有选项的长度</td></tr><tr><td>276</td><td>2</td><td>time_stamp_pos</td><td>time stamp_pos</td></tr><tr><td>278</td><td>2</td><td>80</td><td>存储screen的列数</td></tr><tr><td>280</td><td>2</td><td>22</td><td>存储screen的行数</td></tr><tr><td>282</td><td>2</td><td>null_fields</td><td>存储表中定义空列的数量</td></tr><tr><td>284</td><td>2</td><td>com_length</td><td>字段注释长度</td></tr><tr><td>286</td><td>2</td><td>gcol_info_length</td><td>虚拟列信息长度</td></tr></table>

表 4-4 字段信息的详细解释  

<table><tr><td>偏移量</td><td>长度/B</td><td>值</td><td>解释</td><td>说明</td></tr><tr><td>0</td><td>1</td><td>field-&gt;row</td><td>字段名称显示在屏幕第几行</td><td rowspan="13">存储字段相关属性信息,多个字段重复存储</td></tr><tr><td>1</td><td>1</td><td>field-&gt;col</td><td>字段名称在屏幕显示占用的宽度</td></tr><tr><td>2</td><td>1</td><td>field-&gt;sc_length</td><td>字段值在屏幕显示占用的宽度</td></tr><tr><td>3</td><td>2</td><td>field-&gt;length</td><td>字段值最大长度</td></tr><tr><td>5</td><td>3</td><td>recpos</td><td>字段在一行中的偏移量</td></tr><tr><td>8</td><td>2</td><td>field-&gt;pack_flag</td><td>字段的标识</td></tr><tr><td>10</td><td>1</td><td>field-&gt;unireg_check</td><td>支持TIMESTAMP类型使用NOW( )作为默认值,引入unireg类型,在该字段中存储一些字段的属性值</td></tr><tr><td>11</td><td>1</td><td>field-&gt;charset-&gt;number &gt;&gt; 8</td><td>存储字符集</td></tr><tr><td>12</td><td>1</td><td>field-&gt;interval_id</td><td>存储enum、set类型字段中选项列表ID</td></tr><tr><td>13</td><td>1</td><td>field-&gt;sql_type</td><td>存储字段类型</td></tr><tr><td>14</td><td>1</td><td>field-&gt;charset-&gt;number</td><td>存储geometry类型字段的字符集</td></tr><tr><td>15</td><td>2</td><td>field-&gt;comment.length</td><td>存储字段注释长度</td></tr><tr><td>17</td><td>1</td><td>NAMESSEPCHAR</td><td>分隔符</td></tr><tr><td>18</td><td>xxx</td><td>field-&gt;field_name</td><td>field-&gt;field_name</td><td rowspan="3">存储字段名称,多个字段重复存储</td></tr><tr><td></td><td>1</td><td>NAMES_SEP_CHAR</td><td>分隔符</td></tr><tr><td></td><td>xxx</td><td>comment.str</td><td>存储字段注释</td></tr></table>

屏显信息在此不做介绍，感兴趣的读者可以自行参考pack Screens方法。根据前述说明，显而易见，.frm文件中保存的有表的全部元数据信息。在MySQL Server层，仅需将.frm文件载入内存并创建相应的表对象，即可获取表内所有信息以执行相关操作。然而，自MySQL 8.0起，.frm文件已被废弃。其主要原因是MySQL 8.0开始采用InnoDB存储引擎统一存储数据字典，从而在Server层不再保留.frm文件。这一改变意味着MySQL只需维护单一的数据字典信息，从而避免了在执行DDL操作时Server层与InnoDB层数据字典不一致的问题。

# 4.3 数据字典的使用

前面我们已经对数据字典的结构及加载流程有所了解。接下来，本节将深入探讨数据字典在InnoDB存储引擎中的应用。由于数据字典本质上由四个核心字典表组成的，因此，对于表的操作基本可以归纳为创建（增）、删除（删）、修改（改）和查询（查）等。各项操作的应用场景如下所示：

□ 创建：创建表的时候。  
□删除：删除表、索引的时候。  
□修改：修改索引信息、列信息的时候。  
□查询：查询用户表触发加载的时候。

由于篇幅问题，这里重点介绍下创建和查询。通过这两个操作，我们基本就能够了解在InnoDB引擎中是如何使用数据字典的。

# 4.3.1 创建表

创建表的时候，MySQL会生成表记录并插入数据字典表中。例如，我们创建一个表：

```sql
create table zbddb(id int, primary key(^id)); 
```

这条命令首先会生成 SYS_TABLES 表记录并插入，生成的记录详细字段如表 4-5 所示。

表 4-5 SYS_TABLES 生成的记录详细字段  

<table><tr><td>字段</td><td>值</td></tr><tr><td>NAME</td><td>zbdba/zbdba</td></tr><tr><td>ID(table id)</td><td>9489</td></tr><tr><td>N_COLS</td><td>ROW_FORMAT = (N_COLS &gt;&gt; 31) ? COMPACT: REDUNDANT</td></tr><tr><td>TYPE (table flags)</td><td>33</td></tr><tr><td>MIX_ID (obsolete)</td><td>—</td></tr><tr><td>MIX_LEN (additional flags)</td><td>80</td></tr><tr><td>CLUSTER_NAME</td><td>(默认为空,无实际意义)</td></tr><tr><td>SPACE</td><td>9992</td></tr></table>

在完成 SYS_TABLES 表记录插入之后，会生成数据插入 SYS_COLUMNS 中，生成的数据详细字段如表 4-6 所示。

表 4-6 SYS_COLUMNS 生成的数据详细字段  

<table><tr><td>字段</td><td>值</td></tr><tr><td>TABLE_ID</td><td>9489</td></tr><tr><td>POS</td><td>0</td></tr><tr><td>NAME</td><td>id</td></tr><tr><td>MTYPE</td><td>6 (DATA_INT)</td></tr><tr><td>PRTYPE (MySQL DATA TYPE、charset code、flag)</td><td>1283</td></tr><tr><td>LEN (Column Len)</td><td>4</td></tr><tr><td>PREC</td><td>0</td></tr></table>

注意这里因为例子中的表只有一列，所以只有一行数据。如果表有多列，那么SYSantiumS表中对应就有多行数据。

SYS_INDEXES 的详细字段如表 4-7 所示，在创建表的时候如果表中有索引的话，就会生成对应的记录插入 SYS_INDEXES 表。

表 4-7 SYS_INDEXES 的详细字段  

<table><tr><td>字段</td><td>值</td></tr><tr><td>TABLE_ID</td><td>9489</td></tr><tr><td>ID(索引ID)</td><td>62249</td></tr><tr><td>NAME(索引名称)</td><td>PRIAMRY</td></tr><tr><td>N_FIELDS(索引字段数量)</td><td>1</td></tr><tr><td>TYPE(索引类型)</td><td>3</td></tr><tr><td>SPACE(表空间ID)</td><td>9992</td></tr><tr><td>PAGE_NO(索引根节点页)</td><td>0xFFFFFF(初始化值,后续创建索引会更新该值)</td></tr><tr><td>MERGE_threshold(索引合并阈值)</td><td>50</td></tr></table>

本例中只有一个聚簇索引，所以这里只有一条记录，如果有多个索引，这里会有多条记录。请注意，如果表中没有任何索引，在InnoDB引擎中还是会创建一个聚簇索引，因为

InnoDB底层的数据是由聚簇索引组织的，所以最终还是会向SYS_INDEXES中插入一条记录，隐藏的聚簇索引名为GEN_CLUST_INDEX。

我们可以看到，SYS_INDEXES中没有列具体信息，索引的列信息是存储在SYS_FIELDS中的。下面介绍SYS_FIELDS的详细字段，如表4-8所示。

表 4-8 SYS_FIELDS 的详细字段  

<table><tr><td>字段</td><td>值</td></tr><tr><td>INDEX_ID</td><td>62249</td></tr><tr><td>POS</td><td>0</td></tr><tr><td>COL_NAME</td><td>id</td></tr></table>

至此，所有的数据字典都插入了对应的数据字典表中，通过上面插入的记录，我们可以看到表中所有的信息。

# 4.3.2 查询表

了解了数据字典增加的过程后，再来看看数据字典的使用过程。在执行如下语句时：

```sql
select * from zbdba.zbdba; 
```

在InnoDB底层会打开zbdba.zbdba表，在打开之前需要从数据字典中查询对应的信息。具体流程如下：

1）通过表名去dict_sys_t对象维护的哈希表中查找是否存在对应的表对象，如果不存在则需要从SYS_TABLES中查找对应的记录，匹配到记录之后会创建dict_table_t对象。  
2）将dict_table_t表对象加入到数据字典cache中，这个cache就是前面介绍的dict_sys_t对象维护的table_hash和table_id_hash哈希表。并且根据是否可以淘汰加入到dict_sys_t维护的table_LRU或table_non_LRU链表中，用于后续进行表对象的淘汰。  
3）依据从 SYS_TABLES 中检索到的表 ID，在 SYS_COLUMNS 中执行查询操作，以匹配相应的记录。将匹配到的字段信息存储至 dict_table_t 对象所维护的 cols 数组中。  
4）在 SYS_INDEXES 表中，依据从 SYS_TABLES 获取的 table id 执行查询操作，一旦匹配到相应记录，便会创建 dict_index_t 索引对象，并将其插入由 dict_table_t 对象管理的 indexes 链表中。  
5）在从 SYS_INDEXES 获得索引记录的之后还需要获取该索引对应的字段信息，就根据索引的 id 从 SYS_FIELDS 中获取该索引对应的字段信息，拿到对应的记录之后就插入 dict_index_t 索引对象维护的 fields 列数组中。

至此，数据字典信息的加载工作已全部完成。可见，在内存中维护了一个名为dict_table_t的表对象，其中包含了表列和索引的相关信息。在后续对表进行操作时，只需直

接获取该dict_table_t表对象。表对象中还存储了表空间ID和聚族索引的根节点页信息，有了这些信息，我们便可以对底层数据文件进行数据扫描。

# 4.3.3 rowid

在先前章节中，我们已经提及dict_sys_t结构体中对rowid的维护。本小节将对rowid的概念进行简要阐述。此处所指的rowid是InnoDB表内部的一个系统列。当InnoDB表未设置主键时，底层会自动创建一个聚簇索引。聚簇索引中的每条记录均包含一个rowid值，并且聚簇索引的排序依据正是rowid。rowid值具有全局唯一性，为所有表所共享。

在数据插入过程中，系统会采取内部互斥锁机制，以获取当前的rowid值。一旦获取完成，系统会将该rowid值递增1，随后释放相应的互斥锁。因此，对于无主键的表，在高并发环境下插入数据时，这一机制可能会对性能产生影响。

了解rowid存储于数据字典头部之后，接下来的问题是如何实现rowid的持久化。在前述获取rowid的方法中，存在以下判断逻辑：

```c
if (0 == (id % DICT_HDR_ROW_ID_WRITEMargins)) { dict hdr Flush_row_id(); 
```

即每隔 DICT_HDR_ROW_ID_WRITEMargins 的间隔进行一次数据持久化，其中 DICT_HDR_ROW_ID_WRITEMargins 的数值设定为 256。在持久化过程中，最新的 rowid 值会被记录到数据字典头部，即更新相应的系统数据页。随后，与此次操作相关的信息会被写入重做缓冲区中。后台线程将异步地将重做缓冲区和脏页数据同步至磁盘，以完成持久化操作。对于可能存在的疑问，如在 MySQL 崩溃前未能及时完成持久化是否会导致 rowid 出现重复，MySQL 在启动时已通过特定设置来避免此类情况发生。

```c
dict_sys->row_id = DICT_HDR_ROW_ID_WRITEMargins + ut_uint64_align_up(mach_read_from_8(dist hdr + DICT_HDR_ROW_ID), DICT_HDR_ROW_ID_WRITEMargins); 
```

通过上述方法，在读取rowid的基础上增加一个范围值，便能避免与先前的rowid发生冲突，从而防止数据覆盖。此过程与事务ID持久化的方式相似，然而，区别在于事务ID的处理中加入了双倍的范围值，而rowid仅增加了一倍的范围。其原因在于rowid的申请与持久化是在同一个函数中顺序执行的，一旦触发了持久化操作，若未完成，则无法继续分配新的rowid。

至此MySQL5.7数据字典已经全部介绍完成，下面我们简单地总结一下。在本章最开始介绍了数据字典分别在文件层、InnoDB存储引擎层、Server层是如何存储和管理的：

□在文件层主要是ibdata1存储了InnoDB存储引擎的数据字典信息以及数据字典头信息，然后在.frm文件中存储了Server层的数据字典信息。  
□在InnoDB存储引擎层主要介绍了它是如何管理数据字典信息和表对象的，我们知

道它维护了table_name和table_id两个哈希表以及table_LRU和table_non_LRU两个链表用来管理表对象，这其实就是LRU Cache的实现，大小由table_defined_cache参数控制。表对象的信息则需要从底层的数据字典表进行加载。

□在Server层，我们了解到它实际上负责管理一系列的表对象。这些表对象的数据字典信息是从.frm文件中读取的。Server层的表对象分为两个层面：全局表对象TABLE_SHARED和会话级别的表对象TABLE。这两个层面的表对象均采用类似LRU缓存机制进行管理。其中，TABLE_SHARED缓存的大小由table_definedcache参数控制，而TABLE缓存的大小则由table_cache_size参数进行调节。

接下来，详细阐述了.frm文件的相关内容。.frm文件结构较为复杂，它包含了数据库表的所有元数据信息。然而，自MySQL8.0版本起，.frm文件已被弃用。在MySQL8.0版本之前，.frm文件在MySQLServer层扮演着关键角色，所有需要持久化存储的存储引擎都配有相应的.frm文件。由于不同存储引擎的实现方式各异，它们各自维护了数据字典信息，这导致了存在两套数据字典，可能在执行DDL操作时引发数据字典不一致的问题。

然后用举例的方式介绍了InnoDB存储引擎对数据字典的管理，主要列举了在创建表和查询表的时候对数据字典的操作：

□ 创建表的时候其实就是将创建表语句进行语法解析器解析后的结果得到表相应的字段、索引等信息，然后插入对应的数据字典表即可。  
□查询表的时候其实就是看是否有缓存对应的表对象信息，没有的话则需要从磁盘中查询对应的数据字典表信息，然后在内存中创建表对象的，然后再插入缓存中。

最后还附带介绍了rowid，由于它是存储在数据字典头中的一个字段，在rowid小节中我们了解到如果多个表没有主键，在高并发插入的时候会造成互斥锁等待。

通过上述介绍的信息，相信大家对MySQL的数据字典有了深刻的认识，不过在MySQL8.0我们所了解到的这一切将基本推翻，因为MySQL8.0对数据字典进行了非常大的重构工作，在下面的章节中会详细介绍。

# 4.4 MySQL 8.0 数据字典

在前面章节中，我们探讨了MySQL8.0版本之前的数据字典。本节将深入探讨MySQL8.0之后的数据字典。在MySQL8.0中，数据字典经历了重构，重构的主要原因在于MySQLServer层和存储引擎层各自维护独立的数据字典信息，导致实现复杂，且存在数据冗余问题，特别是在执行DDL操作时难以保证操作的原子性。在MySQL8.0中，数据字典与系统表实现了统一管理，并且默认采用InnoDB存储引擎。MySQL8.0数据字典整体架构如图4-4所示。

![](images/3f807ff57b42533b0ca14ba174286f50d3e88944d0eb54e5f305824e5d6552cd.jpg)  
图4-4 MySQL8.0数据字典整体架构

在MySQL 8.0版本中，所有数据字典表的信息均存储于名为mysql.ibd的文件内。每张数据字典表均配有相应的序列化数据字典信息（serialized Dictionary Information，SDI），而每个SDI又对应一个索引。该索引中记录的内容实际上是一个JSON格式的文件，详细记录了数据字典的全部信息。mysql.ibd文件的头部也包含该数据文件中所有表对应的SDI信息。SDI信息可以视为数据字典表的备份，以JSON格式存储。关于SDI信息的具体内容，将在后续章节中详述。

mysql. ibd 文件中有一个名为 dd_property 份的表，该表是所有表的元数据起源，记录了数据字典表的索引根节点页等关键信息。MySQL 在启动时首先加载此表，以便读取其他数据字典表的信息，从而加载出所有表的数据字典信息。

在获取表的数据字典信息时，MySQL 8.0 并未沿用先前 5.7 版本的逻辑，而是实现了一套数据字典缓存机制。其核心思想是构建多层缓存架构，最终仍需查询 mysql.ibd 文件中的对应数据字典表。在图 4-4 中，数据字典存储适配器负责从 mysql.ibd 文件中获取数据字典信息，而全局共享数据字典缓存则用于存储数据字典信息。最上层的数据字典客户端位于用户线程中，缓存该用户线程所使用的表的数据字典信息。

MySQL 8.0 不再依赖.frm 文件，表数据字典信息存储两份：一份存在数据字典表中，

也就是 mysql. ibd 文件中；一份存在 SDI 中，位于该表数据文件的 SDI 索引。MySQL 5.7 和 8.0 版本的数据字典的主要区别如下：

存储位置不同。在MySQL 5.7版本中，数据字典的核心信息主要保存于系统表空间内，其中数据字典表的结构以及索引的根节点页号均被硬编码于程序代码之中。相比之下，MySQL 8.0版本的数据字典信息则被存储于名为mysql.ibd的文件内，尽管数据字典表结构信息依旧被硬编码在程序代码里，但索引的根节点页号信息则被保存在名为dd_propertyst的表中。  
□ 获取方式不同。在MySQL 5.7版本中，获取表的数据字典信息是通过直接查询数据字典表的索引来实现的，遵循常规的查询流程。而到了MySQL 8.0版本，引入了双层数据字典缓存机制，查询时会首先在缓存中查找所需的数据字典信息，不过最终数据的检索仍然依赖于各个数据字典表的索引。

# 4.4.1 文件存储层

本小节将深入探讨存储在 mysql ibd 文件内的这些数据字典表的具体组织结构。首先，让我们看下 mysql.ibd 文件所包含的全部表。

```txt
dd_propertyss  
innodb_dynamic_metadata  
innodb_ddl_log  
catalogs  
character_sets  
collations  
column_statistics  
column_type_elements  
columns  
events  
foreign_key_columnusage  
foreign_keys  
index_columnusage  
index_partitionitions  
index.stats  
indexes  
parameter_type_elements  
parameters  
resource_groups  
routines  
schemata  
st_spatial_reference_systems  
table_partition_values  
table_partitionss  
table.stats  
tables  
tablespace_files  
tablespaces 
```

```txt
triggers viewRoutineusage view_tableusage 
```

可以看到，mysql. ibd 文件中包含很多数据字典表信息，这里我们重点介绍其中四项。

第一项是 tables。它存储所有表的表信息，对应 MySQL 5.7 中的 SYS_TABLES 表，其结构为：

```txt
mysql> select * from mysqltables limit 1\G  
********** 1. row ***  
id: 1  
schema_id: 1  
name: dd_propertystypes: BASE TABLE  
engine: InnoDB  
mysql_version_id: 80019  
row_format: Dynamic  
collation_id: 83  
comment:  
hidden: System  
options: avg_row_length=0;encrypt_type=N;explicit_tablespace=1;key_block_size=0;keysdisabled=0;pack_record=1;row_type=2;statsauto_recalc=0;statsPersistent=0;stats_sample_pages=0;  
se_private_data: NULL  
se_private_id: 1  
tablespace_id: 1  
partition_type: NULL  
partition_expression: NULL  
partition_expression utf8: NULL  
default_partitioning: NULL  
subpartition_type: NULL  
subpartition_expression: NULL  
subpartition_expression utf8: NULL  
default_subpartitioning: NULL  
created: 2023-03-04 03:26:58  
last_altered: 2023-03-04 03:26:58  
view_defined: NULL  
view_defined utf8: NULL  
view_check_option: NULL  
view_is_updatable: NULL  
view_algorithm: NULL  
view_security_type: NULL  
view_definer: NULL  
view_client_collation_id: NULL  
view_connectivitycollation_id: NULL  
view_column_names: NULL  
last_checked_for upgrade_version_id: 0 
```

第二项是columns。它存储所有表的列信息，对应MySQL5.7中的SYS.columns表，其结构为：

mysql>select \*from mysql.columns limit 1\G   
********** 1. row**********   
id:1 table_id:1 name:properties   
ordinal_position:1 type:MYSQL_TYPE_MEDIUM_BLOB is_nullable:1 is_zerofill:0 is_unsigned:0 char_length:16777215   
numeric_precision:0 numeric_scale:NULL datetime_precision:NULL collation_id:63 has_no_default:0 default_value:NULL default_value utf8:NULL default_option:NULL update_option:NULL is.auto_increment:0 is_virtual:0   
generation_expression:NULL   
generation_expression utf8:NULL comment: hidden:Visible options:interval_count $= 0$ se_private_data:table_id $= 1$ column_key: column_type utf8: mediumblob srs_id:NULL is_explicit_collation:1   
1 row in set (0.00 sec)

第三项是indexes。它存储所有表的索引信息，对应MySQL5.7中的SYS_INDEXES表，其结构为：

```txt
mysql>select \*from mysql.indexes limit 1\G   
********** 1. row ****** 
```

```ini
hidden: 1  
ordinal_position: 1  
comment:  
options: NULL  
se_private_data: id=1; root=4; space_id=4294967294; table_id=1;trx_id=0;  
tablespace_id: 1  
engine: InnoDB  
1 row in set (0.00 sec) 
```

第四项是 index_columnusage。它存储所有表索引的字段信息，对应MySQL 5.7 中的 SYS_FIELDS 表，其结构为：

```txt
mysql> select * from mysql.index_column_USAGE limit 1\G  
********** 1. row ***  
index_id: 1  
ordinal_position: 1  
column_id: 2  
length: NULL  
order: ASC  
hidden: 1  
1 row in set (0.01 sec) 
```

可以看出，数据库的所有数据字典信息主要还是存储在这四张表中的。这跟MySQL5.7类似，不过其中的一些字段有些区别，并且这四张表存储在mysql.ibd文件中，MySQL5.7对应的四张表是存储在系统数据文件中的。

注意 查看上述数据字典表信息时需要执行如下语句：

SET SESSION debug='+d,skip dd table access check';

否则会报错：

```txt
mysql>  
mysql> show create table mysqltables\G  
ERROR 3554 (HY000): Access to data dictionary table 'mysql_tables' is rejected. 
```

在MySQL数据库中，.ibd文件负责存储具体的数据内容。表的结构定义则保存在数据字典中。与MySQL5.7版本一样，这些数据字典表结构信息实际上是硬编码在代码内部的。

要获取数据字典表中的信息，除了表结构信息和表数据外，还需要知道聚簇索引的根节点页号。有了这些信息，我们便能定位到表在数据文件中的确切位置，进而读取聚簇索引的根节点页。一旦获取到聚簇索引的根节点页，就可以开始扫描索引中的数据。实际上，数据字典的聚簇索引根节点页号是存储在 dd_property 表中的。接下来，我们将详细探讨 dd_property 表所保存的具体信息。首先介绍 dd_property 的表结构信息：

```sql
mysql> show create table mysql.dd.properties; 
```

Table | Create Table   
1   
+   
+   
| dd_propertyles | CREATE TABLE `dd_propertyles` ( 'properties` mediumblob   
) /\*!50100 TABLESPACE `mysql` */ ENGINE=InnoDB DEFAULT CHAREN=utf8 COLLATE=utf8_ bin STATS_PERSISTENT $\equiv$ 0 ROW_format $\equiv$ DYNAMIC   
+   
1 row in set (0.01 sec)

可以看到 dd_property 表只有一个 properties 字段，并且是 blob 类型的，这里就不查询了，因为查出来也是二进制格式，不方便查看内容，这里直接说明一下：

dd_property 以键值的形式存储各个系统表的名字和表对应的属性，其中值包含每个表的 root page number、index id、space id 等私有数据，除此之外，还包含其他类型的数据，例如 SDI_VERSION、LCTN、MYSQL_VERSION_LO、MYSQL_VERSION_HI、MYSQL_VERSION、MINOR_DOWNGRADE_threshold、MYSQL_VERSION_UPGRADED 等。

dd_propertites表中存储的关键内容就是各个表的私有数据，包含索引的根节点页信息，拿到这个信息就可以找到索引根节点页的位置，从而读取数据了。

至此，数据字典的文件存储就介绍完毕了，这里总结如下：

□数据字典表的数据存储在 mysql. ibd 文件中。  
□数据字典表的表结构信息硬编码在代码中。  
□数据字典聚簇索引根节点页号存储在dd_propertyst表中。

大家可能还想知道 dd_property 表的聚簇索引根节点页存储在哪里，其实它是硬编码在代码中的，这个就是“先有鸡还是先有蛋”的问题了。

# 4.4.2 数据字典缓存

前面提到在MySQL8.0中引入了数据字典缓存，本小节将详细介绍整个缓存的设计和访问流程。数据字典缓存分为如下三层：

□数据字典存储适配器（Storage_adapter)，从mysql. ibd中读取数据字典表信息。  
□全局共享数据字典缓存（Shared dictionary_cache)，全局的数据字典cache，用于缓存Storageadapter读取的结果信息。  
□数据字典客户端（Dictionary_client），位于每个用户线程中，缓存用户线程用到表的数据字典信息。

下面将详细介绍每个部分的设计和详细流程。

# 1. 数据字典存储适配器

数据字典存储适配器的主要作用是从各个数据字典表中获取对应的信息，把这些信息组装成对应的数据字典对象提供给上层的用户线程使用，其主要逻辑位于sql/dd/impl/cache/storageadapter.cc文件，这里介绍3个重要的方法：

□ Storage_adapter::get。从数据字典表读取数据字典信息，从 tables 表获取表对应的数据字典信息，调用 restore_object_from_record 方法分别从 ddproperties、indexes、foreign_key、partitions、triggers、checkConstraints 获取对应的信息。最终生成 ddobjects 对象，ddobjects 对象存储了整个表的数据字典信息。  
□ Storage_adapter::drop。从数据字典表中删除数据字典信息，删除存储在 tables、dd_propertyles、indexes、foreign_key、partitions、triggers、 check Constraints 等数据字典表中的数据字典信息。  
□ Storage_adapter::store。将数据字典信息存储在对应数据字典表中，存储在 tables、dd_property、indexes、foreign_key、 partitions、triggers、check Constraints 等表中，在创建表或者更改表时会调用该逻辑进行数据字典信息的存储或者修改。

如下是Storage_adapter:::get方法相关的代码，感兴趣的读者可自行研究：

//从持久存储中获取一个字典对象

```cpp
template <typename K, typename T> bool Storage_adapter::get(THD *thd, const K &key, enum_tx_isolation isolation, bool bypass_coreregistry, const T **object) {
    DEBUG-AssERT(object);
    *object = nullptr;
    if (!bypass_coreregistry) {
        instance()->core_get(key, object);
        if (*object || s_use Fake_storage) return false;
    }
} 
```

// 在服务器启动期间检查现有表时，我们可能会出现缓存未命中的情况。在这个阶段，该对象将被视为不存在。

```rust
if (bootstrap::DD.bootstrapCtx::instance().get_stage() < bootstrap::Stage::CREATED_TABLES) return false; 
```

// 启动一个 DD 事务以获取该对象。

```txt
Transaction_ro trx(thd, isolation);  
trx.otox.register_tables();  
if (trx.otox.open_tables()) {  
    DBUG-AssERT(thd->is_system_thread() || thd->killed || thd->is_error());  
    return true; 
```

}   
const Entity_object_table &table = T::DD_table::instance(); // Get main object table. Raw_table \*t =trx.oth.get_table(table.name()); // 通过对象ID查找记录。 std::unique_ptr<Raw_record> r; if (t->find_record(key,r)) { BUG-assERT(thd->is_system_thread() || thd->killed || thd->is_error()); return true; }   
//从记录中恢复对象。 Entity_object *new_object $=$ NULL; if (r.get()) && tablerestore_object_from_record(&trx.othx,\*r.get(),&new_object)){ BUG-assERT(thd->is_system_thread() || thd->killed || thd->is_error()); return true; }   
//如果动态类型转换失败，则删除新对象。 if (new_object){ //在此，动态类型转换失败并非合法情况。 //在生产环境中，我们会报告错误。 \*object $=$ dynamic cast<T $\ast >$ (new_object); if (!\*object){ /\*purecov:begin inspected \*/ my_error(ER_INVALID_DD_OBJECT,MYF(0),new_object->name().c_str()); delete new_object; BUG-assERT(false); return true; /\*purecov:end \*/ } }   
return false;

# 2. 全局共享数据字典缓存

共享数据字典缓存是为所有用户线程提供服务的缓存机制，允许它们从中检索相应的数据字典信息。若在该缓存中未找到所需信息，即发生缓存未命中，则会启动Storage_adapter::get方法，从mysql.ibd文件中读取所需的数据字典信息，并随后将这些信息存入缓存中。接下来，我们将详细探讨缓存的具体实现方式。

Shared_dictionaries_cache其实是基于std::map实现的，MySQL创建了如下几种类别的映射：

```txt
Sharedmulti_map<Abstract_table> mabstract_table_map; 
```

```txt
Sharedmulti_map<Charset> m_charetmap;   
Sharedmulti_map<Collation> m_collation_map;   
Sharedmulti_map<Column_statistics> m_column_stat_map;   
Sharedmulti_map<Event> m_event_map;   
Sharedmulti_map<Resource_group> m_resource_group_map;   
Sharedmulti_map<Routine> m_routine_map;   
Sharedmulti_map<Schema> m_schema_map;   
Sharedmulti_map<Spatial_reference_system> m_spatial_reference_system_map;   
Sharedmulti_map<Tablespace> m_tablespace_map; 
```

m_ABSTRACT_table_map 是一个通用的映射表，用于存储大多数数据字典表和用户表的信息。其他映射表则分别存储与之对应的数据字典表信息。例如，m charset_map专门用于存储与字符集相关的数据字典信息。实际上，上述 Shared-multi_map 在底层维护了多个映射表，这些映射表以 id、name、aux 作为键值。因此可以通过表名或表 ID从映射表中检索到相应的数据字典对象。每个不同的映射表对应不同的数据字典对象，而这些数据字典对象包含了所需的所有数据字典信息。以 m_ABSTRACT_table_map 为例，它存储的数据字典对象包含如下信息：

//字段

```c
Object_id m_se_private_id;  
String_type m_engine;  
String_type m/comment; 
```

// 将此值设置为 0 意味着每个表都将通过 CHECK TABLE FOR UPGRADE 检查一次，即使它是在这个版本中创建的。

// 如果我们改为初始化为 MSQL_VERSION_ID，则只有在真正升级后才会运行 CHECK TABLE FOR UPGRADE

```txt
uint m_last_checkeded_for_upgrade_version_id = 0;  
Properties Impl m_se_private_data;  
enum_row_format m_row_format;  
bool m is temporary; 
```

// - 分区相关字段。

```txt
enum_partition_type m_partition_type;   
String_type m_partition_expression;   
String_type m_partition_expression utf8;   
enum_default_partitioning m_default_partitioning;   
enum_subpartition_type m_subpartition_type;   
String_type m_subpartition_expression;   
String_type m_subpartition_expression utf8;   
enum_default_partitioning m_default_subpartitioning; 
```

//对紧密耦合对象的引用。

```txt
Index.collection m_Indexes;   
Foreign_key.collection m_foreign_keys;   
Foreign_key_parent.collection m_foreign_key-parents;   
Partition.collection m partitions;   
Partition_leaf_vector m_leaf partitions;   
Trigger.collection m_triggers;   
Checkconstraint.collection m_checkConstraints;   
// References to other objects.   
Object_id m.collation_id;   
Object_id m_tablespace_id; 
```

上述字段是在Table_impl类中定义的，可以看到存储的数据字典对象中包含表的信息、索引的信息、外键信息等。同理，其他类型的映射保存的数据字典对象也可以参考对应的类定义：

```cpp
dd::CharsetImpl  
dd::CollationImpl  
dd::Column_statisticsImpl  
dd::SchemaImpl  
dd::TableImpl  
dd::TablespaceImpl  
dd::ViewImpl  
dd::EventImpl  
dd::ProcedureImpl 
```

前面介绍了Shared_dictionaries_cache的映射实现，下面再来看看Shared_dictionaries_cache提供的方法：

□ Shared_dictionaries_cache::get。从对应映射中获取数据字典对象，如果命中则直接返回，如果未命中则调用Shared_dictionaries_cache::get_uncached方法向Storage_adapter请求获取。  
□ Shared dictionary_cache::put。将对应的数据字典对象放到对应的映射中，后续请求相同的表时，直接从该映射命中返回即可。  
□ Shared dictionary_cache::get_uncached。请求Storage_adapter触发从mysql.ibd文件中读取数据字典信息，读取到之后，将数据字典信息封装成对应的对象存储到对应的映射中。

上述映射都有大小限制，并且有LRU机制，每个映射大小如下所示，有些是硬编码在代码中的，无法更改，有些则是复用的其他参数，可以通过调整参数间接调整：

```txt
instance()->m_map<Collision>(); ->set_capacity(collation_capacity);
instance()->m_map<Charset>(); ->set_capacity(charset_capacity); 
```

// 设置容量，为所有连接留出空间，在缓存中留下一个未使用的元素  
// 以避免例如在打开表时频繁的缓存未命中。

```cpp
instance()->m_map<Abstract_table>(); ->set_capacity(max Connections);
instance()->m_map(Event()); ->set_capacity(event_capacity);
instance()->m_map<Routine>(); ->set_capacity(storedProgram_def_size);
instance()->m_map<Schema>(); ->set_capacityschema_def_size);
instance()->m_map<Column_statistics>(); ->set_capacity(column_statistics_capacity);
instance()->m_map<Spatial_reference_system>(); ->set_capacity(spatial_reference_system_capacity);
instance()->mmap<Tablespace>(); ->set_capacity(tablespace_def_size);
instance()->mmap<Resource_group>(); ->set_capacity/resource_group_capacity); 
```

# 3. 数据字典客户端

数据字典客户端为每个用户线程维护着一份数据字典信息，记录了该线程所涉及的表的相关信息。用户线程在启动时首先会向数据字典客户端查询所需的数据字典信息。若查询未命中，则会向Shareddictionary_cache发起请求。一旦获取到所需的数据字典信息对象，就将其缓存至当前的Dictionary_client中。

实际上，Dictionary_client的底层实现是基于std::map的，与Shareddictionary_cache相同。MySQL为Dictionary_client创建了多种类型的映射，如下所示：

```txt
Sharedmulti_map<Abstract_table> m_ABstract_table_map;   
Shared multi map<Charset> m_charset_map;   
Shared multi map<Collation> m_collation_map;   
Shared multi map<Column_statistics> m_column_stat_map;   
Shared multi map<Event> m_event_map;   
Shared multi map<Resource_group> m_resource_group_map;   
Shared multi map<Routine> m_routine_map;   
Shared multi map<Schema> m_schema_map;   
Shared multi map<Spatial_reference_system> m_spatial_reference_system_map;   
Shared multi map<Tablespace> m_tablespace_map; 
```

数据字典类型也是复用Shared_dictionaries_cache的。

下面介绍Dictionary_client提供的方法：

□Dictionary_client::acquire。从对应的数据字典映射中获取数据字典对象，如果未命中则调用acquire uncommitted从Shared dictionary cache请求。

□ Dictionary_client::acquire_uncommitte。从Shareddictionary_cache中获取对应的数据字典对象，然后存储到Dictionary_client维护的对应映射中。

□Dictionary_client::store。调用Storage_adapter::store方法将数据字典信息存储到对应的数据字典表中，然后将数据字典对象缓存到Dictionary_client维护的对应映射中。

□Dictionary_client::drop。调用Storage_adapter::drop方法将数据字典信息从对应的数据字典表中删除，然后将缓存到维护的映射中的数据字典对象移除。

现在我们已经了解了数据字典缓存的整体设计，下面来总结一下打开一张表时访问数据字典缓存的流程：

首先，在用户线程中打开表时，会检索与该表相对应的数据字典信息。具体操作是，用户线程向维护的Dictionary_client发出请求，以获取相应的数据字典信息。随后，以表名为键，在Dictionary_client所维护的映射中检索并获取所需信息。

如果在Dictionary_client获取到对应的数据字典对象，则直接返回；否则调用Shared_dictionaries_cache::get从全局的缓存中获取数据字典信息。

如果在 Shared_dictionaries_cache 获取到对应的数据字典对象，则直接返回，并把数据字典对象缓存到 Dictionary_client 的映射中，否则调用 Storage_adapter::get 从数据字典表中获取对应的数据字典信息，从而触发 MySQL 去 mysql.ibd 文件中扫描对应数据字典的表。

如果在 Storage_adapter 获取到相关的数据字典信息，则封装成对应的数据字典对象存储在 Shared_dictary_cache 中，最终存储到 Dictionary_client 中。

大致的流程就是如此，下面是对应的调用栈，感兴趣的读者可自行研究：

```txt
dd::cache::Storage_adapter::get<dd::Item_name_key, dd::Abstract_table> storage_adapter.cc:154  
dd::cache::Shared_dictary_cache::get_uncached<dd::Item_name_key, dd::Abstract_table> shared_dictary_cache.cc:113  
dd::cache::Shared_dictary_cache::get<dd::Item_name_key, dd::Abstract_table> shared_dictary_cache.cc:98  
dd::cache::Dictionary_client::acquire<dd::Item_name_key, dd::Abstract_table> dictionary_client.cc:895  
dd::cache::Dictionary_client::acquire<dd::Abstract_table> dictionary_client.cc:1340  
get_table_share sql_base.cc:750  
get_table_share_with_discover sql_base.cc:860  
open_table sql_base.cc:3160  
open_and_process_table sql_base.cc:4993  
open_tables sql_base.cc:5648  
open_tables_for_query sql_base.cc:6503  
mysqld_list_fields sql_show.cc:764  
dispatch_command sql_scan.cc:1949  
do-command sql_scan.cc:1275  
handle_Connection connectionhandler_per_thread.cc:302  
pfs_spawn_thread pfs.cc:2854  
start_thread 0x00007f5850912ea5  
clone 0x00007f584ee4eb0d 
```

至此，数据字典缓存的相关介绍已全部完成。可以观察到，采用两层缓存结构能够显著提升数据字典访问的性能。其功能与先前的表缓存和表定义缓存相似，但数据字典缓存的设计更为规范和紧凑。此外，MySQL在这一领域增加了大量的代码逻辑。对此有兴趣的读者可以进一步探索研究。

# 4.4.3 数据字典的使用

我们已经了解了数据字典的整体设计，下面介绍MySQL8.0中数据字典的使用，跟MySQL5.7一样，数据字典的使用涉及增、删、改、查，如下：

□ 创建表的时候（增）  
□删除表、索引的时候（删）  
□修改索引信息、列信息的时候（改）  
□查询用户表触发加载的时候（查）

通过“增”与“查”这两个操作基本就能够了解MySQL8.0中是如何使用数据字典的。查询流程其实在介绍数据字典缓存时基本已经覆盖，这里重点介绍创建表的流程。

在执行如下语句的时候：

create table zbdba(id int, name varchar(36), primary key('id'));

MySQL首先会调用dd::create_table方法以创建数据字典对象，随后会构建数据字典表对象，即dd::table，接着调用fill_dd_table_from_create_info方法以填充表对象中的索引、字段、外键等其他数据字典信息。最终，一个完整的数据字典对象得以形成。之后，调用Dictionary_client::store方法，该方法最终会触发Storage_adapter::store方法，将数据字典信息存储到相应的数据字典表中。

这里重点介绍Storage_adapter:::store的逻辑：

1）解析数据字典对象保存的表相关信息，将其插入 mysqltables 中。  
2）解析数据字典对象保存的列信息，将其插入 mysql.columns 中。  
3）解析数据字典对象保存的索引信息，将其插入 mysql.indexes 中。  
4）解析数据字典对象保存的索引使用列信息，将其插入mysql.index_column_USAGE中。  
（5）将数据字典信息序列化成SDI并插入zbdba.ibd的SDI索引上。