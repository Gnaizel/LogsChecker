package ru.gnaizel.model;

public class CheckCommandState {
    private String url;
    private String file;
    private int step = 0; // 0: ждем URL, 1: ждем файл, 2: все получено

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}