上一章我们已经初步实现了 DistroClientDataProcessor 这个订阅者，并且在经过一番分析之后，我们最终要实现该类的 syncToAllServer() 方法了，而该方法的具体逻辑也分析得很清楚了， 那就是在该方法中把得到的 Client 客户端对象的信息收集一下，然后把收集到的信息封装到延迟执行任务中，再把延迟执行任务提交给延迟任务执行器 。这个逻辑大家应该都还有印象，或者说应该十分清楚。我把上一章最后重构的 DistroClientDataProcessor 类搬运过来了，请看下面代码块。  
package com.cqfy.nacos.naming.consistency.ephemeral.distro.v2;  
  
  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/8/25

\*/  
  
//表示当前处理器要处理的数据类型，也就是Client客户端对象存储的数据

//v2就是v2版本的意思，v1版本是旧版本nacos使用的版本

public static final String TYPE \= "Nacos:Naming:v2:ClientData";  
  
//构造方法

public DistroClientDataProcessor () {

//别忘了当前处理器本身也是一个订阅者，在这里把订阅者注册到事件通知中心

}  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/8/25

\* @Description:当前订阅者关注的事件

\*/

@ Override

List < Class <? extends Event >> result \= new LinkedList <> ();

//目前这个订阅者只关注客户端变更事件

result.add (ClientEvent.ClientChangedEvent.class);

return result;

当然，这个 DistroClientDataProcessor 类还没有最终重构完毕，DistroClientDataProcessor 客户端数据处理器的 syncToAllServer() 方法还是没有实现完毕，因为 Distro 集群的延迟执行任务和延迟任务执行器我们都还没有定义，要先把这两个重要组件定义完毕，才能继续实现 DistroClientDataProcessor 客户端数据处理器的 syncToAllServer() 方法。上一章的内容就进行到这里了，所以这一章，就让我来为大家实现这一部分的功能，也就是定义 Distro 集群要使用的延迟任务和延迟任务执行器。  
  
引入 DistroDelayTask、DistroDelayTaskExecuteEngine 类  
  
很好，既然现在要定义 Distro 延迟任务和延迟任务执行引擎了，那我们要做的事就很明确了，直接借鉴 nacos 服务端主动推送最新服务实例信息给客户端的功能即可。在实现 nacos 服务端订阅推送功能的时候，我们已经知道了， 所谓的延迟任务的作用就是把目的相同的两个延迟任务合并一下，所谓目的相同也就是说这两个延迟任务要同步的数据属于同一个 Client 客户端对象，这一点在上一章已经解释过了；而延迟任务执行器的作用其实就是等待延迟任务的延迟执行时间结束，然后把任务提交给立即执行任务的执行引擎去执行即可 。这些逻辑大家都理解了之后，我们直接模仿 nacos 服务端订阅推送功能的实现逻辑来实现 Distro 集群备份数据的功能即可。  
  
首先我就为大家把 Distro 集群要使用的延迟任务定义出来， 我就把这个延迟任务定义为 DistroDelayTask 类，并且在这个类中定义一个最核心的 merge() 方法，这个方法就是用来将两个目的相同的延迟任务合并到一起的方法 。接下来就应该为大家具体展示这个 DistroDelayTask 类了，确实，这个类确实被我实现完毕了，我应该马上为大家展示出来，但是在展示出来之前，我还有一个问题需要跟大家探讨。那就是请大家思考一下： 判断两个延迟任务是否可以合并的条件究竟有哪些呢 ？之前我们已经分析了一个条件了，那就是在 Distro 集群的某个节点中，如果这个节点创建了两个延迟执行任务，这两个延迟执行任务要备份给其他节点的数据都在这个节点内部的同一个 Client 客户端对象中，可能这个 Client 客户端对象内部的服务实例信息变更比较频繁，短时间内变更了两次，所以创建了两个延迟执行任务，这时候我们就认为这两个任务可以合并，这个逻辑想必大家都可以理解了；那么请大家想一想，当我们想合并两个延迟执行任务时，只需关心要备份的数据是否属于同一个 Client 客户端对象就够了吗？显然不是这样的， 假如这两个延迟任务要把数据备份给 Distro 集群中不同的节点，这个时候显然就不能直接合并这两个延迟执行任务 。这一点大家应该也可以理解，如果这样的两个任务合并了，那么合并之后的任务究竟要把数据备份给集群中的哪个节点呢？所以判断两个延迟执行任务是否可以合并，应该判断两个重要条件：  
1 两个延迟执行任务要备份给其他节点的数据是否都在这个节点内部的同一个 Client 客户端对象中。  
2 两个延迟任务是否要把数据备份给 Distro 集群中的同一个节点。  
只要以上两个条件全部满足，那么 Distro 集群中的两个延时任务就可以合并。  
  
以上的逻辑理解了，这时候大家肯定好奇在合并任务的时候，该怎么判断两个任务操作是不是同一个 Client 客户端下的数据，以及这两个任务要备份的目标节点是不是同一个呢？这时候其实就轮到我在上一章为大家展示的 DistroKey 登场了，请看下面代码块。  
可以看到，在上面代码块中，DistroKey 对象封装了要备份的数据所在的 Client 客户端信息，还封装了数据要备份的目标节点。所以， 在合并两个延迟任务的时候，只需要判断这两个延时任务持有的 DistroKey 对象的 hashCode 是否相同，就可以决定是否执行进一步的合并操作了 。好了，这些逻辑大家都理解了，接下来我就可以为大家展示早就定义完毕的 Distro 集群要使用的 DistroDelayTask 延迟任务了。请看下面代码块。  
如果大家还记得 nacos 服务端订阅推送功能用到的 PushDelayTask 类的内容的话，大家就会发现我为大家展示的 DistroDelayTask 类的内容和 PushDelayTask 类的内容没有什么大的区别， 都定义了任务被创建的时间，以及任务被处理的间隔时间，还有最重要的合并两个延时任务的 merge() 方法 。当然，我为大家展示的 DistroDelayTask 类中的 merge() 方法还没有实现，因为还有一个小细节需要为大家补充，补充完毕之后，大家才能明白两个 DistroDelayTask 延迟任务该怎么合并。也就是说，我们现在面临的问题就是： 两个 DistroDelayTask 延迟任务应该如何合并呢 ？  
  
要理清楚这个问题， 那就要再为 Distro 集群备份数据引入一个概念，那就是处理数据的操作符，所谓操作符就是要对备份的数据执行什么操作的意思 。这个操作符理解起来其实非常简单，请大家想一想，我们一直说 Distro 集群的某个节点要把最新的服务实例信息备份给集群的其他节点，这里所说的备份其实是一个很宽泛的操作，服务实例新增了需要备份，服务实例注销了也需要备份，这两个操作大家肯定都能理解。而不管服务实例是新增了还是注销了，这都属于服务实例发生了变更的情况；那除此之外，Distro 集群中还可以执行什么备份数据的操作呢？也许大家一时间想不出来，但我只要简单提示一下，大家就立刻明白了， 比如 Distro 集群的节点还可以执行备份快照的操作，假如有一个节点新加入到 Distro 集群中了，这个时候肯定从其他节点拉取最新的快照，就能使自己尽快和其他节点的数据完成同步，所以说快照操作也是 Distro 集群中可以执行的一个操作 。还有一种情况， 那就是集群中某个节点管理的某个客户端连接忽然断开了，那么这个节点为连接断开的 nacos 客户端创建的 Client 对象也要从内部清除了，这个时候，当前节点是不是还要通知集群中的其他节点，让其他节点把备份过去的 Client 客户端对象也从自己内部给清楚了呢 ？大家可以仔细想想这种情况，当然，还有一些其他的情况我就不一一例举了，接下来我就直接为大家把 Distro 集群中要用到的操作符定义一下，然后展示给大家。这个操作符显然要定义成枚举对象，具体实现如下，请看下面代码块。  
好了，现在 Distro 集群要使用的操作符定义完毕了，接下来我们就该回归主线，思考一下 DistroDelayTask 延时任务该怎么合并了。请大家再跟着我的思路想一想： 假如程序先创建了一个延迟任务，这个任务执行的是 CHANGE 操作，也就是节点内部的某个 Client 客户端对象中新增了服务实例信息，或者注销了服务实例信息，现在要把最新的服务实例信息同步给集群的其他节点了，这个就是 CHANGE 操作符代表的情况 ； 过了没几十毫秒，当前节点发现自己管理的 Client 客户端对象对应的客户端连接断开了，并且客户端管理器使用 NotifyCenter 事件通知中心发布了一个 ClientDisconnectEvent 客户端连接断开事件，这就意味着当前节点要从客户端管理器中清除连接断开的 Client 对象，与此同时也要通知集群中的其他节点，让其他节点把备份过去的对应的 Client 客户端对象也清除了。这就是 DELETE 操作符代表的情况，这个时候也会创建一个延迟任务来执行这个操作 。这这两种情况下，创建的两个延迟任务处理的数据都是同一个 Client 客户端对象下的，也要备份给集群中的同一个节点，那么这两个任务就可以合并，那现在问题来了， 在合并的时候操作符该怎么处理呢 ？两个任务的操作符可并不相同啊。  
  
也许有些朋友会觉得这个问题比较棘手，这很正常，因为这个问题本身就带有迷惑性，其实它非常容易解决。大家只要理清楚一点：我们要做的就是把最新的服务实例信息备份给集群的其他节点，所以不管对这些数据执行了什么操作，只要使用最新的操作符就行了。比如说上面的情况， 先创建的延迟任务要备份服务实例变更情况给集群的其他节点，后创建的延迟任务要把客户端断连事件备份给集群的其他节点。这种情况肯定使用后创建的延迟任务的操作符，因为客户端都不存在了，还备份什么数据呢 ？这个情况大家可以理解吧？所以这个时候，我就可以为大家展示重构之后的 DistroDelayTask 类了，请看下面代码块。  
上面代码块中的注释非常详细，所以我就不再重复讲解其中的逻辑了，而且 merge() 方法的逻辑非常简单，大家自己看看就行。好了，现在 DistroDelayTask 延迟任务定义完毕了，接下来就该定义延迟任务执行器了。 这个执行器我就不再仔细分析了，因为它非常简单，主要功能都定义在了它的父类，也就是 NacosDelayTaskExecuteEngine 中了 ，具体的类结构我也搬运过来了，帮助大家再回顾一下。请看下面简图。  
在实现 nacos 服务端主动推送客户端最新服务实例功能时，我已经把 NacosDelayTaskExecuteEngine 类完全展示给大家了，大家应该都完全掌握了，所以接下来我就直接展示 Distro 集群要使用的延迟任务执行器了，也就是 DistroDelayTaskExecuteEngine 类。请看下面代码块。  
从上面代码块的第 165 行，也就是 processTasks() 方法中可以看到， Distro 集群的 DistroDelayTaskExecuteEngine 延迟任务执行引擎在处理 DistroDelayTask 延迟任务时，需要先得到 DistroDelayTask 延迟任务处理器，然后再用延迟任务处理器处理任务即可。而 Distro 集群的延迟任务处理器处理任务的逻辑肯定就是把任务重新包装一下，包装成一个可以立刻执行的任务，然后把这个任务提交给可以立即执行任务的引擎即可 。这些逻辑我们在实现 nacos 服务端主动推送最新服务实例信息给客户端功能时都已经讲解过了，Distro 集群处理延迟任务也是一样的逻辑，我就不再仔细分析了。所以，现在我们要解决的问题有三个：  
1 把处理 DistroDelayTask 延迟任务的处理器定义出来。  
2 把这个处理器交给 DistroDelayTaskExecuteEngine 延迟任务执行引擎使用。  
3 定义 Distro 集群的立即执行任务的执行引擎 。  
如果大家能跟得上我的思路的话，应该很容易就能理解本章内容是如何推进到这里的。接下来我们只要解决了以上三个问题，就可以让 Distro 集群的立即执行任务的执行引擎执行任务，把数据备份给 Distro 集群的其它节点。看到这里，大家可能以为 Distro 集群备份数据功能的实现马上就要迎来尾声了。也确实是这样，因为这三个问题都很简单，就比如说第三个问题吧，定义 Distro 集群要使用的立即执行任务的执行引擎。这个引擎非常容易定义，确切地说，几乎不用定义。 如果大家掌握了 nacos 服务端主动推送最新服务实例信息给客户端功能的实现逻辑，那大家肯定就还记得一个可以立即执行任务的执行引擎，那就是 NacosExecuteTaskExecuteEngine 任务执行引擎 。在 nacos 服务端主动推送最新服务实例信息给客户端的过程中，就用到了这个任务引擎，这个引擎的代码我就不详细展示给大家了，如果大家没有印象了，可以直接去我提供的第 11 版本代码中查看。  
  
好了，我把 NacosExecuteTaskExecuteEngine 这个任务执行引擎引出来之后，接下来就可以定义 Distro 集群要使用的可以立即执行任务的引擎了， 这个引擎就被我定义为了 DistroExecuteTaskExecuteEngine 类，并且，这个类就是 NacosExecuteTaskExecuteEngine 类的子类 ，具体内容如下，请看下面代码块。  
可以看到， DistroExecuteTaskExecuteEngine 类其实完全沿用了 NacosExecuteTaskExecuteEngine 类中定义的所有方法和功能 。现在大家应该也清楚了我刚才说的，实际上 Distro 集群要使用的可以立即执行任务的引擎根本不必定义是什么意思了吧？ 因为它本来使用的就是我们之前早就实现了的 NacosExecuteTaskExecuteEngine 任务引擎。只不过为了在功能模块上区分得更清楚，所以又给 Distro 集群定义了一个 DistroExecuteTaskExecuteEngine 类 ，表示这个执行引擎是 Distro 集群要使用的。好了，刚才提出的三个问题，目前只剩下两个还没有解决了。  
  
这时候大家应该也相信我之前说的话了吧，要不了多久 Distro 集群备份数据的功能就会被完全实现。但我现在还不想进行得那么快，因为现在还有一些细节问题没有处理，而且最重要的是，我们上一章引入的 DistroClientDataProcessor 客户端数据处理器一直没有重构完毕。之前无法重构是因为 Distro 集群延迟任务和延时任务执行引擎没有定义，现在这两个类也定义完毕了，那么是时候先把 DistroClientDataProcessor 类重构一下了。  
  
重构 DistroClientDataProcessor 类  
  
随着 Distro 集群的延迟任务和延迟任务执行引擎的引入，重构 DistroClientDataProcessor 类的工作就变得非常简单了。当然，考虑到在上一小节我们还引入了 Distro 集群可以执行的操作符，还应该围绕着操作符来重构 DistroClientDataProcessor 类。就比如说，在上一小节我跟大家讲解了， 当 nacos 服务端的 NotifyCenter 事件通知中心发布了一个 ClientDisconnectEvent 客户端连接断开事件，这就意味着当前节点要从客户端管理器中清除连接断开的 Client 对象，与此同时也要通知集群中的其他节点，让其他节点把备份过去的对应的 Client 客户端对象也清除了 。这个逻辑大家已经非常清楚了，那该怎么实现呢？这都不用怎么分析了， 肯定是再定义一个 ClientDisconnectEvent 客户端连接断开事件的订阅者，订阅者可以接收并处理该事件。而处理该事件的具体逻辑就是创建一个延时执行任务，把该任务提交给 Distro 集群的延迟任务执行引擎中 。这个流程大家应该可以理解了吧？如果这个逻辑大家理解了，那么接下来我就可以为大家展示 ClientDisconnectEvent 事件的订阅者了。这个订阅者其实就是 DistroClientDataProcessor 集群客户端处理器，在 nacos 源码中，程序就是这么设计的，所以接下来就请大家看一看再次重构之后的 DistroClientDataProcessor 类，请看下面代码块。  
可以看到，我们已经让 DistroClientDataProcessor 订阅者也订阅了 ClientDisconnectEvent 客户端连接断开事件，实现的方式也很简单。遗憾的是，我们仍然没能为 DistroClientDataProcessor 集群客户端数据处理器引入延迟任务和延迟任务执行引擎，这也就造成了 DistroClientDataProcessor 类的 syncToAllServer() 方法迟迟无法实现。大家肯定会觉得特别奇怪，我为什么迟迟不实现 syncToAllServer() 方法呢？ 直接把 DistroDelayTaskExecuteEngine 延迟任务执行引擎定义为 DistroClientDataProcessor 类的成员变量，然后直接在 syncToAllServer() 方法中使用不就行了 ？就像下面展示的这样，请看下面代码块。  
上面代码块中的 syncToAllServer() 方法虽然仍然是伪代码，但大家应该都能明白我的意思。好了，接下来让我给大家解释一下，为什么我不把 DistroDelayTaskExecuteEngine 延时任务执行引擎定义为 DistroClientDataProcessor 类的成员变量，如果只解释最直接的原因，这就太容易了： 因为在 nacos 源码中，定义了一个新的组件，那就是 DistroTaskEngineHolder 类，翻译过来就是 Distro 集群执行引擎持有者的意思。Distro 集群要用到的延迟任务执行引擎和非延迟任务执行引擎都定义为这个类的成员变量了，并且这个类还被 springboot 的 @Component 注解标记了，表示这个类的对象会被 springboot 容器管理 ，该类的具体内容如下，请看下面代码块。  
通过以上代码块我们可以看到， 这个新定义的 DistroTaskEngineHolder 类持有了 Distro 集群要用到的延迟任务执行引擎和非延迟任务执行引擎，这两个执行引擎的创建时机也展示得非常清楚，并且在这个类中还把处理延迟任务的 DistroDelayTaskProcessor 处理器交给延迟任务执行引擎使用了 。nacos 源码定义这个 DistroTaskEngineHolder 类的意图非常明显， 那就是用一个组件管理这两个要被 Distro 集群用到的执行引擎，换句话说，只要 Distro 集群哪里需要某个执行引擎了，只需要获得 DistroTaskEngineHolder 类的对象，然后从 DistroTaskEngineHolder 对象中得到对应的执行引擎即可。而 DistroTaskEngineHolder 对象是被 springboot 容器管理的，哪里需要它，它就可以被自动注入到哪里 。这时候肯定有朋友会说，那就直接把 DistroTaskEngineHolder 对象注入到 DistroClientDataProcessor 类中吧，这样一来，DistroClientDataProcessor 对象也可以得到延迟执行引擎，然后使用延迟执行引擎了。  
  
想法非常不错，我也觉得可以，但是真要这么做的时候，大家就会发现完全行不通。 因为 DistroClientDataProcessor 这个类的对象根本就没有被 springboot 管理，如果一个类的对象没有被springboot容器管理，那我们就无法向这个类中注入任何被 springboot 容器管理的对象 。这一点大家应该可以理解吧？讲到这里也许大家会忽然回过神，发现我们一直使用的 DistroClientDataProcessor 集群客户端数据处理器原来根本不是被 springboot 容器管理的对象，那么它该如何创建呢？ 只有创建该对象的时候，调用了该对象的构造方法，才会在构造方法中把该订阅者对象注册到 NotifyCenter 事件通知中心啊 。所以现在我们又多了一个问题，那就是 DistroClientDataProcessor 对象的创建时机。除了这个问题之外，现在我们也没办法把 DistroTaskEngineHolder 对象注入到 DistroClientDataProcessor 对象中了，那么 DistroClientDataProcessor 对象该怎么使用延迟任务执行引擎呢？最后就是上一小节提出的三个问题中，剩下的最后一个没有解决的问题，那就是 DistroDelayTask 延迟任务处理器，也就是 DistroDelayTaskProcessor 类该怎么定义呢？现在我们又得到了三个问题，我为大家总结了一下：  
1 DistroClientDataProcessor 集群客户端数据处理器对象的创建时机。  
2 DistroClientDataProcessor 对象该怎么使用延迟任务执行引擎呢？  
3 DistroDelayTask 延迟任务处理器，也就是 DistroDelayTaskProcessor 类该怎么定义呢 ？  
这几个问题在这一章显然是无法解决了，要想解决就要引入 Distro 集群另一个非常重要的组件，也就是 DistroProtocol 类，但这是下一章的内容了，就留到下一章讲解吧。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/pbw73ksq562ldxty*  
*All content belongs to its respective owners and creators.*