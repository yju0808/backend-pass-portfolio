


use portfolio;

select * from ch3_orders;

SHOW INDEX FROM ch3_orders;




explain select
    o1_0.id,
    o1_0.created_at,
    o1_0.member_id,
    o1_0.order_date,
    o1_0.order_number,
    o1_0.status,
    o1_0.total_amount,
    o1_0.updated_at 
from
    ch3_orders o1_0 
where
    o1_0.order_date>='2024-01-01' 
    and o1_0.status='COMPLETED' 
    and o1_0.total_amount>=10000 
order by
    o1_0.order_date desc 
limit
    0, 10


select count(*) from ch2_members;



ALTER TABLE ch3_orders
    ADD INDEX idx_order_date_status_amount (order_date DESC, status, total_amount);


DROP INDEX idx_order_date_status_amount ON ch3_orders;