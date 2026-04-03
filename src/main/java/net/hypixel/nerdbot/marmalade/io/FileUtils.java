package net.hypixel.nerdbot.marmalade.io;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
@UtilityClass
public class FileUtils {

    public static final DateTimeFormatter REGULAR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ZZZ").withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter FILE_NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    public static String getBranchName() {
        String branchName = System.getenv("BRANCH_NAME");
        return branchName == null || branchName.isBlank() ? "unknown" : branchName;
    }

    public static String getCommitHash() {
        String commitHash = System.getProperty("COMMIT_SHA", System.getenv("COMMIT_SHA"));
        return commitHash == null || commitHash.isBlank() ? "unknown" : commitHash;
    }

    public static Map<String, Properties> getDependencyGitInfo() {
        Map<String, Properties> result = new HashMap<>();

        try {
            // Scan for git-*.properties files inside JARs on the classpath (works with shaded fat JARs)
            String classPath = System.getProperty("java.class.path", "");
            for (String path : classPath.split(File.pathSeparator)) {
                File file = new File(path);
                if (!file.isFile() || !file.getName().endsWith(".jar")) {
                    continue;
                }

                try (JarFile jar = new JarFile(file)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().matches("git-.*\\.properties")) {
                            Properties props = new Properties();
                            try (InputStream is = jar.getInputStream(entry)) {
                                props.load(is);
                            }

                            // Derive name from filename: "git-minecraftimagegenerator.properties" -> "MinecraftImageGenerator"
                            String name = entry.getName()
                                .replaceFirst("^git-", "")
                                .replaceFirst("\\.properties$", "");
                            String displayName = props.getProperty("git.build.name", name);
                            result.put(displayName, props);
                        }
                    }
                } catch (IOException e) {
                    log.debug("Failed to read git properties from {}", file.getName(), e);
                }
            }
        } catch (IOException e) {
            log.debug("Failed to scan for dependency git info", e);
        }

        return result;
    }

    public static String getDockerContainerId() {
        try {
            return Files.readString(Path.of("/etc/hostname")).trim();
        } catch (IOException e) {
            log.error("Failed to read Docker container ID from /etc/hostname", e);
            return "unknown";
        }
    }

    public static File createTempFile(String fileName, String content) throws IOException {
        String dir = System.getProperty("java.io.tmpdir");
        File file = new File(dir + File.separator + fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
        log.info("Created temporary file {}", file.getAbsolutePath());
        return file;
    }

    public static CompletableFuture<File> createTempFileAsync(String fileName, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createTempFile(fileName, content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static File toFile(BufferedImage imageToSave) throws IOException {
        File tempFile = File.createTempFile("image", ".png");
        ImageIO.write(imageToSave, "PNG", tempFile);
        return tempFile;
    }

}
