# resource-bundler-maven-plugin
A Maven plugin that minifies and bundles static resource Javascript and CSS files.

## Why does this plugin exist?
1. I wanted to learn about developing Maven plugins in general and thought this would make a useful project. 
2. I needed a simple lightweight plugin that would both minify and bundle my static Javascript and CSS files.

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
            <version>1.1-SNAPSHOT</version>
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
            <version>1.1-SNAPSHOT</version>
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
