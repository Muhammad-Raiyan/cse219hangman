package controller;

import apptemplate.AppTemplate;
import data.GameData;
import gui.Workspace;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import propertymanager.PropertyManager;
import ui.AppMessageDialogSingleton;
import ui.YesNoCancelDialogSingleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static settings.AppPropertyType.*;
import static settings.InitializationParameters.APP_WORKDIR_PATH;

/**
 * @author Ritwik Banerjee
 */
public class HangmanController implements FileController {

    public enum GameState {
        UNINITIALIZED,
        INITIALIZED_UNMODIFIED,
        INITIALIZED_MODIFIED,
        ENDED
    }

    private AppTemplate appTemplate; // shared reference to the application
    private GameData    gamedata;    // shared reference to the game being played, loaded or saved
    private GameState   gamestate;   // the state of the game being shown in the workspace
    private Text[]      progress;    // reference to the text area for the word
    private boolean     success;     // whether or not player was successful
    private int         discovered;  // the number of letters already discovered
    private Button      gameButton;  // shared reference to the "start game" button
    private Label       remains;     // dynamically updated label that indicates the number of remaining guesses
    private Path        workFile;
    private Button      hintButton;
    private FlowPane    alphabet;

    public HangmanController(AppTemplate appTemplate, Button gameButton) {
        this(appTemplate);
        this.gameButton = gameButton;
    }

    public HangmanController(AppTemplate appTemplate) {
        this.appTemplate = appTemplate;
        this.gamestate = GameState.UNINITIALIZED;
    }

    public void enableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(false);
    }

    public void disableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(true);
    }

    public void setGameState(GameState gamestate) {
        this.gamestate = gamestate;
    }

    public GameState getGamestate() {
        return this.gamestate;
    }

    /**
     * In the homework code given to you, we had the line
     * gamedata = new GameData(appTemplate, true);
     * This meant that the 'gamedata' variable had access to the app, but the data component of the app was still
     * the empty game data! What we need is to change this so that our 'gamedata' refers to the data component of
     * the app, instead of being a new object of type GameData. There are several ways of doing this. One of which
     * is to write (and use) the GameData#init() method.
     */
    public void start() {
        gamedata = (GameData) appTemplate.getDataComponent();
        success = false;
        discovered = 0;

        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gamedata.init();
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        HBox remainingGuessBox = (HBox)gameWorkspace.getGameTextsPane().getChildren().get(0);
        FlowPane guessedLetters    = (FlowPane) gameWorkspace.getGameTextsPane().getChildren().get(1);
        alphabet = (FlowPane) gameWorkspace.getGameTextsPane().getChildren().get(3);
        alphabet.setPadding(new Insets(10));
        hintButton = (Button) gameWorkspace.getGameTextsPane().getChildren().get(2);
        hintButton.setDisable(false);
        gamedata.setHintState(false);
        if(!isHintable()) hintButton.setVisible(false);
        else    hintButton.setVisible(true);
        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        initWordGraphics(guessedLetters);
        drawLetterList(alphabet);
        play();
    }

    private void end() {
        appTemplate.getGUI().getPrimaryScene().setOnKeyTyped(null);
        gameButton.setDisable(true);
        hintButton.setDisable(true);
        setGameState(GameState.ENDED);
        fillOutEmptyBoxes();
        appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
        Platform.runLater(() -> {
            PropertyManager           manager    = PropertyManager.getManager();
            AppMessageDialogSingleton dialog     = AppMessageDialogSingleton.getSingleton();
            String                    endMessage = manager.getPropertyValue(success ? GAME_WON_MESSAGE : GAME_LOST_MESSAGE);
            if (dialog.isShowing())
                dialog.toFront();
            else
                dialog.show(manager.getPropertyValue(GAME_OVER_TITLE), endMessage);
        });
    }

    public void play() {
        disableGameButton();
        Workspace gameWorkspace =  (Workspace) appTemplate.getWorkspaceComponent();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
                appTemplate.getGUI().getPrimaryScene().setOnKeyTyped((KeyEvent event) -> {
                    char guess = Character.toLowerCase(event.getCharacter().charAt(0));
                    if (!alreadyGuessed(guess) && isValid(guess)) {
                        boolean goodguess = false;
                        useBox(guess);
                        for (int i = 0; i < progress.length; i++) {
                            if (gamedata.getTargetWord().charAt(i) == guess) {
                                progress[i].setVisible(true);
                                gamedata.addGoodGuess(guess);
                                goodguess = true;
                                discovered++;
                            }
                        }
                        if (!goodguess) {
                            gamedata.addBadGuess(guess);
                            gameWorkspace.drawHangman(10-gamedata.getRemainingGuesses());
                            if(gamedata.getRemainingGuesses()==1){
                                hintButton.setDisable(true);
                            }
                        }

                        success = (discovered == progress.length);
                        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                    }
                    setGameState(GameState.INITIALIZED_MODIFIED);
                });
                gameWorkspace.getHint().setOnMouseClicked((MouseEvent event) ->{
                    useBox(giveHint());
                    success = (discovered == progress.length);
                    remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                    gameWorkspace.drawHangman(10-gamedata.getRemainingGuesses());
                });
                if (gamedata.getRemainingGuesses() <= 0 || success)
                    stop();
            }

            @Override
            public void stop() {
                super.stop();
                end();
            }
        };
        timer.start();
    }

    public boolean isValid(char c){
        return c >= 'a' && c <= 'z';
    }

    public char giveHint() {
        hintButton.setDisable(true);
        gamedata.setHintState(true);
        setGameState(GameState.INITIALIZED_MODIFIED);
        char guess = selectAChar().charAt(0);
        for (int i = 0; i < progress.length; i++) {
            if (gamedata.getTargetWord().charAt(i) == guess) {
                progress[i].setVisible(true);
                discovered++;
            }
        }
        gamedata.addBadGuess(guess);
        gamedata.addGoodGuess(guess);
        //gamedata.reduceRemainingGuess();
        return guess;
    }

    public String selectAChar(){
        Random random = new Random();
        String letter = null;
        while(letter == null){
            int index = random.nextInt(gamedata.getTargetWord().length());
            letter = Character.toString(gamedata.getTargetWord().charAt(index));
            if(gamedata.getGoodGuesses().contains(letter.charAt(0))) letter = null;
        }

        return letter;
    }

    public boolean isHintable(){
        Set<Character> word = new HashSet<>();
        char[] test = gamedata.getTargetWord().toCharArray();
        for(Character c: test){
            word.add(c);
        }
        return word.size() > GameData.HINT_THRESHOLD;
    }
    private void initWordGraphics(FlowPane guessedLetters) {
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < progress.length; i++) {
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setFont(new Font(20));
            progress[i].setVisible(false);
        }
        guessedLetters.getChildren().addAll(progress);
        drawBox(guessedLetters);
    }

    public void fillOutEmptyBoxes(){
        for (Text progres : progress) {
            if (!progres.isVisible()) {
                progres.setVisible(true);
                progres.setFill(Color.RED);
            }
        }
    }

    public void useBox(char c){
        StackPane temp = (StackPane) alphabet.getChildren().get(c-'a');
        Rectangle r = (Rectangle) temp.getChildren().get(0);
        Text t = (Text) temp.getChildren().get(1);
        t.setFill(Color.WHITE);
        r.setFill(Color.BLUE);
    }

    private void drawLetterList(FlowPane alphabet){
        StackPane allLetter;
        for(int i = 0; i<26; i++){
            allLetter = new StackPane();
            String temp =  Character.toString((char) ('A' + i));
            Rectangle r = new Rectangle(50, 50, Color.YELLOW);
            Text t = new Text(temp);
            t.setFont(new Font(20));
            t.setFill(Color.BROWN);
            r.setStroke(Color.BLACK);
            allLetter.getChildren().addAll(r, t);
            alphabet.getChildren().add(allLetter);
        }
    }

    private void drawBox(Pane guessedLetters){
        String word = gamedata.getTargetWord();
        //System.out.println(gamedata.getTargetWord());
        StackPane pn;
        Rectangle r;
        if(progress==null) throw new NullPointerException("Error rendering graphics");
        for(int i = 0; i< word.length(); i++){
            pn = new StackPane();
            pn.setPadding(new Insets(5, 5, 5, 5));
            r = new Rectangle(30 , 30, Color.WHITE);
            r.setStroke(Color.BLACK);
            pn.getChildren().addAll(r, progress[i]);
            guessedLetters.getChildren().add(pn);
        }
    }

    private void restoreGUI() {
        disableGameButton();
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gameWorkspace.reinitialize();
        FlowPane guessedLetters = (FlowPane) gameWorkspace.getGameTextsPane().getChildren().get(1);
        restoreWordGraphics(guessedLetters);

        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        remains = new Label(Integer.toString(gamedata.getRemainingGuesses()));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        hintButton = gameWorkspace.getHint();
        if(gamedata.getHintIsUsed() || gamedata.getRemainingGuesses()==1) {
            hintButton.setDisable(true);
            hintButton.setVisible(true);
        }
        else if(isHintable()){
            hintButton.setDisable(false);
            hintButton.setVisible(true);
        }
        else {
            hintButton.setDisable(true);
            hintButton.setVisible(false);
        }
        success = false;
        int left = gamedata.getBadGuesses().size();
        gameWorkspace.clearHangman();
        while(left!=0){
            gameWorkspace.drawHangman(left);
            left--;
        }
        alphabet = (FlowPane) gameWorkspace.getGameTextsPane().getChildren().get(3);
        alphabet.setPadding(new Insets(10));
        drawLetterList(alphabet);
        reflect();
        play();
    }
    private void reflect(){
        HashSet<Character> good = (HashSet<Character>) gamedata.getGoodGuesses();
        HashSet<Character> bad = (HashSet<Character>) gamedata.getBadGuesses();
        for(Character it : good){
            useBox(it);
        }
        for(Character it: bad){
            useBox(it);
        }
    }
    private void restoreWordGraphics(FlowPane guessedLetters) {
        discovered = 0;
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < progress.length; i++) {
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)));
            progress[i].setFont(new Font(20));
            if (progress[i].isVisible())
                discovered++;
        }
        guessedLetters.getChildren().addAll(progress);
        drawBox(guessedLetters);
    }

    private boolean alreadyGuessed(char c) {
        return gamedata.getGoodGuesses().contains(c) || gamedata.getBadGuesses().contains(c);
    }

    @Override
    public void handleNewRequest() {
        AppMessageDialogSingleton messageDialog   = AppMessageDialogSingleton.getSingleton();
        PropertyManager           propertyManager = PropertyManager.getManager();
        boolean                   makenew         = true;
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            try {
                makenew = promptToSave();
            } catch (IOException e) {
                messageDialog.show(propertyManager.getPropertyValue(NEW_ERROR_TITLE), propertyManager.getPropertyValue(NEW_ERROR_MESSAGE));
            }
        if (makenew) {
            appTemplate.getDataComponent().reset();                // reset the data (should be reflected in GUI)
            appTemplate.getWorkspaceComponent().reloadWorkspace(); // load data into workspace
            ensureActivatedWorkspace();                            // ensure workspace is activated
            workFile = null;                                       // new workspace has never been saved to a file
            gameWorkspace.reinitialize();
            enableGameButton();
            gameWorkspace.clearHangman();
        }
        if (gamestate.equals(GameState.ENDED)) {
            appTemplate.getGUI().updateWorkspaceToolbar(false);
            gameWorkspace.reinitialize();
            gameWorkspace.clearHangman();
        }

    }

    @Override
    public void handleSaveRequest() throws IOException {
        PropertyManager propertyManager = PropertyManager.getManager();
        if (workFile == null) {
            FileChooser filechooser = new FileChooser();
            Path        appDirPath  = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path        targetPath  = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(SAVE_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                                                            String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showSaveDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null)
                save(selectedFile.toPath());
        } else
            save(workFile);
    }

    @Override
    public void handleLoadRequest() throws IOException {
        boolean load = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            load = promptToSave();
        if (load) {
            PropertyManager propertyManager = PropertyManager.getManager();
            AppMessageDialogSingleton messageDialog   = AppMessageDialogSingleton.getSingleton();
            FileChooser     filechooser     = new FileChooser();
            Path            appDirPath      = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path            targetPath      = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(LOAD_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                                                            String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showOpenDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null && selectedFile.exists()) {
                try {
                    load(selectedFile.toPath());
                    restoreGUI(); // restores the GUI to reflect the state in which the loaded game was last saved
                }
                catch (NullPointerException e){
                    messageDialog.show(propertyManager.getPropertyValue(LOAD_ERROR_TITLE), propertyManager.getPropertyValue(LOAD_ERROR_MESSAGE));
                }
            }
        }
    }

    @Override
    public void handleExitRequest() {
        try {
            boolean exit = true;
            if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
                exit = promptToSave();
            if (exit)
                System.exit(0);
        } catch (IOException ioe) {
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager           props  = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        }
    }

    private void ensureActivatedWorkspace() {
        appTemplate.getWorkspaceComponent().activateWorkspace(appTemplate.getGUI().getAppPane());
    }

    private boolean promptToSave() throws IOException {
        PropertyManager            propertyManager   = PropertyManager.getManager();
        YesNoCancelDialogSingleton yesNoCancelDialog = YesNoCancelDialogSingleton.getSingleton();

        yesNoCancelDialog.show(propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE),
                               propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_MESSAGE));

        if (yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES))
            handleSaveRequest();

        return !yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.CANCEL);
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param target The file to which the work will be saved.
     * @throws IOException
     */
    private void save(Path target) throws IOException {
        appTemplate.getFileComponent().saveData(appTemplate.getDataComponent(), target);
        workFile = target;
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(SAVE_COMPLETED_TITLE), props.getPropertyValue(SAVE_COMPLETED_MESSAGE));
    }

    /**
     * A helper method to load saved game data. It loads the game data, notified the user, and then updates the GUI to
     * reflect the correct state of the game.
     *
     * @param source The source data file from which the game is loaded.
     * @throws IOException
     */
    private void load(Path source) throws IOException {
        // load game data
        try {
            appTemplate.getFileComponent().loadData(appTemplate.getDataComponent(), source);
        }
        catch(IOException e){

        }
        // set the work file as the file from which the game was loaded
        workFile = source;

        // notify the user that load was successful
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(LOAD_COMPLETED_TITLE), props.getPropertyValue(LOAD_COMPLETED_MESSAGE));

        setGameState(GameState.INITIALIZED_UNMODIFIED);
        Workspace gameworkspace = (Workspace) appTemplate.getWorkspaceComponent();
        ensureActivatedWorkspace();
        gameworkspace.reinitialize();
        gamedata = (GameData) appTemplate.getDataComponent();
    }
}
