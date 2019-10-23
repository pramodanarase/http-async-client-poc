package org.async;

import org.async.metric.Sustanibility;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

class Schedule implements Runnable {
    List<ProfileTask> profileTasks;
    ThreadPoolExecutor executor;

    Schedule(List<ProfileTask> profileTasks, ThreadPoolExecutor executor) {
        this.profileTasks = profileTasks;
        this.executor = executor;
    }

    @Override
    public void run() {
        sleep();
        if (HttpAsyn.singleRun) {
            waitTofinish();
            closeAll();
        } else {
            periodicSchedule();
        }

    }

    public void sleep() {
        try {
            Thread.sleep(10 * 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void closeAll() {
        try {
            if (ProfileTask.requestExecutor != null)
                ProfileTask.requestExecutor.shutdown();
            HttpAsyn.executor.shutdown();

            //close http client
            if (HttpAsyn.type == 1) {
                ExecuteUrlAsync.asynchttpclient.close();
            } else if(HttpAsyn.type == 2){
                ExecuteUrlApacheAsync.httpAsyncClient.close();
            }

            if (Sustanibility.executor != null)
                Sustanibility.executor.shutdown();
            if (!HttpAsyn.singleRun)
                HttpAsyn.scheduler.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void periodicSchedule() {
        long currentTime = System.currentTimeMillis();
        for (ProfileTask task : profileTasks) {
            if ((currentTime - task.getStartTime()) >= (task.getInterval() * 1000)) {
                if (!task.isRunning()) {
                    System.out.println("Profile scheduled : " + task.getProfileName());
                    //taskList.add(task);
                    executor.submit(task);
                    task.setRunning(true);
                    task.setStartTime(System.currentTimeMillis());
                } else {
                    System.out.println("Skipping the schedule for : " + task.getProfileName() + " ::Increase the interval Or Reduce the number of profile to be monitor");
                }
            }
        }
    }

    public void waitTofinish() {
        Collection<ProfileTask> collection = profileTasks;
        long time = System.currentTimeMillis();
        try {
            executor.invokeAll(collection);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Total time: " + (System.currentTimeMillis() - time));

    }
}

