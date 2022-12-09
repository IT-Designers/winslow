package de.itdesigners.winslow;

import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ElectionManager {

    private static final Logger LOG = Logger.getLogger(ElectionManager.class.getSimpleName());

    public static final Locale DECIMAL_LOCALE    = Locale.US;
    public static final String PROPERTY_PROJECT  = "project";
    public static final String PROPERTY_AFFINITY = "affinity";
    public static final String PROPERTY_AVERSION = "aversion";

    private final @Nonnull LockBus               lockBus;
    private final @Nonnull Map<String, Election> elections = new ConcurrentHashMap<>();

    private final @Nonnull List<Consumer<Election>> electionStartedListeners = new ArrayList<>();
    private final @Nonnull List<Consumer<Election>> electionClosedListeners  = new ArrayList<>();


    public ElectionManager(@Nonnull LockBus lockBus) {
        this.lockBus = lockBus;
    }

    public synchronized void registerOnElectionStarted(@Nonnull Consumer<Election> listener) {
        this.electionStartedListeners.add(listener);
    }

    public synchronized void registerOnElectionClosed(@Nonnull Consumer<Election> listener) {
        this.electionClosedListeners.add(listener);
    }

    private void checkForOutdatedElection(@Nonnull String projectId) {
        var election = this.elections.get(projectId);
        if (election != null && !election.isStillRunning()) {
            election = this.elections.remove(projectId);
            if (election != null) {
                LOG.info("Removing election that is no longer running. Project " + election.getProjectId());
                notifyAll(electionClosedListeners, election);
            }
        }
    }

    @Nonnull
    public Optional<Election> getElection(@Nonnull String projectId) {
        checkForOutdatedElection(projectId);
        return Optional.ofNullable(this.elections.get(projectId));
    }

    public synchronized boolean maybeStartElection(
            @Nonnull String projectId,
            long duration) throws LockException, IOException {
        checkForOutdatedElection(projectId);
        if (this.elections.isEmpty()) {
            //if (!this.elections.containsKey(projectId)) {
            var properties = new Properties();
            properties.setProperty(PROPERTY_PROJECT, projectId);
            this.lockBus.publishCommand(Event.Command.ELECTION_START, toSubjectLine(properties), duration);
            return true;
        } else {
            return false;
        }
    }

    public void closeElection(@Nonnull String projectId) throws LockException, IOException {
        if (this.elections.containsKey(projectId)) {
            var properties = new Properties();
            properties.setProperty(PROPERTY_PROJECT, projectId);
            this.lockBus.publishCommand(Event.Command.ELECTION_STOP, toSubjectLine(properties));
        }
    }

    public void participate(
            @Nonnull Election election,
            @Nonnull Election.Participation participation) throws IOException, LockException {
        var properties = new Properties();
        properties.setProperty(PROPERTY_PROJECT, election.getProjectId());
        properties.setProperty(PROPERTY_AFFINITY, String.format(DECIMAL_LOCALE, "%f", participation.affinity));
        properties.setProperty(PROPERTY_AVERSION, String.format(DECIMAL_LOCALE, "%f", participation.aversion));

        lockBus.publishCommand(Event.Command.ELECTION_PARTICIPATION, toSubjectLine(properties));
    }

    public synchronized void onElectionStarted(
            @Nonnull String subject,
            @Nonnull String issuer,
            long time,
            long duration) throws IOException {
        var properties = fromSubjectLine(subject);
        var projectId  = properties.getProperty(PROPERTY_PROJECT);
        if (projectId != null) {
            var election = new Election(issuer, projectId, time, duration);
            this.elections.put(election.getProjectId(), election);
            notifyAll(electionStartedListeners, election);
        }
    }

    public void onNewElectionParticipant(@Nonnull String subject, @Nonnull String issuer) throws IOException {
        var properties = fromSubjectLine(subject);
        var project    = Optional.ofNullable(properties.getProperty(PROPERTY_PROJECT));
        var election   = project.map(this.elections::get).orElse(null);
        if (election != null) {
            Locale.setDefault(DECIMAL_LOCALE);
            var participation = new Election.Participation(
                    Float.parseFloat(properties.getProperty(PROPERTY_AFFINITY)),
                    Float.parseFloat(properties.getProperty(PROPERTY_AVERSION))
            );
            election.onNewParticipant(issuer, participation);
        }
    }

    public synchronized void onElectionClosed(@Nonnull String subject) throws IOException {
        var properties = fromSubjectLine(subject);
        var projectId  = properties.getProperty(PROPERTY_PROJECT);
        var election   = this.elections.remove(projectId);
        if (election != null) {
            notifyAll(electionClosedListeners, election);
        }
    }

    private static <T> void notifyAll(@Nonnull Iterable<Consumer<T>> listeners, @Nonnull T value) {
        for (var listener : listeners) {
            try {
                listener.accept(value);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Nonnull
    private static String toSubjectLine(@Nonnull Properties properties) throws IOException {
        var writer = new StringWriter();
        properties.store(writer, null);
        return writer.getBuffer().toString().lines().skip(1).collect(Collectors.joining(" "));
    }

    @Nonnull
    private static Properties fromSubjectLine(@Nonnull String subject) throws IOException {
        var reader     = new StringReader(subject.replaceAll(" ", "\n"));
        var properties = new Properties();
        properties.load(reader);
        return properties;
    }
}
