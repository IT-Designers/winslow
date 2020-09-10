package de.itdesigners.winslow.web.webdav;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.project.Project;
import io.milton.annotations.Name;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.*;

public class WebDavRootFolder {

    private final @Nonnull Winslow winslow;
    private final @Nonnull String  exportName;

    private final @Nonnull Map<String, WebDavEntry> entries = new HashMap<>();

    public WebDavRootFolder(@Nonnull Winslow winslow, @Nonnull String exportName) {
        this.winslow    = winslow;
        this.exportName = exportName;

        var resources = winslow.getResourceManager();
        resources.getResourceDirectory().ifPresent(dir -> {
            var root  = dir.getParent();
            var path  = root.relativize(dir);
            var entry = new WebDavDirectory(root, path);
            entries.put(entry.getName(), entry);
        });

        resources.getWorkspacesDirectory().ifPresent(dir -> {
            var root = dir.getParent();
            var path = root.relativize(dir);
            var entry = new WebDavWorkspaceDirectory(root, path, name -> winslow
                    .getProjectRepository()
                    .getProject(name)
                    .unsafe()
                    .map(Project::getName)
            );
            entries.put(entry.getName(), entry);
        });
    }

    @Name
    @Nonnull
    public String getExportName() {
        return exportName;
    }

    @Nonnull
    public Winslow getWinslow() {
        return winslow;
    }

    public Optional<WebDavEntry> getFolder(@Nonnull String name) {
        return Optional.ofNullable(entries.get(name));
    }

    public Collection<WebDavEntry> listFolders() {
        return entries.values();
    }

    private void forResourceRoot(@Nonnull List<WebDavEntry> target, @Nonnull Path dir) {
        var root = dir.getParent();
        var path = root.relativize(dir);
        target.add(new WebDavDirectory(root, path));
    }
}
