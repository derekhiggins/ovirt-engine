package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ovirt.engine.core.bll.job.ExecutionContext;
import org.ovirt.engine.core.bll.job.ExecutionContext.ExecutionMethod;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.job.JobRepositoryFactory;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.bll.storage.StorageHelperDirector;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.IVdsAsyncCommand;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.LunDisk;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.storage_server_connections;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.job.Job;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.vdscommands.FailedToRunVmVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.UpdateVdsDynamicDataVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.UpdateVmDynamicDataVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VmMonitorCommandVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

/**
 * Base class for asynchronous running process handling
 */
public abstract class RunVmCommandBase<T extends VmOperationParameterBase> extends VmCommand<T> implements
        IVdsAsyncCommand {

    private static Log log = LogFactory.getLog(RunVmCommandBase.class);
    protected static final HashMap<Guid, Integer> _vds_pending_vm_count = new HashMap<Guid, Integer>();
    private final Object _decreaseLock = new Object();
    private VdsSelector privateVdsSelector;
    protected boolean _isRerun = false;
    protected VDS _destinationVds;
    private SnapshotsValidator snapshotsValidator=new SnapshotsValidator();

    protected RunVmCommandBase(Guid commandId) {
        super(commandId);
    }

    public RunVmCommandBase(T parameters) {
        super(parameters);
    }

    protected abstract VDS getDestinationVds();

    protected VdsSelector getVdsSelector() {
        return privateVdsSelector;
    }

    protected void setVdsSelector(VdsSelector value) {
        privateVdsSelector = value;
    }

    public SnapshotsValidator getSnapshotsValidator() {
        return snapshotsValidator;
    }

    public void setSnapshotsValidator(SnapshotsValidator snapshotsValidator) {
        this.snapshotsValidator = snapshotsValidator;
    }

    /**
     * List on all vdss, vm run on. In the case of problem to run vm will be more then one
     */
    private List<Guid> getRunVdssList() {
        return getVdsSelector().getRunVdssList();
    }

    /**
     * Check if the given host has enough CPU to run the VM, without exceeding the high utilization threshold.
     *
     * @param vds
     *            The host to check.
     * @param vm
     *            The VM to check.
     * @return Does this host has enough CPU (without exceeding the threshold) to run the VM.
     */
    public static boolean hasCpuToRunVM(VDS vds, VM vm) {
        if (vds.getusage_cpu_percent() == null || vm.getusage_cpu_percent() == null) {
            return false;
        }

        // The predicted CPU is actually the CPU that the VM will take considering how many cores it has and now many
        // cores the host has. This is why we take both parameters into consideration.
        int predictedVmCpu = (vm.getusage_cpu_percent() * vm.getnum_of_cpus()) / vds.getcpu_cores();
        boolean result = vds.getusage_cpu_percent() + predictedVmCpu <= vds.gethigh_utilization();
        if (log.isDebugEnabled()) {
            log.debugFormat("Host {0} has {1}% CPU load; VM {2} is predicted to have {3}% CPU load; " +
                    "High threshold is {4}%. Host is {5}suitable in terms of CPU.",
                    vds.getvds_name(),
                    vds.getusage_cpu_percent(),
                    vm.getvm_name(),
                    predictedVmCpu,
                    vds.gethigh_utilization(),
                    (result ? "" : "not "));
        }

        return result;
    }

    public static boolean hasMemoryToRunVM(VDS curVds, VM vm) {
        boolean retVal = false;
        if (curVds.getmem_commited() != null && curVds.getphysical_mem_mb() != null && curVds.getreserved_mem() != null) {
            double vdsCurrentMem =
                    curVds.getmem_commited() + curVds.getpending_vmem_size() + curVds.getguest_overhead() + curVds
                            .getreserved_mem() + vm.getMinAllocatedMem();
            double vdsMemLimit = curVds.getmax_vds_memory_over_commit() * curVds.getphysical_mem_mb() / 100.0;
            if (log.isDebugEnabled()) {
                log.debugFormat("hasMemoryToRunVM: host {0} pending vmem size is : {1} MB",
                        curVds.getvds_name(),
                        curVds.getpending_vmem_size());
                log.debugFormat("Host Mem Conmmitted: {0}, Host Reserved Mem: {1}, Host Guest Overhead {2}, VM Min Allocated Mem {3}",
                        curVds.getmem_commited(),
                        curVds.getreserved_mem(),
                        curVds.getguest_overhead(),
                        vm.getMinAllocatedMem());
                log.debugFormat("{0} <= ???  {1}", vdsCurrentMem, vdsMemLimit);
            }
            retVal = (vdsCurrentMem <= vdsMemLimit);
        }
        return retVal;
    }

    public static boolean hasCapacityToRunVM(VDS curVds) {
        boolean hasCapacity = true;
        if (curVds.getvds_type() == VDSType.PowerClient) {
            log.infoFormat(
                    "Checking capacity for a power client - id:{0}, name:{1}, host_name(ip):{2}, vds.vm_count:{3}, PowerClientMaxNumberOfConcurrentVMs:{4}",
                    curVds.getId(),
                    curVds.getvds_name(),
                    curVds.gethost_name(),
                    curVds.getvm_count(),
                    Config.<Integer> GetValue(ConfigValues.PowerClientMaxNumberOfConcurrentVMs));
            int pending_vm_count = 0;
            if (Config.<Boolean> GetValue(ConfigValues.PowerClientRunVmShouldVerifyPendingVMsAsWell)
                    && _vds_pending_vm_count.containsKey(curVds.getId())) {
                pending_vm_count = _vds_pending_vm_count.get(curVds.getId());
            }

            final int powerClientMaxNumberOfConcurrentVMs = Config
            .<Integer> GetValue(ConfigValues.PowerClientMaxNumberOfConcurrentVMs);
            if ((curVds.getvm_count() + pending_vm_count + 1) > powerClientMaxNumberOfConcurrentVMs) {
                log.infoFormat(
                        "No capacity for a power client - id:{0}, name:{1}, host_name(ip):{2}, vds.vm_count:{3}, PowerClientMaxNumberOfConcurrentVMs:{4}",
                        curVds.getId(),
                        curVds.getvds_name(),
                        curVds.gethost_name(),
                        curVds.getvm_count(),
                        powerClientMaxNumberOfConcurrentVMs);
                hasCapacity = false;
            }
        }
        return hasCapacity;
    }

    public static void DoCompressionCheck(VDS vds, VmDynamic vm) {
        if (Config.<Boolean> GetValue(ConfigValues.PowerClientSpiceDynamicCompressionManagement)) {
            // compression always enabled on VDS
            if (vds.getvds_type() != VDSType.PowerClient) {
                return;
            } else {
                String compression_enabled = "on";
                if (StringHelper.EqOp(vds.gethost_name(), vm.getclient_ip())) {
                    compression_enabled = "off";
                }
                log.infoFormat(
                        "VdcBLL.VmHandler.DoCompressionCheck - sending monitor command for vmid: {0} - set_red_image_compression and set_red_streaming_video to {1}",
                        vm.getId(),
                        compression_enabled);
                Backend.getInstance()
                        .getResourceManager()
                        .RunVdsCommand(
                                VDSCommandType.VmMonitorCommand,
                                new VmMonitorCommandVDSCommandParameters(vds.getId(), vm.getId(),
                                        "set_red_image_compression " + compression_enabled));
                Backend.getInstance()
                        .getResourceManager()
                        .RunVdsCommand(
                                VDSCommandType.VmMonitorCommand,
                                new VmMonitorCommandVDSCommandParameters(vds.getId(), vm.getId(),
                                        "set_red_streaming_video " + compression_enabled));
            }
        }
    }

    /**
     * This function called when vds failed to run vm. Vm will be run on another vds (if exist) that's why the function
     * should be run at a new thread because of it will lock a new VDSM
     */
    @Override
    public final void Rerun() {
        ThreadPoolUtil.execute(new Runnable() {
            @Override
            public void run() {
                rerunInternal();
            }
        });
    }

    protected void rerunInternal() {
        Guid vdsId = getDestinationVds() != null ? getDestinationVds().getId() : getCurrentVdsId();
        DecreasePendingVms(vdsId);

        setSucceeded(false);
        setVm(null);
        /**
         * Rerun VM only if not exceeded maximum rerun attempts. for example if there are 10 hosts that can run VM and
         * predefine maximum 3 attempts to rerun VM - on 4th turn vm will stop to run despite there are still available
         * hosts to run it DO NOT TRY TO RERUN IF RESUME FAILED.
         */
        if (getRunVdssList().size() < Config.<Integer> GetValue(ConfigValues.MaxRerunVmOnVdsCount)
                && getVm().getstatus() != VMStatus.Paused) {
            _isRerun = true;
            // restore CanDoAction value to false so CanDoAction checks will run again
            getReturnValue().setCanDoAction(false);
            log();
            if (getExecutionContext() != null) {
                Job job = getExecutionContext().getJob();
                if (job != null) {
                    // mark previous steps as fail
                    JobRepositoryFactory.getJobRepository().closeCompletedJobSteps(job.getId(), JobExecutionStatus.FAILED);
                }
            }
            ExecuteAction();
            if (!getReturnValue().getCanDoAction()) {
                _isRerun = false;
                log();
                FailedToRunVm();
            }
        } else {
            Backend.getInstance().getResourceManager().RemoveAsyncRunningCommand(getVmId());
            FailedToRunVm();
            _isRerun = false;
        }
    }

    protected void FailedToRunVm() {
        ThreadPoolUtil.execute(new Runnable() {
            @Override
            public void run() {
                processVmPoolOnStopVm();
            }
        });
        ExecutionHandler.setAsyncJob(getExecutionContext(), false);
        ExecutionHandler.endJob(getExecutionContext(), false);
    }

    private void processVmPoolOnStopVm() {
        VmPoolHandler.ProcessVmPoolOnStopVm(getVm().getId(),
                ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
    }

    /**
     * Asynchronous event, send by vds on running vm success. Vm decided successfully run when it's status turn to Up.
     * If there are vdss, not succeeded to run vm - treat them as suspicious.
     */
    @Override
    public void RunningSucceded() {
        DecreasePendingVms(getCurrentVdsId());

        setSucceeded(true);
        setActionReturnValue(VMStatus.Up);
        log();
        ExecutionHandler.setAsyncJob(getExecutionContext(), false);
        ExecutionHandler.endJob(getExecutionContext(), true);
        for (Guid vdsId : getRunVdssList()) {
            if (!getCurrentVdsId().equals(vdsId)) {
                Backend.getInstance().getResourceManager()
                        .RunVdsCommand(VDSCommandType.FailedToRunVm, new FailedToRunVmVDSCommandParameters(vdsId));
            }
        }

        if (getVm().getlast_vds_run_on() == null || !getVm().getlast_vds_run_on().equals(getCurrentVdsId())) {
            getVm().setlast_vds_run_on(getCurrentVdsId());
        }
        if (!StringHelper.isNullOrEmpty(getVm().gethibernation_vol_handle())) {
            HandleHibernatedVm(getActionType(), true);
            // In order to prevent a race where VdsUpdateRuntimeInfo saves the Vm Dynamic as UP prior to execution of
            // this method (which is a part of the cached VM command,
            // so the state this method is aware to is RESTORING, in case of RunVmCommand after the VM got suspended.
            // In addition, as the boolean return value of HandleHIbernateVm is ignored here, it is safe to set the
            // status to up.
            getVm().setstatus(VMStatus.Up);
            getVm().sethibernation_vol_handle(null);
            Backend.getInstance()
                    .getResourceManager()
                    .RunVdsCommand(VDSCommandType.UpdateVmDynamicData,
                            new UpdateVmDynamicDataVDSCommandParameters(getCurrentVdsId(), getVm().getDynamicData()));
        }
    }

    @Override
    public void reportCompleted() {
        ExecutionContext executionContext = getExecutionContext();
        if (executionContext != null && executionContext.isMonitored()) {
            if (!executionContext.isCompleted()) {
                if (executionContext.getExecutionMethod() == ExecutionMethod.AsJob) {
                    ExecutionHandler.endJob(executionContext, false);
                } else if (executionContext.getExecutionMethod() == ExecutionMethod.AsStep) {
                    ExecutionHandler.endStep(executionContext, executionContext.getStep(), false);
                }
            }
        }
    }

    @Override
    protected void EndVmCommand() {
        setCommandShouldBeLogged(false);
        setSucceeded(true);
    }

    protected Guid getCurrentVdsId() {
        return getVds().getId();
    }

    @Override
    public boolean getAutoStart() {
        return getVm().getauto_startup();
    }

    @Override
    public Guid getAutoStartVdsId() {
        return null;
    }

    protected boolean connectLunDisks(Guid hostId) {
        if (getVm().getDiskMap().isEmpty()) {
            VmHandler.updateDisksFromDb(getVm());
        }
        List<LunDisk> lunDisks = ImagesHandler.filterDiskBasedOnLuns(getVm().getDiskMap().values());
        for (LunDisk lunDisk : lunDisks) {
            LUNs lun = lunDisk.getLun();
            lun.setLunConnections(new ArrayList<storage_server_connections>(DbFacade.getInstance()
                                            .getStorageServerConnectionDAO()
                                            .getAllForLun(lun.getLUN_id())));
            if (!StorageHelperDirector.getInstance().getItem(lun.getLunConnections().get(0).getstorage_type())
                        .ConnectStorageToLunByVdsId(null, hostId, lun, getVm().getstorage_pool_id())) {
                log.infoFormat("Failed to connect  a lun disk to vdsm {0} skiping it", hostId);
                return false;
            }
        }
        return true;
    }

    protected void DecreasePendingVms(Guid vdsId) {
        synchronized (_decreaseLock) {
            boolean updateDynamic = false;
            VDS vds = DbFacade.getInstance().getVdsDAO().get(vdsId);
            if (vds == null)
                return;
            // VCPU
            if (vds.getpending_vcpus_count() != null && !vds.getpending_vcpus_count().equals(0)) {
                vds.setpending_vcpus_count(vds.getpending_vcpus_count() - getVm().getnum_of_cpus());
                updateDynamic = true;
            } else if (log.isDebugEnabled()) {
                log.debugFormat(
                        "DecreasePendingVms::Decreasing vds {0} pending vcpu count failed, its already 0 or null",
                        vds.getvds_name(), getVm().getvm_name());
            }
            // VMEM
            if (vds.getpending_vmem_size() > 0) {
                // decrease min memory assigned, because it is already taken in account when VM is up
                updateDynamic = true;
                if (vds.getpending_vmem_size() >= getVm().getMinAllocatedMem()) {
                    vds.setpending_vmem_size(vds.getpending_vmem_size() - getVm().getMinAllocatedMem());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debugFormat("Pending host {0} vmem {1} is smaller than VM min allocated memory {2},Setting pending host vmem to 0.",
                                vds.getvds_name(),
                                vds.getpending_vmem_size(),
                                getVm().getMinAllocatedMem());
                    }
                    vds.setpending_vmem_size(0);
                }
            } else if (log.isDebugEnabled()) {
                log.debugFormat(
                        "DecreasePendingVms::Decreasing vds {0} pending vmem size failed, its already 0 or null",
                        vds.getvds_name(), getVm().getvm_name());
            }
            if (updateDynamic) {
                Backend.getInstance()
                        .getResourceManager()
                        .RunVdsCommand(VDSCommandType.UpdateVdsDynamicData,
                                new UpdateVdsDynamicDataVDSCommandParameters(vds.getDynamicData()));
                if (log.isDebugEnabled()) {
                    log.debugFormat("DecreasePendingVms::Decreasing vds {0} pending vcpu count, now {1}. Vm: {2}",
                            vds.getvds_name(), vds.getpending_vcpus_count(), getVm().getvm_name());
                    log.debugFormat("DecreasePendingVms::Decreasing vds {0} pending vmem size, now {1}. Vm: {2}",
                            vds.getvds_name(), vds.getpending_vmem_size(), getVm().getvm_name());
                }
            }
        }
    }

}
