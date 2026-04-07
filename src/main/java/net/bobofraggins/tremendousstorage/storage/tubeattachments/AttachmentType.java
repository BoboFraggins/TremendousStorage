package net.bobofraggins.tremendousstorage.storage.tubeattachments;

/** The type of attachment installed on a tube face. */
public enum AttachmentType {
    NONE,
    STORAGE_INTERFACE,
    IMPORT_INTERFACE,
    EXPORT_INTERFACE;

    public static AttachmentType fromOrdinal(int o) {
        AttachmentType[] values = values();
        if (o < 0 || o >= values.length) return NONE;
        return values[o];
    }

    public boolean hasAttachment() {
        return this != NONE;
    }
}
