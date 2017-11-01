package de.bonn.eis.controller;

import de.bonn.eis.model.*;
import de.bonn.eis.utils.QGenUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */

@Path("/qgen")
public class QGenApplication {

    private static final String DBPEDIA_PERSON = "DBpedia:Person";
    private static final String GAP_FILL = "gap-fill";
    private static final String SELECT = "select";
    private static final String WHO_AM_I = "whoami";
    private static final int NO_OF_FREQUENT_RESOURCES = 5;
    @Context
    private ServletContext servletContext;

    @Path("/{type}/{level}/{deckID}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateQuestionsForSlides(@PathParam("type") String type, @PathParam("level") String level, @PathParam("deckID") String deckID) {
        NLP nlp = NLPServiceClient.getNlp(deckID);
        if (nlp != null) {
            DBPediaSpotlightResult spotlightResults = nlp.getDBPediaSpotlight();
            if (spotlightResults != null) {
                List<DBPediaResource> resources = spotlightResults.getDBPediaResources();
                if (resources != null && !resources.isEmpty()) {
                    resources = QGenUtils.removeDuplicatesFromResourceList(resources);
                    List<Frequency> frequencies = nlp.getDBPediaSpotlightURIFrequencies();
                    List<DBPediaResource> temp = new ArrayList<>();
                    for (Frequency frequency : frequencies) {
                        for (DBPediaResource resource : resources) {
                            if (resource.getURI().equals(frequency.getEntry())) {
                                if (type.equals(WHO_AM_I)) {
                                    if (resource.getTypes().contains(DBPEDIA_PERSON)) {
                                        temp.add(resource);
                                    }
                                } else {
                                    temp.add(resource);
                                }
                            }
                        }
                        if (temp.size() == NO_OF_FREQUENT_RESOURCES) {
                            break;
                        }
                    }
                    resources = temp;
                    if (type.equals(GAP_FILL)) {
                        List<GapFillDistractor> distractors = QuestionAndDistractorGenerator.getGapFillDistractors(servletContext, resources, level);
                        List<QuestionSetPerSlide> gapFillQuestionSets = QuestionAndDistractorGenerator.getGapFillQuestions(null, nlp.getChildren(), distractors);
                        if (gapFillQuestionSets != null) {
                            return Response.status(200).entity(gapFillQuestionSets).build();
                        }
                    }
                    if (type.equals(SELECT)) {
                        List<MCQQuestion> selectQuestions = QuestionAndDistractorGenerator.getSelectQuestions(resources, level);
                        if (selectQuestions != null) {
                            return Response.status(200).entity(selectQuestions).build();
                        }
                    }
                    if (type.equals(WHO_AM_I)) {
                        List<MCQQuestion> questions = QuestionAndDistractorGenerator.getWhoamIQuestions(resources, level);
                        if (questions != null) {
                            return Response.status(200).entity(questions).build();
                        }
                    }
                }
            }
        }
        return Response.noContent().build();
    }

    @Path("/{type}/{level}/text")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateQuestionsForText(@PathParam("type") String type, @PathParam("level") String level, String text) throws FileNotFoundException, UnsupportedEncodingException {
        TextInfoRetriever retriever;
        List<DBPediaResource> resources = new ArrayList<>();
        if (type.equals(WHO_AM_I)) {
            retriever = new TextInfoRetriever(text, DBPEDIA_PERSON, servletContext);
        } else {
            retriever = new TextInfoRetriever(text, servletContext);
        }
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        if (dbPediaResources != null && !dbPediaResources.isEmpty()) {
            resources.addAll(retriever.getFrequentWords(NO_OF_FREQUENT_RESOURCES).keySet());
            if (type.equals(GAP_FILL)) {
                List<GapFillDistractor> distractors = QuestionAndDistractorGenerator.getGapFillDistractors(servletContext, resources, level);
                List<QuestionSetPerSlide> gapFillQuestionSets = QuestionAndDistractorGenerator.getGapFillQuestions(text, null, distractors);
                if (gapFillQuestionSets != null) {
                    return Response.status(200).entity(gapFillQuestionSets).build();
                }
            }
            if (type.equals(SELECT)) {
                List<MCQQuestion> selectQuestions = QuestionAndDistractorGenerator.getSelectQuestions(resources, level);
                if (selectQuestions != null) {
                    return Response.status(200).entity(selectQuestions).build();
                }
            }
            if (type.equals(WHO_AM_I)) {
                List<MCQQuestion> questions = QuestionAndDistractorGenerator.getWhoamIQuestions(resources, level);
                if (questions != null) {
                    return Response.status(200).entity(questions).build();
                }
            }
        }
        return Response.noContent().build();
    }
}