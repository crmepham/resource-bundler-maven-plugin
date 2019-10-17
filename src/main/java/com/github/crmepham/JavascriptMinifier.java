package com.github.crmepham;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.maven.plugin.logging.Log;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import net_alchim31_maven_yuicompressor.ErrorReporter4Mojo;

/**
 * Copyright (c) 2013 Yahoo! Inc.
 * All rights reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY Yahoo! Inc. ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Yahoo! Inc. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @see https://github.com/yui/yuicompressor
 */
class JavascriptMinifier extends Minifier {

    private Log log;
    private BuildContext context;

    JavascriptMinifier(Log log, BuildContext context) {
        this.log = log;
        this.context = context;
    }

    /**
     * Minifies the given Javascript file contents using the YUICompressor library.
     *
     * @param input The file contents as a String.
     * @return The minified file contents.
     */
    String minify(String input) throws IOException {
        final ErrorReporter4Mojo errorReporter = new ErrorReporter4Mojo(log, false, context);
        final JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(input), errorReporter);
        final StringWriter writer = new StringWriter();
        compressor.compress(writer, 1, false, false, false, false);
        return writer.toString();
    }
}
