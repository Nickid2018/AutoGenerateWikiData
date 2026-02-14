package io.github.nickid2018.easymock;

import java.util.regex.Pattern;

public class Constants {
    public static final Pattern PACKAGE_INST = Pattern.compile("@package ([.0-9a-zA-Z_]+)");
    public static final Pattern CLASS_INST = Pattern.compile("@(abstract|enum|interface|class|record) ([a-zA-Z_$][0-9a-zA-Z_$]*)(?: extends ([-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*))?(?: impl ([-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*(?:,[-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*)*))?( [.0-9a-zA-Z_$<>;/*+:()\\[]+)?");
    public static final Pattern INNERCLASS_INST = Pattern.compile("@innerclass ([a-zA-Z_][0-9a-zA-Z_]*)( (?:abstract|enum|interface|class|record))?( static)?");
    public static final Pattern VIS_INNERCLASS_INST = Pattern.compile("@visitinner ([-a-zA-Z_][.0-9a-zA-Z_$]*)( (?:abstract|enum|interface|class|record))?( static)?");
    public static final Pattern OUTERCLASS_INST = Pattern.compile("@outerclass( (?:abstract|enum|interface|class|record))?( static)?");
    public static final Pattern ECONST_INST = Pattern.compile("@econst ([a-zA-Z_][0-9a-zA-Z_]*)");
    public static final Pattern FIELD_INST = Pattern.compile("@(field|static|final|const) ([a-zA-Z_][0-9a-zA-Z_]*) (\\[*[-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*)( [.0-9a-zA-Z_$<>;/*+:()\\[]+)?");
    public static final Pattern COMPONENT_INST = Pattern.compile("@component ([a-zA-Z_][0-9a-zA-Z_]*) (\\[*[-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*)( [.0-9a-zA-Z_$<>;/*+:()\\[]+)?");
    public static final Pattern METHOD_INST = Pattern.compile("@(method|virtual|function) ([a-zA-Z_<][0-9a-zA-Z_]*>?)\\((\\[*[-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*(?:,\\[*[-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*)*)?\\) (\\[*[-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*)( [.0-9a-zA-Z_$<>;/*+:()\\[]+)?(?: throws ([-.0-9a-zA-Z_$][.0-9a-zA-Z_$]*(?:,[-.0-9a-zA-Z_$][.0-9a-zA-Z_$])*))?");
}
