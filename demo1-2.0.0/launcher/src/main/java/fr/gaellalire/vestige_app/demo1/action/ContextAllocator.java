package fr.gaellalire.vestige_app.demo1.action;

public interface ContextAllocator<SubContext, Context> {

    SubContext allocate(Context context);
}
