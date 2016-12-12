package com.atex.plugins.collaboration;

import com.google.common.base.Objects;

/**
 * The message payload we are sending to slack api.
 *
 * @author mnova
 */
public class MessagePayload {

    private String text;
    private String channel;
    private String username;
    private String icon_url;
    private String icon_emoji;

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getIcon_url() {
        return icon_url;
    }

    public void setIcon_url(final String icon_url) {
        this.icon_url = icon_url;
    }

    public String getIcon_emoji() {
        return icon_emoji;
    }

    public void setIcon_emoji(final String icon_emoji) {
        this.icon_emoji = icon_emoji;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                      .add("text", text)
                      .add("channel", channel)
                      .add("username", username)
                      .add("icon_url", icon_url)
                      .add("icon_emoji", icon_emoji)
                      .toString();
    }

}
