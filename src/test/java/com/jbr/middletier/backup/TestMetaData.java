package com.jbr.middletier.backup;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.backup.manager.FileSystemImageData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import com.jbr.middletier.backup.manager.FileSystem;

import java.io.File;
import java.time.Month;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestMetaData {
    @Autowired
    FileSystem fileSystem;

    @Test
    public void testMetaDataJpeg() {
        File jpeg = new File("src/test/resources/synchronise/IMG_1015.JPEG");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(jpeg);

        Assert.assertTrue(meta.isPresent());
        Assert.assertTrue(meta.get().isValid());
        Assert.assertNotNull(meta.get().getImageSize());
        Assert.assertNotNull(meta.get().getDateTime());
        Assert.assertNotNull(meta.get().getLatLong());
        Assert.assertEquals("image/jpeg",meta.get().getMimeType());
        Assert.assertEquals(1536,meta.get().getImageSize().width());
        Assert.assertEquals(2048,meta.get().getImageSize().height());
        Assert.assertEquals(51.456225,meta.get().getLatLong().getLatitude(),0.00001);
        Assert.assertEquals(-2.6257111,meta.get().getLatLong().getLongitude(),0.00001);
        Assert.assertEquals(2025,meta.get().getDateTime().getYear());
        Assert.assertEquals(Month.MARCH,meta.get().getDateTime().getMonth());
        Assert.assertEquals(1,meta.get().getDateTime().getDayOfMonth());
        Assert.assertEquals(13,meta.get().getDateTime().getHour());
        Assert.assertEquals(56,meta.get().getDateTime().getMinute());
        Assert.assertEquals(44,meta.get().getDateTime().getSecond());
    }

    @Test
    public void testMetaDataMov() {
        File jpeg = new File("src/test/resources/synchronise/IMG_1015.MOV");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(jpeg);

        Assert.assertTrue(meta.isPresent());
        Assert.assertTrue(meta.get().isValid());
        Assert.assertNotNull(meta.get().getImageSize());
        Assert.assertNotNull(meta.get().getDateTime());
        Assert.assertNotNull(meta.get().getLatLong());
        Assert.assertEquals("video/quicktime",meta.get().getMimeType());
        Assert.assertEquals(960,meta.get().getImageSize().width());
        Assert.assertEquals(720,meta.get().getImageSize().height());
        Assert.assertEquals(51.4562,meta.get().getLatLong().getLatitude(),0.00001);
        Assert.assertEquals(-2.6257,meta.get().getLatLong().getLongitude(),0.00001);
        Assert.assertEquals(2025,meta.get().getDateTime().getYear());
        Assert.assertEquals(Month.MARCH,meta.get().getDateTime().getMonth());
        Assert.assertEquals(1,meta.get().getDateTime().getDayOfMonth());
        Assert.assertEquals(13,meta.get().getDateTime().getHour());
        Assert.assertEquals(56,meta.get().getDateTime().getMinute());
        Assert.assertEquals(43,meta.get().getDateTime().getSecond());
    }

    @Test
    public void testMetaDataMp4() {
        File mp4 = new File("src/test/resources/synchronise/20171224_152453.mp4");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(mp4);

        Assert.assertTrue(meta.isPresent());
        Assert.assertTrue(meta.get().isValid());
        Assert.assertNotNull(meta.get().getImageSize());
        Assert.assertNotNull(meta.get().getDateTime());
        Assert.assertNull(meta.get().getLatLong());
        Assert.assertEquals("video/mp4",meta.get().getMimeType());
        Assert.assertEquals(1920,meta.get().getImageSize().width());
        Assert.assertEquals(1080,meta.get().getImageSize().height());
        Assert.assertEquals(2017,meta.get().getDateTime().getYear());
        Assert.assertEquals(Month.DECEMBER,meta.get().getDateTime().getMonth());
        Assert.assertEquals(24,meta.get().getDateTime().getDayOfMonth());
        Assert.assertEquals(15,meta.get().getDateTime().getHour());
        Assert.assertEquals(25,meta.get().getDateTime().getMinute());
        Assert.assertEquals(14,meta.get().getDateTime().getSecond());
    }

    @Test
    public void testMetaDataPDF() {
        File pdf = new File("src/test/resources/synchronise/Document.PDF");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(pdf);

        Assert.assertFalse(meta.isPresent());
    }

    @Test
    public void testMetaDataODT() {
        File pdf = new File("src/test/resources/synchronise/Document.odt");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(pdf);

        Assert.assertFalse(meta.isPresent());
    }

    @Test
    public void testMetaDataText() {
        File text = new File("src/test/resources/synchronise/Text.txt");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(text);

        Assert.assertFalse(meta.isPresent());
    }

    @Test
    public void testMetaDataHEIC() {
        File text = new File("src/test/resources/synchronise/Photo.HEIC");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(text);

        Assert.assertTrue(meta.isPresent());
        Assert.assertTrue(meta.get().isValid());
        Assert.assertNotNull(meta.get().getImageSize());
        Assert.assertNotNull(meta.get().getDateTime());
        Assert.assertNotNull(meta.get().getLatLong());
        Assert.assertEquals("image/heic",meta.get().getMimeType());
        Assert.assertEquals(53.26959,meta.get().getLatLong().getLatitude(),0.00001);
        Assert.assertEquals(-9.05526,meta.get().getLatLong().getLongitude(),0.00001);
        Assert.assertEquals(3024,meta.get().getImageSize().width());
        Assert.assertEquals(4032,meta.get().getImageSize().height());
        Assert.assertEquals(2022,meta.get().getDateTime().getYear());
        Assert.assertEquals(Month.AUGUST,meta.get().getDateTime().getMonth());
        Assert.assertEquals(20,meta.get().getDateTime().getDayOfMonth());
        Assert.assertEquals(9,meta.get().getDateTime().getHour());
        Assert.assertEquals(13,meta.get().getDateTime().getMinute());
        Assert.assertEquals(37,meta.get().getDateTime().getSecond());
    }

    @Test
    public void testMetaDataPNG() {
        File text = new File("src/test/resources/synchronise/Photo.png");

        Optional<FileSystemImageData> meta = fileSystem.readImageMetaData(text);

        Assert.assertTrue(meta.isPresent());
        Assert.assertTrue(meta.get().isValid());
        Assert.assertNotNull(meta.get().getImageSize());
        Assert.assertNotNull(meta.get().getDateTime());
        Assert.assertNotNull(meta.get().getLatLong());
        Assert.assertNull(meta.get().getDuration());
        Assert.assertEquals("image/png",meta.get().getMimeType());
        Assert.assertEquals(51.601416666666665,meta.get().getLatLong().getLatitude(),0.00001);
        Assert.assertEquals(-0.37810,meta.get().getLatLong().getLongitude(),0.00001);
        Assert.assertEquals(3024,meta.get().getImageSize().width());
        Assert.assertEquals(4032,meta.get().getImageSize().height());
        Assert.assertEquals(2022,meta.get().getDateTime().getYear());
        Assert.assertEquals(Month.MAY,meta.get().getDateTime().getMonth());
        Assert.assertEquals(20,meta.get().getDateTime().getDayOfMonth());
        Assert.assertEquals(13,meta.get().getDateTime().getHour());
        Assert.assertEquals(21,meta.get().getDateTime().getMinute());
        Assert.assertEquals(59,meta.get().getDateTime().getSecond());
    }
}
