module com.example.ex_filme {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.ex_filme to javafx.fxml;
    exports com.example.ex_filme;
}