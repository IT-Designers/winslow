package de.itdesigners.winslow.config;

public enum StageType {
    Execution,
    AndGateway,
    XOrGateway;

    public boolean isGateway() {
        switch (this) {
            case AndGateway:
            case XOrGateway:
                return true;
            case Execution:
        }
        return false;
    }
}
