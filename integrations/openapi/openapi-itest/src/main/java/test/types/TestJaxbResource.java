package test.types;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;

public class TestJaxbResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/operation")
    @Operation(description = "operation")
    public JaxbModel getJsonObject() {
        return new JaxbModel() {{foo = "Hello!";}};
    }

}
