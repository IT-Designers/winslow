package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.apimodel.StreamFrame;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.FramedStream;
import com.hashicorp.nomad.javasdk.NomadException;

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
    private Iterator<String> next = null;

    public LogIterator(String id, String taskName, String logType, ClientApi clientApi, Supplier<Optional<AllocationListStub>> state) {
        this.id        = id;
        this.taskName  = taskName;
        this.logType   = logType;
        this.clientApi = clientApi;
        this.state     = state;
        this.tryReadNext();
    }

    private boolean hasTaskStarted(AllocationListStub allocation) {
        return NomadOrchestrator.hasTaskStarted(allocation, taskName).orElse(false);
    }

    private boolean tryEnsureStream() {
        if (stream == null) {
            return state.get().map(allocation -> {
                if (hasTaskStarted(allocation)) {
                    try {
                        stream = clientApi.logsAsFrames(allocation.getId(), taskName, false, logType);
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

    private void tryReadNext() {
        this.next = null;

        if (!this.tryEnsureStream()) {
            return;
        }
        try {
            StreamFrame next = stream.nextFrame();
            if (next != null && next.getData() != null) {
                this.next = new String(next.getData()).lines().iterator();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (!hasNext() && stream != null) {
                stream.close();
                stream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return this.next != null && this.next.hasNext();
    }

    @Override
    public String next() {
        var result = this.next.next();
        if (!this.next.hasNext()) {
            this.tryReadNext();
        }
        return result;
    }
}
