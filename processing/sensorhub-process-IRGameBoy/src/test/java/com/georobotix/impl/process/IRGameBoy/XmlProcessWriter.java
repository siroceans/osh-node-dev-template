package com.georobotix.impl.process.IRGameBoy;

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

        chainBuilder.uid("urn:osh:process:gameboycamera:uid");
        chainBuilder.name("gameboyProcess");
        chainBuilder.description("Process that takes joycon IR image and applies a gameboy camera filter to it");
        chainBuilder.addOutputList(new GameboyProcess().getOutputList());
        chainBuilder.addDataSource("joyconcamera", "urn:osh:sensor:joycon-image");
        chainBuilder.addConnection("joyconcamera/joyconIROutput", "ProcessInput");
//        chainBuilder.addConnection("ProcessInput", "gameboyProcess/")
    }
}
