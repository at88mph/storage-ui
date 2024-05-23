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
import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.net.RemoteServiceException;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.rest.RestAction;
import ca.nrc.cadc.util.StringUtil;
import net.canfar.storage.PathUtils;
import org.opencadc.storage.config.StorageConfiguration;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.storage.config.VOSpaceServiceConfigManager;
import net.canfar.storage.web.view.StorageItem;
import org.apache.log4j.Logger;
import org.opencadc.token.Client;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.LinkNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeNotFoundException;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.VOSpaceClient;
import org.opencadc.vospace.client.async.RecursiveSetNode;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class StorageAction extends RestAction {
    private static final Logger LOGGER = Logger.getLogger(StorageAction.class);

    StorageItemFactory storageItemFactory;
    VOSpaceClient voSpaceClient;
    VOSpaceServiceConfig currentService;

    final StorageConfiguration storageConfiguration;
    private final VOSpaceServiceConfigManager voSpaceServiceConfigManager;

    /**
     * Empty constructor needed for Restlet to manage it.  Needs to be public.
     */
    public StorageAction() {
        this.storageConfiguration = new StorageConfiguration();
        this.voSpaceServiceConfigManager = new VOSpaceServiceConfigManager(this.storageConfiguration);
    }

    /**
     * Only used for testing as no Request is coming through to initialize it as it would in Production.
     * @param storageConfiguration          The StorageConfiguration object.
     * @param voSpaceServiceConfigManager   The VOSpaceServiceConfigManager object.
     * @param storageItemFactory            The StorageItemFactory object.
     * @param voSpaceClient                 The VOSpaceClient object.
     * @param serviceConfig                 The current VOSpace Service.
     */
    StorageAction(StorageConfiguration storageConfiguration,
                  VOSpaceServiceConfigManager voSpaceServiceConfigManager,
                  StorageItemFactory storageItemFactory,
                  VOSpaceClient voSpaceClient, VOSpaceServiceConfig serviceConfig) {
        this.storageConfiguration = storageConfiguration;
        this.voSpaceServiceConfigManager = voSpaceServiceConfigManager;
        this.storageItemFactory = storageItemFactory;
        this.voSpaceClient = voSpaceClient;
        this.currentService = serviceConfig;
    }

    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return null;
    }

    @Override
    public void initAction() throws Exception {
        if (this.currentService == null) {
            // TODO: This will throw an IllegalArgumentException if the specified services name does not exist
            // TODO: in this configuration, which will render as a 404 Not Found to the user.
            // TODO: Could redirect to a chooser instead?
            // TODO: jenkinsd 2024.05.20
            this.currentService = this.voSpaceServiceConfigManager.getServiceConfig(getCurrentVOSpaceService());
        }

        if (this.voSpaceClient == null) {
            this.voSpaceClient = new VOSpaceClient(this.currentService.getResourceID());
        }

        if (this.storageItemFactory == null) {
            this.storageItemFactory = new StorageItemFactory(this.syncInput.getContextPath(), this.currentService);
        }
    }

    Path getCurrentPath()  {
        final Path requestPath = getRequestPath();
        return requestPath.subpath(2, requestPath.getNameCount());
    }

    Path getRequestPath() {
        return Path.of(this.syncInput.getRequestPath());
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

    List<String> getVOSpaceServiceList() {
        return this.voSpaceServiceConfigManager.getServiceList();
    }

    VOSURI getCurrentItemURI() {
        return new VOSURI(URI.create(this.currentService.getNodeResourceID() + getCurrentPath().toString()));
    }

    String getCurrentName() {
        return getCurrentPath().getFileName().toString();
    }

    VOSURI toURI(final Path path) {
        return new VOSURI(URI.create(this.currentService.getNodeResourceID() + path.toString()));
    }

    VOSURI toURI(final Node node) {
        final Path path = PathUtils.toPath(node);
        return toURI(path);
    }

//    void setInheritedPermissions(final Path nodePath) throws Exception {
//        final ContainerNode parentNode = getCurrentNode();
//        final Node newNode = getNode(nodePath, null);
//        final Set<NodeProperty> newNodeProperties = newNode.getProperties();
//
//        // Clean slate.
//        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, ""));
//        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, ""));
//        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, ""));
//
//        final String parentReadGroupURIValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD);
//        if (StringUtil.hasText(parentReadGroupURIValue)) {
//            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, parentReadGroupURIValue));
//        }
//
//        final String parentWriteGroupURIValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE);
//
//        if (StringUtil.hasText(parentWriteGroupURIValue)) {
//            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, parentWriteGroupURIValue));
//        }
//
//        final String isPublicValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_ISPUBLIC);
//        if (StringUtil.hasText(isPublicValue)) {
//            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, isPublicValue));
//        }
//
//        setNodeSecure(newNode);
//    }

    /**
     * Perform the HTTPS command to recursively set permissions for a node.
     * Returns when job is complete, OR a maximum of (15) seconds has elapsed.
     * If timeout has been reached, job will continue to run until is cancelled.
     *
     * @param newNode The Node whose permissions are to be recursively set
     */
    void setNodeRecursiveSecure(final Node newNode) throws Exception {
        try {
            Subject.doAs(getCurrentSubject(), (PrivilegedExceptionAction<Void>) () -> {
                final RecursiveSetNode rj = voSpaceClient.createRecursiveSetNode(toURI(newNode), newNode);

                // Fire & forget is 'false'. 'true' will mean the run job does not return until it's finished.
                rj.setMonitor(false);
                rj.run();

                return null;
            });
        } catch (PrivilegedActionException pae) {
            throw new IOException(pae.getException());
        }
    }

    protected Client getOIDCClient() throws IOException {
        return this.storageConfiguration.getOIDCClient();
    }

    protected Subject getCurrentSubject() throws Exception {
        final String rawCookieHeader = this.syncInput.getHeader("cookie");
        final Subject subject = AuthenticationUtil.getCurrentSubject();

        if (StringUtil.hasText(rawCookieHeader)) {
            final String[] firstPartyCookies =
                    Arrays.stream(rawCookieHeader.split(";"))
                          .map(String::trim)
                          .filter(cookieString -> cookieString.startsWith(
                                  StorageConfiguration.FIRST_PARTY_COOKIE_NAME))
                          .toArray(String[]::new);

            if (firstPartyCookies.length > 0 && storageConfiguration.isOIDCConfigured()) {
                for (final String cookie : firstPartyCookies) {
                    // Only split on the first "=" symbol, and trim any wrapping double quotes
                    final String encryptedCookieValue =
                            cookie.split("=", 2)[1].replaceAll("\"", "");

                    try {
                        final String accessToken = getOIDCClient().getAccessToken(encryptedCookieValue);

                        subject.getPrincipals().add(new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER,
                                                                                    AuthenticationUtil.CHALLENGE_TYPE_BEARER
                                                                                    + " " + accessToken));
                        subject.getPublicCredentials().add(
                                new AuthorizationToken(AuthenticationUtil.CHALLENGE_TYPE_BEARER, accessToken,
                                                       Collections.singletonList(
                                                               URI.create(syncInput.getRequestURI()).getHost())));
                    } catch (NoSuchElementException noTokenForKeyInCacheException) {
                        LOGGER.warn("Cookie found and decrypted but no value in cache.  Ignoring cookie...");
                    }
                }

                if (!subject.getPrincipals(AuthorizationTokenPrincipal.class).isEmpty()) {
                    // Ensure it's clean first.
                    subject.getPublicCredentials(AuthMethod.class)
                           .forEach(authMethod -> subject.getPublicCredentials().remove(authMethod));
                    subject.getPublicCredentials().add(AuthMethod.TOKEN);
                }
            }
        }

        return subject;
    }

    /**
     * Perform the HTTPS command.
     *
     * @param newNode The newly created Node.
     */
    void setNodeSecure(final Node newNode) throws Exception {
        executeSecurely((PrivilegedExceptionAction<Void>) () -> {
            voSpaceClient.setNode(toURI(newNode), newNode);
            return null;
        });
    }


    void createLink(final URI target) throws Exception {
        createNode(toLinkNode(target));
    }

    private LinkNode toLinkNode(final URI target) {
        final Path path = getCurrentPath();
        final LinkNode linkNode = new LinkNode(path.getFileName().toString(), target);
        PathUtils.augmentParents(path, linkNode);

        return linkNode;
    }

    void createFolder() throws Exception {
        createNode(toContainerNode());
    }

    private ContainerNode toContainerNode() {
        final ContainerNode containerNode = new ContainerNode(getCurrentName());
        PathUtils.augmentParents(getCurrentPath(), containerNode);

        return containerNode;
    }

    void createNode(final Node newNode) throws Exception {
        executeSecurely((PrivilegedExceptionAction<Void>) () -> {
            voSpaceClient.createNode(toURI(newNode), newNode, false);
            return null;
        });
    }

    <T> T executeSecurely(final PrivilegedExceptionAction<T> runnable) throws Exception {
        try {
            return executeSecurely(getCurrentSubject(), runnable);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
    }

    <T> T executeSecurely(final Subject subject, final PrivilegedExceptionAction<T> runnable) throws Exception {
        try {
            return Subject.doAs(subject, runnable);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
    }

    enum StorageItemContext {
        FILE, FOLDER, LINK, LIST
    }
}
