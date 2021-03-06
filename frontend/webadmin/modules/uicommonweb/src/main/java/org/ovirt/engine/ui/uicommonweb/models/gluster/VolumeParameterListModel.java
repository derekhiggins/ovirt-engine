package org.ovirt.engine.ui.uicommonweb.models.gluster;

import java.util.ArrayList;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeOptionParameters;
import org.ovirt.engine.core.common.action.gluster.ResetGlusterVolumeOptionsParameters;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionInfo;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;

public class VolumeParameterListModel extends SearchableListModel {

    private UICommand addParameterCommand;
    private UICommand editParameterCommand;
    private UICommand resetParameterCommand;
    private UICommand resetAllParameterCommand;

    public UICommand getAddParameterCommand() {
        return addParameterCommand;
    }

    public void setAddParameterCommand(UICommand command) {
        this.addParameterCommand = command;
    }

    public UICommand getEditParameterCommand() {
        return editParameterCommand;
    }

    public void setEditParameterCommand(UICommand command) {
        this.editParameterCommand = command;
    }

    public void setResetParameterCommand(UICommand command) {
        this.resetParameterCommand = command;
    }

    public UICommand getResetParameterCommand() {
        return resetParameterCommand;
    }

    public void setResetAllParameterCommand(UICommand command) {
        this.resetAllParameterCommand = command;
    }

    public UICommand getResetAllParameterCommand() {
        return resetAllParameterCommand;
    }

    @Override
    protected String getListName() {
        // TODO Auto-generated method stub
        return "VolumeParameterListModel"; //$NON-NLS-1$
    }

    public VolumeParameterListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().parameterTitle());
        setHashName("parameters"); //$NON-NLS-1$
        setAddParameterCommand(new UICommand(ConstantsManager.getInstance().getConstants().AddVolume(), this));
        setEditParameterCommand(new UICommand(ConstantsManager.getInstance().getConstants().editVolume(), this));
        setResetParameterCommand(new UICommand(ConstantsManager.getInstance().getConstants().resetVolume(), this));
        setResetAllParameterCommand(new UICommand(ConstantsManager.getInstance().getConstants().resetAllVolume(), this));
    }

    @Override
    protected void OnSelectedItemChanged()
    {
        super.OnSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void SelectedItemsChanged()
    {
        super.SelectedItemsChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability()
    {
        getEditParameterCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() == 1);
        getResetParameterCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() == 1);
    }

    private void addParameter() {
        if (getWindow() != null) {
            return;
        }

        GlusterVolumeEntity volume = (GlusterVolumeEntity) getEntity();
        if (volume == null)
        {
            return;
        }

        VolumeParameterModel volumeParameterModel = new VolumeParameterModel();
        volumeParameterModel.setTitle(ConstantsManager.getInstance().getConstants().addOptionVolume());
        setWindow(volumeParameterModel);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                VolumeParameterListModel volumeParameterListModel = (VolumeParameterListModel) model;
                VolumeParameterModel innerParameterModel = (VolumeParameterModel) getWindow();

                ArrayList<GlusterVolumeOptionInfo> optionInfoList = (ArrayList<GlusterVolumeOptionInfo>) result;
                innerParameterModel.getKeyList().setItems(optionInfoList);

                UICommand command = new UICommand("OnSetParameter", volumeParameterListModel); //$NON-NLS-1$
                command.setTitle(ConstantsManager.getInstance().getConstants().ok());
                command.setIsDefault(true);
                innerParameterModel.getCommands().add(command);
                command = new UICommand("OnCancel", volumeParameterListModel); //$NON-NLS-1$
                command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                command.setIsCancel(true);
                innerParameterModel.getCommands().add(command);
            }
        };
        AsyncDataProvider.GetGlusterVolumeOptionInfoList(_asyncQuery, volume.getClusterId());
    }

    private void onSetParameter() {
        if (getEntity() == null) {
            return;
        }

        GlusterVolumeEntity volume = (GlusterVolumeEntity) getEntity();

        VolumeParameterModel model = (VolumeParameterModel) getWindow();

        if (!model.Validate())
        {
            return;
        }

        GlusterVolumeOptionEntity option = new GlusterVolumeOptionEntity();
        option.setVolumeId(volume.getId());
        option.setKey(((GlusterVolumeOptionInfo) model.getKeyList().getSelectedItem()).getKey());
        option.setValue((String) model.getValue().getEntity());

        model.StartProgress(null);

        Frontend.RunAction(VdcActionType.SetGlusterVolumeOption,
                new GlusterVolumeOptionParameters(option),
                new IFrontendActionAsyncCallback() {

                    @Override
                    public void Executed(FrontendActionAsyncResult result) {
                        VolumeParameterListModel localModel = (VolumeParameterListModel) result.getState();
                        localModel.postOnSetParameter(result.getReturnValue());
                    }
                }, this);
    }

    public void postOnSetParameter(VdcReturnValueBase returnValue)
    {
        VolumeParameterModel model = (VolumeParameterModel) getWindow();

        model.StopProgress();

        if (returnValue != null && returnValue.getSucceeded())
        {
            cancel();
        }
    }

    private void cancel() {
        setWindow(null);
    }

    private void editParameter() {
        if (getWindow() != null) {
            return;
        }

        GlusterVolumeEntity volume = (GlusterVolumeEntity) getEntity();
        if (volume == null)
        {
            return;
        }

        VolumeParameterModel volumeParameterModel = new VolumeParameterModel();
        volumeParameterModel.setTitle(ConstantsManager.getInstance().getConstants().editOptionVolume());
        setWindow(volumeParameterModel);

        volumeParameterModel.getKeyList().setIsChangable(false);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                VolumeParameterListModel volumeParameterListModel = (VolumeParameterListModel) model;
                VolumeParameterModel innerParameterModel = (VolumeParameterModel) getWindow();

                ArrayList<GlusterVolumeOptionInfo> optionInfoList = (ArrayList<GlusterVolumeOptionInfo>) result;
                innerParameterModel.getKeyList().setItems(optionInfoList);

                GlusterVolumeOptionEntity selectedOption = (GlusterVolumeOptionEntity) getSelectedItem();

                for (GlusterVolumeOptionInfo option : optionInfoList) {
                    if (option.getKey().equals(selectedOption.getKey()))
                    {
                        innerParameterModel.getKeyList().setSelectedItem(option);
                        break;
                    }
                }

                innerParameterModel.getValue().setEntity(selectedOption.getValue());

                UICommand command = new UICommand("OnSetParameter", volumeParameterListModel); //$NON-NLS-1$
                command.setTitle(ConstantsManager.getInstance().getConstants().ok());
                command.setIsDefault(true);
                innerParameterModel.getCommands().add(command);
                command = new UICommand("OnCancel", volumeParameterListModel); //$NON-NLS-1$
                command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                command.setIsCancel(true);
                innerParameterModel.getCommands().add(command);
            }
        };
        AsyncDataProvider.GetGlusterVolumeOptionInfoList(_asyncQuery, volume.getClusterId());
    }

    private void resetParameter() {
        if (getWindow() != null)
        {
            return;
        }

        if (getSelectedItem() == null)
        {
            return;
        }
        GlusterVolumeOptionEntity selectedOption = (GlusterVolumeOptionEntity) getSelectedItem();

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().resetOptionVolumeTitle());
        model.setHashName("reset_option"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().resetOptionVolumeMsg());

        ArrayList<String> list = new ArrayList<String>();
        list.add(selectedOption.getKey());
        model.setItems(list);

        UICommand okCommand = new UICommand("OnResetParameter", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        model.getCommands().add(okCommand);
        UICommand cancelCommand = new UICommand("OnCancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.getCommands().add(cancelCommand);
    }

    private void onResetParameter() {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (getSelectedItem() == null)
        {
            return;
        }
        GlusterVolumeOptionEntity selectedOption = (GlusterVolumeOptionEntity) getSelectedItem();

        ResetGlusterVolumeOptionsParameters parameters =
                new ResetGlusterVolumeOptionsParameters(selectedOption.getVolumeId(), selectedOption.getKey(), false);

        model.StartProgress(null);

        Frontend.RunAction(VdcActionType.ResetGlusterVolumeOptions,
                parameters,
                new IFrontendActionAsyncCallback() {

                    @Override
                    public void Executed(FrontendActionAsyncResult result) {
                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.StopProgress();
                        cancel();
                    }
                }, model);
    }

    private void resetAllParameters() {
        if (getWindow() != null)
        {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().resetAllOptionsTitle());
        model.setHashName("reset_all_options"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().resetAllOptionsMsg());

        UICommand okCommand = new UICommand("OnResetAllParameters", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        model.getCommands().add(okCommand);
        UICommand cancelCommand = new UICommand("OnCancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        model.getCommands().add(cancelCommand);
    }

    private void onResetAllParameters() {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (getEntity() == null)
        {
            return;
        }
        GlusterVolumeEntity volume = (GlusterVolumeEntity) getEntity();

        ResetGlusterVolumeOptionsParameters parameters =
                new ResetGlusterVolumeOptionsParameters(volume.getId(), null, false);

        model.StartProgress(null);

        Frontend.RunAction(VdcActionType.ResetGlusterVolumeOptions,
                parameters,
                new IFrontendActionAsyncCallback() {

                    @Override
                    public void Executed(FrontendActionAsyncResult result) {
                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.StopProgress();
                        cancel();
                    }
                }, model);
    }

    @Override
    protected void OnEntityChanged() {
        if (getEntity() == null) {
            return;
        }
        super.OnEntityChanged();
        GlusterVolumeEntity glusterVolumeEntity = (GlusterVolumeEntity) getEntity();
        ArrayList<GlusterVolumeOptionEntity> list = new ArrayList<GlusterVolumeOptionEntity>();
        for (GlusterVolumeOptionEntity glusterVolumeOption : glusterVolumeEntity.getOptions()) {
            list.add(glusterVolumeOption);
        }
        setItems(list);
    }

    @Override
    public void ExecuteCommand(UICommand command) {
        super.ExecuteCommand(command);
        if (command.equals(getAddParameterCommand())) {
            addParameter();
        }
        else if (command.getName().equals("OnSetParameter")) { //$NON-NLS-1$
            onSetParameter();
        }
        else if (command.getName().equals("OnCancel")) { //$NON-NLS-1$
            cancel();
        }
        else if (command.equals(getEditParameterCommand())) {
            editParameter();
        }
        else if (command.equals(getResetParameterCommand())) {
            resetParameter();
        }
        else if (command.getName().equals("OnResetParameter")) { //$NON-NLS-1$
            onResetParameter();
        }
        else if (command.equals(getResetAllParameterCommand())) {
            resetAllParameters();
        }
        else if (command.getName().equals("OnResetAllParameters")) { //$NON-NLS-1$
            onResetAllParameters();
        }
    }
}
