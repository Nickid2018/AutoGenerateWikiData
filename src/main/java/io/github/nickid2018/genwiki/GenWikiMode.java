package io.github.nickid2018.genwiki;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GenWikiMode {

    AUTOVALUE(false),
    STATISTICS(false),
    ISO(true),
    CLIENT_DATA(true);

    public final boolean isClient;
}
