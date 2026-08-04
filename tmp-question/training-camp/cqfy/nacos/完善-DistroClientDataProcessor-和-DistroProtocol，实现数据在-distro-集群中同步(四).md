在上一章结尾，我们留下了三个问题等待解决，这三个问题就是：  
1 DistroClientDataProcessor 集群客户端数据处理器对象的创建时机。  
2 DistroClientDataProcessor 对象该怎么使用延迟任务执行引擎呢？  
3 DistroDelayTask 延迟任务处理器，也就是 DistroDelayTaskProcessor 类该怎么定义呢 ？  
其中第一个问题非常关键，如果 DistroClientDataProcessor 集群客户端数据处理器不被 springboot 容器管理，那么它就不会被自动创建，它的构造方法也就不会被调用，也就不会把自己这个订阅者注册到 NotifyCenter 事件通知中心，那么当 Distro 集群的某个节点内部发布了 ClientChangedEvent 客户端变更事件、ClientDisconnectEvent 客户端连接断开事件之后，当前节点的 DistroClientDataProcessor 订阅者就无法接收并处理这两个事件，那么把数据备份给集群其他节点的操作也就无从谈起了。所以，当务之急是先明确 DistroClientDataProcessor 集群客户端数据处理器的创建时机，至于剩下的两个问题则非常容易解决，所以暂时先放在一边。  
  
再次重构 DistroClientDataProcessor 类  
  
现在我们要分析 DistroClientDataProcessor 对象的创建时机，但是如果对着这个问题硬想，我相信一时半会大家也没什么头绪。所以接下来，我想先抛开这个问题，先分析分析别的。首先还是请大家先回顾一下我们上一章重构之后的 DistroClientDataProcessor 类，请看下面代码块。  
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

//关注客户端变更事件

result.add (ClientEvent.ClientChangedEvent.class);

//关注客户端连接断开事件

也许有的朋友有点疑惑，为什么我总是让大家回顾这个 DistroClientDataProcessor 类的内容。文章更新到这里，这个 DistroClientDataProcessor 类的内容似乎也没有发生什么大的变化。无非就是作为订阅者来说，订阅的事件多了一个，到此为止，这个 DistroClientDataProcessor 客户端数据处理器仍然没有实现把延迟任务提交给延迟任务执行器的操作。好了，不管怎么说吧，先让我们把这些遗留的问题放到一边，因为我忽然在这个 DistroClientDataProcessor 类上发现了一点新的东西，想跟大家分享一下。  
  
大家看完刚才展示的代码块，对 DistroClientDataProcessor 类的印象肯定非常清晰，那就是它是一个订阅者的身份存在的。这个应该没有异议吧，毕竟从它内部定义的方法来看，它现在完全就是一个提供了订阅者功能的组件。但是接下来， 请大家把视角定格在 DistroClientDataProcessor 这个类的类名上，类名翻译过来是 Distro 集群客户端数据处理器的意思，所谓客户端其实就是每一个节点为 nacos 客户端连接创建的 Client 客户端对象 。 也许有朋友很早之前就感到困惑了，为什么明明是一个订阅者对象，却定义了一个客户端数据处理器的类名，如果大家之前对此没什么感觉，那现在我把这个问题提出来了，应该也会觉得疑惑了吧？所以，请大家想一想， 为什么从身份上来说，DistroClientDataProcessor 明明是一个订阅者，订阅了 ClientChangedEvent 客户端变更事件和 ClientDisconnectEvent 客户端连接断开事件，确把它的类名定义成了客户端数据处理器的意思呢 ？  
  
当然，如果思考得稍微粗暴一些，这个问题的答案也是显而易见的， 既然把一个订阅者定义为了客户端数据处理器，就意味着 DistroClientDataProcessor 类的对象有两种身份，一种是订阅者，一种是客户端数据处理器，两种身份就意味着这个 DistroClientDataProcessor 类的对象要承担两种职责 ，这个答案应该说是合情合理的吧？  
  
好了，如果大家同意我刚才给出的答案，那么现在问题就来了， 从目前实现的 DistroClientDataProcessor 类来看，我们只看到这个类的对象承担了订阅者的职责，并没有没看到这个类的对象对 Client 客户端对象存储的数据进行任何处理啊，也就是说 DistroClientDataProcessor 完全没有体现出客户端数据处理器的职责 。对于 DistroClientDataProcessor 类的名称来说，这显然是一个败笔。也许有朋友会觉得我对 DistroClientDataProcessor 类的名称进行一番分析非常可笑，这个类什么名称还不是我自己定义的，既然这个类的内容完全没体现出客户端数据处理器的职责，那就给这个类再换一个名称就好了，纠结这些无关紧要的内容干什么？没错，我确实也可以给 DistroClientDataProcessor 这个类再换一个名字，让这个类只负责订阅者的功能即可，但既然说到这里了，索性就敞开解释一下， 我为大家引入的这个 DistroClientDataProcessor 类就是按照 nacos 源码来命名的，换句话说，在 nacos 源码中就有 DistroClientDataProcessor 这个类，这个类既是订阅者又是客户端数据处理器 ，所以我也是按照 nacos 源码中的 DistroClientDataProcessor 类的内容来给大家分析的。当然，大家尽可以当我是在马后炮。不过这些并不重要，能把功能分析清楚就行，接下来就请大家继续思考一个问题： 既然 DistroClientDataProcessor 类作为订阅者的同时，也需要体现出客户端数据处理器的功能，那怎么重构 DistroClientDataProcessor 类，给 DistroClientDataProcessor 类添加什么功能，才能让它体现出客户端数据处理器的功能呢 ？  
  
很好，问题已经明确了，就意味着我们要研究的方法明确了，接下来就很好办了。 要想让 DistroClientDataProcessor 类的对象体现出客户端数据处理器的功能，那我们就应该考虑一下，一个客户端数据处理器应该具备什么功能 。如果仍然从类名上研究，所谓的客户端处理器就是专门处理 Client 客户端对象存储的数据的处理器，那对于 Client 客户端对象存储的数据，究竟要执行什么操作呢？这个就很明显了，Distro 集群的每一个节点内部可能都创建了多个 Client 客户端对象，每一个 Client 客户端对象都存储了很多服务实例信息，而在 Distro 集群中，要对每一个节点的 Client 对象执行的操作非常简单： 那就是当 Distro 集群中某一个节点的 Client 客户端对象内部存储的服务实例信息发生变更了，该节点要把最新的服务实例信息同步给集群的其他节点。在这个操作执行的过程中，需要从服务实例发生变更的 Client 客户端对象中获得最新的所有服务实例信息；而当这些服务实例信息被同步给集群的其他节点之后，其他节点需要接收这些服务实例信息，然后更新自己内部对应的 Client 客户端对象存储的服务实例信息 。这个逻辑大家可以理解吧？如果这个逻辑大家理解了，那么 DistroClientDataProcessor 客户端数据处理器要定义的新功能也就明确了。  
  
根据我们刚才的分析，DistroClientDataProcessor 客户端数据处理器中显然应该新定义两个功能： 第一个功能就是当 Distro 集群的一个节点要把某个 Client 客户端对象内部的服务实例信息备份给集群的其他节点时，DistroClientDataProcessor 客户端数据处理器可以直接得到该 Client 客户端对象内存放的所有服务实例信息；第二个要添加的功能就是当 Distro 集群中的某个节点接收到了从其他节点备份过来的数据，那么该节点可以直接使用 DistroClientDataProcessor 客户端数据处理器把这些数据更新给对应的 Client 客户端对象 。这就是应该向 DistroClientDataProcessor 客户端数据处理器中新添加的功能。既然 DistroClientDataProcessor 客户端数据处理器要和 Client 客户端对象打交道，而 Client 客户端对象需要从客户端管理器中获得， 那我们就可以把 ClientManager 客户端管理器定义为 DistroClientDataProcessor 类的成员变量 。当然，只实现了这些还不够，我们还应该再定义一个新的对象，这个对象就是用来封装从 Client 客户端中获得的所有服务实例信息的。原因很简单，从 Client 客户端对象中获得了最新的服务实例信息之后，要把这些服务实例信息备份给 Distro 集群中的其他节点呀，我们定义的这个新对象正好可以用来网络传输这些数据。我把这个对象定义为了 DistroData，具体实现请看下面代码块。  
好了，这个 DistroData 类展示完毕之后，接下来就应该根据我们之前的分析，重构一下 DistroClientDataProcessor 类了。我首先把第一个要添加的功能重构到 DistroClientDataProcessor 类中，请看下面代码块。  
好了朋友们，通过以上代码块，我想大家应该就清楚了 DistroClientDataProcessor 客户端数据处理器是怎么获得指定 Client 客户端对象内部存储的所有服务实例信息的了。 只要调用了 DistroClientDataProcessor 对象的 getDistroData() 方法，就可以得到该 Client 客户端对象内部存储的所有服务实例信息 。好了，第一个要添加的功能已经定义完毕了，接下来我就要继续重构 DistroClientDataProcessor 客户端数据处理器，把第二个功能， 也就是把从 Distro 集群其他节点接收到数据更新给对应的 Client 客户端对象的功能给实现了 。具体实现请看下面代码块。  
上面代码块展示完毕之后，我们就可以知道了， 当 Distro 集群中的一个节点把数据备份给其他节点之后，接收备份数据的节点最终会把数据交给 DistroClientDataProcessor 客户端数据处理器来处理，调用该处理器的 processData() 方法，在该方法中判断要执行的备份操作是什么，然后对指定的 Client 客户端对象执行对应的操作 。这个逻辑想必也非常清楚了。当然， 接收备份数据的节点并不是直接就调用 DistroClientDataProcessor 处理器的 processData() 方法来接收数据，实际上还有外层的组件在工作，是外层的组件接收备份过来的数据，最后把数据交给 DistroClientDataProcessor 客户端数据处理器来处理 。这一点大家很快就会清楚了，现在大家先不必关心外层接收备份过来的数据的组件是什么，总之，现在我们已经把 DistroClientDataProcessor 类重构完毕了，这个类也承担了客户端数据处理器的职责，而且我们还可以再一次发现， 当 Distro 集群中的某个节点向其他节点备份数据的时候，确实是以 Client 客户端对象为单位进行数据备份的，每一次备份都会把对应的 Client 客户端对象下的所有服务实例信息全部备份给集群的其他节点 。  
  
好了，到此为止，DistroClientDataProcessor 客户端数据处理器也就重构得差不多了，这个类已经同时具备了订阅者功能和客户端数据处理功能，我知道这时候肯定有朋友会好奇，如此大费周章地重构这个类，并且为这个 DistroClientDataProcessor 类定义处理 Client 客户端存储的数据的功能，究竟是为了什么呢？别急，接下来就让我为大家详细解答这个问题。  
  
引入 DistroProtocol 组件  
  
实际上，在 nacos 源码中为 Distro 集群中的各个组件划分了非常清晰的职责界限，就比如说在上一小节我为大家重构完毕的 DistroClientDataProcessor 客户端数据处理器， 在 nacos 源码中，这个类的对象就专门负责和 Client 客户端数据打交道，所谓打交道其实就是从对应的 Client 对象中获得所有服务实例信息，或者把从其他节点备份过来的服务实例信息更新到本节点对应的 Client 客户端对象中。所以这个类的名称才会定义为 DistroClientDataProcessor，也就是客户端数据处理器的意思 。好了，现在大家应该已经从 nacos 源码的角度清楚了 DistroClientDataProcessor 客户端数据处理器的由来，并且也知道了这个处理器只负责和 Client 客户端数据打交道，那这时候就应该有所意识了： 那就是把数据备份给 Distro 集群其他节点的操作并不是由该 DistroClientDataProcessor 处理器发起的，也就是说，我们并不能把 Distro 集群要用的延迟任务执行器交给 DistroClientDataProcessor 处理器使用，更不应该在该处理器的 syncToAllServer() 方法中创建延迟任务，然后把延迟任务提交给延迟任务执行器处理 。这个逻辑理解起来应该也没有那么困难吧？如果大家理解了这个逻辑，那么现在就请大家思考一下， 把数据备份给 Distro 集群其他节点的操作应该由哪个组件来执行呢？也就是说，应该把延迟任务执行器交给哪个组件使用，并且在这个组件中创建延迟任务，然后把延迟任务提交给延迟任务执行器呢 ？这就是我们目前面临的问题。  
  
既然我已经为大家引入了 nacos 源码的某些内容，所以我就不在这个问题上卖关子了。实际上， 在 nacos 源码中还定义了一个新的组件，就是本小节标题中写明的 DistroProtocol 组件，翻译过来就是 Distro 集群协议组件的意思 。 从这个组件的名字上也能看出来，在 nacos 源码中，这个组件就负责 Distro 集群层面上的所有操作 ，那什么是 Distro 集群层面的操作呢？就目前的情况来说，Distro 集群层面的操作无非就两个： 一个就是当前节点把某个 Client 客户端内部最新的服务实例备份给集群的其他节点，这个操作的实现就要使用延迟任务和延迟任务执行引擎；另一个操作就是当前节点接收到从集群其他节点备份过来的某个 Client 客户端下的最新的服务实例信息，在这个操作执行的过程中，DistroProtocol 组件会把接收到的服务实例信息交给 DistroClientDataProcessor 客户端数据处理器，让客户端数据处理器把服务实例信息更新到对应的 Client 客户端对象内部 。由此可见，在源码中 Distro 集群中的各个组件的功能划分是非常清楚的。并且通过刚才的分析，我们也已经清楚了， 既然 Distro 集群备份数据的操作是由新定义的 DistroProtocol 组件负责的，那么延迟任务执行器肯定要交给这个 DistroProtocol 组件使用，并且延迟任务肯定也是在这个组件中创建的，然后提交给延迟任务执行器处理 。这个逻辑清楚了之后，接下来我们就趁热打铁，先把 DistroProtocol 组件给简单定义一下。当然一开始先不用关注太多功能， 就先实现当前节点把某个 Client 客户端内部最新的服务实例备份给集群的其他节点的功能即可 。请看下面代码块。  
从上面代码块中可以看到，DistroProtocol 这个类上添加了 springboot 的 @Component 注解，这就意味着这个 DistroProtocol 类的对象也是要交给 springboot 容器管理的。除此之外，我们还能看到这个 DistroProtocol 协议组件中定义了一些方法，这些方法就是用来执行把数据备份给集群其他节点操作的。通过上面代码块中的注释，我相信每一位朋友对程序的执行流程已经非常清楚了， 那就是只要当 DistroProtocol 组件的 sync() 方法一被调用，该组件就可以执行备份数据给其他节点的其实操作了，最后程序会执行到该组件的 syncToTarget() 方法中，在该方法中，就会创建向其他节点同步数据的延迟任务，然后把任务提交给延迟任务执行引擎 。这个流程理解起来应该也没有难度了吧？如果这个流程大家已经理解了， 那么现在我们就要想一想这个 DistroProtocol 的 sync() 方法究竟在哪里会被调用呢？因为只要这个方法被调用了，当前节点才会真正开始执行向其他节点备份数据的操作 。这就很好说了，还是寻找操作的起始源头即可。Distro 集群的一个节点什么时候才会把数据备份给集群的其他节点呢？当然是某个 Client 客户端存储的数据发生变更时，那 Client 客户端存储的数据什么时候会发生变更呢？当然是程序的 NotifyCenter 事件通知中心发布了 ClientChangedEvent 或者 ClientDisconnectEvent 事件呀？那这些事件会被谁处理呢？当然是 DistroClientDataProcessor 客户端数据处理器呀。答案就这样分析出来了，接下来， 我们只需要在 DistroClientDataProcessor 客户端数据处理器处理这两个事件的时候调用 DistroProtocol 组件的 sync() 方法即可 。 这也就意味着 DistroClientDataProcessor 客户端数据处理器是要持有 DistroProtocol 协议组件的 。好了，接下来就让我为大家展示一下最后重构的 DistroClientDataProcessor 类，请看下面代码块。  
到此为止，DistroClientDataProcessor 客户端数据处理器才算重构得差不多了，要用到的各个组件也都定义为它的成员变量了，大家也应该清楚了 Distro 的节点备份数据的操作流程了吧？当然，还有后续的流程我没有给大家实现，比如延迟任务该怎么被处理，然后怎么提交给非延迟任务执行引擎执行等等。这个马上就会讲到了，大家没必要着急。接下来，让我们把目光继续集中在 DistroProtocol 组件上，看看它的第二个功能要怎么实现。所谓第二个功能， 就是当前节点接收从集群其他节点备份过来的某个 Client 客户端下的最新的服务实例信息，在这个操作执行的过程中，DistroProtocol 组件会把接收到的服务实例信息交给 DistroClientDataProcessor 客户端数据处理器，让客户端数据处理器把服务实例信息更新到对应的 Client 客户端对象内部 。这一点之前已经分析过了，接下来我们就一起来看看这个功能该怎么实现。  
  
如果大家对之前的内容还有印象，肯定还记得， 当 Distro 集群中的某一个节点把服务实例备份给集群的其他节点时，会把要备份的服务实例信息封装到一个 DistroData 对象中，然后把这个对象传输给集群的其他节点。这样一来，当集群的其他节点接收到备份过来的数据时，肯定也是接收到了一个 DistroData 对象 。既然是这样，那 DistroProtocol 组件的第二个功能就很好实现了， 我们直接给这个组件定义一个 onReceive() 方法，在这个方法中接收从其他节点传输过来的 DistroData 对象，然后把该对象交给 DistroClientDataProcessor 客户端数据处理器处理即可。如果这个逻辑大家都能理解，那么就应该能想到，DistroClientDataProcessor 客户端数据处理器显然就应该交给 DistroProtocol 协议组件使用 。理解这一点也没问题吧？好了，接下来我就为大家展示一下实现了第二个功能的 DistroProtocol 组件，请看下面代码块。  
上面代码块展示完毕之后，现在大家应该也清楚了 DistroProtocol 协议组件是怎么接收从集群其他节点备份过来的数据的了。只需要调用 DistroProtocol 对象的 onReceive() 方法就可以接收到从其他节点传输过来的 DistroData 对象，而 DistroData 对象封装了需要备份的服务实例信息，把它交给客户端数据处理器直接处理即可。这个逻辑非常简单，大家肯定都能理解。但是，随着上面代码块的展示，我相信很多朋友心里肯定产生了更多的疑问，至少有两个疑问：  
1 那就是在我们刚刚展示的 DistroProtocol 类中，虽然把 DistroClientDataProcessor 客户端数据处理器定义为它的成员变量了，但是我并没有在 DistroProtocol 类中把 DistroClientDataProcessor 对象创建出来，因为我们到现在也没有 DistroClientDataProcessor 对象的创建时机，这个时机不确定，DistroClientDataProcessor 对象就无法被创建，也就无法在 DistroProtocol 协议组件和 NotifyCenter 事件通知中心中发挥作用 。  
2 我们都知道，只要 DistroProtocol 组件的 onReceive() 方法被调用了，当前节点就可以处理从其他节点接收到的备份过来的数据，那 DistroProtocol 组件的 onReceive() 方法应该在哪里被调用呢？只有这个方法被调用的时机弄清楚了，我们才知道当前节点究竟是怎么接收到其他节点备份过来的数据的 。  
我相信目前困扰着大家的就是这两个问题。当然，我们在上一章遗留的三个问题还有两个也没有得到解决，就是以下两个问题：  
1 DistroClientDataProcessor 集群客户端数据处理器对象的创建时机。  
2 DistroDelayTask 延迟任务处理器，也就是 DistroDelayTaskProcessor 类该怎么定义呢？  
本来上一章遗留了三个问题，但这三个问题中的第二个问题是 DistroClientDataProcessor 对象该怎么使用延迟任务执行引擎？现在我们已经知道了延迟任务执行引擎并不能交给 DistroClientDataProcessor 对象使用，而应该交给 DistroProtocol 组件使用 ，所以这个问题就算是解决了。总之到目前为止，我们还剩下四个问题没有解决。  
  
并且我们还可以看到，在这四个问题中，有两个问题都和 DistroClientDataProcessor 客户端数据处理器的创建时机有关。但这个问题我们迟迟没有解决，接下来就应该正式解决它了。之前一直不解决这个问题是因为时机还不太成熟，现在时机已经成熟了。我这么说也是有原因的，因为我们已经为 Distro 集群定义了非常多的组件了，接下来就让我们从这方面入手，看看这个问题该怎么解决。  
  
引入 DistroDataRequestHandler 处理器和 Distro 集群组件持有者 DistroComponentHolder  
  
首先让我们先来看看，到目前为止我们为了实现 Distro 集群备份数据的操作，已经为 Distro 集群引入了多少组件了。 DistroProtocol 协议组件已经引入了，DistroClientDataProcessor 客户端数据处理器也引入了，持有者延迟任务执行引擎和非延迟任务执行引擎的 DistroTaskEngineHolder 引擎持有者也引入了 。总的来说，到目前为止已经引入了三个非常重要的组件。  
  
其中 DistroProtocol 协议组件就是用来执行 Distro 集群层面的操作的；DistroClientDataProcessor 客户端数据处理器就是专门用来和 Client 客户端数据打交道的；DistroTaskEngineHolder 引擎持有者就是为其他组件提供延迟任务执行引擎和非延迟任务执行引擎的 。这三个组件的作用非常清楚。但是，仅仅靠这三个组件，就目前的情况来说，我们还无法彻底完成 Distro 集群备份数据给其他节点的功能。原因很简单，也很直接， 虽然这三个组件负责的功能非常重要，但还欠缺最后一个非常重要的组件，那就是网络数据传输组件，没有这个网络数据传输组件，节点也不可能把要备份的数据传输给集群的其他节点啊 。也许有朋友会说，网络数据传输组件不就是网络通信组件吗？这个组件在前面构建 Distro 集群的时候不是已经实现了吗？就是所谓的 ClusterRpcClientProxy 组件，我把这个 ClusterRpcClientProxy 集群客户端代理器的内容也搬运过来了，帮助大家回顾一下，请看下面代码块。  
在看完上面代码块展示的内容之后，大家应该也会同意我直接把 ClusterRpcClientProxy 类的对象当成 Distro 集群的数据传输组件，反正从这个 ClusterRpcClientProxy 类的对象中就可以获得集群其他节点的客户端，就可以使用这些客户端和集群中各自对应的节点进行通信，传输要备份的数据，这么一来，网络数据传输组件不就早实现了吗？没错，确实是这个道理， 但是在 nacos 源码中，又对这个 ClusterRpcClientProxy 类做了一层封装，也就是说为 Distro 集群定义了一个专门的网络数据传输组件，这个组件内部持有了 ClusterRpcClientProxy 对象，这个新定义的网络数据传输组件真正发送数据的时候使用的仍然是 ClusterRpcClientProxy 对象，但是在发送数据之前还做了一些额外的操作，比如说检查集群中的目标节点是否处于健康状态 。接下来我就为大家展示一下专门为 Distro 集群新定义的这个网络数据传输组件，我把它定义为了 DistroClientTransportAgent 类，具体实现请看下面代码块。  
在看到 DistroClientTransportAgent 组件的内容之后，现在大家是不是觉得思路越来越清晰了？ 当 Distro 集群的某个节点要把数据备份给集群中的其他节点时，最终就是通过 DistroClientTransportAgent 组件的 syncData() 方法发送出去的，在该方法中，创建了一个 DistroDataRequest 请求对象，并且把要备份的数据封装到该对象中传输给目标节点了 。这个逻辑大家应该也清楚了吧？但随着这个 DistroClientTransportAgent 组件的定义，现在又出现了两个新的问题， 那就是这个 DistroClientTransportAgent 组件该怎么被创建呢？它可没有添加什么 springboot 的注解，也就意味着这个类的对象需要 nacos 自己创建；第二个问题就是现在我们终于发现集群节点备份数据的时候，是通过 DistroDataRequest 请求把数据传输给目标节点的，那么这个请求到了目标节点之后该怎么被接收呢 ？  
  
第二个问题非常容易解决， 直接定义一个请求处理器即可，反正在 nacos 框架中有非常多的请求处理器，每一个请求处理器都对应着一个请求，当节点接收到 DistroDataRequest 请求之后，就使用对应的请求处理器处理该请求即可。这个请求处理器我也定义完毕了，就定义为了 DistroDataRequestHandler 请求处理器 ，该处理器的具体实现请看下面代码块。  
好了，现在 DistroDataRequestHandler 请求处理器也定义完毕了，大家也知道了封装着要备份的数据的 DistroDataRequest 请求究竟是怎么被接收的了。接下来还是回过头看看刚才的问题，那就是DistroClientTransportAgent 组件该怎么被创建呢？这个问题我就直接解答了吧？现在我们已经为 Distro 集群引入了非常多的组件了，并且通过刚才展示的几个组件的内容， 大家可以看到有些组件也会被其他组件使用，有些组件被 springboot 容器管理了，而有的组件并没有被 springboot 容器管理 。就目前的情况来看，这几个组件是比较散乱的，没有被系统的管理，所以接下来我要定义一个新的组件， 那就是 DistroComponentHolder 类，翻译过来就是 Distro 集群所有组件持有者的意思，这个类的对象就持有了我们之前定义的所有要被 Distro 集群用到的组件，这样一来，只要程序哪个地方需要用到某个 Distro 集群的组件，直接就可以从 DistroComponentHolder 对象中获取即可。而 DistroComponentHolder 对象则被 springboot 容器管理，这样它就可以随时被其它对象或者组件获取了 。接下来就请大家看一下我新定义的 DistroComponentHolder 类，请看下面代码块。  
可以看到，我新定义的新的 DistroComponentHolder 组件持有了网络数据传输器和客户端数据处理器，至于 DistroProtocol 协议组件和 DistroComponentHolder 任务执行引擎持有者则并没有被 DistroComponentHolder组件管理，原因也很简单，因为这两个组件都被 springboot 容器管理了。当然， 我现在只是把 DistroComponentHolder 组件定义完毕了，那么网络数据传输器和客户端数据处理器怎么被设置到这个 DistroComponentHolder 组件中呢 ？很好，这时候终于涉及到这两个组件的创建时机了。  
  
实际上在 nacos 服务端源码中还定义了一个被 springboot 容器管理的组件，那就是 DistroClientComponentRegistry 集群客户端组件注册器，这个注册器会创建 Distro 集群要用到的、并且是没有被 springboot 容器管理的组件，然后把组件设置到 DistroComponentHolder 组件持有器中。这个 DistroClientComponentRegistry 集群客户端组件注册器我也定义完毕了，接下来就展示给大家，请看下面代码块。  
现在，网络数据传输器和客户端数据处理器的创建时机，大家就都清楚了吧？也就是说， 当 springboot 程序启动的时候，当 springboot 容器创建 DistroClientComponentRegistry 对象的时候，就会把 Distro 集群要用到的网络数据传输器和客户端数据处理器创建完毕，然后交给 DistroComponentHolder 组件管理使用。Distro 集群要使用某些组件的时候，只要可以获得 DistroComponentHolder 对象就能直接从该对象中得到自己想要使用的组件，而 DistroComponentHolder 对象也是被 springboot 容器管理的，所以这个对象也可以随时被其它对象或组件获得 。到此为止，之前遗留的所有问题基本上都解决完毕了，只剩下最后一个问题， 那就是DistroDelayTask 延迟任务处理器，也就是 DistroDelayTaskProcessor 类该怎么定义呢 ？这个问题大家没有忘记吧？ 现在我们已经清楚 Distro 集群的一个节点发起数据备份操作的时机了，也知道数据备份的操作是哪个组件执行的了，也知道延迟任务是怎么提交给延迟任务执行器的了，也知道了要备份数据的请求是怎么网络传输数据器被发送给目标节点的，也知道目标节点是怎么接收并处理要备份的数据的，现在就差怎么把要备份的数据交给网络传输数据器发送了，这一点大家可以理解吧 ？所以， 我们接下来要实现的就是把要备份的数据交给 DistroClientTransportAgent 网络数据传输器发送的功能 。而这个功能不用我说大家也能猜到了，这肯定就是处理延时任务的处理器要承担的责任，所以接下来，就让我们实现它吧。  
  
实现 DistroDelayTaskProcessor 延迟任务处理器  
  
在具体实现这个延迟任务执行器之前，我还是要先帮助大家回顾一下，这个延迟任务执行器的创建时机，大家应该都清楚了，因为上一章我已经展示给大家了。就在上一章引入的 DistroTaskEngineHolder 任务执行器持有者中创建的，具体的内容请看下面代码块。  
在上面代码块中可以看到这个延迟任务处理器就是 DistroDelayTaskProcessor 对象，它的创建时机也非常清楚，接下来就该实现它了。 实现它的过程也没什么好分析的了，就是针对不同的操作创建不同的可以被立即执行任务，然后把任务提交给立即执行任务的执行引擎即可 。具体实现请看下面代码块。  
可以看到，延迟任务处理器在处理延迟任务的时候，会根据任务的操作符创建不同的可以被立即执行的任务对象， 如果是客户端连接断开的操作，那就创建 DistroSyncDeleteTask 任务对象提交给立即执行任务的引擎去执行；如果是同步数据的操作，就创建 DistroSyncChangeTask 任务对象提交给立即执行任务的引擎去执行 。这些逻辑已经非常清楚了。所以接下来我们就要看看这些可以立即被执行的任务是怎么被定义的，在这些任务中，也许就有把要备份的数据交给网络传输数据器的操作。在文章中我就只展示 DistroSyncChangeTask 任务对象了，另外的任务对象和它的逻辑大同小异，就留给大家自己去我提供的第 11 版本代码中查看吧。DistroSyncChangeTask 类的具体内容如下，请看下面代码块。  
到此为止，我就为大家把 Distro 集群备份数据的全功能都实现完毕了，说实话，内容非常多，写到这里我也已经心力交瘁了，所以越到后面功能实现进展得就越快，有些内容根本来不及展开， 比如验证每个节点备份的数据是否一致的操作，新加入集群的节点从某个节点下载快照的操作，向集群中的其他节点查询某些数据的操作等等，这些操作的执行流程和数据备份的流程几乎一样，所以就都留到我提供的第十一版本代码中 ，给大家自己查看吧。现在第十一版本代码的内容大家都可以查看了。大家可以认真读几遍文章，自己画画流程图，把这些知识都消化了。  
  
当然，有些内容来不及展开，但并不意味着一点都不讲解， 就比如说验证每个节点备份的数据是否一致的功能，新加入集群的节点从某个节点下载快照的功能 ，这两个功能其实非常有必要详细讲解，在文章中一点点地剖析它们的实现思路，然后实现它们，因为它们是很重要的功能 。Distro 集群备份了数据之后，肯定需要经常验证每一个节点存放的数据是否一致，如果不一致就要执行某些操作，使数据一致，所以集群启动的时候还要启动一个定期验证节点数据是否一致的定时任务；还有一个新的节点添加到 Distro 集群中，肯定要以下载快照的方式使自己尽快和集群其他节点保存的数据同步了，那是不是得启动一个远程加载快照的任务呢 ？这些功能我都没有在文章中为大家讲解，这是因为现在的篇幅实在是太长了，写不下了，所以就省略了。但这些功能的实现思路非常简单，都是封装成任务，让执行器去执行。并且这些操作就定义在了 DistroProtocol 组件中，我把对应的代码搬运到这里了，请看大家看一看，请看下面代码块。  
执行集群节点数据验证和下载快照的任务我就不在文章中展示了，大家直接去看我提供的第十一版本代码即可。好了朋友们，Distro 集群的内容到此就全部结束了，下一章就要构建 jraft 集群了，jraft 集群的内容更简单，很快就能实现完毕。朋友们，我们下一章见！  
  
  
  
附：其实文章中有非常多的细节都省略了，如果全都铺展开讲解实现，那么十章内容也讲不完。所以大家一定要认真看文章，然后再去看我提供的代码，代码中注释非常详细，大家只要把文章掌握了，那么所有代码都能看明白。至于省略的知识，我就给大家简单列举一下，这样大家也能有目的地去看我提供的代码，比如说 Distro 集群成员变更了，添加了新的节点之后，要怎么做呢？如果大家仔细阅读 Member 工具类中的代码，那大家就会清楚集群成员变更之后，本地集群配置文件中的内容也会更新。还有一点，nacos 把 Distro 集群定义为 ap 模式的集群，大家可以仔细阅读 Distro 集群备份数据的代码，然后思考一下为什么这个 Distro 集群被定义为了 ap 模式的集群？好了，多余的话我就不说了，大家自己去代码中寻找答案吧，源码之中，没有秘密可言。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/qh4lvs7a8bsgwdgx*  
*All content belongs to its respective owners and creators.*