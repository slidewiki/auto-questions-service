package de.bonn.eis.controller;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.LinkSUMResultRow;
import de.bonn.eis.model.WhoAmIQuestion;
import de.bonn.eis.utils.NLPConsts;
import de.bonn.eis.utils.QGenLogger;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import rita.RiTa;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private static final String DBPEDIA_URL = "http://dbpedia.org/";
    private static final String DBPEDIA_LIVE_SPARQL_SERVICE = "http://dbpedia-live.openlinksw.com/sparql/";
    private static final String DBPEDIA_SPARQL_SERVICE = DBPEDIA_URL + "sparql/";
    private static final String WORDNET_SPARQL_SERVICE = "http://wordnet-rdf.princeton.edu/sparql/";
    private static final int QUERY_LIMIT = 10;
    private static final String PREFIX_RDF = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";
    private static final String PREFIX_FOAF = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";
    private static final String PREFIX_RDFS = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";
    private static final String PREFIX_WIKIDATA = "PREFIX wikidata: <http://www.wikidata.org/entity/>\n";
    private static final String PREFIX_OWL = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n";
    private static final String TIMEOUT_VALUE = "100000";
    private static final String UNION = "UNION\n";
    private static final String MINUS = "MINUS\n";
    private static final int ALLOWED_DEPTH_FOR_MEDIUM = 6;
    //    private static final int ALLOWED_DEPTH_FOR_HARD = 12;
    private static final String DBPEDIA = "dbpedia";
    private static final int NO_OF_SPECIFIC_TYPES = 10;
    private static final String NNP_POS_TAG = "nnp";
    private static final int NO_OF_SPECIFIC_TYPES_EASY = 3;
    private static final String WIKICAT = "wikicat";
    private static final String YAGO = "yago";
    private static final String OWL_PERSON = "http://dbpedia.org/ontology/Person";
    private static final String DBPEDIA_CLASS_YAGO_WIKICAT = "http://dbpedia.org/class/yago/Wikicat";
    private static final String PREFIX_VRANK = "PREFIX vrank:<http://purl.org/voc/vrank#>";
    private static final String PREFIX_DBPEDIA_ONTOLOGY = "PREFIX dbp-ont:<http://dbpedia.org/ontology/>";
    private static final String PREFIX_DBPEDIA_PROPERTY = "PREFIX dbp-prop:<http://dbpedia.org/property/>";
    private static final String DBPEDIA_PAGE_RANK = "http://people.aifb.kit.edu/ath/#DBpedia_PageRank";
    private final String PREFIX_DBRES = "PREFIX dbres: <" + DBPEDIA_URL + "ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";
    private final String PREFIX_DUL = "PREFIX dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl>\n";

    // TODO Use QueryBuilder
    // http://stackoverflow.com/questions/7250189/how-to-build-sparql-queries-in-java
    public List<String> getSimilarResourceNames(DBPediaResource resource, String level) {
        List<String> resourceNames = new ArrayList<>();
        List<String> resourceTypeList = null;

        if (level.equals(NLPConsts.LEVEL_EASY)) {
            resourceNames = getDistractorsFromWordnet(resource.getSurfaceForm()); // surface form as in text or label of the resource disambiguated?
            if (resourceNames.size() > 3) {
                return resourceNames;
            } else {
                resourceNames.clear();
            }
            String[] types = resource.getTypes().split(",");
            resourceTypeList = Arrays.asList(types[types.length - 1]);
        } else if (level.equals(NLPConsts.LEVEL_MEDIUM) || level.equals(NLPConsts.LEVEL_HARD)) {
            resourceTypeList = getResourceTypes(resource);
            if (resourceTypeList != null && !resourceTypeList.isEmpty()) {
                if (resourceTypeList.size() == 1) {
                    String type = resourceTypeList.get(0);
                    if (type.contains("owl:Thing") || type.contains("owl#Thing")) {
                        return null;
                    }
                }
                QGenLogger.info("Resource: " + resource.getSurfaceForm());
//                int maxAllowedDepth = ALLOWED_DEPTH_FOR_HARD;
                int maxAllowedDepth = -1;
                if (level.equals(NLPConsts.LEVEL_MEDIUM)) {
                    maxAllowedDepth = ALLOWED_DEPTH_FOR_MEDIUM;
                }
                resourceTypeList = getMostSpecificTypes(resourceTypeList, level);
            }
        }

        String queryString =
                PREFIX_RDF + PREFIX_FOAF + PREFIX_RDFS +
//                        "SELECT DISTINCT ?name (COUNT(*) AS ?count) FROM <http://dbpedia.org> WHERE {\n" +
                        "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                        "{ ?s foaf:name ?name }\n" + UNION +
                        "{ ?s rdfs:label ?name }\n";


        if (resourceTypeList == null || resourceTypeList.isEmpty()) {
            return null;
        }

        queryString = addTriplePatternsAndUnions(resourceTypeList, queryString);
//        queryString = addTriplePatternsAndMinus(resourceTypeList, queryString);
        queryString += "FILTER (langMatches(lang(?name), \"EN\")) .";
        queryString += "}\nORDER BY RAND()\nLIMIT " + QUERY_LIMIT; // add bind(rand(1 + strlen(str(?s))*0) as ?rid) for randomization
//        queryString += "}\nGROUP BY ?name\nORDER BY DESC(?count)\nLIMIT " + QUERY_LIMIT; // add bind(rand(1 + strlen(str(?s))*0) as ?rid) for randomization
        // TODO Refine Query results

        ResultSet results = null;
        try {
            QGenLogger.fine("###########################RESOURCE: " + resource.getSurfaceForm() + "###########################\n" + "SELECT Query:\n" + queryString);
            results = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
        } catch (Exception e) {
            QGenLogger.severe("Exception in SELECT\n" + queryString + "\n" + e.getMessage());
        }
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution result = results.next();
                RDFNode n = result.get("name");
                String nameLiteral;
                if (n.isLiteral()) {
                    nameLiteral = ((Literal) n).getLexicalForm();
                    resourceNames.add(nameLiteral);
                }
            }
        }
        return resourceNames;
    }

    private List<String> getDistractorsFromWordnet(String resourceName) {
        List<String> distractors = new ArrayList<>();
        List<String> lexicalDomains = getWordnetLexicalDomains(resourceName);
        if (lexicalDomains.isEmpty()) {
            // wordnet sometimes gives results for last names (in the case of people)
            int index = resourceName.trim().lastIndexOf(" ");
            if (index > 0) {
                resourceName = resourceName.substring(index).trim();
                String[] tags = RiTa.getPosTags(resourceName);
                for (String tag : tags) {
                    if (tag.equals(NNP_POS_TAG)) {
                        lexicalDomains = getWordnetLexicalDomains(resourceName);
                        break;
                    }
                }
            }
        }
        for (String lexicalDomain : lexicalDomains) {
            distractors.addAll(getWordnetHypernyms(resourceName, lexicalDomain));
        }
        return distractors;
    }

    private List<String> getWordnetHypernyms(String resourceName, String lexicalDomain) {
        String queryString = "PREFIX lemon: <http://lemon-model.net/lemon#> \n" +
                "PREFIX wordnet: <http://wordnet-rdf.princeton.edu/> \n" +
                "PREFIX wordnet-ontology: <http://wordnet-rdf.princeton.edu/ontology#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "\n" +
                "select ?name where {\n" +
                " ?s rdfs:label \"" + resourceName + "\"@eng .\n" +
                " ?s wordnet-ontology:lexical_domain <" + lexicalDomain + "> ." +
                " {\n" +
                " ?s wordnet-ontology:instance_hypernym ?p .\n" +
                " ?res wordnet-ontology:instance_hypernym ?p .\n" +
                " ?res rdfs:label ?name .\n" +
                " }\n" +
                " UNION\n" +
                " {\n" +
                "  ?s wordnet-ontology:hypernym ?p .\n" +
                "  ?res wordnet-ontology:hypernym ?p .\n" +
                " ?res rdfs:label ?name .\n" +
                " }\n" +
                " FILTER (?s != ?res)\n" +
                "} ORDER BY RAND() LIMIT 4";
        ResultSet results = null;
        List<String> resources = new ArrayList<>();
        try {
            results = runSelectQuery(queryString, WORDNET_SPARQL_SERVICE);
        } catch (Exception e) {
            QGenLogger.severe("Exception in SELECT\n" + queryString + "\n" + e.getMessage());
        }
        addResultsToList(results, resources, "name");
        return resources;
    }

    private void addResultsToList(ResultSet results, List<String> resources, String var) {
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution result = results.next();
                if (result != null) {
                    RDFNode n = result.get(var);
                    String nameLiteral;
                    if (n.isLiteral()) {
                        nameLiteral = ((Literal) n).getLexicalForm();
                        resources.add(nameLiteral);
                    }
                }
            }
        }
    }

    private List<String> getWordnetLexicalDomains(String resourceName) {
        String queryString = "PREFIX lemon: <http://lemon-model.net/lemon#> \n" +
                "PREFIX wordnet: <http://wordnet-rdf.princeton.edu/> \n" +
                "PREFIX wordnet-ontology: <http://wordnet-rdf.princeton.edu/ontology#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "\n" +
                "select distinct ?d where {\n" +
                " ?s rdfs:label \"" + resourceName + "\"@eng .\n" +
                " ?s wordnet-ontology:lexical_domain ?d .\n" +
                " filter( regex(str(?d), \"noun\" ))\n" +
                "} ORDER BY DESC (?d)";
        ResultSet results = null;
        List<String> domains = new ArrayList<>();
        try {
            results = runSelectQuery(queryString, WORDNET_SPARQL_SERVICE);
        } catch (Exception e) {
            QGenLogger.severe("Exception in SELECT\n" + queryString + "\n" + e.getMessage());
        }
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution result = results.next();
                if (result != null) {
                    RDFNode node = result.get("d");
                    domains.add(node.toString());
                }
            }
        }
        return domains;
    }

    private String addTriplePatternsAndUnions(List<String> resourceTypeList, String queryString) {
        int numberOfTypes = resourceTypeList.size();
        int groupSize = getGroupSize(numberOfTypes);
        int count = 0;
        boolean curlyBraceOpened = false;
//        boolean groupIsOdd = true;
        for (int i = 0; i < numberOfTypes; i++) {
            if (groupSize > 0 && count == 0) {
                queryString += "{\n";
                curlyBraceOpened = true;
//                if(groupIsOdd){
//                    queryString += "?res ?p ?s .\n";
//                } else {
//                    queryString += "?s ?p ?res .\n";
//                }
//                groupIsOdd = !groupIsOdd;
            }
            String nsAndType = resourceTypeList.get(i);
            queryString = addNsAndType(queryString, nsAndType, true);
            if (groupSize > 0) {
                count++;
                if (groupSize == count) {
                    queryString += "}\n";
                    curlyBraceOpened = false;
                    if (i < numberOfTypes - 1) {
                        queryString += UNION;
                    }
//                    else if( i == numberOfTypes - 2){
//                        queryString += MINUS;
//                    }
                    count = 0;
                }
            }
        }
        if (curlyBraceOpened) {
            queryString += "}\n";
        }
        return queryString;
    }

    private String addNsAndType(String queryString, String nsAndType, boolean shouldAddTriple) {
        if (nsAndType.contains("Http://") || nsAndType.contains("http://")) { // TODO Something more flexible?
            nsAndType = nsAndType.replace("Http://", "http://");
            nsAndType = "<" + nsAndType + ">";
        } else {
            if (nsAndType.indexOf(":") > 0) {
                String[] nsTypePair = nsAndType.split(":");
                String namespace = nsTypePair[0].trim();
                String type = nsTypePair[1];
                nsAndType = getNamespace(namespace) + ":" + type;
                queryString = addPrefix(queryString, namespace);
            } else if (nsAndType.contains("@")) {
                nsAndType = nsAndType.substring(0, nsAndType.indexOf("@")).trim();
            }
        }
        if (shouldAddTriple) {
            queryString += "?s a " + nsAndType + " .\n";
        }
        return queryString;
    }

    private String addTriplePatternsAndMinus(List<String> resourceTypeList, String queryString) {
        int size = resourceTypeList.size();
        for (int i = 0; i < size; i++) {
            if (i < size - 2) {
                queryString = addNsAndType(queryString, resourceTypeList.get(i), true);
            } else {
                queryString += MINUS + "{\n";
                queryString = addNsAndType(queryString, resourceTypeList.get(i), true);
                queryString += "}\n";
            }
        }
        return queryString;
    }

    private int getGroupSize(int sizeOfList) {
        int maxGroupSize = 3;
        if (sizeOfList <= maxGroupSize) {
            return sizeOfList - 1;
        }
        return maxGroupSize;
    }

    //TODO Query builder
    private List<String> getResourceTypes(DBPediaResource resource) {

        List<String> resourceTypes = new ArrayList<>();
        String uri = "<" + resource.getURI() + ">";
        String queryString = PREFIX_RDF + "SELECT ?o WHERE {\n" +
                uri + " a ?o .\n" +
                "}";

        ResultSet results = null;
        try {
            results = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
        } catch (Exception e) {
            QGenLogger.severe("Exception in SELECT\n" + queryString + "\n" + e.getMessage());
        }
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution result = results.next();
                if (result != null) {
                    RDFNode node = result.get("o");
                    resourceTypes.add(node.toString());
                }
            }
        }
        return resourceTypes;
    }

    private ResultSet runSelectQuery(String queryString, String service) throws Exception {
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(service, queryString);
        qExec.addDefaultGraph(DBPEDIA_URL);
//        qExec.addParam("timeout", TIMEOUT_VALUE); //1 sec
        ResultSet set = null;
        try {
            set = qExec.execSelect();
            set = ResultSetFactory.copyResults(set);
        } catch (Exception e) {
            QGenLogger.severe("Exception in SELECT\n" + queryString + "\n" + e.getMessage());
        } finally {
            qExec.close();
        }
        return set;
    }

    /**
     * Get the most specific rdf:type i.e. the one that has the most number of super classes
     *
     * @param types
     * @param level
     * @return
     */
    private List<String> getMostSpecificTypes(List<String> types, String level) {

        if (types.isEmpty()) {
            return null;
        } else if (types.size() == 1) {
            return types;
        }

        List<String> mostSpecificTypes = new ArrayList<>();
        Function<String, Integer> typeDepthFunction = this::getTypeDepth;

        ImmutableListMultimap<Integer, String> map = Multimaps.index(types, typeDepthFunction::apply);
        ImmutableSet<Integer> keySet = map.keySet();
        Integer maxDepth = Collections.max(keySet); // as it is for easy

        if (level.equalsIgnoreCase(NLPConsts.LEVEL_MEDIUM)) {
            maxDepth = ALLOWED_DEPTH_FOR_MEDIUM;
        } else if (level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)) {
            maxDepth--; // the second most specific
        }

        int noOfTypes = NO_OF_SPECIFIC_TYPES;
        if (level.equalsIgnoreCase(NLPConsts.LEVEL_EASY)) {
            noOfTypes = NO_OF_SPECIFIC_TYPES_EASY;
        }

        while (mostSpecificTypes.size() < noOfTypes) {
            mostSpecificTypes.addAll(0, map.get(maxDepth));
            maxDepth--;
        }
        if (mostSpecificTypes.size() > NO_OF_SPECIFIC_TYPES) {
            mostSpecificTypes = mostSpecificTypes.subList(0, NO_OF_SPECIFIC_TYPES + 1);
        }

        // If env is dev
        for (Integer key : keySet) {
            System.out.println(key);
            System.out.println(map.get(key));
        }
        return mostSpecificTypes;
    }

    private int getTypeDepth(String type) {
        String typeInLowerCase = type.toLowerCase();
        if (!typeInLowerCase.contains(DBPEDIA)) {
            return 0;
        }
//        if(typeInLowerCase.contains("yago") && !typeInLowerCase.contains("wikicat")){
//            return 0;
//        }
        String queryString =
                PREFIX_RDF + PREFIX_FOAF + PREFIX_RDFS +
                        "SELECT DISTINCT ?path FROM <http://dbpedia.org> WHERE {\n" +
                        "<" + type + "> rdfs:subClassOf* ?path . }";

        ResultSet results;
        int count = 0;
        try {
            results = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
            while (results.hasNext()) {
                results.next();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private String addPrefix(String queryString, String namespace) {
        String prefix = "";
        if (namespace.equalsIgnoreCase("dbpedia")) {
            prefix = PREFIX_DBRES;
        } else if (namespace.equalsIgnoreCase("schema")) {
            prefix = PREFIX_SCHEMA;
        } else if (namespace.equalsIgnoreCase("dul")) {
            prefix = PREFIX_DUL;
        } else if (namespace.equalsIgnoreCase("wikidata")) {
            prefix = PREFIX_WIKIDATA;
        }
        if (!queryString.contains(prefix)) {
            queryString = prefix + queryString;
        }
        return queryString;
    }

    // TODO
    private String getNamespace(String ns) {
        switch (ns) {
            case "DBpedia":
                return "dbres";
            case "Schema":
                return "schema";
            case "DUL":
                return "dul";
            case "Wikidata":
                return "wikidata";
        }
        return "";
    }

    public List<String> getSisterTypes(DBPediaResource answer, String level) {
        List<String> sisterTypes = new ArrayList<>();
        if (level.equalsIgnoreCase(NLPConsts.LEVEL_EASY)) {
            String types = answer.getTypes();
            if (!types.isEmpty()) {
                String[] typesArray = types.split(",");
                List<String> resourceTypeList = Arrays.asList(typesArray);
                resourceTypeList = resourceTypeList.stream().filter(type -> type.toLowerCase().contains(DBPEDIA)).collect(Collectors.toList());
                if (resourceTypeList != null) {
                    int size = resourceTypeList.size();
                    sisterTypes.add(getLabelOfSpotlightType(resourceTypeList.get(size - 1)));
                    for (int i = size - 1; i > 0; i--) {
                        String type = resourceTypeList.get(i);
                        String superType = resourceTypeList.get(i - 1);
                        String queryString =
                                PREFIX_RDF + PREFIX_RDFS + PREFIX_OWL;
                        queryString = addNsAndType(queryString, type, false);
                        queryString = addNsAndType(queryString, superType, false);
                        type = getNsAndTypeForSpotlightType(type);
                        superType = getNsAndTypeForSpotlightType(superType);

                        queryString += "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                                //                            "<" + answer.getURI() + "> a " + type + " .\n" +
                                " ?t a owl:Class .\n" +
                                " ?t rdfs:subClassOf " + superType + " .\n" +
                                " ?t rdfs:label ?name .\n" +
                                " filter (?t != " + type + ") .\n" +
                                " filter (langMatches(lang(?name), \"EN\")) ." +
                                " filter not exists {<" + answer.getURI() + "> a ?t } ." +
                                "} order by rand() limit 3";
                        ResultSet results = null;
                        try {
                            results = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        addResultsToList(results, sisterTypes, "name");
                        if (sisterTypes.size() == 4) {
                            break;
                        } else if (sisterTypes.size() > 4) {
                            sisterTypes = sisterTypes.subList(0, 4);
                        }
                    }
                }
            } else {
                String queryString = PREFIX_RDFS + PREFIX_OWL;
                String resource = "<" + answer.getURI() + ">";
                queryString += "SELECT DISTINCT ?dName ?aName WHERE {\n" +
                        resource + " a ?t .\n" +
                        resource + " a ?a .\n" +
                        " ?t a owl:Class .\n" +
                        " ?a a owl:Class .\n" +
                        " ?d a owl:Class .\n" +
                        " ?d rdfs:label ?dName .\n" +
                        " ?a rdfs:label ?aName .\n" +
                        " ?d rdfs:subClassOf ?t .\n" +
                        " filter (langMatches(lang(?aName), \"EN\")) .\n" +
                        " filter (langMatches(lang(?dName), \"EN\")) .\n" +
                        " filter not exists{?s rdfs:subClassOf ?a}\n" +
                        " filter not exists{" + resource + " a ?d .}\n" +
                        "} order by rand() limit 3";

                ResultSet resultSet = null;
                try {
                    resultSet = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RDFNode answerType = null;
                if (resultSet != null) {
                    while (resultSet.hasNext()) {
                        QuerySolution result = resultSet.next();
                        if (result != null) {
                            answerType = result.get("aName");
                            RDFNode distractor = result.get("dName");
                            String literal = getLiteral(distractor);
                            if (literal != null) {
                                sisterTypes.add(literal);
                            }
                        }
                    }
                    String literal = getLiteral(answerType);
                    if (literal != null) {
                        sisterTypes.add(0, literal);
                    }
                }
            }
        } else if (level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)) {
            List<String> typeStrings = getNMostSpecificTypes(answer.getURI(), 10, false);
            //TODO Decide whether to use many types = distractors are of different types
            //TODO one type = distractors are of the same type e.g. all are rivers
//            List<String> typeStrings = getNMostUniqueTypes(resource, 10);
            if(typeStrings == null || typeStrings.isEmpty()){
                return null;
            }
            String queryString;
            ResultSet resultSet;
            queryString = PREFIX_RDFS + PREFIX_FOAF;
            queryString += "SELECT DISTINCT ?dName ?aName ?d FROM <http://dbpedia.org> WHERE {\n";
            for (String typeString : typeStrings) {
                queryString += "{\n <" + typeString + "> rdfs:subClassOf ?st .\n" +
                        " optional { <" + typeString + "> rdfs:label ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                        " optional { <" + typeString + "> foaf:name ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                        "}\n";
                if (typeStrings.indexOf(typeString) != typeStrings.size() - 1) {
                    queryString += UNION;
                }
            }

            queryString += " ?d rdfs:subClassOf ?st .\n" +
                    " optional {?d rdfs:label ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                    " optional {?d foaf:name ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                    " filter not exists{<" + answer.getURI() + "> a ?d .}\n" +
                    "} order by rand() limit 3";

            resultSet = null;
            try {
                resultSet = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            RDFNode answerType = null;
            if (resultSet != null) {
                while (resultSet.hasNext()) {
                    QuerySolution result = resultSet.next();
                    if (result != null) {
                        answerType = result.get("aName");
                        RDFNode distractor = result.get("dName");
                        String literal = getLiteral(distractor);
                        if (literal != null) {
                            sisterTypes.add(literal);
                        } else {
                            RDFNode distractorNode = result.get("d");
                            String distractorString = distractorNode.toString();
                            sisterTypes.add(getWikicatYAGOTypeName(distractorString));
                        }
                    }
                }
                String literal = getLiteral(answerType);
                if (literal != null) {
                    sisterTypes.add(0, literal);
                } else {
                    int randomIndex = ThreadLocalRandom.current().nextInt(typeStrings.size());
                    sisterTypes.add(0, getWikicatYAGOTypeName(typeStrings.get(randomIndex)));
                }
            }
        }
        ArrayList<String> singleTypes = new ArrayList<>();
        for (String sisterType : sisterTypes) {
            String[] typeArray = sisterType.split(" ");
            StringBuilder result = new StringBuilder();
            for (String s : typeArray) {
                String singular = RiTa.singularize(s);
                String plural = RiTa.pluralize(singular);
                if (plural.equalsIgnoreCase(s)) {
                    result.append(singular).append(" ");
                } else {
                    result.append(s).append(" ");
                }
            }
            singleTypes.add(result.toString().trim());
        }
        return singleTypes;
    }

    private List<String> getNMostUniqueTypes(String resourceURI, int n, boolean owlClassOnly) {
        String queryString = PREFIX_RDFS;
        String variableName = "category";
        resourceURI = "<" + resourceURI + ">";
        queryString += "SELECT ?" + variableName + " (COUNT(?member) as ?memberCount) FROM <http://dbpedia.org> WHERE {\n" +
                "?member a ?" + variableName + ".\n" +
                "{ SELECT ?" + variableName + " WHERE { " + resourceURI + " a ?" + variableName + ". ";
        if(owlClassOnly){
            queryString = PREFIX_OWL + queryString;
            queryString += "?" + variableName + " a owl:Class .\n";
        } else{
            queryString += "FILTER (strstarts(str(?" + variableName + "), \"" + DBPEDIA_URL + "\"))";
        }
        queryString += "} }\n" +
                "}\n" +
                "group by (?" + variableName + ") ORDER BY ?memberCount \n";
        if(n != -1){
            queryString += "limit " + n + "\n";
        }

        ResultSet resultSet = null;
        try {
            resultSet = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResultSetAsStringList(resultSet, variableName);
    }

    private List<String> getNMostSpecificTypes(String resourceURI, int n, boolean owlClassOnly) {
        String queryString = PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";

        queryString += "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) FROM <http://dbpedia.org> WHERE {\n" +
                " {\n" +
                " select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n";
        if(owlClassOnly){
            queryString = PREFIX_OWL + queryString;
            queryString += "?" + variableName + " a owl:Class .\n";
        } else{
            queryString += "FILTER (strstarts(str(?" + variableName + "), \"" + DBPEDIA_URL + "\"))";
        }
        queryString += " }\n" +
        " }\n" +
        " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
        " } group by (?" + variableName + ") order by desc (?count) limit " + n + "\n";

        ResultSet resultSet = null;
        try {
            resultSet = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResultSetAsStringList(resultSet, variableName);
    }

    private List<String> getNMostSpecificYAGOTypesForDepthLimit(String resourceURI, int n ){
        String queryString = PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";
        queryString += "SELECT ?" + variableName + " ?count WHERE {\n" +
                "{\n" +
                "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) WHERE {\n" +
                "  {\n" +
                "   select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n" +
                "   filter (strstarts(str(?" + variableName + "), \"" + DBPEDIA_URL + "\"))\n" +
                "   filter (!strstarts(str(?" + variableName + "), \"" + DBPEDIA_CLASS_YAGO_WIKICAT + "\"))\n" +
                "  }\n" +
                " }\n" +
                " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
                "} group by ?" + variableName + " order by desc  (?count)\n" +
                "}\n";
        if(n != -1){
            queryString += "filter (?count < 14 && ?count > 11)\n";
        }
        queryString += "} limit " + n;

//        ResultSet resultSet = null;
//        try {
//            resultSet = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(DBPEDIA_SPARQL_SERVICE , queryString);
        qExec.addDefaultGraph("http://dbpedia.org");
        ResultSet resultSet = null;
        try {
            resultSet = qExec.execSelect();
            resultSet = ResultSetFactory.copyResults(resultSet);
        } catch (Exception e) {
            // Report exception
        } finally {
            qExec.close();
        }
        return getResultSetAsStringList(resultSet, variableName);
    }

//    private String getMostSpecificType (String resourceURI, boolean owlClassOnly) {
//        return getNMostSpecificTypes(resourceURI, 1, owlClassOnly).get(0);
//    }

    private List<String> getResultSetAsStringList(ResultSet resultSet, String variableName) {
        List<String> typeStrings = new ArrayList<>();
        RDFNode specificType;
        if (resultSet != null) {
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.next();
                if (result != null) {
                    specificType = result.get(variableName);
                    typeStrings.add(specificType.toString());
                }
            }
        }
        return typeStrings;
    }

    private String getWikicatYAGOTypeName(String type) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean containsYago = type.toLowerCase().contains(YAGO);
        String[] typeArray = type.split("/");
        type = typeArray[typeArray.length - 1];
        // Split camel case or title case
        String[] array = type.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[0-9])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=[0-9])");
        int synsetIDLength = 8;
        String prefixOne = "1";
        for (int i = 0; i < array.length; i++) {
            String s = array[i];
            if (s.equalsIgnoreCase(WIKICAT)) {
                continue;
            }
            if (s.equalsIgnoreCase(YAGO)) {
                containsYago = true;
                continue;
            }
            if (containsYago && i == array.length - 1 &&
                    s.startsWith(prefixOne) && s.length() == synsetIDLength + 1) {
                continue;
            }
            stringBuilder.append(s).append(" ");
        }
        return stringBuilder.toString().trim();
    }

    private String getLiteral(RDFNode node) {
        String literal = null;
        if (node != null && node.isLiteral()) {
            literal = ((Literal) node).getLexicalForm();
        }
        return literal;
    }

    private String getLabelOfSpotlightType(String type) {
        String nsAndType = getNsAndTypeForSpotlightType(type);
        String queryString = PREFIX_RDFS + PREFIX_DBRES;
        queryString += "SELECT ?name FROM <http://dbpedia.org> WHERE {\n" +
                nsAndType + " rdfs:label ?name .\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> resultList = new ArrayList<>();
        addResultsToList(results, resultList, "name");
        return resultList.get(0);
    }

    private String getLabelOfType(String type) {
        type = "<" + type + ">";
        String queryString = PREFIX_RDFS + PREFIX_FOAF;
        String variable = "name";
        queryString += "SELECT ?" + variable + " FROM <http://dbpedia.org> WHERE {\n" +
                "{" + type + " rdfs:label ?name .}\n" +
                UNION +
                "{" + type + " foaf:name ?name .}\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = runSelectQuery(queryString, DBPEDIA_LIVE_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(results != null){
            if (results.hasNext()){
                QuerySolution result = results.next();
                return getLiteral(result.get(variable));
            }
        }
        return null;
    }

    private String getNsAndTypeForSpotlightType(String nsAndType) {
        String[] nsTypePair = nsAndType.split(":");
        String namespace = nsTypePair[0].trim();
        String type = nsTypePair[1];
        return getNamespace(namespace) + ":" + type;
    }

    private List<LinkSUMResultRow> getLinkSUMForResource(String resourceURI) {
        resourceURI = "<" + resourceURI + ">";
        String queryString = PREFIX_RDFS + PREFIX_VRANK + PREFIX_DBPEDIA_ONTOLOGY + PREFIX_DBPEDIA_PROPERTY +
                "SELECT distinct (SAMPLE(?slabel) AS ?sublabel) (SAMPLE (?plabel) AS ?predlabel) (SAMPLE(?olabel) AS ?oblabel) ?v \n" +
                "FROM <" + DBPEDIA_URL + "> \n" +
                "FROM <" + DBPEDIA_PAGE_RANK + "> \n" +
                "WHERE {\n" +
                "\t{\t" + resourceURI + " ?p ?o.\n" +
                "\t\tFILTER regex(str(?o),\"http://dbpedia.org/resource\",\"i\").\n" +
                "\t\tFILTER (?p != dbp-ont:wikiPageWikiLink && ?p != <http://purl.org/dc/terms/subject> " +
                "&& ?p != dbp-prop:wikiPageUsesTemplate && ?p != rdfs:seeAlso && ?p != <http://www.w3.org/2002/07/owl#differentFrom> " +
                "&& ?p != dbp-ont:wikiPageDisambiguates && ?p != dbp-ont:wikiPageRedirects && ?p != dbp-ont:wikiPageExternalLink).\n" +
                "\t\tOPTIONAL {?o rdfs:label ?olabel. FILTER langmatches( lang(?olabel), \"EN\" ). }.\n" +
                "\t\tOPTIONAL {?p rdfs:label ?plabel. FILTER langmatches( lang(?plabel), \"EN\" ).}.\n" +
                "\t\tOPTIONAL {" + resourceURI + " rdfs:label ?slabel. FILTER langmatches( lang(?slabel), \"EN\" ).}.\n" +
                "\t\tOPTIONAL {?o vrank:hasRank ?r. ?r vrank:rankValue ?v}.\n" +
                "\t} \n" +
                "UNION\n" +
                "\t{\t?s ?p " + resourceURI + ".\n" +
                "\t\tFILTER regex(str(?s),\"http://dbpedia.org/resource\",\"i\").\n" +
                "\t\tFILTER (?p != dbp-ont:wikiPageWikiLink && ?p != <http://purl.org/dc/terms/subject> " +
                "&& ?p != dbp-prop:wikiPageUsesTemplate && ?p != rdfs:seeAlso && ?p != <http://www.w3.org/2002/07/owl#differentFrom> " +
                "&& ?p != dbp-ont:wikiPageDisambiguates && ?p != dbp-ont:wikiPageRedirects && ?p != dbp-ont:wikiPageExternalLink).\n" +
                "\t\tOPTIONAL {?s rdfs:label ?slabel.   FILTER langmatches( lang(?slabel), \"EN\" ). }.\n" +
                "\t\tOPTIONAL {?p rdfs:label ?plabel.  FILTER langmatches( lang(?plabel), \"EN\" ).}.\n" +
                "\t\tOPTIONAL {" + resourceURI + " rdfs:label ?olabel. FILTER langmatches( lang(?olabel), \"EN\" ).}.\n" +
                "\t\tOPTIONAL {?s vrank:hasRank ?r. ?r vrank:rankValue ?v}.\n" +
                "\t}\n" +
                "} group by ?v order by desc (?v)";

//        ResultSet resultSet = null;
//        try {
//            resultSet = runSelectQuery(query, DBPEDIA_SPARQL_SERVICE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        String DBPEDIA_SPARQL_SERVICE = "http://dbpedia.org/sparql/";
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(DBPEDIA_SPARQL_SERVICE , queryString);
        qExec.addDefaultGraph("http://dbpedia.org");
        ResultSet resultSet = null;
        try {
            resultSet = qExec.execSelect();
            resultSet = ResultSetFactory.copyResults(resultSet);
        } catch (Exception e) {
            // Report exception
        } finally {
            qExec.close();
        }

//        if (resultSet != null) {
//            while (resultSet.hasNext()) {
//                QuerySolution result = resultSet.next();
//                if (result != null) {
//                    System.out.println(result);
//                }
//            }
//        }

        return getResultSetAsObjectList(resultSet);
    }

    private List<LinkSUMResultRow> getResultSetAsObjectList(ResultSet resultSet) {
        List<LinkSUMResultRow> rows = new ArrayList<>();
        String subLabel = "sublabel";
        String predLabel = "predlabel";
        String obLabel = "oblabel";
        String vRank = "v";

        if (resultSet != null) {
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.next();
                LinkSUMResultRow.LinkSUMResultRowBuilder builder = LinkSUMResultRow.builder();
                if (result != null) {
                    builder.subject(getLiteral(result.get(subLabel)))
                            .predicate(getLiteral(result.get(predLabel)))
                            .object(getLiteral(result.get(obLabel)));
                    RDFNode vRankNode = result.get(vRank);
                    if(vRankNode != null && vRankNode.isLiteral()){
                        Literal literal = (Literal) vRankNode;
                        builder.vRank(literal.getFloat());
                    }
                    rows.add(builder.build());
                }
            }
        }
        return rows;
    }

    public WhoAmIQuestion getWhoAmIQuestion(DBPediaResource resource, String level) {
        String uri = resource.getURI();
        List<String> mostSpecificTypes = getNMostSpecificTypes(uri, 1, true);
        String baseType = "";
        if(mostSpecificTypes != null && !mostSpecificTypes.isEmpty()){
            baseType = mostSpecificTypes.get(0);
        }
        if(baseType.isEmpty() || baseType.equalsIgnoreCase(OWL_PERSON)){
            List<String> yagoTypes = getNMostSpecificYAGOTypesForDepthLimit(uri, 10);
            if(!yagoTypes.isEmpty()){
                int randomIndex = ThreadLocalRandom.current().nextInt(yagoTypes.size());
                baseType = getWikicatYAGOTypeName(yagoTypes.get(randomIndex));
            }
        } else{
            baseType = getLabelOfType(baseType);
        }
        if(baseType == null || baseType.isEmpty()){
            return null;
        }

        WhoAmIQuestion.WhoAmIQuestionBuilder builder = WhoAmIQuestion.builder();
        String resourceName = resource.getSurfaceForm();

        builder.baseType(baseType);
        builder.answer(resourceName);

        List<LinkSUMResultRow> linkSUMResults = getLinkSUMForResource(uri);
        int randomIndex;
        LinkSUMResultRow linkSUMResultRow, secondLinkSUMResultRow;
        List<LinkSUMResultRow> tempResults = new ArrayList<>();
        if(!linkSUMResults.isEmpty()){
            linkSUMResults.forEach(resultRow -> {
                if (resultRow != null) {
                    if (resultRow.getObject() != null && resultRow.getSubject() != null
                            && resultRow.getPredicate() != null) {
                        if (!isIdentityRevealed(resultRow, resourceName)) {
                            tempResults.add(resultRow);
                        }
                    }
                }
            });
            linkSUMResults = tempResults;
            int size = linkSUMResults.size();
            if(level.equalsIgnoreCase(NLPConsts.LEVEL_EASY)){
                int bound = size <= 10 ? size : 10;
                randomIndex = ThreadLocalRandom.current().nextInt(bound);
                linkSUMResultRow = linkSUMResults.get(randomIndex);
                builder = getWhoAmIFromLinkSUMRow(builder, resourceName, linkSUMResultRow, 1);

                randomIndex = ThreadLocalRandom.current().nextInt(bound);
                secondLinkSUMResultRow = linkSUMResults.get(randomIndex);
                int i = bound;
                while (linkSUMResultRow == secondLinkSUMResultRow ||
                        linkSUMResultRow.getPredicate().equalsIgnoreCase(secondLinkSUMResultRow.getPredicate())
                        && i >= 0){
                    randomIndex = ThreadLocalRandom.current().nextInt(bound);
                    secondLinkSUMResultRow = linkSUMResults.get(randomIndex);
                    i--;
                }
                builder = getWhoAmIFromLinkSUMRow(builder, resourceName, secondLinkSUMResultRow, 2);
                return builder.build();

            } else if(level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)){
                linkSUMResultRow = getHardPropertyForWhoAmI(linkSUMResults);
                builder = getWhoAmIFromLinkSUMRow(builder, resourceName, linkSUMResultRow, 1);

                secondLinkSUMResultRow = getHardPropertyForWhoAmI(linkSUMResults);
                int i = size;
                while (linkSUMResultRow == secondLinkSUMResultRow ||
                        linkSUMResultRow.getPredicate().equalsIgnoreCase(secondLinkSUMResultRow.getPredicate())
                        && i >= 0){
                    secondLinkSUMResultRow = getHardPropertyForWhoAmI(linkSUMResults);
                    i--;
                }
                builder = getWhoAmIFromLinkSUMRow(builder, resourceName, secondLinkSUMResultRow, 2);
                return builder.build();
            }
        }
        return null;
    }

    private boolean isIdentityRevealed(LinkSUMResultRow linkSUMResultRow, String resourceName) {
        String[] nameParts = resourceName.split(" ");
        for (String namePart : nameParts) {
            String subject = linkSUMResultRow.getSubject();
            String object = linkSUMResultRow.getObject();
            if(subject.contains(namePart) && object.contains(namePart)){
                return true;
            }
        }
        return false;
    }

    private LinkSUMResultRow getHardPropertyForWhoAmI(List<LinkSUMResultRow> linkSUMResults) {
        LinkSUMResultRow linkSUMResultRow;
        float lowerBound = 0.2f;
        float upperBound = 0.5f;
        float maxVRank = linkSUMResults.get(0).getVRank();
        linkSUMResultRow = getLinkSUMResultRowForVRankRange(linkSUMResults, lowerBound, upperBound);
        float j = 1.0f;
        while (linkSUMResultRow == null && upperBound <= maxVRank){
            lowerBound = upperBound;
            upperBound += 0.2f * j;
            linkSUMResultRow = getLinkSUMResultRowForVRankRange(linkSUMResults, lowerBound, upperBound);
            j += 2.0f;
        }
        return linkSUMResultRow;
    }

    private LinkSUMResultRow getLinkSUMResultRowForVRankRange(List<LinkSUMResultRow> linkSUMResults, float lowerBound, float upperBound) {
        int randomIndex;
        int size = linkSUMResults.size();
        LinkSUMResultRow linkSUMResultRow = null;
        for (int i = size - 1; i >= 0; i--) {
            randomIndex = ThreadLocalRandom.current().nextInt(size);
            linkSUMResultRow = linkSUMResults.get(randomIndex);
            if(linkSUMResultRow.getVRank() > lowerBound && linkSUMResultRow.getVRank() < upperBound){
                break;
            }
        }
        return linkSUMResultRow;
    }

    private WhoAmIQuestion.WhoAmIQuestionBuilder getWhoAmIFromLinkSUMRow(WhoAmIQuestion.WhoAmIQuestionBuilder builder, String resourceName, LinkSUMResultRow linkSUMResultRow, int propNo) {
        switch (propNo){
            case 1:
                if(resourceName.equalsIgnoreCase(linkSUMResultRow.getSubject())
                        || linkSUMResultRow.getSubject().contains(resourceName))
                {
                    builder.firstPredicate(linkSUMResultRow.getPredicate())
                            .firstObject(linkSUMResultRow.getObject());
                }
                else if(resourceName.equalsIgnoreCase(linkSUMResultRow.getObject())
                        || linkSUMResultRow.getObject().contains(resourceName))
                {
                    builder.firstPredicate(linkSUMResultRow.getPredicate())
                            .firstSubject(linkSUMResultRow.getSubject());
                }
                break;
            case 2:
                if(resourceName.equalsIgnoreCase(linkSUMResultRow.getSubject())
                        || linkSUMResultRow.getSubject().contains(resourceName))
                {
                    builder.secondPredicate(linkSUMResultRow.getPredicate())
                            .secondObject(linkSUMResultRow.getObject());
                } else if(resourceName.equalsIgnoreCase(linkSUMResultRow.getObject()))
                {
                    builder.secondPredicate(linkSUMResultRow.getPredicate())
                            .secondSubject(linkSUMResultRow.getSubject());
                }
        }
        return builder;
    }
}
