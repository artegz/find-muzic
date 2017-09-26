package ru.asm.api.rest;

import ru.asm.core.dev.model.Artist;
import ru.asm.core.dev.model.Song;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: artem.smirnov
 * Date: 26.09.2017
 * Time: 8:49
 */
public class Test {

    public static void main(String[] args) {
        final Artist artist1 = new Artist();
        artist1.setArtistId(1);
        final Artist artist2 = new Artist();
        artist2.setArtistId(2);


        final Song song1 = new Song();
        song1.setArtist(artist1);
        final Song song2 = new Song();
        song2.setArtist(artist1);
        final Song song3 = new Song();
        song3.setArtist(artist2);

        final List<Song> songList = Arrays.asList(
                song1, song2, song3
        );

//        final List<ArtistSongCollection> result = new ArrayList<>();
//
//        final List<ArtistSongCollection> reduceResult = songList.stream()
//                .map(song -> new ArtistSongCollection(song.getArtist(), new ArrayList<>(Collections.singletonList(song))))
//                .reduce(result, (c1, c2) -> {
//
//                    return c1;
//                }, (c1, c2) -> {
//                    return c1;
//                });

        Map<Integer, List<Song>> result =
                songList.stream().collect(Collectors.groupingBy(o -> o.getArtist().getArtistId()));

        for (Integer artistId : result.keySet()) {
            System.out.println(String.format("artist %s have %s songs", artistId, result.get(artistId)));
        }




    }
}
