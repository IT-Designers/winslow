package de.itd.tracking.winslow.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

@ControllerAdvice
public class ExceptionConfig {

    private static final Logger LOG = Logger.getGlobal();

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handle(HttpMessageNotWritableException e) {
        LOG.log(Level.WARNING, "Failed to write message", e);
        throw e;
    }
}
