package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.web.api.FilesController;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriUtils;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RestController
public class ProxyRouting {
    public static final Logger LOG            = Logger.getLogger(Executor.class.getSimpleName());
    public static final String REQUEST_PREFIX = "/proxied/";

    private final Map<Path, Route> mapping = new HashMap<>();

    public ProxyRouting() {
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
            return new ResponseEntity<>(HttpStatus.OK);
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
            return new ResponseEntity<>(HttpStatus.OK);
        }

        var location = route.uri;

        if (!location.endsWith("/")) {
            location += "/";
        }

        var uri = location + search.relativize(normalized) + "?" + request
                .getParameterMap()
                .entrySet()
                .stream()
                .flatMap(entry -> Stream
                        .of(entry.getValue())
                        .map(v -> UriUtils.encodeQueryParam(
                                entry.getKey(),
                                StandardCharsets.UTF_8
                        ) + "=" + UriUtils.encodeQueryParam(
                                v,
                                StandardCharsets.UTF_8
                        )))
                .collect(Collectors.joining("&"));

        var mappedProxy = proxy.uri(uri).body(body);

        try {
            return switch (method) {
                case GET -> mappedProxy.get();
                case HEAD -> mappedProxy.head();
                case POST -> mappedProxy.post();
                case PUT -> mappedProxy.put();
                case PATCH -> mappedProxy.patch();
                case DELETE -> mappedProxy.delete();
                case OPTIONS -> mappedProxy.options();
                default -> new ResponseEntity<>(HttpStatus.OK);
            };
        } catch (ResourceAccessException rae) {
            LOG.log(Level.FINE, "Unable to retrieve data from proxy destination", rae);
            return new ResponseEntity<>(
                    """
                            <html>
                                <body>
                                    <h2>Not Ready Yet?</h2>
                                    <i>Will try again in a moment ...</i>
                                    <meta http-equiv="refresh" content="1">
                               </body>
                           </html>
                        """,
                    HttpStatus.TOO_EARLY
            );
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
