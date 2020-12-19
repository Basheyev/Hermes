package com.axiom.hermes.common.interceptors;

import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class RequestInterceptor implements ContainerRequestFilter {
/*
    private static final Logger LOG = Logger.getLogger(RequestInterceptor.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;*/

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

       /* final String method = requestContext.getMethod();
        final String path = info.getPath();
        final String address = request.remoteAddress().toString();

        LOG.infof("Request %s %s from IP %s", method, path, address);*/
    }

}
