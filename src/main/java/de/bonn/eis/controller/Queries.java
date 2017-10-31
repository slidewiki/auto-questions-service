package de.bonn.eis.controller;

import de.bonn.eis.model.LinkSUMResultRow;
import de.bonn.eis.utils.SPARQLConsts;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 10/31/17.
 */
public class Queries {
    static int getTypeDepth(String type) {
        String queryString =
                SPARQLConsts.PREFIX_RDF + SPARQLConsts.PREFIX_FOAF + SPARQLConsts.PREFIX_RDFS +
                        "SELECT DISTINCT ?path FROM <http://dbpedia.org> WHERE {\n" +
                        "<" + type + "> rdfs:subClassOf* ?path . }";

        ResultSet results;
        int count = 0;
        try {
            results = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
            while (results.hasNext()) {
                results.next();
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    static List<String> getNPopularDistractorsForBaseType(String uri, String baseType, int n) {
        List<String> distractors = new ArrayList<>();
        float answerPop = QueryUtils.getVRankOfResource(uri);
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
            ResultSet results = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE, SPARQLConsts.PAGE_RANK_GRAPH);
            distractors = QueryUtils.getResultSetAsStringList(results, var, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distractors;
    }

    static List<String> getNMostSpecificTypes(String resourceURI, int n, boolean owlClassOnly) {
        String queryString = SPARQLConsts.PREFIX_RDFS;
        String variableName = "type";
        resourceURI = "<" + resourceURI + ">";

        queryString += "SELECT DISTINCT ?" + variableName + " (COUNT(*) as ?count) FROM <http://dbpedia.org> WHERE {\n" +
                " {\n" +
                " select distinct ?" + variableName + " where {\n" +
                resourceURI + " a ?" + variableName + " .\n";
        if (owlClassOnly) {
            queryString = SPARQLConsts.PREFIX_OWL + queryString;
            queryString += "?" + variableName + " a owl:Class .\n";
        } else {
            queryString += "FILTER (strstarts(str(?" + variableName + "), \"" + SPARQLConsts.DBPEDIA_URL + "\"))";
        }
        queryString += " }\n" +
                " }\n" +
                " ?" + variableName + " rdfs:subClassOf* ?path .\n" +
                " } group by (?" + variableName + ") order by desc (?count) limit " + n + "\n";

        ResultSet resultSet = null;
        try {
            resultSet = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return QueryUtils.getResultSetAsStringList(resultSet, variableName, false);
    }

    static List<String> getNMostSpecificYAGOTypesForDepthRange(String resourceURI, int n, int depthLowerBound, int depthUpperBound) {
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

        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(SPARQLConsts.DBPEDIA_SPARQL_SERVICE, queryString);
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
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(SPARQLConsts.DBPEDIA_SPARQL_SERVICE, queryString);
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

        return QueryUtils.getResultSetAsObjectList(resultSet);
    }

    static List<String> getNPopularDistractorsForBaseType(String uri, String baseType, LinkSUMResultRow first, LinkSUMResultRow second, int n, int level) {
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

        String queryString = SPARQLConsts.PREFIX_RDFS +
                SPARQLConsts.PREFIX_FOAF +
                SPARQLConsts.PREFIX_RDF +
                SPARQLConsts.PREFIX_VRANK +
                SPARQLConsts.PREFIX_DBPEDIA_ONTOLOGY +
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
            ResultSet results = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE, SPARQLConsts.PAGE_RANK_GRAPH);
            distractors = QueryUtils.getResultSetAsStringList(results, var, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distractors;
    }
}
