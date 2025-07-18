/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.georobotix.impl.sensor.joyconIR;

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.cdm.common.CDMException;
import org.vast.data.DataBlockMixed;
import org.vast.swe.helper.RasterHelper;
import org.vast.data.AbstractDataBlock;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Output specification and provider for {@link JoyConImageSensor}.
 */
public class JoyConImageOutput extends AbstractSensorOutput<JoyConImageSensor> {
    static final String SENSOR_OUTPUT_NAME = "joyconIROutput";
    static final String SENSOR_OUTPUT_LABEL = "IR Images and Video Output";
    static final String SENSOR_OUTPUT_DESCRIPTION = "Images and video from the infrared camera in the Nintendo Switch JoyCon";

    private final int width;
    private final int height;
    private final String codec = "JPEG";

    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();

    private DataComponent dataRecord;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    JoyConImageOutput(JoyConImageSensor parentSensor, int width, int height) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
        this.width = width;
        this.height = height;
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {
        // Get an instance of SWE Factory suitable to build components
        RasterHelper sweFactory = new RasterHelper();

        // Create the data record description
        dataRecord = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(sweFactory.getPropertyUri("VideoFrame"))
                .addField("time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Frame Timestamp")
                        .description("Time of data collection"))
                .addField("width", sweFactory.createCount()
                        .id("IMAGE_WIDTH")
                        .label("Frame Width")
                        .build())
                .addField("height", sweFactory.createCount()
                        .id("IMAGE_HEIGHT")
                        .label("Frame Height")
                        .build())
                .addField("img", sweFactory.newRgbImage(width, height, DataType.BYTE))
                .build();

        BinaryEncoding dataEnc = sweFactory.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        BinaryBlock compressedBlock = sweFactory.newBinaryBlock();
        compressedBlock.setRef("/" + dataRecord.getComponent(3).getName());
        compressedBlock.setCompression(codec);
        dataEnc.addMemberAsBlock(compressedBlock);

        try {
            sweFactory.assignBinaryEncoding(dataRecord, dataEnc);
        } catch (CDMException e) {
            throw new RuntimeException("Invalid binary encoding configuration", e);
        }
        this.dataEncoding = dataEnc;
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
    public void setData(byte[] imageBuf) {
        synchronized (processingLock) {
            long timestamp = System.currentTimeMillis();

            /* Troubleshoot Efforts
            byte[] imgJpeg = new byte[19*4096];
            Arrays.fill(imgJpeg, (byte) 0);

            // Convert raw bytes to jpeg format!
            try {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                img.getRaster().setDataElements(0, 0, width, height, imageBuf);
                ByteArrayOutputStream imgOutput = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", imgOutput);
                imgJpeg = imgOutput.toByteArray();
            } catch (IOException io) {
                System.err.println("IO Exception: " + io);
            }
             */


            // Get or renew the data block.
            DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

            updateIntervalHistogram();

            // Populate the data block
            // set the timestamp.
            dataBlock.setDoubleValue(0, timestamp / 1000d);
            dataBlock.setIntValue(1, this.width);
            dataBlock.setIntValue(2, this.height);

            // set the image buffer
            AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[3];
            frameData.setUnderlyingObject(imageBuf);

            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = timestamp;
            eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
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
