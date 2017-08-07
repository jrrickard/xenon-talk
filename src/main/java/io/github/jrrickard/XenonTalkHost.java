package io.github.jrrickard;

//This is the import to bring along the base Xenon service host
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceHost;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.services.common.RootNamespaceService;
import com.vmware.xenon.services.common.SimpleTransactionFactoryService;
import com.vmware.xenon.ui.UiService;
import io.github.jrrickard.services.DeleteMeetupsTaskService;
import io.github.jrrickard.services.MeetupFactoryService;
import io.github.jrrickard.services.StatlessServiceExample;
import io.github.jrrickard.services.TagService;

import java.util.logging.Level;

public class XenonTalkHost extends ServiceHost {

    public static void main(final String[] args) throws Throwable {
        final XenonTalkHost h = new XenonTalkHost();
        h.initialize(args);
        h.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            h.log(Level.WARNING, "Host stopping ...");
            h.stop();
            h.log(Level.WARNING, "Host is stopped");
        }));
    }

    /**
     * Start services: a host can run multiple services.
     */
    @Override
    public ServiceHost start() throws Throwable {
        super.start();
        // Start core services (logging, gossiping) - must be done once
        startDefaultCoreServicesSynchronously();

        /*
         * Starting services can be done two ways :
         */
        super.startService(Operation.createPost(UriUtils.buildUri(this, RootNamespaceService.class)), new RootNamespaceService());
        super.startService(new UiService());

        super.startService(new MeetupFactoryService());
        super.startService(new StatlessServiceExample());
        super.startService(new TagService());

        super.startFactory(DeleteMeetupsTaskService.class, DeleteMeetupsTaskService::createFactory);

        System.err.println(String.format("Great, let's see the UI. Go to %s",
                "http://127.0.0.1:8000/core/ui/default/#/core/ui/default/"));

        System.err.println(String.format("To see the services that are loaded. Go to %s",
                "http://127.0.0.1:8000"));
        return this;
    }
}
