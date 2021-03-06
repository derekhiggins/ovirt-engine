--------------------------------------------------
-- DB helper functions
--------------------------------------------------

-- Creates a column in the given table (if not exists)
Create or replace FUNCTION fn_db_add_column(v_table varchar(128), v_column varchar(128), v_column_def text)
returns void
AS $procedure$
declare
v_sql text;

begin
	if (not exists (select 1 from information_schema.columns where table_name ilike v_table and column_name ilike v_column)) then
	    begin
		v_sql := 'ALTER TABLE ' || v_table || ' ADD COLUMN ' || v_column || ' ' || v_column_def;
		EXECUTE v_sql;
            end;
	end if;
END; $procedure$
LANGUAGE plpgsql;

-- delete a column from a table and all its dependencied
Create or replace FUNCTION fn_db_drop_column(v_table varchar(128), v_column varchar(128))
returns void
AS $procedure$
declare
v_sql text;
begin
        if (exists (select 1 from information_schema.columns where table_name ilike v_table and column_name ilike v_column)) then
            begin
                v_sql := 'ALTER TABLE ' || v_table || ' DROP COLUMN ' || v_column;
                EXECUTE v_sql;
            end;
        end if;
end;$procedure$
LANGUAGE plpgsql;

-- Changes a column data type (if value conversion is supported)
Create or replace FUNCTION fn_db_change_column_type(v_table varchar(128), v_column varchar(128),
                                                    v_type varchar(128), v_new_type varchar(128))
returns void
AS $procedure$
declare
v_sql text;

begin
	if (exists (select 1 from information_schema.columns where table_name ilike v_table and column_name ilike v_column and (udt_name ilike v_type or data_type ilike v_type))) then
	    begin
		v_sql := 'ALTER TABLE ' || v_table || ' ALTER COLUMN ' || v_column || ' TYPE ' || v_new_type;
		EXECUTE v_sql;
            end;
	end if;
END; $procedure$
LANGUAGE plpgsql;

-- rename a column for a given table
Create or replace FUNCTION fn_db_rename_column(v_table varchar(128), v_column varchar(128), v_new_name varchar(128))
returns void
AS $procedure$
declare
v_sql text;

begin
	if (exists (select 1 from information_schema.columns where table_name ilike v_table and column_name ilike v_column)) then
	    begin
		v_sql := 'ALTER TABLE ' || v_table || ' RENAME COLUMN ' || v_column || ' TO ' || v_new_name;
		EXECUTE v_sql;
            end;
	end if;
END; $procedure$
LANGUAGE plpgsql;



-- Adds a value to vdc_options (if not exists)
create or replace FUNCTION fn_db_add_config_value(v_option_name varchar(100), v_option_value varchar(4000),
                                                  v_version varchar(40))
returns void
AS $procedure$
begin
    if (not exists (select 1 from vdc_options where option_name ilike v_option_name and version = v_version)) then
        begin
            insert into vdc_options (option_name, option_value, version) values (v_option_name, v_option_value, v_version);
        end;
    end if;
END; $procedure$
LANGUAGE plpgsql;

-- Deletes a key from vdc_options (if exists)
create or replace FUNCTION fn_db_delete_config_value(v_option_name varchar(100), v_version varchar(40))
returns void
AS $procedure$
begin
    if (exists (select 1 from vdc_options where option_name ilike v_option_name and version = v_version)) then
        begin
            delete from vdc_options where option_name ilike v_option_name and version = v_version;
        end;
    end if;
END; $procedure$
LANGUAGE plpgsql;


-- Updates a value in vdc_options (if exists)
create or replace FUNCTION fn_db_update_config_value(v_option_name varchar(100), v_option_value varchar(4000),
                                                  v_version varchar(40))
returns void
AS $procedure$
begin
    if (exists (select 1 from vdc_options where option_name ilike v_option_name and version = v_version)) then
        begin
            update  vdc_options set option_value = v_option_value
            where option_name ilike v_option_name and version = v_version;
        end;
    end if;
END; $procedure$
LANGUAGE plpgsql;

-- Updates a value in vdc_options (if exists) if default value wasn't changed
create or replace FUNCTION fn_db_update_default_config_value(v_option_name varchar(100),v_default_option_value varchar(4000),v_option_value varchar(4000),v_version varchar(40),v_ignore_default_value_case boolean)
returns void
AS $procedure$
begin
    if (exists (select 1 from vdc_options where option_name ilike v_option_name and version = v_version)) then
        begin
            if (v_ignore_default_value_case)
            then
               update  vdc_options set option_value = v_option_value
               where option_name ilike v_option_name and option_value ilike v_default_option_value and version = v_version;
            else
               update  vdc_options set option_value = v_option_value
               where option_name ilike v_option_name and option_value = v_default_option_value and version = v_version;
            end if;
        end;
    end if;
END; $procedure$
LANGUAGE plpgsql;

--------------------------------------------------
-- End of DB helper functions
--------------------------------------------------

CREATE OR REPLACE FUNCTION isloggingenabled(errorcode text)
  RETURNS boolean AS
$BODY$
-- determine if logging errors is enabled.
-- define in your postgresql.conf:
-- custom_variable_classes = 'engine'
-- set engine.logging = 'true';
-- or this could be in a config table

-- NOTE: We should look at checking error codes as not all are suitable for exceptions some are just notice or info
declare 
    result boolean := false;
    prop text;
begin
    -- check for log setting in postgresql.conf
    select current_setting('engine.logging') into prop;
    if prop = 'true' then
result = true;
    end if;
    return result;
exception
    when others then
result = true;  -- default to log if not specified 
return result;
end;
$BODY$
  LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION attach_user_to_su_role()
  RETURNS void AS
$procedure$
   DECLARE
   v_user_entry VARCHAR(255);
   v_user_id  UUID;
   v_name  VARCHAR(255);
   v_domain  VARCHAR(255);
   v_user_name  VARCHAR(255);

   v_document  VARCHAR(64);
   v_index  INTEGER;
BEGIN

   select   option_value INTO v_user_entry from vdc_options where option_name = 'AdUserId';
   select   option_value INTO v_name from vdc_options where option_name = 'AdUserName';
   select   option_value INTO v_domain from vdc_options where option_name = 'DomainName';

   v_index := POSITION(':' IN v_user_entry);
   if ( v_index <> 0 ) then
      v_user_entry := substring( v_user_entry from v_index + 1 );
      v_user_id := CAST( v_user_entry AS uuid );
   end if;

   v_index := POSITION(':' IN v_name);
   if ( v_index <> 0 ) then
      v_name := substring( v_name from v_index + 1 );
   end if;

-- find if name already includes domain (@)
   v_index := POSITION('@' IN v_name);

   if (v_index = 0) then
      v_user_name := coalesce(v_name,'') || '@' || coalesce(v_domain,'');
   else
      v_user_name := v_name;
   end if;


insert into users(user_id,name,domain,username,groups,status) select v_user_id, v_name, v_domain, v_user_name,'',1 where not exists (select user_id,name,domain,username,groups,status from users where user_id = v_user_id and name = v_name and domain = v_domain and username = v_user_name and groups = '' and status = 1);

insert into permissions(id,role_id,ad_element_id,object_id,object_type_id) select uuid_generate_v1(), '00000000-0000-0000-0000-000000000001', v_user_id, getGlobalIds('system'), 1 where not exists(select role_id,ad_element_id,object_id,object_type_id from permissions where role_id = '00000000-0000-0000-0000-000000000001' and ad_element_id = v_user_id and object_id= getGlobalIds('system') and object_type_id = 1);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION CheckDBConnection() RETURNS SETOF integer
   AS $procedure$
BEGIN
    RETURN QUERY SELECT 1;
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION generate_drop_all_functions_syntax() RETURNS SETOF text
   AS $procedure$
BEGIN
RETURN QUERY select 'drop function if exists ' || ns.nspname || '.' || proname || '(' || oidvectortypes(proargtypes) || ') cascade;' from pg_proc inner join pg_namespace ns on (pg_proc.pronamespace=ns.oid) where ns.nspname = 'public' and proname not ilike 'uuid%' order by proname;
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION generate_drop_all_views_syntax() RETURNS SETOF text
   AS $procedure$
BEGIN
RETURN QUERY select 'DROP VIEW if exists ' || table_name || ' CASCADE;' from information_schema.views where table_schema = 'public' order by table_name;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION fn_get_column_size( v_table varchar(64), v_column varchar(64)) returns integer
   AS $procedure$
   declare
   retvalue  integer;
BEGIN
   retvalue := character_maximum_length from information_schema.columns
    where
    table_name ilike v_table and column_name ilike v_column and
    table_schema = 'public' and udt_name in ('char','varchar');
   return retvalue;
END; $procedure$
LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION attach_user_to_su_role(v_user_id VARCHAR(255), v_name VARCHAR(255), v_domain VARCHAR(255))
  RETURNS void AS
$BODY$
   DECLARE
   v_user_name VARCHAR(255);
   v_document  VARCHAR(64);
   v_index  INTEGER;
   input_uuid uuid;
BEGIN
   input_uuid = CAST( v_user_id AS uuid );
-- find if name already includes domain (@)
   v_index := POSITION('@' IN v_name);

   if (v_index = 0) then
      v_user_name := coalesce(v_name,'') || '@' || coalesce(v_domain,'');
   else
      v_user_name := v_name;
   end if;


insert into users(user_id,name,domain,username,groups,status) select input_uuid, v_name, v_domain, v_user_name,'',1 where not exists (select user_id,name,domain,username,groups,status from users where user_id = input_uuid);

insert into permissions(id,role_id,ad_element_id,object_id,object_type_id) select uuid_generate_v1(), '00000000-0000-0000-0000-000000000001', input_uuid, getGlobalIds('system'), 1 where not exists(select role_id,ad_element_id,object_id,object_type_id from permissions where role_id = '00000000-0000-0000-0000-000000000001' and ad_element_id = input_uuid and object_id= getGlobalIds('system') and object_type_id = 1);
END; $BODY$

LANGUAGE plpgsql;

-- This function splits a config value: given a config value with one row for 'general', it creates new options
-- with the old value, for each version, except the newest version, which gets the input value
CREATE OR REPLACE FUNCTION fn_db_split_config_value(v_option_name character varying, v_old_option_value character varying, v_new_option_value character varying)
  RETURNS void AS
$BODY$
declare
v_old_value varchar(4000);
v_cur cursor for select distinct version from vdc_options where version <> 'general' order by version;
v_version varchar(40);
v_index integer;
v_count integer;
v_total_count integer;
begin
    v_total_count := count(version) from vdc_options where option_name = v_option_name;
    v_old_value := option_value from vdc_options where option_name = v_option_name and version = 'general';
    if (v_total_count <= 1) then
        begin
            if (v_old_value IS NULL) then
                v_old_value := v_old_option_value;
            end if;
            v_count := count(distinct version) from vdc_options where version <> 'general';
            v_index := 1;
        open v_cur;
        loop
            fetch v_cur into v_version;
            exit when not found;
            if (v_index = v_count) then
                insert into vdc_options (option_name, option_value, version) values (v_option_name, v_new_option_value, v_version);
            else
                insert into vdc_options (option_name, option_value, version) values (v_option_name, v_old_value, v_version);
            end if;
            v_index := v_index +1;
        end loop;
        close v_cur;
        delete from vdc_options where option_name = v_option_name and version = 'general';
        end;
    end if;
END; $BODY$
LANGUAGE plpgsql;

-- Function: fn_db_grant_action_group_to_all_roles(integer)
-- This function adds the input v_action_group_id to all the existing roles (both pre-defined and custom), besides the
-- input roles to filter.
CREATE OR REPLACE FUNCTION fn_db_grant_action_group_to_all_roles_filter(v_action_group_id integer, uuid[])
  RETURNS void AS
$BODY$
declare
v_role_id_to_filter alias for $2;
begin
    insert into roles_groups (role_id, action_group_id)
    select distinct role_id, v_action_group_id
    from roles_groups rg
    where not ARRAY [role_id] <@ v_role_id_to_filter and not exists (select 1 from roles_groups where role_id = rg.role_id and action_group_id = v_action_group_id);
END; $BODY$
LANGUAGE plpgsql;

-- The following function accepts a table or view object
-- Values of columns not matching the ones stored for this object in object_column_white_list table
-- will be masked with an empty value.
CREATE OR REPLACE FUNCTION fn_db_mask_object(v_object regclass) RETURNS setof record as
$BODY$
DECLARE
    v_sql TEXT;
    v_table record;
    v_table_name TEXT;
    temprec record;
BEGIN
    -- get full table/view name from v_object (i.e <namespace>.<name>)
    select c.relname, n.nspname INTO v_table
        FROM pg_class c join pg_namespace n on c.relnamespace = n.oid WHERE c.oid = v_object;
    -- try to get filtered query syntax from previous execution
    if exists (select 1 from object_column_white_list_sql where object_name = v_table.relname) then
	select sql into v_sql from object_column_white_list_sql where object_name = v_table.relname;
    else
        v_table_name := quote_ident( v_table.nspname ) || '.' || quote_ident( v_table.relname );
        -- compose sql statement while skipping values for columns not defined in object_column_white_list for this table.
        for temprec in select a.attname, t.typname
                       FROM pg_attribute a join pg_type t on a.atttypid = t.oid
                       WHERE a.attrelid = v_object AND a.attnum > 0 AND NOT a.attisdropped ORDER BY a.attnum
        loop
            v_sql := coalesce( v_sql || ', ', 'SELECT ' );
            if exists(select 1 from object_column_white_list
               where object_name = v_table.relname and column_name = temprec.attname) then
               v_sql := v_sql || quote_ident( temprec.attname );
            ELSE
               v_sql := v_sql || 'NULL::' || quote_ident( temprec.typname ) || ' as ' || quote_ident( temprec.attname );
            END IF;
        END LOOP;
        v_sql := v_sql || ' FROM ' || v_table_name;
        v_sql := 'SELECT x::' || v_table_name || ' as rec FROM (' || v_sql || ') as x';
        -- save generated query for further use
        insert into object_column_white_list_sql(object_name,sql) values (v_table.relname, v_sql);
    end if;
    RETURN QUERY EXECUTE v_sql;
END; $BODY$
LANGUAGE plpgsql;

-- Adds a table/view new added column to the white list
create or replace FUNCTION fn_db_add_column_to_object_white_list(v_object_name varchar(128), v_column_name varchar(128))
returns void
AS $procedure$
begin
    if (not exists (select 1 from object_column_white_list
                    where object_name = v_object_name and column_name = v_column_name)) then
        begin
            -- verify that there is such object in db
            if exists (select 1 from information_schema.columns
                       where table_name = v_object_name and column_name = v_column_name) then
                insert into object_column_white_list (object_name, column_name) values (v_object_name, v_column_name);
            end if;
        end;
    end if;
END; $procedure$
LANGUAGE plpgsql;
