* [背景](#背景)
* [痛点](#痛点)
* [解决方案](#解决方案)
* [改造效果](#改造效果)
  * [1、传统模式压测](#1传统模式压测)
  * [2、advisory lock模式压测](#2advisory-lock模式压测)
      
# 背景
任务调度系统，可能会利用数据库来做可靠的锁。
通常他们可能会使用数据库的一条记录来实现锁的SLOT和状态信息。
```SQL
create table lock_test (  
  tid int primary key,   -- 任务ID  
  state int default 1,   -- 任务状态，1表示初始状态，-1表示正在处理, 0表示处理结束  
  retry int default -1,   -- 重试次数  
  info text,   -- 其他信息  
  crt_time timestamp  -- 时间  
);  
```
任务处理系统到数据库获取任务，多个客户端worker抢占任务后处理再设置状态。state一方面表示任务状态，另一方面有乐观锁的感觉。
毕竟一个worker设置成功，其他的workers都没戏了。
例如
```SQL
 update lock_test set state=-1 , retry=retry+1 where tid=? and state=1;  
```
  
处理失败
```SQL
   update lock_test set state=1 where tid=? and state=-1; 
```

处理成功
```SQL
   update lock_test set state=0 where tid=? and state=-1;  

```
  
# 痛点
当多个客户端并行获得同一个任务时，就会引发冲突，导致等待（虽然等待时间可能不长，但是在大型任务调度系统中，一点点的等待都无法忍受）。


# 解决方案
事务级或会话级，根据业务形态选择。
```SQL
                                        List of functions  
   Schema   |               Name               | Result data type | Argument data types |  Type    
------------+----------------------------------+------------------+---------------------+--------  
 pg_catalog | pg_try_advisory_lock             | boolean          | bigint              | normal  
 pg_catalog | pg_try_advisory_xact_lock        | boolean          | bigint              | normal  
```
SQL改造如下

开始处理任务
```SQL
update lock_test set state=-1 , retry=retry+1 where tid=? and state=1 and pg_try_advisory_xact_lock(?) returning *;  
```
处理失败
```SQL
update lock_test set state=1 where tid=? and state=-1 and pg_try_advisory_xact_lock(?);  
```
处理成功
```SQL
update lock_test set state=0 where tid=? and state=-1 and pg_try_advisory_xact_lock(?);  
```

# 改造效果
为了体现冲突的问题，我们使用一条记录来表示一个任务，大家都来抢一个任务的极端场景。
```sql

create table lock_test (  
  tid int primary key,   -- 任务ID  
  state int default 1,   -- 任务状态，1表示初始状态，-1表示正在处理, 0表示处理结束  
  retry int default -1,   -- 重试次数  
  info text,   -- 其他信息  
  crt_time timestamp  -- 时间  
);  
  
insert into lock_test values (1, 1, -1, 'test', now());  
```
## 1、传统模式压测
vi test1.sql  
```SQL  
update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1;  
update lock_test set state=1 where tid=1 and state=-1;  
```
```shell
pgbench -M prepared -n -r -P 1 -f ./test1.sql -c 64 -j 64 -T 120  
  
query mode: prepared  
number of clients: 64  
number of threads: 64  
duration: 120 s  
number of transactions actually processed: 966106  
latency average = 7.940 ms  
latency stddev = 6.840 ms  
tps = 8050.081170 (including connections establishing)  
tps = 8054.812052 (excluding connections establishing)  
script statistics:  
 - statement latencies in milliseconds:  
         3.978  update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1;  
         3.962  update lock_test set state=1 where tid=1 and state=-1;  
```  
## 2、advisory lock模式压测

vi test2.sql  
```sql
update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1 and pg_try_advisory_xact_lock(1) returning *;  
update lock_test set state=1 where tid=1 and state=-1 and pg_try_advisory_xact_lock(1);  
```

```shell

pgbench -M prepared -n -r -P 1 -f ./test2.sql -c 64 -j 64 -T 120  
  
query mode: prepared  
number of clients: 64  
number of threads: 64  
duration: 120 s  
number of transactions actually processed: 23984594  
latency average = 0.320 ms  
latency stddev = 0.274 ms  
tps = 199855.983575 (including connections establishing)  
tps = 199962.502494 (excluding connections establishing)  
script statistics:  
 - statement latencies in milliseconds:  
         0.163  update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1 and pg_try_advisory_xact_lock(1) returning *;  
         0.156  update lock_test set state=1 where tid=1 and state=-1 and pg_try_advisory_xact_lock(1);  
```
8000 TPS提升到20万 TPS。开不开心、意不意外。
