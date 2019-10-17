package com.github.crmepham;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class MinifierTest {

    @Test
    public void testMinify() throws IOException
    {
        assertThat(new CssMinifier().minify("/* comment */\n .valid {\n\tcolor: green;\nbackground: yellow;\n}\n/* another comment.\n That has multiple\nlines */"))
            .isEqualTo(".valid{color:green;background:yellow}");
    }
}
