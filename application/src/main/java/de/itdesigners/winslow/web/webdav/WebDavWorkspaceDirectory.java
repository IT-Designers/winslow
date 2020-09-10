package de.itdesigners.winslow.web.webdav;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class WebDavWorkspaceDirectory extends WebDavDirectory {

    private final Function<String, Optional<String>> nameResolver;

    public WebDavWorkspaceDirectory(
            @Nonnull Path root,
            @Nonnull Path path,
            Function<String, Optional<String>> nameResolver) {
        super(root, path);
        this.nameResolver = nameResolver;
    }

    @Override
    public Collection<WebDavDirectory> listFolders() {
        var list = new ArrayList<>(super.listFolders());
        for (int i = 0; i < list.size(); ++i) {
            var entry = list.get(i);
            var name  = nameResolver.apply(entry.getName());
            var index = i;
            name.ifPresent(display -> {
                list.set(index, new WebDavDirectory(entry.getRoot(), entry.getPath(), display));
            });
        }
        return list;
    }
}
