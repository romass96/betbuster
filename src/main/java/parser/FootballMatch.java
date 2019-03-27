package parser;

public class FootballMatch extends SportEvent {
    private String firstTimeScore;
    private String myscoreId;

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

    @Override
    public String toString() {
        return "FootballMatch{" +
                "firstTimeScore='" + firstTimeScore + '\'' +
                "} " + super.toString();
    }
}
