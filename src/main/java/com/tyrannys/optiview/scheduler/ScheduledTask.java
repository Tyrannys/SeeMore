package com.tyrannys.optiview.scheduler;

public interface ScheduledTask {

    void cancel();

    boolean isCancelled();

}

