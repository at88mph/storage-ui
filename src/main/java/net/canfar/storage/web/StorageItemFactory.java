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

package net.canfar.storage.web;

import net.canfar.storage.PathUtils;
import net.canfar.storage.web.config.VOSpaceServiceConfig;
import org.opencadc.gms.GroupURI;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.StringUtil;
import org.opencadc.vospace.*;
import net.canfar.storage.web.view.*;

import javax.security.auth.Subject;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.opencadc.vospace.server.Utils;


public class StorageItemFactory {
    private static final Logger LOGGER = Logger.getLogger(StorageItemFactory.class);

    private final String contextPath;
    private final VOSpaceServiceConfig serviceConfig;

    private static final Map<String, String> NODE_TYPE_ENDPOINT_MAPPING = new HashMap<>();

    static {
        StorageItemFactory.NODE_TYPE_ENDPOINT_MAPPING.put(LinkNode.class.getName(), "link");
        StorageItemFactory.NODE_TYPE_ENDPOINT_MAPPING.put(DataNode.class.getName(), "file");
        StorageItemFactory.NODE_TYPE_ENDPOINT_MAPPING.put(ContainerNode.class.getName(), "list");
    }


    public StorageItemFactory(final String contextPath, final VOSpaceServiceConfig serviceConfig) {
        this.contextPath = contextPath;
        this.serviceConfig = serviceConfig;
    }

    private Path getTarget(final Node node) {
        final String endpoint = StorageItemFactory.NODE_TYPE_ENDPOINT_MAPPING.get(node.getClass().getName());
        final String path = PathUtils.toPath(node).toString();
        return Paths.get(contextPath, serviceConfig.getName(), endpoint, path);
    }

    /**
     * Parse this node's last modified date.
     *
     * @param node The Node whose date to parse.
     * @return The Date parsed, or null if it cannot be parsed.
     */
    private Date parseDate(final Node node) {
        final String dateProperty = node.getPropertyValue(VOS.PROPERTY_URI_DATE);

        if (dateProperty == null) {
            return null;
        } else {
            try {
                return DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC).parse(dateProperty);
            } catch (ParseException e) {
                // Date cannot be parsed for some reason.
                return null;
            }
        }
    }


    /**
     * Translate the given node into a view object (StorageItem) instance.
     *
     * @param node The VOSpace Node instance.
     * @return StorageItem instance, never null.
     *
     * @throws URISyntaxException If the URI of the node cannot be translated.
     */
    public StorageItem translate(final Node node) throws URISyntaxException {
        final StorageItem nextItem;
        final boolean isRoot = Utils.isRoot(Utils.getPath(node));
        final Date lastModifiedDate = isRoot ? null : parseDate(node);

        final boolean publicFlag = node.isPublic != null && node.isPublic;
        final boolean lockedFlag = node.isLocked != null && node.isLocked;

        final GroupURI[] writeGroupURIs = node.getReadWriteGroup().toArray(new GroupURI[0]);
        final GroupURI[] readGroupURIs = node.getReadOnlyGroup().toArray(new GroupURI[0]);
        final String readableFlagValue = node.getPropertyValue(VOS.PROPERTY_URI_READABLE);

        // Default to readable if the flag is missing.  Any interaction will then be left up to the backend API.
        final boolean readableFlag = !StringUtil.hasLength(readableFlagValue)
                                     || (StringUtil.hasLength(readableFlagValue)
                                         && Boolean.parseBoolean(readableFlagValue));

        final Subject currentAuthenticatedUser = AuthenticationUtil.getCurrentSubject();
        final Set<Principal> principals = (currentAuthenticatedUser == null)
                                          ? new HashSet<>() : currentAuthenticatedUser.getPrincipals();
        final boolean writableFlag;
        final boolean parentIsRoot = node.parent == null || Utils.isRoot(Utils.getPath(node.parent));

        if (parentIsRoot && (!principals.isEmpty())) {
            writableFlag = true;
        } else {
            final String writableFlagValue = node.getPropertyValue(VOS.PROPERTY_URI_WRITABLE);
            writableFlag = StringUtil.hasLength(writableFlagValue) && Boolean.parseBoolean(writableFlagValue);
        }

        final String owner = node.ownerDisplay;

        final VOSURI nodeURI = serviceConfig.toURI(node);

        if (node instanceof ContainerNode) {
            final String totalChildCountValue =
                    node.getPropertyValue(URI.create("ivo://ivoa.net/vospace/core#childCount"));
            final int totalChildCount = StringUtil.hasLength(totalChildCountValue)
                                        ? Integer.parseInt(totalChildCountValue)
                                        : -1;
            final ContainerNode containerNode = (ContainerNode) node;

            final long sizeInBytes = containerNode.bytesUsed == null ? -1L : containerNode.bytesUsed;
            nextItem = new FolderItem(nodeURI, sizeInBytes, lastModifiedDate, publicFlag, lockedFlag, writeGroupURIs,
                                      readGroupURIs, owner, readableFlag, writableFlag, totalChildCount,
                                      getTarget(containerNode));
        } else if (node instanceof LinkNode) {
            nextItem = new LinkItem(nodeURI, lastModifiedDate, publicFlag, lockedFlag, writeGroupURIs,
                                    readGroupURIs, owner, readableFlag, writableFlag, getTarget(node));
        } else {
            final DataNode dataNode = (DataNode) node;
            final long sizeInBytes = dataNode.bytesUsed == null ? -1L : dataNode.bytesUsed;

            nextItem = new FileItem(nodeURI, sizeInBytes, lastModifiedDate, publicFlag, lockedFlag, writeGroupURIs,
                                    readGroupURIs, owner, readableFlag, writableFlag, getTarget(node));
        }

        return nextItem;
    }

    public FolderItem getFolderItemView(final ContainerNode containerNode) throws URISyntaxException {
        return (FolderItem) translate(containerNode);
    }
}
