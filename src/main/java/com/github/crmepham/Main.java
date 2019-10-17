package com.github.crmepham;

import static com.github.crmepham.FileExtension.css;
import static com.github.crmepham.FileExtension.js;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.plexus.build.incremental.ThreadBuildContext.getContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import com.google.gson.Gson;

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
 *     By default this directory is <em>target/classes/static</em>. This can be overridden
 *     by specifying a value for the <em>toPath</em> property. By default on the bundle
 *     files are copied across to the target directory, however, you can override this
 *     behaviour by setting the <em>copyBundleFilesOnly</em> property to <em>false</em>.
 * </p>
 *
 * See https://github.com/crmepham/resource-bundler-maven-plugin
 *
 * @author Christopher Mepham
 */
@Mojo(name = "bundle", defaultPhase = LifecyclePhase.PACKAGE)
public class Main extends AbstractMojo {

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
     * The path to the JSON file containing the list of external URI's. These URI's will point to
     * <em>Javascript</em> or <em>CSS</em> content. By default the file path is <em>src/main/resources/bundler/dependencies.json</em>.
     */
    @Parameter(defaultValue = "${project.build.resources[0].directory}/bundler/dependencies.json", readonly = true)
    private String externalDependenciesFilePath;

    /**
     * The top-level directory to scan for <em>Javascript</em> and <em>CSS</em> files to bundle.
     * By default this is the <em>static</em> directory underneath <em>src/main/resources</em>.
     */
    @Parameter(defaultValue = "static", readonly = true)
    private String fromPath;

    /**
     * If true, only the bundle files are copied across to the build (target) directory. Not the
     * <em>Javascript</em> and <em>CSS</em> files that were bundled.
     */
    @Parameter(defaultValue = "true", readonly = true)
    private boolean copyBundleFilesOnly;

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

        if (!bundleLocal(directory)) {
            getLog().error("Bundling failed. See above for details.");
            return;
        }

        final File dependencies = new File(externalDependenciesFilePath);
        if (!dependencies.exists()) {
            getLog().error("Skipping bundling of external dependencies. No file found at: " + externalDependenciesFilePath);
            return;
        }

        if (!bundleExternal(dependencies)) {
            getLog().error("Bundling of external dependencies failed. See above for details.");
            return;
        }

        getLog().info("Bundling completed successfully!");
    }

    /**
     * Minifies and bundles external <em>Javascript</em> and <em>CSS</em> content. The external dependency URI's must
     * be listed in the corresponding dependencies file, by default this file is located at <em>src/main/resources/bundler/dependencies.json</em>.
     * This feature is useful if you want to use the latest versions of the dependencies, assuming these dependencies have
     * a <em>latest</em> URI you can point to.
     * @param file The file containing the JSON list of URI's.
     * @return True if the execution was successful.
     */
    private boolean bundleExternal(final File file) throws MojoExecutionException {
        getLog().info("Bundling external Javascript and CSS dependencies.");
        final List<Map<String, Object>> dependencies;
        try {
            dependencies = new ArrayList<>(new Gson().fromJson(getFileAsString(file), List.class));
        } catch (IOException e) {
            getLog().error("Failed to read file: " + file.getAbsolutePath());
            return false;
        }

        if (dependencies.isEmpty()) {
            getLog().info("No external dependencies defined.");
        }

        for (Map<String, Object> bundle : dependencies) {
            createExternalBundle(bundle, js);
            createExternalBundle(bundle, css);
        }

        return true;
    }

    /**
     * Will attempt to fetch external <em>Javascript</em> and <em>CSS</em> dependencies, minify
     * them and then bundle them.
     * @param bundle The map of bundle name to list of uri's.
     * @param extension The desired bundle file extension.
     * @throws MojoExecutionException
     */
    private void createExternalBundle(final Map<String, Object> bundle, final FileExtension extension) throws MojoExecutionException {
        final List<String> uris = (List<String>) bundle.get(extension.name());
        if (uris == null || uris.isEmpty()) {
            return;
        }

        final String name = (String) bundle.get("name");
        final String fullPath = projectResourcesDirectory + File.separator + fromPath + File.separator + name + "-bundle." + extension.name();
        final File bundleFile = new File(fullPath);
        final StringBuilder builder = new StringBuilder();
        for (String uri : uris) {
            try {
                getLog().info("Fetching external dependency: " + uri);
                final String content = ExternalDependencyFetcher.fetch(uri);
                final Minifier minifier = extension == FileExtension.js ? new JavascriptMinifier(getLog(), getContext()) : new CssMinifier();
                builder.append(minifier.minify(content));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to fetch external dependency from: " + uri);
            }
        }

        try {
            if (bundleFile.exists()) {
                builder.append(FileUtils.readFileToString(bundleFile, UTF_8));
            }

            FileUtils.writeStringToFile(bundleFile, builder.toString(), UTF_8);
        } catch (IOException e) {
            getLog().error("Failed to write contents to file: " + fullPath);
        }

        final File destination = new File(projectBuildDirectory + File.separator + toPath);
        if (!destination.isDirectory()) {
            throw new MojoExecutionException("Invalid destination directory: " + destination.getAbsolutePath());
        }

        getLog().info(format("Copying external bundle file '%s' to '%s':", bundleFile.getName(), destination.getAbsolutePath()));

        try {
            FileUtils.copyFileToDirectory(bundleFile, destination, true);
        } catch (IOException e) {
            getLog().error(format("Failed to move file '%s' to target directory 'ss': %s", bundleFile.getAbsolutePath(), e.getMessage()));
        }
    }

    /**
     * Minifies and bundles <em>Javascript</em> and <em>CSS</em> files in the specified
     * source directory. Once bundled, the bundle files are copied to the specified target directory.
     * @param directory The source directory where the unminified <em>Javascript</em> and <em>CSS</em> files reside.
     * @return True if the execution was successful.
     */
    boolean bundleLocal(final File directory) throws MojoExecutionException {
        getLog().info("Bundling local Javascript and CSS dependencies.");
        final Map<File, List<File>> bundles = new HashMap<>();
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                final String bundleName = f.getName();
                List<File> files = collectFiles(f, js, new ArrayList<>());
                if (!files.isEmpty()) {
                    final File bundle = createBundle(bundleName, js, files);
                    if (bundle != null) {
                        bundles.put(bundle, files);
                    }
                }

                files = collectFiles(f, css, new ArrayList<>());
                if (!files.isEmpty()) {
                    final File bundle = createBundle(bundleName, css, files);
                    if (bundle != null) {
                        bundles.put(bundle, files);
                    }
                }
            }
        }

        if (bundles.isEmpty()) {
            getLog().info("Either no files were found or the files were empty in directory: " + directory.getAbsolutePath());
            return true;
        }

        final File destination = new File(projectBuildDirectory + File.separator + toPath);
        if (!destination.isDirectory()) {
            throw new MojoExecutionException("Invalid destination directory: " + destination.getAbsolutePath());
        }

        getLog().info(format("Copying the following %s bundle file(s) to '%s':", bundles.size(), destination.getAbsolutePath()));

        for (Map.Entry<File, List<File>> entry : bundles.entrySet()) {
            final File bundle = entry.getKey();
            try {
                FileUtils.copyFileToDirectory(bundle, destination, true);
                if (copyBundleFilesOnly) {
                    final List<File> files = entry.getValue();
                    for (File file : files) {
                        deleteFile(file.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                getLog().error(format("Failed to move file '%s' to target directory 'ss': %s", bundle.getAbsolutePath(), e.getMessage()));
            }
        }
        return true;
    }

    /**
     * Delete the files at the given absolute path.
     * @param absolutePath The absolute path to directory you wish to delete files from.
     */
    private void deleteFile(final String absolutePath) {
        final String destinationBasePath = projectBuildDirectory + File.separator + toPath;
        final String destinationFileAbsolutePath = absolutePath.replace(projectResourcesDirectory + File.separator + fromPath, destinationBasePath);
        getLog().info("Deleting file: " + destinationFileAbsolutePath);
        final File fileToDelete = new File(destinationFileAbsolutePath);
        boolean deleted = fileToDelete.delete();
        if (!deleted) {
            getLog().error("Failed to delete file: " + destinationFileAbsolutePath);
        }
        recursivelyDeleteEmptyDirectories(fileToDelete.getParent(), destinationBasePath);
    }

    /**
     * If only copy across the bundle files then this method will attempt to delete empty parent directories of the
     * file that was deleted.
     * @param destinationPath The path to the directory the file resides in, and any parent directory there after.
     * @param destinationBasePath The base target directory. This directory and its parents should not be deleted.
     */
    private void recursivelyDeleteEmptyDirectories(final String destinationPath, final String destinationBasePath) {
        if (destinationPath == null || destinationPath.length() == 0) {
            return;
        }

        if (destinationPath.equals(destinationBasePath)) {
            return;
        }

        final File file = new File(destinationPath);
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            if (file.listFiles().length > 0) {
                return;
            }
            file.delete();
        }
        recursivelyDeleteEmptyDirectories(file.getParent(), destinationBasePath);
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
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0, j = files.size(); i < j; i++) {
            final File file = files.get(i);
            try {
                final String content = getFileAsString(file);
                final Minifier minifier = extension == FileExtension.js ? new JavascriptMinifier(getLog(), getContext()) : new CssMinifier();
                buffer.append(minifier.minify(content));
                getLog().info(i+1 + ". " + file.getAbsolutePath());

            } catch (IOException e) {
                getLog().error(format("Failed to read file: %s", file.getAbsolutePath()));
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
            getLog().error("Failed to write contents to file: " + absolutePath);
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
