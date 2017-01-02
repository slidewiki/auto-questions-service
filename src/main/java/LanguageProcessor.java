import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * Created by andy on 12/6/16.
 */
public class LanguageProcessor {

    private Properties props;
    private StanfordCoreNLP pipeline;
    private Annotation document;

    public LanguageProcessor(String text) {
        props = new Properties();
//        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("annotators", "tokenize, ssplit");
        pipeline = new StanfordCoreNLP(props);
        document = new Annotation(text);
        pipeline.annotate(document);
    }

    public List<String> getSentences() {

        List<CoreMap> coreMaps = document.get(CoreAnnotations.SentencesAnnotation.class);
        List<String> sentences = new ArrayList<>();
        coreMaps.forEach(coreMap -> sentences.add(coreMap.get(CoreAnnotations.TextAnnotation.class)));
        return sentences;

//        for (CoreMap sentence : sentences) {
//            ArrayList<String> keywords = new ArrayList<String>();
//            List<CoreLabel> coreLabels = sentence.get(CoreAnnotations.TokensAnnotation.class);
//            for (int i = 0; i < coreLabels.size(); ) {
//                CoreLabel token = coreLabels.get(i);
//                String word = token.get(CoreAnnotations.TextAnnotation.class);
//                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//
//                if (!ne.equals("O")) {
//                    String combinedNamedEntities = "";
//                    while (!coreLabels.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class).equals("O")) {
//                        combinedNamedEntities += coreLabels.get(i).get(CoreAnnotations.TextAnnotation.class) + " ";
//                        i++;
//                    }
//                    keywords.add(combinedNamedEntities.trim());
//                } else if (pos.equalsIgnoreCase("PRP") && sentence.toString().indexOf(word) == 0) {
//                    keywords.add(word);
//                }
//                i++;
//            }
//            sentenceKeywordPairs.put(sentence.toString(), keywords);
//        }
//        return sentenceKeywordPairs;
    }


}
