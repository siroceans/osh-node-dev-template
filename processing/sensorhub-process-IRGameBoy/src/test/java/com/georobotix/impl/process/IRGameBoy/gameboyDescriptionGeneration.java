package com.georobotix.impl.process.IRGameBoy;

import com.botts.process.helpers.ProcessHelper;
import com.google.errorprone.annotations.RestrictedApi;
import net.opengis.sensorml.v20.AggregateProcess;
import net.opengis.swe.v20.*;
import org.junit.Test;
import org.vast.data.SWEFactory;
import org.vast.process.ProcessException;
import org.vast.xml.XMLWriterException;

public class gameboyDescriptionGeneration {
    SWEFactory fac = new SWEFactory();
    ProcessHelper processHelper = new ProcessHelper();

    public AggregateProcess generateDescription() throws ProcessException {
        GameboyProcess p1 = new GameboyProcess();
        p1.init();

        return processHelper.createProcessChain()
                .name("gameboycamera")
                .uid("urn:osh:process:gameboycamera")
                .description("generic description duh")
                .addDataSource("joyconcamera", "urn:osh:sensor:joycon-image:sensor001")
                .addOutputList(p1.getOutputList())
                .addProcess("gameboyprocess", p1)
                .addConnection("components/joyconcamera/outputs/joyconIROutput",
                        "components/gameboyprocess/inputs/VideoFrame")
                .addConnection("components/gameboyprocess/outputs/VideoFrame",
                        "outputs/VideoFrame")
                .build();
    }

    @Test
    public void generateDescXML() throws ProcessException, XMLWriterException {
        processHelper.writeProcess(System.out, generateDescription(), true);
    }

}
