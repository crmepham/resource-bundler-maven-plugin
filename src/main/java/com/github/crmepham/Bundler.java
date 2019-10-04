package com.github.crmepham;

import static com.github.crmepham.FileExtension.css;
import static com.github.crmepham.FileExtension.js;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>Minifies and bundles static <em>Javascript</em> and <em>CSS</em> resources.</p>
 *
 * <p>
 *     By default this plugin will look for <em>Javascript</em> and <em>CSS</em> files
 *     in the <em>resources/static</em> directory. This can be overridden by specifying
 *     a value for the <em>fromPath</em> property. It will create bundle files based on
 *     the directories it finds in the top-level directory. It excludes any files in the
 *     top-level directory. For every top-level directory the plugin will recursively
 *     collect <em>Javascript</em> and <em>CSS</em> files, minify them, and combine them.
 *     If there are a mixture of <em>Javascript</em> and <em>CSS</em> files under a
 *     top-level directory then two bundle files will be created, the first for the
 *     combined <em>Javascript</em> files, and the second for the combined <em>CSS</em>
 *     files. The two files will share the same name but will have different extensions.
 * </p>
 *
 * <p>
 *     Once the bundled files are created they are then copied to the target directory.
 *     By default this directory is <em>target/class/static</em>. This can be overridden
 *     by specifying a value for the <em>toPath</em> property.
 * </p>
 *
 * See https://github.com/crmepham/resource-bundler-maven-plugin
 *
 * @author Christopher Mepham
 */
@Mojo(name = "bundle", defaultPhase = LifecyclePhase.PACKAGE)
public class Bundler extends AbstractMojo {

    /**
     * The project build directory. Typically this is the <em>target</em> directory.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private String projectBuildDirectory;

    /**
     * The resource directory. Typically this is the <em>src/main/resources</em> directory.
     */
    @Parameter(defaultValue = "${project.build.resources[0].directory}", required = true, readonly = true)
    private String projectResourcesDirectory;

    /**
     * The top-level directory to scan for <em>Javascript</em> and <em>CSS</em> files to bundle.
     * By default this is the <em>static</em> directory underneath <em>src/main/resources</em>.
     */
    @Parameter(defaultValue = "static", readonly = true)
    private String fromPath;

    /**
     * The directory to copy the bundled files to within the <em>target</em> directory.
     * By default this is the <em>classes/static</em> directory.
     */
    @Parameter(defaultValue = "classes/static", readonly = true)
    private String toPath;

    public void execute() throws MojoExecutionException {
        final String fullPath = projectResourcesDirectory + File.separator + fromPath;
        final File directory = new File(fullPath);
        if (!directory.exists()) {
            throw new MojoExecutionException("Directory does not exist: " + fullPath);
        }
        if (bundle(directory)) {
            getLog().error("Bundling failed. See above for details.");
        }
    }

    /**
     * Minifies and bundles <em>Javascript</em> and <em>CSS</em> files in the specified
     * source directory. Once bundled, the bundle files are copied to the specified target directory.
     * @param directory The source directory where the unminified <em>Javascript</em> and <em>CSS</em> files reside.
     * @return True if the execution was successful.
     */
    boolean bundle(final File directory) {
        final List<File> bundles = new ArrayList<>();
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                final String bundleName = f.getName();
                File bundle = createBundle(bundleName, js, collectFiles(f, js, new ArrayList<>()));
                if (bundle != null) {
                    bundles.add(bundle);
                }

                bundle = createBundle(bundleName, css, collectFiles(f, css, new ArrayList<>()));
                if (bundle != null) {
                    bundles.add(bundle);
                }
            }
        }

        if (bundles.isEmpty()) {
            getLog().info("No directories found in: " + directory.getAbsolutePath());
            return false;
        }

        final File destination = new File(projectBuildDirectory + File.separator + toPath);
        if (!destination.isDirectory()) {
            getLog().error("Invalid destination directory: " + destination.getAbsolutePath());
            return false;
        }

        getLog().info(format("Copying the following %s bundle file(s) to '%s':", bundles.size(), destination.getAbsolutePath()));

        for (int i = 0, j = bundles.size(); i < j; i++) {
            final File bundle = bundles.get(i);
            final File target = new File(destination + File.separator + bundle.getName());
            if (target.exists()) {
                target.delete();
            }
            try {
                FileUtils.copyFileToDirectory(bundle, destination, true);
                getLog().info(i+1 + ". " + bundle.getName());
            } catch (IOException e) {
                getLog().error(format("Failed to move file '%s' to target directory 'ss': %s",
                                     bundle.getAbsolutePath(), e.getMessage()));
            }
        }
        return true;
    }

    /**
     * Creates a single file, for each of the top-level directories. This file will
     * contain the contents of all child files of the given extension. This contents
     * will minified.
     * @param bundleName The name of the bundle.
     * @param extension The extension.
     * @param files The child files that will be minified and concatenated into the bundle file.
     * @return The resulting bundle file.
     */
    File createBundle(String bundleName, FileExtension extension, List<File> files) {
        if (files.isEmpty()) {
            return null;
        }

        final String name = bundleName + "-bundle" + "." + extension.name();
        getLog().info(format("Found the following %s file(s) that will be minified and bundled into file '%s':", files.size(), name));
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0, j = files.size(); i < j; i++) {
            final File file = files.get(i);
            try {
                final String content = Minifier.minify(getFileAsString(file), extension);
                buffer.append(content);
                getLog().info(i+1 + ". " + file.getAbsolutePath());

            } catch (IOException e) {
                getLog().error(format("Failed to read file: %s", file.getAbsolutePath()));
                continue;
            }
        }

        if (buffer.length() == 0) {
            return null;
        }

        final String absolutePath = projectResourcesDirectory + File.separator + fromPath + File.separator + name;
        final File bundle = new File(absolutePath);
        try {
            FileUtils.writeStringToFile(bundle, buffer.toString(), UTF_8);
            return bundle;
        } catch (IOException e) {
            getLog().error(format("Failed to write contents to file: " + absolutePath));
            return null;
        }
    }

    /**
     * Get the contents of the file as a String.
     * @param file The file.
     * @return The contents of the file as a String.
     * @throws IOException If something went wrong.
     */
    String getFileAsString(final File file) throws IOException {
        return FileUtils.readFileToString(file, UTF_8);
    }

    /**
     * Using depth-first, recursively collects files with the given extension.
     * @param directory The top-level directory to drill-down from.
     * @param extension The file extension.
     * @param result The collection of files that were identified.
     * @return The collection of files that were identified.
     */
    List<File> collectFiles(final File directory, final FileExtension extension, final List<File> result) {
        if (directory == null) {
            return result;
        }

        final File[] files = directory.listFiles();
        if (files == null) {
            return result;
        }

        if (files.length == 0) {
            return result;
        }

        for (File f : files) {
            if (f.isDirectory()) {
                collectFiles(f, extension, result);
            } else {
                final String fileExtension = getFileExtension(f.getName());
                if (extension.name().equals(fileExtension)) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    /**
     * Get the file extension from the file name.
     * @param filename The name of the file.
     * @return The extension of the file if found, otherwise <code>null</code>.
     */
    String getFileExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        if (filename.length() == 0) {
            return null;
        }
        if (!filename.contains(".")) {
            return null;
        }
        final int i = filename.lastIndexOf('.') + 1;
        if (i == filename.length()) {
            return null;
        }
        return filename.substring(i);
    }
}
