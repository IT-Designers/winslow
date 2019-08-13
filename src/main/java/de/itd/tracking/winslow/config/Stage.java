package de.itd.tracking.winslow.config;

import java.util.Objects;
import java.util.Optional;

public class Stage {
    private final String       name;
    private final String       desc;
    private final Image        image;
    private final Requirements requires;
    private final UserInput    userInput;

    public Stage(String name, String desc, Image image, Requirements requires, UserInput userInput) {
        this.name = name;
        this.desc = desc;
        this.image = image;
        this.requires = requires;
        this.userInput = userInput;
        this.check();
    }

    public void check() {
        Objects.requireNonNull(name, "The name of a stage must be set");
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(desc);
    }

    public Optional<Image> getImage() {
        return Optional.ofNullable(image);
    }

    public Optional<Requirements> getRequirements() {
        return Optional.ofNullable(requires);
    }

    public Optional<UserInput> getUserInput() {
        return Optional.ofNullable(userInput);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "',desc='" + this.desc + "',image=" + this.image + ",userInput=" + this.userInput + "}#" + this.hashCode();
    }
}
