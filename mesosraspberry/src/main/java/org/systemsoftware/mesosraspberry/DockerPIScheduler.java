package org.systemsoftware.mesosraspberry;

import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by anusha vijay
 */
public class DockerPIScheduler implements Scheduler {

    String dockerName;
    AtomicInteger taskIDGenerator = new AtomicInteger();
    boolean launched;
    List<Protos.TaskInfo> tasksList = new ArrayList<Protos.TaskInfo>();
    List<Protos.TaskID> tasksID = new ArrayList<Protos.TaskID>();

    public DockerPIScheduler(String dockerName) {
        this.dockerName = dockerName;
        this.launched = false;
    }

    @Override
    public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID, Protos.MasterInfo masterInfo) {
        System.out.println("Scheduler registered with id " + frameworkID.getValue());
    }

    @Override
    public void reregistered(SchedulerDriver schedulerDriver, Protos.MasterInfo masterInfo) {
        System.out.println("Scheduler re-registered with Master: "+ masterInfo.getHostname());
    }

    @Override
    public void resourceOffers(SchedulerDriver schedulerDriver, List<Protos.Offer> list) {


        List<Protos.OfferID> offerList = new ArrayList<Protos.OfferID>();
        for (Protos.Offer offer : list) {
            System.out.println("Got offer");
            if (!launched) {
                // generate a unique task ID
                Protos.TaskID taskId = Protos.TaskID.newBuilder()
                        .setValue(Integer.toString(taskIDGenerator.incrementAndGet())).build();
                tasksID.add(taskId);
                System.out.println("Added Task ID:" + taskId.getValue());


                System.out.println("Launching task " + taskId.getValue());

                // docker image info
                Protos.ContainerInfo.DockerInfo.Builder dockerInfoBuilder = Protos.ContainerInfo.DockerInfo.newBuilder();
                dockerInfoBuilder.setImage(dockerName);
                dockerInfoBuilder.setNetwork(Protos.ContainerInfo.DockerInfo.Network.BRIDGE);

                // container info
                Protos.ContainerInfo.Builder containerInfoBuilder = Protos.ContainerInfo.newBuilder();
                containerInfoBuilder.setType(Protos.ContainerInfo.Type.DOCKER);
                containerInfoBuilder.setDocker(dockerInfoBuilder.build());

                // create task to run
                Protos.TaskInfo dockerTask = Protos.TaskInfo.newBuilder()
                        .setName("task " + taskId.getValue())
                        .setTaskId(taskId)
                        .setSlaveId(offer.getSlaveId())
                        .addResources(Protos.Resource.newBuilder()
                                .setName("cpus")
                                .setType(Protos.Value.Type.SCALAR)
                                .setScalar(Protos.Value.Scalar.newBuilder().setValue(1)))
                        .addResources(Protos.Resource.newBuilder()
                                .setName("mem")
                                .setType(Protos.Value.Type.SCALAR)
                                .setScalar(Protos.Value.Scalar.newBuilder().setValue(128)))
                        .setContainer(containerInfoBuilder)
                        .setCommand(Protos.CommandInfo.newBuilder().setShell(false))
                        .build();

                tasksList.add(dockerTask);
                offerList.add(offer.getId());

                Protos.Filters filters = Protos.Filters.newBuilder().setRefuseSeconds(1).build();

//                schedulerDriver.launchTasks(offer.getId(), tasksList, filters);
                schedulerDriver.launchTasks(offerList, tasksList, filters);
            }
        }

    }


    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, Protos.OfferID offerID) {
        launched=false;
    //Case when the slave is lost or when Another framework is using the resources offered.
        // Status update is TASK_LOST when we use that offer and the slave is lost
        // Request for resources. accept it. Launch it.

    //schedulerDriver.requestResources(Collection<Protos.Request> requests);

    }

    @Override
    public void statusUpdate(SchedulerDriver schedulerDriver, Protos.TaskStatus taskStatus) {
        System.out.println("Status update: task "+taskStatus.getTaskId().getValue()+" state is "+taskStatus.getState());
        if (taskStatus.getState().equals(Protos.TaskState.TASK_FINISHED)){
            //Need to send an update to the Global master saying its finished
            schedulerDriver.stop();

        if (taskStatus.getState().equals(Protos.TaskState.TASK_LOST) || taskStatus.getState().equals(Protos.TaskState.TASK_FAILED)){
            System.out.println(" Task "+taskStatus.getTaskId()+" has "+ taskStatus.getState()+"Relaunch task ");
            //Need to send an update to the Global master saying its finished
        }



        } else {
            System.out.println("Task "+taskStatus.getTaskId().getValue()+" has message "+taskStatus.getMessage());
            //Need to send an update to the Global Master about status
            //Relaunching it with new offer
        }

    }

    @Override
    public void frameworkMessage(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {

    }

    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        System.out.println("Scheduler Disconnected");

    }

    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, Protos.SlaveID slaveID) {

        // Need to send a put request saying this slave was lost
        // Request resources from another slave.
        // Launch tasks

    }

    @Override
    public void executorLost(SchedulerDriver schedulerDriver, Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {

    }

    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {

    }
}
