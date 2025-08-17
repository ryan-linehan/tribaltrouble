package com.oddlabs.matchserver.discord;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.Disposable;

import com.oddlabs.matchserver.ChatRoom;

public class DiscordChatroomCoordinator {
	public TextChannel getDiscordChannelForRoom(String roomName) {
		try {
			int roomNumber = Integer.parseInt(roomName.substring("Chatroom".length()));
			return DiscordBotService.getInstance().getDiscordChannelByTTRoomNumber(roomNumber);
		} catch (Exception e) {
			System.err.println("Error getting Discord channel for room " + roomName + ": " + e.getMessage());
			return null;
		}
	}

	public Disposable subscribeToDiscordMessages(TextChannel discordChannel, ChatRoom chatRoom) {
		if (discordChannel == null) return null;
		return discordChannel
				.getClient()
				.on(MessageCreateEvent.class)
				.filter(event -> event.getMessage().getChannelId().equals(discordChannel.getId()))
				.filter(event -> !event.getMessage().getAuthor().map(user -> user.getId().equals(DiscordBotService.getInstance().getBotId())).orElse(false))
				.subscribe(event -> handleIncomingDiscordMessage(event, chatRoom));
	}

	public void sendDiscordMessage(TextChannel discordChannel, String owner, String msg) {
		try {
			System.out.println("sending message to discord");
			if (discordChannel != null) {
				if (!msg.startsWith("<")) msg = formatChat(owner, msg);
				discordChannel.createMessage(msg).retry(3).subscribe(
						success -> {},
						error -> System.err.println("Failed to send Discord message: " + error.getMessage())
				);
			}
		} catch (Exception e) {
			System.err.println("Error sending discord message: " + e.getMessage());
		}
	}

	public void sendDiscordEmbed(TextChannel discordChannel, EmbedCreateSpec embed, String roomName) {
		try {
			if (discordChannel != null) {
				discordChannel.createMessage(embed).retry(3).subscribe(
						success -> {},
						error -> System.err.println("Failed to send Discord embed: " + error.getMessage())
				);
			}
		} catch (Exception e) {
			System.err.println("Error sending discord embed in chat room " + roomName + ": " + e.getMessage());
		}
	}

	public void handleIncomingDiscordMessage(MessageCreateEvent event, ChatRoom chatRoom) {
		System.out.println("Handling Discord message for chatroom " + chatRoom.getName());
		String content = event.getMessage().getContent();
		event.getMember().ifPresent(member -> {
			String displayName = member.getDisplayName();
			String author = "@" + displayName;
			System.out.println("Processing Discord message from " + author + ": " + content);
			chatRoom.sendMessage(author, content);
		});
	}

	private String formatChat(String owner, String message) {
		return "<" + owner + "> " + message;
	}
}
