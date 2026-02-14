package io.github.nickid2018.mock;

import io.github.nickid2018.easymock.EasyMockGenerator;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.internal.hash.Hashing;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Mocker {

    private final Project project;

    public Mocker(Project project) {
        this.project = project;
    }

    private File getMockDescFile(String path) {
        ProjectLayout layout = project.getLayout();
        RegularFile resolved = layout.getProjectDirectory().file(path);
        return resolved.getAsFile();
    }

    public FileCollection mock(String path) throws IOException {
        File descFile = getMockDescFile(path);
        String fileName = descFile.getName();
        String content = IOUtils.toString(new FileInputStream(descFile), StandardCharsets.UTF_8);
        String hash = Hashing.sha1().hashString(content).toString();

        Provider<RegularFile> resolved = project.getRootProject().getLayout().getBuildDirectory().file("mock/" + fileName + "-" + hash + ".jar");
        if (resolved.isPresent() && !resolved.get().getAsFile().exists()) {
            File parent = resolved.get().getAsFile().getParentFile();
            if (!parent.isDirectory()) {
                parent.mkdirs();
            }

            Map<String, ClassNode> classNodes = EasyMockGenerator.generate(content);
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(resolved.get().getAsFile()))) {
                for (Map.Entry<String, ClassNode> entry : classNodes.entrySet()) {
                    ZipEntry zipEntry = new ZipEntry(entry.getKey());
                    zos.putNextEntry(zipEntry);
                    ClassWriter writer = new ClassWriter(0);
                    entry.getValue().accept(writer);
                    zos.write(writer.toByteArray());
                    zos.closeEntry();
                }
            }
        }

        return project.files(resolved);
    }
}
