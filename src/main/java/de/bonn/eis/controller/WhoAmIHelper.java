package de.bonn.eis.controller;

import de.bonn.eis.model.LinkSUMResultRow;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by andy on 10/31/17.
 */
public class WhoAmIHelper {
    static LinkSUMResultRow getHardPropertyForWhoAmI(List<LinkSUMResultRow> linkSUMResults) {
        LinkSUMResultRow linkSUMResultRow;
        float lowerBound = 0.2f;
        float upperBound = 0.5f;
        float maxVRank = linkSUMResults.get(0).getVRank();
        linkSUMResultRow = getLinkSUMResultRowForVRankRange(linkSUMResults, lowerBound, upperBound);
        while (linkSUMResultRow == null && upperBound <= maxVRank) {
            lowerBound = upperBound;
            upperBound *= 2;
            linkSUMResultRow = getLinkSUMResultRowForVRankRange(linkSUMResults, lowerBound, upperBound);
        }
        return linkSUMResultRow;
    }

    private static LinkSUMResultRow getLinkSUMResultRowForVRankRange(List<LinkSUMResultRow> linkSUMResults, float lowerBound, float upperBound) {
        int randomIndex;
        int size = linkSUMResults.size();
        LinkSUMResultRow linkSUMResultRow = null;
        for (int i = size - 1; i >= 0; i--) {
            randomIndex = ThreadLocalRandom.current().nextInt(size);
            linkSUMResultRow = linkSUMResults.get(randomIndex);
            if (linkSUMResultRow.getVRank() > lowerBound && linkSUMResultRow.getVRank() < upperBound) {
                break;
            }
        }
        return linkSUMResultRow;
    }

}
