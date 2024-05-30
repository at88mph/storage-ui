package org.opencadc.storage;

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.InlineContentException;
import ca.nrc.cadc.rest.InlineContentHandler;
import java.io.IOException;
import java.io.InputStream;
import javax.security.auth.Subject;
import net.canfar.storage.web.UploadVerifier;
import org.opencadc.storage.config.VOSpaceServiceConfig;


/**
 * Just a hub for the InlineContentHandler.
 */
public class PutInlineContentHandler implements InlineContentHandler {
    private final VOSpaceServiceConfig currentService;
    private final Subject subject;
    private final UploadVerifier uploadVerifier;


    public PutInlineContentHandler(VOSpaceServiceConfig currentService, Subject subject, UploadVerifier uploadVerifier) {
        this.currentService = currentService;
        this.subject = subject;
        this.uploadVerifier = uploadVerifier;
    }

    @Override
    public Content accept(String name, String contentType, InputStream inputStream)
        throws InlineContentException, IOException, ResourceNotFoundException, TransientException {
        if (name == null && contentType != null && contentType.equals("application/json")) {
            return new JSONInlineContentHandler().accept(null, contentType, inputStream);
        } else {
            return new FileUploadInlineContentHandler(this.currentService, this.uploadVerifier, subject).accept(name, contentType, inputStream);
        }
    }
}
