-- Widen weight_per_bird_kg (lot weights exceed NUMERIC(5,3)).
-- Drop views that reference generated column total_amount, then recreate them unchanged.

BEGIN;

DROP VIEW IF EXISTS v_profit_loss;
DROP VIEW IF EXISTS v_global_profit_loss;

ALTER TABLE flock_sales DROP COLUMN total_amount;

ALTER TABLE flock_sales
  ALTER COLUMN weight_per_bird_kg TYPE NUMERIC(12,3);

ALTER TABLE flock_sales
  ADD COLUMN total_amount NUMERIC(12, 2)
  GENERATED ALWAYS AS (((qty_sold)::numeric * weight_per_bird_kg) * price_per_kg) STORED;

CREATE VIEW v_global_profit_loss AS
 SELECT (COALESCE(( SELECT sum(flock_sales.total_amount) AS sum
           FROM flock_sales), (0)::numeric) + COALESCE(( SELECT sum(other_sales.amount) AS sum
           FROM other_sales), (0)::numeric)) AS total_revenue,
    (((COALESCE(( SELECT sum(feed_purchases.total_cost) AS sum
           FROM feed_purchases), (0)::numeric) + COALESCE(( SELECT sum(medicine_purchases.total_cost) AS sum
           FROM medicine_purchases), (0)::numeric)) + COALESCE(( SELECT sum(brada_purchases.total_cost) AS sum
           FROM brada_purchases), (0)::numeric)) + COALESCE(( SELECT sum(expenses.amount) AS sum
           FROM expenses), (0)::numeric)) AS total_cash_outflow,
    ((COALESCE(( SELECT sum(flock_sales.total_amount) AS sum
           FROM flock_sales), (0)::numeric) + COALESCE(( SELECT sum(other_sales.amount) AS sum
           FROM other_sales), (0)::numeric)) - (((COALESCE(( SELECT sum(feed_purchases.total_cost) AS sum
           FROM feed_purchases), (0)::numeric) + COALESCE(( SELECT sum(medicine_purchases.total_cost) AS sum
           FROM medicine_purchases), (0)::numeric)) + COALESCE(( SELECT sum(brada_purchases.total_cost) AS sum
           FROM brada_purchases), (0)::numeric)) + COALESCE(( SELECT sum(expenses.amount) AS sum
           FROM expenses), (0)::numeric))) AS net_profit;

CREATE VIEW v_profit_loss AS
 SELECT flock_id,
    breed,
    arrival_date,
    close_date,
    COALESCE(( SELECT sum(fs.total_amount) AS sum
           FROM flock_sales fs
          WHERE (fs.flock_id = f.flock_id)), (0)::numeric) AS flock_revenue,
    COALESCE(( SELECT sum(((fu.sacks_used)::numeric * avg_price.cost_per_sack)) AS sum
           FROM (feed_usage fu
             JOIN ( SELECT feed_purchases.feed_type_id,
                    avg(feed_purchases.cost_per_sack) AS cost_per_sack
                   FROM feed_purchases
                  GROUP BY feed_purchases.feed_type_id) avg_price ON ((avg_price.feed_type_id = fu.feed_type_id)))
          WHERE (fu.flock_id = f.flock_id)), (0)::numeric) AS estimated_feed_cost,
    COALESCE(( SELECT sum((mu.dosage * avg_price.unit_cost)) AS sum
           FROM (medicine_usage mu
             JOIN ( SELECT medicine_purchases.medicine_id,
                    avg(medicine_purchases.unit_cost) AS unit_cost
                   FROM medicine_purchases
                  GROUP BY medicine_purchases.medicine_id) avg_price ON ((avg_price.medicine_id = mu.medicine_id)))
          WHERE (mu.flock_id = f.flock_id)), (0)::numeric) AS estimated_medicine_cost,
    COALESCE(( SELECT sum(bp.total_cost) AS sum
           FROM brada_purchases bp
          WHERE (bp.flock_id = f.flock_id)), (0)::numeric) AS brada_cost,
    COALESCE(( SELECT sum(e.amount) AS sum
           FROM expenses e
          WHERE (e.flock_id = f.flock_id)), (0)::numeric) AS expense_cost,
    (COALESCE(( SELECT sum(fs.total_amount) AS sum
           FROM flock_sales fs
          WHERE (fs.flock_id = f.flock_id)), (0)::numeric) - (((COALESCE(( SELECT sum(((fu.sacks_used)::numeric * avg_price.cost_per_sack)) AS sum
           FROM (feed_usage fu
             JOIN ( SELECT feed_purchases.feed_type_id,
                    avg(feed_purchases.cost_per_sack) AS cost_per_sack
                   FROM feed_purchases
                  GROUP BY feed_purchases.feed_type_id) avg_price ON ((avg_price.feed_type_id = fu.feed_type_id)))
          WHERE (fu.flock_id = f.flock_id)), (0)::numeric) + COALESCE(( SELECT sum((mu.dosage * avg_price.unit_cost)) AS sum
           FROM (medicine_usage mu
             JOIN ( SELECT medicine_purchases.medicine_id,
                    avg(medicine_purchases.unit_cost) AS unit_cost
                   FROM medicine_purchases
                  GROUP BY medicine_purchases.medicine_id) avg_price ON ((avg_price.medicine_id = mu.medicine_id)))
          WHERE (mu.flock_id = f.flock_id)), (0)::numeric)) + COALESCE(( SELECT sum(bp.total_cost) AS sum
           FROM brada_purchases bp
          WHERE (bp.flock_id = f.flock_id)), (0)::numeric)) + COALESCE(( SELECT sum(e.amount) AS sum
           FROM expenses e
          WHERE (e.flock_id = f.flock_id)), (0)::numeric))) AS net_profit
   FROM flocks f;

COMMIT;
