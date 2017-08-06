package io.github.jrrickard.services;

import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ReflectionUtils;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.ServiceDocumentDescription;
import com.vmware.xenon.common.StatefulService;
import io.github.jrrickard.podo.Meetup;

public class MeetupService extends StatefulService {

    public MeetupService() {
        super(Meetup.class);
        super.toggleOption(ServiceOption.PERSISTENCE, true);
        super.toggleOption(ServiceOption.OWNER_SELECTION, true);
        super.toggleOption(ServiceOption.REPLICATION, true);
    }

    @Override
    public void handlePatch(final Operation patch) {
        final Meetup currentState = getState(patch);
        final Meetup patchState = patch.getBody(Meetup.class);

        try {
            merge(getDocumentTemplate().documentDescription, currentState, patchState);
        } catch (final Exception e) {
            patch.fail(e, "Unable to patch");
        }
        patch.complete();
    }
    private void merge(final ServiceDocumentDescription desc, final Meetup source, final Meetup patch) {
        final Class<? extends ServiceDocument> clazz = source.getClass();
        if (!patch.getClass().equals(clazz)) {
            throw new IllegalArgumentException("Source object and patch object types mismatch");
        }
        for (final ServiceDocumentDescription.PropertyDescription prop : desc.propertyDescriptions.values()) {
            if (prop.usageOptions != null && prop.usageOptions.contains(ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)) {
                Object o = ReflectionUtils.getPropertyValue(prop, patch);
                if (o != null && !o.equals(ReflectionUtils.getPropertyValue(prop, source))) {
                    ReflectionUtils.setPropertyValue(prop, source, o);
                  ;
                }
            }
        }
    }
}
