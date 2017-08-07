package io.github.jrrickard.podo;

import com.vmware.xenon.common.ServiceDocumentDescription;
import com.vmware.xenon.services.common.TaskService;

import java.util.List;

/**
 * Created by jrickard on 8/6/17.
 */
public class DeleteTaskServiceState extends TaskService.TaskServiceState {


    public enum SubStage {
        QUERY_MEETUPS, DELETE_MEETUPS
    }

    @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
    public SubStage subStage;

    @UsageOption(option = ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
    public List<String> refLinks;


}
