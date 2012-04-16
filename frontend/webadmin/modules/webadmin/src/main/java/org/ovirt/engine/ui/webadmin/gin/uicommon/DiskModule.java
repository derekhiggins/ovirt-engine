package org.ovirt.engine.ui.webadmin.gin.uicommon;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.DetailTabModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.MainTabModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailTabModelProvider;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskTemplateListModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskVmListModel;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjector;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.DisksAllocationPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmDiskRemovePopupPresenterWidget;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DiskModule extends AbstractGinModule {

    // Main List Model

    @Provides
    @Singleton
    public MainModelProvider<DiskImage, DiskListModel> getDiskListProvider(ClientGinjector ginjector,
            final Provider<VmDiskRemovePopupPresenterWidget> removeConfirmPopupProvider,
            final Provider<DisksAllocationPopupPresenterWidget> moveorCopyPopupProvider) {
        return new MainTabModelProvider<DiskImage, DiskListModel>(ginjector, DiskListModel.class) {
            @Override
            protected AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(UICommand lastExecutedCommand) {
                DiskListModel model = getModel();

                if (lastExecutedCommand == getModel().getMoveCommand()
                        || lastExecutedCommand == getModel().getCopyCommand()) {
                    return moveorCopyPopupProvider.get();
                } else {
                    return super.getModelPopup(lastExecutedCommand);
                }
            }

            @Override
            protected AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(UICommand lastExecutedCommand) {
                if (lastExecutedCommand == getModel().getRemoveCommand()) {
                    return removeConfirmPopupProvider.get();
                } else {
                    return super.getConfirmModelPopup(lastExecutedCommand);
                }
            }
        };
    }

    // Form Detail Models

    @Provides
    @Singleton
    public DetailModelProvider<DiskListModel, DiskGeneralModel> getDiskGeneralProvider(ClientGinjector ginjector) {
        return new DetailTabModelProvider<DiskListModel, DiskGeneralModel>(ginjector,
                DiskListModel.class,
                DiskGeneralModel.class);
    }

    // Searchable Detail Models

    @Provides
    @Singleton
    public SearchableDetailModelProvider<VM, DiskListModel, DiskVmListModel> getDiskVmModelProvider(ClientGinjector ginjector) {
        return new SearchableDetailTabModelProvider<VM, DiskListModel, DiskVmListModel>(ginjector,
                DiskListModel.class,
                DiskVmListModel.class);
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<VmTemplate, DiskListModel, DiskTemplateListModel> getDiskTemplateModelProvider(ClientGinjector ginjector) {
        return new SearchableDetailTabModelProvider<VmTemplate, DiskListModel, DiskTemplateListModel>(ginjector,
                DiskListModel.class,
                DiskTemplateListModel.class);
    }

    @Override
    protected void configure() {
    }

}