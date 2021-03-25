package com.freenow.sauron.plugins

import com.freenow.sauron.model.DataSet
import com.freenow.sauron.plugins.SauronExtension
import com.freenow.sauron.plugins.protocw.Checker
import com.freenow.sauron.plugins.protocw.DefaultChecker
import com.freenow.sauron.plugins.protocw.DefaultService
import com.freenow.sauron.plugins.protocw.DefaultValidator
import com.freenow.sauron.properties.PluginsConfigurationProperties
import mu.KLogging
import org.pf4j.Extension

@Extension
class ProtocwChecker(
    private val checker: Checker = DefaultChecker(DefaultService(), DefaultValidator())
) : SauronExtension, KLogging() {

    override fun apply(properties: PluginsConfigurationProperties, input: DataSet): DataSet {
        checker.apply(
            input.getStringAdditionalInformation(INPUT_REPO_PATH),
            properties.getPluginConfigurationProperty("protocw-checker", PROP_PROTOC_FILE_NAME),
            properties.getPluginConfigurationProperty("protocw-checker", PROP_PROTOCW_PROPERTIRES_FILE_NAME)
        ).fold(
            { logger.info { it } },
            {
                input.setAdditionalInformation(OUTPUT_MISSING_PROTOCW, it == null)
                input.setAdditionalInformation(OUTPUT_PROTOC_VERSION, it)
            }
        )

        return input
    }

    companion object {
        const val PROP_PROTOC_FILE_NAME = "protocw-file-name"
        const val PROP_PROTOCW_PROPERTIRES_FILE_NAME = "protocw-properties-file-name"

        const val INPUT_REPO_PATH = "repositoryPath" // Set by git-checkout

        const val OUTPUT_MISSING_PROTOCW = "missingProtocw"
        const val OUTPUT_PROTOC_VERSION = "protocVersion"
    }
}
