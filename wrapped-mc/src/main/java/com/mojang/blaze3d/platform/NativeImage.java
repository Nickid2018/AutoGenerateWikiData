package com.mojang.blaze3d.platform;

import java.io.IOException;
import java.nio.file.Path;

public class NativeImage {

    public NativeImage(int n, int n2, boolean bl) {
    }

    public void downloadTexture(int n, boolean bl) {
        throw new RuntimeException();
    }

    public void flipY() {
        throw new RuntimeException();
    }

    public int getPixelRGBA(int n, int n2) {
        throw new RuntimeException();
    }

    public void writeToFile(Path path) throws IOException {
        throw new RuntimeException();
    }

    public void setPixelABGR(int n, int n2, int n3) {
        throw new RuntimeException();
    }
}
