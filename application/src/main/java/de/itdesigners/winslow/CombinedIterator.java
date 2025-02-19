package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

public class CombinedIterator<T> implements Iterator<T> {

    private static final long   MAX_TIME_PER_ITERATOR_MS = 100;
    private static final Logger LOG                      = Logger.getLogger(CombinedIterator.class.getSimpleName());

    @Nonnull private final Iterator<T>[] iterators;
    private                int           offset        = 0;
    private                long          offsetSinceMs = 0;

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

        // if to long on the current, move to the next
        if (tooLongOnThisIterator()) {
            setOffset(this.offset + 1);
        }

        for (int i = 0; i < this.iterators.length; ++i) {
            var offsetIndex    = (i + offset) % this.iterators.length;
            var iter           = this.iterators[offsetIndex];
            var beforeBreak    = System.currentTimeMillis();
            var shallBreak     = iter.hasNext() && (next = iter.next()) != null;
            var checkBreakTook = System.currentTimeMillis() - beforeBreak;

            if (checkBreakTook > 150) {
                LOG.warning("Expected non-blocking behavior, but it took " + checkBreakTook + "ms to check for " + iter);
            }

            if (shallBreak) {
                setOffset(offsetIndex); // remember last position as start position
                break;
            }
        }
        return next;
    }

    private void setOffset(int offset) {
        this.offset        = offset;
        this.offsetSinceMs = System.currentTimeMillis();
    }

    private boolean tooLongOnThisIterator() {
        return offsetSinceMs != 0 && offsetSinceMs + MAX_TIME_PER_ITERATOR_MS < System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "CombinedIterator@{"
                + "iterators=" + Arrays.toString(iterators) +
                ",offset=" + offset
                + "}#"
                + hashCode();
    }
}
