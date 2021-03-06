package org.ovirt.engine.ui.webadmin.section.main.view.tab.gluster;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.gluster.VolumeParameterListModel;
import org.ovirt.engine.ui.uicommonweb.models.volumes.VolumeListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.gluster.SubTabVolumeParameterPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

import com.google.inject.Inject;

public class SubTabVolumeParameterView extends AbstractSubTabTableView<GlusterVolumeEntity, GlusterVolumeOptionEntity, VolumeListModel, VolumeParameterListModel> implements SubTabVolumeParameterPresenter.ViewDef {

    @Inject
    public SubTabVolumeParameterView(SearchableDetailModelProvider<GlusterVolumeOptionEntity, VolumeListModel, VolumeParameterListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        TextColumnWithTooltip<GlusterVolumeOptionEntity> optionKeyColumn = new TextColumnWithTooltip<GlusterVolumeOptionEntity>() {
            @Override
            public String getValue(GlusterVolumeOptionEntity option) {
                return option.getKey();
            }
        };
        getTable().addColumn(optionKeyColumn, constants.optionKeyVolumeParameter());
        TextColumnWithTooltip<GlusterVolumeOptionEntity> optionValueColumn =
                new TextColumnWithTooltip<GlusterVolumeOptionEntity>() {
                    @Override
                    public String getValue(GlusterVolumeOptionEntity option) {
                        return option.getValue();
                    }
                };
        getTable().addColumn(optionValueColumn, constants.optionValueVolumeParameter());

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.addVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getAddParameterCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.editVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getEditParameterCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.resetVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getResetParameterCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<GlusterVolumeOptionEntity>(constants.resetAllVolumeParameter()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getResetAllParameterCommand();
            }
        });
    }

}
