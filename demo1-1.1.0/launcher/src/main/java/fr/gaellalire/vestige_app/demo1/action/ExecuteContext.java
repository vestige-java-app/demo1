package fr.gaellalire.vestige_app.demo1.action;


public interface ExecuteContext<Context> {

    void execute(Context context);

    void pauseAndWait() throws InterruptedException;

    void resume();

    boolean isPaused();

}
