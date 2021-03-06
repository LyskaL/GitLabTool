package com.lgc.gitlabtool.git.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class of utilities for working with java.nio.file.Path class.
 *
 * @author Lyudmila Lyska
 */
public class PathUtilities {

    private static final Logger logger = LogManager.getLogger(PathUtilities.class);
    public static final String PATH_NOT_EXISTS_OR_NOT_DIRECTORY = "The transmitted path does not exist or is not a directory.";

    /**
     * Checks path is exist and it is directory.
     *
     * @param path the local path
     * @return <code>true</code> if a path exists and it is folder, otherwise <code>false</code>.
     */
    public static boolean isExistsAndDirectory(Path path) {
        return path != null && Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * Checks path is exist and it is regular file.
     *
     * @param path the local path
     * @return <code>true</code> if a path exists and it is regular file, otherwise <code>false</code>.
     */
    public static boolean isExistsAndRegularFile(String path) {
        if (path == null) {
            return false;
        }
        return isExistsAndRegularFile(Paths.get(path));
    }

    /**
     * Checks path is exist and it is regular file.
     *
     * @param path the local path
     * @return <code>true</code> if a path exists and it is regular file, otherwise <code>false</code>.
     */
    public static boolean isExistsAndRegularFile(Path path) {
        return path != null && Files.exists(path) && Files.isRegularFile(path);
    }

    /**
     * Gets all folders from directory
     *
     * @param path path on disk
     * @return names of found folders
     */
    public static Collection<String> getFolders(Path path) {
        if (!isExistsAndDirectory(path)) {
            return Collections.emptyList();
        }
        Collection<String> folders = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            stream.forEach((dir) -> folders.add(dir.getFileName().toString()));
        } catch (IOException e) {
            logger.error("Error getting forders: " + e.getMessage());
        }
        return folders;
    }

    /**
     * Delete path from a local disk
     *
     * @param  path the path on the local disk
     * @return true - if it was deleted successful, otherwise - false.
     */
    public static boolean deletePath(Path path) {
        if (!Files.exists(path)) {
            return false;
        }
        try {
            FileUtils.forceDelete(path.toFile());
            return true;
        } catch (IOException e) {
            logger.error("Error deleting path: " + e.getMessage());
        }
        return false;
    }

    /**
     * Delete path from a local disk
     *
     * @param  string the path on the local disk
     * @return true - if it was deleted successful, otherwise - false.
     */
    public static boolean deletePath(String string) {
        if (string == null) {
            return false;
        }
        Path path = Paths.get(string);
        return deletePath(path);
    }

    /**
     * Creates path on the local disk.
     *
     * @param path the path for creating
     * @param isFolder if <code>true</code> - a folder will be create, <code>false</code> - a file will be create.
     * @return status of operation
     */
    public static boolean createPath(Path path, boolean isFolder) {
        try {
            if (isFolder) {
                Files.createDirectories(path);
                return true;
            }
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(path);
            return true;
        } catch (IOException e) {
            logger.error("Error creating path: " + e.getMessage());
        }
        return false;
    }
}
