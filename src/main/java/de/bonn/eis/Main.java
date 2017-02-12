package de.bonn.eis;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

@Path("/hello")
public class Main {

    public static void main(String[] args) {
//        try {
//            new QuestionGenerator().generate();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        new de.bonn.eis.NIF().nifRun();
//        de.bonn.eis.ARQClient.getResourceTypes();
    }

    @Path("/hi")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public static String getMessage() {
//        try {
//            new QuestionGenerator().generate();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        return "Hello World";
    }
}
