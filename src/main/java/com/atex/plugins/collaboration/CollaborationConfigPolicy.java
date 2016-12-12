package com.atex.plugins.collaboration;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.atex.plugins.baseline.policy.BaselinePolicy;
import com.atex.plugins.collaboration.data.ContentTypeTemplate;
import com.atex.plugins.collaboration.data.DepartmentOverride;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.app.policy.CheckboxPolicy;
import com.polopoly.cm.app.policy.ContentTreeSelectPolicy;
import com.polopoly.cm.app.policy.DuplicatorPolicy;
import com.polopoly.cm.app.policy.DuplicatorPolicy.DuplicatorElement;
import com.polopoly.cm.app.policy.SingleValued;
import com.polopoly.cm.app.search.categorization.TypeProvider;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.Policy;
import com.polopoly.cm.policy.PolicyCMServer;

/**
 * Configuration policy.
 *
 * @author mnova
 */
public class CollaborationConfigPolicy extends BaselinePolicy {

    private static final Logger LOGGER = Logger.getLogger(CollaborationConfigPolicy.class.getName());

    public static final String EXTERNAL_ID = "plugins.com.atex.plugins.collaboration.Config";

    private static final String ENABLED = "enabled";
    private static final String WEBHOOKURL = "webHookUrl";
    private static final String TEMPLATE = "template";
    private static final String DEFAULT_CHANNEL = "defaultChannel";
    private static final String DEFAULT_USERNAME = "defaultUsername";
    private static final String DEFAULT_PUBLISHUPDATES = "defaultPublishUpdates";
    private static final String ALLOWED_TYPES = "allowedTypes";
    private static final String TEMPLATES = "templates";
    private static final String OVERRIDES = "overrides";

    public boolean isEnabled() {
        try {
            final CheckboxPolicy checkboxPolicy = (CheckboxPolicy) getChildPolicy(ENABLED);
            if (checkboxPolicy != null) {
                if (checkboxPolicy.getChecked()) {
                    return !Strings.isNullOrEmpty(getWebHookUrl());
                }
            }
        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return false;
    }

    public String getWebHookUrl() {
        return getChildValue(WEBHOOKURL, null);
    }

    public String getTemplate() {
        return getChildValue(TEMPLATE, "");
    }

    public String getDefaultChannel() {
        return getChildValue(DEFAULT_CHANNEL, "");
    }

    public String getDefaultUsername() {
        return getChildValue(DEFAULT_USERNAME, "");
    }

    public boolean getDefaultPublishUpdates() {
        return Boolean.parseBoolean(getChildValue(DEFAULT_PUBLISHUPDATES, "false"));
    }

    public List<String> getAllowedTypes() {
        final List<String> types = Lists.newArrayList();
        try {
            final TypeProvider typeProvider = (TypeProvider) getChildPolicy(ALLOWED_TYPES);

            for (final ExternalContentId eid : typeProvider.getTypes()) {
                types.add(eid.getExternalId());
            }
        } catch (CMException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve types.", e);
        }
        return types;
    }

    public List<ContentTypeTemplate> getTemplates() {
        final List<ContentTypeTemplate> templates = Lists.newArrayList();
        try {
            final DuplicatorPolicy dup = (DuplicatorPolicy) getChildPolicy(TEMPLATES);
            final List<DuplicatorElement> list = dup.getDuplicatorElements();
            if (list != null) {
                for (final DuplicatorElement element : list) {
                    final List<String> types = Lists.newArrayList();

                    final TypeProvider typeProvider = (TypeProvider) element.getChildPolicy("contentType");
                    for (final ExternalContentId eid : typeProvider.getTypes()) {
                        types.add(eid.getExternalId());
                    }

                    final String template = ((SingleValued) element.getChildPolicy("template")).getValue();
                    final ContentTypeTemplate data = new ContentTypeTemplate();
                    data.setContentTypes(types);
                    data.setTemplate(template);
                    templates.add(data);
                }
            }
        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return templates;
    }

    public List<DepartmentOverride> getOverrides() {
        final List<DepartmentOverride> overrides = Lists.newArrayList();
        try {
            final DuplicatorPolicy dup = (DuplicatorPolicy) getChildPolicy(OVERRIDES);
            final List<DuplicatorElement> list = dup.getDuplicatorElements();
            if (list != null) {
                for (final DuplicatorElement element : list) {

                    final DepartmentOverride data = new DepartmentOverride();

                    final ContentTreeSelectPolicy contentTree = (ContentTreeSelectPolicy) element.getChildPolicy("department");
                    data.setDepartments(Lists.newArrayList(contentTree.getReferences()));

                    final TypeProvider typeProvider = (TypeProvider) element.getChildPolicy("allowedTypes");
                    for (final ExternalContentId eid : typeProvider.getTypes()) {
                        data.getAllowedTypes().add(eid.getExternalId());
                    }

                    data.setEnabled(Boolean.parseBoolean(((SingleValued) element.getChildPolicy("enabled")).getValue()));
                    data.setChannel(getSingleValued(element,"channel"));
                    data.setUsername(getSingleValued(element,"username"));

                    final String publishUpdates = getSingleValued(element, "publishUpdates");
                    if (publishUpdates.equals("0")) {
                        data.setPublishUpdates(getDefaultPublishUpdates());
                    } else {
                        data.setPublishUpdates(publishUpdates.equals("1"));
                    }

                    overrides.add(data);
                }
            }
        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return overrides;
    }

    private String getSingleValued(final Policy policy, final String name) throws CMException {
        return ((SingleValued) policy.getChildPolicy(name)).getValue();
    }

    public static CollaborationConfigPolicy getConfiguration(final PolicyCMServer cmServer) throws CMException {
        return (CollaborationConfigPolicy) cmServer.getPolicy(new ExternalContentId(EXTERNAL_ID));
    }

}
