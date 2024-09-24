package org.opencadc.storage;

import ca.nrc.cadc.net.ResourceNotFoundException;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opencadc.storage.config.VOSpaceServiceConfigManager;


public class RequestPathParserTest {
    private static final String CONTEXT_PATH = "/storage";

    final VOSpaceServiceConfigManager mockVOSpaceConfigManager = Mockito.mock(VOSpaceServiceConfigManager.class);

    @Test
    public void parseError() throws Exception {
        try {
            new RequestPathParser(null, this.mockVOSpaceConfigManager);
            Assert.fail("Should throw IllegalStateException.");
        } catch (IllegalStateException illegalStateException) {
            // Good.
        }

        try {
            new RequestPathParser("/path", null);
            Assert.fail("Should throw IllegalStateException.");
        } catch (IllegalStateException illegalStateException) {
            // Good.
        }

        try {
            final RequestPathParser requestPathParser = new RequestPathParser("/user-storage/service-name/list", this.mockVOSpaceConfigManager);
            Mockito.when(this.mockVOSpaceConfigManager.getServiceConfig("service-name")).thenThrow(new ResourceNotFoundException(""));
            requestPathParser.getRealPath();
            Assert.fail("Should throw IllegalStateException.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good.
        } finally {
            Mockito.verify(this.mockVOSpaceConfigManager, Mockito.times(1)).getServiceConfig("service-name");
        }
    }

    @Test
    public void parseToDefault() {
        final String serviceName = "test";
        Mockito.when(mockVOSpaceConfigManager.getDefaultServiceName()).thenReturn(serviceName);

        final StorageItemContext contextVerb = StorageItemContext.LIST;
        final String endpoint = String.format("%s/%s", RequestPathParserTest.CONTEXT_PATH, contextVerb.endpoint);
        comparePaths(endpoint, Path.of(String.format("%s/%s/%s", RequestPathParserTest.CONTEXT_PATH, serviceName,
                                                     contextVerb.endpoint)), contextVerb, serviceName);
        Mockito.verify(mockVOSpaceConfigManager, Mockito.times(3)).getDefaultServiceName();

        Mockito.reset(mockVOSpaceConfigManager);
        final String serviceName2 = "test2";
        Mockito.when(mockVOSpaceConfigManager.getDefaultServiceName()).thenReturn(serviceName2);
        final StorageItemContext contextVerb2 = StorageItemContext.LIST;
        final String endpointBase = String.format("%s", RequestPathParserTest.CONTEXT_PATH);
        comparePaths(endpointBase, Path.of(String.format("%s/%s/%s", RequestPathParserTest.CONTEXT_PATH, serviceName2,
                                                         contextVerb2.endpoint)), contextVerb2, serviceName2);
        Mockito.verify(mockVOSpaceConfigManager, Mockito.times(3)).getDefaultServiceName();

        Mockito.reset(mockVOSpaceConfigManager);
        final String serviceName3 = "test3";
        Mockito.when(mockVOSpaceConfigManager.getDefaultServiceName()).thenReturn(serviceName3);
        final StorageItemContext contextVerb3 = StorageItemContext.PAGE;
        final String endpoint3 = String.format("%s/%s/%s/", RequestPathParserTest.CONTEXT_PATH, serviceName3, contextVerb3.endpoint);
        comparePaths(endpoint3, Path.of(String.format("%s/%s/%s/", RequestPathParserTest.CONTEXT_PATH, serviceName3,
                                                         contextVerb3.endpoint)), contextVerb3, serviceName3);
        Mockito.verify(mockVOSpaceConfigManager, Mockito.times(3)).getDefaultServiceName();

        Mockito.reset(mockVOSpaceConfigManager);
        final String serviceName4 = "test4";
        Mockito.when(mockVOSpaceConfigManager.getDefaultServiceName()).thenReturn(serviceName4);

        final StorageItemContext contextVerb4 = StorageItemContext.PAGE;
        final String endpoint4 = String.format("%s/%s/path/to/node", RequestPathParserTest.CONTEXT_PATH, contextVerb4.endpoint);
        comparePaths(endpoint4, Path.of(String.format("%s/%s/%s/path/to/node", RequestPathParserTest.CONTEXT_PATH, serviceName4,
                                                      contextVerb4.endpoint)), contextVerb4, serviceName4);
        Mockito.verify(mockVOSpaceConfigManager, Mockito.times(6)).getDefaultServiceName();
    }

    private void comparePaths(final String endpoint, final Path expectedPath, final StorageItemContext expectedContextVerb, final String expectedService) {

        final RequestPathParser testSubject = new RequestPathParser(endpoint, mockVOSpaceConfigManager);
        Assert.assertEquals("Wrong real path.", expectedPath, testSubject.getRealPath());
        Assert.assertEquals("Wrong service name.", expectedService, testSubject.getServiceName());
        Assert.assertEquals("Wrong context verb", expectedContextVerb, testSubject.getContextVerb());
    }
}
