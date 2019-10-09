package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.apimodel.AllocationListStub;
import com.hashicorp.nomad.javasdk.ClientApi;
import com.hashicorp.nomad.javasdk.NomadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public class LogIterator implements Iterator<String> {

    private final String                                 id;
    private final String                                 taskName;
    private final String                                 logType;
    private final ClientApi                              clientApi;
    private final Supplier<Optional<AllocationListStub>> state;

    private BufferedReader reader   = null;
    private String         nextLine = null;

    public LogIterator(
            String id,
            String taskName,
            String logType,
            ClientApi clientApi,
            Supplier<Optional<AllocationListStub>> state) {
        this.id = id;
        this.taskName = taskName;
        this.logType = logType;
        this.clientApi = clientApi;
        this.state = state;
    }

    private boolean hasTaskStarted(AllocationListStub allocation) {
        return NomadOrchestrator.hasTaskStarted(allocation, taskName).orElse(Boolean.FALSE);
    }

    private boolean tryEnsureStream() {
        if (reader == null) {
            return state.get().map(allocation -> {
                if (hasTaskStarted(allocation)) {
                    try {
                        reader = new BufferedReader(new InputStreamReader(clientApi.logs(allocation.getId(),
                                                                                         taskName,
                                                                                         false,
                                                                                         logType
                                                                                        )));
                        return Boolean.TRUE;
                    } catch (IOException | NomadException e) {
                        e.printStackTrace();
                        return Boolean.FALSE;
                    }
                } else {
                    return Boolean.FALSE;
                }
            }).orElse(Boolean.FALSE);
        } else {
            return true;
        }
    }

    @Override
    public boolean hasNext() {
        if (!tryEnsureStream()) {
            return false;
        }

        if (this.nextLine != null) {
            return true;
        } else {
            try {
                this.nextLine = this.reader.readLine();
                return nextLine != null;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read next line", e);
            }
        }
    }

    @Override
    public String next() {
        if (this.nextLine == null && !this.hasNext()) {
            throw new NoSuchElementException();
        } else {
            var line = this.nextLine;
            this.nextLine = null;
            return line;
        }
    }
}
