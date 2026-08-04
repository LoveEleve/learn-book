  
引入 PushExecuteTask 推送任务  
  
话不多说，让我们直接开始本章的内容。在上一章我为大家详细剖析了 nacos 服务端的执行引擎体系，并且也引入了处理延迟推送任务的 PushDelayTaskExecuteEngine 引擎，我们也知道了，这个延迟推送任务执行引擎并不会直接处理延迟推送任务，而是 合并相同的任务，等待任务度过限时等待时间，然后把任务提交给可以并发处理所有任务的新的执行引擎，也就是立即执行任务的引擎 。也就是说，上 一章我们一直没有实现的 PushDelayTaskExecuteEngine 引擎内部的 PushDelayTaskProcessor 处理器，应该执行的就是把任务提交给立即执行任务的引擎的操作 。PushDelayTaskExecuteEngine 类的内容如下，请看下面代码块。  
package com.cqfy.nacos.naming.push.v2.task;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/22

\* @方法描述：延迟推送任务执行引擎

\*/

public class PushDelayTaskExecuteEngine extends NacosDelayTaskExecuteEngine {  
//构造方法

public PushDelayTaskExecuteEngine () {  
//在创建PushDelayTaskExecuteEngine对象的时候，就创建了一个延迟推送任务处理器对象

//并且把这个处理器对象设置到当前类对象的顶级抽象父类AbstractNacosTaskExecuteEngine中了

setDefaultTaskProcessor (new PushDelayTaskProcessor (this));

}  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @date:2024/7/22

\* @方法描述：延迟推送任务处理器

\*/

private static class PushDelayTaskProcessor implements NacosTaskProcessor {  
private final PushDelayTaskExecuteEngine executeEngine;  
public PushDelayTaskProcessor (PushDelayTaskExecuteEngine executeEngine) {

this.executeEngine \= executeEngine;

}  
//处理延迟推送任务的方法

通过上面代码块可以看到，我们还是没有实现 PushDelayTaskProcessor 处理器的 process() 方法，虽然我们知道最终任务是要被新的执行引擎执行，PushDelayTaskProcessor 处理器的 process() 方法要做的就是把延迟推送任务提交给新的执行引擎，这些操作似乎还挺容易实现的，无非就是把新的执行引擎定义出来，然后把任务提交到新引擎的任务队列中就行了。但是请大家思考一个问题： 那就提交给新执行引擎的究竟是什么任务呢？是直接从 PushDelayTaskExecuteEngine 任务队列中取出来的延迟推送任务吗 ？  
  
我请大家思考这个问题的原因很简单，因为任务一旦被提交给新的执行引擎，也就是立即执行任务的引擎，这就意味着任务要被执行了。而执行任务的那一套模式我们都已经很熟悉了，无非就是线程执行 Runnable 任务，可是我们目前定义的 延迟推送任务根本不是一个 Runnable 啊。我把 PushDelayTask 类给大家搬运到这里了，请看下面代码块。  
从上面代码块可以看到，PushDelayTask 类的对象根本就不是 Runnable 任务，没办法被执行器执行，在 PushDelayTask 类中我们也看不到有什么 run 方法。所以， 如果我们直接把 PushDelayTask 延迟推送任务提交给新的执行引擎，也就是立即执行任务的引擎，新的执行引擎根本没办法执行这些任务 。这一点大家应该可以理解吧？那这时候该怎么办呢？不能直接提交延时推送任务了，那要提交什么任务给新的执行引擎呢？  
  
其实进行到这里大家应该也能意识到了， 实际上完全没必要再把 PushDelayTask 延迟推送任务提交给新的执行引擎来执行。因为该任务最主要的功能就是用来合并相同的延迟推送任务，任务合并完成之后，就不需要再使用 PushDelayTask 任务来封装把服务实例信息推送给客户端的操作了 。我们最终的目标就是要让 nacos 服务端把最新的服务实例信息主动推送给订阅了对应 Service 服务的客户端，在这个过程中， 我们需要根据延迟推送任务中封装的 Service 信息找出订阅了该服务的所有客户端，还要更新 ServiceStorage 中缓存的服务实例信息，然后从 ServiceStorage 中得到最新的服务实例信息，把这些信息推送给对应的客户端 。这些流程在前面的章节我就为大家总结完毕了，还记得我为大家总结的那四个操作吧？第一个操作已经实现完毕了，还剩下三个操作待实现， 现在我们要做的显然就是再定义一个新的任务对象，这个新的任务对象就是一个 Runnable，然后把剩下的三个操作定义在这个新任务对象的 run 方法中，我们只需要把这个新的任务对象提交给新的执行引擎即可 。这样一来，新的执行引擎就会执行新的任务，把最新的服务实例信息推送给对应的客户端。  
  
比如说我们就把这个新的任务定义为 PushExecuteTask，这个任务实现了 Runnable 接口，可以直接被线程执行 。在这个任务的 run 方法中，我们要执行的操作有三个：  
1 更新 ServiceStorage 服务信息存储器中的服务实例信息，然后从 ServiceStorage 中得到发生变更的 Service 下的最新的服务实例信息。  
2 根据 Service 找出订阅了该服务的所有客户端。  
3 把最新的服务实例信息推送给所有客户端。  
以上就是 PushExecuteTask 任务要执行的操作，接下来就让我们根据刚才的分析，把 PushExecuteTask 类定义出来，请看下面代码块。  
到此为止，我就为大家把 PushExecuteTask 任务定义完毕了，也把 nacos 服务端向客户端推送最新服务实例信息的核心操作定义完成了。因为上面代码块中的注释非常详细，而且在展示代码块之前我也给大家把功能的设计思路分析得十分详细了，所以就不再为大家重复解释上面代码块的逻辑了。总之，现在我们就剩下最后一个问题没有解决： 那就是新定义的这个 PushExecuteTask 任务该怎么被执行呢 ？很简单， 当然就是交给新引入的可以立即执行任务的引擎去执行，也就是我上一章为大家引入的 NacosExecuteTaskExecuteEngine 这个执行引擎 。虽然这个 NacosExecuteTaskExecuteEngine 引擎的具体内容还没有跟大家展示，但是大家现在肯定都清楚这个引擎的具体作用是什么了。这一点理解了之后，那我们就可以再思考思考这个 PushExecuteTask 任务怎么被创建，又怎么提交给新引入的执行引擎呢？ 这时候就可以把 PushDelayTaskExecuteEngine 延迟推送任务执行引擎的 PushDelayTaskProcessor 处理器的 process() 方法实现一下了，因为创建 PushExecuteTask 任务，并且把任务提交给新引入的执行引擎的操作就是在 process() 方法中执行的 。  
  
引入 NacosExecuteTaskExecuteEngine 执行引擎  
  
接下来我就先为大家展示一下实现之后的 PushDelayTaskExecuteEngine 延迟推送任务执行引擎的 PushDelayTaskProcessor 处理器的 process() 方法，请看下面代码块。  
在上面代码块中，我们就可以看到确实是在 PushDelayTaskProcessor 处理器的 process() 方法中创建了真正要被执行的 PushExecuteTask 任务，这一点大家肯定再清楚不过了。但是这个 PushExecuteTask 任务不是要提交给新引入的 NacosExecuteTaskExecuteEngine 立即执行任务的引擎吗？在上面代码块中并没有体现出这一点。而是执行了 NamingExecuteTaskDispatcher.getInstance().dispatchAndExecuteTask() 这行代码，大家别急， 实际上这行代码就是把创建的 PushExecuteTask 任务分发给 NacosExecuteTaskExecuteEngine 引擎了 。我把 NamingExecuteTaskDispatcher 类的内容展示一下大家就立刻清楚了，请看下面代码块。  
NamingExecuteTaskDispatcher 类的内容展示了之后，最后，也轮到展示这个 NacosExecuteTaskExecuteEngine 引擎了，这个引擎就是用来真正执行 PushExecuteTask 任务的，把最新的服务实例信息推送给订阅了对应的 Service 的客户端。这个 NacosExecuteTaskExecuteEngine 引擎就和我们之前看到过的各种执行器的常规组件一样了，举一个最直接的例子，n etty 中的 EventLoopGroup 大家都还记得吧？EventLoopGroup 管理着一个单线程执行器数组，每一个单线程执行器就是用来真正执行任务的。NacosExecuteTaskExecuteEngine 执行引擎的结构和 EventLoopGroup 类似，它也管理者一个单线程执行器数组，所谓单线程执行器就是 TaskExecuteWorker 对象，TaskExecuteWorker 类的内容我也会为大家展示出来 ，这些内容都很简单，我把代码一展示大家就全明白了。这些组件大家应该都见过很多次了，是最常见的执行器组件，我在过去的课程中也给大家展示过类似的组件，所以就不再重复讲解 NacosExecuteTaskExecuteEngine 执行引擎了，大家自己看看代码就行。  
  
首先是 NacosExecuteTaskExecuteEngine 的内容，请看下面代码块。  
接下来是 TaskExecuteWorker 单线程执行器的内容，请看下面代码块。  
到此为止，我就为大家把 nacos 服务端主动推送最新服务实例信息给客户端的功能全部实现了，现在大家已经可以阅读我提供的第九版本代码的全部内容了。在阅读代码的过程中，大家可以对比一下我在文章中实现的功能，展示的代码，想对于我提供的第九版本代码在哪些地方做了简化，以及没有在文章中全部展示的组件究竟是什么样的。这一章的内容非常简单，大家可以当成一次休息站，下一章我们就要正式开始搭建 nacos 服务端集群了，为框架引入 Distro 集群。  
  
当然，我还是那句话，不管课程进度推进到哪里，整个 nacos 框架可以说是一点难度都没有，就算来到集群模块了，无非就是要实现的功能繁琐一些，要定义的操作也丰富一些，其他的也就没有什么了。如果有朋友一篇篇文章看到这里，之前的版本代码也都看完了，你们可以问一问自己，学到这里难道有哪一块的知识真的很难吗？我想大家应该都是平推过来的。所有代码没有一点技巧可言、没有一点新意可言，都是最普通的代码；很多类都是加一个 springboot 注释，被 springboot 容器管理，然后启动定时任务执行器执行任务，需要提高效率就使用线程池，搞来搞去，一直都是这一套。所以，继续跟下去就完事了。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/flvg4pommw36r5mr*  
*All content belongs to its respective owners and creators.*