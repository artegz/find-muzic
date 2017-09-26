CREATE TABLE SONGS (
  SONG_ID INTEGER,
  ARTIST_ID INTEGER,
  TITLE VARCHAR(100),
  CONSTRAINT XAK1_SONGS UNIQUE (ARTIST_ID,SONG_ID)
);

CREATE TABLE PLAYLIST_SONGS (
  ARTIST_ID INTEGER,
  SONG_ID INTEGER,
  PLAYLIST VARCHAR(100),
  COMMENT VARCHAR(400),
  CONSTRAINT XAKPLAYLIST_SONGS UNIQUE (ARTIST_ID,SONG_ID,PLAYLIST)
);

CREATE TABLE ARTISTS (
  ARTIST_ID INTEGER,
  ARTIST VARCHAR(100)
);

CREATE TABLE ARTIST_TORRENTS_STATUS (
  ARTIST_ID INTEGER,
  TORRENT_ID VARCHAR(20),
  FORMAT VARCHAR(5),
  FORUM_ID VARCHAR(20),
  STATUS VARCHAR(1000),
  CONSTRAINT XAKARTIST_TORRENTS UNIQUE (ARTIST_ID,TORRENT_ID,FORMAT)
);
