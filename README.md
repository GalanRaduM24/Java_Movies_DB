# Movie Database Manager

## Description
Movie Database Manager is a JavaFX application that allows users to manage a movie collection. Users can add, delete, search for, and mark movies as watched. The application is backed by a SQLite database.

## Features
- Add movies with details such as title, director, genre, actors, and release year
- Search for movies based on different criteria
- View details of movies, including watched status
- Mark movies as watched/unwatched
- View a list of watched movies
- Delete movies from the database

## Technologies Used
- JavaFX
- SQLite (via JDBC)
- Java 17+

## Installation & Setup
1. Clone the repository:
   ```sh
   git clone https://github.com/GalanRaduM24/Java_Movies_DB
   cd movie-database-manager
   ```
2. Open the project in an IDE that supports JavaFX (e.g., IntelliJ IDEA, Eclipse with e(fx)clipse plugin).
3. Ensure JavaFX dependencies are set up correctly.
4. Run the `HelloApplication.java` file to start the application.

## Database Structure
The application uses SQLite to store movie details with the following structure:

**Movies Table**
| Column      | Type    | Description |
|------------|--------|-------------|
| id         | INTEGER | Primary key |
| title      | TEXT   | Movie title |
| director   | TEXT   | Director's name |
| genre      | TEXT   | Movie genre |
| actors     | TEXT   | Comma-separated list of actors |
| release_year | INTEGER | Year of release |
| watched    | BOOLEAN | Indicates if the movie has been watched |
| watched_date | DATE | Date when the movie was marked as watched |

## Usage
- **Adding a Movie:** Click 'Add Movie', enter details, and save.
- **Searching for a Movie:** Enter search criteria and click 'Search'.
- **Viewing Movie Details:** Double-click a movie from the list.
- **Marking as Watched/Unwatched:** Open movie details and toggle the watched status.
- **Deleting a Movie:** Select a movie and click 'Delete Movie'.

## Screenshots
*(https://github.com/GalanRaduM24/Java_Movies_DB/blob/main/Pictures/img.png)*
*(https://github.com/GalanRaduM24/Java_Movies_DB/blob/main/Pictures/img_1.png)*

