package parser;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public abstract class SportEventParser {
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
            htmlPage.executeJavaScript("set_calendar_date(" + dateIndex + ");");
            webClient.waitForBackgroundJavaScript(6000);
            expandLeagues(htmlPage);

            DomNodeList<DomNode> leagueNodes = htmlPage.querySelectorAll(getLeagueSelector());
            Date currentDate = calendar.getTime();
            List<League> leagues = new ArrayList<>();
            for (DomNode leagueNode : leagueNodes) {
                League league = new League();
                DomNodeList<DomNode> matchNodes = leagueNode.querySelectorAll("tbody tr:not(.blank-line)");

//                league.setEvents(extractMatches(matchNodes, calendar));
                league.setRegion(leagueNode.querySelector("thead tr td.head_ab .country_part").getTextContent());
                league.setName(leagueNode.querySelector("thead tr td.head_ab .tournament_part").getTextContent());
                leagues.add(league);
            }
            data.put(currentDate, leagues);
            calendar.setTime(currentDate);
            calendar.add(Calendar.DATE, 1);
        }

        checkResults(data);
        closeWebClient();
    }

    public abstract String getUrl();
    public abstract String getLeagueSelector();
    public abstract List<SportEvent> extractMatches(DomNodeList<DomNode> matchNodes, Calendar calendar);

    private void checkResults(Map<Date, List<League>> data) {
        for (Map.Entry<Date, List<League>> entry : data.entrySet()) {
            System.out.println("Checking for " + entry.getKey() + " .....");

            for (League league : entry.getValue()) {
                for (SportEvent event : league.getEvents()) {
                    String[] score = event.getScore().split("-");
                    if (score[0].trim().equals("0") && score[1].trim().equals("0")) {
                        System.out.println(event);
                    }
                }
            }
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

    protected Date getStartDate(String time, Calendar calendar) {
        String[] timeOptions = time.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timeOptions[0]));
        calendar.set(Calendar.MINUTE, Integer.valueOf(timeOptions[1]));
        return calendar.getTime();
    }

    private void expandLeagues(HtmlPage htmlPage) throws IOException {
//        List<DomElement> collapsedLeagues = htmlPage.getByXPath("//div[@class='expand']");
//        for (DomElement collapsedLeague : collapsedLeagues) {
//            collapsedLeague.click();
//        }

        htmlPage.executeJavaScript("document.querySelectorAll('div.event__expander.expand').forEach(function(element) { element.click();})");
    }

    private int calculateDayDifference(Date from, Date to) {
        Calendar fromCalendar = getPrimaryCalendarInstance(from);
        Calendar toCalendar = getPrimaryCalendarInstance(to);
        long difference = toCalendar.getTime().getTime() - fromCalendar.getTime().getTime();
        return (int) (difference / (1000 * 60 * 60 * 24));
    }

    protected boolean hasClass(DomNode domNode, String className) {
        String classes = domNode.getAttributes().getNamedItem("class").getNodeValue();
        return classes.contains(className);
    }

    public static void main(String[] args) throws IOException {
        FootballEventParser footballEventParser = new FootballEventParser();

        Calendar fromCalendar = GregorianCalendar.getInstance();
        Calendar toCalendar = GregorianCalendar.getInstance();
        toCalendar.add(Calendar.DATE, -1);
        fromCalendar.add(Calendar.DATE, -2);

        footballEventParser.parseSportEvents(fromCalendar.getTime(), toCalendar.getTime());

    }
}
