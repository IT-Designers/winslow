package de.itdesigners.winslow.web.webdav;

import io.milton.annotations.ContentLength;
import io.milton.annotations.Get;
import io.milton.annotations.ModifiedDate;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.Date;

public class WebDavFile extends WebDavEntry {

    public WebDavFile(
            @Nonnull Path root,
            @Nonnull Path path) {
        super(root, path);
    }

    @ModifiedDate
    public Date getModifiedDate() {
        return new Date(getFullPath().toFile().lastModified());
    }

    @ContentLength
    public long getContentLength() {
        return getFullPath().toFile().length();
    }

    @Get
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(getFullPath().toFile());
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(getFullPath().toFile());
    }
}
