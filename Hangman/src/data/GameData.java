package data;

import apptemplate.AppTemplate;
import components.AppDataComponent;
import ui.AppMessageDialogSingleton;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Ritwik Banerjee
 */
public class GameData implements AppDataComponent {

    public static final  int TOTAL_NUMBER_OF_GUESSES_ALLOWED = 10;
    private static final int TOTAL_NUMBER_OF_STORED_WORDS    = 330622;
    public static final int HINT_THRESHOLD                  = 7;

    private String         targetWord;
    private Set<Character> goodGuesses;
    private Set<Character> badGuesses;
    private int            remainingGuesses;
    public  AppTemplate    appTemplate;
    private  boolean        hintState;

    public GameData(AppTemplate appTemplate) {
        this(appTemplate, false);
    }

    public GameData(AppTemplate appTemplate, boolean initiateGame) {
        if (initiateGame) {
            this.appTemplate = appTemplate;
            this.targetWord = setTargetWord();
            this.goodGuesses = new HashSet<>();
            this.badGuesses = new HashSet<>();
            this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        } else {
            this.appTemplate = appTemplate;
        }
    }

    public void init() {
        this.targetWord = setTargetWord();
        this.goodGuesses = new HashSet<>();
        this.badGuesses = new HashSet<>();
        this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
    }

    @Override
    public void reset() {
        this.targetWord = null;
        hintState = false;
        this.goodGuesses = new HashSet<>();
        this.badGuesses = new HashSet<>();
        this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        appTemplate.getWorkspaceComponent().reloadWorkspace();
    }

    public String getTargetWord() {
        return targetWord;
    }

    private String setTargetWord() {
        URL wordsResource = getClass().getClassLoader().getResource("words/words.txt");
        assert wordsResource != null;
        String word = null;
        while(word == null){
            int toSkip = new Random().nextInt(TOTAL_NUMBER_OF_STORED_WORDS);
            try (Stream<String> lines = Files.lines(Paths.get(wordsResource.toURI()))) {
                word =  lines.skip(toSkip).findFirst().get();
                if(!word.chars().allMatch(Character::isLetter)) word = null;
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                System.exit(1);
            } catch(NoSuchElementException e){
                AppMessageDialogSingleton dialogSingleton = AppMessageDialogSingleton.getSingleton();
                dialogSingleton.show("ERROR","Unable to load initial target word.");
                break;

            }
        }
        if(word == null){
            System.exit(0);
        }
        return word;
        /*int toSkip = new Random().nextInt(TOTAL_NUMBER_OF_STORED_WORDS);
        try (Stream<String> lines = Files.lines(Paths.get(wordsResource.toURI()))) {
            return lines.skip(toSkip).findFirst().get();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            System.exit(1);
        }*/

        //throw new GameError("Unable to load initial target word.");
    }

    public GameData setTargetWord(String targetWord) {
        this.targetWord = targetWord;
        return this;
    }

    public Set<Character> getGoodGuesses() {
        return goodGuesses;
    }

    public GameData setGoodGuesses(Set<Character> goodGuesses) {
        this.goodGuesses = goodGuesses;
        return this;
    }

    public Set<Character> getBadGuesses() {
        return badGuesses;
    }

    public GameData setBadGuesses(Set<Character> badGuesses) {
        this.badGuesses = badGuesses;
        return this;
    }

    public int getRemainingGuesses() {
        return remainingGuesses;
    }

    public void addGoodGuess(char c) {
        goodGuesses.add(c);
    }

    public void addBadGuess(char c) {
        if (!badGuesses.contains(c)) {
            badGuesses.add(c);
            remainingGuesses--;
        }
    }

    public void reduceRemainingGuess(){
        --this.remainingGuesses;
    }

    public boolean getHintIsUsed(){
        return hintState;
    }

    public void setHintState(boolean state){
        this.hintState = state;
    }
}
