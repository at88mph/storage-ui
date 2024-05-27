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

package org.opencadc.storage;


import ca.nrc.cadc.util.StringUtil;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.canfar.storage.web.view.FolderItem;
import org.opencadc.storage.node.FileHandler;
import org.opencadc.storage.node.FolderHandler;
import org.opencadc.storage.node.LinkHandler;
import org.opencadc.vospace.ContainerNode;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;


public class GetAction extends StorageAction {
    @Override
    public void doAction() throws Exception {
        final StorageItemContext storageItemType = getStorageItemType();

        switch (storageItemType) {
            case FOLDER:
                handleFolder();
                break;
            case LINK:
                handleLink();
                break;
            case FILE:
                handleFile();
                break;
            case LIST:
                handleList();
                break;
            case PAGE:
        }
    }

    void handleFolder() throws Exception {
        final FolderHandler folderHandler = new FolderHandler(this.currentService, getCurrentSubject());
        final Writer writer = new OutputStreamWriter(this.syncOutput.getOutputStream());
        folderHandler.writeQuota(getCurrentPath(), writer);
    }

    void handleLink() throws Exception {
        final LinkHandler linkHandler = new LinkHandler(this.currentService, getCurrentSubject());
        redirectSeeOther(linkHandler.resolve(getCurrentPath(), getStorageItemFactory()).toString());
    }

    void handleFile() throws Exception {
        final FileHandler fileHandler = new FileHandler(this.currentService, getCurrentSubject());
        redirectSeeOther(fileHandler.getDownloadEndpoint(getCurrentPath()));
    }

    void handleList() throws Exception {
        final FolderHandler folderHandler = new FolderHandler(this.currentService, getCurrentSubject());
        final String pathString = getCurrentPath().toString();
        final VOS.Detail detail = (!StringUtil.hasText(pathString) || pathString.trim().equals("/"))
                                  ? VOS.Detail.raw : VOS.Detail.max;
        final Iterator<String> initialRows = folderHandler.iterator(getCurrentPath(), getStorageItemFactory());
    }

    Representation representFolderItem(final FolderItem folderItem, final Iterator<String> initialRows,
                                       final VOSURI startNextPageURI) throws Exception {
        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("initialRows", initialRows);

        // Explicitly set whether folder is writable or not, handling null situation as equal to false
        dataModel.put("folderWritable", folderItem.isWritable());
        dataModel.put("folder", folderItem);

        if (startNextPageURI != null) {
            dataModel.put("startURI", startNextPageURI.toString());
        }

        // Add the current VOSpace service name so that navigation links can be rendered correctly
        String vospaceSvcName = this.currentService.getName();
        String nodePrefixURI = this.currentService.getNodeResourceID().toString();
        dataModel.put("vospaceSvcPath", vospaceSvcName + "/");
        dataModel.put("vospaceSvcName", vospaceSvcName);
        dataModel.put("vospaceNodePrefixURI", nodePrefixURI);

        // Used to populate VOSpace service dropdown
        dataModel.put("vospaceServices", getVOSpaceServiceList());

        final String httpUsername = getDisplayName();

        if (httpUsername != null) {
            dataModel.put("username", httpUsername);

            try {
                // Check to see if home directory exists
                final String userHomeBase = this.currentService.homeDir;
                if (StringUtil.hasLength(userHomeBase)) {
                    final Path userHomePath = Paths.get(userHomeBase, httpUsername);
//                    getNode(userHomePath, null, 0);
                    dataModel.put("homeDir", userHomePath.toString());
                }
            } catch (ResourceException re) {
                // Ignore this as there is no 'home' VOSpace defined in org.opencadc.vosui.properties
            }
        }

        final Map<String, Boolean> featureMap = new HashMap<>();
        featureMap.put("batchDownload", currentService.supportsBatchDownloads());
        featureMap.put("batchUpload", currentService.supportsBatchUploads());
        featureMap.put("externalLinks", currentService.supportsExternalLinks());
        featureMap.put("paging", currentService.supportsPaging());

        dataModel.put("features", featureMap);

        return new TemplateRepresentation(String.format("themes/%s/index.ftl", storageConfiguration.getThemeName()),
                                          storageConfiguration.getFreeMarkerConfiguration(this.syncInput.getContextPath()), dataModel, MediaType.TEXT_HTML);
    }
}
