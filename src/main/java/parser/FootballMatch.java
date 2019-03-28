package parser;

import java.util.Date;
import java.util.Objects;

public class FootballMatch {
    private String myscoreId;
    private String id;
    private String firstPlayer;
    private String secondPlayer;
    private Date startDate;
    private String status;
    private TeamStatistics firstTeamStatistics;
    private TeamStatistics secondTeamStatistics;
    private League league;


    public String getMyscoreId() {
        return myscoreId;
    }

    public void setMyscoreId(String myscoreId) {
        this.myscoreId = myscoreId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstPlayer() {
        return firstPlayer;
    }

    public void setFirstPlayer(String firstPlayer) {
        this.firstPlayer = firstPlayer;
    }

    public String getSecondPlayer() {
        return secondPlayer;
    }

    public void setSecondPlayer(String secondPlayer) {
        this.secondPlayer = secondPlayer;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TeamStatistics getFirstTeamStatistics() {
        return firstTeamStatistics;
    }

    public void setFirstTeamStatistics(TeamStatistics firstTeamStatistics) {
        this.firstTeamStatistics = firstTeamStatistics;
    }

    public TeamStatistics getSecondTeamStatistics() {
        return secondTeamStatistics;
    }

    public void setSecondTeamStatistics(TeamStatistics secondTeamStatistics) {
        this.secondTeamStatistics = secondTeamStatistics;
    }

    public League getLeague() {
        return league;
    }

    public void setLeague(League league) {
        this.league = league;
    }

    @Override
    public boolean equals(Object o) {
        return myscoreId.equals(((FootballMatch)o).getMyscoreId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myscoreId);
    }

    @Override
    public String toString() {
        return "FootballMatch{" +
                "myscoreId='" + myscoreId + '\'' +
                ", id='" + id + '\'' +
                ", firstPlayer='" + firstPlayer + '\'' +
                ", secondPlayer='" + secondPlayer + '\'' +
                ", startDate=" + startDate +
                ", status='" + status + '\'' +
                ", firstTeamStatistics=" + firstTeamStatistics +
                ", secondTeamStatistics=" + secondTeamStatistics +
                ", league=" + league +
                '}';
    }
}
