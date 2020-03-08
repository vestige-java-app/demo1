package fr.gaellalire.vestige_app.demo1.action;

public interface Promise<Context> {

    void then(Promise<? super Context> promise);

    void conditionalThen(Condition<? super Context> condition, Promise<? super Context> promise);

  //  <Type> Promise<Context> then(ContextAccessor<? super Context, Type> accessor, CompositeAction<? super Type> action);

}
