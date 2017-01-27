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
        String text = NLPConsts.FIFA_WC_ARTICLE;
        TextInfoRetriever retriever = new TextInfoRetriever(text);
        List<DBPediaResource> resources = retriever.getDbPediaResources();
        PrintWriter resourceWriter = new PrintWriter("resources.txt", "UTF-8");
        resources.forEach(resource -> resourceWriter.println(resource.toString()));
        resourceWriter.close();


        Map<DBPediaResource, Integer> frequentWords = retriever.getFrequentWords(NLPConsts.FREQUENT_WORDS_COUNT);
        Set<DBPediaResource> topResources = frequentWords.keySet();
        PrintWriter questionWriter = new PrintWriter("questions.txt", "UTF-8");
        topResources.forEach((resource) -> questionWriter.println(resource.getSurfaceForm()));

//        List<DBPediaResource> topResources = retriever.getMostRelevantWords(NLPConsts.FREQUENT_WORDS_COUNT);
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
                if(s.contains(surfaceForm)){
                    questionWriter.println("Question: " + s.replace(surfaceForm, "________"));
                    questionWriter.println("Answer: " + surfaceForm);
                    questionWriter.print("Distractors: ");
//                    List<String> finalDistractors = DataStructureUtils.getRandomItemsFromList(distractors, 5);
                    distractors.forEach(d -> questionWriter.print(d + ", "));
                    questionWriter.println("\n");
                }
            });
        }
        questionWriter.close();
    }
}
