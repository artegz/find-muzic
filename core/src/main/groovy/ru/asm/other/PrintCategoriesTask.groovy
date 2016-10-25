package ru.asm.other

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.format.number.NumberStyleFormatter
import ru.asm.domain.TorrentInfoVO
/**
 * User: artem.smirnov
 * Date: 07.06.2016
 * Time: 8:58
 */
class PrintCategoriesTask {

    public static final format = new NumberStyleFormatter().getNumberFormat(Locale.ENGLISH)
    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yy HH:mm").withZoneUTC()

    public static final long offset = 0
    public static final int GROUP_SIZE = 10000

    public static void main(String[] args) {

        def categories = new TreeSet<String>()

        try {
            def file = new File("C:\\TEMP\\torrents\\table_sorted\\table_sorted.txt")

            printMemoryInfo()

            int totalChecked = 0

            file.withReader { reader ->
                boolean ready = false;

                println "reading lines starting from ${offset} row"

                if (offset > 0) {
                    for (long i = 0; i < offset; i++) {
                        reader.readLine()
                    }
                }

                while (!ready) {
                    def lines = new ArrayList<>()
                    for (int i = 0; i < GROUP_SIZE; i++) {
                        def ln = reader.readLine()
                        if (ln != null) {
                            lines << ln
                        } else {
                            ready = true
                            break
                        }
                    }

                    println "${lines.size()} lines were read"

                    if (!lines.isEmpty()) {
                        def torrentInfoVOs = parseLines(lines)

                        for (TorrentInfoVO vo : torrentInfoVOs) {
                            categories.add(vo.getMainCategory())
                        }

                        totalChecked += torrentInfoVOs.size()
                        println "${torrentInfoVOs.size()} entries were checked (total ${totalChecked})"
                    }

                    if (ready) {
                        println "no more lines to read"
                        break
                    }
                }
            }

            for (int i = 0; i < categories.size(); i++) {
                println "${i + 1}. ${categories.getAt(i)}"
            }

            printMemoryInfo()
        } finally {
            // nothing to do
        }
    }

    private static ArrayList<TorrentInfoVO> parseLines(lines) {
        List<TorrentInfoVO> torrentInfoVOs = new ArrayList<TorrentInfoVO>()
        lines.each { line ->
            torrentInfoVOs.add(parse(line as String))
        }
        torrentInfoVOs
    }

    private static TorrentInfoVO parse(String line) {
        def vals = line.split("\t")

        def torrentInfoVO = new TorrentInfoVO()

        torrentInfoVO.setId(vals[0])
        torrentInfoVO.setTitle(vals[1])
        torrentInfoVO.setSize(Long.valueOf(vals[2]))
        torrentInfoVO.setSeedsNum(Integer.valueOf(vals[3]))
        torrentInfoVO.setPeerNum(Integer.valueOf(vals[4]))
        torrentInfoVO.setHash(vals[5])
        torrentInfoVO.setDownloads(Integer.valueOf(vals[6]))
        torrentInfoVO.setCreationDate(parseDate(vals[7]))
        torrentInfoVO.setCategory(vals[8])
        torrentInfoVO
    }

    private static void printMemoryInfo() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long allocatedMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();

        println("FREE: ${format.format(freeMemory)} bytes, ALLOCATED: ${format.format(allocatedMemory)} bytes, TOTAL: ${format.format(maxMemory)} bytes")
    }

    private static Date parseDate(String s) {
        try {
            return DateTime.parse(s, formatter).toDate()
        } catch (Throwable e) {
            println(e.getMessage())
            return null
        }
    }


}
