package net.canfar.storage.web.view;

import net.canfar.storage.FileSizeRepresentation;
import org.opencadc.gms.GroupURI;
import org.opencadc.vospace.VOSURI;

import java.nio.file.Path;
import java.util.Date;


/**
 * Representation of a Link in VOSpace.
 */
public class LinkItem extends StorageItem {
    public LinkItem(VOSURI uri, long sizeInBytes, Date lastModified,
                    boolean publicFlag, boolean lockedFlag,
                    GroupURI[] writeGroupURIs, GroupURI[] readGroupURIs,
                    String owner, boolean readableFlag, boolean writableFlag, Path targetPath) {
        super(uri, sizeInBytes, lastModified, publicFlag, lockedFlag,
              writeGroupURIs, readGroupURIs, owner, readableFlag, writableFlag, targetPath);
    }


    @Override
    public String getItemIconCSS() {
        return "glyphicon-link";
    }

    /**
     * Links have no size.
     *
     * @return String '--'.
     */
    @Override
    public String getSizeHumanReadable() {
        return FileSizeRepresentation.NO_SIZE;
    }
}
