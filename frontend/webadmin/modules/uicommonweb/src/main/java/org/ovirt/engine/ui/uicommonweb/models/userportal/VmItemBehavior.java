package org.ovirt.engine.ui.uicommonweb.models.userportal;

import java.util.ArrayList;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.ChangeDiskCommandParameters;
import org.ovirt.engine.core.common.action.HibernateVmParameters;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.ShutdownVmParameters;
import org.ovirt.engine.core.common.action.StopVmParameters;
import org.ovirt.engine.core.common.action.StopVmTypeEnum;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.vm_pools;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.NotImplementedException;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.DataProvider;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.configure.ChangeCDModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ConsoleModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.RdpConsoleModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.SpiceConsoleModel;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public class VmItemBehavior extends ItemBehavior
{
    public VmItemBehavior(UserPortalItemModel item)
    {
        super(item);
    }

    @Override
    public void OnEntityChanged()
    {
        UpdateProperties();
        UpdateActionAvailability();
    }

    @Override
    public void EntityPropertyChanged(PropertyChangedEventArgs e)
    {
        UpdateProperties();
        if (e.PropertyName.equals("status")) //$NON-NLS-1$
        {
            UpdateActionAvailability();
        }
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        if (command == getItem().getRunCommand())
        {
            Run();
        }
        else if (command == getItem().getPauseCommand())
        {
            Pause();
        }
        else if (command == getItem().getStopCommand())
        {
            stop();
        }
        else if (command == getItem().getShutdownCommand())
        {
            Shutdown();
        }
        else if (command == getItem().getReturnVmCommand())
        {
            ReturnVm();
        }
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        if (ev.equals(ChangeCDModel.ExecutedEventDefinition))
        {
            ChangeCD(sender, args);
        }
    }

    private void ChangeCD(Object sender, EventArgs args)
    {
        VM entity = (VM) getItem().getEntity();
        ChangeCDModel model = (ChangeCDModel) sender;

        // TODO: Patch!
        String imageName = model.getTitle();
        if (StringHelper.stringsEqual(imageName, "No CDs")) //$NON-NLS-1$
        {
            return;
        }

        Frontend.RunAction(VdcActionType.ChangeDisk,
                new ChangeDiskCommandParameters(entity.getId(), StringHelper.stringsEqual(imageName,
                        ConsoleModel.EjectLabel) ? "" : imageName)); //$NON-NLS-1$
    }

    private void ReturnVm()
    {
        VM entity = (VM) getItem().getEntity();

        Frontend.RunAction(VdcActionType.ShutdownVm, new ShutdownVmParameters(entity.getId(), false),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendActionAsyncResult result) {

                    }
                }, null);
    }

    private void Shutdown()
    {
        VM entity = (VM) getItem().getEntity();
        Frontend.RunAction(VdcActionType.ShutdownVm, new ShutdownVmParameters(entity.getId(), true));
    }

    private void stop()
    {
        VM entity = (VM) getItem().getEntity();
        Frontend.RunAction(VdcActionType.StopVm, new StopVmParameters(entity.getId(), StopVmTypeEnum.NORMAL));
    }

    private void Pause()
    {
        VM entity = (VM) getItem().getEntity();
        Frontend.RunAction(VdcActionType.HibernateVm, new HibernateVmParameters(entity.getId()));
    }

    private void Run()
    {
        VM entity = (VM) getItem().getEntity();
        // use sysprep iff the vm is not initialized and vm has Win OS
        boolean reinitialize = !entity.getis_initialized() && DataProvider.IsWindowsOsType(entity.getvm_os());
        RunVmParams tempVar = new RunVmParams(entity.getId());
        tempVar.setReinitialize(reinitialize);
        Frontend.RunAction(VdcActionType.RunVm, tempVar);
    }

    private void UpdateProperties()
    {
        VM entity = (VM) getItem().getEntity();

        getItem().setName(entity.getvm_name());
        getItem().setDescription(entity.getvm_description());
        getItem().setStatus(entity.getstatus());
        getItem().setIsPool(false);
        getItem().setIsServer(entity.getvm_type() == VmType.Server);
        getItem().setOsType(entity.getvm_os());
        getItem().setIsFromPool(entity.getVmPoolId() != null);
        getItem().setSpiceDriverVersion(entity.getSpiceDriverVersion());

        // Assign PoolType.
        if (entity.getVmPoolId() != null)
        {
            vm_pools pool = getItem().getResolutionService().ResolveVmPoolById(entity.getVmPoolId().getValue());

            // Throw exception. Will help finding bugs in development phase.
            if (pool == null)
            {
                throw new NotImplementedException();
            }

            getItem().setPoolType(pool.getvm_pool_type());
        }

        if (getItem().getDefaultConsole() == null)
        {
            getItem().setDefaultConsole(new SpiceConsoleModel());
        }
        getItem().getDefaultConsole().setEntity(entity);

        // Support RDP console for windows VMs.
        if (DataProvider.IsWindowsOsType(entity.getvm_os()))
        {
            if (getItem().getAdditionalConsole() == null)
            {
                getItem().setAdditionalConsole(new RdpConsoleModel());
            }
            getItem().getAdditionalConsole().setEntity(entity);
            getItem().setHasAdditionalConsole(true);
        }
        else
        {
            getItem().setAdditionalConsole(null);
            getItem().setHasAdditionalConsole(false);
        }
    }

    private void UpdateActionAvailability()
    {
        VM entity = (VM) getItem().getEntity();

        getItem().getTakeVmCommand().setIsAvailable(false);

        ArrayList<VM> entities = new ArrayList<VM>();
        entities.add(entity);

        getItem().getRunCommand().setIsExecutionAllowed(VdcActionUtils.CanExecute(entities,
                VM.class,
                VdcActionType.RunVm));
        getItem().getPauseCommand().setIsExecutionAllowed(VdcActionUtils.CanExecute(entities,
                VM.class,
                VdcActionType.HibernateVm));
        getItem().getShutdownCommand().setIsExecutionAllowed(VdcActionUtils.CanExecute(entities,
                VM.class,
                VdcActionType.ShutdownVm));
        getItem().getStopCommand().setIsExecutionAllowed(VdcActionUtils.CanExecute(entities,
                VM.class,
                VdcActionType.StopVm));

        // Check whether a VM is from the manual pool.
        if (entity.getVmPoolId() != null)
        {
            AsyncDataProvider.GetPoolById(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void OnSuccess(Object target, Object returnValue) {

                            VmItemBehavior behavior = (VmItemBehavior) target;
                            vm_pools pool = (vm_pools) returnValue;
                            boolean isManualPool = pool.getvm_pool_type() == VmPoolType.Manual;
                            behavior.UpdateCommandsAccordingToPoolType(isManualPool);

                        }
                    }), entity.getVmPoolId().getValue());
        }
        else
        {
            UpdateCommandsAccordingToPoolType(true);
        }
    }

    public void UpdateCommandsAccordingToPoolType(boolean isManualPool)
    {
        getItem().getReturnVmCommand().setIsAvailable(!isManualPool);
        getItem().getRunCommand().setIsAvailable(isManualPool);
    }
}
