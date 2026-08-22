在上一章结尾，我为大家展示了一个并不完善的、自己定义的 CompletableFuture 类，并且我在这个类中使用了同步锁保证并发安全。我把相关的代码再次搬运过来了，请大家简单回顾一下，请看下面代码块。  
package org.apache.rocketmq.namesrv.test;  
  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：自定义的CompletableFuture类，这个类是JDK1.8中的CompletableFuture的简化版，用于学习CompletableFuture的原理。目前这个类的内容大约是源码的三分之一。

\*/

public class CompletableFuture < T > implements Future < T > {  
//当前CompletableFuture对象要执行的任务的执行结果

volatile Object result;  
  
//阻塞的线程集合

private final List < Thread > waiters \= new ArrayList <> ();  
  
/\*\*

\* @课程描述:从零带你写框架系列中的课程，整个系列包含netty，xxl-job，rocketmq，nacos，sofajraft，spring，springboot，disruptor，编译器，虚拟机等等。

\* @author：陈清风扬，个人微信号：chenqingfengyangjj。

\* @方法描述：获取任务结果的方法，如果任务未完成，需要阻塞等待一段时间

\*/

@ Override

public T get () throws InterruptedException,ExecutionException {

Object r \= result;

if (r \== null) {

//因为多个线程可能会同时调用get方法

//会同时先waiters集合中存放线程数据，所以要使用同步锁保证并发安全

synchronized (waiters) {

//双重校验

if (result \== null) {

//把当前调用get方法的线程添加到集合中

waiters.add (Thread.currentThread ());

//阻塞线程

回顾了上一章重构玩完毕的 CompletableFuture 类之后，大家应该还记得我们这样重构 CompletableFuture 类的原因： 如果 CompletableFuture 类中定义的是一个 Thread 成员变量，而非一个存放 Thread 线程的集合，那么多个线程同时调用同一个 CompletableFuture 对象的 get() 方法时，Thread 只能记录最新调用 get() 方法的线程，如果多个线程同时调用了 CompletableFuture 对象的 get() 方法，那只有最后调用 get() 方法的线程会被记录下来，唤醒的时候也只有这个线程会被唤醒 。为了解决这个问题，我们最终使用存放 Thread 对象的集合代替了 Thread 成员变量，这样一来，每一个线程调用了相同 CompletableFuture 对象的 get() 方法后，如果当前 CompletableFuture 对象还没有执行结果，那么所有要被阻塞的线程都可以存放到集合中，等到要被唤醒的时候，再依次唤醒集合中的每一个线程即可。重构到这种程度，这个问题到此为止其实就已经算是解决了。这一点大家应该没有异议了吧？  
  
当然，大家完全可以再思考思考，除了使用集合搭配同步锁的方式，还有没有其他方式能解决刚才的问题呢？其实稍微想想就能意识到，这个问题的本质非常清晰： 首先必须使用某种数据结构，或者说某个容器记录每一个要阻塞的线程，而在记录这些线程的时候还需要采取某种同步措施保证并发安全 。这一点大家应该都认同吧？使用什么容器或者数据结构来记录线程我们可以先放在一边，先让我们来看看保证并发安全的几种方式，我相信在看了很多框架的代码之后，在大家脑海里一定会形成一种认知，那就是保证并发安全的手段非常简单，最常用的无非就是 CAS 原子操作和 Synchronized 同步锁。要想在并发操作某些数据时不出问题，选择这两个手段中的其中一种即可。如果再深入探讨一下，那我们还可以得出一个结论： 那就是在并发情况比较激烈时，也就是并发线程数量非常多时，最好使用 Synchronized 保证并发安全，线程竞争并不激烈时那就可以使用 CAS 。原因也很简单，因为 Synchronized 会让没获得锁的线程阻塞一段时间，而 CAS 并不会让线程阻塞，竞争资源失败直接进入下一次循环，多线程竞争某个资源时，可能会造成大量线程空转，浪费资源。所以，从同步机制上来说，并不意味着 Synchronized 比 CAS 优秀，或者 CAS 比 Synchronized 优秀，而是具体情况选择不同的方式，线程该阻塞的时候就必须阻塞。这些知识我相信大家也早就掌握了，所以我就不在这方面继续罗嗦了。  
  
好了，保证并发安全的两种方式已经分析完毕了，接下来让我们继续回到主题，回到上面展示的代码块种。在我们目前定义的 CompletableFuture 类中使用的是 Synchronized 同步锁，我相信大家肯定已经彻底掌握这种方式了，但在 JDK 源码中，使用的是 CAS 的方式保证并发安全，接下来我就使用源码的方式再次对我们定义的 CompletableFuture 类进行重构。当然，在开始重构之前还有一个小问题需要简单讨论一下，那就是在我们定义的 CompletableFuture 类中，使用一个集合来存储需要阻塞的线程的，那么除了集合还可以使用其他的容器，或者数据结构吗？ 比如说我想使用链表存储每一个需要阻塞的线程可以吗 ？当然是可以的，并且在 JDK 源码中就是这么做的，接下就请大家看看我是怎么仿照源码重构自己的 CompletableFuture 类吧。  
  
引入 Signaller 类  
  
我不知道大家对 JDK 的 ForkJoinPool 源码了解有多深，也许有的朋友对这个类掌握得非常透彻，也许有的朋友几乎没使用过这个类的功能， 总之接下来我们要实现的线程阻塞的功能需要 ForkJoinPool 类的帮助 ，所以请允许我为大家简单介绍一下 ForkJoinPool 类的相关功能。在 ForkJoinPool 类中存在一个接口，就是 ManagedBlocker 接口，具体内容请看下面代码块。  
既然 ManagedBlocker 是一个接口，那么这个接口肯定就会有实现类，并且这个实现类可以被用户自己定义。当然， 用户在定义 ManagedBlocker 接口的实现类时，肯定要在 block() 方法中实现阻塞当前线程的逻辑，在 isReleasable() 方法中，也要实现判断被阻塞的线程是否可以被唤醒的逻辑 。这一点大家应该也都可以理解吧？假如说我们目前就定义了一个 ManagedBlocker 接口的实现类， 就比如说这个实现类被定义为了 Signaller 类，也就是线程阻塞和唤醒的信号员的意思，负责线程的阻塞和唤醒，并且它还是 CompletableFuture 的内部类 ，就像下面代码块展示的这样，请看下面代码块。  
好了，现在 ManagedBlocker 接口的实现类有定义完毕了，那么当我们需要阻塞某个线程时，ForkJoinPool 会怎么帮助我们呢？这就要看看大家对 ForkJoinPool 类掌握到什么程度了。 实际上在 ForkJoinPool 类中提供了一个 managedBlock() 静态方法，从名字上就能看出来这个方法的作用就是管理线程阻塞的，当某个线程调用了 ForkJoinPool 类的 managedBlock() 方法时，需要向方法中穿入一个 ManagedBlocker 对象，也就是 ManagedBlocker 接口的实现类对象。这样一来调用了 ForkJoinPool 类的 managedBlock() 方法的线程就会阻塞了 。光这么说大家也许会觉得非常模糊，接下来我把相关的代码展示一下，大家就全都清楚了。请看下面代码块。  
到此为止，我相信大家都已经清楚了 ForkJoinPool 类是怎么让一个线程阻塞的。但是明白了这些显然还不够，因为真正的核心逻辑都定义在了 ManagedBlocker 接口的实现类中，也就是我们定义的 Signaller 类中，但就目前的情况来说，Signaller 类并没有被我们真正实现。所以就算我们知道了 ForkJoinPool 类的 managedBlock() 方法如何让一个线程阻塞，这也只意味着我们掌握了表面的流程，真正的核心逻辑还等着我们去实现呢。那么这个核心逻辑该如何实现呢？也就是说，这个 Signaller 类该定义什么内容呢？  
  
分析起来其实也没什么难度，我们已经知道了 Signaller 类要肩负的具体职责： 无非就是在这个类的 block() 方法中实现阻塞当前线程的逻辑，在这个类的 isReleasable() 方法中，实现判断被阻塞的线程是否可以被唤醒的逻辑 。这些其实都很容易实现，反正阻塞线程使用的就是 LockSupport 的 park() 方法，还是老一套，那我们实现 Signaller 类的 block() 方法时，就可以在这个方法中直接调用 LockSupport 的 park() 方法阻塞当前线程；当然，线程阻塞也分为限时阻塞和不限时阻塞，如果是限时阻塞，那就应该把阻塞时间也传到 LockSupport 的 park() 方法中。既然是这样，那这就意味着如果一个线程需要限时阻塞的话，那这个 Signaller 对象还需要掌握线程限时阻塞的时间，如果是这样， 那我索性就在 Signaller 类中定义一个 long 整数成员变量，变量名称为 nanos，用来记录要阻塞的线程的阻塞时间。在 block() 方法中可以先判断一下 Signaller 对象的 nanos 成员变量是否不为空，如果不为空则意味着是限时阻塞，如果为空则意味着是不限时阻塞 。根据不同的情况执行不同的操作即可。  
  
好了，到此为止，Signaller 类的 block() 方法的实现逻辑我们已经分析完毕了，接下来该分析分析 isReleasable() 方法的实现逻辑了。要实现这个方法就更简单了，我们已经知道了该方法的作用就是判断线程是否可以被释放，也就是被唤醒，那么线程什么时候可以被唤醒呢？这时候又要分为两种情况了，一种是线程是限时阻塞，这个就很好办了，那就判断这个限时阻塞的线程是否阻塞到截止时间了，如果到时间了，那就意味着这个线程可以被唤醒了，当然，我这里说的被唤醒指的是线程自动就脱离阻塞状态了， LockSupport.parkNanos(this, nanos) 这个方法的作用大家肯定都清楚。在这种情况下， 我打算再给 Signaller 类定义一个新的 long 整数成员变量，变量名称为 deadline，代表限时阻塞的线程要阻塞的截止时间 ；第二种情况就是线程不限时阻塞，要是这种情况的话，那就更简单了，因为线程不限时阻塞，所以不管经过多长时间，线程都不会主动脱离阻塞状态，除非被其他的线程唤醒。 而当某个线程一旦调用了 CompletableFuture 对象的 complete() 方法，所有阻塞的线程就都会被唤醒了 。所以当线程不限时阻塞的时候，isReleasable() 方法并不能对这个阻塞的线程执行什么操作，这不是 isReleasable() 方法要负责的工作。那我们就可以在 isReleasable() 方法中直接忽略第二种情况了。  
  
好了，到此为止，Signaller 类的 isReleasable() 方法的实现原理也分析完毕了。接下来就该从代码层面上真正实现 Signaller 类了。但在实现之前，我还有一个问题想问问大家，那就是目前我们分析的 Signaller 类的实现内容只和线程阻塞相关，但是从这个类的名字上来看，我显然是希望 Signaller 这个类能负责线程的阻塞和唤醒，但现在 Signaller 类中的内容只和线程的阻塞有关，假如 CompletableFuture 对象有了执行结果，线程需要被唤醒了，那作为线程阻塞和唤醒的信号员，Signaller 对象应该怎么唤醒线程呢？  
  
我的想法是在 Signaller 类中再定义一个新的方法， 那就是 tryFire() 方法，表示尝试执行的意思，在该方法中，被阻塞的线程就会被唤醒，这也就意味着该方法会在 CompletableFuture 对象有了任务结果后被调用，也就是在 CompletableFuture 对象的 complete() 方法中被调用 。既然是这样，那么 Signaller 类的 tryFire() 方法肯定就要知道哪个线程应该被唤醒，这也很好办， 在 Signaller 类中再定义一个新的 Thread 成员变量，用这个变量记录被阻塞的线程即可 。当 tryFire() 方法要唤醒某个线程时，直接根据 Thread 成员变量唤醒对应的线程即可。好了，这些都分析完毕之后，请大家看看我实现的 Signaller 类，请看下面代码块。  
好了，现在我已经把 CompletableFuture 的内部类 Signaller 类的内容实现完毕了，接下来就可以使用这个类重构 CompletableFuture 类的其他内容了。现在新的问题就来了，那就是这个 Signaller 类应该怎么使用呢？  
  
重构 CompletableFuture 类  
  
从目前的 Signaller 类来看，显然一个 Signaller 对象只能管理一个线程的阻塞与唤醒，因为在 Signaller 类中定义的是一个 Thread 成员变量，这个成员变量显然只能记录一个要被阻塞的线程的信息。如果多个线程同时调用了同一个 CompletableFuture 对象的 get() 方法，那该怎么办呢？其实非常简单， 那就为每一个要阻塞的线程都创建一个 Signaller 对象好了，在创建 Signaller 对象的时候，就可以把要阻塞的线程赋值给 Signaller 对象的 thread 成员变量，然后再把创建完毕的 Signaller 对象存储到 CompletableFuture 的 waiters 集合中，当然，这个时候 waiters 集合显然存放就应该是 Signaller 对象了，而不再是 Thread 对象 。现在大家应该清楚了为什么要在 Signaller 类的构造方法中使用 Thread.currentThread() 给 thread 成员变量赋值了吧？当然， 当 CompletableFuture 有了执行结果，也就是 CompletableFuture 对象的 complete() 方法被调用时，只需要在该方法中依次调用 waiters 集合中每一个 Signaller 对象的 tryFire() 方法，就可以唤醒所有被阻塞的线程了 。这个流程大家应该清楚了吧？如果大家清楚这些流程了，那接下来就让我为大家展示一下重构之后的 CompletableFuture 类，请看下面代码块。  
好了朋友们，到此为止，CompletableFuture 类就重构完毕了。上面代码块中的注释非常详细，所以我就不再重复讲解了。现在我要说的是，虽然我们对 CompletableFuture 类进行了一次重构，但还远远达不到我想要的效果。因为我们只是使用 Signaller 类把线程阻塞和唤醒的功能重构了，但在前面我跟大家说了， 在 JDK 源码中，并没有使用 Synchronized 同步锁来保证并发安全，使用的是 CAS，也没有使用 List 集合来存放要阻塞的线程对象，在 JDK 的 CompletableFuture 类中使用的是链表 。而这些内容并没有出现在我们自己定义的 CompletableFuture 类中，所以接下来我还要继续对 CompletableFuture 类进行重构，直到它和源码一致。那本章内容进行到这里，就要为我们自己定义的 CompletableFuture 类引入一个最关键的内部类了，那就是 Completion 类。  
  
引入 Completion 类  
  
我在上一小节结尾说要再引入一个 Completion 类，这个决定肯定让大家觉得内容有些跳跃，没做什么铺垫就直接蹦出来一个 Completion 类，我能理解大家的这种心情，接下来请大家稍安勿躁，耐心听我为大家解释一下这个 Completion 类的具体作用。  
  
从 Completion 类的名称上就能看出来，这个类的表示完成的意思，而它在源码中发挥的作用也和它的名字一模一样。实际上， 在 JDK 的 CompletableFuture 源码中，并没有定义专门存放 Signaller 对象的集合，而是把每一个 Signaller 对象包装成了一个 Completion 对象，如果有多个线程同时调用了同一个 CompletableFuture 对象的 get() 方法，那么就会创建多个 Completion 对象，然后以链表的方式把这些 Completion 对象连接起来，而链表的头节点就是 CompletableFuture 类的成员变量。这样一来，等到 CompletableFuture 有了执行结果，就可以直接根据链表的头节点，依次执行每一个 Completion 对象中的指定方法，将被阻塞的线程唤醒 。我知道光是文字描述肯定还是会让大家觉得很模糊，接下来就让我先为大家展示一下要引入的这个 Completion 类的内容。  
  
在源码中这个 Completion 也是 CompletableFuture 的内部类，为了和源码保持一致，我也把 Completion 定义为我们自己的 CompletableFuture 的内部类。就目前的情况来说， Completion 类中的内容非常简单，只需要定义一个 tryFire() 方法，还需要定义一个 Completion 成员变量，作为指向链表下一个节点的指针 。我相信大家都明白在 Completion 类中定义一个 Completion 成员变量作为指针的作用，但不一定清楚定义 tryFire() 方法的作用。而且这个 tryFire() 方法和 Signaller 类中的方法显然同名了呀。接下来就让我来解释一下，我之前说在 JDK 源码中会把 Signaller 对象包装成 Completion 对象，这么说其实不太准确。确切地说， 一个 Signaller 对象其实就是 Completion 对象，因为在 JDK 源码中 Signaller 继承了 Completion 类，并且实现了 Completion 类中的 tryFire() 抽象方法 。而 Completion 类之所以被定义为 Completion 这个名字，是因为这个 Completion 对象的 tryFire() 方法会在 CompletableFuture 有了执行结果后被调用，也就是在 CompletableFuture 对象任务完成之后，Completion 对象的 tryFire() 方法就会被调用，这也就意味着每一个 Signaller 对象的 tryFire() 方法就会被调用，那些阻塞的线程就会被唤醒了。好了，在分析完 Completion 类的作用之后，接下来就请大家看一下我新引入的这个 Completion 类的具体内容，请看下面代码块。  
好了，现在 Completion 内部类也引入进来了，那怎么使用这个内部类再次重构 CompletableFuture 类呢？现在已经明确了要使用链表的方式把所有 Completion 对象连接起来，也就是把所有的 Signaller 对象连接起来，那就不能在 CompletableFuture 类中继续使用存放 Signaller 对象的集合了，所以这个成员变量要取消了。 确切地说，这个成员变量应该被一个 Completion 对象取代，而新定义在 CompletableFuture 类中的这个 Completion 成员变量就是链表的头节点 。这一点想必大家都能理解吧？如果这个逻辑理解了，那么接下来就很好说了，直接使用链表重构 CompletableFuture 类吧，重构的思路也很简单： 就是在 get() 方法或者 get(long timeout, TimeUnit unit) 方法中判断需要阻塞当前线程时，创建了对应的 Signaller 对象之后，直接把 Signaller 对象设置为链表的头节点好了，之前的头节点变成链表的第二个节点即可 。 这也就意味着在组成链表的时候使用的是头插法，也就意味着最先调用 CompletableFuture 对象 get() 方法的线程，其对应的 Signaller 对象就是链表的尾节点，既然是尾节点，那么最后沿着链表的每一个节点唤醒被阻塞的线程时，最先被阻塞的线程会在最后被唤醒 。如果大家理解了这个操作，那么大家可以继续想一想，这个操作是不是让这个链表成为了一个栈，也就是说先进来的 Completion 对象，它的 tryFire() 方法会在后面执行，先进后出这不就是一个栈吗？没错吧？所以在 JDK 源码中把 CompletableFuture 类中 Completion 成员变量的名称定义为了 stack，而在我们自己的 CompletableFuture 类中，我也会这么做。  
  
好了，现在链表的构成方式已经分析完毕了，接下来就该分析一下怎么使用 CAS 重构 CompletableFuture 类了，这个也很好说，之前我们在 get() 方法中使用同步锁，是为了避免多线程同时操作集合时出现并发问题，现在我们使用一个链表取代了集合，而多线程操作链表时，会在设置链表头节点时出现并发问题， 所以我们这时候使用 CAS，使用原子操作更新链表头节点 ，这不就保证并发安全了吗？好了，这一点分析完毕之后，接下来就可以真正重构 CompletableFuture 类了，请大家看看我重构之后的 CompletableFuture 类，请看下面代码块。  
到此为止，我们就按照源码的方式，使用 CAS 和链表将 CompletableFuture 类重构完毕了。上面代码块中的内容非常详细，我也就不再重复讲解了。本章的核心内容到这里也就讲解完毕了，当然，这并不意味着我们定义的 CompletableFuture 类已经迭代到了最终版本，实际上它离最终版本还差的很远，别忘了上一章我最后我针对尚未完善的 CompletableFuture 类提出了五个问题呢？其中第三个问题已经被我们解决了，那还剩下四个问题呢，我把剩下的四个问题再次搬运过来了，请大家简单回顾一下：  
1 目前的 CompletableFuture 类并不能处理执行结果为 null 或者为异常的情况。  
2 假如多个线程同时调用了 CompletableFuture 类的 complete() 方法，同时操作 result 成员变量的时候，很容易出现并发问题。  
3 线程也是可以被中断的，一个线程调用了 CompletableFuture 的 get() 方法，阻塞等待任务结果，但是也可以在等待的过程中被中断，取消等待操作，这个功能我们并没有实现。  
4 限时阻塞的 get() 方法在等待超时后，没有对结果进行是否为 null 判断，也没有抛出超时异常 。  
那以上四个问题应该怎么解决呢？其实第一和第二个问题很容易解决，仍然使用 CAS 即可， 也就是原子操作给 CompletableFuture 的 result 成员变量赋值 。至于异常情况，那就直接根据执行结果来抛出对应异常即可。接下来就请大家看一下我再次重构之后的 CompletableFuture 类，请看下面代码块。  
上面代码块的逻辑并不难理解，并且注释非常详细，我也不再重复讲解了。总之，看完上面的代码块之后，我相信大家对上一章给出的一个测试类肯定豁然开朗了，就是下面的这个测试类，请看下面代码块。  
现在大家应该知道为什么明明使用 TimeoutException 超时异常设置了执行结果，结果运行测试类后在控制台输出了 ExecutionException 异常。这是因为使用异常为 CompletableFuture 的执行结果赋值后，thenApply() 方法中的任务就不会执行了，而是会把异常传递给 get() 方法注册到 CompletableFuture 对象，这也就意味着 get() 方法注册的 CompletableFuture 对象已经有了执行结果，而 get() 方法发现自己注册的 CompletableFuture 对象已经有结果了，并且结果是一个异常，然后就会在 get() 方法中执行 reportGet() 方法，在该方法中判断出没有和 TimeoutException 相匹配的异常类型，于是就会抛出 ExecutionException 异常。现在大家应该清楚程序的内部逻辑了。至于为什么 cf 对象的执行结果为异常，thenApply() 中的任务就不会执行，这就是下一章的内容了，就留到下一章讲解吧。好了朋友们，这一章的内容够多了，我就不再继续往下写了，大家可以认真看看文章，消化一下，把本章的核心内容掌握了，如果这一章的内容没有掌握，那么下一章的内容肯定看不懂，因为下一章我就要实现 CompletableFuture 类最核心的功能，也就是注册以及执行 CompletableFuture 任务链的功能，而这一功能就建立在本章内容之上。好了朋友们，我们下一章见！  
若有收获，就点个赞吧

---
*Source: https://www.yuque.com/u26328320/kxtdy3/gdbsumy9akq0wcgx*  
*All content belongs to its respective owners and creators.*