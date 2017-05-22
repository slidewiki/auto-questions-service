package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.WhoAmIQuestionStructure;

import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/3/17.
 */
public class DistractorGenerator {
    public static List<String> getExternalDistractors(DBPediaResource answer, String level) {
        ARQClient arqClient = new ARQClient();
        return arqClient.getSimilarResourceNames(answer, level);
    }

    public static List<String> getSelectQuestionDistractors(DBPediaResource answer, String level) {
        ARQClient arqClient = new ARQClient();
        return arqClient.getSisterTypes(answer, level);
    }

    public static WhoAmIQuestionStructure getWhoAmIQuestionAndDistractors(DBPediaResource resource, String level) {
        ARQClient arqClient = new ARQClient();
        return arqClient.getWhoAmIQuestion(resource, level);
    }
}
