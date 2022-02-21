package edu.uwp.appfactory.wishope.views.messaging.items;

import android.graphics.Bitmap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * <h1>.</h1>
 * <p>
 *     Specifically Item that goes into a conversation Text bubbles
 * </p>
 *
 * @author Nick Apicelli
 * @version 1.9.5
 * @since 04-12-2020
 */
public class MessageItem implements Comparable<MessageItem> {
    private Bitmap profilePicture;
    private String fullName;
    private String sender;
    private String conversationId;
    private String lastMessage;
    private String lastDate;
    private boolean read;

    public MessageItem(
            final Bitmap profilePicture,
            final String fullName,
            final String lastMessage,
            final String lastDate,
            final String conversationId,
            final String sender,
            boolean read) {
        this.profilePicture = profilePicture;
        this.fullName = fullName;
        this.lastMessage = lastMessage;
        this.lastDate = lastDate;
        this.conversationId = conversationId;
        this.sender = sender;
        this.read = read;
    }

    public MessageItem() {

    }

    public Bitmap getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(Bitmap profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(final String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastDate() {
        return lastDate;
    }

    public void setLastDate(String lastDate) {
        this.lastDate = lastDate;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public int compareTo(MessageItem other) {
        try {
            Date date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.US).parse(getLastDate());
            Date otherDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z", Locale.US).parse(other.getLastDate());
            return otherDate.compareTo(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MessageItem) {
            MessageItem that = (MessageItem) o;
            return this.conversationId.equals(that.conversationId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.conversationId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%n{%n" +
                        "\t\"coachProfilePicture\": \"%s\",%n" +
                        "\t\"coachName\": \"%s\",%n" +
                        "\t\"lastMessage\": \"%s\",%n" +
                        "\t\"lastDate\": \"%s\",%n" +
                        "\t\"sender\": \"%s\",%n" +
                        "\t\"conversationId\": \"%s\"%n" +
                        "}",
                this.profilePicture,
                this.fullName,
                this.lastMessage,
                this.lastDate,
                this.sender,
                this.conversationId);
    }
}
