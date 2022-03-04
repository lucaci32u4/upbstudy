package xyz.lucaci32u4.upbstudy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.Getter;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(name = "UPBStudyServer", mixinStandardHelpOptions = true, versionProvider = Main.VersionProvider.class,
        description = "Server that sends reservation emails to UPB Library")
public class Main implements Callable<Integer> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    @Getter
    private Config config;

    @CommandLine.Option(names = { "-c", "--config"}, description = "The config file to load", required = true)
    private String configFile;


    @Override
    public Integer call() throws IOException {
        if (configFile.startsWith("~" + File.separator)) {
            configFile = configFile.replace("~", System.getProperty("user.home"));
        }
        log.info("Starting with config file at {}", configFile);
        if (!loadConfig(configFile)) {
            return -1;
        }

        Mailer mailer = new Mailer(config);

        Javalin javalin = Javalin.create(jc -> {
            jc.showJavalinBanner = false;
            jc.defaultContentType = "application/json";
        });

        javalin.post("/", ctx -> {
            PostReservation postReservation;
            try {
                postReservation = mapper.readValue(ctx.body(), PostReservation.class);
                //if (!config.apiKey().equals(postReservation.apiKey())) throw new Exception();
            } catch (Exception e) {
                ctx.status(400);
                ctx.result(mapper.writeValueAsBytes(new RestError("Request body must be valid JSON")));
                return;
            }
            Set<ConstraintViolation<PostReservation>> violations = factory.getValidator().validate(postReservation);
            if (!violations.isEmpty()) {
                ctx.status(400);
                ctx.result(mapper.writeValueAsBytes(new RestError(violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("\n")))));
                return;
            }
            if (!config.apiKey().equals(postReservation.apiKey())) {
                ctx.status(403);
                ctx.result(mapper.writeValueAsBytes(new RestError("Wrong API Key")));
                return;
            }
            try {
                mailer.sendMail(postReservation.dateTime());
            } catch (Mailer.MailerException exception) {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                ctx.status(500);
                ctx.result(mapper.writeValueAsBytes(new RestError(sw.toString())));
                return;
            }
            ctx.status(200);
        });

        javalin.start(config.apiHost(), config.apiPort());

        return 0;
    }

    private boolean loadConfig(String configPath) {
        Path configFile = Path.of(configPath);
        if (!Files.exists(configFile)) {
            log.error("Config file {} does not exist", configPath);
            return false;
        }
        if (!Files.isRegularFile(configFile)) {
            log.error("Config file {} is not a regular file", configPath);
            return false;
        }
        try {
            config = mapper.readValue(Files.readAllBytes(Path.of(configPath)), Config.class);
            if (config == null) {
                log.error("Config must not be empty");
                return false;
            }
            Set<ConstraintViolation<Config>> violations = factory.getValidator().validate(config);
            if (!violations.isEmpty()) {
                violations.stream().map(ConstraintViolation::getMessage).forEach(m -> {
                    log.error("Config error: {}", m);
                });
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to read config", e);
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            try {
                Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (resources.hasMoreElements()) {
                    try (var stream = resources.nextElement().openStream()) {
                        var props = new Properties();
                        props.load(stream);
                        if (props.contains("App-Version")) {
                            return new String[] { props.getProperty("App-Version") };
                        }
                    }
                }
            } catch (Exception e) {

            }
            return new String[] { "DEVELOPMENT" };
        }
    }

}


