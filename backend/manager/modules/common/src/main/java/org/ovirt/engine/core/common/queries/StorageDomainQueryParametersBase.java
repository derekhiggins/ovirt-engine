package org.ovirt.engine.core.common.queries;

import org.ovirt.engine.core.compat.*;

public class StorageDomainQueryParametersBase extends VdcQueryParametersBase {
    private static final long serialVersionUID = -1267869804833489615L;

    private Guid privateStorageDomainId = new Guid();

    public Guid getStorageDomainId() {
        return privateStorageDomainId;
    }

    private void setStorageDomainId(Guid value) {
        privateStorageDomainId = value;
    }

    public StorageDomainQueryParametersBase(Guid storageDomainId) {
        setStorageDomainId(storageDomainId);
    }

    public StorageDomainQueryParametersBase() {
    }
}
