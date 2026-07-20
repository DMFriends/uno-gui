package application;
	
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;

public class Main extends Application {
	public static final String APP_VERSION = "v1.0";
	private static final int MAX_PLAYERS = 4;
	private static final int MIN_PLAYERS = 2;
	private static final int STARTING_HAND_SIZE = 7;

	private Deck deck;
	private List<Player> players;
    private int playerCount;
	private int currentPlayerIndex;
	private boolean isClockwise;
	private String currentColor;


	private Stage primaryStage;
    private Scene appScene;


	public Main() {
		this.deck = new Deck();
		this.currentPlayerIndex = 0;
		this.isClockwise = true;
		this.currentColor = null;
	}

	@Override
	public void start(Stage stage) {
		try {
			// BorderPane root = new BorderPane();
			// Scene scene = new Scene(root,400,400);
			primaryStage = stage;
			primaryStage.setTitle("Uno " + APP_VERSION);
			//scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());



			//primaryStage.setScene(scene);
            showScene(createStartScreen());
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void fitToWorkArea()
    {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        primaryStage.setMaximized(false);
        primaryStage.setResizable(false);
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
    }

	private HBox createWindowBar()
    {
        Label title = new Label("Uno " + APP_VERSION);
        title.getStyleClass().add("window-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimize = new Button("_");
        minimize.getStyleClass().add("window-control");
        minimize.setTooltip(new Tooltip("Minimize"));
        minimize.setOnAction(_ -> primaryStage.setIconified(true));

        Button close = new Button("X");
        close.getStyleClass().addAll("window-control", "window-close");
        close.setTooltip(new Tooltip("Close"));
        close.setOnAction(_ -> primaryStage.close());

        HBox bar = new HBox(8, title, spacer, minimize, close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("window-bar");
        return bar;
    }

	private Parent createWindowFrame(Parent content)
    {
        BorderPane frame = new BorderPane();
        frame.getStyleClass().add("window-frame");
        frame.setTop(createWindowBar());
        frame.setCenter(content);
        return frame;
    }

	private Scene createScene(Parent root)
    {
        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/resources/uno-design.css").toExternalForm());
        return scene;
    }

	private void showScene(Parent root)
    {
        Parent framedRoot = createWindowFrame(root);

        if(appScene == null)
        {
            appScene = createScene(framedRoot);
            primaryStage.setScene(appScene);
        }
        else
        {
            appScene.setRoot(framedRoot);
        }

        Platform.runLater(this::fitToWorkArea);
    }

     private Parent createStartScreen()
    {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");

        VBox setupPanel = new VBox(18);
        setupPanel.setAlignment(Pos.CENTER_LEFT);
        setupPanel.getStyleClass().add("setup-panel");

        Label title = new Label("UNO");
        title.getStyleClass().add("setup-title");

        Label subtitle = new Label("Set up players");
        subtitle.getStyleClass().add("setup-subtitle");

        ComboBox<Integer> playerCountBox = new ComboBox<>(FXCollections.observableArrayList(2, 3, 4, 5, 6));
        playerCountBox.setValue(playerCount);
        playerCountBox.getStyleClass().add("setup-control");

        VBox nameFields = new VBox(10);
        nameFields.getStyleClass().add("name-fields");
        renderNameFields(nameFields, playerCount);

        playerCountBox.setOnAction(_ -> {
            playerCount = playerCountBox.getValue();
            renderNameFields(nameFields, playerCount);
        });

        Button startButton = new Button("Start Game");
        startButton.getStyleClass().add("primary-button");
        startButton.setOnAction(_ -> startGame(nameFields));

        Button rulesButton = new Button("Read Rules");
        rulesButton.getStyleClass().add("primary-button");
        // rulesButton.setTooltip(new Tooltip("Open Durak rules"));
        // rulesButton.setOnAction(_ -> getHostServices().showDocument("https://playjoy.com/en/durak/rules/"));

        HBox startActions = new HBox(10, startButton, rulesButton);
        startActions.setAlignment(Pos.CENTER_LEFT);

        setupPanel.getChildren().addAll(title, subtitle, labelledControl("Players", playerCountBox), nameFields, startActions);
        root.setCenter(setupPanel);
        return root;
    }


	
	public static void main(String[] args) {
		launch(args);
	}
}
