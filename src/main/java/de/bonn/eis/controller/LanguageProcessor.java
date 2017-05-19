package de.bonn.eis.controller;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

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

    public List<String> getSentences() {
        List<String> sentences = new ArrayList<>();
        SentenceDetectorME detector = new SentenceDetectorME(model);
        sentences.addAll(Arrays.asList(detector.sentDetect(text)));
        return sentences;
    }
}