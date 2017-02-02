package de.bonn.eis;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * Created by Ainuddin Faizan on 12/6/16.
 */
public class LanguageProcessor {

    private Properties props;
    private StanfordCoreNLP pipeline;
    private Annotation document;

    public LanguageProcessor(String text) {
        props = new Properties();
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
    }
}
