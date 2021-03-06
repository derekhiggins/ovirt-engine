package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;

import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.ISingleAsyncOperation;

public class RefreshPoolSingleAsyncOperationFactory extends ActivateDeactivateSingleAsyncOperationFactory {
    private java.util.ArrayList<Guid> _vdsIdsToSetNonOperational;

    @Override
    public void Initialize(ArrayList parameters) {
        super.Initialize(parameters);
        if (!(parameters.get(3) instanceof java.util.ArrayList)) {
            throw new InvalidOperationException();
        }
        ArrayList l = (ArrayList) parameters.get(3);
        if (!l.isEmpty() && !(l.get(0) instanceof Integer)) {
            throw new InvalidOperationException();
        }
        _vdsIdsToSetNonOperational = (ArrayList<Guid>) parameters.get(3);
    }

    @Override
    public ISingleAsyncOperation CreateSingleAsyncOperation() {
        ISingleAsyncOperation tempVar = new RefreshPoolSingleAsyncOperation(getVdss(), getStorageDomain(),
                getStoragePool(), _vdsIdsToSetNonOperational);
        return tempVar;
    }
}
