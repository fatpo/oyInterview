## 问：线程池如何按照core,max,queue的执行顺序？

## 知识储备
```
线程池执行顺序：corePool -> workQueue -> maxPool
也就是说，问题让我们改源码，使其顺序发生改变。
```
线程池执行顺序：
![线程池执行顺序](https://github.com/emaste-r/oyInterview/blob/master/imgs/线程池执行顺序.png?raw=true)

参数：
```

public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), defaultHandler);
}
```
五个核心参数：
```
corePoolSize： 
    the number of threads to keep in the pool, even if they are idle, unless {@code allowCoreThreadTimeOut} is set
    核心线程池数量，无论是否空闲，都保持着。
    
maximumPoolSize: 
    the maximum number of threads to allow in the pool
    最大线程池数量。

keepAliveTime: 
    when the number of threads is greater than the core, 
    this is the maximum time that excess idle threads will wait for new tasks before terminating.
    如果核心线程数30，现在线程数开到了50个，如果这额外的20个线程在 keepAliveTime 时间内没收到新的任务，那么就会回收。

unit:
    the time unit for the {@code keepAliveTime} argument
    楼上的时间单位。


workQueue: 
    the queue to use for holding tasks before they are executed.  
    This queue will hold only the {@code Runnable} tasks submitted by the {@code execute} method.
    超过核心线程池的先放到队列里面存着。
    
```
额外两个核心参数：
```
threadFactory: 
    the factory to use when the executor creates a new thread
    创建线程的工厂函数，默认是：DefaultThreadFactory
    
handler: 
    the handler to use when execution is blocked because the thread bounds and queue capacities are reached
    连队列都满了的时候，采取的拒绝策略，默认是：AbortPolicy
```