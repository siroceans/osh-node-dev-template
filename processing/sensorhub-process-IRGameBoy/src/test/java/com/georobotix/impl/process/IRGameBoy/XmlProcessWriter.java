package com.georobotix.impl.process.IRGameBoy;

import net.opengis.sensorml.v20.AggregateProcess;
import org.junit.Test;
import com.botts.process.helpers.ProcessHelper;

public class XmlProcessWriter {

    public XmlProcessWriter() throws Exception {
        testGameboyProcess();
    }

    @Test
    public void testGameboyProcess() throws Exception {

        ProcessHelper processHelper = new ProcessHelper();
        ProcessHelper.ProcessChainBuilder chainBuilder = processHelper.createProcessChain();

        // JoyCon IR Driver
        chainBuilder.addDataSource("joyconcamera", "urn:osh:sensor:joycon-image");

        // gameboy camera process and output list
        chainBuilder.addProcess("gameboyprocess", new GameboyProcess());
        chainBuilder.addOutputList(new GameboyProcess().getOutputList());

        // Connections
        // components/[datasource-name]/outputs/[classname]/outputname -> components/[process-name]/inputs/[datarecord-name]/inputname
        chainBuilder.addConnection("components/joyconcamera/outputs/JoyConImageOutput/time",
                "components/gameboyprocess/inputs/VideoFrame/time");
        chainBuilder.addConnection("components/joyconcamera/outputs/JoyConImageOutput/width",
                "components/gameboyprocess/inputs/VideoFrame/width");
        chainBuilder.addConnection("components/joyconcamera/outputs/JoyConImageOutput/height",
                "components/gameboyprocess/inputs/VideoFrame/height");
        chainBuilder.addConnection("components/joyconcamera/outputs/JoyConImageOutput/img",
                "components/joyconcamera/outputs/img");

        AggregateProcess finalProcess = chainBuilder.build();

        processHelper.writeProcessXML(finalProcess, System.out);

    }
}
