package de.bonn.eis.controller;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import de.bonn.eis.model.DBPediaResource;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private static final String DBPEDIA_SPARQL_SERVICE = "http://dbpedia.org/sparql";
    private static final String WORDNET_SPARQL_SERVICE = "http://wordnet-rdf.princeton.edu/sparql/";
    private static final int QUERY_LIMIT = 10;
    private static final String PREFIX_RDF = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";
    private static final String PREFIX_FOAF = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";
    private static final String PREFIX_RDFS = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";
    private static final String PREFIX_WIKIDATA = "PREFIX wikidata: <http://www.wikidata.org/entity/>\n";
    private static final String PREFIX_OWL = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n";
    private static final String TIMEOUT_VALUE = "10000";
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
    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
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
            results = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
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
            results = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
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
        qExec.addParam("timeout", TIMEOUT_VALUE); //1 sec
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
            results = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
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
                    sisterTypes.add(getLabelOfType(resourceTypeList.get(size - 1)));
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
                            results = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
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
                    resultSet = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
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
            String resource = "<" + answer.getURI() + ">";
            List<String> typeStrings = getNMostSpecificTypes(resource, 3);
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
                    " filter not exists{" + resource + " a ?d .}\n" +
                    "} order by rand() limit 3";

            System.out.println(queryString);

            resultSet = null;
            try {
                resultSet = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
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
                    int randomIndex = new Random().nextInt(typeStrings.size());
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

    private List<String> getNMostSpecificTypes(String resource, int n) {
        String queryString = PREFIX_RDFS;
        String variableName = "type";
        queryString += "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) FROM <http://dbpedia.org> WHERE {\n" +
                " {\n" +
                " select distinct ?" + variableName + " where {\n" +
                resource + " a ?" + variableName + " .\n" +
                " }\n" +
                " }\n" +
                " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
                " } group by (?" + variableName + ") order by desc (?count) limit " + n + "\n";

        ResultSet resultSet = null;
        try {
            resultSet = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResultSetAsStringList(resultSet, variableName);
    }

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
        String dNameLiteral = null;
        if (node != null && node.isLiteral()) {
            dNameLiteral = ((Literal) node).getLexicalForm();
        }
        return dNameLiteral;
    }

    private String getLabelOfType(String type) {
        String nsAndType = getNsAndTypeForSpotlightType(type);
        String queryString = PREFIX_RDFS + PREFIX_DBRES;
        queryString += "SELECT ?name FROM <http://dbpedia.org> WHERE {\n" +
                nsAndType + " rdfs:label ?name .\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> resultList = new ArrayList<>();
        addResultsToList(results, resultList, "name");
        return resultList.get(0);
    }

    private String getNsAndTypeForSpotlightType(String nsAndType) {
        String[] nsTypePair = nsAndType.split(":");
        String namespace = nsTypePair[0].trim();
        String type = nsTypePair[1];
        return getNamespace(namespace) + ":" + type;
    }
}
