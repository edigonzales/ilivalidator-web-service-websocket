package ch.so.agi.ilivalidator;

import java.io.File;

public class FileTuple {
    String name;
    File file;
    
    public FileTuple() {}
    
    public FileTuple(String name, File file) {
        this.name = name;
        this.file = file;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
    @Override
    public String toString() {
        return "FileTuple [name=" + name + ", file=" + file + "]";
    }
}
