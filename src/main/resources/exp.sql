def interest = to_char(measure_time, 'yyyy') between '2000' and '2014' and rain > 0
def stddev_val = (select stddev(rain)/10 from rain where rain > 0 and to_char(measure_time, 'yyyy') between '1937' and '1980')
select
round(count(*)*100.0 / (select count(*) from rain where &interest), 1) as percentage
from
(select (rain-(mod(rain,&stddev_val))) / &stddev_val as grouped from rain where &interest)
group by grouped
order by grouped asc;

