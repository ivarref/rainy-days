drop table rain;
-- date format oracle : yyyy-MM-dd HH24:MI:SS

create table rain (measure_time timestamp, rain number(15,5), quality number);

create index measure_time_idx on rain(measure_time);
commit;

select stddev(rain) from rain where rain > 0;

select * from rain order by rain desc;

-- 1993-01-04 13:00:00

-- avg 0.9094451503600169419737399407030919102075
-- stddev 1.47714994069103761022074064706227422142

select to_char(measure_time, 'yyyy') as year, 
(select count(distinct to_char(measure_time, 'yyyy-MM-dd')) from rain
where rain > (select 2*stddev(rain) from rain where rain > 0)
and to_char(measure_time, 'yyyy') = to_char(x.measure_time, 'yyyy')) as dagar_med_ekstremnedbor
from rain x
where
rain > (select 2*stddev(rain) from rain where rain > 0)
group by to_char(measure_time, 'yyyy')
order by to_char(measure_time, 'yyyy') desc;

select to_char(measure_time, 'MM') as tid, count(*) as antall
from rain x
where
rain > (select 2*stddev(rain) from rain where rain > 0)
group by to_char(measure_time, 'MM')
order by to_char(measure_time, 'MM') asc;

select 2*stddev(rain) from rain where rain > 0;

select count(*) from rain;

select to_char(measure_time, 'yyyy-MM'), count(*)
from rain 
where 
rain > (select 2*stddev(rain) from rain where rain > 0)
and to_char(measure_time, 'yyyy') = '2010'
group by to_char(measure_time, 'yyyy-MM')
order by to_char(measure_time, 'yyyy-MM') asc
;


