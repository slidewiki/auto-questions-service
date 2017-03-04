package de.bonn.eis.controller;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
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
    private List<CoreMap> coreMaps;

    public LanguageProcessor(String text) {
        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos");
        pipeline = new StanfordCoreNLP(props);
        document = new Annotation(text);
        pipeline.annotate(document);
        coreMaps = document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public List<String> getSentences() {
        List<String> sentences = new ArrayList<>();
        coreMaps.forEach(coreMap -> sentences.add(coreMap.get(CoreAnnotations.TextAnnotation.class)));
        return sentences;
    }

    public Map<String, List<String>> getCardinals() {
        Map<String, List<String>> cardinalsAndSentencesMap = new LinkedHashMap<>();
        coreMaps.forEach(coreMap -> {
            for (CoreLabel token : coreMap.get(CoreAnnotations.TokensAnnotation.class)) {
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (pos.equalsIgnoreCase("CD")) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    List<String> sentenceList = cardinalsAndSentencesMap.getOrDefault(word, new ArrayList<>());
                    sentenceList.add(coreMap.get(CoreAnnotations.TextAnnotation.class));
                    cardinalsAndSentencesMap.put(word, sentenceList);
                }
            }
        });
        return cardinalsAndSentencesMap;
    }
//
//    public String getPOSTag(int offset) {
//        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
//        CoreLabel token = tokens.get(offset);
//        return token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//    }
}
