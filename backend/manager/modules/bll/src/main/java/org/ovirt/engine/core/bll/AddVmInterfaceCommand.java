package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddVmInterfaceParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.vdscommands.HotPlugUnplgNicVDSParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Regex;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogField;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogFields;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

@CustomLogFields({ @CustomLogField("InterfaceName") })
public class AddVmInterfaceCommand<T extends AddVmInterfaceParameters> extends VmCommand<T> {

    private static final long serialVersionUID = -835005784345476993L;

    public AddVmInterfaceCommand(T parameters) {
        super(parameters);
    }

    public String getInterfaceName() {
        return getParameters().getInterface().getName();
    }

    @Override
    protected void ExecuteVmCommand() {
        AddCustomValue("InterfaceType",
                (VmInterfaceType.forValue(getParameters().getInterface().getType()).getInterfaceTranslation()).toString());
        this.setVmName(DbFacade.getInstance().getVmStaticDAO().get(getParameters().getVmId()).getvm_name());
        if (StringUtils.isEmpty(getParameters().getInterface().getMacAddress())) {
            getParameters().getInterface().setMacAddress(MacPoolManager.getInstance().allocateNewMac());
        }

        getParameters().getInterface().setSpeed(
                VmInterfaceType.forValue(
                        getParameters().getInterface().getType()).getSpeed());

        getParameters().getInterface().setId(Guid.NewGuid());
        getParameters().getInterface().setVmId(getParameters().getVmId());
        DbFacade dbFacade = DbFacade.getInstance();
        dbFacade
                .getVmNetworkInterfaceDAO()
                .save(getParameters().getInterface());
        dbFacade
                .getVmNetworkStatisticsDAO()
                .save(getParameters().getInterface().getStatistics());

        VmDevice vmDevice = VmDeviceUtils.addNetworkInterfaceDevice(
                new VmDeviceId(getParameters().getInterface().getId(), getParameters().getVmId()),
                getParameters().getInterface().isActive());

        boolean succeded = true;
        VmDynamic vmDynamic = getVm().getDynamicData();
        if (getParameters().getInterface().isActive() && vmDynamic.getstatus() == VMStatus.Up) {
            succeded = hotPlugNic(vmDevice, vmDynamic);
            if (!succeded) {
                getReturnValue().getExecuteFailedMessages().add("Failed hot-plugging nic to VM");
            }
        }
        setSucceeded(succeded);
    }

    private boolean hotPlugNic(VmDevice vmDevice, VmDynamic vmDynamic) {
        return runVdsCommand(VDSCommandType.HotPlugNic,
                new HotPlugUnplgNicVDSParameters(vmDynamic.getrun_on_vds().getValue(),
                vmDynamic.getId(),
                getParameters().getInterface(),
                        vmDevice)).getSucceeded();

    }

    @Override
    protected boolean canDoAction() {
        VmStatic vm = getVm().getStaticData();
        if (vm == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST);
            return false;
        }

        switch (DbFacade.getInstance().getVmDynamicDAO().get(getParameters().getVmId()).getstatus()) {
        case Up:
        case Down:
        case ImageLocked:
            break;
        default:
            addCanDoActionMessage(VdcBllMessages.NETWORK_CANNOT_ADD_INTERFACE_WHEN_VM_STATUS_NOT_UP_DOWN_LOCKED);
            return false;
        }

        List<VmNetworkInterface> interfaces = DbFacade.getInstance().getVmNetworkInterfaceDAO()
                .getAllForVm(getParameters().getVmId());

        if (!VmHandler.IsNotDuplicateInterfaceName(interfaces,
                getParameters().getInterface().getName(),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (getParameters().getInterface().getVmTemplateId() != null) {
            addCanDoActionMessage(VdcBllMessages.NETWORK_INTERFACE_TEMPLATE_CANNOT_BE_SET);
            return false;
        }

        // check that not exceeded PCI and IDE limit
        List<VmNetworkInterface> allInterfaces = new ArrayList<VmNetworkInterface>(interfaces);
        allInterfaces.add(getParameters().getInterface());

        List<Disk> allDisks = DbFacade.getInstance().getDiskDao().getAllForVm(getParameters().getVmId());
        if (!CheckPCIAndIDELimit(vm.getnum_of_monitors(), allInterfaces, allDisks, getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // check that the number of interfaces does not exceed limit. Necessary
        // only for version 2.2.
        boolean limitNumOfNics = Config.<Boolean> GetValue(ConfigValues.LimitNumberOfNetworkInterfaces, getVm()
                .getvds_group_compatibility_version().toString());
        if (limitNumOfNics) {
            boolean numOfNicsLegal = validateNumberOfNics(interfaces, getParameters().getInterface());
            if (!numOfNicsLegal) {
                addCanDoActionMessage(VdcBllMessages.NETWORK_INTERFACE_EXITED_MAX_INTERFACES);
                return false;
            }
        }
        // this must be the last check because it's add mac to the pool
        if (!StringUtils.isEmpty(getParameters().getInterface().getMacAddress())) {
            Regex re = new Regex(ValidationUtils.INVALID_NULLABLE_MAC_ADDRESS);
            if (re.IsMatch(getParameters().getInterface().getMacAddress())) {
                addCanDoActionMessage(VdcBllMessages.NETWORK_INVALID_MAC_ADDRESS);
                return false;
            }

            Boolean allowDupMacs = Config.<Boolean> GetValue(ConfigValues.AllowDuplicateMacAddresses);
            if (!MacPoolManager.getInstance().AddMac(getParameters().getInterface().getMacAddress())
                    && !allowDupMacs) {
                addCanDoActionMessage(VdcBllMessages.NETWORK_MAC_ADDRESS_IN_USE);
                return false;
            }
        } else if (MacPoolManager.getInstance().getavailableMacsCount() <= 0) // check
                                                                              // if
                                                                              // we
                                                                              // have
                                                                              // mac
                                                                              // address
                                                                              // in
                                                                              // pool
        {
            addCanDoActionMessage(VdcBllMessages.MAC_POOL_NOT_ENOUGH_MAC_ADDRESSES);
            return false;
        }

        // check that the exists in current cluster
        List<Network> networks = DbFacade.getInstance().getNetworkDAO().getAllForCluster(vm.getvds_group_id());

        Network interfaceNetwork = LinqUtils.firstOrNull(networks, new Predicate<Network>() {
            @Override
            public boolean eval(Network network) {
                return network.getname().equals(getParameters().getInterface().getNetworkName());
            }
        });

        if (interfaceNetwork == null) {
            addCanDoActionMessage(VdcBllMessages.NETWORK_NOT_EXISTS_IN_CURRENT_CLUSTER);
            return false;
        } else if (!interfaceNetwork.isVmNetwork()){
            AddCustomValue("networks", interfaceNetwork.getname());
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NOT_A_VM_NETWORK);
            return false;
        }

        if (getParameters().getInterface().isActive()) {
            if (getVm().getstatus() == VMStatus.Up && !canPerformHotPlug()) {
                return false;
            }
        }
        return super.canDoAction();
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    /**
     * Set the parameters for bll messages, such as type and action,
     */
    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__INTERFACE);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_ADD_VM_INTERFACE : AuditLogType.NETWORK_ADD_VM_INTERFACE_FAILED;
    }

    @Override
    public void Rollback() {
        super.Rollback();
        try {
            MacPoolManager.getInstance().freeMac(getParameters().getInterface().getMacAddress());
        } catch (java.lang.Exception e) {
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = super.getPermissionCheckSubjects();
        if (getParameters().getInterface() != null && getVm() != null && getParameters().getInterface().isPortMirroring()) {
            permissionList.add(new PermissionSubject(getVm().getstorage_pool_id(),
                    VdcObjectType.StoragePool,
                    ActionGroup.PORT_MIRRORING));
        }
        return permissionList;
    }
}
