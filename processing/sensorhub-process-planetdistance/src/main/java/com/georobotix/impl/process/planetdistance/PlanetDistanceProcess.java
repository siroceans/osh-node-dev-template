package com.georobotix.impl.process.planetdistance;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

public class PlanetDistanceProcess extends ExecutableProcessImpl {

    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "distance-from-earth",
            "Distance From Earth Process",
            "Process that calculates the distance from Earth of another planet",
            PlanetDistanceProcess.class);

    Count input1;
    Count output1;
    Count parameter1;

    private DataComponent dataRecordInput;
    private DataComponent dataRecordOutput;

    SWEHelper sweFactory = new SWEHelper();
    private DataEncoding dataEncoding;
    Planet Earth = new Planet("earth");
    Planet planet;
    double[] currentEarthPosition;
    double[] currentPlanetPosition;

    /**
     * Typically, you will initialize your input, output, and parameter data structures in the constructor
     */
    protected PlanetDistanceProcess() {
        super(INFO);

        var dataRecordInput = createPositionInput();
        var dataRecordOutput = createDistanceOutput();

        inputData.add(dataRecordInput.getName(), dataRecordInput);
        outputData.add(dataRecordOutput.getName(), dataRecordOutput);

        // Instantiate observation planet object.
    }

    private double calculateDistanceToEarth() {


    }

    /**
     * Process execution method. This is what gets called when your process runs
     */
    @Override
    public void execute() {
        int paramValue = parameter1.getData().getIntValue();
        int inputValue = input1.getData().getIntValue();

        // Do whatever computations/processing with your input and parameter data,


        // and use it to populate the output data blocks
        int equation = inputValue * paramValue;

        output1.getData().setIntValue(equation);
    }

    private DataComponent createPositionInput() {
        // Input is output from the planet sensor driver.
        dataRecordInput = sweFactory.createRecord()
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

        return dataRecordInput;
    }

    private DataComponent createDistanceOutput() {
        dataRecordOutput = sweFactory.createRecord()
                .name("DistanceOutput")
                .definition(sweFactory.getPropertyUri("Distance"))
                .description("Distance to Earth in Km")
                .addField("distance", sweFactory.createQuantity()
                        .definition(sweFactory.getPropertyUri("Distance"))
                        .label("Distance to Earth")
                        .uom("Km"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        return dataRecordOutput;
    }
}
