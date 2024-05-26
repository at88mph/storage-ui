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

package org.opencadc.storage;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.rest.InlineContentException;
import ca.nrc.cadc.rest.InlineContentHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import net.canfar.storage.web.UploadOutputStreamWrapper;
import net.canfar.storage.web.UploadOutputStreamWrapperImpl;
import net.canfar.storage.web.UploadVerifier;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.DataNode;
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

public class FileUploadInlineContentHandler implements InlineContentHandler {
    private final static AuthMethod[] PROTOCOL_AUTH_METHODS = new AuthMethod[] {AuthMethod.ANON, AuthMethod.CERT, AuthMethod.COOKIE};
    private static final int BUFFER_SIZE = 8192;
    private final UploadVerifier uploadVerifier;
    private final Subject subject;
    private final VOSpaceServiceConfig currentService;


    public FileUploadInlineContentHandler(VOSpaceServiceConfig currentService, UploadVerifier uploadVerifier, Subject subject) {
        this.currentService = currentService;
        this.uploadVerifier = uploadVerifier;
        this.subject = subject;
    }

    @Override
    public Content accept(String fileName, String fileContentType, InputStream fileInputStream)
        throws InlineContentException, IOException, ResourceNotFoundException, TransientException {
        final DataNode dataNode = new DataNode(fileName);
        try {
            final Path nodePath = upload(fileInputStream, dataNode, fileContentType);
            final Content content = new Content();
            content.name = fileName;
            content.value = nodePath;

            return content;
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage(), exception);
        }
    }

    /**
     * Do the secure upload.
     *
     * @param inputStream The InputStream to pull from.
     * @param dataNode    The DataNode to upload to.
     * @param contentType The file content type.
     */
    protected Path upload(final InputStream inputStream, final DataNode dataNode, final String contentType) throws Exception {
        final UploadOutputStreamWrapper outputStreamWrapper = new UploadOutputStreamWrapperImpl(inputStream, FileUploadInlineContentHandler.BUFFER_SIZE);
        final Path dataNodePath = Paths.get(Utils.getPath(dataNode));

        return Subject.doAs(this.subject, (PrivilegedExceptionAction<Path>) () -> {
            final VOSpaceClient voSpaceClient = this.currentService.getVOSpaceClient();
            try {
                // Due to a bug in VOSpace that returns a 400 while checking
                // for an existing Node, we will work around it by checking manually
                // rather than looking for a NodeNotFoundException as expected, and
                // return the 409 code, while maintaining backward compatibility with the catch below.
                // jenkinsd 2016.07.25
                voSpaceClient.getNode(dataNodePath.toString(), null);
            } catch (IllegalStateException e) {
                final Throwable illegalStateCause = e.getCause();
                if (illegalStateCause instanceof ResourceNotFoundException) {
                    voSpaceClient.createNode(this.currentService.toURI(dataNode), dataNode);
                } else {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            } catch (ResourceNotFoundException notFoundException) {
                voSpaceClient.createNode(this.currentService.toURI(dataNode), dataNode);
            }

            upload(outputStreamWrapper, dataNode, contentType);

            return dataNodePath;
        });
    }

    /**
     * Abstract away the Transfer stuff.  It's cumbersome.
     *
     * @param outputStreamWrapper The OutputStream wrapper.
     * @param dataNode            The node to upload.
     * @param contentType         The file content type.
     * @throws Exception To capture transfer and upload failures.
     */
    void upload(final UploadOutputStreamWrapper outputStreamWrapper, final DataNode dataNode, final String contentType) throws Exception {
        final VOSURI dataNodeVOSURI = this.currentService.toURI(dataNode);

        final List<Protocol> protocols = Arrays.stream(FileUploadInlineContentHandler.PROTOCOL_AUTH_METHODS).map(authMethod -> {
            final Protocol httpsAuth = new Protocol(VOS.PROTOCOL_HTTPS_PUT);
            httpsAuth.setSecurityMethod(Standards.getSecurityMethod(authMethod));
            return httpsAuth;
        }).collect(Collectors.toList());

        final Transfer transfer = new Transfer(dataNodeVOSURI.getURI(), Direction.pushToVoSpace);
        transfer.setView(new View(VOS.VIEW_DEFAULT));
        transfer.getProtocols().addAll(protocols);
        transfer.version = VOS.VOSPACE_21;

        final ClientTransfer ct = this.currentService.getVOSpaceClient().createTransfer(transfer);
        ct.setRequestProperty("content-type", contentType);
        ct.setOutputStreamWrapper(outputStreamWrapper);
        ct.runTransfer();

        // Check uws job status
        VOSClientUtil.checkTransferFailure(ct);

        if (ct.getHttpTransferDetails().getDigest() != null) {
            uploadVerifier.verifyMD5(outputStreamWrapper.getCalculatedMD5(), ct.getHttpTransferDetails().getDigest().getSchemeSpecificPart());
        }
    }
}
