/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
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

package org.opencadc.storage;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.auth.AuthorizationTokenPrincipal;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.rest.RestAction;
import ca.nrc.cadc.util.StringUtil;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.opencadc.storage.config.StorageConfiguration;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.storage.config.VOSpaceServiceConfigManager;
import org.opencadc.token.Client;
import org.opencadc.vospace.client.VOSpaceClient;


public abstract class StorageAction extends RestAction {

    private static final Logger LOGGER = Logger.getLogger(StorageAction.class);
    final StorageConfiguration storageConfiguration;
    private final VOSpaceServiceConfigManager voSpaceServiceConfigManager;
    VOSpaceServiceConfig currentService;

    /**
     * Empty constructor needed for Restlet to manage it.  Needs to be public.
     */
    public StorageAction() {
        this.storageConfiguration = new StorageConfiguration();
        this.voSpaceServiceConfigManager = new VOSpaceServiceConfigManager(this.storageConfiguration);
    }

    /**
     * Only used for testing as no Request is coming through to initialize it as it would in Production.
     *
     * @param storageConfiguration        The StorageConfiguration object.
     * @param voSpaceServiceConfigManager The VOSpaceServiceConfigManager object.
     */
    StorageAction(StorageConfiguration storageConfiguration, VOSpaceServiceConfigManager voSpaceServiceConfigManager) {
        this.storageConfiguration = storageConfiguration;
        this.voSpaceServiceConfigManager = voSpaceServiceConfigManager;
    }

    @Override
    public void initAction() throws Exception {
        this.currentService = this.voSpaceServiceConfigManager.getServiceConfig(getCurrentVOSpaceService());
    }

    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return null;
    }


    Path getCurrentPath() {
        final Path requestPath = getRequestPath();
        return requestPath.subpath(2, requestPath.getNameCount());
    }

    Path getRequestPath() {
        return Path.of(this.syncInput.getRequestPath());
    }

    StorageItemFactory getStorageItemFactory() {
        return new StorageItemFactory(this.syncInput.getContextPath(), this.currentService);
    }

    StorageItemContext getStorageItemType() {
        return StorageItemContext.valueOf(getRequestPath().getName(1).toString().toUpperCase());
    }

    String getCurrentVOSpaceService() {
        final String ret;

        final Path requestPath = getRequestPath();
        final String voSpaceService = requestPath.getName(0).toString();
        if (StringUtil.hasText(voSpaceService)) {
            if (getVOSpaceServiceList().contains(voSpaceService.toLowerCase())) {
                ret = voSpaceService;
            } else {
                String errMsg = "service not found in storage-ui configuration: " + voSpaceService;
                throw new IllegalArgumentException(errMsg);
            }
        } else {
            // no svc parameter found - return the current default
            ret = this.voSpaceServiceConfigManager.getDefaultServiceName();
        }

        return ret;
    }

    VOSpaceClient getVOSpaceClient() {
        return new VOSpaceClient(this.currentService.getResourceID());
    }

    List<String> getVOSpaceServiceList() {
        return this.voSpaceServiceConfigManager.getServiceList();
    }

    String getCurrentName() {
        return getCurrentPath().getFileName().toString();
    }

    protected Client getOIDCClient() throws IOException {
        return this.storageConfiguration.getOIDCClient();
    }

    protected Subject getCurrentSubject() throws Exception {
        final String rawCookieHeader = this.syncInput.getHeader("cookie");
        final Subject subject = AuthenticationUtil.getCurrentSubject();

        if (StringUtil.hasText(rawCookieHeader)) {
            final String[] firstPartyCookies = Arrays.stream(rawCookieHeader.split(";")).map(String::trim)
                                                     .filter(cookieString -> cookieString.startsWith(StorageConfiguration.FIRST_PARTY_COOKIE_NAME))
                                                     .toArray(String[]::new);

            if (firstPartyCookies.length > 0 && storageConfiguration.isOIDCConfigured()) {
                for (final String cookie : firstPartyCookies) {
                    // Only split on the first "=" symbol, and trim any wrapping double quotes
                    final String encryptedCookieValue = cookie.split("=", 2)[1].replaceAll("\"", "");

                    try {
                        final String accessToken = getOIDCClient().getAccessToken(encryptedCookieValue);

                        subject.getPrincipals().add(new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER,
                                                                                    AuthenticationUtil.CHALLENGE_TYPE_BEARER + " " + accessToken));
                        subject.getPublicCredentials().add(new AuthorizationToken(AuthenticationUtil.CHALLENGE_TYPE_BEARER, accessToken,
                                                                                  Collections.singletonList(URI.create(syncInput.getRequestURI()).getHost())));
                    } catch (NoSuchElementException noTokenForKeyInCacheException) {
                        LOGGER.warn("Cookie found and decrypted but no value in cache.  Ignoring cookie...");
                    }
                }

                if (!subject.getPrincipals(AuthorizationTokenPrincipal.class).isEmpty()) {
                    // Ensure it's clean first.
                    subject.getPublicCredentials(AuthMethod.class).forEach(authMethod -> subject.getPublicCredentials().remove(authMethod));
                    subject.getPublicCredentials().add(AuthMethod.TOKEN);
                }
            }
        }

        return subject;
    }

    protected String getDisplayName() throws Exception {
        final IdentityManager identityManager = AuthenticationUtil.getIdentityManager();
        return identityManager.toDisplayString(getCurrentSubject());
    }

    void redirectSeeOther(final String redirectURL) {
        this.syncOutput.setCode(HttpServletResponse.SC_SEE_OTHER);
        this.syncOutput.setHeader("location", redirectURL);
    }

    enum StorageItemContext {
        FILE, FOLDER, ITEM, LINK, LIST, RAW, PAGE, PKG
    }
}
