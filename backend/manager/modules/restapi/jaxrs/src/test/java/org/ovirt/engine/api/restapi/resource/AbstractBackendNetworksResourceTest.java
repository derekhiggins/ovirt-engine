package org.ovirt.engine.api.restapi.resource;

import java.util.List;
import java.util.ArrayList;
import javax.ws.rs.WebApplicationException;

import org.junit.Ignore;
import org.junit.Test;

import org.ovirt.engine.api.model.Network;

import static org.easymock.classextension.EasyMock.expect;

public abstract class AbstractBackendNetworksResourceTest
        extends AbstractBackendCollectionResourceTest<Network, org.ovirt.engine.core.common.businessentities.Network, AbstractBackendNetworksResource> {

    public AbstractBackendNetworksResourceTest(AbstractBackendNetworksResource collection) {
        super(collection, null, "");
    }

    @Test
    @Ignore
    public void testQuery() throws Exception {
        // skip test inherited from base class as searching
        // over networks is unsupported by the backend
    }

    @Test
    public void testRemoveBadGuid() throws Exception {
        control.replay();
        try {
            collection.remove("foo");
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    protected void setUpQueryExpectations(String query) throws Exception {
        setUpEntityQueryExpectations(1);
        control.replay();
    }

    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        setUpEntityQueryExpectations(1, failure);
        control.replay();
    }

    protected void setUpEntityQueryExpectations(int times) throws Exception {
        setUpEntityQueryExpectations(times, null);
    }

    protected abstract void setUpEntityQueryExpectations(int times, Object failure) throws Exception;

    protected List<org.ovirt.engine.core.common.businessentities.Network> getEntityList() {
        List<org.ovirt.engine.core.common.businessentities.Network> entities = new ArrayList<org.ovirt.engine.core.common.businessentities.Network>();
        for (int i = 0; i < NAMES.length; i++) {
            entities.add(getEntity(i));
        }
        return entities;
    }

    protected org.ovirt.engine.core.common.businessentities.Network getEntity(int index, boolean isDisplay, boolean isRequired) {
        return setUpEntityExpectations(control.createMock(org.ovirt.engine.core.common.businessentities.Network.class), isDisplay, isRequired, index);
    }

    protected org.ovirt.engine.core.common.businessentities.Network getEntity(int index) {
        return setUpEntityExpectations(control.createMock(org.ovirt.engine.core.common.businessentities.Network.class), false, false, index);
    }

    static org.ovirt.engine.core.common.businessentities.Network setUpEntityExpectations(org.ovirt.engine.core.common.businessentities.Network entity, boolean isDisplay, boolean isRequired, int index) {
        expect(entity.getis_display()).andReturn(isDisplay).anyTimes();
        expect(entity.isRequired()).andReturn(isRequired).anyTimes();
        return setUpEntityExpectations(entity, index);
     }

    static org.ovirt.engine.core.common.businessentities.Network setUpEntityExpectations(org.ovirt.engine.core.common.businessentities.Network entity, int index) {
        expect(entity.getId()).andReturn(GUIDS[index]).anyTimes();
        expect(entity.getname()).andReturn(NAMES[index]).anyTimes();
        expect(entity.getdescription()).andReturn(DESCRIPTIONS[index]).anyTimes();
        expect(entity.getstorage_pool_id()).andReturn(GUIDS[1]).anyTimes();
        return entity;
    }

    protected List<Network> getCollection() {
        return collection.list().getNetworks();
    }

    static Network getModel(int index) {
        Network model = new Network();
        model.setId(GUIDS[0].toString());
        model.setName(NAMES[index]);
        model.setDescription(DESCRIPTIONS[index]);
        return model;
    }

    protected void verifyModel(Network model, int index) {
        super.verifyModel(model, index);
    }
}
