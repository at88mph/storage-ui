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

package net.canfar.storage.web.resources;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.net.RemoteServiceException;
import ca.nrc.cadc.util.StringUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import net.canfar.storage.PathUtils;
import net.canfar.storage.web.StorageItemFactory;
import net.canfar.storage.web.config.StorageConfiguration;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import net.canfar.storage.web.config.VOSpaceServiceConfigManager;
import net.canfar.storage.web.restlet.StorageApplication;
import net.canfar.storage.web.view.StorageItem;
import org.json.JSONObject;
import org.opencadc.gms.GroupURI;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.LinkNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeNotFoundException;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.VOSpaceClient;
import org.opencadc.vospace.client.async.RecursiveSetNode;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;


public class StorageItemServerResource extends SecureServerResource {
    // Page size for the initial page display.
    private static final int DEFAULT_DISPLAY_PAGE_SIZE = 35;
    private final VOSpaceServiceConfigManager voSpaceServiceConfigManager;
    StorageItemFactory storageItemFactory;
    VOSpaceClient voSpaceClient;
    VOSpaceServiceConfig currentService;

    /**
     * Empty constructor needed for Restlet to manage it.  Needs to be public.
     */
    public StorageItemServerResource() {
        this.voSpaceServiceConfigManager = new VOSpaceServiceConfigManager(storageConfiguration);
    }

    /**
     * Only used for testing as no Request is coming through to initialize it as it would in Production.
     *
     * @param storageConfiguration        The StorageConfiguration object.
     * @param voSpaceServiceConfigManager The VOSpaceServiceConfigManager object.
     * @param storageItemFactory          The StorageItemFactory object.
     * @param voSpaceClient               The VOSpaceClient object.
     * @param serviceConfig               The current VOSpace Service.
     */
    StorageItemServerResource(StorageConfiguration storageConfiguration,
                              VOSpaceServiceConfigManager voSpaceServiceConfigManager,
                              StorageItemFactory storageItemFactory,
                              VOSpaceClient voSpaceClient, VOSpaceServiceConfig serviceConfig) {
        super(storageConfiguration);
        this.voSpaceServiceConfigManager = voSpaceServiceConfigManager;
        this.storageItemFactory = storageItemFactory;
        this.voSpaceClient = voSpaceClient;
        this.currentService = serviceConfig;
    }

    /**
     * Complete constructor for testing.
     *
     * @param voSpaceClient The VOSpace Client to use.
     */
    StorageItemServerResource(final VOSpaceClient voSpaceClient, final VOSpaceServiceConfig serviceConfig) {
        this();
        this.currentService = serviceConfig;
        this.voSpaceClient = voSpaceClient;
        initializeStorageItemFactory();
    }


    /**
     * Set-up method.  This ensures there is a context first before pulling
     * out some necessary objects for further work.
     * <p></p>
     * Tester
     */
    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        this.currentService = this.voSpaceServiceConfigManager.getServiceConfig(getCurrentVOSpaceService());
        this.voSpaceClient = new VOSpaceClient(this.currentService.getResourceID());
        initializeStorageItemFactory();
    }

    private void initializeStorageItemFactory() {
        final ServletContext servletContext = getServletContext();
        this.storageItemFactory = new StorageItemFactory((servletContext == null)
                                                             ? StorageApplication.DEFAULT_CONTEXT_PATH
                                                             : servletContext.getContextPath(),
                                                         this.currentService);
    }

    Path getCurrentPath() {
        if (getRequestAttributes().containsKey("path")) {
            final String pathInRequest = getRequestAttribute("path");
            return Paths.get(pathInRequest);
        } else {
            // Assume root.
            return Paths.get("/");
        }
    }

    String getCurrentVOSpaceService() {
        final String ret;

        if (getRequestAttributes().containsKey("svc")) {
            final String vospaceService = getRequestAttribute("svc");
            if (getVOSpaceServiceList().contains(vospaceService.toLowerCase())) {
                ret = vospaceService;
            } else {
                String errMsg = "service not found in vosui configuration: " + vospaceService;
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

    private <T extends Node> T getCurrentNode() {
        return getCurrentNode(VOS.Detail.max);
    }

    final <T extends Node> T getCurrentNode(final VOS.Detail detail) {
        return getNode(getCurrentPath(), detail);
    }

    @SuppressWarnings("unchecked")
    <T extends Node> T getNode(final Path nodePath, final VOS.Detail detail, final Integer limit)
        throws ResourceException {
        final Map<String, Object> queryPayload = new HashMap<>();
        if (limit != null) {
            queryPayload.put("limit", limit);
        }

        if (detail != null) {
            queryPayload.put("detail", detail.name());
        }

        final String query = queryPayload.entrySet().stream()
                                         .map(entry -> entry.getKey() + "=" + entry.getValue())
                                         .collect(Collectors.joining("&"));

        try {
            final T currentNode = executeSecurely(() -> (T) voSpaceClient.getNode(nodePath.toString(), query));
            if (currentNode != null) {
                PathUtils.augmentParents(nodePath, currentNode);
            }
            return currentNode;
        } catch (IllegalArgumentException e) {
            // Very specific hack to try again without the (possibly) unsupported limit parameter.
            if (limit != null
                && e.getMessage().startsWith("OptionNotSupported")) {
                return getNode(nodePath, detail, null);
            } else {
                throw new ResourceException(e);
            }
        } catch (RemoteServiceException remoteServiceException) {
            // For Cavern backend systems, some kind of authenticated access is required, and a RemoteServiceException
            // is thrown as a result, which results in a 500 Server Error.  Catch it here to re-map it as an
            // authentication problem and encourage the User to sign in.
            // jenkinsd 2024.02.03
            //
            if (remoteServiceException.getMessage().contains("PosixMapperClient.augment(Subject)")) {
                throw new NotAuthenticatedException("Nobody is authenticated.");
            } else {
                throw remoteServiceException;
            }
        } catch (RuntimeException runtimeException) {
            // Working around a bug where the RegistryClient improperly handles an unauthenticated request.
            if (runtimeException.getCause() != null) {
                if (runtimeException.getCause() instanceof IOException
                    && runtimeException.getCause().getCause() != null
                    && (runtimeException.getCause().getCause() instanceof NotAuthenticatedException)) {
                    throw new ResourceException(runtimeException.getCause().getCause());
                } else if (runtimeException.getCause().getMessage().contains("PosixMapperClient.augment(Subject)")) {
                    // More hacks for the base call being unauthenticated.
                    throw new NotAuthenticatedException("Nobody is authenticated.");
                } else {
                    throw runtimeException;
                }
            } else {
                throw runtimeException;
            }
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    <T extends Node> T getNode(final Path nodePath, final VOS.Detail detail) throws ResourceException {
        final int pageSize;

        if ((detail == VOS.Detail.max) || (detail == VOS.Detail.raw)) {
            pageSize = DEFAULT_DISPLAY_PAGE_SIZE;
        } else {
            pageSize = 0;
        }

        return getNode(nodePath, detail, pageSize);
    }

    VOSURI toURI(final Path path) {
        return new VOSURI(URI.create(this.currentService.getNodeResourceID() + path.toString()));
    }

    VOSURI toURI(final Node node) {
        final Path path = PathUtils.toPath(node);
        return toURI(path);
    }

    void setInheritedPermissions(final Path nodePath) throws Exception {
        final ContainerNode parentNode = getCurrentNode();
        final Node newNode = getNode(nodePath, null);
        final Set<NodeProperty> newNodeProperties = newNode.getProperties();

        // Clean slate.
        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, ""));
        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, ""));
        newNodeProperties.remove(new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, ""));

        final String parentReadGroupURIValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_GROUPREAD);
        if (StringUtil.hasText(parentReadGroupURIValue)) {
            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, parentReadGroupURIValue));
        }

        final String parentWriteGroupURIValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_GROUPWRITE);

        if (StringUtil.hasText(parentWriteGroupURIValue)) {
            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, parentWriteGroupURIValue));
        }

        final String isPublicValue = parentNode.getPropertyValue(VOS.PROPERTY_URI_ISPUBLIC);
        if (StringUtil.hasText(isPublicValue)) {
            newNodeProperties.add(new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, isPublicValue));
        }

        setNodeSecure(newNode);
    }

    /**
     * Resolve this link Node's target to its final destination.  This method
     * will follow the target of the provided LinkNode, and continue to do so
     * until an external URL is found, or Node that is not a Link Node.
     * <p></p>
     * Finally, this method will redirect to the appropriate endpoint.
     *
     * @throws Exception For any parsing errors.
     */
    void resolveLink() throws Exception {
        final LinkNode linkNode = getCurrentNode(null);
        final URI resolvedURI = resolveLink(linkNode);
        getResponse().redirectTemporary(resolvedURI.toString());
    }

    /**
     * Resolve the given LinkNode's target URI and return it.
     *
     * @param linkNode The LinkNode to resolve.
     * @return URI of the target.
     * @throws NodeNotFoundException If the target is not found.
     */
    private URI resolveLink(final LinkNode linkNode) throws NodeNotFoundException {
        final URI endPoint;
        final URI targetURI = linkNode.getTarget();

        // Should ALWAYS be true for a LinkNode!
        if (targetURI == null) {
            throw new IllegalArgumentException("**BUG**: LinkNode has a null target!");
        } else {
            try {
                final VOSURI vosURI = new VOSURI(targetURI);
                final Node targetNode = getNode(Paths.get(vosURI.getPath()), null);

                if (targetNode == null) {
                    throw new NodeNotFoundException("No target found or broken link for node: " + linkNode.getName());
                } else {
                    if (targetNode instanceof LinkNode) {
                        endPoint = resolveLink((LinkNode) targetNode);
                    } else {
                        final StorageItem storageItem = storageItemFactory.translate(targetNode);
                        endPoint = URI.create(storageItem.getTargetPath());
                    }
                }
            } catch (IllegalArgumentException | URISyntaxException e) {
                // Not a VOSpace URI, so return this URI.
                return targetURI;
            }
        }

        return endPoint;
    }

    /**
     * Perform the HTTPS command to recursively set permissions for a node.
     * Returns when job is complete, OR a maximum of (15) seconds has elapsed.
     * If timeout has been reached, job will continue to run until is cancelled.
     *
     * @param newNode The Node whose permissions are to be recursively set
     */
    private void setNodeRecursiveSecure(final Node newNode) throws Exception {
        try {
            Subject.doAs(getVOSpaceCallingSubject(), (PrivilegedExceptionAction<Void>) () -> {
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


    /**
     * Perform the HTTPS command.
     *
     * @param newNode The newly created Node.
     */
    private void setNodeSecure(final Node newNode) throws Exception {
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
            return executeSecurely(getVOSpaceCallingSubject(), runnable);
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

    String getDisplayName() throws Exception {
        final IdentityManager identityManager = AuthenticationUtil.getIdentityManager();
        return identityManager.toDisplayString(getVOSpaceCallingSubject());
    }

    /**
     * Convenience method to obtain a Subject targeted for the current VOSpace backend.  When using Tokens, for example, the AuthenticationToken instance
     * in the Subject's Public Credentials will contain the domain of the backend VOSpace API.
     * @return  Subject instance.  Never null.
     */
    Subject getVOSpaceCallingSubject() throws Exception {
        return super.getCallingSubject(new URL(this.voSpaceClient.getBaseURL()));
    }

    @Delete
    public void deleteNode() throws Exception {
        executeSecurely((PrivilegedExceptionAction<Void>) () -> {
            voSpaceClient.deleteNode(getCurrentPath().toString());
            return null;
        });
    }

    @Post("json")
    public void update(final JsonRepresentation payload) throws Exception {
        final JSONObject jsonObject = payload.getJsonObject();

        // limit=0, detail=min so should only get the current node
        final Node currentNode = getCurrentNode(VOS.Detail.properties);
        final Set<String> keySet = jsonObject.keySet();

        if (keySet.contains("publicPermission")) {
            currentNode.isPublic = jsonObject.get("publicPermission").equals("on");
        } else {
            currentNode.isPublic = false;
            currentNode.clearIsPublic = true;
        }

        currentNode.getReadOnlyGroup().clear();
        if (keySet.contains("readGroup") && StringUtil.hasText(jsonObject.getString("readGroup"))) {
            final GroupURI newReadGroupURI =
                new GroupURI(storageConfiguration.getGroupURI(jsonObject.getString("readGroup")));
            currentNode.getReadOnlyGroup().add(newReadGroupURI);
        } else {
            currentNode.clearReadOnlyGroups = true;
        }

        currentNode.getReadWriteGroup().clear();
        if (keySet.contains("writeGroup") && StringUtil.hasText(jsonObject.getString("writeGroup"))) {
            final GroupURI newReadWriteGroupURI =
                new GroupURI(storageConfiguration.getGroupURI(jsonObject.getString("writeGroup")));
            currentNode.getReadWriteGroup().add(newReadWriteGroupURI);
        } else {
            currentNode.clearReadWriteGroups = true;
        }

        // Recursively set permissions if requested
        if (keySet.contains("recursive")) {
            if (jsonObject.get("recursive").equals("on")) {
                setNodeRecursiveSecure(currentNode);
                getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            }
        } else {
            // Update the node properties
            setNodeSecure(currentNode);
            getResponse().setStatus(Status.SUCCESS_OK);
        }
    }
}
