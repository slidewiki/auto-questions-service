package de.bonn.eis.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Created by andy on 4/7/17.
 */
@Data
@Builder
public class LinkSUMResultRow {
    private String subject;
    private String predicate;
    private String object;
    private String subjectLabel;
    private String predicateLabel;
    private String objectLabel;
    private float vRank;

    public boolean isIdentityRevealed(String resourceName) {
        String[] nameParts = resourceName.split(" ");
        for (String namePart : nameParts) {
            String subject = getSubjectLabel();
            String object = getObjectLabel();
            if(subject.contains(namePart) && object.contains(namePart)){
                return true;
            }
        }
        return false;
    }

    public void getWhoAmIFromLinkSUMRow(WhoAmIQuestionStructure.WhoAmIQuestionStructureBuilder builder, int propNo) {
        switch (propNo){
            case 1:
                if(getSubject() == null)
                {
                    builder.firstPredicate(getPredicateLabel())
                            .firstObject(getObjectLabel());
                }
                else if(getObject() == null)
                {
                    builder.firstPredicate(getPredicateLabel())
                            .firstSubject(getSubjectLabel());
                }
                break;
            case 2:
                if(getSubject() == null)
                {
                    builder.secondPredicate(getPredicateLabel())
                            .secondObject(getObjectLabel());
                }
                else if(getObject() == null)
                {
                    builder.secondPredicate(getPredicateLabel())
                            .secondSubject(getSubjectLabel());
                }
        }
    }
}
