package ru.asm.core.flac;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 19.04.2017
 * Time: 10:07
 */
public class CueParser {

    public static final Logger logger = LoggerFactory.getLogger(CueParser.class);

    private static final String PERFORMER = "PERFORMER";
    private static final String TITLE = "TITLE";
    private static final String FILE = "FILE";
    private static final String INDEX = "INDEX";
    private static final String TRACK = "TRACK";

    private static final List<String> excludedEncodings = Arrays.asList("EUC-JP");

    public List<FFileDescriptor> parseCue(File torrentDir, File cueFile) throws IOException {

        // e.g. C:\download\group\album\1.cue
        final String cueFilePath = cueFile.getAbsolutePath();
        // e.g. C:\download\group\
        final String downloadPath = torrentDir.getAbsolutePath();
        final String relativePath = cueFilePath.replace(downloadPath, "");

        final List<String> lines = readLines(cueFile);

        final List<FFileDescriptor> files = new ArrayList<>();

        String performer = getDefaultPerformerForFile(cueFile);
        String title = "N/A";

        String trackNum = null;
        String trackType = null;
        String indexNum = null;
        String indexTime = null;

        FFileDescriptor file = null;
        FTrackDescriptor track = null;

        for (String line : lines) {
            final String[] commandWithParams = parseCommandWithParams(line);
            final String command = getCommand(commandWithParams);
            final String[] params = getParams(commandWithParams);

            switch (command) {
                case PERFORMER:
                    if (params.length > 0 && isNotEmpty(params[0])) {
                        performer = params[0];
                    } else {
                        logger.error("performer is not defined");
                    }
                    break;
                case TITLE:
                    if (params.length > 0 && isNotEmpty(params[0])) {
                        title = params[0];
                    } else {
                        logger.error("title is not defined");
                    }
                    break;
                case FILE:
                    String fileName = null;
                    String fileType = null;
                    if (params.length > 0) {
                        fileName = params[0];
                        if (params.length > 1) {
                            fileType = params[1];
                        } else {
                            logger.warn("bad format, file type is not defined");
                        }
                    } else {
                        logger.warn("bad format, file name is not defined");
                    }

                    if (file != null) {
                        logger.debug("file ended");
                    }
                    // start new file
                    file = new FFileDescriptor(relativePath, cueFile.getName());
                    file.setFile(fileName);
                    file.setFileType(fileType);
                    file.setTitle(title);
                    file.setPerformer(performer);
                    file.setTrackDescriptors(new ArrayList<>());

                    if (file.getTitle() == null) {
                        logger.warn("title is null (file {})", cueFile.getAbsolutePath());
                    }
                    files.add(file);

                    if (file != null) {
                        logger.debug("new file started");
                    }

                    break;
                case INDEX:
                    if (params.length > 0) {
                        indexNum = params[0];

                        if (params.length > 1) {
                            indexTime = params[1];
                        } else {
                            logger.warn("bad format, index time is not defined");
                        }
                    } else {
                        logger.warn("bad format, index number is not defined");
                    }
                    break;
                case TRACK:
                    // complete previous track
                    if (track != null) {
                        track.setTrackNum(trackNum);
                        track.setTrackType(trackType);
                        track.setTitle(title);
                        track.setPerformer(performer);
                        track.setIndexNum(indexNum);
                        track.setIndexTime(indexTime);

                        if (track.getTitle() == null) {
                            logger.warn("title is null (file {})", cueFile.getAbsolutePath());
                        }
                        file.getTrackDescriptors().add(track);

                        logger.debug("track has ended");
                    }

                    // start new track
                    track = new FTrackDescriptor();
                    if (params.length > 0) {
                        trackNum = params[0];
                        if (params.length > 1) {
                            trackType = params[1];
                        } else {
                            logger.warn("bad format, track type is not defined");
                        }
                    } else {
                        logger.warn("bad format, track number is not defined");
                    }
                    indexNum = null;
                    indexTime = null;

                    logger.debug("new track has started");
                    break;
                default:
                    logger.debug("ignore command {} with params {}", command, params);
                    break;
            }
        }

        // complete last track
        if (track != null) {
            track.setTrackNum(trackNum);
            track.setTrackType(trackType);
            track.setTitle(title);
            track.setPerformer(performer);
            track.setIndexNum(indexNum);
            track.setIndexTime(indexTime);

            file.getTrackDescriptors().add(track);

            logger.debug("last track has been completed");
        }

        return files;
    }

    private String getCommand(String[] commandWithParams) {
        return commandWithParams[0];
    }

    private String[] getParams(String[] commandWithParams) {
        final String[] params = new String[commandWithParams.length - 1];
        System.arraycopy(commandWithParams, 1, params, 0, commandWithParams.length - 1);
        return params;
    }

    private String[] parseCommandWithParams(String line) {
        final String commandWithParams = getTrimmedLine(line);
        String[] tokens = commandWithParams.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        final String[] result = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].startsWith("\"") && tokens[i].endsWith("\"")) {
                result[i] = tokens[i].substring(1, tokens[i].length() - 1);
            } else {
                result[i] = tokens[i];
            }
        }
        return result;
    }

    private List<String> readLines(File cueFile) throws IOException {
        final Charset encoding = getEncoding(cueFile);
        final List<String> fileLines = FileUtils.readLines(cueFile, encoding);
        return new ArrayList<>(fileLines);
    }

    private String getTrimmedLine(String line) {
        String trimLine = line.trim();
        trimLine = cutBom(trimLine);
        return trimLine;
    }

    private static boolean isNotEmpty(String tmp) {
        return tmp != null && tmp.trim().length() > 0;
    }

    private static String cutBom(String trimLine) {
        if (!trimLine.isEmpty() && trimLine.charAt(0) == '\uFEFF') {
            trimLine = trimLine.substring(1, trimLine.length());
        }
        return trimLine;
    }

    private static String getDefaultPerformerForFile(File cueFile) {
        return cueFile.getName().substring(0, cueFile.getName().length() - 4);
    }

    private static Charset getEncoding(File file) throws IOException {
        String charset = "windows-1251"; // Default charset

        byte[] fileContent;
        FileInputStream fin;

        //create FileInputStream object
        fin = new FileInputStream(file.getPath());

            /*
             * Create byte array large enough to hold the content of the file.
             * Use File.length to determine size of the file in bytes.
             */
        fileContent = new byte[(int) file.length()];

            /*
             * To read content of the file in byte array, use
             * int read(byte[] byteArray) method of java FileInputStream class.
             *
             */
        //noinspection ResultOfMethodCallIgnored
        fin.read(fileContent);

        byte[] data =  fileContent;

        CharsetDetector detector = new CharsetDetector();
        detector.setText(data);

        //final List<String> excluded = Arrays.asList("ISO-8859-1", "ISO-8859-9");

        Integer confidence = null;
        final CharsetMatch[] cms = detector.detectAll();
        if (cms != null) {
            for (CharsetMatch cm : cms) {
                int confidenceL = cm.getConfidence();
                //System.out.println("Encoding: " + cm.getName() + " - Confidence: " + confidenceL + "%");
                //Here you have the encode name and the confidence
                //In my case if the confidence is > 50 I return the encode, else I return the default value
                if (confidenceL > 50 && !excludedEncodings.contains(cm.getName())) {
                    charset = cm.getName();
                    confidence = confidenceL;
                    break;
                }
            }
        }

        //System.out.println(charset);
        if (confidence != null) {
            logger.debug("file \"{}\" encoding is \"{}\" (confidence {}%)", file.getAbsolutePath(), charset, confidence);
        } else {
            logger.debug("file \"{}\" encoding is \"{}\"", file.getAbsolutePath(), charset);
        }

        return Charset.forName(charset);

    }
}
