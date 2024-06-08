-- shares in
alter table tx_detail
    add column date_acquired date;

-- lot actions:  sell, shares out, transfer
alter table security_lot
    rename column sale_tx_detail_id to related_tx_detail_id,
    rename column sale_shares to adjusted_shares;