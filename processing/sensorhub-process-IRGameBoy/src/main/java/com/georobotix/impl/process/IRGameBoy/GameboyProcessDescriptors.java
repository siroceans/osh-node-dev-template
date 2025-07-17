package com.georobotix.impl.process.IRGameBoy;

import org.sensorhub.impl.processing.AbstractProcessProvider;

public class GameboyProcessDescriptors extends AbstractProcessProvider {

    public GameboyProcessDescriptors() {
        addImpl(GameboyProcess.INFO);
    }

}