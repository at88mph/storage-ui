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

import ca.nrc.cadc.net.ResourceNotFoundException;
import org.apache.log4j.Logger;
import org.opencadc.storage.NoSuchConfigurationException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;


public class VOSpaceServiceConfigManager {
    private static final Logger LOGGER = Logger.getLogger(VOSpaceServiceConfigManager.class);
    private final TreeMap<String, VOSpaceServiceConfig> serviceConfigMap = new TreeMap<>();

    private String defaultServiceName;
    private final List<String> serviceList = new ArrayList<>();

    // Properties files keys
    public static final String SERVICE_NAME_RESOURCE_ID_PROPERTY_KEY_FORMAT = "%s%s.service.resourceid";
    public static final String SERVICE_NODE_RESOURCE_ID_PROPERTY_KEY_FORMAT = "%s%s.node.resourceid";
    public static final String SERVICE_USER_HOME_PROPERTY_KEY_FORMAT = "%s%s.user.home";
    public static final String SERVICE_FEATURE_BATCH_UPLOAD_PROPERTY_KEY_FORMAT = "%s%s.service.features.batchUpload";
    public static final String SERVICE_FEATURE_BATCH_DOWNLOAD_PROPERTY_KEY_FORMAT =
            "%s%s.service.features.batchDownload";
    public static final String SERVICE_FEATURE_EXTERNAL_LINKS_PROPERTY_KEY_FORMAT =
            "%s%s.service.features.externalLinks";
    public static final String SERVICE_FEATURE_PAGING_PROPERTY_KEY_FORMAT = "%s%s.service.features.paging";

    // Used to construct service-specific property keys (ie KEY_BASE + {svc name} + {rest of key}
    public static final String KEY_BASE = "org.opencadc.vosui.";

    private final StorageConfiguration applicationConfiguration;


    public VOSpaceServiceConfigManager(StorageConfiguration appConfig) {
        this.applicationConfiguration = appConfig;
        loadConfig();
    }

    public void loadConfig() {
        this.serviceConfigMap.clear();
        this.serviceList.clear();

        // Grab and validate a default service name
        this.defaultServiceName = applicationConfiguration.getDefaultServiceName();

        // Get all VOSpace services named in the config file
        this.serviceList.addAll(Arrays.asList(applicationConfiguration.getServiceNames()));
        loadServices();
    }

    private void loadServices() {
        this.serviceList.forEach(storageServiceName -> {
            final String servicePrefixKey =
                    String.format(VOSpaceServiceConfigManager.SERVICE_NAME_RESOURCE_ID_PROPERTY_KEY_FORMAT,
                                  VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);

            LOGGER.debug("adding vospace service to map: " + servicePrefixKey + ": " + storageServiceName);

            final String vospaceResourceIDStr = applicationConfiguration.lookup(servicePrefixKey, true);
            final URI vospaceResourceID = URI.create(vospaceResourceIDStr);

            final String nodeResourcePrefixKey =
                    String.format(VOSpaceServiceConfigManager.SERVICE_NODE_RESOURCE_ID_PROPERTY_KEY_FORMAT,
                                  VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);
            final String nodeResourceIDStr = applicationConfiguration.lookup(nodeResourcePrefixKey, true);
            final URI nodeResourceID = URI.create(nodeResourceIDStr);

            LOGGER.debug("node resource id base: " + nodeResourceID);

            final String userHomePrefixKey =
                    String.format(VOSpaceServiceConfigManager.SERVICE_USER_HOME_PROPERTY_KEY_FORMAT,
                                  VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);
            final String userHomeStr = applicationConfiguration.lookup(userHomePrefixKey, true);

            // At this point, the values have been validated
            final VOSpaceServiceConfig voSpaceServiceConfig = new VOSpaceServiceConfig(storageServiceName,
                                                                                       vospaceResourceID,
                                                                                       nodeResourceID,
                                                                                       getFeatures(storageServiceName));
            voSpaceServiceConfig.homeDir = userHomeStr;

            this.serviceConfigMap.put(storageServiceName, voSpaceServiceConfig);
        });
    }

    public VOSpaceServiceConfig getServiceConfig(String serviceName) throws ResourceNotFoundException {
        if (!this.serviceConfigMap.containsKey(serviceName)) {
            throw new ResourceNotFoundException("No such service configured: " + serviceName);
        } else {
            return this.serviceConfigMap.get(serviceName);
        }
    }

    public String getDefaultServiceName() {
        return defaultServiceName;
    }

    public List<String> getServiceList() {
        final List<String> listToSort = new ArrayList<>(this.serviceList);
        Collections.sort(listToSort);
        Collections.reverse(listToSort);
        LOGGER.debug("sorted service list: " + listToSort);
        return listToSort;
    }

    VOSpaceServiceConfig.Features getFeatures(final String storageServiceName) {
        final VOSpaceServiceConfig.Features features = new VOSpaceServiceConfig.Features();

        final String supportsBatchDownloadProperty =
                String.format(VOSpaceServiceConfigManager.SERVICE_FEATURE_BATCH_DOWNLOAD_PROPERTY_KEY_FORMAT,
                              VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);
        final boolean supportsBatchDownload = applicationConfiguration.lookupFlag(supportsBatchDownloadProperty, true);
        if (supportsBatchDownload) {
            features.supportsBatchDownloads();
        }

        final String supportsBatchUploadProperty =
                String.format(VOSpaceServiceConfigManager.SERVICE_FEATURE_BATCH_UPLOAD_PROPERTY_KEY_FORMAT,
                              VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);
        final boolean supportsBatchUpload = applicationConfiguration.lookupFlag(supportsBatchUploadProperty, true);
        if (supportsBatchUpload) {
            features.supportsBatchUploads();
        }

        final String supportsExternalLinksProperty =
                String.format(VOSpaceServiceConfigManager.SERVICE_FEATURE_EXTERNAL_LINKS_PROPERTY_KEY_FORMAT,
                              VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);
        final boolean supportsExternalLinks = applicationConfiguration.lookupFlag(supportsExternalLinksProperty, true);
        if (supportsExternalLinks) {
            features.supportsExternalLinks();
        }

        final String supportsPagingProperty =
                String.format(VOSpaceServiceConfigManager.SERVICE_FEATURE_PAGING_PROPERTY_KEY_FORMAT,
                              VOSpaceServiceConfigManager.KEY_BASE, storageServiceName);
        final boolean supportsPaging = applicationConfiguration.lookupFlag(supportsPagingProperty, true);
        if (supportsPaging) {
            features.supportsPaging();
        }

        return features;
    }
}
