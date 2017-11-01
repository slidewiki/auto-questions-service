package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.LinkSUMResultRow;
import de.bonn.eis.utils.Constants;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by andy on 10/31/17.
 */
public class QueryUtils {
    private static final String PREFIX_DBRES = "PREFIX dbres: <" + Constants.DBPEDIA_ONTOLOGY + ">\n";
    private static final String PREFIX_SCHEMA = "PREFIX schema: <http://schema.org/>\n";
    private static final String PREFIX_DUL = "PREFIX dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl>\n";


    static String getBaseTypeForEasy(DBPediaResource resource) {
        String baseType = "";
        if (!resource.getTypes().isEmpty()) {
            baseType = QueryUtils.getMostSpecificSpotlightType(resource.getTypes());
        } else {
            List<String> specificTypes = Queries.getNMostSpecificTypes(resource.getURI(), 1, true);
            if (!specificTypes.isEmpty()) {
                baseType = specificTypes.get(0);
            }
        }
        if (baseType == null || baseType.isEmpty() || baseType.equalsIgnoreCase(Constants.OWL_PERSON) || Queries.getTypeDepth(baseType) <= 3) {
            List<String> yagoTypes = null;
            int lowerBound = 11;
            while ((yagoTypes == null || yagoTypes.isEmpty()) && lowerBound > 3) {
                yagoTypes = Queries.getNMostSpecificYAGOTypesForDepthRange(resource.getURI(), 10, lowerBound, lowerBound + 3);
                if (lowerBound == 5) {
                    lowerBound--;
                } else {
                    lowerBound -= 3;
                }
            }
            if (yagoTypes != null && !yagoTypes.isEmpty()) {
                int randomIndex = ThreadLocalRandom.current().nextInt(yagoTypes.size());
                baseType = yagoTypes.get(randomIndex);
            }
        }
        return baseType;
    }

    static float getVRankOfResource(String uri) {
        uri = "<" + uri + ">";
        String var = "v";
        String queryString = Constants.PREFIX_VRANK +
                "SELECT ?" + var + "\n" +
                "FROM <http://dbpedia.org> \n" +
                "FROM <http://people.aifb.kit.edu/ath/#DBpedia_PageRank> \n" +
                "WHERE {\n" +
                uri + " vrank:hasRank/vrank:rankValue ?" + var + ".\n" +
                "}\n";

        try {
            ResultSet results = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
            if (results != null) {
                while (results.hasNext()) {
                    QuerySolution result = results.next();
                    RDFNode vRank = result.get(var);
                    if (vRank != null && vRank.isLiteral()) {
                        Literal literal = (Literal) vRank;
                        return (literal.getFloat());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static String getStringLiteral(RDFNode node) {
        String literal = null;
        if (node != null && node.isLiteral()) {
            literal = ((Literal) node).getLexicalForm();
        }
        return literal;
    }

    static List<String> getResultSetAsStringList(ResultSet resultSet, String variableName, boolean literalRequired) {
        List<String> resultStrings = new ArrayList<>();
        RDFNode node;
        if (resultSet != null) {
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.next();
                if (result != null) {
                    node = result.get(variableName);
                    if (literalRequired) {
                        String literal = getStringLiteral(node);
                        if (literal != null) {
                            resultStrings.add(literal);
                        }
                    } else {
                        if (node != null) {
                            resultStrings.add(node.toString());
                        }
                    }
                }
            }
        }
        return resultStrings;
    }

    static String getLabelOfType(String type) {
        type = "<" + type + ">";
        String queryString = Constants.PREFIX_RDFS + Constants.PREFIX_FOAF;
        String variable = "name";
        queryString += "SELECT ?" + variable + " FROM <http://dbpedia.org> WHERE {\n" +
                "{" + type + " rdfs:label ?name .}\n" +
                Constants.UNION +
                "{" + type + " foaf:name ?name .}\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (results != null) {
            if (results.hasNext()) {
                QuerySolution result = results.next();
                return getStringLiteral(result.get(variable));
            }
        }
        return null;
    }

    static List<LinkSUMResultRow> getResultSetAsObjectList(ResultSet resultSet) {
        List<LinkSUMResultRow> rows = new ArrayList<>();
        String subject = "subject";
        String predicate = "pred";
        String object = "object";
        String subLabel = "sublabel";
        String predLabel = "predlabel";
        String obLabel = "oblabel";
        String vRank = "v";

        if (resultSet != null) {
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.next();
                LinkSUMResultRow.LinkSUMResultRowBuilder builder = LinkSUMResultRow.builder();
                if (result != null) {
                    RDFNode subjectNode = result.get(subject);
                    RDFNode predicateNode = result.get(predicate);
                    RDFNode objectNode = result.get(object);
                    builder
                            .subject(subjectNode != null ? subjectNode.toString() : null)
                            .predicate(predicateNode != null ? predicateNode.toString() : null)
                            .object(objectNode != null ? objectNode.toString() : null)
                            .subjectLabel(getStringLiteral(result.get(subLabel)))
                            .predicateLabel(getStringLiteral(result.get(predLabel)))
                            .objectLabel(getStringLiteral(result.get(obLabel)));
                    RDFNode vRankNode = result.get(vRank);
                    if (vRankNode != null && vRankNode.isLiteral()) {
                        Literal literal = (Literal) vRankNode;
                        builder.vRank(literal.getFloat());
                    }
                    rows.add(builder.build());
                }
            }
        }
        return rows;
    }

    // TODO
    static String getNamespace(String ns) {
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

    static String getNsAndTypeForSpotlightType(String nsAndType) {
        String[] nsTypePair = nsAndType.split(":");
        String namespace = nsTypePair[0].trim();
        String type = nsTypePair[1];
        return getNamespace(namespace) + ":" + type;
    }

    static String getLabelOfSpotlightType(String type) {
        String nsAndType = getNsAndTypeForSpotlightType(type);
        String queryString = Constants.PREFIX_RDFS + PREFIX_DBRES;
        queryString += "SELECT ?name FROM <http://dbpedia.org> WHERE {\n" +
                nsAndType + " rdfs:label ?name .\n" +
                " filter (langMatches(lang(?name), \"EN\")) ." +
                "}";
        ResultSet results = null;
        try {
            results = SPARQLClient.runSelectQuery(queryString, Constants.DBPEDIA_SPARQL_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> resultList = new ArrayList<>();
        addResultsToList(results, resultList, "name");
        return resultList.get(0);
    }

    static void addResultsToList(ResultSet results, List<String> resources, String var) {
        if (results != null) {
            while (results.hasNext()) {
                QuerySolution result = results.next();
                if (result != null) {
                    RDFNode n = result.get(var);
                    String nameLiteral;
                    if (n.isLiteral()) {
                        nameLiteral = ((Literal) n).getLexicalForm();
                        resources.add(nameLiteral);
                    }
                }
            }
        }
    }

    static String addNsAndType(String queryString, String nsAndType, boolean shouldAddTriple) {
        if (nsAndType.contains("Http://") || nsAndType.contains("http://")) { // TODO Something more flexible?
            nsAndType = nsAndType.replace("Http://", "http://");
            nsAndType = "<" + nsAndType + ">";
        } else {
            if (nsAndType.indexOf(":") > 0) {
                String[] nsTypePair = nsAndType.split(":");
                String namespace = nsTypePair[0].trim();
                String type = nsTypePair[1];
                nsAndType = getNamespace(namespace) + ":" + type;
                queryString = addPrefix(queryString, namespace);
            } else if (nsAndType.contains("@")) {
                nsAndType = nsAndType.substring(0, nsAndType.indexOf("@")).trim();
            }
        }
        if (shouldAddTriple) {
            queryString += "?s a " + nsAndType + " .\n";
        }
        return queryString;
    }

    private static String addPrefix(String queryString, String namespace) {
        String prefix = "";
        if (namespace.equalsIgnoreCase("dbpedia")) {
            prefix = PREFIX_DBRES;
        } else if (namespace.equalsIgnoreCase("schema")) {
            prefix = PREFIX_SCHEMA;
        } else if (namespace.equalsIgnoreCase("dul")) {
            prefix = PREFIX_DUL;
        } else if (namespace.equalsIgnoreCase("wikidata")) {
            prefix = Constants.PREFIX_WIKIDATA;
        }
        if (!queryString.contains(prefix)) {
            queryString = prefix + queryString;
        }
        return queryString;
    }

    static String getWikicatYAGOTypeName(String type) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean containsYago = type.toLowerCase().contains(Constants.YAGO);
        String[] typeArray = type.split("/");
        type = typeArray[typeArray.length - 1];
        // Split camel case or title case
        String[] array = type.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|(?<=[0-9])(?=[A-Z][a-z])|(?<=[a-zA-Z])(?=[0-9])");
        int synsetIDLength = 8;
        String prefixOne = "1";
        for (int i = 0; i < array.length; i++) {
            String s = array[i];
            if (s.equalsIgnoreCase(Constants.WIKICAT)) {
                continue;
            }
            if (s.equalsIgnoreCase(Constants.YAGO)) {
                containsYago = true;
                continue;
            }
            if (containsYago && i == array.length - 1 &&
                    s.startsWith(prefixOne) && s.length() == synsetIDLength + 1) {
                continue;
            }
            stringBuilder.append(s).append(" ");
        }
        return stringBuilder.toString().trim();
    }

    static String getMostSpecificSpotlightType(String types) {
        String[] typeArray = types.split(",");
        for (int i = typeArray.length - 1; i >= 0; i--) {
            if (typeArray[i].toLowerCase().contains(Constants.DBPEDIA)) {
                String ontologyName = typeArray[i].split(":")[1];
                return Constants.DBPEDIA_ONTOLOGY + ontologyName;
            }
        }
        return null;
    }
}
