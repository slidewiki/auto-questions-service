import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.*;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    public static final String SPARQL_SERVICE = "http://dbpedia.org/sparql";
    public static final int QUERY_LIMIT = 20;
    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";
    private static final Logger LOGGER = Logger.getLogger(ARQClient.class.getName());
    private Handler fileHandler;

    public ARQClient() {
        try {
            fileHandler = new FileHandler("./query_messages.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.addHandler(fileHandler);
        fileHandler.setLevel(Level.ALL);
        LOGGER.setLevel(Level.ALL);
    }

    public List<String> getSimilarResourceNames(DBPediaResource resource) {

        List<String> resourceTypeList;
        String queryString =
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                        "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                        "?s foaf:name ?name .\n";

        String resourceTypes = resource.getTypes();
        if (resourceTypes.isEmpty()) {
//            resourceTypeList = getResourceTypes(resource);// TODO Use URI to fetch data about resource
            return null;
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
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_SERVICE, query);
        try {
            ResultSet results = qexec.execSelect();
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
            LOGGER.severe("Exception in SELECT\n" + resource.getURI() + "\n" + queryString + "\n" + e.getMessage());
//            LOGGER.severe(resource.getURI());
//            LOGGER.severe(queryString);
//            LOGGER.severe(e.getMessage());
        } finally {
            qexec.close();
        }
        return resourceNames;
    }

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
                        resourceTypes.add(object.toString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Exception in CONSTRUCT\n" + queryString + "\n" + e.getMessage());
//            LOGGER.severe("Exception in CONSTRUCT");
//            LOGGER.info(queryString);
//            LOGGER.severe(e.getMessage());
        }
        finally {
            qexec.close();
        }
        return resourceTypes;
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
