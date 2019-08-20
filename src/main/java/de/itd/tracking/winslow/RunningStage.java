package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

public interface RunningStage {

    default Iterable<String> getStdOut() {
        return getStdOut(Integer.MAX_VALUE);
    }

    Iterable<String> getStdOut(int lastNLines);

    default Iterable<String> getStdErr() {
        return getStdErr(Integer.MAX_VALUE);
    }

    Iterable<String> getStdErr(int lastNLines);
}
