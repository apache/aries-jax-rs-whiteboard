package test.types;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("tie")
public class TestResourceTie {

    @Path("{a}")
    public Object getA(@PathParam("a") String a) {
        return new ResourceA();
    }

    public static class ResourceA {

        @GET
        @Path("{b}")
        public String getB(@PathParam("b") String b) {
            return "tie/a/b 1";
        }

    }

}
