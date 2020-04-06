

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

### 日期函数

```sql
1.时间戳转时间
from_unixtime(bigint unixtime[, string format]) 转成string
hive> select from_unixtime(1323308943, 'yyyyMMdd') from tableName;
20111208

2.获取当前时间戳
unix_timestamp()
hive> select unix_timestamp() from tableName;
1323309615

hive> select unix_timestamp('2011-12-07 13:01:03') from tableName;
1323234063

3.指定时间转时间戳
unix_timestamp(string date, string pattern)
hive> select unix_timestamp('20111207 13:01:03','yyyyMMdd HH:mm:ss') from tableName;
1323234063

4to_date
hive> select to_date('2011-12-08 10:03:01') from tableName;
2011-12-08

```

​	

### 字符串函数

```shell
字符串长度函数：length
hive> select length('abcedfg') from tableName;

字符串反转函数：reverse
hive> select reverse('abcedfg') from tableName;
gfdecba

字符串连接函数：concat
hive> select concat('abc','def','gh') from tableName;
abcdefgh

字符串连接并指定字符串分隔符：concat_ws
hive> select concat_ws(',','abc','def','gh') from tableName;
abc,def,gh

字符串截取函数：substr
hive> select substr('abcde',3) from tableName;
cde
hive> select substring('abcde',3) from tableName;
cde
hive> select substr('abcde',-1) from tableName;  （和ORACLE相同）
e

字符串截取函数：substr, substring 
hive> select substr('abcde',3,2) from tableName;
cd
hive> select substring('abcde',3,2) from tableName;
cd
hive>select substring('abcde',-3,2) from tableName;
cd

字符串转大写函数：upper, ucase  
字符串转小写函数：lower, lcase 

去空格函数：trim 

url解析函数  parse_url


```

​	

### 行转列

```shell
CONCAT(string A/col, string B/col…)：返回输入字符串连接后的结果，支持任意个输入字符串;

CONCAT_WS(separator, str1, str2,...)：它是一个特殊形式的 CONCAT()。

COLLECT_SET(col)：函数只接受基本数据类型，它的主要作用是将某字段的值进行去重汇总，产生**array**类型字段。
```



### 列转行

```shell
EXPLODE(col)：将hive一列中复杂的array或者map结构拆分成多行。

LATERAL VIEW

- 用法：LATERAL VIEW udtf(expression) tableAlias AS columnAlias
- 解释：用于和split, explode等UDTF一起使用，它能够将一列数据拆成多行数据，在此基础上可以对拆分后的数据进行聚合。


《疑犯追踪》	悬疑,动作,科幻,剧情
《Lie to me》	悬疑,警匪,动作,心理,剧情
《战狼2》	战争,动作,灾难

hive (hive_explode)> select movie, category_name from movie_info 
lateral view explode(category) table_tmp as category_name;

《疑犯追踪》	悬疑
《疑犯追踪》	动作
《疑犯追踪》	科幻
《疑犯追踪》	剧情
《Lie to me》	悬疑
《Lie to me》	警匪
《Lie to me》	动作
《Lie to me》	心理
《Lie to me》	剧情
《战狼2》	战争
《战狼2》	动作
《战狼2》	灾难


```

### reflect函数

```shell
reflect函数可以支持在sql中调用java中的自带函数，秒杀一切udf函数。

hive (hive_explode)> select reflect("java.lang.Math","max", col1, col2) from test_udf;

hive (hive_explode)> select reflect("org.apache.commons.lang.math.NumberUtils", "isNumber", "123");

```



### topN

```shell
1、ROW_NUMBER()：

- 从1开始，按照顺序，给分组内的记录加序列；
  - 比如，按照pv降序排列，生成分组内每天的pv名次,ROW_NUMBER()的应用场景非常多
  - 再比如，获取分组内排序第一的记录;
  - 获取一个session中的第一条refer等。 

2、RANK() ：

- 生成数据项在分组中的排名，排名相等会在名次中留下空位 

3、DENSE_RANK() ：

- 生成数据项在分组中的排名，排名相等会在名次中不会留下空位 

4、CUME_DIST ：

- 小于等于当前值的行数/分组内总行数。比如，统计小于等于当前薪水的人数，所占总人数的比例 

5、PERCENT_RANK ：

- 分组内当前行的RANK值/分组内总行数

6、NTILE(n) ：

- 用于将分组数据按照顺序切分成n片，返回当前切片值
- 如果切片不均匀，默认增加第一个切片的分布。
- NTILE不支持ROWS BETWEEN，比如 NTILE(2) OVER(PARTITION BY cookieid ORDER BY createtime ROWS BETWEEN 3 PRECEDING AND CURRENT ROW)


select * from (
SELECT 
cookieid,
createtime,
pv,
RANK() OVER(PARTITION BY cookieid ORDER BY pv desc) AS rn1,
DENSE_RANK() OVER(PARTITION BY cookieid ORDER BY pv desc) AS rn2,
ROW_NUMBER() OVER(PARTITION BY cookieid ORDER BY pv DESC) AS rn3 
FROM cookie_pv 
) temp where temp.rn1 <= 3;


```



​	