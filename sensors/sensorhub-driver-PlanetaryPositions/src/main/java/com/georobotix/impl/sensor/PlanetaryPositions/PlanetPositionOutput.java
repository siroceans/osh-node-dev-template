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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.ArrayList;

/**
 * Output specification and provider for {@link Sensor}.
 */
public class PlanetPositionOutput extends AbstractSensorOutput<Sensor> {
    static final String SENSOR_OUTPUT_NAME = "PlanetPositions";
    static final String SENSOR_OUTPUT_LABEL = "PlanetaryPositions Output";
    static final String SENSOR_OUTPUT_DESCRIPTION = "Current heliocentric positions of the planets.";

    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();

    private DataRecord dataRecord;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    PlanetPositionOutput(Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {
        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // Create the data record description
        dataRecord = sweFactory.createRecord()
                .name("PlanetaryInformation")
                .label("Heliocentric Planetary Position and Velocity")
                .description("Position and Velocity of a Planet in the Heliocentric coordinate system.")
                // Time of the observation
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time"))
                // Name of the planet
                .addField("PlanetName", sweFactory.createText()
                        .definition(sweFactory.getPropertyUri("PlanetName"))
                        .label("Name of the Planet"))
                // Planet Position
                .addField("position", sweFactory.createVector()
                        .definition(sweFactory.getPropertyUri("PositionVector"))
                        .label("Position Vector")
                        .description("Heliocentric position vector of the planet in AU.")
                        .addCoordinate("i", sweFactory.createQuantity()
                                .label("i Component")
                                .uom("AU"))
                        .addCoordinate("j", sweFactory.createQuantity()
                                .label("j Component")
                                .uom("AU"))
                        .addCoordinate("k", sweFactory.createQuantity()
                                .label("k Component")
                                .uom("AU")))
                .addField("velocity", sweFactory.createVector()
                        .definition(sweFactory.getPropertyUri("VelocityVector"))
                        .label("Velocity Vector")
                        .description("Heliocentric velocity vector of the planet in AU/TU")
                        .addCoordinate("i", sweFactory.createQuantity()
                                .label("i Component")
                                .uom("AU/TU"))
                        .addCoordinate("j", sweFactory.createQuantity()
                                .label("j Component")
                                .uom("AU/TU"))
                        .addCoordinate("k", sweFactory.createQuantity()
                                .label("k Component")
                                .uom("AU/TU")))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        synchronized (histogramLock) {
            double sum = 0;
            for (double sample : intervalHistogram)
                sum += sample;

            return sum / intervalHistogram.size();
        }
    }

    /**
     * Sets the data for the output and publishes it.
     */
    public void setData(long timestamp, String planetName, double[] rCurrent, double[] vCurrent) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

            updateIntervalHistogram();

            // Populate the data block
            dataBlock.setDoubleValue(0, timestamp / 1000d);
            dataBlock.setStringValue(1, planetName);
            dataBlock.setDoubleValue(2, rCurrent[0]);
            dataBlock.setDoubleValue(3, rCurrent[1]);
            dataBlock.setDoubleValue(4, rCurrent[2]);
            dataBlock.setDoubleValue(5, vCurrent[0]);
            dataBlock.setDoubleValue(6, vCurrent[1]);
            dataBlock.setDoubleValue(7, vCurrent[2]);

            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = timestamp;
            eventHandler.publish(new DataEvent(latestRecordTime, PlanetPositionOutput.this, dataBlock));
        }
    }

    /**
     * Updates the interval histogram with the time between the latest record and the current time
     * for calculating the average sampling period.
     */
    private void updateIntervalHistogram() {
        synchronized (histogramLock) {
            if (latestRecord != null && latestRecordTime != Long.MIN_VALUE) {
                long interval = System.currentTimeMillis() - latestRecordTime;
                intervalHistogram.add(interval / 1000d);

                if (intervalHistogram.size() > MAX_NUM_TIMING_SAMPLES) {
                    intervalHistogram.remove(0);
                }
            }
        }
    }
}
