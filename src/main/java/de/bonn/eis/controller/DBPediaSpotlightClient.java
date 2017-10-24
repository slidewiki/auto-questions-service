package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaSpotlightResult;

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
    private final static String API_URL = "http://model.dbpedia-spotlight.org/en/annotate";
    private final static String LOCAL_API_URL = "http://spotlight/rest/annotate/";
    private static final double CONFIDENCE = 0.6;
    private static final int SUPPORT = 0;
    private static final String CONFIDENCE_PARAM = "confidence";
    private static final String TEXT_PARAM = "text";
    private static final String TYPES_PARAM = "types";
    private WebTarget webTarget;

    @PostConstruct
    protected void init(ServletContext servletContext) {
        Client client = ClientBuilder.newClient();
        String attr = servletContext.getInitParameter("env");
        String hostIp = API_URL;
        if (attr != null && attr.equalsIgnoreCase("prod")) {
            hostIp = LOCAL_API_URL;
        }
        webTarget = client.target(hostIp);
    }

    /**
     * Annotate large piece of text via POST request
     *
     * @param text Text to be annotated
     */
    public DBPediaSpotlightResult annotatePost(String text) {
        Form form = new Form();
        form.param(CONFIDENCE_PARAM, String.valueOf(CONFIDENCE));
        form.param(TEXT_PARAM, text);
        return webTarget.
                request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), DBPediaSpotlightResult.class);
    }

    public DBPediaSpotlightResult annotatePost(String text, String filterType) {
        Form form = new Form();
        form.param(CONFIDENCE_PARAM, String.valueOf(CONFIDENCE));
        form.param(TEXT_PARAM, text);
        form.param(TYPES_PARAM, filterType);
        return webTarget.
                request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), DBPediaSpotlightResult.class);
    }
}
