package de.bonn.eis;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */

@Path("/qgen")
public class QuestionGenerator {

    @Path("{text}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String generate(@PathParam("text") String text) throws FileNotFoundException, UnsupportedEncodingException {
//        String text = NLPConsts.SOLAR_SYSTEM_ARTICLE;
        TextInfoRetriever retriever = new TextInfoRetriever(text);
        List<DBPediaResource> resources = retriever.getDbPediaResources();

        //TODO Log extraneous info
//        PrintWriter resourceWriter = new PrintWriter("resources.txt", "UTF-8");
//        resources.forEach(resource -> resourceWriter.println(resource.toString()));
//        resourceWriter.close();


        // Selecting most frequently occurring words
//        Map<de.bonn.eis.DBPediaResource, Integer> frequentWords = retriever.getFrequentWords(de.bonn.eis.NLPConsts.WORDS_COUNT);
//        Set<de.bonn.eis.DBPediaResource> topResources = frequentWords.keySet();

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
        final String[] response = {""};
        for(DBPediaResource resource : topResources) {
            String surfaceForm = resource.getSurfaceForm();
            List<String> distractors = retriever.getDistractors(resource); // TODO distractors need to be much more specific - calculate contextual score ?

            sentences.forEach(s -> {
                if(QGenUtils.sourceHasWord(s, surfaceForm)){
//                    questionWriter.println("Question: " + s.replace(surfaceForm, "________"));
//                    questionWriter.println("Answer: " + surfaceForm);
//                    questionWriter.print("Distractors: ");

                    response[0] += "Question: " + s.replace(surfaceForm, "________");
                    response[0] += "Answer: " + surfaceForm;
                    response[0] += ("Distractors: ");

//                    List<String> finalDistractors = de.bonn.eis.QGenUtils.getRandomItemsFromList(distractors, 5);
                    distractors.forEach(d -> {
//                        questionWriter.print(d + ", ");
                        response[0] += (d + ", ");
                        if(distractors.indexOf(d) % 5 == 0){
//                            questionWriter.println();
                            response[0] += "\n";
                        }
                    });
//                    questionWriter.println("\n");
                    response[0] += "\n";
                }
            });
        }
//        questionWriter.close();
        return response[0];
    }
}
