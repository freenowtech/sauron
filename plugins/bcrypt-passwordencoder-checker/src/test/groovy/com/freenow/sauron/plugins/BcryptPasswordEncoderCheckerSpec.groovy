package com.freenow.sauron.plugins

import com.blogspot.toomuchcoding.spock.subjcollabs.Subject
import com.freenow.sauron.model.DataSet
import com.freenow.sauron.properties.PluginsConfigurationProperties
import spock.lang.Specification
import spock.lang.Unroll

import static com.freenow.sauron.plugins.BcryptPasswordEncoderChecker.ENCODES_PASSWORD_WITH_BCRYPT


class BcryptPasswordEncoderCheckerSpec extends Specification {

    private static final String CONFIG_DIRECTORIES_PROPERTY = "config-directories";


    @Subject
    BcryptPasswordEncoderChecker plugin = new BcryptPasswordEncoderChecker()

    def "should not find encodesPasswordWithBcrypt in the dataset when repository path is empty"() {

        given: "A dataset"
        DataSet dataSet = new DataSet()

        when: "the plugin is applied"
        DataSet result = plugin.apply(new PluginsConfigurationProperties(), dataSet)

        then: "Property is not found "
        result.getBooleanAdditionalInformation(ENCODES_PASSWORD_WITH_BCRYPT).isEmpty()
    }


    @Unroll
    def "should check for classes declaring BcryptPasswordEncoder in  #repositoryPath directory"() {

        given: "A repository path directory specified in the dataset"
        def dataset = createDataSet(repositoryPath)

        and: "The properties configuration with the config directories"
        def pluginConfigurationProperties = configurationProperties

        when: "the plugin is applied"
        DataSet result = plugin.apply(pluginConfigurationProperties, dataset)

        then: "The result is the expected"

        result.getBooleanAdditionalInformation(ENCODES_PASSWORD_WITH_BCRYPT).isPresent()
        result.getBooleanAdditionalInformation(ENCODES_PASSWORD_WITH_BCRYPT).get() == expected

        where:
        repositoryPath             | configurationProperties                             || expected
        "repoWithBcrypt"           | new PluginsConfigurationProperties()                || true
        "repoWithoutBcrypt"        | new PluginsConfigurationProperties()                || false
        "anotherRepoWithBcrypt"    | createConfigProperties(["configuration"])           || true
        "anotherRepoWithoutBcrypt" | createConfigProperties(["mufasa"])                  || false
        "yetAnotherRepoWithBcrypt" | createConfigProperties(["config", "configuration"]) || true

    }


    def createDataSet(String repositoryPath) {
        DataSet dataSet = new DataSet()
        String additionalInformation = "src/test/resources/${repositoryPath}"
        dataSet.setAdditionalInformation("repositoryPath", additionalInformation)
        dataSet
    }

    def createConfigProperties(List<String> configDirectories) {
        def pluginProperties = new PluginsConfigurationProperties()
        def configDirectoriesProperties = new LinkedHashMap<String, Object>()

        configDirectories.each {
            configDirectoriesProperties[it] = [name: it]
        }

        pluginProperties.put("bcrypt-passwordencoder-checker", new HashMap<String, Object>()
        {

            {
                put(CONFIG_DIRECTORIES_PROPERTY, configDirectoriesProperties)
            }
        })

        return pluginProperties
    }

}
