package io.github.jrrickard.services;

import com.vmware.xenon.common.FactoryService;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.OperationJoin;
import com.vmware.xenon.common.ServiceDocumentQueryResult;
import com.vmware.xenon.common.TaskState;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.services.common.QueryTask;
import com.vmware.xenon.services.common.ServiceUriPaths;
import com.vmware.xenon.services.common.TaskFactoryService;
import com.vmware.xenon.services.common.TaskService;
import io.github.jrrickard.podo.DeleteTaskServiceState;
import io.github.jrrickard.podo.Meetup;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class DeleteMeetupsTaskService extends TaskService<DeleteTaskServiceState> {

    public static final String FACTORY_LINK = "/cleanup-tasks";

    public DeleteMeetupsTaskService() {
        super(DeleteTaskServiceState.class);
        toggleOption(ServiceOption.PERSISTENCE, true);
        toggleOption(ServiceOption.REPLICATION, true);
        toggleOption(ServiceOption.INSTRUMENTATION, true);
        toggleOption(ServiceOption.OWNER_SELECTION, true);
    }

    public static FactoryService createFactory() {
        return TaskFactoryService.create(DeleteMeetupsTaskService.class, ServiceOption.IDEMPOTENT_POST,
                ServiceOption.INSTRUMENTATION);
    }

    @Override
    public void handleStart(final Operation post) {
        final DeleteTaskServiceState initialState = validateStartPost(post);
        initializeState(initialState, post);
        // complete creation POST
        post.setStatusCode(Operation.STATUS_CODE_ACCEPTED).complete();
        // self patch to start state machine
        sendSelfPatch(initialState, TaskState.TaskStage.STARTED, null);
    }

    @Override
    protected void initializeState(final DeleteTaskServiceState task, final Operation taskOperation) {
        task.subStage = DeleteTaskServiceState.SubStage.QUERY_MEETUPS;
        super.initializeState(task, taskOperation);

    }

    @Override
    protected DeleteTaskServiceState validateStartPost(final Operation taskOperation) {
        return super.validateStartPost(taskOperation);
    }

    @Override
    public void handlePatch(final Operation patch) {
        final DeleteTaskServiceState currentTask = getState(patch);
        final DeleteTaskServiceState patchBody = getBody(patch);
        updateState(currentTask, patchBody);
        patch.complete();

        switch (patchBody.taskInfo.stage) {
            case STARTED:
                handleSubstage(patchBody);
                break;
            case CANCELLED:
                logInfo("Task canceled: not implemented, ignoring");
                break;
            case FINISHED:
                logFine("Task finished successfully");
                break;
            case FAILED:
                logWarning("Task failed: %s", (patchBody.failureMessage == null ? "No reason given"
                        : patchBody.failureMessage));
                break;
            default:
                logWarning("Unexpected stage: %s", patchBody.taskInfo.stage);
                break;
        }
    }

    private void handleSubstage(final DeleteTaskServiceState task) {
        switch (task.subStage) {
            case QUERY_MEETUPS:
                handleQueryMeetups(task);
                break;
            case DELETE_MEETUPS:
                handleDeleteMeetups(task);
                break;
            default:
                logWarning("Unexpected sub stage: %s", task.subStage);
                break;
        }
    }

    private void handleQueryMeetups(final DeleteTaskServiceState task) {
        final QueryTask.Query.Builder builder = QueryTask.Query.Builder.create()
                .addKindFieldClause(Meetup.class);

        final QueryTask.Query meetupQuery = builder.build();

        final QueryTask queryTask = QueryTask.Builder.createDirectTask()
                .setQuery(meetupQuery)
                .build();

        final URI queryTaskUri = UriUtils.buildUri(this.getHost(), ServiceUriPaths.CORE_QUERY_TASKS);
        final Operation queryRequest = Operation.createPost(queryTaskUri).setBody(queryTask)
                .setCompletion((op, ex) -> {
                    if (ex != null) {
                        sendSelfFailurePatch(task, ex.getMessage());
                        return;
                    }
                    final ServiceDocumentQueryResult results = op.getBody(QueryTask.class).results;
                    if (results.documentLinks == null || results.documentLinks.isEmpty()) {
                        sendSelfFailurePatch(task, "No results");
                        return;
                    }
                    task.refLinks = results.documentLinks;
                    /*
                     * Obviously, never do this. This is just for demo emphasis.
                     */
                    try {
                        Thread.sleep(30000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendSelfPatch(task, TaskState.TaskStage.STARTED, subStageSetter(DeleteTaskServiceState.SubStage.DELETE_MEETUPS));
                });
        sendRequest(queryRequest);

    }

    private void handleDeleteMeetups(final DeleteTaskServiceState task) {
        if (task.refLinks == null || task.refLinks.isEmpty()) {
            sendSelfFailurePatch(task, "No results");
            return;
        }

        final Set<Operation> deleteOperations = new HashSet<>();
        for (final String meetupLink : task.refLinks) {
            final URI meetupUri = UriUtils.buildUri(this.getHost(), meetupLink);
            final Operation deleteOp = Operation.createDelete(meetupUri);
            deleteOperations.add(deleteOp);
        }

        final OperationJoin operationJoin = OperationJoin.create();
        operationJoin
                .setOperations(deleteOperations)
                .setCompletion((ops, exs) -> {
                    if (exs != null && !exs.isEmpty()) {
                        sendSelfFailurePatch(task, String.format("%d deletes failed", exs.size()));
                        return;
                    } else {
                        sendSelfPatch(task, TaskState.TaskStage.FINISHED, null);
                    }
                }).sendWith(this);
    }

    private Consumer<DeleteTaskServiceState> subStageSetter(final DeleteTaskServiceState.SubStage subStage) {
        return taskState -> taskState.subStage = subStage;
    }
}
