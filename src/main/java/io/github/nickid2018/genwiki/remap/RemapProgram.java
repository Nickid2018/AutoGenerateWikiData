package io.github.nickid2018.genwiki.remap;

import com.google.common.hash.Hashing;
import io.github.nickid2018.genwiki.util.ClassUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Setter
@RequiredArgsConstructor
public class RemapProgram {

    public static final String META_INF_VERSIONS = "META-INF/versions.list";
    public static final File TEMP_ZIP_SERVER = new File("temp-server.jar");
    public static final File TEMP_REMAPPED_SERVER = new File("temp-server-remapped.jar");

    private final Map<String, Set<PostTransform>> postTransforms = new HashMap<>();
    private final Set<InjectEntries> injectEntries = new HashSet<>();
    @Getter
    private final MojangMapping mapping;
    @Getter
    private final File inputFile;
    @Getter
    private final File outputFile;
    @Getter
    private File serverFile = TEMP_ZIP_SERVER;
    @Getter
    private File remappedFile = TEMP_REMAPPED_SERVER;
    private String[] extractData;

    public void addPostTransform(String clazz, PostTransform transform) {
        postTransforms.computeIfAbsent(clazz, k -> new HashSet<>()).add(transform);
    }

    public void addInjectEntries(InjectEntries entries) {
        injectEntries.add(entries);
    }

    public void extractServer() throws IOException {
        try (ZipFile file = new ZipFile(inputFile)) {
            String versionData = IOUtils.toString(
                file.getInputStream(file.getEntry(META_INF_VERSIONS)),
                StandardCharsets.UTF_8
            );

            extractData = versionData.split("\t", 3);
            IOUtils.copy(
                file.getInputStream(file.getEntry("META-INF/versions/" + extractData[2])),
                new FileOutputStream(serverFile)
            );

            String serverHash = Hashing.sha256().hashBytes(Files.readAllBytes(serverFile.toPath())).toString();
            if (!serverHash.equals(extractData[0]))
                throw new IOException("Server file hash not match!");
        }
    }

    public void fillRemapFormat() throws IOException {
        try (ZipFile file = new ZipFile(serverFile)) {
            Map<String, ClassNode> classNodes = new HashMap<>();

            for (ZipEntry e : file.stream().filter(e -> !e.isDirectory() && e.getName().endsWith(".class")).toList()) {
                ClassReader reader = new ClassReader(IOUtils.toByteArray(file.getInputStream(e)));
                String className = ClassUtils.toBinaryName(reader.getClassName());
                ClassNode node = new ClassNode(Opcodes.ASM9);
                reader.accept(node, ClassReader.SKIP_CODE);
                classNodes.put(className, node);
            }

            for (Map.Entry<String, ClassNode> entry : classNodes.entrySet()) {
                String className = entry.getKey();
                MappingClassData clazz = mapping.getToNamedClass(className);
                if (clazz != null)
                    continue;
                ClassNode node = entry.getValue();
                clazz = new MappingClassData(className, className);
                for (MethodNode mno : node.methods)
                    clazz.methodMappings.put(mno.name + mno.desc, mno.name);
                for (FieldNode flo : node.fields)
                    clazz.fieldMappings.put(flo.name + "+" + flo.desc, flo.name);
                mapping.addRemapClass(className, clazz);
            }

            for (Map.Entry<String, ClassNode> entry : classNodes.entrySet()) {
                ClassNode node = entry.getValue();
                MappingClassData clazz = mapping.getToNamedClass(entry.getKey());
                clazz.superClasses.add(mapping.getToNamedClass(ClassUtils.toBinaryName(node.superName)));
                for (String name : node.interfaces)
                    clazz.superClasses.add(mapping.getToNamedClass(ClassUtils.toBinaryName(name)));
            }
        }
    }

    public void remapClasses() throws IOException {
        Map<String, byte[]> remappedData = new HashMap<>();
        try (ZipFile server = new ZipFile(serverFile)) {
            List<? extends ZipEntry> entries = server
                .stream()
                .filter(e -> !e.isDirectory() && !e.getName().startsWith("META-INF"))
                .toList();
            for (ZipEntry entry : entries) {
                String nowFile = entry.getName();
                byte[] bytes = IOUtils.toByteArray(server.getInputStream(entry));
                if (!nowFile.endsWith(".class")) {
                    remappedData.put(nowFile, bytes);
                    continue;
                }

                String className = ClassUtils.toBinaryName(entry.getName());
                className = className.substring(0, className.length() - 6);
                String classNameRemapped = mapping.getToNamedClass(className).mapName();

                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, mapping.remapper), 0);

                if (postTransforms.containsKey(classNameRemapped)) {
                    ClassNode node = new ClassNode(Opcodes.ASM9);
                    ClassReader transformed = new ClassReader(writer.toByteArray());
                    transformed.accept(node, 0);
                    postTransforms.get(classNameRemapped).forEach(transform -> transform.transform(node));
                    writer = new ClassWriter(0);
                    node.accept(writer);
                }

                remappedData.put(ClassUtils.toInternalName(classNameRemapped) + ".class", writer.toByteArray());
            }

            remappedData.put(
                "META-INF/MANIFEST.MF",
                "Manifest-Version: 1.0\r\nMain-Class: net.minecraft.server.Main".getBytes()
            );
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(remappedFile))) {
            for (String entry : remappedData.keySet()) {
                ZipEntry zipEntry = new ZipEntry(entry);
                zos.putNextEntry(zipEntry);
                zos.write(remappedData.get(entry));
            }
            for (InjectEntries entries : injectEntries) {
                Map<String, byte[]> injectData = entries.getInjectEntries();
                for (String entry : injectData.keySet()) {
                    ZipEntry zipEntry = new ZipEntry(entry);
                    zos.putNextEntry(zipEntry);
                    zos.write(injectData.get(entry));
                }
            }
        }
    }

    public void rePackServer() throws IOException {
        try (
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile));
            ZipFile input = new ZipFile(inputFile)
        ) {
            byte[] remappedData = Files.readAllBytes(remappedFile.toPath());
            String hash = Hashing.sha256().hashBytes(remappedData).toString();
            String versionData = hash + "\t" + extractData[1] + "\t" + extractData[2];

            for (ZipEntry entry : input.stream().toList()) {
                zos.putNextEntry(new ZipEntry(entry.getName()));

                String name = entry.getName();
                if (name.equals("META-INF/versions.list")) {
                    zos.write(versionData.getBytes(StandardCharsets.UTF_8));
                } else if (name.equals("META-INF/versions/" + extractData[2])) {
                    zos.write(remappedData);
                    remappedData = new byte[0];
                } else {
                    IOUtils.copy(input.getInputStream(entry), zos);
                }
            }
        }
    }
}
