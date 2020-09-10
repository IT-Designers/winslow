package de.itdesigners.winslow.web.webdav;

import io.milton.annotations.*;

import java.util.Date;

public class WebDavFile {
    private String name;
    private Date   createdDate;
    private Date   modifiedDate;
    private Long   contentLength;
    private byte[] bytes;

    public WebDavFile(String name, byte[] bytes) {
        this.name = name;
        this.bytes = bytes;
        this.createdDate = new Date();
        this.modifiedDate= new Date();
        this.contentLength = (long) bytes.length;
    }

    @Name
    public String getName() {
        return name;
    }

    @UniqueId
    public String getUniqueId() {
        return name;
    }

    @ModifiedDate
    public Date getModifiedDate() {
        return modifiedDate;
    }

    @CreatedDate
    public Date getCreatedDate() {
        return createdDate;
    }

    @ContentLength
    public Long getContentLength() {
        return (long) bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
