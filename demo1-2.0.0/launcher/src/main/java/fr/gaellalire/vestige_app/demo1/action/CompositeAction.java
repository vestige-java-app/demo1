package fr.gaellalire.vestige_app.demo1.action;

public interface CompositeAction<Context extends ActionContext> {

    void describe(CompositeActionHelper<Context> compositeActionHelper);

}
