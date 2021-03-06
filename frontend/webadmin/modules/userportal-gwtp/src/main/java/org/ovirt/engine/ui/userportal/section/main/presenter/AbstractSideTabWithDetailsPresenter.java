package org.ovirt.engine.ui.userportal.section.main.presenter;

import java.util.List;

import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.table.OrderedMultiSelectionModel;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.userportal.place.ApplicationPlaces;
import org.ovirt.engine.ui.userportal.section.main.presenter.tab.MainTabExtendedPresenter;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;

/**
 * Base class for side tab presenters that work with {@link ListWithDetailsModel}.
 *
 * @param <T>
 *            Table row data type.
 * @param <M>
 *            Model type.
 * @param <V>
 *            View type.
 * @param <P>
 *            Proxy type.
 */
public abstract class AbstractSideTabWithDetailsPresenter<T, M extends ListWithDetailsModel, V extends AbstractSideTabWithDetailsPresenter.ViewDef<T>, P extends Proxy<?>>
        extends AbstractModelActivationPresenter<T, M, V, P> {

    public interface ViewDef<T> extends View {

        /**
         * Controls the sub tab panel visibility.
         */
        void setSubTabPanelVisible(boolean subTabPanelVisible);

        /**
         * Returns the selection model used by the side tab table widget.
         */
        OrderedMultiSelectionModel<T> getTableSelectionModel();

    }

    private final PlaceManager placeManager;

    public AbstractSideTabWithDetailsPresenter(EventBus eventBus, V view, P proxy,
            PlaceManager placeManager, SearchableTableModelProvider<T, M> modelProvider) {
        super(eventBus, view, proxy, modelProvider);
        this.placeManager = placeManager;
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainTabExtendedPresenter.TYPE_SetTabContent, this);
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(getView().getTableSelectionModel()
                .addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
                    @Override
                    public void onSelectionChange(SelectionChangeEvent event) {
                        // Update model selection
                        modelProvider.setSelectedItems(getSelectedItems());

                        // Let others know that the table selection has changed
                        fireTableSelectionChangeEvent();

                        // Update the layout
                        updateLayout();

                        // Reveal the appropriate place based on selection
                        if (hasSelection()) {
                            placeManager.revealPlace(getSubTabRequest());
                        } else {
                            placeManager.revealPlace(getSideTabRequest());
                        }
                    }
                }));
    }

    @Override
    protected void onReveal() {
        super.onReveal();

        if (hasSelection()) {
            clearSelection();
        } else {
            updateLayout();
        }
    }

    /**
     * Subclasses should fire an event to indicate that the table selection has changed.
     */
    protected abstract void fireTableSelectionChangeEvent();

    void updateLayout() {
        getView().setSubTabPanelVisible(hasSelection());
    }

    /**
     * Returns the place request associated with this side tab presenter.
     */
    protected abstract PlaceRequest getSideTabRequest();

    private PlaceRequest getSubTabRequest() {
        return new PlaceRequest(createRequestToken());
    }

    protected String createRequestToken() {
        String subTabName = modelProvider.getModel().getActiveDetailModel().getTitle().toLowerCase().replace(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
        String requestToken = getSideTabRequest().getNameToken() + ApplicationPlaces.SUB_TAB_PREFIX + subTabName;
        return requestToken;
    }

    /**
     * Returns items currently selected in the table.
     */
    protected List<T> getSelectedItems() {
        return getView().getTableSelectionModel().getSelectedList();
    }

    /**
     * Returns {@code true} when there is at least one item selected in the table, {@code false} otherwise.
     */
    protected boolean hasSelection() {
        return !getSelectedItems().isEmpty();
    }

    /**
     * Deselects any selected values in the table.
     */
    protected void clearSelection() {
        getView().getTableSelectionModel().clear();
    }

}
