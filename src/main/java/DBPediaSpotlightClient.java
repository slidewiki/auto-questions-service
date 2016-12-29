import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Created by andy on 12/29/16.
 */
public class DBPediaSpotlightClient {
    private Client client;
    private WebTarget webTarget;
    private final static String API_URL = "http://spotlight.sztaki.hu:2222/";
    private static final double CONFIDENCE = 0.0;
    private static final int SUPPORT = 0;

    @PostConstruct
    protected void init(){
        client = ClientBuilder.newClient();
        webTarget = client.target(API_URL + "rest/annotate/")
                .queryParam("confidence", CONFIDENCE);
    }

    public void getResponse(String text) {
        DBPediaSpotlightPOJO response = webTarget.queryParam("text", text)
                .request(MediaType.APPLICATION_JSON)
                .get(DBPediaSpotlightPOJO.class);
        System.out.println(response.toString());
    }
}
