package org.ovirt.engine.ui.common.widget.uicommon.storage;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.common.widget.table.column.LunSelectionColumn;
import org.ovirt.engine.ui.common.widget.table.column.LunTextColumn;
import org.ovirt.engine.ui.common.widget.table.column.ScrollableTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.LunModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.SanStorageModelBase;
import org.ovirt.engine.ui.uicommonweb.models.storage.SanTargetModel;
import org.ovirt.engine.ui.uicompat.EnumTranslator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.ValueBoxBase.TextAlignment;

public class SanStorageTargetToLunList extends AbstractSanStorageList<SanTargetModel, ListModel> {

    protected int treeScrollPosition;

    public SanStorageTargetToLunList(SanStorageModelBase model) {
        super(model);
    }

    public SanStorageTargetToLunList(SanStorageModelBase model, boolean hideLeaf) {
        super(model, hideLeaf, false);
    }

    public SanStorageTargetToLunList(SanStorageModelBase model, boolean hideLeaf, boolean multiSelection) {
        super(model, hideLeaf, multiSelection);
    }

    @Override
    protected void createSanStorageListWidget() {
        super.createSanStorageListWidget();

    }

    @Override
    protected ListModel getLeafModel(SanTargetModel rootModel) {
        return rootModel.getLunsList();
    }

    @Override
    protected void createHeaderWidget() {
        EntityModelCellTable<ListModel> table = new EntityModelCellTable<ListModel>(false,
                (Resources) GWT.create(SanStorageListHeaderResources.class),
                true);

        // Add first blank column
        table.addColumn(new TextColumn<SanTargetModel>() {
            @Override
            public String getValue(SanTargetModel model) {
                return ""; //$NON-NLS-1$
            }
        }, constants.empty(), "20px"); //$NON-NLS-1$

        table.addColumn(new ScrollableTextColumn<SanTargetModel>() {
            @Override
            public String getValue(SanTargetModel model) {
                return model.getName();
            }
        }, constants.targetNameSanStorage());

        table.addColumn(new ScrollableTextColumn<SanTargetModel>() {
            @Override
            public String getValue(SanTargetModel model) {
                return model.getAddress();
            }
        }, constants.addressSanStorage(), "95px"); //$NON-NLS-1$

        table.addColumn(new ScrollableTextColumn<SanTargetModel>() {
            @Override
            public String getValue(SanTargetModel model) {
                return model.getPort();
            }
        }, constants.portSanStorage(), "65px"); //$NON-NLS-1$

        table.setWidth("100%", true); //$NON-NLS-1$

        // Add last blank column
        table.addColumn(new TextColumn<SanTargetModel>() {
            @Override
            public String getValue(SanTargetModel model) {
                return ""; //$NON-NLS-1$
            }
        }, constants.empty(), "80px"); //$NON-NLS-1$

        // Add blank item list
        table.setRowData(new ArrayList<EntityModel>());

        // Add table as header widget
        treeHeader.add(table);
    }

    private void addLoginButton(HorizontalPanel panel, SanTargetModel rootModel) {
        final UiCommandButton loginButton = new UiCommandButton();
        loginButton.setCommand(rootModel.getLoginCommand());
        loginButton.setLabel(constants.storageIscsiPopupLoginButtonLabel());
        loginButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                treeScrollPosition = treeContainer.getVerticalScrollPosition();
                loginButton.getCommand().Execute();
            }
        });
        loginButton.setWidth("55px"); //$NON-NLS-1$

        panel.add(loginButton);
        panel.setCellWidth(loginButton, "60px"); //$NON-NLS-1$
        panel.setCellHorizontalAlignment(loginButton, HasHorizontalAlignment.ALIGN_RIGHT);
    }

    private void additemToRootNodePanel(HorizontalPanel panel,
            TextBoxLabel item,
            String text,
            String width,
            TextAlignment align) {
        item.getElement().getStyle().setBackgroundColor("transparent"); //$NON-NLS-1$
        item.getElement().getStyle().setColor("black"); //$NON-NLS-1$
        item.setAlignment(align);
        item.setText(text);

        panel.add(item);
        panel.setCellWidth(item, width);
    }

    @Override
    protected TreeItem createRootNode(SanTargetModel rootModel) {
        HorizontalPanel panel = new HorizontalPanel();

        additemToRootNodePanel(panel, new TextBoxLabel(), rootModel.getName(), "310px", TextAlignment.LEFT); //$NON-NLS-1$
        additemToRootNodePanel(panel, new TextBoxLabel(), rootModel.getAddress(), "80px", TextAlignment.CENTER); //$NON-NLS-1$
        additemToRootNodePanel(panel, new TextBoxLabel(), rootModel.getPort(), "45px", TextAlignment.CENTER); //$NON-NLS-1$
        addLoginButton(panel, rootModel);

        panel.setSpacing(1);
        panel.setWidth("100%"); //$NON-NLS-1$

        return new TreeItem(panel);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected TreeItem createLeafNode(final ListModel leafModel) {
        TreeItem item = new TreeItem();
        List<LunModel> items = (List<LunModel>) leafModel.getItems();

        if (hideLeaf || items.isEmpty()) {
            item.setUserObject(Boolean.TRUE);
            return item;
        }

        if (!multiSelection) {
            leafModel.getSelectedItemChangedEvent().addListener(new IEventListener() {
                @Override
                public void eventRaised(Event ev, Object sender, EventArgs args) {
                    LunModel lunModel = (LunModel) leafModel.getSelectedItem();
                    if (leafModel.getSelectedItem() != null) {
                        // Clear current selection
                        for (SanTargetModel item : (List<SanTargetModel>) model.getItems()) {
                            if (item.getLunsList() != leafModel) {
                                item.getLunsList().setSelectedItem(null);
                            }
                        }

                        if (model.isIgnoreGrayedOut()) {
                            updateSelectedLunWarning(lunModel);
                        }
                    }
                }
            });
        }

        EntityModelCellTable<ListModel> table =
                new EntityModelCellTable<ListModel>(multiSelection,
                        (Resources) GWT.create(SanStorageListLunTableResources.class));

        LunSelectionColumn lunSelectionColumn = new LunSelectionColumn(multiSelection) {
            @Override
            public LunModel getValue(LunModel object) {
                return object;
            }
        };

        table.setCustomSelectionColumn(lunSelectionColumn, "30px"); //$NON-NLS-1$

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return model.getLunId();
            }
        }, constants.lunIdSanStorage());

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return String.valueOf(model.getSize()) + "GB"; //$NON-NLS-1$
            }
        }, constants.devSizeSanStorage(), "60px"); //$NON-NLS-1$

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return String.valueOf(model.getMultipathing());
            }
        }, constants.pathSanStorage(), "40px"); //$NON-NLS-1$

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return EnumTranslator.createAndTranslate(model.getStatus());
            }
        }, constants.statusSanStorage(), "60px"); //$NON-NLS-1$

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return model.getVendorId();
            }
        }, constants.vendorIdSanStorage());

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return model.getProductId();
            }
        }, constants.productIdSanStorage());

        table.addColumn(new LunTextColumn() {
            @Override
            public String getRawValue(LunModel model) {
                return model.getSerial();
            }
        }, constants.serialSanStorage(), "90px"); //$NON-NLS-1$

        table.setRowData(items == null ? new ArrayList<LunModel>() : items);
        table.edit(leafModel);

        ScrollPanel panel = new ScrollPanel();
        panel.add(table);
        item.setWidget(panel);

        // Display LUNs as grayed-out if needed
        for (LunModel lunModel : items) {
            if (lunModel.getIsGrayedOut()) {
                grayOutItem(lunModel.getGrayedOutReasons(), lunModel, table);
            }
        }

        return item;
    }

    @Override
    protected void updateItems() {
        super.updateItems();

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                treeContainer.setVerticalScrollPosition(treeScrollPosition);
            }
        });
    }
}
