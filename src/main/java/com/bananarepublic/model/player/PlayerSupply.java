package com.bananarepublic.model.player;

public class PlayerSupply {
    public static final int INITIAL_MONITORING_POSTS = 5;
    public static final int INITIAL_LABORATORIES = 4;
    public static final int INITIAL_PIPES = 15;

    private int remainingMonitoringPosts;
    private int remainingLaboratories;
    private int remainingPipes;

    public PlayerSupply() {
        this.remainingMonitoringPosts = INITIAL_MONITORING_POSTS;
        this.remainingLaboratories = INITIAL_LABORATORIES;
        this.remainingPipes = INITIAL_PIPES;
    }

    public int getRemainingMonitoringPosts() {
        return remainingMonitoringPosts;
    }

    public int getRemainingLaboratories() {
        return remainingLaboratories;
    }

    public int getRemainingPipes() {
        return remainingPipes;
    }

    public boolean hasMonitoringPost() {
        return remainingMonitoringPosts > 0;
    }

    public boolean hasLaboratory() {
        return remainingLaboratories > 0;
    }

    public boolean hasPipe() {
        return remainingPipes > 0;
    }

    public void useMonitoringPost() {
        if (!hasMonitoringPost()) {
            throw new IllegalStateException("No monitoring post supply left");
        }

        remainingMonitoringPosts--;
    }

    public void returnMonitoringPost() {
        if (remainingMonitoringPosts >= INITIAL_MONITORING_POSTS) {
            throw new IllegalStateException("Monitoring post supply cannot exceed initial amount");
        }

        remainingMonitoringPosts++;
    }

    public void useLaboratory() {
        if (!hasLaboratory()) {
            throw new IllegalStateException("No laboratory supply left");
        }

        remainingLaboratories--;
    }

    public void returnLaboratory() {
        if (remainingLaboratories >= INITIAL_LABORATORIES) {
            throw new IllegalStateException("Laboratory supply cannot exceed initial amount");
        }

        remainingLaboratories++;
    }

    public void usePipe() {
        if (!hasPipe()) {
            throw new IllegalStateException("No pipe supply left");
        }

        remainingPipes--;
    }

    public void returnPipe() {
        if (remainingPipes >= INITIAL_PIPES) {
            throw new IllegalStateException("Pipe supply cannot exceed initial amount");
        }

        remainingPipes++;
    }
}