package de.itdesigners.winslow;

import de.itdesigners.winslow.api.auth.Role;
import de.itdesigners.winslow.auth.*;
import de.itdesigners.winslow.cli.FixWorkspacePaths;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.node.NodeRepository;
import de.itdesigners.winslow.project.AuthTokenRepository;
import de.itdesigners.winslow.project.LogRepository;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Winslow implements Runnable {

    private static final Logger LOG = Logger.getLogger(Winslow.class.getSimpleName());

    @Nonnull private final Orchestrator                 orchestrator;
    @Nonnull private final WorkDirectoryConfiguration   configuration;
    @Nonnull private final ResourceManager              resourceManager;
    @Nonnull private final GroupManager                 groupManager;
    @Nonnull private final UserManager                  userManager;
    @Nonnull private final PipelineDefinitionRepository pipelineRepository;
    @Nonnull private final ProjectRepository            projectRepository;
    @Nonnull private final AuthTokenRepository          projectAuthTokenRepository;
    @Nonnull private final NodeRepository               nodeRepository;
    @Nonnull private final SettingsRepository           settingsRepository;

    public Winslow(
            @Nonnull Orchestrator orchestrator,
            @Nonnull WorkDirectoryConfiguration configuration,
            @Nonnull LockBus lockBus,
            @Nonnull ResourceManager resourceManager,
            @Nonnull ProjectRepository projectRepository,
            @Nonnull AuthTokenRepository projectAuthTokenRepository,
            @Nonnull SettingsRepository settingsRepository,
            @Nonnull NodeRepository nodeRepository,
            @Nonnull GroupManager groupManager,
            @Nonnull UserManager userManager) throws IOException {
        this.orchestrator               = orchestrator;
        this.configuration              = configuration;
        this.resourceManager            = resourceManager;
        this.groupManager               = groupManager;
        this.userManager                = userManager;
        this.projectAuthTokenRepository = projectAuthTokenRepository;

        this.pipelineRepository = new PipelineDefinitionRepository(lockBus, configuration);
        this.projectRepository  = projectRepository;
        this.settingsRepository = settingsRepository;
        this.nodeRepository     = nodeRepository;

        for (var userName : Env.getRootUsers()) {
            try {
                userManager.createUserAndGroupIgnoreIfAlreadyExists(userName);
                groupManager.addOrUpdateMembership(Group.SUPER_GROUP_NAME, userName, Role.OWNER);
            } catch (InvalidNameException | NameNotFoundException e) {
                // there is nothing reasonable recovery from this, fail fast, don't hide the error!
                throw new RuntimeException(e);
            }
        }
    }

    public void run() {
        LOG.info("Starting the orchestrator");
        orchestrator.start();

        LOG.info("Serving interactive console");
        var commands = new TreeMap<String, BiConsumer<ConsoleHandle, Arguments>>();
        commands.put("exit", ConsoleHandle::stop);
        commands.put("stop", ConsoleHandle::stop);
        commands.put("reload", this::reload);
        commands.put("fix-workspace-paths", this::fixWorkspacePaths);
        commands.put("prune-lost-workspaces", this::pruneLostWorkspaces);
        commands.put("passwd", this::passwd);
        commands.put("adduser", this::addUser);
        commands.put("lsuser", this::lsUser);
        commands.put("moduser", this::modUser);

        try (Scanner scanner = new Scanner(System.in)) {
            var handle = new ConsoleHandle(scanner);

            String line;
            while ((line = handle.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                var command = line.trim();
                var argLine = (String) null;
                var split   = (String[]) null;

                var firstSpace = command.indexOf(' ');
                if (firstSpace > 0) {
                    argLine = command.substring(firstSpace).trim();
                    command = command.substring(0, firstSpace);
                    split   = argLine.split(" ");
                }

                if (commands.containsKey(command)) {
                    commands.get(command).accept(handle, new Arguments(command, argLine, split));
                } else if (!line.isEmpty()) {
                    System.out.println("Unknown command, valid commands are: ");
                    commands.keySet().forEach(k -> System.out.println(" - " + k));
                }
            }
        }
    }

    private void reload(@Nonnull ConsoleHandle _console, @Nonnull Arguments _args) {
        getProjectRepository()
                .getProjects()
                .map(BaseRepository.Handle::unsafe)
                .flatMap(Optional::stream)
                .filter(project -> orchestrator.getPipeline(project).isEmpty())
                .forEach(project -> {
                    try {
                        orchestrator.createPipeline(project);
                        System.out.println(" - recreated pipeline for " + project.getId());
                    } catch (OrchestratorException e) {
                        System.out.println(" - failed for " + project.getId() + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                });
    }

    private void pruneLostWorkspaces(@Nonnull ConsoleHandle console, @Nonnull Arguments _args) {
        getOrchestrator()
                .getPipelines()
                .getAllPipelines()
                .flatMap(h -> h.unsafe().stream())
                .forEach(pipeline -> {
                    resourceManager.getWorkspace(Path.of(pipeline.getProjectId())).ifPresent(workspace -> {
                        var directories = new TreeMap<String, Path>();
                        try (var list = Files.list(workspace)) {
                            list
                                    .map(p -> p.getFileName().toString())
                                    .filter(p -> p.contains("_"))
                                    .filter(p -> p.split("_", 2)[0].matches("[\\d]+"))
                                    .map(workspace::resolve)
                                    .forEach(w -> resourceManager
                                            .getWorkspacesDirectory()
                                            .map(d -> d.relativize(w))
                                            .ifPresent(relative -> directories.put(relative.toString(), w))
                                    );
                            pipeline
                                    .getActiveAndPastExecutionGroups()
                                    .flatMap(ExecutionGroup::getStages)
                                    .flatMap(s -> s.getWorkspace().stream())
                                    .forEach(directories::remove);
                            directories.keySet().forEach(System.out::println);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!directories.isEmpty()) {

                            System.out.println(">> Delete? [y/N] ");
                            var result = console.readLine();

                            if ("y".equals(result)) {
                                directories.values().forEach(directory -> {
                                    try {
                                        orchestrator.forcePurgeWorkspace(
                                                pipeline.getProjectId(),
                                                directory
                                        );
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                System.out.println("Aborted!");
                            }
                        }
                    });
                });
        System.out.println("Done.");
    }

    private void fixWorkspacePaths(@Nonnull ConsoleHandle _console, @Nonnull Arguments _args) {
        new FixWorkspacePaths()
                .withProjectIds(this.projectRepository.getProjectIds().collect(Collectors.toList()))
                .searchForProjectsWithFixableWorkspaces(projectRepository, orchestrator)
                .tryFixProjectsWithFixableWorkspacePaths(orchestrator)
                .printResults(System.out);
    }

    /**
     * Updates the password of a {@link User}.
     * Accepts the username as first argument, otherwise it will read it interactively from the {@link ConsoleHandle}.
     *
     * @param console {@link ConsoleHandle} to use for reading the user password
     * @param args    {@link Arguments}, optionally accepts the username here.
     */
    private void passwd(@Nonnull ConsoleHandle console, @Nonnull Arguments args) {
        var username = args.argLine;
        if (username == null) {
            System.out.print("       Username: ");
            username = console.readLine();
        }

        var pw1 = console.readPassword("       Password: ");
        var pw2 = console.readPassword("Repeat password: ");


        if (pw1 == null || pw2 == null) {
            System.out.println("ERROR: Failed to access system console for reading passwords, operation aborted.");
        } else if (!Arrays.equals(pw1, pw2)) {
            System.out.println("ERROR: Passwords do not match");
        } else if (username == null) {
            System.out.println("ERROR: Failed to read username");
        } else {
            try {
                this.userManager.setUserPassword(username, pw1.length > 0 ? pw1 : null);
                System.out.println("Password has been updated");
            } catch (IOException e) {
                System.out.println("ERROR: io-error: " + e.getMessage());
            } catch (InvalidNameException e) {
                System.out.println("ERROR: Invalid username");
            } catch (NameNotFoundException e) {
                System.out.println("ERROR: User not found");
            } catch (InvalidPasswordException e) {
                System.out.println("ERROR: Invalid password: " + e.getMessage());
            }

        }
    }

    /**
     * Adds a new {@link User}.
     * Accepts the username as first argument, otherwise it will read it interactively from the {@link ConsoleHandle}.
     * Further user details are always read interactively from the {@link ConsoleHandle}
     *
     * @param console {@link ConsoleHandle} to use for IO
     * @param args    {@link Arguments}, optionally accepts the username here.
     */
    private void addUser(@Nonnull ConsoleHandle console, @Nonnull Arguments args) {
        var username = args.argLine;
        if (username == null) {
            System.out.print("         Username: ");
            username = console.readLine();
        }

        System.out.print("Display Name   []: ");
        var displayName = console.readLine();
        if (displayName != null && displayName.trim().isEmpty()) {
            displayName = null;
        }

        System.out.print("E-Mail Address []: ");
        var email = console.readLine();
        if (email != null && email.trim().isEmpty()) {
            email = null;
        }

        var pw1 = console.readPassword("         Password: ");
        var pw2 = console.readPassword("  Repeat password: ");

        if (pw1 == null || pw2 == null) {
            System.out.println("ERROR: Failed to access system console for reading passwords, operation aborted.");
        } else if (!Arrays.equals(pw1, pw2)) {
            System.out.println("ERROR: Passwords do not match");
        } else if (username == null) {
            System.out.println("ERROR: Missing user name");
        } else {
            try {
                this.userManager.createUserAndGroup(username, displayName, email, pw1);
                System.out.println("User has been created successfully");
            } catch (InvalidNameException e) {
                System.out.println("ERROR: Invalid user name");
            } catch (InvalidPasswordException e) {
                System.out.println("ERROR: Invalid password: " + e.getMessage());
            } catch (NameAlreadyInUseException e) {
                System.out.println("ERROR: Username is already in use");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Lists a table with all {@link User}s that are returend by {@link UserManager#getUsersPotentiallyIncomplete()}.
     *
     * @param console {@link ConsoleHandle} to use for IO
     * @param _args   ignored
     */
    private void lsUser(@Nonnull ConsoleHandle console, @Nonnull Arguments _args) {
        var header = new String[]{
                "Active", "Name", "Display Name", "E-Mail", "Password", "Privileges", "Groups"
        };
        var columnWidths = new int[header.length];
        var contents     = new ArrayList<String[]>();
        contents.add(header);

        this.userManager.getUsersPotentiallyIncomplete().forEach(user -> {
            contents.add(new String[]{
                    user.active() ? "yes" : "no",
                    user.name(),
                    user.displayName() != null ? user.displayName() : "",
                    user.email() != null ? user.email() : "",
                    user.password() != null ? "*" : "",
                    user.hasSuperPrivileges() ? "super" : "user",
                    user.getGroups().stream().map(Group::name).collect(Collectors.joining(", "))
            });
        });

        for (var content : contents) {
            for (int i = 0; i < content.length; ++i) {
                if (columnWidths[i] < content[i].length()) {
                    columnWidths[i] = content[i].length();
                }
            }
        }

        int currentRow = 0;
        System.out.println();
        for (var content : contents) {
            if (currentRow == 1) {
                for (int i = 0; i < columnWidths.length; ++i) {
                    if (i != 0) {
                        System.out.print("--+--");
                    }
                    System.out.print("-".repeat(columnWidths[i]));
                }
                System.out.println();
            }

            for (int i = 0; i < content.length; ++i) {
                if (i != 0) {
                    System.out.print("  |  ");
                }
                System.out.printf("%" + columnWidths[i] + "s", content[i]);
            }
            System.out.println();
            ++currentRow;
        }
        System.out.println();
    }

    private void modUser(@Nonnull ConsoleHandle console, @Nonnull Arguments args) {
        if (args.args == null || args.args.length < 2) {
            System.out.println("ERROR: Expected at least two arguments: <action> <user>");
        } else {
            var action   = args.args[0];
            var username = args.args[1];

            try {
                var user = this.userManager.getUser(username).orElseThrow(() -> new NameNotFoundException(username));

                switch (action) {
                    case "activate", "deactivate" -> this.userManager.updateUser(
                            user.name(),
                            user.displayName(),
                            user.email(),
                            Objects.equals("activate", action)
                    );
                    default -> System.out.println("ERROR: Unexpected action: '" + action + "'");
                }
            } catch (InvalidNameException e) {
                System.out.println("ERROR: Invalid username");
            } catch (NameNotFoundException e) {
                System.out.println("ERROR: User not found");
            } catch (IOException e) {
                System.out.println("ERROR: io-error: " + e.getMessage());
            }
        }
    }

    @Nonnull
    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    @Nonnull
    public WorkDirectoryConfiguration getWorkDirectoryConfiguration() {
        return configuration;
    }

    @Nonnull
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    @Nonnull
    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }


    @Nonnull
    public AuthTokenRepository getProjectAuthTokenRepository() {
        return projectAuthTokenRepository;
    }

    @Nonnull
    public NodeRepository getNodeRepository() {
        return nodeRepository;
    }

    @Nonnull
    public RunInfoRepository getRunInfoRepository() {
        return getOrchestrator().getRunInfoRepository();
    }

    @Nonnull
    public LogRepository getLogRepository() {
        return getOrchestrator().getLogRepository();
    }

    @Nonnull
    public UserManager getUserManager() {
        return this.userManager;
    }

    @Nonnull
    public GroupManager getGroupManager() {
        return groupManager;
    }

    @Nonnull
    public PipelineDefinitionRepository getPipelineRepository() {
        return pipelineRepository;
    }

    @Nonnull
    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    private record Arguments(
            @Nonnull String command,
            @Nullable String argLine,
            @Nullable String[] args
    ) {
    }

    private static class ConsoleHandle {
        private final @Nonnull Scanner scanner;
        private                boolean keepRunning = true;

        private ConsoleHandle(@Nonnull Scanner scanner) {
            this.scanner = scanner;
        }

        @Nullable
        public String readLine() {
            if (keepRunning) {
                return this.scanner.nextLine();
            } else {
                return null;
            }
        }

        @Nullable
        public char[] readPassword(@Nonnull String fmt, Object... args) {
            var console = keepRunning ? System.console() : null;
            if (console != null) {
                return console.readPassword(fmt, args);
            } else if (Env.isDevEnv()) {
                System.out.println("Console not available, dev-env allows cleartext password input");
                System.out.printf(fmt, args);
                var line = readLine();
                if (line != null) {
                    return line.toCharArray();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        public void stop(@Nonnull Arguments _args) {
            this.keepRunning = false;
        }
    }
}
