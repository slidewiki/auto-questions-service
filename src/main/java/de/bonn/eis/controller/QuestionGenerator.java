package de.bonn.eis.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.Question;
import de.bonn.eis.model.SlideContent;
import de.bonn.eis.utils.NLPConsts;
import de.bonn.eis.utils.QGenLogger;
import de.bonn.eis.utils.QGenUtils;
import rita.RiString;
import rita.RiWordNet;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */

@Path("/qgen")
public class QuestionGenerator {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(SlideContent content, @Context ServletContext servletContext) throws FileNotFoundException, UnsupportedEncodingException {

        List<Question> questions = new ArrayList<>();
        String text = content.getText();
        String env = servletContext.getInitParameter("env");
        boolean envIsDev = env == null || !env.equalsIgnoreCase("prod");

//        RiWordNet wordnet = new RiWordNet("/questions-service/wordnet");
//        if(envIsDev){
//            String[] syn = wordnet.getAllSynonyms("The W3C has over 10000 standards", RiWordNet.NOUN);
//            Arrays.asList(syn).forEach(s -> QGenLogger.info(s));
//        }

        TextInfoRetriever retriever = new TextInfoRetriever(text, servletContext);
        List<DBPediaResource> dbPediaResources = retriever.getDbPediaResources();
        if(dbPediaResources == null || dbPediaResources.size() == 0) {
            return Response.noContent().build();
        }
        if(envIsDev){
            QGenLogger.info("Resources retrieved");
            dbPediaResources.forEach(resource -> QGenLogger.info(resource.getSurfaceForm()));
        }
        // Selecting most relevant occurring words
        List<DBPediaResource> topResources = retriever.getMostRelevantWords(NLPConsts.WORDS_COUNT);
        if(envIsDev){
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
            if(envIsDev)
                QGenLogger.info(type);
            if(type.isEmpty()){
                for (DBPediaResource resource : groupedResources) {
                    if(envIsDev)
                        QGenLogger.info(resource.getSurfaceForm());
                    List<String> externalDistractors = retriever.getExternalDistractors(resource);
                    questions.addAll(getQuestionsForResource(sentences, resource.getSurfaceForm(), externalDistractors, new ArrayList<>()));
                }
            } else {
                DBPediaResource firstResource = groupedResources.get(0);
                List<String> externalDistractors = retriever.getExternalDistractors(firstResource); // TODO externalDistractors need to be much more specific - calculate contextual score ?
                for (DBPediaResource resource : groupedResources) {
                    if(envIsDev)
                        QGenLogger.info(resource.getSurfaceForm());
                    List<String> inTextDistractors = groupedResources.stream().filter(res -> !res.equals(resource))
                            .map(res -> res.getSurfaceForm()).collect(Collectors.toList());
                    questions.addAll(getQuestionsForResource(sentences, resource.getSurfaceForm(), externalDistractors, inTextDistractors));
                }
            }
        });
        return Response.status(200).entity(questions).build();
    }

private List<Question> getQuestionsForResource(List<String> sentences, String resourceName, List<String> externalDistractors, List<String> inTextDistractors) {
        List<Question> questions = new ArrayList<>();

        sentences.forEach(s -> {
            if(QGenUtils.sourceHasWord(s, resourceName)){
                Question question = new Question();
                String questionText = s.replace(resourceName, "________");
                question.setQuestionText(questionText);
                question.setAnswer(resourceName);
                question.setExternalDistractors(externalDistractors);
                question.setInTextDistractors(inTextDistractors);
                questions.add(question);
            }
        });
        return questions;
    }
}
