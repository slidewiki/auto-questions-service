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
    @Context
    private ServletContext servletContext;

    @Path("/{level}/{deckID}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateQuestionsForSlides(@PathParam("level") String level, @PathParam("deckID") String deckID) {
        Client client = ClientBuilder.newClient();
        String hostIp = "https://nlpservice.experimental.slidewiki.org/nlp/nlpForDeck/" + deckID;
        WebTarget webTarget = client.target(hostIp);
        NLP nlp = webTarget
                .request(MediaType.APPLICATION_JSON)
                .get(NLP.class);
        DBPediaSpotlightPOJO spotlightResults = nlp.getNlpProcessResultsForDeck().getDBPediaSpotlightPerDeck();
        List<DBPediaResource> resources = QGenUtils.removeDuplicatesFromResourceList(spotlightResults.getDBPediaResources());
        List<Question> questions = getQuestionsForText(spotlightResults.getText(), resources, level);
        return Response.status(200).entity(questions).build();
    }

    @Path("/{level}/text")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateQuestionsForText(@PathParam("level") String level, String text) throws FileNotFoundException, UnsupportedEncodingException {

        List<Question> questions;
        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        questions = getQuestionsForText(text, dbPediaResources, level);

        if (questions.isEmpty()) return Response.noContent().build();
        return Response.status(200).entity(questions).build();
    }

    @Path("/text/numbers")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateQuestionsForValues(String text) {
        LanguageProcessor processor = new LanguageProcessor(text);
        List<Question> questions = new ArrayList<>();
        Map<String, List<String>> sentencesWithNumbers = processor.getCardinals();
        System.out.println(sentencesWithNumbers);
        Set<String> numbers = sentencesWithNumbers.keySet();
        sentencesWithNumbers.forEach((numberString, sentences) -> {
            Question.QuestionBuilder builder = Question.builder();
            builder.
                    answer(numberString).
                    inTextDistractors(numbers.stream().
                            filter(num -> !num.equalsIgnoreCase(numberString)).collect(Collectors.toList()));
            List<String> questionStrings = new ArrayList<>();
            sentences.forEach(sentence -> {
                String questionText = sentence.replaceAll("\\b" + numberString + "\\b", BLANK);
                questionStrings.add(questionText);
            });
            builder.questions(questionStrings);
            questions.add(builder.build());
        });
        return Response.status(200).entity(questions).build();
    }

    @Path("/select/{level}/{deckID}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateSelectQuestions(@PathParam("level") String level, @PathParam("deckID") String deckID) {
        Client client = ClientBuilder.newClient();
        String hostIp = "https://nlpservice.experimental.slidewiki.org/nlp/nlpForDeck/" + deckID;
        WebTarget webTarget = client.target(hostIp);
        NLP nlp = webTarget
                .request(MediaType.APPLICATION_JSON)
                .get(NLP.class);
        DBPediaSpotlightPOJO spotlightResults = nlp.getNlpProcessResultsForDeck().getDBPediaSpotlightPerDeck();
        List<DBPediaResource> resources = QGenUtils.removeDuplicatesFromResourceList(spotlightResults.getDBPediaResources());
        List<SelectQuestion> questions = getSelectQuestions(resources, level);
        return Response.status(200).entity(questions).build();
    }

    @Path("/select/{level}/text")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateSelectQuestionsForText(@PathParam("level") String level, String text) throws FileNotFoundException, UnsupportedEncodingException {
        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        dbPediaResources = QGenUtils.removeDuplicatesFromResourceList(dbPediaResources);
        List<SelectQuestion> questions = getSelectQuestions(dbPediaResources, level);
        return Response.status(200).entity(questions).build();
    }

    private List<SelectQuestion> getSelectQuestions(List<DBPediaResource> resources, String level) {
        List<SelectQuestion> selectQuestions = new ArrayList<>();
        resources.forEach(resource -> {
            SelectQuestion.SelectQuestionBuilder questionBuilder = SelectQuestion.builder();
            List<String> answerAndDistractors = DistractorGenerator.getSelectQuestionDistractors(resource, level);
            if(answerAndDistractors != null && !answerAndDistractors.isEmpty()){
                String answer = answerAndDistractors.get(0);
                if(!answer.trim().isEmpty()){
                    questionBuilder.questionText(resource.getSurfaceForm() + SELECT_QUESTION_TEXT)
                            .answer(answer);
                    if(answerAndDistractors.size() > 1){
                        questionBuilder.distractors(answerAndDistractors.subList(1, answerAndDistractors.size()));
                    }
                    selectQuestions.add(questionBuilder.build());
                }
            }
        });
        return selectQuestions;
    }

    private List<Question> getQuestionsForText(String text, List<DBPediaResource> dbPediaResources, String level) {
        List<Question> questions = new ArrayList<>();

        String env = servletContext.getInitParameter("env");
        boolean envIsDev = env == null || !env.equalsIgnoreCase("prod");

        String dir = System.getProperty("user.dir");
        RiWordNet wordnet = new RiWordNet(dir + "/wordnet/");

//        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
//        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();

        if (dbPediaResources == null || dbPediaResources.size() == 0) {
            return questions;
        }
        if (envIsDev) {
            QGenLogger.info("Resources retrieved");
            dbPediaResources.forEach(resource -> QGenLogger.info(resource.getSurfaceForm()));
        }
        // Selecting most relevant occurring words
//        List<DBPediaResource> topResources = retriever.getMostRelevantWords(NLPConsts.WORDS_COUNT);
//        List<DBPediaResource> topResources = dbPediaResources;
//        if (envIsDev) {
//            QGenLogger.info("Relevant resources");
//            topResources.forEach(resource -> QGenLogger.info(resource.getSurfaceForm()));
//        }
        String cleanText = text.replaceAll("/\\s*(?:[\\dA-Z]+\\.|[a-z]\\)|•)+/gm", ".");
        System.out.println(cleanText);
        LanguageProcessor processor = new LanguageProcessor(cleanText);
        List<String> sentences = processor.getSentences();

        //TODO Efficiency?
        //TODO Create distractor cache for resources with same types or create some scheme

        ImmutableListMultimap<String, DBPediaResource> mapOfGroupedResources = TextInfoRetriever.groupResourcesByType(dbPediaResources);
        dbPediaResources.forEach(resource -> {
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
            questions.add(getQuestionsForResource(sentences, surfaceForm, plural, externalDistractors, inTextDistractors));
        });

//        ImmutableListMultimap<String, DBPediaResource> mapOfGroupedResources = TextInfoRetriever.groupResourcesByType(topResources);
//        ImmutableSet<String> types = mapOfGroupedResources.keySet();
//        types.forEach(type -> {
//            ImmutableList<DBPediaResource> groupedResources = mapOfGroupedResources.get(type);
//            if (envIsDev)
//                QGenLogger.info(type);
//            if (type.isEmpty()) {
//                for (DBPediaResource resource : groupedResources) {
//                    String surfaceForm = resource.getSurfaceForm();
//                    String plural = RiTa.pluralize(surfaceForm);
//                    if (envIsDev) {
//                        QGenLogger.info(surfaceForm);
//                    }
//                    List<String> externalDistractors = TextInfoRetriever.getExternalDistractors(resource);
//                    if (externalDistractors == null) {
//                        externalDistractors = attemptToGetSynonyms(wordnet, resource.getSurfaceForm());
//                    }
//                    questions.addAll(getQuestionsForResource(sentences, surfaceForm, plural, externalDistractors, new ArrayList<>()));
//                }
//            } else {
//                DBPediaResource firstResource = groupedResources.get(0);
//                List<String> externalDistractors = TextInfoRetriever.getExternalDistractors(firstResource);
//                for (DBPediaResource resource : groupedResources) {
//                    String surfaceForm = resource.getSurfaceForm();
//                    String plural = RiTa.pluralize(surfaceForm);
//                    if (envIsDev) {
//                        QGenLogger.info(surfaceForm);
//                    }
//                    if (externalDistractors == null) {
//                        externalDistractors = attemptToGetSynonyms(wordnet, resource.getSurfaceForm());
//                    }
//                    List<String> inTextDistractors = groupedResources.stream().filter(res -> !res.equals(resource))
//                            .map(res -> surfaceForm).collect(Collectors.toList());
//                    questions.addAll(getQuestionsForResource(sentences, surfaceForm, plural, externalDistractors, inTextDistractors));
//                }
//            }
//        });
        return questions;
    }

    private List<String> attemptToGetSynonyms(RiWordNet wordnet, String surfaceForm) {
        List<String> synList;
        String[] synonyms = wordnet.getAllSynonyms(surfaceForm, RiWordNet.NOUN);
        synList = Arrays.asList(synonyms);
        return synList;
    }

    private Question getQuestionsForResource(List<String> sentences, String resourceName, String pluralResourceName, List<String> externalDistractors, List<String> inTextDistractors) {
        List<String> questions = new ArrayList<>();
        Question.QuestionBuilder builder = Question.builder();
        builder.answer(resourceName).
                externalDistractors(externalDistractors).
                inTextDistractors(inTextDistractors);
        sentences.forEach(s -> {
            if (QGenUtils.sourceHasWord(s, resourceName)) {
                String questionText = s.replaceAll("\\b" + resourceName + "\\b", BLANK);
                if (QGenUtils.sourceHasWord(s, pluralResourceName)) {
                    questionText = questionText.replaceAll("\\b" + pluralResourceName + "\\b", BLANK);
                    builder.answer(resourceName + ", " + pluralResourceName);
                }
                questions.add(questionText);
            }
        });
        builder.questions(questions);
        return builder.build();
    }
}