package org.ovirt.engine.ui.uicommonweb.models.datacenters;

import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.RegexValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

@SuppressWarnings("unused")
public class NetworkModel extends Model
{

    private boolean privateIsNew;

    public boolean getIsNew()
    {
        return privateIsNew;
    }

    public void setIsNew(boolean value)
    {
        privateIsNew = value;
    }

    private EntityModel privateName;

    public EntityModel getName()
    {
        return privateName;
    }

    private void setName(EntityModel value)
    {
        privateName = value;
    }

    private EntityModel privateDescription;

    public EntityModel getDescription()
    {
        return privateDescription;
    }

    private void setDescription(EntityModel value)
    {
        privateDescription = value;
    }

    private EntityModel privateVLanTag;

    public EntityModel getVLanTag()
    {
        return privateVLanTag;
    }

    private void setVLanTag(EntityModel value)
    {
        privateVLanTag = value;
    }

    private EntityModel privateIsStpEnabled;

    public EntityModel getIsStpEnabled()
    {
        return privateIsStpEnabled;
    }

    private void setIsStpEnabled(EntityModel value)
    {
        privateIsStpEnabled = value;
    }

    private EntityModel privateHasVLanTag;

    public EntityModel getHasVLanTag()
    {
        return privateHasVLanTag;
    }

    private void setHasVLanTag(EntityModel value)
    {
        privateHasVLanTag = value;
    }

    private EntityModel privateHasMtu;

    public EntityModel getHasMtu()
    {
        return privateHasMtu;
    }

    private void setHasMtu(EntityModel value)
    {
        privateHasMtu = value;
    }

    private EntityModel privateMtu;

    public EntityModel getMtu()
    {
        return privateMtu;
    }

    private void setMtu(EntityModel value)
    {
        privateMtu = value;
    }

    private EntityModel privateIsVmNetwork;

    public EntityModel getIsVmNetwork()
    {
        return privateIsVmNetwork;
    }

    private void setIsVmNetwork(EntityModel value)
    {
        privateIsVmNetwork = value;
    }

    public NetworkModel()
    {
        setName(new EntityModel());
        setDescription(new EntityModel());
        setVLanTag(new EntityModel());
        EntityModel tempVar = new EntityModel();
        tempVar.setEntity(false);
        setIsStpEnabled(tempVar);
        EntityModel tempVar2 = new EntityModel();
        tempVar2.setEntity(false);
        setHasVLanTag(tempVar2);
        setMtu(new EntityModel());
        EntityModel tempVar3 = new EntityModel();
        tempVar3.setEntity(false);
        setHasMtu(tempVar3);
        EntityModel tempVar4 = new EntityModel();
        tempVar4.setEntity(true);
        setIsVmNetwork(tempVar4);
    }

    public boolean Validate()
    {
        RegexValidation tempVar = new RegexValidation();
        tempVar.setExpression("^[A-Za-z0-9_]{1,15}$"); //$NON-NLS-1$
        tempVar.setMessage(ConstantsManager.getInstance().getConstants().nameMustContainAlphanumericMaxLenMsg());
        RegexValidation tempVar2 = new RegexValidation();
        tempVar2.setIsNegate(true);
        tempVar2.setExpression("^(bond)"); //$NON-NLS-1$
        tempVar2.setMessage(ConstantsManager.getInstance().getConstants().networkNameStartMsg());
        getName().ValidateEntity(new IValidation[] { new NotEmptyValidation(), tempVar, tempVar2 });

        LengthValidation tempVar3 = new LengthValidation();
        tempVar3.setMaxLength(40);
        getDescription().ValidateEntity(new IValidation[] { tempVar3 });

        getVLanTag().setIsValid(true);
        if ((Boolean) getHasVLanTag().getEntity())
        {
            IntegerValidation tempVar4 = new IntegerValidation();
            tempVar4.setMinimum(0);
            tempVar4.setMaximum(4095);
            getVLanTag().ValidateEntity(new IValidation[] { new NotEmptyValidation(), tempVar4 });
        }

        getMtu().setIsValid(true);
        if ((Boolean) getHasMtu().getEntity())
        {
            IntegerValidation tempVar5 = new IntegerValidation();
            tempVar5.setMinimum(68);
            tempVar5.setMaximum(9000);
            getMtu().ValidateEntity(new IValidation[] { new NotEmptyValidation(), tempVar5 });
        }

        return getName().getIsValid() && getVLanTag().getIsValid() && getDescription().getIsValid()
                && getMtu().getIsValid();
    }

}
