aqs(abstractQueuedSynchronized) 官网api:
```
https://docs.oracle.com/javase/8/docs/api/index.html
```

要自己实现以下方法：
* isHeldExclusively 是否持有排它锁
* tryAcquire 尝试加锁
* tryRelease 尝试释放锁

搞笑的事，java8和java15关于这个是否持有排它锁的demo竟然不一致！
肯定是后者更准确，因为如果我没有持有这个锁，我还是有可能判断它状态确实是1，虽然state是1，但它不是我的锁啊！难不成我还能释放别的线程的锁？
```
 // java8 api:  Reports whether in locked state
 protected boolean isHeldExclusively() {
   return getState() == 1;
 }

// java15 api
public boolean isHeldExclusively() {
// a data race, but safe due to out-of-thin-air guarantees
return getExclusiveOwnerThread() == Thread.currentThread();
}
```


实际上我们只需要自己实现 tryAcquire()和tryRelease()，因为真正复杂的核心逻辑在（人家框架写好了，不用我们操心）：
```
public final void acquire(long arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```
我觉得看AQS需要知道点java线程中断的知识：
```
https://www.cnblogs.com/carmanloneliness/p/3516405.html
```