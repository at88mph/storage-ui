package org.opencadc.storage.node;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.reg.Standards;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.opencadc.storage.config.VOSpaceServiceConfig;
import org.opencadc.vospace.VOS;
import org.opencadc.vospace.View;
import org.opencadc.vospace.client.ClientTransfer;
import org.opencadc.vospace.transfer.Direction;
import org.opencadc.vospace.transfer.Protocol;
import org.opencadc.vospace.transfer.Transfer;

public class PackageHandler extends StorageHandler {
    private static final AuthMethod[] PROTOCOL_AUTH_METHODS = new AuthMethod[] {AuthMethod.ANON, AuthMethod.CERT, AuthMethod.COOKIE};

    static final String ZIP_CONTENT_TYPE = "application/zip";
    static final String TAR_CONTENT_TYPE = "application/x-tar";


    public PackageHandler(VOSpaceServiceConfig currentService, Subject subject) {
        super(currentService, subject);
    }

    public String getDownloadEndpoint(final String contentType) {
        final List<Protocol> protocols = Arrays.stream(PackageHandler.PROTOCOL_AUTH_METHODS).map(authMethod -> {
            final Protocol httpsAuth = new Protocol(VOS.PROTOCOL_HTTPS_PUT);
            httpsAuth.setSecurityMethod(Standards.getSecurityMethod(authMethod));
            return httpsAuth;
        }).collect(Collectors.toList());

        final Transfer transfer = new Transfer(Direction.pullFromVoSpace);

        final View packageView = new View(Standards.PKG_10);
        packageView.getParameters().add(new View.Parameter(VOS.PROPERTY_URI_FORMAT, contentType));

        transfer.setView(packageView);
        transfer.getProtocols().addAll(protocols);
        transfer.version = VOS.VOSPACE_21;

        transfer.setView(packageView);
        Protocol protocol = new Protocol(VOS.PROTOCOL_HTTPS_GET);
        protocol.setSecurityMethod(Standards.SECURITY_METHOD_COOKIE);
        transfer.getProtocols().add(protocol);

        final ClientTransfer ct = this.currentService.getVOSpaceClient().createTransfer(transfer);
        return ct.getTransfer().getEndpoint();
    }
}
