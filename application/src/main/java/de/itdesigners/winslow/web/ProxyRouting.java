package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.auth.User;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

@Service
@RestController
public class ProxyRouting {
    public static final Logger LOG            = Logger.getLogger(Executor.class.getSimpleName());
    public static final String REQUEST_PREFIX = "/proxied/";

    private Map<Path, Route> mapping = new HashMap<>();

    public ProxyRouting() {
        this.addRoute(
                Path.of("tensorboard", "project-sec-1"),
                new Route("http://192.168.1.178:8888/proxied/tensorboard/project-sec-1/", (u) -> true)
        );
    }

    @Nonnull
    public String addRoute(@Nonnull Path path, @Nonnull Route uri) {
        mapping.put(path, uri);
        return getPublicLocation(path.toString());
    }


    @RequestMapping(REQUEST_PREFIX + "**")
    public ResponseEntity<Object> proxy(
            User user,
            ProxyExchange<Object> proxy,
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) Object body) throws Exception {
        var normalized = FilesController.normalizedPath(request).orElse(Path.of(""));

        if (normalized.getNameCount() == 0) {
            return null;
        }

        var search = (Path) null;
        var route  = (Route) null;

        for (var segment : normalized) {
            if (search == null) {
                search = segment;
            } else {
                search = search.resolve(segment);
            }
            route = mapping.get(search);
            if (route != null) {
                break;
            }
        }

        if (route == null || !route.allowedToAccess.test(user)) {
            return null;
        }

        var location = route.uri;

        if (!location.endsWith("/")) {
            location += "/";
        }

        var uri         = location + search.relativize(normalized);
        var mappedProxy = proxy.uri(uri).body(body);

        switch (method) {
            case GET:
                return mappedProxy.get();
            case HEAD:
                return mappedProxy.head();
            case POST:
                return mappedProxy.post();
            case PUT:
                return mappedProxy.put();
            case PATCH:
                return mappedProxy.patch();
            case DELETE:
                return mappedProxy.delete();
            case OPTIONS:
                return mappedProxy.options();
            default:
            case TRACE:
                return null;
        }
    }

    @Nonnull
    public static String getPublicLocation(@Nonnull String path) {
        if (path.startsWith("/") && REQUEST_PREFIX.endsWith("/")) {
            return REQUEST_PREFIX + path.substring(1);
        } else {
            return REQUEST_PREFIX + path;
        }
    }

    public static class Route {
        public final String          uri;
        public final Predicate<User> allowedToAccess;

        public Route(String uri, Predicate<User> allowedToAccess) {
            this.uri             = uri;
            this.allowedToAccess = allowedToAccess;
        }
    }
}
