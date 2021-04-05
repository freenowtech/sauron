package com.freenow.sauron.service;

import com.freenow.sauron.mapper.BuildMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildMapperTest extends UtilsBaseTest
{
    @Test
    public void testMakeDataSet()
    {
        assertEquals(BuildMapper.makeDataSet(buildRequest()), dataSet());
    }
}
