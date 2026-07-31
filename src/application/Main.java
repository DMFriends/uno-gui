package application;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
	public static final String APP_VERSION = "v1.0";
	private static final int MAX_PLAYERS = 4;
	private static final int MIN_PLAYERS = 2;

	private static final double CARD_W = 96;
	private static final double CARD_H = 144;
	private static final double PILE_CARD_W = 104;
	private static final double PILE_CARD_H = 156;
	private static final double HOVER_LIFT = 24;
	/** Fixed so a seat keeps the same height however many cards it holds. */
	private static final double FAN_HEIGHT = CARD_H + 60;
	/** How far the middle of the table sits above true centre. */
	private static final double CENTER_LIFT = 40;
	/** Clearance from the bottom of the table, so the prompt clears the hand.
	 *  Carries the same lift, which keeps the prompt's gap below the piles. */
	private static final double ACTIONS_BOTTOM_GAP = CARD_H + 110 + CENTER_LIFT;

	private Deck deck;
	private Game game;
	private ArrayList<Player> players;
    private int playerCount;

	private Stage primaryStage;
    private Scene appScene;

    private Label setupMessage;
    private StackPane tableLayer;

    /** A player may draw only once per turn, and may then only play that card. */
    private boolean hasDrawnThisTurn;
    private Card drawnCard;

    /** Players sitting on one card who called Uno in time. */
    private final Set<Player> calledUno = new HashSet<>();
    /** A player who went down to one card without calling Uno, and the turn
     *  they did it on. They can be caught for as long as the next turn lasts. */
    private Player uncalledUno;
    private int uncalledUnoTurn;
    private int turnNumber;
    /** Set between playing a second to last card and handing the turn over. */
    private Player awaitingUnoCall;

	public Main() {
		this.playerCount = MIN_PLAYERS;
	}

	@Override
	public void start(Stage stage) {
		try {
			primaryStage = stage;
			primaryStage.setTitle("Uno " + APP_VERSION);
			primaryStage.initStyle(StageStyle.UNDECORATED);

			URL iconUrl = getClass().getResource("/resources/uno.png");
			if(iconUrl != null)
			{
				primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
			}

			playerCount = MIN_PLAYERS;

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
        frame.getStyleClass().addAll("uno-design", "window-frame");
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
        setupPanel.setPrefWidth(400);
        // Hug the content instead of stretching to fill the window.
        setupPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label title = new Label("UNO");
        title.getStyleClass().add("setup-title");

        Label subtitle = new Label("Set up players");
        subtitle.getStyleClass().add("setup-subtitle");

        ComboBox<Integer> playerCountBox = new ComboBox<>(FXCollections.observableArrayList(2, 3, 4));
        playerCountBox.setValue(playerCount);
        playerCountBox.getStyleClass().add("setup-control");

        VBox nameFields = new VBox(10);
        nameFields.getStyleClass().add("name-fields");
        renderNameFields(nameFields, playerCount);

        playerCountBox.setOnAction(_ -> {
            playerCount = playerCountBox.getValue();
            renderNameFields(nameFields, playerCount);
        });

        setupMessage = new Label();
        setupMessage.getStyleClass().add("setup-message");

        Button startButton = new Button("Start Game");
        startButton.getStyleClass().add("primary-button");
        startButton.setOnAction(_ -> startGame(nameFields));

        Button rulesButton = new Button("Read Rules");
        rulesButton.getStyleClass().add("primary-button");
        rulesButton.setTooltip(new Tooltip("Open the official Uno rules"));
        rulesButton.setOnAction(_ -> getHostServices().showDocument("https://www.unorules.com/"));

        HBox startActions = new HBox(10, startButton, rulesButton);
        startActions.setAlignment(Pos.CENTER_LEFT);

        setupPanel.getChildren().addAll(title, subtitle, labelledControl("Players", playerCountBox), nameFields,
                startActions, setupMessage);

        StackPane centered = new StackPane(setupPanel);
        centered.getStyleClass().add("setup-stage");
        root.setCenter(centered);
        return root;
    }

    private void renderNameFields(VBox nameFields, int count)
    {
        nameFields.getChildren().clear();

        for(int i = 0; i < count; i++)
        {
            TextField field = new TextField();
            field.getStyleClass().add("name-field");
            field.setPromptText("Player " + (i + 1) + " name");
            nameFields.getChildren().add(field);
        }
    }

    private VBox labelledControl(String labelText, ComboBox<Integer> control)
    {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        return new VBox(6, label, control);
    }

    private void startGame(VBox nameFields)
    {
        List<String> playerNames = new ArrayList<>();

        for(Node node : nameFields.getChildren())
        {
            if(node instanceof TextField field)
            {
                String name = field.getText().trim();
                playerNames.add(name.isEmpty() ? "Player " + (playerNames.size() + 1) : name);
            }
        }

        if(playerNames.size() < MIN_PLAYERS || playerNames.size() > MAX_PLAYERS)
        {
            setupMessage.setText("Please pick between " + MIN_PLAYERS + " and " + MAX_PLAYERS + " players.");
            return;
        }

        startGame(playerNames);
    }

    private void startGame(List<String> playerNames)
    {
        players = new ArrayList<>();

        for(int i = 0; i < playerNames.size(); i++)
        {
            Player player = new Player(i);
            player.setPlayerName(playerNames.get(i));
            players.add(player);
        }

        deck = new Deck();
        game = new Game(players, deck);
        game.startGame(players);

        calledUno.clear();
        uncalledUno = null;
        awaitingUnoCall = null;
        turnNumber = 0;

        beginTurn(false);
    }

    // ------------------------------------------------------------------
    // Game table
    // ------------------------------------------------------------------

    private Parent createGameScreen()
    {
        StackPane table = new StackPane();
        table.getStyleClass().add("table-felt");

        List<Player> seated = seatingOrder();
        Pos[] spots = seatSpots(seated.size());
        double[] angles = seatAngles(seated.size());

        for(int i = 1; i < seated.size(); i++)
        {
            Node seat = createSeat(seated.get(i), false, angles[i]);
            StackPane.setAlignment(seat, spots[i]);
            table.getChildren().add(seat);
        }

        Node center = createTableCenter();
        StackPane.setAlignment(center, Pos.CENTER);
        table.getChildren().add(center);

        // The prompt sits just above the player's hand rather than under the
        // piles, so it does not push the piles off centre.
        Node actions = createTurnActions(seated.get(0));
        StackPane.setAlignment(actions, Pos.BOTTOM_CENTER);
        StackPane.setMargin(actions, new Insets(0, 0, ACTIONS_BOTTOM_GAP, 0));
        table.getChildren().add(actions);

        Node me = createSeat(seated.get(0), true, 0);
        StackPane.setAlignment(me, Pos.BOTTOM_CENTER);
        table.getChildren().add(me);

        Button newGame = new Button("Start New Game");
        newGame.getStyleClass().add("primary-button");
        newGame.setOnAction(_ -> showScene(createStartScreen()));
        StackPane.setAlignment(newGame, Pos.TOP_LEFT);
        table.getChildren().add(newGame);

        tableLayer = table;
        return table;
    }

    /**
     * Orders the players so the one to move sits at the bottom of the table and
     * the rest follow around it in the current direction of play.
     */
    private List<Player> seatingOrder()
    {
        List<Player> seated = new ArrayList<>();
        int size = players.size();
        int step = game.isReversed() ? -1 : 1;

        for(int i = 0; i < size; i++)
        {
            int index = Math.floorMod(viewIndex() + i * step, size);
            seated.add(players.get(index));
        }

        return seated;
    }

    /**
     * Whose table this is. Normally the player to move, but an action card
     * advances the turn as part of its effect, so during the Uno call moment
     * the table still belongs to the player who just played.
     */
    private int viewIndex()
    {
        if(awaitingUnoCall != null)
        {
            return players.indexOf(awaitingUnoCall);
        }
        return game.getCurrentPlayerIndex();
    }

    private Pos[] seatSpots(int count)
    {
        return switch(count) {
            case 2 -> new Pos[] { Pos.BOTTOM_CENTER, Pos.TOP_CENTER };
            case 3 -> new Pos[] { Pos.BOTTOM_CENTER, Pos.CENTER_LEFT, Pos.CENTER_RIGHT };
            default -> new Pos[] { Pos.BOTTOM_CENTER, Pos.CENTER_LEFT, Pos.TOP_CENTER, Pos.CENTER_RIGHT };
        };
    }

    private double[] seatAngles(int count)
    {
        return switch(count) {
            case 2 -> new double[] { 0, 180 };
            case 3 -> new double[] { 0, -90, 90 };
            default -> new double[] { 0, -90, 180, 90 };
        };
    }

    /**
     * Builds one seat: the player's name plus their fanned hand, rotated so it
     * faces the middle of the table.
     */
    private Node createSeat(Player player, boolean faceUp, double angle)
    {
        Label name = new Label(player.getPlayerName() + "  (" + player.getCards().size() + ")");
        name.getStyleClass().add("seat-name");
        if(faceUp)
        {
            name.getStyleClass().add("current");
        }
        // Only a player who actually called it gets the announcement; anyone
        // else sitting on one card is quietly catchable.
        if(player.getCards().size() == 1 && calledUno.contains(player))
        {
            name.getStyleClass().add("uno-call");
            name.setText(player.getPlayerName() + "  UNO!");
        }

        Node fan = createFan(player, faceUp);

        VBox seat = new VBox(8);
        seat.setAlignment(Pos.CENTER);
        seat.getStyleClass().add("seat");

        // Side seats read better with the name on the inner edge, so the fan
        // comes first there and the rotation puts the label towards the middle.
        if(angle == 0 || angle == 180)
        {
            seat.getChildren().addAll(name, fan);
        }
        else
        {
            seat.getChildren().addAll(fan, name);
        }

        seat.setRotate(angle);
        return new Group(seat);
    }

    /**
     * Lays the hand out as an overlapping arc, the way cards sit in a real hand.
     */
    private Node createFan(Player player, boolean faceUp)
    {
        List<Card> cards = player.getCards();
        int count = cards.size();

        HBox fan = new HBox();
        fan.setAlignment(Pos.CENTER);
        fan.getStyleClass().add("hand-fan");
        // Pin the height: greyed cards are wrapped in a Group, whose bounds
        // include the fan rotation, which would otherwise move the seat around.
        fan.setMinHeight(FAN_HEIGHT);
        fan.setPrefHeight(FAN_HEIGHT);
        fan.setMaxHeight(FAN_HEIGHT);

        double overlap = count <= 8 ? 0.42 : Math.min(0.78, 0.42 + (count - 8) * 0.04);
        fan.setSpacing(-CARD_W * overlap);

        double step = count <= 1 ? 0 : Math.min(5.5, 40.0 / count);

        for(int i = 0; i < count; i++)
        {
            Card card = cards.get(i);
            double offset = i - (count - 1) / 2.0;
            double baseY = offset * offset * 2.2;

            StackPane cardNode = faceUp ? createCardFace(card, CARD_W, CARD_H) : createCardBack(CARD_W, CARD_H);
            Node node = cardNode;

            if(faceUp)
            {
                if(card == drawnCard)
                {
                    cardNode.getStyleClass().add("drawn");
                }

                if(isPlayableNow(card))
                {
                    cardNode.getStyleClass().add("playable");
                    cardNode.setOnMouseClicked(_ -> playCard(player, card));
                }
                else
                {
                    // Cards that cannot be played this turn grey out. The effect goes
                    // on a wrapper because the stylesheet owns the card's own effect.
                    ColorAdjust dim = new ColorAdjust();
                    dim.setSaturation(-0.9);
                    dim.setBrightness(-0.3);

                    Group greyed = new Group(cardNode);
                    greyed.setEffect(dim);
                    node = greyed;
                }
            }

            final Node fanned = node;
            fanned.setRotate(offset * step);
            fanned.setTranslateY(baseY);

            if(faceUp && isPlayableNow(card))
            {
                fanned.setOnMouseEntered(_ -> {
                    fanned.setTranslateY(baseY - HOVER_LIFT);
                    fanned.setViewOrder(-1);
                });
                fanned.setOnMouseExited(_ -> {
                    fanned.setTranslateY(baseY);
                    fanned.setViewOrder(0);
                });
            }

            fan.getChildren().add(fanned);
        }

        return fan;
    }

    /**
     * Builds the middle of the table: whose turn it is, the discard pile and the
     * draw pile.
     */
    private Node createTableCenter()
    {
        Player current = players.get(viewIndex());
        Card topCard = game.getTopCard();

        Label turn = new Label(current.getPlayerName() + "'s turn");
        turn.getStyleClass().add("turn-label");

        Circle colorChip = new Circle(9, colorOf(topCard));
        colorChip.getStyleClass().add("color-chip");

        Label direction = new Label(game.isReversed() ? "↺" : "↻");
        direction.getStyleClass().add("direction-label");
        direction.setTooltip(new Tooltip(game.isReversed() ? "Playing counter-clockwise" : "Playing clockwise"));

        HBox turnRow = new HBox(10, colorChip, turn, direction);
        turnRow.setAlignment(Pos.CENTER);

        Node discard = createCardFace(topCard, PILE_CARD_W, PILE_CARD_H);
        discard.setRotate(-7);
        VBox discardPile = pileBox("DISCARD", new StackPane(discard));

        StackPane drawStack = new StackPane();
        for(int i = 0; i < 3; i++)
        {
            Node back = createCardBack(PILE_CARD_W, PILE_CARD_H);
            back.setTranslateX(i * 2.5);
            back.setTranslateY(i * 2.5);
            drawStack.getChildren().add(back);
        }
        drawStack.getStyleClass().add("draw-pile");
        if(hasDrawnThisTurn || awaitingUnoCall != null)
        {
            // One draw per turn: the pile is spent until play moves on.
            drawStack.getStyleClass().add("spent");
            Tooltip.install(drawStack, new Tooltip("You have already drawn this turn"));
        }
        else
        {
            drawStack.setOnMouseClicked(_ -> drawForCurrentPlayer());
            Tooltip.install(drawStack, new Tooltip("Draw a card"));
        }
        VBox drawPile = pileBox("DRAW  (" + deck.size() + ")", drawStack);

        HBox piles = new HBox(30, discardPile, drawPile);
        piles.setAlignment(Pos.CENTER);

        VBox center = new VBox(16, turnRow, piles);
        center.setAlignment(Pos.CENTER);
        center.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        center.setTranslateY(-CENTER_LIFT);
        return center;
    }

    /**
     * The prompt under the piles. After a draw the player either plays the card
     * they just drew or passes, which is what ends the turn.
     */
    private Node createTurnActions(Player current)
    {
        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // The card is played and the player is holding their last one: this is
        // the moment to call it, before play moves on.
        if(awaitingUnoCall == current)
        {
            Label hint = new Label("You are down to your last card.");
            hint.getStyleClass().add("table-hint");

            Button sayUno = new Button("Say UNO!");
            sayUno.getStyleClass().addAll("primary-button", "uno-button");
            sayUno.setTooltip(new Tooltip("Call it now or the next player can catch you"));
            sayUno.setOnAction(_ -> callUno(current));

            Button quiet = new Button("End turn");
            quiet.getStyleClass().add("primary-button");
            quiet.setTooltip(new Tooltip("Say nothing and hope nobody notices"));
            quiet.setOnAction(_ -> endUnoCallMoment(current));

            HBox row = new HBox(10, sayUno, quiet);
            row.setAlignment(Pos.CENTER);

            actions.getChildren().addAll(hint, row);
            return actions;
        }

        boolean canPlayDrawn = hasDrawnThisTurn && drawnCard != null && game.isValidMove(drawnCard);

        String message;
        if(!hasDrawnThisTurn)
        {
            message = current.hasPlayableCard(game)
                    ? "Play a card, or draw from the pile."
                    : "No card you can play — draw from the pile.";
        }
        else
        {
            message = canPlayDrawn
                    ? "You drew a playable card — play it or pass."
                    : "You drew a card you cannot play.";
        }

        Label hint = new Label(message);
        hint.getStyleClass().add("table-hint");
        actions.getChildren().add(hint);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);

        if(uncalledUno != null && uncalledUno != current)
        {
            Button catchThem = new Button("Catch " + uncalledUno.getPlayerName() + " — no UNO!");
            catchThem.getStyleClass().addAll("primary-button", "catch-button");
            catchThem.setTooltip(new Tooltip(uncalledUno.getPlayerName() + " draws 2 cards"));
            catchThem.setOnAction(_ -> catchUncalledUno());
            buttons.getChildren().add(catchThem);
        }

        if(hasDrawnThisTurn)
        {
            Button pass = new Button(canPlayDrawn ? "Pass" : "End turn");
            pass.getStyleClass().add("primary-button");
            pass.setOnAction(_ -> passTurn());
            buttons.getChildren().add(pass);
        }

        if(!buttons.getChildren().isEmpty())
        {
            actions.getChildren().add(buttons);
        }

        return actions;
    }

    private VBox pileBox(String caption, Node pile)
    {
        Label label = new Label(caption);
        label.getStyleClass().add("pile-caption");

        VBox box = new VBox(8, label, pile);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ------------------------------------------------------------------
    // Card rendering
    // ------------------------------------------------------------------

    private StackPane createCardFace(Card card, double w, double h)
    {
        StackPane node = cardShell(w, h);
        node.getStyleClass().add(colorStyleClass(card));

        node.getChildren().add(createCardOval(card, w, h));

        Text symbol = new Text(cardSymbol(card));
        symbol.setFont(symbolFont(card, h));
        symbol.setFill(Color.WHITE);
        symbol.setStroke(inkOf(card));
        symbol.setStrokeWidth(h * 0.030);
        node.getChildren().add(symbol);

        Text topLeft = cornerText(card, h);
        StackPane.setAlignment(topLeft, Pos.TOP_LEFT);
        StackPane.setMargin(topLeft, new Insets(h * 0.055, 0, 0, w * 0.09));

        Text bottomRight = cornerText(card, h);
        bottomRight.setRotate(180);
        StackPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bottomRight, new Insets(0, w * 0.09, h * 0.055, 0));

        node.getChildren().addAll(topLeft, bottomRight);
        return node;
    }

    private StackPane createCardBack(double w, double h)
    {
        StackPane node = cardShell(w, h);
        node.getStyleClass().add("card-back");

        Ellipse oval = new Ellipse(w * 0.33, h * 0.40);
        oval.setFill(Color.web("#d92b1e"));
        oval.setStroke(Color.web("#7f1208"));
        oval.setStrokeWidth(1.5);
        oval.setRotate(-22);

        Text logo = new Text("UNO");
        logo.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, FontPosture.ITALIC, h * 0.20));
        logo.setFill(Color.WHITE);
        logo.setStroke(Color.web("#2b0a06"));
        logo.setStrokeWidth(h * 0.008);
        logo.setRotate(-22);

        node.getChildren().addAll(oval, logo);
        return node;
    }

    private StackPane cardShell(double w, double h)
    {
        StackPane node = new StackPane();
        node.getStyleClass().add("uno-card");
        node.setPrefSize(w, h);
        node.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return node;
    }

    /**
     * The white oval in the middle of a card. Wild cards that have not been
     * assigned a color yet get the four-colour version instead.
     */
    private Node createCardOval(Card card, double w, double h)
    {
        double rx = w * 0.29;
        double ry = h * 0.33;

        if(isUncoloredWild(card))
        {
            Group quarters = new Group();
            Color[] colors = { Color.web("#d0211c"), Color.web("#e8b71a"), Color.web("#2f9e41"), Color.web("#1c6fd0") };

            for(int i = 0; i < 4; i++)
            {
                Arc quarter = new Arc(0, 0, rx, ry, i * 90, 90);
                quarter.setType(ArcType.ROUND);
                quarter.setFill(colors[i]);
                quarters.getChildren().add(quarter);
            }

            quarters.setRotate(-22);
            return quarters;
        }

        Ellipse oval = new Ellipse(rx, ry);
        oval.setFill(Color.WHITE);
        oval.setRotate(-22);
        return oval;
    }

    private Text cornerText(Card card, double h)
    {
        Text text = new Text(cardSymbol(card));
        text.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, cornerPosture(card), h * 0.135));
        text.setFill(Color.WHITE);
        return text;
    }

    /**
     * Numbers get the slanted look of a real Uno card; the action symbols stay
     * upright because they are hard to read when they are skewed.
     */
    private Font symbolFont(Card card, double h)
    {
        if(card != null && card.isActionCard())
        {
            double size = card.getValue() == 12 || card.isPlusFour() ? h * 0.30 : h * 0.34;
            return Font.font("Segoe UI", FontWeight.EXTRA_BOLD, FontPosture.REGULAR, size);
        }
        return Font.font("Segoe UI", FontWeight.EXTRA_BOLD, FontPosture.ITALIC, h * 0.42);
    }

    private FontPosture cornerPosture(Card card)
    {
        return card != null && card.isActionCard() ? FontPosture.REGULAR : FontPosture.ITALIC;
    }

    private String cardSymbol(Card card)
    {
        if(card == null)
        {
            return "";
        }
        if(card.isPlusFour())
        {
            return "+4";
        }
        if(card.isWild())
        {
            return "W";
        }

        return switch(card.getValue()) {
            case 10 -> "⇅";
            case 11 -> "⊘";
            case 12 -> "+2";
            default -> String.valueOf(card.getValue());
        };
    }

    private String colorStyleClass(Card card)
    {
        if(card == null)
        {
            return "card-wild";
        }

        return switch(card.getColor()) {
            case "R" -> "card-red";
            case "G" -> "card-green";
            case "B" -> "card-blue";
            case "Y" -> "card-yellow";
            default -> "card-wild";
        };
    }

    private Color colorOf(Card card)
    {
        if(card == null)
        {
            return Color.web("#e2e8f0");
        }

        return switch(card.getColor()) {
            case "R" -> Color.web("#d0211c");
            case "G" -> Color.web("#2f9e41");
            case "B" -> Color.web("#1c6fd0");
            case "Y" -> Color.web("#e8b71a");
            default -> Color.web("#e2e8f0");
        };
    }

    /** Darker version of the card colour, used to outline the numbers. */
    private Color inkOf(Card card)
    {
        if(isUncoloredWild(card))
        {
            return Color.web("#1a1a1a");
        }
        return colorOf(card).darker();
    }

    private boolean isUncoloredWild(Card card)
    {
        return card != null && card.isWild() && !card.getColor().matches("[RGBY]");
    }

    // ------------------------------------------------------------------
    // Turn handling
    // ------------------------------------------------------------------

    /**
     * A card is playable if the rules allow it on the current top card, and,
     * once the player has drawn, only the drawn card may still be played.
     */
    private boolean isPlayableNow(Card card)
    {
        if(awaitingUnoCall != null)
        {
            return false;
        }
        if(hasDrawnThisTurn && card != drawnCard)
        {
            return false;
        }
        return game.isValidMove(card);
    }

    private void playCard(Player player, Card card)
    {
        if(!isPlayableNow(card))
        {
            return;
        }

        // A wild leaves the hand only once a color is picked, so backing out of
        // the color dialog puts the player back exactly where they were.
        if(card.isWild())
        {
            chooseWildColor(color -> {
                player.remove(card);
                game.applyCard(card, color);
                resolvePlay(player);
            });
        }
        else
        {
            player.remove(card);
            game.applyCard(card, null);
            resolvePlay(player);
        }
    }

    /**
     * Hands the turn on, unless the play left the player holding a single card,
     * in which case they first get the chance to call Uno.
     */
    private void resolvePlay(Player player)
    {
        // Reaching one card by playing is always a fresh descent from two, so
        // an earlier call does not carry over: they have to call it again.
        if(player.getCards().size() == 1)
        {
            calledUno.remove(player);
            awaitingUnoCall = player;
            showScene(createGameScreen());
            return;
        }

        endTurn(player);
    }

    /** Chose to stay quiet: the turn ends and the player is catchable. */
    private void endUnoCallMoment(Player player)
    {
        awaitingUnoCall = null;
        endTurn(player);
    }

    /**
     * Draws a single card. Uno allows one draw per turn: the player may then
     * play that card if it fits, otherwise passing is their only move.
     */
    private void drawForCurrentPlayer()
    {
        if(hasDrawnThisTurn)
        {
            return;
        }

        drawnCard = game.drawCard(players.get(game.getCurrentPlayerIndex()));
        hasDrawnThisTurn = true;
        showScene(createGameScreen());
    }

    /** Ends the turn without playing, which is only allowed after drawing. */
    private void passTurn()
    {
        if(!hasDrawnThisTurn)
        {
            return;
        }

        Player passer = players.get(game.getCurrentPlayerIndex());
        game.nextPlayer();
        beginTurn(playerToMove() == passer);
    }

    private void endTurn(Player player)
    {
        // Down to one card without calling it: catchable on the next turn.
        if(player.getCards().size() == 1 && !calledUno.contains(player))
        {
            uncalledUno = player;
            uncalledUnoTurn = turnNumber;
        }

        if(player.hasWon())
        {
            hasDrawnThisTurn = false;
            drawnCard = null;
            showScene(createGameScreen());
            showWinner(player);
            return;
        }

        game.nextPlayer();

        // Skips, reverses and draw cards advance the turn as part of their own
        // effect, so play can land back on the player who just went. Compare
        // against them, not against the index before this call.
        beginTurn(playerToMove() == player);
    }

    private Player playerToMove()
    {
        return players.get(game.getCurrentPlayerIndex());
    }

    /**
     * Starts the next turn. The hand-off screen is skipped when play stays with
     * the same person, which happens on a skip or draw card in a two player game.
     */
    private void beginTurn(boolean samePlayer)
    {
        turnNumber++;
        hasDrawnThisTurn = false;
        drawnCard = null;
        awaitingUnoCall = null;

        // A call only stands while the player is actually down to one card.
        calledUno.removeIf(p -> p.getCards().size() != 1);

        // The chance to catch someone closes once the next turn is over.
        if(uncalledUno != null && turnNumber > uncalledUnoTurn + 1)
        {
            uncalledUno = null;
        }

        if(samePlayer)
        {
            showScene(createGameScreen());
        }
        else
        {
            showScene(createBufferScreen());
        }
    }

    private void callUno(Player player)
    {
        calledUno.add(player);

        // Calling it clears the player's own exposure.
        if(uncalledUno == player)
        {
            uncalledUno = null;
        }

        if(awaitingUnoCall == player)
        {
            awaitingUnoCall = null;
            endTurn(player);
            return;
        }

        showScene(createGameScreen());
    }

    /** The standard penalty for being caught without calling Uno: draw 2. */
    private void catchUncalledUno()
    {
        if(uncalledUno == null)
        {
            return;
        }

        game.drawTwoCards(uncalledUno);
        uncalledUno = null;
        showScene(createGameScreen());
    }

    private Parent createBufferScreen()
    {
        Player currentPlayer = players.get(game.getCurrentPlayerIndex());
        Label prompt = new Label(currentPlayer.getPlayerName() + ", ready for your turn?");
        prompt.getStyleClass().add("overlay-title");

        Button ready = new Button("Ready");
        ready.getStyleClass().add("primary-button");
        ready.setOnAction(_ -> {
            showScene(createGameScreen());
        });
        
        VBox panel = new VBox(18, prompt, ready);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("overlay-panel");
        
        StackPane root = new StackPane(panel);
        root.getStyleClass().add("table-felt");
        return root;
    }

    private void chooseWildColor(Consumer<String> onChosen)
    {
        Label prompt = new Label("Pick a color");
        prompt.getStyleClass().add("overlay-title");

        HBox swatches = new HBox(14);
        swatches.setAlignment(Pos.CENTER);

        String[] codes = { "R", "G", "B", "Y" };
        String[] names = { "Red", "Green", "Blue", "Yellow" };
        String[] classes = { "swatch-red", "swatch-green", "swatch-blue", "swatch-yellow" };

        VBox panel = new VBox(18, prompt, swatches);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("overlay-panel");

        StackPane overlay = showOverlay(panel);

        for(int i = 0; i < codes.length; i++)
        {
            String code = codes[i];
            Button swatch = new Button(names[i]);
            swatch.getStyleClass().addAll("swatch", classes[i]);
            swatch.setOnAction(_ -> {
                tableLayer.getChildren().remove(overlay);
                onChosen.accept(code);
            });
            swatches.getChildren().add(swatch);
        }

        // Nothing has been played yet, so closing simply returns to the turn.
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("primary-button", "cancel-button");
        cancel.setTooltip(new Tooltip("Keep the card and choose another move"));
        cancel.setOnAction(_ -> tableLayer.getChildren().remove(overlay));
        panel.getChildren().add(cancel);
    }

    private void showWinner(Player winner)
    {
        Label title = new Label(winner.getPlayerName() + " wins!");
        title.getStyleClass().add("overlay-title");

        Label subtitle = new Label("All cards played.");
        subtitle.getStyleClass().add("overlay-subtitle");

        Button newGame = new Button("Start New Game");
        newGame.getStyleClass().add("primary-button");
        newGame.setOnAction(_ -> showScene(createStartScreen()));

        HBox actions = new HBox(12, newGame);
        actions.setAlignment(Pos.CENTER);

        VBox panel = new VBox(16, title, subtitle, actions);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("overlay-panel");

        showOverlay(panel);
    }

    private StackPane showOverlay(Node content)
    {
        StackPane overlay = new StackPane(content);
        overlay.getStyleClass().add("overlay");
        tableLayer.getChildren().add(overlay);
        return overlay;
    }

	public static void main(String[] args) {
		launch(args);
	}
}
