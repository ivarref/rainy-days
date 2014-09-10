create index measure_time_idx on rain(measure_time);
commit;

select 
to_char(measure_time, 'yyyy-MM-dd') as measure_time, 
to_char(measure_time, 'yyyy') as year, 
to_char(measure_time, 'MM-dd') as month_and_day, 
round((select avg(rain) from rain r1 where r1.measure_time between r2.measure_time-30 and r2.measure_time),2) as rain_mma
from rain r2
where measure_time > (select min(measure_time)+30 from rain)
and to_char(measure_time, 'yyyy') = '2014'
order by measure_time asc;
