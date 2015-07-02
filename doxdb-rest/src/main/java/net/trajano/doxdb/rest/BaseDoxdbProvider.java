package net.trajano.doxdb.rest;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class is extended by clients to provide a list of objects that are
 * allowed and allowed OOB references along with their schema.
 * <p>
 * Registration is done through a list of schemas provided by the
 * {@link #getRegisteredSchemaResources()} method. The DOXDB table that gets
 * created would be based on the the "$doxdb" object that is required in the
 * schema. In the future an alternate version of this provider will allow
 * passing in the contents of "$doxdb" only with a reference to a schema.
 * 
 * @author Archimedes
 */
public abstract class BaseDoxdbProvider {

    protected abstract String[] getRegisteredSchemaResources();

    @PostConstruct
    public void init() {

        System.out.println("schemas=" + getRegisteredSchemaResources());
    }

    @Path("{collection}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("collection") String collection,
        String content) {

        System.out.println("collection=" + collection);
        System.out.println("content=" + content);
        return Response.ok().entity(content).build();
    }

}
