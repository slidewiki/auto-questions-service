package de.bonn.eis.utils;

import de.bonn.eis.model.DBPediaResource;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public static List<DBPediaResource> removeDuplicatesFromResourceList(List<DBPediaResource> list) {
        Map<String, DBPediaResource> map = new LinkedHashMap<>();
        for (DBPediaResource obj : list) {
            map.put(obj.getURI(), obj);
        }
        list.clear();
        list.addAll(map.values());
        return list;
    }

    public static void removeDuplicatesFromStringList(List<String> list) {
        Set<String> hashSet = new LinkedHashSet<>();
        hashSet.addAll(list);
        list.clear();
        list.addAll(hashSet);
    }

    public static List<String> getRandomItemsFromList(List<String> list, int n) {
        List<String> randomItems = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < n && i < size; i++) {
            String random = list.get(ThreadLocalRandom.current().nextInt(0, size));
            randomItems.add(random);
        }
        return randomItems;
    }

    public static boolean sourceHasWord(String source, String word) {
        String pattern = "\\b" + word + "\\b";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(source);
        return m.find();
    }

    public static <T> boolean listEqualsNoOrder(List<T> l1, List<T> l2) {
        final Set<T> s1 = new HashSet<>(l1);
        final Set<T> s2 = new HashSet<>(l2);

        return s1.equals(s2);
    }
}
