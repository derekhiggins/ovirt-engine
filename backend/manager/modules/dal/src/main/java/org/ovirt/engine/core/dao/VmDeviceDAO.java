package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.compat.Guid;

public interface VmDeviceDAO extends GenericDao<VmDevice, VmDeviceId>, MassOperationsDao<VmDevice> {

    /**
     * Check if the {@link VmDevice} with the given id exists or not.
     * @param id
     *            The device id.
     * @return Does the device exist or not.
     */
    boolean exists(VmDeviceId id);

    List<VmDevice> getVmDeviceByVmId(Guid vmId);

    List<VmDevice> getVmDeviceByVmIdAndType(Guid vmId, String type);

    List<VmDevice> getVmDeviceByVmIdTypeAndDevice(Guid vmId, String type, String device);

    List<VmDevice> getVmDeviceByVmIdTypeAndDevice(Guid vmId, String type, String device, Guid userID, boolean isFiltered);

    List<VmDevice> getUnmanagedDevicesByVmId(Guid vmId);

    boolean isMemBalloonEnabled(Guid vmId);

    void removeAll(List<VmDeviceId> removedDeviceIds);

    void saveAll(List<VmDevice> newVmDevices);

    /**
     * Clear the device address kept for this device. When A VM is started, devices with empty addresses are allocated
     * with new one. Device addresses are fetched by the engine from each host per each VM periodically so once a VM is
     * up, its devices map is fetched and saved to DB.
     * Use this method when the address is not used anymore or is not valid e.g when changing a disk interface type from
     * IDE to VirtIO.
     * @param device
     */
    void clearDeviceAddress(Guid deviceId);
}
