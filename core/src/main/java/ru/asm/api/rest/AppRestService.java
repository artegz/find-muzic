package ru.asm.api.rest;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import ru.asm.core.AppConfiguration;
import ru.asm.core.persistence.domain.StatusEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;
import ru.asm.core.torrent.TorrentDbService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 17:55
 */
@Path("/")
public class AppRestService {

    @Autowired
    private PlaylistSongsMapper playlistSongsMapper;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/sample")
    public InputStream sample() {
        return getClass().getResourceAsStream("/sample.html");
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/artists")
    public List<String> artists() {
        final List<String> artists = playlistSongsMapper.getAllArtists();
        return artists;
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs")
    public List<String> getTorrentDbs() {
        final AppConfiguration appConfiguration = AppConfiguration.getInstance();

        final File[] files = appConfiguration.getTorrentDbsStorageLocation().listFiles(File::isFile);
        assert files != null;
        final List<String> filenames = Lists.transform(Arrays.asList(files), File::getName);

        return filenames;
    }


    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/download")
    public void downloadTorrentDb(@QueryParam("magnet") String magnet) {
        final AppConfiguration appConfiguration = AppConfiguration.getInstance();
        new TorrentDbService().download(appConfiguration.getTorrentDbsStorageLocation(), magnet);
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/results/statuses")
    public List<StatusEntity> getStatuses() {
        final List<StatusEntity> entities = playlistSongsMapper.getStatuses();
        return entities;
    }

}
