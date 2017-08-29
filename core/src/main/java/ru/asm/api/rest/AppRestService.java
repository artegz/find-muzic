package ru.asm.api.rest;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import ru.asm.core.AppConfiguration;
import ru.asm.core.AppCoreService;
import ru.asm.core.persistence.domain.ResolvedSongEntity;
import ru.asm.core.persistence.domain.StatusEntity;
import ru.asm.core.persistence.mappers.PlaylistSongsMapper;

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

    @Autowired
    private AppCoreService appCoreService;


    // Step 1.1: download fresh torrents DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/download")
    public void downloadTorrentDb(@QueryParam("magnet") String magnet) {
        if (magnet == null) {
            magnet = "magnet:?xt=urn:btih:77D5646C8645A40BDB635FBED263B5B3A86BCB0F";
        }
        final AppConfiguration appConfiguration = AppConfiguration.getInstance();
        appCoreService.downloadTorrent(appConfiguration.getTorrentDbsStorageLocation(), magnet);
    }

    // Step 1.2: extract downloaded torrents DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/extract")
    public void extractTorrentDb() {
        // todo: extract zip
    }

    // Step 1.3: index downloaded torrents DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/torrentDbs/index")
    public void indexTorrentDb() {
        final String pathname = "C:\\TEMP\\find-music\\rutracker_org_db\\backup.20170208185701\\backup.20170208185701.xml";
        final File backup = new File(pathname);

        appCoreService.indexTorrentsDb(backup);
    }

    // Step 2.1: download (fetch) desired playlist
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/playlists/download")
    public void downloadPlaylist() {
        // todo: download playlist
    }

    // Step 2.2: import playlist into DB
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/playlists/import")
    public void importPlaylist() {
        final String playlist = "nashe";
        final String comment = "test";
        final File file = new File("C:\\IdeaProjects\\find-muzic\\core\\src\\main\\resources\\playlists/102015.nashe.playlist.txt");

        appCoreService.importPlaylist(playlist, comment, file);
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/artists")
    public List<Integer> getArtists() {
        return appCoreService.getArtistIds();
    }

    // Step 3: resolve artist torrents (find artists in torrents DB)
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/artists/search")
    public void searchArtists() {
        appCoreService.resolveArtists();
    }

    // Step 4: index songs from torrents
    // search for playlist artist songs in torrents (mp3 and flac files) and index them
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/songs/index")
    public void indexSongs(@QueryParam("format") String format) {
        if (format.equals("mp3")) {
            appCoreService.resolveSongs_mp3(null, null);
        } else if (format.equals("flac")) {
            appCoreService.resolveSongs_flac(null, null);
        }
    }

    // Step 5: resolve playlist songs (find desired sons in index)
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/songs/search")
    public void searchSongs() {
        appCoreService.resolveSongs();
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/songs")
    public List<ResolvedSongEntity> getFoundSongs() {
        return appCoreService.getFoundSongs();
    }

    // Step 6: download playlist songs
    @POST
    @Produces("application/json; charset=UTF-8")
    @Path("/songs/download")
    public void downloadSongs() {
        // todo
    }



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


    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/results/statuses")
    public List<StatusEntity> getStatuses() {
        final List<StatusEntity> entities = playlistSongsMapper.getStatuses();
        return entities;
    }

    @GET
    @Produces("application/json; charset=UTF-8")
    @Path("/results/songs")
    public List<ResolvedSongEntity> getResolvedSongs() {
        return getFoundSongs();
    }

}
