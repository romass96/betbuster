package parser;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FootballParser {

    private static final int LIVE_TIME = 46;
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

    public void parseSportEvents(Date from, Date to) throws IOException {
        int dayDifference = calculateDayDifference(from, to);
        checkDayDifference(dayDifference);

        initWebClient();
        Map<Date, List<League>> data = new HashMap<>();

        HtmlPage htmlPage = webClient.getPage(getUrl());
        webClient.waitForBackgroundJavaScript(10000);

        Calendar calendar = getPrimaryCalendarInstance(new Date());
        int startDateIndex = calculateDayDifference(calendar.getTime(), from);
        int endDateIndex = calculateDayDifference(calendar.getTime(), to);
        calendar = getPrimaryCalendarInstance(from);
        checkParsingDateIndexes(startDateIndex, endDateIndex);
        for (int dateIndex = startDateIndex; dateIndex <= endDateIndex; dateIndex++) {
            setDate(htmlPage, dateIndex);
            webClient.waitForBackgroundJavaScript(6000);
            expandLeagues(htmlPage);
            webClient.waitForBackgroundJavaScript(6000);

            DomNodeList<DomNode> nodes = htmlPage.querySelectorAll("div.event > div");

            Date currentDate = calendar.getTime();
            LinkedList<League> leagues = new LinkedList<>();
            for (DomNode node : nodes) {
                if (hasClass(node, "event__header")) {
                    League league = extractLeague(node);
                    leagues.addLast(league);
                } else if (hasClass(node, "event__match")) {
                    List<FootballMatch> events = leagues.getLast().getEvents();
                    events.add(extractEvent(node, calendar));
                }
            }
            data.put(currentDate, leagues);
            calendar.setTime(currentDate);
            calendar.add(Calendar.DATE, 1);
        }

//        checkResults(data);
        closeWebClient();
    }

    public void parseTodaySportEvents() throws IOException {
        initWebClient();

        HtmlPage htmlPage = webClient.getPage(getUrl());
        webClient.waitForBackgroundJavaScript(10000);

        Calendar calendar = getPrimaryCalendarInstance(new Date());

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
                events.add(extractEvent(node, calendar));
            }
        }

        checkResults(leagues);
        closeWebClient();
    }

    private League extractLeague(DomNode node) {
        League league = new League();
        league.setName(node.querySelector("span.event__title--name").getTextContent());
        league.setRegion(node.querySelector("div.event__title").getTextContent());

        return league;
    }

    private FootballMatch extractEvent(DomNode node, Calendar calendar) {
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
            event.setStartDate(getStartDate(time, calendar));
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

    public String getUrl() {
        return "https://www.myscore.com.ua";
    }

    private void checkResults(List<League> leagues) throws IOException {
        for (League league : leagues) {
            for (FootballMatch event : league.getEvents()) {
                if (getIntTime(event) >= LIVE_TIME && event.getScore() != null ) {
                    String[] score = event.getScore().split("-");
                    if (score[0].equals("0") && score[1].equals("0") && checkLastGames(event)) {
                        Sender.sendMessage("https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#match-summary");
                    }
                }
            }
        }
    }

    private boolean checkLastGames(FootballMatch event) throws IOException {
        String url = "https://www.myscore.com.ua/match/" + event.getMyscoreId() +"/#h2h;overall";
        HtmlPage htmlPage = webClient.getPage(url);
        DomNodeList<DomNode> tables = htmlPage.querySelectorAll("table.head_to_head");
        for (DomNode table : tables) {
            DomNodeList<DomNode> rows = table.querySelectorAll("tbody tr");
            String firstRowScore = rows.get(0).querySelector("span.score").getTextContent();
            String secondRowScore = rows.get(1).querySelector("span.score").getTextContent();
            if (isZeroScore(firstRowScore) && isZeroScore(secondRowScore)) {
                return true;
            }

        }

        return false;
    }

    private boolean isZeroScore(String score) {
        String[] scores = score.split(":");
        return Integer.valueOf(scores[0].replaceAll("[^0-9]", "")) == 0
                && Integer.valueOf(scores[1].replaceAll("[^0-9]", "")) == 0;
    }

    private int getIntTime(FootballMatch footballMatch) {
        try {
            return Integer.valueOf(footballMatch.getStatus().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }


    private void checkDayDifference(int dayDifference) {
        if (dayDifference < 0) {
            throw new IllegalArgumentException();
        }
    }

    private void checkParsingDateIndexes(int startDateIndex, int endDateIndex) {
        if (startDateIndex < -7 || endDateIndex > 7) {
            throw new IllegalArgumentException();
        }
    }

    private Calendar getPrimaryCalendarInstance(Date date) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Date getStartDate(String time, Calendar calendar) {
        String[] timeOptions = time.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timeOptions[0]));
        calendar.set(Calendar.MINUTE, Integer.valueOf(timeOptions[1]));
        return calendar.getTime();
    }

    private void expandLeagues(HtmlPage htmlPage) {
        htmlPage.executeJavaScript("document.querySelectorAll('div.event__expander.expand').forEach(function(element) { element.click();})");
    }

    private void setDate(HtmlPage htmlPage, int dateIndex) {
        htmlPage.executeJavaScript("set_calendar_date(" + dateIndex + ");");
    }

    private int calculateDayDifference(Date from, Date to) {
        Calendar fromCalendar = getPrimaryCalendarInstance(from);
        Calendar toCalendar = getPrimaryCalendarInstance(to);
        long difference = toCalendar.getTime().getTime() - fromCalendar.getTime().getTime();
        return (int) (difference / (1000 * 60 * 60 * 24));
    }

    private boolean hasClass(DomNode domNode, String className) {
        String classes = domNode.getAttributes().getNamedItem("class").getNodeValue();
        return classes.contains(className);
    }

    public static void main(String[] args) throws IOException {
        FootballParser footballParser = new FootballParser();

        Calendar fromCalendar = GregorianCalendar.getInstance();
        Calendar toCalendar = GregorianCalendar.getInstance();
        toCalendar.add(Calendar.DATE, 0);
        fromCalendar.add(Calendar.DATE, 0);

//        footballParser.parseSportEvents(fromCalendar.getTime(), toCalendar.getTime());
        footballParser.parseTodaySportEvents();

    }
}
