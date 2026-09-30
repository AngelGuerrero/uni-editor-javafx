package io.github.unieditor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** UTF-8 documents with explicit newline preservation and atomic replacement. */
final class TextFiles {
    record Content(String text, String newline, boolean bom) {}

    static Content read(Path path) throws IOException {
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        boolean bom = raw.startsWith("\uFEFF");
        if (bom) raw = raw.substring(1);
        String newline = raw.contains("\r\n") ? "\r\n" : raw.contains("\r") ? "\r" : "\n";
        return new Content(raw.replace("\r\n", "\n").replace('\r', '\n'), newline, bom);
    }

    static void write(Path path, Content content) throws IOException {
        Path absolute = path.toAbsolutePath();
        // Do not replace a symlink itself when saving its document.
        if (Files.exists(absolute)) absolute = absolute.toRealPath();
        Path temp = Files.createTempFile(absolute.getParent(), ".uni-editor-", ".tmp");
        try {
            String text = (content.bom() ? "\uFEFF" : "") + content.text().replace("\n", content.newline());
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            try {
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    static Path rename(Path path, String name) throws IOException {
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")) {
            throw new IOException("Escribe solamente el nombre del archivo, sin carpetas.");
        }
        return Files.move(path, path.resolveSibling(name));
    }
}
