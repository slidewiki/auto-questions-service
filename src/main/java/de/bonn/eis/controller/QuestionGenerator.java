package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.Question;
import de.bonn.eis.model.SlideContent;
import de.bonn.eis.utils.NLPConsts;
import de.bonn.eis.utils.QGenUtils;

import javax.ws.rs.*;
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
public class QuestionGenerator {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generate(SlideContent content) throws FileNotFoundException, UnsupportedEncodingException {

        List<Question> questions = new ArrayList<>();
        String text = content.getText();
        TextInfoRetriever retriever = new TextInfoRetriever(text);
        List<DBPediaResource> resources = retriever.getDbPediaResources();

        //TODO Log extraneous info
//        PrintWriter resourceWriter = new PrintWriter("resources.txt", "UTF-8");
//        resources.forEach(resource -> resourceWriter.println(resource.toString()));
//        resourceWriter.close();


        // Selecting most frequently occurring words
//        Map<de.bonn.eis.model.DBPediaResource, Integer> frequentWords = retriever.getFrequentWords(de.bonn.eis.utils.NLPConsts.WORDS_COUNT);
//        Set<de.bonn.eis.model.DBPediaResource> topResources = frequentWords.keySet();

//        PrintWriter questionWriter = new PrintWriter("questions.txt", "UTF-8");
//        topResources.forEach((resource) -> questionWriter.println(resource.getSurfaceForm()));

        // Selecting most relevant occurring words
        List<DBPediaResource> topResources = retriever.getMostRelevantWords(NLPConsts.WORDS_COUNT);
//        PrintWriter wordWriter = new PrintWriter("relevantWords.txt", "UTF-8");
//        topResources.forEach((resource) -> wordWriter.println(resource.getSurfaceForm()));
//        wordWriter.close();

        LanguageProcessor processor = new LanguageProcessor(text);
        List<String> sentences = processor.getSentences();

        //TODO Efficiency?
        //TODO Create distractor cache for resources with same types or create some scheme
        for(DBPediaResource resource : topResources) {
            String surfaceForm = resource.getSurfaceForm();
            List<String> distractors = retriever.getDistractors(resource); // TODO distractors need to be much more specific - calculate contextual score ?

            sentences.forEach(s -> {
                if(QGenUtils.sourceHasWord(s, surfaceForm)){
                    Question question = new Question();
                    String questionText = s.replace(surfaceForm, "________");
                    question.setQuestionText(questionText);
                    question.setAnswer(surfaceForm);
                    question.setDistractors(distractors);
                    questions.add(question);
                }
            });
        }
        return Response.status(200).entity(questions).build();
    }
}
