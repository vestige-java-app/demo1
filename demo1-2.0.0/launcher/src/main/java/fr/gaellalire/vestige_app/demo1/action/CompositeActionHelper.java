package fr.gaellalire.vestige_app.demo1.action;


public interface CompositeActionHelper<Context extends ActionContext> {

    Promise<Context> getFirstPromise();

    Promise<Context> simpleAction(SimpleAction<? super Context> simpleAction);

    Promise<Context> action(Action<? super Context> action);

    <Type> Promise<Context> action(ContextAccessor<? super Context, Type> accessor, Action<? super Type> action);

    <SubContext extends ActionContext> Promise<Context> fork(ContextAllocator<SubContext, ? super Context> allocator, CompositeAction<SubContext> compositeAction);

}
