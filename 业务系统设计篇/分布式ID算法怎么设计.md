

# from
https://mp.weixin.qq.com/s?__biz=MjM5ODYxMDA5OQ==&mid=2651960245&idx=1&sn=5cef3d8ca6a3e6e94f61e0edaf985d11&chksm=bd2d06698a5a8f7fc89056af619b9b7e79b158bceb91bdeb776475bc686721e36fb925904a67&mpshare=1&scene=1&srcid=0626i5Oy0egfPAKrFc5VFrAK

# 需求
* 全局唯一
* 趋势有序

## 为啥需要趋势有序？
很多业务系统需要按照 time 排序如：
* 拉取最新的帖子
* 拉取最新的消息
* 拉取最新的订单

## 为啥不能 time 字段做为索引
但是 time 做为 2 级索引，因为回表，检索时间较长。
所以有没有 time 的替代品？

## 方案 1：数据库的auto_increment
- 方案
    * 利用数据库表的 auto_increment 自增保证唯一递增性
- 优点
    * 方案简单，依靠 DB 已有功能即可完成
    * 保证唯一性
    * 保证了递增
    * 步长固定可控
- 缺点
    * 可用性堪忧：常用的数据库是一主多从+读写分离，写是单节点，主库挂了就 GG
    * 性能上限：写请求是单点，数据库主库的写性能决定了 ID 生成的性能
    * 扩展性差：还是那句话，写请求是单点，无法扩展
- 改进
    * 方案
        * 既然写请求是单点，那么就冗余主库，用 3 库，步长都是 3，第一个库从 1开始，第二库从 2开始，第三个从 3 开始
    * 优点
        * 可用性有保证
        * 保证了趋势递增
    * 缺点
        * 丧失了这个方案的绝对递增性的优点
        * 每次生成 ID 依旧依赖数据库的写性能，增加数据库写压力

## 方案 2：单点批量ID生成服务

我很认可的一句话，分布式系统之所以难，是因为没有一个全局的时钟，难以保证绝对的时序。
很多时候，为了绝对的时序，还真的是只能使用单点服务！

- 方案
    * 批量生成ID，减少数据库主库的写压力
    
    
## 方案 3：本地uuid
- 方案
    * 不再依靠 DB，也不依赖远程调用，本地生成！
- 优点
    * 本地生成，无需远程调用，时延低
    * 拓展性好，性能贼好，理论上无上限
- 缺点
    * 1、无法保证连续性，甚至都无法保证趋势递增
    * 2、主键 64 位的字符串，作为主键索引查询效率慢
- 改进
    - 方案
        * 变成 2 个 uint64整数存储
        * 或者折半存储
    - 优点
        * 稍微能优化下上述缺点 2
    - 缺点
        * 折半存储无法保证唯一性
         
## 方案 4：本地毫秒数
- 方案
    * 不再依赖 DB，也不依赖远程调用，本地根据毫秒数生成ID
- 优点
    * 本地生成，无需远程调用，时延低
    * ID 是趋势递增的
    * ID 是整数，作为主键索引性能好，查询效率高
- 缺点
    * 致命！并发超过 1000 时，ID 重复，无法保证唯一性
- 改进
    - 方案
        * 采用微秒
    - 优点
        * 冲突概率降低，每秒可生成 1000000 个ID
    - 缺点
        * 还是那个致命问题，ID 不唯一。微秒只是降低了概率而已，治标不治本
        
## 方案5：snowflake
- 方案
    * 1 bit: 不用
    * 41 bit: 毫秒数，容纳 69 年的时间
    * 10 bit: 机器编号, 高位5bit是数据中心ID（datacenterId），低位5bit是工作节点ID（workerId），1024台服务器
    * 12 bit: 毫秒内序列号
- 优点
    * 趋势递增
    * ID唯一
- 缺点
    * 虽然每台服务器ID都是绝对递增的，但没有一个统一时钟，服务器有早有晚总体看来只能保证趋势递增
    * 如果某台机器的系统时钟回拨，有可能造成ID冲突，或者ID乱序
    
    
```java
package distributed;

/**
 * twitter的snowflake算法 -- java实现
 *
 * @author beyond
 * @date 2016/11/26
 */
public class SnowFlake {

    /**
     * 起始的时间戳
     */
    private final static long START_STMP = 1480166465631L;

    /**
     * 每一部分占用的位数
     */
    private final static long SEQUENCE_BIT = 12; //序列号占用的位数
    private final static long MACHINE_BIT = 5;   //机器标识占用的位数
    private final static long DATACENTER_BIT = 5;//数据中心占用的位数

    /**
     * 每一部分的最大值
     */
    private final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long datacenterId;  //数据中心
    private long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastStmp = -1L;//上一次时间戳

    public SnowFlake(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     *
     * @return
     */
    public synchronized long nextId() {
        long currStmp = getNewstmp();
        if (currStmp < lastStmp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currStmp == lastStmp) {
            //相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                // 这里会while死循环 等待到下一毫秒
                currStmp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStmp = currStmp;

        return (currStmp - START_STMP) << TIMESTMP_LEFT //时间戳部分
                | datacenterId << DATACENTER_LEFT       //数据中心部分
                | machineId << MACHINE_LEFT             //机器标识部分
                | sequence;                             //序列号部分
    }

    private long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }

    private long getNewstmp() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SnowFlake snowFlake = new SnowFlake(2, 3);

        for (int i = 0; i < (1 << 12); i++) {
            System.out.println(snowFlake.nextId());
        }

    }
}
```