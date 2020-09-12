* [redo日志刷盘的时机](#redo日志刷盘的时机)
* [redo日志刷到哪里](#redo日志刷到哪里)
* [每一个redo日志文件长的怎么样](#每一个redo日志文件长的怎么样)
* [关于redo日志文件的前4个block的一个吐槽：太浪费了](#关于redo日志文件的前4个block的一个吐槽太浪费了)
  * [先吐槽结果：](#先吐槽结果)
  * [再吐槽细节：](#再吐槽细节)
  * [个人给innodb的浪费洗个免费的地](#个人给innodb的浪费洗个免费的地)
* [lsn是什么](#lsn是什么)
* [checkpoint](#checkpoint)
  * [什么是checkpoint](#什么是checkpoint)
  * [checkpoint 过程](#checkpoint-过程)
* [怎么甩掉数据还是可能丢失的锅](#怎么甩掉数据还是可能丢失的锅)
* [崩溃恢复](#崩溃恢复)
* [查看系统中的各种LSN值](#查看系统中的各种LSN值)
* [最后再捋一捋修改数据的全流程](#最后再捋一捋修改数据的全流程)
   
# redo日志刷盘的时机
* log buffer 不够用了
* 事务提交了
* checkpoint
* 后台有个线程在刷刷刷
* 关闭Server的时候

# redo日志刷到哪里
* 在磁盘的ib_logfile0，满了就去ib_logfile1，以此类推
* 一共最多可以有100个磁盘日志文件
* `循环利用`

![redo日志文件组.png](../../imgs/mysql/redo日志文件组.png?raw=true)

# 每一个redo日志文件长的怎么样
* 和log buffer很相似，都是一个个block填满，每个block依旧是512B
* 比log buffer多4个block，放在文件头作为控制信息用的
    * block1: log file header（常见套路header打头）
        * LOG_HEADER_FORMAT - 4字节 - redo日志的版本，在MySQL 5.7.21中该值永远为1
        * LOG_HEADER_PAD1 - 4字节 - 无用
        * LOG_HEADER_START_LSN - 8字节 - 标记本redo日志文件开始的lsn
        * LOG_HEADER_CREATOR - 32字节 - 标记这个文件创建者是谁（正常："MySQL 5.7.21"，备份："ibbackup"）
        * LOG_BLOCK_CHECKSUM - 4字节 - 该block的checksum 
    * block2: checkpoint1: 刷盘点1（难道header还不够吗？非要额外来一个checkpoint？）
        * LOG_CHECKPOINT_NO - 8字节- 服务器做checkpoint的编号
        * LOG_CHECKPOINT_LSN - 8字节 - 做checkpoint结束时的lsn（系统崩溃就从这开始）
        * LOG_CHECKPOINT_OFFSET - 8字节 - LOG_CHECKPOINT_LSN的文件offset
        * LOG_CHECKPOINT_LOG_BUF_SIZE - 8字节 - 服务器做checkpoint时log buffer的大小(要这个size干嘛？)
        * LOG_BLOCK_CHECKSUM - 4字节 - 该block的checksum
    * block3: 无用的
    * block4: checkpoint2：刷盘点2（点2和点1，区别在哪？为啥要弄2个？）
        * 当checkpoint_no的值是偶数时，就写到checkpoint1中，是奇数时，就写到checkpoint2中
        * 设计这么多checkpoint也没用，只会写到第一个日志组的第一个文件的block2（checkpoint1）或者block4（checkpoint2）

![redo日志组示意图2.png](../../imgs/mysql/redo日志组示意图2.png?raw=true)

# 关于redo日志文件的前4个block的一个吐槽：太浪费了
## 先吐槽结果：
* block1只放了52个有用的字节，但是整个是 512 字节，浪费了90%
* 明明block2和block4都是表示checkpoint，为啥非要分奇偶，查了资料也暂时看不出分奇偶是为了解决啥问题。。设计的时候突然阔气了？
* 就算checkpoint要分奇偶，你可以可以放到block1中啊。。
* 假设100个logfile，就会有100个block1，100个block2:checkpoint1，100个无用block3，100个block4:checkpoint2，但实际上checkpoint信息只在第一个logfile存储

## 再吐槽细节：
向来非常节俭的innodb作者，在此时此刻，在logfile上，竟然如此大方。
block1一共有用的字节是：52字节，整个block字节是512字节，足足浪费了90%！！！
之前有多小气：
* 每条记录record的额外信息里面，有可变字段长度列表，有null值列表，就是为了能省几个字节，宁愿多多算算offset也不想多占用空间
* mtr的结束符号，是的，怎么看一个mtr是否结束，就看结尾有没有一个MLOG_MULTI_REC_END，本来这个设计就很完美了，结果为了小气，对于某些只有一行的mtr，
不带这个结束标识符了，而是在redo日志的header里面的某一个bit来表示，如果是1表示mtr只有1条redo日志，就默认带了 MLOG_MULTI_REC_END。
* 为redo日志（type+spaceId+pageNo+data）的type设置了50多个类型，想方设法节约空间，什么1字节2字节，紧凑不紧凑的。

## 个人给innodb的浪费洗个免费的地
* 不比行记录record、redo日志的mtr组那些数据量庞大的业务，这个logfile顶天了才100个，也就是顶多400个block1，节省不了多少。
* 毕竟不是内存是磁盘空间不要太在乎大小，而且固定的文件格式头，很方便编码offset定位


# lsn是什么
* 自系统开始运行，就不断的在修改页面，也就意味着会不断的生成redo日志。
* 为了记录已经写了多少个redo日志，设计了一个`全局变量`： Log Sequence Number。
* 不同于人类的年龄才1开始计算，LSN的年龄从`8704`开始计算
    * 这种奇怪的数字，估计又是作者的`恶趣味`，莫非是他的生日？
* 你以为一条redo日志对应一个lsn？
    * 并不是的，一组mtr才对应一个lsn，所以lsn不是+1,+1,+1这么跳，而是+999, +10086（mtr组大小的size）。

我们需要知道的 4 个lsn：
* lsn
    * 表示当前log buffer记录到哪里了，有新的redo日志进log buffer，那么这个lsn就会随之增加
* flushed_to_disk_lsn
    * 当前log buffer已经刷到磁盘的lsn
* write_lsn
    * 和 flushed_to_disk_lsn 差不多，当前log buffer已经刷到OS的缓冲区的lsn
* checkpoint_lsn
    * 脏页链表 flush 链表的最后一个节点的 oldest_modification 写入 checkpoint_lsn
    * 将 此次checkpoint的checkpoint_lsn和offset以及编码写入logfile文件组的第一个文件的第2或者4个block(checkpoint1或者checkpoint2)

混个脸熟就好了，只要知道有这么 4 个lsn需要重点关照，接下来一一介绍之。

# checkpoint

## 什么是checkpoint

满足某些判定条件的时候把脏页刷盘的过程叫做：checkpoint！
* 在某些情况下触发checkpoint，脏页刷盘
    * Buffer Pool 不够用时
    * log Buffer 不够用时
* checkpoint的方式
    * Sharp Checkpoint： 数据库关闭的时候，全部脏页刷盘
    * Fuzzy Checkpoint： mysql运行时innodb引擎每次只刷新一部分脏页
        * MasterThread Checkpoint： 每秒或者每10秒从Buffer Pool的flush list刷一定比例回盘
        * FLUSH_LRU_LIST Checkpoint： 为了保证Buffer Pool的lru list至少有100个页面（满的lru可以有8K个页 = 128MB/16KB）
        * Async/Sync Flush Checkpoint: log buffer中的redo日志不可用的情况，需要强制刷新页回磁盘，此时的页时脏页列表选取的
        * Dirty Page too much Checkpoint：为了保证Buffer Pool的flush list数量别太多，超过75%就来一波checkpoint
        
checkpoint_lsn：checkpoint触发过程需要依赖lsn的定位，这个lsn来自：Buffer Pool 的flush链表的最后一个节点的`oldest_modification`!

## checkpoint 过程
* 计算当前log buffer可被覆盖的lsn值：flush 链表的`最后一个节点`里面的 oldest_modification ，赋值给 checkpoint_lsn
![flush链表的控制块信息.png](../../imgs/mysql/flush链表的控制块信息.png?raw=true)
* 把checkpoint_lsn以及对应的checkpoint_offset和编号设置到日志文件的管理信息（就是checkpoint1或者checkpoint2）
![logfile的几个lsn.png](../../imgs/mysql/logfile的几个lsn.png?raw=true)


# 怎么甩掉数据还是可能丢失的锅

我们捋一捋修改记录后，innodb做了啥：
* 先把数据页load入内存，具体就是load如Buffer Pool中
* 对内存的缓存页进行数据修改，sql可以分为n个原子操作，会生成n个mtr组
* mtr组写入内存的log buffer中
* 脏页放到flush list中
* 【有一个后台线程】等脏页刷入磁盘后，flush list删掉那个相应页面的控制块
* 更新checkpoint_lsn并写入logfile

这期间有个问题，如果我数据刚写入到log buffer还没刷盘，脏页也还没刷盘，断电了。
数据是真的不见了。想通过redo日志重做都不行，因为log buffer也没刷盘。

这个问题确实在innodb存在且无解，所以innodb的作者把锅甩给我们，用一个参数：innodb_flush_log_at_trx_commit
* 0: 事务提交时不立即向磁盘中同步redo日志，这个任务是交给后台线程做的
* 1: 事务提交的时候，redo日志同步刷盘（性能最差，但最稳）
* 2: 事务提交的时候，至少要等待redo日志刷到OS的缓冲区才算完成（性价比最高，OS安全有保证，但断电GG）

好了，这个持久性和性能的平衡就让各位用户自己去权衡吧，这锅作者不接。


# 崩溃恢复

崩溃后只能根据logfile文件，来慢慢恢复了。
先确定恢复的起点和终点：
* 起点：logfile组的第一个文件的block2或者block4里面的checkpoint_lsn（注意要比较下checkpoint1和checkpoint2）
* 终点：遍历logfile，找到这么一个block，它的LOG_BLOCK_HDR_DATA_LEN != 512（因为等于512的说明block写满了，不等的说明写到那里就GG了）

恢复：
* redo日志是有spaceId和pageNo的，要把这些页给load到Buffer Pool里面，然后还是脏页，flush list、刷盘那一套

但是这么恢复有个问题，如果连续很多个redo日志，操作同一页，岂不是一直在做重复的事？
按照innodb的作者尿性，一般性能影响就2个法宝：批量 + 顺序IO。很可惜这里好像用不到顺序IO，只用到了批量。

* 批量： 先把要恢复的页记录到一个hashMap中，如果同一页的话，value就是list<Redo日志>
![崩溃恢复hashmap示意图.png](../../imgs/mysql/崩溃恢复hashmap示意图.png?raw=true)

* 跳过已经ok的页：
    * 因为checkpoint_lsn之后对应的数据页其实也可能被刷到磁盘了，也有可能没刷，断电前也不知道后台刷盘线程给不给力。
    * 所以我们可以用数据页的File Header里面的FIL_PAGE_LSN（最有一次更新的LSN）来判断，如果FIL_PAGE_LSN比checkpoint_lsn大，说明它刷过盘了！
    * 对于这些`侥幸刷盘`的数据页，我们也是无需恢复的，跳过之.
    
    
# 查看系统中的各种LSN值
```
mysql> SHOW ENGINE INNODB STATUS\G

(...省略前边的许多状态)
LOG
---
Log sequence number 124476971       # 当前log buffer的lsn量
Log flushed up to   124099769       # 已经刷盘的redo日志的lsn：flushed_to_disk_lsn
Pages flushed up to 124052503       # 已经刷盘的数据页的lsn：它就是下一个 checkpoint_lsn
Last checkpoint at  124052494       # 上一次的checkpoint_lsns
0 pending log flushes, 0 pending chkp writes
24 log i/o's done, 2.00 log i/o's/second
----------------------
(...省略后边的许多状态)
```

# 最后再捋一捋修改数据的全流程
* 一条语句可能会产生20-30条redo日志，把这波redo日志分成n组mtr
* 每一个mtr表示对底层页面的一次`原子`操作
* 在mtr结束时，会把这一组redo日志写入到log buffer中
* 在mtr结束时，会把能修改过的页面加入到Buffer Pool的flush链表
    * 具体是脏页对应的控制块加到flush链表的头
    * flush链表的头节点的设置 oldest_modification（修改该页面的mtr开始时对应的lsn值）
    * flush链表的头节点的设置 newest_modification（修改该页面的mtr结束时对应的lsn值）
* 后台线程-flush链表刷盘： flush 链表最后一个节点（控制块）对应的脏页刷盘，然后该节点删除
* 后台线程-redo日志刷盘：每秒都会刷新一次log buffer中的redo日志到磁盘
* 后台或用户同步线程-checkpoint：
    * 在某些情况下触发checkpoint，脏页刷盘
        * Buffer Pool 不够用时
        * log Buffer 不够用时
    * checkpoint的方式
        * Sharp Checkpoint： 数据库关闭的时候，全部脏页刷盘
        * Fuzzy Checkpoint： mysql运行时innodb引擎每次只刷新一部分脏页
            * MasterThread Checkpoint： 每秒或者每10秒从Buffer Pool的flush list刷一定比例回盘
            * FLUSH_LRU_LIST Checkpoint： 为了保证Buffer Pool的lru list至少有100个页面（满的lru可以有8K个页 = 128MB/16KB）
            * Async/Sync Flush Checkpoint: log buffer中的redo日志不可用的情况，需要强制刷新页回磁盘，此时的页时脏页列表选取的
            * Dirty Page too much Checkpoint：为了保证Buffer Pool的flush list数量别太多，超过75%就来一波checkpoint