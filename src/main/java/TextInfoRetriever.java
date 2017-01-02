import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */
public class TextInfoRetriever {

    public Map<DBPediaResource, Integer> getFrequentWords(String text) throws FileNotFoundException, UnsupportedEncodingException {
        DBPediaSpotlightClient dbPediaSpotlightClient = new DBPediaSpotlightClient();
        dbPediaSpotlightClient.init();
        DBPediaSpotlightPOJO response = dbPediaSpotlightClient.annotatePost(text);
        Map<String, Integer> wordFrequencyPairs = new LinkedHashMap<>();
        Map<String, DBPediaResource> frequentResources = new LinkedHashMap<>();
        List<DBPediaResource> dbPediaResources = response.getDBPediaResources();

        // get frequency of each unique word
        for (DBPediaResource dbPediaResource : dbPediaResources) {
            if (Double.parseDouble(dbPediaResource.getPercentageOfSecondRank()) < 0.5) {
                int frequency = wordFrequencyPairs.getOrDefault(dbPediaResource.getSurfaceForm(), 0) + 1;
                wordFrequencyPairs.put(dbPediaResource.getSurfaceForm(), frequency);
                frequentResources.putIfAbsent(dbPediaResource.getSurfaceForm(), dbPediaResource);
            }
        }

        Map<String, Integer> sortedMap = DataStructureUtils.sortMap(wordFrequencyPairs);
        List<Map.Entry<String,Integer>> entryList = new ArrayList<>(sortedMap.entrySet());
        int size = entryList.size();
        sortedMap.clear();
        entryList.subList(size - 20, size - 1).forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

        Map<DBPediaResource, Integer> topWords = new LinkedHashMap<>();
        sortedMap.forEach((s, integer) -> topWords.put(frequentResources.get(s), integer));

        return topWords;
    }
}
