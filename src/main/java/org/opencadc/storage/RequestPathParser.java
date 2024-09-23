package org.opencadc.storage;

import ca.nrc.cadc.util.StringUtil;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.opencadc.storage.config.VOSpaceServiceConfig;

/**
 * Handles incoming path requests and provides "real" path translations.
 * <p />
 * Paths should have at least three items:
 * <code>/{servlet-context}/{service-name}/{action-verb}</code>
 * <p />
 * Where there are fewer than three, this parser should provide the redirect.
 * For example, consider this incoming request path:
 * <code>/example-storage/list</code>
 * Should redirect to a default service:
 * <code>/example-storage/default-service/list</code>
 * So that any down-stream actors can accurately deduce how to proceed.
 */
public class RequestPathParser {
    private static final StorageItemContext DEFAULT_CONTEXT_VERB = StorageItemContext.LIST;
    private final Path requestPath;
    private final VOSpaceServiceConfig defaultService;

    public RequestPathParser(String requestPath, VOSpaceServiceConfig defaultService) {
        if (!StringUtil.hasText(requestPath)) {
            throw new IllegalStateException("Input requestPath cannot be null");
        } else if (defaultService == null) {
            throw new IllegalStateException("Default VOSpaceService is required.");
        }

        try {
            this.requestPath = Path.of(requestPath);
        } catch (InvalidPathException invalidPathException) {
            throw new IllegalStateException(invalidPathException.getMessage(), invalidPathException);
        }

        this.defaultService = defaultService;
    }

    /**
     * Decide if the current path is incomplete, and provide a redirect.
     * @return  Path to redirect to, if necessary.
     */
    Path getRealPath() {
        final int pathElementCount = requestPath.getNameCount();
        final Path realPath;

        if (pathElementCount == 2) {
            realPath = Path.of("/" + requestPath.getName(0), this.defaultService.getName(), requestPath.getName(1).toString());
        } else {
            realPath = requestPath;
        }

        return realPath;
    }

    enum StorageItemContext {
        FILE("file"),
        FOLDER("folder"),
        GROUPS("groups"),
        ITEM("item"),
        LINK("link"),
        LIST("list"),
        RAW("raw"),
        PAGE("page"),
        PKG("pkg"),
        OIDC_LOGIN("oidc-login"),
        OIDC_CALLBACK("oidc-callback");

        final String endpoint;

        StorageItemContext(String name) {
            this.endpoint = name;
        }

        static StorageAction.StorageItemContext fromEndpoint(final String endpoint) {
            return Arrays.stream(StorageAction.StorageItemContext.values())
                         .filter(storageItemContext -> storageItemContext.endpoint.equalsIgnoreCase(endpoint))
                         .findFirst()
                         .orElseThrow(NoSuchElementException::new);
        }
    }
}
