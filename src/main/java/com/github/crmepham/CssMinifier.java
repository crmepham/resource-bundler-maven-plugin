package com.github.crmepham;

import java.io.IOException;

/**
 * Contains static method that will perform minification of supplied file contents.
 *
 * @author Christopher Mepham
 */
class CssMinifier extends Minifier {

    /**
     * Minifies the given file contents based on the file extension.
     * @param input The file contents as a String.
     * @return The minified file contents.
     */
    String minify(String input) throws IOException {
        input = super.minify(input);

        // Remove all whitespace
        input = input.replace(" ", "");

        // Remove the last semi-colon in a CSS block.
        return input.replace(";}", "}");
    }
}
