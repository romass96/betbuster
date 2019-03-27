package parser;

import java.util.ArrayList;
import java.util.List;

public class League {
    private String name;
    private String region;
    private List<FootballMatch> events = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<FootballMatch> getEvents() {
        return events;
    }

    public void setEvents(List<FootballMatch> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "League{" +
                "name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", events=" + events +
                '}';
    }
}
