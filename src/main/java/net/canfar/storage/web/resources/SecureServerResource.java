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

package net.canfar.storage.web.resources;

import ca.nrc.cadc.accesscontrol.AccessControlClient;
import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.auth.AuthorizationTokenPrincipal;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.auth.SSOCookieCredential;
import ca.nrc.cadc.reg.client.RegistryClient;
import net.canfar.storage.web.config.StorageConfiguration;
import net.canfar.storage.web.config.VOSpaceServiceConfigManager;
import net.canfar.storage.web.restlet.StorageApplication;

import org.apache.log4j.Logger;
import org.opencadc.token.Client;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


class SecureServerResource extends ServerResource {
    private static final Logger LOGGER = Logger.getLogger(SecureServerResource.class);
    final StorageConfiguration storageConfiguration;

    public SecureServerResource() {
        this(new StorageConfiguration());
    }

    /**
     * Full constructor.  Useful for testing, or overriding default configuration.
     *
     * @param storageConfiguration The main configuration.
     */
    SecureServerResource(final StorageConfiguration storageConfiguration) {
        this.storageConfiguration = storageConfiguration;
    }

    @SuppressWarnings("unchecked")
    <T> T getRequestAttribute(final String attributeName) {
        final Map<String, Object> requestAttributes = getRequestAttributes();
        final Object o = requestAttributes.get(attributeName);
        return (o == null) ? null : (T) getRequestAttributes().get(attributeName);
    }

    @SuppressWarnings("unchecked")
    <T> T getContextAttribute(final String attributeName) {
        return (T) getContext().getAttributes().get(attributeName);
    }

    RegistryClient getRegistryClient() {
        return new RegistryClient();
    }

    Subject getCurrentSubject() throws Exception {
        final Cookie firstPartyCookie =
                getRequest().getCookies().getFirst(StorageConfiguration.FIRST_PARTY_COOKIE_NAME);
        final Subject subject = AuthenticationUtil.getCurrentSubject();

        if (firstPartyCookie != null && storageConfiguration.isOIDCConfigured()) {
            long startTime = System.currentTimeMillis();
            try {
                LOGGER.debug("Obtaining access token");
                final String accessToken = getOIDCClient().getAccessToken(firstPartyCookie.getValue());
                LOGGER.debug(String.format("Obtaining access token: OK - (%d seconds)",
                                           (System.currentTimeMillis() - startTime) / 1000));

                subject.getPrincipals().add(new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER,
                                                                            AuthenticationUtil.CHALLENGE_TYPE_BEARER
                                                                            + " " + accessToken));
                subject.getPublicCredentials().add(
                        new AuthorizationToken(AuthenticationUtil.CHALLENGE_TYPE_BEARER, accessToken,
                                               Collections.singletonList(
                                                       URI.create(getRequest().getResourceRef().toString()).getHost())));


                if (!subject.getPrincipals(AuthorizationTokenPrincipal.class).isEmpty()) {
                    startTime = System.currentTimeMillis();
                    LOGGER.debug("Validating subject.");
                    // Ensure it's clean first.
                    subject.getPublicCredentials(AuthMethod.class)
                           .forEach(authMethod -> subject.getPublicCredentials().remove(authMethod));
                    subject.getPublicCredentials().add(AuthMethod.TOKEN);

                    final Subject validatedSubject = AuthenticationUtil.getIdentityManager().validate(subject);

                    LOGGER.debug(String.format("Validating subject: OK - (%d seconds)",
                                               (System.currentTimeMillis() - startTime) / 1000));

                    return validatedSubject;
                }
            } catch (NoSuchElementException noSuchElementException) {
                // No Asset found
            }
        } else {
            // Ensure the valid backend Domains is added in.
            // TODO: Is this insecure?  It seems like a bit of a bastardization, but the assumption is that if the
            // TODO: caller has a cookie, then they must be valid, right?
            // TODO: jenkinsd 2024.01.24
            //
            final Set<SSOCookieCredential> cookieCredentialSet =
                    subject.getPublicCredentials(SSOCookieCredential.class);
            if (!cookieCredentialSet.isEmpty()) {
                final SSOCookieCredential firstCredential = cookieCredentialSet.stream().findFirst().get();
                final VOSpaceServiceConfigManager voSpaceServiceConfigManager =
                        new VOSpaceServiceConfigManager(this.storageConfiguration);
                voSpaceServiceConfigManager.getServiceList().forEach(serviceName -> {
                    final String domain = voSpaceServiceConfigManager.getServiceConfig(serviceName)
                                                                     .getResourceID().getHost();

                    subject.getPublicCredentials().add(new SSOCookieCredential(firstCredential.getSsoCookieValue(),
                                                                               domain,
                                                                               firstCredential.getExpiryDate()));
                });
            }
        }

        return subject;
    }

    protected String getDisplayName() throws Exception {
        final IdentityManager identityManager = AuthenticationUtil.getIdentityManager();
        return identityManager.toDisplayString(getCurrentSubject());
    }

    ServletContext getServletContext() {
        final Map<String, Object> attributes = getApplication().getContext().getAttributes();
        return (ServletContext) attributes.get(StorageApplication.SERVLET_CONTEXT_ATTRIBUTE_KEY);
    }

    /**
     * Set a default context path when this is not running in a servlet
     * container.
     *
     * @return String path.
     */
    String getContextPath() {
        return (getServletContext() == null)
               ? StorageApplication.DEFAULT_CONTEXT_PATH : getServletContext().getContextPath();
    }

    Client getOIDCClient() throws IOException {
        return this.storageConfiguration.getOIDCClient();
    }

    AccessControlClient getAccessControlClient() {
        return storageConfiguration.getAccessControlClient();
    }

    /**
     * Write out the given status and representation body to the response.
     *
     * @param status         The Status to set.
     * @param representation The representation used for the body of the response.
     */
    void writeResponse(final Status status, final Representation representation) {
        final Response response = getResponse();

        response.setStatus(status);
        response.setEntity(representation);
    }
}
