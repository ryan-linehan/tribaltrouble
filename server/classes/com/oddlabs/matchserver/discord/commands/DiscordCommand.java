package com.oddlabs.matchserver.discord.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandRequest;

public abstract class DiscordCommand {

    public abstract String getCommandName();

    public abstract InteractionApplicationCommandCallbackReplyMono executeCommand(
            ChatInputInteractionEvent event);

    public abstract ApplicationCommandRequest getCommand();
}
