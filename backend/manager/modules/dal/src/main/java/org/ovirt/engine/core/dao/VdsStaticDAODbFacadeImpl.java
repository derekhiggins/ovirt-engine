package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.ovirt.engine.core.engineencryptutils.EncryptionUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

/**
 * <code>VdsDAODbFacadeImpl</code> provides an implementation of {@link VdsDAO} that uses previously written code from
 * {@code DbFacade}.
 */
public class VdsStaticDAODbFacadeImpl extends BaseDAODbFacade implements VdsStaticDAO {

    @Override
    public VdsStatic get(Guid id) {
        return getCallsHandler().executeRead("GetVdsStaticByVdsId",
                new VdsStaticRowMapper(),
                getCustomMapSqlParameterSource()
                        .addValue("vds_id", id));
    }

    @Override
    public VdsStatic get(String name) {
        return (VdsStatic) DbFacadeUtils.asSingleResult(getCallsHandler().executeReadList("GetVdsStaticByVdsName",
                new VdsStaticRowMapper(),
                getCustomMapSqlParameterSource()
                        .addValue("vds_name", name)));
    }

    @Override
    public List<VdsStatic> getAllForHost(String host) {
        return getCallsHandler().executeReadList("GetVdsStaticByHostName",
                new VdsStaticRowMapper(),
                getCustomMapSqlParameterSource()
                        .addValue("host_name", host));
    }

    @Override
    public List<VdsStatic> getAllWithIpAddress(String address) {
        return getCallsHandler().executeReadList("GetVdsStaticByIp",
                new VdsStaticRowMapper(),
                getCustomMapSqlParameterSource()
                        .addValue("ip", address));
    }

    @Override
    public List<VdsStatic> getAllForVdsGroup(Guid vdsGroup) {
        return getCallsHandler().executeReadList("GetVdsStaticByVdsGroupId",
                new VdsStaticRowMapper(),
                getCustomMapSqlParameterSource()
                        .addValue("vds_group_id", vdsGroup));
    }

    @Override
    public void save(VdsStatic vds) {
        Map<String, Object> dbResults = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("InsertVdsStatic").execute(getInsertOrUpdateParams(vds));

        vds.setId(new Guid(dbResults.get("vds_id").toString()));
    }

    @Override
    public void update(VdsStatic vds) {
        getCallsHandler().executeModification("UpdateVdsStatic", getInsertOrUpdateParams(vds));
    }

    private MapSqlParameterSource getInsertOrUpdateParams(final VdsStatic vds) {
        return getCustomMapSqlParameterSource()
                .addValue("host_name", vds.gethost_name())
                .addValue("ip", vds.getManagmentIp())
                .addValue("vds_unique_id", vds.getUniqueID())
                .addValue("port", vds.getport())
                .addValue("vds_group_id", vds.getvds_group_id())
                .addValue("vds_id", vds.getId())
                .addValue("vds_name", vds.getvds_name())
                .addValue("server_SSL_enabled", vds.getserver_SSL_enabled())
                .addValue("vds_type", vds.getvds_type())
                .addValue("vds_strength", vds.getvds_strength())
                .addValue("pm_type", vds.getpm_type())
                .addValue("pm_user", vds.getpm_user())
                .addValue("pm_password", encryptPassword(vds.getpm_password()))
                .addValue("pm_port", vds.getpm_port())
                .addValue("pm_options", vds.getpm_options())
                .addValue("pm_enabled", vds.getpm_enabled())
                .addValue("otp_validity", vds.getOtpValidity())
                .addValue("vds_spm_priority", vds.getVdsSpmPriority());
    }

    @Override
    public void remove(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vds_id", id);

        getCallsHandler().executeModification("DeleteVdsStatic", parameterSource);
    }

    @Override
    public List<VdsStatic> getAll() {
        throw new NotImplementedException();
    }

    public static String encryptPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return password;
        }
        String keyFile = Config.resolveKeyStorePath();
        String passwd = Config.<String> GetValue(ConfigValues.keystorePass, Config.DefaultConfigurationVersion);
        String alias = Config.<String> GetValue(ConfigValues.CertAlias, Config.DefaultConfigurationVersion);
        try {
            return EncryptionUtils.encrypt((String) password, keyFile, passwd, alias);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public static String decryptPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return password;
        }
        String keyFile = Config.resolveKeyStorePath();
        String passwd = Config.<String> GetValue(ConfigValues.keystorePass, Config.DefaultConfigurationVersion);
        String alias = Config.<String> GetValue(ConfigValues.CertAlias, Config.DefaultConfigurationVersion);
        try {
            return EncryptionUtils.decrypt((String) password, keyFile, passwd, alias);
        } catch (Exception e) {
            log.debugFormat("Failed to decrypt password, error message: {0}", e.getMessage());
            return password;
        }
    }

    private static Log log = LogFactory.getLog(VdsStaticDAODbFacadeImpl.class);

    private final static class VdsStaticRowMapper implements ParameterizedRowMapper<VdsStatic> {
        @Override
        public VdsStatic mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            VdsStatic entity = new VdsStatic();
            entity.sethost_name(rs.getString("host_name"));
            entity.setManagmentIp(rs.getString("ip"));
            entity.setUniqueID(rs.getString("vds_unique_id"));
            entity.setport(rs.getInt("port"));
            entity.setvds_group_id(Guid.createGuidFromString(rs
                    .getString("vds_group_id")));
            entity.setId(Guid.createGuidFromString(rs
                    .getString("vds_id")));
            entity.setvds_name(rs.getString("vds_name"));
            entity.setserver_SSL_enabled(rs
                    .getBoolean("server_SSL_enabled"));
            entity.setvds_type(VDSType.forValue(rs.getInt("vds_type")));
            entity.setvds_strength(rs.getInt("vds_strength"));
            entity.setpm_type(rs.getString("pm_type"));
            entity.setpm_user(rs.getString("pm_user"));
            entity.setpm_password(decryptPassword(rs.getString("pm_password")));
            entity.setpm_port((Integer) rs.getObject("pm_port"));
            entity.setpm_options(rs.getString("pm_options"));
            entity.setpm_enabled(rs.getBoolean("pm_enabled"));
            entity.setOtpValidity(rs.getLong("otp_validity"));

            return entity;
        }
    }

}
