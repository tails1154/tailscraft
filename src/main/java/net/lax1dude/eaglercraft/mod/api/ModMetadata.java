package net.lax1dude.eaglercraft.mod.api;

import java.util.ArrayList;
import java.util.List;

public class ModMetadata {

    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String icon;
    private final List<String> dependencies;
    private final List<String> documentationPages;

    public ModMetadata(String name, String version, String author) {
        this(name, version, author, "", "", new ArrayList<>(), new ArrayList<>());
    }

    public ModMetadata(String name, String version, String author, String description,
                       String icon, List<String> dependencies, List<String> documentationPages) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.icon = icon;
        this.dependencies = dependencies;
        this.documentationPages = documentationPages;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public List<String> getDependencies() { return dependencies; }
    public List<String> getDocumentationPages() { return documentationPages; }

    public static class Builder {
        private String name;
        private String version;
        private String author;
        private String description = "";
        private String icon = "";
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> documentationPages = new ArrayList<>();

        public Builder(String name, String version, String author) {
            this.name = name;
            this.version = version;
            this.author = author;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder dependency(String modName) {
            this.dependencies.add(modName);
            return this;
        }

        public Builder documentationPage(String page) {
            this.documentationPages.add(page);
            return this;
        }

        public ModMetadata build() {
            return new ModMetadata(name, version, author, description, icon, dependencies, documentationPages);
        }
    }
}
