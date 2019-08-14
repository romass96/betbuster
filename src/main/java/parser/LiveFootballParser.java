package parser;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class LiveFootballParser {

    private static final int LIVE_BET_TIME = 60;
    private WebClient webClient;
    private Map<FootballMatch, StrategyResult> notifications = new HashMap<>();
    private Set<FootballMatch> liveEvents;

    public void initWebClient() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
    }

    public void closeWebClient() {
        webClient.close();
    }

    public void parseTodaySportEvents() throws IOException {
        liveEvents = new HashSet<>();
        HtmlPage htmlPage = webClient.getPage("https://www.myscore.com.ua");
        webClient.waitForBackgroundJavaScript(10000);

        expandLeagues(htmlPage);
        webClient.waitForBackgroundJavaScript(6000);

        DomNodeList<DomNode> nodes = htmlPage.querySelectorAll("div.event > div.event__match--live");
        for (DomNode node : nodes) {
            liveEvents.add(extractEvent(node));
        }
        cleanNonLiveNotifications();
        filterResults();
    }

    private League extractLeague(DomNode node) {
        League league = new League();
        league.setName(node.querySelector("span.event__title--name").getTextContent());
        league.setRegion(node.querySelector("div.event__title").getTextContent());

        return league;
    }

    private FootballMatch extractEvent(DomNode node) {
        FootballMatch event = new FootballMatch();
        event.setFirstPlayer(node.querySelector("div.event__participant.event__participant--home").getTextContent().trim());
        event.setSecondPlayer(node.querySelector("div.event__participant.event__participant--away").getTextContent().trim());
        event.setMyscoreId(extractMyscoreId(node));
        event.setFirstTeamStatistics(extractFirstTeamStatistics(node));
        event.setSecondTeamStatistics(extractSecondTeamStatistics(node));

        DomNode timeNode = node.querySelector("div.event__stage");
        if (timeNode != null) {
            event.setStatus(timeNode.getTextContent().trim());
        } else {
            timeNode = node.querySelector("div.event__time");
            String time= timeNode.getFirstChild().getTextContent();
            event.setStartDate(getStartDate(time));
        }

        return event;
    }

    private TeamStatistics extractFirstTeamStatistics(DomNode node) {
        TeamStatistics teamStatistics = new TeamStatistics();
        teamStatistics.setRedCardCount(node.querySelectorAll("event__participant event__participant--home icon--redCard").size());
        teamStatistics.setGoalCount(Integer.parseInt(removeSpaces(node.querySelector("div.event__scores span:first-child").getTextContent())));
        return teamStatistics;
    }

    private TeamStatistics extractSecondTeamStatistics(DomNode node) {
        TeamStatistics teamStatistics = new TeamStatistics();
        teamStatistics.setRedCardCount(node.querySelectorAll("event__participant event__participant--away icon--redCard").size());
        teamStatistics.setGoalCount(Integer.parseInt(removeSpaces(node.querySelector("div.event__scores span:last-child").getTextContent())));
        return teamStatistics;
    }

    private String extractMyscoreId(DomNode node) {
        return node.getAttributes().getNamedItem("id").getNodeValue().substring(4);
    }

    private void filterResults() throws IOException {
        for (FootballMatch event : liveEvents) {
            if (getTimeInNumberFormat(event) >= LIVE_BET_TIME) {
                if (isNoGoalsInGame(event)) {
                    String url = "https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#h2h;overall";
                    HtmlPage htmlPage = webClient.getPage(url);
                    DomNodeList<DomNode> tables = htmlPage.querySelectorAll("table.head_to_head");

                    DomNodeList<DomNode> firstTeamRows = tables.get(0).querySelectorAll("tbody tr");
                    DomNodeList<DomNode> secondTeamRows = tables.get(1).querySelectorAll("tbody tr");

                    if (isZeroScore(3, firstTeamRows) || isZeroScore(3, secondTeamRows)) {
                        notifyInTelegram(event, StrategyResult.THREE_LAST_GAMES_ZERO_DRAWS);
                    } else if (isZeroScore(2, firstTeamRows) || isZeroScore(2, secondTeamRows)) {
                        notifyInTelegram(event, StrategyResult.TWO_LAST_GAMES_ZERO_DRAWS);
                    } else if (isZeroScore(1, firstTeamRows) && isZeroScore(1, secondTeamRows)) {
                        notifyInTelegram(event, StrategyResult.ZERO_DRAWS_FOR_ALL_TEAMS);
                    } else if (isDraw(event)) {
                        notifyInTelegram(event, StrategyResult.DRAW);
                    }

                } else if (hasOneTeamMoreRedCardsThanAnother(event) && isDraw(event)) {
                    notifyInTelegram(event, StrategyResult.RED_CARD);
                }
            }
        }
    }

    private void cleanNonLiveNotifications() {
        Iterator<Map.Entry<FootballMatch, StrategyResult>> iterator = notifications.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FootballMatch, StrategyResult> entry = iterator.next();
            if (!liveEvents.contains(entry.getKey())) {
                notifications.remove(entry.getKey());
            }
        }
    }

    private boolean hasOneTeamMoreRedCardsThanAnother(FootballMatch event) {
        return event.getFirstTeamStatistics().getRedCardCount() != event.getSecondTeamStatistics().getRedCardCount();
    }

    private boolean isDraw(FootballMatch event) {
        return event.getFirstTeamStatistics().getGoalCount() == event.getSecondTeamStatistics().getGoalCount();
    }

    private boolean isNoGoalsInGame(FootballMatch event) {
        return event.getFirstTeamStatistics().getGoalCount() == 0 && event.getSecondTeamStatistics().getGoalCount() == 0;
    }

    private void notifyInTelegram(FootballMatch event, StrategyResult result) throws IOException {
        if (!isNotifiedEvent(event, result)) {
            TelegramSender.sendMessage(result.getDescription());
            TelegramSender.sendMessage("https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#match-summary");
            notifications.put(event, result);
        }
    }

    private boolean isNotifiedEvent(FootballMatch event, StrategyResult result) {
        StrategyResult value = notifications.get(event);
        return value != null && value.equals(result);
    }

    private boolean isZeroScore(int count, DomNodeList<DomNode> rows) {
        int zeroScoreCounter = 0;
        for (int i = 0; i < 4; i++) {
            String score = rows.get(i).querySelector("span.score").getTextContent();
            if (isZeroScore(score)) {
                zeroScoreCounter++;
            }
            if (zeroScoreCounter == count) {
                return true;
            }
        }
        return false;
    }

    private boolean isZeroScore(String score) {
        String[] scores = score.split(":");
        return Integer.valueOf(removeSpaces(scores[0])) == 0
                && Integer.valueOf(removeSpaces(scores[1])) == 0;
    }

    private int getTimeInNumberFormat(FootballMatch footballMatch) {
        try {
            return Integer.valueOf(removeSpaces(footballMatch.getStatus()));
        } catch (Exception e) {
            return 0;
        }
    }

    private String removeSpaces(String dirtyString) {
        return dirtyString.replaceAll("[^0-9]", "");
    }

    private Date getStartDate(String time) {
        Calendar calendar = new GregorianCalendar();
        String[] timeOptions = time.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timeOptions[0]));
        calendar.set(Calendar.MINUTE, Integer.valueOf(timeOptions[1]));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void expandLeagues(HtmlPage htmlPage) {
        htmlPage.executeJavaScript("document.querySelectorAll('div.event__expander.expand').forEach(function(element) { element.click();})");
    }


    private boolean hasClass(DomNode domNode, String className) {
        String classes = domNode.getAttributes().getNamedItem("class").getNodeValue();
        return classes.contains(className);
    }

    public static void main(String[] args) {
        LiveFootballParser parser = new LiveFootballParser();
        parser.initWebClient();
        while (true) {
            try {
                parser.parseTodaySportEvents();
                Thread.sleep(1000 * 60 * 4);
            } catch (Exception e) {

            }
        }
    }
}
