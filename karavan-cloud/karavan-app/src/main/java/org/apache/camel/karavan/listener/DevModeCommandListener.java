package org.apache.camel.karavan.listener;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import org.apache.camel.karavan.datagrid.DatagridService;
import org.apache.camel.karavan.datagrid.model.DevModeCommand;
import org.apache.camel.karavan.datagrid.model.DevModeCommandName;
import org.apache.camel.karavan.datagrid.model.Project;
import org.apache.camel.karavan.kubernetes.KubernetesService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.Objects;

import static org.apache.camel.karavan.service.CamelService.DEVMODE_SUFFIX;

@ApplicationScoped
public class DevModeCommandListener {

    @ConfigProperty(name = "karavan.environment")
    String environment;

    @Inject
    DatagridService datagridService;

    @Inject
    KubernetesService kubernetesService;

    @ConsumeEvent(value = DatagridService.ADDRESS_DEVMODE_COMMAND, blocking = true, ordered = true)
    void receiveCommand(JsonObject message) {
        System.out.println("receiveCommand " + message);
        if (kubernetesService.inKubernetes()) {
            DevModeCommand command = message.mapTo(DevModeCommand.class);
            String runnerName = command.getProjectId() + "-" + DEVMODE_SUFFIX;
            if (Objects.equals(command.getCommandName(), DevModeCommandName.RUN)) {
                Project p = datagridService.getProject(command.getProjectId());
                kubernetesService.tryCreateRunner(p, runnerName, "");
            } else if (Objects.equals(command.getCommandName(), DevModeCommandName.DELETE)){
                kubernetesService.deleteRunner(runnerName, false);
                datagridService.deleteDevModeStatus(command.getProjectId());
            }
        }
    }
}