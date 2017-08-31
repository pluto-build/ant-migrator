package antplutomigrator.testrunners.utils.tasks;

import antplutomigrator.testrunners.utils.TestTask;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.Files;

/**
 * Created by manuel on 08.06.17.
 */
public class UnzipTask extends TestTask {

    private final File file;
    private final File destDir;

    public UnzipTask(File file, File destDir) {
        this.file = file;
        this.destDir = destDir;
    }

    @Override
    public String getDescription() {
        return "Unzipping " + file + " to " + destDir;
    }

    @Override
    public void execute() throws Exception {
        if (file.getName().endsWith("zip")) {
            ZipFile zipFile = new ZipFile(file);
            zipFile.setRunInThread(true);
            zipFile.extractAll(destDir.getAbsolutePath());
            while (zipFile.getProgressMonitor().getState() == ProgressMonitor.STATE_BUSY)
                Thread.sleep(1);
        } else if (file.getName().endsWith("tar.gz")) {
            Files.createDirectories(destDir.toPath());
            TarArchiveInputStream tarIn = null;

            tarIn = new TarArchiveInputStream(
                    new GzipCompressorInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(
                                            file
                                    )
                            )
                    )
            );

            TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
            // tarIn is a TarArchiveInputStream
            while (tarEntry != null) {// create a file with the same name as the tarEntry
                File destPath = new File(destDir, tarEntry.getName());
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.createNewFile();
                    //byte [] btoRead = new byte[(int)tarEntry.getSize()];
                    byte [] btoRead = new byte[1024];
                    //FileInputStream fin
                    //  = new FileInputStream(destPath.getCanonicalPath());
                    BufferedOutputStream bout =
                            new BufferedOutputStream(new FileOutputStream(destPath));
                    int len = 0;

                    while((len = tarIn.read(btoRead)) != -1)
                    {
                        bout.write(btoRead,0,len);
                    }

                    bout.close();
                    btoRead = null;

                }
                tarEntry = tarIn.getNextTarEntry();
            }
            tarIn.close();
        } else {
            throw new Exception("Unsupported archive exception...");
        }
    }
}
