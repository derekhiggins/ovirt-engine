package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.command.utils.StorageDomainSpaceChecker;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.CreateSnapshotFromTemplateParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.queries.IsVmWithSameNameExistParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmDynamicDAO;
import org.ovirt.engine.core.dao.VmStaticDAO;
import org.ovirt.engine.core.utils.GuidUtils;
import org.ovirt.engine.core.utils.linq.All;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils.VMCustomProperties;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils.ValidationError;

@SuppressWarnings("serial")
@LockIdNameAttribute
public class AddVmCommand<T extends VmManagementParametersBase> extends VmManagementCommandBase<T> {

    protected HashMap<Guid, DiskImage> diskInfoDestinationMap;
    protected Map<Guid, storage_domains> destStorages = new HashMap<Guid, storage_domains>();
    protected Map<Guid, List<DiskImage>> storageToDisksMap;
    protected String newMac = "";

    /**
     * A list of the new disk images which were saved for the VM.
     */
    protected List<DiskImage> newDiskImages = new ArrayList<DiskImage>();

    public AddVmCommand(T parameters) {
        super(parameters);
        // if we came from EndAction the VmId is not null
        setVmId((parameters.getVmId().equals(Guid.Empty)) ? Guid.NewGuid() : parameters.getVmId());
        parameters.setVmId(getVmId());
        setStorageDomainId(getParameters().getStorageDomainId());
        if (parameters.getVmStaticData() != null) {
            setVmTemplateId(parameters.getVmStaticData().getvmt_guid());
            setQuotaId(getParameters().getVmStaticData().getQuotaId());
        }

        parameters.setEntityId(getVmId());
        initTemplateDisks();
        initStoragePoolId();
        diskInfoDestinationMap = getParameters().getDiskInfoDestinationMap();
        if (diskInfoDestinationMap == null) {
            diskInfoDestinationMap = new HashMap<Guid, DiskImage>();
        }
    }

    protected AddVmCommand(Guid commandId) {
        super(commandId);
    }

    protected void initStoragePoolId() {
        if (getVdsGroup() != null) {
            setStoragePoolId(getVdsGroup().getstorage_pool_id() != null ? getVdsGroup().getstorage_pool_id().getValue()
                    : Guid.Empty);
        }
    }

    protected void initTemplateDisks() {
        if (getVmTemplate() != null) {
            VmTemplateHandler.UpdateDisksFromDb(getVmTemplate());
        }
    }

    private Guid _vmSnapshotId = Guid.Empty;

    protected Guid getVmSnapshotId() {
        return _vmSnapshotId;
    }

    protected List<VmNetworkInterface> _vmInterfaces;

    protected List<VmNetworkInterface> getVmInterfaces() {
        if (_vmInterfaces == null) {
            List<VmNetworkInterface> vmNetworkInterfaces =
                    DbFacade.getInstance().getVmNetworkInterfaceDAO().getAllForTemplate(getVmTemplate().getId());
            _vmInterfaces =
                    (vmNetworkInterfaces != null) ? vmNetworkInterfaces
                            : new ArrayList<VmNetworkInterface>();
        }
        return _vmInterfaces;
    }

    protected List<? extends Disk> _vmDisks;

    protected List<? extends Disk> getVmDisks() {
        if (_vmDisks == null) {
            _vmDisks =
                    DbFacade.getInstance()
                            .getDiskDao()
                            .getAllForVm(getVmTemplateId());
        }

        return _vmDisks;
    }

    protected boolean CanAddVm(ArrayList<String> reasons, Collection<storage_domains> destStorages) {
        VmStatic vmStaticFromParams = getParameters().getVmStaticData();
        boolean returnValue = canAddVm(reasons, vmStaticFromParams.getvm_name(), getStoragePoolId()
                .getValue(), vmStaticFromParams.getpriority());

        if (returnValue) {
            List<ValidationError> validationErrors = validateCustomProperties(vmStaticFromParams);
            if (!validationErrors.isEmpty()) {
                VmHandler.handleCustomPropertiesError(validationErrors, reasons);
                returnValue = false;
            }
        }

        // check that template image and vm are on the same storage pool
        if (returnValue
                && shouldCheckSpaceInStorageDomains()) {
            if (!getStoragePoolId().equals(getStoragePoolIdFromSourceImageContainer())) {
                reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_MATCH.toString());
                returnValue = false;
            } else {
                for (storage_domains domain : destStorages) {
                    if (!StorageDomainSpaceChecker.isBelowThresholds(domain)) {
                        returnValue = false;
                        reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString());
                        break;
                    } else if (!StorageDomainSpaceChecker.hasSpaceForRequest(domain,
                            getNeededDiskSize(domain.getId()))) {
                        returnValue = false;
                        reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW.toString());
                        break;
                    }
                }
            }
        }
        if (returnValue) {
            returnValue = isDedicatedVdsOnSameCluster(vmStaticFromParams);
        }
        return returnValue;
    }

    protected boolean shouldCheckSpaceInStorageDomains() {
        return !getImagesToCheckDestinationStorageDomains().isEmpty()
                && !LinqUtils.firstOrNull(getImagesToCheckDestinationStorageDomains(), new All<DiskImage>())
                        .getImageId().equals(VmTemplateHandler.BlankVmTemplateId);
    }

    protected Guid getStoragePoolIdFromSourceImageContainer() {
        return getVmTemplate().getstorage_pool_id().getValue();
    }

    protected int getNeededDiskSize(Guid domainId) {
        return getBlockSparseInitSizeInGB() * storageToDisksMap.get(domainId).size();
    }

    protected boolean CanDoAddVmCommand() {
        boolean returnValue = false;
        returnValue = areParametersLegal(getReturnValue().getCanDoActionMessages());
        // Check if number of monitors passed is legal
        returnValue =
                returnValue
                        && checkNumberOfMonitors();

        returnValue =
                returnValue
                        && CheckPCIAndIDELimit(getParameters().getVmStaticData().getnum_of_monitors(),
                                getVmInterfaces(),
                                getVmDisks(), getReturnValue().getCanDoActionMessages())
                        && CanAddVm(getReturnValue().getCanDoActionMessages(), destStorages.values())
                        && hostToRunExist();
        return returnValue;
    }

    protected boolean checkNumberOfMonitors() {
        return VmHandler.isNumOfMonitorsLegal(getParameters().getVmStaticData().getdefault_display_type(),
                getParameters().getVmStaticData().getnum_of_monitors(),
                getReturnValue().getCanDoActionMessages());
    }

    protected boolean hostToRunExist() {
        if (getParameters().getVmStaticData().getdedicated_vm_for_vds() != null) {
            if (DbFacade.getInstance().getVdsDAO().get(getParameters().getVmStaticData().getdedicated_vm_for_vds()) == null) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_HOST_NOT_EXIST);
                return false;
            }
        }
        return true;
    }

    public static boolean CheckCpuSockets(int num_of_sockets, int cpu_per_socket, String compatibility_version,
            List<String> CanDoActionMessages) {
        boolean retValue = true;
        if (retValue
                && (num_of_sockets * cpu_per_socket) > Config.<Integer> GetValue(ConfigValues.MaxNumOfVmCpus,
                        compatibility_version)) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MAX_NUM_CPU.toString());
            retValue = false;
        }
        if (retValue
                && num_of_sockets > Config.<Integer> GetValue(ConfigValues.MaxNumOfVmSockets, compatibility_version)) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MAX_NUM_SOCKETS.toString());
            retValue = false;
        }
        if (retValue
                && cpu_per_socket > Config.<Integer> GetValue(ConfigValues.MaxNumOfCpuPerSocket, compatibility_version)) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MAX_CPU_PER_SOCKET.toString());
            retValue = false;
        }
        if (retValue && cpu_per_socket < 1) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MIN_CPU_PER_SOCKET.toString());
            retValue = false;
        }
        if (retValue && num_of_sockets < 1) {
            CanDoActionMessages.add(VdcBllMessages.ACTION_TYPE_FAILED_MIN_NUM_SOCKETS.toString());
            retValue = false;
        }
        return retValue;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM);
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;
        if (getVmTemplate() == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
            return false;
        }
        returnValue = buildAndCheckDestStorageDomains();
        if (returnValue) {
            storageToDisksMap =
                    ImagesHandler.buildStorageToDiskMap(getImagesToCheckDestinationStorageDomains(),
                            diskInfoDestinationMap);
            returnValue = CanDoAddVmCommand();
        }

        String vmName = getParameters().getVm().getvm_name();
        if (vmName == null || vmName.isEmpty()) {
            returnValue = false;
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NAME_MAY_NOT_BE_EMPTY);
        } else {
            // check that VM name is not too long
            boolean vmNameValidLength = isVmNameValidLength(getParameters().getVm());
            if (!vmNameValidLength) {
                returnValue = false;
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
            }
        }

        if (returnValue && Config.<Boolean> GetValue(ConfigValues.LimitNumberOfNetworkInterfaces,
                getVdsGroup().getcompatibility_version().toString())) {
            // check that we have no more then 8 interfaces (kvm limitation in version 2.x)
            if (!validateNumberOfNics(getVmInterfaces(), null)) {
                addCanDoActionMessage(VdcBllMessages.NETWORK_INTERFACE_EXITED_MAX_INTERFACES);
                returnValue = false;
            }
        }

        // check for Vm Payload
        if (returnValue && getParameters().getVmPayload() != null) {
            returnValue = checkPayload(getParameters().getVmPayload(),
                    getParameters().getVmStaticData().getiso_path());
            if (returnValue) {
                // we save the content in base64 string
                getParameters().getVmPayload().setContent(Base64.encodeBase64String(
                        getParameters().getVmPayload().getContent().getBytes()));
            }
        }

        // Check that the USB policy is legal
        if (!VmHandler.isUsbPolicyLegal(getParameters().getVm().getusb_policy(),
                getParameters().getVm().getos(),
                getVdsGroup(),
                getReturnValue().getCanDoActionMessages())) {
            returnValue = false;
        }

        // check cpuPinning
        if (returnValue && !isCpuPinningValid(getParameters().getVm().getCpuPinning())) {
            returnValue = false;
            addCanDoActionMessage(VdcBllMessages.VM_PINNING_FORMAT_INVALID);
        }
        return returnValue && checkCpuSockets();
    }

    protected boolean checkCpuSockets() {
        return AddVmCommand.CheckCpuSockets(getParameters().getVmStaticData().getnum_of_sockets(),
                getParameters().getVmStaticData().getcpu_per_socket(), getVdsGroup().getcompatibility_version()
                        .toString(), getReturnValue().getCanDoActionMessages());
    }

    protected boolean buildAndCheckDestStorageDomains() {
        boolean retValue = true;
        if (diskInfoDestinationMap.isEmpty()) {
            retValue = fillDestMap();
        } else {
            retValue = validateProvidedDestinations();
        }
        if (retValue && getImagesToCheckDestinationStorageDomains().size() != diskInfoDestinationMap.size()) {
            log.errorFormat("Can not found any default active domain for one of the disks of template with id : {0}",
                    getVmTemplate().getId());
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_MISSED_STORAGES_FOR_SOME_DISKS);
            retValue = false;
        }

        return retValue && validateIsImagesOnDomains();
    }

    protected Collection<DiskImage> getImagesToCheckDestinationStorageDomains() {
        return getVmTemplate().getDiskMap().values();
    }

    private boolean validateProvidedDestinations() {
        for (DiskImage diskImage : diskInfoDestinationMap.values()) {
            if (diskImage.getstorage_ids() == null || diskImage.getstorage_ids().isEmpty()) {
                diskImage.setstorage_ids(new ArrayList<Guid>());
                diskImage.getstorage_ids().add(getParameters().getStorageDomainId());
            }
            Guid storageDomainId = diskImage.getstorage_ids().get(0);
            if (destStorages.get(storageDomainId) == null) {
                storage_domains storage = DbFacade.getInstance().getStorageDomainDAO().getForStoragePool(
                        storageDomainId, getStoragePoolId());
                StorageDomainValidator validator =
                        new StorageDomainValidator(storage);
                if (!validator.isDomainExistAndActive(getReturnValue().getCanDoActionMessages())
                        || !validator.domainIsValidDestination(getReturnValue().getCanDoActionMessages())) {
                    return false;
                }
                destStorages.put(storage.getId(), storage);
            }
        }
        return true;
    }

    private boolean fillDestMap() {
        if (getParameters().getStorageDomainId() != null
                && !Guid.Empty.equals(getParameters().getStorageDomainId())) {
            Guid storageId = getParameters().getStorageDomainId();
            for (DiskImage image : getImagesToCheckDestinationStorageDomains()) {
                diskInfoDestinationMap.put(image.getId(), makeNewImage(storageId, image));
            }
            return validateProvidedDestinations();
        }
        fillImagesMapBasedOnTemplate();
        return true;
    }

    protected void fillImagesMapBasedOnTemplate() {
        ImagesHandler.fillImagesMapBasedOnTemplate(getVmTemplate(),
                getStorageDomainDAO().getAllForStoragePool(getVmTemplate().getstorage_pool_id().getValue()),
                diskInfoDestinationMap,
                destStorages, false);
    }

    protected boolean validateIsImagesOnDomains() {
        for (DiskImage image : getImagesToCheckDestinationStorageDomains()) {
            if (!image.getstorage_ids().containsAll(diskInfoDestinationMap.get(image.getId()).getstorage_ids())) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_DESTINATION_DOMAIN);
                return false;
            }
        }
        return true;
    }

    private DiskImage makeNewImage(Guid storageId, DiskImage image) {
        DiskImage newImage = new DiskImage();
        newImage.setImageId(image.getImageId());
        newImage.setvolume_format(image.getvolume_format());
        newImage.setvolume_type(image.getvolume_type());
        ArrayList<Guid> storageIds = new ArrayList<Guid>();
        storageIds.add(storageId);
        newImage.setstorage_ids(storageIds);
        newImage.setQuotaId(image.getQuotaId());
        return newImage;
    }

    @Override
    protected boolean validateQuota() {
        // Set default quota id if storage pool enforcement is disabled.
        setQuotaId(QuotaHelper.getInstance().getQuotaIdToConsume(getQuotaId(), getStoragePool()));

        // Set default quota id for each disk image in the source if storage pool enforcement is disabled.
        for (DiskImage diskImage : getImagesToCheckDestinationStorageDomains()) {
            diskImage.setQuotaId(QuotaHelper.getInstance()
                    .getQuotaIdToConsume(diskImage.getQuotaId(), getStoragePool()));
        }
        if (!isInternalExecution()) {
            return QuotaManager.validateMultiStorageQuota(getStoragePool().getQuotaEnforcementType(),
                    getQuotaConsumeMap(diskInfoDestinationMap.values()),
                    getCommandId(),
                    getReturnValue().getCanDoActionMessages());
        }
        return true;
    }

    protected boolean canAddVm(List<String> reasons, String name, Guid storagePoolId,
            int vmPriority) {
        boolean returnValue;
        // Checking if a desktop with same name already exists
        boolean exists = (Boolean) getBackend()
                .runInternalQuery(VdcQueryType.IsVmWithSameNameExist, new IsVmWithSameNameExistParameters(name))
                .getReturnValue();

        if (exists) {
            if (reasons != null) {
                reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_ALREADY_EXIST.toString());
            }

            return false;
        }

        boolean checkTemplateLock = getParameters().getParentCommand() == VdcActionType.AddVmPoolWithVms ? false : true;

        returnValue = verifyAddVM(reasons, storagePoolId, vmPriority);

        if (returnValue && !getParameters().getDontCheckTemplateImages()) {
            for (storage_domains storage : destStorages.values()) {
                if (!VmTemplateCommand.isVmTemplateImagesReady(getVmTemplate(), storage.getId(),
                        reasons, false, checkTemplateLock, true, true, storageToDisksMap.get(storage.getId()))) {
                    return false;
                }
            }
        }

        return returnValue;
    }

    protected boolean verifyAddVM(List<String> reasons, Guid storagePoolId, int vmPriority) {
        return VmHandler.VerifyAddVm(reasons,
                getVmInterfaces().size(),
                getVmTemplate(),
                storagePoolId,
                vmPriority);
    }

    @Override
    protected void ExecuteVmCommand() {
        ArrayList<String> errorMessages = new ArrayList<String>();
        if (CanAddVm(errorMessages, destStorages.values())) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                    AddVmStatic();
                    AddVmDynamic();
                    AddVmNetwork();
                    AddVmStatistics();
                    addActiveSnapshot();
                    getCompensationContext().stateChanged();
                    return null;
                }
            });
            freeLock();

            addVmPermission();
            if (AddVmImages()) {
                copyVmDevices();
                addDiskPermissions(newDiskImages);
                addVmPayload();
                setActionReturnValue(getVm().getId());
                setSucceeded(true);
            }
        } else {
            log.errorFormat("Failed to add vm . The reasons are: {0}", StringUtils.join(errorMessages, ','));
        }
    }

    protected void addVmPayload() {
        VmPayload payload = getParameters().getVmPayload();

        if (payload != null) {
            VmDeviceUtils.addManagedDevice(new VmDeviceId(Guid.NewGuid(), getParameters().getVmId()),
                    VmDeviceType.DISK,
                    payload.getType(),
                    payload.getSpecParams(),
                    true,
                    true);
        }
    }

    protected void copyVmDevices() {
        VmDeviceUtils.copyVmDevices(getVmTemplateId(),
                getVmId(),
                newDiskImages,
                _vmInterfaces);
    }

    protected static boolean IsLegalClusterId(Guid clusterId, List<String> reasons) {
        // check given cluster id
        VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDAO().get(clusterId);
        boolean legalClusterId = (vdsGroup != null);
        if (!legalClusterId) {
            reasons.add(VdcBllErrors.VM_INVALID_SERVER_CLUSTER_ID.toString());
        }
        return legalClusterId;
    }

    protected boolean areParametersLegal(List<String> reasons) {
        boolean returnValue = false;
        final VmStatic vmStaticData = getParameters().getVmStaticData();

        if (vmStaticData != null) {

            returnValue = vmStaticData.getMigrationSupport() != MigrationSupport.PINNED_TO_HOST
                    || !vmStaticData.getauto_startup();

            if (!returnValue) {
                reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_CANNOT_BE_HIGHLY_AVAILABLE_AND_PINNED_TO_HOST
                        .toString());
            }

            if (!returnValue) {
                returnValue = returnValue && IsLegalClusterId(vmStaticData.getvds_group_id(), reasons);
            }

            if (!isPinningAndMigrationValid(reasons, vmStaticData, getParameters().getVm().getCpuPinning())) {
                returnValue = false;
            }

            returnValue = returnValue
                    && VmHandler.isMemorySizeLegal(vmStaticData.getos(), vmStaticData.getmem_size_mb(),
                            reasons, getVdsGroup().getcompatibility_version().toString());

        }
        return returnValue;
    }

    protected void AddVmNetwork() {
        // Add interfaces from template
        for (VmNetworkInterface iface : getVmInterfaces()) {
            iface.setId(Guid.NewGuid());
            iface.setMacAddress(MacPoolManager.getInstance().allocateNewMac());
            iface.setSpeed(VmInterfaceType.forValue(iface.getType()).getSpeed());
            iface.setVmTemplateId(null);
            iface.setVmId(getParameters().getVmStaticData().getId());
            DbFacade.getInstance().getVmNetworkInterfaceDAO().save(iface);
            getCompensationContext().snapshotNewEntity(iface);
            DbFacade.getInstance().getVmNetworkStatisticsDAO().save(iface.getStatistics());
            getCompensationContext().snapshotNewEntity(iface.getStatistics());
        }
    }

    protected void AddVmStatic() {
        VmStatic vmStatic = getParameters().getVmStaticData();
        if (vmStatic.getorigin() == null) {
            vmStatic.setorigin(OriginType.valueOf(Config.<String> GetValue(ConfigValues.OriginType)));
        }
        vmStatic.setId(getVmId());
        vmStatic.setQuotaId(getQuotaId());
        vmStatic.setcreation_date(new Date());
        // Parses the custom properties field that was filled by frontend to
        // predefined and user defined fields
        if (vmStatic.getCustomProperties() != null) {
            VMCustomProperties properties =
                    VmPropertiesUtils.getInstance().parseProperties(getVdsGroupDAO()
                            .get(getParameters().getVm().getvds_group_id())
                            .getcompatibility_version(),
                            vmStatic.getCustomProperties());
            String predefinedProperties = properties.getPredefinedProperties();
            String userDefinedProperties = properties.getUseDefinedProperties();
            vmStatic.setPredefinedProperties(predefinedProperties);
            vmStatic.setUserDefinedProperties(userDefinedProperties);
        }
        getVmStaticDao().save(vmStatic);
        getCompensationContext().snapshotNewEntity(vmStatic);
    }

    void AddVmDynamic() {
        VmDynamic tempVar = new VmDynamic();
        tempVar.setId(getVmId());
        tempVar.setstatus(VMStatus.Down);
        tempVar.setvm_host("");
        tempVar.setvm_ip("");
        tempVar.setdisplay_type(getParameters().getVmStaticData().getdefault_display_type());
        VmDynamic vmDynamic = tempVar;
        DbFacade.getInstance().getVmDynamicDAO().save(vmDynamic);
        getCompensationContext().snapshotNewEntity(vmDynamic);
    }

    void AddVmStatistics() {
        VmStatistics stats = new VmStatistics();
        stats.setId(getVmId());
        DbFacade.getInstance().getVmStatisticsDAO().save(stats);
        getCompensationContext().snapshotNewEntity(stats);
    }

    protected boolean AddVmImages() {
        if (getVmTemplate().getDiskMap().size() > 0) {
            if (getVm().getstatus() != VMStatus.Down) {
                log.error("Cannot add images. VM is not Down");
                throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
            }
            VmHandler.LockVm(getVmId());
            for (DiskImage dit : getImagesToCheckDestinationStorageDomains()) {
                CreateSnapshotFromTemplateParameters tempVar = new CreateSnapshotFromTemplateParameters(
                        dit.getImageId(), getParameters().getVmStaticData().getId());
                tempVar.setDestStorageDomainId(diskInfoDestinationMap.get(dit.getId()).getstorage_ids().get(0));
                tempVar.setStorageDomainId(dit.getstorage_ids().get(0));
                tempVar.setVmSnapshotId(getVmSnapshotId());
                tempVar.setParentCommand(VdcActionType.AddVm);
                tempVar.setEntityId(getParameters().getEntityId());
                tempVar.setParentParemeters(getParameters());
                tempVar.setQuotaId(diskInfoDestinationMap.get(dit.getId()).getQuotaId());
                VdcReturnValueBase result =
                        getBackend().runInternalAction(VdcActionType.CreateSnapshotFromTemplate,
                                tempVar,
                                ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
                getParameters().getImagesParameters().add(tempVar);

                /**
                 * if couldn't create snapshot then stop the transaction and the command
                 */
                if (!result.getSucceeded()) {
                    throw new VdcBLLException(result.getFault().getError());
                } else {
                    getTaskIdList().addAll(result.getInternalTaskIdList());
                    newDiskImages.add((DiskImage) result.getActionReturnValue());
                }
            }
        }
        return true;
    }

    @Override
    protected void removeQuotaCommandLeftOver() {
        if (!isInternalExecution()) {
            QuotaManager.removeMultiStorageDeltaQuotaCommand(getQuotaConsumeMap(diskInfoDestinationMap.values()),
                    getStoragePool().getQuotaEnforcementType(),
                    getCommandId());
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? (getReturnValue().getTaskIdList().size() > 0 ? AuditLogType.USER_ADD_VM_STARTED
                    : AuditLogType.USER_ADD_VM) : AuditLogType.USER_FAILED_ADD_VM;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_ADD_VM_FINISHED_SUCCESS
                    : AuditLogType.USER_ADD_VM_FINISHED_FAILURE;

        default:
            return AuditLogType.USER_ADD_VM_FINISHED_FAILURE;
        }
    }

    @Override
    protected VdcActionType getChildActionType() {
        return VdcActionType.CreateSnapshotFromTemplate;
    }

    @Override
    protected void EndWithFailure() {
        super.EndActionOnDisks();

        if (getVm() != null) {
            RemoveVmInSpm(getVm().getstorage_pool_id(), getVmId());
        }
        removeVmRelatedEntitiesFromDb();
        setSucceeded(true);
    }

    protected void removeVmRelatedEntitiesFromDb() {
        RemoveVmUsers();
        RemoveVmNetwork();
        new SnapshotsManager().removeSnapshots(getVmId());
        RemoveVmStatic();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();
        permissionList.add(new PermissionSubject(getVdsGroupId(),
                VdcObjectType.VdsGroups,
                getActionType().getActionGroup()));
        permissionList.add(new PermissionSubject(getVmTemplateId(),
                VdcObjectType.VmTemplate,
                getActionType().getActionGroup()));
        if (getVmTemplate() != null && !getVmTemplate().getDiskList().isEmpty()) {
            addStoragePermissionByQuotaMode(permissionList,
                    GuidUtils.getGuidValue(getStoragePoolId()),
                    GuidUtils.getGuidValue(getStorageDomainId()));
        }
        permissionList =
                QuotaHelper.getInstance().addQuotaPermissionSubject(permissionList, getStoragePool(), getQuotaId());
        permissionList.addAll(QuotaHelper.getInstance()
                .getPermissionsForDiskImagesList(diskInfoDestinationMap.values(), getStoragePool()));

        addPermissionSubjectForCustomProperties(permissionList);
        return permissionList;
    }

    protected void addPermissionSubjectForCustomProperties(List<PermissionSubject> permissionList) {
        VmStatic vmFromParams = getParameters().getVmStaticData();

        // user need specific permission to change custom properties
        if (vmFromParams != null && !StringUtils.isEmpty(vmFromParams.getCustomProperties())) {
            permissionList.add(new PermissionSubject(getVdsGroupId(),
                    VdcObjectType.VdsGroups, ActionGroup.CHANGE_VM_CUSTOM_PROPERTIES));
        }
    }

    protected void addVmPermission() {
        if ((getParameters()).isMakeCreatorExplicitOwner()) {
            permissions perms = new permissions(getCurrentUser().getUserId(), PredefinedRoles.VM_OPERATOR.getId(),
                    getVmId(), VdcObjectType.VM);
            MultiLevelAdministrationHandler.addPermission(perms);
        }
    }

    protected void addDiskPermissions(List<DiskImage> newDiskImages) {
        permissions[] permsArray = new permissions[newDiskImages.size()];
        for (int i = 0; i < newDiskImages.size(); i++) {
            permsArray[i] =
                    new permissions(getCurrentUser().getUserId(),
                            PredefinedRoles.DISK_OPERATOR.getId(),
                            newDiskImages.get(i).getId(),
                            VdcObjectType.Disk);
        }
        MultiLevelAdministrationHandler.addPermission(permsArray);
    }

    protected void addActiveSnapshot() {
        _vmSnapshotId = Guid.NewGuid();
        new SnapshotsManager().addActiveSnapshot(_vmSnapshotId, getVm(), getCompensationContext());
    }

    @Override
    protected Map<String, String> getExclusiveLocks() {
        if (!StringUtils.isBlank(getParameters().getVm().getvm_name())) {
            return Collections.singletonMap(getParameters().getVm().getvm_name(), LockingGroup.VM_NAME.name());
        }
        return null;
    }

    protected VmDynamicDAO getVmDynamicDao() {
        return DbFacade.getInstance().getVmDynamicDAO();
    }

    protected VmStaticDAO getVmStaticDao() {
        return DbFacade.getInstance().getVmStaticDAO();
    }

}
