package org.opencadc.storage;

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.InlineContentException;
import ca.nrc.cadc.rest.InlineContentHandler;
import java.io.IOException;
import java.io.InputStream;


/**
 * Just a hub for the InlineContentHandler.
 */
public class PutInlineContentHandler implements InlineContentHandler {
    @Override
    public Content accept(String name, String contentType, InputStream inputStream)
        throws InlineContentException, IOException, ResourceNotFoundException, TransientException {

        // A null name means it's the incoming stream we're looking for, rather than a parameter.
        if (name == null && contentType != null && contentType.equals("application/json")) {
            return new JSONInlineContentHandler().accept(null, contentType, inputStream);
        } else {
            return new FileUploadInlineContentHandler().accept(name, contentType, inputStream);
        }
    }
}
