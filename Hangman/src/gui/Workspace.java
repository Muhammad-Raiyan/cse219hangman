package gui;

import apptemplate.AppTemplate;
import components.AppWorkspaceComponent;
import controller.HangmanController;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import propertymanager.PropertyManager;
import ui.AppGUI;

import java.io.IOException;
import java.util.HashMap;

import static hangman.HangmanProperties.*;

/**
 * This class serves as the GUI component for the Hangman game.
 *
 * @author Ritwik Banerjee
 */
public class Workspace extends AppWorkspaceComponent {

    AppTemplate app; // the actual application
    AppGUI      gui; // the GUI inside which the application sits

    Label             guiHeadingLabel;   // workspace (GUI) heading label
    HBox              headPane;          // conatainer to display the heading
    FlowPane bodyPane;          // container for the main game displays
    ToolBar           footToolbar;       // toolbar for game buttons
    BorderPane        figurePane;        // container to display the namesake graphic of the (potentially) hanging person
    VBox              gameTextsPane;     // container to display the text-related parts of the game
    FlowPane          guessedLetters;    // text area displaying all the letters guessed so far
    HBox              remainingGuessBox; // container to display the number of remaining guesses
    Button            startGame;         // the button to start playing a game of Hangman
    HangmanController controller;
    BorderPane        layout;
    StackPane         allLetter;
    HashMap<String, StackPane> letterBox;
    Button            hint;
    private Canvas canvas;
    private GraphicsContext gc;

    /**
     * Constructor for initializing the workspace, note that this constructor
     * will fully setup the workspace user interface for use.
     *
     * @param initApp The application this workspace is part of.
     * @throws IOException Thrown should there be an error loading application
     *                     data for setting up the user interface.
     */
    public Workspace(AppTemplate initApp) throws IOException {
        app = initApp;
        gui = app.getGUI();
        controller = (HangmanController) gui.getFileController();    //new HangmanController(app, startGame); <-- THIS WAS A MAJOR BUG!??
        layoutGUI();     // initialize all the workspace (GUI) components including the containers and their layout
        setupHandlers(); // ... and set up event handling
    }

    private void layoutGUI() {
        PropertyManager propertyManager = PropertyManager.getManager();
        guiHeadingLabel = new Label(propertyManager.getPropertyValue(WORKSPACE_HEADING_LABEL));
        canvas = new Canvas(550, 600);
        gc = canvas.getGraphicsContext2D();

        headPane = new HBox();
        headPane.getChildren().add(guiHeadingLabel);
        headPane.setAlignment(Pos.CENTER);

        bodyPane = new FlowPane();
        /*for(int i = 0; i<26; i++){
            allLetter = new StackPane();
            String temp =  Character.toString((char) ('A' + i));
            allLetter.getChildren().addAll(new Rectangle(25, 25, Color.GREEN), new Text(temp));
            bodyPane.getChildren().add(allLetter);
        }*/
        //allLetter.setVisible(true);

        guessedLetters = new FlowPane();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        remainingGuessBox = new HBox();
        gameTextsPane = new VBox();
        hint = new Button("HINT");
        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters, hint, bodyPane);

        startGame = new Button("Start Playing");
        HBox blankBoxLeft  = new HBox();
        HBox blankBoxRight = new HBox();
        HBox.setHgrow(blankBoxLeft, Priority.ALWAYS);
        HBox.setHgrow(blankBoxRight, Priority.ALWAYS);
        footToolbar = new ToolBar(blankBoxLeft, startGame, blankBoxRight);

        figurePane = new BorderPane();
        figurePane.setLeft(canvas);

        layout = new BorderPane();
        //layout.setBottom(footToolbar);
        layout.setRight(gameTextsPane);
        layout.setLeft(figurePane);
        //layout.setTop(headPane);

        workspace = new VBox();
        workspace.getChildren().addAll(headPane, footToolbar, layout);
        VBox.setVgrow(layout, Priority.ALWAYS);
    }

    public void drawHangman(int i){
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(5);
        switch(--i){
            case 0:
                gc.strokeLine(50, 450, 450, 450);   break;       //base
            case 1:
                gc.strokeLine(50, 50, 50, 450);     break;       //wall
            case 2:
                gc.strokeLine(50, 50, 400, 50);     break;       //roof
            case 3:
                gc.strokeLine(305, 50, 305, 120);   break;       //rope
            case 4:
                gc.strokeOval(275, 120, 70, 70);    break;        //head
            case 5:
                gc.strokeLine(310, 190, 310, 280);  break;        //body
            case 6:
                gc.strokeLine(310, 200, 280, 240);  break;      //left hand
            case 7:
                gc.strokeLine(310, 200, 340, 240);  break;     //right hand
            case 8:
                gc.strokeLine(310, 280, 270, 330);  break;       //left leg
            case 9:
                gc.strokeLine(310, 280, 350, 330);  break;      //right leg

        }
    }

    private void setupHandlers() {
        startGame.setOnMouseClicked(e -> controller.start());
        //hint.setOnMouseClicked(e -> controller.giveHint());
    }

    /**
     * This function specifies the CSS for all the UI components known at the time the workspace is initially
     * constructed. Components added and/or removed dynamically as the application runs need to be set up separately.
     */
    @Override
    public void initStyle() {
        PropertyManager propertyManager = PropertyManager.getManager();

        gui.getAppPane().setId(propertyManager.getPropertyValue(ROOT_BORDERPANE_ID));
        gui.getToolbarPane().getStyleClass().setAll(propertyManager.getPropertyValue(SEGMENTED_BUTTON_BAR));
        gui.getToolbarPane().setId(propertyManager.getPropertyValue(TOP_TOOLBAR_ID));

        ObservableList<Node> toolbarChildren = gui.getToolbarPane().getChildren();
        toolbarChildren.get(0).getStyleClass().add(propertyManager.getPropertyValue(FIRST_TOOLBAR_BUTTON));
        toolbarChildren.get(toolbarChildren.size() - 1).getStyleClass().add(propertyManager.getPropertyValue(LAST_TOOLBAR_BUTTON));

        workspace.getStyleClass().add(CLASS_BORDERED_PANE);
        guiHeadingLabel.getStyleClass().setAll(propertyManager.getPropertyValue(HEADING_LABEL));

    }

    /** This function reloads the entire workspace */
    @Override
    public void reloadWorkspace() {
        /* does nothing; use reinitialize() instead */
    }

    public VBox getGameTextsPane() {
        return gameTextsPane;
    }

    public StackPane getAllLetterBox(){
        return allLetter;
    }

    public HBox getRemainingGuessBox() {
        return remainingGuessBox;
    }

    public Button getStartGame() {
        return startGame;
    }

    public Button getHint(){
        return hint;
    }

    public void clearHangman(){
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    public void reinitialize() {
        guessedLetters = new FlowPane();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        remainingGuessBox = new HBox();
        bodyPane = new FlowPane();
        hint = new Button("HINT");
        hint.setVisible(false);
        gameTextsPane = new VBox();
        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters, hint, bodyPane);
        layout.setRight(gameTextsPane);
    }
}
