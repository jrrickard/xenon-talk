package io.github.jrrickard.services;

import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocumentQueryResult;
import com.vmware.xenon.common.StatelessService;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.common.Utils;
import io.github.jrrickard.dtos.Topic;
import io.github.jrrickard.dtos.TopicList;
import io.github.jrrickard.podo.Meetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagService extends StatelessService {

    public static final String SELF_LINK = "/tags";

    public TagService() {
        toggleOption(ServiceOption.URI_NAMESPACE_OWNER, true);
    }

    @Override
    public void handleGet(final Operation get) {

        final String template = "/tags/{tag}";
        final Map<String, String> params = UriUtils.parseUriPathSegments(get.getUri(), template);
        if (params == null || params.isEmpty()) {
            get.setBody("You must provide a tag");
            get.fail(400, new IllegalArgumentException("You must provide a tag"), "You must provide a tag");
        }

        final Operation getMeetups = Operation.createGet(this, "/meetups?expand=true").setCompletion((op, err) -> {
            final ServiceDocumentQueryResult body = op.getBody(ServiceDocumentQueryResult.class);
            final List<Topic> topics = new ArrayList<>();
            body.documents.forEach( (key, document) -> {
                final Meetup meetup = Utils.fromJson(document, Meetup.class);
                if (meetup.tags != null) {
                    if (meetup.tags.contains(params.get("tag"))) {
                        final Topic topic = new Topic();
                        topic.topic = meetup.title;
                        topic.refLink = meetup.documentSelfLink;
                        topics.add(topic);
                    }
                }
            });
            if (topics.isEmpty()) {
                get.setStatusCode(404);
                get.complete();
            } else {
                final TopicList topicList = new TopicList();
                topicList.topics = topics;
                get.setBody(topicList);
                get.setStatusCode(200);
                get.complete();
            }
        });
        getMeetups.sendWith(this);
    }
}
