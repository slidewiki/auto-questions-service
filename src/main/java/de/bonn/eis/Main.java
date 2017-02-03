package de.bonn.eis;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class Main {

    public static void main(String[] args) {
        try {
            new QuestionGenerator().generate();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        new de.bonn.eis.NIF().nifRun();
//        de.bonn.eis.ARQClient.getResourceTypes();
    }

    public static String getMessage() {
        try {
            new QuestionGenerator().generate();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "Hello World";
    }
}
