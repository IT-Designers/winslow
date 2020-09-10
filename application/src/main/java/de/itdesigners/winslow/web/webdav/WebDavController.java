package de.itdesigners.winslow.web.webdav;

import io.milton.annotations.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

@ResourceController
public class WebDavController {

    private static Logger                      LOG         = Logger.getLogger(WebDavController.class.getSimpleName());
    private static HashMap<String, WebDavFile> FILES = new HashMap<>();

    static {
        byte[] bytes  = "Hello World".getBytes(StandardCharsets.UTF_8);
        FILES.put("file1.txt", new WebDavFile("file1.txt", bytes));
        FILES.put("file2.txt", new WebDavFile("file2.txt", bytes));
    }
    @Root
    public WebDavController getRoot() {
        return this;
    }

    @ChildrenOf
    public List<WebDavFolder> getWebDavFolders(WebDavController root) {
        List<WebDavFolder> webDavFolders = new ArrayList<>();
        webDavFolders.add(new WebDavFolder("webdav"));
        webDavFolders.add(new WebDavFolder("folder2"));
        return webDavFolders;
    }

    @ChildrenOf
    public Collection<WebDavFile> getWebDavFiles(WebDavFolder webDavFolder) {
        return FILES.values();
    }

    @ChildrenOf
    public Collection<WebDavFolder> getWebDavFolders(WebDavFolder webDavFolder) {
        return webDavFolder.listFolders();
    }

    @Get
    public InputStream getChild(WebDavFile webDavFile) {
        return new ByteArrayInputStream(FILES.get(webDavFile.getName()).getBytes());
    }

    @PutChild
    public void putChild(WebDavFile parent, String name, byte[] bytes) {
        if(name!=null) {
            FILES.put(name, new WebDavFile(name, bytes));
        } else {
            parent.setBytes(bytes);
            FILES.put(parent.getName(), parent);
        }
    }

    @Delete
    public void delete(WebDavFile webDavFile) {
        FILES.remove(webDavFile.getName());
    }

    @Name
    public String getWebDavFile(WebDavFile webDavFile) {
        return webDavFile.getName();
    }

    @DisplayName
    public String getDisplayName(WebDavFile webDavFile) {
        return webDavFile.getName();
    }

    @UniqueId
    public String getUniqueId(WebDavFile webDavFile) {
        return webDavFile.getName();
    }

    @ModifiedDate
    public Date getModifiedDate(WebDavFile webDavFile) {
        return webDavFile.getModifiedDate();
    }

    @CreatedDate
    public Date getCreatedDate(WebDavFile webDavFile) {
        return webDavFile.getCreatedDate();
    }
}
