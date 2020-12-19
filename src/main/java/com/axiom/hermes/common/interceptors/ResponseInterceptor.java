package com.axiom.hermes.common.interceptors;

import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class ResponseInterceptor implements ContainerResponseFilter {
/*
    private static final Logger LOG = Logger.getLogger(ResponseInterceptor.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;
*/
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        /*final int status = responseContext.getStatus();
        final String path = info.getPath();
        final String address = request.remoteAddress().toString();
        LOG.info("Response from " + path + " status " + status  +" to IP " + address);*/
    }
}
