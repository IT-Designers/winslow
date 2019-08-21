package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.StreamFrame;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadException;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
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

    private boolean taskHasEnded() {
        return state.get().map(this::hasEnded).orElse(false);
    }

    private boolean hasEnded(AllocationListStub allocation) {
        return allocation.getTaskStates().get(taskName).getFinishedAt().after(new Date(1));
    }

    private boolean taskHasStarted() {
        return state.get().map(this::hasStarted).orElse(false);
    }

    private boolean hasStarted(AllocationListStub allocation) {
        return allocation.getTaskStates().get(taskName).getStartedAt().after(new Date(1));
    }

    private boolean tryEnsureStream() {
        if (stream == null) {
            return state.get().map(allocation -> {
                if (hasStarted(allocation)) {
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
        return (stream == null || !taskHasEnded())
                && !(taskHasEnded() && !taskHasStarted());
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to read next StreamFrame for id=" + this.id + ",taskName=" + this.taskName, e);
        }
        return "";
    }
}
