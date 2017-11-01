package de.bonn.eis.controller;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import rita.RiTa;
import rita.RiWordNet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 12/6/16.
 */
public class LanguageProcessor {

    private SentenceModel model;
    private String text;


    public LanguageProcessor(String text) {
        String dir = System.getProperty("user.dir");
        try {
            InputStream inputStream = new FileInputStream(dir + "/en-sent.bin");
            model = new SentenceModel(inputStream);
            this.text = text;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static List<String> attemptToGetSynonyms(RiWordNet wordnet, String surfaceForm) {
        List<String> synList;
        String[] synonyms = wordnet.getAllSynonyms(surfaceForm, RiWordNet.NOUN);
        synList = Arrays.asList(synonyms);
        return synList;
    }

    static List<String> singularizePluralTypes(List<String> pluralTypes) {
        // Singularize plural types
        ArrayList<String> singleTypes = new ArrayList<>();
        for (String pluralType : pluralTypes) {
            String[] typeArray = pluralType.split(" ");
            StringBuilder result = new StringBuilder();
            for (String s : typeArray) {
                String singular = RiTa.singularize(s);
                String plural = RiTa.pluralize(singular);
                if (plural.equalsIgnoreCase(s)) {
                    result.append(singular).append(" ");
                } else {
                    result.append(s).append(" ");
                }
            }
            singleTypes.add(result.toString().trim());
        }
        return singleTypes;
    }

    public List<String> getSentences() {
        List<String> sentences = new ArrayList<>();
        SentenceDetectorME detector = new SentenceDetectorME(model);
        sentences.addAll(Arrays.asList(detector.sentDetect(text)));
        return sentences;
    }
}