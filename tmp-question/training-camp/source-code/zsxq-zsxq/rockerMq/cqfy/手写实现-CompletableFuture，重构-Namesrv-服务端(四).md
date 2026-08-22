  
实现复杂的 CompletableFuture 任务链功能  
  
上一章结尾我给大家展示了一个测试例子，简单介绍了一下什么是复杂的 CompletableFuture 任务链功能。其实这个功能我在上一章前半部分就给大家展示过了，当时我也展示的也是相同的测试例子，请看下面代码块。  
package org.apache.rocketmq.namesrv.test;  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：自定义的CompletableFuture测试类

\*/

public class Test {

public static void main (String \[\] args) throws ExecutionException,InterruptedException {

//创建初始 CompletableFuture

CompletableFuture < String > cf \= new CompletableFuture <> ();  
  
CompletableFuture < String > chain1 \= cf.thenApply (s -> {

System.out.println ("Chain 1 Task 1: " + s);

return s + "扬";

});  
CompletableFuture < String > chain2 \= chain1.thenApply (s -> {

System.out.println ("Chain 2 Task 2: " + s);

return s + "扬";

});  
  
CompletableFuture < String > chain3 \= chain2.thenApply (s -> {

System.out.println ("Chain 3 Task 3: " + s);

return s + "扬";

});  
//触发任务链执行

cf.complete ("陈清风");  
System.out.println (chain3.get ());

从上面代码块中可以看到， 当 CompletableFuture 对象调用了它的 thenApply() 方法之后，就会返回一个新的 CompletableFuture 对象，然后又可以调用新对象的 thenApply() 方法，注册新的回调方法。并且按照这种方式注册的回调任务，执行顺序也是固定的，那就是 Task 1 会最先被执行，Task 2 第二个执行，Task 3 最后执行，也就是说，会按照回调方法注册的顺序进行回调。还有一点要强调的是，通过 thenApply() 方法注册的回调方法都有返回值，并且该方法的返回值会传递给下一个要被回调的方法 。  
  
看到这里，大家可能会有一点困惑，上一章我们实现的 CompletableFuture 简单任务链功能，在这个功能中所有回调方法不都是按照栈先进后出的原则被调用执行吗？也就是先注册的回调方法会在最后才被执行，为什么在上面的代码块中确是先注册的回调方法先执行呢？这显然没有遵循栈结构啊。这一点其实很好解释，在上一章实现的 CompletableFuture 简单任务链功能中，所有回调方法都是注册到同一个 CompletableFuture 对象中的，所有回调方法都放在了同一个 CompletableFuture 对象的任务栈中，而在现在展示的 CompletableFuture 复杂任务链功能中，回调方法注册在了不同的 CompletableFuture 对象中。这样一来情况就发生变化了。请听我给大家详细分析一下。  
  
在上面代码块中， 当 cf 对象第一次调用 thenApply() 方法的时候，就会把回调方法封装成一个 Completion 对象，然后把该对象注册到 cf 对象的任务栈中，与此同时也会创建一个新的 CompletableFuture 对象，这个新对象的执行结果就是回调方法的返回值 。这些逻辑大家肯定已经很清楚了，这个时候在 cf 内部，任务栈的结构是这样的，请看下面代码块。  
以上代码块展示的就是 cf 对象第一次调用了 thenApply() 方法之后的任务栈情况，并且执行了该方法之后，还返回了一个新的 CompletableFuture 对象，也就是 chain1 对象，这个对象除了会返回给用户，还会被封装 Task1 任务的 Completion 对象引用。这一点大家应该清楚吧？上一章我们已经实现了对应的功能。  
  
好了，如果刚才的逻辑大家都理解了，那么我们接着往下分析，在 cf 调用完 thenApply() 方法之后，接下来就没有 cf 对象的事了，因为接下来注册的回调方法是注册到了新创建的 chain1 对象上。当 chain1 对象调用完 thenApply() 方法之后，其内部的任务栈的结构如下所示，请看下面代码块。  
这个操作也会返回一个新的 CompletableFuture 对象，也就是 chain2 对象，该对象除了会返回给用户，还会被封装 Task2 任务的 Completion 对象引用。这个逻辑也能理解吧？剩下代码的逻辑和我刚才分析的两个是一模一样的，所以我就不再继续分析了，总之，上面测试例子中的代码一旦开始执行，在 cf 对象没有调用 complete() 方法之前，各个 CompletableFuture 对象内部的任务栈情况就是下面展示的这样，请看下面代码块。  
从上面的代码块中可以看到， 新创建的这些 CompletableFuture 对象似乎都没有直接的联系，确切地说，是封装了每一个回调方法的 Completion 对象似乎没什么直接联系 。在之前实现的功能中，所有回调方法都被注册到同一个 CompletableFuture 对象中，所以封装了回调方法的所有 Completion 对象能存放到相同的 CompletableFuture 对象的任务栈中。但现在的情况是各个回调方法注册到不同的 CompletableFuture 对象中，那在这种情况下，从上面测试例子的执行结果可以看到，一旦 cf 对象调用了它的 complete() 方法，各个回调方法就会按照注册顺序先后被执行，那这是怎么回事呢？从执行结果上看，各个回调方法虽然注册到了不同的 CompletableFuture 对象中，但是它们之间肯定存在了某种联系，使它们也能像拴在一条链子上那样，有序地丝滑执行。那它们之间究竟是怎么联系的呢？接下来还是请大家听我慢慢分析。  
  
掌握了上一章的内容之后， 大家肯定都知道当我们把一个回调方法注册到一个 CompletableFuture 对象中时，会为这个回调方法创建一个对应的新 CompletableFuture 对象，并且这个新的 CompletableFuture 对象还会被封装了回调方法的 Completion 对象持有，而这个封装了回调方法的 Completion 对象又会被加入到其依赖的 CompletableFuture 对象的任务栈中 。重点来了，如果按照刚才的测试例子来分析，cf 对象可以从自己的任务栈中得到封装了 Task1 任务的 Completion 对象，而通过 Completion 对象则可以得到 Task1 回调方法对应的 CompletableFuture 对象，也就是 chain1 对象，而通过这个 chain1 对象则可以得到注册期内部的 Task2 回调方法。这样分析下来，我们是不是可以认为 cf 对象其实是可以得到用户定义的 Task2 任务，以及 Task3 任务呢？我把对应的逻辑链展示在下面了，请看下面代码块。  
以上代码块中的逻辑链应该很清晰了吧？如果上面的逻辑大家都清楚了，那么接下来大家就可以思考一下，怎么在代码层面按顺序候获得每一个回调方法呢？按照源码来看， 所有的回调方法肯定是按照注册顺序执行的 ，那我们也这么实现吧，先执行 Task1，再执行 Task2，再执行 Task3，一个接一个执行。这该怎么实现呢？让我们接着分析，执行 Task1 的时候就要先从 cf 的任务栈中得到封装 Task1 的 Completion 对象，调用该对象的 tryFire() 方法。在该方法中会执行用户定义的回调方法，也就是 Task1；接下来就该执行 Task2 任务了，而我们都知道， 封装了 Task2 任务的 Completion 对象会存放到 chain1 这个 CompletableFuture 对象的任务栈中，而 chain1 则被封装 Task1 的 Completion 对象持有 ，既然是这样， 那直接在封装 Task1 的 Completion 对象执行了它的 tryFire() 方法之后，把成员变量 chain1 返回出去不就好了？这就就可以通过 chain1 得到 Task2 了，然后就可以执行第二个回调方法了 。接下来就按照这个流程依次执行每一个回调方法即可。所以，根据这个逻辑，我们可以对 UniApply 类的 tryFire() 方法进行简单重构，在该方法中判断一下， 如果当前 Completion 的 future 成员变量，也就是要注册下一个回调方法的 CompletableFuture 对象，如果该对象上确实注册了下一个回调方法，那就把这个对象返回出去，然后从这个对象中得到下一个要执行的回调方法，再执行回调方法即可 。我把重构之后的代码展示在下面了，请看下面代码块。  
上面代码块中的注释非常详细，我就不再重复讲解其中的内容了，总之，到此为止 UniApply 类的 tryFire() 方法就重构完毕了，但只是这个方法重构完毕没用，我们虽然把能获得下一个回调方法的 CompletableFuture 对象返回到外层方法了，外层方法主动从这个 CompletableFuture 对象中获得下一个要被回调的方法，然后执行它才行啊。但现在执行 UniApply 类的 tryFire() 方法的外层方法是什么样的呢？CompletableFuture 类的 postComplete() 就是外层方法，也许这个方法也需要简单重构，所以先让我们看看这个 postComplete() 方法目前是怎么实现的，请看下面代码块。  
这种感觉真棒！因为我在回顾了我们之前实现的 postComplete() 方法的内容之后，我欣喜地发现这个方法根本不必重构。原因很简单，在上面代码块的第 45 行我们可以看到，每一个 Completion 对象执行了它的 tryFire() 方法之后，都会返回一个 CompletableFuture 对象， 如果 Completion 对象封装的是阻塞的线程信息，那么它的 tryFire() 方法就会返回 null，但如果是在复杂的 CompletableFuture 任务链情况中，Completion 对象就有可能返回一个注册了下一个回调方法的 CompletableFuture 对象 。这两种情况都会让程序进入下一次循环，然后执行对应的操作。所以这个 postComplete() 方法根本不必重构，仍然使用原来的内容即可，它已经能够链式执行所有注册到不同 CompletableFuture 对象中的回调方法了。到此为止，复杂的 CompletableFuture 任务链功能也就实现完毕了。  
  
朋友们，实际上到这里为止，CompletableFuture 类的核心内容就全部结束了。当然，我们自己实现的 CompletableFuture 类的内容和源码比起来肯定还不完善，因为我只实现了同步情况下 CompletableFuture 执行回调方法的功能，和异步相关的任何功能我都没有引入，如果真要引入的话，恐怕我就要再为这个小小的 CompletableFuture 类写上很多篇文章，由此也可以看出，这个 CompletableFuture 类确实是有一定难度，要完全掌握并非易事。但我们确实没那么多时间投入到这个类上了，还有很多 RocketMq 的内容等着我为大家讲解和实现，所以 CompletableFuture 类的其他内容，就让我抽时间为大家补充完整吧。当然，我这么说并不意味着这一章就真的要结束了，实际上这一章还结束不了，因为还有一些 CompletableFuture 类的内容要补充给大家，并且这一部分的内容也并不那么容易理解的。  
  
实现 CompletableFuture 简单复杂任务链结合使用的功能  
  
接下来请打击思考另一个情景，在此之前我们已经实现了 CompletableFuture 简单任务连功能，CompletableFuture 复杂任务链功能，并且也为这两个功能列举了测试例子，那现在我就想把简单任务链功能和复杂任务链功能结合到一起使用呢？就像下面代码块展示的这样，请看下面代码块。  
上面测试例子中的操作非常简单，在实现了简单 CompletableFuture 任务链功能和复杂 CompletableFuture 任务链功能之后，大家肯定都清楚上面每一行代码的作用，并且应该也能在脑海中构建出来，CompletableFuture 任务链的具体情况。当 cf 对象首先调用了 thenApply() 方法的时候，就会把 Task1 任务封装成 Completion 对象存放到 cf 的任务栈中，与此同时又返回了一个新的 CompletableFuture 对象，然后向该对象中注册了 Task11 这个任务。如果只分析上面代码块第 7—13 行代码的流程，那么这段代码执行完毕之后，cf 任务链的情况可以简化成下面这样，请看下面代码块。  
上面代码块中的任务链大家应该都明白是怎么回事吧？我就不详细展开分析了，如果上面代码块中任务链的逻辑大家都清楚了，那么后面的就很好说了，因为 cf 后面又开始向自己内部注册回调方法，但执行的操作和上面代码块第 7—13 行代码的操作一模一样，所以最后分析下来，cf 对象任务链的情况应该是下面这样的，请看下面代码块。  
好了，上面代码块就是测试例子中 cf 任务链的详细情况，大家可以结合我给出的任务链情况，自己梳理一下其中的逻辑，如果大家把前面几章的内容都掌握了，我相信每一位朋友都能知道任务链是怎样构建的。那现在 cf 任务链的构建情况有了，当 cf 对象调用了它的 complete() 方法之后，任务链中的每一个回调方法都要被执行了。那执行的会按照什么顺序执行呢？根据我们之前实现的功能来看，肯定是会先执行 Task3 这个方法，这个方法执行完毕之后，会得到为这个 Task3 方法创建的 CompletableFuture 对象，然后判断出这个 CompletableFuture 对象上确实注册了回调方法，所以接下来就会执行 Task33 方法；这个方法执行完毕之后，按照链条中节点的顺序，程序显然就该执行 Task2 任务了，然后是 Task22，之后以此类推即可。所以，我认为测试例子的执行结果应该是这样的，请看下面代码块。  
非常幸运的是，当我使用 JDK 的 CompletableFuture 源码执行上面的测试例子，结果和我预想得完全一致，这就证明了我们的推理没错，程序就应该这么执行，那接下来我们就应该再回过头看一看 CompletableFuture 类的 postComplete() 方法，看看这个方法目前的内容是否满足需求，是否足以支撑程序执行出上面代码块中的结果，我把推理的逻辑写在下面的代码注释中了，请看下面代码块。  
上面代码块展示完毕之后，我们会发现我们的运气真的不错，因为 CompletableFuture 类的 postComplete() 方法仍然不需要重构，目前的 postComplete() 方法完全可以支撑程序运行，最终得到和源码一致的结果。要是这么说的话，那么这个 CompletableFuture 简单复杂任务链结合使用的功能我们似乎也已经实现了，并且根据这个例子的执行结果我们还可以发现： 如果是简单、复杂任务链功能仪器使用的话，那么在执行注册到 CompletableFuture 中的回调方法时，似乎是先执行复杂任务链功能的方法，然后再执行简单任务链功能的方法 。就比如说上面的例子， 总是先执行任务链分支上的回调方法，因为分支上的回调方法注册到了不同的 CompletableFuture 对象中，分支执行完毕了，再回归主线，执行注册到同一个 CompletableFuture 任务栈中的回调方法 。这个执行流程很清晰吧？看起来确实是这样的，但我要说的是，这次只不过是凑巧罢了，也可说就是运气好，CompletableFuture 简单复杂任务链结合使用的情况分很多种，刚才我为大家展示的只是其中一种，而 CompletableFuture 类的 postComplete() 方法恰好支持这种情况，如果换另一种情况，CompletableFuture 类的 postComplete() 方法就不一定支持了。就比如说下面展示的这种情况，请看下面代码块。  
好了，闲话少说，在阅读了上面代码块的内容之后，接下来就让我先为大家把 cf 对象的任务链情况展示一下，请看下面代码块。  
上面代码块中的任务链非常简单，大家肯定也能看出来，这个任务链的情况并不全面，只对应测试例子中 5—21行的代码，后面还有几行代码的内容没有展示。我先把简单的任务链展示出来也是为了让大家理解得更顺畅，现在大家可以看到，如果只是使用了 CompletableFuture 复杂任务链功能，也就是把各个回调方法注册到不同的 CompletableFuture 对象中，那么任务链情况就是上面代码块中展示的那样。最后任务执行情况肯定是 Task1、Task2、Task3 顺序执行。但在测试例子中还执行了一些操作， 那就是继续向 chain1、chain2 中注册了新的回调方法，而按照我们之前分析的，向 CompletableFuture 中注册多个回调方法时，会把这些回调方法放到 CompletableFuture 的任务栈中，使用的是头插法 。所以按照这样分析，cf 任务链的结构就变成了下面这样，请看下面代码块。  
上面代码块中的任务链应该很清晰了吧？如果大家都理解了其构建原理，那么接下来请大家思考一下，按照上面任务链的顺序，各个回调方法的执行顺序应该是怎样的呢？当 cf 调用了它的 complete() 方法之后，肯定是 Task1 方法先被执行，然后是 Task11，然后是 Task2，然后是 Task22，最后是 Task3。这么分析应该没什么问题吧？但我可以先告诉大家，使用 JDK 源码执行上面的测试例子之后，得到的执行结果和我们分析的根本不一样，我把执行结果展示在下面了，请看下面代码块。  
可以看到，回调方法确实没有按照我们分析得那样执行，那为什么会以这样的顺序执行呢？我相信很多朋友都会感到困惑，其实不止是大家感到困惑，我一开始也非常困惑，不知道为什么会是这样，因为从上面的展示的任务链的结构上来看，回调方法确实应该像我们分析的那样执行啊，后来我转变了一下思路，把任务链稍微旋转了一下，就沿着顺时针旋转 90 度，然后我就知道为什么源码的会给出上面代码块中的执行结果了。接下来，请大家看一下旋转之后的任务链结构，请看下面代码块。  
从上面代码块中可以看到，现在的 cf 任务链结构似乎变成了一颗树，从顺序上来看，树的左侧节点先执行，右侧节点后执行，如果用一点专业的话来解释： 那么我们可以称在 CompletableFuture 对象内部，是采用深度优先遍历的方式来执行所有回调方法的 。现在大家应该清楚这一切是怎么回事了吧？好了，这个内容介绍完毕了之后，大家也清楚了 CompletableFuture 内部的回调方法究竟该怎么执行之后，那接下来我们还要再回到 CompletableFuture 类的 postComplete() 方法，看看这个方法目前是否支持程序这样运行。当然，我其实可以直接告诉大家了，目前 CompletableFuture 类的 postComplete() 方法根本不能让上面的测试例子得到和源码一致的结果，所以这个 postComplete() 方法肯定需要重构。而重构之后的 postComplete() 方法我也展示出来了，并且在该方法中添加了非常详细的注释，请看下面代码块。  
上面代码块中的注释非常详细，我就不再为大家重复解释了，大家自己看看文章和我提供的第二版本代码即可。到此为止，整篇文章就结束了，虽然还有很多 CompletableFuture 类的内容没有讲解，但是在我提供的代码中，大家可以看到很多方法都添加了详细注释，比如取消任务的方法，终端任务的方法，以及重构之后的可以被中断的 get() 方法，还记得之前遗留的两个问题吗？就是下面这两个：  
1 线程也是可以被中断的，一个线程调用了 CompletableFuture 的 get() 方法，阻塞等待任务结果，但是也可以在等待的过程中被中断，取消等待操作，这个功能我们并没有实现。  
2 限时阻塞的 get() 方法在等待超时后，没有对结果进行是否为 null 判断，也没有抛出超时异常 。  
这两个功能我就不在文章中为大家展示了，我提供的代码中注释很详细，就留给大家自己去阅读查看吧，我实在是没有精力把所有内容都展示在文章中了。还有一点要强调的是，我并没有为大家展示 whenComplete() 方法的实现逻辑，以及该回调方法的执行顺序，我想补充的是， whenComplete() 方法和 thenApply() 以及 thenAccept()、exceptionally() 方法的实现逻辑都是一样的，都是把回调方法封装成 Completion 对象，然后添加到其依赖的 CompletableFuture 对象的任务栈中，执行的时候也是按照我们刚才分析的深度优先遍历执行的，不管是什么回调方法，最后都是封装成 Completion 对象，然后被执行 。当然，回调方法具体执行的时候可能会有一些不同，不如说 thenApply() 注册的回调方法都有返回值，并且该回调方法在异常情况下不会执行，而 whenComplete() 方法注册的回调方法没有返回值，并且该回调方法可以处理程序执行过程中的异常。这些都是 CompletableFuture 本身的知识了，我就不再多说什么了。  
  
最后，我还想再说一句，我为大家实现的 CompletableFuture 类和源码比起来，难度降低了很多，展示的内容也只有三分之一，我实现的只是同步执行回调方法的内容，异步执行的功能我都没有实现，如果要实现这一部分功能，那么要讲解的源码就更多了，现在确实没有这个精力和时间。我是仿照 JDK 21 版本来定义我们自己的 CompletableFuture 类的，如果大家在阅读了我的文章之后，想继续深入了解 CompletableFuture 源码，那可以直接去看 JDK 21 版本的 CompletableFuture 源码。好了朋友们，这一章就到此为止吧，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/aux6wo8xs7k3oa6x*  
*All content belongs to its respective owners and creators.*