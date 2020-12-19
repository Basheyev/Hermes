package com.axiom.hermes.common.interceptors;

import com.axiom.hermes.common.exceptions.HermesException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.sql.Timestamp;

/**
 * Формирует ответ клиенту на основе перехваченного исключения HermesException
 */
@Provider
public class ExceptionInterceptor implements ExceptionMapper<HermesException> {

    @Override
    public Response toResponse(HermesException exception) {
        return Response
                .status(exception.getStatus())
                .entity(exception.getPrettyJSON())
                .build();
    }

}
