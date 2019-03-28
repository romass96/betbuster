package parser;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FootballParser {

    private static final int LIVE_BET_TIME = 60;
    private WebClient webClient;

    private void initWebClient() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
    }

    private void closeWebClient() {
        webClient.close();
    }

    public void parseTodaySportEvents() throws IOException {
        initWebClient();

        HtmlPage htmlPage = webClient.getPage("https://www.myscore.com.ua");
        webClient.waitForBackgroundJavaScript(10000);

        expandLeagues(htmlPage);
        webClient.waitForBackgroundJavaScript(6000);

        DomNodeList<DomNode> nodes = htmlPage.querySelectorAll("div.event > div");

        LinkedList<League> leagues = new LinkedList<>();
        for (DomNode node : nodes) {
            if (hasClass(node, "event__header")) {
                League league = extractLeague(node);
                leagues.addLast(league);
            } else if (hasClass(node, "event__match")) {
                List<FootballMatch> events = leagues.getLast().getEvents();
                events.add(extractEvent(node));
            }
        }

        filterResults(leagues);
        closeWebClient();
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

        DomNode timeNode = node.querySelector("div.event__stage");
        if (timeNode != null) {
            event.setStatus(timeNode.getTextContent().trim());
        } else {
            timeNode = node.querySelector("div.event__time");
            String time= timeNode.getFirstChild().getTextContent();
            event.setStartDate(getStartDate(time));
        }
        event.setScore(extractScore(node));

        return event;
    }

    private String extractScore(DomNode node) {
        DomNodeList<DomNode> domNodes = node.querySelectorAll("div.event__scores span");
        if (domNodes.size() == 0) {
            return null;
        } else {
            return domNodes.get(0).getTextContent().trim() + "-" + domNodes.get(1).getTextContent().trim();
        }
    }

    private String extractMyscoreId(DomNode node) {
        return node.getAttributes().getNamedItem("id").getNodeValue().substring(4);
    }

    private void filterResults(List<League> leagues) throws IOException {
        for (League league : leagues) {
            for (FootballMatch event : league.getEvents()) {
                if (getTimeInNumberFormat(event) >= LIVE_BET_TIME
                        && isNoGoalsInGame(event.getScore())) {

                    if (checkLastGames(event, 3)) {
                        Sender.sendMessage("3 ничьи подряд");
                        Sender.sendMessage("_______________");
                        Sender.sendMessage("https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#match-summary");
                        Sender.sendMessage("_______________");
                    }

                    if (checkLastGames(event, 2)) {
                        Sender.sendMessage("2 ничьи подряд");
                        Sender.sendMessage("_______________");
                        Sender.sendMessage("https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#match-summary");
                        Sender.sendMessage("_______________");
                    }

                }
            }
        }
    }

    private boolean isNoGoalsInGame(String score) {
        String[] goals = score.split("-");
        return goals[0].equals("0") && goals[1].equals("0");
    }

    private boolean checkLastGames(FootballMatch event, int zeroScoreCount) throws IOException {
        String url = "https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#h2h;overall";
        HtmlPage htmlPage = webClient.getPage(url);
        DomNodeList<DomNode> tables = htmlPage.querySelectorAll("table.head_to_head");
        for (DomNode table : tables) {
            DomNodeList<DomNode> rows = table.querySelectorAll("tbody tr");
            return isZeroScore(zeroScoreCount, rows);
        }

        return false;
    }

    private boolean isZeroScore(int count, DomNodeList<DomNode> rows) {
        int zeroScoreCounter = 0;
        for (int i = 1; i < count; i++) {
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

    public static void main(String[] args) throws IOException {
        new FootballParser().parseTodaySportEvents();
    }
}
