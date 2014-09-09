drop table rain;
-- date format oracle : yyyy-MM-dd HH24:MI:SS

create table rain (measure_time timestamp, rain number(15,5), quality number);

select stddev(rain) from rain where rain > 0;

select * from rain order by rain desc;

-- 1993-01-04 13:00:00

-- avg 0.9094451503600169419737399407030919102075
-- stddev 1.47714994069103761022074064706227422142

select avg(rain), stddev(rain) from rain;