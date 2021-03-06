package org.ovirt.engine.core.bll.adbroker;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;

import org.ovirt.engine.core.ldap.LdapProviderType;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class Domain {

    private String name; // domain name
    private RootDSE rootDSE; // rootDSE for domain
    private Map<URI, ScorableLDAPServer> ldapServers = new HashMap<URI, ScorableLDAPServer>(); // LDAP servers that match
    private LdapProviderType ldapProviderType;
    private LDAPSecurityAuthentication ldapSecurityAuthentication;
    private String userName;
    private String password;
    private ReentrantLock lock = new ReentrantLock();

    // for this
    private final static Log log = LogFactory.getLog(ScorableLDAPServer.class);

    public Domain(String domainName) {
        name = domainName;
    }

    public LdapProviderType getLdapProviderType() {
        return ldapProviderType;
    }

    public void setLdapProviderType(LdapProviderType ldapProviderType) {
        this.ldapProviderType = ldapProviderType;
    }

    public String getName() {
        return name;
    }

    public void setRootDSE(RootDSE rootDSE) {
        this.rootDSE = rootDSE;
    }

    public RootDSE getRootDSE() {
        return rootDSE;
    }

    public void setLdapServers(List<URI> ldapServersURIs) {
        for (URI uri : ldapServersURIs) {
            ldapServers.put(uri, new ScorableLDAPServer(uri));
        }
    }

    public List<URI> getLdapServers() {
        List<ScorableLDAPServer> ldapServersCopy = new ArrayList<ScorableLDAPServer>(ldapServers.values());
        Collections.sort(ldapServersCopy);

        List<URI> servers = new ArrayList<URI>();
        for (ScorableLDAPServer server : ldapServersCopy) {
            servers.add(server.getURI());
        }
        return servers;
    }

    public void scoreLdapServer(URI ldapURI, Score score) {
        ScorableLDAPServer server = ldapServers.get(ldapURI);
        if (server != null) {
            server.setScore(score.getValue());
            if (log.isDebugEnabled()) {
                log.debug("LDAP server " + ldapURI.toString() + " has been scored " + score);
            }
        }
    }

    public void addLDAPServer(URI uri) {
        if (uri != null) {
            ldapServers.put(uri, new ScorableLDAPServer(uri));
        }
    }

    public LDAPSecurityAuthentication getLdapSecurityAuthentication() {
        return ldapSecurityAuthentication;
    }

    public void setLdapSecurityAuthentication(LDAPSecurityAuthentication ldapSecurityAuthentication) {
        this.ldapSecurityAuthentication = ldapSecurityAuthentication;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ReentrantLock getLock() {
        return lock;
    }

}
