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

package ca.nrc.cadc.beacon.web.resources;

import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.beacon.AbstractUnitTest;

import ca.nrc.cadc.beacon.web.restlet.VOSpaceApplication;
import ca.nrc.cadc.beacon.web.view.FolderItem;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.web.AccessControlClient;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.restlet.Context;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;


public class MainPageServerResourceTest
        extends AbstractServerResourceTest<MainPageServerResource>
{
    @Test
    @SuppressWarnings("unchecked")
    public void representFolderItem() throws Exception
    {
        final FolderItem mockFolderItem = createMock(FolderItem.class);
        final List<String> initialRowData = new ArrayList<>();
        final Subject subject = new Subject();
        final VOSURI startURI =
                new VOSURI(URI.create("vos://myhost.com/node/1"));

        initialRowData.add("child1");
        initialRowData.add("child2");
        initialRowData.add("child3");

        AccessControlClient mockAccessControlClient = createMock(AccessControlClient.class);

        expect(mockAccessControlClient.getCurrentHttpPrincipalUsername(subject)).andReturn("CADCtest");
        replay(mockAccessControlClient);

        // Taken from VOSpaceApplication - they are private there, so
        // just pilfering the values here for the test
        final String DEFAULT_GMS_SERVICE_ID =
                "ivo://cadc.nrc.ca/gms";
        final String GMS_SERVICE_PROPERTY_KEY =
                "org.opencadc.gms.service_id";

        Configuration configuration = new SystemConfiguration();
        ConcurrentHashMap<String,Object> mockContextAttributes = new ConcurrentHashMap<String,Object>();
        AccessControlClient acc = new AccessControlClient(URI.create(
                configuration.getString(DEFAULT_GMS_SERVICE_ID,
                        GMS_SERVICE_PROPERTY_KEY)));
        mockContextAttributes.put(VOSpaceApplication.ACCESS_CONTROL_CLIENT_KEY, acc);

        expect(mockContext.getAttributes()).andReturn(mockContextAttributes).anyTimes();

        replay(mockServletContext, mockRegistryClient, mockContext);


        testSubject = new MainPageServerResource()
        {
            @Override
            Subject getCurrentUser()
            {
                return subject;
            }

            @Override
            ServletContext getServletContext()
            {
                return mockServletContext;
            }

            @Override
            public Context getContext() {
                return mockContext;
            }


        };
        testSubject.accessControlClient = mockAccessControlClient;

        replay(mockFolderItem);

        final Representation representation =
                testSubject.representFolderItem(mockFolderItem,
                                                initialRowData.iterator(),
                                                subject, startURI);

        final TemplateRepresentation templateRepresentation =
                (TemplateRepresentation) representation;

        final Map<String, Object> dataModel =
                (Map<String, Object>) templateRepresentation.getDataModel();

        assertTrue("Should be a folder.", dataModel.containsKey("folder"));
        assertEquals("Should have URI for next page.",
                     startURI.getURI().toString(), dataModel.get("startURI"));
        assertTrue("Should contain initialRows",
                   dataModel.containsKey("initialRows"));

        verify(mockFolderItem, mockServletContext);
    }
}
