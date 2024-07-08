/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2016.                            (c) 2016.
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


import ca.nrc.cadc.reg.client.RegistryClient;
import net.canfar.storage.FileSizeRepresentation;
import ca.nrc.cadc.uws.ErrorSummary;
import ca.nrc.cadc.uws.ErrorType;
import ca.nrc.cadc.uws.ExecutionPhase;
import net.canfar.storage.web.StorageItemFactory;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import org.opencadc.vospace.*;

import org.opencadc.vospace.client.ClientTransfer;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.opencadc.vospace.transfer.Transfer;
import org.restlet.Context;
import org.restlet.Response;

import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;


public class FolderItemServerResourceTest extends AbstractServerResourceTest<FolderItemServerResource> {
    @Test
    public void create() throws Exception {
        final VOSURI folderURI = new VOSURI(URI.create(VOSPACE_NODE_URI_PREFIX + "/my/node"));
        final ContainerNode containerNode = new ContainerNode("node");

        final VOSpaceServiceConfig testServiceConfig =
                new VOSpaceServiceConfig("myvospace", URI.create("ivo://example.org/myvospace"),
                                         URI.create("vos://example.org~myvospace"), new VOSpaceServiceConfig.Features());

        final Map<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put("path", folderURI.getPath());

        Mockito.when(mockServletContext.getContextPath()).thenReturn("/teststorage");
        Mockito.when(mockVOSpaceClient.createNode(folderURI, containerNode, false))
               .thenReturn(containerNode);

        testSubject = new FolderItemServerResource(null, null,
                                                   new StorageItemFactory("/servletpath", testServiceConfig),
                                                   mockVOSpaceClient, testServiceConfig) {
            @Override
            VOSURI getCurrentItemURI() {
                return folderURI;
            }

            @Override
            Subject getVOSpaceCallingSubject() {
                return new Subject();
            }

            @Override
            public Map<String, Object> getRequestAttributes() {
                return requestAttributes;
            }

            /**
             * Returns the current context.
             *
             * @return The current context.
             */
            @Override
            public Context getContext() {
                return mockContext;
            }

            /**
             * Returns the handled response.
             *
             * @return The handled response.
             */
            @Override
            public Response getResponse() {
                return mockResponse;
            }

            @Override
            RegistryClient getRegistryClient() {
                return mockRegistryClient;
            }
        };

        Mockito.doNothing().when(mockResponse).setStatus(Status.SUCCESS_CREATED);

        testSubject.create();

        Mockito.verify(mockResponse, Mockito.times(1)).setStatus(Status.SUCCESS_CREATED);
    }

    @Test
    public void retrieveNormalQuota() throws Exception {
        long folderSize = 123456789L;
        long quota = 9876543210L;
        String expectedRemainingSize = FileSizeRepresentation.getSizeHumanReadable(quota - folderSize);
        this.retrieveQuota(quota, expectedRemainingSize, folderSize);
    }

    @Test
    public void retrieveNotEnoughQuota() throws Exception {
        long folderSize = 9876543210L;
        long quota = 123456789L;
        final String expectedRemainingSize = FileSizeRepresentation.getSizeHumanReadable(0);
        this.retrieveQuota(quota, expectedRemainingSize, folderSize);
    }

    private void retrieveQuota(long quota, final String expectedRemainingSize, final long folderSize) throws Exception {
        final String expectedQuota = FileSizeRepresentation.getSizeHumanReadable(quota);
        final VOSURI folderURI = new VOSURI(URI.create(VOSPACE_NODE_URI_PREFIX + "/my/node"));

        final VOSpaceServiceConfig testServiceConfig =
                new VOSpaceServiceConfig("vault", URI.create("ivo://example.org/vault"),
                                         URI.create("vos://example.org~vault"), new VOSpaceServiceConfig.Features());
        final NodeProperty prop = new NodeProperty(VOS.PROPERTY_URI_QUOTA, Long.toString(quota));

        final ContainerNode containerNode = new ContainerNode("node");
        containerNode.bytesUsed = folderSize;
        containerNode.getProperties().add(prop);

        final Map<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put("path", folderURI.getPath());

        testSubject = new FolderItemServerResource(null, null,
                                                   new StorageItemFactory("/servletpath", testServiceConfig),
                                                   mockVOSpaceClient, testServiceConfig) {
            @Override
            VOSURI getCurrentItemURI() {
                return folderURI;
            }

            @Override
            public Map<String, Object> getRequestAttributes() {
                return requestAttributes;
            }

            /**
             * Returns the handled response.
             *
             * @return The handled response.
             */
            @Override
            public Response getResponse() {
                return mockResponse;
            }

            @Override
            ServletContext getServletContext() {
                return mockServletContext;
            }

            @SuppressWarnings("unchecked")
            @Override
            <T extends Node> T getNode(final Path nodePath, final VOS.Detail detail) throws ResourceException {
                return (T) containerNode;
            }
        };

        Representation jsonRep = testSubject.retrieveQuota();

        final StringWriter swriter = new StringWriter();
        jsonRep.write(swriter);
        String[] kvps = swriter.getBuffer().toString().split(",");
        assertEquals("Should only contain two properties", 2, kvps.length);
        for (String kvp : kvps) {
            String[] kv = kvp.split(":");
            String key = extract(kv[0]);
            String value = extract(kv[1]);
            switch (key) {
                case "size":
                    Assert.assertEquals("Remainng size is incorrect", expectedRemainingSize, value);
                    break;
                case "quota":
                    Assert.assertEquals("Quota is incorrect", expectedQuota, value);
                    break;
                default:
                    fail("Incorrect property");
                    break;
            }
        }

    }

    private String extract(final String text) {
        int beginIndex = text.indexOf('"');
        int endIndex = text.lastIndexOf('"');
        return text.substring(beginIndex + 1, endIndex);
    }


    @Test
    public void moveItemsToFolder() throws Exception {
        final String srcNodeName = "/my/source_node";
        final String destNodeName = "/my/dest_node";

        final VOSURI destination = new VOSURI(URI.create(VOSPACE_NODE_URI_PREFIX + destNodeName));
        final ContainerNode mockDestinationNode = new ContainerNode("dest_node");

        final Transfer mockTransfer = Mockito.mock(Transfer.class);

        // Need mock ClientTransfer object as well.
        final ClientTransfer mockClientTransfer = Mockito.mock(ClientTransfer.class);

        // Override runTransfer & setMonitor methods
        mockClientTransfer.setMonitor(true);
        mockClientTransfer.runTransfer();

        final VOSpaceServiceConfig testServiceConfig =
                new VOSpaceServiceConfig("myvospace", URI.create("ivo://example.org/myvospace"),
                                         URI.create("vos://example.org~myvospace"), new VOSpaceServiceConfig.Features());

        final Map<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put("path", destination.getPath());

        // Return an empty error message with getServerError() in order to
        // signal that the move succeeded
        final ErrorSummary es = new ErrorSummary("", ErrorType.TRANSIENT);

        Mockito.when(mockClientTransfer.getServerError()).thenReturn(es);
        Mockito.when(mockClientTransfer.getPhase()).thenReturn(ExecutionPhase.COMPLETED);

        // Set up return code in response
        Mockito.doNothing().when(mockResponse).setStatus(Status.SUCCESS_OK);
        Mockito.when(mockVOSpaceClient.createTransfer(mockTransfer)).thenReturn(mockClientTransfer);
        Mockito.when(mockServletContext.getContextPath()).thenReturn("/teststorage");


        testSubject = new FolderItemServerResource(null, null,
                                                   new StorageItemFactory("/servletpath", testServiceConfig),
                                                   mockVOSpaceClient, testServiceConfig) {
            @Override
            RegistryClient getRegistryClient() {
                return mockRegistryClient;
            }

            @Override
            public Map<String, Object> getRequestAttributes() {
                return requestAttributes;
            }

            /**
             * Returns the current context.
             *
             * @return The current context.
             */
            @Override
            public Context getContext() {
                return mockContext;
            }

            @Override
            Subject getVOSpaceCallingSubject() {
                return new Subject();
            }

            @Override
            public Response getResponse() {
                return mockResponse;
            }

            @Override
            VOSURI getCurrentItemURI() {
                return destination;
            }

            @SuppressWarnings("unchecked")
            @Override
            <T extends Node> T getNode(Path nodePath, VOS.Detail detail) throws ResourceException {
                return (T) mockDestinationNode;
            }

            @Override
            Transfer getTransfer(VOSURI source, VOSURI destination) {
                return mockTransfer;
            }
        };


        final JSONObject sourceJSON = new JSONObject("{\"destNode\":\"" + destNodeName + "\","
                                                     + "\"srcNodes\":\"" + srcNodeName + "\"}");

        final JsonRepresentation payload = new JsonRepresentation(sourceJSON);

        testSubject.moveToFolder(payload);

        Mockito.verify(mockClientTransfer, Mockito.times(1)).getServerError();
        Mockito.verify(mockResponse, Mockito.times(1)).setStatus(Status.SUCCESS_OK);
        Mockito.verify(mockVOSpaceClient, Mockito.times(1)).createTransfer(mockTransfer);
    }
}
