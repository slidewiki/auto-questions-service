import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */
public class TextInfoRetriever {

    private List<DBPediaResource> dbPediaResources;

    public TextInfoRetriever(String text) {
        DBPediaSpotlightClient dbPediaSpotlightClient = new DBPediaSpotlightClient();
        dbPediaSpotlightClient.init();
        DBPediaSpotlightPOJO response = dbPediaSpotlightClient.annotatePost(text);
        dbPediaResources = response.getDBPediaResources();
    }

    /**
     *
     * @param n Number of frequent words desired. Pass 0 to get all words
     * @return A Map of most frequent DBPediaResource objects and their count in Integer
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public Map<DBPediaResource, Integer> getFrequentWords(int n) throws FileNotFoundException, UnsupportedEncodingException {

        Map<String, Integer> wordFrequencyPairs = new LinkedHashMap<>();
        Map<String, DBPediaResource> frequentResources = new LinkedHashMap<>();

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
        int firstIndex = 0;
        if(n > 0 && n < size) {
            firstIndex = size - n;
        }
        entryList.subList(firstIndex, size - 1).forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));

        Map<DBPediaResource, Integer> topWords = new LinkedHashMap<>();
        sortedMap.forEach((s, integer) -> topWords.put(frequentResources.get(s), integer));

        return topWords;
    }

    public List<DBPediaResource> getMostRelevantWords(int n) {
        Set<DBPediaResource> noDupeResources = new LinkedHashSet<>(dbPediaResources);
        dbPediaResources.clear();
        dbPediaResources.addAll(noDupeResources);
        Comparator<DBPediaResource> comparator = Comparator.comparing(o -> Double.valueOf(o.getPercentageOfSecondRank()));
        Collections.sort(dbPediaResources, comparator);
        int size = dbPediaResources.size();
        int firstIndex = 0;
        if(n > 0 && n < size) {
            firstIndex = size - n;
        }
        return dbPediaResources.subList(firstIndex, size - 1);
    }

    public List<String> getDistractors(DBPediaResource resource) {
        DistractorGenerator generator = new DistractorGenerator();
        List<String> distractors = new ArrayList<>();
        List<String> inTextDistractors = generator.getInTextDistractors(resource, dbPediaResources);
        List<String> externalDistractors = generator.getExternalDistractors(resource);

        if(inTextDistractors != null && !inTextDistractors.isEmpty()){
            distractors.add("\nIn text distractors\n");
            distractors.addAll(inTextDistractors);
        }
        if(externalDistractors != null && !externalDistractors.isEmpty()){
            distractors.add("\nExternal distractors\n");
            distractors.addAll(externalDistractors);
        }
        return distractors;
    }

    public List<DBPediaResource> getDbPediaResources() {
        return dbPediaResources;
    }
}
