-- move date format from import_field to import_file

alter table import_file
	add column date_format varchar(50) not null;

update import_file set date_format = (
	select date_format
    from import_field
    where import_file_id = id and date_format is not null);

alter table import_field drop column date_format;