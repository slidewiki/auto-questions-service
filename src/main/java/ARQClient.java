import org.apache.jena.query.*;

/**
 * Created by Ainuddin Faizan on 1/7/17.
 */
public class ARQClient {

    public static void sampleRequest() {
        String queryString =
                "PREFIX pr:<http://xmlns.com/foaf/0.1/>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "SELECT DISTINCT ?s ?label WHERE {" +
                        "?s rdfs:label ?label . " +
                        "?s a pr:Person . " +
                        "FILTER (lang(?label) = 'en') . " +
                        "}";

        // now creating query object
        Query query = QueryFactory.create(queryString);

        // initializing queryExecution factory with remote service.
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

        //after it goes standard query execution and result processing which can
        // be found in almost any Jena/SPARQL tutorial.
        try {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution result = results.next();
                System.out.println(result.toString());
                // Result processing is done here.
            }
        } finally {
            qexec.close();
        }
    }
}
