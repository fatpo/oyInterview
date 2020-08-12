* [事务的隔离级别](#事务的隔离级别)
  * [说隔离级别之前先说遇到的问题（因为级别是为了解决这些问题而划分的）：](#说隔离级别之前先说遇到的问题因为级别是为了解决这些问题而划分的)
  * [好了，为了解决上述的问题，SQL定义了4个隔离级别:](#好了为了解决上述的问题sql定义了4个隔离级别)
  * [MYSQL擅自修改了4个隔离级别](#mysql擅自修改了4个隔离级别)
* [MVCC原理](#mvcc原理)
  * [版本链](#版本链)
  * [ReadView](#readview)
  * [ReadView 解释Read Committed 隔离级别](#readview-解释read-committed-隔离级别)
  * [ReadView 解释Read Repeatable 隔离级别](#readview-解释read-repeatable-隔离级别)
  * [innodb 在 RR 级别下 MVCC 能否解决幻读](#innodb-在-rr-级别下-mvcc-能否解决幻读)
* [关于purge](#关于purge)



# 事务的隔离级别
写在前面的感悟：
* 理论上来说事务的ACID特性中，我们都是渴望绝对隔离的
* 但实际上来说，性能不允许，只能设置不同的隔离级别，去适配不同的性能要求

## 说隔离级别之前先说遇到的问题（因为级别是为了解决这些问题而划分的）：
* 脏写
    * 假设hero的名字为孙权
    * session1开启事务1，该事务更新hero的名字为：刘备
    * session2开启事务2，该事务更新hero的名字为：曹操
    * session1提交事务1
    * session2**回滚事务2**
    * 结果最终的hero名字还是孙权，session1就很懵逼，我更新了数据，也提交事务，最终啥都没做？
* 脏读
    * 假设hero的名字为孙权
    * session1开启事务1
    * session2开启事务2，该事务2更新hero的名字为：曹操(**记住，事务 2 还没提交！**)
    * session1的事务1查询hero，发现竟然是曹操
    * session2 **回滚** 事务2，数据应该回滚为：孙权
    * 但可惜session1的事务拿到的数据一直是曹操(它读到了一个不存在的数据)，这就难搞了
* 不可重复读
    * 假设hero的名字为孙权
    * session1开启事务1，读到了孙权
    * session2开启事务2，该事务2更新hero名字为：曹操，并提交事务2！
    * session1的事务1再读一次hero，发现读到了曹操
    * 这session1在同一个事务1中前后两次读到2个英雄，懵逼了。
* 幻读
    * 假设hero表为空，没有一条数据
    * session1开启事务1，select * from hero where id>0，读不到任何记录（这很好）
    * session2开启事务2，insert一条hero记录（曹操）并提交
    * session1的事务1继续之前的 select * from hero where id>0，竟然读到了曹操
    * session1仿佛产生幻觉，所以叫幻读...
        * 幻读强调的是一个事务按照某个相同条件多次读取记录时，后读取时读到了之前没有读到的记录
        * 那对于先前已经读到的记录，之后又读取不到这种情况，算啥呢？(算不可重复读)
        
## 好了，为了解决上述的问题，SQL定义了4个隔离级别: 
首先，隔离级别是牺牲一点隔离性来成全性能，但是至少要保证基本的隔离性、正确性，所以脏写是不允许存在的。
* 读未提交
    * 顾名思义，可以读到别的事务未提交的数据
    * 会出现：脏读、不可重复读、幻读
* 读已提交
    * 可以读到别的事务已经提交的数据
    * 会出现：不可重复读、幻读
* 可重复读
    * 保证一个事务内，相同条件读到的数据一致
    * 会出现：幻读
* 可串行化
    * 绝对隔离
    * 啥都不会出现


## MYSQL擅自修改了4个隔离级别

** 有一点点不同：mysql的可重复读，能保证无法出现幻读！！！**

* 读未提交
    * 顾名思义，可以读到别的事务未提交的数据
    * 会出现：脏读、不可重复读、幻读
* 读已提交
    * 可以读到别的事务已经提交的数据
    * 会出现：不可重复读、幻读
* 可重复读
    * 保证一个事务内，相同条件读到的数据一致
    * **啥都不会出现**
* 可串行化
    * 绝对隔离
    * 啥都不会出现

# MVCC原理

MVCC（Multi-Version Concurrency Control ，多版本并发控制）
指的就是在使用READ COMMITTD、REPEATABLE READ这两种隔离级别的事务在执行普通的SELECT操作时访问记录的版本链的过程，这样子可以使不同事务的读-写、写-读操作并发执行，从而提升系统性能。
* 关键词1：只在READ COMMITTD、REPEATABLE READ两种隔离级别
* 关键词2：执行普通的SELECT操作
* 关键词3：允许读写-写读的并发执行，提高系统性能

## 版本链
* 每一个记录的3个隐藏字段： row_id（非必要） + trx_id + roll_pointer
* 其中的roll_pointer能把这条记录的所有undo日志串联起来
* MVCC的底层原理就是版本链 + ReadView

我们先看版本链：
![mvcc版本链.png](../../imgs/mysql/mvcc版本链.png)

这里友情提示下：版本链的意思是，roll_pointer把这条纪录的所有undo日志给串起来。

想到这里，发现insert类型的undo日志还真的应该和update类型的区别对待，理由：
* insert 的 undo 日志，在事务提交后就没用了，因为它在版本链的最后一个，删了也不影响版本链的遍历
* update 的 undo 日志，在事务提交后虽然也没用，但是不能删，因为它可能在版本链的中间节点，删了版本链就无法遍历。

## ReadView
先看两个和 MVCC 无关的隔离级别：
* READ UNCOMMITTED隔离级别： 直接读版本链的最新记录就好了
* SERIALIZABLE隔离级别： 这些事务和MVCC无关，全靠锁解决

再看两个和 MVCC 有关的隔离级别：
* READ COMMITTED: 都必须保证读到已经提交了的事务修改过的记录 + 我选择读别的事务提交的！
* REPEATABLE READ:  都必须保证读到已经提交了的事务修改过的记录 + 但我选择不读别的事务提交的！

看看 ReadView 的结构：
* m_ids：生成该 ReadView 时系统活动的事务 ID 列表
* creator_trx_id: 生成该 ReadView 时的事务 ID
* min_trx_id: 生成该 ReadView 时的最小活动事务 ID
* max_trx_id: 生成该 ReadView 时，系统应该分配给下一个事务的 ID

## ReadView 解释Read Committed 隔离级别

先把级别调整为 Read Committed ：
```dtd
mysql> SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;
Query OK, 0 rows affected (0.01 sec)

mysql> SHOW VARIABLES LIKE 'transaction_isolation';
+-----------------------+----------------+
| Variable_name         | Value          |
+-----------------------+----------------+
| transaction_isolation | READ-COMMITTED |
+-----------------------+----------------+
1 row in set (0.01 sec)
```
准备两张表：
```dtd
mysql> show create table hero;
+-------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Table | Create Table                                                                                                                                                                                                           |
+-------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| hero  | CREATE TABLE `hero` (
  `number` int(11) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`number`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 |
+-------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
1 row in set (0.00 sec)

mysql> show create table hero1;
+-------+----------------------------------------------------------------------------------------+
| Table | Create Table                                                                           |
+-------+----------------------------------------------------------------------------------------+
| hero1 | CREATE TABLE `hero1` (
  `id` int(4) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 |
+-------+----------------------------------------------------------------------------------------+
1 row in set (0.00 sec)
```
其中 hero 是主角，hero1 是工具人。给 hero 插入一条基础数据：
```dtd
insert into hero(1, "0", "haha"); # 假设 insert undo log 的 trx_id = 80
```
现在开启 3 个事务来解释 read committed 为啥能每次都 select 到被人提交后的数据：

|事务 1(id=100)|事务 2(id=200)|事务 3(id=300)|
|:---|---|---|
|begin;|begin;|begin;|
|set hero=1|  set hero1=1 (改hero1工具人表以便生成 trx_id) |select hero=0 (生成 ReadView(m_ids=[100,200], creator=0, min=100,max=301)) 遍历版本链时它只能看到不在 m_ids 里面且 trx_id < min(100) 或者 =creator(0) 的版本，也就是 hero=0 了|
|set hero=2| | |
|commit| | |
| | |select hero=2 (生成 ReadView(m_ids=[200], creator=0, min=200,max=301)) 遍历版本链时它只能看到不在 m_ids 里面且 trx_id < min(200) 或者 =creator(0)  的版本，也就是 hero=2 了|
| |set hero=3| |
| |set hero=4| |
| |commit| |
| | |select hero=4 (生成 ReadView(m_ids=[], creator=0, min=?, max=301)) 遍历版本链时它只能看到不在 m_ids 里面且 trx_id < max(301) （因为没有 min 可比较） 或者 =creator(0)  的版本，也就是 hero=4 了|

版本链：

|记录| undo 日志版本链|
|:---|---|
|hero=4| update undo log(trx_id=200, hero=4)|
| | update undo log(trx_id=200, hero=3)|
| | update undo log(trx_id=100, hero=2)|
| | update undo log(trx_id=100, hero=1)|
| | insert undo log(trx_id=80, hero=0)|

关键点：
    * 事务中每次 select 语句前先生成一个 ReadView
    * 比较 m_ids，max_trx_id, mix_trx_id, creator_trx_id
    
    
## ReadView 解释Read Repeatable 隔离级别

设置下级别 + 回滚数据 hero = 0：
```dtd
mysql> SET GLOBAL TRANSACTION ISOLATION LEVEL REPEATABLE READ;
Query OK, 0 rows affected (0.00 sec)

mysql> show variables like 'transaction_isolation';
+-----------------------+-----------------+
| Variable_name         | Value           |
+-----------------------+-----------------+
| transaction_isolation | REPEATABLE-READ |
+-----------------------+-----------------+
1 row in set (0.01 sec)

mysql> update hero set name='0' where number=1;
Query OK, 1 row affected (0.00 sec)
Rows matched: 1  Changed: 1  Warnings: 0
```

还是那个版本链：
  
  |记录| undo 日志版本链|
  |:---|---|
  |hero=4| update undo log(trx_id=200, hero=4)|
  | | update undo log(trx_id=200, hero=3)|
  | | update undo log(trx_id=100, hero=2)|
  | | update undo log(trx_id=100, hero=1)|
  | | insert undo log(trx_id=80, hero=0)|
  
还是用 3 个事务来解释 RR 隔离级别：  
  
|事务 1(id=100)|事务 2(id=200)|事务 3(id=300)|
|:---|---|---|
|begin;|begin;|begin;|
| set hero=1 | set hero1=1 (改hero1工具人表以便生成 trx_id)  |select hero=0 (生成 ReadView(m_ids=[100,200], creator=0, min=100,max=301)) 遍历版本链时它只能看到不在 m_ids 里面且 trx_id < min(100) 或者 =creator(0) 的版本，也就是 hero=0 了|
|set hero=2| | |
|commit| | |
| | |select hero=2 沿用第一次生成的 ReadView，只有 trx=80 的 hero=0 才可见|
| |set hero=3| |
| |set hero=4| |
| |commit| |
| | |select hero=4  沿用第一次生成的 ReadView，只有 trx=80 的 hero=0 才可见 |


## innodb 在 RR 级别下 MVCC 能否解决幻读
其实答案可是能，也可以是不能，看你从哪个角度来看待了。
* 一般情况下，能，解决幻读的 2 个利器：
    * MVCC
    * 加锁
* 很特殊的情况下，不能，我能给你举个 bad case

所以网上的人们别吵了，MVCC 基本上能解决幻读，除非是下面这种情况：

|事务 1(id=100)|事务 2(id=200)
|:---|---|
|begin;|begin;|
|update hero set name=1 where number=1| select name where number>0 生成的 ReadView(m_ids[100],min=100,max=201,creator=0)，我只能看到 trx<100的 hero=0|
|insert hero values(2, "100", "haha")| |
|commit | |
| | update hero set name=2 where number=1 (此时会被 trx_id=200设置到那条记录上面，版本链多了这个 200！！) |
| | select name where number>0 虽然沿用之前的 ReadView(m_ids[100],min=100,max=201,creator=0)，我只能看到 trx=200 > min 那条版本记录 的 number=2 的hero=100，幻读出现！|

这种情况太极端了，谁没事会去 update 一个不存在的数据，出现幻读也不是很致命。所以我们大体认为 MVCC 是可以解决幻读的。


# 关于purge
大家有没有发现两件事儿：

* 我们说insert undo在事务提交之后就可以被释放掉了，而update undo由于还需要支持MVCC，不能立即删除掉。

* 为了支持MVCC，对于delete mark操作来说，仅仅是在记录上打一个删除标记，并没有真正将它删除掉。

* 随着系统的运行，在确定系统中包含最早产生的那个ReadView的事务不会再访问某些update undo日志以及被打了删除标记的记录后，有一个后台运行的purge线程会把它们真正的删除掉。关于更多的purge细节，我们将放到纸质书中进行详细唠叨，不见不散哈～
