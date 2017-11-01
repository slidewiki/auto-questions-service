package de.bonn.eis.controller;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
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

    public List<String> getSentences() {
        List<String> sentences = new ArrayList<>();
        SentenceDetectorME detector = new SentenceDetectorME(model);
        sentences.addAll(Arrays.asList(detector.sentDetect(text)));
        return sentences;
    }
}