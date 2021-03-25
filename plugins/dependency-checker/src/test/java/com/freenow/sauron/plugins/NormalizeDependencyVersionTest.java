package com.freenow.sauron.plugins;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NormalizeDependencyVersionTest
{
    @Test
    public void withReleaseQualifier()
    {

        String version = "2.1.4000.RELEASE";
        assertEquals("0002.0001.4000", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withOutReleaseQualifier()
    {

        String version = "0.0.111";
        assertEquals("0000.0000.0111", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withDashOnQualifier()
    {

        String version = "1.0.0-UP-1580-2";
        assertEquals("0001.0000.0000", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withInvalidSemantics()
    {

        String version = "1.0.a-UP-1580-2";
        assertEquals("0001.0000.0000", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withMoreThanThreeGroups()
    {

        String version = "1.9.666.9";
        assertEquals("0001.0009.0666", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withMajorMinorGroupsOnly()
    {

        String version = "1.9";
        assertEquals("0001.0009.0000", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withMajorMinorGroupsAndInvalidSemantics()
    {

        String version = "1.19-aXXX";
        assertEquals("0001.1900.0000", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withMajorOnly()
    {

        String version = "1";
        assertEquals("0001.0000.0000", NormalizeDependencyVersion.toMajorMinorIncremental(version));
    }


    @Test
    public void withInvalidIncrementalSemantics()
    {

        String version = "1.0.01(RC)";
        assertEquals("0001.0000.0100", NormalizeDependencyVersion.toMajorMinorIncremental(version));

    }
}