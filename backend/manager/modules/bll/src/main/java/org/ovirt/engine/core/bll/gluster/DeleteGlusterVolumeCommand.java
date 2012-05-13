package org.ovirt.engine.core.bll.gluster;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;

/**
 * BLL command to delete a Gluster volume
 */
@NonTransactiveCommandAttribute
public class DeleteGlusterVolumeCommand extends GlusterVolumeCommandBase<GlusterVolumeParameters> {

    private static final long serialVersionUID = -4045244619084727618L;

    public DeleteGlusterVolumeCommand(GlusterVolumeParameters params) {
        super(params);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__REMOVE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_VOLUME);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        if (getGlusterVolume().isOnline()) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_VOLUME_IS_UP);
            return false;
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue returnValue =
                runVdsCommand(
                        VDSCommandType.DeleteGlusterVolume,
                        new GlusterVolumeVDSParameters(getUpServer().getId(),
                                getGlusterVolumeName()));
        setSucceeded(returnValue.getSucceeded());
        if (getSucceeded()) {
            updateVolumeStatusInDb(getParameters().getVolumeId());
        } else {
            getReturnValue().getExecuteFailedMessages().add(returnValue.getVdsError().getMessage());
            return;
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_DELETE;
        } else {
            return AuditLogType.GLUSTER_VOLUME_DELETE_FAILED;
        }
    }

    private void updateVolumeStatusInDb(Guid volumeId) {
        getGlusterVolumeDao().remove(volumeId);
    }
}