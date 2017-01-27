import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/2/17.
 */
public class QGenUtils {

    public static Map<String, Integer> sortMap(Map<String, Integer> map) {
        return map.entrySet().stream().
                sorted(Map.Entry.comparingByValue()).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static List<DBPediaResource> removeDuplicatesFromList(List<DBPediaResource> list) {
        Map<String, DBPediaResource> map = new LinkedHashMap<>();
        for (DBPediaResource obj : list) {
            map.put(obj.getURI(), obj);
        }
        list.clear();
        list.addAll(map.values());
        return list;
    }

    public static List<String> getRandomItemsFromList (List<String> list, int n){
        List<String> randomItems = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < n && i < size; i++) {
            String random = list.get(ThreadLocalRandom.current().nextInt(0, size));
            randomItems.add(random);
        }
        return randomItems;
    }
}
