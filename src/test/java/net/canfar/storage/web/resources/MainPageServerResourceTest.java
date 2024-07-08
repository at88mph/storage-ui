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


import net.canfar.storage.web.StorageItemFactory;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import net.canfar.storage.web.config.VOSpaceServiceConfigManager;
import org.opencadc.vospace.*;
import net.canfar.storage.web.config.StorageConfiguration;
import net.canfar.storage.web.view.FolderItem;
import net.canfar.storage.web.view.FreeMarkerConfiguration;
import org.mockito.Mockito;
import org.opencadc.token.Client;
import org.restlet.Context;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;


public class MainPageServerResourceTest extends AbstractServerResourceTest<MainPageServerResource> {
    @Test
    @SuppressWarnings("unchecked")
    public void representFolderItem() throws Exception {
        final FolderItem mockFolderItem = Mockito.mock(FolderItem.class);
        final List<String> initialRowData = new ArrayList<>();
        final ContainerNode containerNode = new ContainerNode("node");

        final VOSpaceServiceConfig testServiceConfig =
                new VOSpaceServiceConfig("vos", URI.create("ivo://example.org/vos"),
                                         URI.create("vos://example.org~vos"), new VOSpaceServiceConfig.Features());

        final StorageItemFactory testStorageItemFactory = new StorageItemFactory("/mystore",
                                                                                 testServiceConfig);

        initialRowData.add("child1");
        initialRowData.add("child2");
        initialRowData.add("child3");

        final List<String> vospaceServiceList = new ArrayList<>();
        vospaceServiceList.add("vos");
        vospaceServiceList.add("arc");

        final FreeMarkerConfiguration mockFreeMarkerConfiguration = Mockito.mock(FreeMarkerConfiguration.class);
        final StorageConfiguration mockStorageConfiguration = Mockito.mock(StorageConfiguration.class);
        final Client mockOIDCConfiguration = Mockito.mock(Client.class);

        final VOSpaceServiceConfigManager mockVOSpaceServiceConfigManager =
                Mockito.mock(VOSpaceServiceConfigManager.class);
        Mockito.when(mockVOSpaceServiceConfigManager.getServiceList()).thenReturn(vospaceServiceList);

        Mockito.when(mockStorageConfiguration.getThemeName()).thenReturn("vos");
        Mockito.when(mockFreeMarkerConfiguration.getTemplate("themes/vos/index.ftl")).thenReturn(null);

        testSubject = new MainPageServerResource(mockStorageConfiguration, mockVOSpaceServiceConfigManager,
                                                 testStorageItemFactory, mockVOSpaceClient, testServiceConfig) {
            @Override
            FreeMarkerConfiguration getFreeMarkerConfiguration() {
                return mockFreeMarkerConfiguration;
            }

            @Override
            Subject getVOSpaceCallingSubject() {
                return new Subject();
            }

            @Override
            protected Client getOIDCClient() {
                return mockOIDCConfiguration;
            }

            @Override
            protected String getDisplayName() {
                return "testuser";
            }

            @Override
            ServletContext getServletContext() {
                return mockServletContext;
            }

            @Override
            public Context getContext() {
                return mockContext;
            }

            @Override
            public String getCurrentVOSpaceService() {
                return "vos";
            }

            @SuppressWarnings("unchecked")
            @Override
            <T extends Node> T getNode(final Path nodePath, final VOS.Detail detail) throws ResourceException {
                return (T) containerNode;
            }
        };

        Mockito.when(mockFolderItem.isWritable()).thenReturn(true);
        final Representation representation = testSubject.representFolderItem(mockFolderItem,
                                                                              initialRowData.iterator(), null);
        final TemplateRepresentation templateRepresentation = (TemplateRepresentation) representation;
        final Map<String, Object> dataModel = (Map<String, Object>) templateRepresentation.getDataModel();

        assertTrue("Should be a folder.", dataModel.containsKey("folder"));
        assertTrue("Should contain initialRows", dataModel.containsKey("initialRows"));
        assertEquals("Wrong username", "testuser", dataModel.get("username"));

        Mockito.verify(mockVOSpaceServiceConfigManager, Mockito.times(1)).getServiceList();
        Mockito.verify(mockStorageConfiguration, Mockito.times(1)).getThemeName();
        Mockito.verify(mockFreeMarkerConfiguration, Mockito.times(1))
               .getTemplate("themes/vos/index.ftl");
    }
}
