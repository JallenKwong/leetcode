# 线程安全与锁优化 #

[1.概述](#概述)

[2.线程安全](#线程安全)

[2.1.Java语言中的线程安全](#java语言中的线程安全)

[2.1.1.不可变](#不可变)

[2.1.2.绝对线程安全](#绝对线程安全)

[2.1.3.相对线程安全](#相对线程安全)

[2.1.4.线程兼容](#线程兼容)

[2.1.5.线程对立](#线程对立)

[2.2.线程安全的实现方法](#线程安全的实现方法)

[2.2.1.互斥同步](#互斥同步)

[2.2.2.非阻塞同步](#非阻塞同步)

[2.2.3.无同步方案](#无同步方案)

[2.2.4.可重入代码](#可重入代码)

[2.2.4.1.线程本地存储](#线程本地存储)

[3.锁优化](#锁优化)

[3.1.自旋锁与自适应自选](#自旋锁与自适应自选)

[3.2.锁消除](#锁消除)

[3.3.锁粗化](#锁粗化)

[3.4.轻量级锁](#轻量级锁)

[3.4.1.对象头部分](#对象头部分)

[3.4.2.轻量级锁的执行过程](#轻量级锁的执行过程)

[3.5.偏向锁](#偏向锁)

## 概述 ##

在软件业发展的初期，程序编写都是以算法为核心的，程序员会把数据和过程分别作为独立的部分来考虑，数据代表问题空间中的客体，程序代码则用于处理这些数据，这种思维方式直接站在计算机的角度去抽象问题和解决问题，称为面向过程的编程思想。与此相对的是，**面向对象的编程思想是站在现实世界的角度去抽象和解决问题，它把数据和行为都看做是对象的一部分，这样可以让程序员能以符合现实世界的思维方式来编写和组织程序**。

面向过程的编程思想极大地提升了现代软件开发的生产效率和软件可以达到的规模，但是显示世界与计算机世界之间不可避免地存在一些差异。例如，人们很难想象现实中的对象在一项工作进行期间，会被不停地中断和切换，对象的属性（数据）可能会在中断期间被修改和变 “脏”，而这些事件在计算机世界中则是很正常的事情。有时候，良好的设计原则不得不向现实做出一些让步，**我们必须让程序在计算机正确无误地运行，然后再考虑如何将代码组织得更好，让程序运行得更快**。对于这部分的主题 “高效并发” 来讲，首先需要保证并发的正确性，然后在此基础上实现高效。

## 线程安全 ##

“线程安全” 这个名称，扑朔迷离。

《Java Concurrency In Practice》的作者 Brian Goetz 对 “线程安全” 有一个比较恰当的定义：“当多个线程访问一个对象时，如果

1. 不用考虑这些线程在运行时环境下的调度和交替执行，
2. 也不需要进行额外的同步，
3. 或者在调用方进行任何其他的协调操作，

调用这个对象的行为都可以获得正确的结果，那这个对象是线程安全的”。

这个定义比较严谨，它要求线程安全的代码都必须具备一个特征：代码本身封装了所有必要的正确性保障手段（如互斥同步等），令调用者无须关心多线程的问题，更无须自己采取任何措施来保证多线程的正确调用。**这点听起来简单，但其实并不容易做到**，在大多数场景中，我们都会将这个定义弱化一些，如果把 “调用这个对象的行为” 限定为 “单次调用” 这个定义的其他描述也能够成立的话，我们就可以称它是线程安全了，为什么要弱化这个定义，现在暂且放下，稍后再详细探讨。

### Java语言中的线程安全 ###

线程安全具体是如何体现的？

有哪些操作是线程安全的？

这里讨论的线程安全，就限定于多个线程之间存在共享数据访问这个前提，因为如果一段代码根本不会与其他线程共享数据，那么从线程安全的角度来看，程序是串行执行还是多线程执行对它来说是完全没有区别的。

为了更加深入地理解线程安全，在这里我们可以不把线程安全当做一个非真即假的二元排他选项来看待，按照线程安全的 “安全程度” 由强至弱来排序，（注：这种划分方法也是 Brian Goetz 在 IBM developWorkers 上发表的一篇论文中提出的）可以将 Java 语言中各个操作共享的数据分为以下 5 类：

1. 不可变、
2. 绝对线程安全、
3. 相对线程安全、
4. 线程兼容、
5. 线程对立。

#### 不可变 ####

在 Java 语言中（特指 JDK 1.5 以后，即 Java 内存模型被修正之后的 Java 语言），**不可变（Immutable）的对象一定是线程安全的**，无论是对象的方法实现还是方法的调用者，都不需要采取任何的线程安全保障措施，只要一个不可变的对象被正确地构建出来（没有发生 this 引用逃逸的情况），那其外部的可见状态永远也不会改变，永远也不会看到它在多个线程之中处于不一致的状态。“不可变” 带来的安全性是最简单和最纯粹的。

Java 语言中，如果共享数据是一个基本数据类型，那么只要在定义时使用 final 关键字修饰它就可以保证它是不可变的。如果共享数据是一个对象，那就需要保证对象的行为不会对其状态产生任何影响才行，

不妨想一想 java.lang.String 类的对象，它是一个典型的不可变对象，我们调用它的 substring()、replace() 和 concat() 这些方法都不会影响它原来的值，只会返回一个新的构造的字符串对象。

保证对象行为不影响自己状态的途径有很多种，其中最简单的就是把对象中带有状态的变量都声明为 final，这样在构造函数结束之后，它就是不可变的，例如下面代码中 java.lang.Integer 构造函数所示的，它通过将内部状态变量 value 定义为 final 来保障状态不变。

    /**
     * The value of the {@code Integer}.
     *
     * @serial
     */
    private final int value;

    /**
     * Constructs a newly allocated {@code Integer} object that
     * represents the specified {@code int} value.
     *
     * @param   value   the value to be represented by the
     *                  {@code Integer} object.
     */
    public Integer(int value) {
        this.value = value;
    }

在 Java API 中符合不可变要求的类型，除了上面提到的 String 之外，常用的还有枚举类型，以及 java.lang.Number 的部分子类，如 Long 和 Double 等数值包装类型，BigInteger 和 BigDecimal 等大数据类型；

但同为 Number 的子类型的原子类 AtomicInteger 和 AtomicLong 则并非不可变的，不妨看看这两个原子类的源码，想一想为什么？

>PS.AtomicInteger 和 AtomicLong调用sun.misc.Unsafe进行操作

#### 绝对线程安全 ####

绝对的线程安全完全满足 Brian Goetz 给出的线程安全的定义，这个定义其实是很严格的，一个类要达到 “不管运行是环境如何，调用者都不需要任何额外的同步措施” 通常需要付出很大的，甚至有时候是不切实际的代价。**在 Java API 中标注自己是线程安全的类，大多数都不是绝对的线程安全**。可以通过 Java API 中一个不是 “绝对线程安全” 的线程安全类来看看这里的 “绝对” 是什么意思。

如果说 java.util.Vector 是一个线程安全的容器，相信所有的 Java 程序员对此都不会有异议，因为它的 add()、get() 和 size() 这类方法都是被 synchronized 修饰的，尽管这样效率很低，但确实是安全的。但是，即时它所有的方法都被修饰成同步，也不意味着调用它的时候永远都不需要同步手段了，请看一下面测试代码。

[VectorThreadSafe](VectorThreadSafe.java)


	java.lang.ArrayIndexOutOfBoundsException: Array index out of range: 49
		at java.util.Vector.get(Vector.java:748)
		at com.lun.other.jvm.c13.VectorThreadSafe$2.run(VectorThreadSafe.java:37)
		at java.lang.Thread.run(Thread.java:748)

>PS. 得到这异常结果需要等待程序运行较长一段时间。

很明显，尽管这里使用到的 Vector 的 get()、remove() 和 size() 方法都是同步的，但是在多线程的环境中，如果不在方法调用端做额外的同步措施的话，使用这段代码仍然是不安全的，因为如果另一个线程恰好在错误的时间里删除了一个元素，导致序号 i 已经不再可用的话，再用 i 访问数组就会抛出一个 ArrayIndexOutOfBoundsException。如果要保证这段代码能正确执行下去，不得不把 removeThread 和 printThread 的定义改成如下代码的样子。

	Thread removeThread = new Thread(new Runnable() {
		@Override
		public void run() {
			synchronized(vector) {
				for (int i = 0; i < vector.size(); i++) {
					vector.remove(i);
				}
			}
		}
	});
	
	Thread printThread = new Thread(new Runnable() {
		@Override
		public void run() {
			synchronized(vector) {
				for (int i = 0; i < vector.size(); i++) {
					System.out.println(vector.get(i));
				}
			}
		}
	});

#### 相对线程安全 ####

相对的线程安全就是我们通常意义上所讲的线程安全，它需要保证对这个对象单独的操作是线程安全，我们在调用的时候不需要做额外的保障措施，但是对于一些特定顺序的连续调用，就可能需要在调用端使用额外的同步手段来保证调用的正确性。上面两对代码就是相对线程安全的明显的案例。

在 Java 语言中，大部分的线程安全类都属于这种类型，例如 Vector、HashTable、Collections 的 synchronizedCollection() 方法包装的集合等。

#### 线程兼容 ####

线程兼容是指对象本身并不是线程安全的，但是可以通过在调用端正确地使用同步手段来保证对象在并发环境中可以安全地使用，我们平常说一个类不是线程安全的，绝大多数时候指的是这一种情况。Java API 中大部分的类都是属于线程兼容的，如与前面的 Vector 和 HashTable 相对应的集合类 ArrayList 和 HashMap 等。

#### 线程对立 ####

**线程对立是指无论调用端是否采取了同步措施，都无法在多线程环境中并发使用的代码**。由于 Java 语言天生就具备多线程特性，线程对立这种排斥多线程的代码是很少出现的，而且通常都是有害的，应当尽量避免。

一个线程对立的例子是 Thread 类的 suspend() 和 resume() 方法，如果有两个线程同时持有一个线程对象，一个尝试去中断线程，另一个尝试去恢复线程，如果并发进行的话，无论调用时是否进行了同步，目标线程都是存在死锁风险的，如果 suspend() 中断的线程就是即将要执行 resume() 的那个线程，那就肯定要产生死锁了。也正是由于这个原因，suspend() 和 resume() 方法已经被 JDK 声明废弃（@Deprecated）了。

常见的线程对立的操作还有 System.setIn()、System.setOut() 和 System.runFinalizerosOnExit() 等。

### 线程安全的实现方法 ###

如何实现线程安全与代码编写有很大的关系，但虚拟机提供的同步和锁机制也起到了非常重要的作用。代码编写如何实现线程安全和虚拟机如何实现同步与锁这两者都会有所涉及，相对而言更偏重后者一些，只要了解了虚拟机线程安全手段的运作过程，自己去思考代码如何编写并不是一件困难的事情。

#### 互斥同步 ####

**互斥同步**（Mutual Exclusion & Synchronization）是常见的一种并发正确性保障手段。同步是指在多个线程并发访问共享数据时，保证共享数据在同一个时刻只被一个（或者是一些，使用信号量的时候）线程使用。而互斥是实现同步的一种手段，临界区（Critical Section）、互斥量（Mutex）和信号量（Semaphore）都是主要的互斥实现方式。因此，在这 4 个字里面，互斥是因，同步是果；互斥是方法，同步是目的。

在 Java 中，**最基本的互斥同步手段就是 synchronized 关键字**，synchronized 关键字经过编译之后，会在同步块的前后分别形成 monitorenter 和 monitorexit 这两个字节码指令，这两个字节码都需要一个 reference 类型的参数来指明要锁定和解锁的对象。如果 Java 程序中的 synchronized 明确指定了对象参数，那就是这个对象的 reference；如果没有明确指定，那就根据 synchronized 修饰的是实例方法还是类方法，去取对应的对象实例或 Class 对象来作为锁对象。

根据虚拟机规范的要求，在执行 monitorenter 指令时，首先要尝试获取对象的锁。如果这个对象没被锁定，或者当前线程已经拥有了那个对象的锁，把锁的计数器加 1，相应的，在执行 monitorexit 指令时将锁计数器减 1，当计数器为 0 时，锁就被释放。如果获取对象锁失败，那当前线程就要阻塞等待，知道对象锁被另外一个线程释放为止。

在虚拟机规范对 monitorenter 和 monitorexit 的行为描述中，有两点是需要特别注意的。

1. synchronized 同步块对同一条线程来说是可重入的，不会出现自己把自己锁死的问题。

2. 同步块在已进入的线程执行完之前，会阻塞后面其他线程的进入。

在前面讲过，Java 的线程是映射到操作系统的原生线程之上的，如果要阻塞或唤醒一个线程，都需要操作系统来帮忙完成，这就需要从用户态转换到核心态中，因此状态转换需要耗费很多的处理器时间。对于代码简单的同步块（如被 synchronized 修饰的 getter() 或 setter() 方法），状态转换消耗的时间有可能比用户代码执行的时间还要长。所以 **synchronized 是 Java 语言中一个重要级（Heavyweight）的操作**，有经验的程序员都会在确实必要的情况下才使用这种操作。而虚拟机本身也会进行一些优化，譬如在通知操作系统阻塞线程之前加入一段自旋等待过程，避免频繁地切入到核心态之中。

---

除了 synchronized 之外，我们还可以使用 java.util.concurrent（下文成 J.U.C）包中的重入锁（**ReentrantLock**）来实现同步，在基本用法上，ReentrantLock 与 synchronized 很相似，他们都具备一样的线程重入特性，只是代码写法上有点区别，一个表现为 API 层面的互斥锁（lock() 和 unlock() 方法配合 try/finally 语句块来完成），另一个表现为原生语法层面的互斥锁。不过，相比 synchronized，ReentrantLock **增加了一些高级功能**，主要有以下 3 项：等待可中断、可实现公平锁，以及锁可以绑定多个条件。

- 等待可中断是指当持有锁的线程长期不释放锁的时候，正在等待的线程可以选择放弃等待，改为处理其他事情，可中断特性对处理执行时间非常长的同步块很有帮助。

- 公平锁是指多个线程在等待同一个锁时，必须按照申请锁的时间顺序来依次获得锁；而非公平锁则不保证这一点，在锁被释放时，任何一个等待锁的线程都有机会获得锁。synchronized 中的锁是非公平的，ReentrantLock 默认情况下也是非公平的，但可以通过带布尔值的构造函数要求使用公平锁。

- 锁绑定多个条件是指一个 ReentrantLock 对象可以同时绑定多个 Condition 对象，而在 synchronized 中，锁对象的 wait() 和 notify() 或 notifyAll() 方法可以实现一个隐含的条件，如果要和多于一个的条件关联的时候，就不得不额外添加一个锁，而 ReentrantLock 则无须这样做，只需要多次调用 newCondition() 方法即可。

---

如果需要使用上述功能，选用 ReentrantLock 是一个很好的选择，那**如果是基于性能考虑呢**？关于 synchronized 和 ReentrantLock 的性能问题，Brian Goetz 对这两种锁在 JDK 1.5 与单核处理器，以及 JDK 1.5 与双 Xeon 处理器环境下做了一组吞吐量对比的实现，实现结果如下图所示

![](image/two-lock.png)

![](image/two-lock2.png)

从上面两图可以看出，多线程环境下 synchronized 的吞吐量下降得非常严重，而 ReentrantLock 则能基本保持在同一个比较稳定的水平上。与其说 ReentrantLock 性能好，**还不如说 synchronized 还有非常大的优化余地**。后续的技术发展也证明了这一地单，JDK 1.6 中加入了很多针对锁的优化措施，JDK 1.6 发布之后，人们就发现 synchronized 与 ReentrantLock 的性能基本上是完全持平了。因此，如果读者的程序是使用 JDK 1.6 或以上部署的话，性能因素就不再是选择 ReentrantLock 的理由了，虚拟机在未来的性能改进中肯定也会更加偏向于原生的 synchronized，**所以还是提倡在 synchronized 能实现需求的情况下，优先考虑使用 synchronized 来进行同步。**

#### 非阻塞同步 ####

互斥同步**最主要的问题**就是进行线程阻塞和唤醒所带来的性能问题，因此这种同步也称为阻塞同步（Blocking Synchronization）。从处理问题的方式上来说，**互斥同步属于一种悲观的并发策略**，总是认为只要不去做正确的同步措施（例如加锁），那就肯定会出现问题，无论共享数据是否真的会出现竞争，它都要进行加锁（这里讨论的是概念模型，实际上虚拟机会优化掉很大一部分不必要的加锁）、用户态核心态转换、维护锁计数器和检查是否有被阻塞的线程需要唤醒等操作。

随着硬件指令集的发展，我们有了另外一个选择：**基于冲突检测的乐观并发策略**，通俗地说，就是先进行操作，如果没有其他线程争用共享数据，那操作就成功了；如果共享数据有争用，产生了冲突，那就再采取其他的补偿措施（最常见的补偿措施就是不断地重试，知道成功为止），这种乐观的并发策略的许多实现都不需要把线程挂起，因此这种同步操作称为**非阻塞同步**（Non-Blocking Synchronization）。

**为什么说使用乐观并发策略需要 “硬件指令集的发展” 才能进行呢**？因为我们需要操作和冲突检测这两个步骤具备原子性，靠什么来保证呢？如果这里再使用互斥同步来保证就失去意义了，所以我们只能靠硬件来完成这件事情，硬件保证一个从语法上看起来需要多次操作的行为只通过一条处理器指令就能完成，这类指令常用的有：

- 测试并设置（Test-and-Set）。
- 获取并增加（Fetch-and-Increment）。
- 交换（Swap）。
- 比较并交换（Compare-and Swap，下文成 CAS）。
- 加载连接 / 条件存储（Load-Linked / Store-Conditional，下文称 LL/SC）。

其中，**前面的 3 条**是 20 世纪就已经存在于大多数指令集之中的处理器指令，后面的两条是现代处理器新增的，而且这两条指令的目的和功能是类似的。

在 IA64、x86 指令集中有 cmpxchg 指令完成 **CAS** 功能，在 sparc-TSO 也有 casa 指令实现，而在 ARM 和 PowerPC 架构下，则需要使用一对 Idrex/strex 指令来完成 **LL/SC** 的功能。

---

**CAS 指令**需要有 3 个操作数，分别是内存位置（在 Java 中可以简单理解为变量的内存地址，用 V 表示）、旧的预期值（用 A 表示）和新值（用 B 表示）。CAS 指令执行时，当且仅当 V 符合旧值预期值 A 时，处理器用新值 B 更新 V 的值，否则它就不执行更新，但是无论是否更新了 V 的值，都会返回 V 的旧值，上述的处理过程是一个原子操作。

在 JDK 1.5 之后，**Java 程序中才可以使用 CAS 操作**，该操作由 sun.misc.Unsafe 类里面的 compareAndSwapInt() 和 compareAndSwapLong() 等几个方法包装提供，虚拟机在内部对这些方法做了特殊处理，即时编译出来的结果就是一条平台相关的处理器 CAS 指令，没有方法调用的过程，或者可以认为是无条件内联进去了。（注：这种被虚拟机特殊处理的方法称为固有函数（intrinsics），类似的固有函数还有 Math.sin() 等）

由于 Unsafe 类不是提供给用户程序调用的类（Unsafe.getUnsafe() 的代码中限制了只有启动类加载器（Bootstrap ClassLoader）加载的 Class 才能访问它），因此，如果不采用反射手段，我们只能通过其他的 Java API 来间接使用它，如 J.U.C 包里面的整数原子类，其中的 compareAndSet() 和 getAndIncrement() 等方法都使用了 Unsafe 类的 CAS 操作。

看看如何使用 CAS 操作来避免阻塞同步。我们曾经通过这段 20 个线程自增 10000 次的代码来证明 volatile 变量不具备原子性，那么如何才能让它具备原子性呢？把 “race++” 操作或 increase() 方法用同步块包裹起来当然是一个办法，但是如果改成下面所示的代码，那效率将会提高许多。

[AtomicTest](AtomicTest.java)

运行结果为

	200000

使用 AtomicInteger 代替 int 后，程序输出了正确的结果，一切都要归功于 incrementAndGet() 方法的原子性。

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    public final int incrementAndGet() {
        for (;;) {
            int current = get();
            int next = current + 1;
            if (compareAndSet(current, next))
                return next;
        }
    }

incrementAndGet() 方法在一个无限循环中，不断尝试将一个比当前值大 1 的新值赋给自己。如果失败了，那说明在执行 “获取-设置” 操作的时候值已经有了修改，于是再次循环进行下一次操作，直到设置成功为止。

---

在JDK1.8的源码

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }

	//下面是通过jd-gui反汇编Unsafe得到，Unsafe在Eclipse下看不到源码
	//-------------------------

	public final int getAndAddInt(Object paramObject, long paramLong, int paramInt)
	{
		int i;
		do
		{
		  i = getIntVolatile(paramObject, paramLong);
		} while (!compareAndSwapInt(paramObject, paramLong, i, i + paramInt));
		return i;
	}


	//-------------------------

	public native int getIntVolatile(Object paramObject, long paramLong);

	//-------------------------

	public final native boolean compareAndSwapInt(Object paramObject, long paramLong, int paramInt1, int paramInt2);

---


尽管 CAS 看起来很美，但显然这种操作无法涵盖互斥同步的所有使用场景，并且 CAS 从语义上来说并不是完美的，存在这样的一个逻辑漏洞：如果一个变量 V 初次读取的时候是 A 值，并且在准备赋值的时候检查到它仍然为 A 值，**那我们就能说它没有被其他线程改变过了吗**？如果在这段期间它的值曾经被改成了 B，后来又被改回 A，那 CAS 操作就会误认为它从来没有被改变过。这个漏洞称为 CAS 操作的 “ABA” 问题。J.U.C 包为了解决这个问题，提供了一个带有标记的原子引用类 “AtomicStampedReference”，它可以通过控制变量值的版本来保证 CAS 的正确性。不过目前来说这个类比较 “鸡肋”，大部分情况下 ABA 问题不会影响程序并发的正确性，如果需要解决 ABA 问题，改用传统的互斥同步可能会比原子类更高效。

>PS. 没有银弹

#### 无同步方案 ####

要保证线程安全，并不是一定就要进行同步，两者没有因果关系。同步只是保证共享数据争用时的正确性的手段，如果一个方法本来就不涉及共享数据，那它自然就无须任何同步措施去保证正确性，因此会有一些代码天生就是线程安全的，笔者简单地介绍其中的两类。

#### 可重入代码 ####

可重入代码（Reentrant Code）：这种代码也叫做纯代码（Pure Code），可以在代码执行的任何时刻中断它，转而去执行另外一段代码（包括递归调用它本身），而在控制权返回后，原来的程序不会出现任何错误。相对线程安全来说，可重入性是更基本的特性，它可以保证线程安全，即所有的可重入的代码都是线程安全的，但是并非所有的线程安全的代码都是可重入的。

可重入代码有一些共同的特征，例如不依赖存储在堆上的数据和公用的系统资源、用到的状态量都由参数中传入、不调用非可重入的方法等。我们可以通过一个简单的原则来判断代码是否具备可重入性：如果一个方法，它的返回结果是可以预测的，只要输入了相同的数据，就都能返回相同的结果，那它就满足可重入性的要求，当然也就是线程安全的。

>PS. Math.abs()等静态方法

##### 线程本地存储 #####

线程本地存储（Thread Local Storage）：如果一段代码中所需要的数据必须与其他代码共享，那就看看这些共享数据的代码是否能保证在同一个线程中执行？如果能保证，我们就可以把共享数据的可见范围限制在同一个线程之内，这样，无须同步也能保证线程之间不出现数据争用的问题。

符合这种特点的应用并不少见，大部分使用消费队列的架构模式（如 “生产者 - 消费者” 模式）都会将产品的消费过程尽量在一个线程中消费完，其中最重要的一个应用实例就是经典 Web 交互模型中的 “一个请求对应一个服务器线程”（Thread-per-Request）的处理方式，这种处理方式的广泛应用使得很多 Web 服务端应用都可以使用线程本地存储来解决线程安全问题。

Java 语言中，如果一个变量要被多线程访问，可以使用 volatile 关键字声明它为 “易变的”；如果一个变量要被某个线程独享，Java 中就没有类似 C++ 中的 __declspec(thread) （注：在 Visual C++ 是 “__declspec(thread)” 关键字，而在 GCC 中是 “__thread”）这样的关键字，不过还是可以通过 **java.lang.ThreadLocal** 类来实现线程本地存储的功能。每一个线程的 Thread 对象中都有一个 ThreadLocalMap 对象，这个对象存储了一组以 ThreadLocal.threadLocalHashCode 为键，以本地线程变量为值的 K-V 值对，ThreadLocal 对象就是当前线程的 ThreadLocalMap 的访问入口，每一个 ThreadLocal 对象都包含了一个独一无二的 threadLocalHashCode 值，使用这个值就可以在线程 K-V 值对中找回对应的本地线程变量。

## 锁优化 ##

高效并发是从 JDK 1.5 到 JDK 1.6 的一个重要改进，HotSpot 虚拟机开发团队在这个版本上花费了大量的精力去实现各种锁优化技术，如

1. 适应性自旋（Adaptive Spinning）、
2. 锁消除（Lock Elimination）、
3. 锁粗化（Lock Coarsening）、
4. 轻量级锁（Lightweight Locking）、
5. 偏向锁（Biased Locking）等。

**这些技术都是为了在线程之间更高效地共享数据，以及解决竞争问题，从而提高程序的执行效率**。

### 自旋锁与自适应自选 ###

前面我们讨论互斥同步的时候，提到了互斥同步对性能最大的影响阻塞的实现，挂起线程和恢复线程的操作都需要转入内核态完成，这些操作给系统的并发性能带来了很大的压力。同时，虚拟机的开发团队也注意到在许多应用上，**共享数据的锁定状态只会持续很短的一段时间，为了这段时间去挂起和恢复线程并不值得**。如果物理机器有一个以上的处理器，能让两个或以上的线程同时并行执行，我们就可以让后面请求锁的那个线程 “**稍等一下**”，但不放弃处理器的执行时间，看看持有锁的线程是否很快就会释放锁。为了让线程等待，我们只需让线程执行一个忙循环（自旋），这项技术就是所谓的自旋锁。

自旋锁在 JDK 1.4.2 中就已经引入，只不过默认是关闭的，可以使用 -XX:+UseSpinning 参数来开启，在 JDK 1.6 就已经改为默认开启了。**自旋等待不能代替阻塞**，且先不说对处理器数量的要求，自旋等待本身虽然避免了线程切换的开销，但它是要占用处理器时间的，

因此，如果锁被占用的时间很短，自旋等待的效果就会非常好，反之，如果锁被占用的时候很长，那么自旋的线程只会白白消耗处理器资源，而不会做任何有用的工作，反而会带来性能上的浪费。

因此，自旋等待的时间必须要有一定的限度，如果自旋超过了限定的次数仍然没有成功获得锁，就应当使用传统的方式去挂起线程了。自旋次数的默认值是 10 次，用户可以使用参数 -XX:PreBlockSpin 来更改。

---

在 JDK 1.6 中引入了**自适应的自旋锁**。自适应意味着自旋的时间不再固定了，而是由前一次在同一个锁上的自旋时间及锁的拥有者的状态来决定。如果在同一个锁对象上，自旋等待刚刚成功获得过锁，并且持有锁的线程正在运行中，那么虚拟机就会认为这次自旋也很有可能再次成功，进而它将允许自旋等待持续相对更长的时间，比如 100 个循环。另外，如果对于某个锁，自旋很少成功获得过，那在以后要获取这个锁时将可能省略掉自旋过程，以避免浪费处理器资源。有了自适应自旋，随着程序运行和性能监控信息的不断完善，虚拟机对程序锁的状况预测就会越来越准确，虚拟机就会变得越来越 “聪明” 了。

>PS. 醒目的 自适应的自旋锁

### 锁消除 ###

**锁消除是指虚拟机即时编译器在运行时，对一些代码上要求同步，但是被检测到不可能存在共享数据竞争的锁进行消除**。锁消除的主要判定依据来源于**逃逸分析**的数据支持，如果判定在一段代码中，堆上的所有数据都不会逃逸出去从而被其他线程访问到，那就可以把他们当做栈上数据对待，认为它们是线程私有的，同步加锁自然就无须进行。

也许会有疑问，变量是否逃逸，对于虚拟机来说需要使用数据流分析来确定，但是程序自己应该是很清楚的，怎么会在明知道不存在数据争用的情况下要求同步呢？

答案是有许多同步措施并不是程序员自己加入的。同步的代码在 Java 程序中的普遍程度也许超过了大部分读者的想象。请看代码的例子，这段非常简单的代码仅仅是输出 3 个字符串相加的结果，无论是源码字面上还是程序语义上都没有同步。

	public static String concatString(String s1, String s2, String s3) {
		return s1 + s2 + s3;
	}

由于 String 是一个不可变的类，对字符串的连接操作总是通过生成新的 String 对象来进行的，因此 Javac 编译器会对 String 连接做自动优化。

在 JDK 1.5 之前，会转化为 StringBuffer 对象的连续 append() 操作，在 JDK 1.5 及以后的版本中，会转化为 StringBuilder 对象的连续 append() 操作，即上面代码可能会变成代码的样子

>客观地说，既然谈到锁消除与逃逸分析，那虚拟机就不可能是 JDK 1.5 之前的版本，实际上会转化为非线程安全的 StringBuilder 来完成字符串拼接，并不会加锁，但这也不影响笔者用这个例子证明 Java 对象中同步的普遍性。

	public static String concatString(String s1, String s2, String s3) {
		StringBuffer sb = new StringBuffer();
		sb.append(s1);
		sb.append(s2);
		sb.append(s3);
		return sb.toString();
	}

每个 StringBuffer.append() 方法中都有一个同步块，锁就是 sb 对象。虚拟机观察变量 sb，很快就会发现它的动态作用域被限制在 concatString() 方法内部。也就是说，sb 的所有引用永远不会 “逃逸” 道 concatString() 方法之外，其他线程无法访问到它，因此，虽然这里有锁，但是可以被**安全地消除掉**，在即时编译之后，这段代码就会忽略掉所有的同步而直接执行了。

### 锁粗化 ###

原则上，我们在编写代码的时候，总是推荐将同步块的作用范围限制得尽量小——只在共享数据的实际作用域中才进行同步，这样是为了使得需要同步的操作数量尽可能变小，如果存在锁竞争，那等待锁的线程也能尽快拿到锁。

大部分情况下，上面的原则都是正确的，但是如果一系列的连续操作都对同一个对象反复加锁和解锁，甚至加锁操作是出现在循环体中，那即使没有线程竞争，频繁地进行互斥同步操作也会导致不必要的性能损耗。

上面代码中连续的 append() 方法就属于这类情况。如果虚拟机探测到由这样的一串零碎的操作都对同一个对象加锁，将会把加锁同步的范围扩展（粗化）到整个操作序列的外部，以上面代码中为例，就是扩展到第一个 append() 操作之前直至最后一个 append() 操作之后，这样只需要加锁一次就可以了。

### 轻量级锁 ###

轻量级锁是 JDK 1.6 之中加入的新型锁机制，它名字中的 “轻量级” 是相对于使用操作系统互斥量来实现的传统锁而言的，因此传统的锁机制就称为 “重量级” 锁。首先需要强调一点的是，轻量级锁并不是用来代替重要级锁的，**它的本意是在没有多线程竞争的前提下，减少传统的重量级锁使用操作系统互斥量产生的性能消耗**。

#### 对象头部分 ####

要理解轻量级锁，以及后面会讲到的偏向锁的原理和运作过程，必须从 HotSpot 虚拟机的对象（**对象头部分**）的内存布局开始介绍。HotSpot 虚拟机的对象头（Object Header）分为两部分信息，第一部分用于存储对象自身的运行时数据，如哈希码（HashCode）、GC 分代年龄（Generational GC Age）等，这部分数据是长度在 32 位和 64 位的虚拟机中分别为 32 bit 和 64 bit，官方称它为 “Mark Word”，它是实现轻量级锁和偏向锁的关键。另外一部分用于存储指向方法区对象类型数据的指针，如果是数组对象的话，还会有一个额外的部分用于存储数组长度。

对象头信息是与对象自身定义的数据无关的额外存储成本，考虑到虚拟机的空间效率，Mark Work 被设计成一个非固定的数据结构以便在极小的空间内存储尽量多的信息，它会根据对象的状态复用自己的存储空间。

例如，在 32 位的 HotSpot 虚拟机中对象未被锁定的状态下，Mark Word 的 32bit 空间中的 25bit 用于存储对象哈希码（HashCode），4bit 用于存储对象分代年龄，2bit 用于存储锁标志位，1bit 固定为 0，在其他状态（轻量级锁定、重量级锁定、GC 标记、可偏向）下对象的存储内容如下。

存储内容|标志位|状态
---|---|---
对象哈希码、对象分代年龄|01|未锁定
指向锁记录的指针|00|轻量级锁定
指向重量级锁的指针|10|膨胀（重量级锁定）
空，不需要记录信息|11|GC标记
偏向线程ID、偏向时间戳、对象分代年龄|01|可偏向

#### 轻量级锁的执行过程 ####

简单地介绍了对象的内存布局后，话题返回到轻量级锁的执行过程上。在代码进入同步块的时候，如果此同步对象没有被锁定（锁标志位为 “01” 状态）虚拟机首先将在当前线程的栈帧中建立一个名为锁记录（Lock Record）的空间，用于存储锁对象目前的 Mark Word 的拷贝（官方把这份拷贝加上了一个 Displaced 前缀，即 Displaced Mark Word），这时候线程堆栈与对象头的状态如下图所示。

![](image/lightweight-lock.png)

然后，虚拟机将使用 CAS 操作尝试将对象的 Mark Word 更新为指向 Lock Record 的指针。如果这个更新动作成功了，那么这个线程就拥有了该对象的锁，并且对象 Mark Word 的锁标志位 （Mark Word 的最后 2bit）将转变为 “00”，即表示此对象处于轻量级锁定状态，这时候线程堆栈与对象头的状态如下图所示。

![](image/lightweight-lock2.png)

如果这个更新操作失败了，虚拟机首先会检查对象的 Mark Word 是否指向当前线程的栈帧，如果只说明当前线程已经拥有了这个对象的锁，那就可以直接进入同步块继续执行，否则说明这个锁对象以及被其他线程线程抢占了。

如果有两条以上的线程争用同一个锁，那轻量级锁就不再有效，要膨胀为重量级锁，所标志的状态变为 “10”，Mark Word 中存储的就是指向重量级锁（互斥量）的指针，后面等待锁的线程也要进入阻塞状态。

上面描述的是轻量级锁的加锁过程，它的解锁过程也是通过 CAS 操作来进行的，如果对象的 Mark Word 仍然指向着线程的锁记录，那就用 CAS 操作把对象当前的 Mark Word 和线程中复制的 Displaced Mark Word 替换回来，如果替换成功，整个同步过程就完成了。如果替换失败，说明有其他线程尝试过获取该锁，那就要释放锁的同时，唤醒被挂起的线程。

轻量级锁能提升程序同步性能的依据是 “**对于绝大部分的锁，在整个同步周期内都是不存在竞争的”**，这是一个经验数据。如果没有竞争，轻量级锁使用 CAS 操作避免了使用互斥量的开销，但如果存在锁竞争，除了互斥量的开销外，还额外发生了 CAS 操作，因此在有竞争的情况下，轻量级锁会比传统的重量级锁更慢。

### 偏向锁 ###

偏向锁也是 JDK 1.6 中引入的一项锁优化，它的目的是消除数据在无竞争情况下的同步原语，进一步提高程序的运行性能。如果说轻量级锁是在无竞争的情况下使用 CAS 操作去消除同步使用的互斥量，那偏向锁就是在无竞争的情况下把整个同步都消除掉，连 CAS 操作都不做了。

**偏向锁的 “偏”，就是偏心的 “偏”、偏袒的 “偏”，它的意思是这个锁会偏向于第一个获得它的线程，如果在接下来的执行过程中，该锁没有被其他的线程获取，则持有偏向锁的线程将永远不需要再进行同步**。

如果读懂了前面轻量级锁中关于对象头 Mark Word 与线程之间的操作过程，那偏向锁的原理理解起来就会很简单。假设当前虚拟机启用了偏向锁（启用参数 -XX:+UseBiasedLocking，这是 JDK 1.6 的默认值），那么，当锁对象第一次被线程获取的时候，虚拟机将会把对象头中的标志位设为 “01”，即偏向模式。同时使用 CAS 操作把获取到这个锁的线程 ID 记录在对象的 Mark Word 之中，如果 CAS 操作成功，持有偏向锁的线程以后每次进入这个锁相关的同步块时，虚拟机都可以不再进行如何同步操作（例如 Locking、Unlocking 及对 Mark Word 的 Update 等）。

当有另外一个线程去尝试获取这个锁时，偏向模式就宣告结束。根据锁对象目前是否处于被锁定的状态，撤销偏向（Revoke Bias）后恢复到未锁定（标志位为 “01”）或轻量级锁定（标志位为 “00”）的状态，后续的同步操作就如上面介绍的轻量级锁那样执行。偏向锁、轻量级锁的状态转换及对象 Mark Word 的关系如下图所示。

![](image/biased-lock.png)

偏向锁可以提高带有同步但无竞争的程序性能。它同样是一个带有效益权衡（Trade Off）性质的优化，也就是说，它并不一定总是对程序运行有利，如果程序中大多数的锁总是被多个不同的线程访问，那偏向模式就是多余的。在具体问题具体分析的前提下，有时候使用参数 -XX:-UseBiasedLocking 来禁止偏向锁优化反而可以提升性能。
