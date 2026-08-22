  
剖析 CommitLog 持久化数据的格式，重构 CommitLog 类  
  
请大家想一想，不管是执行 flush() 方法还是执行 commit() 方法，这两个方法所做的都是操作数据，flush() 方法会把内存映射缓冲区的数据刷新到硬盘，commit() 方法会把堆外内存的数据提交给 pagecache，这两个操作都有一个前提，那就是需要有真实的数据存放到缓冲区了。但现在的情况是什么，我们连 CommitLog 类都还没有真正实现呢，里面的方法都是伪方法，也就是说根本没有数据到来，CommitLog 根本就没有把数据存放到内存映射缓冲区或者是堆外内存，那 flush() 方法和 commit() 方法显然也就没有执行的必要。所以，要想分析 flush() 方法和 commit() 方法的调用时机，我们首先应该把 CommitLog 类重构了，让 CommitLog 能把数据提交到缓冲区。我们目前实现的 CommitLog 类是这样的，请看下面代码块。  
package org.apache.rocketmq.store;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/6/25

\* @方法描述：存储生产者客户端发送过来的消息的核心类，在第十三版本代码中，该类的内容非常简单，很多方法都没实现

\* 就算实现的也是伪方法

\*/

public class CommitLog implements Swappable {  
  
protected final DefaultMessageStore defaultMessageStore;  
//CommitLog文件的内存映射文件管理器

protected final MappedFileQueue mappedFileQueue;  
  
public CommitLog (final DefaultMessageStore messageStore) {  
//得到CommitLog文件的存储路径

String storePath \= messageStore.getMessageStoreConfig ().getStorePathCommitLog ();

//创建mappedFileQueue对象，其实是这样的，CommitLog只是一个文件夹，文件夹里有多个存储了消息的文件

//在内存映射技术下，每一个文件都会对应一个MappedFile文件映射对象，而mappedFileQueue对象则是用来管理这些MappedFile文件对象的

this.mappedFileQueue \= new MappedFileQueue (storePath,

//得到每一个MappedFile文件的默认大小

messageStore.getMessageStoreConfig ().getMappedFileSizeCommitLog (),

//使用内存映射文件分配组件创建mappedFile对象

messageStore.getAllocateMappedFileService ());

this.defaultMessageStore \= messageStore;

}  
public boolean load () {

return true;

这个 CommitLog 显然非常简陋，但没关系，这正给了我们一点点重构它的机会，最终把它重构成我们想要的样子，如果它已经被别人定义好了，调教好了，那么我们使用它的时候也没什么成就感。很好，那接下来应该做什么呢？不用考虑别的， 肯定是对接收到的消息执行编码操作，因为这些消息要被放入缓冲区中了，不管是堆外内存缓冲区还是内存映射缓冲区，都要对消息进行编码吧？这样一来就可以直接按照固定格式把消息刷新到本地硬盘 。也许大家对所谓的消息编码还不太清楚是什么意思，接下来然我给大家解释一下，所谓对消息编码就是在一块缓冲区中，消息按照什么格式存储，比如一块缓冲区最多可以存储 100 个字节，那么在存储消息的时候，前 4 个字节存储什么，后面 4 个字节存储什么，后面 8 个字节存储什么等等，按照预先定义好的格式把消息存放到缓冲区中就是所谓的消息编码。  
  
那么当 Broker 节点接收到生产者客户端发送过来的消息之后，要怎么对这条消息编码呢？这就没什么可分析的了，我就直接把 RocketMq 源码中的内容复制过来讲解了，在源码中定义了一个专门对消息进行编码的编码器对象，在编码器对象中定义了一个内存缓冲区，对消息进行编码的时候，会以下面的格式把消息写入到这个缓冲区对象中，请看下面代码块。  
以上就是消息存储时的编码格式，当然，有些内容现在还用不到，我在代码块中也跟大家写明了，现在可以先不会部分信息。如果大家理解了其他内容，那么接下来我就给大家展示一下这个所谓的消息编码器。在源码中把这个消息编码器定义为了 MessageExtEncoder 类，我把这个类的内容搬运到我们自己的程序中了，请看下面代码块。  
好了，现在消息编码器也定义完毕了，在 CommitLog 类的 asyncPutMessage() 方法中，接收到的消息正好被封装在一个 MessageExtBrokerInner 对象中，而编码器的 encode() 方法恰好就需要对 MessageExtBrokerInner 对象中的消息进行编码，并且编码完毕的信息存储在 MessageExtEncoder 对象的 ByteBuf 成员变量中了。那接下来就很好办了： 我们只需要在 CommitLog 类的 asyncPutMessage() 方法中得到 MessageExtEncoder 编码器，然后使用编码器对 MessageExtBrokerInner 中的消息进行编码，然后从 MessageExtEncoder 编码器中得到存放了编码完毕消息的缓冲区对象，接着再从内存映射文件管理器中得到最新的内存映射文件，最后把编码器缓冲区中的数据存放到内存映射文件对象的缓冲区中即可，不管是内存映射缓冲区还是堆外内存缓冲区，总之存放进去即可 。这个流程已经很清晰了吧？而这就是我们要重构的 CommitLog 类的 asyncPutMessage() 方法的逻辑，整个内容就这么点，是不是很简单？源码中就是这么简单。  
  
不过，我们还不能直接开始按照刚才的思路重构 CommitLog 类的 asyncPutMessage() 方法，因为还有一些很重要的问题需要我们讨论一下，请大家思考一下，假如有多个生产者客户端都向 Broker 节点发送了消息，这就意味着在 Broker 节点内部，可能会有多个线程并发处理消息，也就意味着 CommitLog 类的 asyncPutMessage() 方法会并发执行。那再并发执行这个方法的时候会遇到一个情况： 如果程序内部只有一个消息编码器，消息编码器内部只有一个缓存消息的缓冲区对象，那么在并发情况下，多个线程同时使用同一个消息编码器对消息进行编码，同时把数据写入同一个缓冲区对象，这么做显然会出现并发问题 。那该怎么解决这个问题呢？很简单，给每一个线程分配一个消息编码器不就行了？ 让每一个线程拥有自己的消息编码器，这样就算在并发情况下，每个线程使用的都是自己的消息编码器，肯定不会出现并发问题了 。这个时候就轮到 ThreadLocal 登场了，T hreadLocal 登场之后，就可以向每一个线程的私有 Map 中存储一个 PutMessageThreadLocal 对象，而通过 PutMessageThreadLocal 对象就可以得到一个消息编码器 。现在大家清楚了 MessageExtEncoder 的静态内部类 PutMessageThreadLocal 的作用了吧？  
  
除了我们刚才分析的消息编码并发问题之外，还有一个并发问题需要简单解释一下，那就是在把消息存放到内存映射文件的缓冲区、或者是堆外内存缓冲区时，需不需要加锁呢？答案是肯定的， 这个时候所有消息都会写入到同一个缓冲区中，肯定需要加锁，大家要注意这一点 。除了这个问题，还有另一问题需要我们讨论一下，那就是假如消息写入内存映射缓冲区、或者是堆外内存缓冲区失败了呢？这时候该怎么办呢？成功就成功了，这么什么好讨论的，那失败了应该执行什么操作呢？这时候就要看看是哪种失败情况了， 如果是当前要追加消息的缓冲区容量不够了，那就应该创建一个新的本地文件，然后映射到内存中，把消息追加到新的缓冲区中；如果是消息本身的问题，比如单条消息字节超过了规定限制，或者是什么未知的错误，那这个时候就没办法了，消息存储失败，要返回给客户端错误响应 。以上分析的这些内容也应该体现在 CommitLog 类的 asyncPutMessage() 方法中。这样分析下来之后，CommitLog 类的 asyncPutMessage() 方法应该被重构成下面这样，请看下面代码块。  
上面代码块的注释非常详细，我就不再重复讲解了。阅读完上面的代码块之后，大家一定对一行代码感到困惑，那就是：mappedFile.appendMessage(msg, this.appendMessageCallback, putMessageContext); 这行代码， 大家不清楚为什么还要把编码后的编码器缓冲区设置到 MessageExtBrokerInner 消息对象中？也不清楚这个 appendMessageCallback 成员变量是什么 ，对吧？appendMessageCallback 成员变量是什么很容易解释， 生产者客户端向 Broker 节点发送消息的时候，可能会发送单条消息，也可能会发送批量消息，既然是这样，那 Broker 节点在处理消息的时候也要考虑到这两种情况。具体到 CommitLog 类中，就定义了一个 AppendMessageCallback 成员变量，这个成员变量就是一个回调方法对象，在回调对象中定义了两个方法，一个是追加单条消息到缓冲区，一个是追加批量消息到缓冲区，具体调用哪个方法，就看追加的消息是单条还是批量了 。现在大家应该清楚这个 AppendMessageCallback 成员变量的作用了吧？  
  
那么执行了 mappedFile.appendMessage(msg, this.appendMessageCallback, putMessageContext) 这行代码之后，在内存映射文件内部会执行什么操作呢？  
  
实现消息追加到内存映射缓冲区功能  
  
我们之前实现的 DefaultMappedFile 类的 appendMessage() 方法是这样追加消息到缓冲区的，请看下面代码块。  
现在我们知道要区分单条消息和批量消息，使用 AppendMessageCallback 对象来追加消息到缓冲区中，那就要对 DefaultMappedFile 类的 appendMessage() 方法进行重构，可以重构成下面这样，请看下面代码块。  
可以看到，在上面代码块的 appendMessagesInner() 方法中，会根据要处理的消息是单条还是批量来执行追加操作，而执行的方法就是 AppendMessageCallback 对象的 doAppend() 方法，但到现在我为我们还不知道这个 AppendMessageCallback 对象的具体内容，接下来我就为大家展示一下这个 AppendMessageCallback 类的具体内容。实际上 AppendMessageCallback 就是 CommitLog 的一个内部类，具体内容请看下面代码块。  
AppendMessageCallback 内部类的具体内容我就不为大家分析了，上面代码中注释非常详细，我就不为大家详细解释了。到此为止本篇文章的所有内容就全部讲解完毕了，这一篇文章的内容非常多，实现了临时存储池功能，引入了内存映射文件分配器，实现了内存映射文件预热功能以及预分配功能，对 DefaultMappedFile 类和 MappedFileQueue 类进行了大量重构了，最终实现了把消息追加到内存映射文件缓冲区、或者是堆外内存的功能。正因为实现的功能非常多，所以我不得不把这篇文章拆分成好几章，在没拆分之前，完整的文章已经达到了四万多字。大家可以耐心仔细地阅读这几篇文章，把所有内容都掌握了，再去阅读下一篇文章，下一篇文章就要实现刷新数据到本地硬盘的操作，遗留地两个问题，也就是下面这两个问题就会得到解决：  
1 DefaultMappedFile 对象的 flush() 方法的执行时机是什么时候？  
2 DefaultMappedFile 对象的 commit() 方法的执行时机是什么时候 ？  
  
当然，现在大家去阅读第十四版本代码内容还是有点早，因为有些内容我还没有在文章中讲解实现。不过大家可以结合文章阅读第十四版本代码的部分内容，再结合代码注释，肯定能把我们讲过的内容都掌握了。至于第十四版本代码中的各种 load() 方法，我建议大家先不要关注，因为这些方法还不完整，大家真的可以直接忽略。在迭代第十四版本代码时，我采用的是先存储后加载的方式来迭代的，也就是先实现存储消息的功能，等大家都熟悉各个类，各个成员变量的作用了，再回过头实现宕机数据恢复，也就是初始化各个存储组件，执行 load() 方法的功能；当然，这些功能肯定是在后面版本代码才实现了。第十四版本代码只是把消息追加到缓冲区功能和消息刷新到硬盘功能实现了。好了朋友们，这一章就到此为止吧，下一章我们就一起实现消息刷新硬盘功能，朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/hv47esu6syodcg99*  
*All content belongs to its respective owners and creators.*