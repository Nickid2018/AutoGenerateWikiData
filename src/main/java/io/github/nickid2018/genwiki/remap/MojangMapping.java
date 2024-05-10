package io.github.nickid2018.genwiki.remap;

import lombok.Getter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Getter
public class MojangMapping {

    protected final Map<String, MappingClassData> remaps = new HashMap<>();
    public final Map<String, String> revClass = new HashMap<>();

    protected ASMRemapper remapper;

    public MojangMapping(InputStream stream) throws IOException {
        remapper = new ASMRemapper(remaps);
        byte[] bytes = stream.readAllBytes();
        readClassTokens(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
        readComponents(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes))));
    }

    public void addRemapClass(String name, MappingClassData clazz) {
        remaps.put(name, clazz);
    }

    public MappingClassData getToNamedClass(String source) {
        return remaps.getOrDefault(source, new MappingClassData(source, source));
    }

    private void readClassTokens(BufferedReader reader) throws IOException {
        String nowClass;
        String toClass;
        String nowStr;
        while ((nowStr = reader.readLine()) != null) {
            if (nowStr.startsWith("#"))
                continue;
            String now = nowStr.trim();
            if (!nowStr.startsWith(" ")) {
                // Class
                now = now.substring(0, now.length() - 1);
                String[] splits = now.split(" -> ");
                nowClass = splits[0];
                toClass = splits[1];
                remaps.put(toClass, new MappingClassData(toClass, nowClass));
                revClass.put(nowClass, toClass);
            }
        }
    }

    private void readComponents(BufferedReader reader) throws IOException {
        String toClass;
        String nowStr;
        MappingClassData nowClass = new MappingClassData("", "");
        while ((nowStr = reader.readLine()) != null) {
            if (nowStr.startsWith("#"))
                continue;
            String now = nowStr.trim();
            if (nowStr.startsWith(" ")) {
                if (now.indexOf('(') >= 0) {
                    // Function
                    String[] ssource = now.split(":", 3);
                    String[] splits = ssource[ssource.length - 1].trim().split(" -> ");
                    StringBuilder nowTo = new StringBuilder(splits[1] + "(");
                    String[] descs = splits[0].split(" ");
                    String[] argss = descs[1].split("[()]");
                    if (argss.length == 2) {
                        String[] args = argss[1].split(",");
                        for (String a : args)
                            nowTo.append(mapSignature(a, revClass));
                    }
                    nowTo.append(")");
                    nowTo.append(mapSignature(descs[0], revClass));
                    String source = nowTo.toString().trim();
                    String to = descs[1].split("\\(")[0].trim();
                    nowClass.methodMappings.put(source, to);
                } else {
                    // Field
                    String[] splits = now.trim().split(" -> ");
                    String source = splits[1];
                    String to = splits[0].split(" ")[1];
                    nowClass.fieldMappings.put(source + "+" + mapSignature(splits[0].split(" ")[0], revClass), to);
                }
            } else {
                // Class
                now = now.substring(0, now.length() - 1);
                String[] splits = now.split(" -> ");
                toClass = splits[1];
                nowClass = remaps.get(toClass);
            }
        }
    }

    public static String mapSignature(String str, Map<String, String> revClass) {
        if (str.indexOf('[') >= 0) {
            String[] sp = str.split("\\[");
            return "[".repeat(sp.length - 1) + mapSignature(sp[0], revClass);
        }
        return switch (str) {
            case "int" -> "I";
            case "float" -> "F";
            case "double" -> "D";
            case "long" -> "J";
            case "boolean" -> "Z";
            case "short" -> "S";
            case "byte" -> "B";
            case "char" -> "C";
            case "void" -> "V";
            default -> "L" + revClass.getOrDefault(str, str).replace('.', '/') + ";";
        };
    }
}
