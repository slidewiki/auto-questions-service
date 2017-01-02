import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by andy on 12/6/16.
 */
public class NLPStuff {

    private HashMap<String, ArrayList<String>> getKeywords(String text) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Annotation document = new Annotation(text);
        HashMap<String, ArrayList<String>> sentenceKeywordPairs = new HashMap<String, ArrayList<String>>();

        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
//            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
//            Set<Constituent> constituents = tree.constituents();
//            for (Constituent constituent : constituents) {
//                keywords.add(constituent.toString());
//            }
            ArrayList<String> keywords = new ArrayList<String>();
            List<CoreLabel> coreLabels = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < coreLabels.size(); ) {
                CoreLabel token = coreLabels.get(i);
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                if (!ne.equals("O")) {
                    String combinedNamedEntities = "";
                    while (!coreLabels.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class).equals("O")) {
                        combinedNamedEntities += coreLabels.get(i).get(CoreAnnotations.TextAnnotation.class) + " ";
                        i++;
                    }
                    keywords.add(combinedNamedEntities.trim());
                } else if (pos.equalsIgnoreCase("PRP") && sentence.toString().indexOf(word) == 0) {
                    keywords.add(word);
                }
                i++;
            }
            sentenceKeywordPairs.put(sentence.toString(), keywords);
        }
        return sentenceKeywordPairs;
    }


}
