package org.opencadc.storage;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum StorageItemContext {
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

    static StorageItemContext fromEndpoint(final String endpoint) {
        return Arrays.stream(StorageItemContext.values())
                     .filter(storageItemContext -> storageItemContext.endpoint.equalsIgnoreCase(endpoint))
                     .findFirst()
                     .orElseThrow(NoSuchElementException::new);
    }
}
