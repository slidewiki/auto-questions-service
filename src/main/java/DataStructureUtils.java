import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by andy on 1/2/17.
 */
public class DataStructureUtils {

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
}
