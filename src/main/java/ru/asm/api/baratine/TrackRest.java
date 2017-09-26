package ru.asm.api.baratine;

import io.baratine.service.Result;
import io.baratine.vault.IdAsset;
import io.baratine.web.*;

import javax.inject.Inject;
import java.util.Collection;

/**
 * User: artem.smirnov
 * Date: 22.08.2017
 * Time: 10:50
 */
//@Service
public class TrackRest {

    final TrackVault trackVault;

    @Inject
    public TrackRest(TrackVault trackVault) {
        this.trackVault = trackVault;
    }

    @Delete("/tracks/{id}")
    public void deleteTrack(@Path("id") Track track,
                            Result<String> result) {
        trackVault.delete(track.getId(), result.then(b -> b ? "ok" : "nok"));
    }

    @Get("/tracks")
    public void getTracks(RequestWeb request,
                          Result<Collection<Track>> result) {
        //result.okShim(request.service(TrackVault.class));
        request.service(TrackVault.class).findAll(result);
//        Result.of(o -> "")
//        request.service(TrackVault.class).create(newData, result);
    }

    @Post("/tracks")
    public void createTrack(RequestWeb request,
                            @Body Track newData,
                            Result<IdAsset> result) {
        request.service(TrackVault.class).create(newData, result);
    }

    @Get("/tracks/{id}")
    public void getTrack(@Path("id") Track track,
                        Result<Track> result) {
        track.get(result);
    }

    @Post("/tracks/{id}")
    public void setTrack(@Path("id") Track track,
                        @Body Track newData,
                        Result<String> result) {
        track.set(newData, result.then(b -> b ? "ok" : "nok"));
    }

}
