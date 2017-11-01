package de.bonn.eis.controller;

import de.bonn.eis.model.NLP;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Created by andy on 11/2/17.
 */
class NLPServiceClient {
    private static final String DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_SLIDE = "dbpediaSpotlightConfidenceForSlide";
    private static final String DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_DECK = "dbpediaSpotlightConfidenceForDeck";
    private static final double SPOTLIGHT_CONFIDENCE_FOR_SLIDE_VALUE = 0.6;
    private static final double SPOTLIGHT_CONFIDENCE_FOR_DECK_VALUE = 0.6;

    static NLP getNlp(String deckID) {
        Client client = ClientBuilder.newClient();
        String hostIp = "https://nlpstore.experimental.slidewiki.org/nlp/" + deckID;
        WebTarget webTarget = client.target(hostIp);
        NLP nlp = webTarget
                .request(MediaType.APPLICATION_JSON)
                .get(NLP.class);
        if (nlp == null) {
            hostIp = "https://nlpservice.experimental.slidewiki.org/nlp/nlpForDeck/" + deckID;
            webTarget = client.target(hostIp);
            nlp = webTarget
                    .queryParam(DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_SLIDE, SPOTLIGHT_CONFIDENCE_FOR_SLIDE_VALUE)
                    .queryParam(DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_DECK, SPOTLIGHT_CONFIDENCE_FOR_DECK_VALUE)
                    .request(MediaType.APPLICATION_JSON)
                    .get(NLP.class);
        }
        return nlp;
    }
}
