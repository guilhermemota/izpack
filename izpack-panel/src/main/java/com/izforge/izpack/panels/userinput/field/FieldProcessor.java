/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2012 Tim Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels.userinput.field;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.ConfigurationOption;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.exception.IzPackException;
import com.izforge.izpack.panels.userinput.processor.Processor;
import com.izforge.izpack.panels.userinput.processorclient.ValuesProcessingClient;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * FieldProcessor is a wrapper around a {@link Processor}.
 *
 * @author Tim Anderson
 */
public class FieldProcessor
{
    private final IXMLElement processorElement;

    /**
     * The configuration.
     */
    private final Config config;

    /**

     * The processor class name.
     */
    private final String className;

    /**
     * The name of the variable holding the original value before processing (optional)
     */
    private final String originalValueVariable;

    /**
     * The name of the variable holding the processed value after processing (optional)
     */
    private final String toVariable;

    /**
     * The original value before processing (optional)
     */
    private String originalValue;

    /**
     * The cached processor instance.
     */
    private Processor processor;

    private InstallData installData;

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(FieldProcessor.class.getName());


    /**
     * Constructs a {@code FieldProcessor}.
     *
     * @param processorElement the processor element
     * @param config    the configuration
     */
    public FieldProcessor(IXMLElement processorElement, Config config)
    {
        className = config.getAttribute(processorElement, "class");
        originalValueVariable = config.getAttribute(processorElement, "backupVariable", true);
        toVariable = config.getAttribute(processorElement, "toVariable", true);
        this.processorElement = processorElement;
        this.config = config;
    }

    public void setInstallData(InstallData installData)
    {
        this.installData = installData;
    }

    /**
     * Processes a set of values.
     *
     * @param values the values to process
     * @return the result of the processing
     * @throws IzPackException if processing fails
     */
    public String process(String... values)
    {
        String result;
        try
        {
            if (processor == null)
            {
                processor = config.getFactory().create(className, Processor.class);
            }

            ValuesProcessingClient client = new ValuesProcessingClient(values);
            client.readParameters(processorElement);

            // Copy optional processor configuration parameters
            Set<String> names = client.getNames();
            if (names != null)
            {
                for (String key : names)
                {
                    ConfigurationOption option = client.getConfigurationOption(key);
                    if (installData != null)
                    {
                        // Resolve variables in processor configuration parameters
                        String value = option.getValue(installData.getRules());
                        String newValue = installData.getVariables().replace(value);
                        if (value != null && !value.equals(newValue))
                        {
                            option = new ConfigurationOption(newValue);
                        }
                    }
                    client.addConfigurationOption(key, option);
                }
            }

            originalValue = client.getText();
            result = processor.process(client);
        }
        catch (Throwable exception)
        {
            logger.log(Level.WARNING, "Processing using " + className + " failed: " + exception.getMessage(),
                       exception);
            if (exception instanceof IzPackException)
            {
                throw (IzPackException) exception;
            }
            throw new IzPackException("Processing using " + className + " failed: " + exception.getMessage(),
                                      exception);
        }
        return result;
    }

    public String getBackupVariable()
    {
        return originalValueVariable;
    }

    public String getOriginalValue()
    {
        return originalValue;
    }

    public String getToVariable()
    {
        return toVariable;
    }
}
