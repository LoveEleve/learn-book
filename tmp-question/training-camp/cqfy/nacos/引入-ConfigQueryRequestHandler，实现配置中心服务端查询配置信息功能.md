  
引入 DumpService 数据导出器  
  
在上一章结尾留下了一个小尾巴，那就是定义一个定时任务，定时任务会定期数据库中存储的所有配置信息导入到服务端的缓存系统和文件系统，其实就是在每一次执行的时候，调用 ConfigCacheService 类的 dump() 方法。这个定时任务一旦定义完毕，并且成功启动，那么配置中心服务端存储客户端发布的配置信息的功能也就全部实现完毕了。但是在上一章我们并没有讨论出这个定时任务该在哪里定义，在什么时候启动，所以接下来，就让我们一起来看看这个定时任务的创建和启动时机。  
  
首先让我们来分析一下这个定时任务的启动时机，因为定时任务的启动时机分析完毕之后，定时任务的创建时机也就清楚了，只需要在启动定时任务之前创建该任务即可。按照 nacos 框架深度依赖 springboot 的特性，并且这个定时任务执行的操作又是把数据库中的配置信息导入到服务端的缓存系统和文件系统， 那这个定时任务其实就可以在 springboot 程序启动的过程中被创建来，然后提交给定时任务执行器。当然，真正执行应该在程序完全启动之后才执行，所以应该给这个定时任务定义一个延迟执行时间 。同时还要考虑到这个定时任务每一次执行的时候，都会查询数据库中的所有配置信息，然后倒入到服务端的缓存系统和文件系统，这个工程量是比较大的，所以要把定时任务的执行间隔时间定义的长一些。这些细节 nacos 源码都考虑到了，所以在 nacos 配置中心源码中，就把这个定时任务的执行间隔时间定义为了 6 小时，并且定义了值为 10 分钟的最小延迟执行时间。这也就是说， 当程序在 springboot 启动的过程中会把这个定时任务创建出来，然后把它提交给执行器，而执行器至少会在十分钟之后才开始执行这个定时任务，并且每一个执行的时间间隔为 6 个小时 。如果大家理解了这个逻辑，接下来我就要展示相关的代码了。  
  
在 nacos 配置中心源码中，定义了一个 DumpService 数据导出服务组件，这个 DumpService 类的对象就是专门用来把数据库中的信息导入到服务端的缓存系统和文件系统中的 。DumpService 是一个抽象类，在这个类下还有两个子类，一个是 ExternalDumpService 类，这个类的对象就是用来把外部数据源，也就是 mysql 数据库中的配置信息导入到务端的缓存系统和文件系统中的；另一个是 EmbeddedDumpService 类，这个类的对象就是用来把内部数据源，也就是 derby 数据库中的配置信息导入到务端的缓存系统和文件系统中的。 在我为大家提供的第十七版本代码中，只实现了 ExternalDumpService 类，毕竟我们使用的是 mysql 数据库存储客户端发布的配置信息 。接下来我就先为大家展示一下 ExternalDumpService 类的内容，请看下面代码块。  
package com.cqfy.nacos.config.server.service.dump;  
  
  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/10/28

\* @Description:外部数据源的配置信息导出服务组件，与此类相对应的就是使用了内部数据源，也就是derby数据库的配置信息导出服务组件，也就是EmbeddedDumpService类

\* 这不过在主线内容中我并没有为大家引入该类，实现支线内容的时候，就会为大家引入该类的。当然，只要大家ExternalDumpService和DumpService的内容掌握了

\* 那么EmbeddedDumpService类的内容也就很容易看懂了，方法逻辑几乎都没什么区别，只不过就是使用的存储配置信息的数据库不同而已

\*/

@ Conditional (ConditionOnExternalStorage.class)

@ Component

@ DependsOn ({ "rpcConfigChangeNotifier" })

public class ExternalDumpService extends DumpService {  
  
public ExternalDumpService (ConfigInfoPersistService configInfoPersistService,HistoryConfigInfoPersistService historyConfigInfoPersistService,ServerMemberManager memberManager) {

super (configInfoPersistService,historyConfigInfoPersistService,memberManager);

}  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/10/28

\* @Description:该方法会在当前类的对象被创建之后调用

\*/

@ PostConstruct

@ Override

protected void init () throws Throwable {

//调用父类的方法

dumpOperate (dumpAllProcessor);

}  
  
从上面代码块中可以看到，ExternalDumpService 类的对象也被 springboot 容器管理，并且在 springboot 创建完该对象之后，就会调用该对象的 init() 方法，在该方法中，就会调用父类，也就是 DumpService 类的 dumpOperate() 方法。现在我可以告诉大家： 在 DumpService 类的 dumpOperate() 方法中，就会把我们之前分析的那个定时任务创建出来，然后提交给定时任务执行器 ，与此同时也会创建一些其他的定时任务，比 如清除历史配置信息的定时任务；定期检查数据库中是否有配置发生变更，然后把变更的配置信息更新到服务端的缓存系统和文件系统中的定时任务等等 。这些定时任务我就不在文章中展开了，大家直接去我提供的第十七版本代码中查看即可，代码中注释非常详细，而且定时任务的逻辑非常简单，就是从数据库中查询配置信息，然后调用 ConfigCacheService 缓存组件的 dump() 方法，真的没什么可讲的。好了，话不多说，接下来就请大家看一看 DumpService 类的内容吧，请看下面代码块。  
好了，到此为止，我们就终于实现了配置中心服务端存储客户端发布的配置信息全功能。总的来说， 就是配置中心服务端会把客户度发布的配置信息存储在数据库中，而在服务端内部会启动一个定时任务，该定时任务会定期把数据库中的配置信息导入到服务端的缓存系统和本地文件系统中 。通过这些操作大家也能看出来， nacos 配置中心服务端其实是使用了缓存层、本地文件层、数据库层，三个层次来保存配置信息的 。大家可以再好好消化消化其中的原理和逻辑。  
  
引入 ConfigQueryRequestHandler  
  
在实现了配置中心服务端存储客户端发布的配置信息之后，接下来要就实现客户端从服务端查询指定配置信息的功能了。现在来看，这个功能实现起来就非常简单了。客户端向服务端查询指定配置信息时， 会向服务端发送一个 ConfigQueryRequest 请求，服务端会把客户端要查询的配置信息封装在 ConfigQueryResponse 响应中回复给客户端 。那我们就可以直接定一个专门处理 ConfigQueryRequest 请求的处理器，在该处理器中查询客户端指定的配置信息，当然， 现在我们就可以直接从本地文件中查询配置信息的具体内容了，而配置信息的 MD5 的值，以及密钥等信息都缓存在内存中，也可以直接从服务端内部得到 ，这些操作就都可以定义在 ConfigQueryRequest 请求处理器中，最后把查询到的数据封装在 ConfigQueryResponse 响应对象中返回给客户端即可。我把服务端专门处理 ConfigQueryRequest 请求的处理器定义为了 ConfigQueryRequestHandler，具体内容也已经实现完毕了，请看下面代码块。  
到此为止，客户端从服务端查询指定配置信息的功能也实现完毕了。接下来就该实现配置变更功能了，其实这么说也不太准确， 配置变更与否是客户端和 web 控制台的，只要客户端更新了服务端存储的配置信息，或者用户直接在 web 控制台修改了某个配置信息，那么服务端存储的配置信息肯定就会发生变更 ，这些功能我们都已经实现了，无非就是更新数据库中存储的配置信息而已。 所以说配置变更功能本身并不重要，重要的是配置变更之后要执行的各种操作：比如服务端发现有配置变更之后，可以通知客户端，让客户端知道有配置发生变更了；服务端还可以把最新的配置信息同步给集群中的其他节点 。这些功能是十八版本代码的内容，这里就不再展开讲解了。下一章我就会为大家实现这些功能。现在大家已经可以去看我提供的第十七版本代码了，好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/dcqt65x8o8ci3tv2*  
*All content belongs to its respective owners and creators.*