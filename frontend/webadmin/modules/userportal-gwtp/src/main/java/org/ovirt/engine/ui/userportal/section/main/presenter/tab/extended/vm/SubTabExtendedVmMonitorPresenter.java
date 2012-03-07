package org.ovirt.engine.ui.userportal.section.main.presenter.tab.extended.vm;

import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmMonitorModel;
import org.ovirt.engine.ui.userportal.gin.ClientGinjector;
import org.ovirt.engine.ui.userportal.place.ApplicationPlaces;
import org.ovirt.engine.ui.userportal.uicommon.model.vm.VmMonitorModelProvider;
import org.ovirt.engine.ui.userportal.uicommon.model.vm.VmMonitorValueChangeEvent;
import org.ovirt.engine.ui.userportal.uicommon.model.vm.VmMonitorValueChangeEvent.VmMonitorValueChangeHandler;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.TabDataBasic;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabExtendedVmMonitorPresenter
        extends AbstractSubTabExtendedVmPresenter<VmMonitorModel, SubTabExtendedVmMonitorPresenter.ViewDef, SubTabExtendedVmMonitorPresenter.ProxyDef>
        implements VmMonitorValueChangeHandler {

    @ProxyCodeSplit
    @NameToken(ApplicationPlaces.extendedVirtualMachineMonitorSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabExtendedVmMonitorPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<UserPortalItemModel> {

        void update();

    }

    @TabInfo(container = ExtendedVmSubTabPanelPresenter.class)
    static TabData getTabData(ClientGinjector ginjector) {
        return new TabDataBasic(ginjector.getApplicationConstants().extendedVirtualMachineMonitorSubTabLabel(), 7);
    }

    @Inject
    public SubTabExtendedVmMonitorPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, VmMonitorModelProvider modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider);
    }

    @Override
    protected void onDetailModelEntityChange(Object entity) {
        getView().update();
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(getEventBus().addHandler(VmMonitorValueChangeEvent.getType(), this));
    }

    @Override
    public void onVmMonitorValueChange(VmMonitorValueChangeEvent event) {
        getView().update();
    }

}