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

    // One can use other entities of same type found in the text as distractors
    public List<String> getInTextDistractors(DBPediaResource answer, List<DBPediaResource> resources) {
        Map<String, DBPediaResource> resourceMap = new LinkedHashMap<>();
        List<String> distractors;
        for (DBPediaResource dbPediaResource : resources) {
            if (!dbPediaResource.getTypes().equals("")
                    && dbPediaResource.getTypes().equals(answer.getTypes())
                    && !dbPediaResource.getSurfaceForm().equals(answer.getSurfaceForm())) {
                resourceMap.put(dbPediaResource.getSurfaceForm(), dbPediaResource);
            }
        }
        distractors = new ArrayList<>(resourceMap.keySet());
        return distractors;
    }

    public List<String> getExternalDistractors(DBPediaResource answer) {
        ARQClient arqClient = new ARQClient();
        return arqClient.getSimilarResourceNames(answer);
    }
}