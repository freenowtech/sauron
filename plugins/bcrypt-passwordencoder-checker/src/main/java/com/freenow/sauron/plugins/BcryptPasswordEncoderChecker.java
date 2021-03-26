package com.freenow.sauron.plugins;

import com.freenow.sauron.model.DataSet;
import com.freenow.sauron.properties.PluginsConfigurationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toSet;

@Extension
@Slf4j
public class BcryptPasswordEncoderChecker implements SauronExtension
{

    public static final String REPOSITORY_PATH = "repositoryPath";
    public static final String PLUGIN_ID = "bcrypt-passwordencoder-checker";
    public static final String DEFAULT_CONFIG_DIRECTORY = "config";
    public static final String ENCODES_PASSWORD_WITH_BCRYPT = "encodesPasswordsWithBcrypt";
    private static final String CONFIG_DIRECTORIES_PROPERTY = "config-directories";
    private static final String BCRYPT_PASSWORD_ENCODER_NEW_INSTANCE = "BCryptPasswordEncoder()";
    private static final int MAX_DEPTH_TO_SEARCH_BCRYPT = 5;
    private static final int MAX_DEPTH_CONFIG_DIRECTORIES = 15;
    public static final String NAME = "name";


    @Override
    public DataSet apply(PluginsConfigurationProperties properties, DataSet input)
    {
        input.getStringAdditionalInformation(REPOSITORY_PATH).ifPresent(repositoryPathAsString -> {

            try
            {
                final var repositoryPath = Paths.get(repositoryPathAsString);

                final var configDirectories = getConfigDirectories(repositoryPath, properties);

                final var filesDeclaringBcryptCount = configDirectories.stream()
                    .filter(this::hasAnyFileDeclaringBcryptPasswordEncoder)
                    .count();

                input.setAdditionalInformation(ENCODES_PASSWORD_WITH_BCRYPT, filesDeclaringBcryptCount > 0);
            }
            catch (IOException ex)
            {
                log.error(ex.getMessage(), ex);
            }
        });

        return input;
    }


    private Set<Path> getConfigDirectories(final Path repositoryPath, final PluginsConfigurationProperties configurationProperties) throws IOException
    {

        final var configDirectoriesToSearch = configurationProperties.getPluginConfigurationProperty(PLUGIN_ID, CONFIG_DIRECTORIES_PROPERTY)
            .map(directories ->
                ((Map<String, Object>) directories).values().stream()
                    .map(configDirectoryEntry -> {
                        final LinkedHashMap<String, Object> configDirectoryEntryMap = (LinkedHashMap<String, Object>) configDirectoryEntry;
                        return valueOf(configDirectoryEntryMap.get(NAME));
                    }).collect(toSet())
            ).orElse(Set.of(DEFAULT_CONFIG_DIRECTORY));

        final BiPredicate<Path, BasicFileAttributes> findPredicate = (path, basicFileAttributes) -> {

            final var file = path.toFile();
            return file.isDirectory() && configDirectoriesToSearch.contains(file.getName());
        };

        try (var configDirectoriesPaths = Files.find(repositoryPath, MAX_DEPTH_CONFIG_DIRECTORIES, findPredicate))
        {
            return configDirectoriesPaths.collect(toSet());
        }
    }


    private boolean hasAnyFileDeclaringBcryptPasswordEncoder(final Path configPath)
    {
        final BiPredicate<Path, BasicFileAttributes> findPredicate = (path, basicFileAttributes) -> {
            final var file = path.toFile();
            return !file.isDirectory() && containsBcryptPasswordEncoder(path);
        };

        try (var filesWithBcrypt = Files.find(configPath, MAX_DEPTH_TO_SEARCH_BCRYPT, findPredicate))
        {
            return filesWithBcrypt.findFirst().isPresent();
        }
        catch (final IOException ex)
        {
            log.error("Error detecting BcryptPasswordDecoder in {}", configPath);
        }
        return false;
    }


    private boolean containsBcryptPasswordEncoder(final Path path)
    {
        try (var lines = Files.lines(path))
        {
            return lines.anyMatch(line -> line.contains(BCRYPT_PASSWORD_ENCODER_NEW_INSTANCE));
        }
        catch (IOException e)
        {
            log.error("error reading file {}", path.toFile().getName());
        }
        return false;
    }
}


