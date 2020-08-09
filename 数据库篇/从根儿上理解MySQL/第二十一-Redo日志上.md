* [redo 日志是啥](#redo-日志是啥)
* [redo 日志解决什么问题](#redo-日志解决什么问题)
* [redo 日志长得如何？](#redo-日志长得如何)
* [插入一条记录 innodb 最坏情况到底做了什么操作](#插入一条记录-innodb-最坏情况到底做了什么操作)
* [mtr是一组 redo 日志](#mtr是一组-redo-日志)
* [redo log block](#redo-log-block)
* [Log Buffer](#log-buffer)
* [redo log block 怎么写入 log buffer](#redo-log-block-怎么写入-log-buffer)

# redo 日志是啥
这是还得从 Buffer Pool讲起，我们已经知道内存中的 flush 链表是需要刷入磁盘的，也确实有后台进程在默默干这件事。
但是这个线程很慢，优先级很 low，很 slow。我们很担心在内存中突然断电拉，那个后台线程远水解不了近渴啊！
解决方案：
* 每当出现脏页，我就刷入磁盘，优点是事少容易实现，缺点是一条记录的改动就需要磁盘大动干戈，磁盘IO又很慢，我要这个 Buffer Pool 有何用？
* 如果采用上述方案，一条 SQL 里面改了 100 个页，这些页可能是1,9,1000,83,38,2999,...888，那么磁盘岂不是随机 IO慢到飞起？

转变思路：
* 之前的思路：把 第0号表空间的100号页面 刷入磁盘
* 之后的思路：将 第0号表空间的100号页面的偏移量为1000处的值更新为2

它有 3 个优点：
* 它确实满足了刷到磁盘后不怕突然断电的需求拉，断电我就根据 redo 日志记录的地址和数据慢慢回滚即可
* 我每次写入的数据贼少，可能才 100 字节，比那个动不动刷 1 页 16KB 的强太多了！
* 我之前 100 个页面是随机分布的导致需要随机 IO，现在写入 redo 日志是顺序往下写，性能提高几个数量级！

上面的那个替代脏页刷入磁盘的日志就叫`redo 日志`。

# redo 日志解决什么问题
看完 redo 日志是什么后，这个解决什么问题也清楚了：
* 防数据丢失
* 快
    * 随机 IO 改顺序 IO
    * 刷入磁盘数据量大幅度缩小，16KB 变成几十字节
* 也不会让 Buffer Pool的设计变成白给的，辛辛苦苦设计这么多如 flush 链表，结果你还要每次改动就一页页刷磁盘？    


# redo 日志长得如何？
![redo日志通用结构.png](../../imgs/mysql/redo日志通用结构.png?raw=true)
* 有 50 多种 type
    * 1 字节 MLOG_1BYTE
    * 2 字节 MLOG_2BYTE
    * 4 字节 MLOG_4BYTE
    * 8 字节 MLOG_8BYTE
    * String MLOG_WRITE_STRING
    * ...
    

# 插入一条记录 innodb 最坏情况到底做了什么操作
* MAX_ROW_ID +1
    * 最差情况下 MAX_ROW_ID 加到 256 的倍数就要写入表空间的那个页（系统表空间页号为7的页面的相应偏移量处写入8个字节的值）
* 向聚簇索引对应B+树的页面中插入一条记录
    * 最坏情况下，数据页放不下了，要页分裂
        * 新申请数据页
            * 改动一些系统页面, 比方说要修改各种段、区的统计信息信息，各种链表的统计信息
            * 如果该层数据页也不足了，要先申请B+树上一层的目录项页
    * 修改数据页的槽信息，使其维持可 2 分的顺序    
    * 修改数据页的page header的槽数量（PAGE_N_DIR_SLOTS），可能+1     
    * 修改数据页的page header的记录数量（PAGE_N_HEAP），可能+1      
    * 修改数据页的上一条记录的 next_record 指向当前新增的记录
    * 修改数据页的代表的还未使用的空间最小地址
    
* 向二级索引对应的B+树的页面插入一条记录
    * 同上
    
# mtr是一组 redo 日志
知道了`插入一条记录 innodb 做了很多骚操作`后，我们发现一条普普通通的 insert sql 做太多事了。

**这么多事儿都要用 redo 日志记下来的！！**

innodb 把上面很多操作对应的 redo 日志分组，如：
* MAX_ROW_ID +1 是一组
* 向聚簇索引对应B+树的页面中插入一条记录 作为一组
* 向二级索引对应的B+树的页面插入一条记录 作为另外一组

这个redo 日志组就叫：mtr, Mini-Transaction！仿佛就是一个小小的事务！
用一个结束 flag 来表示这个 mtr 结束： MLOG_MULTI_REC_END
![一条完整的redo日志.png](../../imgs/mysql/一条完整的redo日志.png?raw=true)


# redo log block
为了更好地管理上面那些 redo 日志 mtr，innodb 将其写入一个 block 中，称之为 redo log block。
![redo log block示意图.png](../../imgs/mysql/redo log block示意图.png?raw=true)


# Log Buffer
* 设计InnoDB的大叔为了解决磁盘速度过慢的问题而引入了Buffer Pool。
* 同理，写入redo日志时也不能直接直接写到磁盘上。
* 实际上在服务器启动时就向操作系统申请了一大片称之为redo log buffer的连续内存空间。
* 翻译成中文就是redo日志缓冲区，我们也可以简称为log buffer。
* 默认`16MB`。
![logBuffer结构示意图.png](../../imgs/mysql/logBuffer结构示意图.png?raw=true)

# redo log block 怎么写入 log buffer
* mtr 写入 redo log block 是以整个 mtr 为一组的。
    * 而是每个mtr运行过程中产生的日志先暂时存到一个地方，当该mtr结束的时候，将过程中产生的一组redo日志再全部复制到log buffer中
* buf_free 这个全局变量来表示哪个 block 是可以写入的（innodb 惯用套路，拒绝遍历）

![log buffer 示意图2.png](../../imgs/mysql/logBuffer示意图2.png?raw=true)


