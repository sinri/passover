# Passover

A simple web gateway based on Vert.X 4.

Let Vert.x be the HTTP server, when requests come, filter them and send to configured target server, and pass the
responses to clients.

## Basic Usage

1. Fork this project.
2. Implement your own filter classes (which should extend `PassoverFilter`) and put them
   into `io.github.sinri.passover.filters`.
3. Write your own `keel.properties` and `config.yml`, and put them into `src/main/resources` or along with the
   packaged `jar` file.
4. Package project with `mvn clean package`.
5. Run with `jar` file using `java -jar` command. 