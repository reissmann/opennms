package org.opennms.container.jaas;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.opennms.netmgt.config.api.UserConfig;
import org.opennms.netmgt.config.users.User;
import org.opennms.netmgt.model.OnmsUser;
import org.opennms.web.springframework.security.SpringSecurityUserDao;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

public class OpenNMSLoginModule extends AbstractKarafLoginModule {
    private static final transient Logger LOG = LoggerFactory.getLogger(OpenNMSLoginModule.class);
    private static transient volatile BundleContext m_context;

    private UserConfig m_userConfig;
    private SpringSecurityUserDao m_userDao;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
    }

    @Override
    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];

        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(callbacks);
        } catch (final IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (final UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }

        user = ((NameCallback) callbacks[0]).getName();
        if (user == null) {
            throw new LoginException("Username can not be null");
        }

        // password callback get value
        if (((PasswordCallback) callbacks[1]).getPassword() == null) {
            throw new LoginException("Password can not be null");
        }
        final String password = new String(((PasswordCallback) callbacks[1]).getPassword());

        final User configUser;
        final OnmsUser onmsUser;
        try {
            configUser = getUserConfig().getUser(user);
            onmsUser = getSpringSecurityUserDao().getByUsername(user);
        } catch (final Exception e) {
            LOG.debug("Failed to retrieve user " + user + " from OpenNMS UserConfig.", e);
            throw new LoginException("Failed to retrieve user " + user + " from OpenNMS UserConfig.");
        }

        if (configUser == null) {
            throw new FailedLoginException("User  " + user + " does not exist");
        }

        if (!getUserConfig().comparePasswords(user, password)) {
            throw new FailedLoginException("login failed");
        };

        principals = new HashSet<Principal>();
        for (final GrantedAuthority auth : onmsUser.getAuthorities()) {
            // not sure if karaf is OK with ROLE_* or wants lower-case *
            principals.add(new RolePrincipal(auth.getAuthority().toLowerCase().replaceFirst("^role_", "")));
            principals.add(new RolePrincipal(auth.getAuthority()));
        }

        if (debug) {
            LOG.debug("Successfully logged in {}", user);
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        clear();
        if (debug) {
            LOG.debug("abort");
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        if (debug) {
            LOG.debug("logout");
        }
        return true;
    }

    public UserConfig getUserConfig() {
        if (m_userConfig == null) {
            m_userConfig = getFromRegistry(UserConfig.class);
        }
        return m_userConfig;
    }

    public SpringSecurityUserDao getSpringSecurityUserDao() {
        if (m_userDao == null) {
            m_userDao = getFromRegistry(SpringSecurityUserDao.class);
        }
        return m_userDao;
    }

    public <T> T getFromRegistry(final Class<T> clazz) {
        if (m_context == null) {
            LOG.warn("No bundle context.  Unable to get class {} from the registry.", clazz);
            return null;
        }
        final ServiceReference<T> ref = m_context.getServiceReference(clazz);
        return m_context.getService(ref);
    }

    public static synchronized void setContext(final BundleContext context) {
        m_context = context;
    }
    
    public static synchronized BundleContext getContext() {
        return m_context;
    }
}
