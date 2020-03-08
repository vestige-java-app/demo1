package fr.gaellalire.vestige_app.demo1.action;


public interface Action<Context> {

    int DONE = 0;

    int PENDING = 1;

    int perform(ActionHelper actionHelper, Context context);

}
