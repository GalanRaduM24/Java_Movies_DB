package com.example.ex_filme;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

public class Movie {
    private int id;
    private String title;
    private String director;
    private String genre;
    private List<String> actors;
    private LocalDate releaseDate;
    private boolean watched;
    private LocalDate watchedDate;

    public Movie(String title, String director, String genre, List<String> actors, LocalDate releaseDate) {
        this.title = title;
        this.director = director;
        this.genre = genre;
        this.actors = actors;
        this.releaseDate = releaseDate;
        this.watched = false;
        this.watchedDate = null;
    }


    public void setWatched(boolean watched) {
        if (!this.watched) {
            this.watched = watched;
            watchedDate = LocalDate.now();
        }
        else {
            this.watched = watched;
            watchedDate = null;
        }
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDirector() {
        return director;
    }

    public String getGenre() {
        return genre;
    }

    public List<String> getActors() {
        return actors;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public boolean isWatched() {
        return watched;
    }

    public LocalDate getWatchedDate() {
        if (!watched)
            return null;
        else {
            return watchedDate;
        }
    }

    public void setId(int anInt) {
        this.id = anInt;
    }

    public String getFormattedActors() {
        return String.join(", ", actors);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", director='" + director + '\'' +
                ", genre='" + genre + '\'' +
                ", actors=" + actors +
                ", releaseDate=" + releaseDate +
                ", watched=" + watched +
                ", watchedDate=" + watchedDate +
                '}';
    }

    public void setWatchedDate(LocalDate date) {
        this.watchedDate = date;
    }
}
