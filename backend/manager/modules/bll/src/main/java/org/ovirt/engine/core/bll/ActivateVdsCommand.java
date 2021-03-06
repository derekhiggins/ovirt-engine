package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.vdscommands.ActivateVdsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVdsStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@LockIdNameAttribute
public class ActivateVdsCommand<T extends VdsActionParameters> extends VdsCommand<T> {
    public ActivateVdsCommand(T parameters) {
        super(parameters);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if(getParameters().isRunSilent()) {
            return getSucceeded() ? AuditLogType.VDS_ACTIVATE_ASYNC : AuditLogType.VDS_ACTIVATE_FAILED_ASYNC;
        } else {
            return getSucceeded() ? AuditLogType.VDS_ACTIVATE : AuditLogType.VDS_ACTIVATE_FAILED;
        }
    }

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected ActivateVdsCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected void executeCommand() {

        final VDS vds = getVds();
        ExecutionHandler.updateSpecificActionJobCompleted(vds.getId(), VdcActionType.MaintananceVds, false);
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                getCompensationContext().snapshotEntityStatus(vds.getDynamicData(), vds.getstatus());
                Backend.getInstance().getResourceManager().RunVdsCommand(VDSCommandType.SetVdsStatus,
                        new SetVdsStatusVDSCommandParameters(getVdsId(), VDSStatus.Unassigned));
                getCompensationContext().stateChanged();
                return null;
            }
        });

        setSucceeded(Backend.getInstance().getResourceManager()
                .RunVdsCommand(VDSCommandType.ActivateVds, new ActivateVdsVDSCommandParameters(getVdsId()))
                .getSucceeded());
        if (getSucceeded()) {
            // set network to operational / non-operational
            List<Network> networks = DbFacade.getInstance().getNetworkDAO()
                    .getAllForCluster(vds.getvds_group_id());
            for (Network net : networks) {
                AttachNetworkToVdsGroupCommand.SetNetworkStatus(vds.getvds_group_id(), net);
            }
        }
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;
        if (getVds() == null) {
            AddErrorMessages(VdcBllMessages.VAR__ACTION__ACTIVATE, VdcBllMessages.VDS_CANNOT_ACTIVATE_VDS_NOT_EXIST);
            returnValue = false;
        }
        if (returnValue && getVds().getstatus() == VDSStatus.Up) {
            AddErrorMessages(VdcBllMessages.VAR__ACTION__ACTIVATE, VdcBllMessages.VDS_CANNOT_ACTIVATE_VDS_ALREADY_UP);
            returnValue = false;
        }
        return returnValue;
    }

    @Override
    protected Map<String, String> getExclusiveLocks() {
        return Collections.singletonMap(getParameters().getVdsId().toString(), LockingGroup.VDS.name());
    }

    private void AddErrorMessages(VdcBllMessages messageActionTypeParameter, VdcBllMessages messageReason) {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__HOST);
        addCanDoActionMessage(messageActionTypeParameter);
        addCanDoActionMessage(messageReason);
    }
}
