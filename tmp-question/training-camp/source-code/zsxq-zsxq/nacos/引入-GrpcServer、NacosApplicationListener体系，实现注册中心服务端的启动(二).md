上一章结尾我跟大家解释了 nacos 的 EnvUtil 组件的重要性，并且遗留了两个问题没有解决，这两个问题就是：  
EnvUtil 组件该如何定义，如何实现？  
启动的 springboot 程序，怎么把环境变量信息和用户定义的配置信息交给 EnvUtil 组件使用？  
  
也没必要再说什么废话了，上一章结尾已经把该说得都说了，所以现在我们就直接来看看这两个问题该如何解决。首先来看第一个问题，EnvUtil 组件该如何定义。在上一章我跟大家说了， EnvUtil 就是用来封装 nacos 注册中心服务端要使用的环境变量信息和配置信息的，不管是在 nacos 服务端哪个地方，只要需要某些配置信息了，就可以直接从 EnvUtil 组件中获取，这就是 EnvUtil 组件的作用 。而且我还跟大家简单类比了一下，从发挥的作用以及重要性来说，EnvUtil 组件对于 nacos ，就像是 ConfigurableEnvironment 对于 springboot，并且我还特意强调了，EnvUtil 组件中持有的所有数据，都是从 springboot 的 ConfigurableEnvironment 组件中获得的。所以， 我们要先要做的就是先把 EnvUtil 定义出来，然后再寻找合适的时机，把 springboot 的 ConfigurableEnvironment 组件中的数据交给 nacos 的 EnvUtil 组件来使用 。那么这个 EnvUtil 组件该如何定义呢？  
  
简单定义 EnvUtil 组件  
  
如果想用最直接，或者说最简便的方式定义 EnvUtil 组件，其实就可以把 springboot 的 ConfigurableEnvironment 组件定义到 EnvUtil 组件中。反正我们也要把 springboot 的 ConfigurableEnvironment 对象封装的数据信息交给 nacos 的 EventUtil 使用， 不妨就直接把 ConfigurableEnvironment 定义为 EventUtil 的成员变量，这样只需要直接给 ConfigurableEnvironment 成员变量赋值 ，EventUtil 不就得到了 springboot 收集的环境变量信息和用户定义的配置信息了吗？所以，这个 EventUtil 可以先定义成下面这样，请看下面代码块。  
package com.cqfy.nacos.sys.env;  
  
public class EnvUtil {  
//springboot的组件

private static ConfigurableEnvironment environment;  
public static ConfigurableEnvironment getEnvironment () {

return environment;

}  
public static void setEnvironment (ConfigurableEnvironment environment) {

EnvUtil.environment \= environment;

}

}

现在我就把 EnvUtil 组件定义好了，如果真的采用这种方式定义 EnvUtil 组件，那么我们只需要在恰当的时机给 EnvUtil 组件的 environment 成员变量赋值即可，就调用 EnvUtil 类的 setEnvironment() 方法。并且我还可以告诉大家，实际上在 nacos 源码中，就是这么定义 EnvUtil 组件的。当然，我们现在定义的只是 EnvUtil 中最基础的内容，别忘了 EnvUtil 组件的功能是什么，它需要为 nacos 服务端提供很多配置信息和环境变量信息。 在 springboot 的 ConfigurableEnvironment 组件中，是以键值对的方式存放所有信息的；所以到了 nacos 的 EnvUtil 组件，也是以键值对的方式存放所有信息的。既然是键值对的方式存放所有信息，那么 nacos 服务端想要从 EnvUtil 组件中获得某些配置信息，肯定就是根据 key 寻找对应的 value。所以，EnvUtil 组件还应该提供根据 key 查找对应 value 的方法 。这样一来，刚才定义的 EnvUtil 组件就要稍微重构一下，请看下面代码块。  
package com.cqfy.nacos.sys.env;  
  
public class EnvUtil {  
//springboot的组件

private static ConfigurableEnvironment environment;  
public static ConfigurableEnvironment getEnvironment () {

return environment;

}  
public static void setEnvironment (ConfigurableEnvironment environment) {

EnvUtil.environment \= environment;

}  
  
//添加根据key获得对应value的方法  
//判断数据源中是否包含对应数据

public static boolean containsProperty (String key) {

return environment.containsProperty (key);

}  
//根据key得到对应value

public static String getProperty (String key) {

return environment.getProperty (key);

}  
//这个方法定义了返回的数据的默认类型

public static String getProperty (String key,String defaultValue) {

return environment.getProperty (key,defaultValue);

}  
到此为止，我就为大家把 EnvUtil 组件给实现完毕了，当然，目前我在文章中展示的 EnvUtil 组件还比较简单，实际上在我提供的第七版本代码和 nacos 源码中，这个 EnvUtil 类中还包含更多内容，定义了很多常量 key，还有一些从 springboot 数据源中获得对应 value 的方法，逻辑都很简单，大家自己看看就成。  
  
现在 EnvUtil 已经定义完了，这样一来，就剩下最后一个问题需要解决了，那就是：  
启动的 springboot 程序，怎么把环境变量信息和用户定义的配置信息交给 EnvUtil 组件使用？ 这个问题就需要仔细分析分析了。  
  
分析 Springboot 程序启动经历的几个阶段  
  
随着 EnvUtil 组件的引入，我们已经知道了，要想把 springboot 持有的配置信息和环境变量信息交给 nacos 的 EnvUtil 使用，只需要直接给 EnvUtil 的 environment 成员变量赋值即可。而 environment 成员变量其实就是 springboot 的 ConfigurableEnvironment 组件。这样一来，springboot 的 ConfigurableEnvironment 组件直接赋值给 EnvUtil 的 environment 成员变量的操作就非常简单了，如果大家熟悉 springboot 的各个扩展接口的用法，肯定知道 springboot 有一个 EnvironmentAware 接口，只要让 EnvUtil 类实现 EnvironmentAware 接口，然后再把 EnvUtil 定义为 springboot 容器管理的 bean 对象，这样一来，在 EnvUtil 对象被创建的时候，它的 environment 成员变量就会被赋值了。就像下面代码块展示的这样，请看下面代码块。  
我相信这个 EnvironmentAware 接口的作用大家都很熟悉，并且也知道这个 EnvironmentAware 接口会在 bean 对象创建完毕，被 ApplicationContextAwareProcessor 这个 bean 处理器处理的时候发挥作用 。如果不考虑其他情况，那么我用上面代码块展示的方式给 EnvUtil 的 environment 成员变量赋值就没一点问题。但是我们不得不考虑现实情况： 那就是 nacos 服务端启动之前，或者说 nacos 正在向 grpc 服务端设置配置信息的时候，springboot 程序肯定启动到了创建 bean 对象阶段，这个在上一章已经讲解过了。因为只有 GrpcSdkServer 对象创建完毕，才会调用顶层父类 BaseRpcServer 添加了 @PostConstruct 注解的 start() 方法，nacos 服务端才会从 EnvUtil 组件中获得用户自己定义的配置信息的值。这就意味着，肯定要在 springboot 创建 bean 对象阶段之前，就把 springboot 管理的环境变量和配置信息交给 nacos 的 EnvUtil 组件 。大家可以仔细品味一下这个逻辑，然后再继续向下看。  
  
如果大家理解了刚才的逻辑，那现在问题就来了，如果要把 EnvUtil 定义为被 springboot 容器管理的 bean 对象，然后再使用 EnvironmentAware 接口和 ApplicationContextAwareProcessor 处理器为 EnvUtil 的 environment 成员变量赋值，这个操作也会在 springboot 创建 bean 对象阶段被执行。可是在这个阶段 nacos 服务端已经需要从 EnvUtil 组件获得用户定义的配置信息了， 这就要求在 GrpcSdkServer 对象创建之前，EnvUtil 对象一定要被创建成功，并且被 bean 处理器处理完毕了 。这一点想必也很容易理解，这样一来，我们就必须采取某些手段， 让 springboot 在启动的过程中，创建 bean 对象的时候，先于 GrpcSdkServer 对象创建出 EnvUtil 对象 。这样一来，当 nacos 服务端才能在启动之前从 EnvUtil 组件中获得用户自己定义的配置信息。那应该怎么自定义 springboot 容器管理的 bean 对象的创建顺序呢？  
  
springboot 为我们提供了一个很方便的注解， 那就是 @DependsOn 注解，如果某个类上添加了这个注解，那么注解中的类对象就会先于当前类对象被 springboot 创建 。这个注解就是“依赖于”的意思，nacos 框架中也使用了这个注解，我把对应的代码例子展示在下面的代码块中了，我知道大家现在肯定还不清楚 nacos 中这几个类的作用，能用这个例子掌握 @DependsOn 注解的使用方法就行了。请看下面代码块。  
好了，现在大家也清楚了 @DependsOn 注解的作用，当然也就知道怎么简单控制 springboot 容器管理的 bean 对象的创建顺序。照这么看，我们只需要在 GrpcSdkServer类上添加一个 @DependsOn 注解，并且在注解中定义 "envUtil" 字符串，那么 EnvUtil 对象就会先一步创建出来，这样一来，等到 GrpcSdkServer 对象需要从 EnvUtil 中获得用户自定义的配置信息时，就可以轻松获得了。在我看来，这个逻辑还是挺流畅的，相关操作实施起来也很简单，很容易就能实现。但是，我不得不说一句，在 nacos 服务端源码中，并不是这么做的。  
  
实际上在 nacos 源码中，把 nacos 服务器的启动和 springboot 程序启动结合起来了，确切地说，是 nacos 服务器利用了 springboot 启动过程中的各个扩展点为自己服务，执行了很多很有必要的操作。比如说，在程序刚启动的时候、springboot 的环境变量组件，也就是 ConfigurableEnvironment 准备完毕了、springboot 程序启动成功了、springboot 启动失败了等等阶段，nacos 服务端都可以执行一些操作 。说白了，就是 nacos 服务端可以在 springboot 程序启动的各个阶段，执行一些必要的操作，举一个最直接的例子， 当 springboot 的环境变量组件 ConfigurableEnvironment 准备完毕了，这个时候就可以直接使用 ConfigurableEnvironment 对象给 nacos 的 EnvUtil 组件的 environment 成员变量赋值 。而当 springboot 环境变量组件准备完毕时，springboot 管理 bean 对象的容器还没有创建呢，既然容器都没有创建，那些 bean 对象自然也都没有创建，所以这么做完全符合我们之前的分析： 要在 springboot 创建 bean 对象阶段之前，就把 springboot 管理的环境变量和配置信息交给 nacos 的 EnvUtil 组件 。  
  
分析到这里应该就有朋友会问了，nacos 服务端怎么就知道 springboot 启动到哪个阶段了呢？这个也很简答，springboot 早就为用户提供了一个监听器，用户只要实现这个监听器中的各个方法，这些方法就会在 springboot 启动的各个阶段被回调，执行用户自己定义的属于 nacos 服务端的操作。 这个 springboot 监听器就是非常著名的 SpringApplicationRunListener 。相信每个朋友都知道这个 SpringApplicationRunListener 监听器的作用， 这个监听器中定义了非常多的方法，监听 springboot 启动的不同阶段，springboot 程序每启动到一个阶段，SpringApplicationRunListener 监听器中对应的方法就会被回调 。我把这个 SpringApplicationRunListener 监听器的代码从 srpingboot 框架中搬运过来了，并且把该监听器中每个方法的回调时机写在注释中了，请看下面代码块。  
以上代码块展示的就是 springboot 提供的 SpringApplicationRunListener 监听器的全部内容。 SpringApplicationRunListener 只是一个接口，用户可以自己定义该接口的实现类，然后通过 springboot 的 spi 加载这个类，让这个监听器对象发挥作用 。当然，上面接口中我添加的注释比较少，其实这个监听器接口有非常多的内容可以讲解，如果我们是在更新 spring 和 springboot 的课程，我会花很长的篇幅为大家讲解这个监听器接口的功能和使用方式，但现在我们是在实现 nacos 服务端，所以我就简单添加一些注释，大家知道这个监听器对象中的各个方法会在什么时候被回调就行了。如果大家对这个功能感兴趣，可以直接去看 springboot 的源码，就以 SpringApplicationRunListeners、SpringApplication 这两个类作为突破口即可。  
  
好了，还是让我们言归正传吧，现在我们已经知道了 springboot 程序启动的各个阶段，并且也可以通过实现 SpringApplicationRunListener 监听器接口，定义自己的监听器对象，实现监听器对象的各个方法，nacos 服务端就可以在 springboot 启动的各个阶段执行相应的操作。这个编程思路已经确定了，那该怎么编写代码呢？接下来就轮到 nacos 自己的监听器对象登场了。  
  
引入 NacosApplicationListener 监听器  
  
我们的首要目标已经非常明确了，肯定是先为 nacos 自定义一个类，让这个类实现 springboot 的 SpringApplicationRunListener 监听器接口，这样一来，nacos 就可以使用 springboot 监听器的功能了。 比如说我就把这个类也定义为 SpringApplicationRunListener，让这个类实现 springboot 的 SpringApplicationRunListener 监听器接口 。请看下面代码块。  
可以看到，现在我已经实现了 nacos 自己的监听器对象， 并且我可以把这个监听器定义在 springboot 的 spring.factories 文件中，这样一来，nacos 自己的监听器对象就会被 springboot 容器管理，其内部定义的各个方法就可以在 springboot 启动的各个阶段被回调了 。与此同时，大家也能看到，我并没有实现 nacos 自己的监听器中的任何方法，所有方法都是空实现，这是因为我忽然意识到， 当用户在使用我们开发的 nacos 框架的时候，很可能需要自定义多个监听器对象，这些监听器对象中的各个方法都可以在 springboot 程序启动的各个阶段被回调 。如果仅仅为 nacos 定义一个 SpringApplicationRunListener 监听器对象，那扩展性也太低了，并且定义的这个 SpringApplicationRunListener 监听器也是 nacos 内置监听器，用户无法自定义自己的监听器对象。为了能让用户定义自己的监听器对象，并且这些监听器对象中的方法也可以在 springboot 程序启动的各个阶段被回调，我决定再引入一个新的接口，然后再对 nacos 自己的 SpringApplicationRunListener 类进行一下重构。  
  
要引入的接口就是 NacosApplicationListener 接口，这个接口就是 nacos 框架暴露给用户的接口，用户只要实现了这个接口，就意味着实现了一个监听器对象 ，这个接口的具体内容如下，请看下面代码块。  
可以看到，我刚刚定义的这个 NacosApplicationListener 接口中的方法和 nacos 自己的监听器 SpringApplicationRunListener 中的方法名称都一样。接口已经定义好了，那么用户定义的接口的实现类，也就是自定义的监听器对象怎么被执行呢？这就很简单了， 既然是用户自定义的 NacosApplicationListener 接口实现类，那就可以使用 spi 的方式把用户自定义的监听器对象加载到程序中，确切地说，是交给 nacos 的 SpringApplicationRunListener 监听器对象管理 。 当 nacos 的 SpringApplicationRunListener 监听器对象中的方法被回调时，直接执行用户定义的各个监听器中的同名方法 ，这样一来不就实现了我们想要的功能了吗？接下来就请大家看看我重构之后的 SpringApplicationRunListener 类，请看下面代码块。  
好了，到此为止，nacos 的监听器体系就构建得差不多了。监听器功能实现之后，现在我们想在 springboot 容器管理的 bean 创建之前，把 springboot 的 ConfigurableEnvironment 环境变量组件赋值给 EnvUtil 的 environment 成员变量，不就易如反掌了吗？ 直接实现 NacosApplicationListener 接口，自定义一个 nacos 的监听器对象，并且实现这个监听器对象的 environmentPrepared() 方法，在该方法中把 ConfigurableEnvironment 对象赋值给 EnvUtil 的 environment 成员变量 ，这个功能不就实现了吗？当然， 除了给 EnvUtil 的 environment 成员变量赋值，nacos 注册中心的服务端还有很多其他操作需要在 springboot 启动的不同阶段执行，所以在 nacos 中已经内置了一个监听器对象，那就是 StartingApplicationListener 监听器对象。这个 StartingApplicationListener 实现了 NacosApplicationListener 接口 ，该类的内容比较多，但是都非常简单，我就不为大家分析得那么详细了，大家直接看代码就能看得很明白。所以接下来，我就为大家把 StartingApplicationListener 类的内容展示一下，请看下面代码块。  
上面代码块注释添加的非常详细，通过上面代码块，大家就可以清楚地知道在 springboot 程序启动的时候，nacos 的服务端会在启动的各个阶段执行什么操作，定义的方法都非常简单，所以我就不再赘述了。当然，有一点还是值得补充的， 那就是上面代码块的第 135 行有这样一行代码：ApplicationUtils.setStarted(true) 。这行代码是在 started() 方法中被调用的，当 springboot 程序启动完毕时，监听器对象的 started() 方法才会被调用，这个流程大家早就清楚了。那么这个 ApplicationUtils 是什么呢？如果我没有记错，在之前讲解得 hippo4j 框架的时候，也出现了一个 ApplicationUtils 工具类，其实 nacos 的这个 ApplicationUtils 工具类和 hippo4j 的工具类作用都是一样的，毕竟是 hippo4j 借鉴了 nacos 的代码。 这个 ApplicationUtils 工具类的内容同样很多，但是逻辑非常简单，之所以定义这个工具类，主要是为了能让 nacos 随时可以使用 springboot 的一些功能，比如可以随时从 springboot 容器中获得想要的对象，或者是得到容器对象等等 。在我提供的第七版本代码中，我也把这个 ApplicationUtils 工具类实现了，并没有做任何删减，和源码一致。但是在文章中，为了让大家尽快了解这个 ApplicationUtils 类的内容和作用，接下来，我就简单展示该类的部分代码，请看下面代码块。  
在看了上面的代码块之后，大家应该就知道了在 StartingApplicationListener 监听器对象的 started() 方法中，调用 ApplicationUtils.setStarted(true) 这个方法的作用了吧？其实就是给 ApplicationUtils 类中设置程序启动完成的标志，那么设置这个标志的作用是什么呢？这就是后两章的内容了，到时候大家就明白了，好了朋友们，我们下一章见！  
  
附：这一章和前两章的内容都非常简单，内容也很少，就是几个注册中心服务端核心类和监听器对象。大家可以看得稍微快一点，后两章内容就开始多起来了，当然，仅仅是内容增多，同样没什么难度。只不过涉及到服务端维护管理客户端连接的方式时，需要仔细分析一下，其他的就没什么了，都是非常普通的逻辑。还有一点需要补充，那就是到此为止大家仍然没必要去看第七版本的代码，只看文章展示类就可以了。现在文章中实现的功能还是太少，也比较简单，等后两章写完了，实现得功能就比较多了，到时候大家直接去看第七版本代码即可。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/bd38wn2qpms5hv7q*  
*All content belongs to its respective owners and creators.*