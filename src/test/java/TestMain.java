import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.TreeMap;

public class TestMain {

    public static void main(String[] args) throws Exception {
        JsonObject element = JsonParser.parseReader(new FileReader("D:\\Minecraft\\.minecraft\\assets\\objects\\6d\\6d20059360ea271460e8845d3717b8c9607128f3")).getAsJsonObject();
        Map<String, String> mapping = new TreeMap<>();
        for (String key : element.keySet()) {
            if (key.startsWith("block.minecraft")) {
                String id = key.substring(16);
                if (id.contains("."))
                    continue;
                String val = element.get(key).getAsString();
                mapping.put(id, val);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            builder.append("\t['").append(entry.getValue()).append("'] = '").append(entry.getKey()).append("',\n");
        }
        FileUtils.write(new File("D:\\id.txt"), builder.toString(), "UTF-8");
    }
}
