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
    private static final String DBPEDIA_PERSON = "DBPedia:Person";
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
        DBPediaSpotlightResult spotlightResults = nlp.getNlpProcessResultsForDeck().getDBPediaSpotlightPerDeck();
        List<DBPediaResource> resources = QGenUtils.removeDuplicatesFromResourceList(spotlightResults.getDBPediaResources());
        List<QuestionSet> questionSets = getQuestionsForText(spotlightResults.getText(), resources, level);
        return Response.status(200).entity(questionSets).build();
    }

    @Path("/{level}/text")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateQuestionsForText(@PathParam("level") String level, String text) throws FileNotFoundException, UnsupportedEncodingException {

        List<QuestionSet> questionSets;
        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        questionSets = getQuestionsForText(text, dbPediaResources, level);

        if (questionSets.isEmpty()) return Response.noContent().build();
        return Response.status(200).entity(questionSets).build();
    }

    @Path("/text/numbers")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateQuestionsForValues(String text) {
        LanguageProcessor processor = new LanguageProcessor(text);
        List<QuestionSet> questionSets = new ArrayList<>();
        Map<String, List<String>> sentencesWithNumbers = processor.getCardinals();
        System.out.println(sentencesWithNumbers);
        Set<String> numbers = sentencesWithNumbers.keySet();
        sentencesWithNumbers.forEach((numberString, sentences) -> {
            QuestionSet.QuestionSetBuilder builder = QuestionSet.builder();
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
            questionSets.add(builder.build());
        });
        return Response.status(200).entity(questionSets).build();
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
        DBPediaSpotlightResult spotlightResults = nlp.getNlpProcessResultsForDeck().getDBPediaSpotlightPerDeck();
        if(spotlightResults != null){
            List<DBPediaResource> dbPediaResources = spotlightResults.getDBPediaResources();
            List<Question> questions = getSelectQuestions(dbPediaResources, level);
            if(questions != null){
                return Response.status(200).entity(questions).build();
            }
        }
        return Response.noContent().build();
    }

    @Path("/select/{level}/text")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateSelectQuestionsForText(@PathParam("level") String level, String text) throws FileNotFoundException, UnsupportedEncodingException {
        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        List<Question> questions = getSelectQuestions(dbPediaResources, level);
        if(questions != null){
            return Response.status(200).entity(questions).build();
        }
        return Response.noContent().build();
    }

    @Path("/whoami/{level}/text")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response generateWhoAmIQuestionsForText(@PathParam("level") String level, String text) throws FileNotFoundException, UnsupportedEncodingException {
        TextInfoRetriever retriever = new TextInfoRetriever(text, DBPEDIA_PERSON, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        List<WhoAmIQuestion> questions = getWhoamIQuestions(dbPediaResources, level);
        if(questions != null) {
            return Response.status(200).entity(questions).build();
        }
        return Response.noContent().build();
    }

    private List<WhoAmIQuestion> getWhoamIQuestions(List<DBPediaResource> dbPediaResources, String level) {
        if(dbPediaResources != null && !dbPediaResources.isEmpty()){
            dbPediaResources = QGenUtils.removeDuplicatesFromResourceList(dbPediaResources);
            List<WhoAmIQuestion> whoAmIQuestions = new ArrayList<>();
            dbPediaResources.forEach(resource -> {
                WhoAmIQuestion whoAmIQuestionAndAnswers = DistractorGenerator.getWhoAmIQuestionAndDistractors(resource, level);
                if (whoAmIQuestionAndAnswers != null) {
//                    String questionText = whoAmIQuestionAndAnswers.get(0);
//                    String answer = whoAmIQuestionAndAnswers.get(1);
//                    if (!answer.trim().isEmpty()) {
//                        questionBuilder.questionText(questionText)
//                                .answer(answer);
//                        if (whoAmIQuestionAndAnswers.size() > 2) {
//                            questionBuilder.distractors(whoAmIQuestionAndAnswers.subList(2, whoAmIQuestionAndAnswers.size()));
//                        }
//                        whoAmIQuestions.add(questionBuilder.build());
//                    }
                    whoAmIQuestions.add(whoAmIQuestionAndAnswers);
                }
            });
            return whoAmIQuestions;
        }
        return null;
    }

    private List<Question> getSelectQuestions(List<DBPediaResource> resources, String level) {
        if(resources!= null && !resources.isEmpty()) {
            resources = QGenUtils.removeDuplicatesFromResourceList(resources);
            List<Question> questions = new ArrayList<>();
            resources.forEach(resource -> {
                Question.QuestionBuilder questionBuilder = Question.builder();
                List<String> answerAndDistractors = DistractorGenerator.getSelectQuestionDistractors(resource, level);
                if (answerAndDistractors != null && !answerAndDistractors.isEmpty()) {
                    String answer = answerAndDistractors.get(0);
                    if (!answer.trim().isEmpty()) {
                        questionBuilder.questionText(resource.getSurfaceForm() + SELECT_QUESTION_TEXT)
                                .answer(answer);
                        if (answerAndDistractors.size() > 1) {
                            questionBuilder.distractors(answerAndDistractors.subList(1, answerAndDistractors.size()));
                        }
                        questions.add(questionBuilder.build());
                    }
                }
            });
            return questions;
        }
        return null;
    }

    private List<QuestionSet> getQuestionsForText(String text, List<DBPediaResource> dbPediaResources, String level) {
        List<QuestionSet> questionSets = new ArrayList<>();

        String env = servletContext.getInitParameter("env");
        boolean envIsDev = env == null || !env.equalsIgnoreCase("prod");

        String dir = System.getProperty("user.dir");
        RiWordNet wordnet = new RiWordNet(dir + "/wordnet/");

//        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
//        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();

        if (dbPediaResources == null || dbPediaResources.size() == 0) {
            return questionSets;
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
            questionSets.add(getQuestionsForResource(sentences, surfaceForm, plural, externalDistractors, inTextDistractors));
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
        return questionSets;
    }

    private List<String> attemptToGetSynonyms(RiWordNet wordnet, String surfaceForm) {
        List<String> synList;
        String[] synonyms = wordnet.getAllSynonyms(surfaceForm, RiWordNet.NOUN);
        synList = Arrays.asList(synonyms);
        return synList;
    }

    private QuestionSet getQuestionsForResource(List<String> sentences, String resourceName, String pluralResourceName, List<String> externalDistractors, List<String> inTextDistractors) {
        List<String> questions = new ArrayList<>();
        QuestionSet.QuestionSetBuilder builder = QuestionSet.builder();
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