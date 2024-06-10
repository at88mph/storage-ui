package org.opencadc.storage.node;

import ca.nrc.cadc.auth.NotAuthenticatedException;
import java.io.IOException;
import java.io.Writer;
import javax.security.auth.Subject;
import org.opencadc.storage.CSVStorageItemProducer;
import org.opencadc.storage.StorageItemCSVWriter;
import org.opencadc.storage.StorageItemFactory;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.VOSURI;
import org.restlet.resource.ResourceException;

public class PageHandler extends StorageHandler {
    private final VOSURI currentItemURI;
    private final VOSURI startItemURI;

    public PageHandler(VOSpaceServiceConfig currentService, Subject subject, final VOSURI currentItemURI, final VOSURI startItemURI) {
        super(currentService, subject);
        this.currentItemURI = currentItemURI;
        this.startItemURI = startItemURI;
    }

    public void writePage(final Writer writer, final StorageItemFactory storageItemFactory, final Integer pageSize) throws Exception {
        final CSVStorageItemProducer csvNodeProducer = getStorageItemProducer(writer, storageItemFactory, pageSize);

        try {
            csvNodeProducer.writePage();
            writer.flush();
        } catch (IllegalArgumentException e) {
            // Very specific hack to try again without the (possibly) unsupported limit parameter.
            if (pageSize != null
                && e.getMessage().startsWith("OptionNotSupported")) {
                final CSVStorageItemProducer pagelessProducer = getStorageItemProducer(writer, storageItemFactory, null);
                try {
                    pagelessProducer.writePage();
                    writer.flush();
                } catch (Exception innerException) {
                    throw new ResourceException(innerException);
                }
            } else {
                throw new ResourceException(e);
            }
        } catch (RuntimeException runtimeException) {
            // Working around a bug where the RegistryClient improperly handles an unauthenticated request.
            if (runtimeException.getCause() != null
                && (runtimeException.getCause() instanceof IOException)
                && runtimeException.getCause().getCause() != null
                && (runtimeException.getCause().getCause() instanceof NotAuthenticatedException)) {
                throw new ResourceException(runtimeException.getCause().getCause());
            } else {
                throw runtimeException;
            }
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    CSVStorageItemProducer getStorageItemProducer(final Writer writer, final StorageItemFactory storageItemFactory, final Integer pageSize) {
        return new CSVStorageItemProducer(pageSize, currentItemURI, startItemURI, new StorageItemCSVWriter(writer), this.subject, storageItemFactory,
                                          currentService, getVOSpaceClient());
    }
}
