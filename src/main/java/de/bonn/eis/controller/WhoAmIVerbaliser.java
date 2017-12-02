package de.bonn.eis.controller;

import de.bonn.eis.model.Question;
import de.bonn.eis.model.WhoAmIQuestionStructure;
import de.bonn.eis.utils.QGenUtils;
import rita.RiTa;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by andy on 4/11/17.
 */
public class WhoAmIVerbaliser implements Verbaliser{

    private static final String I_AM_A = "I am a ";
    private static final String FULL_STOP_AND_SPACE = ". ";
    private static final String I_WAS = "I was ";
    private static final String THE = "the ";
    private static final String AND = " and ";
    private static final String OF = " of ";
    private static final String MY = "My ";
    private static final String IS = " is ";
    private static final String SUCCESSOR = "successor";
    private static final String PREDECESSOR = "predecessor";
    private static final String BEFORE = "before";
    private static final String AFTER = "after";
    private static final String SPACE = " ";
    private static final String I_AM_THE = "I am the ";
    private static final String INFLUENCED = "influenced";
    private static final String WHO_AM_I = "Who am I?";
    private static final String KNOWN_FOR = "known for";
    private static final String NAMED_FOR = "named for";
    private static final String ME = " me";
    private static final String I_AM_AN = "I am an ";
    private Map<String, String> predicateToPhraseMap;

    public WhoAmIVerbaliser() {
        predicateToPhraseMap = new LinkedHashMap<>();
        predicateToPhraseMap = populatePredicateMap();
    }

    private Map<String, String> populatePredicateMap() {
        predicateToPhraseMap.put("starring", "I starred in ");
        predicateToPhraseMap.put("spouse", "I am married to ");
        predicateToPhraseMap.put("birth place", "I was born in ");
        predicateToPhraseMap.put("death place", "I died in ");
        predicateToPhraseMap.put("team", "I played for ");
        predicateToPhraseMap.put("club", "I played for ");
        predicateToPhraseMap.put("clubs", "I played for ");
        predicateToPhraseMap.put("residence", "I live in ");
        predicateToPhraseMap.put("currentclub", "I play for ");
        predicateToPhraseMap.put("alma mater", "I studied at ");
        predicateToPhraseMap.put("award", "I won the ");
        return predicateToPhraseMap;
    }

    @Override
    public String verbalise(Question question) {
        if(question != null && question instanceof WhoAmIQuestionStructure){
            WhoAmIQuestionStructure whoAmIQuestionStructure = (WhoAmIQuestionStructure) question;
            StringBuilder questionText = new StringBuilder();
            String baseType = whoAmIQuestionStructure.getBaseType().toLowerCase();
            String firstSubject = whoAmIQuestionStructure.getFirstSubject();
            String firstPredicate = whoAmIQuestionStructure.getFirstPredicate();
            String firstObject = whoAmIQuestionStructure.getFirstObject();
            String secondSubject = whoAmIQuestionStructure.getSecondSubject();
            String secondPredicate = whoAmIQuestionStructure.getSecondPredicate();
            String secondObject = whoAmIQuestionStructure.getSecondObject();

            boolean firstPredicateHasNoValue = firstPredicate == null || firstPredicate.isEmpty();
            boolean secondPredicateHasNoValue = secondPredicate == null || secondPredicate.isEmpty();

            if(firstPredicateHasNoValue &&
                    secondPredicateHasNoValue){
                return null;
            }

            if(startWithAVowel(baseType)){
                questionText.append(I_AM_AN);
            } else {
                questionText.append(I_AM_A);
            }
            questionText.append(baseType).
                    append(FULL_STOP_AND_SPACE);

            if(!firstPredicateHasNoValue){
                getVerbalisationOfTriple(questionText, firstSubject, firstPredicate, firstObject, true);
            }

            if(!secondPredicateHasNoValue){
                if(!firstPredicateHasNoValue){
                    questionText.append(AND);
                }
                getVerbalisationOfTriple(questionText, secondSubject, secondPredicate, secondObject, false);
            }
            questionText.append(FULL_STOP_AND_SPACE);
            questionText.append(WHO_AM_I);
            return questionText.toString();
        }
        return null;
    }

    private boolean startWithAVowel(String baseType) {
        String vowels = "aeiou";
        return vowels.indexOf(Character.toLowerCase(baseType.charAt(0))) != -1;
    }

    private void getVerbalisationOfTriple(StringBuilder questionText, String subject, String predicate, String object, boolean isFirstPart) {
        String predicateString = predicateToPhraseMap.getOrDefault(predicate, null);
        if(predicateString != null){
            questionText.append(predicateString).
                    append(subject == null ? object : subject);
        } else {
            predicate = RiTa.singularize(predicate);
            if(subject == null) {
                if(QGenUtils.sourceHasWordIgnoreCase(predicate, "by")){
                    questionText.append(I_WAS).
                            append(predicate).
                            append(SPACE).
                            append(object);
                } else if(predicate.equalsIgnoreCase(KNOWN_FOR)) {
                    questionText.append("I am ").
                            append(predicate).
                            append(SPACE).
                            append(object);
                } else if(predicate.equalsIgnoreCase(NAMED_FOR)) {
                    questionText.append(object).
                            append(IS).
                            append(predicate).
                            append(ME);
                } else if(predicate.equalsIgnoreCase(BEFORE)
                        || predicate.equalsIgnoreCase(AFTER)){
                    questionText.append(I_AM_THE).
                            append(predicate.equalsIgnoreCase(BEFORE) ? PREDECESSOR : SUCCESSOR).
                            append(OF).
                            append(object);
                } else if (predicate.endsWith("er") ||
                        predicate.endsWith("or")){
                    if(predicate.equalsIgnoreCase(SUCCESSOR)
                            || predicate.equalsIgnoreCase(PREDECESSOR)){
                        questionText.append(I_AM_THE).
                                append(predicate).
                                append(OF).
                                append(object);
                    } else {
                        questionText.append(I_WAS).
                                append(THE).
                                append(predicate).
                                append(OF).
                                append(object);
                    }
                } else if(predicate.equalsIgnoreCase(INFLUENCED)) {
                    questionText.append("I ").
                            append(predicate).
                            append(SPACE).
                            append(object);
                } else {
                    String my = isFirstPart ? MY : MY.toLowerCase();
                    questionText.append(my).
                            append(predicate).
                            append(IS).
                            append(object);
                }
            } else if(object == null) {
                if(QGenUtils.sourceHasWordIgnoreCase(predicate, "by")){
                    questionText.append(subject).
                            append(" was ").
                            append(predicate).
                            append(ME);
                } else if(predicate.equalsIgnoreCase(KNOWN_FOR)) {
                    questionText.append(subject).
                            append(IS).
                            append(predicate).
                            append(SPACE).
                            append(ME);
                } else if(predicate.equalsIgnoreCase(BEFORE)
                        || predicate.equalsIgnoreCase(AFTER)){
                    questionText.append(subject).
                            append(" is my ").
                            append(predicate.equalsIgnoreCase(BEFORE) ? PREDECESSOR : SUCCESSOR);
                } else if (predicate.endsWith("er") ||
                        predicate.endsWith("or")){
                    if(predicate.equalsIgnoreCase(SUCCESSOR)
                            || predicate.equalsIgnoreCase(PREDECESSOR)){
                        questionText.append(subject).
                                append(" is my ").
                                append(predicate);
                    } else {
                        questionText.append(I_WAS).
                                append(THE).
                                append(predicate).
                                append(OF).
                                append(subject);
                    }
                } else if(predicate.equalsIgnoreCase(INFLUENCED)) {
                    questionText.append(subject).
                            append(SPACE).
                            append(predicate).
                            append(ME);
                } else {
                    questionText.append(I_WAS).
                            append(THE).
                            append(predicate).
                            append(OF).
                            append(subject);
                }
            }
        }
    }

    //TODO Use POS tag for advanced mapping
    private String getPredicatePhrase (String predicate) {

        return null;
    }
}
