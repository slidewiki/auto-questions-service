package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ainuddin Faizan on 1/3/17.
 */
public class DistractorGenerator {
    public List<String> getExternalDistractors(DBPediaResource answer) {
        ARQClient arqClient = new ARQClient();
        return arqClient.getSimilarResourceNames(answer);
    }
}
