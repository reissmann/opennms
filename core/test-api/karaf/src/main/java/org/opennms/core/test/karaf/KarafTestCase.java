/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.core.test.karaf;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class KarafTestCase {

    private static Logger LOG = LoggerFactory.getLogger(KarafTestCase.class);

    private static String getKarafVersion() {
        final String karafVersion = System.getProperty("karafVersion", "2.4.0");
        Objects.requireNonNull(karafVersion, "Please define a system property 'karafVersion'.");
        return karafVersion;
    }

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featuresService;

    /**
     * This {@link ProbeBuilder} can be used to add OSGi metadata to the test
     * probe bundle. We only use it to give the bundle a nice human-readable name
     * of "org.opennms.core.test.karaf.test".
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.BUNDLE_SYMBOLICNAME, "org.opennms.core.test.karaf.test");
        //probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.opennms.core.test.karaf,*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    /**
     * This is the default {@link Configuration} for any Pax Exam tests that
     * use this abstract base class. If you wish to add more Configuration parameters,
     * you should call {@link #configAsList()}, append the {@link Option} values
     * to the list, and then return it in a {@link Configuration} function that
     * overrides {@link #config()}.
     */
    @Configuration
    public Option[] config() {
        return configAsArray();
    }

    protected List<Option> configAsList() {
        return new ArrayList<Option>(Arrays.asList(configAsArray()));
    }

    protected Option[] configAsArray() {
        Option[] options = new Option[]{
            // Use Karaf as the container
            karafDistributionConfiguration().frameworkUrl(
                maven()
                    .groupId("org.apache.karaf")
                    .artifactId("apache-karaf")
                    .type("tar.gz")
                    .version(getKarafVersion()))
                .karafVersion(getKarafVersion())
                .name("Apache Karaf")
                .unpackDirectory(new File("target/paxexam/")
            )
                // Turn off using the deploy folder or stream bundle provisioning
                // won't happen before the probe bundle executes, causing problems
                // like {@link NoClassDefFoundError}.
                .useDeployFolder(false), 

            // Pack this parent class from src/main/java into a stream bundle
            // so that it is accessible inside the container
            provision(
                 bundle()
                     .add(KarafTestCase.class)
                     .set(Constants.BUNDLE_SYMBOLICNAME, "org.opennms.core.test.karaf")
                     .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
                     .set(Constants.EXPORT_PACKAGE, "org.opennms.core.test.karaf")
                     .build()
            ),

            //keepRuntimeFolder(),

            // Set logging to INFO
            logLevel(LogLevelOption.LogLevel.INFO),

            // We overwrite mvn repository settings, because we want a "naked" repository
            editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories", ""),
            editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.defaultRepositories", "file:${karaf.home}/${karaf.default.repository}@snapshots@id=karaf.${karaf.default.repository}"),
            editConfigurationFileExtend("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.defaultRepositories", "file:${karaf.home}/../test-repo@snapshots@id=default-repo"),
            editConfigurationFileExtend("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", "file:${karaf.home}/../opennms-repo@snapshots@id=opennms-repo"),

            //editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "config,ssh,http,http-whiteboard,exam"),

            // Change the SSH port so that it doesn't conflict with a running OpenNMS instance
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", "8201")
        };

        if (Boolean.valueOf(System.getProperty("debug"))) {
            options = Arrays.copyOf(options, options.length + 1);
            options[options.length -1] = debugConfiguration("8889", true);
        }
        return options;
    }

    protected void addFeaturesUrl(String url) {
        try {
            featuresService.addRepository(URI.create(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void installFeature(String featureName) {
        try {
            LOG.info("Installing feature {}", featureName);
            featuresService.installFeature(featureName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command
     * @return
     */
    protected String executeCommand(final String command) {
        try (
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        ) {
            Subject subject = new Subject();
            subject.getPrincipals().add(new RolePrincipal("admin"));
            return Subject.doAs(subject, new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
                    final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
                    LOG.info("{}", command);
                    Object response = commandSession.execute(command);
                    LOG.info("Response: {}", response);
                    printStream.flush();
                    return byteArrayOutputStream.toString();
                }
            });
        } catch (Exception e) {
            LOG.error("Error while executing command", e);
            throw new RuntimeException(e);
        }
    }

    protected <T> T getOsgiService(Class<T> type) {
        ServiceReference<T> serviceReference = bundleContext.getServiceReference(type);
        if (serviceReference != null) {
            return type.cast(bundleContext.getService(serviceReference));
        }
        return null;
    }
}
