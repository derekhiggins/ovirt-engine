package org.ovirt.engine.ui.uicommonweb.models.quota;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.GetEntitiesRelatedToQuotaIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;

public class QuotaTemplateListModel extends SearchableListModel {

    public QuotaTemplateListModel() {
        setTitle("Templates");
        setIsTimerDisabled(true);
    }

    @Override
    protected void SyncSearch() {
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
                QuotaTemplateListModel vmModel = (QuotaTemplateListModel) model;
                vmModel.setItems((java.util.ArrayList<VM>) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
                vmModel.setIsEmpty(((java.util.List) vmModel.getItems()).size() == 0);
            }
        };

        GetEntitiesRelatedToQuotaIdParameters tempVar =
                new GetEntitiesRelatedToQuotaIdParameters();
        tempVar.setQuotaId(((Quota) getEntity()).getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.RunQuery(VdcQueryType.GetTemplatesRelatedToQuotaId, tempVar, _asyncQuery);
    }

    @Override
    protected void OnEntityChanged()
    {
        super.OnEntityChanged();
        getSearchCommand().Execute();
    }

    @Override
    protected String getListName() {
        return "QuotaTemplateListModel";
    }

}