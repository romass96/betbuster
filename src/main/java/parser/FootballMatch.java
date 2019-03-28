package parser;

import java.util.Date;

public class FootballMatch {
    private String firstTimeScore;
    private String myscoreId;
    private String id;
    private String firstPlayer;
    private String secondPlayer;
    private Date startDate;
    private String status;
    private String score;

    public String getFirstTimeScore() {
        return firstTimeScore;
    }

    public void setFirstTimeScore(String firstTimeScore) {
        this.firstTimeScore = firstTimeScore;
    }

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

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "FootballMatch{" +
                "firstTimeScore='" + firstTimeScore + '\'' +
                ", myscoreId='" + myscoreId + '\'' +
                ", id='" + id + '\'' +
                ", firstPlayer='" + firstPlayer + '\'' +
                ", secondPlayer='" + secondPlayer + '\'' +
                ", startDate=" + startDate +
                ", status='" + status + '\'' +
                ", score='" + score + '\'' +
                '}';
    }
}
