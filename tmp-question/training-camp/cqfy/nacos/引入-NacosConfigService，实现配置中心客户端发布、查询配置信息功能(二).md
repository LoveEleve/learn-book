  
引入配置文件过滤器，实现配置文件的加密和解密  
  
上一章结尾我为大家分析了 NacosConfigService 类的 publishConfigInner() 方法， 跟大家说当 nacos 配置中心要把配置信息发布到服务端时，都会执行对配置文件加密的操作，而加密的操作就应该定义在 NacosConfigService 类的 publishConfigInner() 中 。原因很简单， 因为在这个方法中调用了 ClientWorker 类的 publishConfig() 方法，在调用该方法的时候，需要把配置文件的加密密钥传递进去 。就像下面代码块中展示的这样，请看下面代码块。  
package com.cqfy.nacos.client.config;  
  
  
  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/8/31

\* @Description:配置中心服客户端服务组件

\*/

@ SuppressWarnings ("PMD.ServiceOrDaoClassShouldEndWithImplRule")

public class NacosConfigService implements ConfigService {  
//省略该类的其他内容  
  
/\*\*

\* @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。

\* @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。

\* @Date:2024/8/31

\* @Description:配置中心客户端发布配置文件的方法，这个方法的参数中有一个casMd5字符串，如果客户端在发布配置信息时计算了配置文件的md5

\* 那么配置文件被服务端接收后，就会比较配置文件的md5和之前存储的配置文件的md5是否不一致，如果不一致说明客户端发布的配置文件是更新过的

\* 同时我们也能看到该方法中还有几个参数，比如说betaIps、tag等等，tag是配置信息的标签，是在dataId之下对配置信息更细粒度的划分

\* betaIps就是灰度发布的IP地址，这些内容在nacos主线章节中并不会涉及，等到我为大家更新nacos支线章节时，会为大家实现和这些参数相关的功能

\*/

private boolean publishConfigInner (String tenant,String dataId,String group,String tag,String appName,

String betaIps,String content,String type,String casMd5) throws NacosException {

//对配置文件所在组名判空，为空则使用默认组名

group \= blank2defaultGroup (group);

//判断配置文件是否合规

ParamUtils.checkParam (dataId,group,content);

//在这里使用客户端工作组件把配置文件发布到服务端

//ClientWorker类的publishConfig方法中传进去了一个encryptedDataKey参数

//但是我们目前根本不知道这个参数是从哪里得到的

return worker.publishConfig (dataId,group,tenant,appName,tag,betaIps,content,encryptedDataKey,casMd5,type);

从上面代码块中可以看到，虽然在 NacosConfigService 对象的 publishConfigInner() 方法中调用了 ClientWorker 类的 publishConfig() 方法，把配置信息发布给服务端，但是在执行 ClientWorker 类的 publishConfig() 方法时，需要向该方法中传进去一个 encryptedDataKey 参数，但是在 NacosConfigService 对象的 publishConfigInner() 方法中，我们根本就没有得到这个 encryptedDataKey 参数。这是因为我们还没有实现对配置信息进行加密的功能。接下来我就为大家实现这个功能。  
  
实际上，在 nacos 配置中心源码中，当客户端要向服务端发布配置信息的时候，并不会只对配置信息进行文件加密的操作，加密操作只是其中的一个环节。其实是这样的， 在 nacos 配置中心客户端启动的时候，会创建一个过滤器管理器对象，这个对象内部持有很多过滤器，构成了一个过滤链，当配置文件要被客户端发布到服务端之前，配置文件就要先经过过滤器管理器的过滤链处理，在处理的过程中，过滤链中的每一个过滤器都会对配置文件进行处理，而对配置文件进行加密操作，就是其中一个过滤器要执行的操作 。这就是配置中心客户端在发布配置信息到服务端之前，要对配置信息执行的重要操作。  
  
当然， nacos 配置中心所做的也只是构建了一个过滤链而已，过滤链中每一个过滤器，其实还是交给用户来实现了。也就是说，用户可以自定义任何过滤器，对要发布的配置信息执行相应的操作，就算要对配置信息进行加密的过滤器，或者说加密器，也是由用户自己定义的。用户自己定义的各种过滤器，最终会以 SPI 的方式加载到 nacos 配置中心的过滤器管理器中 。接下来我就为大家把具体的代码展示一下。  
  
首先是 nacos 配置中心暴露给用户的过滤器的借口，就是 IConfigFilter，请看下面代码块。  
用户可以为 IConfigFilter 该接口实现各种各样的实现类，这些过滤器实现类最终会以 SPI 的方式被加载到程序中，最后会被存储在 nacos 配置中心客户端的过滤器管理器中， 也就是我接下来要为大家展示的 ConfigFilterChainManager 类 ，请看下面代码块。  
在上面代码块中可以看到， 在 ConfigFilterChainManager 类的构造方法中，就使用 SPI 的方式收集到了用户定义的所有过滤器，并且在 ConfigFilterChainManager 类中还定义了一个 doFilter() 方法，在该方法中过滤器链就会对配置信息进行过滤处理 。 接下来我们只需要把 ConfigFilterChainManager 过滤器管理器定义为 NacosConfigService 类的成员变量，然后在 NacosConfigService 类的 publishConfigInner() 方法中把要发布的配置文件交给过滤器管理器处理 ，这样不就可以对配置文件进行加密了吗？具体实现请看下面代码块。  
好了，到此为止，对配置信息进行加密的操作，我就为大家实现完毕了，但是仅仅实现到这个程度是不够的，因为真正对配置文件进行加密的过滤器，我还没有实现呢。这时候我就要为大家解释一下了，实际上，在 nacos 配置中心客户端有一个已经定义好的，可以对配置信息进行加密和解密的过滤器，就是 ConfigEncryptionFilter 过滤器，该过滤器实现了 IConfigFilter 接口，并且会以 SPI 的方式被存储到过滤器管理器中。确切地说， ConfigEncryptionFilter 类的对象就是 nacos 配置中心客户端内置的对配置文件执行加密解密的组件 。该类的具体内容如下，请看下面代码块。  
这个 ConfigEncryptionFilter 类的内容非常简单，就是对配置信息执行加密和解密的操作，这没什么可讲的，代码中的注释也非常详细，所以我就不再重复讲解了。但是我也知道大家肯定都很疑惑，因为我刚才跟大家说了，配置文件的加密解密组件也是可以由用户自己定义的，但现在我展示的这个 ConfigEncryptionFilter 过滤器好想把加密解密的操作已经定义完毕了，那留给用户自由发挥的空间在哪里呢？  
  
这时候，就可以一起看看上面代码块的第 39 和 67 行的代码了，就是下面这两行，请看下面代码块。  
从上面代码块中可以看到，这两行代码就是对数据进行加密和解密操作的，并且加密和解密都用到了同一个组件，那就是 EncryptionHandler。现在就让我来为大家详细解释一下， 实际上在 nacos 配置中心有一个 EncryptionPluginService 接口，该接口的实现类就是提供加密服务的组件，并且该接口的实现类可以由用户自己定义，也就是说对配置文件进行加密的组件可以有用户自己定义，定义完毕的加密器就会以 SPI 的方式被 nacos 加载到程序内部 ； 除了这个 EncryptionPluginService 接口之外，在 nacos 配置中心客户端还定义了一个 EncryptionPluginManager 加密插件管理器，这个管理器在被创建的时候，就会以 SPI 的方式把用户定义的所有 EncryptionPluginService 加密器都加载到内存中，然后保存在自己内部。也就是说，nacos 程序一旦启动，用户自己定义的所有配置文件加密器都会被 EncryptionPluginManager 加密插件管理器来管理 。这又是 nacos 插件化思想的一个体现。  
  
那么，EncryptionPluginManager 得到了用户定义的所有加密插件之后会怎样呢？这时候就简单多了，在上面的代码块中不是已经展示了，EncryptionHandler 组件就是对配置信息进行加密和解密的，而用户定义的加密插件都被 EncryptionPluginManager 管理器保存着， 那么显然 EncryptionHandler 组件在给配置信息执行加密操作时，会从 EncryptionPluginManager 管理器中得到用户定义的加密插件，然后再执行加密操作 。这就是 nacos 配置中心客户端对要发布到服务端的配置信息执行加密操作的全流程，相关的代码我就不在展示了，因为代码也不多，而且内容都很简单，我就只在文章中给大家分析一下流程思路，大家按照我分析的流程去看代码即可。当然，我分析了这一大堆也有另外的原因，因为我在为大家提供的第十四版本代码中，并没有严格按照源码引入加密插件体系，只是从 nacos 提供的测试类中找了例子复制到 EncryptionHandler 组件中了。如果大家想看原汁原味的加密插件体系，大家可以直接去看 nacos 源码。源码中的内容就是我刚才分析的那些，非常简单，我相信每一位朋友都能看懂。好了，到此为止，nacos 配置中心客户端向服务端发布配置信息的功能，我就为大家全部实现完毕了。  
  
实现配置中心客户端从服务端查询指定配置信息功能  
  
好了，我们已经把 nacos 配置中心客户端发布配置信息的功能实现完毕了，接下来该实现客户端从服务端查询指定配置信息的功能了。有了配置发布功能做铺垫，配置信息查询功能实现起来就更简单了，还是定义一个请求对象，然后配置中心客户端把请求发送给服务端，服务端回复对应的响应即可。当然，响应中肯定封装着客户端要查询的配置信息。 因为配置信息查询的真正操作是在 ClientWorker 类的 getServerConfig() 方法中执行的，所以接下来我们只需要重构 ClientWorker 类的 getServerConfig() 方法即可 。重构的逻辑非常简单，就是创建一个请求对象，该对象封装了客户端要从服务端查询的指定配置信息，然后使用 RpcClient 客户端对象发送给服务端，然后接收到服务端回复的响应对象，响应中封装着客户端要查询的配置信息。按照这个逻辑来重构的话，我们首先要把和查询配置信息有关的请求对象和响应对象定义出来。这两个对象我也定义完毕了，接下来就展示给大家。  
  
首先是请求体，我把它定义为了 ConfigQueryRequest，请看下面代码块。  
然后是响应体，我把它定义为 ConfigQueryResponse，请看下面代码块。  
请求体和响应体定义完毕了，接下来就该按照我们之前分析的逻辑实现 ClientWorker 类的 getServerConfig() 方法了，请看下面代码块。  
从上面代码块的 getServerConfig() 方法中，我们可以看到向服务单发送了一个 ConfigQueryRequest 请求，然后从服务端得到一个 ConfigQueryResponse 响应，这个响应对象中封装着客户端指定要查询的配置信息。当然，getServerConfig() 方法中执行的操作不只是这些，我们还可以看到， 当从服务端接收到 ConfigQueryResponse 响应对象后，nacos 配置中心客户端又创建了一个 ConfigResponse 对象，然后把从服务端得到了已经加密过的配置信息，以及密钥都设置到这个新创建的 ConfigResponse 对象中了，最后把 ConfigResponse 对象返回了 。如果我没有为大家详细讲解和配置文件加密相关的操作流程，大家看到 ClientWorker 类的 getServerConfig() 方法后肯定会感到疑惑，不知道为什么要这么做， 但现在大家已经知道了从客户端发布到服务端的配置信息要加密之后才能发布，那也就能理解从服务端得到指定的配置信息后，要对配置信息进行解密操作的原因了 。当然，对配置信息进行解密的操作并不是在 ClientWorker 类的 getServerConfig() 方法执行的，而是在调用了 ClientWorker 类的 getServerConfig() 方法的外层方法中执行的，也就是在 NacosConfigService 对象的 getConfigInner() 方法中执行的，所以接下来我们就要重构一下 NacosConfigService 对象的 getConfigInner() 方法。重构的逻辑也非常简单， 只要再次使用过滤器对从服务端获取的配置信息进行解密即可 ，我把重构完毕的代码展示在下面了，请看下面代码块。  
到此为止我就把 nacos 配置中心客户端发布和查询配置信息的功能实现完毕了，可以看到，这两个功能实现起来非常简单，几乎不用怎么思考，模仿者注册中心来就能实现。当然，对于 nacos 配置中心来说，这两个功能并不是最重要的， 最重要的当然是客户端向服务端订阅配置信息的功能，其实就是客户端监听服务端配置信息变更功能。因为只有客户端监听了指定的配置信息之后，被监听的配置信息才能缓存在客户端内部，那么配置信息动态更新的功能也就可以实现了。对于配置中心客户端来说，这个才是最重要的功能 。在下一章，我就会为大家实现这个功能，好了朋友们，我们下一章见！  
  
  
附：现在大家已经可以去看我提供的第十四版本代码了，但是有一点我要解释一下，大家在阅读第十四版本代码的时候，会发现我提供的代码和 nacos 源码中，配置中心客户端的构建和我在文章中展示的稍微有点不一样，虽然都用到了 RpcClient 对象，但是在获得该对象之前，还做了点其他的操作；至于文章中展示的其他内容，就和第十四版本代码一样了。我之所以不在本章中把配置中心客户端实现的和源码一致，是因为对客户端进行重构需要掌握部分和配置信息订阅功能相关的知识，而这是下一章的知识，所以我就不在本章中继续对配置中心客户端进行重构了。第十四版本代码，大家愿意看就看看，不愿意看就再等等，反正内容也没多少，看文章也能看懂，等下一篇文章更新完毕了，我把配置中心客户端重构得和源码一致了，大家直接去看第十五版本代码即可，到时候就全清楚了。当然，出现这种情况主要也是因为我懒得改造源码了，因为十四十五版本代码联系非常紧密，而十四版本代码对应的内容又很简单，就偷了下懒，没有对源码的客户端部分进行改造，在这理跟大家说声抱歉了。  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/hkq56twxqlfvg7b0*  
*All content belongs to its respective owners and creators.*