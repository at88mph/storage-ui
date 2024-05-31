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

import ca.nrc.cadc.io.ByteCountOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;


public class UploadOutputStreamWrapperImpl implements UploadOutputStreamWrapper
{
    private static final int DEFAULT_BUFFER_SIZE = 8192;


    private InputStream sourceInputStream;
    private long byteCount;
    private byte[] calculatedMD5;
    private int bufferSize;


    /**
     * Constructor to use the default buffer size.
     *
     * @param sourceInputStream The source stream.
     */
    public UploadOutputStreamWrapperImpl(final InputStream sourceInputStream)
    {
        this(sourceInputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Full Constructor.  Provide the source stream to read from, and the
     * desired buffer size.
     *
     * @param sourceInputStream The source stream.
     * @param bufferSize        The desired buffer size while reading.
     */
    public UploadOutputStreamWrapperImpl(final InputStream sourceInputStream,
                                         final int bufferSize)
    {
        this.sourceInputStream = sourceInputStream;
        this.bufferSize = bufferSize;
    }


    /**
     * Perform the write operation to the given output.
     *
     * @param out The output to write to.
     * @throws IOException For any unhandled error.
     */
    public void write(final OutputStream out) throws IOException
    {
        if (out == null)
        {
            throw new IllegalArgumentException(
                    "BUG - Given OutputStream cannot be null.");
        }

        final MessageDigest messageDigest = getMD5Digest();
        final ByteCountOutputStream outputStream =
                new ByteCountOutputStream(out);
        final BufferedInputStream bis =
                new BufferedInputStream(getSourceInputStream(), getBufferSize());
        final byte[] buffer = new byte[getBufferSize()];
        int bytesRead;

        while ((bytesRead = bis.read(buffer)) >= 0)
        {
            messageDigest.update(buffer, 0, bytesRead);
            outputStream.write(buffer, 0, bytesRead);
        }

        setByteCount(outputStream.getByteCount());
        setCalculatedMD5(messageDigest.digest());
    }

    /**
     * Obtain a new MD5 digester.
     *
     * @return MessageDigest instance.
     */
    protected MessageDigest getMD5Digest()
    {
        try
        {
            return MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }
    }


    protected InputStream getSourceInputStream()
    {
        return sourceInputStream;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public long getByteCount()
    {
        return byteCount;
    }

    public void setByteCount(long byteCount)
    {
        this.byteCount = byteCount;
    }

    public byte[] getCalculatedMD5()
    {
        return calculatedMD5;
    }

    public void setCalculatedMD5(byte[] calculatedMD5)
    {
        this.calculatedMD5 = calculatedMD5;
    }
}
