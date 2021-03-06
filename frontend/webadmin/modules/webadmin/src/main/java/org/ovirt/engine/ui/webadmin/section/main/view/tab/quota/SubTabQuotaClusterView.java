package org.ovirt.engine.ui.webadmin.section.main.view.tab.quota;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.quota.SubTabQuotaClusterPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;

import com.google.gwt.core.client.GWT;

public class SubTabQuotaClusterView extends AbstractSubTabTableView<Quota, QuotaVdsGroup, QuotaListModel, QuotaClusterListModel>
        implements SubTabQuotaClusterPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabQuotaClusterView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabQuotaClusterView(SearchableDetailModelProvider<QuotaVdsGroup, QuotaListModel, QuotaClusterListModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    private void initTable(final ApplicationConstants constants) {
        getTable().addColumn(new TextColumnWithTooltip<QuotaVdsGroup>() {
            @Override
            public String getValue(QuotaVdsGroup object) {
                return object.getVdsGroupName() == null || object.getVdsGroupName() == "" ? constants.ultQuotaForAllClustersQuotaPopup()
                        : object.getVdsGroupName();
            }
        },
                constants.nameCluster());

        getTable().addColumn(new TextColumnWithTooltip<QuotaVdsGroup>() {
            @Override
            public String getValue(QuotaVdsGroup object) {
                return (object.getMemSizeMBUsage() == null ? "0" : object.getMemSizeMBUsage().toString()) + constants.outOfQuota() //$NON-NLS-1$
                        + (object.getMemSizeMB() == -1 ? constants.unlimitedQuota() : object.getMemSizeMB()
                                .toString()) + " MB"; //$NON-NLS-1$
            }
        },
                constants.usedMemoryTotalCluster());

        getTable().addColumn(new TextColumnWithTooltip<QuotaVdsGroup>() {
            @Override
            public String getValue(QuotaVdsGroup object) {
                return (object.getVirtualCpuUsage() == null ? "0" : object.getVirtualCpuUsage().toString()) + constants.outOfQuota() //$NON-NLS-1$
                        + (object.getVirtualCpu() == -1 ? constants.unlimitedQuota() : object.getVirtualCpu()
                                .toString());
            }
        },
                constants.runningCpuTotalCluster());
    }

}
