package de.bonn.eis.controller;

import com.google.common.collect.ImmutableListMultimap;
import de.bonn.eis.model.*;
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
    private static final String SELECT_QUESTION_TEXT = " is a: ";
    private static final String DBPEDIA_PERSON = "DBpedia:Person";
    private static final String DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_SLIDE = "dbpediaSpotlightConfidenceForSlide";
    private static final String DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_DECK = "dbpediaSpotlightConfidenceForDeck";
    private static final double SPOTLIGHT_CONFIDENCE_FOR_SLIDE_VALUE = 0.6;
    private static final double SPOTLIGHT_CONFIDENCE_FOR_DECK_VALUE = 0.6;
    private static final String GAP_FILL = "gap-fill";
    private static final String SELECT = "select";
    private static final String WHOAMI = "whoami";
    @Context
    private ServletContext servletContext;

    @Path("/{type}/{level}/{deckID}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateQuestionsForSlides(@PathParam("type") String type, @PathParam("level") String level, @PathParam("deckID") String deckID) {
        Client client = ClientBuilder.newClient();
        String hostIp = "https://nlpstore.experimental.slidewiki.org/nlp/" + deckID;
        WebTarget webTarget = client.target(hostIp);
        NLP nlp = webTarget
                .request(MediaType.APPLICATION_JSON)
                .get(NLP.class);
        if(nlp == null){
            hostIp = "https://nlpservice.experimental.slidewiki.org/nlp/nlpForDeck/" + deckID;
            webTarget = client.target(hostIp);
            nlp = webTarget
                    .queryParam(DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_SLIDE, SPOTLIGHT_CONFIDENCE_FOR_SLIDE_VALUE)
                    .queryParam(DBPEDIA_SPOTLIGHT_CONFIDENCE_FOR_DECK, SPOTLIGHT_CONFIDENCE_FOR_DECK_VALUE)
                    .request(MediaType.APPLICATION_JSON)
                    .get(NLP.class);
        }
        if(nlp != null){
            DBPediaSpotlightResult spotlightResults = nlp.getDBPediaSpotlight();
            if(spotlightResults != null){
                List<DBPediaResource> resources = spotlightResults.getDBPediaResources();
                if(resources != null && !resources.isEmpty()){
                    List<Frequency> frequencies = nlp.getDBPediaSpotlightURIFrequencies();
                    int maxIndex = frequencies.size() < 5 ? frequencies.size() - 1 : 5;
                    frequencies = frequencies.subList(0, maxIndex);
                    resources = QGenUtils.removeDuplicatesFromResourceList(resources);
                    List<DBPediaResource> temp = new ArrayList<>();
                    for (DBPediaResource resource : resources) {
                        for (Frequency frequency : frequencies) {
                            if(resource.getURI().equals(frequency.getEntry())){
                                temp.add(resource);
                            }
                        }
                    }
                    resources = temp;
                    if(type.equals(GAP_FILL)) {
                        List<GapFillDistractor> distractors = getGapFillDistractors(resources, level);
                        List<QuestionSetPerSlide> gapFillQuestionSets = getGapFillQuestions(null, nlp.getChildren(), distractors);
                        if(gapFillQuestionSets != null) {
                            return Response.status(200).entity(gapFillQuestionSets).build();
                        }
                    }
                    if(type.equals(SELECT)) {
                        List<SelectQuestion> selectQuestions = getSelectQuestions(resources, level);
                        if(selectQuestions != null){
                            return Response.status(200).entity(selectQuestions).build();
                        }
                    }
                    if(type.equals(WHOAMI)) {
                        List<WhoAmIQuestion> questions = getWhoamIQuestions(resources, level);
                        if(questions != null){
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
        if(type.equals(WHOAMI)){
            retriever = new TextInfoRetriever(text, DBPEDIA_PERSON, servletContext);
        } else {
            retriever = new TextInfoRetriever(text, servletContext);
        }
        int maxIndex = retriever.getDbPediaResources().size() < 5 ? retriever.getDbPediaResources().size() - 1 : 5;
        resources.addAll(retriever.getFrequentWords(maxIndex).keySet());

        if(type.equals(GAP_FILL)) {
            List<GapFillDistractor> distractors = getGapFillDistractors(resources, level);
            List<QuestionSetPerSlide> gapFillQuestionSets = getGapFillQuestions(text, null, distractors);
            if(gapFillQuestionSets != null) {
                return Response.status(200).entity(gapFillQuestionSets).build();
            }
        }
        if(type.equals(SELECT)) {
            List<SelectQuestion> selectQuestions = getSelectQuestions(resources, level);
            if(selectQuestions != null){
                return Response.status(200).entity(selectQuestions).build();
            }
        }
        if(type.equals(WHOAMI)) {
            List<WhoAmIQuestion> questions = getWhoamIQuestions(resources, level);
            if(questions != null){
                return Response.status(200).entity(questions).build();
            }
        }
        return Response.noContent().build();
    }

    private List<WhoAmIQuestion> getWhoamIQuestions(List<DBPediaResource> dbPediaResources, String level) {
        if(dbPediaResources != null && !dbPediaResources.isEmpty()){
            dbPediaResources = QGenUtils.removeDuplicatesFromResourceList(dbPediaResources);
            dbPediaResources = dbPediaResources.stream().
                    filter(dbPediaResource -> {
                        String types = dbPediaResource.getTypes();
                        return !types.isEmpty() && types.contains(DBPEDIA_PERSON);
                    }).collect(Collectors.toList());
            List<WhoAmIQuestion> whoAmIQuestions = new ArrayList<>();
            dbPediaResources.forEach(resource -> {
                WhoAmIQuestion whoAmIQuestionAndAnswers = DistractorGenerator.getWhoAmIQuestionAndDistractors(resource, level);
                if (whoAmIQuestionAndAnswers != null) {
                    whoAmIQuestions.add(whoAmIQuestionAndAnswers);
                }
            });
            return whoAmIQuestions;
        }
        return null;
    }

    private List<SelectQuestion> getSelectQuestions(List<DBPediaResource> resources, String level) {
        if(resources!= null && !resources.isEmpty()) {
            resources = QGenUtils.removeDuplicatesFromResourceList(resources);
            List<SelectQuestion> selectQuestions = new ArrayList<>();
            resources.forEach(resource -> {
                SelectQuestion.SelectQuestionBuilder questionBuilder = SelectQuestion.builder();
                List<String> answerAndDistractors = DistractorGenerator.getSelectQuestionDistractors(resource, level);
                if (answerAndDistractors != null && !answerAndDistractors.isEmpty()) {
                    String answer = answerAndDistractors.get(0);
                    if (!answer.trim().isEmpty()) {
                        questionBuilder.questionText(resource.getSurfaceForm() + SELECT_QUESTION_TEXT)
                                .answer(answer);
                        if (answerAndDistractors.size() > 1) {
                            questionBuilder.distractors(answerAndDistractors.subList(1, answerAndDistractors.size()));
                        }
                        selectQuestions.add(questionBuilder.build());
                    }
                }
            });
            return selectQuestions;
        }
        return null;
    }

    private List<QuestionSetPerSlide> getGapFillQuestions(String text, List<NlpProcessResults> nlpProcessResults, List<GapFillDistractor> distractorsPerResource) {
        List<QuestionSetPerSlide> questionSetPerSlideList = new ArrayList<>();
        QuestionSetPerSlide.QuestionSetPerSlideBuilder builder;
        List<Question> questions;

        if(text != null) {
            builder = QuestionSetPerSlide.builder();
            questions = getGapFillQuestionSetForText(text, distractorsPerResource);
            builder.questionSet(questions);
            questionSetPerSlideList.add(builder.build());
        } else if(nlpProcessResults != null) {
            // Each process result is for a slide
            for (NlpProcessResults result : nlpProcessResults) {
                builder = QuestionSetPerSlide.builder();
                questions = getGapFillQuestionSetForText(result.getSlideTitleAndText(), distractorsPerResource);
                builder.
                        slideId(result.getSlideId()).
                        questionSet(questions);
                questionSetPerSlideList.add(builder.build());
            }
        }
        return questionSetPerSlideList;
    }

    private List<Question> getGapFillQuestionSetForText(String text, List<GapFillDistractor> distractorsPerResource) {
        List<Question> gapFillQuestionSets = new ArrayList<>();
        String cleanText = text.replaceAll("/\\s*(?:[\\dA-Z]+\\.|[a-z]\\)|•)+/gm", ". ");
        cleanText = cleanText.replaceAll("/(\r\n|\n|\r)/gm",". ");
        cleanText = cleanText.replaceAll("\n",". ");
        LanguageProcessor processor = new LanguageProcessor(cleanText);
        List<String> sentences = processor.getSentences();
        for (GapFillDistractor gapFillDistractor : distractorsPerResource) {
            GapFillQuestionSet questionSet = makeGapFillQuestionsFromSentences(sentences, gapFillDistractor.getSurfaceForm(), gapFillDistractor.getPluralSurfaceForm()
                    , gapFillDistractor.getExternalDistractors(), gapFillDistractor.getInTextDistractors());
            if(questionSet != null){
                gapFillQuestionSets.add(questionSet);
            }
        }
        if(gapFillQuestionSets.isEmpty()){
            return null;
        }
        return gapFillQuestionSets;
    }

    private List<GapFillDistractor> getGapFillDistractors(List<DBPediaResource> dbPediaResources, String level) {

        List<GapFillDistractor> distractorsPerResource = new ArrayList<>();
        String env = servletContext.getInitParameter("env");
        boolean envIsDev = env == null || !env.equalsIgnoreCase("prod");

        String dir = System.getProperty("user.dir");
        RiWordNet wordnet = new RiWordNet(dir + "/wordnet/");

        ImmutableListMultimap<String, DBPediaResource> mapOfGroupedResources = TextInfoRetriever.groupResourcesByType(dbPediaResources);
        for (DBPediaResource resource: dbPediaResources) {
            String surfaceForm = resource.getSurfaceForm();
            String plural = RiTa.pluralize(surfaceForm);
            if (envIsDev) {
                QGenLogger.info(surfaceForm);
            }
            List<String> externalDistractors = TextInfoRetriever.getExternalDistractors(resource, level);
            if (externalDistractors == null) {
                externalDistractors = attemptToGetSynonyms(wordnet, resource.getSurfaceForm());
            }
            List<String> inTextDistractors = mapOfGroupedResources.get(resource.getTypes()).stream().
                    filter(res -> (!res.equals(resource) && !res.getSurfaceForm().equalsIgnoreCase(resource.getSurfaceForm())))
                    .map(DBPediaResource::getSurfaceForm).collect(Collectors.toList());
            QGenUtils.removeDuplicatesFromStringList(inTextDistractors);
            GapFillDistractor.GapFillDistractorBuilder builder = GapFillDistractor.builder();
            builder.
                    surfaceForm(resource.getSurfaceForm()).
                    pluralSurfaceForm(plural).
                    resourceURI(resource.getURI()).
                    inTextDistractors(inTextDistractors).
                    externalDistractors(externalDistractors);
            distractorsPerResource.add(builder.build());
        }
        return distractorsPerResource;
    }

    private List<String> attemptToGetSynonyms(RiWordNet wordnet, String surfaceForm) {
        List<String> synList;
        String[] synonyms = wordnet.getAllSynonyms(surfaceForm, RiWordNet.NOUN);
        synList = Arrays.asList(synonyms);
        return synList;
    }

    private GapFillQuestionSet makeGapFillQuestionsFromSentences(List<String> sentences, String resourceName, String pluralResourceName,
                                                                 List<String> externalDistractors, List<String> inTextDistractors) {
        List<String> questions = new ArrayList<>();
        GapFillQuestionSet.GapFillQuestionSetBuilder builder = GapFillQuestionSet.builder();
        sentences.forEach(s -> {
            if (!s.equalsIgnoreCase(resourceName + ".") && QGenUtils.sourceHasWord(s, resourceName)) {
                String questionText = s.replaceAll("\\b" + resourceName + "\\b", BLANK);
                if (QGenUtils.sourceHasWord(s, pluralResourceName)) {
                    questionText = questionText.replaceAll("\\b" + pluralResourceName + "\\b", BLANK);
                    builder.answer(resourceName + ", " + pluralResourceName);
                }
                questions.add(questionText);
            }
        });
        if(questions.isEmpty()){
            return null;
        }
        return builder.
                questions(questions).
                answer(resourceName).
                externalDistractors(externalDistractors).
                inTextDistractors(inTextDistractors).
                build();
    }
}