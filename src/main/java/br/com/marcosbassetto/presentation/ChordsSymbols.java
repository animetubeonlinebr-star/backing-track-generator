package br.com.marcosbassetto.presentation;

public enum ChordsSymbols {
    LA("A"),
    SI("B"),
    DO("C"),
    RE("D"),
    MI("E"),
    FA("F"),
    SOL("G"),
    SUST("#"),
    BEM("b"),
    MENOR("m"),
    MAIOR("+"),
    DIM("º");

    private final String symbol;

    ChordsSymbols(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
