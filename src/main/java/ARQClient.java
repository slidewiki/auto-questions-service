import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private static final String SPARQL_SERVICE = "http://dbpedia.org/sparql";
    private static final int QUERY_LIMIT = 20;
    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";

    public List<String> getSimilarResourceNames(DBPediaResource resource) {

        List<String> resourceTypeList;
        String queryString =
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                        "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                        "?s foaf:name ?name .\n";

        String resourceTypes = resource.getTypes();
        if (resourceTypes.isEmpty()) {
            resourceTypeList = getResourceTypes(resource);
            if(!resourceTypeList.isEmpty()){
                String mostSpecificType = getMostSpecificType(resourceTypeList);
                if(mostSpecificType != null){
                    List<String> tempList = new ArrayList<>();
                    tempList.add(mostSpecificType);
                    resourceTypeList = tempList;
                }
            }
        } else {
            String[] typeArray = resourceTypes.split(",");
            resourceTypeList = new ArrayList<>(Arrays.asList(typeArray));
        }

        for (String nsAndType : resourceTypeList) {
            if (nsAndType.contains("Http://") || nsAndType.contains("http://")) { // TODO Something more flexible?
                nsAndType = nsAndType.replace("Http://", "http://");
                queryString += "?s rdf:type <" + nsAndType + "> .\n";
            } else {
                if(nsAndType.contains(":")){
                    String[] nsTypePair = nsAndType.split(":");
                    String namespace = nsTypePair[0];
                    String type = nsTypePair[1];
                    queryString = addPrefix(queryString, namespace);
                    nsAndType = getNamespace(namespace) + ":" + type;
                } else if(nsAndType.contains("@")){
                    nsAndType = nsAndType.substring(0, nsAndType.indexOf("@")).trim();
                }
                queryString += "?s rdf:type " + nsAndType + " .\n";
            }
        }
        queryString += "} LIMIT " + QUERY_LIMIT;
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

    // TODO Write query to fetch types, not the entire object
    private List<String> getResourceTypes(DBPediaResource resource) {

        List<String> resourceTypes = new ArrayList<>();
        String uri = "<" + resource.getURI() + ">";
        String queryString = "construct { \n" +
                "  " + uri + " ?p ?o .\n" +
                "} \n" +
                "where { \n" +
                "  { " + uri + " ?p ?o } \n" +
                "}";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_SERVICE, query);

        try {
            Model result = qexec.execConstruct();
            if (result != null) {
                StmtIterator stmtIterator = result.listStatements();
                while (stmtIterator.hasNext()) {
                    Statement statement = stmtIterator.nextStatement();
                    if (statement.getPredicate().getLocalName().equals("type")) {
                        RDFNode object = statement.getObject();
                        String objectNodeAsString = object.toString();
//                        if(!objectNodeAsString.contains("class/yago")){
                            resourceTypes.add(objectNodeAsString);
//                        }
                    }
                }
            }
        } catch (Exception e) {
            QGenLogger.severe("Exception in CONSTRUCT\n" + queryString + "\n" + e.getMessage());
        }
        finally {
            qexec.close();
        }
        return resourceTypes;
    }

    private ResultSet runSelectQuery(String queryString) throws Exception {
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.sparqlService(SPARQL_SERVICE, query);
        ResultSet set = null;
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
     * @param types
     * @return
     */
    private String getMostSpecificType(List<String> types) {

        if(types.size() <= 1) {
            return null;
        }

        String mostSpecificType = null;
        int maxPathDepth = 0;

        for (String type: types) {
            if(type.contains("dbpedia")){
                String queryString =
                        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
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
                    System.out.println("Type: " + type + "\t" + "Depth: " + count);
                    if(count > maxPathDepth) {
                        maxPathDepth = count;
                        mostSpecificType = type;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return mostSpecificType;
    }

    private String addPrefix(String queryString, String namespace) {
        String prefix = "";
        if (namespace.equalsIgnoreCase("dbpedia")) {
            prefix = PREFIX_DBRES;
        } else if (namespace.equalsIgnoreCase("schema")) {
            prefix = PREFIX_SCHEMA;
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
        }
        return "";
    }
}
