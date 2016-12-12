package com.atex.plugins.collaboration.data;

import java.util.List;

/**
 * A representation of a specific content type template.
 *
 * @author mnova
 */
public class ContentTypeTemplate {
    private List<String> contentTypes;
    private String template;

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(final List<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }
}
