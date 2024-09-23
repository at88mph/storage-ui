package org.opencadc.storage;

import java.net.URI;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.storage.config.VOSpaceServiceConfig;

public class RequestPathParserTest {
    private static final String CONTEXT_PATH = "/storage";
    private static final String[] PATHS = new String[] {
        String.format("%s/%s", RequestPathParserTest.CONTEXT_PATH, StorageAction.StorageItemContext.LIST.endpoint),
        String.format("%s/%s/", RequestPathParserTest.CONTEXT_PATH, StorageAction.StorageItemContext.LIST.endpoint),
        String.format("%s/%s", RequestPathParserTest.CONTEXT_PATH, StorageAction.StorageItemContext.GROUPS.endpoint),
        String.format("%s/%s/", RequestPathParserTest.CONTEXT_PATH, StorageAction.StorageItemContext.GROUPS.endpoint)
    };

    @Test
    public void parse() throws Exception {
        final String endpoint = String.format("%s/%s", RequestPathParserTest.CONTEXT_PATH, StorageAction.StorageItemContext.LIST.endpoint);
        final VOSpaceServiceConfig defaultService = new VOSpaceServiceConfig("test", URI.create("ivo://example.org/vospace"), URI.create("vos://example.org"),
                                                                             VOSpaceServiceConfig.Features.create().withBatchDownloads());
        final RequestPathParser testSubject = new RequestPathParser(endpoint, defaultService);
        Assert.assertEquals("Wrong real path.", Path.of(String.format("%s/test/list", RequestPathParserTest.CONTEXT_PATH)),
                            testSubject.getRealPath());
    }
}
