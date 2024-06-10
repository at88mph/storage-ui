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

package org.opencadc.storage;

import ca.nrc.cadc.net.NetUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.VOSpaceClient;


abstract class AbstractStorageItemProducer<T extends StorageItemWriter> implements StorageItemProducer {
    private VOSURI current;
    private final Integer pageSize;
    private final VOSURI folderURI;
    private final Subject user;
    private final T storageItemWriter;
    private final StorageItemFactory storageItemFactory;
    private final VOSpaceServiceConfig serviceConfig;
    private final VOSpaceClient voSpaceClient;


    AbstractStorageItemProducer(Integer pageSize, VOSURI folderURI,
                                final VOSURI startURI,
                                final T storageItemWriter,
                                final Subject user,
                                final StorageItemFactory storageItemFactory,
                                final VOSpaceServiceConfig serviceConfig,
                                final VOSpaceClient voSpaceClient) {
        this.pageSize = pageSize;
        this.folderURI = folderURI;
        this.current = startURI;
        this.storageItemWriter = storageItemWriter;
        this.user = user;
        this.storageItemFactory = storageItemFactory;
        this.serviceConfig = serviceConfig;
        this.voSpaceClient = voSpaceClient;
    }


    String getQuery() {
        final Map<String, String> queryPayload = new HashMap<>();
        final VOS.Detail detail = folderURI.isRoot()
            ? VOS.Detail.raw : VOS.Detail.max;
        queryPayload.put("detail", detail.name());

        if (this.pageSize != null) {
            queryPayload.put("limit", this.pageSize.toString());
        }

        if (current != null) {
            queryPayload.put("uri", NetUtil.encode(current.toString()));
        }

        return queryPayload.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                           .collect(Collectors.joining("&"));
    }

    private Iterator<Node> nextPage() throws Exception {
        return Subject.doAs(user, (PrivilegedExceptionAction<Iterator<Node>>) () -> {
            final ContainerNode pageNode = ((ContainerNode) voSpaceClient.getNode(folderURI.getPath(), getQuery()));
            final List<Node> childNodes = pageNode.getNodes();

            // If the child count is one or zero, then this page contains the requested startURI only, or nothing.  If
            // it only contains the startURI, then the next page will as well, and this will become an endless request
            // of pages.
            // jenkinsd 2024.01.25
            //
            if (childNodes.size() < 2 && this.current != null) {
                return Collections.emptyIterator();
            } else {
                return pageNode.childIterator == null
                    ? pageNode.getNodes().iterator()
                    : pageNode.childIterator;
            }
        });
    }

    private boolean writePage(final Iterator<Node> page) throws IOException, URISyntaxException {
        if (!page.hasNext()) {
            return false;
        } else {
            while (page.hasNext()) {
                final Node n = page.next();
                PathUtils.augmentParents(Paths.get(folderURI.getPath(), n.getName()), n);
                this.storageItemWriter.write(storageItemFactory.translate(n));
                this.current = this.serviceConfig.toURI(n);
            }

            return true;
        }
    }

    @Override
    public boolean writePage() throws Exception {
        return writePage(nextPage());
    }

    void writePages() throws Exception {
        boolean hasMore = writePage();
        while (hasMore) {
            hasMore = writePage();
        }
    }
}
