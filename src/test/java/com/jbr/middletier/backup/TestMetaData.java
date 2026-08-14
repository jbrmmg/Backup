package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.manager.FileSystemCustomData;
import com.jbr.middletier.backup.manager.FileSystemImageData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import com.jbr.middletier.backup.manager.FileSystem;

import java.io.File;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SpringBootTest(classes = MiddleTier.class)
class TestMetaData {
    @Autowired
    FileSystem fileSystem;

    @Test
    void testMetaDataJpeg() {
        File jpeg = new File("src/test/resources/synchronise/IMG_1015.JPEG");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(jpeg);

        assertTrue(meta.isPresent());
        assertTrue(meta.get().isValid());
        assertNotNull(meta.get().getImageSize());
        assertNotNull(meta.get().getDateTime());
        assertNotNull(meta.get().getLatLong());
        assertEquals("image/jpeg",meta.get().getMimeType());
        assertEquals(1536,meta.get().getImageSize().width());
        assertEquals(2048,meta.get().getImageSize().height());
        assertEquals(51.456225,meta.get().getLatLong().getLatitude(),0.00001);
        assertEquals(-2.6257111,meta.get().getLatLong().getLongitude(),0.00001);
        assertEquals(2025,meta.get().getDateTime().getYear());
        assertEquals(Month.MARCH,meta.get().getDateTime().getMonth());
        assertEquals(1,meta.get().getDateTime().getDayOfMonth());
        assertEquals(13,meta.get().getDateTime().getHour());
        assertEquals(56,meta.get().getDateTime().getMinute());
        assertEquals(44,meta.get().getDateTime().getSecond());
    }

    @Test
    void testMetaDataMov() {
        File jpeg = new File("src/test/resources/synchronise/IMG_1015.MOV");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(jpeg);

        assertTrue(meta.isPresent());
        assertTrue(meta.get().isValid());
        assertNotNull(meta.get().getImageSize());
        assertNotNull(meta.get().getDateTime());
        assertNotNull(meta.get().getLatLong());
        assertEquals("video/quicktime",meta.get().getMimeType());
        assertEquals(960,meta.get().getImageSize().width());
        assertEquals(720,meta.get().getImageSize().height());
        assertEquals(51.4562,meta.get().getLatLong().getLatitude(),0.00001);
        assertEquals(-2.6257,meta.get().getLatLong().getLongitude(),0.00001);
        assertEquals(2025,meta.get().getDateTime().getYear());
        assertEquals(Month.MARCH,meta.get().getDateTime().getMonth());
        assertEquals(1,meta.get().getDateTime().getDayOfMonth());
        assertEquals(13,meta.get().getDateTime().getHour());
        assertEquals(56,meta.get().getDateTime().getMinute());
        assertEquals(43,meta.get().getDateTime().getSecond());
    }

    @Test
    void testMetaDataMp4() {
        File mp4 = new File("src/test/resources/synchronise/20171224_152453.mp4");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(mp4);

        assertTrue(meta.isPresent());
        assertTrue(meta.get().isValid());
        assertNotNull(meta.get().getImageSize());
        assertNotNull(meta.get().getDateTime());
        assertNull(meta.get().getLatLong());
        assertEquals("video/mp4",meta.get().getMimeType());
        assertEquals(1920,meta.get().getImageSize().width());
        assertEquals(1080,meta.get().getImageSize().height());
        assertEquals(2017,meta.get().getDateTime().getYear());
        assertEquals(Month.DECEMBER,meta.get().getDateTime().getMonth());
        assertEquals(24,meta.get().getDateTime().getDayOfMonth());
        assertEquals(15,meta.get().getDateTime().getHour());
        assertEquals(25,meta.get().getDateTime().getMinute());
        assertEquals(14,meta.get().getDateTime().getSecond());
    }

    @Test
    void testMetaDataPDF() {
        File pdf = new File("src/test/resources/synchronise/Document.PDF");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(pdf);

        assertFalse(meta.isPresent());
    }

    @Test
    void testMetaDataODT() {
        File pdf = new File("src/test/resources/synchronise/Document.odt");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(pdf);

        assertFalse(meta.isPresent());
    }

    @Test
    void testMetaDataText() {
        File text = new File("src/test/resources/synchronise/Text.txt");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(text);

        assertFalse(meta.isPresent());
    }

    @Test
    void testMetaDataHEIC() {
        File text = new File("src/test/resources/synchronise/Photo.HEIC");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(text);

        assertTrue(meta.isPresent());
        assertTrue(meta.get().isValid());
        assertNotNull(meta.get().getImageSize());
        assertNotNull(meta.get().getDateTime());
        assertNotNull(meta.get().getLatLong());
        assertEquals("image/heic",meta.get().getMimeType());
        assertEquals(53.26959,meta.get().getLatLong().getLatitude(),0.00001);
        assertEquals(-9.05526,meta.get().getLatLong().getLongitude(),0.00001);
        assertEquals(3024,meta.get().getImageSize().width());
        assertEquals(4032,meta.get().getImageSize().height());
        assertEquals(2022,meta.get().getDateTime().getYear());
        assertEquals(Month.AUGUST,meta.get().getDateTime().getMonth());
        assertEquals(20,meta.get().getDateTime().getDayOfMonth());
        assertEquals(9,meta.get().getDateTime().getHour());
        assertEquals(13,meta.get().getDateTime().getMinute());
        assertEquals(37,meta.get().getDateTime().getSecond());
    }

    @Test
    void testMetaDataPNG() {
        File text = new File("src/test/resources/synchronise/Photo.png");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(text);

        assertTrue(meta.isPresent());
        assertTrue(meta.get().isValid());
        assertNotNull(meta.get().getImageSize());
        assertNotNull(meta.get().getDateTime());
        assertNotNull(meta.get().getLatLong());
        assertNull(meta.get().getDuration());
        assertEquals("image/png",meta.get().getMimeType());
        assertEquals(51.601416666666665,meta.get().getLatLong().getLatitude(),0.00001);
        assertEquals(-0.37810,meta.get().getLatLong().getLongitude(),0.00001);
        assertEquals(3024,meta.get().getImageSize().width());
        assertEquals(4032,meta.get().getImageSize().height());
        assertEquals(2022,meta.get().getDateTime().getYear());
        assertEquals(Month.MAY,meta.get().getDateTime().getMonth());
        assertEquals(20,meta.get().getDateTime().getDayOfMonth());
        assertEquals(13,meta.get().getDateTime().getHour());
        assertEquals(21,meta.get().getDateTime().getMinute());
        assertEquals(59,meta.get().getDateTime().getSecond());
    }

    @Test
    void testMetaDataInterpretation() {
        Map<String,String> map = new HashMap<>();
        map.put("mime type","image/jpeg");
        map.put("date/time original","2023:05:21 12:37:23");
        map.put("image size","10x12");
        map.put("gps position","51 deg 27' 22.41\" N, 2 deg 37' 32.56\" W");

        FileSystemImageData imageData = new FileSystemImageData(map);

        assertTrue(imageData.isValid());
        assertTrue(imageData.isImage());
        assertEquals(10,imageData.getImageSize().width());
        assertEquals(12,imageData.getImageSize().height());

        // Check the video.
        map = new HashMap<>();
        map.put("mime type","video/mp4");
        map.put("date/time original","2023:05:21 12:37:23");
        map.put("image size","10x12");
        map.put("gps position","51 deg 27' 22.41\" N, 2 deg 37' 32.56\" W");
        map.put("duration","1:01");

        FileSystemImageData videoData = new FileSystemImageData(map);
        assertTrue(videoData.isValid());
        assertTrue(videoData.isVideo());
        assertEquals(10,videoData.getImageSize().width());
        assertEquals(12,videoData.getImageSize().height());
        assertEquals(61,videoData.getDuration(),0.00001);

        // Check the video.
        map = new HashMap<>();
        map.put("mime type","video/mp4");
        map.put("date/time original","2023:05:21 12:37:23");
        map.put("image size","10x12");
        map.put("gps position","51 deg 27' 22.41\" N, 2 deg 37' 32.56\" W");
        map.put("duration","1:01:01");

        videoData = new FileSystemImageData(map);
        assertTrue(videoData.isValid());
        assertTrue(videoData.isVideo());
        assertEquals(3661,videoData.getDuration(),0.00001);
    }

    @Test
    void testCustomMetaDataFromConvertedMp4() {
        File mp4 = new File("src/test/resources/synchronise/IMG_1015_converted.mp4");

        Optional<FileSystemCustomData> customData = fileSystem.readCustomMetaData(mp4);

        assertTrue(customData.isPresent(), "Converted MP4 should have custom metadata");
        assertEquals("img_1015.mov", customData.get().getOriginalFile());
        assertEquals("8d4f46976377897dfadf214d0526cf56", customData.get().getOriginalMd5());
        assertEquals(1821435L, customData.get().getOriginalSize());
    }

    @Test
    void testCustomMetaDataWithUnderscoreKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("jbr_original_file", "IMG_1015.MOV");
        map.put("jbr_original_file_md5", "8d4f46976377897dfadf214d0526cf56");
        map.put("jbr_original_file_size", "12345678");

        FileSystemCustomData customData = new FileSystemCustomData(map);

        assertTrue(customData.hasData());
        assertEquals("IMG_1015.MOV", customData.getOriginalFile());
        assertEquals("8d4f46976377897dfadf214d0526cf56", customData.getOriginalMd5());
        assertEquals(12345678L, customData.getOriginalSize());
    }

    @Test
    void testCustomMetaDataWithSpaceKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("jbr original file", "IMG_1015.MOV");
        map.put("jbr original file md5", "8d4f46976377897dfadf214d0526cf56");
        map.put("jbr original file size", "12345678");

        FileSystemCustomData customData = new FileSystemCustomData(map);

        assertTrue(customData.hasData());
        assertEquals("IMG_1015.MOV", customData.getOriginalFile());
        assertEquals("8d4f46976377897dfadf214d0526cf56", customData.getOriginalMd5());
        assertEquals(12345678L, customData.getOriginalSize());
    }

    @Test
    void testCustomMetaDataEmpty() {
        Map<String, String> map = new HashMap<>();
        map.put("mime type", "video/mp4");

        FileSystemCustomData customData = new FileSystemCustomData(map);

        assertFalse(customData.hasData());
        assertNull(customData.getOriginalFile());
        assertNull(customData.getOriginalMd5());
        assertNull(customData.getOriginalSize());
    }

    @Test
    void testCustomMetaDataPartial() {
        Map<String, String> map = new HashMap<>();
        map.put("jbr_original_file", "test.mov");

        FileSystemCustomData customData = new FileSystemCustomData(map);

        assertTrue(customData.hasData());
        assertEquals("test.mov", customData.getOriginalFile());
        assertNull(customData.getOriginalMd5());
        assertNull(customData.getOriginalSize());
    }

    @Test
    void testCustomMetaDataInvalidSize() {
        Map<String, String> map = new HashMap<>();
        map.put("jbr_original_file", "test.mov");
        map.put("jbr_original_file_size", "not-a-number");

        FileSystemCustomData customData = new FileSystemCustomData(map);

        assertTrue(customData.hasData());
        assertNull(customData.getOriginalSize());
    }
}
