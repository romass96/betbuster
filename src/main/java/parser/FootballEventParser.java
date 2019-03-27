package parser;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FootballEventParser extends SportEventParser {
    @Override
    public String getUrl() {
        return "https://www.myscore.com.ua";
    }

    @Override
    public String getLeagueSelector() {
        return "div.event__header";
    }

    @Override
    public List<SportEvent> extractMatches(DomNodeList<DomNode> matchNodes, Calendar calendar) {
        List<SportEvent> events = new ArrayList<>();
        for (DomNode matchNode : matchNodes) {
            FootballMatch match = new FootballMatch();
            match.setFirstPlayer(matchNode.querySelector("td.team-home span.padr").getTextContent());
            match.setSecondPlayer(matchNode.querySelector("td.team-away span.padl").getTextContent());
            match.setStartDate(getStartDate(matchNode.querySelector("td.time-playing, td.time").getTextContent(), calendar));

            if (hasClass(matchNode, "stage-live") || hasClass(matchNode, "stage-finished")) {
                match.setStatus(matchNode.querySelector("td.timer span").getTextContent());

                String score = matchNode.querySelector("td.score").getTextContent();
                if (!score.trim().equals("-")) {
                    match.setScore(score);
                }

                String firstTimeScore = matchNode.querySelector("td.part-top").getTextContent();
                if (firstTimeScore.length() >= 5) {
                    match.setFirstTimeScore(firstTimeScore.substring(1, firstTimeScore.length() - 1));
                }
            }

            events.add(match);
        }
        return events;
    }
}
