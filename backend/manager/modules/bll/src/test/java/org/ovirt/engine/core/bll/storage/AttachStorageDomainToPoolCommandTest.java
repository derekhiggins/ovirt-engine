package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.ovirt.engine.core.bll.ExecuteTransactionAnswer;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.action.StorageDomainPoolParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.storage_pool_iso_map;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.StoragePoolDAO;
import org.ovirt.engine.core.dao.StoragePoolIsoMapDAO;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;

@RunWith(MockitoJUnitRunner.class)
public class AttachStorageDomainToPoolCommandTest {
    @Mock
    private DbFacade dbFacade;
    @Mock
    private StoragePoolIsoMapDAO isoMapDAO;
    @Mock
    private StoragePoolDAO storagePoolDAO;
    @Mock
    private StorageDomainDAO storageDomainDAO;
    @Mock
    private VdsDAO vdsDAO;
    @Mock
    private BackendInternal backendInternal;
    @Mock
    private VDSBrokerFrontend vdsBrokerFrontend;
    @Mock
    private VDS vds;
    storage_pool_iso_map map = null;

    @Test
    public void statusSetInMap() {
        StorageDomainPoolParametersBase params = new StorageDomainPoolParametersBase(Guid.NewGuid(), Guid.NewGuid());
        AttachStorageDomainToPoolCommand<StorageDomainPoolParametersBase> cmd =
                spy(new AttachStorageDomainToPoolCommand<StorageDomainPoolParametersBase>(params));

        doReturn(dbFacade).when(cmd).getDbFacade();

        when(dbFacade.getStoragePoolIsoMapDAO()).thenReturn(isoMapDAO);
        when(dbFacade.getStoragePoolDAO()).thenReturn(storagePoolDAO);
        when(dbFacade.getVdsDAO()).thenReturn(vdsDAO);
        when(dbFacade.getStorageDomainDAO()).thenReturn(storageDomainDAO);
        storage_pool pool = new storage_pool();
        pool.setstatus(StoragePoolStatus.Up);
        when(storagePoolDAO.get(any(Guid.class))).thenReturn(pool);
        when(isoMapDAO.get(any(StoragePoolIsoMapId.class))).thenReturn(map);
        when(storageDomainDAO.getForStoragePool(any(Guid.class), any(Guid.class))).thenReturn(new storage_domains());

        doReturn(backendInternal).when(cmd).getBackend();
        when(backendInternal.runInternalQuery(any(VdcQueryType.class), any(VdcQueryParametersBase.class))).thenReturn
                (new VdcQueryReturnValue());
        when(backendInternal.getResourceManager()).thenReturn(vdsBrokerFrontend);
        VDSReturnValue returnValue = new VDSReturnValue();
        returnValue.setSucceeded(true);
        VdcReturnValueBase vdcReturnValue = new VdcReturnValueBase();
        vdcReturnValue.setSucceeded(true);
        when(backendInternal.runInternalAction(any(VdcActionType.class),
                any(VdcActionParametersBase.class),
                any(CommandContext.class))).thenReturn(vdcReturnValue);
        when(vdsBrokerFrontend.RunVdsCommand(any(VDSCommandType.class), any(VDSParametersBase.class)))
                .thenReturn(returnValue);
        when(vdsDAO.get(any(Guid.class))).thenReturn(vds);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                map = (storage_pool_iso_map) invocation.getArguments()[0];
                return null;
            }
        }).when(isoMapDAO).save(any(storage_pool_iso_map.class));

        doAnswer(new ExecuteTransactionAnswer(0)).when(cmd).executeInNewTransaction(any(TransactionMethod.class));
        doAnswer(new ExecuteTransactionAnswer(1)).when(cmd).executeInScope(any(TransactionScopeOption.class),
                any(TransactionMethod.class));

        cmd.setCompensationContext(mock(CompensationContext.class));
        cmd.executeCommand();
        assertNotNull(map);
        assertEquals(StorageDomainStatus.Maintenance, map.getstatus());
    }
}
