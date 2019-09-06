package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.StreamFrame;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadException;
import de.itd.tracking.winslow.Submission;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

public class LogIterator implements Iterator<String> {

    private final String                                 id;
    private final String                                 taskName;
    private final String                                 logType;
    private final ClientApi                              clientApi;
    private final Supplier<Optional<AllocationListStub>> state;

    private FramedStream stream = null;

    public LogIterator(String id, String taskName, String logType, ClientApi clientApi, Supplier<Optional<AllocationListStub>> state) {
        this.id = id;
        this.taskName = taskName;
        this.logType = logType;
        this.clientApi = clientApi;
        this.state = state;
        this.tryEnsureStream();
    }

    private boolean hasTaskStarted(AllocationListStub allocation) {
        return NomadOrchestrator.hasTaskStarted(allocation, taskName).orElse(false);
    }

    private boolean tryEnsureStream() {
        if (stream == null) {
            return state.get().map(allocation -> {
                if (hasTaskStarted(allocation)) {
                    try {
                        stream = clientApi.logsAsFrames(allocation.getId(), taskName, true, logType);
                        return true;
                    } catch (IOException | NomadException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    return false;
                }
            }).orElse(false);
        } else {
            return true;
        }
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = state.get()
                .flatMap(alloc -> NomadOrchestrator.toRunningStageState(alloc, taskName))
                .map(state -> state == Submission.State.Running || state == Submission.State.Preparing)
                .orElse(false);
        if (!hasNext && stream != null) {
            try {
                this.stream.close();
                this.stream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hasNext;
    }

    @Override
    public String next() {
        if (!this.tryEnsureStream()) {
            return "";
        }
        try {
            StreamFrame next = stream.nextFrame();
            if (next != null && next.getData() != null) {
                return new String(next.getData());
            }
            if (!hasNext() && stream != null) {
                stream.close();
                stream = null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read next StreamFrame for id=" + this.id + ",taskName=" + this.taskName, e);
        }
        return "";
    }
}
