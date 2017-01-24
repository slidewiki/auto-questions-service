import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";

    public List<DBPediaResource> getSimilarResources(DBPediaResource resource) {

        if (resource.getTypes().isEmpty()) {
            return null;// TODO Use URI to fetch data about resource
        }
        String queryString =
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "SELECT DISTINCT ?name ?s WHERE {\n" +
                "?s foaf:name ?name .";
        String[] resourceTypes = resource.getTypes().split(",");
        for (String nsAndType : resourceTypes) {
            String[] nsTypePair = nsAndType.split(":");
            String namespace = nsTypePair[0];
            String type = nsTypePair[1];

            if(namespace.equalsIgnoreCase("dbpedia")) {
                queryString = addPrefix(queryString, PREFIX_DBRES);
            } else if(namespace.equalsIgnoreCase("schema")) {
                queryString = addPrefix(queryString, PREFIX_SCHEMA);
            }
            queryString += "?s rdf:type " + getNamespace(namespace) + ":" + type + " .\n";
        }
        queryString += "} LIMIT 20";
        System.out.println(queryString);

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        try {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution result = results.next();
                RDFNode n = result.get("s");
                // If you need to test the thing returned
                if ( n.isLiteral() )
                    ((Literal)n).getLexicalForm() ;
                if ( n.isResource() )
                {
                    Resource r = (Resource)n ;
                    if ( ! r.isAnon() )
                    {
                        r.getURI();
                    }
                }
                System.out.println(result.toString());
            }
        } finally {
            qexec.close();
        }
        return null;
    }

    public void getResourceTypes() {

        String uri = "<http://dbpedia.org/resource/Berlin>";
        String queryString = "construct { \n" +
                "  " + uri + " ?p ?o .\n" +
                "} \n" +
                "where { \n" +
                "  { " + uri + " ?p ?o } \n" +
                "}";

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

        try {
            Model results = qexec.execConstruct();
//            for (; results.hasNext(); ) {
//                QuerySolution result = results.next();
//                System.out.println(result.toString());
//                // Result processing is done here.
//            }
            System.out.println(results.toString());
        } finally {
            qexec.close();
        }
    }

    private String addPrefix(String queryString, String prefix){
        if(!queryString.contains(prefix)) {
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

    //TODO
    public List<String> getResourceForClass(String className) {
        return null;
    }

    //TODO
    public String getAllDataForResource(String uri) {
        String query = "construct { \n" +
                "  " + uri + " ?p ?o .\n" +
                "} \n" +
                "where { \n" +
                "  { " + uri + " ?p ?o } \n" +
                "}";
        // EXEC query
        return null;
    }
}
