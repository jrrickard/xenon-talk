package io.github.jrrickard.services;

import com.vmware.xenon.common.StatefulService;
import io.github.jrrickard.podo.Meetup;

public class MeetupService extends StatefulService {

    public MeetupService() {
        super(Meetup.class);
        super.toggleOption(ServiceOption.PERSISTENCE, true);
        super.toggleOption(ServiceOption.OWNER_SELECTION, true);
        super.toggleOption(ServiceOption.REPLICATION, true);
    }

}
