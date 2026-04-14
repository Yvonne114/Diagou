package com.diagou.backend.exception;

import lombok.Getter;

@Getter
public class InvalidStateTransitionException extends RuntimeException {

    private final String currentState;
    private final String targetState;

    public InvalidStateTransitionException(String currentState, String targetState) {
        super("Invalid state transition from " + currentState + " to " + targetState);
        this.currentState = currentState;
        this.targetState = targetState;
    }
}
