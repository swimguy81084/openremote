package org.openremote.container.web;

import io.mikael.urlbuilder.UrlBuilder;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.HttpString;
import io.undertow.util.MimeMappings;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.container.ConfigurationException;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static javax.ws.rs.core.UriBuilder.fromUri;

public abstract class WebService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public static final String AUTH_PATH = "/auth";
    public static final String API_PATH = "/api";
    public static final String STATIC_PATH = "/static";

    public static final String WEB_SERVER_LISTEN_HOST = "WEB_SERVER_LISTEN_HOST";
    public static final String WEB_SERVER_LISTEN_HOST_DEFAULT = "localhost";
    public static final String WEB_SERVER_LISTEN_PORT = "WEB_SERVER_LISTEN_PORT";
    public static final int WEB_SERVER_LISTEN_PORT_DEFAULT = 8080;

    protected final Pattern PATTERN_STATIC = Pattern.compile(Pattern.quote(STATIC_PATH) + "(\\/.*)?");
    protected final Pattern PATTERN_REALM_ROOT = Pattern.compile("\\/([a-z]+)\\/?");
    protected final Pattern PATTERN_REALM_SUB = Pattern.compile("\\/([a-z]+)\\/(.*)");

    protected String host;
    protected int port;
    protected PathHandler pathHandler;
    protected Undertow undertow;
    protected Collection<Class<?>> apiClasses = new HashSet<>();
    protected Collection<Object> apiSingletons = new HashSet<>();

    @Override
    public void prepare(Container container) {
        host = container.getConfig(WEB_SERVER_LISTEN_HOST, WEB_SERVER_LISTEN_HOST_DEFAULT);
        port = container.getConfigInteger(WEB_SERVER_LISTEN_PORT, WEB_SERVER_LISTEN_PORT_DEFAULT);

        pathHandler = Handlers.path();

        undertow = prepare(
            container,
            Undertow.builder()
                .addHttpListener(port, host)
        ).build();
    }

    @Override
    public void start(Container container) {
        if (undertow != null) {
            LOG.info("Starting webserver on http://" + host + ":" + port);
            undertow.start();
        }
    }

    @Override
    public void stop(Container container) {
        if (undertow != null) {
            LOG.info("Stopping webserver...");
            undertow = null;
        }
    }

    protected Undertow.Builder prepare(Container container, Undertow.Builder builder) {

        Path docRoot = getStaticResourceDocRoot(container);
        HttpHandler staticResourceHandler = docRoot != null ? createStaticResourceHandler(container, docRoot) : null;

        HttpHandler apiHandler = createApiHandler(container);

        HttpHandler handler = exchange -> {
            String requestPath = exchange.getRequestPath();
            LOG.fine("Handling request: " + exchange.getRequestURL());

            // Redirect / to default realm
            if (requestPath.equals("/")) {
                LOG.fine("Handling root request, redirecting client to default realm: " + requestPath);
                new RedirectHandler(
                    fromUri(exchange.getRequestURL()).replacePath(getDefaultRealm()).build().toString()
                ).handleRequest(exchange);
                return;
            }

            if (staticResourceHandler != null) {
                // Serve /<realm>/index.html
                Matcher realmRootMatcher = PATTERN_REALM_ROOT.matcher(requestPath);
                if (realmRootMatcher.matches()) {
                    LOG.fine("Serving index document of realm: " + requestPath);
                    exchange.setRelativePath("/index.html");
                    staticResourceHandler.handleRequest(exchange);
                    return;
                }

                // Serve static resources with path /static/*
                Matcher staticMatcher = PATTERN_STATIC.matcher(requestPath);
                if (staticMatcher.matches()) {
                    LOG.fine("Serving static resource: " + requestPath);
                    String remaining = staticMatcher.group(1);
                    exchange.setRelativePath(remaining == null || remaining.length() == 0 ? "/" : remaining);
                    // Special handling for compression of pbf static files (they are already compressed...)
                    if (exchange.getRequestPath().endsWith(".pbf")) {
                        exchange.getResponseHeaders().put(new HttpString(CONTENT_ENCODING), "gzip");
                    }
                    staticResourceHandler.handleRequest(exchange);
                    return;
                }
            }

            // Serve API with path /<realm>/*
            Matcher realmSubMatcher = PATTERN_REALM_SUB.matcher(requestPath);
            if (apiHandler != null && realmSubMatcher.matches()) {
                LOG.fine("Serving API call: " + requestPath);
                String realm = realmSubMatcher.group(1);

                // Move the realm from path segment to query parameter
                URI apiUrl = fromUri(exchange.getRequestURL())
                    .replacePath(API_PATH).path(realmSubMatcher.group(2))
                    .replaceQuery(exchange.getQueryString()).queryParam("realm", realm)
                    .build();

                exchange.setRequestURI(apiUrl.toString(), true);
                exchange.setRequestPath(apiUrl.getPath());
                exchange.setRelativePath(apiUrl.getPath());

                // This is probably the only actual source of query details for Resteasy
                exchange.setQueryString(apiUrl.getRawQuery());

                // Just to make it look nice
                exchange.addQueryParam("realm", realm);

                apiHandler.handleRequest(exchange);
                return;
            }

            LOG.fine("No resource found: " + requestPath);
            ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
        };

        builder.setHandler(handler);

        return builder;
    }

    protected HttpHandler createStaticResourceHandler(Container container, Path docRoot) {
        if (!Files.isDirectory(docRoot))
            throw new ConfigurationException("Missing document root directory: " + docRoot.toAbsolutePath());
        LOG.info("Static document root directory: " + docRoot.toAbsolutePath());
        ResourceManager staticResourcesManager = new FileResourceManager(docRoot.toFile(), 0, true, false);

        MimeMappings.Builder mimeBuilder = MimeMappings.builder(true);
        mimeBuilder.addMapping("pbf", "application/x-protobuf");
        mimeBuilder.addMapping("wsdl", "application/xml");
        mimeBuilder.addMapping("xsl", "text/xsl");
        // TODO: Add more mime/magic stuff?

        return Handlers.resource(staticResourcesManager)
            .setMimeMappings(mimeBuilder.build());
    }

    protected HttpHandler createApiHandler(Container container) {
        Application restApplication = getRestApplication(container);
        if (restApplication == null)
            return null;

        LOG.fine("Registering REST application: " + restApplication.getClass().getName());

        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        resteasyDeployment.setApplication(restApplication);

        ServletInfo restServlet = Servlets.servlet("RESTEasy Servlet", HttpServlet30Dispatcher.class)
            .setAsyncSupported(true)
            .setLoadOnStartup(1)
            .addMapping("/*");

        DeploymentInfo deploymentInfo = new DeploymentInfo()
            .setContextPath(API_PATH)
            .addServletContextAttribute(ResteasyDeployment.class.getName(), resteasyDeployment)
            .addServlet(restServlet).setDeploymentName("RESTEasy Deployment")
            .setClassLoader(restApplication.getClass().getClassLoader());

        try {
            DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
            manager.deploy();
            return manager.start();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Override this method to serve static resources from file system on the /static/* path.
     */
    protected Path getStaticResourceDocRoot(Container container) {
        return null;
    }

    /**
     * Override this to register a JAX-RS application served on the /api/* path.
     */
    protected WebApplication getRestApplication(Container container) {
        if (getApiClasses()!= null || getApiSingletons() != null) {
            return new WebApplication(container, getApiClasses(), getApiSingletons());
        }
        return null;
    }

    public Collection<Class<?>> getApiClasses() {
        return apiClasses;
    }

    public Collection<Object> getApiSingletons() {
        return apiSingletons;
    }

    protected abstract String getDefaultRealm();

}