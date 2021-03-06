package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ImagesContainterParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskParameters;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.AsyncTaskResultEnum;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.businessentities.async_tasks;
import org.ovirt.engine.core.common.vdscommands.DeleteImageGroupVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;

/**
 * This command is reponsible for removing a template image.
 */
@InternalCommandAttribute
public class RemoveTemplateSnapshotCommand<T extends ImagesContainterParametersBase> extends BaseImagesCommand<T> {
    public RemoveTemplateSnapshotCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        VDSReturnValue vdsReturnValue = Backend
                .getInstance()
                .getResourceManager()
                .RunVdsCommand(
                        VDSCommandType.DeleteImageGroup,
                        new DeleteImageGroupVDSCommandParameters(getParameters().getStoragePoolId(), getParameters()
                                .getStorageDomainId(), getParameters().getImageGroupID(), getParameters()
                                .getWipeAfterDelete(), false, getStoragePool().getcompatibility_version().toString()));

        if (vdsReturnValue.getSucceeded()) {
            getReturnValue().getInternalTaskIdList().add(
                    CreateTask(vdsReturnValue.getCreationInfo(),
                            VdcActionType.RemoveVmTemplate,
                            VdcObjectType.Storage,
                            getParameters().getStorageDomainId()));

            setSucceeded(true);
        }
    }

    @Override
    protected SPMAsyncTask ConcreteCreateTask(AsyncTaskCreationInfo asyncTaskCreationInfo, VdcActionType parentCommand) {
        AsyncTaskParameters p = new AsyncTaskParameters(asyncTaskCreationInfo, new async_tasks(parentCommand,
                AsyncTaskResultEnum.success, AsyncTaskStatusEnum.running, asyncTaskCreationInfo.getTaskID(),
                getParameters(), asyncTaskCreationInfo.getStepId(), getCommandId()));
        p.setEntityId(getParameters().getEntityId());
        return AsyncTaskManager.getInstance().CreateTask(AsyncTaskType.deleteImage, p);
    }
}
