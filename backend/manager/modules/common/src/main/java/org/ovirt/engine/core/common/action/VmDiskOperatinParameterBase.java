package org.ovirt.engine.core.common.action;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.compat.Guid;

public class VmDiskOperatinParameterBase extends VmOperationParameterBase {

    private static final long serialVersionUID = 337339450251569362L;

    @Valid
    private Disk diskInfo;

    public VmDiskOperatinParameterBase() {
    }

    public VmDiskOperatinParameterBase(Guid vmId, Disk diskInfo) {
        super(vmId);
        setDiskInfo(diskInfo);
    }

    public Disk getDiskInfo() {
        return diskInfo;
    }

    public void setDiskInfo(Disk value) {
        diskInfo = value;
    }
}
