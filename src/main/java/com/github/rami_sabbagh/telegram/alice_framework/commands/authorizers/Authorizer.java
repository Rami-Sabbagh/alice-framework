package com.github.rami_sabbagh.telegram.alice_framework.commands.authorizers;

import com.github.rami_sabbagh.telegram.alice_framework.commands.Command;
import com.github.rami_sabbagh.telegram.alice_framework.commands.ParsedCommand;

/**
 * Determines if a user is allowed to have his command executed or not. (functional interface).
 */
public interface Authorizer {
    /**
     * Determines if a user is allowed to have his command executed or not.
     *
     * @param parsedCommand The parsed command request by the user.
     * @param command       The command implementation to execute.
     * @return {@code null} if the request was accepted, otherwise the reason why it was rejected.
     */
    String authorize(ParsedCommand parsedCommand, Command command);
}
