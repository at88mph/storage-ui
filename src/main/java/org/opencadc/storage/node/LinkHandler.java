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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.security.auth.Subject;
import net.canfar.storage.PathUtils;
import net.canfar.storage.web.view.StorageItem;
import org.json.JSONObject;
import org.opencadc.storage.StorageItemFactory;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.LinkNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.NodeNotFoundException;
import org.opencadc.vospace.VOSURI;


public class LinkHandler extends StorageHandler {
    private final static String JSON_TARGET_KEY = "link_url";

    public LinkHandler(VOSpaceServiceConfig currentService, Subject subject) {
        super(currentService, subject);
    }

    /**
     * Resolve this link Node's target to its final destination.  This method
     * will follow the target of the provided LinkNode, and continue to do so
     * until an external URL is found, or Node that is not a Link Node.
     * <p>
     * Finally, this method will redirect to the appropriate endpoint.
     *
     * @param nodePath           The Path of the LinkNode to resolve.
     * @param storageItemFactory The StorageItemFactory to resolve the link.
     * @throws Exception If the target is not found, or backend access is compromised
     */
    public URI resolve(final Path nodePath, final StorageItemFactory storageItemFactory) throws Exception {
        final LinkNode linkNode = getNode(nodePath);
        return resolve(linkNode, storageItemFactory);
    }

    /**
     * Create a new LinkNode.
     *
     * @param path       The path of the Link.
     * @param jsonObject The JSON containing the target URI of the link.
     * @throws Exception If the node cannot be created.
     */
    public void create(final Path path, final JSONObject jsonObject) throws Exception {
        createNode(toLinkNode(path, URI.create(jsonObject.getString(LinkHandler.JSON_TARGET_KEY))));
    }

    /**
     * Resolve the given LinkNode's target URI and return it.
     *
     * @param linkNode The LinkNode to resolve.
     * @return URI of the target.
     * @throws NodeNotFoundException If the target is not found.
     */
    private URI resolve(final LinkNode linkNode, final StorageItemFactory storageItemFactory) throws Exception {
        final URI endPoint;
        final URI targetURI = linkNode.getTarget();

        // Should ALWAYS be true for a LinkNode!
        if (targetURI == null) {
            throw new IllegalArgumentException("**BUG**: LinkNode has a null target!");
        } else {
            try {
                final VOSURI vosURI = new VOSURI(targetURI);
                final Node targetNode = getNode(Paths.get(vosURI.getPath()), null);

                if (targetNode == null) {
                    throw new NodeNotFoundException("No target found or broken link for node: " + linkNode.getName());
                } else {
                    if (targetNode instanceof LinkNode) {
                        endPoint = resolve((LinkNode) targetNode, storageItemFactory);
                    } else {
                        final StorageItem storageItem = storageItemFactory.translate(targetNode);
                        endPoint = URI.create(storageItem.getTargetPath());
                    }
                }
            } catch (IllegalArgumentException | URISyntaxException e) {
                // Not a VOSpace URI, so return this URI.
                return targetURI;
            }
        }

        if (endPoint == null) {
            throw new IllegalArgumentException("Link " + linkNode.getTarget() + " cannot be resolved.");
        }

        return endPoint;
    }

    private LinkNode toLinkNode(final Path path, final URI target) {
        final LinkNode linkNode = new LinkNode(path.getFileName().toString(), target);
        PathUtils.augmentParents(path, linkNode);

        return linkNode;
    }
}
