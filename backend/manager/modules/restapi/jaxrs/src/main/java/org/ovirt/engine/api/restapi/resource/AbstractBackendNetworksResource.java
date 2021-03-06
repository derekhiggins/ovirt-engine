package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Network;
import org.ovirt.engine.api.model.Networks;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public abstract class AbstractBackendNetworksResource extends AbstractBackendCollectionResource<Network, org.ovirt.engine.core.common.businessentities.Network>
{

    protected VdcQueryType queryType;
    protected VdcActionType addAction;
    protected VdcActionType removeAction;

    public AbstractBackendNetworksResource(VdcQueryType queryType, VdcActionType addAction, VdcActionType removeAction) {
        super(Network.class, org.ovirt.engine.core.common.businessentities.Network.class);
        this.queryType = queryType;
        this.addAction = addAction;
        this.removeAction = removeAction;
    }

    protected abstract VdcQueryParametersBase getQueryParameters();

    protected abstract VdcActionParametersBase getActionParameters(Network network, org.ovirt.engine.core.common.businessentities.Network entity);

    public Networks list() {
        return mapCollection(getBackendCollection(queryType, getQueryParameters()));
    }

    public Response performRemove(String id) {
        org.ovirt.engine.core.common.businessentities.Network entity = lookupNetwork(asGuidOr404(id));
        if (entity == null) {
            notFound();
            return null;
        }
        return performAction(removeAction, getActionParameters(null, entity));
    }

    protected Networks mapCollection(List<org.ovirt.engine.core.common.businessentities.Network> entities) {
        Networks collection = new Networks();
        for (org.ovirt.engine.core.common.businessentities.Network entity : entities) {
            collection.getNetworks().add(addLinks(map(entity)));
        }
        return collection;
    }

    public org.ovirt.engine.core.common.businessentities.Network lookupNetwork(Guid id) {
        return lookupNetwork(id, null);
    }

    public org.ovirt.engine.core.common.businessentities.Network lookupNetwork(Guid id, String name) {
        for (org.ovirt.engine.core.common.businessentities.Network entity : getBackendCollection(queryType, getQueryParameters())) {
            if ((id != null && id.equals(entity.getId())) ||
                (name != null && name.equals(entity.getname()))) {
                return entity;
            }
        }
        return null;
    }

    public org.ovirt.engine.core.common.businessentities.Network lookupNetwork(Guid id, String name, String dataCenterId) {
        for (org.ovirt.engine.core.common.businessentities.Network entity : getBackendCollection(queryType, getQueryParameters())) {
            if ((id != null && id.equals(entity.getId())) ||
                (name != null && name.equals(entity.getname()))
                && (entity.getstorage_pool_id()!=null) && (entity.getstorage_pool_id().toString().equals(dataCenterId))) {
                return entity;
            }
        }
        return null;
    }

    public EntityIdResolver getNetworkIdResolver() {
        return new NetworkIdResolver();
    }

    protected class NetworkIdResolver extends EntityIdResolver {

        protected String name;

        NetworkIdResolver() {}

        NetworkIdResolver(String name) {
            this.name = name;
        }

        @Override
        public org.ovirt.engine.core.common.businessentities.Network lookupEntity(Guid id) throws BackendFailureException {
            return lookupNetwork(id, name);
        }
    }

    protected class DataCenterNetworkIdResolver extends NetworkIdResolver {

        private String dataCenterId;

        DataCenterNetworkIdResolver(String name, String dataCenterId) {
            super(name);
            this.dataCenterId = dataCenterId;
        }

        @Override
        public org.ovirt.engine.core.common.businessentities.Network lookupEntity(Guid id) throws BackendFailureException {
            return lookupNetwork(id, name, dataCenterId);
        }
    }
}
