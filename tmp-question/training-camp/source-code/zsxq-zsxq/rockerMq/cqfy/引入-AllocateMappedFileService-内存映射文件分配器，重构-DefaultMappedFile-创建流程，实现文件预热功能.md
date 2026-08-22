  
引入 AllocateMappedFileService 内存映射文件分配器，重构 DefaultMappedFile 创建流程，实现文件预热功能  
  
为什么要实现 RocketMq 文件预热功能，这个我已经跟大家解释过了，所以就不再具体重复了，简单来说就是本地文件被映射到内存时，并没有给被映射的虚拟内存分配物理地址，这就导致只有当消息开始存储的时候才会为虚拟内存分配物理地址，这样一来在存储消息的时候程序就会频繁切换到内核态，影响程序执行效率。而我们解决问题的方法也很明确： 那就是在 DefaultMappedFile 对象创建完毕，本地文件创建完毕，本地文件被映射到用户进程的虚拟内存之后，就把物理地址给虚拟内存分配完毕 。这也就是所谓的文件预热功能。  
  
那这个功能该怎么实现呢？我的思路非常明确，我希望当 DefaultMappedFile 对象创建完毕之后，文件映射的虚拟内存就会被分配物理地址， 那么我不妨就在 DefaultMappedFile 对象创建完毕之后，立刻就像文件对应的内存映射缓冲区中写入数据，这不就是第一次访问内存吗？然后发现虚拟内存没有分配物理地址，接着触发缺页中断，给虚拟内存分配物理地址 ，这个逻辑可以理解吧？那该怎么写入数据呢？写入什么数据呢？如果你不知道写什么，就写 0 吧，那要怎么写入呢？别忘了存储数据的本地文件可以有 1GB 大小，难道要直接要向这 1GB 的文件写满数据？当然不需要这样， 我们已经清楚了 pagecache 是按照 4KB 一个缓存页来加载数据的，那么我们向内存映射缓冲区中写入数据的时候就可以每隔 4KB 写入一个 0 字节，在循环中把数据写完之后，这样一来那每 4KB 对应的物理页已经分配完毕了 ，这个逻辑也可以理解吧？如果理解了这个逻辑，接下来就可以编写具体的代码了， 我决定在 DefaultMappedFile 类中再定义一个 warmMappedFile() 方法 ，翻译过来就是文件预热的意思，请看下面代码块。  
package org.apache.rocketmq.store.logfile;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2025/6/29

\* @方法描述：内存映射文件对象

\*/

public class DefaultMappedFile implements MappedFile {  
  
protected static final Logger log \= LoggerFactory.getLogger (LoggerName.STORE\_LOGGER\_NAME);

//操作系统缓存页大小，默认是4KB

public static final int OS\_PAGE\_SIZE \= 1024 \* 4;  
  
//省略其他内容  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：对内存映射文件进行预热的方法，至于需要预热的原因，我已经在AllocateMappedFileService类的mmapOperation()方法中解释过了

\*/

@ Override

public void warmMappedFile (FlushDiskType type,int pages) {

//记录预热操作开始执行的时间

long beginTime \= System.currentTimeMillis ();

//创建内存映射缓冲区的切片，以便共享底层数据

ByteBuffer byteBuffer \= this.mappedByteBuffer.slice ();

//逐页遍历文件，每一页大小为4KB

for (long i \= 0,j \= 0;i < this.fileSize;i += DefaultMappedFile.OS\_PAGE\_SIZE,j ++) {

//向文件映射的每一页都写入一个0字节，以便触发缺页中断

//注意，上面循环的条件i += DefaultMappedFile.OS\_PAGE\_SIZE，表示每一次循环i就会增加一个缓存页的大小

好了，现在文件预热的功能实现完毕了，接下来要做的就是执行文件预热方法。大家可以看到，我被文件预热的方法定义在了 DefaultMappedFile 类中，这是因为我希望 DefaultMappedFile 对象一被创建，就可以立刻调用它的 warmMappedFile() 方法对文件进行预热。那么应该在哪里调用 DefaultMappedFile 对象的 warmMappedFile() 方法呢？这时候就要引入一个新的类，那就是 AllocateMappedFileService 类。  
  
让我来给大家详细解释一下，其实在 RocketMq 源码中，DefaultMappedFile 对象并不是直接由 MappedFileQueue 内存映射文件管理器创建的，因为除了文件预热，在源码中还对 DefaultMappedFile 对象的创建执行了另一个优化，那就是文件预分配， 所谓预分配就是在创建 DefaultMappedFile 对象的时候，会一次性创建两个，把当前需要的内存映射文件对象和接下来可能会用到的文件一起创建出来 。 这样一来再真正使用 DefaultMappedFile 对象的时候，就可以拿来即用。 而创建 DefaultMappedFile 对象的操作就在 AllocateMappedFileService 对象中执行，所以这个 AllocateMappedFileService 类翻译过来就是内存映射文件分配器的意思 ， 而 AllocateMappedFileService 对象把 DefaultMappedFile 对象创建完毕之后，就会立刻执行 DefaultMappedFile 的 warmMappedFile() 文件预热方法 。现在大家应该清楚我们要对 DefaultMappedFile 对象创建进行什么优化了吧？  
  
以上就是我们需要对 DefaultMappedFile 对象的创建执行的两个优化，核心内容只有这点，其他的就没什么可讲的了。至于 AllocateMappedFileService 类的内容，真的非常简单，没什么可分析的，我就大概讲讲它的运行流程吧。它其实就是一个后台线程，继承了 ServiceThread 类，在 AllocateMappedFileService 内部定义了一个 putRequestAndReturnMappedFile() 方法，该方法会专门接收 DefaultMappedFile 内存映射文件对象的创建路径，如果 MappedFileQueue 内存映射文件管理器要创建 DefaultMappedFile 对象，就会调用 AllocateMappedFileService 对象的 putRequestAndReturnMappedFile() 方法，然后把要创建的 DefaultMappedFile 对象的路径传递给该方法，就像下面代码块展示的这样，请看下面代码块。  
而当 AllocateMappedFileService 对象的 putRequestAndReturnMappedFile() 方法接收到要创建的两个内存映射文件对象的路径后，就会在该方法中把创建 DefaultMappedFile 对象的操作封装成一个 AllocateRequest 任务，然后把任务存放到内部队列中；因为 AllocateMappedFileService 分配器实际上是一个后台线程，那么它启动之后就会执行自己的 run() 方法，而在 run() 方法中就会从内部队列中循环获取 AllocateRequest 任务，处理任务，也就是创建 DefaultMappedFile 对象，创建完毕后调用 DefaultMappedFile 对象的文件预热方法即可。这就是 AllocateMappedFileService 内存映射文件分配器要执行的所有操作，我已经把 AllocateMappedFileService 类的代码编写完毕了，接下来就请大家阅读一下，请看下面代码块。  
上面代码块的内容虽然有点多，但是注释非常详细，我就不重复讲解了。现在大家可能还对 AllocateMappedFileService 类的创建以及启动时机感兴趣，这个也没什么可分析的，和 TransientStorePool 临时存储池一样， AllocateMappedFileService 对象也是在 DefaultMessageStore 的构造方法中被创建的，启动则是在 DefaultMessageStore 的 start() 方法中 ，请看下面代码块。  
到此为止，文件预热和文件与分配的功能就实现完毕了，AllocateMappedFileService 类的内容也展示完毕了。接下来就该回过头，看看我们之前提出的那三个问题了。  
  
重构 MappedFileQueue，为 MappedFileQueue 定义 flush()、commit() 方法  
  
在文章一开头我就为大家总结了三个问题，我把这几个问题搬运到下面了：  
1 DefaultMappedFile 对象的 flush() 方法的执行时机是什么时候？  
2 需要给 MappedFileQueue 也定义一个 flush() 方法，然后执行刷新数据到硬盘的操作吗？  
3 在 DefaultMappedFile 对象被创建的过程中，还有什么可以被优化的地方吗 ？  
现在我们已经把第三个问题解决了，看样子只剩下两个问题了，但实际上问题更多了，因为在我们实现内存读写分离功能时，对 DefaultMappedFile 类进行了重构，为该类新定义了一个 commit() 方法，就像我们想知道 DefaultMappedFile 对象的 flush() 方法的执行时机那样，我们也想知道 DefaultMappedFile 对象的 commit() 方法的执行时机；当然，如果我们不嫌麻烦，我们其实还可以进一步讨论，那就是需要给 MappedFileQueue 也定义一个 commit() 方法，然后执行数据提交到 pagecache 的操作吗？这样分析下来，缠绕我们的问题就变成了四个：  
1 DefaultMappedFile 对象的 flush() 方法的执行时机是什么时候？  
2 需要给 MappedFileQueue 也定义一个 flush() 方法，然后执行刷新数据到硬盘的操作吗？  
3 DefaultMappedFile 对象的 commit() 方法的执行时机是什么时候？  
4 需要给 MappedFileQueue 也定义一个 commit() 方法，然后执行数据提交到 pagecache 的操作吗 ？  
  
很好，不怕问题多，问题越多越刺激，解决它们越有成就感。那现在请大家想一下，就只看第二个问题的话，如果我们不需要给 MappedFileQueue 类定义 flush() 方法，那么数据刷盘的时候应该怎么做呢？是直接得到最新的内存映射文件对象，然后执行该对象的 flush() 方法把数据刷盘吗？这个问题涉及到数据刷新到本地文件的流程，本来数据是在内存映射缓冲区中存放着，当某个时刻程序执行了 DefaultMappedFile 对象的 flush() 方法后，数据就可以被刷新到本地文件中，这个情况大家都很清楚。但是，大家别忘记了，程序中可能存在多个内存映射文件，假如现在有这样一种情况：消息队列程序刚启动，还没有接收到任何消息数据，所以也就没有创建存储消息数据的本地文件，过了一会，生产者客户端开始向 Broker 节点发送消息了，于是 Broker 节点的存储引擎开始接收并存储消息，消息要存储在本地，根据预分配原则，一下子创建了两个本地文件，接下来生产者客户端生产的所有消息都存储到了本地文件中。按照我们的理解，消息肯定会先被存储到第一个本地文件中，等第一个存储满了，才会写入第二个本地文件中，当然，消息首先肯定是先被写入第一个本地文件对应的内存映射缓冲区中，然后再被写入到本地文件，而且从逻辑上来说，相对于消息被写入到内存映射缓冲区，消息被刷新到本地的操作肯定是延后的。也就是说可能已经有 100 个字节的消息被写入到内存映射缓冲区，但只有内存映射缓冲区的前 50 个字节被刷新到了本地文件中，这个逻辑可以理解吧？  
  
如果大家理解了这个逻辑，那请大家想一想，程序中也许会存在这样一种情况： 那就是第一个本地文件对应的内存映射缓冲区已经写满了，消息被存储到了第二个本地文件对应的内存映射缓冲区，但是第一个本地文件对应的内存映射缓冲区中的消息还没有被完全刷新到本地文件中，如果这个时候程序要执行数据刷新操作，直接得到最新的内存映射文件对象，也就是第二个本地文件对应的内存映射文件对象，开始刷新它的消息到本地硬盘，这样一来，不就出现了存储的消息不连贯的情况了吗？直接跳过第一个内存映射文件的数据，甚至会导致数据丢失啊 。这样分析下来，我们应该意识到， 在程序执行数据刷新操作时，不能直接得到最新的内存映射文件对象执行刷新操作，而是应该先找到数据刷新执行到哪个内存映射文件中了，然后得到该文件，继续对该文件执行数据刷新操作，以此保证数据存储的连续性 。那这个功能应该怎么实现呢？  
  
我的想法是定义一个全局的数据刷新计数器，用这个计数器来记录总共接收到的消息的刷新进度。比如说现在还是两个本地文件，每个文件只能存储 100 字节消息，那第一个文件就会被 0 字节命名，第二个文件就会被 100 命名，命名方式分别是它们存储数据在全局中的起始偏移量。假如 Broker 节点一共接收到 150 字节消息，那显然消息已经开始存放到第二个内存映射缓冲区中了，但是只有 50 个字节刷新到本地硬盘上了，这就意味着全局数据刷新计数器的值为 50，还有 100 个字节要刷新到硬盘中呢。而 50 偏移量显然在第一个内存映射文件范围中，所以执行刷新操作的时候要得到第一个内存映射文件对象，然后执行刷新数据操作。这个逻辑想必已经很清晰了吧？  
  
那么这个全局的数据刷新计数器该怎么定义呢？定义在哪里呢？DefaultMappedFile 中倒是定义了原子计数器用来记录本文件中数据刷新的进度，但这个进度是局部的，记录的只是本文件中数据的进度，比如说第二个内存映射文件存储了 100~199 的数据，现在这个文件内部的数据已经刷新到 55 了，这 55 只是文件内部数据的相对偏移量，并不是全局的，那全局的该怎么得到呢？其实很简单， 只要让文件存储数据的起始偏移量加上内部的局部偏移量，不就是全局的数据刷新偏移量吗 ？100 + 55 得到 155，这 155 就是总共接收到的消息已刷新数据的位置。而且有了这个全局数据刷新偏移量，我们就能很快定位到要从哪个文件继续刷新数据，定位文件的公式我也准备好了，请看下面代码块。  
现在大家应该清楚了这个全局数据刷新计数器是如何发挥作用的了，那这个全局数据刷新计数器应该定义在哪里呢？这个时候就不用猜了，大家应该也想到了， 肯定是定义在 MappedFileQueue 类中，因为 MappedFileQueue 内存映射文件管理器管理着每一个内存映射文件，也就意味着它可以得到每一个内存映射文件，如果让它来执行内存映射文件的刷新操作，在刷新完毕之后使用内存映射文件的起始偏移量加上本次刷新字节数，不就正好得到了本次刷新之后的全局数据刷新计数器的值了吗 ？那既然这样，我们就直接给 MappedFileQueue 类定义一个 flush() 方法，让该方法来管理并记录数据刷新的信息吧。饶了一圈之后，我们发现，确实应该给 MappedFileQueue 类定义一个 flush() 方法。这个逻辑大家应该清楚了吧？  
  
如果以上逻辑大家都清楚了，那大家可以自己思考一下是否应该给 MappedFileQueue 类再定义一个 commit() 方法，当然，肯定是要定义的，但为什么定义，大家可以思考思考，这个问题我就不再分析了，全部流程和刚才分析 flush() 方法一模一样，就留给大家自己梳理吧。接下来就让我把重构之后的 MappedFileQueue 类展示给大家，请看下面代码块。  
到此为止，我就把 MappedFileQueue 类重构完毕了，这样一来，上面的四个问题就还剩下两个没有解决了，那就是：  
1 DefaultMappedFile 对象的 flush() 方法的执行时机是什么时候？  
2 DefaultMappedFile 对象的 commit() 方法的执行时机是什么时候 ？  
这两个问题解决起来就有的说了，而且要从头说起了。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/re61g57gw69h7m4y*  
*All content belongs to its respective owners and creators.*