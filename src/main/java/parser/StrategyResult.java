package parser;

public enum StrategyResult {
    TWO_LAST_GAMES_ZERO_DRAWS("3 ничьи подряд"),
    THREE_LAST_GAMES_ZERO_DRAWS("2 ничьи подряд"),
    ZERO_DRAWS_FOR_ALL_TEAMS("Ничьи у обеих команд"),
    RED_CARD("Красная карточка"),
    DRAW("Ничья");

    private String description;

    StrategyResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
