package com.jbr.middletier.backup.integration;

import com.jbr.middletier.backup.WebTester;
import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.ClassificationDTO;
import com.jbr.middletier.backup.exception.ClassificationIdException;
import com.jbr.middletier.backup.manager.AssociatedFileDataManager;
import com.jbr.middletier.backup.manager.FileSystemObjectManager;
import org.junit.Assert;
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
    protected static final String importDirectory = "./target/it_test/import";
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
            this.md5 = (structureItems.length > 5) ? structureItems[5] : "";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
            this.dateTime = sdf.parse(structureItems[3]);

            checked = false;
        }
    }

    static class ValidateNode {
        public final String name;
        public final List<ValidateNode> children;
        public final boolean directory;
        public Date date;
        public String dateIndicator;
        public Long size;
        public String sizeIndicator;
        public String md5;
        public String md5Indicator;
        public boolean matched;
        public Integer dbId;
        public static int highestWidth = 0;
        public static boolean overallAssert = false;

        private ValidateNode(String name, boolean directory) {
            this.children = new ArrayList<>();
            this.name = name;
            this.directory = directory;
            this.date = null;
            this.dateIndicator = " ";
            this.size = null;
            this.sizeIndicator = " ";
            this.md5 = "";
            this.md5Indicator = " ";
            this.matched = false;
            this.dbId = null;
        }

        public ValidateNode(String name) {
            this(name,true);
        }

        public ValidateNode(String name, ValidateNode parent) {
            this(name,false);
            parent.addChild(this);
        }

        public void addChild(ValidateNode child) {
            this.children.add(child);
        }

        public ValidateNode insertDirectoryTree(int index, String[] directories) {
            if(index >= directories.length) {
                return this;
            }

            // Does this already exist?
            for(ValidateNode nextChild : children) {
                if(nextChild.name.equals(directories[index])) {
                    if(nextChild.directory) {
                        return nextChild.insertDirectoryTree(++index, directories);
                    }
                }
            }

            ValidateNode newChild = new ValidateNode(directories[index]);
            this.children.add(newChild);
            return newChild.insertDirectoryTree(++index, directories);
        }

        private void setHighest(int highest) {
            if(highest > highestWidth) {
                highestWidth = highest;
            }
        }

        public void getFileOutputWidth(int level) {
            if(0 == level) {
                highestWidth = 10;
                overallAssert = true;
            }
            for(ValidateNode nextChild : children) {
                if(nextChild.directory) {
                    setHighest(nextChild.name.length() + level);
                    nextChild.getFileOutputWidth(level+1);
                } else {
                    setHighest(nextChild.name.length() + level + 1);
                }
            }
        }

        private String getString(char padding, int length) {
            return new String(new char[length]).replace('\0',padding);
        }

        private void appendText(StringBuilder text, String item, int level) {
            text.append(getString(' ', level));
            text.append(item);

            int remaining = highestWidth - level - item.length();
            if(remaining > 0) {
                text.append(getString(' ', remaining));
            }
        }

        private void appendName(StringBuilder text, int level) {
            appendText(text, name, level);
        }

        private void outputSelf(int level) {
            StringBuilder text = new StringBuilder();

            text.append("|");
            text.append(directory ? "D" : "F");
            text.append(" ");
            if(matched || null == dbId) {
                appendName(text, level);
            } else {
                text.append(getString(' ', highestWidth));
            }
            text.append("|");
            if(matched || null != dbId) {
                appendName(text, level);
            } else {
                text.append(getString(' ', highestWidth));
            }
            text.append("|");

            if(matched) {
                text.append(" |");
                text.append(dateIndicator);
                if(!dateIndicator.equals(" ")) {
                    overallAssert = false;
                }
                text.append("|");

                text.append(sizeIndicator);
                if(!sizeIndicator.equals(" ")) {
                    overallAssert = false;
                }
                text.append("|");

                text.append(md5Indicator);
                if(!md5Indicator.equals(" ")) {
                    overallAssert = false;
                }
            } else {
                if(null == dbId) {
                    text.append("<");
                } else {
                    text.append(">");
                }
                overallAssert = false;
                text.append(" | | ");
            }
            text.append("|");

            LOG.info(text.toString());
        }

        private void outputLine() {

            String text = "+" +
                    getString('-', highestWidth + 2) +
                    "+" +
                    getString('-', highestWidth) +
                    "+-+-+-+-+";

            LOG.info(text);
        }

        private void outputHeaderFooter(boolean header) {
            outputLine();
            if(header) {
                StringBuilder text = new StringBuilder();
                text.append("|");
                text.append("  ");
                appendText(text,"Structure", 0);
                text.append("|");
                appendText(text,"Database", 0);
                text.append("| |D|S|M|");

                LOG.info(text.toString());

                outputLine();
            }
        }

        public void output(int level) {
            if(0 == level) {
                outputHeaderFooter(true);
            }

            // Output this.
            if(level > 0)
                this.outputSelf(level);

            // Output Directories
            for(ValidateNode nextNode : children) {
                if(nextNode.directory) {
                    nextNode.output(level+1);
                }
            }

            // Output the file children
            for(ValidateNode nextNode : children) {
                if(!nextNode.directory) {
                    nextNode.outputSelf(level + 1);
                }
            }

            if(0 == level) {
                outputHeaderFooter(false);
            }
        }

        public boolean allOK() {
            return overallAssert;
        }

        public static void insertDbDirectoryTree(int sourceId, ValidateNode node, FileSystemObjectManager fileSystemObjectManager) {
            // get the files and directories in the db at this level.
            List<DirectoryInfo> directories = new ArrayList<>();
            List<FileInfo> files = new ArrayList<>();
            fileSystemObjectManager.loadImmediateByParent(sourceId,directories,files);

            // Add the files.
            for(FileInfo nextFile : files) {
                boolean matched = false;

                for(ValidateNode childNode : node.children) {
                    if(!childNode.directory && childNode.name.equals(nextFile.getName())) {
                        childNode.dbId = nextFile.getIdAndType().getId();
                        childNode.matched = true;
                        childNode.dateIndicator = childNode.date.equals(nextFile.getDate()) ? " " : "X";
                        childNode.sizeIndicator = childNode.size.equals(nextFile.getSize()) ? " " : "X";
                        childNode.md5Indicator = childNode.md5.equals(nextFile.getMD5().toString()) ? " " : "X";
                        matched = true;
                        break;
                    }
                }

                if(!matched) {
                    // This file needs to be added as not matched.
                    ValidateNode dbFile = new ValidateNode(nextFile.getName(),node);
                    dbFile.dbId = nextFile.getIdAndType().getId();
                }
            }

            // Process the directories
            for(DirectoryInfo nextDirectory : directories) {
                boolean matched = false;

                for(ValidateNode childNode : node.children) {
                    if(childNode.directory && childNode.name.equals(nextDirectory.getName())) {
                        childNode.dbId = nextDirectory.getIdAndType().getId();
                        childNode.matched = true;
                        matched = true;

                        // Need to process the children.
                        insertDbDirectoryTree(childNode.dbId, childNode, fileSystemObjectManager);
                    }
                }

                if(!matched) {
                    // This needs to be added.
                    ValidateNode newDirectory = new ValidateNode(nextDirectory.getName());
                    node.addChild(newDirectory);

                    // Process this directory
                    insertDbDirectoryTree(nextDirectory.getIdAndType().getId(), newDirectory, fileSystemObjectManager);
                }
            }
        }
    }

    protected void validateSource(FileSystemObjectManager fileSystemObjectManager,
                                  Source source,
                                  List<StructureDescription> structure) {

        // Create a tree of the comparison between the structure and the database.

        // Start by creating a tree of the structure.
        ValidateNode tree = new ValidateNode(".");

        for(StructureDescription nextFile : structure) {
            ValidateNode newDirectory = tree.insertDirectoryTree(0, nextFile.directory.split(FileSystems.getDefault().getSeparator()));
            ValidateNode newFile = new ValidateNode(nextFile.destinationName, newDirectory);
            newFile.size = nextFile.fileSize;
            newFile.date = nextFile.dateTime;
            newFile.md5 = nextFile.md5;
        }

        // Add the directories into the structure.
        ValidateNode.insertDbDirectoryTree(source.getIdAndType().getId(), tree, fileSystemObjectManager);

        // Output the state of the comparison.
        tree.getFileOutputWidth(0);

        // Output the details of what we found.
        tree.output(0);

        // Assert OK.
        if(!tree.allOK()) {
            LOG.warn("Something is different.");
        }
        Assert.assertTrue(tree.allOK());
    }

    protected List<StructureDescription> getTestStructure(String testName) throws IOException, ParseException {
        List<StructureDescription> result = new ArrayList<>();

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("synchronise/structure/" + testName + ".structure.txt");
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

        //noinspection ResultOfMethodCallIgnored
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

        deleteDirectoryContents(new File(importDirectory).toPath());
        Files.createDirectories(new File(importDirectory).toPath());
    }

    protected void addClassification(AssociatedFileDataManager associatedFileDataManager, String regex, ClassificationActionType action, int order, boolean useMD5, boolean image, boolean video) throws ClassificationIdException {
        for(Classification nextClassification : associatedFileDataManager.internalFindAllClassification()) {
            if(nextClassification.getRegex().equalsIgnoreCase(regex)) {
                return;
            }
        }

        // If we get here, it should be added.
        ClassificationDTO newClassificationDTO = new ClassificationDTO();
        newClassificationDTO.setRegex(regex);
        newClassificationDTO.setOrder(order);
        newClassificationDTO.setVideo(video);
        newClassificationDTO.setImage(image);
        newClassificationDTO.setAction(action);
        newClassificationDTO.setUseMD5(useMD5);

        associatedFileDataManager.createClassification(newClassificationDTO);
    }
}
