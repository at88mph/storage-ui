package org.opencadc.storage;

import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.util.StringUtil;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import org.opencadc.storage.config.VOSpaceServiceConfigManager;


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
    private final VOSpaceServiceConfigManager voSpaceServiceConfigManager;

    public RequestPathParser(String requestPath, VOSpaceServiceConfigManager voSpaceServiceConfigManager) {
        if (!StringUtil.hasText(requestPath) || Path.of(requestPath).getNameCount() == 0) {
            throw new IllegalStateException("Input requestPath cannot be null");
        } else if (voSpaceServiceConfigManager == null) {
            throw new IllegalStateException("VOSpaceServiceConfigManager instance is required.");
        }

        try {
            this.requestPath = Path.of(requestPath);
        } catch (InvalidPathException invalidPathException) {
            throw new IllegalStateException(invalidPathException.getMessage(), invalidPathException);
        }

        this.voSpaceServiceConfigManager = voSpaceServiceConfigManager;
    }


    String getServiceName() {
        return getRealPath().getName(1).toString();
    }

    StorageItemContext getContextVerb() {
        return StorageItemContext.fromEndpoint(getRealPath().getName(2).toString());
    }

    Path getContextPath() {
        return getRealPath().getName(0);
    }

    /**
     * Decide if the current path is incomplete, and provide a redirect.
     * @return  Path to redirect to, if necessary.
     */
    Path getRealPath() {
        final int pathElementCount = requestPath.getNameCount();
        final String defaultServiceName = this.voSpaceServiceConfigManager.getDefaultServiceName();
        final Path realPath;

        if (pathElementCount == 2) {
            realPath = Path.of("/" + requestPath.getName(0), defaultServiceName, requestPath.getName(1).toString());
        } else if (pathElementCount == 1) {
            realPath = Path.of("/" + requestPath.getName(0), defaultServiceName, RequestPathParser.DEFAULT_CONTEXT_VERB.endpoint);
        } else {
            realPath = ensureCurrentContext();
        }

        return realPath;
    }

    private Path ensureCurrentContext() {
        // The first three items should consist of the servlet context, current service, and context verb.
        final Path contextualPath = this.requestPath.subpath(0, 3);
        final String serviceNameOrContextVerb = contextualPath.getName(1).toString();

        try {
            final StorageItemContext contextVerb = StorageItemContext.fromEndpoint(serviceNameOrContextVerb);

            // It's a verb, so inject the default service.
            return Path.of("/" + contextualPath.getName(0), this.voSpaceServiceConfigManager.getDefaultServiceName(), contextVerb.endpoint,
                           this.requestPath.subpath(2, this.requestPath.getNameCount()).toString());
        } catch (NoSuchElementException notAVerb) {
            // Assume the path is already setup in the format /<servlet context>/<service name>/<context verb>, so check the service name.
            try {
                this.voSpaceServiceConfigManager.getServiceConfig(serviceNameOrContextVerb);
            } catch (ResourceNotFoundException resourceNotFoundException) {
                throw new IllegalArgumentException("No such service: " + serviceNameOrContextVerb);
            }
        }

        return this.requestPath;
    }
}
