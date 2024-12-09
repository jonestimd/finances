USE finances;

delimiter //

drop function if exists adjust_shares//
create function
    adjust_shares(security_id bigint, from_date datetime, shares decimal(19,6))
    returns decimal(19,6)
    deterministic
    reads sql data
begin
    declare done boolean default false;
    declare shares_in  decimal(19,6);
    declare shares_out decimal(19,6);
    declare cur cursor for
        select ss.shares_out, ss.shares_in
         from stock_split ss
        where ss.security_id = security_id and ss.date >= from_date;
    declare continue handler for NOT FOUND set done = true;

    if shares is not null then
        open cur;
        read_loop: loop
            fetch cur into shares_out, shares_in;
            if done then
                leave read_loop;
            end if;
            set shares = shares * shares_out / shares_in;
        end loop read_loop;
        close cur;
    end if;
    return shares;
end//

drop view if exists account_security//
create view account_security as
with shares_out as (
  select sx.account_id
       , sx.security_id
       , rx.account_id xfer_account_id
       , sum(abs(round(pd.amount * sl.purchase_shares / pd.asset_quantity, 2))) cost_basis
  from security_lot sl
  join tx_detail pd on sl.purchase_tx_detail_id = pd.id
  join tx_detail sd on sl.related_tx_detail_id = sd.id
  join tx sx on sd.tx_id = sx.id
  left join tx_detail rd on sd.related_detail_id = rd.id
  left join tx rx on rd.tx_id = rx.id
  group by sx.account_id, sx.security_id, rx.account_id
)
select tx.account_id, tx.security_id
     , sum(adjust_shares(tx.security_id, tx.date, td.asset_quantity)) shares
     , sum(case when td.asset_quantity > 0 and td.related_detail_id is null then abs(td.amount) else 0 end)
        - (select coalesce(sum(cost_basis), 0) from shares_out where account_id = tx.account_id and security_id = tx.security_id)
        + (select coalesce(sum(cost_basis), 0) from shares_out where xfer_account_id = tx.account_id and security_id = tx.security_id) cost_basis
     , sum(case when td.asset_quantity is null and td.related_detail_id is null then greatest(td.amount, 0) else 0 end) dividends
     , min(tx.date) first_acquired
     , count(distinct tx.id) use_count
from tx
join tx_detail td on tx.id = td.tx_id
where tx.security_id is not null
group by tx.account_id, tx.security_id//

delimiter ;