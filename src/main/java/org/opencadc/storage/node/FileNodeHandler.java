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

package org.opencadc.storage.node;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.rest.SyncOutput;
import net.canfar.storage.PathUtils;
import org.apache.log4j.Logger;
import org.opencadc.vospace.DataNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.View;
import org.opencadc.vospace.client.ClientTransfer;
import org.opencadc.vospace.client.VOSpaceClient;
import org.opencadc.vospace.transfer.Direction;
import org.opencadc.vospace.transfer.Protocol;
import org.opencadc.vospace.transfer.Transfer;

import javax.security.auth.Subject;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class FileNodeHandler extends NodeHandler {
    private static final Logger LOGGER = Logger.getLogger(FileNodeHandler.class);

    public FileNodeHandler(VOSpaceClient voSpaceClient, Subject subject) {
        super(voSpaceClient, subject);
    }

    public void download(final Path nodePath, final SyncOutput syncOutput) throws Exception {
        final DataNode dataNode = getNode(nodePath, null, null);
        download(dataNode);
    }

    void download(final DataNode dataNode) throws Exception {
        final VOSURI dataNodeVOSURI = toURI(dataNode);
        final AuthMethod authMethod = AuthenticationUtil.getAuthMethodFromCredentials(this.subject);
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
}
