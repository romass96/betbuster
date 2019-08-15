package parser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public class Notification {
    private String myscoreId;
    private LocalDateTime dateTime;
    private StrategyResult strategyResult;

    public Notification(String myscoreId, LocalDateTime dateTime, StrategyResult strategyResult) {
        this.myscoreId = myscoreId;
        this.dateTime = dateTime;
        this.strategyResult = strategyResult;
    }

    public String getMyscoreId() {
        return myscoreId;
    }

    public void setMyscoreId(String myscoreId) {
        this.myscoreId = myscoreId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public StrategyResult getStrategyResult() {
        return strategyResult;
    }

    public void setStrategyResult(StrategyResult strategyResult) {
        this.strategyResult = strategyResult;
    }

    @Override
    public int hashCode() {
        return myscoreId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return ((Notification) obj).getMyscoreId().equals(myscoreId);
    }
}
