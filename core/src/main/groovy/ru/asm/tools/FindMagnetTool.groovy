package ru.asm.tools

import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import ru.asm.TorrentsDatabaseService
import ru.asm.domain.TorrentInfoVO
import ru.asm.repositories.TorrentInfoRepository
import ru.asm.torrent.TorrentClient
import ru.asm.torrent.TorrentClient.TorrentClientException

/**
 * User: artem.smirnov
 * Date: 13.03.2017
 * Time: 8:48
 */
class FindMagnetTool {

    public static void main(String[] args) {
//        String[] mainCategory = ["Рок-музыка"]
        String[] mainCategory = ["Музыка"]
//        String[] mainCategory = null
//        String[] subCategory = ["Отечественный Рок | Рок, Панк, Альтернатива (lossless)"]
        String[] subCategory = ["Отечественный"]
        String[] folderQuery = ["lossless"]
//        String[] subCategory = null

        def artist = "Пикник"
        def song = "У Шамана Три Руки"

        def applicationContext = new AnnotationConfigApplicationContext("ru.asm")

        def torrentClient = new TorrentClient()

        def template = new RepositoryTemplate(applicationContext)
        template.withRepository(new RepositoryAction() {
            @Override
            void doAction(TorrentInfoRepository repo) {
                def torrentsDatabaseService = new TorrentsDatabaseService(repo)

                def page = torrentsDatabaseService.findPage(mainCategory, subCategory, folderQuery, artist.toLowerCase(), 0)

                page.eachWithIndex { TorrentInfoVO entry, int index ->
                    println "${index + 1}. [${entry.getMainCategory()}] | [${entry.getSubCategory()}] | [${entry.getFolders()}] [${entry.getTitle()}] --- ${entry.getHash()}"

                    try {
                        def torrentInfo = torrentClient.findByHash(entry.getHash())

                        // enlist files
                        for (int i = 0; i < torrentInfo.files().numFiles(); i++) {
                            println(torrentInfo.files().filePath(i))
                        }
                        println("SUCCEEDED")
                    } catch (TorrentClientException e) {
                        println("FAILED: " + e.errCode)
                    }
                }
                println("total: ${page.totalElements}")
            }
        })


    }

    private static class RepositoryTemplate {

        org.elasticsearch.node.Node node
        TorrentInfoRepository torrentInfoRepository
        ElasticsearchOperations elasticsearchOperations

        public RepositoryTemplate(ApplicationContext applicationContext) {
            this.node = applicationContext.getBean(org.elasticsearch.node.Node.class)
            this.torrentInfoRepository = applicationContext.getBean(TorrentInfoRepository.class)
            this.elasticsearchOperations = applicationContext.getBean(ElasticsearchOperations.class)
        }

        public void withRepository(RepositoryAction r) {
            try {
                // await at least for yellow status
                def response = elasticsearchOperations.getClient()
                        .admin()
                        .cluster()
                        .prepareHealth()
                        //.setWaitForYellowStatus()
                        .setWaitForGreenStatus()
                        .get()
                if (response.getStatus() != ClusterHealthStatus.YELLOW) {
                    throw new IllegalStateException("repository is not initialized")
                }

                def count = torrentInfoRepository.count()
                if (count <= 0) {
                    throw new IllegalArgumentException("repository is empty")
                } else {
                    println "${count} entries in repository"
                }

                r.doAction(torrentInfoRepository)
            } finally {
                node.close()
            }
        }

    }

    private static interface RepositoryAction {

        void doAction(TorrentInfoRepository repo)
    }

}
