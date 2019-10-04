package com.github.crmepham;

import static com.github.crmepham.FileExtension.css;

/**
 * Contains static method that will perform minification of supplied file contents.
 *
 * @author Christopher Mepham
 */
class Minifier {

    /**
     * Minifies the given file contents based on the file extension.
     * @param input The file contents as a String.
     * @param extension The extension is used to identify what type of minification to perform.
     * @return The minified file contents.
     */
    static String minify(String input, FileExtension extension) {
        // Remove any whitespace.
        input = input.replace(" ", "");

        // Remove new lines.
        input = input.replace("\n", "");

        // Remove tabs.
        input = input.replace("\t", "");

        // Remove all comments.
        input = input.replaceAll("(\\/\\*.+?\\*\\/)", "");

        if (extension == css) {
            // Remove the last semi-colon in a CSS block.
            input = input.replace(";}", "}");
        }
        return input;
    }
}
