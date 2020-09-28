# slowlog 慢日志
## 配置
```
➜  conf cat redis.conf | grep --color "slowlog"
slowlog-log-slower-than 10000
slowlog-max-len 128
```
`slowlog-log-slower-than`表示：
 ```
高于10000微秒也就是10ms的会被记录。
```
`slowlog-max-len`表示：
```
只会记录128个最大值
```

## 查看
`slowlog get 200` 获取前200个慢日志：
```
127.0.0.1:6379> slowlog get 200
1) 1) (integer) 0               # 唯一性(unique)的日志标识符
   2) (integer) 1600940282      # 被记录命令的执行时间点，以 UNIX 时间戳格式表示
   3) (integer) 15512           # 查询执行时间，以微秒为单位，这里是15ms
   4) 1) "eval"                 # 这里完整的命令： eval for i=1,512 do redis.call('SADD', KEYS[1],i ) end 1 integers
      2) "for i=1,512 do redis.call('SADD', KEYS[1],i ) end"
      3) "1"
      4) "integers"
   5) "127.0.0.1:56716"         # 发起请求的客户端IP和端口
   6) ""                        # 客户端名字
```