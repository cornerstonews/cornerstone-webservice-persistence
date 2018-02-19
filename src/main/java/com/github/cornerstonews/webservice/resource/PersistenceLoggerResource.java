package com.github.cornerstonews.webservice.resource;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.github.cornerstonews.webservice.exception.InputValidationException;

//@Path("persistenceLogger")
public class PersistenceLoggerResource {

    private EntityManagerFactory emf;

    public PersistenceLoggerResource(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    @PUT
    @Path("{level}")
    public Response setPersistenceLogLevel(@PathParam("level") String level) {
        Map<String, Object> props = emf.getProperties();
        props.put("eclipselink.logging.level", getValidLogLevel(level));
        // TODO: Figure out how to replace already created emf so all new requests uses that.
        return Response.ok().build();
    }
    
    @PUT
    @Path("sql/{level}")
    public Response setPersistenceSqlLogLevel(@PathParam("level") String level, @QueryParam("logParameters") Boolean logParameters) {
        Map<String, Object> props = emf.getProperties();
        props.put("eclipselink.logging.level.sql", getValidLogLevel(level));
        if(logParameters) {
            props.put("eclipselink.logging.parameters", true);
        }
        
        // TODO: Figure out how to replace already created emf so all new requests uses that.
        return Response.ok().build();
    }
    
    private String getValidLogLevel(String level) {
        switch (level.toUpperCase()) {
            case "OFF":
            case "SEVERE":
            case "WARNING":
            case "INFO":
            case "CONFIG":
            case "FINE":
            case "FINER":
            case "FINEST":
            case "ALL":
                return level.toUpperCase();
            default:
                String error = "Invalid Log level provided. Valid log levels are: "
                            + "OFF SEVERE WARNING INFO CONFIG FINE FINER FINEST ALL";
                throw new InputValidationException(error);
        }
    }
}
