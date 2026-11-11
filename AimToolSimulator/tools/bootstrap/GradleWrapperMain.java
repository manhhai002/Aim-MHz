package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Minimal transparent Gradle distribution launcher built for this source example.
 * It is NOT Gradle's official wrapper JAR; replace it with the official one if desired.
 * Downloads only the hard-coded Gradle 8.13 distribution and verifies Gradle's SHA-256.
 */
public final class GradleWrapperMain {
    private static final String URL = "https://services.gradle.org/distributions/gradle-8.13-bin.zip";
    private static final String SHA256 = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78";

    public static void main(String[] args) throws Exception {
        Path base = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "aim-simulator-8.13");
        Path install = base.resolve("gradle-8.13");
        String executable = isWindows() ? "gradle.bat" : "gradle";
        Path bin = install.resolve("bin").resolve(executable);
        if (!Files.isRegularFile(bin)) {
            Files.createDirectories(base);
            Path tempZip = base.resolve("gradle-8.13-bin.tmp.zip");
            System.out.println("Downloading verified Gradle 8.13 from " + URL);
            download(tempZip);
            if (!SHA256.equalsIgnoreCase(sha256(tempZip))) {
                Files.deleteIfExists(tempZip);
                throw new SecurityException("Gradle distribution checksum mismatch: aborting");
            }
            Path staging = base.resolve("staging-" + ProcessHandle.current().pid());
            Files.createDirectories(staging);
            try {
                unpack(tempZip, staging);
                Path extracted = staging.resolve("gradle-8.13");
                if (!Files.isDirectory(extracted)) throw new IOException("Invalid Gradle archive layout");
                if (!Files.exists(install)) Files.move(extracted, install);
            } finally {
                Files.deleteIfExists(tempZip);
            }
        }
        if (!isWindows()) bin.toFile().setExecutable(true, false);
        List<String> cmd = new ArrayList<>();
        if (isWindows()) { cmd.add("cmd.exe"); cmd.add("/c"); }
        cmd.add(bin.toAbsolutePath().toString());
        Collections.addAll(cmd, args);
        Process process = new ProcessBuilder(cmd).inheritIO().start();
        System.exit(process.waitFor());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void download(Path to) throws IOException {
        URL url = URI.create(URL).toURL();
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) sha.update(buf, 0, n);
        }
        StringBuilder s = new StringBuilder();
        for (byte v : sha.digest()) s.append(String.format("%02x", v & 0xff));
        return s.toString();
    }

    private static void unpack(Path zipFile, Path output) throws IOException {
        long total = 0L;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[65536];
            while ((entry = zip.getNextEntry()) != null) {
                Path to = output.resolve(entry.getName()).normalize();
                if (!to.startsWith(output)) throw new SecurityException("Unsafe archive path");
                if (entry.isDirectory()) { Files.createDirectories(to); continue; }
                Files.createDirectories(to.getParent());
                try (OutputStream out = Files.newOutputStream(to)) {
                    int n;
                    while ((n = zip.read(buf)) != -1) {
                        total += n;
                        if (total > 900_000_000L) throw new IOException("Archive exceeded safety limit");
                        out.write(buf, 0, n);
                    }
                }
                zip.closeEntry();
            }
        }
    }
}
