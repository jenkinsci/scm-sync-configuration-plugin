package hudson.plugins.scm_sync_configuration.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

/**
 * @author fcamblor
 * Utility class allowing to provide easy access to jenkins files checksums
 */
public class Checksums {

    private static final int BUF_SIZE = 0x1000; // 4K

    public static boolean fileAndByteArrayContentAreEqual(File file, byte[] content) throws IOException {
        if(!file.exists()){
            return content == null || content.length == 0;
        }

        long fileChecksum;
        CheckedInputStream in = null;
        try {
            in = new CheckedInputStream(new FileInputStream(file), createChecksum());
            byte[] buffer = new byte[BUF_SIZE];
            while (in.read(buffer, 0, buffer.length) >= 0) {
                // keep updating checksum
            }
            fileChecksum = in.getChecksum().getValue();
        } finally {
            if (in != null) {
                in.close();
            }
        }

        Checksum checksum = createChecksum();
        checksum.update(content, 0, content.length);
        long contentChecksum = checksum.getValue();

        return fileChecksum == contentChecksum;
    }

    private static Checksum createChecksum(){
        return new CRC32();
    }
}
