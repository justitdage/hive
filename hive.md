

### hive 命令

```shell
启动hive
1 hive命令
2 前台启动 hive --server hiveserver2
3 后台启动 nohup hive --service hiveserver2 &

beeline连接hiveserver2
[hadoop@node03 ~]$ beeline
beeline> !connect jdbc:hive2://node03:10000

查看mysql的状态
service mysqld status

Hive的命令
使用 –e 参数来直接执行hql语句 
[hadoop@node03 ~]$ hive -e "show databases"

执行hive脚本
create database if not exists myhive;
[hadoop@node03 install]$ hive -f /kkb/install/hive.sql
```

### hiveDDL

```shell
建表语句
CREATE [EXTERNAL] TABLE [IF NOT EXISTS] table_name 
[(col_name data_type [COMMENT col_comment], ...)] 
[COMMENT table_comment] 
[PARTITIONED BY (col_name data_type [COMMENT col_comment], ...)] 分区
[CLUSTERED BY (col_name, col_name, ...) 分桶
[SORTED BY (col_name [ASC|DESC], ...)] INTO num_buckets BUCKETS] 
[ROW FORMAT row_format]  row format delimited fields terminated by “分隔符”
[STORED AS file_format] 
[LOCATION hdfs_path]

插入
可以通过insert into向hive表当中插入数据，但是不建议工作当中这么做；因为每个insert语句转换成mr后会生成一个文件
insert into stu(id,name) values(1,"zhangsan");
insert into stu(id,name) values(2,"lisi");

查询建表法
create table if not exists myhive.stu1 as select id, name from stu;

加载数据
本地：
load data local inpath '/kkb/install/hivedatas/teacher.csv' into table myhive.teacher;
hdfs：
load data inpath '/kkb/hdfsload/hivedatas' overwrite into table myhive.teacher;、

内部表外部表转换
alter table stu set tblproperties('EXTERNAL'='TRUE');
alter table teacher set tblproperties('EXTERNAL'='FALSE');

分区表
创建
hive (myhive)> create table score(s_id string, c_id string, s_score int) partitioned by (month string) row format delimited fields terminated by '\t';

hive (myhive)> create table score2 (s_id string,c_id string, s_score int) partitioned by (year string, month string, day string) row format delimited fields terminated by '\t';

加载数据
 hive (myhive)>load data local inpath '/kkb/install/hivedatas/score.csv' into table score partition (month='201806');
 
 hive (myhive)> load data local inpath '/kkb/install/hivedatas/score.csv' into table score2 partition(year='2018', month='06', day='01');

查看分区
hive (myhive)> show  partitions  score;
添加分区
hive (myhive)> alter table score add partition(month='201805');
添加多个分区
hive (myhive)> alter table score add partition(month='201804') partition(month = '201803');
删除分区
hive (myhive)> alter table score drop partition(month = '201806');

创建分桶表
- 在创建分桶表之前要执行的命令
- ==set hive.enforce.bucketing=true;==  开启对分桶表的支持
- ==set mapreduce.job.reduces=4;==      设置与桶相同的reduce个数（默认只有一个reduce）

- 进入hive客户端然后执行以下命令
use myhive;
set hive.enforce.bucketing=true; 
set mapreduce.job.reduces=4;  

-- 创建分桶表
create table myhive.user_buckets_demo(id int, name string)
clustered by(id) 
into 4 buckets 
row format delimited fields terminated by '\t';

-- 创建普通表
create table user_demo(id int, name string)
row format delimited fields terminated by '\t';

加载数据到普通表 user_demo 中
load data local inpath '/kkb/install/hivedatas/user_bucket.txt'  overwrite into table user_demo; 
insert into table user_buckets_demo select * from user_demo;

抽样查询桶表的数据
tablesample抽样语句语法：tablesample(bucket  x  out  of  y)

- x表示从第几个桶开始取数据
- y与进行采样的桶数的个数、每个采样桶的采样比例有关；
select * from user_buckets_demo tablesample(bucket 1 out of 2);
-- 需要采样的总桶数=4/2=2个
-- 先从第1个桶中取出数据
-- 1+2=3，再从第3个桶中取出数据




```

### hive导入数据 

```shell
1 直接插入
hive (myhive)> create table score3 like score;
hive (myhive)> insert into table score3 partition(month ='201807') values ('001','002','100');
2 load
hive (myhive)> load data local inpath '/kkb/install/hivedatas/score.csv' overwrite into table score3 partition(month='201806');
3.通过查询
hive (myhive)> create table score5 like score;
hive (myhive)> insert overwrite table score5 partition(month = '201806') select s_id,c_id,s_score from score;
4.查询语句中创建表并加载数据（as select）
hive (myhive)> create table score6 as select * from score;
5 创建表时指定location
hive (myhive)> create external table score7 (s_id string,c_id string,s_score int) row format delimited fields terminated by '\t' location '/myscore7';
6 export导出与import 导入 hive表数据（内部表操作）
hive (myhive)> create table teacher2 like teacher;
hive (myhive)> export table teacher to  '/kkb/teacher';
hive (myhive)> import table teacher2 from '/kkb/teacher';

```

### hive导出

```shell
insert 导出
1 将查询的结果导出到本地
insert overwrite local directory '/kkb/install/hivedatas/stu' select * from stu;
2.将查询的结果**格式化**导出到本地
insert overwrite local directory '/kkb/install/hivedatas/stu2' row format delimited fields terminated by ',' select * from stu;
3.将查询的结果导出到HDFS上
insert overwrite directory '/kkb/hivedatas/stu' row format delimited fields terminated by  ','  select * from stu;

 Hive Shell 命令导出
hive -e 'select * from myhive.stu;' > /kkb/install/hivedatas/student1.txt

export导出到HDFS上
export table  myhive.stu to '/kkb/install/hivedatas/stuexport';


```

### hive静态分区 动态分区

```shell
静态分区
use myhive;
create table order_partition(
order_number string,
order_price  double,
order_time string
)
partitioned BY(month string)
row format delimited fields terminated by '\t';

load data local inpath '/kkb/install/hivedatas/order.txt' overwrite into table order_partition partition(month='2019-03');


select * from order_partition where month='2019-03';



动态分区
根据分区字段不同的值，自动将数据导入到分区表不同的分区中** 

create table t_order(
    order_number string,
    order_price  double, 
    order_time   string
)row format delimited fields terminated by '\t';

--创建目标分区表
create table order_dynamic_partition(
    order_number string,
    order_price  double    
)partitioned BY(order_time string)
row format delimited fields terminated by '\t';

load data local inpath '/kkb/install/hivedatas/order_partition.txt' overwrite into table t_order;

-- 要想进行动态分区，需要设置参数
-- 开启动态分区功能
hive> set hive.exec.dynamic.partition=true; 
-- 设置hive为非严格模式
hive> set hive.exec.dynamic.partition.mode=nonstrict; 
hive> insert into table order_dynamic_partition partition(order_time) select order_number, order_price, order_time from t_order;

查看分区
hive> show partitions order_dynamic_partition;

```

#### 排序

```shell
1. order by 全局排序
只有一个reduce

2.sort by(分区内部有序)
 每个MapReduce内部排序（Sort By）局部排序
 set mapreduce.job.reduces=3;
 
 3 distribute by 采集hash算法，在map端将查询的结果中hash值相同的结果分发到对应的reduce文件中
 注意：Hive要求 **distribute by** 语句要写在 **sort by** 语句之前。
 set mapreduce.job.reduces=3;
 通过distribute by  进行数据的分区,，将不同的sid 划分到对应的reduce当中去
 insert overwrite local directory '/kkb/install/hivedatas/distribute' select * from score distribute by s_id sort by s_score;
 
 4 cluster by
 当distribute by和sort by字段相同时，可以使用cluster by方式代替
 
```

