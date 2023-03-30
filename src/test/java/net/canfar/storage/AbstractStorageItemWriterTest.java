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

package net.canfar.storage;

import net.canfar.storage.web.view.StorageItem;
import ca.nrc.cadc.vos.VOSURI;

import static org.easymock.EasyMock.*;


public abstract class AbstractStorageItemWriterTest<T extends StorageItemWriter>
        extends AbstractUnitTest<T> {

    <T2 extends StorageItem> T2 mockStorageItem(final String name,
                                                final String sizeHumanReadable,
                                                final String writeGroups,
                                                final String readGroups,
                                                final String dateHumanReadable,
                                                final boolean isPublic,
                                                final boolean isLocked,
                                                final Class<T2> type,
                                                final String itemCSS,
                                                final VOSURI uri,
                                                final String linkURI,
                                                final boolean isReadable,
                                                final Boolean isWritable,
                                                final String owner)
            throws Exception {
        final T2 mockStorageItem = createMock(type);

        expect(mockStorageItem.getName()).andReturn(name).once();
        expect(mockStorageItem.getSizeHumanReadable()).andReturn(
                sizeHumanReadable).once();
        expect(mockStorageItem.getLastModifiedHumanReadable()).andReturn(
                dateHumanReadable).once();
        expect(mockStorageItem.getWriteGroupNames()).andReturn(writeGroups).
                                                                                   once();
        expect(mockStorageItem.getReadGroupNames()).andReturn(readGroups).
                                                                                 once();

        // Hidden on main UI.
        expect(mockStorageItem.isPublic()).andReturn(isPublic).once();
        expect(mockStorageItem.isLocked()).andReturn(isLocked).once();
        expect(mockStorageItem.getItemIconCSS()).andReturn(itemCSS).once();
        expect(mockStorageItem.getPath()).andReturn(uri.getPath()).once();
        expect(mockStorageItem.getURI()).andReturn(uri).once();
        expect(mockStorageItem.getTargetURL()).andReturn(linkURI).once();
        expect(mockStorageItem.isReadable()).andReturn(isReadable).once();
        expect(mockStorageItem.isWritable()).andReturn(isWritable).once();
        expect(mockStorageItem.getOwnerCN()).andReturn(owner).once();

        return mockStorageItem;
    }
}
