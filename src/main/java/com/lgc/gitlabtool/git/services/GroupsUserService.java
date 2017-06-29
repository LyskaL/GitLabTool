package com.lgc.gitlabtool.git.services;

import java.util.List;
import java.util.Map;

import com.lgc.gitlabtool.git.entities.Group;
import com.lgc.gitlabtool.git.entities.User;

public interface GroupsUserService {

    /**
     * Gets user's groups
     *
     * @param user User with groups
     * @return List of groups for user <br>
     * null, if an error occurred during the request
     */
    Object getGroups(User user);

    /**
     * Clones list of user's groups and adds their to the ClonedGroups class.
     *
     * @param groups           List of groups for cloning
     * @param destinationPath  Local path of workspace
     * @param progressListener Listener for obtaining data on the process of performing the operation.
     */
    void cloneGroups(List<Group> groups, String destinationPath, ProgressListener progressListener);

    /**
     * Gets group by id
     *
     * @param idGroup Id of group
     * @return Group
     */
    Group getGroupById(int idGroup);

    /**
     * Imports a group from the local repository. Gets all data about a group from the GitLab.
     * Also, updates statuses, types and local paths of cloned projects.
     *
     * @param  groupPath path to cloned group
     * @throws IllegalArgumentException if data is incorrect
     * @return loaded group
     */
    Group importGroup(String groupPath);

    /**
    * Removes a group from the workspace
    *
    * @param group                 the group for deletion
    * @param isRemoveFromLocalDisk if <true> remove group from local disk, otherwise - false
    *
    * @return status and message operation.
    */
    Map<Boolean, String> removeGroup(Group group, boolean isRemoveFromLocalDisk);

}