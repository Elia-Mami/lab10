package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private final DrawNumber model;
    private final List<DrawNumberView> views;
    private final static int MIN = 0;
    private final static int MAX = 100;
    private final static int ATTEMPTS = 10;


    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String configFile, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }

        Configuration.Builder customBuilder = new Configuration.Builder();
        try(BufferedReader settings = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(configFile)))){
            for(String line = settings.readLine(); line != null; line = settings.readLine()){
                final String[] configs = line.split(":");
                
                switch(configs[0]){
                    case "maximum":
                        customBuilder.setMax(Integer.parseInt(configs[1].trim()));
                        break;
                    case "minimum":
                        customBuilder.setMin(Integer.parseInt(configs[1].trim()));
                        break;
                    case "attempts":
                        customBuilder.setAttempts(Integer.parseInt(configs[1].trim()));
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }
        catch(Exception ex){
            displayError(ex.getMessage());
        }
        final Configuration configuration = customBuilder.build();
        if(configuration.isConsistent()){
            this.model = new DrawNumberImpl(configuration.getMin(), configuration.getMax(), configuration.getAttempts());
        }
        else{
            displayError(new IllegalStateException().getMessage());
            //default case
            this.model = new DrawNumberImpl(MIN, MAX, ATTEMPTS);
        }
    }
    
    private void displayError(String message){
        for (final DrawNumberView view: views) {
            view.displayError(message);
        }   
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml", new DrawNumberViewImpl(), new DrawNumberViewImpl(), new PrintStreamView(System.out), new PrintStreamView("output.log"));
    }

}
