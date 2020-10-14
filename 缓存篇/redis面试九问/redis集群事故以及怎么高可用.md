# redis集群事故以及怎么高可用

```
事前：redis 高可用，主从+哨兵，redis cluster，避免全盘崩溃。
事中：本地 ehcache 缓存 + hystrix 限流&降级，避免 MySQL 被打死。
事后：redis 持久化，一旦重启，自动从磁盘上加载数据，快速恢复缓存数据。
```
