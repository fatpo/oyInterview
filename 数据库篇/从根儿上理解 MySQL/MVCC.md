```dtd
CREATE TABLE hero (
    number INT,
    name VARCHAR(100),
    country varchar(100),
    PRIMARY KEY (number)
) Engine=InnoDB CHARSET=utf8;

INSERT INTO hero VALUES
    (1, 'liubei', 'shu'),
    (3, 'zhugeliang', 'shu'),
    (8, 'caocao', 'wei'),
    (15, 'xuyu', 'wei'),
    (20, 'sunquan', 'wu');
```


## ReadView
ReadView类似快照，以下四个属性对于实现隔离级别，非常有用：
* max_trx_id: 生成 ReadView 的那一瞬间，系统应该分配给下一个事务的 ID
* m_ids: 生成 ReadView 那一瞬间，系统活跃的读写事务 ID 列表
* min_trx_id: 生成 ReadView 那一瞬间，系统活跃的读写事务 ID 列表的最小的那个事务 ID
* creator_trx_id: 生成该 ReadView 的事务 ID

关键词：
* 生成 ReadView 的那一瞬间
* 活跃的读写事务






