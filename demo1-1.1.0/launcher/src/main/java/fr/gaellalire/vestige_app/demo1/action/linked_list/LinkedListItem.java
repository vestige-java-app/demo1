package fr.gaellalire.vestige_app.demo1.action.linked_list;

/**
 * @author gaellalire
 */
public abstract class LinkedListItem<ItemType extends LinkedListItem<ItemType>> {

    public ItemType next;

    public ItemType previous;

    public abstract ItemType getThis();

}
