# 目标
当达到最大内存时，要释放没用的key。

怎么挑选没用的key，这是一个策略。

# maxmemory配置
```
➜  conf cat redis.conf | grep --color "maxmemory"
# according to the eviction policy selected (see maxmemory-policy).
# WARNING: If you have slaves attached to an instance with maxmemory on,
# limit for maxmemory so that there is some free RAM on the system for slave
# maxmemory <bytes>
# MAXMEMORY POLICY: how Redis will select what to remove when maxmemory
# maxmemory-policy noeviction
# maxmemory-samples 5
```
# 客户端查看：
```
127.0.0.1:6379> CONFIG GET maxmemory*
1) "maxmemory"
2) "0"
3) "maxmemory-samples"
4) "5"
5) "maxmemory-policy"
6) "noeviction"
127.0.0.1:6379>
```


# 驱逐策略 - 可选参数
```
volatile-lru:从已设置过期时间的内存数据集中挑选最近最少使用的数据 淘汰；
volatile-ttl: 从已设置过期时间的内存数据集中挑选即将过期的数据 淘汰；
volatile-random:从已设置过期时间的内存数据集中任意挑选数据 淘汰；
allkeys-lru:从内存数据集中挑选最近最少使用的数据 淘汰；
allkeys-random:从数据集中任意挑选数据 淘汰；
no-enviction(驱逐)：禁止驱逐数据。（默认淘汰策略。当redis内存数据达到maxmemory，在该策略下，直接返回OOM错误）；
```