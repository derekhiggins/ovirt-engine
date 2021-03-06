

----------------------------------------------------------------
-- [all_disks] View
--






Create or replace FUNCTION GetAllFromDisks() RETURNS SETOF all_disks
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT *
    FROM   all_disks;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetDiskByDiskId(v_disk_id UUID)
RETURNS SETOF all_disks
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT *
    FROM   all_disks
    WHERE  image_group_id = v_disk_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION GetDisksVmGuid(v_vm_guid UUID, v_user_id UUID, v_is_filtered BOOLEAN)
RETURNS SETOF all_disks
   AS $procedure$
BEGIN
      RETURN QUERY SELECT *
      FROM all_disks
      WHERE
      vm_guid = v_vm_guid
      AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                        FROM   user_disk_permissions_view
                                        WHERE  user_id = v_user_id AND entity_id = all_disks.disk_id));

END; $procedure$
LANGUAGE plpgsql;

-- Returns all the attachable disks in the storage pool
-- If storage pool is ommited, all the attachable disks are retrurned.
-- in case vm id is provided, returning all the disks in SP that are not attached to the vm
Create or replace FUNCTION GetAllAttachableDisksByPoolId(v_storage_pool_id UUID, v_vm_id uuid,  v_user_id UUID, v_is_filtered BOOLEAN)
RETURNS SETOF all_disks
   AS $procedure$
BEGIN
    RETURN QUERY SELECT all_disks.*
    FROM all_disks
    WHERE (v_storage_pool_id IS NULL OR all_disks.storage_pool_id = v_storage_pool_id)
    AND (all_disks.vm_guid IS NULL OR all_disks.shareable)
    AND (v_vm_id IS NULL OR all_disks.vm_guid IS NULL OR v_vm_id != all_disks.vm_guid)
    AND   (NOT v_is_filtered OR EXISTS (SELECT 1
                                        FROM   user_disk_permissions_view
                                        WHERE  user_id = v_user_id AND entity_id = disk_id));
END; $procedure$
LANGUAGE plpgsql;



