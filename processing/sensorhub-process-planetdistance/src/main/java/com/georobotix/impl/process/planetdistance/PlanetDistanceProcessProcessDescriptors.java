package com.georobotix.impl.process.planetdistance;

import org.sensorhub.impl.processing.AbstractProcessProvider;

public class PlanetDistanceProcessProcessDescriptors extends AbstractProcessProvider {

    public PlanetDistanceProcessProcessDescriptors() {
        addImpl(MyProcess.INFO);
    }

}