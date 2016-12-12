package com.atex.plugins.collaboration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.RepositoryClient;
import com.atex.plugins.baseline.url.URLBuilder;
import com.atex.plugins.baseline.url.URLBuilderCapabilities;
import com.atex.plugins.baseline.url.URLBuilderLoader;
import com.atex.plugins.collaboration.data.CollaborationConfig;
import com.atex.plugins.collaboration.data.CollaborationConfig.CollaborationData;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.PublishingDateTime;
import com.polopoly.cm.VersionInfo;
import com.polopoly.cm.VersionedContentId;
import com.polopoly.cm.WorkflowInfo;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.event.TagEvent;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.integration.IntegrationServerApplication;
import com.polopoly.metadata.Dimension;
import com.polopoly.metadata.Entity;
import com.polopoly.metadata.Metadata;
import com.polopoly.metadata.MetadataAware;
import com.polopoly.user.server.User;
import com.polopoly.user.server.UserId;
import com.polopoly.user.server.UserServer;

/**
 * A camel processor that will process polopoly jms events.
 *
 * @author mnova
 */
public class ContentEventProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(ContentEventProcessor.class.getName());

    static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        /**
         * See {@link ThreadLocal#initialValue()}
         * @return a not null value.
         */
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
    };

    final TemplateService service = new TemplateServiceImpl();

    private PolicyCMServer cmServer;
    private UserServer userServer;
    private ContentManager contentManager;
    private URLBuilder urlBuilder;

    @Override
    public void process(final Exchange exchange) throws Exception {

        LOGGER.log(Level.FINE, "Processing " + exchange.getIn().getBody());

        init();

        final CollaborationConfigPolicy config = CollaborationConfigPolicy.getConfiguration(cmServer);
        if (!config.isEnabled()) {
            LOGGER.log(Level.FINE, "collaboration disabled");
        } else {
            final Object body = exchange.getIn().getBody();

            if (body instanceof TagEvent) {
                final TagEvent event = (TagEvent) body;

                final WebClientUtils webClient = new WebClientUtils(config.getWebHookUrl());
                postUpdate(webClient, event, config);
            }
        }
    }

    private boolean isNew(final TagEvent event) {
        return (event.getBeforeVersion() == VersionedContentId.NO_EXISTING_VERSION);
    }

    private void postUpdate(final WebClientUtils webClient, final TagEvent event, final CollaborationConfigPolicy config) throws CMException {
        try {
            final ContentPolicy policy = (ContentPolicy) cmServer.getPolicy(event.getContentId());
            final CollaborationData configData = new CollaborationConfig(config).getData(policy);
            if (!Strings.isNullOrEmpty(configData.getTemplate())) {
                final String contentType = policy.getInputTemplate().getExternalId().getExternalId();
                if (configData.isEnabled()) {

                    if (configData.isPublishUpdates() || isNew(event)) {

                        final VersionInfo versionInfo = policy.getVersionInfo();
                        final ContentBean bean = new ContentBean();
                        bean.setId(event.getContentId());
                        bean.setHeadline(policy.getName());
                        bean.setContentType(contentType);
                        bean.setByline(getUserById(versionInfo.getCommittedBy()));
                        bean.setUrl(urlBuilder.buildUrl(event.getContentId()));
                        bean.setCreated(DATE_FORMAT.get().format(policy.getContentCreationTime()));
                        bean.setUserCreated(getUserById(policy.getCreatedBy()));
                        bean.setUserCommitted(getUserById(versionInfo.getCommittedBy()));

                        final long publishingTime;
                        if (policy instanceof PublishingDateTime) {
                            publishingTime = ((PublishingDateTime) policy).getPublishingDateTime();
                        } else {
                            publishingTime = versionInfo.getVersionCommitDate().getTime();
                        }
                        bean.setPublished(DATE_FORMAT.get().format(publishingTime));

                        final WorkflowInfo workflowInfo = policy.getWorkflowInfo();
                        if (workflowInfo != null) {
                            bean.setApproved(DATE_FORMAT.get().format(workflowInfo.getVersionApproveDate()));
                            bean.setUserApproved(getUserById(workflowInfo.getApprovedBy()));
                        }

                        if (policy instanceof MetadataAware) {
                            final Metadata metadata = ((MetadataAware) policy).getMetadata();
                            if (metadata != null) {
                                final Dimension tagDimension = metadata.getDimensionById("dimension.Tag");
                                if (tagDimension != null && tagDimension.getEntities().size() > 0) {
                                    final List<String> tags = Lists.newArrayList();
                                    for (final Entity entity : tagDimension.getEntities()) {
                                        tags.add(entity.getName());
                                    }
                                    bean.setTags(tags);
                                }
                            }
                        }

                        final MessagePayload message = new MessagePayload();
                        if (!Strings.isNullOrEmpty(configData.getChannel())) {
                            message.setChannel(configData.getChannel());
                        }
                        if (!Strings.isNullOrEmpty(configData.getUsername())) {
                            message.setUsername(configData.getUsername());
                        }
                        message.setText(getText(bean, configData.getTemplate()));

                        webClient.publish(message);
                    }
                } else {
                    LOGGER.log(Level.FINE, "content type " + contentType + " is not allowed");
                }
            } else {
                LOGGER.fine("template is empty");
            }
        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private String getUserById(final UserId userId) throws CMException {
        try {
            final User user = userServer.getUserByUserId(userId);
            return user.getLoginName();
        } catch (CreateException | RemoteException e) {
            throw new CMException(e);
        }
    }

    private String getText(final ContentBean bean, final String template) throws CMException {
        try {

            try (final Reader reader = new StringReader(template)){
                return service.execute(reader, new Object[] {
                        bean
                });
            }
        } catch (IOException e) {
            throw new CMException(e);
        }
    }

    private void init() {
        try {
            final Application application = IntegrationServerApplication.getPolopolyApplication();
            final CmClient cmClient = application.getPreferredApplicationComponent(CmClient.class);
            cmServer = cmClient.getPolicyCMServer();
            userServer = cmClient.getUserServer();

            final RepositoryClient repoClient = application.getPreferredApplicationComponent(RepositoryClient.class);
            contentManager = repoClient.getContentManager();

            urlBuilder = new URLBuilderLoader(cmServer, contentManager).create(URLBuilderCapabilities.WWW);
        } catch (IllegalApplicationStateException e) {
            throw new RuntimeException(e);
        }
    }

}
