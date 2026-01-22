package macha;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("main.fxml"));
        Scene scene = new Scene(loader.load(), 520, 360);

        stage.setTitle("Macha");
        stage.setScene(scene);
        stage.show();
    }

    public String getGreeting() {
        return "Hello from App!";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
