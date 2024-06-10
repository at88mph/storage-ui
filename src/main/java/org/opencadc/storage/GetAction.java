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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.opencadc.storage.config.StorageConfiguration;
import org.opencadc.storage.node.FileHandler;
import org.opencadc.storage.node.FolderHandler;
import org.opencadc.storage.node.LinkHandler;
import org.opencadc.storage.node.PageHandler;
import org.opencadc.token.Client;
import org.opencadc.vospace.VOSURI;


public class GetAction extends StorageAction {
    @Override
    public void doAction() throws Exception {
        final StorageItemContext storageItemType = getStorageItemType();
        if (this.currentService == null) {
            if (storageItemType == StorageItemContext.OIDC_CALLBACK) {
                handleOIDCCallback();
            } else if (storageItemType == StorageItemContext.OIDC_LOGIN) {
                handleOIDCLogin();
            } else {
                redirectDefaultService();
            }
        } else {
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
                    handlePage();
                    break;
                default: {
                    throw new UnsupportedOperationException("No GET support for " + storageItemType + " with service " + this.currentService.getName());
                }
            }
        }
    }

    void handleFolder() throws Exception {
        final FolderHandler folderHandler = new FolderHandler(this.currentService, getCurrentSubject(), getStorageItemFactory());
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
        final FolderHandler folderHandler = new FolderHandler(this.currentService, getCurrentSubject(), getStorageItemFactory());
        final Writer writer = new OutputStreamWriter(this.syncOutput.getOutputStream());
        folderHandler.writePage(getCurrentPath(), null, getVOSpaceServiceList(), getDisplayName(),
                                this.storageConfiguration.getFreeMarkerConfiguration(this.syncInput.getContextPath()), writer);
    }

    void handlePage() throws Exception {
        final String startURIParameterValue = this.syncInput.getParameter("startURI");
        final String pageSizeParameterValue = this.syncInput.getParameter("pageSize");
        final VOSURI startURI = StringUtil.hasLength(startURIParameterValue) ? new VOSURI(URI.create(startURIParameterValue)) : null;
        final Integer pageSize = StringUtil.hasLength(pageSizeParameterValue) ? Integer.parseInt(pageSizeParameterValue) : null;
        final Writer writer = new OutputStreamWriter(this.syncOutput.getOutputStream());

        final PageHandler pageHandler = new PageHandler(this.currentService, getCurrentSubject(), this.currentService.toURI(getCurrentPath()), startURI);
        pageHandler.writePage(writer, getStorageItemFactory(), pageSize);
    }

    void handleOIDCCallback() throws Exception {
        final Client oidcClient = getOIDCClient();
        final byte[] encryptedKey = oidcClient.setAccessToken(getRequestURI());
        setCookie(encryptedKey);
        redirectTo(HttpServletResponse.SC_FOUND, getOIDCClient().getCallbackURL().toExternalForm());
    }

    void handleOIDCLogin() throws Exception {
        redirectTo(HttpServletResponse.SC_FOUND, getOIDCClient().getAuthorizationURL().toExternalForm());
    }

    void setCookie(final byte[] encryptedAssetsKey) {
        syncOutput.setHeader("set-cookie", String.format(StorageConfiguration.COOKIE_FORMAT,
                                                         new String(encryptedAssetsKey,
                                                                    StandardCharsets.ISO_8859_1)));
    }

    URI getRequestURI() {
        final String requestSchemeHostPath = this.syncInput.getRequestURI();
        final String requestQueryString =
            this.syncInput.getParameterNames().stream()
                          .map(parameterName -> String.format("%s=%s", parameterName,
                                                              this.syncInput.getParameter(parameterName)))
                          .collect(Collectors.joining("&"));
        return URI.create(requestSchemeHostPath + (StringUtil.hasText(requestQueryString) ? "?" + requestQueryString : ""));
    }
}
