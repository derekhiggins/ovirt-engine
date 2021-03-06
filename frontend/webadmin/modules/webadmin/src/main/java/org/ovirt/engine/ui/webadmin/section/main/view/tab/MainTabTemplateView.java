package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import java.util.Date;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabTemplatePresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.GeneralDateTimeColumn;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class MainTabTemplateView extends AbstractMainTabWithDetailsTableView<VmTemplate, TemplateListModel> implements MainTabTemplatePresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainTabTemplateView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public MainTabTemplateView(MainModelProvider<VmTemplate, TemplateListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        TextColumnWithTooltip<VmTemplate> nameColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return object.getname();
            }
        };
        getTable().addColumn(nameColumn, constants.namePool());

        TextColumnWithTooltip<VmTemplate> domainColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return object.getdomain();
            }
        };
        getTable().addColumn(domainColumn, constants.domainTemplate());

        TextColumnWithTooltip<VmTemplate> creationDateColumn = new GeneralDateTimeColumn<VmTemplate>() {
            @Override
            protected Date getRawValue(VmTemplate object) {
                return object.getcreation_date();
            }
        };
        getTable().addColumn(creationDateColumn, constants.creationDateTemplate());

        TextColumnWithTooltip<VmTemplate> statusColumn = new EnumColumn<VmTemplate, VmTemplateStatus>() {
            @Override
            protected VmTemplateStatus getRawValue(VmTemplate object) {
                return object.getstatus();
            }
        };
        getTable().addColumn(statusColumn, constants.statusTemplate());

        TextColumnWithTooltip<VmTemplate> clusterColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return object.getvds_group_name();
            }
        };
        getTable().addColumn(clusterColumn, constants.clusterTemplate());

        TextColumnWithTooltip<VmTemplate> dcColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return object.getstorage_pool_name();
            }
        };
        getTable().addColumn(dcColumn, constants.dcTemplate());

        TextColumnWithTooltip<VmTemplate> descriptionColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                return object.getdescription();
            }
        };
        getTable().addColumn(descriptionColumn, constants.descriptionTemplate());

        getTable().addActionButton(new WebAdminButtonDefinition<VmTemplate>(constants.editTemplate()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEditCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VmTemplate>(constants.removeTemplate()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VmTemplate>(constants.exportTemplate()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getExportCommand();
            }
        });
    }

}
