* [undo日志是什么](#undo日志是什么)
* [undo日志放在哪里](#undo日志放在哪里)
* [undo日志的格式](#undo日志的格式)
  * [insert 类型的undo日志](#insert-类型的undo日志)
  * [delete 类型的undo日志](#delete-类型的undo日志)
     * [delete的流程](#delete的流程)
     * [delete undo日志串联版本链](#delete-undo日志串联版本链)
  * [update 类型的undo日志](#update-类型的undo日志)
     * [不更新主键的情况](#不更新主键的情况)
        * [就地更新（in-place update）](#就地更新in-place-update)
     * [先删除掉旧记录，再插入新记录](#先删除掉旧记录再插入新记录)
     * [更新主键的情况](#更新主键的情况)
* [为啥undo日志不需要一个类似脏页的池子buffer pool，redo日志的池子log buffer](#为啥undo日志不需要一个类似脏页的池子buffer-poolredo日志的池子log-buffer)
* [之前一直好奇redo和undo2个日志哪个早哪个后](#之前一直好奇redo和undo2个日志哪个早哪个后)


# undo日志是什么
悔棋，回滚的依据。

# undo日志放在哪里
因为undo日志根据增删改有3种类型，讲起来比较长，我们先看看这些undo日志们存放在哪里。
之前一直在说索引页（也就是喜闻乐见的数据页），页类型是：FILE_PAGE_INDEX。

今天继续来看比较常见的页类型：**FIL_PAGE_UNDO_LOG** 。
![undo日志存放的页.png](../../imgs/mysql/undo日志存放的页.png?raw=true)



# undo日志的格式
## insert 类型的undo日志
![insert类型的undo日志.png](../../imgs/mysql/insert类型的undo日志.png?raw=true)
insert 语句的undo最简单，创建一个undo日志，然后有改新增的记录的roll_pointer(就是那个记录的第3个隐藏列，7个字节)指向这个undo日志。

复习下**记录的格式**：
* 隐藏列1：row_id
* 隐藏列2：trx_id
* 隐藏列3：roll_pointer(`主角`)
![记录的真实数据.png](../../imgs/mysql/记录的真实数据.png?raw=true)

insert之后的内存页面如下：
![undo日志存放的页.png](../../imgs/mysql/undo日志存放的页.png?raw=true)



## delete 类型的undo日志

### delete的流程
* 已知数据页的记录都用next_record串联成一个链表
* 已知数据页的记录删除后记录头有个delete_mask=1，并不是真正删除
* 已知数据页的page header有个 PAGE_FREE 把这些删除的record串联起来，叫**垃圾链表**

好了，我们来看看delete的流程
* 阶段一：把记录的delete_mask=1，然后啥事都不做，不会加入到垃圾链表（事务提交之前都是这个中间状态）
![记录删除的第一阶段.png](../../imgs/mysql/记录删除的第一阶段.png?raw=true)

* 阶段二：当事务提交后，有个purge后台线程过来善后：
    * 从页面内正常的链表中删除
    * 加入到垃圾链表
    * 修改下page header的槽位信息，记录的数量，已删除字节信息等
![记录删除的第二阶段.png](../../imgs/mysql/记录删除的第二阶段.png?raw=true)

### delete undo日志串联版本链
在对一条记录进行delete mark操作前，需要把该记录的旧的trx_id和roll_pointer隐藏列的值都给记到对应的undo日志中来。
那么前后改动的数据就能串起来了：
![delete过程undo日志的指针变动示意图.png](../../imgs/mysql/delete过程undo日志的指针变动示意图.png?raw=true)


## update 类型的undo日志

### 不更新主键的情况
#### 就地更新（in-place update）
不更新主键的情况下，如果更新后的数据和之前的行记录，存储空间一致！

### 先删除掉旧记录，再插入新记录
在不更新主键的情况下，任何一个被更新的列更新前和更新后占用的存储空间大小不一致，那么就先删后insert
* 这里的删除是直接删除，不是delete_mask=1后等待purge后台线程来善后，而是用户线程同步给它干掉，正常记录链表中移除并加入到垃圾链表中.
* 同步删除后还要维持数据页的page header的统计数据：比如PAGE_FREE、PAGE_GARBAGE等这些信息


### 更新主键的情况
如果碰到主键就只有一条路，先删后插，因为改前改后的主键值可能相差甚远，甚至隔了很多数据页。
分了2个阶段:
* 将旧记录进行delete mark操作
    * 这里是中间状态，**并不是真正删除！！！**
        * 原因是: 是因为别的事务同时也可能访问这条记录，如果把它真正的删除加入到垃圾链表后，别的事务就访问不到了(MVCC啊)!
    * 在事务提交后才由专门的线程做purge操作，把它加入到垃圾链表中
* 根据更新后各列的值创建一条新记录，并将其插入到聚簇索引中（需重新定位插入的位置）

因此一个update主键的sql会有2条undo日志：
* 类型为删除的undo日志：TRX_UNDO_DEL_MARK_REC
* 类型为插入的undo日志：TRX_UNDO_INSERT_REC


# 为啥undo日志不需要一个类似脏页的池子buffer pool，redo日志的池子log buffer
* 第一：人家那2个池子是为了缓解CPU和磁盘的矛盾的，你本身就在内存中（页类型：FILE_PAGE_UNDO_FILE），凑什么热闹
* 第二：你有必要缓存吗？无非是两种情况：
    * 事务提交前，断电了，你要undo日志何用，反正磁盘数据又没改动
    * 事务提交后，断电了，你要undo日志何用，提交了自然有redo日志来处理恢复
    
    
# 之前一直好奇redo和undo2个日志哪个早哪个后
* 第一：这两个理论上来说各司其职，非要说先后，先undo日志，后redo日志
* 第二：你对页的任何更改，都会落到redo日志的log buffer区域最后刷盘
* 第三：对页的回滚操作也是要依靠FILE_PAGE_UNDO_FILE的存储，改动了，那就要写redo日志

这么说来吧，undo日志就是语句执行过程中的一个必要程序，redo日志就是把语句执行过程的所有mtr都给存起来的刷盘的日志。