package io.github.jrrickard.services;

import com.vmware.xenon.common.FactoryService;
import com.vmware.xenon.common.Service;
import io.github.jrrickard.podo.Meetup;

/**
 * Created by jrickard on 8/6/17.
 */
public class MeetupFactoryService extends FactoryService {

    public static final String SELF_LINK = "/meetups";

    public MeetupFactoryService() {
        super(Meetup.class);
    }

    @Override
    public Service createServiceInstance() throws Throwable {
        return new MeetupService();
    }
}
