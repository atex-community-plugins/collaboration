package com.atex.plugins.collaboration.data;

import java.util.List;

import com.google.common.collect.Lists;
import com.polopoly.cm.ContentId;

/**
 * A representation of a specific department override.
 *
 * @author mnova
 */
public class DepartmentOverride {
    private List<ContentId> departments = new ArrayList<>();
    private boolean enabled;
    private String channel;
    private String username;
    private boolean publishUpdates;
    private List<String> allowedTypes = new ArrayList<>();

    public List<ContentId> getDepartments() {
        return departments;
    }

    public void setDepartments(final List<ContentId> departments) {
        this.departments = departments;
    }

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

    public boolean isPublishUpdates() {
        return publishUpdates;
    }

    public void setPublishUpdates(final boolean publishUpdates) {
        this.publishUpdates = publishUpdates;
    }

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(final List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
}
