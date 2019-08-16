package parser;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LiveFootballParser {

    private static final int LIVE_BET_TIME = 60;
    private WebClient webClient;
    private Set<Notification> notifications = new HashSet<>();
    private Set<FootballMatch> liveEvents = new HashSet<>();

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

    public void parseLiveEvents() throws IOException {
        HtmlPage htmlPage = webClient.getPage("https://www.myscore.com.ua");
        webClient.waitForBackgroundJavaScript(10000);

        expandLeagues(htmlPage);
        webClient.waitForBackgroundJavaScript(6000);

        DomNodeList<DomNode> nodes = htmlPage.querySelectorAll("div.event__match--live");
        System.out.println("Parsed " + nodes.size() + " events");
        for (DomNode node : nodes) {
            liveEvents.add(extractEvent(node));
        }
        clearNotifications();

        Set<Notification> liveNotifications = getNotifications(liveEvents);
        for (Notification notification : liveNotifications) {
            System.out.println(notification);
            if (!notifications.contains(notification)) {
                notifications.add(notification);
                notifyInTelegram(notification.getMyscoreId(), notification.getStrategyResult());
            }
        }

        liveEvents.clear();
    }

    private void expandLeagues(HtmlPage htmlPage) {
        htmlPage.executeJavaScript("document.querySelectorAll('div.event__expander.expand').forEach(function(element) { element.click();})");
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

    private void clearNotifications() {
        notifications = notifications.stream()
                .filter(notification -> Duration.between(LocalDateTime.now(), notification.getDateTime()).toMinutes() > 30)
                .collect(Collectors.toSet());
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

    private Set<Notification> getNotifications(Set<FootballMatch> liveEvents) throws IOException {
        Set<Notification> notifications = new HashSet<>();

        for (FootballMatch event : liveEvents) {
            System.out.println(event);
            if (getTimeInNumberFormat(event) >= LIVE_BET_TIME) {
                if (isTwoDrawsTogether(event, 0) || isTwoDrawsTogether(event, 1)) {
                    String url = "https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#h2h;overall";
                    HtmlPage htmlPage = webClient.getPage(url);
                    DomNodeList<DomNode> tables = htmlPage.querySelectorAll("table.head_to_head");

                    DomNodeList<DomNode> firstTeamRows = tables.get(0).querySelectorAll("tbody tr");
                    DomNodeList<DomNode> secondTeamRows = tables.get(1).querySelectorAll("tbody tr");

                    if (isTwoDrawsTogether(firstTeamRows, 0) || isTwoDrawsTogether(secondTeamRows, 0)) {
                        notifications.add(new Notification(event.getMyscoreId(), LocalDateTime.now(), StrategyResult.TWO_LAST_GAMES_ZERO_DRAWS));
                    } else if (isTwoDrawsTogether(firstTeamRows, 1) || isTwoDrawsTogether(secondTeamRows, 1)) {
                        notifications.add(new Notification(event.getMyscoreId(), LocalDateTime.now(), StrategyResult.TWO_LAST_GAMES_ONE_GOAL_DRAWS));
                    }

                }
            }
        }
        return notifications;
    }

    private boolean hasOneTeamMoreRedCardsThanAnother(FootballMatch event) {
        return event.getFirstTeamStatistics().getRedCardCount() != event.getSecondTeamStatistics().getRedCardCount();
    }

    private boolean isTwoDrawsTogether(FootballMatch event, int goalCount) {
        return event.getFirstTeamStatistics().getGoalCount() == goalCount
                && event.getSecondTeamStatistics().getGoalCount() == goalCount;
    }

    private void notifyInTelegram(String myscoreId, StrategyResult result) throws IOException {
        TelegramSender.sendMessage(result.getDescription());
        TelegramSender.sendMessage("https://www.myscore.com.ua/match/" + myscoreId +"/#match-summary");
    }

    private boolean isTwoDrawsTogether(DomNodeList<DomNode> rows, int goalCount) {
        String firstScore = rows.get(0).querySelector("span.score").getTextContent();
        String secondScore = rows.get(1).querySelector("span.score").getTextContent();
        return isDraw(firstScore, goalCount) && isDraw(secondScore, goalCount);
    }

    private boolean isDraw(String score, int goalCount) {
        String[] scores = score.split(":");
        return Integer.parseInt(removeSpaces(scores[0])) == goalCount
                && Integer.parseInt(removeSpaces(scores[1])) == goalCount;
    }

    private int getTimeInNumberFormat(FootballMatch footballMatch) {
        try {
            return Integer.parseInt(removeSpaces(footballMatch.getStatus()));
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
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeOptions[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeOptions[1]));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
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
                parser.parseLiveEvents();
                Thread.sleep(1000 * 60 * 4);
            } catch (Exception e) {
                break;
            }
        }
        parser.closeWebClient();
    }
}
