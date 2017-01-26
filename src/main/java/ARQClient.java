import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    public static final String SPARQL_SERVICE = "http://dbpedia.org/sparql";
    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";

    public List<String> getSimilarResourceNames(DBPediaResource resource) {

        List<String> resourceTypeList;

        String queryString =
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                        "SELECT DISTINCT ?name WHERE {\n" +
                        "?s foaf:name ?name .\n";

        String resourceTypes = resource.getTypes();
        if (resourceTypes.isEmpty()) {
            resourceTypeList = getResourceTypes(resource);// TODO Use URI to fetch data about resource
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
        queryString += "} LIMIT 100";

        // TODO Refine Query results
        List<String> resourceNames = new ArrayList<>();
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
            e.printStackTrace();
        } finally {
            qexec.close();
        }
        return resourceNames;
    }

    public List<String> getResourceTypes(DBPediaResource resource) {

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
        } finally {
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
