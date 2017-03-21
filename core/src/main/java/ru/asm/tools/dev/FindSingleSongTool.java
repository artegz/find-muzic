package ru.asm.tools.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import ru.asm.core.index.RepositoryAction;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.TorrentsDatabaseService;
import ru.asm.core.index.domain.TorrentInfoVO;
import ru.asm.core.index.repositories.TorrentInfoRepository;
import ru.asm.core.torrent.TorrentClient;

/**
 * User: artem.smirnov
 * Date: 13.03.2017
 * Time: 8:48
 */
public class FindSingleSongTool {

    private static final Logger logger = LoggerFactory.getLogger(FindSingleSongTool.class);

    public static void main(String[] args) {
        String[] forumIds = new String[] {
                //"737",  // Рок, Панк, Альтернатива (lossless)
                "738"  // Рок, Панк, Альтернатива (lossy)
                //"1755", // Рок-музыка (Hi-Res stereo)
                //"1757", // Рок-музыка (многоканальная музыка)
                //"731",  // Сборники зарубежного рока (lossless)
                //"1799", // Сборники зарубежного рока (lossy)
                //"1756", // Зарубежная рок-музыка
                //"1758"  // Отечественная рок-музыка
        };

        String artist = "Пикник";
        String song = "У Шамана Три Руки";

        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("ru.asm");

        TorrentClient torrentClient = new TorrentClient();

        RepositoryTemplate template = new RepositoryTemplate(applicationContext);
        template.withRepository(new RepositoryAction() {
            @Override
            public void doAction(TorrentInfoRepository repo) {
                TorrentsDatabaseService torrentsDatabaseService = new TorrentsDatabaseService(repo);

                Page<TorrentInfoVO> page = torrentsDatabaseService.findPage(forumIds, null/*artist.toLowerCase()*/, 0, 100);

                for (int i = 0; i < page.getNumberOfElements(); i++) {
                    final TorrentInfoVO entry = page.getContent().get(i);

                    logger.info("{}. [{}] {}", i+1, entry.getForum(), entry.getTitle());

//                    try {
//                        TorrentInfo torrentInfo = torrentClient.findByMagnet(entry.getMagnet());
//
//                        // enlist files
//                        for (int j = 0; j < torrentInfo.files().numFiles(); j++) {
//                            logger.info(torrentInfo.files().filePath(j));
//                        }
//                        logger.info("SUCCEEDED");
//                    } catch (TorrentClientException e) {
//                        logger.info("FAILED: " + e.getErrCode());
//                    }
                }
                logger.info("total: {}", page.getTotalElements());
            }
        });


    }

}
