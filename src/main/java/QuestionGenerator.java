import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

/**
 * Created by andy on 1/2/17.
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
        Set<Map.Entry<DBPediaResource, Integer>> entries = frequentWords.entrySet();
        String changedText = text;
        for(Map.Entry<DBPediaResource, Integer> entry : entries) {
            changedText = changedText.replace(entry.getKey().getSurfaceForm(), "________");
        }
        questionWriter.println(changedText);
        questionWriter.close();
    }
}
