package fr.gaellalire.vestige_app.demo1.action;


public interface ActionExecutor {

    <Context extends ActionContext> ExecuteContext<Context> createExecuteContext(CompositeAction<Context> compositeAction);

}
