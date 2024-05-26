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
import net.canfar.storage.web.config.StorageConfiguration;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import net.canfar.storage.web.config.VOSpaceServiceConfigManager;
import org.json.JSONArray;
import org.opencadc.vospace.transfer.Transfer;
import org.opencadc.vospace.*;
import org.opencadc.vospace.VOS.Detail;
import org.opencadc.vospace.client.ClientTransfer;
import org.opencadc.vospace.client.VOSClientUtil;
import org.opencadc.vospace.client.VOSpaceClient;
import net.canfar.storage.FileSizeRepresentation;
import net.canfar.storage.web.restlet.JSONRepresentation;

import java.net.URI;
import java.nio.file.Path;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import javax.security.auth.Subject;


public class FolderItemServerResource extends StorageItemServerResource {
    private static final Logger LOGGER = Logger.getLogger(FolderItemServerResource.class);

    /**
     * Empty constructor needed for Restlet to manage it.
     */
    public FolderItemServerResource() {
    }

    FolderItemServerResource(StorageConfiguration storageConfiguration,
                             VOSpaceServiceConfigManager voSpaceServiceConfigManager,
                             StorageItemFactory storageItemFactory, VOSpaceClient voSpaceClient,
                             VOSpaceServiceConfig serviceConfig) {
        super(storageConfiguration, voSpaceServiceConfigManager, storageItemFactory, voSpaceClient, serviceConfig);
    }

    @Put
    public void create() throws Exception {
        createFolder();
        getResponse().setStatus(Status.SUCCESS_CREATED);
    }

    @Get("json")
    public Representation retrieveQuota() {
        final Path currentPath = getCurrentPath();
        final ContainerNode containerNodeWithQuota = getContainerNodeWithQuota(currentPath, 0);

        if (containerNodeWithQuota != null) {
            final long quotaSize = getQuotaPropertyValue(containerNodeWithQuota);
            LOGGER.debug(String.format("Reported quota size %d for path %s.", quotaSize, currentPath));
            final String quotaString = FileSizeRepresentation.getSizeHumanReadable(quotaSize);
            final long folderSize = containerNodeWithQuota.bytesUsed == null ? -1 : containerNodeWithQuota.bytesUsed;
            final String remainingSizeString = FileSizeRepresentation.getSizeHumanReadable(
                    ((quotaSize - folderSize) > 0) ? (quotaSize - folderSize) : 0);

            return new JSONRepresentation() {
                @Override
                public void write(final JSONWriter jsonWriter) throws JSONException {
                    jsonWriter.object()
                              .key("size").value(remainingSizeString)
                              .key("quota").value(quotaString)
                              .endObject();
                }
            };
        } else {
            return new JSONRepresentation() {
                @Override
                public void write(final JSONWriter jsonWriter)
                        throws JSONException {
                    jsonWriter.object()
                              .key("msg").value("quota not reported by VOSpace service")
                              .endObject();
                }
            };
        }
    }

    private ContainerNode getContainerNodeWithQuota(final Path path, final int pathElementIndex) {
        if (path == null) {
            return null;
        }

        final int pathElementCount = path.getNameCount();

        if (pathElementCount > pathElementIndex) {
            // Add one to the end index as it's exclusive.
            final ContainerNode rootPathContainerNode = getNode(path.subpath(0, pathElementIndex + 1),
                                                                Detail.properties);
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

    Transfer getTransfer(VOSURI source, VOSURI destination) {
        return new Transfer(source.getURI(), destination.getURI(), false);
    }

    @Post("json")
    public void moveToFolder(final JsonRepresentation payload) throws Exception {
        final JSONObject jsonObject = payload.getJsonObject();
        LOGGER.debug("moveToFolder input: " + jsonObject);

        final ContainerNode currentNode = getCurrentNode(null);
        final Set<String> keySet = jsonObject.keySet();

        if (keySet.contains("srcNodes")) {
            final Object srcNodeObject = jsonObject.get("srcNodes");
            final String[] srcNodes;
            if (srcNodeObject instanceof JSONArray) {
                final List<String> srcNodePaths = new ArrayList<>();
                for (final Object o : (JSONArray) srcNodeObject) {
                    srcNodePaths.add(o.toString());
                }
                srcNodes = srcNodePaths.toArray(new String[0]);
            } else {
                srcNodes = new String[] {srcNodeObject.toString()};
            }

            // iterate over each srcNode & call clientTransfer
            for (final String srcNode : srcNodes) {
                final VOSURI sourceURI = new VOSURI(URI.create(this.currentService.getNodeResourceID() + srcNode));
                final VOSURI destinationURI = toURI(currentNode);
                LOGGER.debug("moving " + sourceURI + " to " + destinationURI.toString());
                move(sourceURI, destinationURI);
            }
            // move() will throw an exception if there is a problem
            getResponse().setStatus(Status.SUCCESS_OK);
        }
    }

    private void move(final VOSURI source, final VOSURI destination) throws Exception {
        // According to ivoa.net VOSpace 2.1 spec, a move is handled using
        // a transfer. keepBytes = false. destination URI is the Direction.
        final Transfer transfer = getTransfer(source, destination);

        try {
            Subject.doAs(getCurrentSubject(),
                         (PrivilegedExceptionAction<Void>) () -> {
                             final ClientTransfer clientTransfer = voSpaceClient.createTransfer(transfer);
                             clientTransfer.setMonitor(true);
                             clientTransfer.runTransfer();

                             LOGGER.debug("transfer run complete");
                             VOSClientUtil.checkTransferFailure(clientTransfer);
                             LOGGER.debug("no errors in transfer");
                             return null;
                         });
        } catch (PrivilegedActionException e) {
            LOGGER.debug("error in transfer.", e);
            throw e.getException();
        }
    }
}
