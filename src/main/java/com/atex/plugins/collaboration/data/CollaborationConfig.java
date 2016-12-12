package com.atex.plugins.collaboration.data;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.atex.plugins.collaboration.CollaborationConfigPolicy;
import com.polopoly.cm.ContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.siteengine.structure.ParentPathResolver;

/**
 * Build a {@link CollaborationData} from the {@link CollaborationConfigPolicy} policy.
 *
 * @author mnova
 */
public class CollaborationConfig {

    private static final Logger LOGGER = Logger.getLogger(CollaborationConfig.class.getName());

    private final CollaborationConfigPolicy config;
    private final List<ContentTypeTemplate> templates;
    private final List<DepartmentOverride> overrides;

    public CollaborationConfig(final CollaborationConfigPolicy config) {
        this.config = config;
        this.templates = config.getTemplates();
        this.overrides = config.getOverrides();
    }

    public CollaborationData getData(final ContentPolicy policy) {
        final CollaborationData data = new CollaborationData();
        data.setEnabled(false);
        if (config.isEnabled()) {
            try {
                final ContentId[] parentIds = new ParentPathResolver().getParentPath(policy.getContentId(), getCMServer());
                final String contentType = policy.getInputTemplate().getExternalId().getExternalId();

                DepartmentOverride override = null;
                for (int idx = parentIds.length - 1; idx >= 0; idx--) {
                    final ContentId contentId = parentIds[idx];
                    override = getOverride(contentId);
                    if (override != null) {
                        break;
                    }
                }
                if (override != null) {
                    data.setEnabled(override.isEnabled() && isAllowed(override.getAllowedTypes(), contentType));
                    data.setUsername(override.getUsername());
                    data.setChannel(override.getChannel());
                    data.setPublishUpdates(override.isPublishUpdates());
                } else {
                    data.setEnabled(isAllowed(config.getAllowedTypes(), contentType));
                    data.setPublishUpdates(config.getDefaultPublishUpdates());
                }
                if (data.getUsername() == null) {
                    data.setUsername(config.getDefaultUsername());
                }
                if (data.getChannel() == null) {
                    data.setChannel(config.getDefaultChannel());
                }
                for (final ContentTypeTemplate contentTypeTemplate : templates) {
                    if (isAllowed(contentTypeTemplate.getContentTypes(), contentType)) {
                        data.setTemplate(contentTypeTemplate.getTemplate());
                        break;
                    }
                }
                if (data.getTemplate() == null) {
                    data.setTemplate(config.getTemplate());
                }
            } catch (CMException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return data;
    }

    private DepartmentOverride getOverride(final ContentId contentId) {
        for (final DepartmentOverride override : overrides) {
            final List<ContentId> departments = override.getDepartments();
            if (departments != null && departments.size() > 0) {
                if (departments.contains(contentId)) {
                    return override;
                }
            }
        }
        return null;
    }

    private boolean isAllowed(final List<String> allowedTypes, final String contentType) {
        if (allowedTypes.size() > 0) {
            return allowedTypes.contains(contentType);
        } else {
            return true;
        }
    }

    private PolicyCMServer getCMServer() {
        return config.getCMServer();
    }

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
}
