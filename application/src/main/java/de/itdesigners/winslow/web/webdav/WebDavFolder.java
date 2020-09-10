package de.itdesigners.winslow.web.webdav;

import io.milton.annotations.ChildrenOf;
import io.milton.annotations.CreatedDate;
import io.milton.annotations.Name;
import io.milton.annotations.UniqueId;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class WebDavFolder {
    private String name;
    private Date createdDate;

    public WebDavFolder(String name) {
        this.name = name;
    }

    @Name
    public String getName() {
        return name;
    }

    @UniqueId
    public String getUniqueId() {
        return name;
    }

    @CreatedDate
    public Date getCreatedDate() {
        return createdDate;
    }

    public Collection<WebDavFile> listFiles() {
        return Collections.emptyList();
    }

    public Collection<WebDavFolder> listFolders() {
        return Collections.singletonList(new WebDavFolder("berndos"));
    }
}
