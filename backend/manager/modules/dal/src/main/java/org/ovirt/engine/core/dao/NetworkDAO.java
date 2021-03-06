package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>NetworkDAO</code> defines a type for performing CRUD operations on instances of {@link Network}.
 *
 *
 */
public interface NetworkDAO extends GenericDao<Network, Guid> {
    /**
     * Retrieves the network with the specified name.
     *
     * @param name
     *            the network name
     * @return the network
     */
    Network getByName(String name);

    /**
     * Retrieves all networks for the given data center.
     *
     * @param id
     *            the data center
     * @return the list of networks
     */
    List<Network> getAllForDataCenter(Guid id);

    /**
     * Retrieves all networks for the given cluster.
     *
     * @param id
     *            the cluster
     * @return the list of networks
     */
    List<Network> getAllForCluster(Guid id);

    /**
     * Retrieves all networks for the given cluster with optional permission filtering.
     *
     * @param id
     *            the cluster
     * @param userID
     *            the ID of the user requesting the information
     * @param isFiltered
     *            Whether the results should be filtered according to the user's permissions
     * @return the list of networks
     */
    List<Network> getAllForCluster(Guid id, Guid userID, boolean isFiltered);
}
