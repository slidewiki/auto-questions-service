package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.LinkSUMResultRow;
import de.bonn.eis.model.WhoAmIQuestionStructure;
import de.bonn.eis.utils.NLPConsts;
import de.bonn.eis.utils.SPARQLConsts;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import rita.RiTa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private final String PREFIX_DBRES = "PREFIX dbres: <" + SPARQLConsts.DBPEDIA_ONTOLOGY + ">\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";
    private final String PREFIX_DUL = "PREFIX dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl>\n";

    // TODO Use QueryBuilder
    // http://stackoverflow.com/questions/7250189/how-to-build-sparql-queries-in-java
    List<String> getSimilarResourceNames(DBPediaResource resource, String level) {
        List<String> resourceNames = new ArrayList<>();

        if (level.equals(NLPConsts.LEVEL_EASY)) {
            String baseType = getBaseTypeForEasy(resource);
            return getNPopularDistractorsForBaseType(resource.getURI(), baseType, 3);
        }
        else if(level.equals(NLPConsts.LEVEL_HARD)) {
            List<String> types = getNMostSpecificTypes(resource.getURI(), 5, false);
            List<String> distractors = new ArrayList<>();
            for (String type: types) {
                distractors.addAll(getNPopularDistractorsForBaseType(resource.getURI(), type, 3));
                if(distractors.size() >= 3){
                    return distractors.subList(0, 3);
                }
            }
        }
        return resourceNames;
    }

    private String getBaseTypeForEasy(DBPediaResource resource) {
        String baseType = "";
        if(!resource.getTypes().isEmpty()) {
            baseType = getMostSpecificSpotlightType(resource.getTypes());
        }
        else {
            List<String> specificTypes = getNMostSpecificTypes(resource.getURI(), 1, true);
            if(!specificTypes.isEmpty()){
                baseType = specificTypes.get(0);
            }
        }
        if(baseType == null || baseType.isEmpty() || baseType.equalsIgnoreCase(SPARQLConsts.OWL_PERSON) || getTypeDepth(baseType) <= 3){
            List<String> yagoTypes = null;
            int lowerBound = 11;
            while((yagoTypes == null || yagoTypes.isEmpty()) && lowerBound > 3){
                yagoTypes = getNMostSpecificYAGOTypesForDepthRange(resource.getURI(), 10, lowerBound, lowerBound+3);
                if(lowerBound == 5){
                    lowerBound--;
                }
                else {
                    lowerBound -= 3;
                }
            }
            if(yagoTypes != null && !yagoTypes.isEmpty()){
                int randomIndex = ThreadLocalRandom.current().nextInt(yagoTypes.size());
                baseType = yagoTypes.get(randomIndex);
            }
        }
        return baseType;
    }

    private String getMostSpecificSpotlightType(String types) {
        String[] typeArray = types.split(",");
        for (int i = typeArray.length - 1; i >= 0; i--) {
            if (typeArray[i].toLowerCase().contains(SPARQLConsts.DBPEDIA)) {
                String ontologyName = typeArray[i].split(":")[1];
                return SPARQLConsts.DBPEDIA_ONTOLOGY + ontologyName;
            }
        }
        return null;
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

    private int getTypeDepth(String type) {
//        String typeInLowerCase = type.toLowerCase();
//        if (!typeInLowerCase.contains(DBPEDIA)) {
//            return 0;
//        }
//        if(typeInLowerCase.contains("yago") && !typeInLowerCase.contains("wikicat")){
//            return 0;
//        }
        String queryString =
                SPARQLConsts.PREFIX_RDF + SPARQLConsts.PREFIX_FOAF + SPARQLConsts.PREFIX_RDFS +
                        "SELECT DISTINCT ?path FROM <http://dbpedia.org> WHERE {\n" +
                        "<" + type + "> rdfs:subClassOf* ?path . }";

        ResultSet results;
        int count = 0;
        try {
            results = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
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
            prefix = SPARQLConsts.PREFIX_WIKIDATA;
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

    List<String> getSisterTypes(DBPediaResource answer, String level) {
        List<String> sisterTypes = new ArrayList<>();
        if (level.equalsIgnoreCase(NLPConsts.LEVEL_EASY)) {
            String types = answer.getTypes();
            if (!types.isEmpty()) {
                String[] typesArray = types.split(",");
                List<String> resourceTypeList = Arrays.asList(typesArray);
                resourceTypeList = resourceTypeList.stream().filter(type -> type.toLowerCase().contains(SPARQLConsts.DBPEDIA)).collect(Collectors.toList());
                if (resourceTypeList != null) {
                    int size = resourceTypeList.size();
                    sisterTypes.add(getLabelOfSpotlightType(resourceTypeList.get(size - 1)));
                    for (int i = size - 1; i > 0; i--) {
                        String type = resourceTypeList.get(i);
                        String superType = resourceTypeList.get(i - 1);
                        String queryString =
                                SPARQLConsts.PREFIX_RDF + SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_OWL;
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
                        ResultSet resultSet = null;
                        try {
                            resultSet = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        addResultsToList(resultSet, sisterTypes, "name");
                        if (sisterTypes.size() == 4) {
                            break;
                        } else if (sisterTypes.size() > 4) {
                            sisterTypes = sisterTypes.subList(0, 4);
                        }
                    }
                }
            } else {
                String queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_OWL;
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
                    resultSet = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
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
                            String literal = getStringLiteral(distractor);
                            if (literal != null) {
                                sisterTypes.add(literal);
                            }
                        }
                    }
                    String literal = getStringLiteral(answerType);
                    if (literal != null) {
                        sisterTypes.add(0, literal);
                    }
                }
            }
        } else if (level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)) {
            List<String> typeStrings = getNMostSpecificTypes(answer.getURI(), 10, false);
            //TODO Decide whether to use many types = distractors are of different types
            //TODO one type = distractors are of the same type e.g. all are rivers
            if(typeStrings == null || typeStrings.isEmpty()){
                return null;
            }
            String queryString;
            ResultSet resultSet;
            queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_FOAF;
            queryString += "SELECT DISTINCT ?dName ?aName ?d FROM <http://dbpedia.org> WHERE {\n";
            for (String typeString : typeStrings) {
                queryString += "{\n <" + typeString + "> rdfs:subClassOf ?st .\n" +
                        " optional { <" + typeString + "> rdfs:label ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                        " optional { <" + typeString + "> foaf:name ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                        "}\n";
                if (typeStrings.indexOf(typeString) != typeStrings.size() - 1) {
                    queryString += SPARQLConsts.UNION;
                }
            }

            queryString += " ?d rdfs:subClassOf ?st .\n" +
                    " optional {?d rdfs:label ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                    " optional {?d foaf:name ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                    " filter not exists{<" + answer.getURI() + "> a ?d .}\n" +
                    "} order by rand() limit 3";

            resultSet = null;
            try {
                resultSet = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
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
                        String literal = getStringLiteral(distractor);
                        if (literal != null) {
                            sisterTypes.add(literal);
                        } else {
                            RDFNode distractorNode = result.get("d");
                            String distractorString = distractorNode.toString();
                            sisterTypes.add(getWikicatYAGOTypeName(distractorString));
                        }
                    }
                }
                String literal = getStringLiteral(answerType);
                if (literal != null) {
                    sisterTypes.add(0, literal);
                } else {
                    int randomIndex = ThreadLocalRandom.current().nextInt(typeStrings.size());
                    sisterTypes.add(0, getWikicatYAGOTypeName(typeStrings.get(randomIndex)));
                }
            }
        }
        // Singularize plural types
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

    private List<String> getNMostSpecificTypes(String resourceURI, int n, boolean owlClassOnly) {
        String queryString = SPARQLConsts.PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";

        queryString += "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) FROM <http://dbpedia.org> WHERE {\n" +
                " {\n" +
                " select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n";
        if(owlClassOnly){
            queryString = SPARQLConsts.PREFIX_OWL + queryString;
            queryString += "?" + variableName + " a owl:Class .\n";
        } else{
            queryString += "FILTER (strstarts(str(?" + variableName + "), \"" + SPARQLConsts.DBPEDIA_URL + "\"))";
        }
        queryString += " }\n" +
        " }\n" +
        " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
        " } group by (?" + variableName + ") order by desc (?count) limit " + n + "\n";

        ResultSet resultSet = null;
        try {
            resultSet = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getResultSetAsStringList(resultSet, variableName, false);
    }

    private List<String> getNMostSpecificYAGOTypesForDepthRange(String resourceURI, int n, int depthLowerBound, int depthUpperBound ){
        String queryString = SPARQLConsts.PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";
        queryString += "SELECT ?" + variableName + " ?count WHERE {\n" +
                "{\n" +
                "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) WHERE {\n" +
                "  {\n" +
                "   select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n" +
                "   filter (strstarts(str(?" + variableName + "), \"" + SPARQLConsts.DBPEDIA_URL + "\"))\n" +
                "   filter (!strstarts(str(?" + variableName + "), \"" + SPARQLConsts.DBPEDIA_CLASS_YAGO_WIKICAT + "\"))\n" +
                "  }\n" +
                " }\n" +
                " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
                "} group by ?" + variableName + " order by desc  (?count)\n" +
                "}\n";
        if(n != -1){
            queryString += "filter (?count < " + depthUpperBound + " && ?count > " + depthLowerBound + ")\n";
        }
        queryString += "} limit " + n;

//        ResultSet resultSet = null;
//        try {
//            resultSet = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(SPARQLConsts.DBPEDIA_SPARQL_SERVICE , queryString);
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
        return getResultSetAsStringList(resultSet, variableName, false);
    }

    private List<String> getResultSetAsStringList(ResultSet resultSet, String variableName, boolean literalRequired) {
        List<String> resultStrings = new ArrayList<>();
        RDFNode node;
        if (resultSet != null) {
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.next();
                if (result != null) {
                    node = result.get(variableName);
                    if(literalRequired){
                        String literal = getStringLiteral(node);
                        if(literal != null){
                            resultStrings.add(literal);
                        }
                    } else {
                        if(node != null) {
                            resultStrings.add(node.toString());
                        }
                    }
                }
            }
        }
        return resultStrings;
    }

    private String getWikicatYAGOTypeName(String type) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean containsYago = type.toLowerCase().contains(SPARQLConsts.YAGO);
        String[] typeArray = type.split("/");
        type = typeArray[typeArray.length - 1];
        // Split camel case or title case
        String[] array = type.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[0-9])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=[0-9])");
        int synsetIDLength = 8;
        String prefixOne = "1";
        for (int i = 0; i < array.length; i++) {
            String s = array[i];
            if (s.equalsIgnoreCase(SPARQLConsts.WIKICAT)) {
                continue;
            }
            if (s.equalsIgnoreCase(SPARQLConsts.YAGO)) {
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

    private String getStringLiteral(RDFNode node) {
        String literal = null;
        if (node != null && node.isLiteral()) {
            literal = ((Literal) node).getLexicalForm();
        }
        return literal;
    }

    private String getLabelOfSpotlightType(String type) {
        String nsAndType = getNsAndTypeForSpotlightType(type);
        String queryString = SPARQLConsts.PREFIX_RDFS + PREFIX_DBRES;
        queryString += "SELECT ?name FROM <http://dbpedia.org> WHERE {\n" +
                nsAndType + " rdfs:label ?name .\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> resultList = new ArrayList<>();
        addResultsToList(results, resultList, "name");
        return resultList.get(0);
    }

    private String getLabelOfType(String type) {
        type = "<" + type + ">";
        String queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_FOAF;
        String variable = "name";
        queryString += "SELECT ?" + variable + " FROM <http://dbpedia.org> WHERE {\n" +
                "{" + type + " rdfs:label ?name .}\n" +
                SPARQLConsts.UNION +
                "{" + type + " foaf:name ?name .}\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(results != null){
            if (results.hasNext()){
                QuerySolution result = results.next();
                return getStringLiteral(result.get(variable));
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
        String queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_VRANK + SPARQLConsts.PREFIX_DBPEDIA_ONTOLOGY + SPARQLConsts.PREFIX_DBPEDIA_PROPERTY +
                "SELECT distinct (SAMPLE (?s) AS ?subject) (SAMPLE (?p) AS ?pred) (SAMPLE(?o) AS ?object) " +
                "(SAMPLE(?slabel) AS ?sublabel) (SAMPLE (?plabel) AS ?predlabel) (SAMPLE(?olabel) AS ?oblabel) ?v \n" +
                "FROM <" + SPARQLConsts.DBPEDIA_URL + "> \n" +
                "FROM <" + SPARQLConsts.DBPEDIA_PAGE_RANK + "> \n" +
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
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(SPARQLConsts.DBPEDIA_SPARQL_SERVICE , queryString);
        qExec.addDefaultGraph(SPARQLConsts.DBPEDIA_URL);
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
        String subject = "subject";
        String predicate = "pred";
        String object = "object";
        String subLabel = "sublabel";
        String predLabel = "predlabel";
        String obLabel = "oblabel";
        String vRank = "v";

        if (resultSet != null) {
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.next();
                LinkSUMResultRow.LinkSUMResultRowBuilder builder = LinkSUMResultRow.builder();
                if (result != null) {
                    RDFNode subjectNode = result.get(subject);
                    RDFNode predicateNode = result.get(predicate);
                    RDFNode objectNode = result.get(object);
                    builder
                            .subject(subjectNode != null ? subjectNode.toString() : null)
                            .predicate(predicateNode != null ? predicateNode.toString() : null)
                            .object(objectNode != null ? objectNode.toString() : null)
                            .subjectLabel(getStringLiteral(result.get(subLabel)))
                            .predicateLabel(getStringLiteral(result.get(predLabel)))
                            .objectLabel(getStringLiteral(result.get(obLabel)));
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

    WhoAmIQuestionStructure getWhoAmIQuestion(DBPediaResource resource, String level) {
        String uri = resource.getURI();
        List<String> mostSpecificTypes = getNMostSpecificTypes(uri, 1, true);
        String baseType = "";
        String baseTypeLabel = "";
        if(mostSpecificTypes != null && !mostSpecificTypes.isEmpty()){
            baseType = mostSpecificTypes.get(0);
        }
        if(baseType.isEmpty() || baseType.equalsIgnoreCase(SPARQLConsts.OWL_PERSON)){
            List<String> yagoTypes = getNMostSpecificYAGOTypesForDepthRange(uri, 10, 11, 14);
            if(!yagoTypes.isEmpty()){
                int randomIndex = ThreadLocalRandom.current().nextInt(yagoTypes.size());
                baseType = yagoTypes.get(randomIndex);
                baseTypeLabel = getWikicatYAGOTypeName(baseType);
            }
        } else{
            baseTypeLabel = getLabelOfType(baseType);
        }
        if(baseTypeLabel == null || baseTypeLabel.isEmpty()){
            return null;
        }

        WhoAmIQuestionStructure.WhoAmIQuestionStructureBuilder builder = WhoAmIQuestionStructure.builder();
        String resourceName = resource.getSurfaceForm();

        builder.baseType(RiTa.singularize(baseTypeLabel));
        builder.answer(resourceName);

        List<LinkSUMResultRow> linkSUMResults = getLinkSUMForResource(uri);
        int randomIndex;
        LinkSUMResultRow linkSUMResultRow, secondLinkSUMResultRow;
        List<LinkSUMResultRow> tempResults = new ArrayList<>();
        if(!linkSUMResults.isEmpty()){
            linkSUMResults.forEach(resultRow -> {
                if (resultRow != null) {
                    if (resultRow.getObjectLabel() != null && resultRow.getSubjectLabel() != null
                            && resultRow.getPredicateLabel() != null) {
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
                getWhoAmIFromLinkSUMRow(builder, linkSUMResultRow, 1);

                int secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                int i = bound;
                while(secondRandomIndex == randomIndex && i >= 0){
                    secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                    i--;
                }
                secondLinkSUMResultRow = linkSUMResults.get(secondRandomIndex);
                i = bound;
                while (linkSUMResultRow.getPredicateLabel().equalsIgnoreCase(secondLinkSUMResultRow.getPredicateLabel())
                        && i >= 0){
                    secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                    secondLinkSUMResultRow = linkSUMResults.get(secondRandomIndex);
                    i--;
                }
                getWhoAmIFromLinkSUMRow(builder, secondLinkSUMResultRow, 2);
                List<String> distractors = getNPopularDistractorsForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3, 1);
                builder.distractors(distractors);

            } else if(level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)){
                linkSUMResultRow = getHardPropertyForWhoAmI(linkSUMResults);
                getWhoAmIFromLinkSUMRow(builder, linkSUMResultRow, 1);

                secondLinkSUMResultRow = getHardPropertyForWhoAmI(linkSUMResults);
                int i = size;
                while (linkSUMResultRow == secondLinkSUMResultRow ||
                        linkSUMResultRow.getPredicateLabel().equalsIgnoreCase(secondLinkSUMResultRow.getPredicateLabel())
                        && i >= 0){
                    secondLinkSUMResultRow = getHardPropertyForWhoAmI(linkSUMResults);
                    i--;
                }
                getWhoAmIFromLinkSUMRow(builder, secondLinkSUMResultRow, 2);
                List<String> distractors = getNPopularDistractorsForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3, 2);
                if(distractors.isEmpty() || distractors.size() < 3){
                    distractors.addAll(getNPopularDistractorsForBaseType(uri, baseType, secondLinkSUMResultRow, linkSUMResultRow, 3 - distractors.size(), 2));
                    if(distractors.isEmpty() || distractors.size() < 3){
                        distractors.addAll(getNPopularDistractorsForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3 - distractors.size(), 1));
                    }
                }
                builder.distractors(distractors);
            }
            WhoAmIVerbaliser whoAmIVerbaliser = new WhoAmIVerbaliser();
            builder.question(whoAmIVerbaliser.verbalise(builder.build()));
            return builder.build();
        }
        return null;
    }

    private List<String> getNPopularDistractorsForBaseType(String uri, String baseType, int n) {
        List<String> distractors = new ArrayList<>();
        float answerPop = getVRankOfResource(uri);
        uri = "<" + uri + ">";
        baseType = "<" + baseType + ">";
        String var = "label";
        String queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_FOAF + SPARQLConsts.PREFIX_RDF + SPARQLConsts.PREFIX_VRANK + SPARQLConsts.PREFIX_DBPEDIA_ONTOLOGY +
                "select distinct ?d (SAMPLE(?dlabel) AS ?" + var + ") where {" +
                "{?d rdfs:label ?dlabel .}\n" +
                SPARQLConsts.UNION +
                "{?d foaf:name ?dlabel .}\n" +
                "{\n" +
                "select distinct ?d (ABS(" + answerPop + " - ?v) AS ?sd) where {\n" +
                "{\n" +
                "SELECT distinct ?d ?v WHERE {\n" +
                "?d a " + baseType + " .\n" +
                "?d vrank:hasRank/vrank:rankValue ?v.\n" +
                "filter (?d != " + uri + ")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "filter (langMatches(lang(?dlabel), \"EN\"))\n" +
                "} group by ?d ?sd order by (?sd) limit " + n;

        try {
            ResultSet results = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE, SPARQLConsts.PAGE_RANK_GRAPH);
            distractors = getResultSetAsStringList(results, var, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distractors;
    }

    private List<String> getNPopularDistractorsForBaseType(String uri, String baseType, LinkSUMResultRow first, LinkSUMResultRow second,  int n, int level) {
        List<String> distractors = new ArrayList<>();
        float answerPop = getVRankOfResource(uri);
        uri = "<" + uri + ">";
        baseType = "<" + baseType + ">";
        String var = "label";
        String s1 = first.getSubject() != null ? "<" + first.getSubject() + "> " : "?d ";
        String p1 = first.getPredicate() != null ? "<" + first.getPredicate() + "> " : null;
        String o1 = first.getObject() != null ? "<" + first.getObject() + "> " : "?d ";
        String s2 = second.getSubject() != null ? "<" + second.getSubject() + "> " : "?d ";
        String p2 = second.getPredicate() != null ? "<" + second.getPredicate() + "> " : null;
        String o2 = second.getObject() != null ? "<" + second.getObject() + "> " : "?d ";

        String queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_FOAF + SPARQLConsts.PREFIX_RDF + SPARQLConsts.PREFIX_VRANK + SPARQLConsts.PREFIX_DBPEDIA_ONTOLOGY +
                "select distinct ?d (SAMPLE(?dlabel) AS ?" + var + ") where {" +
                "{?d rdfs:label ?dlabel .}\n" +
                SPARQLConsts.UNION +
                "{?d foaf:name ?dlabel .}\n" +
                "{\n" +
                "select distinct ?d (ABS(" + answerPop + " - ?v) AS ?sd) where {\n" +
                "{\n" +
                "SELECT distinct ?d ?v WHERE {\n" +
                "?d a " + baseType + " .\n" +
                "?d vrank:hasRank/vrank:rankValue ?v.\n" +
                "filter (?d != " + uri + ")\n";

        if(level == 1){
            if(p1 != null) {
                queryString += "filter not exists {" + s1 + p1 + o1  + "}\n";
            }
            if(p2 != null) {
                queryString += "filter not exists {" + s2 + p2 + o2  + "}\n";
            }
        } else if (level == 2) {
            if(p1 != null) {
                queryString += s1 + p1 + o1  + "\n";
            }
            if(p2 != null) {
                queryString += "filter not exists {" + s2 + p2 + o2  + "}\n";
            }
        }

        queryString +=
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "filter (langMatches(lang(?dlabel), \"EN\"))\n" +
                "} group by ?d ?sd order by (?sd) limit " + n;

        try {
            ResultSet results = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE, SPARQLConsts.PAGE_RANK_GRAPH);
            distractors = getResultSetAsStringList(results, var, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distractors;
    }

    private float getVRankOfResource(String uri) {
        uri = "<" + uri + ">";
        String var = "v";
        String queryString = SPARQLConsts.PREFIX_VRANK +
                "SELECT ?" + var + "\n" +
                "FROM <http://dbpedia.org> \n" +
                "FROM <http://people.aifb.kit.edu/ath/#DBpedia_PageRank> \n" +
                "WHERE {\n" +
                uri + " vrank:hasRank/vrank:rankValue ?" + var + ".\n" +
                "}\n";

        try {
            ResultSet results = QueryExecutor.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
            if(results != null){
                while (results.hasNext()){
                    QuerySolution result = results.next();
                    RDFNode vRank = result.get(var);
                    if(vRank != null && vRank.isLiteral()){
                        Literal literal = (Literal) vRank;
                        return (literal.getFloat());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private boolean isIdentityRevealed(LinkSUMResultRow linkSUMResultRow, String resourceName) {
        String[] nameParts = resourceName.split(" ");
        for (String namePart : nameParts) {
            String subject = linkSUMResultRow.getSubjectLabel();
            String object = linkSUMResultRow.getObjectLabel();
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
        while (linkSUMResultRow == null && upperBound <= maxVRank){
            lowerBound = upperBound;
            upperBound *= 2;
            linkSUMResultRow = getLinkSUMResultRowForVRankRange(linkSUMResults, lowerBound, upperBound);
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

    private void getWhoAmIFromLinkSUMRow(WhoAmIQuestionStructure.WhoAmIQuestionStructureBuilder builder, LinkSUMResultRow linkSUMResultRow, int propNo) {
        switch (propNo){
            case 1:
                if(linkSUMResultRow.getSubject() == null)
                {
                    builder.firstPredicate(linkSUMResultRow.getPredicateLabel())
                            .firstObject(linkSUMResultRow.getObjectLabel());
                }
                else if(linkSUMResultRow.getObject() == null)
                {
                    builder.firstPredicate(linkSUMResultRow.getPredicateLabel())
                            .firstSubject(linkSUMResultRow.getSubjectLabel());
                }
                break;
            case 2:
                if(linkSUMResultRow.getSubject() == null)
                {
                    builder.secondPredicate(linkSUMResultRow.getPredicateLabel())
                            .secondObject(linkSUMResultRow.getObjectLabel());
                }
                else if(linkSUMResultRow.getObject() == null)
                {
                    builder.secondPredicate(linkSUMResultRow.getPredicateLabel())
                            .secondSubject(linkSUMResultRow.getSubjectLabel());
                }
        }
    }
}
