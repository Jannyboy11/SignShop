# SignShop 2

A backwards incompatible fork of [SignShop 2](https://www.spigotmc.org/resources/signshop.10997),
updated for Spigot 1.14.4.

## Compiling
This is a bit complicated for the first time because SignShop depends on a bunch of other plugins that are not available on a maven repository.
Those plugins need to be copied into the /libs folder. A list of dependencies can be found in the pom.xml.
Then, per usual for a [maven](https://maven.apache.org/) project, run `mvn clean package` to compile the SignShop jar.
Since I'm using the --release compiler option, JDK 9 or higher is required to compile the plugin,
but it'll still run on Java 8 runtime environments for now.
