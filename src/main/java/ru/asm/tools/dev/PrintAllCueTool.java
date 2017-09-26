package ru.asm.tools.dev;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.asm.core.flac.FFileDescriptor;
import ru.asm.core.flac.FTrackDescriptor;
import ru.asm.tools.CueParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 07.04.2017
 * Time: 13:30
 */
public class PrintAllCueTool {

    private static final Logger logger = LoggerFactory.getLogger(PrintAllCueTool.class);

    public static final Charset ENCODING = Charset.forName("windows-1251");

    public static void main(String[] args) {
        final File file = new File("C:\\TEMP\\find-music\\downloads\\flac\\");

        try {
            getTorrentSongs(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int i = 0;
        System.out.println(String.valueOf(i++));
    }

    private static void getTorrentSongs(File torrentDir) throws IOException {
        final Iterator<File> fileIterator = FileUtils.iterateFiles(torrentDir, new String[]{"cue"}, true);
        while (fileIterator.hasNext()) {
            final File cueFile = fileIterator.next();
            parseCue(torrentDir, cueFile);
        }
    }

    private static List<FFileDescriptor> parseCue(File torrentDir, File cueFile) throws IOException {

        final List<FFileDescriptor> fFileDescriptors = new CueParser().parseCue(torrentDir, cueFile);
        for (FFileDescriptor fFileDescriptor : fFileDescriptors) {
            logger.info(String.format("%s || %s ", fFileDescriptor.getPerformer(), fFileDescriptor.getTitle()));
            for (FTrackDescriptor trackDescriptor : fFileDescriptor.getTrackDescriptors()) {
                logger.info(String.format("   %s || %s ", trackDescriptor.getPerformer(), trackDescriptor.getTitle()));
            }
        }

//        // e.g. C:\download\group\album\1.cue
//        final String cueFilePath = cueFile.getAbsolutePath();
//        // e.g. C:\download\group\
//        final String downloadPath = torrentDir.getAbsolutePath();
//        final String relativePath = cueFilePath.replace(downloadPath, "");
//
//        final FileInputStream cueInputStream = new FileInputStream(cueFile);
//        final Charset encoding = getEncoding(cueInputStream, cueFile);
//        final List<String> fileLines = FileUtils.readLines(cueFile, encoding);
//        final List<String> lines = new ArrayList<>(fileLines);
//
////        final TikaConfig defaultConfig = TikaConfig.getDefaultConfig();
////        final Tika tika = new Tika(new TextDetector(), new AutoDetectParser(defaultConfig), defaultConfig.getTranslator());
////        tika.setMaxStringLength(-1);
////        final String content;
////        try {
////            content = tika.parseToString(cueFile);
////        } catch (TikaException e) {
////            throw new AssertionError();
////        }
////        final List<String> lines = Arrays.asList(content.split(System.lineSeparator()));
//
//        String performer = null;
//        String title = null;
//
//        String trackNum = null;
//        String trackType = null;
//
//        String indexNum = null;
//        String indexTime = null;
//
//        FFileDescriptor ffileDescriptor = null;
//        FTrackDescriptor ftrackDescriptor = null;
//
//        String file = null;
//        String fileType = null;
//
//        for (String line : lines) {
//            final String trimLine = line.trim();
//            if (trimLine.startsWith("PERFORMER")) {
//                performer = trimLine.split(" ")[1];
//            } else if (trimLine.startsWith("TITLE")) {
//                title = trimLine.replaceAll("TITLE \"(.*)\"", "$1");
//            } else if (trimLine.startsWith("FILE")) {
//
//                file = trimLine.replaceAll("FILE \"(.*)\" .*", "$1");
//                fileType = trimLine.replaceAll("FILE \".*\" (.*)", "$1");
//
//                title = null;
//                performer = null;
//            } else if (trimLine.startsWith("INDEX")) {
//                indexNum = trimLine.split(" ")[1];
//                indexTime = trimLine.split(" ")[2];
//            } else if (trimLine.startsWith("TRACK")) {
//                if (ftrackDescriptor != null) {
//                    logger.info(String.format("|| %s || %s || %s ||", performer, title, title));
//                }
//
//                ftrackDescriptor = new FTrackDescriptor();
//                trackNum = trimLine.split(" ")[1];
//                trackType = trimLine.split(" ")[2];
//                title = null;
//                performer = null;
//                indexNum = null;
//                indexTime = null;
//            }
//        }
//
//        logger.info(String.format("|| %s || %s || %s ||", performer, title, title));
        return null;
    }

//    private static Charset getEncoding(FileInputStream cueInputStream, File file) throws IOException {
////        final Charset utf8 = Charset.forName("UTF-8");
////        final Charset cp1252 = Charset.forName("windows-1252");
////        Charset[] charsets = new Charset[] { utf8, cp1252 };
////
////        for (Charset charset : charsets) {
////            FileUtils.readLines(cueFile, charset);
////        }
//        String charset = "windows-1251"; //Default chartset, put whatever you want
//
//        byte[] fileContent = null;
//        FileInputStream fin = null;
//
//        //create FileInputStream object
//        fin = new FileInputStream(file.getPath());
//
//            /*
//             * Create byte array large enough to hold the content of the file.
//             * Use File.length to determine size of the file in bytes.
//             */
//        fileContent = new byte[(int) file.length()];
//
//            /*
//             * To read content of the file in byte array, use
//             * int read(byte[] byteArray) method of java FileInputStream class.
//             *
//             */
//        fin.read(fileContent);
//
//        byte[] data =  fileContent;
//
//        CharsetDetector detector = new CharsetDetector();
//        detector.setText(data);
//
//        //final List<String> excluded = Arrays.asList("ISO-8859-1", "ISO-8859-9");
//
//        final CharsetMatch[] cms = detector.detectAll();
//        if (cms != null) {
//            for (CharsetMatch cm : cms) {
//                int confidence = cm.getConfidence();
//                System.out.println("Encoding: " + cm.getName() + " - Confidence: " + confidence + "%");
//                //Here you have the encode name and the confidence
//                //In my case if the confidence is > 50 I return the encode, else I return the default value
//                if (confidence > 50) {
//                    charset = cm.getName();
//                }
//            }
//        }
//
//
////        CharsetMatch cm = detector.detect();
////
////        if (cm != null) {
////            int confidence = cm.getConfidence();
////            System.out.println("Encoding: " + cm.getName() + " - Confidence: " + confidence + "%");
////            //Here you have the encode name and the confidence
////            //In my case if the confidence is > 50 I return the encode, else I return the default value
////            if (confidence > 20) {
////                charset = cm.getName();
////            }
////        }
//
//        System.out.println(charset);
//
//
//
//
//        return Charset.forName(charset);
//
//    }
//        final MutableBoolean found = new MutableBoolean(false);
//        final MutableObject result = new MutableObject(null);
//
//        // Initalize the nsDetector() ;
//        int lang = nsPSMDetector.MAX_VERIFIERS;
//        nsDetector det = new nsDetector(lang);
//
//        // Set an observer...
//        // The Notify() will be called when a matching charset is found.
//
//        det.Init(new nsICharsetDetectionObserver() {
//            public void Notify(String charset) {
//                found.setValue(true);
//                System.out.println("CHARSET = " + charset);
//                result.setValue(charset);
//            }
//        });
//
//        BufferedInputStream imp = new BufferedInputStream(cueInputStream);
//
//        byte[] buf = new byte[1024];
//        int len;
//        boolean done = false;
//        boolean isAscii = true;
//
//        while ((len = imp.read(buf, 0, buf.length)) != -1) {
//
//            // Check if the stream is only ascii.
//            if (isAscii)
//                isAscii = det.isAscii(buf, len);
//
//            // DoIt if non-ascii and not done yet.
//            if (!isAscii && !done)
//                done = det.DoIt(buf, len, false);
//        }
//        det.DataEnd();
//
//        if (isAscii) {
//            System.out.println("CHARSET = ASCII");
//            found.setValue(true);
//            result.setValue("ASCII");
//        }
//
//        if (!((Boolean) found.getValue())) {
//            String prob[] = det.getProbableCharsets();
//            for (int i = 0; i < prob.length; i++) {
//                System.out.println("Probable Charset = " + prob[i]);
//            }
//            if (prob.length > 0) {
//                result.setValue(prob[0]);
//            }
//        }
//
//        if (result.getValue() == null) {
//            System.out.println("unable determine encoding");
//        }
//        return result.getValue() != null
//                ? Charset.forName(((String) result.getValue()))
//                : ENCODING;
//    }

}
