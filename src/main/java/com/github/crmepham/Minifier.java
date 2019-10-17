package com.github.crmepham;

/**
 * Contains static method that will perform minification of supplied file contents.
 *
 * @author Christopher Mepham
 */
abstract class Minifier {

    /**
     * Line terminator characters are characters that can safely be removed during minification.
     */
    String[] WHITESPACE_CHARACTERS = new String[] {"\n", "\t", "\r", "\f"};

    /**
     * Minifies the given file contents based on the file extension.
     * @param input The file contents as a String.
     * @return The minified file contents.
     */
    String minify(String input) {

        // Remove line terminator characters.
        for (int i = 0, j = WHITESPACE_CHARACTERS.length; i < j; i++) {
            input = input.replace(WHITESPACE_CHARACTERS[i], "");
        }

        // Remove all block comments.
        return input.replaceAll("(\\/\\*.+?\\*\\/)", "");
    }
}
