package fr.gaellalire.vestige_app.demo1.action.linked_list;


/**
 * @author gaellalire
 * @param <ItemType> item type
 */
public class LinkedList<ItemType extends LinkedListItem<ItemType>> {

    private ItemType firstItem;

    private ItemType lastItem;

    public ItemType getFirst() {
        return firstItem;
    }

    public ItemType getLast() {
        return lastItem;
    }

    public ItemType popFirst() {
        ItemType itemType = firstItem;
        if (itemType == null) {
            return null;
        }
        firstItem = itemType.next;
        if (firstItem == null) {
            lastItem = null;
        }
        itemType.next = null;
        return itemType;
    }

    public void pushFirst(final ItemType itemType) {
        itemType.next = firstItem;
        if (firstItem == null) {
            lastItem = itemType;
        } else {
            firstItem.previous = itemType;
        }
        firstItem = itemType;
        itemType.previous = null;
    }

    public void remove(final ItemType itemType) {
        if (firstItem == itemType) {
            firstItem = itemType.next;
        } else {
            itemType.previous.next = itemType.next;
        }
        if (lastItem == itemType) {
            lastItem = itemType.previous;
        } else {
            itemType.next.previous = itemType.previous;
        }
        itemType.next = null;
        itemType.previous = null;
    }


    public ItemType popLast() {
        ItemType itemType = lastItem;
        if (itemType == null) {
            return null;
        }
        lastItem = itemType.previous;
        if (lastItem == null) {
            firstItem = null;
        }
        itemType.previous = null;
        return itemType;
    }

    /**
     * Push all linkedList items to this linkedList, then clear linkedList (because items cannot be shared between linkedList).
     */
    public void pushLast(final LinkedList<ItemType> linkedList) {
        ItemType itemType = linkedList.firstItem;
        if (itemType == null) {
            // empty list
            return;
        }
        itemType.previous = lastItem;
        if (lastItem == null) {
            firstItem = itemType;
        } else {
            lastItem.next = itemType;
        }
        lastItem = linkedList.lastItem;
        linkedList.firstItem = null;
        linkedList.lastItem = null;
    }


    public void pushLast(final ItemType itemType) {
        itemType.previous = lastItem;
        if (lastItem == null) {
            firstItem = itemType;
        } else {
            lastItem.next = itemType;
        }
        lastItem = itemType;
        itemType.next = null;
    }

    public boolean isEmpty() {
        return firstItem == null;
    }

    public void clear() {
        firstItem = null;
        lastItem = null;
    }

    public void replace(final ItemType itemType, final ItemType replacementItemType) {
        if (firstItem == itemType) {
            firstItem = replacementItemType;
        } else {
            itemType.previous.next = replacementItemType;
        }
        replacementItemType.next = itemType.next;
        itemType.next = null;
        if (lastItem == itemType) {
            lastItem = replacementItemType;
        } else {
            itemType.next.previous = replacementItemType;
        }
        replacementItemType.previous = itemType.previous;
        itemType.previous = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        ItemType item = null;
        if (firstItem != null) {
            sb.append(firstItem);
            item = firstItem.next;
        }
        while (item != null) {
            sb.append(", ");
            sb.append(item);
            item = item.next;
        }
        sb.append("]");
        return sb.toString();
    }

}
