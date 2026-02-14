package io.github.nickid2018.easymock;

import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static io.github.nickid2018.easymock.Constants.*;
import static org.objectweb.asm.Opcodes.*;

public class EasyMockGenerator {
    private static String convertSimpleName(String name, String packageName) {
        if (name == null) return null;
        if (name.startsWith("-")) name = packageName + name.substring(1);
        return name.replace('.', '/');
    }

    private static String convertName(String name, String packageName) {
        if (name == null) return null;
        int arrays = 0;
        while (name.startsWith("[")) {
            arrays++;
            name = name.substring(1);
        }
        if (name.length() == 1) return "[".repeat(arrays) + name;
        if (name.startsWith("-")) name = packageName + name.substring(1);
        return "[".repeat(arrays) + "L" + name.replace('.', '/') + ";";
    }

    private static String convertDesc(String args, String ret, String packageName) {
        String argList;
        if (args == null)
            argList = "";
        else
            argList = Arrays.stream(args.split(",")).map(s -> convertName(s, packageName)).collect(Collectors.joining());
        return "(" + argList + ")" + convertName(ret, packageName);
    }

    private static int makeClassAccess(String type, boolean inner) {
        int access = ACC_PUBLIC;
        if (type == null) return access;
        switch (type.trim()) {
            case "abstract":
                access += ACC_ABSTRACT;
                break;
            case "enum":
                access += ACC_ENUM + ACC_FINAL;
                break;
            case "interface":
                access += ACC_ABSTRACT + ACC_INTERFACE;
                break;
            case "record":
                access += ACC_RECORD + ACC_FINAL;
                break;
        }
        if (inner) {
            access &= ~ACC_RECORD;
        } else {
            access += ACC_SUPER;
        }
        return access;
    }

    public static Map<String, ClassNode> generate(String content) {
        GeneratorState state = new GeneratorState();
        Map<String, ClassNode> classNodes = new HashMap<>();

        Arrays.stream(content.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).forEach(c -> {
            Matcher matcher;

            if ((matcher = PACKAGE_INST.matcher(c)).matches()) {
                state.packageName = matcher.group(1).replace('.', '/');
                return;
            }

            if (state.packageName == null)
                throw new RuntimeException("Control flow error: no package name specified");

            if ((matcher = CLASS_INST.matcher(c)).matches()) {
                ClassNode classNode = new ClassNode();
                classNode.name = state.packageName + "/" + matcher.group(2);
                classNode.access = makeClassAccess(matcher.group(1), false);
                classNode.version = V21;
                classNode.superName = convertSimpleName(matcher.group(3), state.packageName);
                if (matcher.group(1).equals("enum")) {
                    classNode.superName = "java/lang/Enum";
                }
                if (classNode.superName == null) {
                    classNode.superName = "java/lang/Object";
                }
                if (matcher.group(4) != null) {
                    classNode.interfaces = Arrays.stream(matcher.group(4).split(",")).map(s -> convertSimpleName(s, state.packageName)).toList();
                }
                if (matcher.group(5) != null) {
                    classNode.signature = matcher.group(5).trim();
                }

                state.classNode = classNode;
                classNodes.put(state.packageName + "/" + matcher.group(2) + ".class", classNode);

                return;
            }

            if (state.classNode == null)
                throw new RuntimeException("Control flow error: no class specified");

            String className = state.classNode.name;

            if ((matcher = INNERCLASS_INST.matcher(c)).matches()) {
                String name = matcher.group(1);
                int access = makeClassAccess(matcher.group(2), true);
                if (matcher.group(3) != null) {
                    access += ACC_STATIC;
                }
                state.classNode.innerClasses.add(new InnerClassNode(className + "$" + name, className, name, access));
                if (state.classNode.nestMembers == null) state.classNode.nestMembers = new ArrayList<>();
                state.classNode.nestMembers.add(className + "$" + name);
                return;
            }

            if ((matcher = VIS_INNERCLASS_INST.matcher(c)).matches()) {
                String name = convertSimpleName(matcher.group(1), state.packageName);
                int split = name.lastIndexOf('$');
                int access = makeClassAccess(matcher.group(2), true);
                if (matcher.group(3) != null) {
                    access += ACC_STATIC;
                }
                state.classNode.innerClasses.add(new InnerClassNode(name, name.substring(0, split), name.substring(split + 1), access));
                return;
            }

            if ((matcher = OUTERCLASS_INST.matcher(c)).matches()) {
                int split = className.lastIndexOf('$');
                int access = makeClassAccess(matcher.group(1), true);
                if (matcher.group(2) != null) {
                    access += ACC_STATIC;
                }
                state.classNode.innerClasses.add(new InnerClassNode(className, state.classNode.outerClass, className.substring(split + 1), access));
                state.classNode.nestHostClass = state.classNode.outerClass;
                return;
            }

            if ((matcher = ECONST_INST.matcher(c)).matches()) {
                state.classNode.fields.add(new FieldNode(
                        ACC_PUBLIC + ACC_STATIC + ACC_FINAL + ACC_ENUM, matcher.group(1), "L" + className + ";", null, null
                ));
                return;
            }

            if ((matcher = FIELD_INST.matcher(c)).matches()) {
                int access = switch (matcher.group(1)) {
                    case "final" -> ACC_PUBLIC + ACC_FINAL;
                    case "static" -> ACC_PUBLIC + ACC_STATIC;
                    case "const" -> ACC_PUBLIC + ACC_STATIC + ACC_FINAL;
                    default -> ACC_PUBLIC;
                };
                FieldNode fieldNode = new FieldNode(access, matcher.group(2), convertName(matcher.group(3), state.packageName), null, null);
                state.classNode.fields.add(fieldNode);
                if (matcher.group(4) != null) {
                    fieldNode.signature = matcher.group(4).trim();
                }
                return;
            }

            if ((matcher = COMPONENT_INST.matcher(c)).matches()) {
                FieldNode fieldNode = new FieldNode(ACC_PRIVATE + ACC_FINAL, matcher.group(1), convertName(matcher.group(2), state.packageName), null, null);
                state.classNode.fields.add(fieldNode);
                String methodSig = null;
                if (matcher.group(3) != null) {
                    fieldNode.signature = matcher.group(3).trim();
                    methodSig = "()" + matcher.group(3).trim();
                }
                if (state.classNode.recordComponents == null) state.classNode.recordComponents = new ArrayList<>();
                state.classNode.recordComponents.add(new RecordComponentNode(fieldNode.name, fieldNode.desc, fieldNode.signature));
                state.classNode.methods.add(new MethodNode(ACC_PUBLIC, fieldNode.name, "()" + fieldNode.desc, methodSig, null));
                return;
            }

            if ((matcher = METHOD_INST.matcher(c)).matches()) {
                int access = switch (matcher.group(1)) {
                    case "virtual" -> ACC_PUBLIC + ACC_ABSTRACT;
                    case "function" -> ACC_PUBLIC + ACC_STATIC;
                    default -> ACC_PUBLIC;
                };
                MethodNode methodNode = new MethodNode(
                        access, matcher.group(2), convertDesc(matcher.group(3), matcher.group(4), state.packageName), null, null
                );
                state.classNode.methods.add(methodNode);
                if (matcher.group(5) != null) {
                    methodNode.signature = matcher.group(5).trim();
                }
                if (matcher.group(6) != null) {
                    methodNode.exceptions = Arrays.stream(matcher.group(6).split(",")).map(s -> convertSimpleName(s, state.packageName)).toList();
                }
                return;
            }

            throw new RuntimeException("Unknown instruction: " + c);
        });

        return classNodes;
    }
}
