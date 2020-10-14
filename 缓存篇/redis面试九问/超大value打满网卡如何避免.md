# 超大value打满网卡如何避免

## 从源头限制
获取用户Post过来的数据，对Key，Value长度进行限制，避免产生超大的Key,Value，打满网卡

## 缩小规模，multiGet
可以将超大value的数据拆分成几个Key-value,用multiGet（同时获取多个key的值）取值,降低IO消耗.


