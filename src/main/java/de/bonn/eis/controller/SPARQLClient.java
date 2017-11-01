package de.bonn.eis.controller;

import de.bonn.eis.utils.QGenLogger;
import de.bonn.eis.utils.Constants;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * Created by andy on 10/31/17.
 */
public class SPARQLClient {
    //TODO Query builder
    static ResultSet runSelectQuery(String queryString, String service, String... defaultGraphs) throws Exception {
        QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(service, queryString);
        qExec.addDefaultGraph(Constants.DBPEDIA_URL);
        if (defaultGraphs != null) {
            for (String defaultGraph : defaultGraphs) {
                qExec.addDefaultGraph(defaultGraph);
            }
        }
        qExec.addParam("timeout", Constants.TIMEOUT_VALUE); //100 sec
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
}
