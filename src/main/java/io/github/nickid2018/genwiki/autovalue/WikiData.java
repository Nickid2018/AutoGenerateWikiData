package io.github.nickid2018.genwiki.autovalue;

import io.github.nickid2018.genwiki.inject.InjectionConstant;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public interface WikiData {

    static void write(WikiData data, String file) throws IOException {
        File outputFile = new File(InjectionConstant.OUTPUT_FOLDER, file);
        FileUtils.write(outputFile, data.output(1), "UTF-8");
    }

    static void write(WikiData data, ExceptData exceptData, String file) throws IOException {
        File outputFile = new File(InjectionConstant.OUTPUT_FOLDER, file);
        FileUtils.write(outputFile, data.output(1) + "\n=== Except Data ===\n" + exceptData.output(), "UTF-8");
    }

    String output(int indent);
}
