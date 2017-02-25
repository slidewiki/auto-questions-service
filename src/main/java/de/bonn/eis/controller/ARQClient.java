package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

import java.util.*;

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
    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";
    private final String PREFIX_DUL = "PREFIX dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl>\n";

    // TODO Use QueryBuilder
    // http://stackoverflow.com/questions/7250189/how-to-build-sparql-queries-in-java
    public List<String> getSimilarResourceNames(DBPediaResource resource) {

        List<String> resourceTypeList;
        String queryString =
                PREFIX_RDF + PREFIX_FOAF +
                        "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                        "?s foaf:name ?name .\n";

        String resourceTypes = resource.getTypes();
        if (resourceTypes.isEmpty()) {
            resourceTypeList = getResourceTypes(resource);
            if(!resourceTypeList.isEmpty()){
                QGenLogger.info("Resource: " + resource.getSurfaceForm());
                resourceTypeList = getMostSpecificTypes(resourceTypeList);
            }
        } else {
            String[] typeArray = resourceTypes.split(",");
            resourceTypeList = new ArrayList<>(Arrays.asList(typeArray));
        }

        if(resourceTypeList == null){
            return new ArrayList<>();
        }
        for (String nsAndType : resourceTypeList) {
            if (nsAndType.contains("Http://") || nsAndType.contains("http://")) { // TODO Something more flexible?
                nsAndType = nsAndType.replace("Http://", "http://");
                queryString += "?s rdf:type <" + nsAndType + "> .\n";
            } else {
                if(nsAndType.indexOf(":") > 0){
                    String[] nsTypePair = nsAndType.split(":");
                    String namespace = nsTypePair[0].trim();
                    String type = nsTypePair[1];
                    queryString = addPrefix(queryString, namespace);
                    nsAndType = getNamespace(namespace) + ":" + type;
                } else if(nsAndType.contains("@")){
                    nsAndType = nsAndType.substring(0, nsAndType.indexOf("@")).trim();
                }
                queryString += "?s rdf:type " + nsAndType + " .\n";
            }
        }
        queryString += "} LIMIT " + QUERY_LIMIT; // add bind(rand(1 + strlen(str(?s))*0) as ?rid) for randomization
        // TODO Refine Query results
        List<String> resourceNames = new ArrayList<>();
        resourceNames.add("Distractors queried from DBPedia\n");

        try {
            ResultSet results = runSelectQuery(queryString);
            while (results.hasNext()) {
                QuerySolution result = results.next();
                RDFNode n = result.get("name");
                String nameLiteral;
                if (n.isLiteral()) {
                    nameLiteral = ((Literal) n).getLexicalForm();
                    resourceNames.add(nameLiteral);
                }
            }
        } catch (Exception e) {
            QGenLogger.severe("Exception in SELECT\n" + resource.getURI() + "\n" + queryString + "\n" + e.getMessage());
        }
        return resourceNames;
    }

    //TODO Query builder
    private List<String> getResourceTypes(DBPediaResource resource) {

        List<String> resourceTypes = new ArrayList<>();
        String uri = "<" + resource.getURI() + ">";
        String queryString = PREFIX_RDF  + "SELECT ?o WHERE {\n" +
                uri + " rdf:type ?o .\n" +
                "}";

        try {
            ResultSet results = runSelectQuery(queryString);
            while (results.hasNext()) {
                QuerySolution result = results.next();
                if(result != null){
                    RDFNode node = result.get("o");
                    resourceTypes.add(node.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceTypes;
    }

    private ResultSet runSelectQuery(String queryString) throws Exception {
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(SPARQL_SERVICE, queryString);
        qExec.addParam("timeout","1000"); //1 sec

        ResultSet set;
        try {
            set = qExec.execSelect();
            set = ResultSetFactory.copyResults(set);
        } catch (Exception e) {
            throw e;
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
     * @return
     */
    private List<String> getMostSpecificTypes(List<String> types) {

        if(types.isEmpty()){
            return null;
        } else if(types.size() == 1) {
            return types;
        }

        List<String> mostSpecificTypes = new ArrayList<>();
        int maxPathDepth = 0;

        for (String type: types) {
            if(type.contains("dbpedia")){
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
                    QGenLogger.info("Type: " + type + "\t" + "Depth: " + count);
                    if(count > maxPathDepth) {
                        if(!mostSpecificTypes.isEmpty()){
                            mostSpecificTypes.clear();
                        }
                        maxPathDepth = count;
                        mostSpecificTypes.add(type);
                    } else if(count == maxPathDepth) {
                        mostSpecificTypes.add(type);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
