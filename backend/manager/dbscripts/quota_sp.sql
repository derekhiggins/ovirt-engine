Create or replace FUNCTION InsertQuota(v_id UUID, v_storage_pool_id UUID, v_quota_name VARCHAR(50), v_description VARCHAR(500), v_threshold_vds_group_percentage INTEGER, v_threshold_storage_percentage INTEGER, v_grace_vds_group_percentage INTEGER, v_grace_storage_percentage INTEGER, v_is_default_quota BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO quota(id, storage_pool_id, quota_name, description, threshold_vds_group_percentage, threshold_storage_percentage, grace_vds_group_percentage, grace_storage_percentage, is_default_quota)
   VALUES(v_id,  v_storage_pool_id, v_quota_name,  v_description,  v_threshold_vds_group_percentage ,  v_threshold_storage_percentage,  v_grace_vds_group_percentage,  v_grace_storage_percentage, v_is_default_quota);
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION InsertQuotaLimitation(v_id UUID, v_quota_id UUID, v_storage_id UUID, v_vds_group_id UUID, v_virtual_cpu INTEGER, v_mem_size_mb BIGINT, v_storage_size_gb BIGINT)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO quota_limitation(id, quota_id, storage_id, vds_group_id, virtual_cpu, mem_size_mb, storage_size_gb)
   VALUES(v_id, v_quota_id, v_storage_id, v_vds_group_id, v_virtual_cpu, v_mem_size_mb, v_storage_size_gb);

END; $procedure$
LANGUAGE plpgsql;


-- Returns all the Quota storages in the storage pool if v_storage_id is null, if v_storage_id is not null then a specific quota storage will be returned.
-- Empty quotas are returned only if v_allow_empty is set to TRUE
Create or replace FUNCTION GetQuotaStorageByStorageGuid(v_storage_id UUID, v_id UUID, v_allow_empty BOOLEAN)
RETURNS SETOF quota_storage_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM
   (SELECT  COALESCE(q_storage_view.quota_storage_id, q_g_view.quota_id) as quota_storage_id,
            q_g_view.quota_id as quota_id,
            q_storage_view.storage_id,
            q_storage_view.storage_name,
            COALESCE(q_storage_view.storage_size_gb, q_g_view.storage_size_gb) as storage_size_gb,
            COALESCE(q_storage_view.storage_size_gb_usage, q_g_view.storage_size_gb_usage) as storage_size_gb_usage,
            q_g_view.is_default_quota
    FROM  quota_global_view q_g_view 
    LEFT OUTER JOIN quota_storage_view q_storage_view ON q_g_view.quota_id = q_storage_view.quota_id
    AND  (v_storage_id = q_storage_view.storage_id OR v_storage_id IS NULL)
   WHERE q_g_view.quota_id = v_id) sub
   WHERE (v_allow_empty OR storage_size_gb IS NOT NULL);
END; $procedure$
LANGUAGE plpgsql;


-- Returns all the global quotas in the storage pool if v_storage_pool_id is null, if v_storage_id is not null then a specific quota storage will be returned.
Create or replace FUNCTION GetQuotaByAdElementId(v_ad_element_id UUID, v_storage_pool_id UUID)
RETURNS SETOF quota_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT * FROM quota_view WHERE quota_view.quota_id IN
   (SELECT object_id
    FROM PERMISSIONS
    WHERE object_type_id = 17
    AND role_id in (SELECT role_id FROM ROLES_groups where action_group_id = 901)
    AND ad_element_id = v_ad_element_id)
    AND (v_storage_pool_id = quota_view.storage_pool_id or v_storage_pool_id IS NULL);
  END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetQuotaStorageByQuotaGuid(v_id UUID)
RETURNS SETOF quota_storage_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM  quota_storage_view
   WHERE quota_id = v_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetQuotaVdsGroupByVdsGroupGuid(v_vds_group_id UUID, v_id UUID, v_allow_empty BOOLEAN)
RETURNS SETOF quota_vds_group_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM (SELECT COALESCE(q_vds_view.quota_vds_group_id, q_g_view.quota_id) as quota_vds_group_id,
                q_g_view.quota_id as quota_id,
                q_vds_view.vds_group_id as vds_group_id,
                q_vds_view.vds_group_name as vds_group_name,
                COALESCE(q_vds_view.virtual_cpu,q_g_view.virtual_cpu) as virtual_cpu,
                COALESCE(q_vds_view.virtual_cpu_usage, q_g_view.virtual_cpu_usage) as virtual_cpu_usage,
                COALESCE(q_vds_view.mem_size_mb,q_g_view.mem_size_mb) as mem_size_mb,
                COALESCE(q_vds_view.mem_size_mb_usage, q_g_view.mem_size_mb_usage) as mem_size_mb_usage,
                q_g_view.is_default_quota
         FROM   quota_global_view q_g_view 
         LEFT OUTER JOIN quota_vds_group_view q_vds_view ON q_g_view.quota_id = q_vds_view.quota_id
         AND   (v_vds_group_id = q_vds_view.vds_group_id OR v_vds_group_id IS NULL)
         WHERE q_g_view.quota_id = v_id) sub
   WHERE v_allow_empty OR virtual_cpu IS NOT NULL OR mem_size_mb IS NOT NULL;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetQuotaVdsGroupByQuotaGuid(v_id UUID)
RETURNS SETOF quota_vds_group_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT quota_vds_group_view.*
   FROM  quota_vds_group_view
   WHERE quota_id = v_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION DeleteQuotaByQuotaGuid(v_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
   DELETE FROM quota
   WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION DeleteQuotaLimitationByQuotaGuid(v_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
   DELETE FROM quota_limitation
   WHERE quota_id = v_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION UpdateQuotaMetaData(v_id UUID, v_storage_pool_id UUID, v_quota_name VARCHAR(50), v_description VARCHAR(500), v_threshold_vds_group_percentage INTEGER, v_threshold_storage_percentage INTEGER, v_grace_vds_group_percentage INTEGER, v_grace_storage_percentage INTEGER, v_is_default_quota BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
UPDATE quota
   SET storage_pool_id = v_storage_pool_id,
   quota_name = v_quota_name,
   description = v_description,
   _update_date = LOCALTIMESTAMP,
   threshold_vds_group_percentage = v_threshold_vds_group_percentage,
   threshold_storage_percentage = v_threshold_storage_percentage,
   grace_vds_group_percentage = v_grace_vds_group_percentage,
   grace_storage_percentage = v_grace_storage_percentage,
   is_default_quota = v_is_default_quota
   WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;

-- Returns all the Quota storages in the storage pool if v_storage_id is null, if v_storage_id is not null then a specific quota storage will be returned.
Create or replace FUNCTION GetQuotaByStoragePoolGuid(v_storage_pool_id UUID)
RETURNS SETOF quota_global_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM  quota_global_view
   WHERE (storage_pool_id = v_storage_pool_id or v_storage_pool_id IS NULL);
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetQuotaByQuotaGuid(v_id UUID)
RETURNS SETOF quota_global_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM quota_global_view
   WHERE quota_id = v_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetQuotaByQuotaName(v_quota_name VARCHAR)
RETURNS SETOF quota_global_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM quota_global_view
   WHERE quota_name = v_quota_name;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetDefaultQuotaByStoragePoolId(v_storage_pool_id UUID)
RETURNS SETOF quota_global_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM quota_global_view
   WHERE storage_pool_id = v_storage_pool_id
     and is_default_quota = true;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetAllThinQuotasByStorageId(v_storage_id UUID)
RETURNS SETOF quota_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT DISTINCT
    quota_id,
    storage_pool_id,
    storage_pool_name,
    quota_name,
    description,
    threshold_vds_group_percentage,
    threshold_storage_percentage,
    grace_vds_group_percentage,
    grace_storage_percentage,
    quota_enforcement_type,
    is_default_quota
   FROM   quota_limitations_view
   WHERE  storage_id = v_storage_id
   OR     (is_global AND NOT is_empty AND storage_pool_id IN (SELECT storage_pool_id
                                                              FROM   storage_pool_iso_map
                                                              WHERE  storage_id = v_storage_id));
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetAllThinQuotasByVDSGroupId(v_vds_group_id UUID)
RETURNS SETOF quota_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT DISTINCT
    quota_id,
    storage_pool_id,
    storage_pool_name,
    quota_name,
    description,
    threshold_vds_group_percentage,
    threshold_storage_percentage,
    grace_vds_group_percentage,
    grace_storage_percentage,
    quota_enforcement_type,
    is_default_quota
   FROM   quota_limitations_view
   WHERE  vds_group_id = v_vds_group_id
   OR     (is_global AND NOT is_empty AND storage_pool_id IN (SELECT storage_pool_id
                                                              FROM   vds_groups
                                                              WHERE  vds_group_id = v_vds_group_id));
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION generate_quota_default_name(v_quota_name varchar)
RETURNS varchar
    AS $function$
    DECLARE
    v_name varchar;
BEGIN
    SELECT v_quota_name || coalesce (cast(max(cast (CASE WHEN length(suffix) > 0 THEN suffix ELSE '0' END AS integer)) + 1 AS varchar), '')
    INTO v_name
    from (
        SELECT replace(quota_name, v_quota_name,'') AS suffix
        FROM quota
        WHERE quota_name LIKE v_quota_name || '%'
    ) AS inner_quota_query;

return v_name;

END; $function$
LANGUAGE plpgsql;

