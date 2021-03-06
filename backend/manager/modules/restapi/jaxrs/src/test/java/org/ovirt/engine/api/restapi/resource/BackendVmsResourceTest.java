package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.model.CreationStatus;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.Disks;
import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.model.Snapshot;
import org.ovirt.engine.api.model.Snapshots;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.Template;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.model.VmPlacementPolicy;
import org.ovirt.engine.api.restapi.types.DiskMapper;
import org.ovirt.engine.core.common.action.AddVmFromScratchParameters;
import org.ovirt.engine.core.common.action.AddVmFromSnapshotParameters;
import org.ovirt.engine.core.common.action.AddVmFromTemplateParameters;
import org.ovirt.engine.core.common.action.RemoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskImageBase;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetVdsGroupByVdsGroupIdParameters;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.common.queries.GetVmConfigurationBySnapshotQueryParams;
import org.ovirt.engine.core.common.queries.GetVmTemplateParameters;
import org.ovirt.engine.core.common.queries.GetVmTemplatesDisksParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmsResourceTest
        extends AbstractBackendCollectionResourceTest<VM, org.ovirt.engine.core.common.businessentities.VM, BackendVmsResource> {

    private static final String DEFAULT_TEMPLATE_ID = Guid.Empty.toString();

    public BackendVmsResourceTest() {
        super(new BackendVmsResource(), SearchType.VM, "VMs : ");
    }

    @Test
    public void testListIncludeStatistics() throws Exception {
        try {
            accepts.add("application/xml; detail=statistics");
            UriInfo uriInfo = setUpUriExpectations(null);

            org.ovirt.engine.core.common.businessentities.VM vm = new org.ovirt.engine.core.common.businessentities.VM();
            VmStatistics vmStatistics = new VmStatistics();
            vmStatistics.setcpu_sys(0D);
            vmStatistics.setcpu_user(0D);
            vmStatistics.setelapsed_time(0D);
            vmStatistics.setRoundedElapsedTime(0D);
            vmStatistics.setusage_cpu_percent(0);
            vmStatistics.setusage_mem_percent(0);
            vmStatistics.setusage_network_percent(0);
            vm.setStatisticsData(vmStatistics);
            for (int i=0; i<GUIDS.length-1; i++) {
                setUpGetEntityExpectations(VdcQueryType.GetVmByVmId,
                        GetVmByVmIdParameters.class,
                        new String[] { "Id" },
                        new Object[] { GUIDS[i] },
                        vm);
            }
            setUpQueryExpectations("");
            collection.setUriInfo(uriInfo);
            List<VM> vms = getCollection();
            assertTrue(vms.get(0).isSetStatistics());
            verifyCollection(vms);
        } finally {
            accepts.clear();
        }
    }

    @Test
    public void testRemove() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations();
        setUpGetPayloadExpectations(0);
        setUpGetBallooningExpectations();
        setUpActionExpectations(VdcActionType.RemoveVm, RemoveVmParameters.class, new String[] {
                "VmId", "Force" }, new Object[] { GUIDS[0], Boolean.FALSE }, true, true);
        verifyRemove(collection.remove(GUIDS[0].toString()));
    }

    @Test
    public void testRemoveForced() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations();
        setUpGetPayloadExpectations(0);
        setUpGetBallooningExpectations();
        setUpActionExpectations(VdcActionType.RemoveVm, RemoveVmParameters.class, new String[] {
            "VmId", "Force" }, new Object[] { GUIDS[0], Boolean.TRUE }, true, true);
        verifyRemove(collection.remove(GUIDS[0].toString(), new Action(){{setForce(true);}}));
    }

    @Test
    public void testRemoveForcedIncomplete() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations();
        setUpGetPayloadExpectations(0);
        setUpGetBallooningExpectations();
        setUpActionExpectations(VdcActionType.RemoveVm, RemoveVmParameters.class, new String[] {
                                "VmId", "Force" }, new Object[] { GUIDS[0], Boolean.FALSE }, true, true);
        verifyRemove(collection.remove(GUIDS[0].toString(), new Action(){{}}));
    }

    @Test
    public void testRemoveNonExistant() throws Exception{
        setUpGetEntityExpectations(VdcQueryType.GetVmByVmId,
                GetVmByVmIdParameters.class,
                new String[] { "Id" },
                new Object[] { NON_EXISTANT_GUID },
                null);
        control.replay();
        try {
            collection.remove(NON_EXISTANT_GUID.toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(404, wae.getResponse().getStatus());
        }
    }

    private void setUpGetEntityExpectations() throws Exception {
        setUpGetEntityExpectations(VdcQueryType.GetVmByVmId,
                GetVmByVmIdParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                new org.ovirt.engine.core.common.businessentities.VM());
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    protected void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpGetEntityExpectations();
        setUpGetPayloadExpectations(0);
        setUpGetBallooningExpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveVm,
                                           RemoveVmParameters.class,
                                           new String[] { "VmId", "Force" },
                                           new Object[] { GUIDS[0], Boolean.FALSE },
                                           canDo,
                                           success));
        try {
            collection.remove(GUIDS[0].toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddAsyncPending() throws Exception {
        doTestAddAsync(AsyncTaskStatusEnum.init, CreationStatus.PENDING);
    }

    @Test
    public void testAddAsyncInProgress() throws Exception {
        doTestAddAsync(AsyncTaskStatusEnum.running, CreationStatus.IN_PROGRESS);
    }

    @Test
    public void testAddAsyncFinished() throws Exception {
        doTestAddAsync(AsyncTaskStatusEnum.finished, CreationStatus.COMPLETE);
    }

    private void doTestAddAsync(AsyncTaskStatusEnum asyncStatus, CreationStatus creationStatus) throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[1] },
                getVdsGroupEntity());

        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getTemplateEntity(0));
        setUpCreationExpectations(VdcActionType.AddVmFromScratch,
                                  AddVmFromScratchParameters.class,
                                  new String[] { "StorageDomainId"},
                                  new Object[] { Guid.Empty},
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[1]),
                                  asList(new AsyncTaskStatus(asyncStatus)),
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[0] },
                                  getEntity(0));
        VM model = getModel(0);
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[1].toString());
        model.setTemplate(new Template());
        model.getTemplate().setId(DEFAULT_TEMPLATE_ID);

        Response response = collection.add(model);
        assertEquals(202, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM)response.getEntity(), 0);
        VM created = (VM)response.getEntity();
        assertNotNull(created.getCreationStatus());
        assertEquals(creationStatus.value(), created.getCreationStatus().getState());
    }

    @Test
    public void testAddFromScratch() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetVmByVmId,
                                     GetVmByVmIdParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[1] },
                getVdsGroupEntity());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getTemplateEntity(0));

        Disks disks = new Disks();
        disks.getDisks().add(new Disk());
        setUpCreationExpectations(VdcActionType.AddVmFromScratch,
                                  AddVmFromScratchParameters.class,
                                  new String[] { "StorageDomainId", "DiskInfoList" },
                                  new Object[] { Guid.Empty, mapDisks(disks) },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[1]),
                                  asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[0] },
                                  getEntity(0));
        VM model = getModel(0);
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[1].toString());
        model.setTemplate(new Template());
        model.getTemplate().setId(DEFAULT_TEMPLATE_ID);
        model.setDisks(disks);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 0);
        assertNull(((VM)response.getEntity()).getCreationStatus());
    }

    @Test
    public void testAddFromScratchWithStorageDomain() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetVmByVmId,
                                     GetVmByVmIdParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[1] },
                getVdsGroupEntity());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getTemplateEntity(0));
        setUpCreationExpectations(VdcActionType.AddVmFromScratch,
                                  AddVmFromScratchParameters.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[1] },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[1]),
                                  asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[0] },
                                  getEntity(0));
        VM model = getModel(0);
        addStorageDomainToModel(model);
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[1].toString());
        model.setTemplate(new Template());
        model.getTemplate().setId(DEFAULT_TEMPLATE_ID);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 0);
        assertNull(((VM)response.getEntity()).getCreationStatus());
    }

    @Test
    public void testAddFromScratchNamedCluster() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpGetEntityExpectations("Cluster: name=" + NAMES[1],
                                   SearchType.Cluster,
                                   setUpVDSGroup(GUIDS[1]));
        setUpEntityQueryExpectations(VdcQueryType.GetVmByVmId,
                                     GetVmByVmIdParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[1] },
                getVdsGroupEntity());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getTemplateEntity(0));

        setUpCreationExpectations(VdcActionType.AddVmFromScratch,
                                  AddVmFromScratchParameters.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { Guid.Empty },
                                  true,
                                  true,
                                  GUIDS[0],
                                  asList(GUIDS[1]),
                                  asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[0] },
                                  getEntity(0));
        VM model = getModel(0);
        model.setCluster(new Cluster());
        model.getCluster().setName(NAMES[1]);
        model.setTemplate(new Template());
        model.getTemplate().setId(DEFAULT_TEMPLATE_ID);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 0);
    }

    @Test
    public void testAddFromScratchCantDo() throws Exception {
        doTestBadAddFromScratch(false, true, CANT_DO);
    }

    @Test
    public void testAddFromScratchFailure() throws Exception {
        doTestBadAddFromScratch(true, false, FAILURE);
    }

    private void doTestBadAddFromScratch(boolean canDo, boolean success, String detail)
            throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[0] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[1] },
                getVdsGroupEntity());
        setUriInfo(setUpActionExpectations(VdcActionType.AddVmFromScratch,
                                           AddVmFromScratchParameters.class,
                                           new String[] { "StorageDomainId" },
                                           new Object[] { Guid.Empty },
                                           canDo,
                                           success));
        VM model = getModel(0);
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[1].toString());
        model.setTemplate(new Template());
        model.getTemplate().setId(DEFAULT_TEMPLATE_ID);

        try {
            collection.add(model);
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testCloneWithDisk() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpTemplateDisksExpectations(GUIDS[1]);
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUpCreationExpectations(VdcActionType.AddVmFromTemplate,
                                  AddVmFromTemplateParameters.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[0] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));

        Response response = collection.add(createModel(createDisksCollection()));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    @Test
    public void testCloneVmFromSnapshot() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                GetVmTemplateParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[1] },
                getTemplateEntity(0));

        org.ovirt.engine.core.common.businessentities.VM vmConfiguration = getEntity(0);
        Map<Guid, org.ovirt.engine.core.common.businessentities.Disk> diskImageMap = new HashMap<Guid, org.ovirt.engine.core.common.businessentities.Disk>();
        diskImageMap.put(Guid.NewGuid(), new DiskImage());
        expect(vmConfiguration.getDiskMap()).andReturn(diskImageMap).anyTimes();
        VmStatic vmStatic = new VmStatic();
        vmStatic.setId(GUIDS[0]);
        vmStatic.setvm_name(NAMES[0]);
        expect(vmConfiguration.getStaticData()).andReturn(vmStatic).anyTimes();

        setUpEntityQueryExpectations(VdcQueryType.GetVmConfigurationBySnapshot,
                GetVmConfigurationBySnapshotQueryParams.class,
                new String[] { "SnapshotId" },
                new Object[] { GUIDS[1] },
                vmConfiguration);
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUpCreationExpectations(VdcActionType.AddVmFromSnapshot,
                                  AddVmFromSnapshotParameters.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[0] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));

        Response response = collection.add(createModel(createDisksCollection(), createSnapshotsCollection(1)));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    @Test
    public void testClone() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUpCreationExpectations(VdcActionType.AddVmFromTemplate,
                                  AddVmFromTemplateParameters.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[0] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));

        Response response = collection.add(createModel(new Disks(){{setClone(true);}}));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    @Test
    public void testAdd() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                                    GetVdsGroupByVdsGroupIdParameters.class,
                                    new String[] { "VdsGroupId" },
                                    new Object[] { GUIDS[2] },
                                    getVdsGroupEntity());
        setUpCreationExpectations(VdcActionType.AddVm,
                                  VmManagementParametersBase.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[0] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));
        Response response = collection.add(createModel(null));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    @Test
    public void testAddWithPlacementPolicy() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpGetEntityExpectations("Hosts: name=" + NAMES[1],
                SearchType.VDS,
                getHost());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUpCreationExpectations(VdcActionType.AddVm,
                                  VmManagementParametersBase.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[0] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));

        VM model = createModel(null);
        model.setPlacementPolicy(new VmPlacementPolicy());
        model.getPlacementPolicy().setHost(new Host());
        model.getPlacementPolicy().getHost().setName(NAMES[1]);
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    private VDS getHost() {
        VDS vds = new VDS();
        vds.setId(GUIDS[2]);
        return vds;
    }

    @Test
    public void testAddWithStorageDomain() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUpCreationExpectations(VdcActionType.AddVm,
                                  VmManagementParametersBase.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[1] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));

        VM model = createModel(null);
        addStorageDomainToModel(model);
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    @Test
    public void testAddNamedCluster() throws Exception {
        setUriInfo(setUpBasicUriExpectations());

        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUpGetEntityExpectations("Cluster: name=" + NAMES[2],
                                   SearchType.Cluster,
                                   setUpVDSGroup(GUIDS[2]));

        setUpCreationExpectations(VdcActionType.AddVm,
                                  VmManagementParametersBase.class,
                                  new String[] { "StorageDomainId" },
                                  new Object[] { GUIDS[0] },
                                  true,
                                  true,
                                  GUIDS[2],
                                  VdcQueryType.GetVmByVmId,
                                  GetVmByVmIdParameters.class,
                                  new String[] { "Id" },
                                  new Object[] { GUIDS[2] },
                                  getEntity(2));

        VM model = getModel(2);
        model.setTemplate(new Template());
        model.getTemplate().setId(GUIDS[1].toString());
        model.setCluster(new Cluster());
        model.getCluster().setName(NAMES[2]);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof VM);
        verifyModel((VM) response.getEntity(), 2);
    }

    @Test
    public void testAddCantDo() throws Exception {
        doTestBadAdd(false, true, CANT_DO);
    }

    @Test
    public void testAddFailed() throws Exception {
        doTestBadAdd(true, false, FAILURE);
    }

    @Test
    @Override
    public void testList() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(null);

        setUpQueryExpectations("");
        collection.setUriInfo(uriInfo);
        verifyCollection(getCollection());
    }

    @Test
    @Override
    public void testQuery() throws Exception {
        UriInfo uriInfo = setUpUriExpectations(QUERY);

        setUpQueryExpectations(QUERY);
        collection.setUriInfo(uriInfo);
        verifyCollection(getCollection());
    }

    private void doTestBadAdd(boolean canDo, boolean success, String detail)
            throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplate,
                                     GetVmTemplateParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { GUIDS[1] },
                                     getTemplateEntity(0));
        setUpEntityQueryExpectations(VdcQueryType.GetVdsGroupByVdsGroupId,
                GetVdsGroupByVdsGroupIdParameters.class,
                new String[] { "VdsGroupId" },
                new Object[] { GUIDS[2] },
                getVdsGroupEntity());
        setUriInfo(setUpActionExpectations(VdcActionType.AddVm,
                                           VmManagementParametersBase.class,
                                           new String[] { "StorageDomainId" },
                                           new Object[] { GUIDS[0] },
                                           canDo,
                                           success));

        try {
            collection.add(createModel(null));
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddIncompleteParameters() throws Exception {
        VM model = new VM();
        model.setName(NAMES[0]);
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(model);
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "VM", "add", "template.id|name", "cluster.id|name");
        }
    }

    private void setUpTemplateDisksExpectations(Guid templateId) {
        setUpEntityQueryExpectations(VdcQueryType.GetVmTemplatesDisks,
                                     GetVmTemplatesDisksParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { templateId },
                                     createDiskList());
    }

    @SuppressWarnings("serial")
    private List<DiskImage> createDiskList() {
        return new ArrayList<DiskImage>(){{
                                            add(new DiskImage(){{setId(GUIDS[0]);}});
                                         }};
    }

    static org.ovirt.engine.core.common.businessentities.VM setUpEntityExpectations(
            org.ovirt.engine.core.common.businessentities.VM entity, VmStatistics statistics, int index) {
        expect(entity.getId()).andReturn(GUIDS[index]).anyTimes();
        expect(entity.getvds_group_id()).andReturn(GUIDS[2]).anyTimes();
        expect(entity.getvm_name()).andReturn(NAMES[index]).anyTimes();
        expect(entity.getvm_description()).andReturn(DESCRIPTIONS[index]).anyTimes();
        expect(entity.getnum_of_cpus()).andReturn(8).anyTimes();
        expect(entity.getnum_of_sockets()).andReturn(2).anyTimes();
        expect(entity.getusage_mem_percent()).andReturn(Integer.valueOf(20)).anyTimes();
        expect(entity.getdisplay_type()).andReturn(DisplayType.vnc).anyTimes();
        expect(entity.getdisplay_secure_port()).andReturn(5900).anyTimes();
        expect(entity.getnum_of_monitors()).andReturn(2).anyTimes();
        expect(entity.getvm_type()).andReturn(VmType.Server).anyTimes();
        expect(entity.getrun_on_vds_name()).andReturn(NAMES[NAMES.length -1]).anyTimes();
        setUpStatisticalEntityExpectations(entity, statistics);
        return entity;
    }

    static org.ovirt.engine.core.common.businessentities.VmTemplate setUpEntityExpectations(
            org.ovirt.engine.core.common.businessentities.VmTemplate entity, int index) {
        expect(entity.getId()).andReturn(GUIDS[index]).anyTimes();
        expect(entity.getvds_group_id()).andReturn(GUIDS[2]).anyTimes();
        expect(entity.getname()).andReturn(NAMES[index]).anyTimes();
        expect(entity.getdescription()).andReturn(DESCRIPTIONS[index]).anyTimes();
        expect(entity.getnum_of_cpus()).andReturn(8).anyTimes();
        expect(entity.getnum_of_sockets()).andReturn(2).anyTimes();
        expect(entity.getdefault_display_type()).andReturn(DisplayType.vnc).anyTimes();
        expect(entity.getnum_of_monitors()).andReturn(2).anyTimes();
        expect(entity.getvm_type()).andReturn(VmType.Server).anyTimes();
        return entity;
    }

    static org.ovirt.engine.core.common.businessentities.VM setUpStatisticalEntityExpectations(
            org.ovirt.engine.core.common.businessentities.VM entity, VmStatistics statistics) {
        expect(entity.getmem_size_mb()).andReturn(10).anyTimes();
        expect(entity.getStatisticsData()).andReturn(statistics).anyTimes();
        expect(statistics.getusage_mem_percent()).andReturn(20).anyTimes();
        expect(statistics.getcpu_user()).andReturn(Double.valueOf(30L)).anyTimes();
        expect(statistics.getcpu_sys()).andReturn(Double.valueOf(40L)).anyTimes();
        expect(statistics.getusage_cpu_percent()).andReturn(50).anyTimes();
        return entity;
    }

    static VM getModel(int index) {
        VM model = new VM();
        model.setName(NAMES[index]);
        model.setDescription(DESCRIPTIONS[index]);
        model.setId(GUIDS[0].toString());
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[2].toString());
        return model;
    }

    protected List<VM> getCollection() {
        return collection.list().getVMs();
    }

    protected void verifyModel(VM model, int index) {
        super.verifyModel(model, index);
        verifyModelSpecific(model, index);
    }

    static void verifyModelSpecific(VM model, int index) {
        assertNotNull(model.getCluster());
        assertNotNull(model.getCluster().getId());
        assertNotNull(model.getCpu());
        assertNotNull(model.getCpu().getTopology());
        assertEquals(4, model.getCpu().getTopology().getCores().intValue());
        assertEquals(2, model.getCpu().getTopology().getSockets().intValue());
    }

    private VM createModel(Disks disks) {
        VM model = getModel(2);

        model.setTemplate(new Template());
        model.getTemplate().setId(GUIDS[1].toString());
        model.setCluster(new Cluster());
        model.getCluster().setId(GUIDS[2].toString());
        if(disks != null){
            model.setDisks(disks);
        }

        return model;
    }

    private VM createModel(Disks disks, Snapshots snapshots) {
        VM model = createModel(disks);
        if (snapshots != null) {
            model.setSnapshots(snapshots);
        }
        return model;
    }

    private void addStorageDomainToModel(VM model) {
        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setId(GUIDS[1].toString());
        model.setStorageDomain(storageDomain);
    }

    protected org.ovirt.engine.core.common.businessentities.VM getEntity(int index) {
        return setUpEntityExpectations(
                control.createMock(org.ovirt.engine.core.common.businessentities.VM.class),
                control.createMock(VmStatistics.class),
                index);
    }

    protected org.ovirt.engine.core.common.businessentities.VmTemplate getTemplateEntity(int index) {
        return setUpEntityExpectations(
                control.createMock(org.ovirt.engine.core.common.businessentities.VmTemplate.class),
                index);
    }

    protected org.ovirt.engine.core.common.businessentities.VDSGroup getVdsGroupEntity() {
        return new VDSGroup();
    }

    private Disks createDisksCollection() {
        Disks disks = new Disks();
        disks.setClone(true);
        disks.getDisks().add(map(createDiskList().get(0), null));
        return disks;
    }

    private Snapshots createSnapshotsCollection(int index) {
        Snapshots snapshots = new Snapshots();
        snapshots.getSnapshots().add(map(createSnapshot(index), null));
        return snapshots;
    }

    private org.ovirt.engine.core.common.businessentities.Snapshot createSnapshot(int index) {
        org.ovirt.engine.core.common.businessentities.Snapshot result =
                new org.ovirt.engine.core.common.businessentities.Snapshot();
        result.setId(GUIDS[index]);
        result.setDescription("snap1");
        return result;
    }

    private Disk map(DiskImage entity, Disk template) {
        return getMapper(org.ovirt.engine.core.common.businessentities.Disk.class, Disk.class).map(entity, template);
    }

    private Snapshot map(org.ovirt.engine.core.common.businessentities.Snapshot entity, Snapshot template) {
        return getMapper(org.ovirt.engine.core.common.businessentities.Snapshot.class, Snapshot.class).map(entity,
                template);
    }

    private ArrayList<DiskImageBase> mapDisks(Disks disks) {
        ArrayList<DiskImageBase> diskImages = null;
        if (disks!=null && disks.isSetDisks()) {
            diskImages = new ArrayList<DiskImageBase>();
            for (Disk disk : disks.getDisks()) {
                DiskImage diskImage = (DiskImage)DiskMapper.map(disk, null);
                diskImages.add(diskImage);
            }
        }
        return diskImages;
    }

    protected void setUpGetPayloadExpectations(int index) throws Exception {
        VmPayload payload = new VmPayload();
        setUpGetEntityExpectations(VdcQueryType.GetVmPayload,
                                   GetVmByVmIdParameters.class,
                                   new String[] { "Id" },
                                   new Object[] { GUIDS[index] },
                                   payload);
    }

    private void setUpGetBallooningExpectations() throws Exception {
        setUpGetEntityExpectations(VdcQueryType.IsBalloonEnabled,
                GetVmByVmIdParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                true);
    }
}
