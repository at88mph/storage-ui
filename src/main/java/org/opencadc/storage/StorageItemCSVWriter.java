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


import com.opencsv.CSVWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import net.canfar.storage.web.view.StorageItem;


public class StorageItemCSVWriter implements StorageItemWriter {
    private final CSVWriter csvWriter;


    public StorageItemCSVWriter(final Writer writer) {
        this(new CSVWriter(writer));
    }

    public StorageItemCSVWriter(CSVWriter csvWriter) {
        this.csvWriter = csvWriter;
    }


    @Override
    public void write(final StorageItem storageItem) {
        final List<String> row = new ArrayList<>();

        // Checkbox column [0]
        row.add("");

        // Name [1]
        row.add(storageItem.getName());

        // File size in human-readable format. [2]
        row.add(storageItem.getSizeHumanReadable());

        // Last Modified in human-readable format. [3]
        row.add(storageItem.getLastModifiedHumanReadable());

        // Write Group Names [4]
        row.add(storageItem.getWriteGroupNames());

        // Read Group Names [5]
        row.add(storageItem.getReadGroupNames());

        // Hidden items.

        // Is public flag. [6]
        row.add(Boolean.toString(storageItem.isPublic()));

        // Is Locked flag. [7]
        row.add(Boolean.toString(storageItem.isLocked()));

        // CSS for icon to display [8]
        row.add(storageItem.getItemIconCSS());

        // Path [9]
        row.add(storageItem.getPath());

        // URI [10]
        row.add(storageItem.getURI().toString());

        // Link for click action. [11]
        row.add(storageItem.getTargetPath());

        // Readable flag.  [12]
        row.add(Boolean.toString(storageItem.isReadable()));

        // Writable flag.  [13]
        row.add(Boolean.toString(storageItem.isWritable()));

        // Owner: distinguished name [14]
        // TODO: change this to human readable name when issue
        // of authentication for /ac/users/{userid}?typeId=http  is solved for this app
        row.add(storageItem.getOwnerCN());

        csvWriter.writeNext(row.toArray(new String[0]));
    }
}
