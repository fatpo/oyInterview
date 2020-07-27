## 准备mysql
```dtd
docker pull mysql5.6
```
```dtd
docker run -di -p 3306:3306 \
	-e MYSQL_USER=ouyang \
	-e MYSQL_PASSWORD=123qwe \
	-e MYSQL_ROOT_PASSWORD=123qwe \
	-v /Users/ouyang/mysql_home/lib:/var/lib/mysql \
	--name mysql mysql:5.6 
```
跑起来:
```dtd
docker exec -it 19e6a790faa4 /bin/bash
```

## 准备sysbench
```dtd
brew install sysbench
```


## 准备测试数据
```dtd
sysbench --test=/usr/local/Cellar/sysbench/1.0.20/share/sysbench/oltp_insert.lua \
--mysql-host=127.0.0.1 \
--mysql-port=3306 \
--mysql-user=root \
--mysql-password='123qwe' \
--mysql-db=test \
--db-driver=mysql \
--tables=10 \
--table-size=500000 \
--report-interval=10 \
--threads=128 \
prepare
```
执行结果:
```dtd
~/Desktop » sh 1.txt                                                                                                                                   ouyang@oymac
WARNING: the --test option is deprecated. You can pass a script name or path on the command line without any options.
sysbench 1.0.20 (using bundled LuaJIT 2.1.0-beta2)

Initializing worker threads...

Creating table 'sbtest3'...
Creating table 'sbtest5'...
Creating table 'sbtest1'...
Creating table 'sbtest9'...
Creating table 'sbtest8'...
Creating table 'sbtest7'...
Creating table 'sbtest10'...
Creating table 'sbtest2'...
Creating table 'sbtest6'...
sysbench --test=/usr/local/Cellar/sysbench/1.0.20/share/sysbench/oltp_point_select.lua \
Creating table 'sbtest4'...
Inserting 500000 records into 'sbtest7'
Inserting 500000 records into 'sbtest10'
Inserting 500000 records into 'sbtest1'
Inserting 500000 records into 'sbtest9'
Inserting 500000 records into 'sbtest2'
Inserting 500000 records into 'sbtest3'
Inserting 500000 records into 'sbtest6'
Inserting 500000 records into 'sbtest8'
Inserting 500000 records into 'sbtest5'
Inserting 500000 records into 'sbtest4'
```

## 测试主键查询
```dtd
sysbench --test=/usr/local/Cellar/sysbench/1.0.20/share/sysbench/oltp_point_select.lua \
--mysql-host=127.0.0.1 \
--mysql-port=3306 \
--mysql-user=root \
--mysql-password='123qwe' \
--mysql-db=test \
--db-driver=mysql \
--table-size=500000 \
--report-interval=10 \
--threads=5 \
--time=200 \
run
```
执行结果：
```dtd
~/Desktop » sh 3.txt                                                                                                                                   ouyang@oymac
WARNING: the --test option is deprecated. You can pass a script name or path on the command line without any options.
sysbench 1.0.20 (using bundled LuaJIT 2.1.0-beta2)

Running the test with following options:
Number of threads: 5
Report intermediate results every 10 second(s)
Initializing random number generator from current time


Initializing worker threads...

Threads started!

[ 10s ] thds: 5 tps: 1687.45 qps: 1687.45 (r/w/o: 1687.45/0.00/0.00) lat (ms,95%): 6.21 err/s: 0.00 reconn/s: 0.00
[ 20s ] thds: 5 tps: 1547.75 qps: 1547.75 (r/w/o: 1547.75/0.00/0.00) lat (ms,95%): 7.17 err/s: 0.00 reconn/s: 0.00
[ 30s ] thds: 5 tps: 1039.28 qps: 1039.28 (r/w/o: 1039.28/0.00/0.00) lat (ms,95%): 12.75 err/s: 0.00 reconn/s: 0.00
[ 40s ] thds: 5 tps: 1181.78 qps: 1181.78 (r/w/o: 1181.78/0.00/0.00) lat (ms,95%): 10.46 err/s: 0.00 reconn/s: 0.00
[ 50s ] thds: 5 tps: 1733.32 qps: 1733.32 (r/w/o: 1733.32/0.00/0.00) lat (ms,95%): 5.99 err/s: 0.00 reconn/s: 0.00
[ 60s ] thds: 5 tps: 1724.36 qps: 1724.36 (r/w/o: 1724.36/0.00/0.00) lat (ms,95%): 5.88 err/s: 0.00 reconn/s: 0.00
[ 70s ] thds: 5 tps: 1688.93 qps: 1688.93 (r/w/o: 1688.93/0.00/0.00) lat (ms,95%): 6.21 err/s: 0.00 reconn/s: 0.00
[ 80s ] thds: 5 tps: 1605.95 qps: 1605.95 (r/w/o: 1605.95/0.00/0.00) lat (ms,95%): 6.67 err/s: 0.00 reconn/s: 0.00
[ 90s ] thds: 5 tps: 1521.18 qps: 1521.18 (r/w/o: 1521.18/0.00/0.00) lat (ms,95%): 7.30 err/s: 0.00 reconn/s: 0.00
[ 100s ] thds: 5 tps: 1612.08 qps: 1612.08 (r/w/o: 1612.08/0.00/0.00) lat (ms,95%): 6.55 err/s: 0.00 reconn/s: 0.00
[ 110s ] thds: 5 tps: 1665.41 qps: 1665.41 (r/w/o: 1665.41/0.00/0.00) lat (ms,95%): 6.32 err/s: 0.00 reconn/s: 0.00
[ 120s ] thds: 5 tps: 1578.30 qps: 1578.30 (r/w/o: 1578.30/0.00/0.00) lat (ms,95%): 6.91 err/s: 0.00 reconn/s: 0.00
[ 130s ] thds: 5 tps: 1604.94 qps: 1604.94 (r/w/o: 1604.94/0.00/0.00) lat (ms,95%): 6.67 err/s: 0.00 reconn/s: 0.00
[ 140s ] thds: 5 tps: 1543.24 qps: 1543.24 (r/w/o: 1543.24/0.00/0.00) lat (ms,95%): 7.30 err/s: 0.00 reconn/s: 0.00
[ 150s ] thds: 5 tps: 997.82 qps: 997.82 (r/w/o: 997.82/0.00/0.00) lat (ms,95%): 13.22 err/s: 0.00 reconn/s: 0.00
[ 160s ] thds: 5 tps: 1172.00 qps: 1172.00 (r/w/o: 1172.00/0.00/0.00) lat (ms,95%): 10.65 err/s: 0.00 reconn/s: 0.00
[ 170s ] thds: 5 tps: 1209.07 qps: 1209.07 (r/w/o: 1209.07/0.00/0.00) lat (ms,95%): 10.27 err/s: 0.00 reconn/s: 0.00
[ 180s ] thds: 5 tps: 1172.30 qps: 1172.30 (r/w/o: 1172.30/0.00/0.00) lat (ms,95%): 10.27 err/s: 0.00 reconn/s: 0.00
[ 190s ] thds: 5 tps: 1249.51 qps: 1249.51 (r/w/o: 1249.51/0.00/0.00) lat (ms,95%): 9.06 err/s: 0.00 reconn/s: 0.00
[ 200s ] thds: 3 tps: 726.97 qps: 726.97 (r/w/o: 726.97/0.00/0.00) lat (ms,95%): 20.00 err/s: 0.00 reconn/s: 0.00
SQL statistics:
    queries performed:
        read:                            282625
        write:                           0
        other:                           0
        total:                           282625
    transactions:                        282625 (1413.07 per sec.)
    queries:                             282625 (1413.07 per sec.)
    ignored errors:                      0      (0.00 per sec.)
    reconnects:                          0      (0.00 per sec.)
General statistics:
    total time:                          200.0058s
    total number of events:              282625
Latency (ms):
         min:                                    0.79
         avg:                                    3.54
         max:                                  213.40
         95th percentile:                        7.84
         sum:                               999555.25
Threads fairness:
    events (avg/stddev):           56525.0000/274.40
    execution time (avg/stddev):   199.9110/0.00
```
这个TPS/QPS有点低：
```
transactions:                        282625 (1413.07 per sec.)
    queries:                         282625 (1413.07 per sec.)
```

## 清空测试数据
```dtd
sysbench --test=/usr/local/Cellar/sysbench/1.0.20/share/sysbench/oltp_insert.lua \
--mysql-host=127.0.0.1 \
--mysql-port=3306 \
--mysql-user=root \
--mysql-password='123qwe' \
--mysql-db=test \
--db-driver=mysql \
--tables=10 \
--table-size=500000 \
--report-interval=10 \
--threads=128 \
cleanup
```
执行结果：
```dtd
~/Desktop » sh 2.txt                                                                                                                                   ouyang@oymac
WARNING: the --test option is deprecated. You can pass a script name or path on the command line without any options.
sysbench 1.0.20 (using bundled LuaJIT 2.1.0-beta2)

Dropping table 'sbtest1'...
Dropping table 'sbtest2'...
Dropping table 'sbtest3'...
Dropping table 'sbtest4'...
Dropping table 'sbtest5'...
Dropping table 'sbtest6'...
Dropping table 'sbtest7'...
Dropping table 'sbtest8'...
Dropping table 'sbtest9'...
Dropping table 'sbtest10'...
```