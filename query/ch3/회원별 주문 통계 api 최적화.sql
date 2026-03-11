

use portfolio;

explain SELECT
        m.email as memberEmail,
        COUNT(*) as totalOrders,
        SUM(o.total_amount) as totalAmount,
        AVG(o.total_amount) as averageAmount 
    FROM
        ch3_orders o 
    JOIN
        ch2_members m 
            ON o.member_id = m.id 
    GROUP BY
        m.email 
    HAVING
        SUM(o.total_amount) >= 5000
    limit
        100

SHOW INDEX FROM ch2_members;



explain select
    o1_0.member_id,
    o1_0.avg_amount,
    o1_0.email,
    o1_0.last_order_date,
    o1_0.order_count,
    o1_0.total_amount,
    o1_0.updated_at 
from
    ch3_order_stats o1_0 
where
    o1_0.total_amount >= 5000
limit
    0, 100

select * from ch3_order_stats o1_0;