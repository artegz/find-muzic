package ru.asm

import groovyx.gpars.GParsPool
import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.elasticsearch.node.Node
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.format.number.NumberStyleFormatter
import ru.asm.domain.TorrentInfoVO
import ru.asm.repositories.TorrentInfoRepository

import java.util.concurrent.atomic.AtomicLong

/**
 * User: artem.smirnov
 * Date: 07.06.2016
 * Time: 8:58
 */
class BuildIndexTask {

    public static final format = new NumberStyleFormatter().getNumberFormat(Locale.ENGLISH)
    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yy HH:mm").withZoneUTC()

    public static void main(String[] args) {
        def applicationContext = new AnnotationConfigApplicationContext("ru.asm")
        def node = applicationContext.getBean(Node.class)
        def torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class)
        def elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class)

        try {
//            def file = new File("C:\\TEMP\\torrents\\table_sorted\\test.txt")
            def file = new File("C:\\TEMP\\torrents\\table_sorted\\table_sorted.txt")

            // await at least for yellow status
            def response = elasticsearchOperations.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().get()
            if (response.getStatus() != ClusterHealthStatus.YELLOW) {
                throw new AssertionError();
            }

            printMemoryInfo()

            def count = torrentInfoRepository.count()

            println "${count} entries in repository"

            if (count > 0) {
                torrentInfoRepository.findAll().each { torrentInfo ->
                    println "${torrentInfo}"
                }
            } else {
                def progressManager = new ProgressManager(file.size())

                file.withReader { reader ->
                    GParsPool.withPool(16) {
                        def lines = reader.readLines()
                        long linesNum = lines.size()

                        def linesReady = new AtomicLong(0L)

                        lines.eachParallel { line ->
                            TorrentInfoVO torrentInfoVO = parse(line)
                            torrentInfoRepository.save(torrentInfoVO)

//                            progressManager.increase(line.bytes.length)
//
//                            progressManager.printIfChanged()
                            def linesReadyNum = linesReady.getAndIncrement()
                            println "${linesReadyNum} / ${linesNum}"
                        }

                    }
                }
            }

            println "${torrentInfoRepository.count()} entries in repository"
            printMemoryInfo()
        } finally {
            node.close();
        }
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

    public static class ProgressManager {

        private long total

        volatile private long ready = 0

        private double minStep = 0.01d / 100;

        private volatile long lastPrinted = -1

        private long startTime = -1

        ProgressManager(long total) {
            this.total = total
            this.startTime = System.nanoTime()
        }

        ProgressManager(long total, double minStep) {
            this.total = total
            this.minStep = minStep
            this.startTime = System.nanoTime()
        }

        public void increase(long inc) {
            ready += inc
        }

        public void printIfChanged() {
            if (isChanged()) {
                long currentTime = System.nanoTime()
                long elapsedTime = currentTime - startTime

                // elapsed / ready = totalTime / total
                long totalTime = (long) elapsedTime * (((double) total) / ((double) ready))

                long restNanos = totalTime - elapsedTime
                long restMiles = (long) ((double) restNanos) / 1000
                long restSeconds = (long) ((double) restMiles) / 1000
                long restMinutes = (long) ((double) restSeconds) / 60
                long restHours = (long) ((double) restMinutes) / 60

                def percent = "${format.format(ready / total * 100d)}%"

                if (restHours > 1) {
                    println "[${percent}] (${restHours}h more)"
                } else if (restMinutes > 1) {
                    println "[${percent}] (${restMinutes}m more)"
                } else {
                    println "[${percent}] (${restSeconds}s more)"
                }
                lastPrinted = ready
            }
        }

        public boolean isChanged() {
            if (lastPrinted < 0) {
                return true
            }

            def current = ((double) ready) / ((double) total)
            def prev = ((double) lastPrinted) / ((double) total)

            if (Math.abs(current - prev) > minStep) {
                return true
            } else {
                return false
            }
        }
    }
}
