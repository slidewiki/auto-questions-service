package de.bonn.eis.controller;

import com.google.common.collect.ImmutableListMultimap;
import de.bonn.eis.model.*;
import de.bonn.eis.utils.Constants;
import de.bonn.eis.utils.QGenLogger;
import de.bonn.eis.utils.QGenUtils;
import rita.RiTa;
import rita.RiWordNet;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by Ainuddin Faizan on 1/3/17.
 */
class QuestionAndDistractorGenerator {
    private static final String BLANK = "________";
    private static final String SELECT_QUESTION_TEXT = " is a: ";
    private ServletContext servletContext;
    private List<DBPediaResource> dbPediaResources;
    private String level;

    QuestionAndDistractorGenerator(ServletContext servletContext, List<DBPediaResource> dbPediaResources, String level) {
        this.servletContext = servletContext;
        this.dbPediaResources = dbPediaResources;
        this.level = level;
    }

    List<QuestionSetPerSlide> getGapFillQuestions(String text, List<NlpProcessResults> nlpProcessResults, List<GapFillDistractor> distractorsPerResource) {
        List<QuestionSetPerSlide> questionSetPerSlideList = new ArrayList<>();
        QuestionSetPerSlide.QuestionSetPerSlideBuilder builder;
        List<Question> questions;

        if (text != null) {
            builder = QuestionSetPerSlide.builder();
            questions = getGapFillQuestionSetForText(text, distractorsPerResource);
            builder.questionSet(questions);
            questionSetPerSlideList.add(builder.build());
        } else if (nlpProcessResults != null) {
            // Each process result is for a slide
            for (NlpProcessResults result : nlpProcessResults) {
                builder = QuestionSetPerSlide.builder();
                questions = getGapFillQuestionSetForText(result.getSlideTitleAndText(), distractorsPerResource);
                builder.
                        slideId(result.getSlideId()).
                        questionSet(questions);
                questionSetPerSlideList.add(builder.build());
            }
        }
        return questionSetPerSlideList;
    }

    private List<Question> getGapFillQuestionSetForText(String text, List<GapFillDistractor> distractorsPerResource) {
        List<Question> gapFillQuestionSets = new ArrayList<>();
        String cleanText = text.replaceAll("/\\s*(?:[\\dA-Z]+\\.|[a-z]\\)|•)+/gm", ". ");
        cleanText = cleanText.replaceAll("/(\r\n|\n|\r)/gm", ". ");
        cleanText = cleanText.replaceAll("\n", ". ");
        LanguageProcessor processor = new LanguageProcessor(cleanText);
        List<String> sentences = processor.getSentences();

        for (String s : sentences) {
            for (GapFillDistractor gapFillDistractor : distractorsPerResource) {
                String resourceName = gapFillDistractor.getSurfaceForm();
                String pluralResourceName = gapFillDistractor.getPluralSurfaceForm();
                MCQQuestion.MCQQuestionBuilder builder = MCQQuestion.builder();
                if (!s.equalsIgnoreCase(resourceName + ".") && QGenUtils.sourceHasWordIgnoreCase(s, resourceName)) {
                    String questionText = s.replaceAll("(?i)\\b" + resourceName + "\\b", BLANK);
                    if (QGenUtils.sourceHasWordIgnoreCase(s, pluralResourceName)) {
                        questionText = questionText.replaceAll("(?i)\\b" + pluralResourceName + "\\b", BLANK);
                        resourceName = resourceName + ", " + pluralResourceName;
                    }
                    List<String> inTextDistractors = gapFillDistractor.getInTextDistractors();
                    String finalQuestionText = questionText;
                    inTextDistractors = inTextDistractors.stream().filter(d -> !QGenUtils.sourceHasWordIgnoreCase(finalQuestionText, d)).collect(Collectors.toList());
                    builder.
                            questionText(questionText).
                            answer(resourceName).
                            inTextDistractors(inTextDistractors).
                            externalDistractors(gapFillDistractor.getExternalDistractors());
                    gapFillQuestionSets.add(builder.build());
                }
            }
        }
        if (gapFillQuestionSets.isEmpty()) {
            return null;
        }
        return gapFillQuestionSets;
    }

    List<GapFillDistractor> getGapFillDistractors() {

        List<GapFillDistractor> distractorsPerResource = new ArrayList<>();
        String env = servletContext.getInitParameter("env");
        boolean envIsDev = env == null || !env.equalsIgnoreCase("prod");

        String dir = System.getProperty("user.dir");
        RiWordNet wordnet = new RiWordNet(dir + "/wordnet/");

        ImmutableListMultimap<String, DBPediaResource> mapOfGroupedResources = TextInfoRetriever.groupResourcesByType(dbPediaResources);
        for (DBPediaResource resource : dbPediaResources) {
            String surfaceForm = resource.getSurfaceForm();
            String plural = RiTa.pluralize(surfaceForm);
            if (envIsDev) {
                QGenLogger.info(surfaceForm);
            }
            List<String> externalDistractors = getExternalDistractors(resource, level);
            if (externalDistractors == null) {
                externalDistractors = LanguageProcessor.attemptToGetSynonyms(wordnet, resource.getSurfaceForm());
            }
            List<String> inTextDistractors = mapOfGroupedResources.get(resource.getTypes()).stream().
                    filter(res -> (!res.equals(resource) && !res.getSurfaceForm().equalsIgnoreCase(resource.getSurfaceForm())))
                    .map(DBPediaResource::getSurfaceForm).collect(Collectors.toList());
            QGenUtils.removeDuplicatesFromStringList(inTextDistractors);
            GapFillDistractor.GapFillDistractorBuilder builder = GapFillDistractor.builder();
            builder.
                    surfaceForm(resource.getSurfaceForm()).
                    pluralSurfaceForm(plural).
                    resourceURI(resource.getURI()).
                    inTextDistractors(inTextDistractors).
                    externalDistractors(externalDistractors);
            distractorsPerResource.add(builder.build());
        }
        return distractorsPerResource;
    }

    private List<String> getExternalDistractors(DBPediaResource answer, String level) {
        List<String> resourceNames = new ArrayList<>();

        if (level.equals(Constants.LEVEL_EASY)) {
            String baseType = QueryUtils.getBaseTypeForEasy(answer);
            return Queries.getDistractorsByPopularityForBaseType(answer.getURI(), baseType, 3);
        } else if (level.equals(Constants.LEVEL_HARD)) {
            List<String> types = Queries.getNMostSpecificTypes(answer.getURI(), 5, false);
            List<String> distractors = new ArrayList<>();
            for (String type : types) {
                distractors.addAll(Queries.getDistractorsByPopularityForBaseType(answer.getURI(), type, 3));
                if (distractors.size() >= 3) {
                    return distractors.subList(0, 3);
                }
            }
        }
        return resourceNames;
    }

    List<MCQQuestion> getSelectQuestions() {
        if (dbPediaResources != null && !dbPediaResources.isEmpty()) {
            List<MCQQuestion> selectQuestions = new ArrayList<>();
            dbPediaResources.forEach(resource -> {
                MCQQuestion.MCQQuestionBuilder questionBuilder = MCQQuestion.builder();
                List<String> answerAndDistractors = getSelectQuestionDistractors(resource, level);
                if (answerAndDistractors != null && !answerAndDistractors.isEmpty()) {
                    String answer = answerAndDistractors.get(0);
                    if (!answer.trim().isEmpty()) {
                        questionBuilder.questionText(resource.getSurfaceForm() + SELECT_QUESTION_TEXT)
                                .answer(answer);
                        if (answerAndDistractors.size() > 1) {
                            questionBuilder.externalDistractors(answerAndDistractors.subList(1, answerAndDistractors.size()));
                        }
                        selectQuestions.add(questionBuilder.build());
                    }
                }
            });
            return selectQuestions;
        }
        return null;
    }

    private List<String> getSelectQuestionDistractors(DBPediaResource answer, String level) {
        List<String> sisterTypes = new ArrayList<>();
        if (level.equalsIgnoreCase(Constants.LEVEL_EASY)) {
            String types = answer.getTypes();
            if (!types.isEmpty()) {
                sisterTypes = Queries.getEasySiblingTypesFromSpotlightTypes(answer, sisterTypes, types);
            } else {
                Queries.getEasySiblingTypesForResource(answer, sisterTypes);
            }
        } else if (level.equalsIgnoreCase(Constants.LEVEL_HARD)) {
            List<String> typeStrings = Queries.getNMostSpecificTypes(answer.getURI(), 10, false);
            //TODO Decide whether to use many types = distractors are of different types
            //TODO one type = distractors are of the same type e.g. all are rivers
            if (typeStrings == null || typeStrings.isEmpty()) {
                return null;
            }
            Queries.getHardSiblingTypes(answer, sisterTypes, typeStrings);
        }
        return LanguageProcessor.singularizePluralTypes(sisterTypes);
    }

    List<MCQQuestion> getWhoamIQuestions() {
        if (dbPediaResources != null && !dbPediaResources.isEmpty()) {
            List<MCQQuestion> questions = new ArrayList<>();
            dbPediaResources.forEach(resource -> {
                WhoAmIQuestionStructure whoAmIQuestionStructureAndAnswers = getWhoAmIQuestionAndDistractors(resource, level);
                if (whoAmIQuestionStructureAndAnswers != null) {
                    MCQQuestion.MCQQuestionBuilder builder = MCQQuestion.builder();
                    builder
                            .questionText(whoAmIQuestionStructureAndAnswers.getQuestion())
                            .answer(whoAmIQuestionStructureAndAnswers.getAnswer())
                            .externalDistractors(whoAmIQuestionStructureAndAnswers.getDistractors());
                    questions.add(builder.build());
                }
            });
            return questions;
        }
        return null;
    }

    private WhoAmIQuestionStructure getWhoAmIQuestionAndDistractors(DBPediaResource resource, String level) {
        String uri = resource.getURI();
        List<String> mostSpecificTypes = Queries.getNMostSpecificTypes(uri, 1, true);
        String baseType = "";
        String baseTypeLabel = "";
        if (mostSpecificTypes != null && !mostSpecificTypes.isEmpty()) {
            baseType = mostSpecificTypes.get(0);
        }
        if (baseType.isEmpty() || baseType.equalsIgnoreCase(Constants.OWL_PERSON)) {
            List<String> yagoTypes = Queries.getNMostSpecificYAGOTypesForDepthRange(uri, 10, 11, 14);
            if (!yagoTypes.isEmpty()) {
                int randomIndex = ThreadLocalRandom.current().nextInt(yagoTypes.size());
                baseType = yagoTypes.get(randomIndex);
                baseTypeLabel = QueryUtils.getWikicatYAGOTypeName(baseType);
            }
        } else {
            baseTypeLabel = QueryUtils.getLabelOfType(baseType);
        }
        if (baseTypeLabel == null || baseTypeLabel.isEmpty()) {
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
        if (!linkSUMResults.isEmpty()) {
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
            if (level.equalsIgnoreCase(Constants.LEVEL_EASY)) {
                int bound = size <= 10 ? size : 10;
                randomIndex = ThreadLocalRandom.current().nextInt(bound);
                linkSUMResultRow = linkSUMResults.get(randomIndex);
                linkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 1);

                int secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                int i = bound;
                while (secondRandomIndex == randomIndex && i >= 0) {
                    secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                    i--;
                }
                secondLinkSUMResultRow = linkSUMResults.get(secondRandomIndex);
                i = bound;
                while (linkSUMResultRow.getPredicateLabel().equalsIgnoreCase(secondLinkSUMResultRow.getPredicateLabel())
                        && i >= 0) {
                    secondRandomIndex = ThreadLocalRandom.current().nextInt(bound);
                    secondLinkSUMResultRow = linkSUMResults.get(secondRandomIndex);
                    i--;
                }
                secondLinkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 2);
                List<String> distractors = Queries.getDistractorsByPopularityForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3, 1);
                builder.distractors(distractors);

            } else if (level.equalsIgnoreCase(Constants.LEVEL_HARD)) {
                linkSUMResultRow = WhoAmIHelper.getHardPropertyForWhoAmI(linkSUMResults);
                linkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 1);

                secondLinkSUMResultRow = WhoAmIHelper.getHardPropertyForWhoAmI(linkSUMResults);
                int i = size;
                while (linkSUMResultRow == secondLinkSUMResultRow ||
                        linkSUMResultRow.getPredicateLabel().equalsIgnoreCase(secondLinkSUMResultRow.getPredicateLabel())
                                && i >= 0) {
                    secondLinkSUMResultRow = WhoAmIHelper.getHardPropertyForWhoAmI(linkSUMResults);
                    i--;
                }
                secondLinkSUMResultRow.getWhoAmIFromLinkSUMRow(builder, 2);
                List<String> distractors = Queries.getDistractorsByPopularityForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3, 2);
                if (distractors.isEmpty() || distractors.size() < 3) {
                    distractors.addAll(Queries.getDistractorsByPopularityForBaseType(uri, baseType, secondLinkSUMResultRow, linkSUMResultRow, 3 - distractors.size(), 2));
                    if (distractors.isEmpty() || distractors.size() < 3) {
                        distractors.addAll(Queries.getDistractorsByPopularityForBaseType(uri, baseType, linkSUMResultRow, secondLinkSUMResultRow, 3 - distractors.size(), 1));
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
