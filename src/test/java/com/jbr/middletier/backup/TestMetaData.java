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
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TestMetaData {
    @Autowired
    FileSystem fileSystem;

    @Test
    public void testMetaData() {
        File jpeg = new File("src/test/resources/synchronise/IMG_1015.JPEG");
        //Optional<FileSystemImageData> image = fileSystem.readImageMetaData(jpeg);

        //Assert.assertTrue(image.isPresent());

        File mov = new File("src/test/resources/synchronise/IMG_1015.MOV");
        Optional<FileSystemImageData> video = fileSystem.readImageMetaData(mov);

        Assert.assertTrue(video.isPresent());
    }
}
