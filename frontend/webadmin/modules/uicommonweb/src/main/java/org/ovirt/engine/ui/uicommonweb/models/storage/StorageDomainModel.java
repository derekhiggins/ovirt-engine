package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;

public class StorageDomainModel extends EntityModel
{

    private storage_domains privateStorageDomain;

    public storage_domains getStorageDomain()
    {
        return privateStorageDomain;
    }

    public void setStorageDomain(storage_domains value)
    {
        privateStorageDomain = value;
    }

    private ArrayList<DiskImage> disks;

    public ArrayList<DiskImage> getDisks()
    {
        return disks;
    }

    public void setDisks(ArrayList<DiskImage> value)
    {
        if (disks != value)
        {
            disks = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Disks")); //$NON-NLS-1$
        }
    }

    public StorageDomainModel()
    {

    }

    public ArrayList<DiskModel> getDisksModels() {
        ArrayList<DiskModel> diskModels = new ArrayList<DiskModel>();

        for (DiskImage diskImage : getDisks()) {
            DiskModel diskModel = new DiskModel();
            diskModel.setDisk(diskImage);

            ListModel storageDomain = new ListModel();
            storageDomain.setSelectedItem(getStorageDomain());
            diskModel.setStorageDomain(storageDomain);

            diskModels.add(diskModel);
        }

        return diskModels;
    }

}
