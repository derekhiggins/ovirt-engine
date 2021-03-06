package org.ovirt.engine.ui.common.widget.uicommon.template;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateDiskListModel;

import com.google.gwt.event.shared.EventBus;

public class TemplateDiskListModelTable<T extends TemplateDiskListModel> extends AbstractModelBoundTableWidget<DiskImage, T> {

    public TemplateDiskListModelTable(
            SearchableTableModelProvider<DiskImage, T> modelProvider,
            EventBus eventBus, ClientStorage clientStorage, CommonApplicationConstants constants) {
        super(modelProvider, eventBus, clientStorage, false);
    }

    @Override
    public void initTable(CommonApplicationConstants constants) {
        TextColumnWithTooltip<DiskImage> nameColumn = new TextColumnWithTooltip<DiskImage>() {
            @Override
            public String getValue(DiskImage object) {
                return object.getDiskAlias(); //$NON-NLS-1$
            }
        };
        getTable().addColumn(nameColumn, constants.nameDisk());

        TextColumnWithTooltip<DiskImage> sizeColumn = new TextColumnWithTooltip<DiskImage>() {
            @Override
            public String getValue(DiskImage object) {
                return String.valueOf(object.getSizeInGigabytes()) + " GB"; //$NON-NLS-1$
            }
        };
        getTable().addColumn(sizeColumn, constants.sizeDisk());

        TextColumnWithTooltip<DiskImage> formatColumn = new EnumColumn<DiskImage, VolumeFormat>() {
            @Override
            protected VolumeFormat getRawValue(DiskImage object) {
                return object.getvolume_format();
            }
        };
        getTable().addColumn(formatColumn, constants.formatDisk());

        TextColumnWithTooltip<DiskImage> allocationColumn = new EnumColumn<DiskImage, VolumeType>() {
            @Override
            protected VolumeType getRawValue(DiskImage object) {
                return VolumeType.forValue(object.getvolume_type().getValue());
            }
        };
        getTable().addColumn(allocationColumn, constants.allocationDisk());

        TextColumnWithTooltip<DiskImage> interfaceColumn = new EnumColumn<DiskImage, DiskInterface>() {
            @Override
            protected DiskInterface getRawValue(DiskImage object) {
                return object.getDiskInterface();
            }
        };
        getTable().addColumn(interfaceColumn, constants.interfaceDisk());
    }

}
