package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.core.common.businessentities.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.VmNetworkStatistics;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * <code>VmNetworkStatisticsDAODbFacadeImpl</code> provides an implementation of {@link VmNetworkStatisticsDAO}.
 */
public class VmNetworkStatisticsDAODbFacadeImpl extends MassOperationsGenericDaoDbFacade<VmNetworkStatistics, Guid>
        implements VmNetworkStatisticsDAO {

    public VmNetworkStatisticsDAODbFacadeImpl() {
        super("vm_interface_statistics");
        setProcedureNameForGet("Getvm_interface_statisticsById");
    }

    @Override
    public List<VmNetworkStatistics> getAll() {
        throw new NotImplementedException();
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VmNetworkStatistics stats) {
        return createIdParameterMapper(stats.getId())
                .addValue("rx_drop", stats.getReceiveDropRate())
                .addValue("rx_rate", stats.getReceiveRate())
                .addValue("tx_drop", stats.getTransmitDropRate())
                .addValue("tx_rate", stats.getTransmitRate())
                .addValue("iface_status", stats.getStatus())
                .addValue("vm_id", stats.getVmId());
    }

    @Override
    protected ParameterizedRowMapper<VmNetworkStatistics> createEntityRowMapper() {
        return new ParameterizedRowMapper<VmNetworkStatistics>() {
            @Override
            public VmNetworkStatistics mapRow(ResultSet rs, int rowNum)
                    throws SQLException {
                VmNetworkStatistics entity = new VmNetworkStatistics();
                entity.setId(Guid.createGuidFromString(rs.getString("id")));
                entity.setReceiveRate(rs.getDouble("rx_rate"));
                entity.setTransmitRate(rs.getDouble("tx_rate"));
                entity.setReceiveDropRate(rs.getDouble("rx_drop"));
                entity.setTransmitDropRate(rs.getDouble("tx_drop"));
                entity.setStatus(InterfaceStatus.forValue(rs.getInt("iface_status")));
                return entity;
            }
        };
    }
}
