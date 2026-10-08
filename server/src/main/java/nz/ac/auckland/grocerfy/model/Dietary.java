package nz.ac.auckland.grocerfy.model;

public enum Dietary {
    ORGANIC("organic"),
    VEGAN("vegan"),
    NONGMO("non-gmo"),
    VEGETARIAN("vegetarian");

    private final String keyword;

    Dietary(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
