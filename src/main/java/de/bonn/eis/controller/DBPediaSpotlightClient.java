package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaSpotlightPOJO;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

/**
 * Created by Ainuddin Faizan on 12/29/16.
 */
public class DBPediaSpotlightClient {
    private final static String API_URL = "http://spotlight.sztaki.hu:2222/"; // TODO Update spotlight api URL
    private final static String LOCAL_API_URL = "http://spotlight/";
    private static final double CONFIDENCE = 0.6;
    private static final int SUPPORT = 0;
    private WebTarget webTarget;

    @PostConstruct
    protected void init(ServletContext servletContext) {
        Client client = ClientBuilder.newClient();
        String attr = servletContext.getInitParameter("env");
        String hostIp = API_URL;
        if (attr != null && attr.equalsIgnoreCase("prod")) {
            hostIp = LOCAL_API_URL;
        }
        webTarget = client.target(hostIp + "rest/annotate/");
    }

    /**
     * Annotate small piece of text via GET request
     *
     * @param text
     */
    public DBPediaSpotlightPOJO annotateGet(String text) {
        return webTarget
                .queryParam("confidence", CONFIDENCE)
                .queryParam("text", text)
                .request(MediaType.APPLICATION_JSON)
                .get(DBPediaSpotlightPOJO.class);
    }

    /**
     * Annotate large piece of text via POST request
     *
     * @param text Text to be annotated
     */
    public DBPediaSpotlightPOJO annotatePost(String text) {
        Form form = new Form();
        form.param("confidence", String.valueOf(CONFIDENCE));
        form.param("text", text);
        return webTarget.
                request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), DBPediaSpotlightPOJO.class);
    }
}
