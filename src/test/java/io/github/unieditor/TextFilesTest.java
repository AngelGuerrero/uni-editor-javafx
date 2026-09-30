package io.github.unieditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class TextFilesTest {
    @TempDir Path dir;
    @Test void roundTripPreservesEncodingNewlinesBomAndMissingFinalNewline() throws Exception {
        for (String newline : new String[]{"\n", "\r\n", "\r"}) {
            for (String bom : new String[]{"", "\uFEFF"}) {
                Path path = dir.resolve("texto.txt");
                String original = bom + "México 🎉" + newline + "última línea";
                Files.writeString(path, original);
                TextFiles.write(path, TextFiles.read(path));
                assertEquals(original, Files.readString(path));
            }
        }
    }
    @Test void renameNeverOverwritesOrEscapesDirectory() throws Exception {
        Path source = Files.writeString(dir.resolve("a.txt"), "a");
        Path other = Files.writeString(dir.resolve("b.txt"), "b");
        assertThrows(IOException.class, () -> TextFiles.rename(source, "../escape.txt"));
        assertThrows(IOException.class, () -> TextFiles.rename(source, "b.txt"));
        assertEquals("b", Files.readString(other));
        assertEquals("a", Files.readString(TextFiles.rename(source, "sin extensión")));
    }
    @Test void invalidUtf8IsRejectedAndTemporaryFilesAreRemoved() throws Exception {
        Path path = dir.resolve("binary"); Files.write(path, new byte[]{(byte)0xff});
        assertThrows(IOException.class, () -> TextFiles.read(path));
        TextFiles.write(path, new TextFiles.Content("nuevo", "\n", false));
        try (var files = Files.list(dir)) { assertEquals(1, files.count()); }
    }
}
