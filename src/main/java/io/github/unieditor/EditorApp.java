package io.github.unieditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public final class EditorApp extends Application {
    private final TabPane tabs = new TabPane();
    private final Label status = new Label("Listo · UTF-8");
    private Stage window;
    private int fontSize = 15;

    private final class Document {
        Path path;
        String saved = "", newline = "\n";
        boolean bom;
        final TextArea editor = new TextArea();
        final Tab tab = new Tab();
        Document() {
            tab.setUserData(this);
            tab.setContent(editor);
            editor.setWrapText(false);
            style();
            editor.textProperty().addListener((o, before, after) -> update());
            editor.caretPositionProperty().addListener((o, before, after) -> updateStatus());
            tab.setOnCloseRequest(e -> { if (!canClose(this)) e.consume(); });
            update();
        }
        boolean dirty() { return !saved.equals(editor.getText()); }
        void style() { editor.setStyle("-fx-font-family: 'Consolas', 'DejaVu Sans Mono', monospace; -fx-font-size: " + fontSize + "px;"); }
        void update() {
            tab.setText((dirty() ? "● " : "") + (path == null ? "Sin título" : path.getFileName().toString()));
            tab.setTooltip(new Tooltip(path == null ? "Documento sin guardar" : path.toString()));
            updateStatus();
        }
    }

    @Override public void start(Stage stage) {
        window = stage;
        Menu file = new Menu("Archivo");
        file.getItems().addAll(item("Nuevo", "Shortcut+N", this::newDocument),
            item("Abrir…", "Shortcut+O", this::open), item("Guardar", "Shortcut+S", () -> save(current(), false)),
            item("Guardar como…", "Shortcut+Shift+S", () -> save(current(), true)),
            new SeparatorMenuItem(), item("Renombrar…", null, this::rename), item("Eliminar…", null, this::delete),
            item("Cerrar pestaña", "Shortcut+W", this::closeCurrent), new SeparatorMenuItem(),
            item("Salir", null, () -> window.fireEvent(new javafx.stage.WindowEvent(window, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST))));
        CheckMenuItem wrap = new CheckMenuItem("Ajustar líneas");
        wrap.setOnAction(e -> tabs.getTabs().forEach(t -> ((Document)t.getUserData()).editor.setWrapText(wrap.isSelected())));
        tabs.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) c -> {
            while (c.next()) for (Tab t : c.getAddedSubList()) ((Document)t.getUserData()).editor.setWrapText(wrap.isSelected());
        });
        Menu view = new Menu("Ver");
        view.getItems().addAll(wrap, item("Aumentar texto", "Shortcut+Equals", () -> zoom(1)), item("Reducir texto", "Shortcut+Minus", () -> zoom(-1)));
        Menu help = new Menu("Ayuda");
        help.getItems().add(item("Acerca de", null, () -> alert(Alert.AlertType.INFORMATION, "Uni Editor 2.0", "Editor de texto UTF-8 · JavaFX\nCtrl+N: nuevo · Ctrl+O: abrir · Ctrl+S: guardar")));
        BorderPane root = new BorderPane(tabs, new MenuBar(file, view, help), null, status, null);
        status.setStyle("-fx-padding: 10; -fx-text-fill: #a9bbd3;");
        Scene scene = new Scene(root, 1050, 720);
        scene.getStylesheets().add(EditorApp.class.getResource("editor.css").toExternalForm());
        stage.setTitle("Uni Editor"); stage.setMinWidth(640); stage.setMinHeight(420); stage.setScene(scene);
        tabs.getSelectionModel().selectedItemProperty().addListener((o, before, after) -> updateStatus());
        stage.setOnCloseRequest(e -> {
            for (Tab t : new ArrayList<>(tabs.getTabs())) if (!canClose((Document)t.getUserData())) { e.consume(); return; }
        });
        newDocument(); stage.show();
        for (String arg : getParameters().getRaw()) openPath(Path.of(arg));
    }

    private MenuItem item(String title, String shortcut, Runnable action) {
        MenuItem item = new MenuItem(title);
        if (shortcut != null) item.setAccelerator(KeyCombination.keyCombination(shortcut));
        item.setOnAction(e -> action.run()); return item;
    }
    private Document current() { Tab t = tabs.getSelectionModel().getSelectedItem(); return t == null ? null : (Document)t.getUserData(); }
    private void newDocument() { Document d = new Document(); tabs.getTabs().add(d.tab); tabs.getSelectionModel().select(d.tab); d.editor.requestFocus(); }
    private FileChooser chooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Todos los archivos", "*.*"), new FileChooser.ExtensionFilter("Texto", "*.txt", "*.md", "*.sql", "*.json", "*.java"));
        Document d = current();
        File dir = d != null && d.path != null ? d.path.toAbsolutePath().getParent().toFile() : new File(System.getProperty("user.dir"));
        if (dir.isDirectory()) chooser.setInitialDirectory(dir);
        return chooser;
    }
    private void open() { var files = chooser().showOpenMultipleDialog(window); if (files != null) files.forEach(f -> openPath(f.toPath())); }
    private void openPath(Path path) {
        try {
            Path real = path.toRealPath();
            for (Tab t : tabs.getTabs()) if (real.equals(((Document)t.getUserData()).path)) { tabs.getSelectionModel().select(t); return; }
            TextFiles.Content content = TextFiles.read(real);
            Document d = new Document(); d.path = real; d.newline = content.newline(); d.bom = content.bom();
            d.saved = content.text(); d.editor.setText(content.text()); d.update();
            tabs.getTabs().add(d.tab); tabs.getSelectionModel().select(d.tab);
        } catch (IOException | RuntimeException e) { error(e); }
    }
    private boolean save(Document d, boolean saveAs) {
        if (d == null) return false;
        Path target = d.path;
        if (target == null || saveAs) {
            FileChooser chooser = chooser(); chooser.setInitialFileName(target == null ? "documento.txt" : target.getFileName().toString());
            File file = chooser.showSaveDialog(window); if (file == null) return false; target = file.toPath().toAbsolutePath();
        }
        try {
            Path identity = Files.exists(target) ? target.toRealPath() : target;
            for (Tab t : tabs.getTabs()) {
                Document other = (Document)t.getUserData();
                if (other != d && identity.equals(other.path)) throw new IOException("El archivo está abierto en otra pestaña.");
            }
            TextFiles.write(target, new TextFiles.Content(d.editor.getText(), d.newline, d.bom));
            d.path = target.toRealPath(); d.saved = d.editor.getText(); d.update(); return true;
        } catch (IOException | RuntimeException e) { error(e); return false; }
    }
    private boolean canClose(Document d) {
        if (!d.dirty()) return true;
        ButtonType save = new ButtonType("Guardar"), discard = new ButtonType("Descartar"), cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert prompt = new Alert(Alert.AlertType.CONFIRMATION, "Hay cambios sin guardar en " + d.tab.getText(), save, discard, cancel);
        prompt.initOwner(window); prompt.setHeaderText("Guardar cambios");
        ButtonType result = prompt.showAndWait().orElse(cancel);
        return result == discard || result == save && save(d, false);
    }
    private void closeCurrent() { Document d = current(); if (d != null && canClose(d)) tabs.getTabs().remove(d.tab); }
    private void rename() {
        Document d = current(); if (d == null || d.path == null) return;
        TextInputDialog dialog = new TextInputDialog(d.path.getFileName().toString()); dialog.initOwner(window); dialog.setHeaderText("Nuevo nombre del archivo");
        dialog.showAndWait().ifPresent(name -> { try { d.path = TextFiles.rename(d.path, name); d.update(); } catch (IOException | RuntimeException e) { error(e); } });
    }
    private void delete() {
        Document d = current(); if (d == null || d.path == null) return;
        Alert prompt = new Alert(Alert.AlertType.CONFIRMATION, "Eliminar definitivamente " + d.path + "\nTambién se descartarán los cambios de esta pestaña.", ButtonType.OK, ButtonType.CANCEL);
        prompt.initOwner(window); prompt.setHeaderText("Eliminar archivo");
        if (prompt.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { Files.delete(d.path); tabs.getTabs().remove(d.tab); } catch (IOException e) { error(e); }
    }
    private void zoom(int step) { fontSize = Math.clamp(fontSize + step, 10, 40); tabs.getTabs().forEach(t -> ((Document)t.getUserData()).style()); }
    private void updateStatus() {
        Document d = current(); if (d == null) { status.setText("Listo · UTF-8"); return; }
        int caret = d.editor.getCaretPosition(); String before = d.editor.getText().substring(0, caret);
        long line = before.chars().filter(c -> c == '\n').count() + 1;
        status.setText("Línea " + line + " · Columna " + (caret - before.lastIndexOf('\n')) + " · UTF-8 · " + (d.newline.equals("\r\n") ? "CRLF" : d.newline.equals("\r") ? "CR" : "LF") + (d.dirty() ? " · Sin guardar" : " · Guardado"));
    }
    private void error(Exception e) { alert(Alert.AlertType.ERROR, "No se pudo completar la operación", e.getMessage()); }
    private void alert(Alert.AlertType type, String title, String text) { Alert a = new Alert(type, text); a.initOwner(window); a.setHeaderText(title); a.showAndWait(); }
    public static void main(String[] args) { launch(args); }
}
