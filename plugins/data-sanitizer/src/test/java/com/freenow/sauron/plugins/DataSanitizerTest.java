package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.junit.Test;

public class DataSanitizerTest
{
    private static final String SANITIZED_SERVICE_NAME_KEY = "sanitizedServiceName";

    private static final String SANITIZED_OWNER_KEY = "sanitizedOwner";


    @Test
    public void testDataSanitizerNoChanges()
    {
        DataSet dataSet = apply("service", "owner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerUppercaseService()
    {
        DataSet dataSet = apply("Service", "owner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerUppercaseOwner()
    {
        DataSet dataSet = apply("service", "Owner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizeCamelCaseService()
    {
        DataSet dataSet = apply("ServiceName", "owner");
        Assert.assertEquals("servicename", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerCamelCaseOwner()
    {
        DataSet dataSet = apply("service", "MyOwner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("myowner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerDashService()
    {
        DataSet dataSet = apply("service-name", "owner");
        Assert.assertEquals("servicename", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerDashOwner()
    {
        DataSet dataSet = apply("service", "my-owner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("myowner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerUnderscoreService()
    {
        DataSet dataSet = apply("service_name", "owner");
        Assert.assertEquals("servicename", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerUnderscoreOwner()
    {
        DataSet dataSet = apply("service", "my_owner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("myowner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerSpacesService()
    {
        DataSet dataSet = apply("Service NaMe", "owner");
        Assert.assertEquals("servicename", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerSpacesOwner()
    {
        DataSet dataSet = apply("service", "My owner");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("myowner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizeMixedService()
    {
        DataSet dataSet = apply("Serv ice-naMe", "owner");
        Assert.assertEquals("servicename", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("owner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    @Test
    public void testDataSanitizerMixedOwner()
    {
        DataSet dataSet = apply("service", "My-oWn er");
        Assert.assertEquals("service", dataSet.getStringAdditionalInformation(SANITIZED_SERVICE_NAME_KEY).orElseThrow(AssertionFailedError::new));
        Assert.assertEquals("myowner", dataSet.getStringAdditionalInformation(SANITIZED_OWNER_KEY).orElseThrow(AssertionFailedError::new));
    }


    private DataSet apply(String service, String owner)
    {
        return new DataSanitizer().apply(new PluginsConfigurationProperties(), createDataSet(service, owner));
    }


    private DataSet createDataSet(String service, String owner)
    {
        DataSet dataSet = new DataSet();
        dataSet.setServiceName(service);
        dataSet.setOwner(owner);
        return dataSet;
    }
}