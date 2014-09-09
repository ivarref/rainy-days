select 2*stddev(rain) from rain where rain > 0 
and to_char(measure_time, 'yyyy') >= '1950'
and to_char(measure_time, 'yyyy') <= '1980';

select to_char(measure_time, 'yyyy') as year, sum(rain)
from rain x
where to_char(measure_time, 'MM') in ('06', '07', '08')
group by to_char(measure_time, 'yyyy')
order by to_char(measure_time, 'yyyy') desc;

select to_char(measure_time, 'yyyy') as year, count(*) as antall_regndager
from rain
where to_char(measure_time, 'MM') in ('06', '07', '08') and rain > 0
group by to_char(measure_time, 'yyyy')
order by to_char(measure_time, 'yyyy') desc;


select to_char(measure_time, 'yyyy') as year, count(*) as antall_regndager
from rain
where to_char(measure_time, 'MM') in ('06', '07', '08') and rain > 0
group by to_char(measure_time, 'yyyy')
order by to_char(measure_time, 'yyyy') desc;




