package com.lgc.gitlabtool.git.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lgc.gitlabtool.git.entities.ClonedGroups;
import com.lgc.gitlabtool.git.entities.Group;
import com.lgc.gitlabtool.git.util.XMLParser;
import com.lgc.gitlabtool.git.xml.Server;
import com.lgc.gitlabtool.git.xml.Servers;


public class StorageServiceImpl implements StorageService {
    private static final Logger _logger = LogManager.getLogger(StorageServiceImpl.class);

    private static final String USER_HOME_PROPERTY = "user.home";
    private static final String WORKSPACE_DIRECTORY_PROPERTY = ".GitlabTool";
    private static final String PATH_SEPARATOR = File.separator;
    private static final String CLONED_GROUPS_FILENAME = "clonedGroups.xml";
    private static final String SERVERS_FILENAME = "servers.xml";

    private final String _workingDirectory;

    public StorageServiceImpl() {
        _workingDirectory = System.getProperty(USER_HOME_PROPERTY) + PATH_SEPARATOR + WORKSPACE_DIRECTORY_PROPERTY;
    }

    @Override
    public boolean updateStorage(String server, String username) {
        try {
            File file = getPropFile(server, username);
            XMLParser.saveObject(file, ClonedGroups.getInstance());
            return true;
        } catch (IOException | JAXBException e) {
            _logger.error("Error updating storage: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Group> loadStorage(String server, String username) {
        try {
            File file = getPropFile(server, username);
            List<Group> list = XMLParser.loadObject(file, ClonedGroups.class).getClonedGroups();
            return list == null ? Collections.emptyList() : list;
        } catch (IOException | JAXBException e) {
            _logger.error("Error loading storage: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateServers(Servers servers) {
        try {
            File file = getServersFile();
            XMLParser.saveObject(file, servers);
            return true;
        } catch (IOException | JAXBException e) {
            _logger.error("Error updating servers: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Servers loadServers() {
        Servers servers = null;
        try {
            File file = getServersFile();
            servers = XMLParser.loadObject(file, Servers.class);
        } catch (IOException | JAXBException e) {
            _logger.error("Error updating services: " + e.getMessage());
            _logger.warn(SERVERS_FILENAME + " file empty or does not exist. Load defaults");
        } finally {
            if (servers == null) {
                updateServers(new Servers());
                servers = loadServers();
            }
        }
        return servers;
    }

    private File getPropFile(String server, String username) throws IOException {
        Path propertyFilePath = Paths.get(_workingDirectory + PATH_SEPARATOR + server + PATH_SEPARATOR + username
                + PATH_SEPARATOR + CLONED_GROUPS_FILENAME);
        return getFile(propertyFilePath);
    }

    private File getServersFile() throws IOException {
        Path serversFilePath = Paths.get(_workingDirectory + PATH_SEPARATOR + SERVERS_FILENAME);
        return getFile(serversFilePath);
    }

    private File getFile(Path path) throws IOException {
        if (Files.exists(path)) {
            return path.toFile();
        } else {
            File file = new File(path.toUri());
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            file.createNewFile();
            return file;
        }
    }

    @Override
    public boolean updateLastUserName(String serverName, String userName) {
        Servers servers = loadServers();
        servers.getServers().forEach(server -> {
            if (server.getName().contentEquals(serverName)) {
                server.setLastUserName(userName);
                server.setLastUsed(true);
            } else {
                server.setLastUsed(false);
            }
        });

        return updateServers(servers);
    }

    @Override
    public String getLastUserName(String serverName) {
        return loadServers().getServer(serverName)
                            .map(Server::getLastUserName)
                            .orElse(StringUtils.EMPTY);
    }

    @Override
    public Server getLastUsedServer() {
        return loadServers().getServers()
                .stream()
                .filter(Server::isLastUsed)
                .findAny()
                .orElse(null);
    }
}
