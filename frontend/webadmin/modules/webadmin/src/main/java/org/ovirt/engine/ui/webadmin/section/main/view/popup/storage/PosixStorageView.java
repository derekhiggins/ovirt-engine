package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.uicommon.storage.AbstractStorageView;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.PosixStorageModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ValueBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PosixStorageView extends AbstractStorageView<PosixStorageModel> {

    interface Driver extends SimpleBeanEditorDriver<PosixStorageModel, PosixStorageView> {

        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<Widget, PosixStorageView> {

        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<PosixStorageView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @WithElementId
    @Path(value = "path.entity")
    EntityModelTextBoxOnlyEditor pathEditor;

    @UiField
    @Ignore
    Label pathLabel;

    @UiField
    @Ignore
    Label pathHintLabel;

    @UiField
    @WithElementId
    @Path(value = "vfsType.entity")
    EntityModelTextBoxOnlyEditor vfsTypeEditor;

    @UiField
    @Ignore
    Label vfsTypeLabel;

    @UiField
    @WithElementId
    @Path(value = "mountOptions.entity")
    EntityModelTextBoxOnlyEditor mountOptionsEditor;

    @UiField
    @Ignore
    Label mountOptionsLabel;

    @UiField
    Label message;


    @Inject
    public PosixStorageView() {
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(ClientGinjectorProvider.instance().getApplicationConstants());
        ViewIdHandler.idHandler.generateAndSetIds(this);
        addStyles();
        Driver.driver.initialize(this);
    }

    void addStyles() {
        pathEditor.addContentWidgetStyleName(style.pathEditorContent());
    }

    void localize(ApplicationConstants constants) {

        pathLabel.setText(constants.storagePopupPosixPathLabel());
        pathHintLabel.setText(constants.storagePopupPosixPathHintLabel());
        vfsTypeLabel.setText(constants.storagePopupVfsTypeLabel());
        mountOptionsLabel.setText(constants.storagePopupMountOptionsLabel());
    }

    @Override
    public void edit(PosixStorageModel object) {
        Driver.driver.edit(object);

        pathHintLabel.setVisible(object.getPath().getIsAvailable() && object.getPath().getIsChangable());

        StyleTextBoxEditor(pathEditor, object.getPath());
        StyleTextBoxEditor(vfsTypeEditor, object.getVfsType());
        StyleTextBoxEditor(mountOptionsEditor, object.getMountOptions());
    }

    /*
    Makes a provided editor look like label (enabled, read-only textbox).
     */
    private void StyleTextBoxEditor(EntityModelTextBoxOnlyEditor editor, EntityModel model) {

        if (!model.getIsChangable()) {

            editor.setEnabled(true);

            ValueBox<Object> valueBox = editor.asValueBox();
            valueBox.setReadOnly(true);
            valueBox.getElement().getStyle().setBorderWidth(0, Style.Unit.PX);
        }
    }

    @Override
    public PosixStorageModel flush() {
        return Driver.driver.flush();
    }

    interface WidgetStyle extends CssResource {

        String pathEditorContent();
    }

    @Override
    public void focus() {
        pathEditor.setFocus(true);
    }
}
