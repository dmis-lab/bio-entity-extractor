import java.util.ArrayList;
import java.util.HashSet;
import org._dmis.object.BioEntityExtractor;
import org._dmis.object.BioEntityInfo;
import org._dmis.object.BioEntityType;

public class BioEntityExtractTest {
    public static void main(String[] args) {
        String dictPath = "src/main/resources/dictionary.txt";
        BioEntityExtractor bee = new BioEntityExtractor(dictPath);

        String text = "Lapatinib inhibits cell growth in cell lines.";

        HashSet<BioEntityInfo> entitySet = bee.extractEntities(text);

        for(BioEntityInfo entity : entitySet) {
            System.out.println("ID: " + entity.getIdStr() + ", Name: " + entity.getName());

            ArrayList<BioEntityType> types = entity.getTypes();

            for(BioEntityType bet : types) {
                System.out.print("\t" + bet.getName4EndUser());
            }
            System.out.println();
        }
    }
}