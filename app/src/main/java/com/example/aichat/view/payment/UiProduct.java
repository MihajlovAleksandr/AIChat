package com.example.aichat.view.payment;

public class UiProduct {

    private final String id;

    private final String name;

    private final String title;

    private final String description;

    private final String price;

    private final String badge;

    private final int iconRes;

    private final int accentColor;

    public UiProduct(
            String id,
            String name,
            String title,
            String description,
            String price,
            String badge,
            int iconRes,
            int accentColor
    ) {

        this.id = id;
        this.name = name;
        this.title = title;
        this.description = description;
        this.price = price;
        this.badge = badge;
        this.iconRes = iconRes;
        this.accentColor = accentColor;
    }

    public String getId() {

        return id;
    }

    public String getName() {

        return name;
    }

    public String getTitle() {

        return title;
    }

    public String getDescription() {

        return description;
    }

    public String getPrice() {

        return price;
    }

    public String getBadge() {

        return badge;
    }

    public int getIconRes() {

        return iconRes;
    }

    public int getAccentColor() {

        return accentColor;
    }
}
