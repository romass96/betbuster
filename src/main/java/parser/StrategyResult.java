package parser;

public enum StrategyResult {
    TWO_LAST_GAMES_ZERO_DRAWS("2 ничьи подряд 0:0"),
    TWO_LAST_GAMES_ONE_GOAL_DRAWS("2 ничьи подряд 1:1"),
    RED_CARD("Красная карточка");

    private String description;

    StrategyResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
