package io.github.nickid2018.mcde.remapper;

import io.github.nickid2018.genwiki.*;
import io.github.nickid2018.mcde.format.MappingClassData;
import io.github.nickid2018.mcde.format.MappingFormat;
import io.github.nickid2018.mcde.util.ClassUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileProcessor {

    public static void processServer(File fileInput, MappingFormat remapper, File output) throws Exception {
        ZipFile file = new ZipFile(fileInput);
        String versionData = IOUtils.toString(
                file.getInputStream(file.getEntry("META-INF/versions.list")), StandardCharsets.UTF_8);
        String[] extractData = versionData.split("\t", 3);
        File tempZip = new File("temp-server.jar");
        File tempRemapped = new File("temp-server-remapped.jar");

        IOUtils.copy(file.getInputStream(file.getEntry("META-INF/versions/" + extractData[2])),
                new FileOutputStream(tempZip));
        checkIntegrity(tempZip.toPath(), extractData[0]);
        try (ZipFile server = new ZipFile(tempZip)) {
            process(server, remapper, tempRemapped, true);
        }
        tempZip.delete();

        String remappedHash = computeHash(tempRemapped.toPath());
        extractData[0] = remappedHash;
        versionData = String.join("\t", extractData);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
            for (Iterator<? extends ZipEntry> it = file.entries().asIterator(); it.hasNext(); ) {
                ZipEntry entry = it.next();
                zos.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals("META-INF/versions.list"))
                    zos.write(versionData.getBytes(StandardCharsets.UTF_8));
                else if (entry.getName().equals("META-INF/versions/" + extractData[2]))
                    IOUtils.copy(new FileInputStream(tempRemapped), zos);
                else
                    IOUtils.copy(file.getInputStream(entry), zos);
            }
        }
        tempRemapped.delete();
    }

    private static String computeHash(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream output = Files.newInputStream(file)) {
            output.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
            return byteToHex(digest.digest());
        }
    }

    private static void checkIntegrity(Path file, String expectedHash) throws Exception {
        String hash = computeHash(file);
        if (!hash.equals(expectedHash))
            throw new Exception("Hash mismatch! Expected: " + expectedHash + ", got: " + hash);
    }

    private static String byteToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(Character.forDigit(b >> 4 & 15, 16));
            result.append(Character.forDigit(b & 15, 16));
        }
        return result.toString();
    }

    public static void process(ZipFile file, MappingFormat remapper, File output, boolean server) throws Exception {
        addPlainClasses(file, remapper);
        generateInheritTree(file, remapper);
        runPack(output, remapAllClasses(file, remapper.getToNamedMapper(), remapper, server));
    }

    public static void addPlainClasses(ZipFile file, MappingFormat format) throws Exception {
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class"))
                continue;
            ClassReader reader = new ClassReader(IOUtils.toByteArray(file.getInputStream(entry)));
            String className = ClassUtils.toBinaryName(reader.getClassName());
            MappingClassData clazz = format.getToNamedClass(className);
            if (clazz == null) {
                ClassNode node = new ClassNode(Opcodes.ASM9);
                reader.accept(node, 0);
                clazz = new MappingClassData(className, className);
                format.addRemapClass(className, clazz);
                for (MethodNode mno : node.methods)
                    clazz.methodMappings.put(mno.name + mno.desc, mno.name);
                for (FieldNode flo : node.fields)
                    clazz.fieldMappings.put(flo.name + "+" + flo.desc, flo.name);
            }
        }
    }

    public static void generateInheritTree(ZipFile file, MappingFormat format) throws IOException {
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class"))
                continue;
            ClassReader reader = new ClassReader(IOUtils.toByteArray(file.getInputStream(entry)));
            MappingClassData clazz = format.getToNamedClass(ClassUtils.toBinaryName(reader.getClassName()));
            clazz.superClasses.add(format.getToNamedClass(ClassUtils.toBinaryName(reader.getSuperName())));
            for (String name : reader.getInterfaces())
                clazz.superClasses.add(format.getToNamedClass(ClassUtils.toBinaryName(name)));
        }
    }

    public static Map<String, byte[]> remapAllClasses(ZipFile file, ASMRemapper remapper, MappingFormat format, boolean server)
            throws IOException {
        Map<String, byte[]> remappedData = new HashMap<>();

        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            String nowFile;
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || entry.getName().startsWith("META-INF"))
                continue;
            byte[] bytes = IOUtils.toByteArray(file.getInputStream(entry));
            if (!(nowFile = entry.getName()).endsWith(".class")) {
                remappedData.put(nowFile, bytes);
                continue;
            }
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(0);
            String className = ClassUtils.toBinaryName(entry.getName());
            className = className.substring(0, className.length() - 6);
            reader.accept(new ClassRemapperFix(writer, remapper), 0);
            byte[] remapped = writer.toByteArray();
            String classNameRemapped = format.getToNamedClass(className).mapName();
            remapped = postTransform(classNameRemapped, remapped);
            remappedData.put(ClassUtils.toInternalName(classNameRemapped) + ".class",
                    remapped);
        }

        remappedData.put("META-INF/MANIFEST.MF",
                ("Manifest-Version: 1.0\r\nMain-Class: " +
                        (server ? "net.minecraft.server.Main" : "net.minecraft.client.main.Main")).getBytes());

        return remappedData;
    }

    public static byte[] postTransform(String className, byte[] classfileBuffer) {
        if (className.equals(Constants.INJECT_POINT_CLASS)) {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            boolean injected = false;
            for (int i = 0; i < node.methods.size(); i++) {
                MethodNode method = node.methods.get(i);
                if (method.name.equals(Constants.INJECT_POINT_METHOD) && method.desc.equals(Constants.INJECT_POINT_METHOD_DESC)) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/nickid2018/genwiki/inject/InjectedProcess", "onInjection", "(Ljava/lang/Object;)V", false));
                    method.instructions.insert(list);
                    injected = true;
                    break;
                }
            }
            if (!injected)
                throw new RuntimeException("Failed to inject!");
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        }

        if (className.equals(Constants.INJECT_SERVER_PROPERTIES)) {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            boolean injected = false;
            for (int i = 0; i < node.methods.size(); i++) {
                MethodNode method = node.methods.get(i);
                if (method.name.equals(Constants.INJECT_SERVER_PROPERTIES_METHOD) && method.desc.equals(Constants.INJECT_SERVER_PROPERTIES_METHOD_DESC)) {
                    InsnList list = new InsnList();
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/nickid2018/genwiki/inject/InjectedProcess", "preprocessDataPacks", "()Ljava/lang/String;", false));
                    list.add(new VarInsnNode(Opcodes.ASTORE, 0));
                    method.instructions.insert(list);
                    injected = true;
                    break;
                }
            }
            if (!injected)
                throw new RuntimeException("Failed to inject!");
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        }

        return classfileBuffer;
    }

    public static void runPack(File dest, Map<String, byte[]> map) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dest.toPath()));
        for (String entry : map.keySet()) {
            ZipEntry zipEntry = new ZipEntry(entry);
            zos.putNextEntry(zipEntry);
            zos.write(map.get(entry));
        }

        JarFile thisJar = new JarFile(FileProcessor.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        for (Enumeration<? extends ZipEntry> it = thisJar.entries(); it.hasMoreElements(); ) {
            ZipEntry entry = it.nextElement();
            if (entry.getName().contains("inject")) {
                zos.putNextEntry(new ZipEntry(entry.getName()));
                IOUtils.copy(thisJar.getInputStream(entry), zos);
            }
        }

        zos.close();
    }
}
