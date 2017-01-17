import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.util.List;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    public static void sampleRequest() {
        String queryString =
                "PREFIX dbres: <http://dbpedia.org/ontology/>\n" +
                "PREFIX schema: <http://schema.org/>\n" +
                        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "SELECT DISTINCT ?s WHERE {\n" +
                        "?s rdf:type dbres:Organisation .\n" +
                        "?s rdf:type dbres:Agent .\n" +
                        "?s rdf:type schema:Organization .\n" +
                        "?s rdf:type schema:SportsTeam .\n" +
                        "?s rdf:type dbres:SportsTeam .\n" +
                        "?s rdf:type dbres:SoccerClub .\n" +
                        "} LIMIT 20";

        String uri = "<http://dbpedia.org/resource/Berlin>";
        queryString = "construct { \n" +
                "  " + uri + " ?p ?o .\n" +
                "} \n" +
                "where { \n" +
                "  { " + uri + " ?p ?o } \n" +
                "}";

        // now creating query object
        Query query = QueryFactory.create(queryString);

        // initializing queryExecution factory with remote service.
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

        //after it goes standard query execution and result processing which can
        // be found in almost any Jena/SPARQL tutorial.
        try {
//            ResultSet results = qexec.execSelect();
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
