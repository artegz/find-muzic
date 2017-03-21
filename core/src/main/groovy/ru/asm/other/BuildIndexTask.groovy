package ru.asm.other

import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.elasticsearch.node.Node
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.format.number.NumberStyleFormatter
import ru.asm.core.index.domain.TorrentInfoVO
import ru.asm.core.index.repositories.TorrentInfoRepository
/**
 * User: artem.smirnov
 * Date: 07.06.2016
 * Time: 8:58
 */
class BuildIndexTask {

    public static final format = new NumberStyleFormatter().getNumberFormat(Locale.ENGLISH)
    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yy HH:mm").withZoneUTC()

    public static final long offset = 0
    public static final int GROUP_SIZE = 10000

    public static void main(String[] args) {
        def applicationContext = new AnnotationConfigApplicationContext("ru.asm")
        def node = applicationContext.getBean(Node.class)
        def torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class)
        def elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class)

        try {
            def file = new File("C:\\TEMP\\torrents\\table_sorted\\table_sorted.txt")

            // await at least for yellow status
            def response = elasticsearchOperations.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get()
            if (response.getStatus() != ClusterHealthStatus.YELLOW) {
                throw new AssertionError();
            }

            printMemoryInfo()

            int totalIndexed = 0

            def count = torrentInfoRepository.count()
            println "${count} entries in repository"

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
                        torrentInfoRepository.save(torrentInfoVOs)
                        totalIndexed += torrentInfoVOs.size()
                        println "${torrentInfoVOs.size()} entries were added into index (total: ${totalIndexed})"
                    }

                    if (ready) {
                        println "no more lines to read"
                        break
                    }
                }
            }

            println "${torrentInfoRepository.count()} entries in repository"
            printMemoryInfo()
        } finally {
            node.close();
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

        def categoryStr = vals[8]
        String[] parts = categoryStr.split("\\|")

        if (parts.size() > 0) {
            torrentInfoVO.setMainCategory(parts[0].trim())
        }
        if (parts.size() > 1) {
            torrentInfoVO.setSubCategory(parts[1].trim())
        }
        if (parts.size() > 2) {
            List<String> folders = new ArrayList<String>()
            for (int i = 2; i < parts.size(); i++) {
                folders << parts[i].trim()
            }

            def sb = new StringBuilder()
            def it = folders.iterator()
            while (it.hasNext()) {
                sb.append(it.next())
                if (it.hasNext()) {
                    sb.append(" | ")
                }
            }

            torrentInfoVO.setFolders(sb.toString())
        }

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
