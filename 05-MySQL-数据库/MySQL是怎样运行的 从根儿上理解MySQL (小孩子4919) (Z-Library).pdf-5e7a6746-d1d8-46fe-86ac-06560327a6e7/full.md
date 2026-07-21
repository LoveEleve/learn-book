![](images/a80c0493dcf667f415d8d6220456e72cc6c3a5252a6c8e97c8c017a4ce49cda1.jpg)

——从根儿上理解 MySQL

小孩子4919 著

# 目录

# 第0章 楔子——阅读前必看……1

# 第1章 装作自己是个小白初识MySQL· 3

1.1 MySQL的客户端/服务器架构  
1.2 MySQL的安装· 3   
1.3 启动MySQL服务器程序

1.3.1 在类UNIX系统中启动服务器程序  
1.3.2 在Windows系统中启动服务器程序

1.4 启动MySQL客户端程序   
1.5 客户端与服务器连接的过程 10

1.5.1 TCP/IP 10   
1.5.2 命名管道和共享内存… 10  
1.5.3 UNIX域套接字 11

1.6 服务器处理客户端请求

1.6.1 连接管理 12  
1.6.2 解析与优化 12  
1.6.3 存储引擎 14

1.7 常用存储引擎· 14  
1.8 关于存储引擎的一些操作 16

1.8.1 查看当前服务器程序支持的存储引擎 16  
1.8.2 设置表的存储引擎… 16

1.9 总结 17

# 第2章 MySQL的调控按钮启动选项和系统变量 19

2.1 启动选项和配置文件 19

2.1.1 在命令行上使用选项 19  
2.1.2 配置文件中使用选项 21  
2.1.3 在命令行和配置文件中启动选项的区别 26

2.2 系统变量· 27

2.2.1 系统变量简介· 27  
2.2.2 查看系统变量 27  
2.2.3 设置系统变量 28

2.3 状态变量 32  
2.4 总结 32

# 第3章 字符集和比较规则 34

3.1 字符集和比较规则简介 34

3.1.1 字符集简介 34  
3.1.2 比较规则简介 34  
3.1.3 一些重要的字符集… 35

3.2 MySQL中支持的字符集和比较规则…36

3.2.1 MySQL中的utf8和utf8mb4…36  
3.2.2 字符集的查看 36   
3.2.3 比较规则的查看 38

3.3 字符集和比较规则的应用 39

3.3.1 各级别的字符集和比较规则…39  
3.3.2 客户端和服务器通信过程中使用的字符集 44  
3.3.3 比较规则的应用 52

3.4 总结 53

# 第4章 从一条记录说起——InnoDB记录存储结构……55

4.1 准备工作 55  
4.2 InnoDB页简介· 55

4.3 InnoDB 行格式 56

4.3.1 指定行格式的语法… 56  
4.3.2 COMPACT 行格式 56  
4.3.3 REDUNDANT 行格式 64  
4.3.4 溢出列 68   
4.3.5 DYNAMIC行格式和 COMPRESSED行格式…70

4.4 总结 71

第5章 盛放记录的大盒子——InnoDB数据页结构· 72

5.1 不同类型的页简介· 72  
5.2 数据页结构快览 72  
5.3 记录在页中的存储… 73  
5.4 Page Directory（页目录） 80  
5.5 PageHeader（页面头部） 85  
5.6 FileHeader（文件头部） 86  
5.7 FileTrailer（文件尾部）· 88  
5.8 总结 88

第6章 快速查询的秘籍——B+树索引 90

6.1 没有索引时进行查找 90

6.1.1 在一个页中查找 90  
6.1.2 在很多页中查找… 91

6.2 索引· 91

6.2.1 一个简单的索引方案… 92  
6.2.2 InnoDB中的索引方案 94  
6.2.3 InnoDB中B+树索引的注意事项· 102  
6.2.4 MyISAM中的索引方案简介…104  
6.2.5 MySQL中创建和删除索引的语句· 105

6.3 总结 106

第7章 B+树索引的使用 107  
7.1 B+树索引示意图的简化· 107

7.2 索引的代价 109  
7.3 应用B+树索引 110

7.3.1 扫描区间和边界条件 …………………… 110  
7.3.2 索引用于排序 122  
7.3.3 索引用于分组 125

7.4回表的代价 126   
7.5 更好地创建和使用索引 … 127

7.5.1 只为用于搜索、排序或分组的列创建索引· 127  
7.5.2 考虑索引列中不重复值的个数 127  
7.5.3 索引列的类型尽量小 127  
7.5.4 为列前缀建立索引· 128  
7.5.5 覆盖索引 129   
7.5.6 让索引列以列名的形式在搜索条件中单独出现……129  
7.5.7 新插入记录时主键大小对效率的影响· 129  
7.5.8 冗余和重复索引· 130

7.6 总结 131

第8章 数据的家——MySQL的数据目录……132

8.1 数据库和文件系统的关系 132  
8.2 MySQL数据目录 132

8.2.1 数据目录和安装目录的区别 132  
8.2.2 如何确定MySQL中的数据目录 132

8.3 数据目录的结构 133

8.3.1 数据库在文件系统中的表示 133  
8.3.2 表在文件系统中的表示……134  
8.3.3 其他的文件 137

8.4 文件系统对数据库的影响· 137  
8.5 MySQL系统数据库简介 138  
8.6 总结 138

13.3 基于内存的非永久性统计数据…217  
13.4innodb.stats_method的使用…217  
13.5 总结 219

# 第14章 基于规则的优化（内含子查询优化二三事） 220

14.1 条件化简· 220

14.1.1 移除不必要的括号 220  
14.1.2 常量传递 220  
14.1.3 移除没用的条件 221  
14.1.4 表达式计算 221  
14.1.5 HAVING子句和WHERE子句的合并· 221  
14.1.6 常量表检测 221

14.2 外连接消除 222  
14.3 子查询优化 224

14.3.1 子查询语法 225  
14.3.2 子查询在MySQL中是怎么执行的· 230

14.4 总结 244

15.4 总结 269

# 第16章 神兵利器——optimizertrace的神奇功效· 270

16.1 optimizer trace简介 270  
16.2 通过optimizertrace分析查询优化器的具体工作过程· 271

# 第17章 调节磁盘和CPU的矛盾——InnoDB的BufferPool……278

17.1 缓存的重要性 278  
17.2 InnoDB的Buffer Pool 278

17.2.1 喻是Buffer Pool 278   
17.2.2 Buffer Pool 内部组成………278  
17.2.3 free链表的管理 279  
17.2.4 缓冲页的哈希处理 280  
17.2.5 flush链表的管理 281  
17.2.6 LRU链表的管理 282  
17.2.7 其他的一些链表 286  
17.2.8 刷新脏页到磁盘 287   
17.2.9 多个BufferPool实例………287

第15章查询优化的子科学术

# 18.3 MySQL中事务的语法 300

18.3.1 开启事务 300  
18.3.2 提交事务 301  
18.3.3 手动中止事务 302  
18.3.4 支持事务的存储引擎………302  
18.3.5 自动提交 303  
18.3.6 隐式提交 304   
18.3.7 保存点· 305

# 18.4 总结 307

# 第19章 说过的话就一定要做到——redo日志· 308

19.1 事先说明· 308  
19.2 redo日志是啥 308   
19.3 redo日志格式 309

19.3.1 简单的redo日志类型……309   
19.3.2 复杂一些的redo日志类型· 311   
19.3.3 redo日志格式小结 314

19.4 Mini-Transaction 315

19.4.1 以组的形式写入redo日志· 315  
19.4.2 Mini-Transaction的概念…319

19.5 redo日志的写入过程 319

19.5.1 redo log block 319   
19.5.2 redo日志缓冲区 320  
19.5.3 redo日志写入logbuffer…321

19.6 redo日志文件 323

19.6.1 redo日志刷盘时机 323  
19.6.2 redo日志文件组 323  
19.6.3 redo日志文件格式 324

19.7 log sequence number 327

19.7.1 flushed_to_disk_lsn 328   
19.7.2 Isn值和redo日志文件组中的偏移量的对应关系 330  
19.7.3 flush链表中的Isn 330

# 19.8 checkpoint 332

19.9 用户线程批量从flush链表中刷出脏页· 335   
19.10 查看系统中的各种Isn值……335  
19.11innodb Flush_log_at_trx_commit的用法· 336   
19.12 崩溃恢复 336

19.12.1 确定恢复的起点 337  
19.12.2 确定恢复的终点 337  
19.12.3 怎么恢复 337

19.13 遗漏的问题：LOG_BLOCK_
HDR_NO是如何计算的………339  
19.14 总结 340

# 第20章 后悔了怎么办——undo日志 342

20.1 事务回滚的需求 ……………………342  
20.2 事务id· 343

20.2.1 分配事务id的时机……343  
20.2.2 事务id是怎么生成的……343  
20.2.3 trx_id 隐藏列 … 344

20.3undo日志的格式 344

20.3.1 INSERT 操作对应的  
undo 日志……… 345  
20.3.2 DELETE操作对应的undo日志· 347   
20.3.3 UPDATE操作对应的undo日志· 353   
20.3.4 增删改操作对二级索引的影响· 357

20.4 通用链表结构· 357  
20.5 FIL_PAGE_UNDO_LOG页面……359  
20.6 Undo页面链表 361

20.6.1 单个事务中的 Undo 页面链表· 361  
20.6.2 多个事务中的 Undo 页面链表· 362

# 20.7undo日志具体写入过程 363

20.7.1 段的概念 … 363   
20.7.2 Undo Log Segment Header 364   
20.7.3 Undo Log Header 365   
20.7.4 小结 367

20.8 重用 Undo 页面· 368  
20.9 回滚段 369

20.9.1 回滚段的概念 369  
20.9.2 从回滚段中申请 Undo页面链表·  
20.9.3 多个回滚段 372  
20.9.4 回滚段的分类 374  
20.9.5 roll_pointer的组成 374  
20.9.6 为事务分配 Undo 页面链表的详细过程· 375

20.10 回滚段相关配置 376

20.10.1 配置回滚段数量 376  
20.10.2 配置undo表空间 376

20.11 undo日志在崩溃恢复时的作用…377  
20.12 总结 377

# 第21章 一条记录的多副面孔——事务隔离级别和MVCC……379

21.1 事前准备· 379  
21.2 事务隔离级别· 379

21.2.1 事务并发执行时遇到的一致性问题· 382  
21.2.2 SQL标准中的4种隔离级别· 385  
21.2.3 MySQL中支持的4种隔离级别· 386

21.3 MVCC原理 388  
21.3.1 版本链 388

21.3.2 ReadView 390   
21.3.3 二级索引与MVCC· 397  
21.3.4 MVCC小结 397

21.4 关于purge 398  
21.5 总结 399

# 第22章 工作面试老大难——锁……401

# 22.1 解决并发事务带来问题的

两种基本方式 401

22.1.1 写-写情况 401   
22.1.2 读-写或写-读情况…403  
22.1.3 一致性读· 404  
22.1.4 锁定读 404   
22.1.5 写操作· 405

22.2 多粒度锁 406  
22.3 MySQL中的行锁和表锁 408

22.3.1 其他存储引擎中的锁 408  
22.3.2 InnoDB存储引擎中的锁…409  
22.3.3 InnoDB锁的内存结构……417

22.4 语句加锁分析· 423

22.4.1 普通的SELECT语句………423  
22.4.2 锁定读的语句 424   
22.4.3 半一致性读的语句 441  
22.4.4 INSERT 语句· 442

22.5 查看事务加锁情况 444

22.5.1 使用information_schema

数据库中的表获取锁信息…444

22.5.2 使用SHOW ENINGE INNODB

STATUS 获取锁信息 446

22.6 死锁 450   
22.7 总结 454

# 参考资料 455

# 第0章 楔子——阅读前必看

尊敬的各位同学，本书是我花了相当长的时间写出来的，章节内容也都是经过精心编排的。为了尊重用户的认知规律，给各位同学一个良好的学习体验，我甚至推敲了很久各种概念的出场顺序对用户体验的影响。如果您对MySQL不熟，或者不是很熟，那么请暂时忘掉已经学到的一些关于MySQL的知识。就像是张无忌在学习太极剑法时那样，这样才能更好地跟着我设计好的套路走，从而达到事半功倍的效果。在学习过程中，大家千万千万千万要遵循下面这3个建议：

$\bullet$ 一定要逐章学习本书，千万不要跳着阅读！  
- 一定要逐章学习本书，千万不要跳着阅读！  
$\bullet$ 一定要逐章学习本书，千万不要跳着阅读！

因为从前期读者在本书电子版的测试过程中反馈的问题来看，绝大部分问题都是因为违反了上面这3个建议而产生的。

当然，尽管我尽了自己很大的努力来让进阶MySQL的这个过程变得更容易些，但终究不可能满足所有人的需求，有很多读者在阅读过程中肯定会产生这样那样的疑惑，我知道在自己阅读技术图书的过程中遇到了困惑而不得解是一种多么大的折磨，如果大家在阅读本书的过程中遇到了什么不得解的困惑，请使用微信扫描下方的二维码进入答疑群提问（人数较多的微信群只能通过个人手动拉入）。另外，请大家在答疑群中提问，而不是直接私信，一对一答疑对我来说负担是很大的。

![](images/0631eb9f300c36a6125135a2ea84df9907c7944a2aac25065921324c836a02c7.jpg)

![](images/e8d25a2a8ca1656a34b0d5123edbafd9a7ea904ca1d3e47b53c2eb0ee59ff3ed.jpg)

上面的二维码是使用我的个人域名生成的，所以在使用微信扫码时可能会提示非微信官方网页，大家继续访问就好了。如果扫码不成功，可以手动添加微信号：xiaohaizi4920。有可能个人微信已经加满，此时大家也可以到“我们都是小青蛙”公众号中获取答疑群的进入方式。另外，由于我比较忙，所以可能回复不及时，望见谅。

这里特别注意一下，需要提问的同学一定要先搞清楚自己到底哪里不清楚，然后用通顺的语句把它表达出来。以往很多同学的问题都是含糊不清，表达不通顺，这样的问题真的是让人看了要发疯。所以为了我们双方的方便，提问前请认真思考一下。另外，我只负责回答关于本书的问题，其他问题请和其他同学讨论吧（一是作者很可能也不会，二是作者精力实在有限，望理解）。

哇唔，看到这里，大家有没有觉得我好善良呢？买书还附赠答疑解惑服务。我当然不是这么单纯，建立答疑群其实有两个目的：

- 对于读者来说，可以解答在学习过程中的疑惑，读者由此受益；  
- 对于作者来说，可以为我的图书营造一个好的口碑，以后再写别的图书也好卖一些，我也可以从中受益。

另外，本书的知识比较系统，需要大家花费较多的时间认真研读，不过受个人能力和篇幅所限，无法做到面面俱到，大家也并不能把本书当作MySQL百科全书。为此，我开设了一个名为“我们都是小青蛙”的微信公众号，里面会不定期地发布一些原创的技术文章，偶尔也会“扯扯犊子”，希望能对大家有帮助。

![](images/5448ec90e28226176c2715c35d070c7b3c4d5b29a5e18542ced3470255baaa3f.jpg)  
“我们都是小青蛙”公众号

# 第1章 装作自己是个小白——初识MySQL

# 1.1 MySQL的客户端/服务器架构

以我们平时使用的微信为例，它其实是由客户端程序（可以简称为客户端）和服务器程序（可以简称为服务器）两部分组成的。微信客户端可能具有多种形式，比如手机App、桌面端的软件或者网页版的微信。微信的每个客户端都有一个唯一的用户名，即微信号。另一方面，腾讯公司在它们的机房里运行着微信的服务器程序。我们平时在微信上的各种操作，其实就是使用微信客户端与微信服务器打交道。比如狗哥使用微信给猫爷发一条微信消息的过程大致如下所示。

1. 狗哥发出的微信消息被客户端进行包装，添加了发送者和接收者的信息，然后从客户端发送到微信服务器。  
2. 微信服务器从收到的消息中获取发送者和接收者信息，并据此将消息发送到猫爷的微信客户端。然后，猫爷的微信客户端就会显示狗哥给他发的消息。

MySQL的运行过程与之类似，即它的服务器程序直接与要存储的数据打交道，多个客户端程序可以连接到这个服务器程序，向服务器发送增删查改的请求，然后服务器程序根据这些请求，对存储的数据进行相应处理。与微信一样，MySQL的每一个客户端都需要使用用户名和密码才能登录服务器，而且只有在登录之后才能向服务器发送某些请求来操作数据。MySQL的日常使用场景是下面这样的。

1. 启动MySQL服务器程序。  
2. 启动MySQL客户端程序，并连接到服务器程序。  
3. 在客户端程序中输入命令语句，并将其作为请求发送给服务器程序。服务器程序在收到这些请求后，根据请求的内容来操作具体的数据，并将结果返回给客户端。

众所周知，现在计算机的功能都很强大，一台计算机上可以同时运行多个程序，比如微信、QQ、英雄联盟游戏、文本编辑器等。计算机上运行的每一个程序也称为一个进程。运行过程中的MySQL服务器程序和客户端程序在本质上来说都算是计算机中的进程，其中代表MySQL服务器程序的进程称为MySQL数据库实例（instance）。

# 1.2 MySQL的安装

在安装MySQL时，无论是通过下载源代码的方式自行编译安装，还是直接使用官方提供的安装包进行安装，MySQL的服务器程序和客户端程序都会安装到机器上。无论采用上述哪种安装方式，一定要记住MySQL安装在哪里了。换句话说，一定一定一定要记住MySQL的安装目录。

![](images/05b647abea9fadef2a9135ccbfc6f0423d2a3d5d51ce13a2e5111c00eaf4bbbf.jpg)

MySQL 的大部分安装包都包含了服务器程序和客户端程序，不过在 Linux 环境下可以使用不同的 RPM 包分别安装服务器程序和客户端程序。具体安装细节可以参阅 MySQL 文档。

另外，MySQL可以运行在各种类型的操作系统上，本书后文会讨论MySQL在类UNIX操作系统和Windows操作系统上的一些使用差别。为了方便大家理解，我在macOS操作系统和Windows操作系统上都安装了MySQL，它们的安装目录分别如下。

- macOS 操作系统上的安装目录: /usr/local/mysql/。  
- Windows 操作系统上的安装目录：C:\Program Files\MySQL\MySQL Server 5.7\。

下面会以这两个安装目录为例来进一步引出更多的概念。大家一定要注意，上面这两个安装目录是在我的运行不同操作系统的机器上的安装目录，大家在后文的示例中一定要将安装目录替换为自己机器上的安装目录。

![](images/90a30b7e0779f5f45bc2ff1396544e15ddfc6a389f7557d506f6209fa0c0ea33.jpg)

类UNIX操作系统非常多，比如FreeBSD、Linux、macOS、Solaris等都属于UNIX操作系统的范畴。这里使用macOS操作系统代表类UNIX操作系统来运行MySQL。

# 1.2.1 bin目录下的可执行文件

在MySQL的安装目录下有一个特别重要的bin目录，这个目录存放着许多可执行文件。以macOS系统为例，这个bin目录的绝对路径（在我的机器上）就是/usr/local/mysql/bin。

下面我们列出macOS系统中这个bin目录下的部分可执行文件，如下所示。需要说明的是，该目录下的文件太多，这里仅列出了部分。

```txt
mysql  
mysql.server->./support-files/mysql.server  
mysqladmin  
mysqlbinlog  
mysqlcheck  
mysqld  
mysqld-multi  
mysqld_safe  
mysqldump  
mysqlimport  
mysqlpump  
（省略其他文件）  
0directories，40files
```

Windows中的可执行文件与macOS中的类似，不过都是以.exe为扩展名（不同的操作系统中，bin目录下包含的可执行文件并不完全相同）。这些可执行文件中，有的是服务器程序，有的是客户端程序。后面会详细介绍一些比较重要的可执行文件，这里先看一下这些文件的执行方式。

在具有图形用户界面的操作系统中，可以通过鼠标点击的方式打开并运行某个可执行文件。但是我们现在要关注的是，如何在命令行解释器下运行这些可执行文件。

![](images/5c3edb92304c0a823263ca8374d340a543fd36419de4d8e7ae4f848fd27e23f1.jpg)

# 小贴士

所谓命令行解释器，指的就是类UNIX系统中的Shell或者Windows系统中的cmd.exe。因为命令行解释器的界面通常是一个黑框框，所以在后文中我们可能会直接把命令行解释器称作黑框框。

下面以macOS系统为例来看看如何执行这些可执行文件（Windows中的操作与之类似，这里不再赘述）。

- 使用可执行文件的相对/绝对路径来执行。

假设命令行解释器中的当前工作目录是MySQL的安装目录，即/usr/local/mysql，要想执行bin目录下的mysqld可执行文件，可以使用相对路径，如下所示：

./bin/mysqld

也可以通过直接输入mysqld的绝对路径的方式来执行，如下所示：

/usr/local/mysql/bin/mysqld

- 将 bin 目录的绝对路径加入到环境变量 PATH 中。

大家应该发现，若每次执行一个文件都需要输入一长串路径名，这未免太麻烦了。针对这种情况，可以把bin目录所对应的绝对路径添加到环境变量PATH中。环境变量PATH是一系列路径的集合，各个路径之间使用冒号（：）隔离开。

比如，在我的机器上，环境变量PATH的值为/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin。这个值表明，在我输入某个命令时，系统会在/usr/local/bin、/usr/bin、/bin、/usr/sbin和/sbin目录下按照顺序依次寻找输入的这个命令。如果寻找成功，则执行该命令。

也可以修改这个环境变量PATH，把MySQL安装目录下的bin目录的绝对路径添加到PATH中。修改后的环境变量PATH的值为/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/mysql/bin。这样一来，无论命令行解释器的当前工作目录是啥，都可以直接输入可执行文件的名字来启动，比如下面这样：

mysqld

可见，这样一来方便多了！

![](images/d13dc5992757bcbcf79a385b7e28dfd202b20bb590ec5bd8545586520078ba73.jpg)

# 小贴士

有关环境变量的概念以及在系统中修改环境变量的方法不属于本书讲解范围，大家可以自行查询相关资料进行更深入的学习。

# 1.3 启动MySQL服务器程序

# 1.3.1 在类UNIX系统中启动服务器程序

在类UNIX系统中，用来启动MySQL服务器程序的可执行文件有很多，且大部分都位于MySQL安装目录的bin目录下。下面我们一探究竟。

# 1. mysqld

mysqld可执行文件就表示MySQL服务器程序，运行这个可执行文件就可以直接启动一个MySQL服务器进程。但这个可执行文件并不常用，我们继续看其他的启动命令。

# 2. mysqld_safe

mysqld_safe 是一个启动脚本，它会间接调用 mysqld 并持续监控服务器的运行状态。当服务器进程出现错误时，它还可以帮助重启服务器程序。另外，使用 mysqldsafe 启动 MySQL 服务器程序时，它会将服务器程序的出错信息和其他诊断信息输出到错误日志，以方便后期查找发生错误的原因。

![](images/cafdbc8b923afee730197f1487b9546d06c58bbf478ae910834c91e14fc1e7e6.jpg)

出错日志默认写到一个以.err为扩展名的文件中，该文件位于MySQL的数据目录中。后续章节会介绍MySQL的这个数据目录。

# 3. mysql.server

mysql.server 也是一个启动脚本，它会间接地调用 mysqld_safe。在执行 mysql.server 时，在后面添加 start 参数就可以启动服务器程序了，如下所示：

mysql.serverstart

需要注意的是，mysql.server文件其实是一个链接文件，它对应的实际文件是../support-files/mysql.server。

![](images/4b124c126296f9399464880b76e8975ebd0acc160ffa450281f84b6465e4c29e.jpg)

通过源码安装MySQL，或者使用一个没有自动安装mysql.server脚本的安装包来安装MySQL时，需要手动安装这个mysql.server脚本。具体安装方式可以参阅相关文档。

还可以使用 mysql.server 来关闭正在运行的服务器程序，此时只需把 start 参数换成 stop 即可，如下所示：

mysql.serverstop

# 4. mysqld_multiple

其实我们在一台计算机上也可以运行多个服务器实例，也就是运行多个MySQL服务器进程。mysqldmulti可执行文件可以启动或停止多个服务器进程，也能报告它们的运行状态。这个命令的使用比较复杂，本书主要是为了讲清楚单个MySQL服务器的运行过程，不会对启动多个服务器程序进行过多唠叨。

![](images/bb4ef8f5d10091c88e1af4e1ae3a987143ffa5b5c405851141868bb991aee537.jpg)

mysqld-safe、mysql.server 和 mysqlmulti 本质上是一个 Shell 脚本，大家可以直接把它们当作文本打开并进行浏览（前提是能看懂 Shell 脚本）。

# 1.3.2 在Windows系统中启动服务器程序

尽管在Windows中没有提供像UNIX中的那么多启动脚本，但是它也提供了两种启动方

后的处理结果呢？客户端可以向服务器发送增删改查等各类请求，这里以比较复杂的查询请求为例来展示一下大致的过程，如图1-1所示。

![](images/ceccaf98854ca4dd54f60b7dc5ce456038036250c015c85e71f365408d3fc111.jpg)

的处理，其中几个比较重要的部分分别是查询缓存、语法解析和查询优化。下面我们详细来看。

# 1. 查询缓存

如果我问你 $9 + 8 \times 16 - 3 \times 2 \times 17$ 的值是多少，你可能会用计算器去算一下，或者再厉害一点直接用心算，最终得到了结果35。如果我再问你一遍 $9 + 8 \times 16 - 3 \times 2 \times 17$ 的值是多少，你还用再傻呵呵地算一遍么？我们刚刚已经算过了，直接说答案就好了。

MySQL 服务器程序处理查询请求的过程也是这样，会把刚刚处理过的查询请求和结果缓存起来。如果下一次有同样的请求过来，直接从缓存中查找结果就好了，就不用再去底层的表中查找了。这个查询缓存可以在不同的客户端之间共享，也就是说，如果客户端 A 刚刚发送了一个查询请求，而客户端 B 之后发送了同样的查询请求，那么客户端 B 的这次查询就可以直接使用查询缓存中的数据了。

当然，MySQL服务器并没有人那么聪明，如果两个查询请求有任何字符上的不同（例如，空格、注释、大小写），都会导致缓存不会命中。另外，如果查询请求中包含某些系统函数、用户自定义变量和函数、系统表，如mysql、information_schema、performance_schema数据库中的表，则这个请求就不会被缓存。以某些系统函数为例，同一个函数的两次调用可能会产生不一样的结果。比如函数NOW，每次调用时都会产生最新的当前时间。如果在两个查询请求中调用了这个函数，即使查询请求的文本信息都一样，那么不同时间的两次查询也应该得到不同的结果。如果在第一次查询时就缓存了结果，在第二次查询时直接使用第一次查询的结果就是错误的！

不过既然是缓存，那就有缓存失效的时候。MySQL的缓存系统会监测涉及的每张表，只要该表的结构或者数据被修改，比如对该表使用了INSERT、UPDATE、DELETE、TRUNCATE TABLE、ALTER TABLE、DROP TABLE或DROP DATABASE语句，则与该表有关的所有查询缓存都将变为无效并从查询缓存中删除！

![](images/afb1ca9cfcbcfcb597c9d0174114f8d55c4b94e61868d6b1c832cdc10dbe497e.jpg)

虽然查询缓存有时可以提升系统性能，但也不得不因维护这块缓存而造成一些开销。比如每次都要去查询缓存中检索，查询请求处理完后需要更新查询缓存，需要维护该查询缓存对应的内存区域等。从MySQL 5.7.20开始，不推荐使用查询缓存，在MySQL 8.0中直接将其删除。

# 2. 语法解析

如果查询缓存没有命中，接下来就需要进入正式的查询阶段了。因为客户端程序发送过来的请求只是一段文本，所以MySQL服务器程序首先要对这段文本进行分析，判断请求的语法是否正确，然后从文本中将要查询的表、各种查询条件都提取出来放到MySQL服务器内部使用的一些数据结构上。

![](images/50fe99650442c9201c2e41f9680b2749f08d029c29526d6ea638886ad37de8df.jpg)

小贴士

从本质上来说，这个从指定的文本中提取出需要的信息算是一个编译过程，涉及词法解析、语法分析、语义分析等阶段。这些问题不属于我们讨论的范畴，大家只要了解在处理请求的过程中需要这个步骤就好了。

# 3. 查询优化

在语法解析之后，服务器程序获得了需要的信息，比如要查询的表和列是哪些、搜索

条件是什么等。但光有这些是不够的，因为我们写的MySQL语句执行起来效率可能并不是很高，MySQL的优化程序会对我们的语句做一些优化，如外连接转换为内连接、表达式简化、子查询转为连接等一堆东西。优化的结果就是生成一个执行计划，这个执行计划表明了应该使用哪些索引执行查询，以及表之间的连接顺序是啥样，等等。我们可以使用EXPLAIN语句来查看某个语句的执行计划。关于查询优化的详细内容我们后边会仔细唠叨，现在只需要知道在MySQL服务器程序处理请求的过程中有这么一个步骤就好了。

# 1.6.3 存储引擎

到服务器程序完成了查询优化为止，还没有真正地去访问真实的表中数据（在查询优化期间可能访问表中少量数据，在讨论查询优化的章节中我们会详细唠叨）。MySQL服务器把数据的存储和提取操作都封装到了一个名为存储引擎的模块中。我们知道，表是由一行一行的记录组成的，但这只是一个逻辑上的概念。在物理上如何表示记录，怎么从表中读取数据，以及怎么把数据写入具体的物理存储器上，都是存储引擎负责的事情。为了实现不同的功能，MySQL提供了各式各样的存储引擎，不同存储引擎管理的表可能有不同的存储结构，采用的存取算法也可能不同。

![](images/08a4773eca9980fe55f609c4dd6d4b7edd08e765f5ad1a1d21b89bb9a7f0548f.jpg)

为什么叫引擎呢？可能这个名字更拉风吧。其实这个存储引擎以前叫作表处理器，后来可能人们觉得太土，就改成了存储引擎。它的功能就是接收上层传下来的指令，然后对表中的数据进行读取或写入操作。

为了方便管理，人们把MySQL服务器处理请求的过程简单地划分为server层和存储引擎层。连接管理、查询缓存、语法解析、查询优化这些并不涉及真实数据存取的功能划分为server层的功能，存取真实数据的功能划分为存储引擎层的功能。各种不同的存储引擎为server层提供统一的调用接口，其中包含了几十个不同用途的底层函数，比如“读取索引第一条记录”“读取索引下一条记录”“插入记录”等。

所以在server层完成了查询优化后，只需按照生成的执行计划调用底层存储引擎提供的接口获取到数据后返回给客户端就好了。不过需要注意的一点是，server层和存储引擎层交互时，一般是以记录为单位的。以SELECT语句为例，server层根据执行计划先向存储引擎层取一条记录，然后判断是否符合WHERE条件；如果符合，就发送给客户端，否则跳过该记录，然后继续向存储引擎索要下一条记录；依此类推。

![](images/4255ec6f108d2cc9554062ff72b62989a376f212b38551d8756d14dfd8ce6c33.jpg)

server层在判断某条记录符合要求之后，其实是先将其发送到一个缓冲区，待到该缓冲区满了，才向客户端发送真正的记录。该缓冲区大小由系统变量net_buffer_length控制，当然，你现在可能不知道啥是系统变量，不过下一章就会知道了。

# 1.7 常用存储引擎

MySQL支持多种存储引擎，先来看看表1-2中列出的部分存储引擎。

表 1-2 MySQL 支持的存储引擎  

<table><tr><td></td><td>存储引擎</td><td></td><td>描述</td></tr><tr><td>ARCHIVE</td><td></td><td></td><td>用于数据存档(记录插入后不能再修改)</td></tr><tr><td>BLACKHOLE</td><td></td><td></td><td>丢弃写操作,读操作会返回空内容</td></tr><tr><td>CSV</td><td></td><td></td><td>在存储数据时,以逗号分隔各个数据项</td></tr><tr><td>FEDERATED</td><td></td><td></td><td>用来访问远程表</td></tr><tr><td>InnoDB</td><td></td><td></td><td>支持事务、行级锁、外键</td></tr><tr><td>MEMORY</td><td></td><td></td><td>数据只存储在内存,不存储在磁盘;多用于临时表</td></tr><tr><td>MERGE</td><td></td><td></td><td>用来管理多个MyISAM表构成的表集合</td></tr><tr><td>MyISAM</td><td></td><td></td><td>主要的非事务处理存储引擎</td></tr><tr><td>NDB</td><td></td><td></td><td>MySQL集群专用存储引擎</td></tr></table>

这么多存储引擎，看着都眼花了，我们怎么挑啊。其实大家多虑了，我们最常用的就是InnoDB和MyISAM，偶尔还会提一下MEMORY。其中InnoDB是MySQL默认的存储引擎，我们之后会详细唠叨这个存储引擎的各种功能，现在先看一下部分存储引擎对于某些功能的支持情况，如表1-3所示。

表 1-3 存储引擎对于某些功能的支持情况  

<table><tr><td>功能</td><td>MyISAM</td><td>MEMORY</td><td>InnoDB</td><td>ARCHIVE</td><td>NDB</td></tr><tr><td>B-tree indexes</td><td>是</td><td>是</td><td>是</td><td>否</td><td>否</td></tr><tr><td>Backup/point-in-time recovery</td><td>是</td><td>是</td><td>是</td><td>是</td><td>是</td></tr><tr><td>Cluster database support</td><td>否</td><td>否</td><td>否</td><td>否</td><td>是</td></tr><tr><td>Clustered indexes</td><td>否</td><td>否</td><td>是</td><td>否</td><td>否</td></tr><tr><td>Compressed data</td><td>是</td><td>否</td><td>是</td><td>是</td><td>否</td></tr><tr><td>Data caches</td><td>否</td><td>N/A</td><td>是</td><td>否</td><td>是</td></tr><tr><td>Encrypted data</td><td>是</td><td>是</td><td>是</td><td>是</td><td>是</td></tr><tr><td>Foreign key support</td><td>否</td><td>否</td><td>是</td><td>否</td><td>是</td></tr><tr><td>Full-text search indexes</td><td>是</td><td>否</td><td>是</td><td>否</td><td>否</td></tr><tr><td>Geospatial data type support</td><td>是</td><td>否</td><td>是</td><td>是</td><td>是</td></tr><tr><td>Geospatial indexing support</td><td>是</td><td>否</td><td>是</td><td>否</td><td>否</td></tr><tr><td>Hash indexes</td><td>否</td><td>是</td><td>否</td><td>否</td><td>是</td></tr><tr><td>Index caches</td><td>是</td><td>N/A</td><td>是</td><td>否</td><td>是</td></tr><tr><td>Locking granularity</td><td>表</td><td>表</td><td>行</td><td>行</td><td>行</td></tr><tr><td>MVCC</td><td>否</td><td>否</td><td>是</td><td>否</td><td>否</td></tr><tr><td>Query cache support</td><td>是</td><td>是</td><td>是</td><td>是</td><td>是</td></tr><tr><td>Replication support</td><td>是</td><td>有限支持</td><td>是</td><td>是</td><td>是</td></tr><tr><td>Storage limits</td><td>256TB</td><td>RAM</td><td>64TB</td><td>无存储限制</td><td>384EB</td></tr><tr><td>T-tree indexes</td><td>否</td><td>否</td><td>否</td><td>否</td><td>是</td></tr><tr><td>Transactions</td><td>否</td><td>否</td><td>是</td><td>否</td><td>是</td></tr><tr><td>Update statistics for data dictionary</td><td>是</td><td>是</td><td>是</td><td>是</td><td>是</td></tr></table>

表1-3密密麻麻列了这么多，看得让人头皮发麻，目的就是想告诉你：这玩意儿很复杂（其实表1-3是我从MySQL文档中直接复制过来的）。其实这些东西大家没必要立即记住，这里主要是想让大家明白不同的存储引擎支持不同的功能。有些重要的功能我们会在后面的唠叨中慢慢让大家理解。

![](images/da0b38df0eedc5721abf6080d4adebc09819f59ab34b46b4e73498f9f8060f5c.jpg)

小贴士

InnoDB 从 MySQL 5.5.5 版本开始作为 MySQL 的默认存储引擎，之前版本的默认存储引擎为 MyISAM。

# 1.8 关于存储引擎的一些操作

# 1.8.1 查看当前服务器程序支持的存储引擎

我们可以用下面这个命令来查看当前服务器程序支持的存储引擎：

SHOW ENGINE;

来看一下调用效果：

mysql>SHOW ENGINESE;

<table><tr><td>| Engine | Support | Comment | Transactions | XA | Savepoints |</td></tr><tr><td>| InncDB | DEFAULT | Supports transactions, row-level locking, and foreign keys | YES | YES | YES |</td></tr><tr><td>| MRG_MYISAM | YES | Collection of identical MyISAM tables | NO | NO | NO |</td></tr><tr><td>| MEMORY | YES | Hash based, stored in memory, useful for temporary tables | NO | NO | NO |</td></tr><tr><td>| BLACKHOLE | YES | /dev/null storage engine (anything you write to it disappears) | NO | NO | NO |</td></tr><tr><td>| MyISAM | YES | MyISAM storage engine | NO | NO | NO |</td></tr><tr><td>| CSV | YES | CSV storage engine | NO | NO | NO |</td></tr><tr><td>| ARCHIVE | YES | Archive storage engine | NO | NO | NO |</td></tr><tr><td>| PERFORMANCE_SCHEMA | YES | Performance Schema | NO | NO | NO |</td></tr><tr><td>| FERERATED | NO | Federated MySQL storage engine | NULL | NULL | NULL |</td></tr></table>

9 rows in set (0.00 sec)

其中，Support列表示该存储引擎是否可用，DEFAULT值代表当前服务器程序的默认存储引擎；Comment列是对存储引擎的一个描述；Transactions列代表该存储引擎是否支持事务处理；XA列代表该存储引擎是否支持分布式事务；Savepoints列代表该存储引擎是否支持事务的部分回滚。

![](images/de02f66bf54d0a47c4ed2826ab473b7d77ab3ebd414efecd3a03389e01bda1e7.jpg)

小贴士

好吧，也许你并不知道什么是事务，更别提分布式事务了。关于事务的详细情况后续章节会非常详细地唠叨，少安毋躁。

# 1.8.2 设置表的存储引擎

我们前边说过，存储引擎是负责对表中的数据进行读取和写入工作的，我们可以为不同的表设置不同的存储引擎。也就是说，不同的表可以有不同的物理存储结构、不同的读取和写入方式。

# 1. 创建表时指定存储引擎

如果我们在创建表的语句中没有指定表的存储引擎，那就会使用默认的存储引擎 InnoDB。

续表  

<table><tr><td>路径名</td><td></td><td>备注</td></tr><tr><td>\$MYSQL_HOME/my.cnf</td><td></td><td>特定于服务器的选项(仅限服务器)</td></tr><tr><td>defaults-extra-file</td><td></td><td>命令行指定的额外配置文件路径</td></tr><tr><td>\~/.my.cnf</td><td></td><td>特定于用户的选项</td></tr><tr><td>\~/.mylogin.cnf</td><td></td><td>特定于用户的登录路径选项(仅限客户端)</td></tr></table>

同样，在阅读类UNIX操作系统下的这些配置文件路径时，需要注意下面这些事情。

- SYSCONFDIR 表示在使用 CMake 构建 MySQL 时使用 SYSCONFDIR 选项指定的目录。

![](images/56d39fbf2f4526a8fe3114ebacf87299f9303667bbff4ca0584d28c5804e8b9b.jpg)

小贴士

如果你不懂啥是CMake，啥是编译，那就跳过吧，这对理解后续的文章没啥影响。

- MySQL_HOME 是一个环境变量,该变量的值是我们自己设置的(想设置就设置,不想设置就不设置)。该变量的值代表一个路径,我们可以在该路径下创建一个 my.cnf配置文件,这个配置文件中只能放置与启动服务器程序相关的选项(.mylogin.cnf 只能存放客户端相关的一些选项,除.mylogin.cnf以及$MySQL_HOME/my.cnf配置文件外,其余配置文件既可以存放服务器相关的选项,也可以存放客户端相关的选项)。

![](images/3f2164b0e1d95e74e3fa0e6e4c9fa23b8df048d73a48fad6595c8ac18dbba9a4.jpg)

小贴士

如果使用mysqld_safe启动服务器程序，而且我们也没有主动设置这个MySQL_HOME环境变量的值，那么这个环境变量的值将自动被设置为MySQL的安装目录，也就是MySQL服务器将会在安装目录下查找名为my.cnf的配置文件。

- 表2-3中最后两个以～开头的路径是用户相关的。类UNIX系统中都有一个当前登录用户的概念，每个用户都可以有一个用户目录，～就代表这个用户目录。大家可以查看HOME环境变量的值来确定当前用户的用户目录。比如，我的macOS机器上的用户目录就是/Users/xiaohaizi4919。之所以说表2-3中最后两个配置文件是用户相关的，是因为不同的类UNIX系统的用户都可以在自己的用户目录下创建.my.cnf或者.mylogin.cnf。换句话说，不同登录用户使用的.my.cnf或者.mylogin.cnf配置文件是不同的。

- defaults-extra-file的含义与Windows中的一样，不再赘述。  
- .mylogin.cnf的含义也同Windows中的一样。再次强调一遍，它不是纯文本文件，只能使用mysql_configeditor实用程序去创建或修改，用于存放客户端登录服务器时的相关选项。

总之，在我的计算机中，这几个路径中的任意一个都可以当作配置文件来使用。如果它们不存在，可以手动创建一个。比如，在 $\sim/.my.cnf$ 路径下手动创建一个配置文件。

另外，我们在唠叨如何启动MSOL服务器程序的时候说过，使用mysql安全程序启

```shell
mysqld_safe --skip-networking 
```

则在mysqld_safe调用mysqld时，会把它处理不了的这个skip-networking选项交给mysqld处理。

# 2. 配置文件的内容

与在命令行中指定启动选项不同的是，配置文件中的启动选项被划分为若干个组，每个组有一个组名，用中括号[]扩起来，像下面这样：

```ini
[server]  
(具体的启动选项...)  
[mysqld]  
(具体的启动选项...)  
[mysqld_safe]  
(具体的启动选项...)  
[client]  
(具体的启动选项...)  
[mysql]  
(具体的启动选项...)  
[mysqladmin]  
(具体的启动选项...) 
```

上面这个配置文件里就定义了许多个组，组名分别是 server、mysqld、mysqld-safe、client、mysql、mysqladmin。每个组下边可以定义若干个启动选项。我们以 [server] 组为例来看一下填写启动选项的形式（其他组中启动选项的形式是一样的）：

```txt
[server]  
option1 #这是option1，该选项不需要选项值  
option2 = value2 #这是option2，该选项需要选项值
```

在配置文件中指定启动选项的语法类似于命令行语法，但是在配置文件中只能使用长形式的选项，而且在配置文件中指定的启动选项不允许加--前缀，并且每行只指定一个选项，等号 $=$ 周围可以有空白字符（在命令行中，选项名、=、选项值之间不允许有空白字符）。另外，在配置文件中，我们可以使用#来添加注释，从#出现直到行尾的内容都属于注释内容，MySQL程序会忽略这些注释内容。

为了让大家更容易对比在命令行和配置文件中指定启动选项的区别，我们再把在命令行中指定 option1 和 option2 两个选项的格式写一遍看看：

```css
--option1 --option2=value2 
```

在配置文件中，不同的选项组是给不同的程序使用的。如果选项组名称与程序名称相同，则组中的选项将专门应用于该程序。例如，[mysqld]和[mysql]组分别应用于mysqld服务器程序和mysql客户端程序。不过有两个选项组比较特别：

- [server] 组下面的启动选项将作用于所有的服务器程序；  
- [client]组下面的启动选项将作用于所有的客户端程序。

需要注意的一点是，mysqldsafe和mysql.server这两个程序在启动时都会读取[mysqld]选项组中的内容。为了直观感受一下，我们挑一些程序来看看它们能读取的选项组都有哪些（见表2-4）。

表 2-4 程序的对应类别和能读取的组  

<table><tr><td>程序名</td><td>类别</td><td>能读取的组</td></tr><tr><td>mysqld</td><td>启动服务器</td><td>[mysqld]、[server]</td></tr><tr><td>mysqld-safe</td><td>启动服务器</td><td>[mysqld]、[server]、[mysqldSAFE]</td></tr><tr><td>mysql.server</td><td>启动服务器</td><td>[mysqld]、[server]、[mysq server]</td></tr><tr><td>mysql</td><td>启动客户端</td><td>[mysq]、[client]</td></tr><tr><td>mysqladmin</td><td>启动客户端</td><td>[mysqadmin]、[client]</td></tr><tr><td>mysqldump</td><td>启动客户端</td><td>[mysqdump]、[client]</td></tr></table>

现在以 macOS 操作系统为例，在 /etc/mysql/my.cnf 配置文件中添加一些内容（如果大家使用的是 Windows 系统，请自行参考前文提到的配置文件路径）：

```txt
[server] skip-networking default-storage-engine=MyISAM 
```

然后直接用mysqld启动服务器程序：

```txt
mysqld 
```

虽然在命令行中没有添加启动选项，但是在程序启动时，会默认地到我们上面提到的配置文件路径下查找配置文件，其中就包括/etc/mysql/my.cnf。又由于mysqld命令可以读取[server]选项组的内容，所以skip-networking和default-storage-engine=MyISAM这两个选项是生效的。大家可以把这些启动选项放在[client]组中，然后再试试用mysqld启动服务器程序，看看里面的启动选项是否生效（剧透一下，不生效）。

# 3. 特定MySQL版本的专用选项组

我们可以在选项组的名称后加上特定的MySQL版本号。比如对于[mysqld]选项组来说，我们可以定义一个[mysqld-5.7]的选项组。它的含义和[mysqld]一样，只不过只有版本号为5.7的mysqld程序才能使用这个选项组中的选项。

# 4. 配置文件的优先级

我们前面唠叨过，MySQL将在某些固定的路径下搜索配置文件。我们也可以通过在命令行中指定 defaults-extra-file启动选项来指定额外的配置文件路径。MySQL将按照表2-2或表2-3中给定的顺序（具体取决于所用的操作系统）依次读取各个配置文件。如果该文件不存在，则

忽略。值得注意的是，如果我们在多个配置文件中设置了相同的启动选项，则以最后一个配置文件中的为准。比如/etc/my.cnf文件的内容是这样的：

```txt
[server] default-storage-engine=InnoDB 
```

而 $\sim$ .my.cnf文件中的内容是这样的：

```txt
[server] default-storage-engine=MyISAM 
```

又因为 $\sim /$ .my.cnf比/etc/my.cnf顺序靠后，因此，若两个配置文件中出现相同的启动选项，将以 $\sim /$ .my.cnf中的为准。所以，在MySQL服务器程序启动之后，default-storage-engine的值就是MyISAM。

# 5. 同一个配置文件中多个组的优先级

我们说同一个程序可以访问配置文件中的多个组，比如mysqld可以访问[mysqld]、[server]组。如果在同一个配置文件中（比如\~/.my.cnf），在[mysqld]、[server]组里出现了同样的启动选项，比如下面这样：

[server]   
default-storage-engine $=$ InnoDB   
[mysqld]   
default-storage-engine $\equiv$ MyISAM

那么，将以最后一个出现的组中的启动选项为准。比如，在上面的例子中，default-storage-engine既出现在[server]组也出现在[mysqld]组，由于[mysqld]组在[server]组后边，所以将以[mysqld]组中的配置项为准。

# 6. defaults-file的使用

如果我们不想让MySQL到默认的路径下搜索配置文件，则可以在命令行指定 defaults-file选项，比如下面这样（以类UNIX系统为例）：

```batch
mysqld --defaults-file=/tmp/myconfig.txt 
```

这样一来，在程序启动时将只在/tmp/myconfig.txt路径下搜索配置文件。如果文件不存在或无法访问，则会发生错误。

![](images/aeb7746e466b81575717a5e745aab9400b1e525441ed9eb1607cc349b852d3b9.jpg)

注意 defaults-extra-file和 defaults-file的区别，使用 defaults-extra-file可以指定额外的配置文件路径（也就是说那些固定的配置文件路径也会被搜索）。

# 2.1.3 在命令行和配置文件中启动选项的区别

在命令行中指定的绝大部分启动选项都可以放到配置文件中，但是有一些选项是专门为命令行设计的，比如 defaults-extra-file、defaults-file这样的选项本身就是为了指定配置文件路径

的，如果再放在配置文件中使用就没啥意义了。剩下的一些只能用到命令行中而不能用到配置文件中的启动选项就不一一列举了，等到用的时候再提（本书中用不到，有兴趣的读者请移步到官方文档）。

另外有一点需要特别注意：如果同一个启动选项既出现在命令行中，又出现在配置文件中，那么以命令行中的启动选项为准！比如我们在配置文件中写了：

```txt
[server] default-storage-engine=InnoDB 
```

而我们的启动命令是：

```batch
mysqld --default-storage-engine=MyISAM 
```

那么，最后 default-storage-engine 的值就是 MyISAM！

# 2.2 系统变量

# 2.2.1 系统变量简介

MySQL 服务器程序在运行过程中会用到许多影响程序行为的变量，它们被称为系统变量。比如，允许同时连入的客户端数量用系统变量 max Connections 表示；表的默认存储引擎用系统变量 default_storage_engine 表示；查询缓存的大小用系统变量 query_cache_size 表示。MySQL 服务器程序的系统变量有好几百个，这里不再一一列举。每个系统变量都有一个默认值，我们可以使用命令行或者配置文件中的选项在启动服务器时改变一些系统变量的值。大多数系统变量的值也可以在程序运行过程中修改，而无须停止并重新启动服务器。

# 2.2.2 查看系统变量

我们可以使用下列命令查看MySQL服务器程序支持的系统变量以及它们的当前值：

SHOW VARIABLES [LIKE 匹配的模式]；

由于系统变量实在太多了，如果我们直接使用SHOW VARIABLES查看的话，就直接在屏幕上刷屏了，所以通常都会使用一个LIKE表达式来指定过滤条件，比如这么写：

```txt
mysql>SHOW VARIABLES LIKE 'default_storage_engine';  
+  
Variable_name | Value |  
+  
| default_storage_engine | InnoDB |  
+  
1 row in set (0.01 sec)  
mysql>SHOW VARIABLES like 'max.getConnections';  
+  
Variable_name | Value | 
```

```txt
+  
| max Connections | 151  
+  
1 row in set (0.00 sec) 
```

可以看到，现在服务器程序使用的默认存储引擎就是InnoDB，允许同时连接的客户端数量最多为151。

![](images/986856366a9432fd73c37cd806c1d3fef561fce986056cf842ea301838a61f8e.jpg)

更严谨地说，MySQL服务器实际上允许max(connections + 1个客户端连接，额外的1个是给超级用户准备的（很显然这是超级用户的一个特权）。

别忘了LIKE表达式中可以使用通配符来进行模糊查询，也就是说我们可以这么写：

```txt
mysql>SHOW VARIABLES LIKE 'default%';  
+  
Variable_name | Value  
+  
defaultAuthenticationPlugin | mysql_native_password  
| default_password_l lifetime | 0  
| default_storage_engine | InnoDB  
| default_tmp_storage_engine | InnoDB  
| default Week_format | 0  
+  
5 rows in set (0.01 sec) 
```

这样就查出了所有以 default 开头的系统变量的值。

# 2.2.3 设置系统变量

# 1. 通过启动选项设置

大部分系统变量都可以通过在启动服务器时传送启动选项的方式来设置。如何填写启动选项我们在前面已经花了大量篇幅来唠叨了，其实就是下面这两种方式：

- 通过命令行添加启动选项。

比方说在启动服务器程序时用这个命令：

```batch
mysqld --default-storage-engine=MyISAM --max-connections=10 
```

- 通过配置文件添加启动选项。

可以这样填写配置文件：

```ini
[server]  
default-storage-engine=MyISAM  
max-connections=10 
```

当使用上面的任何一种方式启动服务器程序后，再来看一下系统变量的值：

```javascript
mysql>SHOWVARIABLESLIKE'default storing_engine'; + Variable_name |Value 
```

```txt
+  
| default_storage_engine | MyISAM |  
+  
1 row in set (0.00 sec)  
mysql> SHOW VARIABLES LIKE 'max(connections';  
+  
| Variable_name | Value |  
+  
| max(connections | 10 |  
+  
1 row in set (0.00 sec) 
```

可以看到 default_storage_engine 和 max Connections 这两个系统变量的值已经被修改了。需要注意的一点是，对于启动选项来说，如果启动选项名由多个单词组成，各个单词之间用短划线（-）或者下划线（_）连接起来都可以；但是对于对应的系统变量来说，各个单词之间必须使用下划线（_）连接起来。

# 2. 服务器程序运行过程中设置

对于大部分系统变量来说，它们的值可以在服务器程序运行过程中进行动态修改而无须停止并重启服务器。不过系统变量有作用范围之分，下面详细唠叨一下。

# （1）设置不同作用范围的系统变量

我们前面说过，多个客户端程序可以同时连接到一个服务器程序。对于同一个系统变量，我们有时想让不同的客户端有不同的值。比方说狗哥使用客户端A，他想让当前客户端对应的默认存储引擎为InnoDB，所以他可以把系统变量default_storage_engine的值设置为InnoDB；猫爷使用客户端B，他想让当前客户端对应的默认存储引擎为MyISAM，所以他可以把系统变量default_storage_engine的值设置为MyISAM。这样可以使狗哥和猫爷的的客户端拥有不同的默认存储引擎，且在使用时互不影响，十分方便。但是，这样一来各个客户端都私有一份系统变量，这会产生两个问题。

- 有一些系统变量并不是针对单个客户端的，比如允许同时连接到服务器的客户端数量max(connections、查询缓存的大小query_cache_size，这些公有的系统变量让某个客户端私有显然不合适。  
- 一个新客户端连接到服务器时，与它对应的系统变量的值该怎么设置。

为了解决这两个问题，设计MySQL的大叔提出了系统变量的作用范围的概念。具体来说，作用范围分为下面两种。

- GLOBAL（全局范围）：影响服务器的整体操作。具有GLOBAL作用范围的系统变量可以称为全面变量。  
- SESSION（会话范围）：影响某个客户端连接的操作。具有SESSION作用范围的系统变量可以称为会话变量。

服务器在启动时，会将每个全局变量初始化为其默认值（可以通过命令行或配置文件中指定的选项更改这些默认值）。服务器还为每个连接的客户端维护一组会话变量，客户端的会话变量在连接时使用相应全局变量的当前值进行初始化（也有一些会话变量不依据相应的全局变量值进行初始化，不过这里不展开唠叨了）。

这话有点儿绕，还是以 default_storage_engine 为例来解释。在服务器启动时会初始化一

个名为 default_storage_engine、作用范围为 GLOBAL 的系统变量。之后每当有一个客户端连接到该服务器时，服务器都会单独为该客户端分配一个名为 default_storage_engine、作用范围为SESSION 的系统变量，这个作用范围为 SESSION 的系统变量值按照当前作用范围为 GLOBAL 的同名系统变量值进行初始化。

很显然，通过启动选项设置的系统变量的作用范围都是GLOBAL的，因为在服务器启动的时候还没有客户端程序连接进来呢。了解了系统变量的GLOBAL和SESSION作用范围之后，我们再看一下在服务器程序运行期间通过客户端程序设置系统变量的语法：

SET [GLOBAL|SESSION] 系统变量名 = 值;

或者写成这样也行：

SET[@@(GLOBAL|SESSION).]系统变量名 = 值;

比如，我们想在服务器的运行过程中把作用范围为GLOBAL的系统变量default_storageengine的值修改为MyISAM，也就是想让之后新连接到服务器的客户端都用MyISAM作为默认的存储引擎，则可以选择下面两条语句中的任意一条来设置。

- 语句1：SET GLOBAL default_storage_engine = MyISAM;   
- 语句2：SET@@GLOBAL.default_storage_engine = MyISAM;

如果只想对本客户端生效，也可以选择下面3条语句中的任意一条来设置。

- 语句1：SETSESSION default_storage_engine = MyISAM;   
- 语句2: SET@@SESSION.default_storage_engine = MyISAM;   
- 语句3：SET default_storage_engine = MyISAM;

从上面的语句3也可以看出，如果在设置系统变量的语句中省略了作用范围，默认的作用范围就是SESSION。也就是说“SET系统变量名=值”和“SETSESSION系统变量名=值”是等价的。

（2）查看不同作用范围的系统变量

我们可以在查看系统变量的语句中加上要查看哪个作用范围的系统变量的修饰符，就像下面这样：

SHOW [GLOBAL|SESSION] VARIABLES [LIKE 匹配的模式];

- 如果使用 GLOBAL 修饰符，则显示全局系统变量的值。如果某个系统变量没有 GLOBAL 作用范围，则不显示它。  
- 如果使用SESSION 修饰符，则显示针对当前连接有效的系统变量值。如果某个系统变量没有SESSION 作用范围，则显示 GLOBAL 作用范围的值。  
- 如果没写修饰符，则与使用SESSION 修饰符效果一样。

下面演示一下完整地设置并查看系统变量的过程：

```txt
mysql>SHOW SESSION VARIABLES LIKE 'default_storage_engine';  
+  
Variable_name | Value  
+  
| default_storage_engine | InnoDB | 
```

```txt
1 row in set (0.00 sec)  
mysql> SHOW GLOBAL VARIABLES LIKE 'default_storage_engine';  
+  
Variable_name | Value |  
+  
| default_storage_engine | InnoDB |  
+  
1 row in set (0.00 sec)  
mysql> SET SESSION default_storage_engine = MyISAM;  
Query OK, 0 rows affected (0.00 sec)  
mysql> SHOW SESSION VARIABLES LIKE 'default_storage_engine';  
+  
Variable_name | Value |  
+  
| default_storage_engine | MyISAM |  
+  
1 row in set (0.00 sec)  
mysql> SHOW GLOBAL VARIABLES LIKE 'default-storage_engine';  
+  
Variable_name | Value |  
+  
| default_storage_engine | InnoDB |  
+  
1 row in set (0.00 sec) 
```

可以看到，最初 default_storage_engine 的系统变量无论是在 GLOBAL 作用范围还是在 SESSION 作用范围，值都是 InnoDB。我们把 SESSION 作用范围的系统变量值设置为 MyISAM 之后，可以看到 GLOBAL 作用范围的值并没有改变。

![](images/11ed34a6e05a9a8ba773da0cb991b6589c7ebe5695be501cff2ed44794b78290.jpg)

小贴士

如果某个客户端改变了某个系统变量在GLOBAL作用范围的值，并不会影响该系统变量在当前已经连接的客户端作用范围为SESSION的值，只会影响后续连入的客户端作用范围为SESSION的值。

（3）注意事项

- 并不是所有的系统变量都具有 GLOBAL 和 SESSION 的作用范围。

- 有一些系统变量只具有 GLOBAL 作用范围，比如 max Connections，它表示服务器程序支持同时最多有多少个客户端程序进行连接。  
- 有一些系统变量只具有SESSION 作用范围，比如 insert_id，它表示在对某个包含 AUTO_INCREMENT 列的表进行插入时，该列初始的值。  
- 有一些系统变量的值既具有 GLOBAL 作用范围，也具有 SESSION 作用范围，比如前面用到的 default_storage_engine，而且其实大部分的系统变量都是这样的。

- 有些系统变量是只读的，并不能设置值。

比如 version，它表示当前 MySQL 的版本。客户端不能设置它的值，只能在 SHOW VARIABLES 语句中查看。

# 3. 启动选项和系统变量的区别

启动选项是在程序启动时由用户传递的一些参数，而系统变量是影响服务器程序运行行为的变量。它们之间的关系如下。

- 大部分的系统变量都可以当作启动选项传入。  
- 有些系统变量是在程序运行过程中自动生成的，不可以当作启动选项来设置，比如 character_set_client。  
- 有些启动选项也不是系统变量，比如 defaults-file。

# 2.3 状态变量

为了让我们更好地了解服务器程序的运行情况，MySQL 服务器程序中维护了好多关于程序运行状态的变量，它们被称为状态变量。比如，Threads_CONNECTED 表示当前有多少客户端与服务器建立了连接；Innodb_rows_update 表示更新了多少条以 InnoDB 为存储引擎的表中的记录。像这样显示服务器程序状态信息的状态变量还有好几百个，我们就不一一唠叨了，等遇到时再详细说明它们的作用。

由于状态变量是用来显示服务器程序运行状态的，所以它们的值只能由服务器程序自己来设置，不能人为设置。与系统变量类似，状态变量也有GLOBAL和SESSION两个作用范围，查看状态变量的语句可以这么写：

```txt
SHOW [GLOBAL|SESSION] STATUS [LIKE 匹配的模式]; 
```

类似地，如果不写修饰符，则与使用SESSION 修饰符效果一样。

我们看一下所有以Thread开头的状态变量的值都是什么：

```txt
mysql>SHOW STATUS LIKE 'thread%';  
+-----------------------------------+  
| Variable_name | Value |  
+-----------------------------------+  
| Threads_cached | 0 |  
| Threads_CONNECTED | 1 |  
| Threads_created | 1 |  
| Threads_runting | 1 |  
+-----------------------------------+  
4 rows in set (0.00 sec) 
```

# 2.4 总结

启动选项可以调整服务器启动后的一些行为。它们可以在命令行中指定，也可以将它们写入配置文件中。

在命令行中指定启动选项时，可以将各个启动选项写到一行中，每一个启动选项名称前面添加--，而且各个启动选项之间使用空白字符隔开。有一些启动选项不需要指定选项值，有一些选项需要指定选项值。在命令行中指定有值的启动选项时需要注意，选项名、=、选项值之

间不可以有空白字符。一些常用的启动选项具有短形式的选项名，使用短形式选项时在选项名前只加一个短划线-前缀。

服务器程序在启动时将会在一些给定的路径下搜索配置文件，不同操作系统的搜索路径是不同的。

配置文件中的启动选项被划分为若干个组，每个组有一个组名，用中括号[]扩起来。在配置文件中指定的启动选项不允许添加--前缀，并且每行只指定一个选项，而且等号 $=$ 周围可以有空白字符。我们可以使用#来添加注释。

系统变量是服务器程序中维护的一些变量，这些变量影响着服务器的行为。修改系统变量的方式如下。

- 在服务器启动时通过添加相应的启动选项进行修改。  
- 在运行时使用 SET 语句修改，下面两种方式都可以：

SET [GLOBAL|SESSION] 系统变量名 = 值；   
SET [@@(GLOBAL|SESSION).] 系统变量名 = 值：

查看系统变量的方式如下所示：

```sql
SHOW [GLOBAL|SESSION] VARIABLES [LIKE 匹配的模式]; 
```

状态变量是用来显示服务器程序运行状态的，我们可以使用下面的命令来查看，而且只能查看：

```txt
SHOW [GLOBAL|SESSION] STATUS [LIKE 匹配的模式]; 
```

# 第3章 字符集和比较规则

# 3.1 字符集和比较规则简介

# 3.1.1 字符集简介

我们知道，计算机中实际存储的是二进制数据，那它是怎么存储字符串呢？当然是建立字符与二进制数据的映射关系了。要建立这个关系，最起码要搞清楚下面这两件事儿。

- 要把哪些字符映射成二进制数据？也就是界定字符范围。  
- 怎么映射？将字符映射成二进制数据的过程叫作编码，将二进制数据映射到字符的过程叫作解码。

人们抽象出一个字符集的概念来描述某个字符范围的编码规则。比如，我们自定义一个名称为xiaohaizi4919的字符集，它包含的字符范围和编码规则如下。

- 包含字符 'a'、'b'、'A'、'B'。  
- 编码规则为一个字节编码一个字符的形式。字符和字节的映射关系如下。

'a' -> 00000001（十六进制0x01）  
'b' -> 00000010（十六进制0x02）  
'A' -> 00000011（十六进制0x03）  
'B' -> 00000100（十六进制0x04）

![](images/8fb5cc52669abf01eca4c2ee78c5a02da97c4d0ed0c3850094047f833d85837f.jpg)

xiaohaizi4919字符集在现实生活中并没有，它是我自定义的字符集！是我自定义的字符集！是我自定义的字符集！（重要的事情讲三遍）

有了xiaohaizi4919字符集，我们就可以用二进制形式表示一些字符串了。下面是一些字符串用xiaohaizi4919字符集编码后的二进制表示：

$\bullet \mathrm{bA}^{\prime}\rightarrow 000001000000011$ （十六进制 $0\mathrm{x}0203)$   
baB $\rightarrow >000000100000000100000100$ （十六进制 $0\mathrm{x}020104)$   
- 'cd' 无法表示，因为字符集 xiaohaizi4919 不包含字符 'c' 和 'd'。

# 3.1.2 比较规则简介

在确定了xiaohaizi4919字符集表示的字符范围以及编码规则后，该怎么比较两个字符的大小呢？最容易想到的就是直接比较这两个字符对应的二进制编码的大小。比如字符'a'的编码为0x01，字符'b'的编码为0x02，所以'a'小于'b'。这种简单的比较规则也可以称为二进制比较规则。

二进制比较规则尽管很简单，但有时候并不符合现实需求。比如，在很多场合下，英文字符都是不区分大小写的，也就是说‘a’和‘A’是相等的。此时就不能简单粗暴地使用二进制比较规则了，这时可以这样指定比较规则：

- 将两个大小写不同的字符全都转为大写或者小写；  
- 再比较这两个字符对应的二进制数据。

这是一种稍微复杂一点儿的比较规则，但是实际生活中的字符不止英文字符这一种，还有中文字符、德文字符、法文字符等。对于某一种字符集来说，可以制定用来比较字符大小的多种规则，也就是说同一种字符集可以有多种比较规则。稍后将介绍现实生活中使用的各种字符集以及它们的一些比较规则。

# 3.1.3 一些重要的字符集

我们所在的世界实在太大了，不同的人制定出了不同的字符集，它们表示的字符范围和用到的编码规则可能都不一样。我们看一下一些常用字符集的情况。

- ASCII字符集：共收录128个字符，包括空格、标点符号、数字、大小写字母和一些不可见字符。由于ASCII字符集总共才128个字符，所以可以使用一个字节来进行编码。我们来看几个字符的编码方式：

L'->01001100（十六进制0x4C，十进制76）

'M' -> 01001101（十六进制0x4D，十进制77）

- ISO 8859-1 字符集：共收录 256 个字符，它在 ASCII 字符集的基础上又扩充了 128 个西欧常用字符（包括德法两国的字母）。ISO 8859-1 字符集也可以使用一个字节来进行编码（这个字符集也有一个别名 Latin1）。  
- GB2312字符集：收录了汉字以及拉丁字母、希腊字母、日文平假名及片假名字母、俄语西里尔字母，收录汉字6763个，收录其他文字符号682个。这种字符集同时又兼容ASCII字符集，所以在编码方式上显得有些奇怪：如果该字符在ASCII字符集中，则采用一字节编码；否则采用两字节编码。

这种使用不同字节数来表示一个字符的编码方式称为变长编码方式。比如字符串“爱u”，其中的'爱'需要用2字节进行编码，编码后的十六进制表示为0xB0AE；'u'需要用1字节进行编码，编码后的十六进制表示为0x75，所以拼合起来就是0xB0AE75。

![](images/d83feabe4e303d616ed12e879029776369aa265dc2113c15cd6404f1e4f1a57f.jpg)

计算机在读取一个字节序列时，怎么区分某个字节代表的是一个单独的字符还是某个字符的一部分呢？别忘了ASCII字符集只收录128个字符，使用 $0\sim 127$ 就可以表示全部字符。所以，如果某个字节是在 $0\sim 127$ 之内（该字节的最高位为0），就意味着一个字节代表一个单独的字符，否则（该字节的最高位为1）就是两个字节代表一个单独的字符。

- GBK字符集：GBK字符集只是在收录的字符范围上对GB2312字符集进行了扩充，编码方式兼容GB2312字符集。  
- UTF-8字符集：几乎收录了当今世界各个国家/地区使用的字符，而且还在不断扩充。这种字符集兼容ASCII字符集，采用变长编码方式，编码一个字符时需要使用 $1\sim 4$ 字节，比如下面这样：

L'-> 01001100（1字节，十六进制0x4C）

啊' $\rightarrow$ 111001011001010110001010（3字节，十六进制0xE5958A）

![](images/46236c8d4bd329a76bb4e0f090c57ebb6d542880b6069ae4b21c7d31fa7f60aa.jpg)

# 小贴士

其实准确地说，UTF-8只是Unicode字符集的一种编码方案，Unicode字符集可以采用UTF-8、UTF-16、UTF-32这几种编码方案。UTF-8使用 $1\sim 4$ 字节编码一个字符，UTF-16使用2或4字节编码一个字符，UTF-32使用4字节编码一个字符。更详细的Unicode及其编码方案的知识不是本书的重点，大家可以自行查阅。

MySQL并不区分字符集和编码方案的概念，所以后面唠叨的时候会把UTF-8、UTF-16、UTF-32都当作一种字符集对待。

对于同一个字符，不同字符集可能采用不同的编码方式。比如对于汉字'我'来说，ASCII字符集中根本没有收录这个字符，UTF-8和GB2312字符集对汉字'我'的编码方式如下。

- UTF-8编码：111001101000100010010001（3字节，十六进制形式为0xE68891）。  
- GB2312编码：1100111011010010（2字节，十六进制形式为0xCED2）。

# 3.2 MySQL中支持的字符集和比较规则

# 3.2.1 MySQL中的utf8和utf8mb4

前文讲到，UTF-8字符集在表示一个字符时需要使用 $1\sim 4$ 字节，但是我们常用的一些字符使用 $1\sim 3$ 字节就可以表示了。而在MySQL中，字符集表示一个字符所用的最大字节长度在某些方面会影响系统的存储和性能。设计MySQL的大叔“偷偷”地定义了下面两个概念。

- utf8mb3: “阉割”过的 UTF-8 字符集, 只使用 $1 \sim 3$ 字节表示字符。  
- utf8mb4：正宗的 UTF-8 字符集，使用 $1 \sim 4$ 字节表示字符。

有一点需要注意：在MySQL中，utf8是utf8mb3的别名，所以后文在MySQL中提到utf8时，就意味着使用 $1\sim 3$ 字节来表示一个字符。如果大家有使用4字节编码一个字符的情况，比如存储一些emoji表情，请使用utf8mb4。

![](images/9d2b9bfb1a05b2c94df005d14e86d748371eeb62543796cac9edee68effa809e.jpg)

# 小贴士

在MySQL8.0中，设计MySQL的大叔已经很大程度地优化了utf8mb4字符集的性能，而且已经将其设置为默认的字符集。

# 3.2.2 字符集的查看

MySQL支持非常多的字符集，可以用下面这个语句来查看当前MySQL中支持的字符集：

SHOW (CHARACTER SET|CHARSET) [LIKE 匹配的模式];

其中，CHARACTER SET 和 CHARSET 是同义词，用任意一个都可以。在后文中用到 CHARACTER SET 的地方都可以用 CHARSET 替换，我们就不强调了。我们执行一下上述语句（由于支持的字符集太多，这里省略了一些）：

```typescript
mysql>SHOWCHARSET; ++ |Charetd|Description |Defaultcollation |Maxlen| 
```

```csv
big5 | Big5 Traditional Chinese | big5_chinese_ci | 2 |
... |
| latin1 | cpl252 West European | latin1_swedish_ci | 1 |
| latin2 | ISO 8859-2 Central European | latin2_general_ci | 1 |
... |
|ascii | US ASCII |ascii_general_ci | 1 |
... |
| gb2312 | GB2312 Simplified Chinese | gb2312_chinese_ci | 2 |
... |
| gbk | GBK Simplified Chinese | gbk_chinese_ci | 2 |
| latin5 | ISO 8859-9 Turkish | latin5_turkish_ci | 1 |
... |
| utf8 | UTF-8 Unicode | utf8_general_ci | 3 |
| ucs2 | UCS-2 Unicode | ucs2_general_ci | 2 |
... |
| latin7 | ISO 8859-13 Baltic | latin7_general_ci | 1 |
| utf8mb4 | UTF-8 Unicode | utf8mb4_general_ci | 4 |
| utf16 | UTF-16 Unicode | utf16_general_ci | 4 |
| utf16le | UTF-16LE Unicode | utf16le_general_ci | 4 |
... |
| utf32 | UTF-32 Unicode | utf32_general_ci | 4 |
| binary | Binary pseudo charset | binary | 1 |
... |
| gb18030 | China National Standard GB18030 | gb18030_chinese_ci | 4 |
+  
41 rows in set (0.01 sec) 
```

![](images/3193aa7d20119135a137939ef38ee6759eeb675d28566e58341386b248aa5b7c.jpg)

# 小贴士

从输出中可以看到，MySQL中表示字符集的名称时使用小写形式。

我使用的这个MySQL版本一共支持41种字符集，其中Default collation列表示这种字符集中一种默认的比较规则。大家注意返回结果中的最后一列Maxlen，它代表这种字符集最多需要几个字节来表示一个字符。为了让大家的印象更深刻，我把几个常用字符集的Maxlen列摘抄下来（见表3-1），大家务必记住。

表 3-1 字符集名称及其 Maxlen 列  

<table><tr><td>字符集名称</td><td></td><td>Maxlen</td></tr><tr><td>ascii</td><td></td><td>1</td></tr><tr><td>latin1</td><td></td><td>1</td></tr><tr><td>gb2312</td><td></td><td>2</td></tr><tr><td>gbk</td><td></td><td>2</td></tr><tr><td>utf8</td><td></td><td>3</td></tr><tr><td>utf8mb4</td><td></td><td>4</td></tr></table>

# 3.2.3 比较规则的查看

可以使用如下命令来查看MySQL中支持的比较规则：

```javascript
SHOWCOLLATION[LIKE匹配的模式]；
```

前文说过，一种字符集可能对应着若干种比较规则。MySQL支持的字符集已经非常多，所以支持的比较规则就更多了。我们先看一下utf8字符集下的比较规则：

```txt
mysql>SHOW COLLATION LIKE 'utf8\%';  
COLLATION | Charet | Id | Default | Compiled | Sortlen |  
| utf8_general_ci | utf8 | 33 | Yes | Yes | 1 |  
| utf8_bin | utf8 | 83 | | Yes | 1 |  
| utf8_unICODE_ci | utf8 | 192 | | Yes | 8 |  
| utf8_icelandic_ci | utf8 | 193 | | Yes | 8 |  
| utf8_latvian_ci | utf8 | 194 | | Yes | 8 |  
| utf8_romanian_ci | utf8 | 195 | | Yes | 8 |  
| utf8_slovenian_ci | utf8 | 196 | | Yes | 8 |  
| utf8_polish_ci | utf8 | 197 | | Yes | 8 |  
| utf8_estonian_ci | utf8 | 198 | | Yes | 8 |  
| utf8_spanish_ci | utf8 | 199 | | Yes | 8 |  
| utf8_swedish_ci | utf8 | 200 | | Yes | 8 |  
| utf8_turkish_ci | utf8 | 201 | | Yes | 8 |  
| utf8_czech_ci | utf8 | 202 | | Yes | 8 |  
| utf8_danish_ci | utf8 | 203 | | Yes | 8 |  
| utf8_lithuanian_ci | utf8 | 204 | | Yes | 8 |  
| utf8_slovak_ci | utf8 | 205 | | Yes | 8 |  
| utf8_spanish2_ci | utf8 | 206 | | Yes | 8 |  
| utf8_roman_ci | utf8 | 207 | | Yes | 8 |  
| utf8_persian_ci | utf8 | 208 | | Yes | 8 |  
| utf8esperanto_ci | utf8 | 209 | | Yes | 8 |  
| utf8_hungarian_ci | utf8 | 210 | | Yes | 8 |  
| utf8_sinhala_ci | utf8 | 211 | | Yes | 8 |  
| utf8_german2_ci | utf8 | 212 | | Yes | 8 |  
| utf8_croatian_ci | utf8 | 213 | | Yes | 8 |  
| utf8_unicode_520_ci | utf8 | 214 | | Yes | 8 |  
| utf8_vietnamese_ci | utf8 | 215 | | Yes | 8 |
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---.
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
|---|---|
)
```

```txt
27 ows in set (0.00 sec) 
```

这些比较规则的命名还都挺有规律的，具体如下。

- 比较规则的名称以与其关联的字符集的名称开头。比如在上面的查询结果中，比较规则的名称都是以utf8 开头的。  
- 后面紧跟着该比较规则所应用的语言。比如，utf8_polish_ci 表示波兰语的比较规则；utf8_spanish_ci 表示班牙语的比较规则；utf8_general_ci 是一种通用的比较规则。  
- 名称后缀意味着该比较规则是否区分语言中的重音、大小写等，具体可用的值如表3-2所示。

表 3-2 比较规则名称后缀英文释义及描述  

<table><tr><td>后缀</td><td>英文释义</td><td>描述</td></tr><tr><td>.ai</td><td>accent insensitive</td><td>不区分重音</td></tr><tr><td>_as</td><td>accent sensitive</td><td>区分重音</td></tr><tr><td>_ci</td><td>case insensitive</td><td>不区分大小写</td></tr><tr><td>-CS</td><td>case sensitive</td><td>区分大小写</td></tr><tr><td>_bin</td><td>binary</td><td>以二进制方式比较</td></tr></table>

比如比较规则utf8_general_ci是以ci结尾的，说明不区分大小写。

每种字符集对应若干种比较规则，且每种字符集都有一种默认的比较规则。在执行SHOW COLLATION语句后返回的结果中，Default列的值为YES的比较规则，就是该字符集的默认比较规则，比如utf8字符集默认的比较规则就是utf8_general_ci。

# 3.3 字符集和比较规则的应用

# 3.3.1 各级别的字符集和比较规则

MySQL有4个级别的字符集和比较规则，分别是服务器级别、数据库级别、表级别、列级别。

下面仔细看一下怎么设置和查看这几个级别的字符集和比较规则。

# 1. 服务器级别

MySQL 提供了两个系统变量来表示服务器级别的字符集和比较规则，如表 3-3 所示。

表 3-3 服务器级别的字符集和比较规则对应的系统变量及其描述  

<table><tr><td></td><td>系统变量</td><td></td><td>描述</td></tr><tr><td></td><td>character_set_server</td><td></td><td>服务器级别的字符集</td></tr><tr><td></td><td>collation_server</td><td></td><td>服务器级别的比较规则</td></tr></table>

我们看一下这两个系统变量的值：

```txt
mysql>SHOW VARIABLES LIKE 'character_set_server';  
+  
Variable_name | Value |  
+  
| character_set_server | utf8 |  
+  
1 row in set (0.00 sec)  
mysql>SHOW VARIABLES LIKE 'collation_server'; 
```

```txt
+  
Variable_name | Value  
+  
collation_server | utf8_general_ci  
+  
1 row in set (0.00 sec) 
```

可以看到，在我的计算机中，MySQL服务器级别默认的字符集是utf8，默认的比较规则是utf8_general_ci。

在启动服务器程序时，可以通过启动选项或者在服务器程序运行过程中使用SET语句来修改这两个变量的值。比如，我们可以在配置文件中这样写：

```ini
[server]  
character_set_server=gb2312  
collation_server=gb2312_chinese_ci 
```

当服务器在启动时读取这个配置文件后，这两个系统变量的值便修改了。

# 2. 数据库级别

我们在创建和修改数据库时可以指定该数据库的字符集和比较规则，具体语法如下：

```sql
CREATE DATABASE 数据库名  
[[DEFAULT] CHARACTER SET 字符集名称]  
[[DEFAULT] COLLATE 比较规则名称];
```

```txt
ALTER DATABASE 数据库名  
[[DEFAULT] CHARACTER SET 字符集名称]  
[[DEFAULT] COLLATE 比较规则名称]；
```

其中的DEFAULT可以省略，并不影响语句的语义。比如，我们新建一个名为subsetdemo_db的数据库，在创建时指定它使用的字符集为gb2312，比较规则为gb2312_chinese_ci：

```txt
mysql> CREATE DATABASE charset_demo_db  
-> CHARACTER SET gb2312  
-> COLLATE gb2312_chinese_ci;  
Query OK, 1 row affected (0.01 sec) 
```

如果想查看当前数据库使用的字符集和比较规则，可以查看表3-4中的两个系统变量的值（前提是使用USE语句选择当前的默认数据库。如果没有默认数据库，则变量与服务器级别下相应的系统变量具有相同的值）。

表 3-4 数据库级别的字符集和比较规则对应的系统变量及描述  

<table><tr><td>系统变量</td><td>描述</td></tr><tr><td>character_set_database</td><td>当前数据库的字符集</td></tr><tr><td>collation_database</td><td>当前数据库的比较规则</td></tr></table>

我们来看一下刚刚创建的 charset_demo_db 数据库的字符集和比较规则：

```sql
mysql>USE charset_demo_db; Database changed   
mysql>SHOW VARIABLES LIKE 'character_set_database'; 
```

```txt
+ Variable_name | Value |
+ + + + + |
| character_set_database | gb2312 |
+ + + + |
| row in set (0.00 sec) 
```

```txt
mysql>SHOW VARIABLES LIKE 'collation_database';  
+  
Variable_name | Value  
+  
| collation_database | gb2312_chinese_ci |  
+  
1 row in set (0.00 sec) 
```

可以看到，这个 charset_demo_db 数据库的字符集和比较规则就是我们在创建数据库语句时指定的。需要注意的一点是，character_set_database 和 collation_database 这两个系统变量只是用来告诉用户当前数据库的字符集和比较规则是什么。我们不能通过修改这两个变量的值来改变当前数据库的字符集和比较规则。

在数据库的创建语句中也可以不指定字符集和比较规则，比如这样：

CREATE DATABASE 数据库名；

这将使用服务器级别的字符集和比较规则作为数据库的字符集和比较规则。

# 3. 表级别

我们也可以在创建和修改表的时候指定表的字符集和比较规则，语法如下：

```txt
CREATE TABLE 表名（列的信息）  
[[DEFAULT] CHARACTER SET 字符集名称]  
[COLLATE 比较规则名称]；
```

```sql
ALTER TABLE 表名  
[[DEFAULT] CHARACTER SET 字符集名称]  
[COLLATE 比较规则名称]；
```

比如，我们在刚刚创建的 charset_demo_db 数据库中创建一个名为 t 的表，并指定这个表的字符集和比较规则：

```txt
mysql>USE charset_demo_db  
Database changed  
mysql> CREATE TABLE t(  
-> colVARCHAR(10)  
->) CHARACTER SET utf8 COLLATE utf8_general_ci;  
Query OK, 0 rows affected (0.03 sec) 
```

如果创建表的语句中没有指明字符集和比较规则，则使用该表所在数据库的字符集和比较规则作为该表的字符集和比较规则。假设表t的建表语句是这么写的：

```sql
CREATE TABLE t(
col VARCHAR(10);
);
```

因为表t的建表语句中并没有明确指定字符集和比较规则，所以表t的字符集和比较规则将继续所在数据库charAtdemo_db的字符集和比较规则，也就是gb2312和gb2312_chinese_c。

# 4. 列级别

需要注意的是，对于存储字符串的列，同一个表中不同的列也可以有不同的字符集和比较规则。我们在创建和修改列的时候可以指定该列的字符集和比较规则，语法如下：

```txt
CREATE TABLE 表名（列名字符串类型 [CHARACTER SET 字符集名称] [COLLATE 比较规则名称]，其他列...）；
```

ALTER TABLE 表名 MODIFY 列名 字符串类型 [CHARACTER SET 字符集名称] [COLLATE 比较规则名称];

比如我们修改一下表t中列col的字符集和比较规则，可以这么写：

```txt
mysql> ALTER TABLE t MODIFY colVARCHAR(10) CHARACTER SET gbk COLLATE gbk_chinese_ci;  
Query OK, 0 rows affected (0.04 sec)  
Records: 0 Duplicates: 0Warnings: 0 
```

对于某个列来说，如果在创建和修改表的语句中没有指明字符集和比较规则，则使用该列所在表的字符集和比较规则作为其字符集和比较规则。比如，表t的字符集是utf8，比较规则是utf8_general_ci，修改列col的语句是这么写的：

ALTER TABLE t MODIFY colVARCHAR(10);

这样一来，列col的字符集和比较规则将使用表t的字符集和比较规则，也就是utf8和utf8_general_ci。

![](images/49f1cd10a59562da3eef7e9a50698a2478b1b4e46d6c9372b264063582de35d3.jpg)

小贴士

在修改列的字符集时需要注意，如果列中存储的数据不能用修改后的字符集进行表示，则会发生错误。比如，列最初使用的字符集是utf8，列中存储了一些汉字，现在把列的字符集转换为ascii的话就会出错，因为ascii字符集并不能表示汉字字符。

# 5. 仅修改字符集或仅修改比较规则

由于字符集和比较规则之间相互关联，因此如果只修改字符集，比较规则也会跟着变化；如果只修改比较规则，字符集也会跟着变化。具体规则如下：

- 只修改字符集，则比较规则将变为修改后的字符集默认的比较规则；  
- 只修改比较规则，则字符集将变为修改后的比较规则对应的字符集。

无论哪个级别的字符集和比较规则，这两条规则都适用。我们以服务器级别的字符集和比较规则为例来看一下详细过程。

- 只修改字符集，则比较规则将变为修改后的字符集默认的比较规则。

```sql
mysql> SET character_set_server = gb2312;  
Query OK, 0 rows affected (0.00 sec)  
mysql> SHOW VARIABLES LIKE 'character_set_server'; 
```

```txt
| Variable_name | Value |
|--------|--------|
| character_set_server | gb2312 |
|--------|--------|
| 1 row in set (0.00 sec) |
| mysql> SHOW VARIABLES LIKE 'collation_server'; |
|--------|--------|
| Variable_name | Value |
|--------|--------|
| collation_server | gb2312_chinese_ci |
|--------|--------|
| 1 row in set (0.00 sec) | 
```

我们只将 character_set_server 的值修改为 GB2312，collation_server 的值自动变为了 GB2312_chinese_ci。

- 只修改比较规则，则字符集将变为修改后的比较规则对应的字符集。

```txt
mysql> SET collation_server = utf8_general_ci;  
Query OK, 0 rows affected (0.00 sec)  
mysql> SHOW VARIABLES LIKE 'character_set_server';  
+-----------------------------------+  
| Variable_name | Value |  
+-----------------------------------+  
| character_set_server | utf8 |  
+-----------------------------------+  
1 row in set (0.00 sec)  
mysql> SHOW VARIABLES LIKE 'collation_server';  
+-----------------------------------+  
| Variable_name | Value |  
+-----------------------------------+  
| collation_server | utf8_general_ci |  
+-----------------------------------+  
1 row in set (0.00 sec) 
```

我们只将collation_server的值修改为为utf8_general_ci，character_set_server的值自动变为了utf8。

# 6. 各级别字符集和比较规则小结

前文介绍的这4个级别的字符集和比较规则的联系如下：

- 如果创建或修改列时没有显式指定字符集和比较规则，则该列默认使用表的字符集和比较规则；  
- 如果创建表时没有显式指定字符集和比较规则，则该表默认使用数据库的字符集和比较规则；  
- 如果创建数据库时没有显式指定字符集和比较规则，则该数据库默认使用服务器的字符集和比较规则。

知道了这些规则后，对于给定的表，我们应该知道它的各个列的字符集和比较规则是什么，从而根据这个列的类型来确定每个列存储的实际数据所占用的存储空间大小。比如我们向

表 t 中插入一条记录：

```sql
mysql> INSERT INTO t(col) VALUES('我我');  
Query OK, 1 row affected (0.00 sec)  
mysql> SELECT * FROM t;  
+-----------------------------------+  
| col |  
+-----------------------------------+  
| 我我 |  
+-----------------------------------+  
1 row in set (0.00 sec) 
```

如果列 col 使用的字符集是 gbk，一个字符 '我' 在 gbk 中的编码为 0xCED2，占用 2 字节，则两个字符就占用 4 字节。如果把该列的字符集修改为 utf8，这两个字符实际占用的存储空间就是 6 字节了。

# 3.3.2 客户端和服务器通信过程中使用的字符集

# 1. 编码和解码使用的字符集不一致

说到底，字符串在计算机上的体现就是一个字节序列。如果使用不同的字符集去解码这个字节序列，最后得到的结果可能让你挠头。

我们知道，字符串'我'在UTF-8字符集编码下的字节序列是0xE68891。如果程序A把这个字节序列发送到程序B，程序B使用不同的字符集解码这个字节序列（假设使用的是GBK字符集），解码过程如下所示。

1. 首先看第一个字节0xE6，它的值大于0x7F（十进制127），说明待读取字符是两字节编码。继续读一字节后得到0xE688，然后从GBK编码表中查找字节为0xE688对应的字符，发现是字符'鎰'。  
2. 继续读一个字节0x91，它的值也大于0x7F，试图再读一个字节时发现后边没有了，所以这是半个字符。  
3. 最终，0xE68891 被 GBK 字符集解释成一个字符 '鎳' 和半个字符。

假设使用ISO-8859-1（也就是Latin1字符集）去解释这串字节，解码过程如下。

1. 先读第一个字节0xE6，它对应的Latin1字符为 $\mathfrak{a}$   
2. 再读第二个字节 0x88，它对应的 Latin1 字符为^。  
3. 再读第三个字节 0x91，它对应的 Latin1 字符为'。  
4. 所以整串字节 0xE68891 被 Latin1 字符集解释后的字符串就是 “æ`”。

有上可见，对于同一个字符串，如果编码和解码使用的字符集不一样，会产生意想不到的结果。在我们看来就像是产生了乱码一样。

# 2.字符集转换的概念

如果接收0xE68891这个字节序列的程序按照UTF-8字符集进行解码，然后又把它按照GBK字符集进行编码，则编码后的字节序列就是0xCED2。我们把这个过程称为字符集的转换，也就是字符串'我'从UTF-8字符集转换为GBK字符集。

# 3. MySQL中的字符集转换过程

如果我们仅仅把MySQL当作一个软件，那么从用户的角度来看，客户端发送的请求以及服务器返回的响应都是一个字符串。但是从机器的角度来看，客户端发送的请求和服务器返回的响应本质上就是一个字节序列。在这个“客户端发送请求，服务器返回响应”的过程中，其实经历了多次的字符集转换。下面详细分析一下。

# 客户端发送请求

MySQL 客户端发送给服务器的请求以及服务器返回给客户端的响应，其实都遵从了一定的格式（这个“格式”指明了请求和响应的每一个字节分别代表什么意思）。我们把 MySQL 客户端与服务器进行通信的过程中事先规定好的数据格式称为 MySQL 通信协议。由于 MySQL 本身是开源软件，因此可以直接分析代码来了解这个协议。即使不想查看源码，也可以简单地使用诸如 Wireshark 等抓包软件来分析这个协议。在了解了 MySQL 通信协议之后，我们甚至可以动手制作自己的客户端软件。

由于市面上的MySQL客户端软件种类繁多，我们只以MySQL安装目录的bin目录下自带的mysql客户端程序为例进行分析。一般情况下，客户端编码请求字符串时使用的字符集与操作系统当前使用的字符集一致。可以使用下述方法获取操作系统当前使用的字符集。

- 当使用类 UNIX 操作系统时

LC_ALL、LC_CTYPE、LANG 这 3 个环境变量的值决定了操作系统当前使用的是哪种字符集。其中，LC_ALL 的优先级比 LC_CTYPE 高，LC_CTYPE 的优先级比 LANG 高。也就是说，如果设置了 LC_ALL，则无论是否设置了 LC_CTYPE 或者 LANG，最终都以 LC_ALL 为准；如果没有设置 LC_ALL，就以 LC_CTYPE 为准；如果既没有设置 LC_ALL 也没有设置 LC_CTYPE，就以 LANG 为准。

下面看一下这3个变量的值在我的macOS操作系统上分别是什么：

```shell
shell> echo $LC_ALL
zh_CN.UTF-8
shell> echo $LC_CTYPE
shell> echo $LANG 
```

很显然，只设置了 LC_ALL 的值：zh_CN.UTF-8（其中的 zh_CN 表示语言以及国家地区的代码，大家可以忽略）。这就意味着我的 macOS 操作系统当前使用的字符集是 UTF-8。

如果这3个环境变量都没有设置，那么操作系统当前使用的字符集就是其默认的字符集。比如在我的macOS10.15.3操作系统中，默认的字符集为US-ASCII。

![](images/d080f0ec417bf5dcd2bb9c1b016276bb201c8e110fe7b3d82cf346294cc55db4.jpg)

获取类UNIX操作系统当前使用的字符集时，调用的是系统函数nl-langinfo(CODESET)，该函数会分析上述3个系统变量的值。对源码感兴趣的读者可以进一步研究。

- 当使用 Windows 操作系统时

在Windows中，字符集称为代码页（code page），一个代码页与一个唯一的数字相关联。比如，936代表GBK字符集，65001代表UTF-8字符集。我们可以在Windows命令行窗口的

标题栏上单击鼠标右键，在弹出的菜单中单击“属性”子菜单，从弹出的对话框中选择“选项”选项卡，如图3-1所示。

![](images/a9c2accd3b7cf8af69d592b6c8f41086132b1bba37e4890bd7d62685be2ae6ed.jpg)  
图3-1 在Windows中用来查看代码页的选项卡

可以看到，当前代码页的值是936，也就表示当前的命令行窗口使用的是GBK字符集。更简单的方法则是直接运行chcp命令，查看当前代码页是什么，如图3-2所示。

![](images/5b0fd0b201b47197d3b19201c706072e2217a30eecfc97cb6d08921e63a32d36.jpg)  
图3-2 通过执行命令查看Windows的当前代码页

![](images/4629da80e4828d64d777ffe4930a958e6cb1b5fbf41c7600f866e8ca57bc87e5.jpg)

在Windows中获取当前代码页时，调用的系统函数为GetConsoleCP。对源码感兴趣的读者可以进一步研究。

在Windows操作系统中，如果在启动MySQL客户端程序时携带了default-character-set启动选项，那么MySQL客户端将以该启动选项指定的字符集对请求的字符串进行编码（这一点并不适用于类UNIX操作系统）。

比如，我们在Windows的命令行窗口中使用如下命令启动客户端（省略了用户名、密码等其他启动选项）：

```batch
mysql --default-character-set=utf8 
```

那么客户端将会以UTF-8字符集对请求的字符串进行编码。

# 服务器接收请求

从本质上来说，服务器接收到的请求就是一个字节序列。服务器将这个字节序列看作是使用系统变量 character_set_client 代表的字符集进行编码的字节序列（每个客户端与服务器建立连接后，服务器都会为该客户端维护一个单独的 character_set_client 变量，这个变量是SESSION 级别的）。

大家在这里应该意识到一件事儿：客户端在编码请求字符串时实际使用的字符集，与服务器在收到一个字节序列后认为该字节序列所采用的编码字符集，是两个独立的字符集。一般情况下，我们应该尽量保证这两个字符集是一致的。就像我跟你说的是中文，你也要把听到的话当成中文来理解，如果你要把它当成英文来理解，那就把人整迷糊了。

当然，我们并不限制你非要把中文当成英文来理解的权利，就像在MySQL中可以通过SET命令来修改character_set_client的值一样。假如客户端实际使用UTF-8字符集来编码请求的字符串，我们还是可以通过下面的命令将character_set_client设置为latin1字符集：

```txt
SET character_set_client=latin1; 
```

这样一来，就发生了“鸡同鸭讲”的事情。比如，客户端实际发送的是一个汉字字符‘我’（UTF-8的编码为0xE68891），但服务器却将其理解为3个字符：'æ'、''和''。

另外还需要注意的是，如果 character_set_client 对应的字符集不能解释请求的字节序列，那么服务器就会发出警告。比如，客户端实际使用 UTF-8 字符集来编码请求的字符串，我们现在把 character_set_client 设置成ascii 字符集，而请求字符串中包含了一个汉字 '我'（对应的字节序列就是 0xE68891），那么将会发生这样的事情：

```txt
mysql> SET character_set_client=ascii;  
Query OK, 0 rows affected (0.00 sec)  
mysql> SELECT '我';  
+-----------------------------------+  
| ??? |  
+-----------------------------------+  
| ??? |  
+-----------------------------------+  
1 row in set, 1 warning (0.00 sec)  
mysql> SHOWWarnings\G  
********** 1. row ***  
Level: Warning  
Code: 1300  
Message: Invalidascii character string: '\xE6\x88\x91'  
1 row in set (0.00 sec) 
```

从上面的输出结果中可以看到，0xE68891并不是正确的ascii字符。

# 服务器处理请求

我们知道，服务器会将请求的字节序列当作采用 character_set_client 对应的字符集进行编码的字节序列，不过在真正处理请求时又会将其转换为使用 SESSION 级别的系统变量 character_set_connection 对应的字符集进行编码的字节序列。

我们也可以通过SET命令单独修改character_set_connection系统变量。比如，客户端发送给服务器的请求中包含字节序列0xE68891，然后服务器针对该客户端的系统变量character_set_client为utf8，此时服务器就知道该字节序列其实是代表汉字'我'。如果服务器针对该客户端的系统变量character_set_connection为gbk，那么还要在计算机内部将该字符转换为采用gbk字符集编码的形式，也就是0xCD2。

有的同学可能认为这一步骤多此一举了，但是请考虑下面这个查询语句：

```sql
mysql> SELECT 'a' = 'A'; 
```

这个查询语句的返回结果是TRUE还是FALSE？其实仅仅根据这个语句是不能确定结果的。这是因为我们并不知道这两个字符串到底采用了什么字符集进行编码，也不知道这里使用的比较规则是什么。

此时，character_set_connection 系统变量就发挥了作用，它表示这些字符串应该使用哪种字符集进行编码。当然，还有一个与之配套的系统变量 collation_connection，这个系统变量表示这些字符串应该使用哪种比较规则。现在通过 SET 命令将 character_set_connection 和 collation_connection 系统变量的值分别设置为 gbk 和 gbk_chinese_ci，然后再比较 'a' 和 'A'：

mysql> SET character_set_connection $\equiv$ gbk; Query OK, 0 rows affected (0.00 sec)   
mysql> SET collation_connection $\equiv$ gbk_chinese_ci; Query OK, 0 rows affected (0.00 sec)   
mysql> SELECT 'a' = 'A'; + - - - - - - + | 'a' = 'A' | + - - - - - + | 1 | + - - - - - + 1 row in set (0.00 sec)

可以看到，在这种情况下这两个字符串是相等的。

现在通过 SET 命令修改 character_set_connection 和 collation_connection 的值，将它们分别设置为 gbk 和 gbk_bin，然后比较 'a' 和 'A'：

mysql> SET character_set_connection $\equiv$ gbk; Query OK, 0 rows affected (0.00 sec)  
mysql> SET collation_connection $\equiv$ gbk_bin; Query OK, 0 rows affected (0.00 sec)  
mysql> SELECT 'a' = 'A';  
+-----------------------------------+  
| 'a' = 'A' |

```txt
+  
1 row in set (0.00 sec) 
```

可以看到，在这种情况下这两个字符串就不相等了。

我们接下来考虑请求中的字符串和某个列进行比较的情况。比如我们有一个表tt：

```sql
CREATE TABLE tt (
cVARCHAR(100)
) ENGINE=INNODB CHARSET=utf8; 
```

很显然，列c采用的字符集和表级别字符集utf8一致。这里采用默认的比较规则utf8_general_ci。表tt中有一条记录：

```txt
mysql> SELECT * FROM tt;  
+-----------------------------------+  
| c |  
+-----------------------------------+  
| 我 |  
+-----------------------------------+  
1 row in set (0.00 sec) 
```

假设现在 character_set_connection 和 collation_connection 的值分别设置为 gbk 和 gbk_chinese(ci)。然后我们有下面这样一条查询语句：

```sql
SELECT \* FROM tt WHERE c = '我'; 
```

在执行这个语句前，面临一个很重要的问题：字符串'我'是使用gbk字符集进行编码的，比较规则是gbk_chinese_c；而列c是采用utf8字符集进行编码的，比较规则为utf8_general_c。这该怎么比较呢？设计MySQL的大叔规定，在这种情况下，列的字符集和排序规则的优先级更高。因此，这里需要将请求中的字符串'我'先从gbk字符集转换为utf8字符集，然后再使用列c的比较规则utf8_general_c进行比较。

# 服务器生成响应

还是以前面创建的表tt为例。列c是使用utf8字符集进行编码的，所以字符串'我'在列c中的存放格式就是0xE68891。当执行下面这个语句时：

```sql
SELECT \* FROM tt;
```

是不是直接将0xE68891读出后发送到客户端呢？这可不一定，这取决于SESSION级别的系统变量character_set_results的值。服务器会先将字符串'我'从utf8字符集编码的0xE68891转换为character_set_results系统变量对应的字符集编码后的字节序列，之后再发送给客户端。

如果有特殊需要，也可以使用SET命令来修改character_set_results的值。比如我们执行下述语句：

```txt
SET character_set_results = gbk; 
```

那么，如果再次执行 SELECT * FROM tt 语句，在服务器返回给客户端的响应中，字符串 '我' 对应的就是字节序列 0xCED2。

现在已经唠叨完了 character_set_client、character_set_connection 和 character_set_results 这 3 个系统变量，需要总结一下了。这 3 个系统变量的作用如表 3-5 所示。

表 3-5 character_set_client、character_set_connection 和 character_set_results 系统变量的作用  

<table><tr><td>系统变量</td><td>描述</td></tr><tr><td>character_set_client</td><td>服务器认为请求是按照该系统变量指定的字符集进行编码的</td></tr><tr><td>character_set_connection</td><td>服务器在处理请求时，会把请求字节序列从 character_set_client 转换为 character_set_connection</td></tr><tr><td>character_set_results</td><td>服务器采用该系统变量指定的字符集对返回给客户端的字符串进行编码</td></tr></table>

这3个系统变量在服务器中的作用范围都是SESSION级别。每个客户端在与服务器建立连接后，服务器都会为这个连接维护这3个变量，如图3-3所示（假设连接1的这3个变量均为utf8，连接2的这3个变量均为gbk，连接3的这3个变量均为latin1）。

![](images/73400b0721a16a2c34bf2f1320ef9c2c838a671e82b828e300172af825b840c6.jpg)  
图3-3 客户端与服务器建立连接后，服务器维护的变量

每个MySQL客户端都维护着一个客户端默认字符集，客户端在启动时会自动检测所在操作系统当前使用的字符集，并按照一定的规则映射成MySQL支持的字符集，然后将该字符集作为客户端默认的字符集。通常的情况是，操作系统当前使用什么字符集，就映射为什么字符集。但是总存在一些特殊情况。假如操作系统当前使用的是ascii字符集，则会被映射为MySQL支持的latin1字符集。如果MySQL不支持操作系统当前使用的字符集，则会将客户端默认的字符集设置为MySQL的默认字符集。

![](images/ed40b66385ff97ab4b1865dc65df8e43d0b4cfa52d59ee0ac7b71a2ff420bd2b.jpg)

在MySQL5.7以及之前的版本中，MySQL的默认字符集为latin1。自MySQL8.0版本开始，MySQL的默认字符集改为utf8mb4。

另外，如果在启动MySQL客户端时设置了default-character-set启动选项，那么服务器会忽视操作系统当前使用的字符集，直接将default-character-set启动选项中指定的值作为客户端

的默认字符集。

在连接服务器时，客户端将默认的字符集信息与用户名、密码等信息一起发送给服务器，服务器在收到后会将 character_set_client、character_set_connection 和 character_set_results 这 3 个系统变量的值初始化为客户端的默认字符集。

在客户端成功连接到服务器后，可以使用 SET 语句分别修改 character_set_client、character_set_connection 和 character_set_results 系统变量的值，也可以使用下面的语句一次性修改这几个系统变量的值：

```sql
SET NAMES charset_name; 
```

上面这条语句与下面这3条语句的效果一样：

```sql
SET character_set_client = charset_name;  
SET character_set_results = charset_name;  
SET character_set_connection = charset_name; 
```

不过需要特别注意的是，SET NAMES语句并不会改变客户端在编码请求字符串时使用的字符集，也不会修改客户端的默认字符集。

# 客户端接收到响应

客户端收到的响应其实也是一个字节序列。对于类UNIX操作系统来说，收到的字节序列基本上相当于直接写到黑框框中（请注意这里的用词是“基本上相当于”，其实内部还会做一些工作，这里就不关注具体细节了），再由黑框框将这个字节序列解释为人类能看懂的字符（如果没有特殊设置的话，一般用操作系统当前使用的字符集来解释这个字节序列）。对于Windows操作系统来说，客户端会使用客户端的默认字符集来解释这个字节序列。

![](images/b9d47427e5634cd45bb966f7d45e9237e5b75ce42f2d5be329059d3d34ccf57b.jpg)

# 小贴士

对于类 UNIX 操作系统来说，在向黑框框中写入数据时，调用的是系统函数 fputs、putc 或者 fwrite。对于 Windows 操作系统来说，调用的是系统函数 WriteConsoleW。对源码感兴趣的读者可以进一步研究。

我们通过一个例子来理解这个过程。比如操作系统当前使用的字符集为 UTF-8，我们在启动 MySQL 客户端时使用了 --default-character-set=gbk 启动选项，那么客户端的默认字符集会被设置为 gbk，服务器的 character_set_results 系统变量的值也会被设置为 gbk。现在假设服务器的响应中包含字符 '我'，发送到客户端的字节序列就是 '我' 的 gbk 编码 0xCED2，针对不同的操作系统，会发生如下行为。

- 对于类 UNIX 操作系统来说，会把接收到的字节序列（也就是 0xCED2）直接写到黑框框中，并默认使用操作系统当前使用的字符集（UTF-8）来解释这个字符。很显然无法解释，所以我们在屏幕上看到的就是乱码。  
- 对于类 Windows 操作系统来说，会使用客户端的默认字符集（gbk）来解释这个字符，很显然会成功地解释成字符 '我'。

上面唠叨了这么多东西，主要是想让大家明白5件事情：

- 客户端发送的请求字节序列是采用哪种字符集进行编码的；  
- 服务器接收到请求字节序列后会认为它是采用哪种字符集进行编码的；

- 服务器在运行过程中会把请求的字节序列转换为以哪种字符集编码的字节序列；  
- 服务器在向客户端返回字节序列时，是采用哪种字符集进行编码的；  
- 客户端在收到响应字节序列后，是怎么把它们写到黑框框框中的。

# 3.3.3 比较规则的应用

结束了字符集的“漫游”，我们把视角再次聚焦到比较规则。比较规则通常用来比较字符串的大小以及对某些字符串进行排序，所以有时候也称为排序规则。比如表t的列col使用的字符集是gbk，使用的比较规则是gbk_chinese_ci，我们向里面插入几条记录：

```txt
mysql> INSERT INTO t(col) VALUES('a'), ('b'), ('A'), ('B');  
Query OK, 4 rows affected (0.00 sec)  
Records: 4 Duplicates: 0Warnings: 0 
```

我们在查询的时候按照col列排序一下：

```txt
mysql> SELECT * FROM t ORDER BY col;  
+-----------------------------------+  
| col |  
+-----------------------------------+  
| a |  
| A |  
| b |  
| B |  
| 我 |  
+-----------------------------------+  
5 rows in set (0.00 sec) 
```

可以看到在默认的比较规则gbk_chinese_ci中是不区分大小写的。我们现在把列col的比较规则修改为gbk_bin：

```txt
mysql> ALTER TABLE t MODIFY colVARCHAR(10) COLLATE gbk_bin;  
Query OK, 5 rows affected (0.02 sec)  
Records: 5 Duplicates: 0Warnings: 0 
```

由于gbk_bin是直接比较字符的二进制编码，所以是区分大小写的。我们看一下排序后的查询结果：

```txt
mysql> SELECT \* FROM t ORDER BY col;  
+-----------------------------------+  
| col |  
+-----------------------------------+  
| A |  
| B |  
| a |  
| b |  
| 我 |  
+-----------------------------------+  
5 rows in set (0.00 sec) 
```

大家在对字符串进行比较，或者对某个字符串列执行排序操作时，如果没有得到想象中的结果，需要思考一下是不是比较规则的问题。

![](images/7d000fdafb8b64100ead5e6d9109fdb611d71d15b80fad4ca477fa28a10ba514.jpg)

小贴士

列col中各个字符在使用gbk字符集编码后对应的数字如下：

$\bullet \mathrm{A}^{\prime} > 65$ （十进制）；  
$\bullet \mathrm{B}^{\prime}\rightarrow 66$ （十进制）；  
$\cdot \mathrm{a}^{\prime}\rightarrow 97$ （十进制）；  
- ${b}^{\prime } > {98}$ (十进制);   
$\bullet$ 我 $\rightarrow 52946$ （十进制）。

# 3.4 总结

字符集指的是某个字符范围的编码规则。

比较规则是对某个字符集中的字符比较大小的一种规则。

在MySQL中，一个字符集可以有若干种比较规则，其中有一个默认的比较规则。一个比较规则必须对应一个字符集。

在MySQL中查看支持的字符集和比较规则的语句如下：

- SHOW (CHARACTER SET|CHARSET) [LIKE 匹配的模式];   
- SHOW COLLATION [LIKE 匹配的模式];

MySQL有4个级别的字符集和比较规则，具体如下。

$\bullet$ 服务器级别

character_set_server 表示服务器级别的字符集，collation_server 表示服务器级别的比较规则。

$\bullet$ 数据库级别

创建和修改数据库时可以指定字符集和比较规则：

CREATE DATABASE 数据库名

[[DEFAULT]CHARACTERSET字符集名称]

[[DEFAULT] COLLATE 比较规则名称];

ALTER DATABASE 数据库名

[[DEFAULT] CHARACTER SET 字符集名称]

[[DEFAULT] COLLATE 比较规则名称];

character_set_database 表示当前数据库的字符集，collation_database 表示当前数据库的比较规则。这两个系统变量只用来读取，修改它们并不会改变当前数据库的字符集和比较规则。如果没有指定当前数据库，则这两个系统变量与服务器级别相应的系统变量具有相同的值。

表级别

创建和修改表的时候指定表的字符集和比较规则：

CREATE TABLE 表名（列的信息）

[[DEFAULT]CHARACTER SET字符集名称]

[COLLATE比较规则名称]；

ALTER TABLE 表名

```json
[[DEFAULT]CHARACTER SET字符集名称][COLLATE比较规则名称]；
```

$\bullet$ 列级别

创建和修改列的时候指定该列的字符集和比较规则：

```txt
CREATE TABLE 表名（列名字符串类型 [CHARACTER SET 字符集名称] [COLLATE 比较规则名称]，其他列...）；
```

```sql
ALTER TABLE 表名 MODIFY 列名 字符串类型 [CHARACTER SET 字符集名称] [COLLATE 比较规则名称];
```

从发送请求到接收响应的过程中发生的字符集转换如下所示。

- 客户端发送的请求字节序列是采用哪种字符集进行编码的。

这一步骤主要取决于操作系统当前使用的字符集；对于Windows操作系统来说，还与客户端启动时设置的default-character-set启动选项有关。

- 服务器接收到请求字节序列后会认为它是采用哪种字符集进行编码的。

这一步骤取决于系统变量 character_set_client 的值。

- 服务器在运行过程中会把请求的字节序列转换为以哪种字符集编码的字节序列。

这一步骤取决于系统变量 character_set_connection 的值。

- 服务器在向客户端返回字节序列时，是采用哪种字符集进行编码的。

这一步骤取决于系统变量 character_set_results 的值。

- 客户端在收到响应字节序列后，是怎么把它们写到黑框框中的。

这一步骤主要取决于操作系统当前使用的字符集；对于Windows操作系统来说，还与客户端启动时设置的default-character-set启动选项有关。

在这个过程中，各个系统变量的含义如表3-6所示。

表 3-6 系统变量及其含义  

<table><tr><td>系统变量</td><td>描述</td></tr><tr><td>character_set_client</td><td>服务器认为请求是按照该系统变量指定的字符集进行编码的</td></tr><tr><td>character_set_connection</td><td>服务器在处理请求时，会把请求字节序列从 character_set_client 转换为 character_set_connection</td></tr><tr><td>character_set_results</td><td>服务器采用该系统变量指定的字符集对返回给客户端的字符串进行编码</td></tr></table>

比较规则通常用来比较字符串的大小以及对某些字符串进行排列。

# 第4章 从一条记录说起——InnoDB记录存储结构

# 4.1 准备工作

到现在为止，MySQL对于我们来说还是一个“黑盒”，我们只负责使用客户端发送请求并等待服务器返回结果。表中的数据到底存到了哪里？以什么格式存放的？MySQL以什么方式来访问这些数据？这些问题的答案我们统统不知道。

我们前面在唠叨请求处理过程的时候提到，MySQL服务器中负责对表中的数据进行读取和写入工作的部分是存储引擎，而服务器又支持不同类型的存储引擎，比如InnoDB、MyISAM、MEMORY啥的。不同的存储引擎一般是由不同的人为实现不同的特性而开发的，真实数据在不同存储引擎中的存放格式一般是不同的，甚至有的存储引擎（比如MEMORY）都不用磁盘来存储数据。也就是对于使用MEMORY存储引擎的表来说，关闭服务器后表中的数据就消失了。由于InnoDB是MySQL默认的存储引擎，也是我们最常用到的存储引擎，另外我们也没有那么多时间去把各个存储引擎的内部实现都看一遍，所以本章要唠叨的是使用InnoDB作为存储引擎的记录存储结构。在了解了一个存储引擎的记录存储结构之后，其他的存储引擎都是“依葫芦画瓢”，就不多唠叨了。

# 4.2 InnoDB页简介

InnoDB 是一个将表中的数据存储到磁盘上的存储引擎，即使我们关闭并重启服务器，数据还是存在。而真正处理数据的过程发生在内存中，所以需要把磁盘中的数据加载到内存中。如果是处理写入或修改请求，还需要把内存中的内容刷新到磁盘上。而我们知道读写磁盘的速度非常慢，与读写内存差了几个数量级。当我们想从表中获取某些记录时，InnoDB 存储引擎需要一条一条地把记录从磁盘上读出来么？不，那样会慢死，InnoDB 采取的方式是，将数据划分为若干个页，以页作为磁盘和内存之间交互的基本单位。InnoDB 中页的大小一般为 16KB。也就是在一般情况下，一次最少从磁盘中读取 16KB 的内容到内存中，一次最少把内存中的 16KB 内容刷新到磁盘中。

![](images/169c670bf24e298b031b6d1004084e2b4742154e542e9698cb93eaaf34fea218.jpg)

系统变量innodb_page_size表明了InnoDB存储引擎中的页大小，默认值为16384（单位是字节），也就是16KB。该变量只能在第一次初始化MySQL数据目录时指定，之后就再也不能更改了（通过命令mysqld--initialize来初始化数据目录。我们之前没有过多地唠叨初始化数据目录的过程，大家只要知道在服务器运行过程中不可以更改页面大小就好了）。

# 4.3 InnoDB 行格式

我们平时都是以记录为单位向表中插入数据的，这些记录在磁盘上的存放形式也被称为行格式或者记录格式。设计InnoDB存储引擎的大叔到现在为止设计了4种不同类型的行格式，分别是COMPACT、REDUNDANT、DYNAMIC和COMPRESSED。随着时间的推移，他们可能会设计出更多的行格式，但是不管怎么变，这些行格式在原理上大体都是相同的。

# 4.3.1 指定行格式的语法

我们可以在创建或修改表的语句中指定记录所使用的行格式：

CREATE TABLE 表名（列的信息）ROW_FORMAT=行格式名称；

ALTER TABLE 表名 ROW_FORMAT=行格式名称；

比如在xiaohaizi数据库中创建一个演示用的表record_format_demo，可以这样指定它的行格式：

mysql>USExiaohaizi;   
Database changed   
mysql>CREATE TABLE record_format_demo( -- c1VARCHAR(10), -- c2VARCHAR(10)NOT NULL, -- c3CHAR(10), --c4VARCHAR(10) --)CHARSET $\equiv$ asciROW_FORMAT=COMPACT; Query OK, 0 rows affected (0.03 sec)

可以看到，我们刚刚创建的这个表的行格式就是 COMPACT。另外，我们还显式指定了这个表的字符集为assic。因为assic 字符集只包括空格、标点符号、数字、大小写字母和一些不可见字符，所以汉字是不能存到这个表里的。向这个表中插入两条记录：

```txt
mysql> INSERT INTO record_format_demo(c1, c2, c3, c4) VALUES ('aaaa', 'bbb', 'cc', 'd'), ('eeee', 'fff', NULL, NULL); Query OK, 2 rows affected (0.02 sec) Records: 2 Duplicates: 0Warnings: 0 
```

现在，表中的记录就是这个样子的：

```sql
mysql> SELECT * FROM record_format_demo;  
+-----------------------------------+-----------------------------------+-----------------------------------+  
| c1 | c2 | c3 | c4 |  
+-----------------------------------+-----------------------------------+-----------------------------------+  
| aaaa |bbb | cc | d |  
| eeee | fff | NULL | NULL |  
+-----------------------------------+-----------------------------------+-----------------------------------+  
2 rows in set (0.00 sec) 
```

演示表的内容也填充好了，现在来看看各个行格式下的存储结构到底有啥不同。

# 4.3.2 COMPACT行格式

话不多说，直接看图4-1。

![](images/5785b5a532fbaed40599911ebdf751e5c33d3d82b809477dbfc2dcee3a1a9069.jpg)  
图4-1 COMPACT行格式示意图

从图4-1中可以看出，一条完整的记录其实可以被分为记录的额外信息和记录的真实数据两大部分。下面我们分别看一下这两大部分的组成。

# 1. 记录的额外信息

这部分信息是服务器为了更好地管理记录而不得不额外添加的一些信息。这些额外信息分为3个部分，分别是变长字段长度列表、NULL值列表和记录头信息。

# （1）变长字段长度列表

我们知道，MySQL支持一些变长的数据类型，比如VARCHAR(M)、VARBINARY(M)、各种TEXT类型、各种BLOB类型。我们也可以把拥有这些数据类型的列称为变长字段。变长字段中存储多少字节的数据是不固定的，所以我们在存储真实数据的时候需要顺便把这些数据占用的字节数也存起来，这样才不至于把MySQL服务器搞懵。也就是说这些变长字段占用的存储空间分为两部分：

$\bullet$ 真正的数据内容；  
- 该数据占用的字节数。

在 COMPACT 行格式中，所有变长字段的真实数据占用的字节数都存放在记录的开头位置，从而形成一个变长字段长度列表，各变长字段的真实数据占用的字节数按照列的顺序逆序存放。再次强调一遍，是逆序存放！

![](images/63c28bb44432e83ab128c87c858043e6eb09b7fd1ee3bdeae40b929da37930a6.jpg)  
小贴士

关于为啥逆序存放会在下一章介绍，这里少安毋躁。

我们拿 record_format_demo 表中的第一条记录来举个例子。因为 record_format_demo 表的 c1、c2、c4 列都是VARCHAR(10) 类型的，也就是变长的数据类型，所以这 3 个列的值占用的存储空间字节数都需要保存在记录开头处。record_format_demo 表中的各个列使用的都是ascii 字符集，每个字符只需要一个字节来编码。来看一下第一条记录各变长字段内容的长度（见表 4-1）。

表 4-1 第一条记录中各变长字段内容的长度  

<table><tr><td>列名</td><td>存储内容</td><td>内容长度（十进制表示）</td><td>内容长度（十六进制表示）</td></tr><tr><td>c1</td><td>&#x27;aaaa&#x27;</td><td>4</td><td>0x04</td></tr><tr><td>c2</td><td>&#x27;bbb&#x27;</td><td>3</td><td>0x03</td></tr><tr><td>c4</td><td>&#x27;d&#x27;</td><td>1</td><td>0x01</td></tr></table>

因为这些长度值需要按照列的顺序逆序存放，所以最后变长字段长度列表的字节串用十六

进制表示的效果就是：

01 03 04

需要说明的是，上述各个字节之间实际上没有空格，这里使用空格只是为了方便理解。把这个字节串组成的变长字段长度列表填入图4-1中的效果如图4-2所示。

![](images/56c1abe7f20674b62c18a01b15e904cb646454920d3ef25ed5624b70ef0bbdb5.jpg)  
图4-2 第一条记录的存储格式

由于第一条记录中c1、c2、c4列中的字符串都比较短，也就是说占用的字节数比较小（c1列内容是'aaaa'，占用4字节；c2列内容是'bbb'，占用3字节；c4列内容是'd'，占用1字节），每个变长字段的内容占用的字节数用1字节就可以表示（也就是4、3、1这3个数字可以分别用字节0x04、0x03、0x01表示）。但是，如果变长字段的内容占用的字节数比较多，可能就需要用2字节来表示。至于用1字节还是2字节来表示变长字段的真实数据占用的字节数，InnoDB有它的一套规则。为了更好地表述清楚这个规则，我们引入W、M和L这几个符号，先分别看看这些符号的意思。

- 假设某个字符集中最多需要 W 字节来表示一个字符（也就执行 SHOW CHARSET 语句后结果中的 Maxlen 列）。比如 utf8mb4 字符集中的 W 就是 4，utf8 字符集中的 W 就是 3，gbk 字符集中的 W 就是 2，ascii 字符集中的 W 就是 1。  
- 对于变长类型VARCHAR(M)来说，这种类型表示能存储最多M个字符（注意是字符不是字节），所以这种类型能表示的字符串最多占用的字节数就是 $\mathrm{M} \times \mathrm{W}$ 。  
- 假设该变长字段实际存储的字符串占用的字节数是 L。

确定使用1字节还是2字节来表示一个变长字段的真实数据占用的字节数的规则就是这样：

- 如果 $\mathrm{M} \times \mathrm{W} \leqslant 255$ ，那么使用1字节来表示真实数据占用的字节数。

![](images/eeeceb1884db3208216d4e0de75590bd02dc4e5b5649cf020acd2bf34db675ce.jpg)

InnoDB在读取记录的变长字段长度列表时先查看表结构，先查看表结构，先查看表结构（重要的事情说三遍）。如果某个变长字段允许存储的最大字节数不大于255，可以认为只使用1字节来表示真实数据占用的字节数。

- 如果 $\mathrm{M} \times \mathrm{W} > 255$ ，则分为下面两种情况：

如果 $\mathrm{L}\leqslant 127$ ，则用1字节来表示真实数据占用的字节数；  
如果 $\mathrm{L} > 127$ ，则用2字节来表示真实数据占用的字节数。

![](images/f1eddffb08ea0830e09a55fc8d65d227396dee3bdb9213da22b417e8e31cf8c0.jpg)

InnoDB在读取记录的变长字段长度列表时先查看表结构。如果某个变长字段允许存储的最大字节数大于255，该怎么区分它正在读的某个字节是一个单独的字段长度还是半个字段长度呢？设计InnoDB的大叔使用该字节的第一个二进制位作为标志位：如果该字节的第一个位为0，该字节就是一个单独的字段长度（在使用一个字节表示不大于127的数字时，第一个位都为0）；如果该字节的第一个位为1，该字节就是半个字段长度。这个规则特别像我们前面说过的GBK字符集的编码规则。

对于一条记录来说，如果某个字段占用的字节数特别多，InnoDB有可能把该字段的值的一部分数据存放到所谓的溢出页中（我们后面会详细唠叨）。那么该字段在记录的变长字段长度列表处只存储留在本页面中的长度，所以使用2字节就可以表示这个留在本页面中的字节长度。尽管也是使用2字节，但对于溢出字段来说，采用的方案并不是单纯地将首字节的第一个二进制位置为0，而是采用了一种特殊的表示方式。关于表示溢出字段占用字节数的特殊表示方式我们就不多唠叨了，这里就是提一下，大家也不用深究。

总结一下就是：如果该变长字段允许存储的最大字节数（ $\mathrm{M} \times \mathrm{W}$ ）超过255字节，并且真实数据占用的字节数（L）超过127字节，则使用2字节来表示真实数据占用的字节数，否则使用1字节。

另外需要注意的一点是，变长字段长度列表中只存储值为非NULL的列的内容长度，不存储值为NULL的列的内容长度。也就是说对于第二条记录，因为c4列的值为NULL，所以第二条记录的变长字段长度列表只需要存储c1和c2列的内容长度即可。其中c1列存储的值为'eeee'，占用的字节数为4；c2列存储的值为'fff'，占用的字节数为3。数字4可以用1字节（0x04）表示，3也可以用1字节（0x03）表示，这样第二条记录的整个变长字段长度列表共需2字节。填充完变长字段长度列表的两条记录的对比如图4-3所示。

![](images/83a0dffa6a9a476ce2c6bb171253714ef2ce7da6cf8d0d98d0f62f08d82e2fc7.jpg)  
图4-3 两条记录存储格式对比

![](images/e37935278d66e352cde13ce80aa19157d951b000770e889e7cb1ee41c119494a.jpg)

并不是所有记录都有这个变长字段长度列表部分，如果表中所有的列都不是变长的数据类型或者所有列的值都是NULL的话，就不需要有变长字段长度列表。

# （2）NULL值列表

我们知道，一条记录中的某些列可能存储 NULL 值，如果把这些 NULL 值都放到记录的真实数据中存储会很占地方，所以 COMPACT 行格式把一条记录中值为 NULL 的列统一管理起来，存储到 NULL 值列表中。它的处理过程如下所示。

1. 首先统计表中允许存储 NULL 的列有哪些。

主键列以及使用 NOT NULL 修饰的列都是不可以存储 NULL 值的，所以在统计的时候不会把这些列算进去。比如表 record_format_demo 的 3 个列 c1、c3、c4 都允许存储 NULL 值，而 c2 列使用 NOT NULL 进行了修饰，不允许存储 NULL 值。

2. 如果表中没有允许存储 NULL 的列，则 NULL 值列表也就不存在了，否则将每个允许存储 NULL 的列对应一个二进制位，二进制位按照列的顺序逆序排列。二进制位表示的意义如下：

- 二进制位的值为1时，代表该列的值为NULL；

- 二进制位的值为0时，代表该列的值不为NULL。

因为表 record_format_demo 有 3 个值允许为 NULL 的列，所以这 3 个列和二进制位的对应关系如图 4-4 所示。

再一次强调，二进制位按照列的顺序逆序排列，所以第一个列c1和最后一个二进制位对应。

3. MySQL规定NULL值列表必须用整数个字节的位表示，如果使用的二进制位个数不是整数个字节，则在字节的高位补0。

表 record_format_demo 只有 3 个值允许为 NULL 的列，对应 3 个二进制位，不足一个字节，所以在字节的高位补 0，效果如图 4-5 所示。

![](images/c4a9bbfc4bc68e752c703892e5661334ed69df47fc44b0dd8adc132dc7176755.jpg)  
图4-4列和二进制位的对应关系

![](images/2751a2a9bf31f1f30930efe50d77589e5bc67bc6f096b16e2382bba4f3c50213.jpg)  
图4-5 字节高位补0的效果

依此类推，如果一个表中有9个值允许为NULL的列，则这个记录的NULL值列表部分就需要2字节来表示了。

知道了规则之后，我们再返回头看看表 record_format_demo 中两条记录中的 NULL 值列表应该怎么储存。因为只有 c1、c3、c4 这 3 个列允许存储 NULL 值，所以记录的 NULL 值列表处只需要一个字节。

- 对于第一条记录来说，c1、c3、c4这3个列的值都不为NULL，所以它们对应的二进制位都是0，如图4-6所示。

所以第一条记录的NULL值列表用十六进制表示就是 $0\mathrm{x}00$

- 对于第二条记录来说，c1、c3、c4这3个列中c3和c4的值都为NULL，所以这3个列对应的二进制位的情况如图4-7所示。

所以第二条记录的 NULL 值列表用十六进制表示就是 0x06。

![](images/a4d16f869c801697f4a57cf6125843f2ab2df32554f13e39f5c173977e74ea53.jpg)  
图4-6第一条记录的c1、c3、c4三个列的值

![](images/f5be353e089d565a87863f9ae4216e123ac3755d115f06403354b6e4a83a7fbf.jpg)  
图4-7 第二条记录的c1、c3、c4三个列的值

这两条记录在填充了NULL值列表后的示意图如图4-8所示。

![](images/42e4d93c8a246e00ae800294d5b1718f502640460ba993b3c1246da683901a0b.jpg)  
图4-8 两条记录在填充了NULL值列表后的示意图

# （3）记录头信息

除了变长字段长度列表、NULL值列表之外，还有一个称之为记录头信息的部分。记录头信息由固定的5字节组成，用于描述记录的一些属性。5字节也就是40个二进制位，不同的位代表不同的意思，如图4-9所示。

![](images/a6db990cdf974f11f4b4d412d03762248f152d2cddad11e6733b7feb207805b6.jpg)  
图4-9 记录头信息示意图

这些二进制位代表的详细信息如表4-2所示。

表 4-2 记录头信息中各二进制位代表的详细信息  

<table><tr><td>名称</td><td>大小(位)</td><td>描述</td></tr><tr><td>预留位1</td><td>1</td><td>没有使用</td></tr><tr><td>预留位2</td><td>1</td><td>没有使用</td></tr><tr><td>deleted_flag</td><td>1</td><td>标记该记录是否被删除</td></tr><tr><td>min_rec_flag</td><td>1</td><td>B+树的每层非叶子节点中最小的目录项记录都会添加该标记</td></tr><tr><td>n-owned</td><td>4</td><td>一个页面中的记录会被分成若干个组,每个组中有一个记录是“带头大哥”,其余的记录都是“小弟”。“带头大哥”记录的n-owned值代表该组中所有的记录条数,“小弟”记录的n-owned值都为0</td></tr><tr><td>heap_no</td><td>13</td><td>表示当前记录在页面堆中的相对位置</td></tr><tr><td>record_type</td><td>3</td><td>表示当前记录的类型,0表示普通记录,1表示B+树非叶子节点的目录项记录,2表示Infimum记录,3表示Supremum记录</td></tr><tr><td>next_record</td><td>16</td><td>表示下一条记录的相对位置</td></tr></table>

另外，记录头信息的前4个位也被称为info bit。

大家千万不要被这么多属性和陌生的概念吓着，这里把这些位代表的意思都写出来只是为了内容的完整性。没必要把它们的意思都记住，记住也没啥用，现在只需要看一遍混个脸熟，等之后用到这些属性的时候再回过头来看就好了。

因为我们并不清楚这些属性的详细用法，所以这里就不分析各个属性值是怎么产生的了，之后我们会详细唠叨的，少安毋躁。现在直接看一下record_format_demo表中的两条记录的记录头信息分别是什么，如图4-10所示。

![](images/8ce78fac75e3c85af28596794b1a75a71b8560eb937ac6a9de32762f645447b1.jpg)

![](images/8e7dba3a6c2d3bc3ff241c2c7d1c4174796e8d2a875448f73f3b034fdfdcdbe6.jpg)  
图4-10 两条记录的记录头信息详情

![](images/008bd8c86c26f6ae322fc888a25873acbe95d39e7c1502b1656c50d5b0c2fc47.jpg)

再一次强调，大家如果看不懂记录头信息中各个位代表的概念也千万别纠结，我们后面会说的。

# 2. 记录的真实数据

对于record_formatdemo表来说，记录的真实数据除了c1、c2、c3、c4这几个我们自己定义的列的数据外，MySQL会为每个记录默认地添加一些列（也称为隐藏列），具体的列如表4-3所示。

表 4-3 MySQL 为每个记录默认添加的列  

<table><tr><td>列名</td><td>是否必需</td><td>占用空间</td><td>描述</td></tr><tr><td>row_id</td><td>否</td><td>6字节</td><td>行ID，唯一标识一条记录</td></tr><tr><td>trx_id</td><td>是</td><td>6字节</td><td>事务ID</td></tr><tr><td>roll_pointer</td><td>是</td><td>7字节</td><td>回滚指针</td></tr></table>

![](images/adc7bade7d777a002ba262fc861ca5173892a026b6ad82494c90d63d8ca75104.jpg)

实际上这几个列的真正名称是DB_ROW_ID、DB_TRX_ID、DB_ROLL_PTR，只不过我觉得它们不好看，才写成了小写形式，大家之后在阅读文档或者别的资料时意识到这个问题就好了。

这里需要提一下InnoDB表的主键生成策略：优先使用用户自定义的主键作为主键；如果用户没有定义主键，则选取一个不允许存储NULL值的UNIQUE键作为主键；如果表中连不允许存储NULL值的UNIQUE键都没有定义，则InnoDB会为表默认添加一个名为row_id的隐藏列作为主键。

所以从表4-3中可以看出，InnoDB存储引擎会为每条记录都添加trx_id和roll_pointer这两个列，但是row_id是可选的（在没有自定义主键以及不允许存储NULL值的UNIQUE键的

情况下才会添加该列)。这些隐藏列的值不用我们操心，InnoDB存储引擎会自动帮我们生成。

因为表 record_format_demo 并没有定义主键，所以 MySQL 服务器会为每条记录增加上述的 3 个列。现在看一下加上记录的真实数据的两条记录长什么样子，如图 4-11 所示。

![](images/5578449f04b3ea21fc62b1b6b721e6d413788a4800c63c06595dc4b8725da32f.jpg)  
图4-11 记录真实数据的两条记录

看图4-11时要注意以下几点。

- 表 record_format_demo 使用的是ascii 字符集，所以 0x61616161 就表示字符串 'aaaa'，0x626262 就表示字符串 'bbb'；依此类推。  
- 注意第一条记录中c3列是CHAR(10)类型的，它实际存储的字符串是'cc'，使用ascii字符集来编码这个字符串得到的结果是'0x6363'。虽然表示这个字符串只占用了2字节，但整个c3列仍然占用了10字节的空间，除真实数据以外的8字节统统都用空格字符填充，空格字符在ascii字符集中的编码就是 $0 \times 20$ 。  
- 注意第二条记录中c3和c4列的值都为NULL，它们被存储在了前面的NULL值列表处，在记录的真实数据处就不再冗余存储，从而节省了存储空间。

# 3. CHAR(M)列的存储格式

前面讲到，在COMPACT行格式下，变长字段长度列表只是用来存放一条记录中各个变长字段的值占用的字节长度的。record_formatdemo表的c1、c2、c4列的类型是VARCHAR(10)，也就是说c1、c2、c4都是变长字段；而c3列的类型是CHAR(10)，也就是说c3列不属于变长字段。所以会把一条记录的c1、c2、c4这3个列占用的字节长度逆序存到变长字段长度列表中，如图4-12所示。

但这只是建立在我们的record_format_demo表采用的是ascii字符集的情况下，这个字符集采用固定的一个字节来编码一个字符，是一个定长编码字符集。如果采用变长编码的字符集（也就是表示一个字符需要的字节数不确定，比如gbk表示一个字符要 $1\sim 2$ 字节、utf8表示一个字符要 $1\sim 3$ 字节等），虽然c3列的类型是CHAR(10)，但是设计COMPACT行格式的大叔规定，此时该列的值占用的字节数也会被存储到变长字段长度列表中。比如我们修改一下c3列的字符集：

```txt
mysql> ALTER TABLE record_format_demo MODIFY COLUMN c3 CHAR(10) CHARACTER SET utf8;  
Query OK, 2 rows affected (0.02 sec)  
Records: 2 Duplicates: 0Warnings: 0 
```

修改该列字符集后，记录的变长字段长度列表也发生了变化，如图4-13所示。

这就意味着，对于CHAR(M)类型的列来说，当列采用的是定长编码的字符集时，该列占用的字节数不会被加到变长字段长度列表；而如果采用变长编码的字符集时，该列占用的字节数就会被加到变长字段长度列表。

![](images/15908f1d733c81e6dbd9f91ce77c5b53977660786280ad5eed46b5b0e2b272f6.jpg)  
图4-12 变长字段长度列表  
图4-13 变长字段长度列表的变化

另外还有一点需要注意，设计 COMPACT 行格式的大叔还规定，采用变长编码字符集的 CHAR(M) 类型的列要求至少占用 M 个字节，而VARCHAR(M) 却没有这个要求。比如对于使用 utf8 字符集、类型为 CHAR(10) 的列来说，该列存储的数据占用的字节长度的范围就是 $10 \sim 30$ 字节。即使我们向该列中存储一个空字符串也会占用 10 字节，这主要是希望在将来更新该列时，在新值的字节长度大于旧值的字节长度但不大于 10 个字节时，可以在该记录处直接更新。而不是在存储空间中再重新分配一个新的记录空间，导致原有的记录空间成为所谓的碎片（大家应该在这里感受到设计 COMPACT 行格式的大叔既想节省存储空间，又不想因为更新 CHAR(M) 类型的列而产生碎片的纠结心情了吧）。

# 4.3.3 REDUNDANT 行格式

其实知道了COMPACT行格式之后，其他的行格式就是“依葫芦画瓢”了。我们现在要介绍的REDUNDANT行格式是MySQL5.0之前就在使用的一种行格式。也就是说它已经非常古老了，但是本着知识完整性还是要提一下，大家看一下就好。

REDUNDANT行格式的全貌如图4-14所示。

![](images/7ea885568da5310dee11ceedd89720fdd22171427d24bb9d27d5e6e3404ea168.jpg)  
图4-14 REDUNDANT行格式示意图

现在把表 record_format_demo 的行格式修改为 REDUNDANT：

```txt
mysql> ALTER TABLE record_format_demo ROW_FORMAT=REDUNDANT;  
Query OK, 0 rows affected (0.05 sec)  
Records: 0 Duplicates: 0Warnings: 0 
```

为了方便大家理解和节省篇幅，我们直接把表 record_format_demo 在 REDUNDANT 行格式下的两条记录的具体格式提供出来（见图 4-15），之后再着重分析两种行格式的不同即可。

下边我们从各个方面看一下REDUNDANT行格式与COMPACT行格式相比，有什么不同的地方。

# 1. 字段长度偏移列表

注意 COMPACT 行格式的开头是变长字段长度列表，而 REDUNDANT 行格式的开头是字

段长度偏移列表，它与变长字段长度列表相比有两处不同：

- 没有了“变长”两个字，意味着REDUNDANT行格式会把该条记录中所有列（包括隐藏列）的长度信息都按照逆序存储到字段长度偏移列表；  
- 多了个“偏移”两个字，这意味着计算列值长度的方式不像COMPACT行格式那么直观，它是采用两个相邻偏移量的差值来计算各个列值的长度。

第一条记录的存储格式：

![](images/40a95ff0c4985c57563cfa9b6ac002a77c7c6a5117733ee08a6ded2653ec30ca.jpg)  
图4-15 REDUNDANT行格式下两条记录的具体格式

比如第一条记录的字段长度偏移列表就是：

25 24 1A 17 13 0C 06

因为它是逆序排放的，所以按照列的顺序排列就是：

06 0C 13 17 1A 24 25

按照两个相邻偏移量的差值来计算各个列值的长度的意思就是：

- 第一列（row_id）的长度就是0x06个字节，也就是6字节；  
- 第二列（trx_id）的长度就是（0x0C - 0x06）个字节，也就是6字节；  
- 第三列（roll_pointer）的长度就是（0x13 - 0x0C）个字节，也就是7字节；  
- 第四列（c1）的长度就是（0x17 - 0x13）个字节，也就是4字节；  
- 第五列（c2）的长度就是（0x1A - 0x17）个字节，也就是3字节；  
- 第六列（c3）的长度就是（0x24 - 0x1A）个字节，也就是10字节；  
- 第七列（c4）的长度就是（0x25 - 0x24）个字节，也就是1字节。

# 2. 记录头信息

REDUNDANT行格式的记录头信息占用6字节，总计48个二进制位，这些二进制位代表的意思如表4-4所示。

表 4-4 REDUNDANT 行格式的记录头信息占用的 48 个二进制位及其描述  

<table><tr><td>名称</td><td>大小(位)</td><td>描述</td></tr><tr><td>预留位1</td><td>1</td><td>没有使用</td></tr><tr><td>预留位2</td><td>1</td><td>没有使用</td></tr><tr><td>deleted_flag</td><td>1</td><td>标记该记录是否被删除</td></tr><tr><td>min_rec_flag</td><td>1</td><td>B+树的每层非叶子节点中的最小的目录项记录都会添加该标记</td></tr><tr><td>n-owned</td><td>4</td><td>一个页面中的记录会被分成若干个组,每个组中有一个记录是“带头大哥”,其余的记录都是“小弟”。“带头大哥”记录的n-owned值代表该组中所有的记录条数,“小弟”记录的n-owned值都为0</td></tr><tr><td>heap_no</td><td>13</td><td>表示当前记录在页面堆中的相对位置</td></tr><tr><td>n_field</td><td>10</td><td>表示记录中列的数量</td></tr><tr><td>lbyte_offs_flag</td><td>1</td><td>标记字段长度偏移列表中每个列对应的偏移量是使用1字节还是2字节表示的</td></tr><tr><td>next_record</td><td>16</td><td>表示下一条记录的相对位置</td></tr></table>

第一条记录中的头信息是：

```txt
00 00 10 OF 00 BC 
```

根据这6字节可以计算出各个属性的值，如下：

```txt
预留位1：0x00  
预留位2：0x00  
deleted_flag: 0x00  
min_rec_flag: 0x00  
n-owned: 0x00  
heap_no: 0x02  
n_field: 0x07  
lbyte_offs_flag: 0x01  
next_record: 0xBC
```

与 COMPACT 行格式的记录头信息对比来看，有两处不同：

- REDUNDANT 行格式多了 n_field 和 1byte_offs_flag 这两个属性；  
- REDUNDANT 行格式没有 record_type 这个属性。

# 3. 记录头信息中的 1byte_offs_flag 的值是怎么选择的

从本质上来说，字段长度偏移列表存储的偏移量指的是每个列的值占用的空间在记录的真实数据处结束的位置。这句话有点儿拗口，我们还是拿record_format_demo第一条记录为例来分析一下。0x06代表第一个列在记录的真实数据第6字节处结束，0x0C代表第二个列在记录的真实数据第12字节处结束，0x13代表第三个列在记录的真实数据第19字节处结束……最后一个列对应的偏移量值为0x25，也就意味着最后一个列在记录的真实数据第37字节处结束，也就意味着整条记录的真实数据实际上占用37字节。

在字段长度偏移列表中，每个列对应的偏移量可以使用1字节或者2字节来存储，那到底什么时候使用1字节、什么时候用2字节呢？这是根据该条REDUNDANT行格式记录的真实数据占用的总大小来判断的。

- 当记录的真实数据占用的字节数不大于127（十六进制0x7F，二进制0111111）时，每个列对应的偏移量占用1字节。

- 当记录的真实数据占用的字节数大于127，但不大于32767（十六进制0x7FFF，二进制011111111111111）时，每个列对应的偏移量占用2字节。  
- 有没有记录的真实数据大于32767的情况呢？有，不过此时记录的一部分已经存放到了所谓的溢出页中（后面我们会详细讨论），在本页中只保留前768字节和20字节的溢出页面地址（当然这20字节中还记录了一些别的信息）。在这种情况下只使用2字节来存储每个列对应的偏移量就够了。

大家可以看出来，设计REDUNDANT行格式的大叔采用的策略还是比较简单粗暴的：直接使用整个记录的真实数据占用的字节长度来决定使用1字节还是2字节存储列对应的偏移量。只要整条记录的真实数据占用的存储空间长度大于127字节，即使某个列的值占用的存储空间不大于127字节，那对不起，也需要使用2字节来表示该列对应的偏移量。简单粗暴，就是这么简单粗暴（所以这种行格式有些过时了）。

![](images/e6a5b85658d4ca104bebf60c74ad2abda80628596fc72bd582d53553b647c525.jpg)

不知大家是否有疑问，既然一个字节表示的范围是 $0 \sim 255$ ，为啥在记录的真实数据占用的存储空间大于127字节时就采用2字节表示各个列的偏移量，而不是大于255字节时再采用2字节表示各个列的偏移量呢？少安毋躁，后面唠叨NULL值处理的时候会解决这个疑惑。

为了在解析记录时知道每个列的偏移量是使用1字节还是2字节表示的，设计REDUNDANT行格式的大叔特意在记录头信息中放置了一个称为lbyte_offs_flag的属性：

- 当它的值为 1 时，表明使用 1 字节存储偏移量。  
- 当它的值为 0 时，表明使用 2 字节存储偏移量。

# 4. REDUNDANT行格式中NULL值的处理

因为REDUNDANT行格式并没有NULL值列表，所以设计REDUNDANT行格式的大叔在字段长度偏移列表中对各列对应的偏移量处做了一些特殊处理——将列对应的偏移量值的第一个比特位作为是否为NULL的依据，该比特位也可以称之为NULL比特位。也就是说在解析一条记录的某个列时，首先看一下该列对应的偏移量的NULL比特位是否为1。如果为1，那么该列的值就是NULL，否则就不是NULL。

这也就解释了前文提到的“为什么只要记录的真实数据大于127（十六进制0x7F，二进制0111111）时，就采用2字节来表示一个列对应的偏移量”。原因就是第一个比特位是所谓的NULL比特位，用来标记该列的值是否为NULL。

但是还有一点需要注意，对于值为NULL的列来说，该列的类型是否为变长类型决定了该列在记录的真实数据处的存储方式。我们接下来分析record_format_demo表的第二条记录，它对应的字段长度偏移列表如下：

A4 A4 1A 17 13 0C 06

按照列的顺序排放就是：

06 C 13 17 1A A4 A4

我们分情况看一下。

- 如果存储 NULL 值的字段是定长类型的，比如是 CHAR(M) 数据类型的，则 NULL 值也将占用记录的真实数据部分，并把该字段对应的数据使用 0x00 字节填充。

在图4-15所示的第二条记录中，c3列的值是NULL，而c3列的类型是CHAR(10)，在记录的真实数据部分占用了10字节，所以我们看到在REDUNDANT行格式中使用 $0\mathrm{x}00000000000000000000$ 来表示NULL值。

另外，c3列对应的偏移量为0xA4，对应的二进制为10100100，可以看到最高位为1，意味着该列的值是NULL。将最高位去掉后的值变成了0100100，对应的十进制值为36，而c2列对应的偏移量为0x1A，也就是十进制的26。 $36 - 26 = 10$ ，也就是说最终c3列占用的存储空间为10字节。

- 如果存储 NULL 值的字段是变长数据类型的，则不在记录的真实数据部分占用任何存储空间。

比如 record_formatdemo 表的 c4 列是VARCHAR(10) 类型的，VARCHAR(10) 是一个变长数据类型。c4 列对应的偏移量为 0xA4，与 c3 列对应的偏移量相同。这也就意味着它的值也为 NULL，将 0xA4 的最高位去掉后对应的十进制值也是 36。 $36 - 36 = 0$ ，也就意味着 c4 列本身不占用记录的真实数据处的空间。

除了上面几点之外，REDUNDANT行格式和COMPACT行格式大致相同，但是能明显感觉到使用COMPACT行格式的记录占用的空间更少一点，所以显得更紧凑，这样就可以在一个页面中存放尽可能多的记录。

# 5. CHAR(M) 列的存储格式

我们知道，在使用COMPACT行格式时，CHAR(M)类型的列所使用的字符集不同（具体分为定长编码的字符集和变长编码的字符集），该列的真实数据的具体存储方案也不同。

而REDUNDANT行格式则十分干脆，不管该列使用的字符集是啥，只要使用CHAR(M)类型，该列的真实数据占用的存储空间大小就是该字符集表示一个字符最多需要的字节数和M的乘积。比如，使用utf8字符集的CHAR(10)类型的列，其真实数据占用的存储空间大小始终为30字节；使用gbk字符集的CHAR(10)类型的列，其真实数据占用的存储空间大小始终为20字节。这样的话，将来在对该列进行更新时，可以直接在原位置更新，而不需要为记录申请新的存储空间。当然，这样的坏处就是可能会浪费一些存储空间。

# 4.3.4 溢出列

# 1. 溢出列

我们以使用ascii字符集的off_page_demo表为例，向该表中插入一条记录：

```txt
mysql> CREATE TABLE off_page_demo (
-> cVARCHAR(65532)
->) CHAREN=ascii ROW_FORMAT=COMPACT;
Query OK, 0 rows affected (0.01 sec)
mysql> INSERT INTO off_page_demo (c) VALUES(REPEAT('a', 65532));
Query OK, 1 row affected (0.00 sec) 
```

其中 $\mathrm{REPEAT}(\mathbf{a}^{\prime},65532)$ 是一个函数调用，它表示生成一个把字符 'a' 重复 65532 次的字符串。

由于使用的是ascii字符集，所以这个字符串实际占用的字节数就是65532。前文说过，InnoDB中磁盘和内存交互的基本单位是页，也就是说InnoDB是以页为基本单位来管理存储空间的，我们的记录都会被分配到某个页中存储。而一个页的大小一般是16KB，也就是16384字节，而本例中一个列的实际数据就需要占用65532字节，很显然一个页也存不了一条记录，这不是贼尴尬么。

在 COMPACT 和 REDUNDANT 行格式中，对于占用存储空间非常多的列，在记录的真实数据处只会存储该列的一部分数据，而把剩余的数据分散存储在几个其他的页中，然后在记录的真实数据处用 20 字节存储指向这些页的地址（当然，这 20 字节还包括分散在其他页面中的数据所占用的字节数），从而可以找到剩余数据所在的页，如图 4-16 所示。

![](images/16db46576e36f928c6e2469d51bcf64e40f771edfe94b134688dfa0049c786a7.jpg)  
图4-16 当列占用的存储空间相当大时，COMPACT行格式的存储方式

从图4-16中可以看出，对于COMPACT和REDUNDANT行格式来说，如果某一列中的数据非常多，则在本记录的真实数据处只会存储该列前768字节的数据以及一个指向其他页的地址，然后把剩下的数据存放到其他页中。这些存储768字节之外的数据的页面也称为溢出页。图4-17是图4-16的简化示意图。

![](images/1e6143b79e2425e55a4a409e59460dcf9473fafac2c7961461d1a88ae659fd19.jpg)  
图4-17 图4-16的简化示意图

off_page_demo 表的这条记录的列 c 的数据需要使用溢出页来存储，那么我们就把这个列称为溢出列（其实设计 InnoDB 的大叔把该列称之为 off-page 列）。最后需要注意的是，不只是VARCHAR(M) 类型的列可能成为溢出列，像 TEXT、BLOB 这些类型的列在存储的数据相当多的时候也会成为溢出列。

# 2. 产生溢出页的临界点

一个列在存储了多少字节之后会变为溢出列呢？

MySQL中规定一个页中至少存放两行记录。至于为什么这么规定后面再说，现在看一下这个规定造成的影响。以上面的off_page_demo表为例，它只有一个列c。我们往这个表中插入两条记录，每条记录最少包含多少字节的数据才会需要溢出页呢？这得分析一下页中的空间都是如何利用的。

- 每个页除了存放我们的记录以外，也需要存储一些额外的信息。这些乱七八糟的额外信息加起来需要132字节的空间（现在只要知道这个数字就好了，下一章可以精确计算），其他的空间都可以被用来存储记录。  
- 每个记录需要的额外信息是27字节。这27字节包括下面这些内容：

2字节用于存储真实数据的长度；  
1字节用于存储列是否是NULL值；  
5字节大小的头信息；  
6字节的row_id列；  
6字节的trx_id列；  
7字节的roll_pointer列。

假设一个列的真实数据占用的字节数为 $n$ ，设计MySQL的大叔规定，如果该列不发生溢出现象，就需要满足下面这个不等式：

$$
1 3 2 + 2 \times (2 7 + n) <   1 6 3 8 4
$$

![](images/16337b96ddc736138c8978d7bdf229c683f0d3c7e5c32844d29a24ae064c8e22.jpg)

有读者可能会有疑问，为啥上面的这个不等式不是 $132 + 2 \times (27 + n) \leqslant 16384$ 呢？我只能说设计MySQL的大叔就是这么规定的。另外，存放正常记录的页面和溢出页是两种不同类型的页面（下一章会介绍页面类型）。对于溢出页来说，并没有规定一个页面中最少存放两条记录。

通过求解这个不等式，得出的解是 $n < 8099$ 。也就是说，如果一个列中存储的数据小于8099字节，那么该列就不会成为溢出列，否则就会成为溢出列。不过，这个8099字节的限制只是针对只有一个列的off_page_demo表来说的。如果表中有多个列，则上面的不等式和结论就需要改一改了，所以重点就是：我们不用关注这个临界点是什么，只要知道如果一条记录的某个列中存储的数据占用的字节数非常多时，该列就可能成为溢出列。

# 4.3.5 DYNAMIC行格式和COMPRESSED行格式

下面来看另外两个行格式：DYNAMIC 行格式和 COMPRESSED 行格式。我现在使用的 MySQL 版本是 5.7，它的默认行格式就是 DYNAMIC。这两个行格式与 COMPACT 行格式挺像，只不过在处理溢出列的数据时有点儿分歧：它们不会在记录的真实数据处存储该溢出列真实数据的前 768 字节，而是把该列的所有真实数据都存储到溢出页中，只在记录的真实数据处存储 20 字节大小的指向溢出页的地址（当然，这 20 字节还包括真实数据占用的字节数），如图 4-18 所示。

COMPRESSED 行格式不同于 DYNAMIC 行格式的一点是，COMPRESSED 行格式会采用

压缩算法对页面进行压缩，以节省空间。这里就不多唠叨了。

![](images/30698a4ced9d7a50cf0ced32eeff84dbabbee013980f7ff4c1d51a27ed42469e.jpg)  
图4-18DYNAMIC行格式和COMPRESSED行格式的溢出页

![](images/2a7c7264d7716bfc95bc2bd37128ef2698858d54770f9e46c6ec7a9d02473699.jpg)

REDUNDANT是一种比较原始的行格式，它是非紧凑的。而COMPACT、DYNAMIC以及COMPRESSED行格式是较新的行格式，它们是紧凑的（占用的存储空间更少）。

# 4.4 总结

页是InnoDB中磁盘和内存交互的基本单位，也是InnoDB管理存储空间的基本单位，默认大小为16KB。

指定和修改行格式的语法如下：

CREATE TABLE 表名（列的信息）ROW_FORMAT=行格式名称；  
ALTER TABLE 表名 ROW_FORMAT=行格式名称；

InnoDB目前定义了4种行格式。

- COMPACT 行格式，如图 4-19 所示。

![](images/d7512745dfbfa18c8bfb97f48b53db864ad0aaecf8f913ff6f907ec374abc1c7.jpg)  
图4-19 COMPACT行格式示意图

- REDUNDANT 行格式，如图 4-20 所示。

![](images/a739953b9c47f8284551d458a05116cf587aa10969e2c4b8b05e1d800d26a09d.jpg)  
图4-20 REDUNDANT行格式示意图

DYNAMIC和COMPRESSED行格式。

这两种行格式类似于 COMPACT 行格式，只不过在处理溢出列数据时有点儿分歧：它们不会在记录的真实数据处存储列真实数据的前 768 字节，而是把所有的数据都存储到所谓的溢出页中，只在记录的真实数据处存储指向这些溢出页的地址。另外，COMPRESSED 行格式会采用压缩算法对页面进行压缩。

# 第5章 盛放记录的大盒子——InnoDB数据页结构

# 5.1 不同类型的页简介

前面章节简单介绍了页的概念。页是InnoDB管理存储空间的基本单位，一个页的大小一般是16KB。InnoDB为了不同的目的而设计了多种不同类型的页，比如存放表空间头部信息的页、存放ChangeBuffer信息的页、存放NODE信息的页、存放undo日志信息的页；等等等等。当然，如果我说的这些名词你一个都没有听过，也没有关系。我们今天也不准备说这些类型的页，我们关心的是那些存放表中记录的那种类型的页，官方称这种存放记录的页为索引（INDEX）页。鉴于我们还没有介绍过索引是什么，而这些表中的记录就是我们日常所称的数据，所以目前还是将这种存放记录的页称为数据页吧。

![](images/08417a1440d57ada8ac1efcd9a4dd1d72001390e118afbc2f7d8e8b768c484eb.jpg)

小贴士

别的数据库图书可能不把存放记录的页面称之为数据页，这里是为了表述方便才这么说的，大家在阅读其他图书的时候需要注意一下。

# 5.2 数据页结构快览

数据页代表的这块16KB大小的存储空间可以划分为多个部分，不同部分有不同的功能，如图5-1所示。

![](images/50b3f785bba4ae8fd04893ee83dd6ff671f288991b8c693764402d2136636046.jpg)  
图5-1InnoDB数据页结构示意图

从图5-1可以看出，一个InnoDB数据页的存储空间大致被划分成7个部分，有的部分占

用的字节数是确定的，有的部分占用的字节数是不确定的。下面我们通过表5-1大致描述一下这7个部分都存储一些什么内容（快速地瞅一眼就行了，后面会详细唠叨的）。

表 5-1 InnoDB 数据页结构  

<table><tr><td>名称</td><td></td><td>中文名</td><td>占用空间大小</td><td>简单描述</td></tr><tr><td>File Header</td><td></td><td>文件头部</td><td>38字节</td><td>页的一些通用信息</td></tr><tr><td>Page Header</td><td></td><td>页面头部</td><td>56字节</td><td>数据页专有的一些信息</td></tr><tr><td>Infimum + Supremum</td><td colspan="2">页面中的最小记录和最大记录</td><td>26字节</td><td>两个虚拟的记录</td></tr><tr><td>User Records</td><td></td><td>用户记录</td><td>不确定</td><td>用户存储的记录内容</td></tr><tr><td>Free Space</td><td></td><td>空闲空间</td><td>不确定</td><td>页中尚未使用的空间</td></tr><tr><td>Page Directory</td><td></td><td>页目录</td><td>不确定</td><td>页中某些记录的相对位置</td></tr><tr><td>File Trailer</td><td></td><td>文件尾部</td><td>8字节</td><td>校验页是否完整</td></tr></table>

![](images/5732871d8d2228bb852d002448ddcc1286d351ecec4909fde9a8240a77f80665.jpg)  
小贴士

我们接下来并不打算按照页中各个部分的出现顺序来依次介绍它们，因为按照顺序介绍的话会把大量陌生的概念一股脑儿地呈现在大家面前，严重打击各位的信心与兴趣。现在还是希望大家的学习曲线能够平缓一点儿，由浅入深一点儿，希望大家能接受这种写作手法。

# 5.3 记录在页中的存储

在页的7个组成部分中，我们自己存储的记录会按照指定的行格式存储到UserRecords部分。但是在一开始生成页的时候，其实并没有UserRecords部分，每当插入一条记录时，都会从FreeSpace部分（也就是尚未使用的存储空间）申请一个记录大小的空间，并将这个空间划分到UserRecords部分。当FreeSpace部分的空间全部被UserRecords部分替代掉之后，也就意味着这个页使用完了，此时如果还有新的记录插入，就需要去申请新的页了。这个过程如图5-2所示。

![](images/06ad5b608fb48dd0d121231fc2bc69457a9365f9a7faef6ebd54659e3ee00b94.jpg)  
图5-2 记录在页中的存储

为了更好地管理UserRecords中的这些记录，设计InnoDB的大叔可费了一番力气。他们将力气费在哪里了呢？不就是把记录按照指定的行格式一条一条摆在UserRecords部分么？其实这么说也没啥问题，但是“魔鬼藏在细节中”，我们还得从记录行格式的记录头信息说起。

# 5.3.1 记录头信息的秘密

为了故事的顺利发展，我们先创建一个表：

```erlang
mysql> CREATE TABLE page_demo(
-> c1 INT,
-> c2 INT,
-> c3VARCHAR(10000),
-> PRIMARY KEY (cl)
->) CHAREN=ascii ROW_FORMAT=COMPACT;
Query OK, 0 rows affected (0.03 sec) 
```

这个新创建的page_demo表有3列，其中c1和c2列用来存储整数，c3列用来存储字符串。需要注意的是，我们把c1列指定为主键，所以InnoDB就没必要创建那个所谓的row_id隐藏列了。而且我们为这个表指定了ascii字符集以及COMPACT的行格式，所以这个表中记录的行格式示意图如图5-3所示。

![](images/1bcbe4454b41360f9ea15db43bef3f07c8652b2d39d4efbc1fb59e6654e6d648.jpg)  
图5-3 COMPACT行格式示意图

从图5-3可以看到，我们特意把记录头信息的5字节的数据给标出来了，说明它很重要。我们再次浏览一下这些记录头信息中各个属性的大体意思（目前使用COMPACT行格式进行演示），如表5-2所示。

表 5-2 记录头信息的属性及描述  

<table><tr><td>名称</td><td>大小(比特)</td><td>描述</td></tr><tr><td>预留位1</td><td>1</td><td>没有使用</td></tr><tr><td>预留位2</td><td>1</td><td>没有使用</td></tr><tr><td>deleted_flag</td><td>1</td><td>标记该记录是否被删除</td></tr><tr><td>min_rec_flag</td><td>1</td><td>B+树中每层非叶子节点中的最小的目录项记录都会添加该标记</td></tr><tr><td>n-owned</td><td>4</td><td>一个页面中的记录会被分成若干个组,每个组中有一个记录是“带头大哥”,其余的记录都是“小弟”。“带头大哥”记录的n-owned值代表该组中所有的记录条数,“小弟”记录的n-owned值都为0</td></tr><tr><td>heap_no</td><td>13</td><td>表示当前记录在页面堆中的相对位置</td></tr><tr><td>record_type</td><td>3</td><td>表示当前记录的类型,0表示普通记录,1表示B+树非叶节点的目录项记录,2表示Infimum记录,3表示Supremum记录</td></tr><tr><td>next_record</td><td>16</td><td>表示下一条记录的相对位置</td></tr></table>

由于我们现在主要在唠叨记录头信息的作用，所以为了方便大家理解，我们只在 pagedemo 表的行格式演示图中（见图 5-4）画出有关的头信息属性以及 c1、c2、c3 列的信息（其他信息没画不代表它们不存在，只是为了理解上的方便而在图中省略了）。

![](images/bebd1543df24043bef76cb220eac6147076b240c7c8a14352ea5c94675cf1415.jpg)  
图5-4 page_demo 表的行格式简化图

下面试着向 page_demo 表中插入几条记录：

```txt
mysql> INSERT INTO page_demo VALUES(1, 100, 'aaaa'), (2, 200, 'bbb', (3, 300, 'cccc'), (4, 400, 'dddd'));  
Query OK, 4 rows affected (0.00 sec)  
Records: 4 Duplicates: 0Warnings: 0 
```

为了方便大家分析这些记录在页的UserRecords部分是怎么表示的，这里把记录中的头信息和实际的列数据都用十进制表示出来了（其实是一堆二进制位）。这些记录的示意图如图5-5所示。

在查看图5-5时需要注意一下，各条记录在UserRecords中存储的时候并没有空隙，这里只是为了方便大家观看才把每条记录单独画在一行中。我们现在对照着图5-5来看看记录头信息中的各个属性是什么意思。

![](images/5cae4e3b8d3b3964d30ff75bdae1a3a0d8dd2d11e268e95b27a84b715b4b93b3.jpg)  
图5-5 记录在页的UserRecords部分的存储结构

- deleted_flag：这个属性用来标记当前记录是否被删除，占用1比特。值为0时表示记

录没有被删除，值为1时表示记录被删除了。

啥？被删除的记录还在页中么？是的，摆在台面上的和背地里做的可能大相径庭。你以为记录被删除了，可它还在真实的磁盘上。这些被删除的记录之所以不从磁盘上移除，是因为在移除它们之后，还需要在磁盘上重新排列其他的记录，这会带来性能消耗，所以只打一个删除标记就可以避免这个问题。所有被删除掉的记录会组成一个垃圾链表，记录在这个链表中占用的空间称为可重用空间（关于链表是怎么形成的，在介绍过next_record属性后大家就知道了）。之后若有新记录插入到表中，它们就可能覆盖掉被删除的这些记录占用的存储空间。

![](images/2ddb8147de168929708864a458e8a98d090baa8e0e59a421e9283caa15425196.jpg)

小贴士

将deleted_flag属性设置为1和将被删除的记录加入到垃圾链表中其实是两个阶段，后面在介绍undo日志时会详细唠叨删除操作的详细执行过程，现在少安毋躁。

- min_rec_flag: B+ 树每层非叶子节点中的最小的目录项记录都会添加该标记。什么是 B+ 树？什么是非叶子节点？什么是目录项记录？好吧，等第 6 章介绍索引的时候再聊这些话题。我们现在只需要知道自己插入的 4 条记录的 min_rec_flag 值都是 0，意味着它们都不是 B+ 树非叶子节点中的最小的目录项记录。  
- n-owned：这个暂时保密，稍后它是主角。  
- heap_no：我们向表中插入的记录从本质上来说都是放到数据页的User Records部分，这些记录一条一条地亲密无间地排列着，如图5-6所示（这不是page_demo表中的记录，只是一个通用的示意图）。

![](images/7c037b852094991818e8640d8453c76e4ef45b2c080a7f63e56bc0aaba3261e1.jpg)  
图5-6 记录在数据页的UserRecords中的排放方式

设计 InnoDB 的大叔把记录一条一条亲密无间排列的结构称之为堆（heap）。为了方便管理这个堆，他们把一条记录（这条记录的 deleted_flag 可以为 1）在堆中的相对位置称之为 heap_no。在页面前边的记录 heap_no 相对较小，在页面后边的记录 heap_no 相对较大，每新申请一条记录的存储空间时，该条记录比物理位置在它前边的那条记录的 heap_no 值大 1。从图 5-5 所示的 page_demo 表的各条记录示意图中可以看出，我们插入的 4 条记录的 heap_no 属性值分别是 2、3、4、5。是不是少了点啥？是的，怎么不见 heap_no 值为 0 和 1 的记录呢？

这其实是设计InnoDB的大叔玩的一个小把戏，他们自动给每个页里面加了两条记录，由于这两条记录并不是用户自己插入的，所以有时候也称为伪记录或者虚拟记录。在这两条伪记录中，一条代表页面中的最小记录（也可以写作Infimum记录），另外一条代表页面中的最大记录（也可以写作Supremum记录）。这两条伪记录也算作堆的一部分（很显然这两条伪记录的heap_no值最小，说明它们在页面中的相对位置最靠前）。

等一下，记录可以比大小么？

是的，记录也可以比大小。对于一条完整的记录来说，比较记录的大小就是比较主键的大小。比如，我们插入的4条记录的主键值分别是1、2、3、4，这也就意味着这4条记录从小到大依次递增。

![](images/0134343e3ae3a6b1093a25076dd82936b29cb924dcd85e8347c83b969d142abc.jpg)  
小贴士

请注意，前文强调的是对于“一条完整的记录”来说，比较记录的大小相当于比的是主键的大小。下一章还会介绍只存储一条记录的部分列的情况，敬请期待。

但是，无论我们向页中插入了多少条记录，设计InnoDB的大叔都规定，任何用户记录都比Infimum记录大，任何用户记录都比Supremum记录小。

![](images/54ad09178b699c93385635d80519d347c655fd650f6130ca5758324c8f9318fd.jpg)  
小贴士

虽然 Infimum 记录和 Supremum 记录没有主键值，但是设计 InnoDB 的大叔规定：Infimum 记录是一个页面中最小的记录，Supremum 记录是一个页面中最大的记录。这是规定！规定！规定！

Infimum 和 Supremum 这两条记录的构造十分简单，都是由 5 字节大小的记录头信息和 8 字节大小的一个固定单词组成的，如图 5-7 所示。

![](images/ca0c19773e5d6fd40d6560469a1e47175d8d64da02190c0c14bb16163fc34091.jpg)  
图5-7 Infimum和Supremum记录的结构

由于 Infimum 和 Supremum 这两条记录是设计 InnoDB 的大叔默认创建的记录，为了与用户自己插入的记录进行区分，我们就不把它们存放在页的 User Records 部分，而是单独放在一个称为 Infimum + Supremum 的部分，如图 5-8 所示。

从图5-8可以看出，Infimum记录和Supremum记录的heap_no值分别是0和1，也就是说它们在堆中的相对位置最靠前。另外还需要注意的一点是，堆中记录的heap_no值在分配之后就不会发生改动了，即使之后删除了堆中的某条记录，这条被删除记录的heap_no值也仍然保持不变。

- record_type：这个属性表示当前记录的类型。一共有4种类型的记录，其中0表示普通记录，1表示B+树非叶节点的目录项记录，2表示Infimum记录，3表示Supremum记录。从图5-8也可以看出，我们自己插入的记录就是普通记录，它们的record_type值都是0，而Infimum记录和Supremum记录的record_type值分别为2和3。至于record_type为1的情况，我们之后在唠叨索引的时候会重点介绍的。

![](images/f8cf75c3287529e8fd7794b7cf25b70a64776fadfd07736dd4e707e1bde39f8c.jpg)  
图5-8 记录存放方式

- next_record：这个属性非常重要，它表示从当前记录的真实数据到下一条记录的真实数据的距离。如果该属性值为正数，说明当前记录的下一条记录在当前记录的后面；如果该属性值为负数，说明当前记录的下一条记录在当前记录的前面。比如，第1条记录的next_record值为32，意味着从第1条记录的真实数据的地址处向后找32字节便是下一条记录的真实数据。再比如，第4条记录的next_record值为-111，意味着从第4条记录的真实数据的地址处向前找111字节便是下一条记录的真实数据。如果大家熟悉数据结构的话，就会立即明白这其实就是个链表，可以通过一条记录找到它的下一条记录。但是需要注意的一点是，下一条记录指的并不是插入顺序中的下一条记录，而是按照主键值由小到大的顺序排列的下一条记录。而且规定Infimum记录的下一条记录就是本页中主键值最小的用户记录，本页中主键值最大的用户记录的下一条记录就是Supremum记录。为了更形象地表示这个next_record属性的作用，我们用箭头来替代next_record中的值（注意箭头指向的位置，每个箭头都指向记录的真实数据开始的地方），如图5-9所示。

![](images/d9ea2cbde85e83060ac9fa8c218ceabfdf001e9f571844c0f38661776cde175f.jpg)  
图5-9 使用箭头替代next_record的值

从图5-9可以看出，记录按照主键从小到大的顺序形成了一个单向链表。Supremum记录的next_record值为0，也就是说Supremum记录之后就没有下一条记录了，这也意味着Supremum记录就是这个单向链表中的最后一个节点。如果从表中删除一条记录，这个由记录组成的单向链表也是会跟着变化，比如我们把第2条记录删掉：

```txt
mysql> DELETE FROM page_demo WHERE c1 = 2;  
Query OK, 1 row affected (0.02 sec) 
```

删掉第2条记录后的示意图如图5-10所示。

![](images/1efe8004fb55cbc247d079089c7cd0ca6420f568bbbd651a36b7b2cdfdf02d1d.jpg)  
图5-10删掉第2条记录后的示意图

从图5-10可以看出，删除第2条记录后主要发生了下面这些变化：

- 第2条记录并没有从存储空间中移除，而是把该条记录的deleted_flag值设置为1；  
- 第2条记录的next_record值变为0，意味着该记录没有下一条记录了；  
- 第1条记录的next_record指向了第3条记录；  
- 还有一点大家可能忽略了，那就是Supremum记录的n-owned值从5变成了4。关于这一点变化稍后会详细介绍。

所以，无论怎么对页中的记录进行增删改操作，InnoDB始终会维护记录的一个单向链表，链表中的各个节点是按照主键值由小到大的顺序链接起来的。

![](images/f9798eadc20b6c275bcf28cb9234c36d7fa30f9b8b910d587a94cca0e616731d.jpg)

大家会不会觉得 next_record 这个指针有点儿奇怪，它为啥要指向记录头信息和真实数据之间的位置呢？为啥不干脆指向整条记录的开头位置，也就是记录的额外信息开头的位置呢？原因是这个位置刚刚好，向左读取就是记录头信息，向右读取就是真实数据。前文还说过，变长字段长度列表、NULL 值列表中的信息都是逆序存放的，这样可以使记录中位置靠前的字段和它们对应的字段长度信息在内存中的距离更近，这可能会提高高速缓存的命中率。

再来看一个有意思的事情：主键值为2的记录被删掉了，但是却没有回收存储空间（该记录的heap_no也未发生改变），如果我们再次把这条记录插入到表中，会发生什么呢？

```sql
mysql> INSERT INTO page_demo VALUES(2, 200, 'bbb'); Query OK, 1 row affected (0.00 sec) 
```

我们看一下记录的存储情况，如图5-11所示。

![](images/edea45bfb535659f70e3c129a57098e77cee8e8c9476e5f54e284e2094531821.jpg)  
图5-11再次将第2条记录插入后，记录的存储情况

从图5-11可以看到，InnoDB并没有因为新记录的插入而为它申请新的存储空间，而是直接复用了原来被删除记录的存储空间。

![](images/e2b4cb60e0aba2b506117756a2e6bda5840f7ad9f217c94934c09d4b3ae75582.jpg)

当数据页中存在多条被删除的记录时，可以使用这些记录的 next_record 属性将这些被删除的记录组成一个垃圾链表，以备之后重用这部分存储空间。关于垃圾链表的更多信息，第 20 章会进行超级详细的描述，现在先蜻蜓点水般地知道这些就好了，一口吃不成个胖子。

# 5.4 Page Directory（页目录）

现在，我们知道了记录在页中是按照主键值由小到大的顺序串联成一个单向链表，如果想根据主键值查找页中的某条记录，该咋办呢？比如说下面这样的查询语句：

```sql
SELECT \* FROM page_demo WHERE c1 = 3; 
```

最笨的办法是从 Infimum 记录开始，沿着单向链表一直往后找，总有一天会找到（或者找不到）。而且在找的时候还能投机取巧，因为链表中各个记录的值是按照从小到大的顺序排列的，所以当链表中某个节点代表的记录的主键值大于想要查找的主键值时，就可以停止查找了（因为该节点后边的节点的主键值依次递增）。

当页中存储的记录数量比较少时，这种方法用起来也没有啥问题。比如，我们的表中只插入了4条自己的记录，所以最多找4次就可以把所有记录都遍历一遍。但是，如果一个页中存储了非常多的记录，遍历操作对性能来说还是有损耗的，所以说这种遍历查找是一个笨办法。但是设计InnoDB的大叔是什么人，他们能用这么笨的办法么，当然是要设计一种更好的查找

方式了。他们从图书的目录中找到了灵感。

我们平时在一本书中查找某个内容的时候，一般会先看目录，找到该内容对应的图书页码，然后再到对应的页码去查看内容。设计InnoDB的大叔也为我们的记录制作了一个类似的目录。制作过程如下所示。

1. 将所有正常的记录（包括 Infimum 和 Supremum 记录，但不包括已经移除到垃圾链表的记录）划分为几个组。  
2. 每个组的最后一条记录（也就是组内最大的那条记录）相当于“带头大哥”，组内其余的记录相当于“小弟”。“带头大哥”记录的头信息中的n-owned属性表示该组内共有几条记录。  
3. 将每个组中最后一条记录在页面中的地址偏移量（就是该记录的真实数据与页面中第0个字节之间的距离）单独提取出来，按顺序存储到靠近页尾部的地方。这个地方就是Page Directory（页目录，见图5-1）。页目录中的这些地址偏移量称为槽（Slot），每个槽占用2字节。页目录就是由多个槽组成的。

![](images/5dfeb82b199199ffbab56747436e9d6d79f4f65edcfa3abada4cd36109f15891.jpg)

小贴士

一个正常的页面也就是16KB大小，即16384字节，而2字节可以表示的地址偏移量范围是 $0\sim 65535$ ，所以用2字节表示一个槽足够了。

比如，现在 page_demo 表中正常的记录共有 6 条。InnoDB 会把它们分成 2 个组，第一组只有一个 Infimum 记录，第二组是剩余的 5 条记录。2 个组就对应着 2 个槽，每个槽中存放每个组中最大的那条记录在页面中的地址偏移量，如图 5-12 所示。

![](images/73368888719470173fdd32677b822abf19a6d9683f65217531fad2361533c89e.jpg)  
图5-12 page_demo表中的记录排列方式

在图5-12中需要注意下面几点。

- 页目录部分中有2个槽，也就意味着记录被分成了2个组。槽1中的值是112，代表Supremum记录在页面中的地址偏移量（就是从页面的0字节开始数，数112字节）；

槽0中的值是99，代表Infimum记录的地址偏移量。

- 注意 Infimum 记录和 Supremum 记录的头信息中的 n-owned 属性。

- Infimum记录的n-owned值为1，这表示以Infimum记录为最后一个节点的这个分组中只有1条记录，也就是Infimum记录自身。  
Supremum记录的n-owned值为5，这表示以Supremum记录为最后一个节点的这个分组中有5条记录，即除了Supremum记录自身之外，还有我们插入的4条记录。

- 每个槽占用2字节，按照对应记录的大小相邻分布。槽对应的记录越小，它的位置越靠近FileTrailer。

99和112这样的地址偏移量很不直观，我们用箭头指向的方式替代数字，这样更容易理解。修改后的记录排列方式示意图如图5-13所示。

![](images/f347359b9360679ac3ca705bd56d240646acf06bceb79e471e08f1550c42e491.jpg)  
图5-13 用箭头替代槽中数字的示意图

怎么看上去怪怪的呢？这么乱的图对于我这个强迫症患者真是不能忍，我们暂时先不管各条记录在存储设备上的排列方式了，单纯从逻辑上看一下这些记录和页目录的关系，如图5-14所示。

这样一来就顺眼多了！不过划分分组的依据是什么呢，也就是说，为什么 Infimum 记录的 n-owned 值为 1，而 Supremum 记录的 n-owned 值为 5 呢？这里面有什么猫腻？

是的，设计InnoDB的大叔对每个分组中的记录条数是有规定的：对于Infimum记录所在的分组只能有1条记录，Supremum记录所在的分组拥有的记录条数只能在 $1\sim 8$ 条之间，剩下的分组中记录的条数范围只能是在 $4\sim 8$ 条之间。所以给记录进行分组是按照下面的步骤进行的。

1. 在初始情况下，一个数据页中只有 Infimum 记录和 Supremum 记录这两条，它们分属于两个分组。页目录中也只有两个槽，分别代表 Infimum 记录和 Supremum 记录在页面中的地址偏移量。  
2. 之后每插入一条记录，都会从页目录中找到对应记录的主键值比待插入记录的主键值大并且差值最小的槽（从本质上来说，槽是一个组内最大的那条记录在页面中的地址偏移量，通过槽可以快速找到对应的记录的主键值），然后把该槽对应的记录的n-owned值

加1，表示本组内又添加了一条记录，直到该组中的记录数等于8个。

![](images/f28ce6021376bc33b968f16375527d4f3a5f447afbc6310309958ebc53717e0e.jpg)  
图5-14 记录和页目录的关系

![](images/d43ce47c80dabe49b6b20e95ff5574e98543a1db0b08e493f93463c9c087b760.jpg)

# 小贴士

再次强调，对于 Infimum 记录和 Supremum 记录来说，它们虽然没有主键值，但是人为规定它们是一个页面中最小和最大的记录。

3. 当一个组中的记录数等于8后，再插入一条记录，会将组中的记录拆分成两个组，其中一个组中4条记录，另一个5条记录。这个拆分过程会在页目录中新增一个槽，记录这个新增分组中最大的那条记录的偏移量。

由于现在 page_demo 表中的记录太少，无法演示在添加页目录之后是如何加快查找速度的，所以我们再往 page_demo 表中添加一些记录：

```txt
mysql> INSERT INTO page_demo VALUES(5, 500, 'eeee'), (6, 600, 'ffff'), (7, 700, 'gggg'), (8, 800, 'hhhh'), (9, 900, 'iii'), (10, 1000, 'jjjj'), (11, 1100, 'kkkk'), (12, 1200, 'llll'), (13, 1300, 'mmm'), (14, 1400, 'nnnn'), (15, 1500, 'ooo'), (16, 1600, 'pppp'); Query OK, 12 rows affected (0.00 sec)  
Records: 12 Duplicates: 0Warnings: 0 
```

我们一口气往表中添加了12条记录，现在页中就一共有18条记录了（包括Infimum和Supremum记录）。这些记录被分成了5个组，如图5-15所示。

![](images/82525c7fe39712b8898fca6cd424ccf7fe074d5d2b5b09b459a94e8c4f349278.jpg)

# 小贴士

我们这里在插入记录时是按照主键值从小到大的顺序依次插入的。如果插入的记录的主键值没有顺序，你可以试试按照我们刚才唠叨过的给记录进行分组的步骤推演一下，页目录将会变成什么样子。

因为把16条记录的全部信息都画在一张图中太占地方，也容易让人眼花，所以这里也省略了各个记录之间的箭头（图里没画不代表没有！），只保留了用户记录头信息中的n-owned和next_record属性。现在看看怎么在这个页目录中查找记录。因为一个槽占用2字节，各个

槽之间是挨着的，而且它们代表的记录的主键值都是从小到大排序的，所以可以使用二分法快速查找。5个槽的编号分别是0、1、2、3、4，所以初始情况下最低的槽就是low=0，最高的槽就是high=4。比如，我们想找主键值为6的记录，过程是这样的。

![](images/3c1645bfb82bde8c67d83e2537a5b069bd7f1d93f2cefa4272b75ca248cc204e.jpg)  
图5-15 向page_demo表中添加记录

1. 计算中间槽的位置： $(0 + 4) / 2 = 2$ ，查看槽2对应记录的主键值为8；又因为 $8 > 6$ ，所以设置high=2，low保持不变。  
2. 重新计算中间槽的位置： $(0 + 2) / 2 = 1$ ，查看槽1对应记录的主键值为4；又因为 $4 < 6$ 所以设置 $\mathrm{low} = 1$ ，high保持不变。  
3. 因为 high-low 的值为 1，所以确定主键值为 6 的记录在槽 2 对应的组中。此时需要找到槽 2 所在分组中主键值最小的那条记录，然后沿着单向链表遍历槽 2 中的记录。但是前文又说过，每个槽对应的记录都是该组中主键值最大的记录，这里槽 2 对应的记录是主键值为 8 的记录，怎么定位一个组中最小的记录呢？别忘了各个槽都是挨着的，我们可以很轻易地找到槽 1 对应的记录（主键值为 4），这条记录的下一条记录就是槽 2 所在分组中主键值最小的记录，其主键值为 5。所以，我们可以从这条主键值为 5 的记录出发，遍历槽 2 中的各条记录，直到找到主键值为 6 的那条记录即可。由于一个组中包含的记录条数最多是 8 条，所以遍历一个组中的记录的代价是很小的。

综上所述，在一个数据页中查找指定主键值的记录时，过程分为两步。

1. 通过二分法确定该记录所在分组对应的槽，然后找到该槽所在分组中主键值最小的那条记录。  
2. 通过记录的 next_record 属性遍历该槽所在的组中的各个记录。

# 5.5 PageHeader（页面头部）

设计InnoDB的大叔为了能得到存储在数据页中的记录的状态信息，比如数据页中已经存储了多少条记录、FreeSpace在页面中的地址偏移量、页目录中存储了多少个槽等，特意在数据页中定义了一个名为PageHeader的部分，它是页结构的第2部分（见图5-1），占用固定的56字节，专门存储各种状态信息。PageHeader中各个字节的具体用途如表5-3所示。

表 5-3 Page Header 的结构及描述  

<table><tr><td>状态名称</td><td>占用空间大小</td><td>描述</td></tr><tr><td>PAGE_N_DIR_SLOTS</td><td>2字节</td><td>在页目录中的槽数量</td></tr><tr><td>PAGE_HEAP_TOP</td><td>2字节</td><td>还未使用的空间最小地址,也就是说从该地址之后就是Free Space</td></tr><tr><td>PAGE_N_HEAP</td><td>2字节</td><td>第1位表示本记录是否为紧凑型的记录,剩余的15位表示本页的堆中记录的数量(包括Infimum和Supremum记录以及标记为“已删除”的记录)</td></tr><tr><td>PAGE-Free</td><td>2字节</td><td>各个已删除的记录通过next_record 组成一个单向链表,这个单向链表中的记录所占用的存储空间可以被重新利用;PAGE-Free 表示该链表头节点对应记录在页面中的偏移量</td></tr><tr><td>PAGE_GARBAGE</td><td>2字节</td><td>已删除记录占用的字节数</td></tr><tr><td>PAGE LAST_insert</td><td>2字节</td><td>最后插入记录的位置</td></tr><tr><td>PAGE_DIRECTION</td><td>2字节</td><td>记录插入的方向</td></tr><tr><td>PAGE_N_DIRECTION</td><td>2字节</td><td>一个方向连续插入的记录数量</td></tr><tr><td>PAGE_N_RECS</td><td>2字节</td><td>该页中用户记录的数量(不包括Infimum 和Supremum记录以及被删除的记录)</td></tr><tr><td>PAGE_MAX_TRX_ID</td><td>8字节</td><td>修改当前页的最大事务id,该值仅在二级索引页面中定义</td></tr><tr><td>PAGE_LEVEL</td><td>2字节</td><td>当前页在B+树中所处的层级</td></tr><tr><td>PAGE_INDEX_ID</td><td>8字节</td><td>索引ID,表示当前页属于哪个索引</td></tr><tr><td>PAGE_BTR_SEG_LEAF</td><td>10字节</td><td>B+树叶子节点段的头部信息,仅在B+树的根页面中定义</td></tr><tr><td>PAGE_BTR_SEG_TOP</td><td>10字节</td><td>B+树非叶子节点段的头部信息,仅在B+树的根页面中定义</td></tr></table>

如果大家认真看过前文，一定会清楚从PAGE_N_DIR_SLOTS到PAGE LAST Insert以及PAGE_N_RECS的意思。如果不清楚，你应该回头再看一遍前文。剩下的状态信息看不明白也不要着急，饭要一口一口吃，东西要一点一点学（一定要沉住气，不要被这些名词吓到）。

在这里我们先唠叨PAGE_DIRECTION和PAGE_N_DIRECTION的意思。

- PAGE_DIRECTION：假如新插入的一条记录的主键值比上一条记录的主键值大，我们说这条记录的插入方向是右边，反之则是左边。用来表示最后一条记录插入方向的状态就是PAGE_DIRECTION。  
- PAGE_N_DIRECTION：假设连续几次插入新记录的方向都是一致的，InnoDB会把沿着同一个方向插入记录的条数记下来，这个条数就用PAGE_N_DIRECTION状态表示。当然，如果最后一条记录的插入方向发生了改变，这个状态的值会被清零后重新统计。

至于还没提到的那些状态，现在大家还不需要知道。不要着急，当我们学完了后面的内容后再回头来看，就会发现一切都是那么清晰。

# 5.6 FileHeader（文件头部）

前文唠叨的PageHeader专门针对的是数据页记录的各种状态信息，比如页里有多少条记录，有多少个槽。现在介绍的FileHeader通用于各种类型的页，也就是说各种类型的页都会以FileHeader作为第一个组成部分，它描述了一些通用于各种页的信息，比如这个页的编号是多少，它的上一个页和下一个页是谁；等等等等。FileHeader部分占用固定的38字节，由表5-4所示的内容组成。

表 5-4 File Header 的结构及描述  

<table><tr><td>状态名称</td><td>占用空间大小</td><td>描述</td></tr><tr><td>FIL_PAGE_SPACE_OR_CHKSUM</td><td>4字节</td><td>当MySQL的版本低于4.0.14时,该属性表示本页面所在的表空间ID;在之后的版本中,该属性表示页的校验和(checksum)</td></tr><tr><td>FIL_PAGE_OFFSET</td><td>4字节</td><td>页号</td></tr><tr><td>FIL_PAGE_PREV</td><td>4字节</td><td>上一个页的页号</td></tr><tr><td>FIL_PAGE_NEXT</td><td>4字节</td><td>下一个页的页号</td></tr><tr><td>FIL_PAGE_LSN</td><td>8字节</td><td>页面被最后修改时对应的LSN(Log Sequence Number,日志序列号)值</td></tr><tr><td>FIL_PAGE_TYPE</td><td>2字节</td><td>该页的类型</td></tr><tr><td>FIL_PAGE_FILE Flush_LSN</td><td>8字节</td><td>仅在系统表空间的第一个页中定义,代表文件至少被刷新到了对应的LSN值</td></tr><tr><td>FIL_PAGE_ARCHIVE_LOG_NO_OR_SPACE_ID</td><td>4字节</td><td>页属于哪个表空间</td></tr></table>

对照着表5-4，我们看几个目前比较重要的部分。

- FIL_PAGE_SPACE_OR_CHKSUM: MySQL 4.0.14 以下的版本应该没人用了吧，我们直接讨论高于 4.0.14 版本的情况。这个属性代表当前页面的校验和（checksum）。啥是校验和？就是对于一个很长的字节串来说，我们会通过某种算法计算出一个比较短的

值来代表这个很长的字节串，这个比较短的值就称为校验和。这样在比较两个很长的字节串之前，先比较这两个长字节串的校验和。如果校验和都不一样，则两个长字节串肯定是不同的，这样就省去了直接比较两个长字节串的时间损耗。

- FIL_PAGE_OFFSET：每一个页都有一个单独的页号，如同我们的身份证号码一样。InnoDB通过页号来唯一定位一个页。  
- FIL_PAGE_TYPE：表示当前页的类型。前文说过，InnoDB为了不同的目的而把页分为不同的类型，我们前面介绍的都是存储记录的数据页，其实还有很多其他类型的页，如表5-5所示。

表 5-5 其他类型的页  

<table><tr><td>类型名称</td><td>十六进制</td><td>描述</td></tr><tr><td>FIL_PAGE_TYPE_ALLOCATED</td><td>0x0000</td><td>最新分配,还未使用</td></tr><tr><td>FIL_PAGE_UNDO_LOG</td><td>0x0002</td><td>undo 日志页</td></tr><tr><td>FIL_PAGE_INODE</td><td>0x0003</td><td>存储段的信息</td></tr><tr><td>FIL_PAGE_IBUF-Free_LIST</td><td>0x0004</td><td>Change Buffer 空闲列表</td></tr><tr><td>FIL_PAGE_IBUF_BITMAP</td><td>0x0005</td><td>Change Buffer 的一些属性</td></tr><tr><td>FIL_PAGE_TYPE_SYS</td><td>0x0006</td><td>存储一些系统数据</td></tr><tr><td>FIL_PAGE_TYPE_TRX_SYS</td><td>0x0007</td><td>事务系统数据</td></tr><tr><td>FIL_PAGE_TYPE_FSP_HDR</td><td>0x0008</td><td>表空间头部信息</td></tr><tr><td>FIL_PAGE_TYPE_XDES</td><td>0x0009</td><td>存储区的一些属性</td></tr><tr><td>FIL_PAGE_TYPE_BLOB</td><td>0x000A</td><td>溢出页</td></tr><tr><td>FIL_PAGE_INDEX</td><td>0x45BF</td><td>索引页,也就是我们所说的数据页</td></tr></table>

用来存放记录的数据页的类型其实是FIL_PAGE_INDEX，也就是索引页。至于啥是索引，且听下回分解。

![](images/ee75638305a998641ab8cada4270274d048f19c1b48b53bbb4dd5cfac5794d93.jpg)

我们在前面唠叨记录的存储结构时，所说的溢出页的类型是FIL_PAGE_TYPE_BLOB，而存放正常记录的页面类型是FIL_PAGE_INDEX，两者是不一样的。

- FIL_PAGE_PREV 和 FIL_PAGE_NEXT：前文强调过，InnoDB 是以页为单位存放数据的，有时在存放某种类型的数据时，占用的空间非常大（比如一张表中可以有成千上万条记录）。InnoDB 可能无法一次性为这么多数据分配一个非常大的存储空间，而如果分散到多个不连续的页中进行存储，则需要把这些页关联起来，FIL_PAGE_PREV 和 FIL_PAGE_NEXT 就分别代表本数据页的上一个页和下一个页的页号。这样通过建立一个双向链表就把许许多多的页串联起来了，而无须这些页在物理上真正连着。需要注意的是，并不是所有类型的页都有上一个页和下一个页的属性，不过我们这里唠

叨的数据页（也就是类型为FIL_PAGE_INDEX的页）是有这两个属性的。所以，存储记录的数据页其实可以组成一个双向链表，如图5-16所示。

![](images/63f83f0e0fb7093db096d48e98b22fe57e5b2555e7ba69442c7a3d04cfd56aa0.jpg)  
图5-16 数据页组成的双向链表

关于FileHeader的其他属性暂时用不到，等后面用到的时候再提。

# 5.7 FileTrailer（文件尾部）

我们知道，InnoDB存储引擎会把数据存储到磁盘上，但是磁盘速度太慢，需要以页为单位把数据加载到内存中处理。如果该页中的数据在内存中被修改了，那么在修改后的某个时间还需要把数据刷新到磁盘中。但是，如果在刷新还没有结束的时候断电了该咋办，这不是相当尴尬么？为了检测一个页是否完整（也就是在刷新时有没有发生只刷新了一部分的尴尬情况），设计InnoDB的大叔在每个页的尾部都加了一个FileTrailer部分，这个部分由8字节组成，可以分成2个小部分。

- 前4字节代表页的校验和。这个部分与FileHeader中的校验和相对应。每当一个页面在内存中发生修改时，在刷新之前就要把页面的校验和算出来。因为FileHeader在页面的前边，所以FileHeader中的校验和会被首先刷新到磁盘，当完全写完后，校验和也会被写到页的尾部。如果页面刷新成功，则页首和页尾的校验和应该是一致的。如果刷新了一部分后断电了，那么FileHeader中的校验和就代表着已经修改过的页，而FileTrailer中的校验和代表着原先的页，二者不同则意味着刷新期间发生了错误。  
- 后4字节代表页面被最后修改时对应的LSN的后4字节，正常情况下应该与FileHeader部分的FIL_PAGE_LSN的后4字节相同。这个部分也是用于校验页的完整性，不过我们目前还没说LSN是什么意思，所以大家可以先不用管这个属性。

这个FileTrailer与FileHeader类似，都通用于所有类型的页。

# 5.8 总结

InnoDB 为了不同的目的而设计了不同类型的页，我们把用于存放记录的页称为数据页。

一个数据页可以被大致划分为7个部分，分别如下。

- FileHeader：表示页的一些通用信息，占固定的38字节。  
- PageHeader：表示数据页专有的一些信息，占固定的56字节。  
- Infimum + Supremum：两个虚拟的伪记录，分别表示页中的最小记录和最大记录，占固定的26字节。  
- User Records：真正存储我们插入的记录，大小不固定。  
- Free Space：页中尚未使用的部分，大小不固定。  
- Page Directory：页中某些记录的相对位置，也就是各个槽对应的记录在页面中的地址偏移量；大小不固定，插入的记录越多，这个部分占用的空间就越多。  
- File Trailer：用于检验页是否完整，占固定的8字节。

每个记录的头信息中都有一个 next_record 属性，从而可以使页中的所有记录串联成一个单向链表。

InnoDB会把页中的记录划分为若干个组，每个组的最后一个记录的地址偏移量作为一个槽，存放在Page Directory中，一个槽占用2字节。在一个页中根据主键查找记录是非常快的，分为两步。

1. 通过二分法确定该记录所在分组对应的槽，并找到该槽所在分组中主键值最小的那条记录。  
2. 通过记录的 next_record 属性遍历该槽所在的组中的各个记录。

每个数据页的FileHeader部分都有上一个页和下一个页的编号，所以所有的数据页会组成一个双向链表。

在将页从内存刷新到磁盘时，为了保证页的完整性，页首和页尾都会存储页中数据的校验和，以及页面最后修改时对应的LSN值（页尾只会存储LSN值的后4字节）。如果页首和页尾的校验和以及LSN值校验不成功，就说明刷新期间出现了问题。

# 第6章 快速查询的秘籍——B+树索引

前文详细唠叨了InnoDB数据页的7个组成部分，我们知道了各个数据页可以组成一个双向链表，而每个数据页中的记录会按照主键值从小到大的顺序组成一个单向链表。每个数据页都会为存储在它里面的记录生成一个页目录，在通过主键查找某条记录的时候可以在页目录中使用二分法快速定位到对应的槽，然后再遍历该槽对应分组中的记录即可快速找到指定的记录（如果你对这段话有一丁点儿疑惑，那么接下来的部分不适合你，返回去看一下数据页结构吧）。页和记录的关系示意图如图6-1所示。

![](images/9183d24a6474a55255278010652f05612a77fb14f866beb1f65e3b19c0f709db.jpg)  
图6-1 页和记录的关系示意图

其中，页a、页b、页c……页n这些页可以不在物理结构上相连，只要通过双向链表相关联即可。

# 6.1 没有索引时进行查找

本章的主题是索引。在正式介绍索引之前，我们需要了解一下在没有索引时是怎么查找记录的。为了方便大家理解，我们先只唠叨搜索条件为某个列等于某个常数的情况，比如下面这样：

SELECT [查询列表] FROM 表名 WHERE 列名 = xxx;

# 6.1.1 在一个页中查找

假设目前表中的记录比较少，所有的记录都可以存放到一个页中。在查找记录时，可以根据搜索条件的不同分为两种情况。

- 以主键为搜索条件：这个查找过程我们已经很熟悉了，可以在页目录中使用二分法快速定位到对应的槽；然后再遍历该槽对应分组中的记录，即可快速找到指定的记录。  
- 以其他列作为搜索条件：对非主键列的查找可就不这么幸运了，因为在数据页中并没

有为非主键列建立所谓的页目录，所以无法通过二分法快速定位相应的槽。在这种情况下，只能从 Infimum 记录开始依次遍历单向链表中的每条记录，然后对比每条记录是否符合搜索条件。很显然，这种查找的效率非常低。

# 6.1.2 在很多页中查找

在很多时候，表中存放的记录都是非常多的，需要用到好多的数据页来存储这些记录。在很多页中查找记录可以分为两个步骤：

- 定位到记录所在的页；  
- 从所在的页内查找相应的记录。

在没有索引的情况下，无论是根据主键列还是其他列的值进行查找，由于我们不能快速地定位到记录所在的页，所以只能从第一页沿着双向链表一直往下找。在每一页中我们根据刚刚唠叨过的查找方式去查找指定的记录。因为要遍历所有的数据页，所以这种方式显然是超级耗时的。如果一个表有1亿条记录，使用这种方式去查找记录，估计要等到猴年马月才能查找到结果。所以人们都在期盼一种能高效完成搜索的方法，索引“同志”就要亮相登台了。

# 6.2 索引

为了故事的顺利发展，我们先建一个表：

```erlang
mysql> CREATE TABLE index_demo(
-> c1 INT,
-> c2 INT,
-> c3 CHAR(1),
-> PRIMARY KEY(c1)
->) ROW_FORMAT = COMPACT;
Query OK, 0 rows affected (0.03 sec) 
```

这个新建的 index_demo 表中有 2 个 INT 类型的列、1 个 CHAR(1) 类型的列，而且我们规定 c1 列为主键。这个表使用 COMPACT 行格式来实际存储记录。为了理解上的方便，我们简化了 index_demo 表的行格式示意图，如图 6-2 所示。

![](images/baca1c3ea75d5876a2983e89dfcb7e70662826db2ee50efb757e09f78882398d.jpg)  
图6-2 index_demo 表的行格式示意图

下面只讲解图6-2中展示的这几个部分。

- record_type：记录头信息的一项属性，表示记录的类型。其中，0表示普通记录；2表示Infimum记录；3表示Supremum记录；1还没用过，等会再说。

- next_record：记录头信息的一项属性，表示从当前记录的真实数据到下一条记录的真实数据的距离。为了方便大家理解，我们会用箭头来表明下一条记录是谁。  
- 各个列的值：这里只展示在 index_demo 表中的 3 个列，分别是 c1、c2 和 c3。  
- 其他信息：除了上述3种信息以外的所有信息，包括其他隐藏列的值以及记录的额外信息。

为了节省篇幅，后文的示意图会把记录中的“其他信息”部分省略掉，因为它占地方，并且也没有什么观赏效果。另外，我觉得把记录竖着看感觉更好，所以，记录格式示意图的“其他信息”去掉后并竖起来的效果如图6-3所示。

把一些记录放到页里边的示意图如图6-4所示。

![](images/d5aab67b53dd34eed18760fd94d10483f587f7a29c61df64029826a440bbd199.jpg)  
图6-3 竖放记录的效果

![](images/dfb1a17794865f92562d89be76887f5ac7167c0dff75867b584b96a7de4f0160.jpg)  
图6-4 记录放到页里边的示意图

# 6.2.1 一个简单的索引方案

回到正题，我们在根据某个搜索条件查找一些记录时，为什么要遍历所有的数据页呢？原因是各个页中的记录并没有规律，我们并不知道搜索条件会匹配哪些页中的记录，所以不得不依次遍历所有的数据页。如果想快速定位到需要查找的记录在哪些数据页中，该咋办？还记得我们为了根据主键值快速定位一条记录在页中的位置而设立的页目录么？我们也可以想办法为快速定位记录所在的数据页而建立一个别的目录，在建这个目录的过程中必须完成两件事儿。

1. 下一个数据页中用户记录的主键值必须大于上一个页中用户记录的主键值。

为了故事的顺利发展，我们这里需要做一个假设：每个数据页最多能存放3条记录（实际上一个数据页非常大，可以存放好多记录）。有了这个假设之后，我们向index_demo表插入3条记录：

```txt
mysql> INSERT INTO index_demo VALUES(1, 4, 'u'), (3, 9, 'd'), (5, 3, 'y'); Query OK, 3 rows affected (0.01 sec)  
Records: 3 Duplicates: 0Warnings: 0 
```

那么，这些记录已经按照主键值的大小串联成一个单向链表了，如图6-5所示。

从图6-5可以看出，index_demo表中的3条记录都被插入到编号为10的数据页中。此时再插入一条记录：

```txt
mysql> INSERT INTO index_demo VALUES(4, 4, 'a');  
Query OK, 1 row affected (0.00 sec) 
```

因为页10最多只能放3条记录，所以我们不得不再分配一个新页，如图6-6所示。

![](images/9295ce87a1a9c7d7de63734ee7308abf8f52c0f25b0adc40ed8a687198daf0c9.jpg)  
图6-5 记录组成的单向链表

![](images/a0619403ef7fea526f7a69dd3d63706a7d396bb499c324221197cca19795edde.jpg)  
图6-6 为记录分配新页

咦？怎么分配的页号是28呀，不应该是11么？再强调一遍，新分配的数据页编号可能并不是连续的，也就是说我们使用的这些页在磁盘上可能并不挨着（不过设计InnoDB的大叔会尽量让这些页面相邻，这个问题我们会在表空间的章节中详细唠叨）。它们只是通过维护上一页和下一页的编号而建立了链表关系。另外，页10中用户记录最大的主键值是5，而页28中有一条记录的主键值是4，因为 $5 > 4$ ，所以这就不符合“下一个数据页中用户记录的主键值必须大于上一个页中用户记录的主键值”的要求，所以在插入主键值为4的记录时需要伴随着一次记录移动，也就是把主键值为5的记录移动到页28中，再把主键值为4的记录插入到页10中。这个过程的示意图如图6-7所示。

![](images/2052945255aae12f207e1596de0b0d8733dde41688bd45f646d15f36ae7bcef0.jpg)  
图6-7 为记录分配新页的过程

这个过程表明，在对页中的记录进行增删改操作的过程中，我们必须通过一些诸如记录移动的操作来始终保证这个状态一直成立：下一个数据页中用户记录的主键值必须大于上一个页中用户记录的主键值。这个过程也可以称为页分裂。

2. 给所有的页建立一个目录项。

由于数据页的编号可能并不是连续的，所以在向 index_demo 表中插入许多条记录后，可能会形成如图 6-8 所示的效果。

![](images/be044311d563c121438761d8fca96050beebbb8e5502aaf8eb21bf8dd141a231.jpg)  
图6-8 向index_demo表中插入许多条记录后的效果

由于这些大小为16KB的页在磁盘上可能并不挨着，如果想从这么多页中根据主键值快速定位某些记录所在的页，就需要给它们编制一个目录，每个页对应一个目录项，每个目录项包括下面两个部分：

- 页的用户记录中最小的主键值，用key来表示；  
- 页号，用page_no表示。

所以我们为上面几个页编制的目录如图6-9所示。

![](images/9ee7782d504dcd8bd5405998e6600da799c0986d60395dcf1ff90754ce15fb0d.jpg)  
图6-9 为页编制目录

以页28为例，它对应目录项2，这个目录项中包含着该页的页号28以及该页中用户记录的最小主键值5。我们只需要把几个目录项在物理存储器上连续存储，比如把它们放到一个数组中，就可以实现根据主键值快速查找某条记录的功能了。比如，我们想查找主键值为20的记录，具体查找过程分两步。

1. 先从目录项中根据二分法快速确定出主键值为20的记录在目录项3中（因为 $12 < 20 < 209$ ），它对应的页是页9。  
2. 再根据前文讲的在页中查找记录的方式去页9中定位具体的记录。

至此，针对数据页编制的简易目录就搞定了。刚才忘记说了，这个目录有一个别名，称为索引。

# 6.2.2 InnoDB中的索引方案

之所以说刚才为每个数据页制作目录项的过程是一个简易的索引方案，是因为我们在根据

主键值进行查找时，为了使用二分法快速定位具体的目录项，而假设所有目录项都可以在物理存储器上连续存储。但是这样做有下面几个问题。

- InnoDB 使用页作为管理存储空间的基本单位，也就是最多只能保证 16KB 的连续存储空间。虽然一个目录项占用不了多大的存储空间，但是架不住表中记录越来越多。此时需要非常大的连续的存储空间才能把所有的目录项都放下，这对记录数量非常多的表来说是不现实的。  
- 我们时常会对记录执行增删改操作，假设我们把页28中的记录都删除，页28也就没有了存在的必要。这也就意味着目录项2也没有了存在的必要，这就需要把目录项2后的目录项都向前移动一下。这种牵一发而动全身的设计不是什么好主意。又或者不移动目录项2，而是将其作为冗余放在目录项列表中，从而浪费了很多存储空间。

所以，设计InnoDB的大叔需要一种可以灵活管理所有目录项的方式。他们发现这些目录项其实与用户记录长得很像，只不过目录项中的两个列是主键和页号而已，所以他们灵光乍现，复用了之前存储用户记录的数据页来存储目录项。为了与用户记录进行区分，我们把这些用来表示目录项的记录称为目录项记录。那么，InnoDB是怎么区分一条记录是普通的用户记录还是目录项记录呢？大家别忘了记录头信息中的record_type属性，它的各个取值代表的意思如下。

- 0：普通的用户记录。  
- 1：目录项记录。  
- 2: Infimum 记录。  
- 3: Supremum 记录。

原来这个值为1的record_type是这个意思。我们把前面使用到的目录项放到数据页中，如图6-10所示。

![](images/0f8398f229b79cc4421699a00b3829a5b594522c462885ec8b3b84d6f42564f9.jpg)  
图6-10 将目录项放到数据页中的效果

从图6-10可以看出，我们新分配了一个编号为30的页来专门存储目录项记录。这里再次强调一下目录项记录和普通的用户记录的不同点。

- 目录项记录的 record_type 值是 1，普通用户记录的 record_type 值是 0。  
- 目录项记录只有主键值和页的编号两个列，而普通用户记录的列是用户自己定义的，可能包含很多列，另外还有InnoDB自己添加的隐藏列。  
- 我们在前面唠叨记录头信息时说过一个名为min_rec_flag的属性，只有目录项记录的

min_rec_flag属性才可能为1，普通用户记录的min_rec_flag属性都是0。

除了上述几点外，这两者就没啥差别了：它们用的是一样的数据页（页面类型都是0x45BF，这个属性在FileHeader中）；页的组成结构也是一样的（就是我们前面介绍过的7个部分）；都会为主键值生成PageDirectory（页目录），从而在按照主键值进行查找时可以使用二分法来加快查询速度。

现在以查找主键为20的记录为例，根据某个主键值去查找记录的步骤可以大致拆分为两步。

1. 先到存储目录项记录的页（也就是页30）中通过二分法快速定位到对应的目录项记录，因为 $12 < 20 < 209$ ，所以定位到对应的用户记录所在的页就是页9。

2. 再到存储用户记录的页9中根据二分法快速定位到主键值为20的用户记录。

虽然说目录项记录中只存储主键值和对应的页号，比用户记录需要的存储空间小多了，但是毕竟一个页只有16KB大小，能存放的目录项记录也是有限的。如果表中的数据太多，以至于一个数据页不足以存放所有的目录项记录，该咋办呢？

当然是再多整一个存储目录项记录的页了。为了让大家更好地理解新分配一个存储目录项记录的页的过程，我们假设一个存储目录项记录的页最多只能存放4条目录项记录（请注意这是假设，真实情况下可以存放好多条）。如果此时再向图6-10中插入一条主键值为320的用户记录，那就需要分配一个新的存储目录项记录的页了，如图6-11所示。

![](images/d6a2f146ba72432266e4f06ba47b2ed015fabc8487912612944e822af1b60b50.jpg)  
图6-11 分配新的数据页

从图6-11可以看出，在插入了一条主键值为320的用户记录之后，需要两个新的数据页：

- 为存储该用户记录而新生成了页31；  
- 因为存储目录项记录的页30的容量已满（前面假设每个页只能存储4条目录项记录），所以不得不需要一个新的页32来存放页31对应的目录项目录。

现在因为存储目录项记录的页不止一个，此时如果想根据主键值查找一条用户记录，则大致需要3个步骤。以查找主键值为20的记录为例，具体如下。

步骤1. 确定存储目录项记录的页。

现在存储目录项记录的页有两个，即页30和页32。又因为页30表示的目录项记录主键值的范围是[1,320)，页32表示的目录项记录主键值不小于320，所以主键值为20的记录对应的目录项记录在页30中。

步骤2. 通过存储目录项记录的页确定用户记录真正所在的页。

前文已经讲过如何在一个存储目录项记录的页中通过主键值定位一条目录项记录，因此这里不再赘述。

步骤3. 在真正存储用户记录的页中定位到具体的记录。

在一个存储用户记录的页中通过主键值定位一条用户记录的方式已经说过好多遍了。你要是还不会，我就……我就求你翻到上一章多看几遍有关数据页结构的内容了。

那么问题来了，在这个查询步骤的步骤1中，我们需要定位存储目录项记录的页，但是这些页在存储空间中也可能不挨着。如果表中的数据非常多，则会产生很多存储目录项记录的页，那我们怎么根据主键值快速定位一个存储目录项记录的页呢？其实也简单，为这些存储目录项记录的页再生成一个更高级的目录，就像是一个多级目录一样，大目录里嵌套小目录，小目录里才是实际的数据。所以，现在各个页的示意图如图6-12所示。

![](images/4f4e1e17bd9fe49ebfa939cdcdd05729f1242f66b19b70fdfd36e23936c09b97.jpg)  
图6-12 生成存储更高级目录项记录的数据页

在图6-12中，我们生成了一个存储更高级目录项记录的页33。这个页中的两条记录分别代表页30和页32。如果用户记录的主键值在[1,320)之间，则到页30中查找更详细的目录项记录；如果主键值不小于320，就到页32中查找更详细的目录项记录。随着表中记录的增加，这个目录的层级会继续增加，如果简化一下，那么可以用图6-13来描述它。

大家看，这玩意儿像不像一棵倒过来的树呢——上面是树根，下面是树叶！其实这是一种组织数据的形式，或者说是一种数据结构，它的名称是 $\mathrm{B}+$ 树。

无论是存放用户记录的数据页，还是存放目录项记录的数据页，我们都把它们存放到 $\mathrm{B + }$ 树这个数据结构中。我们也将这些数据页称为 $\mathrm{B + }$ 树的节点。从图6-12可以看出，我们真正的用户记录其实都存放在 $\mathrm{B + }$ 树最底层的节点上，这些节点也称为叶子节点或叶节点。其余用来存放目录项记录的节点称为非叶子节点或者内节点，其中 $\mathrm{B + }$ 树最上边的那个节点也称为根节点。

![](images/42e57012e9e1424decab3b43ec23750e80b5e39a8f8519175dfb8d8121ffbe4d.jpg)  
图6-13 B+树

从图6-13可以看出，一个 $\mathrm{B + }$ 树的节点其实可以分成好多层。设计InnoDB的大叔为了讨论方便，规定最下面的那层（也就是存放用户记录的那层）为第0层，之后层级依次往上加。在之前的讨论中，我们做了一个非常极端的假设：存放用户记录的页最多存放3条记录，存放目录项记录的页最多存放4条记录。其实在真实环境中，一个页存放的记录数量是非常大的。假设所有存放用户记录的叶子节点所代表的数据页可以存放100条用户记录，所有存放目录项记录的内节点所代表的数据页可以存放1,000条目录项记录，那么：

- 如果 $\mathrm{B}+$ 树只有1层，也就是只有1个用于存放用户记录的节点，则最多能存放100条用户记录；  
- 如果 $\mathrm{B}+$ 树有2层，最多能存放 $1,000 \times 100 = 100,000$ 条用户记录；  
- 如果 $\mathrm{B}+$ 树有3层，最多能存放 $1,000 \times 1,000 \times 100 = 100,000,000$ 条用户记录；  
- 如果 $\mathrm{B}+$ 树有4层，最多能存放 $1,000 \times 1,000 \times 1,000 \times 100 = 100,000,000,000$ 条用户记录。（这么多的记录！）

你的表里能存放100,000,000,000条记录么？所以在一般情况下，我们用到的B+树都不会超过4层。这样一来，在通过主键值去查找某条记录时，最多只需要进行4个页面内的查找（查找3个存储目录项记录的页和1个存储用户记录的页）。又因为在每个页面内存在Page Directory（页目录），所以在页面内也可以通过二分法快速定位记录。

![](images/784ee4a1b776404cc21dc402d9759dbfba75fdc23d57b0cfcdfa49034c5fa2a2.jpg)

# 小贴士

我们在唠叨数据页的PageHeader部分时介绍过一个名为PAGE_LEVEL的属性，它就代表着这个数据页作为节点在B+树中的层级。

# 1. 聚簇索引

前面介绍的 $\mathbf{B}+$ 树本身就是一个目录，或者说本身就是一个索引，它有下面两个特点。

- 使用记录主键值的大小进行记录和页的排序，这包括3方面的含义。

页（包括叶子节点和内节点）内的记录按照主键的大小顺序排成一个单向链表，页内的记录被划分成若干个组，每个组中主键值最大的记录在页内的偏移量会被当作槽依次存放在页目录中（当然Supremum记录比任何用户记录都大），我们可以在页目录中通过二分法快速定位到主键列等于某个值的记录。  
- 各个存放用户记录的页也是根据页中用户记录的主键大小顺序排成一个双向链表。

- 存放目录项记录的页分为不同的层级，在同一层级中的页也是根据页中目录项记录的主键大小顺序排成一个双向链表。

- B+树的叶子节点存储的是完整的用户记录。所谓完整的用户记录，就是指这个记录中存储了所有列的值（包括隐藏列）。

我们把具有这两个特点的 $\mathrm{B}+$ 树称为聚簇索引，所有完整的用户记录都存放在这个聚簇索引的叶子节点处。这种聚簇索引并不需要我们在MySQL语句中显式地使用INDEX语句去创建（后边会介绍索引相关的语句），InnoDB存储引擎会自动为我们创建聚簇索引。另外有趣的一点是，在InnoDB存储引擎中，聚簇索引就是数据的存储方式（所有的用户记录都存储在了叶子节点），也就是所谓的“索引即数据，数据即索引”。

# 2. 二级索引

大家是否发现，聚簇索引只能在搜索条件是主键值时才能发挥作用，原因是B+树中的数据都是按照主键进行排序的。如果我们想以别的列作为搜索条件该咋办呢？难道只能从头到尾沿着链表依次遍历记录么？

不！我们可以多建几棵 $\mathrm{B + }$ 树，并且不同 $\mathbf{B}+$ 树中的数据采用不同的排序规则。比如，我们用c2列的大小作为数据页、页中记录的排序规则，然后再建一棵 $\mathrm{B + }$ 树，如图6-14所示。

![](images/24082d9a1c66a521b679ee9ca0f24ac481125e4fbaeebba9a5112c7d9a38ecf6.jpg)  
图6-14 新建B+树

这个 $\mathrm{B}+$ 树与前文介绍的聚簇索引有几处不同。

- 使用记录c2列的大小进行记录和页的排序，这包括3方面的含义。

页（包括叶子节点和内节点）内的记录是按照c2列的大小顺序排成一个单向链表，页内的记录被划分成若干个组，每个组中c2列值最大的记录在页内的偏移量会被当作槽依次存放在页目录中（当然规定Supremum记录比任何用户记录都大），我们可以在页目录中通过二分法快速定位到c2列等于某个值的记录。

- 各个存放用户记录的页也是根据页中记录的c2列大小顺序排成一个双向链表。  
- 存放目录项记录的页分为不同的层级，在同一层级中的页也是根据页中目录项记录的c2列大小顺序排成一个双向链表。

- B+ 树的叶子节点存储的并不是完整的用户记录，而只是 c2 列 + 主键这两个列的值。  
- 目录项记录中不再是主键 + 页号的搭配，而变成了 c2 列 + 页号的搭配。

现在，比方说我们想查找满足搜索条件 $c2 = 4$ 的记录，就可以使用刚刚建好的这棵 $\mathrm{B + }$ 树了。不过我们这里需要注意一下，因为c2列并没有唯一性约束，也就是说满足搜索条件 $c2 = 4$ 的记录可能有很多条，其实我们只需要在该 $\mathbf{B}+$ 树的叶子节点处定位到第一条满足搜索条件 $c2 = 4$ 的那条记录，然后沿着由记录组成的单向链表一直向后扫描即可。另外，各个叶子节点组成了双向链表，搜索完了本页面的记录后可以很顺利地跳到下一个页面中的第一条记录，然后继续沿着记录组成的单向链表向后扫描。查找过程如下。

步骤1. 确定第一条符合 $c2 = 4$ 条件的目录项记录所在的页。

根据根页面（也就是页44）可以快速定位到第一条符合 $c2 = 4$ 条件的目录项记录所在的页为页42（因为 $2 < 4 < 9$ ）。

步骤2. 通过第一条符合 $c2 = 4$ 条件的目录项记录所在的页面确定第一条符合 $c2 = 4$ 条件的用户记录所在的页。

根据页42可以快速定位到第一条符合条件的用户记录所在的页为页34或者页35中（因为 $2 < 4 \leqslant 4$ ）。

步骤3.在真正存储第一条符合 $c2 = 4$ 条件的用户记录的页中定位到具体的记录。

到页34和页35中定位到具体的用户记录（如果在页34中使用页目录定位到第一条符合条件的用户记录，就不需要再到页35中使用页目录去定位第一条符合条件的用户记录了）。

步骤4. 这个 $\mathrm{B}+$ 树的叶子节点中的记录只存储了c2和c1（也就是主键）两个列。在这个 $\mathrm{B}+$ 树的叶子节点处定位到第一条符合条件的那条用户记录之后，我们需要根据该记录中的主键信息到聚簇索引中查找到完整的用户记录。这个通过携带主键信息到聚簇索引中重新定位完整的用户记录的过程也称为回表。然后再返回到这棵 $\mathrm{B}+$ 树的叶子节点处，找到刚才定位到的符合条件的那条用户记录，并沿着记录组成的单向链表向后继续搜索其他也满足 $c2 = 4$ 的记录，每找到一条的话就继续进行回表操作。重复这个过程，直到下一条记录不满足 $c2 = 4$ 的这个条件为止。

为什么还需要一次回表操作呢？直接把完整的用户记录放到叶子节点不就好了么？你说得对，如果把完整的用户记录放到叶子节点是可以不用回表，但是太占地方了——相当于每建立一棵B+树都需要把所有的用户记录复制一遍，这就太浪费存储空间了。

因为这种以非主键列的大小为排序规则而建立的 $\mathrm{B + }$ 树需要执行回表操作才可以定位到完整的用户记录，所以这种 $\mathrm{B + }$ 树也称为二级索引（Secondary Index）或辅助索引。由于我们是以c2列的大小作为 $\mathrm{B + }$ 树的排序规则，所以我们也称这棵 $\mathrm{B + }$ 树为为c2列建立的索引，把c2列称为索引列。二级索引记录和聚簇索引记录使用的一样的记录行格式，只不过二级索引记录存储的列不像聚簇索引记录那么完整。

![](images/54e994ec173353c5796812129060b8edca4cac7ca1713019cd6eb17733b2de2f.jpg)

我们上面把聚簇索引或者二级索引的叶子节点中的记录称为用户记录。为了区分，也把聚簇索引叶子节点中的记录称为完整的用户记录，把二级索引叶子节点中的记录称为不完整的用户记录。

另外，c1、c2列存储的都是数字，为这两个列建立索引的过程比较好理解。如果我们为一个存储字符串的列建立索引，比如为c3列建立索引，情况会和上述过程有啥不一样么？没啥不一样，别忘了我们前面唠叨过的字符集和比较规则，字符串也是可以比较大小的。

# 3. 联合索引

我们也可以同时以多个列的大小作为排序规则，也就是同时为多个列建立索引。比如，我们想让 $\mathrm{B + }$ 树按照c2和c3列的大小进行排序，这里面包含两层含义：

- 先把各个记录和页按照c2列进行排序；  
- 在记录的 c2 列相同的情况下，再采用 c3 列进行排序。

为c2和c3列建立索引，示意图如图6-15所示。

![](images/7e4a3a0feb4e27c363a5f570e3f19d2cde96fc4eabe12dfcfaf5b0c993be867c.jpg)  
图6-15 为c2和c3列建立的索引示意图

在图6-15中需要注意以下两点。

- 每条目录项记录都由c2列、c3列、页号这3部分组成。各条记录先按照c2列的值进行排序，如果记录的c2列相同，则按照c3列的值进行排序。  
- $\mathrm{B + }$ 树叶子节点处的用户记录由c2列、c3列和主键c1列组成。

千万要注意的是，以c2和c3列的大小为排序规则建立的B+树称为联合索引，也称为复合索引或多列索引。它本质上也是一个二级索引，它的索引列包括c2、c3。需要注意的是，“以c2和c3列的大小为排序规则建立联合索引”和“分别为c2和c3列建立索引”的表述是不同的，不同点如下。

- 建立联合索引只会建立如图6-15所示的一棵 $\mathrm{B}+$ 树。  
- 为c2和c3列分别建立索引时，则会分别以c2和c3列的大小为排序规则建立两棵 $\mathrm{B}+$ 树。

# 6.2.3 InnoDB中B+树索引的注意事项

# 1. 根页面万年不动窝

前面在介绍 $\mathrm{B + }$ 树索引的时候，为了方便理解，我们先把存储用户记录的叶子节点都画出来，然后再画出存储目录项记录的内节点。实际上 $\mathrm{B + }$ 树的形成过程是下面这样的。

- 每当为某个表创建一个 $\mathrm{B}+$ 树索引（聚簇索引不是人为创建的，它默认就存在）时，都会为这个索引创建一个根节点页面。最开始表中没有数据的时候，每个 $\mathrm{B}+$ 树索引对应的根节点中既没有用户记录，也没有目录项记录。  
- 随后向表中插入用户记录时，先把用户记录存储到这个根节点中。  
- 在根节点中的可用空间用完时继续插入记录，此时会将根节点中的所有记录复制到一个新分配的页（比如页a）中，然后对这个新页进行页分裂操作，得到另一个新页（比如页b）。这时新插入的记录会根据键值（也就是聚簇索引中的主键值，或二级索引中对应的索引列的值）的大小分配到页a或页b中。根节点此时便升级为存储目录项记录的页，也就需要把页a和页b对应的目录项记录插入到根节点中。

在这个过程中，需要特别注意的是，一个 $\mathrm{B}+$ 树索引的根节点自创建之日起便不会再移动（也就是页号不再改变）。这样只要我们对某个表建立一个索引，那么它的根节点的页号便会被记录到某个地方，后续凡是InnoDB存储引擎需要用到这个索引时，都会从那个固定的地方取出根节点的页号，从而访问这个索引。

![](images/9ea4d7f45d7421acc134e3ce49acccd87f62c829da404e552baf6d7cbcadafbc.jpg)

小贴士

跟大家剧透一下，这个“存储某个索引的根节点在哪个页面中”的信息就是传说中的数据字典中的一项信息。关于数据字典的更多内容，第9章会详细唠叨，请别着急。

# 2. 内节点中目录项记录的唯一性

我们知道，在B+树索引的内节点中，目录项记录的内容是索引列加页号的搭配，但是这个搭配对于二级索引来说有点儿不严谨。还是以index_demo表为例进行讲解，假设这个表中的数据如表6-1所示。

表 6-1 index_demo 表中的数据  

<table><tr><td>c1</td><td>c2</td><td>c3</td></tr><tr><td>1</td><td>1</td><td>&#x27;u&#x27;</td></tr><tr><td>3</td><td>1</td><td>&#x27;d&#x27;</td></tr><tr><td>5</td><td>1</td><td>&#x27;y&#x27;</td></tr><tr><td>7</td><td>1</td><td>&#x27;a&#x27;</td></tr></table>

如果在二级索引中，目录项记录的内容只是索引列 + 页号的搭配，那么为 c2 列建立索引后的 B+ 树应该如图 6-16 所示。

如果我们想新插入一行记录，其中c1、c2、c3的值分别为9、1、'c'，那么在修改为c2列建立的二级索引对应的B+树时，便碰到了个大问题：由于页3中存储的目录项记录是由c2列 + 页号构成的，页3中的两条目录项记录对应的c2列的值都是1，而新插入的这条记录中，c2列的值也是1，

那么这条新插入的记录到底应该放到页4中，还是应该放到页5中呢？答案是：对不起，发懵了。

为了让新插入的记录能找到自己在哪个页中，就需要保证 $\mathrm{B + }$ 树同一层内节点的目录项记录除页号这个字段以外是唯一的。所以二级索引的内节点的目录项记录的内容实际上是由3部分构成的：

- 索引列的值；  
- 主键值；  
- 页号。

也就是我们把主键值也添加到二级索引内节点中的目录项记录中，这样就能保证 $\mathrm{B + }$ 树每一层节点中各条目录项记录除页号这个字段外是唯一的，所以我们为c2列建立二级索引后的示意图实际上应该如图6-17所示。

![](images/4960bb7820bc61a12c5c8b12ff26b91c8d235a30c28651536bb6340c7168d18a.jpg)  
图6-16 为c2列建立索引后的B+树

![](images/dee7682d1a8463f8473c673d921f50bb17f676d5cfc71f97cf65d2d0168c7e46.jpg)  
图6-17 二级索引内节点的目录项记录实际包含主键值

这样我们再插入记录 $(9,1,'c')$ 时，由于页3中存储的目录项记录是由c2列 $^+$ 主键 $^+$ 页号构成的，因此可以先把新记录的c2列的值和页3中各目录项记录的c2列的值进行比较；如果c2列的值相同，可以接着比较主键值。因为B+树同一层中不同目录项记录的c2列 $^+$ 主键的值肯定是不一样的，所以最后肯定能定位到唯一的一条目录项记录。

在本例中，最后确定新记录应该插入到页5中。

![](images/12e0dbc7ba213a63e232405b891faf6490ce039ae2e37f39e09909ee4ec5de4b.jpg)

对于二级索引记录来说，是先按照二级索引列的值进行排序，在二级索引列值相同的情况下，再按照主键值进行排序。所以，为c2列建立索引其实相当于为(c2,c1)列建立了一个联合索引。另外，对于唯一二级索引（当我们为某个列或列组合声明UNIQUE属性时，便会为这个列或列组合建立唯一二级索引）来说，也可能会出现多条记录键值相同的情况（一是声明为UNIQUE属性的列可能存储多个NULL值，二是我们后面要讲的MVCC服务），唯一二级索引的内节点的目录项记录也会包含记录的主键值。

# 3. 一个页面至少容纳2条记录

前面说过，一棵 $\mathrm{B + }$ 树只需要很少的层级就可以轻松存储数亿条记录，查询速度杠杠的！这是因为 $\mathrm{B + }$ 树本质上就是一个大的多层级目录，每经过一个目录时都会过滤掉许多无效的子目录，直到最后访问到存储真正数据的目录。

如果一个大的目录中只存放一个子目录是啥效果呢？那就是目录层级会非常多，而且最后那个存放真正数据的目录中只能存放一条记录。费了半天劲只能存放一条真正的用户记录？逗我玩？所以InnoDB的一个数据页至少可以存放2条记录，这也是我们之前唠叨记录行格式的时候说过的一个结论（我们当时以这个结论为基础，推导了表中只有一个列且该列在不发生溢出的情况下，最多能存储多少字节。大家如果忘了的话请回过头去看看吧）。

![](images/64163a983130792d671c813c1a186d1956ef0ee463ed37101f729bb6915ebbd7.jpg)

其实，让 $\mathrm{B}+$ 树的叶子节点只存储一条记录，让内节点存储多条记录，也还是可以发挥 $\mathrm{B}+$ 树作用的。但是，设计InnoDB的大叔还是为了避免 $\mathrm{B}+$ 树的层级增长得过高，而要求所有数据页都至少可以容纳2条记录。

# 6.2.4 MyISAM中的索引方案简介

至此，我们介绍的都是InnoDB存储引擎中的索引方案。为了内容的完整性，以及各位同学可能会在面试时遇到这类问题，我们还是有必要简单介绍一下MyISAM存储引擎中的索引方案。

我们知道，在InnoDB中索引即数据，也就是聚簇索引的那棵 $\mathrm{B + }$ 树的叶子节点中已经包含了所有完整的用户记录。MyISAM的索引方案虽然也使用树形结构，但是却将索引和数据分开存储。

- 将表中的记录按照记录的插入顺序单独存储在一个文件中（称之为数据文件）。这个文件并不划分为若干个数据页，有多少记录就往这个文件中塞多少记录。这样一来，我们可以通过行号快速访问到一条记录。

MyISAM记录也需要记录头信息来存储一些额外数据。我们以前文唠叨过的index_demo表为例，看一下这个表在使用MyISAM作为存储引擎时，它的记录如何在存储空间中表示，如图6-18所示。

![](images/3f3ce62dedbb593d66cc8215617d952156f554d7f670d3561ddf4251c4f97555.jpg)  
图6-18 index_demo 表使用MyISAM作为存储引擎在存储空间中的表示

由于在插入数据时并没有刻意按照主键大小排序，所以我们不能在这些数据上使用二分法进行查找。

- 使用MyISAM存储引擎的表会把索引信息单独存储到另外一个文件中（称为索引文件）。MyISAM会为表的主键单独创建一个索引，只不过在索引的叶子节点中存储的不是完整的用户记录，而是主键值与行号的组合。也就是先通过索引找到对应的行号，再通过行号去找对应的记录！

这一点与InnoDB是完全不相同的。在InnoDB存储引擎中，我们只需要根据主键值对聚簇索引进行一次查找就能找到对应的记录，而在MyISAM中却需要进行一次回表操作，这也意味着MyISAM中建立的索引相当于全部都是二级索引！

- 如果有必要，我们也可以为其他列分别建立索引或者建立联合索引，其原理与InnoDB中的索引差不多，只不过在叶子节点处存储的是相应的列+行号。这些索引也全部都是二级索引。

![](images/a8239779ab6224c3c22c462523c70b26c11f86ac1fe83442da664a4531649dec.jpg)

MyISAM的行格式有定长记录格式（Static）、变长记录格式（Dynamic）、压缩记录格式（Compressed）等。上文用到的index_demo表采用定长记录格式，也就是一条记录占用的存储空间是固定的，这样就可以使用行号轻松算出某条记录在数据文件中的地址偏移量了。但是变长记录格式就不行了，MyISAM会直接在索引叶子节点处存储该条记录在数据文件中的地址偏移量。由此可以看出，MyISAM的回表操作是十分快速的，因为它是拿着地址偏移量直接到文件中取数据，而InnoDB是通过获取主键之后再去聚簇索引中找记录，虽然说也不慢，但还是比不上直接用地址去访问。

这里只是非常简要地介绍了MyISAM的索引，要是将具体细节全部写出来就又可以独立成章了。这里只是希望大家理解InnoDB中的“索引即数据，数据即索引”，而在MyISAM中却是“索引是索引，数据是数据”。

# 6.2.5 MySQL中创建和删除索引的语句

前文光顾着唠叨索引的原理了，我们如何使用MySQL语句建立这种索引呢？InnoDB和MyISAM会自动为主键或者带有UNION属性的列建立索引。如果想为其他的列建立索引，就需要我们显式地指明了。为啥不自动为每个列都建立索引呢？别忘了，每建立一个索引都会建立一棵B+树，而且每增、删、改一条记录都要维护各个记录、数据页的排序关系，这是很费性能和存储空间的。

我们可以在创建表的时候，指定需要建立索引的单个列或者建立联合索引的多个列：

```txt
CREATE TALBE 表名（各个列的信息，（KEY|INDEX）索引名（需要被索引的单个列或多个列）
```

其中，KEY和INDEX是同义词，任意选用一个就可以。

我们也可以在修改表结构的时候添加索引：

ALTER TABLE 表名 ADD (INDEX|KEY) 索引名（需要被索引的单个列或多个列）；

还可以在修改表结构的时候删除索引：

ALTER TABLE 表名 DROP (INDEX|KEY) 索引名;

比如，我们想在创建 index_demo 表时就为 c2 和 c3 列添加一个联合索引，可以这么写建表语句：

```sql
CREATE TABLE index_demo(
    c1 INT,
    c2 INT,
    c3 CHAR(1),
    PRIMARY KEY(c1),
    INDEX idx_c2_c3 (c2, c3);
); 
```

在这个建表语句中，创建的索引的名称是idx_c2_c3。索引的名字尽管可以随意起，不过还是建议在命名时能以idx_为前缀，后面跟着需要建立索引的列名，且多个列名之间用下划线分隔开。

如果我们想删除这个索引，可以这么写：

```sql
ALTER TABLE index_demo DROP INDEX idx_c2_c3; 
```

# 6.3 总结

InnoDB存储引擎的索引是一棵 $\mathrm{B + }$ 树，完整的用户记录都存储在 $\mathrm{B + }$ 树第0层的叶子节点；其他层次的节点都属于内节点，内节点中存储的是目录项记录。

InnoDB的索引分为两种。

- 聚簇索引：以主键值的大小作为页和记录的排序规则，在叶子节点处存储的记录包含了表中所有的列。  
- 二级索引：以索引列的大小作为页和记录的排序规则，在叶子节点处存储的记录内容是索引列 + 主键。

InnoDB存储引擎的 $\mathbf{B}+$ 树根节点自创建之日起就不再移动。

在二级索引的 $\mathbf{B}+$ 树内节点中，目录项记录由索引列的值、主键值和页号组成。

一个数据页至少可以容纳2条记录。

MyISAM存储引擎的数据和索引分开存储，这种存储引擎的索引全部都是二级索引，在叶子节点处存储的是列 + 行号（对于定长记录格式的记录来说）。

# 第7章 B+树索引的使用

前面的章节非常详细地唠叨了InnoDB存储引擎的 $\mathrm{B + }$ 树索引，我们必须熟悉下面这些结论。

- 每个索引都对应一棵 $\mathrm{B}+$ 树。 $\mathrm{B}+$ 树分为好多层，最下边一层是叶子节点，其余的是内节点。所有用户记录都存储在 $\mathrm{B}+$ 树的叶子节点，所有目录项记录都存储在内节点。  
- InnoDB存储引擎会自动为主键建立聚簇索引（如果没有显式指定主键或者没有声明不允许存储NULL的UNIQUE键，它会自动添加主键），聚簇索引的叶子节点包含完整的用户记录。  
- 我们可以为感兴趣的列建立二级索引，二级索引的叶子节点包含的用户记录由索引列和主键组成。如果想通过二级索引查找完整的用户记录，需要执行回表操作，也就是在通过二级索引找到主键值之后，再到聚簇索引中查找完整的用户记录。  
- B+树中的每层节点都按照索引列的值从小到大的顺序排序组成了双向链表，而且每个页内的记录（无论是用户记录还是目录项记录）都按照索引列的值从小到大的顺序形成了一个单向链表。如果是联合索引，则页面和记录先按照索引列中前面的列的值排序；如果该列的值相同，再按照索引列中后面的列的值排序。比如，我们对列c2和c3建立了联合索引idx_c2_c3(c2, c3)，那么该索引中的页面和记录就先按照c2列的值进行排序；如果c2列的值相同，再按照c3列的值排序。  
- 通过索引查找记录时，是从 $\mathrm{B}+$ 树的根节点开始一层一层向下搜索的。由于每个页面（无论是内节点页面还是叶子节点页面）中的记录都划分成了若干个组，每个组中索引列值最大的记录在页内的偏移量会被当作槽依次存放在页目录中（当然，规定Supremum记录比任何用户记录都大），因此可以在页目录中通过二分法快速定位到索引列等于某个值的记录。

如果大家在阅读上述结论时哪怕有一点疑惑，那么下面的内容就不适合你，请回过头去反复阅读前面的章节。

# 7.1 B+树索引示意图的简化

为了故事的顺利发展，我们需要先建立一个表：

```sql
CREATE TABLE single_table (
id INT NOT NULL AUTO_INCREMENT,
key1VARCHAR(100),
key2 INT,
key3VARCHAR(100),
key_part1VARCHAR(100),
key_part2VARCHAR(100),
key_part3VARCHAR(100),
common_field VARCHAR(100),
PRIMARY KEY (id),
KEY idx_key1 (key1), 
```

```txt
UNIQUE KEY uk_key2 (key2),  
KEY idx_key3 (key3),  
KEY idx_key_part(key_part1, key_part2, key_part3)  
) Engine=InnoDB CHARSET=utf8; 
```

我们为这个single_table表建立了1个聚簇索引和4个二级索引，分别是：

- 为 id 列建立的聚簇索引；  
- 为key1列建立的idx_key1二级索引；  
- 为 key2 列建立的 uk_key2 二级索引，而且该索引是唯一二级索引；  
- 为 key3 列建立的 idx_key3 二级索引；  
- 为 key_part1、key_part2、key_part3 列建立的 idx_key_part 二级索引，这也是一个联合索引。

然后需要为这个表插入10,000行记录。除id列外，其余的列插入随机值就好了。具体的插入语句这里就不写了，大家可以自己写个程序插入（id列是自增主键列，不需要手动插入）。这个表会在后面章节中频繁用到，大家需要留意。

为了方便大家理解，第6章把 $\mathrm{B + }$ 树的完整结构画了出来，包括它的内节点和叶子节点，以及各个节点中的记录。不过我们现在已经掌握了 $\mathrm{B + }$ 树的基本原理，知道了 $\mathrm{B + }$ 树其实是一个“矮矮的大胖子”，并且学习了如何利用 $\mathrm{B + }$ 树快速地定位记录。所以，是时候简化一下 $\mathrm{B + }$ 树的示意图了。比如我们可以把single_table表的聚簇索引示意图简化为如图7-1所示的样子。

在图7-1中，我们把聚簇索引对应的复杂的 $\mathrm{B + }$ 树结构进行了极度精简。可以看到，图中忽略掉了页的结构，直接把所有的叶子节点中的记录都放在一起展示。方便起见，我们后面把聚簇索引叶子节点中的记录称为聚簇索引记录。虽然图7-1很简陋，但还是突出了聚簇索引的一个非常重要的特点：聚簇索引记录是按照主键值由小到大的顺序排序的。当然，为了追求视觉上的极致简洁，图7-1中的“其他列”也可以略去，只需要保留id列即可。再次简化后的 $\mathrm{B + }$ 树示意图如图7-2所示。

![](images/2861ddccc4f8de97d8311eb4546c031753258aa67576db2813857bcfd23d857d.jpg)  
图7-1 简化后的聚簇索引示意图

![](images/2ff654b9ffe2c28c700d6f69c05a61d092708b726e213cce1d5ef03a478fb6f4.jpg)  
图7-2 再次简化后的聚簇索引示意图

好了，不能再简化了，再简化就要把id列也删去了，这样就只剩一个三角形了，那就真尴尬了。

通过聚簇索引对应的 $\mathrm{B + }$ 树，我们可以很容易地定位到主键值等于某个值的聚簇索引记录。比如我们想通过这个 $\mathrm{B + }$ 树定位到id值为1438的记录，那么示意图就如图7-3所示。

下面以二级索引idx_key1为例，画出二级索引简化后的B+树示意图，如图7-4所示。

在图7-4中，我们在二级索引idx_key1对应的B+树中保留了叶子节点的记录，这些记录包括

key1列以及id列。这些记录是按照key1列的值由小到大的顺序排序的。如果key1列的值相同，则按照id列的值进行排序。方便起见，我们之后就把二级索引叶子节点中的记录称为二级索引记录。

![](images/62b81e08b8149b6d2d94bae2370aa88458c9f08ed9718f13fabe67829390cd04.jpg)  
图7-3 定位id值为1438的记录的示意图

![](images/fe9b8167d77f7a4ff9dc8ee2e001a2f9c7025714da246a9bfb0a626b3068741e.jpg)  
图7-4二级索引idx_key1简化后的 $\mathbb{B}+$ 树示意图

如果想查找key1值等于某个值的二级索引记录，那么通过idx_key1对应的B+树，可以很容易地定位到第一条key1列的值等于某个值的二级索引记录，然后沿着记录所在的单向链表向后扫描即可。比如我们想通过这棵B+树定位到key1值为'abc'的第一条记录，则示意图如图7-5所示。

![](images/007fd5a9257639f3f99cf5e36d8d3b309693a661fd9b82deafe7c0efe419543b.jpg)  
图7-5 定位key1值为'abc'的第一条记录时的示意图

# 7.2 索引的代价

现在大家应该熟悉了 $\mathrm{B}+$ 树索引的原理。本章的主题是唠叨如何更好地使用索引。虽然索引是个好东西，但不能肆意创建。在介绍如何更好地使用索引之前，有必要先了解一下使用索引的代价——它在空间和时间上都会“拖后腿”。

- 空间上的代价

这个是显而易见的，因为每建立一个索引，都要为它建立一棵 $\mathrm{B + }$ 树。每一棵 $\mathrm{B + }$ 树的每一个节点都是一个数据页。一个数据页默认会占用16KB的存储空间，而一棵很大的 $\mathrm{B + }$ 树由许多数据页组成，这将占用很大的一片存储空间。

# 时间上的代价

每当对表中的数据进行增删改操作时，都需要修改各个 $\mathrm{B + }$ 树索引。而且我们讲过， $\mathrm{B + }$ 树中的每层节点都按照索引列的值从小到大的顺序排序组成了双向链表。无论是叶子节点中的记录还是内节点中的记录（也就是无论是用户记录还是目录项记录），都按照索引列的值从小到大的顺序形成了一个单向链表。而增删改操作可能会对节点和记录的排序造成破坏，所以存储引擎需要额外的时间进行页面分裂、页面回收等操作，以维护节点和记录的排序。如果建立了许多索引，每个索引对应的 $\mathrm{B + }$ 树都要进行相关的维护操作，这能不给性能拖后腿么？

另外还有一点就是在执行查询语句前，首先要生成一个执行计划。一般情况下，一条查询语句在执行过程中最多使用一个二级索引（当然也有例外，这将在第10章详细唠叨），在生成执行计划时需要计算使用不同索引执行查询时所需的成本，最后选取成本最低的那个索引执行查询（关于如何计算查询的成本，将在第12章详细唠叨）。此时如果建了太多索引，可能会导致成本分析过程耗时太多，从而影响查询语句的执行性能。

所以，在一个表中建立的索引越多，占用的存储空间也就越多，在增删改记录或者生成执行计划时性能也就越差。为了建立又好又快的索引，我们得先了解索引在查询执行期间到底是如何发挥作用的。

# 7.3 应用B+树索引

# 7.3.1 扫描区间和边界条件

对于某个查询来说，最简单粗暴的执行方案就是扫描表中的所有记录，判断每一条记录是否符合搜索条件。如果符合，就将其发送到客户端，否则就跳过该记录。这种执行方案也称为全表扫描。对于使用InnoDB存储引擎的表来说，全表扫描意味着从聚簇索引第一个叶子节点的第一条记录开始，沿着记录所在的单向链表向后扫描，直到最后一个叶子节点的最后一条记录。虽然全表扫描是一种很笨的执行方案，但却是一种万能的执行方案，所有的查询都可以使用这种方案来执行。

前文讲到，可以利用 $\mathrm{B + }$ 树查找索引列值等于某个值的记录，这样可以明显减少需要扫描的记录数量。由于 $\mathrm{B + }$ 树叶子节点中的记录是按照索引列值由小到大的顺序排序的，所以只扫描某个区间或者某些区间中的记录也可以明显减少需要扫描的记录数量。比如下面这个查询语句：

SELECT $\star$ FROM single_table WHERE id >= 2 AND id <= 100;

这个语句其实是想查找id值在[2,100]区间中的所有聚簇索引记录。我们可以通过聚簇索引对应的B+树快速地定位到id值为2的那条聚簇索引记录，然后沿着记录所在的单向链表向后扫描，直到某条聚簇索引记录的id值不在[2,100]区间中为止（即id值不再符合 $\mathrm{id}<=100$ 条件）。

与扫描全部的聚簇索引记录相比，扫描id值在[2,100]区间中的记录已经很大程度地减少了需要扫描的记录数量，所以提升了查询效率。简便起见，我们把这个例子中待扫描记录的id值所在的区间称为扫描区间，把形成这个扫描区间的搜索条件（也就是 $\mathrm{id} >= 2$ AND $\mathrm{id} <= 100$ ）称为形成这个扫描区间的边界条件。

![](images/927941d9b95de30c0e78130345e6c11d9a614087c5f1075b2e5cafdc419ce190.jpg)

其实对于全表扫描来说，相当于扫描id值在 $(-\infty, +\infty)$ 区间中的记录，也就是说全表扫描对应的扫描区间是 $(-\infty, +\infty)$ 。

对于下面这个查询语句：

SELECT * FROM single_table WHERE key2 IN (1438, 6328) OR (key2 >= 38 AND key2 <= 79); 当然可以直接使用全表扫描的方式执行该查询，但是我们发现该查询的搜索条件涉及 key2 列，而我们又正好为 key2 列建立了 uk_key2 索引。如果使用 uk_key2 索引执行这个查询，则相当于从下面的 3 个扫描区间中获取二级索引记录。

- [1438, 1438]: 对应的边界条件就是 key2 IN (1438)。  
- [6328, 6328]: 对应的边界条件就是 key2 IN (6328)。  
- [38, 79]：对应的边界条件就是 key2 >= 38 AND key2 <= 79。

这些扫描区间对应到数轴上时，如图7-6所示。

![](images/afeda2086b0e604d1a202bb8c30b01fce7ca98a7014db6ee4fa1520816babfbb.jpg)  
图7-6 扫描区间在数轴上的显示

方便起见，我们把像[1438, 1438]、[6328, 6328]这样只包含一个值的扫描区间称为单点扫描区间，把[38, 79]这样包含多个值的扫描区间称为范围扫描区间。另外，由于我们的查询列表是\*，也就是需要读取完整的用户记录，所以从上述扫描区间中每获取一条二级索引记录，就需要根据该二级索引记录的id列的值执行回表操作，也就是到聚簇索引中找到相应的聚簇索引记录。

![](images/dc492ffd621786546ccbde9b75edc8d9a0c70cd14ea7c8de2fb8da7a40db4fda.jpg)

其实我们不仅仅可以使用uk_key2执行上述查询，还可以使用idx_key1、idx_key3、idx_key_part执行上述查询。以idx_key1为例，很显然无法通过搜索条件形成合适的扫描区间来减少需要扫描的idx_key1二级索引记录的数量，只能扫描idx_key1的全部二级索引记录。针对获取到的每一条二级索引记录，都需要执行回表操作来获取完整的用户记录。我们也可以说，使用idx_key1执行查询时对应的扫描区间就是 $(-\infty, +\infty)$ 。

这样虽然行得通，但我们图啥呢？最简单粗暴的全表扫描方式已经需要扫描全部的聚簇索引记录了，这里除了需要访问全部的聚簇索引记录，还要扫描全部的idx_key1二级索引记录，这不是费力不讨好么。可见，在这个过程中并没有减少需要扫描的记录数量，效率反而比全表扫描更差。所以如果想使用某个索引来执行查询，但是又无法通过搜索条件形成合适的扫描区间来减少需要扫描的记录数量时，则不考虑使用这个索引执行查询。

并不是所有的搜索条件都可以成为边界条件，比如这个查询语句：

```sql
SELECT * FROM single_table WHERE key1 < 'a' AND key3 > 'z' AND common_field = 'abc'; 
```

- 如果使用idx_key1执行查询，那么相应的扫描区间就是 $(-\infty, 'a')$ ，形成该扫描区间的边界条件就是 $key1 < 'a'$ 。而 $key3 > 'z'$ AND common_field = 'abc' 就是普通的搜索条件，这些普通的搜索条件需要在获取到idx_key1的二级索引记录后，再执行回表操作，在获取到完整的用户记录后才能去判断它们是否成立。  
- 如果使用idx_key3执行查询，那么相应的扫描区间就是 $(\mathbf{z}', + \infty)$ ，形成该扫描区间的边界条件就是 $\mathrm{key}3 > \mathrm{z}'$ 。而 $\mathrm{key1} < \mathrm{a}'$ AND common_field = 'abc' 就是普通的搜索条件，

这些普通的搜索条件需要在获取到idx_key3的二级索引记录后，再执行回表操作，在获取到完整的用户记录后才能去判断它们是否成立。

从上述描述中可以看到，在使用某个索引执行查询时，关键的问题就是通过搜索条件找出合适的扫描区间，然后再到对应的B+树中扫描索引列值在这些扫描区间的记录。对于每个扫描区间来说，仅需要通过B+树定位到该扫描区间中的第一条记录，然后就可以沿着记录所在的单向链表向后扫描，直到某条记录不符合形成该扫描区间的边界条件为止。其实对于B+树索引来说，只要索引列和常数使用 $= 、 < = >$ 、IN、NOT IN、IS NULL、IS NOT NULL、>、<、 BETWEEN、!=（也可以写成<>）或者LIKE操作符连接起来，就可以产生所谓的扫描区间。不过有下面几点需要注意。

- IN 操作符的语义与若干个等值匹配操作符（=）之间用 OR 连接起来的语义是一样的，都会产生多个单点扫描区间。比如下面这两个语句的语义效果是一样的：

```sql
SELECT \* FROM single_table WHERE key2 IN (1438, 6328); SELECT \* FROM single_table WHERE key2 = 1438 OR key2 = 6328; 
```

- $! =$ 产生的扫描区间比较有趣,也容易被大家忽略,比如:

SELECT $\star$ FROM single_table WHERE key1 $! = ^{\prime}$ a';

此时使用idx_key1执行查询时对应的扫描区间就是 $(-\infty ,a^{\prime})$ 和 $(a^{\prime}, + \infty)$

- LIKE 操作符比较特殊，只有在匹配完整的字符串或者匹配字符串前缀时才产生合适的扫描区间。

比较字符串的大小其实就相当于依次比较每个字符的大小。字符串的比较过程如下所示。

■ 先比较字符串的第一个字符；第一个字符小的那个字符串就比较小。  
如果两个字符串的第一个字符相同，再比较第二个字符；第二个字符比较小的那个字符串就比较小；  
如果两个字符串的前两个字符都相同，那就接着比较第三个字符；依此类推。

对于某个索引列来说，字符串前缀相同的记录在由记录组成的单向链表中肯定是相邻的。比如我们有一个搜索条件是key1LIKE‘a%’，对于二级索引idx_key1来说，所有字符串前缀为‘a'的二级索引记录肯定是相邻的。这也就意味着我们只要定位到key1值的字符串前缀为‘a'的第一条记录，就可以沿着记录所在的单向链表向后扫描，直到某条二级索引记录的字符串前缀不为‘a'为止，如图7-7所示。

很显然，key1 LIKE 'a%' 形成的扫描区间相当于 ['a', 'b')。

前面介绍的几个例子的搜索条件都比较简单，在使用某个索引执行查询时，我们可以很容易识别出对应的扫描区间，以及形成该扫描区间的边界条件。在日常的工作中，一个查询语句中的WHERE子句可能有很多个小的搜索条件，这些搜索条件使用AND或者OR操作符连接起来。虽然大家都知道这两个操作符的作用，但这里还是要再强调一遍。

- cond1 AND cond2：只有当 cond1 和 cond2 都为 TRUE 时，整个表达式才为 TRUE。  
- cond1 OR cond2: 只要 cond1 或者 cond2 中有一个为 TRUE, 整个表达式就为 TRUE。

在我们执行一个查询语句时，首先需要找出所有可用的索引以及使用它们时对应的扫描区间。下面我们来看一下怎么从包含若干个AND或OR的复杂搜索条件中提取出正确的扫描区间。

![](images/a7bdc37a932300ae99ff051151fc7f89741cb0c15bac140d93d65d97bb6e7b1b.jpg)  
图7-7定位key1值的字符串前缀为'a'时的示意图

# 1. 所有搜索条件都可以生成合适的扫描区间的情况

在使用某个索引执行查询时，有时每个小的搜索条件都可以生成一个合适的扫描区间来减少需要扫描的记录数量。比如下面这个查询语句：

```sql
SELECT * FROM single_table WHERE key2 > 100 AND key2 > 200; 
```

在使用uk_key2执行查询时，key $2 > 100$ 和 $\mathrm{key}2 > 200$ 这两个小的搜索条件都可以形成一个扫描区间。由于这两个小的搜索条件是使用AND操作符连接的，所以最终的扫描区间就是对这两个小的搜索条件形成的扫描区间取交集后的结果。取交集的过程如图7-8所示。

![](images/f5090686546744f5f3c7c918c19189c9e29fce13b90d246941b0f5aeb8a0cf3a.jpg)  
图7-8 根据搜索条件取区间交集

key2 > 100 和 key2 > 200 的交集当然就是 key2 > 200 了，也就是说上面这个查询语句使用 uk_key2 索引执行查询时对应的扫描区间就是 $(200, +\infty)$ ，形成该扫描区间的边界条件就是 key2 > 200。

我们再看一下使用OR操作符将多个搜索条件连接在一起的情况。来看下面这个查询语句：

```sql
SELECT \* FROM single_table WHERE key2 > 100 OR key2 > 200; 
```

OR意味着需要取各个扫描区间的并集。取并集的过程如图7-9所示

![](images/c3fa1942081c0d6bbab4b5ba95e421d3e6b6b1868db6e48799dfe12729dbc90a.jpg)  
图7-9 根据搜索条件取区间并集

也就是说上面这个查询语句在使用uk_key2索引执行查询时，对应的扫描区间就是（100， $+\infty$ ），形成扫描区间的条件就是 $\mathrm{key}2 > 100$ 。

# 2. 有的搜索条件不能生成合适的扫描区间的情况

在使用某个索引执行查询时，有时某个小的搜索条件不能生成合适的扫描区间来减少需要扫描的记录数量。比如下面这个查询语句：

```sql
SELECT * FROM single_table WHERE key2 > 100 AND common_field = 'abc'; 
```

在使用uk_key2执行查询时，很显然搜索条件key2>100可以形成扫描区间（100,+∞）。但是，由于uk_key2的二级索引记录并不按照common_field列进行排序（其实uk_key2二级索引记录中压根儿就不包含common_field列），所以仅凭搜索条件common_field='abc'并不能减少需要扫描的二级索引记录数量。也就是说此时该搜索条件生成的扫描区间其实就是（-∞,+∞）。由于key2>100和common_field='abc'这两个小的搜索条件是使用AND操作符连接起来的，所以对（100,+∞）和（-∞,+∞）这两个扫描区间取交集后得到的结果自然是（100,+∞）。也就是说在使用uk_key2执行上述查询时，最终对应的扫描区间就是（100,+∞），形成该扫描区间的条件就是key2>100。

其实，在使用uk_key2执行查询时，在寻找对应的扫描区间的过程中，搜索条件common_field = 'abc'没起到任何作用，我们可以直接把common_field = 'abc'搜索条件替换为TRUE（TRUE对应的扫描区间也是 $(-\infty, +\infty)$ ），如下所示：

```sql
SELECT * FROM single_table WHERE key2 > 100 AND TRUE;
```

在化简之后如下所示：

```sql
SELECT \* FROM single_table WHERE key2 > 100; 
```

也就是说上面那个查询语句在使用uk_key2执行查询时对应的扫描区间就是 $(100, +\infty)$ 。再来看一下使用OR操作符的情况。查询语句如下所示：

```sql
SELECT * FROM single_table WHERE key2 > 100 OR common_field = 'abc'; 
```

同理，我们把使用不到uk_key2索引的搜索条件替换为TRUE，如下所示：

```sql
SELECT \* FROM single_table WHERE key2 > 100 OR TRUE;
```

接着化简，结果如下所示：

SELECT $\star$ FROM single_table WHERE TRUE;

可见，如果强制使用uk_key2执行查询，对应的扫描区间就是 $(-\infty, +\infty)$ ，也就是需要扫描uk_key2的全部二级索引记录，并且对于获取到的每一条二级索引记录，都需要执行回表操作。这个代价肯定要比执行全表扫描的代价都大。在这种情况下，我们是不考虑使用uk_key2来执行查询的。

# 3. 从复杂的搜索条件中找出扫描区间

有些查询语句的搜索条件可能特别复杂，光是找出在使用某个索引执行查询时对应的扫描

区间就挺麻烦的。比如下面这个查询语句：

SELECT \* FROM single_table WHERE (key1 $>$ 'xyz' AND key2 $= 748$ ) OR (key1 $<$ 'abc' AND key1 $>$ 'lmn') OR (key1 LIKE '%suf' AND key1 $>$ 'zzz' AND (key2 $<  8000$ OR common_field $=$ 'abc'))

额滴个神！这个搜索条件简直绝了，不过大家不要被复杂的表象迷住了双眼，我们按下面的套路分析一下。

- 首先查看WHERE子句中的搜索条件都涉及了哪些列，以及我们为哪些列建立了索引。这个查询语句的搜索条件涉及了key1、key2、common_field这3个列，其中key1列有普通的二级索引idx_key1，key2列有唯一二级索引uk_key2。  
- 对于那些可能用到的索引，分析它们的扫描区间。

1. 假设使用idx_key1执行查询

我们需要把那些不能形成合适扫描区间的搜索条件暂时移除掉。移除方法也很简单，直接把它们替换为TRUE就好了。上面的查询中除了有关key2和common_field列的搜索条件不能形成合适的扫描区间外，key1LIKE $\%$ suf形成的扫描区间是 $(-\infty, +\infty)$ ，所以也需要将它替换为TRUE。把这些不能形成合适扫描区间的搜索条件替换为TRUE之后，搜索条件如下所示：

```txt
(key1 > 'xyz' AND TRUE) OR (key1 < 'abc' AND key1 > 'lmn') OR (TRUE AND key1 > 'zzz' AND 
```

对这个搜索条件进行化简，结果如下所示：

```txt
(key1 > 'xyz') OR (key1 < 'abc' AND key1 > 'lmn') OR (key1 > 'zzz') 
```

下面替换掉永远为TRUE或FALSE的条件。由于key1 $<  ^{\prime}$ abc'ANDkey1 $\rightharpoondown$ 'lmn'永远为FALSE，所以上面的搜索条件可以写成下面这样：

```lisp
(key1 > 'xyz') OR (key1 > 'zzz') 
```

继续化简。由于key1 > 'xyz'和key1 > 'zzz'之间是使用OR操作符连接起来的，这意味着要取并集，所以最终的化简结果就是key1 > 'xyz'。也就是说，最初的查询语句如果使用idx_key1索引执行查询，则对应的扫描区间就是 $(xyz', +\infty)$ 。也就是需要把满足key1 > 'xyz'条件的所有二级索引记录都取出来，针对获取到的每一条二级索引记录，都要用它的主键值再执行回表操作，在得到完整的用户记录之后再使用其他的搜索条件进行过滤。

假设使用uk_key2执行查询

我们需要把那些不能形成合适扫描区间的搜索条件暂时使用TRUE替换掉，其中有关key1和common_field的搜索条件都需要被替换掉，替换后的结果如下所示：

```txt
（TRUEANDkey2=748）OR（TRUEANDTRUE）OR（TRUEANDTRUEAND（key2<8000ORTRUE)）
```

哎呀呀！key2<8000ORTRUE的结果肯定是TRUE呀，也就是说化简之后的搜索条件成下面这样了：

```txt
key2 = 748 OR TRUE 
```

这个化简之后的结果就更简单了：

TRUE

这个结果也就意味着如果要使用uk_key2索引执行查询，则对应的扫描区间就是 $(- \infty, + \infty)$ ，也就是需要扫描uk_key2的全部二级索引记录，针对获取到的每一条二级索引记录还要进行回表操作。这不是得不偿失么！所以在这种情况下是不会使用uk_key2索引的。

![](images/d32e7c40c670dbfea45836dab7b42fedf17581e34e9e57bbd012053d2d229639.jpg)

在使用idx_key1执行上述查询时，搜索条件key1LIKE"%suf'比较特殊。虽然它不能作为形成扫描区间的边界条件，但是idx_key1的二级索引记录是包含key1列的。因此，我们可以先判断获取到的二级索引记录是否符合这个条件。如果符合再执行回表操作，如果不符合就不执行回表操作了。这样可能减少因回表操作而带来的性能损耗，这种优化方式称为索引条件下推。

# 4. 使用联合索引执行查询时对应的扫描区间

联合索引的索引列包含多个列，B+树中的每一层页面以及每个页面中的记录采用的排序规则较为复杂。以single_table表的idx_key_part联合索引为例，它采用的排序规则如下所示：

- 先按照 key_part1 列的值进行排序；  
- 在 key_part1 列的值相同的情况下，再按照 key_part2 列的值进行排序；  
- 在 key_part1 和 key_part2 列的值都相同的情况下，再按照 key_part3 列的值进行排序。我们来画一下 idx_key_part 索引的示意图，如图 7-10 所示。

![](images/127749f79eae1551ba213b6beba1a3a53efa1c8880f9d638afe7e42c5d689cb4.jpg)  
图7-10 idx_key_part索引的示意图

对于查询语句Q1来说：

Q1: SELECT * FROM single_table WHERE key_part1 = 'a';

由于二级索引记录是先按照 key_part1 列的值排序的，所以符合 key_part1 = 'a' 条件的所有记录肯定是相邻的。我们可以定位到符合 key_part1 = 'a' 条件的第一条记录，然后沿着记录所在的单向链表向后扫描（如果本页面中的记录扫描完了，就根据叶子节点的双向链表找到下一个页面中的第一条记录，继续沿着记录所在的单向链表向后扫描。我们之后就不强调叶子节点的双

向链表了），直到某条记录不符合key_part1 = 'a'条件为止（当然，对于获取到的每一条二级索引记录都要执行回表操作，这里就不展示了），如图7-11所示。

![](images/3d7c5ccfd7ed353319a3f1ba95d6c37d75d9bcf325b2a19c564d899b3de828c3.jpg)  
图7