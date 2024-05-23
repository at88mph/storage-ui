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

import ca.nrc.cadc.rest.SyncOutput;
import net.canfar.storage.FileSizeRepresentation;
import net.canfar.storage.web.restlet.JSONRepresentation;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONWriter;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.client.VOSpaceClient;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;

public class FolderNodeHandler extends NodeHandler {
    private static final Logger LOGGER = Logger.getLogger(FolderNodeHandler.class);

    public FolderNodeHandler(VOSpaceClient voSpaceClient, Subject subject) {
        super(voSpaceClient, subject);
    }

    /**
     * Obtain the quota for the provided path.  This method will scan downward from the root of the path to find a
     * path that contains the Quota property.
     *
     * @param nodePath The path to check.
     * @return
     */
    public void retrieveQuota(final Path nodePath, final SyncOutput syncOutput) throws IOException {
        final ContainerNode containerNodeWithQuota = getContainerNodeWithQuota(nodePath, 0);
        final Writer streamWriter = new OutputStreamWriter(syncOutput.getOutputStream());
        final JSONWriter writer = new JSONWriter(streamWriter);

        if (containerNodeWithQuota != null) {
            final long quotaSize = getQuotaPropertyValue(containerNodeWithQuota);
            LOGGER.debug(String.format("Reported quota size %d for path %s.", quotaSize, nodePath));
            final String quotaString = FileSizeRepresentation.getSizeHumanReadable(quotaSize);
            final long folderSize = containerNodeWithQuota.bytesUsed == null ? -1 : containerNodeWithQuota.bytesUsed;
            final String remainingSizeString = FileSizeRepresentation.getSizeHumanReadable(
                    ((quotaSize - folderSize) > 0) ? (quotaSize - folderSize) : 0);

            writer.object()
                  .key("size").value(remainingSizeString)
                  .key("quota").value(quotaString)
                  .endObject();
        } else {
            writer.object()
                  .key("msg").value("quota not reported by VOSpace service")
                  .endObject();
        }

        streamWriter.flush();
    }

    private ContainerNode getContainerNodeWithQuota(final Path path, final int pathElementIndex) {
        if (path == null) {
            return null;
        }

        final int pathElementCount = path.getNameCount();

        if (pathElementCount > pathElementIndex) {
            // Add one to the end index as it's exclusive.
            final ContainerNode rootPathContainerNode = getNode(path.subpath(0, pathElementIndex + 1),
                                                                VOS.Detail.properties);
            if (rootPathContainerNode.getProperties().contains(new NodeProperty(VOS.PROPERTY_URI_QUOTA))) {
                return rootPathContainerNode;
            } else {
                return getContainerNodeWithQuota(path, pathElementIndex + 1);
            }
        } else {
            return null;
        }
    }

    private long getQuotaPropertyValue(final Node node) {
        final NodeProperty property = node.getProperty(VOS.PROPERTY_URI_QUOTA);
        return (property == null) ? 0L : Long.parseLong(property.getValue());
    }
}
