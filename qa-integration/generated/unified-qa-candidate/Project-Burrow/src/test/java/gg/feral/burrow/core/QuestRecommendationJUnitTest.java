package gg.feral.burrow.core;

import org.junit.jupiter.api.Test;

/** The same offline projection checks are also selected by a genuine Gradle/JDK17 run. */
public final class QuestRecommendationJUnitTest {
    @Test void onlyRuneLiteConfirmedUnfinishedQuestCanBeRecommended() throws Exception {
        QuestRecommendationTest.main(new String[0]);
    }
}
