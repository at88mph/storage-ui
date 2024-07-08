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

import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.util.StringUtil;
import org.opencadc.vospace.VOSURI;
import net.canfar.storage.CSVStorageItemProducer;
import net.canfar.storage.StorageItemCSVWriter;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;


public class PageServerResource extends StorageItemServerResource {
    @Get
    public Representation represent() throws Exception {
        final String startURIParameterValue = getQueryValue("startURI");
        final String pageSizeParameterValue = getQueryValue("pageSize");
        final VOSURI startURI = StringUtil.hasLength(startURIParameterValue)
                                ? new VOSURI(URI.create(startURIParameterValue))
                                : null;
        final Integer pageSize = StringUtil.hasLength(pageSizeParameterValue) ? Integer.parseInt(pageSizeParameterValue)
                                                                              : null;
        final Subject currentSubject = getVOSpaceCallingSubject();

        return new WriterRepresentation(MediaType.TEXT_CSV) {
            @Override
            public void write(final Writer writer) {
                final CSVStorageItemProducer csvNodeProducer =
                        new CSVStorageItemProducer(pageSize, getCurrentItemURI(), startURI,
                                                   new StorageItemCSVWriter(writer), currentSubject, storageItemFactory,
                                                   currentService, voSpaceClient);

                try {
                    csvNodeProducer.writePage();
                } catch (IllegalArgumentException e) {
                    // Very specific hack to try again without the (possibly) unsupported limit parameter.
                    if (pageSize != null
                        && e.getMessage().startsWith("OptionNotSupported")) {
                        final CSVStorageItemProducer pagelessProducer =
                                new CSVStorageItemProducer(null, getCurrentItemURI(), startURI,
                                                           new StorageItemCSVWriter(writer), currentSubject,
                                                           storageItemFactory, currentService, voSpaceClient);
                        try {
                            pagelessProducer.writePage();
                        } catch (Exception innerException) {
                            throw new ResourceException(innerException);
                        }
                    } else {
                        throw new ResourceException(e);
                    }
                } catch (RuntimeException runtimeException) {
                    // Working around a bug where the RegistryClient improperly handles an unauthenticated request.
                    if (runtimeException.getCause() != null
                        && (runtimeException.getCause() instanceof IOException)
                        && runtimeException.getCause().getCause() != null
                        && (runtimeException.getCause().getCause() instanceof NotAuthenticatedException)) {
                        throw new ResourceException(runtimeException.getCause().getCause());
                    } else {
                        throw runtimeException;
                    }
                } catch (Exception e) {
                    throw new ResourceException(e);
                }
            }
        };
    }
}
