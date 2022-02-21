package edu.uwp.appfactory.wishope.views.messaging.items;

import androidx.annotation.Nullable;

/**
 * <h1>.</h1>
 * <p>
 *     This Goes into the list of different possible texts not in a specific conversation
 * </p>
 *
 * @author Nick Apicelli
 * @version 1.9.5
 * @since 04-12-2020
 */
public class TextMessageItem {
    private String sender;
    private String textMessage;
    private String date;

    //Three main things needed for a text item
    public TextMessageItem(final String textMessage, final String sender, final String date) {
        this.textMessage = textMessage;
        this.sender = sender;
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public int hashCode() {
        return this.textMessage.hashCode() + this.date.hashCode() + this.sender.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof TextMessageItem) {
            TextMessageItem other = (TextMessageItem) obj;
            return this.textMessage.equals(other.getTextMessage()) && this.date.equals(other.getDate()) && this.sender.equals(other.getSender());
        }
        return false;
    }
}
