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
import net.canfar.storage.web.StorageItemFactory;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.LinkNode;
import org.opencadc.vospace.VOSURI;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;

import javax.security.auth.Subject;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class LinkItemServerResourceTest extends AbstractServerResourceTest<LinkItemServerResource> {
    @Test
    public void createLink() throws Exception {
        final URI target = URI.create("http://gohere.com/to/see");
        final VOSURI expectedURI = new VOSURI(URI.create(VOSPACE_NODE_URI_PREFIX + "/curr/dir/MY_LINK"));
        final LinkNode linkNode = new LinkNode("MY_LINK", target);

        final VOSpaceServiceConfig testServiceConfig =
                new VOSpaceServiceConfig("vault", URI.create("ivo://ca.nrc.cadc/vault"),
                                         URI.create(VOSPACE_NODE_URI_PREFIX), new VOSpaceServiceConfig.Features());

        Mockito.when(mockServletContext.getContextPath()).thenReturn("/to");
        Mockito.when(mockVOSpaceClient.createNode(expectedURI, linkNode, false)).thenReturn(linkNode);

        final JSONObject sourceJSON = new JSONObject("{\"link_name\":\"MY_LINK\","
                                                     + "\"link_url\":\"http://gohere.com/to/see\"}");

        final JsonRepresentation payload = new JsonRepresentation(sourceJSON);

        final Map<String, Object> attributes = new HashMap<>();

        attributes.put("path", "/curr/dir/MY_LINK");

        Mockito.doNothing().when(mockResponse).setStatus(Status.SUCCESS_CREATED);
        Mockito.when(mockContext.getAttributes()).thenReturn(new ConcurrentHashMap<>());

        testSubject = new LinkItemServerResource(null, null,
                                                 new StorageItemFactory("/servletpath", testServiceConfig),
                                                 mockVOSpaceClient, testServiceConfig) {
            @Override
            RegistryClient getRegistryClient() {
                return mockRegistryClient;
            }

            @Override
            Subject getVOSpaceCallingSubject() {
                return new Subject();
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
             * Returns the request attributes.
             *
             * @return The request attributes.
             * @see Request#getAttributes()
             */
            @Override
            public Map<String, Object> getRequestAttributes() {
                return attributes;
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
        };

        testSubject.create(payload);

        Mockito.verify(mockResponse, Mockito.times(1)).setStatus(Status.SUCCESS_CREATED);
        Mockito.verify(mockVOSpaceClient, Mockito.times(1)).createNode(expectedURI, linkNode, false);
    }

    @Test
    public void resolve() throws Exception {
        final String getNodeRequestQuery = "limit=0";
        final URI target = URI.create("vos://cadc.nrc.ca!linktest/other/dir/my/dir");
        final LinkNode linkNode = new LinkNode("MY_LINK", target);
        final ContainerNode targetContainerNode = new ContainerNode("dir");
        final VOSpaceServiceConfig testServiceConfig =
                new VOSpaceServiceConfig("linktest", URI.create("ivo://example.org/linktest"),
                                         URI.create("vos://example.org~linktest"), new VOSpaceServiceConfig.Features());

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("path", "/curr/dir/MY_LINK");

        final ConcurrentMap<String, Object> mockAttributeMap = new ConcurrentHashMap<>();
        Mockito.when(mockContext.getAttributes()).thenReturn(mockAttributeMap);

        Mockito.when(mockVOSpaceClient.getNode("/other/dir/my/dir", getNodeRequestQuery))
               .thenReturn(targetContainerNode);
        Mockito.when(mockVOSpaceClient.getNode("/curr/dir/MY_LINK", getNodeRequestQuery)).thenReturn(linkNode);

        testSubject = new LinkItemServerResource(null, null,
                                                 new StorageItemFactory("/servletpath", testServiceConfig),
                                                 mockVOSpaceClient, testServiceConfig) {
            @Override
            RegistryClient getRegistryClient() {
                return mockRegistryClient;
            }

            @Override
            Subject getVOSpaceCallingSubject() {
                return new Subject();
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
             * Returns the request attributes.
             *
             * @return The request attributes.
             * @see Request#getAttributes()
             */
            @Override
            public Map<String, Object> getRequestAttributes() {
                return attributes;
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
        };

        testSubject.resolve();

        Mockito.verify(mockVOSpaceClient, Mockito.times(1)).getNode("/curr/dir/MY_LINK",
                                                                    getNodeRequestQuery);
        Mockito.verify(mockVOSpaceClient, Mockito.times(1)).getNode("/other/dir/my/dir",
                                                                    getNodeRequestQuery);
    }
}
