package org.ovirt.engine.ui.common.widget.table.column;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.uicompat.EnumTranslator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;

/**
 * Column for rendering {@link ImageResource} instances using {@link StyledImageResourceCell}.
 *
 * @param <T>
 *            Table row data type.
 */
public abstract class ImageResourceColumn<T> extends Column<T, ImageResource> {

    private static final CommonApplicationResources RESOURCES = GWT.create(CommonApplicationResources.class);
    private static final CommonApplicationConstants CONSTANTS = GWT.create(CommonApplicationConstants.class);


    public ImageResourceColumn() {
        super(new StyledImageResourceCell());
    }

    public void setTitle(String title) {
        ((StyledImageResourceCell) getCell()).setTitle(title);
    }

    public void setEnumTitle(Enum<?> enumObj) {
        setTitle(EnumTranslator.createAndTranslate(enumObj));
    }

    protected CommonApplicationResources getCommonResources() {
        return RESOURCES;
    }

    protected CommonApplicationConstants getCommonConstants() {
        return CONSTANTS;
    }

}
