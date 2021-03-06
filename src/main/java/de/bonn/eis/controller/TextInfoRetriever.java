package de.bonn.eis.controller;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.DBPediaSpotlightResult;
import de.bonn.eis.utils.QGenUtils;

import javax.servlet.ServletContext;
import java.util.*;
import java.util.function.Function;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */
public class TextInfoRetriever {

    private List<DBPediaResource> dbPediaResources;

    public TextInfoRetriever(String text, ServletContext servletContext) {
        DBPediaSpotlightClient dbPediaSpotlightClient = new DBPediaSpotlightClient();
        dbPediaSpotlightClient.init(servletContext);
        DBPediaSpotlightResult response;
        response = dbPediaSpotlightClient.annotatePost(text);
        dbPediaResources = response.getDBPediaResources();
    }

    public TextInfoRetriever(String text, String filterType, ServletContext servletContext) {
        DBPediaSpotlightClient dbPediaSpotlightClient = new DBPediaSpotlightClient();
        dbPediaSpotlightClient.init(servletContext);
        DBPediaSpotlightResult response;
        response = dbPediaSpotlightClient.annotatePost(text, filterType);
        dbPediaResources = response.getDBPediaResources();
    }

    public TextInfoRetriever(List<DBPediaResource> dbPediaResources) {
        this.dbPediaResources = dbPediaResources;
    }

    /**
     * @param n Number of frequent resources desired. Pass 0 to get all words
     * @return A Map of most frequent de.bonn.eis.model.DBPediaResource objects and their count in Integer
     */
    public Map<DBPediaResource, Integer> getFrequentWords(int n) {

        Map<String, Integer> wordFrequencyPairs = new LinkedHashMap<>();
        Map<String, DBPediaResource> frequentResources = new LinkedHashMap<>();

        // get frequency of each unique resource
        for (DBPediaResource dbPediaResource : dbPediaResources) {
                int frequency = wordFrequencyPairs.getOrDefault(dbPediaResource.getURI(), 0) + 1;
                wordFrequencyPairs.put(dbPediaResource.getURI(), frequency);
                frequentResources.putIfAbsent(dbPediaResource.getURI(), dbPediaResource);
        }

        Map<String, Integer> sortedMap = QGenUtils.sortMap(wordFrequencyPairs);
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(sortedMap.entrySet());
        int size = entryList.size();
        sortedMap.clear();
        int firstIndex = 0;
        if (n > 0 && n < size) {
            firstIndex = size - n;
        }
        entryList.subList(firstIndex, size).forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

        Map<DBPediaResource, Integer> topWords = new LinkedHashMap<>();
        sortedMap.forEach((s, integer) -> topWords.put(frequentResources.get(s), integer));

        return topWords;
    }

    /**
     * @param n The number of relevant words desired. Pass 0 to get all words
     * @return A List of most relevant de.bonn.eis.model.DBPediaResource objects
     */
    public List<DBPediaResource> getMostRelevantWords(int n) {
        Set<DBPediaResource> noDupeResources = new LinkedHashSet<>(dbPediaResources);
        dbPediaResources.clear();
        dbPediaResources.addAll(noDupeResources);
        Comparator<DBPediaResource> comparator = Comparator.comparing(o -> Double.valueOf(o.getPercentageOfSecondRank()));
        Collections.sort(dbPediaResources, comparator);
        int size = dbPediaResources.size();
        int firstIndex = 0;
        if (n > 0 && n < size) {
            firstIndex = size - n;
        }
        return dbPediaResources.subList(firstIndex, size);
    }

    /**
     * @param dbPediaResources List of resources returned by DBPediaSpotlightResult
     * @return An ImmutableListMultimap of lists of DBPediaResources grouped by their types
     */
    public static ImmutableListMultimap<String, DBPediaResource> groupResourcesByType(List<DBPediaResource> dbPediaResources) {
        Function<DBPediaResource, String> typesFunction = DBPediaResource::getTypes;
        return Multimaps.index(dbPediaResources, typesFunction::apply);
    }

    public List<DBPediaResource> getDbPediaResources() {
        return dbPediaResources;
    }
}
