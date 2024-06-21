/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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

package org.opencadc.storage.config;

import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.LocalAuthority;
import ca.nrc.cadc.util.StringUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.opencadc.storage.PathUtils;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.VOSpaceClient;

public class VOSpaceServiceConfig {
    private final String name;
    private final URI resourceID;
    private final URI nodeResourceID;

    private final Features features;

    // Default provided, can be overridden
    public String homeDir;

    public VOSpaceServiceConfig(String name, URI resourceID, URI nodeResourceID, final Features features) {

        // Validation for required properties
        if (!StringUtil.hasLength(name)) {
            throw new IllegalArgumentException("VOSpace service name required");
        }
        if (resourceID == null) {
            throw new IllegalArgumentException("VOSpace service resource ID required");
        }
        if (nodeResourceID == null) {
            throw new IllegalArgumentException("VOSpace node resource ID required");
        }

        this.name = name;
        this.resourceID = resourceID;
        this.nodeResourceID = nodeResourceID;
        this.features = features;

        // Set default for optional properties
        this.homeDir = "/";
    }

    public static URI getGroupURI(final String groupName) {
        return URI.create(VOSpaceServiceConfig.getGMSServiceURI() + "?" + groupName);
    }

    public static URI getGMSServiceURI() {
        final LocalAuthority localAuthority = new LocalAuthority();
        return localAuthority.getServiceURI(Standards.GMS_SEARCH_10.toASCIIString());
    }

    public String getName() {
        return this.name;
    }

    public URI getResourceID() {
        return this.resourceID;
    }

    public URI getNodeResourceID() {
        return this.nodeResourceID;
    }

    public final boolean supportsBatchDownloads() {
        return this.features.supportsBatchDownloads;
    }

    public final boolean supportsBatchUploads() {
        return this.features.supportsBatchUploads;
    }

    public final boolean supportsExternalLinks() {
        return this.features.supportsExternalLinks;
    }

    public final boolean supportsPaging() {
        return this.features.supportsPaging;
    }

    public VOSURI toURI(final Path path) throws URISyntaxException {
        return toURI(path.toString());
    }

    public VOSURI toURI(final Node node) throws URISyntaxException {
        final Path path = PathUtils.toPath(node);
        return toURI(path.toString());
    }

    public VOSURI toURI(final String path) throws URISyntaxException {
        return new VOSURI(new URI(this.nodeResourceID + PathUtils.ensureSlashPrepended(path)));
    }

    public VOSpaceClient getVOSpaceClient() {
        return new VOSpaceClient(this.resourceID);
    }

    /**
     * Features that require flags to disable it, or is generally optional.  Some Cavern style VOSpace services do not
     * support pagination (i.e. limit={number}&startURI={pageStartURI}), for example.
     */
    public static final class Features {
        private boolean supportsBatchDownloads = false;
        private boolean supportsBatchUploads = false;
        private boolean supportsExternalLinks = false;
        private boolean supportsPaging = false;

        public Features() {

        }

        Features(boolean supportsBatchDownloads, boolean supportsBatchUploads, boolean supportsExternalLinks, boolean supportsPaging) {
            this.supportsBatchDownloads = supportsBatchDownloads;
            this.supportsBatchUploads = supportsBatchUploads;
            this.supportsExternalLinks = supportsExternalLinks;
            this.supportsPaging = supportsPaging;
        }

        public void supportsBatchDownloads() {
            this.supportsBatchDownloads = true;
        }

        public void supportsBatchUploads() {
            this.supportsBatchUploads = true;
        }

        public void supportsExternalLinks() {
            this.supportsExternalLinks = true;
        }

        public void supportsPaging() {
            this.supportsPaging = true;
        }
    }
}
