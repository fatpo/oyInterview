   * [数据准备](#数据准备)
   * [compact 行格式](#compact-行格式)
      * [记录的额外信息](#记录的额外信息)
         * [变长字段长度列表](#变长字段长度列表)
         * [NULL值列表](#null值列表)
         * [重头戏：记录头信息](#重头戏记录头信息)
      * [记录的真实数据](#记录的真实数据)
   * [redundant 行格式（5.0之前）](#redundant-行格式50之前)
      * [为啥 redundant 可以没有 NULL 值列表](#为啥-redundant-可以没有-null-值列表)
      * [redundant 的char(M) 的存储格式](#redundant-的charm-的存储格式)
   * [dynamic 行格式 (5.7默认)](#dynamic-行格式-57默认)
   * [compressed 行格式](#compressed-行格式)
   * [记录溢出](#记录溢出)
      * [列的极限](#列的极限)
      * [VARCHAR(M)最多能存储的数据](#varcharm最多能存储的数据)
         * [先看一个列的组成部分:](#先看一个列的组成部分)
         * [根据这个字段允不允许NULL来判断真实数据最大长度：](#根据这个字段允不允许null来判断真实数据最大长度)
         * [根据字符集来判断真实数据最大长度](#根据字符集来判断真实数据最大长度)
         * [1 页 16KB，也才16384个字节，那么65532个字节怎么存？？](#1-页-16kb也才16384个字节那么65532个字节怎么存)
   
# 数据准备
```dtd
create database test;

use test;

CREATE TABLE record_format_demo (
         c1 VARCHAR(10),
         c2 VARCHAR(10) NOT NULL,
         c3 CHAR(10),
         c4 VARCHAR(10)
   ) CHARSET=ascii ROW_FORMAT=COMPACT;

INSERT INTO record_format_demo(c1, c2, c3, c4) VALUES('aaaa', 'bbb', 'cc', 'd'), ('eeee', 'fff', NULL, NULL);

mysql> SELECT * FROM record_format_demo;
+------+-----+------+------+
| c1   | c2  | c3   | c4   |
+------+-----+------+------+
| aaaa | bbb | cc   | d    |
| eeee | fff | NULL | NULL |
+------+-----+------+------+
2 rows in set (0.00 sec)

mysql> show table status like 'record_format_demo' \G;
*************************** 1. row ***************************
           Name: record_format_demo
         Engine: InnoDB
        Version: 10
     Row_format: Dynamic
           Rows: 2
 Avg_row_length: 8192
    Data_length: 16384
Max_data_length: 0
   Index_length: 0
      Data_free: 0
 Auto_increment: NULL
    Create_time: 2020-08-01 14:44:35
    Update_time: 2020-08-01 14:45:27
     Check_time: NULL
      Collation: ascii_general_ci
       Checksum: NULL
 Create_options:
        Comment:
1 row in set (0.00 sec)
```

# compact 行格式
![compat行格式示意图](../imgs/mysql/compat行格式示意图.png?raw=true)

## 记录的额外信息
### 变长字段长度列表
关键词如下：
* 逆序(逆序是因为指针在记录的中间，往左可以遍历记录的额外信息，往右可以遍历记录的真实数据，所有往左的那部分要逆序)
* varchar, 某些字符集的 char, text, blob
* 只能存储非 null 字段（null 字段有别的逻辑去处理）
* 表示可变字段长度时，要么一字节要么两字节

何时一字节，何时两字节：
* 基础概念
    * M: varchar(M) 表示最大存储 M 个**字符**（非字节）
    * W: width, 表示每个字符需要 W 个字节去表示
    * L: 该行该列，实际字符串占用 L 个字节
* 判断标准
    * 如果 M * W >= 255 && L > 127，两个字节，除去 1 个 bit 做为单双字节标记，剩余15个bit可以表示32768个字节长度
    * 如果 M * W >= 255 && L <= 127，一个字节
    * 如果 M * W < 255，一个字节

假如连续的几个字节如010304,怎么判断它属于一字节还是两字节的其中之一：
* 8个 bit 的最高位，如果是 0 表示单字节
* 8个 bit 的最高位，如果是 1 表示它是双字节的一半

varchar 肯定是变长字段，那么 char(10)呢？
* varchar 确实是变长字段
* char(10) 在字符集是变长的时候，也会化身为变长字段，如 gbk(每个字符可能是1-2字节), utf-8(每个字符可能是1-3字节)
* char(10) **以防碎片**, 在变长字符集时，至少占用 10 字节，就算是空字符串也要占着。


### NULL值列表
关键词：
* 0,1bit 来表示
* 逆序(理由同可变字段长度列表)
* 只表示可为 NULL 的 列(排除：主键列、被NOT NULL修饰的列)
* 高位补0: 比如我只有 3 个可为 NULL 字段，都为 1，那么就是 111，补全后：00000111
![NULL值列表](../imgs/mysql/NULL值列表.png?raw=true)

### 重头戏：记录头信息
![记录头信息](../imgs/mysql/记录头信息.png?raw=true)

| 名称	| 大小（单位：bit） |	描述 |
| :-----|---- | :---- |
|预留位1|	1	|没有使用|
|预留位2|	1	|没有使用|
|delete_mask|	1	|标记该记录是否被删除|
|min_rec_mask|	1	|B+树的每层非叶子节点中的最小记录都会添加该标记|
|n_owned|	4	|表示当前记录拥有的记录数|
|heap_no|	13	|表示当前记录在记录堆的位置信息|
|record_type|	3	|表示当前记录的类型，0表示普通记录，1表示B+树非叶子节点记录，2表示最小记录，3表示最大记录|
|next_record|	16	|表示下一条记录的相对位置|

最关心这几个：
* delete_mask 删除标记位 
* next_record 下一条记录的相对位置（优化点，快速定位下一条记录的 offset） 
* record_type 记录类型，B+树的哪个位置？叶子？非叶子？最大记录，最小记录


## 记录的真实数据
对于record_format_demo表来说，记录的真实数据除了c1、c2、c3、c4这几个我们自己定义的列的数据以外，MySQL会为每个记录默认的添加一些列（也称为隐藏列），具体的列如下：

| 列名	| 是否必须 |	占用空间	| 描述 |
| :-----|:---- | :---- |:---- |
|row_id	|否	|6字节	|行ID，唯一标识一条记录|
|transaction_id	|是	|6字节|	事务ID|
|roll_pointer	|是	|7字节|	回滚指针|

为啥只有 row_id 才是可选？什么情况下 mysql 会自动帮你添加 row_id 列？
* 没有自定义主键
* 非空Unique键

所以对于record_format_demo表来说，真实的 2 个记录：
![记录的真实数据](../imgs/mysql/记录的真实数据.png?raw=true)

# redundant 行格式（5.0之前）

![Redundant行格式示意图](../imgs/mysql/Redundant行格式示意图.png?raw=true)

* 和 compact行格式 类似
* 记录的额外信息变了
    * redundant: **字符长度偏移列表** + 记录头信息
    * compact: 变长字段长度列表 + NULL 值列表 + 记录头信息

## 为啥 redundant 可以没有 NULL 值列表
* 因为这个信息嵌入在字段长度偏移列表的高位 bit
* 也就是说在解析一条记录的某个列时，首先看一下该列对应的偏移量的NULL比特位是不是为1，如果为1，那么该列的值就是NULL，否则不是NULL

   
## redundant 的char(M) 的存储格式
* 占用的真实数据空间就是该字符集表示一个字符最多需要的字节数和M的乘积
* ascii: char(10) = 10个字节
* gbk: char(10) = 20个字节
* utf-8: char(10) = 30个字节

反正不会产生碎片！

# dynamic 行格式 (5.7默认)
* 和 compact行格式 类似
* 记录的真实数据那里：溢出列不再存储 768 个字节 + 20字节的溢出页地址，而是单纯放 20 字节的溢出页地址


# compressed 行格式
* 和 compact行格式 类似
* 行格式会采用压缩算法对页面进行压缩


# 记录溢出
## 列的极限
* 除了 text 和 blob，其他所有的列（不包括隐藏列和记录头信息）占用的字节长度加起来不能超过**65535**个字节(是字节！)

 
## VARCHAR(M)最多能存储的数据
### 先看一个列的组成部分:
* 真实数据（要求这个X？）
* 真实数据占用字节的长度
* NULL值标识，如果该列有NOT NULL属性则可以没有这部分存储空间

### 根据这个字段允不允许NULL来判断真实数据最大长度：
* 如果不为 NULL,那么就 65533 + 2 个字节的长度
* 如果不为 NULL,那么就 65532 + 2 个字节的长度 + 1 个字节的 NULL标记

### 根据字符集来判断真实数据最大长度
* ascii: 1 个字符就是 1 个字节，真实数据最大长度可以 65532
* gbk: 1 个字符最大可能是 2 字节，真实数据最大长度可以是 32766
* utf-8: 1 个字符最大可能是 3 字节，真实数据最大长度可以是 21844

### 1 页 16KB，也才16384个字节，那么65532个字节怎么存？？
![记录数据溢出示意图](../imgs/mysql/记录数据溢出示意图.png?raw=true)
