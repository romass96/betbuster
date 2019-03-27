package parser;

import java.util.Date;

public class SportEvent {
    private String id;
    private String firstPlayer;
    private String secondPlayer;
    private Date startDate;
    private String status;
    private String score;

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
        return "SportEvent{" +
                "id='" + id + '\'' +
                ", firstPlayer='" + firstPlayer + '\'' +
                ", secondPlayer='" + secondPlayer + '\'' +
                ", startDate=" + startDate +
                ", status='" + status + '\'' +
                ", score='" + score + '\'' +
                '}';
    }
}
