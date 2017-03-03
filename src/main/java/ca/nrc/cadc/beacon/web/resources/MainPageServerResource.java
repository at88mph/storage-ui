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

import ca.nrc.cadc.beacon.StorageItemCSVWriter;
import ca.nrc.cadc.beacon.StorageItemWriter;
import ca.nrc.cadc.beacon.web.restlet.VOSpaceApplication;
import ca.nrc.cadc.beacon.web.view.FolderItem;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.vos.*;
import ca.nrc.cadc.accesscontrol.AccessControlClient;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;


public class MainPageServerResource extends StorageItemServerResource
{
    private final Configuration freemarkerConfiguration =
            new Configuration(Configuration.VERSION_2_3_25);


    /**
     * Set-up method that can be overridden in order to initialize the state of
     * the resource. By default it does nothing.
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();

        freemarkerConfiguration.setLocalizedLookup(false);

        final ServletContext servletContext = getServletContext();

        try
        {
            if (servletContext == null)
            {
                freemarkerConfiguration.setSharedVariable(
                        "contextPath", VOSpaceApplication.DEFAULT_CONTEXT_PATH);
                freemarkerConfiguration.setTemplateLoader(
                        new FileTemplateLoader(new File("src/main/webapp")));
            }
            else
            {
                final String servletPath = servletContext.getContextPath();

                freemarkerConfiguration.setSharedVariable(
                        "contextPath",
                        servletPath + (servletPath.endsWith("/") ? "" : "/"));
                freemarkerConfiguration.setTemplateLoader(
                        new WebappTemplateLoader(servletContext));
            }
        }
        catch (IOException | TemplateModelException e)
        {
            throw new ResourceException(e);
        }
    }

    @Get
    public Representation represent() throws Exception
    {
        final ContainerNode containerNode = getCurrentNode();
        return representContainerNode(containerNode);
    }

    private Representation representContainerNode(
            final ContainerNode containerNode) throws Exception
    {
        final List<Node> childNodes = containerNode.getNodes();
        final Iterator<String> initialRows = new Iterator<String>()
        {
            final Iterator<Node> childNodeIterator = childNodes.iterator();

            @Override
            public boolean hasNext()
            {
                return childNodeIterator.hasNext();
            }

            @Override
            public String next()
            {
                final Writer writer = new StringWriter();
                final StorageItemWriter storageItemWriter =
                        new StorageItemCSVWriter(writer);

                try
                {
                    storageItemWriter.write(storageItemFactory.translate(
                            childNodeIterator.next()));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }

                return writer.toString();
            }

            /**
             * Removes from the underlying collection the last element returned
             * by this iterator (optional operation).  This method can be called
             * only once per call to {@link #next}.  The behavior of an iterator
             * is unspecified if the underlying collection is modified while the
             * iteration is in progress in any way other than by calling this
             * method.
             *
             * @throws UnsupportedOperationException if the {@code remove}
             *                                       operation is not supported by this iterator
             * @throws IllegalStateException         if the {@code next} method has not
             *                                       yet been called, or the {@code remove} method has already
             *                                       been called after the last call to the {@code next}
             *                                       method
             * {@link UnsupportedOperationException} and performs no other action.
             */
            @Override
            public void remove()
            {
                childNodeIterator.remove();
            }
        };

        final VOSURI startNextPageURI =
                childNodes.isEmpty() ? null :
                childNodes.get(childNodes.size() - 1).getUri();

        final FolderItem folderItem =
                storageItemFactory.getFolderItemView(containerNode);

        return representFolderItem(folderItem, initialRows, startNextPageURI);
    }

    Representation representFolderItem(final FolderItem folderItem,
                                       final Iterator<String> initialRows,
                                       final VOSURI startNextPageURI)
            throws Exception
    {
        final Map<String, Object> dataModel = new HashMap<>();
        final AccessControlClient accessControlClient =
                (AccessControlClient) getContext().getAttributes().get(
                        VOSpaceApplication.ACCESS_CONTROL_CLIENT_KEY);

        dataModel.put("initialRows", initialRows);
        dataModel.put("folder", folderItem);
        if (startNextPageURI != null)
        {
            dataModel.put("startURI", startNextPageURI.toString());
        }

        // HttpPrincipal username will be pulled from current user
        Subject s = getCurrentUser();
        String httpUsername = accessControlClient
                .getCurrentHttpPrincipalUsername(s);

        if (StringUtil.hasText(httpUsername))
        {
            dataModel.put("username", httpUsername);
        }

        return new TemplateRepresentation("index.ftl", freemarkerConfiguration, dataModel,
                                          MediaType.TEXT_HTML);
    }
}
