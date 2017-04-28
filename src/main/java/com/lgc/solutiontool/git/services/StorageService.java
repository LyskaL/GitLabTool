package com.lgc.solutiontool.git.services;


import java.util.Map;

import com.lgc.solutiontool.git.entities.Group;
import com.lgc.solutiontool.git.xml.Servers;

/**
 * Class for work with program storage.
 *
 * @author Pavlo Pidhornyi
 */
public interface StorageService {

    /**
     * Updates user preference storage
     *
     * @param server   Name of current git-server
     * @param username Name of current user
     * @return Status of updating storage
     */
    boolean updateStorage(String server, String username);

    /**
     * Load cloned user groups from local storage
     *
     * @param server   Name of current git-server
     * @param username Name of current user
     * @return Cloned groups and their directories
     */
    Map<Group, String> loadStorage(String server, String username);
    
    boolean updateServers(Servers servers);
    
    Servers loadServers();
}
