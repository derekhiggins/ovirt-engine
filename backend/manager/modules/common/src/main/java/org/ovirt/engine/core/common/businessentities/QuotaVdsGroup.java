package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;

import javax.validation.constraints.Min;

import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.INotifyPropertyChanged;

/**
 * The <code>QuotaVdsGroup</code> is a business entity that reflects vdsGroup limitation.
 */
public class QuotaVdsGroup extends IVdcQueryable implements INotifyPropertyChanged, Serializable {

    /**
     * Automatic generated serial version ID.
     */
    private static final long serialVersionUID = 168673911413393235L;

    /**
     * The quota vds group Id.
     */
    private Guid quotaVdsGroupId;

    /**
     * The quota ID.
     */
    private Guid quotaId;

    /**
     * The vds group Id which this limitation is enforced on.
     */
    private Guid vdsGroupId;

    /**
     * Transient field indicates the vds group name.
     */
    private String vdsGroupName;

    /**
     * The virtual CPU limitations for vds group.
     */
    @Min(-1)
    private Integer virtualCpu;

    /**
     * Transient field indicates the virtual CPU usage of the quota vds group.
     */
    private Integer virtualCpuUsage;

    /**
     * The virtual memory limitations for specific quota vdsGroup.
     */
    @Min(-1)
    private Long memSizeMB;

    /**
     * Transient field indicates the virtual memory usage of the quota vdsGroup.
     */
    private Long memSizeMBUsage;

    /**
     * Default constructor of QuotaVdsGroup.
     */
    public QuotaVdsGroup() {
    }

    public QuotaVdsGroup(Guid quotaVdsGroupId,
            Guid quotaId,
            Guid vdsGroupId,
            Integer virtualCpu,
            Integer virtualCpuUsage,
            Long memSizeMB,
            Long memSizeMBUsage) {
        this.quotaVdsGroupId = quotaVdsGroupId;
        this.quotaId = quotaId;
        this.vdsGroupId = vdsGroupId;
        this.virtualCpu = virtualCpu;
        this.virtualCpuUsage = virtualCpuUsage;
        this.memSizeMB = memSizeMB;
        this.memSizeMBUsage = memSizeMBUsage;
    }

    public QuotaVdsGroup(QuotaVdsGroup quotaVdsGroup) {
        this.quotaVdsGroupId = quotaVdsGroup.getVdsGroupId();
        this.quotaId = quotaVdsGroup.getQuotaId();
        this.vdsGroupId = quotaVdsGroup.getVdsGroupId();
        this.virtualCpu = quotaVdsGroup.getVirtualCpu();
        this.virtualCpuUsage = quotaVdsGroup.getVirtualCpuUsage();
        this.memSizeMB = quotaVdsGroup.getMemSizeMB();
        this.memSizeMBUsage = quotaVdsGroup.getMemSizeMBUsage();
    }

    /**
     * @return the quotaId
     */
    public Guid getQuotaId() {
        return quotaId;
    }

    /**
     * @param quotaId
     *            the quotaId to set
     */
    public void setQuotaId(Guid quotaId) {
        this.quotaId = quotaId;
    }

    /**
     * @return the vdsGroupId
     */
    public Guid getVdsGroupId() {
        return vdsGroupId;
    }

    /**
     * @param vdsGroupId
     *            the vdsGroupId to set
     */
    public void setVdsGroupId(Guid vdsGroupId) {
        this.vdsGroupId = vdsGroupId;
    }

    /**
     * @return the memSizeMBUsage
     */
    public Long getMemSizeMBUsage() {
        return memSizeMBUsage;
    }

    /**
     * @param memSizeMB
     *            the memSizeMB to set
     */
    public void setMemSizeMB(Long memSizeMB) {
        this.memSizeMB = memSizeMB;
    }

    /**
     * @return the virtualCpuUsage
     */
    public Integer getVirtualCpuUsage() {
        return virtualCpuUsage;
    }

    /**
     * @param virtualCpuUsage
     *            the virtualCpuUsage to set
     */
    public void setVirtualCpuUsage(Integer virtualCpuUsage) {
        this.virtualCpuUsage = virtualCpuUsage;
    }

    /**
     * @return the virtualCpu
     */
    public Integer getVirtualCpu() {
        return virtualCpu;
    }

    /**
     * @param virtualCpu
     *            the virtualCpu to set
     */
    public void setVirtualCpu(Integer virtualCpu) {
        this.virtualCpu = virtualCpu;
    }

    /**
     * @param memSizeMBUsage
     *            the memSizeMBUsage to set
     */
    public void setMemSizeMBUsage(Long memSizeMBUsage) {
        this.memSizeMBUsage = memSizeMBUsage;
    }

    /**
     * @return the memory size in mega bytes.
     */
    public Long getMemSizeMB() {
        return memSizeMB;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((quotaId == null) ? 0 : quotaId.hashCode());
        result = prime * result + ((quotaVdsGroupId == null) ? 0 : quotaVdsGroupId.hashCode());
        result = prime * result + ((vdsGroupId == null) ? 0 : vdsGroupId.hashCode());
        result = prime * result + ((virtualCpu == null) ? 0 : virtualCpu.hashCode());
        result = prime * result + ((memSizeMB == null) ? 0 : memSizeMB.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QuotaVdsGroup other = (QuotaVdsGroup) obj;
        if (quotaId == null) {
            if (other.quotaId != null)
                return false;
        } else if (!quotaId.equals(other.quotaId))
            return false;
        if (quotaVdsGroupId == null) {
            if (other.quotaVdsGroupId != null)
                return false;
        } else if (!quotaVdsGroupId.equals(other.quotaVdsGroupId))
            return false;
        if (vdsGroupId == null) {
            if (other.vdsGroupId != null)
                return false;
        } else if (!vdsGroupId.equals(other.vdsGroupId))
            return false;
        if (virtualCpu == null) {
            if (other.virtualCpu != null)
                return false;
        } else if (!virtualCpu.equals(other.virtualCpu))
            return false;
        if (memSizeMB == null) {
            if (other.memSizeMB != null)
                return false;
        } else if (!memSizeMB.equals(other.memSizeMB))
            return false;
        return true;
    }

    /**
     * @return the quotaVdsGroupId
     */
    public Guid getQuotaVdsGroupId() {
        return quotaVdsGroupId;
    }

    /**
     * @param quotaVdsGroupId
     *            the quotaVdsGroupId to set
     */
    public void setQuotaVdsGroupId(Guid quotaVdsGroupId) {
        this.quotaVdsGroupId = quotaVdsGroupId;
    }

    /**
     * @return the vdsGroupName
     */
    public String getVdsGroupName() {
        return vdsGroupName;
    }

    /**
     * @param vdsGroupName
     *            the vdsGroupName to set
     */
    public void setVdsGroupName(String vdsGroupName) {
        this.vdsGroupName = vdsGroupName;
    }

    @Override
    public Object getQueryableId() {
        return getQuotaVdsGroupId();
    }
}
