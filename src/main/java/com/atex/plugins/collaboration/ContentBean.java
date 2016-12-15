package com.atex.plugins.collaboration;

import java.util.List;

import com.polopoly.cm.ContentId;

/**
 * A simple bean used to create a message from a template.
 *
 * @author mnova
 */
public class ContentBean {
    private ContentId id;
    private String contentType;
    private String headline;
    private String byline;
    private String url;
    private String created;
    private String published;
    private String approved;
    private String userCreated;
    private String userCommitted;
    private String userApproved;
    private String contentState;
    private List<String> tags;

    public ContentId getId() {
        return id;
    }

    public void setId(final ContentId id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(final String headline) {
        this.headline = headline;
    }

    public String getByline() {
        return byline;
    }

    public void setByline(final String byline) {
        this.byline = byline;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(final String published) {
        this.published = published;
    }

    public String getApproved() {
        return approved;
    }

    public void setApproved(final String approved) {
        this.approved = approved;
    }

    public String getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(final String userCreated) {
        this.userCreated = userCreated;
    }

    public String getUserCommitted() {
        return userCommitted;
    }

    public void setUserCommitted(final String userCommitted) {
        this.userCommitted = userCommitted;
    }

    public String getUserApproved() {
        return userApproved;
    }

    public void setUserApproved(final String userApproved) {
        this.userApproved = userApproved;
    }

    public String getContentState() {
        return contentState;
    }

    public void setContentState(final String contentState) {
        this.contentState = contentState;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }
}
