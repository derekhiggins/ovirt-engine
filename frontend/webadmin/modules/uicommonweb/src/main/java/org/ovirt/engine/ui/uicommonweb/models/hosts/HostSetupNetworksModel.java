package org.ovirt.engine.ui.uicommonweb.models.hosts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network;
import org.ovirt.engine.core.common.queries.GetVdsByVdsIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.EventDefinition;
import org.ovirt.engine.core.compat.KeyValuePairCompat;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.BaseCommandTarget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.BondNetworkInterfaceModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.LogicalNetworkModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkCommand;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkInterfaceModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkItemModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkOperation;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkOperationFactory;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.NetworkOperationFactory.OperationMap;
import org.ovirt.engine.ui.uicommonweb.models.hosts.network.OperationCadidateEventArgs;

/**
 * A Model for the Setup Networks Dialog<BR>
 * The Entity is the VDS being edited.<BR>
 * The Dialog holds two different Lists: NIC Models, and Network Models.<BR>
 * These two Lists are fetched from the backend, and cannot be changed by the User.<BR>
 * The user only changes the topology of their connections.
 */
public class HostSetupNetworksModel extends EntityModel {

    private static final EventDefinition NICS_CHANGED_EVENT_DEFINITION = new EventDefinition("NicsChanged",
            HostSetupNetworksModel.class);
    private static final EventDefinition NETWORKS_CHANGED_EVENT_DEFINITION = new EventDefinition("NetworksChanged",
            HostSetupNetworksModel.class);

    private static final EventDefinition OPERATION_CANDIDATE_EVENT_DEFINITION =
            new EventDefinition("OperationCandidate", NetworkOperationFactory.class);

    private Event privateOperationCandidateEvent;

    private Event privateNicsChangedEvent;

    private Event privateNetworksChangedEvent;

    private final UICommand okCommand;

    private final UICommand cancelCommand;

    private List<VdsNetworkInterface> allNics;

    private Map<String, NetworkInterfaceModel> nicMap;

    private Map<String, LogicalNetworkModel> networkMap;

    private NetworkOperationFactory operationFactory;
    private List<network> allNetworks;
    private final HostInterfaceListModel hostInterfaceListModel;
    private List<VdsNetworkInterface> allBonds;
    private NetworkOperation currentCandidate;
    private NetworkItemModel<?> currentOp1;
    private NetworkItemModel<?> currentOp2;

    public HostSetupNetworksModel(HostInterfaceListModel hostInterfaceListModel) {
        this.hostInterfaceListModel = hostInterfaceListModel;
        setNicsChangedEvent(new Event(NICS_CHANGED_EVENT_DEFINITION));
        setNetworksChangedEvent(new Event(NETWORKS_CHANGED_EVENT_DEFINITION));
        setOperationCandidateEvent(new Event(OPERATION_CANDIDATE_EVENT_DEFINITION));

        // ok command
        okCommand = new UICommand("OnSetupNetworks", this);
        okCommand.setTitle("OK");
        okCommand.setIsDefault(true);

        // cancel command
        cancelCommand = new UICommand("Cancel", this);
        cancelCommand.setTitle("Cancel");
        cancelCommand.setIsCancel(true);

    }

    public boolean candidateOperation(String op1Key, String op2Key, boolean drop) {
        NetworkInterfaceModel nic1 = nicMap.get(op1Key);
        LogicalNetworkModel network1 = networkMap.get(op1Key);

        NetworkInterfaceModel nic2 = nicMap.get(op2Key);
        LogicalNetworkModel network2 = networkMap.get(op2Key);

        NetworkItemModel<?> op1 = nic1 == null ? network1 : nic1;
        NetworkItemModel<?> op2 = nic2 == null ? network2 : nic2;

        if (op1 == null || (op1 == null && op2 == null)) {
            throw new IllegalArgumentException("null Operands");
        }

        NetworkOperation candidate = NetworkOperationFactory.operationFor(op1, op2);

        if (drop) {
            onOperation(candidate, candidate.getCommand(op1, op2, allNics));
        } else {
            // raise the candidate event only if it was changed
            if (!candidate.equals(currentCandidate) || !equals(op1, currentOp1) || !equals(op2, currentOp2)) {
                currentCandidate = candidate;
                currentOp1 = op1;
                currentOp2 = op2;
                getOperationCandidateEvent().raise(this, new OperationCadidateEventArgs(candidate, op1, op2));
            }
        }
        return candidate != NetworkOperation.NULL_OPERATION;
    }

    public OperationMap commandsFor(NetworkItemModel<?> item) {
        return operationFactory.commandsFor(item, allNics);
    }

    public List<VdsNetworkInterface> getAllNics() {
        return allNics;
    }

    @Override
    public UICommand getCancelCommand() {
        return cancelCommand;
    }

    public List<LogicalNetworkModel> getNetworks() {
        return new ArrayList<LogicalNetworkModel>(networkMap.values());
    }

    public Event getNetworksChangedEvent() {
        return privateNetworksChangedEvent;
    }

    public List<NetworkInterfaceModel> getNics() {
        return new ArrayList<NetworkInterfaceModel>(nicMap.values());
    }

    public Event getNicsChangedEvent() {
        return privateNicsChangedEvent;
    }

    public UICommand getOkCommand() {
        return okCommand;
    }

    public Event getOperationCandidateEvent() {
        return privateOperationCandidateEvent;
    }

    public void onEdit(NetworkItemModel<?> item) {
        assert item instanceof NetworkInterfaceModel : "only nics can be edited";
        NetworkInterfaceModel nic = (NetworkInterfaceModel) item;
        assert nic.getItems().size() > 0 : "must have at least one network to edit";
        final VdsNetworkInterface entity = nic.getEntity();
        Model editPopup;
        BaseCommandTarget okTarget;
        if (nic instanceof BondNetworkInterfaceModel) {
            /*****************
             * Bond Dialog
             *****************/
            BondNetworkInterfaceModel bondModel = (BondNetworkInterfaceModel) nic;
            editPopup = new HostBondInterfaceModel(true);
            final HostBondInterfaceModel bondDialogModel = (HostBondInterfaceModel) editPopup;
            bondDialogModel.setTitle("Edit Bond Interface " + entity.getName());
            bondDialogModel.getBondingOptions().setIsAvailable(true);
            bondDialogModel.getBond().setIsAvailable(false);

            // this dialog has only one selected network, just put the first one
            bondDialogModel.getNetwork().setSelectedItem(nic.getItems().get(0).getEntity());
            bondDialogModel.setBootProtocol(entity.getBootProtocol());

            // bond options
            String bondOptions = entity.getBondOptions();
            List<KeyValuePairCompat<String, EntityModel>> items =
                    (List<KeyValuePairCompat<String, EntityModel>>) bondDialogModel.getBondingOptions().getItems();
            boolean found = false;
            KeyValuePairCompat<String, EntityModel> customKey = null;
            for (KeyValuePairCompat<String, EntityModel> pair : items) {
                String key = pair.getKey();
                if (key.equals(bondOptions)) {
                    bondDialogModel.getBondingOptions().setSelectedItem(pair);
                    found = true;
                    break;
                } else {
                    if ("custom".equals(key)) {
                        customKey = pair;
                    }
                }
            }
            if (!found) {
                EntityModel value = new EntityModel();
                value.setEntity(bondOptions);
                customKey.setValue(value);
                bondDialogModel.getBondingOptions().setSelectedItem(customKey);
            }

            // Addresses
            bondDialogModel.getAddress().setEntity(entity.getAddress());
            bondDialogModel.getSubnet().setEntity(entity.getSubnet());
            bondDialogModel.getGateway().setEntity(entity.getGateway());
            bondDialogModel.getNetwork().setIsAvailable(false);
            bondDialogModel.setNoneBootProtocolAvailable(!entity.getIsManagement());

            // OK Target
            okTarget = new BaseCommandTarget() {
                @Override
                public void ExecuteCommand(UICommand command) {
                    if (!bondDialogModel.Validate()) {
                        return;
                    }
                    entity.setAddress((String) bondDialogModel.getAddress().getEntity());
                    entity.setSubnet((String) bondDialogModel.getSubnet().getEntity());
                    entity.setGateway((String) bondDialogModel.getGateway().getEntity());
                    entity.setBootProtocol(bondDialogModel.getBootProtocol());
                    setBondOptions(entity, bondDialogModel);
                    hostInterfaceListModel.CancelConfirm();
                }
            };
        } else if (entity.getIsManagement()) {
            /*****************
             * Management Network Dialog
             *****************/
            editPopup = new HostManagementNetworkModel(true);
            final HostManagementNetworkModel mgmntDialogModel = (HostManagementNetworkModel) editPopup;
            mgmntDialogModel.setTitle("Edit Management Interface " + entity.getName());
            mgmntDialogModel.getAddress().setEntity(entity.getAddress());
            mgmntDialogModel.getSubnet().setEntity(entity.getSubnet());
            mgmntDialogModel.getGateway().setEntity(entity.getGateway());
            mgmntDialogModel.setNoneBootProtocolAvailable(false);
            mgmntDialogModel.getBondingOptions().setIsAvailable(false);
            mgmntDialogModel.getInterface().setIsAvailable(false);
            mgmntDialogModel.setBootProtocol(entity.getBootProtocol());

            // OK Target
            okTarget = new BaseCommandTarget() {
                @Override
                public void ExecuteCommand(UICommand command) {
                    if (!mgmntDialogModel.Validate()) {
                        return;
                    }
                    entity.setBootProtocol(mgmntDialogModel.getBootProtocol());
                    entity.setAddress((String) mgmntDialogModel.getAddress().getEntity());
                    entity.setSubnet((String) mgmntDialogModel.getSubnet().getEntity());
                    entity.setGateway((String) mgmntDialogModel.getGateway().getEntity());
                    hostInterfaceListModel.CancelConfirm();
                }
            };
        } else {
            /*****************
             * Nic Dialog
             *****************/
            editPopup = new HostInterfaceModel(true);
            final HostInterfaceModel interfaceDialogModel = (HostInterfaceModel) editPopup;
            interfaceDialogModel.setTitle("Edit Interface " + entity.getName());
            interfaceDialogModel.getAddress().setEntity(entity.getAddress());
            interfaceDialogModel.getSubnet().setEntity(entity.getSubnet());
            interfaceDialogModel.getName().setIsAvailable(false);
            interfaceDialogModel.getNetwork().setIsAvailable(false);
            // this dialog has only one selected network, just put the first one
            interfaceDialogModel.getNetwork().setSelectedItem(nic.getItems().get(0).getEntity());
            interfaceDialogModel.setBootProtocol(entity.getBootProtocol());

            // OK Target
            okTarget = new BaseCommandTarget() {
                @Override
                public void ExecuteCommand(UICommand command) {
                    if (!interfaceDialogModel.Validate()) {
                        return;
                    }
                    entity.setBootProtocol(interfaceDialogModel.getBootProtocol());
                    entity.setAddress((String) interfaceDialogModel.getAddress().getEntity());
                    entity.setSubnet((String) interfaceDialogModel.getSubnet().getEntity());
                    hostInterfaceListModel.CancelConfirm();
                }
            };
        }

        // ok command
        UICommand okCommand = new UICommand("OK", okTarget);
        okCommand.setTitle("OK");
        okCommand.setIsDefault(true);

        // cancel command
        UICommand cancelCommand = new UICommand("Cancel", new BaseCommandTarget() {
            @Override
            public void ExecuteCommand(UICommand command) {
                hostInterfaceListModel.CancelConfirm();
            }
        });
        cancelCommand.setTitle("Cancel");
        cancelCommand.setIsCancel(true);

        editPopup.getCommands().add(okCommand);
        editPopup.getCommands().add(cancelCommand);
        hostInterfaceListModel.setConfirmWindow(editPopup);
    }

    public void onOperation(NetworkOperation operation, final NetworkCommand networkCommand) {
        Model popupWindow;

        UICommand cancelCommand = new UICommand("Cancel", new BaseCommandTarget() {
            @Override
            public void ExecuteCommand(UICommand command) {
                hostInterfaceListModel.CancelConfirm();
            }
        });
        cancelCommand.setTitle("cancel");
        cancelCommand.setIsCancel(true);

        if (operation == NetworkOperation.NULL_OPERATION) {
            return;
        } else if (operation == NetworkOperation.BOND_WITH) {
            final HostBondInterfaceModel bondPopup = new HostBondInterfaceModel(true);
            bondPopup.setTitle("Create New Bond");
            bondPopup.getNetwork().setIsAvailable(false);
            bondPopup.getCheckConnectivity().setIsAvailable(false);
            bondPopup.setBootProtocol(NetworkBootProtocol.None);
            bondPopup.getAddress().setIsAvailable(false);
            bondPopup.getSubnet().setIsAvailable(false);
            bondPopup.getGateway().setIsAvailable(false);
            List<VdsNetworkInterface> freeBonds = getFreeBonds();
            if (freeBonds.isEmpty()) {
                popupWindow = new ConfirmationModel();
                popupWindow.setTitle("Error");
                popupWindow.setMessage("There are no available Bonds");
                popupWindow.getCommands().add(cancelCommand);
                hostInterfaceListModel.setConfirmWindow(popupWindow);
                return;
            }
            bondPopup.getBond().setItems(freeBonds);
            bondPopup.setBootProtocolAvailable(false);
            bondPopup.getCommands().add(new UICommand("OK", new BaseCommandTarget() {

                @Override
                public void ExecuteCommand(UICommand command) {
                    hostInterfaceListModel.CancelConfirm();
                    VdsNetworkInterface bond = (VdsNetworkInterface) bondPopup.getBond().getSelectedItem();
                    setBondOptions(bond, bondPopup);
                    networkCommand.Execute(bond);
                    redraw();
                }
            }));

            popupWindow = bondPopup;
        } else {
            // just execute the command
            networkCommand.Execute();
            redraw();
            return;
        }

        // add cancel
        popupWindow.getCommands().add(cancelCommand);

        // set window
        hostInterfaceListModel.setConfirmWindow(popupWindow);
    }

    public void redraw() {
        initAllModels(false);
    }

    @Override
    protected void OnEntityChanged() {
        super.OnEntityChanged();
        initAllModels(true);
    }

    protected void onNicsChanged() {
        operationFactory = new NetworkOperationFactory(getNetworks(), getNics());
        queryFreeBonds();
        validate();
        getNetworksChangedEvent().raise(this, EventArgs.Empty);
    }

    private LogicalNetworkModel createErrorNetworkModel(String networkName, Integer vlanId) {
        network errorNetwork = new network();
        errorNetwork.setname(networkName);
        errorNetwork.setvlan_id(vlanId);
        LogicalNetworkModel networkModel = new LogicalNetworkModel(errorNetwork, this);
        networkModel.setError("This Network does not exist in the Cluster");
        networkMap.put(networkName, networkModel);
        return networkModel;
    }

    private boolean equals(NetworkItemModel<?> item1, NetworkItemModel<?> item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        return (item1 != null) ? item1.equals(item2) : item2.equals(item1);

    }

    private List<VdsNetworkInterface> getFreeBonds() {
        List<VdsNetworkInterface> freeBonds = new ArrayList<VdsNetworkInterface>();
        for (VdsNetworkInterface bond : allBonds) {
            if (!nicMap.containsKey(bond.getName())) {
                freeBonds.add(bond);
            }
        }
        return freeBonds;
    }

    private void initAllModels(boolean fetchFromBackend) {
        if (fetchFromBackend) {
            // run query for networks, this chains the query for nics, and also stops progress when done
            StartProgress(null);
            queryNetworks();
        } else {
            initNetworkModels();
            initNicModels();
        }
    }

    private void initNetworkModels() {
        Map<String, LogicalNetworkModel> networkModels = new HashMap<String, LogicalNetworkModel>();
        for (network network : allNetworks) {
            networkModels.put(network.getname(), new LogicalNetworkModel(network, this));
        }
        setNetworks(networkModels);
    }

    private void initNicModels() {
        Map<String, NetworkInterfaceModel> nicModels = new HashMap<String, NetworkInterfaceModel>();
        Map<String, VdsNetworkInterface> nicMap = new HashMap<String, VdsNetworkInterface>();
        Map<String, VdsNetworkInterface> physicalNics = new HashMap<String, VdsNetworkInterface>();
        Map<String, List<String>> bondToNic = new HashMap<String, List<String>>();
        Map<String, List<String>> nicToNetwork = new HashMap<String, List<String>>();
        Map<String, Integer> networkToVlanId = new HashMap<String, Integer>();

        // map all nics
        for (VdsNetworkInterface nic : allNics) {
            nicMap.put(nic.getName(), nic);
        }

        // pass over all nics
        for (VdsNetworkInterface nic : allNics) {
            final Boolean isBonded = ((nic.getBonded() != null) && nic.getBonded() && nic.getName().indexOf('.') < 0);
            // is this a management nic? (comes from backend)
            boolean isNicManagement = nic.getIsManagement();
            final String nicName = nic.getName();
            final String networkName = nic.getNetworkName();
            final String bondName = nic.getBondName();
            final Integer vlanId = nic.getVlanId();
            final int dotpos = nicName.indexOf('.');

            // is this a physical nic?
            boolean isPhysicalInterface = vlanId == null;

            if (isPhysicalInterface) {
                physicalNics.put(nicName, nic);
            }

            // is the nic bonded?
            if (bondName != null) {
                if (bondToNic.containsKey(bondName)) {
                    bondToNic.get(bondName).add(nicName);
                } else {
                    List<String> bondedNics = new ArrayList<String>();
                    bondedNics.add(nicName);
                    bondToNic.put(bondName, bondedNics);
                }
            }

            // does this nic have a network?
            if (networkName != null) {
                LogicalNetworkModel networkModel = networkMap.get(networkName);
                if (networkModel == null) {
                    networkModel = createErrorNetworkModel(networkName, null);
                }

                // is this a management network (from backend)?
                if (isNicManagement) {
                    networkModel.setManagement(true);
                }

                networkToVlanId.put(networkName, vlanId);
                // bridge name is either <nic>, <nic.vlanid> or <bond.vlanid>
                String ifName;
                if (dotpos > 0) {
                    ifName = nicName.substring(0, dotpos);
                    // get stripped nic
                    VdsNetworkInterface iface = nicMap.get(ifName);
                    // pass the bond onto this nic
                    nic.setBonded(iface.getBonded());
                } else {
                    ifName = nicName;
                }
                Collection<LogicalNetworkModel> nicNetworks = new ArrayList<LogicalNetworkModel>();
                nicNetworks.add(networkModel);
                // set iface bridge to network
                NetworkInterfaceModel existingEridge = networkModel.getBridge();
                assert existingEridge == null : "should have only one bridge, but found " + existingEridge;
                networkModel.setBridge(new NetworkInterfaceModel(nic, nicNetworks, this));

                if (nicToNetwork.containsKey(ifName)) {
                    nicToNetwork.get(ifName).add(networkName);
                } else {
                    List<String> bridgedNetworks = new ArrayList<String>();
                    bridgedNetworks.add(networkName);
                    nicToNetwork.put(ifName, bridgedNetworks);
                }
            }
        }

        // build models
        for (VdsNetworkInterface nic : physicalNics.values()) {
            String nicName = nic.getName();
            // dont show bonded nics
            if (nic.getBondName() != null) {
                continue;
            }
            List<LogicalNetworkModel> nicNetworks = new ArrayList<LogicalNetworkModel>();
            List<String> networkNames = nicToNetwork.get(nicName);
            if (networkNames != null) {
                for (String networkName : networkNames) {
                    Integer vlanId = networkToVlanId.get(networkName);
                    LogicalNetworkModel networkModel;
                    networkModel = networkMap.get(networkName);
                    if (networkModel == null) {
                        // the network does not exist in this cluster
                        networkModel = createErrorNetworkModel(networkName, vlanId);
                    }
                    nicNetworks.add(networkModel);
                }
            }
            List<String> bondedNicNames = bondToNic.get(nicName);
            NetworkInterfaceModel nicModel;
            if (bondedNicNames != null) {
                List<NetworkInterfaceModel> bondedNics = new ArrayList<NetworkInterfaceModel>();
                for (String bondedNicName : bondedNicNames) {
                    VdsNetworkInterface bonded = nicMap.get(bondedNicName);
                    NetworkInterfaceModel bondedModel = new NetworkInterfaceModel(bonded, this);
                    bondedModel.setBonded(true);
                    bondedNics.add(bondedModel);
                }
                nicModel = new BondNetworkInterfaceModel(nic, nicNetworks, bondedNics, this);
            } else {
                nicModel = new NetworkInterfaceModel(nic, nicNetworks, this);
            }

            nicModels.put(nicName, nicModel);
        }
        setNics(nicModels);
    }

    private void queryFreeBonds() {
        // query for all bonds on the host
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(this);
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object returnValue)
            {
                List<VdsNetworkInterface> bonds =
                        (List<VdsNetworkInterface>) ((VdcQueryReturnValue) returnValue).getReturnValue();
                allBonds = bonds;
            }
        };

        VDS vds = (VDS) getEntity();
        Frontend.RunQuery(VdcQueryType.GetVdsFreeBondsByVdsId, new GetVdsByVdsIdParameters(vds.getId()), asyncQuery);
    }

    private void queryInterfaces() {
        // query for interfaces
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(this);
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object returnValueObj)
            {
                VdcQueryReturnValue returnValue = (VdcQueryReturnValue) returnValueObj;
                Object returnValue2 = returnValue.getReturnValue();
                List<VdsNetworkInterface> allNics = (List<VdsNetworkInterface>) returnValue2;
                HostSetupNetworksModel.this.allNics = allNics;
                initNicModels();
                StopProgress();
            }
        };

        VDS vds = (VDS) getEntity();
        GetVdsByVdsIdParameters params = new GetVdsByVdsIdParameters(vds.getId());
        params.setRefresh(false);
        Frontend.RunQuery(VdcQueryType.GetVdsInterfacesByVdsId, params, asyncQuery);
    }

    private void queryNetworks() {
        // query for networks
        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(this);
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object returnValue)
            {
                List<network> networks = (List<network>) returnValue;
                allNetworks = networks;
                initNetworkModels();
                // chain the nic query
                queryInterfaces();
            }
        };

        VDS vds = (VDS) getEntity();
        AsyncDataProvider.GetClusterNetworkList(asyncQuery, vds.getvds_group_id());
    }

    private void setBondOptions(VdsNetworkInterface entity, HostBondInterfaceModel bondDialogModel) {
        KeyValuePairCompat<String, EntityModel> BondPair =
                (KeyValuePairCompat<String, EntityModel>) bondDialogModel.getBondingOptions()
                        .getSelectedItem();
        String key = BondPair.getKey();
        entity.setBondOptions((String) ("custom".equals(key) ? BondPair.getValue().getEntity() : key));
    }

    private void setNetworks(Map<String, LogicalNetworkModel> networks) {
        networkMap = networks;
        getNetworksChangedEvent().raise(this, EventArgs.Empty);
    }

    private void setNetworksChangedEvent(Event value) {
        privateNetworksChangedEvent = value;
    }

    private void setNics(Map<String, NetworkInterfaceModel> nics) {
        nicMap = nics;
        onNicsChanged();
        getNicsChangedEvent().raise(this, EventArgs.Empty);
    }

    private void setNicsChangedEvent(Event value) {
        privateNicsChangedEvent = value;
    }

    private void setOperationCandidateEvent(Event event) {
        privateOperationCandidateEvent = event;
    }

    private void validate() {
        // check if management network is attached
        LogicalNetworkModel mgmtNetwork = networkMap.get(HostInterfaceListModel.ENGINE_NETWORK_NAME);
        if (!mgmtNetwork.isAttached()) {
            okCommand.setIsExecutionAllowed(false);
        } else {
            okCommand.setIsExecutionAllowed(true);
        }
    }

}