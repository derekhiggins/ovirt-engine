package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.User;
import org.ovirt.engine.api.model.Users;
import org.ovirt.engine.api.resource.UserResource;
import org.ovirt.engine.api.resource.UsersResource;
import org.ovirt.engine.api.restapi.resource.BackendUsersResourceBase.UserIdResolver;
import org.ovirt.engine.core.common.action.AddUserParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.AdUser;
import org.ovirt.engine.core.common.businessentities.DbUser;
import org.ovirt.engine.core.common.interfaces.SearchType;

public class BackendUsersResource extends BackendUsersResourceBase implements UsersResource {

    public BackendUsersResource() {
        super(User.class, DbUser.class, SUB_COLLECTIONS);
    }

    public BackendUsersResource(String id, BackendDomainResource parent) {
        super(id, parent);
    }

    @Override
    @SingleEntityResource
    public UserResource getUserSubResource(String id) {
        return inject(new BackendUserResource(id, this));
    }

    @Override
    public Users list() {
          return mapDbUserCollection(getBackendCollection(SearchType.DBUser, getSearchPattern()));
    }

    @Override
    public Response add(User user) {
        validateParameters(user, "userName");
        String domain = getDomain(user);
        AdUser adUser = getEntity(AdUser.class,
                                  SearchType.AdUser,
                                  getSearchPattern(user.getUserName(), domain));
        AddUserParameters newUser = new AddUserParameters();
        newUser.setVdcUser(map(adUser));
        return performCreation(VdcActionType.AddUser, newUser, new UserIdResolver(adUser.getUserId()));
    }

}
