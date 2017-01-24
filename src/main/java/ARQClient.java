import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    private final String PREFIX_DBRES = "PREFIX dbres: <http://dbpedia.org/ontology/>\n";
    private final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";

    public List<String> getSimilarResourceNames(DBPediaResource resource) {

        if (resource.getTypes().isEmpty()) {
            return null;// TODO Use URI to fetch data about resource
        }

        if(resource.getSurfaceForm().contains("Platini")){
            System.out.println(resource.toString());
        }

        String queryString =
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "SELECT DISTINCT ?name WHERE {\n" +
                "?s foaf:name ?name .\n";
        String[] resourceTypes = resource.getTypes().split(",");
        for (String nsAndType : resourceTypes) {
            if(nsAndType.contains("Http://")){ // TODO Something more flexible?
                nsAndType = nsAndType.replace("Http://", "http://");
                queryString += "?s rdf:type <" + nsAndType + "> .\n";
            }
            else {
                String[] nsTypePair = nsAndType.split(":");
                String namespace = nsTypePair[0];
                String type = nsTypePair[1];
                queryString = addPrefix(queryString, namespace);
                queryString += "?s rdf:type " + getNamespace(namespace) + ":" + type + " .\n";
            }
        }
        queryString += "} LIMIT 100";

        // TODO Refine Query results
        List<String> resourceNames = new ArrayList<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        try {
            ResultSet results = qexec.execSelect();
            if(!results.hasNext()) {
                System.out.println(results.toString());
            }
            for (; results.hasNext(); ) {
                QuerySolution result = results.next();
                RDFNode n = result.get("name");
                String nameLiteral = "";
                if ( n.isLiteral() ){
                    nameLiteral = ((Literal)n).getLexicalForm() ;
                }
                if ( n.isResource() )
                {
                    Resource r = (Resource)n ;
                    if ( ! r.isAnon() )
                    {
                        r.getURI();
                    }
                }
                resourceNames.add(nameLiteral);
            }
        } finally {
            qexec.close();
        }
        return resourceNames;
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

    private String addPrefix(String queryString, String namespace){
        String prefix = "";
        if(namespace.equalsIgnoreCase("dbpedia")) {
            prefix = PREFIX_DBRES;
        } else if(namespace.equalsIgnoreCase("schema")) {
            prefix = PREFIX_SCHEMA;
        }
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
