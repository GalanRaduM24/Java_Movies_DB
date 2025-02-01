package com.example.ex_filme;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "" ;  //insert database path
    private static final String USER = "";  //insert database user
    private static final String PASSWORD = ""; //insert databse admin

    public static Connection getConnection() throws SQLException {
        try {
            System.out.println("Bravo");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new SQLException("Failed to establish a database connection.", e);
        }
    }


    //Create tabels and insert into them
    //Movie tabel
    //Actors tabel
    //Movie actors tabel connection between the movie and actor
    public static void createTables() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            System.out.println("Creating tables...");
            String createMovieTableQuery = "CREATE TABLE IF NOT EXISTS movies (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "title TEXT," +
                    "director TEXT," +
                    "genre TEXT," +
                    "releaseDate DATE," +
                    "watched BOOLEAN," +
                    "watchedDate DATE)";
            statement.execute(createMovieTableQuery);

            String createActorsTableQuery = "CREATE TABLE IF NOT EXISTS actors (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "name TEXT)";
            statement.execute(createActorsTableQuery);

            String createMovieActorsTableQuery = "CREATE TABLE IF NOT EXISTS movie_actors (" +
                    "movie_id INTEGER," +
                    "actor_id INTEGER," +
                    "FOREIGN KEY (movie_id) REFERENCES movies(id)," +
                    "FOREIGN KEY (actor_id) REFERENCES actors(id)," +
                    "PRIMARY KEY (movie_id, actor_id))";
            statement.execute(createMovieActorsTableQuery);

            System.out.println("Tables created successfully.");


        } catch (SQLException e) {
            System.err.println("Error creating tables:");
            e.printStackTrace();
        }
    }

    public static void insertMovie(Movie movie) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO movies (title, director, genre, releaseDate, watched, watchedDate) " +
                             "VALUES (?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, movie.getTitle());
            statement.setString(2, movie.getDirector());
            statement.setString(3, movie.getGenre());
            statement.setDate(4, java.sql.Date.valueOf(movie.getReleaseDate()));
            statement.setBoolean(5, movie.isWatched());
            statement.setDate(6, (movie.isWatched() ? java.sql.Date.valueOf(movie.getWatchedDate()) : null));

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting movie failed, no rows affected.");
            }

            try (var generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    movie.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Inserting movie failed, no ID obtained.");
                }
            }

            insertMovieActors(movie);

        } catch (SQLException e) {
            System.err.println("Error inserting movie:");
            e.printStackTrace();
        }
    }

    private static void insertMovieActors(Movie movie) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO movie_actors (movie_id, actor_id) VALUES (?, ?)")) {

            for (String actorName : movie.getActors()) {
                int actorId = getOrCreateActorId(actorName);
                statement.setInt(1, movie.getId());
                statement.setInt(2, actorId);
                statement.addBatch();
            }

            statement.executeBatch();

        } catch (SQLException e) {
            System.err.println("Error inserting movie actors:");
            e.printStackTrace();
        }
    }

    private static int getOrCreateActorId(String actorName) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT IGNORE INTO actors (name) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, actorName);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    return getActorIdByName(actorName);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting or creating actor ID:");
            e.printStackTrace();
            return -1;
        }
    }

    private static int getActorIdByName(String actorName) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM actors WHERE name = ?")) {

            statement.setString(1, actorName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                } else {
                    // Actor not found
                    return -1;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting actor ID by name:");
            e.printStackTrace();
            return -1;
        }
    }

    public static List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM movies")) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                String director = resultSet.getString("director");
                String genre = resultSet.getString("genre");
                LocalDate releaseDate = resultSet.getDate("releaseDate").toLocalDate();
                boolean watched = resultSet.getBoolean("watched");
                LocalDate watchedDate = resultSet.getDate("watchedDate") != null ?
                        resultSet.getDate("watchedDate").toLocalDate() : null;

                List<String> actors = getActorsForMovie(id);

                Movie movie = new Movie(title, director, genre, actors, releaseDate);
                movie.setId(id);
                movies.add(movie);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

    public static List<String> getActorsForMovie(int movieId) {
        List<String> actors = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT actors.name FROM actors " +
                             "JOIN movie_actors ON actors.id = movie_actors.actor_id " +
                             "WHERE movie_actors.movie_id = ?")) {

            statement.setInt(1, movieId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String actorName = resultSet.getString("name");
                actors.add(actorName);
            }
            ;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return actors;
    }


    //Search for a certain movie
    public static List<Movie> searchMovies(String name, String director, String genre, List<String> actors, Integer releaseYear) {
        List<Movie> filteredMovies = new ArrayList<>();

        try (Connection connection = getConnection();

             PreparedStatement statement = buildSearchStatement(name, director, genre, actors, releaseYear);
             ResultSet resultSet = statement.executeQuery()){

            System.out.println("SQL Query: " + statement.toString());


            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String movieDirector = resultSet.getString("director");
                String movieGenre = resultSet.getString("genre");
                LocalDate releaseDate = resultSet.getDate("releaseDate").toLocalDate();
                boolean watched = resultSet.getBoolean("watched");

                // Retrieve the list of actors for the movie
                int movieId = resultSet.getInt("id");
                List<String> movieActors;

                if (actors.size() == 1 && !actors.get(0).isEmpty()) {
                    movieActors = Collections.singletonList(actors.get(0));
                } else {
                     movieActors = getActorsForMovie(movieId);
                }

                Movie movie = new Movie(title, movieDirector, movieGenre, movieActors, releaseDate);
                movie.setWatched(watched);
                System.out.println(movie.getTitle());
                filteredMovies.add(movie);
            }

        } catch (SQLException e) {
            System.err.println("Error searching movies:");
            e.printStackTrace();
        }

        return filteredMovies;
    }

    private static PreparedStatement buildSearchStatement(String name, String director, String genre, List<String> actors, Integer releaseYear) throws SQLException {
        Connection connection = getConnection();

        String query = "SELECT DISTINCT movies.* FROM movies";

        if (!actors.isEmpty()) {
            query += " INNER JOIN movie_actors ON movies.id = movie_actors.movie_id " +
                    "INNER JOIN actors ON movie_actors.actor_id = actors.id " +
                    "WHERE actors.name IN (?";
            for (int i = 1; i < actors.size(); i++) {
                query += ", ?";
            }
            query += ")";
        }

        if (name != null && !name.isEmpty()) {
            if (actors.isEmpty()) {
                query += " WHERE";
            } else {
                query += " AND";
            }
            query += " title LIKE ?";
        }
        if (director != null && !director.isEmpty()) {
            if (actors.isEmpty()) {
                query += " WHERE";
            } else {
                query += " AND";
            }
            query += " director LIKE ?";
        }
        if (genre != null && !genre.isEmpty()) {
            if (actors.isEmpty()) {
                query += " WHERE";
            } else {
                query += " AND";
            }
            query += " genre LIKE ?";
        }
        if (releaseYear != null && actors.isEmpty()) {
            if (actors.isEmpty()) {
                query += " WHERE";
            } else {
                query += " AND";
            }
            query += " YEAR(releaseDate) =  ?";
        }

        PreparedStatement statement = connection.prepareStatement(query);

        int parameterIndex = 1;
        if (!actors.isEmpty()) {
            for (String actor : actors) {
                statement.setString(parameterIndex++, actor);
            }
        }
        if (name != null && !name.isEmpty()) {
            statement.setString(parameterIndex++, "%" + name + "%");
        }
        if (director != null && !director.isEmpty()) {
            statement.setString(parameterIndex++, "%" + director + "%");
        }
        if (genre != null && !genre.isEmpty()) {
            statement.setString(parameterIndex++, "%" + genre + "%");
        }
        if (releaseYear != null && actors.isEmpty()) {
            statement.setInt(parameterIndex++, releaseYear);
        }

        return statement;
    }



    public static int getMovieId(String title) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM movies WHERE title = ?")) {

            statement.setString(1, title);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("id");
            } else {
                return -1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void deleteMovie(int movieId) {
        try (Connection connection = getConnection()) {
            String deleteRelationshipsQuery = "DELETE FROM movie_actors WHERE movie_id = ?";
            try (PreparedStatement relationshipsStatement = connection.prepareStatement(deleteRelationshipsQuery)) {
                relationshipsStatement.setInt(1, movieId);
                relationshipsStatement.executeUpdate();
            }

            String deleteMovieQuery = "DELETE FROM movies WHERE id = ?";
            try (PreparedStatement movieStatement = connection.prepareStatement(deleteMovieQuery)) {
                movieStatement.setInt(1, movieId);
                int affectedRows = movieStatement.executeUpdate();

                if (affectedRows == 0) {
                    System.out.println("No movie with id " + movieId + " found.");
                } else {
                    System.out.println("Movie with id " + movieId + " deleted successfully.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Movie> getWatchedMovies() {
        List<Movie> watchedMovies = new ArrayList<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM movies WHERE watched = true")) {

            while (resultSet.next()) {
                int movieId = resultSet.getInt("id");
                String title = resultSet.getString("title");
                String director = resultSet.getString("director");
                String genre = resultSet.getString("genre");
                LocalDate releaseDate = resultSet.getDate("releaseDate").toLocalDate();

                List<String> actors = getActorsForMovie(movieId);

                Movie movie = new Movie(title, director, genre, actors, releaseDate);
                movie.setId(movieId);
                movie.setWatched(true);

                watchedMovies.add(movie);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return watchedMovies;
    }

    public static void updateMovieToUnwatched(Movie movie) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE movies SET title = ?, director = ?, genre = ?, releaseDate = ?, watched = ?, watchedDate = ? WHERE id = ?")) {

            statement.setString(1, movie.getTitle());
            statement.setString(2, movie.getDirector());
            statement.setString(3, movie.getGenre());
            statement.setDate(4, java.sql.Date.valueOf(movie.getReleaseDate()));
            statement.setBoolean(5, movie.isWatched());
            statement.setDate(6, (movie.isWatched() ? java.sql.Date.valueOf(movie.getWatchedDate()) : null));
            statement.setInt(7, movie.getId());
            movie.setWatched(false);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating movie failed, no rows affected.");
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateMovieToWatched(int movieId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE movies SET watched = true, watchedDate = CURRENT_DATE WHERE id = ?")) {

            statement.setInt(1, movieId);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("No movie with id " + movieId + " found.");
            } else {
                System.out.println("Movie with id " + movieId + " updated to watched successfully.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isMovieWatched(int movieId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT watched FROM movies WHERE id = ?")) {

            statement.setInt(1, movieId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean("watched");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


}