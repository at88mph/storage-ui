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

import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.net.RemoteServiceException;
import ca.nrc.cadc.reg.client.RegistryClient;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import net.canfar.storage.PathUtils;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.Node;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.VOSURI;
import org.opencadc.vospace.client.VOSpaceClient;


public abstract class StorageHandler {
    // Page size for the initial page display.
    private static final int DEFAULT_DISPLAY_PAGE_SIZE = 35;
    final Subject subject;
    final VOSpaceServiceConfig currentService;


    StorageHandler(final VOSpaceServiceConfig currentService, final Subject subject) {
        this.subject = subject;
        this.currentService = currentService;
    }

    <T extends Node> T getNode(final Path nodePath) {
        return getNode(nodePath, VOS.Detail.max, StorageHandler.DEFAULT_DISPLAY_PAGE_SIZE);
    }

    VOSpaceClient getVOSpaceClient() {
        return new VOSpaceClient(this.currentService.getResourceID());
    }

    @SuppressWarnings("unchecked")
    final <T extends Node> T getNode(final Path nodePath, final VOS.Detail detail, final Integer limit) {
        final Map<String, Object> queryPayload = new HashMap<>();
        if (limit != null) {
            queryPayload.put("limit", limit);
        }

        if (detail != null) {
            queryPayload.put("detail", detail.name());
        }

        final String query = queryPayload.entrySet().stream()
                                         .map(entry -> entry.getKey() + "=" + entry.getValue())
                                         .collect(Collectors.joining("&"));

        try {
            final T currentNode = Subject.doAs(subject, (PrivilegedExceptionAction<T>) () ->
                (T) getVOSpaceClient().getNode(nodePath.toString(), query));
            if (currentNode != null) {
                PathUtils.augmentParents(nodePath, currentNode);
            }
            return currentNode;
        } catch (IllegalArgumentException e) {
            // Very specific hack to try again without the (possibly) unsupported limit parameter.
            if (limit != null
                && e.getMessage().startsWith("OptionNotSupported")) {
                return getNode(nodePath, detail, null);
            } else {
                throw new RuntimeException(e.getMessage(), e);
            }
        } catch (RemoteServiceException remoteServiceException) {
            // For Cavern backend systems, some kind of authenticated access is required, and a RemoteServiceException
            // is thrown as a result, which results in a 500 Server Error.  Catch it here to re-map it as an
            // authentication problem and encourage the User to sign in.
            // jenkinsd 2024.02.03
            //
            if (remoteServiceException.getMessage().contains("PosixMapperClient.augment(Subject)")) {
                throw new NotAuthenticatedException("Nobody is authenticated.");
            } else {
                throw remoteServiceException;
            }
        } catch (RuntimeException runtimeException) {
            // Working around a bug where the RegistryClient improperly handles an unauthenticated request.
            if (runtimeException.getCause() != null) {
                if (runtimeException.getCause() instanceof IOException
                    && runtimeException.getCause().getCause() != null
                    && (runtimeException.getCause().getCause() instanceof NotAuthenticatedException)) {
                    throw new RuntimeException(runtimeException.getCause().getCause());
                } else if (runtimeException.getCause().getMessage().contains("PosixMapperClient.augment(Subject)")) {
                    // More hacks for the base call being unauthenticated.
                    throw new NotAuthenticatedException("Nobody is authenticated.");
                } else {
                    throw runtimeException;
                }
            } else {
                throw runtimeException;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    <T extends Node> T getNode(final Path nodePath, final VOS.Detail detail) {
        final int pageSize;

        if ((detail == VOS.Detail.max) || (detail == VOS.Detail.raw)) {
            pageSize = DEFAULT_DISPLAY_PAGE_SIZE;
        } else {
            pageSize = 0;
        }

        return getNode(nodePath, detail, pageSize);
    }

    void createNode(final Node newNode) throws Exception {
        executeSecurely((PrivilegedExceptionAction<Void>) () -> {
            getVOSpaceClient().createNode(toURI(newNode), newNode, false);
            return null;
        });
    }

    <T> void executeSecurely(final PrivilegedExceptionAction<T> runnable) throws Exception {
        try {
            Subject.doAs(this.subject, runnable);
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
    }

    VOSURI toURI(final Path path) {
        return new VOSURI(URI.create(this.currentService.getNodeResourceID() + path.toString()));
    }

    VOSURI toURI(final Node node) {
        final Path path = PathUtils.toPath(node);
        return toURI(path);
    }

    RegistryClient getRegistryClient() {
        return new RegistryClient();
    }
}
