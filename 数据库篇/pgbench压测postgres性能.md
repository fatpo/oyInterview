## 准备postgres
```dtd
docker pull postgres

docker run --name postgres1 \ 
    -e POSTGRES_PASSWORD=123qwe \
    -e POSTGRES_USER=root \
    -v /Users/ouyang/postgres_home/data:/var/lib/postgresql/data \
    -d -p5432:5432 postgres

docker run --rm -t -i --link postgres1:pg postgres  bash
docker exec -it 468df2e9ea61 bash

```

## 准备测试数据
```dtd
root@f749151d0325:/# psql -h 172.17.0.3 -p 5432 -U root -W
Password:
psql (12.3 (Debian 12.3-1.pgdg100+1))
Type "help" for help.
root=#
root=#
root=# create table lock_test (
root(#   tid int primary key,   -- 任务ID
root(#   state int default 1,   -- 任务状态，1表示初始状态，-1表示正在处理, 0表示处理结束
root(#   retry int default -1,   -- 重试次数
root(#   info text,   -- 其他信息
root(#   crt_time timestamp  -- 时间
root(# );
CREATE TABLE
root=#
root=#
root=# \d
         List of relations
 Schema |   Name    | Type  | Owner
--------+-----------+-------+-------
 public | lock_test | table | root
(1 row)

root=# \d lock_test
                           Table "public.lock_test"
  Column  |            Type             | Collation | Nullable |    Default
----------+-----------------------------+-----------+----------+---------------
 tid      | integer                     |           | not null |
 state    | integer                     |           |          | 1
 retry    | integer                     |           |          | '-1'::integer
 info     | text                        |           |          |
 crt_time | timestamp without time zone |           |          |
Indexes:
    "lock_test_pkey" PRIMARY KEY, btree (tid)
    
root=# insert into lock_test values (1, 1, -1, 'test', now());
INSERT 0 1
root=#
```

## test1.sql
```dtd
update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1 and get_lock("test_lock", 0);
update lock_test set state=1 where tid=1 and state=-1 and release_lock("test_lock");
```

## test2.sql
```dtd
update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1;
update lock_test set state=1 where tid=1 and state=-1;
```


## 带协同锁的执行结果
```dtd
root@468df2e9ea61:/# pgbench -M prepared -n -r -P 1 -f ./test2.sql -c 64 -j 64 -T 120
progress: 1.0 s, 7155.6 tps, lat 4.366 ms stddev 13.091
progress: 2.0 s, 14216.2 tps, lat 4.395 ms stddev 14.229
progress: 3.0 s, 15069.8 tps, lat 4.259 ms stddev 14.130
progress: 4.0 s, 16134.1 tps, lat 3.970 ms stddev 12.980
progress: 5.0 s, 15164.0 tps, lat 4.151 ms stddev 14.527
progress: 6.0 s, 14957.3 tps, lat 4.350 ms stddev 14.174
progress: 7.0 s, 15050.6 tps, lat 4.242 ms stddev 13.328
progress: 8.0 s, 15114.3 tps, lat 4.240 ms stddev 13.704
progress: 9.0 s, 13516.8 tps, lat 4.712 ms stddev 14.074
progress: 10.0 s, 13635.6 tps, lat 4.595 ms stddev 15.202
progress: 11.0 s, 14468.5 tps, lat 4.518 ms stddev 19.869
progress: 12.0 s, 15493.2 tps, lat 4.132 ms stddev 13.915
progress: 13.0 s, 16954.0 tps, lat 3.770 ms stddev 13.563
progress: 14.0 s, 14776.2 tps, lat 4.313 ms stddev 15.838
progress: 15.0 s, 15154.1 tps, lat 4.242 ms stddev 14.710
progress: 16.0 s, 17114.1 tps, lat 3.729 ms stddev 13.640
progress: 17.0 s, 18200.8 tps, lat 3.516 ms stddev 14.784
progress: 18.0 s, 16585.3 tps, lat 3.871 ms stddev 14.144
progress: 19.0 s, 16776.8 tps, lat 3.802 ms stddev 14.098
progress: 20.0 s, 17973.0 tps, lat 3.547 ms stddev 14.538
progress: 21.0 s, 18250.5 tps, lat 3.508 ms stddev 15.412
progress: 22.0 s, 18414.4 tps, lat 3.466 ms stddev 14.944
progress: 23.0 s, 15585.5 tps, lat 4.121 ms stddev 16.834
progress: 24.0 s, 14167.1 tps, lat 4.485 ms stddev 18.865
progress: 25.0 s, 19561.2 tps, lat 3.291 ms stddev 14.432
progress: 26.0 s, 19901.5 tps, lat 3.211 ms stddev 15.016
progress: 27.0 s, 19699.7 tps, lat 3.248 ms stddev 14.206
progress: 28.0 s, 18778.3 tps, lat 3.424 ms stddev 14.074
progress: 29.0 s, 19003.6 tps, lat 3.347 ms stddev 14.602
progress: 30.0 s, 19403.9 tps, lat 3.306 ms stddev 14.706
progress: 31.0 s, 20899.3 tps, lat 3.052 ms stddev 14.157
progress: 32.0 s, 20439.5 tps, lat 3.135 ms stddev 14.770
progress: 33.0 s, 19584.0 tps, lat 3.274 ms stddev 14.274
progress: 34.0 s, 19382.3 tps, lat 3.270 ms stddev 14.737
progress: 35.0 s, 20908.9 tps, lat 3.090 ms stddev 14.707
progress: 36.0 s, 20893.3 tps, lat 3.069 ms stddev 14.177
progress: 37.0 s, 20854.9 tps, lat 3.058 ms stddev 14.400
progress: 38.0 s, 19446.3 tps, lat 3.265 ms stddev 14.404
progress: 39.0 s, 2299.3 tps, lat 28.234 ms stddev 39.363
progress: 40.0 s, 1137.0 tps, lat 55.923 ms stddev 26.499
progress: 41.0 s, 1212.0 tps, lat 52.977 ms stddev 23.712
progress: 42.0 s, 1159.0 tps, lat 55.262 ms stddev 30.133
progress: 43.0 s, 1295.0 tps, lat 49.459 ms stddev 23.906
progress: 44.0 s, 1230.0 tps, lat 52.129 ms stddev 24.147
progress: 45.0 s, 1211.0 tps, lat 52.692 ms stddev 24.469
progress: 46.0 s, 1186.0 tps, lat 54.009 ms stddev 25.140
progress: 47.0 s, 761.9 tps, lat 82.325 ms stddev 74.730
progress: 48.0 s, 1206.0 tps, lat 54.220 ms stddev 26.405
progress: 49.0 s, 1111.1 tps, lat 56.910 ms stddev 26.992
progress: 50.0 s, 1185.8 tps, lat 54.446 ms stddev 25.446
progress: 51.0 s, 1020.1 tps, lat 62.266 ms stddev 30.484
progress: 52.0 s, 1105.7 tps, lat 57.327 ms stddev 28.665
progress: 53.0 s, 1148.4 tps, lat 56.508 ms stddev 28.610
progress: 54.0 s, 1248.9 tps, lat 51.307 ms stddev 24.273
progress: 55.0 s, 1149.1 tps, lat 55.459 ms stddev 26.371
progress: 56.0 s, 1143.9 tps, lat 56.182 ms stddev 25.218
progress: 57.0 s, 1112.1 tps, lat 57.620 ms stddev 28.497
progress: 58.0 s, 1002.9 tps, lat 63.419 ms stddev 31.482
progress: 59.0 s, 1065.1 tps, lat 60.671 ms stddev 27.219
progress: 60.0 s, 1192.9 tps, lat 53.457 ms stddev 24.010
progress: 61.0 s, 1118.1 tps, lat 57.420 ms stddev 28.198
progress: 62.0 s, 1165.1 tps, lat 54.859 ms stddev 26.068
progress: 63.0 s, 1147.9 tps, lat 55.417 ms stddev 23.447
progress: 64.0 s, 1175.1 tps, lat 54.424 ms stddev 25.722
progress: 65.0 s, 1274.7 tps, lat 50.185 ms stddev 24.305
progress: 66.0 s, 1056.1 tps, lat 60.105 ms stddev 37.930
progress: 67.0 s, 1308.1 tps, lat 49.553 ms stddev 26.863
progress: 68.0 s, 1367.2 tps, lat 46.979 ms stddev 23.360
progress: 69.0 s, 1332.8 tps, lat 48.186 ms stddev 22.923
progress: 70.0 s, 1398.1 tps, lat 45.805 ms stddev 20.995
progress: 71.0 s, 1351.9 tps, lat 47.042 ms stddev 20.777
progress: 72.0 s, 1281.0 tps, lat 50.160 ms stddev 23.609
progress: 73.0 s, 1360.1 tps, lat 47.027 ms stddev 21.722
progress: 74.0 s, 1292.0 tps, lat 49.333 ms stddev 23.181
progress: 75.0 s, 1306.0 tps, lat 49.351 ms stddev 22.738
progress: 76.0 s, 1376.9 tps, lat 46.445 ms stddev 22.468
progress: 77.0 s, 1359.1 tps, lat 47.055 ms stddev 23.007
progress: 78.0 s, 1249.9 tps, lat 50.952 ms stddev 23.045
progress: 79.0 s, 1121.0 tps, lat 56.809 ms stddev 26.056
progress: 80.0 s, 1231.0 tps, lat 52.356 ms stddev 27.286
progress: 81.0 s, 1308.0 tps, lat 49.070 ms stddev 22.641
progress: 82.0 s, 1328.0 tps, lat 48.037 ms stddev 22.761
progress: 83.0 s, 1336.0 tps, lat 47.915 ms stddev 22.443
progress: 84.0 s, 1249.1 tps, lat 51.211 ms stddev 24.565
progress: 85.0 s, 1388.9 tps, lat 46.109 ms stddev 22.140
progress: 86.0 s, 1141.9 tps, lat 56.197 ms stddev 26.602
progress: 87.0 s, 1393.2 tps, lat 45.879 ms stddev 20.271
progress: 88.0 s, 1344.9 tps, lat 47.478 ms stddev 21.467
progress: 89.0 s, 1282.0 tps, lat 49.983 ms stddev 23.159
progress: 90.0 s, 1312.0 tps, lat 48.354 ms stddev 22.335
progress: 91.0 s, 1110.9 tps, lat 56.926 ms stddev 27.913
progress: 92.0 s, 1089.2 tps, lat 58.780 ms stddev 28.782
progress: 93.0 s, 761.8 tps, lat 77.320 ms stddev 41.213
progress: 94.0 s, 933.2 tps, lat 73.672 ms stddev 49.178
progress: 95.0 s, 948.9 tps, lat 68.147 ms stddev 37.432
progress: 96.0 s, 910.1 tps, lat 70.612 ms stddev 38.453
progress: 97.0 s, 972.0 tps, lat 65.754 ms stddev 32.652
progress: 98.2 s, 790.5 tps, lat 66.286 ms stddev 34.046
progress: 99.0 s, 1040.1 tps, lat 77.997 ms stddev 74.527
progress: 100.0 s, 869.1 tps, lat 73.022 ms stddev 37.076
progress: 101.0 s, 1150.9 tps, lat 56.743 ms stddev 48.983
progress: 102.0 s, 726.0 tps, lat 84.860 ms stddev 52.402
progress: 103.0 s, 527.0 tps, lat 115.342 ms stddev 58.170
progress: 104.0 s, 451.2 tps, lat 142.494 ms stddev 79.714
progress: 105.1 s, 452.9 tps, lat 118.083 ms stddev 54.202
progress: 106.0 s, 526.0 tps, lat 146.816 ms stddev 106.543
progress: 107.0 s, 524.2 tps, lat 127.875 ms stddev 71.184
progress: 108.0 s, 704.1 tps, lat 91.530 ms stddev 39.915
progress: 109.0 s, 727.0 tps, lat 87.456 ms stddev 39.534
progress: 110.0 s, 690.0 tps, lat 92.674 ms stddev 39.701
progress: 111.0 s, 722.9 tps, lat 88.471 ms stddev 42.529
progress: 112.0 s, 623.2 tps, lat 102.446 ms stddev 43.544
progress: 113.0 s, 661.9 tps, lat 95.426 ms stddev 40.532
progress: 114.0 s, 662.7 tps, lat 98.162 ms stddev 47.014
progress: 115.0 s, 635.3 tps, lat 101.221 ms stddev 42.984
progress: 116.0 s, 719.0 tps, lat 90.135 ms stddev 41.771
progress: 117.0 s, 1133.1 tps, lat 57.165 ms stddev 27.947
progress: 118.0 s, 1298.9 tps, lat 49.368 ms stddev 22.726
progress: 119.0 s, 1087.0 tps, lat 58.997 ms stddev 29.049
progress: 120.0 s, 1219.0 tps, lat 52.252 ms stddev 25.303
transaction type: ./test2.sql
scaling factor: 1
query mode: prepared
number of clients: 64
number of threads: 64
duration: 120 s
number of transactions actually processed: 737901
latency average = 10.368 ms
latency stddev = 26.128 ms
tps = 6142.576832 (including connections establishing)
tps = 6168.538033 (excluding connections establishing)
statement latencies in milliseconds:
         5.172  update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1 and pg_try_advisory_xact_lock(1) returning *;
         5.179  update lock_test set state=1 where tid=1 and state=-1 and pg_try_advisory_xact_lock(1);
```

# 不带协议锁的结果
```dtd
root@468df2e9ea61:/# pgbench -M prepared -n -r -P 1 -f ./test1.sql -c 64 -j 64 -T 120
progress: 1.0 s, 523.8 tps, lat 53.084 ms stddev 43.244
progress: 2.0 s, 700.1 tps, lat 94.085 ms stddev 71.832
progress: 3.0 s, 828.0 tps, lat 76.573 ms stddev 55.679
progress: 4.0 s, 854.0 tps, lat 74.213 ms stddev 49.206
progress: 5.0 s, 871.9 tps, lat 74.626 ms stddev 49.310
progress: 6.0 s, 792.0 tps, lat 78.621 ms stddev 53.977
progress: 7.0 s, 799.0 tps, lat 80.779 ms stddev 55.429
progress: 8.0 s, 866.1 tps, lat 75.490 ms stddev 50.695
progress: 9.0 s, 865.0 tps, lat 73.520 ms stddev 46.644
progress: 10.0 s, 776.8 tps, lat 82.195 ms stddev 57.954
progress: 11.0 s, 731.0 tps, lat 88.037 ms stddev 59.603
progress: 12.0 s, 452.8 tps, lat 114.667 ms stddev 90.702
progress: 13.0 s, 633.3 tps, lat 116.479 ms stddev 105.017
progress: 14.0 s, 687.0 tps, lat 93.287 ms stddev 75.800
progress: 15.0 s, 710.9 tps, lat 92.290 ms stddev 72.614
progress: 16.0 s, 634.1 tps, lat 99.107 ms stddev 67.364
progress: 17.0 s, 715.0 tps, lat 91.544 ms stddev 73.968
progress: 18.0 s, 635.1 tps, lat 99.366 ms stddev 69.830
progress: 19.0 s, 711.8 tps, lat 89.552 ms stddev 64.334
progress: 20.0 s, 604.2 tps, lat 107.445 ms stddev 79.577
progress: 21.0 s, 633.0 tps, lat 98.044 ms stddev 67.652
progress: 22.0 s, 498.4 tps, lat 111.052 ms stddev 73.156
progress: 23.0 s, 243.2 tps, lat 261.476 ms stddev 193.020
progress: 24.0 s, 215.1 tps, lat 285.115 ms stddev 236.040
progress: 25.0 s, 451.1 tps, lat 167.563 ms stddev 162.981
progress: 26.0 s, 716.2 tps, lat 92.828 ms stddev 67.420
progress: 27.0 s, 763.9 tps, lat 83.780 ms stddev 61.477
progress: 28.0 s, 598.0 tps, lat 99.070 ms stddev 69.312
progress: 29.0 s, 647.0 tps, lat 101.768 ms stddev 80.691
progress: 30.0 s, 608.0 tps, lat 108.435 ms stddev 100.881
progress: 31.0 s, 718.9 tps, lat 88.326 ms stddev 71.728
progress: 32.0 s, 725.2 tps, lat 90.013 ms stddev 83.856
progress: 33.0 s, 347.3 tps, lat 167.624 ms stddev 151.493
progress: 34.0 s, 389.7 tps, lat 176.134 ms stddev 159.455
progress: 35.0 s, 909.0 tps, lat 72.207 ms stddev 60.041
progress: 36.0 s, 928.0 tps, lat 68.986 ms stddev 54.430
progress: 37.0 s, 989.9 tps, lat 64.621 ms stddev 47.996
progress: 38.0 s, 917.0 tps, lat 69.431 ms stddev 50.215
progress: 39.0 s, 460.2 tps, lat 125.344 ms stddev 116.954
progress: 40.0 s, 660.4 tps, lat 100.167 ms stddev 105.277
progress: 41.0 s, 374.0 tps, lat 173.619 ms stddev 142.347
progress: 42.0 s, 435.4 tps, lat 144.768 ms stddev 122.661
progress: 43.0 s, 506.5 tps, lat 114.698 ms stddev 98.824
progress: 44.0 s, 495.2 tps, lat 144.637 ms stddev 140.836
progress: 45.0 s, 427.0 tps, lat 138.264 ms stddev 116.413
progress: 46.0 s, 476.9 tps, lat 139.217 ms stddev 120.514
progress: 47.1 s, 306.2 tps, lat 164.980 ms stddev 142.868
progress: 48.0 s, 292.5 tps, lat 253.793 ms stddev 215.114
progress: 49.0 s, 401.0 tps, lat 166.156 ms stddev 150.084
progress: 50.0 s, 395.9 tps, lat 160.073 ms stddev 147.263
progress: 51.0 s, 407.8 tps, lat 159.367 ms stddev 137.160
progress: 52.0 s, 411.3 tps, lat 157.266 ms stddev 134.828
progress: 53.0 s, 380.8 tps, lat 163.757 ms stddev 138.469
progress: 54.0 s, 524.3 tps, lat 135.079 ms stddev 141.358
progress: 55.0 s, 771.0 tps, lat 81.858 ms stddev 70.377
progress: 56.0 s, 707.0 tps, lat 91.230 ms stddev 73.563
progress: 57.0 s, 797.1 tps, lat 81.973 ms stddev 64.471
progress: 58.0 s, 860.9 tps, lat 73.289 ms stddev 62.204
progress: 59.0 s, 825.0 tps, lat 76.150 ms stddev 64.472
progress: 60.0 s, 923.0 tps, lat 71.236 ms stddev 62.876
progress: 61.0 s, 829.1 tps, lat 77.349 ms stddev 67.492
progress: 62.0 s, 815.9 tps, lat 78.439 ms stddev 62.942
progress: 63.0 s, 860.0 tps, lat 74.820 ms stddev 58.094
progress: 64.0 s, 790.1 tps, lat 80.569 ms stddev 60.229
progress: 65.0 s, 855.0 tps, lat 73.932 ms stddev 64.137
progress: 66.0 s, 863.6 tps, lat 73.765 ms stddev 63.736
progress: 67.0 s, 827.3 tps, lat 77.976 ms stddev 63.856
progress: 68.0 s, 958.0 tps, lat 68.312 ms stddev 55.573
progress: 69.0 s, 946.0 tps, lat 65.965 ms stddev 50.691
progress: 70.0 s, 859.1 tps, lat 76.005 ms stddev 59.643
progress: 71.0 s, 770.9 tps, lat 80.538 ms stddev 64.134
progress: 72.0 s, 934.9 tps, lat 68.682 ms stddev 56.302
progress: 73.0 s, 861.1 tps, lat 76.649 ms stddev 69.257
progress: 74.0 s, 991.0 tps, lat 64.448 ms stddev 45.732
progress: 75.0 s, 851.0 tps, lat 75.004 ms stddev 52.621
progress: 76.0 s, 853.7 tps, lat 73.943 ms stddev 58.858
progress: 77.0 s, 811.1 tps, lat 79.577 ms stddev 63.481
progress: 78.0 s, 883.2 tps, lat 71.841 ms stddev 55.869
progress: 79.0 s, 814.0 tps, lat 78.230 ms stddev 63.203
progress: 80.0 s, 801.0 tps, lat 80.740 ms stddev 62.707
progress: 81.0 s, 906.0 tps, lat 70.981 ms stddev 62.004
progress: 82.0 s, 871.0 tps, lat 73.396 ms stddev 55.437
progress: 83.0 s, 947.0 tps, lat 67.584 ms stddev 52.320
progress: 84.0 s, 1014.0 tps, lat 62.923 ms stddev 54.909
progress: 85.0 s, 835.0 tps, lat 74.295 ms stddev 61.436
progress: 86.0 s, 762.6 tps, lat 83.251 ms stddev 67.214
progress: 87.0 s, 369.0 tps, lat 72.616 ms stddev 71.435
progress: 88.0 s, 809.5 tps, lat 129.200 ms stddev 240.131
progress: 89.0 s, 792.0 tps, lat 79.203 ms stddev 62.883
progress: 90.0 s, 883.0 tps, lat 73.222 ms stddev 62.993
progress: 91.0 s, 885.9 tps, lat 70.491 ms stddev 51.610
progress: 92.0 s, 783.1 tps, lat 83.352 ms stddev 68.328
progress: 93.0 s, 1000.8 tps, lat 64.413 ms stddev 52.463
progress: 94.0 s, 1024.2 tps, lat 61.869 ms stddev 49.300
progress: 95.0 s, 997.0 tps, lat 62.760 ms stddev 50.395
progress: 96.0 s, 954.9 tps, lat 69.508 ms stddev 56.361
progress: 97.0 s, 1009.9 tps, lat 61.426 ms stddev 46.592
progress: 98.0 s, 1002.1 tps, lat 64.855 ms stddev 57.588
progress: 99.0 s, 1024.0 tps, lat 62.837 ms stddev 47.694
progress: 100.0 s, 1001.0 tps, lat 63.743 ms stddev 49.530
progress: 101.0 s, 1013.9 tps, lat 63.397 ms stddev 49.539
progress: 102.0 s, 1011.0 tps, lat 62.223 ms stddev 47.840
progress: 103.0 s, 810.9 tps, lat 80.264 ms stddev 63.115
progress: 104.0 s, 944.1 tps, lat 68.229 ms stddev 51.539
progress: 105.0 s, 878.0 tps, lat 72.440 ms stddev 51.998
progress: 106.0 s, 921.0 tps, lat 69.384 ms stddev 55.085
progress: 107.0 s, 810.7 tps, lat 76.052 ms stddev 57.816
progress: 108.0 s, 914.5 tps, lat 72.499 ms stddev 62.607
progress: 109.0 s, 843.8 tps, lat 75.295 ms stddev 56.320
progress: 110.0 s, 835.0 tps, lat 74.129 ms stddev 59.936
progress: 111.0 s, 689.1 tps, lat 97.026 ms stddev 78.485
progress: 112.0 s, 949.0 tps, lat 67.615 ms stddev 51.938
progress: 113.0 s, 979.0 tps, lat 64.871 ms stddev 48.257
progress: 114.0 s, 934.0 tps, lat 67.305 ms stddev 51.546
progress: 115.0 s, 867.0 tps, lat 74.963 ms stddev 61.285
progress: 116.0 s, 464.0 tps, lat 137.310 ms stddev 129.755
progress: 117.0 s, 652.9 tps, lat 98.932 ms stddev 79.358
progress: 118.0 s, 893.0 tps, lat 71.354 ms stddev 52.155
progress: 119.0 s, 900.0 tps, lat 70.978 ms stddev 56.878
progress: 120.0 s, 507.1 tps, lat 119.103 ms stddev 102.914
transaction type: ./test1.sql
scaling factor: 1
query mode: prepared
number of clients: 64
number of threads: 64
duration: 120 s
number of transactions actually processed: 89457
latency average = 85.574 ms
latency stddev = 82.567 ms
tps = 744.029681 (including connections establishing)
tps = 747.046241 (excluding connections establishing)
statement latencies in milliseconds:
        43.041  update lock_test set state=-1 , retry=retry+1 where tid=1 and state=1;
        42.561  update lock_test set state=1 where tid=1 and state=-1;
```


## 结论：
协同锁提高性能10倍

## pgbench的参数中文说明
```dtd
选项
下面分成了三个章节：不同的选项在数据库初始化和运行benchmark时使用， 有些选项在两种情况下都使用。

初始化选项
pgbench接受下列的命令行初始化参数：

-i
要求调用初始化模式。

-F fillfactor
用给定的填充因子创建pgbench_accounts、pgbench_tellers 和pgbench_branches表。缺省是100。

-n
在初始化之后不执行清理。

-q
日志切换到安静模式，每5秒钟只产生一条进度消息。缺省的日志输出是每100000行一条消息， 通常每秒输出多行（特别是在好的硬件上）。

-s scale_factor
乘以比例因子生成的行数。例如，-s 100将在 pgbench_accounts表中创建10,000,000行。缺省是1。 当比例是20,000或更大时，用于保存计数标识符的字段（aid字段） 将切换到使用更大的整数(bigint)，以足够保存计数标识符的范围。

--foreign-keys
在标准表之间创建外键约束。

--index-tablespace=index_tablespace
在指定的表空间中创建索引，而不是在缺省的表空间中。

--tablespace=tablespace
在指定的表空间中创建表，而不是在缺省的表空间中。

--unlogged-tables
创建所有的表为unlogged表，而不是永久表。

基准选项
pgbench接受下列的命令行基准参数：

-c clients
模拟客户端的数量，也就是并发数据库会话的数量。缺省是1。

-C
为每个事务建立一个新的连接，而不是每客户端会话只执行一次。 这对于测量连接开销是有用的。

-d
打印调试输出。

-D varname=value
定义一个自定义脚本使用的变量（见下文）。允许使用多个-D选项。

-f filename
从filename中读取事务脚本。见下文获取细节。 -N、-S、和-f是互相排斥的。

-j threads
pgbench中工作线程的数量。在多CPU的机器上使用多个线程会很有帮助。 客户端的数量必须是线程数量的倍数，因为每个线程都有相同数量的客户端会话管理。 缺省是1。

-l
记录每个事务写入日志文件的时间。见下文获取细节。

-M querymode
提交查询到服务器使用的协议：

simple：使用简单的查询协议。

extended：使用扩展的查询协议。

prepared：使用带有预备语句的扩展查询协议。

缺省是简单的查询协议。（参阅第 48 章获取更多信息。）

-n
运行测试时不执行清理。如果你正在运行一个不包含标准表pgbench_accounts、 pgbench_branches、pgbench_history、和 pgbench_tellers的自定义测试，那么该选项是必需的。

-N
不要更新pgbench_tellers和pgbench_branches。 这将避免争用这些表，但是它使得测试用例更不像TPC-B。

-r
在benchmark完成后报告每个命令的平均每语句延迟（从客户的角度看的执行时间）。 见下文获取细节。

-s scale_factor
在pgbench的输出中报告指定的比例因子。在内建的测试中，这不是必需的； 正确的比例因子将通过计数pgbench_branches表中的行数检测到。 不过，在测试自定义benchmark(-f选项)时，比例因子将报告为1，除非使用了该选项。

-S
执行只有select的事务，替代类似TPC-B的测试。

-t transactions
每个客户端运行的事务数量。缺省是10。

-T seconds
运行测试这么多秒，而不是每客户端固定数量的事务。-t 和-T是互相排斥的。

-v
在运行测试之前清理四个标准表。既不用-n也不用-v， pgbench将清理pgbench_tellers和pgbench_branches表， 截断pgbench_history表。

--aggregate-interval=seconds
汇总时间间隔的长度（以秒计）。可能只与-l选项一起使用， 日志包含每间隔的总结（事务的数量、最小/最大延迟和可用于方差估计的两个额外字段）。

目前在Windows上不支持这个选项。

--sampling-rate=rate
采样率，在写入数据到日志时使用，以减少生成日志的数量。如果给出了这个选项， 则只记录指定比例的事务。1.0意味着记录所有事务，0.05意味着只记录了5%的事务。

在处理日志文件时记得计算上采样率。例如，计算tps值时，需要乘以相应的数字 （比如，0.01的采样率，将只得到1/100的实际tps）。

公共选项
pgbench接受下列的命令行公共参数：

-h hostname
数据库服务器的主机名

-p port
数据库服务器的端口号

-U login
要连接的用户名

-V
--version
打印pgbench的版本并退出。

-?
--help
显示关于pgbench命令行参数的帮助并退出。
```