package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.queries.GetAllRelevantQuotasForVdsGroupParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.QuotaDAO;

/**
 * A test for the {@link GetAllRelevantQuotasForVdsGroupQuery} class.
 * It tests that flow (i.e., that the query delegates properly to the DAO}).
 * The internal workings of the DAO are not tested.
 */
public class GetAllRelevantQuotasForVdsGroupQueryTest extends AbstractQueryTest<GetAllRelevantQuotasForVdsGroupParameters, GetAllRelevantQuotasForVdsGroupQuery<GetAllRelevantQuotasForVdsGroupParameters>> {
    @Test
    public void testExecuteQueryCommand() {
        // Set up the query parameters
        Guid quotaID = Guid.NewGuid();
        when(params.getVdsGroupId()).thenReturn(quotaID);

        // Set up the DAOs
        List<Quota> expected = Collections.singletonList(new Quota());
        QuotaDAO quotaDAOMock = mock(QuotaDAO.class);
        when(quotaDAOMock.getAllRelevantQuotasForVdsGroup(quotaID)).thenReturn(expected);
        when(getDbFacadeMockInstance().getQuotaDAO()).thenReturn(quotaDAOMock);

        // Run the query
        GetAllRelevantQuotasForVdsGroupQuery<GetAllRelevantQuotasForVdsGroupParameters> query = getQuery();
        query.executeQueryCommand();

        // Assert the result
        assertEquals("Wrong result returned", expected, getQuery().getQueryReturnValue().getReturnValue());
    }
}
