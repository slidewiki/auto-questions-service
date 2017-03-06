package de.bonn.eis.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import de.bonn.eis.model.*;
import de.bonn.eis.utils.NLPConsts;
import de.bonn.eis.utils.QGenLogger;
import de.bonn.eis.utils.QGenUtils;
import rita.RiTa;
import rita.RiWordNet;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */

@Path("/qgen")
public class QuestionGenerator {

    private static final String BLANK = "________";

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(SlideContent content, @Context ServletContext servletContext) throws FileNotFoundException, UnsupportedEncodingException {

        List<Question> questions = new ArrayList<>();
        String text = content.getText();
        String env = servletContext.getInitParameter("env");
        boolean envIsDev = env == null || !env.equalsIgnoreCase("prod");

        String dir = System.getProperty("user.dir");
        RiWordNet wordnet = new RiWordNet(dir + "/wordnet/");

        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        if (dbPediaResources == null || dbPediaResources.size() == 0) {
            return Response.noContent().build();
        }
        if (envIsDev) {
            QGenLogger.info("Resources retrieved");
            dbPediaResources.forEach(resource -> QGenLogger.info(resource.getSurfaceForm()));
        }
        // Selecting most relevant occurring words
        List<DBPediaResource> topResources = retriever.getMostRelevantWords(NLPConsts.WORDS_COUNT);
        if (envIsDev) {
            QGenLogger.info("Relevant resources");
            topResources.forEach(resource -> QGenLogger.info(resource.getSurfaceForm()));
        }
        LanguageProcessor processor = new LanguageProcessor(text);
        List<String> sentences = processor.getSentences();

        //TODO Efficiency?
        //TODO Create distractor cache for resources with same types or create some scheme

        ImmutableListMultimap<String, DBPediaResource> mapOfGroupedResources = retriever.groupResourcesByType(topResources);
        ImmutableSet<String> types = mapOfGroupedResources.keySet();
        types.forEach(type -> {
            ImmutableList<DBPediaResource> groupedResources = mapOfGroupedResources.get(type);
            if (envIsDev)
                QGenLogger.info(type);
            if (type.isEmpty()) {
                for (DBPediaResource resource : groupedResources) {
                    String surfaceForm = resource.getSurfaceForm();
                    String plural = RiTa.pluralize(surfaceForm);
                    if (envIsDev) {
                        QGenLogger.info(surfaceForm);
                    }
                    List<String> externalDistractors = retriever.getExternalDistractors(resource);
                    if (externalDistractors == null) {
                        externalDistractors = attemptToGetSynonyms(wordnet, resource.getSurfaceForm());
                    }
                    questions.addAll(getQuestionsForResource(sentences, surfaceForm, plural, externalDistractors, new ArrayList<>()));
                }
            } else {
                DBPediaResource firstResource = groupedResources.get(0);
                List<String> externalDistractors = retriever.getExternalDistractors(firstResource);
                for (DBPediaResource resource : groupedResources) {
                    String surfaceForm = resource.getSurfaceForm();
                    String plural = RiTa.pluralize(surfaceForm);
                    if (envIsDev) {
                        QGenLogger.info(surfaceForm);
                    }
                    if (externalDistractors == null) {
                        externalDistractors = attemptToGetSynonyms(wordnet, resource.getSurfaceForm());
                    }
                    List<String> inTextDistractors = groupedResources.stream().filter(res -> !res.equals(resource))
                            .map(res -> surfaceForm).collect(Collectors.toList());
                    questions.addAll(getQuestionsForResource(sentences, surfaceForm, plural, externalDistractors, inTextDistractors));
                }
            }
        });
        return Response.status(200).entity(questions).build();
    }

    private List<String> attemptToGetSynonyms(RiWordNet wordnet, String surfaceForm) {
        List<String> synList;
        String[] synonyms = wordnet.getAllSynonyms(surfaceForm, RiWordNet.NOUN);
        synList = Arrays.asList(synonyms);
        return synList;
    }

    private List<Question> getQuestionsForResource(List<String> sentences, String resourceName, String pluralResourceName, List<String> externalDistractors, List<String> inTextDistractors) {
        List<Question> questions = new ArrayList<>();

        sentences.forEach(s -> {
            if (QGenUtils.sourceHasWord(s, resourceName)) {
                Question.QuestionBuilder builder = Question.builder();
                String questionText = s.replaceAll("\\b" + resourceName + "\\b", BLANK);
                String answer = resourceName;
                if (QGenUtils.sourceHasWord(s, pluralResourceName)) {
                    questionText = questionText.replaceAll("\\b" + pluralResourceName + "\\b", BLANK);
                    answer += "(s)";
                }
                builder.questionText(questionText).
                        answer(answer).
                        externalDistractors(externalDistractors).
                        inTextDistractors(inTextDistractors);
                questions.add(builder.build());
            }
        });
        return questions;
    }

    @Path("/numbers")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateQuestionsForValues(SlideContent slideContent) {
        String text = slideContent.getText();
        LanguageProcessor processor = new LanguageProcessor(text);
        List<Question> questions = new ArrayList<>();
        Map<String, List<String>> sentencesWithNumbers = processor.getCardinals();
        Set<String> numbers = sentencesWithNumbers.keySet();
        sentencesWithNumbers.forEach((numberString, sentences) -> sentences.forEach(sentence -> {
            String questionText = sentence.replaceAll("\\b" + numberString + "\\b", BLANK);
            Question.QuestionBuilder builder = Question.builder();
            builder.questionText(questionText).
                    answer(numberString).
                    inTextDistractors(numbers.stream().
                            filter(num -> !num.equalsIgnoreCase(numberString)).collect(Collectors.toList()));
            questions.add(builder.build());
        }));
        return Response.status(200).entity(questions).build();
    }

    @Path("/slides/{deckID}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateQuestionsForSlides(@PathParam("deckID") String deckID) {
        Client client = ClientBuilder.newClient();
        String hostIp = "https://deckservice.experimental.slidewiki.org/deck/" + deckID + "/slides";
        WebTarget webTarget = client.target(hostIp);
        Deck deck = webTarget
                .request(MediaType.APPLICATION_JSON)
                .get(Deck.class);
        return Response.status(200).entity(deck).build();
    }
}