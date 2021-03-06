package org.ovirt.engine.core.common.vdscommands.gluster;

import org.ovirt.engine.core.compat.Guid;

/**
 * VDS parameters class with volume name, volume option  and force as parameters,
 * apart from volume name inherited from {@link GlusterVolumeVDSParameters}.
 * Used by the "Reset gluster volume option" command.
 */
public class ResetGlusterVolumeOptionsVDSParameters extends GlusterVolumeVDSParameters {

    private String volumeOption;
    private boolean forceAction;

    public ResetGlusterVolumeOptionsVDSParameters(Guid serverId, String volumeName, String volumeOption, boolean forceAction) {
        super(serverId, volumeName);
        this.volumeOption = volumeOption;
        this.forceAction = forceAction;
    }

    public String getVolumeOption() {
        return volumeOption;
    }

    public boolean isforceAction() {
        return forceAction;
    }

}
