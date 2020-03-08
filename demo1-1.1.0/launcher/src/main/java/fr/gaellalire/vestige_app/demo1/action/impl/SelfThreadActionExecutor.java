package fr.gaellalire.vestige_app.demo1.action.impl;

import java.util.ArrayList;
import java.util.List;

import fr.gaellalire.vestige_app.demo1.action.Action;
import fr.gaellalire.vestige_app.demo1.action.ActionContext;
import fr.gaellalire.vestige_app.demo1.action.ActionExecutor;
import fr.gaellalire.vestige_app.demo1.action.ActionHelper;
import fr.gaellalire.vestige_app.demo1.action.CompositeAction;
import fr.gaellalire.vestige_app.demo1.action.CompositeActionHelper;
import fr.gaellalire.vestige_app.demo1.action.Condition;
import fr.gaellalire.vestige_app.demo1.action.ContextAccessor;
import fr.gaellalire.vestige_app.demo1.action.ContextAllocator;
import fr.gaellalire.vestige_app.demo1.action.ExecuteContext;
import fr.gaellalire.vestige_app.demo1.action.Promise;
import fr.gaellalire.vestige_app.demo1.action.SimpleAction;
import fr.gaellalire.vestige_app.demo1.action.linked_list.LinkedList;
import fr.gaellalire.vestige_app.demo1.action.linked_list.LinkedListItem;
import fr.gaellalire.vestige_app.demo1.action.unparker.NotificationHandler;

public class SelfThreadActionExecutor implements ActionExecutor {

    static class RunContext<Context extends ActionContext> {

        LinkedList<STExecutable<?>> nextsST;

        LinkedList<AbstractPromise<? super Context>> nexts = new LinkedList<AbstractPromise<? super Context>>();

        ActionHelper actionHelper;

        Context context;

        public RunContext(final ActionHelper actionHelper, final LinkedList<STExecutable<?>> nextsST, final Context context, final AbstractPromise<? super Context> firstPromise) {
            this.actionHelper = actionHelper;
            this.nextsST = nextsST;
            this.context = context;
            firstPromise.queued = true;
            nexts.pushLast(firstPromise);
        }

        public Context getContext() {
            return context;
        }

        public ActionHelper getActionHelper() {
            return actionHelper;
        }

        void addST(final STExecutable<?> abstractPromise) {
            nextsST.pushFirst(abstractPromise);
        }

        void addPromise(final AbstractPromise<? super Context> abstractPromise) {
            if (!abstractPromise.queued) {
                nexts.pushFirst(abstractPromise);
                abstractPromise.queued = true;
            }
        }

    }

    static class PromiseLink<Context extends ActionContext> extends LinkedListItem<PromiseLink<Context>> {

        Condition<? super Context> condition;

        AbstractPromise<? super Context> promise;

        public PromiseLink(final Condition<? super Context> condition, final Promise<? super Context> promise) {
            this.condition = condition;
            this.promise = (AbstractPromise<? super Context>) promise;
        }

        @Override
        public PromiseLink<Context> getThis() {
            return this;
        }

    }

    static abstract class AbstractPromise<Context extends ActionContext> extends LinkedListItem<AbstractPromise<Context>> implements Promise<Context> {

        @Override
        public AbstractPromise<Context> getThis() {
            return this;
        }

        private boolean pending;

        private boolean queued;

        LinkedList<PromiseLink<Context>> thenAbstractPromises = new LinkedList<PromiseLink<Context>>();

        AbstractPromise<Context> duplicating;

        public void then(final Promise<? super Context> promise) {
            thenAbstractPromises.pushLast(new PromiseLink<Context>(null, promise));
        }

        public void conditionalThen(final Condition<? super Context> condition, final Promise<? super Context> promise) {
            thenAbstractPromises.pushLast(new PromiseLink<Context>(condition, promise));
        }

        public boolean run(final RunContext<? extends Context> runContext) {
            pending = true;
            if (innerRun(runContext)) {
                pending = false;
                PromiseLink<Context> promiseLink = thenAbstractPromises.getFirst();
                while (promiseLink != null) {
                    if (!promiseLink.promise.pending && (promiseLink.condition == null || promiseLink.condition.test(runContext.getContext()))) {
                        AbstractPromise<? super Context> promise = promiseLink.promise;
                        runContext.addPromise(promise);
                    }
                    promiseLink = promiseLink.next;
                }
                return true;
            }
            return false;
        }

        public abstract boolean innerRun(RunContext<? extends Context> context);

        public abstract AbstractPromise<Context> innerDuplicate();

        public AbstractPromise<Context> duplicate() {
            if (duplicating != null) {
                return duplicating;
            }
            duplicating = innerDuplicate();
            PromiseLink<Context> promiseLink = thenAbstractPromises.getFirst();
            while (promiseLink != null) {
                duplicating.thenAbstractPromises.pushLast(new PromiseLink<Context>(promiseLink.condition, promiseLink.promise.duplicate()));
                promiseLink = promiseLink.next;
            }
            AbstractPromise<Context> duplicated = duplicating;
            duplicating = null;
            return duplicated;
        }

    }

    static class STExecutable<Context extends ActionContext> extends LinkedListItem<STExecutable<Context>> {

        LinkedList<AbstractPromise<? super Context>> delayed = new LinkedList<AbstractPromise<? super Context>>();

        RunContext<Context> context;

        public STExecutable(final ActionHelper actionHelper, final LinkedList<STExecutable<?>> nextsST, final Context context, final AbstractPromise<? super Context> firstPromise) {
            this.context = new RunContext<Context>(actionHelper, nextsST, context, firstPromise);
        }

        @Override
        public STExecutable<Context> getThis() {
            return this;
        }

        public void run(final LinkedList<STExecutable<?>> nexts, final LinkedList<STExecutable<?>> delayed) {
            AbstractPromise<? super Context> firstAP = context.nexts.getFirst();
            if (firstAP == null) {
                nexts.remove(this);
                if (!this.delayed.isEmpty()) {
                    context.nexts.pushLast(this.delayed);
                    this.delayed.clear();
                    delayed.pushLast(this);
                }
                return;
            }
            context.nexts.remove(firstAP);
            firstAP.queued = false;
            if (!firstAP.run(context)) {
                this.delayed.pushLast(firstAP);
            }
        }

    }

    static class NoopPromise<Context extends ActionContext> extends AbstractPromise<Context> {

        public NoopPromise() {
        }

        @Override
        public boolean innerRun(final RunContext<? extends Context> context) {
            return true;
        }

        @Override
        public AbstractPromise<Context> innerDuplicate() {
            return new NoopPromise<Context>();
        }

    }

    static class STSubContextActionPromise<SubContext extends ActionContext, Context extends ActionContext> extends AbstractPromise<Context> {

        ContextAllocator<SubContext, ? super Context> allocator;

        NoopPromise<SubContext> subContextPromise;

        List<AbstractPromise<? super SubContext>> currents = new ArrayList<AbstractPromise<? super SubContext>>();

        public STSubContextActionPromise(final ContextAllocator<SubContext, ? super Context> allocator, final NoopPromise<SubContext> subContextPromise) {
            this.allocator = allocator;
            this.subContextPromise = subContextPromise;
        }

        @Override
        public boolean innerRun(final RunContext<? extends Context> context) {
            context.addST(new STExecutable<SubContext>(context.getActionHelper(), context.nextsST, allocator.allocate(context.getContext()), subContextPromise.duplicate()));
            return true;
        }

        @Override
        public AbstractPromise<Context> innerDuplicate() {
            // subContextPromise should be duplicated because the duplicate
            // function is not thread safe, however in single thread executor it
            // is not an issue
            return new STSubContextActionPromise<SubContext, Context>(allocator, subContextPromise);
        }

    }

    static class SimpleActionPromise<Context extends ActionContext> extends AbstractPromise<Context> {

        SimpleAction<? super Context> action;

        public SimpleActionPromise(final SimpleAction<? super Context> action) {
            this.action = action;
        }

        @Override
        public boolean innerRun(final RunContext<? extends Context> context) {
            action.perform(context.getContext());
            return true;
        }

        @Override
        public String toString() {
            return action.toString();
        }

        @Override
        public AbstractPromise<Context> innerDuplicate() {
            return new SimpleActionPromise<Context>(action);
        }

    }

    static class ActionAccessorPromise<Context extends ActionContext, Type> extends AbstractPromise<Context> {

        ContextAccessor<? super Context, Type> accessor;

        Action<? super Type> action;

        public ActionAccessorPromise(final ContextAccessor<? super Context, Type> accessor, final Action<? super Type> action) {
            this.accessor = accessor;
            this.action = action;
        }

        @Override
        public boolean innerRun(final RunContext<? extends Context> context) {
            switch (action.perform(context.getActionHelper(), accessor.access(context.getContext()))) {
            case Action.PENDING:
                return false;
            default:
                return true;
            }
        }

        @Override
        public String toString() {
            return action.toString();
        }

        @Override
        public AbstractPromise<Context> innerDuplicate() {
            return new ActionAccessorPromise<Context, Type>(accessor, action);
        }

    }

    static class ActionPromise<Context extends ActionContext> extends AbstractPromise<Context> {

        Action<? super Context> action;

        public ActionPromise(final Action<? super Context> action) {
            this.action = action;
        }

        @Override
        public boolean innerRun(final RunContext<? extends Context> context) {
            switch (action.perform(context.getActionHelper(), context.getContext())) {
            case Action.PENDING:
                return false;
            default:
                return true;
            }
        }

        @Override
        public String toString() {
            return action.toString();
        }

        @Override
        public AbstractPromise<Context> innerDuplicate() {
            return new ActionPromise<Context>(action);
        }

    }

    public static <Context extends ActionContext> CompositeActionHelper<Context> createCompositeActionHelper(final Promise<Context> firstPromise) {
        return new CompositeActionHelper<Context>() {

            public Promise<Context> getFirstPromise() {
                return firstPromise;
            }

            public Promise<Context> simpleAction(final SimpleAction<? super Context> simpleAction) {
                return new SimpleActionPromise<Context>(simpleAction);
            }

            public Promise<Context> action(final Action<? super Context> action) {
                return new ActionPromise<Context>(action);
            }

            public <Type> Promise<Context> action(final ContextAccessor<? super Context, Type> accessor, final Action<? super Type> action) {
                return new ActionAccessorPromise<Context, Type>(accessor, action);
            }

            public <SubContext extends ActionContext> Promise<Context> fork(final ContextAllocator<SubContext, ? super Context> allocator,
                    final CompositeAction<SubContext> compositeAction) {
                NoopPromise<SubContext> subContextPromise = new NoopPromise<SubContext>();
                compositeAction.describe(createCompositeActionHelper(subContextPromise));
                return new STSubContextActionPromise<SubContext, Context>(allocator, subContextPromise);
            }
        };
    }

    public <Context extends ActionContext> ExecuteContext<Context> createExecuteContext(final CompositeAction<Context> compositeAction) {
        return new ExecuteContext<Context>() {

            private LinkedList<STExecutable<?>> nextsST = new LinkedList<STExecutable<?>>();

            private Object mutex = new Object();

            private boolean shouldNotWait = false;

            // 0: initial
            // 1: initial + pause asked
            // 2: running
            // 3: running + paused asked
            private int state;

            private boolean paused;

            private ActionHelper actionHelper;

            private NoopPromise<Context> firstPromise;

            {
                firstPromise = new NoopPromise<Context>();
                CompositeActionHelper<Context> compositeActionHelper = createCompositeActionHelper(firstPromise);
                compositeAction.describe(compositeActionHelper);
                actionHelper = new ActionHelper() {

                    public void setReadyTime(final long nanoDeadline) {

                    }

                    public void setReadyDelay(final long nanoTime) {

                    }

                    public NotificationHandler getReadyNotificationHandler() {
                        return new NotificationHandler() {

                            public void handle() {
                                synchronized (mutex) {
                                    shouldNotWait = true;
                                    mutex.notifyAll();
                                }
                            }
                        };
                    }
                };
            }

            public void pauseAndWait() throws InterruptedException {
                synchronized (mutex) {
                    if (state == 0 || state == 2) {
                        state++;
                    }
                    mutex.notify();
                    try {
                        do {
                            mutex.wait();
                        } while (state % 2 == 1);
                    } catch (InterruptedException e) {
                        if (state % 2 == 1) {
                            // not paused, cancel it
                            state--;
                            throw e;
                        } else {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            }

            public boolean isPaused() {
                return paused;
            }

            public void execute(final Context context) {
                STExecutable<?> current = nextsST.popLast();
                while (current != null) {
                    current.context.context.close();
                    current = nextsST.popLast();
                }
                nextsST.pushFirst(new STExecutable<Context>(actionHelper, nextsST, context, firstPromise.duplicate()));
                innerRun();
            }

            public void resume() {
                STExecutable<?> current = nextsST.getFirst();
                while (current != null) {
                    current.context.context.resume();
                    current = current.next;
                }
                innerRun();
            }

            public void innerRun() {
                paused = false;
                synchronized (mutex) {
                    if (state == 1) {
                        state = 0;
                        return;
                    }
                    state = 2;
                }

                LinkedList<STExecutable<?>> delayed = new LinkedList<STExecutable<?>>();
                while (true) {
                    STExecutable<?> first = nextsST.getFirst();
                    while (first != null) {
                        first.run(nextsST, delayed);
                        first = nextsST.getFirst();
                    }
                    if (delayed.isEmpty()) {
                        break;
                    }
                    nextsST.pushLast(delayed);
                    delayed.clear();

                    synchronized (mutex) {
                        if (state == 3) {
                            state = 0;
                            paused = true;
                            break;
                        }
                        if (shouldNotWait) {
                            shouldNotWait = false;
                        } else {
                            try {
                                mutex.wait();
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }
                if (paused) {
                    STExecutable<?> current = nextsST.getLast();
                    while (current != null) {
                        current.context.context.pause();
                        current = current.previous;
                    }
                } else {
                    STExecutable<?> current = nextsST.popLast();
                    while (current != null) {
                        current.context.context.close();
                        current = nextsST.popLast();
                    }
                }
                synchronized (mutex) {
                    state = 0;
                    mutex.notifyAll();
                }
            }

        };

    }

}
