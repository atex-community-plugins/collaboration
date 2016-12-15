package com.atex.plugins.collaboration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.atex.onecms.content.Content;
import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.IdUtil;
import com.atex.onecms.content.RepositoryClient;
import com.atex.onecms.content.Subject;
import com.atex.onecms.content.aspects.Aspect;
import com.atex.plugins.baseline.url.URLBuilder;
import com.atex.plugins.baseline.url.URLBuilderCapabilities;
import com.atex.plugins.baseline.url.URLBuilderLoader;
import com.atex.plugins.collaboration.data.CollaborationConfig;
import com.atex.plugins.collaboration.data.CollaborationData;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.polopoly.application.Application;
import com.polopoly.application.IllegalApplicationStateException;
import com.polopoly.cm.PublishingDateTime;
import com.polopoly.cm.VersionInfo;
import com.polopoly.cm.VersionedContentId;
import com.polopoly.cm.WorkflowInfo;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.cm.client.WorkflowAware;
import com.polopoly.cm.event.CommitEvent;
import com.polopoly.cm.event.TagEvent;
import com.polopoly.cm.policy.ContentPolicy;
import com.polopoly.cm.policy.Policy;
import com.polopoly.cm.policy.PolicyCMServer;
import com.polopoly.cm.workflow.WorkflowState;
import com.polopoly.integration.IntegrationServerApplication;
import com.polopoly.metadata.Dimension;
import com.polopoly.metadata.Entity;
import com.polopoly.metadata.Metadata;
import com.polopoly.metadata.MetadataAware;
import com.polopoly.siteengine.resource.Resources;
import com.polopoly.siteengine.structure.ParentPathResolver;
import com.polopoly.siteengine.structure.Site;
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
    private static final Subject SYSTEM_SUBJECT = new Subject("98", null);

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
            } else if (body instanceof CommitEvent) {
                final CommitEvent event = (CommitEvent) body;

                final WebClientUtils webClient = new WebClientUtils(config.getWebHookUrl());
                postApproval(webClient, event, config);
            }
        }
    }

    private void postApproval(final WebClientUtils webClient, final CommitEvent event, final CollaborationConfigPolicy config) {
        try {
            final ContentPolicy policy = (ContentPolicy) cmServer.getPolicy(event.getContentId().getContentId());
            final CollaborationData configData = new CollaborationConfig(config).getData(policy);
            if (!Strings.isNullOrEmpty(configData.getTemplate())) {
                final String contentType = policy.getInputTemplate().getExternalId().getExternalId();
                if (configData.isEnabled()) {
                    final WorkflowInfo workflowInfo = policy.getWorkflowInfo();
                    if ((workflowInfo != null) && !workflowInfo.isWorkflowApproved()) {
                        final WorkflowAware workflowAware = (WorkflowAware) policy.getChildPolicy("workflowAction");
                        final WorkflowState state = workflowAware.getWorkflowState();

                        final ContentBean bean = createContentBean(policy);

                        String contentState = null;
                        if (state.getLabel() != null) {
                            final Resources resources = getResourceFromPolicy(policy);
                            if (resources != null) {
                                contentState = Objects.toString(resources.getStrings().get(state.getLabel()));
                            }
                        }
                        if (Strings.isNullOrEmpty(contentState)) {
                            contentState = state.getName();
                        }
                        bean.setContentState(contentState);
                        final MessagePayload message = createMessagePayload(bean, policy, configData);

                        webClient.publish(message);
                    }
                } else {
                    LOGGER.log(Level.FINE, "content type " + contentType + " is not allowed");
                }
            }

        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private boolean isNew(final TagEvent event) {
        return (event.getBeforeVersion() == VersionedContentId.NO_EXISTING_VERSION);
    }

    private void postUpdate(final WebClientUtils webClient, final TagEvent event, final CollaborationConfigPolicy config) throws CMException {
        try {
            final ContentPolicy policy = (ContentPolicy) cmServer.getPolicy(event.getContentId().getContentId());
            final CollaborationData configData = new CollaborationConfig(config).getData(policy);
            if (!Strings.isNullOrEmpty(configData.getTemplate())) {
                if (configData.isEnabled()) {

                    if (configData.isPublishUpdates() || isNew(event)) {

                        final ContentBean bean = createContentBean(policy);
                        final MessagePayload message = createMessagePayload(bean, policy, configData);

                        webClient.publish(message);
                    }
                } else {
                    final String contentType = policy.getInputTemplate().getExternalId().getExternalId();
                    LOGGER.log(Level.FINE, "content type " + contentType + " is not allowed");
                }
            } else {
                LOGGER.fine("template is empty");
            }
        } catch (CMException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private MessagePayload createMessagePayload(final ContentBean bean, final ContentPolicy policy, final CollaborationData configData)
            throws CMException {

        final MessagePayload message = new MessagePayload();
        if (!Strings.isNullOrEmpty(configData.getChannel())) {
            message.setChannel(configData.getChannel());
        }
        if (!Strings.isNullOrEmpty(configData.getUsername())) {
            message.setUsername(configData.getUsername());
        }

        message.setText(getText(bean, getContentResult(policy), configData.getTemplate()));
        return message;
    }

    private ContentBean createContentBean(final ContentPolicy policy) throws CMException {
        final String contentType = policy.getInputTemplate().getExternalId().getExternalId();
        final com.polopoly.cm.ContentId contentId = policy.getContentId().getContentId();
        final VersionInfo versionInfo = policy.getVersionInfo();
        final ContentBean bean = new ContentBean();
        bean.setId(contentId);
        bean.setHeadline(policy.getName());
        bean.setContentType(contentType);
        bean.setByline(getUserById(versionInfo.getCommittedBy()));
        bean.setUrl(urlBuilder.buildUrl(contentId));
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
            if (workflowInfo.getVersionApproveDate() != null) {
                bean.setApproved(DATE_FORMAT.get().format(workflowInfo.getVersionApproveDate()));
            }
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
        return bean;
    }

    private ContentResult<Object> getContentResult(final ContentPolicy policy) {
        try {
            final ContentId id = IdUtil.fromPolicyContentId(policy.getContentId());
            final ContentVersionId vid = contentManager.resolve(id, SYSTEM_SUBJECT);
            return contentManager.get(vid, null, SYSTEM_SUBJECT);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    private String getUserById(final UserId userId) throws CMException {
        if (userId != null) {
            try {
                final User user = userServer.getUserByUserId(userId);
                return user.getLoginName();
            } catch (CreateException | RemoteException e) {
                throw new CMException(e);
            }
        }
        return null;
    }

    private String getText(final ContentBean bean, final ContentResult<Object> result, final String template) throws CMException {
        try (final Reader reader = new StringReader(template)) {
            final List<Object> objects = Lists.newArrayList(bean);
            if (result != null && result.getStatus().isSuccess()) {
                final Content<Object> content = result.getContent();
                if (content != null) {
                    final Map<String, Object> map = Maps.newHashMap();
                    map.put("contentData", content.getContentData());
                    for (final Aspect aspect : content.getAspects()) {
                        if (aspect != null) {
                            map.put(aspect.getName(), aspect.getData());
                        }
                    }
                    objects.add(map);
                }
            }
            return service.execute(reader, objects.toArray(new Object[objects.size()]));
        } catch (IOException e) {
            throw new CMException(e);
        }
    }

    private Resources getResourceFromPolicy(final ContentPolicy policy) throws CMException {
        final List<com.polopoly.cm.ContentId> ids = new ParentPathResolver().getParentPathAsList(policy, policy.getCMServer());
        if (ids != null) {
            for (int idx = ids.size() - 1; idx >= 0; idx--) {
                final Policy parentPolicy = policy.getCMServer().getPolicy(ids.get(idx));
                if (parentPolicy instanceof Site) {
                    return ((Site) parentPolicy).getResources();
                }
            }
        }
        return null;
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
