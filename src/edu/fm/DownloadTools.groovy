package edu.fm

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import java.text.DecimalFormat

/**
 * User: artem.smirnov
 * Date: 06.11.2015
 * Time: 9:45
 */
class DownloadTools {

    static void downloadFile(String song, String url, File workDir) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);

        HttpResponse response = httpClient.execute(httpget);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            //def filename = url.substring(url.lastIndexOf("/") + 1, url.length())
            def filename = song.replaceAll("\"", "").replaceAll("/", " ") + ".mp3"
            def file = new File(workDir, filename)

            if (!file.exists()) {
                file.createNewFile()

                //println "(${song}) downloading '${filename}' from ${url}"

                long bytes = 0

                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                int inByte;
                while ((inByte = bis.read()) != -1) {
                    bos.write(inByte)
                    bytes++
                };
                bis.close();
                bos.close();

                printf("%,d bytes (%s Mb) ...", bytes, new DecimalFormat("#0.00").format(bytes / 1024 / 1024))
                if (bytes < 1 * 1024 * 1024) {
                    file.delete()
                    throw new Exception("loaded file is too small and may be corrupted")
                }
            } else {
                throw new Exception("file already exists")
            }
        } else {
            throw new Exception("unable to download}")
        }
    }
}
