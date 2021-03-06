package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.gluster.VolumeModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class VolumePopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<VolumeModel, VolumePopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<VolumeModel> {
    }

    private Provider<AddBrickPopupPresenterWidget> popupProvider;

    @Inject
    public VolumePopupPresenterWidget(EventBus eventBus, ViewDef view,
            Provider<AddBrickPopupPresenterWidget> popupProvider) {
        super(eventBus, view);
        this.popupProvider = popupProvider;
    }

    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VolumeModel source,
            UICommand lastExecutedCommand, Model windowModel) {
        if (lastExecutedCommand.equals(source.getAddBricksCommand())) {
            return popupProvider.get();
        } else {
            return super.getModelPopup(source, lastExecutedCommand, windowModel);
        }
    }

}
