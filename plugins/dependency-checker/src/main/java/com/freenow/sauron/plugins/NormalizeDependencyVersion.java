package com.freenow.sauron.plugins;

import lombok.extern.slf4j.Slf4j;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

@Slf4j
public final class NormalizeDependencyVersion
{

    private static final String GROUP_DELIMITER = "\\.";

    private static final int GROUP_DELIMITER_LIMIT = 4;

    private static final int GROUP_SIZE = 3;

    private static final int OF_SIZE = 4;

    private static final String PAD_STR_ZERO = "0";

    private static final String NORMALIZED_DELIMITER = ".";

    private static final String THREE_GROUPS = "\\d{" + OF_SIZE + "}.\\d{" + OF_SIZE + "}.\\d{" + OF_SIZE + "}";

    private static final String GROUP_VERSION = "0000";


    private NormalizeDependencyVersion()
    {
    }


    /**
     * Normalize a given dependency version in a representation of 0000.0000.0000, where the first group represents MAJOR,
     * second MINOR and the third the Incremental version number. Each group will contain the number with leading zeros.
     *
     * @param version dependency version
     * @return string with normalized version or empty if given version is not possible to be normalized
     */
    public static String toMajorMinorIncremental(String version)
    {

        String majorMinorIncremental = "";

        try
        {

            if (isNotBlank(version))
            {

                String normalizedMajorMinorIncremental = stream(version.split(GROUP_DELIMITER, GROUP_DELIMITER_LIMIT))
                    .limit(GROUP_SIZE)
                    .map(NormalizeDependencyVersion::toVersionGroup)
                    .collect(joining(NORMALIZED_DELIMITER));

                normalizedMajorMinorIncremental = inThreeGroups(normalizedMajorMinorIncremental);

                if (isNotBlank(normalizedMajorMinorIncremental) && normalizedMajorMinorIncremental.matches(THREE_GROUPS))
                {
                    majorMinorIncremental = normalizedMajorMinorIncremental;
                }
            }

        }
        catch (Exception exception)
        {
            log.error("Was not possible to normalize version: {} - Exception: {}", version, exception.getMessage(), exception);
        }

        return majorMinorIncremental;
    }


    private static String inThreeGroups(final String normalizedMajorMinorIncremental)
    {

        String majorMinorIncremental = normalizedMajorMinorIncremental;

        if (isNotBlank((normalizedMajorMinorIncremental)))
        {
            int numberOfGroups = normalizedMajorMinorIncremental.split(GROUP_DELIMITER).length;

            if (numberOfGroups < GROUP_DELIMITER_LIMIT)
            {
                majorMinorIncremental = normalizedMajorMinorIncremental.concat(repeat(NORMALIZED_DELIMITER + GROUP_VERSION, GROUP_SIZE - numberOfGroups));
            }
        }
        return majorMinorIncremental;
    }


    private static String toVersionGroup(String partial)
    {
        String group = leftPad(partial, OF_SIZE, PAD_STR_ZERO).substring(0, OF_SIZE);

        if (!isDigits(group))
        {

            for (int i = 0; i < group.length(); i++)
            {
                char c = group.charAt(i);
                if (!Character.isDigit(c))
                {
                    group = group.replace(c, '0');
                }
            }
        }

        return group;
    }
}
