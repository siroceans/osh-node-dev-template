package com.georobotix.impl.process.IRGameBoy;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.CvType;

import net.opengis.swe.v20.*;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.cdm.common.CDMException;
import org.vast.data.AbstractArrayImpl;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockMixed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.swe.helper.RasterHelper;

import javax.xml.crypto.Data;

public class GameboyProcess extends ExecutableProcessImpl {

    protected double timeStamp;
    protected int imageWidth = 160; // these two are hardcoded only for the testing purposes. they change later!
    protected int imageHeight = 120;
    protected DataArray image;
    private static final String frame = "VideoFrame";
    private DataComponent dataRecordInput;
    private DataComponent dataRecordOutput;
    private DataEncoding dataEncoding;
    private final String codec = "JPEG";

    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "gameboycamera",
            "Image Process",
            "Image processing to make the IR images from the JoyCon IR camera look like gameboy camera images.",
            GameboyProcess.class);
    RasterHelper fac = new RasterHelper();


    /**
     * Typically, you will initialize your input, output, and parameter data structures in the constructor
     */
    public GameboyProcess() {
        super(INFO);

        var imageInput = createImageInput();
        var imageOutput = createImageOutput();
        inputData.add(imageInput.getName(), imageInput);
        outputData.add(imageOutput.getName(), imageOutput);
        nu.pattern.OpenCV.loadShared();
    }

    private DataComponent createImageInput() {
        dataRecordInput =  fac.createRecord()
                .name(frame)
                .definition(fac.getPropertyUri("VideoFrame"))
                .description("Image Data")
                .addField("time", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Frame Timestamp")
                        .description("Time of data collection"))
                .addField("width", fac.createCount()
                        .id("IMAGE_WIDTH")
                        .label("Frame Width")
                        .build())
                .addField("height", fac.createCount()
                        .id("IMAGE_HEIGHT")
                        .label("Frame Height")
                        .build())
                .addField("img", fac.newRgbImage(imageWidth, imageHeight, DataType.BYTE))
                .build();

        // Set Encoding
        BinaryEncoding dataEnc = fac.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        BinaryComponent timeEnc = fac.newBinaryComponent();
        timeEnc.setRef("/" + dataRecordInput.getComponent(0).getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(timeEnc);

        BinaryBlock compressedBlock = fac.newBinaryBlock();
        compressedBlock.setRef("/" + dataRecordInput.getComponent(1).getName());
        compressedBlock.setCompression(codec);
        dataEnc.addMemberAsBlock(compressedBlock);

        try {
            fac.assignBinaryEncoding(dataRecordInput, dataEnc);
        } catch(CDMException e) {
            throw new RuntimeException("Invalid binary encoding configuration", e);
        }
        this.dataEncoding = dataEnc;

        return dataRecordInput;
    }

    private DataComponent createImageOutput() {
        dataRecordOutput =  fac.createRecord()
                .name(frame)
                .definition(fac.getPropertyUri("VideoFrame"))
                .description("Image Data")
                .addField("time", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Frame Timestamp")
                        .description("Time of data collection"))
                .addField("width", fac.createCount()
                        .id("IMAGE_WIDTH")
                        .label("Frame Width")
                        .build())
                .addField("height", fac.createCount()
                        .id("IMAGE_HEIGHT")
                        .label("Frame Height")
                        .build())
                .addField("img", fac.newRgbImage(imageWidth, imageHeight, DataType.BYTE))
                .build();

        // Set Encoding
        BinaryEncoding dataEnc = fac.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        BinaryComponent timeEnc = fac.newBinaryComponent();
        timeEnc.setRef("/" + dataRecordOutput.getComponent(0).getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(timeEnc);

        BinaryBlock compressedBlock = fac.newBinaryBlock();
        compressedBlock.setRef("/" + dataRecordOutput.getComponent(1).getName());
        compressedBlock.setCompression(codec);
        dataEnc.addMemberAsBlock(compressedBlock);

        try {
            fac.assignBinaryEncoding(dataRecordOutput, dataEnc);
        } catch(CDMException e) {
            throw new RuntimeException("Invalid binary encoding configuration", e);
        }
        this.dataEncoding = dataEnc;

        return dataRecordOutput;
    }

    private byte[] processImageColors(byte[] jpegImageBuf) {
        // LUT to map grayscale values to 4 green gameboy colors!! (sourced gameboy color palette online)
        Mat lut = new Mat(1, 256, CvType.CV_8UC3);
        int[] gameboyPalette = {
                15,  56, 15,    // Dark green
                48,  8, 48,    // Medium green
                139, 172, 15,  // Medium Light green
                155, 188, 15   // Light green
        };
        
        for (int i = 0; i < 256; i++) {
            int pIndex = (i / 64) * 3;
            // backwards, opencv expects bgr
            lut.put(0, i,
                    gameboyPalette[pIndex + 2],
                    gameboyPalette[pIndex + 1],
                    gameboyPalette[pIndex]);
        }


        Mat inputImage = Imgcodecs.imdecode(new MatOfByte(jpegImageBuf), Imgcodecs.IMREAD_COLOR);
        Mat outputImage = new Mat();
        Core.LUT(inputImage, lut, outputImage);
        MatOfByte outputBuffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", outputImage, outputBuffer);

        return outputBuffer.toArray();
    }

    /**
     * Process execution method. This is what gets called when your process runs
     */
    @Override
    public void execute() {
        // Get input Image! (from sensor driver)

        DataBlock inputDataBlock = inputData.getComponent(frame).getData();
        DataBlock outputDataBlock = inputDataBlock.clone();

        // Processing the image
        AbstractDataBlock frameDataIn = ((DataBlockMixed) inputDataBlock).getUnderlyingObject()[3];
        byte[] inputJpegBuffer = (byte[]) frameDataIn.getUnderlyingObject();
        //System.out.println(inputJpegBuffer.length);
        byte[] outputJpegBuffer = processImageColors(inputJpegBuffer);
        //System.out.println(outputJpegBuffer.length);

        // Setting output Data Block
        AbstractDataBlock frameDataOut = ((DataBlockMixed) outputDataBlock).getUnderlyingObject()[3];
        frameDataOut.setUnderlyingObject(outputJpegBuffer);

        outputData.getComponent(frame).setData(outputDataBlock);
    }
}
