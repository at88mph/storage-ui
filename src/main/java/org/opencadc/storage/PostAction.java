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

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.InlineContentException;
import ca.nrc.cadc.rest.InlineContentHandler;
import ca.nrc.cadc.util.StringUtil;
import org.json.JSONObject;
import org.opencadc.gms.GroupURI;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOS;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


public class PostAction extends StorageAction {
    private final static String PAYLOAD_KEY = "payload";

    @Override
    public void doAction() throws Exception {
        final JSONObject jsonObject = (JSONObject) this.syncInput.getContent(PostAction.PAYLOAD_KEY);

        // limit=0, detail=min so should only get the current node
        final Node currentNode = getCurrentNode(VOS.Detail.properties);
        final Set<String> keySet = jsonObject.keySet();

        if (keySet.contains(JSONFormInputs.PUBLIC_FLAG.fieldName)) {
            currentNode.isPublic = jsonObject.get(JSONFormInputs.PUBLIC_FLAG.fieldName).equals("on");
        } else {
            currentNode.isPublic = false;
            currentNode.clearIsPublic = true;
        }

        currentNode.getReadOnlyGroup().clear();
        if (keySet.contains(JSONFormInputs.READ_GROUP_INPUT.fieldName)
            && StringUtil.hasText(jsonObject.getString(JSONFormInputs.READ_GROUP_INPUT.fieldName))) {
            final GroupURI newReadGroupURI =
                    new GroupURI(storageConfiguration.getGroupURI(
                            jsonObject.getString(JSONFormInputs.READ_GROUP_INPUT.fieldName)));
            currentNode.getReadOnlyGroup().add(newReadGroupURI);
        } else {
            currentNode.clearReadOnlyGroups = true;
        }

        currentNode.getReadWriteGroup().clear();
        if (keySet.contains(JSONFormInputs.WRITE_GROUP_INPUT.fieldName)
            && StringUtil.hasText(jsonObject.getString(JSONFormInputs.WRITE_GROUP_INPUT.fieldName))) {
            final GroupURI newReadWriteGroupURI =
                    new GroupURI(storageConfiguration.getGroupURI(
                            jsonObject.getString(JSONFormInputs.WRITE_GROUP_INPUT.fieldName)));
            currentNode.getReadWriteGroup().add(newReadWriteGroupURI);
        } else {
            currentNode.clearReadWriteGroups = true;
        }

        // Recursively set permissions if requested
        if (keySet.contains(JSONFormInputs.RECURSIVE_FLAG.fieldName)) {
            if (jsonObject.get(JSONFormInputs.RECURSIVE_FLAG.fieldName).equals("on")) {
                setNodeRecursiveSecure(currentNode);
                this.syncOutput.setCode(HttpServletResponse.SC_ACCEPTED);
            }
        } else {
            // Update the node properties
            setNodeSecure(currentNode);
            this.syncOutput.setCode(HttpServletResponse.SC_OK);
        }
    }

    @Override
    protected InlineContentHandler getInlineContentHandler() {
        return new JSONInlineContentHandler();
    }

    private static class JSONInlineContentHandler implements InlineContentHandler {
        @Override
        public Content accept(String name, String contentType, InputStream inputStream)
                throws InlineContentException, IOException, TransientException {
            if (contentType != null && contentType.equals("application/json")) {
                final String jsonString = new String(inputStream.readAllBytes());
                final JSONObject jsonObject = new JSONObject(jsonString);
                final InlineContentHandler.Content payloadContent = new InlineContentHandler.Content();
                payloadContent.name = PostAction.PAYLOAD_KEY;
                payloadContent.value = jsonObject;

                return payloadContent;
            } else {
                throw new InlineContentException("Unsupported content type: " + contentType);
            }
        }
    }

    private enum JSONFormInputs {
        PUBLIC_FLAG("public"),
        READ_GROUP_INPUT("readGroup"),
        WRITE_GROUP_INPUT("writeGroup"),
        RECURSIVE_FLAG("recursive");

        final String fieldName;

        JSONFormInputs(String fieldName) {
            this.fieldName = fieldName;
        }
    }
}
