package com.freenow.sauron.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectTypeTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new java.io.File("target"));

    @Test
    public void testFromPathMaven() throws IOException
    {
        tempFolder.newFile("pom.xml");
        assertEquals(ProjectType.MAVEN, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathGradleGroovy() throws IOException
    {
        tempFolder.newFile("build.gradle");
        assertEquals(ProjectType.GRADLE_GROOVY, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathGradleKotlinDsl() throws IOException
    {
        tempFolder.newFile("build.gradle.kts");
        assertEquals(ProjectType.GRADLE_KOTLIN_DSL, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathNodeJs() throws IOException
    {
        tempFolder.newFile("package.json");
        assertEquals(ProjectType.NODEJS, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathPythonPoetry() throws IOException
    {
        tempFolder.newFile("pyproject.toml");
        assertEquals(ProjectType.PYTHON_POETRY, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathPythonRequirements() throws IOException
    {
        tempFolder.newFile("requirements.txt");
        assertEquals(ProjectType.PYTHON_REQUIREMENTS, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathSbt() throws IOException
    {
        tempFolder.newFile("build.sbt");
        assertEquals(ProjectType.SBT, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathClojure() throws IOException
    {
        tempFolder.newFile("project.clj");
        assertEquals(ProjectType.CLOJURE, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathGo() throws IOException
    {
        tempFolder.newFile("go.mod");
        assertEquals(ProjectType.GO, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathUnknown()
    {
        assertEquals(ProjectType.UNKNOWN, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }

    @Test
    public void testFromPathPrecedence() throws IOException
    {
        tempFolder.newFile("pom.xml");
        tempFolder.newFile("build.gradle");
        // Maven has precedence over Gradle
        assertEquals(ProjectType.MAVEN, ProjectType.fromPath(tempFolder.getRoot().toPath()));
    }


    @Test
    public void testHasNullGroup()
    {
        assertTrue(ProjectType.NODEJS.hasNullGroup());
        assertTrue(ProjectType.PYTHON_POETRY.hasNullGroup());
        assertTrue(ProjectType.PYTHON_REQUIREMENTS.hasNullGroup());
        assertTrue(ProjectType.GO.hasNullGroup());

        assertFalse(ProjectType.MAVEN.hasNullGroup());
        assertFalse(ProjectType.GRADLE_GROOVY.hasNullGroup());
        assertFalse(ProjectType.GRADLE_KOTLIN_DSL.hasNullGroup());
        assertFalse(ProjectType.SBT.hasNullGroup());
        assertFalse(ProjectType.CLOJURE.hasNullGroup());
        assertFalse(ProjectType.UNKNOWN.hasNullGroup());
    }

    @Test
    public void testDefaultGroup()
    {
        assertEquals("org.npmjs", ProjectType.NODEJS.defaultGroup());
        assertEquals("org.python", ProjectType.PYTHON_POETRY.defaultGroup());
        assertEquals("org.python", ProjectType.PYTHON_REQUIREMENTS.defaultGroup());
        assertEquals("org.golang", ProjectType.GO.defaultGroup());
        assertEquals("", ProjectType.MAVEN.defaultGroup());
        assertEquals("", ProjectType.GRADLE_GROOVY.defaultGroup());
        assertEquals("", ProjectType.UNKNOWN.defaultGroup());
    }

    @Test
    public void testFindGoModInRoot() throws IOException
    {
        Path root = tempFolder.getRoot().toPath();
        Path rootGoMod = root.resolve("go.mod");
        Files.createFile(rootGoMod);
        assertEquals("Should find go.mod in the root", Optional.of(root), ProjectType.findGoMod(root));
    }


    @Test
    public void testFindGoModNested() throws IOException
    {
        Path root = tempFolder.getRoot().toPath();
        Path subDir = tempFolder.newFolder("module-a").toPath();
        Path nestedGoMod = subDir.resolve("go.mod");
        Files.createFile(nestedGoMod);
        assertEquals("Should find go.mod in a subdirectory", Optional.of(subDir), ProjectType.findGoMod(root));
    }


    @Test
    public void testFindGoModIgnoresExcludedDirectories() throws IOException
    {
        Path root = tempFolder.getRoot().toPath();
        String[] excludedDirs = {".git", "vendor", "kustomize"};
        for (String dirName : excludedDirs)
        {
            Path excludedDir = tempFolder.newFolder(dirName).toPath();
            Files.createFile(excludedDir.resolve("go.mod"));
        }
        assertEquals("Should return empty when only excluded go.mod files exist", Optional.empty(), ProjectType.findGoMod(root));
    }


    @Test
    public void testFindGoModIgnoresDeeplyNestedExcludedDirectories() throws IOException
    {
        Path root = tempFolder.getRoot().toPath();
        Path deepVendor = tempFolder.newFolder("src", "internal", "vendor").toPath();
        Files.createFile(deepVendor.resolve("go.mod"));
        assertEquals("Should ignore deeply nested vendor directories", Optional.empty(), ProjectType.findGoMod(root));
    }
}
