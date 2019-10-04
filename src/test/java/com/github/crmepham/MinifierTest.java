package com.github.crmepham;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MinifierTest {

    @Test
    public void testMinify() {
        assertThat(Minifier.minify("/* comment */\n .valid {\n\tcolor: green;\nbackground: yellow;\n}\n/* another comment.\n That has multiple\nlines */", FileExtension.css))
            .isEqualTo(".valid{color:green;background:yellow}");
    }
}
