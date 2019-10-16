package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.logging.Logger;

public class CombinedIterator<T> implements Iterator<T> {

    private static final Logger LOG = Logger.getLogger(CombinedIterator.class.getSimpleName());

    @Nonnull private final Iterator<T>[] iterators;
    private                int           offset = 0;

    @SafeVarargs
    public CombinedIterator(@Nonnull Iterator<T>... iterators) {
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
        for (int i = 0; i < this.iterators.length; ++i) {
            var iter           = this.iterators[(i + offset) % this.iterators.length];
            var beforeBreak    = System.currentTimeMillis();
            var shallBreak     = iter.hasNext() && (next = iter.next()) != null;
            var checkBreakTook = System.currentTimeMillis() - beforeBreak;

            if (checkBreakTook > 150) {
                LOG.warning("Expected non-blocking behavior, it took " + checkBreakTook + "ms to check for " + iter);
            }

            if (shallBreak) {
                offset = i; // remember last position as start position
                break;
            }
        }
        return next;
    }
}
