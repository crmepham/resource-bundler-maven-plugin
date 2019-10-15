# resource-bundler-maven-plugin
A simple and lightweight Maven plugin that will minify and bundle both local and external Javascript and CSS dependencies.

## How does it work?
Assuming you have created a Spring Boot project and have some static Javascript and CSS files in the `resources/static` directory, like so:

```
- resources
  - static
    - css
      - styles.css
      - another.css
    - js
      - default.js
      - another.js
```

During the `package` phase the plugin will first minify the files, and then bundle them together based on the sub-directory the files reside within. After the plugin has completed executing the `resources/static` directory will look like this:

```
- resources
  - static
    - css-bundle.css
    - js-bundle.js
    - css
      - styles.css
      - another.css
    - js
      - default.js
      - another.js
```

Additionally, the bundle files will be generated in the `target` build directory:

```
- target
  - classes
    - static
      - css-bundle.css
      - js-bundle.js
```

## How do I use it?

1. Include the plugin in your projects `pom.xml` before any plugin that generates the resulting jar.

```
<build>
    <plugins>
        <plugin>
            <groupId>com.github.crmepham</groupId>
            <artifactId>resource-bundler-maven-plugin</artifactId>
            <version>1.0.5</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>bundle</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

2. To customize the input and output paths simply specify the property values in a `<configuration>` block:

```
<build>
    <plugins>
        <plugin>
            <groupId>com.github.crmepham</groupId>
            <artifactId>resource-bundler-maven-plugin</artifactId>
            <version>1.0.5</version>
            <configuration>
                <fromPath>path/to/resources</fromPath>
                <toPath>path/to/target/destination</toPath>
            </configuration>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>bundle</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

3. By default only the bundle files will get copied across to the target directory. You can override this by specifying the following configuration property `<copyBundleFilesOnly>false</copyBundleFilesOnly>`.

4. You can also define external dependencies. These dependencies will be fetched, minified and bundled just like the local files. By default this must be done by creating the file `src/main/resources/dependencies.json`. This path can be overridden by specifying its value in the `<configuration>` section of the `pom.xml`, for example: `<externalDependenciesFilePath>path/to/file.json</externalDependenciesFilePath>`. The following is an example of how to configure your bundles and the external dependencies to be included in each bundle:

```
[
  {
    "name": "bootstrap",
    "js": [
      "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js",
      "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
    ],
    "css": [
      "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
    ]
  },
  {
    "name": "jquery",
    "js": [
      "https://code.jquery.com/jquery-3.4.1.slim.min.js"
    ]
  }
]
```
