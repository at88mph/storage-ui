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


import ca.nrc.cadc.reg.client.RegistryClient;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import org.junit.Test;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;


public class BatchDownloadServerResourceTest
        extends AbstractServerResourceTest<BatchDownloadServerResource>
{
    @Test
    public void handleURLList() throws Exception
    {
        final String manifest =
                "OK\thttp://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fa%2Fuws_output.csv&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget\tTESTPLUS/a/uws_output.csv\n"
                + "OK\thttp://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fexample%2Binput.xml&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget\tTESTPLUS/example+input.xml";

        expect(mockServletContext.getContextPath()).andReturn("/teststorage")
                .once();

        replay(mockServletContext);

        testSubject = new BatchDownloadServerResource(mockVOSpaceClient)
        {
            @Override
            ServletContext getServletContext()
            {
                return mockServletContext;
            }

            @Override
            RegistryClient getRegistryClient()
            {
                return mockRegistryClient;
            }

            /**
             * Remove the Node associated with the given Path.
             * <p>
             * It is the responsibility of the caller to handle proper closing of
             * the writer.
             *
             * @param path   The path of the Node to delete.
             * @param writer Where to pump output to.
             */
            @Override
            void getManifest(String path, Writer writer) throws IOException
            {
                writer.write(manifest);
            }

            @Override
            Subject getCurrentUser() {
                return new Subject();
            }
        };


        final Representation rep = testSubject.handleDownload(
                BatchDownloadServerResource.DownloadMethod.URL_LIST.requestPropertyValue,
                new String[]{"vos://cadc.nrc.ca~vospace/path/1"});

        assertEquals("Wrong content type.", MediaType.TEXT_URI_LIST,
                     rep.getMediaType());
        assertEquals("Wrong text.", manifest, rep.getText());

        verify(mockServletContext);
    }

    @Test
    public void handleHTMLList() throws Exception
    {
        final String templateHTML = "<!DOCTYPE html>\n"
                                + "<html lang=\"en\">\n"
                                + "<head>\n"
                                + "  <meta charset=\"UTF-8\">\n"
                                + "  <title>Download stuff</title>\n"
                                + "</head>\n"
                                + "<body>\n"
                                + "%%LIST%%\n"
                                + "</body>\n"
                                + "</html>";

        final String manifest =
                "OK\thttp://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fa%2Fuws_output.csv&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget\tTESTPLUS/a/uws_output.csv\n"
                + "OK\thttp://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fexample%2Binput.xml&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget\tTESTPLUS/example+input.xml";

        final String expectedHTML =
                "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <title>Download stuff</title>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <p><a href=\"http://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fa%2Fuws_output.csv&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget\">http://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fa%2Fuws_output.csv&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget</a></p>\n"
                + "  <p><a href=\"http://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fexample%2Binput.xml&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget\">http://jenkinsd.cadc.dao.nrc.ca/vospace/synctrans?TARGET=vos%3A%2F%2Fcadc.nrc.ca%21vospace%2FCADCtest%2FTESTPLUS%2Fexample%2Binput.xml&DIRECTION=pullFromVoSpace&PROTOCOL=ivo%3A%2F%2Fivoa.net%2Fvospace%2Fcore%23httpget</a></p>\n"
                + "</body>\n"
                + "</html>\n";

        expect(mockServletContext.getContextPath()).andReturn("/storage-ui/go")
                .once();

        replay(mockServletContext);

        testSubject = new BatchDownloadServerResource(mockVOSpaceClient)
        {
            @Override
            ServletContext getServletContext()
            {
                return mockServletContext;
            }

            @Override
            RegistryClient getRegistryClient()
            {
                return mockRegistryClient;
            }

            /**
             * Remove the Node associated with the given Path.
             * <p>
             * It is the responsibility of the caller to handle proper closing of
             * the writer.
             *
             * @param path   The path of the Node to delete.
             * @param writer Where to pump output to.
             */
            @Override
            void getManifest(String path, Writer writer) throws IOException
            {
                writer.write(manifest);
            }

            @Override
            BufferedReader loadTemplateHTML()
            {
                return new BufferedReader(new StringReader(templateHTML));
            }

            @Override
            Subject getCurrentUser() {
                return new Subject();
            }
        };

        final Representation rep = testSubject.handleDownload(
                BatchDownloadServerResource.DownloadMethod.HTML_LIST.requestPropertyValue,
                new String[]{"vos://cadc.nrc.ca~vospace/path/1"});

        assertEquals("Wrong content type.", MediaType.TEXT_HTML,
                     rep.getMediaType());
        assertEquals("Wrong text.", expectedHTML.trim(), rep.getText().trim());

        verify(mockServletContext);
    }
}
