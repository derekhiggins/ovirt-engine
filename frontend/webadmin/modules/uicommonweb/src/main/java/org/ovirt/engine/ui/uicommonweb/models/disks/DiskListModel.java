package org.ovirt.engine.ui.uicommonweb.models.disks;

import java.util.ArrayList;

import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.AttachDettachVmDiskParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.UpdateVmDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmDiskOperatinParameterBase;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.LunDisk;
import org.ovirt.engine.core.common.businessentities.PropagateErrors;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.ObservableCollection;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.CopyDiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.MoveDiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.RemoveDiskModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

@SuppressWarnings("unused")
public class DiskListModel extends ListWithDetailsModel
{
    private UICommand privateNewCommand;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privateEditCommand;

    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    private UICommand privateMoveCommand;

    public UICommand getMoveCommand()
    {
        return privateMoveCommand;
    }

    private void setMoveCommand(UICommand value)
    {
        privateMoveCommand = value;
    }

    private UICommand privateCopyCommand;

    public UICommand getCopyCommand()
    {
        return privateCopyCommand;
    }

    private void setCopyCommand(UICommand value)
    {
        privateCopyCommand = value;
    }

    private ListModel diskVmListModel;
    private ListModel diskTemplateListModel;
    private ListModel diskStorageListModel;

    public DiskListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().disksTitle());

        setDefaultSearchString("Disks:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setSearchObjects(new String[] { SearchObjects.DISK_OBJ_NAME, SearchObjects.DISK_PLU_OBJ_NAME });
        setAvailableInModes(ApplicationMode.VirtOnly);

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        setMoveCommand(new UICommand("Move", this)); //$NON-NLS-1$
        setCopyCommand(new UICommand("Copy", this)); //$NON-NLS-1$

        UpdateActionAvailability();

        getSearchNextPageCommand().setIsAvailable(true);
        getSearchPreviousPageCommand().setIsAvailable(true);
    }

    @Override
    protected void SyncSearch()
    {
        SearchParameters tempVar = new SearchParameters(getSearchString(), SearchType.Disk);
        tempVar.setMaxCount(getSearchPageSize());
        super.SyncSearch(VdcQueryType.Search, tempVar);
    }

    @Override
    public void setItems(Iterable value)
    {
        if (value == null) {
            super.setItems(null);
            return;
        }

        ArrayList<Disk> disks = Linq.<Disk> Cast(value);
        super.setItems(disks);
    }

    @Override
    protected void InitDetailModels()
    {
        super.InitDetailModels();

        diskVmListModel = new DiskVmListModel();
        diskVmListModel.setIsAvailable(false);

        diskTemplateListModel = new DiskTemplateListModel();
        diskTemplateListModel.setIsAvailable(false);

        diskStorageListModel = new DiskStorageListModel();
        diskStorageListModel.setIsAvailable(false);

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new DiskGeneralModel());
        list.add(diskVmListModel);
        list.add(diskTemplateListModel);
        list.add(diskStorageListModel);
        list.add(new PermissionListModel());
        setDetailModels(list);
    }

    @Override
    protected void UpdateDetailsAvailability()
    {
        if (getSelectedItem() != null)
        {
            Disk disk = (Disk) getSelectedItem();

            diskVmListModel.setIsAvailable(disk.getVmEntityType() != VmEntityType.TEMPLATE);
            diskTemplateListModel.setIsAvailable(disk.getVmEntityType() == VmEntityType.TEMPLATE);
            diskStorageListModel.setIsAvailable(disk.getDiskStorageType() == DiskStorageType.IMAGE);
        }
    }

    public void Cancel()
    {
        setWindow(null);
    }

    @Override
    protected void OnSelectedItemChanged()
    {
        super.OnSelectedItemChanged();
        UpdateActionAvailability();
    }

    @Override
    protected void SelectedItemsChanged()
    {
        super.SelectedItemsChanged();
        UpdateActionAvailability();
    }

    private void New()
    {
        DiskModel model = new DiskModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().addVirtualDiskTitle());
        model.setHashName("new_virtual_disk"); //$NON-NLS-1$
        model.setIsNew(true);
        model.getIsInVm().setEntity(false);
        model.StartProgress(null);

        AsyncDataProvider.GetDataCenterList(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object target, Object returnValue) {
                DiskListModel diskListModel = (DiskListModel) target;
                DiskModel diskModel = (DiskModel) diskListModel.getWindow();
                ArrayList<storage_pool> dataCenters = (ArrayList<storage_pool>) returnValue;

                diskModel.getDataCenter().setItems(dataCenters);
                diskModel.getDataCenter().setSelectedItem(Linq.FirstOrDefault(dataCenters));

                ArrayList<UICommand> commands = new ArrayList<UICommand>();
                UICommand tempVar2 = new UICommand("OnSave", diskListModel); //$NON-NLS-1$
                tempVar2.setTitle(ConstantsManager.getInstance().getConstants().ok());
                tempVar2.setIsDefault(true);
                diskModel.getCommands().add(tempVar2);
                UICommand tempVar3 = new UICommand("Cancel", diskListModel); //$NON-NLS-1$
                tempVar3.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                tempVar3.setIsCancel(true);
                diskModel.getCommands().add(tempVar3);

                diskModel.StopProgress();
            }
        }));
    }

    private void Edit()
    {

    }

    private void OnSave()
    {
        DiskModel model = (DiskModel) getWindow();

        if (model.getProgress() != null || !model.Validate())
        {
            return;
        }

        if ((Boolean) model.getAttachDisk().getEntity())
        {
            OnAttachDisks();
            return;
        }

        // Save changes.
        storage_domains storageDomain = (storage_domains) model.getStorageDomain().getSelectedItem();

        Disk disk;
        if ((Boolean) model.getIsInternal().getEntity()) {
            DiskImage diskImage = model.getIsNew() ? new DiskImage() : (DiskImage) getSelectedItem();
            diskImage.setSizeInGigabytes(Integer.parseInt(model.getSize().getEntity().toString()));
            diskImage.setvolume_type((VolumeType) model.getVolumeType().getSelectedItem());
            diskImage.setvolume_format(model.getVolumeFormat());
            if (model.getQuota().getIsAvailable()) {
                diskImage.setQuotaId(((Quota) model.getQuota().getSelectedItem()).getId());
            }

            disk = diskImage;
        }
        else {
            LunDisk lunDisk = model.getIsNew() ? new LunDisk() : (LunDisk) getSelectedItem();
            lunDisk.setLun((LUNs) model.getSanStorageModel().getAddedLuns().get(0).getEntity());

            disk = lunDisk;
        }

        disk.setDiskAlias((String) model.getAlias().getEntity());
        disk.setDiskDescription((String) model.getDescription().getEntity());
        disk.setDiskInterface((DiskInterface) model.getInterface().getSelectedItem());
        disk.setWipeAfterDelete((Boolean) model.getWipeAfterDelete().getEntity());
        disk.setBoot((Boolean) model.getIsBootable().getEntity());
        disk.setShareable((Boolean) model.getIsShareable().getEntity());
        disk.setPlugged((Boolean) model.getIsPlugged().getEntity());
        disk.setPropagateErrors(PropagateErrors.Off);

        VdcActionType actionType;
        VmDiskOperatinParameterBase parameters;
        if (model.getIsNew())
        {
            parameters = new AddDiskParameters(Guid.Empty, disk);
            ((AddDiskParameters) parameters).setStorageDomainId(storageDomain.getId());
            actionType = VdcActionType.AddDisk;
        }
        else
        {
            parameters = new UpdateVmDiskParameters(Guid.Empty, disk.getId(), disk);
            actionType = VdcActionType.UpdateVmDisk;
        }

        model.StartProgress(null);

        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();
        paramerterList.add(parameters);

        Frontend.RunMultipleAction(actionType, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {
                        DiskListModel localModel = (DiskListModel) result.getState();
                        localModel.getWindow().StopProgress();
                        Cancel();
                    }
                },
                this);
    }

    private void OnAttachDisks()
    {
        VM vm = (VM) getEntity();
        DiskModel model = (DiskModel) getWindow();
        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();

        ArrayList<EntityModel> disksToAttach = (Boolean) model.getIsInternal().getEntity() ?
                (ArrayList<EntityModel>) model.getInternalAttachableDisks().getSelectedItems() :
                (ArrayList<EntityModel>) model.getExternalAttachableDisks().getSelectedItems();

        for (EntityModel item : disksToAttach)
        {
            DiskModel disk = (DiskModel) item.getEntity();
            AttachDettachVmDiskParameters parameters = new AttachDettachVmDiskParameters(
                    vm.getId(), disk.getDisk().getId(), (Boolean) model.getIsPlugged().getEntity());
            paramerterList.add(parameters);
        }

        model.StartProgress(null);

        Frontend.RunMultipleAction(VdcActionType.AttachDiskToVm, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {
                        DiskListModel localModel = (DiskListModel) result.getState();
                        localModel.getWindow().StopProgress();
                        Cancel();
                    }
                },
                this);
    }

    private void Move()
    {
        ArrayList<DiskImage> disks = (ArrayList<DiskImage>) getSelectedItems();

        if (disks == null || getWindow() != null)
        {
            return;
        }

        MoveDiskModel model = new MoveDiskModel();
        model.setIsSingleDiskMove(disks.size() == 1);
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().moveDisksTitle());
        model.setHashName("move_disks"); //$NON-NLS-1$
        model.setIsSourceStorageDomainNameAvailable(true);
        model.setEntity(this);
        model.init(disks);
        model.StartProgress(null);
    }

    private void Copy()
    {
        ArrayList<DiskImage> disks = (ArrayList<DiskImage>) getSelectedItems();

        if (disks == null || getWindow() != null)
        {
            return;
        }

        CopyDiskModel model = new CopyDiskModel();
        model.setIsSingleDiskMove(disks.size() == 1);
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().copyDisksTitle());
        model.setHashName("copy_disks"); //$NON-NLS-1$
        model.setEntity(this);
        model.init(disks);
        model.StartProgress(null);
    }

    private void Remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        RemoveDiskModel model = new RemoveDiskModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeDisksTitle());
        model.setHashName("remove_disk"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().disksMsg());

        model.getLatch().setIsAvailable(false);

        ArrayList<String> items = new ArrayList<String>();
        for (Object item : getSelectedItems())
        {
            Disk disk = (Disk) item;
            items.add(disk.getDiskAlias());
        }
        model.setItems(items);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private void OnRemove()
    {
        VM vm = (VM) getEntity();
        RemoveDiskModel model = (RemoveDiskModel) getWindow();
        ArrayList<VdcActionParametersBase> paramerterList = new ArrayList<VdcActionParametersBase>();

        for (Object item : getSelectedItems())
        {
            Disk disk = (Disk) item;
            VdcActionParametersBase parameters = new RemoveDiskParameters(disk.getId());
            paramerterList.add(parameters);
        }

        model.StartProgress(null);

        Frontend.RunMultipleAction(VdcActionType.RemoveDisk, paramerterList,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {
                        DiskListModel localModel = (DiskListModel) result.getState();
                        localModel.StopProgress();
                        Cancel();
                    }
                },
                this);
    }

    private void UpdateActionAvailability()
    {
        VM vm = (VM) getEntity();
        Disk disk = (Disk) getSelectedItem();
        ArrayList<Disk> disks = getSelectedItems() != null ? (ArrayList<Disk>) getSelectedItems() : null;
        boolean isDiskLocked = disk != null && disk.getDiskStorageType() == DiskStorageType.IMAGE &&
                ((DiskImage) disk).getimageStatus() == ImageStatus.LOCKED;

        getNewCommand().setIsExecutionAllowed(true);
        getEditCommand().setIsExecutionAllowed(disk != null && disks != null && disks.size() == 1 && !isDiskLocked);
        getRemoveCommand().setIsExecutionAllowed(disks != null && disks.size() > 0 && isRemoveCommandAvailable());
        getMoveCommand().setIsExecutionAllowed(disks != null && disks.size() > 0 && isMoveCommandAvailable());
        getCopyCommand().setIsExecutionAllowed(disks != null && disks.size() > 0 && isCopyCommandAvailable());
    }

    private boolean isMoveCommandAvailable() {
        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> Cast(getSelectedItems()) : new ArrayList<Disk>();

        Disk firstDisk = disks.get(0);
        if (firstDisk.getDiskStorageType() != DiskStorageType.IMAGE) {
            return false;
        }

        NGuid datacenterId = ((DiskImage) firstDisk).getstorage_pool_id();

        for (Disk disk : disks)
        {
            if (disk.getDiskStorageType() != DiskStorageType.IMAGE) {
                return false;
            }

            DiskImage diskImage = (DiskImage) disk;
            if (disk.getDiskStorageType() != DiskStorageType.IMAGE ||
                    diskImage.getimageStatus() != ImageStatus.OK ||
                    disk.getVmEntityType() == VmEntityType.TEMPLATE ||
                    !datacenterId.equals(diskImage.getstorage_pool_id()))
            {
                return false;
            }
        }

        return true;
    }

    private boolean isCopyCommandAvailable() {
        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> Cast(getSelectedItems()) : new ArrayList<Disk>();

        Disk firstDisk = disks.get(0);
        if (firstDisk.getDiskStorageType() != DiskStorageType.IMAGE) {
            return false;
        }

        NGuid datacenterId = ((DiskImage) firstDisk).getstorage_pool_id();

        for (Disk disk : disks)
        {
            DiskImage diskImage = (DiskImage) disk;
            if (diskImage.getimageStatus() != ImageStatus.OK || disk.getVmEntityType() != VmEntityType.TEMPLATE ||
                    !datacenterId.equals(diskImage.getstorage_pool_id()))
            {
                return false;
            }
        }

        return true;
    }

    private boolean isRemoveCommandAvailable() {
        ArrayList<Disk> disks =
                getSelectedItems() != null ? Linq.<Disk> Cast(getSelectedItems()) : new ArrayList<Disk>();

        for (Disk disk : disks)
        {
            boolean isTemplateDisk = disk.getVmEntityType() == VmEntityType.TEMPLATE;
            boolean isImageLocked = disk.getDiskStorageType() == DiskStorageType.IMAGE
                    && ((DiskImage) disk).getimageStatus() == ImageStatus.LOCKED;

            if (isTemplateDisk || isImageLocked)
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getNewCommand())
        {
            New();
        }
        else if (command == getEditCommand())
        {
            Edit();
        }
        else if (command == getRemoveCommand())
        {
            Remove();
        }
        else if (command == getMoveCommand())
        {
            Move();
        }
        else if (command == getCopyCommand())
        {
            Copy();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSave")) //$NON-NLS-1$
        {
            OnSave();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            Cancel();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
        {
            OnRemove();
        }
    }

    @Override
    public boolean IsSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("disk"); //$NON-NLS-1$
    }

    @Override
    protected String getListName() {
        return "DiskListModel"; //$NON-NLS-1$
    }
}
