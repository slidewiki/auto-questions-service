package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;

import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/3/17.
 */
public class DistractorGenerator {
    public List<String> getExternalDistractors(DBPediaResource answer) {
        ARQClient arqClient = new ARQClient();
        return arqClient.getSimilarResourceNames(answer);
    }
}
