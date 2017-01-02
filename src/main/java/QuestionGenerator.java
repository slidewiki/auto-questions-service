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
        String text = NLPConsts.article;
        TextInfoRetriever retriever = new TextInfoRetriever();
        Map<DBPediaResource, Integer> frequentWords = retriever.getFrequentWords(text);

        PrintWriter wordWriter = new PrintWriter("frequentWords.txt", "UTF-8");
        frequentWords.forEach((resource, integer) -> wordWriter.println(resource.getSurfaceForm() + " " + integer));
        wordWriter.close();

        PrintWriter questionWriter = new PrintWriter("questions.txt", "UTF-8");
        Set<DBPediaResource> frequentResources = frequentWords.keySet();
        LanguageProcessor processor = new LanguageProcessor(text);
        List<String> sentences = processor.getSentences();

        for(DBPediaResource entry : frequentResources) {
            sentences.forEach(s -> {
                if(s.contains(entry.getSurfaceForm())){
                    questionWriter.println("Question: " + s.replace(entry.getSurfaceForm(), "________"));
                    questionWriter.println("Answer: " + entry.getSurfaceForm());
                }
            });
            questionWriter.println();
        }
        questionWriter.close();
    }
}
