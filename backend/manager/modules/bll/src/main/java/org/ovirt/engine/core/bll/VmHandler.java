package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.utils.VmValidationUtils;
import org.ovirt.engine.core.common.vdscommands.IrsBaseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVmStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RpmVersion;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.ObjectIdentityChecker;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils.ValidationError;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils.ValidationFailureReason;

public class VmHandler {
    private static final Map<VmPropertiesUtils.ValidationFailureReason, String> failureReasonsToVdcBllMessagesMap =
            new HashMap<VmPropertiesUtils.ValidationFailureReason, String>();
    private static final Map<VmPropertiesUtils.ValidationFailureReason, String> failureReasonsToFormatMessages =
            new HashMap<VmPropertiesUtils.ValidationFailureReason, String>();
    static {
        failureReasonsToVdcBllMessagesMap.put(ValidationFailureReason.DUPLICATE_KEY,
                VdcBllMessages.ACTION_TYPE_FAILED_INVALID_CUSTOM_VM_PROPERTIES_DUPLICATE_KEYS.name());
        failureReasonsToVdcBllMessagesMap.put(ValidationFailureReason.KEY_DOES_NOT_EXIST,
                VdcBllMessages.ACTION_TYPE_FAILED_INVALID_CUSTOM_VM_PROPERTIES_INVALID_KEYS.name());
        failureReasonsToVdcBllMessagesMap.put(ValidationFailureReason.INCORRECT_VALUE,
                VdcBllMessages.ACTION_TYPE_FAILED_INVALID_CUSTOM_VM_PROPERTIES_INVALID_VALUES.name());
        failureReasonsToFormatMessages.put(ValidationFailureReason.DUPLICATE_KEY, "$DuplicateKeys %1$s");
        failureReasonsToFormatMessages.put(ValidationFailureReason.KEY_DOES_NOT_EXIST, "$MissingKeys %1$s");
        failureReasonsToFormatMessages.put(ValidationFailureReason.INCORRECT_VALUE, "$WrongValueKeys %1$s");

    }

    public static ObjectIdentityChecker mUpdateVmsStatic;

    /**
     * Initialize static list containers, for identity and permission check. The initialization should be executed
     * before calling ObjectIdentityChecker.
     *
     * @see Backend#InitHandlers
     */
    public static void Init() {
        mUpdateVmsStatic = new ObjectIdentityChecker(VmHandler.class,
                Arrays.asList(new String[] { "VM", "VmStatic", "VmDynamic" }), VMStatus.class);

        mUpdateVmsStatic.AddPermittedFields(new String[] { "vm_name", "description", "domain", "os", "osType",
                "creation_date", "num_of_monitors", "allowConsoleReconnect", "usb_policy", "is_auto_suspend",
                "auto_startup",
                "dedicated_vm_for_vds", "default_display_type", "priority", "default_boot_sequence", "initrd_url",
                "kernel_url", "kernel_params", "migrationSupport", "minAllocatedMem", "quotaId", "quotaName",
                "quotaEnforcementType", "cpuPinning",
                "vmPayload", "balloonEnabled" });
        mUpdateVmsStatic.AddFields(
                Arrays.asList(new Enum<?>[] { VMStatus.Down }),
                Arrays.asList(new String[] { "vds_group_id", "time_zone", "is_stateless", "nice_level", "mem_size_mb",
                        "num_of_sockets", "cpu_per_socket", "iso_path", "userDefinedProperties",
                        "predefinedProperties", "customProperties", "images", "interfaces" }));
    }

    /**
     * Verifies the add vm command .
     *
     * @param reasons
     *            The reasons.
     * @param nicsCount
     *            How many vNICs need to be allocated.
     * @param vmTemplate
     *            The vm template id.
     * @return
     */
    public static boolean VerifyAddVm(List<String> reasons,
            int nicsCount,
            VmTemplate vmTemplate,
            Guid storagePoolId,
            int vmPriority) {
        boolean returnValue = true;
        if (MacPoolManager.getInstance().getavailableMacsCount() < nicsCount) {
            if (reasons != null) {
                reasons.add(VdcBllMessages.MAC_POOL_NOT_ENOUGH_MAC_ADDRESSES.toString());
            }
            returnValue = false;
        } else {
            boolean isValid = ((Boolean) Backend.getInstance().getResourceManager()
                    .RunVdsCommand(VDSCommandType.IsValid, new IrsBaseVDSCommandParameters(storagePoolId))
                    .getReturnValue()).booleanValue();
            if (isValid) {
                if (!VmTemplateCommand.IsVmPriorityValueLegal(vmPriority, reasons)) {
                    returnValue = false;
                }
            } else if (reasons != null) {
                reasons.add(VdcBllMessages.IMAGE_REPOSITORY_NOT_FOUND.toString());
            }
        }
        return returnValue;
    }

    public static boolean isVmWithSameNameExistStatic(String vmName) {
        List<VmStatic> vmStatic = DbFacade.getInstance().getVmStaticDAO().getAllByName(vmName);
        return (vmStatic.size() != 0);
    }

    public static void QueueAndLockVm(Guid vmId) {
        LockVm(vmId);
    }

    /**
     * Lock the VM in a new transaction, saving compensation data of the old status.
     *
     * @param vm
     *            The VM to lock.
     * @param compensationContext
     *            Used to save the old VM status, for compensation purposes.
     */
    public static void LockVm(final VmDynamic vm, final CompensationContext compensationContext) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                compensationContext.snapshotEntityStatus(vm, vm.getstatus());
                LockVm(vm.getId());
                compensationContext.stateChanged();
                return null;
            }
        });
    }

    /**
     * Check VM status before locking it, If VM status is not down, we throw an exception.
     *
     * @param status
     *            - The status of the VM
     */
    private static void checkStatusBeforeLock(VMStatus status) {
        if (status == VMStatus.ImageLocked) {
            log.error("VM status cannot change to image locked, since it is already locked");
            throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
        }
    }

    /**
     * Lock VM after check its status, If VM status is locked, we throw an exception.
     *
     * @param status
     *            - The status of the VM
     * @param vmId
     *            - The ID of the VM.
     */
    public static void checkStatusAndLockVm(Guid vmId) {
        VmDynamic vmDynamic = DbFacade.getInstance().getVmDynamicDAO().get(vmId);
        checkStatusBeforeLock(vmDynamic.getstatus());
        LockVm(vmId);
    }

    /**
     * Lock VM with compensation, after checking its status, If VM status is locked, we throw an exception.
     *
     * @param status
     *            - The status of the VM
     * @param vmId
     *            - The ID of the VM, which we want to lock.
     * @param compensationContext
     *            - Used to save the old VM status for compensation purposes.
     */
    public static void checkStatusAndLockVm(Guid vmId, CompensationContext compensationContext) {
        VmDynamic vmDynamic = DbFacade.getInstance().getVmDynamicDAO().get(vmId);
        checkStatusBeforeLock(vmDynamic.getstatus());
        LockVm(vmDynamic, compensationContext);
    }

    public static void LockVm(Guid vmId) {
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.SetVmStatus,
                        new SetVmStatusVDSCommandParameters(vmId, VMStatus.ImageLocked));
    }

    /**
     * Unlock the VM in a new transaction, saving compensation data of the old status.
     *
     * @param vm
     *            The VM to unlock.
     * @param compensationContext
     *            Used to save the old VM status, for compensation purposes.
     */
    public static void unlockVm(final VmDynamic vm, final CompensationContext compensationContext) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                compensationContext.snapshotEntityStatus(vm, vm.getstatus());
                vm.setstatus(VMStatus.Down);
                UnLockVm(vm.getId());
                compensationContext.stateChanged();
                return null;
            }
        });
    }

    public static void UnLockVm(Guid vmId) {
        VM vm = DbFacade.getInstance().getVmDAO().get(vmId);
        if (vm.getstatus() == VMStatus.ImageLocked) {
            Backend.getInstance()
                    .getResourceManager()
                    .RunVdsCommand(VDSCommandType.SetVmStatus, new SetVmStatusVDSCommandParameters(vmId, VMStatus.Down));
        } else {
            log.errorFormat("Trying to unlock vm {0} in status {1} - not moving to down!", vm.getvm_name(),
                    vm.getstatus());
        }
    }

    public static void MarkVmAsIllegal(Guid vmId) {
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.SetVmStatus,
                        new SetVmStatusVDSCommandParameters(vmId, VMStatus.ImageIllegal));
    }

    public static void updateDisksFromDb(VM vm) {
        List<Disk> imageList = DbFacade.getInstance().getDiskDao().getAllForVm(vm.getId());
        updateDisksForVm(vm, imageList);
    }

    public static void updateDisksForVm(VM vm, List<? extends Disk> diskList) {
        for (Disk disk : diskList) {
            if (disk.isAllowSnapshot()) {
                DiskImage image = (DiskImage) disk;
                if (image.getactive() != null && image.getactive()) {
                    vm.getDiskMap().put(image.getId(), image);
                    vm.getDiskList().add(image);
                }
            } else {
                vm.getDiskMap().put(disk.getId(), disk);
            }
        }
    }

    public static void updateNetworkInterfacesFromDb(VM vm) {
        List<VmNetworkInterface> interfaces = DbFacade.getInstance().getVmNetworkInterfaceDAO().getAllForVm(vm.getId());
        vm.setInterfaces(interfaces);
    }

    private static Version GetApplicationVersion(final String part, final String appName) {
        try {
            return new RpmVersion(part, getAppName(part, appName), true);
        } catch (Exception e) {
            log.debugFormat("Failed to create rpm version object, part: {0} appName: {1}, error: {2}",
                    part,
                    appName,
                    e.toString());
            return null;
        }
    }

    private static String getAppName(final String part, final String appName) {
        if (StringUtils.contains(part, appName + "64")) { // 64 bit Agent has extension
            // to its name.
            return appName + "64";
        }
        return appName;
    }

    /**
     * Updates the {@link VM}'s {@link VM#getGuestAgentVersion()} and {@link VM#getSpiceDriverVersion()} based on the
     * VM's {@link VM#getapp_list()} property.
     *
     * @param vm
     *            the VM
     */
    public static void UpdateVmGuestAgentVersion(final VM vm) {
        if (vm.getapp_list() != null) {
            final String[] parts = vm.getapp_list().split("[,]", -1);
            if (parts != null && parts.length != 0) {
                final String agentAppName = Config.<String> GetValue(ConfigValues.AgentAppName);
                final Map<String, String> spiceDriversInGuest =
                        Config.<Map<String, String>> GetValue(ConfigValues.SpiceDriverNameInGuest);
                final String spiceDriverInGuest =
                        spiceDriversInGuest.get(ObjectUtils.toString(vm.getos().getOsType()).toLowerCase());

                for (final String part : parts) {
                    if (StringUtils.containsIgnoreCase(part, agentAppName)) {
                        vm.setGuestAgentVersion(GetApplicationVersion(part,
                                agentAppName));
                    }
                    if (StringUtils.containsIgnoreCase(part,
                            spiceDriverInGuest)) {
                        vm.setSpiceDriverVersion(GetApplicationVersion(part,
                                spiceDriverInGuest));
                    }
                }
            }
        }
    }

    /**
     * Checks the validity of the given memory size according to OS type.
     *
     * @param osType
     *            Type of the os.
     * @param memSizeInMB
     *            The mem size in MB.
     * @param reasons
     *            The reasons.VdsGroups
     * @return
     */
    public static boolean isMemorySizeLegal(VmOsType osType,
            int memSizeInMB,
            List<String> reasons,
            String clsuter_version) {
        boolean result = VmValidationUtils.isMemorySizeLegal(osType, memSizeInMB, clsuter_version);
        if (!result) {
            reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_ILLEGAL_MEMORY_SIZE.toString());
            reasons.add(String.format("$minMemorySize %s", VmValidationUtils.getMinMemorySizeInMb()));
            reasons.add(String.format("$maxMemorySize %s",
                    VmValidationUtils.getMaxMemorySizeInMb(osType, clsuter_version)));
        }
        return result;
    }

    /**
     * Check if the interface name is not duplicate in the list of interfaces.
     *
     * @param interfaces
     *            - List of interfaces the VM/Template got.
     * @param interfaceName
     *            - Candidate for interface name.
     * @param messages
     *            - Messages for CanDoAction().
     * @return - True , if name is valid, false, if name already exist.
     */
    public static boolean IsNotDuplicateInterfaceName(List<VmNetworkInterface> interfaces,
            final String interfaceName,
            List<String> messages) {

        // Interface iface = interfaces.FirstOrDefault(i => i.name ==
        // AddVmInterfaceParameters.Interface.name);
        VmNetworkInterface iface = LinqUtils.firstOrNull(interfaces, new Predicate<VmNetworkInterface>() {
            @Override
            public boolean eval(VmNetworkInterface i) {
                return i.getName().equals(interfaceName);
            }
        });

        if (iface != null) {
            messages.add(VdcBllMessages.NETWORK_INTERFACE_NAME_ALREADY_IN_USE.name());
            return false;
        }
        return true;
    }

    /**
     * Checks number of monitors validation according to VM  and Display types.
     * @param displayType
     *     Display type : Spice or Vnc
     * @param numOfMonitors
     *     Number of monitors
     * @param messages
     *     Messages for CanDoAction().
     * @return
     */
    public static boolean isNumOfMonitorsLegal(DisplayType displayType, int numOfMonitors, List<String> reasons) {
        boolean legal = true;
        if (displayType == DisplayType.vnc) {
            legal = (numOfMonitors <= 1);
        }
        else { // Spice
            legal = (numOfMonitors <= getMaxNumberOfMonitors());
        }
        if (!legal) {
            reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_ILLEGAL_NUM_OF_MONITORS.toString());
        }
        return legal;
    }

    /**
     * get max of allowed monitors from config
     * config value is a comma separated list of integers
     * @return
     */
    private static int getMaxNumberOfMonitors() {
        int max = 0;
        String numOfMonitorsStr =
                Config.GetValue(ConfigValues.ValidNumOfMonitors).toString().replaceAll("[\\[\\]]", "");
        String values[] = numOfMonitorsStr.split(",");
        for (String val : values) {
            val = val.trim();
            if (Integer.valueOf(val) > max) {
                max = Integer.valueOf(val);
            }
        }
        return max;
    }

    /**
     * Checks that the USB policy is legal for the VM. If it is ENABLED_NATIVE then it is legal only
     * in case the cluster level is >= 3.1. If it is ENABLED_LEGACY then it is not legal on Linux VMs.
     * @param vm
     *     The VM object
     * @param messages
     *     Messages for CanDoAction().
     * @return
     */
    public static boolean isUsbPolicyLegal(UsbPolicy usbPolicy,
            VmOsType osType,
            VDSGroup vdsGroup,
            List<String> messages) {
        boolean retVal = true;
        if (UsbPolicy.ENABLED_NATIVE.equals(usbPolicy)) {
            if (!Config.<Boolean> GetValue(ConfigValues.NativeUSBEnabled, vdsGroup.getcompatibility_version()
                    .getValue())) {
                messages.add(VdcBllMessages.USB_NATIVE_SUPPORT_ONLY_AVAILABLE_ON_CLUSTER_LEVEL.toString());
                retVal = false;
            }
        } else if (UsbPolicy.ENABLED_LEGACY.equals(usbPolicy)) {
            if (osType.isLinux()) {
                messages.add(VdcBllMessages.USB_LEGACY_NOT_SUPPORTED_ON_LINUX_VMS.toString());
                retVal = false;
            }
        }
        return retVal;
    }

    protected static void handleCustomPropertiesError(List<ValidationError> validationErrors, ArrayList<String> message) {
        String invalidSyntaxMsg = VdcBllMessages.ACTION_TYPE_FAILED_INVALID_CUSTOM_VM_PROPERTIES_INVALID_SYNTAX.name();

        List<String> errorMessages =
                VmPropertiesUtils.getInstance().generateErrorMessages(validationErrors, invalidSyntaxMsg,
                        failureReasonsToVdcBllMessagesMap, failureReasonsToFormatMessages);
        message.addAll(errorMessages);
    }

    private static final Log log = LogFactory.getLog(VmHandler.class);

}
