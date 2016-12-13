package com.atex.plugins.collaboration.data;

/**
 * The specific collaboration data for a given content.
 *
 * @author mnova
 */
public class CollaborationData {

    private boolean enabled;
    private String channel;
    private String username;
    private String template;
    private boolean publishUpdates;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    public boolean isPublishUpdates() {
        return publishUpdates;
    }

    public void setPublishUpdates(final boolean publishUpdates) {
        this.publishUpdates = publishUpdates;
    }

}
