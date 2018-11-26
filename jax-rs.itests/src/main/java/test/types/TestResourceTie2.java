package test.types;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("tie/a/b")
public class TestResourceTie2 {

    @GET
    public String getB() {
        return "tie/a/b 2";
    }

}
