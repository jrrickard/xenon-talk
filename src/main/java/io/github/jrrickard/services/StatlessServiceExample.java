package io.github.jrrickard.services;

import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocumentQueryResult;
import com.vmware.xenon.common.StatelessService;
import com.vmware.xenon.common.Utils;
import com.vmware.xenon.services.common.ServiceUriPaths;
import io.github.jrrickard.dtos.Topic;
import io.github.jrrickard.dtos.TopicList;
import io.github.jrrickard.podo.Meetup;

import java.util.ArrayList;
import java.util.List;


public class StatlessServiceExample extends StatelessService {

    public static final String SELF_LINK = "/topics";

    public StatlessServiceExample() {
        toggleOption(ServiceOption.URI_NAMESPACE_OWNER, true);
    }

    @Override
    public void handleGet(final Operation get) {
        final Operation getMeetups = Operation.createGet(this, "/meetups?expand=true").setCompletion( (op,err) -> {
            final ServiceDocumentQueryResult body = op.getBody(ServiceDocumentQueryResult.class);
            final List<Topic> topics = new ArrayList<>();
            body.documents.forEach( (key, document) -> {
                final Meetup meetup = Utils.fromJson(document, Meetup.class);
                final Topic topic = new Topic();
                topic.topic = meetup.title;
                topic.refLink = meetup.documentSelfLink;
                topics.add(topic);
            });
            final TopicList topicList = new TopicList();
            topicList.topics = topics;
            get.setBody(topicList);
            get.setStatusCode(200);
            get.complete();
        });
        getMeetups.sendWith(this);
    }
}
