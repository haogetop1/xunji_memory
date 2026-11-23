import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssembleApp {
    private static final String PREFIX = "Lcom/xunji/";
    private static final Opcodes OPCODES = Opcodes.forDexVersion(38);

    public static void main(String[] args) throws Exception {
        Path smali = Path.of(args[0]);
        Path original = Path.of(args[1]);
        Path output = Path.of(args[2]);
        int count = 0;
        try (var directories = java.nio.file.Files.list(smali)) {
            for (Path directory : directories.filter(java.nio.file.Files::isDirectory).sorted().toList()) {
                String name = directory.getFileName() + ".dex";
                count += rebuildDex(directory, original.resolve(name), output.resolve(name));
            }
        }
        System.out.printf("Assembled and reinserted %d application classes.%n", count);
    }

    private static int rebuildDex(Path directory, Path original, Path output) throws Exception {
        File appOnly = File.createTempFile("xunji-app-", ".dex", output.getParent().toFile());
        try {
            var options = new SmaliOptions();
            options.apiLevel = 26;
            options.jobs = 3;
            options.outputDexFile = appOnly.getPath();
            if (!Smali.assemble(options, directory.toString())) {
                throw new IllegalStateException("Smali assembly failed for " + directory.getFileName());
            }

            Map<String, ClassDef> updates = new HashMap<>();
            for (ClassDef definition : DexFileFactory.loadDexFile(appOnly, OPCODES).getClasses()) {
                if (!definition.getType().startsWith(PREFIX)) {
                    throw new IllegalStateException("Unexpected package " + definition.getType());
                }
                updates.put(definition.getType(), definition);
            }

            Set<String> expected = new HashSet<>();
            var pool = new DexPool(OPCODES);
            for (ClassDef originalClass : DexFileFactory.loadDexFile(original.toFile(), OPCODES).getClasses()) {
                String type = originalClass.getType();
                if (!type.startsWith(PREFIX)) {
                    pool.internClass(originalClass);
                    continue;
                }
                expected.add(type);
                ClassDef replacement = updates.get(type);
                if (replacement == null) throw new IllegalStateException("Missing Smali class " + type);
                pool.internClass(replacement);
            }
            if (!updates.keySet().equals(expected)) {
                throw new IllegalStateException("Application class inventory changed for " + original.getFileName());
            }
            var store = new FileDataStore(output.toFile());
            try {
                pool.writeTo(store);
            } finally {
                store.close();
            }
            return updates.size();
        } finally {
            java.nio.file.Files.deleteIfExists(appOnly.toPath());
        }
    }
}
