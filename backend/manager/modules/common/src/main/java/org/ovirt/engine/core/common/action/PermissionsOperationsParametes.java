package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.common.businessentities.*;

import org.ovirt.engine.core.common.users.*;

public class PermissionsOperationsParametes extends VdcActionParametersBase {
    private static final long serialVersionUID = 8854712438369127152L;

    public PermissionsOperationsParametes(permissions permission) {
        setPermission(permission);
    }

    public PermissionsOperationsParametes(permissions permission, VdcUser vdcUser) {
        this(permission);
        setVdcUser(vdcUser);
    }

    public PermissionsOperationsParametes(permissions permission, ad_groups adGroup) {
        this(permission);
        setAdGroup(adGroup);
    }

    private VdcUser privateVdcUser;

    public VdcUser getVdcUser() {
        return privateVdcUser;
    }

    public void setVdcUser(VdcUser value) {
        privateVdcUser = value;
    }

    private ad_groups privateAdGroup;

    public ad_groups getAdGroup() {
        return privateAdGroup;
    }

    public void setAdGroup(ad_groups value) {
        privateAdGroup = value;
    }

    private permissions privatePermission;

    public permissions getPermission() {
        return privatePermission;
    }

    public void setPermission(permissions value) {
        privatePermission = value;
    }

    public PermissionsOperationsParametes() {
    }
}
