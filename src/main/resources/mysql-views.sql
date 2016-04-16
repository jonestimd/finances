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

delimiter ;