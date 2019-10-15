package com.github.crmepham;

import static com.github.crmepham.FileExtension.css;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BundlerTest {

    private Main bundler;

    @Before
    public void setUp () {
        bundler = Mockito.spy(new Main());
    }

    @Test
    public void testBundle_nullBundles() throws MojoExecutionException
    {
        final File directory = mock(File.class);
        final List<File> files = new ArrayList<>();
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);
        files.add(file1);
        files.add(file2);
        final ArrayList<File> emptyList = new ArrayList<>();

        doReturn(new File[]{file1, file2}).when(directory).listFiles();
        doReturn(true).when(file1).isDirectory();
        doReturn("name").when(file1).getName();
        doReturn(emptyList).when(bundler).collectFiles(file1, FileExtension.js, new ArrayList<>());
        doReturn(null).when(bundler).createBundle("name", FileExtension.js, emptyList);
        doReturn(emptyList).when(bundler).collectFiles(file2, FileExtension.css, new ArrayList<>());
        doReturn(null).when(bundler).createBundle("name", FileExtension.css, emptyList);

        assertThat(bundler.bundleLocal(directory)).isTrue();
    }

    @Test
    public void testBundle_NoDirectoriesFound() throws MojoExecutionException
    {
        final File directory = mock(File.class);
        final List<File> files = new ArrayList<>();
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);
        files.add(file1);
        files.add(file2);
        final ArrayList<File> emptyList = new ArrayList<>();

        doReturn(new File[]{file1, file2}).when(directory).listFiles();
        doReturn(true).when(file1).isDirectory();
        doReturn("name").when(file1).getName();
        doReturn(emptyList).when(bundler).collectFiles(file1, FileExtension.js, new ArrayList<>());
        doReturn(file1).when(bundler).createBundle("name", FileExtension.js, emptyList);
        doReturn(emptyList).when(bundler).collectFiles(file2, FileExtension.css, new ArrayList<>());
        doReturn(file2).when(bundler).createBundle("name", FileExtension.css, emptyList);
        doReturn("name1").when(file1).getName();
        doReturn("name2").when(file1).getName();

        assertThat(bundler.bundleLocal(directory)).isTrue();
    }

    @Test
    public void testBundle_NoDirectoriesFound2() throws MojoExecutionException
    {
        final File directory = mock(File.class);
        final List<File> files = new ArrayList<>();
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);
        files.add(file1);
        files.add(file2);
        final ArrayList<File> emptyList = new ArrayList<>();

        doReturn(new File[]{file1, file2}).when(directory).listFiles();
        doReturn(true).when(file1).isDirectory();
        doReturn("name").when(file1).getName();
        doReturn(emptyList).when(bundler).collectFiles(file1, FileExtension.js, new ArrayList<>());
        doReturn(file1).when(bundler).createBundle("name", FileExtension.js, emptyList);
        doReturn(emptyList).when(bundler).collectFiles(file2, FileExtension.css, new ArrayList<>());
        doReturn(file2).when(bundler).createBundle("name", FileExtension.css, emptyList);
        doReturn("name1").when(file1).getName();
        doReturn("name2").when(file1).getName();

        assertThat(bundler.bundleLocal(directory)).isTrue();
    }

    @Test
    public void testCreateBundle_null() {
        assertThat(bundler.createBundle("name", FileExtension.css, new ArrayList<>())).isNull();
    }

    @Test
    public void testCreateBundle_emptyFileContentsAndThrowsIOException() throws IOException {
        final List<File> files = new ArrayList<>();
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);
        files.add(file1);
        files.add(file2);

        doThrow(IOException.class).when(bundler).getFileAsString(file1);
        doReturn("").when(bundler).getFileAsString(file2);

        assertThat(bundler.createBundle("name", FileExtension.css, files)).isNull();
    }

    @Test
    public void testCreateBundle_withFileContents() throws IOException {
        final List<File> files = new ArrayList<>();
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);
        files.add(file1);
        files.add(file2);

        doThrow(IOException.class).when(bundler).getFileAsString(file1);
        doReturn("contents").when(bundler).getFileAsString(file2);

        final File result = bundler.createBundle("name", css, files);
        assertThat(result).isNotNull();
        assertThat(result.getAbsolutePath()).contains("null/null/name-bundle.css");
    }

    @Test
    public void testCollectFiles_nullFile() {
        assertThat(bundler.collectFiles(null, css, new ArrayList<>())).isEmpty();
    }

    @Test
    public void testCollectFiles_listFilesReturnsNull() {
        final File directory = mock(File.class);
        doReturn(null).when(directory).listFiles();
        assertThat(bundler.collectFiles(directory, css, new ArrayList<>())).isEmpty();
    }

    @Test
    public void testCollectFiles_listFilesReturnsEmpty() {
        final File directory = mock(File.class);
        doReturn(new File[]{}).when(directory).listFiles();
        assertThat(bundler.collectFiles(directory, css, new ArrayList<>())).isEmpty();
    }

    @Test
    public void testCollectFiles_extensionDoesNotMatch() {
        final File directory = mock(File.class);
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);

        doReturn(new File[]{file1, file2}).when(directory).listFiles();
        doReturn(true).when(file1).isDirectory();
        doReturn(null).when(file1).listFiles();
        doReturn("file.invalid").when(file2).getName();

        assertThat(bundler.collectFiles(directory, css, new ArrayList<>())).isEmpty();
    }

    @Test
    public void testCollectFiles_success() {
        final File directory = mock(File.class);
        final File file1 = mock(File.class);
        final File file2 = mock(File.class);

        doReturn(new File[]{file1, file2}).when(directory).listFiles();
        doReturn(true).when(file1).isDirectory();
        doReturn(null).when(file1).listFiles();
        doReturn("file.css").when(file2).getName();

        assertThat(bundler.collectFiles(directory, css, new ArrayList<>()).size()).isEqualTo(1);
    }

    @Test
    public void testGetFileExtension() {
        assertThat(bundler.getFileExtension(null)).isNull();
        assertThat(bundler.getFileExtension("")).isNull();
        assertThat(bundler.getFileExtension("invalid")).isNull();
        assertThat(bundler.getFileExtension(".")).isNull();
        assertThat(bundler.getFileExtension("..")).isNull();
        assertThat(bundler.getFileExtension("file.extension.")).isNull();
        assertThat(bundler.getFileExtension("file.extension")).isEqualTo("extension");
    }
}
