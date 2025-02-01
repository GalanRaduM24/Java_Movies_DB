package com.example.ex_filme;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

public class HelloApplication extends Application {

    public Map<String, Movie> movieMap = new HashMap<>();
    ListView<String> movieList = new ListView<>();
    private List<Movie> allMovies = new ArrayList<>();



    public static void main(String[] args) {
        launch(args);
    }

    private TextField nameField = new TextField();
    private TextField directorField = new TextField();
    private TextField genreField = new TextField();
    private TextField actorsField = new TextField();
    private ComboBox<Integer> releaseYearComboBox = new ComboBox<>(FXCollections.observableArrayList(getYearRange()));

    @Override
    public void start(Stage stage) {

        try {
            Connection connection = DatabaseManager.getConnection();
            System.out.println("Connected to the database!");
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
        }

        loadMoviesFromDatabase();
        DatabaseManager.createTables();

        List<String> movieTitles = getMovieTitles();
        movieList.getItems().addAll(movieTitles);

        // Create UI components
        Button searchButton = new Button("Search");
        Button addMovieButton = new Button("Add Movie");
        Button deleteMovieButton = new Button("Delete Movie");
        Button seeMovieDetailsButton = new Button("See Movie Details");
        Button viewWatchedMoviesButton = new Button("View Watched Movies");
        Button refreshButton = new Button("Refresh");

        VBox refreshLayout = new VBox(refreshButton);
        refreshLayout.setAlignment(Pos.BOTTOM_LEFT);
        refreshLayout.setPadding(new Insets(10));

        // Create left section for searching movies
        VBox searchSection = new VBox(
                new Label("Search Movies"),
                new HBox(new Label("Name:"), nameField),
                new HBox(new Label("Director:"), directorField),
                new HBox(new Label("Genre:"), genreField),
                new HBox(new Label("Actors:"), actorsField),
                new HBox(new Label("Release Year:"), releaseYearComboBox),
                searchButton
        );
        searchSection.setSpacing(10);
        searchSection.setPadding(new Insets(10));

        VBox buttonSection = new VBox(
                addMovieButton,
                deleteMovieButton,
                seeMovieDetailsButton,
                viewWatchedMoviesButton
        );
        buttonSection.setSpacing(10);
        buttonSection.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(movieList);
        root.setLeft(searchSection);
        root.setRight(buttonSection);
        root.setBottom(refreshLayout);


        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/com/example/ex_filme/style.css").toExternalForm());
        stage.setTitle("Movie Database Manager");
        stage.setScene(scene);
        stage.show();

        refreshButton.setOnAction(e -> {
            loadMoviesFromDatabase();
            updateMovieList();

            nameField.clear();
            directorField.clear();
            genreField.clear();
            actorsField.clear();
            releaseYearComboBox.setValue(null);
        });
        searchButton.setOnAction(e -> handleSearch());
        addMovieButton.setOnAction(e -> openAddMovieWindow());
        deleteMovieButton.setOnAction(e -> handleDelete());
        viewWatchedMoviesButton.setOnAction(e -> openWatchedMoviesWindow());

        seeMovieDetailsButton.setOnAction(e -> {
            String selectedMovieTitle = movieList.getSelectionModel().getSelectedItem();
            openMovieDetailsWindow(selectedMovieTitle);
            loadMoviesFromDatabase();
            updateMovieList();
        });

        movieList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {  // Double-click
                String selectedMovieTitle = movieList.getSelectionModel().getSelectedItem();
                openMovieDetailsWindow(selectedMovieTitle);
                loadMoviesFromDatabase();
                updateMovieList();
            }
        });


    }

    public List<String> getMovieTitles() {
        List<String> movieTitles = new ArrayList<>(movieMap.keySet());
        return movieTitles;
    }


    private void loadMoviesFromDatabase() {
        List<Movie> movies = DatabaseManager.getAllMovies();

        for (Movie movie : movies) {
            boolean watchedInDatabase = DatabaseManager.isMovieWatched(movie.getId());
            movie.setWatched(watchedInDatabase);

            movieMap.put(movie.getTitle(), movie);
        }
    }

    private void updateMovieList() {
        movieList.getItems().clear();

        movieList.getItems().addAll(movieMap.keySet());
    }

    private Integer[] getYearRange() {
        int currentYear = Year.now().getValue();
        Integer[] years = new Integer[currentYear - 1899];

        for (int i = 0; i < years.length; i++) {
            years[i] = 1900 + i;
        }

        return years;
    }

    private void openAddMovieWindow() {
        Stage addMovieStage = new Stage();
        addMovieStage.setTitle("Add Movie");

        TextField titleField = new TextField();
        TextField directorField = new TextField();
        TextField genreField = new TextField();
        TextField actorsField = new TextField();
        DatePicker releaseDateField = new DatePicker();
        Button saveButton = new Button("Save");

        VBox addMovieLayout = new VBox(
                new HBox(new Label("Title:"), titleField),
                new HBox(new Label("Director:"), directorField),
                new HBox(new Label("Genre:"), genreField),
                new HBox(new Label("Actors (comma-separated):"), actorsField),
                new HBox(new Label("Release Date:"), releaseDateField),
                saveButton
        );
        addMovieLayout.setSpacing(10);
        addMovieLayout.setPadding(new Insets(10));

        Scene addMovieScene = new Scene(addMovieLayout, 400, 300);
        addMovieScene.getStylesheets().add(getClass().getResource("/com/example/ex_filme/style.css").toExternalForm());

        addMovieStage.setScene(addMovieScene);
        saveButton.setOnAction(event -> {
            List<String> actors = Arrays.asList(actorsField.getText().split(",\\s*"));
            Movie newMovie = new Movie(
                    titleField.getText(),
                    directorField.getText(),
                    genreField.getText(),
                    actors,
                    releaseDateField.getValue()
            );
            DatabaseManager.insertMovie(newMovie);

            addMovieStage.close();

            loadMoviesFromDatabase();
            updateMovieList();
        });

        addMovieStage.show();
    }

    private void handleSearch() {
        String name = nameField.getText();
        String director = directorField.getText();
        String genre = genreField.getText();
        List<String> actorsList = Arrays.asList(actorsField.getText().split(",\\s*"));

        if (actorsList.isEmpty() || (actorsList.size() == 1 && actorsList.get(0).isEmpty())) {
            actorsList = Collections.emptyList();
        }

        Integer releaseYear = releaseYearComboBox.getValue();
        List<Movie> filteredMovies = DatabaseManager.searchMovies(name, director, genre, actorsList, releaseYear);

        updateSearchResultsList(filteredMovies);
    }

    private void updateSearchResultsList(List<Movie> filteredMovies) {
        movieList.getItems().clear();

        for (Movie movie : filteredMovies) {
            movieList.getItems().add(movie.getTitle());
        }
    }

    private void handleDelete() {
        String selectedMovieTitle = movieList.getSelectionModel().getSelectedItem();

        if (selectedMovieTitle != null) {
            int movieId = DatabaseManager.getMovieId(selectedMovieTitle);

            DatabaseManager.deleteMovie(movieId);

            movieMap.remove(selectedMovieTitle);

            updateMovieList();
        }
    }
    private void openWatchedMoviesWindow() {
        List<Movie> watchedMovies = DatabaseManager.getWatchedMovies();

        Stage watchedMoviesStage = new Stage();
        watchedMoviesStage.setTitle("Watched Movies");

        ListView<String> watchedMoviesList = new ListView<>();
        watchedMoviesList.getItems().addAll(watchedMovies.stream().map(Movie::getTitle).collect(Collectors.toList()));

        Button setToUnwatchedButton = new Button("Set to Unwatched");
        setToUnwatchedButton.setOnAction(event -> {
            setMovieToUnwatched(watchedMoviesList, watchedMovies);
            watchedMoviesList.refresh();
        });
        VBox watchedMoviesLayout = new VBox(
                new Label("Watched Movies"),
                watchedMoviesList,
                setToUnwatchedButton
        );
        watchedMoviesLayout.setSpacing(10);
        watchedMoviesLayout.setPadding(new Insets(10));

        Scene watchedMoviesScene = new Scene(watchedMoviesLayout, 400, 300);
        watchedMoviesScene.getStylesheets().add(getClass().getResource("/com/example/ex_filme/style.css").toExternalForm());

        watchedMoviesStage.setScene(watchedMoviesScene);

        watchedMoviesStage.show();
    }
    private void setMovieToUnwatched(ListView<String> watchedMoviesList, List<Movie> watchedMovies) {
        String selectedMovieTitle = watchedMoviesList.getSelectionModel().getSelectedItem();

        Movie selectedMovie = watchedMovies.stream()
                .filter(movie -> movie.getTitle().equals(selectedMovieTitle))
                .findFirst()
                .orElse(null);

        if (selectedMovie != null) {
            selectedMovie.setWatched(false);
            DatabaseManager.updateMovieToUnwatched(selectedMovie);

            watchedMoviesList.getItems().clear();
            watchedMoviesList.getItems().addAll(watchedMovies.stream().map(Movie::getTitle).collect(Collectors.toList()));
        }
    }
    private void openMovieDetailsWindow(String selectedMovieTitle) {
        Movie selectedMovie = movieMap.get(selectedMovieTitle);

        Stage detailsStage = new Stage();
        detailsStage.setTitle("Movie Details");

        Label titleLabel = new Label("Title: " + selectedMovie.getTitle());
        Label directorLabel = new Label("Director: " + selectedMovie.getDirector());
        Label genreLabel = new Label("Genre: " + selectedMovie.getGenre());
        Label actors = new Label("Actors:" + selectedMovie.getFormattedActors());
        Label releaseDateLabel = new Label("Release Date: " + selectedMovie.getReleaseDate());
        Label watchedLabel = new Label("Watched: " + selectedMovie.isWatched());
        Label watchedDateLabel = new Label("Watched Date: " + selectedMovie.getWatchedDate());

        Button setWatchedButton = new Button(selectedMovie.isWatched() ? "Set to Unwatched" : "Set to Watched");

        VBox detailsLayout = new VBox(
                titleLabel,
                directorLabel,
                genreLabel,
                actors,
                releaseDateLabel,
                watchedLabel,
                watchedDateLabel,
                setWatchedButton
        );
        detailsLayout.setSpacing(10);
        detailsLayout.setPadding(new Insets(10));

        Scene detailsScene = new Scene(detailsLayout, 400, 300);
        detailsScene.getStylesheets().add(getClass().getResource("/com/example/ex_filme/style.css").toExternalForm());

        detailsStage.setScene(detailsScene);

        setWatchedButton.setOnAction(e -> {
            selectedMovie.setWatched(!selectedMovie.isWatched());

            int selectedMovieId = selectedMovie.getId();

            if (selectedMovie.isWatched()) {
                DatabaseManager.updateMovieToWatched(selectedMovieId);
            } else if (!selectedMovie.isWatched()) {
                DatabaseManager.updateMovieToUnwatched(selectedMovie);

            }

            watchedLabel.setText("Watched: " + selectedMovie.isWatched());
            watchedDateLabel.setText("Watched Date: " + selectedMovie.getWatchedDate());

            loadMoviesFromDatabase();
            updateMovieList();
        });


        loadMoviesFromDatabase();
        updateMovieList();
        detailsStage.close();


        detailsStage.show();
    }

}



