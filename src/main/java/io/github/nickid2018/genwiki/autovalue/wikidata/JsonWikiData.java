package io.github.nickid2018.genwiki.autovalue.wikidata;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import lombok.SneakyThrows;

import java.io.StringWriter;

public abstract class JsonWikiData implements WikiData {

    public abstract JsonElement asJsonData();

    @Override
    @SneakyThrows
    public String output(int indent) {
        StringWriter sw = new StringWriter();
        JsonWriter jw = new JsonWriter(sw);
        jw.setIndent("    ");
        Streams.write(asJsonData(), jw);
        return sw.toString();
    }
}
