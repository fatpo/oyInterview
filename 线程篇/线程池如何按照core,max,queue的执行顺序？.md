## 问：线程池如何按照core,max,queue的执行顺序？

## 知识储备
```
线程池执行顺序：corePool -> workQueue -> maxPool
也就是说，问题让我们改源码，使其顺序发生改变。
```

![线程池执行顺序](https://github.com/emaste-r/oyInterview/blob/master/imgs/线程池执行顺序.png?raw=true)