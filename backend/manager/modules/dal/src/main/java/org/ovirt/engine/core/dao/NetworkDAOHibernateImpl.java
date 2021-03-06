package org.ovirt.engine.core.dao;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NotImplementedException;

/**
 * <code>NetworkDAOHibernateImpl</code> provides an implementation of {@Link NetworkDAO} using Hibernate.
 *
 */
public class NetworkDAOHibernateImpl extends BaseDAOHibernateImpl<Network, Guid> implements NetworkDAO {
    public NetworkDAOHibernateImpl() {
        super(Network.class);
    }

    @Override
    public List<Network> getAllForDataCenter(Guid id) {
        return findByCriteria(Restrictions.eq("storage_pool_id", id));
    }

    @Override
    public List<Network> getAllForCluster(Guid id) {
        return findByCriteria(Restrictions.eq("cluster.clusterId", id));
    }

    @Override
    public List<Network> getAllForCluster(Guid id, Guid userID, boolean isFiltered) {
        throw new NotImplementedException();
    }
}
