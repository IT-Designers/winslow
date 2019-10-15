package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.util.Iterator;

public class CombinedIterator<T> implements Iterator<T> {

    @Nonnull private final Iterator<T>[] iterators;

    public CombinedIterator(@Nonnull Iterator<T>...iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        for (Iterator<T> iter : iterators) {
            if (iter.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T next() {
        T next = null;
        for (Iterator<T> iter : iterators) {
            if (iter.hasNext() && (next = iter.next()) != null) {
                break;
            }
        }
        return next;
    }
}
