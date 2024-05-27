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

import ca.nrc.cadc.util.StringUtil;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import net.canfar.storage.FileSizeRepresentation;
import net.canfar.storage.PathUtils;
import net.canfar.storage.StorageItemCSVWriter;
import net.canfar.storage.StorageItemWriter;
import net.canfar.storage.web.view.FolderItem;
import net.canfar.storage.web.view.StorageItem;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.opencadc.storage.StorageItemFactory;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeProperty;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.ClientTransfer;
import org.opencadc.vospace.client.VOSClientUtil;
import org.opencadc.vospace.transfer.Transfer;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;


public class FolderHandler extends StorageHandler {
    private static final Logger LOGGER = Logger.getLogger(FolderHandler.class);

    public FolderHandler(VOSpaceServiceConfig currentService, Subject subject) {
        super(currentService, subject);
    }

    /**
     * Obtain the quota for the provided path.  This method will scan downward from the root of the path to find a
     * path that contains the Quota property.
     *
     * @param nodePath The path to check.
     */
    public void writeQuota(final Path nodePath, final Writer writer) throws Exception {
        final ContainerNode containerNodeWithQuota = getContainerNodeWithQuota(nodePath, 0);
        final JSONWriter jsonWriter = new JSONWriter(writer);

        if (containerNodeWithQuota != null) {
            final long quotaSize = getQuotaPropertyValue(containerNodeWithQuota);
            LOGGER.debug(String.format("Reported quota size %d for path %s.", quotaSize, nodePath));
            final String quotaString = FileSizeRepresentation.getSizeHumanReadable(quotaSize);
            final long folderSize = containerNodeWithQuota.bytesUsed == null ? -1 : containerNodeWithQuota.bytesUsed;
            final String remainingSizeString = FileSizeRepresentation.getSizeHumanReadable(
                ((quotaSize - folderSize) > 0) ? (quotaSize - folderSize) : 0);

            jsonWriter.object()
                      .key("size").value(remainingSizeString)
                      .key("quota").value(quotaString)
                      .endObject();
        } else {
            jsonWriter.object()
                      .key("msg").value("quota not reported by VOSpace service")
                      .endObject();
        }

        writer.flush();
    }

    /**
     * Create a new folder.  This is the handler for a PUT action.
     *
     * @param newFolderPath The path of the folder to create.
     * @throws Exception If backend execution fails.
     */
    public void create(final Path newFolderPath) throws Exception {
        createNode(toContainerNode(newFolderPath));
    }

    /**
     * Move the source node URIs in the payload to the folder represented by the destination ContainerNode.
     *
     * @param payload         JSON Object containing a single URI string, or an array called <code>srcNodes</code> of URI strings.
     * @param destinationNode The Node to move the payload to .
     * @throws Exception If the transfer fails.
     */
    public void move(final JSONObject payload, final ContainerNode destinationNode) throws Exception {
        LOGGER.debug("moveToFolder input: " + payload);

        final Set<String> keySet = payload.keySet();

        if (keySet.contains("srcNodes")) {
            final Object srcNodeObject = payload.get("srcNodes");
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
                final VOSURI destinationURI = this.currentService.toURI(destinationNode);
                LOGGER.debug("moving " + sourceURI + " to " + destinationURI.toString());
                move(sourceURI, destinationURI);
            }
        }
    }

    /**
     * Iterate over the
     *
     * @param path
     * @param storageItemFactory
     * @throws Exception
     */
    public Iterator<String> iterator(final Path path, final StorageItemFactory storageItemFactory) throws Exception {
        final ContainerNode containerNode = getNode(path);
        final Path parentPath = PathUtils.toPath(containerNode);
        final VOSURI startNextPageURI;
        final Iterator<Node> childNodeIterator;
        if (currentService.supportsPaging()) {
            final List<Node> childNodes = containerNode.getNodes();
            if (childNodes.isEmpty()) {
                startNextPageURI = null;
                childNodeIterator = Collections.emptyIterator();
            } else {
                final Node nextNode = childNodes.get(childNodes.size() - 1);
                nextNode.parent = containerNode;
                PathUtils.augmentParents(PathUtils.toPath(nextNode), nextNode);
                startNextPageURI = childNodes.isEmpty() ? null : this.currentService.toURI(nextNode);
                childNodeIterator = childNodes.iterator();
            }
        } else {
            childNodeIterator = containerNode.childIterator == null
                                ? containerNode.getNodes().iterator()
                                : containerNode.childIterator;
            startNextPageURI = null;
        }

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return childNodeIterator.hasNext();
            }

            @Override
            public String next() {
                final Writer writer = new StringWriter();
                final StorageItemWriter storageItemWriter = new StorageItemCSVWriter(writer);
                final Node nextChild = childNodeIterator.next();
                PathUtils.augmentParents(Paths.get(parentPath.toString(), nextChild.getName()), nextChild);

                try {
                    // Check the translated storage item's URI first to handle an exception.
                    final StorageItem storageItem = storageItemFactory.translate(nextChild);

                    storageItemWriter.write(storageItem);
                } catch (URISyntaxException uriSyntaxException) {
                    LOGGER.error("Cannot create a URI from node {}.  Skipping over " + nextChild.getName(), uriSyntaxException);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return writer.toString();
            }
        };
    }

    private void move(final VOSURI source, final VOSURI destination) throws Exception {
        // According to ivoa.net VOSpace 2.1 spec, a move is handled using
        // a transfer. keepBytes = false. destination URI is the Direction.
        final Transfer transfer = getTransfer(source, destination);

        try {
            Subject.doAs(this.subject,
                         (PrivilegedExceptionAction<Void>) () -> {
                             final ClientTransfer clientTransfer = getVOSpaceClient().createTransfer(transfer);
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

    Transfer getTransfer(VOSURI source, VOSURI destination) {
        return new Transfer(source.getURI(), destination.getURI(), false);
    }

    private ContainerNode toContainerNode(final Path newFolderPath) {
        final ContainerNode containerNode = new ContainerNode(newFolderPath.getFileName().toString());
        PathUtils.augmentParents(newFolderPath, containerNode);

        return containerNode;
    }

    private ContainerNode getContainerNodeWithQuota(final Path path, final int pathElementIndex) throws Exception {
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
