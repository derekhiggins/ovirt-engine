package org.ovirt.engine.core.bll.network;

import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.MacPoolManager;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VmNetworkInterfaceDAO;
import org.ovirt.engine.core.dao.VmNetworkStatisticsDAO;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

/**
 * Helper class to use for adding/removing {@link VmNetworkInterface}s.
 */
public class VmInterfaceManager {

    protected Log log = LogFactory.getLog(getClass());
    /**
     * Add a {@link VmNetworkInterface} to the VM, trying to acquire a MAC from the {@link MacPoolManager}.<br>
     * If the MAC is already in use, a warning will be sent to the user.
     *
     * @param iface
     *            The interface to save.
     * @param compensationContext
     *            Used to snapshot the saved entities.
     * @return <code>true</code> if the MAC wasn't used, <code>false</code> if it was.
     */
    public boolean add(VmNetworkInterface iface, CompensationContext compensationContext, boolean allocateMac) {
        boolean macAdded = false;
        if (allocateMac) {
            iface.setMacAddress(getMacPoolManager().allocateNewMac());
            macAdded = true;
        } else if (getMacPoolManager().IsMacInUse(iface.getMacAddress())) {
            AuditLogableBase logable = new AuditLogableBase();
            logable.AddCustomValue("MACAddr", iface.getMacAddress());
            logable.AddCustomValue("VmName", iface.getVmName());
            log(logable, AuditLogType.MAC_ADDRESS_IS_IN_USE);
        } else {
            macAdded = getMacPoolManager().AddMac(iface.getMacAddress());
        }
        getVmNetworkInterfaceDAO().save(iface);
        getVmNetworkStatisticsDAO().save(iface.getStatistics());
        compensationContext.snapshotNewEntity(iface);
        compensationContext.snapshotNewEntity(iface.getStatistics());

        return macAdded;
    }

    /**
     * Remove all {@link VmNetworkInterface}s from the VM, removing from {@link MacPoolManager} if required.
     *
     * @param removeFromMacPool
     *            Should the MAC be removed from {@link MacPoolManager}?
     * @param vmId
     *            The ID of the VM to remove from.
     */
    public void removeAll(boolean removeFromMacPool, Guid vmId) {
        List<VmNetworkInterface> interfaces = getVmNetworkInterfaceDAO().getAllForVm(vmId);
        if (interfaces != null) {
            for (VmNetworkInterface iface : interfaces) {
                if (removeFromMacPool) {
                    getMacPoolManager().freeMac(iface.getMacAddress());
                }

                getVmNetworkInterfaceDAO().remove(iface.getId());
                getVmNetworkStatisticsDAO().remove(iface.getId());
            }
        }
    }

    /**
     * Checks if a Network belongs to the cluster and is a VM Network
     * @param iface
     * @param clusterId
     * @return
     */
    public static boolean isValidVmNetwork(VmNetworkInterface iface, Guid clusterId) {
        Map<String, Network> networksByName =
            Entities.entitiesByName(DbFacade.getInstance()
                    .getNetworkDAO()
                    .getAllForCluster(clusterId));
        String networkName = iface.getNetworkName();
        return (networksByName.containsKey(networkName) && networksByName.get(networkName).isVmNetwork());
    }

    /**
     * Log the given loggable & message to the {@link AuditLogDirector}.
     *
     * @param logable
     * @param auditLogType
     */
    protected void log(AuditLogableBase logable, AuditLogType auditLogType) {
        AuditLogDirector.log(logable, auditLogType);
    }

    protected MacPoolManager getMacPoolManager() {
        return MacPoolManager.getInstance();
    }

    protected VmNetworkStatisticsDAO getVmNetworkStatisticsDAO() {
        return DbFacade.getInstance().getVmNetworkStatisticsDAO();
    }

    protected VmNetworkInterfaceDAO getVmNetworkInterfaceDAO() {
        return DbFacade.getInstance().getVmNetworkInterfaceDAO();
    }
}
