package io.github.nickid2018.genwiki.iso;

import org.joml.Matrix4f;
import org.joml.Vector3f;

@SuppressWarnings("unused")
public class ISOInjectionEntryPoints {

    public static void clampColorInjection(Vector3f vector3f) {
        vector3f.set(1, 1, 1);
    }

    private static boolean ortho = false;
    private static int invokeCount = 0;
    private static Matrix4f orthoMatrix = new Matrix4f().ortho(-2, 2, -2, 2, -0.1f, 1000);

    public static Matrix4f getProjectionMatrixInjection(Matrix4f source) {
        invokeCount = (invokeCount + 1) % 3;
        if (invokeCount == 2) // Avoid Frustum Culling
            return source;
        if (ortho)
            return orthoMatrix;
        return source;
    }

    public static void handleChat(String chat) {
        chat = chat.toLowerCase();
        switch (chat) {
            case "psp" -> ortho = false;
            case "ortho" -> ortho = true;
        }
    }
}
