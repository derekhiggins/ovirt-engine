package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.uicommon.storage.AbstractStorageView;
import org.ovirt.engine.ui.uicommonweb.models.storage.LocalStorageModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LocalStorageView extends AbstractStorageView<LocalStorageModel> {

    interface Driver extends SimpleBeanEditorDriver<LocalStorageModel, LocalStorageView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<Widget, LocalStorageView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<LocalStorageView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @WithElementId
    @Path(value = "path.entity")
    EntityModelTextBoxEditor localPathEditor;

    @UiField
    Label message;

    @Inject
    public LocalStorageView() {
        createPathEditor();
        localPathEditor = pathEditor;
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(ClientGinjectorProvider.instance().getApplicationConstants());
        ViewIdHandler.idHandler.generateAndSetIds(this);
        addStyles();
        Driver.driver.initialize(this);
    }

    void addStyles() {
        localPathEditor.addContentWidgetStyleName(style.localPathContentWidget());
    }

    void localize(ApplicationConstants constants) {
        localPathEditor.setLabel(constants.storagePopupLocalPathLabel());
    }

    @Override
    public void edit(LocalStorageModel object) {
        Driver.driver.edit(object);
    }

    @Override
    public LocalStorageModel flush() {
        return Driver.driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String localPathContentWidget();
    }

    @Override
    public void focus() {
        localPathEditor.setFocus(true);
    }
}
