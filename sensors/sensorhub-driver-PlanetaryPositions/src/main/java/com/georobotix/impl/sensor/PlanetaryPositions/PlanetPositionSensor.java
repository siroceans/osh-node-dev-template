/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.impl.sensor.PlanetaryPositions;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class PlanetPositionSensor extends AbstractSensorModule<Config> {
    static final String UID_PREFIX = "urn:osh:planetary-position:";
    static final String XML_PREFIX = "PLANET_POSITION_";

    private static final Logger logger = LoggerFactory.getLogger(PlanetPositionSensor.class);

    PlanetPositionOutput output;
    Thread processingThread;
    volatile boolean doProcessing = true;

    // Planet location and velocity values
    Planet sensorPlanet;
    double[] planetPos;
    double[] planetVel;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // Create and initialize output
        output = new PlanetPositionOutput(this);
        addOutput(output, false);
        output.doInit();

        // Initialize the desired planet.
        Planet sensorPlanet = new Planet(config.planetNameConfig);
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();
        startProcessing();
    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();
        stopProcessing();
    }

    @Override
    public boolean isConnected() {
        return processingThread != null && processingThread.isAlive();
    }

    /**
     * Starts the data processing thread.
     * <p>
     * This method simulates sensor data collection and processing by generating data samples at regular intervals.
     */
    public void startProcessing() {
        doProcessing = true;

        processingThread = new Thread(() -> {
            while (doProcessing) {
                // Simulate data collection and processing
                planetPos = sensorPlanet.getCurrentPosition();
                planetVel = sensorPlanet.getCurrentVelocity();

                output.setData(System.currentTimeMillis(), sensorPlanet.getPlanetName(), planetPos, planetVel);

                // Simulate a delay between data samples
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        processingThread.start();
    }

    /**
     * Signals the processing thread to stop.
     */
    public void stopProcessing() {
        doProcessing = false;
    }
}
