package de.bonn.eis.controller;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private static final String SPARQL_SERVICE = "http://dbpedia.org/sparql";
    private static final int QUERY_LIMIT = 20;
    private static final String PREFIX_RDF = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";
    private static final String PREFIX_FOAF = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";
    private static final String PREFIX_RDFS = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";
    private static final String PREFIX_WIKIDATA = "PREFIX wikidata: <http://www.wikidata.org/entity/>\n";
    private static final String TIMEOUT_VALUE = "5000";
    private static final String UNION = "UNION\n";
    private static final int ALLOWED_DEPTH_FOR_MEDIUM = 8;
    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";
    private final String PREFIX_DUL = "PREFIX dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl>\n";

    // TODO Use QueryBuilder
    // http://stackoverflow.com/questions/7250189/how-to-build-sparql-queries-in-java
    public List<String> getSimilarResourceNames(DBPediaResource resource, String level) {
        List<String> resourceNames = new ArrayList<>();
        List<String> resourceTypeList = null;

        if(level.equals(NLPConsts.LEVEL_EASY)){
            String[] types = resource.getTypes().split(",");
            resourceTypeList = Arrays.asList(types[types.length - 1]);
        } else if(level.equals(NLPConsts.LEVEL_MEDIUM) || level.equals(NLPConsts.LEVEL_HARD)){
            resourceTypeList = getResourceTypes(resource);
            if (resourceTypeList != null && !resourceTypeList.isEmpty()) {
                if (resourceTypeList.size() == 1) {
                    String type = resourceTypeList.get(0);
                    if (type.contains("owl:Thing") || type.contains("owl#Thing")) {
                        return null;
                    }
                }
                QGenLogger.info("Resource: " + resource.getSurfaceForm());
                int maxAllowedDepth = Integer.MAX_VALUE;
                if(level.equals(NLPConsts.LEVEL_MEDIUM)){
                    maxAllowedDepth = ALLOWED_DEPTH_FOR_MEDIUM;
                }
                resourceTypeList = getMostSpecificTypes(resourceTypeList, maxAllowedDepth);
            }
        }

        String queryString =
                PREFIX_RDF + PREFIX_FOAF + PREFIX_RDFS +
                        "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                        "{ ?s foaf:name ?name }\n" + UNION +
                        "{ ?s rdfs:label ?name }\n";


        if (resourceTypeList == null || resourceTypeList.isEmpty()) {
            return null;
        }

        queryString = addTriplePatternsAndUnions(resourceTypeList, queryString);
        queryString += "FILTER (langMatches(lang(?name), \"EN\")) .";
        queryString += "}\nORDER BY RAND()\nLIMIT " + QUERY_LIMIT; // add bind(rand(1 + strlen(str(?s))*0) as ?rid) for randomization
        // TODO Refine Query results

        ResultSet results = null;
        try {
            QGenLogger.fine("###########################RESOURCE: " + resource.getSurfaceForm() + "###########################\n" + "SELECT Query:\n" + queryString);
            results = runSelectQuery(queryString);
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

    private String addTriplePatternsAndUnions(List<String> resourceTypeList, String queryString) {
        int numberOfTypes = resourceTypeList.size();
        int groupSize = getGroupSize(numberOfTypes);
        int count = 0;
        boolean curlyBraceOpened = false;
        for (int i = 0; i < numberOfTypes; i++) {
            String nsAndType = resourceTypeList.get(i);
            if (groupSize > 0 && count == 0) {
                queryString += "{\n";
                curlyBraceOpened = true;
            }
            if (nsAndType.contains("Http://") || nsAndType.contains("http://")) { // TODO Something more flexible?
                nsAndType = nsAndType.replace("Http://", "http://");
                nsAndType = "<" + nsAndType + ">";
            } else {
                if (nsAndType.indexOf(":") > 0) {
                    String[] nsTypePair = nsAndType.split(":");
                    String namespace = nsTypePair[0].trim();
                    String type = nsTypePair[1];
                    queryString = addPrefix(queryString, namespace);
                    nsAndType = getNamespace(namespace) + ":" + type;
                } else if (nsAndType.contains("@")) {
                    nsAndType = nsAndType.substring(0, nsAndType.indexOf("@")).trim();
                }
            }
            queryString += "?s a " + nsAndType + " .\n";
            if (groupSize > 0) {
                count++;
                if (groupSize == count) {
                    queryString += "}\n";
                    curlyBraceOpened = false;
                    if (i < numberOfTypes - 1) {
                        queryString += UNION;
                    }
                    count = 0;
                }
            }
        }
        if (curlyBraceOpened) {
            queryString += "}\n";
        }
        return queryString;
    }

    private int getGroupSize(int sizeOfList) {
        int maxGroupSize = 2;
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
            results = runSelectQuery(queryString);
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

    private ResultSet runSelectQuery(String queryString) throws Exception {
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(SPARQL_SERVICE, queryString);
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

    //TODO Select multiple types for more results or changing difficulty

    /**
     * Get the most specific rdf:type i.e. the one that has the most number of super classes
     *
     * @param types
     * @param maxAllowedDepth
     * @return
     */
    private List<String> getMostSpecificTypes(List<String> types, int maxAllowedDepth) {

        if (types.isEmpty()) {
            return null;
        } else if (types.size() == 1) {
            return types;
        }

        List<String> mostSpecificTypes = new ArrayList<>();
        int maxPathDepth = 0;

        for (String type : types) {
            if (type.toLowerCase().contains("dbpedia")) {
//                if (type.toLowerCase().contains("yago") && !type.toLowerCase().contains("wikicat")) {
//                    continue;
//                }
                    String queryString =
                        PREFIX_RDF + PREFIX_FOAF + PREFIX_RDFS +
                                "SELECT DISTINCT ?path FROM <http://dbpedia.org> WHERE {\n" +
                                "<" + type + "> rdfs:subClassOf* ?path . }";

                ResultSet results;
                int count = 0;
                try {
                    results = runSelectQuery(queryString);
                    while (results.hasNext()) {
                        results.next();
                        count++;
                    }
                    if (count <= maxAllowedDepth) {
                        if (count > maxPathDepth) {
                            if (!mostSpecificTypes.isEmpty()) {
                                mostSpecificTypes.clear();
                            }
                            maxPathDepth = count;
                            mostSpecificTypes.add(type);
                        } else if (count == maxPathDepth) {
                            mostSpecificTypes.add(type);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (mostSpecificTypes.size() > 5) {
            mostSpecificTypes = mostSpecificTypes.subList(0, 6);
        }
        return mostSpecificTypes;
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
}
