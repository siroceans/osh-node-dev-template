package com.georobotix.impl.process.planetdistance;

import org.sensorhub.impl.processing.AbstractProcessProvider;

public class ProcessDescriptors extends AbstractProcessProvider {

    public ProcessDescriptors() {
        addImpl(MyProcess.INFO);
    }

}