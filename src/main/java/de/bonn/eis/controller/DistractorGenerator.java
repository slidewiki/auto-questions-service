package de.bonn.eis.controller;

import de.bonn.eis.model.DBPediaResource;
import de.bonn.eis.model.LinkSUMResultRow;
import de.bonn.eis.model.WhoAmIQuestionStructure;
import de.bonn.eis.utils.NLPConsts;
import de.bonn.eis.utils.SPARQLConsts;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import rita.RiTa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/3/17.
 */
class DistractorGenerator {
    static List<String> getExternalDistractors(DBPediaResource answer, String level) {
        List<String> resourceNames = new ArrayList<>();

        if (level.equals(NLPConsts.LEVEL_EASY)) {
            String baseType = QueryUtils.getBaseTypeForEasy(answer);
            return Queries.getNPopularDistractorsForBaseType(answer.getURI(), baseType, 3);
        }
        else if(level.equals(NLPConsts.LEVEL_HARD)) {
            List<String> types = Queries.getNMostSpecificTypes(answer.getURI(), 5, false);
            List<String> distractors = new ArrayList<>();
            for (String type: types) {
                distractors.addAll(Queries.getNPopularDistractorsForBaseType(answer.getURI(), type, 3));
                if(distractors.size() >= 3){
                    return distractors.subList(0, 3);
                }
            }
        }
        return resourceNames;
    }

    static List<String> getSelectQuestionDistractors(DBPediaResource answer, String level) {
        List<String> sisterTypes = new ArrayList<>();
        if (level.equalsIgnoreCase(NLPConsts.LEVEL_EASY)) {
            String types = answer.getTypes();
            if (!types.isEmpty()) {
                String[] typesArray = types.split(",");
                List<String> resourceTypeList = Arrays.asList(typesArray);
                resourceTypeList = resourceTypeList.stream().filter(type -> type.toLowerCase().contains(SPARQLConsts.DBPEDIA)).collect(Collectors.toList());
                if (resourceTypeList != null) {
                    int size = resourceTypeList.size();
                    sisterTypes.add(QueryUtils.getLabelOfSpotlightType(resourceTypeList.get(size - 1)));
                    for (int i = size - 1; i > 0; i--) {
                        String type = resourceTypeList.get(i);
                        String superType = resourceTypeList.get(i - 1);
                        String queryString =
                                SPARQLConsts.PREFIX_RDF + SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_OWL;
                        queryString = QueryUtils.addNsAndType(queryString, type, false);
                        queryString = QueryUtils.addNsAndType(queryString, superType, false);
                        type = QueryUtils.getNsAndTypeForSpotlightType(type);
                        superType = QueryUtils.getNsAndTypeForSpotlightType(superType);

                        queryString += "SELECT DISTINCT ?name FROM <http://dbpedia.org> WHERE {\n" +
                                //                            "<" + answer.getURI() + "> a " + type + " .\n" +
                                " ?t a owl:Class .\n" +
                                " ?t rdfs:subClassOf " + superType + " .\n" +
                                " ?t rdfs:label ?name .\n" +
                                " filter (?t != " + type + ") .\n" +
                                " filter (langMatches(lang(?name), \"EN\")) ." +
                                " filter not exists {<" + answer.getURI() + "> a ?t } ." +
                                "} order by rand() limit 3";
                        ResultSet resultSet = null;
                        try {
                            resultSet = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        QueryUtils.addResultsToList(resultSet, sisterTypes, "name");
                        if (sisterTypes.size() == 4) {
                            break;
                        } else if (sisterTypes.size() > 4) {
                            sisterTypes = sisterTypes.subList(0, 4);
                        }
                    }
                }
            } else {
                String queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_OWL;
                String resource = "<" + answer.getURI() + ">";
                queryString += "SELECT DISTINCT ?dName ?aName WHERE {\n" +
                        resource + " a ?t .\n" +
                        resource + " a ?a .\n" +
                        " ?t a owl:Class .\n" +
                        " ?a a owl:Class .\n" +
                        " ?d a owl:Class .\n" +
                        " ?d rdfs:label ?dName .\n" +
                        " ?a rdfs:label ?aName .\n" +
                        " ?d rdfs:subClassOf ?t .\n" +
                        " filter (langMatches(lang(?aName), \"EN\")) .\n" +
                        " filter (langMatches(lang(?dName), \"EN\")) .\n" +
                        " filter not exists{?s rdfs:subClassOf ?a}\n" +
                        " filter not exists{" + resource + " a ?d .}\n" +
                        "} order by rand() limit 3";

                ResultSet resultSet = null;
                try {
                    resultSet = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RDFNode answerType = null;
                if (resultSet != null) {
                    while (resultSet.hasNext()) {
                        QuerySolution result = resultSet.next();
                        if (result != null) {
                            answerType = result.get("aName");
                            RDFNode distractor = result.get("dName");
                            String literal = QueryUtils.getStringLiteral(distractor);
                            if (literal != null) {
                                sisterTypes.add(literal);
                            }
                        }
                    }
                    String literal = QueryUtils.getStringLiteral(answerType);
                    if (literal != null) {
                        sisterTypes.add(0, literal);
                    }
                }
            }
        } else if (level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)) {
            List<String> typeStrings = Queries.getNMostSpecificTypes(answer.getURI(), 10, false);
            //TODO Decide whether to use many types = distractors are of different types
            //TODO one type = distractors are of the same type e.g. all are rivers
            if(typeStrings == null || typeStrings.isEmpty()){
                return null;
            }
            String queryString;
            ResultSet resultSet;
            queryString = SPARQLConsts.PREFIX_RDFS + SPARQLConsts.PREFIX_FOAF;
            queryString += "SELECT DISTINCT ?dName ?aName ?d FROM <http://dbpedia.org> WHERE {\n";
            for (String typeString : typeStrings) {
                queryString += "{\n <" + typeString + "> rdfs:subClassOf ?st .\n" +
                        " optional { <" + typeString + "> rdfs:label ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                        " optional { <" + typeString + "> foaf:name ?aName. filter (langMatches(lang(?aName), \"EN\")) }\n" +
                        "}\n";
                if (typeStrings.indexOf(typeString) != typeStrings.size() - 1) {
                    queryString += SPARQLConsts.UNION;
                }
            }

            queryString += " ?d rdfs:subClassOf ?st .\n" +
                    " optional {?d rdfs:label ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                    " optional {?d foaf:name ?dName . filter (langMatches(lang(?dName), \"EN\"))}\n" +
                    " filter not exists{<" + answer.getURI() + "> a ?d .}\n" +
                    "} order by rand() limit 3";

            resultSet = null;
            try {
                resultSet = SPARQLClient.runSelectQuery(queryString, SPARQLConsts.DBPEDIA_SPARQL_SERVICE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            RDFNode answerType = null;
            if (resultSet != null) {
                while (resultSet.hasNext()) {
                    QuerySolution result = resultSet.next();
                    if (result != null) {
                        answerType = result.get("aName");
                        RDFNode distractor = result.get("dName");
                        String literal = QueryUtils.getStringLiteral(distractor);
                        if (literal != null) {
                            sisterTypes.add(literal);
                        } else {
                            RDFNode distractorNode = result.get("d");
                            String distractorString = distractorNode.toString();
                            sisterTypes.add(QueryUtils.getWikicatYAGOTypeName(distractorString));
                        }
                    }
                }
                String literal = QueryUtils.getStringLiteral(answerType);
                if (literal != null) {
                    sisterTypes.add(0, literal);
                } else {
                    int randomIndex = ThreadLocalRandom.current().nextInt(typeStrings.size());
                    sisterTypes.add(0, QueryUtils.getWikicatYAGOTypeName(typeStrings.get(randomIndex)));
                }
            }
        }
        // Singularize plural types
        ArrayList<String> singleTypes = new ArrayList<>();
        for (String sisterType : sisterTypes) {
            String[] typeArray = sisterType.split(" ");
            StringBuilder result = new StringBuilder();
            for (String s : typeArray) {
                String singular = RiTa.singularize(s);
                String plural = RiTa.pluralize(singular);
                if (plural.equalsIgnoreCase(s)) {
                    result.append(singular).append(" ");
                } else {
                    result.append(s).append(" ");
                }
            }
            singleTypes.add(result.toString().trim());
        }
        return singleTypes;
    }

    static WhoAmIQuestionStructure getWhoAmIQuestionAndDistractors(DBPediaResource resource, String level) {
        String uri = resource.getURI();
        List<String> mostSpecificTypes = Queries.getNMostSpecificTypes(uri, 1, true);
        String baseType = "";
        String baseTypeLabel = "";
        if(mostSpecificTypes != null && !mostSpecificTypes.isEmpty()){
            baseType = mostSpecificTypes.get(0);
        }
        if(baseType.isEmpty() || baseType.equalsIgnoreCase(SPARQLConsts.OWL_PERSON)){
            List<String> yagoTypes = Queries.getNMostSpecificYAGOTypesForDepthRange(uri, 10, 11, 14);
            if(!yagoTypes.isEmpty()){
                int randomIndex = ThreadLocalRandom.current().nextInt(yagoTypes.size());
                baseType = yagoTypes.get(randomIndex);
                baseTypeLabel = QueryUtils.getWikicatYAGOTypeName(baseType);
            }
        } else{
            baseTypeLabel = QueryUtils.getLabelOfType(baseType);
        }
        if(baseTypeLabel == null || baseTypeLabel.isEmpty()){
            return null;
        }

        WhoAmIQuestionStructure.WhoAmIQuestionStructureBuilder builder = WhoAmIQuestionStructure.builder();
        String resourceName = resource.getSurfaceForm();

        builder.baseType(RiTa.singularize(baseTypeLabel));
        builder.answer(resourceName);

        List<LinkSUMResultRow> linkSUMResults = Queries.getLinkSUMForResource(uri);
        int randomIndex;
        LinkSUMResultRow linkSUMResultRow, secondLinkSUMResultRow;
        List<LinkSUMResultRow> tempResults = new ArrayList<>();
        if(!linkSUMResults.isEmpty()){
            linkSUMResults.forEach(resultRow -> {
                if (resultRow != null) {
                    if (resultRow.getObjectLabel() != null && resultRow.getSubjectLabel() != null
                            && resultRow.getPredicateLabel() != null) {
                        if (!resultRow.isIdentityRevealed(resourceName)) {
                            tempResults.add(resultRow);
                        }
                    }
                }
            });
            linkSUMResults = tempResults;
            int size = linkSUMResults.size();
            if(level.equalsIgnoreCase(NLPConsts.LEVEL_EASY)){
                int bound = size <= 10 ? size : 10;
                randomIndex = ThreadLocalRandom.current().nextInt(bound);
                linkSUMResultRow = linkSUMResults.get(randomIndex);
                linkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 1);

                int secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                int i = bound;
                while(secondRandomIndex == randomIndex && i >= 0){
                    secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                    i--;
                }
                secondLinkSUMResultRow = linkSUMResults.get(secondRandomIndex);
                i = bound;
                while (linkSUMResultRow.getPredicateLabel().equalsIgnoreCase(secondLinkSUMResultRow.getPredicateLabel())
                        && i >= 0){
                    secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                    secondLinkSUMResultRow = linkSUMResults.get(secondRandomIndex);
                    i--;
                }
                secondLinkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 2);
                List<String> distractors = Queries.getNPopularDistractorsForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3, 1);
                builder.distractors(distractors);

            } else if(level.equalsIgnoreCase(NLPConsts.LEVEL_HARD)){
                linkSUMResultRow = WhoAmIHelper.getHardPropertyForWhoAmI(linkSUMResults);
                linkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 1);

                secondLinkSUMResultRow = WhoAmIHelper.getHardPropertyForWhoAmI(linkSUMResults);
                int i = size;
                while (linkSUMResultRow == secondLinkSUMResultRow ||
                        linkSUMResultRow.getPredicateLabel().equalsIgnoreCase(secondLinkSUMResultRow.getPredicateLabel())
                                && i >= 0){
                    secondLinkSUMResultRow = WhoAmIHelper.getHardPropertyForWhoAmI(linkSUMResults);
                    i--;
                }
                secondLinkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 2);
                List<String> distractors = Queries.getNPopularDistractorsForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3, 2);
                if(distractors.isEmpty() || distractors.size() < 3){
                    distractors.addAll(Queries.getNPopularDistractorsForBaseType(uri, baseType, secondLinkSUMResultRow, linkSUMResultRow, 3 - distractors.size(), 2));
                    if(distractors.isEmpty() || distractors.size() < 3){
                        distractors.addAll(Queries.getNPopularDistractorsForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3 - distractors.size(), 1));
                    }
                }
                builder.distractors(distractors);
            }
            WhoAmIVerbaliser whoAmIVerbaliser = new WhoAmIVerbaliser();
            builder.question(whoAmIVerbaliser.verbalise(builder.build()));
            return builder.build();
        }
        return null;
    }
}
