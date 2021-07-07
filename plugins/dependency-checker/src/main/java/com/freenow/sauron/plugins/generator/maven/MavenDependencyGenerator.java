package com.freenow.sauron.plugins.generator.maven;

import com.freenow.sauron.plugins.generator.DependencyGenerator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Slf4j
public class MavenDependencyGenerator implements DependencyGenerator
{
    @Override
    public Path generateCycloneDxBom(Path repositoryPath)
    {
        try
        {
            File pom = repositoryPath.resolve("pom.xml").toFile();
            injectCycloneDxPlugin(pom);
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(pom);
            request.setGoals(Collections.singletonList("cyclonedx:makeBom"));

            Invoker invoker = new DefaultInvoker();
            invoker.execute(request);
            return repositoryPath.resolve("target/bom.xml");
        }
        catch (IllegalStateException | MavenInvocationException | IOException | XmlPullParserException e)
        {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    private void injectCycloneDxPlugin(File pom) throws IOException, XmlPullParserException
    {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model pomModel = reader.read(new FileReader(pom));

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.cyclonedx");
        plugin.setArtifactId("cyclonedx-maven-plugin");
        plugin.setVersion("1.6.4");

        Xpp3Dom includeTestScope = new Xpp3Dom("includeTestScope");
        includeTestScope.setValue("true");

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(includeTestScope);
        plugin.setConfiguration(configuration);

        if (pomModel.getBuild() == null)
        {
            pomModel.setBuild(new Build());
        }

        pomModel.getBuild().getPlugins().add(plugin);

        new MavenXpp3Writer().write(new FileWriter(pom), pomModel);
    }
}