package com.example.aichat.view.main.marketplace;

public class AiModel {

    public String title;
    public String category;
    public String price;
    public String rating;
    public String reviews;
    public String downloads;

    public AiModel(String title, String category, String price,
                   String rating, String reviews, String downloads) {
        this.title = title;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.reviews = reviews;
        this.downloads = downloads;
    }
}