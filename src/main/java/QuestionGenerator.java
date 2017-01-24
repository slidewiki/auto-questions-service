import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */
public class QuestionGenerator {

    public void generate() throws FileNotFoundException, UnsupportedEncodingException {
        String text = NLPConsts.SOLAR_SYSTEM_ARTICLE;
        TextInfoRetriever retriever = new TextInfoRetriever(text);
        List<DBPediaResource> resources = retriever.getDbPediaResources();
        PrintWriter resourceWriter = new PrintWriter("resources.txt", "UTF-8");
        resources.forEach(resource -> resourceWriter.println(resource.toString()));
        resourceWriter.close();

        Map<DBPediaResource, Integer> frequentWords = retriever.getFrequentWords(NLPConsts.FREQUENT_WORDS_COUNT);

        PrintWriter wordWriter = new PrintWriter("frequentWords.txt", "UTF-8");
        frequentWords.forEach((resource, integer) -> wordWriter.println(resource.getSurfaceForm() + " " + integer));
        wordWriter.close();

        // TODO Get most relevant words
        PrintWriter questionWriter = new PrintWriter("questions.txt", "UTF-8");
        Set<DBPediaResource> frequentResources = frequentWords.keySet();
        LanguageProcessor processor = new LanguageProcessor(text);
        List<String> sentences = processor.getSentences();

        //TODO Efficiency?
        for(DBPediaResource resource : frequentResources) {
            String surfaceForm = resource.getSurfaceForm();
            List<String> distractors = retriever.getDistractors(resource); // TODO Cache distractors
            sentences.forEach(s -> {
                if(s.contains(surfaceForm)){
                    questionWriter.println("Question: " + s.replace(surfaceForm, "________"));
                    questionWriter.println("Answer: " + surfaceForm);
                    questionWriter.print("Distractors: ");
                    List<String> finalDistractors = DataStructureUtils.getRandomItemsFromList(distractors, 5);
                    finalDistractors.forEach(d -> questionWriter.print(d + ", "));
                    questionWriter.println("\n");
                }
            });
        }
        questionWriter.close();
    }
}
