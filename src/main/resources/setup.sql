drop table rain;
-- date format oracle : yyyy-MM-dd HH24:MI:SS

create table rain (measure_time timestamp, rain number(15,5), quality number);

select stddev(rain) from rain where rain > 0 order by measure_time asc;

select * from rain order by rain desc;
