package org.ovirt.engine.ui.userportal.system;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.auth.CurrentUser;
import org.ovirt.engine.ui.common.system.BaseApplicationInit;
import org.ovirt.engine.ui.common.uicommon.FrontendEventsHandlerImpl;
import org.ovirt.engine.ui.common.uicommon.FrontendFailureEventListener;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.ITypeResolver;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalLoginModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.auth.CurrentUserRole;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelInitEvent;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ApplicationInit extends BaseApplicationInit<UserPortalLoginModel> {

    private final CurrentUserRole userRole;

    @Inject
    public ApplicationInit(ITypeResolver typeResolver,
            FrontendEventsHandlerImpl frontendEventsHandler,
            FrontendFailureEventListener frontendFailureEventListener,
            CurrentUser user,
            Provider<UserPortalLoginModel> loginModelProvider,
            EventBus eventBus,
            CurrentUserRole userRole,
            ApplicationConstants constants) {
        super(typeResolver, frontendEventsHandler, frontendFailureEventListener, user, loginModelProvider, eventBus);
        this.userRole = userRole;
        Window.setTitle(constants.applicationTitle());
    }

    @Override
    protected void beforeUiCommonInitEvent(UserPortalLoginModel loginModel) {
        UserPortalModelInitEvent.fire(eventBus);
    }

    @Override
    protected boolean filterFrontendQueries() {
        return true;
    }

    @Override
    protected void initLoginModel() {
        super.initLoginModel();

        final UserPortalLoginModel loginModel = getLoginModel();

        // Login model "IsENGINEUser" property determines the availability
        // of the "Extended" main tab and starts the actual login operation
        loginModel.getIsENGINEUser().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                Boolean isEngineUser = (Boolean) loginModel.getIsENGINEUser().getEntity();

                if (isEngineUser != null) {
                    userRole.setEngineUser(isEngineUser);

                    // Proceed with login operation
                    performLogin(loginModel);
                }
            }
        });
    }

    @Override
    protected void onLogin(UserPortalLoginModel loginModel) {
        // Instead of performing login now, request update for "IsENGINEUser" property
        loginModel.UpdateIsENGINEUser(loginModel.getLoggedUser());
    }

    @Override
    public void onLogout() {
        AsyncQuery query = new AsyncQuery();
        query.setHandleFailure(true);
        query.setModel(this);
        query.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object ReturnValue) {
                Frontend.setLoggedInUser(null);
                getLoginModel().resetAfterLogout();
                AsyncDataProvider.clearCache();
                ApplicationInit.super.onLogout();
            }
        };

        Frontend.LogoffAsync(Frontend.getLoggedInUser(), query);
    }

}
