package nz.ac.auckland.grocerfy.model;

import java.util.List;

public enum Allergen {
    EGG("egg", "eggs"),
    DAIRY("dairy", "milk", "lactose"),
    FISH("fish"),
    SHELLFISH("shellfish", "crustacean", "crab", "prawn", "shrimp", "lobster"),
    PEANUT("peanut", "peanuts"),
    SOY("soy", "soya", "soybean", "soyabean"),
    TREENUT("almond", "almonds", "treenut", "treenuts", "tree nuts", "tree nut", "cashew", "cashews", "walnut", "walnuts", "pistachio", "pistachios", "pecan", "pecans", "nut", "nuts"),
    SESAME("seed", "sesame", "seeds", "poppy"),
    WHEAT("wheat"),
    GLUTEN("gluten"),
    UNKNOWN(); // when no product data can conclude

    private final List<String> keywords;

    Allergen(String... variants) {
        keywords = List.of(variants);
    }

    public List<String> getKeywords() {
        return keywords;
    }
}
