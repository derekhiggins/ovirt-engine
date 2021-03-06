package org.ovirt.engine.ui.uicommonweb.models.clusters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ovirt.engine.core.common.action.AddNetworkStoragePoolParameters;
import org.ovirt.engine.core.common.action.AttachNetworkToVdsGroupParameter;
import org.ovirt.engine.core.common.action.DisplayNetworkToVdsGroupParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.VdsGroupQueryParamenters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostInterfaceListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

@SuppressWarnings("unused")
public class ClusterNetworkListModel extends SearchableListModel
{

    private UICommand privateNewNetworkCommand;

    public UICommand getNewNetworkCommand()
    {
        return privateNewNetworkCommand;
    }

    private void setNewNetworkCommand(UICommand value)
    {
        privateNewNetworkCommand = value;
    }

    private UICommand privateManageCommand;

    public UICommand getManageCommand()
    {
        return privateManageCommand;
    }

    private void setManageCommand(UICommand value)
    {
        privateManageCommand = value;
    }

    private UICommand privateSetAsDisplayCommand;

    public UICommand getSetAsDisplayCommand()
    {
        return privateSetAsDisplayCommand;
    }

    private void setSetAsDisplayCommand(UICommand value)
    {
        privateSetAsDisplayCommand = value;
    }

    private Network displayNetwork = null;

    private final Comparator<ClusterNetworkManageModel> networkComparator =
            new Comparator<ClusterNetworkManageModel>() {
                @Override
                public int compare(ClusterNetworkManageModel o1, ClusterNetworkManageModel o2) {
                    // management first
                    return o1.isManagement() ? -1 : o1.getName().compareTo(o2.getName());
                }
            };

    @Override
    public VDSGroup getEntity()
    {
        return (VDSGroup) ((super.getEntity() instanceof VDSGroup) ? super.getEntity() : null);
    }

    public void setEntity(VDSGroup value)
    {
        super.setEntity(value);
    }

    public ClusterNetworkListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().logicalNetworksTitle());
        setHashName("logical_networks"); //$NON-NLS-1$

        setManageCommand(new UICommand("Manage", this)); //$NON-NLS-1$
        setSetAsDisplayCommand(new UICommand("SetAsDisplay", this)); //$NON-NLS-1$
        setNewNetworkCommand(new UICommand("New", this)); //$NON-NLS-1$

        UpdateActionAvailability();
    }

    @Override
    protected void OnEntityChanged()
    {
        super.OnEntityChanged();
        getSearchCommand().Execute();
    }

    @Override
    public void Search()
    {
        if (getEntity() != null)
        {
            super.Search();
        }
    }

    @Override
    protected void SyncSearch()
    {
        if (getEntity() == null)
        {
            return;
        }

        super.SyncSearch();

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object ReturnValue)
            {
                SearchableListModel searchableListModel = (SearchableListModel) model;
                ArrayList<Network> newItems = (ArrayList<Network>) ((VdcQueryReturnValue) ReturnValue).getReturnValue();
                Collections.sort(newItems, new Comparator<Network>() {
                    @Override
                    public int compare(Network o1, Network o2) {
                        // management first
                        return HostInterfaceListModel.ENGINE_NETWORK_NAME.equals(o1.getname()) ? -1
                                : o1.getname().compareTo(o2.getname());
                    }
                });
                searchableListModel.setItems(newItems);
            }
        };

        VdsGroupQueryParamenters tempVar = new VdsGroupQueryParamenters(getEntity().getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.RunQuery(VdcQueryType.GetAllNetworksByClusterId, tempVar, _asyncQuery);
    }

    @Override
    protected void AsyncSearch()
    {
        super.AsyncSearch();

        setAsyncResult(Frontend.RegisterQuery(VdcQueryType.GetAllNetworksByClusterId,
                new VdsGroupQueryParamenters(getEntity().getId())));
        setItems(getAsyncResult().getData());
    }

    public void SetAsDisplay()
    {
        Network network = (Network) getSelectedItem();

        Frontend.RunAction(VdcActionType.UpdateDisplayToVdsGroup, new DisplayNetworkToVdsGroupParameters(getEntity(),
                network,
                true));
    }

    public void Manage()
    {
        if (getWindow() != null)
        {
            return;
        }

        Guid storagePoolId =
                (getEntity().getstorage_pool_id() != null) ? getEntity().getstorage_pool_id().getValue() : NGuid.Empty;

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                ClusterNetworkListModel clusterNetworkListModel = (ClusterNetworkListModel) model;
                ArrayList<Network> dcNetworks = (ArrayList<Network>) result;
                ListModel networkToManage = createNetworkList(dcNetworks);
                clusterNetworkListModel.setWindow(networkToManage);
                networkToManage.setTitle(ConstantsManager.getInstance().getConstants().assignDetachNetworksTitle());
                networkToManage.setHashName("assign_networks"); //$NON-NLS-1$
            }
        };
        // fetch the list of DC Networks
        AsyncDataProvider.GetNetworkList(_asyncQuery, storagePoolId);
    }

    private ListModel createNetworkList(List<Network> dcNetworks) {
        List<ClusterNetworkManageModel> networkList = new ArrayList<ClusterNetworkManageModel>();
        java.util.ArrayList<Network> clusterNetworks = Linq.<Network> Cast(getItems());
        for (Network network : dcNetworks) {
            ClusterNetworkManageModel networkManageModel = new ClusterNetworkManageModel(network);
            int index = clusterNetworks.indexOf(network);
            if (index >= 0) {
                Network clusterNetwork = clusterNetworks.get(index);
                networkManageModel.setVmNetwork(clusterNetwork.isVmNetwork());
                networkManageModel.setRequired(clusterNetwork.isRequired());
                networkManageModel.setDisplayNetwork(clusterNetwork.getis_display());
                if (clusterNetwork.getis_display()) {
                    displayNetwork = clusterNetwork;
                }
                networkManageModel.setAttached(true);
            } else {
                networkManageModel.setAttached(false);
            }
            networkList.add(networkManageModel);
        }

        Collections.sort(networkList, networkComparator);

        ListModel listModel = new ListModel();
        listModel.setItems(networkList);

        UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        listModel.getCommands().add(cancelCommand);

        UICommand okCommand = new UICommand("OnManage", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        listModel.getCommands().add(0, okCommand);

        return listModel;
    }

    public void OnManage() {
        final ListModel windowModel = (ListModel) getWindow();

        List<ClusterNetworkManageModel> manageList = Linq.<ClusterNetworkManageModel> Cast(windowModel.getItems());
        List<Network> existingClusterNetworks = Linq.<Network> Cast(getItems());
        final ArrayList<VdcActionParametersBase> toAttach = new ArrayList<VdcActionParametersBase>();
        final ArrayList<VdcActionParametersBase> toDetach = new ArrayList<VdcActionParametersBase>();

        for (ClusterNetworkManageModel networkModel : manageList) {
            Network network = networkModel.getEntity();
            boolean contains = existingClusterNetworks.contains(network);

            boolean needsAttach = networkModel.isAttached() && !contains;
            boolean needsDetach = !networkModel.isAttached() && contains;
            boolean needsUpdate = false;

            if (contains && !needsDetach) {
                Network clusterNetwork = existingClusterNetworks.get(existingClusterNetworks.indexOf(network));

                if ((networkModel.isRequired() != clusterNetwork.isRequired())
                                || (networkModel.isDisplayNetwork() != clusterNetwork.getis_display())) {
                    needsUpdate = true;
                }
            }

            if (needsAttach || needsUpdate) {
                toAttach.add(new AttachNetworkToVdsGroupParameter(getEntity(), network));
            }

            if (needsDetach) {
                toDetach.add(new AttachNetworkToVdsGroupParameter(getEntity(), network));
            }
        }

        final IFrontendMultipleActionAsyncCallback callback = new IFrontendMultipleActionAsyncCallback() {
            Boolean needsAttach = !toAttach.isEmpty();
            Boolean needsDetach = !toDetach.isEmpty();

            @Override
            public void Executed(FrontendMultipleActionAsyncResult result) {
                if (result.getActionType() == VdcActionType.DetachNetworkToVdsGroup) {
                    needsDetach = false;
                }
                if (result.getActionType() == VdcActionType.AttachNetworkToVdsGroup) {
                    needsAttach = false;
                }

                if (needsAttach) {
                    Frontend.RunMultipleAction(VdcActionType.AttachNetworkToVdsGroup, toAttach, this, null);
                }

                if (needsDetach) {
                    Frontend.RunMultipleAction(VdcActionType.DetachNetworkToVdsGroup, toDetach, this, null);
                }

                if (!needsAttach && !needsDetach) {
                    doFinish();
                }
            }

            private void doFinish() {
                windowModel.StopProgress();
                Cancel();
                ForceRefresh();
            }
        };

        callback.Executed(new FrontendMultipleActionAsyncResult(null, null, null));
        windowModel.StartProgress(null);
    }

    public void Cancel()
    {
        setWindow(null);
    }

    @Override
    protected void EntityChanging(Object newValue, Object oldValue)
    {
        VDSGroup vdsGroup = (VDSGroup) newValue;
        getNewNetworkCommand().setIsExecutionAllowed(vdsGroup != null && vdsGroup.getstorage_pool_id() != null);
    }

    @Override
    protected void OnSelectedItemChanged()
    {
        super.OnSelectedItemChanged();
        UpdateActionAvailability();
    }

    @Override
    protected void SelectedItemsChanged()
    {
        super.SelectedItemsChanged();
        UpdateActionAvailability();
    }

    private void UpdateActionAvailability()
    {
        Network network = (Network) getSelectedItem();

        // CanRemove = SelectedItems != null && SelectedItems.Count > 0;
        getSetAsDisplayCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() == 1
                && network != null && !(network.getis_display() == null ? false : network.getis_display())
                && network.getStatus() != NetworkStatus.NonOperational);
    }

    public void New()
    {
        if (getWindow() != null)
        {
            return;
        }

        ClusterNetworkModel clusterModel = new ClusterNetworkModel();
        setWindow(clusterModel);
        clusterModel.setTitle(ConstantsManager.getInstance().getConstants().newLogicalNetworkTitle());
        clusterModel.setHashName("new_logical_network"); //$NON-NLS-1$
        clusterModel.setIsNew(true);
        if (getEntity().getstorage_pool_id() != null)
        {
            AsyncQuery _asyncQuery = new AsyncQuery();
            _asyncQuery.setModel(clusterModel);
            _asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object model, Object result)
                {
                    ClusterNetworkModel clusterNetworkModel = (ClusterNetworkModel) model;
                    storage_pool dataCenter = (storage_pool) result;
                    clusterNetworkModel.setDataCenterName(dataCenter.getname());
                }
            };
            AsyncDataProvider.GetDataCenterById(_asyncQuery, getEntity().getstorage_pool_id().getValue());
        }
        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        clusterModel.getCommands().add(tempVar);

        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        clusterModel.getCommands().add(tempVar2);
    }

    public void OnSave()
    {
        ClusterNetworkModel model = (ClusterNetworkModel) getWindow();
        Network network = new Network(null);

        if (getEntity() == null)
        {
            Cancel();
            return;
        }

        if (!model.Validate() || getEntity().getstorage_pool_id() == null)
        {
            return;
        }

        network.setstorage_pool_id(getEntity().getstorage_pool_id());
        network.setname((String) model.getName().getEntity());
        network.setstp((Boolean) model.getIsStpEnabled().getEntity());
        network.setdescription((String) model.getDescription().getEntity());
        network.setVmNetwork((Boolean) model.getIsVmNetwork().getEntity());

        network.setMtu(0);
        if (model.getMtu().getEntity() != null)
        {
            network.setMtu(Integer.parseInt(model.getMtu().getEntity().toString()));
        }

        network.setvlan_id(null);
        if ((Boolean) model.getHasVLanTag().getEntity())
        {
            network.setvlan_id(Integer.parseInt(model.getVLanTag().getEntity().toString()));
        }

        Frontend.RunAction(VdcActionType.AddNetwork, new AddNetworkStoragePoolParameters(network.getstorage_pool_id()
                .getValue(), network),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendActionAsyncResult result) {

                        Object[] data = (Object[]) result.getState();
                        ClusterNetworkListModel networkListModel = (ClusterNetworkListModel) data[0];
                        VdcReturnValueBase retVal = result.getReturnValue();
                        if (retVal != null && retVal.getSucceeded())
                        {
                            Network tempVar = new Network(null);
                            tempVar.setId((Guid) retVal.getActionReturnValue());
                            tempVar.setname(((Network) data[1]).getname());
                            Frontend.RunAction(VdcActionType.AttachNetworkToVdsGroup,
                                    new AttachNetworkToVdsGroupParameter(networkListModel.getEntity(), tempVar));
                        }
                        networkListModel.Cancel();

                    }
                }, new Object[] { this, network });
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getManageCommand())
        {
            Manage();
        }
        else if (command == getSetAsDisplayCommand())
        {
            SetAsDisplay();
        }

        else if (StringHelper.stringsEqual(command.getName(), "OnManage")) //$NON-NLS-1$
        {
            OnManage();
        }
        else if (StringHelper.stringsEqual(command.getName(), "New")) //$NON-NLS-1$
        {
            New();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSave")) //$NON-NLS-1$
        {
            OnSave();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            Cancel();
        }
    }

    @Override
    protected String getListName() {
        return "ClusterNetworkListModel"; //$NON-NLS-1$
    }

}
