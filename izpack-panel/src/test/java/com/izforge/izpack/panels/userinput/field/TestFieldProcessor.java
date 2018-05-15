package com.izforge.izpack.panels.userinput.field;

import com.izforge.izpack.panels.userinput.processor.Processor;
import com.izforge.izpack.panels.userinput.processorclient.ProcessingClient;

public class TestFieldProcessor implements Processor
{

    @Override
    public String process(final ProcessingClient client)
    {
        return "Processed: " + client.getText();
    }

}
