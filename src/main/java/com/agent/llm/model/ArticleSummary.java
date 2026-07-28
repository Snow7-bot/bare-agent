package com.agent.llm.model;

import java.util.List;

public class ArticleSummary {
    private String title;
    private List<String> keywords;
    private String summary;
    private String author;
    private String date;

    public ArticleSummary() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @Override
    public String toString() {
        return "ArticleSummary{title='" + title + "', keywords=" + keywords
                + ", summary='" + summary + "', author='" + author + "', date='" + date + "'}";
    }
}