package fr.gaellalire.vestige_app.demo1.action;

public interface ContextAccessor<Context, Type> {

    Type access(Context context);

}
