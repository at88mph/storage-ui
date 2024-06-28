/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2024.                            (c) 2024.
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

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.util.StringUtil;
import net.canfar.storage.PathUtils;
import net.canfar.storage.web.*;
import net.canfar.storage.web.config.StorageConfiguration;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import net.canfar.storage.web.config.VOSpaceServiceConfigManager;
import net.canfar.storage.web.restlet.JSONRepresentation;

import javax.security.auth.Subject;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONWriter;
import org.opencadc.vospace.DataNode;
import org.opencadc.vospace.NodeNotFoundException;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.View;
import org.opencadc.vospace.client.ClientTransfer;
import org.opencadc.vospace.client.VOSClientUtil;
import org.opencadc.vospace.client.VOSpaceClient;
import org.opencadc.vospace.server.Utils;
import org.opencadc.vospace.transfer.Direction;
import org.opencadc.vospace.transfer.Protocol;
import org.opencadc.vospace.transfer.Transfer;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class FileItemServerResource extends StorageItemServerResource {

    private static final Logger LOGGER = Logger.getLogger(FileItemServerResource.class);

    private static final int BUFFER_SIZE = 8192;
    private static final String UPLOAD_FILE_KEY = "upload";

    private final UploadVerifier uploadVerifier;
    private final FileValidator fileValidator;

    private final static AuthMethod[] PROTOCOL_AUTH_METHODS = new AuthMethod[] {
            AuthMethod.ANON,
            AuthMethod.CERT,
            AuthMethod.COOKIE
    };


    FileItemServerResource(StorageConfiguration storageConfiguration,
                           VOSpaceServiceConfigManager voSpaceServiceConfigManager,
                           StorageItemFactory storageItemFactory, VOSpaceClient voSpaceClient,
                           VOSpaceServiceConfig serviceConfig, UploadVerifier uploadVerifier,
                           FileValidator fileValidator) {
        super(storageConfiguration, voSpaceServiceConfigManager, storageItemFactory, voSpaceClient, serviceConfig);
        this.uploadVerifier = uploadVerifier;
        this.fileValidator = fileValidator;
    }

    public FileItemServerResource() {
        this.uploadVerifier = new UploadVerifier();
        this.fileValidator = new RegexFileValidator();
    }

    @Get
    public void download() throws Exception {
        final DataNode dataNode = getNode(getCurrentPath(), null, null);
        download(dataNode);
    }

    void download(final DataNode dataNode) throws Exception {
        final Subject subject = getVOSpaceCallingSubject();
        final VOSURI dataNodeVOSURI = toURI(dataNode);
        final AuthMethod authMethod = AuthenticationUtil.getAuthMethodFromCredentials(subject);
        final URL baseURL = lookupDownloadEndpoint(dataNodeVOSURI.getServiceURI(), authMethod);

        // Special handling for tokenized pre-auth URLs
        if (Objects.requireNonNull(authMethod) == AuthMethod.TOKEN) {
            final Set<AuthorizationToken> accessTokens = subject.getPublicCredentials(AuthorizationToken.class);
            if (accessTokens.isEmpty()) {
                redirectSeeOther(baseURL + dataNodeVOSURI.getPath());
            } else {
                redirectSeeOther(toEndpoint(dataNodeVOSURI.getURI()));
            }
        } else {
            redirectSeeOther(baseURL + dataNodeVOSURI.getPath());
        }
        LOGGER.debug("Download URL: " + baseURL);
    }

    /**
     * Check both the new prototype and old Files service lookup endpoints.
     * @param serviceURI    The URI that identifies the Service to use.
     * @param authMethod    The AuthMethod interface to pick out.
     * @return  URL, never null.
     * @throws IllegalStateException if no URL can be found.
     */
    private URL lookupDownloadEndpoint(final URI serviceURI, final AuthMethod authMethod) {
        final URI[] downloadEndpointStandards = new URI[] {
                Standards.VOSPACE_FILES,
                Standards.VOSPACE_FILES_20
        };

        for (final URI uri : downloadEndpointStandards) {
            final URL serviceURL = lookupDownloadEndpoint(serviceURI, uri, authMethod);
            if (serviceURL != null) {
                return serviceURL;
            }
        }

        throw new IllegalStateException("Incomplete configuration in the registry.  No endpoint for "
                                        + serviceURI + " could be found from ("
                                        + Arrays.toString(downloadEndpointStandards) + ")");
    }

    private URL lookupDownloadEndpoint(final URI serviceURI, final URI capabilityStandardURI,
                                       final AuthMethod authMethod) {
        return getRegistryClient().getServiceURL(serviceURI, capabilityStandardURI, authMethod);
    }

    String toEndpoint(final URI downloadURI) {
        final Transfer transfer = new Transfer(downloadURI, Direction.pullFromVoSpace);
        transfer.setView(new View(VOS.VIEW_DEFAULT));
        transfer.getProtocols().add(new Protocol(VOS.PROTOCOL_HTTPS_GET));
        transfer.version = VOS.VOSPACE_21;

        final ClientTransfer ct = voSpaceClient.createTransfer(transfer);
        return ct.getTransfer().getEndpoint();
    }

    @Post
    @Put
    public void accept(final Representation payload) throws Exception {
        if ((payload != null) && MediaType.MULTIPART_FORM_DATA.equals(payload.getMediaType(), true)) {
            // The Apache FileUpload project parses HTTP requests which
            // conform to RFC 1867, "Form-based File Upload in HTML". That
            // is, if an HTTP request is submitted using the POST method,
            // and with a content type of "multipart/form-data", then
            // FileUpload can parse that request, and get all uploaded files
            // as FileItem.

            // Obtain the file upload Representation as an iterator.
            final ServletFileUpload upload = parseRepresentation();
            final FileItemIterator fileItemIterator = upload.getItemIterator(ServletUtils.getRequest(getRequest()));

            if (!fileItemIterator.hasNext()) {
                // Some problem occurs, sent back a simple line of text.
                uploadError(Status.CLIENT_ERROR_BAD_REQUEST,
                            "Unable to upload corrupted or incompatible data.");
            } else {
                upload(fileItemIterator);
            }
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            getResponse().setEntity("Nothing to upload or invalid data.", MediaType.TEXT_PLAIN);
        }
    }

    /**
     * Upload the given items from the iterator.
     *
     * @param fileItemIterator Iterator of file items to upload.
     * @throws Exception Any errors during permission setting or uploading over the network.
     */
    protected void upload(final FileItemIterator fileItemIterator) throws Exception {
        boolean inheritParentPermissions = false;
        Path newNodePath = null;

        try {
            while (fileItemIterator.hasNext()) {
                final FileItemStream nextFileItemStream = fileItemIterator.next();

                if (nextFileItemStream.getFieldName().startsWith(UPLOAD_FILE_KEY)) {
                    newNodePath = upload(nextFileItemStream);
                } else if (nextFileItemStream.getFieldName().equals("inheritPermissionsCheckBox")) {
                    inheritParentPermissions = true;
                }
            }
        } catch (FileUploadException e) {
            throw new IOException(e);
        }

        if (inheritParentPermissions) {
            setInheritedPermissions(newNodePath);
        }
    }

    /**
     * Perform the actual upload.
     *
     * @param fileItemStream The upload file item stream.
     * @return The Path to the new Node.
     * @throws IOException If anything goes wrong.
     */
    Path upload(final FileItemStream fileItemStream) throws Exception {
        final String filename = fileItemStream.getName();

        if (fileValidator.validateFileName(filename)) {
            final DataNode dataNode = new DataNode(filename);
            PathUtils.augmentParents(Paths.get(getCurrentPath().toString(), filename), dataNode);

            final String contentType = fileItemStream.getContentType();

            try (final InputStream inputStream = fileItemStream.openStream()) {
                upload(inputStream, dataNode, contentType);
            }

            return PathUtils.toPath(dataNode);
        } else {
            throw new ResourceException(new IllegalArgumentException(
                    String.format("Invalid file name: %s -- File name must match %s.", filename,
                                  fileValidator.getRule())));
        }
    }

    /**
     * Do the secure upload.
     *
     * @param inputStream The InputStream to pull from.
     * @param dataNode    The DataNode to upload to.
     * @param contentType           The file content type.
     */
    protected void upload(final InputStream inputStream, final DataNode dataNode, final String contentType)
            throws Exception {
        final UploadOutputStreamWrapper outputStreamWrapper = new UploadOutputStreamWrapperImpl(inputStream,
                                                                                                BUFFER_SIZE);

        try {
            // Due to a bug in VOSpace that returns a 400 while checking
            // for an existing Node, we will work around it by checking manually
            // rather than looking for a NodeNotFoundException as expected, and
            // return the 409 code, while maintaining backward compatibility with the catch below.
            // jenkinsd 2016.07.25
            getNode(Paths.get(Utils.getPath(dataNode)), null);
        } catch (ResourceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IllegalStateException) {
                final Throwable illegalStateCause = cause.getCause();
                if ((illegalStateCause instanceof NodeNotFoundException)
                    || (illegalStateCause instanceof ResourceNotFoundException)) {
                    createNode(dataNode);
                } else {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getCause());
                }
            } else if ((cause instanceof NodeNotFoundException) || (cause instanceof ResourceNotFoundException)) {
                createNode(dataNode);
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getCause());
            }
        }

        try {
            executeSecurely(() -> {
                upload(outputStreamWrapper, dataNode, contentType);
                return null;
            });
        } catch (Exception e) {
            final String message;

            if ((e.getCause() != null) && StringUtil.hasText(e.getCause().getMessage())) {
                message = e.getCause().getMessage();
            } else if (StringUtil.hasText(e.getMessage())) {
                message = e.getMessage();
            } else {
                message = "Error during upload.";
            }

            uploadError(Status.SERVER_ERROR_INTERNAL, message);
        }
    }

    /**
     * Abstract away the Transfer stuff.  It's cumbersome.
     *
     * @param outputStreamWrapper The OutputStream wrapper.
     * @param dataNode            The node to upload.
     * @param contentType           The file content type.
     * @throws Exception To capture transfer and upload failures.
     */
    void upload(final UploadOutputStreamWrapper outputStreamWrapper, final DataNode dataNode, final String contentType)
            throws Exception {
        final VOSURI dataNodeVOSURI = toURI(dataNode);

        final List<Protocol> protocols = Arrays.stream(FileItemServerResource.PROTOCOL_AUTH_METHODS).map(authMethod -> {
            final Protocol httpsAuth = new Protocol(VOS.PROTOCOL_HTTPS_PUT);
            httpsAuth.setSecurityMethod(Standards.getSecurityMethod(authMethod));
            return httpsAuth;
        }).collect(Collectors.toList());

        final Transfer transfer = new Transfer(dataNodeVOSURI.getURI(), Direction.pushToVoSpace);
        transfer.setView(new View(VOS.VIEW_DEFAULT));
        transfer.getProtocols().addAll(protocols);
        transfer.version = VOS.VOSPACE_21;

        final ClientTransfer ct = voSpaceClient.createTransfer(transfer);
        ct.setRequestProperty("content-type", contentType);
        ct.setOutputStreamWrapper(outputStreamWrapper);
        ct.runTransfer();

        // Check uws job status
        VOSClientUtil.checkTransferFailure(ct);

        if (ct.getHttpTransferDetails().getDigest() != null) {
            uploadVerifier.verifyMD5(outputStreamWrapper.getCalculatedMD5(),
                                     ct.getHttpTransferDetails().getDigest().getSchemeSpecificPart());
        }

        uploadSuccess();
    }

    /**
     * Parse the representation into a Map for easier access to Form elements.
     *
     * @return Map of field names to File Items, or empty Map.  Never null.
     */
    private ServletFileUpload parseRepresentation() {
        // 1/ Create a factory for disk-based file items
        final DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1000240);

        // Create a new file upload handler.
        return createFileUpload(factory);
    }

    /**
     * External method of obtaining a Restlet File Upload.
     *
     * @param factory Factory used to create the upload.
     * @return RestletFileUpload instance.
     */
    private ServletFileUpload createFileUpload(final DiskFileItemFactory factory) {
        return new ServletFileUpload(factory);
    }


    private void uploadError(final Status status, final String message) {
        writeResponse(status,
                      new JSONRepresentation() {
                          @Override
                          public void write(final JSONWriter jsonWriter)
                                  throws JSONException {
                              jsonWriter.object().key("error").value(message).endObject();
                          }
                      });
    }

    private void uploadSuccess() {
        writeResponse(Status.SUCCESS_CREATED,
                      new JSONRepresentation() {
                          @Override
                          public void write(final JSONWriter jsonWriter)
                                  throws JSONException {
                              jsonWriter.object().key("code").value(0).endObject();
                          }
                      });
    }
}
