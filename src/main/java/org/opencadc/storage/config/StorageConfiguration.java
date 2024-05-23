/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2023.                            (c) 2023.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package org.opencadc.storage.config;

import ca.nrc.cadc.ac.client.GMSClient;
import ca.nrc.cadc.accesscontrol.AccessControlClient;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.LocalAuthority;
import ca.nrc.cadc.util.StringUtil;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.MergeCombiner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.opencadc.token.Client;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class StorageConfiguration {
    private static final Logger LOGGER = LogManager.getLogger(StorageConfiguration.class);
    public static final String FIRST_PARTY_COOKIE_NAME = "__Host-storage-ui-auth";

    private static final String DEFAULT_CONFIG_FILE_PATH =
            System.getProperty("user.home") + "/config/org.opencadc.vosui.properties";
    private final Configuration configuration;

    public StorageConfiguration() {
        final CombinedConfiguration combinedConfiguration = new CombinedConfiguration(new MergeCombiner());
        final String filePath = StorageConfiguration.DEFAULT_CONFIG_FILE_PATH;

        // Prefer System properties.
        combinedConfiguration.addConfiguration(new SystemConfiguration());

        final Parameters parameters = new Parameters();
        final FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                        .configure(parameters.properties().setFileName(filePath));

        try {
            combinedConfiguration.addConfiguration(builder.getConfiguration());
        } catch (ConfigurationException exception) {
            LOGGER.error(String.format("No configuration found at %s.\nUsing defaults.", filePath));
        }

        this.configuration = combinedConfiguration;
    }


    public String getDefaultServiceName() {
        return lookup(StorageConfigurationKey.DEFAULT_SERVICE_NAME);
    }

    public String[] getServiceNames() {
        return lookupStringArray(StorageConfigurationKey.SERVICE_NAME);
    }

    public URI getGMSServiceURI() {
        final LocalAuthority localAuthority = new LocalAuthority();
        return localAuthority.getServiceURI(Standards.GMS_SEARCH_10.toASCIIString());
    }

    public String getTokenCacheURLString() {
        return lookup(StorageConfigurationKey.TOKEN_CACHE_URL);
    }

    public String getThemeName() {
        return lookup(StorageConfigurationKey.THEME_NAME, "canfar");
    }

    public String getOIDCClientID() {
        return lookup(StorageConfigurationKey.OIDC_CLIENT_ID);
    }

    public String getOIDCClientSecret() {
        return lookup(StorageConfigurationKey.OIDC_CLIENT_SECRET);
    }

    public String getOIDCCallbackURI() {
        return lookup(StorageConfigurationKey.OIDC_CALLBACK_URI);
    }

    public String getOIDCRedirectURI() {
        return lookup(StorageConfigurationKey.OIDC_REDIRECT_URI);
    }

    public String getOIDCScope() {
        return lookup(StorageConfigurationKey.OIDC_SCOPE);
    }

    public boolean isOIDCConfigured() {
        return StringUtil.hasText(getOIDCClientID()) && StringUtil.hasText(getOIDCClientSecret())
               && StringUtil.hasText(getOIDCCallbackURI()) && StringUtil.hasText(getOIDCScope())
               && StringUtil.hasText(getTokenCacheURLString());
    }

    public Client getOIDCClient() throws IOException {
        return new Client(getOIDCClientID(), getOIDCClientSecret(),
                          new URL(getOIDCCallbackURI()), new URL(getOIDCRedirectURI()),
                          getOIDCScope().split(" "), getTokenCacheURLString());
    }

    public GMSClient getGMSClient() {
        return new GMSClient(getGMSServiceURI());
    }

    public URI getGroupURI(final String groupName) {
        return URI.create(getGMSServiceURI() + "?" + groupName);
    }

    public AccessControlClient getAccessControlClient() {
        return new AccessControlClient(getGMSServiceURI());
    }

    String lookup(final StorageConfigurationKey key) {
        return lookup(key.propertyName, key.required);
    }

    String lookup(final StorageConfigurationKey key, final String defaultValue) {
        return lookup(key.propertyName, defaultValue);
    }

    public String lookup(final String propertyName, final boolean required) {
        final String propertyValue = configuration.getString(propertyName);
        if (propertyValue == null && required) {
            throw new IllegalStateException(
                    "Required value " + propertyName + " not found in application config.");
        }

        return propertyValue;
    }

    public boolean lookupFlag(final String propertyName, final boolean required) {
        if (required && !configuration.containsKey(propertyName)) {
            throw new IllegalStateException(
                    "Required value " + propertyName + " not found in application config.");
        }

        return configuration.getBoolean(propertyName, false);
    }

    String lookup(final String propertyName, final String defaultValue) {
        final String propertyValue = configuration.getString(propertyName);
        return propertyValue == null ? defaultValue : propertyValue;
    }

    String[] lookupStringArray(final StorageConfigurationKey key) {
        return lookupStringArray(key.propertyName, key.required);
    }

    public String[] lookupStringArray(final String key, final boolean required) {
        final String[] propertyValues = configuration.getStringArray(key);

        if (propertyValues == null && required) {
            throw new IllegalStateException(
                    "Required value " + key + " not found in application config.");
        }

        return propertyValues;
    }

    enum StorageConfigurationKey {
        DEFAULT_SERVICE_NAME("org.opencadc.vosui.service.default", true),
        SERVICE_NAME("org.opencadc.vosui.service.name", true),
        THEME_NAME("org.opencadc.vosui.theme.name", false),
        TOKEN_CACHE_URL("org.opencadc.vosui.tokenCache.url", false),
        OIDC_CLIENT_ID("org.opencadc.vosui.oidc.clientID", false),
        OIDC_CLIENT_SECRET("org.opencadc.vosui.oidc.clientSecret", false),
        OIDC_REDIRECT_URI("org.opencadc.vosui.oidc.redirectURI", false),
        OIDC_CALLBACK_URI("org.opencadc.vosui.oidc.callbackURI", false),
        OIDC_SCOPE("org.opencadc.vosui.oidc.scope", false);

        private final String propertyName;
        private final boolean required;

        StorageConfigurationKey(String propertyName, boolean required) {
            this.propertyName = propertyName;
            this.required = required;
        }
    }
}
