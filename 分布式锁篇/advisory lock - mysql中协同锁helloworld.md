* [协同(Advisory)锁](#协同advisory锁)
* [mysql 提供协同锁API](#mysql-提供协同锁api)
* [实操](#实操)


# 协同(Advisory)锁
* 协同锁<big>**没有能力**</big>避免来自其它客户端的数据访问，但它们基于一种概念，即所有客户端会使用一个约定的规则来协同使用一种资源。
* 这种约定的规则可以是一个锁名，即一个简单的字符串。
* 当这个名字被锁住，此协同锁认为被获取。那么了解到此锁状态的其它的客户端就会抑制并避免其进行和持有锁的用户具有冲突的操作。


# mysql 提供协同锁API
获取锁, 锁名字：a，等待时间: 1s:
```sql
select get_lock("a", 1);
```
释放锁：
```sql
select release_lock("a");
```
查看锁释放了没：
```sql
select is_free_lock("a");
```
查看锁使用了没：
```sql
select is_used_lock("a");
```

# 实操
|  会话1   | 会话2  |
| :-----| ----: | 
| select get_lock("a", 1); <br> 1 <br> 获得锁|  |
|   | select get_lock("a", 1);  <br> 0 <br> 等了一秒后没有获得锁 |
|   | select is_used_lock("a");  <br> 5  <br> sessionId=5会话在占用锁|
|   | select is_free_lock("a");  <br> 0  <br> 这个锁并没有释放|
|   | select release_lock("a");  <br> 0 <br> 非锁拥有者释放锁失败|
| select release_lock("a"); <br> 1 <br> 释放锁 |   |
|   | select is_used_lock("a"); <br> NULL <br> 锁空闲 |
|   | select is_free_lock("a"); <br> 1 <br> 锁空闲 |
|   | select get_lock("a", 1); <br> 1 <br> 获得锁|
|select is_used_lock("a"); <br> 6 <br> sessionId=6会话在占用锁   |  |
| select is_free_lock("a"); <br> 0 <br> 锁被占用  |  |
| | select release_lock("a"); <br> 1 <br> 锁释放   |
|select is_free_lock("a"); <br> 1 <br> 锁空闲 |  |