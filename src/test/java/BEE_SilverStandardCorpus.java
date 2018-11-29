import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org._dmis.object.BioEntityExtractor;
import org._dmis.object.BioEntityInfo;
import org._dmis.object.BioEntityType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class BEE_SilverStandardCorpus {
    public BEE_SilverStandardCorpus() {}

    private static void extract(String inputDir, String outputDir) throws IOException {
        File out = new File(outputDir);
        if (!out.exists()) {
            if (out.mkdirs()) {
                System.out.println("Made " + outputDir);
            }
        }

        final String dictPath = "src/main/resources/dictionary.txt";
        BioEntityExtractor bee = new BioEntityExtractor(dictPath);

        Gson gson = new Gson();
        final Charset charset = Charset.forName("UTF-8");

        int lineCnt = 0;
        int hasEntityArticleCnt = 0;
        int idxErrs = 0;

        HashMap<String, Integer> typeFreq = new HashMap<>();

        Stack<Path> stack = new Stack<>();
        stack.push(Paths.get(inputDir));
        while (!stack.isEmpty()) {
            Path child = stack.pop();
            if (Files.isDirectory(child)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(child)) {
                    for (Path path : stream) {
                        stack.push(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println(child);

                Path outPath = Paths.get(outputDir, child.getFileName().toString());
                if (Files.exists(outPath)) {
                    System.out.println("Already exists: " + outPath.getFileName());
                    continue;
                }

                BufferedWriter writer = Files.newBufferedWriter(outPath, charset);
                BufferedReader reader = Files.newBufferedReader(child, charset);
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCnt++;

                    JsonObject obj = gson.fromJson(line, JsonObject.class);

                    String title = obj.get("title").getAsString();
                    String abstractText = obj.get("abstract").getAsString();
                    String titleSpaceAbstract = title + " " + abstractText;

                    HashSet<BioEntityInfo> entitySet;
                    try {
                        entitySet = bee.extractEntities(titleSpaceAbstract);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // e.printStackTrace();
                        idxErrs++;
                        continue;
                    }

                    if (entitySet.size() > 0) {
                        hasEntityArticleCnt++;

                        HashMap<String, ArrayList<Entity>> entityMap = new HashMap<>();

                        for (BioEntityInfo entity : entitySet) {

                            // span
                            int startOffset = entity.getStart();
                            int endOffset = startOffset + entity.getName().length() - 1; // CollaboNet-STMs
                            assert entity.getName().equals(titleSpaceAbstract.substring(startOffset, endOffset + 1));

                            ArrayList<BioEntityType> types = entity.getTypes();
                            for (BioEntityType type: types) {
                                String typeName = type.name().toLowerCase();
                                ArrayList<Entity> entities = entityMap.get(typeName);
                                if (entities == null) {
                                    entities = new ArrayList<>();
                                    entities.add(new Entity(startOffset, endOffset));
                                    entityMap.put(typeName, entities);
                                } else {
                                    entities.add(new Entity(startOffset, endOffset));
                                }

                                Integer freq = typeFreq.get(typeName);
                                if (freq == null) {
                                    typeFreq.put(typeName, 1);
                                } else {
                                    typeFreq.put(typeName, freq + 1);
                                }
                            }
                        }

                        JsonObject object = new JsonObject();
                        object.addProperty("pmid", obj.get("pmid").getAsString());
                        object.addProperty("title", title);
                        object.addProperty("abstract", abstractText);

                        object.addProperty("ssc_model", "BEST Entity Extractor (BEE)");
                        JsonObject entityObject = new JsonObject();
                        Set<String> entityTypes = entityMap.keySet();
                        for (String entityType : entityTypes) {
                            entityObject.add(entityType,
                                    gson.fromJson(gson.toJson(entityMap.get(entityType)), JsonElement.class));
                        }
                        object.add("entities", entityObject);

                        writer.write(gson.toJson(object));
                        writer.newLine();
                    }
                }

                System.out.println(hasEntityArticleCnt + "/" + lineCnt + "\t" + idxErrs);

                reader.close();
                writer.close();
            }
        }

        Set<String> types = typeFreq.keySet();
        for (String entityType : types) {
            System.out.println(entityType + "\t" + typeFreq.get(entityType));
        }
    }

    private static class Entity {
        int start;
        int end;

        private Entity(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    public static void main(String[] args) throws IOException {
        String inputDir = "YOUR_PUBMED_JSON_INPUT_DIR";
        String outputDir = "OUTPUT_DIR";
        extract(inputDir, outputDir);
    }
}