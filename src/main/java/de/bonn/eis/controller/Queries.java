package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.LinkSUMResultRow;
import de.bonn.eis.utils.Constants;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by andy on 10/31/17.
 */
public class Queries {
    static int getTypeDepth(String type) {
        String queryString =
                Constants.PREFIX_RDF + Constants.PREFIX_FOAF + Constants.PREFIX_RDFS +
                        "SELECT DISTINCT ?path FROM <http://dbpedia.org> WHERE {\n" +
                        "<" + type + "> rdfs:subClassOf* ?path . }";

        ResultSet results;
        int count = 0;
        try {
            results = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
            while (results.hasNext()) {
                results.next();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    static List<String> getDistractorsByPopularityForBaseType(String uri, String baseType, int n) {
        List<String> distractors = new ArrayList<>();
        float answerPop = QueryUtils.getVRankOfResource(uri);
        uri = "<" + uri + ">";
        baseType = "<" + baseType + ">";
        String var = "label";
        String queryString = Constants.PREFIX_RDFS + Constants.PREFIX_FOAF + Constants.PREFIX_RDF + Constants.PREFIX_VRANK + Constants.PREFIX_DBPEDIA_ONTOLOGY +
                "select distinct ?d (SAMPLE(?dlabel) AS ?" + var + ") where {" +
                "{?d rdfs:label ?dlabel .}\n" +
                Constants.UNION +
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
            ResultSet results = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE, Constants.PAGE_RANK_GRAPH);
            distractors = QueryUtils.getResultSetAsStringList(results, var, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distractors;
    }

    static List<String> getNMostSpecificTypes(String resourceURI, int n, boolean owlClassOnly) {
        String queryString = Constants.PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";

        queryString += "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) FROM <http://dbpedia.org> WHERE {\n" +
                " {\n" +
                " select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n";
        if (owlClassOnly) {
            queryString = Constants.PREFIX_OWL + queryString;
            queryString += "?" + variableName + " a owl:Class .\n";
        } else {
            queryString += "FILTER (strstarts(str(?" + variableName + "), \"" + Constants.DBPEDIA_URL + "\"))";
        }
        queryString += " }\n" +
                " }\n" +
                " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
                " } group by (?" + variableName + ") order by desc (?count) limit " + n + "\n";

        ResultSet resultSet = null;
        try {
            resultSet = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return QueryUtils.getResultSetAsStringList(resultSet, variableName, false);
    }

    static List<String> getNMostSpecificYAGOTypesForDepthRange(String resourceURI, int n, int depthLowerBound, int depthUpperBound) {
        String queryString = Constants.PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";
        queryString += "SELECT ?" + variableName + " ?count WHERE {\n" +
                "{\n" +
                "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) WHERE {\n" +
                "  {\n" +
                "   select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n" +
                "   filter (strstarts(str(?" + variableName + "), \"" + Constants.DBPEDIA_URL + "\"))\n" +
                "   filter (!strstarts(str(?" + variableName + "), \"" + Constants.DBPEDIA_CLASS_YAGO_WIKICAT + "\"))\n" +
                "  }\n" +
                " }\n" +
                " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
                "} group by ?" + variableName + " order by desc  (?count)\n" +
                "}\n";
        if (n != -1) {
            queryString += "filter (?count < " + depthUpperBound + " && ?count > " + depthLowerBound + ")\n";
        }
        queryString += "} limit " + n;

//        ResultSet resultSet = null;
//        try {
//            resultSet = runSelectQuery(queryString, DBPEDIA_SPARQL_SERVICE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(Constants.DBPEDIA_SPARQL_SERVICE, queryString);
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
        return QueryUtils.getResultSetAsStringList(resultSet, variableName, false);
    }

    static List<LinkSUMResultRow> getLinkSUMForResource(String resourceURI) {
        resourceURI = "<" + resourceURI + ">";
        String queryString = Constants.PREFIX_RDFS + Constants.PREFIX_VRANK + Constants.PREFIX_DBPEDIA_ONTOLOGY + Constants.PREFIX_DBPEDIA_PROPERTY +
                "SELECT distinct (SAMPLE (?s) AS ?subject) (SAMPLE (?p) AS ?pred) (SAMPLE(?o) AS ?object) " +
                "(SAMPLE(?slabel) AS ?sublabel) (SAMPLE (?plabel) AS ?predlabel) (SAMPLE(?olabel) AS ?oblabel) ?v \n" +
                "FROM <" + Constants.DBPEDIA_URL + "> \n" +
                "FROM <" + Constants.DBPEDIA_PAGE_RANK + "> \n" +
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
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(Constants.DBPEDIA_SPARQL_SERVICE, queryString);
        qExec.addDefaultGraph(Constants.DBPEDIA_URL);
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

        return QueryUtils.getResultSetAsObjectList(resultSet);
    }

    static List<String> getDistractorsByPopularityForBaseType(String uri, String baseType, LinkSUMResultRow first, LinkSUMResultRow second, int n, int level) {
        List<String> distractors = new ArrayList<>();
        float answerPop = QueryUtils.getVRankOfResource(uri);
        uri = "<" + uri + ">";
        baseType = "<" + baseType + ">";
        String var = "label";
        String s1 = first.getSubject() != null ? "<" + first.getSubject() + "> " : "?d ";
        String p1 = first.getPredicate() != null ? "<" + first.getPredicate() + "> " : null;
        String o1 = first.getObject() != null ? "<" + first.getObject() + "> " : "?d ";
        String s2 = second.getSubject() != null ? "<" + second.getSubject() + "> " : "?d ";
        String p2 = second.getPredicate() != null ? "<" + second.getPredicate() + "> " : null;
        String o2 = second.getObject() != null ? "<" + second.getObject() + "> " : "?d ";

        String queryString = Constants.PREFIX_RDFS +
                Constants.PREFIX_FOAF +
                Constants.PREFIX_RDF +
                Constants.PREFIX_VRANK +
                Constants.PREFIX_DBPEDIA_ONTOLOGY +
                "select distinct ?d (SAMPLE(?dlabel) AS ?" + var + ") where {" +
                "{?d rdfs:label ?dlabel .}\n" +
                Constants.UNION +
                "{?d foaf:name ?dlabel .}\n" +
                "{\n" +
                "select distinct ?d (ABS(" + answerPop + " - ?v) AS ?sd) where {\n" +
                "{\n" +
                "SELECT distinct ?d ?v WHERE {\n" +
                "?d a " + baseType + " .\n" +
                "?d vrank:hasRank/vrank:rankValue ?v.\n" +
                "filter (?d != " + uri + ")\n";

        if (level == 1) {
            if (p1 != null) {
                queryString += "filter not exists {" + s1 + p1 + o1 + "}\n";
            }
            if (p2 != null) {
                queryString += "filter not exists {" + s2 + p2 + o2 + "}\n";
            }
        } else if (level == 2) {
            if (p1 != null) {
                queryString += s1 + p1 + o1 + "\n";
            }
            if (p2 != null) {
                queryString += "filter not exists {" + s2 + p2 + o2 + "}\n";
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
            ResultSet results = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE, Constants.PAGE_RANK_GRAPH);
            distractors = QueryUtils.getResultSetAsStringList(results, var, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distractors;
    }

    static void getHardSiblingTypes(DBPediaResource answer, List<String> sisterTypes, List<String> typeStrings) {
        String queryString;
        ResultSet resultSet;
        queryString = Constants.PREFIX_RDFS + Constants.PREFIX_FOAF;
        queryString += "SELECT DISTINCT ?dName ?aName ?d FROM <http://dbpedia.org> WHERE {\n";
        for (String typeString : typeStrings) {
            queryString += "{\n <" + typeString + "> rdfs:subClassOf ?st .\n" +
                    " optional { <" + typeString + "> rdfs:label ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                    " optional { <" + typeString + "> foaf:name ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                    "}\n";
            if (typeStrings.indexOf(typeString) != typeStrings.size() - 1) {
                queryString += Constants.UNION;
            }
        }

        queryString += " ?d rdfs:subClassOf ?st .\n" +
                " optional {?d rdfs:label ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                " optional {?d foaf:name ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                " filter not exists{<" + answer.getURI() + "> a ?d .}\n" +
                "} order by rand() limit 3";

        resultSet = null;
        try {
            resultSet = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
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
                    String literal = QueryUtils.getStringLiteral(distractor);
                    if (literal != null) {
                        sisterTypes.add(literal);
                    } else {
                        RDFNode distractorNode = result.get("d");
                        String distractorString = distractorNode.toString();
                        sisterTypes.add(QueryUtils.getWikicatYAGOTypeName(distractorString));
                    }
                }
            }
            String literal = QueryUtils.getStringLiteral(answerType);
            if (literal != null) {
                sisterTypes.add(0, literal);
            } else {
                int randomIndex = ThreadLocalRandom.current().nextInt(typeStrings.size());
                sisterTypes.add(0, QueryUtils.getWikicatYAGOTypeName(typeStrings.get(randomIndex)));
            }
        }
    }

    static void getEasySiblingTypesForResource(DBPediaResource answer, List<String> sisterTypes) {
        String queryString = Constants.PREFIX_RDFS + Constants.PREFIX_OWL;
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
            resultSet = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
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
                    String literal = QueryUtils.getStringLiteral(distractor);
                    if (literal != null) {
                        sisterTypes.add(literal);
                    }
                }
            }
            String literal = QueryUtils.getStringLiteral(answerType);
            if (literal != null) {
                sisterTypes.add(0, literal);
            }
        }
    }

    static List<String> getEasySiblingTypesFromSpotlightTypes(DBPediaResource answer, List<String> sisterTypes, String types) {
        String[] typesArray = types.split(",");
        List<String> resourceTypeList = Arrays.asList(typesArray);
        resourceTypeList = resourceTypeList.stream().filter(type -> type.toLowerCase().contains(Constants.DBPEDIA)).collect(Collectors.toList());
        if (resourceTypeList != null) {
            int size = resourceTypeList.size();
            sisterTypes.add(QueryUtils.getLabelOfSpotlightType(resourceTypeList.get(size - 1)));
            for (int i = size - 1; i > 0; i--) {
                String type = resourceTypeList.get(i);
                String superType = resourceTypeList.get(i - 1);
                String queryString =
                        Constants.PREFIX_RDF + Constants.PREFIX_RDFS + Constants.PREFIX_OWL;
                queryString = QueryUtils.addNsAndType(queryString, type, false);
                queryString = QueryUtils.addNsAndType(queryString, superType, false);
                type = QueryUtils.getNsAndTypeForSpotlightType(type);
                superType = QueryUtils.getNsAndTypeForSpotlightType(superType);

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
                    resultSet = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                QueryUtils.addResultsToList(resultSet, sisterTypes, "name");
                if (sisterTypes.size() == 4) {
                    break;
                } else if (sisterTypes.size() > 4) {
                    sisterTypes = sisterTypes.subList(0, 4);
                }
            }
        }
        return sisterTypes;
    }
}
