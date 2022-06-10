package com.jbr.middletier.backup.integration;

import com.jbr.middletier.backup.WebTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FileTester extends WebTester {
    private static final Logger LOG = LoggerFactory.getLogger(FileTester.class);
    protected static final String sourceDirectory = "./target/it_test/source";
    protected static final String destinationDirectory = "./target/it_test/destination";

    protected static class StructureDescription {
        public final String filename;
        public final String directory;
        public final String destinationName;
        public final String md5;
        public final Date dateTime;
        public final Long fileSize;
        public boolean checked;

        public StructureDescription(String description) throws ParseException {
            String[] structureItems = description.split("\\s+");

            this.filename = structureItems[0];
            this.directory = structureItems[1];
            this.destinationName = structureItems[2];
            this.fileSize = (structureItems.length > 4) ? Long.parseLong(structureItems[4]) : null;
            this.md5 = (structureItems.length > 5) ? structureItems[5] : null;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
            this.dateTime = sdf.parse(structureItems[3]);

            checked = false;
        }
    }

    protected List<StructureDescription> getTestStructure(String testName) throws IOException, ParseException {
        List<StructureDescription> result = new ArrayList<>();

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("synchronise/" + testName + ".structure.txt");
        assert stream != null;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        String resource;
        while((resource = br.readLine()) != null) {
            result.add(new StructureDescription(resource));
            LOG.info(resource);
        }

        return result;
    }

    protected void copyFiles(List<StructureDescription> description, String destination) throws IOException {
        for(StructureDescription nextFile: description) {
            Files.createDirectories(new File(destination + FileSystems.getDefault().getSeparator() + nextFile.directory).toPath());

            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("synchronise/" + nextFile.filename);

            if(stream != null) {
                Path destinationFile = new File(destination + FileSystems.getDefault().getSeparator() + nextFile.directory + FileSystems.getDefault().getSeparator() + nextFile.destinationName).toPath();
                Files.copy(stream,
                        destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);

                Files.setLastModifiedTime(destinationFile, FileTime.fromMillis(nextFile.dateTime.getTime()));
            }
        }
    }

    protected void deleteDirectoryContents(Path path) throws IOException {
        if(!Files.exists(path))
            return;

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    protected void initialiseDirectories() throws IOException {
        // During this test create files in the following directories
        deleteDirectoryContents(new File(sourceDirectory).toPath());
        Files.createDirectories(new File(sourceDirectory).toPath());

        deleteDirectoryContents(new File(destinationDirectory).toPath());
        Files.createDirectories(new File(destinationDirectory).toPath());
    }
}
