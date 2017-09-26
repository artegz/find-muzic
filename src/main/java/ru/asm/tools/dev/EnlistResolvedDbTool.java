package ru.asm.tools.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.asm.core.index.RepositoryTemplate;
import ru.asm.core.index.domain.TorrentFilesVO;
import ru.asm.core.index.domain.TorrentSongVO;
import ru.asm.core.index.repositories.TorrentFilesRepository;

/**
 * User: artem.smirnov
 * Date: 21.03.2017
 * Time: 12:22
 */
public class EnlistResolvedDbTool {

    private static final Logger logger = LoggerFactory.getLogger(EnlistResolvedDbTool.class);

    private ApplicationContext applicationContext;



    public static void main(String[] args) {
        new EnlistResolvedDbTool().printAll();
    }


    public void printAll() {
        applicationContext = new AnnotationConfigApplicationContext("ru.asm");

        try {
            final TorrentFilesRepository torrentFilesRepository = applicationContext.getBean(TorrentFilesRepository.class);

            new RepositoryTemplate<>(applicationContext, torrentFilesRepository).withRepository(torrentFilesRepo -> {
                final TorrentFilesRepository repo = (TorrentFilesRepository) torrentFilesRepo;

                final Iterable<TorrentFilesVO> iterable = repo.findAll();
                for (TorrentFilesVO torrentFilesVO : iterable) {
                    final StringBuffer sb = new StringBuffer();
                    sb.append(torrentFilesVO.getArtist())
                            .append(System.lineSeparator());
                    for (TorrentSongVO songVO : torrentFilesVO.getTorrentSongs()) {
                        sb.append(" - ")
                                .append(songVO.getSongName())
                                .append(System.lineSeparator());
                    }
//                    for (String file : torrentFilesVO.getFileNames()) {
//                        sb.append(" - ")
//                                .append(file)
//                                .append(System.lineSeparator());
//                    }
                    logger.info(sb.toString());
                }
            });


//            for (String s : notFound) {
//                System.out.println("\"" + s + "\"");
//            }
        } finally {

        }
    }
}
