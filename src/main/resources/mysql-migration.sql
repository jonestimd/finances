-- move date format from import_field to import_file

alter table import_file
    add column date_format varchar(50) not null;

update import_file set date_format = (
    select date_format
    from import_field
    where import_file_id = id and date_format is not null);

alter table import_field drop column date_format;

-- add category and transfer account to import_field

alter table import_field
    add column tx_category_id bigint references transaction_category(id),
    add column transfer_account_id bigint references account(id);

update import_field f, import_field_label l, import_transfer_account a
set f.transfer_account_id = a.account_id
where f.import_file_id = a.import_file_id
  and f.id = l.import_field_id
  and l.label = a.account_alias;

delete a
from import_transfer_account a
join import_field f on a.import_file_id = f.import_file_id
join import_field_label l on f.id = l.import_field_id and l.label = a.account_alias
where f.transfer_account_id = a.account_id;

update import_field f, import_field_label l, import_category c
set f.tx_category_id = c.tx_category_id
where f.import_file_id = c.import_file_id
  and f.id = l.import_field_id
  and l.label = c.type_alias;

delete c
from import_category c
join import_field f on c.import_file_id = f.import_file_id
join import_field_label l on f.id = l.import_field_id and l.label = c.type_alias
where f.tx_category_id = c.tx_category_id;

-- add name to import_page_region

alter table import_page_region add column name varchar(250);
update import_page_region set name = id;
alter table import_page_region modify column name varchar(250) not null;
alter table import_page_region add unique index import_page_region_ak(import_file_id, name);
