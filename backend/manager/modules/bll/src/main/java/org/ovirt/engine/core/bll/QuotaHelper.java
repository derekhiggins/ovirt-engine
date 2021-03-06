package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.PermissionsOperationsParametes;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.QuotaDAO;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.StoragePoolDAO;
import org.ovirt.engine.core.dao.VdsGroupDAO;
import org.ovirt.engine.core.utils.Pair;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class QuotaHelper {
    private static final String DEFAULT_QUOTA_NAME_PERFIX = "DefaultQuota-";
    public static final Long UNLIMITED = -1L;
    public static final Long EMPTY = 0L;
    private static final Log log = LogFactory.getLog(QuotaHelper.class);

    private QuotaHelper() {
    }

    private static final QuotaHelper quotaHelper = new QuotaHelper();

    public static QuotaHelper getInstance() {
        return quotaHelper;
    }

    /**
     * Returns default quota id if the <code>Data Center</code> is disabled, <BR/>
     * or the quota id that was send.
     * @param quotaId
     * @param storagePoolId
     * @return
     */
    public Guid getQuotaIdToConsume(Guid quotaId, storage_pool storagePool) {
        Guid returnedQuotaGuid = quotaId;
        if (storagePool == null) {
            log.errorFormat("Storage pool is null, Quota id will be set from the parameter");
        } else if (storagePool.getQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED) {
            // If storage pool has disabled quota enforcement, then initialize default quota.
            log.debugFormat("Storage pool quota is disabled, Quota id which will be consume from is the default DC quota");
            returnedQuotaGuid =
                    getQuotaDAO()
                            .getDefaultQuotaByStoragePoolId(storagePool.getId())
                            .getId();
        }
        return returnedQuotaGuid;
    }

    public Collection<PermissionSubject> getPermissionsForDiskImagesList(Collection<DiskImage> diskImages,
            storage_pool storagePool) {
        List<PermissionSubject> permissionSubjectList = new ArrayList<PermissionSubject>();
        Map<Guid, Object> quotaMap = new HashMap<Guid, Object>();

        // Distinct the quotas for images.
        for (DiskImage diskImage : diskImages) {
            if (quotaMap.containsKey(diskImage.getQuotaId())) {
                quotaMap.put(diskImage.getQuotaId(), diskImage.getQuotaId());
                permissionSubjectList =
                        addQuotaPermissionSubject(permissionSubjectList, storagePool, diskImage.getQuotaId());
            }
        }
        return permissionSubjectList;
    }

    public void setDefaultQuotaAsRegularQuota(storage_pool storagePool) {
        Quota quota = getQuotaDAO().getDefaultQuotaByStoragePoolId(storagePool.getId());
        if (quota != null) {
            quota.setIsDefaultQuota(false);
            getQuotaDAO().update(quota);
        }
    }

    public List<PermissionSubject> addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList,
            storage_pool storagePool,
            Guid quotaId) {
        if (storagePool != null && storagePool.getQuotaEnforcementType() != QuotaEnforcementTypeEnum.DISABLED) {
            log.debug("Adding validation for consume quota to permission subjects list");
            quotaPermissionList.add(new PermissionSubject(quotaId, VdcObjectType.Quota, ActionGroup.CONSUME_QUOTA));
        }
        return quotaPermissionList;
    }

    /**
     * Returns unlimited Quota for storage pool.
     *
     * @param storagePool
     *            - The storage pool to create the unlimited Quota for.
     * @param isDefaultQuota
     *            - If the generated quota should be the default one or not
     * @return The quota
     */
    public Quota getUnlimitedQuota(storage_pool storagePool, boolean isDefaultQuota) {
        return getUnlimitedQuota(storagePool, isDefaultQuota, false).getFirst();
    }

    /**
     * Returns unlimited Quota for storage pool.
     *
     * The return value is a {@link Pair} of the unlimited quota for the storage pool and a {@link Boolean}
     * indicating if an existing quota was really reused or not.
     * <b>Notes:</b>
     * 1. If {@link #allowReuseExisting} is <code>false</code> the return value will always be
     * a {@link Pair} of a new quota object and <code>false</code>.
     * 2. If the {@link Quota} object could not be created for any reason (e.g., the given
     * {@link #storage_pool} was <code>null</code>), a {@link Pair} of <code>null</code> and <code>false</code>
     * is returned.
     *
     * @param storagePool
     *            - The storage pool to create the unlimited Quota for.
     * @param isDefaultQuota
     *            - If the generated quota should be the default one or not
     * @param allowReuseExisting
     *            - Whether to use an exiting quota unlimited quota or not
     * @return A {@link Pair} of a {@link Quota} object and an indication if it was reused or not.
     */
    @SuppressWarnings("null")
    public Pair<Quota, Boolean> getUnlimitedQuota(storage_pool storagePool,
            boolean isDefaultQuota,
            boolean allowReuseExisting) {
        if (storagePool == null || storagePool.getId() == null) {
            log.error("Unlimited Quota cannot be created or reused, Storage pool is not valid ");
            return new Pair<Quota, Boolean>(null, false);
        }

        Quota quota = null;
        boolean isExistingQuotaReused = false;
        if (allowReuseExisting) {
            quota = getQuotaDAO().getDefaultQuotaByStoragePoolId(storagePool.getId());
            isExistingQuotaReused = (quota != null);
        }

        if (!isExistingQuotaReused) {
            quota = generateUnlimitedQuota(storagePool);
        }

        quota.setIsDefaultQuota(isDefaultQuota);

        return new Pair<Quota, Boolean>(quota, isExistingQuotaReused);
    }

    /**
     * Generates a new default quota
     * @param storagePool
     *            - The storage pool to create the unlimited Quota for.
     * @return The generated quota
     */
    private Quota generateUnlimitedQuota(storage_pool storagePool) {
        // Set new Quota definition.
        Quota quota = new Quota();
        Guid quotaId = Guid.NewGuid();
        quota.setId(quotaId);
        quota.setStoragePoolId(storagePool.getId());
        quota.setQuotaName(generateDefaultQuotaName(storagePool));
        quota.setDescription("Automatic generated Quota for Data Center " + storagePool.getname());
        quota.setThresholdVdsGroupPercentage(getQuotaThresholdVdsGroup());
        quota.setThresholdStoragePercentage(getQuotaThresholdStorage());
        quota.setGraceVdsGroupPercentage(getQuotaGraceVdsGroup());
        quota.setGraceStoragePercentage(getQuotaGraceStorage());
        quota.setQuotaVdsGroups(new ArrayList<QuotaVdsGroup>());
        quota.setQuotaStorages(new ArrayList<QuotaStorage>());

        // Set Quota storage capacity definition.
        QuotaStorage quotaStorage = new QuotaStorage();
        quotaStorage.setStorageSizeGB(UNLIMITED);
        quota.setGlobalQuotaStorage(quotaStorage);

        // Set Quota cluster virtual memory definition and virtual CPU definition.
        QuotaVdsGroup quotaVdsGroup = new QuotaVdsGroup();
        quotaVdsGroup.setVirtualCpu(UNLIMITED.intValue());
        quotaVdsGroup.setMemSizeMB(UNLIMITED);
        quota.setGlobalQuotaVdsGroup(quotaVdsGroup);

        return quota;
    }

    /**
     * generate a new name for default quota that not exists in the system
     * @param storagePool
     * @return new unused default quota name
     */
    public String generateDefaultQuotaName(storage_pool storagePool) {
        String quotaName = getDefaultQuotaName(storagePool.getname());
        return getQuotaDAO().getDefaultQuotaName(quotaName);
    }

    public String getDefaultQuotaName(storage_pool storagePool) {
        return getDefaultQuotaName(storagePool.getname());
    }

    public String getDefaultQuotaName(String storagePoolName) {
        return DEFAULT_QUOTA_NAME_PERFIX + storagePoolName;
    }

    public boolean checkQuotaValidationForAdd(Quota quota, List<String> messages) {
        // All common checks
        if (!checkQuotaValidationCommon(quota, messages)) {
            return false;
        }

        // Check quota added is not default quota.
        if (quota.getIsDefaultQuota()) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_CAN_NOT_HAVE_DEFAULT_INDICATION.toString());
            return false;
        }

        return true;
    }

    public boolean checkQuotaValidationForEdit(Quota quota, List<String> messages) {
        // All common checks
        if (!checkQuotaValidationCommon(quota, messages)) {
            return false;
        }

        // Check editing the default quota is not allowed for a disabled DC
        // Note that the check is made vs. the existing quota in the database,
        // in order to prevent making the quota not default if the DC has quota disabled
        Quota oldQuota = getQuotaDAO().getById(quota.getId());
        if (oldQuota != null && oldQuota.getIsDefaultQuota()
                && oldQuota.getQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_CAN_NOT_HAVE_DEFAULT_INDICATION.toString());
            return false;
        }

        return true;
    }

    private boolean checkQuotaValidationCommon(Quota quota, List<String> messages) {
        if (quota == null) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_IS_NOT_VALID.toString());
            return false;
        }

        // Check if quota name's prefix isn't reserved.
        // In edit - Once a quota is edited, it stops being the default quota, so the default prefix is not allowed
        // In add - the default quota is generated by the AddEmptyStoragePoolCommand, and does not pass in this flow.
        // If a user manually tries to add a quota with this reserved name, it should not be allowed.
        if (!checkQuotaNamePrefixReserved(quota, messages)) {
            return false;
        }

        // Check if quota name exists.
        if (!checkQuotaNameExisting(quota, messages)) {
            return false;
        }

        // If specific Quota for storage is specified
        if (!validateQuotaStorageLimitation(quota, messages)) {
            return false;
        }

        // If specific Quota for VDS Group is specific
        if (!validateQuotaVdsGroupLimitation(quota, messages)) {
            return false;
        }

        return true;
    }

    /**
     * Save new <code>Quota</code> with permissions for ad_element_id to consume from.
     *
     * @param quota
     *            - The quota to be saved
     * @param ad_element_id
     *            - The user which will have consume permissions on the quota.
     * @param reuse
     *            - whether to update an existing quota or create a new one
     */
    public void saveQuotaForUser(Quota quota, Guid ad_element_id) {
        saveOrUpdateQuotaForUser(quota, ad_element_id, false);
    }

    /**
     * Save <code>Quota</code> with permissions for ad_element_id to consume from.
     *
     * @param quota
     *            - The quota to be saved
     * @param ad_element_id
     *            - The user which will have consume permissions on the quota.
     * @param reuse
     *            - whether to update an existing quota or create a new one
     */
    public void saveOrUpdateQuotaForUser(Quota quota, Guid ad_element_id, boolean reuse) {
        if (reuse) {
            getQuotaDAO().update(quota);
        } else {
            getQuotaDAO().save(quota);
        }
        permissions perm =
                new permissions(ad_element_id,
                        PredefinedRoles.QUOTA_CONSUMER.getId(),
                        quota.getId(),
                        VdcObjectType.Quota);
        PermissionsOperationsParametes permParams = new PermissionsOperationsParametes(perm);
        Backend.getInstance().runInternalAction(VdcActionType.AddPermission,
                permParams);
    }

    /**
     * Helper method which get as an input disk image list for VM or template and returns a list of quotas and their
     * desired limitation to be used.<BR/>
     *
     * @param diskImages
     *            - The disk image list to be grouped by
     * @param NumberOfVms
     *            - Number of VMs when creating the pool.
     * @param blockSparseInitSizeInGB
     *            - The initial size of sparse block size.
     * @return List of summarized requested size for quota.
     */
    public Map<Pair<Guid, Guid>, Double> getQuotaConsumeMapForVmPool(Collection<DiskImage> diskImages,
            Integer NumberOfVms,
            Integer blockSparseInitSizeInGB) {
        Map<Pair<Guid, Guid>, Double> quotaForStorageConsumption = new HashMap<Pair<Guid, Guid>, Double>();
        for (DiskImage disk : diskImages) {
            Pair<Guid, Guid> quotaForStorageKey = new Pair<Guid, Guid>(disk.getQuotaId(), disk.getstorage_ids().get(0));
            Long sizeRequested = disk.getsize() * NumberOfVms * blockSparseInitSizeInGB;
            Double storageRequest = quotaForStorageConsumption.get(quotaForStorageKey);
            if (storageRequest != null) {
                storageRequest += sizeRequested;
            } else {
                storageRequest = new Double(sizeRequested);
            }
            quotaForStorageConsumption.put(quotaForStorageKey, storageRequest);
        }
        return quotaForStorageConsumption;
    }

    /**
     * Helper method which get as an input disk image list for VM or template and returns a list of quotas and their
     * desired limitation to be used.<BR/>
     *
     * @param diskImages
     *            - The disk image list to be grouped by
     * @return List of summarized requested size for quota.
     */
    public Map<Pair<Guid, Guid>, Double> getQuotaConsumeMap(Collection<DiskImage> diskImages) {
        Map<Pair<Guid, Guid>, Double> quotaForStorageConsumption = new HashMap<Pair<Guid, Guid>, Double>();
        for (DiskImage disk : diskImages) {
            Pair<Guid, Guid> quotaForStorageKey =
                    new Pair<Guid, Guid>(disk.getQuotaId(), disk.getstorage_ids().get(0).getValue());
            Double storageRequest = quotaForStorageConsumption.get(quotaForStorageKey);
            if (storageRequest != null) {
                storageRequest += disk.getsize();
            } else {
                storageRequest = new Double(disk.getsize());
            }
            quotaForStorageConsumption.put(quotaForStorageKey, storageRequest);
        }

        return quotaForStorageConsumption;
    }

    public boolean checkQuotaNameExisting(Quota quota, List<String> messages) {
        Quota quotaByName = getQuotaDAO().getQuotaByQuotaName(quota.getQuotaName());

        // Check if there is no quota with the same name that already exists.
        if ((quotaByName != null) && (!quotaByName.getId().equals(quota.getId()))) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_NAME_ALREADY_EXISTS.toString());
            return false;
        }
        return true;
    }

    /**
     * Checks the the quota's name does not begin in {@link #DEFAULT_QUOTA_NAME_PERFIX}, which is reserved for default quotas.
     * @param quota The quota to check.
     * @param messages A {@link List} of messages that the relevant error message will be appended to in needed.
     * @return <code>true</code> if the quota's name is OK, <code>false</code> otherwise.
     */
    public boolean checkQuotaNamePrefixReserved(Quota quota, List<String> messages) {
        if (quota.getQuotaName().startsWith(DEFAULT_QUOTA_NAME_PERFIX)) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_NAME_RESERVED_FOR_DEFAULT.toString());
            return false;
        }

        return true;
    }

    /**
     * Validate Quota storage restrictions.
     *
     * @param quota
     * @param messages
     * @return
     */
    private static boolean validateQuotaStorageLimitation(Quota quota, List<String> messages) {
        boolean isValid = true;
        List<QuotaStorage> quotaStorageList = quota.getQuotaStorages();
        if (quota.isGlobalStorageQuota() && (quotaStorageList != null && !quotaStorageList.isEmpty())) {
            messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_LIMIT_IS_SPECIFIC_AND_GENERAL.toString());
            isValid = false;
        }
        return isValid;
    }

    /**
     * Validate Quota vds group restrictions.
     *
     * @param quota
     *            - Quota we validate
     * @param messages
     *            - Messages of can do action.
     * @return Boolean value if the quota is valid or not.
     */
    private static boolean validateQuotaVdsGroupLimitation(Quota quota, List<String> messages) {
        boolean isValid = true;
        List<QuotaVdsGroup> quotaVdsGroupList = quota.getQuotaVdsGroups();
        if (quotaVdsGroupList != null && !quotaVdsGroupList.isEmpty()) {
            boolean isSpecificVirtualCpu = false;
            boolean isSpecificVirtualRam = false;

            for (QuotaVdsGroup quotaVdsGroup : quotaVdsGroupList) {
                if (quotaVdsGroup.getVirtualCpu() != null) {
                    isSpecificVirtualCpu = true;
                }
                if (quotaVdsGroup.getMemSizeMB() != null) {
                    isSpecificVirtualRam = true;
                }
            }

            // if the global vds group limit was not specified, then specific limitation must be specified.
            if (quota.isGlobalVdsGroupQuota() && (isSpecificVirtualRam || isSpecificVirtualCpu)) {
                messages.add(VdcBllMessages.ACTION_TYPE_FAILED_QUOTA_LIMIT_IS_SPECIFIC_AND_GENERAL.toString());
                isValid = false;
            }
        }
        return isValid;
    }

    /**
     * @return The VdsGroupDAO
     */
    protected VdsGroupDAO getVdsGroupDao() {
        return DbFacade.getInstance().getVdsGroupDAO();
    }

    /**
     * @return The StorageDomainDAO
     */
    protected StorageDomainDAO getStorageDomainDao() {
        return DbFacade.getInstance().getStorageDomainDAO();
    }

    /**
     * @return The quotaDAO
     */
    protected QuotaDAO getQuotaDAO() {
        return DbFacade.getInstance().getQuotaDAO();
    }

    /**
     * @return The StoragePoolDAO
     */
    protected StoragePoolDAO getStoragePoolDao() {
        return DbFacade.getInstance().getStoragePoolDAO();
    }

    /** @return The VDS Group's quota threshold */
    protected int getQuotaThresholdVdsGroup() {
        return getIntegerConfig(ConfigValues.QuotaThresholdVdsGroup);
    }

    /** @return The Storage's quota threshold */
    protected int getQuotaThresholdStorage() {
        return getIntegerConfig(ConfigValues.QuotaThresholdStorage);
    }

    /** @return The VDS Group's quota grace */
    protected int getQuotaGraceVdsGroup() {
        return getIntegerConfig(ConfigValues.QuotaGraceVdsGroup);
    }

    /** @return The Storage's quota grace */
    protected int getQuotaGraceStorage() {
        return getIntegerConfig(ConfigValues.QuotaGraceStorage);
    }

    /**
     * @param value The required configuration value
     * @return The appropriate int-value from the configuration
     */
    private static int getIntegerConfig(ConfigValues value) {
        return Config.<Integer> GetValue(value);
    }
}
