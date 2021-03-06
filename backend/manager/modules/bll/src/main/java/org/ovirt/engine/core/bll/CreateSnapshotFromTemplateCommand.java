package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.action.CreateSnapshotFromTemplateParameters;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

/**
 * This command responsible to creating new snapshot. Usually it will be called
 * during new vm creation. In the case of create snapshot from template new
 * image created from master image aka image template so new created image
 * it_guid will be equal to master image guid.
 *
 * Parameters: Guid imageId - id of ImageTemplate, snapshot will be created from
 * Guid containerId - id of VmTemplate, contains ImageTemplate
 */

@InternalCommandAttribute
public class CreateSnapshotFromTemplateCommand<T extends CreateSnapshotFromTemplateParameters> extends
        CreateSnapshotCommand<T> {

    public CreateSnapshotFromTemplateCommand(T parameters) {
        super(parameters);
        super.setVmId(parameters.getVmId());
        setImageGroupId(Guid.NewGuid());
    }

    /**
     * Old image not have to be changed
     */
    @Override
    protected void ProcessOldImageFromDb() {
    }

    @Override
    protected DiskImage CloneDiskImage(Guid newImageGuid) {
        DiskImage returnValue = super.CloneDiskImage(newImageGuid);
        returnValue.setit_guid(getImage().getImageId());
        return returnValue;
    }

    @Override
    protected Guid getDestinationStorageDomainId() {
        Guid storageDomainId = getParameters().getDestStorageDomainId();
        if (getParameters().getDestinationImageId() == null
                || Guid.Empty.equals(getParameters().getDestStorageDomainId())) {
            storageDomainId = getParameters().getStorageDomainId();
        }
        storageDomainId = (storageDomainId == null) ? Guid.Empty : storageDomainId;
        return (!Guid.Empty.equals(storageDomainId)) ? storageDomainId : super.getDestinationStorageDomainId();
    }

    @Override
    protected void EndWithFailure() {
        if (getDestinationDiskImage() != null) {
            DbFacade.getInstance().getBaseDiskDao().remove(getDestinationDiskImage().getimage_group_id());
            if (DbFacade.getInstance().getDiskImageDynamicDAO().get(getDestinationDiskImage().getImageId()) != null) {
                DbFacade.getInstance().getDiskImageDynamicDAO().remove(getDestinationDiskImage().getImageId());
            }
        }

        super.EndWithFailure();
    }
}
